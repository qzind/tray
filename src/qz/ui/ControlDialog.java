package qz.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.ui.component.IconCache;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

/**
 * Created by Tres on 11/16/2022
 * A container for all the System Tray menu items
 */
public class ControlDialog extends BasicDialog implements Themeable {
    private static final Logger log = LogManager.getLogger(ControlDialog.class);

    public enum Category {
        STATUS,
        GENERAL,
        ADVANCED,
        DIAGNOSTIC
    }

    private boolean persistent = false;

    private JPanel statusPanel;
    private JPanel statusLeftPanel;
    private JPanel statusRightPanel;
    private JPanel generalPanel;
    private JPanel advancedPanel;
    private JPanel diagnosticPanel;

    private JLabel runningIndicator;
    private JLabel runningLabel;

    public ControlDialog(IconCache iconCache) {
        super(Constants.ABOUT_TITLE, iconCache);
        initComponents();
    }

    public void add(Component component, Category category) {
        add(component, null, category);
    }

    public void add(Component component, String text, Category category) {
        JComponent toAdd = null;
        if(component instanceof JCheckBoxMenuItem) {
            JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem)component;
            JCheckBox checkBox = new JCheckBox(text == null ? checkBoxMenuItem.getText() : text, checkBoxMenuItem.getIcon(), checkBoxMenuItem.getState());
            Arrays.stream(checkBoxMenuItem.getActionListeners()).forEach(checkBox::addActionListener);

            // Keep original in sync
            checkBox.addChangeListener(e -> {
                checkBoxMenuItem.setState(checkBox.isSelected());
            });

            // Add change listener on the original too
            checkBoxMenuItem.addChangeListener(e -> checkBox.setSelected(checkBoxMenuItem.getState()));

            toAdd = checkBox;
        } else if(component instanceof JMenuItem) {
            JMenuItem menuItem = (JMenuItem)component;
            JButton button = new JButton(text == null ? menuItem.getText() : text, menuItem.getIcon());
            Arrays.stream(menuItem.getActionListeners()).forEach(button::addActionListener);
            toAdd = button;
        } else if(component instanceof JSeparator){
            toAdd = (JSeparator)component;
        } else if (component instanceof JComponent){
            toAdd = (JComponent)component;
        } else {
            log.warn("Cannot add {}, not yet supported.", component.getClass().getSimpleName());
        }
        if(toAdd != null) {
            switch(category) {
                case STATUS:
                    statusRightPanel.add(toAdd);
                    break;
                case ADVANCED:
                    advancedPanel.add(toAdd);
                    break;
                case DIAGNOSTIC:
                    diagnosticPanel.add(toAdd);
                    break;
                case GENERAL:
                default:
                    generalPanel.add(toAdd);
            }
        }
        pack();
    }

    private void initComponents() {

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(createPaddedLineBorder(5, SwingConstants.SOUTH));

        statusLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        statusPanel = new JPanel(new GridLayout(1, 3));

        // Running indicator
        runningIndicator = new JLabel("•");
        runningIndicator.setFont(new Font("", Font.BOLD, 24));
        runningIndicator.setForeground(Constants.STOPPED_COLOR);
        runningIndicator.setBorder(new EmptyBorder(0, 2, 2, 2));
        runningLabel = new JLabel("Stopped");
        runningLabel.setBorder(new EmptyBorder(0, 0, 0, 4));
        statusLeftPanel.add(runningIndicator);
        statusLeftPanel.add(runningLabel);
        statusPanel.add(statusLeftPanel);

        // Version label
        JLabel versionLabel = new JLabel(Constants.ABOUT_TITLE + " " + Constants.VERSION);
        versionLabel.setFont(runningLabel.getFont());
        statusPanel.add(versionLabel);

        // Additional controls added with .add()
        statusPanel.add(statusRightPanel);

        Font headingFont = new JLabel().getFont().deriveFont(16f).deriveFont(Font.BOLD);

        generalPanel = new JPanel();
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        generalPanel.setBorder(createPaddedLineBorder(5, SwingConstants.EAST));
        JLabel generalLabel = new JLabel("General");
        generalLabel.setFont(headingFont);
        generalPanel.add(generalLabel);

        advancedPanel = new JPanel();
        advancedPanel.setLayout(new BoxLayout(advancedPanel, BoxLayout.Y_AXIS));
        advancedPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel advancedLabel = new JLabel("Advanced");
        advancedLabel.setFont(headingFont);
        advancedPanel.add(advancedLabel);

        diagnosticPanel = new JPanel();
        diagnosticPanel.setLayout(new BoxLayout(diagnosticPanel, BoxLayout.Y_AXIS));
        diagnosticPanel.setBorder(createPaddedLineBorder(5, SwingConstants.WEST));
        JLabel diagnosticLabel = new JLabel("Diagnostic");
        diagnosticLabel.setFont(headingFont);
        diagnosticPanel.add(diagnosticLabel);

        setHeader(statusPanel);
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        mainPanel.add(generalPanel, BorderLayout.LINE_START);
        mainPanel.add(advancedPanel, BorderLayout.CENTER);
        mainPanel.add(diagnosticPanel, BorderLayout.LINE_END);
        setContent(mainPanel, false);

        if(persistent) {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    setExtendedState(JFrame.ICONIFIED);
                }
            });
        }

        setResizable(false);
        pack();

        setLocationRelativeTo(null);    // center on main display
    }

    private static Border createPaddedLineBorder(int padding, int position) {
        Color borderColor = new JSeparator().getForeground();
        Border margins = new EmptyBorder(padding, padding, padding, padding);
        Border border;

        switch(position) {
            case SwingConstants.NORTH:
                border = new MatteBorder(1, 0, 0, 0, borderColor);
                break;
            case SwingConstants.WEST:
                border = new MatteBorder(0, 1, 0, 0, borderColor);
                break;
            case SwingConstants.SOUTH:
                border = new MatteBorder(0, 0, 1, 0, borderColor);
                break;
            default:
            case SwingConstants.EAST:
                border = new MatteBorder(0, 0, 0, 1, borderColor);
        }
        return new CompoundBorder(border, margins);
    }

    @Override
    public void setVisible(boolean b) {
        // Prevent closing if persistent mode is enabled
        if(!b && persistent) {
            setExtendedState(JFrame.ICONIFIED);
        } else {
            super.setVisible(b);
        }
    }

    @Override
    public void refresh() {
        ThemeUtilities.refreshAll(this);
    }

    public void setRunningIndicator(IconCache.Icon icon) {
        switch(icon) {
            case WARNING_ICON:
                runningIndicator.setForeground(Constants.PENDING_COLOR);
                runningLabel.setText("Pending");
                break;
            case DANGER_ICON:
                runningIndicator.setForeground(Constants.STOPPED_COLOR);
                runningLabel.setText("Stopped");
                break;
            case DEFAULT_ICON:
            default:
                runningIndicator.setForeground(Constants.RUNNING_COLOR);
                runningLabel.setText("Running");
        }
    }

    /**
     * Sets persistent mode, window can't be closed unless shutdown
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }
}

