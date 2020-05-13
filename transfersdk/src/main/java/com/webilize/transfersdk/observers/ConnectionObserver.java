package com.webilize.transfersdk.observers;

import com.webilize.transfersdk.Metadata;
import com.webilize.transfersdk.socket.DataWrapper;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

public abstract class ConnectionObserver extends ReadObserver {
    public ConnectionObserver() {
        connectedNotification = true;
    }

    @Override
    public void onNext(DataWrapper dataWrapper) {
        super.onNext(dataWrapper);
    }

    @Override
    protected void onConnected() {
        super.onConnected();
    }

    @Override
    protected void onRead(File data) {

    }

    @Override
    protected void onRead(List<File> data) {

    }

    @Override
    protected void onStart(Metadata data) {

    }

    @Override
    protected void onRead(JSONObject json) {

    }

    @Override
    public void progress(int p) {

    }


    @Override
    public void newConnection() {

    }

    @Override
    public void clientDisconnected() {

    }

    @Override
    public void onComplete() {

    }
}
