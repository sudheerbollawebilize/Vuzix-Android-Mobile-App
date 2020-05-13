package com.webilize.transfersdk.read;

import android.util.Log;

import com.webilize.transfersdk.helpers.StreamHelper;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;

import org.json.JSONException;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.reactivex.subjects.Subject;

public abstract class IReadStrategy<T> {
    protected static final String TAG = "IRead";
    //region Default values
    protected static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    //endregion

    protected boolean bufferedStreams;
    protected int bufferSize;
    protected InputStream inputStream;

    protected Subject<DataWrapper> emitter;

    private StreamHelper.ProgressListener listener;

    protected void onError(String msg) {
        if (emitter != null) {
            emitter.onError(new Exception(msg));
        } else
            Log.e(TAG, "onError: " + msg);
    }

    protected void onError(Exception ex) {
        if (emitter != null) {
            emitter.onError(ex);
        } else {
            Log.e(TAG, "onError: ", ex);
        }
    }


    public T read() throws Exception {
        try {
            // we already read the first Byte
            readHeader();

            OutputStream outputStream = getOutputStream();
            final long size = getContentSize();
            if (bufferedStreams) {
                outputStream = new BufferedOutputStream(outputStream);
            }

            StreamHelper.ProgressListener progressListener = null;

            if (listener != null) {
                progressListener = (currentRead, totalRead) -> listener.progress(currentRead, totalRead);
            } else {
                if (emitter != null)
                    progressListener = (currentRead, totalRead) -> emitter.onNext(new DataWrapper(SocketState.PROGRESS, (int) (totalRead * 100 / size)));
            }
            StreamHelper.parse(inputStream, outputStream, bufferSize, size, progressListener);

            outputStream.flush();
            if (closeOutStream()) {
                outputStream.close();
            }

            return getResult();

        } catch (Exception ex) {
            onError(ex);
            return null;
        }
    }

    abstract OutputStream getOutputStream() throws FileNotFoundException;

    abstract void readHeader() throws Exception;

    abstract T getResult() throws JSONException, IOException;

    protected abstract long getContentSize();

    protected boolean closeOutStream() {
        return true;
    }

    public void setEmitter(Subject<DataWrapper> emitter) {
        this.emitter = emitter;
    }

    public void setListener(StreamHelper.ProgressListener listener) {
        this.listener = listener;
    }
}
