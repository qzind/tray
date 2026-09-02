package qz.utils;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.communication.NetworkIO;
import qz.communication.SocketIO;
import qz.communication.UDPIO;
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
        Charset encodingInternal = StandardCharsets.UTF_8;
        if (!params.isNull("options")) {
            JSONObject options = params.getJSONObject("options");
            if (!options.isNull("encoding")) {
                encodingInternal = Charset.forName(options.getString("encoding"));
            }
        }

        final Charset encodingFinal = encodingInternal;
        try {
            final NetworkIO socket;
            String protocol = "tcp";
            int localPort = 0;
            String responseFormat = "PLAIN";

            if (!params.isNull("options")) {
                JSONObject options = params.getJSONObject("options");
                protocol = options.optString("protocol", "tcp").toLowerCase();
                localPort = options.optInt("localPort", options.optJSONObject("options") != null ? options.optJSONObject("options").optInt("localPort", 0) : 0);
                responseFormat = options.optString("responseFormat", "PLAIN").toUpperCase();
            } else {
                protocol = params.optString("protocol", "tcp").toLowerCase();
            }

            final PrintingUtilities.Flavor format = PrintingUtilities.Flavor.parse(responseFormat, PrintingUtilities.Flavor.PLAIN);

            if ("udp".equals(protocol)) {
                socket = new UDPIO(host, port, localPort, encodingFinal, connection);
            } else {
                socket = new SocketIO(host, port, encodingFinal, connection);
            }

            if (socket.open()) {
                connection.addNetworkSocket(location, socket);

                new Thread(() -> {
                    StreamEvent event = new StreamEvent(StreamEvent.Stream.SOCKET, StreamEvent.Type.RECEIVE)
                            .withData("host", host).withData("port", port);

                    try {
                        while(socket.isOpen()) {
                            byte[] response = socket.processResponse();

                            if (response != null) {
                                String formatted = ByteUtilities.toString(format, response);
                                log.debug("Received socket response ({}): {}", format, formatted);
                                PrintSocketClient.sendStream(session, event.withData("response", formatted), (Runnable)socket::close);
                            }
                        }
                    }
                    catch(IOException e) {
                        StreamEvent eventErr = new StreamEvent(StreamEvent.Stream.SOCKET, StreamEvent.Type.ERROR)
                                .withData("host", host).withData("port", port).withException(e);
                        PrintSocketClient.sendStream(session, eventErr, (Runnable)socket::close);
                    }

                    if (!socket.isOpen()) {
                        // ensure cleanup if device was closed
                        connection.removeNetworkSocket(String.format("%s:%s", host, port));
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
