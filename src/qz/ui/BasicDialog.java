package qz.ui;

import qz.common.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/**
 * Created by Tres on 2/23/2015.
 */
public class BasicDialog extends JDialog {
    private JPanel mainPanel;
    private JComponent headerComponent;
    private JComponent contentComponent;

    private JPanel buttonPanel;
    private JButton closeButton;

    private IconCache iconCache;

    private int stockButtonCount = 0;

    public BasicDialog(JMenuItem caller, IconCache iconCache) {
        super((Frame)null, caller.getText().replaceAll("\\.+", ""), true);
        this.iconCache = iconCache;
        initBasicComponents();
    }

    public BasicDialog(Frame owner, String title, IconCache iconCache) {
        super(owner, title, true);
        this.iconCache = iconCache;
        initBasicComponents();
    }

    public void initBasicComponents() {
        setIconImage(iconCache.getImage(IconCache.Icon.DEFAULT_ICON));
        mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(Constants.BORDER_PADDING, Constants.BORDER_PADDING, Constants.BORDER_PADDING, Constants.BORDER_PADDING));

        headerComponent = new JLabel();
        headerComponent.setBorder(new EmptyBorder(0, 0, Constants.BORDER_PADDING, Constants.BORDER_PADDING));
        mainPanel.add(headerComponent, BorderLayout.PAGE_START);

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        closeButton = addPanelButton("Close", IconCache.Icon.ALLOW_ICON, KeyEvent.VK_C);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        stockButtonCount = buttonPanel.getComponents().length;

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(headerComponent, BorderLayout.PAGE_START);
        mainPanel.add(contentComponent = new JLabel("Hello world!"), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.PAGE_END);

        addKeyListener(KeyEvent.VK_ESCAPE, closeButton);

        getContentPane().add(mainPanel);
        setResizable(false);

        pack();

        setLocationRelativeTo(null);    // center on main display
    }

    public JLabel setHeader(String header) {
        if (headerComponent instanceof JLabel) {
            ((JLabel)headerComponent).setText(String.format(header, "").replaceAll("\\s+", " "));
            return (JLabel)headerComponent;
        }
        return (JLabel)setHeader(new JLabel(header));
    }

    public JComponent setHeader(JComponent headerComponent) {
        headerComponent.setAlignmentX(this.headerComponent.getAlignmentX());
        headerComponent.setBorder(this.headerComponent.getBorder());
        mainPanel.add(headerComponent, BorderLayout.PAGE_START, indexOf(this.headerComponent));
        mainPanel.remove(indexOf(this.headerComponent));
        this.headerComponent = headerComponent;
        mainPanel.invalidate();
        return headerComponent;
    }

    public JComponent setContent(JComponent contentComponent, boolean autoCenter) {
        if (contentComponent != null) {
            contentComponent.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(contentComponent, BorderLayout.CENTER, indexOf(this.contentComponent));
        }

        mainPanel.remove(indexOf(this.contentComponent));
        this.contentComponent = contentComponent;
        mainPanel.invalidate();
        pack();
        if (autoCenter) {
            setLocationRelativeTo(null);
        }
        return contentComponent;
    }

    public JButton addPanelButton(String title, IconCache.Icon icon, int mnemonic) {
        return addPanelButton(title, iconCache == null? null:iconCache.getIcon(icon), mnemonic);
    }

    public JButton addPanelButton(String title, Icon icon, int mnemonic) {
        JButton button = new JButton(title, icon);
        button.setMnemonic(mnemonic);
        buttonPanel.add(button, buttonPanel.getComponents().length - stockButtonCount);
        return button;
    }

    public void addKeyListener(int virtualKey, final AbstractButton actionButton) {
        getRootPane().getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(virtualKey, 0), actionButton.toString());
        getRootPane().getActionMap().put(actionButton.toString(), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionButton.doClick();
            }
        });
    }

    public int indexOf(Component findComponent) {
        int i = -1;
        for(Component currentComponent : mainPanel.getComponents()) {
            i++;
            if (findComponent == currentComponent) {
                break;
            }
        }
        return i;
    }

    public BufferedImage getImage(IconCache.Icon icon) {
        if (iconCache != null) {
            return iconCache.getImage(icon);
        }
        return null;
    }

    public ImageIcon getIcon(IconCache.Icon icon) {
        if (iconCache != null) {
            return iconCache.getIcon(icon);
        }
        return null;
    }
}
