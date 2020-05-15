/**
 * @title : SRIDPanel.java
 * @precis: Attempt to provide user with interactive way to select SRID from thousands!
 * @author: Simon Greener
 *          Created on 10/05/2010, 10:48:29 AM
 *
 * @tobedone 1. Fix cross-column regular expression filtering
 * 
 */

package org.GeoRaptor.OracleSpatial.SRID;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.TableSortIndicator;

import oracle.jdbc.OracleConnection;


/**
 *
 * @author Simon
 * 
 */
public class SRIDPanel extends javax.swing.JDialog implements Observer {

	private static final long serialVersionUID = 2461910316368426646L;

	/**
     * We have only one instance of this class
     */
    private static SRIDPanel classInstance;

    /**
     * Get reference to single isntance of GeoRaptor's Preferences
     */
    protected Preferences sridPreferences;

    /**
     * The pattern that is applied to the JTable generated from the SQL
     * executed in QueryTableModel class
     */
    private QueryTableModel qtm;
    private TableSortIndicator fSortIndicator;
    private TableRowSorter<QueryTableModel> sorter;
    private List<RowSorter.SortKey> sortKeys;
    private List<RowFilter<Object, Object>> rowFilters;
    private RowFilter<QueryTableModel, Object> orFilters;
    // Sort icons for in header
    private Icon ascIcon = UIManager.getIcon("Table.ascendingSortIcon"),
                descIcon = UIManager.getIcon("Table.descendingSortIcon");
    
    /**
     * Reference to resource manager for accesing messages in properties file.
     */
    protected PropertiesManager propertyManager;
    private static final String propertiesFile = "org.GeoRaptor.OracleSpatial.SRID.SRIDPanel";

    /**
     * Database connection
     * Needed to query the MDSYS CS tables
     */
    private Connection conn;

    private boolean cancelled = false;

    // ====================== Constructors

    private SRIDPanel() {
        this(null, "", true);
    }

    /**
     * Creates new form SRIDPanel.
     * @param _parent
     * @param _title
     * @param _modal
     */
    public SRIDPanel(java.awt.Frame _parent, String _title, boolean _modal) {
        super(_parent, _title, _modal); // modal should always be true

        // Get the one reference to GeoRaptor's preferences
        //
        this.sridPreferences = MainSettings.getInstance().getPreferences();

        this.propertyManager = new PropertiesManager(SRIDPanel.propertiesFile);

        initComponents();

        // Change design English text to that in the properties file
        //
        this.setTitle(this.propertyManager.getMsg("SRID_DIALOG_TITLE"));
        this.pnlSRIDSelection.setBorder(javax.swing.BorderFactory.createTitledBorder(this.propertyManager.getMsg("SRID_BORDER_TITLE")));
        this.btnCancel.setText(this.propertyManager.getMsg("SRID_CANCEL_BUTTON"));
        this.lblSRID.setText(this.propertyManager.getMsg("SRID_SELECT_LABEL"));
        this.btnOK.setText(this.propertyManager.getMsg("SRID_OK_BUTTON"));
        this.btnSavePreference.setText(this.propertyManager.getMsg("SRID_PREFS_BUTTON"));
        this.tfSRID.setText(this.sridPreferences.getSRID());
        this.tfSRID.setEnabled(false); // Don't want user to edit it directly
    }

    /**
     * The preferred public method of instantiating the class.
     * Guarantees only one instance.
     * @return
     */
    public static SRIDPanel getInstance() {
        if (SRIDPanel.classInstance == null) {
            SRIDPanel.classInstance = new SRIDPanel();
        }
        return SRIDPanel.classInstance;
    }

    // ====================== Non-Netbeans initiation

