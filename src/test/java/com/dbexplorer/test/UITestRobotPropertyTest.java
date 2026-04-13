package com.dbexplorer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/**
 * Property-based tests for UITestRobot configuration parsing and AI skip logic.
 * Feature: ui-test-robot
 */
public class UITestRobotPropertyTest {

    private static final Gson GSON = new Gson();

    // Valid DatabaseType enum values
    private static final String[] DB_TYPES = {
            "POSTGRESQL", "MYSQL", "ORACLE", "SQLSERVER", "SQLITE", "DYNAMODB", "GENERIC"
    };

    // -------------------------------------------------------------------------
    // Generators
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> dbTypes() {
        return Arbitraries.of(DB_TYPES);
    }

    @Provide
    Arbitrary<String> nonEmptyStrings() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> optionalStrings() {
        return Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(50)
                .injectNull(0.3);
    }

    @Provide
    Arbitrary<UITestRobot.TestConfig> validConfigs() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30),  // connectionName
                Arbitraries.of(DB_TYPES),                                       // databaseType
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30),  // host
                Arbitraries.integers().between(1, 65535),                       // port
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30),  // databaseName
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30),  // username
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30)   // password
        ).as((connName, dbType, host, port, dbName, user, pass) ->
                new UITestRobot.TestConfig(connName, dbType, host, port, dbName, user, pass,
                        null, null, null, null, null, null, null)
        );
    }

    // -------------------------------------------------------------------------
    // Property 1: Config parsing round trip
    // -------------------------------------------------------------------------

    // Feature: ui-test-robot, Property 1: Config parsing round trip
    // **Validates: Requirements 1.2, 1.5, 21.1, 23.5**
    @Property(tries = 100)
    void configParsingRoundTrip(
            @ForAll("nonEmptyStrings") String connectionName,
            @ForAll("dbTypes") String databaseType,
            @ForAll("nonEmptyStrings") String host,
            @ForAll @IntRange(min = 1, max = 65535) int port,
            @ForAll("nonEmptyStrings") String databaseName,
            @ForAll("nonEmptyStrings") String username,
            @ForAll("nonEmptyStrings") String password,
            @ForAll("optionalStrings") String query,
            @ForAll("optionalStrings") String ddlScriptPath,
            @ForAll("optionalStrings") String aiProvider,
            @ForAll("optionalStrings") String aiModel,
            @ForAll("optionalStrings") String aiBaseUrl,
            @ForAll("optionalStrings") String aiApiKey,
            @ForAll("optionalStrings") String aiPrompt) {

        UITestRobot.TestConfig original = new UITestRobot.TestConfig(
                connectionName, databaseType, host, port, databaseName, username, password,
                query, ddlScriptPath, aiProvider, aiModel, aiBaseUrl, aiApiKey, aiPrompt);

        String json = GSON.toJson(original);
        UITestRobot.TestConfig parsed = UITestRobot.parseConfigFromJson(json);

        assertEquals(original, parsed,
                "Round-trip serialization/deserialization should produce equal TestConfig");
    }

    // -------------------------------------------------------------------------
    // Property 2: Invalid config rejection
    // -------------------------------------------------------------------------

    // Feature: ui-test-robot, Property 2: Invalid config rejection
    // **Validates: Requirements 1.4**
    @Property(tries = 100)
    void invalidJsonIsRejected(
            @ForAll("invalidJsonStrings") String invalidJson) {

        assertThrows(IllegalArgumentException.class,
                () -> UITestRobot.parseConfigFromJson(invalidJson),
                "Invalid JSON should throw IllegalArgumentException: " + invalidJson);
    }

    @Provide
    Arbitrary<String> invalidJsonStrings() {
        Arbitrary<String> syntacticallyInvalid = Arbitraries.of(
                "",
                "{",
                "{ invalid }",
                "not json at all",
                "[1, 2, 3]",
                "null",
                "{ \"connectionName\": }",
                "{{{}}}",
                "\"just a string\""
        );

        // Valid JSON but missing one or more required fields
        Arbitrary<String> missingRequiredFields = Arbitraries.of(
                "{}",
                "{ \"connectionName\": \"test\" }",
                "{ \"connectionName\": \"test\", \"databaseType\": \"POSTGRESQL\" }",
                "{ \"connectionName\": \"test\", \"databaseType\": \"POSTGRESQL\", \"host\": \"localhost\" }",
                "{ \"connectionName\": \"test\", \"databaseType\": \"POSTGRESQL\", \"host\": \"localhost\", \"port\": 5432 }",
                "{ \"connectionName\": \"test\", \"databaseType\": \"POSTGRESQL\", \"host\": \"localhost\", \"port\": 5432, \"databaseName\": \"db\" }",
                "{ \"connectionName\": \"test\", \"databaseType\": \"POSTGRESQL\", \"host\": \"localhost\", \"port\": 5432, \"databaseName\": \"db\", \"username\": \"user\" }",
                "{ \"host\": \"localhost\", \"port\": 5432, \"databaseName\": \"db\", \"username\": \"user\", \"password\": \"pass\" }",
                "{ \"connectionName\": \"\", \"databaseType\": \"POSTGRESQL\", \"host\": \"localhost\", \"port\": 5432, \"databaseName\": \"db\", \"username\": \"user\", \"password\": \"pass\" }",
                "{ \"connectionName\": \"test\", \"databaseType\": \"POSTGRESQL\", \"host\": \"localhost\", \"port\": 0, \"databaseName\": \"db\", \"username\": \"user\", \"password\": \"pass\" }",
                "{ \"connectionName\": \"test\", \"databaseType\": \"POSTGRESQL\", \"host\": \"localhost\", \"port\": -1, \"databaseName\": \"db\", \"username\": \"user\", \"password\": \"pass\" }"
        );

        return Arbitraries.oneOf(syntacticallyInvalid, missingRequiredFields);
    }

    // -------------------------------------------------------------------------
    // Property 5: AI test cases skip when AI config is absent
    // -------------------------------------------------------------------------

    // Feature: ui-test-robot, Property 5: AI test cases skip when AI config is absent
    // **Validates: Requirements 21.2**
    @Property(tries = 100)
    void shouldSkipAIWhenProviderOrKeyMissing(
            @ForAll("nonEmptyStrings") String connectionName,
            @ForAll("dbTypes") String databaseType,
            @ForAll("nonEmptyStrings") String host,
            @ForAll @IntRange(min = 1, max = 65535) int port,
            @ForAll("nonEmptyStrings") String databaseName,
            @ForAll("nonEmptyStrings") String username,
            @ForAll("nonEmptyStrings") String password,
            @ForAll("aiProviderValues") String aiProvider,
            @ForAll("aiKeyValues") String aiApiKey) {

        UITestRobot.TestConfig config = new UITestRobot.TestConfig(
                connectionName, databaseType, host, port, databaseName, username, password,
                null, null, aiProvider, null, null, aiApiKey, null);

        boolean shouldSkip = UITestRobot.shouldSkipAI(config);

        boolean providerPresent = aiProvider != null && !aiProvider.trim().isEmpty();
        boolean keyPresent = aiApiKey != null && !aiApiKey.trim().isEmpty();

        if (providerPresent && keyPresent) {
            assertFalse(shouldSkip,
                    "shouldSkipAI should return false when both aiProvider and aiApiKey are present");
        } else {
            assertTrue(shouldSkip,
                    "shouldSkipAI should return true when aiProvider='" + aiProvider
                            + "' or aiApiKey='" + aiApiKey + "' is null/empty");
        }
    }

    // -------------------------------------------------------------------------
    // Property 3: Test report formatting
    // -------------------------------------------------------------------------

    // Feature: ui-test-robot, Property 3: Test report formatting
    // **Validates: Requirements 17.1, 17.2**
    @Property(tries = 100)
    void testReportFormattingContainsExpectedFields(
            @ForAll("nonEmptyStrings") String testName,
            @ForAll("testStatuses") UITestRobot.TestResult.Status status,
            @ForAll @IntRange(min = 0, max = 999999) int durationMs,
            @ForAll("nonEmptyStrings") String reason) {

        switch (status) {
            case PASS -> {
                String line = UITestRobot.TestReporter.formatPass(testName, durationMs);
                assertTrue(line.contains(testName),
                        "PASS line should contain test name: " + line);
                assertTrue(line.contains("[PASS]"),
                        "PASS line should contain [PASS]: " + line);
                assertTrue(line.contains(durationMs + "ms"),
                        "PASS line should contain duration: " + line);
            }
            case FAIL -> {
                String line = UITestRobot.TestReporter.formatFail(testName, durationMs, reason);
                assertTrue(line.contains(testName),
                        "FAIL line should contain test name: " + line);
                assertTrue(line.contains("[FAIL]"),
                        "FAIL line should contain [FAIL]: " + line);
                assertTrue(line.contains(durationMs + "ms"),
                        "FAIL line should contain duration: " + line);
                assertTrue(line.contains(reason),
                        "FAIL line should contain failure reason: " + line);
            }
            case SKIP -> {
                String line = UITestRobot.TestReporter.formatSkip(testName, reason);
                assertTrue(line.contains(testName),
                        "SKIP line should contain test name: " + line);
                assertTrue(line.contains("[SKIP]"),
                        "SKIP line should contain [SKIP]: " + line);
                assertTrue(line.contains(reason),
                        "SKIP line should contain reason: " + line);
            }
        }
    }

    @Provide
    Arbitrary<UITestRobot.TestResult.Status> testStatuses() {
        return Arbitraries.of(UITestRobot.TestResult.Status.values());
    }

    // -------------------------------------------------------------------------
    // Property 4: Test summary and exit code correctness
    // -------------------------------------------------------------------------

    // Feature: ui-test-robot, Property 4: Test summary and exit code correctness
    // **Validates: Requirements 17.3, 17.4**
    @Property(tries = 100)
    void testSummaryCountsAndExitCode(
            @ForAll("testResultLists") java.util.List<UITestRobot.TestResult> resultList) {

        UITestRobot.TestReporter reporter = new UITestRobot.TestReporter();

        for (UITestRobot.TestResult result : resultList) {
            switch (result.getStatus()) {
                case PASS -> reporter.reportPass(result.getName(), result.getDurationMs());
                case FAIL -> reporter.reportFail(result.getName(), result.getDurationMs(), result.getFailureReason());
                case SKIP -> reporter.reportSkip(result.getName(), result.getFailureReason());
            }
        }

        java.util.List<UITestRobot.TestResult> collected = reporter.getResults();

        assertEquals(resultList.size(), collected.size(),
                "Total results should match input list size");

        long expectedPassed = resultList.stream()
                .filter(r -> r.getStatus() == UITestRobot.TestResult.Status.PASS).count();
        long expectedFailed = resultList.stream()
                .filter(r -> r.getStatus() == UITestRobot.TestResult.Status.FAIL).count();
        long expectedSkipped = resultList.stream()
                .filter(r -> r.getStatus() == UITestRobot.TestResult.Status.SKIP).count();

        long actualPassed = collected.stream()
                .filter(r -> r.getStatus() == UITestRobot.TestResult.Status.PASS).count();
        long actualFailed = collected.stream()
                .filter(r -> r.getStatus() == UITestRobot.TestResult.Status.FAIL).count();
        long actualSkipped = collected.stream()
                .filter(r -> r.getStatus() == UITestRobot.TestResult.Status.SKIP).count();

        assertEquals(expectedPassed, actualPassed, "Passed count mismatch");
        assertEquals(expectedFailed, actualFailed, "Failed count mismatch");
        assertEquals(expectedSkipped, actualSkipped, "Skipped count mismatch");

        int exitCode = reporter.getExitCode();
        if (expectedFailed == 0) {
            assertEquals(0, exitCode, "Exit code should be 0 when no tests failed");
        } else {
            assertEquals(1, exitCode, "Exit code should be 1 when any test failed");
        }
    }

    @Provide
    Arbitrary<java.util.List<UITestRobot.TestResult>> testResultLists() {
        Arbitrary<UITestRobot.TestResult> resultArb = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30),
                Arbitraries.of(UITestRobot.TestResult.Status.values()),
                Arbitraries.longs().between(0, 999999),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)
        ).as((name, status, duration, reason) ->
                new UITestRobot.TestResult(name, status, duration,
                        status == UITestRobot.TestResult.Status.PASS ? null : reason)
        );
        return resultArb.list().ofMinSize(1).ofMaxSize(20);
    }

    // -------------------------------------------------------------------------
    // Property 5: AI test cases skip when AI config is absent
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> aiProviderValues() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.of("OpenAI", "Anthropic", "Google", "Custom")
        );
    }

    @Provide
    Arbitrary<String> aiKeyValues() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(40)
        );
    }
}
