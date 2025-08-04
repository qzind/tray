package qz.printer.action.html;

import com.sun.javafx.print.Units;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.print.JobSettings;
import javafx.print.Paper;
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

public class PreviewHTML {
    enum UNIT {
        IN("in", 72, 8, 1, "#.##"),
        CM("cm", 72 / inToCm, 10, 1, "#.#"),
        MM("mm", 72 / (inToCm * 10), 10, 10, "#.#"),
        PX("px", 1, 5, 50, "#");

        String label;
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



    private static final Logger log = LogManager.getLogger(PreviewHTML.class);

    private final WebAppModel model;
    private final JobSettings settings;
    private final Stage previewStage;

    private final WebView webPreview = new WebView();
    private TextField widthField;
    private TextField heightField;
    private Canvas leftRuler;
    private Canvas topRuler;
    private Scene scene;
    private ToolBar toolBar;

    private int thickness = 20;

    //private Units unit = Units.INCH;
    //private String unitFormat = "#.##";
    private DecimalFormat unitFormat = UNIT.IN.unitFormat;
    private double dpu = 72;
    private double divisions = 8;
    private int increment = 1;

    private double contentWidth;
    private double contentHeight;
    private double topRulerWidth;
    private double leftRulerHeight;

    //private Units unit = Units.MM;
    //private String unitFormat = "#.#";
    //private double dpu = 72 / (inToCm * 10);
    //private double divisions = 10;
    //private int increment = 10;

    //private Units unit = Units.CM;
    //private String unitFormat = "#.#";
    //private double dpu = 72 / (inToCm);
    //private double divisions = 10;
    //private int increment = 1;

    //private Units unit = Units.POINT;
    //private String unitFormat = "#";
    //private double dpu = 1;
    //private double divisions = 5;
    //private int increment = 50;

    private static double inToCm = 2.54;

    public PreviewHTML(final WebAppModel model, JobSettings settings, Stage previewStage, Paper paper) {
        this.model = model;
        this.settings = settings;
        this.previewStage = previewStage;

        initialize(paper.getWidth(), paper.getHeight());

        Platform.runLater(() -> {
            previewStage.toFront();
            webPreview.requestFocus();
        });
    }

    private void initialize(double width, double height) {
        if (model.isPlainText()) {
            webPreview.getEngine().loadContent(model.getSource(), "text/html");
        } else {
            webPreview.getEngine().load(model.getSource());
        }
        //webPreview.setZoom(0.5);

        leftRuler = new Canvas(thickness, height);
        topRuler = new Canvas(width + thickness, thickness);

        drawLeftRuler(leftRuler, thickness, leftRuler.getHeight());
        drawTopRuler(topRuler, thickness, topRuler.getWidth());

        StackPane webContainer = new StackPane(webPreview);
        webPreview.prefWidthProperty().bind(webContainer.widthProperty());
        webPreview.prefHeightProperty().bind(webContainer.heightProperty());

        final BorderPane borderPane = new BorderPane();
        borderPane.setTop(topRuler);
        borderPane.setLeft(leftRuler);
        borderPane.setCenter(webContainer);

        final Label info = new Label("Resize me!");
        widthField = new TextField();
        heightField = new TextField();
        ObservableList<String> options =
                FXCollections.observableArrayList(
                        UNIT.IN.toString(),
                        UNIT.CM.toString(),
                        UNIT.MM.toString(),
                        UNIT.PX.toString()
                );
        final ComboBox unit = new ComboBox(options);
        unit.setValue(UNIT.IN.toString());

        widthField.setPrefColumnCount(3);
        heightField.setPrefColumnCount(3);

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
                unit,
                spring, //for spacing
                cancel,
                done
        );

        final BorderPane toolbarPane = new BorderPane();
        toolbarPane.setTop(toolBar);
        toolbarPane.setCenter(borderPane);

        scene = new Scene(toolbarPane);
        previewStage.setTitle("HTML Preview - " + settings.getJobName());
        previewStage.setScene(scene);
        previewStage.sizeToScene();
        previewStage.show();

        updateSizeLabels(scene.getWidth(), scene.getHeight() - thickness);

        // Listeners

        unit.valueProperty().addListener((ChangeListener<String>)(ov, t, unitString) -> {
            UNIT newUnit = UNIT.fromString(unitString);
            dpu = newUnit.dpu;
            increment = (int)newUnit.unitsPerLabel;
            divisions = newUnit.divisions;
            unitFormat = newUnit.unitFormat;

            updateSizeLabels(scene.getWidth(), scene.getHeight() - thickness);
            drawTopRuler(topRuler, thickness, scene.getWidth());
            drawLeftRuler(leftRuler, thickness, scene.getHeight() - thickness);
        });

