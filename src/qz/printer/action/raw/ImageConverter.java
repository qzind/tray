package qz.printer.action.raw;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import qz.common.ByteArrayBuilder;

import java.awt.image.BufferedImage;

/**
 * Abstract class for converting a <code>BufferedImage</code> into a raw/pcl-specific byte array
 */
public abstract class ImageConverter implements ByteAppender {
    private static final Logger log = LogManager.getLogger(ImageConverter.class);

    private BufferedImage bufferedImage;
    private LanguageType languageType;

    /**
     * JSON parameter processor
     */
    abstract public void setParams(JSONObject params);

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
        log.info("Loaded BufferedImage: {}x{}", getWidth(), getHeight());
    }

    /**
     * Header commands to precede the image if printing a full label (or receipt, etc.)
     * e.g. <code>"^XA\n"</code>
     */
    abstract public String getHeader();

    /**
     * Footer commands to succeed the image if printing a full label (or receipt, etc.)
     * e.g. <code>"^XZ\n"</code>
     */
    abstract public String getFooter();

    public void setLanguageType(LanguageType languageType) {
        this.languageType = languageType;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public LanguageType getLanguageType() {
        return languageType;
    }

    public int getWidth() {
        return bufferedImage.getWidth();
    }

    public int getHeight() {
        return bufferedImage.getHeight();
    }
}
