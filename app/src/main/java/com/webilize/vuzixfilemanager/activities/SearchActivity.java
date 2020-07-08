package com.webilize.vuzixfilemanager.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.adapters.SearchFileFoldersAdapter;
import com.webilize.vuzixfilemanager.databinding.ActivitySearchBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.AppConstants;

import java.io.File;
import java.util.ArrayList;

public class SearchActivity extends BaseActivity implements View.OnClickListener, IClickListener {

    private ActivitySearchBinding activitySearchBinding;
    private ArrayList<FileFolderItem> fileFolderItemArrayList;
    SearchFileFoldersAdapter searchFileFoldersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySearchBinding = DataBindingUtil.setContentView(this, R.layout.activity_search);
    }

    @Override
    void initComponents() {
        fileFolderItemArrayList = new ArrayList<>();
        activitySearchBinding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                fileFolderItemArrayList = new ArrayList<>();
                AsyncTask.execute(() -> {
                    if (TextUtils.isEmpty(editable.toString())) {
                        search();
                    } else search(editable.toString(), AppConstants.HOME_DIRECTORY);
                });
            }
        });
        activitySearchBinding.imgBack.setOnClickListener(this);
        search();
        setAdapter();
        activitySearchBinding.edtSearch.requestFocus();
    }

    private void setAdapter() {
        activitySearchBinding.recyclerViewSearch.setLayoutManager(new LinearLayoutManager(this));
        searchFileFoldersAdapter = new SearchFileFoldersAdapter(fileFolderItemArrayList, this);
        activitySearchBinding.recyclerViewSearch.setAdapter(searchFileFoldersAdapter);
    }

    public void search() {
        File directory = AppConstants.HOME_DIRECTORY;
        if (directory.isDirectory()) {
            if (directory.canRead()) {
                for (File temp : directory.listFiles()) {
                    fileFolderItemArrayList.add(new FileFolderItem(temp));
                }
            }
        } else {
            fileFolderItemArrayList.add(new FileFolderItem(directory));
        }
    }

    public void search(String searchQuery, File directory) {
        if (directory.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
            fileFolderItemArrayList.add(new FileFolderItem(directory));
        }
        if (directory.isDirectory()) {
            if (directory.canRead()) {
                for (File temp : directory.listFiles()) {
                    if (temp.isDirectory()) {
                        search(searchQuery, temp);
                    } else {
                        if (temp.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                            fileFolderItemArrayList.add(new FileFolderItem(temp));
                        }
                    }
                }
            }
        }
        updateAdapterWithSearchData();
    }

    private void updateAdapterWithSearchData() {
        runOnUiThread(() -> {
            activitySearchBinding.recyclerViewSearch.setLayoutManager(new LinearLayoutManager(SearchActivity.this));
            if (searchFileFoldersAdapter != null)
                searchFileFoldersAdapter.setFileFolderItemArrayList(fileFolderItemArrayList);
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imgBack) onBackPressed();
    }

    @Override
    public void onClick(View view, int position) {
        Intent intent = getIntent();
        intent.putExtra(AppConstants.INTENT_SELECTED_FILE, fileFolderItemArrayList.get(position));
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onLongClick(View view, int position) {

    }

}
