import javax.swing.*;
import javax.swing.table.AbstractTableModel;

public class MyJTable extends JTable {

    private static int cellSize;

    public MyJTable(AbstractTableModel mapTableModel) {
        super(mapTableModel);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        cellSize = Math.min(getHeight() / getRowCount(), getWidth() / getColumnCount());
        if (cellSize < 1) {
            cellSize = 1;
        }
        setRowHeight(cellSize);
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setMaxWidth(cellSize);
            getColumnModel().getColumn(i).setMinWidth(cellSize);
        }

        revalidate();
        repaint();
    }
}
