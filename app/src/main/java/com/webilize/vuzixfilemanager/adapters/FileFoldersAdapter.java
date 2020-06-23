package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemFileBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

import java.util.ArrayList;

public class FileFoldersAdapter extends RecyclerView.Adapter<FileFoldersAdapter.FileViewHolder> {

    private ArrayList<FileFolderItem> fileFolderItemArrayList;
    private Context context;
    private IClickListener iClickListener;
    private int listMode;

    public FileFoldersAdapter(Context context, ArrayList<FileFolderItem> fileFolderItemArrayList, IClickListener iClickListener) {
        this.context = context;
        this.fileFolderItemArrayList = fileFolderItemArrayList;
        this.iClickListener = iClickListener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileFolderItem fileFolderItem = fileFolderItemArrayList.get(position);
        listMode = AppStorage.getInstance(context).getValue(AppStorage.SP_LIST_MODE, AppConstants.SHOW_GRID);
        holder.itemFileBinding.setFileFolderItem(fileFolderItem);
        holder.itemView.setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (iClickListener != null) iClickListener.onLongClick(v, position);
            return false;
        });
        holder.itemFileBinding.imgMore.setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
        if (listMode == AppConstants.SHOW_LIST) {
            holder.itemFileBinding.imgFile.setVisibility(View.GONE);
            holder.itemFileBinding.imgFileSmall.setVisibility(View.VISIBLE);
            holder.itemFileBinding.txtFileDetails.setText(fileFolderItem.size + " " + fileFolderItem.timeStamp);
        } else {
            holder.itemFileBinding.txtFileDetails.setText(fileFolderItem.size + "\n" + fileFolderItem.timeStamp);
            holder.itemFileBinding.imgFile.setVisibility(View.VISIBLE);
            holder.itemFileBinding.imgFileSmall.setVisibility(View.GONE);
            int thumb;
            if (fileFolderItem.file == null)
                thumb = StaticUtils.getFileDrawable(fileFolderItem.usbFile);
            else thumb = StaticUtils.getFileDrawable(fileFolderItem.file);
            holder.itemFileBinding.imgFile.setImageResource(thumb);
            holder.itemFileBinding.imgFile.setScaleType(ImageView.ScaleType.CENTER);
        }
        if (fileFolderItem.isSelected) {
            holder.itemFileBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorRedTint));
        } else {
            holder.itemFileBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorWhite));
/*
            if (listMode == AppConstants.SHOW_LIST) {
                holder.itemFolderBinding.txtFolderName.setGravity(Gravity.CENTER_VERTICAL);
                holder.itemFolderBinding.txtFolderName.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(context, R.drawable.ic_folder), null, null, null);
            } else {
                holder.itemFolderBinding.txtFolderName.setGravity(Gravity.CENTER);
                holder.itemFolderBinding.txtFolderName.setCompoundDrawablesWithIntrinsicBounds(
                        null, ContextCompat.getDrawable(context, R.drawable.ic_folder), null, null);
            }
*/
        }
    }

    @Override
    public int getItemViewType(int position) {
//        return fileFolderItemArrayList.get(position).file.isFile() ? 1 : 0;
        if (fileFolderItemArrayList.get(position).file == null)
            return fileFolderItemArrayList.get(position).usbFile.isDirectory() ? 0 : 1;
        else return fileFolderItemArrayList.get(position).file.isFile() ? 1 : 0;
    }

    public void setFileFolderItemArrayList(ArrayList<FileFolderItem> fileFolderItemArrayList) {
        this.fileFolderItemArrayList.clear();
        this.fileFolderItemArrayList.addAll(fileFolderItemArrayList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return fileFolderItemArrayList != null ? fileFolderItemArrayList.size() : 0;
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        public ItemFileBinding itemFileBinding;

        public FileViewHolder(@NonNull ItemFileBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFileBinding = itemFileFolderBinding;
        }
    }

}
