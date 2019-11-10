/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer.certificate;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.installer.Installer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WindowsCertificateInstaller extends NativeCertificateInstaller {
    private static final Logger log = LoggerFactory.getLogger(WindowsCertificateInstaller.class);
    private WinCrypt.HCERTSTORE store;
    private Installer.PrivilegeLevel certType;

    public WindowsCertificateInstaller(Installer.PrivilegeLevel certType) {
        setInstallType(certType);
    }

    public boolean add(File certFile) {
        log.info("Writing certificate {} to {} store using Crypt32...", certFile, certType);
        try {
            PEMParser pem = new PEMParser(new FileReader(certFile));
            X509CertificateHolder certHolder = (X509CertificateHolder)pem.readObject();
            byte[] bytes = certHolder.getEncoded();
            Pointer pointer = new Memory(bytes.length);
            pointer.write(0, bytes, 0, bytes.length);

            boolean success = Crypt32.INSTANCE.CertAddEncodedCertificateToStore(
                    openStore(),
                    WinCrypt.X509_ASN_ENCODING,
                    pointer,
                    bytes.length,
                    Crypt32.CERT_STORE_ADD_REPLACE_EXISTING,
                    null
            );
            log.info(Kernel32Util.formatMessage(Native.getLastError()));

            closeStore();

            return success;
        } catch(IOException e) {
            log.warn("An error occurred installing the certificate", e);
        }
        return false;
    }

    private WinCrypt.HCERTSTORE openStore() {
        if(store == null) {
            store = openStore(certType);
        }
        return store;
    }

    private void closeStore() {
        if(store != null && closeStore(store)) {
            store = null;
        } else {
            log.warn("Unable to close {} cert store", certType);
        }
    }

    private static WinCrypt.HCERTSTORE openStore(Installer.PrivilegeLevel certType) {
        log.info("Opening {} store using Crypt32...", certType);

        WinCrypt.HCERTSTORE store = Crypt32.INSTANCE.CertOpenStore(
                Crypt32.CERT_STORE_PROV_SYSTEM,
                0,
                null,
                certType == Installer.PrivilegeLevel.USER ? Crypt32.CERT_SYSTEM_STORE_CURRENT_USER : Crypt32.CERT_SYSTEM_STORE_LOCAL_MACHINE,
                "ROOT"
        );
        if(store == null) {
            log.warn(Kernel32Util.formatMessage(Native.getLastError()));
        }
        return store;
    }

    private static boolean closeStore(WinCrypt.HCERTSTORE certStore) {
        boolean isClosed = Crypt32.INSTANCE.CertCloseStore(
                certStore, 0
        );
        if(!isClosed) {
            log.warn(Kernel32Util.formatMessage(Native.getLastError()));
        }
        return isClosed;
    }

    public boolean remove(List<String> ignore) {
        boolean success = true;

        List<WinCrypt.CERT_CONTEXT> hCertContextList = new ArrayList<>();

        WinCrypt.CERT_CONTEXT hCertContext;
        while(true) {
            hCertContext = Crypt32.INSTANCE.CertFindCertificateInStore(
                    openStore(),
                    WinCrypt.X509_ASN_ENCODING,
                    0,
                    Crypt32.CERT_FIND_SUBJECT_STR,
                    Constants.ABOUT_EMAIL,
                    null);


            if(hCertContext == null) {
                break;
            }
            if(success = (success && Crypt32.INSTANCE.CertDeleteCertificateFromStore(hCertContext))) {
                log.info("Successfully deleted certificate matching {}", Constants.ABOUT_EMAIL);
            } else {
                log.info(Kernel32Util.formatMessage(Native.getLastError()));
            }
            if(!Crypt32.INSTANCE.CertFreeCertificateContext(hCertContext)) {
                log.warn(Kernel32Util.formatMessage(Native.getLastError()));
            }
        }

        closeStore();
        return success;
    }

    public List<String> find() {
        return null;
    }

    public void setInstallType(Installer.PrivilegeLevel type) {
        this.certType = type;
    }

    public Installer.PrivilegeLevel getInstallType() {
        return certType;
    }

    // TODO: Switch to JNA
    public boolean verify(File certFile) {
        return WindowsCertificateInstallerCli.verifyCert(certFile);
    }

    /**
     * The JNA's Crypt32 instance oversimplifies store handling, preventing user stores from being used
     */
    interface Crypt32 extends StdCallLibrary {
        int CERT_SYSTEM_STORE_CURRENT_USER = 65536;
        int CERT_SYSTEM_STORE_LOCAL_MACHINE = 131072;
        int CERT_STORE_PROV_SYSTEM = 10;
        int CERT_STORE_ADD_REPLACE_EXISTING = 3;
        int CERT_FIND_SUBJECT_STR = 524295;

        Crypt32 INSTANCE = Native.loadLibrary("Crypt32", Crypt32.class, W32APIOptions.DEFAULT_OPTIONS);

        WinCrypt.HCERTSTORE CertOpenStore(int lpszStoreProvider, int dwMsgAndCertEncodingType, Pointer hCryptProv, int dwFlags, String pvPara);
        boolean CertCloseStore(WinCrypt.HCERTSTORE hCertStore, int dwFlags);
        boolean CertAddEncodedCertificateToStore(WinCrypt.HCERTSTORE hCertStore, int dwCertEncodingType, Pointer pbCertEncoded, int cbCertEncoded, int dwAddDisposition, Pointer ppCertContext);
        WinCrypt.CERT_CONTEXT CertFindCertificateInStore (WinCrypt.HCERTSTORE hCertStore, int dwCertEncodingType, int dwFindFlags, int dwFindType, String pvFindPara, WinCrypt.CERT_CONTEXT pPrevCertContext);
        boolean CertDeleteCertificateFromStore(WinCrypt.CERT_CONTEXT pCertContext);
        boolean CertFreeCertificateContext(WinCrypt.CERT_CONTEXT pCertContext);
    }

    // Polyfill from JNA5+
    @SuppressWarnings("UnusedDeclaration") //Library class
    public static class WinCrypt {
        public static int X509_ASN_ENCODING = 0x00000001;
        public static class HCERTSTORE extends WinNT.HANDLE {
            public HCERTSTORE() {}
            public HCERTSTORE(Pointer p) {
                super(p);
            }
        }
        public static class CERT_CONTEXT extends WinNT.HANDLE {
            public CERT_CONTEXT() {}
            public CERT_CONTEXT(Pointer p) {
                super(p);
            }
        }
    }
}
