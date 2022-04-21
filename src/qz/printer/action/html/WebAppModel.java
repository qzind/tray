package qz.printer.action.html;

public class WebAppModel {

    private String source;
    private boolean plainText;

    private double width, webWidth;
    private double height, webHeight;
    private boolean isScaled;
    private double zoom;

    public WebAppModel(String source, boolean plainText, double width, double height, boolean isScaled, double zoom) {
        this.source = source;
        this.plainText = plainText;
        this.width = width;
        this.height = height;
        this.webWidth = width * (96d / 72d);
        this.webHeight = height * (96d / 72d);
        this.isScaled = isScaled;
        this.zoom = zoom;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isPlainText() {
        return plainText;
    }

    public void setPlainText(boolean plainText) {
        this.plainText = plainText;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
        this.webWidth = width * (96d / 72d);
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
        this.webHeight = height * (96d / 72d);
    }

    public double getWebWidth() {
        return webWidth;
    }

    public double getWebHeight() {
        return webHeight;
    }

    public boolean isScaled() {
        return isScaled;
    }

    public void setScaled(boolean scaled) {
        isScaled = scaled;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }
}
