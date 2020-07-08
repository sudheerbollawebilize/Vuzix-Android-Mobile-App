package com.webilize.vuzixfilemanager.fragments;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    abstract void initComponents();

    @Override
    public void onStart() {
        super.onStart();
        initComponents();
    }

}
