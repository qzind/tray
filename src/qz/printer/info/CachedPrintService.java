package qz.printer.info;

import qz.common.CachedObject;

import javax.print.*;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeListener;
import java.util.HashMap;
import java.util.function.Supplier;

/**
 * <code>PrintService.getName()</code> is slow, and gets increasingly slower the more times it's called.
 *
 * By overriding and caching the <code>PrintService</code> attributes, we're able to help suppress/delay the
 * performance loss of this bug.
 *
 * See also JDK-7001133
 */
public class CachedPrintService implements PrintService {
    private PrintService printService;
    private final long lifespan;
    private final CachedObject<String> cachedName;
    private final CachedObject<PrintServiceAttributeSet> cachedAttributeSet;
    private final HashMap<Class<?>, CachedObject<?>> cachedAttributes = new HashMap<>();

    public CachedPrintService(PrintService printService, long lifespan) {
        this.printService = printService;
        this.lifespan = lifespan;
        cachedName = new CachedObject<>(this.printService::getName, lifespan);
        cachedAttributeSet = new CachedObject<>(this.printService::getAttributes, lifespan);
    }

    public CachedPrintService(PrintService printService) {
        this(printService, CachedObject.DEFAULT_LIFESPAN);
    }

    @Override
    public String getName() {
        return cachedName.get();
    }

    @Override
    public DocPrintJob createPrintJob() {
        return printService.createPrintJob();
    }

    @Override
    public void addPrintServiceAttributeListener(PrintServiceAttributeListener listener) {
        printService.addPrintServiceAttributeListener(listener);
    }

    @Override
    public void removePrintServiceAttributeListener(PrintServiceAttributeListener listener) {
        printService.removePrintServiceAttributeListener(listener);
    }

    @Override
    public PrintServiceAttributeSet getAttributes() {
        return cachedAttributeSet.get();
    }

    @Override
    public <T extends PrintServiceAttribute> T getAttribute(Class<T> category) {
        if (!cachedAttributes.containsKey(category)) {
            Supplier<T> supplier = () -> printService.getAttribute(category);
            CachedObject<T> cachedObject = new CachedObject<>(supplier, lifespan);
            cachedAttributes.put(category, cachedObject);
        }
        return category.cast(cachedAttributes.get(category).get());
    }

    @Override
    public DocFlavor[] getSupportedDocFlavors() {
        return printService.getSupportedDocFlavors();
    }

    @Override
    public boolean isDocFlavorSupported(DocFlavor flavor) {
        return printService.isDocFlavorSupported(flavor);
    }

    @Override
    public Class<?>[] getSupportedAttributeCategories() {
        return printService.getSupportedAttributeCategories();
    }

    @Override
    public boolean isAttributeCategorySupported(Class<? extends Attribute> category) {
        return printService.isAttributeCategorySupported(category);
    }

    @Override
    public Object getDefaultAttributeValue(Class<? extends Attribute> category) {
        return printService.getDefaultAttributeValue(category);
    }

    @Override
    public Object getSupportedAttributeValues(Class<? extends Attribute> category, DocFlavor flavor, AttributeSet attributes) {
        return printService.getSupportedAttributeValues(category, flavor, attributes);
    }

    @Override
    public boolean isAttributeValueSupported(Attribute attrval, DocFlavor flavor, AttributeSet attributes) {
        return printService.isAttributeValueSupported(attrval, flavor, attributes);
    }

    @Override
    public AttributeSet getUnsupportedAttributes(DocFlavor flavor, AttributeSet attributes) {
        return printService.getUnsupportedAttributes(flavor, attributes);
    }

    @Override
    public ServiceUIFactory getServiceUIFactory() {
        return printService.getServiceUIFactory();
    }
}
