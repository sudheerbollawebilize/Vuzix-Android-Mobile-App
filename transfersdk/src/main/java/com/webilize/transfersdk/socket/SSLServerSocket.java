package com.webilize.transfersdk.socket;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.spec.InvalidParameterSpecException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class SSLServerSocket extends ISocket {

    private static final String TAG = "ServerSocket";
    private final Context context;
    private SecureRandom secureRandom;

    private File certificateFile;
    private char[] certificatePassword;
    private char[] keystorePassword;
    private File trustedPublicKeyFile;
    private char[] trustedPublicKeyFilePassword;

    private javax.net.ssl.SSLServerSocket serverSocket;
    private Socket client;
    private KeyStore serverKeyStore;
    private KeyStore clientKeyStore;

    private boolean doubleCertification = false;

    private boolean createRuntimeCertificate = false;

    private SSLServerSocketFactory sf;
    private SSLContext sslContext;

    /**
     * Passphrase for accessing our authentication keystore
     */

    //region constructor
    public SSLServerSocket(Context context, SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        this.context = context;

        secureRandom = new SecureRandom();
        secureRandom.nextInt();
    }

    public SSLServerSocket(Context context
            , SocketConfig socketConfig
            , File certificateFile
            , char[] certificatePassword
            , char[] keystorePassword
            , File trustedPublicKeyFile
            , char[] publicKeyFilePassword) {
        this(context, socketConfig);

        if (certificateFile != null && certificatePassword != null) {
            this.certificateFile = certificateFile;
            this.certificatePassword = certificatePassword;
            this.keystorePassword = keystorePassword;
        }

        if (trustedPublicKeyFile != null && publicKeyFilePassword != null) {
            this.trustedPublicKeyFile = trustedPublicKeyFile;
            this.trustedPublicKeyFilePassword = publicKeyFilePassword;
        }
    }

    //endregion

    //region privates
    private void setupClientKeyStore() throws GeneralSecurityException, IOException {

        clientKeyStore = KeyStore.getInstance("PKCS12");
        clientKeyStore.load(new FileInputStream(trustedPublicKeyFile),
                trustedPublicKeyFilePassword);
    }

    private void setupServerKeystore() throws GeneralSecurityException, IOException {
        serverKeyStore = KeyStore.getInstance("PKCS12");
        serverKeyStore.load(new FileInputStream(certificateFile), certificatePassword);
    }

    private void setupSSLContext() throws GeneralSecurityException, IOException {
        TrustManager[] trustManagers = null;
        if (doubleCertification) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(clientKeyStore);

            trustManagers = tmf.getTrustManagers();
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverKeyStore, keystorePassword);

        sslContext = SSLContext.getInstance("SSL");
        sslContext.init(kmf.getKeyManagers(),
                trustManagers,
                secureRandom);
    }

    //endregion

    //region ISocket implementation

    @Override
    public void initialSetup() throws Exception {
        if (doubleCertification)
            setupClientKeyStore();
        if (!createRuntimeCertificate) {
            setupServerKeystore();
            setupSSLContext();
        } else {
            sslContext = createSSLContext();
        }


        sf = sslContext.getServerSocketFactory();
    }

    @Override
    public void connect() throws Exception {

        serverSocket = (javax.net.ssl.SSLServerSocket) sf.createServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(socketConfig.getPort()));
        socketConfig.setPort(serverSocket.getLocalPort());
        socketConfig.setIp(serverSocket.getInetAddress().getHostAddress());

    }

    @Override
    public void disconnect() throws Exception {
        if (client != null)
            client.close();

        if (serverSocket != null) {
            Log.d(TAG, "Closing server: " + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());
            serverSocket.close();
        }

        client = null;
        serverSocket = null;
    }

    @Override
    public Socket getSocket() throws IOException {
        if (serverSocket != null) {
            if (client == null) {
                client = serverSocket.accept();
                client.setTcpNoDelay(true);
            }
        } else {
            client = null;
        }
        return client;
    }

    @Override
    public boolean isConnected() {
        return serverSocket != null; //&& client != null;
    }

    @Override
    public boolean isReady() {
        return isConnected() && client != null;
    }

    @Override
    public void connectionLost() {
        client = null;
    }

    @Override
    public boolean isSSL() {
        return true;
    }
    //endregion

    //region builder
    public static class Builder {
        private Context nestedContext;
        private SocketConfig nestedSocketConfig;

        // client certificate
        private File nestedCertificateFile;
        private char[] nestedCertificateFilePassword;
        private char[] nestedCertificatePassword;

        // server public key
        private File nestedServerPublicKeyFile;
        private char[] nestedServerPublicKeyFilePassword;

        public Builder(Context context, SocketConfig socketConfig) throws InvalidParameterSpecException {
            if (socketConfig == null || !socketConfig.isServer() || !socketConfig.isSsl())
                throw new InvalidParameterSpecException();

            this.nestedContext = context.getApplicationContext();
            this.nestedSocketConfig = socketConfig;
        }

        public Builder setCertificate(File certificate
                , char[] certificatePassword
                , char[] keystorePassword) throws FileNotFoundException {
            if (!certificate.exists())
                throw new FileNotFoundException(certificate.getName());

            if (certificatePassword == null) {
                certificatePassword = new char[]{};
            }

            this.nestedCertificateFile = certificate;
            this.nestedCertificateFilePassword = certificatePassword;
            this.nestedCertificatePassword = keystorePassword;
            return this;
        }

        /**
         * Sets the server's public key to trust.
         * <p>
         * todo: trust multiple servers
         */
        public Builder setTrustedServer(File publicKey, char[] password) throws FileNotFoundException {
            if (!publicKey.exists())
                throw new FileNotFoundException(publicKey.getName());

            if (password == null) {
                password = new char[]{};
            }

            this.nestedServerPublicKeyFile = publicKey;
            this.nestedServerPublicKeyFilePassword = password;
            return this;
        }


        public SSLServerSocket build() {
            return new SSLServerSocket(nestedContext, nestedSocketConfig
                    , nestedCertificateFile, nestedCertificateFilePassword, nestedCertificatePassword
                    , nestedServerPublicKeyFile, nestedServerPublicKeyFilePassword);
        }

    }
    //endregion

    @Override
    public InputStream getInputStream() throws IOException {
        return client.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return client.getOutputStream();
    }

}
