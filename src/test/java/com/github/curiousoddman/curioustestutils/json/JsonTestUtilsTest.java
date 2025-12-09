package com.github.curiousoddman.curioustestutils.json;

import com.github.curiousoddman.curioustestutils.junit.extension.MetricsExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.github.curiousoddman.curioustestutils.json.JsonTestUtils.assertJsonEquals;
import static com.github.curiousoddman.curioustestutils.populate.PopulatePojo.populatePojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MetricsExtension.class)
class JsonTestUtilsTest {

    TimeZone defaultZone = TimeZone.getDefault();

    @Test
    void verifyHappyFlow() {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Riga"));
            assertJsonEquals("expected-success.json", populatePojo(new TestPojo(), 10500));
        } finally {
            TimeZone.setDefault(defaultZone);
        }
    }

    @Test
    void verifyHappyFlowInAnotherTz() {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertJsonEquals("expected-success.json", populatePojo(new TestPojo(), 10500));
        } finally {
            TimeZone.setDefault(defaultZone);
        }
    }

    @Test
    void verifyError() {
        TestPojo testPojo = populatePojo(new TestPojo(), 10500);
        assertThrows(AssertionError.class, () -> assertJsonEquals("expected-failed.json", testPojo));
    }

    @Test
    void verifyNullIsNotSavedToJson() {
        assertJsonEquals("expected-without-nulls.json", new TestPojo());
    }

    @Test
    void verifyExclusionsWorkTest() {
        TestPojo testPojo = populatePojo(new TestPojo(), 10500);
        testPojo.setText("another-text-that-does-not-match");
        assertJsonEquals("expected-success.json", testPojo,
                "text");
    }

    @Test
    void verifySubelementExclusionsWorkTest() {
        OuterTestPojo testPojo = populatePojo(new OuterTestPojo(), 10500);
        testPojo.getTestPojo().setOffsetDateTime(OffsetDateTime.now());
        assertJsonEquals("expected-success-excluding-offsetdatetime.json", testPojo,
                "testPojo.offsetDateTime");
    }

    @Test
    void verifyListElementExclusionsWorkTest() {
        TestPojo testPojo = populatePojo(new TestPojo(), 10500);
        TestPojo testPojo1 = populatePojo(new TestPojo(), 10501);
        TestPojo testPojo2 = populatePojo(new TestPojo(), 10502);
        assertJsonEquals("expected-success-array-elemnt-with-excluded-field.json",
                List.of(testPojo, testPojo1, testPojo2),
                "*.offsetDateTime");
    }

    @Test
    void verifyListElementExclusionsWorkBuilderTest() {
        TestPojo testPojo = populatePojo(new TestPojo(), 10500);
        TestPojo testPojo1 = populatePojo(new TestPojo(), 10501);
        TestPojo testPojo2 = populatePojo(new TestPojo(), 10502);

        List<TestPojo> list = new ArrayList<>(List.of(testPojo1, testPojo, testPojo2));
        Collections.shuffle(list);

        JsonTestUtils.assertJsonArray(list)
                .orderedBy(TestPojo::getCount)
                .ignoring("*.offsetDateTime")
                .isEqualTo("expected-success-array-elemnt-with-excluded-field.json");
    }

    @Test
    void verifyMetricsTest(MeterRegistry meterRegistry) {
        Metrics.counter("test-counter", "tag1", "val1", "tag2", "val2").increment(100500);
        Metrics.timer("test-timer", "tag3", "val3").record(123, TimeUnit.SECONDS);

        assertJsonEquals("test-counter-expected.json", meterRegistry.get("test-counter"));
        assertJsonEquals("test-timer-expected.json", meterRegistry.get("test-timer"));
    }

    @Test
    void verifyMetricsListTest(MeterRegistry meterRegistry) {
        Metrics.counter("test-counter", "tag1", "val1", "tag2", "val2").increment(100500);
        Metrics.timer("test-timer", "tag3", "val3").record(123, TimeUnit.SECONDS);

        assertJsonEquals("test-metrics-expected.json", meterRegistry, List.of("test-counter", "test-timer"));
    }

    @Test
    void verifyNonExistentExclusionsAreReportedTest() {
        TestPojo testPojo = populatePojo(new TestPojo(), 10500);
        testPojo.setText("another-text-that-does-not-match");
        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () ->
                assertJsonEquals("expected-success.json", testPojo, "text", "the-field-that-does-not-exists"));
        assertEquals("Path exclusions that are not found in data: the-field-that-does-not-exists", illegalStateException.getMessage());
    }

    @Data
    public static class OuterTestPojo {
        TestPojo testPojo;
        String text;
    }

    @Data
    public static class TestPojo {
        String text;
        Integer count;
        long primitive;
        OffsetDateTime offsetDateTime;
        LocalDateTime localDateTime;
        LocalDate localDate;
        java.sql.Date sqlDate;
        java.sql.Timestamp sqlTimestamp;
        java.util.Date utilDate;
    }
}
