package com.exam.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private final JwtUtil jwtUtil;
  public JwtAuthenticationFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String auth = request.getHeader("Authorization");
    if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
      String token = auth.substring(7);
      try {
        Claims claims = jwtUtil.parse(token);
        
        // 检查token是否过期
        Date expiration = claims.getExpiration();
        if (expiration != null && expiration.before(new Date())) {
          logger.debug("Token已过期: {}", claims.getSubject());
          // 清除认证信息，让后续过滤器处理
          SecurityContextHolder.clearContext();
        } else {
        String username = claims.getSubject();
        Object rolesObj = claims.get("roles");
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (rolesObj instanceof List) {
          for (Object r : (List<?>) rolesObj) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + String.valueOf(r)));
          }
        }
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      } catch (ExpiredJwtException e) {
        logger.debug("Token已过期: {}", e.getClaims().getSubject());
        SecurityContextHolder.clearContext();
      } catch (JwtException e) {
        logger.debug("JWT解析失败: {}", e.getMessage());
        SecurityContextHolder.clearContext();
      } catch (Exception e) {
        logger.warn("JWT认证处理异常: {}", e.getMessage());
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}

