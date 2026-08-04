package qz.utils;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import qz.common.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileUtilitiesTests {
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        // Create a base temporary directory for isolated test runs
        tempDir = Files.createTempDirectory("test_dir_");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        // Cleanup any remaining temporary files created during testing
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        FileUtilities.deleteDirectory(tempDir);
    }

    @Test
    public void testConfigureAssetToFile() throws IOException {
        File tempFile = new File(tempDir.toFile(), "configured-asset.ini");
        FileUtilities.configureAssetToFile(
                this.getClass(),
                "resources/configurable-test-file.ini.in",
                new HashMap<>(Map.of("%SAMPLE_DATA%", "7890")),
                tempFile
        );

        Properties props = new Properties();
        try(FileInputStream fis = new FileInputStream(tempFile)) {
            props.load(fis);
            Assert.assertEquals(props.get("about_title"), Constants.ABOUT_TITLE);
            Assert.assertEquals(props.get("sample_data"), "7890");
            Assert.assertEquals(props.get("static_data"), "Static data");
        }
    }

    @Test
    public void testDeleteDirectory() throws IOException {
        Path toDelete = Files.createDirectory(tempDir.resolve("does_exist"));
        Path deletedFile = Files.createFile(toDelete.resolve("file_to_delete.txt"));

        Assert.assertTrue(Files.exists(toDelete), "Directory should exist before deletion.");

        FileUtilities.deleteDirectory(toDelete);

        Assert.assertFalse(Files.exists(deletedFile), "File should be deleted.");
        Assert.assertFalse(Files.exists(toDelete), "Directory should be deleted.");
    }

    @Test
    public void testDeleteDirectoryMissing() throws IOException {
        Path toDelete = tempDir.resolve("does_not_exist");

        Assert.assertFalse(Files.exists(toDelete), "Directory shouldn't exist");

        // Should execute without throwing an exception
        FileUtilities.deleteDirectory(toDelete);

        Assert.assertFalse(Files.exists(toDelete));
    }

    @Test
    public void testDeleteDirectoryNestedLinks() throws IOException {
        Path toDelete = Files.createDirectory(tempDir.resolve("does_exist"));
        Path toRetain = Files.createDirectory(tempDir.resolve("to_retain"));
        Path fileToRetain = Files.createFile(toRetain.resolve("file_to_retain.txt"));

        Path linkLocation = toDelete.resolve("to_retain"); // does_exist/to_retain

        // Make a symlink or a junction
        Assert.assertTrue(makeLink(toRetain, linkLocation));

        // Ensure we can see our file at the linked location  (e.g. does_exist/to_retain/file_to_retain.txt)
        Path fileNestedPath = linkLocation.resolve("file_to_retain.txt");
        Assert.assertTrue(Files.exists(fileNestedPath), String.format("File %s in linked location should exist", fileNestedPath));

        FileUtilities.deleteDirectory(toDelete);

        Assert.assertTrue(Files.exists(fileToRetain), String.format("File %s should outlive deletion of its parent", fileToRetain));
    }

    @Test
    public void testDeleteDirectoryWrongType() throws IOException {
        Path regularFile = Files.createFile(tempDir.resolve("regular_file.txt"));
        Assert.assertTrue(Files.exists(regularFile));

        FileUtilities.deleteDirectory(regularFile);

        Assert.assertTrue(Files.exists(regularFile), "Regular file should not be deleted by deleteDirectory.");
    }

    private static boolean makeLink(Path source, Path target) {
        if (SystemUtilities.isWindows()) {
            return ShellUtilities.execute("cmd", "/c", String.format("mklink /j \"%s\" \"%s\"", target.toString(), source.toString()));
        }
        return ShellUtilities.execute("ln", "-s", source.toString(), target.toString());
    }
}

