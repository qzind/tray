package qz.ui;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;
import qz.utils.FileUtilities;
import qz.utils.LoggerUtilities;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.StringWriter;

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
        int defaultFontSize = new JLabel().getFont().getSize();
        LinkLabel logDirLabel = new LinkLabel(FileUtilities.USER_DIR + File.separator);
        logDirLabel.setLinkLocation(new File(FileUtilities.USER_DIR + File.separator));
        setHeader(logDirLabel);

        StringWriter writeTarget = new StringWriter() {
            @Override
            public void flush() {
                SwingUtilities.invokeLater(() -> {
                    StringBuffer buf = getBuffer();
                    logArea.append(buf.toString());
                    buf.setLength(0);
                    logPane.getVerticalScrollBar().setValue(logPane.getVerticalScrollBar().getMaximum());
                });
            }
        };

        logArea = new JTextArea(ROWS, COLS);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("", Font.PLAIN, defaultFontSize)); //force fallback font for character support

        //log truncation
        AbstractDocument doc = (AbstractDocument)logArea.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
              @Override
              public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                  super.insertString(fb, offset, string, attr);

                  // calls to fb bypass this filter, avoiding recursion
                  Document doc = fb.getDocument();
                  Element map = doc.getDefaultRootElement();
                  int lines = map.getElementCount();
                  if (lines > 200) {
                      int i = map.getElement(lines - 200).getStartOffset();
                      fb.remove(0, i);
                  }
              }
        });

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
            logArea.setText(null);
            LoggerUtilities.getRootLogger().addAppender(logStream);
        } else {
            LoggerUtilities.getRootLogger().removeAppender(logStream);
        }

        super.setVisible(visible);
    }

}
