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

    private Printable lastPrint;
    private int lastStarted;

    public BookBundle() {
        super();
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

            return printable.print(g, format, pageIndex - lastStarted);
        }

        return NO_SUCH_PAGE;
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
