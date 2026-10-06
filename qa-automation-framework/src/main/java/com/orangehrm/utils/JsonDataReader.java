package com.orangehrm.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generic JSON test-data reader backed by Jackson.
 *
 * <p>Supports loading both from the <strong>classpath</strong> and from
 * <strong>file-system paths</strong>, making it resilient across IDE, Maven, and CI.</p>
 */
public class JsonDataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonDataReader() {
        // utility class
    }

    /**
     * Reads a JSON file from the classpath or filesystem and maps it to the given POJO type.
     */
    public static <T> T readData(String path, Class<T> clazz) {
        String cleanPath = path;
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        if (cleanPath.startsWith("src/test/resources/")) {
            cleanPath = cleanPath.substring("src/test/resources/".length());
        }

        // Try classpath first
        InputStream is = JsonDataReader.class.getClassLoader().getResourceAsStream(cleanPath);

        // Fallback to direct file system
        if (is == null) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    is = new FileInputStream(file);
                } catch (IOException ignored) {
                }
            }
        }

        if (is == null) {
            throw new RuntimeException("Test data file not found on classpath or filesystem: " + path);
        }

        try (InputStream in = is) {
            return MAPPER.readValue(in, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON test data from: " + path, e);
        }
    }
}
