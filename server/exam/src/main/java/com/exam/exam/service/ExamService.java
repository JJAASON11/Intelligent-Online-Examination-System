package com.exam.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.exam.bank.mapper.QuestionMapper;
import com.exam.bank.model.Question;
import com.exam.exam.mapper.AnswerRecordMapper;
import com.exam.exam.mapper.ExamSessionMapper;
import com.exam.exam.model.AnswerRecord;
import com.exam.exam.model.ExamSession;
import com.exam.paper.mapper.PaperQuestionMapper;
import com.exam.paper.model.PaperQuestion;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExamService {
  private final ExamSessionMapper sessionMapper;
  private final AnswerRecordMapper answerMapper;
  private final PaperQuestionMapper paperQuestionMapper;
  private final QuestionMapper questionMapper;

  public ExamService(ExamSessionMapper s, AnswerRecordMapper a, PaperQuestionMapper pqm, QuestionMapper qm) {
    this.sessionMapper = s; this.answerMapper = a; this.paperQuestionMapper = pqm; this.questionMapper = qm;
  }

  public void createOrUpsertSession(ExamSession s) {
    ExamSession exist = sessionMapper.selectById(s.getId());
    if (exist == null) sessionMapper.insert(s); else sessionMapper.updateById(s);
  }

  public Long start(Long sessionId, Long paperId, Long studentId) {
    ExamSession exist = sessionMapper.selectById(sessionId);
    if (exist != null) {
      exist.setStudentId(studentId != null ? studentId : exist.getStudentId());
      exist.setStatus("START");
      sessionMapper.updateById(exist);
      return exist.getId();
    }
    ExamSession s = new ExamSession();
    s.setId(sessionId);
    s.setPaperId(paperId);
    s.setStudentId(studentId != null ? studentId : 1L);
    s.setStatus("START");
    sessionMapper.insert(s);
    return s.getId();
  }

  public void saveAnswer(Long sessionId, Long studentId, Long questionId, String answerJson) {
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnswerRecord> qw =
      new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnswerRecord>()
        .eq(AnswerRecord::getSessionId, sessionId)
        .eq(AnswerRecord::getQuestionId, questionId);
    AnswerRecord ar = answerMapper.selectOne(qw);
    if (ar == null) {
      ar = new AnswerRecord();
      ar.setSessionId(sessionId);
      ar.setQuestionId(questionId);
      ar.setAnswerJson(answerJson);
      answerMapper.insert(ar);
    } else {
      ar.setAnswerJson(answerJson);
      answerMapper.updateById(ar);
    }
  }

  public void gradeSubjective(Long sessionId, Long questionId, Integer score) {
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnswerRecord> qw =
      new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnswerRecord>()
        .eq(AnswerRecord::getSessionId, sessionId)
        .eq(AnswerRecord::getQuestionId, questionId);
    AnswerRecord ar = answerMapper.selectOne(qw);
    if (ar == null) return;
    ar.setScoreFinal(score);
    ar.setJudged(true);
    answerMapper.updateById(ar);
    // no-op
  }

  public Map<String,Integer> submitAndAutoGrade(Long sessionId, Long paperId) {
    List<PaperQuestion> mappings = paperQuestionMapper.selectList(new QueryWrapper<PaperQuestion>().eq("paper_id", paperId));
    int obj = 0, sub = 0;
    for (PaperQuestion pq : mappings) {
      Question q = questionMapper.selectById(pq.getQuestionId());
      com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnswerRecord> qwA =
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnswerRecord>()
          .eq(AnswerRecord::getSessionId, sessionId)
          .eq(AnswerRecord::getQuestionId, pq.getQuestionId());
      AnswerRecord ar = answerMapper.selectOne(qwA);
      if (ar == null) { ar = new AnswerRecord(); ar.setSessionId(sessionId); ar.setQuestionId(pq.getQuestionId()); ar.setAnswerJson(null); }
      String type = q.getType();
      boolean objective = Arrays.asList("SINGLE","MULTI","JUDGE","FILL").contains(type);
      int s = 0; boolean ok = false; boolean judged = false;
      if (objective) {
        judged = true;
        ok = checkCorrect(q, ar.getAnswerJson());
        s = ok ? (pq.getScore()==null?0:pq.getScore()) : 0;
        obj += s;
        ar.setScoreAuto(s);
      } else {
        judged = false;
        s = ar.getScoreFinal() == null ? 0 : ar.getScoreFinal();
        sub += s;
      }
      // 不存 judged/correct 字段（数据库无该列），只写入 auto/final 分数
      if (ar.getId() == null) answerMapper.insert(ar); else answerMapper.updateById(ar);
    }
    ExamSession ses = sessionMapper.selectById(sessionId);
    if (ses != null) { ses.setStatus("SUBMIT"); sessionMapper.updateById(ses); }
    Map<String,Integer> r = new HashMap<>(); r.put("objective", obj); r.put("subjective", sub); r.put("total", obj+sub); return r;
  }

  public List<ExamSession> listSessions() { return sessionMapper.selectList(null); }
  public List<ExamSession> listSessionsByStudent(Long studentId) {
    return sessionMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExamSession>().eq("student_id", studentId));
  }

  public List<AnswerRecord> listAnswers(Long sessionId) {
    return answerMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnswerRecord>()
      .eq(AnswerRecord::getSessionId, sessionId));
  }

  public java.util.Map<String,Integer> getSummary(Long sessionId) {
    int obj = 0, sub = 0;
    for (AnswerRecord ar : listAnswers(sessionId)) {
      if (ar.getScoreAuto()!=null) obj += ar.getScoreAuto();
      if (ar.getScoreFinal()!=null) sub += ar.getScoreFinal();
    }
    java.util.HashMap<String,Integer> map = new java.util.HashMap<>();
    map.put("objective", obj); map.put("subjective", sub); map.put("total", obj+sub);
    return map;
  }

  public boolean updateStatus(Long sessionId, String status) {
    ExamSession ses = sessionMapper.selectById(sessionId);
    if (ses == null) return false;
    ses.setStatus(status);
    sessionMapper.updateById(ses);
    return true;
  }

  private void recompute(Long sessionId) { }

  private boolean checkCorrect(Question q, String answerJson) {
    Map<String,Object> ans = parseJson(answerJson);
    Map<String,Object> std = parseJson(q.getAnswerJson());
    String t = q.getType();
    if (t == null) return false;
    switch (t) {
      case "SINGLE": {
        Object i1 = ans.get("index"), i2 = std.get("index");
        return i1 != null && i1.toString().equals(i2==null?null:i2.toString());
      }
      case "MULTI": {
        Set<String> a = new HashSet<>(); Object ai = ans.get("indexes"); if (ai instanceof java.util.List) for (Object o:(List<?>)ai) a.add(String.valueOf(o));
        Set<String> s = new HashSet<>(); Object si = std.get("indexes"); if (si instanceof java.util.List) for (Object o:(List<?>)si) s.add(String.valueOf(o));
        return !a.isEmpty() && a.equals(s);
      }
      case "JUDGE": {
        Object v1 = ans.get("value"), v2 = std.get("value");
        return v1 != null && v1.toString().equals(v2==null?null:v2.toString());
      }
      case "FILL": {
        List<String> a = toStringList(ans.get("values"));
        List<String> s = toStringList(std.get("values"));
        if (a.size()!=s.size()) return false;
        for (int i=0;i<a.size();i++) { if (!a.get(i).trim().equalsIgnoreCase(s.get(i).trim())) return false; }
        return true;
      }
      default: return false;
    }
  }

  private Map<String,Object> parseJson(String s) {
    if (s==null || s.isEmpty()) return new HashMap<>();
    try {
      com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
      if (s.trim().startsWith("[")) {
        java.util.List list = om.readValue(s, java.util.List.class);
        java.util.HashMap map = new java.util.HashMap();
        map.put("values", list);
        return map;
      }
      return om.readValue(s, java.util.HashMap.class);
    } catch(Exception e){ return new HashMap<>(); }
  }
  private List<String> toStringList(Object v){
    List<String> r = new ArrayList<>();
    if (v instanceof List<?>) for (Object o:(List<?>)v) r.add(o==null?"":String.valueOf(o));
    return r;
  }
}

