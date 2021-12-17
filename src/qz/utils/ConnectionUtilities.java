/**
 * @author Ewan McDougall
 *
 * Copyright (C) 2017 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.utils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;

import javax.net.ssl.*;

public final class ConnectionUtilities {

    private static final Logger log = LogManager.getLogger(ConnectionUtilities.class);
    private static Map<String,String> requestProps;

    /**
     * Returns an input stream that reads from the URL.
     * While setting the underlying URLConnections User-Agent.
     *
     * @param urlString an absolute URL giving location of resource to read.
     */
    public static InputStream getInputStream(String urlString) throws IOException {
        try {
            URLConnection urlConn = new URL(urlString).openConnection();
            for( String key : getRequestProperties().keySet()) {
                urlConn.setRequestProperty(key, requestProps.get(key));
            }
            return urlConn.getInputStream();
        } catch(IOException e) {
            if(e instanceof SSLHandshakeException) {
                logSslInformation(urlString);
            }
            throw e;
        }
    }

    /**
     * A blind SSL trust manager, for debugging SSL issues
     */
    private static X509TrustManager BLIND_TRUST_MANAGER = new X509TrustManager() {
        private X509Certificate[] accepted;

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) {
            // do nothing
        }

        @Override
        public void checkServerTrusted(X509Certificate[] accepted, String string) {
            this.accepted = accepted;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return accepted;
        }
    };

    /**
     * Log certificate information for a given URL, useful for troubleshooting "PKIX path building failed"
     */
    private static void logSslInformation(String urlString) {
        StringBuilder certInfo = new StringBuilder("\nCertificate details are unavailable");
        try {
            URL url = new URL(urlString);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {BLIND_TRUST_MANAGER}, null);
            SSLSocketFactory factory = context.getSocketFactory();
            SSLSocket socket = (SSLSocket)factory.createSocket(url.getHost(), url.getPort());
            socket.startHandshake();
            socket.close();

            Certificate[] chain = socket.getSession().getPeerCertificates();

            if (chain != null) {
                certInfo = new StringBuilder();
                for(java.security.cert.Certificate cert : chain) {
                    if (cert instanceof X509Certificate) {
                        X509Certificate x = (X509Certificate)cert;
                        certInfo.append(String.format("\n\n\t%s: %s", "Subject: ", x.getIssuerX500Principal()));
                        certInfo.append(String.format("\n\t%s: %s", "From: ", x.getNotBefore()));
                        certInfo.append(String.format("\n\t%s: %s", "Expires: ", x.getNotAfter()));
                    }
                }
            }

        } catch(Exception ignore) {}
        log.error("A trust exception has occurred with the provided certificate(s). This\n" +
                          "\tmay be SSL misconfiguration, interception by proxy, firewall, antivirus\n" +
                          "\tor in some cases a dated or corrupted Java installation. Please attempt\n" +
                          "\tto resolve this problem manually before reaching out to support." +
                          "{}\n", certInfo);
    }

    private static Map<String, String> getRequestProperties() {
        if (requestProps == null) {
            requestProps = new HashMap<String, String>() {
                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    for (String key : keySet())
                        sb.append(key + ": " + get(key) + "\n");
                    return sb.toString();
                }
            };

            // Legacy User-Agent String
            requestProps.put("User-Agent", String.format("Mozilla/5.0 (%s; %s) %s/%s %s/%s",
                                                         getUserAgentOS(),
                                                         getUserAgentArch(),
                                                         Constants.ABOUT_TITLE.replaceAll("[^a-zA-Z]", ""),
                                                         Constants.VERSION.getNormalVersion(),
                                                         getFrameworkName(),
                                                         getFrameworkMajorVersion()

            ));

            // Client Hints
            requestProps.put("Sec-CH-UA-Platform", getPlatform(false));
            requestProps.put("Sec-CH-UA-Platform-Version", getPlatformVersion());
            requestProps.put("Sec-CH-UA-Arch", getArch());
            requestProps.put("Sec-CH-UA-Bitness", getBitness());
            requestProps.put("Sec-CH-UA-Full-Version", Constants.VERSION.toString());
            requestProps.put("Sec-CH-UA", String.format("\"%s\"; v=\"%s\", \"%s\"; v=\"%s\"",
                                                        Constants.ABOUT_TITLE,
                                                        Constants.VERSION,
                                                        getFrameworkName(),
                                                        getFrameworkVersion()));
            log.trace("User agent string for URL requests:\n\n{}", requestProps.toString());
        }
        return requestProps;
    }

    private static String getArch() {
        switch(SystemUtilities.getJreArch()) {
            case X86:
            case X86_64:
                return "x86";
            case AARCH64:
                return "arm";
            case RISCV:
                return "riscv";
            case PPC:
                return "ppc";
            default:
                return "unknown";
        }
    }

    private static String getBitness() {
        // If available, will return "64" or "32"
        String bitness = System.getProperty("sun.arch.data.model");
        if(bitness != null ) {
            return bitness;
        }
        // fallback on some sane, hard-coded values
        switch(SystemUtilities.getJreArch()) {
            case ARM:
            case X86:
                return "32";
            case X86_64:
            case RISCV:
            case PPC:
            default:
                return "64";
        }
    }

    private static String getPlatform(boolean legacy) {
        if(SystemUtilities.isWindows()) {
            return legacy ? "Windows NT" : "Windows";
        } else if(SystemUtilities.isMac()) {
            return legacy ? "Macintosh" : "macOS";
        } else if(SystemUtilities.isLinux()) {
            //detect display manager
            String linuxOS = "";
            String[] parts = StringUtils.split(System.getProperty("awt.toolkit"), ".");
            //assume sun.awt.X11.XToolKit namespace
            if (!GraphicsEnvironment.isHeadless() && parts != null && parts.length > 2) {
                linuxOS = parts[2];
            }
            if (UnixUtilities.isUbuntu()) {
                linuxOS += (linuxOS.isEmpty() ? "" : "; ") + "Ubuntu";
            } else if(UnixUtilities.isFedora()) {
                linuxOS += (linuxOS.isEmpty()? "" : "; ") + "Fedora";
            }
            return linuxOS;
        }
        return System.getProperty("os.name");
    }

    private static String getPlatformVersion() {
        return System.getProperty("os.version");
    }

    private static String getFrameworkName() {
        return "Java";
    }

    private static String getFrameworkMajorVersion() {
        return System.getProperty("java.vm.specification.version");
    }

    private static String getFrameworkVersion() {
        return Constants.JAVA_VERSION.toString();
    }

    private static String getUserAgentOS() {
        if (SystemUtilities.isWindows()) {
            //assume NT
            return String.format("%s %s", getPlatform(true), getPlatformVersion());
        } else if(SystemUtilities.isMac()) {
            return String.format("%s; %s %s", getPlatform(true), System.getProperty("os.name"), getPlatformVersion().replace('.', '_'));
        }
        return getPlatform(true);
    }

    private static String getUserAgentArch() {
        String arch;
        switch (SystemUtilities.getJreArch()) {
            case X86:
                arch = "x86";
                break;
            case X86_64:
                arch = "x86_64";
                break;
            default:
                arch = SystemUtilities.OS_ARCH;
        }

        switch(SystemUtilities.getOsType()) {
            case LINUX:
                return "Linux " + arch;
            case WINDOWS:
                if(WindowsUtilities.isWow64()) {
                    return "WOW64";
                }
            default:
                return arch;
        }
    }
}
