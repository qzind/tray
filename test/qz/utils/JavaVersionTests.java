package qz.utils;

import com.github.zafarkhaja.semver.Version;
import org.testng.Assert;
import org.testng.annotations.Test;
import qz.build.JLink;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import static qz.utils.SystemUtilities.*;

public class JavaVersionTests {
    @Test
    public static void javaVersionTests() {
        // Missing minor, patch
        Assert.assertEquals(Version.parse("25.0.0+1"),
                            getJavaVersion("openjdk 25 2025-01-21\n" +
                                                   "OpenJDK Runtime Environment (build 25+1)\n" +
                                                   "OpenJDK 64-Bit Server VM (build 25+1, mixed mode, sharing)")
        );

        // Strip "-LTS"
        Assert.assertEquals(Version.parse("25.0.2+12"),
                            getJavaVersion("openjdk 25.0.2 2026-01-20 LTS\n" +
                                                   "OpenJDK Runtime Environment (build 25.0.2+12-LTS)\n" +
                                                   "OpenJDK 64-Bit Server VM (build 25.0.2+12-LTS, mixed mode, sharing)")
        );

        // JDK11
        Assert.assertEquals(Version.parse("11.0.27+0"),
                            getJavaVersion("openjdk 11.0.27 2025-04-15\n" +
                                                   "OpenJDK Runtime Environment Homebrew (build 11.0.27+0)\n" +
                                                   "OpenJDK 64-Bit Server VM Homebrew (build 11.0.27+0, mixed mode)")
        );

        // Homebrew's bare number format
        Assert.assertEquals(Version.parse("25.0.0"),
                            getJavaVersion("openjdk 25 2025-09-16\n" +
                                                   "OpenJDK Runtime Environment Homebrew (build 25)\n" +
                                                   "OpenJDK 64-Bit Server VM Homebrew (build 25, mixed mode, sharing)"));

        // Oracle Java + _JAVA_OPTIONS pollution
        Assert.assertEquals(Version.parse("11.0.4+10"),
                            getJavaVersion("Picked up _JAVA_OPTIONS: - Xmx512M\n" +
                                                   "java version \"11.0.4\" 2019-07-16 LTS\n" +
                                                   "Java(TM) SE Runtime Environment 18.9 (build 11.0.4+10-LTS)\n" +
                                                   "Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.4+10-LTS, mixed mode)"));

        // Old "cursed" 1.8 format
        Assert.assertEquals(Version.parse("1.8.0+202"),
                            getJavaVersion("java version \"1.8.0_202\"\n" +
                                                   "Java(TM) SE Runtime Environment (build 1.8.0_202-b08)\n" +
                                                   "Java HotSpot (TM) 64-Bit Server VM (huild 25.202-h08, mixed mode)"));

        // JLink internal class version
        Assert.assertTrue(getJavaVersion(JLink.JAVA_DEFAULT_VERSION).majorVersion() >= 11);

        // Currently installed Java version
        Assert.assertTrue(getJavaVersion().majorVersion() >= 11);

        // From ant properties
        Properties antProperties = new Properties();
        try {
            antProperties.load(new FileReader(Paths.get("ant/project.properties").toAbsolutePath().toFile()));
        } catch(IOException e) {
            System.err.printf("Can't load properties file: %s", e.getLocalizedMessage());
        }
        String javaVersion = antProperties.getProperty("jlink.java.version");
        // Ensures version in project.properties doesn't get corrupted by our own complicated parsing logic
        Assert.assertEquals(Version.parse(javaVersion), getJavaVersion(javaVersion));
    }
}
