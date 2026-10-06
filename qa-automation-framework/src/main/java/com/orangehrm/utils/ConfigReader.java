package com.orangehrm.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads framework configuration in strict priority order:
 * <ol>
 *   <li><strong>System property</strong> (e.g. {@code -Dheadless=true}, {@code -Dbrowser=chrome}, {@code -Denv=qa})</li>
 *   <li><strong>Environment variable</strong> (e.g. {@code ORANGEHRM_USERNAME}, {@code ORANGEHRM_PASSWORD}, {@code BASE_URL})</li>
 *   <li><strong>Selected environment property file</strong> ({@code config-<env>.properties}, default {@code env=qa})</li>
 *   <li><strong>Default value</strong></li>
 * </ol>
 */
public class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    private static volatile Properties envProperties;
    private static volatile Properties baseProperties;
    private static String activeEnv;

    private ConfigReader() {
        // utility class
    }

    private static synchronized void load() {
        if (envProperties != null) return;

        // Resolve active environment: 1. SysProp, 2. EnvVar, 3. default "qa"
        activeEnv = System.getProperty("env");
        if (activeEnv == null || activeEnv.isBlank()) {
            activeEnv = System.getenv("ENV");
        }
        if (activeEnv == null || activeEnv.isBlank()) {
            activeEnv = "qa";
        }
        activeEnv = activeEnv.trim().toLowerCase();

        envProperties = new Properties();
        String envFileName = "config-" + activeEnv + ".properties";
        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream(envFileName)) {
            if (is != null) {
                envProperties.load(is);
                log.info("Loaded environment configuration from {}", envFileName);
            } else {
                log.warn("Environment file '{}' not found on classpath; falling back to config.properties", envFileName);
            }
        } catch (IOException e) {
            log.warn("Failed to load '{}': {}", envFileName, e.getMessage());
        }

        baseProperties = new Properties();
        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                baseProperties.load(is);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Resolves the value for key following the 4-tier priority order:
     * 1. System property
     * 2. Environment variable (including mapped ORANGEHRM_* variables)
     * 3. Environment property file (config-<env>.properties / config.properties)
     * 4. Throws RuntimeException if not found
     */
    public static String get(String key) {
        // 1. System property
        String sysVal = System.getProperty(key);
        if (sysVal != null && !sysVal.isBlank()) {
            return sysVal.trim();
        }

        // 2. Environment variable
        String envVal = resolveEnvVar(key);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }

        // 3. Selected environment property file
        load();
        String propVal = envProperties.getProperty(key);
        if (propVal == null || propVal.isBlank()) {
            propVal = baseProperties.getProperty(key);
        }

        if (propVal != null && !propVal.isBlank()) {
            return propVal.trim();
        }

        throw new RuntimeException(
                "Missing configuration property '" + key + "'. " +
                "Configure it via -D" + key + ", environment variable, or in config-" + activeEnv + ".properties.");
    }

    /** Like {@link #get(String)} but returns defaultValue instead of throwing. */
    public static String get(String key, String defaultValue) {
        try {
            return get(key);
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key, "false"));
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(get(key));
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    public static String getActiveEnvironment() {
        load();
        return activeEnv;
    }

    private static String resolveEnvVar(String key) {
        // Dedicated credential mappings
        if ("login.username".equalsIgnoreCase(key)) {
            String val = System.getenv("ORANGEHRM_USERNAME");
            if (val != null && !val.isBlank()) return val;
        }
        if ("login.password".equalsIgnoreCase(key)) {
            String val = System.getenv("ORANGEHRM_PASSWORD");
            if (val != null && !val.isBlank()) return val;
        }
        if ("ess.username".equalsIgnoreCase(key)) {
            String val = System.getenv("ORANGEHRM_ESS_USERNAME");
            if (val != null && !val.isBlank()) return val;
        }
        if ("ess.password".equalsIgnoreCase(key)) {
            String val = System.getenv("ORANGEHRM_ESS_PASSWORD");
            if (val != null && !val.isBlank()) return val;
        }

        // Direct key check
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) return val;

        // Uppercase underscore convention (e.g. base.url -> BASE_URL, headless -> HEADLESS)
        String envKey = key.replace('.', '_').replace('-', '_').toUpperCase();
        return System.getenv(envKey);
    }
}
