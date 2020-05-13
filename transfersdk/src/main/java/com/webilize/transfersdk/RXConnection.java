package com.webilize.transfersdk;

import android.content.Context;
import android.util.Log;

import com.webilize.transfersdk.helpers.StreamHelper;
import com.webilize.transfersdk.read.FileReadStrategy;
import com.webilize.transfersdk.read.JsonReadStrategy;
import com.webilize.transfersdk.read.MultipleFilesReadStrategy;
import com.webilize.transfersdk.socket.ClientSocket;
import com.webilize.transfersdk.socket.DataWrapper;
import com.webilize.transfersdk.socket.ISocket;
import com.webilize.transfersdk.socket.SSLClientSocket;
import com.webilize.transfersdk.socket.SSLServerSocket;
import com.webilize.transfersdk.socket.ServerSocket;
import com.webilize.transfersdk.socket.SocketConfig;
import com.webilize.transfersdk.socket.SocketState;
import com.webilize.transfersdk.write.FileWriteStrategy;
import com.webilize.transfersdk.write.IWriteStrategy;
import com.webilize.transfersdk.write.JsonWriteStrategy;
import com.webilize.transfersdk.write.MultipleFilesWriteStrategy;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.spec.InvalidParameterSpecException;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.subjects.PublishSubject;

public class RXConnection {

    private static final String TAG = "RXConnection";

    private final int CONNECTION_TRIES = 10;
    private final int CONNECTION_TRIES_DELAY = 1500; //ms

    private ISocket iSocket;

    private File folder;

    private RXConnection(Context context, ISocket iSocket) {
        this.iSocket = iSocket;
        folder = context.getCacheDir();
    }

    //region creational methods

    public static RXConnection createSocket(Context context, SocketConfig socketConfig) {
        ISocket iSocket;
        if (socketConfig.isSsl()) {
            if (socketConfig.isServer()) {
                iSocket = new SSLServerSocket(context, socketConfig);
            } else {
                iSocket = new SSLClientSocket(context, socketConfig);
            }
        } else {
            if (socketConfig.isServer()) {
                iSocket = new ServerSocket(socketConfig);
            } else {
                iSocket = new ClientSocket(socketConfig);
            }
        }
        return new RXConnection(context, iSocket);
    }

    public static RXConnection createServerSocket(Context context, SocketConfig socketConfig) {
        return new RXConnection(context, new ServerSocket(socketConfig));
    }

    public static RXConnection createSSLSocket(Context context, SocketConfig socketConfig
            , Certificate privateCertificate, Certificate publicCertificate) throws InvalidParameterSpecException, FileNotFoundException {
        //todo: add public certificate to doubleCertification
        if (socketConfig.isServer()) {
            return createSSLServerSocket(context, socketConfig, privateCertificate, publicCertificate);
        } else {
            return createSSLClientSocket(context, socketConfig, privateCertificate, publicCertificate);
        }
    }

    private static RXConnection createSSLClientSocket(Context context
            , SocketConfig socketConfig
            , Certificate privateCertificate
            , Certificate publicCertificate) throws InvalidParameterSpecException, FileNotFoundException {
        SSLClientSocket.Builder builder = new SSLClientSocket.Builder(context, socketConfig);
        if (privateCertificate != null) {
            builder.setCertificate(privateCertificate.getCertificate()
                    , privateCertificate.getCertificatePassword()
                    , privateCertificate.getKeystorePassword());
        }

        if (publicCertificate != null) {
            builder.setTrustedServer(publicCertificate.getCertificate()
                    , publicCertificate.getCertificatePassword());
        }

        return new RXConnection(context, builder.build());
    }

    private static RXConnection createSSLServerSocket(Context context
            , SocketConfig socketConfig
            , Certificate privateCertificate
            , Certificate publicCertificate) throws InvalidParameterSpecException, FileNotFoundException {
        SSLServerSocket.Builder builder = new SSLServerSocket.Builder(context, socketConfig);
        if (privateCertificate != null) {
            builder.setCertificate(privateCertificate.getCertificate()
                    , privateCertificate.getCertificatePassword()
                    , privateCertificate.getKeystorePassword());
        }

        if (publicCertificate != null) {
            builder.setTrustedServer(publicCertificate.getCertificate()
                    , publicCertificate.getCertificatePassword());
        }

        return new RXConnection(context, builder.build());
    }


    public static RXConnection createSSLServerSocket(Context context, SocketConfig socketConfig
            , File certificate
            , char[] certificateFilePassword) throws InvalidParameterSpecException, FileNotFoundException {
        return createSSLServerSocket(context, socketConfig, certificate, certificateFilePassword, null);
    }

