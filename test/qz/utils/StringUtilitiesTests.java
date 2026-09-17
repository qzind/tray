package qz.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

public class StringUtilitiesTests {
    @Test
    public void testLegacyEncoding() {
        if(SystemUtilities.isWindows()) {
            Assert.assertEquals(
                    StringUtilities.getCharset("legacy"),
                    StandardCharsets.UTF_8);
        }

        // Verify that encoding "legacy" returns "windows-1252" or similar 8-bit encoding
        Assert.assertNotEquals(
                StringUtilities.getCharset("legacy"),
                StandardCharsets.UTF_8);
    }
}
