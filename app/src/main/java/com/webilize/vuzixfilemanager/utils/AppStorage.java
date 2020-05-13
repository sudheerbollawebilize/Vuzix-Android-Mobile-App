package com.webilize.vuzixfilemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class AppStorage {

    private static AppStorage appStorage;
    protected Context mContext;
    private static SharedPreferences mSharedPreferences;
    private static SharedPreferences.Editor mSharedPreferencesEditor;

    public static final String SP_IP = "SP_IP";
    public static final String SP_PORT = "SP_PORT";
    public static final String SP_SSID = "SP_SSID";
    public static final String SP_DEVICE_NAME = "SP_DEVICE_NAME";
    public static final String SP_DEVICE_MODE = "SP_DEVICE_MODE";
    public static final String SP_IS_STORAGE_FIRST_TIME = "SP_IS_STORAGE_FIRST_TIME";
    public static final String SP_SHOW_HIDDEN = "SP_SHOW_HIDDEN";
    public static final String SP_LIST_MODE = "SP_LIST_MODE";
    public static final String SP_SORT_MODE = "SP_SORT_MODE";
    public static final String SP_BOOKMARKED_FOLDERS = "SP_BOOKMARKED_FOLDERS";
    public static final String SP_CP_TEXT = "SP_CP_TEXT";
    public static final String SP_CP_FILE_PATHS = "SP_CP_FILE_PATHS";
    public static final String SP_SHOW_EMPTY_FOLDERS = "SP_SHOW_EMPTY_FOLDERS";
    public static final String SP_SHOW_ONLY_FILES = "SP_SHOW_ONLY_FILES";
    public static final String SP_SHOW_ONLY_FOLDERS = "SP_SHOW_ONLY_FOLDERS";
    public static final String SP_SHOW_FINISHED = "SP_SHOW_FINISHED";
    public static final String SP_DEFAULT_INCOMING_FOLDER = "SP_DEFAULT_INCOMING_FOLDER";
    public static final String SP_DEVICE_ADDRESS = "SP_DEVICE_ADDRESS";

    private AppStorage(Context context) {
        mContext = context;
        mSharedPreferences = context.getSharedPreferences("com.webilize.vuzixfilemanager", Context.MODE_PRIVATE);
        mSharedPreferencesEditor = mSharedPreferences.edit();
    }

    /**
     * Creates single instance of SharedPreferenceUtils
     *
     * @param context context of Activity or Service
     * @return Returns instance of SharedPreferenceUtils
     */
    public static synchronized AppStorage getInstance(Context context) {
        if (appStorage == null) {
            appStorage = new AppStorage(context.getApplicationContext());
        }
        return appStorage;
    }

    public void setClipBoardText(String text) {
        setValue(SP_CP_TEXT, text);
    }

    public String getClipBoardText() {
        String cpText = getValue(SP_CP_TEXT, "");
        if (TextUtils.isEmpty(cpText)) return "";
        else return cpText;
    }

    public void setClipBoardPath(String[] texts) {
        String cpText = getValue(SP_CP_FILE_PATHS, "");
        for (String text : texts) {
            if (TextUtils.isEmpty(cpText)) {
                cpText += text;
            } else {
                if (!TextUtils.isEmpty(text))
                    cpText += AppConstants.BOOKMARK_SEPERATOR + text;
            }
        }
        setValue(SP_CP_FILE_PATHS, cpText);
    }

    public String[] getClipBoardPaths() {
        String cpText = getValue(SP_CP_FILE_PATHS, "");
        if (TextUtils.isEmpty(cpText)) {
            return null;
        } else if (!cpText.contains(AppConstants.BOOKMARK_SEPERATOR)) {
            return new String[]{cpText};
        } else {
            return cpText.split(AppConstants.BOOKMARK_SEPERATOR);
        }
    }

    public void clearClipBoardText() {
        setValue(SP_CP_TEXT, "");
    }

    public void clearClipBoardUri() {
        setValue(SP_CP_FILE_PATHS, "");
    }

    public void setIP(String ip) {
        setValue(SP_IP, ip);
    }

    public void setPort(int port) {
        setValue(SP_PORT, port);
    }

    public String getIP() {
        return getValue(SP_IP, "");
    }

    public int getPort() {
        return getValue(SP_PORT, -1);
    }

    public void setDeviceName(String deviceName) {
        setValue(SP_DEVICE_NAME, deviceName);
    }

    public void setSSID(String ssid) {
        setValue(SP_SSID, ssid);
    }

    public String getDeviceName() {
        return getValue(SP_DEVICE_NAME, "");
    }

    public String getSSID() {
        return getValue(SP_SSID, "");
    }

    /**
     * Stores String value in preference
     *
     * @param key   key of preference
     * @param value value for that key
     */
    public void setValue(String key, String value) {
        mSharedPreferencesEditor.putString(key, value);
        mSharedPreferencesEditor.commit();
    }

    /**
     * Stores int value in preference
     *
     * @param key   key of preference
     * @param value value for that key
     */
    public void setValue(String key, int value) {
        mSharedPreferencesEditor.putInt(key, value);
        mSharedPreferencesEditor.commit();
    }

    /**
     * Stores Double value in String format in preference
     *
     * @param key   key of preference
     * @param value value for that key
     */
    public void setValue(String key, double value) {
        setValue(key, Double.toString(value));
    }

    /**
     * Stores long value in preference
     *
     * @param key   key of preference
     * @param value value for that key
     */
    public void setValue(String key, long value) {
        mSharedPreferencesEditor.putLong(key, value);
        mSharedPreferencesEditor.commit();
    }

    /**
     * Stores boolean value in preference
     *
     * @param key   key of preference
     * @param value value for that key
     */
    public void setValue(String key, boolean value) {
        mSharedPreferencesEditor.putBoolean(key, value);
        mSharedPreferencesEditor.commit();
    }

    /**
     * Retrieves String value from preference
     *
     * @param key          key of preference
     * @param defaultValue default value if no key found
     */
    public String getValue(String key, String defaultValue) {
        return mSharedPreferences.getString(key, defaultValue);
    }

    /**
     * Retrieves int value from preference
     *
     * @param key          key of preference
     * @param defaultValue default value if no key found
     */
    public int getValue(String key, int defaultValue) {
        return mSharedPreferences.getInt(key, defaultValue);
    }

    /**
     * Retrieves long value from preference
     *
     * @param key          key of preference
     * @param defaultValue default value if no key found
     */
    public long getValue(String key, long defaultValue) {
        return mSharedPreferences.getLong(key, defaultValue);
    }

    /**
     * Retrieves boolean value from preference
     *
     * @param keyFlag      key of preference
     * @param defaultValue default value if no key found
     */
    public boolean getValue(String keyFlag, boolean defaultValue) {
        return mSharedPreferences.getBoolean(keyFlag, defaultValue);
    }

    /**
     * Removes key from preference
     *
     * @param key key of preference that is to be deleted
     */
    public void removeKey(String key) {
        if (mSharedPreferencesEditor != null) {
            mSharedPreferencesEditor.remove(key);
            mSharedPreferencesEditor.commit();
        }
    }

    /**
     * Clears all the preferences stored
     */
    public void clear() {
        mSharedPreferencesEditor.clear().commit();
    }

}
