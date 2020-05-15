/**
 * ShapefileLoad.java
 *
 * Created on 08/05/2010, 6:26:43 PM
**/

package org.GeoRaptor.io.Import;

import java.awt.Toolkit;

import java.io.File;
import java.io.IOException;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Types;

import java.text.DateFormat;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import oracle.ide.dialogs.ProgressBar;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;

import oracle.spatial.geometry.JGeometry;
import oracle.spatial.util.DBFReaderJGeom;
import oracle.spatial.util.ShapefileFeatureJGeom;
import oracle.spatial.util.ShapefileReaderJGeom;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Messages;
import org.GeoRaptor.OracleSpatial.CreateSpatialIndex.ManageSpatialIndex;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.OracleSpatial.SRID.SRIDPanel;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.sql.DatabaseConnection;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.HtmlHelp;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;

import org.xBaseJ.micro.DBF;
import org.xBaseJ.micro.fields.Field;

/**
 * @author Simon Greener, May 2010
 * @tobedone 1. True, array loading to increase performance. 2. Any action in
 *           attribute table (eg tick box in column lisEist - Load?), keep FID
 *           selection if exists.
 */
public class ShapefileLoad extends javax.swing.JDialog {

	private static final long serialVersionUID = -1491318497084715236L;

	/**
	 * We have only one instance of this class
	 */
	private static ShapefileLoad classInstance;

	protected Preferences prefs;
	protected PropertiesManager propertyManager;
	private static final String propertiesFile = "org.GeoRaptor.io.Import.import";
	private final String iconDirectory = "org/GeoRaptor/images/";
	private ClassLoader cl = this.getClass().getClassLoader();

	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Import.import");

	/**
	 * Connection properties
	 */
	private String connName = "";
	MetadataEntry mEntry = null;

	private String TITLE;
	private String totalShapesToLoad = "";
	private String sNone = "<<NONE>>"; // NOI18N?
	private String userFID = null; // Holds any fid the user may have entered until all shapefiles are removed.
	private int totalRecords;

	private static enum LOAD_TYPE {
		NEW, INSERT, UPDATE
	};

	@SuppressWarnings("unused")
	private LOAD_TYPE load_operation = LOAD_TYPE.NEW;

	private DefaultListModel<String> ShapefileListModel;
	
	private SHPTableModel shptm;

	/**
	 * @constructor ShapefileLoad
	 * @author Simon Greener, June 10th 2010 - John O'Toole comment: "It's a modal
	 *         window & probably shouldn't be. "
	 */
	public ShapefileLoad() {
		this(null, false);
	}

	/** Creates new form ShapefileLoad */
	public ShapefileLoad(java.awt.Frame parent, boolean modal) {
		super(parent, modal);

		initComponents();

		this.prefs = MainSettings.getInstance().getPreferences();

		this.propertyManager = new PropertiesManager(ShapefileLoad.propertiesFile);

		FileNameExtensionFilter shpFNEX = new FileNameExtensionFilter("ESRI Shapefile", "shp");
		this.fcShapefile.addChoosableFileFilter(shpFNEX);
		this.fcShapefile.setFileFilter(shpFNEX);
		this.fcShapefile.setFileSelectionMode(JFileChooser.FILES_ONLY);
		this.fcShapefile.setCurrentDirectory(new java.io.File("/"));

		// Set widget names from properties file
		//
		this.TITLE = this.propertyManager.getMsg("SHPFILE_IMPORTER_TITLE");
		// Give this window a title.
		this.setTitle(this.TITLE);
		this.fcShapefile.setDialogTitle(this.propertyManager.getMsg("SHPFILE_IMPORTER_DIALOG_TITLE"));
		this.fcShapefile.setToolTipText(this.propertyManager.getMsg("SHPFILE_IMPORTER_FC_TOOLTOP"));
		this.lblConnections.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_CONNECTIONS"));
		this.btnCancel.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_CANCEL"));
		this.btnLoad.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_LOAD"));
		this.btnAddShp.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_ADD_SHP"));
		this.btnRemoveShp.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_REMOVE_SHP"));
		this.btnSRID.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_SRID_BTN"));
		this.cbSQL.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_EDIT_SQL"));
		this.pnlTableProperties.setBorder(javax.swing.BorderFactory
				.createTitledBorder(this.propertyManager.getMsg("SHPFILE_IMPORTER_TABLE_BORDER")));
		this.lblTableName.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_TABLE_NAME"));
		this.lblGeomColumn.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_GEOM_NAME"));
		this.lblCommitInterval.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_COMMIT_INTERVAL"));
		this.lblDecimalPlaces.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_DECIMAL_PLACES"));
		this.cmbDecimalPlaces.setToolTipText(this.propertyManager.getMsg("SHPFILE_IMPORTER_TOOLTIP_DECIMAL_PLACES"));
		this.lblFID.setText(this.propertyManager.getMsg("SHPFILE_IMPORTER_FID_COLUMN_NAME"));
		this.cmbFID.setToolTipText(this.propertyManager.getMsg("SHPFILE_IMPORTER_FID_TOOLTIP"));
		this.totalShapesToLoad = this.propertyManager.getMsg("SHPFILE_TOTAL_SHAPES_TO_LOAD");
		this.lblTotalShapesToLoad.setText(String.format(this.totalShapesToLoad, 0));
	}

	/**
	 * The preferred public method of instantiating the class. Guarantees only one
	 * instance.
	 * 
	 * @return
	 */
	public static ShapefileLoad getInstance() {
		if (ShapefileLoad.classInstance == null) {
			ShapefileLoad.classInstance = new ShapefileLoad();
		}
		return ShapefileLoad.classInstance;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		fcShapefile = new javax.swing.JFileChooser();
		btnCancel = new javax.swing.JButton();
		btnLoad = new javax.swing.JButton();
		pnlShapefileSet = new javax.swing.JPanel();
		scrlShapefiles = new javax.swing.JScrollPane();
		lstShapefiles = new javax.swing.JList<String>();
		btnAddShp = new javax.swing.JButton();
		btnRemoveShp = new javax.swing.JButton();
		lblTotalRecords = new javax.swing.JLabel();
		pnlSQL = new javax.swing.JPanel();
		cbSQL = new javax.swing.JCheckBox();
		jScrollPane1 = new javax.swing.JScrollPane();
		taSQL = new javax.swing.JTextArea();
		pnlTableProperties = new javax.swing.JPanel();
		cbNoLogging = new javax.swing.JCheckBox();
		scrlColumns = new javax.swing.JScrollPane();
		tblAttributeColumns = new javax.swing.JTable();
		tfTablename = new javax.swing.JTextField();
		lblTableName = new javax.swing.JLabel();
		pnlProcessingOptions = new javax.swing.JPanel();
		lblDecimalPlaces = new javax.swing.JLabel();
		sldrCommitInterval = new javax.swing.JSlider();
		lblCommitInterval = new javax.swing.JLabel();
		cmbDecimalPlaces = new javax.swing.JComboBox<String>();
		cbSpatialIndex = new javax.swing.JCheckBox();
		cbMetadata = new javax.swing.JCheckBox();
		lblTotalShapesToLoad = new javax.swing.JLabel();
		pnlMisc = new javax.swing.JPanel();
		lblFID = new javax.swing.JLabel();
		lblGeomColumn = new javax.swing.JLabel();
		tfSRID = new javax.swing.JTextField();
		tfGeometryColumn = new javax.swing.JTextField();
		btnSRID = new javax.swing.JButton();
		lblConnections = new javax.swing.JLabel();
		cmbConnections = new javax.swing.JComboBox<String>();
		lblSRID = new javax.swing.JLabel();
		cmbFID = new javax.swing.JComboBox<String>();
		btnHelp = new javax.swing.JButton();

		fcShapefile.setCurrentDirectory(new java.io.File("C:\\"));
		fcShapefile.setDialogTitle("Shapefile Selector");
		fcShapefile.setFileFilter(null);
		fcShapefile.setToolTipText("Navigate to shapefile and select");
		fcShapefile.setName("fcShapefile"); // NOI18N
		fcShapefile.getAccessibleContext().setAccessibleParent(this);

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);

