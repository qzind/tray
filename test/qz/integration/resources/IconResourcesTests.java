package qz.integration.resources;

import org.testng.Assert;
import org.testng.annotations.Test;
import qz.ui.component.IconCache;

public class IconResourcesTests {
    /**
     * Try to detect unobvious pitfalls when dealing with embedded resources
     * <ul>
     *  <li>First, <code>Class.getResourceAsStream()</code> handles relative paths containing <code>..</code>
     *  differently inside a JAR vs IDE.</li>
     *  <li>Second, <code>Path</code> calculations on Windows will process as <code>resources\foo.svg</code>
     *  instead of<code>resources/foo.svg</code></li>
     * </ul>
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
