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
import javafx.scene.transform.Scale;
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

    private static final int STARTUP_PAUSE = 5; //number of pauses before assuming failure

    private static WebApp instance = null;

    private static Stage stage;
    private static WebView webView;
    private static double pageHeight;

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

            //we have to resize the width first, for responsive html, then calculate the best fit height
            if (pageHeight <= 0) {
                String heightText = webView.getEngine().executeScript("Math.max(document.body.offsetHeight, document.body.scrollHeight)").toString();
                pageHeight = Double.parseDouble(heightText);

                log.trace("Setting HTML page height to {}", pageHeight);
                webView.setMinHeight(pageHeight);
                webView.setPrefHeight(pageHeight);
                webView.setMaxHeight(pageHeight);
                webView.autosize();
            }

            //account for difference in print vs web dpi
            double scale = 72d / 96d;
            webView.getTransforms().add(new Scale(scale, scale));

            double increase = 96d / 72d;
            webView.setMinWidth(webView.getWidth() * increase);
            webView.setPrefWidth(webView.getWidth() * increase);
            webView.setMaxWidth(webView.getWidth() * increase);
            webView.autosize();

            snap.playFromStart();
        }
    };

    //listens for load progress
    private static ChangeListener<Number> workDoneListener = (ov, oldWork, newWork) -> log.trace("Done: {} > {}", oldWork, newWork);


    /** Called by JavaFX thread */
    public WebApp() {
        instance = this;
    }

    /** Starts JavaFX thread if not already running */
    public static synchronized void initialize() throws IOException {
        if (instance == null) {
            new Thread(() -> Application.launch(WebApp.class)).start();
        }

        for(int i = 0; i < STARTUP_PAUSE; i++) {
            if (webView != null) { break; }

            log.trace("Waiting for JavaFX..");
            try { Thread.sleep(1000); } catch(Exception ignore) {}
        }

        if (webView == null) {
            throw new IOException("JavaFX did not start");
        }
    }

    @Override
    public void start(Stage st) throws Exception {
        log.debug("Started JavaFX");

        webView = new WebView();
        st.setScene(new Scene(webView));
        stage = st;

        Worker<Void> worker = webView.getEngine().getLoadWorker();
        worker.stateProperty().addListener(stateListener);
        worker.workDoneProperty().addListener(workDoneListener);

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
                    if ((webView.getWidth() / webView.getHeight()) >= (layout.getPrintableWidth() / layout.getPrintableHeight())) {
                        scale = layout.getPrintableWidth() / webView.getWidth();
                    } else {
                        scale = layout.getPrintableHeight() / webView.getHeight();
                    }
                    webView.getTransforms().add(new Scale(scale, scale));
                }

                Platform.runLater(() -> complete.set(job.printPage(webView)));
            }
            catch(Exception e) { thrown.set(e); }
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
            webView.getEngine().getLoadWorker().exceptionProperty().addListener((obs, oldExc, newExc) -> {
                if (newExc != null) { thrown.set(newExc); }
            });

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
