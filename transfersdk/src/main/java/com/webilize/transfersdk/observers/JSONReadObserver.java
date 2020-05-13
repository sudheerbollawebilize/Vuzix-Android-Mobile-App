package com.webilize.transfersdk.observers;

import com.webilize.transfersdk.Metadata;

import java.io.File;
import java.util.List;

public abstract class JSONReadObserver extends ReadObserver {
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
    public void progress(int p) {

    }



    @Override
    public void newConnection() {

    }


    @Override
    public void onComplete() {

    }
}
