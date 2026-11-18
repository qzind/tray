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
import java.awt.print.PrinterAbortException;

public class PrintDialogTests {
    @Test
    public void testConfiguredCopies() throws Exception {
        PrintOptions.Pixel options = options(false);
        PrintRequestAttributeSet attributes = attributes(3);

        Assert.assertEquals(PrintPixel.getEffectiveCopies(options, attributes), 2);
    }

    @Test
    public void testAcceptedCopies() throws Exception {
        PrintOptions.Pixel options = options(true);
        PrintRequestAttributeSet attributes = attributes(3);

        Assert.assertEquals(PrintPixel.getEffectiveCopies(options, attributes), 3);
    }

    @Test
    public void testAccepted() throws Exception {
        PrintPixel.checkDialog(true);
    }

    @Test
    public void testCancelled() {
        Assert.expectThrows(PrinterAbortException.class, () -> PrintPixel.checkDialog(false));
    }

    private PrintOptions.Pixel options(boolean showDialog) throws Exception {
        JSONObject values = new JSONObject()
                .put("copies", 2)
                .put("showDialog", showDialog);
        return new PrintOptions(values, new PrintOutput(null), PrintingUtilities.Format.PDF).getPixelOptions();
    }

    private PrintRequestAttributeSet attributes(int copies) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new Copies(copies));
        return attributes;
    }
}
