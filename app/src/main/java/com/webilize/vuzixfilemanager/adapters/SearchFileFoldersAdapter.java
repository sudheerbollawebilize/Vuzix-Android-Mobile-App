package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemFileBinding;
import com.webilize.vuzixfilemanager.databinding.ItemFolderBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.FileFolderItem;

import java.util.ArrayList;

public class SearchFileFoldersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<FileFolderItem> fileFolderItemArrayList;
    private Context context;
    private IClickListener iClickListener;

    public SearchFileFoldersAdapter(Context context, ArrayList<FileFolderItem> fileFolderItemArrayList, IClickListener iClickListener) {
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
        return fileFolderItemArrayList.get(position).file.isFile() ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder fileFoldersViewHolder, int position) {
        FileFolderItem fileFolderItem = fileFolderItemArrayList.get(position);
        if (getItemViewType(position) == 0) {
            ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.setFileFolderItem(fileFolderItem);
            fileFoldersViewHolder.itemView.setOnClickListener(v -> {
                if (iClickListener != null) iClickListener.onClick(v, position);
            });
            fileFoldersViewHolder.itemView.setOnLongClickListener(v -> {
                if (iClickListener != null) iClickListener.onLongClick(v, position);
                return false;
            });
//            ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.imgMore.setOnClickListener(v -> {
//                if (iClickListener != null) iClickListener.onClick(v, position);
//            });
            ((FoldersViewHolder) fileFoldersViewHolder).itemFolderBinding.imgMore.setVisibility(View.GONE);
        } else {
            ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.setFileFolderItem(fileFolderItem);
            fileFoldersViewHolder.itemView.setOnClickListener(v -> {
                if (iClickListener != null) iClickListener.onClick(v, position);
            });
            fileFoldersViewHolder.itemView.setOnLongClickListener(v -> {
                if (iClickListener != null) iClickListener.onLongClick(v, position);
                return false;
            });
//            ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgMore.setOnClickListener(v -> {
//                if (iClickListener != null) iClickListener.onClick(v, position);
//            });
            ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgMore.setVisibility(View.GONE);
            ((FileViewHolder) fileFoldersViewHolder).itemFileBinding.imgFile.setVisibility(View.GONE);
        }
    }

    public void setFileFolderItemArrayList(ArrayList<FileFolderItem> fileFolderItemArrayList) {
        this.fileFolderItemArrayList = fileFolderItemArrayList;
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
