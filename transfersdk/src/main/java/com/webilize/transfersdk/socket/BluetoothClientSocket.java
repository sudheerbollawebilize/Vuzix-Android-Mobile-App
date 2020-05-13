package com.webilize.transfersdk.socket;

import android.bluetooth.BluetoothSocket;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothClientSocket extends ISocket {
    boolean connected;

    public BluetoothClientSocket(BluetoothSocket socket) {
        this.socket = socket;
        connected = false;
    }

    BluetoothSocket socket;


    @Override
    public void connect() throws IOException, Exception {
        connected = true;
        socket.connect();
    }

    @Override
    public void disconnect() throws IOException {
        socket.close();
    }

    @Override
    public boolean isConnected() {
        return connected && socket != null;
    }

    @Override
    public Closeable getSocket() throws IOException {
        return socket;
    }

    @Override
    public boolean isReady() {
        return socket != null;
    }

    @Override
    public void connectionLost() throws IOException {

    }

    @Override
    public boolean isServer() {
        return false;
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
