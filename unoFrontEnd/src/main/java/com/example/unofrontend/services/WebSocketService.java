package com.example.unofrontend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class WebSocketService {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private boolean connected = false;
    private String gameRoomId;
    private List<Consumer<String>> statusListeners = new ArrayList<>();

    public WebSocketService() {
        setupStompClient();
    }

    private void setupStompClient() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    public void connect(String url, Consumer<String> statusCallback) {
        if (connected && stompSession != null && stompSession.isConnected()) {
            System.out.println("WebSocket already connected - reusing existing connection");
            statusCallback.accept("Already connected");
            return;
        }

        // Reset connection state if session is dead
        if (stompSession != null && !stompSession.isConnected()) {
            connected = false;
            stompSession = null;
        }

        statusCallback.accept("Connecting...");
        try {

            String jwtToken = com.example.unofrontend.session.SessionManager.getToken();
            
            System.out.println("JWT Token from SessionManager: " + (jwtToken != null ? "Present (length: " + jwtToken.length() + ")" : "NULL"));
            
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                System.out.println("JWT token is null or empty!");
                statusCallback.accept("Connection failed: No JWT token found");
                return;
            }
            

            System.out.println("WebSocket connecting with JWT in STOMP headers");
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            StompHeaders stompHeaders = new StompHeaders();
            stompHeaders.add("Authorization", "Bearer " + jwtToken);
            

            CompletableFuture<StompSession> future = stompClient.connectAsync(url, headers, stompHeaders, new CustomStompSessionHandler(statusCallback));
            
            // Set timeout of 10 seconds
            stompSession = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
            connected = true;
            statusCallback.accept("Connected");
            
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("WebSocket connection timeout", e);
            System.out.println("WebSocket connection timeout after 10 seconds");
            statusCallback.accept("Connection failed: Timeout");
            connected = false;
            stompSession = null;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error connecting to WebSocket", e);
            System.out.println("WebSocket connection error details: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("WebSocket connection error cause: " + e.getCause().getMessage());
            }
            statusCallback.accept("Connection failed: " + e.getMessage());
            connected = false;
            stompSession = null;
        } catch (Exception e) {
            logger.error("Unexpected error during WebSocket connection", e);
            System.out.println("Unexpected WebSocket error: " + e.getMessage());
            statusCallback.accept("Connection failed: " + e.getMessage());
            connected = false;
            stompSession = null;
        }
    }

    public void addStatusListener(Consumer<String> listener) {
        statusListeners.add(listener);
    }

    private void notifyStatusListeners(String status) {
        for (Consumer<String> listener : statusListeners) {
            listener.accept(status);
        }
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            connected = false;
            notifyStatusListeners("Disconnected");
        }
    }

    public void subscribeToGameTopic(String gameId, Consumer<Map<String, Object>> messageHandler) {
        if (!connected || stompSession == null) {
            logger.error("Cannot subscribe: not connected");
            return;
        }

        this.gameRoomId = gameId;
        stompSession.subscribe("/topic/game/" + gameId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) payload;
                    messageHandler.accept(message);
                    notifyStatusListeners("Message received");
                }
            }
        });
        notifyStatusListeners("Subscribed to game: " + gameId);
    }

    public void subscribeToGameState(Consumer<Map<String, Object>> messageHandler) {
        if (!connected || stompSession == null) {
            logger.error("Cannot subscribe to game state: not connected");
            return;
        }

        stompSession.subscribe("/user/queue/gameState", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) payload;
                    messageHandler.accept(message);
                    notifyStatusListeners("Game state received");
                }
            }
        });
        notifyStatusListeners("Subscribed to individual game state updates");
    }

    public void joinGame(String gameId, String playerName) {
        if (!connected || stompSession == null) {
            logger.error("Cannot join game: not connected");
            return;
        }

        this.gameRoomId = gameId;
        
        Map<String, String> joinMessage = new HashMap<>();
        joinMessage.put("gameId", gameId);
        joinMessage.put("player", playerName);
        joinMessage.put("action", "JOIN");
        
        stompSession.send("/app/join", joinMessage);
        notifyStatusListeners("Joined game: " + gameId);
    }

    public void playCard(String gameId, String card, String color, String playerName) {
        if (!connected || stompSession == null) {
            logger.error("Cannot play card: not connected");
            return;
        }

        Map<String, String> cardInfo = new HashMap<>();
        cardInfo.put("gameId", gameId);
        cardInfo.put("card", card);
        cardInfo.put("color", color);
        cardInfo.put("player", playerName);
        
        stompSession.send("/app/playCard", cardInfo);
        notifyStatusListeners("Card played: " + card);
    }

    public void sendMessage(String destination, Map<String, String> message) {
        if (!connected || stompSession == null) {
            logger.error("Cannot send message: not connected");
            return;
        }
        
        stompSession.send(destination, message);
        notifyStatusListeners("Message sent to: " + destination);
    }

    public boolean isConnected() {
        return connected && stompSession != null && stompSession.isConnected();
    }

    public String getGameRoomId() {
        return gameRoomId;
    }

    public void subscribeToErrors(Consumer<Map<String, Object>> messageHandler) {
        if (!connected || stompSession == null) {
            logger.error("Cannot subscribe to errors: not connected");
            return;
        }
        stompSession.subscribe("/user/queue/errors", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) payload;
                    messageHandler.accept(message);
                    notifyStatusListeners("Error message received");
                }
            }
        });
        notifyStatusListeners("Subscribed to error queue");
    }

    private class CustomStompSessionHandler extends StompSessionHandlerAdapter {
        private final Consumer<String> statusCallback;

        public CustomStompSessionHandler(Consumer<String> statusCallback) {
            this.statusCallback = statusCallback;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            logger.info("Connected to WebSocket server");
            statusCallback.accept("Connected");
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, 
                                  byte[] payload, Throwable exception) {
            logger.error("WebSocket exception occurred", exception);
            System.out.println("WebSocket exception: " + exception.getMessage());
            statusCallback.accept("Connection error: " + exception.getMessage());
            connected = false;
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            logger.error("WebSocket transport error", exception);
            System.out.println("WebSocket transport error: " + exception.getMessage());
            statusCallback.accept("Transport error: " + exception.getMessage());
            connected = false;
        }
    }
} 