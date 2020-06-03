package com.webilize.vuzixfilemanager.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.github.mjdev.libaums.fs.UsbFile;
import com.webilize.transfersdk.wifidirect.direct.WiFiDirectUtils;
import com.webilize.vuzixfilemanager.BaseApplication;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.dbutils.DBHelper;
import com.webilize.vuzixfilemanager.models.DeviceFavouritesModel;
import com.webilize.vuzixfilemanager.models.DeviceModel;
import com.webilize.vuzixfilemanager.services.HotSpotIntentService;
import com.webilize.vuzixfilemanager.services.RXConnectionFGService;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class StaticUtils {
    private static final String DISPLAY_DATE_TIME_FORMAT = "dd-MM-yyyy hh:mm a";
    //    public static final String storageDir = Environment.getExternalStorageDirectory().getPath() + "/" + "UReview" + "/";
    public static int SCREEN_HEIGHT, SCREEN_WIDTH;
    private static ClipboardManager clipboard;
    private static List<String> bookMarks;

    public static void setUpDeviceDefaultDetails(Context context) {
        getWindowDimensions(context);
        BaseApplication.isVuzix = isVuzix();
    }

    public static boolean isVuzixBlade() {
//        String deviceName = android.os.Build.MODEL;
        String deviceMan = Build.MANUFACTURER;
        String build = Build.PRODUCT;
//        String brand = Build.BRAND;
//        String device = Build.DEVICE;
//        deviceName: Blade ,deviceMan: vuzix ,build: blade ,brand: vuzix ,device: blade
//         deviceName: Pixel 3 ,deviceMan: Google ,build: blueline ,brand: google ,device: blueline
        return deviceMan.equalsIgnoreCase("vuzix") && build.equalsIgnoreCase("blade");
    }

    private static boolean isPackageInstalled(String packageName, Context context) {
        boolean found = true;
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            found = false;
        }
        return found;
    }

    @DrawableRes
    public static int getFileDrawable(File file) {
        int drawable = R.drawable.ic_file;
        if (file.isDirectory()) drawable = R.drawable.ic_folder;
        else {
            String path = file.getPath();
            String mimeType = URLConnection.guessContentTypeFromName(path);
            if (FileUtils.isImage(mimeType)) {
                drawable = R.drawable.ic_image;
            } else if (FileUtils.isVideo(mimeType)) {
                drawable = R.drawable.ic_video;
            } else if (FileUtils.isAudio(mimeType)) {
                drawable = R.drawable.ic_music;
            } else if (FileUtils.isPDF(path)) {
                drawable = R.drawable.ic_pdf;
            } else if (FileUtils.isPresentation(path)) {
                drawable = R.drawable.ic_ppt;
            } else if (FileUtils.isSpreadSheet(path)) {
                drawable = R.drawable.ic_excel;
            } else if (FileUtils.isTextFile(path)) {
                drawable = R.drawable.ic_txt;
            }
        }
        return drawable;
    }


    @DrawableRes
    public static int getFileDrawable(boolean isFolder) {
        return isFolder ? R.drawable.ic_folder : R.drawable.ic_file;
    }

    @DrawableRes
    public static int getFileDrawable(UsbFile file) {
        int drawable = R.drawable.ic_file;
        if (file.isDirectory()) drawable = R.drawable.ic_folder;
        else {
            String path = file.getAbsolutePath();
            String mimeType = URLConnection.guessContentTypeFromName(path);
            if (FileUtils.isImage(mimeType)) {
                drawable = R.drawable.ic_image;
            } else if (FileUtils.isVideo(mimeType)) {
                drawable = R.drawable.ic_video;
            } else if (FileUtils.isAudio(mimeType)) {
                drawable = R.drawable.ic_music;
            } else if (FileUtils.isPDF(path)) {
                drawable = R.drawable.ic_pdf;
            } else if (FileUtils.isPresentation(path)) {
                drawable = R.drawable.ic_ppt;
            } else if (FileUtils.isSpreadSheet(path)) {
                drawable = R.drawable.ic_excel;
            } else if (FileUtils.isTextFile(path)) {
                drawable = R.drawable.ic_txt;
            }
        }
        return drawable;
    }

    public static boolean isVuzix() {
//        String deviceName = android.os.Build.MODEL;
        String deviceMan = Build.MANUFACTURER;
//        String build = android.os.Build.PRODUCT;
//        String brand = Build.BRAND;
//        String device = Build.DEVICE;
        return deviceMan.equalsIgnoreCase("vuzix");
    }

    public static boolean checkInternetConnection(Context context) {
        NetworkInfo _activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return _activeNetwork != null && _activeNetwork.isConnectedOrConnecting();
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private static void getWindowDimensions(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        SCREEN_WIDTH = size.x;
        SCREEN_HEIGHT = size.y;
    }

    public static String getMimeType(String url) {
        String type = "";
        if (!TextUtils.isEmpty(url)) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if (!TextUtils.isEmpty(extension)) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }
        return type;
    }

    public static String getMimeTypeFromExtension(String extension) {
        String type = "";
        if (!TextUtils.isEmpty(extension)) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isAllPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void hideKeyboard(Context context, View view) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideSoftKeyboard(Activity act) {
        try {
            if (act.getCurrentFocus() != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    public static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null && children.length > 0) {
                for (String aChildren : children) {
                    try {
                        deleteDirectory(new File(dir, aChildren));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        dir.delete();
    }

    public static float convertDpToPixel(float dp, Context context) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float convertPixelsToDp(float px, Context context) {
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static List<String> getBookMarkedLocations(Context context) {
//        AppStorage.getInstance(context).setValue(AppStorage.SP_BOOKMARKED_FOLDERS, "");
        if (bookMarks == null) {
            bookMarks = new ArrayList<>();
            String bookMarkString = AppStorage.getInstance(context).getValue(AppStorage.SP_BOOKMARKED_FOLDERS, "");
            if (TextUtils.isEmpty(bookMarkString)) {
                bookMarkString = addDefaultLocations(context);
            }
            bookMarks = new LinkedList<>(Arrays.asList(bookMarkString.split(AppConstants.BOOKMARK_SEPERATOR)));
        }
        return bookMarks;
    }

    public static boolean isInDeviceFavourites(Context context, String path) {
        ArrayList<DeviceFavouritesModel> bladeItemArrayList = new ArrayList<>();
        String dev = AppStorage.getInstance(context).getValue(AppStorage.SP_DEVICE_ADDRESS, "");
        DBHelper dbHelper = new DBHelper(context);
        DeviceModel deviceModel = dbHelper.getDeviceModel(dev);
        bladeItemArrayList.addAll(dbHelper.getDeviceFavouritesModelArrayList(deviceModel.id));
        for (DeviceFavouritesModel deviceFavouritesModel : bladeItemArrayList) {
            if (deviceFavouritesModel.path.equalsIgnoreCase(path)) {
                return true;
            }
        }
        return false;
    }

    private static String addDefaultLocations(Context context) {
        String bookMarkString = AppConstants.homeDirectory.getAbsolutePath();
        bookMarkString += AppConstants.BOOKMARK_SEPERATOR + new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DCIM).getAbsolutePath();
        bookMarkString += AppConstants.BOOKMARK_SEPERATOR + new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        bookMarkString += AppConstants.BOOKMARK_SEPERATOR + new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        bookMarkString += AppConstants.BOOKMARK_SEPERATOR + new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MUSIC).getAbsolutePath();
        bookMarkString += AppConstants.BOOKMARK_SEPERATOR + new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MOVIES).getAbsolutePath();
        AppStorage.getInstance(context).setValue(AppStorage.SP_BOOKMARKED_FOLDERS, bookMarkString);
        return bookMarkString;
    }

    public static void addSavedLocationToStorage(Context context, String path) {
        String bookMarkString = AppStorage.getInstance(context).getValue(AppStorage.SP_BOOKMARKED_FOLDERS, "");
        bookMarkString += AppConstants.BOOKMARK_SEPERATOR + path;
        AppStorage.getInstance(context).setValue(AppStorage.SP_BOOKMARKED_FOLDERS, bookMarkString);
        if (!bookMarks.contains(path)) bookMarks.add(path);
    }

    public static void removeSavedLocationFromStorage(Context context, String path) {
        bookMarks.remove(path);
        StringBuilder bookMarkString = new StringBuilder();
        for (int i = 0; i < bookMarks.size(); i++) {
            String bpath = bookMarks.get(i);
            if (i == 0) {
                bookMarkString.append(bpath);
            } else bookMarkString.append(AppConstants.BOOKMARK_SEPERATOR).append(bpath);
        }
        AppStorage.getInstance(context).setValue(AppStorage.SP_BOOKMARKED_FOLDERS, bookMarkString.toString());
    }

    //    https://www.codejava.net/java-se/file-io/how-to-copy-a-directory-programmatically-in-java

    public static void copyTextToClipBoard(Context context, String textToCopy) {
        try {
            if (clipboard == null)
                clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("selectedFilePath", textToCopy);
            clipboard.setPrimaryClip(clip);
            showToast(context, context.getString(R.string.copied_to_clipboard));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFilesToClipBoard(Context context, String[] filePaths) {
        try {
            AppStorage.getInstance(context).setClipBoardPath(filePaths);
            showToast(context, context.getString(R.string.copied_to_clipboard));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFileToClipBoard(Context context, String filePath) {
        try {
            AppStorage.getInstance(context).setClipBoardPath(new String[]{filePath});
            showToast(context, context.getString(R.string.copied_to_clipboard));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] getFilesFromClipBoard(Context context) {
        try {
            return AppStorage.getInstance(context).getClipBoardPaths();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkWifiOnAndConnected(Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            return wifiInfo.getNetworkId() != -1;
        } else {
            return false;
        }
    }

    public static boolean isWifiOn(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connMgr.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connMgr.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
    }

    public static boolean isPrimaryTextAvailableInClipboard(Context context) {
        if (clipboard == null)
            clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        return clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null && (clipboard.getPrimaryClip().getItemAt(0) != null &&
                !TextUtils.isEmpty(clipboard.getPrimaryClip().getItemAt(0).getText()));
    }

    public static boolean isPrimaryUriAvailableInClipboard(Context context) {
        String[] strings = AppStorage.getInstance(context).getClipBoardPaths();
        return strings != null && strings.length != 0;
    }

    public static void pasteTextToClipBoard(Context context, String textToCopy) {
        try {
            if (clipboard == null)
                clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("selectedFilePath", textToCopy);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isExternalStorageReadOnly() {
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState());
    }

    public static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static String getDeviceName(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences(WiFiDirectUtils.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            return sp.getString(WiFiDirectUtils.WIFI_DIRECT_REMOTE_DEVICE_NAME, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String connectionType = "";

    public static String getConnectionType() {
        return connectionType;
    }

    public static void setConnectionType(String type) {
        connectionType = type;
    }

    public static void turnOnHotSpot(Context c) {
        Uri uri = new Uri.Builder().scheme(AppConstants.DATA_SCHEME).authority(AppConstants.DATA_SCHEME_ON).build();
        showToast(c, "Turn on. Uri: " + uri.toString());
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(uri);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        c.startActivity(i);
        HotSpotIntentService.start(c, i);

        Intent serviceIntent = new Intent(c, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "start");
        serviceIntent.putExtra("IsQr", false);
        ContextCompat.startForegroundService(c, serviceIntent);

    }

    public static void turnOffHotSpot(Context c) {
        Uri uri = new Uri.Builder().scheme(AppConstants.DATA_SCHEME).authority(AppConstants.DATA_SCHEME_OFF).build();
        showToast(c, "Turn off. Uri: " + uri.toString());
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(uri);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        c.startActivity(i);
        HotSpotIntentService.start(c, i);

        Intent serviceIntent = new Intent(c, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "stop");
        ContextCompat.startForegroundService(c, serviceIntent);
    }

    public static void sendImplicitBroadcast(Context ctxt, Intent i) {
        PackageManager pm = ctxt.getPackageManager();
        List<ResolveInfo> matches = pm.queryBroadcastReceivers(i, 0);
        for (ResolveInfo resolveInfo : matches) {
            Intent explicit = new Intent(i);
            ComponentName cn = new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name);
            explicit.setComponent(cn);
            ctxt.sendBroadcast(explicit);
        }
    }

}
