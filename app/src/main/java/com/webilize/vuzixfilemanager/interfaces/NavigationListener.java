package com.webilize.vuzixfilemanager.interfaces;

import com.webilize.vuzixfilemanager.models.FileFolderItem;

public interface NavigationListener {

    void open(FileFolderItem file);

    void back();

    void openDetails(FileFolderItem currentFile);

//    void updateTitle(String folderName);

    void openQRScanner();

    void openWifiDirect();

    void openSendScreen();

}
