package com.orangehrm.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads and exposes framework configuration from src/test/resources/config.properties.
 * Implemented as a lazily-initialized singleton so the properties file is read only once.
 */
public class ConfigReader {

    private static final String CONFIG_PATH = "src/test/resources/config.properties";
    private static Properties properties;

    private ConfigReader() {
        // utility class - prevent instantiation
    }

    private static void load() {
        if (properties == null) {
            properties = new Properties();
            try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
                properties.load(fis);
            } catch (IOException e) {
                throw new RuntimeException("Unable to load config.properties from " + CONFIG_PATH, e);
            }
        }
    }

    public static String get(String key) {
        load();
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing property '" + key + "' in config.properties");
        }
        return value.trim();
    }

    public static String get(String key, String defaultValue) {
        load();
        return properties.getProperty(key, defaultValue).trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }
}