        widthField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) return; //we just gained focus, we don't need to parse any input yet

            double newWidth = parseInput(widthField.getText());
            if (newWidth > 0) {
                log.warn("changing width");
                // The stage size and scene size are not the same. I think this is due to the window border. I could not find a more direct approach.
                double fudgeFactor = previewStage.getWidth() - scene.getWidth();
                previewStage.setWidth(newWidth * dpu + thickness + fudgeFactor);
            } else {
                widthField.setText(unitFormat.format((scene.getWidth() - thickness) / dpu));
            }
        });

        heightField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) return; //we just gained focus, we don't need to parse any input yet

            double newHeight = parseInput(heightField.getText());
            if (newHeight > 0) {
                log.warn("changing height");
                // The stage size and scene size are not the same. I think this is due to the window border. I could not find a more direct approach.
                double fudgeFactor = previewStage.getHeight() - scene.getHeight();
                previewStage.setHeight(newHeight * dpu + thickness + toolBar.getHeight() + fudgeFactor);
            } else {
                heightField.setText(unitFormat.format((scene.getHeight() - thickness - toolBar.getHeight()) / dpu));
            }
        });

        widthField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                info.requestFocus();
            }
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                widthField.setText(unitFormat.format((scene.getWidth() - thickness) / dpu));
                info.requestFocus();
            }
        });

        heightField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                info.requestFocus();
            }
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                heightField.setText(unitFormat.format((scene.getHeight() - thickness - toolBar.getHeight()) / dpu));
                info.requestFocus();
            }
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            double newWidth = (double)newVal;

            info.setText("Current: ");
            updateSizeLabels(newWidth, scene.getHeight());

            topRuler.setWidth(newWidth);
            drawTopRuler(topRuler, thickness, newWidth);
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            double newHeight = (double)newVal - thickness; //the top ruler cuts this ruler off

            info.setText("Current: ");
            updateSizeLabels(scene.getWidth(), newHeight);

            leftRuler.setHeight(newHeight);
            drawLeftRuler(leftRuler, thickness, newHeight);
        });
    }

    private void updateSizeLabels(double width, double height) {
        widthField.setText(unitFormat.format((width - thickness) / dpu));
        heightField.setText(unitFormat.format((height - toolBar.getHeight()) / dpu));
    }

    private static double parseInput(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void drawLeftRuler(Canvas canvas, int thickness, double height) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, thickness, height);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        Font font = Font.font("Arial", FontWeight.LIGHT, 8);
        gc.setFont(font);

        double spacing = dpu * increment / divisions;
        for (int i = 1; i * spacing < height; i++) {
            double lineHeight = thickness * 0.2;
            if (i % (divisions / 2) == 0) lineHeight += thickness * 0.2;
            if (i % divisions == 0) {
                Text helper = new Text(String.valueOf(i * increment / (int)divisions));
                helper.setFont(font);
                double textWidth = Math.ceil(helper.getLayoutBounds().getWidth());

                gc.save();
                gc.translate(0, i * spacing);
                gc.rotate(-90);
                gc.strokeText(String.valueOf(i * increment/ (int)divisions), -textWidth / 2, thickness - 2);
                gc.restore();
            }
            gc.strokeLine(0, i * spacing, lineHeight, i * spacing);
        }
    }

    private void drawTopRuler(Canvas canvas, int thickness, double width) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, thickness);
        gc.setStroke(Color.BLACK);
        gc.setFill(Color.GRAY);
        gc.fillRect(0,0, thickness, thickness);
        gc.setLineWidth(1);
        Font font = Font.font("Arial", FontWeight.LIGHT, 8);
        gc.setFont(font);

        double spacing = dpu * increment / divisions;
        for (int i = 1; i * spacing < width; i++) {
            double lineHeight = thickness * 0.2;
            if (i % (divisions / 2) == 0) lineHeight += thickness * 0.2;
            if (i % divisions == 0) {
                Text helper = new Text(String.valueOf(i * increment / (int)divisions));
                helper.setFont(font);
                double textWidth = Math.ceil(helper.getLayoutBounds().getWidth());
                gc.strokeText(String.valueOf(i * increment / (int)divisions),
                              thickness + i * spacing - (textWidth / 2), thickness - 2);
            }
            gc.strokeLine(thickness + i * spacing, 0, thickness + i * spacing, lineHeight);
        }
    }
}
