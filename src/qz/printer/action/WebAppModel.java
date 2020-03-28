package qz.printer.action;

public class WebAppModel {

    private String source;
    private boolean plainText;

    private double webWidth;
    private double webHeight;
    private boolean isScaled;
    private double zoom;

    public WebAppModel(String source, boolean plainText, double webWidth, double webHeight, boolean isScaled, double zoom) {
        this.source = source;
        this.plainText = plainText;
        this.webWidth = webWidth;
        this.webHeight = webHeight;
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

    public double getWebWidth() {
        return webWidth;
    }

    public void setWebWidth(double webWidth) {
        this.webWidth = webWidth;
    }

    public double getWebHeight() {
        return webHeight;
    }

    public void setWebHeight(double webHeight) {
        this.webHeight = webHeight;
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
