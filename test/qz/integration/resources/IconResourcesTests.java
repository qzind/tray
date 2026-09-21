package qz.integration.resources;

import org.testng.Assert;
import org.testng.annotations.Test;
import qz.ui.component.IconCache;

public class IconResourcesTests {
    /**
     * Relative paths (e.g. ../resources) can unobviously pass unit tests and then fail integration tests
     * Class.getResourceAsStream() handles relative paths containing <code>..</code> differently inside a packaged
     * JAR compared to an IDE.
     */
    @Test
    public void testIconCache() {
        try {
            for(IconCache.Icon icon : IconCache.Icon.values()) {
                for(boolean isDark : new boolean[]{true, false}) {
                    Assert.assertNotNull(IconCache.getInstance().getIcon(icon, isDark));
                }
            }
        } catch(RuntimeException e) {
            Assert.fail(e.getMessage());
        }
    }
}
