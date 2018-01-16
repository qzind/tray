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

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class PrintDirect extends PrintRaw {

    private static final Logger log = LoggerFactory.getLogger(PrintDirect.class);

    private ArrayList<String> prints = new ArrayList<>();
    private ArrayList<PrintingUtilities.Format> formats = new ArrayList<>();


    @Override
    public PrintingUtilities.Type getType() {
        return PrintingUtilities.Type.DIRECT;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.optJSONObject(i);
            if (data == null) { continue; }

            prints.add(data.getString("data"));
            formats.add(PrintingUtilities.Format.valueOf(data.optString("format", "PLAIN").toUpperCase(Locale.ENGLISH)));
        }
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrintException {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new JobName(options.getRawOptions().getJobName(Constants.RAW_PRINT), Locale.getDefault()));

        for(int i = 0; i < prints.size(); i++) {
            DocPrintJob printJob = output.getPrintService().createPrintJob();
            InputStream stream = null;

            try {
                switch(formats.get(i)) {
                    case BASE64:
                        stream = new Base64InputStream(new ByteArrayInputStream(prints.get(i).getBytes("UTF-8")));
                        break;
                    case FILE:
                        stream = new DataInputStream(new URL(prints.get(i)).openStream());
                        break;
                    case PLAIN:
                    default:
                        stream = new ByteArrayInputStream(prints.get(i).getBytes("UTF-8"));
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
    }

    @Override
    public void cleanup() {
        prints.clear();
        formats.clear();
    }

}
