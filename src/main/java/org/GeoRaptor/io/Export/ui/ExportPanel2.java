package org.GeoRaptor.io.Export.ui;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;

import org.GeoRaptor.Constants;
import org.GeoRaptor.io.Export.DBaseWriter;
import org.GeoRaptor.tools.Strings;


public class ExportPanel2 extends JPanel {
    
	private static final long serialVersionUID = -3346734482669703038L;

	private  ExportColumnModel             ecm = null;
    protected Constants.EXPORT_TYPE exportType = Constants.EXPORT_TYPE.GML;

    private JPanel               pnlAttributes = new JPanel();
    private XYLayout             xyLayoutPanel = new XYLayout();
    private XYLayout        xyLayoutAttributes = new XYLayout();
    private JTable               tblAttributes = new JTable();
    private JScrollPane   scrollPaneAttributes = null;    
    protected JLabel                lblFlavour = new JLabel();
    private JButton               btnSelectAll = new JButton();
    private JButton              btnSelectNone = new JButton();
    private JLabel                   lblSelect = new JLabel();
    private JComboBox<String> cmbAttributeFlavour = new JComboBox<>();

    public ExportPanel2() {
        super();
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ExportPanel2(boolean b) {
        super(b);
    }

    public ExportPanel2(LayoutManager layoutManager) {
        super(layoutManager);
    }

    public ExportPanel2(LayoutManager layoutManager, boolean b) {
        super(layoutManager, b);
    }

    private void jbInit() throws Exception {
        this.setLayout(xyLayoutPanel);
        
        pnlAttributes.setLayout(xyLayoutAttributes);
        pnlAttributes.setBorder(BorderFactory.createTitledBorder("Select Attributes For Export"));
        
        tblAttributes.setMaximumSize(new Dimension(355, 233));
        tblAttributes.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        tblAttributes.setPreferredScrollableViewportSize(new Dimension(355,1000));
        this.ecm = new ExportColumnModel();
        tblAttributes.setModel(ecm);

        scrollPaneAttributes = new JScrollPane(tblAttributes);    
        
        lblFlavour.setText("Flavour:");
        lblFlavour.setPreferredSize(new Dimension(40, 20));
        lblFlavour.setMaximumSize(new Dimension(40, 20));
        lblFlavour.setMinimumSize(new Dimension(40, 20));

        btnSelectAll.setText("All");
        btnSelectAll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnSelectAll_actionPerformed(e);
                }
            });
        btnSelectNone.setText("None");
        btnSelectNone.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnSelectNone_actionPerformed(e);
                }
            });
        lblSelect.setText("Select:");

        pnlAttributes.add(cmbAttributeFlavour, new XYConstraints(50, 1, 155, 20));
        pnlAttributes.add(lblSelect, new XYConstraints(210, 1, 34, 14));
        pnlAttributes.add(btnSelectNone, new XYConstraints(300, -4, 75, 21));
        pnlAttributes.add(btnSelectAll, new XYConstraints(250, -4, 45, 21));
        pnlAttributes.add(scrollPaneAttributes, new XYConstraints(0, 26, 375, 235));
        pnlAttributes.add(lblFlavour, new XYConstraints(5, 1, 40, 20));

        this.add(pnlAttributes, new XYConstraints(5, 5, 390, 290));
    }
    
    public void setColumnWidths() {
       this.tblAttributes.getColumnModel().getColumn(2).setPreferredWidth(50);
    }
  
    public String getAttributeFlavour() 
    {
        return this.cmbAttributeFlavour.getSelectedIndex()==-1
               ? this.cmbAttributeFlavour.getItemAt(0)
               : (String)this.cmbAttributeFlavour.getSelectedItem();
    }

    public void setExportType(Constants.EXPORT_TYPE _exportType) {
        this.exportType = _exportType;
        setAttributeFlavour();
    }
  
    public void setAttributeFlavour() {
        switch (this.exportType) {
        case TAB :
        case SHP : this.cmbAttributeFlavour.setModel(DBaseWriter.getXBaseTypesAsCombo());
                   break;
        case KML : this.cmbAttributeFlavour.setModel(Constants.getXMLAttributeFlavourAsCombo(true)); 
                   this.cmbAttributeFlavour.setEnabled(false);
                   break;
        default  : 
        case GML : this.cmbAttributeFlavour.setModel(Constants.getXMLAttributeFlavourAsCombo(false)); 
                   break;
        }
        this.cmbAttributeFlavour.setSelectedIndex(0);
    }
    
    public void loadAttributes(LinkedHashMap<String, String> _columns,
                               String _columnName)
    {
        try {
            if ( _columns != null ) {
                ((ExportColumnModel)this.tblAttributes.getModel()).loadAttributes(_columns,_columnName);
            }
        } catch (Exception e) {
        }
    }
    
    public String getExportableColumns() {
        return ((ExportColumnModel)this.tblAttributes.getModel()).getSelectedColumnNames(',');
    }

    public boolean isAttributeSelected() {
        return this.ecm.getSelectedAttributeCount()!=0;      
    }
    
    private void btnSelectAll_actionPerformed(ActionEvent e) {
        ecm.setSelected(true);
    }

    private void btnSelectNone_actionPerformed(ActionEvent e) {
        ecm.setSelected(false);
        /* 
        if ( exportType == Constants.EXPORT_TYPE.SHP ||
             exportType == Constants.EXPORT_TYPE.TAB ) {
            // Must have at least one attribute selected for shapefiles
            ecm.setValueAt(Boolean.valueOf(true), 0, ecm.OracleSelectedColumn);
        }
        */
    }

    public static enum columns { ORACLE_COLUMN_NAME, ORACLE_DATA_TYPE, IS_EXPORT };

  class ExportColumnModel extends AbstractTableModel 
  {
      /**
	 * 
	 */
	private static final long serialVersionUID = 7289861505514172469L;

	String[] headers = new String [] { "Column",
                                         "Data Type",
                                         "Export?" };
      
      public final int OracleNameColumn = 0;
      public final int OracleDataTypeColumn = 1;
      public final int OracleSelectedColumn = 2;
      
      @SuppressWarnings("rawtypes")
      Class[] types =new Class[] { String.class,
                                   String.class,
                                   Boolean.class };

      boolean[] canEdit = new boolean[] { false, false, true };

      private int cacheCapacity = 255;
      Vector<Object[]> cache; // will hold table column metadata

      public ExportColumnModel() {
          cache = new Vector<Object[]>(cacheCapacity);
      }
  
      public String getSelectedColumnNames(char _delimiter) {
          StringBuffer sb = new StringBuffer();
          ListIterator<Object[]> cacheIter = cache.listIterator();
          while (cacheIter.hasNext() ) {
              Object[] obj = cacheIter.next();
              if ( ! (Boolean)obj[2] )
                  continue;
              sb.append((String)obj[0] + _delimiter);
          }
          return Strings.trimEnd(sb.toString(),_delimiter);
      }

      public List<String> getSelectedColumns() {
        List<String> returnList = new ArrayList<String>(cacheCapacity);
        ListIterator<Object[]> cacheIter = cache.listIterator();
        while (cacheIter.hasNext() ) {
            Object [] obj = cacheIter.next();
            if ( ! (Boolean)obj[OracleSelectedColumn] )
                continue;
            returnList.add((String)obj[0]);
        }
        return returnList;
      }
      
      public Vector<Object[]> getLoadColumnData(String[] _nameSet) {
          Vector<Object[]> returnVector = new Vector<Object[]>(cacheCapacity);
          ListIterator<Object[]> cacheIter = cache.listIterator();
          while (cacheIter.hasNext() ) {
              Object [] obj = cacheIter.next();
              if ( ! (Boolean)obj[OracleSelectedColumn] )
                  continue;
              if ( ( _nameSet == null ) || 
                   ( inNameSet(obj[0].toString(), _nameSet ) ) )
                  returnVector.addElement(obj);
          }
          return returnVector;
      }
       
      public boolean inNameSet(String _name, String[] _nameSet) 
      {
          if (_nameSet == null || _nameSet.length == 0 || Strings.isEmpty(_name) )
              return false;
          for (int i=0;i<_nameSet.length;i++) {
              if (_name.equalsIgnoreCase(_nameSet[i]))
                  return true;
          }
          return false;
      }
                                                                                                      
      public String getColumnName(int i) {
          return headers[i];
      }

      public int getColumnPosition(ExportPanel2.columns _col) 
      {
          int position = -1;
          switch (_col) {
              case ORACLE_COLUMN_NAME : { position = OracleNameColumn; break; }
              case ORACLE_DATA_TYPE   : { position = OracleDataTypeColumn; break; }
              case IS_EXPORT          : { position = OracleSelectedColumn; break; }

          }
          return position;
      }
  
      public int getColumnCount() {
          return headers.length;
      }
  
      public int getRowCount() {
          if (this.cache == null) {
              return 0;
          } else {
              return this.cache.size();
          }
      }

      public void addRow(Object[] row) {
          cache.addElement(row);
      }
  
      @SuppressWarnings("unused")
	  private boolean valueExists(String name) 
      {
          boolean exists = false;
          ListIterator<Object[]> chcIter = cache.listIterator();
          while (chcIter.hasNext()) {
              Object[] o = chcIter.next();
              if ( ((String)o[OracleNameColumn]).equalsIgnoreCase(name) ) {
                  exists = true;
                  break;
              }
          }
          return exists;
      }
      
      /**
       * JTable uses this method to determine the default renderer/
       * editor for each cell.
      **/
      public Class<?> getColumnClass(int col) {
          return types[col];
      }
  
      public String[] getColumnNames() {
          return headers;
      }
      
      public Object getValueAt(int row, int col) {
          return cache.elementAt(row)[col];
      }
      
      public void setValueAt(Object value, int row, int col) 
      {
          if ( ! canEdit[col] )
              return;
          Object[] obj = cache.elementAt(row);
          obj[col] = value;
          cache.setElementAt(obj,row);
          this.fireTableRowsUpdated(row,col);
      }

      public void setSelected(boolean _selected) {
          for (int row=0; row < this.getRowCount(); row++ ) {
              Object[] obj = cache.elementAt(row);
              obj[OracleSelectedColumn] = Boolean.valueOf(_selected);
              cache.setElementAt(obj,row);
          }
          this.fireTableRowsUpdated(0, this.getRowCount());
      }

      public int getSelectedAttributeCount() {
          int aCount = 0;
          for (int row=0; row < this.getRowCount(); row++ ) {
              Object[] obj = cache.elementAt(row);
              if ( (Boolean)obj[OracleSelectedColumn] )
                  aCount++;
          }
          return aCount;
      }

      public boolean isCellEditable(int row, int col) {
          return canEdit[col];
      }

      public void loadAttributes(LinkedHashMap<String,String> _columns,
                                 String                       _columnName) 
      throws Exception
      {
          String geoColumnName = Strings.isEmpty(_columnName) ? "" : _columnName; 
          this.clearModel();
          try 
          {
              Iterator<String> keyIter = _columns.keySet().iterator();
              String key = "";
              while (keyIter.hasNext()) {
                  key = keyIter.next();  // Get the key.
                  if ( geoColumnName.equalsIgnoreCase(key.replace("\"","")) )
                      continue;
                  Object[] record = new Object[3];
                  record[OracleNameColumn] = key;
                  record[OracleDataTypeColumn] = _columns.get(key);
                  record[OracleSelectedColumn] = Boolean.valueOf(true);
                  // add Metadata to internal cache
                  //
                  this.addRow(record);
              }
              this.fireTableRowsInserted(0,cache.size()-1); // notify everyone that we have a new table.
          } catch (Exception e) {
              this.clearModel();
              throw new Exception("Error loading attributes - " + e.getMessage());
          }
      }
      
      public void clearModel() {
          if ( cache == null ) {
              cache = new Vector<Object[]>(cacheCapacity);
          } else {
              cache.clear();
          }
          this.fireTableDataChanged();
      }
  }  // Class

}
