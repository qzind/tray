package qz.common;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.ssl.Base64;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.certificate.KeyPairWrapper;
import qz.installer.certificate.CertificateManager;
import qz.utils.StringUtilities;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

public class AboutInfo {

    private static final Logger log = LogManager.getLogger(AboutInfo.class);

    private static String preferredHostname = "localhost";

    public static JSONObject gatherAbout(String domain, CertificateManager certificateManager) {
        JSONObject about = new JSONObject();

        try {
            about.put("product", product());
            about.put("socket", socket(certificateManager, domain));
            about.put("environment", environment());
            about.put("ssl", ssl(certificateManager));
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

    private static JSONObject socket(CertificateManager certificateManager, String domain) throws JSONException {
        JSONObject socket = new JSONObject();
        String sanitizeDomain = StringUtilities.escapeHtmlEntities(domain);

        // Gracefully handle XSS per https://github.com/qzind/tray/issues/1099
        if(sanitizeDomain.contains("&lt;") || sanitizeDomain.contains("&gt;")) {
            log.warn("Something smells fishy about this domain: \"{}\", skipping", domain);
            sanitizeDomain = "unknown";
        }

        socket
                .put("domain", sanitizeDomain)
                .put("secureProtocol", "wss")
                .put("securePort", certificateManager.isSslActive() ? PrintSocketServer.getSecurePortInUse() : "none")
                .put("insecureProtocol", "ws")
                .put("insecurePort", PrintSocketServer.getInsecurePortInUse());

        return socket;
    }

    private static JSONObject environment() throws JSONException {
        JSONObject environment = new JSONObject();

        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        environment
                .put("os", SystemUtilities.getOsDisplayName())
                .put("os version", SystemUtilities.getOsDisplayVersion())
                .put("java", String.format("%s (%s)", Constants.JAVA_VERSION, SystemUtilities.getJreArch().toString().toLowerCase()))
                .put("java (location)", System.getProperty("java.home"))
                .put("java (vendor)", Constants.JAVA_VENDOR)
                .put("uptime", DurationFormatUtils.formatDurationWords(uptime, true, false))
                .put("uptimeMillis", uptime);

        return environment;
    }

    private static JSONObject ssl(CertificateManager certificateManager) throws JSONException, CertificateEncodingException {
        JSONObject ssl = new JSONObject();

        JSONArray certs = new JSONArray();

        for (KeyPairWrapper keyPair : new KeyPairWrapper[]{certificateManager.getCaKeyPair(), certificateManager.getSslKeyPair() }) {
            X509Certificate x509 = keyPair.getCert();
            if (x509 != null) {
                JSONObject cert = new JSONObject();
                cert.put("alias", keyPair.getAlias());
                try {
                    ASN1Primitive ext = X509ExtensionUtil.fromExtensionValue(x509.getExtensionValue(Extension.basicConstraints.getId()));
                    cert.put("rootca", BasicConstraints.getInstance(ext).isCA());
                }
                catch(IOException | NullPointerException e) {
                    cert.put("rootca", false);
                }
                cert.put("subject", x509.getSubjectX500Principal().getName());
                cert.put("expires", SystemUtilities.toISO(x509.getNotAfter()));
                cert.put("data", formatCert(x509.getEncoded()));
                certs.put(cert);
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

    public static String getPreferredHostname() {
        return preferredHostname;
    }

    public static Version findLatestVersion() {
        log.trace("Looking for newer versions of {} online", Constants.ABOUT_TITLE);
        try {
            URL api = new URL(Constants.VERSION_CHECK_URL);
            BufferedReader br = new BufferedReader(new InputStreamReader(api.openStream()));

            StringBuilder rawJson = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) {
                rawJson.append(line);
            }

            JSONArray versions = new JSONArray(rawJson.toString());
            for(int i = 0; i < versions.length(); i++) {
                JSONObject versionData = versions.getJSONObject(i);
                if(versionData.getString("target_commitish").equals("master")) {
                    Version latestVersion = Version.valueOf(versionData.getString("name"));
                    log.trace("Found latest version of {} online: {}", Constants.ABOUT_TITLE, latestVersion);
                    return latestVersion;
                }
            }
            throw new Exception("Could not find valid json version information online.");
        }
        catch(Exception e) {
            log.error("Failed to get latest version of {} online", Constants.ABOUT_TITLE, e);
        }

        return Constants.VERSION;
    }

}
