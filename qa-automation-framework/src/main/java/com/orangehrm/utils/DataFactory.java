package com.orangehrm.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime test-data factory for {@link EmployeeData}.
 *
 * <h3>Uniqueness Strategy</h3>
 * <ul>
 *   <li><strong>First name</strong>: chosen randomly from a configurable pool
 *       ({@code testdata.firstname.pool} in {@code testdata.properties}), making
 *       report output human-readable while avoiding repetition.</li>
 *   <li><strong>Last name</strong>: {@code Test} + the last 6 hex digits of a UUID,
 *       e.g. {@code Test3fa2c1}. A UUID suffix survives parallel JVM forks because
 *       it is process-independent — unlike a plain timestamp counter.</li>
 *   <li><strong>Employee ID</strong>: configurable prefix (default {@code A}) +
 *       8-digit zero-padded counter seeded from the current millisecond clock mod
 *       99&nbsp;000&nbsp;000, then incremented atomically per call.
 *       The counter stays within OrangeHRM's 9-character employee-ID field
 *       ({@code A} + 8 digits = 9 chars).</li>
 * </ul>
 *
 * <h3>Data Separation</h3>
 * <ul>
 *   <li>Job title and employment status are <em>not</em> hardcoded; they are read
 *       from {@code testdata.properties} ({@code testdata.employee.updatedJobTitle},
 *       {@code testdata.employee.updatedEmploymentStatus}).</li>
 *   <li>{@link #fromJson()} always overwrites the template's firstName/lastName/employeeId
 *       with runtime-unique values, so the JSON file can never cause collisions.</li>
 * </ul>
 *
 * <h3>Parallel Safety</h3>
 * No static mutable employee data is shared across tests.  Each {@link #randomEmployee()}
 * call returns an independent object; the only shared state is the atomic counter and the
 * immutable name-pool array.
 */
public class DataFactory {

    private static final Logger log = LogManager.getLogger(DataFactory.class);

    // ── Uniqueness counter ─────────────────────────────────────────────────────
    // Seeded well away from zero to make IDs visually distinct from system records.
    private static final AtomicLong COUNTER =
            new AtomicLong((System.currentTimeMillis() % 99_000_000L) + 1_000_000L);

    // ── Test-data properties (testdata.properties) ─────────────────────────────
    private static final Properties TESTDATA_PROPS = loadTestDataProps();

    // ── Name pool ──────────────────────────────────────────────────────────────
    private static final List<String> FIRST_NAME_POOL = buildFirstNamePool();

    private DataFactory() {
        // utility class — not instantiable
    }

    // ── Public factory methods ─────────────────────────────────────────────────

    /**
     * Returns a new {@link EmployeeData} with fully unique, runtime-generated values.
     *
     * <p>Safe to call concurrently from multiple threads/test methods.</p>
     */
    public static EmployeeData randomEmployee() {
        long seq = COUNTER.getAndIncrement();
        String uuidSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String prefix = testdataProp("testdata.employeeid.prefix", "A");

        // OrangeHRM employee ID field is 9 chars max: 1-char prefix + 8 digits.
        String employeeId = String.format("%s%08d", prefix, seq % 99_000_000L);

        EmployeeData data = new EmployeeData();
        data.setFirstName(randomFirstName());
        data.setLastName("Test" + uuidSuffix);
        data.setEmployeeId(employeeId);
        data.setProfilePicture(ConfigReader.get("image.profile.path", "src/test/resources/images/profile.png"));
        data.setUpdatedJobTitle(testdataProp("testdata.employee.updatedJobTitle", "Software Engineer"));
        data.setUpdatedEmploymentStatus(testdataProp("testdata.employee.updatedEmploymentStatus", "Full-Time Permanent"));

        log.debug("[DataFactory] Generated employee: id={} name='{} {}'",
                employeeId, data.getFirstName(), data.getLastName());
        return data;
    }

    /**
     * Loads the JSON template from {@code testdata/employee.json} and enriches it
     * with runtime-unique values (firstName, lastName, employeeId).
     *
     * <p><strong>The template file is never returned as-is</strong> — this method
     * always overwrites the three identity fields so the JSON file cannot cause
     * ID collisions between tests or across parallel runs.</p>
     *
     * <p>Use this when test-specific defaults (e.g., profile picture path, job title)
     * should come from the JSON file rather than code.</p>
     */
    public static EmployeeData fromJson() {
        String path = testdataProp("testdata.employee.path",
                ConfigReader.get("testdata.employee.path", "testdata/employee.json"));
        EmployeeData template = JsonDataReader.readData(path, EmployeeData.class);

        // Always overwrite identity fields with runtime-unique values.
        // The JSON template is a defaults carrier, not a static record.
        EmployeeData unique = randomEmployee();
        template.setFirstName(unique.getFirstName());
        template.setLastName(unique.getLastName());
        template.setEmployeeId(unique.getEmployeeId());

        log.debug("[DataFactory] Enriched JSON template: id={} name='{} {}'",
                template.getEmployeeId(), template.getFirstName(), template.getLastName());
        return template;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Picks a random first name from the configured pool. */
    private static String randomFirstName() {
        return FIRST_NAME_POOL.get(ThreadLocalRandom.current().nextInt(FIRST_NAME_POOL.size()));
    }

    /** Reads a property from testdata.properties, falling back to a default. */
    private static String testdataProp(String key, String defaultValue) {
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String val = TESTDATA_PROPS.getProperty(key);
        return (val != null && !val.isBlank()) ? val.trim() : defaultValue;
    }

    /**
     * Loads {@code testdata.properties} from the classpath once at class-load time.
     * Returns an empty Properties object (not null) if the file is absent, so all
     * callers fall back to their coded defaults gracefully.
     */
    private static Properties loadTestDataProps() {
        Properties props = new Properties();
        try (InputStream is = DataFactory.class.getClassLoader()
                .getResourceAsStream("testdata.properties")) {
            if (is != null) {
                props.load(is);
                LogManager.getLogger(DataFactory.class)
                        .debug("[DataFactory] Loaded testdata.properties from classpath");
            } else {
                LogManager.getLogger(DataFactory.class)
                        .warn("[DataFactory] testdata.properties not found on classpath; " +
                              "all test-data values will use coded defaults.");
            }
        } catch (IOException e) {
            LogManager.getLogger(DataFactory.class)
                    .warn("[DataFactory] Failed to read testdata.properties: {}", e.getMessage());
        }
        return props;
    }

    /** Builds the first-name pool from testdata.properties or uses the built-in default. */
    private static List<String> buildFirstNamePool() {
        String poolStr = new Properties() {{
            // Re-use already-loaded props without re-reading the file
        }}.getProperty("testdata.firstname.pool");

        // Read from the already-loaded TESTDATA_PROPS (after static init order is safe)
        // We resolve lazily via the static block sequence; safe because TESTDATA_PROPS
        // is initialised before FIRST_NAME_POOL in the static field declaration order.
        String configured = TESTDATA_PROPS.getProperty("testdata.firstname.pool");
        String sysProp    = System.getProperty("testdata.firstname.pool");
        String raw = (sysProp != null && !sysProp.isBlank()) ? sysProp
                   : (configured != null && !configured.isBlank()) ? configured
                   : "Alex,Jordan,Morgan,Taylor,Casey,Riley,Quinn,Avery,Blake,Drew";

        return Arrays.asList(raw.split(","));
    }
}
