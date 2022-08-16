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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;

public class JLink {
    private static final Logger log = LogManager.getLogger(JLink.class);
    private static final String JAVA_AMD64_VENDOR = "AdoptOpenJDK";
    private static final String JAVA_ARM64_VENDOR = "BellSoft";
    private static final String JAVA_VERSION = "11.0.16+8";
    private static final String JAVA_MAJOR = JAVA_VERSION.split("\\.")[0];
    private static final String JAVA_MINOR = JAVA_VERSION.split("\\.")[1];
    private static final String JAVA_PATCH = JAVA_VERSION.split("\\.|\\+|-")[2];
    private static final String JAVA_VERSION_FILE = JAVA_VERSION.replaceAll("\\+", "_");
    private static final String JAVA_DEFAULT_GC_ENGINE = "hotspot";
    private static final String JAVA_DEFAULT_ARCH = VendorArch.ADOPT_AMD64.use;

    private Path jarPath;
    private Path jdepsPath;
    private Path jlinkPath;
    private Path jmodsPath;
    private Path outPath;
    private Version jdepsVersion;
    private String javaVendor;
    private String targetPlatform;
    private LinkedHashSet<String> depList;

    public JLink(String targetPlatform, String arch, String gcEngine) throws IOException {
        this.javaVendor = getJavaVendor(arch);
        this.targetPlatform = targetPlatform;

        if(needsDownload(SystemUtilities.getJavaVersion(JAVA_VERSION), Constants.JAVA_VERSION)) {
            log.warn("Java versions are incompatible, locating a suitable runtime for Java " + JAVA_MAJOR + "...");
            String hostArch = System.getProperty("os.arch");
            String hostJdk = downloadJdk(getJavaVendor(hostArch), null, hostArch, gcEngine);
            calculateToolPaths(Paths.get(hostJdk));
        } else {
            calculateToolPaths(null);
        }

        String extractedJdk = downloadJdk(javaVendor, targetPlatform, arch, gcEngine);
        jmodsPath = Paths.get(extractedJdk, "jmods");
        log.info("Selecting jmods folder: {}", jmodsPath);

        calculateJarPath()
                .calculateOutPath()
                .calculateDepList()
                .deployJre();
    }

    public static void main(String ... args) throws IOException {
        JLink jlink = new JLink(null, null, null).calculateJarPath();
        System.out.println(jlink.jarPath);
        if(true) {
            System.exit(0);
        }


        new JLink(args.length > 0 ? args[0] : null,
                  args.length > 1 ? args[1] : null,
                  args.length > 2 ? args[2] : null);
    }

    /**
     * Handle incompatibilities between JDKs, download a fresh one if needed
     */
    private static boolean needsDownload(Version want, Version installed) {
        // jdeps and jlink historically require matching major JDK versions.  Download if needed.
        boolean downloadJdk = installed.getMajorVersion() != want.getMajorVersion();

        // Per JDK-8240734: Major versions checks aren't enough starting with 11.0.16+8
        // see also https://github.com/adoptium/adoptium-support/issues/557
        Version bad = SystemUtilities.getJavaVersion("11.0.16+8");
        if(want.greaterThanOrEqualTo(bad) && installed.lessThan(bad) ||
                installed.greaterThanOrEqualTo(bad) && want.lessThan(bad)) {
                // Force download
                // Fixes "Hash of java.rmi differs from expected hash"
                downloadJdk = true;
        }
        return downloadJdk;
    }

    private String getJavaVendor(String arch) {
        String vendor;
        switch(SystemUtilities.getJreArch(arch)) {
            case AARCH64:
                vendor = JAVA_ARM64_VENDOR;
                break;
            case X86_64:
            default:
                vendor = JAVA_AMD64_VENDOR;
        }
        return vendor;
    }

