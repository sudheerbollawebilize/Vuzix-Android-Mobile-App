package com.webilize.transfersdk.read;

import android.util.Log;

import com.webilize.transfersdk.Protocol;
import com.webilize.transfersdk.helpers.StreamHelper;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.subjects.Subject;


public class MultipleFilesReadStrategy {
    private static final String TAG = "MultipleFilesIRead";
    private static final byte TYPE = Protocol.MULTIPLE;

    protected static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    protected boolean bufferedStreams;
    protected int bufferSize;
    private InputStream inputStream;
    protected Subject<DataWrapper> emitter;
    private File folder;

    private long currentRead;

    public MultipleFilesReadStrategy(InputStream inputStream, File folder, Boolean bufferedStreams, Integer bufferSize) {
        this.inputStream = inputStream;
        this.folder = folder;
        this.bufferedStreams = bufferedStreams;
        this.bufferSize = bufferSize;
        currentRead = 0;
    }

    public List<File> read() throws Exception {
        ArrayList<File> result = new ArrayList<>();

        JsonReadStrategy jsonStrategy = new JsonReadStrategy.Builder()
                .setInputStream(inputStream)
                .setBufferedStreams(bufferedStreams)
                .setBufferSize(bufferSize)
                .build();
        //jsonStrategy.setEmitter(emitter);
        JSONObject jsonObject = jsonStrategy.read();

        JSONArray filesArray = jsonObject.getJSONArray("files");
        int i = 0;
        int totalSize = 0;
        while (i < filesArray.length()) {
            JSONObject fileObject = filesArray.getJSONObject(i);
            totalSize += fileObject.getInt("size");
            i++;
        }

        Log.d(TAG, "total size: " + totalSize);

        //todo: use the emitter to send metadata ???

        final long finalTotalSize = totalSize;

        i = 0;
        while (i < filesArray.length()) {
            ByteArrayOutputStream byteArrayOutputStream = StreamHelper.read(inputStream, 1);
            if (byteArrayOutputStream == null) {
                throw new Exception("Connection failed");
            }
            byte[] typeBytes = byteArrayOutputStream.toByteArray();

            // todo: check type
            FileReadStrategy fileReadStrategy = new FileReadStrategy.Builder()
                    .setFolder(folder)
                    .setInputStream(inputStream)
                    .setBufferedStreams(bufferedStreams)
                    .setBufferSize(bufferSize)
                    .build();

            if (emitter != null)
                fileReadStrategy.setListener((n, totalRead) -> {
                    currentRead += n;
                    long percent = currentRead * 100 / finalTotalSize;
                    //Log.d(TAG, "percent: " + percent);
                    emitter.onNext(new DataWrapper(SocketState.PROGRESS, (int) percent));
                });

            File file = fileReadStrategy.read();
            result.add(file);
            i++;
        }


        return result;
    }

    public void setEmitter(Subject<DataWrapper> emitter) {
        this.emitter = emitter;
    }


    //region Builder
    public static class Builder {
        private InputStream _inputStream;
        private File _folder;
        private Boolean _bufferedStreams;
        private Integer _bufferSize;

        public Builder setInputStream(InputStream _inputStream) {
            this._inputStream = _inputStream;
            return this;
        }

        public Builder setFolder(File _folder) throws Exception {
            this._folder = _folder;
            if (!_folder.exists()) {
                _folder.mkdir();
            }

            if (!_folder.isDirectory()) {
                throw new Exception("Folder is not valid");
            }

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

        public MultipleFilesReadStrategy build() throws Exception {
            if (_inputStream != null && _folder != null) {
                if (_bufferedStreams == null)
                    _bufferedStreams = false;

                if (_bufferSize == null)
                    _bufferSize = DEFAULT_BUFFER_SIZE;

                return new MultipleFilesReadStrategy(_inputStream, _folder, _bufferedStreams, _bufferSize);
            } else {
                throw new Exception("Must initialize InputStream and Folder");
            }
        }
    }
    //endregion
}
