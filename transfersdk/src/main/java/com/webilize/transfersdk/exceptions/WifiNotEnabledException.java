package com.webilize.transfersdk.exceptions;

/**
 * If wifi in the device is not enabled, we are going to throw this exception.
 */
public class WifiNotEnabledException extends Exception {

    @Override
    public String getMessage() {
        return "Wifi Not Enabled, " + super.getMessage();
    }

    @Override
    public String toString() {
        return "Wifi Not Enabled, " + super.toString();
    }

}
