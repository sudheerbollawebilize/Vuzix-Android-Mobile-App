package com.webilize.transfersdk.socket;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SocketConfig {
    private String ip;
    private int port;
    private Charset charset = StandardCharsets.UTF_8;
    private boolean isServer = true;
    private boolean ssl = true;

    FileInputStream publicKey;
    char[] password;

    public SocketConfig(int port) {
        this.port = port;
    }

    public SocketConfig(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public FileInputStream getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(FileInputStream publicKey) {
        this.publicKey = publicKey;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }
}
