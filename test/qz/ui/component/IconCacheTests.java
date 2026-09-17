package qz.ui.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qz.utils.SystemUtilities;

import javax.swing.ImageIcon;

import static qz.ui.component.IconCache.*;
import static qz.ui.component.IconCache.Icon.*;

public class IconCacheTests {
    private static final Logger log = LogManager.getLogger(IconCacheTests.class);

    IconCache iconCache;

    @BeforeClass
    public void setUp() {
        Assert.assertNotNull(iconCache = IconCache.getInstance());
    }

    @Test
    public void checkBuiltIns() {
        for(Icon icon : Icon.values()) {
            for(int size : icon.getSizes()) {
                int expectedSize = accountForPadding(icon, size);

                for(Theme theme : Theme.values()) {
                    ImageIcon imageIcon = iconCache.getIcon(icon, theme, size);
                    log.info("Checking IconCache entry for {} '{}': Expected: '{}', Actual: '{}x{}'",
                             icon, icon.getFileName(theme, size), expectedSize,
                             imageIcon.getIconWidth(), imageIcon.getIconHeight());

                    Assert.assertEquals(imageIcon.getIconHeight(), expectedSize);
                }
            }
        }
    }

    /**
     * macOS tray icons require 25% padding
     */
    private int accountForPadding(Icon icon, int size) {
        if(icon.getType() == Type.SYSTEM_TRAY && SystemUtilities.isMac()) {
            return size + (size / 4); // handle 25% padding
        }
        return size;
    }
}
