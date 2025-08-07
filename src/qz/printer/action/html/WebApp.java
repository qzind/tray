package qz.printer.action.html;

import com.github.zafarkhaja.semver.Version;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * JavaFX container for taking HTML snapshots.
 * Used by PrintHTML to generate printable images.
 * <p/>
 * Do not use constructor (used by JavaFX), instead call {@code WebApp.initialize()}
 */
public class WebApp extends Application {

    private static final Logger log = LogManager.getLogger(WebApp.class);

    private static WebApp instance;

    // JDK-8283686: Printing WebView may results in empty page
    public static final Version JDK_8283686_START = Version.valueOf(/* WebKit */ "609.1.0");
    public static final Version JDK_8283686_END = Version.valueOf(/* WebKit */ "612.1.0");
    public static final int JDK_8283686_VECTOR_FRAMES = 30;
    public static int CAPTURE_FRAMES = 2;
    public static int VECTOR_FRAMES = 1;

    public static Version webkitVersion = null;
    private static boolean headless;

    private static Stage stage;
    private static CountDownLatch startupLatch;


    /** Called by JavaFX thread */
    public WebApp() {
        instance = this;
    }

    /** Starts JavaFX thread if not already running */
    public static synchronized void initialize() throws IOException {
        if (instance == null) {
            startupLatch = new CountDownLatch(1);
            // For JDK8 compat
            headless = false;

            // JDK11+ depends bundled javafx
            if (Constants.JAVA_VERSION.getMajorVersion() >= 11) {
                // Monocle default for unit tests
                boolean useMonocle = true;
                if (PrintSocketServer.getTrayManager() != null) {
                    // Honor user monocle override
                    useMonocle = PrintSocketServer.getTrayManager().isMonoclePreferred();
                    // Trust TrayManager's headless detection
                    headless = PrintSocketServer.getTrayManager().isHeadless();
                } else {
                    // Fallback for JDK11+
                    headless = true;
                }
                if (useMonocle && SystemUtilities.hasMonocle()) {
                    log.trace("Initializing monocle platform");
                    System.setProperty("javafx.platform", "monocle");
                    // Don't set glass.platform on Linux per https://github.com/qzind/tray/issues/702
                    switch(SystemUtilities.getOs()) {
                        case WINDOWS:
                        case MAC:
                            System.setProperty("glass.platform", "Monocle");
                            break;
                        default:
                            // don't set "glass.platform"
                    }

                    //software rendering required headless environments
                    if (headless) {
                        System.setProperty("prism.order", "sw");
                    }
                } else {
                    log.warn("Monocle platform will not be used");
                }
            }

            new Thread(() -> Application.launch(WebApp.class)).start();
        }

        if (startupLatch.getCount() > 0) {
            try {
                log.trace("Waiting for JavaFX..");
                if (!startupLatch.await(60, TimeUnit.SECONDS)) {
                    throw new IOException("JavaFX did not start");
                } else {
                    log.trace("Running a test snapshot to size the stage...");
                    try {
                        raster(new WebAppModel("<h1>startup</h1>", true, 0, 0, true, 2));
                        log.trace("JFX initialized successfully");
                    }
                    catch(Throwable t) {
                        throw new IOException(t);
                    }
                }
            }
            catch(InterruptedException ignore) {}
        }
    }

    @Override
    public void start(Stage st) throws Exception {
        startupLatch.countDown();
        log.debug("Started JavaFX");

        // JDK-8283686: Printing WebView may results in empty page
        // See also https://github.com/qzind/tray/issues/778
        if(getWebkitVersion() == null ||
                (getWebkitVersion().greaterThan(JDK_8283686_START) &&
                        getWebkitVersion().lessThan(JDK_8283686_END))) {
            VECTOR_FRAMES = JDK_8283686_VECTOR_FRAMES; // Additional pulses needed for vector graphics
        }

        stage = st;
        stage.setWidth(1);
        stage.setHeight(1);

        //prevents JavaFX from shutting down when hiding window
        Platform.setImplicitExit(false);
    }

    public static Version getWebkitVersion() {
        if(webkitVersion == null) {
            WebView webView = new WebView();
            if(webView != null) {
                String userAgent = webView.getEngine().getUserAgent();
                String[] parts = userAgent.split("WebKit/");
                if (parts.length > 1) {
                    String[] split = parts[1].split(" ");
                    if (split.length > 0) {
                        try {
                            webkitVersion = Version.valueOf(split[0]);
                            log.info("WebKit version {} detected", webkitVersion);
                        } catch(Exception ignore) {}
                    }
                }
                if(webkitVersion == null) {
                    log.warn("WebKit version couldn't be parsed from UserAgent: {}", userAgent);
                }
            } else {
                log.warn("Can't get WebKit version, JavaFX hasn't started yet.");
            }
        }
        return webkitVersion;
    }

    public static boolean isHeadless() {
        return headless;
    }

    public static boolean hasStarted() {
        return startupLatch.getCount() > 0;
    }
}
