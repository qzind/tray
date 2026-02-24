package qz.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.auth.RequestState;
import qz.common.Constants;
import qz.common.Sluggable;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;
import qz.ui.component.table.CertificateField;
import qz.ui.component.table.FieldStyle;
import qz.ui.component.table.FieldValueTable;
import qz.ui.component.table.RequestTable;
import qz.ui.headless.HeadlessDialog;
import qz.utils.ByteUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Optional;

import static javax.swing.JComponent.*;
import static javax.swing.WindowConstants.*;
import static qz.ui.component.table.CertificateField.*;

/**
 * Created by Tres on 2/19/2015.
 * A basic allow/block dialog with support for displaying Certificate information
 */
public class GatewayDialog extends HeadlessDialog implements Themeable {
    private static final Logger log = LogManager.getLogger(GatewayDialog.class);

    // Main dialog
    public static String BUTTON_ALLOW = "Allow";
    public static String BUTTON_BLOCK = "Block";
    public static String LINK_REQUEST_DETAILS = "View request details";
    public static String CHECKBOX_REMEMBER = Constants.REMEMBER_THIS_DECISION;

    private final IconCache iconCache;
    private final boolean headless;
    public final String title;

    // Main dialog
    private String description;
    private Color detailColor;
    private ConfirmDialog confirmDialog;
    private String uid;
    private IconCache.Icon icon;
    private String iconToolTip;
    private RequestState requestState;
    private ResponseState responseState;

    // Confirm dialog
    public static String CONFIRM_BLOCK_TITLE = "Confirm";
    public String confirmBlockText;

    // Details dialog
    public static String DETAILS_DIALOG_TITLE = "Details";

    private JDialog dialog;
    private JLabel verifiedLabel;
    private JLabel descriptionLabel;
    private LinkLabel certInfoLabel;
    private DetailsDialog detailsDialog;
    private JButton allowButton;
    private JCheckBox rememberCheckbox;

    public GatewayDialog(boolean headless, Frame owner, String title, IconCache iconCache) {
        this.title = title;
        this.headless = headless;
        this.iconCache = iconCache;
        this.description = "Description missing";
        this.responseState = ResponseState.UNANSWERED;

        if(!headless) {
            initComponents(owner);
            refreshComponents();
        }
    }

