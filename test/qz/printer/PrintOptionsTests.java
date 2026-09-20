package qz.printer;

import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import qz.utils.PrintingUtilities;

public class PrintOptionsTests {
    @Test
    public void testLegacyEncodingStringForm() throws Exception {
        JSONObject options = new JSONObject().put("encoding", "legacy");
        PrintOptions printOptions = new PrintOptions(options, new PrintOutput(null), PrintingUtilities.Format.COMMAND);

        Assert.assertNotNull(printOptions.getRawOptions().getDestEncoding());
    }

    @Test
    public void testLegacyEncodingObjectForm() throws Exception {
        JSONObject encoding = new JSONObject()
                .put("from", "legacy")
                .put("to", "legacy");
        JSONObject options = new JSONObject().put("encoding", encoding);
        PrintOptions printOptions = new PrintOptions(options, new PrintOutput(null), PrintingUtilities.Format.COMMAND);

        Assert.assertNotNull(printOptions.getRawOptions().getSrcEncoding());
        Assert.assertNotNull(printOptions.getRawOptions().getDestEncoding());
    }
}
