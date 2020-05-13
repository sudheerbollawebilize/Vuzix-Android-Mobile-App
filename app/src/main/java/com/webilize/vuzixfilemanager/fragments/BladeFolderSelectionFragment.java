package com.webilize.vuzixfilemanager.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.activities.BladeFoldersActivity;
import com.webilize.vuzixfilemanager.adapters.BladeFileFoldersAdapter;
import com.webilize.vuzixfilemanager.databinding.FragmentBladeFolderSelectBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.BladeItem;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

public class BladeFolderSelectionFragment extends BaseFragment implements IClickListener {

    private FragmentBladeFolderSelectBinding folderFragmentBinding;
    private static final String ARG_FOLDER_ITEM = "ARG_FOLDER_ITEM";
    private static final String ARG_FOLDER_LIST = "ARG_FOLDER_LIST";
    private BladeFileFoldersAdapter fileFoldersAdapter;
    private BladeFoldersActivity bladeFoldersActivity;
    private String folderPath = "";
    private PopupMenu popupMenu;
    private BladeItem selectedFile;
    private int counter = 0;
    private CommunicationProtocol cp;
    private ArrayList<BladeItem> bladeItemArrayList;

    public static BladeFolderSelectionFragment newInstance(ArrayList<BladeItem> bladeItemArrayList) {
        BladeFolderSelectionFragment bladeFolderFragment = new BladeFolderSelectionFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(ARG_FOLDER_LIST, bladeItemArrayList);
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
        folderFragmentBinding.progressBar.setVisibility(View.VISIBLE);
        if (cp.isConnected()) {
            setRecyclerViewAdapter();
            updateListVisibility();
        }
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
            if (view.getId() == R.id.imgMore) {
                showMoreBottomSheet();
            } else {
                if (selectedFile.size > 0) {
                    bladeFoldersActivity.requestForBladeFolders(selectedFile.path);
                } else StaticUtils.showToast(bladeFoldersActivity, "no folders inside this folder");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLongClick(View view, int position) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data != null && data.hasExtra("viewid")) {
                onBottomSheetItemClicked(data.getIntExtra("viewid", 0));
            }
        }
    }

//    @Override
//    public void onStop() {
//        EventBus.getDefault().unregister(this);
//        super.onStop();
//    }
//
//    @Override
//    public void onStart() {
//        EventBus.getDefault().register(this);
//        super.onStart();
//    }

    private void getBundleData() {
        bladeItemArrayList = new ArrayList<>();
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            if (bundle.containsKey(ARG_FOLDER_ITEM)) {
                folderPath = bundle.getString(ARG_FOLDER_ITEM, "");
            }
            if (bundle.containsKey(ARG_FOLDER_LIST)) {
                bladeItemArrayList = bundle.getParcelableArrayList(ARG_FOLDER_LIST);
            }
        }
    }

    private void setRecyclerViewAdapter() {
        folderFragmentBinding.recyclerView.setLayoutManager(new LinearLayoutManager(bladeFoldersActivity));
        fileFoldersAdapter = new BladeFileFoldersAdapter(bladeFoldersActivity, bladeItemArrayList, this);
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

    private void showMoreBottomSheet() {
        BladeSelectOptionsBottomDialogFragment addPhotoBottomDialogFragment = BladeSelectOptionsBottomDialogFragment.newInstance(selectedFile);
        addPhotoBottomDialogFragment.setTargetFragment(this, 0);
        addPhotoBottomDialogFragment.show(bladeFoldersActivity.getSupportFragmentManager(), BladeSelectOptionsBottomDialogFragment.class.getSimpleName());
    }

    private void onBottomSheetItemClicked(int viewid) {
        switch (viewid) {
            case R.id.txtSendToFolder:
                bladeFoldersActivity.sendToFolder(selectedFile.path);
                break;
            case R.id.txtSetFavourite:
                bladeFoldersActivity.addToFavourites(selectedFile);
                break;
            default:
                break;
        }
    }

}