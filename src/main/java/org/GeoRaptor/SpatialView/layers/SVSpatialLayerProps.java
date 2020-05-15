package org.GeoRaptor.SpatialView.layers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.SupportClasses.DiscoverGeometryType;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.LineStyle;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.sql.DatabaseConnection;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.HtmlHelp;
import org.GeoRaptor.tools.LabelStyler;
import org.GeoRaptor.tools.MathUtils;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.geotools.util.logging.Logger;


public class SVSpatialLayerProps extends JDialog {

	private static final long serialVersionUID = 8121341328541147361L;

	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVSpatialLayerProps");
    
    /**
     * Properties File Manager
     **/
    private static final String propertiesFile = "org.GeoRaptor.SpatialView.resource.SVSpatialLayerProps";
    //protected ErrorDialogHandler errorDialogHandler;
    protected PropertiesManager propertyManager = null;

    private final String iconDirectory = "org/GeoRaptor/SpatialView/images/";
    private ClassLoader cl = this.getClass().getClassLoader();

    /**
     * Reference to data structure with properties
     */
    protected SVSpatialLayer layer;

    /**
     * Get reference to single instance of GeoRaptor's Preferences
     */
    protected Preferences SVSpatialLayerPreferences;

    /** Styling related attributes
     **/
    protected AttributeSet                 labelAttributes = null;
    protected Constants.TEXT_OFFSET_POSITION labelPosition = Constants.TEXT_OFFSET_POSITION.CC;
    protected int                      labelOffsetDistance = 0;
    
    /** Marking of vertices
     */
    protected AttributeSet        markLabelAttributes = null;
    protected Constants.TEXT_OFFSET_POSITION markLabelPosition = Constants.TEXT_OFFSET_POSITION.CL;
    protected int                  markOffsetDistance = 0;

    /**
     * Color of point
     */
    protected Color pointColor;

    /**
     * Color of line
     */
    protected Color lineColor;

    /**
     * Temporary color for shade
     */
    protected Color shadeColor;

    /**
     * Temporary color for selected object color
     */
    protected Color selectShadeColor;

    InputVerifier verifyPointSize = null;
    InputVerifier verifyLineWidth = null;
    InputVerifier scaleVerifier = null;
    InputVerifier bufferVerifier = null;

    protected Constants.SEGMENT_ARROWS_TYPE segmentArrow;

    /*
     * List of displayable attributes
     */
    LinkedHashMap<String, String> labelColumnsAndTypes = null;

    // ########################## CONSTRUCTORS ########################################

    /** Creates new form SVSpatialLayerProps */
    public SVSpatialLayerProps(Frame parent, String title, boolean modal) {
        super(parent, title, modal);

        try {
            // Get the one reference to GeoRaptor's preferences
            //
            this.SVSpatialLayerPreferences =
                    MainSettings.getInstance().getPreferences();

            this.propertyManager = new PropertiesManager(SVSpatialLayerProps.propertiesFile);

            this.initComponents();

            this.applyLabelStrings();

            verifyPointSize = new InputVerifier() {
                    public boolean verify(JComponent comp) {
                        boolean returnValue = true;
                        JTextField textField = (JTextField)comp;
                        try {
                            // This will throw an exception if the value is not an integer
                            int size = Integer.parseInt(textField.getText());
                            if (size < 4 || size > 72) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e) {
                            setAlwaysOnTop(false);
                            JOptionPane.showMessageDialog(null, 
                                                          propertyManager.getMsg("ERROR_TEXT_SIZE"),
                                                          MainSettings.EXTENSION_NAME,
                                                          JOptionPane.ERROR_MESSAGE);
                            setAlwaysOnTop(true);
                            returnValue = false;
                        }
                        return returnValue;
                    }
                };
            pointSizeTF.setInputVerifier(verifyPointSize);
            tfSelectionPointSize.setInputVerifier(verifyPointSize);

            verifyLineWidth = new InputVerifier() {
                    public boolean verify(JComponent comp) {
                        JTextField textField = (JTextField)comp;
                        try {
                            // This will throw an exception if the value is not an integer
                            int size = Integer.parseInt(textField.getText());
                            if (size < 1 || size > 16) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e) {
                            setAlwaysOnTop(false);
                            JOptionPane.showMessageDialog(null, 
                                                          propertyManager.getMsg("ERROR_LINE_WIDTH"),
                                                          MainSettings.EXTENSION_NAME,
                                                          JOptionPane.ERROR_MESSAGE);
                            setAlwaysOnTop(true);
                            return false;
                        }
                        return true;
                    }
                };
            lineWidthTF.setInputVerifier(verifyLineWidth);
            tfSelectionLineWidth.setInputVerifier(verifyLineWidth);

            bufferVerifier = new InputVerifier() {
                    public boolean verify(JComponent comp) {
                        boolean returnValue = true;
                        JTextField textField = (JTextField)comp;
                        try {
                            // This will throw an exception if the value is not an integer
                            Double.parseDouble(textField.getText());
                        } catch (NumberFormatException e) {
                            JOptionPane.showMessageDialog(null, e.getMessage(),
                                                          MainSettings.EXTENSION_NAME,
                                                          JOptionPane.ERROR_MESSAGE);
                            returnValue = false;
                        }
                        return returnValue;
                    }
                };
             tfQueryBufferDistance.setInputVerifier(bufferVerifier);

            scaleVerifier = new InputVerifier() {
                    public boolean verify(JComponent comp) {
                        JTextField textField = (JTextField)comp;
                            // This will throw an exception if the value is not an integer
                            int size = 0;
                            if ( !Strings.isEmpty(textField.getText()) ) {
                                try {
                                    size = Integer.parseInt(textField.getText());
                                    if (size < 0 || size > 10000000) {
                                        throw new NumberFormatException();
                                    }
                                } catch (NumberFormatException e) {
                                    setAlwaysOnTop(false);
                                    JOptionPane.showMessageDialog(null, 
                                                                  propertyManager.getMsg("ERROR_MAP_SCALE"),
                                                                  MainSettings.EXTENSION_NAME,
                                                                  JOptionPane.ERROR_MESSAGE);
                                    setAlwaysOnTop(true);
                                    return false;
                                }
                            }
                        return true;
                    }
                };
                tfLoScale.setInputVerifier(scaleVerifier);
                tfHiScale.setInputVerifier(scaleVerifier);
        } catch (Exception e) {
        }
    }

    // ########################## INITIALISATION ##################################

    /** Widget text are overridden by properties file values
     */

    private String DIALOG_LABEL = "Details";
    private String TAB_PROPERTIES = "Properties";
    private String TAB_POINT = "Point";
    private String TAB_STROKE = "Stroke";
    private String TAB_SELECTION = "Selection";
    private String TAB_SQL = "SQL";
    private String TAB_FILL = "Fill";
    private String TAB_LABEL = "Labelling";
    private String TAB_ROTATION = "Rotation";

    private String BUTTON_APPLY = "Apply";
    private String BUTTON_CHANGE = "Change";
    private String TT_VALUE_0_1 = "Value must be between 0 and 1";
    //private String TT_VALUE_MILLISECONDS = "Use %MS% for current milliseconds number";
    private String LABEL_DB_CONNECTION = "DB Connection";
    private String LABEL_LAYER_NAME = "Name:";
    private String LABEL_GEOCOLUMN_NAME = "Geometry column:";
    private String ERROR_GEOCOLUMN_NAME = "Geometry column cannot be empty";
    private String BUTTON_DISCOVER = "Discover";

    private String LABEL_SRID = "SRID:";

    private String LABEL_RECALCULATE_MBR = "Recalculate MBR (next draw)"; 
    private String TT_LAYER_MBR = "MBR or extent of layer";
    private String LABEL_FETCH_SIZE = "Fetch size:";
    private String LABEL_PIXEL_FILTERING = "Pixel Filtering:";

    //private String LABEL_FILL = "Fill:";
    private String LABEL_COLUMN_COLOR = "Column";
    private String LABEL_CONSTANT_COLOR = "Constant";
    private String BUTTON_SHADE_COLOR = "Color";
    private String LABEL_NONE = "None";
    private String LABEL_LINE_WIDTH = "Width:";
    private String BUTTON_LINE_COLOR = "Color";

    /*
    private String LABEL_ARROW_NONE = "None";
    private String LABEL_ARROW_START = "Start";
    private String LABEL_ARROW_MIDDLE = "Middle";
    private String LABEL_ARROW_END    = "End";
    private String LABEL_ARROW_END_ONLY = "End Only";
     */

    //private String BORDER_ARROWS = "Mark Arrows";
    private String BORDER_MARK_VERTICES = "Mark Vertices";
    private String LABEL_MARK_START = "Start";
    private String LABEL_MARK_ALL = "All";

    private String BUTTON_POINT_COLOR = "Color";
    //private String LABEL_POINT_SIZE = "Size:";
    private String LABEL_POINT_MARKER = "Marker:";
    private String BUTTON_SELECTION_COLOR = "Color";
    private String LABEL_SELECT_LINE_WIDTH = "Line width:";
    private String LABEL_SELECT_POINT_SIZE = "Point Size:";
    private String LABEL_SELECT_LINE_STYLE = "Line Style:";

    private String LABEL_TRANSPARENCY = "Value: ";
    private String LABEL_OPAQUE = "Solid";
    private String LABEL_TRANSPARENT = "Invisible";

    private String BUTTON_CLOSE = "OK";
    private String BUTTON_HELP = "Cancel";

    private String LABEL_COLUMNS_PULLDOWN = "Label Column:";
    private String LABEL_ROTATION_COLUMN = "Rotation Column:";
    //private String TT_LABEL_COLUMNS_PULLDOWN = "Select column in table to use as label";
    //private String TT_ROTATION_COLUMNS_PULLDOWN = "If label column selected, select column in table that contains rotation angle (otherwise 0)";
    private String LABEL_DEGREES = "Degrees";
    private String LABEL_RADIANS = "Radians";
    private String LABEL_MARKER_ROTATION = "Apply to Marker";
    private String TT_MARKER_ROTATION = "If label rotation column selected, do you want the actual marker symbol to be rotated as well as the text?";

    private String TT_DEGREES = "Value in column is in degrees";
    private String TT_RADIANS = "Value in column is in radians";
    //private String LABEL_COLUMN_STYLING = "Style";
    private String LABEL_POSITION = "Position";
    private String LABEL_OFFSET = "Offset";
    private String BORDER_LABEL_VERTICES = "Vertices";
    private String LABEL_MARK_ORIENTED = "Orient Labels";
    private String GEOMETRY_LABEL_POSITION = "Label Point:";
    
    public void applyLabelStrings() {

        this.DIALOG_LABEL = this.propertyManager.getMsg("DIALOG_LABEL");
        this.TAB_PROPERTIES = this.propertyManager.getMsg("TAB_PROPERTIES");
        this.TAB_STROKE = this.propertyManager.getMsg("TAB_STROKE");
        this.TAB_POINT = this.propertyManager.getMsg("TAB_POINT");
        this.TAB_LABEL = this.propertyManager.getMsg("TAB_LABEL");
        this.TAB_ROTATION = this.propertyManager.getMsg("TAB_ROTATION");
        this.TAB_SELECTION = this.propertyManager.getMsg("TAB_SELECTION");

        //this.BORDER_ARROWS = this.propertyManager.getMsg("BORDER_ARROWS");
        this.BORDER_MARK_VERTICES = this.propertyManager.getMsg("BORDER_MARK_VERTICES");

        this.BUTTON_APPLY = this.propertyManager.getMsg("BUTTON_APPLY");
        this.BUTTON_CHANGE = this.propertyManager.getMsg("BUTTON_CHANGE");

        this.pnlDetails.setBorder(javax.swing.BorderFactory.createTitledBorder(this.DIALOG_LABEL));
        this.pnlProperties.setBorder(javax.swing.BorderFactory.createTitledBorder(this.TAB_PROPERTIES));

        // Non tab panels
        this.pnlMark.setBorder(javax.swing.BorderFactory.createTitledBorder(this.BORDER_MARK_VERTICES));

        // Tab panels
        for (int i = 0; i < pnlProperties.getTabCount(); i++) {
            if (pnlProperties.getTitleAt(i).equals("Point"))
                pnlProperties.setTitleAt(i, this.TAB_POINT);
            else if (pnlProperties.getTitleAt(i).equals("Line"))
                pnlProperties.setTitleAt(i, this.TAB_STROKE);
            else if (pnlProperties.getTitleAt(i).equals("Area"))
                pnlProperties.setTitleAt(i, this.TAB_FILL);
            else if (pnlProperties.getTitleAt(i).equals("Selection"))
                pnlProperties.setTitleAt(i, this.TAB_SELECTION);
            else if (pnlProperties.getTitleAt(i).equals("Label"))
                pnlProperties.setTitleAt(i, this.TAB_LABEL);
            else if (pnlProperties.getTitleAt(i).equals("Rotation"))
                pnlProperties.setTitleAt(i, this.TAB_ROTATION);
            else if (pnlProperties.getTitleAt(i).equals("SQL"))
                pnlProperties.setTitleAt(i, this.TAB_SQL);
        }

        this.LABEL_DB_CONNECTION = this.propertyManager.getMsg("LABEL_DB_CONNECTION");
        this.LABEL_LAYER_NAME =  this.propertyManager.getMsg("LABEL_LAYER_NAME");
        this.LABEL_GEOCOLUMN_NAME = this.propertyManager.getMsg("LABEL_GEOCOLUMN_NAME");
        this.ERROR_GEOCOLUMN_NAME = this.propertyManager.getMsg("ERROR_GEOCOLUMN_NAME");
        this.BUTTON_DISCOVER = this.propertyManager.getMsg("BUTTON_DISCOVER");
      
        this.TT_LAYER_MBR = this.propertyManager.getMsg("TT_LAYER_MBR");
        this.LABEL_RECALCULATE_MBR = this.propertyManager.getMsg("LABEL_RECALCULATE_MBR");
        
        this.LABEL_FETCH_SIZE = this.propertyManager.getMsg("LABEL_FETCH_SIZE");
        this.LABEL_PIXEL_FILTERING = this.propertyManager.getMsg("LABEL_PIXEL_FILTERING");
        //this.LABEL_FILL = this.propertyManager.getMsg("LABEL_FILL");
        this.LABEL_COLUMN_COLOR = this.propertyManager.getMsg("LABEL_COLUMN_COLOR");
        this.LABEL_CONSTANT_COLOR = this.propertyManager.getMsg("LABEL_CONSTANT_COLOR"); 
        this.BUTTON_SHADE_COLOR = this.propertyManager.getMsg("BUTTON_SHADE_COLOR");
        this.LABEL_NONE = this.propertyManager.getMsg("LABEL_NONE");
        this.LABEL_LINE_WIDTH = this.propertyManager.getMsg("LABEL_LINE_WIDTH");
        this.BUTTON_LINE_COLOR = this.propertyManager.getMsg("BUTTON_LINE_COLOR");
        
        //this.LABEL_ARROW_NONE = this.propertyManager.getMsg("LABEL_ARROW_NONE");
        //this.LABEL_ARROW_START = this.propertyManager.getMsg("LABEL_ARROW_START");
        //this.LABEL_ARROW_MIDDLE = this.propertyManager.getMsg("LABEL_ARROW_MIDDLE");
        //this.LABEL_ARROW_END = this.propertyManager.getMsg("LABEL_ARROW_END");
        //this.LABEL_ARROW_END_ONLY = this.propertyManager.getMsg("LABEL_ARROW_END_ONLY");
        
        this.LABEL_MARK_START = this.propertyManager.getMsg("LABEL_MARK_START");
        this.LABEL_MARK_ALL = this.propertyManager.getMsg("LABEL_MARK_ALL");
        this.LABEL_OFFSET  = this.propertyManager.getMsg("LABEL_OFFSET");
        this.LABEL_POSITION = this.propertyManager.getMsg("LABEL_POSITION");
        this.GEOMETRY_LABEL_POSITION = this.propertyManager.getMsg("LABEL_POINT");
        //this.LABEL_POINT_SIZE = this.propertyManager.getMsg("LABEL_POINT_SIZE");
        this.LABEL_POINT_MARKER = this.propertyManager.getMsg("LABEL_POINT_MARKER");
        this.LABEL_MARK_ORIENTED = this.propertyManager.getMsg("LABEL_MARK_ORIENTED");
        
        this.BUTTON_POINT_COLOR = this.propertyManager.getMsg("BUTTON_POINT_COLOR");
        this.BUTTON_SELECTION_COLOR = this.propertyManager.getMsg("BUTTON_SELECTION_COLOR");
        this.LABEL_SELECT_LINE_WIDTH = this.propertyManager.getMsg("LABEL_SELECT_LINE_WIDTH");
        this.LABEL_SELECT_POINT_SIZE = this.propertyManager.getMsg("LABEL_SELECT_POINT_SIZE");
        this.LABEL_SELECT_LINE_STYLE = this.propertyManager.getMsg("LABEL_SELECT_LINE_STYLE");
        this.LABEL_TRANSPARENCY = this.propertyManager.getMsg("LABEL_TRANSPARENCY");
        this.LABEL_OPAQUE = this.propertyManager.getMsg("LABEL_OPAQUE");
        this.LABEL_TRANSPARENT = this.propertyManager.getMsg("LABEL_TRANSPARENT");
        this.TT_VALUE_0_1 = this.propertyManager.getMsg("TT_VALUE_0_1");
        //this.TT_VALUE_MILLISECONDS = this.propertyManager.getMsg("TT_VALUE_MILLISECONDS");

        this.LABEL_COLUMNS_PULLDOWN = this.propertyManager.getMsg("LABEL_COLUMNS_PULLDOWN");
        this.LABEL_ROTATION_COLUMN = this.propertyManager.getMsg("LABEL_ROTATION_COLUMN");
        this.LABEL_DEGREES = this.propertyManager.getMsg("LABEL_DEGREES");
        this.LABEL_RADIANS = this.propertyManager.getMsg("LABEL_RADIANS");
        this.LABEL_MARKER_ROTATION = this.propertyManager.getMsg("LABEL_MARKER_ROTATION");
        this.TT_MARKER_ROTATION = this.propertyManager.getMsg("TT_MARKER_ROTATION");
        this.TT_DEGREES = this.propertyManager.getMsg("TT_DEGREES");
        this.TT_RADIANS = this.propertyManager.getMsg("TT_RADIANS");
        //this.TT_LABEL_COLUMNS_PULLDOWN = this.propertyManager.getMsg("TT_LABEL_COLUMNS_PULLDOWN");
        //this.TT_ROTATION_COLUMNS_PULLDOWN = this.propertyManager.getMsg("TT_ROTATION_COLUMNS_PULLDOWN");
        //this.LABEL_COLUMN_STYLING = this.propertyManager.getMsg("LABEL_COLUMN_STYLING");
        this.LABEL_POSITION = this.propertyManager.getMsg("LABEL_POSITION");
        this.BORDER_LABEL_VERTICES = this.propertyManager.getMsg("BORDER_LABEL_VERTICES");

        this.BUTTON_CLOSE = this.propertyManager.getMsg("BUTTON_CLOSE");
        this.BUTTON_HELP = this.propertyManager.getMsg("BUTTON_HELP");

        this.lblDBConnection.setText(this.LABEL_DB_CONNECTION);
        this.lblLayerName.setText(this.LABEL_LAYER_NAME);
        this.lblGeomColumnName.setText(this.LABEL_GEOCOLUMN_NAME);
        this.btnGTypeDiscover.setText(this.BUTTON_DISCOVER);
        this.lblSRID.setText(this.LABEL_SRID);
        this.lblTolerance.setText(this.propertyManager.getMsg("lblTolerance"));
        this.lblPrecision.setText(this.propertyManager.getMsg("lblPrecision"));
        this.lblURX.setText("?");
        this.lblLLX.setText("?");
        this.lblURY.setText("?");
        this.lblLLY.setText("?");
        this.cbRecalculateMBR.setText(this.LABEL_RECALCULATE_MBR);

        this.lblLabelColumn.setText(this.LABEL_COLUMNS_PULLDOWN);
        this.lblRotationColumn.setText(this.LABEL_ROTATION_COLUMN);
        this.btnLabelStyling.setText(this.BUTTON_CHANGE);
        this.btnPosition.setText(this.BUTTON_CHANGE);
        this.rbDegrees.setText(this.LABEL_DEGREES);
        this.rbDegrees.setToolTipText(this.TT_DEGREES);
        this.rbRadians.setText(this.LABEL_RADIANS);
        this.rbRadians.setToolTipText(this.TT_RADIANS);
        this.lblRotationTarget.setText(this.LABEL_MARKER_ROTATION);
        this.lblRotationTarget.setToolTipText(this.TT_MARKER_ROTATION);
        this.cbOrientVertexLabel.setText(this.LABEL_MARK_ORIENTED);

        this.lblFetchSize.setText(LABEL_FETCH_SIZE);
        this.chkMinResolution.setText(this.LABEL_PIXEL_FILTERING);
        this.lblShadeTransLevel.setText(String.format(LABEL_TRANSPARENCY + "%3d", 0));
        this.fixShadeColorB.setText(this.BUTTON_SHADE_COLOR);
        this.columnShadeColorRB.setText(this.LABEL_COLUMN_COLOR);
        this.fixShadeColorRB.setText(this.LABEL_CONSTANT_COLOR);
        this.noShadeRB.setText(this.LABEL_NONE);
        this.lblFillTranspEnd.setText(this.LABEL_TRANSPARENT);
        this.lblFillSolidEnd.setText(this.LABEL_OPAQUE);
        this.lblLineWidth.setText(this.LABEL_LINE_WIDTH);
        this.strokeColorButton.setText(this.BUTTON_LINE_COLOR);

        this.lblVertexStart.setText(this.LABEL_MARK_START);
        this.lblVertexAll.setText(this.LABEL_MARK_ALL);

        this.pointColorButton.setText(this.BUTTON_POINT_COLOR);
        this.lblPointStyle.setText(this.LABEL_POINT_MARKER);
        this.btnSelectionColor.setText(this.BUTTON_SELECTION_COLOR);
        this.lblSelectionTransLevel.setText(String.format(LABEL_TRANSPARENCY + "%3d", 0));
        this.lblSelectionLineWidth.setText(this.LABEL_SELECT_LINE_WIDTH);
        this.lblSelectionPointSize.setText(this.LABEL_SELECT_POINT_SIZE);
        this.lblSelectionLineStyles.setText(this.LABEL_SELECT_LINE_STYLE);
        this.lblSelectionTranspEnd.setText(this.LABEL_TRANSPARENT);
        this.lblSelectionSolidEnd.setText(this.LABEL_OPAQUE);
        
        this.lblOffset.setText(this.LABEL_OFFSET);
        this.lblPosition.setText(this.LABEL_POSITION);
        this.lblGeometryLabelPoint.setText(this.GEOMETRY_LABEL_POSITION);
        this.pnlVertexlMarking.setBorder(javax.swing.BorderFactory.createTitledBorder(this.BORDER_LABEL_VERTICES));
        this.btnHelp.setText(this.BUTTON_HELP);
        this.btnClose.setText(this.BUTTON_CLOSE);
        
        this.btnPointApply.setText(this.BUTTON_APPLY);
        this.btnStrokeApply.setText(this.BUTTON_APPLY);
        this.btnFillApply.setText(this.BUTTON_APPLY);
        this.btnLabelApply.setText(this.BUTTON_APPLY);
        this.btnRotationApply.setText(this.BUTTON_APPLY);
        this.btnSQLApply.setText(this.BUTTON_APPLY);

    }

