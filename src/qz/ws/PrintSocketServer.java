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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.servlet.WebSocketUpgradeFilter;
import qz.App;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.installer.certificate.CertificateManager;

import javax.servlet.DispatcherType;
import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robert on 9/9/2014.
 */

public class PrintSocketServer {

    private static final Logger log = LogManager.getLogger(PrintSocketServer.class);

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

        server = findAvailableSecurePort(certManager);
        Connector secureConnector = null;
        if (server.getConnectors().length > 0 && !server.getConnectors()[0].isFailed()) {
            secureConnector = server.getConnectors()[0];
        }

        while(!running.get() && insecurePortIndex.get() < INSECURE_PORTS.size()) {
            try {
                ServerConnector connector = new ServerConnector(server);
                connector.setPort(getInsecurePortInUse());
                if (secureConnector != null) {
                    //setup insecure connector before secure
                    server.setConnectors(new Connector[] {connector, secureConnector});
                } else {
                    server.setConnectors(new Connector[] {connector});
                }

                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

                // Handle WebSocket connections
                context.addFilter(WebSocketUpgradeFilter.class, "/", EnumSet.of(DispatcherType.REQUEST));
                JettyWebSocketServletContainerInitializer.configure(context, (ctx, container) -> {
                    container.addMapping("/", (req, resp) -> new PrintSocketClient(server));
                    container.setMaxTextMessageSize(MAX_MESSAGE_SIZE);
                    container.setIdleTimeout(Duration.ofMinutes(5));
                });

                // Handle HTTP landing page
                ServletHolder httpServlet = new ServletHolder(new HttpAboutServlet(certManager));
                httpServlet.setInitParameter("resourceBase", "/");

                context.addServlet(httpServlet, "/");
                context.addServlet(httpServlet, "/json");

                server.setHandler(context);
                server.setStopAtShutdown(true);
                server.start();

                trayManager.setReloadThread(new Thread(() -> {
                    try {
                        trayManager.setDangerIcon();
                        running.set(false);
                        securePortIndex.set(0);
                        insecurePortIndex.set(0);
                        server.stop();
                    }
                    catch(Exception e) {
                        log.error("Failed to reload", e);
                        trayManager.displayErrorMessage("Error stopping print socket: " + e.getLocalizedMessage());
                    }
                }));

                running.set(true);

                log.info("Server started on port(s) " + getPorts(server));
                trayManager.setServer(server, insecurePortIndex.get());
                server.join();
            }
            catch(IOException | MultiException e) {
                //order of getConnectors is the order we added them -> insecure first
                if (server.isFailed()) {
                    insecurePortIndex.incrementAndGet();
                }

                //explicitly stop the server, because if only 1 port has an exception the other will still be opened
                try { server.stop(); }catch(Exception stopEx) { stopEx.printStackTrace(); }
            }
            catch(Exception e) {
                e.printStackTrace();
                trayManager.displayErrorMessage(e.getLocalizedMessage());
                break;
            }
        }
    }

    private static Server findAvailableSecurePort(CertificateManager certManager) {
        Server server = new Server();

        if (certManager != null) {
            final AtomicBoolean runningSecure = new AtomicBoolean(false);
            while(!runningSecure.get() && securePortIndex.get() < SECURE_PORTS.size()) {
                try {
                    // Bind the secure socket on the proper port number (i.e. 8181), add it as an additional connector
                    SslConnectionFactory sslConnection = new SslConnectionFactory(certManager.configureSslContextFactory(), HttpVersion.HTTP_1_1.asString());
                    HttpConnectionFactory httpConnection = new HttpConnectionFactory(new HttpConfiguration());

                    ServerConnector secureConnector = new ServerConnector(server, sslConnection, httpConnection);
                    secureConnector.setHost(certManager.getProperties().getProperty("wss.host"));
                    secureConnector.setPort(getSecurePortInUse());
                    server.setConnectors(new Connector[] {secureConnector});

                    server.start();
                    log.trace("Established secure WebSocket on port {}", getSecurePortInUse());

                    //only starting to test port availability; insecure port will actually start
                    server.stop();
                    runningSecure.set(true);
                }
                catch(IOException | MultiException e) {
                    if (server.isFailed()) {
                        securePortIndex.incrementAndGet();
                    }

                    try { server.stop(); }catch(Exception stopEx) { stopEx.printStackTrace(); }
                }
                catch(Exception e) {
                    e.printStackTrace();
                    trayManager.displayErrorMessage(e.getLocalizedMessage());
                    break;
                }
            }
        }

        if (server.getConnectors().length == 0 || server.getConnectors()[0].isFailed()) {
            log.warn("Could not start secure WebSocket");
        }

        return server;
    }

    public static void setTrayManager(TrayManager manager) {
        trayManager = manager;
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
