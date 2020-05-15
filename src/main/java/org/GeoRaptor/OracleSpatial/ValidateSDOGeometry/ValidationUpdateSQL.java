package org.GeoRaptor.OracleSpatial.ValidateSDOGeometry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;

import org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.ValidateSDOGeometry.ValidationTableModel;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;


public class ValidationUpdateSQL extends JDialog 
{

	private static final long serialVersionUID = 1263145744784066832L;

	private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.ValidateSDOGeometry.ValidationUpdateSQL");

    private ValidateSDOGeometry validationObject = null;
    
    private JPanel pnlAssnTable = new JPanel();
    private JScrollPane spAssnTable = new JScrollPane();
    private GridLayout glAssociationTable = new GridLayout();
    private JTable tblAssociations = new JTable();
    private JPopupMenu associationPopupMenu = new JPopupMenu();
    private AssociationTableModel atModel;
    private JButton btnApply = new JButton();
    private XYLayout xYLayout1 = new XYLayout();
    private JButton btnCancel = new JButton();
    private JScrollPane spUpdateSQL = new JScrollPane();
    private JTextArea taUpdateSQL = new JTextArea();
    private JButton btnCopySQL = new JButton();
    private JButton btnSaveAssns = new JButton();
    private JLabel lblMessages = new JLabel();
    private JButton btnResetAssns = new JButton();
    private JCheckBox cbUseSelection = new JCheckBox();

