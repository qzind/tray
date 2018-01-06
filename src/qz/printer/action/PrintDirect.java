package qz.printer.action;

import org.apache.commons.codec.binary.Base64InputStream;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import java.io.*;
import java.net.URL;
import java.util.Locale;

public class PrintDirect extends PrintRaw {

    private static final Logger log = LoggerFactory.getLogger(PrintDirect.class);

    private PrintingUtilities.Format format;
    private String command;


    @Override
    public PrintingUtilities.Type getType() {
        return PrintingUtilities.Type.DIRECT;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        if (printData.length() > 1) {
            log.warn("Direct print only supports single data entries for printing");
        }

        JSONObject data = printData.optJSONObject(0);
        if (data == null) { return; }

        format = PrintingUtilities.Format.valueOf(data.optString("format", "PLAIN").toUpperCase(Locale.ENGLISH));
        command = data.getString("data");
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrintException {
        DocPrintJob printJob = output.getPrintService().createPrintJob();

        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new JobName(options.getRawOptions().getJobName(Constants.RAW_PRINT), Locale.getDefault()));

        InputStream stream = null;

        try {
            switch(format) {
                case BASE64:
                    stream = new Base64InputStream(new ByteArrayInputStream(command.getBytes("UTF-8")));
                    break;
                case FILE:
                    stream = new DataInputStream(new URL(command).openStream());
                    break;
                case PLAIN:
                default:
                    stream = new ByteArrayInputStream(command.getBytes("UTF-8"));
                    break;
            }

            SimpleDoc doc = new SimpleDoc(stream, DocFlavor.INPUT_STREAM.AUTOSENSE, null);

            waitForPrint(printJob, doc, attributes);
        }
        catch(IOException e) {
            throw new PrintException(e);
        }
        finally {
            if (stream != null) {
                try { stream.close(); } catch(Exception ignore) {}
            }
        }
    }

    @Override
    public void cleanup() {
        command = null;
    }

}
