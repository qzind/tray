package qz.ui.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.ui.Themeable;
import qz.utils.ShellUtilities;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a JButton which visually appears as a clickable link
 *
 * TODO: Rename this class.  Since switching from JLabel to a JButton, this class now has a misleading name.
 *
 * Created by Tres on 2/19/2015.
 */
public class LinkLabel extends JButton implements Themeable {

    private static final Logger log = LogManager.getLogger(LinkLabel.class);

    public LinkLabel() {
        super();
        initialize();
    }

    public LinkLabel(String text) {
        super(text);
        initialize();
    }

    public LinkLabel(String text, float multiplier, boolean underline) {
        super(text);
        EmLabel.stylizeComponent(this, multiplier, underline);
        initialize();
    }

    public void setLinkLocation(final String url) {
        try {
            setLinkLocation(new URL(url));
        }
        catch(MalformedURLException mue) {
            log.error("", mue);
        }
    }

    public void setLinkLocation(final URL location) {
        addActionListener(ae -> {
            try {
                Desktop.getDesktop().browse(location.toURI());
            }
            catch(Exception e) {
                log.error("", e);
            }
        });
    }

    public void setLinkLocation(final File filePath) {
        addActionListener(ae -> ShellUtilities.browseDirectory(filePath.isDirectory()? filePath.getPath():filePath.getParent()));
    }

    private void initialize() {
        Map<TextAttribute,Object> attributes = new HashMap<>(getFont().getAttributes());
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        setFont(getFont().deriveFont(attributes));
        refresh();
    }

    @Override
    public void refresh() {
        setForeground(Constants.TRUSTED_COLOR);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setBorder(null);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleLinkLabel();
        }
        return accessibleContext;
    }

    protected class AccessibleLinkLabel extends AccessibleJButton {
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.HYPERLINK;
        }
    }
}
