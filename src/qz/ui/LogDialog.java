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
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.StringWriter;

/**
 * Created by Tres on 2/26/2015.
 */
public class LogDialog extends BasicDialog {

    private JScrollPane logPane;
    private JTextPane logArea;

    private JButton clearButton;

    private WriterAppender logStream;
    private StringWriter writeTarget;

    private AdjustmentListener scrollToEnd;

    private int maxLogLines = 500;

    public LogDialog(JMenuItem caller, IconCache iconCache) {
        super(caller, iconCache);
        initComponents();
    }

    public void initComponents() {
        int defaultFontSize = new JLabel().getFont().getSize();

        JToolBar header = new JToolBar();
        header.setFloatable(false);
        header.setLayout(new GridBagLayout());

        JPanel maxLines = new JPanel();
        JTextField linesField = new JTextField("" + maxLogLines, 4);
        maxLines.setLayout(new BoxLayout(maxLines, BoxLayout.X_AXIS));
        maxLines.add(new JLabel("Max Lines:"));
        maxLines.add(linesField);

        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        KeyStroke ent = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

        linesField.getInputMap(JComponent.WHEN_FOCUSED).put(esc, "textCancel");
        linesField.getActionMap().put("textCancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                linesField.setText("" + maxLogLines);
                linesField.getRootPane().requestFocus();
                // don't pass the event upwards. esc closes the window if this field isn't focused
            }
        });

        linesField.getInputMap(JComponent.WHEN_FOCUSED).put(ent, "textAccept");
        linesField.getActionMap().put("textAccept", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                linesField.getRootPane().requestFocus();
                // if we intentionally lose focus, the focusLost listener will fire
            }
        });

        linesField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                parseMaxLines(linesField);
                truncateLogs();
            }
        });

        LinkLabel logDirLabel = new LinkLabel(FileUtilities.USER_DIR + File.separator);
        logDirLabel.setLinkLocation(new File(FileUtilities.USER_DIR + File.separator));

        JCheckBox autoScrollBox = new JCheckBox("Auto-Scroll", true);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        header.add(maxLines, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        header.add(logDirLabel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        header.add(autoScrollBox, gbc);

        setHeader(header);

        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setPreferredSize(new Dimension(-1, 100));
        logArea.setFont(new Font("", Font.PLAIN, defaultFontSize)); //force fallback font for character support

        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); // the default caret does some autoscroll stuff, we don't want that

        writeTarget = new StringWriter() {
            @Override
            public void flush() {
                LogStyler.appendStyledText(logArea.getStyledDocument(), toString().stripTrailing());
                getBuffer().setLength(0);

                truncateLogs();
                if (autoScrollBox.isSelected()) {
                    logPane.getVerticalScrollBar().addAdjustmentListener(scrollToEnd);
                }
            }
        };

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

        scrollToEnd = new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    logPane.getVerticalScrollBar().setValue(logPane.getVerticalScrollBar().getMaximum());
                });
                logPane.getVerticalScrollBar().removeAdjustmentListener(scrollToEnd);
            }
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

    private void truncateLogs() {
        StyledDocument doc = logArea.getStyledDocument();
        Element map = doc.getDefaultRootElement();
        int lines = map.getElementCount();
        int max = Math.max(1, maxLogLines);
        if (lines > max) {
            int i = map.getElement(lines - max).getStartOffset();
            try {
                doc.remove(0, i);
            }
            catch(BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void parseMaxLines(JTextField field) {
        try {
            int i = Integer.parseInt(field.getText().trim());
            if (i > 0) maxLogLines = i;
        } catch (Exception ignore) {} // bad number or not a number
        field.setText("" + maxLogLines);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            LoggerUtilities.getRootLogger().addAppender(logStream);
            this.rootPane.requestFocus();
        } else {
            String message = "\n\n\t(Log window was closed)\n\n\n";
            try {
                //todo maybe append? It may get trimmed
                StyledDocument doc = (StyledDocument) logArea.getDocument();
                doc.insertString(doc.getLength(), message, null);
            } catch (BadLocationException ignore) { }
            LoggerUtilities.getRootLogger().removeAppender(logStream);
        }

        super.setVisible(visible);
    }

}
