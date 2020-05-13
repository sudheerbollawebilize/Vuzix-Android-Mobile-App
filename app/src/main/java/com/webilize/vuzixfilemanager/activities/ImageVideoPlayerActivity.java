package com.webilize.vuzixfilemanager.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ActivityImageVideoPlayerBinding;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

public class ImageVideoPlayerActivity extends BaseActivity implements View.OnClickListener {

    private ActivityImageVideoPlayerBinding activityImageVideoPlayerBinding;
    private PopupMenu popupMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityImageVideoPlayerBinding = DataBindingUtil.setContentView(this, R.layout.activity_image_video_player);
    }

    @Override
    void initComponents() {
        preparePopUpMenu();
        activityImageVideoPlayerBinding.imgBack.setOnClickListener(this);
        activityImageVideoPlayerBinding.imgMenu.setOnClickListener(this);
    }

    private void preparePopUpMenu() {
        popupMenu = new PopupMenu(ImageVideoPlayerActivity.this, activityImageVideoPlayerBinding.imgMenu);
        popupMenu.getMenuInflater().inflate(R.menu.menu_image_video, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            StaticUtils.showToast(ImageVideoPlayerActivity.this, item.getTitle().toString());
            return true;
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgMenu:
                popupMenu.show();
                break;
            case R.id.imgBack:
                onBackPressed();
                break;
            default:
                break;
        }
    }

}
