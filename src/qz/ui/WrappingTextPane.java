package qz.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * JTextPane only wraps on whitespace, and never breaks words, regardless of length
 */
public class WrappingTextPane extends JTextPane {

    // If there is a BREAK_PATTERN within WRAP_WINDOW_CHARS to the end, we will break there
    private static final int WRAP_WINDOW_CHARS = 10;
    // regex of where valid breaks can occur.
    private static final Pattern BREAK_PATTERN = Pattern.compile("[\\s,{}:]");

    private SmartGlyphView glyphView = null;

    public WrappingTextPane() {
        setEditorKit(new StyledEditorKit() {
            private final ViewFactory defaultFactory = super.getViewFactory();

            @Override
            public ViewFactory getViewFactory() {
                return elem -> {
                    String kind = elem.getName();
                    if (AbstractDocument.ContentElementName.equals(kind)) {
                        return glyphView = new SmartGlyphView(elem);
                    }
                    return defaultFactory.create(elem);
                };
            }
        });
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public void setWrapping(boolean wrappingEnabled) {
        this.glyphView.setWrappingEnabled(wrappingEnabled);
    }

    private static class SmartGlyphView extends GlyphView {
        private boolean wrappingEnabled = true;

        SmartGlyphView(Element elem) {
            super(elem);
        }

        @Override
        public float getMinimumSpan(int axis) {
            if (axis == View.X_AXIS) {
                return 0f;
            }
            return super.getMinimumSpan(axis);
        }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            if (axis == View.X_AXIS) {
                try {
                    int p0 = getStartOffset();
                    checkPainter();
                    GlyphPainter gp = getGlyphPainter();
                    if (gp != null) {
                        int p1 = gp.getBoundedPosition(this, p0, pos, len);
                        int preferred = findPreferredBreak(p0, p1);
                        if (preferred != -1) {
                            return View.ExcellentBreakWeight;
                        }
                    }
                } catch (Exception ignored) { }
            }
            return super.getBreakWeight(axis, pos, len);
        }

        @Override
        public View breakView(int axis, int p0, float pos, float len) {
            if (axis != View.X_AXIS) {
                return super.breakView(axis, p0, pos, len);
            }

            checkPainter();
            GlyphPainter gp = getGlyphPainter();
            if (gp == null) {
                return super.breakView(axis, p0, pos, len);
            }

            int p1 = gp.getBoundedPosition(this, p0, pos, len);

            int preferred = findPreferredBreak(p0, p1);
            if (preferred > p0 && preferred <= p1) {
                p1 = preferred;
            }

            if (p0 == getStartOffset() && p1 == getEndOffset()) {
                return this;
            }

            return createFragment(p0, p1);
        }

        /**
         * Find a regex break if it is WRAP_WINDOW_CHARS chars left of the right edge.
         */
        private int findPreferredBreak(int p0, int p1) {
            try {
                if (p1 <= p0) return -1;

                Document doc = getDocument();
                int start = Math.max(p0, p1 - WRAP_WINDOW_CHARS);
                int length = p1 - start;
                if (length <= 0) return -1;

                String text = doc.getText(start, length);

                int lastMatchIdx = -1;
                Matcher m = BREAK_PATTERN.matcher(text);
                while (m.find()) {
                    // find the last match
                    lastMatchIdx = m.end() - 1;
                }

                if (lastMatchIdx != -1) {
                    return start + lastMatchIdx + 1; // break after the last match
                }
            } catch (BadLocationException ignored) {
            }
            return -1;
        }

        public void setWrappingEnabled(boolean wrappingEnabled) {
            // FIXME: NO-OP!
            this.wrappingEnabled = wrappingEnabled;
        }
    }
}