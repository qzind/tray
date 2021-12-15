package qz.build;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Fetches a zip or tarball from URL and decompresses it
 */
public class Fetcher {
    public enum Format {
        ZIP(".zip"),
        TARBALL(".tar.gz"),
        UNKNOWN(null);

        String suffix;
        Format(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }

        public static Format parse(String url) {
            for(Format format : Format.values()) {
                if (url.endsWith(format.getSuffix())) {
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

    String resourceName;
    String url;
    Format format;
    Path rootDir;
    File tempArchive;
    File tempExtracted;
    File extracted;

    public Fetcher(String resourceName, String url) {
        this.url = url;
        this.resourceName = resourceName;
        this.format = Format.parse(url);
        // Try to calculate out/
        this.rootDir = SystemUtilities.getJarParentPath().getParent();
    }

    @SuppressWarnings("unused")
    public Fetcher(String resourceName, String url, Format format, String rootDir) {
        this.resourceName = resourceName;
        this.url = url;
        this.format = format;
        this.rootDir = Paths.get(rootDir);
    }

    public Fetcher fetch() throws IOException {
        extracted = new File(rootDir.toString(), resourceName);
        if(extracted.isDirectory() && extracted.exists()) {
            log.info("Resource '{}' from [{}] has already been downloaded and extracted.  Using: [{}]", resourceName, url, extracted);
        } else {
            tempExtracted = new File(rootDir.toString(), resourceName + "~tmp");
            if(tempExtracted.exists()) {
                FileUtils.deleteDirectory(tempExtracted);
            }
            // temp directory to thwart partial extraction
            tempExtracted.mkdirs();
            tempArchive = File.createTempFile(resourceName, ".zip");
            log.info("Fetching '{}' from [{}] and saving to [{}]", resourceName, url, tempArchive);
            FileUtils.copyURLToFile(new URL(url), tempArchive);
        }
        return this;
    }

    public String uncompress() throws IOException {
        if(tempArchive != null) {
            log.info("Unzipping '{}' from [{}] to [{}]", resourceName, tempArchive, tempExtracted);
            if(format == Format.ZIP) {
                unzip(tempArchive.getAbsolutePath(), tempExtracted);
            } else {
                untar(tempArchive.getAbsolutePath(), tempExtracted);
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
