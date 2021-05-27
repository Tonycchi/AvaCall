package com.example.model;

import com.example.model.controls.EV3;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class WebClient extends WebSocketClient {

    private final String TAG = "WebClient";
    private EV3 controller;
    private String id;
    private final String jitsi;
    private boolean receiveCommands;
    private boolean ready;

    public WebClient(URI serverURI, String jitsi, EV3 controller) {
        super(serverURI);
        this.controller = controller;
        this.jitsi = jitsi;
        this.ready = false;
        receiveCommands = false;
    }

    /**
     * Sends a message when successfully connected to the server
     */
    @Override
    public void onOpen(ServerHandshake handshakeData) {
        send("app:" + jitsi);
    }

    /**
     * Sends a message when connection is closed
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        //Log.d(TAG,"closed with exit code " + code + " additional info: " + reason);
        ready = false;
    }

    /**
     * Handles getting an input message from the WebClient
     * @param message the input message
     */
    @Override
    public void onMessage(String message) {
        if (message.startsWith("id:")) {
            id = message.split(":",2)[1];
            ready = true;
        } else {
            if (receiveCommands) {
                String[] values;
                if (message.contains(";")) {
                    values = message.split(";", 2);
                    controller.send(Integer.valueOf(values[0]), Integer.valueOf(values[1]));
                }
            }
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {
    }

    public boolean isReady(){
        return ready;
    }

    /**
     * Send an error log when an error occurs
     */
    @Override
    public void onError(Exception ex) {
        //Log.e(TAG,"an error occurred:" + ex);
    }

    public String getId() {
        return id;
    }

    public void setReceiveCommands() {
        receiveCommands = true;
    }
}