package com.webilize.vuzixfilemanager.utils;


import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetsHelper {

    public static void copyAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        String[] files;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            files = new String[]{};
            Log.e("tag", "Failed to get asset file list.", e);
        }
        if (files == null || files.length == 0) return;
        for (String filename : files) {
            if (filename.contains(".p12")) {
                InputStream in;
                OutputStream out;
                try {
                    in = assetManager.open(filename);
                    File folder;
                    if (filename.contains("private")) {
                        folder = new File(context.getFilesDir(), "cert");
                    } else {
                        folder = context.getFilesDir();
                    }
                    if (!folder.exists())
                        folder.mkdirs();

                    File outFile = new File(folder, filename);
                    if (!outFile.exists()) {
                        out = new FileOutputStream(outFile);
                        copyFile(in, out);
                        out.flush();
                        out.close();
                        out = null;
                    }
                    in.close();
                    in = null;

                } catch (IOException e) {
                    Log.d("tag", "Failed to copy asset file: " + filename, e);
                }
            }
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = in.read(buffer)) > -1) {
            out.write(buffer, 0, read);
        }
    }

}