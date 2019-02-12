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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.rolling.FixedWindowRollingPolicy;
import org.apache.log4j.rolling.RollingFileAppender;
import org.apache.log4j.rolling.SizeBasedTriggeringPolicy;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.server.pathmap.ServletPathSpec;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.common.Constants;
import qz.common.SecurityInfo;
import qz.common.TrayManager;
import qz.deploy.DeployUtilities;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.io.*;
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

    private static TrayManager trayManager;
    private static Properties trayProperties;

    private static boolean headless;


    public static void main(String[] args) {
        List<String> sArgs = Arrays.asList(args);

        if (sArgs.contains("-a") || sArgs.contains("--whitelist")) {
            int fileIndex = Math.max(sArgs.indexOf("-a"), sArgs.indexOf("--whitelist")) + 1;
            addToList(Constants.ALLOW_FILE, new File(sArgs.get(fileIndex)));
            System.exit(0);
        }
        if (sArgs.contains("-b") || sArgs.contains("--blacklist")) {
            int fileIndex = Math.max(sArgs.indexOf("-b"), sArgs.indexOf("--blacklist")) + 1;
            addToList(Constants.BLOCK_FILE, new File(sArgs.get(fileIndex)));
            System.exit(0);
        }
        // Print library list and exits
        if (sArgs.contains("-l") || sArgs.contains("--libinfo")) {
            String format = "%-40s%s%n";
            System.out.printf(format, "LIBRARY NAME:", "VERSION:");
            SortedMap<String, String> libVersions = SecurityInfo.getLibVersions();
            for (Map.Entry<String, String> entry: libVersions.entrySet()) {
                if (entry.getValue() == null) {
                    System.out.printf(format, entry.getKey(), "(unknown)");
                } else {
                    System.out.printf(format, entry.getKey(), entry.getValue());
                }
            }
            System.exit(0);
        }
        if (sArgs.contains("-h") || sArgs.contains("--headless")) {
            headless = true;
        }
        if (sArgs.contains("-v") || sArgs.contains("--version")) {
            System.out.println(Constants.VERSION);
            System.exit(0);
        }

        log.info(Constants.ABOUT_TITLE + " version: {}", Constants.VERSION);
        log.info(Constants.ABOUT_TITLE + " vendor: {}", Constants.ABOUT_COMPANY);
        log.info("Java version: {}", Constants.JAVA_VERSION.toString());
        log.info("Java vendor: {}", Constants.JAVA_VENDOR);
        setupFileLogging();

        try {
            log.info("Starting {} {}", Constants.ABOUT_TITLE, Constants.VERSION);
            SwingUtilities.invokeAndWait(() -> trayManager = new TrayManager(headless));
            runServer();
        }
        catch(Exception e) {
            log.error("Could not start tray manager", e);
        }

        log.warn("The web socket server is no longer running");
    }

    private static void addToList(String list, File certFile) {
        try {
            FileReader fr = new FileReader(certFile);
            Certificate cert = new Certificate(IOUtils.toString(fr));

            if (FileUtilities.printLineToFile(list, cert.data())) {
                log.info("Successfully added {} to {} list", cert.getOrganization(), list);
            } else {
                log.warn("Failed to add certificate to {} list (Insufficient user privileges)", list);
            }
        }
        catch(Exception e) {
            log.error("Failed to add certificate:", e);
        }
    }

    private static void setupFileLogging() {
        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setFileNamePattern(SystemUtilities.getDataDirectory() + File.separator + Constants.LOG_FILE + ".log.%i");
        rollingPolicy.setMaxIndex(Constants.LOG_ROTATIONS);

        SizeBasedTriggeringPolicy triggeringPolicy = new SizeBasedTriggeringPolicy(Constants.LOG_SIZE);

        RollingFileAppender fileAppender = new RollingFileAppender();
        fileAppender.setLayout(new PatternLayout("%d{ISO8601} [%p] %m%n"));
        fileAppender.setThreshold(Level.DEBUG);
        fileAppender.setFile(SystemUtilities.getDataDirectory() + File.separator + Constants.LOG_FILE + ".log");
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.setTriggeringPolicy(triggeringPolicy);
        fileAppender.setEncoding("UTF-8");

        fileAppender.setImmediateFlush(true);
        fileAppender.activateOptions();

        org.apache.log4j.Logger.getRootLogger().addAppender(fileAppender);
    }

    public static void runServer() {
        final AtomicBoolean running = new AtomicBoolean(false);

        trayProperties = getTrayProperties();

        while(!running.get() && securePortIndex.get() < SECURE_PORTS.size() && insecurePortIndex.get() < INSECURE_PORTS.size()) {
            Server server = new Server(getInsecurePortInUse());

            if (trayProperties != null) {
                // Bind the secure socket on the proper port number (i.e. 9341), add it as an additional connector
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(trayProperties.getProperty("wss.keystore"));
                sslContextFactory.setKeyStorePassword(trayProperties.getProperty("wss.storepass"));
                sslContextFactory.setKeyManagerPassword(trayProperties.getProperty("wss.keypass"));

                SslConnectionFactory sslConnection = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
                HttpConnectionFactory httpConnection = new HttpConnectionFactory(new HttpConfiguration());

                ServerConnector connector = new ServerConnector(server, sslConnection, httpConnection);
                connector.setHost(trayProperties.getProperty("wss.host"));
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
                ServletHolder httpServlet = new ServletHolder(new HttpAboutServlet());
                httpServlet.setInitParameter("resourceBase","/");
                context.addServlet(httpServlet, "/");
                context.addServlet(httpServlet, "/json");

                server.setHandler(context);
                server.setStopAtShutdown(true);
                server.start();

                running.set(true);
                trayManager.setServer(server, running, securePortIndex, insecurePortIndex);
                log.info("Server started on port(s) " + TrayManager.getPorts(server));

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

    /**
     * Get the TrayManager instance for this SocketServer
     *
     * @return The TrayManager instance
     */
    public static TrayManager getTrayManager() {
        return trayManager;
    }

    public static Properties getTrayProperties() {
        if (trayProperties == null) {
            trayProperties = DeployUtilities.loadTrayProperties();
        }
        return trayProperties;
    }

    public static int getSecurePortInUse() {
        return SECURE_PORTS.get(securePortIndex.get());
    }

    public static int getInsecurePortInUse() {
        return INSECURE_PORTS.get(insecurePortIndex.get());
    }

}
