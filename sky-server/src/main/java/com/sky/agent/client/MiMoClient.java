package com.sky.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiMo-v2.5 API 客户端（Anthropic 协议）
 * 直接 HTTP 调用，绕过 langchain4j 的 thinking 枚举不支持问题
 */
@Slf4j
@Component
public class MiMoClient {

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public MiMoClient(@Value("${sky.mimo.api.base-url}") String baseUrl,
                      @Value("${sky.mimo.api.api-key}") String apiKey,
                      @Value("${sky.mimo.api.model-name}") String modelName) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    /**
     * 发送聊天请求，返回文本回复（不控制 thinking）
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, false);
    }

    /**
     * 发送聊天请求，返回文本回复
     * @param disableThinking 禁用思考模式（结构化提取任务建议禁用以提速）
     */
    public String chat(String systemPrompt, String userPrompt, boolean disableThinking) {
        try {
            Map<String, Object> body = buildRequestBody(systemPrompt, userPrompt, disableThinking);
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/messages", request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("MiMo API 返回错误 {}: {}", response.getStatusCode(), response.getBody());
                return "{}";
            }

            return extractText(response.getBody());
        } catch (Exception e) {
            log.error("MiMo API 调用失败", e);
            return "{}";
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt, boolean disableThinking) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("max_tokens", 4096);

        if (disableThinking) {
            Map<String, Object> thinking = new LinkedHashMap<>();
            thinking.put("type", "disabled");
            body.put("thinking", thinking);
        }

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        body.put("messages", messages);

        return body;
    }

    /** 从响应中提取 text 类型的内容（跳过 thinking 块） */
    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode block : content) {
                    if ("text".equals(block.get("type").asText())) {
                        return block.get("text").asText().trim();
                    }
                }
            }
            log.warn("MiMo 响应中未找到 text 内容: {}", responseBody);
            return "{}";
        } catch (Exception e) {
            log.error("解析 MiMo 响应失败: {}", responseBody, e);
            return "{}";
        }
    }
}
