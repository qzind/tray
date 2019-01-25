package qz.common;

import org.apache.commons.ssl.Base64;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class AboutInfo {

    private static final Logger log = LoggerFactory.getLogger(AboutInfo.class);


    public static JSONObject gatherAbout(String domain) {
        JSONObject about = new JSONObject();

        KeyStore keyStore = SecurityInfo.getKeyStore(PrintSocketServer.getTrayProperties());

        try {
            about.put("product", product());
            about.put("socket", socket(keyStore, domain));
            about.put("environment", environment());
            about.put("ssl", ssl(keyStore));
            about.put("libraries", libraries());
        }
        catch(JSONException | GeneralSecurityException e) {
            log.error("Failed to write JSON data", e);
        }

        return about;
    }

    private static JSONObject product() throws JSONException {
        JSONObject product = new JSONObject();

        product
                .put("title", Constants.ABOUT_TITLE)
                .put("version", Constants.VERSION)
                .put("vendor", Constants.ABOUT_COMPANY)
                .put("url", Constants.ABOUT_URL);

        return product;
    }

    private static JSONObject socket(KeyStore keystore, String domain) throws JSONException {
        JSONObject socket = new JSONObject();

        socket
                .put("domain", domain)
                .put("secureProtocol", "wss")
                .put("securePort", keystore == null? "none":PrintSocketServer.getSecurePortInUse())
                .put("insecureProtocol", "ws")
                .put("insecurePort", PrintSocketServer.getInsecurePortInUse());

        return socket;
    }

    private static JSONObject environment() throws JSONException {
        JSONObject environment = new JSONObject();

        environment
                .put("os", SystemUtilities.getOS())
                .put("java", Constants.JAVA_VERSION);

        return environment;
    }

    private static JSONObject ssl(KeyStore keystore) throws JSONException, KeyStoreException, CertificateEncodingException {
        JSONObject ssl = new JSONObject();

        JSONArray certs = new JSONArray();
        if (keystore != null) {
            Enumeration<String> aliases = keystore.aliases();
            while(aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if ("X.509".equals(keystore.getCertificate(alias).getType())) {
                    JSONObject cert = new JSONObject();
                    X509Certificate x509 = (X509Certificate)keystore.getCertificate(alias);
                    cert.put("alias", alias);
                    cert.put("rootca", BasicConstraints.getInstance(x509).isCA());
                    cert.put("subject", x509.getSubjectX500Principal().getName());
                    cert.put("expires", toISO(x509.getNotAfter()));
                    cert.put("data", formatCert(x509.getEncoded()));
                    certs.put(cert);
                }
            }
        }
        ssl.put("certificates", certs);

        return ssl;
    }

    public static String formatCert(byte[] encoding) {
        return "-----BEGIN CERTIFICATE-----\r\n" +
                new String(Base64.encodeBase64(encoding, true), StandardCharsets.UTF_8) +
                "-----END CERTIFICATE-----\r\n";
    }

    private static JSONObject libraries() throws JSONException {
        JSONObject libraries = new JSONObject();

        SortedMap<String,String> libs = SecurityInfo.getLibVersions();
        for(Map.Entry<String,String> entry : libs.entrySet()) {
            String version = entry.getValue();
            if (version == null) { version = "unknown"; }

            libraries.put(entry.getKey(), version);
        }

        return libraries;
    }


    private static String toISO(Date d) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        TimeZone tz = TimeZone.getTimeZone("UTC");
        df.setTimeZone(tz);
        return df.format(d);
    }

}
