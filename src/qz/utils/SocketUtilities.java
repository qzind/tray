package qz.utils;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.communication.SocketIO;
import qz.ws.PrintSocketClient;
import qz.ws.SocketConnection;
import qz.ws.StreamEvent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SocketUtilities {

    private static final Logger log = LogManager.getLogger(SocketUtilities.class);

    public static void setupSocket(final Session session, String UID, SocketConnection connection, JSONObject params) throws JSONException {
        final String host = params.getString("host");
        final int port = params.getInt("port");
        final String location = String.format("%s:%s", host, port);

        if (connection.getNetworkSocket(location) != null) {
            PrintSocketClient.sendError(session, UID, String.format("Socket [%s] is already open", location));
            return;
        }

        //TODO - move to dedicated options class?
        Charset encoding = StandardCharsets.UTF_8;
        if (!params.isNull("encoding")) {
            try { encoding = Charset.forName(params.getString("encoding")); }
            catch(JSONException e) { LoggerUtilities.optionWarn(log, "string", "encoding", params.opt("encoding")); }
        }

        try {
            final SocketIO socket = new SocketIO(host, port, encoding);

            if (socket.open()) {
                connection.addNetworkSocket(location, socket);

                new Thread(() -> {
                    StreamEvent event = new StreamEvent(StreamEvent.Stream.SOCKET, StreamEvent.Type.RECEIVE)
                            .withData("host", host).withData("port", port);

                    try {
                        while(socket.isOpen()) {
                            String response = socket.processSocketResponse();

                            if (response != null) {
                                log.debug("Received socket response: {}", response);
                                PrintSocketClient.sendStream(session, event.withData("response", response));
                            }
                        }
                    }
                    catch(IOException e) {
                        StreamEvent eventErr = new StreamEvent(StreamEvent.Stream.SOCKET, StreamEvent.Type.ERROR)
                                .withData("host", host).withData("port", port).withException(e);
                        PrintSocketClient.sendStream(session, eventErr);
                    }

                    try { Thread.sleep(100); } catch(Exception ignore) {}
                }).start();

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
