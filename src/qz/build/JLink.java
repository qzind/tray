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
import qz.build.jlink.Parsable;
import qz.common.Constants;
import qz.utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class JLink {
    private static final Logger log = LogManager.getLogger(JLink.class);
    public static final Vendor JAVA_VENDOR = Vendor.BELLSOFT;
    private static final String JAVA_VERSION = "11.0.17+7";
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

    private Version javaSemver;

    private LinkedHashSet<String> depList;

    /**
     * TODO: Move this to a dedicated class
     */
    public enum Vendor implements Parsable {
        ECLIPSE("Eclipse", "Adoptium", "adoptium", "temurin", "adoptopenjdk"),
        BELLSOFT("BellSoft", "Liberica", "bellsoft", "liberica"),
        IBM("IBM", "Semeru", "ibm", "semeru"),
        MICROSOFT("Microsoft", "OpenJDK", "microsoft"),
        AMAZON("Amazon", "Corretto", "amazon", "corretto");

        public String vendorName;
        public String productName;
        public final String[] matches;
        Vendor(String vendorName, String productName, String ... matches) {
            this.matches = matches;
            this.vendorName = vendorName;
            this.productName = productName;
        }

        public static Vendor parse(String value, Vendor fallback) {
            return Parsable.parse(Vendor.class, value, fallback);
        }

        public static Vendor parse(String value) {
            return Parsable.parse(Vendor.class, value);
        }

        @Override
        public String value() {
            return Parsable.value(this);
        }

        public String getVendorName() {
            return vendorName;
        }

        public String getProductName() {
            return productName;
        }

        public String getUrlArch(Arch arch) {
            switch(arch) {
                case AARCH64:
                    // All vendors seem to use "aarch64" universally
                    return "aarch64";
                case AMD64:
                    switch(this) {
                        // BellSoft uses "amd64"
                        case BELLSOFT:
                            return "amd64";
                    }
                default:
                    return "x64";
            }
        }

        public String getUrlPlatform(Platform platform) {
            switch(platform) {
                case MAC:
                    switch(this) {
                        case BELLSOFT:
                            return "macos";
                        case MICROSOFT:
                            return "macOS";
                    }
                default:
                    return platform.value();
            }
        }

        public String getUrlExtension(Platform platform) {
            switch(this) {
                case BELLSOFT:
                    switch(platform) {
                        case LINUX:
                            return "tar.gz";
                        default:
                            // BellSoft uses "zip" for mac and windows platforms
                            return "zip";
                    }
                default:
                    switch(platform) {
                        case WINDOWS:
                            return "zip";
                        default:
                            return "tar.gz";
                    }
            }
        }
    }

    /**
     * Handling of architectures
     */
    public enum Arch implements Parsable {
        AMD64("amd64", "x86_64", "x64"),
        AARCH64("aarch64", "arm64");

        public final String[] matches;
        Arch(String ... matches) { this.matches = matches; }

        public static Arch parse(String value, Arch fallback) {
            return Parsable.parse(Arch.class, value, fallback);
        }

        public static Arch parse(String value) {
            return Parsable.parse(Arch.class, value);
        }

        public static Arch getCurrentArch() {
            return parse(System.getProperty("os.arch"));
        }

        @Override
        public String value() {
            return Parsable.value(this);
        }
    }

    /**
     * Handling of platform names as they would appear in a URL
     * Values added must also be added to <code>ArgValue.JLINK --platform</code> values
     */
    public enum Platform implements Parsable {
        MAC("mac"),
        WINDOWS("windows"),
        LINUX("linux");

        public final String[] matches;
        Platform(String ... matches) { this.matches = matches; }

        public static Platform parse(String value, Platform fallback) {
            return Parsable.parse(Platform.class, value, fallback);
        }

        public static Platform parse(String value) {
            return Parsable.parse(Platform.class, value);
        }

        public static Platform getCurrentPlatform() {
            switch(SystemUtilities.getOsType()) {
                case MAC:
                    return Platform.MAC;
                case WINDOWS:
                    return Platform.WINDOWS;
                case LINUX:
                default:
                    return Platform.LINUX;
            }
        }

        @Override
        public String value() {
            return Parsable.value(this);
        }
    }

    public JLink(String targetPlatform, String targetArch, String javaVendor, String javaVersion, String gcEngine, String gcVersion) throws IOException {
        this.hostPlatform = Platform.getCurrentPlatform();
        this.hostArch = Arch.getCurrentArch();

        this.targetPlatform = Platform.parse(targetPlatform, this.hostPlatform);
        this.targetArch = Arch.parse(targetArch, this.hostArch);
        this.javaVendor =  Vendor.parse(javaVendor, JAVA_VENDOR);
        this.gcEngine = StringUtils.defaultIfBlank(gcEngine, JAVA_DEFAULT_GC_ENGINE);
        this.javaVersion = StringUtils.defaultIfBlank(javaVersion, JAVA_VERSION);
        this.gcVersion = StringUtils.defaultIfBlank(gcVersion, JAVA_DEFAULT_GC_VERSION);

        this.javaSemver = SystemUtilities.getJavaVersion(this.javaVersion);

        if(needsDownload(javaSemver, Constants.JAVA_VERSION)) {
            log.warn("Java versions are incompatible, locating a suitable runtime for Java " + javaSemver.getMajorVersion() + "...");
            String hostJdk = downloadJdk(this.hostArch, this.hostPlatform);
            calculateToolPaths(Paths.get(hostJdk));
        } else {
            calculateToolPaths(null);
        }

        String targetJdk = downloadJdk(this.targetArch, this.targetPlatform);
        jmodsPath = Paths.get(targetJdk, "jmods");
        log.info("Selecting jmods folder: {}", jmodsPath);

        calculateJarPath()
                .calculateOutPath()
                .calculateDepList()
                .deployJre();
    }

    public static void main(String ... args) throws IOException {
        JLink jlink = new JLink(null, null, null, null, null, null).calculateJarPath();
        System.out.println(jlink.jarPath);
        if(true) {
            System.exit(0);
        }


        new JLink(args.length > 0 ? args[0] : null,
                  args.length > 1 ? args[1] : null,
                  args.length > 2 ? args[2] : null,
                  args.length > 3 ? args[3] : null,
                  args.length > 4 ? args[4] : null,
                  args.length > 5 ? args[5] : null);
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
        // FIXME:  log.info("No platform specified, assuming '{}'", platform);
        // FIXME:  log.info("No garbage collector specified, assuming '{}'", gcEngine);

        String url = VendorUrlPattern.format(this.javaVendor, arch, platform, this.gcEngine, this.javaSemver, this.gcVersion);

        String javaVersionUnderscore = javaSemver.toString().replaceAll("\\+", "_");

        // Saves to out e.g. "out/jlink/jdk-AdoptOpenjdk-amd64-platform-11_0_7"
        String extractedJdk = new Fetcher(String.format("jlink/jdk-%s-%s-%s-%s", javaVendor.value(), arch.value(), platform.value(), javaVersionUnderscore), url)
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
                    .resolve("../../")
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
            fieldMap.put("%BUNDLE_VERSION_FULL%", JAVA_VERSION);
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

}
