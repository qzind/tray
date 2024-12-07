package qz.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

/**
 * Create a label at the multiplier of its normal size, similar to CSS's "em" tag
 */
public class EmLabel extends JLabel {
    public EmLabel(String text, float multiplier) {
        this(text, multiplier, true);
    }
    public EmLabel(String text, float multiplier, boolean underline) {
        super(text);
        stylizeComponent(this, multiplier, underline);
    }

    public static void stylizeComponent(Component j, float multiplier, boolean underline) {
        Font template = j.getFont().deriveFont(multiplier * j.getFont().getSize());
        if (!underline) {
            Map<TextAttribute,Object> attributes = new HashMap<>(template.getAttributes());
            attributes.remove(TextAttribute.UNDERLINE);
            j.setFont(template.deriveFont(attributes));
        } else {
            j.setFont(template);
        }
    }
}
