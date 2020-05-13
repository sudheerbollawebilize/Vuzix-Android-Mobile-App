package com.webilize.transfersdk.wifidirect;


public enum WiFiP2PError {

    ERROR(0), P2P_NOT_SUPPORTED(1), BUSY(2);

    private int reason;

    WiFiP2PError(int reason) {
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }

    public static WiFiP2PError fromReason(int reason) {
        for (WiFiP2PError wiFiP2PError : WiFiP2PError.values()) {
            if (reason == wiFiP2PError.reason) {
                return wiFiP2PError;
            }
        }
        return null;
    }
}
