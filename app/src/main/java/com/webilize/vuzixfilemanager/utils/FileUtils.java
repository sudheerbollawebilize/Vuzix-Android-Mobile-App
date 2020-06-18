package com.webilize.vuzixfilemanager.utils;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.mjdev.libaums.fs.UsbFile;
import com.webilize.vuzixfilemanager.BuildConfig;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.models.Memory;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FileUtils {

//    public static boolean isImageFile(String path) {
//        String mimeType = URLConnection.guessContentTypeFromName(path);
//        return mimeType != null && mimeType.startsWith("image");
//    }

    public static boolean isImage(String mimeType) {
        return !TextUtils.isEmpty(mimeType) && (mimeType.contains("image") || mimeType.contains("gif"));
    }

    public static boolean isVideo(String mimeType) {
        return !TextUtils.isEmpty(mimeType) && mimeType.contains("video");
    }

    public static boolean isAudio(String mimeType) {
        return !TextUtils.isEmpty(mimeType) && mimeType.contains("audio");
    }

    public static String getFileNameWthoutExtension(File file) {
        String name = file.getName();
        if (TextUtils.isEmpty(name)) return "";
        if (name.contains(".")) {
            int pos = name.lastIndexOf(".");
            if (pos == -1) return name;
            return name.substring(0, pos);
        } else return name;
    }

    public static String getFileNameWthoutExtension(UsbFile file) {
        String name = file.getName();
        if (TextUtils.isEmpty(name)) return "";
        if (name.contains(".")) {
            int pos = name.lastIndexOf(".");
            if (pos == -1) return name;
            return name.substring(0, pos);
        } else return name;
    }

    public static boolean isPDF(String name) {
        return name.toLowerCase().endsWith(".pdf");
    }

    public static String getExtensionByStringHandling(String filename) {
        if (TextUtils.isEmpty(filename)) return "";
        if (filename.contains(".")) {
            int pos = filename.lastIndexOf(".") + 1;
            return filename.substring(pos);
        } else return "";
    }

    public static void loadListThumbnailWithGlide(View itemView, File file, ImageView imageView) {
        if (file.isDirectory()) return;
        Glide.with(itemView)
                .load(file)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).format(DecodeFormat.PREFER_RGB_565).override(150))
                .centerInside()
                .into(imageView);
//    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).override(150))
//    format(DecodeFormat.PREFER_ARGB_8888)); //A bit higher memory consumption
//    format(DecodeFormat.PREFER_RGB_565));//A bit lower memory consumption
    }

    public static void loadListThumbnailWithGlide(View itemView, File file, ImageView imageView, int error) {
        if (file.isDirectory()) return;
        Glide.with(itemView)
                .load(file)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).format(DecodeFormat.PREFER_RGB_565).error(error).placeholder(error))
                .centerCrop()
                .into(imageView);
//    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).override(150))
//    format(DecodeFormat.PREFER_ARGB_8888)); //A bit higher memory consumption
//    format(DecodeFormat.PREFER_RGB_565));//A bit lower memory consumption
    }

    public static void loadListThumbnailWithGlide(File file, ImageView imageView, int error) {
        if (file.isDirectory()) return;
        Glide.with(imageView.getContext())
                .load(file)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).format(DecodeFormat.PREFER_RGB_565).error(error).placeholder(error))
                .placeholder(error)
                .centerCrop()
                .into(imageView);
//    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).override(150))
//    format(DecodeFormat.PREFER_ARGB_8888)); //A bit higher memory consumption
//    format(DecodeFormat.PREFER_RGB_565));//A bit lower memory consumption
    }

    public static void loadListThumbnailWithGlide(UsbFile file, ImageView imageView, int error) {
        if (file.isDirectory()) return;
        Glide.with(imageView.getContext())
                .load(file)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).format(DecodeFormat.PREFER_RGB_565).error(error).placeholder(error))
                .placeholder(error)
                .centerCrop()
                .into(imageView);
