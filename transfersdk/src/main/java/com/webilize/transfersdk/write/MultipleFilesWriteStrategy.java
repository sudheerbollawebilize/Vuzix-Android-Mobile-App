package com.webilize.transfersdk.write;

import android.util.Log;

import com.webilize.transfersdk.ProgressItem;
import com.webilize.transfersdk.Protocol;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.reactivex.subjects.Subject;

public class MultipleFilesWriteStrategy {

    //region Default values
    protected static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    //endregion

    protected Subject<DataWrapper> emitter;

    protected OutputStream outputStream;
    protected boolean bufferedStreams;
    protected int bufferSize;

    private long currentRead;

    public void setEmitter(Subject<DataWrapper> emitter) {
        this.emitter = emitter;
    }

    public MultipleFilesWriteStrategy(OutputStream outputStream, Boolean bufferedStreams, Integer bufferSize) {
        this.outputStream = outputStream;
        this.bufferedStreams = bufferedStreams;
        this.bufferSize = bufferSize;
        currentRead = 0;
    }

    public void write(List<File> files) throws Exception {
        currentRead = 0;

        long totalSize = 0;
        JSONObject jsonObject = new JSONObject();
        JSONArray filesJson = new JSONArray();

        for (File file : files) {
            JSONObject fileJson = new JSONObject();

            fileJson.put("fileName", file.getName());
            fileJson.put("size", file.length());
            // todo: add date
            filesJson.put(fileJson);
            totalSize += file.length();
        }

        jsonObject.put("files", filesJson);

        IWriteStrategy writeStrategy = new JsonWriteStrategy.Builder()
                .setJSON(jsonObject)
                .setOutputStream(outputStream)
                .setBufferedStreams(bufferedStreams)
                .setBufferSize(bufferSize)
                .setType(Protocol.MULTIPLE)
                .build();
        writeStrategy.write();

        for (File file : files) {
            IWriteStrategy fileWriteStrategy = new FileWriteStrategy.Builder()
                    .setFile(file)
                    .setOutputStream(outputStream)
                    .setBufferedStreams(bufferedStreams)
                    .setBufferSize(bufferSize)
                    .build();
            final long finalTotalSize = totalSize;
            if (emitter != null)
                fileWriteStrategy.setListener((n, totalRead) -> {
                    currentRead += n;
                    long percent = currentRead * 100 / finalTotalSize;
                    emitter.onNext(new DataWrapper(SocketState.PROGRESS, (int) percent));
                });
          /*  if (emitter != null)
                fileWriteStrategy.setListener((n, totalRead) -> {
                    currentRead += n;
                    indiPerc += n;
                    long percentInd = indiPerc * 100 / file.length();
                    long percent = currentRead * 100 / finalTotalSize;
                    emitter.onNext(new DataWrapper(SocketState.PROGRESS, new ProgressItem(file.getName() + " , " + percentInd, percent)));
                });*/
            fileWriteStrategy.write();
        }
    }

    //region Builder
    public static class Builder {
        private OutputStream _outputStream;
        private Boolean _bufferedStreams;
        private Integer _bufferSize;

        public Builder setOutputStream(OutputStream _inputStream) {
            this._outputStream = _inputStream;
            return this;
        }

        public Builder setBufferedStreams(Boolean _bufferedStreams) {
            this._bufferedStreams = _bufferedStreams;
            return this;
        }

        public Builder setBufferSize(Integer _bufferSize) {
            this._bufferSize = _bufferSize;
            return this;
        }

        public MultipleFilesWriteStrategy build() throws Exception {
            if (_outputStream != null) {
                if (_bufferedStreams == null)
                    _bufferedStreams = false;

                if (_bufferSize == null)
                    _bufferSize = DEFAULT_BUFFER_SIZE;
                return new MultipleFilesWriteStrategy(_outputStream, _bufferedStreams, _bufferSize);
            } else {
                throw new Exception("Must initialize OutputStream and Files");
            }
        }
    }
    //endregion
}
