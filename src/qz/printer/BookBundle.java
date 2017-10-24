package qz.printer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;

import javax.print.attribute.standard.OrientationRequested;
import java.awt.*;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper of the {@code Book} class as a {@code Printable} type,
 * since PrinterJob implementations do not seem to handle the {@code Pageable} interface properly.
 */
public class BookBundle extends Book implements Printable {

    private static final Logger log = LoggerFactory.getLogger(BookBundle.class);

    private List<PDDocument> refDocs = new ArrayList<>();
    private OrientationRequested orient = null;
    private boolean scale = false;

    private Printable lastPrint;
    private int lastStarted;

    public BookBundle(PrintOptions.Orientation orientation, boolean scaled) {
        super();

        if (orientation != null) {
            orient = orientation.getAsAttribute();
        }
        scale = scaled;
    }

    public void append(PDDocument doc, Printable painter, PageFormat page, int numPages) {
        for(int i = 0; i < numPages; i++) {
            refDocs.add(doc);
        }

        append(painter, page, numPages);
    }

    @Override
    public int print(Graphics g, PageFormat format, int pageIndex) throws PrinterException {
        log.trace("Requested page {} for printing", pageIndex);

        if (pageIndex < getNumberOfPages()) {
            Printable printable = getPrintable(pageIndex);
            if (printable != lastPrint) {
                lastPrint = printable;
                lastStarted = pageIndex;
            }

            log.debug("Paper area: {},{}:{},{}", (int)format.getImageableX(), (int)format.getImageableY(),
                      (int)format.getImageableWidth(), (int)format.getImageableHeight());

            if (SystemUtilities.isMac()) {
                adjustPrintForOrientation(g, format, pageIndex);
            }

            return printable.print(g, format, pageIndex - lastStarted);
        }

        return NO_SUCH_PAGE;
    }

    /** Fixes landscape orientations on OSX */
    private void adjustPrintForOrientation(Graphics g, PageFormat format, int pageIndex) {
        PDRectangle bounds = refDocs.get(pageIndex).getPage(pageIndex - lastStarted).getBBox();
        double topAdjust = 0, leftAdjust = 0;

        if (orient == OrientationRequested.LANDSCAPE) {
            //adjust down page to account for wrong origin corner
            if (scale) {
                //only adjusts vertically, so only check if scale affects width
                if ((bounds.getWidth() / bounds.getHeight()) < (format.getImageableWidth() / format.getImageableHeight())) {
                    leftAdjust = format.getImageableHeight() - (bounds.getWidth() * (format.getImageableWidth() / bounds.getHeight()));
                }
            } else {
                leftAdjust = format.getImageableHeight() - bounds.getWidth();
            }
        } else if (orient == OrientationRequested.REVERSE_LANDSCAPE) {
            //adjust across page to account for wrong origin corner
            if (scale) {
                //only adjusts horizontally, so only check is scale affects height
                if ((bounds.getWidth() / bounds.getHeight()) >= (format.getImageableWidth() / format.getImageableHeight())) {
                    topAdjust = format.getImageableWidth() - (bounds.getHeight() * (format.getImageableHeight() / bounds.getWidth()));
                }
            } else {
                topAdjust = format.getImageableWidth() - bounds.getHeight();
            }
        }

        //landscape will have only rotated doc, this adjusts page so [0,0] appears to come from correct corner
        g.translate((int)topAdjust, (int)leftAdjust);
    }

    /**
     * Wrapper of the wrapper class so that PrinterJob implementations will handle it as proper pageable
     */
    public Book wrapAndPresent() {
        Book cover = new Book();
        for(int i = 0; i < getNumberOfPages(); i++) {
            cover.append(this, getPageFormat(i));
        }

        return cover;
    }

}
