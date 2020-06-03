package com.webilize.vuzixfilemanager.dbutils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.webilize.vuzixfilemanager.BaseApplication;
import com.webilize.vuzixfilemanager.models.DeviceFavouritesModel;
import com.webilize.vuzixfilemanager.models.DeviceModel;
import com.webilize.vuzixfilemanager.models.TransferModel;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;

import java.util.ArrayList;

import kotlin.jvm.Synchronized;

public class DBHelper {

    private DatabaseHandler databaseHandler;

    public DBHelper(Context context) {
        databaseHandler = DatabaseHandler.getInstance(context);
    }

    @Synchronized
    public long addTransferModel(TransferModel transferModel) {
        databaseHandler.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TableTransferModel.getInstance().name, transferModel.name);
        values.put(TableTransferModel.getInstance().size, transferModel.size + "");
        values.put(TableTransferModel.getInstance().rawData, transferModel.rawData);
        values.put(TableTransferModel.getInstance().folderPath, transferModel.folderLocation);
        values.put(TableTransferModel.getInstance().progress, transferModel.progress);
        values.put(TableTransferModel.getInstance().isIncoming, transferModel.isIncoming ? 0 : 1);
        values.put(TableTransferModel.getInstance().status, transferModel.status);
        try {
            if (transferModel.id != -1) {
                values.put(TableTransferModel.getInstance().id, transferModel.id);
                return databaseHandler.updateData(
                        TableTransferModel.getInstance().TABLE_NAME, values, TableTransferModel.getInstance().id + "=?",
                        new String[]{transferModel.id + ""}
                );
            } else
                return databaseHandler.insertData(TableTransferModel.getInstance().TABLE_NAME, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Synchronized
    public long addDeviceModel(DeviceModel deviceModel) {
        databaseHandler.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TableDevice.name, deviceModel.name);
        values.put(TableDevice.macAdrress, deviceModel.deviceAddress);
        try {
            if (deviceModel.id != -1) {
                values.put(TableTransferModel.getInstance().id, deviceModel.id);
                return databaseHandler.updateData(
                        TableDevice.TABLE_NAME, values, TableDevice.id + "=?",
                        new String[]{deviceModel.id + ""}
                );
            } else
                return databaseHandler.insertData(TableDevice.TABLE_NAME, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Synchronized
    public long addDeviceFavouritesModel(DeviceFavouritesModel deviceFavouritesModel) {
        databaseHandler.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TableDeviceFavorites.name, deviceFavouritesModel.name);
        values.put(TableDeviceFavorites.folderPath, deviceFavouritesModel.path);
        values.put(TableDeviceFavorites.deviceId, deviceFavouritesModel.deviceId);
        values.put(TableDeviceFavorites.isDefault, deviceFavouritesModel.isDefault ? 0 : 1);
        try {
            if (deviceFavouritesModel.id != -1) {
                values.put(TableTransferModel.getInstance().id, deviceFavouritesModel.id);
                return databaseHandler.updateData(
                        TableDeviceFavorites.TABLE_NAME, values, TableDeviceFavorites.id + "=?",
                        new String[]{deviceFavouritesModel.id + ""}
                );
            } else
                return databaseHandler.insertData(TableDeviceFavorites.TABLE_NAME, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Synchronized
    public DeviceModel getDeviceModel(String macAddress) {
        String selectQuery = "";
        selectQuery =
                "select * FROM " + TableDevice.TABLE_NAME + " WHERE " + TableDevice.macAdrress + "='" + macAddress + "'";
        databaseHandler.getReadableDatabase();
        Cursor cursor = databaseHandler.selectData(selectQuery, true);
        if (cursor.moveToFirst()) {
            DeviceModel deviceModel = new DeviceModel();
            deviceModel.name = cursor.getString(cursor.getColumnIndex(TableDevice.name));
            deviceModel.deviceAddress = cursor.getString(cursor.getColumnIndex(TableDevice.macAdrress));
            deviceModel.id =
                    cursor.getLong(cursor.getColumnIndex(TableDevice.id));
            return deviceModel;
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return null;
    }

    @Synchronized
    public long getDeviceFavModel(String path) {
        String selectQuery = "";
        selectQuery =
                "select * FROM " + TableDeviceFavorites.TABLE_NAME + " WHERE " + TableDeviceFavorites.folderPath + "='" + path + "'";
        databaseHandler.getReadableDatabase();
        Cursor cursor = databaseHandler.selectData(selectQuery, true);
        if (cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndex(TableDeviceFavorites.id));
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return -1;
    }

    @Synchronized
    public ArrayList<DeviceFavouritesModel> getDeviceFavouritesModelArrayList(long deviceId) {
        ArrayList<DeviceFavouritesModel> arrayList = new ArrayList<>();
        String selectQuery =
                "select * FROM " + TableDeviceFavorites.TABLE_NAME + " WHERE " + TableDeviceFavorites.deviceId + "='" + deviceId + "'";

        databaseHandler.getReadableDatabase();
        Cursor cursor = databaseHandler.selectData(selectQuery, true);
        if (cursor.moveToFirst()) {
            do {
                DeviceFavouritesModel deviceFavouritesModel = new DeviceFavouritesModel();
                deviceFavouritesModel.name = cursor.getString(cursor.getColumnIndex(TableDeviceFavorites.name));
                deviceFavouritesModel.path = cursor.getString(cursor.getColumnIndex(TableDeviceFavorites.folderPath));
                deviceFavouritesModel.isDefault = cursor.getInt(cursor.getColumnIndex(TableDeviceFavorites.isDefault)) == 0;
                deviceFavouritesModel.deviceId = cursor.getLong(cursor.getColumnIndex(TableDeviceFavorites.deviceId));
                deviceFavouritesModel.id = cursor.getLong(cursor.getColumnIndex(TableDeviceFavorites.id));
                arrayList.add(deviceFavouritesModel);
            } while (cursor.moveToNext());
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return arrayList;
    }

    /**
     * Get list of records in ascending order for each individual table
     */
    @Synchronized
    public ArrayList<TransferModel> getTransferModelsList(boolean isIncoming) {
        boolean showFinished = AppStorage.getInstance(BaseApplication.getInstance()).getValue(AppStorage.SP_SHOW_FINISHED, false);
        ArrayList<TransferModel> arrayList = new ArrayList<>();
        String selectQuery = "";
        if (showFinished) {
            selectQuery =
                    "select * FROM " + TableTransferModel.getInstance().TABLE_NAME + " ORDER BY " + TableTransferModel.getInstance().id + " DESC LIMIT 20";
        } else {
            selectQuery =
                    "select * FROM " + TableTransferModel.getInstance().TABLE_NAME + " WHERE " + TableTransferModel.getInstance().status + "='" + 0 + "'" + " ORDER BY " + TableTransferModel.getInstance().id + " DESC LIMIT 20";
        }
        databaseHandler.getReadableDatabase();
        Cursor cursor = databaseHandler.selectData(selectQuery, true);
        if (cursor.moveToFirst()) {
            do {
                TransferModel salesReportsModel = new TransferModel();
                salesReportsModel.name =
                        cursor.getString(cursor.getColumnIndex(TableTransferModel.getInstance().name));
                salesReportsModel.folderLocation =
                        cursor.getString(cursor.getColumnIndex(TableTransferModel.getInstance().folderPath));
                salesReportsModel.status =
                        cursor.getInt(cursor.getColumnIndex(TableTransferModel.getInstance().status));
                salesReportsModel.progress =
                        cursor.getInt(cursor.getColumnIndex(TableTransferModel.getInstance().progress));
                salesReportsModel.rawData =
                        cursor.getString(cursor.getColumnIndex(TableTransferModel.getInstance().rawData));
                salesReportsModel.id =
                        cursor.getLong(cursor.getColumnIndex(TableTransferModel.getInstance().id));
                salesReportsModel.size =
                        Long.parseLong(cursor.getString(cursor.getColumnIndex(TableTransferModel.getInstance().size)));
                salesReportsModel.isIncoming =
                        cursor.getInt(cursor.getColumnIndex(TableTransferModel.getInstance().isIncoming)) == 0;
                arrayList.add(salesReportsModel);
            } while (cursor.moveToNext());
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return arrayList;
    }

    @Synchronized
    public ArrayList<Long> getTransferModelsListIds() {
        ArrayList<Long> arrayList = new ArrayList<>();
        String selectQuery = "";
        selectQuery =
                "select * FROM " + TableTransferModel.getInstance().TABLE_NAME + " ORDER BY " + TableTransferModel.getInstance().id + " DESC";
        databaseHandler.getReadableDatabase();
        Cursor cursor = databaseHandler.selectData(selectQuery, true);
        if (cursor.moveToFirst()) {
            do {
                arrayList.add(cursor.getLong(cursor.getColumnIndex(TableTransferModel.getInstance().id)));
            } while (cursor.moveToNext());
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return arrayList;
    }

    /**
     * Delete all data in tables
     */
    @Synchronized
    public int deleteAllTransfersData() {
        return databaseHandler.deleteData(TableTransferModel.getInstance().TABLE_NAME, "", null);
    }

    /**
     * Delete single item in a tables
     */
    @Synchronized
    public void deleteTransferModel(String itemId) {
        databaseHandler.getWritableDatabase();
        databaseHandler.deleteData(
                TableTransferModel.getInstance().TABLE_NAME, TableTransferModel.getInstance().id + "=?",
                new String[]{itemId + ""}
        );
    }

    @Synchronized
    public void deleteDeviceFav(long itemId) {
        databaseHandler.getWritableDatabase();
        databaseHandler.deleteData(TableDeviceFavorites.TABLE_NAME, TableDeviceFavorites.id + "=?", new String[]{itemId + ""});
    }

    /**
     * Check for the db records and clear records more than 20
     */
    @Synchronized
    public void cleanTransferModelRecordsPeriodically() {
        try {
            ArrayList<Long> arrayList = getTransferModelsListIds();
            ArrayList<String> arrayListToDelete = new ArrayList<>();
            if (!arrayList.isEmpty() && arrayList.size() > 20) {
                for (int i = 19; i < arrayList.size(); i++) {
                    arrayListToDelete.add(arrayList.get(i) + "");
                }
                String[] itemIds = arrayListToDelete.toArray(new String[arrayListToDelete.size()]);
                if (itemIds != null && itemIds.length > 0) {
                    databaseHandler.getWritableDatabase();
                    databaseHandler.deleteData(TableTransferModel.getInstance().TABLE_NAME,
                            TableTransferModel.getInstance().id + "=?", itemIds);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Synchronized
    public void cancelTransfer(TransferModel transferModel) {
        databaseHandler.getWritableDatabase();
        transferModel.status = AppConstants.CONST_TRANSFER_CANCELLED;
        addTransferModel(transferModel);
    }

    @Synchronized
    public void cancelTransfers(ArrayList<TransferModel> transferModelArrayList) {
        databaseHandler.getWritableDatabase();
        for (TransferModel transferModel : transferModelArrayList) {
            transferModel.status = AppConstants.CONST_TRANSFER_CANCELLED;
            addTransferModel(transferModel);
        }
    }

    @Synchronized
    public Cursor selectData(String tableName) {
        databaseHandler.getReadableDatabase();
        return databaseHandler.selectData(tableName);
    }

    public void closeDb() {
        databaseHandler.clearDB();
        databaseHandler.close();
    }

    @Synchronized
    public void deleteAll() {
        databaseHandler.clearAllTables();
        databaseHandler.close();
    }

}
