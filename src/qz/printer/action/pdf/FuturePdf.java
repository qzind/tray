package qz.printer.action.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.Scaling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.PrintOptions;
import qz.utils.ConnectionUtilities;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.IOException;

public class FuturePdf extends PDDocument {

    private String futureDocument;
    private FutureWrapper futureWrapper;

    public FuturePdf(String futureDoc) {
        super();

        this.futureDocument = futureDoc;
    }

    public void buildFutureWrapper(PdfParams pdfParams, RenderingHints hints) {
        futureWrapper = new FutureWrapper(this.futureDocument, pdfParams, hints);
    }

    public FutureWrapper getFutureWrapper() {
        return futureWrapper;
    }


    static class FutureWrapper implements Printable {

        private static final Logger log = LoggerFactory.getLogger(FutureWrapper.class);

        private String futureDocument;
        private PdfParams pdfParams;
        private RenderingHints hints;

        private PDDocument presentDocument;
        private PDFWrapper realWrapper;


        FutureWrapper(String futureDoc, PdfParams pdfParams, RenderingHints hints) {
            this.futureDocument = futureDoc;
            this.pdfParams = pdfParams;
            this.hints = hints;
        }

        public void bringToPresent() throws IOException {
            if (presentDocument == null) {
                log.trace("Loading document for use");
                //TODO - include various processing handled for non streamed documents ??
                presentDocument = Loader.loadPDF(new RandomAccessReadBuffer(ConnectionUtilities.protocolRestricted(this.futureDocument).openStream()));
                realWrapper = new PDFWrapper(presentDocument,false, pdfParams, false, hints);
            }
        }

        public void sendToPast() {
            try {
                log.trace("Unloading document after completion");
                presentDocument.close();
            }
            catch(IOException ioe) {
                log.error("Unable to unload streamed pdf document", ioe);
            }
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            try {
                bringToPresent();
                return realWrapper.print(graphics, pageFormat, pageIndex);
            }
            catch(IOException ioe) {
                //todo - how to handle??
                throw new PrinterException(ioe.getMessage());
            }
        }

    }

}
