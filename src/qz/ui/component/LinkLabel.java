package qz.ui.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.ui.Themeable;
import qz.utils.ShellUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tres on 2/19/2015.
 */
public class LinkLabel extends JLabel implements Themeable {

    private static final Logger log = LoggerFactory.getLogger(LinkLabel.class);

    private ArrayList<ActionListener> actionListeners;

    public LinkLabel() {
        super();
        initialize();
    }

    public LinkLabel(final String text, final String action) {
        super(text);
        initialize();

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Sense the action based on the content of the text
                    if (action.contains("@")) {
                        Desktop.getDesktop().mail(new URI(action));
                    } else if (action.contains(File.separator)) {
                        File filePath = new File(action);
                        ShellUtilities.browseDirectory(filePath.isDirectory() ? action : filePath.getParent());
                    } else {
                        Desktop.getDesktop().browse(new URL(action).toURI());
                    }
                }
                catch(Exception ex) {
                    log.error("", ex);
                }
            }
        });
    }


    private void initialize() {
        Map<TextAttribute, Object> attributes = new HashMap<>(getFont().getAttributes());
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
