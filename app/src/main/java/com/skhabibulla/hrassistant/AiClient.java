package com.skhabibulla.hrassistant;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Optional OpenAI-compatible chat-completions client. No built-in account or subscription. */
final class AiClient {
    interface Callback { void onResult(String response, boolean success); }

    static void ask(String endpoint, String model, String apiKey, String message, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String result;
            boolean ok = false;
            try {
                URL url = new URL(endpoint);
                if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getHost().isEmpty() || url.getUserInfo() != null)
                    throw new Exception("Use an HTTPS AI endpoint without embedded credentials.");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(50000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                if (!apiKey.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                JSONObject req = new JSONObject();
                req.put("model", model);
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system")
                        .put("content", "You are HR HABIB AI, a helpful assistant. Answer in the language of the user. Be concise."));
                messages.put(new JSONObject().put("role", "user").put("content", message));
                req.put("messages", messages);
                byte[] data = req.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) { out.write(data); }
                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String raw = readLimited(stream, 1024 * 1024);
                if (status < 200 || status >= 300) throw new Exception("AI provider HTTP " + status + ": " + shortError(raw));
                JSONObject response = new JSONObject(raw);
                result = response.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").optString("content", "No reply.");
                ok = true;
            } catch (Exception e) {
                result = "AI request failed: " + e.getMessage();
            } finally {
                if (connection != null) connection.disconnect();
            }
            String finalResult = result;
            boolean finalOk = ok;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult, finalOk));
        }).start();
    }

    private static String readLimited(InputStream stream, int limit) throws Exception {
        if (stream == null) return "No details";
        try (InputStream input=stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf=new byte[4096]; int n;
            while ((n=input.read(buf))!=-1) {
                if (out.size()+n>limit) throw new Exception("Response too large");
                out.write(buf,0,n);
            }
            return out.toString("UTF-8");
        }
    }
    private static String shortError(String s) {
        if (s == null) return "Unknown error";
        return s.length() > 180 ? s.substring(0,180) : s;
    }
}
