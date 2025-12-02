package com.exam.grading.controller;

import com.exam.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

class AutoGradeRequest { public String answer; public String standard; public Integer score; }
class AutoGradeResult { public Integer autoScore; public AutoGradeResult(Integer s){ this.autoScore = s; } }

@RestController
@RequestMapping("/api/grading")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class GradingController {
  @PostMapping("/auto")
  public ApiResponse<AutoGradeResult> auto(@RequestBody AutoGradeRequest req) {
    int result = (req.standard != null && req.standard.equals(req.answer)) ? (req.score == null ? 0 : req.score) : 0;
    return ApiResponse.ok(new AutoGradeResult(result));
  }
}
