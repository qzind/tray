package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONObject;
import qz.exception.InvalidRawImageException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface ImageConverter {
    byte[] getImageCommand(JSONObject opt) throws InvalidRawImageException, IOException;
    ImageConverterType getImageType();
}
