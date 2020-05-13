package com.webilize.transfersdk.observers;

import com.webilize.transfersdk.socket.DataWrapper;

import io.reactivex.observers.DisposableObserver;

public abstract class WriteObserver extends DisposableObserver<DataWrapper> {
    @Override
    public void onNext(DataWrapper dataWrapper) {
        switch (dataWrapper.getSocketState()) {
            case STARTING_WRITING:
                onStart();
                break;
            case PROGRESS:
                progress((Integer) dataWrapper.getData());
                break;
        }
    }

    public abstract void onStart();
    public abstract void progress(int progress);

}
