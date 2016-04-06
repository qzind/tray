package qz.printer.action;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.iharder.Base64;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PrintPDF extends PrintPixel implements PrintProcessor {

    private static final Logger log = LoggerFactory.getLogger(PrintPDF.class);

    private List<PDDocument> pdfs;


    public PrintPDF() {
        pdfs = new ArrayList<>();
    }


    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.getJSONObject(i);

            PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", "FILE").toUpperCase());

            try {
                PDDocument doc;
                if (format == PrintingUtilities.Format.BASE64) {
                    doc = PDDocument.load(new ByteArrayInputStream(Base64.decode(data.getString("data"))));
                } else {
                    doc = PDDocument.load(new URL(data.getString("data")).openStream());
                }

                pdfs.add(doc);
            }
            catch(FileNotFoundException e) {
                throw new UnsupportedOperationException("PDF file specified could not be found.", e);
            }
            catch(IOException e) {
                throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as a PDF file", format, data.getString("data")), e);
            }
        }

        log.debug("Parsed {} files for printing", pdfs.size());
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrinterException {
        if (pdfs.isEmpty()) {
            log.warn("Nothing to print");
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(output.getPrintService());
        PageFormat page = job.getPageFormat(null);

        PrintOptions.Pixel pxlOpts = options.getPixelOptions();
        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, page);

        Scaling scale = (pxlOpts.isScaleContent()? Scaling.SCALE_TO_FIT:Scaling.ACTUAL_SIZE);

        Book book = new Book();
        for(PDDocument doc : pdfs) {
            book.append(new PDFPrintable(doc, scale, false, (float)(pxlOpts.getDensity() * pxlOpts.getUnits().as1Inch()), false), page, doc.getNumberOfPages());
        }

        job.setJobName(pxlOpts.getJobName(Constants.PDF_PRINT));
        job.setPageable(book);

        printCopies(output, pxlOpts, job, attributes);

        for(PDDocument doc : pdfs) {
            try { doc.close(); } catch(IOException ignore) {}
        }
    }

}
