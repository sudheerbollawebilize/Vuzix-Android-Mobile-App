package com.webilize.vuzixfilemanager;

import android.app.Application;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.webilize.vuzixfilemanager.dbutils.DBHelper;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.AssetsHelper;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

public class BaseApplication extends Application {

    private static BaseApplication baseApplication;
    public static boolean isVuzix = true;
    public static final String NIGHT_MODE = "NIGHT_MODE";
    public AppStorage appStorage;
    public static int filterSortingMode = 0;
    public static boolean isOtgAvailable = false;

    @Override
    public void onCreate() {
        super.onCreate();
        baseApplication = this;
        setUpApplication();
    }

    private void setUpApplication() {
        copyCertificates();
        StaticUtils.setUpDeviceDefaultDetails(this);
        appStorage = AppStorage.getInstance(this);
        filterSortingMode = AppStorage.getInstance(this).getValue(AppStorage.SP_SORT_MODE, 0);
        StaticUtils.getBookMarkedLocations(this);
        new DBHelper(this).cleanTransferModelRecordsPeriodically();
    }

    public static synchronized BaseApplication getInstance() {
        if (baseApplication == null) baseApplication = new BaseApplication();
        return baseApplication;
    }

    private void copyCertificates() {
        Thread assetsThread = new Thread(() -> {
            try {
                AssetsHelper.copyAssets(getApplicationContext());
            } catch (Exception ex) {
                Log.d("Asset: ", "Error coping assets ", ex);
                FirebaseCrashlytics.getInstance().recordException(ex);
            }
        });
        assetsThread.start();
    }

}
