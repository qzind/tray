package qz.printer.action.html;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ui.component.IconCache;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.function.Consumer;

public class PreviewWindow {
    private static final Logger log = LogManager.getLogger(PreviewWindow.class);

    private Consumer<Rectangle2D.Double> onPrint = ignore -> {};
    private Runnable onCancel = () -> {};

    private Stage stage;
    private Node content;
    private Ruler topRuler;
    private Ruler leftRuler;
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

    // Ruler fields
    private static final double thickness = 20;
    private DecimalFormat unitFormat = UNIT.IN.unitFormat;
    private double dpu = UNIT.IN.dpu;

    public PreviewWindow(StageStyle style, Node content) {
        this.content = content;
        stage = new Stage(style);
        initUiElements();
        registerTextFieldListeners();
    }

    public void show() {
        stage.show();

        javafx.geometry.Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        stage.setWidth(vb.getWidth() * 0.60);
        stage.setHeight(vb.getHeight() * 0.60);

        info.requestFocus(); // This is to remove focus from whatever textfield starts with focus
    }

    private void initUiElements() {
        //todo default units? probably look at the print request
        topRuler = new Ruler(thickness, UNIT.IN, false);
        leftRuler = new Ruler(thickness, UNIT.IN, true);

        //This contains the ruler canvases, and the content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);

        BorderPane rulerPane = new BorderPane();
        rulerPane.setTop(topRuler);
        rulerPane.setLeft(leftRuler);

        if (content instanceof WebView) {
            WebView webContent = (WebView)content;
            webContent.setMouseTransparent(true);
            //Putting a webview in a container helps prevent scrollbars. Clipping is preferred
            //Omitting this also breaks the option to 'find height' via js injection
            StackPane webContainer = new StackPane(webContent);
            webContainer.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));
            webContainer.setAlignment(webContainer, Pos.TOP_LEFT);
            webContainer.setAlignment(Pos.TOP_LEFT);

            rulerPane.setCenter(webContainer);
        } else {
            rulerPane.setCenter(content);
        }

        scrollPane.setContent(rulerPane);

        /// toolbar ///
        info = new Label("Dimensions WxH");

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

        cancel.setOnAction(actionEvent -> {
            onCancel.run();
            stage.close();
        });
        done.setOnAction(actionEvent -> {
            onPrint.accept(getDimensions());
            stage.close();
        });
        stage.setOnCloseRequest(evt -> onCancel.run());

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
        toolbarPane.setCenter(scrollPane);

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
            unitFormat = newUnit.unitFormat;

            topRuler.setUnit(newUnit);
            topRuler.draw();
            leftRuler.setUnit(newUnit);
            leftRuler.draw();

            setDimensions(contentWidth, contentHeight);
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

    public void setPreviewHeight(double height) {
        content.maxHeight(height);
        content.minHeight(height);
        content.prefHeight(height);

        if (content instanceof WebView) {
            WebView webContent = (WebView)content;
            webContent.setMinHeight(height);
            webContent.setMaxHeight(height);
            webContent.setPrefHeight(height);
        }

        setDimensions(contentWidth, height);
    }

    void setPreviewWidth(double width) {
        content.maxWidth(width);
        content.minWidth(width);
        content.prefWidth(width);

        //todo: test this with other content types
        if (content instanceof WebView) {
            WebView webContent = (WebView)content;
            webContent.setMinWidth(width);
            webContent.setMaxWidth(width);
            webContent.setPrefWidth(width);
        }

        setDimensions(width, contentHeight);
    }

    private void setDimensions(double width, double height) {
        //todo this method is no longer needed
        contentWidth = width;
        contentHeight = height;

        reportedWidth = unitFormat.format(contentWidth / dpu);
        reportedHeight = unitFormat.format(contentHeight / dpu);

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
