package com.exam.bank.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.bank.mapper.QuestionMapper;
import com.exam.bank.model.Question;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class QuestionService extends ServiceImpl<QuestionMapper, Question> {
  public List<Question> listAll() { return this.list(); }
  public int create(Question q) {
    LocalDateTime now = LocalDateTime.now();
    if (q.getCreatedAt() == null) q.setCreatedAt(now);
    if (q.getUpdatedAt() == null) q.setUpdatedAt(now);
    if (q.getScore() == null) q.setScore(0);
    return this.baseMapper.insert(q);
  }
  public int update(Question q) { return this.baseMapper.updateById(q); }
  public int delete(Long id) { return this.baseMapper.deleteById(id); }
  public Page<Question> page(Page<Question> page) { return this.baseMapper.selectPage(page, null); }
}
