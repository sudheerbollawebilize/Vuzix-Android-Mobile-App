package com.webilize.vuzixfilemanager.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.github.mjdev.libaums.fs.UsbFile;
import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.utils.DateUtils;
import com.webilize.vuzixfilemanager.utils.FileUtils;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

import java.io.File;

public class FileFolderItem implements Parcelable {

    public UsbFile usbFile;
    public File file;
    public String mimeType = "", extension = "", fileInfo = "", name = "", size = "", timeStamp = "";
    public int imageRes = R.drawable.ic_folder;
    public boolean isSelected = false;

    public FileFolderItem(File file) {
//        new FileFolderItem(Parcel.obtain());
        this.file = file;
        if (file.isFile()) {
            extension = FileUtils.getExtensionByStringHandling(file.getAbsolutePath());
            mimeType = StaticUtils.getMimeTypeFromExtension(extension);
            size = FileUtils.getFileSize(file);
            timeStamp = DateUtils.getDateTimeFromTimeStamp(file.lastModified(), DateUtils.DATE_FORMAT_0);
//            fileInfo = size + "\n" + timeStamp;
            imageRes = StaticUtils.getFileDrawable(file);
            name = FileUtils.getFileNameWthoutExtension(file);
        } else {
            name = file.getName();
            size = (file.list() != null ? (file.list().length + " files") : "0 files");
            timeStamp = DateUtils.getDateTimeFromTimeStamp(file.lastModified(), DateUtils.DATE_FORMAT_0);
            if (size.equalsIgnoreCase("0 files")) {
                imageRes = R.drawable.ic_folder_empty;
            } else imageRes = R.drawable.ic_folder;
//            fileInfo = size + "\n" + timeStamp;
        }
        fileInfo = size + "\n" + timeStamp;
    }

    public FileFolderItem(UsbFile file) {
        this.usbFile = file;
        if (!file.isDirectory()) {
            extension = FileUtils.getExtensionByStringHandling(file.getAbsolutePath());
            mimeType = StaticUtils.getMimeTypeFromExtension(extension);
            fileInfo = FileUtils.getFileSize(file) + DateUtils.getDateTimeFromTimeStamp(file.lastModified(), DateUtils.DATE_FORMAT_0);
            imageRes = StaticUtils.getFileDrawable(file);
            name = FileUtils.getFileNameWthoutExtension(file);
        } else name = file.getName();
    }

    protected FileFolderItem(Parcel in) {
        mimeType = in.readString();
        extension = in.readString();
        fileInfo = in.readString();
        name = in.readString();
        isSelected = in.readByte() != 0;
        file = (File) in.readSerializable();
        usbFile = (UsbFile) in.readSerializable();
        imageRes = in.readInt();
    }

    @BindingAdapter({"fileInfo"})
    public static void loadAppropriateImage(ImageView imageView, FileFolderItem file) {
        if (file.file != null)
            FileUtils.loadListThumbnailWithGlide(file.file, imageView, file.imageRes);
        else FileUtils.loadListThumbnailWithGlide(file.usbFile, imageView, file.imageRes);
    }

    @BindingAdapter("imageFile")
    public static void getSmallThumb(ImageView imageView, FileFolderItem file) {
        int imageRes = file.imageRes;
        if (file.isSelected)
            imageView.setImageResource(R.drawable.ic_select);
        else imageView.setImageResource(imageRes);
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
        dest.writeString(fileInfo);
        dest.writeString(name);
        dest.writeByte((byte) (isSelected ? 1 : 0));     //if myBoolean == true, byte == 1
        dest.writeSerializable(file);
        dest.writeInt(imageRes);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public static final Creator<FileFolderItem> CREATOR = new Creator<FileFolderItem>() {
        @Override
        public FileFolderItem createFromParcel(Parcel in) {
            return new FileFolderItem(in);
        }

        @Override
        public FileFolderItem[] newArray(int size) {
            return new FileFolderItem[size];
        }
    };
}
