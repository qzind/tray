package qz.printer.rendering;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.MissingOperandException;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.graphics.GraphicsOperatorProcessor;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.MissingResourceException;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.util.List;

// override draw object to remove any calls to show transparency
public class OpaqueDrawObject extends GraphicsOperatorProcessor {

    public OpaqueDrawObject(PDFGraphicsStreamEngine context) {
        super(context);
    }

    public void process(Operator operator, List<COSBase> operands) throws IOException {
        if (operands.isEmpty()) {
            throw new MissingOperandException(operator, operands);
        } else {
            COSBase base0 = operands.get(0);
            if (base0 instanceof COSName) {
                COSName objectName = (COSName)base0;
                PDXObject xobject = getGraphicsContext().getResources().getXObject(objectName);

                if (xobject == null) {
                    throw new MissingResourceException("Missing XObject: " + objectName.getName());
                } else {
                    if (xobject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject)xobject;
                        getGraphicsContext().drawImage(image);
                    } else if (xobject instanceof PDFormXObject) {
                        try {
                            getGraphicsContext().increaseLevel();
                            if (getGraphicsContext().getLevel() <= 25) {
                                PDFormXObject form = (PDFormXObject)xobject;
                                getGraphicsContext().showForm(form);
                            }

                            //LOG.error("recursion is too deep, skipping form XObject");
                        }
                        finally {
                            getGraphicsContext().decreaseLevel();
                        }
                    }

                }
            }
        }
    }

    public String getName() {
        return "Do";
    }

}
