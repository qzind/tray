package qz.printer.action.html;

import javafx.application.Platform;
import javafx.print.JobSettings;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;

class PreviewHtmlInstance extends AbstractHtmlInstance {
    private static final Logger log = LogManager.getLogger(PreviewHtmlInstance.class);

    private WebAppModel model;
    private final JobSettings settings;
    private PreviewWindow preview;
    //rename or move this
    private CountDownLatch initLatch = new CountDownLatch(1);

    public PreviewHtmlInstance(Stage stage) {
        stateListener = (ov, oldState, newState) -> {
            if (true) {

                if (!hasBody()) return;

                if (model.getWebHeight() <= 0) {
                    disableHtmlScrollbars();
                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                        }
                        catch(InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        Platform.runLater(() -> {
                            preview.setPreviewHeight(findHeight());
                        });
                    }).start();
                }
            }
        };

        //todo
        Platform.runLater(() -> {
            renderStage = new Stage(stage.getStyle());
            webView = new WebView();

            initLatch.countDown();
        });
        settings = null;
    }

    public void show(WebAppModel model) throws InterruptedException {
        this.model = model;
        initLatch.await();

        Platform.runLater(() -> {
            initialize(model.getWebWidth(), model.getWebHeight());

            renderStage.toFront();
            webView.requestFocus();

            initStateListeners(webView.getEngine().getLoadWorker());
        });
    }

    private void initialize(double width, double height) {
        loadSource(model);
        Platform.runLater(() -> {
            preview = new PreviewWindow(renderStage.getStyle(), webView);
            preview.setOnPrint(rectangle -> {
                log.warn(rectangle.getWidth());
                log.warn(rectangle.getHeight());
            });
            preview.setOnCancel(() -> {
                log.warn("Print preview canceled");
            });
            preview.setPreviewWidth(width);
            preview.setPreviewHeight(height);
            preview.show();
        });
    }
}
