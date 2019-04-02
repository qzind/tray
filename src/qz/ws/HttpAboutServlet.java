package qz.ws;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.AboutInfo;
import qz.deploy.DeployUtilities;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.Properties;

/**
 * HTTP JSON endpoint for serving QZ Tray information
 */
public class HttpAboutServlet extends DefaultServlet {

    private static final Logger log = LoggerFactory.getLogger(PrintSocketServer.class);

    private static final int JSON_INDENT = 2;


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        if ("application/json".equals(request.getHeader("Accept")) || "/json".equals(request.getServletPath())) {
            generateJsonResponse(request, response);
        } else if ("application/x-x509-ca-cert".equals(request.getHeader("Accept")) || request.getServletPath().startsWith("/cert/")) {
            generateCertResponse(request, response);
        } else {
            generateHtmlResponse(request, response);
        }
    }

    private void generateHtmlResponse(HttpServletRequest request, HttpServletResponse response) {
        StringBuilder display = new StringBuilder();

        display.append("<html><body>")
                .append("<h1>About</h1>");

        display.append(newTable());

        JSONObject aboutData = AboutInfo.gatherAbout(request.getServerName());
        try {
            display.append(generateFromKeys(aboutData, true));
        }
        catch(JSONException e) {
            log.error("Failed to read JSON data", e);
            display.append("<tr><td>Failed to write information</td></tr>");
        }
        display.append("</table>");

        display.append("</body></html>");

        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");
            response.getOutputStream().print(display.toString());
        }
        catch(Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.warn("Exception occurred loading html page {}", e.getMessage());
        }
    }

    private void generateJsonResponse(HttpServletRequest request, HttpServletResponse response) {
        JSONObject aboutData = AboutInfo.gatherAbout(request.getServerName());

        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getOutputStream().write(aboutData.toString(JSON_INDENT).getBytes(StandardCharsets.UTF_8));
        }
        catch(Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.warn("Exception occurred writing JSONObject {}", aboutData);
        }
    }

    private void generateCertResponse(HttpServletRequest request, HttpServletResponse response) {
        try {
            String alias = request.getServletPath().split("/")[2];
            String certData = loadCertificate(alias);

            if (certData != null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/x-x509-ca-cert");

                response.getOutputStream().print(certData);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getOutputStream().print("Could not find certificate with alias \"" + alias + "\" to download.");
            }
        }
        catch(Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.warn("Exception occurred loading certificate: {}", e.getMessage());
        }
    }

    private String loadCertificate(String alias) throws GeneralSecurityException, IOException {
        Properties sslProps = DeployUtilities.loadTrayProperties();

        if (sslProps != null) {
            KeyStore jks = KeyStore.getInstance("jks");
            jks.load(new FileInputStream(new File(sslProps.getProperty("wss.keystore"))), sslProps.getProperty("wss.storepass").toCharArray());

            if (jks.containsAlias(alias)) {
                return AboutInfo.formatCert(jks.getCertificate(alias).getEncoded());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private StringBuilder generateFromKeys(JSONObject obj, boolean printTitle) throws JSONException {
        StringBuilder rows = new StringBuilder();

        Iterator itr = obj.keys();
        while(itr.hasNext()) {
            String key = (String)itr.next();

            if (printTitle) {
                rows.append(titleRow(key));
            }

            if (obj.optJSONObject(key) != null) {
                rows.append(generateFromKeys(obj.getJSONObject(key), false));
            } else {
                if ("data".equals(key)) { //special case - replace with a "Download" button
                    obj.put(key, "<a href='/cert/" + obj.optString("alias") + "'>Download certificate</a>");
                }
                rows.append(contentRow(key, obj.get(key)));
            }
        }

        return rows;
    }


    private String newTable() {
        return "<table border='1' cellspacing='0' cellpadding='5'>";
    }

    private String titleRow(String title) {
        return "<tr><th colspan='99'>" + title + "</th></tr>";
    }

    private String contentRow(String key, Object value) throws JSONException {
        if (value instanceof JSONArray) {
            return contentRow(key, (JSONArray)value);
        } else {
            return contentRow(key, String.valueOf(value));
        }
    }

    private String contentRow(String key, JSONArray value) throws JSONException {
        StringBuilder valueCell = new StringBuilder();
        for(int i = 0; i < value.length(); i++) {
            if (value.optJSONObject(i) != null) {
                valueCell.append(newTable());
                valueCell.append(generateFromKeys(value.getJSONObject(i), false));
                valueCell.append("</table>");
            } else {
                valueCell.append(value.getString(i)).append("<br/>");
            }
        }

        return contentRow(key, valueCell.toString());
    }

    private String contentRow(String key, String value) {
        return "<tr><td>" + key + "</td> <td>" + value + "</td></tr>";
    }

}
