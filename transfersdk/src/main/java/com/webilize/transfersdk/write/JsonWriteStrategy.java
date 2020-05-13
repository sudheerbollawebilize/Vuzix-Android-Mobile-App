package com.webilize.transfersdk.write;

import com.webilize.transfersdk.Protocol;
import com.webilize.transfersdk.helpers.ByteArrayHelper;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public class JsonWriteStrategy extends IWriteStrategy {
    public static final byte TYPE = 'J';
    //region sizes header
    private static final int N_SIZE_TYPE = 1;
    private static final int N_SIZE_BYTES = 4;
    //endregion
    private static final int MAX_MESSAGE_SIZE = (int) Math.pow(2, 8 * N_SIZE_BYTES);

    private JSONObject jsonObject;

    private byte type = Protocol.JSON;

    private JsonWriteStrategy(OutputStream outputStream, JSONObject jsonObject, boolean bufferedStreams, int bufferSize, byte _type) {
        this.jsonObject = jsonObject;
        this.outputStream = outputStream;
        this.bufferedStreams = bufferedStreams;
        this.bufferSize = bufferSize;
        this.type = _type;
    }

    @Override
    protected byte[] header() {
        byte[] header = new byte[1 + N_SIZE_BYTES];
        header[0] = this.type;

        long size = getContentSize();
        byte[] sizeBytes = ByteArrayHelper.toByteArray(N_SIZE_BYTES, size);
        ByteArrayHelper.copyByteArray(header, sizeBytes, N_SIZE_TYPE);
        return header;
    }

    @Override
    protected InputStream getInputStream() throws FileNotFoundException {
        return new ByteArrayInputStream(jsonObject.toString().getBytes(DEFAULT_CHARSET));
    }

    @Override
    protected long getContentSize() {
        return jsonObject.toString().getBytes(DEFAULT_CHARSET).length;
    }

    //region Builder
    public static class Builder {
        private OutputStream _outputStream;
        private JSONObject _jsonObject;
        private Boolean _bufferedStreams;
        private Integer _bufferSize;
        private byte _type = Protocol.JSON;

        public JsonWriteStrategy.Builder setOutputStream(OutputStream _outputStream) {
            this._outputStream = _outputStream;
            return this;
        }

        public JsonWriteStrategy.Builder setJSON(JSONObject file) throws Exception {
            this._jsonObject = file;

            if (file.length() > MAX_MESSAGE_SIZE) {
                throw new Exception("JSON too big");
            }
            return this;
        }

        public JsonWriteStrategy.Builder setBufferedStreams(Boolean _bufferedStreams) {
            this._bufferedStreams = _bufferedStreams;
            return this;
        }

        public JsonWriteStrategy.Builder setBufferSize(Integer _bufferSize) {
            this._bufferSize = _bufferSize;
            return this;
        }

        public JsonWriteStrategy build() throws Exception {
            if (_outputStream != null && _jsonObject != null) {
                if (_bufferedStreams == null)
                    _bufferedStreams = false;

                if (_bufferSize == null)
                    _bufferSize = DEFAULT_BUFFER_SIZE;
                return new JsonWriteStrategy(_outputStream, _jsonObject, _bufferedStreams, _bufferSize, _type);


            } else {
                throw new Exception("Must initialize OutputStream and JsonObject");
            }
        }

        public Builder setType(byte type) {
            _type = type;
            return this;
        }
    }
    //endregion
}

