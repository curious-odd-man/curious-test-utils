package com.github.curiousoddman.curioustestutils.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.RequiredSearch;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.skyscreamer.jsonassert.*;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@UtilityClass
public class JsonTestUtils {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .appendPattern("x")
            .toFormatter();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer());
        module.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer());
        OBJECT_MAPPER.registerModule(new Jdk8Module());
        OBJECT_MAPPER.registerModule(module);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ") {
            @Override
            public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
                ZoneId utc = ZoneId.of("UTC");
                String formatted = DATE_TIME_FORMATTER.format(ZonedDateTime.ofInstant(date.toInstant(), utc));
                return toAppendTo.append(formatted);
            }
        };
        OBJECT_MAPPER.setDateFormat(dateFormat);
    }

    private static final boolean overwriteFilesOnFailedAssertion = Optional
            .ofNullable(System.getProperty("overwrite.json.on.failed.comparison"))
            .map(Boolean::parseBoolean)
            .orElse(false);

    @SneakyThrows
    public static void assertJsonEquals(String pathToExpectedFile, RequiredSearch metricSearch, String... pathsToIgnore) {
        List<MetricObject> metricObjects = metricSearch
                .meters()
                .stream()
                .map(meter -> new MetricObject(meter.getId().getName(), toStream(meter.measure()).toList(), meter.getId().getTags()))
                .sorted(Comparator.comparing(MetricObject::getName))
                .toList();

        assertJsonEquals(pathToExpectedFile, OBJECT_MAPPER.writeValueAsString(metricObjects), pathsToIgnore);
    }

    @SneakyThrows
    public static void assertJsonEquals(String pathToExpectedFile, MeterRegistry registry, List<String> meterNames, String... pathsToIgnore) {
        List<MetricObject> metricObjects = meterNames
                .stream()
                .map(registry::get)
                .map(RequiredSearch::meters)
                .flatMap(Collection::stream)
                .map(meter -> new MetricObject(meter.getId().getName(), toStream(meter.measure()).toList(), meter.getId().getTags()))
                .sorted(Comparator.comparing(MetricObject::getName))
                .toList();

        assertJsonEquals(pathToExpectedFile, OBJECT_MAPPER.writeValueAsString(metricObjects), pathsToIgnore);
    }

    @Value
    private static class MetricObject {
        String name;
        List<Measurement> measurements;
        List<Tag> metricTags;
    }

    @SneakyThrows
    public static void assertJsonEquals(String pathToExpectedFile, Object object, String... pathsToIgnore) {
        assertJsonEquals(pathToExpectedFile, OBJECT_MAPPER.writeValueAsString(object), pathsToIgnore);
    }

    @SneakyThrows
    public static void assertJsonEquals(String pathToExpectedFile, String actualValue, String... pathsToIgnore) {
        log.info("Actual json '{}'", actualValue);
        if (pathToExpectedFile.charAt(0) == '/' || pathToExpectedFile.charAt(0) == '\\') {
            pathToExpectedFile = pathToExpectedFile.substring(1);
        }
        Path expectedFilePathResolved = Path
                .of("src/test/resources")
                .resolve(pathToExpectedFile)
                .normalize()
                .toAbsolutePath();
        try {
            String expected = Files.readString(expectedFilePathResolved);
            if (pathsToIgnore.length > 0) {
                Map<Customization, String> customizationsPaths = new IdentityHashMap<>();
                for (String path : pathsToIgnore) {
                    customizationsPaths.put(new Customization(path, (o, o2) -> true), path);
                }

                UsageTrackingComparator comparator = new UsageTrackingComparator(JSONCompareMode.STRICT, customizationsPaths.keySet().toArray(new Customization[0]));
                JSONAssert.assertEquals(expected, actualValue, comparator);
                Set<Customization> unusedCustomizations = comparator.getUnusedCustomizations();
                if (!unusedCustomizations.isEmpty()) {
                    String unusedPaths = unusedCustomizations.stream().map(customizationsPaths::get).collect(Collectors.joining(", "));
                    throw new IllegalStateException("Path exclusions that are not found in data: " + unusedPaths);
                }
            } else {
                JSONAssert.assertEquals(expected, actualValue, JSONCompareMode.STRICT);
            }
        } catch (AssertionError | NoSuchFileException error) {
            if (overwriteFilesOnFailedAssertion) {
                Files.createDirectories(expectedFilePathResolved.getParent());
                Files.writeString(expectedFilePathResolved, actualValue);
            }
            throw error;
        }
    }

    public static <T> JsonAssertBuilder<T> assertJsonArray(Iterable<T> object) {
        return new JsonAssertBuilder<>(object);
    }

    @RequiredArgsConstructor
    public static class JsonAssertBuilder<T> {
        private final Iterable<T> collection;

        private Comparator<T> ordering = null;
        private List<String> ignorePaths = List.of();

        public JsonAssertBuilder<T> orderedBy(Comparator<T> ordering) {
            this.ordering = ordering;
            return this;
        }

        public <U extends Comparable<? super U>> JsonAssertBuilder<T> orderedBy(Function<? super T, U> keyExtractor) {
            this.ordering = Comparator.comparing(keyExtractor);
            return this;
        }

        public JsonAssertBuilder<T> ignoring(String... paths) {
            this.ignorePaths = List.of(paths);
            return this;
        }

        public void isEqualTo(String expectedJsonFilePath) {
            Stream<T> stream = ordering == null
                    ? toStream(collection)
                    : toStream(collection).sorted(Comparator.nullsFirst(ordering));

            assertJsonEquals(expectedJsonFilePath, stream.toList(), ignorePaths.toArray(new String[0]));
        }
    }

    public class OffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {
        @Override
        public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return OffsetDateTime.parse(parser.getText(), DATE_TIME_FORMATTER);
        }
    }

    public class OffsetDateTimeSerializer extends JsonSerializer<OffsetDateTime> {
        @Override
        public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.format(DATE_TIME_FORMATTER));
        }
    }

    // This is a copy of CustomComparator with additional tracking which customizations were not used
    public static class UsageTrackingComparator extends DefaultComparator {
        private final Collection<Customization> customizations;
        private final Set<Customization> unusedCustomizations = Collections.newSetFromMap(new IdentityHashMap<>());

        public UsageTrackingComparator(JSONCompareMode mode, Customization... customizations) {
            super(mode);
            this.customizations = Arrays.asList(customizations);
            unusedCustomizations.addAll(this.customizations);
        }

        @Override
        public void compareValues(String prefix, Object expectedValue, Object actualValue, JSONCompareResult result) throws JSONException {
            Customization customization = getCustomization(prefix);
            if (customization != null) {
                unusedCustomizations.remove(customization);
                try {
                    if (!customization.matches(prefix, actualValue, expectedValue, result)) {
                        result.fail(prefix, expectedValue, actualValue);
                    }
                } catch (ValueMatcherException e) {
                    result.fail(prefix, e);
                }
            } else {
                super.compareValues(prefix, expectedValue, actualValue, result);
            }
        }

        private Customization getCustomization(String path) {
            for (Customization c : customizations)
                if (c.appliesToPath(path))
                    return c;
            return null;
        }

        public Set<Customization> getUnusedCustomizations() {
            return Collections.unmodifiableSet(unusedCustomizations);
        }
    }

    private static <T> Stream<T> toStream(Iterable<T> measure) {
        return StreamSupport.stream(measure.spliterator(), false);
    }
}
