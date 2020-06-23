package com.webilize.vuzixfilemanager.interfaces;

import com.webilize.vuzixfilemanager.models.FileFolderItem;

public interface NavigationListener {

    void open(FileFolderItem file);

    void back();

}
