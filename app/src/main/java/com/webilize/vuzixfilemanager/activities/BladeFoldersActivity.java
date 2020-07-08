package com.webilize.vuzixfilemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ActivityBladeFoldersBinding;
import com.webilize.vuzixfilemanager.fragments.BladeFavFolderFragment;
import com.webilize.vuzixfilemanager.fragments.BladeFolderSelectionFragment;
import com.webilize.vuzixfilemanager.models.BladeItem;
import com.webilize.vuzixfilemanager.services.RXConnectionFGService;
import com.webilize.vuzixfilemanager.utils.AppConstants;
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
        serviceIntent.putExtra(AppConstants.INTENT_INPUT_EXTRA, "folder");
        serviceIntent.putExtra(AppConstants.INTENT_FOLDER_PATH, folderPath);
        serviceIntent.putExtra(AppConstants.INTENT_IS_ONLY_FOLDERS, true);
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
            if (jsonObject.has("folders")) {
                try {
                    JSONArray jsonArray = jsonObject.getJSONArray("folders");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        bladeItemArrayList.add(new BladeItem(jsonArray.optJSONObject(i)));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
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
        data.putExtra(AppConstants.INTENT_DESTINATION_PATH, folderPath);
        setResult(RESULT_OK, data);
        finish();
    }

    public void addToFavourites(BladeItem bladeItem) {
        StaticUtils.showToast(getApplicationContext(), getString(R.string.added_to_favourites));
        Intent data = getIntent();
        data.putExtra(AppConstants.INTENT_ADD_TO_FAV, bladeItem.path);
        data.putExtra(AppConstants.INTENT_ADD_TO_FAV_PATH, bladeItem.path);
        data.putExtra(AppConstants.INTENT_ADD_TO_FAV_NAME, bladeItem.name);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imgBack) onBackPressed();
    }

}
