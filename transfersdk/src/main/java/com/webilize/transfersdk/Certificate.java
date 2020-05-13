package com.webilize.transfersdk;

import java.io.File;

public class Certificate {
    private File certificate;
    private char[] certificatePassword;
    private char[] keystorePassword;

    public Certificate(File certificate, char[] certificatePassword) {
        this.certificate = certificate;
        this.certificatePassword = certificatePassword;
        this.keystorePassword = new char[]{};
    }

    public Certificate(File certificate, char[] filePassword, char[] certificatePassword) {
        this.certificate = certificate;
        this.certificatePassword = filePassword;
        this.keystorePassword = certificatePassword;
    }

    public File getCertificate() {
        return certificate;
    }

    public void setCertificate(File certificate) {
        this.certificate = certificate;
    }

    public char[] getCertificatePassword() {
        return certificatePassword;
    }

    public void setCertificatePassword(char[] certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public char[] getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(char[] keystorePassword) {
        this.keystorePassword = keystorePassword;
    }
}
