package qz.printer;

import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import qz.utils.PrintingUtilities;

public class PrintOptionsTests {
    @Test
    public void testPdfInherit() throws Exception {
        PrintOptions printOptions = createSizeOptions("inherit", PrintingUtilities.Format.PDF);

        Assert.assertNotNull(printOptions.getPixelOptions().getSize());
        Assert.assertTrue(printOptions.getPixelOptions().getSize().isInherited());
    }

    @Test
    public void testExplicitSize() throws Exception {
        JSONObject size = new JSONObject()
                .put("width", 4)
                .put("height", 6);
        PrintOptions printOptions = createSizeOptions(size, PrintingUtilities.Format.PDF);

        Assert.assertFalse(printOptions.getPixelOptions().getSize().isInherited());
        Assert.assertEquals(printOptions.getPixelOptions().getSize().getWidth(), 4.0);
        Assert.assertEquals(printOptions.getPixelOptions().getSize().getHeight(), 6.0);
    }

    @Test
    public void testNoSize() throws Exception {
        PrintOptions printOptions = new PrintOptions(new JSONObject(), new PrintOutput(null), PrintingUtilities.Format.PDF);

        Assert.assertNull(printOptions.getPixelOptions().getSize());
    }

    @Test
    public void testImageInherit() throws Exception {
        PrintOptions printOptions = createSizeOptions("inherit", PrintingUtilities.Format.IMAGE);

        // size: "inherit" is PDF-only
        Assert.assertNull(printOptions.getPixelOptions().getSize());
    }

    @Test
    public void testCommandInherit() throws Exception {
        PrintOptions printOptions = createSizeOptions("inherit", PrintingUtilities.Format.COMMAND);

        // size: "inherit" is PDF-only
        Assert.assertNull(printOptions.getPixelOptions().getSize());
    }

    @Test
    public void testUpperInherit() throws Exception {
        PrintOptions printOptions = createSizeOptions("INHERIT", PrintingUtilities.Format.PDF);

        // size: "inherit" is case-sensitive
        Assert.assertNull(printOptions.getPixelOptions().getSize());
    }

    @Test
    public void testInvalidSize() throws Exception {
        PrintOptions printOptions = createSizeOptions("letter", PrintingUtilities.Format.PDF);

        Assert.assertNull(printOptions.getPixelOptions().getSize());
    }

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

    private PrintOptions createSizeOptions(Object size, PrintingUtilities.Format format) throws Exception {
        JSONObject options = new JSONObject().put("size", size);
        return new PrintOptions(options, new PrintOutput(null), format);
    }
}
