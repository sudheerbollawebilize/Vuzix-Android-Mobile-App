package com.webilize.vuzixfilemanager.utils;

import android.os.Environment;

import com.webilize.vuzixfilemanager.BuildConfig;

import java.io.File;

public class AppConstants {

    public static final String NOTIFICATION_CHANNEL = "VUZIX_FILE_MANAGER_CHANNEL";
    public static final String HOTSPOT_NOTIFICATION_CHANNEL = "HOTSPOT_NOTIFICATION_CHANNEL";
    public static final int PERMISSIONS_REQUEST_READ_WRITE_EXTERNAL_STORAGE = 1001;
    public static final int MY_PERMISSIONS_MANAGE_WRITE_SETTINGS = 1002;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1003;

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

    public static final String CONST_QR_CODE = "QR Code";
    public static final String CONST_WIFI_HOTSPOT = "Wifi Hotspot";
    public static final String CONST_BLUETOOTH = "Bluetooth";
    public static final String CONST_WIFI_DIRECT = "Wifi Direct";
    public static final String CONST_CONNECTION_EMPTY = "";

    // start region
    // hotspot constants
    public static final String ACTION_HOTSPOT_TURNON = BuildConfig.APPLICATION_ID + ".TURN_ON";
    public static final String ACTION_HOTSPOT_TURNOFF = BuildConfig.APPLICATION_ID + ".TURN_OFF";
    public static final String DATA_SCHEME = "wifihotspot";
    public static final String DATA_SCHEME_ON = "turnon";
    public static final String DATA_SCHEME_OFF = "turnoff";
    public static final String ON_URI = "<a href=\"wifihotspot://turnon\">wifihotspot://turnon</a>";
    public static final String OFF_URI = "<a href=\"wifihotspot://turnoff\">wifihotspot://turnoff</a>";
    public static final String NEED_PERMISSIONS = BuildConfig.APPLICATION_ID + ".NEED_PERMISSION";
    // end region

}
