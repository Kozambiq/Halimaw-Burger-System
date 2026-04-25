package com.myapp.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {

    private static final Map<String, String> env = new HashMap<>();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        
        Path path = Paths.get(".env");
        if (!Files.exists(path)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    env.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load .env file");
        }
        loaded = true;
    }

    public static String get(String key) {
        load();
        return env.get(key);
    }

    public static String get(String key, String defaultValue) {
        load();
        return env.getOrDefault(key, defaultValue);
    }
}