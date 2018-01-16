package qz.printer.action;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import qz.utils.PrintingUtilities;

public class ProcessorFactory implements KeyedPooledObjectFactory<PrintingUtilities.Type,PrintProcessor> {

    @Override
    public PooledObject<PrintProcessor> makeObject(PrintingUtilities.Type key) throws Exception {
        PrintProcessor processor;
        switch(key) {
            case HTML: processor = new PrintHTML(); break;
            case IMAGE: processor = new PrintImage(); break;
            case PDF: processor = new PrintPDF(); break;
            case DIRECT: processor = new PrintDirect(); break;
            case RAW: default: processor = new PrintRaw(); break;
        }

        return new DefaultPooledObject<>(processor);
    }

    @Override
    public boolean validateObject(PrintingUtilities.Type key, PooledObject<PrintProcessor> p) {
        return true; //no-op
    }

    @Override
    public void activateObject(PrintingUtilities.Type key, PooledObject<PrintProcessor> p) throws Exception {
        //no-op
    }

    @Override
    public void passivateObject(PrintingUtilities.Type key, PooledObject<PrintProcessor> p) throws Exception {
        p.getObject().cleanup();
    }

    @Override
    public void destroyObject(PrintingUtilities.Type key, PooledObject<PrintProcessor> p) throws Exception {
        //no-op
    }

}
