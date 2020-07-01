package com.webilize.vuzixfilemanager.viewmodels;

import android.app.Application;
import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.github.mjdev.libaums.fs.UsbFile;
import com.webilize.vuzixfilemanager.BaseApplication;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.io.comparator.SizeFileComparator;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import io.reactivex.Completable;
import io.reactivex.Single;

import static android.content.Context.DOWNLOAD_SERVICE;

public class FolderViewModel extends AndroidViewModel {

    private static final String TAG = "FolderViewModel";

    private ArrayList<FileFolderItem> currentFiles = new ArrayList<>();
    private ArrayList<WifiP2pDevice> availableDevices = new ArrayList<>();
    private ArrayList<WifiP2pDevice> connectedDevices = new ArrayList<>();
    //    private ArrayList<FileFolderItem> homeFolderFiles = new ArrayList<>();
    private DownloadManager downloadManager = (DownloadManager) getApplication().getSystemService(DOWNLOAD_SERVICE);
    private FileFolderItem currentFileFolderItem;

    public FolderViewModel(@NonNull Application application) {
        super(application);
    }

    public ArrayList<FileFolderItem> getCurrentFiles() {
        return currentFiles;
    }

    public Completable getAllVideos() {
        return Completable.fromAction(() -> {
            currentFiles = new ArrayList<>();
            Uri uri;
            Cursor cursor;
            int column_index_data;
            String PathOfImage = "";
            uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
            cursor = BaseApplication.getInstance().getContentResolver().query(uri, projection, null, null, null);
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                PathOfImage = cursor.getString(column_index_data);
                currentFiles.add(new FileFolderItem(new File(PathOfImage)));
            }
            Log.d(TAG, currentFiles.size() + " file(s)");
        });
    }

    public Completable getAllAudio() {
        return Completable.fromAction(() -> {
            currentFiles = new ArrayList<>();
            Uri uri;
            Cursor cursor;
            int column_index_data;
            String PathOfImage = "";
            uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.MediaColumns.DATA};
            cursor = BaseApplication.getInstance().getContentResolver().query(uri, projection, null, null, null);
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                PathOfImage = cursor.getString(column_index_data);
                currentFiles.add(new FileFolderItem(new File(PathOfImage)));
            }
            Log.d(TAG, currentFiles.size() + " file(s)");
        });
    }

    public Completable getAllFilesRecent() {
        return Completable.fromAction(() -> {
            currentFiles = new ArrayList<>();
            Uri uri;
            Cursor cursor;
            int column_index_data;
            String PathOfImage = "";
            uri = android.provider.MediaStore.Files.getContentUri(AppConstants.homeDirectory.getAbsolutePath());
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            cursor = BaseApplication.getInstance().getContentResolver().query(uri, projection, null, null, null);
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                PathOfImage = cursor.getString(column_index_data);
                currentFiles.add(new FileFolderItem(new File(PathOfImage)));
            }
            Log.d(TAG, currentFiles.size() + " file(s)");
        });
    }

    public Completable getAllImages() {
        return Completable.fromAction(() -> {
            currentFiles = new ArrayList<>();
            Uri uri;
            Cursor cursor;
            int column_index_data;
            String PathOfImage = "";
            uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
            cursor = BaseApplication.getInstance().getContentResolver().query(uri, projection, null, null, null);
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                PathOfImage = cursor.getString(column_index_data);
                currentFiles.add(new FileFolderItem(new File(PathOfImage)));
            }
            Log.d(TAG, currentFiles.size() + " file(s)");
        });
    }

    public boolean isFolderEmpty() {
        return currentFiles == null || currentFiles.isEmpty();
    }

    public FileFolderItem getCurrentFileFolderItem() {
        if (currentFileFolderItem == null)
            currentFileFolderItem = new FileFolderItem(AppConstants.homeDirectory);
        return currentFileFolderItem;
    }

    public Completable getFiles(boolean showHidden, FileFolderItem folder) {
        if (folder == null)
            folder = new FileFolderItem(AppConstants.homeDirectory);

        currentFileFolderItem = folder;
        boolean showOnlyFiles = AppStorage.getInstance(BaseApplication.getInstance()).getValue(AppStorage.SP_SHOW_ONLY_FILES, false);
        boolean showOnlyFolders = AppStorage.getInstance(BaseApplication.getInstance()).getValue(AppStorage.SP_SHOW_ONLY_FOLDERS, false);
        boolean showEmptyFolders = AppStorage.getInstance(BaseApplication.getInstance()).getValue(AppStorage.SP_SHOW_EMPTY_FOLDERS, true);
        int mode = AppStorage.getInstance(BaseApplication.getInstance()).getValue(AppStorage.SP_SORT_DIR, AppConstants.CONST_SORT_ASC);
        return Completable.fromAction(() -> {
            if (currentFileFolderItem.file.list() != null && currentFileFolderItem.file.list().length > 0) {
                File[] filesList = currentFileFolderItem.file.listFiles();
                Arrays.sort(filesList);
                switch (BaseApplication.filterSortingMode) {
                    case AppConstants.CONST_NAME:
                        Arrays.sort(filesList, mode == AppConstants.CONST_SORT_ASC ? NameFileComparator.NAME_INSENSITIVE_COMPARATOR : NameFileComparator.NAME_INSENSITIVE_REVERSE);
                        break;
                    case AppConstants.CONST_MODIFIED:
                        Arrays.sort(filesList, mode == AppConstants.CONST_SORT_ASC ? LastModifiedFileComparator.LASTMODIFIED_COMPARATOR : LastModifiedFileComparator.LASTMODIFIED_REVERSE);
                        break;
                    case AppConstants.CONST_SIZE:
                        Arrays.sort(filesList, mode == AppConstants.CONST_SORT_ASC ? SizeFileComparator.SIZE_COMPARATOR : SizeFileComparator.SIZE_REVERSE);
                        break;
                    default:
                        break;
                }
                currentFiles = new ArrayList<>();
                if (showOnlyFiles) {
                    for (File file : filesList) {
                        if ((showHidden || !file.isHidden()) && file.isFile()) {
                            currentFiles.add(new FileFolderItem(file));
                        }
                    }
                } else if (showOnlyFolders) {
                    for (File file : filesList) {
                        if ((showEmptyFolders || (file.list() != null && file.list().length != 0))
                                && (showHidden || !file.isHidden()) && file.isDirectory()) {
                            currentFiles.add(new FileFolderItem(file));
                        }
                    }
                } else {
                    for (File file : filesList) {
                        if (file.isFile()) {
                            if ((showHidden || !file.isHidden())) {
                                currentFiles.add(new FileFolderItem(file));
                            }
                        } else if ((showEmptyFolders || (file.list() != null && file.list().length != 0))
                                && (showHidden || !file.isHidden())) {
                            currentFiles.add(new FileFolderItem(file));
                        }
                    }
                }
            }
            Log.d(TAG, currentFiles.size() + " file(s)");
        });
    }

    public void getUsbFiles(FileFolderItem folder) {
        if (folder == null)
            folder = new FileFolderItem(AppConstants.homeDirectory);

        currentFileFolderItem = folder;
        try {
            if (currentFileFolderItem.usbFile.list() != null && currentFileFolderItem.usbFile.list().length > 0) {
                UsbFile[] filesList = currentFileFolderItem.usbFile.listFiles();
                currentFiles = new ArrayList<>();
                for (UsbFile file : filesList) {
                    currentFiles.add(new FileFolderItem(file));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, currentFiles.size() + " usb file(s)");
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public FileFolderItem getFile(int position) {
        if (position >= 0 && position < currentFiles.size())
            return currentFiles.get(position);
        else return null;
    }

    public Single<Boolean> deleteFile(FileFolderItem currentFile) {
        return Single.fromCallable(() -> {
            boolean success = currentFile.file.delete();
            if (success)
                currentFiles.remove(currentFile);
            return success;
        });
    }

    public boolean deleteFileFromFolder(FileFolderItem currentFile) {
        if (currentFile.file == null) {
            if (currentFile.usbFile.isDirectory()) {
                deleteSubFolders(currentFile.usbFile);
                return true;
            } else {
                try {
                    currentFile.usbFile.delete();
                    currentFiles.remove(currentFile);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } else {
            if (currentFile.file.isFile()) {
                boolean success = currentFile.file.delete();
                if (success)
                    currentFiles.remove(currentFile);
                return success;
            } else if (currentFile.file.isDirectory()) {
                boolean success = FileUtils.deleteQuietly(currentFile.file);
                if (success)
                    currentFiles.remove(currentFile);
                return success;
            } else return false;
        }
    }

    public void deleteMultipleFiles(String[] currentFiles) {
        for (String filePath : currentFiles) {
            File file = new File(filePath);
            if (file.exists()) {
                if (file.isFile()) {
                    file.delete();
                } else if (file.isDirectory()) {
                    FileUtils.deleteQuietly(file);
                }
            }
        }
    }

    private void deleteSubFolders(UsbFile usbFile) {
        try {
            if (usbFile.isDirectory()) {
                for (UsbFile file : usbFile.listFiles()) {
                    if (file.isDirectory()) {
                        deleteSubFolders(file);
                    } else {
                        file.delete();
                    }
                }
            } else {
                usbFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addNewFolder(FileFolderItem currentFile) {
        currentFiles.add(currentFile);
    }

    public Single<Boolean> requestFile(File destFolder, String urlStrFinal) {
        return Single.fromCallable(() -> {
            try {

                String lowerUrlStr = urlStrFinal.toLowerCase();
                String urlStr = urlStrFinal;
                //region dropbox url parsing
                if (lowerUrlStr.contains("www.dropbox.")) {
                    if (lowerUrlStr.endsWith("?dl=0")) {
                        urlStr = urlStr.substring(0, urlStr.length() - 1).concat("1");
                    }
                }
                //endregion

                //region drive url parsing
                // example: 1g3qno0aC88yJlyX970IijYoBQ1H1_QIm

                if (lowerUrlStr.contains("://drive.google.")) {
                    if (!lowerUrlStr.contains("export=download")) {
                        String id = "";
                        String pattern = "/file/d/";
                        int index = lowerUrlStr.indexOf(pattern);
                        if (index > 0) {
                            id = urlStr.substring(index + pattern.length());
                        }

                        //todo: do it for docs, spreadsheets.... https://www.labnol.org/internet/direct-links-for-google-drive/28356/

                        if (id.length() < 1)
                            return false;
                        else
                            id = id.substring(0, id.indexOf("/"));

                        urlStr = "https://drive.google.com/uc?export=download&id=" + id; // 1g3qno0aC88yJlyX970IijYoBQ1H1_QIm
                    }
                }
                //endregion

                Log.d(TAG, "URL: " + urlStr);

                //region reading header
                URL url = new URL(urlStr);
                String fileName = "";

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("HEAD");
                con.connect();

                String contentType = con.getHeaderField("Content-Type");
                if (contentType != null) {
                    Log.d(TAG, "Content-Type: " + contentType);
                }

                String contentDisposition = con.getHeaderField("Content-Disposition");
                if (contentDisposition != null) {
                    Log.d(TAG, "Content-Disposition: " + contentDisposition);
                    int index = contentDisposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = contentDisposition.substring(index + 10, contentDisposition.length() - 1);
                    }

                    index = fileName.indexOf("\"");
                    if (index > 0) {
                        fileName = fileName.substring(0, index);
                    }
                } else {
                    fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1);
                }
                //endregion

                Log.d(TAG, "fileName: " + fileName);

                //region creating DownloadManager request
                File folder = new File(destFolder, fileName);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlStr))
                        .setTitle("Downloading " + fileName)// Title of the Download Notification
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                        .setDestinationUri(Uri.fromFile(folder))// Uri of the destination file
                        .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                        .setAllowedOverRoaming(true);// Set if download is allowed on roaming network

                downloadManager.enqueue(request);

                //endregion
                return true;
            } catch (Exception ex) {
                Log.e(TAG, "requestFile ", ex);
            }
            return false;
        });
    }

    public void requestDownloadFile(File destFolder, String urlStrFinal) {
        AsyncTask.execute(() -> {
            try {

                String lowerUrlStr = urlStrFinal.toLowerCase();
                String urlStr = urlStrFinal;
                //region dropbox url parsing
                if (lowerUrlStr.contains("www.dropbox.")) {
                    if (lowerUrlStr.endsWith("?dl=0")) {
                        urlStr = urlStr.substring(0, urlStr.length() - 1).concat("1");
                    }
                }
                //endregion
                //region drive url parsing
                // example: 1g3qno0aC88yJlyX970IijYoBQ1H1_QIm
                if (lowerUrlStr.contains("://drive.google.")) {
                    if (!lowerUrlStr.contains("export=download")) {
                        String id = "";
                        String pattern = "/file/d/";
                        int index = lowerUrlStr.indexOf(pattern);
                        if (index > 0) {
                            id = urlStr.substring(index + pattern.length());
                        }
                        //todo: do it for docs, spreadsheets.... https://www.labnol.org/internet/direct-links-for-google-drive/28356/
                        if (id.length() < 1)
                            return;
                        else
                            id = id.substring(0, id.indexOf("/"));
                        urlStr = "https://drive.google.com/uc?export=download&id=" + id; // 1g3qno0aC88yJlyX970IijYoBQ1H1_QIm
                    }
                }
                //endregion
                Log.d(TAG, "URL: " + urlStr);
                //region reading header
                URL url = new URL(urlStr);
                String fileName = "";

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("HEAD");
                con.connect();

                String contentType = con.getHeaderField("Content-Type");
                String contentDisposition = con.getHeaderField("Content-Disposition");
                if (contentDisposition != null) {
                    Log.d(TAG, "Content-Disposition: " + contentDisposition);
                    int index = contentDisposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = contentDisposition.substring(index + 10, contentDisposition.length() - 1);
                    }
                    index = fileName.indexOf("\"");
                    if (index > 0) {
                        fileName = fileName.substring(0, index);
                    }
                } else {
                    fileName = "drive_file_" + Calendar.getInstance().getTimeInMillis();
                }
                //endregion
                Log.d(TAG, "fileName: " + fileName);
//                Intent i = new Intent(Intent.ACTION_VIEW);
//                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                i.setData(Uri.parse(urlStrFinal));
//                context.startActivity(i);

                //region creating DownloadManager request
                File folder = new File(destFolder, fileName);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlStr))
                        .setTitle("Downloading " + fileName)// Title of the Download Notification
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                        .setDestinationUri(Uri.fromFile(folder))// Uri of the destination file
                        .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                        .setAllowedOverRoaming(true);// Set if download is allowed on roaming network

                downloadManager.enqueue(request);
                StaticUtils.showToast(BaseApplication.getInstance(), "Downloaded Successfully.");
            } catch (Exception ex) {
                Log.e(TAG, "requestFile ", ex);
            }
        });

        //endregion

    }

    public ArrayList<WifiP2pDevice> getAvailableWifiP2PDevices() {
        return availableDevices;
    }

    public void updateAvailableWifiP2PDevices(ArrayList<WifiP2pDevice> newAvailableDevices) {
        if (availableDevices == null) availableDevices = new ArrayList<>();
        else availableDevices.clear();
        if (newAvailableDevices == null || newAvailableDevices.isEmpty()) return;
        availableDevices.addAll(newAvailableDevices);
    }

    public ArrayList<WifiP2pDevice> getConnectedWifiP2PDevices() {
        return connectedDevices;
    }

    public void updateConnectedWifiP2PDevices(int position) {
        if (connectedDevices == null) connectedDevices = new ArrayList<>();
        if (!availableDevices.isEmpty()) {
            connectedDevices.add(availableDevices.get(position));
            availableDevices.remove(availableDevices.get(position));
        }
    }

    public void clearAllWifiP2PDevices() {
        if (connectedDevices == null) connectedDevices = new ArrayList<>();
        else connectedDevices.clear();
        if (availableDevices == null) availableDevices = new ArrayList<>();
        else availableDevices.clear();
    }

    public void removeConnectedWifiP2PDevices(int position) {
        if (connectedDevices == null || connectedDevices.isEmpty()) return;
        if (position == -1) {
            connectedDevices.clear();
            return;
        }
        availableDevices.add(connectedDevices.get(position));
        connectedDevices.remove(connectedDevices.get(position));
    }

}