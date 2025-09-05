package qz.printer.action.html;

import com.sun.javafx.geom.Rectangle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import qz.ui.component.IconCache;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.function.Consumer;

public class PreviewWindow {
    private Consumer<Rectangle2D.Double> onPrint = ignore -> {};
    private Runnable onCancel = () -> {};

    private Stage stage;
    private Node content;
    private Canvas topRuler;
    private Canvas leftRuler;
    private Label info;
    private TextField widthField;
    private TextField heightField;
    private ComboBox<String> units;
    private ToolBar toolBar;
    private Scene scene;

    // Dimensions
    private double contentWidth;
    private double contentHeight;
    private String reportedWidth;
    private String reportedHeight;
    private double topRulerWidth;
    private double leftRulerHeight;

    // Ruler fields
    private static double thickness = 20;
    private final DecimalFormat legendFormat = new DecimalFormat("#.#");
    private DecimalFormat unitFormat = UNIT.IN.unitFormat;
    private double dpu = UNIT.IN.dpu;
    private double divisions = UNIT.IN.divisions;
    private double unitsPerLabel = UNIT.IN.unitsPerLabel;

    public PreviewWindow(StageStyle style, Node content) {
        this.content = content;
        stage = new Stage(style);
        initUiElements();
        registerTextFieldListeners();
        registerSizelisteners();
    }

    public void show() {
        stage.show();
        info.requestFocus(); // This is to remove focus from whatever textfield starts with focus
    }

    private void initUiElements() {
        topRuler = new Canvas();
        leftRuler = new Canvas();


        //This contains the ruler canvases, and the content
        final BorderPane rulerPane = new BorderPane();
        //Web views do not like being smaller than 1 pixel
        rulerPane.setMinHeight(topRulerWidth + 1);
        rulerPane.setTop(topRuler);
        rulerPane.setLeft(leftRuler);

        if (content instanceof WebView) {
            WebView webContent = (WebView)content;
            //Putting a webview in a container helps prevent scrollbars. Clipping is preferred
            //Omitting this also breaks the option to 'find height' via js injection
            StackPane webContainer = new StackPane(webContent);
            webContent.prefWidthProperty().bind(webContainer.widthProperty());
            webContent.prefHeightProperty().bind(webContainer.heightProperty());
            rulerPane.setCenter(webContainer);
        } else {
            rulerPane.setCenter(content);
        }

        /// toolbar ///
        info = new Label("WxH");

        widthField = new TextField();
        heightField = new TextField();
        widthField.setPrefColumnCount(3);
        heightField.setPrefColumnCount(3);

        ObservableList<String> options =
                FXCollections.observableArrayList(
                        PreviewWindow.UNIT.IN.toString(),
                        PreviewWindow.UNIT.CM.toString(),
                        PreviewWindow.UNIT.MM.toString(),
                        PreviewWindow.UNIT.PX.toString()
                );
        units = new ComboBox<>(options);
        units.setValue(PreviewWindow.UNIT.IN.toString());

        // visual spacing on the toolbar for the 2 buttons
        HBox spring = new HBox();
        HBox.setHgrow(spring, Priority.ALWAYS);

        ImageView cancelIcon = new ImageView(SwingFXUtils.toFXImage(IconCache.getInstance().getImage(IconCache.Icon.CANCEL_ICON), null));
        ImageView doneIcon = new ImageView(SwingFXUtils.toFXImage(IconCache.getInstance().getImage(IconCache.Icon.ALLOW_ICON), null));
        final Button cancel = new Button("Cancel", cancelIcon);
        final Button done = new Button("Print", doneIcon);

        cancel.setCancelButton(true);
        cancel.setOnAction(actionEvent -> {
            onCancel.run();
            stage.close();
        });
        done.setOnAction(actionEvent -> {
            onPrint.accept(getDimensions());
            stage.close();
        });
        stage.setOnCloseRequest(evt -> {
            onCancel.run();
        });

        toolBar = new ToolBar(
                info,
                widthField,
                heightField,
                units,
                spring, //for spacing
                cancel,
                done
        );

        final BorderPane toolbarPane = new BorderPane();
        toolbarPane.setTop(toolBar);
        toolbarPane.setCenter(rulerPane);

        scene = new Scene(toolbarPane);
        stage.setScene(scene);
        stage.sizeToScene();
    }

    private Rectangle2D.Double getDimensions() {
        return new Rectangle2D.Double(
                0,
                0,
                contentWidth,
                contentHeight
        );
    }

    private void registerTextFieldListeners() {
        // A new unit has been selected. Redraw the rulers
        units.valueProperty().addListener((ov, t, unitString) -> {
            UNIT newUnit = UNIT.fromString(unitString);
            if (newUnit == null) return;
            dpu = newUnit.dpu;
            unitsPerLabel = (int)newUnit.unitsPerLabel;
            divisions = newUnit.divisions;
            unitFormat = newUnit.unitFormat;

            calculateDimensions(scene.getWidth(), scene.getHeight());
            updateSizeLabels();
            drawTopRuler();
            drawLeftRuler();
        });

        // A new dimension was given, resize the window
        widthField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) return; //we just gained focus, we don't need to parse any input yet

