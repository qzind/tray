package qz.utils;

import com.ibm.icu.charset.CharsetEncoderICU;
import com.ibm.icu.charset.CharsetProviderICU;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;

/**
 * Created by Yohanes Nugroho on 7/10/2018.
 */
public class ArabicConversionUtilities {

    /**
     * This is only used for debugging problems in IBM864 generated output
     */
    private final static String [] IBM846_CHARACTER_NAMES = {
            "NULL (U+0000)",
            "START OF HEADING (U+0001)",
            "START OF TEXT (U+0002)",
            "END OF TEXT (U+0003)",
            "END OF TRANSMISSION (U+0004)",
            "ENQUIRY (U+0005)",
            "ACKNOWLEDGE (U+0006)",
            "BELL (U+0007)",
            "BACKSPACE (U+0008)",
            "CHARACTER TABULATION (U+0009)",
            "LINE FEED (LF) (U+000A)",
            "LINE TABULATION (U+000B)",
            "FORM FEED (FF) (U+000C)",
            "CARRIAGE RETURN (CR) (U+000D)",
            "SHIFT OUT (U+000E)",
            "SHIFT IN (U+000F)",
            "DATA LINK ESCAPE (U+0010)",
            "DEVICE CONTROL ONE (U+0011)",
            "DEVICE CONTROL TWO (U+0012)",
            "DEVICE CONTROL THREE (U+0013)",
            "DEVICE CONTROL FOUR (U+0014)",
            "NEGATIVE ACKNOWLEDGE (U+0015)",
            "SYNCHRONOUS IDLE (U+0016)",
            "END OF TRANSMISSION BLOCK (U+0017)",
            "CANCEL (U+0018)",
            "END OF MEDIUM (U+0019)",
            "SUBSTITUTE (U+001A)",
            "ESCAPE (U+001B)",
            "INFORMATION SEPARATOR FOUR (U+001C)",
            "INFORMATION SEPARATOR THREE (U+001D)",
            "INFORMATION SEPARATOR TWO (U+001E)",
            "INFORMATION SEPARATOR ONE (U+001F)",
            "SPACE (U+0020)",
            "EXCLAMATION MARK (U+0021)",
            "QUOTATION MARK (U+0022)",
            "NUMBER SIGN (U+0023)",
            "DOLLAR SIGN (U+0024)",
            "ARABIC PERCENT SIGN (U+066A)",
            "AMPERSAND (U+0026)",
            "APOSTROPHE (U+0027)",
            "LEFT PARENTHESIS (U+0028)",
            "RIGHT PARENTHESIS (U+0029)",
            "ASTERISK (U+002A)",
            "PLUS SIGN (U+002B)",
            "COMMA (U+002C)",
            "HYPHEN-MINUS (U+002D)",
            "FULL STOP (U+002E)",
            "SOLIDUS (U+002F)",
            "DIGIT ZERO (U+0030)",
            "DIGIT ONE (U+0031)",
            "DIGIT TWO (U+0032)",
            "DIGIT THREE (U+0033)",
            "DIGIT FOUR (U+0034)",
            "DIGIT FIVE (U+0035)",
            "DIGIT SIX (U+0036)",
            "DIGIT SEVEN (U+0037)",
            "DIGIT EIGHT (U+0038)",
            "DIGIT NINE (U+0039)",
            "COLON (U+003A)",
            "SEMICOLON (U+003B)",
            "LESS-THAN SIGN (U+003C)",
            "EQUALS SIGN (U+003D)",
            "GREATER-THAN SIGN (U+003E)",
            "QUESTION MARK (U+003F)",
            "COMMERCIAL AT (U+0040)",
            "LATIN CAPITAL LETTER A (U+0041)",
            "LATIN CAPITAL LETTER B (U+0042)",
            "LATIN CAPITAL LETTER C (U+0043)",
            "LATIN CAPITAL LETTER D (U+0044)",
            "LATIN CAPITAL LETTER E (U+0045)",
            "LATIN CAPITAL LETTER F (U+0046)",
            "LATIN CAPITAL LETTER G (U+0047)",
            "LATIN CAPITAL LETTER H (U+0048)",
            "LATIN CAPITAL LETTER I (U+0049)",
            "LATIN CAPITAL LETTER J (U+004A)",
            "LATIN CAPITAL LETTER K (U+004B)",
            "LATIN CAPITAL LETTER L (U+004C)",
            "LATIN CAPITAL LETTER M (U+004D)",
            "LATIN CAPITAL LETTER N (U+004E)",
            "LATIN CAPITAL LETTER O (U+004F)",
            "LATIN CAPITAL LETTER P (U+0050)",
            "LATIN CAPITAL LETTER Q (U+0051)",
            "LATIN CAPITAL LETTER R (U+0052)",
            "LATIN CAPITAL LETTER S (U+0053)",
            "LATIN CAPITAL LETTER T (U+0054)",
            "LATIN CAPITAL LETTER U (U+0055)",
            "LATIN CAPITAL LETTER V (U+0056)",
            "LATIN CAPITAL LETTER W (U+0057)",
            "LATIN CAPITAL LETTER X (U+0058)",
            "LATIN CAPITAL LETTER Y (U+0059)",
            "LATIN CAPITAL LETTER Z (U+005A)",
            "LEFT SQUARE BRACKET (U+005B)",
            "REVERSE SOLIDUS (U+005C)",
            "RIGHT SQUARE BRACKET (U+005D)",
            "CIRCUMFLEX ACCENT (U+005E)",
            "LOW LINE (U+005F)",
            "GRAVE ACCENT (U+0060)",
            "LATIN SMALL LETTER A (U+0061)",
            "LATIN SMALL LETTER B (U+0062)",
            "LATIN SMALL LETTER C (U+0063)",
            "LATIN SMALL LETTER D (U+0064)",
            "LATIN SMALL LETTER E (U+0065)",
            "LATIN SMALL LETTER F (U+0066)",
            "LATIN SMALL LETTER G (U+0067)",
            "LATIN SMALL LETTER H (U+0068)",
            "LATIN SMALL LETTER I (U+0069)",
            "LATIN SMALL LETTER J (U+006A)",
            "LATIN SMALL LETTER K (U+006B)",
            "LATIN SMALL LETTER L (U+006C)",
            "LATIN SMALL LETTER M (U+006D)",
            "LATIN SMALL LETTER N (U+006E)",
            "LATIN SMALL LETTER O (U+006F)",
            "LATIN SMALL LETTER P (U+0070)",
            "LATIN SMALL LETTER Q (U+0071)",
            "LATIN SMALL LETTER R (U+0072)",
            "LATIN SMALL LETTER S (U+0073)",
            "LATIN SMALL LETTER T (U+0074)",
            "LATIN SMALL LETTER U (U+0075)",
            "LATIN SMALL LETTER V (U+0076)",
            "LATIN SMALL LETTER W (U+0077)",
            "LATIN SMALL LETTER X (U+0078)",
            "LATIN SMALL LETTER Y (U+0079)",
            "LATIN SMALL LETTER Z (U+007A)",
            "LEFT CURLY BRACKET (U+007B)",
            "VERTICAL LINE (U+007C)",
            "RIGHT CURLY BRACKET (U+007D)",
            "TILDE (U+007E)",
            "DELETE (U+007F)",
            "DEGREE SIGN (U+00B0)",
            "MIDDLE DOT (U+00B7)",
            "BULLET OPERATOR (U+2219)",
            "SQUARE ROOT (U+221A)",
            "MEDIUM SHADE (U+2592)",
            "BOX DRAWINGS LIGHT HORIZONTAL (U+2500)",
            "BOX DRAWINGS LIGHT VERTICAL (U+2502)",
            "BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL (U+253C)",
            "BOX DRAWINGS LIGHT VERTICAL AND LEFT (U+2524)",
            "BOX DRAWINGS LIGHT DOWN AND HORIZONTAL (U+252C)",
            "BOX DRAWINGS LIGHT VERTICAL AND RIGHT (U+251C)",
            "BOX DRAWINGS LIGHT UP AND HORIZONTAL (U+2534)",
            "BOX DRAWINGS LIGHT DOWN AND LEFT (U+2510)",
            "BOX DRAWINGS LIGHT DOWN AND RIGHT (U+250C)",
            "BOX DRAWINGS LIGHT UP AND RIGHT (U+2514)",
            "BOX DRAWINGS LIGHT UP AND LEFT (U+2518)",
            "GREEK SMALL LETTER BETA (U+03B2)",
            "INFINITY (U+221E)",
            "GREEK SMALL LETTER PHI (U+03C6)",
            "PLUS-MINUS SIGN (U+00B1)",
            "VULGAR FRACTION ONE HALF (U+00BD)",
            "VULGAR FRACTION ONE QUARTER (U+00BC)",
            "ALMOST EQUAL TO (U+2248)",
            "LEFT-POINTING DOUBLE ANGLE QUOTATION MARK (U+00AB)",
            "RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK (U+00BB)",
            "ARABIC LIGATURE LAM WITH ALEF WITH HAMZA ABOVE ISOLATED FORM (U+FEF7)",
            "ARABIC LIGATURE LAM WITH ALEF WITH HAMZA ABOVE FINAL FORM (U+FEF8)",
            "CHAR 0x9b (155)",
            "CHAR 0x9c (156)",
            "ARABIC LIGATURE LAM WITH ALEF ISOLATED FORM (U+FEFB)",
            "ARABIC LIGATURE LAM WITH ALEF FINAL FORM (U+FEFC)",
            "CHAR 0x9f (159)",
            "NO-BREAK SPACE (U+00A0)",
            "SOFT HYPHEN (U+00AD)",
            "ARABIC LETTER ALEF WITH MADDA ABOVE FINAL FORM (U+FE82)",
            "POUND SIGN (U+00A3)",
            "CURRENCY SIGN (U+00A4)",
            "ARABIC LETTER ALEF WITH HAMZA ABOVE FINAL FORM (U+FE84)",
            "CHAR 0xa6 (166)",
            "CHAR 0xa7 (167)",
            "ARABIC LETTER ALEF FINAL FORM (U+FE8E)",
            "ARABIC LETTER BEH ISOLATED FORM (U+FE8F)",
            "ARABIC LETTER TEH ISOLATED FORM (U+FE95)",
            "ARABIC LETTER THEH ISOLATED FORM (U+FE99)",
            "ARABIC COMMA (U+060C)",
            "ARABIC LETTER JEEM ISOLATED FORM (U+FE9D)",
            "ARABIC LETTER HAH ISOLATED FORM (U+FEA1)",
            "ARABIC LETTER KHAH ISOLATED FORM (U+FEA5)",
            "ARABIC-INDIC DIGIT ZERO (U+0660)",
            "ARABIC-INDIC DIGIT ONE (U+0661)",
            "ARABIC-INDIC DIGIT TWO (U+0662)",
            "ARABIC-INDIC DIGIT THREE (U+0663)",
            "ARABIC-INDIC DIGIT FOUR (U+0664)",
            "ARABIC-INDIC DIGIT FIVE (U+0665)",
            "ARABIC-INDIC DIGIT SIX (U+0666)",
            "ARABIC-INDIC DIGIT SEVEN (U+0667)",
            "ARABIC-INDIC DIGIT EIGHT (U+0668)",
            "ARABIC-INDIC DIGIT NINE (U+0669)",
            "ARABIC LETTER FEH ISOLATED FORM (U+FED1)",
            "ARABIC SEMICOLON (U+061B)",
            "ARABIC LETTER SEEN ISOLATED FORM (U+FEB1)",
            "ARABIC LETTER SHEEN ISOLATED FORM (U+FEB5)",
            "ARABIC LETTER SAD ISOLATED FORM (U+FEB9)",
            "ARABIC QUESTION MARK (U+061F)",
            "CENT SIGN (U+00A2)",
            "ARABIC LETTER HAMZA ISOLATED FORM (U+FE80)",
            "ARABIC LETTER ALEF WITH MADDA ABOVE ISOLATED FORM (U+FE81)",
            "ARABIC LETTER ALEF WITH HAMZA ABOVE ISOLATED FORM (U+FE83)",
            "ARABIC LETTER WAW WITH HAMZA ABOVE ISOLATED FORM (U+FE85)",
            "ARABIC LETTER AIN FINAL FORM (U+FECA)",
            "ARABIC LETTER YEH WITH HAMZA ABOVE INITIAL FORM (U+FE8B)",
            "ARABIC LETTER ALEF ISOLATED FORM (U+FE8D)",
            "ARABIC LETTER BEH INITIAL FORM (U+FE91)",
            "ARABIC LETTER TEH MARBUTA ISOLATED FORM (U+FE93)",
            "ARABIC LETTER TEH INITIAL FORM (U+FE97)",
            "ARABIC LETTER THEH INITIAL FORM (U+FE9B)",
            "ARABIC LETTER JEEM INITIAL FORM (U+FE9F)",
            "ARABIC LETTER HAH INITIAL FORM (U+FEA3)",
            "ARABIC LETTER KHAH INITIAL FORM (U+FEA7)",
            "ARABIC LETTER DAL ISOLATED FORM (U+FEA9)",
            "ARABIC LETTER THAL ISOLATED FORM (U+FEAB)",
            "ARABIC LETTER REH ISOLATED FORM (U+FEAD)",
            "ARABIC LETTER ZAIN ISOLATED FORM (U+FEAF)",
            "ARABIC LETTER SEEN INITIAL FORM (U+FEB3)",
            "ARABIC LETTER SHEEN INITIAL FORM (U+FEB7)",
            "ARABIC LETTER SAD INITIAL FORM (U+FEBB)",
            "ARABIC LETTER DAD INITIAL FORM (U+FEBF)",
            "ARABIC LETTER TAH ISOLATED FORM (U+FEC1)",
            "ARABIC LETTER ZAH ISOLATED FORM (U+FEC5)",
            "ARABIC LETTER AIN INITIAL FORM (U+FECB)",
            "ARABIC LETTER GHAIN INITIAL FORM (U+FECF)",
            "BROKEN BAR (U+00A6)",
            "NOT SIGN (U+00AC)",
            "DIVISION SIGN (U+00F7)",
            "MULTIPLICATION SIGN (U+00D7)",
            "ARABIC LETTER AIN ISOLATED FORM (U+FEC9)",
            "ARABIC TATWEEL (U+0640)",
            "ARABIC LETTER FEH INITIAL FORM (U+FED3)",
            "ARABIC LETTER QAF INITIAL FORM (U+FED7)",
            "ARABIC LETTER KAF INITIAL FORM (U+FEDB)",
            "ARABIC LETTER LAM INITIAL FORM (U+FEDF)",
            "ARABIC LETTER MEEM INITIAL FORM (U+FEE3)",
            "ARABIC LETTER NOON INITIAL FORM (U+FEE7)",
            "ARABIC LETTER HEH INITIAL FORM (U+FEEB)",
            "ARABIC LETTER WAW ISOLATED FORM (U+FEED)",
            "ARABIC LETTER ALEF MAKSURA ISOLATED FORM (U+FEEF)",
            "ARABIC LETTER YEH INITIAL FORM (U+FEF3)",
            "ARABIC LETTER DAD ISOLATED FORM (U+FEBD)",
            "ARABIC LETTER AIN MEDIAL FORM (U+FECC)",
            "ARABIC LETTER GHAIN FINAL FORM (U+FECE)",
            "ARABIC LETTER GHAIN ISOLATED FORM (U+FECD)",
            "ARABIC LETTER MEEM ISOLATED FORM (U+FEE1)",
            "ARABIC SHADDA MEDIAL FORM (U+FE7D)",
            "ARABIC SHADDA (U+0651)",
            "ARABIC LETTER NOON ISOLATED FORM (U+FEE5)",
            "ARABIC LETTER HEH ISOLATED FORM (U+FEE9)",
            "ARABIC LETTER HEH MEDIAL FORM (U+FEEC)",
            "ARABIC LETTER ALEF MAKSURA FINAL FORM (U+FEF0)",
            "ARABIC LETTER YEH FINAL FORM (U+FEF2)",
            "ARABIC LETTER GHAIN MEDIAL FORM (U+FED0)",
            "ARABIC LETTER QAF ISOLATED FORM (U+FED5)",
            "ARABIC LIGATURE LAM WITH ALEF WITH MADDA ABOVE ISOLATED FORM (U+FEF5)",
            "ARABIC LIGATURE LAM WITH ALEF WITH MADDA ABOVE FINAL FORM (U+FEF6)",
            "ARABIC LETTER LAM ISOLATED FORM (U+FEDD)",
            "ARABIC LETTER KAF ISOLATED FORM (U+FED9)",
            "ARABIC LETTER YEH ISOLATED FORM (U+FEF1)",
            "BLACK SQUARE (U+25A0)",
            "NO-BREAK SPACE (0xA0)"
    };


