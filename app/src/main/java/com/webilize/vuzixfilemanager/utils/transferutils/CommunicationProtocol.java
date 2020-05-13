package com.webilize.vuzixfilemanager.utils.transferutils;

import android.content.Context;
import android.util.Log;

import com.webilize.transfersdk.RXConnection;
import com.webilize.vuzixfilemanager.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CommunicationProtocol {

    private static final String TAG = "CommProtocol";

    public static final int DEAULT_PORT = 30006;

    private RXConnection rxSocket;

    private CompositeDisposable bag = new CompositeDisposable();

    //region singleton
    private static CommunicationProtocol instance;

    private CommunicationProtocol() {
        bag.clear();
    }

    public static CommunicationProtocol getInstance() {
        if (instance == null) {
            instance = new CommunicationProtocol();
        }
        return instance;
    }

    //Disconnects socket and destroy Singleton
    public void destroy(Context context) {
        Log.d(TAG, "destroy Socket");
        if (bag != null)
            bag.add(deleteCache(context.getApplicationContext())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> Log.d(TAG, "cache cleared")));

        if (rxSocket != null) {
            rxSocket.disconnect()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            bag.add(d);
                        }

                        @Override
                        public void onComplete() {
                            rxSocket = null;
                            instance = null;
                            if (bag != null) bag.dispose();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "onError: ", e);
                        }
                    });
        }

    }
    //endregion

    //region socket
    public void addDisposable(Disposable disposable) {
        bag.add(disposable);
    }
    //endregion

    public static JSONObject createQRContent(String ip, int port, String ssid) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ip", ip);
        jsonObject.put("port", port);
        jsonObject.put("ssid", ssid);
        jsonObject.put("device_name", android.os.Build.MODEL);
        jsonObject.put("app_id", R.string.app_id);
        return jsonObject;
    }

    public static Completable deleteCache(Context context) {
        return Completable.fromAction(() -> deleteRecursive(SocialBladeProtocol.getThumbnailsFolder(context.getApplicationContext())));
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    public RXConnection getConnection() {
        return rxSocket;
    }

    public boolean isConnected() {
        return rxSocket != null && rxSocket.isConnected();
    }

    public Completable disconnect() {
        if (rxSocket != null && rxSocket.isConnected()) return rxSocket.disconnect();
        else return new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver observer) {

            }
        };
    }

    public int getConnectionPort() {
        if (rxSocket != null) {
            return rxSocket.getPort();
        }
        return DEAULT_PORT;
    }

    public void setRXConnection(RXConnection rxConnection) {
        this.rxSocket = rxConnection;
    }
}
