package qz.integration.resources;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.ui.component.IconCache;
import qz.ui.component.iconcache.Cache;
import qz.ui.component.iconcache.Theme;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class IconResourcesTests {
    @DataProvider
    public static Object[][] iconTheme() {
        return Arrays.stream(IconCache.Icon.values())
                .flatMap(icon -> Arrays.stream(Theme.values())
                        .map(theme -> new Object[]{icon, theme}))
                .toArray(Object[][]::new);
    }

    /**
     * Try to detect unobvious pitfalls when dealing with embedded resources
     * <ul>
     *  <li>First, <code>Class.getResourceAsStream()</code> handles relative paths containing <code>..</code>
     *  differently inside a JAR vs IDE.</li>
     *  <li>Second, <code>Path</code> calculations on Windows will process as <code>resources\foo.svg</code>
     *  instead of<code>resources/foo.svg</code></li>
     * </ul>
     */
    @Test(dataProvider = "iconTheme")
    public void testIconCache(IconCache.Icon icon, Theme theme) {
        Assert.assertNotNull(IconCache.getInstance().getIcon(icon, theme.isDark()));
    }
}