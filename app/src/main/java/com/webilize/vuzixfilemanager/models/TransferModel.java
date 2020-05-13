package com.webilize.vuzixfilemanager.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class TransferModel implements Parcelable {

    public String name = "", rawData = "", folderLocation = "";
    public int progress;
    //    Status - 0>ongoing, 1>completed, 2>cancelled
    public int status;
    //    0 true, 1 false
    public boolean isIncoming;
    public long id = -1;
    public long size;

    public TransferModel() {
    }

    protected TransferModel(Parcel in) {
        name = in.readString();
        folderLocation = in.readString();
        rawData = in.readString();
        status = in.readInt();
        progress = in.readInt();
        isIncoming = in.readInt() == 0;
        id = in.readLong();
        size = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(folderLocation);
        dest.writeString(rawData);
        dest.writeInt(progress);
        dest.writeInt(isIncoming ? 0 : 1);
        dest.writeLong(id);
        dest.writeLong(size);
        dest.writeInt(status);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public static final Creator<TransferModel> CREATOR = new Creator<TransferModel>() {
        @Override
        public TransferModel createFromParcel(Parcel in) {
            return new TransferModel(in);
        }

        @Override
        public TransferModel[] newArray(int size) {
            return new TransferModel[size];
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
//        return super.equals(obj);
        TransferModel transferModel = ((TransferModel) obj);
        assert transferModel != null;
        return (transferModel.id != -1 && transferModel.id == this.id) && (transferModel.isIncoming == this.isIncoming);
    }
}
