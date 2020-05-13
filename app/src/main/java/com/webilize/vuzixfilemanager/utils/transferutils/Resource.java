package com.webilize.vuzixfilemanager.utils.transferutils;

public class Resource<T> {
    public enum Status {
        ERROR, LOADING, SUCCESS
    }

    private Status status;
    private T data;
    private String message;
    private Exception exception;

    public Resource(Status status, T data, String msg) {
        this(status, data, msg, null);
    }

    public Resource(Status status, T data, String msg, Exception ex) {
        this.status = status;
        this.data = data;
        this.message = msg;
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(String msg, T data) {
        return new Resource<>(Status.ERROR, data, msg);
    }

    public static <T> Resource<T> error(String msg) {
        return new Resource<>(Status.ERROR, null, msg);
    }

    public static <T> Resource<T> error(Exception ex) {
        return new Resource<>(Status.ERROR, null, null, ex);
    }

    public static <T> Resource<T> loading(String message) {
        return new Resource<>(Status.LOADING, null, message);
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null);
    }

    //region gets

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    //endregion
}

