package com.webilize.vuzixfilemanager.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.webilize.vuzixfilemanager.BaseApplication;
import com.webilize.vuzixfilemanager.BuildConfig;
import com.webilize.vuzixfilemanager.utils.usb.OTGEvent;

import org.greenrobot.eventbus.EventBus;

public class BootUpReceiver extends BroadcastReceiver {

    private static final String ACTION_USB_PERMISSION = BuildConfig.APPLICATION_ID + ".USB_PERMISSION";
    String TAG = "OTG   ";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        OTGEvent otgEvent = new OTGEvent();
        if (action.equalsIgnoreCase(ACTION_USB_PERMISSION)) {
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        BaseApplication.isOtgAvailable = true;
                        otgEvent.usbDevice = device;
                    }
                } else {
                    BaseApplication.isOtgAvailable = false;
                    otgEvent.usbDevice = null;
                }
            }
        } else if (action.equalsIgnoreCase("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            BaseApplication.isOtgAvailable = device != null;
            otgEvent.usbDevice = device;
        } else if (action.equalsIgnoreCase("android.hardware.usb.action.USB_DEVICE_DETACHED")) {
            BaseApplication.isOtgAvailable = false;
            otgEvent.usbDevice = null;
        }
        otgEvent.isOtgAvailable = BaseApplication.isOtgAvailable;
        EventBus.getDefault().post(otgEvent);
    }

}