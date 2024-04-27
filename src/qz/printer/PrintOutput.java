package qz.printer;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.printer.info.NativePrinter;
import qz.utils.FileUtilities;

import javax.print.PrintService;
import javax.print.attribute.standard.Media;
import java.io.File;
import java.nio.file.Paths;

public class PrintOutput {

    private NativePrinter printer = null;

    private File file = null;

    private String host = null;
    private int port = -1;


    public PrintOutput(JSONObject configPrinter) throws JSONException, IllegalArgumentException {
        if (configPrinter == null) { return; }

        if (configPrinter.has("name")) {
            printer = PrintServiceMatcher.matchPrinter(configPrinter.getString("name"));
            if (printer == null) {
                throw new IllegalArgumentException("Cannot find printer with name \"" + configPrinter.getString("name") + "\"");
            }
        }

        if (configPrinter.has("file")) {
            String filename = configPrinter.getString("file");
            if (!FileUtilities.isGoodExtension(Paths.get(filename))) {
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
        return printer != null && printer.getPrintService() != null && !printer.getPrintService().isNull();
    }

    public PrintService getPrintService() {
        return printer.getPrintService().value();
    }

    public NativePrinter getNativePrinter() {
        return printer;
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

    public Media[] getSupportedMedia() {
        return (Media[])getPrintService().getSupportedAttributeValues(Media.class, null, null);
    }

}
