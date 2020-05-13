package com.webilize.transfersdk.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 *
 * All the Wifi related stuff goes here.
 *
 * Add the following permission to your Manifest
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
 */
public class WifiHelper {
    private static final String TAG = "WifiHelper";

    /**
     * Check whether wifi is connected or not.
     * @param context
     * @return
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            @SuppressLint("MissingPermission") NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    /**
     * To get SSID of the connected wifi
     * @param context
     * @return
     */
    public static String getSSID(Context context) {
        String ssid = "";
        if (isConnected(context)) {
            WifiManager wifiManager = (WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                @SuppressLint("MissingPermission") WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                ssid = wifiInfo.getSSID();
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
            }
        }
        return ssid;
    }

    /**
     * This method is for enabling the wifi programmatically without opening settings
     * @param context
     * @param enable
     */
    @SuppressLint("MissingPermission")
    public static void enable(Context context, boolean enable) {
        WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null)
            wifiManager.setWifiEnabled(enable);
    }

    /**
     * Get the current ip address of the connected wifi
     * @param context
     * @return
     */
    @SuppressLint("DefaultLocale")
    public static String getIp(Context context) {
        String ip = "";
        WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            @SuppressLint("MissingPermission") WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        }
        return ip;
    }

    /**
     * To check if wifi direct supported by the current device.
     * @param ctx
     * @return
     */
    public static boolean isWifiDirectSupported(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo info : features) {
            if (info != null && info.name != null && info.name.equalsIgnoreCase("android.hardware.wifi.direct")) {
                return true;
            }
        }
        return false;
    }

    /**
     * To check if wifi is enabled in the device
     * @param context
     * @return
     */
    public static boolean isEnabled(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null)
            return wifi.isWifiEnabled();
        else return false;
    }

}

