package qz.printer.action.html;

import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.print.JobSettings;
import javafx.print.PageLayout;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;

public class WebAppInstance {

    private static final Logger log = LogManager.getLogger(WebAppInstance.class);

    private Stage renderStage;
    private WebView webView;
    private double pageWidth;
    private double pageHeight;
    private double pageZoom;
    private boolean raster;

    private CountDownLatch captureLatch;

    private IntPredicate printAction;
    private final AtomicReference<Throwable> thrown = new AtomicReference<>();

    //listens for a Succeeded state to activate image capture
    private ChangeListener<Worker.State> stateListener = (ov, oldState, newState) -> {
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
    private ChangeListener<Number> workDoneListener = (ov, oldWork, newWork) -> log.trace("Done: {} > {}", oldWork, newWork);

    private ChangeListener<String> msgListener = (ov, oldMsg, newMsg) -> log.trace("New status: {}", newMsg);

    //listens for failures
    private ChangeListener<Throwable> exceptListener = (obs, oldExc, newExc) -> {
        if (newExc != null) { unlatch(newExc); }
    };

    public WebAppInstance(final Stage st) {
        Platform.runLater(() -> {
            webView = new WebView();

            st.setScene(new Scene(webView));
            renderStage = st;
            renderStage.setWidth(1);
            renderStage.setHeight(1);

            Worker<Void> worker = webView.getEngine().getLoadWorker();
            worker.stateProperty().addListener(stateListener);
            worker.workDoneProperty().addListener(workDoneListener);
            worker.exceptionProperty().addListener(exceptListener);
            worker.messageProperty().addListener(msgListener);

            //prevents JavaFX from shutting down when hiding window
            Platform.setImplicitExit(false);
        });
    }

    /**
     * Prints the loaded source specified in the passed {@code model}.
     *
     * @param job   A setup JavaFx {@code PrinterJob}
     * @param model The model specifying the web page parameters
     * @throws Throwable JavaFx will throw a generic {@code Throwable} class for any issues
     */
    public synchronized void print(final PrinterJob job, final WebAppModel model) throws Throwable {
        model.setZoom(1); //vector prints do not need to use zoom
        raster = false;

        load(model, (int frames) -> {
            if(frames == WebApp.VECTOR_FRAMES) {
                try {
                    double printScale = 72d / 96d;
                    webView.getTransforms().add(new Scale(printScale, printScale));

                    PageLayout layout = job.getJobSettings().getPageLayout();
                    if (model.isScaled()) {
                        double viewWidth = webView.getWidth() * printScale;
                        double viewHeight = webView.getHeight() * printScale;

                        double scale;
                        if ((viewWidth / viewHeight) >= (layout.getPrintableWidth() / layout.getPrintableHeight())) {
                            scale = (layout.getPrintableWidth() / viewWidth);
                        } else {
                            scale = (layout.getPrintableHeight() / viewHeight);
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

                        try {
                            for(int row = 0; row < rowsNeed; row++) {
                                for(int col = 0; col < columnsNeed; col++) {
                                    activePage.setX((-col * printBounds.getWidth()) / useScale);
                                    activePage.setY((-row * printBounds.getHeight()) / useScale);

                                    job.printPage(webView);
                                }
                            }

                            unlatch(null);
                        }
                        catch(Exception e) {
                            unlatch(e);
                        }
                        finally {
                            //reset state
                            webView.getTransforms().clear();
                        }
                    });
                }
                catch(Exception e) { unlatch(e); }
            }
            return frames >= WebApp.VECTOR_FRAMES;
        });

        log.trace("Waiting on print..");
        captureLatch.await(); //released when unlatch is called

        if (thrown.get() != null) { throw thrown.get(); }
    }

    public synchronized void openPreview(final WebAppModel model, JobSettings settings, Paper paper) throws IOException {
        ////ensure JavaFX has started before we run
        //if (startupLatch.getCount() > 0) {
        //    throw new IOException("JavaFX has not been started");
        //}
        //
        //Platform.runLater(() -> {
        //    new PreviewHTML(model, settings, new Stage(stage.getStyle()), paper);
        //});
    }

    public synchronized BufferedImage raster(final WebAppModel model) throws Throwable {
        AtomicReference<BufferedImage> capture = new AtomicReference<>();

        //ensure JavaFX has started before we run
        if (WebApp.hasStarted()) {
            throw new IOException("JavaFX has not been started");
        }

        //raster still needs to show stage for valid capture
        Platform.runLater(() -> {
            renderStage.show();
            renderStage.toBack();
        });

        raster = true;

        load(model, (int frames) -> {
            if (frames == WebApp.CAPTURE_FRAMES) {
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
                            unlatch(e);
                        }
                        finally {
                            Toolkit.getToolkit().removePostSceneTkPulseListener(this);
                        }
                    }
                });
                Toolkit.getToolkit().requestNextPulse();
            }

            return frames >= WebApp.CAPTURE_FRAMES;
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
    private synchronized void load(WebAppModel model, IntPredicate action) {
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

    private double findHeight() {
        String heightText = webView.getEngine().executeScript("Math.max(document.body.offsetHeight, document.body.scrollHeight)").toString();
        return Double.parseDouble(heightText);
    }

    private void adjustSize(double toWidth, double toHeight) {
        webView.setMinSize(toWidth, toHeight);
        webView.setPrefSize(toWidth, toHeight);
        webView.setMaxSize(toWidth, toHeight);
    }

    /**
     * Fix blank page after autosize is called
     */
    public void autosize(WebView webView) {
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

    private double calculateSupportedZoom(double width, double height) {
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
    public void unlatch(Throwable t) {
        //todo kill this instead of hiding
        if (t != null) {
            thrown.set(t);
        }

        captureLatch.countDown();
        renderStage.hide();
    }
}
