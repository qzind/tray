package qz.printer;

import java.awt.*;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

/**
 * Wrapper of the {@code Book} class as a {@code Printable} type,
 * since PrinterJob implementations do not seem to handle the {@code Pageable} interface properly.
 */
public class BookBundle extends Book implements Printable {

    private Printable lastPrint;
    private int lastStarted;

    public BookBundle() {
        super();
    }

    @Override
    public int print(Graphics g, PageFormat format, int page) throws PrinterException {
        if (page < getNumberOfPages()) {
            Printable printable = getPrintable(page);
            if (printable != lastPrint) {
                lastPrint = printable;
                lastStarted = page;
            }

            return printable.print(g, format, page - lastStarted);
        }

        return NO_SUCH_PAGE;
    }

}
