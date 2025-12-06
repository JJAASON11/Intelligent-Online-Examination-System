package com.exam.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.exam.bank.mapper.QuestionMapper;
import com.exam.bank.model.Question;
import com.exam.paper.mapper.PaperMapper;
import com.exam.paper.mapper.PaperQuestionMapper;
import com.exam.paper.model.Paper;
import com.exam.paper.model.PaperQuestion;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PaperService {
  private final PaperMapper paperMapper;
  private final PaperQuestionMapper paperQuestionMapper;
  private final QuestionMapper questionMapper;

  public PaperService(PaperMapper paperMapper, PaperQuestionMapper paperQuestionMapper, QuestionMapper questionMapper) {
    this.paperMapper = paperMapper;
    this.paperQuestionMapper = paperQuestionMapper;
    this.questionMapper = questionMapper;
  }

  public Long createPaper(Paper p) {
    Paper latest = paperMapper.selectOne(new QueryWrapper<Paper>()
      .eq("name", p.getName())
      .orderByDesc("created_at")
      .last("limit 1"));
    if (latest != null) {
      java.time.LocalDateTime ca = latest.getCreatedAt();
      if (ca != null) {
        long sec = java.time.Duration.between(ca, java.time.LocalDateTime.now()).getSeconds();
        if (sec >= 0 && sec < 3) {
          return latest.getId();
        }
      }
    }
    paperMapper.insert(p);
    return p.getId();
  }
  public List<Paper> listPapers() { return paperMapper.selectList(null); }

  public List<Question> preview(Long paperId) {
    QueryWrapper<PaperQuestion> qw = new QueryWrapper<>();
    qw.eq("paper_id", paperId).orderByAsc("order_no");
    List<PaperQuestion> mappings = paperQuestionMapper.selectList(qw);
    List<Question> res = new ArrayList<>();
    for (PaperQuestion pq : mappings) {
      Question q = questionMapper.selectById(pq.getQuestionId());
      if (q != null) {
        q.setScore(pq.getScore());
        res.add(q);
      }
    }
    return res;
  }

  public String assemble(Long paperId, Map<String, Integer> strategy, int perScore) {
    int order = 1;
    int totalSelected = 0;
    int totalScore = 0;
    for (Map.Entry<String,Integer> e : strategy.entrySet()) {
      String type = e.getKey();
      int need = Math.max(0, e.getValue());
      if (need == 0) continue;
      QueryWrapper<Question> qw = new QueryWrapper<>();
      qw.eq("type", type).last("limit " + need);
      List<Question> list = questionMapper.selectList(qw);
      if (list.size() < need) {
        return "题库中题型" + type + "数量不足，需" + need + "题，当前" + list.size() + "题";
      }
      for (Question q : list) {
        PaperQuestion pq = new PaperQuestion();
        pq.setPaperId(paperId);
        pq.setQuestionId(q.getId());
        pq.setOrderNo(order++);
        pq.setScore(perScore);
        paperQuestionMapper.insert(pq);
        totalSelected++;
        totalScore += perScore;
      }
    }
    if (totalSelected == 0) return "策略未选任何题型";
    // 更新试卷总分为 题目总数 * 每题分值
    Paper p = paperMapper.selectById(paperId);
    if (p != null) {
      p.setTotalScore(totalScore);
      paperMapper.updateById(p);
    }
    return null;
  }

  public String assembleWithTypeScores(Long paperId, Map<String,Integer> strategy, Map<String,Integer> typeScores) {
    int order = 1;
    int totalScore = 0;
    int totalSelected = 0;
    for (Map.Entry<String,Integer> e : strategy.entrySet()) {
      String type = e.getKey();
      int need = Math.max(0, e.getValue());
      if (need == 0) continue;

      // 随机抽取：直接在数据库层随机 + 限制条数，避免全量加载
      List<Question> picked;
      if ("SHORT".equalsIgnoreCase(type)) {
        picked = questionMapper.selectList(new QueryWrapper<Question>()
          .in("type", java.util.Arrays.asList("SHORT","CODE"))
          .last("ORDER BY RAND() LIMIT " + need));
      } else {
        picked = questionMapper.selectList(new QueryWrapper<Question>()
          .eq("type", type)
          .last("ORDER BY RAND() LIMIT " + need));
      }
      if (picked.size() < need) {
        return "题库中题型" + type + "数量不足，需" + need + "题，当前" + picked.size() + "题";
      }

      Integer fallbackShort = typeScores != null ? typeScores.get("SHORT") : null;
      int scorePerType = 1;
      if (typeScores != null) {
        Integer ts = typeScores.get(type);
        scorePerType = (ts != null && ts > 0) ? ts : (fallbackShort != null && fallbackShort > 0 ? fallbackShort : 1);
      }
      for (Question q : picked) {
        PaperQuestion pq = new PaperQuestion();
        pq.setPaperId(paperId);
        pq.setQuestionId(q.getId());
        pq.setOrderNo(order++);
        pq.setScore(scorePerType);
        paperQuestionMapper.insert(pq);
        totalSelected++;
        totalScore += scorePerType;
      }
    }
    if (totalSelected == 0) return "策略未选任何题型";
    Paper p = paperMapper.selectById(paperId);
    if (p != null) { p.setTotalScore(totalScore); paperMapper.updateById(p); }
    return null;
  }

  public boolean updateQuestionScore(Long paperId, Long questionId, Integer score) {
    if (score == null || score < 0) return false;
    PaperQuestion pq = new PaperQuestion();
    pq.setScore(score);
    QueryWrapper<PaperQuestion> qw = new QueryWrapper<>();
    qw.eq("paper_id", paperId).eq("question_id", questionId);
    int n = paperQuestionMapper.update(pq, qw);
    // 重算总分
    int sum = 0;
    List<PaperQuestion> mappings = paperQuestionMapper.selectList(new QueryWrapper<PaperQuestion>().eq("paper_id", paperId));
    for (PaperQuestion m : mappings) { sum += (m.getScore() == null ? 0 : m.getScore()); }
    Paper p = paperMapper.selectById(paperId);
    if (p != null) { p.setTotalScore(sum); paperMapper.updateById(p); }
    return n > 0;
  }

  public boolean deletePaper(Long paperId) {
    // 删除映射
    paperQuestionMapper.delete(new QueryWrapper<PaperQuestion>().eq("paper_id", paperId));
    // 删除试卷
    int n = paperMapper.deleteById(paperId);
    return n > 0;
  }

  public String regenerateWithTypeScores(Long paperId, Map<String,Integer> strategy, Map<String,Integer> typeScores) {
    // 清理旧映射
    paperQuestionMapper.delete(new QueryWrapper<PaperQuestion>().eq("paper_id", paperId));
    // 按新策略重建
    return assembleWithTypeScores(paperId, strategy, typeScores);
  }
}