		btnCancel.setText("Cancel");
		btnCancel.setName("btnCancel"); // NOI18N
		btnCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnCancelActionPerformed(evt);
			}
		});

		btnLoad.setText("Load");
		btnLoad.setName("btnLoad"); // NOI18N
		btnLoad.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnLoadActionPerformed(evt);
			}
		});

		pnlShapefileSet.setBorder(javax.swing.BorderFactory.createTitledBorder("Shapefile Set"));
		pnlShapefileSet.setMaximumSize(new java.awt.Dimension(413, 224));
		pnlShapefileSet.setMinimumSize(new java.awt.Dimension(413, 224));
		pnlShapefileSet.setName("pnlShapefileSet"); // NOI18N

		scrlShapefiles.setName("scrlShapefiles"); // NOI18N

		lstShapefiles.setName("lstShapefiles"); // NOI18N
		scrlShapefiles.setViewportView(lstShapefiles);

		btnAddShp.setText("Add");
		btnAddShp.setName("btnAddShp"); // NOI18N
		btnAddShp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnAddShpActionPerformed(evt);
			}
		});

		btnRemoveShp.setText("Remove");
		btnRemoveShp.setName("btnRemoveShp"); // NOI18N
		btnRemoveShp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnRemoveShpActionPerformed(evt);
			}
		});

		lblTotalRecords.setText("# 10000");
		lblTotalRecords.setName("lblTotalRecords"); // NOI18N

		javax.swing.GroupLayout pnlShapefileSetLayout = new javax.swing.GroupLayout(pnlShapefileSet);
		pnlShapefileSet.setLayout(pnlShapefileSetLayout);
		pnlShapefileSetLayout.setHorizontalGroup(pnlShapefileSetLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlShapefileSetLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(pnlShapefileSetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(lblTotalRecords)
								.addGroup(pnlShapefileSetLayout
										.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
										.addGroup(pnlShapefileSetLayout.createSequentialGroup().addComponent(btnAddShp)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addComponent(btnRemoveShp))
										.addComponent(scrlShapefiles, javax.swing.GroupLayout.PREFERRED_SIZE, 317,
												javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addContainerGap()));
		pnlShapefileSetLayout.setVerticalGroup(pnlShapefileSetLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlShapefileSetLayout.createSequentialGroup()
						.addGroup(pnlShapefileSetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(btnAddShp).addComponent(btnRemoveShp))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(scrlShapefiles, javax.swing.GroupLayout.PREFERRED_SIZE, 85,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lblTotalRecords)));

		pnlSQL.setBorder(javax.swing.BorderFactory.createTitledBorder("SQL"));
		pnlSQL.setMaximumSize(new java.awt.Dimension(429, 265));
		pnlSQL.setMinimumSize(new java.awt.Dimension(429, 265));
		pnlSQL.setName("pnlSQL"); // NOI18N

		cbSQL.setText("Edit");
		cbSQL.setName("cbSQL"); // NOI18N
		cbSQL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				cbSQLStateChanged(evt);
			}
		});
		cbSQL.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cbSQLActionPerformed(evt);
			}
		});

		jScrollPane1.setName("jScrollPane1"); // NOI18N

		taSQL.setColumns(20);
		taSQL.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
		taSQL.setRows(5);
		taSQL.setMaximumSize(new java.awt.Dimension(144, 74));
		taSQL.setMinimumSize(new java.awt.Dimension(144, 74));
		taSQL.setName("taSQL"); // NOI18N
		jScrollPane1.setViewportView(taSQL);

		javax.swing.GroupLayout pnlSQLLayout = new javax.swing.GroupLayout(pnlSQL);
		pnlSQL.setLayout(pnlSQLLayout);
		pnlSQLLayout.setHorizontalGroup(pnlSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlSQLLayout.createSequentialGroup().addContainerGap()
						.addGroup(pnlSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(cbSQL).addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE,
										363, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap()));
		pnlSQLLayout.setVerticalGroup(pnlSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlSQLLayout.createSequentialGroup()
						.addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 121,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18).addComponent(cbSQL)
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		pnlTableProperties.setBorder(javax.swing.BorderFactory.createTitledBorder("Table and Column Properties"));
		pnlTableProperties.setMaximumSize(new java.awt.Dimension(848, 264));
		pnlTableProperties.setMinimumSize(new java.awt.Dimension(848, 264));
		pnlTableProperties.setName("pnlTableProperties"); // NOI18N

		cbNoLogging.setText("No Logging");
		cbNoLogging.setName("cbNoLogging"); // NOI18N
		cbNoLogging.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				cbNoLoggingStateChanged(evt);
			}
		});

		scrlColumns.setName("scrlColumns"); // NOI18N

		tblAttributeColumns.setModel(new javax.swing.table.DefaultTableModel(
				new Object[][] { { null, null, null, null }, 
					             { null, null, null, null }, 
					             { null, null, null, null },
                                 { null, null, null, null } },
				new String[]     { "DBF Column", 
                                   "Oracle Column", 
                                   "Data Type", 
                                   "Not Null" }) 
		{
            private static final long serialVersionUID = 1L;
			@SuppressWarnings("rawtypes")
			Class[] types = new Class[] { java.lang.String.class, 
                                          java.lang.String.class, 
                                          java.lang.String.class,
                                          java.lang.Object.class };
			boolean[] canEdit = new boolean[] { false, true, true, true };

			public Class<?> getColumnClass(int columnIndex) {
				return types[columnIndex];
			}

			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return canEdit[columnIndex];
			}
		});
		tblAttributeColumns.setMaximumSize(new java.awt.Dimension(300, 64));
		tblAttributeColumns.setMinimumSize(new java.awt.Dimension(300, 64));
		tblAttributeColumns.setName("tblAttributeColumns"); // NOI18N
		scrlColumns.setViewportView(tblAttributeColumns);

		tfTablename.setMaximumSize(new java.awt.Dimension(253, 20));
		tfTablename.setMinimumSize(new java.awt.Dimension(253, 20));
		tfTablename.setName("tfTablename"); // NOI18N
		tfTablename.setPreferredSize(new java.awt.Dimension(253, 20));
		tfTablename.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent evt) {
				tfTablenameFocusLost(evt);
			}
		});
		tfTablename.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent evt) {
				tfTablenameKeyReleased(evt);
			}
		});

		lblTableName.setText("Table Name:");
		lblTableName.setName("lblTableName"); // NOI18N

		javax.swing.GroupLayout pnlTablePropertiesLayout = new javax.swing.GroupLayout(pnlTableProperties);
		pnlTableProperties.setLayout(pnlTablePropertiesLayout);
		pnlTablePropertiesLayout.setHorizontalGroup(pnlTablePropertiesLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlTablePropertiesLayout.createSequentialGroup().addContainerGap()
						.addGroup(pnlTablePropertiesLayout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(scrlColumns)
								.addGroup(pnlTablePropertiesLayout.createSequentialGroup().addComponent(lblTableName)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(tfTablename, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(cbNoLogging)))
						.addContainerGap()));
		pnlTablePropertiesLayout.setVerticalGroup(pnlTablePropertiesLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlTablePropertiesLayout.createSequentialGroup().addGroup(pnlTablePropertiesLayout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblTableName)
						.addComponent(tfTablename, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbNoLogging)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(scrlColumns, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap()));

		pnlProcessingOptions.setBorder(javax.swing.BorderFactory.createTitledBorder("Processing Options"));
		pnlProcessingOptions.setMaximumSize(new java.awt.Dimension(442, 151));
		pnlProcessingOptions.setMinimumSize(new java.awt.Dimension(442, 151));
		pnlProcessingOptions.setName("pnlProcessingOptions"); // NOI18N

		lblDecimalPlaces.setText("Ordinate Rounding:");
		lblDecimalPlaces.setName("lblDecimalPlaces"); // NOI18N

		sldrCommitInterval.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
		sldrCommitInterval.setMajorTickSpacing(500);
		sldrCommitInterval.setMaximum(5000);
		sldrCommitInterval.setMinorTickSpacing(100);
		sldrCommitInterval.setPaintLabels(true);
		sldrCommitInterval.setPaintTicks(true);
		sldrCommitInterval.setSnapToTicks(true);
		sldrCommitInterval.setValue(100);
		sldrCommitInterval.setMaximumSize(new java.awt.Dimension(291, 44));
		sldrCommitInterval.setMinimumSize(new java.awt.Dimension(291, 44));
		sldrCommitInterval.setName("sldrCommitInterval"); // NOI18N
		sldrCommitInterval.setPreferredSize(new java.awt.Dimension(291, 44));
		sldrCommitInterval.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				sldrCommitIntervalStateChanged(evt);
			}
		});

		lblCommitInterval.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		lblCommitInterval.setText("Commit (1000):");
		lblCommitInterval.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		lblCommitInterval.setMaximumSize(new java.awt.Dimension(104, 14));
		lblCommitInterval.setMinimumSize(new java.awt.Dimension(104, 14));
		lblCommitInterval.setName("lblCommitInterval"); // NOI18N
		lblCommitInterval.setPreferredSize(new java.awt.Dimension(104, 14));

		cmbDecimalPlaces.setModel(new javax.swing.DefaultComboBoxModel<String>(
				new String[] { "<<NONE>>", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
		cmbDecimalPlaces.setMaximumSize(new java.awt.Dimension(86, 20));
		cmbDecimalPlaces.setMinimumSize(new java.awt.Dimension(86, 20));
		cmbDecimalPlaces.setName("cmbDecimalPlaces"); // NOI18N
		cmbDecimalPlaces.setPreferredSize(new java.awt.Dimension(86, 20));

		cbSpatialIndex.setText("Create Spatial Index?");
		cbSpatialIndex.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		cbSpatialIndex.setName("cbSpatialIndex"); // NOI18N
		cbSpatialIndex.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cbSpatialIndexActionPerformed(evt);
			}
		});

		cbMetadata.setText("Create Metadata?");
		cbMetadata.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		cbMetadata.setName("cbMetadata"); // NOI18N
		cbMetadata.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cbMetadataActionPerformed(evt);
			}
		});

		lblTotalShapesToLoad.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		lblTotalShapesToLoad.setText("Total Shapes To Load = 100000");
		lblTotalShapesToLoad.setName("lblTotalShapesToLoad"); // NOI18N

		javax.swing.GroupLayout pnlProcessingOptionsLayout = new javax.swing.GroupLayout(pnlProcessingOptions);
		pnlProcessingOptions.setLayout(pnlProcessingOptionsLayout);
		pnlProcessingOptionsLayout.setHorizontalGroup(pnlProcessingOptionsLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlProcessingOptionsLayout.createSequentialGroup().addContainerGap()
						.addGroup(pnlProcessingOptionsLayout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(pnlProcessingOptionsLayout.createSequentialGroup()
										.addComponent(lblCommitInterval, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(sldrCommitInterval, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))
								.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlProcessingOptionsLayout
										.createSequentialGroup()
										.addGroup(pnlProcessingOptionsLayout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
												.addComponent(cbSpatialIndex).addComponent(cbMetadata))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(pnlProcessingOptionsLayout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(lblTotalShapesToLoad,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addGroup(pnlProcessingOptionsLayout.createSequentialGroup()
														.addGap(0, 81, Short.MAX_VALUE).addComponent(lblDecimalPlaces)
														.addPreferredGap(
																javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
														.addComponent(cmbDecimalPlaces,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)))
										.addGap(9, 9, 9)))
						.addContainerGap(15, Short.MAX_VALUE)));
		pnlProcessingOptionsLayout.setVerticalGroup(pnlProcessingOptionsLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlProcessingOptionsLayout.createSequentialGroup().addGroup(pnlProcessingOptionsLayout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(pnlProcessingOptionsLayout.createSequentialGroup().addGap(0, 0, Short.MAX_VALUE)
								.addComponent(cbMetadata)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(cbSpatialIndex))
						.addGroup(pnlProcessingOptionsLayout.createSequentialGroup().addGroup(pnlProcessingOptionsLayout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(sldrCommitInterval, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGroup(pnlProcessingOptionsLayout.createSequentialGroup().addContainerGap()
										.addComponent(lblCommitInterval, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)))
								.addGap(6, 6, 6).addComponent(lblTotalShapesToLoad)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(pnlProcessingOptionsLayout
										.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(lblDecimalPlaces)
										.addComponent(cmbDecimalPlaces, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))))
						.addContainerGap()));

		pnlMisc.setBorder(javax.swing.BorderFactory.createTitledBorder("Target"));
		pnlMisc.setMaximumSize(new java.awt.Dimension(400, 151));
		pnlMisc.setMinimumSize(new java.awt.Dimension(400, 151));
		pnlMisc.setName("pnlMisc"); // NOI18N

		lblFID.setText("Unique Id Column:");
		lblFID.setName("lblFID"); // NOI18N

		lblGeomColumn.setText("Geometry Column:");
		lblGeomColumn.setName("lblGeomColumn"); // NOI18N

		tfSRID.setMaximumSize(new java.awt.Dimension(140, 20));
		tfSRID.setMinimumSize(new java.awt.Dimension(140, 20));
		tfSRID.setName("tfSRID"); // NOI18N
		tfSRID.setPreferredSize(new java.awt.Dimension(140, 20));

		tfGeometryColumn.setText("GEOM");
		tfGeometryColumn.setMaximumSize(new java.awt.Dimension(140, 20));
		tfGeometryColumn.setMinimumSize(new java.awt.Dimension(140, 20));
		tfGeometryColumn.setName("tfGeometryColumn"); // NOI18N
		tfGeometryColumn.setPreferredSize(new java.awt.Dimension(140, 20));
		tfGeometryColumn.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent evt) {
				tfGeometryColumnFocusLost(evt);
			}
		});

		btnSRID.setText("Select SRID");
		btnSRID.setName("btnSRID"); // NOI18N
		btnSRID.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnSRIDActionPerformed(evt);
			}
		});

		lblConnections.setText("Connection:");
		lblConnections.setName("lblConnections"); // NOI18N

		cmbConnections.setLightWeightPopupEnabled(false);
		cmbConnections.setMaximumSize(new java.awt.Dimension(235, 20));
		cmbConnections.setMinimumSize(new java.awt.Dimension(235, 20));
		cmbConnections.setName("cmbConnections"); // NOI18N
		cmbConnections.setPreferredSize(new java.awt.Dimension(235, 20));
		cmbConnections.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cmbConnectionsActionPerformed(evt);
			}
		});

		lblSRID.setText("SRID:");
		lblSRID.setMaximumSize(new java.awt.Dimension(28, 18));
		lblSRID.setMinimumSize(new java.awt.Dimension(28, 18));
		lblSRID.setName("lblSRID"); // NOI18N
		lblSRID.setPreferredSize(new java.awt.Dimension(28, 18));

		cmbFID.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "<<NONE>>" }));
		cmbFID.setToolTipText("Select an existing field or enter new one.");
		cmbFID.setMaximumSize(new java.awt.Dimension(143, 20));
		cmbFID.setMinimumSize(new java.awt.Dimension(143, 20));
		cmbFID.setName("cmbFID"); // NOI18N
		cmbFID.setPreferredSize(new java.awt.Dimension(143, 20));
		cmbFID.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				cmbFIDItemStateChanged(evt);
			}
		});
		cmbFID.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cmbFIDActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout pnlMiscLayout = new javax.swing.GroupLayout(pnlMisc);
		pnlMisc.setLayout(pnlMiscLayout);
		pnlMiscLayout.setHorizontalGroup(pnlMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlMiscLayout.createSequentialGroup().addContainerGap()
						.addGroup(pnlMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(lblConnections).addComponent(lblGeomColumn)
								.addComponent(lblSRID, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblFID))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
						.addGroup(pnlMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
								.addComponent(tfGeometryColumn, javax.swing.GroupLayout.Alignment.LEADING,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)
								.addComponent(cmbFID, javax.swing.GroupLayout.Alignment.LEADING, 0,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addGroup(javax.swing.GroupLayout.Alignment.LEADING,
										pnlMiscLayout.createSequentialGroup()
												.addComponent(tfSRID, javax.swing.GroupLayout.PREFERRED_SIZE, 68,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(btnSRID))
								.addComponent(cmbConnections, 0, 1, Short.MAX_VALUE))
						.addContainerGap()));
		pnlMiscLayout.setVerticalGroup(pnlMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlMiscLayout.createSequentialGroup().addGap(1, 1, 1).addGroup(pnlMiscLayout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(pnlMiscLayout.createSequentialGroup().addGap(50, 50, 50).addGroup(pnlMiscLayout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(lblSRID, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(tfSRID, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnSRID))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(pnlMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(tfGeometryColumn, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(lblGeomColumn)))
						.addGroup(pnlMiscLayout.createSequentialGroup()
								.addGroup(pnlMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(cmbConnections, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(lblConnections))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(pnlMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
										.addComponent(lblFID, javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(cmbFID, javax.swing.GroupLayout.Alignment.LEADING,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))))
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		btnHelp.setText("Help");
		btnHelp.setName("btnHelp"); // NOI18N
		btnHelp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnHelpActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
				.createSequentialGroup().addContainerGap()
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
								.addComponent(pnlSQL, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addGroup(layout.createSequentialGroup().addComponent(btnHelp)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(btnLoad).addGap(166, 166, 166).addComponent(btnCancel))
										.addComponent(pnlShapefileSet, javax.swing.GroupLayout.PREFERRED_SIZE, 0,
												Short.MAX_VALUE)))
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
								.addComponent(pnlTableProperties, javax.swing.GroupLayout.Alignment.LEADING,
										javax.swing.GroupLayout.PREFERRED_SIZE, 746, Short.MAX_VALUE)
								.addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
										.addComponent(pnlMisc, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(pnlProcessingOptions, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))))
				.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(pnlProcessingOptions, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(pnlMisc, javax.swing.GroupLayout.PREFERRED_SIZE, 129,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(
								pnlTableProperties, javax.swing.GroupLayout.PREFERRED_SIZE, 190,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
								.createSequentialGroup()
								.addComponent(pnlShapefileSet, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(btnHelp).addComponent(btnLoad).addComponent(btnCancel))
								.addGap(0, 0, Short.MAX_VALUE))
								.addComponent(pnlSQL, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
						.addContainerGap()));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void btnAddShpActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAddShpActionPerformed
		addShapefileToList();
	}// GEN-LAST:event_btnAddShpActionPerformed

	private void btnRemoveShpActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnRemoveShpActionPerformed
		removeShapefileFromList();
	}// GEN-LAST:event_btnRemoveShpActionPerformed

	private void cmbConnectionsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmbConnectionsActionPerformed
		// Only process even it comboBox is enabled
		if (this.cmbConnections.isEnabled()) {
			String displayName = this.cmbConnections.getSelectedItem().toString();
			DatabaseConnection selectedConn = DatabaseConnections.getInstance().findDisplayName(displayName);
			if (selectedConn != null) {
				this.connName = selectedConn.getConnectionName();
			}
		}
	}// GEN-LAST:event_cmbConnectionsActionPerformed

	private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCancelActionPerformed
		this.setVisible(false);
	}// GEN-LAST:event_btnCancelActionPerformed

	private void btnSRIDActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSRIDActionPerformed
		processSRIDAction();
	}// GEN-LAST:event_btnSRIDActionPerformed

	private void cbSQLStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_cbSQLStateChanged
		this.taSQL.setEditable(this.cbSQL.isSelected());
		this.taSQL.setEnabled(this.cbSQL.isSelected());
	}// GEN-LAST:event_cbSQLStateChanged

	private void cbNoLoggingStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_cbNoLoggingStateChanged
		if (!Strings.isEmpty(this.taSQL.getText())) {
			boolean containsNoLogging = this.taSQL.getText().contains(") NOLOGGING");
			if (containsNoLogging) {
				if (!this.cbNoLogging.isSelected())
					this.taSQL.setText(this.taSQL.getText().replace(") NOLOGGING", ")"));
			} else {
				if (this.cbNoLogging.isSelected())
					this.taSQL.setText(this.taSQL.getText() + " NOLOGGING");
			}
		}
	}// GEN-LAST:event_cbNoLoggingStateChanged

	private void btnLoadActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnLoadActionPerformed
		if (!(Strings.isEmpty(this.taSQL.getText()) && Strings.isEmpty(this.tfTablename.getText()))) {
			String action = "DROP";
			try {
				executeSQL("DROP TABLE " + Strings.append(this.getUserName(), this.tfTablename.getText(), "."));
				// Create the table
				//
				action = "CREATE";
				executeSQL(this.taSQL.getText());
				// Now load all shapefiles
				//
				loadAllShapefiles();
				// @todo: If flag set to opne MetadataManager then do it
				// @todo: And also spatial index
			} catch (SQLException sqle) {
				this.setAlwaysOnTop(false);
				JOptionPane.showMessageDialog(null, action + " TABLE failed:\n" + sqle.getLocalizedMessage(),
						this.TITLE, JOptionPane.INFORMATION_MESSAGE);
				this.setAlwaysOnTop(true);
			}
		}
	}// GEN-LAST:event_btnLoadActionPerformed

	private void sldrCommitIntervalStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_sldrCommitIntervalStateChanged
		this.lblCommitInterval.setText("Commit (" + String.valueOf(this.sldrCommitInterval.getValue()) + "):");
	}// GEN-LAST:event_sldrCommitIntervalStateChanged

	private void tfTablenameFocusLost(java.awt.event.FocusEvent evt) {// GEN-FIRST:event_tfTablenameFocusLost
		if (Strings.isEmpty(this.tfTablename.getText())) {
			Toolkit.getDefaultToolkit().beep();
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(null, "Table name may not be empty", TITLE, JOptionPane.INFORMATION_MESSAGE);
			this.setAlwaysOnTop(true);
			return;
		}
		refreshCreateTable();
	}// GEN-LAST:event_tfTablenameFocusLost

	private void cmbFIDItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_cmbFIDItemStateChanged
		// If another process is updating this Combo Box isEnabled will be false
		// I know of no other way of doing this to stop side-effects
		//
		if (this.cmbFID.isEnabled() && (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED)) {
			refreshCreateTable();
		}
	}// GEN-LAST:event_cmbFIDItemStateChanged

	private void tfGeometryColumnFocusLost(java.awt.event.FocusEvent evt) {// GEN-FIRST:event_tfGeometryColumnFocusLost
		if (Strings.isEmpty(this.tfGeometryColumn.getText())) {
			Toolkit.getDefaultToolkit().beep();
			this.setAlwaysOnTop(false);
			JOptionPane.showConfirmDialog(null, "Geometry column name may not be empty");
			this.setAlwaysOnTop(true);
			return;
		}
		refreshCreateTable();
	}// GEN-LAST:event_tfGeometryColumnFocusLost

	private void cmbFIDActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmbFIDActionPerformed
		if (evt.getActionCommand().equals("comboBoxEdited")) {
			this.userFID = cmbFID.getSelectedItem().toString();
		}
	}// GEN-LAST:event_cmbFIDActionPerformed

	private void cbMetadataActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbMetadataActionPerformed
		if (!this.cbMetadata.isSelected()) {
			this.cbSpatialIndex.setSelected(false);
		}
	}// GEN-LAST:event_cbMetadataActionPerformed

	private void cbSpatialIndexActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbSpatialIndexActionPerformed
		if (this.cbSpatialIndex.isSelected()) {
			this.cbMetadata.setSelected(true);
		}

	}// GEN-LAST:event_cbSpatialIndexActionPerformed

	private void tfTablenameKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_tfTablenameKeyReleased
		if (Strings.isEmpty(this.tfTablename.getText())) {
			return;
		}
		if (!this.tfTablename.getText().substring(0, 1).matches("^[A-Za-z]")
				|| !this.tfTablename.getText().matches("^[A-Za-z][A-Za-z0-9_]*$")) {
			Toolkit.getDefaultToolkit().beep();
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(null,
					"Table names must start with an alphabetic character. Can then have alphabetic, digit or underscore characters.",
					TITLE, JOptionPane.INFORMATION_MESSAGE);
			this.setAlwaysOnTop(true);
			this.tfTablename.setText(this.tfTablename.getText().substring(1));
		}
	}// GEN-LAST:event_tfTablenameKeyReleased

	private void btnHelpActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnHelpActionPerformed
		Object image_url_0 = cl.getResource(iconDirectory + "Shapefile_Load_0.gif").toExternalForm();
		Object image_url_1 = cl.getResource(iconDirectory + "Shapefile_Load_1.png").toExternalForm();
		Object image_url_2 = cl.getResource(iconDirectory + "Shapefile_Load_2.png").toExternalForm();
		Object image_url_3 = cl.getResource(iconDirectory + "Shapefile_Load_3.png").toExternalForm();
		Object image_url_4 = cl.getResource(iconDirectory + "Shapefile_Load_4.png").toExternalForm();
		Object image_url_5 = cl.getResource(iconDirectory + "Shapefile_Load_5.png").toExternalForm();
		Object image_url_6 = cl.getResource(iconDirectory + "Shapefile_Load_6.png").toExternalForm();
		HtmlHelp hh = new HtmlHelp(this.propertyManager.getMsg("HELP_TITLE"),
				this.propertyManager.getMsg("HELP_BORDER"),
				String.format(this.propertyManager.getMsg("HELP_CONTENT"), image_url_0, image_url_1, image_url_2,
						image_url_3, image_url_4, image_url_5, image_url_6, image_url_6));
		hh.display();
	}// GEN-LAST:event_btnHelpActionPerformed

	private void cbSQLActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbSQLActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_cbSQLActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnAddShp;
	private javax.swing.JButton btnCancel;
	private javax.swing.JButton btnHelp;
	private javax.swing.JButton btnLoad;
	private javax.swing.JButton btnRemoveShp;
	private javax.swing.JButton btnSRID;
	private javax.swing.JCheckBox cbMetadata;
	private javax.swing.JCheckBox cbNoLogging;
	private javax.swing.JCheckBox cbSQL;
	private javax.swing.JCheckBox cbSpatialIndex;
	private javax.swing.JComboBox<String> cmbConnections;
	private javax.swing.JComboBox<String> cmbDecimalPlaces;
	private javax.swing.JComboBox<String> cmbFID;
	private javax.swing.JFileChooser fcShapefile;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JLabel lblCommitInterval;
	private javax.swing.JLabel lblConnections;
	private javax.swing.JLabel lblDecimalPlaces;
	private javax.swing.JLabel lblFID;
	private javax.swing.JLabel lblGeomColumn;
	private javax.swing.JLabel lblSRID;
	private javax.swing.JLabel lblTableName;
	private javax.swing.JLabel lblTotalRecords;
	private javax.swing.JLabel lblTotalShapesToLoad;
	private javax.swing.JList<String> lstShapefiles;
	private javax.swing.JPanel pnlMisc;
	private javax.swing.JPanel pnlProcessingOptions;
	private javax.swing.JPanel pnlSQL;
	private javax.swing.JPanel pnlShapefileSet;
	private javax.swing.JPanel pnlTableProperties;
	private javax.swing.JScrollPane scrlColumns;
	private javax.swing.JScrollPane scrlShapefiles;
	private javax.swing.JSlider sldrCommitInterval;
	private javax.swing.JTextArea taSQL;
	private javax.swing.JTable tblAttributeColumns;
	private javax.swing.JTextField tfGeometryColumn;
	private javax.swing.JTextField tfSRID;
	private javax.swing.JTextField tfTablename;
	// End of variables declaration//GEN-END:variables

	public void initialise() {
		this.load_operation = LOAD_TYPE.NEW;
		// Load connection pick list....
		DatabaseConnections.getInstance().setConnections(true/* open only */);
		if (DatabaseConnections.getInstance().getConnectionCount() == 0) {
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(this, propertyManager.getMsg("SHPFILE_IMPORTER_NO_ACTIVE_CONN_ERROR"));
			this.setAlwaysOnTop(true);
			return;
		}
		this.cmbConnections.setEnabled(false);
		this.loadCmbConnections();
		this.cmbConnections.setEnabled(true);
		this.cmbConnections.setSelectedIndex(0); // This fires setting of userName() via cmbConnectionsActionPerformed
		DatabaseConnection dbConn = DatabaseConnections.getInstance()
				.findDisplayName(this.cmbConnections.getItemAt(0));
		if (dbConn == null || dbConn.isOpen() == false) {
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(this, propertyManager.getMsg("SHPFILE_IMPORTER_NO_OPEN_CONN_ERROR"));
			this.setAlwaysOnTop(true);
			return;
		}
		LOGGER.debug("this.cmbConnections.getSelectedIndex(0)=" + this.cmbConnections.getItemAt(0));

		// Initialise other widgets if second or subsequent call
		//
		this.cmbFID.setEditable(false); // until something in tblAttributeColumns
		this.cmbFID.setEnabled(false); // until something in tblAttributeColumns

		// Create table model filling it with SRID data
		//
		this.shptm = new SHPTableModel();
		this.tblAttributeColumns.setModel(shptm);
		this.shptm.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				cmbFID.setEnabled(false);
				refreshCreateTable();
				refreshFIDs();
				cmbFID.setEnabled(true);
			}
		});
		this.is_load = shptm.getColumnPosition(columns.IS_LOAD);
		this.ora_col = shptm.getColumnPosition(columns.ORACLE_COLUMN_NAME);
		this.ora_typ = shptm.getColumnPosition(columns.ORACLE_DATA_TYPE);
		this.dbf_typ = shptm.getColumnPosition(columns.DBF_DATA_TYPE);

		this.ShapefileListModel = new DefaultListModel<String>();
		this.lstShapefiles.setModel(ShapefileListModel);

		this.tfSRID.setText(prefs.getSRID());
		this.userFID = null;
		this.cmbFID.removeAllItems();
		this.cmbFID.addItem(this.sNone);

		this.lblTotalRecords.setText("");
		this.lblTotalShapesToLoad.setText(String.format(this.totalShapesToLoad, 0));
		this.totalRecords = 0;

		this.tfTablename.setText("");
		this.tfGeometryColumn.setText(prefs.getDefGeomColName()); // or a preference
		this.taSQL.setText("");
		this.taSQL.setEditable(false);
		this.taSQL.setEnabled(false);

		this.lblCommitInterval.setText("Commit (" + String.valueOf(this.sldrCommitInterval.getValue()) + "):");
		this.btnLoad.setEnabled(false);

		this.setLocationRelativeTo(super.getComponent(0));
		this.setVisible(true);
	}

	public org.GeoRaptor.SpatialView.SpatialViewPanel.LayerReturnCode initialise(String _schemaName, String _objectName,
			String _columnName, String _connName) {
		this.connName = _connName;
		DatabaseConnections.getInstance().addConnection(this.connName);
		this.load_operation = LOAD_TYPE.INSERT;
		OracleConnection conn = this.getConnection(this.connName, false);

		// Check existance of object to be loaded
		// by getting SDO_GEOM_METADATA for the supplied parameters.
		//
		try {
			LinkedHashMap<String, MetadataEntry> metaEntries = MetadataTool.getMetadata(conn, _schemaName, _objectName,
					_columnName, false);
			if (metaEntries.size() == 0) {
				throw new Exception("No metadata for " + Strings.objectString(_schemaName, _objectName, _columnName));
			}
			LOGGER.debug(metaEntries.toString());
			if (Strings.isEmpty(_columnName) && metaEntries.size() > 1) {
				LOGGER.debug("Building list of metadatEntries to determine choice.");
				// Ask which one want to map
				//
				Iterator<MetadataEntry> iter = metaEntries.values().iterator();
				String entryList[] = new String[metaEntries.size()];
				int i = 0;
				while (iter.hasNext()) {
					entryList[i++] = iter.next().getColumnName();
				}
				String value = (String) JOptionPane.showInputDialog(null,
						propertyManager.getMsg("ADD_LAYER_SELECT_COLUMN"), MainSettings.EXTENSION_NAME,
						JOptionPane.QUESTION_MESSAGE, null,
						// Use default icon
						entryList, entryList[0]);
				if (Strings.isEmpty(value)) {
					return org.GeoRaptor.SpatialView.SpatialViewPanel.LayerReturnCode.Fail;
				}
				// Find and assign mEntry based on choice
				for (MetadataEntry me : metaEntries.values()) {
					if (me.getColumnName().equalsIgnoreCase(value)) {
						mEntry = new MetadataEntry(me);
					}
				}
			} else {
				LOGGER.debug("Getting first metadatEntry from metaEntries with count of " + metaEntries.size());
				mEntry = metaEntries.values().toArray(new MetadataEntry[0])[0];
			}
			LOGGER.debug("Found metadatEntry of " + mEntry.toString());
		} catch (Exception e) {
			LOGGER.debug("Exception thrown is " + e.toString());
			int n = JOptionPane.showConfirmDialog(null,
					propertyManager.getMsg("SHPFILE_COLUMN_MISSING_MD",
							Strings.objectString(_schemaName, _objectName, _columnName)),
					MainSettings.EXTENSION_NAME, JOptionPane.YES_NO_OPTION);
			if (n == 0) {
				return org.GeoRaptor.SpatialView.SpatialViewPanel.LayerReturnCode.Metadata;
			} else {
				return org.GeoRaptor.SpatialView.SpatialViewPanel.LayerReturnCode.Fail;
			}
		}

		// OK, we know the table, we know the column for the shape
		// now let's build the SQL and the table entry
		//
		// Load connection pick list....
		this.cmbConnections.setEnabled(false);
		javax.swing.DefaultComboBoxModel<String> cm = new javax.swing.DefaultComboBoxModel<String>();
		String displayName = DatabaseConnections.getInstance().getConnectionDisplayName(this.connName);
		cm.addElement(displayName);
		this.cmbConnections.setModel(cm);
		this.cmbConnections.setEnabled(true); // This enables setting of this.conn in cmbConnectionsActionPerformed
		this.cmbConnections.setSelectedIndex(0); // This fires setting of this.conn and userName() via
													// cmbConnectionsActionPerformed
		if (conn == null) {
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(this, propertyManager.getMsg("SHPFILE_IMPORTER_NO_OPEN_CONN_ERROR"));
			this.setAlwaysOnTop(true);
		}
		LOGGER.debug("this.cmbConnections.getSelectedIndex(0)=" + this.cmbConnections.getItemAt(0));

		// Create table model filling it with SRID data
		//
		this.shptm = new SHPTableModel();
		this.tblAttributeColumns.setModel(shptm);
		this.shptm.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				cmbFID.setEnabled(false);
				refreshCreateTable();
				refreshFIDs();
				cmbFID.setEnabled(true);
			}
		});
		this.is_load = shptm.getColumnPosition(columns.IS_LOAD);
		this.ora_col = shptm.getColumnPosition(columns.ORACLE_COLUMN_NAME);
		this.ora_typ = shptm.getColumnPosition(columns.ORACLE_DATA_TYPE);
		this.dbf_typ = shptm.getColumnPosition(columns.DBF_DATA_TYPE);

		try {
			this.shptm.loadOraAttributes(MetadataTool.getColumnsAndTypes(conn, this.mEntry.getSchemaName(),
					this.mEntry.getObjectName(), true, true));
		} catch (SQLException e) {
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(this, propertyManager.getMsg("SHPFILE_IMPORTER_LOAD_ORA_COLS_ERROR",
					Strings.append(this.mEntry.getSchemaName(), this.mEntry.getObjectName(), ".")));
			this.setAlwaysOnTop(true);
			return org.GeoRaptor.SpatialView.SpatialViewPanel.LayerReturnCode.Fail;
		}
		this.ShapefileListModel = new DefaultListModel<String>();
		this.lstShapefiles.setModel(ShapefileListModel);

		// Initialise other widgets if second or subsequent call
		//
		this.cmbFID.setEditable(false); // until something in tblAttributeColumns
		this.cmbFID.setEnabled(false); // until something in tblAttributeColumns

		this.tfSRID.setText(prefs.getSRID());
		this.userFID = null;
		this.cmbFID.removeAllItems();
		this.cmbFID.addItem(this.sNone);

		this.lblTotalRecords.setText("");
		this.lblTotalShapesToLoad.setText(String.format(this.totalShapesToLoad, 0));
		this.totalRecords = 0;

		this.tfTablename.setText(mEntry.getObjectName());
		this.tfGeometryColumn.setText(mEntry.getObjectName());
		this.taSQL.setText("");
		this.taSQL.setEditable(false);
		this.taSQL.setEnabled(false);

		this.lblCommitInterval.setText("Commit (" + String.valueOf(this.sldrCommitInterval.getValue()) + "):");
		this.btnLoad.setEnabled(false);

		this.setLocationRelativeTo(super.getComponent(0));
		this.setVisible(true);
		return org.GeoRaptor.SpatialView.SpatialViewPanel.LayerReturnCode.Success;
	}

	private OracleConnection getConnection(String _connName, boolean _display) {
		DatabaseConnection dbConn = _display ? DatabaseConnections.getInstance().findDisplayName(_connName)
				: DatabaseConnections.getInstance().findConnectionName(_connName);
		if (dbConn == null) {
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(null, "Could not get Connection for " + _connName,
					Constants.GEORAPTOR + " - " + this.getClass().getName(), JOptionPane.ERROR_MESSAGE);
			this.setAlwaysOnTop(true);
		}
		return dbConn.getConnection();
	}

	public void setConnection(String _connName) {
		LOGGER.debug("ShapefileLoad setConnection(" + _connName + ")");
		if (!Strings.isEmpty(_connName)) {
			this.connName = _connName;
			DatabaseConnections.getInstance().addConnection(this.connName);
		}
	}

	private void loadCmbConnections() {
		this.cmbConnections.removeAllItems();
		JComboBox<String> jb = DatabaseConnections.getInstance().getConnectionsCombo();
		if (jb == null || jb.getItemCount() == 0) {
			return;
		}
		this.cmbConnections.setModel(jb.getModel());
		if (this.isVisible()) {
			this.repaint();
		}
		return;
	}

	private String getUserName() {
		if (Strings.isEmpty(this.connName)) {
			LOGGER.warn("Can't access userName from null connection");
		}
		String userName = "";
		DatabaseConnection dbConn = DatabaseConnections.getInstance().findConnectionName(this.connName);
		if (dbConn == null) {
			return userName;
		}
		userName = dbConn.getUserName();
		return userName;
	}

	private void setTablename(String _tablename) {
		String newString = "";
		if (!Strings.isEmpty(_tablename)) {
			if (_tablename.matches("^[A-Za-z][A-Za-z0-9_]*$")) {
				newString = _tablename;
			} else {
				// Strip out only valid chars in right position.
				String aChar = "";
				for (int i = 0; i < _tablename.length(); i++) {
					aChar = String.valueOf(_tablename.charAt(i));
					if (newString.length() == 0 && !aChar.matches("^[A-Za-z]$")) {
						continue;
					}
					if (aChar.matches("^[A-Za-z0-9_]$")) {
						newString += aChar;
					}
				}
			}
		}
		this.tfTablename.setText(newString);
	}

	private void processSRIDAction() {
		if (this.cmbConnections == null || this.cmbConnections.getItemCount() == 0) {
			// TOBEDONE: Need error message
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		this.setAlwaysOnTop(false);
		SRIDPanel sp = SRIDPanel.getInstance();
		boolean status = sp.initialise(
				getConnection(this.cmbConnections.getSelectedItem().toString(), true) /* display */,
				this.tfSRID.getText());
		if (status == true) {
			sp.setLocationRelativeTo(this);
			sp.setVisible(true);
			if (!sp.formCancelled())
				this.tfSRID.setText(sp.getSRID());
		}
		this.setAlwaysOnTop(true);
	}

	public int shapefileCount() {
		return this.ShapefileListModel.getSize();
	}

	public ListModel<String> getShapefileModel() {
		return this.lstShapefiles.getModel();
	}

	public int getSRIDAsInteger() {
		return (Strings.isEmpty(this.tfSRID.getText()) || this.tfSRID.getText().equals("NULL")) ? 0
				: Integer.valueOf(this.tfSRID.getText()).intValue();
	}

	public String getSRID() {
		if (Strings.isEmpty(this.tfSRID.getText()) || this.tfSRID.getText().equalsIgnoreCase("NULL"))
			return "NULL";
		try {
			Integer.parseInt(this.tfSRID.getText());
			return this.tfSRID.getText();
		} catch (Exception e) {
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(null,
					this.propertyManager.getMsg("SHPFILE_IMPORTER_SRID_ERROR", this.tfSRID.getText()), this.TITLE,
					JOptionPane.ERROR_MESSAGE);
			this.setAlwaysOnTop(true);
		}
		return "NULL";
	}

	private void addShapefileToList() {
		String errorMessage = null;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		int returnVal = fcShapefile.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String fileName = fcShapefile.getSelectedFile().toString().replaceAll("\\.[Ss][Hh][Pp]$", "");
			int pos = this.ShapefileListModel.getSize();
			// Does select already exist in list?
			if (pos > 0) {
				for (int i = 0; i < pos; i++)
					if (!Strings.isEmpty(this.ShapefileListModel.getElementAt(i).toString())
							&& this.ShapefileListModel.getElementAt(i).equals(fileName)) {
						Toolkit.getDefaultToolkit().beep();
						return;
					}
			} else {
				String onlyTableName = fcShapefile.getSelectedFile().getName().replaceAll("\\.[Ss][Hh][Pp]$", "");
				this.setTablename(onlyTableName);
			}
			// Compute # records
			try {
				ShapefileReaderJGeom sfh = new ShapefileReaderJGeom(fileName);
				this.totalRecords += sfh.numRecords();
				sfh.closeShapefile();
				this.lblTotalRecords.setText("# " + this.totalRecords);
				this.lblTotalShapesToLoad.setText(String.format(this.totalShapesToLoad, this.totalRecords));
				this.ShapefileListModel.addElement(fileName);
				this.btnLoad.setEnabled(true);
				this.btnRemoveShp.setEnabled(true);
				this.cmbFID.setEnabled(false);
				// Add column names etc to set of loadable columns
				//
				shptm.loadShpAttributes(fileName);
				if (shptm.getColumnCount() == 0) {
					throw new Exception("Error opening shapefile: no column definitions read");
				}
				refreshCreateTable();
				refreshFIDs();
				this.cmbFID.setEnabled(true);
			} catch (IOException ioe) {
				// Error message and remove
				errorMessage = ioe.getMessage();
			} catch (Exception e) {
				errorMessage = e.getMessage();
			}
			this.cmbFID.setEnabled(true);
			this.cmbFID.setEditable(true);
		} else {
			errorMessage = "File could not be opened"; // NIO8N
		}
		if (!Strings.isEmpty(errorMessage)) {
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(null, errorMessage + "\n" + fcShapefile.getSelectedFile().getAbsolutePath(),
					this.TITLE, JOptionPane.INFORMATION_MESSAGE);
			this.setAlwaysOnTop(true);
		}
	}

	private void removeShapefileFromList() {
		// If nothing in list then nothing to do
		//
		if (this.ShapefileListModel.getSize() == 0) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		// If nothing selected, nothing to do
		//
		int index = this.lstShapefiles.getSelectedIndex();
		if (index < 0) {
			Toolkit.getDefaultToolkit().beep();
			return; // nothing to do
		}
		// Get element and subtract its record count from totalRecords
		try {
			// Subtract total records..
			//
			String fileName = this.ShapefileListModel.getElementAt(index).toString();
			ShapefileReaderJGeom sfh = new ShapefileReaderJGeom(fileName);
			this.totalRecords -= sfh.numRecords();
			sfh.closeShapefile();
			this.lblTotalRecords.setText("# " + this.totalRecords);
			this.lblTotalShapesToLoad.setText(String.format(this.totalShapesToLoad, this.totalRecords));
			// Now remove selected item
			//
			this.ShapefileListModel.removeElementAt(index);
			// Depending on what's left, do a refresh
			//
			int size = this.ShapefileListModel.getSize();
			// Nobody's left, disable firing.
			if (size == 0) {
				this.btnLoad.setEnabled(false);
				this.userFID = null;
				this.btnRemoveShp.setEnabled(false);
				// Clear anything left in table as no shapefiles to load
				this.tfTablename.setText("");
				this.shptm.fireTableDataChanged();
			} else { // Select an index.
				if (index == this.lstShapefiles.getModel().getSize()) {
					// removed item in last position
					index--;
				}
				this.lstShapefiles.setSelectedIndex(index);
				this.lstShapefiles.ensureIndexIsVisible(index);
			}
			// Refresh tblAttributes as we have removed a shapefile
			refreshTableOfAttributes();
			this.lstShapefiles.updateUI();
		} catch (IOException ioe) {
			// Error message and remove
			this.setAlwaysOnTop(false);
			JOptionPane.showMessageDialog(null,
					ioe.getMessage() + "\n" + fcShapefile.getSelectedFile().getAbsolutePath(), this.TITLE,
					JOptionPane.INFORMATION_MESSAGE);
			this.setAlwaysOnTop(true);
		}
	}

	private void refreshTableOfAttributes() {
		this.shptm.clearModel();
		this.cmbFID.setEnabled(false);
		try {
			for (int i = 0; i < this.ShapefileListModel.getSize(); i++) {
				this.shptm.loadShpAttributes(this.ShapefileListModel.getElementAt(i).toString());
			}
		} catch (Exception e) {
			// Any problems will have been found earlier
		}
		refreshCreateTable();
		refreshFIDs();
		if (this.ShapefileListModel.getSize() != 0)
			this.cmbFID.setEnabled(true);
	}

	/**
	 * @method isUserEnteredFID
	 * @param _colValue
	 * @return boolean
	 * @author Simon Greener, May 2010, Original Coding Simon Greener, May 26th
	 *         2010, Fixed bug with incorrect detection of selected item within
	 *         list. Simon Greener, May 27th 2010, Attempt to support user entered
	 *         FID across shapefile deletes and table refreshes.
	 */
	protected boolean isUserEnteredFID(String _colValue) {
		if (this.cmbFID.getSelectedItem().toString().equals(this.sNone)) {
			return false;
		}
		if (_colValue.equalsIgnoreCase(this.userFID)) {
			return true;
		}
		if (Strings.isEmpty(_colValue) || (this.cmbFID.getModel().getSize() == 1)) {
			return true;
		}
		String colValue = this.cmbFID.getSelectedItem().toString();
		for (int i = 0; i < shptm.getRowCount(); i++) {
			if (shptm.getValueAt(i, this.ora_col).toString().equalsIgnoreCase(colValue)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @method refreshFIDs
	 * @precis Rebuilds FID combo box adding in new attributes in response to
	 *         changes in number of shapefiles or Oracle data types entered
	 * @author Simon Greener, 20th May 2010, Original Coding Simon Greener, 27th May
	 *         2010, Attempt to support user entered FID values
	 */
	protected void refreshFIDs() {
		String currentSelection = "";
		if (this.cmbFID.getModel().getSize() > 1) {
			currentSelection = this.cmbFID.getSelectedItem().toString();
			this.cmbFID.removeAllItems();
			this.cmbFID.addItem(this.sNone);
		}
		for (int i = 0; i < shptm.getRowCount(); i++) {
			if (!((Boolean) shptm.getValueAt(i, this.is_load)).booleanValue()) {
				continue;
			}
			if (!ShapefileLoad.isDBFTypeNumeric((String) shptm.getValueAt(i, this.dbf_typ))) {
				continue;
			}
			this.cmbFID.addItem(shptm.getValueAt(i, ora_col).toString());
		}
		if (Strings.isEmpty(currentSelection) && Strings.isEmpty(this.userFID)) {
			this.cmbFID.setSelectedIndex(0);
		} else {
			this.cmbFID.setSelectedItem(currentSelection);
			// Was this a user Entered field?
			if (!Strings.isEmpty(this.userFID) && this.cmbFID.getSelectedIndex() < 0) {
				this.cmbFID.addItem(this.userFID);
			} else {
				this.cmbFID.setSelectedIndex(0);
			}
		}
		this.cmbFID.setEditable(true);
	}

	/**
	 * @method refreshCreateTable
	 * @precis Rebuilds CREATE TABLE statement depending on what has changed.
	 * @author Simon Greener, 20th May 2010, Original Coding Simon Greener, 27th May
	 *         2010, Simplified in attempt to support user entered FID values
	 */
	protected void refreshCreateTable() {
		String padString = "                                   "; // 35 = 30 (length oracle attribute) + padding
		this.taSQL.setText("");
		if (Strings.isEmpty(this.tfTablename.getText()))
			return;
		String sql = "CREATE TABLE " + Strings.append(this.getUserName(), this.tfTablename.getText().toString(), ".")
				+ " (\n";
		// Do we have a user entered fid to add?
		//
		if (isUserEnteredFID(this.cmbFID.getSelectedItem().toString())) {
			sql += "  " + (this.cmbFID.getSelectedItem().toString() + padString).substring(0, 34) + "INTEGER, \n";
		}
		// Process all columns that are to be loaded
		//
		for (int i = 0; i < shptm.getRowCount(); i++) {
			// is this to be loaded?
			//
			if (!((Boolean) shptm.getValueAt(i, is_load)).booleanValue()) {
				continue;
			}
			// Add column and type to string
			//
			sql += "  " + (shptm.getValueAt(i, ora_col).toString() + padString).substring(0, 34)
					+ shptm.getValueAt(i, ora_typ).toString() + ",\n";
		}
		// Add geometry
		//
		sql += "  " + (this.tfGeometryColumn.getText() + padString).substring(0, 34) + "MDSYS.SDO_GEOMETRY\n)"
				+ (this.cbNoLogging.isSelected() ? " NOLOGGING" : "");
		this.taSQL.setText(sql);
	}

	/**
	 * @method executeSQL
	 * @precis Executes common SQL like DROP TABLE and CREATE TABLE...
	 * @param _sql
	 * @throws SQLException
	 * @author Simon Greener 21st May, 2010 - Original coding
	 */
	public void executeSQL(String _sql) throws SQLException {
		Statement stmt;
		try {
			OracleConnection conn = DatabaseConnections.getInstance().getConnection(this.connName);
			stmt = conn.createStatement();
			stmt.executeUpdate(_sql);
			conn.commit();
			stmt.close();
		} catch (SQLException sqle) {
			if ((sqle.getErrorCode() != 955) && /* ORA-00955: name is already used by an existing object */
					(sqle.getErrorCode() != 942)) // SQL Error: ORA-00942: table or view does not exist
				throw new SQLException(sqle);
		}
	}

	// Functions for loading...
	//

	private void loadAllShapefiles() {
		int shpCount = this.ShapefileListModel.getSize();
		if (shpCount > 0) {
			int roundNumber = this.cmbDecimalPlaces.getSelectedItem().toString().equals(this.sNone) ? -1
					: Integer.valueOf(this.cmbDecimalPlaces.getSelectedItem().toString());
			DatabaseConnection dbConn = DatabaseConnections.getInstance().findConnectionName(this.connName);
			if (dbConn == null) {
				return;
			}
			if (dbConn.isOpen() == false) {
				dbConn.reOpen();
			}
			batchLoader work = new batchLoader(dbConn.getConnectionName(), dbConn.getUserName(),
					this.tfTablename.getText(), this.cmbFID.getSelectedItem().toString(),
					this.tfGeometryColumn.getText(), this.getSRID(), this.sNone, this.totalRecords,
					this.sldrCommitInterval.getValue(), roundNumber, this.cbMetadata.isSelected(),
					this.cbSpatialIndex.isSelected());
			ProgressBar progress = new ProgressBar(this, this.TITLE, work, false);
			progress.setCancelable(true);
			this.setAlwaysOnTop(false);
			work.setProgressBar(progress);
			progress.start("Loading " + shpCount + " shapefile" + ((shpCount == 1) ? "" : "s") + ", please wait...",
					this.ShapefileListModel.getElementAt(0).toString(), 1, this.totalRecords);
			/*
			 * progress.start("Loading " + shpCount + " shapefile" + ((shpCount == 1) ? "" :
			 * "s") + ", please wait...",
			 * this.ShapefileListModel.getElementAt(0).toString());
			 */
			progress.waitUntilDone();
			if (!Strings.isEmpty(work.errorMessage)) {
				// show error message
				JOptionPane.showMessageDialog(null, work.errorMessage, this.TITLE, JOptionPane.ERROR_MESSAGE);
			} else {
				java.util.Date endTime = new java.util.Date();
				long timeDiff = (endTime.getTime() - work.startTime.getTime());
				JOptionPane.showMessageDialog(null,
						(work.errorCount > 0) ? String.valueOf(work.errorCount) + " record(s) not converted.\n"
								: "" + String.valueOf(work.successCount) + " record(s) converted out of "
										+ String.valueOf(work.successCount + work.errorCount) + " processed.\n"
										+ "Processing time: " + Tools.milliseconds2Time(timeDiff),
						this.TITLE, JOptionPane.INFORMATION_MESSAGE);
			}
			this.setAlwaysOnTop(true);
			progress = null;
			work = null;
		}
	}

	class batchLoader implements Runnable {
		protected ProgressBar progressBar;
		protected String errorMessage;

		private String connName;

		protected int errorCount;
		protected long successCount;
		private long totalRecords;
		private long fid = -1;
		private String userName;
		private String tableName;
		private String fidName;
		private String geometryColumn;
		private ListModel<String> ShapeFileListModel;
		private String srid;
		protected java.util.Date startTime;
		private String NONE;
		private int commitInterval;
		private ShapefileLoad sfl;
		private int currentShapefile;
		private int shapeFileCount;
		private int roundNumber;
		private double migrateTolerance;
		private boolean createMetadata;
		private boolean createSpatialIndex;
		private String layerGType = null;
		MetadataEntry me = null;

		public batchLoader(String _connName, String _user, String _tableName, String _userFID, String _geomCol,
				String _srid, String _none, int _totalRecords, int _commitInterval, int _roundNumber,
				boolean _createMetadata, boolean _createSpatialIndex) {
			this.connName = _connName;
			this.errorCount = 0;
			this.successCount = 0;
			this.fidName = _userFID;
			this.userName = _user;
			this.tableName = _tableName;
			this.geometryColumn = _geomCol;
			this.srid = _srid;
			this.totalRecords = _totalRecords;
			this.NONE = _none;
			this.fid = _userFID.equalsIgnoreCase(this.NONE) ? -1 : 0;
			this.sfl = ShapefileLoad.getInstance();
			this.ShapeFileListModel = this.sfl.getShapefileModel();
			this.commitInterval = _commitInterval;
			this.shapeFileCount = 0;
			this.roundNumber = _roundNumber;
			this.migrateTolerance = (this.roundNumber == -1) ? 0.005 : 1 / Math.pow(1, this.roundNumber);
			this.createMetadata = _createMetadata;
			this.createSpatialIndex = _createSpatialIndex;
		}

		public void setProgressBar(ProgressBar _progressBar) {
			this.progressBar = _progressBar;
		}

		public void run() {
			this.startTime = new java.util.Date();
			this.shapeFileCount = this.ShapeFileListModel.getSize();
			this.currentShapefile = 0;

			// Create master metadata entry for the whole set
			//
			if (this.createMetadata) {
				me = new MetadataEntry(this.userName, this.tableName, this.geometryColumn, this.srid);
			}

			String shapeFileName = "";
			for (; this.currentShapefile < this.shapeFileCount; this.currentShapefile++) {
				shapeFileName = this.ShapeFileListModel.getElementAt(this.currentShapefile).toString();
				if (!Strings.isEmpty(shapeFileName)) {
					loadShapefile(shapeFileName, this.fidName, (this.fid >= 0));
				}
			}

			// Create spatial index?
			//
			if (this.createMetadata) {
				createMetadata();
			}

			// Create spatial index?
			//
			if (this.createSpatialIndex) {
				createIndex();
			}

			// "shut down" progressbar
			this.progressBar.setDoneStatus();
		}

		private void createIndex() {
			ManageSpatialIndex msi = ManageSpatialIndex.getInstance();
			String layer_gtype = (this.layerGType != null) ? ", layer_gtype=" + this.layerGType : "";
			String indexName = this.tableName + "_" + this.geometryColumn + "_SPX"; // Should get affix from preferences
			msi.setIndexName(indexName);
			// What if it has been condensed?
			//
			indexName = msi.getIndexName();
			String sql = "CREATE INDEX " + Strings.append(this.userName, indexName, ".") + "\n" + "    ON "
					+ Strings.append(this.userName, this.tableName, ".") + "(" + this.geometryColumn + ")\n"
					+ "INDEXTYPE IS MDSYS.SPATIAL_INDEX PARAMETERS('sdo_indx_dims=2 " + layer_gtype + "')";
			Messages.log("Indexing SQL\n" + sql);
			msi.setConnection(this.connName);
			msi.setSQL(sql);
			msi.executeCreateIndex();
		}

		public String getFileName(String _fullName) {
			int i = _fullName.lastIndexOf(File.separatorChar);
			return (i == -1) ? _fullName : _fullName.substring(i + 1);
		}

		private void displayProgress() {
			/*
			 * Need to create parameters for function:
			 *
			 * updateProgress(String progressText, String stepText)
			 *
			 * progressText - The text to display just above the progress indicator. If a
			 * null string or zero-length string is passed in the text will not be updated.
			 * stepText - The text to display just below the progress indicator. If a null
			 * string or zero-length string is passed in the text will not be updated.
			 */
			String progressText = "Loading " + this.shapeFileCount + " shapefile"
					+ ((this.shapeFileCount == 1) ? "" : "s") + ", please wait...";
			String stepText = null;

			long processedRecords = this.errorCount + this.successCount;
			if (processedRecords > 0 && this.totalRecords != 0) {
				/// Time format for our dates
				//
				DateFormat outFormat = DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault());

				// get Start Time and projected Finish Time.
				//
				java.util.Date now = new java.util.Date();
				long timeDiff = (now.getTime() - this.startTime.getTime());

				// New time in future will be startTime + (diff*(totalRecords/processedRecords))
				//
				java.util.Date estimatedFinish = new java.util.Date(
						this.startTime.getTime() + (timeDiff * totalRecords / processedRecords));

				// Format is: Percentage: StartTime/EstimateFinishTime (time to go)
				//
				progressText = String.format("%3d%%: %s-%s (%s)",
						(int) Math.round(((double) processedRecords / (double) this.totalRecords) * 100.0D),
						outFormat.format(this.startTime).toString().replace(" ", ""),
						outFormat.format(estimatedFinish).toString().replace(" ", ""),
						Tools.milliseconds2Time(((estimatedFinish.getTime() - startTime.getTime()) - timeDiff)));

				String fileName = getFileName(this.ShapeFileListModel.getElementAt(this.currentShapefile).toString());
				stepText = String.format("Processing %s (%s of %s)", fileName,
						String.valueOf(this.currentShapefile + 1).trim(), String.valueOf(this.shapeFileCount).trim());
			}
			this.progressBar.updateProgress((int) processedRecords, progressText, stepText);
		}

		private boolean isOracleNumber(String _oracleDataType) {
			String oracleDataType = _oracleDataType.toUpperCase(Locale.getDefault());
			return (oracleDataType.startsWith("NUMBER") || oracleDataType.startsWith("BINARY_FLOAT")
					|| oracleDataType.startsWith("BINARY_DOUBLE") || oracleDataType.startsWith("NUMERIC")
					|| oracleDataType.startsWith("DECIMAL") || oracleDataType.startsWith("FLOAT")
					|| oracleDataType.startsWith("REAL") || oracleDataType.startsWith("DOUBLE PRECISION"));
		}

		private boolean isOracleInteger(String _oracleDataType) {
			return (_oracleDataType.equalsIgnoreCase("INTEGER") || _oracleDataType.equalsIgnoreCase("INT")
					|| _oracleDataType.equalsIgnoreCase("SMALLINT"));
		}

		private boolean isOracleDate(String _oracleDataType) {
			return (_oracleDataType.equalsIgnoreCase("DATE"));
		}

		/**
		 * @method fixNumber
		 * @precis Fixes string number so it matches the Oracle numeric data type.
		 *
		 *         For example, if you let Oracle do an implicit conversion of
		 *         1234.56789 into a number(7,3) it will work. But if it was number(6,3)
		 *         it won't as there aren't enough precision digits for the 1234. The
		 *         error message would be:
		 *
		 *         SQL Error: ORA-01438: value larger than specified precision allowed
		 *         for this column 01438. 00000 - "value larger than specified precision
		 *         allowed for this column"
		 *
		 *         Similarly, if the string contained non-numeric items then these need
		 *         to be trapped.
		 *
		 * @param _oracleDataType
		 * @param _number
		 * @param roundLeft       -- Divide precision by power(10,maxPossible) - true -
		 *                        or Mod(precisionValue,power(10,maxPossible) false
		 * @return String
		 * @author Simon Greener May 25th 2010 - Original Coding
		 */
		private String fixNumber(String _oracleDataType, String _number, boolean roundLeft) {
			String result = null;
			Integer intVal = null;
			Double dblVal = null;
			// First off, let's check if it is a valid number
			//
			try {
				intVal = Integer.parseInt(_number);
				if (isOracleInteger(_oracleDataType) || (_oracleDataType.indexOf("(") < 0))
					return _number; // No need to truncate the number
			} catch (Exception ie) {
				try {
					dblVal = Double.parseDouble(_number);
				} catch (Exception de) {
					return null;
				}
			}
			// Yes it is
			// Now match it against the Oracle specification
			//

			int scale = 0;
			int precision = 0;
			String precisionToken = "";
			String scaleToken = "";

			try {
				// Get precision and, if exists, scale
				//
				StringTokenizer it = new StringTokenizer(_oracleDataType, "(,)", false);
				if (it.countTokens() < 2) // DOUBLE_INT ?
					return _number;
				precisionToken = it.nextToken(); // discard data type
				precisionToken = it.nextToken(); // this should be the precision
				precision = Integer.valueOf(precisionToken);
				scaleToken = it.hasMoreTokens() ? it.nextToken() : "";
				scale = (scaleToken.length() == 0) ? 0 : Integer.valueOf(scaleToken);

				// Now make number match that which is required
				//
				int leftSize = (intVal != null) ? String.valueOf(Math.abs(intVal)).length()
						: String.valueOf(Math.abs(dblVal.intValue())).length();
				double leftVal = roundLeft ? Math.pow(10, leftSize - (precision - scale))
						: Math.pow(10, precision - scale);
				leftVal = leftVal < 1 ? 1 : leftVal;
				String fmt = "%" + precision + ((scale == 0) ? ".0" : "." + String.valueOf(scale)) + "f";
				result = roundLeft
						? String.format(fmt, (intVal == null ? dblVal : Double.valueOf(intVal)) / leftVal).trim()
						: String.format(fmt, (intVal == null ? dblVal : Double.valueOf(intVal)) % leftVal).trim();
				return result;
			} catch (Exception e) {
				LOGGER.warning(
						"Problem fixing number " + _number + " for " + _oracleDataType + " ==> " + e.getMessage());
			}
			return _number;
		}

		private String getLayerGType(int _shpFileType, String _existingLayerGType) {
			if (!Strings.isEmpty(_existingLayerGType)) {
				if (_existingLayerGType.equals("COLLECTION"))
					return _existingLayerGType;
			}

			String layerGType = "";
			switch (_shpFileType) {
			case ShapefileReaderJGeom.AV_NULL:
				break;
			case ShapefileReaderJGeom.AV_MULTIPOINT:
			case ShapefileReaderJGeom.AV_MULTIPOINTM:
			case ShapefileReaderJGeom.AV_MULTIPOINTZ: {
				layerGType = "MULTIPOINT";
				break;
			}
			case ShapefileReaderJGeom.AV_POINT:
			case ShapefileReaderJGeom.AV_POINTM:
			case ShapefileReaderJGeom.AV_POINTZ: {
				layerGType = (Strings.isEmpty(_existingLayerGType) ? "POINT"
						: (_existingLayerGType.indexOf("POINT") != -1 ? "MULTIPOINT" : "POINT"));
				break;
			}
			case ShapefileReaderJGeom.AV_POLYGON:
			case ShapefileReaderJGeom.AV_POLYGONM:
			case ShapefileReaderJGeom.AV_POLYGONZ: {
				layerGType = "MULTIPOLYGON";
				break;
			}
			case ShapefileReaderJGeom.AV_POLYLINE:
			case ShapefileReaderJGeom.AV_POLYLINEM:
			case ShapefileReaderJGeom.AV_POLYLINEZ: {
				layerGType = "MULTILINE";
				break;
			}
			}

			return Strings.isEmpty(_existingLayerGType) ? layerGType
					: (_existingLayerGType.equals(layerGType) ? layerGType : "COLLECTION");
		}

		/**
		 * Can shapefile have Z and M?
		 */
		private boolean hasM(int _shpFileType) {
			boolean has = false;
			switch (_shpFileType) {
			case ShapefileReaderJGeom.AV_MULTIPOINTM:
			case ShapefileReaderJGeom.AV_POINTM:
			case ShapefileReaderJGeom.AV_POLYGONM:
			case ShapefileReaderJGeom.AV_POLYLINEM: {
				has = true;
				break;
			}
			case ShapefileReaderJGeom.AV_NULL:
			case ShapefileReaderJGeom.AV_MULTIPOINT:
			case ShapefileReaderJGeom.AV_POINT:
			case ShapefileReaderJGeom.AV_POLYGON:
			case ShapefileReaderJGeom.AV_POLYLINE:
			case ShapefileReaderJGeom.AV_MULTIPOINTZ:
			case ShapefileReaderJGeom.AV_POINTZ:
			case ShapefileReaderJGeom.AV_POLYGONZ:
			case ShapefileReaderJGeom.AV_POLYLINEZ: {
				has = false;
				break;
			}
			}
			return has;
		}

		private boolean hasZ(int _shapeFileType) {
			boolean has = false;
			switch (_shapeFileType) {
			case ShapefileReaderJGeom.AV_MULTIPOINTZ:
			case ShapefileReaderJGeom.AV_POINTZ:
			case ShapefileReaderJGeom.AV_POLYGONZ:
			case ShapefileReaderJGeom.AV_POLYLINEZ: {
				has = true;
				break;
			}
			case ShapefileReaderJGeom.AV_NULL:
			case ShapefileReaderJGeom.AV_MULTIPOINT:
			case ShapefileReaderJGeom.AV_POINT:
			case ShapefileReaderJGeom.AV_POLYGON:
			case ShapefileReaderJGeom.AV_POLYLINE:
			case ShapefileReaderJGeom.AV_MULTIPOINTM:
			case ShapefileReaderJGeom.AV_POINTM:
			case ShapefileReaderJGeom.AV_POLYGONM:
			case ShapefileReaderJGeom.AV_POLYLINEM: {
				has = false;
				break;
			}
			}
			return has;
		}

		private void updateMetadata(ShapefileReaderJGeom _sfh, boolean _z, boolean _m) {
			double maxMeasure = _sfh.getMaxMeasure();
			if (maxMeasure <= -10E38)
				maxMeasure = Double.NaN;

			// Make sure a master record exists for this shapefile set (based on target
			// table/column)
			//
			if (this.me == null)
				me = new MetadataEntry(this.userName, this.tableName, this.geometryColumn, this.srid);

			// We need to know if the SRID of this table is geodetic
			//
			OracleConnection conn = DatabaseConnections.getInstance().getConnection(this.connName);
			Constants.SRID_TYPE sridType = null;
			if (conn == null) {
				sridType = Tools.discoverSRIDType(this.srid);
			} else {
				sridType = Tools.discoverSRIDType(conn, this.srid);
			}
			boolean isLatLong = sridType.toString().toUpperCase().startsWith("GEO") ? true : false;
			me.set(isLatLong ? "Longitude" : "X", _sfh.getMinX(), _sfh.getMaxX(),
					isLatLong ? 0.05 : this.migrateTolerance);
			me.set(isLatLong ? "Latitude" : "Y", _sfh.getMinY(), _sfh.getMaxY(),
					isLatLong ? 0.05 : this.migrateTolerance);
			if (_z)
				me.set("Z", _sfh.getMinZ(), _sfh.getMaxZ(), this.migrateTolerance);
			if (_m && !Double.isNaN(maxMeasure))
				me.set("M", _sfh.getMinMeasure(), maxMeasure, this.migrateTolerance);
		}

		private void createMetadata() {
			String sql = "";
			try {
				// Delete any existing record
				//
				sql = me.deleteSQL();
				this.sfl.executeSQL(sql);
				OracleConnection conn = DatabaseConnections.getInstance().getConnection(this.connName);
				conn.commit();

				// Insert new Metadata Entry
				//
				sql = me.insertSQL(false);
				this.sfl.executeSQL(sql);
				conn.commit();
			} catch (SQLException sqle) {
				this.sfl.setAlwaysOnTop(false);
				JOptionPane.showMessageDialog(null, sql + "\n" + sqle.getLocalizedMessage(), "Metadata Creation",
						JOptionPane.INFORMATION_MESSAGE);
				this.sfl.setAlwaysOnTop(true);
			}
		}

		public double roundToDecimals(double d, int c) {
			long temp = (long) ((d * Math.pow(10, c)));
			return (((double) temp) / (double) Math.pow(10, c));
		}

		private int getGType(JGeometry _geom) {
			return ((_geom.getDimensions() * 1000) + ((_geom.isLRSGeometry() && _geom.getDimensions() == 3) ? 300
					: ((_geom.isLRSGeometry() && _geom.getDimensions() == 4) ? 400 : 0)) + _geom.getType());
		}

		private JGeometry roundOrdinates(JGeometry _geom, int _round) 
		{
			int gType = getGType(_geom);
			int dimension = _geom.getDimensions();
			Double x = null, y = null, z = null;

			double[] ords = _geom.getOrdinatesArray();
			if (ords != null) {
				int ordLength = ords.length;
				if (ordLength != 0) {
					for (int i = 0; i < ordLength; i++) {
						ords[i] = roundToDecimals(ords[i], _round);
					}
				}
			}
			if (_geom.isPoint() && ords == null) {
				// A point could be stored in the ordinate array
				// If not, it it in the SDO_POINT_TYPE and needs processing
				//
				double[] point = _geom.getFirstPoint();
				if (point != null && point.length != 0) {
					x = roundToDecimals(point[0], _round);
					y = roundToDecimals(point[1], _round);
					if (dimension > 2) {
						z = roundToDecimals(point[2], _round);
						return new JGeometry(x, y, z, _geom.getSRID());
					} else {
						return new JGeometry(x, y, _geom.getSRID());
					}
				} else {
					return _geom;
				}
			}
			return new JGeometry(gType, _geom.getSRID(), _geom.getElemInfo(), ords);
		}

		/**
		 * @method loadShapefile
		 * @param _shapefileName
		 * @param _idName        Feature Identifier Primary Key Column Name
		 * @param _fidProcessing Are we adding a FID?
		 * @author Simon Greener, May 2010
		 * @description Loads a shape-file into a newly created table Shapefile can be
		 *              one of a batch and may have attributes that are not to be
		 *              loaded, or for which no value exists in the oracle table as
		 *              column may come from another table.
		 */
		private void loadShapefile(String _shapefileName, String _idName, boolean _fidProcessing) {
			OracleConnection conn = DatabaseConnections.getInstance().getConnection(this.connName);

			DBFReaderJGeom dbfr = null;
			ShapefileReaderJGeom sfh = null;
			@SuppressWarnings("unused")
			ShapefileFeatureJGeom sf = null;
			try {
				// Open dbf and input files
				dbfr = new DBFReaderJGeom(_shapefileName.replaceAll("\\.[Ss][Hh][Pp]$", "") + ".dbf");
				sfh = new ShapefileReaderJGeom(_shapefileName);
				sf = new ShapefileFeatureJGeom();
			} catch (IOException ioe) {
				LOGGER.error("IO Read Error: " + ioe.getMessage());
				return;
			}

			////////////////////////////////////////////////////////////////////////////
			// Conversion from Feature to DB
			////////////////////////////////////////////////////////////////////////////

			Hashtable<?, ?> ht = new Hashtable<Object, Object>();
			int numFields = dbfr.numFields();
			int numRecords = dbfr.numRecords();
			int shpFileType = sfh.getShpFileType();
			int shpRecords = sfh.numRecords();
			if (shpRecords != numRecords) {
				LOGGER.warning("WARNING: DBF (" + numRecords + ") and SHP (" + shpRecords
						+ ") files have different record counts");
			}

			// Append metadata for this shapefile to the set
			//
			if (this.createMetadata) {
				updateMetadata(sfh, hasZ(shpFileType), hasM(shpFileType));
			}

			// Get layerGType (which the function "unions" together with the existing global
			// variable
			//
			this.layerGType = getLayerGType(shpFileType, this.layerGType);

			// Get DBF data for conversion
			// 1. Get actual DBF names and data types irrespective as to what user wants to
			// load
			// 2. Oracle Utility's hash table creates keys based on lower case versions of
			// original DBF (which it calls its "Oracle COlumn Names")
			//
			String[] dbfColNames = new String[dbfr.numFields()];
			byte[] fieldTypes = new byte[numFields];
			for (int field = 0; field < dbfr.numFields(); field++) {
				dbfColNames[field] = dbfr.getFieldName(field).toLowerCase(Locale.getDefault());
				fieldTypes[field] = dbfr.getFieldType(field);
			}

			// ====================================================
			// Create prepared statement
			// 1. Check if user fid has been requested and place at front of column and
			// parameter (values) lists
			//
			int colsToStore = 0;
			String insertCols = " (";
			String params = "(";
			boolean userFID = false;
			if (isUserEnteredFID(_idName) && _fidProcessing) {
				userFID = true;
				// Add extra primary key value parameter
				//
				colsToStore = 1; // Flag we have already added one
				insertCols += _idName + ",";
				params += "?,";
			}

			// 2. Get Oracle Columns names for those rows in shptm that are related to this
			// shapefile and are to be loaded
			//
			Vector<Object[]> columnData = this.sfl.shptm.getLoadColumnData(dbfColNames);
			ListIterator<Object[]> dataIter = columnData.listIterator();
			while (dataIter.hasNext()) {
				Object[] obj = dataIter.next();
				colsToStore++;
				insertCols += ((String) obj[shptm.getColumnPosition(columns.ORACLE_COLUMN_NAME)]) + ",";
				params += "?, ";
			}
			insertCols += this.geometryColumn + ")";

			// 3. Add parameter for sdo_geometry but....
			// Migrate (ie fix rotation etc) geometry if polygon, polygonz, or polygonm
			// rather than just write converted JGeometry
			// Remove last ? and replace with TO_CURRENT(?
			//
			colsToStore++;
			if (shpFileType == 5 || shpFileType == 15 || shpFileType == 25) {
				params += "MDSYS.SDO_MIGRATE.TO_CURRENT(?," + Constants.TAG_MDSYS_SDO_DIMARRAY + "("
						+ MetadataTool.createDimElement("X", "0", "1", String.valueOf(this.migrateTolerance)) + ","
						+ MetadataTool.createDimElement("Y", "0", "1", String.valueOf(this.migrateTolerance))
						+ ((shpFileType == 15)
								? "," + MetadataTool.createDimElement("Z", "0", "1",
										String.valueOf(this.migrateTolerance))
								: "")
						+ ((shpFileType == 25) ? ","
								+ MetadataTool.createDimElement("M", "0", "1", String.valueOf(this.migrateTolerance))
								: "")
						+ "))";
			} else {
				params += "?";
			}
			params += ")";

			// 4. Create INSERT Statement
			//
			String targetObjectName = Strings.append(this.userName, this.tableName, ".");
			String insertSQL = "INSERT INTO " + targetObjectName + insertCols + " VALUES " + params;
			LOGGER.logSQL(insertSQL);

			// 5. Create statement for execution
			//
			PreparedStatement ps;
			try {
				ps = conn.prepareStatement(insertSQL);
			} catch (SQLException e) {
				LOGGER.error("ERROR preparing SQL Statement: \n" + insertSQL);
				return;
			}

			// Variables for conversion loop
			//
			JGeometry geom = null;
			Struct geometrySTRUCT = null;
			int parameterIndex = 0;
			String oracleColumnName = "";
			String oracleDataType = "";
			@SuppressWarnings("unused")
			String dbfType = null;
			
			// Now process all records in shapefile DBF
			//
			for (int rec = 1; rec <= numRecords; rec++) {
				try {
					// Use Oracle conversion utility to get feature Get record (i)
					// NOTE: this approach by the Oracle Utility does not handle DATES!
					//
					ht = ShapefileFeatureJGeom.fromRecordToFeature(dbfr, sfh, fieldTypes, numFields, (rec - 1),
							getSRIDAsInteger());
				} catch (IOException ioe) {
					this.errorMessage = ioe.getMessage();
					// printHashtable(ht);
					return;
				}

				// 1. Create primary key id value if needed
				//
				parameterIndex = 1;
				if (_fidProcessing) {
					this.fid++;
					if (userFID) {
						// User entered FID is always first in parameters
						try {
							ps.setLong(parameterIndex++, this.fid);
						} catch (SQLException sqle) {
							LOGGER.warning(
									"Record# " + rec + " ERROR converting " + oracleColumnName + "(" + oracleDataType
											+ ") = " + ht.get(oracleColumnName).toString() + sqle.getMessage());
						}
					}
				}

				// 2. Convert all the non-geometry column data for this record
				//
				ListIterator<Object[]> setIter = columnData.listIterator();
				while (setIter.hasNext()) {
					try {
						Object[] obj = setIter.next();
						oracleColumnName = ((String) obj[shptm.getColumnPosition(columns.DBF_COLUMN_NAME)])
								.toLowerCase(Locale.getDefault());
						oracleDataType = ((String) obj[shptm.getColumnPosition(columns.ORACLE_DATA_TYPE)])
								.toUpperCase(Locale.getDefault());
						dbfType = (String) obj[shptm.getColumnPosition(columns.DBF_DATA_TYPE)];
						// columnBeingProcessed only includes columns in the target table that are in
						// this shapefile
						// The check that the column is in the hash table should not be needed.
						//
						if (ht.get(oracleColumnName) == null) {
							continue;
						}
						if (_fidProcessing && (!userFID) && oracleColumnName.equalsIgnoreCase(_idName)) {
							if ((ht.get(oracleColumnName) instanceof Integer)) {
								ps.setLong(parameterIndex++, this.fid);
							} else if ((ht.get(oracleColumnName) instanceof Double)) {
								ps.setDouble(parameterIndex++, this.fid);
							} else {
								ps.setLong(parameterIndex++, this.fid);
							}
						} else if ((ht.get(oracleColumnName) instanceof String)) {
							// Is it being converted to an Oracle number?
							//
							if (isOracleInteger(oracleDataType)) {
								ps.setInt(parameterIndex++,
										Integer.valueOf(ht.get(oracleColumnName).toString()).intValue());
							} else if (isOracleNumber(oracleDataType)) {
								// need precision and scale
								String number = ht.get(oracleColumnName).toString();
								if (Strings.isEmpty(number)) {
									ps.setNull(parameterIndex++, Types.DOUBLE);
								} else {
									number = fixNumber(oracleDataType, number, false);
									if (Strings.isEmpty(number)) {
										ps.setNull(parameterIndex++, Types.DOUBLE);
									} else {
										ps.setDouble(parameterIndex++, Double.valueOf(number).doubleValue());
									}
								}
							} else {
								ps.setString(parameterIndex++, ht.get(oracleColumnName).toString());
							}
						} else if (isOracleDate(oracleDataType)) {
							String d = ht.get(oracleColumnName).toString().trim();
							if (d.length() == 8)
								d = /* Year */d.substring(0, 4) + "-" + /* Month */d.substring(4, 6) + "-"
										+ /* Day */d.substring(6, 8);
							ps.setDate(parameterIndex++, (d.length() < 8 ? (Date) null : Date.valueOf(d)));
						} else if ((ht.get(oracleColumnName) instanceof Integer))
							ps.setInt(parameterIndex++,
									Integer.valueOf(ht.get(oracleColumnName).toString()).intValue());
						else if ((ht.get(oracleColumnName) instanceof Double))
							ps.setDouble(parameterIndex++,
									Double.valueOf(ht.get(oracleColumnName).toString()).doubleValue());
						else // Unsupported Column Type ? Try String
							ps.setString(parameterIndex++, ht.get(oracleColumnName).toString());
					} catch (SQLException sqle) {
						LOGGER.warning("Record# " + rec + " ERROR converting " + oracleColumnName + "(" + oracleDataType
								+ ") = " + ht.get(oracleColumnName).toString() + sqle.getMessage());
					}
				} // end for loop for dbfColNames

				// 3.1 Convert geometry of shapefile
				//
				try {
					geom = (JGeometry) ht.get("geometry");
					if (this.roundNumber != -1) {
						geom = roundOrdinates(geom, this.roundNumber);
					}
					geometrySTRUCT = JGeometry.storeJS(geom,conn);
				} catch (Exception e) {
					LOGGER.warn("Converting geometry of Record #" + rec + " failed - writing NULL value");
					geometrySTRUCT = null; // Inserting of null geometries allowed
				}

				// 3.2 Set geometry value into prepared statement
				//
				try {
					if (geometrySTRUCT == null) {
						ps.setNull(colsToStore, OracleTypes.STRUCT, Constants.TAG_MDSYS_SDO_GEOMETRY);
					} else {
						ps.setObject(colsToStore, geometrySTRUCT);
					}
				} catch (SQLException sqle) {
					this.errorCount++;
					LOGGER.warning("Record# " + rec + " ERROR converting geometry column " + sqle.getMessage());
				}

				// 4. Send record to Oracle
				// This should be done via more efficient "array" processing
				//
				try {
					ps.executeUpdate();
					this.successCount++;
				} catch (SQLException sqle) {
					this.errorCount++;
					LOGGER.warning("SQL ERROR: " + sqle.getMessage() + "\nRecord #" + rec + " not converted.");
				}

				// Commit and at the same time, display progress and check for termination
				//
				if ((rec % this.commitInterval) == 0) {
					try {
						conn.commit();
					} catch (SQLException sqle) {
						LOGGER.warning("SQL COMMIT ERROR: " + sqle.getMessage() + " at Record #" + rec + ".");
					}
					if (this.progressBar.hasUserCancelled()) {
						try {
							dbfr.closeDBF();
							dbfr = null; // displose of handle
							sfh.closeShapefile();
							sfh = null;
							ps.close();
						} catch (Exception e) {
							LOGGER.warn("Closing shapefile and statement: " + e.getMessage());
						}
						return;
					}
					displayProgress();
					// Check if error count == rows processed
					//
					if (this.errorCount == this.successCount) {
						setAlwaysOnTop(false);
						int result = JOptionPane.showConfirmDialog(null, "Errors () equals Rows Processed ().\nQuit?",
								TITLE, JOptionPane.YES_NO_CANCEL_OPTION);
						setAlwaysOnTop(true);
						if (result == JOptionPane.YES_OPTION) {
							try {
								dbfr.closeDBF();
								dbfr = null; // displose of handle
								sfh.closeShapefile();
								sfh = null;
								ps.close();
							} catch (Exception e) {
								LOGGER.warn("Closing shapefile and statement: " + e.getMessage());
							}
							return;
						}
					}
				}
			} // end_for_each_record

			try {
				dbfr.closeDBF();
				dbfr = null; // displose of handle
				sfh.closeShapefile();
				sfh = null;
			} catch (IOException ioe) {
				this.errorMessage = ioe.getMessage();
			}
			try {
				conn.commit();
				ps.close();
				displayProgress();
			} catch (SQLException sqle) {
				this.errorMessage = sqle.getMessage();
			}

		}

		/**
		 * @method printHashtable
		 * @param ht
		 * @precis DEBUG: For printing out java.util.Oracle hashtable
		 * @author Simon Greener, May 2010
		 **/
		@SuppressWarnings("unused")
		private void printHashtable(Hashtable<?, ?> ht) {
			Set<?> keys = ht.keySet(); // The set of keys in the map.
			Iterator<?> keyIter = keys.iterator();
			System.out.println("The hashmap contains the following associations:");
			while (keyIter.hasNext()) {
				Object key = keyIter.next(); // Get the next key.
				Object value = ht.get(key); // Get the value for that key.
				System.out.println("   (" + key + "," + value + ")");
			}
		}
	}

	public static String mapDBFTypeToOracle(char _dbfType, int _length, int _decPlaces) {
		String dataType = "";
		switch (_dbfType) {
		case 'C':
			if (_length > 0) {
				dataType = "VARCHAR2(" + String.valueOf(_length) + ")";
			} else {
				dataType = "TIMESTAMP";
			}
			break;
		case 'D':
			dataType = "DATE";
			break;
		case 'L':
			dataType = "CHAR(1)";
			break;
		case 'M': /* Memo */
			dataType = "CLOB";
			break;
		case 'P': /* Picture */
			dataType = "BLOB";
			break;
		case 'F':
			dataType = "FLOAT(" + String.valueOf(Math.round(_length * 3.32193)) + ")";
			break;
		case 'N':
			int length = (_length > 38) ? 38 : _length;
			if (_decPlaces != 0) {
				int decPlaces = (int) ((_length > 38) ? (38.0 * (((double) _decPlaces) / ((double) _length)))
						: _decPlaces);
				dataType = "NUMBER(" + String.valueOf(length) + "," + String.valueOf(decPlaces) + ")";
			} else {
				dataType = "NUMBER(" + String.valueOf(_length) + ")";
			}
			break;
		default:
			dataType = "VARCHAR2(254)";
			break;
		}
		return dataType;
	}

	private static boolean isDBFTypeNumeric(String _dbfType) {
		return (_dbfType.equalsIgnoreCase("N") || (_dbfType.equalsIgnoreCase("F")));
	}

	@SuppressWarnings("unused")
	private static boolean isDBFTypeInteger(String _dbfType, int _length) {
		return _length == 0 && isDBFTypeNumeric(_dbfType);
	}

	// Don't use hardcoded column integers eg 0-4 throughout code
	//
	public static enum columns {
		DBF_COLUMN_NAME, ORACLE_COLUMN_NAME, ORACLE_DATA_TYPE, IS_LOAD, DBF_DATA_TYPE
	};

	private int dbf_col = 0;
	private int ora_col = 1;
	private int ora_typ = 2;
	private int is_load = 3;
	private int dbf_typ = 4;

	class SHPTableModel extends AbstractTableModel 
	{
		private static final long serialVersionUID = -549537698068406576L;
		public String[] headers = new String[] { "DBF Column", "Oracle Column", "Data Type", "Load?" };
		@SuppressWarnings("rawtypes")
		Class[] types = new Class[] { java.lang.String.class, 
                                      java.lang.String.class, 
                                      java.lang.String.class,
                                      Boolean.class };

		boolean[] canEdit = new boolean[] { false, true, true, true };

		private int cacheCapacity = 255;
		Vector<Object[]> cache; // will hold table column metadata

		public SHPTableModel() {
			cache = new Vector<Object[]>(cacheCapacity);
		}

		public boolean inNameSet(String _name, String[] _nameSet) {
			if (_nameSet == null || _nameSet.length == 0 || Strings.isEmpty(_name)) {
				return false;
			}
			for (int i = 0; i < _nameSet.length; i++) {
				if (_name.equalsIgnoreCase(_nameSet[i])) {
					return true;
				}
			}
			return false;
		}

		public String getColumnName(int i) {
			return headers[i];
		}

		public int getColumnPosition(ShapefileLoad.columns _col) {
			int position = -1;
			switch (_col) {
			case DBF_COLUMN_NAME:
				position = dbf_col;
				break;
			case ORACLE_COLUMN_NAME:
				position = ora_col;
				break;
			case ORACLE_DATA_TYPE:
				position = ora_typ;
				break;
			case IS_LOAD:
				position = is_load;
				break;
			case DBF_DATA_TYPE:
				position = dbf_typ;
				break;
			}
			return position;
		}

		public int getColumnCount() {
			return headers.length;
		}

		public int getRowCount() {
			return cache.size();
		}

		public String getDBFLoadType(String _field) {
			String b = null;
			ListIterator<Object[]> cacheIter = cache.listIterator();
			while (cacheIter.hasNext()) {
				Object[] obj = cacheIter.next();
				if (((String) obj[dbf_col]).equalsIgnoreCase(_field)) {
					b = (String) obj[dbf_typ];
					break;
				}
			}
			return b;
		}

		public Vector<Object[]> getLoadColumnData(String[] _nameSet) {
			Vector<Object[]> returnVector = new Vector<Object[]>(cacheCapacity);
			ListIterator<Object[]> cacheIter = cache.listIterator();
			while (cacheIter.hasNext()) {
				Object[] obj = cacheIter.next();
				if (!(Boolean) obj[is_load])
					continue;
				if ((_nameSet == null) || (inNameSet(obj[dbf_col].toString(), _nameSet)))
					returnVector.addElement(obj);
			}
			return returnVector;
		}

		public String getColumnType(int row) {
			return (String) cache.elementAt(row)[ora_typ];
		}

		public String getDBFType(int row) {
			return (String) cache.elementAt(row)[dbf_typ];
		}

		public void addRow(Object[] row) {
			cache.addElement(row);
		}

		private boolean valueExists(String name) {
			boolean exists = false;
			ListIterator<Object[]> chcIter = cache.listIterator();
			while (chcIter.hasNext()) {
				Object[] o = chcIter.next();
				if (((String) o[dbf_col]).equalsIgnoreCase(name)) {
					exists = true;
					break;
				}
			}
			return exists;
		}

		/**
		 * JTable uses this method to determine the default renderer/ editor for each
		 * cell.
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

		public void setValueAt(Object value, int row, int col) {
			if (!canEdit[col])
				return;
			// Check for common typo
			if (col == getColumnPosition(columns.ORACLE_DATA_TYPE)) {
				String v = (String) value;
				value = (Object) v.replace("))", ")");
			}
			Object[] obj = cache.elementAt(row);
			obj[col] = value;
			cache.setElementAt(obj, row);
			this.fireTableDataChanged(); // CellUpdated(row, col);
		}

		public boolean isCellEditable(int row, int col) {
			return canEdit[col];
		}

		public void loadShpAttributes(String shapefileName) throws Exception {
			if (cache == null) { // This shouldn't happen
				cache = new Vector<Object[]>(cacheCapacity);
			}
			DBF aDB = null;
			String fName = shapefileName.replaceAll("\\.[Ss][Hh][Pp]$", "") + ".dbf";
			try {
				aDB = new DBF(fName, 'r');
			} catch (Exception e) {
				throw new Exception("Error opening " + fName + " (" + e.toString() + ")");
			}
			int fieldCount = 0;
			int numFields = aDB.getFieldCount();
			for (int field = 1; field <= numFields; field++) {
				try {
					Field dbFld = aDB.getField(field);
					// Don't load if already exists
					// We assume here that the shapefile being loaded does not have duplicate DBF
					// attribute names
					//
					if (!valueExists(dbFld.getName())) {
						Object[] record = new Object[5];
						record[dbf_col] = dbFld.getName();
						record[ora_col] = dbFld.getName().toUpperCase(Locale.getDefault());
						record[ora_typ] = mapDBFTypeToOracle(dbFld.getType(), dbFld.getLength(),
								dbFld.getDecimalPositionCount());
						record[is_load] = Boolean.TRUE;
						record[dbf_typ] = String.valueOf(dbFld.getType());
						// add Metadata to internal cache
						//
						// LOGGER.info("about to add field " + field + " out of " + numFields + ": " +
						// record[ora_col] + " " + record[ora_typ]);
						this.addRow(record);
						fieldCount++;
					}
				} catch (Exception e) {
					LOGGER.info("Field not compatible with driver: " + e.toString());
				}
			} // for
			if (fieldCount > 0) {
				this.fireTableRowsInserted(0, fieldCount); // notify everyone that we have a new table.
			}
		}

		public void loadOraAttributes(LinkedHashMap<String, String> _attributes) {
			if (cache == null) { // This shouldn't happen
				cache = new Vector<Object[]>(cacheCapacity);
			}
			String columnName;
			Iterator<String> iter = _attributes.keySet().iterator();
			while (iter.hasNext()) {
				columnName = iter.next();
				Object[] record = new Object[5];
				record[dbf_col] = columnName;
				record[ora_col] = "";
				record[ora_typ] = _attributes.get(columnName);
				record[is_load] = Boolean.TRUE;
				record[dbf_typ] = 'C';
				// add Metadata to internal cache
				//
				this.addRow(record);
			}
		}

		public void clearModel() {
			if (cache == null) {
				cache = new Vector<Object[]>(cacheCapacity);
			} else {
				cache.clear();
			}
			this.fireTableDataChanged();
		}

		/**
		 * When edit a cell eg Oracle column name or data type, we need to refresh the
		 * table in the SQL text area
		 */
		public void fireTableCellUpdated(int row, int col) {
			// ShapefileLoad.getInstance().refreshCreateTable();
		}

	} //

}
