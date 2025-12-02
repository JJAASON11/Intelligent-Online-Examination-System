package com.exam.exam.controller;

import com.exam.common.api.ApiResponse;
import com.exam.paper.service.PaperService;
import com.exam.exam.service.ExamService;
import com.exam.exam.model.ExamSession;
import com.exam.exam.model.AnswerRecord;
import com.exam.bank.model.Question;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

class CreateSessionRequest { public Long paperId; public String startAt; public String endAt; }
class CreateSessionResponse { public Long sessionId; public CreateSessionResponse(Long id){ this.sessionId = id; } }
class ExamEventRequest { public Long sessionId; public Long studentId; public String type; public String detail; }
class StartExamRequest { public Long sessionId; public Long studentId; public Long paperId; }
class SaveAnswerRequest { public Long sessionId; public Long studentId; public Long questionId; public String answerJson; }
class SessionSummary { public Long sessionId; public Long paperId; public String paperName; public String status; public Integer objective; public Integer subjective; public Integer total; }

@RestController
@RequestMapping("/api/exams")
public class ExamController {
  private final PaperService paperService;
  private final ExamService examService;
  public ExamController(PaperService paperService, ExamService examService) { this.paperService = paperService; this.examService = examService; }

  private static final Map<String, List<String>> EVENTS = new HashMap<>();
  private static final Map<String, Map<Long, String>> ANSWERS = new HashMap<>();

  @PostMapping("/sessions")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<CreateSessionResponse> createSession(@RequestBody CreateSessionRequest req) {
    long sid = System.currentTimeMillis();
    com.exam.exam.model.ExamSession s = new com.exam.exam.model.ExamSession();
    s.setId(sid);
    s.setPaperId(req.paperId);
    s.setStudentId(0L);
    s.setStatus("START");
    try {
      java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      if (req.startAt != null) s.setStartAt(java.time.LocalDateTime.parse(req.startAt, fmt));
      if (req.endAt != null) s.setEndAt(java.time.LocalDateTime.parse(req.endAt, fmt));
    } catch (Exception ignored) {}
    examService.createOrUpsertSession(s);
    return ApiResponse.ok(new CreateSessionResponse(sid));
  }

  @PostMapping("/start")
  public ApiResponse<Map<String,Object>> start(@RequestBody StartExamRequest req) {
    Long sid = examService.start(req.sessionId, req.paperId, req.studentId);
    Map<String,Object> resp = new HashMap<>();
    resp.put("sessionId", sid);
    resp.put("paperId", req.paperId);
    return ApiResponse.ok(resp);
  }

  @GetMapping("/papers/{paperId}/preview")
  public ApiResponse<List<Question>> previewPaper(@PathVariable Long paperId) {
    return ApiResponse.ok(paperService.preview(paperId));
  }

  @PostMapping("/answers")
  public ApiResponse<String> saveAnswer(@RequestBody SaveAnswerRequest req) {
    examService.saveAnswer(req.sessionId, req.studentId, req.questionId, req.answerJson);
    return ApiResponse.ok("ok");
  }

  @PostMapping("/submit")
  public ApiResponse<Map<String,Integer>> submit(@RequestBody StartExamRequest req) {
    Map<String,Integer> result = examService.submitAndAutoGrade(req.sessionId, req.paperId);
    return ApiResponse.ok(result);
  }

  @GetMapping("/sessions")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PROCTOR')")
  public ApiResponse<List<ExamSession>> listSessions(){ return ApiResponse.ok(examService.listSessions()); }

  @GetMapping("/students/{studentId}/records")
  public ApiResponse<List<SessionSummary>> studentRecords(@PathVariable Long studentId){
    java.util.List<ExamSession> sessions = examService.listSessionsByStudent(studentId);
    java.util.List<SessionSummary> res = new java.util.ArrayList<>();
    for (ExamSession s : sessions) {
      java.util.Map<String,Integer> sum = examService.getSummary(s.getId());
      SessionSummary dto = new SessionSummary();
      dto.sessionId = s.getId(); dto.paperId = s.getPaperId(); dto.status = s.getStatus();
      dto.objective = sum.get("objective"); dto.subjective = sum.get("subjective"); dto.total = sum.get("total");
      res.add(dto);
    }
    return ApiResponse.ok(res);
  }

  @GetMapping("/sessions/{sessionId}/summary")
  public ApiResponse<java.util.Map<String,Integer>> sessionSummary(@PathVariable Long sessionId){ return ApiResponse.ok(examService.getSummary(sessionId)); }

  @PutMapping("/sessions/{sessionId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<String> updateStatus(@PathVariable Long sessionId, @RequestParam String status){
    boolean ok = examService.updateStatus(sessionId, status);
    return ok ? ApiResponse.ok("ok") : ApiResponse.error(40400, "not found");
  }

  @GetMapping("/sessions/{sessionId}/answers")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<List<AnswerRecord>> listAnswers(@PathVariable Long sessionId){ return ApiResponse.ok(examService.listAnswers(sessionId)); }

  @PutMapping("/sessions/{sessionId}/answers/{questionId}/score")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<String> grade(@PathVariable Long sessionId, @PathVariable Long questionId, @RequestParam Integer score){
    examService.gradeSubjective(sessionId, questionId, score);
    return ApiResponse.ok("ok");
  }

  @PostMapping("/events")
  @PreAuthorize("hasAnyRole('ADMIN','PROCTOR','TEACHER')")
  public ApiResponse<String> recordEvent(@RequestBody ExamEventRequest req) {
    String key = req.sessionId + ":" + req.studentId;
    EVENTS.computeIfAbsent(key, k -> new ArrayList<>()).add(req.type + ":" + req.detail);
    return ApiResponse.ok("ok");
  }

  @GetMapping("/events/{sessionId}/{studentId}")
  public ApiResponse<List<String>> listEvents(@PathVariable Long sessionId, @PathVariable Long studentId) {
    String key = sessionId + ":" + studentId;
    return ApiResponse.ok(EVENTS.getOrDefault(key, Collections.emptyList()));
  }
}
