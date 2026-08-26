package qz.utils;

import com.github.zafarkhaja.semver.Version;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class GtkUtilitiesTests {
    @Test
    public void testGtkAvailability() {
        switch(SystemUtilities.getOs()) {
            case WINDOWS:
            case MAC:
                throw new SkipException("GTK is not needed on macOS or Windows");
        }
        Assert.assertNotNull(GtkUtilities.GTK_INSTANCE);
        Assert.assertNotEquals(GtkUtilities.GTK_INSTANCE.getVersion(), Version.of(0));
    }
}
