package org.GeoRaptor.tools;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

public class TableSortIndicator {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private JTable fTable;
    private Icon fUpIcon;
    private Icon fDownIcon;

    private SortBy fCurrentSort;
    private SortBy fNoSort = new SortBy();
    private SortBy fTargetSort;

    private final SortOrder fDEFAULT_SORT_ORDER = SortOrder.ASCENDING;

    public TableSortIndicator(JTable aTable, Icon aUpIcon, Icon aDownIcon) {
        fTable = aTable;
        fUpIcon = aUpIcon;
        fDownIcon = aDownIcon;
        fCurrentSort = fNoSort;

        initHeaderClickListener();
        initHeaderRenderers();
        assert getRenderer(0) != null : "Ctor - renderer 0 is null.";
    }

    public SortBy getSortBy() {
        return fCurrentSort;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    void setSortBy(SortBy aTargetSort) {
        validateIdx(aTargetSort.getColumn());
        initHeaderRenderers();
        assert getRenderer(0) != null : "setSortBy - renderer 0 is null.";
        fTargetSort = aTargetSort;

        if (fCurrentSort == fNoSort) {
            setInitialHeader();
        } else if (fCurrentSort.getColumn() == fTargetSort.getColumn()
                && fCurrentSort.getOrder() != fTargetSort.getOrder()) {
            toggleIcon();
        } else {
            updateTwoHeaders();
        }

        SortBy oldSort = fCurrentSort;
        synchCurrentSortWithSelectedSort();
        notifyAndPaint(oldSort, fCurrentSort);
    }

    private boolean isValidColumnIdx(int aColumnIdx) {
        return 0 <= aColumnIdx && aColumnIdx <= fTable.getColumnCount() - 1;
    }

    private void validateIdx(int aSelectedIdx) {
        if (!isValidColumnIdx(aSelectedIdx)) {
            throw new IllegalArgumentException("Column index is out of range: " + aSelectedIdx);
        }
    }

    private void initHeaderRenderers() {
        for (int idx = 0; idx < fTable.getColumnCount(); ++idx) {
            TableColumn column = fTable.getColumnModel().getColumn(idx);
            column.setHeaderRenderer(new Renderer(fTable.getTableHeader()));
            assert column.getHeaderRenderer() != null : "Header Renderer is null";
        }
    }

    private Renderer getRenderer(int aColumnIdx) {
        TableColumn column = fTable.getColumnModel().getColumn(aColumnIdx);
        return (Renderer) column.getHeaderRenderer();
    }

    private void initHeaderClickListener() {
        fTable.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int selectedIdx = fTable.getColumnModel().getColumnIndexAtX(event.getX());
                processClick(selectedIdx);
            }
        });
    }

    private void processClick(int aSelectedIdx) {
        validateIdx(aSelectedIdx);

        if (fCurrentSort.getColumn() == aSelectedIdx)
            fTargetSort = new SortBy(fCurrentSort.toggle(), aSelectedIdx);
        else
            fTargetSort = new SortBy(fDEFAULT_SORT_ORDER, aSelectedIdx);

        if (fCurrentSort == fNoSort)
            setInitialHeader();

        if (fCurrentSort.getColumn() == fTargetSort.getColumn())
            toggleIcon();
        else
            updateTwoHeaders();

        SortBy oldSort = fCurrentSort;
        synchCurrentSortWithSelectedSort();
        notifyAndPaint(oldSort, fCurrentSort);
    }

    private void notifyAndPaint(SortBy oldSort, SortBy newSort) {
        pcs.firePropertyChange("sortBy", oldSort, newSort);
        fTable.getTableHeader().resizeAndRepaint();
    }

    private void setInitialHeader() {
        if (fTargetSort.getOrder() == SortOrder.DESCENDING)
            getRenderer(fTargetSort.getColumn()).setIcon(fDownIcon);
        else if (fTargetSort.getOrder() == SortOrder.ASCENDING)
            getRenderer(fTargetSort.getColumn()).setIcon(fUpIcon);
        else
            getRenderer(fTargetSort.getColumn()).setIcon(null);
    }

    private void toggleIcon() {
        Renderer renderer = getRenderer(fCurrentSort.getColumn());
        if (fCurrentSort.getOrder() == SortOrder.ASCENDING)
            renderer.setIcon(fDownIcon);
        else if (fCurrentSort.getOrder() == SortOrder.DESCENDING)
            renderer.setIcon(fUpIcon);
        else
            renderer.setIcon(null);
    }

    private void updateTwoHeaders() {
        if (fCurrentSort.getColumn() != fCurrentSort.NO_SELECTED_COLUMN)
            getRenderer(fCurrentSort.getColumn()).setIcon(null);
        if (fTargetSort.getColumn() != fTargetSort.NO_SELECTED_COLUMN)
            getRenderer(fTargetSort.getColumn()).setIcon(fUpIcon);
    }

    private void synchCurrentSortWithSelectedSort() {
        fCurrentSort = fTargetSort;
    }

    private final class Renderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 6169119491073832042L;
        private JTableHeader fTableHeader;

        Renderer(JTableHeader aTableHeader) {
            setHorizontalAlignment(JLabel.CENTER);
            setForeground(aTableHeader.getForeground());
            setBackground(aTableHeader.getBackground());
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            fTableHeader = aTableHeader;
        }

        public Component getTableCellRendererComponent(
                JTable aTable,
                Object aValue,
                boolean aIsSelected,
                boolean aHasFocus,
                int aRowIdx,
                int aColumnIdx) {
            setText((aValue == null) ? "" : aValue.toString());
            setFont(fTableHeader.getFont());
            return this;
        }
    }
}