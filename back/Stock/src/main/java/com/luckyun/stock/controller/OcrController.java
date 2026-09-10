package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SaCheckLogin
public class OcrController {

    @Value("${ocr.paddle-url}")
    private String paddleUrl;

    @Value("${ocr.paddle-path:/ocr}")
    private String paddlePath;

    @Value("${ocr.paddle-api-key:}")
    private String paddleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * OCR 文字识别接口
     * 接收图片，转 base64 后调用 PaddleOCR 服务，返回识别出的文本
     */
    @PostMapping("/ocr")
    public ResponseEntity<?> recognize(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "msg", "上传文件不能为空"));
        }

        try {
            // 图片转 base64
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());

            // 构造 JSON 请求体 {"base64_str": "..."}
            Map<String, String> reqBody = new HashMap<>();
            reqBody.put("base64_str", base64);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (paddleApiKey != null && !paddleApiKey.isBlank()) {
                headers.set("X-API-Key", paddleApiKey);
            }

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(reqBody), headers);
            String url = paddleUrl + paddlePath;

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("code", 500, "msg", "PaddleOCR 服务返回异常：" + response.getStatusCode()));
            }

            // 解析返回结果 {"resultcode":200,"data":[[[[x,y]...],["text",conf]]]]}
            JsonNode root = objectMapper.readTree(response.getBody());
            List<String> lines = new ArrayList<>();
            StringBuilder textBuilder = new StringBuilder();
            JsonNode data = root.has("data") ? root.get("data") : null;
            if (data != null && data.isArray()) {
                for (JsonNode group : data) {
                    if (group.isArray()) {
                        for (JsonNode item : group) {
                            // item: [[坐标], ["文字", 置信度]]
                            if (item.isArray() && item.size() > 1) {
                                JsonNode textPart = item.get(1);
                                if (textPart.isArray() && textPart.size() > 0) {
                                    String txt = textPart.get(0).asText("").trim();
                                    if (!txt.isEmpty()) {
                                        lines.add(txt);
                                        if (textBuilder.length() > 0) textBuilder.append("\n");
                                        textBuilder.append(txt);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Map<String, Object> respData = new HashMap<>();
            respData.put("text", textBuilder.toString());
            respData.put("lines", lines);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 200);
            resp.put("msg", "success");
            resp.put("data", respData);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("code", 500, "msg", "OCR 识别失败：" + e.getMessage()));
        }
    }
}
