package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.PrintRaw;
import qz.utils.PrintingUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test helper for dispatching raw image print conversions.
 * NOTE: Language is currently hard-coded to ZPL; will be parameterized later.
 */
public class TestHelper {

    public static class Result { public int ok; public int skipped; public int failed; }

    private static final Logger log = LogManager.getLogger(TestHelper.class);

    public enum Orientation { PORTRAIT, LANDSCAPE }

    private static Path printImageRaw(String format, Path sourcePath, Orientation orientation, Path outDir, LanguageType language) throws Exception {
        if (language == null) throw new Exception();
        Files.createDirectories(outDir);
        try { System.setProperty("security.data.protocols", "http,https,file"); } catch (Throwable ignore) {}

        String ext = language.name().toLowerCase(Locale.ENGLISH);
        String outName = String.format(Locale.ENGLISH, "raw-%s-%s.%s", format, orientation.name().toLowerCase(Locale.ENGLISH), ext);
        Path outFile = outDir.resolve(outName).toAbsolutePath().normalize();

        JSONObject printer = new JSONObject().put("file", outFile.toString());

        JSONObject options = new JSONObject();
        options.put("orientation", orientation == Orientation.PORTRAIT ? "portrait" : "landscape");
        options.put("units", "in");

        // Use defaults to avoid tiny pages in headless environments

        JSONArray data = new JSONArray();
        JSONObject dataObj = new JSONObject();

        // Per-format payload and options
        JSONObject dataOpts = new JSONObject();
        dataOpts.put("language", language.name());

        switch(format.toLowerCase(Locale.ENGLISH)) {
            case "html":
            case "image":
            case "pdf": {
                dataObj.put("type", "raw");
                dataObj.put("format", format);
                dataObj.put("flavor", "file");
                dataObj.put("data", sourcePath.toUri().toString());
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown format: " + format);
        }

        dataObj.put("options", dataOpts);
        data.put(dataObj);

        PrintOutput output = new PrintOutput(printer);
        PrintOptions printOptions = new PrintOptions(options, output, PrintingUtilities.Format.COMMAND);

        PrintRaw processor = new PrintRaw();
        processor.parseData(data, printOptions);
        processor.print(output, printOptions);
        processor.cleanup();

        log.info("Wrote raw {} output: {}", format, outFile);
        return outFile;
    }

    public static void runRawImageTest(Result r, String format, Path sourcePath, Orientation orientation, Path outDir, LanguageType language) {
        try {
            printImageRaw(format, sourcePath, orientation, outDir, language);
            if (r != null) r.ok++;
        } catch (UnsupportedOperationException uoe) {
            if (r != null) r.skipped++;
        } catch (Throwable t) {
            if (r != null) r.failed++;
        }
    }

    public static void requireExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Missing test resource: " + path.toAbsolutePath());
        }
    }

    /**
     * Assert that all files under actualRoot exactly match those under baselineRoot (names and bytes).
     * Throws IOException if any mismatch or missing/extra file is detected.
     */
    public static void assertMatchesBaseline(Path actualRoot, Path baselineRoot) throws IOException {
        if (!Files.isDirectory(baselineRoot)) {
            throw new IOException("Baseline missing: " + baselineRoot.toAbsolutePath());
        }
        if (!Files.isDirectory(actualRoot)) {
            throw new IOException("Output missing: " + actualRoot.toAbsolutePath());
        }

        Map<String, Path> baselineFiles = listFilesRecursive(baselineRoot);
        Map<String, Path> actualFiles = listFilesRecursive(actualRoot);

        // Compare file sets
        if (!actualFiles.keySet().equals(baselineFiles.keySet())) {
            String missing = baselineFiles.keySet().stream().filter(k -> !actualFiles.containsKey(k)).collect(Collectors.joining(", "));
            String extra = actualFiles.keySet().stream().filter(k -> !baselineFiles.containsKey(k)).collect(Collectors.joining(", "));
            throw new IOException("File set mismatch. Missing: [" + missing + "] Extra: [" + extra + "]");
        }

        // Compare contents
        for (String rel : baselineFiles.keySet()) {
            byte[] b1 = Files.readAllBytes(baselineFiles.get(rel));
            byte[] b2 = Files.readAllBytes(actualFiles.get(rel));
            if (b1.length != b2.length) {
                throw new IOException("Size mismatch for " + rel + ": baseline=" + b1.length + ", actual=" + b2.length);
            }
            for (int i = 0; i < b1.length; i++) {
                if (b1[i] != b2[i]) {
                    throw new IOException("Content mismatch for " + rel + " at byte index " + i);
                }
            }
        }
    }

    private static Map<String, Path> listFilesRecursive(Path root) throws IOException {
        Map<String, Path> files = new HashMap<>();
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path p : stream.filter(Files::isRegularFile).collect(Collectors.toList())) {
                String rel = root.relativize(p).toString().replace('\\', '/');
                files.put(rel, p);
            }
        }
        return files;
    }
}
