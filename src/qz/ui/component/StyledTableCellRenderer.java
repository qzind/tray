package qz.ui.component;

import qz.common.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class StyledTableCellRenderer extends DefaultTableCellRenderer {

    protected Color defaultForeground;
    protected Color defaultSelectedForeground;

    final int STATUS_NORMAL = 0;
    final int STATUS_WARNING = 1;
    final int STATUS_TRUSTED = 2;

    public StyledTableCellRenderer() {
        refresh();
    }

    public void refresh() {
        defaultForeground = UIManager.getDefaults().getColor("Table.foreground");
        defaultSelectedForeground = UIManager.getDefaults().getColor("Table.selectionForeground");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
    }

    protected JLabel stylizeLabel(int statusCode, JLabel label, boolean isSelected) {
        return stylizeLabel(statusCode, label, isSelected, null);
    }

    protected JLabel stylizeLabel(int statusCode, JLabel label, boolean isSelected, String reason) {
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
