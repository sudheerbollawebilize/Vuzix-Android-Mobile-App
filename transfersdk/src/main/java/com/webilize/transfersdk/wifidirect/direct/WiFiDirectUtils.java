package com.webilize.transfersdk.wifidirect.direct;


import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.webilize.transfersdk.wifidirect.WiFiP2PError;
import com.webilize.transfersdk.wifidirect.WiFiP2PInstance;

import java.lang.reflect.Method;

public class WiFiDirectUtils {

    public static final String SHARED_PREFERENCES_NAME = "TransferSDK";
    public static final String WIFI_DIRECT_DEVICE_NAME = "WF_DIRECT_DEV_NAME";
    public static final String WIFI_DIRECT_REMOTE_DEVICE_NAME = "WF_DIRECT_REMOTE_DEV_NAME";

    private static final String TAG = WiFiDirectUtils.class.getSimpleName();

    public static void clearServiceRequest(WiFiP2PInstance wiFiP2PInstance, WifiP2pManager.ActionListener listener) {
        wiFiP2PInstance.getWifiP2pManager().clearServiceRequests(wiFiP2PInstance.getChannel(), listener);
    }

    public static void  clearServiceRequest(WiFiP2PInstance wiFiP2PInstance) {
        clearServiceRequest(wiFiP2PInstance, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Success clearing service request");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error clearing service request: " + reason);
            }

        });
    }

    public static void clearLocalServices(WiFiP2PInstance wiFiP2PInstance) {
        wiFiP2PInstance.getWifiP2pManager().clearLocalServices(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Local services cleared");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error clearing local services: " + WiFiP2PError.fromReason(reason));
            }

        });
    }

    public static void cancelConnect(WiFiP2PInstance wiFiP2PInstance) {
        wiFiP2PInstance.getWifiP2pManager().cancelConnect(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Connect canceled successfully");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Error canceling connect: " + WiFiP2PError.fromReason(reason));
            }

        });
    }

    public static void deletePersistentGroups(final WiFiP2PInstance wiFiP2PInstance) {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(wiFiP2PInstance.getWifiP2pManager(), wiFiP2PInstance.getChannel(), netid, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeGroup(final WiFiP2PInstance wiFiP2PInstance, WifiP2pManager.ActionListener listener) {
        Log.d(TAG, "removeGroup: ");

        wiFiP2PInstance.getWifiP2pManager().requestGroupInfo(wiFiP2PInstance.getChannel(), new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(final WifiP2pGroup group) {
                if (group != null) {
                    Log.d(TAG, "removeGroup: " + group.toString());
                    wiFiP2PInstance.getWifiP2pManager().removeGroup(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            String netWorkName = "";
                            if (group != null && group.getNetworkName() != null) {
                                netWorkName = group.getNetworkName();
                            }

                            Log.i(TAG, "Group removed: " + netWorkName);
                            listener.onSuccess();
                        }

                        @Override
                        public void onFailure(int reason) {
                            WiFiDirectUtils.deletePersistentGroups(wiFiP2PInstance);
                            listener.onFailure(reason);
                            Log.e(TAG, "Fail disconnecting from group. Reason: " + WiFiP2PError.fromReason(reason));
                        }
                    });
                } else {
                    listener.onSuccess();
                }
            }
        });
    }

    public static void removeGroup(final WiFiP2PInstance wiFiP2PInstance) {
        removeGroup(wiFiP2PInstance, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    public static void stopPeerDiscovering(WiFiP2PInstance wiFiP2PInstance) {
        wiFiP2PInstance.getWifiP2pManager().stopPeerDiscovery(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Peer discovering stopped");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error stopping peer discovering: " + WiFiP2PError.fromReason(reason));
            }

        });
    }

}
