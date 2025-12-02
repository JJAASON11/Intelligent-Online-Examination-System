package com.exam.paper.controller;

import com.exam.common.api.ApiResponse;
import com.exam.paper.model.Paper;
import com.exam.paper.service.PaperService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CreatePaperRequest { public String name; public String subject; public Integer durationMin; public Map<String,Integer> strategy; public Map<String,Integer> typeScores; }
class AssembleRequest { public Map<String,Integer> strategy; public Integer perScore; }

@RestController
@RequestMapping("/api/papers")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class PaperController {
  private final PaperService paperService;
  public PaperController(PaperService paperService) { this.paperService = paperService; }

  @PostMapping
  public ApiResponse<Long> create(@RequestBody CreatePaperRequest req) {
    Paper p = new Paper();
    p.setName(req.name);
    p.setTotalScore(0);
    p.setDurationMin(req.durationMin != null ? req.durationMin : 60);
    p.setSubject(req.subject != null ? req.subject : "通用");
    Long id = paperService.createPaper(p);
    return ApiResponse.ok(id);
  }

  @GetMapping
  public ApiResponse<List<Paper>> list() { return ApiResponse.ok(paperService.listPapers()); }

  @PostMapping("/{paperId}/assemble")
  public ApiResponse<String> assemble(@PathVariable Long paperId, @RequestBody AssembleRequest req) {
    Map<String,Integer> st = req.strategy != null ? req.strategy : new HashMap<>();
    int perScore = req.perScore != null && req.perScore > 0 ? req.perScore : 1;
    String err = paperService.assemble(paperId, st, perScore);
    if (err != null) return ApiResponse.error(40001, err);
    return ApiResponse.ok("ok");
  }

  @PostMapping("/create-and-assemble")
  public ApiResponse<Long> createAndAssemble(@RequestBody CreatePaperRequest req) {
    Paper p = new Paper();
    p.setName(req.name);
    p.setTotalScore(0);
    p.setDurationMin(req.durationMin != null ? req.durationMin : 60);
    p.setSubject(req.subject != null ? req.subject : "通用");
    Long id = paperService.createPaper(p);
    String err = paperService.assembleWithTypeScores(id, req.strategy != null ? req.strategy : new HashMap<>(), req.typeScores);
    if (err != null) return ApiResponse.error(40001, err);
    return ApiResponse.ok(id);
  }

  @PostMapping("/{paperId}/assemble-type-scores")
  public ApiResponse<String> assembleTypeScores(@PathVariable Long paperId, @RequestBody CreatePaperRequest req) {
    String err = paperService.regenerateWithTypeScores(paperId,
      req.strategy != null ? req.strategy : new HashMap<>(),
      req.typeScores);
    if (err != null) return ApiResponse.error(40001, err);
    return ApiResponse.ok("ok");
  }

  @DeleteMapping("/{paperId}")
  public ApiResponse<String> delete(@PathVariable Long paperId) {
    boolean ok = paperService.deletePaper(paperId);
    return ok ? ApiResponse.ok("ok") : ApiResponse.error(40400, "试卷不存在或删除失败");
  }

  @PutMapping("/{paperId}/question/{questionId}/score")
  public ApiResponse<String> updateScore(@PathVariable Long paperId, @PathVariable Long questionId, @RequestParam Integer score) {
    boolean ok = paperService.updateQuestionScore(paperId, questionId, score);
    return ok ? ApiResponse.ok("ok") : ApiResponse.error(40002, "更新分值失败");
  }

  @GetMapping("/{paperId}/preview")
  public ApiResponse<List<com.exam.bank.model.Question>> preview(@PathVariable Long paperId) {
    return ApiResponse.ok(paperService.preview(paperId));
  }
}
