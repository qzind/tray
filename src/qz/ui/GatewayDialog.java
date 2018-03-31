package qz.ui;

import qz.auth.Certificate;
import qz.common.Constants;
import qz.utils.SystemUtilities;

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
public class GatewayDialog extends JDialog {
    private JLabel verifiedLabel;
    private JLabel descriptionLabel;
    private LinkLabel certInfoLabel;
    private JScrollPane certScrollPane;
    private CertificateTable certTable;
    private JPanel descriptionPanel;

    private JButton allowButton;
    private JButton blockButton;
    private JPanel optionsPanel;

    private JCheckBox persistentCheckBox;
    private JPanel bottomPanel;

    private JPanel mainPanel;

    private final IconCache iconCache;

    private String description;
    private Certificate cert;
    private boolean approved;

    public GatewayDialog(Frame owner, String title, IconCache iconCache) {
        super(owner, title, true);
        this.iconCache = iconCache;
        this.description = "";
        this.approved = false;
        this.setIconImage(iconCache.getImage(IconCache.Icon.DEFAULT_ICON));
        initComponents();
        refreshComponents();
    }

    private void initComponents() {
        descriptionPanel = new JPanel();
        verifiedLabel = new JLabel();
        verifiedLabel.setBorder(new EmptyBorder(3, 3, 3, 3));
        descriptionLabel = new JLabel();

        descriptionPanel.add(verifiedLabel);
        descriptionPanel.add(descriptionLabel);
        descriptionPanel.setBorder(new EmptyBorder(3, 3, 3, 3));

        optionsPanel = new JPanel();
        allowButton = new JButton("Allow", iconCache.getIcon(IconCache.Icon.ALLOW_ICON));
        allowButton.setMnemonic(KeyEvent.VK_A);
        blockButton = new JButton("Block", iconCache.getIcon(IconCache.Icon.BLOCK_ICON));
        blockButton.setMnemonic(KeyEvent.VK_B);
        allowButton.addActionListener(buttonAction);
        blockButton.addActionListener(buttonAction);

        certInfoLabel = new LinkLabel();
        certTable = new CertificateTable(cert, iconCache);
        certScrollPane = new JScrollPane(certTable);
        certInfoLabel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                certTable.setCertificate(cert);
                certTable.autoSize();
                JOptionPane.showMessageDialog(
                        GatewayDialog.this,
                        certScrollPane,
                        "Certificate",
                        JOptionPane.PLAIN_MESSAGE);
            }
        });

        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        persistentCheckBox = new JCheckBox("Remember this decision", false);
        persistentCheckBox.setMnemonic(KeyEvent.VK_R);
        persistentCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                allowButton.setEnabled(!persistentCheckBox.isSelected() || cert.isTrusted());
            }
        });
        persistentCheckBox.setAlignmentX(RIGHT_ALIGNMENT);

        bottomPanel.add(certInfoLabel);
        bottomPanel.add(persistentCheckBox);

        optionsPanel.add(allowButton);
        optionsPanel.add(blockButton);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(descriptionPanel);
        mainPanel.add(optionsPanel);
        mainPanel.add(new JSeparator());
        mainPanel.add(bottomPanel);

        getContentPane().add(mainPanel);

        allowButton.requestFocusInWindow();

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        pack();

        setAlwaysOnTop(true);
        setLocationRelativeTo(null);    // center on main display
    }

    private final transient ActionListener buttonAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            approved = e.getSource().equals(allowButton);

            // Require confirmation for permanent block
            if (!approved && persistentCheckBox.isSelected()) {
                ConfirmDialog confirmDialog = new ConfirmDialog(null, "Please Confirm", iconCache);
                String message = Constants.BLACK_LIST.replace(" blocked ", " block ") + "?";
                message = String.format(message, cert == null? "":cert.getCommonName());
                if (!confirmDialog.prompt(message)) {
                    return;
                }
            }
            setVisible(false);
        }
    };

    public final void refreshComponents() {
        if (cert != null) {
            // TODO:  Add name, publisher
            descriptionLabel.setText("<html>" +
                                             String.format(description, "<p>" + cert.getCommonName()) +
                                             "</p><strong>" + (cert.isTrusted()? Constants.TRUSTED_PUBLISHER:Constants.UNTRUSTED_PUBLISHER) + "</strong>" +
                                             "</html>");
            certInfoLabel.setText("Certificate information");
            verifiedLabel.setIcon(iconCache.getIcon(cert.isTrusted()? IconCache.Icon.VERIFIED_ICON:IconCache.Icon.UNVERIFIED_ICON));
        } else {
            descriptionLabel.setText(description);
            verifiedLabel.setIcon(null);
        }

        approved = false;
        persistentCheckBox.setSelected(false);
        allowButton.setEnabled(true);
        allowButton.requestFocusInWindow();
        pack();
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean isPersistent() {
        return this.persistentCheckBox.isSelected();
    }

    public void setCertificate(Certificate cert) {
        this.cert = cert;
    }

    public Certificate getCertificate() {
        return cert;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean prompt(String description, Certificate cert, Point position) {
        persistentCheckBox.setSelected(false); // prevents re-adding a persistent site to the list it's already on

        setDescription(description);
        setCertificate(cert);
        refreshComponents();
        SystemUtilities.centerDialog(this, position);
        setVisible(true);

        return isApproved();
    }
}

