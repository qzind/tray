package qz.ws;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.utils.SystemUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP JSON endpoint for serving QZ Tray information
 */
public class JsonAboutServlet extends DefaultServlet {
    private static final Logger log = LoggerFactory.getLogger(PrintSocketServer.class);
    private static int JSON_INDENT = 3;

    KeyStore keystore;
    JSONObject data;
    AtomicInteger securePort;
    AtomicInteger insecurePort;

    public JsonAboutServlet(Properties props, AtomicInteger securePort, AtomicInteger insecurePort) {
        this.data = new JSONObject();
        this.keystore = getKeyStore(props);
        this.insecurePort = insecurePort;
        this.securePort = securePort;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        put("product", getProductDetails());
        put("environment", getEnvironmentDetails());
        put("ssl", getSslDetails(keystore));

        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getOutputStream().write(data.toString(JSON_INDENT).getBytes("UTF-8"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.warn("Exception occurred writing JSONObject {}", data);
        }
    }

    /**
     * Fetches the keystore used for SSL connections
     */
    private static KeyStore getKeyStore(Properties props) {
        if (props != null) {
            String store = props.getProperty("wss.keystore", "");
            char[] pass = props.getProperty("wss.storepass", "").toCharArray();
            try {
                KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                keystore.load(new FileInputStream(store), pass);
                return keystore;
            }
            catch(KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
                log.warn("Unable to create keystore from properties file: {}", e.getMessage());
            }
        }
        return null;
    }

    private static JSONArray getSslDetails(KeyStore keystore) {
        JSONArray certs = new JSONArray();
        if (keystore != null) {
            try {
                Enumeration<String> aliases = keystore.aliases();
                while(aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if (keystore.getCertificate(alias).getType().equals("X.509")) {
                        JSONObject cert = new JSONObject();
                        X509Certificate x509 = (X509Certificate)keystore.getCertificate(alias);
                        cert.put("subject", x509.getSubjectX500Principal().getName());
                        cert.put("expires", toISO(x509.getNotAfter()));
                        certs.put(cert);
                    }
                }
            } catch(JSONException | KeyStoreException e) {
                log.warn("Unable to determine certificate information from keystore: {}", e.getMessage());
            }
        }
        return certs;
    }

    private JSONObject getProductDetails() {
        JSONObject info = new JSONObject();
        try {
            info.put("title", Constants.ABOUT_TITLE)
                    .put("version", Constants.VERSION)
                    .put("vendor", Constants.ABOUT_COMPANY)
                    .put("url", Constants.ABOUT_URL)
                    .put("wss", keystore == null ? "none" : PrintSocketServer.SECURE_PORTS.get(securePort.get()))
                    .put("ws", PrintSocketServer.INSECURE_PORTS.get(insecurePort.get()));
        } catch(JSONException e) {
            log.warn("JSON exception occurred: {}", e.getMessage());
        }
        return info;
    }

    private JSONObject getEnvironmentDetails() {
        JSONObject info = new JSONObject();
        try {
            info.put("os", SystemUtilities.getOS())
                    .put("java", Constants.JAVA_VERSION);
        } catch (JSONException e) {
            log.warn("JSON exception occurred: {}", e.getMessage());
        }
        return info;
    }

    private void put(String s, JSONArray o) {
        try {
            data.put(s, o);
        } catch (Exception e) {
            log.warn("Exception occurred putting JSONArray {} {}", s, o);
        }
    }

    private void put(String s, JSONObject o) {
        try {
            data.put(s, o);
        } catch (Exception e) {
            log.warn("Exception occurred putting JSONObject {} {}", s, o);
        }
    }

    private static String toISO(Date d) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        TimeZone tz = TimeZone.getTimeZone("UTC");
        df.setTimeZone(tz);
        return df.format(d);
    }
}
