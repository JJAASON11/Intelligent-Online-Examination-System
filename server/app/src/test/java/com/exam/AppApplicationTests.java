package com.exam;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppApplicationTests {
  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void healthEndpointReturnsOk() {
    ResponseEntity<String> resp = restTemplate.getForEntity("/api/health", String.class);
    assertEquals(200, resp.getStatusCodeValue());
    assertEquals("ok", resp.getBody());
  }
}

