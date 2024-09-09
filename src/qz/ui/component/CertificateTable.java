package qz.ui.component;

import org.joor.Reflect;
import qz.auth.Certificate;
import qz.common.Constants;
import qz.ui.Themeable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

/**
 * Created by Tres on 2/22/2015.
 * Displays Certificate information in a JTable
 */
public class CertificateTable extends DisplayTable implements Themeable {
    private Certificate cert;

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private Instant warn;
    private Instant now;
    private boolean useLocalTimezone = false;

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
                    Certificate.Field rowKey = (Certificate.Field)target.getValueAt(row, 0);
                    if (rowKey == Certificate.Field.VALID_TO || rowKey == Certificate.Field.VALID_FROM) {
                        useLocalTimezone = !useLocalTimezone;
                        refreshComponents();
                        row = -1;
                        col = -1;
                    }
                }
                lastRow = row;
                lastCol = col;
            }

            public void mouseClicked(MouseEvent e) {
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
        for(Certificate.Field field : Certificate.Field.displayFields) {
            if(field.equals(Certificate.Field.TRUSTED) && !Certificate.isTrustBuiltIn()) {
                continue; // Remove "Verified by" text; uncertain in strict mode
            }
            TimeZone timeZone = useLocalTimezone ? TimeZone.getDefault() : UTC_TIME_ZONE;
            model.addRow(new Object[] {field, cert.get(field, timeZone)});
        }

        repaint();
    }

    @Override
    public void refresh() {
        refreshComponents();
        ((StyledTableCellRenderer)getDefaultRenderer(Object.class)).refresh();
    }

    public void autoSize() {
        super.autoSize(Certificate.Field.displayFields.length, 2);
    }

    /** Custom cell renderer for JTable to allow colors and styles not directly available in a JTable */
    private class CertificateTableCellRenderer extends StyledTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            // First Column
            if (value instanceof Certificate.Field) {
                switch((Certificate.Field)value) {
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

            Certificate.Field field = (Certificate.Field)table.getValueAt(row, col - 1);
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
