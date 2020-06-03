package com.webilize.vuzixfilemanager.fragments;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.BtDeviceListActivity;
import com.webilize.vuzixfilemanager.activities.MainActivity;
import com.webilize.vuzixfilemanager.adapters.AvailableDevicesAdapter;
import com.webilize.vuzixfilemanager.databinding.FragmentConnectivityBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.services.RXConnectionFGService;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.DialogUtils;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.eventbus.OnConnectionError;
import com.webilize.vuzixfilemanager.utils.eventbus.OnSocketConnected;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class ConnectivityFragment extends BaseFragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, IClickListener {

    private FragmentConnectivityBinding fragmentConnectivityBinding;

    private View rootView;
    private MainActivity mainActivity;
    private boolean isBluetoothSupported;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private WifiManager wifiMgr;
    private AvailableDevicesAdapter availableDevicesAdapter, connectedDevicesAdapter;
    private ArrayList<WifiP2pDevice> wifiP2pDeviceArrayList, connectedDevicesArrayList;
    private int selectedPos;
    private CommunicationProtocol cp;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private boolean mLocationPermission = false;
    private boolean mSettingPermission = true;

    public ConnectivityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        cp = CommunicationProtocol.getInstance();
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    void initComponents() {
        wifiP2pDeviceArrayList = new ArrayList<>();
        connectedDevicesArrayList = new ArrayList<>();
        wifiP2pDeviceArrayList.addAll(mainActivity.viewModel.getAvailableWifiP2PDevices());
        connectedDevicesArrayList.addAll(mainActivity.viewModel.getConnectedWifiP2PDevices());
        bluetoothManager = (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        updateBluetoothSwitch();
        updateWifiSwitch();
        setListeners();
        setAdapters();
        checkForControlsEnable();
    }

    private void setListeners() {
        fragmentConnectivityBinding.switchWifi.setOnCheckedChangeListener(this);
        fragmentConnectivityBinding.switchBluetooth.setOnCheckedChangeListener(this);
        fragmentConnectivityBinding.btnScanForDevices.setOnClickListener(this);
        fragmentConnectivityBinding.btnQRCode.setOnClickListener(this);
        fragmentConnectivityBinding.btnScanForBTDevices.setOnClickListener(this);
        fragmentConnectivityBinding.btnHotSpot.setOnClickListener(this);
    }

    private void checkForControlsEnable() {
        if (fragmentConnectivityBinding.switchWifi.isChecked() || fragmentConnectivityBinding.switchBluetooth.isChecked()) {
            fragmentConnectivityBinding.btnScanForDevices.setEnabled(true);
            fragmentConnectivityBinding.btnQRCode.setEnabled(true);
        } else {
            fragmentConnectivityBinding.btnScanForDevices.setEnabled(false);
            fragmentConnectivityBinding.btnQRCode.setEnabled(false);
        }
        if (wifiP2pDeviceArrayList.isEmpty()) {
            fragmentConnectivityBinding.txtAvailableDevices.setVisibility(View.GONE);
            fragmentConnectivityBinding.recyclerViewAvailableDevices.setVisibility(View.GONE);
        } else {
            fragmentConnectivityBinding.txtAvailableDevices.setVisibility(View.VISIBLE);
            fragmentConnectivityBinding.recyclerViewAvailableDevices.setVisibility(View.VISIBLE);
        }
        if (connectedDevicesArrayList.isEmpty()) {
            fragmentConnectivityBinding.txtConnectedDevices.setVisibility(View.GONE);
            fragmentConnectivityBinding.recyclerViewConnectedDevices.setVisibility(View.GONE);
        } else {
            fragmentConnectivityBinding.txtConnectedDevices.setVisibility(View.VISIBLE);
            fragmentConnectivityBinding.recyclerViewConnectedDevices.setVisibility(View.VISIBLE);
        }
    }

    private void updateWifiSwitch() {
        wifiMgr = (WifiManager) mainActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (StaticUtils.isWifiOn(mainActivity)) {
            fragmentConnectivityBinding.switchWifi.setChecked(true);
        } else fragmentConnectivityBinding.switchWifi.setChecked(false);

    }

    private void updateBluetoothSwitch() {
//        mBluetoothAdapter = bluetoothManager.getAdapter();
        isBluetoothSupported = mainActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (isBluetoothSupported) {
            fragmentConnectivityBinding.switchBluetooth.setEnabled(true);
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                fragmentConnectivityBinding.switchBluetooth.setEnabled(false);
                fragmentConnectivityBinding.switchBluetooth.setChecked(false);
            } else if (mBluetoothAdapter.isEnabled()) {
                fragmentConnectivityBinding.switchBluetooth.setEnabled(true);
                fragmentConnectivityBinding.switchBluetooth.setChecked(true);
//                ensureDiscoverable();
            } else {
                fragmentConnectivityBinding.switchBluetooth.setEnabled(true);
                fragmentConnectivityBinding.switchBluetooth.setChecked(false);
            }
        } else {
            fragmentConnectivityBinding.switchBluetooth.setEnabled(false);
            fragmentConnectivityBinding.switchBluetooth.setChecked(false);
        }
        fragmentConnectivityBinding.switchBluetooth.setOnCheckedChangeListener(this);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentConnectivityBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_connectivity, container, false);
        rootView = fragmentConnectivityBinding.getRoot();
        return rootView;
    }

    private void connectDevice(Intent data, boolean secure) {
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(BtDeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        initBTService(device, secure);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()) {
            case R.id.switchBluetooth:
                if (isBluetoothSupported && mBluetoothAdapter != null) {
                    if (isChecked)
                        if (mBluetoothAdapter.isEnabled()) {
                            mBluetoothAdapter.disable();
                        } else {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        }
                    else if (mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                    }
                }
                checkForControlsEnable();
                break;
            case R.id.switchWifi:
                wifiMgr.setWifiEnabled(isChecked);
                checkForControlsEnable();
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnScanForDevices:
                if (!cp.isConnected()) {
                    if (availableDevicesAdapter != null)
                        availableDevicesAdapter.notifyDataSetChanged();
                    initWifiDirect();
                    DialogUtils.showProgressDialog(mainActivity, "Looking for devices...", v1 -> {
                        MainActivity.stopServiceManually(mainActivity);
                    });
                } else
                    StaticUtils.showToast(mainActivity, getString(R.string.another_connection_is_active));
                break;
            case R.id.btnQRCode:
                if (!cp.isConnected()) {
                    initQRConnection();
                } else
                    StaticUtils.showToast(mainActivity, getString(R.string.another_connection_is_active));
                break;
            case R.id.btnHotSpot:
                if (!cp.isConnected()) {
                    initHPConnection();
                } else
                    StaticUtils.showToast(mainActivity, getString(R.string.another_connection_is_active));
                break;
            case R.id.btnScanForBTDevices:
                if (!cp.isConnected()) {
                    ensureDiscoverable();
                    initBTConnection();
                } else
                    StaticUtils.showToast(mainActivity, getString(R.string.another_connection_is_active));
                break;
            default:
                break;
        }
    }

    private void initBTConnection() {
        initBTService();
//        Intent serverIntent = new Intent(getActivity(), BtDeviceListActivity.class);
//        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
    }

    private void initWifiDirect() {
        Intent serviceIntent = new Intent(mainActivity, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "start");
        ContextCompat.startForegroundService(mainActivity, serviceIntent);
    }

    private void initQRConnection() {
        Intent serviceIntent = new Intent(mainActivity, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "start");
        serviceIntent.putExtra("IsQr", true);
        ContextCompat.startForegroundService(mainActivity, serviceIntent);
    }

    private void initBTService(BluetoothDevice device, boolean secure) {
        Intent serviceIntent = new Intent(mainActivity, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "start");
        serviceIntent.putExtra("IsBle", true);
        serviceIntent.putExtra("device", device);
        serviceIntent.putExtra("secure", secure);
        ContextCompat.startForegroundService(mainActivity, serviceIntent);
    }

    private void initBTService() {
        Intent serviceIntent = new Intent(mainActivity, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "start");
        serviceIntent.putExtra("IsBle", true);
        ContextCompat.startForegroundService(mainActivity, serviceIntent);
    }

    private void initHPConnection() {
        proceedWithOreoHotSpot();
    }

    private void settingPermission() {
        mSettingPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(mainActivity.getApplicationContext())) {
                mSettingPermission = false;
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + mainActivity.getPackageName()));
                startActivityForResult(intent, AppConstants.MY_PERMISSIONS_MANAGE_WRITE_SETTINGS);
            }
        }
    }

    private void locationsPermission() {
        mLocationPermission = true;
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mLocationPermission = false;
            if (ActivityCompat.shouldShowRequestPermissionRationale(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, AppConstants.MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        }
    }

    private void openHotSpotDialog() {
        DialogUtils.showHotSpotDialog(mainActivity, (dialog, which) -> startHotSpot(), (dialog, which) -> stopHotSpot());
    }

    private void startHotSpot() {
        Intent intent = new Intent(AppConstants.ACTION_HOTSPOT_TURNON);
        StaticUtils.sendImplicitBroadcast(mainActivity, intent);
    }

    private void stopHotSpot() {
        Intent intent = new Intent(AppConstants.ACTION_HOTSPOT_TURNOFF);
        StaticUtils.sendImplicitBroadcast(mainActivity, intent);
    }

    private void proceedWithOreoHotSpot() {
        settingPermission();
        locationsPermission();
        if (mLocationPermission && mSettingPermission) openHotSpotDialog();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT)
            if (resultCode == Activity.RESULT_OK) {
                fragmentConnectivityBinding.switchBluetooth.setChecked(mBluetoothAdapter.isEnabled());
            } else fragmentConnectivityBinding.switchBluetooth.setChecked(false);
        else if (requestCode == REQUEST_CONNECT_DEVICE_INSECURE) {
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
        } else if (requestCode == REQUEST_CONNECT_DEVICE_SECURE) {
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
        } else if (requestCode == AppConstants.MY_PERMISSIONS_MANAGE_WRITE_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                mSettingPermission = true;
                if (!mLocationPermission) locationsPermission();
            } else {
                proceedWithOreoHotSpot();
            }
        } else if (requestCode == AppConstants.MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                mLocationPermission = true;
                if (!mSettingPermission) settingPermission();
            } else {
                proceedWithOreoHotSpot();
            }
        }
        checkForControlsEnable();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConstants.MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            locationsPermission();
        }
    }

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onWifiDevicesListFound() {
        if (wifiP2pDeviceArrayList != null) wifiP2pDeviceArrayList.clear();
        wifiP2pDeviceArrayList.addAll(mainActivity.viewModel.getAvailableWifiP2PDevices());
        setAdapters();
        DialogUtils.dismissProgressDialog();
    }

    private void setAdapters() {
        setAvailableDevicesAdapter();
        setConnectedDevicesAdapter();
        checkForControlsEnable();
    }

    private void setAvailableDevicesAdapter() {
        fragmentConnectivityBinding.recyclerViewAvailableDevices.setLayoutManager(new LinearLayoutManager(mainActivity));
        availableDevicesAdapter = new AvailableDevicesAdapter(mainActivity, wifiP2pDeviceArrayList, this, false);
        fragmentConnectivityBinding.recyclerViewAvailableDevices.setAdapter(availableDevicesAdapter);
    }

    private void setConnectedDevicesAdapter() {
        fragmentConnectivityBinding.recyclerViewConnectedDevices.setLayoutManager(new LinearLayoutManager(mainActivity));
        connectedDevicesAdapter = new AvailableDevicesAdapter(mainActivity, connectedDevicesArrayList, this, true);
        fragmentConnectivityBinding.recyclerViewConnectedDevices.setAdapter(connectedDevicesAdapter);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionError(OnConnectionError connectionError) {
        StaticUtils.showToast(mainActivity, "Error connecting to device");
        DialogUtils.dismissProgressDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketConnected(OnSocketConnected onSocketConnected) {
        if (onSocketConnected.isWififDirect) {
            mainActivity.viewModel.updateConnectedWifiP2PDevices(selectedPos);
            wifiP2pDeviceArrayList = mainActivity.viewModel.getAvailableWifiP2PDevices();
            connectedDevicesArrayList = mainActivity.viewModel.getConnectedWifiP2PDevices();
            setAdapters();
        }
        StaticUtils.showToast(mainActivity, "Connected");
        fragmentConnectivityBinding.txtConnectionType.setText(StaticUtils.getConnectionType());
        DialogUtils.dismissProgressDialog();
        try {
            if (cp.isConnected()) {
                fragmentConnectivityBinding.txtDeviceName.setText(StaticUtils.getDeviceName(mainActivity));
                fragmentConnectivityBinding.btnQRCode.setEnabled(false);
                fragmentConnectivityBinding.btnScanForDevices.setEnabled(false);
                fragmentConnectivityBinding.btnHotSpot.setEnabled(false);
                fragmentConnectivityBinding.btnScanForBTDevices.setEnabled(false);
            } else {
                fragmentConnectivityBinding.txtDeviceName.setText("");
                fragmentConnectivityBinding.btnQRCode.setEnabled(true);
                fragmentConnectivityBinding.btnScanForDevices.setEnabled(true);
                fragmentConnectivityBinding.btnHotSpot.setEnabled(true);
                fragmentConnectivityBinding.btnScanForBTDevices.setEnabled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (cp.isConnected()) {
                fragmentConnectivityBinding.txtDeviceName.setText(StaticUtils.getDeviceName(mainActivity));
                fragmentConnectivityBinding.btnQRCode.setEnabled(false);
                fragmentConnectivityBinding.btnScanForDevices.setEnabled(false);
                fragmentConnectivityBinding.btnHotSpot.setEnabled(false);
                fragmentConnectivityBinding.btnScanForBTDevices.setEnabled(false);
            } else {
                fragmentConnectivityBinding.txtDeviceName.setText("");
                fragmentConnectivityBinding.btnQRCode.setEnabled(true);
                fragmentConnectivityBinding.btnScanForDevices.setEnabled(true);
                fragmentConnectivityBinding.btnHotSpot.setEnabled(true);
                fragmentConnectivityBinding.btnScanForBTDevices.setEnabled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        fragmentConnectivityBinding.txtConnectionType.setText(StaticUtils.getConnectionType());
    }

    @Override
    public void onClick(View view, int position) {
        try {
            if (view instanceof TextView) {
                selectedPos = position;
                if (((TextView) view).getText().toString().equalsIgnoreCase(getString(R.string.connect))) {
                    connectToWifiDirect(wifiP2pDeviceArrayList.get(position));
                } else {
                    MainActivity.stopServiceManually(mainActivity);
                    mainActivity.viewModel.removeConnectedWifiP2PDevices(selectedPos);
                    wifiP2pDeviceArrayList = mainActivity.viewModel.getAvailableWifiP2PDevices();
                    connectedDevicesArrayList = mainActivity.viewModel.getConnectedWifiP2PDevices();
                    setAdapters();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectToWifiDirect(WifiP2pDevice serviceSelected) {
        Intent serviceIntent = new Intent(mainActivity, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "connect");
        serviceIntent.putExtra("wifiDevice", serviceSelected);
        ContextCompat.startForegroundService(mainActivity, serviceIntent);
        DialogUtils.showProgressDialog(mainActivity, "Connecting to " + serviceSelected.deviceName + " ...", v1 -> {
            MainActivity.stopServiceManually(mainActivity);
        });
    }

    @Override
    public void onLongClick(View view, int position) {

    }

}
