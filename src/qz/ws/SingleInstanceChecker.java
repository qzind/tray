/**
 * @author Kyle Berezin
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.ws;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.utils.SystemUtilities;

import java.io.IOException;
import java.net.URI;

/**
 * Created by Kyle on 12/1/2015.
 */

@WebSocket
public class SingleInstanceChecker {

    private static final Logger log = LoggerFactory.getLogger(SingleInstanceChecker.class);

    public static CloseStatus INSTANCE_ALREADY_RUNNING = new CloseStatus(4441, "Already running");
    public static CloseStatus REQUEST_INSTANCE_TAKEOVER = new CloseStatus(4442, "WebSocket stolen");

    private static final int AUTO_CLOSE = 6 * 1000;
    private static final int TIMEOUT = 3 * 1000;

    // FIXME: This should default to false :)
    private static boolean steal = true;

    private TrayManager trayManager;
    private WebSocketClient client;


    public SingleInstanceChecker(TrayManager trayManager, int port) {
        this.trayManager = trayManager;
        log.debug("Checking for a running instance of {} on port {}", Constants.ABOUT_TITLE, port);
        autoCloseClient(AUTO_CLOSE);
        connectTo("ws://localhost:" + port);
    }

    private void connectTo(String uri) {
        try {
            if (client == null) {
                client = new WebSocketClient();
                client.start();
                client.setConnectTimeout(TIMEOUT);
                client.setAsyncWriteTimeout(TIMEOUT);
                client.setMaxIdleTimeout(TIMEOUT);
                client.setStopTimeout(TIMEOUT);
            }

            URI targetUri = new URI(uri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(this, targetUri, request);
        }
        catch(Exception e) {
            log.warn("Could not connect to url {}", uri, e);
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log.warn("Connection closed, {}", reason);
    }

    @OnWebSocketError
    public void onError(Throwable e) {
        if (!e.getMessage().contains("Connection refused") && !e.getMessage().contains("Failed to upgrade to websocket")) {
            log.warn("WebSocket error", e);
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        try {
            session.getRemote().sendString(Constants.PROBE_REQUEST);
        }
        catch(IOException e) {
            log.warn("Could not send data to server", e);
            session.close();
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        // session.close();

        if (message.equals(Constants.PROBE_RESPONSE)) {
            log.warn("{} is already running on {}", Constants.ABOUT_TITLE, session.getRemoteAddress().toString());
            if(steal) {
                stealInstance(session);
            } else {
                shutDown(session);
            }
        }
    }

    private void shutDown(Session session) {
        session.close(INSTANCE_ALREADY_RUNNING);
        log.info("{} is shutting down now.", Constants.ABOUT_TITLE);
        trayManager.exit(0);
    }

    private void stealInstance(Session session) {
        log.info("Asking other instance of {} to shutting down.", Constants.ABOUT_TITLE);
        try {
            JSONObject reply = new JSONObject();
            reply.put("call", SocketMethod.WEBSOCKET_STEAL.getCallName());
            reply.put("pid", SystemUtilities.getProcessId());
            session.getRemote().sendString(reply.toString());
        }
        catch(IOException | JSONException e) {
            log.warn("Unable to send message, giving up.", e);
            shutDown(session);
        }
    }

    private void autoCloseClient(final int millis) {
        new Thread(() -> {
            try {
                Thread.sleep(millis);
                if (client != null) {
                    if (!(client.isStopped() || client.isStopping())) {
                        client.stop();
                    }
                }
            }
            catch(Exception ignore) {
                log.error("Couldn't close client after delay");
            }
        }).start();
    }

    /**
     * Indicate if this instance is to survive when two are detected (default is false)
     *
     * @param steal Whether or not to steal the instance and request the other to shutdown
     */
    public void setSteal(boolean steal) {
        SingleInstanceChecker.steal = steal;
    }
}
