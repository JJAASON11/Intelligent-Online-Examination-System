package com.exam.user.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.common.model.BaseEntity;

@TableName("sys_user")
public class SysUser extends BaseEntity {
  private String username;
  private String password;   // 数据库列名一致
  private String nickname;   // 数据库列名一致
  private Integer status;    // 数据库列名一致

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  // 兼容旧代码：使用 passwordHash 命名的访问器
  public String getPasswordHash() { return password; }
  public void setPasswordHash(String passwordHash) { this.password = passwordHash; }
  public String getRealName() { return nickname; }
  public void setRealName(String realName) { this.nickname = realName; }
  public Integer getEnabled() { return status; }
  public void setEnabled(Integer enabled) { this.status = enabled; }
}
