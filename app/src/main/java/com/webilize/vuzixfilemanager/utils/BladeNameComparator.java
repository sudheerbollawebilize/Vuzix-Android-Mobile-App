package com.webilize.vuzixfilemanager.utils;

import com.webilize.vuzixfilemanager.models.BladeItem;

import java.util.Comparator;

public class BladeNameComparator implements Comparator<BladeItem> {
    private int mode;

    public BladeNameComparator(int mode) {
        this.mode = mode;
    }

    @Override
    public int compare(BladeItem o1, BladeItem o2) {
        if (mode == AppConstants.CONST_SORT_ASC)
            return o1.name.compareTo(o2.name);
        else
            return o2.name.compareTo(o1.name);
    }

    @Override
    public Comparator<BladeItem> reversed() {
        return null;
    }

}