//    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).override(150))
//    format(DecodeFormat.PREFER_ARGB_8888)); //A bit higher memory consumption
//    format(DecodeFormat.PREFER_RGB_565));//A bit lower memory consumption
    }

    public static void loadImageWithGlide(File file, ImageView imageView, int error) {
        if (file.isDirectory()) return;
        Glide.with(imageView.getContext())
                .load(file)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).format(DecodeFormat.PREFER_RGB_565).error(error).placeholder(error))
                .placeholder(error)
                .into(imageView);
    }

    public static void loadImageWithGlide(UsbFile file, ImageView imageView, int error) {
        if (file.isDirectory()) return;
        Glide.with(imageView.getContext())
                .load(file)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).format(DecodeFormat.PREFER_RGB_565).error(error).placeholder(error))
                .placeholder(error)
                .into(imageView);
    }

    public static boolean isWord(String name) {
        return name.toLowerCase().endsWith(".doc") || name.toLowerCase().endsWith(".docx");
    }

    public static boolean isSpreadSheet(String name) {
        return name.toLowerCase().endsWith(".xls") || name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xlw") || name.toLowerCase().endsWith(".xlt");
    }

    public static boolean isPresentation(String name) {
        return name.toLowerCase().endsWith(".ppt") || name.toLowerCase().endsWith(".pptx");
    }

    public static boolean isTextFile(String name) {
        return name.toLowerCase().endsWith(".txt");
    }

    public static String getFormattedDate(Date date) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date);
    }

    public static String getByteCount(File file, boolean si) {
        long bytes = file.length();
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void performCopy(Context context, String pathWithExt, File dest) {
        File source = new File(pathWithExt);
        if (source.exists()) {
            if (source.isFile()) {
                copyFiles(context, pathWithExt, dest);
            } else if (source.isDirectory()) {
                try {
                    copyDirectory(context, new File(pathWithExt), dest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else StaticUtils.showToast(context, "File to copy is not available");
    }

    public static void copyFiles(Context context, String pathWithExt, File dest) {
        File source = new File(pathWithExt);
        String filename = pathWithExt.substring(pathWithExt.lastIndexOf("/") + 1);
        String destinationPath = dest.getAbsolutePath();
        File destination = new File(destinationPath, filename);
        if (destination.exists()) {
            destination = new File(destinationPath, "copy_" + filename);
            if (!destination.exists()) {
                try {
                    org.apache.commons.io.FileUtils.copyFile(source, destination);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else StaticUtils.showToast(context, "Already copy of the file is available");
        } else {
            try {
                org.apache.commons.io.FileUtils.copyFileToDirectory(source, destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyDirectory(Context context, File sourceLocation, File targetLocation) {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdirs();
            }
            String[] children = sourceLocation.list();
            for (int i = 0; i < sourceLocation.listFiles().length; i++) {
                copyDirectory(context, new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            if (sourceLocation.isFile()) {
                copyFiles(context, sourceLocation.getAbsolutePath(), targetLocation);
            }
        }
    }

    public static boolean createNewFolder(Context context, File currentDirectory, String newFolderName) {
        if (!currentDirectory.exists()) {
            return false;
        }
        File newDir = new File(currentDirectory, newFolderName);
        if (newDir.exists()) {
            StaticUtils.showToast(context, "Folder with same name already exists.");
            return false;
        } else {
            return newDir.mkdirs();
        }
    }

    public static String getFileSize(File file) {
        if (file == null || !file.isFile()) {
            return "0 B";
        }
        DecimalFormat format = new DecimalFormat("#.##");
        long GiB = 1024 * 1024 * 1024;
        long MiB = 1024 * 1024;
        final long KiB = 1024;
        final double length = file.length();
        if (length > GiB) {
            return format.format(length / GiB) + " GB ";
        } else if (length > MiB) {
            return format.format(length / MiB) + " MB ";
        } else if (length > KiB) {
            return format.format(length / KiB) + " KB ";
        } else
            return format.format(length) + " B ";
    }

    public static String getCompletedAndFullSize(long fullSize, int progress) {
        return getFileSize((fullSize * progress) / 100) + " / " + getFileSize(fullSize);
    }

    public static String getFileSize(long length) {
        if (length <= 0) {
            return "0 B";
        }
        DecimalFormat format = new DecimalFormat("#.##");
        long GiB = 1024 * 1024 * 1024;
        long MiB = 1024 * 1024;
        final long KiB = 1024;
        if (length > GiB) {
            return format.format(length / GiB) + " GB ";
        } else if (length > MiB) {
            return format.format(length / MiB) + " MB ";
        } else if (length > KiB) {
            return format.format(length / KiB) + " KB ";
        } else
            return format.format(length) + " B ";
    }

    public static String getFileSize(UsbFile file) {
        if (file == null || file.isDirectory()) {
            return "0 B";
        }
        DecimalFormat format = new DecimalFormat("#.##");
        long GiB = 1024 * 1024 * 1024;
        long MiB = 1024 * 1024;
        final long KiB = 1024;
        final double length = file.getLength();
        if (length > GiB) {
            return format.format(length / GiB) + " GB ";
        } else if (length > MiB) {
            return format.format(length / MiB) + " MB ";
        } else if (length > KiB) {
            return format.format(length / KiB) + " KB ";
        } else
            return format.format(length) + " B ";
    }

    public static void openShareFileIntent(Context context, Uri uri) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
//        shareIntent.setType("image/*");
        shareIntent.setType("*/*");
        context.startActivity(Intent.createChooser(shareIntent, context.getResources().getText(R.string.share)));
    }

    public static void installApk(Context context, File file) {
        Uri fileUri = Uri.fromFile(file); //for Build.VERSION.SDK_INT <= 24
        if (Build.VERSION.SDK_INT >= 24) {
            fileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, fileUri);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
       /* File path1 = Environment.getExternalStorageDirectory();
        StatFs stat1 = new StatFs(path1.getPath());
        long blockSize1 = stat1.getBlockSizeLong();
        long availableBlocks1 = stat1.getAvailableBlocksLong();
        Log.e("ava: ", FileUtils.getFileSize(availableBlocks1 * blockSize1) + "");*/
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    public static long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    public static boolean isAPKFile(String file) {
        return file.toLowerCase().endsWith(".apk");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Memory showStorageVolumes(Context context) {
        Memory internalMemory = null;
        StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (storageManager == null || storageStatsManager == null) {
            return null;
        }
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        for (StorageVolume storageVolume : storageVolumes) {
            final String uuidStr = storageVolume.getUuid();
            final UUID uuid = uuidStr == null ? StorageManager.UUID_DEFAULT : UUID.fromString(uuidStr);
            try {
                long free = storageStatsManager.getFreeBytes(uuid);
                long total = storageStatsManager.getTotalBytes(uuid);
                Log.d("AppLog", "storage:" + uuid + " : " + storageVolume.getDescription(context) + " : " + storageVolume.getState());
                Log.d("AppLog", "getFreeBytes:" + Formatter.formatShortFileSize(context, free));
                Log.d("AppLog", "getTotalBytes:" + Formatter.formatShortFileSize(context, total));

                internalMemory = new Memory(FileUtils.getFileSize(total - free),
                        Formatter.formatShortFileSize(context, free),
                        Formatter.formatShortFileSize(context, total));
            } catch (Exception e) {
                // IGNORED
            }
        }
        return internalMemory;
    }

}
