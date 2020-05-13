package com.webilize.vuzixfilemanager.dbutils;

public class TableDevice {

    public static String name = "zName";
    public static String macAdrress = "zMacAdrress";
    //    public String serialNumber = "zSerialNumber";
//    public String ipAddress = "zIP";
//    public String port = "zPort";
    public static String id = "zID";

    public static String TABLE_NAME = "TableDevice";
    public static String CREATE_TABLE = "create table if not exists " + TABLE_NAME + " ( " + id + " INTEGER PRIMARY KEY NOT NULL, " + name +
            " TEXT, " + macAdrress + " TEXT, UNIQUE (" + macAdrress + ") ON CONFLICT REPLACE);";
}
