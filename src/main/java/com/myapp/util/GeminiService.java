package com.myapp.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService {

    private static final String API_KEY = "REMOVED_API_KEY";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public static String analyze(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String body = "{"
                + "\"model\": \"llama-3.1-8b-instant\","
                + "\"messages\": ["
                + "  {\"role\": \"user\", \"content\": " + escapeJson(prompt) + "}"
                + "],"
                + "\"temperature\": 0.4,"
                + "\"max_tokens\": 1024"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("STATUS: " + response.statusCode());
        System.out.println("RAW BODY: " + response.body());
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

    private static String extractText(String json) {
        // Find the content field
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start == -1) {
            marker = "\"content\": \"";
            start = json.indexOf(marker);
        }
        if (start == -1) return "No response from AI.";

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

        String result = json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

        result = result.replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\u0027", "'")
                .replace("\\u0022", "\"")
                .replace("\\u0026", "&");

        // Strip markdown code fences if AI wraps response in ```html or ```
        result = result.trim();
        if (result.startsWith("```html")) {
            result = result.substring(7);
        } else if (result.startsWith("```")) {
            result = result.substring(3);
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }

        System.out.println("EXTRACTED TEXT: " + result);

        return result.trim();
    }

}