    public ValidationUpdateSQL(ValidateSDOGeometry _validationObject) {
        super();
        try {
            this.validationObject = _validationObject;  // If null throw exception
            jbInit();
            createMenu();
            this.tblAssociations.setAutoCreateRowSorter(true);
            this.atModel = new AssociationTableModel(  );
            this.atModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    taUpdateSQL.setText(getUpdateSQL());
                }
            });
            this.tblAssociations.setModel(this.atModel);
            this.addWindowListener(new WindowListener(){
              public void windowOpened(WindowEvent e) {
                  Tools.autoResizeColWidth(tblAssociations,null);
                  taUpdateSQL.setText(getUpdateSQL());
              }
              public void windowActivated(WindowEvent e) {}
              public void windowClosing(WindowEvent e) {
                  validationObject.geoRaptorPreferences.setDefaultAssociationXML(atModel.saveAssociations());
              }
              public void windowClosed(WindowEvent e) {}
              public void windowIconified(WindowEvent e) {}
              public void windowDeiconified(WindowEvent e) {}
              public void windowDeactivated(WindowEvent e) {}
          });
            this.btnResetAssns.setText(this.validationObject.propertyManager.getMsg("BUTTON_RESET"));
        } catch (Exception e) {
            LOGGER.warn("ValidationUpdateSQL error: " + e.getMessage());
            return;
        }
    }

    private void jbInit() throws Exception {
        this.setSize(new Dimension(610, 495));
        this.setResizable(false);
        this.setPreferredSize(new Dimension(610, 495));
        pnlAssnTable.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        pnlAssnTable.setLayout(xYLayout1);
        pnlAssnTable.setSize(new Dimension(598, 485));
        pnlAssnTable.setPreferredSize(new Dimension(598, 485));
        pnlAssnTable.setMinimumSize(new Dimension(598, 485));
        pnlAssnTable.setMaximumSize(new Dimension(598, 485));

        spUpdateSQL.setPreferredSize(new Dimension(590, 205));
        spUpdateSQL.setSize(new Dimension(590, 205));
        spUpdateSQL.setMinimumSize(new Dimension(590, 205));
        spUpdateSQL.setMaximumSize(new Dimension(5000, 500));
        spUpdateSQL.setAutoscrolls(true);
        taUpdateSQL.setFont(new Font("Courier New", 0, 10));
        taUpdateSQL.setPreferredSize(new Dimension(5000, 500));
        taUpdateSQL.setMinimumSize(new Dimension(585, 205));
        taUpdateSQL.setMaximumSize(new Dimension(5000, 500));
        spUpdateSQL.getViewport().add(taUpdateSQL, null);

        tblAssociations.setPreferredSize(new Dimension(585, 173));
        tblAssociations.setSize(new Dimension(585, 173));
        tblAssociations.setMaximumSize(new Dimension(1000, 500));
        tblAssociations.setMinimumSize(new Dimension(585, 173));
        tblAssociations.setPreferredScrollableViewportSize(new Dimension(1000,500));
        tblAssociations.setToolTipText(this.validationObject.propertyManager.getMsg("TT_ASSOCIATION"));
        tblAssociations.setAutoCreateRowSorter(true);
        this.tblAssociations.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.tblAssociations.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                tblAssociationsMouseClicked(evt);
            }
        });
        
        spAssnTable.setPreferredSize(new Dimension(588, 175));
        spAssnTable.setSize(new Dimension(588, 175));
        spAssnTable.getViewport().setLayout(glAssociationTable);
        spAssnTable.setMaximumSize(new Dimension(588, 175));
        spAssnTable.setMinimumSize(new Dimension(588, 175));
        spAssnTable.getViewport().add(tblAssociations, null);

        lblMessages.setForeground(new Color(255, 33, 33));
        lblMessages.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        btnResetAssns.setText("Reset Associations");
        btnResetAssns.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnResetAssns_actionPerformed(e);
                }
            });
        cbUseSelection.setText("Use Selected Geometries");
        cbUseSelection.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cbUseSelection_actionPerformed(e);
                }
            });
        btnApply.setText("Apply");
        btnApply.setSize(new Dimension(75, 20));
        btnApply.setPreferredSize(new Dimension(75, 20));
        btnApply.setMaximumSize(new Dimension(75, 20));
        btnApply.setMinimumSize(new Dimension(75, 20));
        btnApply.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnApply_actionPerformed(e);
                }
            });
        btnCancel.setText("Cancel");
        btnCancel.setPreferredSize(new Dimension(75, 20));
        btnCancel.setMinimumSize(new Dimension(75, 20));
        btnCancel.setMaximumSize(new Dimension(75, 20));
        btnCancel.setSize(new Dimension(75, 20));
        btnCancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnCancel_actionPerformed(e);
                }
            });
        
        btnCopySQL.setText("Copy SQL");
        btnCopySQL.setSize(new Dimension(120, 20));
        btnCopySQL.setPreferredSize(new Dimension(140, 20));
        btnCopySQL.setMinimumSize(new Dimension(140, 20));
        btnCopySQL.setMaximumSize(new Dimension(140, 20));
        btnCopySQL.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnCopySQL_actionPerformed(e);
                }
            });

        btnSaveAssns.setText("Save Associations");
        btnSaveAssns.setPreferredSize(new Dimension(130, 20));
        btnSaveAssns.setMaximumSize(new Dimension(130, 20));
        btnSaveAssns.setMinimumSize(new Dimension(130, 20));
        btnSaveAssns.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnSaveAssns_actionPerformed(e);
                }
            });
        
        pnlAssnTable.add(cbUseSelection,new XYConstraints(315, 403, 140, 20));
        pnlAssnTable.add(btnResetAssns, new XYConstraints(180, 428, 130, 20));
        pnlAssnTable.add(spUpdateSQL,   new XYConstraints(5, 3, 590, 195));
        pnlAssnTable.add(spAssnTable,   new XYConstraints(5, 203, 590, 175));
        pnlAssnTable.add(lblMessages,   new XYConstraints(5, 383, 590, 15));
        pnlAssnTable.add(btnApply,      new XYConstraints(5, 403, 75, 20));
        pnlAssnTable.add(btnSaveAssns,  new XYConstraints(180, 403, 130, 20));
        pnlAssnTable.add(btnCopySQL,    new XYConstraints(315, 428, 85, 20));
        pnlAssnTable.add(btnCancel,     new XYConstraints(520, 403, 75, 20));
        this.getContentPane().add(pnlAssnTable, null);
        this.pack();
    }

    private void tblAssociationsMouseClicked(MouseEvent evt) 
    {
        if ( SwingUtilities.isRightMouseButton(evt) ) 
        {
            // Is there anything under the mouse? 
            //
            if ( this.tblAssociations.getSelectedRowCount() == 0 ) 
            {
                int row = this.tblAssociations.rowAtPoint(evt.getPoint());
                if (row == -1) {
                    return;
                } 
            }
            // Show popup menu
            associationPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    private void createMenu() {
        AbstractAction deleteSelected = 
            new AbstractAction(this.validationObject.propertyManager.getMsg("DELETE_ASSOCIATION")) 
            {
				private static final long serialVersionUID = -1984329604939213055L;

				public void actionPerformed(ActionEvent e) {
                  int rows[] = tblAssociations.getSelectedRows();
                  if ( rows.length == 0 ) {
                    Point i = associationPopupMenu.getLocation();
                    int row = tblAssociations.rowAtPoint(i);
                    if ( row != -1 ) 
                        return;
                  } else if ( rows.length == 1 ) {
                      atModel.removeRow(tblAssociations.convertRowIndexToModel(rows[0]));
                  } else if ( rows.length > 1 ) {
                      atModel.removeSelectedRows(rows);
                  }
                }
            };
        associationPopupMenu.add(deleteSelected);
        
        AbstractAction addAssociation = 
            new AbstractAction(this.validationObject.propertyManager.getMsg("ADD_ASSOCIATION")) 
            {
				private static final long serialVersionUID = 1668340098831843801L;

				public void actionPerformed(ActionEvent e) {
                  addAssociation();
                }
            };
        associationPopupMenu.add(addAssociation);
    }
    
    public void loadAssociations(String _xml) {
        boolean OK = true;
        try {
            if (Strings.isEmpty(_xml) ) {
              this.atModel.loadAssociations(this.validationObject.geoRaptorPreferences.getDefaultAssociationXML());
            } else { 
              this.atModel.loadAssociations(_xml);
            }
        } catch (Exception e) {
        }
        if (this.atModel.getRowCount()==0 ) {
          try {
              this.lblMessages.setText("Load failed: resorting to defaults.");
              this.atModel.loadAssociations(this.validationObject.geoRaptorPreferences.getDefaultAssociationXML());
          } catch (Exception f) {
              this.lblMessages.setText("Loading defaults failed: " + f.getMessage());
              OK = false;
          }
        }
        if ( OK ) {
            Tools.autoResizeColWidth(this.tblAssociations,null);
        }
        this.taUpdateSQL.setText(this.getUpdateSQL());
    }
          
    private boolean CANCELLED = false;
    
    private void btnCancel_actionPerformed(ActionEvent e) {
        this.setCANCELLED(true);
        this.setVisible(false);
    }

    private void btnApply_actionPerformed(ActionEvent e) {
      this.setCANCELLED(false);
      this.setVisible(false);
    }

    private void addAssociation() {
        Object[] record = new Object[3];
        record[0] = "13xxxx";
        record[1] = "MDSYS.";
        record[2] = Boolean.valueOf(true);
        this.atModel.addRow(record);
    }

    private void setMessage(String _message) {
        this.lblMessages.setText(_message);      
    }
    
    private String functionReplace(String _function) {
        return Strings.isEmpty(_function) 
               ? _function 
               : _function.replaceAll(":G",this.validationObject.getSelectedColumnName())
                          .replaceAll(":D",this.validationObject.metadata.get(this.validationObject.getSchemaObjectColumn()).toDimArray())
                          .replaceAll(":T",this.validationObject.getTolerance(this.validationObject.getSchemaObjectColumn()))
                          .replaceAll(":S",this.validationObject.getSRID());
    }
    
    public String getUpdateSQL() 
    {
        /** TOBEDONE: If this.cbUseSelection selected then process JTable and get String which is a list of error numbers eg 13349,13356
         *  Then use this when processing the ticked rows in this.atModel
         **/
        this.setMessage("");
        String validationFunction = String.format("MDSYS.SDO_GEOM.VALIDATE_GEOMETRY(%s,%s)",
                                                  this.validationObject.getSelectedColumnName(),
                                                  this.validationObject.isDimInfo() 
                                                  ? this.validationObject.metadata.get(this.validationObject.getSchemaObjectColumn()).toDimArray() 
                                                  : this.validationObject.getTolerance(this.validationObject.getSchemaObjectColumn()));

        String updateClause = "",
             wherePredicate = String.format("%s <> 'TRUE'",validationFunction),
                      error = "",
                   function = "";
        int selectCount     = this.atModel.getCheckedRowCount();
        if ( selectCount==0 ) {
          updateClause = String.format("NVL(MDSYS.SDO_UTIL.RECTIFY_GEOMETRY(%s,%s),%s)",
                                       this.validationObject.getSelectedColumnName(),
                                       this.validationObject.getTolerance(this.validationObject.getSchemaObjectColumn()),
                                       this.validationObject.getSelectedColumnName());
        } else if ( selectCount==1 ) {
            for (int row=0; row<this.atModel.getRowCount(); row++) 
            {
                if ( (Boolean)this.atModel.getValueAt(row, this.atModel.OracleSelectedColumn) ) {
                    error    = (String)this.atModel.getValueAt(row, this.atModel.OracleErrorCodeColumn);
                    function = (String)this.atModel.getValueAt(row, this.atModel.OracleFunctionColumn);
                    if ( error.equalsIgnoreCase("ELSE") ) {
                        updateClause = functionReplace(function);
                    } else if ( error.equals("13000") || function.toUpperCase().contains("SDO_MIGRATE.TO_CURRENT")) {
                        wherePredicate = "t." + this.validationObject.getSelectedColumnName() + ".sdo_gtype < 2000";
                        updateClause   =  functionReplace(function); 
                    } else if (Strings.isEmpty(error) ) {
                        wherePredicate = "";
                        updateClause = functionReplace(function);
                    } else {
                        StringTokenizer st = new StringTokenizer(error,",");
                        String inClause = "",
                                    tok = "";
                        wherePredicate = validationFunction + " IN (" ;
                        while ( st.hasMoreTokens() ) {
                            tok = "'" + st.nextToken() + "'";
                            if (Strings.isEmpty(inClause) )
                                inClause = tok;
                            else 
                                inClause += "," + tok;
                        }
                        wherePredicate += inClause + ")";
                        updateClause   = functionReplace(function);
                    }
                    break;
                }
            }
        } else {
            updateClause = "\nCASE ";
            boolean elseFound = false;
            wherePredicate = validationFunction + " IN (" ;
            for (int row=0; row<this.atModel.getRowCount(); row++) {
                if ( (Boolean)this.atModel.getValueAt(row, this.atModel.OracleSelectedColumn) ) {
                    error    = (String)this.atModel.getValueAt(row, this.atModel.OracleErrorCodeColumn);
                    function = (String)this.atModel.getValueAt(row, this.atModel.OracleFunctionColumn);
                    if (Strings.isEmpty(error) ) {
                        this.setMessage(function + " can only be applied when single row is selected ");
                    } else if ( error.equalsIgnoreCase("ELSE") ) {
                        elseFound = true;
                        updateClause += "     ELSE " + functionReplace(function)+ "\n";                          
                        continue;
                    } else if ( error.equals("13000") || function.toUpperCase().contains("SDO_MIGRATE.TO_CURRENT")) {
                      updateClause += "     WHEN t." + this.validationObject.getSelectedColumnName() + ".sdo_gtype < 2000\n" +
                                      "     THEN "   + functionReplace(function)+ "\n";
                    } else {
                        StringTokenizer st = new StringTokenizer(error,",");
                        String inClause = "", 
                                    tok = "";
                        updateClause += "     WHEN " + validationFunction + " IN " ;
                        while ( st.hasMoreTokens() ) {
                            tok = "'" + st.nextToken() + "'";
                            if (Strings.isEmpty(inClause) )
                                inClause = tok;
                            else
                                inClause += "," + tok;
                        }
                        wherePredicate += wherePredicate.charAt(wherePredicate.length()-1)=='('?inClause:","+inClause;
                        updateClause += "(" + inClause + ")\n" +
                                        "     THEN " + functionReplace(function)+ "\n";
                    }
                }
            }
            wherePredicate += ")";
            if ( ! elseFound )
                updateClause += String.format("     ELSE NVL(MDSYS.SDO_UTIL.RECTIFY_GEOMETRY(%s,%s),%s)\n",
                                              this.validationObject.getSelectedColumnName(),
                                              this.validationObject.getTolerance(this.validationObject.getSchemaObjectColumn()),
                                              this.validationObject.getSelectedColumnName()); 
            updateClause += " END";
        }
        
        String updateSQL = String.format("UPDATE %s t \n" +
                                         "   SET t.%s = %s \n" +
                                         " WHERE " + wherePredicate + getSelectionPredicate(),
                                         this.validationObject.getFullObjectName(),
                                         this.validationObject.getSelectedColumnName(),
                                         updateClause.replaceFirst("CASE      WHEN", "CASE WHEN")
                                        );
        
        if ( !Strings.isEmpty(this.validationObject.getWhereClause()) ) {
            // check if starts with WHERE
            //
            String predicates = Strings.trimFront(this.validationObject.getWhereClause(), ' ');
            if ( predicates.toUpperCase().startsWith("WHERE ") ) {
                // replace
                predicates = predicates.substring(6);
            } else if ( predicates.toUpperCase().startsWith("AND ") ) {
                predicates = predicates.substring(4);
            } 
            if ( ! updateSQL.contains(predicates) )
                updateSQL += "\n   AND " + predicates;
        }
        return updateSQL;
    }

    private void btnCopySQL_actionPerformed(ActionEvent e) {
      if ( this.taUpdateSQL.getText() == null )
          return;
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      StringSelection ss = new StringSelection(this.taUpdateSQL.getText());
      clipboard.setContents(ss, ss);
    }

    private void btnSaveAssns_actionPerformed(ActionEvent e) {
        this.validationObject.geoRaptorPreferences.setDefaultAssociationXML(this.atModel.saveAssociations());
    }

    private void btnResetAssns_actionPerformed(ActionEvent e) {
        try {
            this.atModel.loadAssociations(this.validationObject.geoRaptorPreferences.getDefaultAssociationXML());
        } catch (Exception ex) { LOGGER.warn("loadAssociations failed: " + ex.getMessage()); }
    }

    private String getSelectionPredicate() {
        if ( ! this.cbUseSelection.isSelected() )
            return "";
        if ( this.validationObject.resultTable.getSelectedRowCount()==0 ) {
            return "";
        } else {
            boolean compoundKey = false;
            String idCol = this.validationObject.getIDColumn();
            if ( idCol.contains(",") ) {
              compoundKey = true;
            }
            String predicate = "",
                     idValue = "";
            int rows[] = this.validationObject.resultTable.getSelectedRows();
            int mRow = -1;
            for (int iRow=0; iRow<rows.length; iRow++) {
                mRow = this.validationObject.resultTable.convertRowIndexToModel(rows[iRow]);
                idValue = (String)this.validationObject.vtModel.getValueAt(mRow,ValidationTableModel.idColumn);
                if ( compoundKey ) {
                    predicate += (iRow==0?"":"\n   OR ") + idCol + " IN ('" + idValue.replace(",","','")+"')";
                } else {
                    predicate += (iRow==0?idCol + " IN (" : ",") + "'" + idValue + "'";
                }
            }
            if ( compoundKey )
                predicate = "\n   AND (" + predicate + ")"; // Can contain ORs
            else 
                predicate = "\n   AND " + predicate + ")";
            return predicate;
        }
    }
    
    private void cbUseSelection_actionPerformed(ActionEvent e) {
        taUpdateSQL.setText(getUpdateSQL());
    }

    public boolean isCANCELLED() {
		return CANCELLED;
	}

	public void setCANCELLED(boolean cANCELLED) {
		CANCELLED = cANCELLED;
	}

	private static enum columns { ORACLE_ERROR_CODE, ORACLE_CORRECTIVE_FUNCTION, APPLY};
    
  /**
   * @author : Simon Greener, March 27th 2011
   */
  class AssociationTableModel extends DefaultTableModel 
  {
      private static final long serialVersionUID = 4207287672069716659L;
      
	  private int cacheCapacity = 255;
      Vector<Object[]> cache; // will hold table column metadata

      protected String[] headers = {"Oracle Error Code", 
                                    "Corrective Function", 
                                    "Apply?"};

      @SuppressWarnings("rawtypes")
	Class[] types = new Class[] { String.class,
                                    String.class,
                                    Boolean.class };
      /**
       * @author : Simon Greener,  March 27th 2011
       */
      boolean[] canEdit = new boolean[] { true, true, true };
      
      public AssociationTableModel(  ) {
      }
      
      public final int OracleErrorCodeColumn = 0;
      public final int OracleFunctionColumn  = 1;
      public final int OracleSelectedColumn  = 2;
    
    public String getSelectedColumnNames(char _delimiter) {
        StringBuffer sb = new StringBuffer();
        ListIterator<Object[]> cacheIter = this.cache.listIterator();
        while (cacheIter.hasNext() ) {
            Object [] obj = cacheIter.next();
            if ( ! (Boolean)obj[2] )
                continue;
            sb.append((String)obj[0] + _delimiter);
        }
        return Strings.trimEnd(sb.toString(),_delimiter);
    }

    public List<String> getSelectedColumns() {
      List<String> returnList = new ArrayList<String>(this.cacheCapacity);
      ListIterator<Object[]> cacheIter = this.cache.listIterator();
      while (cacheIter.hasNext() ) {
          Object [] obj = cacheIter.next();
          if ( ! (Boolean)obj[OracleSelectedColumn] )
              continue;
          returnList.add((String)obj[0]);
      }
      return returnList;
    }

    public int getCheckedRowCount() {
        int selected = 0;
        for (int row=0; row < this.getRowCount(); row++ ) {
            Object[] obj = this.cache.elementAt(row);
            if ( (Boolean)obj[OracleSelectedColumn] )
                selected++;
        }
        return selected;
    }    

    public int getCheckedNoErrorCount() {
        int selected = 0;
        for (int row=0; row < this.getRowCount(); row++ ) {
            Object[] obj = this.cache.elementAt(row);
            if ( (Boolean)obj[OracleSelectedColumn] ) {
              if (Strings.isEmpty((String)obj[OracleErrorCodeColumn]) ) 
                  selected++;
            }
        }
        return selected;
    }
    
    public String getColumnName(int i) {
        return this.headers[i];
    }

    public int getColumnPosition(columns _col) 
    {
        int position = -1;
        switch (_col) {
            case ORACLE_ERROR_CODE          : { position = OracleErrorCodeColumn; break; }
            case ORACLE_CORRECTIVE_FUNCTION : { position = OracleFunctionColumn; break; }
            case APPLY                      : { position = OracleSelectedColumn; break; }
        }
        return position;
    }
    
    public int getColumnCount() {
        return this.headers.length;
    }
    
    public int getRowCount() {
        if (this.cache == null) {
            return 0;
        } else {
            return this.cache.size();
        }
    }

    public void removeSelectedRows(int[] _rows) {
        if ( _rows == null || _rows.length <= 0 )
            return;
        // Get collection of objects for deletion
        //
        int rModel = 0;
        ArrayList<Object[]> dRows = new ArrayList<Object[]>(_rows.length);
        for (int iRow=0; iRow < _rows.length; iRow++) {
            rModel = tblAssociations.convertRowIndexToModel(_rows[iRow]);
            dRows.add(this.cache.get(rModel));
        }
        // Now we can remove them
        //
        this.cache.removeAll(dRows);  
        this.fireTableDataChanged();
    }
    
    public void removeRow(int _row) {
        int row = _row == this.cache.size() ? this.cache.size()-1 : _row;
        this.cache.removeElementAt(row);
        this.fireTableRowsDeleted(this.cache.size()-1,this.cache.size()-1);
    }
    
    public void addRow(Object[] row) {
        cache.addElement(row);
        this.fireTableRowsInserted(cache.size()-1,cache.size()-1);
    }
    
    @SuppressWarnings("unused")
	private boolean valueExists(String _errorCode) 
    {
        boolean exists = false;
        ListIterator<Object[]> chcIter = this.cache.listIterator();
        while (chcIter.hasNext()) {
            Object[] o = chcIter.next();
            if ( ((String)o[OracleErrorCodeColumn]).equalsIgnoreCase(_errorCode) ) {
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
        return this.types[col];
    }
    
    public String[] getColumnNames() {
        return this.headers;
    }
    
    public Object getValueAt(int row, int col) {
        return this.cache.elementAt(row)[col];
    }
    
    public void setValueAt(Object value, int row, int col) 
    {
        if ( ! this.canEdit[col] )
            return;
        Object[] obj = this.cache.elementAt(row);
        obj[col] = value;
        this.cache.setElementAt(obj,row);
        this.fireTableRowsUpdated(row,row);
    }

    public void setAllSelection(boolean _selected) {
        for (int row=0; row < this.getRowCount(); row++ ) {
            Object[] obj = this.cache.elementAt(row);
            obj[OracleSelectedColumn] = Boolean.valueOf(_selected);
            this.cache.setElementAt(obj,row);
        }
        this.fireTableRowsUpdated(0, this.getRowCount());
    }

    public boolean isCellEditable(int row, int col) {
        return this.canEdit[col];
    }

    public String saveAssociations() {
        String xml = "<Validations>";
        ListIterator<Object[]> cacheIter = cache.listIterator();
        while (cacheIter.hasNext()) {
            Object [] obj = cacheIter.next();
            xml += "<Row>" + 
                   "<ErrorCode>" + (Strings.isEmpty((String)obj[OracleErrorCodeColumn])?"NULL":(String)obj[OracleErrorCodeColumn]) + "</ErrorCode>" +
                   "<Function>"  + (String)obj[OracleFunctionColumn] + "</Function>" + 
                   "</Row>";
        }
        xml += "</Validations>";
        return xml;
    }
    
    public void loadAssociations(String _xmlData) 
    throws Exception
    {
        if (Strings.isEmpty(_xmlData)) {
            return;
        }
        try {
            this.clearModel();
            Document doc = null;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(_xmlData)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            // Extract rows
            //
            NodeList nList = (NodeList) xpath.evaluate("/Validations/Row",doc,XPathConstants.NODESET);
            for (int i = 0 ; i < nList.getLength(); i++)
            {
                Object[] record = new Object[3];
                record[OracleErrorCodeColumn] = xpath.evaluate("ErrorCode/text()",nList.item(i),XPathConstants.STRING);
                if ( ((String)record[OracleErrorCodeColumn]).equalsIgnoreCase("NULL") )
                    record[OracleErrorCodeColumn] = "";
                record[OracleFunctionColumn]  = xpath.evaluate("Function/text()",nList.item(i),XPathConstants.STRING);
                record[OracleSelectedColumn]  = Boolean.valueOf(false);
                // add Metadata to internal cache
                //
                this.addRow(record);
            }
            this.fireTableRowsInserted(0,this.cache.size()-1); // notify everyone that we have a new table.
        } catch (Exception e) {
            throw new Exception("Error loading\n"+_xmlData);
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
  }

}
