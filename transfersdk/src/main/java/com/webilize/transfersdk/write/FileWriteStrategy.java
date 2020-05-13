package com.webilize.transfersdk.write;


import android.annotation.SuppressLint;

import com.webilize.transfersdk.helpers.ByteArrayHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Reads a File InputStream and write the result to a socket OutputStream
 */
public class FileWriteStrategy extends IWriteStrategy {

    public static final byte TYPE = 'F';
    //region sizes header
    private static final int N_SIZE_TYPE = 1;
    private static final int N_SIZE_BYTES = 4;
    private static final int N_FILENAME_BYTES = 50;
    private static final int N_TIMESTAMP_BYTES = 12;
    //endregion
    public static final int MAX_MESSAGE_SIZE = (int) Math.pow(2, 8 * N_SIZE_BYTES);

    private File fileIn;

    private FileWriteStrategy(OutputStream outputStream, File fileIn, boolean bufferedStreams, int bufferSize) {
        this.fileIn = fileIn;
        this.outputStream = outputStream;
        this.bufferedStreams = bufferedStreams;
        this.bufferSize = bufferSize;
    }

    @Override
    protected byte[] header() {
        byte[] header = new byte[1 + N_SIZE_BYTES + N_FILENAME_BYTES + N_TIMESTAMP_BYTES];

        header[0] = TYPE;
        byte[] sizeBytes = ByteArrayHelper.toByteArray(N_SIZE_BYTES, fileIn.length());
        byte[] fileName =
                ByteArrayHelper.toByteArray(N_FILENAME_BYTES, fileIn.getName().getBytes(DEFAULT_CHARSET));

        ByteArrayHelper.copyByteArray(header, sizeBytes, N_SIZE_TYPE);
        ByteArrayHelper.copyByteArray(header, fileName, N_SIZE_BYTES + N_SIZE_TYPE);


        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        Date date = new Date(fileIn.lastModified());
        String dateTime = dateFormat.format(date);

        byte[] timestampBytes = dateTime.getBytes();
        ByteArrayHelper.copyByteArray(header, timestampBytes, N_SIZE_BYTES + N_SIZE_TYPE + N_FILENAME_BYTES);

        return header;
    }

    @Override
    protected InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(fileIn);
    }

    @Override
    protected long getContentSize() {
        return fileIn.length();
    }

    //region Builder
    public static class Builder {
        private OutputStream _outputStream;
        private File _file;
        private Boolean _bufferedStreams;
        private Integer _bufferSize;

        public Builder setOutputStream(OutputStream _inputStream) {
            this._outputStream = _inputStream;
            return this;
        }

        public Builder setFile(File file) throws Exception {
            this._file = file;
            if (!file.exists()) {
                throw new Exception("File does not exist");
            }

            if (!file.isFile()) {
                throw new Exception("File is not valid");
            }

            if (file.length() > MAX_MESSAGE_SIZE) {
                throw new Exception("File too big");
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

        public FileWriteStrategy build() throws Exception {
            if (_outputStream != null && _file != null) {
                if (_bufferedStreams == null)
                    _bufferedStreams = false;

                if (_bufferSize == null)
                    _bufferSize = DEFAULT_BUFFER_SIZE;
                return new FileWriteStrategy(_outputStream, _file, _bufferedStreams, _bufferSize);
            } else {
                throw new Exception("Must initialize OutputStream and File");
            }
        }
    }
    //endregion
}

