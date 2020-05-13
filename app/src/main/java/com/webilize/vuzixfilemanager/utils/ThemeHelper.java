package com.webilize.vuzixfilemanager.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    public static final String lightMode = "light";
    public static final String darkMode = "dark";
    public static final String batterySaverMode = "battery";
    public static final String defaultMode = "default";

    public static void applyTheme(Context context,String themeChoice) {
        switch (themeChoice) {
            case lightMode:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                AppStorage.getInstance(context).setValue(AppStorage.SP_DEVICE_MODE, ThemeHelper.lightMode);
                break;
            case darkMode:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                AppStorage.getInstance(context).setValue(AppStorage.SP_DEVICE_MODE, ThemeHelper.darkMode);
                break;
            case batterySaverMode:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                AppStorage.getInstance(context).setValue(AppStorage.SP_DEVICE_MODE, ThemeHelper.batterySaverMode);
                break;
            case defaultMode:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                AppStorage.getInstance(context).setValue(AppStorage.SP_DEVICE_MODE, ThemeHelper.defaultMode);
                break;
        }
    }

}
