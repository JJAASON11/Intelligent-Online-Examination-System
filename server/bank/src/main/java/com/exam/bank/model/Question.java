package com.exam.bank.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.common.model.BaseEntity;

@TableName("question")
public class Question extends BaseEntity {
  private Long bankId;
  private String type;
  private String stem;
  private String optionsJson;
  private String answerJson;
  private Integer score;
  private Integer difficulty;
  private String tags;

  public Long getBankId() { return bankId; }
  public void setBankId(Long bankId) { this.bankId = bankId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getStem() { return stem; }
  public void setStem(String stem) { this.stem = stem; }
  public String getOptionsJson() { return optionsJson; }
  public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
  public String getAnswerJson() { return answerJson; }
  public void setAnswerJson(String answerJson) { this.answerJson = answerJson; }
  public Integer getScore() { return score; }
  public void setScore(Integer score) { this.score = score; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public String getTags() { return tags; }
  public void setTags(String tags) { this.tags = tags; }
}

