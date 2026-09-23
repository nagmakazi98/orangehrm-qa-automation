package com.orangehrm.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Generic JSON test-data reader built on Jackson.
 * Keeps test data decoupled from test logic (data-driven approach).
 */
public class JsonDataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonDataReader() {
    }

    public static <T> T readData(String filePath, Class<T> clazz) {
        try {
            return MAPPER.readValue(new File(filePath), clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read/parse JSON test data at " + filePath, e);
        }
    }
}
