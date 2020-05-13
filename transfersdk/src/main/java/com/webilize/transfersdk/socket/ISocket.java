package com.webilize.transfersdk.socket;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public abstract class ISocket {

    protected KeyStore keyStore = null;
    protected PublicKey publicKey = null;
    SocketConfig socketConfig;

    //region abstract methods

    /**
     * Start the TCP connection
     */
    public abstract void connect() throws IOException, Exception;

    /**
     * Close the TCP connection
     */
    public abstract void disconnect() throws IOException, Exception;

    /**
     * Say if the TCP connection is running
     */
    public abstract boolean isConnected();

    /**
     * Returns the endpoint instance
     */
    public abstract Closeable getSocket() throws IOException;


    public void initialSetup() throws Exception {
    }

    /**
     *
     */
    public abstract boolean isReady();

    public abstract InputStream getInputStream() throws IOException;

    public abstract OutputStream getOutputStream() throws IOException;

    //endregion

    /**
     * Say if the current implementation is a Server
     * <p>
     * The client implementations must override this method to return false.
     */
    public boolean isServer() {
        return true;
    }

    public abstract void connectionLost() throws IOException;

    public PublicKey generateCertificate() throws Exception {
        if (createSelfSignedKeyStore() != null)
            return publicKey;
        else return null;
    }

    private KeyStore createSelfSignedKeyStore() throws Exception {
        keyStore = KeyStore.getInstance("PKCS12");

        keyStore.load(null);
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        final KeyPair keyPair = keyGen.generateKeyPair();
        final Certificate selfSignedCert = generate(InetAddress.getLocalHost().getCanonicalHostName(), keyPair);

        keyStore.setCertificateEntry("alias.cert", selfSignedCert);
        keyStore.setKeyEntry("alias.key", keyPair.getPrivate(), new char[0], new Certificate[]{selfSignedCert});

        publicKey = keyPair.getPublic();
        /*
        PublicKey publicKey = keyPair.getPublic();
        Log.d(TAG, "createSelfSignedKeyStore: algorithm=(" + publicKey.getAlgorithm() + "), format=(" + publicKey.getFormat() + ")");
        Log.d(TAG, "createSelfSignedKeyStore: encoded=(" + String.valueOf(publicKey.getEncoded())+")");
*/
        return keyStore;
    }

    private Certificate generate(final String fqdn, final KeyPair keypair) throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextInt();
        final Calendar now = Calendar.getInstance();
        final Date notBefore = now.getTime();
        final Calendar tenYears = Calendar.getInstance();
        tenYears.add(Calendar.YEAR, 10);
        final Date notAfter = tenYears.getTime();

        final ContentSigner selfSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .build(keypair.getPrivate());
        final X500Name owner = new X500Name("CN=" + fqdn);
        final X509CertificateHolder certHolder = new JcaX509v3CertificateBuilder(
                /*issuer*/owner,
                /*serial*/new BigInteger(64, secureRandom),
                /*cert start*/notBefore,
                /*cert end*/notAfter,
                /*subject*/owner,
                /*subject public key*/keypair.getPublic())
                .build(selfSigner);
        final X509Certificate selfSignedCert = new JcaX509CertificateConverter()
                .getCertificate(certHolder);
        selfSignedCert.verify(keypair.getPublic()); // Certificate is good!
        return selfSignedCert;
    }


    protected SSLContext createSSLContext() throws Exception {
        final KeyStore ks;
        if (this.keyStore == null) {
            ks = createSelfSignedKeyStore();
        } else {
            ks = keyStore;
        }

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, new char[0]);

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

    public String getIP() {
        if (socketConfig == null)
            return null;
        return socketConfig.getIp();

    }

    public Integer getPort() {
        if (socketConfig == null)
            return null;
        return socketConfig.getPort();
    }

    public boolean isSSL() {
        return false;
    }

    /**
     * Sets up the SSLContext to trust all Certificates.
     * <p>
     * Accepting any certificate could endanger data integrity, security, etc.
     */
    protected SSLContext trustEveryone() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager tm = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }


            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sslContext.init(null, new TrustManager[]{tm}, null);
        return sslContext;
    }
}
