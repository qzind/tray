package qz.printer.action.raw.converter;

import org.codehaus.jettison.json.JSONObject;
import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.MonoImageConverter;
import qz.printer.action.raw.PixelGrid;

import java.io.UnsupportedEncodingException;

public class Pgl extends MonoImageConverter {
    private String logoId; // logo name (mandatory)
    private boolean igpDots; // toggle IGP/PGL default resolution of 72dpi

    @Override
    public void setParams(JSONObject params) {
        super.setParams(params);

        logoId = params.optString("logoId");
        igpDots = params.optBoolean("igpDots", false);
    }

    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException, InvalidRawImageException {
        int w = getWidth();
        int h = getHeight();
        PixelGrid pixelGrid = getImageAsPixelGrid();

        if(logoId == null || logoId.trim().isEmpty()) {
            throw new InvalidRawImageException("Printronix graphics require a logoId");
        }
        if(igpDots) {
            // IGP images cannot exceed 240x252
            if(w > 240 || h > 252) {
                throw new InvalidRawImageException("IGP dots is enabled; Size values HL/VL cannot exceed 240x252");
            }
        }

        // igpDots: true: Use IGP standard 60dpi/72dpi graphics (removes "DOTS" from raw command)
        // igpDots: false: Use the printer's native resolution (appends "DOTS" to raw command)
        byteBuffer.append("~LOGO", ";", logoId, ";", h, ";", w, ";", igpDots ? "" : "DOT", "\n");

        /*
         * Printronix format
         *    [line];[black dots range];[more black dots range][newline]
         * e.g
         *    1;1-12;19-22;38-39
         */
        int bitSetIndex = 0;
        for(int lineNum = 1; lineNum <= h; lineNum++) {
            StringBuilder line = new StringBuilder();

            int start = -1;
            int end = -1;

            for(int pixelNum = 1; pixelNum <= w; pixelNum++) {
                if (pixelGrid.get(bitSetIndex)) {
                    if (start == -1) {
                        start = pixelNum;
                    }
                } else {
                    if (start != -1) {
                        end = pixelNum - 1;
                    }
                }

                // Handle trailing pixel
                if (pixelNum == w) {
                    end = pixelNum;
                }

                if (start != -1 && end != -1) {
                    if (start == end) {
                        // append a single dot
                        line.append(start).append(";");
                    } else {
                        // append a range of dots
                        line.append(start).append("-").append(end).append(";");
                    }
                    start = -1;
                    end = -1;
                }
                bitSetIndex++;
            }
            if (line.length() > 0) {
                // Remove trailing ";"
                if (line.charAt(line.length() - 1) == ';') {
                    line.replace(line.length() - 1, line.length(), "");
                }
                // Add line number
                line.insert(0, lineNum + ";");

                // Add to final commands
                byteBuffer.append(line).append("\n");
            }
        }
        return byteBuffer.append("END", "\n");
    }

    @Override
    public String getHeader() {
        return "";
    }

    @Override
    public String getFooter() {
        return "~CREATE;FORM-1;432\n" +     // 432 = Form Length in IGP dot rows
                "LOGO\n" +
                "1;1;" + logoId + "\n" +    // Position of the logo on the form y=1, x=1
                "STOP\n" +
                "END\n" +
                // "~PAPER;CUT 0;PAUSE 0;TEAR 0\n",
                "~EXECUTE;FORM-1;1\n" +
                "~NORMAL\n" +
                "~DELETE FORM;FORM-1\n" +
                "~DELETE LOGO;" + logoId + "\n";
    }
}
