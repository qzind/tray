package qz.printer.action.html;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.function.IntPredicate;

abstract class AbstractHtmlInstance {

    protected Stage renderStage;
    protected WebView webView;
    protected IntPredicate printAction;

    /**
     * Prints the loaded source specified in the passed {@code model}.
     *
     * @param model  The model specifying the web page parameters.
     * @param action EventHandler that will be ran when the WebView completes loading.
     */
    protected synchronized void load(WebAppModel model, IntPredicate action) {
        Platform.runLater(() -> {
            //zoom should only be factored on raster prints
            double pageZoom = model.getZoom();
            double pageWidth = model.getWebWidth();
            double pageHeight = model.getWebHeight();

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

    protected void disableHtmlScrollbars() {
        WebApp.disableHtmlScrollbars(webView);
    }

    protected boolean hasBody() {
        return WebApp.hasBody(webView);
    }
}