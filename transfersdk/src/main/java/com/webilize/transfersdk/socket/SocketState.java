package com.webilize.transfersdk.socket;

public enum SocketState {
    // connection States
    CONNECTED,
    DISCONNECTED,
    SERVER_NOT_AVAILABLE,
    NEW_CONNECTION,
    CLIENT_DISCONNECTED,
    CONNECTION_TIMEOUT,
    METADATA,
    STARTING_WRITING,
    JSON_RECEIVED,
    FILE_RECEIVED,
    STARTING_READING,
    PROGRESS,
    MULTIPLE_FILES

}

