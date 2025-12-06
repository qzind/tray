package qz.ui;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;
import qz.utils.FileUtilities;
import qz.utils.LoggerUtilities;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.StringWriter;

/**
 * Created by Tres on 2/26/2015.
 */
public class LogDialog extends BasicDialog {

    private final int ROWS = 20;
    private final int COLS = 80;
    private final int MAX_LINES = 500;

    private JScrollPane logPane;
    private JTextArea logArea;

    private JButton clearButton;

    private WriterAppender logStream;

    private AdjustmentListener scrollToEnd;


    public LogDialog(JMenuItem caller, IconCache iconCache) {
        super(caller, iconCache);
        initComponents();
    }

    public void initComponents() {
        int defaultFontSize = new JLabel().getFont().getSize();
        LinkLabel logDirLabel = new LinkLabel(FileUtilities.USER_DIR + File.separator);
        logDirLabel.setLinkLocation(new File(FileUtilities.USER_DIR + File.separator));
        setHeader(logDirLabel);

        StringWriter writeTarget = new StringWriter() {
            @Override
            public void flush() {
                final String logString = getBuffer().toString();
                getBuffer().setLength(0);
                SwingUtilities.invokeLater(() -> {
                    logArea.append(logString);

                    // Limit window to MAX_LINES per #1384
                    try {
                        while(logArea.getLineCount() > MAX_LINES) {
                            logArea.replaceRange(null, 0, logArea.getLineEndOffset(0));
                        }
                    } catch (BadLocationException ignore) {}
                    logPane.getVerticalScrollBar().addAdjustmentListener(scrollToEnd);
                });
            }
        };

        logArea = new JTextArea(ROWS, COLS);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("", Font.PLAIN, defaultFontSize)); //force fallback font for character support
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); // the default caret does some autoscroll stuff, we don't want that

        // TODO:  Fix button panel resizing issues
        clearButton = addPanelButton("Clear", IconCache.Icon.DELETE_ICON, KeyEvent.VK_L);
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logArea.setText(null);

                writeTarget.getBuffer().setLength(0);
            }
        });

        logPane = new JScrollPane(logArea);
        setContent(logPane, true);
        setResizable(true);

        /*
         * Hacky "scroll-to-end" trick
         *  1. Adds a listener to the vertical scroll bar which fires when new content is added
         *  2. Immediately removes this listener so we don't recurse
         *  3. Sets the scrollbar to the max value, simulating auto-scroll
         *
         * TODO: Eventually replace this with proper cursor support
         */
        scrollToEnd = e -> {
            logPane.getVerticalScrollBar().removeAdjustmentListener(scrollToEnd); //fire once
            SwingUtilities.invokeLater(() -> {
                logPane.getVerticalScrollBar().setValue(logPane.getVerticalScrollBar().getMaximum());
            });
        };

        // add new appender to Log4J just for text area
        logStream = WriterAppender.newBuilder()
                .setName("ui-dialog")
                .setLayout(PatternLayout.newBuilder().withPattern("[%p] %d{ISO8601} @ %c:%L%n\t%m%n").build())
                .setFilter(ThresholdFilter.createFilter(Level.TRACE, Filter.Result.ACCEPT, Filter.Result.DENY))
                .setTarget(writeTarget)
                .build();
        logStream.start();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            LoggerUtilities.getRootLogger().addAppender(logStream);
        } else {
            LoggerUtilities.getRootLogger().removeAppender(logStream);
        }

        super.setVisible(visible);
    }

}
