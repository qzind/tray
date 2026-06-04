package qz.printer.action.html;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Orientation;
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
import qz.printer.PrintOptions;
import qz.ui.component.IconCache;

import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.function.Consumer;

public class PreviewWindow {

    private Consumer<Rectangle2D.Double> onPrint = ignore -> {};
    private Runnable onCancel = () -> {};

    private final Stage stage;
    private final Node content;
    private Ruler topRuler;
    private Ruler leftRuler;
    private Label info;
    private TextField widthField;
    private TextField heightField;
    private ComboBox<String> units;

    // Dimensions
    private double contentWidth;
    private double contentHeight;
    private String reportedWidth;
    private String reportedHeight;

    // Ruler fields
    private static final double thickness = 20;
    private DecimalFormat unitFormat = Unit.IN.unitFormat;
    private double dpu = Unit.IN.dpu;

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
        topRuler = new Ruler(thickness, Unit.IN, Orientation.HORIZONTAL);
        leftRuler = new Ruler(thickness, Unit.IN, Orientation.VERTICAL);

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
            StackPane.setAlignment(webContainer, Pos.TOP_LEFT);
            webContainer.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));
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
                        PreviewWindow.Unit.IN.toString(),
                        PreviewWindow.Unit.CM.toString(),
                        PreviewWindow.Unit.MM.toString()
                );
        units = new ComboBox<>(options);
        units.setValue(PreviewWindow.Unit.IN.toString());

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

        //for spacing
        ToolBar toolBar = new ToolBar(
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

        Scene scene = new Scene(toolbarPane);
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
            Unit newUnit = Unit.fromString(unitString);
            setUnit(newUnit);
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

    public void setUnit(PrintOptions.Unit newUnit) {
        switch(newUnit) {
            case INCH:
                setUnit(Unit.IN);
                return;
            case CM:
                setUnit(Unit.CM);
                return;
            case MM:
                setUnit(Unit.MM);
        }
    }

    private void setUnit(Unit newUnit) {
        if (newUnit == null) return;
        dpu = newUnit.dpu;
        unitFormat = newUnit.unitFormat;

        topRuler.setUnit(newUnit);
        topRuler.draw();
        leftRuler.setUnit(newUnit);
        leftRuler.draw();

        units.setValue(newUnit.toString());

        setDimensions(contentWidth, contentHeight);
    }

    /**
     * This enum is necessary because it stores ruler/display
     * metadata that PrintOptions.Unit does not have
     */
    enum Unit {
        // WebView dimensions are specified in CSS pixels (1 CSS inch = 96 CSS px)
        IN("in", 96, 8, 1, "#.##"),
        CM("cm", 96 / inToCm, 10, 1, "#.#"),
        MM("mm", 96 / (inToCm * 10), 10, 10, "#.#");

        final String label;
        final DecimalFormat unitFormat;
        final double dpu, unitsPerLabel;
        final int divisions;
        Unit(String label, double dpu, int divisions, double unitsPerLabel, String formatString) {
            this.label = label;
            this.dpu = dpu;
            this.divisions = divisions;
            this.unitsPerLabel = unitsPerLabel;
            unitFormat = new DecimalFormat(formatString);
        }

        static PreviewWindow.Unit fromString(String value) {
            for (PreviewWindow.Unit u : PreviewWindow.Unit.values()) {
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