package com.webilize.transfersdk.socket;

import android.bluetooth.BluetoothSocket;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothServerSocket extends ISocket {

    boolean connected;

    android.bluetooth.BluetoothServerSocket serverSocket;
    private BluetoothSocket client;

    public BluetoothServerSocket(android.bluetooth.BluetoothServerSocket socket) {
        this.serverSocket = socket;
        connected = false;
    }

    @Override
    public void connect() throws IOException, Exception {
        connected = true;
    }

    @Override
    public void disconnect() throws IOException {
        if (client != null)
            client.close();

        if (serverSocket != null)
            serverSocket.close();

        client = null;
        serverSocket = null;
    }

    @Override
    public boolean isConnected() {
        return connected && serverSocket != null;
    }

    @Override
    public Closeable getSocket() throws IOException {
        if (serverSocket != null) {
            if (client == null) {
                client = serverSocket.accept();
            }
        } else {
            client = null;
        }
        return client;
    }

    @Override
    public boolean isReady() {
        return serverSocket != null;
    }

    @Override
    public void connectionLost() throws IOException {

    }

    @Override
    public boolean isServer() {
        return true;
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
