package qz.ui.component;

import org.joor.Reflect;
import qz.auth.Certificate;
import qz.common.Constants;
import qz.ui.Themeable;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Created by Tres on 2/22/2015.
 * Displays Certificate information in a JTable
 */
public class CertificateTable extends DisplayTable implements Themeable {

    /**
     * Certificate fields to be displayed (and the corresponding function to Reflect upon)
     */
    enum CertificateField {
        ORGANIZATION("Organization", "getOrganization"),
        COMMON_NAME("Common Name", "getCommonName"),
        TRUSTED("Trusted", "isTrusted"),
        VALID_FROM("Valid From", "getValidFrom"),
        VALID_TO("Valid To", "getValidTo"),
        FINGERPRINT("Fingerprint", "getFingerprint");

        String description;
        String callBack;

        CertificateField(String description, String callBack) {
            this.description = description;
            this.callBack = callBack;
        }

        /**
         * Returns the <code>String</code> value associated with this certificate field
         *
         * @return Certificate field such as "commonName"
         */
        public String getValue(Certificate cert) {
            if (cert == null) {
                return "";
            }

            Reflect reflect = Reflect.on(cert).call(callBack);
            Object value = reflect == null? null:reflect.get();
            if (value == null) {
                return "";
            }
            return value.toString();
        }

        @Override
        public String toString() {
            return description;
        }

        public String getDescription() {
            return description;
        }

        public static int size() {
            return values().length;
        }
    }

    private Certificate cert;

    private Instant warn;
    private Instant now;

    public CertificateTable(IconCache iconCache) {
        super(iconCache);
        setDefaultRenderer(Object.class, new CertificateTableCellRenderer());
    }

    public void setCertificate(Certificate cert) {
        this.cert = cert;
        refreshComponents();
    }

    @Override
    public void refreshComponents() {
        if (cert == null) {
            return;
        }

        now = Instant.now();
        warn = now.plus(Constants.EXPIRY_WARN, ChronoUnit.DAYS);

        removeRows();

        // First Column
        for(CertificateField field : CertificateField.values()) {
            model.addRow(new Object[] {field, field.getValue(cert)});
        }

        repaint();
    }

    @Override
    public void refresh() {
        refreshComponents();
        ((StyledTableCellRenderer)getDefaultRenderer(Object.class)).refresh();
    }

    public void autoSize() {
        super.autoSize(CertificateField.size(), 2);
    }


    /** Custom cell renderer for JTable to allow colors and styles not directly available in a JTable */
    private class CertificateTableCellRenderer extends StyledTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            // First Column
            if (value instanceof CertificateField) {
                label = stylizeLabel(STATUS_NORMAL, label, isSelected);
                if (iconCache != null) {
                    label.setIcon(iconCache.getIcon(IconCache.Icon.FIELD_ICON));
                }
                return label;
            }

            // Second Column
            if (cert == null || col < 1) { return stylizeLabel(STATUS_NORMAL, label, isSelected); }

            CertificateField field = (CertificateField)table.getValueAt(row, col - 1);
            if (field == null) { return stylizeLabel(STATUS_NORMAL, label, isSelected); }
            switch(field) {
                case TRUSTED:
                    label.setText(cert.isValid()? Constants.TRUSTED_CERT:Constants.UNTRUSTED_CERT);
                    return stylizeLabel(!cert.isValid()? STATUS_WARNING:STATUS_TRUSTED, label, isSelected);
                case VALID_FROM:
                    boolean futureExpiration = cert.getValidFromDate().isAfter(now);
                    return stylizeLabel(futureExpiration? STATUS_WARNING:STATUS_NORMAL, label, isSelected, "future inception");
                case VALID_TO:
                    boolean expiresSoon = cert.getValidToDate().isBefore(warn);
                    boolean expired = cert.getValidToDate().isBefore(now);
                    String reason = expired? "expired":(expiresSoon? "expires soon":null);
                    return stylizeLabel(expiresSoon || expired? STATUS_WARNING:STATUS_NORMAL, label, isSelected, reason);
                default:
                    return stylizeLabel(STATUS_NORMAL, label, isSelected);
            }
        }

    }

}
