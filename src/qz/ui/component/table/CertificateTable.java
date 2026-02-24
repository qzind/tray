package qz.ui.component.table;

import qz.auth.Certificate;
import qz.ui.Themeable;
import qz.ui.component.IconCache;

import javax.swing.*;
import java.awt.*;

import static qz.ui.component.table.CertificateField.*;
import static qz.ui.component.table.FieldStyle.*;

/**
 * Created by Tres on 2/22/2015.
 * Displays Certificate information in a JTable
 */
public class CertificateTable extends FieldValueTable implements Themeable {
    private Certificate cert;

    public CertificateTable(IconCache iconCache) {
        super(iconCache);
        setDefaultRenderer(Object.class, new CertificateTableCellRenderer());

        addMouseListener(new TableFocusBeforeClickListener<>(CertificateField.class, (certificateField) -> {
            if (certificateField.isDateField()) {
                toggleTimeZone();
                refreshComponents();
            }
        }));
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

        removeRows();

        for(Type type : Type.values()) {
            CertificateField field = new CertificateField(type, cert);
            model.addRow(new Object[] {field, field}); // we'll calculate the labels on render
        }

        repaint();
    }

    @Override
    public void refresh() {
        refreshComponents();
        ((StyledTableCellRenderer)getDefaultRenderer(Object.class)).refresh();
    }

    public void autoSize() {
        super.autoSize(Type.size(), 2);
    }

    /** Custom cell renderer for JTable to allow colors and styles not directly available in a JTable */
    private class CertificateTableCellRenderer extends StyledTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            if (cert != null && value instanceof CertificateField) {
                CertificateField field = (CertificateField)value;
                switch(col) {
                    case 0: // First Column - Certificate Field Label
                        label.setText(field.getLabel());
                        label = stylizeLabel(field.getStyle(), label, isSelected);
                        return stylizeIcon(iconCache, IconCache.Icon.FIELD_ICON, value, label, isSelected);
                    case 1: // Second Column - Certificate Field Value
                        label.setText(field.getValue());
                        return stylizeLabel(field.getStyle(), label, isSelected);
                }
            }
            return stylizeLabel(NORMAL, label, isSelected);
        }
    }
}
