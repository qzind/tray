package qz.ui.tray.linux;

import qz.utils.FileUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class LinuxSniIconTheme {

    private static final String ICON_NAME = "qz-tray";
    private static final String RESOURCE_PATH = "/qz/ui/resources/qz-default-%s.png";
    private static final int[] ICON_SIZES = {32, 48};

    static String prepare() throws IOException {
        Path themePath = getThemePath();

        for(int size : ICON_SIZES) {
            copyIcon(size, themePath);
        }

        return themePath.toString();
    }

    private static Path getThemePath() throws IOException {
        return FileUtilities.TEMP_DIR != null
                ? FileUtilities.TEMP_DIR.resolve("sni-icons")
                : Files.createTempDirectory("qz_sni_icons_");
    }

    private static void copyIcon(int size, Path themePath) throws IOException {
        // IconThemePath points to the theme parent
        // tray hosts then resolve IconName through
        // the standard hicolor/<size>/apps layout
        Path iconPath = themePath
                .resolve("hicolor")
                .resolve(size + "x" + size)
                .resolve("apps")
                .resolve(ICON_NAME + ".png");

        Files.createDirectories(iconPath.getParent());

        try(InputStream in = LinuxSniIconTheme.class.getResourceAsStream(String.format(RESOURCE_PATH, size))) {
            if(in == null) {
                throw new IOException(String.format("StatusNotifier icon resource missing for size %s", size));
            }
            Files.copy(in, iconPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
