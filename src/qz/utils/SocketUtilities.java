package qz.utils;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import qz.communication.SocketIO;
import qz.ws.PrintSocketClient;
import qz.ws.SocketConnection;

import java.io.IOException;

public class SocketUtilities {

    public static void setupSocket(final Session session, String UID, SocketConnection connection, JSONObject params) throws JSONException {
        final String host = params.getString("host");
        final int port = params.getInt("port");
        final String location = String.format("%s:%s", host, port);

        if (connection.getNetworkSocket(location) != null) {
            PrintSocketClient.sendError(session, UID, String.format("Socket [%s] is already open", location));
            return;
        }

        try {
            final SocketIO protocol = new SocketIO(host, port);

            if (protocol.open()) {
                connection.addNetworkSocket(location, protocol);
                PrintSocketClient.sendResult(session, UID, null);
            } else {
                PrintSocketClient.sendError(session, UID, String.format("Unable to open socket [%s]", location));
            }
        }
        catch(IOException e) {
            PrintSocketClient.sendError(session, UID, e);
        }
    }

}
