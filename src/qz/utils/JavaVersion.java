package qz.utils;

import com.github.zafarkhaja.semver.Version;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Java version handling including CLI parsing, special nuance handling of <code>Runtime.Version</code>
 * and legacy "1.x" handling.
 *
 * <p>
 *     This class aims to handle all historical and future nuances caused by Java's incompliance and inconsistencies despite the great unlikelihood of some of these
 * past decisions ever being relevant in the future.
 * </p>
 *
 * <p>
 *     Starting with Java 9, <code>Runtime.Version</code> is Java's officially supported way for handling Java version
 * handling, but this library is not fully <a href="https://semver.org/">semver</a> compliant as it prohibits certain items
 * such as trailing zeros yet appends non-standard items such as "-LTS" to the build information.  These discrepancies
 * break predictability.  For example <code>"11.0.3+23-LTS"</code> stores <code>"+23"</code> and <code>"LTS"</code> as
 * separate build meta-data, which is equivalent to <code>"11.0.3+23.LTS"</code> (notice the <code>"-"</code> has been
 * replaced with a <code>"."</code>), so we try to strip that off as it's irrelevant for semantic comparison. Additionally,
 * Java historically has conflated certain numbering.  For example, <b>"Java 8"</b> was internally known as <code>"1.8.0"</code>
 * and would report as <code>"1.8.0"</code> so parsing <code>"8.0"</code> should always match <code>"1.8.0"</code> .
 * Worse yet, the old build information was formatted <code>"1.8.0_202"</code> which can't be parsed by ANY semver library,
 * so special considerations must be made to sanitize all possible input variants.
 * </p>
 */
public class JavaVersion {

    /**
     * Returns the current <code>Runtime.version()</code> as a semantic version
     */
    public static Version current() {
        return toSemantic(Runtime.version());
    }

    /**
     * Parses the input and converts to a semantic version.  Allows both "bare" formatted data such as <code>"11.0"</code>
     * as well as a "wall of text" such as that outputted by <code>"java --version"</code> and handles any sanitization
     * thereof.
     */
    public static Version parse(String rawInput) {
        String isolated = isolate(rawInput);
        String sanitized = sanitize(isolated);
        return parseStrict(sanitized);
    }

    /**
     * Call a java command (e.g. java) with "--version" and parse the output
     * The double dash "--" is since JDK9 but important to send the command output to stdout
     */
    public static Version parseCli(Path javaBin) {
        return parse(ShellUtilities.executeRaw(javaBin.toString(), "--version"));
    }

    /**
     * Parses an isolated and sanitized java version (e.g. "1.8" NOT "1.8.0") using
     * a combination of Java's internal versioning and a dedicated semver library.
     */
    static Version parseStrict(String javaVersion) {
        return toSemantic(Runtime.Version.parse(javaVersion));
    }

    /**
     * Handle nuances with Java's semver reporting.
     * e.g. Strip invalid "-LTS" suffix, coerce "Java 8 = 1.8.0", etc
     */
    static String sanitize(String fuzzyVersion) {
        String sanitized = fuzzyVersion;

        // Chomp off "-LTS"
        if(sanitized.endsWith("-LTS")) {
            sanitized = sanitized.substring(0, sanitized.length() - 4);
        }
        // Legacy formatting
        // isolate first digit
        String firstDigit = (sanitized.length() > 1 && sanitized.contains(".")) ?  sanitized.split("\\.")[0] : sanitized;
        switch(firstDigit) {
            case "8":
            case "7":
            case "6":
            case "5":
            case "4":
            case "3":
            case "2":
                sanitized = "1." + sanitized;
        }

        // Legacy versions: Replace "_" with "+", strip "-bNN" (e.g. "1.8.0_202-b08")
        if(sanitized.startsWith("1.")) {
            sanitized = sanitized.replaceFirst("_", "+");
            if(sanitized.contains("-b")) {
                sanitized = sanitized.split("-b", 2)[0];
            }
        }

        // Java prohibits trailing zeros for no fricken reason
        sanitized = sanitized.replaceAll("(\\.0)+(?=[+\\-]|$)", "");

        return sanitized;
    }

    /**
     * Convert <code>Runtime.Version</code> to a semver-compatible Version
     */
    static Version toSemantic(Runtime.Version rv) {
        Version converted = rv.pre().isPresent() ? Version.of(rv.feature(), rv.interim(), rv.update(), rv.pre().get()) :
                Version.of(rv.feature(), rv.interim(), rv.update());

        List<String> optionals = new ArrayList<>();
        rv.build().ifPresent(o -> optionals.add(o.toString()));
        rv.optional()
                .filter(val -> !"LTS".equals(val))
                .ifPresent(optionals::add);

        if(!optionals.isEmpty()) {
            converted = converted.withBuildMetadata(optionals.toArray(new String[0]));
        }
        return converted;
    }

    /**
     * Isolate the version string part from the raw input.  Can handle both bare version information
     * and output from <code>"java --version"</code>
     */
    static String isolate(String rawInput) {
        // Try to find the "(build 11.0.27+0)" line
        String buildMatch = "(build";
        if(rawInput.contains("\n")) {
            String[] lines = rawInput.split("\n");
            for(String line : lines) {
                int buildLoc = line.indexOf(buildMatch);
                if(buildLoc != -1) {
                    rawInput = line.substring(buildLoc + buildMatch.length() , line.length() -1);
                    break;
                }
            }
        }
        // Chomp off leading "openjdk", etc
        int i;
        for(i = 0; i < rawInput.length() - 1; i++) {
            if(ByteUtilities.isNumber(rawInput.substring(i, i+1))) {
                rawInput = rawInput.substring(i).trim();
                break;
            }
        }
        // Chomp off any trailing data
        if(rawInput.contains(" ")) {
            rawInput = rawInput.split(" ", 2)[0];
        }

        return rawInput;
    }
}
