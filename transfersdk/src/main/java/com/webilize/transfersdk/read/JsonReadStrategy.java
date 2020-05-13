package com.webilize.transfersdk.read;


import android.util.Log;

import com.webilize.transfersdk.Metadata;
import com.webilize.transfersdk.Protocol;
import com.webilize.transfersdk.helpers.StreamHelper;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class JsonReadStrategy extends IReadStrategy<JSONObject> {
    private static final byte TYPE = Protocol.JSON;
    //region sizes header
    private static final int N_SIZE_BYTES = 4;
    //endregion
    private static final int MAX_MESSAGE_SIZE = (int) Math.pow(2, 8 * N_SIZE_BYTES);

    private long size = 0;

    private ByteArrayOutputStream outputStream;

    private JsonReadStrategy(InputStream inputStream, boolean bufferedStreams, int bufferSize) {
        this.inputStream = inputStream;
        this.bufferedStreams = bufferedStreams;
        this.bufferSize = bufferSize;
        outputStream = new ByteArrayOutputStream();
    }

    @Override
    OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    void readHeader() throws Exception {
        //region extract size bytes
        ByteArrayOutputStream sizeBytes = StreamHelper.read(inputStream, N_SIZE_BYTES);
        if(sizeBytes != null) {
            size = ByteBuffer.wrap(sizeBytes.toByteArray()).getInt();

            if (size > MAX_MESSAGE_SIZE) {
                throw new Exception("JSON too big");
            }
        }else{
            onError("Error reading size");
        }
        //endregion

        if(emitter != null) {
            Metadata metadata = new Metadata();
            metadata.setType((char)TYPE);
            metadata.setSize(size);
            emitter.onNext(new DataWrapper(SocketState.METADATA, metadata));
        }
    }

    @Override
    JSONObject getResult() throws JSONException, IOException {
        JSONObject json = null;
        try {
            byte[] jsonObject = outputStream.toByteArray();
            String str = new String(jsonObject, DEFAULT_CHARSET);
            json = new JSONObject(str);
            outputStream = null;

            Log.d(TAG, "JSON: " + str);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    protected long getContentSize() {
        return size;
    }


    //region Builder
    public static class Builder {
        private InputStream _inputStream;
        private Boolean _bufferedStreams;
        private Integer _bufferSize;

        public Builder setInputStream(InputStream _inputStream) {
            this._inputStream = _inputStream;
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

        public JsonReadStrategy build() throws Exception {
            if (_inputStream != null) {
                if (_bufferedStreams == null)
                    _bufferedStreams = false;

                if (_bufferSize == null)
                    _bufferSize = DEFAULT_BUFFER_SIZE;
                return new JsonReadStrategy(_inputStream, _bufferedStreams, _bufferSize);
            } else {
                throw new Exception("Must initialize InputStream");
            }
        }
    }
    //endregion
}

