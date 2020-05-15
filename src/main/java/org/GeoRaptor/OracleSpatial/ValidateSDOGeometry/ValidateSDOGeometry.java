package org.GeoRaptor.OracleSpatial.ValidateSDOGeometry;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.text.DateFormat;
import java.text.NumberFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleTypes;

import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;

import oracle.spatial.geometry.ElementExtractor;
import oracle.spatial.geometry.J3D_Geometry;
import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.ControlerSV;
import org.GeoRaptor.SpatialView.JDevInt.DockableSV;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SpatialViewPanel;
import org.GeoRaptor.SpatialView.SupportClasses.LineStyle;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.layers.SVGraphicLayer;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayer;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayerProps;
import org.GeoRaptor.SpatialView.layers.Styling;
import org.GeoRaptor.sql.DatabaseConnection;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.ErrorDialogHandler;
import org.GeoRaptor.tools.MathUtils;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.TableSortIndicator;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;


/**
 * Table manu point - validate values in selected geometry column
 *
 * @author Matic Petek
 * @version 1.0
 * @version 1.1
 * @author Simon Greener, April 4th 2010
 *          Added support for:
 *              - DimInfo based processing.
 *              - multiple schemas/owners
 *              - multiple geometry columns per object
 *              - user defined primary or unique keys not just ROWID.
 * @author Simon Greener, June 22nd 2010
 *          Added support for:
 *              - Visualising sdo_geometry in JTable
 *              - Showing selection of geometries in spatial view
 *              - Showing where error occurred.
 * @author Simon Greener, June 28th 2010
 *          Support for applying RECTIFY_GEOMETRY, REMOVE_DUPLICATE_VERTICES, SDO_MIGRATE.TO_CURRENT in validation pipeline
 *          Improved dialog by application of titled borders etc.
 *          Added copy to clipboard for SELECT and UPDATE statements
 */
@SuppressWarnings("deprecation")
public class ValidateSDOGeometry extends JDialog implements Observer 
{
	private static final long serialVersionUID = -8079134652561357147L;

	private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.ValidateSDOGeometry");
    
    protected Preferences geoRaptorPreferences;

    protected PropertiesManager  propertyManager    = null;
    protected ErrorDialogHandler errorDialogHandler = null;
    private static final String     propertiesFile  = "org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.Validation";
    protected PropertiesManager         helpManager = null;
    private static final String   errorCodeHelpFile = "org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.ErrorCodes";

    public static final String primaryKeyColumnName = "gid";
    public static final String        contextColumn = "context";
    private             String       geomColumnName = Constants.VAL_GEOMETRY_COLUMN_NAME;
    
    protected RenderTool renderTool;

    protected enum CLIPBOARD_TYPES { GEOMETRY, ELEMENT_INFO, ERROR_TEXT };

    protected LinkedHashMap<String, MetadataEntry> 
                                      metadata = null;
    protected String            connectionName = null;
    protected String                schemaName = "";
    protected String                objectName = "";
    protected String                   userSQL = "";
    protected int                    totalRows = -1;
    protected int                    validRows = -1;
    protected int                     nullRows = -1;
    protected DateFormat             outFormat = null;
    protected ValidationThread     validThread = null;
    protected ValidationTableModel     vtModel = null;
    protected ListSelectionModel         vtLSM = null;
    
    private JButton       btnValidationActions = new JButton();
    protected JTable               resultTable = null;
    private JPopupMenu               popupMenu = new JPopupMenu();
    private JTextField                  tfSRID = new JTextField();
    private JLabel                     lblSRID = new JLabel();
    private JButton         btnLayerProperties = new JButton();
    
    // Sort icons indicators for header
    private TableSortIndicator  fSortIndicator = null;
    private Icon ascIcon = UIManager.getIcon("Table.ascendingSortIcon"),
                descIcon = UIManager.getIcon("Table.descendingSortIcon");
    // Sorter objects
    private TableRowSorter<ValidationTableModel> sorter;
    private List<RowSorter.SortKey>            sortKeys;

    protected SpatialView       validationView = null; // Pointer to one of the below
    protected SVGraphicLayer   validationLayer = null;
    private SpatialViewPanel               svp = null;
    private SVSpatialLayer  currentActiveLayer = null;
    private SpatialView      currentActiveView = null;
    private SpatialView           existingView = null;
    private SpatialView                newView = null;
    private Envelope        usedViewMBR = null;
    private JTextField                tfFilter = new JTextField();
    private JLabel                   lblFilter = new JLabel();
    private boolean                  nullGeoms = false;
    private boolean                     twoDee = false;

