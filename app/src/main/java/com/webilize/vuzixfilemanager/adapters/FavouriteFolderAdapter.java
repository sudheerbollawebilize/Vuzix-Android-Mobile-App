package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemFavFolderBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.DeviceFavouritesModel;

import java.util.ArrayList;

public class FavouriteFolderAdapter extends RecyclerView.Adapter<FavouriteFolderAdapter.DevicesViewHolder> {

    private ArrayList<DeviceFavouritesModel> folderNamesArrayList;
    private Context context;
    private IClickListener iClickListener;

    public FavouriteFolderAdapter(Context context, ArrayList<DeviceFavouritesModel> folderNamesArrayList, IClickListener iClickListener) {
        this.context = context;
        this.folderNamesArrayList = folderNamesArrayList;
        this.iClickListener = iClickListener;
    }

    @NonNull
    @Override
    public DevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DevicesViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_fav_folder, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesViewHolder fileFoldersViewHolder, int position) {
        fileFoldersViewHolder.itemFolderBinding.txtFileName.setText(folderNamesArrayList.get(position).name);
        fileFoldersViewHolder.itemFolderBinding.getRoot().setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
        fileFoldersViewHolder.itemFolderBinding.getRoot().setOnLongClickListener(v -> {
            if (iClickListener != null) iClickListener.onLongClick(v, position);
            return false;
        });
        fileFoldersViewHolder.itemFolderBinding.imgRemove.setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
    }

    @Override
    public int getItemCount() {
        return folderNamesArrayList.size();
    }

    static class DevicesViewHolder extends RecyclerView.ViewHolder {
        ItemFavFolderBinding itemFolderBinding;

        DevicesViewHolder(@NonNull ItemFavFolderBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFolderBinding = itemFileFolderBinding;
        }
    }

}
