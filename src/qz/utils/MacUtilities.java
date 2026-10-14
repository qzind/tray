/*
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import qz.common.Constants;
import qz.utils.mac.NSApplication;
import qz.utils.mac.NSString;
import qz.utils.mac.NSUserDefaults;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
/**
 * Utility class for macOS specific functions.
 *
 * @author Tres Finocchiaro
 */
public class MacUtilities {
    private static final Logger log = LogManager.getLogger(MacUtilities.class);
    private static final boolean sandboxed = System.getenv("APP_SANDBOX_CONTAINER_ID") != null;

    private static String bundleId;

    public static void registerHandler(Desktop.Action action, ActionListener listener) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(action)) {
                switch (action) {
                    case APP_ABOUT -> desktop.setAboutHandler(e -> {
                        listener.actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, action.name()));
                    });
                    case APP_QUIT_HANDLER -> desktop.setQuitHandler((e, response) -> {
                        listener.actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, action.name()));
                        response.performQuit();
                    });
                    default -> log.warn("Unsupported action {}", action.name());
                }
            }
        }
    }

    /**
     * Calculates CFBundleIdentifier for macOS
     */
    public static String getBundleId() {
        if(bundleId == null) {
            ArrayList<String> parts = new ArrayList<>(Arrays.asList(Constants.ABOUT_URL.split("/")));
            for(String part : parts) {
                if(part.contains(".")) {
                    // Try to use this section as the .com, etc
                    String[] domain = part.toLowerCase(Locale.ENGLISH).split("\\.");
                    // Convert to reverse-domain syntax
                    for(int i = domain.length -1; i >= 0; i--) {
                        // Skip "www", "www2", etc
                        if(i == 0 && domain[i].startsWith("www")) {
                            break;
                        }
                        bundleId = (bundleId == null ? "" : bundleId) + domain[i] + ".";
                    }
                }
            }
        }
        if(bundleId != null) {
            bundleId += Constants.PROPS_FILE;
        } else {
            bundleId = "io.qz.fallback." + Constants.PROPS_FILE;
        }
        return bundleId;
    }

    /**
     * Runs a shell command to determine if "Dark" desktop theme is enabled
     */
    public static boolean isDarkDesktop() {
        try {
            return "Dark".equalsIgnoreCase(NSUserDefaults.standard().stringForKey(new NSString("AppleInterfaceStyle")).toString());
        } catch(Exception e) {
            log.warn("An exception occurred obtaining theme information, falling back to command line instead.");
            return !ShellUtilities.execute(new String[] {"defaults", "read", "-g", "AppleInterfaceStyle"}, new String[] {"Dark"}, true, true).isEmpty();
        }
    }

    public static int getScaleFactor() {
        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration graphicsConfig = graphicsDevice.getDefaultConfiguration();
        return (int)graphicsConfig.getDefaultTransform().getScaleX();
    }

    /**
     * The human-readable display version of the Mac
     */
    public static String getOsDisplayVersion() {
        StringBuilder displayVersion;
        String[] command = { "sw_vers" };
        String output = ShellUtilities.executeRaw(command);
        if(!output.trim().isEmpty()) {
            displayVersion = new StringBuilder();
            String[] lines = output.split("\\n");
            if (lines.length >= 3) {
                for(int line = 0; line < 3; line++) {
                    // Get value after ":", e.g. "ProductName:      macOS"
                    String[] parts = lines[line].split(":", 2);
                    if (parts.length > 1) {
                        if (line < 2) {
                            displayVersion.append(parts[1].trim()).append(" ");
                        } else {
                            displayVersion.append("(").append(parts[1].trim()).append(")");
                        }
                    }
                }
            }
        } else {
            displayVersion = new StringBuilder(System.getProperty("os.version", "0.0.0"));
        }

        return displayVersion.toString();
    }

    public static void setFocus() {
        try {
            NSApplication.sharedApplication().activateIgnoringOtherApps(true);
        } catch(Throwable t) {
            log.warn("Couldn't set focus using JNA, falling back to command line instead");
            ShellUtilities.executeAppleScript("tell application \"System Events\" \n" +
                                                      "set frontmost of every process whose unix id is " + UnixUtilities.getProcessId() + " to true \n" +
                                                        "end tell");
        }
    }

    public static boolean nativeFileCopy(Path source, Path destination) {
        Path tempFile = null;
        try {
            // AppleScript's "duplicate" requires an existing destination
            if (!destination.toFile().isDirectory()) {
                // To perform this in a single operation in AppleScript, the source and dest
                // file names must match.  Copy to a temp directory first to retain desired name.
                tempFile = Files.createTempDirectory("qz_cert_").resolve(destination.getFileName());
                log.debug("Copying {} to {} to obtain the desired name", source, tempFile);
                source = Files.copy(source, tempFile);
                destination = destination.getParent();
            }
            return ShellUtilities.executeAppleScript(
                    "tell application \"Finder\" to duplicate " +
                            "file (POSIX file \"" + source + "\" as alias) " +
                            "to folder (POSIX file \"" + destination + "\" as alias) " +
                            "with replacing");
        } catch(Throwable t) {
            log.warn("Unable to perform native file copy using AppleScript", t);
        } finally {
            if(tempFile != null) {
                FileUtils.deleteQuietly(tempFile.getParent().toFile());
            }
        }
        return false;
    }

    public static Document createXmlDocument(Path plist) throws ParserConfigurationException, IOException, SAXException {
        String rawXml = ShellUtilities.executeRaw("/usr/bin/plutil", "-convert", "xml1", "-o", "-", plist.toString());
        return XmlUtilities.createXmlDocument(new ByteArrayInputStream(rawXml.getBytes(StandardCharsets.UTF_8)));
    }

    public static boolean isSandboxed() {
        return sandboxed;
    }
}
