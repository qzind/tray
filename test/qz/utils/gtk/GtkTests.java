package qz.utils.gtk;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qz.utils.SystemUtilities;

import java.awt.*;

public class GtkTests {
    private static final Logger log = LogManager.getLogger(GtkTests.class);

    static final boolean INIT_GTK = !GraphicsEnvironment.isHeadless();
    static final Gtk GTK_INSTANCE = Gtk.getInstance(INIT_GTK);

    @BeforeClass
    public void filterOs() {
        switch(SystemUtilities.getOs()) {
            case WINDOWS:
            case MAC:
                throw new SkipException("GTK is not needed on macOS or Windows");
        }
    }

    @Test
    public void testGtkAvailability() {
        Assert.assertNotNull(GTK_INSTANCE);
    }

    @Test
    public void testGtkVersion() {
        Assert.assertNotNull(GTK_INSTANCE);
        Assert.assertNotEquals(GTK_INSTANCE.getVersion(), Version.of(0));
    }

    @Test
    public void testGtkGetScaleFactor() {
        Assert.assertNotNull(GTK_INSTANCE);
        log.debug("GTK {} scale factor: {}", GTK_INSTANCE.getVersion(), GTK_INSTANCE.getScaleFactor());
        Assert.assertNotEquals(GTK_INSTANCE.getScaleFactor(), 0);
    }
}
