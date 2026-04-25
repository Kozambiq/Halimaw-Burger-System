package com.myapp.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService {

    private static final String API_KEY = EnvLoader.get("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY_HERE");
    private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    public static String analyze(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String body = "{"
            + "\"contents\": [{"
            + "  \"parts\": [{"
            + "    \"text\": " + escapeJson(prompt)
            + "  }]"
            + "}],"
            + "\"generationConfig\": {"
            + "  \"temperature\": 0.4,"
            + "  \"maxOutputTokens\": 1024"
            + "}"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return extractText(response.body());
    }

    private static String escapeJson(String text) {
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            + "\"";
    }

    // Simple text extractor — no JSON library needed
    private static String extractText(String json) {
        // Gemini response: find "text": "..." in the JSON
        String marker = "\"text\": \"";
        int start = json.indexOf(marker);
        if (start == -1) {
            marker = "\"text\":\"";
            start = json.indexOf(marker);
        }
        if (start == -1) return "No response from Gemini.";

        start += marker.length();
        int end = start;
        boolean escaped = false;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            }
            end++;
        }

        return json.substring(start, end)
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}