    /**
     * encode UTF8 string to IBM864 using ICU Encode (not Java default encoder)
     * because the ICU encoder can use fallback conversion.
     * We assume that the input String is already shaped and ordered correctly
     * @param shaped input string
     * @return encoded bytes
     * @throws Exception
     */
    private static byte[] encodeUTF8ToIBM864UsingICU(String shaped) throws CharacterCodingException {
        //then we need to convert it to IBM864 using ICU Encoder
        CharsetProviderICU icu = new CharsetProviderICU();
        Charset cs = icu.charsetForName("IBM864");
        CharsetEncoderICU icuc = (CharsetEncoderICU )cs.newEncoder();

        //We need to use fallback for some character forms that can not be found
        icuc.setFallbackUsed(true);
        ByteBuffer output = ByteBuffer.allocate(shaped.length()*2);
        CharBuffer inp = CharBuffer.wrap(shaped);
        CoderResult res = icuc.encode(inp, output, true);
        if (res.isError()) {
            res.throwException();

        }

        int length = output.position();
        byte all [] = output.array();

        byte out [] = new byte[length];
        System.arraycopy(all, 0, out, 0, length);

        return out;
    }

    /**
     * Shape a visual ordered Arabic string and then encode it in IBM864 encoding
     * @param str input string
     * @return encoded bytes
     * @throws ArabicShapingException
     * @throws CharacterCodingException
     */
    private static byte[] convertVisualOrderedUTF8ToIBM864(String str) throws ArabicShapingException, CharacterCodingException {
        //We shape the characters to map it to Unicode in FExx range
        //Note that the output of Bidi is VISUAL_LTR,
        //so we need the flag: ArabicShaping.TEXT_DIRECTION_VISUAL_LTR)
        ArabicShaping as = new ArabicShaping(ArabicShaping.LETTERS_SHAPE|
                                                     ArabicShaping.TEXT_DIRECTION_VISUAL_LTR|ArabicShaping.LENGTH_GROW_SHRINK);

        String shaped = null;
        shaped = as.shape(str);

        return encodeUTF8ToIBM864UsingICU(shaped);
    }

