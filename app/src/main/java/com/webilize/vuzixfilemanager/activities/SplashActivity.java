package com.webilize.vuzixfilemanager.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;

public class SplashActivity extends BaseActivity {
    private Handler handler;
    private Runnable runnable;
    private AppStorage appStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    void initComponents() {
        appStorage = AppStorage.getInstance(this);
        if (checkForStoragePermissions()) {
            handler = new Handler();
            runnable = () -> {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finishAffinity();
            };
            handler.postDelayed(runnable, 500);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(runnable);
    }

    private boolean checkForStoragePermissions() {
        boolean firstTime = appStorage.getValue(AppStorage.SP_IS_STORAGE_FIRST_TIME, true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                if (firstTime) {
                    appStorage.setValue(AppStorage.SP_IS_STORAGE_FIRST_TIME, false);
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, AppConstants.PERMISSIONS_REQUEST_READ_WRITE_EXTERNAL_STORAGE);
                } else showPermissionMandatoryDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, AppConstants.PERMISSIONS_REQUEST_READ_WRITE_EXTERNAL_STORAGE);
            }
        } else return true;
        return false;
    }

    private void showPermissionMandatoryDialog() {
        new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.app_name).
                setMessage(R.string.storage_permission_mandatory).
                setPositiveButton(R.string.allow, (dialog, which) -> openSettings()).show();
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, AppConstants.PERMISSIONS_REQUEST_READ_WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initComponents();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        initComponents();
    }

}
