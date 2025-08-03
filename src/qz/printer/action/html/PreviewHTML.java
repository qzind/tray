package qz.printer.action.html;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.print.JobSettings;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ui.component.IconCache;

import java.text.DecimalFormat;

public class PreviewHTML {
    private static final Logger log = LogManager.getLogger(PreviewHTML.class);

    private final WebAppModel model;
    private final JobSettings settings;
    private final Stage previewStage;

    private final WebView webPreview = new WebView();

    private int thickness = 20;
    private double dpi = 72;

    public PreviewHTML(final WebAppModel model, JobSettings settings, Stage previewStage) {
        this.model = model;
        this.settings = settings;
        this.previewStage = previewStage;

        initialize(400, 400);

        Platform.runLater(() -> {
            previewStage.toFront();
            webPreview.requestFocus();
        });
    }

    private void initialize(double height, double width) {
        if (model.isPlainText()) {
            webPreview.getEngine().loadContent(model.getSource(), "text/html");
        } else {
            webPreview.getEngine().load(model.getSource());
        }

        Canvas leftRuler = new Canvas(thickness, height);
        Canvas topRuler = new Canvas(width, thickness);

        drawLeftRuler(leftRuler, thickness, leftRuler.getHeight(), dpi);
        drawTopRuler(topRuler, thickness, topRuler.getWidth(), dpi);

        StackPane webContainer = new StackPane(webPreview);
        webPreview.prefWidthProperty().bind(webContainer.widthProperty());
        webPreview.prefHeightProperty().bind(webContainer.heightProperty());

        final BorderPane borderPane = new BorderPane();
        borderPane.setTop(topRuler);
        borderPane.setLeft(leftRuler);
        borderPane.setCenter(webContainer);

        final Label info = new Label("Resize me!");
        final TextField widthField = new TextField();
        final TextField heightField = new TextField();
        final Label unit = new Label("in");

        widthField.setPrefColumnCount(3);
        heightField.setPrefColumnCount(3);

        HBox spring = new HBox();
        HBox.setHgrow(spring, Priority.ALWAYS);

        ImageView cancelIcon = new ImageView(SwingFXUtils.toFXImage(IconCache.getInstance().getImage(IconCache.Icon.CANCEL_ICON), null));
        ImageView doneIcon = new ImageView(SwingFXUtils.toFXImage(IconCache.getInstance().getImage(IconCache.Icon.ALLOW_ICON), null));
        final Button cancel = new Button("Cancel", cancelIcon);
        final Button done = new Button("Done", doneIcon);
        done.setAlignment(Pos.CENTER_RIGHT);

        final ToolBar toolBar = new ToolBar(
                info,
                widthField,
                heightField,
                unit,
                spring, //for spacing
                cancel,
                done
        );

        final BorderPane toolbarPane = new BorderPane();
        toolbarPane.setTop(toolBar);
        toolbarPane.setCenter(borderPane);

        Scene scene = new Scene(toolbarPane);
        previewStage.setTitle("HTML Preview - " + settings.getJobName());
        previewStage.setScene(scene);
        previewStage.sizeToScene();
        previewStage.show();

        // Listeners

        widthField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) return; //we just gained focus, we don't need to parse any input yet

