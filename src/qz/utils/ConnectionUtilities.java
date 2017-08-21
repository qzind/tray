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

import org.apache.commons.lang3.StringUtils;

import org.slf4j.LoggerFactory;
import qz.common.Constants;

public final class ConnectionUtilities {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ConnectionUtilities.class);
    private static String userAgent;

    /**
     * Returns an input stream that reads from the URL.
     * While setting the underlying URLConnections User-Agent.
     *
     * @param urlString an absolute URL giving location of resource to read.
     */
    public static InputStream getInputStream(String urlString) throws IOException {
        URLConnection urlConn = new URL(urlString).openConnection();
        urlConn.setRequestProperty("User-Agent", getUserAgent());
        return urlConn.getInputStream();
    }

    private static String getUserAgent() {
        if (userAgent == null) {
            //mozilla 5.0 compat
            userAgent = String.format("Mozilla/5.0 (%s; %s) %s/%s Java/%s",
                                      getOS(),
                                      getArch(),
                                      Constants.ABOUT_TITLE.replaceAll("[^a-zA-Z]", ""),
                                      Constants.VERSION.getNormalVersion(),
                                      System.getProperty("java.vm.specification.version")
            );
            log.debug("User agent string for URL requests: {}", userAgent);
        }
        return userAgent;
    }

    private static String getArch() {
        String arch = System.getProperty("os.arch");
        arch = "amd64".equalsIgnoreCase(arch) ? "x86_64" : arch;
        if (SystemUtilities.isWow64()) {
            return "WOW64";
        } else if(SystemUtilities.isLinux()) {
            return "Linux " + arch;
        }
        return arch;
    }

    private static String getOS() {
        if (SystemUtilities.isWindows()) {
            //assume NT
            return String.format("Windows NT %s", System.getProperty("os.version"));
        } else if(SystemUtilities.isMac()) {
            return String.format("Macintosh; %s %s", System.getProperty("os.name"), System.getProperty("os.version").replace('.', '_'));
        } else if(SystemUtilities.isLinux()) {
            //detect display manager
            String linuxOS = "";
            String[] parts = StringUtils.split(System.getProperty("awt.toolkit"), ".");
            //assume sun.awt.X11.XToolKit namespace
            if (!GraphicsEnvironment.isHeadless() && parts != null && parts.length > 2) {
                linuxOS = parts[2];
            }
            if (SystemUtilities.isUbuntu()) {
                linuxOS += (linuxOS.isEmpty() ? "" : "; ") + "Ubuntu";
            } else if(SystemUtilities.isFedora()) {
                linuxOS += (linuxOS.isEmpty()? "" : "; ") + "Fedora";
            }
            return linuxOS;
        }
        return System.getProperty("os.name");
    }
}
