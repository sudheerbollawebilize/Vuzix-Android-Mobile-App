package com.webilize.transfersdk;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.util.Log;

import com.webilize.transfersdk.exceptions.NoPeersFoundException;
import com.webilize.transfersdk.exceptions.WifiNotConnectedException;
import com.webilize.transfersdk.exceptions.WifiNotEnabledException;
import com.webilize.transfersdk.exceptions.WifiP2pConnectionFailed;
import com.webilize.transfersdk.helpers.WifiHelper;
import com.webilize.transfersdk.socket.SocketConfig;
import com.webilize.transfersdk.wifidirect.WiFiP2PError;
import com.webilize.transfersdk.wifidirect.WiFiP2PInstance;
import com.webilize.transfersdk.wifidirect.direct.WiFiDirectUtils;
import com.webilize.transfersdk.wifidirect.listeners.ServiceDiscoveredListener;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

public class ConnectionHelper {
    private static final String TAG = "ConnectionHelper";

    private static final long CONNECT_TIMEOUT = 60000L; //ms
    private static final long DISCOVERY_TIME = 4000L; //ms
    private static final Integer MAX_TRY = 6;

    private WiFiP2PInstance wiFiP2PInstance;
    private boolean isBroadcastReceiverRegistered = false;

    private Integer connectTry = 0;
    private Integer findGroupsTry = 0;
    private Integer maxTry = MAX_TRY;

    private boolean forceTCP = false;

    private boolean isDestroyed = false;

    private boolean wifiDirectConnected = false;

    //region get

    public boolean isForceTCP() {
        return forceTCP;
    }

    public boolean isSSL() {
        return isSSL;
    }

    //endregion

    //region SSL properties
    private boolean isSSL = false;
    private Certificate privateCertificate;
    private Certificate publicCertificate;
    //endregion

    private RXConnection rxConnection;
    private CompositeDisposable bag = new CompositeDisposable();

    Handler onConnectHandler;

    //region set methods

    public void setMaxTry(Integer maxTry) {
        this.maxTry = maxTry;
    }

    public void setSSL(boolean SSL) {
        isSSL = SSL;
    }

    public void setPrivateCertificate(Certificate privateCertificate) {
        this.privateCertificate = privateCertificate;
    }

    public void setPublicCertificate(Certificate publicCertificate) {
        this.publicCertificate = publicCertificate;
    }

    public void setForceTCP(boolean forceTCP) {
        this.forceTCP = forceTCP;
    }

    //endregion

    //region public methods
    public void initialize(Context context, Listener listener, boolean isServer, int port) {
        initialize(context, listener, false, isServer, port);
    }

    public void initialize(Context context, Listener listener, boolean isService, boolean isServer, int port) {

        bag = new CompositeDisposable();
        wifiDirectConnected = false;
        findGroupsTry = 0;

        if (!forceTCP && WifiHelper.isWifiDirectSupported(context)) {
            if (WifiHelper.isEnabled(context)) {
                wiFiP2PInstance = WiFiP2PInstance.getInstance(context, isService, port);
                if (!isBroadcastReceiverRegistered) {
                    wiFiP2PInstance.registerReceiver(context);
                    isBroadcastReceiverRegistered = true;
                }

                wiFiP2PInstance.setPeerConnectedListener(ip -> {
                            wifiDirectConnected = true;
                            wiFiP2PInstance.setBroadcastListener(null);
                            startTCPCommunication(context, listener, ip, port, isServer, true);
                        }
                );

                //wiFiP2PInstance.initialize();

                if (!isService) {
                    findWifiDirectDevices(listener);
                }

            } else {
                listener.onError(new WifiNotEnabledException());
            }
        } else {
            if (WifiHelper.isConnected(context)) {
                if (isServer) {
                    startTCPCommunication(context, listener, "", 0, isServer, false);
                } else {
                    listener.onTCPClientReady();
                }
            } else {
                listener.onError(new WifiNotConnectedException());
            }
        }
    }

    public void connect(Context activity, String ip, int port, Listener listener) {
        startTCPCommunication(activity, listener, ip, port, false, false);
    }

    public void connect(Context context, WifiP2pDevice serviceSelected, Listener listener) {
        connect(context, serviceSelected, listener, true);
    }

    public void connect(Context context, WifiP2pDevice serviceSelected, Listener listener, boolean isServer) {
        connect(context, serviceSelected, listener, isServer, CONNECT_TIMEOUT);
    }

