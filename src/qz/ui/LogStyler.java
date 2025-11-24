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
        GRAY(new Color(0x575757), new Color(0x888888)),
        BLUE(new Color(0x0000ff), new Color(0x589dff)),
        GREEN(new Color(0x2e7d32), new Color(0x47c44d)),
        AMBER(new Color(0x805613), new Color(0xf9a825)),
        RED(new Color(0xff0000), new Color(0xeb6261)),
        PURPLE(new Color(0x9c27b0), new Color(0xce33e8)),
        INDIGO(new Color(0x3f51b5), new Color(0x7589ff)),
        DEFAULT(Color.black, Color.white);

        private static final Map<TokenGroup, LogColor> tokenColorMap = new HashMap<>();
        static {
            tokenColorMap.put(TokenGroup.TIMESTAMP, GREEN);
            tokenColorMap.put(TokenGroup.CLASS, PURPLE);
            tokenColorMap.put(TokenGroup.LINE_NUMBER, INDIGO);
        }

        private static final Map<Level, LogColor> levelColorMap = new HashMap<>();
        static {
            levelColorMap.put(Level.OFF, DEFAULT);
            levelColorMap.put(Level.FATAL, RED);
            levelColorMap.put(Level.ERROR, RED);
            levelColorMap.put(Level.WARN, AMBER);
            levelColorMap.put(Level.INFO, GREEN);
            levelColorMap.put(Level.DEBUG, BLUE);
            levelColorMap.put(Level.TRACE, GRAY);
            levelColorMap.put(Level.ALL, DEFAULT);
        }

        private static class AttributePack {
            final SimpleAttributeSet lightTheme;
            final SimpleAttributeSet darkTheme;
            final SimpleAttributeSet lightBoldTheme;
            final SimpleAttributeSet darkBoldTheme;

            private AttributePack() {
                this.lightTheme = new SimpleAttributeSet();
                this.darkTheme = new SimpleAttributeSet();
                this.lightBoldTheme = new SimpleAttributeSet();
                this.darkBoldTheme = new SimpleAttributeSet();
                StyleConstants.setBold(lightBoldTheme, true);
                StyleConstants.setBold(darkBoldTheme, true);
            }
        }

        final AttributePack attributePack;

        LogColor(Color lightThemeColor, Color darkThemeColor) {
            attributePack = new AttributePack();
            StyleConstants.setForeground(attributePack.lightTheme, lightThemeColor);
            StyleConstants.setForeground(attributePack.darkTheme, darkThemeColor);
            StyleConstants.setForeground(attributePack.lightBoldTheme, lightThemeColor);
            StyleConstants.setForeground(attributePack.darkBoldTheme, darkThemeColor);
        }

        public SimpleAttributeSet getThemeColor(Boolean bold) {
            if (bold) {
                return SystemUtilities.isDarkDesktop() ? attributePack.darkBoldTheme : attributePack.lightBoldTheme;
            } else {
                return SystemUtilities.isDarkDesktop()? attributePack.darkTheme : attributePack.lightTheme;
            }
        }

        public static SimpleAttributeSet lookup(Level level) {
            return levelColorMap.get(level).getThemeColor(true);
        }

        public static SimpleAttributeSet lookup(TokenGroup tokenGroup) {
            return tokenColorMap.get(tokenGroup).getThemeColor(false);
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
        LINE_NUMBER (Pattern.compile(":(\\d+)$"));

        private final Pattern pattern;

        TokenGroup(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public SimpleAttributeSet getAttributeSet(String matchString) {
            switch(this) {
                case LEVEL:
                    if (matchString.length() < 3) return null; // should never happen, regex failure guard
                    matchString = matchString.substring(1, matchString.length() -1); // lose the '[ ]' from the match
                    return LogColor.lookup(Level.getLevel(matchString));
                default:
                    return LogColor.lookup(this);
            }
        }
    }

    public static void appendStyledText(StyledDocument doc, String text) {
        synchronized(doc) {
            int offset = doc.getLength();
            append(doc, text);
            String logLine = text.substring(0, text.indexOf('\n'));

            for(TokenGroup tokenGroup : TokenGroup.values()) {
                if (tokenGroup.getPattern() == null) continue;
                Matcher tokens = tokenGroup.getPattern().matcher(logLine);
                if (tokens.find()) {
                    int startIndex = offset + tokens.start(1);
                    int endIndex = offset + tokens.end(1) + 1;
                    SimpleAttributeSet attr = tokenGroup.getAttributeSet(tokens.group(1));
                    doc.setCharacterAttributes(startIndex, endIndex - startIndex, attr, false);
                }
            }
        }
    }

    public static void append(StyledDocument doc, String text) {
        try {
            // logs usually end in \n, we leave it out and prefer to insert it when a new log is appended
            if (doc.getLength() > 0) doc.insertString(doc.getLength(), "\n", null);
            doc.insertString(doc.getLength(), text, null);
        }
        catch(BadLocationException ignore) {}
    }
}