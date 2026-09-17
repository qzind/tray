package qz.ui.component;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
}
