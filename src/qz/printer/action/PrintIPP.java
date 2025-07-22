package qz.printer.action;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.print.PrintException;
import java.awt.print.PrinterException;

public class PrintIPP implements PrintProcessor{
    private JSONArray data;
    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.IPP;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        data = printData;
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrintException, PrinterException {

    }

    @Override
    public void cleanup() {

    }
}
