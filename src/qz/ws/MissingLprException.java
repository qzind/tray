package qz.ws;

import qz.utils.SystemUtilities;

import javax.print.PrintException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Wrapper for the Linux Java print failure caused by a missing CUPS-compatible lpr command.
 */
public class MissingLprException extends Exception {
    private static final String MESSAGE = "The 'lpr' command appears to be missing. Please install the 'cups-bsd' package and then try again.";
    private static final String LPR_PATH = "/usr/bin/lpr";

    public MissingLprException(Exception e) {
        super(MESSAGE);
        this.initCause(e);
    }

    public static Exception wrap(Exception e) {
        if(isLprSignature(e)) {
            return new MissingLprException(e);
        }
        return e;
    }

    static boolean isLprSignature(Exception e) {
        if(!SystemUtilities.isLinux()) {
            return false;
        }
        if(e instanceof PrinterException) {
            return !isLprAvailable() && hasCauseChain(e, PrintException.class, PrinterIOException.class);
        }
        if(e instanceof IOException) {
            return isMessageSignature((IOException)e);
        }
        return false;
    }

    @SafeVarargs
    private static boolean hasCauseChain(Throwable throwable, Class<? extends Throwable>... expectedCauses) {
        Throwable current = throwable;
        for(Class<? extends Throwable> expectedClass : expectedCauses) {
            if(current == null || current.getCause() == null) {
                return false;
            }
            current = current.getCause();
            if(!expectedClass.isInstance(current)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMessageSignature(IOException e) {
        String message = e.getMessage();
        if(message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ENGLISH);
        return normalized.contains("error")
                && normalized.contains("running")
                && normalized.contains("lpr");
    }

    private static boolean isLprAvailable() {
        return Files.isExecutable(Paths.get(LPR_PATH));
    }
}
