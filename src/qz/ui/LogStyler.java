package qz.ui;

import org.apache.commons.lang3.ArrayUtils;
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
    public enum LevelColor {
        GRAY(new Color(0x575757), new Color(0x888888)),
        BLUE(new Color(0x0000ff), new Color(0x589dff)),
        GREEN(new Color(0x2e7d32), new Color(0x47c44d), TokenGroup.TIMESTAMP),
        AMBER(new Color(0x805613), new Color(0xf9a825)),
        RED(new Color(0xff0000), new Color(0xeb6261)),
        PURPLE(new Color(0x9c27b0), new Color(0xce33e8), TokenGroup.CLASS),
        INDIGO(new Color(0x3f51b5), new Color(0x7589ff), TokenGroup.LINE),
        DEFAULT(null, null, TokenGroup.MSG);

        // Logging Levels have special handling
        private static final Map<Level, LevelColor> levelColorMap = new HashMap<>();
        static {
            levelColorMap.put(Level.OFF, DEFAULT);
            levelColorMap.put(Level.FATAL,RED);
            levelColorMap.put(Level.ERROR, RED);
            levelColorMap.put(Level.WARN, AMBER);
            levelColorMap.put(Level.INFO, GREEN);
            levelColorMap.put(Level.DEBUG, BLUE);
            levelColorMap.put(Level.TRACE, GRAY);
            levelColorMap.put(Level.ALL, DEFAULT);
        }

        final SimpleAttributeSet lightAttributeSet;
        final SimpleAttributeSet darkAttributeSet;
        final TokenGroup[] tokenGroups;

        LevelColor(Color lightThemeColor, Color darkThemeColor, TokenGroup ... tokenGroups) {
            if(lightThemeColor != null) {
                this.lightAttributeSet = new SimpleAttributeSet();
                StyleConstants.setForeground(lightAttributeSet, lightThemeColor);
            } else {
                this.lightAttributeSet = null;
            }
            if(darkThemeColor != null) {
                this.darkAttributeSet = new SimpleAttributeSet();
                StyleConstants.setForeground(darkAttributeSet, darkThemeColor);
            } else {
                this.darkAttributeSet = null;
            }
            this.tokenGroups = tokenGroups;
        }

        public SimpleAttributeSet getAttributeSet() {
            return SystemUtilities.isDarkDesktop() ? darkAttributeSet : lightAttributeSet;
        }

        public static SimpleAttributeSet getAttributeSet(TokenGroup tokenGroup, String matchedString) {
            switch(tokenGroup) {
                case LEVEL: // parse from levelColorMap
                    for(Map.Entry<Level, LevelColor> mapEntry : levelColorMap.entrySet()) {
                        if(matchedString.contains(mapEntry.getKey().name())) {
                            SimpleAttributeSet set = (SimpleAttributeSet)mapEntry.getValue().getAttributeSet().clone();
                            StyleConstants.setBold(set, true);
                            return set;
                        }
                    }
                default:
                    for(LevelColor levelColor : values()) {
                        if(ArrayUtils.contains(levelColor.tokenGroups, tokenGroup)) {
                            return levelColor.getAttributeSet();
                        }
                    }
            }

            return DEFAULT.getAttributeSet();
        }
    }

    /**
     * Ordered log patterns
     *   e.g. [DEBUG] 2025-11-22T14:58:25,875 @ qz.auth.Certificate:224
     */
    public enum TokenGroup {
        LEVEL    (Pattern.compile("(\\[[A-Z]+])\\s+")),
        TIMESTAMP(Pattern.compile("([0-9T:.,-]+)\\s+@\\s+")),
        CLASS    (Pattern.compile("@\\s+([\\w.$]+):\\d+")),
        LINE     (Pattern.compile(":(\\d+)$")) ,
        MSG      (null),
        WHOLE_STRING(null);

        private final Pattern pattern;

        TokenGroup(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }

    public static void appendStyledText(StyledDocument doc, String text) {
        int offset = doc.getLength();
        append(doc, text);
        String logLine = text.substring(0, text.indexOf('\n'));

        for(TokenGroup tokenGroup : TokenGroup.values()) {
            if(tokenGroup.getPattern() == null) continue;
            Matcher tokens = tokenGroup.getPattern().matcher(logLine);
            if(tokens.find()) {
                int startIndex = offset + tokens.start(1);
                int endIndex = offset + tokens.end(1) + 1;
                String matchedString = tokens.group(1);
                SimpleAttributeSet attr = LevelColor.getAttributeSet(tokenGroup, matchedString);
                if (attr != null) {
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