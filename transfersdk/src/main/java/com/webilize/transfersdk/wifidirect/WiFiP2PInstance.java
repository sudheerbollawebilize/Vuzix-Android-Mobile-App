package com.webilize.transfersdk.wifidirect;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Handler;
import android.util.Log;

import com.webilize.transfersdk.RXConnection;
import com.webilize.transfersdk.socket.SocketConfig;
import com.webilize.transfersdk.wifidirect.direct.WiFiDirectUtils;
import com.webilize.transfersdk.wifidirect.listeners.PeerConnectedListener;
import com.webilize.transfersdk.wifidirect.listeners.ServiceDiscoveredListener;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;


public class WiFiP2PInstance implements WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = WiFiP2PInstance.class.getSimpleName();

    private static final String GROUP_NAME = "GROUP_NAME";

    private static final int PORT = 30555;

    private static final String KEY_BUDDY_NAME = "buddyname";

    // https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pConfig#groupOwnerIntent
    private int remoteGroupOwnerIntent = 15;

    @SuppressLint("StaticFieldLeak")
    private static WiFiP2PInstance instance;

    // This class provides the API for managing Wi-Fi peer-to-peer connectivity
    // https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager
    private WifiP2pManager wifiP2pManager;
    // A channel that connects the application to the Wifi p2p framework. Most p2p operations require a Channel as an argument.
    private WifiP2pManager.Channel channel;

    private WiFiDirectBroadcastReceiver broadcastReceiver;

    private PeerConnectedListener peerConnectedListener;
    private WifiManager wfManager;

    private int port;

    private Context context;

    private final ArrayList<WifiP2pDevice> peers = new ArrayList<>();
    private final HashMap<String, Map<String, String>> buddies = new HashMap<>();

    private RXConnection rxConnection;

    private final CompositeDisposable bag = new CompositeDisposable();

    private boolean isService = false;

    private String groupName = "Webilize";

    private boolean isServiceDiscovery = false;

    //region constructor
    private WiFiP2PInstance(Context context, boolean isService) {
        this(context, isService, PORT);
    }

    private WiFiP2PInstance(Context context, boolean isService, int port) {
        this(context, isService, port, false);
    }

    private WiFiP2PInstance(Context context, boolean isService, int port, boolean isServiceDiscovery) {
        this(context, isService, port, isServiceDiscovery, true);
    }

    private WiFiP2PInstance(Context context, boolean isService, int port, boolean isServiceDiscovery, boolean initialize) {
        this.port = port;
        this.context = context.getApplicationContext();
        this.isService = isService;
        this.isServiceDiscovery = isServiceDiscovery;
        if (initialize)
            initialize();
    }
    //endregion

    //region get instance
    public static WiFiP2PInstance getInstance(Context context) {
        return getInstance(context, true);
    }

    public static WiFiP2PInstance getInstance(Context context, boolean isServer) {
        return getInstance(context, isServer, PORT);
    }

    public static WiFiP2PInstance getInstance(Context context, boolean isServer, int port) {
        return getInstance(context, isServer, port, false);
    }

    public static WiFiP2PInstance getInstance(Context context, boolean isServer, int port, boolean isServiceDiscovery) {
        return getInstance(context, isServer, port, isServiceDiscovery, true);
    }

    public static WiFiP2PInstance getInstance(Context context, boolean isServer, int port, boolean isServiceDiscovery, boolean initialize) {
        if (instance == null) {
            instance = new WiFiP2PInstance(context, isServer, port, isServiceDiscovery, initialize);
        }
        return instance;
    }
    //endregion

    //region get / set

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setRemoteGroupOwnerIntent(int remoteGroupOwnerIntent) {
        this.remoteGroupOwnerIntent = remoteGroupOwnerIntent;
    }

    //endregion

    public void initialize() {
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        // Registers the application with the Wi-Fi framework.
        // This function must be the first to be called before any p2p operations are performed.
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        wfManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // FIX: https://stackoverflow.com/questions/15152817/can-i-change-the-group-owner-in-a-persistent-group-in-wi-fi-direct/26242221#26242221
        // WiFiDirectUtils.deletePersistentGroups(this);
        WiFiDirectUtils.removeGroup(this, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                initConnection();
            }

            @Override
            public void onFailure(int i) {
                initConnection();
            }
        });

        broadcastReceiver = new WiFiDirectBroadcastReceiver(this);
    }

    private void initConnection() {
        // Service registration and listening
        if (isServiceDiscovery) {
            if (isService)
                addLocalService();
            discoverService();
        } else {
            discoverPeer();
        }
    }

    public static boolean isInstanciated() {
        return instance != null;
    }

    public WifiP2pManager getWifiP2pManager() {
        return wifiP2pManager;
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public void setPeerConnectedListener(PeerConnectedListener peerConnectedListener) {
        this.peerConnectedListener = peerConnectedListener;
    }

    //region WifiP2pManager.ConnectionInfoListener implementation
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        String ip = "";

        if (!info.groupFormed) {
            return;
        }

        if (!info.isGroupOwner) {
            Log.i(TAG, "onConnectionInfoAvailable: I am the server but not the group owner");
            SocketConfig socketConfig = new SocketConfig(info.groupOwnerAddress.getHostAddress(), port);
            socketConfig.setServer(false);
            socketConfig.setSsl(false);

            rxConnection = RXConnection.createSocket(context, socketConfig);
            bag.add(rxConnection.connect(true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(connected -> {
                        if (connected) {
                            Log.i(TAG, "IP exchange successful");
                            didFinishIPExchange(info.groupOwnerAddress.getHostAddress());
                        } else {
                            Log.i(TAG, "IP exchange failed: client not connected");
                        }
                    }, throwable -> {
                        Log.e(TAG, "error: ", throwable);
                    }));

        } else {

            SocketConfig socketConfig = new SocketConfig(port);
            socketConfig.setServer(true);
            socketConfig.setSsl(false);

            rxConnection = RXConnection.createSocket(context, socketConfig);
            bag.add(rxConnection.connect(true)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(connected -> {
                        if (connected) {
                            Log.i(TAG, "IP exchange successful");
                            //client connected
                            Socket client = rxConnection.getSocket();
                            didFinishIPExchange(client.getInetAddress().getHostAddress());
                        } else {
                            Log.i(TAG, "IP exchange failed: server not connected");
                        }
                    }, throwable -> {
                        Log.e(TAG, "error: ", throwable);

                    }));

        }
    }
    //endregion


    public void connect(int position) {
        if (position < peers.size()) {
            connect(peers.get(position));
        } else {
            Log.e(TAG, "Wrong position " + position + ". Peers available: " + peers.size());
        }
    }

    //region on connect
    public void connect(final WifiP2pDevice device, WifiP2pManager.ActionListener listener) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = remoteGroupOwnerIntent;

        wifiP2pManager.connect(channel, config, listener);
    }

    public void connect(final WifiP2pDevice device) {
        connect(device, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Connect on success, check onPeerConnected");
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Connect failed: " + reason + " Device info: " + device.toString());
            }
        });
    }
    //endregion

    private static final String INSTANCE_NAME = "_webilize";
    private static final String SERVICE_TYPE = "_presence._tcp";
    private static final String FULL_DOMAIN = INSTANCE_NAME + "." + SERVICE_TYPE;

    // Register a local service for service discovery. If a local service is registered,
    // the framework automatically responds to a service discovery request from a peer

    //region BroadcastReceiver
    public void registerReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        //activity.registerReceiver(broadcastReceiver, intentFilter);
        broadcastReceiver.register(context, intentFilter);
    }

    public void unregisterReceiver(Context context) {
        broadcastReceiver.unregister(context);
    }

    //endregion

    //region private
    private void cleanup() {
        wifiP2pManager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Local services cleared");
            }

            @Override
            public void onFailure(int arg0) {
                Log.i(TAG, "Could not clear local services");
            }
        });

        wifiP2pManager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Service requestes cleared");
            }

            @Override
            public void onFailure(int arg0) {
                Log.i(TAG, "Could not clear service requests");
            }
        });
    }

    private void discoverServices(final ServiceDiscoveredListener serviceDiscoveredListener) {

        WifiP2pServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        wifiP2pManager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Success adding service request");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error on addServiceRequest");
                serviceDiscoveredListener.onError(WiFiP2PError.fromReason(reason));
            }

        });

        wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Success initiating discovering services");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error on discoverServices");
                serviceDiscoveredListener.onError(WiFiP2PError.fromReason(reason));
            }

        });
    }

    private void didFinishIPExchange(String ip) {
        if (rxConnection != null) {
            bag.add(rxConnection.disconnect()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Log.i(TAG, "shouldConnectToServer: " + ip);
                        peerConnectedListener.onPeerConnected(ip);
                        cleanup();
                    }, error -> Log.e(TAG, "didFinishIPExchange: ", error)));
        } else {
            Log.e(TAG, "didFinishIPExchange: rxConnection is null");
        }
    }

    // The service information can be cleared with calls to removeLocalService
    // or clearLocalServices
    private void addLocalService() {
        Map<String, String> record = new HashMap<>();

        record.put(GROUP_NAME, groupName);
        record.put(KEY_BUDDY_NAME, android.os.Build.MODEL);

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(INSTANCE_NAME, SERVICE_TYPE, record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Local service added");
            }

            @Override
            public void onFailure(int arg0) {
                Log.i(TAG, "Could not add local service");
            }
        });
    }

    //endregion

    //region public

    public void setBroadcastListener(WiFiDirectBroadcastReceiver.WifiP2pConnectionChangedListener wifiP2pConnectionChangedListener) {
        if (broadcastReceiver != null) {
            broadcastReceiver.setListener(wifiP2pConnectionChangedListener);
        }
    }

    public void cancelInvitations() {
        WiFiDirectUtils.cancelConnect(this);
    }

    public void stopPeerDiscovery() {
        // initiate a stop on service discovery
        wifiP2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                WiFiDirectUtils.clearServiceRequest(WiFiP2PInstance.this);
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "FAILED to stop discovery");
            }
        });
    }

    public void addServiceRequest() {
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        wifiP2pManager.addServiceRequest(channel,
                serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "Service request added successfully");
                    }

                    @Override
                    public void onFailure(int code) {
                        Log.i(TAG, "Failed to add service request");
                    }
                });
    }

    public void discoverService() {
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = (fullDomain, record, device) -> {
            Log.d(TAG, "DnsSdTxtRecord -" + record.toString());
            if (record.containsKey(KEY_BUDDY_NAME) && record.containsKey(GROUP_NAME)
                    && record.get(GROUP_NAME) != null
                    && record.get(GROUP_NAME).equals(groupName)) {
                buddies.put(device.deviceAddress, record);
            } else {
                Log.e(TAG, "Group Name & Settings does not match");
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener dnsSdServiceResponseListener =
                (instanceName, registrationType, resourceType) -> {
                    Log.d(TAG, "onDnsSdServiceAvailable: " + resourceType.toString());

                    if (buddies.containsKey(resourceType.deviceAddress) && !peers.contains(resourceType)
                            && resourceType.status == WifiP2pDevice.AVAILABLE) {

                        Map<String, String> record = buddies.get(resourceType.deviceAddress);
                        if (record != null) {
                            if (record.containsKey(KEY_BUDDY_NAME))
                                resourceType.deviceName = record.get(KEY_BUDDY_NAME);

                            if (record.containsKey(GROUP_NAME) && record.get(GROUP_NAME) != null
                                    && record.get(GROUP_NAME).equals(groupName)) {
                                peers.add(resourceType);
                            } else {
                                Log.e(TAG, "Group Name does not match");
                            }
                        } else {
                            Log.e(TAG, "record is null");
                        }
                    }

                };

        wifiP2pManager.setDnsSdResponseListeners(channel, dnsSdServiceResponseListener, txtRecordListener);

        addServiceRequest();

        wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.i(TAG, "Started discovering services");
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onFailure(int reason) {
                String failReason = "Failed";
                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    failReason = "P2P_UNSUPPORTED";
                    Log.e(TAG, "P2P isn't supported on this device.");
                } else if (reason == WifiP2pManager.BUSY) {
                    failReason = "P2P_BUSY";
                    Log.e(TAG, "WiFi P2p is Busy");
                } else if (reason == WifiP2pManager.ERROR) {
                    failReason = "P2P_ERROR";
                    Log.e(TAG, "Internal Error in WiFi P2p");
                }
                Log.d(TAG, "Resetting Wifi Adapter " + failReason);

                //TODO Change the WIFI reset pattern

                if (wfManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                    Log.d("WiFi State:", "WIFI_STATE_ENABLED");

                    //TODO Check for User Shared preferences to see if user permits
                    // Wifi adapter reset
                    wfManager.setWifiEnabled(false);

                    wfManager.setWifiEnabled(true);
                    discoverService();
                } else if (wfManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING
                        || wfManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                    Log.d("WiFi State:", "WIFI_STATE_ENABLING/DISABLING");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    discoverService();
                }
            }
        });
    }

    private void discoverPeer() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: discover peers");
            }

            @Override
            public void onFailure(int i) {
                Log.e(TAG, "onFailure: discover peers " + i);
            }
        });
    }

    public void removeGroups() {
        WiFiDirectUtils.deletePersistentGroups(this);
        WiFiDirectUtils.removeGroup(this);
    }

    public void discoverServices(Long discoveringTimeInMillis, final ServiceDiscoveredListener serviceDiscoveredListener) {
        peers.clear();
        buddies.clear();
        discoverServices(serviceDiscoveredListener);

        if (!isServiceDiscovery)
            peers.addAll(broadcastReceiver.getPeers());

        Handler handler = new Handler();
        handler.postDelayed(() -> serviceDiscoveredListener.onFinishServiceDeviceDiscovered(peers), discoveringTimeInMillis);
    }

    public void onDestroy() {
        WiFiDirectUtils.deletePersistentGroups(this);
        cancelInvitations();
        WiFiDirectUtils.clearLocalServices(this);
        WiFiDirectUtils.clearServiceRequest(this);
        WiFiDirectUtils.removeGroup(this);
        WiFiDirectUtils.stopPeerDiscovering(this);
        instance = null;
    }

    //endregion

    public void setWithServiceDiscovery(boolean withDiscovery) {
        this.isServiceDiscovery = withDiscovery;
    }

}
