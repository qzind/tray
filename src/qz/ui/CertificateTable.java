package qz.ui;

import org.joor.Reflect;
import qz.auth.Certificate;
import qz.common.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Calendar;

/**
 * Created by Tres on 2/22/2015.
 * Displays Certificate information in a JTable
 */
public class CertificateTable extends JTable {
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
    private DefaultTableModel model;

    private Calendar warn;
    private Calendar now;

    private Color defaultForeground;
    private Color defaultSelectedForeground;

    private IconCache iconCache;

    public CertificateTable(Certificate cert, IconCache iconCache) {
        super();
        initComponents();
        setIconCache(iconCache);
        setCertificate(cert);
    }

    private void initComponents() {
        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int x, int y) { return false; }
        };
        model.addColumn("Field");
        model.addColumn("Value");

        getTableHeader().setReorderingAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowSelectionAllowed(true);

        setDefaultRenderer(Object.class, new CertificateTableCellRenderer());
        setModel(model);

        defaultForeground = UIManager.getDefaults().getColor("Table.foreground");
        defaultSelectedForeground = UIManager.getDefaults().getColor("Table.selectionForeground");
    }

    public void refreshComponents() {
        removeRows();

        if (cert == null) {
            return;
        }

        now = Calendar.getInstance();
        warn = Calendar.getInstance();
        warn.add(Calendar.DAY_OF_MONTH, -1 * Constants.EXPIRY_WARN);

        // First Column
        for(CertificateField field : CertificateField.values()) {
            model.addRow(new Object[] {field, ""});
        }

        // Second Column
        for(int col = 0; col < model.getColumnCount(); col++) {
            for(int row = 0; row < model.getRowCount(); row++) {
                Object cell = (model.getValueAt(row, col));
                if (cell instanceof CertificateField) {
                    model.setValueAt(((CertificateField)cell).getValue(cert), row, col + 1);
                }
            }
        }

        repaint();
    }

    public void setIconCache(IconCache iconCache) {
        this.iconCache = iconCache;
    }

    public void setCertificate(Certificate cert) {
        this.cert = cert;
        refreshComponents();
    }

    public void removeRows() {
        for(int row = model.getRowCount() - 1; row >= 0; row--) {
            model.removeRow(row);
        }
    }

    /**
     * Sets preferred <code>ScrollPane</code> preferred viewable height to match the natural table height
     * Leaves the <code>ScrollPane</code> preferred viewable width as default
     */
    public void autoSize() {
        removeRows();
        for(int row = 0; row < CertificateField.size(); row++) {
            model.addRow(new Object[2]);
        }
        int normalWidth = (int)getPreferredScrollableViewportSize().getWidth();
        int autoHeight = (int)getPreferredSize().getHeight();
        setPreferredScrollableViewportSize(new Dimension(normalWidth, autoHeight));
        setFillsViewportHeight(true);
        refreshComponents();
    }

    /**
     * Custom cell renderer for JTable to allow colors and styles not directly available in a JTable
     */
    private class CertificateTableCellRenderer extends DefaultTableCellRenderer {
        final int STATUS_NORMAL = 0;
        final int STATUS_WARNING = 1;
        final int STATUS_TRUSTED = 2;

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
                    label.setText(cert.isTrusted()? Constants.TRUSTED_PUBLISHER:Constants.UNTRUSTED_PUBLISHER);
                    return stylizeLabel(!cert.isTrusted()? STATUS_WARNING:STATUS_TRUSTED, label, isSelected);
                case VALID_FROM:
                    boolean futureExpiration = cert.getValidFromDate().compareTo(now.getTime()) > 0;
                    return stylizeLabel(futureExpiration? STATUS_WARNING:STATUS_NORMAL, label, isSelected, "future inception");
                case VALID_TO:
                    boolean expiresSoon = cert.getValidToDate().compareTo(warn.getTime()) < 0;
                    boolean expired = cert.getValidToDate().compareTo(now.getTime()) < 0;
                    String reason = expiresSoon? "expired":"expires soon";
                    return stylizeLabel(expiresSoon || expired? STATUS_WARNING:STATUS_NORMAL, label, isSelected, reason);
                default:
                    return stylizeLabel(STATUS_NORMAL, label, isSelected);
            }
        }

        private JLabel stylizeLabel(int statusCode, JLabel label, boolean isSelected) {
            return stylizeLabel(statusCode, label, isSelected, null);
        }

        private JLabel stylizeLabel(int statusCode, JLabel label, boolean isSelected, String reason) {
            label.setIcon(null);

            int fontWeight;
            Color foreground;

            switch(statusCode) {

                case STATUS_WARNING:
                    foreground = Constants.WARNING_COLOR;
                    fontWeight = Font.BOLD;
                    break;
                case STATUS_TRUSTED:
                    foreground = Constants.TRUSTED_COLOR;
                    fontWeight = Font.PLAIN;
                    break;
                case STATUS_NORMAL:
                default:
                    foreground = defaultForeground;
                    fontWeight = Font.PLAIN;
            }

            label.setFont(label.getFont().deriveFont(fontWeight));
            label.setForeground(isSelected? defaultSelectedForeground:foreground);
            if (statusCode == STATUS_WARNING && reason != null) {
                label.setText(label.getText() + " (" + reason + ")");
            }
            return label;
        }
    }
}
