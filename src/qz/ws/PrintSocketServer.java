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
import qz.common.TrayManager;
import qz.installer.certificate.CertificateManager;
import qz.utils.ArgValue;
import qz.utils.PrefsSearch;

import javax.servlet.*;
import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by robert on 9/9/2014.
 */

public class PrintSocketServer {

    private static final Logger log = LogManager.getLogger(PrintSocketServer.class);

    private static final int MAX_MESSAGE_SIZE = Integer.MAX_VALUE;
    private static final AtomicBoolean running = new AtomicBoolean(false);

    private static WebsocketPorts websocketPorts;
    private static TrayManager trayManager;
    private static Server server;
    private static boolean httpsOnly;
    private static boolean sniStrict;
    private static String wssHost;
    private static String wssAllowOrigin;

    public static void runServer(CertificateManager certManager, boolean headless) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            PrintSocketServer.setTrayManager(new TrayManager(headless));
        });

        wssHost = PrefsSearch.getString(ArgValue.SECURITY_WSS_HOST, certManager.getProperties());
        wssAllowOrigin = PrefsSearch.getString(ArgValue.SECURITY_WSS_ALLOWORIGIN, certManager.getProperties());
        httpsOnly = PrefsSearch.getBoolean(ArgValue.SECURITY_WSS_HTTPSONLY, certManager.getProperties());
        sniStrict = PrefsSearch.getBoolean(ArgValue.SECURITY_WSS_SNISTRICT, certManager.getProperties());
        websocketPorts = WebsocketPorts.parseFromProperties();

        server = findAvailableSecurePort(certManager);

        Connector secureConnector = null;
        if (server.getConnectors().length > 0 && !server.getConnectors()[0].isFailed()) {
            secureConnector = server.getConnectors()[0];
        }

        if (httpsOnly && secureConnector == null) {
            log.error("Failed to start in https-only mode");
            return;
        }

        while(!running.get() && websocketPorts.insecureBoundsCheck()) {
            try {
                ServerConnector connector = new ServerConnector(server);
                connector.setPort(websocketPorts.getInsecurePort());
                if(httpsOnly) {
                    server.setConnectors(new Connector[] {secureConnector});
                } else if (secureConnector != null) {
                    //setup insecure connector before secure
                    server.setConnectors(new Connector[] {connector, secureConnector});
                } else {
                    server.setConnectors(new Connector[] {connector});
                }

                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

                // Allow private-network access
                context.addFilter(HttpAboutServlet.originFilter(wssAllowOrigin), "/*", null);

                // Handle WebSocket connections
                context.addFilter(WebSocketUpgradeFilter.class, "/", EnumSet.of(DispatcherType.REQUEST));
                JettyWebSocketServletContainerInitializer.configure(context, (ctx, container) -> {
                    container.addMapping("/*", (req, resp) -> PrintSocketClient.originFilterUpgrade(req, resp, server, wssAllowOrigin));
                    container.setMaxTextMessageSize(MAX_MESSAGE_SIZE);
                    container.setIdleTimeout(Duration.ofMinutes(5));
                });

                // Handle HTTP landing page
                ServletHolder httpServlet = new ServletHolder(new HttpAboutServlet(certManager, wssAllowOrigin));
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
                        websocketPorts.resetIndices();
                        server.stop();
                    }
                    catch(Exception e) {
                        log.error("Failed to reload", e);
                        trayManager.displayErrorMessage("Error stopping print socket: " + e.getLocalizedMessage());
                    }
                }));

                running.set(true);

                log.info("Server started on port(s) " + getPorts(server));
                websocketPorts.setHttpsOnly(httpsOnly);
                websocketPorts.setHttpOnly(secureConnector == null);
                trayManager.setServer(server, websocketPorts);
                server.join();
            }
            catch(IOException | MultiException e) {
                //order of getConnectors is the order we added them -> insecure first
                if (server.isFailed()) {
                    websocketPorts.nextInsecureIndex();
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
            while(!runningSecure.get() && websocketPorts.secureBoundsCheck()) {
                try {
                    // Bind the secure socket on the proper port number (i.e. 8181), add it as an additional connector
                    SslConnectionFactory sslConnection = new SslConnectionFactory(certManager.configureSslContextFactory(), HttpVersion.HTTP_1_1.asString());

                    // Disable SNI checks for easier print-server testing (replicates Jetty 9.x behavior)
                    HttpConfiguration httpsConfig = new HttpConfiguration();
                    SecureRequestCustomizer customizer = new SecureRequestCustomizer();
                    customizer.setSniHostCheck(sniStrict);
                    httpsConfig.addCustomizer(customizer);

                    HttpConnectionFactory httpConnection = new HttpConnectionFactory(httpsConfig);

                    ServerConnector secureConnector = new ServerConnector(server, sslConnection, httpConnection);
                    secureConnector.setHost(wssHost);
                    secureConnector.setPort(websocketPorts.getSecurePort());
                    server.setConnectors(new Connector[] {secureConnector});

                    server.start();
                    log.trace("Established secure WebSocket on port {}", websocketPorts.getSecurePort());

                    //only starting to test port availability; insecure port will actually start
                    server.stop();
                    runningSecure.set(true);
                }
                catch(IOException | MultiException e) {
                    if (server.isFailed()) {
                        websocketPorts.nextSecureIndex();
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


    public static WebsocketPorts getWebsocketPorts() {
        return websocketPorts;
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
