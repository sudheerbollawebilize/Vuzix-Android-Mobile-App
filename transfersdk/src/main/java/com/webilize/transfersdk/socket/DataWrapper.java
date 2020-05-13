package com.webilize.transfersdk.socket;

public class DataWrapper {

    private SocketState socketState;
    private Object data;

    public DataWrapper(SocketState socketState, Object data) {
        this.socketState = socketState;
        this.data = data;
    }

    public SocketState getSocketState() {
        return socketState;
    }

    public void setSocketState(SocketState socketState) {
        this.socketState = socketState;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