    /**
     * This will only encode Arabic mixed with English String to bytes
     * It will fail if the text contain some combinations of ESC/P codes
     * for example if the code contains character 0xff which is not mappable to IBM864
     * @param str input string
     * @return encoded bytes
     * @throws CharacterCodingException
     * @throws ArabicShapingException
     */
    public static byte[] convertUTF8ToIBM864(String str) throws  CharacterCodingException, ArabicShapingException{

        //Layout the characters from logical order to visual ordering
        Bidi para = new Bidi();
        para.setPara(str,  Bidi.LEVEL_DEFAULT_LTR, null);
        String data = para.writeReordered(Bidi.DO_MIRRORING);
        return convertVisualOrderedUTF8ToIBM864(data);
    }

    /**
     * This is the simplest and most reliable method:
     * if all characters on input string does not contain any Arabic letters, then return it as it is,
     * otherwise do special Arabic text conversion
     * To send data to printer, we need to split the commands from the text, eg:
     *  var data = ['\x1b\x41\x42', "Arabic text to print", '\x1b\x42x53', "Other texts"]
     *
     * @param escp_or_text a String that contains only ESC/P code or only text
     * @return encoded bytes
     * @throws CharacterCodingException
     * @throws UnsupportedEncodingException
     * @throws ArabicShapingException
     */
    public static byte[] convertUTF8OrESCPToIBM864(String escp_or_text) throws CharacterCodingException,
                                                                               UnsupportedEncodingException,
                                                                               ArabicShapingException {
        boolean allAscii = true;
        for (int i =0; i < escp_or_text.length(); i++) {
            //https://wiki.sei.cmu.edu/confluence/display/java/STR01-J.+Do+not+assume+that+a+Java+char+fully+represents+a+Unicode+code+point
            int ch = escp_or_text.codePointAt(i);
            if (ch>255) {
                allAscii = false;
            }
        }
        if (allAscii) {
            //we use 'ISO-8859-1' that will map bytes as it is
            return escp_or_text.getBytes("ISO-8859-1");
        } else {
            return convertUTF8ToIBM864(escp_or_text);
        }
    }

