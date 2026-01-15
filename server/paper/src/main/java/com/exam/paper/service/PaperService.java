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

  public String assembleWithTypeScores(Long paperId,
                                       Map<String,Integer> strategy,
                                       Map<String,Integer> typeScores,
                                       Integer difficulty) {
    int order = 1;
    int totalScore = 0;
    int totalSelected = 0;
    for (Map.Entry<String,Integer> e : strategy.entrySet()) {
      String type = e.getKey();
      int need = Math.max(0, e.getValue());
      if (need == 0) continue;

      // ------------------ 基于难度的智能选题逻辑 ------------------
      // 目标：在尽量满足「目标难度」的前提下，补充相邻难度的题目，保证题量充足
      List<Question> picked = new ArrayList<>();
      QueryWrapper<Question> baseQw = new QueryWrapper<>();
      if ("SHORT".equalsIgnoreCase(type)) {
        baseQw.in("type", java.util.Arrays.asList("SHORT","CODE"));
      } else {
        baseQw.eq("type", type);
      }

      // 1. 没有指定难度：保持原来的简单随机策略（但在 Java 侧打散）
      if (difficulty == null || difficulty <= 0) {
        List<Question> all = questionMapper.selectList(baseQw);
        if (all.size() < need) {
          return "题库中题型" + type + "数量不足，需" + need + "题，当前" + all.size() + "题";
        }
        // 打乱顺序后直接取前 need 道题
        java.util.Collections.shuffle(all);
        picked.addAll(all.subList(0, need));
      } else {
        // 2. 指定了目标难度：优先取“难度==目标”的题，不足时用「邻近难度」补足

        // 2.1 先取目标难度的题
        QueryWrapper<Question> exactQw = baseQw.clone();
        exactQw.eq("difficulty", difficulty);
        List<Question> exactList = questionMapper.selectList(exactQw);

        java.util.Collections.shuffle(exactList); // 先打散
        if (exactList.size() >= need) {
          picked.addAll(exactList.subList(0, need));
        } else {
          picked.addAll(exactList);
          int remain = need - exactList.size();

          // 2.2 再取其他难度的题作为补充，优先选择「与目标难度差值更小」的题
          QueryWrapper<Question> othersQw = baseQw.clone();
          othersQw.ne("difficulty", difficulty).or().isNull("difficulty");
          List<Question> others = questionMapper.selectList(othersQw);

          if (exactList.size() + others.size() < need) {
            // 整个题型的题目总数都不够，返回错误提示（包含难度信息，便于老师理解）
            return "题库中满足目标难度的题型" + type + "数量不足，需" + need + "题，当前仅有"
              + (exactList.size() + others.size()) + "题";
      }

          // 先打散，再按「与目标难度的绝对差」从小到大排序，这样更偏向于选择接近目标难度的题
          java.util.Collections.shuffle(others);
          others.sort((q1, q2) -> {
            Integer d1 = q1.getDifficulty();
            Integer d2 = q2.getDifficulty();
            int diff1 = (d1 == null ? 10 : Math.abs(d1 - difficulty));
            int diff2 = (d2 == null ? 10 : Math.abs(d2 - difficulty));
            return Integer.compare(diff1, diff2);
          });

          picked.addAll(others.subList(0, remain));
        }
      }
      // ------------------ 选题结束 ------------------

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

  public String regenerateWithTypeScores(Long paperId,
                                         Map<String,Integer> strategy,
                                         Map<String,Integer> typeScores,
                                         Integer difficulty) {
    // 清理旧映射
    paperQuestionMapper.delete(new QueryWrapper<PaperQuestion>().eq("paper_id", paperId));
    // 按新策略重建（带难度筛选）
    return assembleWithTypeScores(paperId, strategy, typeScores, difficulty);
  }
}
