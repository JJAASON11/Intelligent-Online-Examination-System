package com.exam.exam.model;

import com.exam.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;

@TableName("answer_record")
public class AnswerRecord extends BaseEntity {
  @TableField("session_id")
  private Long sessionId;
  @TableField("question_id")
  private Long questionId;
  @TableField("answer_json")
  private String answerJson;
  @TableField("score_auto")
  private Integer scoreAuto;
  @TableField("score_final")
  private Integer scoreFinal;
  @TableField("comment")
  private String comment;
  @TableField(exist=false)
  private Boolean judged;
  @TableField(exist=false)
  private Boolean correct;

  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public String getAnswerJson() { return answerJson; }
  public void setAnswerJson(String answerJson) { this.answerJson = answerJson; }
  public Integer getScoreAuto() { return scoreAuto; }
  public void setScoreAuto(Integer scoreAuto) { this.scoreAuto = scoreAuto; }
  public Integer getScoreFinal() { return scoreFinal; }
  public void setScoreFinal(Integer scoreFinal) { this.scoreFinal = scoreFinal; }
  public String getComment() { return comment; }
  public void setComment(String comment) { this.comment = comment; }
  public Boolean getJudged() { return judged; }
  public void setJudged(Boolean judged) { this.judged = judged; }
  public Boolean getCorrect() { return correct; }
  public void setCorrect(Boolean correct) { this.correct = correct; }
}

