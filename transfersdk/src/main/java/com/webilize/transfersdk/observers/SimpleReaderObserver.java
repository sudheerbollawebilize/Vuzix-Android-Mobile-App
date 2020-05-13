package com.webilize.transfersdk.observers;

import java.io.File;
import java.util.List;

public abstract class SimpleReaderObserver extends ReadObserver {
    @Override
    protected void onRead(List<File> data) {

    }

    @Override
    public void newConnection() {

    }

    @Override
    public void clientDisconnected() {

    }
}
