package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LinuxUtilitiesTests {
    static Logger log = LogManager.getLogger(LinuxUtilitiesTests.class);

    @Test
    public void testGetScaleFactor() {
        double scaleFactor = CompositorType.getBestScaleFactor(false);
        log.info("Detected scale factor {}x from {}", scaleFactor, CompositorType.getCompositors());
        Assert.assertNotEquals(scaleFactor, 0.0);
    }
}
