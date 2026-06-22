package qz.printer;

public class NoSuchPrinterException extends RuntimeException {
    public NoSuchPrinterException(String printerName) {
        super("No such printer: " + printerName);
    }
}
