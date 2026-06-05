package qz.ui.tray.linux;

import org.freedesktop.dbus.DBusPath;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class LinuxStatusNotifierItem implements KdeStatusNotifierItem, FreedesktopStatusNotifierItem {

    private static final String OBJECT_PATH = "/StatusNotifierItem";
    private static final DBusPath MENU_PATH = new DBusPath("/MenuBar");
    private static final String CATEGORY = "ApplicationStatus";
    private static final String ID = "qz-tray";
    private static final String TITLE = "QZ Tray";
    private static final String STATUS = "Active";
    private static final String ICON_NAME = "qz-tray";
    private static final String ICON_PIXMAP_RESOURCE = "/qz/ui/resources/qz-default-%s.png";
    private static final int[] ICON_PIXMAP_SIZES = {16, 22, 24, 32, 48};

    private final String iconThemePath;
    private final LinuxStatusNotifierIconPixmap[] iconPixmap;
    private final boolean iconPixmapOnly;

    LinuxStatusNotifierItem(String iconThemePath, boolean iconPixmapOnly) throws IOException {
        this.iconThemePath = iconThemePath;
        this.iconPixmapOnly = iconPixmapOnly;
        iconPixmap = loadIconPixmap();
    }

    @Override
    public String getObjectPath() {
        return OBJECT_PATH;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getStatus() {
        return STATUS;
    }

    @Override
    public String getIconName() {
        // The SNI spec lets hosts prefer IconName over IconPixmap when both
        // exist. COSMIC currently needs a pixmap-only item to avoid retrying
        // unresolved theme lookup.
        return iconPixmapOnly ? "" : ICON_NAME;
    }

    @Override
    public String getIconThemePath() {
        return iconPixmapOnly ? "" : iconThemePath;
    }

    @Override
    public LinuxStatusNotifierIconPixmap[] getIconPixmap() {
        // COSMIC currently registers the item but does not resolve the generated
        // IconThemePath, so expose direct ARGB pixels as a host-side fallback.
        return iconPixmap;
    }

    @Override
    public DBusPath getMenu() {
        // Without this, Ubuntu GNOME dropped the registered item until it received
        // a non-empty Menu object path before it would consider the tray item as ready
        // for display. The POC exports a minimal com.canonical.dbusmenu object at
        // this path before registering the item.
        return MENU_PATH;
    }

    @Override
    public void contextMenu(int x, int y) {}

    @Override
    public void activate(int x, int y) {}

    @Override
    public void secondaryActivate(int x, int y) {}

    @Override
    public void scroll(int delta, String orientation) {}

    private static LinuxStatusNotifierIconPixmap[] loadIconPixmap() throws IOException {
        LinuxStatusNotifierIconPixmap[] pixmaps = new LinuxStatusNotifierIconPixmap[ICON_PIXMAP_SIZES.length];
        BufferedImage baseImage = readPixmapResource(48);

        for(int i = 0; i < ICON_PIXMAP_SIZES.length; i++) {
            int size = ICON_PIXMAP_SIZES[i];
            BufferedImage image = size == 32 || size == 48 ? readPixmapResource(size) : scalePixmap(baseImage, size);
            pixmaps[i] = new LinuxStatusNotifierIconPixmap(size, size, getArgbPixels(image));
        }

        return pixmaps;
    }

    private static BufferedImage readPixmapResource(int size) throws IOException {
        String resourcePath = String.format(ICON_PIXMAP_RESOURCE, size);

        try(InputStream in = LinuxStatusNotifierItem.class.getResourceAsStream(resourcePath)) {
            if(in == null) {
                throw new IOException(String.format("StatusNotifier icon resource missing: %s", resourcePath));
            }
            BufferedImage image = ImageIO.read(in);
            if(image == null) {
                throw new IOException(String.format("StatusNotifier icon resource unreadable: %s", resourcePath));
            }

            return image;
        }
    }

    private static BufferedImage scalePixmap(BufferedImage image, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(image, 0, 0, size, size, null);
        graphics.dispose();

        return scaled;
    }

    private static byte[] getArgbPixels(BufferedImage image) {
        byte[] pixels = new byte[image.getWidth() * image.getHeight() * 4];
        int offset = 0;

        // StatusNotifier hosts expect IconPixmap as row-major ARGB bytes.
        for(int y = 0; y < image.getHeight(); y++) {
            for(int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                pixels[offset++] = (byte)((argb >> 24) & 0xff);
                pixels[offset++] = (byte)((argb >> 16) & 0xff);
                pixels[offset++] = (byte)((argb >> 8) & 0xff);
                pixels[offset++] = (byte)(argb & 0xff);
            }
        }

        return pixels;
    }
}
