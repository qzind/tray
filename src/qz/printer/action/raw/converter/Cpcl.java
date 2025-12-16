package qz.printer.action.raw.converter;

import org.codehaus.jettison.json.JSONObject;
import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.MonoImageConverter;

import java.io.UnsupportedEncodingException;

public class Cpcl extends MonoImageConverter {
    private int x;
    private int y;

    public void setParams(JSONObject params) {
        super.setParams(params);

        x = params.optInt("x", 0);
        y = params.optInt("y", 0);
    }

    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException {
        int w = getWidth() / 8;
        int h = getHeight();

        return byteBuffer.append("EG", " ", w, " ", h, " ", x, " ", y, " ", convertImageToHexString(), "\r\n");
    }


    @Override
    public String getHeader() {
        // ! [Offset] [Horizontal Res.] [Vertical Res.] [Height] [Quantity]<CR><LF>
        return "! 0 200 200 203 1\r\n";
    }

    @Override
    public String getFooter() {
        return "PRINT\r\n";
    }
}
