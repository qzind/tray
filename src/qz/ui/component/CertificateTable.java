package qz.ui.component;

import qz.auth.Certificate;
import qz.common.Constants;
import qz.ui.Themeable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.function.Function;

/**
 * Created by Tres on 2/22/2015.
 * Displays Certificate information in a JTable
 */
public class CertificateTable extends DisplayTable implements Themeable {
    private Certificate cert;

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static DateTimeFormatter dateParse = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Instant warn;
    private Instant now;
    private boolean useLocalTimezoneValidTo = false;
    private boolean useLocalTimezoneValidFrom = false;

    enum CertificateField {
        ORGANIZATION("Organization", (Certificate cert) -> cert.getOrganization()),
        COMMON_NAME("Common Name", (Certificate cert) -> cert.getCommonName()),
        TRUSTED("Trusted", (Certificate cert) -> cert.isTrusted()),
        VALID_FROM("Valid From", (Certificate cert) -> cert.getValidFrom()),
        VALID_TO("Valid To", (Certificate cert) -> cert.getValidTo()),
        FINGERPRINT("Fingerprint", (Certificate cert) -> cert.getFingerprint());

        String description;
        Function<Certificate, Object> getter;

        CertificateField(String description, Function<Certificate, Object> getter) {
            this.description = description;
            this.getter = getter;
        }

        public String getValue(Certificate cert, TimeZone timeZone) {
            String certFieldValue = getter.apply(cert).toString();
            switch(this) {
                case VALID_FROM:
                case VALID_TO:
                    if (certFieldValue.equals("Not Provided")) return certFieldValue;
                    ZonedDateTime utcTime = LocalDateTime.from(dateParse.parse(certFieldValue)).atZone(ZoneOffset.UTC); // Parse the date string as UTC (Z/GMT)
                    ZonedDateTime zonedTime = Instant.from(utcTime).atZone(timeZone.toZoneId()); // Shift to the new timezone
                    return dateParse.format(zonedTime) + " " + timeZone.getDisplayName(false, TimeZone.SHORT); // Append a short timezone name e.g. "EST"
                default:
                    return certFieldValue;
            }
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

    public CertificateTable(IconCache iconCache) {
        super(iconCache);
        setDefaultRenderer(Object.class, new CertificateTableCellRenderer());
        addMouseListener(new MouseAdapter() {
            int lastRow = -1;
            int lastCol = -1;

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                JTable target = (JTable)e.getSource();
                int row = target.getSelectedRow();
                int col = target.getSelectedColumn();
                // Only trigger after the cell is click AND highlighted. This make copying text easier.
                if (row == lastRow && col == lastCol) {
                    CertificateField rowKey = (CertificateField)target.getValueAt(row, 0);
                    if (rowKey == CertificateField.VALID_TO) {
                        useLocalTimezoneValidTo = !useLocalTimezoneValidTo;
                        refreshComponents();
                        changeSelection(row, col, false, false);
                    } else if (rowKey == CertificateField.VALID_FROM) {
                        useLocalTimezoneValidFrom = !useLocalTimezoneValidFrom;
                        refreshComponents();
                        changeSelection(row, col, false, false);
                    }
                }
                lastRow = row;
                lastCol = col;
            }
        });

    }

    public void setCertificate(Certificate cert) {
        this.cert = cert;
        refreshComponents();
    }

    @Override
    public void refreshComponents() {
        if (cert == null) {
            removeRows();
            repaint();
            return;
        }

        now = Instant.now();
        warn = now.plus(Constants.EXPIRY_WARN, ChronoUnit.DAYS);

        removeRows();

        // First Column
        for(CertificateField field : CertificateField.values()) {
            TimeZone timeZone = null;
            switch(field){
                case TRUSTED:
                    if (!Certificate.isTrustBuiltIn()) continue; // Remove "Verified by" text; uncertain in strict mode
                    break;
                case VALID_TO:
                    timeZone = useLocalTimezoneValidTo ? TimeZone.getDefault() : UTC_TIME_ZONE;
                    break;
                case VALID_FROM:
                    timeZone = useLocalTimezoneValidFrom ? TimeZone.getDefault() : UTC_TIME_ZONE;
                    break;
                default:
                    break;
            }
            model.addRow(new Object[] {field, field.getValue(cert, timeZone)});
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
                switch((CertificateField)value) {
                    case VALID_FROM:
                        boolean futureExpiration = cert.getValidFromDate().isAfter(now);
                        label = stylizeLabel(futureExpiration? STATUS_WARNING:STATUS_NORMAL, label, isSelected, "future inception");
                        break;
                    case VALID_TO:
                        boolean expiresSoon = cert.getValidToDate().isBefore(warn);
                        boolean expired = cert.getValidToDate().isBefore(now);
                        String reason = expired? "expired":(expiresSoon? "expires soon":null);

                        label = stylizeLabel(expiresSoon || expired? STATUS_WARNING:STATUS_NORMAL, label, isSelected, reason);
                        break;
                    default:
                        label = stylizeLabel(STATUS_NORMAL, label, isSelected);
                        break;
                }
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
                    if(cert.isValid()) {
                        if(cert.isSponsored() && Certificate.isTrustBuiltIn()) {
                            // isTrustBuiltIn: Assume only QZ sponsors
                            label.setText(Constants.SPONSORED_CERT);
                        } else {
                            label.setText(Constants.TRUSTED_CERT);
                        }
                    } else {
                        label.setText(Constants.UNTRUSTED_CERT);
                    }
                    return stylizeLabel(!cert.isValid()? STATUS_WARNING:STATUS_TRUSTED, label, isSelected);
                case VALID_FROM:
                    boolean futureExpiration = cert.getValidFromDate().isAfter(now);
                    return stylizeLabel(futureExpiration? STATUS_WARNING:STATUS_NORMAL, label, isSelected);
                case VALID_TO:
                    boolean expiresSoon = cert.getValidToDate().isBefore(warn);
                    boolean expired = cert.getValidToDate().isBefore(now);
                    return stylizeLabel(expiresSoon || expired? STATUS_WARNING:STATUS_NORMAL, label, isSelected);
                default:
                    return stylizeLabel(STATUS_NORMAL, label, isSelected);
            }
        }
    }
}
