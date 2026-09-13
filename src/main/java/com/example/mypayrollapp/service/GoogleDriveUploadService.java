package com.example.mypayrollapp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleDriveUploadService {

    // Link Web App Google Apps Script của bạn
    private final String GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyX0rdLIr0bgmRdltB3PBhvKX6XNBskLgTPEwukZ2lm500MBmATvhUTMxuxgy6ciO-v3g/exec";

    // Client HTTP xử lý tự động chuyển tiếp (302 Redirect của Google)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String uploadToDrive(MultipartFile file, String prefix, String idCardNumber) {
        if (file == null || file.isEmpty()) return "";
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String fileName = prefix + "_" + idCardNumber + "_" + file.getOriginalFilename();
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            // Đóng gói JSON gửi sang Google Script
            String jsonPayload = String.format(
                    "{\"fileName\":\"%s\",\"mimeType\":\"%s\",\"base64\":\"%s\"}",
                    escape(fileName),
                    escape(contentType),
                    base64
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_SCRIPT_URL))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body();
                // Bóc tách link xem ảnh Google Drive
                Pattern pattern = Pattern.compile("\"url\":\\s*\"(https[^\"]+)\"");
                Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    return matcher.group(1).replace("\\/", "/");
                }
            } else {
                System.err.println("Upload lên Google Drive thất bại, mã lỗi HTTP: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}