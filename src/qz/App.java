package qz;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.certificate.CertificateManager;
import qz.installer.certificate.ExpiryTask;
import qz.installer.certificate.KeyPairWrapper;
import qz.installer.certificate.NativeCertificateInstaller;
import qz.utils.*;
import qz.ws.PrintSocketServer;
import qz.ws.SingleInstanceChecker;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Properties;

public class App {
    private static final Logger log = LogManager.getLogger(App.class);
    private static Properties trayProperties = null;

    public static void main(String ... args) {
        ArgParser parser = new ArgParser(args);
        LibUtilities.getInstance().bind();
        if(parser.intercept()) {
            System.exit(parser.getExitCode());
        }
        SingleInstanceChecker.stealWebsocket = parser.hasFlag(ArgValue.STEAL);
        setupFileLogging();
        log.info(Constants.ABOUT_TITLE + " version: {}", Constants.VERSION);
        log.info(Constants.ABOUT_TITLE + " vendor: {}", Constants.ABOUT_COMPANY);
        log.info("Java version: {}", Constants.JAVA_VERSION.toString());
        log.info("Java vendor: {}", Constants.JAVA_VENDOR);

        CertificateManager certManager = null;
        try {
            // Gets and sets the SSL info, properties file
            certManager = Installer.getInstance().certGen(false);
            trayProperties = certManager.getProperties();
            // Reoccurring (e.g. hourly) cert expiration check
            new ExpiryTask(certManager).schedule();
        } catch(Exception e) {
            log.error("Something went critically wrong loading HTTPS", e);
        }
        Installer.getInstance().addUserSettings();

        // Load overridable preferences set in qz-tray.properties file
        NetworkUtilities.setPreferences(certManager.getProperties());
        SingleInstanceChecker.setPreferences(certManager.getProperties());

        // Linux needs the cert installed in user-space on every launch for Chrome SSL to work
        if(!SystemUtilities.isWindows() && !SystemUtilities.isMac()) {
            X509Certificate caCert = certManager.getKeyPair(KeyPairWrapper.Type.CA).getCert();
            // Only install if a CA cert exists (e.g. one we generated)
            if(caCert != null) {
                NativeCertificateInstaller.getInstance().install(certManager.getKeyPair(KeyPairWrapper.Type.CA).getCert());
            }
        }

        try {
            log.info("Starting {} {}", Constants.ABOUT_TITLE, Constants.VERSION);
            // Start the WebSocket
            PrintSocketServer.runServer(certManager, parser.isHeadless());
        }
        catch(Exception e) {
            log.error("Could not start tray manager", e);
        }

        log.warn("The web socket server is no longer running");
    }

    public static Properties getTrayProperties() {
        return trayProperties;
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
}
