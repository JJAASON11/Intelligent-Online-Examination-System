package com.exam.grading.controller;

import com.exam.common.api.ApiResponse;
import com.exam.grading.service.DoubaoAIService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI错题解答Controller
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {
    
    private final DoubaoAIService doubaoAIService;
    
    public AIController(DoubaoAIService doubaoAIService) {
        this.doubaoAIService = doubaoAIService;
    }
    
    /**
     * 解答错题
     * 学生和教师都可以使用
     */
    @PostMapping("/explain-wrong-answer")
    public ApiResponse<String> explainWrongAnswer(@RequestBody ExplainRequest req) {
        try {
            String explanation = doubaoAIService.explainWrongAnswer(
                req.questionStem,
                req.studentAnswer,
                req.correctAnswer,
                req.questionType
            );
            return ApiResponse.ok(explanation);
        } catch (Exception e) {
            return ApiResponse.error(50000, "AI解答失败：" + e.getMessage());
        }
    }
    
    /**
     * 简单问答（用于测试API连接）
     */
    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<String> askQuestion(@RequestBody AskRequest req) {
        try {
            String answer = doubaoAIService.askQuestion(req.question);
            return ApiResponse.ok(answer);
        } catch (Exception e) {
            return ApiResponse.error(50000, "AI问答失败：" + e.getMessage());
        }
    }

    /**
     * 聊天测试（任何已登录用户可用，用于快速验证豆包连通性）
     */
    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody AskRequest req) {
        try {
            String q = (req == null || req.question == null) ? "" : req.question.trim();
            if (q.isEmpty()) {
                return ApiResponse.error(40000, "请输入内容");
            }
            String answer = doubaoAIService.askQuestion(q);
            return ApiResponse.ok(answer);
        } catch (Exception e) {
            return ApiResponse.error(50000, "AI聊天失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查AI服务配置状态
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Map<String, Object>> checkStatus() {
        Map<String, Object> status = new HashMap<>();
        // 这里可以检查配置是否完整
        status.put("configured", true);
        status.put("message", "AI服务已就绪，请确保已配置API密钥");
        return ApiResponse.ok(status);
    }
    
    static class ExplainRequest {
        public String questionStem;      // 题目内容
        public String studentAnswer;     // 学生答案
        public String correctAnswer;     // 正确答案
        public String questionType;      // 题目类型（SINGLE, MULTI, JUDGE, FILL, SHORT, CODE）
    }
    
    static class AskRequest {
        public String question;
    }
}