    /**
     * Download the JDK and return the path it was extracted to
     */
    private static String downloadJdk(String javaVendor, String platform, String arch, String gcEngine) throws IOException {
        if(platform == null) {
            // Must match ArgValue.JLINK --platform values
            switch(SystemUtilities.getOsType()) {
                case MAC:
                    platform = "mac";
                    break;
                case WINDOWS:
                    platform = "windows";
                    break;
                default:
                    platform = "linux";
            }

            log.info("No platform specified, assuming '{}'", platform);
        }

        arch = VendorArch.match(javaVendor, arch, JAVA_DEFAULT_ARCH);
        platform = VendorOs.match(javaVendor, platform);


        if(gcEngine == null) {
            gcEngine = JAVA_DEFAULT_GC_ENGINE;
            log.info("No garbage collector specified, assuming '{}'", gcEngine);
        }

        String fileExt;
        switch(VendorUrlPattern.getVendor(javaVendor)) {
            case BELL:
                fileExt = platform.equals("linux") ? "tar.gz" : "zip";
                break;
            case ADOPT:
            default:
                fileExt = platform.equals("windows") ? "zip" : "tar.gz";
        }

        String url = VendorUrlPattern.format(javaVendor, arch, platform, gcEngine, JAVA_MAJOR, JAVA_VERSION, JAVA_VERSION_FILE, fileExt);

        // Saves to out e.g. "out/jlink/jdk-AdoptOpenjdk-amd64-platform-11_0_7"
        String extractedJdk = new Fetcher(String.format("jlink/jdk-%s-%s-%s-%s", javaVendor.toLowerCase(Locale.ENGLISH), arch, platform, JAVA_VERSION_FILE), url)
                .fetch()
                .uncompress();

        // Get first subfolder, e.g. jdk-11.0.7+10
        for(File subfolder : new File(extractedJdk).listFiles(pathname -> pathname.isDirectory())) {
            extractedJdk = subfolder.getPath();
            if(platform.equals("mac") && Paths.get(extractedJdk, "/Contents/Home").toFile().isDirectory()) {
                extractedJdk += "/Contents/Home";
            }
            log.info("Selecting JDK home: {}", extractedJdk);
            break;
        }

        return extractedJdk;
    }

    private JLink calculateJarPath() {
        if(SystemUtilities.isJar()) {
            jarPath = SystemUtilities.getJarPath();
        } else {
            // Detect out/dist/qz-tray.jar for IDE usage
            jarPath = SystemUtilities.getJarParentPath()
                    .resolve("../../")
                    .resolve(Constants.PROPS_FILE + ".jar");
        }
        log.info("Assuming jar path: {}", jarPath);
        return this;
    }

    private JLink calculateOutPath() {
        if(targetPlatform.equals("mac")) {
            outPath = jarPath.resolve("../Java.runtime/Contents/Home").normalize();
        } else {
            outPath = jarPath.resolve("../runtime").normalize();
        }
        log.info("Assuming output path: {}", outPath);
        return this;
    }

    private JLink calculateToolPaths(Path javaHome) throws IOException {
        if(javaHome == null) {
            javaHome = Paths.get(System.getProperty("java.home"));
        }
        log.info("Using JAVA_HOME: {}", javaHome);
        jdepsPath = javaHome.resolve("bin").resolve(SystemUtilities.isWindows() ? "jdeps.exe" : "jdeps").normalize();
        jlinkPath = javaHome.resolve("bin").resolve(SystemUtilities.isWindows() ? "jlink.exe" : "jlink").normalize();
        log.info("Assuming jdeps path: {}", jdepsPath);
        log.info("Assuming jlink path: {}", jlinkPath);
        jdepsPath.toFile().setExecutable(true, false);
        jlinkPath.toFile().setExecutable(true, false);
        jdepsVersion = SystemUtilities.getJavaVersion(jdepsPath);
        return this;
    }

