package com.exam.paper.model;

import com.exam.common.model.BaseEntity;

public class Paper extends BaseEntity {
  private String name;
  private Integer totalScore;
  private Integer durationMin;
  private String subject;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Integer getTotalScore() { return totalScore; }
  public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
  public Integer getDurationMin() { return durationMin; }
  public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }
}
