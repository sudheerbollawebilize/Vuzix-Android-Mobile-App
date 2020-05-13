package com.webilize.vuzixfilemanager.utils.transferutils;

import java.util.List;

public class Response {
    public enum Type {THUMBNAILS, ORIGINALS, FOLDERS}

    private int pageSize;
    private boolean firstLoad;
    private int width;
    private int height;
    public String folderPath;
    private Type type;
    private List<String> fileNames;

    public Response(Type type) {
        this.type = type;
        firstLoad = false;
        pageSize = 30;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isFirstLoad() {
        return firstLoad;
    }

    public void setFirstLoad(boolean firstLoad) {
        this.firstLoad = firstLoad;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }
}
