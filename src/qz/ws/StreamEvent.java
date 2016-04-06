package qz.ws;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamEvent {

    public enum Stream {
        SERIAL, USB, HID
    }

    public enum Type {
        RECEIVE, ERROR, ACTION
    }

    private static final Logger log = LoggerFactory.getLogger(StreamEvent.class);

    private Stream streamType;
    private Type eventType;

    private JSONObject eventData;


    public StreamEvent(Stream streamType, Type eventType) {
        this.streamType = streamType;
        this.eventType = eventType;

        eventData = new JSONObject();
    }

    public StreamEvent withException(Exception ex) {
        String message = ex.getMessage();
        if (message == null) { message = ex.getClass().getSimpleName(); }

        return withData("exception", message);
    }

    public StreamEvent withData(String key, Object data) {
        try {
            eventData.putOpt(key, data);
        }
        catch(JSONException e) {
            log.warn("Failed to save {} as {}", data, key);
        }

        return this;
    }


    public String getStreamType() {
        return streamType.name();
    }

    public String getEventType() {
        return eventType.name();
    }

    public String toJSON() throws JSONException {
        eventData.put("type", getEventType());
        return eventData.toString();
    }

}
