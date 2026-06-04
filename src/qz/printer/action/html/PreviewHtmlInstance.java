package qz.printer.action.html;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.printer.PrintOptions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;

class PreviewHtmlInstance extends AbstractHtmlInstance {
    private static final Logger log = LogManager.getLogger(PreviewHtmlInstance.class);

    private WebAppModel model;
    private PrintOptions options;
    private PreviewWindow preview;
    private final CountDownLatch initLatch = new CountDownLatch(1);
    private final CountDownLatch doneLatch = new CountDownLatch(1);
    private final AtomicBoolean canceled = new AtomicBoolean(true);

    private void finishAsCanceled() {
        canceled.set(true);
        doneLatch.countDown();
    }

    public PreviewHtmlInstance(Stage stage) {
        stateListener = (ov, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                if (!hasBody()) {
                    finishAsCanceled();
                    return;
                }
                disableHtmlScrollbars();

                if (model.getWebHeight() <= 0) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                        }
                        catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }

                        Platform.runLater(() -> {
                            if (preview != null) {
                                preview.setPreviewHeight(findHeight());
                            }
                        });
                    }).start();
                }
                firePrintAction();
            }
        };

        //todo
        Platform.runLater(() -> {
            try {
                renderStage = new Stage(stage.getStyle());
                webView = new WebView();
                initStateListeners(webView.getEngine().getLoadWorker());
            }
            catch(RuntimeException e) {
                log.error("Failed to initialize preview stage", e);
                finishAsCanceled();
            }
            finally {
                initLatch.countDown();
            }
        });
    }

    public void show(WebAppModel model, PrintOptions options) throws InterruptedException {
        this.model = model;
        this.options = options;
        canceled.set(true);
        initLatch.await();

        load(model, frames -> {
            launchPreview(model.getWebWidth(), model.getWebHeight());

            renderStage.toFront();
            webView.requestFocus();

            return true;
        });
    }

    private void launchPreview(double width, double height) {
        Platform.runLater(() -> {
            try {
                preview = new PreviewWindow(renderStage.getStyle(), webView);
                preview.setOnPrint(rectangle -> {
                    // Match PR #1375 intent: accept preview-driven dimensions before printing.
                    model.setWidth(rectangle.getWidth() * (72d / 96d));
                    model.setHeight(rectangle.getHeight() * (72d / 96d));
                    canceled.set(false);
                    doneLatch.countDown();
                });
                preview.setOnCancel(this::finishAsCanceled);
                preview.setPreviewWidth(width);
                preview.setPreviewHeight(height);
                preview.setUnit(options.getPixelOptions().getUnits());
                preview.show();
            }
            catch(RuntimeException e) {
                log.error("Failed to launch preview window", e);
                finishAsCanceled();
            }
        });
    }

    public void await() throws InterruptedException {
        doneLatch.await();
    }

    public boolean isCanceled() {
        return canceled.get();
    }
}