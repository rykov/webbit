package chatroom;

import com.google.gson.Gson;
import webbit.WebSocketConnection;
import webbit.WebSocketHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Chatroom implements WebSocketHandler {

    private final Gson json = new Gson();

    public static final String USERNAME_KEY = "username";

    static class Incoming {
        enum Action { LOGIN, SAY }
        Action action;
        String loginUsername;
        String message;
    }

    static class Outgoing {
        enum Action { JOIN, LEAVE, SAY }
        Action action;
        String username;
        String message;
    }

    private Set<WebSocketConnection> connections = new HashSet<WebSocketConnection>();

    @Override
    public void onOpen(WebSocketConnection connection) throws Exception {
        connections.add(connection);
    }

    @Override
    public void onMessage(WebSocketConnection connection, String msg) throws Exception {
        Incoming incoming = json.fromJson(msg, Incoming.class);
        switch (incoming.action) {
            case LOGIN:
                login(connection, incoming.loginUsername);
                break;
            case SAY:
                say(connection, incoming.message);
                break;
        }
    }

    private void login(WebSocketConnection connection, String username) {
        connection.data(USERNAME_KEY, username); // associate username with connection

        Outgoing outgoing = new Outgoing();
        outgoing.action = Outgoing.Action.JOIN;
        outgoing.username = username;
        broadcast(outgoing);
    }

    private void say(WebSocketConnection connection, String message) {
        String username = (String) connection.data(USERNAME_KEY);
        if (username != null) {
            Outgoing outgoing = new Outgoing();
            outgoing.action = Outgoing.Action.SAY;
            outgoing.username = username;
            outgoing.message = message;
            broadcast(outgoing);
        }
    }

    private void broadcast(Outgoing outgoing) {
        String json = this.json.toJson(outgoing);
        for (WebSocketConnection connection : connections) {
            if (connection.data(USERNAME_KEY) != null) { // only broadcast to those who have completed login
                connection.send(json);
            }
        }
    }

    @Override
    public void onClose(WebSocketConnection connection) throws Exception {
        String username = (String) connection.data(USERNAME_KEY);
        if (username != null) {
            Outgoing outgoing = new Outgoing();
            outgoing.action = Outgoing.Action.LEAVE;
            outgoing.username = username;
            broadcast(outgoing);
        }
        connections.remove(connection);
    }

}
