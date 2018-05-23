package qz.ws;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.AboutInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP JSON endpoint for serving QZ Tray information
 */
public class JsonAboutServlet extends DefaultServlet {

    private static final Logger log = LoggerFactory.getLogger(PrintSocketServer.class);

    private static final int JSON_INDENT = 2;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JSONObject aboutData = AboutInfo.gatherAbout(request.getServerName());

        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getOutputStream().write(aboutData.toString(JSON_INDENT).getBytes("UTF-8"));
        }
        catch(Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.warn("Exception occurred writing JSONObject {}", aboutData);
        }
    }

}
