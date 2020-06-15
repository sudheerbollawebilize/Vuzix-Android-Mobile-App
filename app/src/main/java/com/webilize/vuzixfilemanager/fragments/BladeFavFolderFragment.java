package com.webilize.vuzixfilemanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.BladeFoldersActivity;
import com.webilize.vuzixfilemanager.adapters.FavouriteFolderAdapter;
import com.webilize.vuzixfilemanager.databinding.FragmentBladeFolderSelectBinding;
import com.webilize.vuzixfilemanager.dbutils.DBHelper;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.DeviceFavouritesModel;
import com.webilize.vuzixfilemanager.models.DeviceModel;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.DialogUtils;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import java.util.ArrayList;

public class BladeFavFolderFragment extends BaseFragment implements IClickListener {

    private FragmentBladeFolderSelectBinding folderFragmentBinding;
    private static final String ARG_FOLDER_ITEM = "ARG_FOLDER_ITEM";
    private FavouriteFolderAdapter fileFoldersAdapter;
    private BladeFoldersActivity bladeFoldersActivity;
    private String folderPath = "";
    private PopupMenu popupMenu;
    private DeviceFavouritesModel selectedFile;
    private int counter = 0;
    private CommunicationProtocol cp;
    private ArrayList<DeviceFavouritesModel> bladeItemArrayList;

    public static BladeFavFolderFragment newInstance(String folderPath) {
        BladeFavFolderFragment bladeFolderFragment = new BladeFavFolderFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_FOLDER_ITEM, folderPath);
        bladeFolderFragment.setArguments(bundle);
        return bladeFolderFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            bladeFoldersActivity = (BladeFoldersActivity) context;
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cp = CommunicationProtocol.getInstance();
        getBundleData();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        folderFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_blade_folder_select, container, false);
        bladeItemArrayList = new ArrayList<>();
        folderFragmentBinding.progressBar.setVisibility(View.VISIBLE);
        bladeItemArrayList = new ArrayList<>();
        String dev = AppStorage.getInstance(bladeFoldersActivity).getValue(AppStorage.SP_DEVICE_ADDRESS, "");

        DeviceModel deviceModel = new DBHelper(bladeFoldersActivity).getDeviceModel(dev);
        bladeItemArrayList.addAll(new DBHelper(bladeFoldersActivity).getDeviceFavouritesModelArrayList(deviceModel.id));
        setRecyclerViewAdapter();
        updateListVisibility();

        folderFragmentBinding.progressBar.setVisibility(View.GONE);
        return folderFragmentBinding.getRoot();
    }

    @Override
    void initComponents() {
    }

    @Override
    public void onClick(View view, int position) {
        try {
            selectedFile = bladeItemArrayList.get(position);
            if (view.getId() == R.id.imgRemove) {
                DialogUtils.showDeleteDialog(bladeFoldersActivity, "Are you sure you want to remove the favourite location?",
                        (dialog, which) -> {
                            new DBHelper(bladeFoldersActivity).deleteDeviceFav(selectedFile.id);
                            bladeItemArrayList.remove(position);
                            fileFoldersAdapter.notifyDataSetChanged();
                            updateListVisibility();
                        });
            } else {
                DialogUtils.showDeleteDialog(bladeFoldersActivity,
                        "Are you sure you want to set this folder as default destination?",
                        "Set Default", (dialog, which) -> {
                            bladeFoldersActivity.sendToFolder(selectedFile.path);
                        }
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    public void onLongClick(View view, int position) {
    }

    private void getBundleData() {
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            if (bundle.containsKey(ARG_FOLDER_ITEM)) {
                folderPath = bundle.getString(ARG_FOLDER_ITEM, "");
            }
        }
    }

    private void setRecyclerViewAdapter() {
        folderFragmentBinding.recyclerView.setLayoutManager(new LinearLayoutManager(bladeFoldersActivity));
        fileFoldersAdapter = new FavouriteFolderAdapter(bladeFoldersActivity, bladeItemArrayList, this);
        folderFragmentBinding.recyclerView.setAdapter(fileFoldersAdapter);
    }

    private void updateListVisibility() {
        if (cp.isConnected()) {
            if (bladeItemArrayList == null || bladeItemArrayList.isEmpty()) {
                folderFragmentBinding.recyclerView.setVisibility(View.GONE);
                folderFragmentBinding.txtNoDataFound.setVisibility(View.VISIBLE);
                folderFragmentBinding.txtNoDataFound.setText(R.string.folder_is_empty);
            } else {
                folderFragmentBinding.recyclerView.setVisibility(View.VISIBLE);
                folderFragmentBinding.txtNoDataFound.setVisibility(View.GONE);
            }
        } else {
            folderFragmentBinding.recyclerView.setVisibility(View.GONE);
            folderFragmentBinding.txtNoDataFound.setVisibility(View.VISIBLE);
            folderFragmentBinding.txtNoDataFound.setText(R.string.currently_no_device_is_connected_nplease_connect_to_blade_to_view_data);
        }
    }

}