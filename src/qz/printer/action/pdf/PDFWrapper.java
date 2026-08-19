package qz.printer.action.pdf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

public class PDFWrapper implements Printable {

    private static final Logger log = LogManager.getLogger(PDFWrapper.class);

    private final PDDocument document;
    private final Scaling scaling;
    private final boolean adjustForReverseLandscape;

    private PDFPrintable printable;

    public PDFWrapper(PDDocument document, Scaling scaling, boolean showPageBorder, boolean ignoreTransparency, boolean useAlternateFontRendering, float dpi, boolean center, boolean adjustForReverseLandscape, RenderingHints hints) {
        this.document = document;
        this.scaling = scaling;
        this.adjustForReverseLandscape = adjustForReverseLandscape;

        PDFRenderer renderer = new ParamPdfRenderer(document, useAlternateFontRendering, ignoreTransparency);
        printable = new PDFPrintable(document, scaling, showPageBorder, dpi, center, renderer);
        printable.setRenderingHints(hints);
    }


    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        log.debug("Paper area: {},{}:{},{}", (int)pageFormat.getImageableX(), (int)pageFormat.getImageableY(),
                  (int)pageFormat.getImageableWidth(), (int)pageFormat.getImageableHeight());

        graphics.drawString(" ", 0, 0);

        if (adjustForReverseLandscape) {
            adjustPrintForOrientation(graphics, pageFormat, pageIndex);
        }

        return printable.print(graphics, pageFormat, pageIndex);
    }

    private void adjustPrintForOrientation(Graphics g, PageFormat format, int page) {
        PDRectangle bounds = document.getPage(page).getBBox();
        double docWidth = bounds.getWidth();
        double docHeight = bounds.getHeight();

        //reports dimensions flipped if rotated
        if (document.getPage(page).getRotation() % 180 == 90) {
            docWidth = bounds.getHeight();
            docHeight = bounds.getWidth();
        }

        //adjust across page to account for wrong origin corner
        double leftAdjust, topAdjust;

        if (scaling != Scaling.ACTUAL_SIZE) {
            if ((docWidth / docHeight) >= (format.getImageableWidth() / format.getImageableHeight())) {
                leftAdjust = 0;
                topAdjust = format.getImageableHeight() - (docHeight / (docWidth / format.getImageableWidth()));
            } else {
                leftAdjust = format.getImageableWidth() - (docWidth / (docHeight / format.getImageableHeight()));
                topAdjust = 0;
            }
        } else {
            leftAdjust = format.getImageableWidth() - docWidth;
            topAdjust = format.getImageableHeight() - docHeight;
        }

        log.info("Adjusting image by {},{} for selected orientation", leftAdjust, topAdjust);

        //reverse landscape will have only rotated doc, this adjusts page so [0,0] appears to come from correct corner
        g.translate((int)leftAdjust, (int)topAdjust);
    }

}
