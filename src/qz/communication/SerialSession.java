package qz.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

public class SerialSession {
    private static final Logger log = LogManager.getLogger(SerialSession.class);

    private final Session session;

    public SerialSession(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public void sendSerialEvent(String portName, String output, Runnable closeHandler) {
        StreamEvent event = new StreamEvent(StreamEvent.Stream.SERIAL, StreamEvent.Type.RECEIVE)
                .withData("portName", portName)
                .withData("output", output);
        PrintSocketClient.sendStream(session, event, closeHandler);
    }
}
