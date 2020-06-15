package com.webilize.vuzixfilemanager.models;

public class Memory {

    private String usedSpace;
    private String freeSpace;
    private String totalSpace;

    public Memory(String usedSpace, String freeSpace, String totalSpace) {
        this.usedSpace = usedSpace;
        this.freeSpace = freeSpace;
        this.totalSpace = totalSpace;
    }

    public String getUsedSpace() {
        return usedSpace;
    }

    public String getFreeSpace() {
        return freeSpace;
    }

    public String getTotalSpace() {
        return totalSpace;
    }

}