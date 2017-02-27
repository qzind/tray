package qz.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import qz.common.Constants;

public final class ConnectionUtilities {
    private ConnectionUtilities() {
    }

    public static InputStream getInputStream(String urlString) throws IOException {
        URLConnection urlConn = new URL(urlString).openConnection();
        urlConn.setRequestProperty("User-Agent", userAgent());
        return urlConn.getInputStream();
    }

    public static String userAgent() {
        String.format("%s/%s (%s; %s) Java/%s (%s)",
            Constants.ABOUT_TITLE.replaceAll("[^a-zA-Z]", ""),
            Constants.VERSION,
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("java.version"),
            System.getProperty("java.vendor"));
    }
}
