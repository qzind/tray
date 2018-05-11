package qz.ws;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP JSON endpoint for serving QZ Tray information
 */
public class HttpAboutServlet extends DefaultServlet {
    private static final Logger log = LoggerFactory.getLogger(PrintSocketServer.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String html = "<html>" +
                "<h1>" + Constants.ABOUT_TITLE + " " + Constants.VERSION + "</h1>" +
                "<ul>" +
                "<li><a href=\"//demo.qz.io\">demo page</a></li>" +
                "<li><a href=\"json/\">json data</a></li>" +
                "</ul>" +
                "</html>";

        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");
            response.getOutputStream().print(html);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.warn("Exception occurred loading html page {}", e.getMessage());
        }
    }
}
