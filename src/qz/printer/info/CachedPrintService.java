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

public class CachedPrintService implements PrintService {
    public PrintService innerPrintService;
    // PrintService.getName() is slow, use a cache instead per JDK-7001133
    // TODO: Remove this comment when upstream bug report is filed
    private static final long lifespan = CachedObject.DEFAULT_LIFESPAN;
    private static final CachedObject<PrintService> cachedDefault = new CachedObject<>(CachedPrintService::innerLookupDefaultPrintService, lifespan);
    private static final CachedObject<PrintService[]> cachedPrintServices = new CachedObject<>(CachedPrintService::innerLookupPrintServices, lifespan);
    private final CachedObject<String> cachedName;
    private final CachedObject<PrintServiceAttributeSet> cachedAttributeSet;
    private final HashMap<Class<?>, CachedObject<?>> cachedAttributes = new HashMap<>();

    public static PrintService lookupDefaultPrintService() {
        return cachedDefault.get();
    }

    public static PrintService[] lookupPrintServices() {
        return cachedPrintServices.get();
    }

    private static PrintService innerLookupDefaultPrintService() {
        return new CachedPrintService(PrintServiceLookup.lookupDefaultPrintService());
    }

    private static PrintService[] innerLookupPrintServices() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        for (int i = 0; i < printServices.length; i++) {
            printServices[i] = new CachedPrintService(printServices[i]);
        }
        return printServices;
    }

    public CachedPrintService(PrintService printService) {
        innerPrintService = printService;
        cachedName = new CachedObject<>(innerPrintService::getName, lifespan);
        cachedAttributeSet = new CachedObject<>(innerPrintService::getAttributes, lifespan);
    }

    @Override
    public String getName() {
        return cachedName.get();
    }

    @Override
    public DocPrintJob createPrintJob() {
        return innerPrintService.createPrintJob();
    }

    @Override
    public void addPrintServiceAttributeListener(PrintServiceAttributeListener listener) {
        innerPrintService.addPrintServiceAttributeListener(listener);
    }

    @Override
    public void removePrintServiceAttributeListener(PrintServiceAttributeListener listener) {
        innerPrintService.removePrintServiceAttributeListener(listener);
    }

    @Override
    public PrintServiceAttributeSet getAttributes() {
        return cachedAttributeSet.get();
    }

    @Override
    public <T extends PrintServiceAttribute> T getAttribute(Class<T> category) {
        if (!cachedAttributes.containsKey(category)) {
            Supplier<T> supplier = () -> innerPrintService.getAttribute(category);
            CachedObject<T> cachedObject = new CachedObject<>(supplier, lifespan);
            cachedAttributes.put(category, cachedObject);
        }
        return category.cast(cachedAttributes.get(category).get());
    }

    @Override
    public DocFlavor[] getSupportedDocFlavors() {
        return innerPrintService.getSupportedDocFlavors();
    }

    @Override
    public boolean isDocFlavorSupported(DocFlavor flavor) {
        return innerPrintService.isDocFlavorSupported(flavor);
    }

    @Override
    public Class<?>[] getSupportedAttributeCategories() {
        return innerPrintService.getSupportedAttributeCategories();
    }

    @Override
    public boolean isAttributeCategorySupported(Class<? extends Attribute> category) {
        return innerPrintService.isAttributeCategorySupported(category);
    }

    @Override
    public Object getDefaultAttributeValue(Class<? extends Attribute> category) {
        return innerPrintService.getDefaultAttributeValue(category);
    }

    @Override
    public Object getSupportedAttributeValues(Class<? extends Attribute> category, DocFlavor flavor, AttributeSet attributes) {
        return innerPrintService.getSupportedAttributeValues(category, flavor, attributes);
    }

    @Override
    public boolean isAttributeValueSupported(Attribute attrval, DocFlavor flavor, AttributeSet attributes) {
        return innerPrintService.isAttributeValueSupported(attrval, flavor, attributes);
    }

    @Override
    public AttributeSet getUnsupportedAttributes(DocFlavor flavor, AttributeSet attributes) {
        return innerPrintService.getUnsupportedAttributes(flavor, attributes);
    }

    @Override
    public ServiceUIFactory getServiceUIFactory() {
        return innerPrintService.getServiceUIFactory();
    }
}
