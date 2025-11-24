package qz.ui;

import org.apache.logging.log4j.Level;
import qz.utils.SystemUtilities;

import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogStyler {
    public enum LogColor {
        /* Colors from IntelliJ --> Settings --> Editor --> Color Scheme --> Java */
        GRAY(new Color(0x8c8c8c), new Color(0x7a7e95)),
        TEAL(new Color(0x007e8a), new Color(0x16baac)),
        BLUE(new Color(0x00627a), new Color(0x56a8f5)),
        GREEN(new Color(0x067d17), new Color(0x6aab73)),
        AMBER(new Color(0x9e880d), new Color(0xb3ae60)),
        RED(new Color(0xff0000), new Color(0xfa6675)),
        PURPLE(new Color(0x851691), new Color(0xc77dbb)),
        DEFAULT(null, null);

        private static final Map<TokenGroup, LogColor> tokenColorMap = new HashMap<>();
        static {
            tokenColorMap.put(TokenGroup.TIMESTAMP, GREEN);
            tokenColorMap.put(TokenGroup.CLASS, PURPLE);
            tokenColorMap.put(TokenGroup.LINE_NUMBER, PURPLE);
            tokenColorMap.put(TokenGroup.WINDOW_CLOSED, GRAY);
            tokenColorMap.put(TokenGroup.STACKTRACE, RED);
        }

        private static final Map<Level, LogColor> levelColorMap = new HashMap<>();
        static {
            levelColorMap.put(Level.INFO, TEAL);
            levelColorMap.put(Level.WARN, AMBER);
            levelColorMap.put(Level.FATAL, RED);
            levelColorMap.put(Level.ERROR, RED);
            levelColorMap.put(Level.DEBUG, BLUE);
            levelColorMap.put(Level.TRACE, DEFAULT);
        }

        final SimpleAttributeSet lightTheme;
        final SimpleAttributeSet darkTheme;
        final SimpleAttributeSet lightThemeBold;
        final SimpleAttributeSet darkThemeBold;
        final SimpleAttributeSet lightThemeItalic;
        final SimpleAttributeSet darkThemeItalic;

        LogColor(Color lightThemeColor, Color darkThemeColor) {
            lightThemeBold = new SimpleAttributeSet();
            StyleConstants.setBold(lightThemeBold, true);
            lightThemeItalic = new SimpleAttributeSet();
            StyleConstants.setItalic(lightThemeItalic, true);
            if(lightThemeColor != null) {
                lightTheme = new SimpleAttributeSet();
                StyleConstants.setForeground(lightTheme, lightThemeColor);
                StyleConstants.setForeground(lightThemeBold, lightThemeColor);
                StyleConstants.setForeground(lightThemeItalic, lightThemeColor);
            } else {
                lightTheme =  null;
            }
            darkThemeBold = new SimpleAttributeSet();
            StyleConstants.setBold(darkThemeBold, true);
            darkThemeItalic = new SimpleAttributeSet();
            StyleConstants.setItalic(darkThemeItalic, true);
            if(darkThemeColor != null) {
                darkTheme = new SimpleAttributeSet();
                StyleConstants.setForeground(darkTheme, darkThemeColor);
                StyleConstants.setForeground(darkThemeBold, darkThemeColor);
                StyleConstants.setForeground(darkThemeItalic, darkThemeColor);
            } else {
                darkTheme = null;
            }
        }

        public SimpleAttributeSet getThemeColor(boolean bold, boolean italic) {
            if (bold) {
                return SystemUtilities.isDarkDesktop() ? darkThemeBold : lightThemeBold;
            } else if(italic) {
                return SystemUtilities.isDarkDesktop() ? darkThemeItalic : lightThemeItalic;
            } else {
                return SystemUtilities.isDarkDesktop() ? darkTheme : lightTheme;
            }
        }

        public static SimpleAttributeSet getAttributeSet(TokenGroup tokenGroup, String matchedString) {
            switch(tokenGroup) {
                case LEVEL:
                    for(Map.Entry<Level, LogColor> mapEntry : levelColorMap.entrySet()) {
                        if(matchedString.contains(mapEntry.getKey().name())) {
                            return mapEntry.getValue().getThemeColor(true /* all levels are bold */, false);
                        }
                    }
                case STACKTRACE:
                    return tokenColorMap.getOrDefault(tokenGroup, DEFAULT).getThemeColor(false, true /* all stacktrace italics */);
                default:
                    return tokenColorMap.getOrDefault(tokenGroup, DEFAULT).getThemeColor(false, false);
            }
        }
    }

    /**
     * Ordered log patterns
     *   e.g. [DEBUG] 2025-11-22T14:58:25,875 @ qz.auth.Certificate:224
     */
    public enum TokenGroup {
        LEVEL       (Pattern.compile("(\\[[A-Z]+])\\s+")),
        TIMESTAMP   (Pattern.compile("([0-9T:.,-]+)\\s+@\\s+")),
        CLASS       (Pattern.compile("@\\s+([\\w.$]+):\\d+")),
        LINE_NUMBER (Pattern.compile(":(\\d+)$")),
        WINDOW_CLOSED(Pattern.compile("\n\t(\\([\\w\\s]+\\))\n")),
        STACKTRACE(Pattern.compile("\t(at .*)\n"));

        private final Pattern pattern;

        private static final TokenGroup[] FIRST_LINE = new TokenGroup[] { LEVEL, TIMESTAMP, CLASS, LINE_NUMBER };
        private static final TokenGroup[] MESSAGE_LINES = new TokenGroup[] { WINDOW_CLOSED, STACKTRACE };

        TokenGroup(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }
     }

    public static void appendStyledText(StyledDocument doc, String text) throws BadLocationException {
        synchronized(doc) {
            int offset = doc.getLength();
            doc.insertString(doc.getLength(), text, null);
            String firstLine = text.substring(0, text.indexOf('\n'));

            for(TokenGroup tokenGroup : TokenGroup.FIRST_LINE) {
                Matcher tokens = tokenGroup.getPattern().matcher(firstLine);
                if (tokens.find()) {
                    SimpleAttributeSet attr = LogColor.getAttributeSet(tokenGroup, tokens.group(1));
                    if(attr == null) continue;
                    int startIndex = offset + tokens.start(1);
                    int endIndex = offset + tokens.end(1);
                    doc.setCharacterAttributes(startIndex, endIndex - startIndex, attr, false);
                }
            }

            String message = text.substring(firstLine.length());
            offset += firstLine.length();
            for(TokenGroup tokenGroup : TokenGroup.MESSAGE_LINES) {
                Matcher tokens = tokenGroup.getPattern().matcher(message);

                if (tokens.find()) {
                    SimpleAttributeSet attr = LogColor.getAttributeSet(tokenGroup, tokens.group(1));
                    if(attr == null) continue;
                    switch(tokenGroup) {
                        case STACKTRACE:
                            // colorize entire message block
                            doc.setCharacterAttributes(offset, message.length(), attr, false);
                            return;
                        default:
                    }

                    int startIndex = offset + tokens.start(1);
                    int endIndex = offset + tokens.end(1);
                    doc.setCharacterAttributes(startIndex, endIndex - startIndex, attr, false);
                }
            }

        }
    }
}