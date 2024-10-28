package bashkirov.store_original.handler;

import bashkirov.store_original.enumeration.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> userSessions = new HashMap<>();
    private WebSocketSession adminSession;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (getRole(session).equals(Role.ROLE_ADMIN)) {
            adminSession = session;
        } else {
            userSessions.put(session.getId(), session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (getRole(session).equals(Role.ROLE_USER) && adminSession != null) {
            adminSession.sendMessage(new TextMessage("User: "  + session.getId() + ", " + message.getPayload()));
        } else if (getRole(session).equals(Role.ROLE_ADMIN)) {
            String[] strings = message.getPayload().split(":");
            String userId = strings[0];
            String adminMessage = strings[1];
            WebSocketSession userSession = userSessions.get(userId);
            if (userSession != null && userSession.isOpen()) {
                userSession.sendMessage(new TextMessage("Admin: " + adminMessage));
            }
        }
    }





    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        userSessions.remove(session.getId());
        if (session.equals(adminSession)) {
            adminSession = null;
        }
    }

    private Role getRole(WebSocketSession session) {
        String uri = session.getUri().toString();
        return uri.contains("admin") ? Role.ROLE_ADMIN : Role.ROLE_USER;
    }

}
