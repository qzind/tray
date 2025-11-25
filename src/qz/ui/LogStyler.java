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

import static qz.ui.LogStyler.LogColor.AttributeFlag.*;

public class LogStyler {
    public enum LogColor {
        /* Colors from IntelliJ --> Settings --> Editor --> Color Scheme --> Java */
        GRAY(new Color(0x8c8c8c), new Color(0x7a7e95)),
        TEAL(new Color(0x007e8a), new Color(0x16baac)),
        BLUE(new Color(0x00627a), new Color(0x56a8f5)),
        GREEN(new Color(0x067d17), new Color(0x6aab73)),
        OLIVE(new Color(0x3b5e19), new Color(0x739156)),
        YELLOW(new Color(0x9e880d), new Color(0xb3ae60)),
        AMBER(new Color(0xe07e3d), new Color(0xcf8e6d)),
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
            tokenColorMap.put(TokenGroup.JSON_KEY, PURPLE);
            tokenColorMap.put(TokenGroup.JSON_LITERAL, AMBER);
            tokenColorMap.put(TokenGroup.JSON_STRING, OLIVE);
        }

        private static final Map<Level, LogColor> levelColorMap = new HashMap<>();
        static {
            levelColorMap.put(Level.INFO, TEAL);
            levelColorMap.put(Level.WARN, YELLOW);
            levelColorMap.put(Level.FATAL, RED);
            levelColorMap.put(Level.ERROR, RED);
            levelColorMap.put(Level.DEBUG, BLUE);
            levelColorMap.put(Level.TRACE, DEFAULT);
        }

        /**
         *  An efficient bitwise-aware and bitwise-indexed attribute array
         */
        enum AttributeFlag {
            // These must increment by powers of 2
            DARK(0x01, StyleConstants.Foreground),
            BOLD(0x02, StyleConstants.Bold),
            ITALIC(0x04, StyleConstants.Italic);

            private final int flag;
            public final Object attributeKey;

            AttributeFlag(int flag, Object attributeKey) {
                this.flag = flag;
                this.attributeKey = attributeKey;
            }

            public int get() {
                return flag;
            }

            public boolean isIn(int flags) {
                return (flag & flags) > 0;
            }
        }

        final SimpleAttributeSet[] attributeArray = new SimpleAttributeSet[(int)Math.pow(2, AttributeFlag.values().length)];

        public SimpleAttributeSet getAttributeSet() {
            return getAttributeSet(0);
        }

        public SimpleAttributeSet getAttributeSet(int flags) {
            if (SystemUtilities.isDarkDesktop()) flags |= AttributeFlag.DARK.get();

            SimpleAttributeSet attributeSet = attributeArray[flags];
            if (attributeSet == null) {
                attributeArray[flags] = attributeSet = new SimpleAttributeSet();
                for(AttributeFlag attrFlag : AttributeFlag.values()) {
                    switch(attrFlag) {
                        case DARK: // Color is either dark or light, it cannot be neither
                            Color themeColor = attrFlag.isIn(flags) ? darkThemeColor : lightThemeColor;
                            if (themeColor != null) attributeSet.addAttribute(attrFlag.attributeKey, themeColor);
                            break;
                        default:
                            if (attrFlag.isIn(flags)) attributeSet.addAttribute(attrFlag.attributeKey, true);
                            break;
                    }
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

        public static LogColor lookup(TokenGroup tokenGroup, String matchedString) {
            switch(tokenGroup) {
                case LEVEL:
                    for(Map.Entry<Level, LogColor> mapEntry : levelColorMap.entrySet()) {
                        if(matchedString.contains(mapEntry.getKey().name())) {
                            return mapEntry.getValue();
                        }
                    }
                default:
                    return tokenColorMap.getOrDefault(tokenGroup, DEFAULT);
            }
        }
    }

    /**
     * Ordered log patterns
     *   e.g. [DEBUG] 2025-11-22T14:58:25,875 @ qz.auth.Certificate:224
     */
    public enum TokenGroup {
        LEVEL(Pattern.compile("(\\[[A-Z]+])\\s+")),
        TIMESTAMP(Pattern.compile("([0-9T:.,-]+)\\s+@\\s+")),
        CLASS(Pattern.compile("@\\s+([\\w.$]+):\\d+")),
        LINE_NUMBER(Pattern.compile(":(\\d+)$")),
        WINDOW_CLOSED(Pattern.compile("\n\t(\\([\\w\\s]+\\))\n")),
        STACKTRACE(Pattern.compile("\t(at .*)\n")),
        JSON_KEY(Pattern.compile("(\"[^\"]*\")\\s*:(?=\\s*[^:\\s])")),
        JSON_STRING(Pattern.compile(":\\s*\"([^\"]*)\"")),
        JSON_LITERAL(Pattern.compile(":(?:\\s*)(true|false|null|-?\\d+(?:\\.\\d+)?)\\b"));

        private final Pattern pattern;

        private static final TokenGroup[] FIRST_LINE = new TokenGroup[] { LEVEL, TIMESTAMP, CLASS, LINE_NUMBER };
        private static final TokenGroup[] MESSAGE_LINES = new TokenGroup[] { WINDOW_CLOSED, STACKTRACE, JSON_KEY, JSON_STRING, JSON_LITERAL };

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
                    LogColor logColor = LogColor.lookup(tokenGroup, tokens.group(1));
                    SimpleAttributeSet attr;
                    switch(tokenGroup) {
                        case LEVEL:
                            attr = logColor.getAttributeSet(BOLD.get());
                            break;
                        default:
                            attr = logColor.getAttributeSet();
                    }

                    int startIndex = offset + tokens.start(1);
                    int endIndex = offset + tokens.end(1);
                    doc.setCharacterAttributes(startIndex, endIndex - startIndex, attr, false);
                }
            }

            String message = text.substring(firstLine.length());
            offset += firstLine.length();
            for(TokenGroup tokenGroup : TokenGroup.MESSAGE_LINES) {
                Matcher tokens = tokenGroup.getPattern().matcher(message);

                while (tokens.find()) {
                    System.out.println("matched " + tokenGroup.name() + " to " + tokens.group(1));
                    LogColor logColor = LogColor.lookup(tokenGroup, tokens.group(1));
                    switch(tokenGroup) {
                        case STACKTRACE:
                            // colorize entire message block
                            doc.setCharacterAttributes(offset, message.length(), logColor.getAttributeSet(ITALIC.get()), false);
                            return;
                        default:
                    }

                    int startIndex = offset + tokens.start(1);
                    int endIndex = offset + tokens.end(1);
                    doc.setCharacterAttributes(startIndex, endIndex - startIndex, logColor.getAttributeSet(), false);
                }
            }
        }
    }
}