package qz.ui;

import javax.swing.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * JTextPane only wraps on whitespace, and never breaks words, regardless of length.
 * LineWrapTextPane breaks after a BREAK_PATTERN match, or breaks long strings if a valid
 * break location isn't within MAX_BREAK_DISTANCE from the end of the view. Wrapping
 * can be toggled with setWrapping.
 */
public class LineWrapTextPane extends JTextPane {
    // If there is a BREAK_PATTERN within MAX_BREAK_DISTANCE of the end, we will break there
    private static final int MAX_BREAK_DISTANCE = 10;
    // regex of where valid breaks can occur
    private static final Pattern BREAK_PATTERN = Pattern.compile("[\\s,{}:]");
    LineWrapGlyphView glyphView;

    private boolean wrappingEnabled = true;

    public LineWrapTextPane() {
        // wrapper for editorKit
        setEditorKit(new StyledEditorKit() {
            private final ViewFactory defaultFactory = super.getViewFactory();

            @Override
            public ViewFactory getViewFactory() {
                // wrapper for ViewFactory, so we can control ContentElement with our LineWrapGlyphView
                return elem -> {
                    String kind = elem.getName();
                    if (AbstractDocument.ContentElementName.equals(kind)) {
                        return glyphView = new LineWrapGlyphView(elem);
                    }
                    return defaultFactory.create(elem);
                };
            }
        });
    }

    // The horizontal scrollbar should only be enabled if wrapping is off
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return wrappingEnabled;
    }

    /**
     * Enables/disables lineWrap. Calls to setWrapping fire a repaint and relayout on the LineWrapTextPane
     * @param wrappingEnabled Enables or disables LineWrap
     */
    public void setWrapping(boolean wrappingEnabled) {
        this.wrappingEnabled = wrappingEnabled;
        rewrap();
    }

    private void rewrap() {
        TextUI ui = getUI();
        if (ui == null) return;

        View root = ui.getRootView(this);
        if (root == null || root.getViewCount() == 0) return;

        // In StyledEditorKit: root[0] is the "section" (BoxView)
        View section = root.getView(0);

        // Its children are ParagraphView (which extends FlowView)
        for (int i = 0; i < section.getViewCount(); i++) {
            View v = section.getView(i);
            if (v instanceof FlowView) {
                v.preferenceChanged(null, true, true);
            }
        }

        revalidate();
        repaint();
    }

    private class LineWrapGlyphView extends GlyphView {
        LineWrapGlyphView(Element elem) {
            super(elem);
        }

        @Override
        public float getMinimumSpan(int axis) {
            if (axis == View.X_AXIS) {
                if (wrappingEnabled) {
                    // If wrapping is on, our minimum width is very small
                    return 0f;
                } else {
                    return getPreferredSpan(View.X_AXIS);
                }
            }
            return super.getMinimumSpan(axis);
        }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            if (!wrappingEnabled && axis == View.X_AXIS) {
                // Wrapping OFF: never break this view horizontally
                return View.BadBreakWeight;
            }
            // Wrapping ON: use the normal break weight from super
            return super.getBreakWeight(axis, pos, len);
        }


        @Override
        public View breakView(int axis, int p0, float pos, float len) {
            if (!wrappingEnabled && axis == View.X_AXIS) {
                // Wrapping OFF: don’t split
                return this;
            }

            if (axis != View.X_AXIS) {
                // If it isn't about the x-axis, we don't care, pass it to super
                return super.breakView(axis, p0, pos, len);
            }

            checkPainter();
            GlyphPainter gp = getGlyphPainter();
            if (gp == null) {
                return super.breakView(axis, p0, pos, len);
            }

            int p1 = gp.getBoundedPosition(this, p0, pos, len);

            // This is where we run the regex, returns the last possible break position
            int preferred = findPreferredBreak(p0, p1);
            if (preferred > p0 && preferred <= p1) {
                p1 = preferred;
            }

            if (p0 == getStartOffset() && p1 == getEndOffset()) {
                // no need to cut it up
                return this;
            }

            // the line is sliced into a fragment, and the remainder is run through again
            return createFragment(p0, p1);
        }

        /**
         * Return the las regex BREAK_PATTERN match index if it is MAX_BREAK_DISTANCE chars left of the right edge.
         * If none is found, the last char index is returned instead.
         */
        private int findPreferredBreak(int p0, int p1) {
            try {
                if (p1 <= p0) return p1;

                Document doc = getDocument();
                int start = Math.max(p0, p1 - MAX_BREAK_DISTANCE);
                int length = p1 - start;
                if (length <= 0) return p1;

                String text = doc.getText(start, length);

                int lastMatchIdx = -1;
                Matcher m = BREAK_PATTERN.matcher(text);
                while (m.find()) {
                    lastMatchIdx = m.end() - 1; // find the last match
                }

                if (lastMatchIdx != -1) {
                    return start + lastMatchIdx + 1; // break after the last match
                }
            } catch (BadLocationException ignore) {}
            // if nothing is found, just break on the last char
            return p1;
        }
    }
}