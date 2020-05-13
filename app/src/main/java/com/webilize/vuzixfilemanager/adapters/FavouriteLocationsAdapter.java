package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemFavLocationsBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;

import java.util.List;

public class FavouriteLocationsAdapter extends RecyclerView.Adapter<FavouriteLocationsAdapter.DevicesViewHolder> {

    private List<String> folderNamesArrayList;
    private Context context;
    private IClickListener iClickListener;
    private boolean showRemove;

    public FavouriteLocationsAdapter(Context context, List<String> folderNamesArrayList, IClickListener iClickListener, boolean showRemove) {
        this.context = context;
        this.showRemove = showRemove;
        this.folderNamesArrayList = folderNamesArrayList;
        this.iClickListener = iClickListener;
    }

    @NonNull
    @Override
    public DevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DevicesViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_fav_locations, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesViewHolder fileFoldersViewHolder, int position) {
        if (showRemove)
            fileFoldersViewHolder.itemFolderBinding.txtFileName.setOnTouchListener((v, event) -> {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (fileFoldersViewHolder.itemFolderBinding.txtFileName.getRight() - fileFoldersViewHolder.itemFolderBinding.txtFileName.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (iClickListener != null) iClickListener.onClick(v, position);
                        return true;
                    }
                }
                return false;
            });
        else
            fileFoldersViewHolder.itemFolderBinding.txtFileName.setOnClickListener(v -> {
                if (iClickListener != null) iClickListener.onClick(v, position);
            });
        fileFoldersViewHolder.itemFolderBinding.txtFileName.setText(folderNamesArrayList.get(position));
    }

    @Override
    public int getItemCount() {
        return folderNamesArrayList.size();
    }

    static class DevicesViewHolder extends RecyclerView.ViewHolder {
        ItemFavLocationsBinding itemFolderBinding;

        DevicesViewHolder(@NonNull ItemFavLocationsBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFolderBinding = itemFileFolderBinding;
        }
    }

}
