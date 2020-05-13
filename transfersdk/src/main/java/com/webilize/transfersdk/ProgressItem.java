package com.webilize.transfersdk;

public class ProgressItem {

    public String fileName = "";
    public long progress;

    public ProgressItem(String fileName, long progress) {
        this.fileName = fileName;
        this.progress = progress;
    }

}
