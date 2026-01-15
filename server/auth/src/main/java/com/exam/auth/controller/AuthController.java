package com.exam.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.exam.common.api.ApiResponse;
import com.exam.auth.jwt.JwtUtil;
import com.exam.user.mapper.SysRoleMapper;
import com.exam.user.mapper.SysUserMapper;
import com.exam.user.mapper.SysUserRoleMapper;
import com.exam.user.model.SysRole;
import com.exam.user.model.SysUser;
import com.exam.user.model.SysUserRole;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Arrays;

class LoginRequest {
  private String username;
  private String password;
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
}

class RegisterRequest {
  private String username;
  private String password;
  private String nickname;
  private String role; // STUDENT, TEACHER, ADMIN
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
}

class LoginResult {
  private String token;
  private String username;
  private List<String> roles;
  private Long userId;
  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public List<String> getRoles() { return roles; }
  public void setRoles(List<String> roles) { this.roles = roles; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
}

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final SysUserMapper userMapper;
  private final SysUserRoleMapper userRoleMapper;
  private final SysRoleMapper roleMapper;
  private final PasswordEncoder encoder;
  private final JwtUtil jwtUtil;

  public AuthController(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper, SysRoleMapper roleMapper,
                        PasswordEncoder encoder, JwtUtil jwtUtil) {
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMapper = roleMapper;
    this.encoder = encoder;
    this.jwtUtil = jwtUtil;
  }

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<LoginResult> login(@RequestBody LoginRequest req) {
    System.out.println("[登录] 收到登录请求，用户名: " + req.getUsername());
    SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", req.getUsername()));
    if (user == null || (user.getEnabled() != null && user.getEnabled() == 0)) {
      System.out.println("[登录] 用户不存在或已禁用");
      return ApiResponse.error(40100, "用户名或密码错误");
    }
    String stored = user.getPasswordHash();
    if (stored == null || stored.isEmpty()) {
      System.out.println("[登录] 密码为空");
      return ApiResponse.error(40100, "用户名或密码错误");
    }
    System.out.println("[登录] 存储的密码格式: " + (stored.length() > 20 ? stored.substring(0, 20) + "..." : stored));
    
    // 检查密码格式：BCrypt密码以 $2a$, $2b$, $2y$ 开头
    boolean isBcryptFormat = stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
    boolean ok = false;
    
    if (isBcryptFormat) {
      // BCrypt格式，使用encoder验证
      ok = encoder.matches(req.getPassword(), stored);
    } else {
      // 处理明文密码格式
      String actualPassword = stored;
      
      // 处理 {plain} 前缀格式的密码
      if (stored.startsWith("{plain}")) {
        actualPassword = stored.substring(7); // 移除 {plain} 前缀（7个字符）
      }
      
      // 尝试多种比较方式（兼容不同的密码格式）
      ok = actualPassword.equals(req.getPassword()) || stored.equals(req.getPassword());
      
      System.out.println("[登录] 密码验证: 存储=" + actualPassword + ", 输入=" + req.getPassword() + ", 匹配=" + ok);
      
      // 如果明文密码验证成功，自动升级为BCrypt格式
      if (ok) {
        String encodedPassword = encoder.encode(req.getPassword());
        user.setPasswordHash(encodedPassword);
        userMapper.updateById(user);
        System.out.println("[登录] 密码已升级为BCrypt格式");
      }
    }
    
    if (!ok) {
      System.out.println("[登录] 密码验证失败");
      return ApiResponse.error(40100, "用户名或密码错误");
    }
    System.out.println("[登录] 登录成功，生成token");
    List<SysUserRole> urs = userRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("user_id", user.getId()));
    List<String> roles = new ArrayList<>();
    if (urs != null && !urs.isEmpty()) {
      for (SysUserRole ur : urs) {
        SysRole role = roleMapper.selectById(ur.getRoleId());
        if (role != null) {
          roles.add(role.getCode());
          System.out.println("[登录] 用户角色: " + role.getCode());
        }
      }
    } else {
      System.out.println("[登录] 警告：用户没有分配任何角色");
    }
    System.out.println("[登录] 用户所有角色: " + roles);
    Map<String,Object> claims = new HashMap<>();
    claims.put("roles", roles);
    String token = jwtUtil.generateToken(claims, user.getUsername());
    LoginResult res = new LoginResult();
    res.setToken(token);
    res.setUsername(user.getUsername());
    res.setRoles(roles);
    res.setUserId(user.getId());
    return ApiResponse.ok(res);
  }

  @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<LoginResult> register(@RequestBody RegisterRequest req) {
    System.out.println("[注册] 收到注册请求，用户名: " + req.getUsername());
    
    // 1. 检查用户名是否已存在
    SysUser existing = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", req.getUsername()));
    if (existing != null) {
      System.out.println("[注册] 用户名已存在: " + req.getUsername());
      return ApiResponse.error(40001, "用户名已存在");
    }
    
    // 2. 验证角色
    String role = req.getRole();
    if (role == null || role.isEmpty()) {
      role = "STUDENT"; // 默认学生
    }
    if (!Arrays.asList("STUDENT", "TEACHER", "ADMIN").contains(role)) {
      return ApiResponse.error(40002, "无效的角色");
    }
    
    // 3. 创建新用户
    SysUser newUser = new SysUser();
    newUser.setUsername(req.getUsername());
    newUser.setPasswordHash(encoder.encode(req.getPassword())); // 使用BCrypt加密
    newUser.setRealName(req.getNickname() != null ? req.getNickname() : req.getUsername());
    newUser.setEnabled(1); // 启用
    
    userMapper.insert(newUser);
    System.out.println("[注册] 用户创建成功，ID: " + newUser.getId());
    
    // 4. 分配角色
    SysRole targetRole = roleMapper.selectOne(new QueryWrapper<SysRole>().eq("code", role));
    if (targetRole != null) {
      SysUserRole ur = new SysUserRole();
      ur.setUserId(newUser.getId());
      ur.setRoleId(targetRole.getId());
      userRoleMapper.insert(ur);
      System.out.println("[注册] 角色分配成功: " + role);
    } else {
      System.out.println("[注册] 警告：找不到角色: " + role);
    }
    
    // 5. 生成token并返回（自动登录）
    List<String> roles = new ArrayList<>();
    roles.add(role);
    Map<String,Object> claims = new HashMap<>();
    claims.put("roles", roles);
    String token = jwtUtil.generateToken(claims, newUser.getUsername());
    
    LoginResult res = new LoginResult();
    res.setToken(token);
    res.setUsername(newUser.getUsername());
    res.setRoles(roles);
    res.setUserId(newUser.getId());
    
    System.out.println("[注册] 注册成功，返回token");
    return ApiResponse.ok(res);
  }

  @GetMapping("/me")
  public ApiResponse<Map<String,Object>> me(@RequestHeader(value = "Authorization", required = false) String auth) {
    Map<String,Object> info = new HashMap<>();
    try{
      String token = auth!=null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
      String username = token!=null ? jwtUtil.parse(token).getSubject() : null;
      if (username != null) {
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        info.put("username", username);
        info.put("userId", user!=null ? user.getId() : null);
        List<SysUserRole> urs = user!=null ? userRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("user_id", user.getId())) : Collections.emptyList();
        List<String> roles = new ArrayList<>();
        for (SysUserRole ur : urs) { SysRole role = roleMapper.selectById(ur.getRoleId()); if (role!=null) roles.add(role.getCode()); }
        info.put("roles", roles);
        info.put("ok", true);
        return ApiResponse.ok(info);
      }
    } catch(Exception ignored) {}
    info.put("ok", false);
    return ApiResponse.ok(info);
  }
}
