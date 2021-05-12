/**
 *
 */

package qz.ws;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.rolling.FixedWindowRollingPolicy;
import org.apache.log4j.rolling.RollingFileAppender;
import org.apache.log4j.rolling.SizeBasedTriggeringPolicy;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.installer.Installer;
import qz.installer.certificate.CertificateManager;
import qz.installer.certificate.ExpiryTask;
import qz.installer.certificate.KeyPairWrapper;
import qz.installer.certificate.NativeCertificateInstaller;
import qz.utils.ArgParser;
import qz.utils.ArgValue;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
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
    private static CertificateManager certificateManager;

    private static boolean forceHeadless;


    public static void main(String[] args) {
        ArgParser parser = new ArgParser(args);
        if (parser.intercept()) {
            System.exit(parser.getExitCode());
        }
        forceHeadless = parser.hasFlag(ArgValue.HEADLESS);
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

        // Linux needs the cert installed in user-space on every launch for Chrome SSL to work
        if (!SystemUtilities.isWindows() && !SystemUtilities.isMac()) {
            NativeCertificateInstaller.getInstance().install(certificateManager.getKeyPair(KeyPairWrapper.Type.CA).getCert());
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
        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setFileNamePattern(FileUtilities.USER_DIR + File.separator + Constants.LOG_FILE + ".log.%i");
        rollingPolicy.setMaxIndex(Constants.LOG_ROTATIONS);

        SizeBasedTriggeringPolicy triggeringPolicy = new SizeBasedTriggeringPolicy(Constants.LOG_SIZE);

        RollingFileAppender fileAppender = new RollingFileAppender();
        fileAppender.setLayout(new PatternLayout("%d{ISO8601} [%p] %m%n"));
        fileAppender.setThreshold(Level.DEBUG);
        fileAppender.setFile(FileUtilities.USER_DIR + File.separator + Constants.LOG_FILE + ".log");
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.setTriggeringPolicy(triggeringPolicy);
        fileAppender.setEncoding("UTF-8");

        fileAppender.setImmediateFlush(true);
        fileAppender.activateOptions();

        org.apache.log4j.Logger.getRootLogger().addAppender(fileAppender);
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
                filter.addMapping(new ServletPathSpec("/"), (req, resp) -> new PrintSocketClient());
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