    /**
     * This will try to convert Arabic+English String that is mixed with ESC/P commands
     * this *should* work fine, however there is a possibility that a certain escape codes *may* interfere with algorithms used by Bidi class
     * It is recommended to use LTR or RTL embedding on input string explicitly to avoid problems with Unicode Bidirectional Algorithm
     * See for more information https://stackoverflow.com/questions/6177294/string-concatenation-containing-arabic-and-western-characters
     * examples:
     * <pre>
     * myEnglishString + "\u202B" + myArabicString + "\u202C" + moreEnglish
     * or
     * myArabicString + "\u202A" + myEnglishString + "\u202C" + moreArabic
     * </pre>
     *
     * @param str
     * @return
     * @throws ArabicShapingException
     * @throws IOException
     */
    public static byte[] convertUTF8ToIBM864MixedWithESCP(String str) throws ArabicShapingException, IOException {

        Bidi para = new Bidi();
        para.setPara(str,  Bidi.LEVEL_DEFAULT_LTR, null);
        String data = para.writeReordered(Bidi.DO_MIRRORING);

        //Split the String into separate blocks, based on code points < 256 and > 256
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int i = 0;
        while (i < data.length()) {
            int ch = data.codePointAt(i);

            if (ch > 255) {
                StringBuilder part = new StringBuilder();

                //we will merge all strings that are > 32 (not control codes)
                while (i< data.length()) {
                    ch = data.codePointAt(i);
                    if (ch < 32) {
                        byte b[] = convertVisualOrderedUTF8ToIBM864(part.toString());
                        bos.write(b);
                        part = null;
                        break;
                    } else {
                        part.appendCodePoint(ch);
                    }
                    i++;
                }
                if (part!=null) {
                    byte b[] = convertVisualOrderedUTF8ToIBM864(part.toString());
                    bos.write(b);
                }

            } else {
                bos.write(ch);
                i++;
            }
        }
        bos.flush();

        return bos.toByteArray();
    }

