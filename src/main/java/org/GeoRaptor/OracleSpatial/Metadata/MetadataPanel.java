/*
 * MetadataPanel.java
 *
 * Created on 22/04/2010, 2:51:18 PM
 */

package org.GeoRaptor.OracleSpatial.Metadata;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Messages;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.SRID.SRIDPanel;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.sql.DatabaseConnection;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.HtmlHelp;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import oracle.ide.dialogs.ProgressBar;
import oracle.jdbc.OracleConnection;


/**
 * @author  MaticP, original coding
 * @author Simon Greener April 2010 - June 2012
 **/
public class MetadataPanel extends javax.swing.JDialog {

	private static final long serialVersionUID = -3294320762367337970L;

	/**
     * We have only one instance of this class
     */
    private static MetadataPanel classInstance;

    /**
     * Get reference to single instance of GeoRaptor's Preferences
     */
    protected Preferences metadataPreferences;

    /**
     * Reference to resource manager for accessing messages in properties file
     */
    protected PropertiesManager propertyManager;
    private static final String propertiesFile = "org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel";

    // ======================
    // Non-Netbeans Variables

    private final String sCancelled = "CANCELLED";
    private final String sError     = "ERROR";
    private final String sOK        = "OK";

    protected static String dialogTitle;
    private static final String CALC_MBR_ERROR = MainSettings.EXTENSION_NAME + " - Calculate MBR error";
    private static final String UPDATE_METADATA_ERROR = MainSettings.EXTENSION_NAME + " - Metadata Processing Error";
    
    // Colummn headings for metadataTable
    private String METADATA_TABLE_COLUMN_0;
    private String METADATA_TABLE_COLUMN_1;
    private String METADATA_TABLE_COLUMN_2;
    private String METADATA_TABLE_COLUMN_3;
    private String METADATA_TABLE_COLUMN_4;
    
    // Popup menu for right mouse clicks on main table 
    private JPopupMenu    popupMenu;
    public enum MENU_SELECTION { COPY, DELETE };
    
    /**
     * Database connection
     */
    protected OracleConnection             conn = null;
    protected DatabaseConnections dbConnections = null;
    
    /**
     * Name of:
     * 1. connection userName (may not own anything ie not schema).
     * 2. schema owning object
     * 3. objectName containing index
     * 4. columnName actual SDO_GEOMETRY column
     */
    protected String userName;
    protected String schemaName;
    protected String objectName;
    protected String objectType;
    protected String columnName;
    
    /* Table handling variables for metdata entries in form
     */
    private UserMetadataTableModel userTM;
    private     ListSelectionModel userLSM;

    private     MetadataTableModel metadataTM;
    protected   ListSelectionModel metadataLSM;

    // ======================

    private MetadataPanel() {
        this(null, "", false);
    }

    public static MetadataPanel getInstance() {
        if (MetadataPanel.classInstance == null) {
            MetadataPanel.classInstance = new MetadataPanel();
        }
        return MetadataPanel.classInstance;
    }
    
