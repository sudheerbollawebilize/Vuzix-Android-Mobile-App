package com.webilize.vuzixfilemanager.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ActivitySettingsBinding;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.DialogUtils;
import com.webilize.vuzixfilemanager.utils.ThemeHelper;
import com.webilize.vuzixfilemanager.utils.transferutils.SocialBladeProtocol;

import java.io.File;

public class SettingsActivity extends BaseActivity implements View.OnClickListener {

    private ActivitySettingsBinding activitySettingsBinding;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySettingsBinding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
    }

    @Override
    void initComponents() {
        setDefaultFolderText();
        mode = AppStorage.getInstance(this).getValue(AppStorage.SP_DEVICE_MODE, ThemeHelper.defaultMode);
        if (mode.equalsIgnoreCase(ThemeHelper.darkMode)) {
            activitySettingsBinding.switchMode.setText(R.string.dark_mode);
            activitySettingsBinding.switchMode.setChecked(false);
        } else {
            activitySettingsBinding.switchMode.setChecked(true);
            activitySettingsBinding.switchMode.setText(R.string.light_mode);
        }
        setListeners();
    }

    private void setListeners() {
        activitySettingsBinding.imgBack.setOnClickListener(this);
        activitySettingsBinding.txtFavouriteLocations.setOnClickListener(this);
        activitySettingsBinding.relSelectIncomingFolder.setOnClickListener(this);
        activitySettingsBinding.switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ThemeHelper.applyTheme(SettingsActivity.this, ThemeHelper.lightMode);
                activitySettingsBinding.switchMode.setText(R.string.dark_mode);
            } else {
                ThemeHelper.applyTheme(SettingsActivity.this, ThemeHelper.darkMode);
                activitySettingsBinding.switchMode.setText(R.string.light_mode);
            }
            restartApp();
        });
    }

    private void restartApp() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finishAffinity();
    }

    private void setDefaultFolderText() {
        String defaultPath = AppStorage.getInstance(this).getValue(AppStorage.SP_DEFAULT_INCOMING_FOLDER, "");
        defaultPath = TextUtils.isEmpty(defaultPath) ? new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SocialBladeProtocol.BLADE_FOLDER).getAbsolutePath() : defaultPath;
        activitySettingsBinding.txtDefaultFolder.setText(defaultPath);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgBack:
                onBackPressed();
                break;
            case R.id.relSelectIncomingFolder:
                openFoldersDialog();
                break;
            case R.id.txtFavouriteLocations:
                openFavouriteLocationsDialog();
                break;
        }
    }

    private void openFavouriteLocationsDialog() {
        DialogUtils.showFavouritesDialog(this,true);
    }

    private void openFoldersDialog() {
        Dialog dialog = DialogUtils.showFolderSelectionDialog(this);
        dialog.setOnDismissListener(dialog1 -> setDefaultFolderText());
    }

}
