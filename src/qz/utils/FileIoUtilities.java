package qz.utils;

import qz.communication.FileIO;
import qz.utils.FileUpdateHandler;
import qz.ws.PrintSocketClient;
import qz.ws.SocketConnection;
import qz.ws.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import org.eclipse.jetty.websocket.api.Session;

import java.io.File;

public class FileIoUtilities {

    private static final Logger log = LoggerFactory.getLogger(FileIoUtilities.class);

	public static void setupFile(final Session session, final String UID, SocketConnection connection, JSONObject params)
			throws JSONException {
		final String filePath = params.getString("path");
		if (connection.getFile(filePath) != null) {
			PrintSocketClient.sendError(session, UID, String.format("File [%s] is already open.", filePath));
			return;
		}

		try {
			log.debug(String.format("setupFile with params: %s", params.toString()));
			FileIO file = new FileIO(filePath, params.getBoolean("receiveCallbacks"));
			connection.addFile(filePath, file);
			file.setFileUpdateHandler(new FileUpdateHandler() {
				private String fileContents(File file) throws IOException {
					byte[] encoded = Files.readAllBytes(file.toPath());
					return new String(encoded, "UTF-8");
				}

				public void handleFileUpdate(File file) {
					try {
						String output = fileContents(file);
						StreamEvent event = new StreamEvent(StreamEvent.Stream.FILE, StreamEvent.Type.RECEIVE)
								.withData("filePath", file.getPath()).withData("output", output);
						log.debug("sendStream");
						PrintSocketClient.sendStream(session, event);

					} catch (IOException ex) {
						PrintSocketClient.sendError(session, UID, ex);
					}
				}
			});
			PrintSocketClient.sendResult(session, UID, null);
		} catch (IOException ex) {
			PrintSocketClient.sendError(session, UID, ex);
		}

	}
}
