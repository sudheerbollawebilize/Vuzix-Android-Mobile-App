package com.webilize.vuzixfilemanager.dbutils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import kotlin.jvm.Synchronized;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static DatabaseHandler mInstance;
    private SQLiteDatabase writableDb = null;
    private static int DATABASE_VERSION = 1;

    private static String DATABASE_NAME = "VuzixFileManager";

    public static DatabaseHandler getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DatabaseHandler(context);
            mInstance.getWritableDatabase();
        }
        return mInstance;
    }

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void close() {
        super.close();
        mInstance = null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TableTransferModel.getInstance().CREATE_TABLE);
        db.execSQL(TableDevice.CREATE_TABLE);
        db.execSQL(TableDeviceFavorites.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            if (oldVersion != newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TableTransferModel.getInstance().TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + TableDevice.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + TableDeviceFavorites.TABLE_NAME);
                onCreate(db);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if (writableDb == null || !writableDb.isOpen()) {
            writableDb = super.getWritableDatabase();
        }
        return writableDb;
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        if (writableDb == null || !writableDb.isOpen()) {
            writableDb = super.getReadableDatabase();
        }
        return writableDb;
    }

    @Synchronized
    public int clearTable(String tableName) {
        return getWritableDatabase().delete(tableName, null, null);
    }

    @Synchronized
    public void clearDB() {
        getWritableDatabase().execSQL("delete from " + TableTransferModel.getInstance().TABLE_NAME);
        getWritableDatabase().execSQL("delete from " + TableDevice.TABLE_NAME);
        getWritableDatabase().execSQL("delete from " + TableDeviceFavorites.TABLE_NAME);
    }

    @Synchronized
    public void clearAllTables() {
        getWritableDatabase().execSQL("delete from " + TableTransferModel.getInstance().TABLE_NAME);
        getWritableDatabase().execSQL("delete from " + TableDevice.TABLE_NAME);
        getWritableDatabase().execSQL("delete from " + TableDeviceFavorites.TABLE_NAME);
    }

    @Synchronized
    public void clearSingleTable(String tableName) {
        getWritableDatabase().execSQL("DELETE FROM " + tableName + ";");
    }

    public long insertData(String tableName, ContentValues values) {
        return getWritableDatabase().insert(tableName, null, values);
    }

    public int updateData(
            String tableName,
            ContentValues values,
            String where,
            String[] args) {
        return getWritableDatabase().update(tableName, values, where, args);
    }

    public int deleteData(String tableName, String where, String[] args) {
        return getWritableDatabase().delete(tableName, where, args);
    }

    public Cursor selectData(String tableName) {
        return getWritableDatabase().query(tableName, null, null, null, null, null, null);
    }

    public Cursor selectData(String tableName, String where, String[] args) {
        return getWritableDatabase().query(tableName, null, where, args, null, null, null);
    }

    public Cursor selectData(String selectQuery, boolean rowQuery) {
        return getWritableDatabase().rawQuery(selectQuery, null);
    }

}