    /**
     * Constructor
     **/
    public ValidateSDOGeometry() 
    {
        try {
            // Get localisation file
            //
               this.propertyManager = new PropertiesManager(ValidateSDOGeometry.propertiesFile);
            this.errorDialogHandler = new ErrorDialogHandler(ValidateSDOGeometry.propertiesFile);
                   this.helpManager = new PropertiesManager(ValidateSDOGeometry.errorCodeHelpFile);
                    this.renderTool = new RenderTool();
            this.connectionName = DatabaseConnections.getActiveConnectionName();
        } catch (Exception e) {
            System.out.println("Problem loading properties file: " + ValidateSDOGeometry.propertiesFile + "\n" + e.getMessage());
        }
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        setPropertyStrings();
        this.svp = DockableSV.getSpatialViewPanel();
        this.vtModel = new ValidationTableModel(  );
        this.vtModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                 return;
            }
        });
        this.resultTable.setModel(this.vtModel);
        
        TableColumnModel tcm = this.resultTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(130);
        tcm.getColumn(0).setResizable(true);
        tcm.getColumn(1).setPreferredWidth(50);
        tcm.getColumn(1).setResizable(true);
        tcm.getColumn(2).setPreferredWidth(300);
        tcm.getColumn(2).setResizable(true);
        tcm.getColumn(3).setPreferredWidth(4000);
        tcm.getColumn(3).setResizable(true);

        this.resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        DefaultTableCellRenderer headerRenderer = ((DefaultTableCellRenderer)this.resultTable.getTableHeader().getDefaultRenderer());
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        this.vtLSM = this.resultTable.getSelectionModel();
        this.resultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                resultTableMouseClicked(evt);
            }
        });
        this.resultTable.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) 
                {
                    bulkDeleteErrors();
                }
            }
            public void keyTyped(KeyEvent e) { }
            public void keyReleased(KeyEvent e) { }
        });
        this.fSortIndicator = new TableSortIndicator(this.resultTable, ascIcon, descIcon);
        this.fSortIndicator.addObserver(this);
        
        // Create actual sorter to hold sortKeys and filters
        //
        sorter = new TableRowSorter<ValidationTableModel>(this.vtModel);
       
        // Create a sort key per column
        //
        sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(ValidationTableModel.errorColumn,  SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(ValidationTableModel.resultColumn, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(ValidationTableModel.idColumn,     SortOrder.ASCENDING));
        
        // Add keys to sorter
        //
        sorter.setSortKeys(sortKeys);

        // Add row sorter to table
        //
        this.resultTable.setRowSorter(sorter);

        // Whenever tfFilter changes, invoke applyFilter().
        //
        tfFilter.addKeyListener(new KeyListener() {
              public void keyPressed(KeyEvent e) {}
              public void keyTyped(KeyEvent e) { }
              public void keyReleased(KeyEvent e) { setMessage(propertyManager.getMsg("MESSAGE_FILTERING_WAIT")); } 
          });

        createRightMouseClickMenu();
        
        this.geomColumnName = geoRaptorPreferences.getDefGeomColName();
        this.validationProgressBar.setStringPainted(true);
        this.outFormat = DateFormat.getTimeInstance(DateFormat.LONG,Locale.getDefault());        
    }

    private void tfFilter_focusLost(FocusEvent e) {
        this.applyFilter();
        this.setMessage(propertyManager.getMsg("MESSAGE_FILTERED"));
    }
    
    // When the user clicks a column header, fSortIndicator will notify 
    // registered observers, who will call getSortBy to fetch the new sort.
    // But we don't really use this in GeoRaptor.
    //  
    public void update(Observable aObservable, Object aData) { }

    /**
     * @function applyFilter()
     * @author Simon Greener
     * @precis applies typed in regex to table via sorter 
     * Can get this to work with an set of orFilters applied to sorter.setRowFilter()
     */
    private void applyFilter() 
    {
        if (Strings.isEmpty(tfFilter.getText())) {
            sorter.setRowFilter(null);
            return;
        } else if ( tfFilter.getText().length() > 2 ) { // Just to speed things up a bit.... Errors are all 13xxx so start filtering at the first x!
            String regex = tfFilter.getText().contains("(?i)") ? tfFilter.getText() : "(?i)" + tfFilter.getText();
            // If current expression doesn't parse, don't update.
            try 
            {
                RowFilter<ValidationTableModel, Object> rf = null;
                rf = RowFilter.regexFilter(regex,
                                           ValidationTableModel.idColumn,
                                           ValidationTableModel.errorColumn,
                                           ValidationTableModel.resultColumn);  // Apply to only searchable columns
                sorter.setRowFilter(rf);
            } catch (PatternSyntaxException e) {
                sorter.setRowFilter(null);
                return;
            }
        }
    }
    
    protected String DIALOG_TITLE                 = "Validate SDO_GEOMETRY";
    protected String TOOLTIP_ZOOM_ERRORS          = "Extracts and zooms to the position of the selected errors eg [Coordinate <3>]";
    protected String TOOLTIP_MAP_ERRORS           = "Extracts and maps the position of the selected errors eg [Coordinate <3>]";
    protected String TOOLTIP_ZOOM_FEATURES        = "Zoom to, and display, the actual geometry with the error";
    protected String TOOLTIP_MAP_FEATURES         = "Display the actual geometry with the error";
    protected String BUTTON_SHOW_SQL              = "Create Validation SQL";
    protected String BUTTON_SHOW_UPDATE_SQL       = "Create Update SQL";
    protected String BUTTON_RUN_VALIDATION        = "Run Validation";
    protected String BUTTON_CANCEL_VALIDATION     = "Cancel";
    protected String BUTTON_EXIT                  = "Exit";
    protected String BUTTON_CLOSE                 = "Close";
    protected String CHECK_BOX_DimInfo_VALIDATION = "Use DimInfo Tolerance:";
    protected String ERROR_NO_WORK_TO_DO          = "Select an update action eg Remove_Duplicate_Vertices before proceeding.";
    protected String LABEL_TOLERANCE              = "Tolerance:";
    protected String LABEL_ROWS_WITH_ERRORS       = "Rows with errors:";
    protected String LABEL_KEY_COLUMN             = "Key column(s):";
    protected String LABEL_GEOMETRY_COLUMN        = "Geometry column:";
    protected String PROGRESS_START               = "Press 'Run Validation' to start the validation";
    protected String TITLE_BORDER_METADATA        = "Metadata";
    protected String TITLE_BORDER_OPTIONS         = "Options";
    protected String TITLE_BORDER_PROCESSING      = "Processing";
    protected String TITLE_BORDER_RESULTS         = "Results (Right-click for options)";
    protected String ERROR_TOLERANCE_VALUE        = "Tolerance must be double value like 0.005";
    protected String CHECK_BOX_VALID_GEOMETRIES   = "Valid Sdo_Geometies";
    protected String ERROR_NO_SELECTION           = "No row(s) selected.";
    protected String ERROR_ONE_SELECTION          = "Only one row can be selected.";
    protected String NO_ERROR_FOUND               = "";
    protected String LABEL_FILTER                 = "Error Filter:";

    private void setPropertyStrings() {
        this.DIALOG_TITLE                 = this.propertyManager.getMsg("DIALOG_TITLE");
        this.TOOLTIP_ZOOM_ERRORS          = this.propertyManager.getMsg("TOOLTIP_ZOOM_ERRORS");
        this.TOOLTIP_ZOOM_FEATURES        = this.propertyManager.getMsg("TOOLTIP_ZOOM_FEATURES");
        this.TOOLTIP_MAP_ERRORS           = this.propertyManager.getMsg("TOOLTIP_MAP_ERRORS");
        this.TOOLTIP_MAP_FEATURES         = this.propertyManager.getMsg("TOOLTIP_MAP_FEATURES");
        this.BUTTON_SHOW_SQL              = this.propertyManager.getMsg("BUTTON_SHOW_SQL");
        this.BUTTON_SHOW_UPDATE_SQL       = this.propertyManager.getMsg("BUTTON_SHOW_UPDATE_SQL");
        this.BUTTON_RUN_VALIDATION        = this.propertyManager.getMsg("BUTTON_RUN_VALIDATION");
        this.BUTTON_CANCEL_VALIDATION     = this.propertyManager.getMsg("BUTTON_CANCEL_VALIDATION");
        this.BUTTON_EXIT                  = this.propertyManager.getMsg("BUTTON_EXIT");
        this.BUTTON_CLOSE                 = this.propertyManager.getMsg("BUTTON_CLOSE");
        this.CHECK_BOX_DimInfo_VALIDATION = this.propertyManager.getMsg("CHECK_BOX_DimInfo_VALIDATION");
        this.ERROR_NO_WORK_TO_DO          = this.propertyManager.getMsg("ERROR_NO_WORK_TO_DO");
        this.LABEL_TOLERANCE              = this.propertyManager.getMsg("LABEL_TOLERANCE");
        this.LABEL_ROWS_WITH_ERRORS       = this.propertyManager.getMsg("LABEL_ROWS_WITH_ERRORS");
        this.LABEL_KEY_COLUMN             = this.propertyManager.getMsg("LABEL_KEY_COLUMN");
        this.LABEL_GEOMETRY_COLUMN        = this.propertyManager.getMsg("LABEL_GEOMETRY_COLUMN");
        this.PROGRESS_START               = this.propertyManager.getMsg("PROGRESS_START","'"+this.BUTTON_RUN_VALIDATION+"'");
        this.TITLE_BORDER_METADATA        = this.propertyManager.getMsg("TITLE_BORDER_METADATA");
        this.TITLE_BORDER_OPTIONS         = this.propertyManager.getMsg("TITLE_BORDER_OPTIONS");
        this.TITLE_BORDER_PROCESSING      = this.propertyManager.getMsg("TITLE_BORDER_PROCESSING");
        this.TITLE_BORDER_RESULTS         = this.propertyManager.getMsg("TITLE_BORDER_RESULTS");
        this.ERROR_TOLERANCE_VALUE        = this.propertyManager.getMsg("ERROR_TOLERANCE_VALUE");
        this.CHECK_BOX_VALID_GEOMETRIES   = this.propertyManager.getMsg("CHECK_BOX_VALID_GEOMETRIES");
        this.ERROR_NO_SELECTION           = this.propertyManager.getMsg("ERROR_NO_SELECTION");
        this.ERROR_ONE_SELECTION          = this.propertyManager.getMsg("ERROR_ONE_SELECTION");
        this.NO_ERROR_FOUND               = this.propertyManager.getMsg("NO_ERROR_FOUND");
        this.LABEL_FILTER                 = this.propertyManager.getMsg("LABEL_FILTER");
      
        this.setTitle(this.DIALOG_TITLE);
        lblKeyColumn.setText(this.LABEL_KEY_COLUMN);
        lblGeomColumn.setText(this.LABEL_GEOMETRY_COLUMN);
        btnCreateSQL.setText(this.BUTTON_SHOW_SQL);
        runValidationButton.setText(this.BUTTON_RUN_VALIDATION);
        exitButton.setText(this.BUTTON_EXIT);
        lblRowsWithErrors.setText(this.LABEL_ROWS_WITH_ERRORS);
        cbDimInfo.setText(this.CHECK_BOX_DimInfo_VALIDATION);
        pnlMetadata.setBorder(BorderFactory.createTitledBorder(this.TITLE_BORDER_METADATA));
        pnlProcessing.setBorder(BorderFactory.createTitledBorder(this.TITLE_BORDER_PROCESSING));
        spResultTable.setBorder(BorderFactory.createTitledBorder(this.TITLE_BORDER_RESULTS));
        
        lblMessages.setText("Ready");
        lblFilter.setText(this.LABEL_FILTER);
    }

    private void jbInit() throws Exception 
    {
        xyProcessingPanel = new XYLayout();
        borderLayout1 = new BorderLayout();
        pnlValidationForm = new JPanel();
        pnlMetadata = new JPanel();
        pnlProcessing = new JPanel();
        flMetadataPanel = new FlowLayout();
        lblKeyColumn = new JLabel();
        cmbKeyColumns = new JComboBox<>();
        lblGeomColumn = new JLabel();
        cmbGeoColumns = new JComboBox<>();
        lblTolerance = new JLabel();
        //toleranceTF = new JTextField();
        //toleranceTF = new JFormattedTextField(Tools.getNLSDecimalFormat(-1, false).getInstance());
	    toleranceTF = new JFormattedTextField(Tools.getDecimalFormatter());
        cbDimInfo = new JCheckBox();
        
        runValidationButton = new JButton();
        exitButton = new JButton();
    
        spResultTable = new JScrollPane();
        resultTable = new JTable();
        lblMessages = new JTextField();
        
        validationProgressBar = new JProgressBar();
        
        lblRowsWithErrors = new JLabel();
        lblErrorCount = new JLabel();
        
        btnCreateSQL = new JButton();
        
        boxLayout = new BoxLayout(pnlValidationForm, BoxLayout.PAGE_AXIS);

        this.setTitle(this.DIALOG_TITLE);
        this.setModalityType(Dialog.ModalityType.MODELESS);
        this.setLayout(borderLayout1);
        this.setSize(new Dimension(823, 484));
        this.setResizable(false);

        //************************************ First Panel (Metadata and View)

        flMetadataPanel.setAlignment(0);
        flMetadataPanel.setVgap(1);
        flMetadataPanel.setHgap(10);        
        pnlMetadata.setLayout(flMetadataPanel);
        pnlMetadata.setBorder(BorderFactory.createTitledBorder("Metadata"));
        pnlMetadata.setBounds(new Rectangle(0,0, 795, 50));
        pnlMetadata.setPreferredSize(new Dimension(821, 50));

        pnlMetadata.setSize(new Dimension(821, 50));
        pnlMetadata.setMinimumSize(new Dimension(821, 50));
        pnlMetadata.setMaximumSize(new Dimension(821, 50));
        lblKeyColumn.setText("Key column(s):");
        lblKeyColumn.setLabelFor(this.cmbKeyColumns);
        
        lblGeomColumn.setText("Geometry column:");
        lblGeomColumn.setLabelFor(this.cmbGeoColumns);

        cmbGeoColumns.setFont(new Font("Tahoma", 1, 11));
        cmbGeoColumns.setEditable(false);
        cmbGeoColumns.setEnabled(false);   // Until init() method called.
        cmbGeoColumns.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                if ( cmbGeoColumns.isEnabled() ) {
                    if (evt.getStateChange() == ItemEvent.SELECTED) {
                        // Item was just selected
                        updateChangedColumn();
                    } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                        // Item is no longer selected
                    }
                }
            }
        });
        
        lblTolerance.setText("Tolerance:");
        toleranceTF.setPreferredSize(new Dimension(70, 21));
        lblTolerance.setLabelFor(toleranceTF);

        cbDimInfo.setText("Use DimInfo Tolerance:");
        cbDimInfo.setSelected(false);
        cbDimInfo.setHorizontalTextPosition(SwingConstants.LEADING);
        cbDimInfo.setActionCommand("Use DimInfo:");
        cbDimInfo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                cbDimInfoItem_StateChanged(evt);
            }
        });

        pnlMetadata.add(lblKeyColumn, null);
        pnlMetadata.add(cmbKeyColumns, null);
        pnlMetadata.add(lblGeomColumn, null);
        pnlMetadata.add(cmbGeoColumns, null);
        pnlMetadata.add(lblTolerance, null);
        pnlMetadata.add(toleranceTF, null);
        pnlMetadata.add(cbDimInfo, null);

        //************************************ Next Panel (Show SQL)

        pnlMetadata.add(lblSRID, null);
        pnlMetadata.add(tfSRID, null);
        btnCreateSQL.setText("Create Validation SQL");
        btnCreateSQL.setActionCommand("");
        btnCreateSQL.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnCreateSQL_actionPerformed(e);
                }
            });
        btnValidationActions.setText("Create Update SQL");
        btnValidationActions.setEnabled(true);
        btnValidationActions.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnValidationActions_actionPerformed(e);
                }
            });

        //************************************ Next Panel (Processing)

        pnlProcessing.setLayout(xyProcessingPanel);
        pnlProcessing.setBorder(BorderFactory.createTitledBorder("Processing"));
        pnlProcessing.setBounds(new Rectangle(0, 160, 905, 500));
        pnlProcessing.setPreferredSize(new Dimension(812, 405));

        pnlProcessing.setMaximumSize(new Dimension(812, 405));
        pnlProcessing.setMinimumSize(new Dimension(812, 405));

        runValidationButton.setText("Run Validation");
        runValidationButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    runValidationButton_actionPerformed(e);
                }
            });

        exitButton.setText("Exit");
        exitButton.setMnemonic('E');
        exitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    exitButton_actionPerformed(e);
                }
            });

        validationProgressBar.setPreferredSize(new Dimension(570, 20));

        validationProgressBar.setMaximumSize(new Dimension(570, 20));
        validationProgressBar.setSize(new Dimension(570, 19));
        validationProgressBar.setMinimumSize(new Dimension(500, 20));
        lblRowsWithErrors.setText("Rows with errors:");
        lblRowsWithErrors.setPreferredSize(new Dimension(40, 22));
        lblRowsWithErrors.setHorizontalTextPosition(SwingConstants.RIGHT);

        lblErrorCount.setText("0");
        lblErrorCount.setFont(new Font("Dialog", 1, 11));
        lblErrorCount.setForeground(Color.red);
        lblErrorCount.setPreferredSize(new Dimension(30, 22));
        lblErrorCount.setHorizontalTextPosition(SwingConstants.CENTER);
        lblErrorCount.setHorizontalAlignment(SwingConstants.CENTER);

        lblMessages.setEnabled(true);
        lblMessages.setEditable(false);
        lblMessages.setForeground(Color.RED);
        lblMessages.setSize(new Dimension(795, 19));
        lblMessages.setPreferredSize(new Dimension(795, 19));
        lblMessages.setMinimumSize(new Dimension(795, 19));
        lblMessages.setMaximumSize(new Dimension(795, 19));
        lblFilter.setText("Error Filter:");
        lblFilter.setHorizontalAlignment(SwingConstants.TRAILING);
        lblFilter.setLabelFor(tfFilter);
        btnLayerProperties.setText("Layer Properties");
        btnLayerProperties.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnLayerProperties_actionPerformed(e);
                }
            });
        tfFilter.setPreferredSize(new Dimension(60, 20));
        tfFilter.setMaximumSize(new Dimension(60, 20));
        tfFilter.setMinimumSize(new Dimension(40, 20));
        tfFilter.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    tfFilter_focusLost(e);
                }
            });
        lblSRID.setText("SRID:");
        lblSRID.setLabelFor(tfSRID);
        tfSRID.setPreferredSize(new Dimension(50, 19));
        tfSRID.setSize(new Dimension(50, 19));
        tfSRID.setMinimumSize(new Dimension(50, 19));
        spResultTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        spResultTable.getViewport().add(this.resultTable, null);

        pnlProcessing.add(lblFilter, new XYConstraints(15, 336, 120, 15));
        pnlProcessing.add(tfFilter, new XYConstraints(140, 336, 60, 20));
        pnlProcessing.add(btnLayerProperties, new XYConstraints(680, -4, 120, 20));
        pnlProcessing.add(runValidationButton,
                          new XYConstraints(160, -4, 155, 20));
        pnlProcessing.add(exitButton, new XYConstraints(725, 361, 75, 20));
        pnlProcessing.add(validationProgressBar,
                          new XYConstraints(205, 336, 595, 20));
        pnlProcessing.add(lblErrorCount, new XYConstraints(90, 361, 45, 20));
        pnlProcessing.add(lblRowsWithErrors,
                          new XYConstraints(5, 360, 90, 22));
        pnlProcessing.add(spResultTable, new XYConstraints(5, 26, 795, 305));

        pnlProcessing.add(lblMessages, new XYConstraints(140, 361, 580, 20));
        pnlProcessing.add(btnCreateSQL, new XYConstraints(5, -4, 150, 20));
        pnlProcessing.add(btnValidationActions,
                          new XYConstraints(320, -4, 145, 20));
        boxLayout = new BoxLayout(pnlValidationForm, BoxLayout.PAGE_AXIS);
        pnlValidationForm.setLayout(boxLayout);
        pnlValidationForm.add(pnlMetadata, 0);
        pnlValidationForm.add(pnlProcessing, 1);
        this.getContentPane().add(pnlValidationForm, BorderLayout.CENTER);
        this.pack();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private BorderLayout borderLayout1;
    private JPanel pnlValidationForm;
    private JPanel pnlMetadata;
    private JPanel pnlProcessing;
    private FlowLayout flMetadataPanel;
    private BoxLayout boxLayout;
  
    private JLabel lblKeyColumn;
    private JComboBox<String> cmbKeyColumns;
    private JLabel lblGeomColumn;
    private JComboBox<String> cmbGeoColumns;
    
    private JLabel lblTolerance;
    private JTextField toleranceTF;
    private JCheckBox cbDimInfo;
  
    private JButton btnCreateSQL;

    private XYLayout xyProcessingPanel;
  
    private JButton runValidationButton;
    private JButton exitButton;
    
    private JProgressBar validationProgressBar;
    
    private JLabel lblRowsWithErrors;
    private JLabel lblErrorCount;
  
    private JScrollPane spResultTable;
    private JTextField lblMessages;
    
    // End of variables declaration//GEN-END:variables

    public void init(String _connName, 
                     String _schemaName,
                     String _objectName,
                     String _columnName) 
         throws SQLException, 
                IllegalArgumentException 
    {
        this.connectionName = _connName;
        this.schemaName     = _schemaName;
        this.objectName     = _objectName;

        // Check supplied geometry column (can be null)
        // Function returns first column if null or same column if exists
        //
        String geoColumn = _objectName;
        OracleConnection conn = DatabaseConnections.getInstance().getConnection(_connName);
        if ( conn!=null ) {
            if ( DatabaseConnections.getInstance().isConnectionOpen(_connName)) {
                geoColumn = MetadataTool.getGeometryColumn(conn,_schemaName,_objectName,_columnName);
                if (Strings.isEmpty(geoColumn) ) {
                    this.errorDialogHandler.showErrorDialog(this,
                                                         "ERROR_MESSAGE_NO_COLUMNS",
                                                            Strings.append(_schemaName,
                                                                      _objectName,
                                                                      Constants.TABLE_COLUMN_SEPARATOR));
                    return;            
                }
            }
        }

        // Set metadata for schema/table
        //
        setMetadata();

        // If nothing we can't do anything
        //
        if ( this.metadata == null || this.metadata.size() == 0) {
            this.errorDialogHandler.showErrorDialog(this,
                                                    "ERROR_MESSAGE_NO_METADATA", Strings.objectString(_schemaName,
                                                                           _objectName,
                                                                           geoColumn,
                                                                           Constants.TABLE_COLUMN_SEPARATOR));
            return;
        }

        // Set up cmbGeoColumns and
        // Copy data relating to current column cmbGeoColumns
        //
        if ( this.cmbGeoColumns.getItemCount() != 0 )
            this.cmbGeoColumns.removeAllItems();
        MetadataEntry mEntry = null;
        Iterator<String> keyIter = metadata.keySet().iterator();
        while (keyIter.hasNext()) {
            String key = (String)keyIter.next();
            mEntry = this.metadata.get(key);
            this.cmbGeoColumns.addItem(mEntry.getColumnName());
            if ( mEntry.getColumnName().equalsIgnoreCase(geoColumn) ) {
                // select this column
                this.cmbGeoColumns.setSelectedIndex(this.cmbGeoColumns.getItemCount()-1);
                // Set tolerance
                this.setTolerance(mEntry);
                // Get SRID
                this.setSRID(mEntry.getSRID());
            }
        }

        getPrimaryKeys();
        
        cbDimInfoItem_StateChanged(null);

        // delete old data
        this.vtModel.clear();
        
        // refresh button status         
        validationProgressBar.setString(PROGRESS_START);
        updateValidationButtonStatus();

        // Now we can activate the listener
        this.cmbGeoColumns.setEnabled(true);

        // Create Controlling Layer
        createDisplayLayer(geoColumn);

        // set dialog title with table name
        this.setTitle(this.DIALOG_TITLE + "[" + Strings.objectString(_schemaName,_objectName,geoColumn) + "]");
        //this.setLocationRelativeTo(super.getComponent(0));
        this.setVisible(true);
    }

    private void createDisplayLayer(String geoColumn) 
    {
        try {
            // Save current active view and layer
            //
            this.currentActiveView  = this.svp.getActiveView();
            this.currentActiveLayer = this.svp.getActiveLayer();  // Could be NULL ie no layers
            
            // There may be an existing view with the right SRID (other than the 
            // active one) we can use for display.
            //
            this.existingView = null;
            
            // Set up layer variables
            //
            String layerName = Strings.objectString(this.schemaName, this.objectName, geoColumn);
            String SRID      = this.metadata.get(layerName).getSRID();
            String viewName  = SpatialView.createViewName(SRID);
            
            // Get View with same SRID as table/geometry's SRID if exists
            //
            this.existingView = this.svp.getViewLayerTree().getView(viewName);
            if ( this.existingView == null ) {
                // OK we need to create a new one
                //
                this.newView = new SpatialView(this.svp,viewName,SRID);
                // Add view to layer tree
                //
                this.svp.getViewLayerTree().addView(this.newView,false);
            } 
            // Set single view to correct one
            this.validationView = this.newView!=null 
                                  ? this.newView 
                                  : ( this.existingView!=null 
                                      ? this.existingView 
                                      : this.currentActiveView);
            
            // Create layer
            //
            this.validationLayer = new SVGraphicLayer(this.validationView,
                                                      layerName,
                                                      layerName,
                                                      "Validation Layer",
                                                      this.metadata.get(layerName),
                                                      true);
            this.validationLayer.setConnection(this.connectionName);
            this.validationLayer.getStyling().setPointSize(8);
            this.validationLayer.getStyling().setPointType(PointMarker.MARKER_TYPES.STAR);
            this.validationLayer.getStyling().setPointColor(Colours.getRandomColor());
            this.validationLayer.getStyling().setLineColor(Colours.getRandomColor());
            this.validationLayer.getStyling().setMarkGeoPoints(PointMarker.MARKER_TYPES.CIRCLE);
            this.validationLayer.getStyling().setMarkGeoStart(PointMarker.MARKER_TYPES.ASTERISK);
            this.validationLayer.getStyling().setSegmentArrow(Constants.SEGMENT_ARROWS_TYPE.MIDDLE);
            this.validationLayer.getStyling().setMarkVertex(Constants.VERTEX_LABEL_TYPE.ID);
            this.validationLayer.getStyling().setMarkOriented(true);
            this.validationLayer.getStyling().setShadeColor(Colours.getRandomColor());
            this.validationLayer.getStyling().setShadeTransLevel(0.5f);
            this.validationLayer.getStyling().setShadeType(Styling.STYLING_TYPE.CONSTANT);
            this.validationLayer.getStyling().setSelectionPointSize(12);
            this.validationLayer.getStyling().setSelectionLineWidth(2);
            this.validationLayer.getStyling().setSelectionLineStroke(LineStyle.LINE_STROKES.LINE_DASHDOT);

            if ( this.newView != null ) {
                this.svp.setActiveView(this.newView);
                this.newView.addLayer(this.validationLayer,true /*isDraw*/,false/*active*/,false /*zoom*/);
            } else if ( this.existingView != null ) {
                this.svp.setActiveView(this.existingView);
                this.existingView.addLayer(this.validationLayer,true /*isDraw*/,false/*active*/,false /*zoom*/);
                // Save its current MBR
                this.usedViewMBR = new Envelope(this.existingView.getMBR());
            } else {
                this.currentActiveView.addLayer(this.validationLayer,true /*isDraw*/,false/*active*/,false /*zoom*/);
                // Save its current MBR
                this.usedViewMBR = new Envelope(this.currentActiveView.getMBR());
            }
            this.svp.setActiveLayer(this.validationLayer);
        } catch (Exception e) {
            LOGGER.warn("ValidateSDOGeometry.createDisplaylayer: Error (" + e.getMessage() + ")");
        }
    }

    private void updateChangedColumn() 
    {
        // get full object name using selected column name in combo box
        //
        String processingObject = Strings.objectString(this.getSchemaName(),this.getObjectName(),this.getSelectedColumnName());
        
        // Get maximum tolerance value associated with this column from this.metadata
        //
        double tol = this.metadata.get(processingObject).getMaxTolerance(Constants.DEFAULT_TOLERANCE);
        
        // Get formatter with the relevant database NLS setting applied
        // 
        // int digits = this.metadata.get(processingObject).getMaxFractionDigits(Constants.DEFAULT_PRECISION);
        //DecimalFormat df = Tools.getNLSDecimalFormat(digits==0?Constants.DEFAULT_PRECISION:digits, false);
        NumberFormat df = Tools.getDecimalFormatter();

        // Format the output
        //
        this.toleranceTF.setText(df.format(tol));
        
        // delete old data
        //
        this.vtModel.clear();
        
        // refresh button status 
        //
        validationProgressBar.setString(PROGRESS_START);
        updateValidationButtonStatus();
    }
    
    private void getPrimaryKeys() 
    {
        // Fill Primary key check box  
        // 
        this.cmbKeyColumns.removeAllItems();
        // Always add rowid as an option
        this.cmbKeyColumns.addItem("ROWID");
        // Now get actual constraints and columns
        //
        try 
        {
            List<String> keyColumns = MetadataTool.keysAndColumns(this.getConnection(),this.getSchemaName(),this.getObjectName());
            if (keyColumns.size() != 0) 
                for (int i = 0; i < keyColumns.size(); i++) {
                    this.cmbKeyColumns.addItem(keyColumns.get(i));
                }
        } catch (SQLException sqle ) {
            this.errorDialogHandler.showErrorDialog(this,
                                                 "ERROR_MESSAGE_CANDIDATE_KEYS",
                                                    Strings.append(this.getSchemaName(),
                                                              this.getObjectName(),
                                                              Constants.TABLE_COLUMN_SEPARATOR) + "\n" +
                                 sqle.getMessage().toString() + "\n" +
                                 "Will use ROWID.");
        } catch (IllegalArgumentException iae ) {
          this.errorDialogHandler.showErrorDialog(this,
                                               "ERROR_MESSAGE_CANDIDATE_KEYS",
                                                    Strings.append(this.getSchemaName(),
                                                            this.getObjectName(),
                                                            Constants.TABLE_COLUMN_SEPARATOR) + "\n" +
                               iae.getMessage().toString() + "\n" +
                               "Will use ROWID.");
        }
        // select first column
        //
        this.cmbKeyColumns.setSelectedIndex(0);
    }

    private void setMetadata()
    {
        try {
            this.metadata = MetadataTool.getMetadata(this.getConnection(),
                                                     this.getSchemaName(), 
                                                     this.getObjectName(), 
                                                     null,
                                                     false);
        } catch (SQLException sqle) {
            JOptionPane.showMessageDialog(null,
                                          "Error retrieving metadata for " +
                                          this.getFullObjectName(),
                                          sqle.getMessage().toString() + "\n" +
                                          MainSettings.EXTENSION_NAME, 
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnCreateSQL_actionPerformed(ActionEvent e) {
        lblMessages.setText("");
        if (Strings.isEmpty(this.userSQL) )
            this.userSQL = this.getInitialSQL();
        ValidationSelectQuery vqr = new ValidationSelectQuery(this,"",true,this);
        vqr.setSql(this.userSQL);
        vqr.setNullGeoms(this.nullGeoms);  // Actually set on creation of form
        vqr.setLocationRelativeTo(this);
        vqr.setAlwaysOnTop(true);
        vqr.setVisible(true);
        if ( ! vqr.wasCancelled() ) {
            this.userSQL = vqr.getSql();
        }
        vqr.dispose();
    }

    private void cbDimInfoItem_StateChanged(ItemEvent evt) 
    {//GEN-FIRST:event_cbAllSchemasItemStateChanged
        this.toleranceTF.setEnabled( ! this.cbDimInfo.isSelected() );
        this.lblTolerance.setEnabled( ! this.cbDimInfo.isSelected() );
    }//GEN-LAST:event_cbAllSchemasItemStateChanged

    private void exitButton_actionPerformed(ActionEvent e) {
        if ( this.newView != null ) {
            this.svp.getViewLayerTree().removeView(this.newView.getViewName(),false);
            this.svp.setActiveView(this.currentActiveView);
        } else if ( this.existingView != null ) {
            this.existingView.removeLayer(this.validationLayer.getLayerName(),false);
            this.existingView.setMBR(this.usedViewMBR);  // Set MBR back to start
            this.svp.getViewLayerTree().collapseView(this.existingView.getViewName());
            this.svp.setActiveView(this.currentActiveView);
        } else {
            this.currentActiveView.removeLayer(this.validationLayer.getLayerName(),false);
        }
        this.svp.getActiveView().setActiveLayer(this.currentActiveLayer);
        this.svp.getViewLayerTree().expandView(this.currentActiveView.getViewName());
        this.dispose();
    }

    private void bulkDeleteErrors() 
    {
  
        int selectedRowCount = this.resultTable.getSelectedRowCount();
        if ( selectedRowCount == 0 ) {
            this.lblMessages.setText(this.propertyManager.getMsg("ERROR_NO_SELECTION"));
            return;
        }
        
        // Ask to confirm deletion
        //
        int selectOpt = JOptionPane.showConfirmDialog(this,
                                                      this.propertyManager.getMsg("CONFIRM_VALIDATE_DELETE",selectedRowCount),
                                                      Constants.GEORAPTOR,
                                                      JOptionPane.YES_NO_OPTION);
        if (selectOpt == 0)
        {
            // Process
            //
            int rows[] = this.resultTable.getSelectedRows();
            if ( selectedRowCount==1 ) {
                this.vtModel.removeRow(this.resultTable.convertRowIndexToModel(rows[0]));
                return;
            }
            // Removing more than one row needs to be done carefully
            this.vtModel.removeSelectedRows(rows);
        }
    }    

    private void resultTableMouseClicked(MouseEvent evt) 
    {
        if ( SwingUtilities.isRightMouseButton(evt) ) 
        {
            // Is there anything under the mouse? 
            //
            if ( this.resultTable.getSelectedRows().length == 0 ) 
            {
                int row = this.resultTable.rowAtPoint(evt.getPoint());
                if (row == -1) {
                    return;
                } else {
                   this.vtLSM.setSelectionInterval(row, row);
                }
            }
            int col = this.resultTable.columnAtPoint(evt.getPoint());
            if ( col == -1 ) return;
            // Show right popup menu
            this.lblMessages.setText("");
            popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    private void createRightMouseClickMenu() {
        AbstractAction helpError = 
            new AbstractAction(this.propertyManager.getMsg("MENU_HELP_ERROR")) {
				private static final long serialVersionUID = -910319575585133596L;

				public void actionPerformed(ActionEvent e) {
                    helpError(e);
                }
            };
        popupMenu.add(helpError);
      
        AbstractAction zoomErrors = 
            new AbstractAction(this.propertyManager.getMsg("MENU_ZOOM_ERRORS")) {
				private static final long serialVersionUID = -5286588698351997042L;

				public void actionPerformed(ActionEvent e) {
                    zoomErrors(e);
                }
            };
        popupMenu.add(zoomErrors);
        
        AbstractAction mapErrors = 
            new AbstractAction(this.propertyManager.getMsg("MENU_MAP_ERRORS")) {
				private static final long serialVersionUID = 169494481915273544L;

				public void actionPerformed(ActionEvent e) {
                  drawErrors(e);
                }
            };
        popupMenu.add(mapErrors);

        AbstractAction zoomFeatures = 
            new AbstractAction(this.propertyManager.getMsg("MENU_ZOOM_FEATURES")) {
				private static final long serialVersionUID = 7201521284965157715L;

				public void actionPerformed(ActionEvent e) {
                  zoomFeatures(e);
                }
            };
        popupMenu.add(zoomFeatures);
  
        AbstractAction mapFeatures = 
            new AbstractAction(this.propertyManager.getMsg("MENU_MAP_FEATURES")) {
				private static final long serialVersionUID = 2255074574233613087L;

				public void actionPerformed(ActionEvent e) {
                  drawFeatures(e);
                }
            };
        popupMenu.add(mapFeatures);

        AbstractAction stepThroughFeatureErrors = 
          new AbstractAction(this.propertyManager.getMsg("MENU_STEP_ERRORS")) {
			private static final long serialVersionUID = 1571765704199067207L;

			public void actionPerformed(ActionEvent e) {
                showSingleFeatureErrors(e);
              }
          };
        popupMenu.add(stepThroughFeatureErrors);
        AbstractAction deleteErrors = 
          new AbstractAction(this.propertyManager.getMsg("MENU_DELETE_ERRORS")) {
			private static final long serialVersionUID = -6607364711079444899L;

			public void actionPerformed(ActionEvent e) {
                bulkDeleteErrors();
              }
          };
        popupMenu.add(deleteErrors);

        /** Clipboard entries **/
        
        JMenu copyToClipboardMenu = new JMenu(this.propertyManager.getMsg("MENU_COPY_CLIPBOARD"));  
        AbstractAction copyGeometry = 
          new AbstractAction(this.propertyManager.getMsg("MENU_COPY_GEOMETRY")) {

			private static final long serialVersionUID = 6616308162079427012L;

			public void actionPerformed(ActionEvent e) {
                copyToClipboard(CLIPBOARD_TYPES.GEOMETRY);
              }
          };
        copyToClipboardMenu.add(copyGeometry);
        
        AbstractAction copyElemInfo = 
          new AbstractAction(this.propertyManager.getMsg("MENU_COPY_SDO_ELEM_INFO")) {

			private static final long serialVersionUID = 3861924974867951855L;

			public void actionPerformed(ActionEvent e) {
                copyToClipboard(CLIPBOARD_TYPES.ELEMENT_INFO);
              }
          };
        copyToClipboardMenu.add(copyElemInfo);
      
        AbstractAction copyErrorText = 
          new AbstractAction(this.propertyManager.getMsg("MENU_COPY_ERROR_TEXT")) {

			private static final long serialVersionUID = -1062059725023738934L;

			public void actionPerformed(ActionEvent e) {
                copyToClipboard(CLIPBOARD_TYPES.ERROR_TEXT);
              }
          };
        copyToClipboardMenu.add(copyErrorText);
        popupMenu.add(copyToClipboardMenu);
        
        /** End of Clipboard entries **/         
    }

    private void zoomErrors(ActionEvent e)     
    {
        makeVisible();
        if ( this.resultTable.getSelectedRowCount() != 0 ) {
            lblMessages.setText("");
            drawResult(true,true,false);
        } else {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.ERROR_NO_SELECTION);
        }
    }

    private void drawErrors(ActionEvent e)     
    {
        makeVisible();
        if ( this.resultTable.getSelectedRowCount() != 0 ) {
            lblMessages.setText("");
            drawResult(true,false,false);
        } else {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.ERROR_NO_SELECTION);
        }
    }

    private void zoomFeatures(ActionEvent e) 
    {
        makeVisible();
        if ( this.resultTable.getSelectedRowCount() != 0 ) {
            lblMessages.setText("");
            drawResult(false,true,false);
        } else {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.ERROR_NO_SELECTION);
        }
    }
    
    private void drawFeatures(ActionEvent e) 
    {
        makeVisible();
        if ( this.resultTable.getSelectedRowCount() != 0 ) {
            lblMessages.setText("");
            drawResult(false,false,false);
        } else {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.ERROR_NO_SELECTION);
        }
    }

    private void helpError(ActionEvent e)     
    {
        if ( this.resultTable.getSelectedRowCount() != 1 ) {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.ERROR_ONE_SELECTION);
            return;
        }
        
        lblMessages.setText("");
        String errorCode = (String)this.vtModel.getValueAt(this.resultTable.convertRowIndexToModel(this.resultTable.getSelectedRow()), 
                                                           ValidationTableModel.errorColumn);
        if (Strings.isEmpty(errorCode) ) {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.NO_ERROR_FOUND);
            return;
        } 
        
        String errorText = ""; 
        try {
            errorText = this.helpManager.getMsg(errorCode);
            if (Strings.isEmpty(errorText) ) {
                Toolkit.getDefaultToolkit().beep();
                this.lblMessages.setText(this.propertyManager.getMsg("NO_ERROR_HELP",errorCode));
                return;
            }
            String[] helpText = errorText.split("[@]");
            JOptionPane.showMessageDialog(this, this.propertyManager.getMsg("HELP_FORMAT_STRING", 
                                                                            errorCode,
                                                                            helpText[0],
                                                                            helpText[1]),
                                          "Help", 
                                          JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.NO_ERROR_FOUND);
        }        
    }
    
    private void copyToClipboard(CLIPBOARD_TYPES _actionType) 
    {
        StringBuffer sBuffer = new StringBuffer();
        int rowsCopied = 0;
        int rows[] = resultTable.getSelectedRows();
        if ( rows.length == 0 ) {
            // Process one if exists?
            Point i = this.popupMenu.getLocation();
            int row = this.resultTable.rowAtPoint(i);
            if ( row == -1 ) {
                sBuffer.append("");
            } else {
                row = this.resultTable.convertRowIndexToModel(row);
                switch (_actionType ) {
                case GEOMETRY     : sBuffer.append(this.vtModel.getValueAt(row,ValidationTableModel.geoConstructorColumn));
                                    break;
                case ELEMENT_INFO : sBuffer.append(
                                        RenderTool.renderElemInfoArray(SDO_GEOMETRY.getSdoElemInfo((STRUCT)this.vtModel.getValueAt(row,ValidationTableModel.geometryColumn)), 
                                                                           false, false, Constants.bracketType.NONE));
                                    break;
                case ERROR_TEXT   :
                default           : sBuffer.append(this.vtModel.getValueAt(row,ValidationTableModel.errorColumn) + " " +
                                                   this.vtModel.getValueAt(row,ValidationTableModel.resultColumn));
                }
                rowsCopied++;
            }
        } else if ( rows.length >= 1 ) {
            rowsCopied = rows.length;
            int viewRow = -1;
            for (int iRow=0; iRow<rows.length; iRow++) {
                viewRow = this.resultTable.convertRowIndexToModel(rows[iRow]);
                switch (_actionType ) {
                case GEOMETRY     : sBuffer.append(this.vtModel.getValueAt(viewRow,ValidationTableModel.geoConstructorColumn)); 
                                    break;
                case ELEMENT_INFO : sBuffer.append(
                                        RenderTool.renderElemInfoArray(SDO_GEOMETRY.getSdoElemInfo((STRUCT)this.vtModel.getValueAt(viewRow,ValidationTableModel.geometryColumn)), 
                                                                           false, false, Constants.bracketType.NONE));
                                    break;
                case ERROR_TEXT   :
                default           : sBuffer.append(this.vtModel.getValueAt(viewRow,ValidationTableModel.errorColumn) + " " +
                                                   this.vtModel.getValueAt(viewRow,ValidationTableModel.resultColumn));
                }
            }
        }
        if ( sBuffer.length() > 0 ) {
            Toolkit.getDefaultToolkit().beep();
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection ss = new StringSelection(sBuffer.toString());
            clipboard.setContents(ss, ss);
            this.lblMessages.setText(this.propertyManager.getMsg("ROWS_COPIED_TO_CLIPBOARD",rowsCopied));
        }
    }
  
    private void showSingleFeatureErrors(ActionEvent e) 
    {
        if ( this.resultTable.getSelectedRowCount() != 1 ) {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.ERROR_ONE_SELECTION);
            return;
        }
        
        OracleConnection conn = this.getConnection();
        lblMessages.setText("");
        int rows[] = this.resultTable.getSelectedRows();
        List<QueryRow> geoSet             = new ArrayList<QueryRow>(); 
        LinkedHashMap<String,Object> attr = new LinkedHashMap<String,Object>();
        int row = this.resultTable.convertRowIndexToModel(rows[0]);
        attr.put(this.vtModel.getColumnName(1), this.vtModel.getValueAt(row,ValidationTableModel.errorColumn));
        // Some error messages are for completely invalid geometries that cannot be processed or mapped
        // if return is null we skip this geometry for mapping or context analysis
        //
        JGeometry geom = convertGeometry((STRUCT)this.vtModel.getValueAt(row,ValidationTableModel.geometryColumn));
        if ( geom == null ) {
            // Should we change the colour of this row selection to red?
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.propertyManager.getMsg("ERROR_GEOMETRY_PROBLEM",this.vtModel.getValueAt(row,ValidationTableModel.resultColumn)));
        } else {
            LinkedHashMap<String,JGeometry> errorGeoms = processContext(geom,
                                                                        this.vtModel.getValueAt(row,ValidationTableModel.resultColumn).toString(),
                                                                        true);
            if ( errorGeoms != null ) {
                String key = "";
                JGeometry jGeom = null;
                Iterator<String> iter = errorGeoms.keySet().iterator();
                while (iter.hasNext() ) {
                    key = (String)iter.next();
                    jGeom = errorGeoms.get(key);
                    if ( jGeom != null ) {
                        try {
                            attr.put("ContextError", key);
                            geoSet.add(new QueryRow(this.vtModel.getValueAt(row,ValidationTableModel.idColumn).toString(),
                                                    new LinkedHashMap<String,Object>(attr),
                                                    jGeom,
                                                    conn));
                        } catch (Exception ex) {
                            LOGGER.warn("Failed to convert error geometry for " + key);
                        }
                    }
                }
            }
        }
        if ( geoSet.size() != 0 ) {
            // Clear display anyway
            svp.getAttDataView().clear();

            // Show geometry objects
            //
            svp.showGeometry(this.validationLayer,
                             null  /* geo */,
                             geoSet,
                             new Envelope(geoSet),
                             false /* Selection Colouring*/,
                             false /* Zoom To Selection*/,
                             false /* drawAfter */ );

            // show attrib data in bottom tabbed pane
            //
            svp.getAttDataView().showData(geoSet);
        }
        attr.clear();
    }
    
    private void runValidationButton_actionPerformed(ActionEvent e) {
        lblMessages.setText("");
        processRunValidationButton();
    }
    
    // Getters and Setters

    public OracleConnection getConnection() {
        OracleConnection conn = DatabaseConnections.getInstance().getConnection(this.connectionName);
        if ( conn == null ) {
            // Try and get any connection though this should be connection of object.
            conn = DatabaseConnections.getInstance().getActiveConnection();
            if ( conn==null ) {
                conn = DatabaseConnections.getInstance().getAnyOpenConnection();
            }
        }
        if ( conn==null || ! DatabaseConnections.getInstance().openConnection(this.connectionName)) {
            return null;
        }
        return conn;
    }

    public void setSchemaName(String _schemaName) {
        this.schemaName = _schemaName;
    }
    public String getSchemaName() {
        return this.schemaName;
    }

    public void setObjectName(String _objectName) {
        this.objectName = _objectName;
    }
    public String getObjectName() {
        return this.objectName;
    }
    public String getFullObjectName() {
        return Strings.isEmpty(this.schemaName) 
               ? this.objectName :
               Strings.append(this.schemaName,this.objectName,Constants.TABLE_COLUMN_SEPARATOR);        
    }

    public String getSelectedColumnName() {
        return (String)this.cmbGeoColumns.getSelectedItem();
    }

    public String getGeomColumnName() {
        return this.geomColumnName;
    }

    public void setGeomColumnName(String _geomColumnName) {
        this.geomColumnName = _geomColumnName;
    }
    
    public String getSchemaObjectColumn() {
      return Strings.objectString(this.getSchemaName(),this.getObjectName(),this.getSelectedColumnName());
    }
    
    public boolean isDimInfo() {
        return this.cbDimInfo.isSelected();
    }
    
    @SuppressWarnings("unused")
	private String getMetadata() {
        String metadataString = "";
        String processingObject = Strings.objectString(this.getSchemaName(),this.getObjectName(),this.getSelectedColumnName());
        MetadataEntry me = this.metadata.get(processingObject);
        if ( this.cbDimInfo.isSelected() ) {
            metadataString = me.toDimArray();
        } else {
            if ( this.metadata == null || this.metadata.size() == 0  ) {
                metadataString = String.format("MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',-180,180,%s),\n" +
                                              "                     MDSYS.SDO_DIM_ELEMENT('Y',-90,90,%s))",
                                              this.getTolerance(processingObject),
                                              this.getTolerance(processingObject));
            } else {
                // update X and Y tolerance of metadata elements
                double oldTolerance = me.getEntry(0).getTol();
                try {
                    //DecimalFormat df = Tools.getNLSDecimalFormat(me.getMaxFractionDigits(Constants.DEFAULT_PRECISION),false);
                    String userTolerance = this.getTolerance(me.getFullName());
                    me.setXYTolerances(Double.valueOf(userTolerance));//.replace(String.valueOf(df.getDecimalFormatSymbols().getGroupingSeparator()), "")));
                } catch (Exception e) {
                  LOGGER.warn("Failed to parse double from tolerance " + this.getTolerance(me.getFullName()));
                }
                metadataString = me.toDimArray();
                me.setXYTolerances(oldTolerance);
            }
        }
        return metadataString;
    }

    protected String getValidationFunction(boolean _isUpdate)
    {
        String processingObject = Strings.objectString(this.getSchemaName(),this.getObjectName(),this.getSelectedColumnName());
        String validationFunction = this.make2D() 
                                    ? String.format("MDSYS.SDO_CS.MAKE_2D(%s)", this.getSelectedColumnName()) 
                                    : this.getSelectedColumnName();
        if ( _isUpdate) {
            validationFunction = String.format("MDSYS.SDO_GEOM.VALIDATE_GEOMETRY(%s,%s)",
                                               validationFunction,
                                               this.cbDimInfo.isSelected() 
                                               ? this.metadata.get(processingObject).toDimArray() 
                                               : this.getTolerance(processingObject));
        } else {
            validationFunction = String.format("MDSYS.SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(%s,%s)",
                                               validationFunction,
                                               this.cbDimInfo.isSelected() 
                                               ? this.metadata.get(processingObject).toDimArray() 
                                               : this.getTolerance(processingObject));
        }
        return validationFunction;
    }
    
    public String getSQL() {
        if (Strings.isEmpty(this.userSQL) ) {
            return this.getInitialSQL();
        } else {
            return this.userSQL;
        }
    }
    
    public String getInitialSQL() 
    {
        // If primary key has multiple columns turn them into a single string...
        // if indicator column is not selected, return '?' character
        //
        String primaryKey = ( !Strings.isEmpty(this.getIDColumn()) ? "t." + this.getIDColumn().replace(","," || ' - ' || t.") : "'???'" );
        String validationFunction = this.getValidationFunction(false /* We are Validating not Updating */ );
        String sql = "SELECT " + primaryKey + " as " + primaryKeyColumnName + ", \n" +
                     "       case when t." + this.getSelectedColumnName() + " is null \n" +
                     "            then 'NULL: " + this.getSelectedColumnName() + " IS NULL' \n" +
                     "            when t." + this.getSelectedColumnName() + ".sdo_gtype < 2000 \n" +
                     "            then '13000: sdo_gtype < 2000'\n" +
                     "            else "+ validationFunction + "\n" +
                     "        end as " + contextColumn + ",\n" +
                     "       " + this.getSelectedColumnName() + " as " + this.getGeomColumnName() + "\n" +
                     "  FROM " + this.getFullObjectName() + " t \n";
        if ( ! this.isNullGeoms() ) {
            sql += " WHERE " + this.getNullGeomsPredicate();
        }
        return sql;
      }
    
    public void setTotalRows(int _totalRows) {
        this.totalRows = _totalRows;
    }
    public int getTotalRows() {
        return this.totalRows;
    }

    public void setNullRowCount(int _nullRows) {
        this.nullRows = _nullRows;
    }

    public int getNullRowCount() {
        return this.nullRows;
    }
    
    public void setValidRowCount(int _validRows) {
        this.validRows = _validRows;
    }
    
    public int getValidRowCount() {
        return this.validRows;
    }
    
    public String getWhereClause() {
        if (Strings.isEmpty(this.userSQL) ) {
            if ( ! this.isNullGeoms() ) {
              return this.getNullGeomsPredicate();
            }
            return null;
        }
        int maxIndex = Math.max(this.userSQL.lastIndexOf("WHERE"),this.userSQL.lastIndexOf("where"));
        return this.userSQL.substring(maxIndex);
    }

    public void setTolerance(String _object) {
        this.setTolerance(this.metadata.get(_object));
    }
    
    public void setTolerance(MetadataEntry _mEntry) {
        if ( _mEntry == null ) return;
        // int       digits = _mEntry.getMaxFractionDigits(Constants.DEFAULT_PRECISION);
        //DecimalFormat df = Tools.getNLSDecimalFormat(digits==0?Constants.DEFAULT_PRECISION:digits, false);
        NumberFormat df = Tools.getDecimalFormatter();
        this.toleranceTF.setText(df.format(_mEntry.getMaxTolerance(Constants.DEFAULT_TOLERANCE)));
    }
    
    public String getTolerance(String _object) {
        if (Strings.isEmpty(this.toleranceTF.getText()) ) { 
            this.setTolerance(_object);
        }
        return this.toleranceTF.getText();
    }

    public void setSRID(String _srid) {
        this.tfSRID.setText(_srid);
    }
    public String getSRID() {
        String processingObject = Strings.objectString(this.getSchemaName(),this.getObjectName(),this.getSelectedColumnName());
        return this.metadata.get(processingObject).getSRID();
    }

    public int getSRIDAsInteger() {
      return this.getSRID().equals(Constants.NULL) 
           ? Constants.SRID_NULL 
           : Integer.valueOf(this.getSRID()).intValue();
    }
    
    public String getIDColumn() {
        return (String)this.cmbKeyColumns.getSelectedItem();
    }

    public int getDefaultCloseOperation() {
        return super.getDefaultCloseOperation();
    }

    private void processRunValidationButton() 
    {    
        if ((this.validThread != null) && (this.validThread.isThreadRunning()) ) {
            // stop current thread
            this.validThread.stopThread();
        } else {
            // check tolerance
            String sTol = "";
            try {

                // Get maximum fraction digits associated with this column from this.metadata
                //
                // int digits = this.metadata.get(processingObject).getMaxFractionDigits(Constants.DEFAULT_PRECISION);
                
                // Get formatter with the relevant database NLS setting applied
                //
                //DecimalFormat df = Tools.getNLSDecimalFormat(digits==0?Constants.DEFAULT_PRECISION:digits, false);
		        @SuppressWarnings("unused")
				NumberFormat df = Tools.getDecimalFormatter();
                sTol = this.toleranceTF.getText();//.replace(String.valueOf(df.getDecimalFormatSymbols().getGroupingSeparator()), "");
                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.parse(sTol);
                // Double.parseDouble(sTol);
            } catch (Exception _e) {
                this.errorDialogHandler.showErrorDialog(this,"ERROR_TOLERANCE_VALUE", sTol);
                return;
            }
            // set default values
            //
            setDefaults();
            // run thread
            this.validThread = new ValidationThread(this);
            this.validThread.start();
            this.updateValidationButtonStatus();
        }
    }

    private void setDefaults() {
        this.totalRows = -1;
        this.validRows = 0;
        this.nullRows = 0;
        this.vtModel.clear();
        lblMessages.setText("");
        validationProgressBar.setString(this.PROGRESS_START);
    }

    public void setVisible(final boolean _visible) {
        if ( _visible ) {
            // set default values
            setDefaults();
        } else {
            // clear some data that can eat a lot of memory
            this.vtModel.clear();
        }
        validationProgressBar.setString(PROGRESS_START);
        super.setVisible(_visible);
    }

    public void setMessage(final String _message) {
      SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                      if (Strings.isEmpty(_message) )
                          lblMessages.setText("");
                      else
                          lblMessages.setText(_message);
                  }
      });
    }

    public void progressMessage(String _msg) {
        final String msg = _msg;
        SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if ( ! validationProgressBar.isIndeterminate() ) {
                            validationProgressBar.setMinimum(0);
                            validationProgressBar.setMaximum(0);
                        }
                        validationProgressBar.setString(msg);
                    }
                });
    }

    public void updateValidationButtonStatus() {
        if ((this.validThread != null) && this.validThread.isThreadRunning() ) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    runValidationButton.setBackground(Color.RED);
                    runValidationButton.setText(BUTTON_CANCEL_VALIDATION);
                    runValidationButton.setMnemonic('C');
                    exitButton.setEnabled(false);
                    lblMessages.setText("");
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    runValidationButton.setBackground(Color.GREEN);
                    runValidationButton.setText(BUTTON_RUN_VALIDATION);
                    runValidationButton.setMnemonic('R');
                    exitButton.setEnabled(true);
                }
            });
        }
    }

    /**
     * @method convertGeometry
     * @param _stGeom
     * @return JGeometry or null
     * @author Simon Greener, June 25th 2010, Original coding
     *          - Some sdo_geometries with errors are so bad they cannot even be mapped
     *            This method tries to check that before any processing proceeds.
     *            The tests we will apply is to:
     *                1. Convert STRUCT to JGeometry
     *                2. Get MBR of JGeometry
     */
    private JGeometry convertGeometry(STRUCT _stGeom)
    {
        if ( _stGeom == null )
            return null;
        JGeometry geom;
        try {
            geom = JGeometry.load(_stGeom);
            if ( geom == null )
                return null;
            Envelope mbr = SDO_GEOMETRY.getGeoMBR(geom);
            if ( mbr == null )
                return null;
            // Must be OK
            return geom;
        } catch (SQLException e) {
            return null;
        }
    }
    
    public void makeVisible() {
        if ( ! ControlerSV.isSpatialViewVisible() ) {
            ControlerSV.showSpatialView();
        }
    }

    public void drawResult(boolean _context, 
                           boolean _zoom,
                           boolean _selectionColouring) 
    {
        DatabaseConnection dbConn = DatabaseConnections.getInstance().findConnectionByName(this.connectionName);
        if ( dbConn==null) {
            LOGGER.error("Connection, " + this.connectionName + " does not exist.");
            return;
        } else {
            if ( ! dbConn.isOpen() ) {
                dbConn.reOpen();
            }
        }
        OracleConnection conn = dbConn.getConnection();
        int rows[] = this.resultTable.getSelectedRows();
        List<QueryRow> geoSet             = new ArrayList<QueryRow>(); 
        LinkedHashMap<String,Object> attr = new LinkedHashMap<String,Object>();
        int row = 0;
        for (int i=0; i<rows.length; i++) {
            row = this.resultTable.convertRowIndexToModel(rows[i]);
            attr.put(this.vtModel.getColumnName(1), this.vtModel.getValueAt(row,ValidationTableModel.errorColumn));
            
            // Some error messages are for completely invalid geometries that cannot be processed or mapped
            // if return is null we skip this geometry for mapping or context analysis
            //
            JGeometry geom = convertGeometry((STRUCT)this.vtModel.getValueAt(row,ValidationTableModel.geometryColumn));
            if ( geom == null ) {
                // Should we change the colour of this row selection to red?
                Toolkit.getDefaultToolkit().beep();
                this.lblMessages.setText(this.propertyManager.getMsg("ERROR_GEOMETRY_PROBLEM",this.vtModel.getValueAt(row,ValidationTableModel.resultColumn)));
            } else {
                if ( _context ) {
                    LinkedHashMap<String,JGeometry> errorGeoms = processContext(geom,
                                                                                this.vtModel.getValueAt(row,ValidationTableModel.resultColumn).toString(),
                                                                                false);
                    if ( errorGeoms != null ) {
                        String key = "";
                        JGeometry jGeom = null;
                        Iterator<String> iter = errorGeoms.keySet().iterator();
                        while (iter.hasNext() ) {
                            key = (String)iter.next();
                            jGeom = errorGeoms.get(key);
                            if ( jGeom != null ) {
                                try {
                                    attr.put("ContextError", key);
                                    geoSet.add(new QueryRow(this.vtModel.getValueAt(row,ValidationTableModel.idColumn).toString(),
                                                            new LinkedHashMap<String,Object>(attr),
                                                            jGeom,
                                                            conn));
                                } catch (Exception e) {
                                    LOGGER.warn("Failed to convert error geometry for " + key);
                                }
                            }
                        }
                    }
                } else {
                    geoSet.add(new QueryRow(this.vtModel.getValueAt(row,0).toString(), 
                                            new LinkedHashMap<String,Object>(attr),
                                            (STRUCT)this.vtModel.getValueAt(row,ValidationTableModel.geometryColumn)));
                }
            }
        }
        if ( geoSet.size() != 0 ) {
            this.svp.getViewLayerTree().refreshNode(this.validationLayer.getLayerName());
            svp.showGeometry(this.validationLayer,
                             null,
                             geoSet,
                             new Envelope(geoSet),
                             _selectionColouring,
                             _zoom,
                             _zoom ? false : true /* drawAfter */);
        }
        attr.clear();
    }

    /**
     * Coordinates: 
     * A coordinate refers to a vertex in a geometry. 
     * In a two-dimensional geometry, a vertex is two numbers (X and Y, or Longitude and Latitude). 
     * In a three-dimensional geometry, a vertex is defined using three numbers; 
     * and in a four-dimensional geometry, a vertex is defined using four numbers. 
     * (You can use the SDO_UTIL.GETVERTICES function to return the coordinates in a geometry.)
     * 
     * If you receive a geometry validation error such as 13356 (adjacent points in a geometry are redundant), 
     * you can call the SDO_UTIL.GETVERTICES function, specifying a rownum stopping condition to include the 
     * coordinate one greater than the coordinate indicated with the error. The last two coordinates shown in 
     * the output are the redundant coordinates. These coordinates may be exactly the same, or they may be 
     * within the user-specified tolerance and thus are considered the same point. You can remove redundant 
     * coordinates by using the SDO_UTIL.REMOVE_DUPLICATE_VERTICES function.
     * 
     * Elements: 
     * An element is a point, a line string, or an exterior polygon with zero or more corresponding interior polygons. 
     * (That is, a polygon element includes the exterior ring and all interior rings associated with that exterior ring.) 
     * If a geometry is a multi-element geometry (for example, multiple points, lines, or polygons), the first element 
     * is element 1, the second element is element 2, and so on.
     * 
     * Rings: 
     * A ring is only used with polygon elements. Exterior rings in a polygon are considered polygon elements, and an 
     * exterior ring can include zero or more interior rings (or holes). Each interior ring has its own ring 
     * designation, but Ring 1 is associated with the exterior polygon itself. For example, Element 1, Ring 1 refers 
     * to the first exterior polygon in a geometry; Element 1, Ring 2 refers to the first interior polygon of the 
     * first exterior polygon; and Element 1, Ring 3 refers to the second interior polygon. If the geometry is a 
     * multipolygon, Element 2, Ring 1 is used to refers to the second exterior polygon. If there are interior 
     * polygons associated with it, Element 2, Ring 2 refers to the first interior polygon of the second exterior polygon.
     * 
     * Edges: 
     * An edge refers to a line segment between two coordinates. Edge 1 refers to the segment between coordinate 1 and 
     * coordinate 2, Edge 2 refers to the line segment between coordinates 2 and 3, and so on. The most common place 
     * to see edge errors when validating geometries is with self-intersecting polygons. (The Open Geospatial Consortium 
     * simple features specification does not allow a polygon to self-intersect.) In such cases, Oracle reports error 
     * 13349 (polygon boundary crosses itself), including the Element, Ring, and Edge numbers where self-intersection occurs.
     * 
     * Example:
     * 13349 [Element <1>] [Ring <1>][Edge <81>][Edge <25>]
     * 13351 [Element <1>] [Ring <1>][Edge <2>] [Element <2>] [Ring <1>][Edge <4>]
     * 13351 [Element <1>] [Rings 4, 5][Edge <5> in ring <4>][Edge <3> in ring <5>]
     * 
     **/
    public LinkedHashMap<String,JGeometry> processContext(JGeometry _geom, 
                                                          String    _validateContext,
                                                          boolean   _saveExplodedParts) 
    {
        JGeometry element = null;
        JGeometry ring    = null;
        JGeometry edge    = null;
        String tok        = "",
               key        = "";
        int elementOffset = -1, 
            edgeReference = -1, 
            ringOffset    = -1, 
            pointOffset   = -1;

        this.lblMessages.setText("");      
        if (Strings.isEmpty(_validateContext) ) {
            Toolkit.getDefaultToolkit().beep();
            this.lblMessages.setText(this.propertyManager.getMsg("ERROR_NO_CONTEXT_INFORMATION",this.getSelectedColumnName()));
            return (LinkedHashMap<String,JGeometry>)null;
        }

        LinkedHashMap<String,JGeometry> contextErrors = new LinkedHashMap<String,JGeometry>();
        StringTokenizer context = new StringTokenizer(_validateContext,"<[]>",false);
        boolean processingRingIntersections = false;
        
        while ( context.hasMoreTokens() ) {
            tok = context.nextToken().trim();
            if (Strings.isEmpty(tok) != true ) {
                key += Strings.isEmpty(key) ? tok : " " + tok;
                if ( tok.equalsIgnoreCase("Element") ) {
                    processingRingIntersections = false;
                    element = null;
                    ring    = null;
                    elementOffset = Integer.valueOf(context.nextToken().trim());
                    key += " " + String.valueOf(elementOffset);
                    try {
                        element = _geom.getElementAt(elementOffset);
                        // DEBUG LOGGER.info("Got Element " + elementOffset);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // Problem possibly with SDO_GTYPE and Element_Info
                        this.lblMessages.setText(this.propertyManager.getMsg("ERROR_ELEMENT_PROBLEM",elementOffset));
                        return (LinkedHashMap<String,JGeometry>)null;
                    }
                    if ( element != null ) {
                        // DEBUG LOGGER.info("element is " + element.getType());
                        if ( element.isRectangle() ) {
                            element = SDO_GEOMETRY.rectangle2Polygon2D(element);
                        }
                    }
                } else if ( tok.startsWith("Rings") ) { 
                    // We ignore the Ring references in [Rings 4, 5] as they
                    // are in the following [Edge n in ring y] element 
                    processingRingIntersections = true;
                } else if ( tok.equalsIgnoreCase("Ring") ) {
                    processingRingIntersections = false;
                    String ringString = context.nextToken().trim();
                    ringOffset = Integer.valueOf(ringString);
                    key += " " + ringString;
                    // DEBUG LOGGER.info(key);
                    if ( element != null ) {
                        ring = getRingAt(element,ringOffset);
                        if ( ring != null && _saveExplodedParts )
                            contextErrors.put(key,ring);
                    }
                } else if ( tok.equalsIgnoreCase("Edge") ) {
                    // [Edge <2>] or 
                    // [Edge <5> in ring <4>]
                    
                    // Next token is the Edge reference in current ring JGeometry
                    edgeReference = Integer.valueOf(context.nextToken().trim());
                    key = "Edge " + String.valueOf(edgeReference);
                    // Now, if we are processing Ring intersections via [Rings 4, 5] construct
                    // we need to extract the relevant ring
                    // [Edge <5> in ring <4>]
                    //
                    if ( processingRingIntersections /* ie [Rings 4, 5] */ ) {
                        // Next token should be the actual the text 'in ring' which we throw away
                        tok = context.nextToken().trim();
                        if (Strings.isEmpty(tok)==false && tok.equalsIgnoreCase("in ring") ) {
                            // Now we can get the ring
                            String ringString = context.nextToken();
                            ringOffset = Integer.valueOf(ringString.trim());
                            key += " in ring " + String.valueOf(ringOffset);
                            if ( element != null ) {
                                ring = getRingAt(element,ringOffset);
                                if ( ring != null && _saveExplodedParts ) {
                                    contextErrors.put(key,ring);
                                }
                            } 
                        } else { // Something is wrong
                            Toolkit.getDefaultToolkit().beep();
                            this.lblMessages.setText(String.format(this.propertyManager.getMsg("ERROR_IN_RING_PROBLEM"),key + "(" + tok + ")",_validateContext));
                            return (LinkedHashMap<String,JGeometry>)null;
                        }
                    }
                    // Now process the edge reference into the Ring.
                    if ( ring != null ) {
                        if ( edgeReference == 0 ) {
                          key += " (Coordinate 0?)";
                          contextErrors.put(key,getPoints(ring,1 /* pointOffset */));
                          this.validationLayer.getStyling().setSegmentArrow(Constants.SEGMENT_ARROWS_TYPE.END);
                        } else {
                            edge = getEdge(ring,elementOffset,ringOffset,edgeReference);
                            if (edge != null) {
                                contextErrors.put(key,edge);
                            }
                        }
                        key = "";
                    } else {
                        Toolkit.getDefaultToolkit().beep();
                        int elements = 0;
                        try {
                            elements = (element==null) ? 0 : ( (element.getElements()==null) ? 0 : element.getElements().length);
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }
                        this.lblMessages.setText(this.propertyManager.getMsg("ERROR_RING_PROBLEM",ringOffset,elementOffset,elements));
                        return (LinkedHashMap<String,JGeometry>)null;
                    }
                } else if ( tok.equalsIgnoreCase("Coordinate") ) {
                    // Coordinate references seem to come before the ring they describe
                    pointOffset = Integer.valueOf(context.nextToken().trim());
                    key += " " + String.valueOf(pointOffset);
                    // For polygons the ring the duplicate coordinate exists in is in the next element!
                    if ( element.getType() == JGeometry.GTYPE_POLYGON 
                         ||
                         element.getType() == JGeometry.GTYPE_MULTIPOLYGON ) 
                    {
                        tok = context.nextToken().trim();
                        if ( tok.length() != 0 && tok.equalsIgnoreCase("Ring") ) {
                            // which ring?
                            ringOffset = Integer.valueOf(context.nextToken().trim());
                            key += " Ring " + String.valueOf(ringOffset);
                            ring = getRingAt(element,ringOffset);
                            if ( ring != null && _saveExplodedParts )
                                contextErrors.put(key,ring);
                            if ( ring == null ) {
                                Toolkit.getDefaultToolkit().beep();
                                this.lblMessages.setText(this.propertyManager.getMsg("ERROR_RING_PROBLEM",ringOffset,elementOffset,element.getElements().length));
                            } else {
                                contextErrors.put(key,getPoints(ring,pointOffset));
                            }
                            key = "";
                        }
                    } else {
                        contextErrors.put(key,getPoints(element,pointOffset));
                        key = "";
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                    this.lblMessages.setText(this.propertyManager.getMsg("ERROR_UNRECOGNISED_TOKEN",tok));
                }
            }
        }
        // We might need to create some fall back geometries for viewing.
        //
        if ( contextErrors.size() == 0 ) {
            if ( ring == null ) {
                if ( element == null ) { 
                    Toolkit.getDefaultToolkit().beep();
                    this.lblMessages.setText(this.propertyManager.getMsg("ERROR_INSUFFICIENT_CONTEXT_INFORMATION"));
                    contextErrors.put("Geometry",_geom);
                } else {
                    this.lblMessages.setText(this.propertyManager.getMsg("ERROR_NO_CONTEXT_INFORMATION",elementOffset));
                    contextErrors.put("Element <" + elementOffset + ">",element);
                }
            } else {
                contextErrors.put(key,ring);
            }
        }
        return contextErrors;
    }
    
    public int getRingCount(JGeometry _element) {
        // We are processing only a simple polygon element's rings
        //
        if ( _element.getType() != JGeometry.GTYPE_POLYGON)
            return -1;
        
        int ringCount = 0;
        J3D_Geometry ring = null;
        ElementExtractor ee;
        try {
            ee = new ElementExtractor(
                                new J3D_Geometry( _element.getType(),
                                                  _element.getSRID(),
                                                  _element.getElemInfo(),
                                                  _element.getOrdinatesArray()),
                                0 /* Element Start*/,
                                ElementExtractor.INNER_OUTER /* Want rings */ );
            if ( ee != null ) {
                int[] inner_outer = {1};  // 1 = outer
                ring = ee.nextElement(inner_outer); 
                if ( ring != null ) {
                    ringCount++;
                    inner_outer[0] = 2; // 2 == inner
                    while ( (ring = ee.nextElement(inner_outer)) != null ) {
                        ringCount++;
                    }
                }
            }
        } catch (Exception e) {
            ringCount = -1;
        }
        return ringCount;
    }
    
    public JGeometry getRingAt(JGeometry _element,
                               int       _ringNumber) 
    {
        // We are processing only simple polygon element's rings
        //
        if ( _element.getType() != JGeometry.GTYPE_POLYGON)
            return null;
        
        J3D_Geometry ring = null;
        ElementExtractor ee;
        int[] inner_outer = {1};  // 1 = outer
        try {
            // Note: to use the Extractor the geometry has to be 3D. 
            // This stuffs up ordinate interpretation
            ee = new ElementExtractor(
                                new J3D_Geometry( _element.getType(),
                                                  _element.getSRID(),
                                                  _element.getElemInfo(),
                                                  _element.getOrdinatesArray()),
                                0 /* Element Start*/,
                                ElementExtractor.INNER_OUTER /* Want rings */ );
            if ( ee != null ) {
                int ringCount = 1;
                inner_outer[0] = 1;  // 1 = outer
                ring = ee.nextElement(inner_outer); 
                if ( ring != null && ringCount != _ringNumber) {
                    inner_outer[0] = 2; // 2 = inner
                    while ( (ring = ee.nextElement(inner_outer)) != null ) {
                        ringCount++;
                        if ( ringCount == _ringNumber )
                            break;
                    }
                }
            }
        } catch (Exception e) {
            // No nothing
        }
        return new JGeometry(_element.getType(),
                             _element.getSRID(),
                             (inner_outer[0]==0) ? new int[] {1,1003,1} : new int[] {1,2003,1},
                             ring.getOrdinatesArray());

            
    }
        
    public JGeometry getEdge(JGeometry _geom, 
                                   int _element,
                                   int _ring,
                                   int _edgeNumber) 
    {
        if ( _geom == null ) { 
            return (JGeometry)null;
        }

        int edgeCount = _geom.getNumPoints()-1;
        if (  edgeCount < _edgeNumber ) {
            Toolkit.getDefaultToolkit().beep();
            lblMessages.setText(String.format(this.propertyManager.getMsg("FORMAT_STRING_EDGE_INVALID"),
                                              _edgeNumber,
                                              _element,
                                              _ring,
                                              edgeCount));
            return (JGeometry)null;
        }
        
        List<Line2D> edgePoints = new ArrayList<Line2D>();
        double[] points = _geom.getOrdinatesArray();
        int dim = _geom.getDimensions();
        int numPoints = _geom.getNumPoints();
        Point2D pt1 = new Point.Double(Double.MAX_VALUE,Double.MAX_VALUE);
        Point2D pt2 = new Point.Double(Double.MAX_VALUE,Double.MAX_VALUE);
        pt1.setLocation(points[0],points[1]);
        pt2.setLocation(pt1);
        int ord = dim;
        for (int pointNum = 1; pointNum < numPoints; pointNum++ ) {
            ord = (pointNum * dim);
            pt2.setLocation(points[ord],points[ord+1]);
            if ( pointNum == _edgeNumber ) {
                edgePoints.add(new Line2D.Double(pt1,pt2));
                break;
            }
            pt1.setLocation(pt2);
        }
        return edgePoints.size()==0 
               ? (JGeometry)null
               : new JGeometry( 2002,
                                _geom.getSRID(),
                                new int[] {1,2,1},
                                new double[] {edgePoints.get(0).getP1().getX(),
                                              edgePoints.get(0).getP1().getY(),
                                              edgePoints.get(0).getP2().getX(),
                                              edgePoints.get(0).getP2().getY()} );
    }

    public JGeometry getPoints(JGeometry _geom, 
                                     int _pointNumber) {
        if ( _geom == null )
            return null;
        double[] points = _geom.getOrdinatesArray();
        int dim = _geom.getDimensions();
        double[] ordArray = new double[dim];
        if ( _pointNumber <= (points.length / dim)) {
            int startOrd  = ( _pointNumber - 1 ) * dim;
            for ( int i=0; i<dim; i++ ) {
                ordArray[i] = points[startOrd+i];
            }
            return new JGeometry(ordArray[0],ordArray[1],_geom.getSRID());
        }
        return null;
    }

    private void btnValidationActions_actionPerformed(ActionEvent e) {
          ValidationUpdateSQL vua = new ValidationUpdateSQL(this);
          vua.loadAssociations(null);
          vua.setLocationRelativeTo(this);
          vua.setVisible(true);
    }

    private void btnLayerProperties_actionPerformed(ActionEvent e) {
        SVSpatialLayerProps slp = new SVSpatialLayerProps(null,"",false);
        slp.initDialog((SVGraphicLayer)this.validationLayer);
        // show dialog
        slp.setVisible(true);
    }

    public void set2D(boolean _2D) {
        this.twoDee = _2D;
    }

    public boolean make2D() {
        return this.twoDee;
    }

    public void setNullGeoms(boolean nullGeoms) {
        this.nullGeoms = nullGeoms;
    }

    public boolean isNullGeoms() {
        return nullGeoms;
    }

    public String getNullGeomsPredicate() {
        return "t." + this.getSelectedColumnName().toUpperCase() + " IS NOT NULL";
    }

    /**
     * @author : Simon Greener, June 21st 2010
     *            - Made inner class
     *            - Brought errorList inside, renamed to dataVector to align more with DefaultTableModel
     *            - Changed to AbstractTableModel
     */
    class ValidationTableModel extends DefaultTableModel // AbstractTableModel 
    {
		private static final long serialVersionUID = -2034788410206571099L;

		protected Vector<TableRow> dataVector = new Vector<TableRow>();

        protected static final String       idColumnName = "ID";
        protected static final int              idColumn = 0;
        protected static final String    errorColumnName = "Error";
        protected static final int           errorColumn = 1;
        protected static final String   resultColumnName = "Validation result";
        protected static final int          resultColumn = 2; 
        protected static final String geometryColumnName = "Geometry";
        protected static final int    geoRenderColumn = 3;
        protected static final int        geometryColumn = 4;
        protected static final int  geoConstructorColumn = 5;
        protected String[] columns = {idColumnName,
                                      errorColumnName,
                                      resultColumnName,
                                      geometryColumnName};

        /**
         * @author : Simon Greener, June 10th 2010
         *            - Made all columns incapable of being edited.
         */
        boolean[] canEdit = new boolean[] { false, false, false, false };
        
        public ValidationTableModel(  ) {
        }
        
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit[columnIndex];
        }
        
        public int getColumnCount() {
            return this.columns.length;
        }

        public String[] getColumnNames() {
            return this.columns;
        }
        
        public String getColumnName(int column) {
            return this.columns[column];
        }

        public int getRowCount() {
            if (this.isEmpty()) {
                return 0;
            } else {
                return this.getDataVector().size();
            }
        }

        public boolean isEmpty() {
            return ( this.dataVector == null || this.dataVector.size()==0 );
        }

        public Object getValueAt(int row, int column) {
            if (this.isEmpty() )
                return "???";
            if (row < this.getDataVector().size()) {
                TableRow tr = (TableRow)this.dataVector.get(row);
                switch (column) {
                    case idColumn             : { return tr.getId(); }
                    case errorColumn          : { return tr.getError(); }
                    case resultColumn         : { return tr.getCheckResult(); }
                    case geoRenderColumn      : { return tr.getGeoRender(); } 
                    case geometryColumn       : { return tr.getGeometry(); }
                    case geoConstructorColumn : { return tr.getGeoConstructor(); }
                }
            } 
            return "???";
        }

        public void removeSelectedRows(int[] _rows) {
            if ( _rows == null || _rows.length <= 0 )
                return;
            // Get collection of objects for deletion
            //
            int rModel = 0;
            ArrayList<TableRow> dRows = new ArrayList<TableRow>(_rows.length);
            for (int iRow=0; iRow < _rows.length; iRow++) {
                rModel = resultTable.convertRowIndexToModel(_rows[iRow]);
                dRows.add(this.dataVector.get(rModel));
            }
            // Now we can remove them
            //
            this.dataVector.removeAll(dRows);  
            this.fireTableDataChanged();
        }
        
        public void removeRow(int _row) {
            this.dataVector.remove(_row);
            this.fireTableRowsDeleted(_row,_row);
        }

        public void addRow(TableRow _validationRow) {
            this.dataVector.add(_validationRow);
        }
        
        public void clear() {
            this.dataVector.clear();
            this.fireTableDataChanged();
        }

        public Vector<TableRow> getDataVector() {
            return new Vector<TableRow>(this.dataVector);
        }

        public void setDataVector(Vector<TableRow> dataVector) {
            this.dataVector = new Vector<TableRow>(dataVector);
        }
    }  // class ValidationTableModel
    
    /**
     * @author Matic
     * @version 1.0
     * @author Simon Greener, April 4th 2010
     *          Added support for validation by DimInfo
     * @author Simon Greener, April 19th 2011
     *          Made inner class 
     *          Access to SQL via named aliases
     **/

    class ValidationThread extends Thread 
    {
        protected ValidateSDOGeometry main;
        protected boolean      cancelCheck;
        protected boolean        threadRun;
        protected Date           startTime;
        private   int       commitInterval = 50; // Should be preference 

        public ValidationThread(ValidateSDOGeometry main) {
            this.main = main;
        }

        private void updateResults(final int rowsProcessed) {
          SwingUtilities.invokeLater(new Runnable() {
              public void run() { 
                  // update progress bar
                  //
                  // update labels
                  main.lblErrorCount.setText(String.valueOf(main.vtModel.getRowCount()));
                  if ( ! main.validationProgressBar.isIndeterminate() ) {
                      main.validationProgressBar.setMinimum(0);
                      main.validationProgressBar.setMaximum(main.totalRows);
                      main.validationProgressBar.setValue(rowsProcessed);
                      if ( main.totalRows > 0 ) { 
                          if ( totalRows == rowsProcessed )
                              main.validationProgressBar.setString(String.format(main.propertyManager.getMsg("FORMAT_STRING_PROGRESS_VALIDATED"),
                                                                                 rowsProcessed,
                                                                                 getTotalRows()));
                          else {
                              /** Compute time taken
                                */
                              Date now = new Date();
                              long timeDiff = 0;
                              if ( getStartTime() != null ) {
                                  timeDiff = ( now.getTime() - getStartTime().getTime() ); 
                              }
                              main.validationProgressBar.setString(String.format(main.propertyManager.getMsg("FORMAT_STRING_PROGRESS_ESTIMATED"),
                                                                                 rowsProcessed,
                                                                                 getTotalRows(),
                                                                                 Tools.milliseconds2Time(timeDiff)));
                          }
                      }
                  } else {
                      /** Compute time taken
                       **/
                      Date now = new Date();
                      long timeDiff = 0;
                      if ( getStartTime() != null ) {
                          timeDiff = ( now.getTime() - getStartTime().getTime() );
                      }
                      main.validationProgressBar.setString(String.format(propertyManager.getMsg("FORMAT_STRING_PROGRESS_ESTIMATED_INDETERMINATE"),
                                                                         rowsProcessed,
                                                                         Tools.milliseconds2Time(timeDiff)));                      
                  }
              }
          });
        }
        
        public void finalResults(final boolean _error) {
          SwingUtilities.invokeLater(new Runnable() {
              public void run() { 
                  // update labels
                  main.lblErrorCount.setText(String.valueOf(main.vtModel.getRowCount()));
                  int rowsProcessed = main.vtModel.getRowCount() + main.getValidRowCount() + ( main.isNullGeoms() ? main.getNullRowCount() : 0 );
                  main.lblErrorCount.setText(String.valueOf(main.vtModel.getRowCount()));
                  if ( ! main.validationProgressBar.isIndeterminate() ) {
                      main.validationProgressBar.setValue(rowsProcessed);  // Will turn off progress bar if rowsProgressed == maximumRows?
                      if ( getTotalRows() == rowsProcessed ) {
                          main.validationProgressBar.setString(String.format(main.propertyManager.getMsg("FORMAT_STRING_VALIDATION_COMPLETED"),
                                                                              main.validRows,
                                                                              main.isNullGeoms() ? String.format(" %d null,",main.getNullRowCount()) : "",
                                                                              main.vtModel.getRowCount(),
                                                                              (getTotalRows()==0) 
                                                                              ? 0 
                                                                              : MathUtils.roundToDecimals((float)main.vtModel.getRowCount() / (float)main.getTotalRows() * 100.0f,2),
                                                                              main.getTotalRows()));
                      } else { 
                          main.validationProgressBar.setString(String.format(main.propertyManager.getMsg("FORMAT_STRING_VALIDATION_TERMINATED"),
                                                                              rowsProcessed,
                                                                              main.vtModel.getRowCount(),
                                                                              main.isNullGeoms() ? String.format("/%d null",main.getNullRowCount()) : "",
                                                                              main.getTotalRows()));
                      }
                  } else {
                      String formatString = _error 
                                           ? "FORMAT_STRING_VALIDATION_FAILED_INDETERMINATE" 
                                           : "FORMAT_STRING_VALIDATION_COMPLETED_INDETERMINATE";
                      float denom = main.getValidRowCount() + getNullRowCount() + main.vtModel.getRowCount();
                      main.validationProgressBar.setString(String.format(main.propertyManager.getMsg(formatString),
                                                                          main.validRows,
                                                                          main.isNullGeoms() ? String.format(" %d null,",main.getNullRowCount()) : "",
                                                                          main.vtModel.getRowCount(),
                                                                          MathUtils.roundToDecimals(((float)main.vtModel.getRowCount() / denom) * 100.0f,2)));
                      main.validationProgressBar.setIndeterminate(false);
                  }
                  if ( main.vtModel.getRowCount()!=0) {
                      // Fire change to force redraw of contents
                      main.vtModel.fireTableRowsInserted(0, main.vtModel.getRowCount()-1);
                      ArrayList<Integer> columns = new ArrayList<Integer>(1);
                      columns.add(ValidationTableModel.resultColumn);
                      Tools.autoResizeColWidth(main.resultTable,columns);
                      main.resultTable.getRowSorter().toggleSortOrder(ValidationTableModel.errorColumn);
                  }
               }
          });
        }

        /**
         * @method run()
         * @author Simon Greener, 9th July 2010.
         *          Added where clause to calculation of number of rows.
         *          Changed status.equals("ORA-NULL") to status.startsWith("ORA-NULL")
         */
        public void run() 
        {
            this.startTime = new Date();
            
           // get number of all rows
            try 
            {
                this.main.setTotalRows(-1);
                this.main.progressMessage("Counting rows in " + this.main.getObjectName());
                String whereClause = this.main.getWhereClause();
                this.main.setTotalRows(MetadataTool.getRowCount(this.main.getConnection(),
                                                                this.main.getSchemaName(),
                                                                this.main.getObjectName(),
                                                                whereClause));
            } catch (SQLException sqle) {
                // getRowCount throws actual SQL as its exception
                //
                Tools.copyToClipboard(this.main.propertyManager.getMsg("ERROR_VALIDATION_SQL",
                                                                       sqle.getErrorCode(), 
                                                                       sqle.getMessage() /* is the actual SQL */),
                                      sqle.getMessage() /* is actual SQL */ );
                this.main.setTotalRows(-1); // flow through to setting indeterminate mode
            }

            if ( this.main.getTotalRows() == -1 ) {
                this.main.progressMessage(this.main.propertyManager.getMsg("ERROR_ROW_COUNT"));
                this.main.validationProgressBar.setIndeterminate(true);
            } else {
                this.main.progressMessage(this.main.getObjectName() + " has " + this.main.getTotalRows() + " to validate.");
                this.main.validationProgressBar.setIndeterminate(false);
            }
            // Execute actual query ...
            //
            String sql = "";
            OraclePreparedStatement validationPS = null;
            OracleResultSet         validationRS = null;
            try {
                sql = this.main.getSQL();
                validationPS = (OraclePreparedStatement)this.main.getConnection().prepareStatement(sql);
                validationPS.setRowPrefetch(150);
                validationPS.setFetchSize(150);
                validationPS.setFetchDirection(ResultSet.FETCH_FORWARD);
                validationRS = (OracleResultSet)validationPS.executeQuery();
                String id = "", 
                    error = "", 
                   status = "";                
                int primaryKeyColumn          = validationRS.findColumn(ValidateSDOGeometry.primaryKeyColumnName);
                if ( primaryKeyColumn == -1 ) throw new SQLException("Required, named, column " + ValidateSDOGeometry.primaryKeyColumnName + " not found");
                int contextColumn             = validationRS.findColumn(ValidateSDOGeometry.contextColumn);
                if ( contextColumn == -1 )    throw new SQLException("Required, named, column " + ValidateSDOGeometry.contextColumn + " not found");
                int geomColumnId              = validationRS.findColumn( this.main.getGeomColumnName());
                if ( geomColumnId==-1 )       throw new SQLException("Required, named, column " + this.main.getGeomColumnName()+ " not found");
                
                STRUCT stGeom;
                this.main.setNullRowCount(0);
                this.main.setValidRowCount(0);
                int indx = -1,
                    rowCount = 0;
                Envelope lMBR = new Envelope(Constants.MAX_PRECISION);
                while (validationRS.next() && this.isNotCancelled()) {
                    error = "";
                    rowCount++;
                    if ( validationRS.getMetaData().getColumnType(primaryKeyColumn)==OracleTypes.ROWID )
                        id = new String(validationRS.getROWID(primaryKeyColumn).getBytes());
                    else
                        id = validationRS.getString(primaryKeyColumn); 
                    status = validationRS.getString(contextColumn);
                    if ( validationRS.wasNull() ) { 
                        status = "NULL";
                    }
                    if (Strings.isEmpty(status) ) {
                        // Do nothing as this will be skipped
                    } else if ( status.startsWith("NULL") ) {
                        this.main.setNullRowCount( this.main.getNullRowCount() + 1 );
                    } else if ( status.equalsIgnoreCase("TRUE") ) {
                        this.main.setValidRowCount( this.main.getValidRowCount() + 1 );
                    } else {
                        if ( status.matches("^13[0-9][0-9][0-9]")){
                            error = status;
                            status = "";
                        } else { 
                            error = status;
                            indx = status.indexOf(" ");
                            if ( indx > 0 ) {
                                error  = status.substring(0,indx);
                                status = status.substring(indx+1);
                            }
                        }
                        stGeom = validationRS.getSTRUCT(geomColumnId);
                        this.main.vtModel.addRow(new TableRow(id, error, status, stGeom));
                        // Update layer MBR with this geometry's
                        //
                        if (stGeom != null) {
                            Envelope rd = SDO_GEOMETRY.getGeoMBR(stGeom);
                            if ( rd != null ) {
                                lMBR.setMaxMBR(rd);
                            }
                            // Set geometry type for layer
                            //
                            this.main.validationLayer.setGeometryType(SDO_GEOMETRY.discoverGeometryType(SDO_GEOMETRY.getFullGType(stGeom, 2001),
                                                                    Constants.GEOMETRY_TYPES.UNKNOWN));
                        }
                    }
                    // Update status
                    if ( rowCount % commitInterval == 0 ) {
                        this.updateResults(rowCount);
                    }
                }
                // Set MBRs
                this.main.validationLayer.setMBR(lMBR);
                this.main.validationView.setMBR(lMBR);
                // Shutdown UI progress elements
                this.finalResults(false);
                this.startTime = null;
            } catch (SQLException sqle) {
                Tools.copyToClipboard(this.main.propertyManager.getMsg("ERROR_VALIDATION_SQL",
                                                                       sqle.getErrorCode(),
                                                                       sql),
                                      sql);
                this.finalResults(true);
                this.stopThread();
            }
            try { validationRS.close(); } catch (Exception e) {} validationRS = null;
            try { validationPS.close(); } catch (Exception e) {} validationPS = null;
            this.setThreadRun(false);
            this.main.updateValidationButtonStatus();
        }

        public synchronized void start() {
            this.threadRun = true;
            super.start();
        }

        public synchronized void stopThread() {
            this.cancelCheck = true;
        }

        public void setThreadRun(boolean threadRun) {
            this.threadRun = threadRun;
        }

        public boolean isNotCancelled() {
            return this.cancelCheck == false;
        }
        
        public boolean isThreadRunning() {
            return this.threadRun;
        }

        public Date getStartTime() {
            return this.startTime;
        }
    } 
} // class ValidateSDOGeometry