    public static RXConnection createSSLServerSocket(Context context
            , SocketConfig socketConfig
            , Certificate certificate) throws InvalidParameterSpecException, FileNotFoundException {
        return createSSLServerSocket(context, socketConfig, certificate.getCertificate()
                , certificate.getCertificatePassword()
                , certificate.getKeystorePassword());
    }

    public static RXConnection createSSLServerSocket(Context context, SocketConfig socketConfig
            , File certificate
            , char[] certificateFilePassword
            , char[] certificatePassword)
            throws InvalidParameterSpecException, FileNotFoundException {
        SSLServerSocket sslServerSocket = new SSLServerSocket.Builder(context, socketConfig)
                .setCertificate(certificate, certificateFilePassword, certificatePassword)
                .build();

        return new RXConnection(context, sslServerSocket);
    }

    private static RXConnection createSSLClientSocket(Context context
            , SocketConfig socketConfig
            , Certificate publicCertificate) throws InvalidParameterSpecException, FileNotFoundException {
        return createSSLClientSocket(context
                , socketConfig
                , publicCertificate.getCertificate()
                , publicCertificate.getCertificatePassword());
    }

    public static RXConnection createSSLClientSocket(Context context, SocketConfig socketConfig
            , File publicKey, char[] filePassword)
            throws InvalidParameterSpecException, FileNotFoundException {
        SSLClientSocket sslSocket = new SSLClientSocket.Builder(context, socketConfig)
                .setTrustedServer(publicKey, filePassword)
                .build();
        return new RXConnection(context, sslSocket);
    }


    public static RXConnection createClientSocket(Context context, SocketConfig socketConfig) {
        return new RXConnection(context, new ClientSocket(socketConfig));
    }

    public static RXConnection createSocket(Context context, ISocket iSocket) {
        return new RXConnection(context, iSocket);
    }

    //endregion

    //region get / set

    public void setDefaultFolder(File file) {
        this.folder = file;
    }

    public String getIP() {
        return iSocket.getIP();
    }

    public Integer getPort() {
        return iSocket.getPort();
    }

    public Socket getSocket() throws IOException {
        if (iSocket.getSocket() instanceof Socket)
            return (Socket) iSocket.getSocket();
        else return null;
    }

    //endregion

