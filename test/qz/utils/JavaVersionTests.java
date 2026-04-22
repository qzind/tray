package qz.utils;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.build.JLink;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import static qz.utils.JavaVersion.*;

public class JavaVersionTests {
    private static final Logger log = LogManager.getLogger(JavaVersionTests.class);

    @DataProvider(name = "versions")
    public Object[][] versions() {
        return new Object[][] {
                // final, sanitized, isolated, raw input
                {
                        "25.0.0+1",
                        "25+1",
                        "25+1",
                        "openjdk 25 2025-01-21\n" +
                                "OpenJDK Runtime Environment (build 25+1)\n" +
                                "OpenJDK 64-Bit Server VM (build 25+1, mixed mode, sharing)"
                },
                {
                        "25.0.2+12",
                        "25.0.2+12", // chomp off "-LTS"
                        "25.0.2+12-LTS",
                        "openjdk 25.0.2 2026-01-20 LTS\n" +
                                "OpenJDK Runtime Environment (build 25.0.2+12-LTS)\n" +
                                "OpenJDK 64-Bit Server VM (build 25.0.2+12-LTS, mixed mode, sharing)"
                },
                {
                        "11.0.27+0",
                        "11.0.27+0",
                        "11.0.27+0",
                        "openjdk 11.0.27 2025-04-15\n" +
                                "OpenJDK Runtime Environment Homebrew (build 11.0.27+0)\n" +
                                "OpenJDK 64-Bit Server VM Homebrew (build 11.0.27+0, mixed mode)"
                },
                {
                        "25.0.0",
                        "25",
                        "25",
                        "openjdk 25 2025-09-16\n" +
                                "OpenJDK Runtime Environment Homebrew (build 25)\n" +
                                "OpenJDK 64-Bit Server VM Homebrew (build 25, mixed mode, sharing)"
                },
                {
                        "11.0.4+10",
                        "11.0.4+10", // chomp off "-LTS"
                        "11.0.4+10-LTS",
                        "Picked up _JAVA_OPTIONS: - Xmx512M\n" +
                                "java version \"11.0.4\" 2019-07-16 LTS\n" +
                                "Java(TM) SE Runtime Environment 18.9 (build 11.0.4+10-LTS)\n" +
                                "Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.4+10-LTS, mixed mode)"
                },
                {
                        "1.8.0+202",
                        "1.8+202", // Runtime.Version doesn't permit trailing zeros
                        "1.8.0_202-b08",
                        "java version \"1.8.0_202\"\n" +
                                "Java(TM) SE Runtime Environment (build 1.8.0_202-b08)\n" +
                                "Java HotSpot (TM) 64-Bit Server VM (build 25.202-h08, mixed mode)"
                },
                {
                        "1.7.0+55",
                        "1.7+55", // Runtime.Version doesn't permit trailing zeros
                        "1.7.0_55-b13",
                        "java version \"1.7.0_55\"\n" +
                                "Java(TM) SE Runtime Environment (build 1.7.0_55-b13)\n" +
                                "Java HotSpot(TM) 64-Bit Server VM (build 24.55-b03, mixed mode)"
                },
                {
                        "27.0.0-ea+18.1643", // semver wants a dot not a dash :/
                        "27-ea+18-1643",
                        "27-ea+18-1643",
                        "openjdk 27-ea 2026-09-15\n" +
                                "OpenJDK Runtime Environment (build 27-ea+18-1643)\n" +
                                "OpenJDK 64-Bit Server VM (build 27-ea+18-1643, mixed mode, sharing)"
                },
                // Java 8 was actually Java 1.8.0
                {
                        "1.8.0",
                        "1.8",
                        "8",
                        "8"
                }
        };
    }

    /**
     * Test stdout values from java --version
     */
    @Test(dataProvider = "versions")
    public void rawInputTests(String finalExpected, String sanitizedExpected, String isolatedExpected, String rawInput) {
        // Ensure we can isolate "11.0.3" from "(build 11.0.3)"
        String isolatedActual = isolate(rawInput);
        log.trace("Comparing isolated values: '{}' : '{}'", isolatedExpected, isolatedActual);
        Assert.assertEquals(isolatedExpected, isolatedActual);

        // Sanitize the value to confirm with Runtime.Version parsing
        String sanitizedActual = sanitize(isolatedActual);
        log.trace("Comparing sanitized values: '{}' : '{}'", sanitizedExpected, sanitizedActual);
        Assert.assertEquals(sanitizedExpected, sanitizedActual);

        // Ensure we can actually parse the isolated value in a predicable fashion
        Version versionExpected = Version.parse(finalExpected);
        Version versionActual = parseStrict(sanitizedActual);
        log.trace("Comparing parsed values: '{}' : '{}'", versionExpected, versionActual);
        Assert.assertEquals(versionExpected, versionActual);
    }

    /**
     * Make sure the JVM and our internal classes pass the most basic of "smoke" tests by comparing the major version
     */
    @Test
    public void smokeTests() {
        // Ensure we're stripping off "-LTS" even for internal versioning
        Runtime.Version rv = Runtime.Version.parse("11.0.4+10-LTS");
        Version semverActual = JavaVersion.toSemantic(rv);
        Version semverExpected = Version.parse("11.0.4+10");
        log.trace("Comparing toSemantic values '{}' : '{}'", semverExpected, semverActual);
        Assert.assertEquals(semverActual, semverExpected);
        log.trace("Ensuring toSemantic stripped '-LTS' suffix '{}' : '{}'", "10", semverExpected.buildMetadata().get());
        Assert.assertFalse(semverExpected.buildMetadata().get().contains("-LTS"));

        // JLink internal class version
        Assert.assertTrue(parse(JLink.JAVA_DEFAULT_VERSION).majorVersion() >= 11);

        // Currently installed Java version
        Assert.assertTrue(current().majorVersion() >= 11);

        // From ant properties
        Properties antProperties = new Properties();
        try {
            antProperties.load(new FileReader(Paths.get("ant/project.properties").toAbsolutePath().toFile()));
        } catch(IOException e) {
            System.err.printf("Can't load properties file: %s", e.getLocalizedMessage());
        }
        String javaVersion = antProperties.getProperty("jlink.java.version");
        // Ensures version in project.properties doesn't get corrupted by our own complicated parsing logic
        Assert.assertEquals(Version.parse(javaVersion), parse(javaVersion));
    }
}
