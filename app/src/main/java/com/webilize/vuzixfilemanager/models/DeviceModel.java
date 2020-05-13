package com.webilize.vuzixfilemanager.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class DeviceModel implements Parcelable {

    public String name = "", deviceAddress = "";
    public long id = -1;

    public DeviceModel() {
    }

    protected DeviceModel(Parcel in) {
        name = in.readString();
        deviceAddress = in.readString();
        id = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(deviceAddress);
        dest.writeLong(id);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public static final Creator<DeviceModel> CREATOR = new Creator<DeviceModel>() {
        @Override
        public DeviceModel createFromParcel(Parcel in) {
            return new DeviceModel(in);
        }

        @Override
        public DeviceModel[] newArray(int size) {
            return new DeviceModel[size];
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        DeviceModel transferModel = ((DeviceModel) obj);
        assert transferModel != null;
        return (transferModel.id != -1 && transferModel.id == this.id) && (transferModel.deviceAddress.equalsIgnoreCase(this.deviceAddress));
    }
}
