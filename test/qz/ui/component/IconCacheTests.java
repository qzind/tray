package qz.ui.component;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static qz.ui.component.IconCache.Icon.*;

public class IconCacheTests {
    private IconCache iconCache;

    @BeforeMethod
    public void setUp() {
        iconCache = new IconCache();
    }

    @Test
    public void testCacheBuilds() {
        Assert.assertNotNull(iconCache);
    }

    @Test
    public void testRepresentativeResourcesLoad() {
        Assert.assertNotNull(iconCache.getImage(LOGO_ICON), "Logo icon should load");
        Assert.assertNotNull(iconCache.getImage(DEFAULT_ICON), "Tray icon should load");
        Assert.assertNotNull(iconCache.getImage(DEFAULT_MASK_ICON), "Tray mask icon should load");
        Assert.assertNotNull(iconCache.getImage(ABOUT_ICON), "Menu icon should load");
        Assert.assertNotNull(iconCache.getImage(TRUST_VERIFIED_ICON), "Dialog icon should load");
    }
}
