package com.exam.common.config;

import com.exam.common.api.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ApiResponse<String> handle(Exception e) {
    return ApiResponse.error(50000, e.getMessage());
  }
}

