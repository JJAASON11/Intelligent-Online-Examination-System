package com.exam.user.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_user_role")
public class SysUserRole {
  @com.baomidou.mybatisplus.annotation.TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
  private Long id;
  @com.baomidou.mybatisplus.annotation.TableField("user_id")
  private Long userId;
  @com.baomidou.mybatisplus.annotation.TableField("role_id")
  private Long roleId;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getRoleId() { return roleId; }
  public void setRoleId(Long roleId) { this.roleId = roleId; }
}
