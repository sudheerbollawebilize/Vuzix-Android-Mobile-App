package com.webilize.vuzixfilemanager.utils;

import android.os.Environment;

import java.io.File;

public class AppConstants {

    public static final String NOTIFICATION_CHANNEL = "FILE_TRANSFER_MANAGER_CHANNEL";

    public static final int PERMISSIONS_REQUEST_READ_WRITE_EXTERNAL_STORAGE = 1001;
    public static final int PERMISSIONS_REQUEST_LOCATION = 1002;
    public static final int BACK_PRESSED_TIME = 2000;

    public static final int REQUEST_SEARCH = 1;
    public static final int REQUEST_BLADE_FOLDERS = 2;
    public static final String ReservedChars = "|\\?*<\":>+\\[\\]/'";

    public static final String BOOKMARK_SEPERATOR = "_///_";

    //    List Modes
    public static final int SHOW_LIST = 001;
    public static final int SHOW_GRID = 002;

    //    Sort Modes
    public static final int CONST_NAME = 0;
    public static final int CONST_MODIFIED = 1;
    public static final int CONST_SIZE = 2;
//    public static final int CONST_TYPE = 3;

    public static final String CONST_IMAGES = "IMAGES";
    public static final String CONST_VIDEOS = "VIDEOS";
    public static final String CONST_AUDIO = "AUDIO";
    public static final String CONST_RECENT = "RECENT";

    public static final int CONST_TRANSFER_ONGOING = 0;
    public static final int CONST_TRANSFER_COMPLETED = 1;
    public static final int CONST_TRANSFER_CANCELLED = 2;

    public static final File homeDirectory = Environment.getExternalStorageDirectory();
}