    private JLink calculateDepList() throws IOException {
        log.info("Calling jdeps to determine runtime dependencies");
        depList = new LinkedHashSet<>();

        // JDK11.0.11+requires suppressing of missing deps
        String raw = jdepsVersion.compareTo(Version.valueOf("11.0.10")) > 0 ?
                ShellUtilities.executeRaw(jdepsPath.toString(), "--multi-release", "9", "--list-deps", "--ignore-missing-deps", jarPath.toString()) :
                ShellUtilities.executeRaw(jdepsPath.toString(), "--multi-release", "9", "--list-deps", jarPath.toString());
        if (raw == null || raw.trim().isEmpty() || raw.trim().startsWith("Warning") ) {
            throw new IOException("An unexpected error occurred calling jdeps.  Please check the logs for details.\n" + raw);
        }
        for(String item : raw.split("\\r?\\n")) {
            item = item.trim();
            if(!item.isEmpty()) {
                if(item.startsWith("JDK") || item.startsWith("jdk8internals")) {
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
        // "jar:" URLs create transient zipfs dependency, see https://stackoverflow.com/a/57846672/3196753
        depList.add("jdk.zipfs");
        // fix for https://github.com/qzind/tray/issues/894 solution from https://github.com/adoptium/adoptium-support/issues/397
        depList.add("jdk.crypto.ec");
        return this;
    }

    private JLink deployJre() throws IOException {
        if(targetPlatform.equals("mac")) {
            // Deploy Contents/MacOS/libjli.dylib
            Path macOS = Files.createDirectories(outPath.resolve("../MacOS").normalize());
            Path jliLib = macOS.resolve("libjli.dylib");
            log.info("Deploying {}", macOS);
            try {
                // Not all jdks use a bundle format, but try this first
                Files.copy(jmodsPath.resolve("../../MacOS/libjli.dylib").normalize(), jliLib, StandardCopyOption.REPLACE_EXISTING);
            } catch(IOException ignore) {
                // Fallback to flat format
                Files.copy(jmodsPath.resolve("../lib/jli/libjli.dylib").normalize(), jliLib, StandardCopyOption.REPLACE_EXISTING);
            }

            // Deploy Contents/Info.plist
            HashMap<String, String> fieldMap = new HashMap<>();
            fieldMap.put("%BUNDLE_ID%", MacUtilities.getBundleId() + ".jre"); // e.g. io.qz.qz-tray.jre
            fieldMap.put("%BUNDLE_VERSION%", String.format("%s.%s.%s", JAVA_MAJOR, JAVA_MINOR, JAVA_PATCH));
            fieldMap.put("%BUNDLE_VERSION_FULL%", JAVA_VERSION);
            fieldMap.put("%BUNDLE_VENDOR%", javaVendor);
            log.info("Deploying {}/Info.plist", macOS.getParent());
            FileUtilities.configureAssetFile("assets/mac-runtime.plist.in", macOS.getParent().resolve("Info.plist"), fieldMap, JLink.class);
        }

        FileUtils.deleteQuietly(outPath.toFile());

        if(ShellUtilities.execute(jlinkPath.toString(),
                                  "--strip-debug",
                                  "--compress=2",
                                  "--no-header-files",
                                  "--no-man-pages",
                                  "--exclude-files=glob:**/legal/**",
                                  "--module-path", jmodsPath.toString(),
                                  "--add-modules", String.join(",", depList),
                                  "--output", outPath.toString())) {
            log.info("Successfully deployed a jre to {}", outPath);

            // Remove all but java/javaw
            String[] keepFiles;
            String keepExt;
            if(targetPlatform.equals("windows")) {
                keepFiles = new String[]{ "java.exe", "javaw.exe" };
                // Windows stores ".dll" files in bin
                keepExt = ".dll";
            } else {
                keepFiles = new String[]{ "java" };
                keepExt = null;
            }

            Files.list(outPath.resolve("bin")).forEach(binFile -> {
                if(Files.isDirectory(binFile) || (keepExt != null && binFile.toString().endsWith(keepExt))) {
                    log.info("Keeping {}", binFile);
                    return; // iterate forEach
                }
                for(String name : keepFiles) {
                    if (binFile.endsWith(name)) {
                        log.info("Keeping {}", binFile);
                        return; // iterate forEach
                    }
                }
                log.info("Deleting {}", binFile);
                binFile.toFile().delete();
            });

            return this;

        }
        throw new IOException("An error occurred deploying the jre.  Please check the logs for details.");
    }

}
