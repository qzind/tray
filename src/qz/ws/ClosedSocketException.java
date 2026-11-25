package qz.ws;

import org.eclipse.jetty.util.ConstantThrowable;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;

/**
 * <code>ClosedChannelException</code> wrapper class for handling edge-cases where Jetty nests or hides
 * the underlying <code>Exception</code> cause.  Inspired by #1300 and #1414
 */
public class ClosedSocketException extends ClosedChannelException {
    public ClosedSocketException(String message) {
        super();
        this.initCause(new IOException(message));
    }

    public ClosedSocketException(Throwable t) {
        super();
        this.initCause(t);
    }

    /**
     * Rethrows anything that we believe to be related to a <code>ClosedChannelException</code>
     * Does nothing otherwise.
     */
    public static void filter(Throwable t) throws ClosedChannelException {
        Throwable throwable = t;
        while(true) {
            // Look for nested ClosedChannelException
            if(throwable instanceof ClosedChannelException) {
                throw (ClosedChannelException)throwable;
            }
            if(throwable != null) {
                throwable = throwable.getCause();
            } else {
                break;
            }
        }

        // Look for edge-cases, such as ConstantThrowable("Closed");
        if (messageMatchesClosed(t)) {
            throw new ClosedSocketException(t);
        }
    }

    /**
     * Check if a <code>Throwable</code> or its cause matches a <code>ConstantThrowable</code>
     * with "closed" in the message string.
     */
    private static boolean messageMatchesClosed(Throwable t) {
        Throwable throwable = t;
        while(true) {
            if(throwable == null) {
                return false;
            } else if(!(throwable instanceof ConstantThrowable)) {
                // go deeper
                throwable = throwable.getCause();
            } else {
                break;
            }
        }
        ConstantThrowable constantThrowable = (ConstantThrowable)throwable;
        return constantThrowable.getMessage() != null && constantThrowable.getMessage().toLowerCase(Locale.ENGLISH).contains("closed");
    }
}
