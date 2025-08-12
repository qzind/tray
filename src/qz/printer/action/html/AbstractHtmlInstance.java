package qz.printer.action.html;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.lang.reflect.Method;
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

    protected boolean raster;
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
            boolean hasBody = (boolean)webView.getEngine().executeScript("document.body != null");
            if (!hasBody) {
                log.warn("Loaded page has no body - likely a redirect, skipping state");
                return;
            }

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
            double fittedHeight = findHeight();
            boolean heightNeeded = pageHeight <= 0;

            if (heightNeeded) {
                pageHeight = fittedHeight;
            }

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
    protected ChangeListener<Number> workDoneListener = (ov, oldWork, newWork) -> log.trace("Done: {} > {}", oldWork, newWork);

    protected ChangeListener<String> msgListener = (ov, oldMsg, newMsg) -> log.trace("New status: {}", newMsg);

    //listens for failures
    protected ChangeListener<Throwable> exceptListener = (obs, oldExc, newExc) -> {
        if (newExc != null) { unlatch(newExc); }
    };


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

            autosize(webView);

            printAction = action;

            if (model.isPlainText()) {
                webView.getEngine().loadContent(model.getSource(), "text/html");
            } else {
                webView.getEngine().load(model.getSource());
            }
        });
    }

    protected double findHeight() {
        String heightText = webView.getEngine().executeScript("Math.max(document.body.offsetHeight, document.body.scrollHeight)").toString();
        return Double.parseDouble(heightText);
    }

    protected void adjustSize(double toWidth, double toHeight) {
        webView.setMinSize(toWidth, toHeight);
        webView.setPrefSize(toWidth, toHeight);
        webView.setMaxSize(toWidth, toHeight);
    }

    /**
     * Fix blank page after autosize is called
     */
    protected void autosize(WebView webView) {
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

    protected double calculateSupportedZoom(double width, double height) {
        long memory = Runtime.getRuntime().maxMemory();
        int allowance = (memory / 1048576L) > 1024? 3:2;
        if (WebApp.isHeadless()) { allowance--; }
        long availSpace = memory << allowance;

        // Memory needed for print is roughly estimated as
        // (width * height) [pixels needed] * (pageZoom * 72d) [print density used] * 3 [rgb channels]
        return Math.sqrt(availSpace / ((width * height) * (pageZoom * 72d) * 3));
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
