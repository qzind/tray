package qz.printer.action.raw.converter;

import org.codehaus.jettison.json.JSONObject;
import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.MonoImageConverter;

import java.io.UnsupportedEncodingException;

public class Epl extends MonoImageConverter {
    private int x;
    private int y;

    @Override
    public void setParams(JSONObject params) {
        super.setParams(params);

        x = params.optInt("x", 0);
        y = params.optInt("y", 0);
    }

    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException {
        int w = getWidth() / 8;
        int h = getHeight();

        return byteBuffer.append("GW", x, ",", y, ",", w, ",", h, ",", getBytes(), "\n");
    }

    @Override
    public String getHeader() {
        return "N\n";
    }

    @Override
    public String getFooter() {
        return "P1,1\n";
    }
}
