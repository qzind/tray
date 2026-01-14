package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.PrintRaw;
import qz.printer.action.raw.converter.MissingImageConverterException;
import qz.utils.ArgValue;
import qz.utils.PrintingUtilities;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * Test helper for dispatching raw image print conversions.
 */
public class RawImageTests {
    // NOTE: BASE_DIR is wiped when establishing baselines
    // Running a single test will delete other tests' baselines
    // Only run the full test class when ESTABLISH_BASELINE is true
    public static final boolean ESTABLISH_BASELINE = false;

    private static final Path OUT_DIR = Paths.get("./out/raw-image-tests");
    private static final Path BASE_DIR = Paths.get("./test/qz/printer/action/raw/raw-image-baseline");
    private static final Path RES_DIR = Paths.get("test/qz/printer/action/resources");

    @BeforeClass
    public void prepareDirectory() throws IOException {
        Files.createDirectories(OUT_DIR);
        Files.createDirectories(BASE_DIR);
        RawImageTests.cleanDirectory(OUT_DIR);
        if (ESTABLISH_BASELINE) RawImageTests.cleanDirectory(BASE_DIR);
    }

    public static void setupEnvironment() {
        // print to file is off by default. Override for our tests
        System.setProperty(ArgValue.SECURITY_PRINT_TOFILE.getMatch(), "true");
        System.setProperty(ArgValue.SECURITY_DATA_PROTOCOLS.getMatch(), "http,https,file");
    }

    /**
     * constructs a test matrix of [title, params] for each LanguageType
     */
    @DataProvider(name = "languages")
    public Object[][] languages() throws JSONException {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (LanguageType languageType : LanguageType.values()) {
            JSONObject params = RawImageTests.constructParams(languageType, PrintOptions.Orientation.PORTRAIT, PrintingUtilities.Format.IMAGE);
            retMatrix.add(new Object[]{languageType.slug(), params});
        }
        return retMatrix.toArray(new Object[0][]);
    }

    /**
     * constructs a test matrix of [title, params] for each Format with a biCreator (pixelConverter)
     */
    @DataProvider(name = "formats")
    public Object[][] pixel() throws JSONException {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (PrintingUtilities.Format format : PrintingUtilities.Format.values()) {
            if (!format.hasBiCreator()) continue;
            for ( PrintOptions.Orientation orientation : PrintOptions.Orientation.values()) {
                retMatrix.add(new Object[] {
                        String.format("%s-%s-%s", format.slug(), orientation.slug(), LanguageType.ZPL.slug()),
                        RawImageTests.constructParams(LanguageType.ZPL, orientation, format)
                });
            }
        }
        return retMatrix.toArray(new Object[0][]);
    }

    public static JSONObject constructParams(LanguageType languageType, PrintOptions.Orientation orientation, PrintingUtilities.Format format) throws JSONException {
        JSONObject params = new JSONObject();

        JSONObject options = new JSONObject()
                .put("orientation", orientation);

        //width and height only matter for non-image printing
        JSONObject dataOptions = new JSONObject()
                .put("language", languageType.slug());

        JSONObject dataObj = new JSONObject()
                .put("type", "raw")
                .put("format", format)
                .put("flavor", "file")
                .put("data", getResourceUri(format))
                .put("options", dataOptions);

        switch(format) {
            case HTML:
                options.put("density", 203);
                options.put("units", "in");
                options.put("size", new JSONObject()
                        .put("width", 4)
                        .put("height", 6));
                // no break, continue to pdf
            case PDF:
                dataOptions.put("pageWidth", 812);
                dataOptions.put("pageHeight", 1218);
        }

        switch(languageType) {
            case PGL:
                dataOptions.put("logoId", "test");
        }

        params.put("options", options);
        params.put("data", new JSONArray().put(dataObj));
        return params;
    }

    private static URI getResourceUri(PrintingUtilities.Format format) {
        // example resource html-sample.html
        String resourceName = String.format(
                "%s-sample.%s",
                format.slug(),
                format == PrintingUtilities.Format.IMAGE ? "png" : format.slug()
        );
        return RES_DIR.resolve(resourceName)
                .toAbsolutePath()
                .toUri();
    }

    @Test(dataProvider = "languages")
    public void testLanguagePrint(String title, JSONObject params) throws Exception {
        printRaw("language-" + title, params);
    }

    @Test(dataProvider = "formats")
    public void testFormatPrint(String title, JSONObject params) throws Exception {
        printRaw("format-" + title, params);
    }

    public static void printRaw(String title, JSONObject params) throws Exception {
        RawImageTests.setupEnvironment();

        Path outFilePath = OUT_DIR.resolve(title + "-test.bin");
        Path baselineFilePath = BASE_DIR.resolve(title + "-test.bin");

        JSONObject printer = new JSONObject().put("file", outFilePath);
        PrintOutput output = new PrintOutput(printer);

        PrintOptions printOptions = new PrintOptions(params.getJSONObject("options"), output, PrintingUtilities.Format.COMMAND);

        PrintRaw processor = new PrintRaw();
        try {
            processor.parseData(params.getJSONArray("data"), printOptions);
            processor.print(output, printOptions);
        } catch(UnsupportedOperationException e) {
            // PrintRaw.parseData wraps all exceptions as UnsupportedOperationException
            if (e.getCause() instanceof MissingImageConverterException) {
                // TestNG will mark this test as skipped
                if (Files.exists(baselineFilePath)) {
                    // We are trying to skip a print that had a valid baseline. That needs to cause a test fail.
                    throw new Exception(
                            "Print test shouldn't be skipped for a valid baseline: " + baselineFilePath
                    );
                }
                throw new SkipException(e.getMessage());
            } else {
                throw e;
            }
        } finally {
            processor.cleanup();
        }

        if (ESTABLISH_BASELINE) {
            Files.copy(outFilePath, baselineFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        RawImageTests.assertMatches(outFilePath, baselineFilePath);
    }

    public static void assertMatches(Path file1, Path file2) throws IOException {
        byte[] b1 = Files.readAllBytes(file1);
        byte[] b2 = Files.readAllBytes(file2);

        if (b1.length != b2.length) {
            throw new IOException("Size mismatch for " + file1 + ": baseline=" + b1.length + ", actual=" + b2.length);
        }
        // todo use Files.mismatch when jvm LL is 12+
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                throw new IOException("Content mismatch for " + file1 + " at byte index " + i);
            }
        }
    }

    public static void cleanDirectory(Path dir) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                Files.deleteIfExists(p);
            }
        }
    }
}
