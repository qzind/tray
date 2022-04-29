package qz.printer.action.pdf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

/**
 * Wrapper of the {@code Book} class as a {@code Printable} type,
 * since PrinterJob implementations do not seem to handle the {@code Pageable} interface properly.
 */
public class BookBundle extends Book {

    private static final Logger log = LogManager.getLogger(BookBundle.class);

    private Printable lastPrint;
    private int lastStarted;

    public BookBundle() {
        super();
    }

    /**
     * Wrapper of the wrapper class so that PrinterJob implementations will handle it as proper pageable
     */
    public Book wrapAndPresent() {
        Book cover = new Book();
        for(int i = 0; i < getNumberOfPages(); i++) {
            cover.append(new PrintingPress(), getPageFormat(i));
        }

        return cover;
    }

    public Book wrapAndPresent(int offset, int length) {
        Book coverSubset = new Book();
        for(int i = offset; i < offset + length && i < getNumberOfPages(); i++) {
            coverSubset.append(new PrintingPress(offset), getPageFormat(i));
        }

        return coverSubset;
    }


    /** Printable wrapper to ensure proper reading of multiple documents across spooling */
    private class PrintingPress implements Printable {
        private int pageOffset;

        public PrintingPress() {
            this(0);
        }

        public PrintingPress(int offset) {
            pageOffset = offset;
        }

        @Override
        public int print(Graphics g, PageFormat format, int pageIndex) throws PrinterException {
            pageIndex += pageOffset;
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
    }

}
