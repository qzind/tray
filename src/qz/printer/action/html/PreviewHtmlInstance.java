package qz.printer.action.html;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.print.PrinterJob;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.printer.PrintOptions;

import java.util.concurrent.CountDownLatch;

class PreviewHtmlInstance extends AbstractHtmlInstance {
    private static final Logger log = LogManager.getLogger(PreviewHtmlInstance.class);

    private WebAppModel model;
    private PrintOptions options;
    private PreviewWindow preview;
    private boolean canceled = false;

    //rename or move this
    private CountDownLatch initLatch = new CountDownLatch(1);
    private CountDownLatch completionLatch = new CountDownLatch(1);

    public PreviewHtmlInstance(Stage stage) {
        stateListener = (ov, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                if (!hasBody()) return;
                disableHtmlScrollbars();

                if (model.getWebHeight() <= 0) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                        }
                        catch(InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        Platform.runLater(() -> preview.setPreviewHeight(findHeight()));
                    }).start();
                }
                firePrintAction();
            }
        };

        //todo
        Platform.runLater(() -> {
            renderStage = new Stage(stage.getStyle());
            webView = new WebView();
            initStateListeners(webView.getEngine().getLoadWorker());

            initLatch.countDown();
        });
    }

    public void show(PrinterJob job, WebAppModel model, PrintOptions options) throws InterruptedException {
        // todo, set up title, possible use jobname in title
        this.model = model;
        this.options = options;
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
            preview = new PreviewWindow(renderStage.getStyle(), webView);
            preview.setOnPrint(rectangle -> {
                //todo wrong place to do this. it should probably happen back in webapp, or at least via a method call from webapp e.g. instance.resizeModel(model)
                //unify webapp conversion
                model.setWidth(rectangle.getWidth() * (72d / 96d));
                model.setHeight(rectangle.getHeight() * (72d / 96d));
                completionLatch.countDown();
            });
            preview.setOnCancel(() -> {
                canceled = true;
                completionLatch.countDown();
            });
            preview.setPreviewWidth(width);
            preview.setPreviewHeight(height);
            preview.setUnit(options.getPixelOptions().getUnits());
            preview.show();
        });
    }

    public void await() throws InterruptedException {
        completionLatch.await();
    }

    public boolean isCanceled() {
        return canceled;
    }
}
