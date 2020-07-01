package com.webilize.vuzixfilemanager.utils;

import com.webilize.vuzixfilemanager.models.BladeItem;

import java.util.Comparator;

public class BladeLastModifiedComparator implements Comparator<BladeItem> {
    private int mode;

    public BladeLastModifiedComparator(int mode) {
        this.mode = mode;
    }

    @Override
    public int compare(BladeItem o1, BladeItem o2) {
        if (mode == AppConstants.CONST_SORT_ASC) {
            return Long.compare(o1.lastModified, o2.lastModified);
        } else {
            return Long.compare(o2.lastModified, o1.lastModified);
        }
    }

    @Override
    public Comparator<BladeItem> reversed() {
        return null;
    }

}
