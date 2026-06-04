package qz.printer.action.html;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;

abstract class AbstractHtmlInstance {
    private static final Logger log = LogManager.getLogger(AbstractHtmlInstance.class);

    protected Stage renderStage;
    protected WebView webView;

    protected double pageWidth;
    protected double pageHeight;
    protected double pageZoom;

    protected IntPredicate printAction;
    protected final AtomicReference<Throwable> thrown = new AtomicReference<>();

    protected CountDownLatch captureLatch;

    //listens for a Succeeded state to activate image capture
    protected ChangeListener<Worker.State> stateListener = (ov, oldState, newState) -> {
        log.trace("New state: {} > {}", oldState, newState);

        // Cancelled should probably throw exception listener, but does not
        if (newState == Worker.State.CANCELLED) {
            // This can happen for file downloads, e.g. "response-content-disposition=attachment"
            // See https://github.com/qzind/tray/issues/1183
            unlatch(new IOException("Page load was cancelled for an unknown reason"));
        }
        if (newState == Worker.State.SUCCEEDED) {
            if (!hasBody()) return;
            disableHtmlScrollbars();

            // width was resized earlier (for responsive html), then calculate the best fit height
            // FIXME: Should only be needed when height is unknown but fixes blank vector prints
            double fittedHeight = findHeight();
            boolean heightNeeded = pageHeight <= 0;

            if (heightNeeded) {
                pageHeight = fittedHeight;
            }
            pageHeight = (pageHeight <= 0) ? findHeight() : pageHeight;

            // find and set page zoom for increased quality
            double usableZoom = calculateSupportedZoom(pageWidth, pageHeight);
            if (usableZoom < pageZoom) {
                log.warn("Zoom level {} decreased to {} due to physical memory limitations", pageZoom, usableZoom);
                pageZoom = usableZoom;
            }
            webView.setZoom(pageZoom);
            log.trace("Zooming in by x{} for increased quality", pageZoom);

            adjustSize(pageWidth * pageZoom, pageHeight * pageZoom);

            //need to check for height again as resizing can cause partial results
            if (heightNeeded) {
                fittedHeight = findHeight();
                if (fittedHeight != pageHeight) {
                    adjustSize(pageWidth * pageZoom, fittedHeight * pageZoom);
                }
            }

            log.trace("Set HTML page height to {}", pageHeight);

            autoSize(webView);

            firePrintAction();
        }
    };

    //listens for load progress
    protected ChangeListener<Number> workDoneListener = (ov, oldWork, newWork) -> log.trace("Done: {} > {}", oldWork, newWork);

    protected ChangeListener<String> msgListener = (ov, oldMsg, newMsg) -> log.trace("New status: {}", newMsg);

    //listens for failures
    protected ChangeListener<Throwable> exceptListener = (obs, oldExc, newExc) -> {
        if (newExc != null) { unlatch(newExc); }
    };

    protected void initStateListeners(Worker<Void> worker) {
        worker.stateProperty().addListener(stateListener);
        worker.workDoneProperty().addListener(workDoneListener);
        worker.exceptionProperty().addListener(exceptListener);
        worker.messageProperty().addListener(msgListener);
    }

    /**
     * Prints the loaded source specified in the passed {@code model}.
     *
     * @param model  The model specifying the web page parameters.
     * @param action EventHandler that will be ran when the WebView completes loading.
     */
    protected synchronized void load(WebAppModel model, IntPredicate action) {
        captureLatch = new CountDownLatch(1);
        thrown.set(null);

        Platform.runLater(() -> {
            //zoom should only be factored on raster prints
            pageZoom = model.getZoom();
            pageWidth = model.getWebWidth();
            pageHeight = model.getWebHeight();

            log.trace("Setting starting size {}:{}", pageWidth, pageHeight);
            adjustSize(pageWidth * pageZoom, pageHeight * pageZoom);

            if (pageHeight == 0) {
                webView.setMinHeight(1);
                webView.setPrefHeight(1);
                webView.setMaxHeight(1);
            }

            autoSize(webView);

            printAction = action;

            loadSource(model);
        });
    }

    protected void loadSource(WebAppModel model) {
        WebApp.loadSource(webView, model);
    }

    protected double findHeight() {
        return WebApp.findHeight(webView);
    }

    protected void adjustSize(double toWidth, double toHeight) {
        WebApp.adjustSize(webView, toWidth, toHeight);
    }

    protected void firePrintAction() {
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

    protected void autoSize(WebView webView) {
        webView.autosize();
        // Delay the peer update until after autoSize so large preview
        // dimensions are not rendered during intermediate sizing.
        // Without this call, large previews are not rendered.
        // This thus fixes blank pages being displayed after
        // autoSize is called
        WebApp.doUpdatePeer(webView);
    }

    protected double calculateSupportedZoom(double width, double height) {
        return WebApp.calculateSupportedZoom(width, height, pageZoom, WebApp.isHeadless());
    }

    protected void disableHtmlScrollbars() {
        WebApp.disableHtmlScrollbars(webView);
    }

    protected boolean hasBody() {
        return WebApp.hasBody(webView);
    }

    /**
     * Final cleanup when no longer capturing
     */
    protected void unlatch(Throwable t) {
        //todo kill this instead of hiding
        if (t != null) {
            thrown.set(t);
        }

        captureLatch.countDown();
        renderStage.hide();
    }
}