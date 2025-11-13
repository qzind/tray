package qz.ui;

import qz.ui.component.IconCache;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Created by Tres on 2/19/2015.
 * A basic allow/block dialog with support for displaying Certificate information
 */
public class ConfirmDialog extends JDialog {
    private JLabel messageLabel;
    private JPanel descriptionPanel;

    private JButton yesButton;
    private JButton noButton;
    private JPanel optionsPanel;
    private JLabel questionLabel;

    private JPanel mainPanel;

    private boolean approved;

    public ConfirmDialog(Frame owner, String title) {
        super(owner, title, true);
        this.approved = false;
        this.setIconImages(IconCache.getInstance().getImages(IconCache.Icon.TASK_BAR_ICON));
        initComponents();
    }

    private void initComponents() {
        descriptionPanel = new JPanel();
        messageLabel = new JLabel();
        questionLabel = new JLabel(IconCache.getInstance().getIcon(IconCache.Icon.QUESTION_ICON));

        descriptionPanel.add(questionLabel);
        descriptionPanel.add(messageLabel);
        descriptionPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
        messageLabel.setText("Are you sure?");

        optionsPanel = new JPanel();
        yesButton = new JButton("OK", IconCache.getInstance().getIcon(IconCache.Icon.ALLOW_ICON));
        yesButton.setMnemonic(KeyEvent.VK_K);
        noButton = new JButton("Cancel", IconCache.getInstance().getIcon(IconCache.Icon.CANCEL_ICON));
        noButton.setMnemonic(KeyEvent.VK_C);
        yesButton.addActionListener(buttonAction);
        noButton.addActionListener(buttonAction);

        optionsPanel.add(yesButton);
        optionsPanel.add(noButton);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(descriptionPanel);
        mainPanel.add(optionsPanel);

        getContentPane().add(mainPanel);

        yesButton.requestFocusInWindow();

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        pack();

        setAlwaysOnTop(true);
        setLocationRelativeTo(null);    // center on main display
    }

    private final transient ActionListener buttonAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            approved = e.getSource().equals(yesButton);
            setVisible(false);
        }
    };

    @Override
    public void setVisible(boolean b) {
        yesButton.requestFocusInWindow();
        super.setVisible(b);
    }

    public boolean isApproved() {
        return approved;
    }

    public String getMessage() {
        return messageLabel.getText();
    }

    public void setMessage(String message) {
        messageLabel.setText(message); pack();
    }

    public boolean prompt(String message) {
        setMessage(message);
        setVisible(true);
        return isApproved();
    }
}

