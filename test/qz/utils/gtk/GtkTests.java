package qz.utils.gtk;

import com.github.zafarkhaja.semver.Version;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import qz.utils.SystemUtilities;

public class GtkTests {
    @Test
    public void testGtkAvailability() {
        switch(SystemUtilities.getOs()) {
            case WINDOWS:
            case MAC:
                throw new SkipException("GTK is not needed on macOS or Windows");
        }
        Gtk GTK_INSTANCE = Gtk.getInstance(false);
        Assert.assertNotNull(GTK_INSTANCE);
        Assert.assertNotEquals(GTK_INSTANCE.getVersion(), Version.of(0));
    }
}
