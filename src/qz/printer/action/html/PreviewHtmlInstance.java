package qz.printer.action.html;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.print.JobSettings;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
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
import java.util.concurrent.CountDownLatch;

class PreviewHtmlInstance extends AbstractHtmlInstance {
    enum UNIT {
        IN("in", 96, 8, 1, "#.##"),
        CM("cm", 72 / inToCm, 10, 1, "#.#"),
        MM("mm", 72 / (inToCm * 10), 10, 10, "#.#"),
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

        static UNIT fromString(String value) {
            for (UNIT u : UNIT.values()) {
                if (value.equals(u.toString())) return u;
            }
            return null;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final Logger log = LogManager.getLogger(PreviewHtmlInstance.class);

    private WebAppModel model;
    private final JobSettings settings;

    private TextField widthField;
    private TextField heightField;
    private Canvas leftRuler;
    private Canvas topRuler;
    private Scene scene;
    private ToolBar toolBar;

    private int thickness = 20;
    private final DecimalFormat legendFormat = new DecimalFormat("#.#");

    private DecimalFormat unitFormat = UNIT.IN.unitFormat;
    private double dpu = UNIT.IN.dpu;
    private double divisions = UNIT.IN.divisions;
    private double unitsPerLabel = UNIT.IN.unitsPerLabel;

    private double contentWidth;
    private double contentHeight;
    private String reportedWidth;
    private String reportedHeight;
    private double topRulerWidth;
    private double leftRulerHeight;

    private static final double inToCm = 2.54;

    //rename or move this
    private CountDownLatch initLatch = new CountDownLatch(1);


    protected ChangeListener<Worker.State> stateListener = (ov, oldState, newState) -> {
        if (contentHeight <= 0) {
            disableHtmlScrollbars();
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                }
                catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }

                Platform.runLater(() -> {
                    setPreviewHeight(findHeight() / dpu);
                });
            }).start();
        }
    };

    public PreviewHtmlInstance(Stage stage) {
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

            Worker<Void> worker = webView.getEngine().getLoadWorker();
            worker.stateProperty().addListener(stateListener);
        });
    }

    private void initialize(double width, double height) {
        loadSource(model);

        leftRulerHeight = height;
        leftRuler = new Canvas(thickness, leftRulerHeight);
        topRulerWidth = width + thickness; // This ruler includes the corner piece
        topRuler = new Canvas(topRulerWidth, thickness);

        drawLeftRuler();
        drawTopRuler();

        //Putting the webview in a container helps prevent scrollbars. Clipping is preferred
        StackPane webContainer = new StackPane(webView);
        webView.prefWidthProperty().bind(webContainer.widthProperty());
        webView.prefHeightProperty().bind(webContainer.heightProperty());

        //This contains the ruler canvases, and the webContainer
        final BorderPane rulerPane = new BorderPane();
        rulerPane.setTop(topRuler);
        rulerPane.setLeft(leftRuler);
        rulerPane.setCenter(webContainer);

        final Label info = new Label();
        widthField = new TextField();
        heightField = new TextField();
        ObservableList<String> options =
                FXCollections.observableArrayList(
                        UNIT.IN.toString(),
                        UNIT.CM.toString(),
                        UNIT.MM.toString(),
                        UNIT.PX.toString()
                );
        final ComboBox<String> units = new ComboBox<>(options);
        units.setValue(UNIT.IN.toString());

        widthField.setPrefColumnCount(3);
        heightField.setPrefColumnCount(3);

        // visual spacing on the toolbar for the 2 buttons
        HBox spring = new HBox();
        HBox.setHgrow(spring, Priority.ALWAYS);

        ImageView cancelIcon = new ImageView(SwingFXUtils.toFXImage(IconCache.getInstance().getImage(IconCache.Icon.CANCEL_ICON), null));
        ImageView doneIcon = new ImageView(SwingFXUtils.toFXImage(IconCache.getInstance().getImage(IconCache.Icon.ALLOW_ICON), null));
        final Button cancel = new Button("Cancel", cancelIcon);
        final Button done = new Button("Print", doneIcon);
        done.setAlignment(Pos.CENTER_RIGHT);

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
        renderStage.setTitle("HTML Preview"); //- " + settings.getJobName());
        renderStage.setScene(scene);
        renderStage.sizeToScene();
        renderStage.show();

        calculateDimensions(scene.getWidth(), scene.getHeight());
        updateSizeLabels();

        // Listeners

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

            double newWidth = parseInput(widthField.getText());
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
                setPreviewHeight(newHeight);
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

        // When the window is resized, live-update the dimension fields
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            calculateDimensions((Double)newVal, scene.getHeight());
            updateSizeLabels();
            drawTopRuler();
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            calculateDimensions(scene.getWidth(), (Double)newVal);
            updateSizeLabels();
            drawLeftRuler();
        });
    }

    private void setPreviewHeight(double height) {
        // The stage size and scene size are not the same. I think this is due to the window border. I could not find a more direct approach.
        double fudgeFactor = renderStage.getHeight() - scene.getHeight();
        renderStage.setHeight(height * dpu + thickness + toolBar.getHeight() + fudgeFactor);
    }

    void setPreviewWidth(double width) {
        // Same as set heighdimentiont but without the toolbar
        double fudgeFactor = renderStage.getWidth() - scene.getWidth();
        renderStage.setWidth(width * dpu + thickness + fudgeFactor);
    }

    private void updateSizeLabels() {
        widthField.setText(reportedWidth);
        heightField.setText(reportedHeight);
    }

    private static double parseInput(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
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
}
