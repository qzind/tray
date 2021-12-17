package qz.ui.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.ui.Themeable;
import qz.utils.ShellUtilities;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tres on 2/19/2015.
 */
public class LinkLabel extends EmLabel implements Themeable {

    private static final Logger log = LogManager.getLogger(LinkLabel.class);

    private ArrayList<ActionListener> actionListeners;

    public LinkLabel() {
        super();
        initialize();
    }

    public LinkLabel(String text) {
        super(text);
        initialize();
    }

    public LinkLabel(String text, float multiplier, boolean underline) {
        super(text, multiplier, underline);
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
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    Desktop.getDesktop().browse(location.toURI());
                }
                catch(Exception e) {
                    log.error("", e);
                }
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

        actionListeners = new ArrayList<>();

        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for(ActionListener actionListener : actionListeners) {
                    actionListener.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "mouseClicked"));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }
        });

        refresh();
    }

    @Override
    public void refresh() {
        setForeground(Constants.TRUSTED_COLOR);
    }

    public void addActionListener(ActionListener action) {
        if (!actionListeners.contains(action)) {
            actionListeners.add(action);
        }
    }

    public void removeActionListener(ActionListener action) {
        actionListeners.remove(action);
    }

}
