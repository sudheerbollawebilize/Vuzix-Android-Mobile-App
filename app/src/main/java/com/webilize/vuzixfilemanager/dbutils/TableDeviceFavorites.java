package com.webilize.vuzixfilemanager.dbutils;

public class TableDeviceFavorites {

    public static String name = "zName";
    public static String folderPath = "zFolderPath";
    public static String isDefault = "zIsDefault";
    public static String deviceId = "zDeviceId";
    public static String id = "zID";

    public static String TABLE_NAME = "TableDeviceFavorites";
    public static String CREATE_TABLE = "create table if not exists " + TABLE_NAME + " ( " + id + " INTEGER PRIMARY KEY NOT NULL, " + name +
            " TEXT, " + deviceId + " INTEGER, " + isDefault + " INTEGER, " +
            folderPath + " TEXT, UNIQUE (" + folderPath + ") ON CONFLICT REPLACE);";
}
