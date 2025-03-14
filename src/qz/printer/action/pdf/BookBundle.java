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

    private boolean streaming = false;
    private int streamAt = 0;

    private Printable lastPrint;
    private int lastStarted;

    public BookBundle() {
        super();
    }

    public void flagForStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    /**
     * Wrapper of the wrapper class so that PrinterJob implementations will handle it as proper pageable
     */
    public Book wrapAndPresent() {
        Book cover = new Book();
        for(int i = 0; i < getNumberOfPages(); i++) {
            cover.append(new PrintingPress(streaming), getPageFormat(i));
        }

        return cover;
    }

    public Book wrapAndPresent(int offset, int length) {
        Book coverSubset = new Book();
        for(int i = offset; i < offset + length && i < getNumberOfPages(); i++) {
            coverSubset.append(new PrintingPress(offset, streaming), getPageFormat(i));
        }

        return coverSubset;
    }

    /** Printable wrapper to ensure proper reading of multiple documents across spooling */
    private class PrintingPress implements Printable {

        private boolean isStream;
        private int pageOffset;

        public PrintingPress(boolean stream) {
            this(0, stream);
        }

        public PrintingPress(int offset, boolean stream) {
            pageOffset = offset;
            isStream = stream;
        }

        @Override
        public int print(Graphics g, PageFormat format, int pageIndex) throws PrinterException {
            pageIndex += pageOffset;
            log.trace("Requested page {} for printing", pageIndex);

            if (isStream) {
                Printable printable = getPrintable(streamAt);
                if (printable != lastPrint) {
                    lastPrint = printable;
                    lastStarted = pageIndex;
                }

                //fixme - this setup results in too many blank pages after a no_such_page
                int result = printable.print(g, format, pageIndex - lastStarted);
                if (result == NO_SUCH_PAGE) {
                    // finished the last page of this document, move to the next
                    streamAt++;
                    ((FuturePdf.FutureWrapper)printable).sendToPast();
                }
                if (streamAt < getNumberOfPages()) {
                    // always return "exists" if there are more documents to print
                    return PAGE_EXISTS;
                }

                return result;
            } else if (pageIndex < getNumberOfPages()) {
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
