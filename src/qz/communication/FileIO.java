package qz.communication;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import qz.common.Constants;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import java.nio.file.Path;
import java.nio.file.WatchKey;

public class FileIO {

    public enum ReadType {
        BYTES, LINES
    }

    private Session session;

    private Path originalPath;
    private Path absolutePath;

    private WatchKey wk;

    private ReadType readType;
    private boolean reversed;
    private long bytes;
    private int lines;

    public FileIO(Session session, JSONObject params, Path originalPath, Path absolutePath) throws JSONException {
        this.session = session;
        this.originalPath = originalPath;
        this.absolutePath = absolutePath;

        JSONObject options = params.optJSONObject("listener");
        if (options != null) {
            // Setup defaults
            bytes = options.optLong("bytes", -1);
            if (bytes > 0) {
                readType = ReadType.BYTES;
            } else {
                readType = ReadType.LINES;
            }

            lines = options.optInt("lines", readType == ReadType.LINES? Constants.FILE_LISTENER_DEFAULT_LINES:-1);
            reversed = options.optBoolean("reverse", readType == ReadType.LINES);
        }
    }

    public boolean returnsContents() {
        return bytes > 0 || lines > 0;
    }

    public ReadType getReadType() {
        return readType;
    }

    public boolean isReversed() {
        return reversed;
    }

    public long getBytes() {
        return bytes;
    }

    public int getLines() {
        return lines;
    }

    public Path getOriginalPath() {
        return originalPath;
    }

    public Path getAbsolutePath() {
        return absolutePath;
    }

    public boolean isWatching() {
        return wk != null && wk.isValid();
    }

    public void setWk(WatchKey wk) {
        this.wk = wk;
    }

    public void fileChanged(String fileName, String type, String fileData) {
        StreamEvent evt = new StreamEvent(StreamEvent.Stream.FILE, StreamEvent.Type.ACTION)
                .withData("file", getOriginalPath().resolve(fileName))
                .withData("eventType", type);

        if (fileData != null) {
            evt.withData("fileData", fileData);
        }

        PrintSocketClient.sendStream(session, evt);
    }

    public void sendError(String message) {
        StreamEvent eventErr = new StreamEvent(StreamEvent.Stream.FILE, StreamEvent.Type.ERROR)
                .withData("message", message);
        PrintSocketClient.sendStream(session, eventErr);
    }


    public void close() {
        if (wk != null) {
            wk.cancel();
        }
    }

}
