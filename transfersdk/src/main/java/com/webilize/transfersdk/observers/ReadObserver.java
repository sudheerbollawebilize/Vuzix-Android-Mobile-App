package com.webilize.transfersdk.observers;

import com.webilize.transfersdk.Metadata;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import io.reactivex.observers.DisposableObserver;

public abstract class ReadObserver extends DisposableObserver<DataWrapper> {
    private boolean notifiedDisconnected = false;
    private boolean notifiedConnected = false;
    boolean connectedNotification = false;

    @Override
    public void onNext(DataWrapper dataWrapper) {
        switch (dataWrapper.getSocketState()) {
            case CONNECTED:
                if (connectedNotification) {
                    if (!notifiedConnected) {
                        onConnected();
                        notifiedConnected = true;
                    }

                }
                break;
            case DISCONNECTED:
                if (!notifiedDisconnected) {
                    onDisconnected();
                    notifiedDisconnected = true;
                    notifiedConnected = false;
                }
                break;
            case PROGRESS:
                progress((Integer) dataWrapper.getData());
                break;
            case NEW_CONNECTION:
                newConnection();
                break;
            case CLIENT_DISCONNECTED:
                clientDisconnected();
                break;
            case JSON_RECEIVED:
                onRead((JSONObject) dataWrapper.getData());
                break;
            case FILE_RECEIVED:
                onRead((File) dataWrapper.getData());
                break;
            case MULTIPLE_FILES:
                onRead((List<File>) dataWrapper.getData());
                break;
            case METADATA:
                onStart((Metadata) dataWrapper.getData());
                break;
        }

        if (dataWrapper.getSocketState() != SocketState.DISCONNECTED) {
            notifiedDisconnected = false;
        }
    }

    protected void onConnected() {

    }

    protected abstract void onRead(File data);

    protected abstract void onRead(List<File> data);

    protected abstract void onStart(Metadata data);

    protected abstract void onRead(JSONObject json);

    public abstract void progress(int p);

    public abstract void onDisconnected();

    public abstract void newConnection();

    public abstract void clientDisconnected();
}
