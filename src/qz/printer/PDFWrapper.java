package qz.printer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

public class PDFWrapper implements Printable {

    private static final Logger log = LoggerFactory.getLogger(PDFWrapper.class);

    private PDFPrintable printable;

    public PDFWrapper(PDDocument document, Scaling scaling, boolean showPageBorder, float dpi, boolean center) {
        printable = new PDFPrintable(document, scaling, showPageBorder, dpi, center);
    }


    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        log.debug("Paper area: {},{}:{},{}", (int)pageFormat.getImageableX(), (int)pageFormat.getImageableY(),
                  (int)pageFormat.getImageableWidth(), (int)pageFormat.getImageableHeight());

        graphics.drawString(" ", 0, 0);

        return printable.print(graphics, pageFormat, pageIndex);
    }
}
