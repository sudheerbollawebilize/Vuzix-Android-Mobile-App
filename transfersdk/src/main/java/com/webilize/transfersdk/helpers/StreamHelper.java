package com.webilize.transfersdk.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamHelper {

    private static final String TAG = "StreamHelper";

    // todo: check if method is correct
    public static ByteArrayOutputStream read(InputStream inputStream, int size) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        int totalRead = 0;
        byte[] data = new byte[1024];

        while (totalRead < size) {
            int read;
            if (data.length <= (size - totalRead))
                read = data.length;
            else
                read = (size - totalRead);
            if ((nRead = inputStream.read(data, 0, read)) == -1)
                return null;
            buffer.write(data, 0, nRead);
            totalRead += nRead;
        }
        return buffer;
    }

    /**
     * @param inputStream
     * @param outputStream
     * @param bufferSize
     * @param size
     * @throws IOException
     */
    public static void parse(InputStream inputStream, OutputStream outputStream, int bufferSize, long size, ProgressListener listener) throws IOException {

        byte[] data = new byte[(int) Math.min(((long) bufferSize), size)];
        int nRead;
        final long start = System.currentTimeMillis();
        int totalRead = 0;

        while (totalRead < size) {
            long read;
            if (data.length <= (size - totalRead))
                read = data.length;
            else
                read = (size - totalRead);
            if ((nRead = inputStream.read(data, 0, (int) read)) == -1)
                throw new IOException("End of stream");
            outputStream.write(data, 0, nRead);
            totalRead += nRead;

            if (listener != null) {
                listener.progress(nRead, totalRead);
            }
        }
        //Log.d(TAG, "Elapsed " + (System.currentTimeMillis() - start) + " ms");
    }

    public interface ProgressListener {
        void progress(long currentRead, long totalRead);
    }

}
