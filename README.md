# curious-test-utils

A collection of useful testing utilities.

## json.JsonTestUtils

Simple utility that uses `JSONAssert` to compare JSON representation of POJO with contents of a `.json` file with
expected json value.

Usage -
see [JsonTestUtilsTest.java](src/test/java/com/github/curiousoddman/curioustestutils/json/JsonTestUtilsTest.java)

## junit.extension.MetricsExtension

Junit 5 extension, that manages metrics registry, so that your test does not need to manage it separately.

Usage:

1. `@ExtendWith(MetricsExtension.class)` - on your test class
2. Add `MeterRegistry meterRegistry` parameter to your `@Test` method
3. do your stuff
4. check metrics like this: `assertJsonEquals("expected.json", meterRegistry, List.of(<<<interesting metrics>>>));`

## populate.PopulatePojo

A simple utility class that populates pojo objects with static "random" data.
Data populated is based on seed value and does not change from run to run.
Useful for large data objects where all fields need to be populated for a test purpose.

See usage: [PopulatePojoTests.java](src/test/java/com/github/curiousoddman/curioustestutils/populate/PopulatePojoTests.java)

## testcontainers.db.*

Almost works. :D
