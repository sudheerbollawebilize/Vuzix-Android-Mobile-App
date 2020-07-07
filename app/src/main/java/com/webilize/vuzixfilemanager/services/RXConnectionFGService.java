package com.webilize.vuzixfilemanager.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.webilize.transfersdk.Certificate;
import com.webilize.transfersdk.ConnectionHelper;
import com.webilize.transfersdk.RXConnection;
import com.webilize.transfersdk.bluetooth.BluetoothChatService;
import com.webilize.transfersdk.bluetooth.BluetoothEventsInterface;
import com.webilize.transfersdk.exceptions.NoPeersFoundException;
import com.webilize.transfersdk.exceptions.WifiNotConnectedException;
import com.webilize.transfersdk.exceptions.WifiNotEnabledException;
import com.webilize.transfersdk.exceptions.WifiP2pConnectionFailed;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.SocketState;
import com.webilize.transfersdk.wifidirect.direct.WiFiDirectUtils;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.MainActivity;
import com.webilize.vuzixfilemanager.dbutils.DBHelper;
import com.webilize.vuzixfilemanager.models.DeviceFavouritesModel;
import com.webilize.vuzixfilemanager.models.DeviceModel;
import com.webilize.vuzixfilemanager.models.TransferModel;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.DateUtils;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.eventbus.OnConnectionError;
import com.webilize.vuzixfilemanager.utils.eventbus.OnJSONObjectReceived;
import com.webilize.vuzixfilemanager.utils.eventbus.OnJSONObjectReceivedFolders;
import com.webilize.vuzixfilemanager.utils.eventbus.OnProgressUpdated;
import com.webilize.vuzixfilemanager.utils.eventbus.OnSocketConnected;
import com.webilize.vuzixfilemanager.utils.eventbus.OnTCPInitialized;
import com.webilize.vuzixfilemanager.utils.eventbus.OnThumbsReceived;
import com.webilize.vuzixfilemanager.utils.eventbus.WifiDevicesList;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;
import com.webilize.vuzixfilemanager.utils.transferutils.SocialBladeProtocol;

