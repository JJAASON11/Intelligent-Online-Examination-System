package com.exam.user.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.common.model.BaseEntity;

@TableName("sys_user")
public class SysUser extends BaseEntity {
  private String username;
  private String passwordHash;
  private String realName;
  private Integer enabled;

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getRealName() { return realName; }
  public void setRealName(String realName) { this.realName = realName; }
  public Integer getEnabled() { return enabled; }
  public void setEnabled(Integer enabled) { this.enabled = enabled; }
}

