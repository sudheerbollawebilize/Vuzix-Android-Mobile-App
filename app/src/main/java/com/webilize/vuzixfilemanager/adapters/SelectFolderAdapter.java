package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemSelectFolderBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;

import java.io.File;
import java.util.ArrayList;

public class SelectFolderAdapter extends RecyclerView.Adapter<SelectFolderAdapter.DevicesViewHolder> {

    private ArrayList<File> folderNamesArrayList;
    private Context context;
    private IClickListener iClickListener;

    public SelectFolderAdapter(Context context, ArrayList<File> folderNamesArrayList, IClickListener iClickListener) {
        this.context = context;
        this.folderNamesArrayList = folderNamesArrayList;
        this.iClickListener = iClickListener;
    }

    @NonNull
    @Override
    public DevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DevicesViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_select_folder, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesViewHolder fileFoldersViewHolder, int position) {
        fileFoldersViewHolder.itemFolderBinding.txtFileName.setText(folderNamesArrayList.get(position).getName());
        fileFoldersViewHolder.itemFolderBinding.getRoot().setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
        fileFoldersViewHolder.itemFolderBinding.getRoot().setOnLongClickListener(v -> {
            if (iClickListener != null) iClickListener.onLongClick(v, position);
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return folderNamesArrayList.size();
    }

    static class DevicesViewHolder extends RecyclerView.ViewHolder {
        ItemSelectFolderBinding itemFolderBinding;

        DevicesViewHolder(@NonNull ItemSelectFolderBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFolderBinding = itemFileFolderBinding;
        }
    }

}
