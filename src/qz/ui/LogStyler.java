package qz.ui;

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
    // Don't try to include a LogManager.logger here, are you crazy?
    static Pattern LOG_PATTERN = Pattern.compile(
        "^" +
        "(\\[[A-Z]+])\\s+" +            // group 1: LEVEL
        "([0-9T:.,-]+)\\s+@\\s+" +      // group 2: TIMESTAMP
        "([\\w.$]+):" +                 // group 3: CLASS
        "(\\d+)" +                      // group 4: LINE
        "(?:\\s+(.*))?" +               // group 5: MSG
        "$"
    );

    //public enum LogColors {
    //    GRAY(Color.GRAY),
    //    GREEN(Color.GREEN),
    //    DARK_GREEN(Color.decode("#51A687")),
    //    MAGENTA(Color.MAGENTA),
    //    BLUE(Color.BLUE),
    //    BLACK(Color.BLACK);
    //
    //    Color color;
    //    LogColors(Color c) {
    //        color = c;
    //    }
    //}

    public static Color defaultColor = Color.gray;

    private static final Map<String, Color> LEVEL_COLORS = new HashMap<>();
    static {
        LEVEL_COLORS.put("[TRACE]",     new Color(0x888888)); // gray
        LEVEL_COLORS.put("[DEBUG]",     new Color(0x1565C0)); // blue
        LEVEL_COLORS.put("[INFO]",      new Color(0x2E7D32)); // green
        LEVEL_COLORS.put("[WARN]",      new Color(0xF9A825)); // amber
        LEVEL_COLORS.put("[ERROR]",     new Color(0xC62828)); // red
        LEVEL_COLORS.put("[FATAL]",     new Color(0xB71C1C)); // dark red
        LEVEL_COLORS.put("TIMESTAMP",   new Color(0x51A687)); // dark green
        LEVEL_COLORS.put("CLASS",       new Color(0x9C27B0)); // purple
        LEVEL_COLORS.put("LINE",        new Color(0x3F51B5)); // indigo/blue
        LEVEL_COLORS.put("MSG",         defaultColor); // black
    }

    public enum TokenGroup {
        WHOLE_STRING,
        LEVEL,
        TIMESTAMP,
        CLASS,
        LINE,
        MSG
    }

    public static void appendStyledText(StyledDocument doc, String text) {
        Matcher tokens = LOG_PATTERN.matcher(text);
        int offset = doc.getLength();
        append(doc, text);
        if (!tokens.matches()) return;

        SimpleAttributeSet attrs = new SimpleAttributeSet();

        for (TokenGroup group : TokenGroup.values()) {
            if (group == TokenGroup.WHOLE_STRING) continue;

            int startIndex = offset + tokens.start(group.ordinal());
            int endIndex = offset + tokens.end(group.ordinal()) + 1;
            String token = tokens.group(group.ordinal());

            if (group == TokenGroup.LEVEL) {
                StyleConstants.setForeground(attrs, LEVEL_COLORS.getOrDefault(token, defaultColor));
                StyleConstants.setBold(attrs, true);
            } else {
                StyleConstants.setForeground(attrs, LEVEL_COLORS.getOrDefault(group.name(), defaultColor));
                StyleConstants.setBold(attrs, false);
            }
            doc.setCharacterAttributes(startIndex, endIndex - startIndex, attrs, false);
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