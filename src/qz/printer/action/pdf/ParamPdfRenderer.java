package qz.printer.action.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import qz.printer.rendering.OpaqueDrawObject;
import qz.printer.rendering.OpaqueGraphicStateParameters;
import qz.printer.rendering.PdfFontPageDrawer;

import java.io.IOException;

public class ParamPdfRenderer extends PDFRenderer {

    private boolean useAlternateFontRendering;
    private boolean ignoreTransparency;

    public ParamPdfRenderer(PDDocument document, boolean useAlternateFontRendering, boolean ignoreTransparency) {
        super(document);

        this.useAlternateFontRendering = useAlternateFontRendering;
        this.ignoreTransparency = ignoreTransparency;
    }

    @Override
    protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
        if (useAlternateFontRendering) {
            return new PdfFontPageDrawer(parameters, ignoreTransparency);
        } else if(ignoreTransparency) {
            return new OpaquePageDrawer(parameters);
        }
        // Fallback to default PageDrawer
        return new PageDrawer(parameters);
    }

    // override drawer to make use of customized draw object
    private static class OpaquePageDrawer extends PageDrawer {
        public OpaquePageDrawer(PageDrawerParameters parameters) throws IOException {
            super(parameters);

            // Note:  These must match PdfFontPageDrawer's ignoreTransparency condition
            addOperator(new OpaqueDrawObject());
            addOperator(new OpaqueGraphicStateParameters());
        }
    }
}