    public boolean initialise(OracleConnection _conn, 
                              String           _srid) {
        // Always refresh getting of connection as last one used may be different from now or closed
        // Any connection will do
        //
        this.conn = (_conn == null) ? DatabaseConnections.getInstance().getAnyOpenConnection() : _conn;
        if ( this.conn == null ) {
            JOptionPane.showMessageDialog(null, 
                                          "Not connected to any database.",
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // No need to reload table if not first time called as we are using one instance of the class
        //
        if ((this.tblSRIDS != null) && (this.qtm != null) && (this.propertyManager != null)) 
        {
            int rid = Strings.isEmpty(_srid) || _srid.equals(Constants.NULL) ? 0 : findSRID(_srid);
            setUpSRID(_srid, rid==-1?0:rid);
            return true;
        }
        try {
            /**
             * A table has three parts:
             * - Jtable: this is the table...
             * - tableModel: this is the data, and the behaviour of the data within the table
             * - tableRender: this is to change how the table is vizualized... (bold text, color, etc)
             **/

            // Create table model filling it with SRID data
            //
            qtm = new QueryTableModel();
            int rowSRID = qtm.runQuery(conn,_srid.equals(Constants.NULL) ? 0 : Integer.valueOf(_srid.trim()));

            // Create a sort key per column
            //
            sortKeys = new ArrayList<RowSorter.SortKey>();
            for (int i = (qtm.getColumnCount() - 1); i > 0; i--)
                sortKeys.add(new RowSorter.SortKey(i, javax.swing.SortOrder.UNSORTED));

            // Create actual sorter to hold sortKeys and filters
            //
            sorter = new TableRowSorter<QueryTableModel>(qtm) {
                @Override
                public void toggleSortOrder(int column) {
                    @SuppressWarnings("unchecked")
					RowFilter<QueryTableModel,Object> rowFilter = (RowFilter<QueryTableModel,Object>)getRowFilter();
					setRowFilter(null);
                    super.toggleSortOrder(column);
                    setRowFilter(rowFilter);
                }
            };

            // Add keys to sorter
            //
            sorter.setSortKeys(sortKeys);

            // Create a filter for each column
            //
            rowFilters = new ArrayList<RowFilter<Object, Object>>(4);
            rowFilters.add(RowFilter.regexFilter("([0-9]*)", 0));
            rowFilters.add(RowFilter.regexFilter("(?i)", 1));
            rowFilters.add(RowFilter.regexFilter("(?i)", 2));
            rowFilters.add(RowFilter.regexFilter("(?i)", 3));
            orFilters = RowFilter.orFilter(rowFilters);

            // Associate those filters with the sorter
            //
            sorter.setRowFilter(orFilters);

            // Add row sorter and filter to table
            //
            tblSRIDS.setRowSorter(sorter);

            // Whenever tfFilter changes, invoke sridFilter.
            //
            tfFilter.addKeyListener(new KeyListener() {
                    public void keyPressed(KeyEvent e) { }
                    public void keyTyped(KeyEvent e) { }
                    public void keyReleased(KeyEvent e) { sridFilter(); }
                });

            // Add selection listener to get SRID value
            //
            tblSRIDS.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent event) {
                        int viewRow = tblSRIDS.getSelectedRow();
                        if (viewRow >= 0) {
                            String srid = tblSRIDS.getValueAt(viewRow, 0/* Column 0 is always the SRID */).toString();
                            tfSRID.setText((srid == "0") ? Constants.NULL : srid);
                        }
                    }
                });
            tblSRIDS.setModel(qtm);
            tblSRIDS.setFillsViewportHeight(true);
            tblSRIDS.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tblSRIDS.setAutoCreateColumnsFromModel(false);
            tblSRIDS.setFocusable(true);

            // Custom Renderers
            // Table
            //
            //qtcr = new QueryTableCellRenderer();
            //tblSRIDS.setDefaultRenderer(Color.class, qtcr);
            
            fSortIndicator = new TableSortIndicator(tblSRIDS, ascIcon, descIcon);
            fSortIndicator.addObserver(this);

            // Now data loaded, position view over passed in SRID 
            //
            setUpSRID(_srid,rowSRID);

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return true;
    }

