package com.webilize.vuzixfilemanager.utils.audioplayer;

import com.webilize.vuzixfilemanager.models.FileFolderItem;

public interface PlayerAdapter {

    void loadMedia(FileFolderItem fileFolderItem);

    void release();

    boolean isPlaying();

    void play();

    void reset();

    void pause();

    void initializeProgressCallback();

    void seekTo(int position);

    String getDuration();
}