            double newWidth = parseInput(widthField.getText()) * dpu;
            if (newWidth > 0) {
                setPreviewWidth(newWidth);
            } else {
                widthField.setText(reportedWidth);
            }
        });

        heightField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) return; //we just gained focus, we don't need to parse any input yet

            double newHeight = parseInput(heightField.getText());
            if (newHeight > 0) {
                setPreviewHeight(newHeight * dpu);
            } else {
                heightField.setText(reportedHeight);
            }
        });

        // Escape and enter need to end focus. This causes the focus listener to fire
        widthField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                info.requestFocus();
            }
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                widthField.setText(reportedWidth);
                info.requestFocus();
            }
        });

        heightField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                info.requestFocus();
            }
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                heightField.setText(reportedHeight);
                info.requestFocus();
            }
        });
    }

    private void registerSizelisteners() {
        // When the window is resized, live-update the dimension fields
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            calculateDimensions((Double)newVal, scene.getHeight());
            updateSizeLabels();
            drawTopRuler();
            scaleContent();
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            calculateDimensions(scene.getWidth(), (Double)newVal);
            updateSizeLabels();
            drawLeftRuler();
            scaleContent();
        });
    }

    public void setPreviewHeight(double height) {
        // The stage size and scene size are not the same. I think this is due to the window border. I could not find a more direct approach.
        double fudgeFactor = stage.getHeight() - scene.getHeight();
        stage.setHeight(height + thickness + toolBar.getHeight() + fudgeFactor);
    }

    void setPreviewWidth(double width) {
        // Same as set height but without the toolbar
        double fudgeFactor = stage.getWidth() - scene.getWidth();
        stage.setWidth(width + thickness + fudgeFactor);
    }

    private void updateSizeLabels() {
        widthField.setText(reportedWidth);
        heightField.setText(reportedHeight);
    }

    private void scaleContent() {
        content.maxWidth(contentWidth);
        content.maxHeight(contentHeight);
    }

    private void calculateDimensions(double width, double height) {
        contentWidth = width - thickness;
        contentHeight = height - thickness - toolBar.getHeight();

        topRulerWidth = contentWidth + thickness;
        leftRulerHeight = contentHeight;

        reportedWidth = unitFormat.format(contentWidth / dpu);
        reportedHeight = unitFormat.format(contentHeight / dpu);
    }

    private void drawLeftRuler() {
        leftRuler.setHeight(leftRulerHeight);
        leftRuler.setWidth(thickness);

        GraphicsContext gc = leftRuler.getGraphicsContext2D();
        gc.clearRect(0, 0, thickness, leftRulerHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        Font font = Font.font("SansSerif", FontWeight.BOLD, 10);
        gc.setFont(font);

        double spacing = dpu * unitsPerLabel / divisions;
        for (int i = 1; i * spacing < leftRulerHeight; i++) {
            double tickLength = thickness * 0.2;
            if (i % (divisions / 2) == 0) tickLength += thickness * 0.3;
            if (i % divisions == 0) {
                tickLength -= thickness * 0.1;
                Text helper = new Text(legendFormat.format(i * unitsPerLabel / divisions));
                helper.setFont(font);
                double textWidth = Math.ceil(helper.getLayoutBounds().getWidth());

                gc.save();
                gc.translate(0, i * spacing);
                gc.rotate(-90);
                gc.setFill(Color.BLACK);
                gc.fillText(legendFormat.format(i * unitsPerLabel / divisions), -textWidth / 2, thickness - 2);
                gc.restore();
            }
            gc.strokeLine(0, i * spacing, tickLength, i * spacing);
        }
    }

    private void drawTopRuler() {
        topRuler.setHeight(thickness);
        topRuler.setWidth(topRulerWidth);

        GraphicsContext gc = topRuler.getGraphicsContext2D();
        gc.clearRect(0, 0, topRulerWidth, thickness);
        gc.setStroke(Color.BLACK);
        gc.setFill(Color.GRAY);
        gc.fillRect(0,0, thickness, thickness);
        gc.setLineWidth(1);
        Font font = Font.font("SansSerif", FontWeight.BOLD, 10);
        gc.setFont(font);

        double spacing = dpu * unitsPerLabel / divisions;
        for (int i = 1; i * spacing < topRulerWidth; i++) {
            double tickLength = thickness * 0.2;
            if (i % (divisions / 2) == 0) tickLength += thickness * 0.3;
            if (i % divisions == 0) {
                tickLength -= thickness * 0.1;
                Text helper = new Text(legendFormat.format(i * unitsPerLabel / divisions));
                helper.setFont(font);
                double textWidth = Math.ceil(helper.getLayoutBounds().getWidth());

                gc.setFill(Color.BLACK);
                gc.fillText(legendFormat.format(i * unitsPerLabel / divisions),
                            thickness + i * spacing - (textWidth / 2), thickness - 2);
            }
            gc.strokeLine(thickness + i * spacing, 0, thickness + i * spacing, tickLength);
        }
    }

    private static double parseInput(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static final double inToCm = 2.54;

    public void setOnPrint(Consumer<Rectangle2D.Double> onPrint) {
        this.onPrint = onPrint;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    enum UNIT {
        //todo 72/96 needs to be cleared up. should pixel be 72 or 96, also 72 should be pulled from somewhere
        IN("in", 96, 8, 1, "#.##"),
        CM("cm", 96 / inToCm, 10, 1, "#.#"),
        MM("mm", 96 / (inToCm * 10), 10, 10, "#.#"),
        PX("px", 1, 5, 50, "#");

        final String label;
        public final DecimalFormat unitFormat;
        public final double dpu, unitsPerLabel;
        public final int divisions;
        UNIT(String label, double dpu, int divisions, double unitsPerLabel, String formatString) {
            this.label = label;
            this.dpu = dpu;
            this.divisions = divisions;
            this.unitsPerLabel = unitsPerLabel;
            unitFormat = new DecimalFormat(formatString);
        }

        static PreviewWindow.UNIT fromString(String value) {
            for (PreviewWindow.UNIT u : PreviewWindow.UNIT.values()) {
                if (value.equals(u.toString())) return u;
            }
            return null;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
