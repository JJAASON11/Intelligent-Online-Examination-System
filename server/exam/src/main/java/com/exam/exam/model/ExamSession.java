package com.exam.exam.model;

import com.exam.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;

@TableName("exam_session")
public class ExamSession extends BaseEntity {
  @TableField("paper_id")
  private Long paperId;
  @TableField("student_id")
  private Long studentId;
  @TableField("status")
  private String status;
  @TableField("start_at")
  private java.time.LocalDateTime startAt;
  @TableField("end_at")
  private java.time.LocalDateTime endAt;
  @TableField("rules_json")
  private String rulesJson;

  public Long getPaperId() { return paperId; }
  public void setPaperId(Long paperId) { this.paperId = paperId; }
  public Long getStudentId() { return studentId; }
  public void setStudentId(Long studentId) { this.studentId = studentId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public java.time.LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(java.time.LocalDateTime startAt) { this.startAt = startAt; }
  public java.time.LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(java.time.LocalDateTime endAt) { this.endAt = endAt; }
  public String getRulesJson() { return rulesJson; }
  public void setRulesJson(String rulesJson) { this.rulesJson = rulesJson; }
}

