package qz.printer.action;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
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

    private static final double WEB_SCALE = 72d / 96d;

    private static WebApp instance = null;

    private static Stage stage;
    private static WebView webView;
    private static double pageHeight;

    private static final AtomicBoolean startup = new AtomicBoolean(false);
    private static final AtomicBoolean complete = new AtomicBoolean(false);
    private static final AtomicReference<Throwable> thrown = new AtomicReference<>();

    private static PauseTransition snap;

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
            if (pageHeight <= 0) {
                String heightText = webView.getEngine().executeScript("Math.max(document.body.offsetHeight, document.body.scrollHeight)").toString();
                pageHeight = Double.parseDouble(heightText);

                log.trace("Setting HTML page height to {}", pageHeight);
                webView.setMinHeight(pageHeight);
                webView.setPrefHeight(pageHeight);
                webView.setMaxHeight(pageHeight);
                webView.autosize();
            }

            //scale web dimensions down to print dpi
            webView.getTransforms().add(new Scale(WEB_SCALE, WEB_SCALE));

            snap.playFromStart();
        }
    };

    //listens for load progress
    private static ChangeListener<Number> workDoneListener = (ov, oldWork, newWork) -> log.trace("Done: {} > {}", oldWork, newWork);

    //listens for failures
    private static ChangeListener<Throwable> exceptListener = (obs, oldExc, newExc) -> {
        if (newExc != null) { thrown.set(newExc); }
    };


    /** Called by JavaFX thread */
    public WebApp() {
        instance = this;
    }

    /** Starts JavaFX thread if not already running */
    public static synchronized void initialize() throws IOException {
        if (instance == null) {
            new Thread(() -> Application.launch(WebApp.class)).start();
            startup.set(false);
        }

        for(int i = 0; i < (TIMEOUT * 1000); i += SLEEP) {
            if (startup.get()) { break; }

            log.trace("Waiting for JavaFX..");
            try { Thread.sleep(SLEEP); } catch(Exception ignore) {}
        }

        if (!startup.get()) {
            throw new IOException("JavaFX did not start");
        }
    }

    @Override
    public void start(Stage st) throws Exception {
        startup.set(true);
        log.debug("Started JavaFX");

        webView = new WebView();
        st.setScene(new Scene(webView));
        stage = st;

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
        load(model, (event) -> {
            try {
                PageLayout layout = job.getJobSettings().getPageLayout();
                if (model.isScaled()) {
                    double scale;
                    if ((webView.getWidth() / webView.getHeight()) / WEB_SCALE >= (layout.getPrintableWidth() / layout.getPrintableHeight())) {
                        scale = (layout.getPrintableWidth() / webView.getWidth()) / WEB_SCALE;
                    } else {
                        scale = (layout.getPrintableHeight() / webView.getHeight()) / WEB_SCALE;
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
                    webView.getTransforms().remove(activePage);

                    complete.set(true);
                });
            }
            catch(Exception e) { thrown.set(e); }
            finally { stage.hide(); }
        });

        Throwable t = null;
        while((job.getJobStatus() == PrinterJob.JobStatus.NOT_STARTED || job.getJobStatus() == PrinterJob.JobStatus.PRINTING)
                && !complete.get() && (t = thrown.get()) == null) {
            log.trace("Waiting on print..");
            try { Thread.sleep(1000); } catch(Exception ignore) {}
        }

        if (t != null) { throw t; }
    }

    public static synchronized BufferedImage raster(final WebAppModel model) throws Throwable {
        AtomicReference<BufferedImage> capture = new AtomicReference<>();

        //ensure JavaFX has started before we run
        if (!startup.get()) {
            throw new IOException("JavaFX has not been started");
        }

        Platform.runLater(() -> {
            stage.show();
            stage.toBack();
        });

        load(model, (event) -> {
            try {
                WritableImage snapshot = webView.snapshot(new SnapshotParameters(), null);
                capture.set(SwingFXUtils.fromFXImage(snapshot, null));

                complete.set(true);
            }
            catch(Throwable t) { thrown.set(t); }
            finally { stage.hide(); }
        });

        Throwable t = null;
        while(!complete.get() && (t = thrown.get()) == null) {
            log.trace("Waiting on capture..");
            try { Thread.sleep(1000); } catch(Exception ignore) {}
        }

        if (t != null) { throw t; }

        return capture.get();
    }

    /**
     * Prints the loaded source specified in the passed {@code model}.
     *
     * @param model  The model specifying the web page parameters.
     * @param action EventHandler that will be ran when the WebView completes loading.
     */
    private static synchronized void load(WebAppModel model, EventHandler<ActionEvent> action) {
        complete.set(false);
        thrown.set(null);

        Platform.runLater(() -> {
            log.trace("Setting starting size {}:{}", model.getWebWidth(), model.getWebHeight());
            webView.setMinSize(model.getWebWidth(), model.getWebHeight());
            webView.setPrefSize(model.getWebWidth(), model.getWebHeight());
            webView.setMaxSize(model.getWebWidth(), model.getWebHeight());
            webView.autosize();

            pageHeight = model.getWebHeight();

            //reset additive properties
            webView.getTransforms().clear();
            webView.setZoom(1.0);

            snap = new PauseTransition(Duration.millis(100));
            snap.setOnFinished(action);

            if (model.isPlainText()) {
                webView.getEngine().loadContent(model.getSource(), "text/html");
            } else {
                webView.getEngine().load(model.getSource());
            }
        });
    }

}
