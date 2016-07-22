package qz.printer.action;

public class WebAppModel {

    private String source;
    private boolean plainText = false;

    private double webWidth = 0.0;
    private double webHeight = 0.0;
    private boolean isScaled = true;

    public WebAppModel(String source, boolean plainText, double webWidth, double webHeight, boolean isScaled) {
        this.source = source;
        this.plainText = plainText;
        this.webWidth = webWidth;
        this.webHeight = webHeight;
        this.isScaled = isScaled;
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

}