    public void disconnectSync() {
        if (iSocket != null) {
            try {
                iSocket.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "disconnectSync: ", e);
            }
        }
    }

    public Completable disconnect() {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                iSocket.disconnect();
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    Log.e(TAG, "disconnect sleep error", ex);
                }
            }
        });
    }

    public Single<Boolean> connect() {
        return connect(false);
    }

    public Single<Boolean> connect(final boolean wait) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

                if (iSocket == null) {
                    throw new Exception("Must initialize socket");
                }

                iSocket.initialSetup();

                boolean fail = true;
                int i = 0;
                while (fail && i < CONNECTION_TRIES && !Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(CONNECTION_TRIES_DELAY);
                        iSocket.connect();
                        fail = false;
                    } catch (Exception ex) {
                        if (ex instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        Log.e(TAG, "Error connecting: ", ex);
                        i++;
                    }
                }

                if (fail)
                    throw new Exception("Connection failed");

                if (iSocket.isServer() && wait) {
                    Closeable socket = iSocket.getSocket();
                    return socket != null;
                }

                return true;
            }
        });
    }

    public void clientDisconnected() {
        try {
            iSocket.connectionLost();
        } catch (IOException e) {
            Log.e(TAG, "clientDisconnected: ", e);
        }
    }

    public Single<DataWrapper> read(final PublishSubject<DataWrapper> emitter) {
        return Single.fromCallable(new Callable<DataWrapper>() {
            @Override
            public DataWrapper call() throws Exception {
                Closeable socket = iSocket.getSocket();
                if (socket != null) {
                    if (iSocket.isServer() && emitter != null) {
                        emitter.onNext(new DataWrapper(SocketState.NEW_CONNECTION, null));
                    }

                    InputStream input = new BufferedInputStream(iSocket.getInputStream());
                    ByteArrayOutputStream buffer = StreamHelper.read(input, 1);
                    if (buffer == null) {
                        iSocket.connectionLost();
                        if (iSocket.isServer()) {
                            return (new DataWrapper(SocketState.CLIENT_DISCONNECTED, null));
                        } else {
                            return (new DataWrapper(SocketState.DISCONNECTED, null));
                        }

                    } else {
                        byte[] typeBytes = buffer.toByteArray();
                        switch (typeBytes[0]) {
                            case Protocol.FILE:
                                FileReadStrategy readStrategy = new FileReadStrategy.Builder()
                                        .setFolder(folder)
                                        .setInputStream(input)
                                        .setBufferedStreams(true)
                                        .build();
                                readStrategy.setEmitter(emitter);
                                File file = readStrategy.read();

                                return (new DataWrapper(SocketState.FILE_RECEIVED, file));
                            case Protocol.JSON:
                                JsonReadStrategy jsonStrategy = new JsonReadStrategy.Builder()
                                        .setInputStream(input)
                                        .setBufferedStreams(true)
                                        .build();
                                jsonStrategy.setEmitter(emitter);
                                JSONObject jsonObject = jsonStrategy.read();

                                return new DataWrapper(SocketState.JSON_RECEIVED, jsonObject);

                            case Protocol.MULTIPLE:

                                MultipleFilesReadStrategy multipleFilesReadStrategy = new MultipleFilesReadStrategy.Builder()
                                        .setFolder(folder)
                                        .setInputStream(input)
                                        .setBufferedStreams(true)
                                        .build();
                                multipleFilesReadStrategy.setEmitter(emitter);

                                List<File> files = multipleFilesReadStrategy.read();
                                return new DataWrapper(SocketState.MULTIPLE_FILES, files);
                            default:
                                throw new Exception("No protocol defined");
                        }
                    }
                } else {
                    iSocket.connectionLost();
                    if (iSocket.isServer()) {
                        return (new DataWrapper(SocketState.CLIENT_DISCONNECTED, null));
                    } else {
                        return (new DataWrapper(SocketState.DISCONNECTED, null));
                    }
                }

            }
        });
    }

    //region write methods

    /**
     * This method is for sending JSONObject as file
     *
     * @param jsonObject the file with details of the files we are sharing.
     * @param emitter
     * @return
     */
    public Completable write(final JSONObject jsonObject, final PublishSubject<DataWrapper> emitter) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                try {
                    Closeable socket = iSocket.getSocket();
                    if (socket != null) {
                        OutputStream outputStream = iSocket.getOutputStream();
                        IWriteStrategy writeStrategy = new JsonWriteStrategy.Builder()
                                .setJSON(jsonObject)
                                .setOutputStream(outputStream)
                                .setBufferedStreams(true)
                                .build();
                        if (emitter != null)
                            writeStrategy.setEmitter(emitter);
                        writeStrategy.write();
                    } else {
                        throw new Exception("Connection lost");
                    }
                } catch (Exception e) {
                    iSocket.connectionLost();
                    throw e;
                }
            }
        });
    }

    /**
     * This method is for sending single file
     *
     * @param file
     * @param emitter
     * @return
     */
    public Completable write(final File file, final PublishSubject<DataWrapper> emitter) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                try {
                    Closeable socket = iSocket.getSocket();
                    if (socket != null) {
                        OutputStream outputStream = iSocket.getOutputStream();
                        IWriteStrategy writeStrategy = new FileWriteStrategy.Builder()
                                .setFile(file)
                                .setOutputStream(outputStream)
                                .setBufferedStreams(true)
                                .build();
                        if (emitter != null)
                            writeStrategy.setEmitter(emitter);
                        writeStrategy.write();
                    } else {
                        throw new Exception("Connection lost");
                    }
                } catch (Exception e) {
                    iSocket.connectionLost();
                    throw e;
                }
            }
        });
    }

    /**
     * This method is for sending multiple files
     *
     * @param files
     * @param emitter
     * @return
     */
    public Completable write(final List<File> files, final PublishSubject<DataWrapper> emitter) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                try {
                    Closeable socket = iSocket.getSocket();
                    if (socket != null) {
                        OutputStream outputStream = iSocket.getOutputStream();
                        MultipleFilesWriteStrategy writeStrategy = new MultipleFilesWriteStrategy.Builder()
                                .setOutputStream(outputStream)
                                .setBufferedStreams(true)
                                .build();
                        if (emitter != null)
                            writeStrategy.setEmitter(emitter);
                        writeStrategy.write(files);
                    } else {
                        throw new Exception("Connection lost");
                    }
                } catch (Exception e) {
                    iSocket.connectionLost();
                    throw e;
                }
            }
        });
    }
    //endregion

    /**
     * This method for checking if the socket is connected or not
     *
     * @return
     */
    public boolean isConnected() {
        return iSocket != null && iSocket.isConnected();
    }

    public Single<Boolean> waitClient() {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

                if (iSocket == null) {
                    throw new Exception("Must initialize socket");
                }

                Closeable socket = iSocket.getSocket();

                return socket != null;
            }
        });
    }

    public boolean clientIsConnected() {
        return isConnected() && iSocket.isReady();
    }

    public boolean isServer() {
        return iSocket.isServer();
    }

}
