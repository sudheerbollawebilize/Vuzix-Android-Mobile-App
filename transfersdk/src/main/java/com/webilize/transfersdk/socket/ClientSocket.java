package com.webilize.transfersdk.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientSocket extends ISocket {
    private static final String TAG = "ClientSocket";

    private Socket socket;

    public ClientSocket(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    @Override
    public void connect() throws IOException {

        socket = new Socket(socketConfig.getIp(), socketConfig.getPort());
        socket.setTcpNoDelay(true);

    }

    @Override
    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
        }
        socket = null;
    }

    @Override
    public boolean isConnected() {
        return socket != null;
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public boolean isServer() {
        return false;
    }

    @Override
    public boolean isReady() {
        return isConnected();
    }

    @Override
    public void connectionLost() throws IOException {
        disconnect();
    }


    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }
}
