/*
 * ManageSpatialIndex.java
 *
 * Created on 08.04.2010, 18:13:46
 */

package org.GeoRaptor.OracleSpatial.CreateSpatialIndex;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Position;

import oracle.ide.dialogs.ProgressBar;

import oracle.jdbc.OracleConnection;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.ErrorDialogHandler;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;

import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 *
 * @author laebe
 * @author Simon Greener, April 27th 2010 Moved getMsg handling out to separate
 *         PropertiesManager class
 *         
 *         Comment out 1467 lines
 **/

public class ManageSpatialIndex extends javax.swing.JDialog 
{

	private static final long serialVersionUID = -2509004245331206103L;

	private static final Logger LOGGER = Logging
			.getLogger("org.GeoRaptor.OracleSpatial.CreateSpatialIndex.ManageSpatialIndex");

	/**
	 * We have only one instance of this class
	 */
	private static ManageSpatialIndex classInstance;

	/**
	 * Reference to resource manager for accesing messages in properties file
	 */
	private static final String propertiesFile = "org.GeoRaptor.OracleSpatial.CreateSpatialIndex.resource.ManageSpatialIndex";
	protected PropertiesManager propertyManager = null;
	protected ErrorDialogHandler errorDialogHandler = null;

	/**
	 * Database connection
	 */
	protected String connName;

	/**
	 * Spatial Index Preferences
	 */
	private Preferences indexPreferences;
	protected boolean indexSuffixFlag;
	protected String indexAffixValue;

	/**
	 * Name of: 1. connecting user 1. schema owning object 2. objectName containing
	 * index 3. geoColumn actual SDO_GEOMETRY column
	 */
	protected String userName;
	protected String schemaName;
	protected String objectName;
	protected String geoColumn;

	private static final String no_tablespace = "<<NONE>>";

	private String indexDimsParameter = "sdo_indx_dims=";
	private String geodeticParameter = "geodetic=";
	private String nonLeafParameter = "sdo_non_leaf_tbl=";
	private String layerGTypeParameter = "layer_gtype=";
	private String indexTablespaceParameter = "tablespace=";
	private String workTablespaceParameter = "work_tablespace=";
	private String dmlBatchSizeParameter = "sdo_dml_batch_size=";
	private final int dmlBatchSizeDefault = 1000;
	private String sSliderPercentPrefix = "Layer Sample (";
	private String sSldrBatchSizePrefix = "DML Batch Size (";
	private String sSldrParallelPrefix = "Parallelism (";

