package qz.ws;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.FilterHolder;
import qz.common.AboutInfo;
import qz.installer.certificate.CertificateManager;
import qz.utils.ByteUtilities;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import static qz.common.Constants.*;
import static qz.ui.component.IconCache.*;
import static qz.ui.component.IconCache.Icon.*;

/**
 * HTTP JSON endpoint for serving QZ Tray information
 */
public class HttpAboutServlet extends DefaultServlet {
    private static final Logger log = LogManager.getLogger(HttpAboutServlet.class);
    private static final int JSON_INDENT = 2;

    private static final String FAVICON = String.format("data:image/png;base64,%s", ByteUtilities.imageToBase64(LOGO_ICON, "png"));
    // TODO: Remove when portal adds "BRAND_COLOR_HEX" value
    private static final String BRAND_COLOR = IS_REBRANDED ? getHtmlColorFromIcon(ABOUT_ICON, BRAND_COLOR_HEX) : BRAND_COLOR_HEX;
    private static final int SALT_LENGTH_RESTART = ThreadLocalRandom.current().nextInt(12, 31);

    private final CertificateManager certificateManager;
    private final String allowOrigin;

    public HttpAboutServlet(CertificateManager certificateManager, String allowOrigin) {
        this.certificateManager = certificateManager;
        this.allowOrigin = allowOrigin;
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", allowOrigin);
        if ("application/json".equals(request.getHeader("Accept")) || "/json".equals(request.getServletPath())) {
            generateJsonResponse(request, response);
        } else if ("application/x-x509-ca-cert".equals(request.getHeader("Accept")) || request.getServletPath().startsWith("/cert/")) {
            generateCertResponse(request, response);
        } else {
            generateHtmlResponse(request, response);
        }
    }

    private void generateHtmlResponse(HttpServletRequest request, HttpServletResponse response) {
        // Handle the "Restart required" prompt
        if(interceptRestartRequest(request, response)) {
            return;
        }

        StringBuilder display = new StringBuilder();

        display.append("<html>")
                .append("<head><meta charset=\"UTF-8\"></head>")
                .append("<body>")
                .append("<h1>About</h1>");

        display.append(newTable());

        JSONObject aboutData = AboutInfo.gatherAbout(request.getServerName(), certificateManager);
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(!"/restart".equals(request.getServletPath())) {
            return;
        }

        String pid = request.getParameter("pid");
        String challenge = request.getParameter("challenge");

        try {
            if(SystemUtilities.validatePidChallenge(pid, challenge, SALT_LENGTH_RESTART)) {
                int pidNum = Integer.parseInt(pid);
                // TODO: Actually close the browser when the button is clicked
                response.setContentType("text/html");
                String message = String.format("Challenge accepted, closing pid=%d... (not really)", pidNum);
                response.getWriter().println("<html><script>alert('" + message + "');</script></html>");
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }
        } catch(NumberFormatException e) {
            log.error("Invalid pid '{}' provided for restart", pid, e);
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        log.warn("Challenge '{}' denied", challenge);
    }

    private boolean interceptRestartRequest(HttpServletRequest request, HttpServletResponse response) {
        if(!"/restart".equals(request.getServletPath())) {
            return false;
        }

        String pid = request.getParameter("pid");
        if(pid == null) {
            pid = "-1";
        }

        try {
            HashMap<String,String> fieldMap = new HashMap<>();
            fieldMap.put("%FAVICON%", FAVICON);
            fieldMap.put("%RESTART_TITLE%", IS_REBRANDED ? "Oh Snap" : "Oh Sheet");
            fieldMap.put("%RESTART_PID%", pid);
            fieldMap.put("%BRAND_COLOR%", BRAND_COLOR);
            fieldMap.put("%RESTART_CHALLENGE%", SystemUtilities.calculatePidChallenge(pid, SALT_LENGTH_RESTART));
            fieldMap.put("%RESTART_SVG%", FileUtilities.readSvgAsset(getClass(),"resources/restart-graphic.svg"));
            String display = FileUtilities.configureAssetToString(getClass(),"resources/restart-required.html.in", fieldMap);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");
            response.getOutputStream().print(display);
        } catch(IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.warn("Exception occurred loading html restart page {}", e.getMessage());
        }
        return true;
    }

    private void generateJsonResponse(HttpServletRequest request, HttpServletResponse response) {
        JSONObject aboutData = AboutInfo.gatherAbout(request.getServerName(), certificateManager);

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
            String certData = AboutInfo.formatCert(certificateManager.getKeyPair(alias).getCert().getEncoded());

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
        return String.format("<tr><th id='%s' colspan='99'>%s</th></tr>", title, title);
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

    /**
     * Support for preflight header filters per https://wicg.github.io/private-network-access/
     * - Origin filter
     * - Private-network check
     */
    public static FilterHolder originFilter(String allowOrigin) {
        return new FilterHolder((servletRequest, servletResponse, filterChain) -> {
            HttpServletResponse response = (HttpServletResponse)servletResponse;
            HttpServletRequest request = (HttpServletRequest)servletRequest;
            response.setHeader("Access-Control-Allow-Origin", allowOrigin);
            if("true".equals(request.getHeader("Access-Control-Request-Private-Network"))) {
                // Only add header if it was specified by the browser
                response.setHeader("Access-Control-Allow-Private-Network", "true");
            }
            filterChain.doFilter(request, response);
        });
    }

}
