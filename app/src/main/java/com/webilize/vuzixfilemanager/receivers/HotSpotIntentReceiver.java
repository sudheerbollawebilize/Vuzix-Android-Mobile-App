package com.webilize.vuzixfilemanager.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.webilize.vuzixfilemanager.utils.AppConstants;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

public class HotSpotIntentReceiver extends BroadcastReceiver {

    private final static String TAG = HotSpotIntentReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String ACTION_TURNON = AppConstants.ACTION_HOTSPOT_TURNON;
        final String ACTION_TURNOFF = AppConstants.ACTION_HOTSPOT_TURNOFF;
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_TURNON.equals(action)) {
                StaticUtils.turnOnHotSpot(context);
            } else if (ACTION_TURNOFF.equals(action)) {
                StaticUtils.turnOffHotSpot(context);
            }
        }

    }
}
