package qz.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.ShellUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Tres on 2/19/2015.
 */
public class LinkLabel extends JLabel {

    private static final Logger log = LoggerFactory.getLogger(LinkLabel.class);

    private ArrayList<ActionListener> actionListeners;


    public LinkLabel() {
        super();
        initialize();
    }

    public LinkLabel(final String text) {
        super(linkify(text));
        initialize();
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Sense the action based on the content of the text
                    if (text.contains("@")) {
                        Desktop.getDesktop().mail(new URI(text));
                    } else {
                        File filePath = new File(text);
                        ShellUtilities.browseDirectory(filePath.isDirectory() ? text : filePath.getParent());
                    }

                }
                catch(Exception ex) {
                    log.error("", ex);
                }
            }
        });
    }

    public LinkLabel(final URL url) {
        super(linkify(url.toString()));
        initialize();
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(url.toURI());
                }
                catch(Exception ex) {
                    log.error("", ex);
                }
            }
        });
    }

    public LinkLabel(final File filePath) {
        super(linkify(filePath.getPath()));
        initialize();
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ShellUtilities.browseDirectory(filePath.isDirectory()? filePath.getCanonicalPath() : filePath.getParent());
                }
                catch(IOException ex) {
                    log.error("", ex);
                }
            }
        });
    }

    private void initialize() {
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
    }

    @Override
    public void setText(String text) {
        super.setText(linkify(text));
    }

    private static String linkify(String text) {
        return "<html><a href=\"#\">" + text + "</a>";
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
