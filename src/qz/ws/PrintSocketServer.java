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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.installer.Installer;
import qz.installer.certificate.CertificateManager;
import qz.installer.certificate.ExpiryTask;
import qz.installer.certificate.KeyPairWrapper;
import qz.installer.certificate.NativeCertificateInstaller;
import org.apache.logging.log4j.LogManager;
import qz.utils.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static qz.utils.ArgValue.STEAL;

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

    private static TrayManager trayManager;
    private static CertificateManager certificateManager;

    private static boolean forceHeadless;

    public static void main(String[] args) {
        ArgParser parser = new ArgParser(args);
        if (parser.intercept()) {
            System.exit(parser.getExitCode());
        }
        forceHeadless = parser.hasFlag(ArgValue.HEADLESS);
        SingleInstanceChecker.stealWebsocket = parser.hasFlag(STEAL);

        log.info(Constants.ABOUT_TITLE + " version: {}", Constants.VERSION);
        log.info(Constants.ABOUT_TITLE + " vendor: {}", Constants.ABOUT_COMPANY);
        log.info("Java version: {}", Constants.JAVA_VERSION.toString());
        log.info("Java vendor: {}", Constants.JAVA_VENDOR);
        setupFileLogging();

        try {
            // Gets and sets the SSL info, properties file
            certificateManager = Installer.getInstance().certGen(false);
            // Reoccurring (e.g. hourly) cert expiration check
            new ExpiryTask(certificateManager).schedule();
        }
        catch(Exception e) {
            log.error("Something went critically wrong loading HTTPS", e);
        }
        Installer.getInstance().addUserSettings();

        // Load overridable preferences set in qz-tray.properties file
        NetworkUtilities.setPreferences(certificateManager.getProperties());
        SingleInstanceChecker.setPreferences(certificateManager.getProperties());

        // Linux needs the cert installed in user-space on every launch for Chrome SSL to work
        if (!SystemUtilities.isWindows() && !SystemUtilities.isMac()) {
            X509Certificate caCert = certificateManager.getKeyPair(KeyPairWrapper.Type.CA).getCert();
            // Only install if a CA cert exists (e.g. one we generated)
            if(caCert != null) {
                NativeCertificateInstaller.getInstance().install(certificateManager.getKeyPair(KeyPairWrapper.Type.CA).getCert());
            }
        }

        try {
            log.info("Starting {} {}", Constants.ABOUT_TITLE, Constants.VERSION);
            SwingUtilities.invokeAndWait(() -> trayManager = new TrayManager(forceHeadless));
            runServer();
        }
        catch(Exception e) {
            log.error("Could not start tray manager", e);
        }

        log.warn("The web socket server is no longer running");
    }

    private static void setupFileLogging() {
        RollingFileAppender fileAppender = RollingFileAppender.newBuilder()
                .setName("log-file")
                .withAppend(true)
                .setLayout(PatternLayout.newBuilder().withPattern("%d{ISO8601} [%p] %m%n").build())
                .setFilter(ThresholdFilter.createFilter(Level.DEBUG, Filter.Result.ACCEPT, Filter.Result.DENY))
                .withFileName(FileUtilities.USER_DIR + File.separator + Constants.LOG_FILE + ".log")
                .withFilePattern(FileUtilities.USER_DIR + File.separator + Constants.LOG_FILE + ".log.%i")
                .withStrategy(DefaultRolloverStrategy.newBuilder().withMax(String.valueOf(Constants.LOG_ROTATIONS)).build())
                .withPolicy(SizeBasedTriggeringPolicy.createPolicy(String.valueOf(Constants.LOG_SIZE)))
                .withImmediateFlush(true)
                .build();
        fileAppender.start();

        LoggerUtilities.getRootLogger().addAppender(fileAppender);

        //disable jetty logging
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
    }

    public static void runServer() {
        Server server = findAvailableSecurePort();
        Connector secureConnector = null;
        if (server.getConnectors().length > 0 && !server.getConnectors()[0].isFailed()) {
            secureConnector = server.getConnectors()[0];
        }

        final AtomicBoolean running = new AtomicBoolean(false);
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
                WebSocketUpgradeFilter filter = WebSocketUpgradeFilter.configure(context);
                filter.addMapping(new ServletPathSpec("/"), (req, resp) -> new PrintSocketClient(server));
                filter.getFactory().getPolicy().setMaxTextMessageSize(MAX_MESSAGE_SIZE);

                // Handle HTTP landing page
                ServletHolder httpServlet = new ServletHolder(new HttpAboutServlet(certificateManager));
                httpServlet.setInitParameter("resourceBase", "/");
                context.addServlet(httpServlet, "/");
                context.addServlet(httpServlet, "/json");

                server.setHandler(context);
                server.setStopAtShutdown(true);
                server.start();

                running.set(true);
                log.info("Server started on port(s) " + TrayManager.getPorts(server));
                trayManager.setServer(server, running, securePortIndex, insecurePortIndex);

                server.join();
            }
            catch(IOException | MultiException e) {
                //order of getConnectors is the order we added them -> insecure first
                if (server.getConnectors()[0].isFailed()) {
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

    private static Server findAvailableSecurePort() {
        Server server = new Server();

        if (certificateManager != null) {
            final AtomicBoolean runningSecure = new AtomicBoolean(false);
            while(!runningSecure.get() && securePortIndex.get() < SECURE_PORTS.size()) {
                try {
                    // Bind the secure socket on the proper port number (i.e. 8181), add it as an additional connector
                    SslConnectionFactory sslConnection = new SslConnectionFactory(certificateManager.configureSslContextFactory(), HttpVersion.HTTP_1_1.asString());
                    HttpConnectionFactory httpConnection = new HttpConnectionFactory(new HttpConfiguration());

                    ServerConnector secureConnector = new ServerConnector(server, sslConnection, httpConnection);
                    secureConnector.setHost(certificateManager.getProperties().getProperty("wss.host"));
                    secureConnector.setPort(getSecurePortInUse());
                    server.setConnectors(new Connector[] {secureConnector});

                    server.start();
                    log.trace("Established secure WebSocket on port {}", getSecurePortInUse());

                    //only starting to test port availability; insecure port will actually start
                    server.stop();
                    runningSecure.set(true);
                }
                catch(IOException | MultiException e) {
                    if (server.getConnectors()[0].isFailed()) {
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

    /**
     * Get the TrayManager instance for this SocketServer
     *
     * @return The TrayManager instance
     */
    public static TrayManager getTrayManager() {
        return trayManager;
    }

    public static int getSecurePortInUse() {
        return SECURE_PORTS.get(securePortIndex.get());
    }

    public static int getInsecurePortInUse() {
        return INSECURE_PORTS.get(insecurePortIndex.get());
    }

    public static Properties getTrayProperties() {
        return certificateManager == null? null:certificateManager.getProperties();
    }

}
