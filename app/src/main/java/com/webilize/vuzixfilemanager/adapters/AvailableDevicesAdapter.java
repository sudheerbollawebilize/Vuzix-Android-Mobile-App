package com.webilize.vuzixfilemanager.adapters;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemDevicesBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;

import java.util.ArrayList;

public class AvailableDevicesAdapter extends RecyclerView.Adapter<AvailableDevicesAdapter.DevicesViewHolder> {

    private ArrayList<WifiP2pDevice> wifiP2pDeviceArrayList;
    private IClickListener iClickListener;
    private boolean isConnected;

    public AvailableDevicesAdapter(ArrayList<WifiP2pDevice> fileFolderItemArrayList, IClickListener iClickListener, boolean isConnected) {
        this.isConnected = isConnected;
        this.wifiP2pDeviceArrayList = fileFolderItemArrayList;
        this.iClickListener = iClickListener;
    }

    @NonNull
    @Override
    public DevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DevicesViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_devices, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesViewHolder fileFoldersViewHolder, int position) {
        WifiP2pDevice wifiP2pDevice = wifiP2pDeviceArrayList.get(position);
        fileFoldersViewHolder.itemFolderBinding.txtConnect.setText(isConnected ? R.string.disconnect : R.string.connect);
        fileFoldersViewHolder.itemFolderBinding.txtDeviceName.setText(wifiP2pDevice.deviceName + "\n" + wifiP2pDevice.deviceAddress);
        fileFoldersViewHolder.itemFolderBinding.txtConnect.setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
        fileFoldersViewHolder.itemFolderBinding.txtConnect.setOnLongClickListener(v -> {
            if (iClickListener != null) iClickListener.onLongClick(v, position);
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return wifiP2pDeviceArrayList.size();
    }

    static class DevicesViewHolder extends RecyclerView.ViewHolder {
        ItemDevicesBinding itemFolderBinding;

        DevicesViewHolder(@NonNull ItemDevicesBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFolderBinding = itemFileFolderBinding;
        }
    }

}
