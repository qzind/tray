package qz.utils;

import com.github.zafarkhaja.semver.Version;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static qz.utils.GtkUtilities.*;

public class GtkUtilitiesTests {
    @Test
    public void testGtkAvailability() {
        switch(SystemUtilities.getOs()) {
            case WINDOWS:
            case MAC:
                throw new SkipException("GTK is not needed on macOS or Windows");
        }
        GTK GTK_INSTANCE = GTK.getInstance(false);
        Assert.assertNotNull(GTK_INSTANCE);
        Assert.assertNotEquals(GTK_INSTANCE.getVersion(), Version.of(0));
    }
}
