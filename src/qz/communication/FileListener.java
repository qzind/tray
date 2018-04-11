package qz.communication;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import java.nio.file.Path;

public class FileListener implements DeviceListener {

    private static final Logger log = LoggerFactory.getLogger(FileListener.class);

    public enum ReadType {
        BYTES,
        LINES
    }

    private final ReadType readType;
    private final boolean contents, reversed;
    private final long bytes;
    private final int lines;

    private Session session;

    public FileListener(Session session, JSONObject params) throws JSONException {
        this.session = session;
        contents = params.optBoolean("options", true);
        JSONObject options = params.optJSONObject("options");
        if (options == null) {
            options = new JSONObject();
        }
        // Setup defaults
        bytes = options.optLong("bytes", -1);
        if (bytes != -1) {
            readType = ReadType.BYTES;
        } else {
            readType = ReadType.LINES;
        }

        lines = options.optInt("lines", readType == ReadType.LINES ? Constants.FILE_LISTENER_DEFAULT_LINES : -1);
        reversed = options.optBoolean("reversed", readType == ReadType.LINES ? true : false);
    }

    public int getLines() {
        return lines;
    }

    public long getBytes() {
        return bytes;
    }

    public boolean isReversed() {
        return reversed;
    }

    public boolean returnsContents() {
        return contents;
    }

    public ReadType getReadType() {
        return readType;
    }

    public void fileChanged(Path path, String type, String fileData) {
        if (fileData == null) {
            PrintSocketClient.sendStream(session, createStreamAction(path.toString(), type));
        } else {
            PrintSocketClient.sendStream(session, createStreamAction(path.toString(), type, fileData));
        }
    }

    public void sendError(String message) {
        PrintSocketClient.sendStream(session, createStreamError(message));
    }

    private StreamEvent createStreamAction(String path, String type) {
        return new StreamEvent(StreamEvent.Stream.FILE, StreamEvent.Type.ACTION)
                .withData("file", path)
                .withData("eventType", type);
    }

    private StreamEvent createStreamAction(String path, String type, String fileData) {
        return new StreamEvent(StreamEvent.Stream.FILE, StreamEvent.Type.ACTION)
                .withData("file", path)
                .withData("fileData", fileData)
                .withData("eventType", type);
    }

    private StreamEvent createStreamError(String message) {
        return new StreamEvent(StreamEvent.Stream.FILE, StreamEvent.Type.ERROR)
                .withData("message", message);
    }

    @Override
    public void close() {
    }
}
