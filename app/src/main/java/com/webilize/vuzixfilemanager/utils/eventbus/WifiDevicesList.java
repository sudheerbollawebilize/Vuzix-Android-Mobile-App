package com.webilize.vuzixfilemanager.utils.eventbus;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.ArrayList;

public class WifiDevicesList {

    public ArrayList<WifiP2pDevice> wifiP2pDeviceArrayList;

    public WifiDevicesList(ArrayList<WifiP2pDevice> wifiP2pDeviceArrayList) {
        this.wifiP2pDeviceArrayList = wifiP2pDeviceArrayList;
    }
}
