package com.webilize.vuzixfilemanager.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.databinding.ItemTransfersBinding;
import com.webilize.vuzixfilemanager.interfaces.IClickListener;
import com.webilize.vuzixfilemanager.models.TransferModel;
import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.FileUtils;

import java.util.ArrayList;

public class TransfersAdapter extends RecyclerView.Adapter<TransfersAdapter.DevicesViewHolder> {

    private ArrayList<TransferModel> transferModelArrayList;
    private Context context;
    private IClickListener iClickListener;

    public TransfersAdapter(Context context, ArrayList<TransferModel> transferModelArrayList, IClickListener iClickListener) {
        this.context = context;
        this.transferModelArrayList = transferModelArrayList;
        this.iClickListener = iClickListener;
    }

    @NonNull
    @Override
    public DevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DevicesViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_transfers, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesViewHolder fileFoldersViewHolder, int position) {
        TransferModel transferModel = transferModelArrayList.get(position);
        fileFoldersViewHolder.itemFolderBinding.txtFileName.setText(transferModel.name);
        fileFoldersViewHolder.itemFolderBinding.txtIsIncoming.setText(transferModel.isIncoming ? "Incoming" : "Outgoing");
        if (transferModel.isIncoming) {
            fileFoldersViewHolder.itemFolderBinding.imgFileSmall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_incoming));
            fileFoldersViewHolder.itemFolderBinding.txtIsIncoming.setText(R.string.incoming);
        } else {
            fileFoldersViewHolder.itemFolderBinding.txtIsIncoming.setText(R.string.outgoing);
            fileFoldersViewHolder.itemFolderBinding.imgFileSmall.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_outgoing));
        }
        fileFoldersViewHolder.itemFolderBinding.imgStatus.setImageDrawable(getStatus(transferModel.status));
        if (transferModel.status == AppConstants.CONST_TRANSFER_ONGOING) {
            fileFoldersViewHolder.itemFolderBinding.txtFileSize.setText(FileUtils.getCompletedAndFullSize(transferModel.size, transferModel.progress));
            fileFoldersViewHolder.itemFolderBinding.progressBar.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileFoldersViewHolder.itemFolderBinding.progressBar.setProgress(transferModel.progress, true);
            } else
                fileFoldersViewHolder.itemFolderBinding.progressBar.setProgress(transferModel.progress);
        } else {
            fileFoldersViewHolder.itemFolderBinding.txtFileSize.setText(FileUtils.getFileSize(transferModel.size));
            fileFoldersViewHolder.itemFolderBinding.progressBar.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileFoldersViewHolder.itemFolderBinding.progressBar.setProgress(transferModel.status == AppConstants.CONST_TRANSFER_CANCELLED ? 0 : 100, true);
            } else
                fileFoldersViewHolder.itemFolderBinding.progressBar.setProgress(transferModel.status == AppConstants.CONST_TRANSFER_CANCELLED ? 0 : 100);
        }

        fileFoldersViewHolder.itemFolderBinding.imgMore.setOnClickListener(v -> {
            if (iClickListener != null) iClickListener.onClick(v, position);
        });
        fileFoldersViewHolder.itemFolderBinding.imgMore.setOnLongClickListener(v -> {
            if (iClickListener != null) iClickListener.onLongClick(v, position);
            return false;
        });
    }

    private Drawable getStatus(int status) {
        switch (status) {
            case AppConstants.CONST_TRANSFER_ONGOING:
                return ContextCompat.getDrawable(context, R.drawable.ic_downloading);
            case AppConstants.CONST_TRANSFER_COMPLETED:
                return ContextCompat.getDrawable(context, R.drawable.ic_complete);
            case AppConstants.CONST_TRANSFER_CANCELLED:
                return ContextCompat.getDrawable(context, R.drawable.ic_clear_blue);
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return transferModelArrayList.size();
    }

    static class DevicesViewHolder extends RecyclerView.ViewHolder {
        ItemTransfersBinding itemFolderBinding;

        DevicesViewHolder(@NonNull ItemTransfersBinding itemFileFolderBinding) {
            super(itemFileFolderBinding.getRoot());
            this.itemFolderBinding = itemFileFolderBinding;
        }
    }

}
