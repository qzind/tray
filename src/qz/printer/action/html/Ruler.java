package qz.printer.action.html;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;

import java.text.DecimalFormat;

public class Ruler extends Canvas {
    private final Double thickness;
    private final boolean isVertical;
    private final DecimalFormat legendFormat = new DecimalFormat("#.#");

    private PreviewWindow.UNIT unit;

    public Ruler(Double thickness, PreviewWindow.UNIT unit, boolean isVertical) {
        this.thickness = thickness;
        this.unit = unit;
        this.isVertical = isVertical;

        widthProperty().addListener(evt -> draw());
        heightProperty().addListener(evt -> draw());
    }

    public void draw() {
        double width = getWidth();
        double height = getHeight();

        GraphicsContext gc = getGraphicsContext2D();
        gc.setTransform(new Affine());
        gc.clearRect(0, 0, width, height);

        gc.setStroke(Color.BLACK);

        double length;
        if (isVertical) {
            gc.rotate(-90);
            length = -height;
        } else {
            gc.setFill(Color.GRAY);
            gc.fillRect(0, 0, thickness, thickness);
            gc.translate(thickness, 0); //here, we fill the notch, and the start of the ruler is 'x = 0'
            length = width - thickness;
        }

        gc.setFill(Color.WHITESMOKE);
        gc.fillRect(0,0, length, thickness);
        gc.setLineWidth(1);
        Font font = Font.font("SansSerif", FontWeight.BOLD, 10);
        gc.setFont(font);

        int direction = isVertical ? -1 : 1; //the vertical line goes 'backwards'
        double spacing = (unit.dpu * unit.unitsPerLabel / unit.divisions) * direction;

        for (int i = 1; Math.abs(i * spacing) < Math.abs(length); i++) {
            double tickLength = thickness * 0.2;
            if (i % (unit.divisions / 2) == 0) tickLength += thickness * 0.3;
            if (i % unit.divisions == 0) {
                tickLength -= thickness * 0.1;
                Text helper = new Text(legendFormat.format(i * unit.unitsPerLabel / unit.divisions));
                helper.setFont(font);
                double textWidth = Math.ceil(helper.getLayoutBounds().getWidth());

                gc.setFill(Color.BLACK);
                gc.fillText(legendFormat.format(i * unit.unitsPerLabel / unit.divisions), i * spacing - (textWidth / 2), thickness - 2);
            }
            gc.strokeLine(i * spacing, 0, i * spacing, tickLength);
        }
    }

    public void setUnit(PreviewWindow.UNIT unit) {
        this.unit = unit;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        if (isVertical) return 20;
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        if (!isVertical) return 20;
        return getHeight();
    }

    @Override
    public double maxHeight(double width) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double maxWidth(double height) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double minWidth(double height) {
        return 1D;
    }

    @Override
    public double minHeight(double width) {
        return 1D;
    }

    @Override
    public void resize(double width, double height) {
        this.setWidth(width);
        this.setHeight(height);
    }
}