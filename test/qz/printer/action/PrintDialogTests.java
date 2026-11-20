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
    
    private PrintOptions.Pixel options(int copies) throws Exception {
        return new PrintOptions(new JSONObject().put("copies", copies), new PrintOutput(null), PrintingUtilities.Format.PDF).getPixelOptions();
    }

    private PrintRequestAttributeSet attributes(int copies) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new Copies(copies));
        return attributes;
    }
}
