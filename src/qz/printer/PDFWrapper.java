package qz.printer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;

import javax.print.attribute.standard.OrientationRequested;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

public class PDFWrapper implements Printable {

    private static final Logger log = LoggerFactory.getLogger(PDFWrapper.class);

    private PDDocument document;
    private Scaling scaling;
    private OrientationRequested orientation = OrientationRequested.PORTRAIT;

    private PDFPrintable printable;

    public PDFWrapper(PDDocument document, Scaling scaling, boolean showPageBorder, float dpi, boolean center, PrintOptions.Orientation orientation) {
        this.document = document;
        this.scaling = scaling;
        if (orientation != null) {
            this.orientation = orientation.getAsAttribute();
        }

        printable = new PDFPrintable(document, scaling, showPageBorder, dpi, center);
    }


    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        log.debug("Paper area: {},{}:{},{}", (int)pageFormat.getImageableX(), (int)pageFormat.getImageableY(),
                  (int)pageFormat.getImageableWidth(), (int)pageFormat.getImageableHeight());

        graphics.drawString(" ", 0, 0);

        //reverse fix for OSX
        if (SystemUtilities.isMac() && orientation == OrientationRequested.REVERSE_LANDSCAPE) {
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
