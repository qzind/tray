package qz.printer.rendering;


import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * PageDrawer overrides derived from PDFBOX 1.8
 * with the help of Alexander Scherbatiy
 */

public class PdfFontPageDrawer extends PageDrawer {

    private static final Logger log = LoggerFactory.getLogger(PdfFontPageDrawer.class);

    private String fallbackFont = "helvetica"; //todo - definable parameter?
    private final Map<PDFont,Font> fonts = new HashMap<>();

    public PdfFontPageDrawer(PageDrawerParameters parameters) throws IOException {
        super(parameters);
    }

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        // fall-back to draw Glyph when awt font has not been found

        AffineTransform at = textRenderingMatrix.createAffineTransform();
        at.concatenate(font.getFontMatrix().createAffineTransform());

        Graphics2D graphics = getGraphics();

        AffineTransform prevTx = graphics.getTransform();
        stretchNonEmbeddedFont(at, font, code, displacement);
        // Probably relates to DEFAULT_FONT_MATRIX transform from PDFont
        at.scale(100, -100);
        graphics.transform(at);

        graphics.setComposite(getGraphicsState().getNonStrokingJavaComposite());
        //FIXME!! Private methods in PageDrawer - custom build for protected visibility
        graphics.setPaint(getNonStrokingPaint());
        setClip();

        Font prevFont = graphics.getFont();
        Font awtFont = getAwtFont(font);
        graphics.setFont(awtFont);

        graphics.drawString(font.toUnicode(code), 0, 0);

        graphics.setFont(prevFont);
        graphics.setTransform(prevTx);
    }

    private void stretchNonEmbeddedFont(AffineTransform at, PDFont font, int code, Vector displacement) throws IOException {
        // Stretch non-embedded glyph if it does not match the height/width contained in the PDF.
        // Vertical fonts have zero X displacement, so the following code scales to 0 if we don't skip it.
        if (!font.isEmbedded() && !font.isVertical() && !font.isStandard14() && font.hasExplicitWidth(code)) {
            float fontWidth = font.getWidthFromFont(code);
            if (fontWidth > 0 && Math.abs(fontWidth - displacement.getX() * 1000) > 0.0001) {
                float pdfWidth = displacement.getX() * 1000;
                at.scale(pdfWidth / fontWidth, 1);
            }
        }
    }

    private Font cacheFont(PDFont font, Font awtFont) {
        fonts.put(font, awtFont);
        return awtFont;
    }

    private Font getAwtFont(PDFont font) throws IOException {
        Font awtFont = fonts.get(font);

        if (awtFont != null) {
            return awtFont;
        }

        if (font instanceof PDType0Font) {
            return cacheFont(font, getPDType0AwtFont((PDType0Font)font));
        }

        if (font instanceof PDType1Font) {
            return cacheFont(font, getPDType1AwtFont((PDType1Font)font));
        }

        String msg = String.format("Not yet implemented: %s", font.getClass().getName());
        throw new UnsupportedOperationException(msg);
    }

    public Font getPDType0AwtFont(PDType0Font font) throws IOException {
        Font awtFont = null;
        PDCIDFont descendantFont = font.getDescendantFont();

        if (descendantFont != null) {

            if (descendantFont instanceof PDCIDFontType2) {
                awtFont = getPDCIDAwtFontType2((PDCIDFontType2)descendantFont);
            }
            if (awtFont != null) {
                /*
                 * Fix Oracle JVM Crashes.
                 * Tested with Oracle JRE 6.0_45-b06 and 7.0_21-b11
                 */
                awtFont.canDisplay(1);
            }
        }

        if (awtFont == null) {
            awtFont = FontManager.getAwtFont(fallbackFont);
            log.debug("Using font {} instead of {}", awtFont.getName(), descendantFont.getFontDescriptor().getFontName());
        }

        return awtFont.deriveFont(10f);
    }

    private Font getPDType1AwtFont(PDType1Font font) throws IOException {
        Font awtFont = null;
        String baseFont = font.getBaseFont();
        PDFontDescriptor fd = font.getFontDescriptor();

        if (fd != null) {
            if (fd.getFontFile() != null) {
                try {
                    // create a type1 font with the embedded data
                    awtFont = Font.createFont(Font.TYPE1_FONT, fd.getFontFile().createInputStream());
                }
                catch(java.awt.FontFormatException e) {
                    log.debug("Can't read the embedded type1 font {}", fd.getFontName());
                }
            }
            if (awtFont == null) {
                // check if the font is part of our environment
                if (fd.getFontName() != null) {
                    awtFont = FontManager.getAwtFont(fd.getFontName());
                }
                if (awtFont == null) {
                    log.debug("Can't find the specified font {}", fd.getFontName());
                }
            }
        } else {
            // check if the font is part of our environment
            awtFont = FontManager.getAwtFont(baseFont);
            if (awtFont == null) {
                log.debug("Can't find the specified basefont {}", baseFont);
            }
        }

        if (awtFont == null) {
            // we can't find anything, so we have to use the standard font
            awtFont = FontManager.getAwtFont(fallbackFont);
            log.debug("Using font {} instead", awtFont.getName());
        }

        return awtFont.deriveFont(20f);
    }

    public Font getPDCIDAwtFontType2(PDCIDFontType2 font) throws IOException {
        Font awtFont = null;
        PDFontDescriptor fd = font.getFontDescriptor();
        PDStream ff2Stream = fd.getFontFile2();

        if (ff2Stream != null) {
            try {
                // create a font with the embedded data
                awtFont = Font.createFont(Font.TRUETYPE_FONT, ff2Stream.createInputStream());
            }
            catch(java.awt.FontFormatException f) {
                log.debug("Can't read the embedded font {}", fd.getFontName());
            }
            if (awtFont == null) {
                if (fd.getFontName() != null) {
                    awtFont = FontManager.getAwtFont(fd.getFontName());
                }
                if (awtFont != null) {
                    log.debug("Using font {} instead", awtFont.getName());
                }
            }
        }

        return awtFont;
    }
}
