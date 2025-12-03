package com.exam.bank.controller;

import com.exam.bank.model.Question;
import com.exam.bank.service.QuestionService;
import com.exam.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class QuestionController {
  private final QuestionService service;
  public QuestionController(QuestionService service) { this.service = service; }

  @GetMapping
  public ApiResponse<java.util.Map<String,Object>> list(@RequestParam(defaultValue = "1") int page,

                                          @RequestParam(defaultValue = "10") int size) {
    System.out.println(1111);
    Page<Question> p = service.page(new Page<>(page, size));
    java.util.Map<String,Object> resp = new java.util.HashMap<>();
    resp.put("records", p.getRecords());
    resp.put("total", p.getTotal());
    resp.put("page", page);
    resp.put("size", size);
    return ApiResponse.ok(resp);
  }

  @PostMapping
  public ApiResponse<Integer> create(@RequestBody Question q) { return ApiResponse.ok(service.create(q)); }

  @PutMapping("/{id}")
  public ApiResponse<Integer> update(@PathVariable Long id, @RequestBody Question q) {
    q.setId(id);
    return ApiResponse.ok(service.update(q));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Integer> delete(@PathVariable Long id) { return ApiResponse.ok(service.delete(id)); }
}
