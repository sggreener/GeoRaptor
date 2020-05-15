package org.GeoRaptor.tools;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Observable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

/** 
 * @class TableSortIndicator
 * Implements Up and Down arrow in Header for when user clicks in header indicating a sort
 * Idea and code originally sourced from: http://www.javapractices.com/topic/TopicAction.do?Id=161
 */
public class TableSortIndicator extends Observable {
    
    // PRIVATE //
    private JTable fTable;
    private Icon   fUpIcon;
    private Icon   fDownIcon;
    
    /**
    * The sort as currently displayed to the user, representing the end result of a 
    * previous user request.
    */
    private SortBy fCurrentSort;
    private SortBy fNoSort = new SortBy();
    
    /**
    * A new sort to be processed, whose origin is either a user preference or a 
    * a mouse click.
    *
    * Once fTargetSort is processed, fCurrentSort is assigned to fTargetSort.
    */
    private SortBy fTargetSort;
    
    private final SortOrder fDEFAULT_SORT_ORDER = SortOrder.ASCENDING;

    /**
      * Constructor.
      *  
      * @param aTable receives indication of table sort ; if it has any custom 
      * header renderers, they will be overwritten by this class.
      * @param aUpIcon placed in column header to indicate ascending sort.
      * @param aDownIcon placed in column header to indicate descending sort.
      */
    public TableSortIndicator(JTable aTable, Icon aUpIcon, Icon aDownIcon) 
    {
        fTable = aTable;
        fUpIcon = aUpIcon;
        fDownIcon = aDownIcon;
        fCurrentSort = fNoSort;
        
        initHeaderClickListener();
        initHeaderRenderers();
        assert getRenderer(0) != null : "Ctor - renderer 0 is null.";
      }
      
      /**
      * Return the identity of column having the primary sort, and the direction
      * of its sort.
      */
      public SortBy getSortBy(){
        return fCurrentSort;
      }
      
      /**
      * Change the sort programmatically, instead of through a user click.
      *
      * <P>If there is a user preference for sort, it is passed to this method.
      * Notifies listeners of the change to the sort.
      */
      void setSortBy( SortBy aTargetSort )
      {
        validateIdx( aTargetSort.getColumn() );
        initHeaderRenderers();
        assert getRenderer(0) != null : "setSortBy - renderer 0 is null.";
        fTargetSort = aTargetSort;
        if ( fCurrentSort == fNoSort ){
            setInitialHeader();
        } else if ( fCurrentSort.getColumn() == fTargetSort.getColumn() && 
                    fCurrentSort.getOrder() != fTargetSort.getOrder() ) {
            toggleIcon();
        } else {
            updateTwoHeaders();
        }
        synchCurrentSortWithSelectedSort();
        notifyAndPaint();
      }

      /**
      * Return true only if the index is in the range 0..N-1, where N is the 
      * number of columns in fTable.
      */
      private boolean isValidColumnIdx(int aColumnIdx) {
        return 0 <= aColumnIdx && aColumnIdx <= fTable.getColumnCount()-1 ;
      }

      private void validateIdx(int aSelectedIdx) {
        if ( ! isValidColumnIdx(aSelectedIdx) ) {
          throw new IllegalArgumentException("Column index is out of range: " + aSelectedIdx);
        }
      }
      
      /**
      * Called both upon construction and by {@link #setSortBy}.
      *  
      * If fireTableStructureChanged is called, then the original headers are lost, and 
      * this method must be called in order to restore them.
      */
      private void initHeaderRenderers()
      {
        /*
        * Attach a default renderer explicitly to all columns. This is a 
        * workaround for the unusual fact that TableColumn.getHeaderRenderer returns 
        * null in the default case; Sun did this as an optimization for tables with 
        * very large numbers of columns. As well, there is only one default renderer
        * instance which is reused by each column.
        * See http://developer.java.sun.com/developer/bugParade/bugs/4276838.html for 
        * further information.
        */
        for (int idx=0; idx < fTable.getColumnCount(); ++idx) {
            TableColumn column = fTable.getColumnModel().getColumn(idx);
            column.setHeaderRenderer( new Renderer(fTable.getTableHeader()) );
            assert column.getHeaderRenderer() != null : "Header Renderer is null";
        }
      }
      
