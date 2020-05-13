package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemFileBinding;
import com.webilize.vuzixfilemanager.databinding.ItemFolderBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.FileFolderItem;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.AppStorage;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

import java.util.ArrayList;

public class FileFoldersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return viewType == 0 ?
                new FoldersViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_folder, parent, false)) :
                new FileViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_file, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
//        return fileFolderItemArrayList.get(position).file.isFile() ? 1 : 0;
        if (fileFolderItemArrayList.get(position).file == null)
            return fileFolderItemArrayList.get(position).usbFile.isDirectory() ? 0 : 1;
        else return fileFolderItemArrayList.get(position).file.isFile() ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder fileFoldersViewHolder, int position) {
        FileFolderItem fileFolderItem = fileFolderItemArrayList.get(position);
        listMode = AppStorage.getInstance(context).getValue(AppStorage.SP_LIST_MODE, AppConstants.SHOW_GRID);
        if (getItemViewType(position) == 0) {
            ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.setFileFolderItem(fileFolderItem);
            fileFoldersViewHolder.itemView.setOnClickListener(v -> {
                if (iClickListener != null) iClickListener.onClick(v, position);
            });
            fileFoldersViewHolder.itemView.setOnLongClickListener(v -> {
                if (iClickListener != null) iClickListener.onLongClick(v, position);
                return false;
            });
            ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.imgMore.setOnClickListener(v -> {
                if (iClickListener != null) iClickListener.onClick(v, position);
            });
            if (fileFolderItem.isSelected) {
                ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context,
                        R.color.colorRedTint));
                ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.txtFolderName.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(context, R.drawable.ic_select), null, null, null);
            } else {
                ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context,
                        R.color.colorWhite));
                ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.txtFolderName.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(context, R.drawable.ic_folder), null, null, null);
            }
        } else {
            ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.setFileFolderItem(fileFolderItem);
            fileFoldersViewHolder.itemView.setOnClickListener(v -> {
                if (iClickListener != null) iClickListener.onClick(v, position);
            });
            fileFoldersViewHolder.itemView.setOnLongClickListener(v -> {
                if (iClickListener != null) iClickListener.onLongClick(v, position);
                return false;
            });
            ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgMore.setOnClickListener(v -> {
                if (iClickListener != null) iClickListener.onClick(v, position);
            });
            if (listMode == AppConstants.SHOW_LIST) {
                ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgFile.setVisibility(View.GONE);
            } else {
                int thumb;
                if (fileFolderItem.file == null)
                    thumb = StaticUtils.getFileDrawable(fileFolderItem.usbFile);
                else thumb = StaticUtils.getFileDrawable(fileFolderItem.file);
                int imgHeight = (int) StaticUtils.convertDpToPixel(150, context);
                ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgFile.setVisibility(View.VISIBLE);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imgHeight);
                ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgFile.setLayoutParams(layoutParams);
                if (!TextUtils.isEmpty(fileFolderItem.mimeType) && (fileFolderItem.mimeType.contains("image") ||
                        fileFolderItem.mimeType.contains("gif") || fileFolderItem.mimeType.contains("video"))) {
                    ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgFile.setScaleType(ImageView.ScaleType.FIT_CENTER);
                } else {
                    ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgFile.setImageResource(thumb);
                    ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgFile.setScaleType(ImageView.ScaleType.CENTER);
                }
            }
            if (fileFolderItem.isSelected) {
                ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context,
                        R.color.colorRedTint));
            } else {
                ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.cardBody.setCardBackgroundColor(ContextCompat.getColor(context,
                        R.color.colorWhite));
            }
        }
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

    class FoldersViewHolder extends RecyclerView.ViewHolder {
        public ItemFolderBinding itemFolderBinding;

        public FoldersViewHolder(@NonNull ItemFolderBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFolderBinding = itemFileFolderBinding;
        }
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        public ItemFileBinding itemFileBinding;

        public FileViewHolder(@NonNull ItemFileBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFileBinding = itemFileFolderBinding;
        }
    }

}
