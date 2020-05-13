package com.webilize.transfersdk.write;

import com.webilize.transfersdk.helpers.StreamHelper;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.reactivex.subjects.Subject;

public abstract class IWriteStrategy {
    protected static final String TAG = "MyTAG";
    //region Default values
    protected static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    //endregion

    protected OutputStream outputStream;
    protected boolean bufferedStreams;
    protected int bufferSize;

    protected Subject<DataWrapper> emitter;
    private StreamHelper.ProgressListener listener;

    public void setEmitter(Subject<DataWrapper> emitter) {
        this.emitter = emitter;
        this.listener = null;
    }

    public void setListener(StreamHelper.ProgressListener listener) {
        this.listener = listener;
        this.emitter = null;
    }

    //region abstract methods
    protected abstract byte[] header();

    protected abstract InputStream getInputStream() throws FileNotFoundException;

    protected abstract long getContentSize();
    //endregion

    public void write() throws Exception {
        try {

            if (emitter != null)
                emitter.onNext(new DataWrapper(SocketState.STARTING_WRITING, null));

            byte[] header = header();
            final long size = getContentSize();

            InputStream inputStream = getInputStream();

            outputStream.write(header);
            outputStream.flush();

            if (bufferedStreams) {
                outputStream = new BufferedOutputStream(outputStream);
            }

            StreamHelper.ProgressListener progressListener = null;
            if (emitter != null)
                progressListener = (currentRead, totalRead) -> emitter.onNext(new DataWrapper(SocketState.PROGRESS, (int) (totalRead * 100 / size)));
            if (listener != null) {
                progressListener = (currentRead, totalRead) -> listener.progress(currentRead, totalRead);
            }

            StreamHelper.parse(inputStream, outputStream, bufferSize, size, progressListener);

            outputStream.flush();
            closeInputStream();


        } catch (Exception ex) {
            closeOutputStream();
            closeInputStream();
            throw ex;
        }

    }

    public void closeOutputStream() throws IOException {
        outputStream.flush();
        outputStream.close();
    }

    public void closeInputStream() throws IOException {
        getInputStream().close();
    }

}
