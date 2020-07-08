package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemBladeFileBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.BladeItem;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;

import java.util.ArrayList;

public class BladeFileFoldersAdapter extends RecyclerView.Adapter<BladeFileFoldersAdapter.FileViewHolder> {

    private ArrayList<BladeItem> fileFolderItemArrayList;
    private Context context;
    private IClickListener iClickListener;

    public BladeFileFoldersAdapter(Context context, ArrayList<BladeItem> fileFolderItemArrayList, IClickListener iClickListener) {
        this.context = context;
        this.fileFolderItemArrayList = fileFolderItemArrayList;
        this.iClickListener = iClickListener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_blade_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder fileFoldersViewHolder, int position) {
        BladeItem fileFolderItem = fileFolderItemArrayList.get(position);
        int listMode = AppStorage.getInstance(context).getValue(AppStorage.SP_LIST_MODE, AppConstants.SHOW_GRID);
        fileFoldersViewHolder.itemView.setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
        fileFoldersViewHolder.itemView.setOnLongClickListener(v -> {
            if (iClickListener != null) iClickListener.onLongClick(v, position);
            return false;
        });
        fileFoldersViewHolder.itemFileBinding.imgMore.setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
        if (listMode == AppConstants.SHOW_LIST) {
            fileFoldersViewHolder.itemFileBinding.imgFile.setVisibility(View.GONE);
            fileFoldersViewHolder.itemFileBinding.imgFileSmall.setVisibility(View.VISIBLE);
        } else {
            fileFoldersViewHolder.itemFileBinding.imgFile.setVisibility(View.VISIBLE);
            fileFoldersViewHolder.itemFileBinding.imgFileSmall.setVisibility(View.GONE);
        }
        int thumb = fileFolderItem.imageRes;
        fileFoldersViewHolder.itemFileBinding.txtFileName.setText(fileFolderItem.name);
        fileFoldersViewHolder.itemFileBinding.txtFileDetails.setText(fileFolderItem.fileInfo);
        if (fileFolderItem.isSelected) {
            if (listMode == AppConstants.SHOW_LIST) {
                fileFoldersViewHolder.itemFileBinding.imgFileSmall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_select));
            } else {
                fileFoldersViewHolder.itemFileBinding.imgFile.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_select));
            }
            fileFoldersViewHolder.itemFileBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context,
                    R.color.colorRedTint));
        } else {
            fileFoldersViewHolder.itemFileBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context,
                    R.color.colorWhite));
            if (listMode == AppConstants.SHOW_LIST) {
                fileFoldersViewHolder.itemFileBinding.imgFileSmall.setImageDrawable(ContextCompat.getDrawable(context, thumb));
            } else {
                fileFoldersViewHolder.itemFileBinding.imgFile.setImageDrawable(ContextCompat.getDrawable(context, thumb));
            }
        }
    }

    public void setFileFolderItemArrayList(ArrayList<BladeItem> fileFolderItemArrayList) {
        this.fileFolderItemArrayList = fileFolderItemArrayList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return fileFolderItemArrayList != null ? fileFolderItemArrayList.size() : 0;
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        public ItemBladeFileBinding itemFileBinding;

        public FileViewHolder(@NonNull ItemBladeFileBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFileBinding = itemFileFolderBinding;
        }
    }

}
