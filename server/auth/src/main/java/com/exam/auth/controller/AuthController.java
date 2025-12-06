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

class LoginRequest {
  private String username;
  private String password;
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
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
    SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", req.getUsername()));
    if (user == null || (user.getEnabled() != null && user.getEnabled() == 0)) {
      return ApiResponse.error(40100, "用户名或密码错误");
    }
    String stored = user.getPasswordHash();
    boolean ok = stored != null && encoder.matches(req.getPassword(), stored);
    if (!ok) {
      return ApiResponse.error(40100, "用户名或密码错误");
    }
    List<SysUserRole> urs = userRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("user_id", user.getId()));
    List<String> roles = new ArrayList<>();
    if (urs != null && !urs.isEmpty()) {
      for (SysUserRole ur : urs) {
        SysRole role = roleMapper.selectById(ur.getRoleId());
        if (role != null) roles.add(role.getCode());
      }
    }
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
