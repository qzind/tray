package qz.ui.tray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.build.provision.params.Os;
import qz.ui.component.IconCache;
import qz.ui.tray.fallback.TaskBarIcon;
import qz.ui.tray.mac.MacTrayIcon;
import qz.ui.tray.windows.WindowsTrayIcon;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Supplier;

public class TrayWrapperTests {
    private static final Logger log = LogManager.getLogger(TrayWrapperTests.class);

    @DataProvider
    public Object[][] selections() {
        ArrayList<Object[]> trayConstructors = new ArrayList<>();
        for(Os os: Os.values()) {
            Supplier<Object> supplier = switch(os) {
                case WINDOWS -> WindowsTrayIcon::new;
                case MAC -> MacTrayIcon::new;
                // case LINUX -> LinuxTrayIcon::new;
                default -> TaskBarIcon::new;
            };
            trayConstructors.add(new Object[]{os, supplier});
        }
        return trayConstructors.toArray(new Object[trayConstructors.size()][]);
    }

    @Test(dataProvider = "selections")
    public void testSelection(Os os, Supplier<Object> supplier) {
        Object tray = supplier.get();
        log.debug("Os: {}, Tray: 'new {}()'", os.name(), tray.getClass().getSimpleName());
        switch(tray) {
            case TaskBarIcon taskBarIcon -> {
                Assert.assertNotNull(tray);
                try {
                    taskBarIcon.setIcon(IconCache.Icon.TRAY_READY);
                } catch(Throwable t) {
                    Assert.fail(t.toString());
                }
            }
            case TrayIcon trayIcon -> {
                Assert.assertNotNull(tray);
                try {
                    trayIcon.setImage(IconCache.getInstance().getImage(IconCache.Icon.TRAY_READY));
                } catch(Throwable t) {
                    Assert.fail(t.toString());
                }
            }
            default -> Assert.fail();
        }
    }
}