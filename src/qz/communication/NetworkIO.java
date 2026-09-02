package qz.communication;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;

public interface NetworkIO {

    boolean open() throws IOException;

    boolean isOpen();

    void sendData(JSONObject params) throws JSONException, IOException;

    /**
     * Reads a response from the network.
     * @return Raw bytes received, or null if no data or timeout.
     */
    byte[] processResponse() throws IOException;

    void close();

    String getHost();

    int getPort();
}
