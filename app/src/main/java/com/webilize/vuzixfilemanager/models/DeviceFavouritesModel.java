package com.webilize.vuzixfilemanager.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class DeviceFavouritesModel implements Parcelable {

    public String name = "", path = "";
    public boolean isDefault;
    public long id = -1, deviceId;

    public DeviceFavouritesModel() {
    }

    protected DeviceFavouritesModel(Parcel in) {
        name = in.readString();
        path = in.readString();
        id = in.readLong();
        deviceId = in.readLong();
        isDefault = in.readInt() == 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(path);
        dest.writeLong(id);
        dest.writeLong(deviceId);
        dest.writeInt(isDefault ? 0 : 1);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public static final Creator<DeviceFavouritesModel> CREATOR = new Creator<DeviceFavouritesModel>() {
        @Override
        public DeviceFavouritesModel createFromParcel(Parcel in) {
            return new DeviceFavouritesModel(in);
        }

        @Override
        public DeviceFavouritesModel[] newArray(int size) {
            return new DeviceFavouritesModel[size];
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        DeviceFavouritesModel transferModel = ((DeviceFavouritesModel) obj);
        assert transferModel != null;
        return (transferModel.path.equalsIgnoreCase(this.path));
    }
}