    // When the user clicks a column header, fSortIndicator will notify 
    // registered observers, who will call getSortBy to fetch the new sort.
    // But we don't really use this in GeoRaptor.
    //  
    public void update(Observable aObservable, Object aData) {
        //extract column and (asc|desc) from fSortIndicator
        fSortIndicator.getSortBy();
    }

    /**
     * @method isGeodetic
     * @precis Allows GeoRaptor class to discover if SRID is geodetic for purposes of metadata construction and tolerance setting
     * @param _srid
     * @return boolean
     * @author Simon Greener, 28th May 2010
     *          Original coding
     */
    public boolean isGeodetic(String _srid) 
    {
        if (Strings.isEmpty(_srid) )
            return false;
        if ( this.qtm == null || this.qtm.getRowCount()==0 )
            return false;
        for (int i=0; i<qtm.getRowCount(); i++) {
            if ( qtm.getValueAt(i,0).toString().equals(_srid)) {
                return qtm.getValueAt(i,2).toString().indexOf("GEODETIC") != -1;
            }
        }
        return false;
    }
    
    /**
     * @function findSRID
     * @param _srid 
     *        Passed in SRID whose location in the table we require
     * @return integer - SRID's location in Model
     */
    private int findSRID(String _srid) {
        int rowSRID = -1;
        QueryTableModel qtm = (QueryTableModel)tblSRIDS.getModel();
        for (int i=0; i< qtm.getRowCount(); i++) {
            if ( qtm.getValueAt(i,0).toString().equals(_srid)) {
                rowSRID = i;
                break;
            }
        }
        return rowSRID;
    }
    
    /** 
     * Assumes table is contained in a JScrollPane.
     * Scrolls the cell (rowIndex, vColIndex) so that it is visible
     * within the viewport.
     */
    public void scrollToVisible(JTable table, int row, int col) 
    {
        if (!(table.getParent() instanceof  JViewport))
            return;

        JViewport viewport = (JViewport) table.getParent();

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = table.getCellRect(row, col, true);

        //rect.y += table.getVisibleRect().getHeight() / 2.0;
        rect.y += table.getPreferredScrollableViewportSize().getHeight() / 2.0;
            
        // The location of the viewport relative to the table
        Point pt = viewport.getViewPosition();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0)
        rect.setLocation(rect.x - pt.x, rect.y - pt.y);

