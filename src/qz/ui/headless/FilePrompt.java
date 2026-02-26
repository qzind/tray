package qz.ui.headless;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.ByteUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;

/**
 * Writes a prompt file and awaits a response
 */
public class FilePrompt implements Endpoint.Promptable {
    private final static Logger log = LogManager.getLogger(FilePrompt.class);
    private final static int READ_ATTEMPTS = 5;

    public static JSONObject filePrompt(String jsonData, Path request, Path response) throws InterruptedException, IOException, JSONException {
        ensureWritable(request);
        ensureReadable(response);

        // 1. Write the JSON content to a file
        Files.writeString(request, jsonData);
        log.info("Dialog request '{}' written, waiting for reply...", request);

        Path directory = response.getParent();
        if (directory == null) directory = Path.of(".");

        // 2. Setup WatchService
        try(WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            while(true) {
                WatchKey key;
                try {
                    key = watchService.take();
                    for(WatchEvent<?> event : key.pollEvents()) {
                        Path context = (Path)event.context();

                        // Try a few times to read JSON in the event we're mid-write
                        for(int i = 0; i < READ_ATTEMPTS; i++) {
                            try {
                                // 3. Check if the changed file is the one we want
                                if (context.endsWith(response.getFileName())) {
                                    //Thread.sleep(100); // Small buffer for OS file-lock release
                                    log.info("Dialog response '{}' found...", response);
                                    String jsonResponse = FileUtils.readFileToString(response.toFile(), StandardCharsets.UTF_8);
                                    JSONObject jsonObject = new JSONObject(jsonResponse);
                                    if (jsonObject.length() == 0) {
                                        continue; // maybe the file is still being written?
                                    }
                                    cleanup(request, response);
                                    return jsonObject;
                                }
                            }
                            catch(JSONException e) {
                                if (i == READ_ATTEMPTS - 1) {
                                    // Give up, JSON is malformed
                                    throw e;
                                }
                            }
                        }
                    }
                    if (!key.reset()) break;
                }
                catch(InterruptedException e) {
                    log.info("Dialog response '{}' cancelled, listener thread interrupted.", response);
                    cleanup(request, response);
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public JSONObject prompt(Endpoint endpoint, JSONObject data) {
        try {
            String content = data.toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hash = ByteUtilities.toHexString(digest.digest(content.getBytes(StandardCharsets.UTF_8)), false);
            String fileName = String.format("%s.json", hash);
            return filePrompt(content, endpoint.getRequestPath().resolve(fileName), endpoint.getResponsePath().resolve(fileName));
        } catch(Exception e) {
            String msg = String.format("An exception occurred while trying to use a %s against %s", endpoint.getType(), endpoint.getPath());
            log.error(msg, e);
        }
        return new JSONObject();
    }

    private static void cleanup(Path ... paths) {
         for(Path path : paths) {
             File file = path.toFile();
             if (file.exists() && file.canWrite()) {
                 if (!file.delete()) {
                     file.deleteOnExit();
                 }
             }
         }
    }

    /**
     * Throws <code>IOException</code> if <code>Path</code> can't be written to.
     * Will attempt to create parent directories as-needed.
     */
    private static void ensureWritable(Path path) throws IOException {
        if(path == null) {
            throw new IOException("Path can't be null");
        }

        File parent = path.getParent().toFile();
        if(!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create parent directories '" + parent + "'");
        }
        if(!parent.canWrite()) {
            throw new IOException("Unable to write to '" + path + "'");
        }
    }

    /**
     * Throws <code>IOException</code> if <code>Path</code> can't be read from.
     * Will attempt to create parent directories as-needed.
     */
    private static void ensureReadable(Path path) throws IOException {
        if(path == null) {
            throw new IOException("Path can't be null");
        }

        File parent = path.getParent().toFile();
        if(!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create parent directories '" + parent + "'");
        }

        if(!parent.canRead()) {
            throw new IOException("Unable to read from '" + path + "'");
        }
    }
}