    /** Creates new form MetadataPanel */
    private MetadataPanel(java.awt.Frame parent, 
                          String title,
                          boolean modal) 
    {
        super(parent, title, modal);

        // Get the one reference to GeoRaptor's preferences
        //
        this.metadataPreferences = MainSettings.getInstance().getPreferences();

        this.propertyManager = new PropertiesManager(MetadataPanel.propertiesFile);
        
        dialogTitle = propertyManager.getMsg("MD_DIALOG_TILE");

        METADATA_TABLE_COLUMN_0 = this.propertyManager.getMsg("METADATA_TABLE_COLUMN_0");
        METADATA_TABLE_COLUMN_1 = this.propertyManager.getMsg("METADATA_TABLE_COLUMN_1");
        METADATA_TABLE_COLUMN_2 = this.propertyManager.getMsg("METADATA_TABLE_COLUMN_2");
        METADATA_TABLE_COLUMN_3 = this.propertyManager.getMsg("METADATA_TABLE_COLUMN_3");
        METADATA_TABLE_COLUMN_4 = this.propertyManager.getMsg("METADATA_TABLE_COLUMN_4");
    
        initComponents();

        // Set up models for both JTables .....
        //
        this.metadataTM = new MetadataTableModel();
        this.metadataTable.setModel(this.metadataTM);
        
        // Set column widths
        // Schema
        this.metadataTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        this.metadataTable.getColumnModel().getColumn(0).setMaxWidth(130);
        this.metadataTable.getColumnModel().getColumn(0).setResizable(true);

        // Object
        this.metadataTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        this.metadataTable.getColumnModel().getColumn(1).setResizable(true);
        
        // Column
        this.metadataTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        this.metadataTable.getColumnModel().getColumn(2).setResizable(true);
        
        // SRID
        this.metadataTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        this.metadataTable.getColumnModel().getColumn(3).setResizable(true);
        
        // DimInfo
        this.metadataTable.getColumnModel().getColumn(4).setPreferredWidth(1000);
        this.metadataTable.getColumnModel().getColumn(4).setResizable(true);

        this.metadataTable.setBackground(this.metadataPreferences.getOrphanColour());
        this.metadataTable.setSelectionForeground(this.metadataPreferences.getHighlightColour());
        
        this.cbOrphan.setBackground(this.metadataPreferences.getOrphanColour());
        this.cbObjectExists.setBackground(this.metadataPreferences.getCorrectColour());
        this.cbMissing.setBackground(this.metadataPreferences.getMissingColour());
        this.cbMissing.setForeground(Color.BLACK);
        
        this.cbObjectExists.setText(this.propertyManager.getMsg("cbObjectExists"));
        this.cbOrphan.setText(this.propertyManager.getMsg("cbOrphan"));
        this.cbMissing.setText(this.propertyManager.getMsg("cbMissing"));

        TableColumnModel metadataColModel = this.metadataTable.getColumnModel();
        for (Enumeration<TableColumn> colEnum = metadataColModel.getColumns(); colEnum.hasMoreElements();) {
            TableColumn c = colEnum.nextElement();
            c.setCellRenderer(new HighlightRenderer());
        }
        this.metadataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableRowSorter<MetadataTableModel> metadataSorter = new TableRowSorter<MetadataTableModel>(this.metadataTM);
        this.metadataTable.setRowSorter(metadataSorter);
        RowFilter<MetadataTableModel,Integer> cbFilter = new RowFilter<MetadataTableModel,Integer>() {
            public boolean include(Entry<? extends MetadataTableModel, ? extends Integer> entry) {
                entry.getModel();
                int row = entry.getIdentifier().intValue();
                MetadataEntry mEntry = (MetadataEntry)metadataTM.getValueAt(row,metadataTM.mEntryPosn);
                // We display if flags align 
                if ( ( cbMissing.isSelected()      && mEntry.isMissing() ) || 
                     ( cbOrphan.isSelected()       && mEntry.isOrphan()  ) ||
                     ( cbObjectExists.isSelected() && mEntry.isValid()   ) )
                    return true;
                return false;
            }
        };
        metadataSorter.setRowFilter(cbFilter);
        
        // Top "User" Table
        // 
        this.userTM = new UserMetadataTableModel(this.schemaName,this.objectName,this.columnName,"NULL");
        this.userSdoMetadataTable.setModel(userTM);

        // set cell renderers for double valued cells
        for (int i=0; i < this.userTM.getColumnCount(); i++ ) {
            if ( this.userTM.getColumnClass(i) == java.lang.Double.class ) {
                NumberRenderer nr = new NumberRenderer(0,15,false);
                this.userSdoMetadataTable.getColumn(this.userTM.getColumnName(i)).setCellRenderer(nr);
            }
        }
        
        this.sldrSample.setValue(100);
        this.lblPercentage.setText(String.valueOf(this.sldrSample.getValue()) + "%" );
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnClose = new javax.swing.JButton();
        btnHelp = new javax.swing.JButton();
        panelCurrentMetadata = new javax.swing.JPanel();
        scrollmetadataTable = new javax.swing.JScrollPane();
        metadataTable = new javax.swing.JTable();
        cbAllSchemas = new javax.swing.JCheckBox();
        cbObjectExists = new javax.swing.JCheckBox();
        cbOrphan = new javax.swing.JCheckBox();
        cbMissing = new javax.swing.JCheckBox();
        panelObjectMetadata = new javax.swing.JPanel();
        lblSchema = new javax.swing.JLabel();
        tfSelectedSchema = new javax.swing.JLabel();
        lblObject = new javax.swing.JLabel();
        tfSelectedObject = new javax.swing.JLabel();
        lblGeometryColumn = new javax.swing.JLabel();
        cmbGeoColumns = new javax.swing.JComboBox<String>();
        lblSRID = new javax.swing.JLabel();
        tfSRID = new javax.swing.JTextField();
        btnSRID = new javax.swing.JButton();
        pnlConnection = new javax.swing.JPanel();
        lblDBConnection = new javax.swing.JLabel();
        cmbConnections = new javax.swing.JComboBox<String>();
        lblConnectedSchema = new javax.swing.JLabel();
        tfConnectedSchema = new javax.swing.JTextField();
        btnReloadConnections = new javax.swing.JButton();
        pnlDimElements = new javax.swing.JPanel();
        btnCalculateDiminfo = new javax.swing.JButton();
        lblSample = new javax.swing.JLabel();
        sldrSample = new javax.swing.JSlider();
        lblPercentage = new javax.swing.JLabel();
        btnAddElement = new javax.swing.JButton();
        scrollUserSdoMetadataTable = new javax.swing.JScrollPane();
        userSdoMetadataTable = new javax.swing.JTable();
        btnUpdate = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("SDO_GEOM_METADATA");

        btnClose.setMnemonic('C');
        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        btnHelp.setMnemonic('L');
        btnHelp.setText("Help");
        btnHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHelpActionPerformed(evt);
            }
        });

        panelCurrentMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("All metadata for user XYZ"));
        panelCurrentMetadata.setMaximumSize(null);
        panelCurrentMetadata.setName(""); // NOI18N

        scrollmetadataTable.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollmetadataTable.setAutoscrolls(true);
        scrollmetadataTable.setMaximumSize(null);
        scrollmetadataTable.setMinimumSize(null);
        scrollmetadataTable.setPreferredSize(new java.awt.Dimension(809, 227));

        metadataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Schema", "Table/View/MView", "Column", "SRID", "DimInfo"
            }
        ) {
			private static final long serialVersionUID = 918428651124757416L;
			@SuppressWarnings("rawtypes")
			Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        metadataTable.setMaximumSize(null);
        metadataTable.setMinimumSize(null);
        scrollmetadataTable.setViewportView(metadataTable);

        cbAllSchemas.setText("All Schemas");
        cbAllSchemas.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        cbAllSchemas.setMaximumSize(null);
        cbAllSchemas.setMinimumSize(null);
        cbAllSchemas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbAllSchemasActionPerformed(evt);
            }
        });

        cbObjectExists.setBackground(new java.awt.Color(255, 102, 102));
        cbObjectExists.setSelected(true);
        cbObjectExists.setText("No Database Object - Orphan Metadata ");
        cbObjectExists.setMaximumSize(null);
        cbObjectExists.setMinimumSize(null);
        cbObjectExists.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbObjectExistsActionPerformed(evt);
            }
        });

        cbOrphan.setBackground(new java.awt.Color(102, 255, 102));
        cbOrphan.setSelected(true);
        cbOrphan.setText("Database Object Has Metadata ");
        cbOrphan.setMaximumSize(null);
        cbOrphan.setMinimumSize(null);
        cbOrphan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbOrphanActionPerformed(evt);
            }
        });

        cbMissing.setBackground(new java.awt.Color(255, 204, 51));
        cbMissing.setSelected(true);
        cbMissing.setText("Database Object with Geometry - No Metadata ");
        cbMissing.setMaximumSize(null);
        cbMissing.setMinimumSize(null);
        cbMissing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbMissingActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelCurrentMetadataLayout = new javax.swing.GroupLayout(panelCurrentMetadata);
        panelCurrentMetadata.setLayout(panelCurrentMetadataLayout);
        panelCurrentMetadataLayout.setHorizontalGroup(
            panelCurrentMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelCurrentMetadataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelCurrentMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollmetadataTable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelCurrentMetadataLayout.createSequentialGroup()
                        .addComponent(cbObjectExists, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(cbOrphan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(cbMissing, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cbAllSchemas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panelCurrentMetadataLayout.setVerticalGroup(
            panelCurrentMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelCurrentMetadataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollmetadataTable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addGroup(panelCurrentMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbAllSchemas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cbObjectExists, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cbOrphan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cbMissing, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        panelObjectMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("Current Object Metadata"));
        panelObjectMetadata.setMaximumSize(null);
        panelObjectMetadata.setPreferredSize(new java.awt.Dimension(841, 336));

        lblSchema.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblSchema.setLabelFor(tfSelectedSchema);
        lblSchema.setText("Schema:");
        lblSchema.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        lblSchema.setMaximumSize(null);
        lblSchema.setMinimumSize(null);
        lblSchema.setPreferredSize(new java.awt.Dimension(87, 14));

        tfSelectedSchema.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        tfSelectedSchema.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        tfSelectedSchema.setText("Schema");
        tfSelectedSchema.setMaximumSize(null);
        tfSelectedSchema.setMinimumSize(null);
        tfSelectedSchema.setPreferredSize(new java.awt.Dimension(402, 16));

        lblObject.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblObject.setLabelFor(tfSelectedObject);
        lblObject.setText("Object (Type):");
        lblObject.setPreferredSize(new java.awt.Dimension(87, 14));

        tfSelectedObject.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        tfSelectedObject.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        tfSelectedObject.setText("TableName");
        tfSelectedObject.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        tfSelectedObject.setMaximumSize(null);
        tfSelectedObject.setMinimumSize(null);
        tfSelectedObject.setPreferredSize(new java.awt.Dimension(402, 16));

        lblGeometryColumn.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblGeometryColumn.setLabelFor(cmbGeoColumns);
        lblGeometryColumn.setText("Geometry Column:");

        cmbGeoColumns.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        cmbGeoColumns.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "GEOMETRY" }));
        cmbGeoColumns.setMaximumSize(null);
        cmbGeoColumns.setMinimumSize(null);
        cmbGeoColumns.setPreferredSize(new java.awt.Dimension(230, 20));
        cmbGeoColumns.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbGeoColumnsActionPerformed(evt);
            }
        });

        lblSRID.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblSRID.setLabelFor(tfSRID);
        lblSRID.setText("SRID:");
        lblSRID.setMaximumSize(null);
        lblSRID.setMinimumSize(null);
        lblSRID.setPreferredSize(new java.awt.Dimension(87, 14));

        tfSRID.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        tfSRID.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        tfSRID.setBorder(null);
        tfSRID.setMargin(new java.awt.Insets(0, 0, 0, 0));
        tfSRID.setMaximumSize(null);
        tfSRID.setMinimumSize(null);
        tfSRID.setPreferredSize(new java.awt.Dimension(69, 24));

        btnSRID.setText("Select SRID");
        btnSRID.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btnSRID.setMaximumSize(null);
        btnSRID.setMinimumSize(null);
        btnSRID.setPreferredSize(new java.awt.Dimension(89, 24));
        btnSRID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSRIDActionPerformed(evt);
            }
        });

        pnlConnection.setBorder(javax.swing.BorderFactory.createTitledBorder("Connection"));
        pnlConnection.setMaximumSize(null);

        lblDBConnection.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblDBConnection.setLabelFor(cmbConnections);
        lblDBConnection.setText("Name:");
        lblDBConnection.setMaximumSize(null);
        lblDBConnection.setMinimumSize(null);
        lblDBConnection.setPreferredSize(new java.awt.Dimension(43, 14));

        cmbConnections.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        cmbConnections.setLightWeightPopupEnabled(false);
        cmbConnections.setMaximumSize(null);
        cmbConnections.setMinimumSize(null);
        cmbConnections.setPreferredSize(new java.awt.Dimension(231, 18));
        cmbConnections.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbConnectionsActionPerformed(evt);
            }
        });

        lblConnectedSchema.setLabelFor(tfConnectedSchema);
        lblConnectedSchema.setText("Schema:");
        lblConnectedSchema.setMaximumSize(null);
        lblConnectedSchema.setMinimumSize(null);
        lblConnectedSchema.setPreferredSize(new java.awt.Dimension(43, 14));

        tfConnectedSchema.setEditable(false);
        tfConnectedSchema.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        tfConnectedSchema.setText("userName");
        tfConnectedSchema.setBorder(null);
        tfConnectedSchema.setMaximumSize(null);
        tfConnectedSchema.setPreferredSize(new java.awt.Dimension(231, 20));

        btnReloadConnections.setText("Reload Connections");
        btnReloadConnections.setMaximumSize(null);
        btnReloadConnections.setMinimumSize(null);
        btnReloadConnections.setPreferredSize(new java.awt.Dimension(135, 23));
        btnReloadConnections.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReloadConnectionsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlConnectionLayout = new javax.swing.GroupLayout(pnlConnection);
        pnlConnection.setLayout(pnlConnectionLayout);
        pnlConnectionLayout.setHorizontalGroup(
            pnlConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlConnectionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnReloadConnections, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlConnectionLayout.createSequentialGroup()
                        .addGroup(pnlConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblDBConnection, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
                            .addComponent(lblConnectedSchema, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tfConnectedSchema, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                            .addComponent(cmbConnections, 0, 1, Short.MAX_VALUE))))
                .addContainerGap())
        );
        pnlConnectionLayout.setVerticalGroup(
            pnlConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlConnectionLayout.createSequentialGroup()
                .addGroup(pnlConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDBConnection, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cmbConnections, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlConnectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblConnectedSchema, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tfConnectedSchema, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnReloadConnections, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(10, 10, 10))
        );

        pnlDimElements.setBorder(javax.swing.BorderFactory.createTitledBorder("Dim Array Elements"));
        pnlDimElements.setMaximumSize(null);
        pnlDimElements.setPreferredSize(new java.awt.Dimension(809, 210));

        btnCalculateDiminfo.setMnemonic('M');
        btnCalculateDiminfo.setText("Calculate Elements");
        btnCalculateDiminfo.setMaximumSize(null);
        btnCalculateDiminfo.setMinimumSize(null);
        btnCalculateDiminfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCalculateDiminfoActionPerformed(evt);
            }
        });

        lblSample.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblSample.setLabelFor(sldrSample);
        lblSample.setText("Sample:");
        lblSample.setMaximumSize(null);
        lblSample.setMinimumSize(null);
        lblSample.setPreferredSize(new java.awt.Dimension(52, 23));

        sldrSample.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        sldrSample.setMajorTickSpacing(25);
        sldrSample.setMinorTickSpacing(10);
        sldrSample.setPaintLabels(true);
        sldrSample.setPaintTicks(true);
        sldrSample.setValue(100);
        sldrSample.setMaximumSize(null);
        sldrSample.setMinimumSize(null);
        sldrSample.setPreferredSize(new java.awt.Dimension(355, 44));
        sldrSample.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrSampleStateChanged(evt);
            }
        });

        lblPercentage.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblPercentage.setText("%");
        lblPercentage.setMaximumSize(null);
        lblPercentage.setMinimumSize(null);
        lblPercentage.setPreferredSize(new java.awt.Dimension(45, 14));

        btnAddElement.setText("Add Element");
        btnAddElement.setMaximumSize(null);
        btnAddElement.setMinimumSize(null);
        btnAddElement.setPreferredSize(new java.awt.Dimension(123, 23));
        btnAddElement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddElementActionPerformed(evt);
            }
        });

        scrollUserSdoMetadataTable.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollUserSdoMetadataTable.setMaximumSize(null);
        scrollUserSdoMetadataTable.setMinimumSize(null);
        scrollUserSdoMetadataTable.setPreferredSize(new java.awt.Dimension(777, 103));

        userSdoMetadataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Dim Name", "SDO_LB", "SDO_UB", "SDO_TOLERANCE"
            }
            ) 
        {
			private static final long serialVersionUID = 3536548795112343013L;
			
			@SuppressWarnings("rawtypes")
			Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        userSdoMetadataTable.setFillsViewportHeight(true);
        userSdoMetadataTable.setMaximumSize(null);
        userSdoMetadataTable.setMinimumSize(null);
        scrollUserSdoMetadataTable.setViewportView(userSdoMetadataTable);

        javax.swing.GroupLayout pnlDimElementsLayout = new javax.swing.GroupLayout(pnlDimElements);
        pnlDimElements.setLayout(pnlDimElementsLayout);
        pnlDimElementsLayout.setHorizontalGroup(
            pnlDimElementsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDimElementsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlDimElementsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlDimElementsLayout.createSequentialGroup()
                        .addComponent(btnCalculateDiminfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblSample, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sldrSample, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(lblPercentage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(186, 186, 186))
                    .addGroup(pnlDimElementsLayout.createSequentialGroup()
                        .addComponent(scrollUserSdoMetadataTable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(pnlDimElementsLayout.createSequentialGroup()
                        .addComponent(btnAddElement, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(664, 664, 664))))
        );
        pnlDimElementsLayout.setVerticalGroup(
            pnlDimElementsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDimElementsLayout.createSequentialGroup()
                .addGroup(pnlDimElementsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlDimElementsLayout.createSequentialGroup()
                        .addGroup(pnlDimElementsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(btnCalculateDiminfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblSample, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(27, 27, 27))
                    .addGroup(pnlDimElementsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sldrSample, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblPercentage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollUserSdoMetadataTable, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnAddElement, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        btnUpdate.setMnemonic('U');
        btnUpdate.setText("Apply Changes");
        btnUpdate.setMaximumSize(null);
        btnUpdate.setMinimumSize(null);
        btnUpdate.setPreferredSize(new java.awt.Dimension(123, 23));
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelObjectMetadataLayout = new javax.swing.GroupLayout(panelObjectMetadata);
        panelObjectMetadata.setLayout(panelObjectMetadataLayout);
        panelObjectMetadataLayout.setHorizontalGroup(
            panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelObjectMetadataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlDimElements, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelObjectMetadataLayout.createSequentialGroup()
                        .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblGeometryColumn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblSchema, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblObject, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblSRID, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tfSelectedObject, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tfSelectedSchema, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelObjectMetadataLayout.createSequentialGroup()
                                .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(cmbGeoColumns, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(panelObjectMetadataLayout.createSequentialGroup()
                                        .addComponent(tfSRID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnSRID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGap(172, 172, 172)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlConnection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnUpdate, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        panelObjectMetadataLayout.setVerticalGroup(
            panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelObjectMetadataLayout.createSequentialGroup()
                .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelObjectMetadataLayout.createSequentialGroup()
                        .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelObjectMetadataLayout.createSequentialGroup()
                                .addComponent(lblSchema, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(7, 7, 7))
                            .addGroup(panelObjectMetadataLayout.createSequentialGroup()
                                .addComponent(tfSelectedSchema, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblObject, javax.swing.GroupLayout.DEFAULT_SIZE, 15, Short.MAX_VALUE)
                            .addComponent(tfSelectedObject, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblGeometryColumn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cmbGeoColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(11, 11, 11)
                        .addGroup(panelObjectMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblSRID, javax.swing.GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE)
                            .addComponent(tfSRID, javax.swing.GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE)
                            .addComponent(btnSRID, javax.swing.GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE))
                    .addComponent(pnlConnection, 0, 106, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnlDimElements, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnUpdate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tfSelectedObject.getAccessibleContext().setAccessibleName("");
        pnlDimElements.getAccessibleContext().setAccessibleParent(panelObjectMetadata);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelObjectMetadata, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelCurrentMetadata, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(btnHelp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(717, 717, 717)
                        .addComponent(btnClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelObjectMetadata, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelCurrentMetadata, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnHelp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        panelCurrentMetadata.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        this.setAlwaysOnTop(false);
        // Write changes to disk.
        boolean OK = updateMetadata();
        if ( OK ) {
            // Because update will change associated row in current metadata table, refresh it.
            //
            this.metadataTM.readMetadataIntoTable(this.conn,this.userName,this.cbAllSchemas.isSelected());
            // Optimise the column sizes
            //
            Tools.autoResizeColWidth(this.metadataTable, null); 
        }
        // Bring back to front.
        this.setAlwaysOnTop(true);
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHelpActionPerformed
        //ClassLoader cl = this.getClass().getClassLoader();    
        //String p = cl.getResource("org/GeoRaptor/SpatialView/images/map_mouse_position.png").toString();
        //new JLabel("<html><table cellpadding=0><tr><td><img src='" + p + "'></td></tr><tr><td>100</td></tr></table></html>") );
        HtmlHelp hh = new HtmlHelp(this.propertyManager.getMsg("HELP_TITLE"),
                                   this.propertyManager.getMsg("HELP_BORDER"),
                                   this.propertyManager.getMsg("HELP_CONTENT"));
        hh.display();
    }//GEN-LAST:event_btnHelpActionPerformed

    private void btnSRIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSRIDActionPerformed
        this.setAlwaysOnTop(false);
        // get SRID to pass to SRIDPanel
        //
        String srid = this.tfSRID.getText().trim();
        srid = (Strings.isEmpty(srid) 
                 ? this.metadataPreferences.getSRID()
                 : srid);
        // Pass SRID to SRIDPanel
        //
        SRIDPanel sp = SRIDPanel.getInstance();
        boolean status = sp.initialise(conn,srid);
        if (status == true) {
            sp.setLocationRelativeTo(this);
            sp.setVisible(true);
            if ( ! sp.formCancelled() ) {
                tfSRID.setText(sp.getSRID());
            }
        }
        this.setAlwaysOnTop(true);
    }//GEN-LAST:event_btnSRIDActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        this.cleanUp();
        this.setAlwaysOnTop(false);
        this.setVisible(false);
    }//GEN-LAST:event_btnCloseActionPerformed

    private void cmbConnectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbConnectionsActionPerformed
        if (cmbConnections.getItemCount()>0 ) {
            int selectedIndex = this.cmbConnections.getSelectedIndex();
            if (selectedIndex!=-1) {
                DatabaseConnection dc = this.dbConnections.getConnectionAt(selectedIndex);
                this.conn = dc.getConnection();
                this.tfConnectedSchema.setText(dc.getCurrentSchema());
                this.userName = dc.getCurrentSchema();
                if ( ! this.cbAllSchemas.isSelected() ) {
                    panelCurrentMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("All metadata for user " + dc.getCurrentSchema()));
                    this.metadataTM.readMetadataIntoTable(this.conn,this.userName,this.cbAllSchemas.isSelected());
                    // Optimize the column sizes
                    Tools.autoResizeColWidth(this.metadataTable, null); 
                } else
                    panelCurrentMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("All metadata for all users"));
            }
        }
    }//GEN-LAST:event_cmbConnectionsActionPerformed

    private void cbMissingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbMissingActionPerformed
        metadataTM.fireTableDataChanged();
    }//GEN-LAST:event_cbMissingActionPerformed

    private void cbOrphanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbOrphanActionPerformed
        metadataTM.fireTableDataChanged();
    }//GEN-LAST:event_cbOrphanActionPerformed

    private void cbObjectExistsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbObjectExistsActionPerformed
        metadataTM.fireTableDataChanged();
    }//GEN-LAST:event_cbObjectExistsActionPerformed

    private void btnAddElementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddElementActionPerformed
        this.userTM.addRow(new MetadataRow("",0D,1D,0.005D));
        this.userTM.fireTableDataChanged();
    }//GEN-LAST:event_btnAddElementActionPerformed

    private void btnCalculateDiminfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCalculateDiminfoActionPerformed
        retrieveDimElementInformation();
    }//GEN-LAST:event_btnCalculateDiminfoActionPerformed

    private void sldrSampleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldrSampleStateChanged
        lblPercentage.setText(String.valueOf(sldrSample.getValue()) + "%" );
    }//GEN-LAST:event_sldrSampleStateChanged

    private void cmbGeoColumnsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbGeoColumnsActionPerformed
        if ( this.cmbGeoColumns.isEnabled() ) {
            if (this.cmbGeoColumns.getItemCount() == 0) {
                this.tfSelectedObject.setText(Strings.append(this.schemaName,
                                                           this.objectName,
                                                           Constants.TABLE_COLUMN_SEPARATOR) +
                                          " (" + this.objectType + ")");
            } else {
                this.columnName = this.cmbGeoColumns.getSelectedItem().toString();
                swapGeometryColumn();
            }
        }
    }//GEN-LAST:event_cmbGeoColumnsActionPerformed

    private void cbAllSchemasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbAllSchemasActionPerformed
        this.metadataTM.readMetadataIntoTable(this.conn,this.userName,this.cbAllSchemas.isSelected());
        if ( this.cbAllSchemas.isSelected() ) {
            panelCurrentMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("All metadata for all users"));
        } else
            panelCurrentMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("All metadata for user " + this.userName));
        // Optimize the column sizes
        Tools.autoResizeColWidth(this.metadataTable, null); 
    }//GEN-LAST:event_cbAllSchemasActionPerformed

    private void btnReloadConnectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReloadConnectionsActionPerformed
        // 1. Save current selected connection
        //
        String currentDisplayName = (String)this.cmbConnections.getSelectedItem();
        // 2. Refresh/reload connections from SQL Developer
        //
        this.dbConnections = DatabaseConnections.getInstance();
        this.dbConnections.setConnections(true);
        if ( this.dbConnections.getConnectionCount()==0 ) {
            JOptionPane.showConfirmDialog(this,propertyManager.getMsg("MD_NO_CONNECTION_FOR","Metadata Manager"));
            return;
        }
        // 3. Replace current connection if old is not in new
        //
        if ( this.dbConnections.findDisplayName(currentDisplayName) == null ) {
            this.conn = this.dbConnections.getConnectionAt(0).getConnection();
            try {
                this.schemaName = this.conn.getCurrentSchema();
            } catch (SQLException e) {
            }
            this.userName   = this.schemaName;
        }
        // 4. Initialize connection pulldown to, possible, new selection
        //
        String[] dbNames = this.dbConnections.getConnectionNames(null); 
        //this.cmbConnections.setModel((ComboBoxModel<String>) new javax.swing.DefaultComboBoxModel<String>(dbNames));
        this.cmbConnections.setModel(new DefaultComboBoxModel<String>(dbNames));

        if ( this.cmbConnections.getItemCount()==1 ) {
            this.cmbConnections.getModel().setSelectedItem((String)this.cmbConnections.getItemAt(0));
        } else {
            for ( int i=0; i<this.cmbConnections.getItemCount(); i++ ) {
                DatabaseConnection dbConn = this.dbConnections.getConnectionAt(i);
                if ( dbConn!=null && dbConn.getCurrentSchema().equalsIgnoreCase(this.schemaName) ) {
                    this.cmbConnections.setSelectedIndex(i);
                    break;
                }
            }
        }

    }//GEN-LAST:event_btnReloadConnectionsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddElement;
    private javax.swing.JButton btnCalculateDiminfo;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnHelp;
    private javax.swing.JButton btnReloadConnections;
    private javax.swing.JButton btnSRID;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JCheckBox cbAllSchemas;
    private javax.swing.JCheckBox cbMissing;
    private javax.swing.JCheckBox cbObjectExists;
    private javax.swing.JCheckBox cbOrphan;
    private javax.swing.JComboBox<String> cmbConnections;
    private javax.swing.JComboBox<String> cmbGeoColumns;
    private javax.swing.JLabel lblConnectedSchema;
    private javax.swing.JLabel lblDBConnection;
    private javax.swing.JLabel lblGeometryColumn;
    private javax.swing.JLabel lblObject;
    private javax.swing.JLabel lblPercentage;
    private javax.swing.JLabel lblSRID;
    private javax.swing.JLabel lblSample;
    private javax.swing.JLabel lblSchema;
    private javax.swing.JTable metadataTable;
    private javax.swing.JPanel panelCurrentMetadata;
    private javax.swing.JPanel panelObjectMetadata;
    private javax.swing.JPanel pnlConnection;
    private javax.swing.JPanel pnlDimElements;
    private javax.swing.JScrollPane scrollUserSdoMetadataTable;
    private javax.swing.JScrollPane scrollmetadataTable;
    private javax.swing.JSlider sldrSample;
    private javax.swing.JTextField tfConnectedSchema;
    private javax.swing.JTextField tfSRID;
    private javax.swing.JLabel tfSelectedObject;
    private javax.swing.JLabel tfSelectedSchema;
    private javax.swing.JTable userSdoMetadataTable;
    // End of variables declaration//GEN-END:variables

    /** ====================================================
     * Non-Netbeans, GeoRaptor, code
     **/

     private void cleanUp() {
         this.schemaName = null;
         this.objectName = null;
         this.columnName = null;
         this.userName   = null;
         this.objectType = null;
         this.dbConnections.removeAll();
         this.dbConnections = null;
         if ( this.cmbGeoColumns != null ) this.cmbGeoColumns.removeAllItems();
         this.popupMenu = null;
         if (metadataTM != null && metadataTM.getRowCount() > 0) metadataTM.clearModel();
         if (this.userTM != null && this.userTM.getRowCount() > 0) this.userTM.clearModel();
         this.removeSelectionModelsAndListeners();
         this.conn       = null;
     }

    private void removeSelectionModelsAndListeners() 
    {
        if ( this.metadataLSM != null )
            this.metadataLSM = null;
        if ( ( this.userSdoMetadataTable.getKeyListeners() != null ) &&
             ( this.userSdoMetadataTable.getKeyListeners().length != 0 ) ) 
            this.userSdoMetadataTable.removeKeyListener(this.userSdoMetadataTable.getKeyListeners()[0]);
        if ( this.userLSM != null )
            this.userLSM = null;
    }

     private void setSelectionModelsAndListeners()
     {
         this.metadataLSM = this.metadataTable.getSelectionModel();
         this.metadataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
         this.metadataTable.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 &&
                        SwingUtilities.isLeftMouseButton(e)) 
                    {
                        // This action overwrites all UserWidgets including table of dim_elements
                        JTable target = (JTable)e.getSource();
                        int vrow = target.getSelectedRow();
                        if (vrow==-1) return;
                        int row = metadataTable.convertRowIndexToModel(vrow);
                        MetadataEntry mEntry = metadataEntryAtPoint(row);
                        if ( mEntry!=null )
                            populateUserWidgets(mEntry);
                    } else if ( e.getClickCount()==1 && 
                                SwingUtilities.isRightMouseButton(e) )
                    {
                        // Is there anything under the mouse? 
                        //
                        if ( metadataTable.getSelectedRowCount() != 0 ) {
                            int row = metadataTable.rowAtPoint(e.getPoint());
                            if (row == -1) return;
                            row = metadataTable.convertRowIndexToModel(row);
                            createRightMouseClickMenu(metadataTable.getSelectedRowCount(),
                                                      row);
                            // Show right popup menu
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });

         this.userLSM = this.userSdoMetadataTable.getSelectionModel();
         this.userSdoMetadataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         // add key listener for "Delete" key
         // Since metadata view display entries for existing table.sdo_geometry columns, 
         // should we do this?
         //
         this.userSdoMetadataTable.addKeyListener(new KeyAdapter() 
         {
             @Override
             public void keyPressed(KeyEvent e) {
                 if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                     deleteDimElement( );
                 }
             }
         });
     }

    private void createRightMouseClickMenu(final int _selectedRowCount,
                                           final int _rowAtPoint) {
        popupMenu = new JPopupMenu();
        String copyMenuItem = _selectedRowCount==1
                          ? "MENU_COPY_METADATA_RECORD"
                          : "MENU_COPY_METADATA_RECORDS";
        AbstractAction copyMetadataRecord = 
            new AbstractAction(this.propertyManager.getMsg(copyMenuItem)) {
              private static final long serialVersionUID = -7029148216864008780L;

			  public void actionPerformed(ActionEvent e) {
                  processMenuSelection(MetadataPanel.MENU_SELECTION.COPY);
              }
          };
        popupMenu.add(copyMetadataRecord);
        
        String deleteMenuItem = _selectedRowCount==1
                          ? "MENU_DELETE_METADATA_RECORD"
                          : "MENU_DELETE_METADATA_RECORDS";
        AbstractAction deleteMetadataRecord = 
            new AbstractAction(this.propertyManager.getMsg(deleteMenuItem)) {
				private static final long serialVersionUID = 4867573240206451437L;

     			public void actionPerformed(ActionEvent e) {
                  processMenuSelection(MetadataPanel.MENU_SELECTION.DELETE);
              }
          };
        popupMenu.add(deleteMetadataRecord);
        
        if ( _selectedRowCount==1 && _rowAtPoint!=-1 ) {
            AbstractAction adoptMetadataRecord = 
                new AbstractAction(this.propertyManager.getMsg("MENU_ADOPT_DIMINFO")) 
              {
					private static final long serialVersionUID = 5525313313067060777L;

     				public void actionPerformed(ActionEvent e) {
                      MetadataEntry mEntry = metadataEntryAtPoint(_rowAtPoint);
                      // Copy dimInfo
                      //
                      copyDiminfoIntoTable(mEntry);
                    }
              };
            popupMenu.add(adoptMetadataRecord);

            AbstractAction switchMetadataRecord = 
                new AbstractAction(this.propertyManager.getMsg("MENU_SWITCH_METADATA")) 
                {
					private static final long serialVersionUID = -2552626980346847490L;

                    public void actionPerformed(ActionEvent e) {
                      MetadataEntry mEntry = metadataEntryAtPoint(_rowAtPoint);
                      // This action overwrites all UserWidgets including table of dim_elements
                      populateUserWidgets(mEntry);
                    }
                };
            popupMenu.add(switchMetadataRecord);
        }
    }
    
    private void processMenuSelection(MENU_SELECTION _selection) 
    {
        StringBuffer sBuffer = new StringBuffer();
        int rows[] = this.metadataTable.getSelectedRows();
        if ( rows.length == 0 ) {
            // Process one if exists?
            Point i = this.popupMenu.getLocation();
            int row = this.metadataTable.rowAtPoint(i);
            if ( row != -1 ) {
                this.metadataLSM.setSelectionInterval(row, row);
                row = this.metadataTable.convertRowIndexToModel(row);
                MetadataEntry mEntry = (MetadataEntry)this.metadataTM.getValueAt(row,this.metadataTM.mEntryPosn);
                if ( _selection == MENU_SELECTION.COPY ) { 
                    sBuffer.append(mEntry.toString());
                } else { 
                    if ( ! mEntry.isMissing() ) {
                        final int rid = metadataTM.indexOf(mEntry.getFullName());
                        deleteMDRow(mEntry);
                        if ( mEntry.isOrphan() ) {
                            // delete model entry and refresh
                            //
                            SwingUtilities.invokeLater(new Runnable() 
                            {
                                public void run() 
                                {
                                    metadataTM.removeRow(rid);
                                }
                            });

                        } else {
                            mEntry.setMissing(true);
                            mEntry.removeAll();
                            final MetadataEntry fmEntry = mEntry;
                            SwingUtilities.invokeLater(new Runnable() 
                            {
                                public void run() 
                                {
                                    metadataTM.setValueAt(fmEntry, 
                                                          rid,
                                                          metadataTM.mEntryPosn);
                                }
                            });
                        }
                    }
                }
            }
        } else if ( rows.length >= 1 ) {
            int viewRow = -1;
            for (int iRow=0; iRow<rows.length; iRow++) {
                viewRow = this.metadataTable.convertRowIndexToModel(rows[iRow]);
                MetadataEntry mEntry = (MetadataEntry)this.metadataTM.getValueAt(viewRow,this.metadataTM.mEntryPosn);
                if ( _selection == MENU_SELECTION.COPY ) {
                    sBuffer.append(mEntry.toString() + (iRow==0||iRow==(rows.length-1)?"":"\n"));
                } else { 
                    if ( ! mEntry.isMissing() ) {
                        deleteMDRow(mEntry);
                        if ( mEntry.isOrphan() ) {
                            // delete model entry and refresh
                            //
                            final int row = viewRow;
                            SwingUtilities.invokeLater(new Runnable() 
                            {
                                public void run() 
                                {
                                    metadataTM.removeRow(row);
                                }
                            });
                        } else {
                            mEntry.setMissing(true);
                            mEntry.removeAll();
                            final MetadataEntry fmEntry = mEntry;
                            SwingUtilities.invokeLater(new Runnable() 
                            {
                                public void run() 
                                {
                                    metadataTM.setValueAt(fmEntry, 
                                                          metadataTM.indexOf(fmEntry.getFullName()), 
                                                          metadataTM.mEntryPosn);
                                }
                            });
                        }
                    } // ! isMissing()
                }
            }
        }
        if ( _selection == MENU_SELECTION.COPY && sBuffer.length() > 0 ) {
            Toolkit.getDefaultToolkit().beep();
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection ss = new StringSelection(sBuffer.toString());
            clipboard.setContents(ss, ss);
        }
    }
    
    public boolean initialise(OracleConnection _conn, 
                              String           _schemaName,
                              String           _objectName,
                              String           _columnName,
                              String           _userName) 
    throws Exception 
    {
        // 1. Set up connections
        //
        this.dbConnections = DatabaseConnections.getInstance();
        this.dbConnections.setConnections(true);
        if ( this.dbConnections.getConnectionCount()==0 ) {
            throw new Exception(propertyManager.getMsg("MD_NO_CONNECTION_FOR","Metadata Manager"));
        }
        this.conn = (_conn!=null)? _conn : this.dbConnections.getConnectionAt(0).getConnection();
        this.schemaName = Strings.isEmpty(_schemaName)?this.conn.getCurrentSchema():_schemaName;
        this.userName = Strings.isEmpty(_userName)  ?this.schemaName             :_userName;

        // 2. Initialize connection pulldown
        //
        this.cmbConnections.setModel(this.dbConnections.getComboBoxModel(this.dbConnections.getConnectionAt(0).getConnectionName()));
        if ( this.cmbConnections.getItemCount()==1 ) {
            this.cmbConnections.getModel().setSelectedItem((String)this.cmbConnections.getItemAt(0));
        } else {
            for ( int i=0; i<this.cmbConnections.getItemCount(); i++ ) {
                DatabaseConnection dbConn = this.dbConnections.getConnectionAt(i);
                if ( dbConn!=null && dbConn.getCurrentSchema().equalsIgnoreCase(this.conn.getCurrentSchema()) ) {
                    this.cmbConnections.setSelectedIndex(i);
                    break;
                }
            }
        }
        
        // Initialise SRID 
        //
        this.tfSRID.setText( this.metadataPreferences.getSRID() );

        // 3.1 Load metadata into table
        //
        String check = this.metadataTM.readMetadataIntoTable(this.conn,this.userName, Strings.isEmpty(_schemaName));
        if ( check.equalsIgnoreCase(this.sCancelled) ) {
            if (Strings.isEmpty(_schemaName) ) {
                // Launched from View menu
                JOptionPane.showConfirmDialog(this, "Metadata manager terminated at your request.");
            } else {
                throw new Exception("Metadata manager terminated at your request.");
            }
        }
        
        // 3.2 Optimise the column sizes
        //
        Tools.autoResizeColWidth(this.metadataTable, null);

        // 4. Set cbAllSchemas based on original _schemaName
        //
        this.cbAllSchemas.setSelected(Strings.isEmpty(_schemaName));

        // 5. Set Border to reflect where metadata has come from
        //
        if (Strings.isEmpty(_schemaName)) {
            panelCurrentMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("All metadata for all users."));
        } else {
            panelCurrentMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("All metadata for user " + this.schemaName));
        }

        // 6. Populate User Widgets
        //    If nothing in top table (currTable) then no metadata for object exists
        //    it will be tagged as isMissing()
        //
        if ( _conn==null ) {
            populateUserWidgets((MetadataEntry)this.metadataTM.getValueAt(0, this.metadataTM.mEntryPosn));
        } else {
            populateUserWidgets(_objectName,_columnName);
        }
        
        // 7. Set up Models and Listeners
        //
        setSelectionModelsAndListeners();
        
        this.setResizable(true);
        
        // Always show dialog relative to calling SQL Developer window
        this.setLocationRelativeTo(super.getComponent(0));

        return true;
    }

     private void populateUserWidgets(MetadataEntry _mEntry) 
     {
         // If we have been given a MetadataEntry then it came from the database
         //
         // 1. Populate key properties
         //
         this.schemaName = _mEntry.getSchemaName();
         this.objectName = _mEntry.getObjectName();
         this.columnName = _mEntry.getColumnName();
         
         // 2. Populate columns combo with single item
         //
         this.cmbGeoColumns.setEnabled(false);  // stop firing change events
         this.cmbGeoColumns.setModel(new DefaultComboBoxModel<String>(new String[]{this.columnName}));
         this.cmbGeoColumns.setSelectedIndex(0);
         this.cmbGeoColumns.setEnabled(true);  // stop firing change events
         this.cmbGeoColumns.setVisible(true);
         
         // 3. Set schema that owns object in the right widget
         //
         this.tfSelectedSchema.setText(this.schemaName);
         
         // 4. Set object type widget ie Table, View, Materialized View
         //
         this.objectType = "";
         try {
             this.objectType = MetadataTool.getObjectType(this.conn, this.schemaName, this.objectName);
         } catch (Exception e /* SQLException sqle and IllegalArgumentException */ ) {
             JOptionPane.showMessageDialog(null,
                                           propertyManager.getMsg("ERROR_MESSAGE_OBJECT_TYPE_NOT_FOUND",
                                                                 Strings.append(this.schemaName,
                                                                    this.objectName,
                                                                    Constants.TABLE_COLUMN_SEPARATOR)),
                                           MainSettings.EXTENSION_NAME,
                                           JOptionPane.INFORMATION_MESSAGE);
             this.objectType = "UNKNOWN";
         }
         
         // 5. Setup full object name
         //
         this.tfSelectedObject.setText(_mEntry.getFullName() + " (" + this.objectType + ")");
         
         // 6. set SRID of dialog
         //
         tfSRID.setText(Strings.isEmpty(_mEntry.getSRID()) ? Constants.NULL : _mEntry.getSRID().trim() );
         
         // 7. Copy dimInfo
         //
         copyDiminfoIntoTable(_mEntry);
    }

     private void populateUserWidgets(String _objectName,
                                      String _columnName) 
     { 
        String schemaUser = Strings.append(this.schemaName,_objectName, Constants.TABLE_COLUMN_SEPARATOR);
        
        // 1. Geometry column checks....
        //
        List<String>tableGeometryColumns = new ArrayList<String>(MetadataTool.validateColumnName(this.conn,
                                                                                                 this.schemaName,
                                                                                                 _objectName,
                                                                                                 _columnName));
        if ( tableGeometryColumns == null || tableGeometryColumns.size()==0 ) {
            throw new IllegalArgumentException(
                               propertyManager.getMsg("MD_TABLE_NO_SDO_GEOMETRY_COLUMN",
                                                      schemaUser));
        }

        // 2. Passed in schema/object/geometry column values are valid
        //
        this.tfSelectedSchema.setText(this.schemaName);
        this.objectName = _objectName;
        this.columnName = Strings.isEmpty(_columnName)?tableGeometryColumns.get(0):_columnName;
        String fullObjectName = Strings.objectString(this.schemaName,
                                                          this.objectName,
                                                          this.columnName,
                                                          Constants.TABLE_COLUMN_SEPARATOR);
        // 3. Set object type widget ie Table, View, Materialized View
        //
        this.objectType = "";
        try {
            this.objectType = MetadataTool.getObjectType(this.conn, this.schemaName, this.objectName);
        } catch (Exception e /* SQLException sqle and IllegalArgumentException */ ) {
            JOptionPane.showMessageDialog(null,
                                          propertyManager.getMsg("ERROR_MESSAGE_OBJECT_TYPE_NOT_FOUND",
                                                                 Strings.append(this.schemaName,
                                                                   this.objectName,
                                                                   Constants.TABLE_COLUMN_SEPARATOR)),
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.INFORMATION_MESSAGE);
            this.objectType = "UNKNOWN";
        }
        this.tfSelectedObject.setText(fullObjectName + " (" + this.objectType + ")");

        // 4. Copy geoColumn data to cmbGeoColumns
        //    We will rebuild the geometry columns combo box based on what we find
        //    with first item (0) being the column to process
        //
        this.cmbGeoColumns.setEnabled(false);  // stop firing change events
        this.cmbGeoColumns.setEditable(true);
        this.cmbGeoColumns.removeAllItems();
        this.cmbGeoColumns.setModel(new DefaultComboBoxModel<String>(tableGeometryColumns.toArray(new String[0])));
        if ( tableGeometryColumns.size()==1 ) {
            this.cmbGeoColumns.setSelectedIndex(0);
        } else {  // Passed in name is one of many sdo_geometry columns, find and set as selected
            for (int i = 0; i < this.cmbGeoColumns.getModel().getSize(); i++) {
                    String geometryName = this.cmbGeoColumns.getModel().getElementAt(i); 
                    if ( geometryName.equalsIgnoreCase(this.columnName)) {
                        this.cmbGeoColumns.setSelectedIndex(i);
                        break;
                    }
                }
        }
        this.cmbGeoColumns.setEnabled(true);  // Re-enable event firing
        this.cmbGeoColumns.setVisible(true);

        // 5. Find Associated MetadataEntry
        //
        MetadataEntry mEntry = this.metadataTM.find(fullObjectName);
        if ( mEntry == null )
            return;

        // 6. set SRID of dialog
        //
        tfSRID.setText(Strings.isEmpty(mEntry.getSRID()) ? Constants.NULL : mEntry.getSRID().trim() );
        
        // 7. Copy dimInfo
        //
        copyDiminfoIntoTable(mEntry);
        
        return;
    }
     
    private void swapGeometryColumn() 
    {
        // Schema and Object Name are the same
        //
        
        // Update the name of the geometry column
        //
        this.columnName = cmbGeoColumns.getSelectedItem().toString();
        String fullObjectName = Strings.objectString(this.schemaName,
                                                          this.objectName,
                                                          this.columnName);
        this.tfSelectedObject.setText(fullObjectName + " (" + this.objectType + ")");
        
        // Get SRID/Diminfo from metadata of this geometry column in metadataTable
        //
        MetadataEntry mEntry = metadataTM.find(fullObjectName);
        if ( mEntry == null )
            return;

        // Set SRID of dialog
        //
        tfSRID.setText(Strings.isEmpty(mEntry.getSRID()) ? Constants.NULL : mEntry.getSRID().trim() );
        
        // Copy dimInfo elements
        //
        copyDiminfoIntoTable(mEntry);
    }

    protected void retrieveDimElementInformation() 
    {
        // show and perform MBR calculation
        String currentColumnName = this.cmbGeoColumns.getSelectedItem().toString();
        final RetrieveDimElementThread work = 
           new RetrieveDimElementThread(this.conn,
                                        this.schemaName,
                                        this.objectName,
                                        currentColumnName,
                                        this.tfSRID.getText(),
                                        this.sldrSample.getValue());
        ProgressBar progress = new ProgressBar(this, "Calculate DimInfo Sdo_Dim_Elements", work, true);
        progress.setCancelable(true);
        work.setProgressBar(progress);
        progress.start("Calculating " + Strings.append(this.objectName,currentColumnName,".") + "'s DimInfo Sdo_Dim_Elements, please wait...", null);
        // get and set MBR
        if (!Strings.isEmpty(work.getErrorMessage())) { 
            // show error message
            this.setAlwaysOnTop(false);
            JOptionPane.showMessageDialog(null, 
                                          work.getErrorMessage(),
                                          MetadataPanel.CALC_MBR_ERROR,
                                          JOptionPane.ERROR_MESSAGE);
            this.setAlwaysOnTop(true);
        } else {
            MetadataEntry me = work.getMetadataEntry();
            if (me != null && me.getEntryCount() > 0 ) {
                // MBR operation successful 
                // 
                SwingUtilities.invokeLater(new Runnable() 
                {
                    public void run() 
                    {
                        List<MetadataRow> meRows = work.getMetadataEntry().getEntries();
                        for ( int i = 0; i < meRows.size(); i++ ) {
                            userTM.setRow(meRows.get(i),i);
                        }
                        tfSRID.setText(work.mbrSRID);
                    }
                });
            } 
        }
        // reset
        progress = null;
    }

    protected boolean updateMetadata() 
    {
        // create SQL
        String sql = null;

        // Get current column
        String targetColumn = this.cmbGeoColumns.getSelectedItem().toString();

        // Get SRID
        //
        String srid = this.tfSRID.getText();

        // create DimInfo structure
        //
        String DimInfo = null;
        try {
            DimInfo = this.userTM.getDimArray();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, 
                                          ex.getMessage(),
                                          UPDATE_METADATA_ERROR,
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        try 
        {
            // Prepare update SQL
            //
            if ( this.schemaName.equalsIgnoreCase(this.conn.getUserName()) ) {
                sql = userSdoGeomMetadataSQL(this.schemaName,
                                             this.objectName,
                                             targetColumn,
                                             srid,
                                             DimInfo);
            } else {
                if (MetadataTool.checkCrossSchemaDMLPermissions(this.conn) )
                    sql = sdoGeomMetadataTableSQL(this.schemaName,
                                                  this.objectName,
                                                  targetColumn,
                                                  srid,
                                                  DimInfo);
                else
                    throw new Exception(propertyManager.getMsg("ERROR_CROSS_SCHEMA_UPDATE",this.userName));
            }
            // Now execute the update
            //
            Statement st = null;
            st = this.conn.createStatement();
            st.executeUpdate(sql);
            this.conn.commit();
            st.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, 
                                          ex.getMessage(), 
                                          UPDATE_METADATA_ERROR,
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Finally, say OK
        //
        JOptionPane.showMessageDialog(null,
                                      propertyManager.getMsg("MD_SUCCESSFUL_UPDATE", 
                                    		  Strings.objectString(this.schemaName,
                                                                                       this.objectName,
                                                                                       targetColumn)).toString(),
                                      MainSettings.EXTENSION_NAME,
                                      JOptionPane.INFORMATION_MESSAGE);

        return true;
    }

    private String userSdoGeomMetadataSQL(String _schemaName,
                                          String _objectName,
                                          String _columnName,
                                          String _srid,
                                          String _DimInfo) 
    throws SQLException
    {
        String sql = null;

        if (MetadataTool.hasGeomMetadataEntry(this.conn,_schemaName,_objectName,_columnName) ) 
            sql = "UPDATE user_sdo_geom_metadata usgm \n " +
                  "   SET usgm.SRID = " + _srid + ", \n" +
                  "       usgm.DimInfo = " + _DimInfo + "\n" +
                  " WHERE usgm.TABLE_NAME = '" + _objectName.toUpperCase() + "' \n" +
                  "   AND usgm.COLUMN_NAME = '" + _columnName + "'";
        else 
            sql = "INSERT INTO user_sdo_geom_metadata (table_name,column_name,DimInfo,srid) \n" +
                  "VALUES ('" + _objectName.toUpperCase() + "', '" + _columnName.toUpperCase() + "'," + _DimInfo + ", " + _srid + ")";
        return sql;
    }

    private String sdoGeomMetadataTableSQL(String _schemaName,
                                           String _objectName,
                                           String _columnName,
                                           String _srid,
                                           String _DimInfo) 
    {
        String nullAwareSchemaName =
            "NVL(" + (Strings.isEmpty(_schemaName) ? "NULL" : "'" + _schemaName.toUpperCase() + "'") +
            ",SYS_CONTEXT('USERENV','SESSION_USER'))";

        return "MERGE INTO mdsys.Sdo_Geom_Metadata_Table M \n" + 
          "USING (SELECT " + nullAwareSchemaName + " as owner, \n" + 
          "              '" + _objectName.toUpperCase() + "' as table_name, \n" +
          "              '" + _columnName.toUpperCase() + "' as column_name \n" +
          "         FROM dual \n" +
          "       ) S \n" + 
          "       On (     M.SDO_Owner       = S.Owner \n" + 
          "            And M.SDO_Table_Name  = S.Table_Name \n" + 
          "            And M.SDO_Column_Name = S.Column_Name \n" + 
          "           ) \n" +
          "WHEN MATCHED THEN \n" +
          "UPDATE SET m.SDO_srid  = " + _srid + ", \n" +
          "           m.SDO_DimInfo = " + _DimInfo + " \n" +
          "WHEN NOT MATCHED THEN \n" +
          "INSERT ( m.SDO_owner,m.SDO_table_name,m.SDO_column_name,m.SDO_DimInfo,m.SDO_srid ) \n" +
          "VALUES (" + nullAwareSchemaName + ", \n" +
          "        '" + _objectName.toUpperCase() + "', \n" +
          "        '" + _columnName.toUpperCase() + "', \n" +
          "       " + _DimInfo + ", \n" + 
          "       " + _srid + ")";
    }
    
    /**
     * @method userSdoGeomMetadataView
     * @param _objectName
     * @param _columnName
     * @param _srid
     * @param _DimInfo
     * @return String
     * @author Simon Greener, April 2010, Original coding
     * @author Simon Greener, 28th May 2010, Removed superfluous _schemaName and handling
     */
    @SuppressWarnings("unused")
	private String userSdoGeomMetadataView(String _objectName,
                                           String _columnName,
                                           String _srid,
                                           String _DimInfo) 
    {
        return "MERGE INTO User_Sdo_Geom_Metadata M \n" + 
          "USING (SELECT '" + _objectName.toUpperCase() + "' as table_name, \n" +
          "              '" + _columnName.toUpperCase() + "' as column_name \n" +
          "         FROM dual \n" +
          "       ) S \n" + 
          "       On (     M.Owner       = S.Owner \n" + 
          "            And M.Table_Name  = S.Table_Name \n" + 
          "            And M.Column_Name = S.Column_Name \n" + 
          "           ) \n" +
          "WHEN MATCHED THEN \n" +
          "UPDATE SET m.srid  = " + _srid + ", \n" +
          "           m.DimInfo = " + _DimInfo + " \n" +
          "WHEN NOT MATCHED THEN \n" +
          "INSERT ( m.owner,m.table_name,m.column_name,m.DimInfo,m.srid ) \n" +
          "VALUES ('" + _objectName.toUpperCase() + "', \n" +
          "        '" + _columnName.toUpperCase() + "', \n" +
          "       " + _DimInfo + "," + _srid + ")";
    }

    // Delete selected Dim Element
    //
    protected void deleteDimElement() 
    {
        int vrow                = this.userLSM.getMaxSelectionIndex();
        int lastSelectedElement = this.userSdoMetadataTable.convertRowIndexToModel(vrow);
        if ( lastSelectedElement != -1 
             &&
             lastSelectedElement < this.userTM.getRowCount() )
        {
            this.userTM.deleteRow(lastSelectedElement);
        }
    }

    /**
     * Delete select metadata row
     */
    protected void deleteMDRow(MetadataEntry _mEntry) 
    {
        final String fullObjectName = _mEntry.getFullName();
        // query options for for delete dialog
        //
        Object[] options = { propertyManager.getMsg("MD_DELETE_MB_YES_BUTTON"), 
                             propertyManager.getMsg("MD_DELETE_MB_NO_BUTTON") };

        this.setAlwaysOnTop(false);
        int selectStatus = JOptionPane.showOptionDialog(
                                  null, 
                                  propertyManager.getMsg("MD_DELETE_MB_QUERY", fullObjectName), 
                                  propertyManager.getMsg("MD_DIALOG_TILE"),
                                  JOptionPane.DEFAULT_OPTION,
                                  JOptionPane.WARNING_MESSAGE, null,
                                  options, 
                                  options[0]);
        this.setAlwaysOnTop(true);

        if (selectStatus != 0) { // not OK button
            return;
        }

        try {
            String sql = "";
            if ( _mEntry.getSchemaName().equalsIgnoreCase(this.userName) ) 
                sql = getSchemaDeleteSQL(_mEntry.getObjectName(), 
                                         _mEntry.getColumnName());
            else {
                if (MetadataTool.checkCrossSchemaDMLPermissions(this.conn) )
                    sql = getCrossSchemaDeleteSQL(_mEntry.getSchemaName(),
                                                  _mEntry.getObjectName(),
                                                  _mEntry.getColumnName());
                else
                    throw new Exception("Cannot execute cross-schema metadata delete.\n" +
                                        "Unless you: \n" +
                                        "GRANT DELETE ON MDSYS.SDO_GEOM_METADATA_TABLE TO PUBLIC (or " + this.userName + ")");
            }

            // delete MD entry
            Statement st = this.conn.createStatement();            
            st.execute(sql);
            this.conn.commit();
            st.close();            
        } catch (Exception ex) {
            this.setAlwaysOnTop(false);
            JOptionPane.showMessageDialog(null, 
                                          ex.getMessage(),
                                          UPDATE_METADATA_ERROR,
                                          JOptionPane.ERROR_MESSAGE);
            this.setAlwaysOnTop(true);
            // show error message
        }
    }
    
    private String getSchemaDeleteSQL(String _ObjectName,
                                      String _ColumnName) {
        String sql;
        sql = "DELETE FROM user_sdo_geom_metadata usgm \n" +
              " WHERE usgm.TABLE_NAME  = '" + _ObjectName.toUpperCase() + "' \n" +
              "   AND usgm.COLUMN_NAME = '" + _ColumnName.toUpperCase() + "'";
        return sql;
    }

    private String getCrossSchemaDeleteSQL(String _SchemaName,
                                           String _ObjectName,
                                           String _ColumnName) {
        String ownerClause = "NVL(" + (Strings.isEmpty(_SchemaName) ? "NULL" : "'" + _SchemaName.toUpperCase() + "'") + ",SYS_CONTEXT('USERENV','SESSION_USER'))) \n";
        String sql;
        sql = "DELETE FROM all_sdo_geom_metadata asgm \n" +
              " WHERE asgm.owner = " + ownerClause + 
              "   AND asgm.TABLE_NAME  = '" + _ObjectName.toUpperCase() + "' \n" +
              "   AND asgm.COLUMN_NAME = '" + _ColumnName.toUpperCase() + "'";
        return sql;
    }

    protected MetadataEntry metadataEntryAtPoint(int _index) 
    {
        if ((this.metadataTM == null) || 
            (_index < 0)              || 
            (_index >= this.metadataTM.getRowCount())) {
            return null;
        }
        // Get actual metadata entry associated with this index
        //
        MetadataEntry mEntry = (MetadataEntry)this.metadataTM.getValueAt(_index,this.metadataTM.mEntryPosn);
        if ( mEntry == null )
            return null;
        return mEntry;
    }

    protected void copyDiminfoIntoTable(final MetadataEntry _mEntry) {
        if ( _mEntry == null ) 
            return;
        SwingUtilities.invokeLater(new Runnable() 
        {
            public void run() 
            {
                userTM.addAllRows(_mEntry);
                tfSRID.setText(_mEntry.getSRID());
            }
        });
        
    }
        
    // ***********************************************************
    
    class UserMetadataTableModel extends AbstractTableModel 
    {

		private static final long serialVersionUID = -8575188589830357277L;
		
		MetadataEntry cache; // will hold metadata for a single data . . .
        
        public UserMetadataTableModel(String _schemaName,
                                          String _objectName,
                                          String _columnName,
                                          String _srid) {
            cache = new MetadataEntry(_schemaName,_objectName,_columnName,_srid);
       }

        String[] headers = new String[] { "Dimension Name", "Lower Bound Value", "Upper Bound Value", "Tolerance" };

        public void setEntry(String _schemaName, 
                             String _objectName,
                             String _columnName, 
                             String _srid) 
        {
            this.cache.schemaName = _schemaName;
            this.cache.objectName = _objectName;
            this.cache.columnName = _columnName;
            this.cache.SRID       = _srid;
        }
        
        public String getColumnName(int i) {
            return headers[i];
        }

        public int getColumnCount() {
            return headers.length;
        }

        public String[] getColumnNames() {
            return headers;
        }

        public int getRowCount() {
            return cache.getEntryCount();
        }

        public void addRow(MetadataRow _row) {
            cache.add(_row);
            if ( cache.getEntryCount() != 0 )
              fireTableRowsInserted(0,cache.getEntryCount()-1);
        }

        /**
         * @method setRowAt
         * @precis Updates LB/UB values based on row number
         * @param i
         * @param _row
         */
        public void setRow(MetadataRow _row, int i) 
        {
            int rowsAdded = 0;
            if ( i < cache.getEntryCount() ) {
                MetadataRow tr = cache.getEntry(i);
                // Update LB etc
                tr.setSdoLB(_row.getSdoLB()); 
                tr.setSdoUB(_row.getSdoUB());
                rowsAdded = cache.set(tr);
            }
            else {
                cache.add(_row);
                rowsAdded++;
            }
            if ( rowsAdded == 0 )
                this.fireTableDataChanged(); // CellUpdated(row, col);
            else
                this.fireTableRowsInserted(0,cache.getEntryCount()-1);
        }

        public void deleteRow(int _row) {
            if ( cache != null && cache.getEntryCount() > 0 && _row < cache.getEntryCount() ) {
                cache.remove(_row);
                this.fireTableRowsDeleted(0,cache.getEntryCount()-1);
            }
        }
        
        public void addAllRows(MetadataEntry _me) 
        {
            if ( _me == null ) {
                return;
            }
            this.clearRows();
            if ( _me.getEntryCount() != 0 ) {
                for (int i  = 0; i < _me.getEntryCount(); i++) {
                    cache.add(_me.getEntries().get(i));
                }
                this.fireTableRowsInserted(0,cache.getEntryCount()-1);
            }
        }

        public void setValueAt(Object value, int row, int col) 
        {
            if ( ! ( (canEdit[col]) && ( row < cache.getEntryCount()) ) ) {
                return;
            }
            MetadataRow tr = cache.getEntry(row);
            switch (col) {
            case 0: tr.setDimName((String)value); break;
            case 1: tr.setSdoLB((Double)value); break;
            case 2: tr.setSdoUB((Double)value); break;
            case 3: tr.setTol((Double)value); break;
            }
            cache.set(row,tr);
            this.fireTableDataChanged(); // CellUpdated(row, col);
        }

        public Object getValueAt(int _row, int _col) 
        {
            if (this.cache == null || this.cache.getEntryCount() == 0) 
                return "???";

            Object obj = null;
            MetadataRow tr = cache.getEntry(_row);
            if ( tr != null ) {
                switch (_col) 
                {
                     case 0 : { obj = tr.getDimName(); break; } 
                     case 1 : { obj = tr.getSdoLB(); break; } 
                     case 2 : { obj = tr.getSdoUB(); break; } 
                     case 3 : { obj = tr.getTol(); break; } 
                }
            }
            return obj;
        }
    
        @SuppressWarnings("rawtypes")
		Class[] types =
            new Class[] { java.lang.String.class, 
                          java.lang.Double.class, 
                          java.lang.Double.class, 
                          java.lang.Double.class };

        /**
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         **/
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return types[columnIndex];
        }
        
        boolean[] canEdit = new boolean[] { true, true, true, true };
    
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit[columnIndex];
        }

        public void addFromMetadata(OracleConnection _conn, 
                                    String           _schemaName,
                                    String           _objectName,
                                    String           _columnName )
        {
            this.clearModel();
            try {
                LinkedHashMap<String, MetadataEntry> 
                metaEntries = MetadataTool.getMetadata(_conn, 
                                             _schemaName,
                                             _objectName,
                                             _columnName,
                                             false);
                // We only ever load the first one (there should only be one!)
                //
                if ( metaEntries != null && metaEntries.size() > 0 ) {                    
                    @SuppressWarnings("unlikely-arg-type")
					MetadataEntry me = metaEntries.get(0);
                    for ( int j = 0; j < me.getEntryCount(); j++ ) 
                        cache.add(me.getEntries().get(j));
                    // notify everyone that we have a new table.
                    this.fireTableRowsInserted(0,cache.getEntryCount()-1); 
                }
            } catch (Exception e) {
                cache.removeAll(); // blank it out and keep going.
            }
            return;
        }
    
        public void clearModel() 
        {
            if ( cache != null && cache.getEntryCount() > 0 ) {
                int numRows = cache.getEntryCount()-1;
                cache.removeAll();
                cache = new MetadataEntry();
                this.fireTableRowsDeleted(0,numRows);
            } else
                cache = new MetadataEntry();
        }

        public void clearRows() 
        {
            if ( cache != null && cache.getEntryCount() > 0 ) {
                int numRows = cache.getEntryCount()-1;
                cache.removeAll();
                this.fireTableRowsDeleted(0,numRows);
            } 
        }

        /**
         * @function getDimArray
         * @return
         * @author Simon Greener April 17th 2010
         *          Created to simplify init function
         * @author Simon Greener, July 29th 2010
         *          Renamed function to fix update metadata bug
         */
        public String getDimArray() 
        {
            return cache.toDimArray();
        }
    }
    
    // *************************************************************
    
    class MetadataTableModel extends DefaultTableModel  
    {
		private static final long serialVersionUID = -2156353053122988215L;
		public final int mEntryPosn   = -1;
        public final int mSchemaPosn  = 0;
        public final int mObjectPosn  = 1;
        public final int mColumnPosn  = 2;
        public final int mSRIDPosn    = 3;
        public final int mDiminofPosn = 4;
        
        private int cacheCapacity = 255;
        Vector<MetadataEntry>   cache;
        protected String[]      columns = { METADATA_TABLE_COLUMN_0,
                                            METADATA_TABLE_COLUMN_1,
                                            METADATA_TABLE_COLUMN_2,
                                            METADATA_TABLE_COLUMN_3,
                                            METADATA_TABLE_COLUMN_4 
                                          };

        @SuppressWarnings("rawtypes")
		Class[] types = new Class [] { java.lang.String.class, 
                                       java.lang.String.class, 
                                       java.lang.String.class, 
                                       java.lang.String.class, 
                                       java.lang.String.class
                                     };
        
        boolean[] canEdit = new boolean [] { false, 
                                             false, 
                                             false, 
                                             false, 
                                             false
                                           };
        
        public MetadataTableModel() {
            this.cache = new Vector<MetadataEntry>(cacheCapacity);
        }

        public Vector<MetadataEntry> getMetadataTableVector() {
            return this.cache;
        }

        public LinkedHashMap<String, MetadataEntry> getMetadataTableEntries() {
            LinkedHashMap<String, MetadataEntry> mEntries = new LinkedHashMap<String, MetadataEntry>(this.cache.size());
            ListIterator<MetadataEntry> cacheIter = this.cache.listIterator();
            while (cacheIter.hasNext() ) {
                MetadataEntry mEntry = cacheIter.next();
                mEntries.put(mEntry.getFullName(),mEntry);
            }
            return mEntries;
        }
        
        public MetadataEntry find(String _fullObjectName) {
            if (Strings.isEmpty(_fullObjectName) )
                return null;
            ListIterator<MetadataEntry> cacheIter = this.cache.listIterator();
            while (cacheIter.hasNext() ) {
                MetadataEntry mEntry = cacheIter.next();
                if ( mEntry.getFullName().equalsIgnoreCase(_fullObjectName) ) {
                    return mEntry;
                }
            }
            return null;
        }

        public int indexOf(String _fullObjectName) {
            int keyIndex = -1;
            if (this.cache == null || this.cache.size() == 0) 
                return keyIndex;
            ListIterator<MetadataEntry> cacheIter = this.cache.listIterator();
            while (cacheIter.hasNext() ) {
                keyIndex++;
                MetadataEntry mEntry = cacheIter.next();
                if ( mEntry.getFullName().equalsIgnoreCase(_fullObjectName) ) {
                    return keyIndex;
                }
            }
            return -1;
        }

        @Override
        public String getColumnName(int column) {
            return this.columns[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return this.types [columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return this.canEdit [columnIndex];
        }

        @Override
        public int getColumnCount() {
            return this.columns.length;
        }

        @Override
        public int getRowCount() {
            if (this.cache == null) {
                return 0;
            } else {
                return this.cache.size();
            }
        }

        @Override
        public Object getValueAt(int _row, int _col) 
        {
            if (this.cache == null || this.cache.size() == 0) 
                return "???";
            MetadataEntry mEntry = this.cache.get(_row);
            switch (_col) {
                case mEntryPosn  : return mEntry;
                case mSchemaPosn : return mEntry.getSchemaName();
                case mObjectPosn : return mEntry.getObjectName();
                case mColumnPosn : return mEntry.getColumnName();
                case mSRIDPosn   : return mEntry.getSRID();
                case mDiminofPosn: return metadataPreferences.isColourDimInfo() 
                                          ? RenderTool.htmlWrap(mEntry.render()) 
                                          : mEntry.toDimArray();
            }
            return "???";
        }

        @Override
        public void setValueAt(Object _mEntry, int _row, int _col) {
            if (this.cache == null || this.cache.size() == 0) 
                return;
            MetadataEntry mEntry = (MetadataEntry)_mEntry;
            this.cache.setElementAt(mEntry, _row);
            this.fireTableRowsUpdated(_row, _row);
        }

        public void deleteRow(String _fullObjectName) 
        {
            if (this.cache        == null || 
                this.cache.size() == 0) 
                return;
            int row = this.indexOf(_fullObjectName);
            if ( row != -1 ) {
                this.cache.removeElementAt(row);
            }
        }

        @Override
        public void removeRow(int _row) {
            if (this.cache        == null || 
                this.cache.size() == 0 ||
                _row              == this.mEntryPosn) 
                return;
            int row = _row == this.cache.size() ? this.cache.size()-1 : _row;
            this.cache.removeElementAt(row);
            this.fireTableRowsDeleted(this.cache.size()-1,this.cache.size()-1);
        }

        public void clearModel() 
        {
            if ( this.cache != null && this.cache.size() > 0 ) {
                int numRows = this.cache.size()-1;
                this.cache.clear();
                this.fireTableRowsDeleted(0,numRows);
            }
        }

        public String readMetadataIntoTable(OracleConnection _conn, 
                                            String           _schemaName,
                                            boolean          _allSchemas)
        {
            String returnString = sOK; 
            setAlwaysOnTop(false);
            // use inner class to do work in a thread that ProgressBar can use
            class MetadataReader implements Runnable 
            {
                protected ProgressBar progressBar;
                protected OracleConnection   conn;
                protected boolean      allSchemas;
                protected String       schemaName;
                protected String     errorMessage = sCancelled;
                
                public MetadataReader(OracleConnection _conn,
                                      String           _schemaName,
                                      boolean          _allSchemas) 
                {
                    this.conn       = _conn;
                    this.allSchemas = _allSchemas;
                    this.schemaName = _schemaName;
                }
                
                public void setProgressBar(ProgressBar _progressBar) {
                    this.progressBar = _progressBar;
                }
                
                public String getErrorMessage() {
                    return this.errorMessage;
                }
                
                public void run () 
                {
                    try 
                    {
                        LinkedHashMap<String, MetadataEntry> mEntries;
                        mEntries = MetadataTool.getMetadata(this.conn,
                                                            this.schemaName,
                                                            null,
                                                            null,
                                                            this.allSchemas);
                        if (this.progressBar.hasUserCancelled()) {
                            this.errorMessage = sCancelled;
                            this.progressBar.setDoneStatus();
                            return;
                        }
                        clearModel(); 
                        String key = "";
                        Iterator<String> iter = mEntries.keySet().iterator();
                        while (iter.hasNext() ) {
                            key = iter.next();
                            MetadataEntry mEntry = mEntries.get(key);
                            cache.add(new MetadataEntry(mEntry));
                        }
                    } catch (SQLException sqle) {
                        this.errorMessage = sqle.getMessage();
                    } catch (IllegalArgumentException iae) {
                        this.errorMessage = iae.getMessage();
                    }
                    // "shut down" progressbar
                    this.errorMessage = sOK;
                    this.progressBar.setDoneStatus();
                }
            }
            MetadataReader mr = new MetadataReader(_conn, 
                                                   _schemaName, 
                                                   _allSchemas);
            MetadataPanel mp = MetadataPanel.getInstance();
            ProgressBar progress = new ProgressBar(mp, "Retrieving " + (_allSchemas?"all":_schemaName+"'s") + " metadata objects.", mr, true);
            progress.setCancelable(true);
            mr.setProgressBar(progress);
            progress.start("Executing query to gather " + (_allSchemas?"all":_schemaName+"'s user")+ "_sdo_geom_metadata entries, please wait...", null);

            if ( ! mr.getErrorMessage().equals(sOK) ) {
                if ( mr.getErrorMessage().equals(sCancelled) ) {
                    returnString=sCancelled;
                } else if ( !Strings.isEmpty(mr.getErrorMessage()) ) { 
                    // There is a real error
                    JOptionPane.showMessageDialog(null, 
                                                  mr.getErrorMessage(),
                                                  UPDATE_METADATA_ERROR,
                                                  JOptionPane.ERROR_MESSAGE);
                    returnString=sError;
                }
            } 
            // Notify that cache has changed
            if ( this.cache.size()!=0 ) {
                this.fireTableRowsInserted(0,this.cache.size()-1); // notify everyone that we have a new table.
            }
            
            progress = null;
            setAlwaysOnTop(true);
            return returnString;
        }

        public void print() 
        {
            if ( this.cache == null )
                return;
            Messages.log("metadataTM.print() - this.cache.size()="+this.cache.size());
            ListIterator<MetadataEntry> cacheIter = this.cache.listIterator();
            while (cacheIter.hasNext() ) {
                MetadataEntry tRow = cacheIter.next();
                Messages.log("metadataTM.print() - MetadataEntry=" + tRow.toString());
            }
        }

    }

    // ***********************************************************

    class NumberRenderer extends DefaultTableCellRenderer 
    {
        
		private static final long serialVersionUID = 5655569854116178518L;
		private NumberFormat formatter;
        
        public NumberRenderer() {
            this(NumberFormat.getNumberInstance(),0,20,false);
        }

        public NumberRenderer(int _minFractions, 
                              int _maxFractions,
                              boolean _commas) {
            this(NumberFormat.getNumberInstance(),_minFractions,_maxFractions,_commas);
        }
        
        public NumberRenderer(NumberFormat formatter,
                              int _minFractions, 
                              int _maxFractions,
                              boolean _commas) {
            super();
            this.formatter = formatter;
            ((DecimalFormat)formatter).setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.getDefault()));
            formatter.setMinimumFractionDigits(_minFractions);
            formatter.setMaximumFractionDigits(_maxFractions);
            // stop commas
            formatter.setGroupingUsed(_commas);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if ((value != null) && (value instanceof Number)){
                if (value instanceof Double) {
                    Double val = (Double) value;
                    if (! val.isNaN()) {
                        this.setText(formatter.format(value));
                    }
                } else
                    this.setText(formatter.format(value));
                this.setHorizontalAlignment(JLabel.RIGHT);                
            } 
            return renderer;
        }
    }
    
    class HighlightRenderer extends DefaultTableCellRenderer {
        
		private static final long serialVersionUID = 360876461753129600L;

		public Component getTableCellRendererComponent(JTable table, 
                                                       Object value,
                                                       boolean isSelected, 
                                                       boolean hasFocus, 
                                                       int row, 
                                                       int column) 
        {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            MetadataEntry me = (MetadataEntry)metadataTM.getValueAt(table.convertRowIndexToModel(row), metadataTM.mEntryPosn);
            if ( me.isOrphan() ) {
                if (isSelected) {
                    comp.setBackground(new Color((metadataPreferences.getHighlightColour().getRGB() ^ comp.getBackground().getRGB())));
                } else {
                    comp.setBackground(new Color(metadataPreferences.getOrphanColour().getRGB()));
                }
                comp.setForeground(Color.BLACK);
            } else if ( me.isMissing() ) {
                if (isSelected) {
                    comp.setBackground(new Color((metadataPreferences.getHighlightColour().getRGB() ^ comp.getBackground().getRGB())));
                } else {
                    comp.setBackground(new Color(metadataPreferences.getMissingColour().getRGB()));
                }
                comp.setForeground(Color.BLACK);
            } else {
                if (isSelected) {
                    comp.setBackground(new Color((metadataPreferences.getHighlightColour().getRGB() ^ comp.getBackground().getRGB())));
                } else {
                    comp.setBackground(new Color(metadataPreferences.getCorrectColour().getRGB()));
                }
                comp.setForeground(Color.BLACK);
            }
            return comp;
        }
    }

}
