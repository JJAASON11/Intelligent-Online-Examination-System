package com.exam.paper.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

@TableName("paper_question")
public class PaperQuestion {
  @TableId(type = IdType.AUTO)
  private Long id;
  @TableField("paper_id")
  private Long paperId;
  @TableField("question_id")
  private Long questionId;
  @TableField("order_no")
  private Integer orderNo;
  @TableField("score")
  private Integer score;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getPaperId() { return paperId; }
  public void setPaperId(Long paperId) { this.paperId = paperId; }
  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public Integer getOrderNo() { return orderNo; }
  public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
  public Integer getScore() { return score; }
  public void setScore(Integer score) { this.score = score; }
}
