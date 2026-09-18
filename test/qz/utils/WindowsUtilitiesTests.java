package qz.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.*;
import static qz.utils.WindowsUtilities.*;

public class WindowsUtilitiesTests {
    private static final Logger log = LogManager.getLogger(WindowsUtilitiesTests.class);

    @BeforeClass
    public void setup() {
        if(!SystemUtilities.isWindows()) {
            throw new SkipException("Only run tests on Windows");
        }
    }

    @Test
    public void testGetSystemAnsiCodePage() {
        int codePage = WindowsUtilities.getSystemAnsiCodePage();
        Assert.assertTrue(codePage > 0); // -1 is error
        Assert.assertTrue(codePage != 65001); // 65001 is UTF8
    }

    @Test
    public void testGetCharsetFromCodePage() {
        // Ensure proper fallback for bad charsets
        Assert.assertEquals(getCharsetFromCodePage(-1), UTF_8); // -1 fallback to UTF8
        Assert.assertEquals(getCharsetFromCodePage(Integer.MAX_VALUE), UTF_8); // missing fallback to UTF8
        Assert.assertEquals(getCharsetFromCodePage(Integer.MAX_VALUE), UTF_8); // missing fallback to UTF8

        // Ensure no fallback for good charsets
        Assert.assertEquals(getCharsetFromCodePage(932), Charset.forName("windows-932")); // Windows Japanese
        Assert.assertEquals(getCharsetFromCodePage(950), Charset.forName("windows-950")); // Windows Traditional Chinese
        Assert.assertEquals(getCharsetFromCodePage(1252), Charset.forName("cp1252")); // Windows Latin-1
        Assert.assertEquals(getCharsetFromCodePage(1253), Charset.forName("cp1253")); // Windows Greek
    }
}
