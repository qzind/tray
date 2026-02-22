package qz.ui.component.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static qz.ui.component.table.CellStyle.*;

public class StyledTableCellRenderer extends DefaultTableCellRenderer {

    protected Color defaultForeground;
    protected Color defaultSelectedForeground;

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

    protected JLabel stylizeLabel(CellStyle style, JLabel label, boolean isSelected) {
        return stylizeLabel(style, label, isSelected, null);
    }

    protected JLabel stylizeLabel(CellStyle style, JLabel label, boolean isSelected, String reason) {
        label.setIcon(null);

        int fontWeight = style.isBold() ? Font.BOLD : Font.PLAIN;
        Color foreground = style.getColor(defaultForeground);

        label.setFont(label.getFont().deriveFont(fontWeight));
        label.setForeground(isSelected? defaultSelectedForeground:foreground);
        if (style == WARNING && reason != null) {
            label.setText(label.getText() + " (" + reason + ")");
        }

        return label;
    }

}
