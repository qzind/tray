package qz.utils;

import com.ibm.icu.charset.CharsetEncoderICU;
import com.ibm.icu.charset.CharsetProviderICU;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

/**
 * Created by Yohanes Nugroho on 7/10/2018.
 */
public class ArabicConversionUtilities {

    /**
     * This is the simplest and most reliable method:
     * If all characters on input string does not contain any Arabic letters then return it as it is,
     * otherwise do special Arabic text conversion
     * <p>
     * To send data to printer, we need to split the commands from the text, eg:<br/>
     * {@code var data = ['\x1b\x41\x42', "Arabic text to print", '\x1b\x42x53', "Other texts"]}
     *
     * @param escp_or_text a String that contains only ESC/P code or only text
     * @return encoded bytes
     */
    public static byte[] convertToIBM864(String escp_or_text) throws CharacterCodingException, ArabicShapingException {
        boolean allAscii = true;
        for(int i = 0; i < escp_or_text.length(); i++) {
            //https://wiki.sei.cmu.edu/confluence/display/java/STR01-J.+Do+not+assume+that+a+Java+char+fully+represents+a+Unicode+code+point
            int ch = escp_or_text.codePointAt(i);
            if (ch > 255) {
                allAscii = false;
            }
        }

        if (allAscii) {
            //we use 'ISO-8859-1' that will map bytes as it is
            return escp_or_text.getBytes(StandardCharsets.ISO_8859_1);
        } else {
            //Layout the characters from logical order to visual ordering
            Bidi para = new Bidi();
            para.setPara(escp_or_text, Bidi.LEVEL_DEFAULT_LTR, null);
            String data = para.writeReordered(Bidi.DO_MIRRORING);
            return convertVisualOrderedToIBM864(data);
        }
    }

    /**
     * Shape a visual ordered Arabic string and then encode it in IBM864 encoding
     *
     * @param str input string
     * @return encoded bytes
     */
    private static byte[] convertVisualOrderedToIBM864(String str) throws ArabicShapingException, CharacterCodingException {
        //We shape the characters to map it to Unicode in FExx range
        //Note that the output of Bidi is VISUAL_LTR, so we need the flag: ArabicShaping.TEXT_DIRECTION_VISUAL_LTR)
        ArabicShaping as = new ArabicShaping(ArabicShaping.LETTERS_SHAPE | ArabicShaping.TEXT_DIRECTION_VISUAL_LTR | ArabicShaping.LENGTH_GROW_SHRINK);
        String shaped = as.shape(str);

        //then we need to convert it to IBM864 using ICU Encoder
        CharsetProviderICU icu = new CharsetProviderICU();
        Charset cs = icu.charsetForName("IBM864");
        CharsetEncoderICU icuc = (CharsetEncoderICU)cs.newEncoder();

        //We need to use fallback for some character forms that can not be found
        icuc.setFallbackUsed(true);
        ByteBuffer output = ByteBuffer.allocate(shaped.length() * 2);
        CharBuffer inp = CharBuffer.wrap(shaped);
        CoderResult res = icuc.encode(inp, output, true);
        if (res.isError()) {
            res.throwException();
        }

        int length = output.position();
        byte all[] = output.array();

        byte out[] = new byte[length];
        System.arraycopy(all, 0, out, 0, length);

        return out;
    }

}
