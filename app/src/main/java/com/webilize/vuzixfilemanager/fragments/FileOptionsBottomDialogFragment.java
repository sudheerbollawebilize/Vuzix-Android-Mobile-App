package com.webilize.vuzixfilemanager.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.FragmentFileOptionsBinding;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

public class FileOptionsBottomDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {
    private FragmentFileOptionsBinding fragmentFileOptionsBinding;
    private boolean isFolder, isExternal;
    private String path;
    private CommunicationProtocol cp;

    public static FileOptionsBottomDialogFragment newInstance(FileFolderItem fileFolderItem) {
        FileOptionsBottomDialogFragment fileOptionsBottomDialogFragment = new FileOptionsBottomDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("isExternal", fileFolderItem.file == null);
        bundle.putString("path", fileFolderItem.file == null ? fileFolderItem.usbFile.getAbsolutePath() : fileFolderItem.file.getAbsolutePath());
        bundle.putBoolean("isFolder", fileFolderItem.file == null ? fileFolderItem.usbFile.isDirectory() : fileFolderItem.file.isDirectory());
        fileOptionsBottomDialogFragment.setArguments(bundle);
        return fileOptionsBottomDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cp = CommunicationProtocol.getInstance();
        if (getArguments() != null) {
            if (getArguments().containsKey("isExternal"))
                isExternal = getArguments().getBoolean("isExternal", false);
            if (getArguments().containsKey("isFolder"))
                isFolder = getArguments().getBoolean("isFolder", false);
            if (getArguments().containsKey("path"))
                path = getArguments().getString("path", AppConstants.homeDirectory.getAbsolutePath());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentFileOptionsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_file_options, container, false);
        initComponents();
        return fragmentFileOptionsBinding.getRoot();
    }

    private void initComponents() {
        if (cp.isConnected()) {
            fragmentFileOptionsBinding.txtSendToDevice.setEnabled(true);
            fragmentFileOptionsBinding.txtSendToDevice.setClickable(true);
            fragmentFileOptionsBinding.txtSendToDevice.setAlpha(1f);
        } else {
            fragmentFileOptionsBinding.txtSendToDevice.setEnabled(false);
            fragmentFileOptionsBinding.txtSendToDevice.setClickable(false);
            fragmentFileOptionsBinding.txtSendToDevice.setAlpha(0.5f);
        }
        if (isFolder) {
            fragmentFileOptionsBinding.txtShare.setVisibility(View.GONE);
            if (StaticUtils.getBookMarkedLocations(getActivity()).contains(path)) {
                fragmentFileOptionsBinding.txtBookMark.setVisibility(View.GONE);
                fragmentFileOptionsBinding.txtRemoveBookMark.setVisibility(View.VISIBLE);
            } else {
                fragmentFileOptionsBinding.txtBookMark.setVisibility(View.VISIBLE);
                fragmentFileOptionsBinding.txtRemoveBookMark.setVisibility(View.GONE);
            }
        } else {
            fragmentFileOptionsBinding.txtShare.setVisibility(View.VISIBLE);
            fragmentFileOptionsBinding.txtBookMark.setVisibility(View.GONE);
            fragmentFileOptionsBinding.txtRemoveBookMark.setVisibility(View.GONE);
        }
        if (isExternal) {
            fragmentFileOptionsBinding.txtShare.setVisibility(View.GONE);
            fragmentFileOptionsBinding.txtRename.setVisibility(View.GONE);
            fragmentFileOptionsBinding.txtRemoveBookMark.setVisibility(View.GONE);
            fragmentFileOptionsBinding.txtBookMark.setVisibility(View.GONE);
        }
        fragmentFileOptionsBinding.txtCopy.setOnClickListener(this);
        fragmentFileOptionsBinding.txtDelete.setOnClickListener(this);
        fragmentFileOptionsBinding.txtRename.setOnClickListener(this);
        fragmentFileOptionsBinding.txtSendToDevice.setOnClickListener(this);
        fragmentFileOptionsBinding.txtShare.setOnClickListener(this);
        fragmentFileOptionsBinding.txtRemoveBookMark.setOnClickListener(this);
        fragmentFileOptionsBinding.txtBookMark.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        intent.putExtra("viewid", view.getId());
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        dismiss();
    }

}