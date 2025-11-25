package qz.ui;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import qz.common.PropertyHelper;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;
import qz.utils.ArgValue;
import qz.utils.FileUtilities;
import qz.utils.LoggerUtilities;
import qz.utils.PrefsSearch;

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
    private final int ROWS = 24;
    private final int COLS = 100;
    private final int DEFAULT_LOG_LINES = 500;

    private final PropertyHelper prefs;

    private JScrollPane logPane;
    private JCheckBox scrollCheckBox;
    private LineWrapTextPane logArea;

    private WriterAppender logStream;
    private StringWriter writeTarget;

    private AdjustmentListener scrollToEnd;
    private int logLines = DEFAULT_LOG_LINES;

    public LogDialog(JMenuItem caller, IconCache iconCache, PropertyHelper prefs) {
        super(caller, iconCache);
        this.prefs = prefs;
        initComponents();
    }

    public void initComponents() {
        int defaultFontSize = new JLabel().getFont().getSize();

        LinkLabel logDirLabel = new LinkLabel(FileUtilities.USER_DIR + File.separator);
        logDirLabel.setLinkLocation(new File(FileUtilities.USER_DIR + File.separator));

        setHeader(logDirLabel);

        logArea = new LineWrapTextPane();
        logArea.setEditable(false);

        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, defaultFontSize)); //force fallback font for character support
        FontMetrics fm = logArea.getFontMetrics(logArea.getFont());
        logArea.setPreferredSize(new Dimension(fm.charWidth('m') * COLS, fm.getHeight() * ROWS));

        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE); // the default caret does some autoscroll stuff, we don't want that

        JLabel maxLinesLabel = new JLabel("Max Lines:");
        JTextField maxLinesField = new JTextField(4);
        maxLinesField.setText(PrefsSearch.getString(ArgValue.TRAY_LOG_LINES, prefs));
        maxLinesField.setHorizontalAlignment(SwingConstants.RIGHT);
        maxLinesLabel.setLabelFor(maxLinesField);
        scrollCheckBox = new JCheckBox("Auto-Scroll");
        scrollCheckBox.setSelected(PrefsSearch.getBoolean(ArgValue.TRAY_LOG_SCROLL));

        JCheckBox wrapCheckBox = new JCheckBox("Wrap Text");
        scrollCheckBox.setSelected(PrefsSearch.getBoolean(ArgValue.TRAY_LOG_WRAP));
        addPanelComponent(maxLinesLabel);
        addPanelComponent(maxLinesField);
        addPanelComponent(scrollCheckBox);
        addPanelComponent(wrapCheckBox);
        addPanelComponent(new JSeparator());
        configureMaxLines(maxLinesField);
        writeTarget = createWriteTarget();

        // TODO:  Fix button panel resizing issues
        JButton clearButton = addPanelButton("Clear", IconCache.Icon.DELETE_ICON, KeyEvent.VK_L);
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

        wrapCheckBox.addActionListener(e -> {
            JCheckBox caller = (JCheckBox)e.getSource();
            logArea.setWrapping(caller.isSelected());
            prefs.setProperty(ArgValue.TRAY_LOG_WRAP, caller.isSelected());

            if (scrollCheckBox.isSelected()) {
                logPane.getVerticalScrollBar().addAdjustmentListener(scrollToEnd);
            }
        });

        scrollCheckBox.addActionListener(e -> {
            // See also
            JCheckBox caller = (JCheckBox)e.getSource();
            prefs.setProperty(ArgValue.TRAY_LOG_SCROLL, caller.isSelected());
        });

        // add new appender to Log4J just for text area
        logStream = WriterAppender.newBuilder()
                .setName("ui-dialog")
                .setLayout(PatternLayout.newBuilder().withPattern("[%p] %d{ISO8601} @ %c:%L%n\t%m%n").build())
                .setFilter(ThresholdFilter.createFilter(Level.TRACE, Filter.Result.ACCEPT, Filter.Result.DENY))
                .setTarget(writeTarget)
                .build();
        logStream.start();
    }

    private void configureMaxLines(JTextField linesField) {
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        KeyStroke ent = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

        linesField.getInputMap(JComponent.WHEN_FOCUSED).put(esc, "textCancel");
        linesField.getActionMap().put("textCancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                linesField.setText("" + DEFAULT_LOG_LINES);
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
                prefs.setProperty(ArgValue.TRAY_LOG_LINES, logLines);
                truncateLogs();
            }
        });
    }

    private StringWriter createWriteTarget() {
        return new StringWriter() {
            @Override
            public void flush() {
                LogDialog.this.append(toString());
                getBuffer().setLength(0);

                truncateLogs();
                if (scrollCheckBox.isSelected()) { // FIXME: Not EDT-safe!
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
        int max = DEFAULT_LOG_LINES + 1;
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
            if (i > 0) logLines = i;
        } catch (Exception ignore) {} // bad number or not a number
        field.setText("" + logLines);
    }

    public void append(String text) {
        try {
            if(text == null || text.isEmpty()) {
                Document doc = logArea.getDocument();
                int len = doc.getLength();
                doc.insertString(len, " ", new SimpleAttributeSet());
                doc.remove(len, 1);
            } else {
                LogStyler.appendStyledText(logArea.getStyledDocument(), text);
            }
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

    @Override
    public void refresh() {
        try {
            Document doc = logArea.getDocument();
            String[] lines = doc.getText(0, doc.getLength()).split("\\r?\\n");
            doc.remove(0, doc.getLength());
            for(String line : lines) {
               append(line + "\n");
            }
        } catch(BadLocationException ignore) {}
        super.refresh();
    }
}
