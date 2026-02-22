package qz.ui.headless;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class FilePrompt {
    /**
     * Writes a prompt file and awaits a response
     * e.g. HeadlessGatewayDialog_prompt.json --> HeadlessGateway_response.json
     */
    public static JSONObject filePrompt(Path fileToWrite, JSONObject dataToWrite, Path fileToWaitFor) throws IOException, InterruptedException, IOException, JSONException {
        // TODO: incorporate this into HeadlessDialog

        // 1. Write the JSON content to a file
        Files.writeString(fileToWrite, dataToWrite.toString());
        System.out.println("File written. Waiting for signal...");

        Path directory = fileToWaitFor.getParent();
        if (directory == null) directory = Path.of(".");

        // 2. Setup WatchService
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take(); // Blocks until an event occurs
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path context = (Path) event.context();

                    // 3. Check if the changed file is the one we want
                    if (context.endsWith(fileToWaitFor.getFileName())) {
                        Thread.sleep(100); // Small buffer for OS file-lock release
                        return new JSONObject(FileUtils.readFileToString(fileToWaitFor.toFile(), StandardCharsets.UTF_8));
                    }
                }
                if (!key.reset()) break;
            }
        }
        return null;
    }
}