    public void connect(Context context, WifiP2pDevice serviceSelected, Listener listener, boolean isServer, long timeout) {
        Log.e(TAG, "Connect WifiP2pDevice");
        if (onConnectHandler == null) {
            onConnectHandler = new Handler();
            onConnectHandler.postDelayed(() -> {
                Log.d(TAG, "Connect handler ");
                if (!wifiDirectConnected && !isDestroyed) {
                    wiFiP2PInstance.setBroadcastListener(null);
                    WiFiDirectUtils.cancelConnect(wiFiP2PInstance);
                    listener.onError(new WifiP2pConnectionFailed());
                }
            }, timeout);
        }

        if (isServer) {
            wiFiP2PInstance.setRemoteGroupOwnerIntent(15);
        }

        //wiFiP2PInstance.setBroadcastListener(() -> listener.onError(new WifiP2pConnectionFailed()));
        wiFiP2PInstance.connect(serviceSelected, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "connect success");
                // onConnectionInfoAvailable notifies for us, ignore
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error connecting '" + serviceSelected.deviceName + "'. Reason: " + WiFiP2PError.fromReason(reason));
                if (connectTry < maxTry) {
                    connect(context, serviceSelected, listener, isServer, timeout);
                    connectTry++;
                } else {
                    connectTry = 0;
                    listener.onError(new Exception("Failed connecting '\" + serviceSelected.deviceName + \"'"));
                    wiFiP2PInstance.removeGroups();
                }
            }
        });
    }

    public void destroy(Context context) {
        Log.d(TAG, "destroyed");
        isDestroyed = true;

        if (onConnectHandler != null) {
            onConnectHandler.removeCallbacksAndMessages(null);
        }

        if (wiFiP2PInstance != null) {
            if (isBroadcastReceiverRegistered)
                wiFiP2PInstance.unregisterReceiver(context);

            wiFiP2PInstance.onDestroy();
        }

        if (rxConnection != null) {
            bag.add(rxConnection.disconnect().subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action() {
                        @Override
                        public void run() throws Exception {
                            bag.dispose();
                        }
                    }));

        } else
            bag.dispose();
    }
    //endregion

    //region privates
    private void findWifiDirectDevices(Listener listener) {
        if (!isDestroyed) {
            // wiFiP2PInstance.discoverService();
            wiFiP2PInstance.discoverServices(DISCOVERY_TIME, new ServiceDiscoveredListener() {

                @Override
                public void onFinishServiceDeviceDiscovered(ArrayList<WifiP2pDevice> serviceDevices) {
                    Log.i(TAG, "Found '" + serviceDevices.size() + "' devices");
                    if (serviceDevices.isEmpty()) {
                        Log.d(TAG, "Discovery try: " + (findGroupsTry + 1));
                        if (findGroupsTry < MAX_TRY) {
                            findWifiDirectDevices(listener);
                            findGroupsTry++;
                        } else {
                            findGroupsTry = 0;
                            wiFiP2PInstance.stopPeerDiscovery();
                            wiFiP2PInstance.removeGroups();
                            wiFiP2PInstance.cancelInvitations();
                            listener.onError(new NoPeersFoundException());
                        }
                    } else {
                        findGroupsTry = 0;
                        listener.availablePeers(serviceDevices);
                    }
                }

                @Override
                public void onError(WiFiP2PError wiFiP2PError) {
                    listener.onError(new Exception("Error searching groups: " + wiFiP2PError.name()));
                }
            });
        }
    }

    private void startTCPCommunication(Context activity, Listener listener, String ip, int port, boolean isServer, boolean isWifiDirect) {
        SocketConfig socketConfig = new SocketConfig(ip, port);

        socketConfig.setServer(isServer);
        socketConfig.setSsl(isSSL);
        if (isSSL) {
            try {
                rxConnection = RXConnection.createSSLSocket(activity, socketConfig, privateCertificate, publicCertificate);
            } catch (Exception ex) {
                listener.onError(ex);
                return;
            }
        } else {
            rxConnection = RXConnection.createSocket(activity, socketConfig);
        }

        bag.add(rxConnection.connect(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connected -> {
                    if (connected) {
                        if (isServer) {
                            // is Server, we should wait for client to connect
                            if (!isWifiDirect) {
                                // user should show the IP & port to the client
                                listener.onTCPServerReady(WifiHelper.getIp(activity.getApplicationContext()), rxConnection.getPort());
                            }
                            iWaitClient(listener, isWifiDirect);
                        } else {
                            listener.onConnected(rxConnection, isWifiDirect);
                        }
                    } else {
                        listener.onError(new Exception("Connection failed"));
                    }
                }, throwable -> listener.onError(new Exception(throwable))));

    }

    private void iWaitClient(Listener listener, boolean isWifiDirect) {
        Log.d(TAG, "iWaitClient: starts");
        bag.add(rxConnection.waitClient().subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connected -> {
                    if (connected) {
                        listener.onConnected(rxConnection, isWifiDirect);
                    } else {
                        listener.onError(new Exception("Client not wifiDirectConnected"));
                    }
                }, throwable -> listener.onError(new Exception(throwable))));
    }

    //endregion

    //region Listener
    public interface Listener {

        /**
         * When this callback is fired you should show the list of available devices and then call
         * {@link ConnectionHelper#connect(Context, WifiP2pDevice, Listener)} to start connection
         *
         * @param wifiP2pDevices: list of available Wifi Direct peers
         */
        void availablePeers(ArrayList<WifiP2pDevice> wifiP2pDevices);

        /**
         * The client TCP local Wi-Fi is ready to connect.
         * When this callback is fired you should call {@link ConnectionHelper#connect(Context, String, int, Listener)}
         * to start connection
         */
        void onTCPClientReady();

        /**
         * The server TCP local Wi-Fi is wifiDirectConnected and is waiting a client to connect.
         * When this callback is fired you should show somehow the IP & the port for the client to
         * connect, pe: show a QR code and scan it with you client
         *
         * @param ip:   current Server's IP address
         * @param port: current Server's port
         */
        void onTCPServerReady(String ip, Integer port);

        /**
         * @param rxConnection
         * @param isWifiDirect: says if the connection is working on Wifi Direct or TCP local Wi-Fi
         */
        void onConnected(RXConnection rxConnection, boolean isWifiDirect);

        /**
         * Called when some error occurred in the workflow.
         * - {@link WifiNotConnectedException}: Wi-Fi Connected
         * - {@link WifiNotEnabledException}: Wi-Fi not enabled
         * - {@link NoPeersFoundException}: No Wi-Fi Direct peers found in the discovery
         */
        void onError(Exception e);

    }
    //endregion

}
