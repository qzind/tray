package qz.ws;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.AboutInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * HTTP JSON endpoint for serving QZ Tray information
 */
public class HttpAboutServlet extends DefaultServlet {

    private static final Logger log = LoggerFactory.getLogger(PrintSocketServer.class);

    private static final int JSON_INDENT = 2;


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        if ("application/json".equals(request.getHeader("Accept"))) {
            generateJsonResponse(request, response);
        } else {
            generateHtmlResponse(request, response);
        }
    }

    private void generateHtmlResponse(HttpServletRequest request, HttpServletResponse response) {
        StringBuilder display = new StringBuilder();

        display.append("<html><body>")
                .append("<h1>About</h1>");

        display.append(newTable());
        try {
            JSONObject aboutData = AboutInfo.gatherAbout(request.getServerName());
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
