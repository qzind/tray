package qz.printer.action;

public class WebAppModel {

    private String source;
    private boolean plainText = false;

    private double webWidth = 0.0;
    private double webHeight = 0.0;
    private boolean isScaled = true;
    private double zoom = 1.0;

    public WebAppModel(String source, boolean plainText, double webWidth, double webHeight, boolean isScaled, double zoom) {
        //values supplied are at print dpi, scale up to web dpi here
        double increase = 96d / 72d;

        this.source = source;
        this.plainText = plainText;
        this.webWidth = webWidth * increase;
        this.webHeight = webHeight * increase;
        this.isScaled = isScaled;
        this.zoom = zoom;
    }

    public String getSource() {
        return source;
    }

    public boolean isPlainText() {
        return plainText;
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

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

}
