package qz.ui.component.table;

import qz.ui.component.IconCache;
import qz.utils.SystemUtilities;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.List;

/**
 * Displays information in a JTable
 */
public class FieldValueTable extends JTable {
    public static List<String> COLUMNS = Arrays.asList("Field", "Value");
    protected DefaultTableModel model;

    protected IconCache iconCache;

    public FieldValueTable(IconCache iconCache) {
        super();
        initComponents();

        this.iconCache = iconCache;
    }

    private void initComponents() {
        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int x, int y) { return false; }
        };
        COLUMNS.forEach(column -> model.addColumn(column));

        // Fix Linux row height
        int origHeight = getRowHeight();
        if(SystemUtilities.getWindowScaleFactor() > 1) {
            setRowHeight((int)(origHeight * SystemUtilities.getWindowScaleFactor()));
        }

        getTableHeader().setReorderingAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowSelectionAllowed(true);

        setModel(model);
    }

    public void refreshComponents() {
        repaint();
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
    public void autoSize(int rows, int columns) {
        removeRows();
        for(int row = 0; row < rows; row++) {
            model.addRow(new Object[columns]);
        }

        setPreferredScrollableViewportSize(
                SystemUtilities.scaleWindowDimension(
                        getPreferredScrollableViewportSize().getWidth(),
                        getPreferredSize().getHeight())
        );
        setFillsViewportHeight(true);
        refreshComponents();
    }

}
