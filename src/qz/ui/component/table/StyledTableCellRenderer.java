package qz.ui.component.table;

import qz.ui.component.IconCache;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

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

    protected JLabel stylizeLabel(FieldStyle style, JLabel label, boolean isSelected) {
        label.setIcon(null);
        label.setFont(label.getFont().deriveFont(style.isBold() ? Font.BOLD : Font.PLAIN));
        label.setForeground(isSelected? defaultSelectedForeground:style.getColor(defaultForeground));
        return label;
    }

    protected JLabel stylizeIcon(IconCache iconCache, IconCache.Icon icon, Object unused, JLabel label, boolean isSelected) {
        if (iconCache != null) {
            label.setIcon(iconCache.getIcon(icon));
        }
        return label;
    }

}
