package com.webilize.transfersdk.observers;

import com.webilize.transfersdk.socket.DataWrapper;

import io.reactivex.observers.DisposableObserver;

public abstract class SocketObserver extends DisposableObserver<DataWrapper> {

    @Override
    public void onNext(DataWrapper dataWrapper) {
        switch (dataWrapper.getSocketState()) {
            case CONNECTED:
                onConnected();
                break;
            case NEW_CONNECTION:
                newConnection();
                break;
            case CONNECTION_TIMEOUT:
                connectionNotAvailable();
                break;
            case SERVER_NOT_AVAILABLE:
                serverNotAvailable();
                break;
        }
    }

    public abstract void onConnected();

    public abstract void connectionNotAvailable();

    public abstract void serverNotAvailable();

    public abstract void newConnection();
}
