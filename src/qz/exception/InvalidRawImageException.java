package qz.exception;

public class InvalidRawImageException extends Exception {
    public InvalidRawImageException(String msg) {
        super(msg);
    }

    public InvalidRawImageException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
