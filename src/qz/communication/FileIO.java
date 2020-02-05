package qz.communication;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.ArrayList;

public class FileIO {
    public static final String SANDBOX_DATA_SUFFIX = "sandbox";
    public static final String GLOBAL_DATA_SUFFIX = "shared";
    public static final int FILE_LISTENER_DEFAULT_LINES = 10;

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

    private IOCase caseSensitivity;

    private ArrayList<String> inclusions;
    private ArrayList<String> exclusions;

    public FileIO(Session session, JSONObject params, Path originalPath, Path absolutePath) throws JSONException {
        this.session = session;
        this.originalPath = originalPath;
        this.absolutePath = absolutePath;

        inclusions = new ArrayList<>();
        exclusions = new ArrayList<>();

        JSONArray inc = params.optJSONArray("include");
        JSONArray exc = params.optJSONArray("exclude");
        caseSensitivity = params.optBoolean("ignoreCase", true) ? IOCase.INSENSITIVE : IOCase.SENSITIVE;

        if (inc != null) {
            for (int i = 0; i < inc.length(); i++) {
                inclusions.add(inc.getString(i));
            }
        }
        if (exc != null) {
            for(int i = 0; i < exc.length(); i++) {
                exclusions.add(exc.getString(i));
            }
        }

        JSONObject options = params.optJSONObject("listener");
        if (options != null) {
            // Setup defaults
            bytes = options.optLong("bytes", -1);
            if (bytes > 0) {
                readType = ReadType.BYTES;
            } else {
                readType = ReadType.LINES;
            }

            lines = options.optInt("lines", readType == ReadType.LINES? FILE_LISTENER_DEFAULT_LINES:-1);
            reversed = options.optBoolean("reverse", readType == ReadType.LINES);
        }
    }

    public boolean isMatch(String fileName) {
        boolean match = inclusions.isEmpty();
        for (String inclusion : inclusions) {
            if(FilenameUtils.wildcardMatch(fileName, inclusion, caseSensitivity)) {
                match = true;
                break;
            }
        }

        if(match) {
            for(String exclusion : exclusions) {
                if (FilenameUtils.wildcardMatch(fileName, exclusion, caseSensitivity)) {
                    match = false;
                    break;
                }
            }
        }
        return match;
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
