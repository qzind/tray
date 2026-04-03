package qz.printer.action.raw;

import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;

import java.io.UnsupportedEncodingException;

public interface ByteAppender {
    ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException, InvalidRawImageException;
}
