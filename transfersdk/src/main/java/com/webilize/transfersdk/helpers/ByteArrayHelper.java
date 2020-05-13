package com.webilize.transfersdk.helpers;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This class deals with the conversion to/from Bytes.
 */
public class ByteArrayHelper {

    private static final String TAG = "ByteArrayHelper";

    public static byte[] toByteArray(int allocate, long value) {
        try {
            return ByteBuffer.allocate(allocate).putLong(value).array();
        } catch (Exception ex) {
            Log.e(TAG, "toByteArray(long): " + value, ex);
            return toByteArray(allocate, (int) value);
        }
    }

    public static byte[] toByteArray(int allocate, int value) {
        return ByteBuffer.allocate(allocate).putInt(value).array();
    }

    public static byte[] toByteArray(int allocate, byte[] value) {
        return ByteBuffer.allocate(allocate).put(value).array();
    }

    public static void copyByteArray(byte[] base, byte[] copy, int start) {
        for (int i = 0; i < copy.length; i++) {
            base[start + i] = copy[i];
        }
//        todo: This method is better. But we have to do proper tests before using it.
        /*System.arraycopy(copy, 0, base, start + 0, copy.length);*/
    }

    public static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        int j = 0;
        while (j < bytes.length && bytes[j] == 0) {
            ++j;
        }

        return Arrays.copyOfRange(bytes, j, i + 1);
    }

}
