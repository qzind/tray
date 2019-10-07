package qz.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

/**
 * Create a label at the multiplier of it's normal size, similar to CSS's "em" tag
 */
public class EmLabel extends JLabel {
    public EmLabel() {}
    public EmLabel(String text) {
        super(text);
    }
    public EmLabel(String text, float multiplier) {
        this(text, multiplier, true);
    }
    public EmLabel(String text, float multiplier, boolean underline) {
        super(text);
        Font template = getFont().deriveFont(multiplier * getFont().getSize());
        if (!underline) {
            Map<TextAttribute,Object> attributes = new HashMap<>(template.getAttributes());
            attributes.remove(TextAttribute.UNDERLINE);
            setFont(template.deriveFont(attributes));
        } else {
            setFont(template);
        }
    }
}