    private void initComponents(Frame owner) {
        dialog = new JDialog(owner, title, true);
        dialog.setIconImages(iconCache.getImages(IconCache.Icon.TASK_BAR_ICON));

        confirmDialog = new ConfirmDialog(null, CONFIRM_BLOCK_TITLE, iconCache);

        JPanel descriptionPanel = new JPanel();
        verifiedLabel = new JLabel();
        verifiedLabel.setBorder(new EmptyBorder(3, 3, 3, 3));
        descriptionLabel = new JLabel();

        descriptionPanel.add(verifiedLabel);
        descriptionPanel.add(descriptionLabel);
        descriptionPanel.setBorder(new EmptyBorder(3, 3, 3, 3));

        JPanel optionsPanel = new JPanel();
        allowButton = new JButton(BUTTON_ALLOW, iconCache.getIcon(IconCache.Icon.ALLOW_ICON));
        allowButton.setMnemonic(KeyEvent.VK_A);
        JButton blockButton = new JButton(BUTTON_BLOCK, iconCache.getIcon(IconCache.Icon.BLOCK_ICON));
        blockButton.setMnemonic(KeyEvent.VK_B);
        allowButton.addActionListener(buttonAction);
        blockButton.addActionListener(buttonAction);

        detailsDialog = new DetailsDialog(iconCache);
        certInfoLabel = new LinkLabel();
        certInfoLabel.setAlignmentX(LEFT_ALIGNMENT);
        certInfoLabel.addActionListener(e -> {
            detailsDialog.updateDisplay();
            JOptionPane.showMessageDialog(
                    dialog,
                    detailsDialog,
                    DETAILS_DIALOG_TITLE,
                    JOptionPane.PLAIN_MESSAGE);
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        rememberCheckbox = new JCheckBox(CHECKBOX_REMEMBER, false);
        rememberCheckbox.setMnemonic(KeyEvent.VK_R);
        rememberCheckbox.addActionListener(e -> allowButton.setEnabled(!rememberCheckbox.isSelected() || requestState.isVerified()));
        rememberCheckbox.setAlignmentX(RIGHT_ALIGNMENT);

        bottomPanel.add(certInfoLabel);
        bottomPanel.add(rememberCheckbox);

        optionsPanel.add(allowButton);
        optionsPanel.add(blockButton);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(descriptionPanel);
        mainPanel.add(optionsPanel);
        mainPanel.add(new JSeparator());
        mainPanel.add(bottomPanel);

        dialog.getContentPane().add(mainPanel);

        allowButton.requestFocusInWindow();

        dialog.setDefaultCloseOperation(HIDE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.pack();

        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(null);    // center on main display
    }

    @Override
    public void refresh() {
        ThemeUtilities.refreshAll(dialog, detailsDialog);
        refreshComponents();
    }

    private final transient ActionListener buttonAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            responseState = ResponseState.getState(e.getSource().equals(allowButton), rememberCheckbox.isSelected());

            // Require confirmation for permanent block
            if (responseState == ResponseState.ALWAYS_BLOCK) {
                if (!confirmDialog.prompt(confirmBlockText)) {
                    responseState = ResponseState.TEMPORARY_BLOCK;
                }
            }
            dialog.setVisible(false);
        }
    };

    public final void refreshComponents() {
        if (requestState != null) {
            // TODO:  Add name, publisher
            detailsDialog.setRequest(requestState);
            descriptionLabel.setText("<html><p>" +
                                             description +
                                             "</p><strong>" + requestState.getValidityString() + "</strong>" +
                                             "</html>");
            certInfoLabel.setText(LINK_REQUEST_DETAILS);
            verifiedLabel.setIcon(iconCache.getIcon(icon));
            verifiedLabel.setToolTipText(iconToolTip);
            certInfoLabel.setForeground(detailColor);
        } else {
            descriptionLabel.setText(description);
            verifiedLabel.setIcon(null);
        }

        rememberCheckbox.setSelected(false);
        allowButton.setEnabled(true);
        allowButton.requestFocusInWindow();
        dialog.pack();
    }

    public void prompt(String UID, String descriptionPattern, RequestState requestState, Point position)  {
        // Main dialog
        this.uid = UID;
        this.requestState = requestState;
        this.description = String.format(descriptionPattern, requestState.getCertName());
        this.icon = getIcon(requestState);
        this.iconToolTip = requestState.isSponsored() ? Constants.SPONSORED_TOOLTIP : null;
        this.detailColor = requestState.isVerified() ? Constants.TRUSTED_COLOR : Constants.WARNING_COLOR;

        // Block confirmation dialog
        this.confirmBlockText = String.format(Constants.BLOCK_SITES_TEXT.replace(" blocked ", " block ") + "?",
                                              requestState.hasCertificate() ?
                                                              requestState.getCertName() : "");

        if(headless) {
            try {
                responseState = ResponseState.getState(promptAndWait());
            }
            catch(JSONException e) {
                log.error(e);
            }
        } else {
            refreshComponents();
            SystemUtilities.centerDialog(this.dialog, position);
            dialog.setVisible(true);
        }
    }

    public static IconCache.Icon getIcon(RequestState requestState) {
        if (requestState.isVerified()) {
            return requestState.isSponsored() ? IconCache.Icon.TRUST_SPONSORED_ICON : IconCache.Icon.TRUST_VERIFIED_ICON;
        }
        return requestState.getCertificate().isValid() ? IconCache.Icon.TRUST_ISSUE_ICON : IconCache.Icon.TRUST_MISSING_ICON;
    }

    @Override
    public JDialog getDialog() {
        return dialog;
    }

    public ResponseState getResponseState() {
        return responseState;
    }

    public enum ResponseState {
        ALWAYS_ALLOW, TEMPORARY_ALLOW, ALWAYS_BLOCK, TEMPORARY_BLOCK, UNANSWERED;

        public static ResponseState getState(boolean allow, boolean remember) {
            if(allow) {
                return remember ? ALWAYS_ALLOW : TEMPORARY_ALLOW;
            }
            return remember ? ALWAYS_BLOCK : TEMPORARY_BLOCK;
        }

        public static ResponseState getState(JSONObject json) {

            return getState(json.optBoolean("allow", false),
                            json.optBoolean("remember", false));
        }

        public boolean state() {
            switch(this) {
                case ALWAYS_ALLOW:
                case TEMPORARY_ALLOW:
                    return true;
            }
            return false;
        }

        public static Optional<ResponseState> filter(RequestState requestState) {
            if(requestState.hasSavedCert()) {
                return Optional.of(ALWAYS_ALLOW);
            }
            if(requestState.hasBlockedCert()) {
                return Optional.of(ALWAYS_BLOCK);
            }
            return Optional.empty();
        }

        public boolean alwaysBlockAnonymous(RequestState requestState) {
            return this == ALWAYS_BLOCK && !requestState.hasCertificate();
        }
    }

    @Override
    public LinkedHashMap<String,Object> serialize() throws JSONException {
        String base64icon = "data:image/png;base64," + ByteUtilities.toBase64(iconCache.getImage(icon), "png");
        LinkedHashMap<String, Object> allFields = new LinkedHashMap<>();

        //
        // Main Dialog
        //
        LinkedHashMap<String, Object> mainFields = new LinkedHashMap<>();
        mainFields.put("title", title);
        mainFields.put("_hint", "The main allow/block dialog");
        mainFields.put("uid", uid);
        mainFields.put("description", description);
        mainFields.put("remember", null);
        mainFields.put("validity", requestState.toJson());
        mainFields.put("icon", base64icon);
        mainFields.put("button-allow", new JSONObject()
                .put("value", BUTTON_ALLOW)
        );
        mainFields.put("button-block", new JSONObject()
                .put("value", BUTTON_BLOCK)
        );
        mainFields.put("button-details", new JSONObject()
                .put("value", LINK_REQUEST_DETAILS)
        );
        mainFields.put("checkbox-remember", new JSONObject()
                .put("value", CHECKBOX_REMEMBER)
                .put("checked", requestState.isVerified()));
        allFields.put("main", mainFields);

        //
        // Confirmation Dialog
        //
        LinkedHashMap<String, Object> blockConfirmFields = new LinkedHashMap<>();
        blockConfirmFields.put("title", CONFIRM_BLOCK_TITLE);
        blockConfirmFields.put("_hint", String.format("Only shown when both '%s' AND '%s' are selected", BUTTON_BLOCK, CHECKBOX_REMEMBER));
        blockConfirmFields.put("description", confirmBlockText);
        allFields.put("confirm", blockConfirmFields);

        //
        // Details
        //
        LinkedHashMap<String, Object> detailsFields = new LinkedHashMap<>();
        detailsFields.put("title", DETAILS_DIALOG_TITLE);
        detailsFields.put("_hint", String.format("Shown when '%s' is clicked. Possible 'class' values: %s",
                                                 LINK_REQUEST_DETAILS,
                                                 Sluggable.sluggedArrayString(FieldStyle.values())));

        //
        // Details: Request
        //
        LinkedHashMap<String, Object> requestFields = new LinkedHashMap<>();
        requestFields.put("type", "table");
        requestFields.put("label", DetailsDialog.REQUEST_TABLE_LABEL);
        requestFields.put("name", DetailsDialog.REQUEST_TABLE_NAME);
        requestFields.put("description", DetailsDialog.REQUEST_TABLE_DESCRIPTION);
        JSONArray requestColumns = new JSONArray();
        FieldValueTable.COLUMNS.forEach(requestColumns::put);
        requestFields.put("columns", requestColumns);

        for(RequestTable.RequestField requestField : RequestTable.RequestField.values()) {
            FieldStyle style = RequestTable.getStyle(requestState, requestField);
            requestFields.put(requestField.getFieldName(true), new JSONObject()
                    .put("label", requestField.getLabel())
                    .put("value", requestField.getValue(requestState))
                    .put("class", style.slug())
                    .put("style", new JSONObject()
                            .put("font-weight", style.isBold() ? "bold" : "normal")
                    )
            );
        }
        detailsFields.put("request", requestFields);

        //
        // Details: Certificate
        //
        LinkedHashMap<String, Object> certFields = new LinkedHashMap<>();
        certFields.put("type", "table");
        certFields.put("label", DetailsDialog.CERT_TABLE_LABEL);
        certFields.put("name", DetailsDialog.CERT_TABLE_NAME);
        certFields.put("description", DetailsDialog.CERT_TABLE_DESCRIPTION);
        JSONArray tableColumns = new JSONArray();
        FieldValueTable.COLUMNS.forEach(tableColumns::put);
        certFields.put("columns", tableColumns);

        if(requestState.hasCertificate()) {
            for(Type type : Type.values()) {
                CertificateField field = new CertificateField(type, requestState.getCertificate());
                FieldStyle style = field.getStyle();
                certFields.put(field.getType().slug(), new JSONObject()
                        .put("label", field.getLabel())
                        .put("value", field.getValue())
                        .put("class", style.slug())
                        .put("style", new JSONObject()
                                .put("font-weight", style.isBold() ? "bold" : "normal")
                        )
                );
            }
        }
        detailsFields.put("cert", certFields);

        allFields.put("details", detailsFields);

        //
        // Response Template
        //
        LinkedHashMap<String, Object> sampleFields = new LinkedHashMap<>();
        sampleFields.put("allow", false);
        sampleFields.put("remember", false);

        allFields.put("_sample_response", sampleFields);

        return allFields;
    }

}

