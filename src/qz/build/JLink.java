/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2020 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.build;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.FileUtils;
import org.slf4j.*;
import qz.common.Constants;
import qz.utils.FileUtilities;
import qz.utils.MacUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class JLink {
    private static final Logger log = LoggerFactory.getLogger(JLink.class);
    private static final String DOWNLOAD_URL = "https://github.com/AdoptOpenJDK/openjdk%s-binaries/releases/download/jdk-%s/OpenJDK%sU-jdk_%s_%s_%s_%s.%s";
    private static final String JAVA_VENDOR = "AdoptOpenJDK";
    private static final String JAVA_VERSION = "11.0.10+9";
    private static final String JAVA_MAJOR = JAVA_VERSION.split("\\.")[0];
    private static final String JAVA_MINOR = JAVA_VERSION.split("\\.")[1];
    private static final String JAVA_PATCH = JAVA_VERSION.split("\\.|\\+|-")[2];
    private static final String JAVA_VERSION_FILE = JAVA_VERSION.replaceAll("\\+", "_");
    private static final String JAVA_DEFAULT_GC_ENGINE = "hotspot";
    private static final String JAVA_DEFAULT_ARCH = "x64";

    private String jarPath;
    private String jdepsPath;
    private Version jdepsVersion;
    private String jlinkPath;
    private String jmodsPath;
    private String outPath;
    private LinkedHashSet<String> depList;


    public JLink(String platform, String arch, String gcEngine) throws IOException {
        downloadJdk(platform, arch, gcEngine)
                .calculateJarPath()
                .calculateOutPath()
                .calculateToolPaths()
                .calculateDepList()
                .deployJre();
    }

    public static void main(String ... args) throws IOException {
        new JLink(args.length > 0 ? args[0] : null,
                  args.length > 1 ? args[1] : null,
                  args.length > 2 ? args[2] : null);
    }

    private JLink downloadJdk(String platform, String arch, String gcEngine) throws IOException {
        if(platform == null) {
            if(SystemUtilities.isMac()) {
                platform = "mac";
            } else if(SystemUtilities.isWindows()) {
                platform = "windows";
            } else {
                platform = "linux";
            }
            log.info("No platform specified, assuming '{}'", platform);
        }
        if(arch == null) {
            arch = JAVA_DEFAULT_ARCH;
            log.info("No architecture specified, assuming '{}'", arch);
        }
        if(gcEngine == null) {
            gcEngine = JAVA_DEFAULT_GC_ENGINE;
            log.info("No garbage collector specified, assuming '{}'", gcEngine);
        }

        String fileExt = platform.equals("windows") ? "zip" : "tar.gz";

        // Assume consistent formatting
        String url = String.format(DOWNLOAD_URL, JAVA_MAJOR, JAVA_VERSION,
                                   JAVA_MAJOR, arch, platform, gcEngine,
                                   JAVA_VERSION_FILE, fileExt);

        // Saves to out e.g. "out/jlink/jdk-platform-11_0_7"
        String extractedJdk = new Fetcher(String.format("jlink/jdk-%s-%s", platform, JAVA_VERSION_FILE), url)
                .fetch()
                .uncompress();

        // Get first subfolder, e.g. jdk-11.0.7+10
        for(File subfolder : new File(extractedJdk).listFiles(pathname -> pathname.isDirectory())) {
            extractedJdk = subfolder.getPath();
            if(platform.equals("mac")) {
                extractedJdk += "/Contents/Home";
            }
            log.info("Selecting JDK home: {}", extractedJdk);
            break;
        }

        jmodsPath = Paths.get(extractedJdk, "jmods").toString();
        log.info("Selecting jmods: {}", jmodsPath);

        return this;
    }

    private JLink calculateJarPath() throws IOException {
        jarPath = SystemUtilities.getJarPath();
        if(!jarPath.endsWith(".jar")) {
            // Assume running from IDE
            jarPath = Paths.get(jarPath, "..", "dist", Constants.PROPS_FILE + ".jar").toFile().getCanonicalPath();
        }
        log.info("Assuming jar path: {}", jarPath);
        return this;
    }

    private JLink calculateOutPath() throws IOException {
        if(SystemUtilities.isMac()) {
            outPath = Paths.get(jarPath, "../PlugIns/Java.runtime/Contents/Home").toFile().getCanonicalPath();
        } else {
            outPath = Paths.get(jarPath, "../jre").toFile().getCanonicalPath();
        }
        log.info("Assuming output path: {}", outPath);
        return this;
    }

    private JLink calculateToolPaths() throws IOException {
        String javaHome = System.getProperty("java.home");
        log.info("Using JAVA_HOME: {}", javaHome);
        jdepsPath = Paths.get(javaHome, "bin", SystemUtilities.isWindows() ? "jdeps.exe" : "jdeps").toFile().getCanonicalPath();
        jlinkPath = Paths.get(javaHome, "bin", SystemUtilities.isWindows() ? "jlink.exe" : "jlink").toFile().getCanonicalPath();
        log.info("Assuming jdeps path: {}", jdepsPath);
        log.info("Assuming jlink path: {}", jlinkPath);
        jdepsVersion = SystemUtilities.getJavaVersion(ShellUtilities.executeRaw(jdepsPath, "--version"));
        return this;
    }

    private JLink calculateDepList() throws IOException {
        log.info("Calling jdeps to determine runtime dependencies");
        depList = new LinkedHashSet<>();

        // JDK13+ allows suppressing of missing deps
        String raw = jdepsVersion.getMajorVersion() >= 13 ?
                ShellUtilities.executeRaw(jdepsPath, "--list-deps", "--ignore-missing-deps", jarPath) :
                ShellUtilities.executeRaw(jdepsPath, "--list-deps", jarPath);
        if (raw == null || raw.trim().isEmpty() || raw.trim().startsWith("Warning") ) {
            throw new IOException("An unexpected error occurred calling jdeps.  Please check the logs for details.\n" + raw);
        }
        for(String item : raw.split("\\r?\\n")) {
            item = item.trim();
            if(!item.isEmpty()) {
                if(item.startsWith("JDK")) {
                    // Remove e.g. "JDK removed internal API/sun.reflect"
                    log.trace("Removing dependency: '{}'", item);
                    continue;
                }
                if(item.contains("/")) {
                    // Isolate base name e.g. "java.base/com.sun.net.ssl"
                    item = item.split("/")[0];
                }
                depList.add(item);
            }
        }
        return this;
    }

    private JLink deployJre() throws IOException {
        if(SystemUtilities.isMac()) {
            // Deploy Contents/MacOS/libjli.dylib
            File macOS = new File(outPath, "../MacOS").getCanonicalFile();
            macOS.mkdirs();
            log.info("Deploying {}/libjli.dylib", macOS);
            FileUtils.copyFileToDirectory(new File(jmodsPath, "../../MacOS/libjli.dylib"), macOS);

            // Deploy Contents/Info.plist
            HashMap<String, String> fieldMap = new HashMap<>();
            fieldMap.put("%BUNDLE_ID%", MacUtilities.getBundleId() + ".jre"); // e.g. io.qz.qz-tray.jre
            fieldMap.put("%BUNDLE_VERSION%", String.format("%s.%s.%s", JAVA_MAJOR, JAVA_MINOR, JAVA_PATCH));
            fieldMap.put("%BUNDLE_VERSION_FULL%", JAVA_VERSION);
            fieldMap.put("%BUNDLE_VENDOR%", JAVA_VENDOR);
            log.info("Deploying {}/Info.plist", macOS.getParent());
            FileUtilities.configureAssetFile("assets/mac-runtime.plist.in", new File(macOS.getParentFile(), "Info.plist"), fieldMap, JLink.class);
        }

        FileUtils.deleteQuietly(new File(outPath));

        if(ShellUtilities.execute(jlinkPath,
                                  "--strip-debug",
                                  "--compress=2",
                                  "--no-header-files",
                                  "--no-man-pages",
                                  "--module-path", jmodsPath,
                                  "--add-modules", String.join(",", depList),
                                  "--output", outPath)) {
            log.info("Successfully deployed a jre to {}", outPath);

            // Remove all but java/javaw
            for(File binFile : new File(outPath, "bin").listFiles()) {
                if(!binFile.getName().startsWith("java")) {
                    log.info("Removing {}", binFile);
                    binFile.delete();
                }
            }

            return this;

        }
        throw new IOException("An error occurred deploying the jre.  Please check the logs for details.");
    }

}
