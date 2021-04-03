/**
 * @author Robert Casto
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.ws;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.server.pathmap.ServletPathSpec;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.App;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.installer.certificate.*;
import qz.utils.ArgParser;
import qz.utils.ArgValue;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robert on 9/9/2014.
 */

public class PrintSocketServer {

    private static final Logger log = LoggerFactory.getLogger(PrintSocketServer.class);

    private static final int MAX_MESSAGE_SIZE = Integer.MAX_VALUE;
    public static final List<Integer> SECURE_PORTS = Collections.unmodifiableList(Arrays.asList(Constants.WSS_PORTS));
    public static final List<Integer> INSECURE_PORTS = Collections.unmodifiableList(Arrays.asList(Constants.WS_PORTS));

    private static final AtomicInteger securePortIndex = new AtomicInteger(0);
    private static final AtomicInteger insecurePortIndex = new AtomicInteger(0);
    private static final AtomicBoolean running = new AtomicBoolean(false);

    private static TrayManager trayManager;
    private static Server server;


    public static void runServer(CertificateManager certManager, boolean headless) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            PrintSocketServer.setTrayManager(new TrayManager(headless));
        });

        while(!running.get() && securePortIndex.get() < SECURE_PORTS.size() && insecurePortIndex.get() < INSECURE_PORTS.size()) {
            server = new Server(getInsecurePortInUse());
            if (certManager != null) {
                // Bind the secure socket on the proper port number (i.e. 9341), add it as an additional connector
                SslConnectionFactory sslConnection = new SslConnectionFactory(certManager.configureSslContextFactory(), HttpVersion.HTTP_1_1.asString());
                HttpConnectionFactory httpConnection = new HttpConnectionFactory(new HttpConfiguration());

                ServerConnector connector = new ServerConnector(server, sslConnection, httpConnection);
                connector.setHost(certManager.getProperties().getProperty("wss.host"));
                connector.setPort(getSecurePortInUse());
                server.addConnector(connector);
            } else {
                log.warn("Could not start secure WebSocket");
            }

            try {
                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

                // Handle WebSocket connections
                WebSocketUpgradeFilter filter = WebSocketUpgradeFilter.configureContext(context);
                filter.addMapping(new ServletPathSpec("/"), new WebSocketCreator() {
                    @Override
                    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
                        return new PrintSocketClient();
                    }
                });
                filter.getFactory().getPolicy().setMaxTextMessageSize(MAX_MESSAGE_SIZE);

                // Handle HTTP landing page
                ServletHolder httpServlet = new ServletHolder(new HttpAboutServlet(certManager));
                httpServlet.setInitParameter("resourceBase","/");
                context.addServlet(httpServlet, "/");
                context.addServlet(httpServlet, "/json");

                server.setHandler(context);
                server.setStopAtShutdown(true);
                server.start();

                running.set(true);
                trayManager.setServer(server, insecurePortIndex.get());
                log.info("Server started on port(s) " + getPorts(server));
                server.join();
            }
            catch(BindException | MultiException e) {
                //order of getConnectors is the order we added them -> insecure first
                if (server.getConnectors()[0].isFailed()) {
                    insecurePortIndex.incrementAndGet();
                }
                if (server.getConnectors().length > 1 && server.getConnectors()[1].isFailed()) {
                    securePortIndex.incrementAndGet();
                }

                //explicitly stop the server, because if only 1 port has an exception the other will still be opened
                try { server.stop(); }catch(Exception ignore) { ignore.printStackTrace(); }
            }
            catch(Exception e) {
                e.printStackTrace();
                trayManager.displayErrorMessage(e.getLocalizedMessage());
            }
        }
    }

    public static void setTrayManager(TrayManager manager) {
        trayManager = manager;
        trayManager.setReloadThread(new Thread(() -> {
            try {
                trayManager.setDangerIcon();
                running.set(false);
                securePortIndex.set(0);
                insecurePortIndex.set(0);
                server.stop();
            }
            catch(Exception e) {
                trayManager.displayErrorMessage("Error stopping print socket: " + e.getLocalizedMessage());
            }
        }));
    }

    public static TrayManager getTrayManager() {
        return trayManager;
    }

    public static Server getServer() {
        return server;
    }

    public static AtomicBoolean getRunning() {
        return running;
    }

    @Deprecated
    public static void main(String ... args) {
        App.main(args);
    }

    public static int getSecurePortInUse() {
        return SECURE_PORTS.get(securePortIndex.get());
    }

    public static int getInsecurePortInUse() {
        return INSECURE_PORTS.get(insecurePortIndex.get());
    }

    /**
     * Returns a String representation of the ports assigned to the specified Server
     */
    public static String getPorts(Server server) {
        StringBuilder ports = new StringBuilder();
        for(Connector c : server.getConnectors()) {
            if (ports.length() > 0) {
                ports.append(", ");
            }

            ports.append(((ServerConnector)c).getLocalPort());
        }

        return ports.toString();
    }

}
