package qz.ui;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Tres on 2/26/2015.
 */
public class LogDialog extends BasicDialog {

    private final int ROWS = 20;
    private final int COLS = 80;

    private JScrollPane logPane;
    private JTextArea logArea;

    private JButton clearButton;

    private WriterAppender logStream;


    public LogDialog(JMenuItem caller, IconCache iconCache) {
        super(caller, iconCache);
        initComponents();
    }

    public void initComponents() {
        setIconImage(getImage(IconCache.Icon.LOG_ICON));

        LinkLabel logDirLabel = new LinkLabel(SystemUtilities.getDataDirectory() + File.separator);
        logDirLabel.setText("Open Log Location");
        setHeader(logDirLabel);

        logArea = new JTextArea(ROWS, COLS);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        // TODO:  Fix button panel resizing issues
        clearButton = addPanelButton("Clear", IconCache.Icon.DELETE_ICON, KeyEvent.VK_L);
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logArea.setText(null);
            }
        });

        logPane = new JScrollPane(logArea);
        setContent(logPane, true);
        setResizable(true);

        // add new appender to Log4J just for text area
        logStream = new WriterAppender(new PatternLayout("[%p] %d{ISO8601} @ %c:%L%n\t%m%n"), new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        logArea.append(String.valueOf((char)b));
                        logPane.getVerticalScrollBar().setValue(logPane.getVerticalScrollBar().getMaximum());
                    }
                });
            }
        });
        logStream.setThreshold(Level.TRACE);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            logArea.setText(null);
            org.apache.log4j.Logger.getRootLogger().addAppender(logStream);
        } else {
            org.apache.log4j.Logger.getRootLogger().removeAppender(logStream);
        }

        super.setVisible(visible);
    }

}
