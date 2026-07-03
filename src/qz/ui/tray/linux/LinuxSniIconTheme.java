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
    private static final String SYMBOLIC_ICON_NAME = "qz-tray-symbolic";
    private static final String NOTIFICATION_DARK_ICON_NAME = "qz-tray-notification-dark";
    private static final String NOTIFICATION_LIGHT_ICON_NAME = "qz-tray-notification-light";
    private static final String DARK_ICON_COLOR = "#2e3436";
    private static final String LIGHT_ICON_COLOR = "#eeeeec";
    // Tray hosts resolve the exported IconName exactly, so the resource
    // is named with the same stable freedesktop-style symbolic icon name.
    private static final String PNG_RESOURCE_PATH = "/qz/ui/resources/qz-default-%s.png";
    private static final String SVG_RESOURCE_PATH = "/qz/ui/resources/%s.svg";
    private static final int[] ICON_SIZES = {32, 48};

    static String prepare() throws IOException {
        Path themePath = getThemePath();

        writeThemeIndex(themePath);
        for(int size : ICON_SIZES) {
            copyIcon(size, themePath);
        }
        copySymbolicIcons(themePath);
        writeNotificationIcon(themePath, NOTIFICATION_DARK_ICON_NAME, DARK_ICON_COLOR);
        writeNotificationIcon(themePath, NOTIFICATION_LIGHT_ICON_NAME, LIGHT_ICON_COLOR);

        return themePath.toString();
    }

    static String getPngIconPath(String themePath) {
        // xapp-sn-watcher accepts an absolute IconName path
        // https://github.com/linuxmint/xapp/blob/master/xapp-sn-watcher/sn-item.c
        return Path.of(themePath)
                .resolve("hicolor")
                .resolve("32x32")
                .resolve("apps")
                .resolve(ICON_NAME + ".png")
                .toString();
    }

    static String getSvgIconPath(String themePath) {
        return Path.of(themePath)
                .resolve("hicolor")
                .resolve("scalable")
                .resolve("apps")
                .resolve(SYMBOLIC_ICON_NAME + ".svg")
                .toString();
    }

    static String getDarkNotificationIconPath(String themePath) {
        return getNotificationIconPath(themePath, NOTIFICATION_DARK_ICON_NAME).toString();
    }

    static String getLightNotificationIconPath(String themePath) {
        return getNotificationIconPath(themePath, NOTIFICATION_LIGHT_ICON_NAME).toString();
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
        appendDirectory(directories, sections, "scalable/status", "Status");
        appendDirectory(directories, sections, "scalable/apps", "Applications");

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

        try(InputStream in = LinuxSniIconTheme.class.getResourceAsStream(String.format(PNG_RESOURCE_PATH, size))) {
            if(in == null) {
                throw new IOException(String.format("StatusNotifier icon resource missing for size %s", size));
            }
            Files.copy(in, iconPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copySymbolicIcons(Path themePath) throws IOException {
        copySymbolicIcon(themePath, "status");
        copySymbolicIcon(themePath, "apps");
    }

    private static void copySymbolicIcon(Path themePath, String context) throws IOException {
        try(InputStream in = LinuxSniIconTheme.class.getResourceAsStream(String.format(SVG_RESOURCE_PATH, SYMBOLIC_ICON_NAME))) {
            if(in == null) {
                throw new IOException(String.format("StatusNotifier SVG icon resource missing: %s", SYMBOLIC_ICON_NAME));
            }

            Path iconPath = themePath
                    .resolve("hicolor")
                    .resolve("scalable")
                    .resolve(context)
                    .resolve(SYMBOLIC_ICON_NAME + ".svg");

            Files.createDirectories(iconPath.getParent());
            Files.copy(in, iconPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeNotificationIcon(Path themePath, String iconName, String color) throws IOException {
        Path iconPath = getNotificationIconPath(themePath.toString(), iconName);
        Files.createDirectories(iconPath.getParent());
        Files.writeString(iconPath, getColorizedSymbolicSvg(color), StandardCharsets.UTF_8);
    }

    private static Path getNotificationIconPath(String themePath, String iconName) {
        return Path.of(themePath)
                .resolve("hicolor")
                .resolve("scalable")
                .resolve("apps")
                .resolve(iconName + ".svg");
    }

    private static String getColorizedSymbolicSvg(String color) throws IOException {
        try(InputStream in = LinuxSniIconTheme.class.getResourceAsStream(String.format(SVG_RESOURCE_PATH, SYMBOLIC_ICON_NAME))) {
            if(in == null) {
                throw new IOException(String.format("StatusNotifier SVG icon resource missing: %s", SYMBOLIC_ICON_NAME));
            }
            String svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return svg.replaceFirst("(?s)<style id=\"current-color-scheme\" type=\"text/css\">.*?</style>",
                    "<style id=\"current-color-scheme\" type=\"text/css\">.ColorScheme-Text { color: " + color + "; }</style>");
        }
    }

    private static void appendDirectory(StringBuilder directories, StringBuilder sections, String directory, String context) {
        if(directories.length() > 0) {
            directories.append(',');
        }
        directories.append(directory);
        sections.append("\n")
                .append('[').append(directory).append(']')
                .append("\n")
                .append("Size=16").append("\n")
                .append("MinSize=1").append("\n")
                .append("MaxSize=256").append("\n")
                .append("Context=").append(context).append("\n")
                .append("Type=Scalable").append("\n");
    }
}