    @SuppressWarnings("unused")
	private void setLabelPosition(String _labelPosition) {
        for (int i = 0; i < cmbGeometryLabelPosition.getItemCount(); i++) {
            if (cmbGeometryLabelPosition.getItemAt(i).toString().toUpperCase().equals(_labelPosition.toUpperCase())) {
                cmbGeometryLabelPosition.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setRotationTarget(String _rotationTarget) {
        for (int i = 0; i < cmbRotationTarget.getItemCount(); i++) {
            if (cmbRotationTarget.getItemAt(i).toString().toUpperCase().equals(_rotationTarget.toUpperCase())) {
                cmbRotationTarget.setSelectedIndex(i);
                break;
            }
        }
    }

    public void initDialog(final SVSpatialLayer _layer) 
    {
        this.layer = _layer;
       // tpProperties.setSelectedIndex(0);   
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // We don't show certain fields when we are setting the properties of a graphic theme.
                    //
                    boolean notGraphicLayer = ! (_layer instanceof SVGraphicLayer);
                    lblDBConnection.setEnabled(notGraphicLayer);
                    cmbConnections.setEnabled(notGraphicLayer);
                    btnConnection.setEnabled(notGraphicLayer);
                    geoColumnTF.setEnabled(notGraphicLayer);
                    btnGTypeDiscover.setEnabled(notGraphicLayer);
                    // SQL Panel
                        lblFetchSize.setEnabled(notGraphicLayer && !(_layer instanceof SVQueryLayer));
                         fetchSizeTF.setEnabled(notGraphicLayer && !(_layer instanceof SVQueryLayer));
                    chkMinResolution.setEnabled(notGraphicLayer && !(_layer instanceof SVQueryLayer || _layer instanceof SVWorksheetLayer));
                         btnSQLApply.setEnabled(notGraphicLayer && !(_layer instanceof SVQueryLayer));
                               sqlTA.setEnabled(notGraphicLayer);
                         scrollSQLTA.setEnabled(notGraphicLayer);
                              pnlSQL.setEnabled(notGraphicLayer);
                    
                    // If not used or useable make them invisible.
                    //
                    lblDBConnection.setVisible(notGraphicLayer);
                    cmbConnections.setVisible(notGraphicLayer);
                    btnConnection.setVisible(notGraphicLayer);
                    btnGTypeDiscover.setVisible(notGraphicLayer);
                    // SQL Panel
                        lblFetchSize.setVisible(notGraphicLayer);
                         fetchSizeTF.setVisible(notGraphicLayer);
                    chkMinResolution.setVisible(notGraphicLayer && !(_layer instanceof SVQueryLayer || _layer instanceof SVWorksheetLayer));
                         btnSQLApply.setVisible(notGraphicLayer);
                               sqlTA.setVisible(notGraphicLayer);
                         scrollSQLTA.setVisible(notGraphicLayer);
                              pnlSQL.setVisible(notGraphicLayer);

                    // If SVQueryLayer we keep the relevant tab, otherwise we remove it
                    for (int i=0;i < pnlProperties.getTabCount(); i++ ) {
                        if ( pnlProperties.getTitleAt(i).equalsIgnoreCase("Query Layer") && ! ( _layer instanceof SVQueryLayer ) ) {
                            pnlProperties.remove(i);
                            break;
                        } 
                    }
                    for (int i=0;i < pnlProperties.getTabCount(); i++ ) {
                        if ( pnlProperties.getTitleAt(i).equalsIgnoreCase("SQL") && ( _layer instanceof SVGraphicLayer ) ) {
                            pnlProperties.remove(i);
                            break;
                        }
                    }

                    // Load open connections and check layer's connection
                    cmbConnections.setModel(DatabaseConnections.getInstance().getComboBoxModel(layer.getConnectionName()));
                    int selectionIndex = -1;
                    String currentDisplayName = layer.getConnectionDisplayName();
                    for ( int i=0; i<cmbConnections.getItemCount(); i++ ) {
                        DatabaseConnection dbConn = DatabaseConnections.getInstance().getConnectionAt(i);
                        if ( dbConn!=null && dbConn.getDisplayName().equalsIgnoreCase(currentDisplayName) ) {
                            selectionIndex = i;
                            break;
                        }
                    }
                    if ( selectionIndex==-1) {
                        // displayName was not in the list of open connections in the comboBox so add it
                        DefaultComboBoxModel<String> cm = (DefaultComboBoxModel<String>)cmbConnections.getModel();
                        cm.addElement(currentDisplayName);
                    }
                    cmbConnections.setSelectedItem(currentDisplayName);
                    
                    nameInputTF.setText(_layer.getVisibleName());
                    geoColumnTF.setText(_layer.getGeoColumn());
                    InputVerifier verifyGeoColumn = new InputVerifier() {
                        public boolean verify(JComponent comp) {
                            boolean returnValue = true;
                            JTextField textField = (JTextField)comp;
                            if (Strings.isEmpty(textField.getText().trim())) {
                                textField.setText(layer.getGeoColumn());
                                layer.getSpatialView().getSVPanel().setMessage(ERROR_GEOCOLUMN_NAME,
                                                                               true);
                                returnValue = false;
                            } else {
                                layer.getSpatialView().getSVPanel().setMessage(null,
                                                                               false);
                            }
                            return returnValue;
                        }
                    };
                    geoColumnTF.setInputVerifier(verifyGeoColumn);
                    lblToleranceValue.setText(formatOrd(_layer.getTolerance(),_layer.getPrecision(false)));
                    tfPrecisionValue.setText(String.valueOf(_layer.getPrecision(false)));
                    cmbSRIDType.setModel(Constants.getSRIDTypeCombo());
                    if (layer.getSRIDType() == Constants.SRID_TYPE.UNKNOWN) {
                        discoverSetSRIDType();
                    } else {
                        for (int i = 0; i < cmbSRIDType.getItemCount(); i++) {
                            if (cmbSRIDType.getItemAt(i).equals(layer.getSRIDType().toString())) {
                                cmbSRIDType.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    cmbSRIDType.setMaximumRowCount(cmbSRIDType.getItemCount());

                    cmbGTypes.setModel(Constants.getGeometryTypesCombo());

                    String geometryType = _layer.getGeometryType().toString();
                    for (int i = 0; i < cmbGTypes.getItemCount(); i++) {
                        if (cmbGTypes.getItemAt(i).toString().equals(geometryType)) {
                            cmbGTypes.setSelectedIndex(i);
                            break;
                        }
                    }
                    cmbGTypes.setMaximumRowCount(cmbGTypes.getItemCount());

                    sridTF.setText(((_layer.getSRID() != null) ?
                                    _layer.getSRID() : "NULL"));
                    // MBR
                    //
                    Envelope mbr = _layer.getMBR();
LOGGER.debug("SVSpatialLayerProps.InitDialog _layer.getMBR()="+mbr.toString() + " layer precision " + _layer.getPrecision(true) + " format.minX=" + formatOrd(mbr.minX, _layer.getPrecision(true)));
                    lblLLX.setText(formatOrd(mbr.minX, _layer.getPrecision(true)));
                    lblLLY.setText(formatOrd(mbr.minY, _layer.getPrecision(true)));
                    lblURX.setText(formatOrd(mbr.maxX, _layer.getPrecision(true)));
                    lblURY.setText(formatOrd(mbr.maxY, _layer.getPrecision(true)));
                    pnlMBR.setBackground(_layer.getSpatialView().getMapPanel().getMapBackground());
                    pnlMBR.setToolTipText(TT_LAYER_MBR);
                    if (notGraphicLayer) {
                         // result set fetch size
                        //
                        fetchSizeTF.setText(String.valueOf(_layer.getResultFetchSize()));
 
                        // Min_resolution Pixel filter
                        //
                        if ( _layer.getGeometryType().toString().toUpperCase().contains(Constants.GEOMETRY_TYPES.POINT.toString().toUpperCase()) ) {
                            chkMinResolution.setSelected(false);
                            chkMinResolution.setEnabled(false);
                        } else {
                            chkMinResolution.setSelected(_layer.getMinResolution());
                        }
                        
                        // SQL sentence
                        //
                        if ( _layer instanceof SVQueryLayer) {
                            sqlTA.setText(((SVQueryLayer)_layer).getSQL());
                        } else if ( _layer instanceof SVWorksheetLayer) {
                            sqlTA.setText(((SVWorksheetLayer)_layer).getSQL());
                        } else if ( _layer instanceof SVGraphicLayer) {
                            sqlTA.setText("");
                        } else /* SVSpatialLayer */ {
                            String initSQL = _layer.getInitSQL(_layer.getGeoColumn());
                            if (_layer.getSQL().equalsIgnoreCase(initSQL))
                                sqlTA.setText(initSQL);
                            else
                                sqlTA.setText(_layer.getSQL());
                        }
                    }

                    // visual properties
                    //

                    // Label
                    labelAttributes = _layer.getStyling().getLabelAttributes();
                    setLabelStyling((SimpleAttributeSet)labelAttributes);
                    labelPosition   = _layer.getStyling().getLabelPosition();
                    lblPosition.setText(propertyManager.getMsg("MARK_POSITION", labelPosition.toString()));
                    labelOffsetDistance     = _layer.getStyling().getLabelOffset();
                    lblOffset.setText(propertyManager.getMsg("MARK_OFFSET",String.valueOf(labelOffsetDistance)));
                    tfLoScale.setText(String.valueOf(_layer.getStyling().getTextLoScale()));
                    tfHiScale.setText(String.valueOf(_layer.getStyling().getTextHiScale()));

                    // Mark
                    markLabelAttributes = _layer.getStyling().getMarkLabelAttributes();
                    setMarkLabelStyling((SimpleAttributeSet)markLabelAttributes);
                    markLabelPosition   = _layer.getStyling().getMarkLabelPosition();
                    lblMarkPosition.setText(propertyManager.getMsg("MARK_POSITION", markLabelPosition.toString()));
                    markOffsetDistance     = _layer.getStyling().getMarkLabelOffset();
                    lblMarkOffset.setText(propertyManager.getMsg("MARK_OFFSET", String.valueOf(markOffsetDistance)));

                    // Geometry Label
                    //
                    cmbGeometryLabelPosition.setModel(Constants.getGeometryLabelPositionCombo());
                    String geometryLabelPoint = _layer.getStyling().getGeometryLabelPosition().toString();
                    for (int i = 0; i < cmbGeometryLabelPosition.getItemCount(); i++) {
                        if (cmbGeometryLabelPosition.getItemAt(i).toString().equals(geometryLabelPoint)) {
                            cmbGeometryLabelPosition.setSelectedIndex(i);
                            break;
                        }
                    }
                    cmbGeometryLabelPosition.setMaximumRowCount(5); 
                    
                    // Point
                    //
                    cmbPointColorColumns.setEnabled(false);
                    pointColorButton.setEnabled(false);
                    pointColorButton.setBackground(null);
                    if (_layer.getStyling().getPointColorType() == Styling.STYLING_TYPE.CONSTANT) {
                        rbPointColorSolid.setSelected(true);
                        pointColor = _layer.getStyling().getPointColor(null);
                        pointColorButton.setBackground(pointColor);
                        pointColorButton.setForeground(Colours.highContrast(pointColor));
                        pointColorButton.setEnabled(true);
                    } else if (_layer.getStyling().getPointColorType() == Styling.STYLING_TYPE.RANDOM) {
                        rbPointColorRandom.setSelected(true);
                    } else if (_layer.getStyling().getPointColorType() == Styling.STYLING_TYPE.COLUMN) {
                        rbPointColorColumn.setSelected(true);
                        cmbPointColorColumns.setEnabled(true);
                    }
                    
                    pointSizeTF.setText(String.valueOf(_layer.getStyling().getPointSize(4)));
                    sldrPointSize.setValue(_layer.getStyling().getPointSize(4));
                    
                    rbPointSizeFixed.setSelected(_layer.getStyling().getPointSizeType() == Styling.STYLING_TYPE.CONSTANT);
                    pointSizeTF.setEnabled(_layer.getStyling().getPointSizeType() == Styling.STYLING_TYPE.CONSTANT);
                    sldrPointSize.setEnabled(_layer.getStyling().getPointSizeType() == Styling.STYLING_TYPE.CONSTANT);
                    rbPointSizeColumn.setSelected(_layer.getStyling().getPointSizeType() == Styling.STYLING_TYPE.COLUMN);
                    cmbPointSizeColumns.setEnabled(_layer.getStyling().getPointSizeType() == Styling.STYLING_TYPE.COLUMN);
                    
                    cmbPointTypes.setModel(PointMarker.getComboBoxModel());
                    cmbPointTypes.setRenderer(PointMarker.getPointTypeRenderer());
                    String displayPointType = Strings.TitleCase(PointMarker.getMarkerTypeAsString(_layer.getStyling().getPointType()));
                    for (int i = 0; i < cmbPointTypes.getItemCount(); i++) {
                        if (cmbPointTypes.getItemAt(i).toString().equals(displayPointType)) {
                            cmbPointTypes.setSelectedIndex(i);
                            break;
                        }
                    }
                    cmbPointTypes.setMaximumRowCount(10); // cmbPointTypes.getItemCount());

                    // Line / Style
                    //
                    cmbStrokeColorColumns.setEnabled(false);
                    strokeColorButton.setEnabled(false);
                    strokeColorButton.setBackground(null);
                    if (_layer.getStyling().getLineColorType() == Styling.STYLING_TYPE.CONSTANT) {
                        rbStrokeColorSolid.setSelected(true);
                        lineColor = _layer.getStyling().getLineColor(null);
                        strokeColorButton.setBackground(lineColor);
                        strokeColorButton.setForeground(Colours.highContrast(lineColor));
                        strokeColorButton.setEnabled(true);
                    } else if (_layer.getStyling().getLineColorType() == Styling.STYLING_TYPE.RANDOM) {
                        rbStrokeColorRandom.setSelected(true);
                    } else if (_layer.getStyling().getLineColorType() == Styling.STYLING_TYPE.COLUMN) {
                        rbStrokeColorColumn.setSelected(true);
                        cmbStrokeColorColumns.setEnabled(true);
                    }

                    sldrStrokeTransLevel.setValue(100 - _layer.getStyling().getLineTransLevelAsPercent());
                    lblStrokeTransLevel.setText(String.format(LABEL_TRANSPARENCY + "%3d",
                                                              100 - _layer.getStyling().getLineTransLevelAsPercent()));
                    sldrStrokeTransLevel.setToolTipText(TT_VALUE_0_1);

                    lineWidthTF.setText(String.valueOf(_layer.getStyling().getLineWidth()));
                    sldrLineWidth.setValue(_layer.getStyling().getLineWidth());
                    lineColor = _layer.getStyling().getLineColor(null); 
                    strokeColorButton.setBackground(lineColor);
                    strokeColorButton.setForeground(Colours.highContrast(lineColor));
                    cmbLineStyles.setModel(LineStyle.getComboBoxModel());
                    cmbLineStyles.setRenderer(LineStyle.getLineStyleRenderer());
                    String displayLineStroke = Strings.TitleCase(LineStyle.getLineStyleAsString(_layer.getStyling().getLineStrokeType()));
                    for (int i = 0; i < cmbLineStyles.getItemCount(); i++) {
                        if (cmbLineStyles.getItemAt(i).toString().equals(displayLineStroke)) {
                            cmbLineStyles.setSelectedIndex(i);
                            break;
                        }
                    }
                    cmbMarkGeoStart.setModel(PointMarker.getComboBoxModel());
                    cmbMarkGeoStart.setRenderer(PointMarker.getPointTypeRenderer());
                    String vertexMarker = Strings.TitleCase(PointMarker.getMarkerTypeAsString(_layer.getStyling().getMarkGeoStart()));
                    for (int i = 0; i < cmbMarkGeoStart.getItemCount(); i++) {
                        if (cmbMarkGeoStart.getItemAt(i).toString().equals(vertexMarker)) {
                            cmbMarkGeoStart.setSelectedIndex(i);
                            break;
                        }
                    }
                    cmbMarkGeoPoints.setModel(PointMarker.getComboBoxModel());
                    cmbMarkGeoPoints.setRenderer(PointMarker.getPointTypeRenderer());
                    vertexMarker = Strings.TitleCase(PointMarker.getMarkerTypeAsString(_layer.getStyling().getMarkGeoPoints()));
                    for (int i = 0; i < cmbMarkGeoPoints.getItemCount(); i++) {
                        if (cmbMarkGeoPoints.getItemAt(i).toString().equals(vertexMarker)) {
                            cmbMarkGeoPoints.setSelectedIndex(i);
                            break;
                        }
                    }
                    
                    cmbMarkSegment.setModel(Constants.getSegmentLabelCombo());
                    cmbMarkSegment.setSelectedIndex(0);
                    String segmentMark = _layer.getStyling().getMarkSegment().toString();
                    for (int i = 0; i < cmbMarkSegment.getItemCount(); i++) {
                        if (cmbMarkSegment.getItemAt(i).toString().equals(segmentMark)) {
                            cmbMarkSegment.setSelectedIndex(i);
                            break;
                        }
                    }
                                        
                    cmbVertexLabelContent.setModel(Constants.getVertexLabelCombo());
                    String vertexLabel = Constants.getVertexLabelType(_layer.getStyling().getMarkVertex());
                    for (int i = 0; i < cmbVertexLabelContent.getItemCount(); i++) {
                        if (cmbVertexLabelContent.getItemAt(i).toString().equals(vertexLabel)) {
                            cmbVertexLabelContent.setSelectedIndex(i);
                            break;
                        }
                    }
                    
                    cmbSegmentArrows.setModel(Constants.getSegmentArrowsType());
                    cmbSegmentArrows.setSelectedIndex(0);
                    String segmentArrows = _layer.getStyling().getSegmentArrow().toString();
                    for (int i = 0; i < cmbSegmentArrows.getItemCount(); i++) {
                        if (cmbSegmentArrows.getItemAt(i).toString().replace(" ","_").toUpperCase().equals(segmentArrows)) {
                            cmbSegmentArrows.setSelectedIndex(i);
                            break;
                        }
                    }

                    cbOrientVertexLabel.setSelected(_layer.getStyling().isMarkOriented());
                    
                    // shade properties
                    //
                    cmbShadeColumns.setEnabled(false);
                    fixShadeColorB.setEnabled(false);
                    fixShadeColorB.setBackground(null);
                    if (_layer.getStyling().getShadeType() == Styling.STYLING_TYPE.NONE) {
                        noShadeRB.setSelected(true);
                    } else if (_layer.getStyling().getShadeType() == Styling.STYLING_TYPE.CONSTANT) {
                        fixShadeColorRB.setSelected(true);
                        shadeColor = _layer.getStyling().getShadeColor(Colours.getRGBa(Color.BLACK));
                        fixShadeColorB.setBackground(shadeColor);
                        fixShadeColorB.setForeground(Colours.highContrast(shadeColor));
                        fixShadeColorB.setEnabled(true);
                    } else if (_layer.getStyling().getShadeType() == Styling.STYLING_TYPE.RANDOM) {
                        randomShadeRB.setSelected(true);
                    } else if (_layer.getStyling().getShadeType() == Styling.STYLING_TYPE.COLUMN) {
                        columnShadeColorRB.setSelected(true);
                        cmbShadeColumns.setEnabled(true);
                    }

                    sldrShadeTransLevel.setValue(100 - _layer.getStyling().getShadeTransLevelAsPercent());
                    lblShadeTransLevel.setText(String.format(LABEL_TRANSPARENCY + "%3d",
                                                             100 - _layer.getStyling().getShadeTransLevelAsPercent()));
                    sldrShadeTransLevel.setToolTipText(TT_VALUE_0_1);

                    // selection properties
                    //
                    tfSelectionPointSize.setText(String.valueOf(_layer.getStyling().getSelectionPointSize()));
                    sldrSelectionPointSize.setValue(_layer.getStyling().getSelectionPointSize());
                    tfSelectionLineWidth.setText(String.valueOf(_layer.getStyling().getSelectionLineWidth()));
                    sldrSelectionLineWidth.setValue(_layer.getStyling().getSelectionLineWidth());
                    cmbSelectionLineStyles.setModel(LineStyle.getComboBoxModel());
                    cmbSelectionLineStyles.setRenderer(LineStyle.getLineStyleRenderer());
                    String displaySelectionLineStyle = Strings.TitleCase(LineStyle.getLineStyleAsString(_layer.getStyling().getLineStrokeType()));
                    for (int i = 0; i < cmbSelectionLineStyles.getItemCount(); i++) {
                        if (cmbSelectionLineStyles.getItemAt(i).toString().equals(displaySelectionLineStyle)) {
                            cmbSelectionLineStyles.setSelectedIndex(i);
                            break;
                        }
                    }
                    selectShadeColor = _layer.getStyling().getSelectionColor();
                    cbSelectionActive.setSelected(_layer.getStyling().isSelectionActive());
                    btnSelectionColor.setBackground(_layer.getStyling().getSelectionColor());
                    btnSelectionColor.setForeground(Colours.highContrast(_layer.getStyling().getSelectionColor()));
                    
                    sldrSelectionTransLevel.setValue(100 - _layer.getStyling().getSelectionShadeTransLevelAsPercent());
                    lblSelectionTransLevel.setText(String.format(LABEL_TRANSPARENCY + "%3d",
                                                                 100 - _layer.getStyling().getSelectionShadeTransLevelAsPercent()));
                    sldrSelectionTransLevel.setToolTipText(TT_VALUE_0_1);

                    // Label stuff
                    //
                    try {
                        if (cmbLabelColumns.getItemCount()       > 0) cmbLabelColumns.removeAllItems();
                        if (cmbRotationColumns.getItemCount()    > 0) cmbRotationColumns.removeAllItems();
                        if (cmbShadeColumns.getItemCount()       > 0) cmbShadeColumns.removeAllItems();
                        if (cmbPointColorColumns.getItemCount()  > 0) cmbPointColorColumns.removeAllItems();
                        if (cmbPointSizeColumns.getItemCount()   > 0) cmbPointSizeColumns.removeAllItems();
                        if (cmbStrokeColorColumns.getItemCount() > 0) cmbStrokeColorColumns.removeAllItems();

                        // By setting to disabled we ensure pulldown event does not fire when program populates
                        cmbLabelColumns.setEnabled(false);
                        cmbRotationColumns.setEnabled(false);
                        cmbShadeColumns.setEnabled(false);
                        cmbPointColorColumns.setEnabled(false);
                        cmbPointSizeColumns.setEnabled(false);
                        cmbStrokeColorColumns.setEnabled(false);

                        // Make sure always have the "no label" text
                        //
                        cmbLabelColumns.addItem(LABEL_NONE);
                        cmbRotationColumns.addItem(LABEL_NONE);
                        cmbShadeColumns.addItem(LABEL_NONE);
                        cmbPointColorColumns.addItem(LABEL_NONE);
                        cmbPointSizeColumns.addItem(LABEL_NONE);
                        cmbStrokeColorColumns.addItem(LABEL_NONE);

                        // Get labelColumnsAndTypes only if SQL filled in and not SVWorksheetLayer
                        //
                        if ( ! (Strings.isEmpty(sqlTA.getText()) && _layer instanceof SVWorksheetLayer ) ) {
                            labelColumnsAndTypes = _layer.getColumnsAndTypes(true, /* Only numbers strings etc */
                                                                             true) /* full DataType wanted */;
                            if (labelColumnsAndTypes != null &&
                                labelColumnsAndTypes.size() > 0) {
                                Iterator<String> it = labelColumnsAndTypes.keySet().iterator();
                                while (it.hasNext()) {
                                    String columnName = it.next();
                                    cmbLabelColumns.addItem(columnName); // First item/key is column name
                                    if (labelColumnsAndTypes.get(columnName).startsWith("NUMBER") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("INTEGER") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("SHORT") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("LONG") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("FLOAT") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("BINARY_DOUBLE") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("BINARY_FLOAT")) {
                                        cmbRotationColumns.addItem(columnName);
                                        cmbPointSizeColumns.addItem(columnName);
                                    }
                                    // Only string and integer columns allowed
                                    if (labelColumnsAndTypes.get(columnName).contains("CHAR") ||
                                        labelColumnsAndTypes.get(columnName).equals("STRING") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("INTEGER") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("SHORT") ||
                                        labelColumnsAndTypes.get(columnName).startsWith("LONG") ||
                                        (labelColumnsAndTypes.get(columnName).startsWith("NUMBER") &&
                                         labelColumnsAndTypes.get(columnName).indexOf(",") == -1)) {
                                        cmbShadeColumns.addItem(columnName);
                                        cmbPointColorColumns.addItem(columnName);
                                        cmbStrokeColorColumns.addItem(columnName);
                                    }
                                }
                            }

                            // Now set label columns comboBox to right column
                            //
                            cmbLabelColumns.setSelectedIndex(0);
                            if (!Strings.isEmpty(_layer.getStyling().getLabelColumn())) {
                                for (int i = 0;
                                    i < cmbLabelColumns.getItemCount(); i++) {
                                    if (cmbLabelColumns.getItemAt(i).toString().equals(_layer.getStyling().getLabelColumn())) {
                                        cmbLabelColumns.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            cmbLabelColumns.setMaximumRowCount(10);
                            cmbLabelColumns.setEnabled(true);

                            // Apply styling
                            setLabelStyling(_layer.getStyling().getLabelAttributes());
                            setMarkLabelStyling(_layer.getStyling().getMarkLabelAttributes());

                            // Now set rotation columns comboBox to right column
                            //
                            if (Strings.isEmpty(_layer.getStyling().getRotationColumn()))
                                cmbRotationColumns.setSelectedIndex(0);
                            else {
                                for (int i = 0;
                                     i < cmbRotationColumns.getItemCount();
                                     i++) {
                                    if (cmbRotationColumns.getItemAt(i).toString().equals(_layer.getStyling().getRotationColumn())) {
                                        cmbRotationColumns.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            cmbRotationColumns.setMaximumRowCount(10);
                            if (_layer.getStyling().getRotationValue() ==
                                Constants.ROTATION_VALUES.DEGREES)
                                rbDegrees.setSelected(true);
                            else
                                rbRadians.setSelected(true);
                            setRotationTarget(_layer.getStyling().getRotationTarget().toString());

                            if (Strings.isEmpty(_layer.getStyling().getPointSizeColumn()))
                                cmbPointSizeColumns.setSelectedIndex(0);
                            else {
                                for (int i = 0;
                                     i < cmbPointSizeColumns.getItemCount();
                                     i++) {
                                    if (cmbPointSizeColumns.getItemAt(i).toString().equals(_layer.getStyling().getPointSizeColumn())) {
                                        cmbPointSizeColumns.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            cmbPointSizeColumns.setMaximumRowCount(10);
                            cmbPointSizeColumns.setEnabled(rbPointSizeColumn.isSelected());

                            // Now set shade columns comboBox to right column
                            //
                            if (Strings.isEmpty(_layer.getStyling().getShadeColumn()))
                                cmbShadeColumns.setSelectedIndex(0);
                            else {
                                for (int i = 0;
                                     i < cmbShadeColumns.getItemCount(); i++) {
                                    if (cmbShadeColumns.getItemAt(i).toString().equals(_layer.getStyling().getShadeColumn())) {
                                        cmbShadeColumns.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            cmbShadeColumns.setMaximumRowCount(10);
                            cmbShadeColumns.setEnabled(columnShadeColorRB.isSelected());
                            
                            // Now set point and line color columns comboBox to right column
                            //
                            if (Strings.isEmpty(_layer.getStyling().getPointColorColumn()))
                                cmbPointColorColumns.setSelectedIndex(0);
                            else {
                                for (int i = 0;
                                     i < cmbPointColorColumns.getItemCount(); i++) {
                                    if (cmbPointColorColumns.getItemAt(i).toString().equals(_layer.getStyling().getPointColorColumn())) {
                                        cmbPointColorColumns.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            cmbPointColorColumns.setMaximumRowCount(10);
                            cmbPointColorColumns.setEnabled(rbPointColorColumn.isSelected());

                            if (Strings.isEmpty(_layer.getStyling().getLineColorColumn()))
                                cmbStrokeColorColumns.setSelectedIndex(0);
                            else {
                                for (int i = 0;
                                     i < cmbStrokeColorColumns.getItemCount(); i++) {
                                    if (cmbStrokeColorColumns.getItemAt(i).toString().equals(_layer.getStyling().getLineColorColumn())) {
                                        cmbStrokeColorColumns.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            cmbStrokeColorColumns.setMaximumRowCount(10);
                            cmbStrokeColorColumns.setEnabled(rbStrokeColorColumn.isSelected());
                          
                        }
                        
                        cmbRotationColumns.setEnabled(cmbRotationColumns.getItemCount() > 1);
                        for (int i = 0; i < cmbRotationTarget.getItemCount();
                             i++) {
                            if (cmbRotationTarget.getItemAt(i).toString().toUpperCase().equals(_layer.getStyling().getRotationTarget().toString().toUpperCase())) {
                                cmbRotationTarget.setSelectedIndex(i);
                                break;
                            }
                        }
                        // can we set the right tab to the foreground based on geometry type?
                        switch ( _layer.getGeometryType() ) {
                        case POINT        :                       
                        case MULTIPOINT   : pnlProperties.setSelectedComponent(pnlPoint);
                                            break;
                        case LINE         :
                        case MULTILINE    : pnlProperties.setSelectedComponent(pnlStroke);
                                            break;
                        case SOLID        :
                        case MULTISOLID   :
                        case POLYGON      : 
                        case MULTIPOLYGON : 
                        case COLLECTION   : pnlProperties.setSelectedComponent(pnlFill);
                                            break;
                        case UNKNOWN      : pnlProperties.setSelectedComponent(pnlPoint);
                        default           : break;
                        }

                        if (_layer instanceof SVQueryLayer) {
                            SVQueryLayer qLayer = (SVQueryLayer)_layer;
                            tfQueryBufferDistance.setText(String.valueOf(qLayer.getBufferDistance()));
                            cbQueryBufferGeometry.setSelected(qLayer.isBuffered());
                            cbQueryShowQueryGeometry.setSelected(qLayer.isDrawQueryGeometry());
                            cmbSdoOperators.setModel(Constants.getSdoOperators());
                            listRelateMasks.setEnabled(false);
                            String sdoOperator = "SDO_" + qLayer.getSdoOperator().toString().toUpperCase();
                            for (int i = 0; i < cmbSdoOperators.getItemCount(); i++) {
                                if (cmbSdoOperators.getItemAt(i).toString().equals(sdoOperator)) {
                                    cmbSdoOperators.setSelectedIndex(i);
                                    if (sdoOperator.equals("SDO_"+Constants.SDO_OPERATORS.RELATE.toString())) {
                                        listRelateMasks.setEnabled(true);
                                    }
                                    break;
                                }
                            }
                            if ( listRelateMasks.isEnabled() ) {
                                listRelateMasks.setModel(Constants.getRelateMasks());
                                listRelateMasks.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                                listRelateMasks.setVisibleRowCount(Constants.SDO_OPERATORS.values().length);
                                String masks = qLayer.getRelationshipMask();
                                // Need to set the list of relate masks
                                if (Strings.isEmpty(qLayer.getRelationshipMask()) ) {
                                    masks = Constants.SDO_OPERATORS.ANYINTERACT.toString();
                                }
                                for (int i = 0; i < listRelateMasks.getModel().getSize(); i++) {
                                    if (masks.indexOf(listRelateMasks.getModel().getElementAt(i).toString())>0 ) {
                                        listRelateMasks.setSelectedIndex(i);
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                    }
                    if ( _layer instanceof SVWorksheetLayer ) {
                        pnlProperties.setSelectedIndex(pnlProperties.indexOfTab("SQL"));
                    }
                    // Now set new size
                    pack();
                }
            });
    }

    // ########################## FORM EVENTS ########################################

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgFillColor = new javax.swing.ButtonGroup();
        setBgArrows(new javax.swing.ButtonGroup());
        bgRotation = new javax.swing.ButtonGroup();
        bgPointColor = new javax.swing.ButtonGroup();
        bgPointSize = new javax.swing.ButtonGroup();
        setBgMarkId(new javax.swing.ButtonGroup());
        bgStrokeColor = new javax.swing.ButtonGroup();
        pnlDetails = new javax.swing.JPanel();
        lblDBConnection = new javax.swing.JLabel();
        lblLayerName = new javax.swing.JLabel();
        lblGeomColumnName = new javax.swing.JLabel();
        nameInputTF = new javax.swing.JTextField();
        geoColumnTF = new javax.swing.JTextField();
        lblSRID = new javax.swing.JLabel();
        sridTF = new javax.swing.JTextField();
        cmbGTypes = new javax.swing.JComboBox<String>();
        lblGType = new javax.swing.JLabel();
        btnGTypeDiscover = new javax.swing.JButton();
        cmbSRIDType = new javax.swing.JComboBox<String>();
        btnDiscoverSRIDType = new javax.swing.JButton();
        lblTolerance = new javax.swing.JLabel();
        lblToleranceValue = new javax.swing.JLabel();
        lblSRIDType = new javax.swing.JLabel();
        tfPrecisionValue = new javax.swing.JTextField();
        cmbConnections = new javax.swing.JComboBox<String>();
        btnConnection = new javax.swing.JButton();
        lblPrecision = new javax.swing.JLabel();
        pnlProperties = new javax.swing.JTabbedPane();
        pnlSQL = new javax.swing.JPanel();
        scrollSQLTA = new javax.swing.JScrollPane();
        sqlTA = new javax.swing.JTextArea();
        lblFetchSize = new javax.swing.JLabel();
        fetchSizeTF = new javax.swing.JTextField();
        chkMinResolution = new javax.swing.JCheckBox();
        btnSQLApply = new javax.swing.JButton();
        btnCopy2Clipboard = new javax.swing.JButton();
        pnlPoint = new javax.swing.JPanel();
        btnPointApply = new javax.swing.JButton();
        pnlPointColor = new javax.swing.JPanel();
        cmbPointColorColumns = new javax.swing.JComboBox<String>();
        rbPointColorRandom = new javax.swing.JRadioButton();
        pointColorButton = new javax.swing.JButton();
        rbPointColorColumn = new javax.swing.JRadioButton();
        rbPointColorSolid = new javax.swing.JRadioButton();
        lblPointStyle = new javax.swing.JLabel();
        cmbPointTypes = new javax.swing.JComboBox<String>();
        pnlPointSize = new javax.swing.JPanel();
        pointSizeTF = new javax.swing.JTextField();
        rbPointSizeFixed = new javax.swing.JRadioButton();
        sldrPointSize = new javax.swing.JSlider();
        rbPointSizeColumn = new javax.swing.JRadioButton();
        cmbPointSizeColumns = new javax.swing.JComboBox<String>();
        pnlStroke = new javax.swing.JPanel();
        pnlMark = new javax.swing.JPanel();
        lblArrowPlacement = new javax.swing.JLabel();
        cmbSegmentArrows = new javax.swing.JComboBox<String>();
        cmbMarkGeoPoints = new javax.swing.JComboBox<String>();
        lblVertexAll = new javax.swing.JLabel();
        lblVertexStart = new javax.swing.JLabel();
        cmbMarkGeoStart = new javax.swing.JComboBox<String>();
        pnlStrokeSizeStyle = new javax.swing.JPanel();
        lblLineWidth = new javax.swing.JLabel();
        lblLineStyles = new javax.swing.JLabel();
        cmbLineStyles = new javax.swing.JComboBox<String>();
        sldrLineWidth = new javax.swing.JSlider();
        lineWidthTF = new javax.swing.JTextField();
        btnStrokeApply = new javax.swing.JButton();
        pnlStrokeColor = new javax.swing.JPanel();
        strokeColorButton = new javax.swing.JButton();
        rbStrokeColorSolid = new javax.swing.JRadioButton();
        rbStrokeColorRandom = new javax.swing.JRadioButton();
        rbStrokeColorColumn = new javax.swing.JRadioButton();
        cmbStrokeColorColumns = new javax.swing.JComboBox<String>();
        rbStrokeColorNone = new javax.swing.JRadioButton();
        pnlStrokeTransparency = new javax.swing.JPanel();
        lblStrokeTransLevel = new javax.swing.JLabel();
        sldrStrokeTransLevel = new javax.swing.JSlider();
        lblStrokeTranspEnd = new javax.swing.JLabel();
        lblStrokeSolid = new javax.swing.JLabel();
        pnlFill = new javax.swing.JPanel();
        btnFillApply = new javax.swing.JButton();
        pnlFillColor = new javax.swing.JPanel();
        noShadeRB = new javax.swing.JRadioButton();
        randomShadeRB = new javax.swing.JRadioButton();
        cmbShadeColumns = new javax.swing.JComboBox<String>();
        fixShadeColorRB = new javax.swing.JRadioButton();
        fixShadeColorB = new javax.swing.JButton();
        columnShadeColorRB = new javax.swing.JRadioButton();
        pnlFillTransparency = new javax.swing.JPanel();
        pnlFillTransparencyInner = new javax.swing.JPanel();
        lblFillSolidEnd = new javax.swing.JLabel();
        lblFillTranspEnd = new javax.swing.JLabel();
        sldrShadeTransLevel = new javax.swing.JSlider();
        lblShadeTransLevel = new javax.swing.JLabel();
        pnlLabel = new javax.swing.JPanel();
        pnlVertexlMarking = new javax.swing.JPanel();
        pnlMarkStyle = new javax.swing.JPanel();
        cbOrientVertexLabel = new javax.swing.JCheckBox();
        cmbVertexLabelContent = new javax.swing.JComboBox<String>();
        lblVertexLabelContent = new javax.swing.JLabel();
        cmbMarkSegment = new javax.swing.JComboBox<String>();
        lblMarkSegment = new javax.swing.JLabel();
        pnlMarkPositionOffset = new javax.swing.JPanel();
        lblMarkOffset = new javax.swing.JLabel();
        btnMarkLabelPosition = new javax.swing.JButton();
        lblMarkPosition = new javax.swing.JLabel();
        lblStyledMarkLabel = new javax.swing.JLabel();
        btnMarkLabelStyling = new javax.swing.JButton();
        pnlLabelGeometry = new javax.swing.JPanel();
        pnlLabelPosition = new javax.swing.JPanel();
        btnPosition = new javax.swing.JButton();
        lblOffset = new javax.swing.JLabel();
        lblPosition = new javax.swing.JLabel();
        pnlLabelStyle = new javax.swing.JPanel();
        btnLabelStyling = new javax.swing.JButton();
        lblStyledLabel = new javax.swing.JLabel();
        cmbLabelColumns = new javax.swing.JComboBox<String>();
        lblLabelColumn = new javax.swing.JLabel();
        lblGeometryLabelPoint = new javax.swing.JLabel();
        cmbGeometryLabelPosition = new javax.swing.JComboBox<String>();
        pnlDisplayScale = new javax.swing.JPanel();
        lblHiScale = new javax.swing.JLabel();
        tfHiScale = new javax.swing.JTextField();
        tfLoScale = new javax.swing.JTextField();
        lblLoScale = new javax.swing.JLabel();
        btnLabelApply = new javax.swing.JButton();
        pnlRotation = new javax.swing.JPanel();
        lblRotationTarget = new javax.swing.JLabel();
        rbDegrees = new javax.swing.JRadioButton();
        cmbRotationColumns = new javax.swing.JComboBox<String>();
        cmbRotationTarget = new javax.swing.JComboBox<String>();
        lblRotationColumn = new javax.swing.JLabel();
        rbRadians = new javax.swing.JRadioButton();
        btnRotationApply = new javax.swing.JButton();
        pnlQueryLayer = new javax.swing.JPanel();
        lblQueryBufferDistance = new javax.swing.JLabel();
        tfQueryBufferDistance = new javax.swing.JTextField();
        cbQueryShowQueryGeometry = new javax.swing.JCheckBox();
        cbQueryBufferGeometry = new javax.swing.JCheckBox();
        btnQueryApply = new javax.swing.JButton();
        cmbSdoOperators = new javax.swing.JComboBox<String>();
        scrpQueryRelateMasks = new javax.swing.JScrollPane();
        listRelateMasks = new javax.swing.JList<String>();
        lblSdoOperator = new javax.swing.JLabel();
        lblRelateMasks = new javax.swing.JLabel();
        btnCopyQueryGeometry = new javax.swing.JButton();
        pnlSelection = new javax.swing.JPanel();
        tfSelectionPointSize = new javax.swing.JTextField();
        lblSelectionLineWidth = new javax.swing.JLabel();
        lblSelectionPointSize = new javax.swing.JLabel();
        tfSelectionLineWidth = new javax.swing.JTextField();
        pnlSelectionTransparency = new javax.swing.JPanel();
        lblSelectionTranspEnd = new javax.swing.JLabel();
        lblSelectionSolidEnd = new javax.swing.JLabel();
        sldrSelectionTransLevel = new javax.swing.JSlider();
        lblSelectionTransLevel = new javax.swing.JLabel();
        cbSelectionActive = new javax.swing.JCheckBox();
        btnSelectionColor = new javax.swing.JButton();
        sldrSelectionPointSize = new javax.swing.JSlider();
        sldrSelectionLineWidth = new javax.swing.JSlider();
        cmbSelectionLineStyles = new javax.swing.JComboBox<String>();
        lblSelectionLineStyles = new javax.swing.JLabel();
        btnSelectionApply = new javax.swing.JButton();
        btnHelp = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        pnlLayerExtent = new javax.swing.JPanel();
        pnlMBR = new javax.swing.JPanel();
        lblLLX = new javax.swing.JLabel();
        lblLLY = new javax.swing.JLabel();
        lblURX = new javax.swing.JLabel();
        lblURY = new javax.swing.JLabel();
        tfMBRFiller = new javax.swing.JTextField();
        cbRecalculateMBR = new javax.swing.JCheckBox();
        btnClipboard = new javax.swing.JButton();

        setTitle("Layer Properties");
        setAlwaysOnTop(true);
        setBackground(java.awt.Color.lightGray);
        setMinimumSize(new java.awt.Dimension(780, 630));

        pnlDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Details"));
        pnlDetails.setMaximumSize(null);
        pnlDetails.setMinimumSize(new java.awt.Dimension(418, 225));
        pnlDetails.setPreferredSize(new java.awt.Dimension(418, 225));

        lblDBConnection.setText("DB Connection:");

        lblLayerName.setLabelFor(nameInputTF);
        lblLayerName.setText("Name:");

        lblGeomColumnName.setLabelFor(geoColumnTF);
        lblGeomColumnName.setText("Geometry column:");

        nameInputTF.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        nameInputTF.setMaximumSize(new java.awt.Dimension(270, 17));
        nameInputTF.setMinimumSize(new java.awt.Dimension(270, 17));
        nameInputTF.setPreferredSize(new java.awt.Dimension(270, 17));
        nameInputTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameInputTFActionPerformed(evt);
            }
        });

        geoColumnTF.setEditable(false);
        geoColumnTF.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        geoColumnTF.setText("TheLargestColumnSizeIs30Chars");
        geoColumnTF.setMaximumSize(new java.awt.Dimension(270, 17));
        geoColumnTF.setMinimumSize(new java.awt.Dimension(270, 17));
        geoColumnTF.setPreferredSize(new java.awt.Dimension(270, 17));

        lblSRID.setLabelFor(sridTF);
        lblSRID.setText("SRID:");

        sridTF.setEditable(false);
        sridTF.setText("NULL");
        sridTF.setBorder(null);
        sridTF.setMaximumSize(new java.awt.Dimension(70, 17));
        sridTF.setMinimumSize(new java.awt.Dimension(70, 17));
        sridTF.setPreferredSize(new java.awt.Dimension(70, 17));

        cmbGTypes.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "COLLECTION", "CIRCLE", "POINT", "MULTIPOINT", "LINE", "MULTILINE", "RECTANGLE", "POLYGON", "MULTIPOLYGON", "SOLID", "MULTISOLID" }));
        cmbGTypes.setMaximumSize(new java.awt.Dimension(186, 22));
        cmbGTypes.setMinimumSize(new java.awt.Dimension(186, 22));
        cmbGTypes.setPreferredSize(new java.awt.Dimension(186, 22));
        cmbGTypes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbGTypesActionPerformed(evt);
            }
        });

        lblGType.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblGType.setLabelFor(cmbGTypes);
        lblGType.setText("Geometry Type:");

        btnGTypeDiscover.setMaximumSize(new java.awt.Dimension(83, 22));
        btnGTypeDiscover.setMinimumSize(new java.awt.Dimension(83, 22));
        btnGTypeDiscover.setPreferredSize(new java.awt.Dimension(83, 22));
        btnGTypeDiscover.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGTypeDiscoverActionPerformed(evt);
            }
        });

        cmbSRIDType.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "COMPOUND", "ENGINEERING", "GEODETIC_COMPOUND", "GEODETIC_GEOCENTRIC", "GEODETIC_GEOGRAPHIC2D", "GEODETIC_GEOGRAPHIC3D", "GEOGRAPHIC2D", "PROJECTED", "UNKNOWN" }));
        cmbSRIDType.setSelectedIndex(5);
        cmbSRIDType.setMaximumSize(new java.awt.Dimension(186, 22));
        cmbSRIDType.setMinimumSize(new java.awt.Dimension(186, 22));
        cmbSRIDType.setPreferredSize(new java.awt.Dimension(186, 22));

        btnDiscoverSRIDType.setText("Discover");
        btnDiscoverSRIDType.setMaximumSize(new java.awt.Dimension(83, 22));
        btnDiscoverSRIDType.setMinimumSize(new java.awt.Dimension(83, 22));
        btnDiscoverSRIDType.setPreferredSize(new java.awt.Dimension(83, 22));
        btnDiscoverSRIDType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDiscoverSRIDTypeActionPerformed(evt);
            }
        });

        lblTolerance.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblTolerance.setText("Tolerance:");

        lblToleranceValue.setText("0.005");
        lblToleranceValue.setMaximumSize(new java.awt.Dimension(70, 17));
        lblToleranceValue.setMinimumSize(new java.awt.Dimension(70, 17));
        lblToleranceValue.setPreferredSize(new java.awt.Dimension(70, 17));

        lblSRIDType.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblSRIDType.setText("SRID Type:");

        tfPrecisionValue.setText("6");
        tfPrecisionValue.setMaximumSize(new java.awt.Dimension(26, 17));
        tfPrecisionValue.setMinimumSize(new java.awt.Dimension(26, 17));
        tfPrecisionValue.setPreferredSize(new java.awt.Dimension(26, 17));

        cmbConnections.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "GIS@GIS11GR2" }));

        btnConnection.setText("Refresh");
        btnConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectionActionPerformed(evt);
            }
        });

        lblPrecision.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblPrecision.setText("Ordinate Precision:");

        javax.swing.GroupLayout pnlDetailsLayout = new javax.swing.GroupLayout(pnlDetails);
        pnlDetails.setLayout(pnlDetailsLayout);
        pnlDetailsLayout.setHorizontalGroup(
            pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailsLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblGType, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSRIDType, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSRID, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblTolerance, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblGeomColumnName, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblLayerName, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblDBConnection, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(geoColumnTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlDetailsLayout.createSequentialGroup()
                            .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(cmbSRIDType, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(cmbGTypes, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(18, 18, 18)
                            .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(btnGTypeDiscover, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnDiscoverSRIDType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addComponent(nameInputTF, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(pnlDetailsLayout.createSequentialGroup()
                            .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(pnlDetailsLayout.createSequentialGroup()
                                    .addComponent(lblToleranceValue, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(lblPrecision)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(tfPrecisionValue, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(pnlDetailsLayout.createSequentialGroup()
                                    .addComponent(cmbConnections, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnConnection)))
                            .addGap(0, 0, Short.MAX_VALUE)))
                    .addComponent(sridTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18))
        );
        pnlDetailsLayout.setVerticalGroup(
            pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailsLayout.createSequentialGroup()
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cmbConnections, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnConnection))
                    .addComponent(lblDBConnection))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nameInputTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLayerName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(geoColumnTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblGeomColumnName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTolerance)
                    .addComponent(lblToleranceValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPrecision)
                    .addComponent(tfPrecisionValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSRID)
                    .addComponent(sridTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3)
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cmbSRIDType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDiscoverSRIDType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSRIDType))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblGType)
                    .addComponent(cmbGTypes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGTypeDiscover, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pnlProperties.setBorder(javax.swing.BorderFactory.createTitledBorder("Properties"));
        pnlProperties.setMaximumSize(null);
        pnlProperties.setMinimumSize(new java.awt.Dimension(710, 348));
        pnlProperties.setOpaque(true);
        pnlProperties.setPreferredSize(new java.awt.Dimension(710, 348));

        pnlSQL.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlSQL.setMaximumSize(null);
        pnlSQL.setMinimumSize(new java.awt.Dimension(664, 219));
        pnlSQL.setPreferredSize(new java.awt.Dimension(664, 219));

        scrollSQLTA.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollSQLTA.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollSQLTA.setAutoscrolls(true);
        scrollSQLTA.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        scrollSQLTA.setMaximumSize(null);
        scrollSQLTA.setMinimumSize(null);
        scrollSQLTA.setPreferredSize(new java.awt.Dimension(673, 244));

        sqlTA.setColumns(80);
        sqlTA.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        sqlTA.setLineWrap(true);
        sqlTA.setRows(30);
        sqlTA.setTabSize(4);
        sqlTA.setToolTipText("Check Help for information on how to edit the SQL");
        sqlTA.setWrapStyleWord(true);
        sqlTA.setMaximumSize(null);
        sqlTA.setPreferredSize(new java.awt.Dimension(690, 1150));
        sqlTA.setSelectionColor(new java.awt.Color(51, 51, 255));
        sqlTA.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                sqlTAFocusLost(evt);
            }
        });
        scrollSQLTA.setViewportView(sqlTA);

        lblFetchSize.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblFetchSize.setLabelFor(fetchSizeTF);
        lblFetchSize.setText("Fetch size:");
        lblFetchSize.setMaximumSize(null);
        lblFetchSize.setMinimumSize(null);
        lblFetchSize.setPreferredSize(new java.awt.Dimension(52, 23));

        fetchSizeTF.setMaximumSize(null);
        fetchSizeTF.setMinimumSize(null);
        fetchSizeTF.setPreferredSize(new java.awt.Dimension(57, 23));

        chkMinResolution.setText("Pixel Filtering:");
        chkMinResolution.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        chkMinResolution.setMaximumSize(null);
        chkMinResolution.setMinimumSize(null);
        chkMinResolution.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkMinResolutionActionPerformed(evt);
            }
        });

        btnSQLApply.setText("Apply");
        btnSQLApply.setMaximumSize(null);
        btnSQLApply.setMinimumSize(null);
        btnSQLApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSQLApplyActionPerformed(evt);
            }
        });

        btnCopy2Clipboard.setText("Copy To Clipboard");
        btnCopy2Clipboard.setMaximumSize(null);
        btnCopy2Clipboard.setMinimumSize(null);
        btnCopy2Clipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCopy2ClipboardActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlSQLLayout = new javax.swing.GroupLayout(pnlSQL);
        pnlSQL.setLayout(pnlSQLLayout);
        pnlSQLLayout.setHorizontalGroup(
            pnlSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSQLLayout.createSequentialGroup()
                .addComponent(btnCopy2Clipboard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(chkMinResolution, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblFetchSize, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fetchSizeTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(216, 216, 216)
                .addComponent(btnSQLApply, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(scrollSQLTA, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pnlSQLLayout.setVerticalGroup(
            pnlSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlSQLLayout.createSequentialGroup()
                .addComponent(scrollSQLTA, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnCopy2Clipboard, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
                    .addGroup(pnlSQLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblFetchSize, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fetchSizeTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(chkMinResolution, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSQLApply, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );

        pnlProperties.addTab("SQL", pnlSQL);
        pnlSQL.getAccessibleContext().setAccessibleParent(pnlProperties);

        pnlPoint.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlPoint.setMaximumSize(null);
        pnlPoint.setMinimumSize(new java.awt.Dimension(623, 300));
        pnlPoint.setPreferredSize(new java.awt.Dimension(623, 300));

        btnPointApply.setText("Apply");
        btnPointApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPointApplyActionPerformed(evt);
            }
        });

        pnlPointColor.setBorder(javax.swing.BorderFactory.createTitledBorder("Color and Styling"));
        pnlPointColor.setMaximumSize(null);
        pnlPointColor.setMinimumSize(new java.awt.Dimension(673, 140));
        pnlPointColor.setPreferredSize(new java.awt.Dimension(673, 140));

        cmbPointColorColumns.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "TheLargestColumnSizeIs30Chars" }));
        cmbPointColorColumns.setEnabled(false);
        cmbPointColorColumns.setMaximumSize(new java.awt.Dimension(253, 20));
        cmbPointColorColumns.setMinimumSize(new java.awt.Dimension(253, 20));
        cmbPointColorColumns.setPreferredSize(new java.awt.Dimension(253, 20));

        bgPointColor.add(rbPointColorRandom);
        rbPointColorRandom.setText("Random");
        rbPointColorRandom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbPointColorRandomActionPerformed(evt);
            }
        });

        pointColorButton.setText("Color");
        pointColorButton.setMaximumSize(new java.awt.Dimension(73, 23));
        pointColorButton.setMinimumSize(new java.awt.Dimension(73, 23));
        pointColorButton.setPreferredSize(new java.awt.Dimension(73, 23));
        pointColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pointColorButtonActionPerformed(evt);
            }
        });

        bgPointColor.add(rbPointColorColumn);
        rbPointColorColumn.setText("Column");
        rbPointColorColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbPointColorColumnActionPerformed(evt);
            }
        });

        bgPointColor.add(rbPointColorSolid);
        rbPointColorSolid.setText("Solid");
        rbPointColorSolid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbPointColorSolidActionPerformed(evt);
            }
        });

        lblPointStyle.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblPointStyle.setText("Marker:");
        lblPointStyle.setMaximumSize(new java.awt.Dimension(45, 14));
        lblPointStyle.setMinimumSize(new java.awt.Dimension(45, 14));
        lblPointStyle.setPreferredSize(new java.awt.Dimension(45, 14));

        cmbPointTypes.setMaximumSize(new java.awt.Dimension(143, 20));
        cmbPointTypes.setMinimumSize(new java.awt.Dimension(143, 20));
        cmbPointTypes.setPreferredSize(new java.awt.Dimension(143, 20));

        javax.swing.GroupLayout pnlPointColorLayout = new javax.swing.GroupLayout(pnlPointColor);
        pnlPointColor.setLayout(pnlPointColorLayout);
        pnlPointColorLayout.setHorizontalGroup(
            pnlPointColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPointColorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlPointColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlPointColorLayout.createSequentialGroup()
                        .addComponent(lblPointStyle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cmbPointTypes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlPointColorLayout.createSequentialGroup()
                        .addComponent(rbPointColorRandom)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rbPointColorSolid)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pointColorButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlPointColorLayout.createSequentialGroup()
                        .addComponent(rbPointColorColumn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbPointColorColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlPointColorLayout.setVerticalGroup(
            pnlPointColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPointColorLayout.createSequentialGroup()
                .addGroup(pnlPointColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbPointColorSolid)
                    .addComponent(rbPointColorRandom)
                    .addComponent(pointColorButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlPointColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbPointColorColumn)
                    .addComponent(cmbPointColorColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlPointColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbPointTypes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPointStyle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(48, Short.MAX_VALUE))
        );

        pnlPointSize.setBorder(javax.swing.BorderFactory.createTitledBorder("Size"));
        pnlPointSize.setMaximumSize(null);
        pnlPointSize.setPreferredSize(new java.awt.Dimension(373, 96));

        pointSizeTF.setMaximumSize(new java.awt.Dimension(52, 20));
        pointSizeTF.setMinimumSize(new java.awt.Dimension(52, 20));
        pointSizeTF.setPreferredSize(new java.awt.Dimension(52, 20));
        pointSizeTF.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                pointSizeTFFocusLost(evt);
            }
        });

        bgPointSize.add(rbPointSizeFixed);
        rbPointSizeFixed.setText("Fixed");
        rbPointSizeFixed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbPointSizeFixedActionPerformed(evt);
            }
        });

        sldrPointSize.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        sldrPointSize.setMajorTickSpacing(8);
        sldrPointSize.setMaximum(72);
        sldrPointSize.setMinimum(4);
        sldrPointSize.setMinorTickSpacing(1);
        sldrPointSize.setPaintLabels(true);
        sldrPointSize.setPaintTicks(true);
        sldrPointSize.setValue(4);
        sldrPointSize.setMaximumSize(new java.awt.Dimension(186, 40));
        sldrPointSize.setMinimumSize(new java.awt.Dimension(186, 40));
        sldrPointSize.setPreferredSize(new java.awt.Dimension(186, 40));
        sldrPointSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrPointSizeStateChanged(evt);
            }
        });

        bgPointSize.add(rbPointSizeColumn);
        rbPointSizeColumn.setText("Column");
        rbPointSizeColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbPointSizeColumnActionPerformed(evt);
            }
        });

        cmbPointSizeColumns.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "TheLargestColumnSizeIs30Chars" }));
        cmbPointSizeColumns.setEnabled(false);
        cmbPointSizeColumns.setMaximumSize(new java.awt.Dimension(253, 20));
        cmbPointSizeColumns.setMinimumSize(new java.awt.Dimension(253, 20));
        cmbPointSizeColumns.setPreferredSize(new java.awt.Dimension(253, 20));

        javax.swing.GroupLayout pnlPointSizeLayout = new javax.swing.GroupLayout(pnlPointSize);
        pnlPointSize.setLayout(pnlPointSizeLayout);
        pnlPointSizeLayout.setHorizontalGroup(
            pnlPointSizeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPointSizeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlPointSizeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlPointSizeLayout.createSequentialGroup()
                        .addComponent(rbPointSizeColumn)
                        .addGap(31, 31, 31)
                        .addComponent(cmbPointSizeColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlPointSizeLayout.createSequentialGroup()
                        .addComponent(rbPointSizeFixed)
                        .addGap(37, 37, 37)
                        .addComponent(pointSizeTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sldrPointSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        pnlPointSizeLayout.setVerticalGroup(
            pnlPointSizeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPointSizeLayout.createSequentialGroup()
                .addGroup(pnlPointSizeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbPointSizeFixed)
                    .addComponent(pointSizeTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sldrPointSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlPointSizeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbPointSizeColumn)
                    .addComponent(cmbPointSizeColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout pnlPointLayout = new javax.swing.GroupLayout(pnlPoint);
        pnlPoint.setLayout(pnlPointLayout);
        pnlPointLayout.setHorizontalGroup(
            pnlPointLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPointLayout.createSequentialGroup()
                .addGroup(pnlPointLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnlPointColor, javax.swing.GroupLayout.PREFERRED_SIZE, 396, Short.MAX_VALUE)
                    .addComponent(pnlPointSize, javax.swing.GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE))
                .addGap(218, 218, 218)
                .addComponent(btnPointApply))
        );
        pnlPointLayout.setVerticalGroup(
            pnlPointLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPointLayout.createSequentialGroup()
                .addComponent(pnlPointSize, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlPointLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnPointApply)
                    .addComponent(pnlPointColor, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE)))
        );

        pnlProperties.addTab("Point", pnlPoint);
        pnlPoint.getAccessibleContext().setAccessibleParent(pnlProperties);

        pnlStroke.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlStroke.setMaximumSize(null);
        pnlStroke.setMinimumSize(new java.awt.Dimension(623, 300));
        pnlStroke.setPreferredSize(new java.awt.Dimension(623, 300));

        pnlMark.setBorder(javax.swing.BorderFactory.createTitledBorder("Vertex and Segment Styling"));
        pnlMark.setMaximumSize(null);
        pnlMark.setMinimumSize(new java.awt.Dimension(318, 176));
        pnlMark.setPreferredSize(new java.awt.Dimension(318, 176));

        lblArrowPlacement.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblArrowPlacement.setLabelFor(cmbSegmentArrows);
        lblArrowPlacement.setText("Arrow Placement:");
        lblArrowPlacement.setMaximumSize(null);
        lblArrowPlacement.setMinimumSize(null);

        cmbSegmentArrows.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "None", "Segment Start", "Segment Middle", "Segment End", "End of Line Only" }));
        cmbSegmentArrows.setMaximumSize(null);
        cmbSegmentArrows.setMinimumSize(null);
        cmbSegmentArrows.setPreferredSize(new java.awt.Dimension(90, 20));

        cmbMarkGeoPoints.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbMarkGeoPoints.setMaximumSize(null);
        cmbMarkGeoPoints.setMinimumSize(null);
        cmbMarkGeoPoints.setPreferredSize(new java.awt.Dimension(90, 20));

        lblVertexAll.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblVertexAll.setText("All Other Vertices:");
        lblVertexAll.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        lblVertexAll.setMaximumSize(null);
        lblVertexAll.setMinimumSize(null);

        lblVertexStart.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblVertexStart.setText("Start Vertex:");
        lblVertexStart.setMaximumSize(null);
        lblVertexStart.setMinimumSize(null);

        cmbMarkGeoStart.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbMarkGeoStart.setMaximumSize(null);
        cmbMarkGeoStart.setMinimumSize(null);
        cmbMarkGeoStart.setPreferredSize(new java.awt.Dimension(90, 20));

        javax.swing.GroupLayout pnlMarkLayout = new javax.swing.GroupLayout(pnlMark);
        pnlMark.setLayout(pnlMarkLayout);
        pnlMarkLayout.setHorizontalGroup(
            pnlMarkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMarkLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlMarkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblVertexStart, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblVertexAll, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblArrowPlacement, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(pnlMarkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(cmbMarkGeoPoints, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cmbMarkGeoStart, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cmbSegmentArrows, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(84, Short.MAX_VALUE))
        );
        pnlMarkLayout.setVerticalGroup(
            pnlMarkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMarkLayout.createSequentialGroup()
                .addGroup(pnlMarkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblVertexStart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cmbMarkGeoStart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlMarkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblVertexAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cmbMarkGeoPoints, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlMarkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblArrowPlacement, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cmbSegmentArrows, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pnlStrokeSizeStyle.setBorder(javax.swing.BorderFactory.createTitledBorder("Size and Style"));
        pnlStrokeSizeStyle.setMaximumSize(null);
        pnlStrokeSizeStyle.setMinimumSize(new java.awt.Dimension(316, 96));

        lblLineWidth.setText("Width:");
        lblLineWidth.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        lblLineWidth.setMaximumSize(null);
        lblLineWidth.setMinimumSize(new java.awt.Dimension(34, 14));
        lblLineWidth.setPreferredSize(new java.awt.Dimension(34, 14));

        lblLineStyles.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblLineStyles.setText("Style:");

        cmbLineStyles.setMaximumSize(null);
        cmbLineStyles.setMinimumSize(null);
        cmbLineStyles.setPreferredSize(new java.awt.Dimension(90, 22));
        cmbLineStyles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbLineStylesActionPerformed(evt);
            }
        });

        sldrLineWidth.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        sldrLineWidth.setMajorTickSpacing(4);
        sldrLineWidth.setMaximum(16);
        sldrLineWidth.setMinimum(1);
        sldrLineWidth.setMinorTickSpacing(1);
        sldrLineWidth.setPaintLabels(true);
        sldrLineWidth.setPaintTicks(true);
        sldrLineWidth.setSnapToTicks(true);
        sldrLineWidth.setValue(1);
        sldrLineWidth.setMaximumSize(null);
        sldrLineWidth.setMinimumSize(new java.awt.Dimension(200, 41));
        sldrLineWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrLineWidthStateChanged(evt);
            }
        });

        lineWidthTF.setMaximumSize(null);
        lineWidthTF.setMinimumSize(new java.awt.Dimension(50, 20));
        lineWidthTF.setPreferredSize(new java.awt.Dimension(50, 20));
        lineWidthTF.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                lineWidthTFFocusLost(evt);
            }
        });

        javax.swing.GroupLayout pnlStrokeSizeStyleLayout = new javax.swing.GroupLayout(pnlStrokeSizeStyle);
        pnlStrokeSizeStyle.setLayout(pnlStrokeSizeStyleLayout);
        pnlStrokeSizeStyleLayout.setHorizontalGroup(
            pnlStrokeSizeStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStrokeSizeStyleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlStrokeSizeStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblLineWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlStrokeSizeStyleLayout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addGroup(pnlStrokeSizeStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblLineStyles, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lineWidthTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlStrokeSizeStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sldrLineWidth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnlStrokeSizeStyleLayout.createSequentialGroup()
                        .addComponent(cmbLineStyles, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        pnlStrokeSizeStyleLayout.setVerticalGroup(
            pnlStrokeSizeStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStrokeSizeStyleLayout.createSequentialGroup()
                .addGroup(pnlStrokeSizeStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sldrLineWidth, javax.swing.GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE)
                    .addComponent(lineWidthTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLineWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlStrokeSizeStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(cmbLineStyles, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                    .addComponent(lblLineStyles))
                .addGap(0, 4, Short.MAX_VALUE))
        );

        btnStrokeApply.setText("Apply");
        btnStrokeApply.setMaximumSize(null);
        btnStrokeApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStrokeApplyActionPerformed(evt);
            }
        });

        pnlStrokeColor.setBorder(javax.swing.BorderFactory.createTitledBorder("Color"));
        pnlStrokeColor.setMaximumSize(null);
        pnlStrokeColor.setMinimumSize(new java.awt.Dimension(347, 178));
        pnlStrokeColor.setRequestFocusEnabled(false);

        strokeColorButton.setText("Color");
        strokeColorButton.setMaximumSize(new java.awt.Dimension(75, 23));
        strokeColorButton.setMinimumSize(new java.awt.Dimension(75, 23));
        strokeColorButton.setPreferredSize(new java.awt.Dimension(75, 23));
        strokeColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                strokeColorButtonActionPerformed(evt);
            }
        });

        bgStrokeColor.add(rbStrokeColorSolid);
        rbStrokeColorSolid.setText("Solid");
        rbStrokeColorSolid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbStrokeColorSolidActionPerformed(evt);
            }
        });

        bgStrokeColor.add(rbStrokeColorRandom);
        rbStrokeColorRandom.setText("Random");
        rbStrokeColorRandom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbStrokeColorRandomActionPerformed(evt);
            }
        });

        bgStrokeColor.add(rbStrokeColorColumn);
        rbStrokeColorColumn.setText("Column");
        rbStrokeColorColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbStrokeColorColumnActionPerformed(evt);
            }
        });

        cmbStrokeColorColumns.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "TheLargestColumnSizeIs30Chars" }));
        cmbStrokeColorColumns.setEnabled(false);
        cmbStrokeColorColumns.setMaximumSize(new java.awt.Dimension(253, 20));
        cmbStrokeColorColumns.setMinimumSize(new java.awt.Dimension(253, 20));
        cmbStrokeColorColumns.setPreferredSize(new java.awt.Dimension(253, 20));

        bgStrokeColor.add(rbStrokeColorNone);
        rbStrokeColorNone.setText("None");

        pnlStrokeTransparency.setBorder(javax.swing.BorderFactory.createTitledBorder("Transparency"));
        pnlStrokeTransparency.setMaximumSize(null);
        pnlStrokeTransparency.setMinimumSize(new java.awt.Dimension(315, 85));
        pnlStrokeTransparency.setPreferredSize(new java.awt.Dimension(315, 85));

        lblStrokeTransLevel.setText("Value: 0");
        lblStrokeTransLevel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblStrokeTransLevel.setMaximumSize(null);
        lblStrokeTransLevel.setMinimumSize(new java.awt.Dimension(70, 20));
        lblStrokeTransLevel.setPreferredSize(new java.awt.Dimension(70, 20));

        sldrStrokeTransLevel.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        sldrStrokeTransLevel.setMajorTickSpacing(25);
        sldrStrokeTransLevel.setMinorTickSpacing(5);
        sldrStrokeTransLevel.setPaintLabels(true);
        sldrStrokeTransLevel.setPaintTicks(true);
        sldrStrokeTransLevel.setSnapToTicks(true);
        sldrStrokeTransLevel.setValue(0);
        sldrStrokeTransLevel.setMaximumSize(null);
        sldrStrokeTransLevel.setMinimumSize(new java.awt.Dimension(227, 35));
        sldrStrokeTransLevel.setPreferredSize(new java.awt.Dimension(227, 35));
        sldrStrokeTransLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrStrokeTransLevelStateChanged(evt);
            }
        });

        lblStrokeTranspEnd.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        lblStrokeTranspEnd.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblStrokeTranspEnd.setText("Clear");
        lblStrokeTranspEnd.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        lblStrokeSolid.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        lblStrokeSolid.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblStrokeSolid.setText("Solid");

        javax.swing.GroupLayout pnlStrokeTransparencyLayout = new javax.swing.GroupLayout(pnlStrokeTransparency);
        pnlStrokeTransparency.setLayout(pnlStrokeTransparencyLayout);
        pnlStrokeTransparencyLayout.setHorizontalGroup(
            pnlStrokeTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStrokeTransparencyLayout.createSequentialGroup()
                .addComponent(lblStrokeTransLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlStrokeTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sldrStrokeTransLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlStrokeTransparencyLayout.createSequentialGroup()
                        .addComponent(lblStrokeSolid)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblStrokeTranspEnd, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        pnlStrokeTransparencyLayout.setVerticalGroup(
            pnlStrokeTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlStrokeTransparencyLayout.createSequentialGroup()
                .addGroup(pnlStrokeTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(pnlStrokeTransparencyLayout.createSequentialGroup()
                        .addComponent(sldrStrokeTransLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(pnlStrokeTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblStrokeTranspEnd)
                            .addComponent(lblStrokeSolid)))
                    .addComponent(lblStrokeTransLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout pnlStrokeColorLayout = new javax.swing.GroupLayout(pnlStrokeColor);
        pnlStrokeColor.setLayout(pnlStrokeColorLayout);
        pnlStrokeColorLayout.setHorizontalGroup(
            pnlStrokeColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStrokeColorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlStrokeColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlStrokeTransparency, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnlStrokeColorLayout.createSequentialGroup()
                        .addGroup(pnlStrokeColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlStrokeColorLayout.createSequentialGroup()
                                .addComponent(rbStrokeColorNone)
                                .addGap(12, 12, 12)
                                .addComponent(rbStrokeColorRandom)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(rbStrokeColorSolid)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(strokeColorButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(pnlStrokeColorLayout.createSequentialGroup()
                                .addComponent(rbStrokeColorColumn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbStrokeColorColumns, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnlStrokeColorLayout.setVerticalGroup(
            pnlStrokeColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStrokeColorLayout.createSequentialGroup()
                .addGroup(pnlStrokeColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbStrokeColorRandom)
                    .addComponent(rbStrokeColorNone)
                    .addComponent(strokeColorButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rbStrokeColorSolid))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlStrokeColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbStrokeColorColumn)
                    .addComponent(cmbStrokeColorColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlStrokeTransparency, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pnlStrokeLayout = new javax.swing.GroupLayout(pnlStroke);
        pnlStroke.setLayout(pnlStrokeLayout);
        pnlStrokeLayout.setHorizontalGroup(
            pnlStrokeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStrokeLayout.createSequentialGroup()
                .addGroup(pnlStrokeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlStrokeSizeStyle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlMark, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(pnlStrokeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlStrokeLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlStrokeColor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlStrokeLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnStrokeApply, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18))))
        );
        pnlStrokeLayout.setVerticalGroup(
            pnlStrokeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlStrokeLayout.createSequentialGroup()
                .addComponent(pnlStrokeColor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(52, 52, 52)
                .addComponent(btnStrokeApply, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
            .addGroup(pnlStrokeLayout.createSequentialGroup()
                .addComponent(pnlStrokeSizeStyle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlMark, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(51, 51, 51))
        );

        pnlProperties.addTab("Line", pnlStroke);
        pnlStroke.getAccessibleContext().setAccessibleParent(pnlProperties);

        pnlFill.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlFill.setMaximumSize(new java.awt.Dimension(623, 251));
        pnlFill.setMinimumSize(new java.awt.Dimension(623, 251));

        btnFillApply.setText("Apply");
        btnFillApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFillApplyActionPerformed(evt);
            }
        });

        pnlFillColor.setBorder(javax.swing.BorderFactory.createTitledBorder("Color"));

        bgFillColor.add(noShadeRB);
        noShadeRB.setText("None");
        noShadeRB.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        noShadeRB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        noShadeRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noShadeRBActionPerformed(evt);
            }
        });

        bgFillColor.add(randomShadeRB);
        randomShadeRB.setText("Random");
        randomShadeRB.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        randomShadeRB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        randomShadeRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                randomShadeRBActionPerformed(evt);
            }
        });

        cmbShadeColumns.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "TheLargestColumnSizeIs30Chars" }));
        cmbShadeColumns.setEnabled(false);
        cmbShadeColumns.setMaximumSize(null);
        cmbShadeColumns.setMinimumSize(new java.awt.Dimension(201, 20));
        cmbShadeColumns.setPreferredSize(new java.awt.Dimension(201, 20));

        bgFillColor.add(fixShadeColorRB);
        fixShadeColorRB.setText("Solid");
        fixShadeColorRB.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        fixShadeColorRB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        fixShadeColorRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixShadeColorRBActionPerformed(evt);
            }
        });

        fixShadeColorB.setText("Color");
        fixShadeColorB.setMaximumSize(new java.awt.Dimension(75, 23));
        fixShadeColorB.setMinimumSize(new java.awt.Dimension(75, 23));
        fixShadeColorB.setPreferredSize(new java.awt.Dimension(75, 23));
        fixShadeColorB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixShadeColorBActionPerformed(evt);
            }
        });

        bgFillColor.add(columnShadeColorRB);
        columnShadeColorRB.setText("Column");
        columnShadeColorRB.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        columnShadeColorRB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        columnShadeColorRB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                columnShadeColorRBActionPerformed(evt);
            }
        });

        pnlFillTransparency.setBorder(javax.swing.BorderFactory.createTitledBorder("Transparency"));
        pnlFillTransparency.setMaximumSize(null);
        pnlFillTransparency.setPreferredSize(new java.awt.Dimension(426, 135));

        pnlFillTransparencyInner.setMaximumSize(null);

        lblFillSolidEnd.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        lblFillSolidEnd.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblFillSolidEnd.setText("Solid");
        lblFillSolidEnd.setPreferredSize(new java.awt.Dimension(37, 15));

        lblFillTranspEnd.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        lblFillTranspEnd.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblFillTranspEnd.setText("Clear");
        lblFillTranspEnd.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        lblFillTranspEnd.setPreferredSize(new java.awt.Dimension(22, 15));

        sldrShadeTransLevel.setMajorTickSpacing(25);
        sldrShadeTransLevel.setMinorTickSpacing(5);
        sldrShadeTransLevel.setPaintLabels(true);
        sldrShadeTransLevel.setPaintTicks(true);
        sldrShadeTransLevel.setSnapToTicks(true);
        sldrShadeTransLevel.setValue(0);
        sldrShadeTransLevel.setMaximumSize(null);
        sldrShadeTransLevel.setMinimumSize(null);
        sldrShadeTransLevel.setPreferredSize(new java.awt.Dimension(265, 36));
        sldrShadeTransLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrShadeTransLevelStateChanged(evt);
            }
        });

        lblShadeTransLevel.setText("Value: 100");
        lblShadeTransLevel.setMaximumSize(null);
        lblShadeTransLevel.setMinimumSize(null);
        lblShadeTransLevel.setPreferredSize(new java.awt.Dimension(59, 14));

        javax.swing.GroupLayout pnlFillTransparencyInnerLayout = new javax.swing.GroupLayout(pnlFillTransparencyInner);
        pnlFillTransparencyInner.setLayout(pnlFillTransparencyInnerLayout);
        pnlFillTransparencyInnerLayout.setHorizontalGroup(
            pnlFillTransparencyInnerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFillTransparencyInnerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblShadeTransLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFillTransparencyInnerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnlFillTransparencyInnerLayout.createSequentialGroup()
                        .addComponent(lblFillTranspEnd, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblFillSolidEnd, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(sldrShadeTransLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 278, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlFillTransparencyInnerLayout.setVerticalGroup(
            pnlFillTransparencyInnerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFillTransparencyInnerLayout.createSequentialGroup()
                .addComponent(lblShadeTransLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(pnlFillTransparencyInnerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sldrShadeTransLevel, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFillTransparencyInnerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblFillTranspEnd, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblFillSolidEnd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout pnlFillTransparencyLayout = new javax.swing.GroupLayout(pnlFillTransparency);
        pnlFillTransparency.setLayout(pnlFillTransparencyLayout);
        pnlFillTransparencyLayout.setHorizontalGroup(
            pnlFillTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFillTransparencyLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlFillTransparencyInner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlFillTransparencyLayout.setVerticalGroup(
            pnlFillTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFillTransparencyLayout.createSequentialGroup()
                .addComponent(pnlFillTransparencyInner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pnlFillColorLayout = new javax.swing.GroupLayout(pnlFillColor);
        pnlFillColor.setLayout(pnlFillColorLayout);
        pnlFillColorLayout.setHorizontalGroup(
            pnlFillColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFillColorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlFillColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlFillColorLayout.createSequentialGroup()
                        .addComponent(pnlFillTransparency, javax.swing.GroupLayout.DEFAULT_SIZE, 630, Short.MAX_VALUE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnlFillColorLayout.createSequentialGroup()
                        .addGroup(pnlFillColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(noShadeRB)
                            .addComponent(columnShadeColorRB, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlFillColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlFillColorLayout.createSequentialGroup()
                                .addComponent(randomShadeRB)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fixShadeColorRB)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fixShadeColorB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(cmbShadeColumns, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(372, 372, 372))))
        );
        pnlFillColorLayout.setVerticalGroup(
            pnlFillColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFillColorLayout.createSequentialGroup()
                .addGroup(pnlFillColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(noShadeRB)
                    .addComponent(randomShadeRB)
                    .addComponent(fixShadeColorRB)
                    .addComponent(fixShadeColorB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlFillColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(columnShadeColorRB)
                    .addComponent(cmbShadeColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addComponent(pnlFillTransparency, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE)
                .addGap(23, 23, 23))
        );

        javax.swing.GroupLayout pnlFillLayout = new javax.swing.GroupLayout(pnlFill);
        pnlFill.setLayout(pnlFillLayout);
        pnlFillLayout.setHorizontalGroup(
            pnlFillLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFillLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlFillLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlFillLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnFillApply))
                    .addComponent(pnlFillColor, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        pnlFillLayout.setVerticalGroup(
            pnlFillLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFillLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlFillColor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnFillApply))
        );

        pnlProperties.addTab("Area", pnlFill);
        pnlFill.getAccessibleContext().setAccessibleParent(pnlProperties);

        pnlLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlLabel.setMaximumSize(new java.awt.Dimension(623, 251));
        pnlLabel.setMinimumSize(new java.awt.Dimension(623, 251));
        pnlLabel.setPreferredSize(new java.awt.Dimension(623, 251));

        pnlVertexlMarking.setBorder(javax.swing.BorderFactory.createTitledBorder("Vertex / Segment Label"));
        pnlVertexlMarking.setMaximumSize(null);
        pnlVertexlMarking.setMinimumSize(new java.awt.Dimension(105, 101));

        pnlMarkStyle.setBorder(javax.swing.BorderFactory.createTitledBorder("Style / Content"));
        pnlMarkStyle.setMaximumSize(null);
        pnlMarkStyle.setMinimumSize(new java.awt.Dimension(315, 93));
        pnlMarkStyle.setPreferredSize(new java.awt.Dimension(315, 93));

        cbOrientVertexLabel.setText("Orient to Line");

        cmbVertexLabelContent.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "None", "Id", "(XY[Z[M]])", "<Id> (XY[Z[M]])", "<html>&lt;Id&gt;<BR/>(XY[Z[M]])</html>", "Cumulative Length", "(X)", "(Y)", "(Z)", "(M)" }));

        lblVertexLabelContent.setLabelFor(cmbVertexLabelContent);
        lblVertexLabelContent.setText("Vertex:");

        cmbMarkSegment.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "None", "Segment Length", "Cumulative Length", "Bearing", "Segment Identifier" }));

        lblMarkSegment.setLabelFor(cmbMarkSegment);
        lblMarkSegment.setText("Segment:");

        javax.swing.GroupLayout pnlMarkStyleLayout = new javax.swing.GroupLayout(pnlMarkStyle);
        pnlMarkStyle.setLayout(pnlMarkStyleLayout);
        pnlMarkStyleLayout.setHorizontalGroup(
            pnlMarkStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMarkStyleLayout.createSequentialGroup()
                .addGroup(pnlMarkStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlMarkStyleLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(lblVertexLabelContent))
                    .addGroup(pnlMarkStyleLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblMarkSegment)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlMarkStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlMarkStyleLayout.createSequentialGroup()
                        .addComponent(cmbVertexLabelContent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                        .addComponent(cbOrientVertexLabel))
                    .addComponent(cmbMarkSegment, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlMarkStyleLayout.setVerticalGroup(
            pnlMarkStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMarkStyleLayout.createSequentialGroup()
                .addGroup(pnlMarkStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblVertexLabelContent)
                    .addGroup(pnlMarkStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cmbVertexLabelContent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(cbOrientVertexLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlMarkStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbMarkSegment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMarkSegment))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnlMarkPositionOffset.setBorder(javax.swing.BorderFactory.createTitledBorder("Position / Offset / Text"));

        lblMarkOffset.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblMarkOffset.setText("Offset: {0}");

        btnMarkLabelPosition.setText("Position / Offset");
        btnMarkLabelPosition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMarkLabelPositionActionPerformed(evt);
            }
        });

        lblMarkPosition.setText("Position: {0}");

        lblStyledMarkLabel.setText("Text");

        btnMarkLabelStyling.setText("Text Style");
        btnMarkLabelStyling.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMarkLabelStylingActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlMarkPositionOffsetLayout = new javax.swing.GroupLayout(pnlMarkPositionOffset);
        pnlMarkPositionOffset.setLayout(pnlMarkPositionOffsetLayout);
        pnlMarkPositionOffsetLayout.setHorizontalGroup(
            pnlMarkPositionOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMarkPositionOffsetLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlMarkPositionOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnMarkLabelPosition, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMarkPositionOffsetLayout.createSequentialGroup()
                        .addComponent(btnMarkLabelStyling)
                        .addGap(30, 30, 30)))
                .addGroup(pnlMarkPositionOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlMarkPositionOffsetLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(pnlMarkPositionOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblMarkOffset)
                            .addComponent(lblMarkPosition)))
                    .addGroup(pnlMarkPositionOffsetLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblStyledMarkLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnlMarkPositionOffsetLayout.setVerticalGroup(
            pnlMarkPositionOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMarkPositionOffsetLayout.createSequentialGroup()
                .addGroup(pnlMarkPositionOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlMarkPositionOffsetLayout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(btnMarkLabelStyling))
                    .addComponent(btnMarkLabelPosition)
                    .addGroup(pnlMarkPositionOffsetLayout.createSequentialGroup()
                        .addComponent(lblMarkOffset)
                        .addGap(2, 2, 2)
                        .addComponent(lblMarkPosition)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblStyledMarkLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout pnlVertexlMarkingLayout = new javax.swing.GroupLayout(pnlVertexlMarking);
        pnlVertexlMarking.setLayout(pnlVertexlMarkingLayout);
        pnlVertexlMarkingLayout.setHorizontalGroup(
            pnlVertexlMarkingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlVertexlMarkingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlVertexlMarkingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlMarkStyle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlMarkPositionOffset, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(5, 5, 5))
        );
        pnlVertexlMarkingLayout.setVerticalGroup(
            pnlVertexlMarkingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlVertexlMarkingLayout.createSequentialGroup()
                .addComponent(pnlMarkStyle, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlMarkPositionOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(22, Short.MAX_VALUE))
        );

        pnlLabelGeometry.setBorder(javax.swing.BorderFactory.createTitledBorder("Geometry Label"));
        pnlLabelGeometry.setMaximumSize(null);
        pnlLabelGeometry.setMinimumSize(new java.awt.Dimension(272, 187));

        pnlLabelPosition.setBorder(javax.swing.BorderFactory.createTitledBorder("Position"));
        pnlLabelPosition.setMaximumSize(null);
        pnlLabelPosition.setMinimumSize(new java.awt.Dimension(106, 103));

        btnPosition.setText("Change");
        btnPosition.setMaximumSize(new java.awt.Dimension(84, 23));
        btnPosition.setMinimumSize(new java.awt.Dimension(84, 23));
        btnPosition.setPreferredSize(new java.awt.Dimension(84, 23));
        btnPosition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPositionActionPerformed(evt);
            }
        });

        lblOffset.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblOffset.setText("Offset: {0}");

        lblPosition.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblPosition.setText("Position: {0}");

        javax.swing.GroupLayout pnlLabelPositionLayout = new javax.swing.GroupLayout(pnlLabelPosition);
        pnlLabelPosition.setLayout(pnlLabelPositionLayout);
        pnlLabelPositionLayout.setHorizontalGroup(
            pnlLabelPositionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLabelPositionLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlLabelPositionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlLabelPositionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblPosition, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(lblOffset, javax.swing.GroupLayout.Alignment.TRAILING))
                    .addComponent(btnPosition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        pnlLabelPositionLayout.setVerticalGroup(
            pnlLabelPositionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLabelPositionLayout.createSequentialGroup()
                .addComponent(lblPosition)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblOffset)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnPosition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13))
        );

        pnlLabelStyle.setBorder(javax.swing.BorderFactory.createTitledBorder("Text Style"));
        pnlLabelStyle.setMaximumSize(null);
        pnlLabelStyle.setMinimumSize(new java.awt.Dimension(132, 103));
        pnlLabelStyle.setPreferredSize(new java.awt.Dimension(132, 103));

        btnLabelStyling.setText("Change");
        btnLabelStyling.setMaximumSize(new java.awt.Dimension(84, 23));
        btnLabelStyling.setMinimumSize(new java.awt.Dimension(84, 23));
        btnLabelStyling.setPreferredSize(new java.awt.Dimension(84, 23));
        btnLabelStyling.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLabelStylingActionPerformed(evt);
            }
        });

        lblStyledLabel.setText("Text");
        lblStyledLabel.setMaximumSize(new java.awt.Dimension(100, 14));
        lblStyledLabel.setMinimumSize(new java.awt.Dimension(100, 14));
        lblStyledLabel.setPreferredSize(new java.awt.Dimension(100, 14));

        javax.swing.GroupLayout pnlLabelStyleLayout = new javax.swing.GroupLayout(pnlLabelStyle);
        pnlLabelStyle.setLayout(pnlLabelStyleLayout);
        pnlLabelStyleLayout.setHorizontalGroup(
            pnlLabelStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLabelStyleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlLabelStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblStyledLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnLabelStyling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlLabelStyleLayout.setVerticalGroup(
            pnlLabelStyleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLabelStyleLayout.createSequentialGroup()
                .addComponent(lblStyledLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnLabelStyling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        cmbLabelColumns.setMaximumRowCount(255);
        cmbLabelColumns.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "TheLargestColumnSizeIs30Chars" }));
        cmbLabelColumns.setToolTipText("Select column in table to use as label");
        cmbLabelColumns.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        cmbLabelColumns.setMaximumSize(new java.awt.Dimension(190, 20));
        cmbLabelColumns.setMinimumSize(new java.awt.Dimension(190, 20));
        cmbLabelColumns.setName("cmbLabelColumns"); // NOI18N
        cmbLabelColumns.setPreferredSize(new java.awt.Dimension(190, 20));
        cmbLabelColumns.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbLabelColumnsItemStateChanged(evt);
            }
        });

        lblLabelColumn.setText("Column:");

        lblGeometryLabelPoint.setLabelFor(cmbGeometryLabelPosition);
        lblGeometryLabelPoint.setText("Label Position:");

        cmbGeometryLabelPosition.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "First Vertex ", "Middle Vertex", "End Vertex", "JTS Centroid", "SDO_POINT" }));

        javax.swing.GroupLayout pnlLabelGeometryLayout = new javax.swing.GroupLayout(pnlLabelGeometry);
        pnlLabelGeometry.setLayout(pnlLabelGeometryLayout);
        pnlLabelGeometryLayout.setHorizontalGroup(
            pnlLabelGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLabelGeometryLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlLabelGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlLabelGeometryLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(lblLabelColumn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbLabelColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlLabelGeometryLayout.createSequentialGroup()
                        .addComponent(pnlLabelPosition, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 31, Short.MAX_VALUE)
                        .addComponent(pnlLabelStyle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnlLabelGeometryLayout.createSequentialGroup()
                        .addComponent(lblGeometryLabelPoint)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbGeometryLabelPosition, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnlLabelGeometryLayout.setVerticalGroup(
            pnlLabelGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLabelGeometryLayout.createSequentialGroup()
                .addGroup(pnlLabelGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbLabelColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLabelColumn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlLabelGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnlLabelStyle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlLabelPosition, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlLabelGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblGeometryLabelPoint)
                    .addComponent(cmbGeometryLabelPosition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5))
        );

        pnlDisplayScale.setBorder(javax.swing.BorderFactory.createTitledBorder("Display Scale"));
        pnlDisplayScale.setMaximumSize(null);
        pnlDisplayScale.setMinimumSize(new java.awt.Dimension(299, 50));
        pnlDisplayScale.setPreferredSize(new java.awt.Dimension(299, 50));

        lblHiScale.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblHiScale.setLabelFor(tfHiScale);
        lblHiScale.setText("Hi:");

        tfHiScale.setMaximumSize(null);
        tfHiScale.setMinimumSize(new java.awt.Dimension(72, 18));
        tfHiScale.setPreferredSize(new java.awt.Dimension(72, 18));

        tfLoScale.setText("0");
        tfLoScale.setMaximumSize(null);
        tfLoScale.setMinimumSize(new java.awt.Dimension(72, 18));
        tfLoScale.setPreferredSize(new java.awt.Dimension(72, 18));
        tfLoScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfLoScaleActionPerformed(evt);
            }
        });

        lblLoScale.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblLoScale.setLabelFor(tfLoScale);
        lblLoScale.setText("Lo:");

        javax.swing.GroupLayout pnlDisplayScaleLayout = new javax.swing.GroupLayout(pnlDisplayScale);
        pnlDisplayScale.setLayout(pnlDisplayScaleLayout);
        pnlDisplayScaleLayout.setHorizontalGroup(
            pnlDisplayScaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDisplayScaleLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblLoScale, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tfLoScale, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblHiScale, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tfHiScale, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        pnlDisplayScaleLayout.setVerticalGroup(
            pnlDisplayScaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDisplayScaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(lblLoScale, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(tfLoScale, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                .addComponent(lblHiScale, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(tfHiScale, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        btnLabelApply.setText("Apply");
        btnLabelApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLabelApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlLabelLayout = new javax.swing.GroupLayout(pnlLabel);
        pnlLabel.setLayout(pnlLabelLayout);
        pnlLabelLayout.setHorizontalGroup(
            pnlLabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLabelLayout.createSequentialGroup()
                .addGroup(pnlLabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlLabelLayout.createSequentialGroup()
                        .addComponent(pnlVertexlMarking, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlLabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(pnlLabelGeometry, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(pnlDisplayScale, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE))
                        .addGap(0, 10, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlLabelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnLabelApply)))
                .addContainerGap())
        );
        pnlLabelLayout.setVerticalGroup(
            pnlLabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLabelLayout.createSequentialGroup()
                .addGroup(pnlLabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlVertexlMarking, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnlLabelLayout.createSequentialGroup()
                        .addComponent(pnlLabelGeometry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlDisplayScale, javax.swing.GroupLayout.DEFAULT_SIZE, 55, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addComponent(btnLabelApply))
        );

        pnlProperties.addTab("Labelling", pnlLabel);
        pnlLabel.getAccessibleContext().setAccessibleParent(pnlProperties);

        pnlRotation.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlRotation.setMaximumSize(new java.awt.Dimension(623, 251));
        pnlRotation.setMinimumSize(new java.awt.Dimension(623, 251));
        pnlRotation.setPreferredSize(new java.awt.Dimension(623, 251));

        lblRotationTarget.setText("Rotate:");

        bgRotation.add(rbDegrees);
        rbDegrees.setSelected(true);
        rbDegrees.setText("Degrees");
        rbDegrees.setToolTipText("Value in column is in degrees");
        rbDegrees.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        cmbRotationColumns.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "TheLargestColumnSizeIs30Chars" }));
        cmbRotationColumns.setToolTipText("If label column selected, select column in table that contains rotation angle (otherwise 0)");
        cmbRotationColumns.setMaximumSize(new java.awt.Dimension(190, 20));
        cmbRotationColumns.setMinimumSize(new java.awt.Dimension(190, 20));
        cmbRotationColumns.setPreferredSize(new java.awt.Dimension(190, 20));
        cmbRotationColumns.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbRotationColumnsActionPerformed(evt);
            }
        });

        cmbRotationTarget.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Marker", "Label", "Both", "None" }));
        cmbRotationTarget.setMaximumSize(new java.awt.Dimension(60, 20));
        cmbRotationTarget.setMinimumSize(new java.awt.Dimension(60, 20));
        cmbRotationTarget.setPreferredSize(new java.awt.Dimension(60, 20));

        lblRotationColumn.setText("Column:");

        bgRotation.add(rbRadians);
        rbRadians.setText("Radians");
        rbRadians.setToolTipText("Value in column is in radians");
        rbRadians.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        btnRotationApply.setText("Apply");
        btnRotationApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRotationApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlRotationLayout = new javax.swing.GroupLayout(pnlRotation);
        pnlRotation.setLayout(pnlRotationLayout);
        pnlRotationLayout.setHorizontalGroup(
            pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlRotationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnRotationApply, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(pnlRotationLayout.createSequentialGroup()
                        .addGroup(pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlRotationLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblRotationTarget)
                                    .addComponent(lblRotationColumn))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(pnlRotationLayout.createSequentialGroup()
                                        .addGap(2, 2, 2)
                                        .addComponent(cmbRotationTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(cmbRotationColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(rbRadians)
                                .addComponent(rbDegrees)))
                        .addContainerGap(416, Short.MAX_VALUE))))
        );
        pnlRotationLayout.setVerticalGroup(
            pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlRotationLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rbDegrees)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbRadians)
                .addGap(16, 16, 16)
                .addGroup(pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cmbRotationTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRotationTarget))
                .addGap(18, 18, 18)
                .addGroup(pnlRotationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblRotationColumn)
                    .addComponent(cmbRotationColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 137, Short.MAX_VALUE)
                .addComponent(btnRotationApply))
        );

        pnlProperties.addTab("Rotation", pnlRotation);
        pnlRotation.getAccessibleContext().setAccessibleParent(pnlProperties);

        pnlQueryLayer.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        lblQueryBufferDistance.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblQueryBufferDistance.setLabelFor(tfQueryBufferDistance);
        lblQueryBufferDistance.setText("Buffer Distance:");
        lblQueryBufferDistance.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        tfQueryBufferDistance.setText("0.0");
        tfQueryBufferDistance.setMinimumSize(new java.awt.Dimension(89, 20));
        tfQueryBufferDistance.setPreferredSize(new java.awt.Dimension(89, 20));

        cbQueryShowQueryGeometry.setText("Show Query Geometry");
        cbQueryShowQueryGeometry.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        cbQueryShowQueryGeometry.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        cbQueryBufferGeometry.setText("Apply Buffer:");
        cbQueryBufferGeometry.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        cbQueryBufferGeometry.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        btnQueryApply.setText("Apply");
        btnQueryApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQueryApplyActionPerformed(evt);
            }
        });

        cmbSdoOperators.setModel(Constants.getSdoOperators());
        cmbSdoOperators.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbSdoOperatorsActionPerformed(evt);
            }
        });

        scrpQueryRelateMasks.setPreferredSize(new java.awt.Dimension(209, 173));

        listRelateMasks.setModel(Constants.getRelateMasks());
        scrpQueryRelateMasks.setViewportView(listRelateMasks);

        lblSdoOperator.setLabelFor(cmbSdoOperators);
        lblSdoOperator.setText("SDO Operator:");
        lblSdoOperator.setPreferredSize(new java.awt.Dimension(83, 14));

        lblRelateMasks.setLabelFor(listRelateMasks);
        lblRelateMasks.setText("Relate Masks:");

        btnCopyQueryGeometry.setText("Copy Query Geometry To Clipboard");
        btnCopyQueryGeometry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCopyQueryGeometryActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlQueryLayerLayout = new javax.swing.GroupLayout(pnlQueryLayer);
        pnlQueryLayer.setLayout(pnlQueryLayerLayout);
        pnlQueryLayerLayout.setHorizontalGroup(
            pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlQueryLayerLayout.createSequentialGroup()
                .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlQueryLayerLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnCopyQueryGeometry))
                    .addGroup(pnlQueryLayerLayout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlQueryLayerLayout.createSequentialGroup()
                                .addComponent(lblQueryBufferDistance, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tfQueryBufferDistance, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(cbQueryBufferGeometry, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(cbQueryShowQueryGeometry)))))
                .addGap(18, 18, 18)
                .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblRelateMasks, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                    .addComponent(lblSdoOperator, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(cmbSdoOperators, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrpQueryRelateMasks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(60, 60, 60)
                .addComponent(btnQueryApply)
                .addContainerGap())
        );
        pnlQueryLayerLayout.setVerticalGroup(
            pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlQueryLayerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlQueryLayerLayout.createSequentialGroup()
                        .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblSdoOperator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblQueryBufferDistance)
                            .addComponent(tfQueryBufferDistance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(11, 11, 11)
                        .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblRelateMasks)
                            .addComponent(cbQueryBufferGeometry, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbQueryShowQueryGeometry)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCopyQueryGeometry))
                    .addGroup(pnlQueryLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(btnQueryApply)
                        .addGroup(pnlQueryLayerLayout.createSequentialGroup()
                            .addComponent(cmbSdoOperators, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(11, 11, 11)
                            .addComponent(scrpQueryRelateMasks, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        pnlProperties.addTab("Query Layer", pnlQueryLayer);

        pnlSelection.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlSelection.setMaximumSize(null);

        tfSelectionPointSize.setMaximumSize(null);
        tfSelectionPointSize.setMinimumSize(null);
        tfSelectionPointSize.setPreferredSize(new java.awt.Dimension(50, 22));
        tfSelectionPointSize.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                tfSelectionPointSizeFocusLost(evt);
            }
        });

        lblSelectionLineWidth.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblSelectionLineWidth.setText("Line width:");
        lblSelectionLineWidth.setMaximumSize(null);
        lblSelectionLineWidth.setMinimumSize(null);
        lblSelectionLineWidth.setPreferredSize(new java.awt.Dimension(62, 14));

        lblSelectionPointSize.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblSelectionPointSize.setText("Point size:");
        lblSelectionPointSize.setMaximumSize(null);
        lblSelectionPointSize.setMinimumSize(null);

        tfSelectionLineWidth.setMaximumSize(null);
        tfSelectionLineWidth.setMinimumSize(null);
        tfSelectionLineWidth.setPreferredSize(new java.awt.Dimension(50, 22));
        tfSelectionLineWidth.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                tfSelectionLineWidthFocusLost(evt);
            }
        });

        pnlSelectionTransparency.setMaximumSize(null);

        lblSelectionTranspEnd.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        lblSelectionTranspEnd.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblSelectionTranspEnd.setText("Clear");
        lblSelectionTranspEnd.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        lblSelectionTranspEnd.setMaximumSize(null);
        lblSelectionTranspEnd.setMinimumSize(null);

        lblSelectionSolidEnd.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N
        lblSelectionSolidEnd.setText("Solid");
        lblSelectionSolidEnd.setMaximumSize(null);
        lblSelectionSolidEnd.setMinimumSize(null);
        lblSelectionSolidEnd.setPreferredSize(new java.awt.Dimension(171, 11));

        sldrSelectionTransLevel.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        sldrSelectionTransLevel.setMajorTickSpacing(25);
        sldrSelectionTransLevel.setMinorTickSpacing(5);
        sldrSelectionTransLevel.setPaintLabels(true);
        sldrSelectionTransLevel.setPaintTicks(true);
        sldrSelectionTransLevel.setSnapToTicks(true);
        sldrSelectionTransLevel.setValue(0);
        sldrSelectionTransLevel.setMaximumSize(null);
        sldrSelectionTransLevel.setMinimumSize(null);
        sldrSelectionTransLevel.setPreferredSize(new java.awt.Dimension(349, 46));
        sldrSelectionTransLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrSelectionTransLevelStateChanged(evt);
            }
        });

        lblSelectionTransLevel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblSelectionTransLevel.setText("Transparency (1.0):");
        lblSelectionTransLevel.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        lblSelectionTransLevel.setMaximumSize(null);
        lblSelectionTransLevel.setMinimumSize(null);
        lblSelectionTransLevel.setPreferredSize(new java.awt.Dimension(117, 36));

        cbSelectionActive.setText("Use Selection Colouring");
        cbSelectionActive.setMaximumSize(null);
        cbSelectionActive.setMinimumSize(null);
        cbSelectionActive.setPreferredSize(new java.awt.Dimension(200, 23));
        cbSelectionActive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbSelectionActiveActionPerformed(evt);
            }
        });

        btnSelectionColor.setText("Color");
        btnSelectionColor.setMaximumSize(null);
        btnSelectionColor.setMinimumSize(null);
        btnSelectionColor.setPreferredSize(new java.awt.Dimension(67, 23));
        btnSelectionColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectionColorActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlSelectionTransparencyLayout = new javax.swing.GroupLayout(pnlSelectionTransparency);
        pnlSelectionTransparency.setLayout(pnlSelectionTransparencyLayout);
        pnlSelectionTransparencyLayout.setHorizontalGroup(
            pnlSelectionTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSelectionTransparencyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlSelectionTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnSelectionColor, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
                    .addComponent(lblSelectionTransLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(10, 10, 10)
                .addGroup(pnlSelectionTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlSelectionTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(pnlSelectionTransparencyLayout.createSequentialGroup()
                            .addComponent(lblSelectionSolidEnd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblSelectionTranspEnd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addComponent(sldrSelectionTransLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(cbSelectionActive, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        pnlSelectionTransparencyLayout.setVerticalGroup(
            pnlSelectionTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSelectionTransparencyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlSelectionTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSelectionColor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cbSelectionActive, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlSelectionTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblSelectionTransLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sldrSelectionTransLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlSelectionTransparencyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSelectionSolidEnd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSelectionTranspEnd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(23, 23, 23))
        );

        sldrSelectionPointSize.setMajorTickSpacing(8);
        sldrSelectionPointSize.setMaximum(72);
        sldrSelectionPointSize.setMinimum(4);
        sldrSelectionPointSize.setMinorTickSpacing(2);
        sldrSelectionPointSize.setPaintLabels(true);
        sldrSelectionPointSize.setPaintTicks(true);
        sldrSelectionPointSize.setValue(4);
        sldrSelectionPointSize.setMaximumSize(null);
        sldrSelectionPointSize.setMinimumSize(null);
        sldrSelectionPointSize.setPreferredSize(new java.awt.Dimension(330, 45));
        sldrSelectionPointSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrSelectionPointSizeStateChanged(evt);
            }
        });

        sldrSelectionLineWidth.setMajorTickSpacing(4);
        sldrSelectionLineWidth.setMaximum(21);
        sldrSelectionLineWidth.setMinimum(1);
        sldrSelectionLineWidth.setMinorTickSpacing(1);
        sldrSelectionLineWidth.setPaintLabels(true);
        sldrSelectionLineWidth.setPaintTicks(true);
        sldrSelectionLineWidth.setValue(1);
        sldrSelectionLineWidth.setMaximumSize(null);
        sldrSelectionLineWidth.setMinimumSize(null);
        sldrSelectionLineWidth.setPreferredSize(new java.awt.Dimension(330, 45));
        sldrSelectionLineWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrSelectionLineWidthStateChanged(evt);
            }
        });

        cmbSelectionLineStyles.setMaximumSize(null);
        cmbSelectionLineStyles.setMinimumSize(null);
        cmbSelectionLineStyles.setPreferredSize(new java.awt.Dimension(125, 22));

        lblSelectionLineStyles.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblSelectionLineStyles.setText("Line Style:");
        lblSelectionLineStyles.setMaximumSize(null);
        lblSelectionLineStyles.setMinimumSize(null);

        btnSelectionApply.setText("Apply");
        btnSelectionApply.setMaximumSize(null);
        btnSelectionApply.setMinimumSize(null);
        btnSelectionApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectionApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlSelectionLayout = new javax.swing.GroupLayout(pnlSelection);
        pnlSelection.setLayout(pnlSelectionLayout);
        pnlSelectionLayout.setHorizontalGroup(
            pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSelectionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlSelectionLayout.createSequentialGroup()
                        .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblSelectionLineStyles, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblSelectionLineWidth, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblSelectionPointSize, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlSelectionLayout.createSequentialGroup()
                                .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(tfSelectionLineWidth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(tfSelectionPointSize, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(sldrSelectionLineWidth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(sldrSelectionPointSize, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addComponent(cmbSelectionLineStyles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(pnlSelectionLayout.createSequentialGroup()
                        .addComponent(pnlSelectionTransparency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(87, 87, 87)
                        .addComponent(btnSelectionApply, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlSelectionLayout.setVerticalGroup(
            pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlSelectionLayout.createSequentialGroup()
                .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sldrSelectionPointSize, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE)
                    .addGroup(pnlSelectionLayout.createSequentialGroup()
                        .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblSelectionPointSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tfSelectionPointSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sldrSelectionLineWidth, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
                    .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(lblSelectionLineWidth, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(tfSelectionLineWidth, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cmbSelectionLineStyles, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnlSelectionLayout.createSequentialGroup()
                        .addComponent(lblSelectionLineStyles, javax.swing.GroupLayout.DEFAULT_SIZE, 17, Short.MAX_VALUE)
                        .addGap(10, 10, 10)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSelectionTransparency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlSelectionLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(btnSelectionApply, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pnlProperties.addTab("Selection", pnlSelection);
        pnlSelection.getAccessibleContext().setAccessibleParent(pnlProperties);

        btnHelp.setMnemonic('H');
        btnHelp.setText("Help");
        btnHelp.setMaximumSize(null);
        btnHelp.setMinimumSize(null);
        btnHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHelpActionPerformed(evt);
            }
        });

        btnClose.setMnemonic('C');
        btnClose.setText("Close");
        btnClose.setMaximumSize(null);
        btnClose.setMinimumSize(null);
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        pnlLayerExtent.setBorder(javax.swing.BorderFactory.createTitledBorder("Layer Extent (MBR)"));
        pnlLayerExtent.setMaximumSize(null);
        pnlLayerExtent.setMinimumSize(new java.awt.Dimension(260, 223));

        pnlMBR.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        pnlMBR.setToolTipText("Minumum Bounding Rectangle (MBR)");
        pnlMBR.setMaximumSize(null);
        pnlMBR.setMinimumSize(new java.awt.Dimension(208, 150));

        lblLLX.setFont(new java.awt.Font("Times New Roman", 0, 11)); // NOI18N
        lblLLX.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblLLX.setText("?");
        lblLLX.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblLLX.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        lblLLX.setIconTextGap(2);
        lblLLX.setMaximumSize(new java.awt.Dimension(148, 13));
        lblLLX.setMinimumSize(new java.awt.Dimension(148, 13));
        lblLLX.setPreferredSize(new java.awt.Dimension(148, 13));
        lblLLX.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        lblLLY.setFont(new java.awt.Font("Times New Roman", 0, 11)); // NOI18N
        lblLLY.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblLLY.setText("?");
        lblLLY.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblLLY.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        lblLLY.setIconTextGap(2);
        lblLLY.setMaximumSize(new java.awt.Dimension(148, 13));
        lblLLY.setMinimumSize(new java.awt.Dimension(148, 13));
        lblLLY.setPreferredSize(new java.awt.Dimension(148, 13));
        lblLLY.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        lblURX.setFont(new java.awt.Font("Times New Roman", 0, 11)); // NOI18N
        lblURX.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblURX.setText("?");
        lblURX.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        lblURX.setMaximumSize(new java.awt.Dimension(148, 13));
        lblURX.setMinimumSize(new java.awt.Dimension(148, 13));
        lblURX.setPreferredSize(new java.awt.Dimension(148, 13));

        lblURY.setFont(new java.awt.Font("Times New Roman", 0, 11)); // NOI18N
        lblURY.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblURY.setText("?");
        lblURY.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        lblURY.setMaximumSize(new java.awt.Dimension(148, 13));
        lblURY.setMinimumSize(new java.awt.Dimension(148, 13));
        lblURY.setPreferredSize(new java.awt.Dimension(148, 13));

        tfMBRFiller.setEditable(false);
        tfMBRFiller.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        tfMBRFiller.setEnabled(false);
        tfMBRFiller.setMaximumSize(new java.awt.Dimension(184, 48));
        tfMBRFiller.setMinimumSize(new java.awt.Dimension(184, 48));
        tfMBRFiller.setPreferredSize(new java.awt.Dimension(184, 48));
        tfMBRFiller.setRequestFocusEnabled(false);
        tfMBRFiller.setVerifyInputWhenFocusTarget(false);

        javax.swing.GroupLayout pnlMBRLayout = new javax.swing.GroupLayout(pnlMBR);
        pnlMBR.setLayout(pnlMBRLayout);
        pnlMBRLayout.setHorizontalGroup(
            pnlMBRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMBRLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlMBRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMBRLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(pnlMBRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblURY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblURX, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(tfMBRFiller, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnlMBRLayout.createSequentialGroup()
                        .addGroup(pnlMBRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblLLX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblLLY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnlMBRLayout.setVerticalGroup(
            pnlMBRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMBRLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblURX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblURY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfMBRFiller, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblLLX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblLLY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        cbRecalculateMBR.setText("Recalculate (next draw)");
        cbRecalculateMBR.setMaximumSize(null);
        cbRecalculateMBR.setMinimumSize(null);

        btnClipboard.setText("Clipboard");
        btnClipboard.setMaximumSize(null);
        btnClipboard.setMinimumSize(null);
        btnClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClipboardActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlLayerExtentLayout = new javax.swing.GroupLayout(pnlLayerExtent);
        pnlLayerExtent.setLayout(pnlLayerExtentLayout);
        pnlLayerExtentLayout.setHorizontalGroup(
            pnlLayerExtentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLayerExtentLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlLayerExtentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlLayerExtentLayout.createSequentialGroup()
                        .addComponent(cbRecalculateMBR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnClipboard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(pnlMBR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlLayerExtentLayout.setVerticalGroup(
            pnlLayerExtentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLayerExtentLayout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addComponent(pnlMBR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlLayerExtentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbRecalculateMBR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnClipboard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlLayerExtent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(pnlProperties, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btnHelp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btnClose, btnHelp});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnlLayerExtent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlProperties, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnHelp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        processBtnClose();
    }//GEN-LAST:event_btnCloseActionPerformed

    private void btnHelpActionPerformed(java.awt.event.ActionEvent evt) {
        Object image_url_0 = cl.getResource(iconDirectory + "Layer_Properties_Full.png").toExternalForm();
        Object image_url_1 = cl.getResource(iconDirectory + "Layer_Properties_Point.png").toExternalForm();
        Object image_url_2 = cl.getResource(iconDirectory + "Layer_Properties_Stroke.png").toExternalForm();
        Object image_url_3 = cl.getResource(iconDirectory + "Layer_Properties_Stroke_Vertices.png").toExternalForm();
        Object image_url_4 = cl.getResource(iconDirectory + "Layer_Properties_Fill.png").toExternalForm();
        Object image_url_5 = cl.getResource(iconDirectory + "Layer_Properties_Selection.png").toExternalForm();
        Object image_url_6 = cl.getResource(iconDirectory + "Layer_Properties_Labelling.png").toExternalForm();
        Object image_url_7 = cl.getResource(iconDirectory + "Layer_Properties_VertexAndSegmentLabellingOptions.png").toExternalForm();
        Object image_url_8 = cl.getResource(iconDirectory + "Layer_Properties_MarkingWithMeasure.png").toExternalForm();        
        Object image_url_9 = cl.getResource(iconDirectory + "Layer_Properties_Label_Position.png").toExternalForm();
        Object image_url_A = cl.getResource(iconDirectory + "Layer_Properties_Label_Styler.png").toExternalForm();
        Object image_url_B = cl.getResource(iconDirectory + "Layer_Properties_Rotation.png").toExternalForm();
        Object image_url_C = cl.getResource(iconDirectory + "Layer_Properties_Query.png").toExternalForm();
        HtmlHelp hh = new HtmlHelp(this.propertyManager.getMsg("HELP_TITLE"),
                                   this.propertyManager.getMsg("HELP_BORDER"),
                                   String.format(this.propertyManager.getMsg("HELP_CONTENT"),
                                                 image_url_0,image_url_1,image_url_2,image_url_3,
                                                 image_url_4,image_url_5,image_url_6,
                                                 image_url_7,image_url_8,
                                                 image_url_9,image_url_A,image_url_B,
                                                 image_url_C));
        hh.display();
    }                                       

    private void nameInputTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nameInputTFActionPerformed
        if (Strings.isEmpty(this.nameInputTF.getText()) ||
            this.nameInputTF.getText().trim().length() == 0) {
            this.nameInputTF.setText(this.layer.getVisibleName());
        }
    }//GEN-LAST:event_nameInputTFActionPerformed

    private void btnGTypeDiscoverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGTypeDiscoverActionPerformed
        DiscoverGeometryType discoveredType =
            new DiscoverGeometryType(true, 
                                     this.layer, 
                                     this.layer.getConnection(),
                                     this.layer.getSchemaName(),
                                     this.layer.getObjectName(),
                                     this.layer.getGeoColumn(),
                                     this.lblGType.getText());
        this.setAlwaysOnTop(false);
        discoveredType.setVisible(true);
        this.setAlwaysOnTop(true);
        if (discoveredType.wasCancelled())
            return;
        String layerGType = discoveredType.getGeometryValue();
        if (this.cmbGTypes.getComponentCount() != 0)
            this.cmbGTypes.setSelectedItem(layerGType);
        else
            this.cmbGTypes.insertItemAt(layerGType, 0);
    }//GEN-LAST:event_btnGTypeDiscoverActionPerformed

    private void btnDiscoverSRIDTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDiscoverSRIDTypeActionPerformed
        discoverSetSRIDType();
    }//GEN-LAST:event_btnDiscoverSRIDTypeActionPerformed

    private void queryApply(boolean _reload) {
        if ( this.layer instanceof SVQueryLayer ) {
            SVQueryLayer qLayer = (SVQueryLayer)this.layer;
            qLayer.setBufferDistance(this.tfQueryBufferDistance.getText());
            qLayer.setBuffered(this.cbQueryBufferGeometry.isSelected());
            qLayer.setDrawQueryGeometry(this.cbQueryShowQueryGeometry.isSelected());
            qLayer.setSdoOperator(Constants.SDO_OPERATORS.valueOf(this.cmbSdoOperators.getSelectedItem().toString().replace("SDO_","")));
            if (this.cmbSdoOperators.getSelectedItem().toString().equals(Constants.SDO_OPERATORS.RELATE.toString()) ) {
                List<String> selectedMasks = this.listRelateMasks.getSelectedValuesList();
                if (selectedMasks.size()==0) {
                    qLayer.setRelationshipMask("ANYINTERACT");
                } else {
                    String masks = selectedMasks.get(0);
                    for (int i=1;i<selectedMasks.size();i++) {
                        masks += "," + selectedMasks.get(i);
                    }
                    qLayer.setRelationshipMask(masks);
                }
            }
            if ( _reload  ) {
                this.layer.getSpatialView().getSVPanel().redraw();
            }
        }
    }

    private void SQLApply(boolean _reload) {
        this.layer.setSQL(this.sqlTA.getText());
        if ( _reload && ! (this.layer instanceof SVQueryLayer || this.layer instanceof SVGraphicLayer ) ) {
            this.layer.getSpatialView().getSVPanel().redraw();
        }
    }

    private void pointApply(boolean _reload) {
        this.layer.getStyling().setPointColor(this.pointColorButton.getBackground());
        this.layer.getStyling().setPointType(cmbPointTypes.getSelectedItem().toString().replace(" ","_").toUpperCase());

        if (this.rbPointColorSolid.isSelected()) {
            this.layer.getStyling().setPointColor(this.pointColor);
            this.layer.getStyling().setPointColorType(Styling.STYLING_TYPE.CONSTANT);
        } else if (this.rbPointColorColumn.isSelected() &&
                   !this.cmbPointColorColumns.getSelectedItem().toString().equals(LABEL_NONE)) {
            this.layer.getStyling().setPointColorType(Styling.STYLING_TYPE.COLUMN);
        } else if (this.rbPointColorRandom.isSelected()) {
            this.layer.getStyling().setPointColorType(Styling.STYLING_TYPE.RANDOM);
        }
        this.layer.getStyling().setPointColorColumn(this.cmbPointColorColumns.getSelectedItem().toString().equals(LABEL_NONE)
                                       ? null
                                       : this.cmbPointColorColumns.getSelectedItem().toString());
        if (this.rbPointSizeFixed.isSelected()) {
            this.layer.getStyling().setPointSizeType(Styling.STYLING_TYPE.CONSTANT);
            try {
                this.layer.getStyling().setPointSize(Integer.parseInt(this.pointSizeTF.getText()));
            } catch (Exception _e) {
                JOptionPane.showMessageDialog(null,
                                              this.propertyManager.getMsg("SPATIAL_LAYER_PROP_POINT_SIZE"),
                                              MainSettings.EXTENSION_NAME +
                                              " - " + this.DIALOG_LABEL,
                                              JOptionPane.ERROR_MESSAGE);
            }
        } else if (this.rbPointSizeColumn.isSelected() &&
                  !this.cmbPointSizeColumns.getSelectedItem().toString().equals(LABEL_NONE)) {
            this.layer.getStyling().setPointSizeType(Styling.STYLING_TYPE.COLUMN);
        }
        this.layer.getStyling().setPointSizeColumn(this.cmbPointSizeColumns.getSelectedItem().toString().equals(LABEL_NONE)
                                     ? null
                                     : this.cmbPointSizeColumns.getSelectedItem().toString());
        if ( _reload )
            this.layer.getSpatialView().getSVPanel().redraw();
    }

    private void selectionApply(boolean _reload) {
        
        float f = (float)MathUtils.roundToDecimals(1.0f - (((float)this.sldrSelectionTransLevel.getValue()) / 100.0f), 2);
        this.layer.getStyling().setSelectionTransLevel(f);
        
        // if color change, refresh tree structure
        this.layer.getStyling().setSelectionColor(this.selectShadeColor);
        this.layer.getStyling().setSelectionLineStrokeType("LINE_" + cmbSelectionLineStyles.getSelectedItem().toString().replace(" ","_").toUpperCase());
        try {
            int lWidth =
                Integer.parseInt(this.tfSelectionLineWidth.getText().trim());
            if (lWidth < 1) {
                throw new Exception("Invalid line width. It must be 1 or more.");
            }
            this.layer.getStyling().setSelectionLineWidth(lWidth);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                                          this.propertyManager.getMsg("SPATIAL_LAYER_PROP_LINE_WIDTH"),
                                          MainSettings.EXTENSION_NAME +
                                          " - " + this.DIALOG_LABEL,
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int pSize =
                Integer.parseInt(this.tfSelectionPointSize.getText().trim());
            this.layer.getStyling().setSelectionPointSize(pSize);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                                          this.propertyManager.getMsg("SPATIAL_LAYER_PROP_POINT_SIZE"),
                                          MainSettings.EXTENSION_NAME +
                                          " - " + this.DIALOG_LABEL,
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        if ( _reload ) {
            this.layer.getSpatialView().getSVPanel().redraw();
        }
    }
    
    private void strokeApply(boolean _reload) {
        
        float f = (float)MathUtils.roundToDecimals(1.0f - (((float)this.sldrStrokeTransLevel.getValue()) / 100.0f), 2);
        this.layer.getStyling().setLineTransLevel(f);

        // Save colour settings
        this.layer.getStyling().setLineColor(this.strokeColorButton.getBackground());
        if (this.rbStrokeColorNone.isSelected()) {
            this.layer.getStyling().setLineColorType(Styling.STYLING_TYPE.NONE);
        } else if (this.rbStrokeColorSolid.isSelected()) {
            this.layer.getStyling().setLineColor(this.lineColor);
            this.layer.getStyling().setLineColorType(Styling.STYLING_TYPE.CONSTANT);
        } else if (this.rbStrokeColorColumn.isSelected() &&
                   !this.cmbStrokeColorColumns.getSelectedItem().toString().equals(LABEL_NONE)) {
            this.layer.getStyling().setLineColorType(Styling.STYLING_TYPE.COLUMN);
        } else if (this.rbStrokeColorRandom.isSelected()) {
            this.layer.getStyling().setLineColorType(Styling.STYLING_TYPE.RANDOM);
        }
        this.layer.getStyling().setLineColorColumn(this.cmbStrokeColorColumns.getSelectedItem().toString().equals(LABEL_NONE)
                                       ? null
                                       : this.cmbStrokeColorColumns.getSelectedItem().toString());


        this.layer.getStyling().setLineStrokeType("LINE_" + cmbLineStyles.getSelectedItem().toString().replace(" ","_").toUpperCase());
        try {
            int lWidth = Integer.parseInt(this.lineWidthTF.getText().trim());
            this.layer.getStyling().setLineWidth(lWidth);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                                          this.propertyManager.getMsg("SPATIAL_LAYER_PROP_LINE_WIDTH"),
                                          MainSettings.EXTENSION_NAME +
                                          " - " + this.DIALOG_LABEL,
                                          JOptionPane.ERROR_MESSAGE);
        }
        this.layer.getStyling().setMarkGeoStart(this.cmbMarkGeoStart.getSelectedItem().toString().replace(" ","_").toUpperCase());
        this.layer.getStyling().setMarkGeoPoints(this.cmbMarkGeoPoints.getSelectedItem().toString().replace(" ","_").toUpperCase());  
        this.layer.getStyling().setSegmentArrow(this.cmbSegmentArrows.getSelectedItem().toString().replace(" ","_").toUpperCase());
        if ( _reload ) {
            this.layer.getSpatialView().getSVPanel().redraw();
        }
    }

    private void fillApply(boolean _reload) {
      
      float f = (float)MathUtils.roundToDecimals(1.0f - (((float)this.sldrShadeTransLevel.getValue()) / 100.0f), 2);
      this.layer.getStyling().setShadeTransLevel(f);

      if (this.noShadeRB.isSelected()) {
          this.layer.getStyling().setShadeType(Styling.STYLING_TYPE.NONE);
      } else if (this.fixShadeColorRB.isSelected()) {
          this.layer.getStyling().setShadeType(Styling.STYLING_TYPE.CONSTANT);
          this.layer.getStyling().setShadeColor(this.shadeColor);
      } else if (this.columnShadeColorRB.isSelected() &&
                 !this.cmbShadeColumns.getSelectedItem().toString().equals(LABEL_NONE)) {
          this.layer.getStyling().setShadeType(Styling.STYLING_TYPE.COLUMN);
      } else if (this.randomShadeRB.isSelected()) {
          this.layer.getStyling().setShadeType(Styling.STYLING_TYPE.RANDOM);
      } else
          this.layer.getStyling().setShadeType(Styling.STYLING_TYPE.NONE);
       this.layer.getStyling().setShadeColumn(this.cmbShadeColumns.getSelectedItem().toString().equals(LABEL_NONE) 
                              ? null 
                              : this.cmbShadeColumns.getSelectedItem().toString());
       
       if ( _reload ) {
           this.layer.getSpatialView().getSVPanel().redraw();
       }
    }

    private void labelApply(boolean _reload) {
        this.layer.getStyling().setLabelPosition(this.labelPosition);
        this.layer.getStyling().setLabelOffset(this.labelOffsetDistance);
        this.layer.getStyling().setLabelAttributes(new SimpleAttributeSet(this.labelAttributes));
        this.layer.getStyling().setLabelColumn(this.cmbLabelColumns.getSelectedItem().toString().equals(this.LABEL_NONE) 
                                  ? null 
                                  : this.cmbLabelColumns.getSelectedItem().toString());
        this.layer.getStyling().setGeometryLabelPosition(this.cmbGeometryLabelPosition.getSelectedItem().toString());
        
        this.layer.getStyling().setMarkLabelAttributes(new SimpleAttributeSet(this.markLabelAttributes));
        this.layer.getStyling().setMarkLabelPosition(this.markLabelPosition);
        this.layer.getStyling().setMarkLabelOffset(this.markOffsetDistance);
        this.layer.getStyling().setMarkVertex(Constants.getVertexLabelType(this.cmbVertexLabelContent.getSelectedItem().toString()));
        this.layer.getStyling().setMarkOriented(this.cbOrientVertexLabel.isSelected());
        this.layer.getStyling().setMarkSegment(this.cmbMarkSegment.getSelectedItem().toString());
        
        this.layer.getStyling().setTextLoScale(this.tfLoScale.getText());
        this.layer.getStyling().setTextHiScale(this.tfHiScale.getText());
        if ( _reload ) {
           this.layer.getSpatialView().getSVPanel().redraw();
        }
    }

    private void rotationApply(boolean _reload) {
        this.layer.getStyling().setRotationColumn(this.cmbRotationColumns.getSelectedItem().toString().equals(LABEL_NONE) 
                                     ? null 
                                     : this.cmbRotationColumns.getSelectedItem().toString());
        this.layer.getStyling().setRotationValue(this.rbDegrees.isSelected() 
                                    ? Constants.ROTATION_VALUES.DEGREES 
                                    : Constants.ROTATION_VALUES.RADIANS);
        this.layer.getStyling().setRotationTarget(this.cmbRotationTarget.getSelectedItem().toString());

        if ( _reload )
            this.layer.getSpatialView().getSVPanel().redraw();
    }

    private void cmbGTypesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbGTypesActionPerformed
        try {
            // Disable MinResolution searching when data is point data
            if ( cmbGTypes.getSelectedItem().toString().toUpperCase().contains(Constants.GEOMETRY_TYPES.POINT.toString().toUpperCase()) ) {
                chkMinResolution.setSelected(false);
                chkMinResolution.setEnabled(false);
            } else {
                chkMinResolution.setEnabled(true);
            }
        } catch (Exception e) {
        }
    }//GEN-LAST:event_cmbGTypesActionPerformed

    private void btnRotationApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRotationApplyActionPerformed
        rotationApply(true);
}//GEN-LAST:event_btnRotationApplyActionPerformed

    private void cmbRotationColumnsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbRotationColumnsActionPerformed
        if (this.cmbRotationColumns == null ||
                this.cmbRotationColumns.getItemCount() == 0 ||
                this.cmbRotationColumns.isEnabled() == false)
            return;
        if (!this.cmbRotationColumns.getSelectedItem().toString().equals(LABEL_NONE)) {
            this.setRotationTarget(Constants.ROTATE.NONE.toString());
        }
}//GEN-LAST:event_cmbRotationColumnsActionPerformed

    private void cmbLabelColumnsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cmbLabelColumnsItemStateChanged
        if (this.cmbLabelColumns == null)
            return;
        if (this.cmbLabelColumns.isEnabled() &&
                this.cmbLabelColumns.getSelectedItem()!=null) {
            if (this.cmbLabelColumns.getSelectedItem().toString().equals(LABEL_NONE)) {
                this.setRotationTarget(this.cmbRotationTarget.getSelectedItem().toString().toUpperCase().equals(Constants.ROTATE.LABEL.toString()) ?
                    Constants.ROTATE.NONE.toString() :
                    (this.cmbRotationTarget.getSelectedItem().equals(Constants.ROTATE.BOTH.toString()) ?
                        Constants.ROTATE.MARKER.toString() :
                        this.cmbRotationTarget.getSelectedItem().toString()));
            }
        }
}//GEN-LAST:event_cmbLabelColumnsItemStateChanged

    private void btnLabelStylingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLabelStylingActionPerformed
        // Get new stying information
        final LabelStyler afcDlg = new LabelStyler(new JFrame(), this.layer.getSpatialView().getMapPanel().getMapBackground());
        SimpleAttributeSet saa = (layer.getStyling().getLabelAttributes() == null)
                ? LabelStyler.getDefaultAttributes(this.layer.getSpatialView().getMapPanel().getMapBackground())
                : this.layer.getStyling().getLabelAttributes();
        afcDlg.setAttributes(saa);
        afcDlg.setModalityType(ModalityType.APPLICATION_MODAL);
        afcDlg.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        this.setAlwaysOnTop(false);
        afcDlg.setAlwaysOnTop(true);
        afcDlg.setVisible(true);
        this.setAlwaysOnTop(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setLabelStyling((SimpleAttributeSet)afcDlg.getAttributes());
            }
        });
}//GEN-LAST:event_btnLabelStylingActionPerformed

    private void btnPositionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPositionActionPerformed
        LabelPositionForm lpf = new LabelPositionForm(null, true, this.propertyManager);
        lpf.setLabelPosition(this.layer.getStyling().getLabelPosition());
        lpf.setLabelOffset(this.layer.getStyling().getLabelOffset());
        this.setAlwaysOnTop(false);
        lpf.setAlwaysOnTop(true);
        lpf.setVisible(true);
        this.setAlwaysOnTop(true);
        if (!lpf.wasCancelled()) {
            labelPosition = lpf.getLabelPosition();
            lblPosition.setText(this.propertyManager.getMsg("MARK_POSITION", labelPosition.toString()));
            labelOffsetDistance   = lpf.getLabelOffset();
            lblOffset.setText(this.propertyManager.getMsg("MARK_OFFSET", String.valueOf(labelOffsetDistance)));
        }
}//GEN-LAST:event_btnPositionActionPerformed

    private void btnMarkLabelStylingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMarkLabelStylingActionPerformed
        // Get new stying information
        final LabelStyler afcDlg = new LabelStyler(new JFrame(), layer.getSpatialView().getMapPanel().getMapBackground());
        SimpleAttributeSet saa = this.layer.getStyling().getMarkLabelAttributes();
        afcDlg.setAttributes(saa);
        afcDlg.setModalityType(ModalityType.APPLICATION_MODAL);
        afcDlg.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        this.setAlwaysOnTop(false);
        afcDlg.setAlwaysOnTop(true);
        afcDlg.setVisible(true);
        this.setAlwaysOnTop(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setMarkLabelStyling((SimpleAttributeSet)afcDlg.getAttributes());
            }
        });
}//GEN-LAST:event_btnMarkLabelStylingActionPerformed

    private void btnMarkLabelPositionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMarkLabelPositionActionPerformed
        LabelPositionForm lpf = new LabelPositionForm(null, true, this.propertyManager);
        lpf.setLabelPosition(this.layer.getStyling().getMarkLabelPosition());
        lpf.setLabelOffset  (this.layer.getStyling().getMarkLabelOffset());
        this.setAlwaysOnTop(false);
        lpf.setAlwaysOnTop(true);
        lpf.setVisible(true);
        this.setAlwaysOnTop(true);
        if (!lpf.wasCancelled()) {
            this.markLabelPosition = lpf.getLabelPosition();
            this.lblMarkPosition.setText(this.propertyManager.getMsg("MARK_POSITION", this.markLabelPosition.toString()));
            this.layer.getStyling().setMarkLabelPosition(this.markLabelPosition);
            
            this.markOffsetDistance   = lpf.getLabelOffset();
            this.lblMarkOffset.setText(this.propertyManager.getMsg("MARK_OFFSET", String.valueOf(this.markOffsetDistance)));
            this.layer.getStyling().setMarkLabelOffset(this.markOffsetDistance);
        }
    }//GEN-LAST:event_btnMarkLabelPositionActionPerformed

    private void btnLabelApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLabelApplyActionPerformed
        labelApply(true);
}//GEN-LAST:event_btnLabelApplyActionPerformed

    private void btnSelectionApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectionApplyActionPerformed
        selectionApply(true);
}//GEN-LAST:event_btnSelectionApplyActionPerformed

    private void cbSelectionActiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbSelectionActiveActionPerformed
        this.layer.getStyling().setSelectionActive(cbSelectionActive.isSelected());
}//GEN-LAST:event_cbSelectionActiveActionPerformed

    private void sldrSelectionLineWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldrSelectionLineWidthStateChanged
        this.tfSelectionLineWidth.setText(String.valueOf(this.sldrSelectionLineWidth.getValue()));
}//GEN-LAST:event_sldrSelectionLineWidthStateChanged

    private void sldrSelectionPointSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldrSelectionPointSizeStateChanged
        this.tfSelectionPointSize.setText(String.valueOf(this.sldrSelectionPointSize.getValue()));
}//GEN-LAST:event_sldrSelectionPointSizeStateChanged

    private void sldrSelectionTransLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldrSelectionTransLevelStateChanged
        this.lblSelectionTransLevel.setText(String.format(LABEL_TRANSPARENCY + "%3d", this.sldrSelectionTransLevel.getValue()));
}//GEN-LAST:event_sldrSelectionTransLevelStateChanged

    private void tfSelectionLineWidthFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfSelectionLineWidthFocusLost
        try {
            this.sldrSelectionLineWidth.setValue(Integer.valueOf(this.tfSelectionLineWidth.getText()));
        } catch (Exception e) {
        }
}//GEN-LAST:event_tfSelectionLineWidthFocusLost

    private void tfSelectionPointSizeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfSelectionPointSizeFocusLost
        try {
            this.sldrSelectionPointSize.setValue(Integer.valueOf(this.tfSelectionPointSize.getText()));
        } catch (Exception e) {
        }
}//GEN-LAST:event_tfSelectionPointSizeFocusLost

    private void btnSelectionColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectionColorActionPerformed
        Color backgroundColor =
                JColorChooser.showDialog(this, "Spatial properties - Selected fill color",
                this.layer.getStyling().getSelectionColor());
        if (backgroundColor != null) {
            this.selectShadeColor = backgroundColor;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    btnSelectionColor.setBackground(selectShadeColor);
                    btnSelectionColor.setForeground(Colours.highContrast(selectShadeColor));
                }
            });
        }
}//GEN-LAST:event_btnSelectionColorActionPerformed

    private void sldrShadeTransLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldrShadeTransLevelStateChanged
        this.lblShadeTransLevel.setText(String.format(LABEL_TRANSPARENCY + "%3d", this.sldrShadeTransLevel.getValue()));
}//GEN-LAST:event_sldrShadeTransLevelStateChanged

    private void columnShadeColorRBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_columnShadeColorRBActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cmbShadeColumns.setEnabled(true);
                fixShadeColorB.setEnabled(false);
            }
        });
}//GEN-LAST:event_columnShadeColorRBActionPerformed

    private void fixShadeColorBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixShadeColorBActionPerformed
        Color backgroundColor =
                JColorChooser.showDialog(this, "Spatial properties - Shade color",
                this.layer.getStyling().getShadeColor(Colours.getRGBa(Color.BLACK)));
        if (backgroundColor != null) {
            this.shadeColor = backgroundColor;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fixShadeColorB.setBackground(shadeColor);
                    fixShadeColorB.setForeground(Colours.highContrast(shadeColor));
                }
            });
        }
}//GEN-LAST:event_fixShadeColorBActionPerformed

    private void fixShadeColorRBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixShadeColorRBActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fixShadeColorB.setEnabled(true);
                cmbShadeColumns.setEnabled(false);
                cmbShadeColumns.setSelectedIndex(0);
            }
        });
}//GEN-LAST:event_fixShadeColorRBActionPerformed

    private void randomShadeRBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_randomShadeRBActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cmbShadeColumns.setSelectedIndex(0);
                cmbShadeColumns.setEnabled(false);
                fixShadeColorB.setEnabled(false);
            }
        });
}//GEN-LAST:event_randomShadeRBActionPerformed

    private void noShadeRBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noShadeRBActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cmbShadeColumns.setSelectedIndex(0);
                cmbShadeColumns.setEnabled(false);
                fixShadeColorB.setEnabled(false);
            }
        });
}//GEN-LAST:event_noShadeRBActionPerformed

    private void btnFillApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFillApplyActionPerformed
        fillApply(true);
}//GEN-LAST:event_btnFillApplyActionPerformed

    private void btnStrokeApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStrokeApplyActionPerformed
        strokeApply(true);
}//GEN-LAST:event_btnStrokeApplyActionPerformed

    private void lineWidthTFFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_lineWidthTFFocusLost
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    if (Strings.isEmpty(lineWidthTF.getText()) ) {
                        lineWidthTF.setText(String.valueOf(sldrLineWidth.getValue()));
                        return;
                    }
                    int sVal = Integer.valueOf(lineWidthTF.getText()).intValue();
                    sldrLineWidth.setValue(sVal);
                } catch (Exception e) {
                }
            }
        });
}//GEN-LAST:event_lineWidthTFFocusLost

    private void sldrLineWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldrLineWidthStateChanged
        this.lineWidthTF.setText(String.valueOf(this.sldrLineWidth.getValue()));
}//GEN-LAST:event_sldrLineWidthStateChanged

    private void rbStrokeColorColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbStrokeColorColumnActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                strokeColorButton.setEnabled(false);
                cmbStrokeColorColumns.setEnabled(true);
            }
        });
}//GEN-LAST:event_rbStrokeColorColumnActionPerformed

    private void rbStrokeColorRandomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbStrokeColorRandomActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                strokeColorButton.setEnabled(false);
                cmbStrokeColorColumns.setEnabled(false);
                cmbStrokeColorColumns.setSelectedIndex(0);
            }
        });
}//GEN-LAST:event_rbStrokeColorRandomActionPerformed

    private void rbStrokeColorSolidActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbStrokeColorSolidActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                strokeColorButton.setEnabled(true);
                cmbStrokeColorColumns.setEnabled(false);
                cmbStrokeColorColumns.setSelectedIndex(0);
            }
        });
}//GEN-LAST:event_rbStrokeColorSolidActionPerformed

    private void strokeColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strokeColorButtonActionPerformed
        Color backgroundColor =
                JColorChooser.showDialog(this, "Spatial properties - Stroke color",
                this.layer.getStyling().getLineColor(null));
        if (backgroundColor != null) {
            this.lineColor = backgroundColor;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    strokeColorButton.setBackground(lineColor);
                    strokeColorButton.setForeground(Colours.highContrast(lineColor));
                }
            });
        }
}//GEN-LAST:event_strokeColorButtonActionPerformed

    private void rbPointSizeColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbPointSizeColumnActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pointSizeTF.setEnabled(false);
                sldrPointSize.setEnabled(false);
                cmbPointSizeColumns.setEnabled(true);
            }
        });
}//GEN-LAST:event_rbPointSizeColumnActionPerformed

    private void sldrPointSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldrPointSizeStateChanged
        this.pointSizeTF.setText(String.valueOf(this.sldrPointSize.getValue()));
}//GEN-LAST:event_sldrPointSizeStateChanged

    private void rbPointSizeFixedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbPointSizeFixedActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pointSizeTF.setEnabled(true);
                sldrPointSize.setEnabled(true);
                cmbPointSizeColumns.setEnabled(false);
            }
        });
}//GEN-LAST:event_rbPointSizeFixedActionPerformed

    private void pointSizeTFFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_pointSizeTFFocusLost

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    if (Strings.isEmpty(pointSizeTF.getText()) ) {
                        pointSizeTF.setText(String.valueOf(sldrPointSize.getValue()));
                        return;
                    }
                    int sVal = Integer.valueOf(pointSizeTF.getText()).intValue();
                    sldrPointSize.setValue(sVal);
                } catch (Exception e) {
                }
            }
        });
}//GEN-LAST:event_pointSizeTFFocusLost

    private void rbPointColorSolidActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbPointColorSolidActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pointColorButton.setEnabled(true);
                cmbPointColorColumns.setEnabled(false);
                cmbPointColorColumns.setSelectedIndex(0);
            }
        });
}//GEN-LAST:event_rbPointColorSolidActionPerformed

    private void rbPointColorColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbPointColorColumnActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cmbPointColorColumns.setEnabled(true);
                pointColorButton.setEnabled(false);
            }
        });
}//GEN-LAST:event_rbPointColorColumnActionPerformed

    private void pointColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pointColorButtonActionPerformed
        Color backgroundColor =
                JColorChooser.showDialog(this, "Spatial properties - Point color",
                this.layer.getStyling().getPointColor(null));
        if (backgroundColor != null) {
            this.pointColor = backgroundColor;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    pointColorButton.setBackground(pointColor);
                    pointColorButton.setForeground(Colours.highContrast(pointColor));
                }
            });
        }
}//GEN-LAST:event_pointColorButtonActionPerformed

    private void rbPointColorRandomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbPointColorRandomActionPerformed
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pointColorButton.setEnabled(false);
                cmbPointColorColumns.setEnabled(false);
                cmbPointColorColumns.setSelectedIndex(0);
            }
        });
}//GEN-LAST:event_rbPointColorRandomActionPerformed

    private void btnPointApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPointApplyActionPerformed
        pointApply(true);
}//GEN-LAST:event_btnPointApplyActionPerformed

    private void btnSQLApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSQLApplyActionPerformed
        this.SQLApply(true);
}//GEN-LAST:event_btnSQLApplyActionPerformed

    private void chkMinResolutionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkMinResolutionActionPerformed
        // write change to layer's properties
        layer.setMinResolution(chkMinResolution.isSelected());
}//GEN-LAST:event_chkMinResolutionActionPerformed

    private void sqlTAFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_sqlTAFocusLost
        if (this.layer.getSQL().equals(this.sqlTA.getText()) == false) {
            // Save changes
            this.layer.setSQL(this.sqlTA.getText());
        }
}//GEN-LAST:event_sqlTAFocusLost

    private void btnQueryApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQueryApplyActionPerformed
        queryApply(true);
    }//GEN-LAST:event_btnQueryApplyActionPerformed

    private void cmbSdoOperatorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbSdoOperatorsActionPerformed
        try {
            // Enable/Disable listRelateMasks depending if selected element is RELATE
            if ( this.cmbSdoOperators.getSelectedItem().toString().equals("SDO_"+Constants.SDO_OPERATORS.RELATE.toString().toUpperCase()) ) {
                this.listRelateMasks.setEnabled(true);
            } else {
                this.listRelateMasks.setEnabled(false);
            }
        } catch (Exception e) {
        }
    }//GEN-LAST:event_cmbSdoOperatorsActionPerformed

    private void btnCopyQueryGeometryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCopyQueryGeometryActionPerformed
        if ( this.layer instanceof SVQueryLayer ) {
            SVQueryLayer qLayer = (SVQueryLayer)this.layer;
            Tools.doClipboardCopy(RenderTool.renderGeometryAsPlainText(qLayer.getGeometry(),Constants.TAG_MDSYS_SDO_GEOMETRY,Constants.bracketType.NONE,qLayer.getPrecision()));
        } else {
            LOGGER.warn(this.layer.getVisibleName() + " is not a query layer");
        }
    }//GEN-LAST:event_btnCopyQueryGeometryActionPerformed

    private void btnConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectionActionPerformed
        // 1. Save current selected connection
        //
        String currentDisplayName = (String)this.cmbConnections.getSelectedItem();
        // 2. Refresh/reload connections from SQL Developer
        //
        DatabaseConnections dbConns = DatabaseConnections.getInstance();
        dbConns.setConnections(true);
        if ( DatabaseConnections.getInstance().getConnectionCount()==0 ) {
            JOptionPane.showConfirmDialog(this,propertyManager.getMsg("ERROR_NO_CONNECTION_FOR","Metadata Manager"));
            return;
        }
        // Initialize connection pulldown to, possible, new selection
        //
        this.cmbConnections.setModel(dbConns.getComboBoxModel(dbConns.findDisplayName(currentDisplayName).connectionName));
        this.cmbConnections.setSelectedIndex(0);        
        if ( this.cmbConnections.getItemCount()>1 ) {
            for ( int i=0; i<this.cmbConnections.getItemCount(); i++ ) {
                DatabaseConnection dbConn = DatabaseConnections.getInstance().getConnectionAt(i);
                if ( dbConn!=null && dbConn.getDisplayName().equalsIgnoreCase(currentDisplayName) ) {
                    this.cmbConnections.setSelectedIndex(i);
                    break;
                }
            }
        }
    }//GEN-LAST:event_btnConnectionActionPerformed

    private void sldrStrokeTransLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldrStrokeTransLevelStateChanged
            this.lblStrokeTransLevel.setText(String.format(LABEL_TRANSPARENCY + "%3d", this.sldrStrokeTransLevel.getValue()));
    }//GEN-LAST:event_sldrStrokeTransLevelStateChanged

    private void btnCopy2ClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCopy2ClipboardActionPerformed
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection ss = new StringSelection(this.sqlTA.getText());
        clipboard.setContents(ss, ss);
        Toolkit.getDefaultToolkit().beep();
    }//GEN-LAST:event_btnCopy2ClipboardActionPerformed

    private void btnClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClipboardActionPerformed
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Envelope mbr = new Envelope(Integer.valueOf(this.tfPrecisionValue.getText()).intValue(),
                                                  Double.valueOf(this.lblLLX.getText()).doubleValue(),
                                                  Double.valueOf(this.lblLLY.getText()).doubleValue(),
                                                  Double.valueOf(this.lblURX.getText()).doubleValue(),
                                                  Double.valueOf(this.lblURY.getText()).doubleValue());
        StringSelection ss = new StringSelection(mbr.toSdoGeometry(this.sridTF.getText(),
                                                                   Integer.valueOf(this.tfPrecisionValue.getText()).intValue()));
        clipboard.setContents(ss, ss);
        Toolkit.getDefaultToolkit().beep();
    }//GEN-LAST:event_btnClipboardActionPerformed

    private void tfLoScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfLoScaleActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tfLoScaleActionPerformed

    private void cmbLineStylesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbLineStylesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbLineStylesActionPerformed

    // ******************************* Class methods ****************************************

    private void discoverSetSRIDType() {
        Constants.SRID_TYPE SRIDType = Tools.discoverSRIDType(this.layer.getConnection(), 
                                                              this.sridTF.getText());
        for (int i = 0; i < cmbSRIDType.getItemCount(); i++) {
            if (cmbSRIDType.getItemAt(i).equals(SRIDType.toString())) {
                cmbSRIDType.setSelectedIndex(i);
                break;
            }
        }
    }

    public void setLabelStyling(SimpleAttributeSet _saa) {
        this.labelAttributes = _saa;
        if (_saa != null) {
            Font newLabelFont = new Font(StyleConstants.getFontFamily(_saa),
                                         ((StyleConstants.isBold(_saa)
                                          ? Font.BOLD : Font.PLAIN) 
                                          &
                                          (StyleConstants.isItalic(_saa)
                                          ? Font.ITALIC : 0)),
                                         StyleConstants.getFontSize(_saa));
            this.lblStyledLabel.setFont(newLabelFont);
            // If we use the full Alpha of the returned colour, the pulldown text will become transparent and hard to read,
            // so let's only give it a "hint" when the alpha is < 100 to indicate we have an alpha applied
            //
            this.lblStyledLabel.setForeground(StyleConstants.getForeground(_saa).getAlpha() < 100
                                               ? Colours.setAlpha(StyleConstants.getForeground(_saa),175)
                                               : StyleConstants.getForeground(_saa));
            this.lblStyledLabel.setBackground(StyleConstants.getBackground(_saa).getAlpha() < 100
                                               ? Colours.setAlpha(StyleConstants.getBackground(_saa),175)
                                               : StyleConstants.getBackground(_saa));
            this.cmbLabelColumns.setFont(newLabelFont);
            this.cmbLabelColumns.setForeground(new Color(this.lblStyledLabel.getForeground().getRGB()));
            this.cmbLabelColumns.setBackground(new Color(this.cmbLabelColumns.getBackground().getRGB()));
        }
    }

    public void setMarkLabelStyling(SimpleAttributeSet _saa) {
        this.markLabelAttributes = _saa;
        if (_saa != null) {
            Font newLabelFont = new Font(StyleConstants.getFontFamily(_saa),
                                         ((StyleConstants.isBold(_saa)
                                          ? Font.BOLD : Font.PLAIN) 
                                          &
                                          (StyleConstants.isItalic(_saa)
                                          ? Font.ITALIC : 0)),
                                         StyleConstants.getFontSize(_saa));
            this.lblStyledMarkLabel.setFont(newLabelFont);
            // If we use the full Alpha of the returned colour, the pulldown text will become transparent and hard to read,
            // so let's only give it a "hint" when the alpha is < 100 to indicate we have an alpha applied
            //
            this.lblStyledMarkLabel.setForeground(StyleConstants.getForeground(_saa).getAlpha() < 100 
                                                  ? Colours.setAlpha(StyleConstants.getForeground(_saa),175) 
                                                  : StyleConstants.getForeground(_saa));
            this.lblStyledMarkLabel.setBackground(StyleConstants.getBackground(_saa).getAlpha() < 100 
                                                  ? Colours.setAlpha(StyleConstants.getBackground(_saa),175) 
                                                  : StyleConstants.getBackground(_saa));
        }
    }

    // ########################## FORM VARIABLES ########################################

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgArrows;
    private javax.swing.ButtonGroup bgMarkId;
    private javax.swing.ButtonGroup bgFillColor;
    private javax.swing.ButtonGroup bgPointColor;
    private javax.swing.ButtonGroup bgPointSize;
    private javax.swing.ButtonGroup bgRotation;
    private javax.swing.ButtonGroup bgStrokeColor;
    private javax.swing.JButton btnClipboard;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnConnection;
    private javax.swing.JButton btnCopy2Clipboard;
    private javax.swing.JButton btnCopyQueryGeometry;
    private javax.swing.JButton btnDiscoverSRIDType;
    private javax.swing.JButton btnFillApply;
    private javax.swing.JButton btnGTypeDiscover;
    private javax.swing.JButton btnHelp;
    private javax.swing.JButton btnLabelApply;
    private javax.swing.JButton btnLabelStyling;
    private javax.swing.JButton btnMarkLabelPosition;
    private javax.swing.JButton btnMarkLabelStyling;
    private javax.swing.JButton btnPointApply;
    private javax.swing.JButton btnPosition;
    private javax.swing.JButton btnQueryApply;
    private javax.swing.JButton btnRotationApply;
    private javax.swing.JButton btnSQLApply;
    private javax.swing.JButton btnSelectionApply;
    private javax.swing.JButton btnSelectionColor;
    private javax.swing.JButton btnStrokeApply;
    private javax.swing.JCheckBox cbOrientVertexLabel;
    private javax.swing.JCheckBox cbQueryBufferGeometry;
    private javax.swing.JCheckBox cbQueryShowQueryGeometry;
    private javax.swing.JCheckBox cbRecalculateMBR;
    private javax.swing.JCheckBox cbSelectionActive;
    private javax.swing.JCheckBox chkMinResolution;
    private javax.swing.JComboBox<String> cmbConnections;
    private javax.swing.JComboBox<String> cmbGTypes;
    private javax.swing.JComboBox<String> cmbGeometryLabelPosition;
    private javax.swing.JComboBox<String> cmbLabelColumns;
    private javax.swing.JComboBox<String> cmbLineStyles;
    private javax.swing.JComboBox<String> cmbMarkGeoPoints;
    private javax.swing.JComboBox<String> cmbMarkGeoStart;
    private javax.swing.JComboBox<String> cmbMarkSegment;
    private javax.swing.JComboBox<String> cmbPointColorColumns;
    private javax.swing.JComboBox<String> cmbPointSizeColumns;
    private javax.swing.JComboBox<String> cmbPointTypes;
    private javax.swing.JComboBox<String> cmbRotationColumns;
    private javax.swing.JComboBox<String> cmbRotationTarget;
    private javax.swing.JComboBox<String> cmbSRIDType;
    private javax.swing.JComboBox<String> cmbSdoOperators;
    private javax.swing.JComboBox<String> cmbSegmentArrows;
    private javax.swing.JComboBox<String> cmbSelectionLineStyles;
    private javax.swing.JComboBox<String> cmbShadeColumns;
    private javax.swing.JComboBox<String> cmbStrokeColorColumns;
    private javax.swing.JComboBox<String> cmbVertexLabelContent;
    private javax.swing.JRadioButton columnShadeColorRB;
    private javax.swing.JTextField fetchSizeTF;
    private javax.swing.JButton fixShadeColorB;
    private javax.swing.JRadioButton fixShadeColorRB;
    private javax.swing.JTextField geoColumnTF;
    private javax.swing.JLabel lblArrowPlacement;
    private javax.swing.JLabel lblDBConnection;
    private javax.swing.JLabel lblFetchSize;
    private javax.swing.JLabel lblFillSolidEnd;
    private javax.swing.JLabel lblFillTranspEnd;
    private javax.swing.JLabel lblGType;
    private javax.swing.JLabel lblGeomColumnName;
    private javax.swing.JLabel lblGeometryLabelPoint;
    private javax.swing.JLabel lblHiScale;
    private javax.swing.JLabel lblLLX;
    private javax.swing.JLabel lblLLY;
    private javax.swing.JLabel lblLabelColumn;
    private javax.swing.JLabel lblLayerName;
    private javax.swing.JLabel lblLineStyles;
    private javax.swing.JLabel lblLineWidth;
    private javax.swing.JLabel lblLoScale;
    private javax.swing.JLabel lblMarkOffset;
    private javax.swing.JLabel lblMarkPosition;
    private javax.swing.JLabel lblMarkSegment;
    private javax.swing.JLabel lblOffset;
    private javax.swing.JLabel lblPointStyle;
    private javax.swing.JLabel lblPosition;
    private javax.swing.JLabel lblPrecision;
    private javax.swing.JLabel lblQueryBufferDistance;
    private javax.swing.JLabel lblRelateMasks;
    private javax.swing.JLabel lblRotationColumn;
    private javax.swing.JLabel lblRotationTarget;
    private javax.swing.JLabel lblSRID;
    private javax.swing.JLabel lblSRIDType;
    private javax.swing.JLabel lblSdoOperator;
    private javax.swing.JLabel lblSelectionLineStyles;
    private javax.swing.JLabel lblSelectionLineWidth;
    private javax.swing.JLabel lblSelectionPointSize;
    private javax.swing.JLabel lblSelectionSolidEnd;
    private javax.swing.JLabel lblSelectionTransLevel;
    private javax.swing.JLabel lblSelectionTranspEnd;
    private javax.swing.JLabel lblShadeTransLevel;
    private javax.swing.JLabel lblStrokeSolid;
    private javax.swing.JLabel lblStrokeTransLevel;
    private javax.swing.JLabel lblStrokeTranspEnd;
    private javax.swing.JLabel lblStyledLabel;
    private javax.swing.JLabel lblStyledMarkLabel;
    private javax.swing.JLabel lblTolerance;
    private javax.swing.JLabel lblToleranceValue;
    private javax.swing.JLabel lblURX;
    private javax.swing.JLabel lblURY;
    private javax.swing.JLabel lblVertexAll;
    private javax.swing.JLabel lblVertexLabelContent;
    private javax.swing.JLabel lblVertexStart;
    private javax.swing.JTextField lineWidthTF;
    private javax.swing.JList<String> listRelateMasks;
    private javax.swing.JTextField nameInputTF;
    private javax.swing.JRadioButton noShadeRB;
    private javax.swing.JPanel pnlDetails;
    private javax.swing.JPanel pnlDisplayScale;
    private javax.swing.JPanel pnlFill;
    private javax.swing.JPanel pnlFillColor;
    private javax.swing.JPanel pnlFillTransparency;
    private javax.swing.JPanel pnlFillTransparencyInner;
    private javax.swing.JPanel pnlLabel;
    private javax.swing.JPanel pnlLabelGeometry;
    private javax.swing.JPanel pnlLabelPosition;
    private javax.swing.JPanel pnlLabelStyle;
    private javax.swing.JPanel pnlLayerExtent;
    private javax.swing.JPanel pnlMBR;
    private javax.swing.JPanel pnlMark;
    private javax.swing.JPanel pnlMarkPositionOffset;
    private javax.swing.JPanel pnlMarkStyle;
    private javax.swing.JPanel pnlPoint;
    private javax.swing.JPanel pnlPointColor;
    private javax.swing.JPanel pnlPointSize;
    private javax.swing.JTabbedPane pnlProperties;
    private javax.swing.JPanel pnlQueryLayer;
    private javax.swing.JPanel pnlRotation;
    private javax.swing.JPanel pnlSQL;
    private javax.swing.JPanel pnlSelection;
    private javax.swing.JPanel pnlSelectionTransparency;
    private javax.swing.JPanel pnlStroke;
    private javax.swing.JPanel pnlStrokeColor;
    private javax.swing.JPanel pnlStrokeSizeStyle;
    private javax.swing.JPanel pnlStrokeTransparency;
    private javax.swing.JPanel pnlVertexlMarking;
    private javax.swing.JButton pointColorButton;
    private javax.swing.JTextField pointSizeTF;
    private javax.swing.JRadioButton randomShadeRB;
    private javax.swing.JRadioButton rbDegrees;
    private javax.swing.JRadioButton rbPointColorColumn;
    private javax.swing.JRadioButton rbPointColorRandom;
    private javax.swing.JRadioButton rbPointColorSolid;
    private javax.swing.JRadioButton rbPointSizeColumn;
    private javax.swing.JRadioButton rbPointSizeFixed;
    private javax.swing.JRadioButton rbRadians;
    private javax.swing.JRadioButton rbStrokeColorColumn;
    private javax.swing.JRadioButton rbStrokeColorNone;
    private javax.swing.JRadioButton rbStrokeColorRandom;
    private javax.swing.JRadioButton rbStrokeColorSolid;
    private javax.swing.JScrollPane scrollSQLTA;
    private javax.swing.JScrollPane scrpQueryRelateMasks;
    private javax.swing.JSlider sldrLineWidth;
    private javax.swing.JSlider sldrPointSize;
    private javax.swing.JSlider sldrSelectionLineWidth;
    private javax.swing.JSlider sldrSelectionPointSize;
    private javax.swing.JSlider sldrSelectionTransLevel;
    private javax.swing.JSlider sldrShadeTransLevel;
    private javax.swing.JSlider sldrStrokeTransLevel;
    private javax.swing.JTextArea sqlTA;
    private javax.swing.JTextField sridTF;
    private javax.swing.JButton strokeColorButton;
    private javax.swing.JTextField tfHiScale;
    private javax.swing.JTextField tfLoScale;
    private javax.swing.JTextField tfMBRFiller;
    private javax.swing.JTextField tfPrecisionValue;
    private javax.swing.JTextField tfQueryBufferDistance;
    private javax.swing.JTextField tfSelectionLineWidth;
    private javax.swing.JTextField tfSelectionPointSize;
    // End of variables declaration//GEN-END:variables

    // ############################ CLASS METHODS #####################################

    private void processBtnClose()
    {
        // Save selected connection
        if (cmbConnections.getItemCount()>0 ) {
            String displayName = (String)this.cmbConnections.getSelectedItem();
            if (!Strings.isEmpty(displayName)) {
                DatabaseConnection dc = DatabaseConnections.getInstance().findDisplayName(displayName);
                if (dc != null ) {
                    this.layer.setConnection(dc.getConnectionName());
                }
            }
        }

        // layer screen name change?
        if (this.nameInputTF.getText().equals(this.layer.getVisibleName()) ==
            false) {
            // is new name valid?
            this.layer.setVisibleName(this.nameInputTF.getText().trim());
            // Refresh LayerNode visible text via 
            this.layer.setNumberOfFeatures(this.layer.getNumberOfFeatures());
        }

        String geoCol = this.geoColumnTF.getText();
        if (geoCol.equals(this.layer.getGeoColumn()) == false) {
            // geometry column has change
            this.layer.setGeoColumn(geoCol.trim());
        }
        layer.setPrecision(tfPrecisionValue.getText());
        if (!cmbSRIDType.getSelectedItem().toString().equalsIgnoreCase(layer.getSRIDType().toString()))
            layer.setSRIDType(cmbSRIDType.getSelectedItem().toString());

        if (cmbGTypes.getSelectedItem().equals(this.layer.getGeometryType()) ==
            false) {
            // geometry column has change
            this.layer.setGeometryType(cmbGTypes.getSelectedItem().toString());
        }

        // MBR Recalculation
        this.layer.setMBRRecalculation(this.cbRecalculateMBR.isSelected());

        // ============== SQL Properties
        this.SQLApply(false);

        // ============== Label Properties
        this.labelApply(false);

        // ============== Rotation Properties
        this.rotationApply(false);
        
        // ============== Point Properties
        this.pointApply(false);
      
        // ============== Line Properties
        this.strokeApply(false);

        // ============== Shade Properties
        this.fillApply(false);

        // ============== Selection Properties
        this.selectionApply(false);

        // ============== Query Layer Properties
        this.queryApply(false);

        // ============== SQL Properties

        this.layer.setResultFetchSize(Integer.parseInt(Strings.isEmpty(this.fetchSizeTF.getText()) ?
                                                       String.valueOf(this.SVSpatialLayerPreferences.getFetchSize()) :
                                                       this.fetchSizeTF.getText()));
        if ( ! cmbGTypes.getSelectedItem().toString().toUpperCase().contains(Constants.GEOMETRY_TYPES.POINT.toString().toUpperCase()) )
            this.layer.setMinResolution(this.chkMinResolution.isSelected());
        this.layer.setSQL(this.sqlTA.getText());

        // ============== End SQL Properties

        this.layer.getSpatialView().getSVPanel().getViewLayerTree().refreshNode(this.layer.getLayerName());
        
        this.setVisible(false);

        // save new settings on disk
        MainSettings.getInstance().save();

    }

    private String formatOrd(double _value, int _precision) {
        DecimalFormat dFormat;
        // Don't use NLS settings as this may use comma as decimal separator which is not supported due to SDO_ORDINATE_ARRAY using comma as separator
        // dFormat = Tools.getNLSDecimalFormat(_precision, SVSpatialLayerPreferences.getGroupingSeparator());
        dFormat = Tools.getDecimalFormatter(_precision); 
        return dFormat.format(_value).toString();
    }

	public javax.swing.ButtonGroup getBgArrows() {
		return bgArrows;
	}

	public void setBgArrows(javax.swing.ButtonGroup bgArrows) {
		this.bgArrows = bgArrows;
	}

	public javax.swing.ButtonGroup getBgMarkId() {
		return bgMarkId;
	}

	public void setBgMarkId(javax.swing.ButtonGroup bgMarkId) {
		this.bgMarkId = bgMarkId;
	}

}
