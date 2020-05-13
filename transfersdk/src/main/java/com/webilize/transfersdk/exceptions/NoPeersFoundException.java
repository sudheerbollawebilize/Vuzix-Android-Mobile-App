package com.webilize.transfersdk.exceptions;

/**
 * After scan is done and no wifi direct devices are found, we are going to throw this exception.
 */
public class NoPeersFoundException extends Exception {

    @Override
    public String getMessage() {
        return "No Peers Found, " + super.getMessage();
    }

    @Override
    public String toString() {
        return "No Peers Found, " + super.toString();
    }

}
