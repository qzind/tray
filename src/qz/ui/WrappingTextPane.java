package qz.ui;

import javax.swing.*;
import javax.swing.text.*;

/**
 * A JTextPane that wraps long lines properly
 */
public class WrappingTextPane extends JTextPane {
    public WrappingTextPane() {
        setEditorKit(new StyledEditorKit() {
            @Override
            public ViewFactory getViewFactory() {
                final ViewFactory fallback = super.getViewFactory();
                return elem -> {
                    if(AbstractDocument.ContentElementName.equals(elem.getName())) {
                        return new LabelView(elem) {
                            @Override
                            public float getMinimumSpan(int axis) {
                                return axis == X_AXIS ? 0 : super.getMinimumSpan(axis);
                            }
                        };
                    }
                    return fallback.create(elem);
                };
            }
        });
    }
}
