package qz.printer.action.raw;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test helper for dispatching raw image print conversions.
 * NOTE: Language is currently hard-coded to ZPL; will be parameterized later.
 */
public class TestHelper {
    private static final Logger log = LogManager.getLogger(TestHelper.class);

    public static class Result {
        public int ok;
        public int skipped;
        public int failed;
        public HashSet<Exception> errors = new HashSet<>();

        public boolean passed() {
            return failed == 0;
        }

        public String getSummaryLine() {
            return "ok=" + ok + " skipped=" + skipped + " failed=" + failed;
        }

        public void logSummary() {
            log.info(getSummaryLine());
            for (Exception e : errors) {
                log.error(ExceptionUtils.getStackTrace(e).trim());
            }
            log.log(passed() ? Level.INFO : Level.ERROR,
                    "Result: {}", passed() ? "PASSED" : "FAILED"
            );
        }
    }

    public enum Orientation { PORTRAIT, LANDSCAPE }

    private static Path printImageRaw(String format, Path sourcePath, Orientation orientation, Path outDir, LanguageType language) throws Exception {
        if (language == null) throw new Exception();
        Files.createDirectories(outDir);

        String ext = language.name().toLowerCase(Locale.ENGLISH);
        String outName = String.format(Locale.ENGLISH, "raw-%s-%s.%s.raw", format, orientation.name().toLowerCase(Locale.ENGLISH), ext);
        Path outFile = outDir.resolve(outName).toAbsolutePath().normalize();

        JSONObject printer = new JSONObject().put("file", outFile.toString());

        JSONObject options = new JSONObject();
        options.put("orientation", orientation == Orientation.PORTRAIT ? "portrait" : "landscape");
        options.put("units", "in");

        JSONObject size = new JSONObject();
        size.put("width", 4);
        size.put("height", 6);
        options.put("size", size);

        JSONArray data = new JSONArray();
        JSONObject dataObj = new JSONObject();

        // Per-format payload and options
        JSONObject dataOpts = new JSONObject();
        dataOpts.put("pageWidth", 400);
        dataOpts.put("pageHeight", 600);
        dataOpts.put("logoId", "test");

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
            r.ok++;
        } catch (UnsupportedOperationException uoe) {
            r.skipped++;
        } catch (Exception e) {
            r.failed++;
            r.errors.add(e);
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
    public static void assertMatchesBaseline(Result r, Path actualRoot, Path baselineRoot) throws IOException {
        if (!Files.isDirectory(baselineRoot)) throw new IOException("Baseline directory missing: " + baselineRoot.toAbsolutePath());
        if (!Files.isDirectory(actualRoot)) throw new IOException("Output directory missing: " + actualRoot.toAbsolutePath());

        Set<Path> baselineFiles = getRelativeFileSet(baselineRoot);
        Set<Path> actualFiles = getRelativeFileSet(actualRoot);

        if (!baselineFiles.equals(actualFiles)) {
            Set<Path> missing = new HashSet<>(baselineFiles);
            missing.removeAll(actualFiles);

            Set<Path> extra = new HashSet<>(actualFiles);
            extra.removeAll(baselineFiles);

            r.errors.add(
                new IOException(
                        "File set mismatch. Missing: " + missing + " Extra: " + extra
                )
            );
            // We will continue the testing even after the fail. Make sure to avoid the missing files.
            baselineFiles.removeAll(missing);
            r.failed += missing.size() + extra.size();
        }

        // Compare contents
        fileLoop: for (Path relativePath : baselineFiles) {
            byte[] b1 = Files.readAllBytes(baselineRoot.resolve(relativePath));
            byte[] b2 = Files.readAllBytes(actualRoot.resolve(relativePath));
            if (b1.length != b2.length) {
                r.failed++;
                r.errors.add(
                        new IOException("Size mismatch for " + relativePath + ": baseline=" + b1.length + ", actual=" + b2.length)
                );
                continue;
            }
            // todo use Files.mismatch when jvm LL is 12+
            for (int i = 0; i < b1.length; i++) {
                if (b1[i] != b2[i]) {
                    r.failed++;
                    r.errors.add(
                            new IOException("Content mismatch for " + relativePath + " at byte index " + i)
                    );
                    continue fileLoop;
                }
            }
            r.ok++;
        }
    }

    private static Set<Path> getRelativeFileSet(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .collect(Collectors.toSet());
        }
    }
}