	private ManageSpatialIndex() {
		this(null, "", false);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	/** Creates new form ManageSpatialIndex */
	private ManageSpatialIndex(java.awt.Frame parent, String title, boolean modal) {
		super(parent, title, modal);

		try {
			// Get localisation file
			//
			this.propertyManager = new PropertiesManager(ManageSpatialIndex.propertiesFile);
			this.errorDialogHandler = new ErrorDialogHandler(ManageSpatialIndex.propertiesFile);
		} catch (Exception e) {
			System.out.println(
					"Problem loading properties file: " + ManageSpatialIndex.propertiesFile + "\n" + e.getMessage());
		}

		// Initialise Menu
		//
		initComponents();

		this.txtSQL.setWrapStyleWord(true);
		DefaultCaret caret = (DefaultCaret) this.txtSQL.getCaret();
		caret.setDot(0, Position.Bias.Forward);
		caret.setUpdatePolicy(DefaultCaret.UPDATE_WHEN_ON_EDT);
		this.cmbIndexTablespaces
				.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { ManageSpatialIndex.no_tablespace }));
		this.cmbWorkTablespaces
				.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { ManageSpatialIndex.no_tablespace }));

		// Blank out lblMessage label (dots useful for design)
		//
		this.lblMessage.setText("");
	}

	public static ManageSpatialIndex getInstance() {
		if (ManageSpatialIndex.classInstance == null) {
			ManageSpatialIndex.classInstance = new ManageSpatialIndex();
		}
		return ManageSpatialIndex.classInstance;
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnClose;
	private javax.swing.JButton btnCopySqlToClipboard;
	private javax.swing.JButton btnCreateIndex;
	private javax.swing.JButton btnHelp;
	private javax.swing.JCheckBox cbNonLeaf;
	private javax.swing.JCheckBox chkEditSQL;
	private javax.swing.JCheckBox chkIsGeodeticIndex;
	private javax.swing.JCheckBox chkLayerGTypeApply;
	private javax.swing.JComboBox<String> cmbColumnName;
	private javax.swing.JComboBox<String> cmbDimension;
	private javax.swing.JComboBox<String> cmbIndexTablespaces;
	private javax.swing.JComboBox<String> cmbLayerGType;
	private javax.swing.JComboBox<String> cmbWorkTablespaces;
	private javax.swing.JButton discoverLayerGTypeBTN;
	private javax.swing.JLabel lblBatchSize;
	private javax.swing.JLabel lblColumnName;
	private javax.swing.JLabel lblDimension;
	private javax.swing.JLabel lblIndexName;
	private javax.swing.JLabel lblIndexTablespace;
	private javax.swing.JLabel lblLayerType;
	private javax.swing.JLabel lblMessage;
	private javax.swing.JLabel lblParallel;
	private javax.swing.JLabel lblParallelismAuto;
	private javax.swing.JLabel lblTablename;
	private javax.swing.JLabel lblWorkTablespace;
	private javax.swing.JLabel llblSamplePct;
	private javax.swing.JPanel panelButtonMessage;
	private javax.swing.JPanel panelLayerGtype;
	private javax.swing.JPanel panelMain;
	private javax.swing.JPanel panelParameter;
	private javax.swing.JPanel panelSQL;
	private javax.swing.JPanel panelTablespace;
	private javax.swing.JPanel pnlIndexObject;
	private javax.swing.JPanel pnlMiscellaneous;
	private javax.swing.JPanel pnlParallelism;
	private javax.swing.JScrollPane scrpSQL;
	private javax.swing.JSlider sldrBatchSize;
	private javax.swing.JSlider sldrParallel;
	private javax.swing.JSlider sldrSample;
	private javax.swing.JTextField txtIndexName;
	private javax.swing.JTextArea txtSQL;
	private javax.swing.JTextField txtTablename;
	// End of variables declaration//GEN-END:variables

	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		panelMain = new javax.swing.JPanel();
		panelParameter = new javax.swing.JPanel();
		panelLayerGtype = new javax.swing.JPanel();
		discoverLayerGTypeBTN = new javax.swing.JButton();
		cmbLayerGType = new javax.swing.JComboBox<String>();
		lblLayerType = new javax.swing.JLabel();
		chkLayerGTypeApply = new javax.swing.JCheckBox();
		llblSamplePct = new javax.swing.JLabel();
		sldrSample = new javax.swing.JSlider();
		panelTablespace = new javax.swing.JPanel();
		cmbIndexTablespaces = new javax.swing.JComboBox<String>();
		lblIndexTablespace = new javax.swing.JLabel();
		cmbWorkTablespaces = new javax.swing.JComboBox<String>();
		lblWorkTablespace = new javax.swing.JLabel();
		pnlMiscellaneous = new javax.swing.JPanel();
		chkIsGeodeticIndex = new javax.swing.JCheckBox();
		cmbDimension = new javax.swing.JComboBox<String>();
		cbNonLeaf = new javax.swing.JCheckBox();
		lblDimension = new javax.swing.JLabel();
		sldrBatchSize = new javax.swing.JSlider();
		lblBatchSize = new javax.swing.JLabel();
		panelSQL = new javax.swing.JPanel();
		scrpSQL = new javax.swing.JScrollPane();
		txtSQL = new javax.swing.JTextArea();
		chkEditSQL = new javax.swing.JCheckBox();
		btnCreateIndex = new javax.swing.JButton();
		btnCopySqlToClipboard = new javax.swing.JButton();
		panelButtonMessage = new javax.swing.JPanel();
		lblMessage = new javax.swing.JLabel();
		btnClose = new javax.swing.JButton();
		btnHelp = new javax.swing.JButton();
		pnlParallelism = new javax.swing.JPanel();
		lblParallel = new javax.swing.JLabel();
		lblParallelismAuto = new javax.swing.JLabel();
		sldrParallel = new javax.swing.JSlider();
		pnlIndexObject = new javax.swing.JPanel();
		lblTablename = new javax.swing.JLabel();
		txtIndexName = new javax.swing.JTextField();
		lblIndexName = new javax.swing.JLabel();
		lblColumnName = new javax.swing.JLabel();
		cmbColumnName = new javax.swing.JComboBox<String>();
		txtTablename = new javax.swing.JTextField();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("GeoRaptor Create Index");
		setLocationByPlatform(true);
		setMaximumSize(null);
		setName("indexDialog"); // NOI18N
		setPreferredSize(new java.awt.Dimension(680, 660));
		getContentPane().setLayout(new java.awt.BorderLayout(5, 5));

		panelMain.setMaximumSize(null);
		panelMain.setMinimumSize(new java.awt.Dimension(0, 0));
		panelMain.setPreferredSize(new java.awt.Dimension(650, 630));

		panelParameter.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));
		panelParameter.setMaximumSize(null);
		panelParameter.setPreferredSize(new java.awt.Dimension(632, 301));

		panelLayerGtype.setBorder(javax.swing.BorderFactory.createTitledBorder("Layer GType"));
		panelLayerGtype.setMaximumSize(null);
		panelLayerGtype.setPreferredSize(new java.awt.Dimension(588, 102));

		discoverLayerGTypeBTN.setText("Discover");
		discoverLayerGTypeBTN.setMaximumSize(null);
		discoverLayerGTypeBTN.setMinimumSize(null);
		discoverLayerGTypeBTN.setName(""); // NOI18N
		discoverLayerGTypeBTN.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				discoverLayerGTypeBTNActionPerformed(evt);
			}
		});

		cmbLayerGType.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "<<Select from List/Discover>>",
				"COLLECTION", "MULTILINE", "LINE", "MULTIPOINT", "POINT", "MULTIPOLYGON", "POLYGON" }));
		cmbLayerGType.setMaximumSize(null);
		cmbLayerGType.setMinimumSize(null);
		cmbLayerGType.setPreferredSize(new java.awt.Dimension(204, 20));
		cmbLayerGType.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				cmbLayerGTypeItemStateChanged(evt);
			}
		});
		cmbLayerGType.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cmbLayerGTypeActionPerformed(evt);
			}
		});

		lblLayerType.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblLayerType.setText("Gtype:");
		lblLayerType.setMaximumSize(null);
		lblLayerType.setMinimumSize(null);
		lblLayerType.setPreferredSize(new java.awt.Dimension(46, 14));

		chkLayerGTypeApply.setText("Apply");
		chkLayerGTypeApply.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		chkLayerGTypeApply.setMaximumSize(null);
		chkLayerGTypeApply.setMinimumSize(null);
		chkLayerGTypeApply.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				chkLayerGTypeApplyStateChanged(evt);
			}
		});

		llblSamplePct.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		llblSamplePct.setText("Sample (100%)");
		llblSamplePct.setMaximumSize(null);
		llblSamplePct.setMinimumSize(null);
		llblSamplePct.setPreferredSize(new java.awt.Dimension(106, 27));

		sldrSample.setMajorTickSpacing(10);
		sldrSample.setMinorTickSpacing(5);
		sldrSample.setPaintLabels(true);
		sldrSample.setPaintTicks(true);
		sldrSample.setSnapToTicks(true);
		sldrSample.setToolTipText("Sets number of rows sampled to discover layer_gtype");
		sldrSample.setValue(100);
		sldrSample.setMaximumSize(null);
		sldrSample.setMinimumSize(null);
		sldrSample.setPreferredSize(new java.awt.Dimension(214, 35));
		sldrSample.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				sldrSampleStateChanged(evt);
			}
		});

		javax.swing.GroupLayout panelLayerGtypeLayout = new javax.swing.GroupLayout(panelLayerGtype);
		panelLayerGtype.setLayout(panelLayerGtypeLayout);
		panelLayerGtypeLayout.setHorizontalGroup(panelLayerGtypeLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelLayerGtypeLayout.createSequentialGroup().addGroup(panelLayerGtypeLayout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(panelLayerGtypeLayout.createSequentialGroup().addGap(52, 52, 52).addComponent(
								lblLayerType, javax.swing.GroupLayout.PREFERRED_SIZE, 62,
								javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
								panelLayerGtypeLayout.createSequentialGroup().addContainerGap().addComponent(
										llblSamplePct, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(panelLayerGtypeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(panelLayerGtypeLayout.createSequentialGroup().addGap(9, 9, 9)
										.addComponent(cmbLayerGType, javax.swing.GroupLayout.PREFERRED_SIZE, 193,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(discoverLayerGTypeBTN, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(chkLayerGTypeApply, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))
								.addComponent(sldrSample, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		panelLayerGtypeLayout.setVerticalGroup(panelLayerGtypeLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelLayerGtypeLayout.createSequentialGroup()
						.addGroup(panelLayerGtypeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(cmbLayerGType, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblLayerType, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(discoverLayerGTypeBTN, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(chkLayerGTypeApply, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(panelLayerGtypeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(llblSamplePct, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(sldrSample, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		panelTablespace.setBorder(javax.swing.BorderFactory.createTitledBorder("Tablespace"));
		panelTablespace.setMaximumSize(null);
		panelTablespace.setPreferredSize(new java.awt.Dimension(588, 47));

		cmbIndexTablespaces
				.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "ATableSpaceNameA32CharactersLong" }));
		cmbIndexTablespaces.setMaximumSize(null);
		cmbIndexTablespaces.setMinimumSize(null);
		cmbIndexTablespaces.setPreferredSize(new java.awt.Dimension(212, 20));
		cmbIndexTablespaces.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cmbIndexTablespacesActionPerformed(evt);
			}
		});

		lblIndexTablespace.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblIndexTablespace.setText("Index:");
		lblIndexTablespace.setMaximumSize(null);
		lblIndexTablespace.setMinimumSize(null);
		lblIndexTablespace.setPreferredSize(new java.awt.Dimension(46, 14));

		cmbWorkTablespaces
				.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "ATableSpaceNameA32CharactersLong" }));
		cmbWorkTablespaces.setMaximumSize(null);
		cmbWorkTablespaces.setMinimumSize(null);
		cmbWorkTablespaces.setPreferredSize(new java.awt.Dimension(220, 20));
		cmbWorkTablespaces.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cmbWorkTablespacesActionPerformed(evt);
			}
		});

		lblWorkTablespace.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblWorkTablespace.setText("Work:");
		lblWorkTablespace.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		lblWorkTablespace.setMaximumSize(null);
		lblWorkTablespace.setMinimumSize(null);
		lblWorkTablespace.setPreferredSize(new java.awt.Dimension(46, 14));

		javax.swing.GroupLayout panelTablespaceLayout = new javax.swing.GroupLayout(panelTablespace);
		panelTablespace.setLayout(panelTablespaceLayout);
		panelTablespaceLayout
				.setHorizontalGroup(panelTablespaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(panelTablespaceLayout.createSequentialGroup().addContainerGap()
								.addComponent(lblIndexTablespace, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(cmbIndexTablespaces, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(14, 14, 14)
								.addComponent(lblWorkTablespace, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(cmbWorkTablespaces, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		panelTablespaceLayout
				.setVerticalGroup(panelTablespaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(panelTablespaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(cmbWorkTablespaces, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblWorkTablespace, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(cmbIndexTablespaces, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblIndexTablespace, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));

		pnlMiscellaneous.setBorder(javax.swing.BorderFactory.createTitledBorder("Miscellaneous"));
		pnlMiscellaneous.setMaximumSize(null);
		pnlMiscellaneous.setPreferredSize(new java.awt.Dimension(588, 102));

		chkIsGeodeticIndex.setText("Geodetic:");
		chkIsGeodeticIndex.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		chkIsGeodeticIndex.setMaximumSize(null);
		chkIsGeodeticIndex.setMinimumSize(null);
		chkIsGeodeticIndex.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				chkIsGeodeticIndexStateChanged(evt);
			}
		});

		cmbDimension.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "2", "3", "4" }));
		cmbDimension.setLightWeightPopupEnabled(false);
		cmbDimension.setMaximumSize(null);
		cmbDimension.setMinimumSize(null);
		cmbDimension.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cmbDimensionActionPerformed(evt);
			}
		});

		cbNonLeaf.setText("Non Leaf");
		cbNonLeaf.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		cbNonLeaf.setMaximumSize(null);
		cbNonLeaf.setMinimumSize(null);
		cbNonLeaf.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cbNonLeafActionPerformed(evt);
			}
		});

		lblDimension.setText("Dimension:");
		lblDimension.setMaximumSize(null);
		lblDimension.setMinimumSize(null);

		sldrBatchSize.setMajorTickSpacing(1000);
		sldrBatchSize.setMaximum(10000);
		sldrBatchSize.setMinimum(1000);
		sldrBatchSize.setMinorTickSpacing(100);
		sldrBatchSize.setPaintLabels(true);
		sldrBatchSize.setPaintTicks(true);
		sldrBatchSize.setSnapToTicks(true);
		sldrBatchSize.setPreferredSize(new java.awt.Dimension(323, 31));
		sldrBatchSize.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				sldrBatchSizeStateChanged(evt);
			}
		});

		lblBatchSize.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblBatchSize.setLabelFor(sldrBatchSize);
		lblBatchSize.setText("DML Batch Size ( 1,000):");
		lblBatchSize.setFocusable(false);
		lblBatchSize.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		lblBatchSize.setMaximumSize(null);
		lblBatchSize.setMinimumSize(null);
		lblBatchSize.setPreferredSize(new java.awt.Dimension(142, 14));

		javax.swing.GroupLayout pnlMiscellaneousLayout = new javax.swing.GroupLayout(pnlMiscellaneous);
		pnlMiscellaneous.setLayout(pnlMiscellaneousLayout);
		pnlMiscellaneousLayout.setHorizontalGroup(pnlMiscellaneousLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlMiscellaneousLayout.createSequentialGroup().addContainerGap()
						.addGroup(pnlMiscellaneousLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(cbNonLeaf, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGroup(pnlMiscellaneousLayout
										.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addGroup(pnlMiscellaneousLayout.createSequentialGroup()
												.addComponent(lblDimension, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(cmbDimension, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE))
										.addComponent(chkIsGeodeticIndex, javax.swing.GroupLayout.Alignment.TRAILING,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(lblBatchSize, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(sldrBatchSize,
								javax.swing.GroupLayout.PREFERRED_SIZE, 309, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap(20, Short.MAX_VALUE)));
		pnlMiscellaneousLayout.setVerticalGroup(pnlMiscellaneousLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlMiscellaneousLayout.createSequentialGroup().addGroup(pnlMiscellaneousLayout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(pnlMiscellaneousLayout.createSequentialGroup()
								.addComponent(sldrBatchSize, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(0, 0, Short.MAX_VALUE))
						.addGroup(pnlMiscellaneousLayout.createSequentialGroup().addGroup(pnlMiscellaneousLayout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(lblDimension, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(cmbDimension, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblBatchSize, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(chkIsGeodeticIndex, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(cbNonLeaf, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addContainerGap()));

		javax.swing.GroupLayout panelParameterLayout = new javax.swing.GroupLayout(panelParameter);
		panelParameter.setLayout(panelParameterLayout);
		panelParameterLayout.setHorizontalGroup(panelParameterLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelParameterLayout.createSequentialGroup().addContainerGap()
						.addGroup(panelParameterLayout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
								.addComponent(panelTablespace, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(panelLayerGtype, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(pnlMiscellaneous, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addContainerGap()));
		panelParameterLayout
				.setVerticalGroup(
						panelParameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(panelParameterLayout.createSequentialGroup()
										.addComponent(panelTablespace, javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(panelLayerGtype, javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(pnlMiscellaneous, javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addContainerGap()));

		panelSQL.setBorder(javax.swing.BorderFactory.createTitledBorder("SQL"));
		panelSQL.setAutoscrolls(true);
		panelSQL.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		panelSQL.setMaximumSize(null);
		panelSQL.setPreferredSize(new java.awt.Dimension(632, 139));
		panelSQL.setVerifyInputWhenFocusTarget(false);

		scrpSQL.setMaximumSize(null);
		scrpSQL.setMinimumSize(null);
		scrpSQL.setPreferredSize(new java.awt.Dimension(600, 84));

		txtSQL.setColumns(80);
		txtSQL.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
		txtSQL.setRows(40);
		txtSQL.setTabSize(4);
		txtSQL.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
		txtSQL.setDisabledTextColor(new java.awt.Color(204, 204, 204));
		txtSQL.setEnabled(false);
		txtSQL.setMaximumSize(null);
		txtSQL.setMinimumSize(null);
		scrpSQL.setViewportView(txtSQL);

		chkEditSQL.setText("Edit");
		chkEditSQL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		chkEditSQL.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		chkEditSQL.setMaximumSize(null);
		chkEditSQL.setMinimumSize(null);
		chkEditSQL.setPreferredSize(new java.awt.Dimension(50, 23));
		chkEditSQL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				chkEditSQLStateChanged(evt);
			}
		});

		btnCreateIndex.setText("Create Index");
		btnCreateIndex.setMaximumSize(null);
		btnCreateIndex.setMinimumSize(null);
		btnCreateIndex.setPreferredSize(new java.awt.Dimension(98, 23));
		btnCreateIndex.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnCreateIndexActionPerformed(evt);
			}
		});

		btnCopySqlToClipboard.setText("Copy SQL to Clipboard");
		btnCopySqlToClipboard.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnCopySqlToClipboardActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout panelSQLLayout = new javax.swing.GroupLayout(panelSQL);
		panelSQL.setLayout(panelSQLLayout);
		panelSQLLayout.setHorizontalGroup(panelSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelSQLLayout.createSequentialGroup().addContainerGap().addGroup(panelSQLLayout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(panelSQLLayout.createSequentialGroup()
								.addComponent(btnCreateIndex, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(btnCopySqlToClipboard)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(chkEditSQL, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addComponent(scrpSQL, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addContainerGap()));
		panelSQLLayout.setVerticalGroup(panelSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelSQLLayout.createSequentialGroup()
						.addComponent(scrpSQL, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(panelSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(chkEditSQL, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnCreateIndex, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnCopySqlToClipboard))));

		panelButtonMessage.setMaximumSize(null);
		panelButtonMessage.setPreferredSize(new java.awt.Dimension(632, 44));
		panelButtonMessage.setRequestFocusEnabled(false);

		lblMessage.setText("....");
		lblMessage.setMaximumSize(new java.awt.Dimension(478, 14));
		lblMessage.setMinimumSize(null);
		lblMessage.setPreferredSize(new java.awt.Dimension(478, 14));

		btnClose.setText("Close");
		btnClose.setMaximumSize(new java.awt.Dimension(61, 22));
		btnClose.setMinimumSize(null);
		btnClose.setPreferredSize(new java.awt.Dimension(61, 22));
		btnClose.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnCloseActionPerformed(evt);
			}
		});

		btnHelp.setText("Help");
		btnHelp.setMaximumSize(new java.awt.Dimension(61, 22));
		btnHelp.setMinimumSize(null);
		btnHelp.setPreferredSize(new java.awt.Dimension(61, 22));
		btnHelp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnHelpActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout panelButtonMessageLayout = new javax.swing.GroupLayout(panelButtonMessage);
		panelButtonMessage.setLayout(panelButtonMessageLayout);
		panelButtonMessageLayout.setHorizontalGroup(
				panelButtonMessageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(panelButtonMessageLayout.createSequentialGroup()
								.addComponent(btnHelp, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(lblMessage, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(4, 4, 4)));
		panelButtonMessageLayout.setVerticalGroup(panelButtonMessageLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelButtonMessageLayout.createSequentialGroup()
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(panelButtonMessageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
								.addComponent(lblMessage, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnHelp, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap()));

		pnlParallelism.setBorder(javax.swing.BorderFactory.createTitledBorder("Parallelism"));
		pnlParallelism.setMaximumSize(null);

		lblParallel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblParallel.setText("Parallelism ( 0):");
		lblParallel.setMaximumSize(null);
		lblParallel.setMinimumSize(null);
		lblParallel.setPreferredSize(new java.awt.Dimension(85, 14));

		lblParallelismAuto.setFont(new java.awt.Font("Tahoma", 2, 10)); // NOI18N
		lblParallelismAuto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		lblParallelismAuto.setText("(-1 = Automatic)");
		lblParallelismAuto.setMaximumSize(new java.awt.Dimension(83, 13));
		lblParallelismAuto.setMinimumSize(new java.awt.Dimension(83, 13));
		lblParallelismAuto.setPreferredSize(new java.awt.Dimension(83, 13));

		sldrParallel.setMajorTickSpacing(4);
		sldrParallel.setMaximum(32);
		sldrParallel.setMinimum(-1);
		sldrParallel.setMinorTickSpacing(1);
		sldrParallel.setPaintLabels(true);
		sldrParallel.setPaintTicks(true);
		sldrParallel.setSnapToTicks(true);
		sldrParallel.setValue(0);
		sldrParallel.setMaximumSize(null);
		sldrParallel.setMinimumSize(null);
		sldrParallel.setPreferredSize(new java.awt.Dimension(180, 45));
		sldrParallel.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				sldrParallelStateChanged(evt);
			}
		});

		javax.swing.GroupLayout pnlParallelismLayout = new javax.swing.GroupLayout(pnlParallelism);
		pnlParallelism.setLayout(pnlParallelismLayout);
		pnlParallelismLayout.setHorizontalGroup(pnlParallelismLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlParallelismLayout.createSequentialGroup().addGap(5, 5, 5)
						.addGroup(pnlParallelismLayout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
								.addComponent(lblParallelismAuto, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(lblParallel, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(sldrParallel, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap()));
		pnlParallelismLayout.setVerticalGroup(pnlParallelismLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlParallelismLayout.createSequentialGroup().addContainerGap().addGroup(pnlParallelismLayout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(sldrParallel, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGroup(pnlParallelismLayout.createSequentialGroup()
								.addComponent(lblParallel, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(lblParallelismAuto, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addGap(30, 30, 30)));

		pnlIndexObject.setBorder(javax.swing.BorderFactory.createTitledBorder("Index Object Properties"));
		pnlIndexObject.setMaximumSize(null);

		lblTablename.setLabelFor(txtTablename);
		lblTablename.setText("Object name:");

		txtIndexName.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent evt) {
				txtIndexNameFocusLost(evt);
			}
		});

		lblIndexName.setText("Index name:");

		lblColumnName.setText("Column name:");

		cmbColumnName.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cmbColumnNameActionPerformed(evt);
			}
		});

		txtTablename.setEnabled(false);

		javax.swing.GroupLayout pnlIndexObjectLayout = new javax.swing.GroupLayout(pnlIndexObject);
		pnlIndexObject.setLayout(pnlIndexObjectLayout);
		pnlIndexObjectLayout.setHorizontalGroup(pnlIndexObjectLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlIndexObjectLayout.createSequentialGroup().addContainerGap()
						.addGroup(pnlIndexObjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(lblIndexName, javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(lblTablename, javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(lblColumnName, javax.swing.GroupLayout.Alignment.TRAILING))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(pnlIndexObjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(txtIndexName).addComponent(txtTablename).addComponent(cmbColumnName,
										javax.swing.GroupLayout.PREFERRED_SIZE, 211,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap()));
		pnlIndexObjectLayout.setVerticalGroup(pnlIndexObjectLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(pnlIndexObjectLayout.createSequentialGroup()
						.addGroup(pnlIndexObjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(txtIndexName, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblIndexName))
						.addGap(9, 9, 9)
						.addGroup(pnlIndexObjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(lblTablename).addComponent(txtTablename,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(pnlIndexObjectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(cmbColumnName, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblColumnName))
						.addContainerGap()));

		javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
		panelMain.setLayout(panelMainLayout);
		panelMainLayout.setHorizontalGroup(panelMainLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelMainLayout.createSequentialGroup().addGap(15, 15, 15)
						.addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
								.addComponent(panelButtonMessage, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addGroup(javax.swing.GroupLayout.Alignment.LEADING,
										panelMainLayout.createSequentialGroup()
												.addComponent(pnlIndexObject, javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addComponent(pnlParallelism, javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
								.addComponent(panelSQL, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(panelParameter, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addGap(15, 15, 15)));
		panelMainLayout.setVerticalGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelMainLayout.createSequentialGroup()
						.addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(pnlParallelism, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(pnlIndexObject, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(panelParameter, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(panelSQL, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
						.addComponent(panelButtonMessage, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addContainerGap()));

		panelSQL.getAccessibleContext().setAccessibleParent(panelMain);

		getContentPane().add(panelMain, java.awt.BorderLayout.PAGE_START);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void chkEditSQLStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_chkEditSQLStateChanged
		// Enable/Disable actual editing
		this.txtSQL.setEditable(this.chkEditSQL.isSelected());
		this.txtSQL.setEnabled(this.chkEditSQL.isSelected());
	}// GEN-LAST:event_chkEditSQLStateChanged

	private void cmbColumnNameActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmbColumnNameActionPerformed
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_cmbColumnNameActionPerformed

	private void cmbLayerGTypeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmbLayerGTypeActionPerformed
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_cmbLayerGTypeActionPerformed

	private void cmbDimensionActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmbDimensionActionPerformed
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_cmbDimensionActionPerformed

	private void btnCreateIndexActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCreateIndexActionPerformed
		this.lblMessage.setText("Creating index ....");
		final String result = executeCreateIndex();
		// progress.waitUntilDone();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				lblMessage.setText(result);
			}
		});
	}// GEN-LAST:event_btnCreateIndexActionPerformed

	private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCloseActionPerformed
		this.setVisible(false);
	}// GEN-LAST:event_btnCloseActionPerformed

	private void discoverLayerGTypeBTNActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_discoverLayerGTypeBTNActionPerformed
		String layerGType = null;
		OracleConnection conn = DatabaseConnections.getInstance().getConnection(this.connName);
		if (conn != null) {
			try {
				layerGType = MetadataTool.getLayerGeometryType(conn, this.schemaName, this.objectName, this.geoColumn,
						this.sldrSample.getValue(), 0);
			} catch (Exception e) {
				this.lblMessage.setText(e.getMessage());
				layerGType = null;
			}
			if (layerGType == null) {
				return;
			}
			if (this.cmbLayerGType.getComponentCount() != 0) {
				this.cmbLayerGType.setSelectedItem(layerGType);
			} else {
				this.cmbLayerGType.insertItemAt(layerGType, 0);
			}
			this.chkLayerGTypeApply.setSelected(true);
		}
	}// GEN-LAST:event_discoverLayerGTypeBTNActionPerformed

	private void chkIsGeodeticIndexStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_chkIsGeodeticIndexStateChanged
		// rebuild SQL with changed parameters clause
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_chkIsGeodeticIndexStateChanged

	private void chkLayerGTypeApplyStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_chkLayerGTypeApplyStateChanged
		// rebuild SQL with changed parameters clause
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_chkLayerGTypeApplyStateChanged

	private void sldrSampleStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_sldrSampleStateChanged
		this.llblSamplePct.setText(this.sSliderPercentPrefix + String.format("%3d", this.sldrSample.getValue()) + "%)");
	}// GEN-LAST:event_sldrSampleStateChanged

	private void cmbLayerGTypeItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_cmbLayerGTypeItemStateChanged
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_cmbLayerGTypeItemStateChanged

	private void cmbIndexTablespacesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmbIndexTablespacesActionPerformed
		// rebuild SQL with changed parameters clause
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_cmbIndexTablespacesActionPerformed

	private void cmbWorkTablespacesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmbWorkTablespacesActionPerformed
		// rebuild SQL with changed parameters clause
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_cmbWorkTablespacesActionPerformed

	private void txtIndexNameFocusLost(java.awt.event.FocusEvent evt) {// GEN-FIRST:event_txtIndexNameFocusLost
		// rebuild SQL with changed index name
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_txtIndexNameFocusLost

	private void cbNonLeafActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbNonLeafActionPerformed
		// rebuild SQL with changed parameters clause
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_cbNonLeafActionPerformed

	private void sldrBatchSizeStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_sldrBatchSizeStateChanged
		this.lblBatchSize
				.setText(this.sSldrBatchSizePrefix + String.format("%,6d", this.sldrBatchSize.getValue()) + ")");
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_sldrBatchSizeStateChanged

	private void sldrParallelStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_sldrParallelStateChanged
		this.lblParallel.setText(this.sSldrParallelPrefix + String.format("%2d", this.sldrParallel.getValue()) + ")");
		this.txtSQL.setText(initialSQL() + parameters());
	}// GEN-LAST:event_sldrParallelStateChanged

	private void btnHelpActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnHelpActionPerformed
		JOptionPane.showMessageDialog(this, "Not Yet Implemented");
	}// GEN-LAST:event_btnHelpActionPerformed

	private void btnCopySqlToClipboardActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCopySqlToClipboardActionPerformed
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection ss = new StringSelection(this.txtSQL.getText());
		clipboard.setContents(ss, ss);
		Toolkit.getDefaultToolkit().beep();
	}// GEN-LAST:event_btnCopySqlToClipboardActionPerformed

	/**
	 * @function setInit
	 * @param _conn
	 * @param _schemaName
	 * @param _objectName
	 * @param _columnName
	 * @return
	 * @author Simon Greener 14th April 2010 Fixed handling of table/column
	 *         depending on which context menu New constants used from
	 *         ManageSpatialIndex.properties.
	 */
	public boolean setInit(String _connectionName, String _schemaName, String _objectName, String _columnName,
			String _userName) {
		if (Strings.isEmpty(_connectionName)) {
			LOGGER.error("No connection name was supplied.");
			return false;
		}

		this.connName = _connectionName;
		this.userName = _userName;
		this.schemaName = _schemaName;
		this.objectName = _objectName;
		this.geoColumn = _columnName;

		String spatialIndexName = "";
		OracleConnection conn = null;
		try {
			// Get connection from DatabaseConnections pool
			//
			conn = DatabaseConnections.getInstance().getConnection(this.connName);
			if (conn == null || DatabaseConnections.getInstance().isConnectionOpen(this.connName) == false) {
				LOGGER.error("An open connection with name (" + this.connName + ") was not found.");
				return false;
			}

			// Get preference information that relates to spatial indexing
			//
			this.indexPreferences = MainSettings.getInstance().getPreferences();
			this.indexSuffixFlag = this.indexPreferences.isSuffixFlag();
			this.indexAffixValue = this.indexPreferences.getAffixString();

			this.llblSamplePct.setText(this.sSliderPercentPrefix + this.sldrSample.getValue() + "%)");
			this.cmbColumnName.removeAllItems();
			if (Strings.isEmpty(this.geoColumn)) {
				// Get all Geometry Columns for this schema.table
				try {
					List<String> geoColumns;
					geoColumns = MetadataTool.getGeoColumns(conn, this.schemaName, this.objectName);
					// If empty, it will be handled after the try...catch
					if (geoColumns.size() != 0) {
						for (int i = 0; i < geoColumns.size(); i++)
							this.cmbColumnName.addItem(geoColumns.get(i));
					}
				} catch (SQLException sqle) {
					if (this.isVisible())
						this.setAlwaysOnTop(false);
					JOptionPane.showMessageDialog(null,
							this.propertyManager.getMsg("ERROR_MESSAGE_NOT_GEOMETRY_COLUMN",
									Strings.append(_schemaName, _objectName, Constants.TABLE_COLUMN_SEPARATOR)),
							MainSettings.EXTENSION_NAME, JOptionPane.INFORMATION_MESSAGE);
					if (this.isVisible())
						this.setAlwaysOnTop(true);
					return false;
				} catch (IllegalArgumentException iae) {
					if (this.isVisible())
						this.setAlwaysOnTop(false);
					JOptionPane
							.showMessageDialog(null,
									this.propertyManager.getMsg("OBJECT_RETRIEVAL_ERROR", iae.getLocalizedMessage(),
											"OBJECT_GEOMETRY_COLUMNS"),
									MainSettings.EXTENSION_NAME, JOptionPane.ERROR_MESSAGE);
					if (this.isVisible())
						this.setAlwaysOnTop(true);
					return false;
				}
			} else { // Check is supplied column is sdo_geometry
				try {
					this.cmbColumnName.addItem(
							MetadataTool.getGeometryColumn(conn, this.schemaName, this.objectName, this.geoColumn));
				} catch (SQLException sqle) {
					if (this.isVisible())
						this.setAlwaysOnTop(false);
					JOptionPane.showMessageDialog(null,
							this.propertyManager.getMsg("ERROR_MESSAGE_NOT_GEOMETRY_COLUMN", _columnName),
							MainSettings.EXTENSION_NAME, JOptionPane.INFORMATION_MESSAGE);
					if (this.isVisible())
						this.setAlwaysOnTop(true);
					return false;
				} catch (IllegalArgumentException iae) {
					if (this.isVisible())
						this.setAlwaysOnTop(false);
					JOptionPane
							.showMessageDialog(null,
									this.propertyManager.getMsg("OBJECT_RETRIEVAL_ERROR", iae.getLocalizedMessage(),
											"OBJECT_GEOMETRY_COLUMN"),
									MainSettings.EXTENSION_NAME, JOptionPane.ERROR_MESSAGE);
					if (this.isVisible())
						this.setAlwaysOnTop(true);
					return false;
				}
			}

			if (this.cmbColumnName.getItemCount() == 0)
				throw new IllegalArgumentException(this.propertyManager.getMsg("ERROR_MESSAGE_NO_TABLE_GEOMETRY_COLUMN",
						Strings.append(this.schemaName, this.objectName, Constants.TABLE_COLUMN_SEPARATOR)));
			this.cmbColumnName.setSelectedIndex(0);
			this.geoColumn = this.cmbColumnName.getItemAt(0).toString();

			// @author Simon Greener April 17th 2010
			// Existing index check to see if a geometry index already exists
			spatialIndexName = getIndexName(conn, this.schemaName, this.objectName, this.geoColumn);
			if (!Strings.isEmpty(spatialIndexName)) {
				// but if failed we can kill it
				if (spatialIndexName.contains("FAILED")) {
					dropIndex(conn, this.schemaName, this.objectName, this.geoColumn, this.userName, false);
				} else {
					this.errorDialogHandler.showErrorDialog(this, "ERROR_MESSAGE_GEO_INDEX_EXISTS",
							spatialIndexName /* Note includes status */,
							Strings.objectString(this.schemaName, this.objectName, this.geoColumn));
				}
				return false;
			}

			// Can't build index if no metadata entry, so check
			//
			try {
				if (!MetadataTool.hasGeomMetadataEntry(conn, this.schemaName, this.objectName, this.geoColumn)) {
					this.errorDialogHandler.showErrorDialog(this, "ERROR_MESSAGE_NO_RECORD_MD",
							Strings.objectString(_schemaName, _objectName, _columnName));
					if (this.isVisible())
						this.setAlwaysOnTop(true);
					return false;
				}
			} catch (Exception e) { /* SQLException and IllegalArgumentException caught as one */
				this.errorDialogHandler.showErrorDialog(this, "OBJECT_RETRIEVAL_ERROR", e.getLocalizedMessage(),
						this.propertyManager.getMsg("OBJECT_SDO_GEOM_METADATA") + "("
								+ Strings.objectString(_schemaName, _objectName, _columnName) + ")");
				return false;
			}

			try {
				// Build list of tablespaces
				List<String> tableSpaces;
				tableSpaces = MetadataTool.getTablespaces(conn);
				// If empty, it will be handled after the try...catch
				if (tableSpaces.size() != 0) {
					for (int i = 0; i < tableSpaces.size(); i++) {
						this.cmbWorkTablespaces.addItem(tableSpaces.get(i));
						this.cmbIndexTablespaces.addItem(tableSpaces.get(i));
					}
				}
			} catch (IllegalArgumentException iae) {
				// Display message and move on.
				this.errorDialogHandler.showErrorDialog(this, "OBJECT_RETRIEVAL_ERROR", iae.getLocalizedMessage(),
						"OBJECT_TABLESPACES");
			}

			// Create index name
			//
			spatialIndexName = buildIndexName(this.objectName, this.geoColumn);

			// Set value for use in SQL builder
			this.txtIndexName.setText(spatialIndexName);

			// build the new SQL statement
			this.txtSQL.setText(initialSQL() + parameters());

			// Refresh values in dialog
			this.txtTablename
					.setText(Strings.append(this.schemaName, this.objectName, Constants.TABLE_COLUMN_SEPARATOR));
			// refresh value in dialog
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// enable / disable screen components
					chkEditSQL.setSelected(false);
					txtIndexName.setEnabled(true);
					txtSQL.setEnabled(false);
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// do not show first instance of dialog in upper left corner.
		if ((this.getBounds().x == 0) && (this.getBounds().y == 0)) {
			this.setLocation(300, 300);
		}

		return true;
	}

	private String buildIndexName(String _objectName, String _columnName) {
		String spatialIndexName = "";

		// Should have a parameter for controlling adding of column name
		// Construct basic name using object.column
		//
		spatialIndexName = Strings.append(_objectName, _columnName, "_");

		// Check size of name
		//
		if (spatialIndexName.length() + this.indexAffixValue.length() > 30)
			spatialIndexName = Strings.append(Strings.condense(_objectName), _columnName, "_");

		// Add Affix?
		//
		if (!Strings.isEmpty(this.indexAffixValue))
			spatialIndexName = this.indexSuffixFlag ? spatialIndexName + this.indexAffixValue
					: this.indexAffixValue + spatialIndexName;

		return spatialIndexName;
	}

	/** Non-forms based code */

	/**
	 * @function getIndexName
	 * @param _schemaName
	 * @param _tableName
	 * @param _columnName
	 * @return String - AI.INDEX_NAME(AI.DOMIDX_OPSTATUS)
	 * @author Simon Greener, April 20th 2010 Refactored to be more commoditised If
	 *         _columnName is null then gets index associated with first
	 */
	private String getIndexName(Connection _conn, String _schemaName, String _tableName, String _columnName) {
		OracleConnection conn = DatabaseConnections.getInstance().getConnection(this.connName);
		String indexName = "";
		String columnClause = (Strings.isEmpty(_columnName) ? ")"
				: "and aic.COLUMN_NAME = '" + _columnName.toUpperCase() + "' ) \n");
		String sql = "Select ai.index_name || '(' || AI.DOMIDX_OPSTATUS || ')' \n" + "  from all_indexes ai \n"
				+ " where ai.owner      = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n"
				+ "   and ai.table_name = ? \n" + "   and ai.Ityp_Name  = 'SPATIAL_INDEX'\n"
				+ "   and exists (select 1 \n" + "                 from all_ind_columns aic \n"
				+ "                where aic.index_owner = ai.owner \n"
				+ "                  and aic.table_name  = ai.table_name \n" + columnClause + "   and rownum < 2";
		try {
			PreparedStatement psIndicies = (_conn != null) ? _conn.prepareStatement(sql) : (conn.prepareStatement(sql));
			psIndicies.setString(1, Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase());
			psIndicies.setString(2, _tableName);
			if (columnClause.contains("?"))
				psIndicies.setString(3, _columnName);
			ResultSet indicesRSet = psIndicies.executeQuery();
			if (indicesRSet.next()) {
				indexName = indicesRSet.getString(1);
				indicesRSet.close();
				psIndicies.close();
			}
			indicesRSet.close();
			indicesRSet = null;
			psIndicies = null;
		} catch (Exception ex) {
			this.errorDialogHandler.showErrorDialog(this, "ERROR_MESSAGE_RETRIEVING_INDEX_NAME",
					Strings.objectString(_schemaName, _tableName, _columnName) + "\n" + ex.toString());
			return null;
		}
		return indexName;
	}

	// Builds PARAMETERS clause from scratch
	private String parameters() {
		String paramClause = "\nPARAMETERS('";
		paramClause += this.indexDimsParameter + this.cmbDimension.getSelectedItem().toString();
		if (this.chkLayerGTypeApply.isSelected()) {
			if (this.cmbLayerGType.getItemCount() > 0
					&& !this.cmbLayerGType.getSelectedItem().toString().contains("<<"))
				paramClause += ", " + this.layerGTypeParameter + this.cmbLayerGType.getSelectedItem().toString();
		}
		// SDO_NON_LEAF clause
		//
		if (this.cbNonLeaf.isSelected()) {
			paramClause += ", " + this.nonLeafParameter + this.cbNonLeaf.isSelected();
		}
		// Geodetic clause
		//
		if (this.chkIsGeodeticIndex.isSelected()) {
			paramClause += ", " + this.geodeticParameter + this.chkIsGeodeticIndex.isSelected();
		}
		// Index Tablespace parameter
		//
		if (!this.cmbIndexTablespaces.getSelectedItem().toString().equals(ManageSpatialIndex.no_tablespace)) {
			paramClause += ", " + this.indexTablespaceParameter + this.cmbIndexTablespaces.getSelectedItem().toString();
		}
		// Work Tablespace parameter
		//
		if (!this.cmbWorkTablespaces.getSelectedItem().toString().equals(ManageSpatialIndex.no_tablespace)) {
			paramClause += ", " + this.workTablespaceParameter + this.cmbWorkTablespaces.getSelectedItem().toString();
		}
		// DML Batch size
		if (this.sldrBatchSize.getValue() != this.dmlBatchSizeDefault) {
			paramClause += ", " + this.dmlBatchSizeParameter + this.sldrBatchSize.getValue();
		}
		return paramClause + "')";
	}

	/**
	 * @method setSQL
	 * @precis For external classes, like ShapefileLoad to create spatial index with
	 *         ProgressBar etc.
	 * @param _sql
	 * @author Simon Greener, 28th May 2010 - Original coding
	 */
	public void setSQL(String _sql) {
		this.txtSQL.setText(_sql);
	}

	/**
	 * @method setIndexName
	 * @precis For external classes, like ShapefileLoad to create spatial index with
	 *         ProgressBar etc.
	 * @param _name
	 * @author Simon Greener, 28th May 2010 - Original coding
	 */
	public void setIndexName(String _name) {
		String indexName = _name.length() > 30 ? Strings.condense(_name) : _name;
		this.txtIndexName.setText(indexName);
	}

	/**
	 * @method getIndexName
	 * @precis For external classes, like ShapefileLoad to create spatial index with
	 *         ProgressBar etc.
	 * @return String (Modified name)
	 * @author Simon Greener, 28th May 2010 - Original coding
	 */
	public String getIndexName() {
		return this.txtIndexName.getText();
	}

	/**
	 * @method setConnection
	 * @precis For external classes, like ShapefileLoad to create spatial index with
	 *         ProgressBar etc.
	 * @param _connName
	 * @author Simon Greener, 28th May 2010 - Original coding
	 */
	public void setConnection(String _connName) {
		this.connName = Strings.isEmpty(_connName) ? this.connName : _connName;
	}

	private String initialSQL() {
		String parallelClause = "";
		switch (this.sldrParallel.getValue()) {
		case -1:
			parallelClause = "\n   PARALLEL";
			break;
		case 0:
			parallelClause = "";
			break;
		default:
			parallelClause = "\n   PARALLEL " + String.valueOf(this.sldrParallel.getValue());
			break;
		}
		String indexName = this.txtIndexName.getText().trim();
		indexName = indexName.length() > 30 ? Strings.condense(indexName) : indexName;
		return "CREATE INDEX " + Strings.append(this.schemaName, indexName, Constants.TABLE_COLUMN_SEPARATOR)
				+ "\n          ON " + Strings.append(this.schemaName, this.objectName, Constants.TABLE_COLUMN_SEPARATOR)
				+ "(" + this.geoColumn + ")\n" + "   INDEXTYPE IS MDSYS.SPATIAL_INDEX " + parallelClause;
	}

	protected void createInitSQL(String _sql) {
		final String sql = _sql;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				txtSQL.setText(sql);
			}
		});
	}

	/**
	 * @function executeCreateIndex
	 * @author Simon Greener April 17th 2010 Added to handle SQL exceptions and thus
	 *         give meaningful error messages to user
	 * @author Simon Greener May 28th 2010 - Made public - Fixed success dialog box
	 *         showing wrong index name
	 **/
	public String executeCreateIndex() {
		// use inner class to do work in a thread that ProgressBar can use
		class executeIndexSQL implements Runnable {
			protected Connection conn;
			protected ProgressBar progressBar;
			protected String errorMessage;
			protected String SQLStatement;

			public executeIndexSQL(Connection _conn, String _sqlStmt) throws SQLException {
				this.conn = _conn;
				this.SQLStatement = _sqlStmt;
			}

			public void setProgressBar(ProgressBar _progressBar) {
				this.progressBar = _progressBar;
			}

			public void run() {
				try {
					Statement st;
					st = this.conn.createStatement();
					st.executeUpdate(this.SQLStatement);
				} catch (SQLException sqle) {
					this.errorMessage = String.valueOf(sqle.getMessage());
				}
				// "shut down" progressbar
				this.progressBar.setDoneStatus();
			}
		}

		String sComplete = "";
		try {
			OracleConnection conn = DatabaseConnections.getInstance().getConnection(this.connName);
			if (conn == null || DatabaseConnections.getInstance().isConnectionOpen(this.connName) == false) {
				return this.propertyManager.getMsg("ERROR_MESSAGE_CONN_UNAVAILABLE", this.connName);
			}
			executeIndexSQL eis = new executeIndexSQL(conn, this.txtSQL.getText());
			ProgressBar progress = new ProgressBar(this, "Building Spatial Index.", eis, true);
			progress.setCancelable(true);
			eis.setProgressBar(progress);
			progress.start("Working, please wait...", null);
			sComplete = this.propertyManager.getMsg("CREATE_SUCCESFUL",
					Strings.isEmpty(this.txtIndexName.getText()) ? "" : this.txtIndexName.getText());
			if (!Strings.isEmpty(eis.errorMessage)) {
				Toolkit.getDefaultToolkit().beep();
				if (this.isVisible())
					this.setAlwaysOnTop(false);
				this.errorDialogHandler.showErrorDialog(this, "ERROR_MESSAGE_CREATE_FAILED_WHY", eis.errorMessage);
				if (this.isVisible())
					this.setAlwaysOnTop(true);
				sComplete = this.propertyManager.getMsg("ERROR_MESSAGE_CREATE_FAILED");
				// Drop failed index
				//
				try {
					String spatialIndexName = getIndexName(conn, this.schemaName, this.objectName, this.geoColumn);
					if (!Strings.isEmpty(spatialIndexName)) {
						spatialIndexName = spatialIndexName.substring(0, spatialIndexName.indexOf("("));
						String dropSQL = "DROP INDEX "
								+ Strings.append(this.schemaName, spatialIndexName, Constants.TABLE_COLUMN_SEPARATOR);
						Statement st;
						st = conn.createStatement();
						st.executeUpdate(dropSQL);
					}
				} catch (SQLException se) {
					// Ignore...
				}
			}
			progress = null;
		} catch (SQLException sqle) {
			Toolkit.getDefaultToolkit().beep();
			sComplete = this.propertyManager.getMsg("ERROR_MESSAGE_CREATE_FAILED", sqle.getMessage());
		}
		return sComplete;
	}

	/**
	 * @function dropIndex
	 * @author Simon Greener April 20th 2010
	 **/
	public boolean dropIndex(Connection _conn, String _schemaName, String _tableName, String _columnName,
			String _userName, boolean _displayResult) {
		this.userName = _userName;
		this.geoColumn = _columnName;

		String getColumnValue = _columnName;
		String dropSQL = "";
		try {
			// Test what we have been given
			// Should get back same as what we supply ... except if NULL
			//
//			getColumnValue = MetadataTool.getGeometryColumn(_conn, _schemaName, _tableName, _columnName);
			if (Strings.isEmpty(getColumnValue)) {
				// There is no geometry column in this table
				//
				this.errorDialogHandler.showErrorDialog(this, "ERROR_MESSAGE_NO_TABLE_GEOMETRY_COLUMN",
						Strings.objectString(_schemaName, _tableName, _columnName));
				return false;
			} else {
				// If same as passed in columnName we are OK
				//
				if ((!Strings.isEmpty(_columnName)) && (!_columnName.equalsIgnoreCase(getColumnValue))) {
					// Passed in column is not of type SDO_GEOMETRY?
					this.errorDialogHandler.showErrorDialog(this, "ERROR_MESSAGE_NOT_GEOMETRY_COLUMN",
							Strings.objectString(_schemaName, _tableName, _columnName));
					return false;
				}
			}

			// geoColumn is not null and will contain the right SDO_GEOMETRY column
			String spatialIndexName = getIndexName(_conn, _schemaName, _tableName, this.geoColumn);
			if (Strings.isEmpty(spatialIndexName)) {
				this.errorDialogHandler.showErrorDialog(this, "NOTHING_TO_DROP",
						Strings.objectString(_schemaName, _tableName, this.geoColumn));
				return false;
			}

			spatialIndexName = spatialIndexName.substring(0, spatialIndexName.indexOf("("));

			Object[] options = { this.propertyManager.getMsg("BUTTON_DROP_INDEX"),
					this.propertyManager.getMsg("BUTTON_CANCEL") };
			if (this.isVisible())
				this.setAlwaysOnTop(false);
			int n = JOptionPane.showOptionDialog(this, this.propertyManager.getMsg("DROP_INDEX_TEXT", spatialIndexName),
					MainSettings.EXTENSION_NAME + " - DROP INDEX", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if (this.isVisible())
				this.setAlwaysOnTop(true);
			if (n == 0) {
				dropSQL = "DROP INDEX "
						+ (Strings.isEmpty(_schemaName) ? spatialIndexName
								: Strings.append(_schemaName, spatialIndexName, Constants.TABLE_COLUMN_SEPARATOR))
						+ " FORCE";
				Statement st;
				st = _conn.createStatement();
				st.executeUpdate(dropSQL);
				if (_displayResult) {
					if (this.isVisible())
						this.setAlwaysOnTop(false);
					JOptionPane.showMessageDialog(null,
							this.propertyManager.getMsg("DROP_SUCCESSFUL", spatialIndexName),
							MainSettings.EXTENSION_NAME, JOptionPane.INFORMATION_MESSAGE);
					this.setVisible(false);
				}
			} else
				return false;
		} catch (SQLException sqle) {
			this.errorDialogHandler.showErrorDialog(this, "ERROR_MESSAGE_DOMAIN_INDEX", dropSQL, sqle.getMessage());
			return false;
		}
		return true;
	}

}
