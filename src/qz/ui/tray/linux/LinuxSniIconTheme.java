package qz.ui.tray.linux;

import qz.ui.component.IconCache;
import qz.ui.component.iconcache.Format;
import qz.utils.FileUtilities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static qz.ui.component.IconCache.Icon.*;

class LinuxSniIconTheme {
    private static final String ICON_NAME = "qz-tray";
    private static final String SYMBOLIC_ICON_NAME = "qz-tray-symbolic";
    // Tray hosts resolve the exported IconName exactly, so the resource
    // is named with the same stable freedesktop-style symbolic icon name.
    private static final int[] ICON_SIZES = {32, 48};

    static String prepare() throws IOException {
        Path themePath = getThemePath();

        writeThemeIndex(themePath);
        for(int size : ICON_SIZES) {
            copyIcon(size, themePath);
        }
        copySymbolicIcons(themePath);

        return themePath.toString();
    }

    static String getPngIconPath(String themePath) {
        // xapp-sn-watcher accepts an absolute IconName path
        // https://github.com/linuxmint/xapp/blob/master/xapp-sn-watcher/sn-item.c
        return Path.of(themePath)
                .resolve("hicolor")
                .resolve("48x48")
                .resolve("apps")
                .resolve(ICON_NAME + ".png")
                .toString();
    }

    static String getPngIconUri(String themePath) {
        return Path.of(getPngIconPath(themePath)).toUri().toString();
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
        Path sizedPng = IconCache.getInstance().extract(Format.PNG, TRAY_READY_COLOR, size);

        // IconThemePath points to the theme parent
        // tray hosts then resolve IconName through
        // the standard hicolor/<size>/apps layout

        Path iconPath = themePath
                .resolve("hicolor")
                .resolve(size + "x" + size)
                .resolve("apps")
                .resolve(ICON_NAME + ".png");

        Files.createDirectories(iconPath.getParent());
        Files.copy(sizedPng, iconPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copySymbolicIcons(Path themePath) throws IOException {
        copySymbolicIcon(themePath, "status");
        copySymbolicIcon(themePath, "apps");
    }

    private static void copySymbolicIcon(Path themePath, String context) throws IOException {
        Path svg = IconCache.getInstance().extract(Format.SVG, TRAY_READY);

        Path iconPath = themePath
                .resolve("hicolor")
                .resolve("scalable")
                .resolve(context)
                .resolve(SYMBOLIC_ICON_NAME);

        Files.createDirectories(iconPath.getParent());
        Files.copy(svg, iconPath, StandardCopyOption.REPLACE_EXISTING);
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
