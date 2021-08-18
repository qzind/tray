package qz.utils;

import com.sun.jna.Platform;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class LibUtilities {
    private static final Logger log = LoggerFactory.getLogger(LibUtilities.class);

    private static Boolean isExternalLibs = null;
    private static Boolean isInternalLibs = null;

    public static void bindLibs() {
        bindJnaLibs();
        bindUsb4JavaLibs();
        bindJavafxLibs();
        bindJsscLibs();
    }

    /**
     * Predicts the location of jnidispatch and sets jna.boot.library.path.
     * Note, this must be called before any jna vars/methods are referenced.
     *
     * @return
     */
    public static void bindJnaLibs() {
        if (externalLibs()) System.setProperty("jna.boot.library.path", getLibsLocation().toString());
    }

    public static Path getLibsLocation() {
        if (SystemUtilities.isInstalled() && SystemUtilities.isMac()) {
             return SystemUtilities.getJarParentPath().getParent().resolve("Frameworks");
        } else {
            return SystemUtilities.getJarParentPath().resolve("libs");
        }
    }

    public static void bindUsb4JavaLibs() {
        if (externalLibs()) {
            try {
                //Todo Remove this debugging log
                //log.warn("extern");
                Field loaded = Loader.class.getDeclaredField("loaded");
                loaded.setAccessible(true);
                // Trick usb4java into thinking it has already ran its load function. This will prevent it loading resources twice.
                loaded.set(Loader.class, true);

                Method getPlatform = Loader.class.getDeclaredMethod("getPlatform");
                Method getLibName = Loader.class.getDeclaredMethod("getLibName");
                // On windows, a second dll is needed. As of version 1.2.0 getExtraLibName returns null for all other OSs.
                Method getExtraLibName = Loader.class.getDeclaredMethod("getExtraLibName");
                getPlatform.setAccessible(true);
                getLibName.setAccessible(true);
                getExtraLibName.setAccessible(true);

                // Simulate path calculation like Loader.load does
                String lib = (String)getLibName.invoke(Loader.class);
                String extraLib = (String)getExtraLibName.invoke(Loader.class);
                if (extraLib != null) System.load(getLibsLocation().resolve(extraLib).toString());
                System.load(getLibsLocation().resolve(lib).toString());
            } catch(InvocationTargetException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
                log.error("Failed to bind usb4java", e);
            }
        }
    }

    public static void bindJsscLibs() {

    }

    public static void bindJavafxLibs() {
        // JavaFX native libs
        if (SystemUtilities.isJar()) {
            SystemUtilities.insertPathProperty(
                    "java.library.path",
                    SystemUtilities.getAppPath().resolve("Contents/Frameworks").toString(),
                    "/jni" /* appends to end if not found */
            );
        } else if (hasConflictingLib()) {
            // IDE helper for "no suitable pipeline found" errors
            System.err.println("\n=== WARNING ===\nWrong javafx platform detected. Delete lib/javafx/<platform> to correct this.\n");
        }
    }

    public static boolean externalLibs() {
        if (isExternalLibs ==  null) {
            // We'll move all native libraries (including JNA) at packaging time using ant task "externalize-libs"
            // JNA is used heavily and is a good way to verify that the libs have been externalized
            try {
                Stream<Path> s = Files.find(getLibsLocation(), 1, (path, basicFileAttributes) -> path.toFile().getName().matches("libjnidispatch.*"));
                isExternalLibs = s.count() != 0;
            }
            catch(IOException e) {
                log.error("Error determining if jna lib exists", e);
                isExternalLibs = false;
            }
        }
        return isExternalLibs;
    }

    public static boolean internalLibs() {
        if (isInternalLibs ==  null) {
            // We'll move all native libraries (including JNA) at packaging time using ant task "externalize-libs"
            // JNA is used heavily and is a good way to verify that the libs have been externalized
            URL resourceUrl = SystemUtilities.class.getResource("/com/sun/jna/" + Platform.RESOURCE_PREFIX);
            isInternalLibs = resourceUrl == null;
        }
        return isInternalLibs;
    }

    private static boolean hasConflictingLib() {
        // If running from the IDE, make sure we're not using the wrong libs
        URL url = Application.class.getResource("/" + Application.class.getName().replace('.', '/') + ".class");
        String graphicsJar = url.toString().replaceAll("file:/|jar:", "").replaceAll("!.*", "");
        log.trace("JavaFX will startup using {}", graphicsJar);
        if (SystemUtilities.isWindows()) {
            return !graphicsJar.contains("windows");
        } else if (SystemUtilities.isMac()) {
            return !graphicsJar.contains("osx") && !graphicsJar.contains("mac");
        }
        return !graphicsJar.contains("linux");
    }
}
