package qz.installer.apps;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for dealing with version nuances reported by various apps
 * <p>
 * Semver is seldomly followed by apps, so we'll try our best to parse a usable <code>Version</code>.
 * Versions are needed for comparisons, so <code>0.0.0</code> is always desired over an
 * <code>Exception</code> or <code>null</code>.
 * </p>
 */
public class AppVersionParser {
    private static final Logger log = LogManager.getLogger(AppVersionParser.class);

    public static Version parse(String version) {
        try {
            if(version == null) {
                version = "";
            }
            if(!version.isBlank()) {
                return Version.parse(version);
            }
        } catch(ParseException | NumberFormatException ignore) {
            // Chromium likes to use x.x.x.x
            // Firefox likes to use x.xa1, x.x-ESR
        }

        try {
            Version.Builder builder = new Version.Builder();
            String[] split = version.split("\\.", 4);
            // Generally speaking, the major version is the most important, but we'll try to get the other info too
            for(int i = 0; i < split.length; i++) {
                // Trust no one: overzealous stripping of any non-numeric data
                String[] parts = splitAtFirstNonNumber(split[i]);
                int numberPart = parts[0].isBlank() ? 0 : Integer.parseInt(parts[0]);
                String otherPart = parts[1];
                switch(i) {
                    case 0:
                        builder.setMajorVersion(numberPart);
                        appendMetaData(builder, otherPart);
                        break;
                    case 1:
                        builder.setMinorVersion(numberPart);
                        appendMetaData(builder, otherPart);
                        break;
                    case 2:
                        builder.setPatchVersion(numberPart);
                        appendMetaData(builder, otherPart);
                        break;
                    case 3:
                        // if we have a 4th value, blindly treat the entire thing as metadata
                        appendMetaData(builder, split[i]);
                        break;
                }
            }
            return builder.build();
        } catch(NumberFormatException | ParseException e) {
            log.warn("An problem occurred trying to parse version: '{}', we'll fallback to '0.0.0' instead", version, e);
        }
        return Version.of(0, 0, 0);
    }

    private static String[] splitAtFirstNonNumber(String input) {
        if (input == null || input.isEmpty()) {
            return new String[]{"", ""};
        }

        int length = input.length();
        int index = 0;

        while (index < length && Character.isDigit(input.charAt(index))) {
            index++;
        }
        return new String[] { input.substring(0, index),  input.substring(index) };
    }

    /**
     * Dynamically appends <code>preReleaseInfo</code> or <code>buildMetaData</code> based on some
     * known version patterns
     */
    private static void appendMetaData(Version.Builder builder, String metaData) {
        if(metaData == null || metaData.isBlank()) {
            return;
        }
        // Let's make a bold assumption that prerelease data and build metadata don't like to coexist
        if (builder.build().isPreRelease() || metaData.matches("(-|(alpha|beta|rc|pre|a|b))(?=[^a-zA-Z]|$).*")) {
            // assume prerelease
            String toAppend = metaData.startsWith("-")? metaData.substring(1):metaData;
            if(!toAppend.isBlank()) {
                builder.addPreReleaseIdentifiers(toAppend);
            }
        } else {
            // assume build metadata
            String toAppend = metaData.startsWith("+")? metaData.substring(1):metaData;
            builder.addBuildIdentifiers(toAppend);
        }
    }
}
