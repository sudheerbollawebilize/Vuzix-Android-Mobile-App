package com.webilize.vuzixfilemanager.utils;

import com.webilize.vuzixfilemanager.models.BladeItem;

import java.util.Comparator;

public class BladeSizeComparator implements Comparator<BladeItem> {
    private int mode;

    public BladeSizeComparator(int mode) {
        this.mode = mode;
    }

    @Override
    public int compare(BladeItem o1, BladeItem o2) {
        if (mode == AppConstants.CONST_SORT_ASC) {
            return Long.compare(o1.size, o2.size);
        } else {
            return Long.compare(o2.size, o1.size);
        }
    }

    @Override
    public Comparator<BladeItem> reversed() {
        return null;
    }

}
