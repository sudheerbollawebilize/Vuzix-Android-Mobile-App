package com.webilize.transfersdk.socket;

import android.content.Context;

import com.webilize.transfersdk.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.spec.InvalidParameterSpecException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SSLClientSocket extends ISocket {

    private static final String TAG = "SSLClientSocket";

    private final SecureRandom secureRandom;

    // region my certificate
    private KeyStore certificateKeyStore;
    private File certificateFile;
    private char[] certificateFilePassword;
    private char[] certificatePassword;
    //endregion

    // region trusted server's public key
    private KeyStore trustKeyStore;
    private File publicKeyFile;
    private char[] publicKeyFilePassword;
    //endregion

    private Context context;
    private SSLSocket socket;

    private SSLContext sslContext;
    private SSLSocketFactory sf;

    // not safe, just for testing proposes
    private boolean trustEveryone = false;
    private boolean doubleCertification = false;

    //region constructor
    public SSLClientSocket(Context context, SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
        this.context = context;

        secureRandom = new SecureRandom();
        secureRandom.nextInt();
    }

    public SSLClientSocket(Context context, SocketConfig socketConfig
            , File certificateFile, char[] certificateFilePassword
            , char[] certificatePassword, File publicKeyFile, char[] publicKeyFilePassword) {
        this(context, socketConfig);

        if (certificateFile != null && certificateFilePassword != null) {
            this.certificateFile = certificateFile;
            this.certificateFilePassword = certificateFilePassword;
            this.certificatePassword = certificatePassword;
        }

        if (publicKeyFile != null && publicKeyFilePassword != null) {
            this.publicKeyFile = publicKeyFile;
            this.publicKeyFilePassword = publicKeyFilePassword;
        }

    }
    //endregion

    //region privates

    /**
     * Certificate pinning with self-singed certificate: It means hard-coding the certificate known
     * to be used by the server in the mobile application. The app can then ignore the device’s
     * trust store and rely on its own, and allow only SSL connections to hosts signed with
     * certificates stored inside the application.
     * <p>
     * DRAWBACKS
     * Less flexibility - when you do SSL certificate pinning, changing the SSL certificate is not
     * that easy. For every SSL certificate change, you have to make an update to the app, push it
     * to Google Play and hope the users will install it.
     */
    private void setupTrustKeystore(File publicKeyFile, char[] password) throws GeneralSecurityException, IOException {
        if (publicKeyFile != null && password != null) {
            trustKeyStore = KeyStore.getInstance("PKCS12");
            trustKeyStore.load(new FileInputStream(publicKeyFile), password);
        }
    }

    private void setupClientKeyStore() throws GeneralSecurityException, IOException {
        certificateKeyStore = KeyStore.getInstance("PKCS12");
        certificateKeyStore.load(new FileInputStream(certificateFile), certificateFilePassword);
    }

    private void setupSSLContext() throws GeneralSecurityException, IOException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustKeyStore);

        KeyManager[] keyManager = null;
        if (doubleCertification) {
            KeyManagerFactory kmf;
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(certificateKeyStore, certificatePassword);
            keyManager = kmf.getKeyManagers();
        }

        sslContext = SSLContext.getInstance("SSL");
        sslContext.init(keyManager,
                tmf.getTrustManagers(),
                secureRandom);
    }
    //endregion

    //region ISocket implementation

    @Override
    public void initialSetup() throws Exception {
        if (doubleCertification)
            setupClientKeyStore();
        if (trustEveryone && BuildConfig.DEBUG) {
            sslContext = trustEveryone();
        } else {
            setupTrustKeystore(publicKeyFile, publicKeyFilePassword);
            setupSSLContext();
        }

        setupTrustKeystore(publicKeyFile, publicKeyFilePassword);
        setupSSLContext();

        sf = sslContext.getSocketFactory();
    }

    @Override
    public void connect() throws Exception {
        socket = (SSLSocket) sf.createSocket(socketConfig.getIp(), socketConfig.getPort());
        socket.setTcpNoDelay(true);
    }

    @Override
    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
        }
        socket = null;
    }

    @Override
    public boolean isConnected() {
        return socket != null;
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public boolean isServer() {
        return false;
    }

    @Override
    public boolean isReady() {
        return isConnected();
    }

    @Override
    public void connectionLost() throws IOException {
        disconnect();
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

        // certificate
        private File nestedCertificateFile;
        private char[] nestedCertificateFilePassword;
        private char[] nestedCertificatePassword;

        // trust end-point
        private File nestedServerPublicKeyFile;
        private char[] nestedServerPublicKeyFilePassword;

        public Builder(Context context, SocketConfig socketConfig) throws InvalidParameterSpecException {
            if (socketConfig == null || socketConfig.isServer() || !socketConfig.isSsl())
                throw new InvalidParameterSpecException();

            this.nestedContext = context.getApplicationContext();
            this.nestedSocketConfig = socketConfig;
        }

        public Builder setCertificate(File certificate
                , char[] filePassword
                , char[] certificatePassword) throws FileNotFoundException {
            if (!certificate.exists())
                throw new FileNotFoundException(certificate.getName());

            if (filePassword == null) {
                filePassword = new char[]{};
            }

            this.nestedCertificateFile = certificate;
            this.nestedCertificateFilePassword = filePassword;
            this.nestedCertificatePassword = certificatePassword;
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


        public SSLClientSocket build() {
            return new SSLClientSocket(nestedContext, nestedSocketConfig
                    , nestedCertificateFile, nestedCertificateFilePassword, nestedCertificatePassword
                    , nestedServerPublicKeyFile, nestedServerPublicKeyFilePassword);
        }

    }
    //endregion

    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }
}
