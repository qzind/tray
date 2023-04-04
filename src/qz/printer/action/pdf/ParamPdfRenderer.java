package qz.printer.action.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import qz.printer.rendering.OpaqueDrawObject;
import qz.printer.rendering.PdfFontPageDrawer;

import java.io.IOException;

public class ParamPdfRenderer extends PDFRenderer {

    private boolean usesAltFontRenderer;
    private boolean ignoresTransparency;

    public ParamPdfRenderer(PDDocument document, boolean usesAltFontRenderer, boolean ignoresTransparency) {
        super(document);

        this.usesAltFontRenderer = usesAltFontRenderer;
        this.ignoresTransparency = ignoresTransparency;
    }

    @Override
    protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
        if (!usesAltFontRenderer) {
            return new OpaquePageDrawer(parameters);
        } else {
            return new PdfFontPageDrawer(parameters, ignoresTransparency);
        }
    }

    // override drawer to make use of customized draw object
    private static class OpaquePageDrawer extends PageDrawer {
        public OpaquePageDrawer(PageDrawerParameters parameters) throws IOException {
            super(parameters);

            addOperator(new OpaqueDrawObject());
        }
    }
}

