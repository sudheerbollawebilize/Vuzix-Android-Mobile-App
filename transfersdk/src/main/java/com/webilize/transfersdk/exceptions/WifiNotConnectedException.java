package com.webilize.transfersdk.exceptions;

/**
 * If there is any issue when establishing connection with wifi, we are going to throw this exception.
 */
public class WifiNotConnectedException extends Exception {

    @Override
    public String getMessage() {
        return "Wifi Not Connected, " + super.getMessage();
    }

    @Override
    public String toString() {
        return "Wifi Not Connected, " + super.toString();
    }

}
