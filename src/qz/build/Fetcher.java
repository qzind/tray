package qz.build;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Fetches a zip or tarball from URL and decompresses it
 */
public class Fetcher {
    public enum Format {
        ZIP(".zip"),
        TARBALL(".tar.gz"),
        JSON(".json"),
        UNKNOWN(null);

        final String suffix;
        Format(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }

        public static Format parse(String url) {
            if(url.contains("?")) {
                url = url.substring(0, url.lastIndexOf("?"));
                log.debug("Stripped parameters from URL to help detecting file type: '{}'", url);
            }
            for(Format format : Format.values()) {
                if (format.getSuffix() != null && url.endsWith(format.getSuffix())) {
                    return format;
                }
            }
            return UNKNOWN;
        }
    }

    private static final Logger log = LogManager.getLogger(Fetcher.class);

    public static void main(String ... args) throws IOException {
        new Fetcher("jlink/qz-tray-src_x.x.x", "https://github.com/qzind/tray/archive/master.tar.gz").fetch().uncompress();
    }

    final String resourceName;
    final String url;
    final Format format;
    final Path rootDir;
    final Map<String, String> headers;

    File tempFile;
    File tempExtracted;
    File extracted;

    public Fetcher(String resourceName, String url, Format format, Path rootDir, Map<String, String> headers) {
        this.resourceName = resourceName;
        this.url = url;
        this.format = format;
        this.rootDir = rootDir;
        this.headers = headers;
    }

    public Fetcher(String resourceName, String url, Format format, Map<String, String> headers) {
        this(resourceName, url, format, SystemUtilities.getJarParentPath().getParent(), headers);
    }

    public Fetcher(String resourceName, String url, Map<String, String> headers) {
        this(resourceName, url, Format.parse(url), headers);
    }

    public Fetcher(String resourceName, String url) {
        this(resourceName, url, null);
    }

    public Fetcher fetch() throws IOException {
        extracted = new File(rootDir.toString(), resourceName);
        if(extracted.isDirectory() && extracted.exists() && Objects.requireNonNull(extracted.listFiles()).length > 0) {
            log.info("Resource '{}' from [{}] has already been downloaded and extracted.  Using: [{}]", resourceName, url, extracted);
        } else {
            tempExtracted = new File(rootDir.toString(), resourceName + "~tmp");
            if(tempExtracted.exists()) {
                FileUtils.deleteDirectory(tempExtracted);
            }
            // temp directory to thwart partial extraction
            if(tempExtracted.mkdirs()) {
                tempFile = File.createTempFile(resourceName, format == Format.JSON ? ".json" : ".zip");
                log.info("Fetching '{}' from [{}] and saving to [{}]", resourceName, url, tempFile);
                copyUrlToFile(new URL(url), tempFile.toPath(), headers);
            } else {
                throw new IOException(String.format("Unable to create directory for jdk extraction '%s'", tempExtracted));
            }
        }
        return this;
    }

    public void copyUrlToFile(URL url, Path targetPath, Map<String, String> headers) throws IOException {
        HttpRequest.Builder requestBuilder;
        try {
            requestBuilder = HttpRequest.newBuilder().uri(url.toURI()).GET();
            if(headers != null) {
                headers.forEach(requestBuilder::header);
            }
            HttpRequest request = requestBuilder.build();

            // stream response to file
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofFile(
                    targetPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            ));
        } catch(URISyntaxException e) {
            throw new IOException(String.format("Invalid URI specified '%s'", url), e);
        } catch(InterruptedException e) {
            throw new IOException(String.format("Request interrupted '%s'", url), e);
        }
    }

    public String uncompress() throws IOException {
        if(tempFile != null) {
            log.info("Unzipping '{}' from [{}] to [{}]", resourceName, tempFile, tempExtracted);
            if(format == Format.ZIP) {
                unzip(tempFile.getAbsolutePath(), tempExtracted);
            } else {
                untar(tempFile.getAbsolutePath(), tempExtracted);
            }
            log.info("Moving [{}] to [{}]", tempExtracted, extracted);
            tempExtracted.renameTo(extracted);
        }
        return extracted.toString();
    }

    public static void untar(String sourceFile, File targetDir) throws IOException {
        // TODO: Switch to TarArchiveInputStream from Apache Commons Compress
        if (!ShellUtilities.execute("tar", "-xzf", sourceFile, "-C", targetDir.getPath())) {
            throw new IOException("Something went wrong extracting " + sourceFile +", check logs for details");
        }
    }

    public static void unzip(String sourceFile, File targetDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(sourceFile))) {
            for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
                Path resolvedPath = targetDir.toPath().resolve(ze.getName());
                if (ze.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zipIn, resolvedPath);
                }
            }
        }
    }
}
