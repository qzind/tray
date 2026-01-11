package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.SkipException;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.PrintRaw;
import qz.utils.ArgValue;
import qz.utils.PrintingUtilities;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test helper for dispatching raw image print conversions.
 * NOTE: Language is currently hard-coded to ZPL; will be parameterized later.
 */
public class TestHelper {
    private static final Path RES_DIR = Paths.get("test/qz/printer/action/resources");

    public enum Orientation {
        PORTRAIT("portrait"),
        LANDSCAPE("landscape");

        private final String value;
        Orientation(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

    }
    public enum Format {
        IMAGE("image", "image_sample_bw.png"),
        PDF("pdf", "pdf_sample.pdf"),
        HTML("html", "raw_sample.html");

        private final String value;

        public final Path samplePath;
        Format(String value, String filename) {
            this.value = value;
            this.samplePath = Paths.get(filename);
        }

        @Override
        public String toString() {
            return value;
        }

    }
    public static void setupEnvironment() {
        // print to file is off by default. Override for our tests
        System.setProperty(ArgValue.SECURITY_PRINT_TOFILE.getMatch(), "true");
        System.setProperty(ArgValue.SECURITY_DATA_PROTOCOLS.getMatch(), "http,https,file");
    }

    public static JSONObject constructParams(LanguageType languageType, TestHelper.Orientation orientation, TestHelper.Format format) throws JSONException {
        JSONObject params = new JSONObject();

        JSONObject options = new JSONObject()
                .put("units", "in")
                .put("density", 203)
                .put("orientation", orientation);
        options.put("size", new JSONObject()
                .put("width", 4)
                .put("height", 6));

        params.put("options", options);

        //width and height only matter for non-image printing
        JSONObject dataOptions = new JSONObject()
                .put("pageWidth", 812)
                .put("pageHeight", 1218)
                .put("language", languageType.name().toLowerCase());

        JSONObject dataObj = new JSONObject()
                .put("type", "raw")
                .put("format", format)
                .put("flavor", "file")
                .put("data", RES_DIR.resolve(format.samplePath)
                        .toAbsolutePath()
                        .toUri())
                .put("options", dataOptions);

        params.put("data", new JSONArray().put(dataObj));

        switch(languageType) {
            case PGL:
                dataOptions.put("logoId", "test");
        }

        return params;
    }

    public static void printRaw(Path outFilePath, JSONObject params) throws Exception {
        JSONObject printer = new JSONObject().put("file", outFilePath.toString());
        PrintOutput output = new PrintOutput(printer);

        PrintOptions printOptions = new PrintOptions(params.getJSONObject("options"), output, PrintingUtilities.Format.COMMAND);

        PrintRaw processor = new PrintRaw();
        try {
            processor.parseData(params.getJSONArray("data"), printOptions);
        } catch(UnsupportedOperationException e) {
            if (e.getMessage().contains("ImageConverter missing for LanguageType:")) {
                throw new SkipException("No image converter for this language, skipping");
            } else {
                throw e;
            }
        }
        processor.print(output, printOptions);
        processor.cleanup();
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
