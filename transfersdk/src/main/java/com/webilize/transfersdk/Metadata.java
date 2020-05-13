package com.webilize.transfersdk;

/**
 * File Metadata
 */
public class Metadata {

    char type;
    long size;
    private String fileName;
    private String mimeType;

    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setMimeType(String mimetype) {
        this.mimeType = mimetype;
    }

}

