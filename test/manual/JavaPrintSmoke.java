package manual;

import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

public class JavaPrintSmoke implements Printable {
    public static void main(String[] args) throws Exception {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("QZ cups-bsd smoke");
        job.setPrintable(new JavaPrintSmoke());
        job.print();
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        graphics.drawString("cups-bsd smoke test", 72, 72);
        return PAGE_EXISTS;
    }
}
