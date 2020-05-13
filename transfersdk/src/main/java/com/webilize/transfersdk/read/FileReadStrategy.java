package com.webilize.transfersdk.read;


import android.annotation.SuppressLint;

import com.webilize.transfersdk.Metadata;
import com.webilize.transfersdk.Protocol;
import com.webilize.transfersdk.helpers.ByteArrayHelper;
import com.webilize.transfersdk.helpers.StreamHelper;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Listen to a InputStream and write the result to a File
 */
public class FileReadStrategy extends IReadStrategy<File> {
    private static final String TAG = "MyTAG";

    //region sizes header
    private static final int N_SIZE_TYPE = 1;
    private static final int N_SIZE_BYTES = 4;
    private static final int N_FILENAME_BYTES = 50;
    private static final int N_TIMESTAMP_BYTES = 12;
    //endregion
    private static final int MAX_MESSAGE_SIZE = (int) Math.pow(2, 8 * N_SIZE_BYTES);

    private File folder;
    private File file;
    private Date date; // last modified file date
    private long size; // file size

    private FileReadStrategy(InputStream inputStream, File folder, boolean bufferedStreams, int bufferSize) {
        this.inputStream = inputStream;
        this.bufferedStreams = bufferedStreams;
        this.folder = folder;
        this.bufferSize = bufferSize;
    }

    @Override
    void readHeader() throws Exception {
        //region extract size bytes
        ByteArrayOutputStream sizeBytes = StreamHelper.read(inputStream, N_SIZE_BYTES);
        if (sizeBytes != null) {
            size = ByteBuffer.wrap(sizeBytes.toByteArray()).getInt();
            if (size > MAX_MESSAGE_SIZE) {
                throw new Exception("File too big");
            }
        } else
            onError("Error reading size");
        //endregion

        //region extract filename
        ByteArrayOutputStream filenameBytes = StreamHelper.read(inputStream, N_FILENAME_BYTES);
        String fileName = "Undefined";
        if (filenameBytes != null) {
            fileName = new String(ByteArrayHelper.trim(filenameBytes.toByteArray()), DEFAULT_CHARSET);
        } else
            onError("Error reading filename");
        //endregion

        //region extract timestamp

        ByteArrayOutputStream timestamp = StreamHelper.read(inputStream, N_TIMESTAMP_BYTES);
        if (timestamp != null) {
            String dtStart = new String(timestamp.toByteArray(), DEFAULT_CHARSET);

            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss");
            //format.setTimeZone(TimeZone.getDefault());

            date = null;
            try {
                date = format.parse(dtStart);
            } catch (Exception ex) {
                onError(ex);
            }
        } else {
            onError("Error reading timestamp");
        }

        //endregion

        if (emitter != null) {
            Metadata metadata = new Metadata();
            metadata.setType((char) Protocol.FILE);
            metadata.setFileName(fileName);
            metadata.setSize(size);

            emitter.onNext(new DataWrapper(SocketState.METADATA, metadata));
        }

        file = new File(folder, fileName);
    }

    @Override
    OutputStream getOutputStream() throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    @Override
    protected long getContentSize() {
        return size;
    }

    @Override
    File getResult() {
        if (date != null && file != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.setTimeZone(TimeZone.getDefault());
            file.setLastModified(cal.getTimeInMillis());
        }

        return file;
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

        public FileReadStrategy build() throws Exception {
            if (_inputStream != null && _folder != null) {
                if (_bufferedStreams == null)
                    _bufferedStreams = false;

                if (_bufferSize == null)
                    _bufferSize = DEFAULT_BUFFER_SIZE;

                return new FileReadStrategy(_inputStream, _folder, _bufferedStreams, _bufferSize);
            } else {
                throw new Exception("Must initialize InputStream and Folder");
            }
        }


    }
    //endregion
}

