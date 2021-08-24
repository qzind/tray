package qz.utils;

import com.sun.jna.Platform;
import javafx.application.Application;
import org.usb4java.Loader;
import qz.common.Constants;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Helper for setting various booth paths for finding native libraries
 *   e.g. "java.library.path", "jna.boot.library.path" ... etc
 */
public class LibUtilities {
    // Files indicating whether or not we can load natives from an external location
    private static final String[] INDICATOR_RESOURCES = {"/com/sun/jna/" + Platform.RESOURCE_PREFIX };
    private static final String LIB_DIR = "libs";
    private static final String MAC_LIB_DIR = "../Contents/Frameworks";

    private static final LibUtilities INSTANCE = new LibUtilities();

    // Track if libraries are externalized into a "libs" folder
    private final boolean externalized;

    // The base library path
    private final Path basePath;

    public LibUtilities() {
        this(calculateBasePath(), calculateExternalized());
    }

    public LibUtilities(Path basePath, boolean externalized) {
        this.basePath = basePath;
        this.externalized = externalized;
    }

    public static LibUtilities getInstance() {
        return INSTANCE;
    }

    public void bind() {
        if(externalized) {
            bindProperties("jna.boot.library.path", // jna, hid4java
                           "jssc.boot.library.path" // jssc
            );
            bindUsb4Java();
        }
        // JavaFX is always externalized
        if(Constants.JAVA_VERSION.getMajorVersion() >= 11) {
            bindProperties("java.library.path"); // javafx
        }

        // TODO: Determine fx "libs" or "${basedir}/lib/javafx" for the running jre and remove
        if (detectJavaFxConflict()) {
            // IDE helper for "no suitable pipeline found" errors
            System.err.println("\n=== WARNING ===\nWrong javafx platform detected. Delete lib/javafx/<platform> to correct this.\n");
        }
    }

    /**
     * Calculates the base native library path based on the jar path
     */
    private static Path calculateBasePath() {
        return SystemUtilities.getJarParentPath().resolve(
                useFrameworks() ? MAC_LIB_DIR : LIB_DIR
        );
    }

    /**
     * Whether to use the standard "libs" directory
     */
    private static boolean useFrameworks() {
        return SystemUtilities.isMac() && SystemUtilities.isInstalled();
    }

    private void bindProperties(String ... properties) {
        Arrays.stream(properties).forEach(this::bindProperty);
    }

    /**
     * Binds a system property to the calculated <code>basePath</code>
     */
    private void bindProperty(String property) {
        bindProperty(property, basePath);
    }

    /**
     * Binds a system property to the specified <code>basePath</code>
     */
    private void bindProperty(String property, Path basePath) {
        if(!property.equals("java.library.path")) {
            System.setProperty(property, basePath.toString());
        } else {
            // Special case for "java.library.path", used by JavaFX
            SystemUtilities.insertPathProperty(
                    "java.library.path",
                    basePath.toString(),
                    "/jni" /* appends to end if not found */
            );
        }
    }

    /**
     * Using reflection, force usb4java to load from the specified boot path
     */
    private void bindUsb4Java() {
        try {
            // Make usb4java think it's already unzipped it's native resources
            Field loaded = Loader.class.getDeclaredField("loaded");
            loaded.setAccessible(true);
            loaded.set(Loader.class, true);

            // Expose private functions (getExtraLibName is only needed for Windows)
            Method getPlatform = Loader.class.getDeclaredMethod("getPlatform");
            Method getLibName = Loader.class.getDeclaredMethod("getLibName");
            Method getExtraLibName = Loader.class.getDeclaredMethod("getExtraLibName");
            getPlatform.setAccessible(true);
            getLibName.setAccessible(true);
            getExtraLibName.setAccessible(true);

            // Simulate Loader.load's path calculation
            String lib = (String)getLibName.invoke(Loader.class);
            String extraLib = (String)getExtraLibName.invoke(Loader.class);
            if (extraLib != null) System.load(basePath.resolve(extraLib).toString());
            System.load(basePath.resolve(lib).toString());
        } catch(Throwable t) {
            System.err.println("Failed to bind usb4java: " + t.getLocalizedMessage());
        }
    }

    // TODO: Determine fx "libs" or "${basedir}/lib/javafx" for the running jre and remove
    private boolean detectJavaFxConflict() {
        // If running from the IDE, make sure we're not using the wrong libs
        URL url = Application.class.getResource("/" + Application.class.getName().replace('.', '/') + ".class");
        String graphicsJar = url.toString().replaceAll("file:/|jar:", "").replaceAll("!.*", "");
        switch(SystemUtilities.getOsType()) {
            case WINDOWS:
                return !graphicsJar.contains("windows");
            case MAC:
                return !graphicsJar.contains("osx") && !graphicsJar.contains("mac");
            default:
                return !graphicsJar.contains("linux");
        }
    }

    /**
     * Detect if the JAR has native resources bundled, if not, we'll assume they've been externalized
     */
    private static boolean calculateExternalized() {
        for(String resource : INDICATOR_RESOURCES)
            if(SystemUtilities.class.getResource(resource) != null)
                return false;

      return true;
    }
}
