package com.webilize.vuzixfilemanager.dbutils;

public class TableTransferModel {

    public static String name = "zname";
    public static String progress = "zprogress";
    public static String timeStamp = "zTimeStamp";
    public static String status = "zstatus";
    public static String rawData = "zrawData";
    public static String isIncoming = "zisIncoming";
    public static String folderPath = "zFolderPath";
    public static String size = "zSize";
    public static String id = "zid";

    public static String TABLE_NAME = "TableAddress";
    public static String CREATE_TABLE = "create table if not exists " + TABLE_NAME + " ( " + id + " INTEGER PRIMARY KEY NOT NULL, " + name +
            " TEXT, " + timeStamp + " TEXT, " + folderPath + " TEXT, " + progress + " INTEGER, " + status + " INTEGER, " + size + " TEXT, " + isIncoming + " INTEGER, " + rawData + " TEXT, UNIQUE (" + id + ") ON CONFLICT REPLACE);";

}
