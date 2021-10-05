package qz;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.rolling.FixedWindowRollingPolicy;
import org.apache.log4j.rolling.RollingFileAppender;
import org.apache.log4j.rolling.SizeBasedTriggeringPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Properties;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
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
            NativeCertificateInstaller.getInstance().install(certManager.getKeyPair(KeyPairWrapper.Type.CA).getCert());
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
}
