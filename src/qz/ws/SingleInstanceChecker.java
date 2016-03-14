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

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.common.TrayManager;

import java.io.IOException;
import java.net.URI;

/**
 * Created by Kyle on 12/1/2015.
 */

@WebSocket
public class SingleInstanceChecker {

    private static final Logger log = LoggerFactory.getLogger(SingleInstanceChecker.class);

    private static final int AUTO_CLOSE = 6 * 1000;
    private static final int TIMEOUT = 3 * 1000;

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
        if (!e.getMessage().contains("Connection refused")) {
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
        session.close();

        if (message.equals(Constants.PROBE_RESPONSE)) {
            log.warn("{} is already running on {}", Constants.ABOUT_TITLE, session.getRemoteAddress().toString());
            trayManager.exit(2);
        }
    }

    private void autoCloseClient(final int millis) {
        new Thread(new Runnable() {
            @Override
            public void run() {
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
            }
        }).start();
    }
}
