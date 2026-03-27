package qz.utils;

import org.testng.Assert;
import org.testng.annotations.Test;
import qz.common.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileUtilitiesTests {
    @Test
    public void testConfigureAssetToFile() throws IOException {
        File tempFile = File.createTempFile("configured-asset-", ".ini");
        FileUtilities.configureAssetToFile(
                this.getClass(),
                "resources/configurable-asset.ini.in",
                new HashMap<>(Map.of("%SAMPLE_DATA%", "7890")),
                tempFile
        );

        Properties props = new Properties();
        props.load(new FileInputStream(tempFile));
        Assert.assertEquals(props.get("about_title"), Constants.ABOUT_TITLE);
        Assert.assertEquals(props.get("sample_data"), "7890");
        Assert.assertEquals(props.get("static_data"), "Static data");
    }
}
