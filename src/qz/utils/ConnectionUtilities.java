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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.LoggerFactory;
import qz.common.Constants;

public final class ConnectionUtilities {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ConnectionUtilities.class);
    private static Map<String,String> requestProps;

    /**
     * Returns an input stream that reads from the URL.
     * While setting the underlying URLConnections User-Agent.
     *
     * @param urlString an absolute URL giving location of resource to read.
     */
    public static InputStream getInputStream(String urlString) throws IOException {
        URLConnection urlConn = new URL(urlString).openConnection();
        for( String key : getRequestProperties().keySet()) {
            urlConn.setRequestProperty(key, requestProps.get(key));
        }
        return urlConn.getInputStream();
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
            requestProps.put("Sec-CH-UA-Full-Version", Constants.VERSION.toString());
            requestProps.put("Sec-CH-UA", String.format("\"%s\"; v=\"%s\"", Constants.ABOUT_TITLE, Constants.VERSION));
            log.trace("User agent string for URL requests:\n\n{}", requestProps.toString());
        }
        return requestProps;
    }

    private static String getArch() {
        String arch = System.getProperty("os.arch");
        return "amd64".equalsIgnoreCase(arch) ? "x86_64" : arch;
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
            if (SystemUtilities.isUbuntu()) {
                linuxOS += (linuxOS.isEmpty() ? "" : "; ") + "Ubuntu";
            } else if(SystemUtilities.isFedora()) {
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
        String arch = System.getProperty("os.arch");
        arch = "amd64".equalsIgnoreCase(arch) ? "x86_64" : arch;
        if (SystemUtilities.isWow64()) {
            return "WOW64";
        } else if(SystemUtilities.isLinux()) {
            return "Linux " + arch;
        }
        return arch;
    }
}
