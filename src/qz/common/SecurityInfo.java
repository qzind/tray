package qz.common;

import com.sun.jna.Native;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.util.Jetty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import purejavahidapi.PureJavaHidApi;
import qz.utils.SystemUtilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.*;

/**
 * Created by Kyle B. on 10/27/2017.
 */
public class SecurityInfo {
    /**
     * Wrap throwable operations into a try/catch
     */
    private static class CheckedTreeMap<K, V> extends TreeMap<K, V> {
        private static final Logger log = LoggerFactory.getLogger(CheckedTreeMap.class);

        interface CheckedValue {
            Object check() throws Throwable;
        }

        @SuppressWarnings("unchecked")
        public V put(K key, CheckedValue value) {
            try {
                return put(key, (V)value.check());
            } catch(Throwable t) {
                log.warn("A checked exception was suppressed adding key \"{}\"", key, t);
            }
            return null;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(SecurityInfo.class);

    public static KeyStore getKeyStore(Properties props) {
        if (props != null) {
            String store = props.getProperty("wss.keystore", "");
            char[] pass = props.getProperty("wss.storepass", "").toCharArray();

            try {
                KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                keystore.load(new FileInputStream(store), pass);
                return keystore;
            }
            catch(GeneralSecurityException | IOException e) {
                log.warn("Unable to create keystore from properties file: {}", e.getMessage());
            }
        }

        return null;
    }

    public static SortedMap<String,String> getLibVersions() {
        CheckedTreeMap<String,String> libVersions = new CheckedTreeMap<>();

        // Use API-provided mechanism if available
        libVersions.put("jna (native)", () -> Native.VERSION_NATIVE);
        libVersions.put("jna", Native.VERSION);
        libVersions.put("jssc", () -> jssc.SerialNativeInterface.getLibraryVersion());
        libVersions.put("jetty", Jetty.VERSION);
        libVersions.put("pdfbox", org.apache.pdfbox.util.Version.getVersion());
        libVersions.put("purejavahidapi", () -> PureJavaHidApi.getVersion());
        libVersions.put("usb-api", javax.usb.Version.getApiVersion());
        libVersions.put("not-yet-commons-ssl", org.apache.commons.ssl.Version.VERSION);
        libVersions.put("mslinks", mslinks.ShellLink.VERSION);
        libVersions.put("simplersa", (String)null);
        libVersions.put("bouncycastle", "" + new BouncyCastleProvider().getVersion());

        libVersions.put("jre", Constants.JAVA_VERSION.toString());
        libVersions.put("jre (vendor)", Constants.JAVA_VENDOR);

        //JFX info, if it exists
        try {
            Class<?> VersionInfo = Class.forName("com.sun.javafx.runtime.VersionInfo");
            String fxPath = URLDecoder.decode(VersionInfo.getProtectionDomain().getCodeSource().getLocation().toString(), "UTF-8");
            Method method = VersionInfo.getMethod("getVersion");
            Object version = method.invoke(null);
            libVersions.put("javafx", (String)version);
            if (fxPath.contains(SystemUtilities.getJarPath().toString()) || fxPath.contains("/tray/")) {
                libVersions.put("javafx (location)", "Bundled/" + Constants.ABOUT_TITLE);
            } else {
                libVersions.put("javafx (location)", "System/" + Constants.JAVA_VENDOR);
            }
        } catch(Throwable e) {
            libVersions.put("javafx", "Failed");
            libVersions.put("javafx (location)", "Failed");
        }

        // Fallback to maven manifest information
        HashMap<String,String> mavenVersions = getMavenVersions();

        String[] mavenLibs = {"jetty-servlet", "apache-log4j-extras", "jetty-io", "websocket-common",
                              "slf4j-log4j12", "usb4java-javax", "java-semver", "commons-pool2",
                              "websocket-server", "jettison", "commons-codec", "log4j", "slf4j-api",
                              "websocket-servlet", "jetty-http", "commons-lang3", "javax-websocket-server-impl",
                              "javax.servlet-api", "usb4java", "websocket-api", "jetty-util", "websocket-client",
                              "javax.websocket-api", "commons-io", "jetty-security"};

        for(String lib : mavenLibs) {
            libVersions.put(lib, mavenVersions.get(lib));
        }

        return libVersions;
    }

    public static void printLibInfo() {
        String format = "%-40s%s%n";
        System.out.printf(format, "LIBRARY NAME:", "VERSION:");
        SortedMap<String,String> libVersions = SecurityInfo.getLibVersions();
        for(Map.Entry<String,String> entry : libVersions.entrySet()) {
            if (entry.getValue() == null) {
                System.out.printf(format, entry.getKey(), "(unknown)");
            } else {
                System.out.printf(format, entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Fetches embedded version information based on maven properties
     *
     * @return HashMap of library name, version
     */
    private static HashMap<String,String> getMavenVersions() {
        final HashMap<String,String> mavenVersions = new HashMap<>();
        String jar = "jar:" + SecurityInfo.class.getProtectionDomain().getCodeSource().getLocation().toString();
        try(FileSystem fs = FileSystems.newFileSystem(new URI(jar), new HashMap<String,String>())) {
            Files.walkFileTree(fs.getPath("/META-INF/maven"), new HashSet<FileVisitOption>(), 3, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".properties")) {
                        try {
                            Properties props = new Properties();
                            props.load(Files.newInputStream(file, StandardOpenOption.READ));
                            mavenVersions.put(props.getProperty("artifactId"), props.getProperty("version"));
                        }
                        catch(Exception e) {
                            log.warn("Error reading properties from {}", file, e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch(Exception ignore) {
            log.warn("Could not open {} for version information.  Most libraries will list as (unknown)", jar);
        }
        return mavenVersions;
    }

}
