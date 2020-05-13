package com.webilize.transfersdk.wifidirect.listeners;

import android.net.wifi.p2p.WifiP2pDevice;

import com.webilize.transfersdk.wifidirect.WiFiP2PError;

import java.util.ArrayList;

public interface ServiceDiscoveredListener {
    void onFinishServiceDeviceDiscovered(ArrayList<WifiP2pDevice> peers);

    void onError(WiFiP2PError wiFiP2PError);
}
