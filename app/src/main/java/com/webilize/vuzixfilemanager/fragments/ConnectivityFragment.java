package com.webilize.vuzixfilemanager.fragments;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.MainActivity;
import com.webilize.vuzixfilemanager.adapters.AvailableDevicesAdapter;
import com.webilize.vuzixfilemanager.databinding.FragmentConnectivityBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.services.RXConnectionFGService;
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

    public ConnectivityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        cp = CommunicationProtocol.getInstance();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1)
            if (resultCode == Activity.RESULT_OK) {
                fragmentConnectivityBinding.switchBluetooth.setChecked(mBluetoothAdapter.isEnabled());
            } else fragmentConnectivityBinding.switchBluetooth.setChecked(false);
        checkForControlsEnable();
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
                            startActivityForResult(enableBtIntent, 1);
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
                if (availableDevicesAdapter != null) availableDevicesAdapter.notifyDataSetChanged();
                initWifiDirect();
                DialogUtils.showProgressDialog(mainActivity, "Looking for devices...", v1 -> {
                    MainActivity.stopServiceManually(mainActivity);
                });
                break;
            case R.id.btnQRCode:
                initQRConnection();
                break;
            case R.id.btnScanForBTDevices:
                initBTConnection();
                break;
            default:
                break;
        }
    }

    private void initBTConnection() {
        BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            }
        };
        mBluetoothAdapter.startLeScan(scanCallback);
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
        try {
            fragmentConnectivityBinding.txtDeviceName.setText(StaticUtils.getDeviceName(mainActivity));
        } catch (Exception e) {
            e.printStackTrace();
        }
        DialogUtils.dismissProgressDialog();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (cp.isConnected()) {
                fragmentConnectivityBinding.txtDeviceName.setText(StaticUtils.getDeviceName(mainActivity));
            } else fragmentConnectivityBinding.txtDeviceName.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
