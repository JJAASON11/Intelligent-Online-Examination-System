package com.exam.proctor.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@Component
public class ProctorSocketHandler extends TextWebSocketHandler {
  private final Map<String, Set<WebSocketSession>> rooms = new HashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    // no-op
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    // 消息格式：{"type":"join","sessionId":"123"} 或 {"type":"event","sessionId":"123","payload":"..."}
    String payload = message.getPayload();
    Map<String, Object> obj = parseJson(payload);
    String type = String.valueOf(obj.get("type"));
    String sid = String.valueOf(obj.get("sessionId"));
    if ("join".equalsIgnoreCase(type)) {
      rooms.computeIfAbsent(sid, k -> new HashSet<>()).add(session);
      session.sendMessage(new TextMessage("joined:" + sid));
    } else if ("event".equalsIgnoreCase(type)) {
      broadcast(sid, new TextMessage("event:" + String.valueOf(obj.get("payload"))));
    } else {
      session.sendMessage(new TextMessage("echo:" + payload));
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
    for (Set<WebSocketSession> set : rooms.values()) set.remove(session);
  }

  private void broadcast(String sid, TextMessage msg) throws Exception {
    Set<WebSocketSession> set = rooms.getOrDefault(sid, Collections.emptySet());
    for (WebSocketSession s : set) {
      if (s.isOpen()) s.sendMessage(msg);
    }
  }

  private Map<String, Object> parseJson(String s) {
    try {
      // 极简解析（不引入额外依赖）
      Map<String, Object> map = new HashMap<>();
      s = s.trim().replaceAll("^\\{|\\}$", "");
      for (String part : s.split(",")) {
        String[] kv = part.split(":",2);
        if (kv.length==2) {
          String k = kv[0].trim().replaceAll("^\"|\"$","" );
          String v = kv[1].trim().replaceAll("^\"|\"$","" );
          map.put(k, v);
        }
      }
      return map;
    } catch (Exception e) {
      return Collections.emptyMap();
    }
  }
}
