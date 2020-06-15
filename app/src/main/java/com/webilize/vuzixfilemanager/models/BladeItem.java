package com.webilize.vuzixfilemanager.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.utils.FileUtils;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

import org.json.JSONObject;

public class BladeItem implements Parcelable {

    public String mimeType = "", extension = "", fileInfo = "", name = "", path = "";
    public long size = 0;
    public int imageRes;
    public boolean isSelected = false, isFolder = false, isOriginalFile = false, isFavourite = false;
//    @Transient
//    public JSONObject jsonObject;

    //                    name, path, fileInfo, isFolder, size, isFavourite
    public BladeItem(JSONObject jsonObject) {
//        this.jsonObject = jsonObject;
        name = jsonObject.optString("name");
        path = jsonObject.optString("path");
        size = jsonObject.optLong("size");

        extension = FileUtils.getExtensionByStringHandling(path);
        mimeType = StaticUtils.getMimeTypeFromExtension(extension);
        isFolder = jsonObject.optBoolean("isFolder");
        isFavourite = jsonObject.optBoolean("isFavourite");
        if (isFolder) {
            fileInfo = size + " files ," + jsonObject.optString("fileInfo");
        } else fileInfo = FileUtils.getFileSize(size) + " " + jsonObject.optString("fileInfo");
        if (isSelected) imageRes = R.drawable.ic_select;
        else {
            if (isImageFile())
                imageRes = R.drawable.ic_image;
            else if (isVideoFile())
                imageRes = R.drawable.ic_video;
            else if (isAudioFile())
                imageRes = R.drawable.ic_music;
            else if (isFolder) {
                if (size == 0) {
                    imageRes = R.drawable.ic_folder_empty;
                } else
                    imageRes = R.drawable.ic_folder;
            } else imageRes = R.drawable.ic_file;
        }
    }

    protected BladeItem(Parcel in) {
        mimeType = in.readString();
        extension = in.readString();
        fileInfo = in.readString();
        name = in.readString();
        size = in.readLong();
        path = in.readString();
        isSelected = in.readByte() != 0;
        isFolder = in.readByte() != 0;
        isOriginalFile = in.readByte() != 0;
        isFavourite = in.readByte() != 0;
//        try {
//            jsonObject = new JSONObject(in.readString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        imageRes = in.readInt();
    }

    public boolean isImageFile() {
        return !TextUtils.isEmpty(mimeType) && FileUtils.isImage(mimeType);
    }

    public boolean isVideoFile() {
        return !TextUtils.isEmpty(mimeType) && FileUtils.isVideo(mimeType);
    }

    public boolean isAudioFile() {
        return !TextUtils.isEmpty(mimeType) && FileUtils.isAudio(mimeType);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mimeType);
        dest.writeString(extension);
        dest.writeString(path);
        dest.writeString(fileInfo);
        dest.writeString(name);
        dest.writeLong(size);
        dest.writeByte((byte) (isFolder ? 1 : 0));
        dest.writeByte((byte) (isFavourite ? 1 : 0));
        dest.writeByte((byte) (isOriginalFile ? 1 : 0));
        dest.writeByte((byte) (isSelected ? 1 : 0));     //if myBoolean == true, byte == 1
//        dest.writeString(jsonObject.toString());
        dest.writeInt(imageRes);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public static final Creator<BladeItem> CREATOR = new Creator<BladeItem>() {
        @Override
        public BladeItem createFromParcel(Parcel in) {
            return new BladeItem(in);
        }

        @Override
        public BladeItem[] newArray(int size) {
            return new BladeItem[size];
        }
    };

}
