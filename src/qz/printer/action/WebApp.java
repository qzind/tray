package qz.printer.action;

import com.github.zafarkhaja.semver.Version;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotResult;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.joor.Reflect;
import org.joor.ReflectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import qz.common.Constants;
import qz.deploy.DeployUtilities;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    private static CountDownLatch startupLatch;
    private static CountDownLatch captureLatch;

    private static final AtomicReference<BufferedImage> capture = new AtomicReference<>();
    private static final AtomicReference<Throwable> thrown = new AtomicReference<>();


    //listens for a Succeeded state to activate image capture
    private static ChangeListener<Worker.State> stateListener = new ChangeListener<Worker.State>() {
        @Override
        public void changed(ObservableValue<? extends Worker.State> ov, Worker.State oldState, Worker.State newState) {
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
                if (pageHeight <= 0) {
                    String heightText = webView.getEngine().executeScript("Math.max(document.body.offsetHeight, document.body.scrollHeight)").toString();
                    pageHeight = Double.parseDouble(heightText);
                }

                log.trace("Setting HTML page height to {}", pageHeight * pageZoom);
                webView.setMinHeight(pageHeight * pageZoom);
                webView.setPrefHeight(pageHeight * pageZoom);
                webView.autosize();

                //without this runlater, the first capture is missed and all following captures are offset
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        new AnimationTimer() {
                            int frames = 0;

                            @Override
                            public void handle(long l) {
                                if (++frames == 2) {
                                    log.debug("Attempting image capture");


                                    webView.snapshot(new Callback<SnapshotResult,Void>() {
                                        @Override
                                        public Void call(SnapshotResult snapshotResult) {
                                            capture.set(SwingFXUtils.fromFXImage(snapshotResult.getImage(), null));
                                            unlatch();

                                            return null;
                                        }
                                    }, null, null);

                                    //stop timer after snapshot
                                    stop();
                                }
                            }
                        }.start();
                    }
                });
            }
        }
    };

    //listens for load progress
    private static ChangeListener<Number> workDoneListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> ov, Number oldWork, Number newWork) {
            log.trace("Done: {} > {}", oldWork, newWork);
        }
    };

    //listens for failures
    private static ChangeListener<Throwable> exceptListener = new ChangeListener<Throwable>() {
        @Override
        public void changed(ObservableValue<? extends Throwable> obs, Throwable oldExc, Throwable newExc) {
            if (newExc != null) {
                thrown.set(newExc);
                unlatch();
            }
        }
    };


    /** Called by JavaFX thread */
    public WebApp() {
        instance = this;
    }

    /** Starts JavaFX thread if not already running */
    public static synchronized void initialize() throws IOException {
        if (instance == null) {
            startupLatch = new CountDownLatch(1);

            // JavaFX native libs
            if (SystemUtilities.isJar() && Constants.JAVA_VERSION.greaterThanOrEqualTo(Version.valueOf("11.0.0"))) {
                System.setProperty("java.library.path", new File(DeployUtilities.detectJarPath()).getParent() + "/libs/");
            }

            if (PrintSocketServer.getTrayManager().isMonocleAllowed()) {
                log.trace("Initializing monocle platform");

                System.setProperty("prism.order", "sw");
                System.setProperty("javafx.platform", "monocle"); // Standard JDKs
                System.setProperty("glass.platform", "Monocle"); // Headless JDKs
                System.setProperty("monocle.platform", "Headless");

                // monocle memory footprint is (geometry^2 * depth) >> 3
                // our allowance will use a lower shift from available memory to ensure a conservative platform size
                long memory = Runtime.getRuntime().maxMemory();
                long memoryMB = memory / 1048576L;
                int allowance = memoryMB > 1024? 2 : 1;

                int geometry = (int)Math.sqrt(((memory)<<allowance) / 32d);
                if (geometry > 40000) { geometry = 40000; } // cap at 40k (A1 size at 1200dpi)

                log.trace("Allowing max headless geometry of {}x{} based on available memory ({} MB)", geometry, geometry, memoryMB);

                System.setProperty("headless.geometry", String.format("%sx%s-32", geometry, geometry));
            }

            new Thread() {
                public void run() {
                    Application.launch(WebApp.class);
                }
            }.start();
        }

        if (startupLatch.getCount() > 0) {
            try {
                log.trace("Waiting for JavaFX..");

                if (!startupLatch.await(60, TimeUnit.SECONDS)) {
                    throw new IOException("JavaFX did not start");
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
        Scene sc = new Scene(webView);

        stage = st;
        stage.setScene(sc);
        stage.setWidth(1);
        stage.setHeight(1);

        Worker<Void> worker = webView.getEngine().getLoadWorker();
        worker.stateProperty().addListener(stateListener);
        worker.workDoneProperty().addListener(workDoneListener);
        worker.exceptionProperty().addListener(exceptListener);

        //prevents JavaFX from shutting down when hiding window
        Platform.setImplicitExit(false);
    }


    public static void clear() {
        capture.set(null);
        thrown.set(null);
    }

    /**
     * Sets up capture to run on JavaFX thread and returns snapshot of rendered page
     *
     * @param model Data about the html to be rendered for capture
     * @return BufferedImage of the rendered html
     */
    public static synchronized BufferedImage capture(final WebAppModel model) throws Throwable {
        captureLatch = new CountDownLatch(1);

        clear();

        //ensure JavaFX has started before we run
        if (startupLatch.getCount() > 0) {
            throw new IOException("JavaFX has not been started");
        }

        // run these actions on the JavaFX thread
        Platform.runLater(new Thread() {
            public void run() {
                try {
                    pageWidth = model.getWebWidth();
                    pageHeight = model.getWebHeight();
                    pageZoom = model.getZoom();

                    try {
                        Reflect.on(webView).call("setZoom", pageZoom);
                        log.trace("Zooming in by x{} for increased quality", pageZoom);
                    }
                    catch(ReflectException e) {
                        log.warn("Unable zoom, using default quality");
                        pageZoom = 1; //only zoom affects webView scaling
                    }

                    webView.setMinSize(pageWidth * pageZoom, pageHeight * pageZoom);
                    webView.setPrefSize(pageWidth * pageZoom, pageHeight * pageZoom);
                    if (pageHeight == 0) {
                        //jfx8 uses a default of 600 if height is exactly 0, set it to 1 here to avoid that behavior
                        webView.setMinHeight(1);
                        webView.setPrefHeight(1);
                    }
                    webView.autosize();

                    stage.show(); //FIXME - will not capture without showing stage
                    stage.toBack();

                    //actually begin loading the html
                    if (model.isPlainText()) {
                        webView.getEngine().loadContent(model.getSource(), "text/html");
                    } else {
                        webView.getEngine().load(model.getSource());
                    }
                }
                catch(Throwable t) {
                    thrown.set(t);
                    unlatch();
                }
            }
        });

        log.trace("Waiting on capture..");
        captureLatch.await(); //should be released when either the capture or thrown variables are set

        if (thrown.get() != null) { throw thrown.get(); }

        return capture.get();
    }

    private static void unlatch() {
        captureLatch.countDown();
        stage.hide(); //hide stage so users won't have to manually close it
    }

}
