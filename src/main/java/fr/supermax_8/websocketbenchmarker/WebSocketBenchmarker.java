package fr.supermax_8.websocketbenchmarker;

import io.javalin.Javalin;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.CompletionStage;

public class WebSocketBenchmarker {

    static WebSocket socket;

    public static void main(String[] args) {
        switch (args[0]) {
            case "server" -> {
                Javalin app = Javalin.create().start(Integer.parseInt(args[1]));

                List<Session> sessions = new ArrayList<>();

                System.out.println("Starting WebSocket...");
                app.ws("/websocket", wsConfig -> {
                    wsConfig.onConnect(ctx -> {
                        sessions.add(ctx.session);
                        System.out.println("New session connected, total: " + sessions.size());
                    });
                    wsConfig.onClose(ctx -> {
                        sessions.remove(ctx.session);
                    });
                    wsConfig.onMessage(ctx -> {
                        System.out.println("Received message: " + ctx.session.getRemoteAddress());
                        List<Session> sessions1 = new LinkedList<>(sessions);
                        sessions1.remove(ctx.session);
                        sessions1.forEach(s -> {
                            try {
                                s.getRemote().sendString(ctx.message());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    });
                }).start();
            }
            case "connect" -> {
                HttpClient client = HttpClient.newHttpClient();
                WebSocket.Builder builder = client.newWebSocketBuilder();
                socket = builder.buildAsync(URI.create("ws://localhost:" + args[1] + "/websocket"), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        webSocket.request(1);
                        System.out.println("Connected to the server");
                        webSocket.sendText("Hello, Server!", true);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        System.out.println("Received message: " + data);
                        webSocket.request(1);  // Continue receiving messages
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.out.println("Closed with status " + statusCode + ", reason: " + reason);
                        return null;
                    }
                }).join();
            }
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            socket.sendText(command, true);
        }
    }

}