import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class RXConnectionFGService extends Service implements ConnectionHelper.Listener {

    public static final String CHANNEL_ID = "ConnectionServiceChannel";
    private static final String TAG = "ConnectionFGService";
    private ConnectionHelper connectionHelper;
    private CommunicationProtocol communicationProtocol;
    private PublishSubject<DataWrapper> progressObservable = PublishSubject.create();
    private Disposable progressDisposable, sendFilesDisposable, getFoldersDisposable, getFilesDisposable;
    private WifiP2pDevice serviceSelected;
    private DeviceModel deviceModel;
    private boolean isAllowNewRequests = true;

    private DBHelper dbHelper;
    private TransferModel transferModel;
    private Consumer<DataWrapper> progressObserver = new Consumer<DataWrapper>() {
        @Override
        public void accept(DataWrapper dataWrapper) {
            if (dataWrapper.getSocketState() == SocketState.PROGRESS) {
                try {
                    if (dataWrapper.getData() != null) {
                        if (dataWrapper.getData() instanceof Integer) {
                            int progress = (Integer) dataWrapper.getData();
                            if (transferModel != null) {
                                transferModel.progress = progress;
                                EventBus.getDefault().post(new OnProgressUpdated(transferModel));
                            }
                            Log.e("progress ", progress + "");
                        }/* else if (dataWrapper.getData() instanceof ProgressItem) {
                            ProgressItem progress = (ProgressItem) dataWrapper.getData();
                            if (transferModel != null) {
                                transferModel.progress = (int) progress.progress;
                                EventBus.getDefault().post(new OnProgressUpdated(transferModel));
                            }
                            Log.e("progress name ", progress.fileName);
                            Log.e("progress ", progress.progress + "");
                        }*/
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            }
        }
    };
    private BluetoothChatService bluetoothChatService;

    public RXConnectionFGService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isAllowNewRequests = true;
        dbHelper = new DBHelper(this);
    }

    @Override
    public void onDestroy() {
        try {
            clearDeviceData();
            if (communicationProtocol != null) {
                communicationProtocol.getConnection().disconnect();
                communicationProtocol.destroy(this);
            }
            if (connectionHelper != null) {
                connectionHelper.destroy(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        super.onDestroy();
    }

    private void clearDeviceData() {
        AppStorage.getInstance(this).setValue(WiFiDirectUtils.WIFI_DIRECT_REMOTE_DEVICE_NAME, "");
        AppStorage.getInstance(this).setValue(WiFiDirectUtils.WIFI_DIRECT_DEVICE_ADDRESS, "");
        AppStorage.getInstance(this).setValue(AppStorage.SP_DEVICE_ADDRESS, "");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (communicationProtocol != null) communicationProtocol.destroy(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "On Start Command");
        if (dbHelper == null) dbHelper = new DBHelper(this);
        if (intent != null && intent.hasExtra("inputExtra")) {
            String input = intent.getStringExtra("inputExtra");
            createNotificationChannel();
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction("stop");
            PendingIntent stopPendingIntent = PendingIntent.getActivity(this, 1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("File Manager Connection")
                    .setContentText(input)
                    .addAction(0, "Stop", stopPendingIntent)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_glasses)
                    .setContentIntent(pendingIntent)
                    .build();
            communicationProtocol = CommunicationProtocol.getInstance();
            if (input.equalsIgnoreCase("start")) {
                if (isAllowNewRequests) {
                    isAllowNewRequests = false;
                    startForeground(1, notification);
                    if (intent.hasExtra("IsQr")) {
                        if (intent.getBooleanExtra("IsQr", true)) {
                            initializeQRConnection();
                        } else {
                            StaticUtils.setConnectionType(AppConstants.CONST_WIFI_HOTSPOT);
                            initializeHPConnection();
                        }
                    } else if (intent.hasExtra("IsBle")) {
                        StaticUtils.setConnectionType(AppConstants.CONST_BLUETOOTH);
                        if (intent.hasExtra("device")) {
                            BluetoothDevice device = intent.getParcelableExtra("device");
                            initialiseBTConnection(device, intent.getBooleanExtra("secure", false));
                        } else {
                            initialiseBTConnection();
                        }
                    } else {
                        StaticUtils.setConnectionType(AppConstants.CONST_WIFI_DIRECT);
                        initializeRXConnection();
                    }
                }
            } else if (input.equalsIgnoreCase("connect")) {
                startForeground(1, notification);
                if (intent.hasExtra("wifiDevice"))
                    if (intent.getParcelableExtra("wifiDevice") != null)
                        connectToWifiDirect(intent.getParcelableExtra("wifiDevice"));
            } else if (input.equalsIgnoreCase("addToFav")) {
                addDeviceFavModel(intent.getStringExtra("addToFavPath"), intent.getStringExtra("addToFavName"));
            } else if (input.equalsIgnoreCase("folder")) {
                if (isAllowNewRequests) {
                    startForeground(1, notification);
                    if (intent.hasExtra("folderPath")) {
                        if (intent.hasExtra("isOnlyfolders")) {
                            if (intent.getBooleanExtra("isOnlyfolders", false))
                                requestForOnlyFolders(intent.getStringExtra("folderPath"));
                            else requestForFolders(intent.getStringExtra("folderPath"));
                        } else requestForFolders(intent.getStringExtra("folderPath"));
                    } else if (intent.hasExtra("fileNames")) {
                        requestForOriginals(intent.getLongExtra("size", 0), intent.getStringArrayListExtra("fileNames"));
                    }
                } else
                    toast("Already another transaction is going on. Please wait till it is done.");
            } else if (input.equalsIgnoreCase("send")) {
                if (isAllowNewRequests) {
                    startForeground(1, notification);
                    if (intent.hasExtra("file")) {
                        if (intent.hasExtra("bt") && intent.getBooleanExtra("bt", false)) {
                            sendFileToBladeBt((File) intent.getSerializableExtra("file"));
                        } else
                            sendFileToBlade((File) intent.getSerializableExtra("file"));
                    }
                    if (intent.hasExtra("files"))
                        if (intent.hasExtra("bt") && intent.getBooleanExtra("bt", false)) {
                            sendFileToBladeBt(intent.getStringArrayExtra("files"));
                        } else sendFileToBlade(intent.getStringArrayExtra("files"));
                } else
                    toast("Already another transaction is going on. Please wait till it is done.");
            } else if (input.equalsIgnoreCase("destinationPath")) {
                if (isAllowNewRequests) {
                    startForeground(1, notification);
                    if (intent.hasExtra("destinationPath")) {
                        File file = null;
                        String[] files = null;
                        if (intent.hasExtra("file")) {
                            file = (File) intent.getSerializableExtra("file");
                        }
                        if (intent.hasExtra("files")) {
                            files = intent.getStringArrayExtra("files");
                        }
                        setDestinationToBlade(intent.getStringExtra("destinationPath"), file, files);
                    }
                } else
                    toast("Already another transaction is going on. Please wait till it is done.");
            } else if (input.equalsIgnoreCase("stop")) {
                clearDeviceData();
                StaticUtils.setConnectionType(AppConstants.CONST_CONNECTION_EMPTY);
                try {
                    if (bluetoothChatService != null && bluetoothChatService.getState() == BluetoothChatService.STATE_CONNECTED)
                        bluetoothChatService.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                stopForeground(true);
                stopSelf();
            } else if (input.equalsIgnoreCase("stopTransfer")) {
                StaticUtils.setConnectionType(AppConstants.CONST_CONNECTION_EMPTY);
                stopOnGoingTransfers();
            }
        }
        return START_STICKY;
    }

    private void initialiseBTConnection(BluetoothDevice device, boolean isSecure) {
        if (bluetoothChatService == null) initialiseBTConnection();
        bluetoothChatService.connect(device, isSecure);
    }

    private void initialiseBTConnection() {
        bluetoothChatService = new BluetoothChatService(this, new BluetoothEventsInterface() {
            @Override
            public void onJsonReceived(JSONObject jsonObject) {
                Log.e(TAG, "json received " + jsonObject.toString());
            }

            @Override
            public void onDataReceived(byte[] data) {
                Log.e(TAG, "onDataReceived");
                File dir = new File(Environment.getExternalStorageDirectory() + "/Bluetooth");
                if (!dir.exists()) dir.mkdirs();
                File someFile = new File(dir, "File_" + System.currentTimeMillis() + ".jpg");
                try {
                    if (someFile.exists()) someFile.delete();
                    someFile.createNewFile();
                    FileUtils.writeByteArrayToFile(someFile, data);
                    android.util.Log.e(TAG, "Done creating");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDeviceConnected(String deviceName, @Nullable String macAddress) {
                Log.e(TAG, "onDeviceConnected" + deviceName);
            }

            @Override
            public void onFileSent() {
                Log.e(TAG, "onFileSent");
            }

            @Override
            public void onJsonSent() {
                Log.e(TAG, "onJsonSent");
            }

            @Override
            public void onConnectionFailed(String reason) {
                Log.e(TAG, "onConnectionFailed " + reason);
            }

            @Override
            public void connectionState(int status) {
                switch (status) {
                    case BluetoothChatService.STATE_CONNECTED:
//                        toast("Connected");
                        break;
                    case BluetoothChatService.STATE_CONNECTING:
//                        toast("Connecting");
                        break;
                    case BluetoothChatService.STATE_LISTEN:
                    case BluetoothChatService.STATE_NONE:
//                        toast("Not Connected");
                        break;
                }
                Log.e(TAG, "connectionState " + status);
            }
        });
        if (bluetoothChatService.getState() == BluetoothChatService.STATE_NONE) {
            bluetoothChatService.start();
        }
    }

    /**
     * This is used to dispose all running disposables.
     * We will cancel all the ongoing transfers here.
     */
    private void stopOnGoingTransfers() {
        if (communicationProtocol.isConnected()) {
            if (getFilesDisposable != null && !getFilesDisposable.isDisposed())
                getFilesDisposable.dispose();
            if (sendFilesDisposable != null && !sendFilesDisposable.isDisposed())
                sendFilesDisposable.dispose();
            if (getFoldersDisposable != null && !getFoldersDisposable.isDisposed())
                getFoldersDisposable.dispose();
            if (progressDisposable != null && !progressDisposable.isDisposed())
                progressDisposable.dispose();
        }
        isAllowNewRequests = true;
    }

    /**
     * @param wifiP2pDevices: list of available Wifi Direct peers
     *                        This call back is invoked, when a list of available wifi direct devices are found after scanning.
     */
    @Override
    public void availablePeers(ArrayList<WifiP2pDevice> wifiP2pDevices) {
        EventBus.getDefault().post(new WifiDevicesList(wifiP2pDevices));
        isAllowNewRequests = true;
    }

    /**
     * This is invoked when the client is ready.
     */
    @Override
    public void onTCPClientReady() {
        Log.e("TCPClientReady ", "onTCPClientReady");
    }

    /**
     * @param ip:   current Server's IP address
     * @param port: current Server's port
     *              This is invoked when we got the server credentials ready. We can generate QRCode from here.
     */
    @Override
    public void onTCPServerReady(String ip, Integer port) {
        isAllowNewRequests = true;
        EventBus.getDefault().post(new OnTCPInitialized(ip, port));
    }

    /**
     * @param rxConnection: The generated RXConnection object instance.
     * @param isWifiDirect: says if the connection is working on Wifi Direct or TCP local Wi-Fi
     */
    @Override
    public void onConnected(RXConnection rxConnection, boolean isWifiDirect) {
        isAllowNewRequests = true;
        if (serviceSelected != null) {
            AppStorage.getInstance(this).setValue(AppStorage.SP_DEVICE_ADDRESS, serviceSelected.deviceAddress);
            updateDBWithDetails();
        }
        toast("Connected");
        communicationProtocol.setRXConnection(rxConnection);
        EventBus.getDefault().post(new OnSocketConnected(isWifiDirect));
        if (!isWifiDirect) {
            requestForDeviceDetails();
        }
    }

    private void requestForDeviceDetails() {
        try {
            if (communicationProtocol.isConnected()) {
                isAllowNewRequests = false;
                progressDisposable = progressObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(progressObserver);
                Disposable getDeviceDetailsDisposable = SocialBladeProtocol.requestForDeviceDetails(
                        communicationProtocol.getConnection(), progressObservable).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(jsonObject -> {
                            isAllowNewRequests = true;
                            /*
                            jsonObject.put("name", "");
                            jsonObject.put("serialno", "");
                            jsonObject.put("MacAddress", "");
                            */
                            AppStorage.getInstance(this).setValue(WiFiDirectUtils.WIFI_DIRECT_REMOTE_DEVICE_NAME, jsonObject.getString("name"));
                            AppStorage.getInstance(this).setValue(WiFiDirectUtils.WIFI_DIRECT_DEVICE_ADDRESS, jsonObject.getString("MacAddress"));
                            AppStorage.getInstance(this).setValue(AppStorage.SP_DEVICE_ADDRESS, jsonObject.getString("MacAddress"));
                            updateDBWithDetails(jsonObject.getString("name"), jsonObject.getString("MacAddress"));
                            EventBus.getDefault().post(new OnSocketConnected(false));
                        }, error -> {
                            isAllowNewRequests = true;
                        });
                communicationProtocol.addDisposable(getDeviceDetailsDisposable);
            } else {
                isAllowNewRequests = true;
                toast("Connection not available");
            }
        } catch (Exception e) {
            isAllowNewRequests = true;
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "onCreateView: ", e);
        }
    }

    /**
     * This method is to update the devices connected and their favourite folders
     */
    private void updateDBWithDetails() {
        deviceModel = new DeviceModel();
        deviceModel.name = serviceSelected.deviceName;
        deviceModel.deviceAddress = serviceSelected.deviceAddress;
        deviceModel.id = dbHelper.addDeviceModel(deviceModel);
        if (deviceModel != null && deviceModel.id != -1) {
            if (dbHelper.getDeviceFavouritesModelArrayList(deviceModel.id).isEmpty()) {
                addDeviceFavModel(Environment.DIRECTORY_DOCUMENTS, false);
                addDeviceFavModel(Environment.DIRECTORY_DCIM, false);
                addDeviceFavModel(Environment.DIRECTORY_DOWNLOADS, false);
                addDeviceFavModel(Environment.DIRECTORY_DOWNLOADS + "/FileManager", true);
            }
        }
    }

    private void updateDBWithDetails(String name, String deviceAddress) {
        deviceModel = new DeviceModel();
        deviceModel.name = name;
        deviceModel.deviceAddress = deviceAddress;
        deviceModel.id = dbHelper.addDeviceModel(deviceModel);
        if (deviceModel != null && deviceModel.id != -1) {
            if (dbHelper.getDeviceFavouritesModelArrayList(deviceModel.id).isEmpty()) {
                addDeviceFavModel(Environment.DIRECTORY_DOCUMENTS, false);
                addDeviceFavModel(Environment.DIRECTORY_DCIM, false);
                addDeviceFavModel(Environment.DIRECTORY_DOWNLOADS, false);
                addDeviceFavModel(Environment.DIRECTORY_DOWNLOADS + "/FileManager", true);
            }
        }
    }

    /**
     * This method is for adding device folder as favourite
     *
     * @param path      : path of device folder to be fav
     * @param isDefault : is default destination
     */
    private void addDeviceFavModel(String path, boolean isDefault) {
        if (deviceModel != null && deviceModel.id != -1 && !TextUtils.isEmpty(path)) {
            DeviceFavouritesModel deviceFavouritesModel = generateDeviceFavModel(deviceModel.id, path);
            if (isDefault) deviceFavouritesModel.isDefault = true;
            deviceFavouritesModel.id = dbHelper.addDeviceFavouritesModel(deviceFavouritesModel);
        }
    }

    /**
     * This method is for adding device folder as favourite
     *
     * @param path : path of device folder to be fav
     * @param name : short name of the folder
     */
    private void addDeviceFavModel(String path, String name) {
        if (deviceModel != null && deviceModel.id != -1 && !TextUtils.isEmpty(path)) {
            DeviceFavouritesModel deviceFavouritesModel = new DeviceFavouritesModel();
            deviceFavouritesModel.deviceId = deviceModel.id;
            deviceFavouritesModel.isDefault = false;
            deviceFavouritesModel.path = path;
            deviceFavouritesModel.name = name;
            deviceFavouritesModel.id = dbHelper.addDeviceFavouritesModel(deviceFavouritesModel);
        }
    }

    /**
     * This method is for preparing device model
     *
     * @param deviceId : unique mac address for device
     * @param name     : visible name of the device.
     * @return
     */
    private DeviceFavouritesModel generateDeviceFavModel(long deviceId, String name) {
        String path = AppConstants.HOME_DIRECTORY.getAbsolutePath();
        String folder = path + "/" + name;
        DeviceFavouritesModel deviceFavouritesModel = new DeviceFavouritesModel();
        deviceFavouritesModel.deviceId = deviceId;
        deviceFavouritesModel.isDefault = false;
        deviceFavouritesModel.path = folder;
        deviceFavouritesModel.name = name;
        return deviceFavouritesModel;
    }

    /**
     * This callback is invoked if there is any issue or error while connecting to devices.
     *
     * @param e
     */
    @Override
    public void onError(Exception e) {
        if (e instanceof WifiNotConnectedException) {
            toast("Wifi connection Exception");
            EventBus.getDefault().post(new OnConnectionError());
        } else if (e instanceof WifiNotEnabledException) {
            toast("enable Wifi");
        } else if (e instanceof NoPeersFoundException) {
            toast("No peers found");
            EventBus.getDefault().post(new WifiDevicesList(null));
        } else if (e instanceof WifiP2pConnectionFailed) {
            toast("Connection failed");
            EventBus.getDefault().post(new OnConnectionError());
        } else e.printStackTrace();
        FirebaseCrashlytics.getInstance().recordException(e);
    }

    /**
     * This method is for connecting the device from list of available wifi peers
     *
     * @param serviceSelected
     */
    private void connectToWifiDirect(WifiP2pDevice serviceSelected) {
        this.serviceSelected = serviceSelected;
        if (connectionHelper != null) {
            connectionHelper.connect(this, serviceSelected, this);
        } else {
            Log.e(TAG, "connectToWifiDirect: ConnectionHelper is null");
        }
    }

    /**
     * Foreground notification showing that background service is running to the users.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "RXConnection Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * We will create Socket with private key and password here.
     */
    private void initializeRXConnection() {
        if (connectionHelper != null)
            connectionHelper.destroy(this);

        connectionHelper = new ConnectionHelper();
        connectionHelper.setSSL(true);
        //region create certificate
        char[] psw = (new char[]{'f', 'd', '6', '=', '(', 's', 'R', 'p', '*', 'S', '&', '+', '6', '\"', 'Y', 'M', '\'', '^', '[', 'c'});
        Certificate certificate = new Certificate(
                new File(new File(getFilesDir(), "cert"), "socialblade.private.p12")
                , psw);
//        //endregion
        connectionHelper.setPrivateCertificate(certificate);

        toast("Initializing connection...");

        connectionHelper.initialize(this, this, true, CommunicationProtocol.DEAULT_PORT);

    }

    /**
     * This method is for initialising the Socket for connection using QRCode
     */
    private void initializeQRConnection() {
        StaticUtils.setConnectionType(AppConstants.CONST_QR_CODE);
        if (connectionHelper != null)
            connectionHelper.destroy(this);

        connectionHelper = new ConnectionHelper();
        connectionHelper.setForceTCP(true);
        toast("Initializing connection...");
        connectionHelper.initialize(this, this, false, true, CommunicationProtocol.DEAULT_PORT);
    }

    private void initializeHPConnection() {
        if (connectionHelper != null)
            connectionHelper.destroy(this);

        connectionHelper = new ConnectionHelper();
        connectionHelper.setForceTCP(true);
        toast("Initializing connection...");
        connectionHelper.initialize(this, this, false, true, CommunicationProtocol.DEAULT_PORT);
    }

    /**
     * This method is for getting folders and files from the specified path. If empty path is specified, it will fetch root folder contents.
     *
     * @param folderPath
     */
    private void requestForFolders(String folderPath) {
        try {
            if (communicationProtocol.isConnected()) {
                isAllowNewRequests = false;
                progressDisposable = progressObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(progressObserver);
                toast("Started Fetching Data from Blade");
                getFoldersDisposable = SocialBladeProtocol.requestFolders(this, folderPath, communicationProtocol.getConnection(), progressObservable, false)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(jsonObject -> {
                            isAllowNewRequests = true;
                            EventBus.getDefault().post(new OnJSONObjectReceived(jsonObject));
                        }, error -> {
                            isAllowNewRequests = true;
                            Log.e(TAG, "fetchThumbnails: ", error);
                        });
                communicationProtocol.addDisposable(getFoldersDisposable);
            } else {
                toast("Connection not available");
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "onCreateView: ", e);
        }
    }

    /**
     * This method is for getting only folders from the specified path. If empty path is specified, it will fetch root folder contents.
     *
     * @param folderPath
     */
    private void requestForOnlyFolders(String folderPath) {
        try {
            if (communicationProtocol.isConnected()) {
                isAllowNewRequests = false;
                progressDisposable = progressObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(progressObserver);
                toast("Started Fetching Folders from Blade");
                getFoldersDisposable = SocialBladeProtocol.requestFolders(this, folderPath, communicationProtocol.getConnection(), progressObservable, true)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(jsonObject -> {
                            isAllowNewRequests = true;
                            EventBus.getDefault().post(new OnJSONObjectReceivedFolders(jsonObject));
                        }, error -> {
                            isAllowNewRequests = true;
                            Log.e(TAG, "fetchThumbnails: ", error);
                        });
                communicationProtocol.addDisposable(getFoldersDisposable);
            } else {
                toast("Connection not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /**
     * This method is used for sending single file from phone to blade.
     *
     * @param file
     */
    private void sendFileToBlade(File file) {
        sendFileToBlade(new String[]{file.getAbsolutePath()});
    }

    /**
     * This method is used for sending multiple files from phone to blade.
     *
     * @param files : string array of paths
     */
    private void sendFileToBlade(String[] files) {
        try {
            long fileSize = 0;
            String parentFolderPath = "";
            File firstItem = new File(files[0]);
            if (firstItem.exists()) {
                parentFolderPath = firstItem.getParent();
            }
            if (communicationProtocol.isConnected()) {
                isAllowNewRequests = false;
                ArrayList<File> fileArrayList = new ArrayList<>();
                for (String filePath : files) {
                    File file = new File(filePath);
                    fileArrayList.add(file);
                    if (file.isFile()) fileSize += file.length();
                }
                progressDisposable = progressObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(progressObserver);
                toast("Sending files to Blade");
                long finalFileSize = fileSize;
                transferModel = new TransferModel();
                try {
                    transferModel.name = fileArrayList.get(0).getName();
                    transferModel.timeStamp = DateUtils.getCurrentDate();
                    transferModel.progress = 0;
                    transferModel.status = AppConstants.CONST_TRANSFER_ONGOING;
                    transferModel.folderLocation = parentFolderPath;
                    transferModel.size = fileSize;
                    transferModel.isIncoming = false;
                    transferModel.id = dbHelper.addTransferModel(transferModel);
                    EventBus.getDefault().post(new OnProgressUpdated(transferModel));
                } catch (Exception e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                sendFilesDisposable = SocialBladeProtocol.sendOriginals(communicationProtocol.getConnection(), fileArrayList, progressObservable)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(jsonObject -> {
                            isAllowNewRequests = true;
                            toast("Sent files to blade.");
                            transferModel.name = fileArrayList.get(0).getName();
                            transferModel.rawData = getJsonDataForFiles(files);
                            transferModel.timeStamp = DateUtils.getCurrentDate();
                            transferModel.status = AppConstants.CONST_TRANSFER_COMPLETED;
                            transferModel.size = finalFileSize;
                            dbHelper.addTransferModel(transferModel);
                            EventBus.getDefault().post(new OnProgressUpdated(transferModel));
                        }, error -> {
                            isAllowNewRequests = true;
                            Log.e(TAG, "sendfile: ", error);
                            if (transferModel.id != -1) {
                                transferModel.status = AppConstants.CONST_TRANSFER_CANCELLED;
                                dbHelper.addTransferModel(transferModel);
                                EventBus.getDefault().post(new OnProgressUpdated(transferModel));
                            }
                        });
                communicationProtocol.addDisposable(sendFilesDisposable);
            } else {
                toast("Connection not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void sendFileToBladeBt(File file) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            Log.e("bt file size: ", bytes.length + "");
            bluetoothChatService.writeImg(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
//        sendFileToBlade(new String[]{file.getAbsolutePath()});
    }

    private void sendFileToBladeBt(String[] files) {
//        ArrayList<File> fileArrayList = new ArrayList<>();
//        for (String filePath : files) {
//            File file = new File(filePath);
//            fileArrayList.add(file);
////            if (file.isFile()) fileSize += file.length();
//        }
    }

    /**
     * This is used for setting the destination path of the files being sent from phone to bluetooth. This will send json object with commands.
     *
     * @param destinationPath : Path that is supposed to be the destination folder in Blade
     * @param file            : If file is not null, then send the file after setting path in Blade.
     * @param files           : If files is not null, then send the files after setting path in Blade.
     */
    private void setDestinationToBlade(String destinationPath, File file, String[] files) {
        try {
            if (communicationProtocol.isConnected()) {
                isAllowNewRequests = false;
                progressDisposable = progressObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(progressObserver);
                communicationProtocol.addDisposable(SocialBladeProtocol.setDestinationFolder(this, communicationProtocol.getConnection(), progressObservable, destinationPath)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(jsonObject -> {
                            isAllowNewRequests = true;
                            String name = destinationPath.replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
                            addDeviceFavModel(destinationPath, name);
                            toast("Succesfully set destination folder in Blade and added to favourites");
                            if (file != null)
                                sendFileToBlade(file);
                            else if (files != null) sendFileToBlade(files);
                        }, error -> {
                            isAllowNewRequests = true;
                            Log.e(TAG, "fetchThumbnails: ", error);
                        }));
            } else {
                toast("Connection not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /**
     * This is for requesting original files from Blade.
     *
     * @param size        : used for calculating progress
     * @param folderPaths : paths of the files to be imported from Blade.
     */
    private void requestForOriginals(long size, ArrayList<String> folderPaths) {
        try {
            if (communicationProtocol.isConnected() && folderPaths != null && !folderPaths.isEmpty()) {
                isAllowNewRequests = false;
                progressDisposable = progressObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(progressObserver);
                toast("Started Fetching Originals from Blade");
                transferModel = new TransferModel();
                try {
                    String name = folderPaths.get(0);
                    if (!TextUtils.isEmpty(name)) {
                        if (name.contains("/")) {
                            String[] spl = name.split("/");
                            name = spl.length > 0 ? spl[spl.length - 1] : "File";
                        }
                    } else {
                        name = "File";
                    }

                    transferModel.name = name;
                    transferModel.timeStamp = DateUtils.getCurrentDate();
                    transferModel.progress = 0;
                    transferModel.status = AppConstants.CONST_TRANSFER_ONGOING;
                    transferModel.size = size;
                    transferModel.folderLocation = SocialBladeProtocol.getExternalFolder(this).getAbsolutePath();
                    transferModel.isIncoming = true;
                    transferModel.id = dbHelper.addTransferModel(transferModel);
                    EventBus.getDefault().post(new OnProgressUpdated(transferModel));
                } catch (Exception e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                getFilesDisposable = SocialBladeProtocol.requestOriginals(this, communicationProtocol.getConnection(), folderPaths, progressObservable)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(filesList -> {
                            isAllowNewRequests = true;
                            EventBus.getDefault().post(new OnThumbsReceived(filesList));
                            transferModel.name = filesList.get(0) != null ? filesList.get(0).getName() : "File";
                            transferModel.rawData = getJsonDataForFiles(filesList);
                            transferModel.timeStamp = DateUtils.getCurrentDate();
                            transferModel.status = AppConstants.CONST_TRANSFER_COMPLETED;
                            transferModel.size = getFilesSize(filesList);
                            dbHelper.addTransferModel(transferModel);
                            EventBus.getDefault().post(new OnProgressUpdated(transferModel));
                        }, error -> {
                            isAllowNewRequests = true;
                            if (transferModel.id != -1) {
                                transferModel.status = AppConstants.CONST_TRANSFER_CANCELLED;
                                dbHelper.addTransferModel(transferModel);
                                EventBus.getDefault().post(new OnProgressUpdated(transferModel));
                            }
                            Log.e(TAG, "fetchOriginals: ", error);
                        });

                communicationProtocol.addDisposable(getFilesDisposable);
            } else {
                toast("Connection not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /**
     * Calculates the files size for progress calculation purpose
     *
     * @param filesList :list of files to be fetched.
     * @return
     */
    private long getFilesSize(List<File> filesList) {
        long fileSize = 0;
        for (File file : filesList) {
            if (file.isFile()) fileSize += file.length();
        }
        return fileSize;
    }

    /**
     * Prepare the files data to be sent.
     *
     * @param filesList
     * @return
     */
    private String getJsonDataForFiles(List<File> filesList) {
        JSONArray jsonArray = new JSONArray();
        for (File file : filesList) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("name", file.getName());
                jsonObject.put("path", file.getAbsolutePath());
            } catch (JSONException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            jsonArray.put(jsonObject);
        }
        return jsonArray.toString();
    }

    /**
     * Prepare the files data to be sent.
     *
     * @param filesList
     * @return
     */
    private String getJsonDataForFiles(String[] filesList) {
        JSONArray jsonArray = new JSONArray();
        for (String file : filesList) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("name", file.substring(file.lastIndexOf("/")));
                jsonObject.put("path", file);
            } catch (JSONException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            jsonArray.put(jsonObject);
        }
        return jsonArray.toString();
    }

    private void toast(String text) {
        try {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

}
