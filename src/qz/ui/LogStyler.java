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

        enum AttributeFlag {
            LIGHT   (0b0000),
            DARK    (0b0001),
            BOLD    (0b0010),
            ITALIC  (0b0100),
            LENGTH  (0b1000);

            private final int flag;

            AttributeFlag(int flag) {
                this.flag = flag;
            }

            public int get() {
                return flag;
            }
        }

        final SimpleAttributeSet[] attributeArray = new SimpleAttributeSet[AttributeFlag.LENGTH.get()];

        public SimpleAttributeSet getAttributeSet(int flags) {
            SimpleAttributeSet attributeSet = attributeArray[flags];
            if (attributeSet == null) {
                attributeSet = attributeArray[flags] = new SimpleAttributeSet();
                if (flags >= AttributeFlag.ITALIC.get()) {
                    flags -= AttributeFlag.ITALIC.get();
                    StyleConstants.setItalic(attributeSet, true);
                }
                if (flags >= AttributeFlag.BOLD.get()) {
                    flags -= AttributeFlag.BOLD.get();
                    StyleConstants.setBold(attributeSet, true);
                }
                if (flags >= AttributeFlag.DARK.get()) {
                    StyleConstants.setForeground(attributeSet, darkThemeColor);
                } else {
                    StyleConstants.setForeground(attributeSet, lightThemeColor);
                }
            }
            return attributeSet;
        }

        final Color lightThemeColor;
        final Color darkThemeColor;

        LogColor(Color lightThemeColor, Color darkThemeColor) {
            this.lightThemeColor = lightThemeColor;
            this.darkThemeColor = darkThemeColor;
        }

        public static SimpleAttributeSet lookupAttributeSet(TokenGroup tokenGroup, String matchedString) {
            int darkFlag = SystemUtilities.isDarkDesktop() ? AttributeFlag.DARK.get() : AttributeFlag.LIGHT.get();
            switch(tokenGroup) {
                case LEVEL:
                    for(Map.Entry<Level, LogColor> mapEntry : levelColorMap.entrySet()) {
                        if(matchedString.contains(mapEntry.getKey().name())) {
                            return mapEntry.getValue().getAttributeSet(darkFlag | AttributeFlag.BOLD.get());
                        }
                    }
                case STACKTRACE:
                    return tokenColorMap.getOrDefault(tokenGroup, DEFAULT).getAttributeSet(darkFlag | AttributeFlag.ITALIC.get());
                default:
                    return tokenColorMap.getOrDefault(tokenGroup, DEFAULT).getAttributeSet(darkFlag);
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
                    SimpleAttributeSet attr = LogColor.lookupAttributeSet(tokenGroup, tokens.group(1));
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
                    SimpleAttributeSet attr = LogColor.lookupAttributeSet(tokenGroup, tokens.group(1));
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