        // Scroll the area into view
        viewport.scrollRectToVisible(rect);
    }

    private void setUpSRID(String _srid, int _rowSRID) 
    {
        // Set up the SRID
        // So that it is selected, highlighted and at the top of the viewport
        // 
        this.tfSRID.setText(_srid);           
        this.tblSRIDS.addRowSelectionInterval(_rowSRID, _rowSRID);
        this.tblSRIDS.addColumnSelectionInterval(0,qtm.getColumnCount()-1);
        this.scrollToVisible(tblSRIDS,_rowSRID,0);
    }
    
    /**
     * @function sridFilter()
     * @author Simon Greener
     * @precis applies typed in regex to table via sorter
     * 
     * TOBEDONE I am not sure how this uses each of the orFilters set in initialise()
     */
    private void sridFilter() 
    {
        RowFilter<QueryTableModel, Object> rf = null;
        // should this have "(?i)" prefixed to the text?
        String userFilterString = tfFilter.getText();
        if (userFilterString.length() != 0) {
            userFilterString = userFilterString.contains("(?i)") ? tfFilter.getText() : "(?i)" + tfFilter.getText();
            //If current expression doesn't parse, don't update.
            try 
            {
                rf = RowFilter.regexFilter(userFilterString);
            } catch (java.util.regex.PatternSyntaxException e) {
                return;
            }
        }
        sorter.setRowFilter(rf);
    }

    // ==================================================== NetBeans

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlSRIDSelection = new javax.swing.JPanel();
        btnCancel = new javax.swing.JButton();
        lblSRID = new javax.swing.JLabel();
        btnOK = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblSRIDS = new javax.swing.JTable();
        tfSRID = new javax.swing.JTextField();
        tfFilter = new javax.swing.JTextField();
        lblFilter = new javax.swing.JLabel();
        btnSavePreference = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        pnlSRIDSelection.setBorder(javax.swing.BorderFactory.createTitledBorder("SRID Selection"));

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        lblSRID.setText("Selected SRID:");

        btnOK.setText("Return Selected SRID");
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });

        jScrollPane1.setAutoscrolls(true);

        tblSRIDS.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] { {null, null, null, null} },
            new String []   { "Title 1", "Title 2", "Title 3", "Title 4" } ) 
        {
			private static final long serialVersionUID = 7496305790572829607L;
			@SuppressWarnings("rawtypes")
			Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tblSRIDS);

        tfSRID.setText("NULL");

        lblFilter.setText("Filter:");

        btnSavePreference.setText("Save Preference");
        btnSavePreference.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSavePreferenceActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlSRIDSelectionLayout = new javax.swing.GroupLayout(pnlSRIDSelection);
        pnlSRIDSelection.setLayout(pnlSRIDSelectionLayout);
        pnlSRIDSelectionLayout.setHorizontalGroup(
            pnlSRIDSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSRIDSelectionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlSRIDSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 761, Short.MAX_VALUE)
                    .addGroup(pnlSRIDSelectionLayout.createSequentialGroup()
                        .addGroup(pnlSRIDSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlSRIDSelectionLayout.createSequentialGroup()
                                .addComponent(lblSRID)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tfSRID, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblFilter)
                                .addGap(3, 3, 3)
                                .addComponent(tfFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(pnlSRIDSelectionLayout.createSequentialGroup()
                                .addComponent(btnSavePreference)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 434, Short.MAX_VALUE)
                                .addComponent(btnOK)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel)))
                .addContainerGap())
        );
        pnlSRIDSelectionLayout.setVerticalGroup(
            pnlSRIDSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSRIDSelectionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlSRIDSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSRID)
                    .addComponent(tfSRID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tfFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblFilter))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlSRIDSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel)
                    .addComponent(btnOK)
                    .addComponent(btnSavePreference))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlSRIDSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlSRIDSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.setAlwaysOnTop(false);
        this.cancelled =
                true; // lets calling program know that the activity was cancelled.
        this.setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOKActionPerformed
        this.setAlwaysOnTop(false);
        this.cancelled =
                false; // lets calling program know that the activity was not cancelled so can get selected SRID value
        this.setVisible(false);
    }//GEN-LAST:event_btnOKActionPerformed

    private void btnSavePreferenceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSavePreferenceActionPerformed
        this.sridPreferences.setSRID(this.tfSRID.getText());
    }//GEN-LAST:event_btnSavePreferenceActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOK;
    private javax.swing.JButton btnSavePreference;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblFilter;
    private javax.swing.JLabel lblSRID;
    private javax.swing.JPanel pnlSRIDSelection;
    private javax.swing.JTable tblSRIDS;
    private javax.swing.JTextField tfFilter;
    private javax.swing.JTextField tfSRID;
    // End of variables declaration//GEN-END:variables

    /**
     * For use by calling classes after SRID selected.
     * @return SRID as text.
     */
    public String getSRID() {
        return (Strings.isEmpty(tfSRID.getText()) || tfSRID.getText().equals("0")
                ? Constants.NULL :
                this.tfSRID.getText());
    }

    public boolean formCancelled() {
        return this.cancelled;
    }

    @SuppressWarnings("unused")
	private void printTableContents() {
        for (int i = 0; i < qtm.getRowCount(); i++) {
            for (int j = 0; j < qtm.getColumnCount(); j++)
                System.out.print(qtm.getValueAt(i, j).toString() + " - ");
            System.out.println();
        } 
    }

    // Declare TableModel for tblSRIDS as inner class so one can see, immediately,
    // the relationship between the dialog's table and the model
    //

    class QueryTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1686766163737605940L;
		
		String[] headers =
            new String[] { "SRID", 
                           "Coord System Name", 
                           "Coord Ref Kind",
                           "Datum Name" };
        Vector<Object[]> cache; // will hold table data . . .

        public QueryTableModel() {
            cache = new Vector<Object[]>();
        }

        public String getColumnName(int i) {
            return headers[i];
        }

        public int getColumnCount() {
            return headers.length;
        }

        public int getRowCount() {
            return cache.size();
        }

        public void addRow(Object[] _row) {
            cache.addElement(_row);
        }

        public Object getValueAt(int row, int col) {
            return ((Object[])cache.elementAt(row))[col];
        }

        Class<?>[] types =
            new Class[] { java.lang.Integer.class, 
                          java.lang.String.class,
                          java.lang.String.class, 
                          java.lang.String.class };

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         */

        public Class<?> getColumnClass(int columnIndex) {
            return types[columnIndex];
        }

        public String[] getColumnNames() {
            return headers;
        }

        boolean[] canEdit = new boolean[] { false, false, false, false };

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit[columnIndex];
        }

        public int runQuery(Connection _conn, int _srid) 
        {
            int rowSRID = 0;
            cache = new Vector<Object[]>();
            try {
                String sql =
                    "Select srid, cs_name, coordref_kind, datum_name \n" +
                    "  from (Select Cs.Srid,  \n" +
                    "               CS.CS_NAME, \n" +
                    "               Case When Crs.Coord_Ref_Sys_Kind Is Not Null \n" +
                    "                    Then Case When Cs.Wktext Like 'GEOGCS%'  \n" +
                    "                              Then Crs.Coord_Ref_Sys_kind  || ' (GEODETIC)' \n" +
                    "                              Else Crs.Coord_Ref_Sys_kind  \n" +
                    "                          End \n" +
                    "                    Else NULL \n" +
                    "                End As Coordref_Kind, \n" +
                    "               D.Datum_Name \n" +
                    "          From Mdsys.Sdo_Cs_Srs Cs \n" +
                    "               Left Join \n" +
                    "               Mdsys.Sdo_Coord_Ref_Sys Crs \n" +
                    "               ON (Crs.Srid = Cs.Srid) \n" +
                    "               Left Join \n" +
                    "               Mdsys.Sdo_Datums D \n" +
                    "               ON (D.Datum_Id = Crs.Datum_Id) \n" +
                    "       ) \n" +
                    " Order By 1";
                Statement st = _conn.createStatement();
                st.setFetchSize(100); // default is 10
                st.setFetchDirection(ResultSet.FETCH_FORWARD);
                ResultSet rSet = st.executeQuery(sql);
                Integer iSRID;
                // Add "dummy" NULL record
                Object[] nullRecord =
                    new Object[] { 0, "NULL", "NULL", "NULL" };
                this.addRow(nullRecord);
                while (rSet.next()) {
                    Object[] record = new Object[4];
                    // Need first column as Integer to get numeric sort order and not text sort order
                    //
                    iSRID = Integer.valueOf(rSet.getInt("SRID"));
                    if (rSet.wasNull()) iSRID = 0;
                    record[0] = iSRID;
                    for (int i = 1; i < 4; i++) {
                        record[i] = rSet.getString(i + 1);
                        if (rSet.wasNull())
                            record[i] = "NULL";
                    }
                    this.addRow(record);
                    if ( iSRID == _srid ) rowSRID = (this.getRowCount() - 1); 
               }
                rSet.close();
                rSet = null;
                st.close();
                st = null;
                this.fireTableRowsInserted(0,cache.size()); // notify everyone that we have a new table.
            } catch (Exception e) {
                cache = new Vector<Object[]>(); // blank it out and keep going.
                e.printStackTrace();
            }
            return rowSRID;
        }

    }

} 
