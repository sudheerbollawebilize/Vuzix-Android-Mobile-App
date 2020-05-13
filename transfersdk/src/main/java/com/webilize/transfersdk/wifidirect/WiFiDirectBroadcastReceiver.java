package com.webilize.transfersdk.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.webilize.transfersdk.wifidirect.direct.WiFiDirectUtils;

import java.util.ArrayList;

import static android.net.wifi.p2p.WifiP2pManager.EXTRA_P2P_DEVICE_LIST;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    public boolean isRegistered = false;
    private static final String TAG = WiFiDirectBroadcastReceiver.class.getName();

    private WiFiP2PInstance wiFiP2PInstance;
    private WifiP2pConnectionChangedListener listener;

    private final ArrayList<WifiP2pDevice> peers = new ArrayList<>();

    public void setListener(WifiP2pConnectionChangedListener listener) {
        this.listener = listener;
    }

    /**
     * register receiver
     *
     * @param context - Context
     * @param filter  - Intent Filter
     * @return see Context.registerReceiver(BroadcastReceiver,IntentFilter)
     */
    public Intent register(Context context, IntentFilter filter) {
        try {
            // ceph3us note:
            // here I propose to create
            // a isRegistered(Contex) method
            // as you can register receiver on different context
            // so you need to match against the same one :)
            // example  by storing a list of weak references
            // see LoadedApk.class - receiver dispatcher
            // its and ArrayMap there for example
            return !isRegistered
                    ? context.registerReceiver(this, filter)
                    : null;
        } finally {
            isRegistered = true;
        }
    }

    /**
     * unregister received
     *
     * @param context - context
     * @return true if was registered else false
     */
    public boolean unregister(Context context) {
        // additional work match on context before unregister
        // eg store weak ref in register then compare in unregister
        // if match same instance
        return isRegistered && unregisterInternal(context);
    }

    private boolean unregisterInternal(Context context) {
        context.unregisterReceiver(this);
        isRegistered = false;
        return true;
    }

    public WiFiDirectBroadcastReceiver(WiFiP2PInstance wiFiP2PInstance) {
        this.wiFiP2PInstance = wiFiP2PInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "onReceive: " + action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // indicate whether Wi-Fi p2p is enabled or disabled
            // EXTRA_WIFI_STATE provides the state information as int

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            boolean enabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED;

            if (!enabled) {
                Log.e(TAG, "WiFi P2P isn't active");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // the available peer list has changed. This can be sent as a result of peers being found, lost or updated
            // EXTRA_P2P_DEVICE_LIST provides the full list of current peers.
            // The full list of peers can also be obtained any time with requestPeers(WifiP2pManager.Channel, WifiP2pManager.PeerListListener)

            peers.clear();
            WifiP2pDeviceList wifiP2pDeviceList = intent.getParcelableExtra(EXTRA_P2P_DEVICE_LIST);
            //Log.d(TAG, "WifiP2pDeviceList: " + devices.toString());

            Log.d(TAG, "discoverServices: " + wifiP2pDeviceList.getDeviceList().size());
            for (WifiP2pDevice wifiP2pDevice : wifiP2pDeviceList.getDeviceList()) {
                if (!wifiP2pDevice.deviceName.contains("[TV]") && !wifiP2pDevice.deviceName.isEmpty() && wifiP2pDevice.status == 3 // connected
                ) {
                    peers.add(wifiP2pDevice);
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // the state of Wi-Fi p2p connectivity has changed
            // EXTRA_WIFI_P2P_INFO provides the p2p connection info in the form of a WifiP2pInfo object
            // EXTRA_NETWORK_INFO provides the network info in the form of a NetworkInfo
            // EXTRA_WIFI_P2P_GROUP provides the details of the group WifiP2pGroup object associated with the p2p network
            if (wiFiP2PInstance == null || wiFiP2PInstance.getWifiP2pManager() == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d(TAG, "networkInfo: " + networkInfo.toString());
            if (intent.hasExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)) {
                WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                Log.d(TAG, "wifiP2pInfo: " + wifiP2pInfo.toString());
            }

            if (intent.hasExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)) {
                WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                Log.d(TAG, "wifiP2pGroup: " + wifiP2pGroup.toString());
                if (wifiP2pGroup.isGroupOwner()) {
                    if (wifiP2pGroup.getClientList() != null && wifiP2pGroup.getClientList().size() > 0) {
                        Log.d(TAG, "wifiP2pGroup ClientList: " + wifiP2pGroup.getClientList().toString());
                        ArrayList<WifiP2pDevice> list = new ArrayList<>(wifiP2pGroup.getClientList());
                        Log.d(TAG, "wifiP2pGroup ClientList 0: " + list.get(0).toString());
                        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(WiFiDirectUtils.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sp.edit().putString(WiFiDirectUtils.WIFI_DIRECT_REMOTE_DEVICE_NAME, list.get(0).deviceName).apply();
                    }
                } else {
                    WifiP2pDevice owner = wifiP2pGroup.getOwner();
                    Log.d(TAG, "wifiP2pGroup: " + wifiP2pGroup.getOwner());
                    if (owner != null) {
                        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(WiFiDirectUtils.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sp.edit().putString(WiFiDirectUtils.WIFI_DIRECT_REMOTE_DEVICE_NAME, owner.deviceName).apply();
                    }
                }
            }

            if (networkInfo.isConnected()) {
                // check onConnectionInfoAvailable inside WiFiP2PInstance
                wiFiP2PInstance.getWifiP2pManager().requestConnectionInfo(wiFiP2PInstance.getChannel(), wiFiP2PInstance);
            } else {
                Log.e(TAG, "networkInfo not connected: ");
                if (listener != null) {
                    listener.onFailed();
                }
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // indicates that this device details have changed
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "My info: " + device.toString());
            SharedPreferences sp = context.getApplicationContext()
                    .getSharedPreferences(WiFiDirectUtils.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(WiFiDirectUtils.WIFI_DIRECT_DEVICE_NAME, device.deviceName).apply();
        }
    }

    public ArrayList<WifiP2pDevice> getPeers() {
        return peers;
    }

    public interface WifiP2pConnectionChangedListener {
        void onFailed();
    }

}
