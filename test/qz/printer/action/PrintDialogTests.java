package qz.printer.action;

import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;

public class PrintDialogTests {
    @Test
    public void testSyncedCopies() throws Exception {
        PrintOptions.Pixel options = options(2);
        PrintRequestAttributeSet attributes = attributes(3);
        options.sync(attributes);

        Assert.assertEquals(options.getCopies(), 3);
    }

    @Test
    public void testRawUnsupported() throws Exception {
        PrintOptions options = dialogOptions(PrintingUtilities.Format.COMMAND);

        Assert.expectThrows(UnsupportedOperationException.class,
                () -> new PrintRaw().print(null, options));
    }

    @Test
    public void testDirectUnsupported() throws Exception {
        PrintOptions options = dialogOptions(PrintingUtilities.Format.DIRECT);

        Assert.expectThrows(UnsupportedOperationException.class,
                () -> new PrintDirect().print(null, options));
    }
    
    private PrintOptions.Pixel options(int copies) throws Exception {
        return new PrintOptions(new JSONObject().put("copies", copies), new PrintOutput(null), PrintingUtilities.Format.PDF).getPixelOptions();
    }

    private PrintOptions dialogOptions(PrintingUtilities.Format format) throws Exception {
        JSONObject options = new JSONObject().put("showDialog", true);
        return new PrintOptions(options, new PrintOutput(null), format);
    }

    private PrintRequestAttributeSet attributes(int copies) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new Copies(copies));
        return attributes;
    }
}
