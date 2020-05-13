package com.webilize.vuzixfilemanager.utils.eventbus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OnThumbsReceived {

    public List<File> filesArrayList = new ArrayList<>();

    public OnThumbsReceived(List<File> filesArrayList) {
        this.filesArrayList = filesArrayList;
    }

}
