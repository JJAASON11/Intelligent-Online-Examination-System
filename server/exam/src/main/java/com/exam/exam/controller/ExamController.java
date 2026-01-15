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
  private static final Map<String, Integer> SWITCH_COUNTS = new HashMap<>();

  @PostMapping("/sessions")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<CreateSessionResponse> createSession(@RequestBody CreateSessionRequest req) {
    long sid = System.currentTimeMillis();
    com.exam.exam.model.ExamSession s = new com.exam.exam.model.ExamSession();
    s.setId(sid);
    s.setPaperId(req.paperId);
    s.setStudentId(0L);
    s.setStatus("SCHEDULED");
    try {
      java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      if (req.startAt != null) s.setStartAt(java.time.LocalDateTime.parse(req.startAt, fmt));
      if (req.endAt != null) s.setEndAt(java.time.LocalDateTime.parse(req.endAt, fmt));
      if (s.getStartAt()!=null && s.getEndAt()!=null) {
        java.time.Duration dur = java.time.Duration.between(s.getStartAt(), s.getEndAt());
        if (dur.isNegative() || dur.isZero()) return ApiResponse.error(40020, "结束时间必须晚于开始时间");
        if (dur.toHours() > 24) return ApiResponse.error(40021, "考试窗口不能超过1天");
      }
    } catch (Exception ignored) {}
    examService.createOrUpsertSession(s);
    return ApiResponse.ok(new CreateSessionResponse(sid));
  }

  @PostMapping("/start")
  public ApiResponse<Map<String,Object>> start(@RequestBody StartExamRequest req) {
    Long sid = examService.start(req.sessionId, req.paperId, req.studentId);
    if (sid == null) return ApiResponse.error(40010, "不在考试时间或会话不存在");
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
  public ApiResponse<List<ExamSession>> listSessions(@RequestParam(required=false) Long paperId,
                                                     @RequestParam(required=false) Boolean onlyPending){
    return ApiResponse.ok(examService.listSessionsFiltered(paperId, onlyPending));
  }

  @GetMapping("/sessions/open")
  public ApiResponse<List<ExamSession>> listOpen(){ return ApiResponse.ok(examService.listOpenSessions()); }

  @GetMapping("/sessions/available")
  public ApiResponse<List<ExamSession>> listAvailable(@RequestParam(required=false) String date){
    java.time.LocalDate d = null;
    try { if (date!=null && !date.isEmpty()) d = java.time.LocalDate.parse(date); } catch(Exception ignored) {}
    return ApiResponse.ok(examService.listAvailableSessions(d));
  }

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

  @GetMapping("/sessions/{sessionId}/my-answers")
  public ApiResponse<List<AnswerRecord>> listMyAnswers(@PathVariable Long sessionId, @RequestParam Long studentId){
    // 学生只能查看自己的答案，需要验证sessionId和studentId匹配
    List<ExamSession> sessions = examService.listSessionsByStudent(studentId);
    boolean hasAccess = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));
    if (!hasAccess) {
      return ApiResponse.error(40300, "无权访问该考试记录");
    }
    return ApiResponse.ok(examService.listAnswers(sessionId));
  }

  @PutMapping("/sessions/{sessionId}/answers/{questionId}/score")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<String> grade(@PathVariable Long sessionId, @PathVariable Long questionId, @RequestParam Integer score){
    examService.gradeSubjective(sessionId, questionId, score);
    return ApiResponse.ok("ok");
  }

  @PostMapping("/events")
  public ApiResponse<String> recordEvent(@RequestBody ExamEventRequest req) {
    // 学生可以上报自己的事件，管理员/教师/监考员可以查看所有事件
    String key = req.sessionId + ":" + req.studentId;
    String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String eventStr = String.format("[%s] %s:%s", timestamp, req.type, req.detail);
    EVENTS.computeIfAbsent(key, k -> new ArrayList<>()).add(eventStr);
    
    String t = String.valueOf(req.type);
    String d = String.valueOf(req.detail);
    
    // 判断是否为切屏事件（避免重复计算）
    boolean isSwitch = ("visibility".equalsIgnoreCase(t) && "hidden".equalsIgnoreCase(d))
      || ("blur".equalsIgnoreCase(t) && !"window".equalsIgnoreCase(d))
      || (d != null && d.contains("fullscreen-exit"))
      || "lock".equalsIgnoreCase(t);
    
    // 切屏计数（使用同步块避免并发问题）
    if (isSwitch) {
      synchronized (SWITCH_COUNTS) {
      SWITCH_COUNTS.put(key, SWITCH_COUNTS.getOrDefault(key, 0) + 1);
      }
    }
    
    return ApiResponse.ok("ok");
  }

  @GetMapping("/events/{sessionId}/{studentId}")
  public ApiResponse<List<String>> listEvents(@PathVariable Long sessionId, @PathVariable Long studentId) {
    String key = sessionId + ":" + studentId;
    return ApiResponse.ok(EVENTS.getOrDefault(key, Collections.emptyList()));
  }

  @GetMapping("/events/{sessionId}/{studentId}/switches")
  public ApiResponse<Integer> switchCount(@PathVariable Long sessionId, @PathVariable Long studentId) {
    String key = sessionId + ":" + studentId;
    return ApiResponse.ok(SWITCH_COUNTS.getOrDefault(key, 0));
  }

  @GetMapping("/sessions/{sessionId}/pending-count")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public ApiResponse<Integer> pendingCount(@PathVariable Long sessionId){ return ApiResponse.ok(examService.pendingCount(sessionId)); }

  @PostMapping("/cleanup")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ApiResponse<java.util.Map<String,Integer>> cleanup(@RequestParam(defaultValue="true") boolean dry){ return ApiResponse.ok(examService.cleanup(dry)); }
}
