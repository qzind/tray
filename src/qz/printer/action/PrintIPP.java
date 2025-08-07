package qz.printer.action;

import de.gmuth.ipp.attributes.TemplateAttributes;
import de.gmuth.ipp.client.CupsClient;
import de.gmuth.ipp.client.IppClient;
import de.gmuth.ipp.client.IppJob;
import de.gmuth.ipp.client.IppPrinter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import qz.printer.action.ipp.Ipp;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.print.PrintException;
import java.awt.print.PrinterException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class PrintIPP implements PrintProcessor{
    private JSONArray data;
    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.IPP;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws UnsupportedOperationException {
        data = printData;
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrintException, PrinterException {
        URI requestedUri = output.getPrinterUri();

        IppClient ippClient = new IppClient();
        Ipp.ServerEntry serverEntry = output.getServer();
        CupsClient cupsClient = new CupsClient(serverEntry.serverUri, ippClient);

        // requestedUri is user provided, we must make sure it belongs to the claimed server
        if(!serverEntry.serverUri.getScheme().equals(requestedUri.getScheme()) ||
                !serverEntry.serverUri.getAuthority().equals(requestedUri.getAuthority())) {
            throw new PrinterException(serverEntry.serverUri + " Is not a printer of the server " + requestedUri);

        }

        //todo: this would also be a good time to raise a prompt

        IppPrinter ippPrinter = new IppPrinter(requestedUri.toString());

        // todo: match this to PrintServiceMatcher.getPrintersJSON syntax
        if (!serverEntry.uname.isEmpty() && !serverEntry.pass.isEmpty()) {
            cupsClient.basicAuth(serverEntry.uname, serverEntry.pass);
        }

        // todo: for testing, assume all data is just plaintext. There are a lot of things to discuss about filetype and format.

        IppJob job = ippPrinter.createJob(TemplateAttributes.jobName("test"));
        String dataString = null;
        try {
            dataString = data.getJSONObject(0).getString("data");
        }
        catch(JSONException e) {
            throw new RuntimeException(e);
        }

        byte[] dataBytes = dataString.getBytes(StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream(dataBytes)) {
            job.sendDocument(in);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
        // todo: I assume we wait?
        job.waitForTermination();
    }

    @Override
    public void cleanup() {

    }
}
