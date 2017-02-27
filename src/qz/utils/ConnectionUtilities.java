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
        urlConn.setRequestProperty("User-Agent", Constants.HTTP_USER_AGENT);
        return urlConn.getInputStream();
	}
}
