package qz.ui;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import qz.common.TrayManager;
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
    private WrappingTextPane logArea;

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

        LinkLabel logDirLabel = new LinkLabel(FileUtilities.USER_DIR + File.separator);
        logDirLabel.setLinkLocation(new File(FileUtilities.USER_DIR + File.separator));

        setHeader(logDirLabel);

        logArea = new WrappingTextPane();
        logArea.setEditable(false);
        logArea.setPreferredSize(new Dimension(800, 400));

        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, defaultFontSize)); //force fallback font for character support

        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); // the default caret does some autoscroll stuff, we don't want that

        JLabel maxLinesLabel = new JLabel("Max Lines:");
        JTextField maxLinesField = new JTextField("" + maxLogLines, 4);
        maxLinesField.setHorizontalAlignment(SwingConstants.RIGHT);
        maxLinesLabel.setLabelFor(maxLinesField);
        JCheckBox autoScrollBox = new JCheckBox("Auto-Scroll", true);
        addPanelComponent(maxLinesLabel);
        addPanelComponent(maxLinesField);
        addPanelComponent(autoScrollBox);
        addPanelComponent(new JSeparator());
        configureMaxLines(maxLinesField);
        writeTarget = createWriteTarget(autoScrollBox);

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
                logPane.getVerticalScrollBar().removeAdjustmentListener(scrollToEnd); //fire once
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

    private JToolBar createHeader(JPanel maxLines, LinkLabel logDirLabel, JCheckBox autoScrollBox) {
        JToolBar header = new JToolBar();
        header.setFloatable(false);
        header.setLayout(new GridBagLayout());

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
        return header;
    }

    private void configureMaxLines(JTextField linesField) {
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
    }

    private StringWriter createWriteTarget(JCheckBox autoScrollBox) {
        return new StringWriter() {
            @Override
            public void flush() {
                LogDialog.this.append(toString());
                getBuffer().setLength(0);

                truncateLogs();
                if (autoScrollBox.isSelected()) {
                    logPane.getVerticalScrollBar().addAdjustmentListener(scrollToEnd);
                }
            }
        };
    }

    private void truncateLogs() {
        StyledDocument doc = logArea.getStyledDocument();
        Element map = doc.getDefaultRootElement();
        int lines = map.getElementCount();

        // Account for trailing newline by adding one
        int max = maxLogLines + 1;
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

    public void append(String text) {
        try {
            LogStyler.appendStyledText(logArea.getStyledDocument(), text);
        } catch(BadLocationException ignore) {}
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            LoggerUtilities.getRootLogger().addAppender(logStream);
            this.rootPane.requestFocus();
        } else {
            append("\n\n\t(Log window was closed)\n\n\n");
            LoggerUtilities.getRootLogger().removeAppender(logStream);
        }

        super.setVisible(visible);
    }

}
