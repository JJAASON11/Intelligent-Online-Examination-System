package com.exam.grading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 豆包AI服务 - 用于解答错题
 */
@Service
public class DoubaoAIService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 兜底：如果未读取到配置，则使用内置默认密钥（按你的要求直接写入代码）
    private static final String DEFAULT_API_KEY = "e9d219b6-eb17-4fd4-b544-47c659a88657";
    
    @Value("${doubao.api-key:}")
    private String apiKey;
    
    @Value("${doubao.model:doubao-seed-1-8-251228}")
    private String model;
    
    @Value("${doubao.api-url:https://ark.cn-beijing.volces.com/api/v3/responses}")
    private String apiUrl;
    
    public DoubaoAIService() {
        // 配置超时时间，避免前端长时间等待
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 之前 5s/8s 在校园网/代理环境下很容易误判“超时”，这里放宽到更合理的默认值
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 调用豆包API解答错题
     * @param questionStem 题目内容
     * @param studentAnswer 学生答案
     * @param correctAnswer 正确答案
     * @param questionType 题目类型
     * @return AI解答内容
     */
    public String explainWrongAnswer(String questionStem, String studentAnswer, String correctAnswer, String questionType) {
        // 检查配置
        String key = (apiKey == null || apiKey.isEmpty()) ? DEFAULT_API_KEY : apiKey;
        
        // 构建提示词
        String systemPrompt = "你是一位耐心的老师，专门帮助学生理解错题。请用简洁、易懂的方式解释为什么学生的答案是错误的，并说明正确答案的思路。";
        
        String userPrompt = buildPrompt(questionStem, studentAnswer, correctAnswer, questionType);
        
        // 构建请求体 - 使用豆包API的正确格式
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        // input数组格式
        java.util.List<Map<String, Object>> inputList = new java.util.ArrayList<>();
        
        // 系统提示词
        Map<String, Object> systemContent = new HashMap<>();
        systemContent.put("type", "input_text");
        systemContent.put("text", systemPrompt);
        
        Map<String, Object> systemInput = new HashMap<>();
        systemInput.put("role", "system");
        systemInput.put("content", new Object[]{systemContent});
        inputList.add(systemInput);
        
        // 用户问题
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("type", "input_text");
        userContent.put("text", userPrompt);
        
        Map<String, Object> userInput = new HashMap<>();
        userInput.put("role", "user");
        userInput.put("content", new Object[]{userContent});
        inputList.add(userInput);
        
        requestBody.put("input", inputList);
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + key);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            // 发送请求
            String url = (apiUrl == null || apiUrl.isEmpty())
                ? "https://ark.cn-beijing.volces.com/api/v3/responses"
                : apiUrl;
            @SuppressWarnings("null")
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 解析响应 - 豆包API的响应格式
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String text = extractAssistantText(jsonNode);
                if (text != null && !text.isEmpty()) return text;

                // 如果都不匹配，返回原始响应用于调试
                return "AI返回格式异常，原始响应：" + response.getBody();
            } else {
                return "调用豆包API失败，状态码：" + response.getStatusCode() + "，响应：" + response.getBody();
            }
        } catch (Exception e) {
            String msg = buildErrorMessage(e);
            return "调用豆包API时发生错误：" + msg;
        }
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String questionStem, String studentAnswer, String correctAnswer, String questionType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("题目：").append(questionStem).append("\n\n");
        prompt.append("题目类型：").append(questionType).append("\n\n");
        prompt.append("学生的答案：").append(studentAnswer != null ? studentAnswer : "未作答").append("\n\n");
        prompt.append("正确答案：").append(correctAnswer != null ? correctAnswer : "暂无标准答案").append("\n\n");
        prompt.append("请分析学生答案错误的原因，并给出详细的解答思路，帮助学生理解正确的解题方法。");
        return prompt.toString();
    }
    
    /**
     * 简单问答（用于测试）
     */
    public String askQuestion(String question) {
        String key = (apiKey == null || apiKey.isEmpty()) ? DEFAULT_API_KEY : apiKey;
        
        // 构建请求体 - 使用豆包API的正确格式
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        // input数组格式
        java.util.List<Map<String, Object>> inputList = new java.util.ArrayList<>();
        
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("type", "input_text");
        userContent.put("text", question);
        
        Map<String, Object> userInput = new HashMap<>();
        userInput.put("role", "user");
        userInput.put("content", new Object[]{userContent});
        inputList.add(userInput);
        
        requestBody.put("input", inputList);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + key);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            String url = (apiUrl == null || apiUrl.isEmpty())
                ? "https://ark.cn-beijing.volces.com/api/v3/responses"
                : apiUrl;
            @SuppressWarnings("null")
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String text = extractAssistantText(jsonNode);
                if (text != null && !text.isEmpty()) return text;

                return "AI返回格式异常，原始响应：" + response.getBody();
            } else {
                return "调用失败：" + response.getStatusCode() + "，响应：" + response.getBody();
            }
        } catch (Exception e) {
            String msg = buildErrorMessage(e);
            return "调用错误：" + msg;
        }
    }

    /**
     * 从 Ark /responses 返回中提取 assistant 的文本。
     *
     * 典型结构（你提供的原始响应）：
     * output: [
     *   { type: "reasoning", ... },
     *   { type: "message", role: "assistant", content: [{ type: "output_text", text: "..." }] }
     * ]
     */
    private String extractAssistantText(JsonNode root) {
        if (root == null) return null;

        // 新版 /responses：从 output 数组里找 type=message 的 assistant 输出
        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                String type = item.path("type").asText("");
                if (!"message".equals(type)) continue;
                String role = item.path("role").asText("");
                if (!role.isEmpty() && !"assistant".equals(role)) continue;

                JsonNode content = item.get("content");
                if (content != null && content.isArray()) {
                    for (JsonNode c : content) {
                        String cType = c.path("type").asText("");
                        if ("output_text".equals(cType) || "text".equals(cType) || "output".equals(cType)) {
                            String text = c.path("text").asText(null);
                            if (text != null && !text.isEmpty()) return text;
                        }
                        if (c.isTextual()) {
                            String text = c.asText();
                            if (text != null && !text.isEmpty()) return text;
                        }
                    }
                }

                // 兜底：部分实现可能直接给 message.text
                String msgText = item.path("text").asText(null);
                if (msgText != null && !msgText.isEmpty()) return msgText;
            }
        }

        // 兼容 OpenAI ChatCompletions 风格 choices[0].message.content
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    if (content.isTextual()) return content.asText();
                    // 兼容 content 数组
                    if (content.isArray() && content.size() > 0) {
                        String text = content.get(0).path("text").asText(null);
                        if (text != null && !text.isEmpty()) return text;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 统一构造错误提示，区分超时/连接错误
     */
    private String buildErrorMessage(Exception e) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException he = (HttpClientErrorException) e;
            String body = he.getResponseBodyAsString();
            if (body != null && !body.isEmpty()) {
                return String.format("HTTP %s：%s", he.getStatusCode(), body);
            }
            return String.format("HTTP %s", he.getStatusCode());
        }
        if (e instanceof ResourceAccessException) {
            Throwable cause = e.getCause();
            if (cause != null) {
                String c = cause.getClass().getSimpleName();
                String m = cause.getMessage();
                if (c.contains("SocketTimeout") || c.contains("Timeout")) {
                    return "调用超时，请检查网络或稍后重试（" + c + (m != null ? (": " + m) : "") + "）";
                }
                return "网络访问失败（" + c + (m != null ? (": " + m) : "") + "）";
            }
            String m = e.getMessage();
            return m == null ? e.toString() : m;
        }
        String m = e.getMessage();
        return m == null ? e.toString() : m;
    }
}