      private Renderer getRenderer(int aColumnIdx) {
        TableColumn column = fTable.getColumnModel().getColumn(aColumnIdx);
        return (Renderer)column.getHeaderRenderer();
      }
      
      private void initHeaderClickListener() {
          fTable.getTableHeader().addMouseListener( new MouseAdapter() {
          public void mouseClicked(MouseEvent event) {
              int selectedIdx = fTable.getColumnModel().getColumnIndexAtX(event.getX());
              processClick( selectedIdx );
          }
        });
      }

      /**
      * Update the display of table headers to reflect a new sort, as indicated by a 
      * mouse click performed by the user on a column header.
      *
      * If <tt>aSelectedIdx</tt> is the column which already has the sort indicator, 
      * then toggle the indicator to its opposite state (up -> down, down -> up). 
      * If <tt>aSelectedIdx</tt> does not already display a sort indicator, then 
      * add a down indicator to it, and remove the indicator from the fCurrentSort 
      * column, if present.
      */
      private void processClick(int aSelectedIdx)
      {
        validateIdx( aSelectedIdx );
        
        if ( fCurrentSort.getColumn() == aSelectedIdx ) 
            fTargetSort = new SortBy(fCurrentSort.toggle(), aSelectedIdx);
        else
            fTargetSort = new SortBy(fDEFAULT_SORT_ORDER , aSelectedIdx);
        
        if ( fCurrentSort == fNoSort ) setInitialHeader(); 
        
        if ( fCurrentSort.getColumn() == fTargetSort.getColumn() ) 
            toggleIcon();
        else 
            updateTwoHeaders();
        
        synchCurrentSortWithSelectedSort();
        notifyAndPaint();
      }

      private void notifyAndPaint(){
        setChanged();
        notifyObservers();
        fTable.getTableHeader().resizeAndRepaint();
      }

      private void setInitialHeader()
      {
        if ( fTargetSort.getOrder() == SortOrder.DESCENDING )
            getRenderer( fTargetSort.getColumn() ).setIcon(fDownIcon);
        else if ( fTargetSort.getOrder() == SortOrder.ASCENDING ) 
            getRenderer( fTargetSort.getColumn() ).setIcon(fUpIcon);  
        else 
            getRenderer( fTargetSort.getColumn() ).setIcon(null);
        
      }

      /**
      * Flip the direction of the icon (up->down or down->up).
      */
      private void toggleIcon()
      {
        Renderer renderer = getRenderer(fCurrentSort.getColumn());
        if ( fCurrentSort.getOrder() == SortOrder.ASCENDING ) 
            renderer.setIcon(fDownIcon);
        else if ( fCurrentSort.getOrder() == SortOrder.DESCENDING ) 
            renderer.setIcon(fUpIcon);
        else
            renderer.setIcon(null);
      }
      
      /**
      * Change the fCurrentSort column to having no icon, and change the fTargetSort 
      * column to having a down icon.
      */
      private void updateTwoHeaders() {
        if ( fCurrentSort.getColumn() != fCurrentSort.NO_SELECTED_COLUMN ) 
            getRenderer(fCurrentSort.getColumn()).setIcon(null);
        if ( fTargetSort.getColumn() != fTargetSort.NO_SELECTED_COLUMN )   
            getRenderer(fTargetSort.getColumn()).setIcon(fUpIcon);
      }
      
      private void synchCurrentSortWithSelectedSort(){
        fCurrentSort = fTargetSort;
      }

      /**
      * Renders a column header with an icon.
      *
      * This class duplicates the default header behavior, but there does 
      * not seem to be any other option, since such an object does not seem
      * to be available from JTableHeader.
      */
      private final class Renderer extends DefaultTableCellRenderer 
      {
		private static final long serialVersionUID = 6169119491073832042L;
		
		Renderer(JTableHeader aTableHeader){
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
          int aColumnIdx
        ) {    
          setText((aValue == null) ? "" /* Consts.EMPTY_STRING*/ : aValue.toString());
          setFont(fTableHeader.getFont());
          return this;
        }
        private JTableHeader fTableHeader;
      }
}

