package com.webilize.vuzixfilemanager.dbutils;

public class TableTransferModel {
    private static TableTransferModel tableTransferModel;

    public static TableTransferModel getInstance() {
        if (tableTransferModel == null) tableTransferModel = new TableTransferModel();
        return tableTransferModel;
    }

    public String name = null;
    public String progress = null;
    public String timeStamp = null;
    public String status = null;
    public String rawData = null;
    public String isIncoming = null;
    public String folderPath = null;
    public String size = null;
    public String id = null;

    public String TABLE_NAME = "TableAddress";
    public String CREATE_TABLE;

    public TableTransferModel() {
        name = "zname";
        size = "zSize";
        progress = "zprogress";
        folderPath = "zFolderPath";
        timeStamp = "zTimeStamp";
        status = "zstatus";
        id = "zid";
        isIncoming = "zisIncoming";
        rawData = "zrawData";

        CREATE_TABLE = "create table if not exists " + TABLE_NAME + " ( " + id + " INTEGER PRIMARY KEY NOT NULL, " + name +
                " TEXT, " + timeStamp + " TEXT, " + folderPath + " TEXT, " + progress + " INTEGER, " + status + " INTEGER, " + size + " TEXT, " + isIncoming + " INTEGER, " + rawData + " TEXT, UNIQUE (" + id + ") ON CONFLICT REPLACE);";

    }
}
