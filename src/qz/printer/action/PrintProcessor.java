package qz.printer.action;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.print.PrintException;
import java.awt.print.PrinterException;

public interface PrintProcessor {


    PrintingUtilities.Type getType();

    /**
     * Used to parse information passed from the web API for printing.
     *
     * @param printData JSON Array of printer data
     * @param options   Printing options to use for the print job
     */
    void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException;


    /**
     * Used to setup and send documents to the specified printing {@code service}.
     *
     * @param output  Destination used for printing
     * @param options Printing options to use for the print job
     */
    void print(PrintOutput output, PrintOptions options) throws PrintException, PrinterException;

    /**
     * Reset a processor back to it's initial state.
     */
    void cleanup();

}
