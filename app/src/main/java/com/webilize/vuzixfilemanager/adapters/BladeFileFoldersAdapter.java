package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemBladeFileBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.BladeItem;

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
        int thumb = fileFolderItem.imageRes;
        fileFoldersViewHolder.itemFileBinding.txtFileName.setText(fileFolderItem.name);
        fileFoldersViewHolder.itemFileBinding.txtFileDetails.setText(fileFolderItem.fileInfo);
        if (fileFolderItem.isSelected) {
            fileFoldersViewHolder.itemFileBinding.imgFileSmall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_select));
            fileFoldersViewHolder.itemFileBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context,
                    R.color.colorRedTint));
        } else {
            fileFoldersViewHolder.itemFileBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context,
                    R.color.colorWhite));
            fileFoldersViewHolder.itemFileBinding.imgFileSmall.setImageDrawable(ContextCompat.getDrawable(context, thumb));
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