    /**
     * This is for debugging a unicode string, it will return  the character code point and character name for every characters in the string
     * @param orig input string
     * @return debug string
     */
    static String debugCharactersInString(String orig) {
        StringBuilder sb = new StringBuilder();
        sb.append("Java Unicode String: ");
        for (int i =0; i < orig.length(); i++) {
            //https://wiki.sei.cmu.edu/confluence/display/java/STR01-J.+Do+not+assume+that+a+Java+char+fully+represents+a+Unicode+code+point
            int ch = orig.codePointAt(i);
            //UCharacter uc = new
            char[] chars=Character.toChars(ch);
            sb.append(String.format("%04x %s (", ch, Character.getName(ch)));
            for (int j =0; j < chars.length; j++) {
                if (j>0) {
                    sb.append(", ");
                }
                sb.append(String.format("%c", chars[j]));
            }
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * This is for debugging Arabic bytes that was encoded in IBM864,
     * this will return the character number and character name for every characters in the byte array
     * @param b input bytes (encoded in IBM864)
     * @return debug string
     */
    static String debugArabicBytes(byte []b) {
        StringBuilder sb = new StringBuilder();
        sb.append("IBM864: ");
        for (int i =0; i < b.length; i++) {
            sb.append(String.format("%04x ", b[i]));
            sb.append(String.format("(%s) ", IBM846_CHARACTER_NAMES [b[i]&0xff]));
        }

        return sb.toString();

    }


}
