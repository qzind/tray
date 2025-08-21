package qz.printer.action.html;

import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

class PrintHtmlInstance extends AbstractHtmlInstance {
    private static final Logger log = LogManager.getLogger(PrintHtmlInstance.class);

    public PrintHtmlInstance(final Stage st) {
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
}
