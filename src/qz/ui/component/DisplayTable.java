package qz.ui.component;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Displays information in a JTable
 */
public class DisplayTable extends JTable {

    protected DefaultTableModel model;

    protected IconCache iconCache;

    public DisplayTable(IconCache iconCache) {
        super();
        initComponents();

        this.iconCache = iconCache;
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

        int normalWidth = (int)getPreferredScrollableViewportSize().getWidth();
        int autoHeight = (int)getPreferredSize().getHeight();
        setPreferredScrollableViewportSize(new Dimension(normalWidth, autoHeight));
        setFillsViewportHeight(true);
        refreshComponents();
    }

}
