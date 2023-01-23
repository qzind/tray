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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.build.jlink.Arch;
import qz.build.jlink.Platform;
import qz.build.jlink.Vendor;
import qz.build.jlink.Url;
import qz.common.Constants;
import qz.utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Properties;

public class JLink {
    private static final Logger log = LogManager.getLogger(JLink.class);
    public static final Vendor JAVA_DEFAULT_VENDOR = Vendor.BELLSOFT;
    private static final String JAVA_DEFAULT_VERSION = "11.0.17+7";
    private static final String JAVA_DEFAULT_GC_ENGINE = "hotspot"; // or "openj9"
    private static final String JAVA_DEFAULT_GC_VERSION = "0.35.0"; // openj9 gc only

    private Path jarPath;
    private Path jdepsPath;
    private Path jlinkPath;
    private Path jmodsPath;
    private Path outPath;
    private Version jdepsVersion;
    private Platform hostPlatform;
    private Platform targetPlatform;
    private Arch hostArch;
    private Arch targetArch;
    private Vendor javaVendor;
    private String gcEngine;
    private String javaVersion;
    private String gcVersion;

    private Path targetJdk;

    private Version javaSemver;

    private LinkedHashSet<String> depList;

    public JLink(String targetPlatform, String targetArch, String javaVendor, String javaVersion, String gcEngine, String gcVersion, String targetJdk) throws IOException {
        this.hostPlatform = Platform.getCurrentPlatform();
        this.hostArch = Arch.getCurrentArch();

        this.targetPlatform = Platform.parse(targetPlatform, this.hostPlatform);
        this.targetArch = Arch.parse(targetArch, this.hostArch);
        this.javaVendor =  Vendor.parse(javaVendor, JAVA_DEFAULT_VENDOR);
        this.gcEngine = getParam("gcEngine", gcEngine, JAVA_DEFAULT_GC_ENGINE);
        this.javaVersion = getParam("javaVersion", javaVersion, JAVA_DEFAULT_VERSION);
        this.gcVersion = getParam("gcVersion", gcVersion, JAVA_DEFAULT_GC_VERSION);

        this.javaSemver = SystemUtilities.getJavaVersion(this.javaVersion);

        // Optional: Provide the location of a custom JDK on the local filesystem
        if(!StringUtils.isEmpty(targetJdk)) {
            Path jdkPath = Paths.get(targetJdk);
            Properties jdkProps = new Properties();
            jdkProps.load(new FileInputStream(jdkPath.resolve("release").toFile()));
            String customVersion = jdkProps.getProperty("JAVA_VERSION");
            if(customVersion.contains("\"")) {
                customVersion = customVersion.split("\"")[1];
            }
            Version customSemver = SystemUtilities.getJavaVersion(customVersion);
            if(needsDownload(javaSemver, customSemver)) {
                // The "release" file doesn't have build info, so we can't auto-download :(
                if(javaSemver.getMajorVersion() != customSemver.getMajorVersion()) {
                    log.error("Error: jlink version {}.0 does not match target java.base version {}.0", javaSemver.getMajorVersion(), customSemver.getMajorVersion());
                } else {
                    // Handle edge-cases (e.g. JDK-8240734)
                    log.error("Error: jlink version {} is incompatible with target java.base version {}", javaSemver.getMajorVersion(), customSemver.getMajorVersion());
                }
                System.exit(2);
            }
            this.targetJdk = Paths.get(targetJdk);
        }

        // Determine if the version we're building with is compatible with the target version
        if (needsDownload(javaSemver, Constants.JAVA_VERSION)) {
            log.warn("Java versions are incompatible, locating a suitable runtime for Java " + javaSemver.getMajorVersion() + "...");
            String hostJdk = downloadJdk(this.hostArch, this.hostPlatform);
            calculateToolPaths(Paths.get(hostJdk));
        } else {
            calculateToolPaths(null);
        }

        if(this.targetJdk == null) {
            targetJdk = downloadJdk(this.targetArch, this.targetPlatform);
            jmodsPath = Paths.get(targetJdk, "jmods");
        } else {
            log.info("\"targetjdk\" was provided {}, skipping download", targetJdk);
            jmodsPath = this.targetJdk.resolve("jmods");
        }

        log.info("Selecting jmods folder: {}", jmodsPath);

        calculateJarPath()
                .calculateOutPath()
                .calculateDepList()
                .deployJre();
    }

    public static void main(String ... args) throws IOException {
        new JLink(null, null, null, null, null, null, null).calculateJarPath();
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

    /**
     * Download the JDK and return the path it was extracted to
     */
    private String downloadJdk(Arch arch, Platform platform) throws IOException {
        String url = new Url(this.javaVendor).format(arch, platform, this.gcEngine, this.javaSemver, this.gcVersion);

        // Saves to out e.g. "out/jlink/jdk-AdoptOpenjdk-amd64-platform-11_0_7"
        String extractedJdk = new Fetcher(String.format("jlink/jdk-%s-%s-%s-%s", javaVendor.value(), arch.value(), platform.value(), javaSemver.toString().replaceAll("\\+", "_")), url)
                .fetch()
                .uncompress();

        // Get first subfolder, e.g. jdk-11.0.7+10
        for(File subfolder : new File(extractedJdk).listFiles(pathname -> pathname.isDirectory())) {
            extractedJdk = subfolder.getPath();
            if(platform == Platform.MAC && Paths.get(extractedJdk, "/Contents/Home").toFile().isDirectory()) {
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
                    .resolve("dist")
                    .resolve(Constants.PROPS_FILE + ".jar");
        }
        log.info("Assuming jar path: {}", jarPath);
        return this;
    }

    private JLink calculateOutPath() {
        switch(targetPlatform) {
            case MAC:
                outPath = jarPath.resolve("../Java.runtime/Contents/Home").normalize();
                break;
            default:
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
        if(targetPlatform == Platform.MAC) {
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
            fieldMap.put("%BUNDLE_VERSION%", String.format("%s.%s.%s", javaSemver.getMajorVersion(), javaSemver.getMinorVersion(), javaSemver.getPatchVersion()));
            fieldMap.put("%BUNDLE_VERSION_FULL%", javaSemver.toString());
            fieldMap.put("%BUNDLE_VENDOR%", javaVendor.getVendorName());
            fieldMap.put("%BUNDLE_PRODUCT%", javaVendor.getProductName());
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
            switch(targetPlatform) {
                case WINDOWS:
                    keepFiles = new String[]{ "java.exe", "javaw.exe" };
                    // Windows stores ".dll" files in bin
                    keepExt = ".dll";
                    break;
                default:
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

    public static String getParam(String paramName, String value, String fallback) {
        if(value != null && !value.isEmpty() && !value.trim().isEmpty()) {
            return value;
        }
        log.info("No {} specified, assuming '{}'", paramName, fallback);
        return fallback;
    }
}