            double newWidth = parseInput(widthField.getText());
            if (newWidth > 0) {
                log.warn("changing width");
                // The stage size and scene size are not the same. I think this is due to the window border. I could not find a more direct approach.
                double fudgeFactor = previewStage.getWidth() - scene.getWidth();
                previewStage.setWidth(newWidth * dpi + thickness + fudgeFactor);
            } else {
                DecimalFormat nf = new DecimalFormat("#.##");
                widthField.setText(nf.format((scene.getWidth() - thickness) / dpi));
            }
        });

        heightField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) return; //we just gained focus, we don't need to parse any input yet

            double newHeight = parseInput(heightField.getText());
            if (newHeight > 0) {
                log.warn("changing height");
                // The stage size and scene size are not the same. I think this is due to the window border. I could not find a more direct approach.
                double fudgeFactor = previewStage.getHeight() - scene.getHeight();
                previewStage.setHeight(newHeight * dpi + thickness + toolBar.getHeight() + fudgeFactor);
            } else {
                DecimalFormat nf = new DecimalFormat("#.##");
                heightField.setText(nf.format((scene.getHeight() - thickness - toolBar.getHeight()) / dpi));
            }
        });

        widthField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                info.requestFocus();
            }
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                DecimalFormat nf = new DecimalFormat("#.##");
                widthField.setText(nf.format((scene.getWidth() - thickness) / dpi));
                info.requestFocus();
            }
        });

        heightField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                info.requestFocus();
            }
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                DecimalFormat nf = new DecimalFormat("#.##");
                heightField.setText(nf.format((scene.getHeight() - thickness - toolBar.getHeight()) / dpi));
                info.requestFocus();
            }
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            double newWidth = (double)newVal;

            DecimalFormat nf = new DecimalFormat("#.##");
            widthField.setText(nf.format((newWidth - thickness) / dpi));
            heightField.setText(nf.format((scene.getHeight() - thickness - toolBar.getHeight()) / dpi));
            info.setText("Current: ");

            topRuler.setWidth(newWidth);
            drawTopRuler(topRuler, thickness, newWidth, dpi);
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            double newHeight = scene.getHeight() - thickness; //the top ruler cuts this ruler off

            DecimalFormat nf = new DecimalFormat("#.##");
            info.setText("Current: ");
            widthField.setText(nf.format((scene.getWidth() - thickness) / dpi));
            heightField.setText(nf.format((newHeight - toolBar.getHeight()) / dpi));

            leftRuler.setHeight(newHeight);
            drawLeftRuler(leftRuler, thickness, newHeight, dpi);
        });
    }

    private static double parseInput(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void drawLeftRuler(Canvas canvas, int thickness, double height, double dpi) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, thickness, height);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        Font font = Font.font("Arial", FontWeight.LIGHT, 8);
        gc.setFont(font);

        double spacing = dpi / 8;
        for (int i = 1; i * spacing < height; i++) {
            double lineHeight = thickness * 0.2;
            if (i % 4 == 0) lineHeight += thickness * 0.3;
            if (i % 8 == 0) {
                Text helper = new Text(String.valueOf(i / 8));
                helper.setFont(font);
                double textWidth = Math.ceil(helper.getLayoutBounds().getWidth());

                gc.save();
                gc.translate(0, i * spacing);
                gc.rotate(-90);
                gc.strokeText(String.valueOf(i / 8), -textWidth / 2, thickness - 2);
                gc.restore();
            }
            gc.strokeLine(0, i * spacing, lineHeight, i * spacing);
        }
    }

    private static void drawTopRuler(Canvas canvas, int thickness, double width, double dpi) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, thickness);
        gc.setStroke(Color.BLACK);
        gc.setFill(Color.GRAY);
        gc.fillRect(0,0, thickness, thickness);
        gc.setLineWidth(1);
        Font font = Font.font("Arial", FontWeight.LIGHT, 8);
        gc.setFont(font);

        double spacing = dpi / 8;
        for (int i = 1; i * spacing < width; i++) {
            double lineHeight = thickness * 0.2;
            if (i % 4 == 0) lineHeight += thickness * 0.3;
            if (i % 8 == 0) {
                Text helper = new Text(String.valueOf(i / 8));
                helper.setFont(font);
                double textWidth = Math.ceil(helper.getLayoutBounds().getWidth());
                gc.strokeText(String.valueOf(i / 8),
                              thickness + i * spacing - (textWidth / 2), thickness - 2);
            }
            gc.strokeLine(thickness + i * spacing, 0, thickness + i * spacing, lineHeight);
        }
    }
}
