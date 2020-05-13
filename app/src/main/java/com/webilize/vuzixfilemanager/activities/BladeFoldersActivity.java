package com.webilize.vuzixfilemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ActivityBladeFoldersBinding;
import com.webilize.vuzixfilemanager.fragments.BladeFavFolderFragment;
import com.webilize.vuzixfilemanager.fragments.BladeFolderSelectionFragment;
import com.webilize.vuzixfilemanager.models.BladeItem;
import com.webilize.vuzixfilemanager.services.RXConnectionFGService;
import com.webilize.vuzixfilemanager.utils.StaticUtils;
import com.webilize.vuzixfilemanager.utils.eventbus.OnJSONObjectReceivedFolders;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class BladeFoldersActivity extends BaseActivity implements View.OnClickListener {

    private ActivityBladeFoldersBinding activityBladeFoldersBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityBladeFoldersBinding = DataBindingUtil.setContentView(this, R.layout.activity_blade_folders);
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    void initComponents() {
        setListeners();
    }

    public void requestForBladeFolders(String folderPath) {
        Intent serviceIntent = new Intent(this, RXConnectionFGService.class);
        serviceIntent.putExtra("inputExtra", "folder");
        serviceIntent.putExtra("folderPath", folderPath);
        serviceIntent.putExtra("isOnlyfolders", true);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void setListeners() {
        activityBladeFoldersBinding.imgBack.setOnClickListener(this);
        activityBladeFoldersBinding.switchFavourites.setOnCheckedChangeListener((buttonView, isChecked) -> {
            clearBackStackCompletely();
            if (isChecked) {
                activityBladeFoldersBinding.switchFavourites.setText(R.string.show_favourite_folders);
                requestForFavouriteFolders();
            } else {
                activityBladeFoldersBinding.switchFavourites.setText(R.string.show_all_folders);
                requestForBladeFolders("");
            }
        });
        activityBladeFoldersBinding.switchFavourites.setText(R.string.show_favourite_folders);
        activityBladeFoldersBinding.switchFavourites.setChecked(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onJSONObjectReceivedFolders(OnJSONObjectReceivedFolders onJSONObjectReceivedFolders) {
        ArrayList<BladeItem> bladeItemArrayList = new ArrayList<>();
        try {
            JSONObject jsonObject = onJSONObjectReceivedFolders.jsonObject;
            JSONArray jsonArray = new JSONArray();
            if (jsonObject.has("folders")) {
                try {
                    jsonArray = jsonObject.getJSONArray("folders");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        BladeItem bladeItem = new BladeItem(jsonArray.optJSONObject(i));
                        bladeItemArrayList.add(bladeItem);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bladeItemArrayList.isEmpty()) {
            StaticUtils.showToast(this, getString(R.string.folder_is_empty));
        } else
            addFragment(BladeFolderSelectionFragment.newInstance(bladeItemArrayList), true, R.id.bladeFrame);
    }

    private void requestForFavouriteFolders() {
        addFragment(BladeFavFolderFragment.newInstance(""), false, R.id.bladeFrame);
    }

    public void sendToFolder(String folderPath) {
        Intent data = getIntent();
        data.putExtra("destinationPath", folderPath);
        setResult(RESULT_OK, data);
        finish();
    }

    public void addToFavourites(BladeItem bladeItem) {
        StaticUtils.showToast(getApplicationContext(), "Added to favourites");
        Intent data = getIntent();
        data.putExtra("addToFav", bladeItem.path);
        data.putExtra("addToFavPath", bladeItem.path);
        data.putExtra("addToFavName", bladeItem.name);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgBack:
                onBackPressed();
                break;
            default:
                break;
        }
    }

}
