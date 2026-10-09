package qz.ui.tray.linux.xdg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.ui.component.IconCache;
import qz.ui.component.iconcache.Format;
import qz.ui.component.iconcache.Type;
import qz.utils.ByteUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Takes a bunch of image paths and constructs a usable <code>index.theme</code> file
 */
public class ThemeBuilder {
    private static final Logger log = LogManager.getLogger(ThemeBuilder.class);

    final static String BASE_DIR = "hicolor";
    final StringBuilder sections = new StringBuilder();
    final HashSet<Path> directories = new LinkedHashSet<>();

    static class Section {
        final Path path;
        boolean isScalable;
        Path relativePath; // heading
        int size;
        int minSize;
        int maxSize;
        String context;
        String type;

        Section(Path path) {
            this.path = path;
            this.isScalable = isScalable();
            this.relativePath = getRelativePath();
            this.size = getSize();
            this.minSize = isScalable ? 1 : size;
            this.maxSize = isScalable ? 256 : size;
            this.context = getContext();
            this.type = isScalable ? "Scalable" : "Fixed";
        }

        Path getRelativePath() {
            String p = path.toString();
            String split = File.separator + BASE_DIR + File.separator;
            return Paths.get(p.substring(p.lastIndexOf(split) + split.length())).getParent();
        }

        int getSize() {
            if(!isScalable) {
                String parsed = path.getParent().getParent().getFileName().toString().split("x")[0];
                if(ByteUtilities.isNumber(parsed)) {
                    return Integer.parseInt(parsed);
                }
            }
            return 16;
        }

        boolean isScalable() {
            return path.getFileName().toString().endsWith(".svg");
        }

        String getContext() {
            return switch(path.getParent().getFileName().toString()) {
                case "devices" -> "Devices";
                case "status" -> "Status";
                default -> "Applications";
            };
        }

        final static String SCALABLE_TEMPLATE = """
            
            [%s]
            Size=%s
            MinSize=%s
            MaxSize=%s
            Context=%s
            Type=%s
            """;

        final static String FIXED_TEMPLATE = """
            
            [%s]
            Size=%s
            Context=%s
            Type=%s
            """;

        @Override
        public String toString() {
            return isScalable ?
                    String.format(SCALABLE_TEMPLATE,
                                 relativePath,
                                 size,
                                 minSize,
                                 maxSize,
                                 context,
                                 type) :
                    String.format(FIXED_TEMPLATE,
                                  relativePath,
                                  size,
                                  context,
                                  type);
        }
    }

    void append(Path image) {
        Section section = new Section(image);
        // List of relative directories in our theme
        if(directories.add(section.relativePath)) { // scalable/apps, 16x16/apps etc
            sections.append(section); // Append the ini section
        }
    }

    final static String INDEX_TEMPLATE = """
            [Icon Theme]
            Name=%s
            Comment=%s System Tray Theme
            Directories=%s
            %s
            """;

    @Override
    public String toString() {
        return String.format(INDEX_TEMPLATE,
                             Constants.ABOUT_TITLE,
                             Constants.ABOUT_TITLE,
                             directories.stream()
                                     .map(Path::toString)
                                     .collect(Collectors.joining(",")),
                             sections
        );
    }

    /**
     * Creates the following theme layout
     * <pre>
     *  $root
     *   └── $BASE_DIR/
     *       ├── index.theme
     *       ├── 32x32/
     *       │   └── apps/
     *       │       └── qz-tray.png
     *       ├── 48x48/
     *       │   └── apps/
     *       │       └── qz-tray.png
     *       └── scalable/
     *           ├── status/
     *           │   └── qz-tray.svg
     *           │   └── qz-tray-symbolic.svg
     *           └── apps/
     *           │   └── qz-tray.svg
     *               └── qz-tray-symbolic.svg
     * </pre>

     */
    public static Path buildThemeLayout(Path root) throws IOException {
        ThemeBuilder builder = new ThemeBuilder();

        // First, create PNGs
        for(int size : Type.SYSTEM_TRAY.getSizes()) {
            Path png = IconCache.getInstance().extract(Format.PNG, IconCache.Icon.TRAY_READY_COLOR, size);
            Path dest = root
                    .resolve(BASE_DIR)
                    .resolve(String.format("%sx%s", size, size))
                    .resolve("apps")
                    .resolve(String.format("%s.png", Constants.PROPS_FILE));

            builder.append(Files.copy(png, Files.createDirectories(dest), StandardCopyOption.REPLACE_EXISTING));
        }

        // Next, create regular and symbolic SVGs
        for(IconCache.Icon icon : new IconCache.Icon[]{ IconCache.Icon.TRAY_READY_COLOR, IconCache.Icon.TRAY_READY }) {
            Path svg = IconCache.getInstance().extract(Format.SVG, icon);
            for(String subfolder : new String[] { "apps", "status" }) {
                Path dest = root
                        .resolve(BASE_DIR)
                        .resolve("scalable")
                        .resolve(subfolder)
                        .resolve(String.format(icon.isMaskIcon() ? "%s-symbolic.svg" : "%s.svg", Constants.PROPS_FILE));

                builder.append(Files.copy(svg, Files.createDirectories(dest), StandardCopyOption.REPLACE_EXISTING));
            }
        }

        Files.writeString(root.resolve(BASE_DIR).resolve("index.theme"), builder.toString());
        log.debug("Wrote '{}/index.theme' for Linux System Tray support", BASE_DIR);
        return root;
    }
}
