package com.webilize.vuzixfilemanager.utils.eventbus;

public class OnTCPInitialized {
    public String ip;
    public int port;

    public OnTCPInitialized(String ip, int port) {
        this.port = port;
        this.ip = ip;
    }
}
