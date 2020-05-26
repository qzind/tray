package qz.printer.action;

import com.github.zafarkhaja.semver.Version;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import qz.common.Constants;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;

/**
 * JavaFX container for taking HTML snapshots.
 * Used by PrintHTML to generate printable images.
 * <p/>
 * Do not use constructor (used by JavaFX), instead call {@code WebApp.initialize()}
 */
public class WebApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(WebApp.class);

    private static WebApp instance = null;

    private static Stage stage;
    private static WebView webView;
    private static double pageWidth;
    private static double pageHeight;
    private static double pageZoom;
    private static boolean raster;
    private static boolean headless;

    private static CountDownLatch startupLatch;
    private static CountDownLatch captureLatch;

    private static IntPredicate printAction;
    private static final AtomicReference<Throwable> thrown = new AtomicReference<>();


    //listens for a Succeeded state to activate image capture
    private static ChangeListener<Worker.State> stateListener = (ov, oldState, newState) -> {
        log.trace("New state: {} > {}", oldState, newState);

        if (newState == Worker.State.SUCCEEDED) {
            //ensure html tag doesn't use scrollbars, clipping page instead
            Document doc = webView.getEngine().getDocument();
            NodeList tags = doc.getElementsByTagName("html");
            if (tags != null && tags.getLength() > 0) {
                Node base = tags.item(0);
                Attr applied = (Attr)base.getAttributes().getNamedItem("style");
                if (applied == null) {
                    applied = doc.createAttribute("style");
                }
                applied.setValue(applied.getValue() + "; overflow: hidden;");
                base.getAttributes().setNamedItem(applied);
            }

            //width was resized earlier (for responsive html), then calculate the best fit height
            // FIXME: Should only be needed when height is unknown but fixes blank vector prints
            String heightText = webView.getEngine().executeScript("Math.max(document.body.offsetHeight, document.body.scrollHeight)").toString();
            if (pageHeight <= 0) {
                pageHeight = Double.parseDouble(heightText);
                log.trace("Setting HTML page height to {}", pageHeight);
            }

            // find and set page zoom for increased quality
            double usableZoom = calculateSupportedZoom(pageWidth, pageHeight);
            if (usableZoom < pageZoom) {
                log.warn("Zoom level {} decreased to {} due to physical memory limitations", pageZoom, usableZoom);
                pageZoom = usableZoom;
            }
            webView.setZoom(pageZoom);
            log.trace("Zooming in by x{} for increased quality", pageZoom);

            webView.setMinSize(pageWidth * pageZoom, pageHeight * pageZoom);
            webView.setPrefSize(pageWidth * pageZoom, pageHeight * pageZoom);
            webView.setMaxSize(pageWidth * pageZoom, pageHeight * pageZoom);

            autosize(webView);

            Platform.runLater(() -> new AnimationTimer() {
                int frames = 0;

                @Override
                public void handle(long l) {
                    if (printAction.test(++frames)) {
                        stop();
                    }
                }
            }.start());
        }
    };

    //listens for load progress
    private static ChangeListener<Number> workDoneListener = (ov, oldWork, newWork) -> log.trace("Done: {} > {}", oldWork, newWork);

    //listens for failures
    private static ChangeListener<Throwable> exceptListener = (obs, oldExc, newExc) -> {
        if (newExc != null) { unlatch(newExc); }
    };


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
            if (Constants.JAVA_VERSION.greaterThanOrEqualTo(Version.valueOf("11.0.0"))) {
                // JavaFX native libs
                if (SystemUtilities.isJar()) {
                    SystemUtilities.appendProperty("java.library.path", new File(SystemUtilities.detectJarPath()).getParent() + "/libs/");
                } else if(hasConflictingLib()) {
                    // IDE helper for "no suitable pipeline found" errors
                    System.err.println("\n=== WARNING ===\nWrong javafx platform detected. Delete lib/javafx/<platform> to correct this.\n");
                }

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
                if (useMonocle) {
                    log.trace("Initializing monocle platform");
                    System.setProperty("javafx.platform", "monocle");

                    //software rendering required headless environments
                    if (headless) {
                        System.setProperty("prism.order", "sw");
                    }
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

        webView = new WebView();
        st.setScene(new Scene(webView));
        stage = st;
        stage.setWidth(1);
        stage.setHeight(1);

        Worker<Void> worker = webView.getEngine().getLoadWorker();
        worker.stateProperty().addListener(stateListener);
        worker.workDoneProperty().addListener(workDoneListener);
        worker.exceptionProperty().addListener(exceptListener);

        //prevents JavaFX from shutting down when hiding window
        Platform.setImplicitExit(false);
    }

    /**
     * Prints the loaded source specified in the passed {@code model}.
     *
     * @param job   A setup JavaFx {@code PrinterJob}
     * @param model The model specifying the web page parameters
     * @throws Throwable JavaFx will throw a generic {@code Throwable} class for any issues
     */
    public static synchronized void print(final PrinterJob job, final WebAppModel model) throws Throwable {
        model.setZoom(1); //vector prints do not need to use zoom
        raster = false;

        load(model, (int frames) -> {
            try {
                PageLayout layout = job.getJobSettings().getPageLayout();
                if (model.isScaled()) {
                    double scale;
                    if ((webView.getWidth() / webView.getHeight()) >= (layout.getPrintableWidth() / layout.getPrintableHeight())) {
                        scale = (layout.getPrintableWidth() / webView.getWidth());
                    } else {
                        scale = (layout.getPrintableHeight() / webView.getHeight());
                    }
                    webView.getTransforms().add(new Scale(scale, scale));
                }

                Platform.runLater(() -> {
                    double useScale = 1;
                    for(Transform t : webView.getTransforms()) {
                        if (t instanceof Scale) { useScale *= ((Scale)t).getX(); }
                    }

                    PageLayout page = job.getJobSettings().getPageLayout();
                    Rectangle printBounds = new Rectangle(0, 0, page.getPrintableWidth(), page.getPrintableHeight());
                    log.debug("Paper area: {},{}:{},{}", (int)page.getLeftMargin(), (int)page.getTopMargin(),
                              (int)page.getPrintableWidth(), (int)page.getPrintableHeight());

                    Translate activePage = new Translate();
                    webView.getTransforms().add(activePage);

                    int columnsNeed = Math.max(1, (int)Math.ceil(webView.getWidth() / printBounds.getWidth() * useScale - 0.1));
                    int rowsNeed = Math.max(1, (int)Math.ceil(webView.getHeight() / printBounds.getHeight() * useScale - 0.1));
                    log.debug("Document will be printed across {} pages", columnsNeed * rowsNeed);

                    for(int row = 0; row < rowsNeed; row++) {
                        for(int col = 0; col < columnsNeed; col++) {
                            activePage.setX((-col * printBounds.getWidth()) / useScale);
                            activePage.setY((-row * printBounds.getHeight()) / useScale);

                            job.printPage(webView);
                        }
                    }

                    //reset state
                    webView.getTransforms().clear();

                    unlatch(null);
                });
            }
            catch(Exception e) { unlatch(e); }

            return true; //only runs on first frame
        });

        log.trace("Waiting on print..");
        captureLatch.await(); //released when unlatch is called

        if (thrown.get() != null) { throw thrown.get(); }
    }

    public static synchronized BufferedImage raster(final WebAppModel model) throws Throwable {
        AtomicReference<BufferedImage> capture = new AtomicReference<>();

        //ensure JavaFX has started before we run
        if (startupLatch.getCount() > 0) {
            throw new IOException("JavaFX has not been started");
        }

        //raster still needs to show stage for valid capture
        Platform.runLater(() -> {
            stage.show();
            stage.toBack();
        });

        //adjust raster prints to web dpi
        double increase = 96d / 72d;
        model.setWebWidth(model.getWebWidth() * increase);
        model.setWebHeight(model.getWebHeight() * increase);

        raster = true;

        load(model, (int frames) -> {
            if (frames == 2) {
                log.debug("Attempting image capture");

                Toolkit.getToolkit().addPostSceneTkPulseListener(new TKPulseListener() {
                    @Override
                    public void pulse() {
                        try {
                            // TODO: Revert to Callback once JDK-8244588/SUPQZ-5 is avail (JDK11+ only)
                            capture.set(SwingFXUtils.fromFXImage(webView.snapshot(null, null), null));
                            unlatch(null);
                        }
                        catch(Exception e) {
                            log.error("Caught during snapshot");
                            unlatch(e);
                        }
                        finally {
                            Toolkit.getToolkit().removePostSceneTkPulseListener(this);
                        }
                    }
                });
            }

            return frames >= 2;
        });

        log.trace("Waiting on capture..");
        captureLatch.await(); //released when unlatch is called

        if (thrown.get() != null) { throw thrown.get(); }

        return capture.get();
    }

    /**
     * Prints the loaded source specified in the passed {@code model}.
     *
     * @param model  The model specifying the web page parameters.
     * @param action EventHandler that will be ran when the WebView completes loading.
     */
    private static synchronized void load(WebAppModel model, IntPredicate action) {
        captureLatch = new CountDownLatch(1);
        thrown.set(null);

        Platform.runLater(() -> {
            //zoom should only be factored on raster prints
            pageZoom = model.getZoom();
            pageWidth = model.getWebWidth();
            pageHeight = model.getWebHeight();

            log.trace("Setting starting size {}:{}", pageWidth, pageHeight);
            webView.setMinSize(pageWidth * pageZoom, pageHeight * pageZoom);
            webView.setPrefSize(pageWidth * pageZoom, pageHeight * pageZoom);
            webView.setMaxSize(pageWidth * pageZoom, pageHeight * pageZoom);

            if (pageHeight == 0) {
                webView.setMinHeight(1);
                webView.setPrefHeight(1);
                webView.setMaxHeight(1);
            }

            autosize(webView);

            printAction = action;

            if (model.isPlainText()) {
                webView.getEngine().loadContent(model.getSource(), "text/html");
            } else {
                webView.getEngine().load(model.getSource());
            }
        });
    }

    /**
     * Fix blank page after autosize is called
     */
    public static void autosize(WebView webView) {
        webView.autosize();

        if (!raster) {
            // Call updatePeer; fixes a bug with webView resizing
            // Can be avoided by calling stage.show() but breaks headless environments
            // See: https://github.com/qzind/tray/issues/513
            String[] methods = {"impl_updatePeer" /*jfx8*/, "doUpdatePeer" /*jfx11*/};
            try {
                for(Method m : webView.getClass().getDeclaredMethods()) {
                    for(String method : methods) {
                        if (m.getName().equals(method)) {
                            m.setAccessible(true);
                            m.invoke(webView);
                            return;
                        }
                    }
                }
            }
            catch(SecurityException | ReflectiveOperationException e) {
                log.warn("Unable to update peer; Blank pages may occur.", e);
            }
        }
    }

    private static double calculateSupportedZoom(double width, double height) {
        long memory = Runtime.getRuntime().maxMemory();
        int allowance = (memory / 1048576L) > 1024? 3:2;
        if (headless) { allowance--; }
        long availSpace = (long)((memory << allowance) / 72d);

        return Math.sqrt(availSpace / (width * height));
    }

    /**
     * Final cleanup when no longer capturing
     */
    public static void unlatch(Throwable t) {
        if (t != null) {
            thrown.set(t);
        }

        captureLatch.countDown();
        stage.hide();
    }

    public static boolean hasConflictingLib() {
        // If running from the IDE, make sure we're not using the wrong libs
        URL url = Application.class.getResource("/" + Application.class.getName().replace('.', '/') + ".class");
        String graphicsJar = url.toString().replaceAll("file:/|jar:", "").replaceAll("!.*", "");
        log.trace("JavaFX will startup using {}", graphicsJar);
        if(SystemUtilities.isWindows()) {
            return !graphicsJar.contains("windows");
        } else if(SystemUtilities.isMac()) {
            return !graphicsJar.contains("osx") && !graphicsJar.contains("mac");
        }
        return !graphicsJar.contains("linux");
    }
}
