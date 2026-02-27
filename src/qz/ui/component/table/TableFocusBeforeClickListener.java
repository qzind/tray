package qz.ui.component.table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class TableFocusBeforeClickListener<T> extends MouseAdapter {
    private final Point lastClick = new Point(-1, -1);
    private final Class<T> type;
    private final Consumer<T> action;

    public TableFocusBeforeClickListener(Class<T> type, Consumer<T> action) {
        this.type = type;
        this.action = action;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        JTable table = (JTable) e.getSource();
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        Point current = new Point(col, row);

        // 1. Check if the cell was already focused
        if (lastClick.equals(current) && row != -1) {
            Object value = table.getValueAt(row, 0); // Assumes column 0 holds the object

            if (type.isInstance(value)) {
                // 2. Run the implementation logic
                action.accept(type.cast(value));

                // 3. Definition handles the selection restoration automatically
                table.changeSelection(row, col, false, false);
            }
        }

        lastClick.setLocation(current);
    }
}