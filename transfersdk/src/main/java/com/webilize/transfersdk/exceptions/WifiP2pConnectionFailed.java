package com.webilize.transfersdk.exceptions;

/**
 * If there is any issue connecting with our wifi peer for establishing socket connection, we are going to throw this exception.
 */
public class WifiP2pConnectionFailed extends Exception {

    @Override
    public String getMessage() {
        return "Wifi P2P Connection Failed, " + super.getMessage();
    }

    @Override
    public String toString() {
        return "Wifi P2P Connection Failed, " + super.toString();
    }

}
