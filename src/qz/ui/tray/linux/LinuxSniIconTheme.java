package qz.ui.tray.linux;

import qz.utils.FileUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class LinuxSniIconTheme {

    private static final String ICON_NAME = "qz-tray";
    private static final String RESOURCE_PATH = "/qz/ui/resources/qz-default-%s.png";
    private static final Path SVG_PATH = Path.of("assets", "branding", "linux-icon.svg");
    private static final int[] ICON_SIZES = {32, 48};

    static String prepare() throws IOException {
        Path themePath = getThemePath();

        writeThemeIndex(themePath);
        for(int size : ICON_SIZES) {
            copyIcon(size, themePath);
        }
        copyScalableIcon(themePath);

        return themePath.toString();
    }

    private static Path getThemePath() throws IOException {
        return FileUtilities.TEMP_DIR != null
                ? FileUtilities.TEMP_DIR.resolve("sni-icons")
                : Files.createTempDirectory("qz_sni_icons_");
    }

    /**
     * Makes the generated hicolor directory a valid icon theme
     * so GTK/GNOME can resolve qz-tray instead of showing a fallback icon
     */
    private static void writeThemeIndex(Path themePath) throws IOException {
        Path indexPath = themePath.resolve("hicolor").resolve("index.theme");
        StringBuilder directories = new StringBuilder();
        StringBuilder sections = new StringBuilder();

        for(int size : ICON_SIZES) {
            if(directories.length() > 0) {
                directories.append(',');
            }
            directories.append(size).append('x').append(size).append("/apps");
            sections.append("\n")
                    .append('[').append(size).append('x').append(size).append("/apps]")
                    .append("\n")
                    .append("Size=").append(size).append("\n")
                    .append("Context=Applications").append("\n")
                    .append("Type=Fixed").append("\n");
        }
        if(Files.exists(SVG_PATH)) {
            if(directories.length() > 0) {
                directories.append(',');
            }
            directories.append("scalable/apps");
            sections.append("\n")
                    .append("[scalable/apps]")
                    .append("\n")
                    .append("Size=128").append("\n")
                    .append("MinSize=1").append("\n")
                    .append("MaxSize=256").append("\n")
                    .append("Context=Applications").append("\n")
                    .append("Type=Scalable").append("\n");
        }

        String index = "[Icon Theme]\n"
                + "Name=QZ Tray\n"
                + "Comment=Temporary QZ Tray StatusNotifier icons\n"
                + "Directories=" + directories + "\n"
                + sections;

        Files.createDirectories(indexPath.getParent());
        Files.writeString(indexPath, index, StandardCharsets.UTF_8);
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

    private static void copyScalableIcon(Path themePath) throws IOException {
        if(!Files.exists(SVG_PATH)) {
            return;
        }

        Path iconPath = themePath
                .resolve("hicolor")
                .resolve("scalable")
                .resolve("apps")
                .resolve(ICON_NAME + ".svg");

        Files.createDirectories(iconPath.getParent());
        Files.copy(SVG_PATH, iconPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
