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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import qz.common.Constants;

public final class ConnectionUtilities {

    private static final String USER_AGENT = String.format("%s/%s (%s; %s) Java/%s (%s)",
        Constants.ABOUT_TITLE.replaceAll("[^a-zA-Z]", ""),
        StringUtils.join(ArrayUtils.subarray(StringUtils.split(Constants.VERSION, "."), 0, 2), "."),
        System.getProperty("os.name"),
        System.getProperty("os.version"),
        StringUtils.join(ArrayUtils.subarray(StringUtils.split(System.getProperty("java.version"), "."), 0, 2), "."),
        System.getProperty("java.vendor"));

    /**
     * Returns an input stream that reads from the URL.
     * While setting the underlying URLConnections User-Agent.
     *
     * @param url an absolute URL giving location of resource to read.
     */
    public static InputStream getInputStream(String urlString) throws IOException {
        URLConnection urlConn = new URL(urlString).openConnection();
        urlConn.setRequestProperty("User-Agent", USER_AGENT);
        return urlConn.getInputStream();
    }
}
