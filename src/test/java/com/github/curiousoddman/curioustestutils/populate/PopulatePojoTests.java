package com.github.curiousoddman.curioustestutils.populate;

import com.github.curiousoddman.curioustestutils.populate.impl.Context;
import com.github.curiousoddman.curioustestutils.populate.impl.DefaultValueGenerators;
import com.github.curiousoddman.curioustestutils.populate.impl.ReadOnlyContext;
import com.github.curiousoddman.curioustestutils.populate.impl.SpecificValueGenerators;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static com.github.curiousoddman.curioustestutils.json.JsonTestUtils.assertJsonEquals;
import static com.github.curiousoddman.curioustestutils.populate.PopulatePojo.populatePojo;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class PopulatePojoTests {

    @SneakyThrows
    @Test
    void allFieldsPopulatedTest() {
        DemoPojo demoPojo = populatePojo(new DemoPojo(), 10);
        assertJsonEquals("/TestUtilsTest/allFieldsPopulatedTest.json", demoPojo);
    }

    @Test
    void subObjectAlsoPopulatedTest() {
        WrapperClass wrapperClass = populatePojo(new WrapperClass(), 10);
        assertJsonEquals("/TestUtilsTest/subObjectAlsoPopulatedTest.json", wrapperClass);
    }

    @Test
    void negativeSeedValuesDoNotFailGenerationTest() {
        WrapperClass pojo = new WrapperClass();
        WrapperClass wrapperClass = assertDoesNotThrow(() -> populatePojo(pojo, -1));
        assertJsonEquals("/TestUtilsTest/negativeSeedValuesDoNotFailGenerationTest.json", wrapperClass);
    }

    @Test
    void testFailedToPopulate() {
        try {
            WrappedAroundNoArgConstructorMissing wrapperClass = populatePojo(new WrappedAroundNoArgConstructorMissing(), 10);
            fail("Expected an exception");
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            assertEquals("Don't know how to generate value for setter at path 'setNoArgConstructorMissing<-setIntermediateClass'", e.getMessage());
        }
    }

    @Test
    void customGeneratorTest() {
        DemoPojo pojo = populatePojo(new DemoPojo(), 10,
                new DefaultValueGenerators.StringGenerator() {
                    @Override
                    public String generateValue(ReadOnlyContext context) {
                        return "Always Cool";
                    }
                });
        assertJsonEquals("/TestUtilsTest/customGeneratorTest.json", pojo);
    }

    @Test
    void collectionsGeneratorTest() {
        CollectionsPojo collectionsPojo = populatePojo(new CollectionsPojo(), 8, new SpecificValueGenerators.FilledListGenerator(10, Integer.class));
        assertJsonEquals("/TestUtilsTest/collectionsGeneratorTest.json", collectionsPojo);
    }

    @Test
    void canGeneratePojoWhenNoNoArgsConstructorPresentTest() {
        RequiredArgsConstructorPojo generatedValue = populatePojo(RequiredArgsConstructorPojo.class, new Context(10));
        assertJsonEquals("/TestUtilsTest/canGeneratePojoWhenNoNoArgsConstructorPresentTest.json", generatedValue);
    }

    public enum DemoEnum {
        HELLO,
        WORLD,
        WELCOME,
        TO,
        HELL
    }

    @Data
    @NoArgsConstructor
    public static class CollectionsPojo {
        private Map<String, String> map;
        private Map<String, Integer> mapOfInts;
        private List<Integer> list;
        private List<String> stringList;
    }

    @Data
    @RequiredArgsConstructor
    public static class RequiredArgsConstructorPojo {
        private final int anInt;
        private final DemoEnum demoEnum;
        private final LocalDateTime localDateTime;
        private final DemoPojo demoPojo;
        private final List<String> stringList;

        private long aLong;
        private float aFloat;
        private double aDouble;
        private boolean aBoolean;
        private Integer integer;
        private Long longClass;
        private Float floatClass;
        private Double doubleClass;
        private Boolean booleanClass;
        private OffsetDateTime offsetDateTime;
    }

    @Data
    @NoArgsConstructor
    public static class DemoPojo {
        private int anInt;
        private long aLong;
        private float aFloat;
        private double aDouble;
        private boolean aBoolean;
        private Integer integer;
        private Long longClass;
        private Float floatClass;
        private Double doubleClass;
        private Boolean booleanClass;
        private OffsetDateTime offsetDateTime;
        private LocalDateTime localDateTime;

        private BigDecimal bigDecimal;

        private String string;

        private Instant instant;
        private LocalDate localDate;

        private Map<String, String> map;
        private List<Integer> list;

        private DemoEnum demoEnum;

        public void setSomething() {
            // no-args setter
        }

        public void setSomething(String something) {
            // overloaded setter with args
        }

        // Overloaded Non-restrictive method arg
        public void setCustom(String custom) {
            demoEnum = DemoEnum.valueOf(custom);
        }

        // Overloaded restrictive method arg
        public void setCustom(DemoEnum demoEnum) {
            this.demoEnum = demoEnum;
        }

        // (inverted order of methods) Overloaded restrictive method arg
        public void setCustomToo(DemoEnum demoEnum) {
            this.demoEnum = demoEnum;
        }

        // (inverted order of methods) Overloaded Non-restrictive method arg
        public void setCustomToo(String custom) {
            demoEnum = DemoEnum.valueOf(custom);
        }
    }

    @Data
    @NoArgsConstructor
    public static class WrapperClass {
        private DemoPojo demoPojo;
    }

    public static class NoArgConstructorMissing {
        public NoArgConstructorMissing(String arg) {

        }
    }

    @Data
    @NoArgsConstructor
    public static class IntermediateClass {
        private NoArgConstructorMissing noArgConstructorMissing;
    }

    @Data
    @NoArgsConstructor
    public static class WrappedAroundNoArgConstructorMissing {
        private IntermediateClass intermediateClass;
    }
}
