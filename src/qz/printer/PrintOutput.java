package qz.printer;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.FileUtilities;

import javax.print.PrintService;
import java.io.File;

public class PrintOutput {

    private PrintService service = null;

    private File file = null;

    private String host = null;
    private int port = -1;


    public PrintOutput(JSONObject configPrinter) throws JSONException, IllegalArgumentException {
        if (configPrinter == null) { return; }

        if (configPrinter.has("name")) {
            service = PrintServiceMatcher.matchService(configPrinter.getString("name"));
            if (service == null) {
                throw new IllegalArgumentException("Cannot find printer with name \"" + configPrinter.getString("name") + "\"");
            }
        }

        if (configPrinter.has("file")) {
            String filename = configPrinter.getString("file");
            if (FileUtilities.isBadExtension(filename)) {
                throw new IllegalArgumentException("Writing to file \"" + filename + "\" is denied for security reasons. (Prohibited file extension)");
            } else if (FileUtilities.isBadPath(filename)) {
                throw new IllegalArgumentException("Writing to file \"" + filename + "\" is denied for security reasons. (Prohibited directory name)");
            } else {
                file = new File(filename);
            }
        }

        if (configPrinter.has("host")) {
            host = configPrinter.getString("host");
            port = configPrinter.optInt("port", 9100); // default to port 9100 (HP/JetDirect standard) if not provided
        }

        //at least one method must be set for printing
        if (!isSetService() && !isSetFile() && !isSetHost()) {
            throw new IllegalArgumentException("No printer output has been specified");
        }
    }


    public boolean isSetService() {
        return service != null;
    }

    public PrintService getPrintService() {
        return service;
    }

    public boolean isSetFile() {
        return file != null;
    }

    public File getFile() {
        return file;
    }

    public boolean isSetHost() {
        return host != null;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

}
