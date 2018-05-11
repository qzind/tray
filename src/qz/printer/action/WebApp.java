package qz.printer.action;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.joor.Reflect;
import org.joor.ReflectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JavaFX container for taking HTML snapshots.
 * Used by PrintHTML to generate printable images.
 * <p/>
 * Do not use constructor (used by JavaFX), instead call {@code WebApp.initialize()}
 */
public class WebApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(WebApp.class);

    private static final int SLEEP = 250;
    private static final int TIMEOUT = 60; //total paused seconds before failing

    private static WebApp instance = null;

    private static Stage stage;
    private static WebView webView;
    private static double pageWidth;
    private static double pageHeight;
    private static double pageZoom;

    private static final AtomicBoolean started = new AtomicBoolean(false);
    private static final AtomicBoolean complete = new AtomicBoolean(false);
    private static final AtomicReference<Throwable> thrown = new AtomicReference<>();

    private static PauseTransition snap;

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

                try {
                    Reflect.on(webView).call("setZoom", pageZoom);
                    log.trace("Zooming in by x{} for increased quality", pageZoom);
                }
                catch(ReflectException e) {
                    log.warn("Unable zoom, using default quality");
                    pageZoom = 1; //only zoom affects webView scaling
                }

                log.trace("Setting HTML page width to {}", (pageWidth * pageZoom));
                webView.setMinWidth(pageWidth * pageZoom);
                webView.setPrefWidth(pageWidth * pageZoom);
                webView.autosize();

                //we have to resize the width first, for responsive html, then calculate the best fit height
                final PauseTransition resize = new PauseTransition(Duration.millis(100));
                resize.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        if (pageHeight <= 0) {
                            String heightText = webView.getEngine().executeScript("Math.max(document.body.offsetHeight, document.body.scrollHeight)").toString();
                            pageHeight = Double.parseDouble(heightText);
                        }

                        log.trace("Setting HTML page height to {}", (pageHeight * pageZoom));
                        webView.setMinHeight(pageHeight * pageZoom);
                        webView.setPrefHeight(pageHeight * pageZoom);
                        webView.autosize();

                        snap.playFromStart();
                    }
                });

                resize.playFromStart();
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


    /** Called by JavaFX thread */
    public WebApp() {
        instance = this;
    }

    /** Starts JavaFX thread if not already running */
    public static synchronized void initialize() throws IOException {
        if (instance == null) {
            new Thread() {
                public void run() {
                    Application.launch(WebApp.class);
                }
            }.start();
        }

        for(int i = 0; i < (TIMEOUT * 1000); i += SLEEP) {
            if (started.get()) { break; }

            log.trace("Waiting for JavaFX..");
            try { Thread.sleep(SLEEP); } catch(Exception ignore) {}
        }

        if (!started.get()) {
            throw new IOException("JavaFX did not start");
        }
    }

    @Override
    public void start(Stage st) throws Exception {
        started.set(true);
        log.debug("Started JavaFX");

        webView = new WebView();
        Scene sc = new Scene(webView);

        stage = st;
        stage.setScene(sc);

        Worker<Void> worker = webView.getEngine().getLoadWorker();
        worker.stateProperty().addListener(stateListener);
        worker.workDoneProperty().addListener(workDoneListener);

        //prevents JavaFX from shutting down when hiding window
        Platform.setImplicitExit(false);
    }


    /**
     * Sets up capture to run on JavaFX thread and returns snapshot of rendered page
     *
     * @param model Data about the html to be rendered for capture
     * @return BufferedImage of the rendered html
     */
    public static synchronized BufferedImage capture(final WebAppModel model) throws Throwable {
        final AtomicReference<BufferedImage> capture = new AtomicReference<>();
        complete.set(false);
        thrown.set(null);

        //ensure JavaFX has started before we run
        if (!started.get()) {
            throw new IOException("JavaFX has not been started");
        }

        // run these actions on the JavaFX thread
        Platform.runLater(new Thread() {
            public void run() {
                try {
                    pageWidth = model.getWebWidth();
                    pageHeight = model.getWebHeight();
                    pageZoom = model.getZoom();

                    webView.setMinSize(100, 100);
                    webView.setPrefSize(100, 100);
                    webView.autosize();

                    stage.show(); //FIXME - will not capture without showing stage
                    stage.toBack();

                    //ran when engine reaches SUCCEEDED state, takes snapshot of loaded html
                    snap = new PauseTransition(Duration.millis(100));
                    snap.setOnFinished(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            try {
                                log.debug("Attempting image capture");

                                WritableImage snapshot = webView.snapshot(new SnapshotParameters(), null);
                                capture.set(SwingFXUtils.fromFXImage(snapshot, null));

                                complete.set(true);
                            }
                            catch(Throwable t) {
                                thrown.set(t);
                            }
                            finally {
                                stage.hide(); //hide stage so users won't have to manually close it
                            }
                        }
                    });

                    //actually begin loading the html
                    if (model.isPlainText()) {
                        webView.getEngine().loadContent(model.getSource(), "text/html");
                    } else {
                        webView.getEngine().load(model.getSource());
                    }
                }
                catch(Throwable t) {
                    thrown.set(t);
                }
            }
        });

        Throwable t = null;
        while(!complete.get() && (t = thrown.get()) == null) {
            log.trace("Waiting on capture..");
            try { Thread.sleep(1000); } catch(Exception ignore) {}
        }

        if (t != null) { throw t; }

        return capture.get();
    }

}
