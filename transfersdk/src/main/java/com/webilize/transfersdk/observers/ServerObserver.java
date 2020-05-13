package com.webilize.transfersdk.observers;

public abstract class ServerObserver extends SocketObserver {
    @Override
    public void serverNotAvailable() {
        // Do nothing
    }
}
