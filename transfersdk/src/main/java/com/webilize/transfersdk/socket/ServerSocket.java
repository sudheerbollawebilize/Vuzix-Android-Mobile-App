package com.webilize.transfersdk.socket;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ServerSocket extends ISocket {
    private static final String TAG = "ServerSocket";

    private java.net.ServerSocket serverSocket;
    private Socket client;

    public ServerSocket(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    @Override
    public void connect() throws Exception {
        serverSocket = new java.net.ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(socketConfig.getPort()));
        socketConfig.setPort(serverSocket.getLocalPort());

        Log.d(TAG, "Server started: " + InetAddress.getLocalHost() + ":" + serverSocket.getLocalPort());
    }

    @Override
    public void disconnect() throws Exception {
        if (client != null)
            client.close();

        if (serverSocket != null) {
            Log.d(TAG, "Closing server: " + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());
            serverSocket.close();
        }

        client = null;
        serverSocket = null;
    }

    @Override
    public Socket getSocket() throws IOException {
        if (serverSocket != null) {
            if (client == null) {
                client = serverSocket.accept();
                client.setTcpNoDelay(true);
            }
        } else {
            client = null;
        }
        return client;
    }

    @Override
    public boolean isConnected() {
        return serverSocket != null; //&& client != null;
    }

    @Override
    public boolean isReady() {
        return isConnected() && client != null;
    }

    @Override
    public void connectionLost() {
        client = null;
    }


    @Override
    public InputStream getInputStream() throws IOException {
        return client.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return client.getOutputStream();
    }

}
