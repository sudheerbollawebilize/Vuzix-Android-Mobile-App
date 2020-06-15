package com.webilize.transfersdk.bluetooth;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;

public interface BluetoothEventsInterface {

    void onJsonReceived(JSONObject jsonObject);

    void onDataReceived(byte[] data);

    void onDeviceConnected(String deviceName, @Nullable String macAddress);

    void onFileSent();

    void onJsonSent();

    void onConnectionFailed(String reason);

    void connectionState(int status);
}