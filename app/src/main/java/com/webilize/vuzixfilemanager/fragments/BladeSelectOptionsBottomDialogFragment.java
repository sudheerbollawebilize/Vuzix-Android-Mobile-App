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
import com.webilize.vuzixfilemanager.databinding.FragmentBladeSelectOptionsBinding;
import com.webilize.vuzixfilemanager.models.BladeItem;
import com.webilize.vuzixfilemanager.utils.transferutils.CommunicationProtocol;

public class BladeSelectOptionsBottomDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {
    private FragmentBladeSelectOptionsBinding fragmentFileOptionsBinding;
    private BladeItem bladeItem;
    private CommunicationProtocol cp;

    public static BladeSelectOptionsBottomDialogFragment newInstance(BladeItem bladeItem) {
        BladeSelectOptionsBottomDialogFragment fileOptionsBottomDialogFragment = new BladeSelectOptionsBottomDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("bladeItem", bladeItem);
        fileOptionsBottomDialogFragment.setArguments(bundle);
        return fileOptionsBottomDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey("bladeItem"))
                bladeItem = getArguments().getParcelable("bladeItem");
        }
        cp = CommunicationProtocol.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentFileOptionsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_blade_select_options, container, false);
        initComponents();
        return fragmentFileOptionsBinding.getRoot();
    }

    private void initComponents() {
        if (cp.isConnected()) {
            fragmentFileOptionsBinding.txtSendToFolder.setEnabled(true);
            fragmentFileOptionsBinding.txtSetFavourite.setEnabled(true);
        } else {
            fragmentFileOptionsBinding.txtSendToFolder.setEnabled(false);
            fragmentFileOptionsBinding.txtSetFavourite.setEnabled(false);
        }
        fragmentFileOptionsBinding.txtSetFavourite.setOnClickListener(this);
        fragmentFileOptionsBinding.txtSendToFolder.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        intent.putExtra("viewid", view.getId());
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        dismiss();
    }

}