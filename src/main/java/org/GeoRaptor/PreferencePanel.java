package org.GeoRaptor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.GeoRaptor.layout.VerticalFlowLayout;
import org.GeoRaptor.layout.XYConstraints;
import org.GeoRaptor.layout.XYLayout;

import org.GeoRaptor.Preferences;
import org.GeoRaptor.PreferencePanel;
import org.GeoRaptor.OracleSpatial.SRID.SRIDPanel;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.geotools.util.logging.Logger;

import oracle.ide.panels.DefaultTraversablePanel;
import oracle.ide.panels.TraversableContext;
import oracle.ide.panels.TraversalException;

/**
 * 
 * @author Bessie Gong 
 * @version 24 Jul 2019
 * The panel of preference
 *
 */
public class PreferencePanel extends DefaultTraversablePanel {
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.PreferencesPanel");

	private static PreferencePanel classInstance;
	private Preferences prefs = null;
	protected ResourceBundle rBundle = ResourceBundle.getBundle("org.GeoRaptor.resource.PreferencesPanel",
			Locale.getDefault());

	private InputVerifier verifyTableCountLimit = null;
	private InputVerifier verifyImageDimension = null;
	private InputVerifier verifySRID = null;
	private InputVerifier verifyQueryLimit = null;
	private InputVerifier verifyMbrAspectRatioLimit = null;

	private Color mapBackground = new Color(0, 0, 255);
	private Color featureColour = new Color(255, 0, 0);
	private Color orphanColour = new Color(255, 102, 102);
	private Color missingColour = new Color(153, 153, 225);
	private Color correctColour = new Color(153, 255, 153);
	private Color highlightColour = new Color(255, 165, 0);
	private Color selectionColour = new Color(255, 255, 0);

	/*************************************
	 * User Interface Widgets
	 **/

	private JTabbedPane tpPreferences = new JTabbedPane();

	private JPanel pnlSpatialView = new JPanel();

	private JLabel lblNewLayerPosition = new JLabel();
	private JComboBox cmbNewLayerPosition = new JComboBox(new DefaultComboBoxModel(new String[] {
			Constants.layerPositionType.TOP.toString(), Constants.layerPositionType.BOTTOM.toString() }));
	private JLabel lblTOCPosition = new JLabel();
	private JComboBox cmbTOCPosition = new JComboBox(
			new DefaultComboBoxModel(new String[] { JSplitPane.LEFT.toUpperCase(), JSplitPane.RIGHT.toUpperCase() }));

	private JCheckBox cbDrawQueryGeometry = new JCheckBox();
	private JCheckBox cbSQLSchemaPrefix = new JCheckBox();
	private JCheckBox cbNumberOfFeatures = new JCheckBox();
	private JCheckBox cbMapScale = new JCheckBox();
	private JButton btnMapBackground = new JButton();
	private JButton btnFeatureColour = new JButton();

	private JLabel lblSearchPixels = new JLabel();
	private JSlider sldrSearchPixels = new JSlider();

	private JLabel lblMBRSaveSize = new JLabel();
	private JSlider sldrMBRSaveSize = new JSlider();

	private JCheckBox cbMinResolution = new JCheckBox();
	private JCheckBox cbSchemaPrefix = new JCheckBox();

	private JLabel lblPanZoomChange = new JLabel();
	private JSlider sldrPanZoomChange = new JSlider();

	private XYLayout xYLayoutSpatial = new XYLayout();
	private XYLayout xYLayoutMBR = new XYLayout();

	// Second tab - Visualisation
	//
	private JPanel pnlVisualisation = new JPanel();
	private XYLayout xYLayoutVisualisation = new XYLayout();

	private JPanel pnlSdoObjectTextDisplay = new JPanel();
	private XYLayout xYObjectTextLayout = new XYLayout();
	private JCheckBox cbColourSdoGeometry = new JCheckBox();
	private JLabel lblSdoGeometryBracketing = new JLabel();
	private JComboBox cmbSdoGeometryBracket = new JComboBox(Constants.getBracketTypeCombo());
	private JComboBox cmbSdoGeometryVisualFormat = new JComboBox(Constants.getRenderTypeCombo());
	private JCheckBox cbSdoGeomCoordNumbering = new JCheckBox();
	private JCheckBox cbSdoGeometryFormat = new JCheckBox();
	private JLabel lblSdoGeometryVisualFormat = new JLabel();
	private JCheckBox cbColourDimInfo = new JCheckBox();

	private ButtonGroup bgSelection = new ButtonGroup();

	private JCheckBox cbGroupingSeparator = new JCheckBox();

	private JCheckBox cbPreviewHorizontalLabels = new JCheckBox();
	private JCheckBox cbPreviewVertexNumbering = new JCheckBox();
	private JTextField tfImageHeight = new JTextField();
	private JTextField tfImageWidth = new JTextField();
	private JLabel lblImageWidth = new JLabel();

	private JPanel pnlMapColours = new JPanel();
	private XYLayout xYMapColoursLayout = new XYLayout();
	private JButton btnOrphanColour = new JButton();
	private JButton btnMissingColour = new JButton();
	private JButton btnCorrectColour = new JButton();
	private JButton btnSelectionColour = new JButton();

	private JPanel pnlMetadataColours = new JPanel();
	private XYLayout xYMetadataColoursLayout = new XYLayout();
	private JButton btnHighlightColour = new JButton();
	private JCheckBox cbPrefixWithMDSYS = new JCheckBox();

	// Third tab - Miscellaneous
	//
	private JPanel pnlMiscellaneous = new JPanel();
	private JTextField tfQueryLimit = new JTextField();
	private JLabel lblQueryLimit = new JLabel();
	private JCheckBox cbDebug = new JCheckBox();
	private XYLayout xYLayoutMiscellaneous = new XYLayout();
	private JLabel lblAffix = new JLabel();
	private JTextField tfAffixString = new JTextField();
	private JRadioButton rbPrefix = new JRadioButton();
	private JRadioButton rbSuffix = new JRadioButton();
	private ButtonGroup bgAffix = new ButtonGroup();
	private JTextField tfSRID = new JTextField();
	private JButton btnSRID = new JButton();
	private VerticalFlowLayout verticalFlowLayout1 = new VerticalFlowLayout();
	private JLabel lblTableCountLimit = new JLabel();
	private JTextField tfTableCountLimit = new JTextField();
	private JCheckBox cbLogSearchStats = new JCheckBox();
	private JCheckBox cbSdoNN = new JCheckBox();
	private JCheckBox cbFastPicklerConversion = new JCheckBox();
	private JPanel pnlMbrSource = new JPanel();
	private JRadioButton rbLayerMBRIndex = new JRadioButton();
	private JRadioButton rbLayerMBRMetadata = new JRadioButton();
	private ButtonGroup bgLayerMBR = new ButtonGroup();
	private JPanel spatialndexPanel = new JPanel();
	private XYLayout xYLayout1 = new XYLayout();
	private JTextField tfRefreshImage = new JTextField();
	private JLabel lblRefreshImage = new JLabel();
	private JTextField tfDefFetchSize = new JTextField();
	private JLabel lblDefFetchSize = new JLabel();
	private JLabel lblDefaultSRID = new JLabel();

	// Fourth tab - Defaults
	//
	private JPanel pnlDefaults = new JPanel();
	private XYLayout xYLayoutDefaults = new XYLayout();
	private JPanel pnlImportExport = new JPanel();
	private XYLayout xYLayoutImportExport = new XYLayout();
	private JPanel pnlNullValueSubstitution = new JPanel();
	private XYLayout xYLayoutNullValueSubstitution = new XYLayout();
	private JTextField tfNullString = new JTextField();
	private JLabel lblNullString = new JLabel();
	private JLabel lblNullNumber = new JLabel();
	private JFormattedTextField tfNullNumber = new JFormattedTextField(Tools.getNLSDecimalFormat(-1, false));
	private JFormattedTextField tfNullInteger = new JFormattedTextField(
			new JFormattedTextField(NumberFormat.getIntegerInstance()));
	private JTextField tfNullDate = new JTextField();
	private JLabel lblNullDate = new JLabel();
	private JLabel lblNullInteger = new JLabel();
	private JCheckBox cbDbaseNullWriteString = new JCheckBox();

	private JPanel pnlColumnShorten = new JPanel();
	private XYLayout xyLayoutColumnShorten = new XYLayout();
	private JLabel lblDBFColumnTruncation = new JLabel();
	private ButtonGroup bgColumnShorten = new ButtonGroup();
	private JRadioButton rbBegin10 = new JRadioButton();
	private JRadioButton rbLast10 = new JRadioButton();

	/*************************************
	 * Internationalisation Strings
	 **/

	protected String sTpMiscellaneous = "Miscellaneous";
	protected String sTpSpatialView = "Spatial View (*Restart)";
	protected String sTpVisualisation = "Visualisation";
	protected String sTpImportExport = "Import/Export";

	protected String sBtnSRID = "Select SRID";
	protected String sCbColourDimInfo = "Colour DimInfo elements:";
	protected String sCbColourDimInfoTT = "Activate colouring of DimInfo structures.";
	protected String sCbColourSdoGeometry = "Colour SDO_GEOMETRY elements?";
	protected String sCbColourSdoGeometryTT = "Render Sdo_Geometry components in colour (or BW) in tabular views";
	protected String sCbMinResolution = "SDO_FILTER pixel filtering (not points)";
	protected String sCbSchemaPrefix = "Prefix Initial layer name with schema?";
	protected String sCbSchemaPrefixTT = "If ticked, the initial visible layer name will be prefixed with the schema eg SCHEMA.TABLE.COLUMN, otherwise just TABLE.COLUMN";
	protected String sCbSdoGeomCoordNumbering = "Number SDO_ORDINATE_ARRAY coordinates?";
	protected String sCbSdoGeomCoordNumberingTT = "Number Coordinate groups using subscripts eg (1,2,3)1, (4,5,6)2 etc";
	protected String sCbSdoGeometryFormat = "Render directly from SDO_GEOMETRY:";
	protected String sCbLogSearchStats = "Log SQL Statements:";
	protected String sCbSdoGeometryFormatTT = "When rendering geometries use native SDO_GEOMETRY (don't convert to JGeometry).";
	protected String sCmbSdoGeometryBracketTT = "Group Sdo_Elem_Info elements into triplets and Sdo_Ordinates into Coordinate groupings using brackets ()";
	protected String sCmbSdoGeometryVisualFormatTT = "Render Sdo_Geometry in tabular views in GML, KML, WKT or Sdo_GEOMETRY formats";
	protected String sCbRandomRendering = "Randomly Style New Layers:";
	protected String sCmbTOCPositionTT = "Layer's position when added to layer list:";
	protected String sLblAffix = "Prefix/Suffix String:";
	protected String sLblDefFetchSize = "SELECT Fetch Size (rows):";
	protected String sLblFeatureColour = "Colour of measure/create feature:";
	protected String sLblFeatureColourTT = "When creating a spatial feature for query, clipboard or as part of a measurement, the colour can be set using this propery.";
	protected String sLblMBRSaveSize = "Maximum window saves (*): 20";
	protected String sLblMapBackground = "Default Map Background (*):";
	protected String sLblNewLayerPosition = "Layer's position when added to layer list:";
	protected String sLblPanZoomChange = "Pan/Zoom change: 10025% of current window";
	protected String sLblRefreshImage = "Refresh Image time (ms):";
	protected String sLblSdoGeometryBracketing = "SDO_ORDINATE_ARRAY coordinate bracket:";
	protected String sLblSdoGeometryVisualFormat = "SDO_GEOMETRY display format:";
	protected String sLblSearchPixels = "Query distance in pixels: 10";
	protected String sLblTOCPosition = "View/Layer Tree Panel position(*):";
	protected String sPnlMbrSource = "Layer MBR Source";
	protected String sDefSRID = "Default SRIDS";
	protected String sPnlSelections = "Selection rendering and drawing";
	protected String sRbLayerMBRIndex = "RTree Index (projected only)";
	protected String sRbLayerMBRMetadata = "Metadata";
	protected String sRbPrefix = "Prefix";
	protected String sRbSuffix = "Suffix";
	protected String sLblTableCountLimit = "Table Count Limit:";
	protected String sLblQueryLimit = "Search/Display/Query Limit (feats):";
	protected String sLblTableCountLimitTT = "Limit to be applied when drawing layers without spatial indexes or when sampling.";
	protected String sCbSdoNN = "Identify using SDO_NN:";
	protected String sCbSdoNNTT = "When identifying features use SDO_NN or SDO_NN_DISTANCE";
	protected String sPnlNullValueSubstitution = "NULL Value Substitution";
	protected String sLblNullString = "String (Varchar etc) NULL Value:";
	protected String sLblNullNumber = "Number NULL Value:";
	protected String sLblNullInteger = "Integer NULL Value:";
	protected String sLblNullDate = "Date / Timestamp NULL Value:";
	protected String sLblPreviewImageWidth = "Image (Width,Height) (";
	protected String sLblPreviewImageHeight = "Image Height";
	protected String sCbPreviewVertexNumbering = "Number Vertices";
	protected String sCbPreviewHorizontalLabels = "Horizontal Labels";
	protected String sTfNullStringTT = "Value to write when text column is NULL";
	protected String sTfNullNumberTT = "Value to write when number column is NULL";
	protected String sTfNullIntegerTT = "Value to write when integer column is NULL";
	protected String sTfNullDateTT = "Value to write when Date/Timestamp column is null";
	protected String sCbDbaseNullWriteString = "DBase Write Empty String (all Fields):";
	protected String sCbPrefixWithMDSYS = "Prefix SDO Types With MDSYS";
	protected String sCbFastPicklerConversion = "Fast conversion of SDO_GEOMETRY:";
	protected String sCbMapScale = "Display 1:xxxx Map Scale";
	protected String sCbNumberOfFeatures = "Display Layer's Feature Count?";

	protected String sLblDBFColumnTruncation = "Extract DBF 10 char name from:";
	protected String sRbBegin10 = "First 10 Chars";
	protected String sRbBegin10TT = "OBK__FLACHEN__BIOTOPTYP_ => OBK__FLACH";
	protected String sRbLast10 = "Last 10 Chars";
	protected String sRbLast10TT = "OBK__FLACHEN__BIOTOPTYP_ => BIOTOPTYP_";
	protected String sTfQueryLimitTT = "Value must be an integer value >= 0 (0 means no limit is applied).";

	private JCheckBox cbRandomRendering = new JCheckBox();
	private JLabel lblGeometryColumnName = new JLabel();
	private JTextField tfGeometryColumnName = new JTextField();

	private JLabel lblMinMbrAspectRatio = new JLabel();
	protected String sLblMinMbrAspectRatio = "Minimum MBR Aspect Ratio (def 3.0):";
	private JTextField tfMinMbrAspectRatio = new JTextField();

	private JPanel pnlPreviewImage = new JPanel();
	private XYLayout xyPreviewImage = new XYLayout();

	private JComboBox cmbPreviewVertexMark = new JComboBox();
	private JLabel lblVertexMarker = new JLabel();
	private JLabel lblWidthHeightPixels = new JLabel();
	private JLabel lblComma = new JLabel();
	private JSlider sldrVertexMarkSize = new JSlider();
	private JLabel lblMarkerSize = new JLabel();
	private JComboBox cmbShapePolygonOrientation = new JComboBox(
			new DefaultComboBoxModel(new String[] { Constants.SHAPE_POLYGON_ORIENTATION.ORACLE.toString(),
					Constants.SHAPE_POLYGON_ORIENTATION.INVERSE.toString(),
					Constants.SHAPE_POLYGON_ORIENTATION.CLOCKWISE.toString(),
					Constants.SHAPE_POLYGON_ORIENTATION.ANTICLOCKWISE.toString() }));
	private JLabel jLabel1 = new JLabel();

	public PreferencePanel() {
		super();
		try {
			jbInit();
			loadPreferenceStrings();
			applyPreferenceStrings();
			setVerifiers();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static PreferencePanel getInstance() {
		if (PreferencePanel.classInstance == null) {
			PreferencePanel.classInstance = new PreferencePanel();
		}
		return PreferencePanel.classInstance;
	}

	private static Preferences getPreferences(TraversableContext tc) {
		return Preferences.getInstance(tc.getPropertyStorage());
	}

	private void loadPreferenceStrings() {
		sTpMiscellaneous = Resources.getString("sTpMiscellaneous");
		sTpSpatialView = Resources.getString("sTpSpatialView");
		sTpVisualisation = Resources.getString("sTpVisualisation");
		sTpImportExport = Resources.getString("sTpImportExport");

		sBtnSRID = Resources.getString("sBtnSRID");
		sCbColourDimInfo = Resources.getString("sCbColourDimInfo");
		sCbColourDimInfoTT = Resources.getString("sCbColourDimInfoTT");
		sCbColourSdoGeometry = Resources.getString("sCbColourSdoGeometry");
		sCbColourSdoGeometryTT = Resources.getString("sCbColourSdoGeometryTT");
		sCbMinResolution = Resources.getString("sCbMinResolution");
		sCbSchemaPrefix = Resources.getString("sCbSchemaPrefix");
		sCbSchemaPrefixTT = Resources.getString("sCbSchemaPrefixTT");
		sCbSdoGeomCoordNumbering = Resources.getString("sCbSdoGeomCoordNumbering");
		sCbSdoGeomCoordNumberingTT = Resources.getString("sCbSdoGeomCoordNumberingTT");
		sCbSdoGeometryFormat = Resources.getString("sCbSdoGeometryFormat");
		sCbSdoGeometryFormatTT = Resources.getString("sCbSdoGeometryFormatTT");
		sCmbSdoGeometryBracketTT = Resources.getString("sCmbSdoGeometryBracketTT");
		sCmbSdoGeometryVisualFormatTT = Resources.getString("sCmbSdoGeometryVisualFormatTT");
		sCbRandomRendering = Resources.getString("sCbRandomRendering");
		sCbLogSearchStats = Resources.getString("sCbLogSearchStats");
		sCmbTOCPositionTT = Resources.getString("sCmbTOCPositionTT");
		sLblAffix = Resources.getString("sLblAffix");
		sLblDefFetchSize = Resources.getString("sLblDefFetchSize");
		sLblFeatureColour = Resources.getString("sLblFeatureColour");
		sLblFeatureColourTT = Resources.getString("sLblFeatureColourTT");
		sLblMBRSaveSize = Resources.getString("sLblMBRSaveSize");
		sLblMapBackground = Resources.getString("sLblMapBackground");
		sLblNewLayerPosition = Resources.getString("sLblNewLayerPosition");
		sLblPanZoomChange = Resources.getString("sLblPanZoomChange");
		sLblRefreshImage = Resources.getString("sLblRefreshImage");
		sLblSdoGeometryBracketing = Resources.getString("sLblSdoGeometryBracketing");
		sLblSdoGeometryVisualFormat = Resources.getString("sLblSdoGeometryVisualFormat");
		sLblSearchPixels = Resources.getString("sLblSearchPixels");
		sLblTOCPosition = Resources.getString("sLblTOCPosition");
		sPnlMbrSource = Resources.getString("sPnlMbrSource");
		sDefSRID = Resources.getString("sDefSRID");
		sPnlSelections = Resources.getString("sPnlSelections");
		sRbLayerMBRIndex = Resources.getString("sRbLayerMBRIndex");
		sRbLayerMBRMetadata = Resources.getString("sRbLayerMBRMetadata");
		sRbPrefix = Resources.getString("sRbPrefix");
		sRbSuffix = Resources.getString("sRbSuffix");
		sLblTableCountLimit = Resources.getString("sLblTableCountLimit");
		sLblQueryLimit = Resources.getString("sLblQueryLimit");
		sLblTableCountLimitTT = Resources.getString("sLblTableCountLimitTT");
		sCbSdoNN = Resources.getString("sCbSdoNN");
		sCbSdoNNTT = Resources.getString("sCbSdoNNTT");
		sPnlNullValueSubstitution = Resources.getString("sPnlNullValueSubstitution");
		sLblNullString = Resources.getString("sLblNullString");
		sLblNullNumber = Resources.getString("sLblNullNumber");
		sLblNullInteger = Resources.getString("sLblNullInteger");
		sLblNullDate = Resources.getString("sLblNullDate");
		sTfNullStringTT = Resources.getString("sTfNullStringTT");
		sTfNullNumberTT = Resources.getString("sTfNullNumberTT");
		sTfNullDateTT = Resources.getString("sTfNullDateTT");
		sTfNullIntegerTT = Resources.getString("sTfNullIntegerTT");
		sCbDbaseNullWriteString = Resources.getString("sCbDbaseNullWriteString");
		sCbPrefixWithMDSYS = Resources.getString("sCbPrefixWithMDSYS");
		sCbFastPicklerConversion = Resources.getString("sCbFastPicklerConversion");
		sCbMapScale = Resources.getString("sCbMapScale");
		sCbNumberOfFeatures = Resources.getString("sCbNumberOfFeatures");
		sLblDBFColumnTruncation = Resources.getString("sLblDBFColumnTruncation");
		sRbBegin10 = Resources.getString("sRbBegin10");
		sRbBegin10TT = Resources.getString("sRbBegin10TT");
		sRbLast10 = Resources.getString("sRbLast10");
		sRbLast10TT = Resources.getString("sRbLast10TT");
		sTfQueryLimitTT = Resources.getString("sTfQueryLimitTT");
		sLblPreviewImageWidth = Resources.getString("sLblPreviewImageWidth");
		sLblPreviewImageHeight = Resources.getString("sLblPreviewImageHeight");
		sCbPreviewVertexNumbering = Resources.getString("sCbPreviewVertexNumbering");
		sCbPreviewHorizontalLabels = Resources.getString("sCbPreviewHorizontalLabels");
		sLblMinMbrAspectRatio = Resources.getString("sLblMinMbrAspectRatio");
	}

	private void applyPreferenceStrings() {
		tpPreferences.setTitleAt(0, sTpSpatialView);
		tpPreferences.setTitleAt(1, sTpVisualisation);
		tpPreferences.setTitleAt(2, sTpMiscellaneous);
		tpPreferences.setTitleAt(3, sTpImportExport);

		btnSRID.setText(sBtnSRID);
		cbColourDimInfo.setText(sCbColourDimInfo);
		cbColourDimInfo.setToolTipText(sCbColourDimInfoTT);
		cbColourSdoGeometry.setText(sCbColourSdoGeometry);
		cbColourSdoGeometry.setToolTipText(sCbColourSdoGeometryTT);
		cbMinResolution.setText(sCbMinResolution);
		cbSchemaPrefix.setText(sCbSchemaPrefix);
		cbSchemaPrefix.setToolTipText(sCbSchemaPrefixTT);
		cbSdoGeomCoordNumbering.setText(sCbSdoGeomCoordNumbering);
		cbSdoGeomCoordNumbering.setToolTipText(sCbSdoGeomCoordNumberingTT);
		cbSdoGeometryFormat.setText(sCbSdoGeometryFormat);
		cbSdoGeometryFormat.setToolTipText(sCbSdoGeometryFormatTT);
		cmbSdoGeometryBracket.setToolTipText(sCmbSdoGeometryBracketTT);
		cmbSdoGeometryVisualFormat.setToolTipText(sCmbSdoGeometryVisualFormatTT);
		cbRandomRendering.setText(sCbRandomRendering);
		cmbTOCPosition.setToolTipText(sCmbTOCPositionTT);
		cbLogSearchStats.setText(sCbLogSearchStats);
		lblAffix.setText(sLblAffix);
		lblDefFetchSize.setText(sLblDefFetchSize);
		lblMBRSaveSize.setText(sLblMBRSaveSize);
		btnFeatureColour.setToolTipText(sLblFeatureColourTT);
		btnMapBackground.setToolTipText(sLblMapBackground);
		lblNewLayerPosition.setText(sLblNewLayerPosition);
		lblPanZoomChange.setText(String.format(sLblPanZoomChange, Constants.VAL_PAN_ZOOM_PERCENTAGE));
		lblRefreshImage.setText(sLblRefreshImage);
		lblSdoGeometryBracketing.setText(sLblSdoGeometryBracketing);
		lblSdoGeometryVisualFormat.setText(sLblSdoGeometryVisualFormat);
		lblSearchPixels.setText(sLblSearchPixels);
		lblTOCPosition.setText(sLblTOCPosition);
		pnlMbrSource.setBorder(BorderFactory.createTitledBorder(sPnlMbrSource));
		this.lblDefaultSRID.setText(sDefSRID);
		rbLayerMBRIndex.setText(sRbLayerMBRIndex);
		rbLayerMBRMetadata.setText(sRbLayerMBRMetadata);
		rbPrefix.setText(sRbPrefix);
		rbSuffix.setText(sRbSuffix);
		lblTableCountLimit.setText(sLblTableCountLimit);
		lblTableCountLimit.setToolTipText(sLblTableCountLimitTT);
		pnlNullValueSubstitution.setBorder(BorderFactory.createTitledBorder(sPnlNullValueSubstitution));
		lblNullString.setText(sLblNullString);
		lblNullNumber.setText(sLblNullNumber);
		lblNullInteger.setText(sLblNullInteger);
		lblNullDate.setText(sLblNullDate);
		tfNullString.setToolTipText(sTfNullStringTT);
		tfNullString.setToolTipText(sTfNullStringTT);
		tfNullNumber.setToolTipText(sTfNullNumberTT);
		tfNullInteger.setToolTipText(sTfNullIntegerTT);
		tfNullDate.setToolTipText(sTfNullDateTT);
		cbDbaseNullWriteString.setText(sCbDbaseNullWriteString);
		cbPrefixWithMDSYS.setText(sCbPrefixWithMDSYS);
		cbFastPicklerConversion.setText(sCbFastPicklerConversion);
		cbMapScale.setText(sCbMapScale);
		cbNumberOfFeatures.setText(sCbNumberOfFeatures);
		lblDBFColumnTruncation.setText(sLblDBFColumnTruncation);
		rbBegin10.setText(sRbBegin10);
		rbBegin10.setToolTipText(sRbBegin10TT);
		rbLast10.setText(sRbLast10);
		rbLast10.setToolTipText(sRbLast10TT);
		lblQueryLimit.setText(sLblQueryLimit);
		tfQueryLimit.setToolTipText(sTfQueryLimitTT);
		lblImageWidth.setText(sLblPreviewImageWidth);
		cbPreviewHorizontalLabels.setText(sCbPreviewHorizontalLabels);
		cbPreviewVertexNumbering.setText(sCbPreviewVertexNumbering);
		lblImageWidth.setText(sLblPreviewImageWidth);
		lblWidthHeightPixels.setText(sLblPreviewImageHeight);
		lblMinMbrAspectRatio.setText(sLblMinMbrAspectRatio);
	}

	private void setVerifiers() {
		verifySRID = new InputVerifier() {
			public boolean verify(JComponent comp) {
				boolean returnValue = true;
				JTextField textField = (JTextField) comp;
				try {
					if (Strings.isEmpty(textField.getText().trim())) {
						textField.setText(Constants.NULL);
					} else if (textField.getText().equalsIgnoreCase(Constants.NULL)) {
						// It is OK
						returnValue = true;
					} else {
						// This will throw an exception if the value is not an integer
						Integer.parseInt(textField.getText());
					}
				} catch (NumberFormatException e) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(null, "SRID must be NULL or integer", MainSettings.EXTENSION_NAME,
							JOptionPane.ERROR_MESSAGE);
					returnValue = false;
				}
				return returnValue;
			}
		};
		tfSRID.setInputVerifier(verifySRID);
		verifyTableCountLimit = new InputVerifier() {
			public boolean verify(JComponent comp) {
				boolean returnValue = true;
				JTextField textField = (JTextField) comp;
				try {
					if (!textField.getText().matches("^[0-9][0-9]*$")) {
						throw new NumberFormatException(Resources.getString("INTEGER_ONLY_DIGITS"));
					}
					// This will throw an exception if the value is not an integer
					int size = Integer.parseInt(textField.getText());
					if (size < 1) {
						throw new NumberFormatException(Resources.getString("ERROR_COUNT_SIZE"));
					}
				} catch (NumberFormatException e) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(null, e.getMessage(), MainSettings.EXTENSION_NAME,
							JOptionPane.ERROR_MESSAGE);
					returnValue = false;
				}
				return returnValue;
			}
		};
		tfTableCountLimit.setInputVerifier(verifyTableCountLimit);
		verifyQueryLimit = new InputVerifier() {
			public boolean verify(JComponent comp) {
				boolean returnValue = true;
				JTextField textField = (JTextField) comp;
				try {
					if (!textField.getText().matches("^[0-9][0-9]*$")) {
						throw new NumberFormatException(Resources.getString("QUERY_DIGITS"));
					}
					// This will throw an exception if the value is not an integer
					int size = Integer.parseInt(textField.getText());
				} catch (NumberFormatException e) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(null, e.getMessage(), MainSettings.EXTENSION_NAME,
							JOptionPane.ERROR_MESSAGE);
					returnValue = false;
				}
				return returnValue;
			}
		};
		tfQueryLimit.setInputVerifier(verifyQueryLimit);
		verifyImageDimension = new InputVerifier() {
			public boolean verify(JComponent comp) {
				boolean returnValue = true;
				JTextField textField = (JTextField) comp;
				try {
					// This will throw an exception if the value is not an integer
					int size = Integer.parseInt(textField.getText());
					if (size < 100 || size > 1200) {
						throw new NumberFormatException(Resources.getString("DIMENSION_SIZE"));
					}
				} catch (NumberFormatException e) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(null, e.getMessage(), MainSettings.EXTENSION_NAME,
							JOptionPane.ERROR_MESSAGE);
					returnValue = false;
				}
				return returnValue;
			}
		};
		tfImageWidth.setInputVerifier(verifyImageDimension);
		tfImageHeight.setInputVerifier(verifyImageDimension);
		verifyMbrAspectRatioLimit = new InputVerifier() {
			public boolean verify(JComponent comp) {
				boolean returnValue = true;
				JTextField textField = (JTextField) comp;
				try {
					// This will throw an exception if the value is not an integer
					double size = Double.parseDouble(textField.getText());
					if (size < 2.0 || size > 10.0) {
						throw new NumberFormatException(Resources.getString("MIN_MBR_ASPECT_RATIO"));
					}
				} catch (NumberFormatException e) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(null, e.getMessage(), MainSettings.EXTENSION_NAME,
							JOptionPane.ERROR_MESSAGE);
					returnValue = false;
				}
				return returnValue;
			}
		};
		this.tfMinMbrAspectRatio.setInputVerifier(verifyMbrAspectRatioLimit);
	}

	private void jbInit() throws Exception {
		this.setLayout(verticalFlowLayout1);
		this.setSize(new Dimension(550, 375));
		// this.setMaximumSize(new Dimension(550, 400));
		// this.setPreferredSize(new Dimension(550, 400));
		// this.setMinimumSize(new Dimension(550, 400));

		// tpPreferences.setSize(new Dimension(540, 365));
		// tpPreferences.setPreferredSize(new Dimension(540, 390));
		// tpPreferences.setMinimumSize(new Dimension(540, 365));
		// tpPreferences.setMaximumSize(new Dimension(540, 390));

		/**
		 * *****************************************************************
		 ** ****************** Spatial View Panel (1) ***********************
		 *****************************************************************/

		pnlSpatialView.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		pnlSpatialView.setFont(new Font("Dialog", 0, 11));
		// pnlSpatialView.setSize(new Dimension(530, 380));
		// pnlSpatialView.setPreferredSize(new Dimension(530, 380));
		// pnlSpatialView.setMinimumSize(new Dimension(530, 380));
		// pnlSpatialView.setMaximumSize(new Dimension(530, 380));
		// pnlSpatialView.setBounds(new Rectangle(2, 20, 530, 380));

		pnlSpatialView.setLayout(xYLayoutSpatial);
		pnlSpatialView.add(cbDrawQueryGeometry, new XYConstraints(268, 128, 220, 20));
		pnlSpatialView.add(cbNumberOfFeatures, new XYConstraints(268, 103, 220, 20));
		pnlSpatialView.add(cbMapScale, new XYConstraints(263, 78, 225, 20));
		pnlSpatialView.add(lblDefaultSRID, new XYConstraints(148, 8, 80, 15));
		pnlSpatialView.add(cbSdoNN, new XYConstraints(113, 78, 135, 20));
		pnlSpatialView.add(lblSearchPixels, new XYConstraints(78, 258, 145, 15));
		pnlSpatialView.add(sldrSearchPixels, new XYConstraints(238, 248, 250, 35));
		pnlSpatialView.add(lblMBRSaveSize, new XYConstraints(63, 208, 160, 15));
		pnlSpatialView.add(sldrMBRSaveSize, new XYConstraints(238, 198, 250, 40));
		pnlSpatialView.add(cbMinResolution, new XYConstraints(23, 123, 225, 20));
		pnlSpatialView.add(lblPanZoomChange, new XYConstraints(3, 158, 220, 15));
		pnlSpatialView.add(sldrPanZoomChange, new XYConstraints(238, 148, 250, 40));
		pnlSpatialView.add(lblNewLayerPosition, new XYConstraints(28, 33, 200, 15));
		pnlSpatialView.add(cmbNewLayerPosition, new XYConstraints(233, 28, 80, 20));
		pnlSpatialView.add(lblTOCPosition, new XYConstraints(63, 58, 165, 15));
		pnlSpatialView.add(cmbTOCPosition, new XYConstraints(233, 53, 80, 20));
		pnlSpatialView.add(cbSchemaPrefix, new XYConstraints(38, 103, 210, 15));
		pnlSpatialView.add(btnSRID, new XYConstraints(333, 3, 90, 20));
		pnlSpatialView.add(tfSRID, new XYConstraints(233, 3, 90, 20));

		lblNewLayerPosition.setText("Layer's position when added to layer list:");
		lblNewLayerPosition.setLabelFor(cmbNewLayerPosition);
		// cmbNewLayerPosition.setMinimumSize(new Dimension(10, 19));
		// cmbNewLayerPosition.setSize(new Dimension(60, 20));
		// cmbNewLayerPosition.setPreferredSize(new Dimension(80, 20));
		cmbNewLayerPosition.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				cmbNewLayerPositionActionPerformed(evt);
			}

			private void cmbNewLayerPositionActionPerformed(ActionEvent evt) {
				setNewLayerPosition(cmbNewLayerPosition.getSelectedItem().toString());
			}
		});

		// lblNewLayerPosition.setPreferredSize(new Dimension(205, 15));
		// lblNewLayerPosition.setMinimumSize(new Dimension(205, 15));
		// lblNewLayerPosition.setMaximumSize(new Dimension(205, 15));

		lblNewLayerPosition.setOpaque(true);
		lblTOCPosition.setText("View/Layer Tree Panel position(*):");
		lblTOCPosition.setLabelFor(cmbTOCPosition);
		// lblTOCPosition.setPreferredSize(new Dimension(165, 15));
		// lblTOCPosition.setMinimumSize(new Dimension(165, 15));
		// lblTOCPosition.setMaximumSize(new Dimension(165, 15));
		lblTOCPosition.setOpaque(true);
		cmbTOCPosition.setToolTipText("Layer's position when added to layer list:");
		// cmbTOCPosition.setMinimumSize(new Dimension(80, 20));
		cmbTOCPosition.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				cmbTOCPositionActionPerformed(evt);
			}

			private void cmbTOCPositionActionPerformed(ActionEvent evt) {
				setTOCPosition(cmbTOCPosition.getSelectedItem().toString());
			}
		});

		rbLayerMBRIndex.setText("RTree Index (projected only)");
		// rbLayerMBRIndex.setPreferredSize(new Dimension(160, 15));
		// rbLayerMBRIndex.setMinimumSize(new Dimension(160, 15));
		// rbLayerMBRIndex.setMaximumSize(new Dimension(160, 15));
		rbLayerMBRIndex.setHorizontalTextPosition(SwingConstants.LEADING);
		rbLayerMBRMetadata.setText("Metadata");
		// rbLayerMBRMetadata.setPreferredSize(new Dimension(80, 15));
		// rbLayerMBRMetadata.setMinimumSize(new Dimension(80, 15));
		// rbLayerMBRMetadata.setMaximumSize(new Dimension(80, 15));
		rbLayerMBRMetadata.setHorizontalAlignment(SwingConstants.LEFT);
		rbLayerMBRMetadata.setHorizontalTextPosition(SwingConstants.LEADING);
		spatialndexPanel.setLayout(xYLayout1);
		spatialndexPanel.setToolTipText("Spatial Index Naming Preferences");
		spatialndexPanel.setBorder(BorderFactory.createTitledBorder("Spatial Index Naming"));
		tfRefreshImage.setText("1500");
		lblRefreshImage.setText("Refresh Image time (ms):");
		lblRefreshImage.setLabelFor(tfRefreshImage);
		// lblRefreshImage.setPreferredSize(new Dimension(130, 15));
		// lblRefreshImage.setMinimumSize(new Dimension(130, 15));
		// lblRefreshImage.setMaximumSize(new Dimension(130, 15));
		lblRefreshImage.setOpaque(true);
		lblRefreshImage.setHorizontalAlignment(SwingConstants.RIGHT);
		tfDefFetchSize.setText("100");
		lblDefFetchSize.setText("SELECT Fetch Size (rows):");
		lblDefFetchSize.setLabelFor(tfDefFetchSize);
		// lblDefFetchSize.setPreferredSize(new Dimension(135, 15));
		// lblDefFetchSize.setMinimumSize(new Dimension(135, 15));
		// lblDefFetchSize.setMaximumSize(new Dimension(135, 15));
		lblDefFetchSize.setOpaque(true);
		lblDefFetchSize.setHorizontalAlignment(SwingConstants.RIGHT);
		lblDefaultSRID.setText("Default SRID:");
		lblDefaultSRID.setHorizontalAlignment(SwingConstants.RIGHT);
		bgLayerMBR.add(rbLayerMBRIndex);
		bgLayerMBR.add(rbLayerMBRMetadata);
		bgLayerMBR.setSelected(rbLayerMBRIndex.getModel(), true);
		bgLayerMBR.setSelected(rbLayerMBRMetadata.getModel(), false);

		pnlMbrSource.setLayout(xYLayoutMBR);
		pnlMbrSource.setBorder(BorderFactory.createTitledBorder("Layer MBR Source"));
		// pnlMbrSource.setPreferredSize(new Dimension(185, 85));
		// pnlMbrSource.setMinimumSize(new Dimension(185, 85));
		// pnlMbrSource.setMaximumSize(new Dimension(185, 85));
		// pnlMbrSource.setSize(new Dimension(130, 60));

		pnlMbrSource.add(rbLayerMBRIndex, new XYConstraints(50, 21, 160, 15));
		pnlMbrSource.add(rbLayerMBRMetadata, new XYConstraints(145, -4, 65, 15));
		lblSearchPixels.setText(sLblSearchPixels);
		sldrSearchPixels.setMaximum(20);
		sldrSearchPixels.setMinimum(1);
		sldrSearchPixels.setMajorTickSpacing(5);
		sldrSearchPixels.setMinorTickSpacing(1);
		sldrSearchPixels.setPaintTicks(true);
		sldrSearchPixels.setSnapToTicks(true);
		sldrSearchPixels.setValue(3);
		sldrSearchPixels.setPaintLabels(true);
		lblSearchPixels.setLabelFor(sldrSearchPixels);
		lblSearchPixels.setText("Query distance in pixels: 10");
		lblSearchPixels.setOpaque(true);
		lblSearchPixels.setHorizontalAlignment(SwingConstants.RIGHT);
		sldrSearchPixels.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				sldrSearchPixelsStateChanged(evt);
			}

			public void sldrSearchPixelsStateChanged(ChangeEvent evt) {
				lblSearchPixels.setText(String.format(sLblSearchPixels + " %3d", sldrSearchPixels.getValue()));

			}
		});

		sldrMBRSaveSize.setMaximum(200);
		sldrMBRSaveSize.setMinimum(20);
		sldrMBRSaveSize.setMajorTickSpacing(20);
		sldrMBRSaveSize.setMinorTickSpacing(5);
		sldrMBRSaveSize.setPaintTicks(true);
		sldrMBRSaveSize.setSnapToTicks(false);
		sldrMBRSaveSize.setValue(20);
		sldrMBRSaveSize.setPaintLabels(true);
		lblMBRSaveSize.setLabelFor(sldrMBRSaveSize);
		lblMBRSaveSize.setText("Maximum window saves (*): 20");
		// lblMBRSaveSize.setPreferredSize(new Dimension(160, 15));
		// lblMBRSaveSize.setMinimumSize(new Dimension(160, 15));
		// lblMBRSaveSize.setMaximumSize(new Dimension(160, 15));
		lblMBRSaveSize.setOpaque(true);
		lblMBRSaveSize.setHorizontalAlignment(SwingConstants.RIGHT);
		sldrMBRSaveSize.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				sldrMBRSaveSizeStateChanged(evt);
			}

			public void sldrMBRSaveSizeStateChanged(ChangeEvent evt) {
				lblMBRSaveSize.setText(String.format(sLblMBRSaveSize + " %3d", sldrMBRSaveSize.getValue()));

			}
		});

		/*----------------------- SRID Panel ------------------------------------*/

		btnSRID.setText("Select SRID");
		btnSRID.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnSRID_actionPerformed(e);
			}
		});
		lblTableCountLimit.setText("Table Count Limit:");
		tfTableCountLimit.setText("1000");
		cbLogSearchStats.setText("Log SQL Statements");
		cbLogSearchStats.setHorizontalTextPosition(SwingConstants.LEADING);
		cbLogSearchStats.setHorizontalAlignment(SwingConstants.RIGHT);
		cbSdoNN.setText("Identify using SDO_NN:");
		cbSdoNN.setHorizontalTextPosition(SwingConstants.LEADING);
		cbSdoNN.setToolTipText("When identifying features use SDO_NN or SDO_NN_DISTANCE");

		/*-----------------------------------------------------------*/

		cbSdoNN.setHorizontalAlignment(SwingConstants.RIGHT);
		cbMinResolution.setText("SDO_FILTER pixel filtering (not points)");

		// Schema Prefix for initial Layer Visible Name?
		cbMinResolution.setHorizontalTextPosition(SwingConstants.LEADING);
		// cbMinResolution.setPreferredSize(new Dimension(210, 20));
		// cbMinResolution.setMinimumSize(new Dimension(210, 20));
		// cbMinResolution.setMaximumSize(new Dimension(210, 20));
		// cbMinResolution.setSize(new Dimension(202, 15));
		cbMinResolution.setHorizontalAlignment(SwingConstants.RIGHT);
		cbSchemaPrefix.setText("Prefix Initial layer name with schema?");
		cbSchemaPrefix.setToolTipText(sCbSchemaPrefixTT);
		cbSchemaPrefix.setSelected(true);

		cbSchemaPrefix.setHorizontalTextPosition(SwingConstants.LEADING);
		cbSchemaPrefix.setHorizontalAlignment(SwingConstants.RIGHT);
		// cbSchemaPrefix.setMinimumSize(new Dimension(210, 15));
		// cbSchemaPrefix.setMaximumSize(new Dimension(210, 15));
		// cbSchemaPrefix.setPreferredSize(new Dimension(210, 15));

		sldrPanZoomChange.setMaximum(50);
		sldrPanZoomChange.setMinimum(5);
		sldrPanZoomChange.setMajorTickSpacing(5);
		sldrPanZoomChange.setMinorTickSpacing(1);
		sldrPanZoomChange.setPaintTicks(true);
		sldrPanZoomChange.setSnapToTicks(true);
		sldrPanZoomChange.setValue(25);
		sldrPanZoomChange.setPaintLabels(true);
		sldrPanZoomChange.setModel(new DefaultBoundedRangeModel(25, 0, 5, 50));
		sldrPanZoomChange.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				sldrPanZoomChangeStateChanged(evt);
			}

			public void sldrPanZoomChangeStateChanged(ChangeEvent evt) {
				lblPanZoomChange.setText(String.format(sLblPanZoomChange, sldrPanZoomChange.getValue()));

			}
		});

		lblPanZoomChange.setLabelFor(sldrPanZoomChange);
		lblPanZoomChange.setText("Pan/Zoom change: 100% of current window");
		// lblPanZoomChange.setSize(new Dimension(201, 15));
		// lblPanZoomChange.setPreferredSize(new Dimension(201, 15));
		// lblPanZoomChange.setMinimumSize(new Dimension(201, 15));
		// lblPanZoomChange.setMaximumSize(new Dimension(201, 15));
		lblPanZoomChange.setOpaque(true);
		lblPanZoomChange.setHorizontalTextPosition(SwingConstants.RIGHT);
		lblPanZoomChange.setHorizontalAlignment(SwingConstants.RIGHT);

		/**
		 * *****************************************************************
		 ** ****************** Visualisation Panel (2) **********************
		 *****************************************************************/

		// pnlVisualisation
		//
		pnlVisualisation.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		pnlVisualisation.setLayout(xYLayoutVisualisation);
		// pnlVisualisation.setSize(new Dimension(530, 380));
		// pnlVisualisation.setPreferredSize(new Dimension(530, 380));
		// pnlVisualisation.setMinimumSize(new Dimension(530, 380));
		// pnlVisualisation.setMaximumSize(new Dimension(530, 387));

		// pnlVisualisation - pnlSdoObjectTextDisplay
		//
		pnlSdoObjectTextDisplay.setLayout(xYObjectTextLayout);
		pnlSdoObjectTextDisplay.setBorder(BorderFactory.createTitledBorder("SDO_GEOMETRY / DIMINFO Text Display"));
		cbColourSdoGeometry.setText("Colour SDO_GEOMETRY elements?");
		cbColourSdoGeometry.setHorizontalTextPosition(SwingConstants.LEADING);
		// cbColourSdoGeometry.setPreferredSize(new Dimension(190, 15));
		// cbColourSdoGeometry.setMinimumSize(new Dimension(190, 15));
		// cbColourSdoGeometry.setMaximumSize(new Dimension(190, 15));
		cbColourSdoGeometry.setHorizontalAlignment(SwingConstants.TRAILING);
		pnlSdoObjectTextDisplay.add(cbRandomRendering, new XYConstraints(290, 94, 215, 20));
		pnlSdoObjectTextDisplay.add(cbPrefixWithMDSYS, new XYConstraints(290, 79, 215, 15));
		pnlSdoObjectTextDisplay.add(cbColourSdoGeometry, new XYConstraints(50, 79, 210, 15));
		pnlSdoObjectTextDisplay.add(cbColourDimInfo, new XYConstraints(100, 97, 160, 15));
		pnlSdoObjectTextDisplay.add(lblSdoGeometryBracketing, new XYConstraints(155, -1, 235, 15));
		pnlSdoObjectTextDisplay.add(cmbSdoGeometryBracket, new XYConstraints(400, -6, 105, 25));
		pnlSdoObjectTextDisplay.add(cbSdoGeomCoordNumbering, new XYConstraints(10, 59, 250, 15));
		pnlSdoObjectTextDisplay.add(lblSdoGeometryVisualFormat, new XYConstraints(205, 24, 185, 20));
		pnlSdoObjectTextDisplay.add(cmbSdoGeometryVisualFormat, new XYConstraints(400, 24, 105, 25));
		pnlSdoObjectTextDisplay.add(cbGroupingSeparator, new XYConstraints(285, 56, 220, 20));
		cbColourDimInfo.setText("Colour DimInfo elements:");
		cbColourDimInfo.setHorizontalTextPosition(SwingConstants.LEADING);
		// cbColourDimInfo.setPreferredSize(new Dimension(150, 15));
		// cbColourDimInfo.setMinimumSize(new Dimension(150, 15));
		// cbColourDimInfo.setMaximumSize(new Dimension(150, 15));
		cbColourDimInfo.setHorizontalAlignment(SwingConstants.TRAILING);
		lblSdoGeometryBracketing.setText("SDO_ORDINATE_ARRAY coordinate bracket:");
		lblSdoGeometryBracketing.setLabelFor(cmbSdoGeometryBracket);
		lblSdoGeometryBracketing.setHorizontalTextPosition(SwingConstants.LEADING);
		lblSdoGeometryBracketing.setHorizontalAlignment(SwingConstants.RIGHT);
		lblSdoGeometryBracketing.setOpaque(true);
		// lblSdoGeometryBracketing.setPreferredSize(new Dimension(220, 15));
		// lblSdoGeometryBracketing.setMinimumSize(new Dimension(220, 15));
		// lblSdoGeometryBracketing.setMaximumSize(new Dimension(220, 15));
		// cmbSdoGeometryBracket = new JComboBox(Constants.getBracketTypeCombo());
		// cmbSdoGeometryBracket.setPreferredSize(new Dimension(100, 25));
		// cmbSdoGeometryBracket.setMinimumSize(new Dimension(100, 25));
		// cmbSdoGeometryBracket.setMaximumSize(new Dimension(100, 25));
		cmbSdoGeometryBracket.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cmbSdoGeometryBracketType_actionPerformed(e);
			}
		});
		cmbSdoGeometryVisualFormat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cmbSdoGeometryVisualFormat_actionPerformed(e);
			}
		});
		cbSdoGeomCoordNumbering.setText("Number SDO_ORDINATE_ARRAY coordinates?");
		cbSdoGeomCoordNumbering.setHorizontalTextPosition(SwingConstants.LEADING);
		// cbSdoGeomCoordNumbering.setPreferredSize(new Dimension(250, 15));
		// cbSdoGeomCoordNumbering.setMinimumSize(new Dimension(250, 15));
		// cbSdoGeomCoordNumbering.setMaximumSize(new Dimension(250, 15));
		cbSdoGeomCoordNumbering.setHorizontalAlignment(SwingConstants.TRAILING);
		lblSdoGeometryVisualFormat.setText("SDO_GEOMETRY display format:");
		lblSdoGeometryVisualFormat.setLabelFor(cmbSdoGeometryVisualFormat);
		lblSdoGeometryVisualFormat.setOpaque(true);
		lblSdoGeometryVisualFormat.setHorizontalAlignment(SwingConstants.TRAILING);
		// lblSdoGeometryVisualFormat.setPreferredSize(new Dimension(185, 20));
		// lblSdoGeometryVisualFormat.setMaximumSize(new Dimension(185, 20));
		// lblSdoGeometryVisualFormat.setMinimumSize(new Dimension(185, 20));
		// cmbSdoGeometryVisualFormat.setPreferredSize(new Dimension(100, 25));
		// cmbSdoGeometryVisualFormat.setMinimumSize(new Dimension(100, 25));
		// cmbSdoGeometryVisualFormat.setMaximumSize(new Dimension(100, 25));
		// cmbSdoGeometryVisualFormat = new JComboBox(Constants.getRenderTypeCombo());

		pnlVisualisation.add(pnlSdoObjectTextDisplay, new XYConstraints(8, 3, 520, 140));
		pnlVisualisation.add(pnlMapColours, new XYConstraints(8, 143, 205, 100));
		pnlVisualisation.add(pnlMetadataColours, new XYConstraints(8, 243, 205, 85));

		pnlPreviewImage.setLayout(xyPreviewImage);
		pnlPreviewImage.setBorder(BorderFactory.createTitledBorder("Preview Image"));
		pnlPreviewImage.add(lblMarkerSize, new XYConstraints(0, 124, 145, 20));
		pnlPreviewImage.add(sldrVertexMarkSize, new XYConstraints(150, 124, 145, 35));
		pnlPreviewImage.add(lblComma, new XYConstraints(205, -1, 10, 20));
		pnlPreviewImage.add(lblWidthHeightPixels, new XYConstraints(245, -1, 45, 20));
		pnlPreviewImage.add(lblImageWidth, new XYConstraints(5, -1, 155, 20));
		pnlPreviewImage.add(tfImageWidth, new XYConstraints(165, -1, 35, 20));
		pnlPreviewImage.add(tfImageHeight, new XYConstraints(210, -1, 35, 20));
		pnlPreviewImage.add(cbPreviewVertexNumbering, new XYConstraints(160, 19, 130, 18));
		pnlPreviewImage.add(cbPreviewHorizontalLabels, new XYConstraints(170, 44, 120, 18));
		pnlPreviewImage.add(tfMinMbrAspectRatio, new XYConstraints(260, 69, 30, 20));
		pnlPreviewImage.add(lblMinMbrAspectRatio, new XYConstraints(20, 69, 235, 20));
		pnlPreviewImage.add(cmbPreviewVertexMark, new XYConstraints(215, 94, 75, 20));

		pnlPreviewImage.add(lblVertexMarker, new XYConstraints(115, 94, 90, 20));
		pnlVisualisation.add(pnlPreviewImage, new XYConstraints(218, 143, 310, 185));

		// pnlVisualisation - PnlMapColours
		//
		// btnMapBackground.setSize(new Dimension(65, 25));
		// btnMapBackground.setPreferredSize(new Dimension(160, 20));
		// btnMapBackground.setMinimumSize(new Dimension(160, 20));
		// btnMapBackground.setMaximumSize(new Dimension(160, 20));
		pnlMapColours.setBorder(BorderFactory.createTitledBorder("Map Colours"));
		pnlMapColours.setLayout(xYMapColoursLayout);
		btnMapBackground.setBackground(mapBackground);
		btnMapBackground.setText("Map Background (*)");
		btnMapBackground.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mapBackgroundButtonActionPerformed(evt);
			}
		});
		// btnSelectionColour.setSize(new Dimension(65, 25));
		// btnSelectionColour.setPreferredSize(new Dimension(160, 20));
		// btnSelectionColour.setMinimumSize(new Dimension(160, 20));
		// btnSelectionColour.setMaximumSize(new Dimension(160, 20));
		btnSelectionColour.setBackground(selectionColour);
		btnSelectionColour.setText("Selection Colour");
		btnSelectionColour.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnSelectionColour_actionPerformed(e);
			}
		});
		pnlMapColours.add(btnMapBackground, new XYConstraints(5, 46, 160, 20));
		pnlMapColours.add(btnSelectionColour, new XYConstraints(5, 21, 111, 21));
		// btnFeatureColour.setSize(new Dimension(65, 25));
		// btnFeatureColour.setPreferredSize(new Dimension(160, 20));
		// btnFeatureColour.setMinimumSize(new Dimension(160, 20));
		// btnFeatureColour.setMaximumSize(new Dimension(160, 20));
		pnlMapColours.add(btnFeatureColour, new XYConstraints(5, -4, 160, 20));
		btnFeatureColour.setBackground(featureColour);
		btnFeatureColour.setText("Measure/Create Feature");
		btnFeatureColour.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				btnFeatureColorActionPerformed(evt);
			}
		});

		// pnlVisualisation - pnlMetadataColours
		//
		// btnCorrectColour.setPreferredSize(new Dimension(80, 20));
		// btnCorrectColour.setMaximumSize(new Dimension(80, 20));
		// btnCorrectColour.setMinimumSize(new Dimension(80, 20));
		// btnCorrectColour.setSize(new Dimension(80, 20));
		pnlMetadataColours.setLayout(xYMetadataColoursLayout);
		pnlMetadataColours.setBorder(BorderFactory.createTitledBorder("Metadata Colours"));

		btnCorrectColour.setText("Correct");
		btnCorrectColour.setBackground(correctColour);
		btnCorrectColour.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnCorrectMetadata_actionPerformed(e);
			}
		});

		// btnOrphanColour.setPreferredSize(new Dimension(80, 20));
		// btnOrphanColour.setMaximumSize(new Dimension(80, 20));
		// btnOrphanColour.setMinimumSize(new Dimension(80, 20));
		btnOrphanColour.setText("Orphan");
		btnOrphanColour.setBackground(orphanColour);
		btnOrphanColour.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnOrphanColour_actionPerformed(e);
			}
		});
		pnlMetadataColours.add(btnCorrectColour, new XYConstraints(10, 1, 75, 20));
		pnlMetadataColours.add(btnOrphanColour, new XYConstraints(95, 1, 80, 20));
		pnlMetadataColours.add(btnMissingColour, new XYConstraints(10, 31, 75, 20));

		// btnMissingColour.setPreferredSize(new Dimension(80, 20));
		// btnMissingColour.setMaximumSize(new Dimension(80, 20));
		// btnMissingColour.setMinimumSize(new Dimension(80, 20));
		// btnMissingColour.setSize(new Dimension(80, 20));
		pnlMetadataColours.add(btnHighlightColour, new XYConstraints(95, 31, 80, 20));
		btnMissingColour.setText("Missing");
		btnMissingColour.setBackground(missingColour);
		btnMissingColour.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnMissingMetadata_actionPerformed(e);
			}
		});

		// btnHighlightColour.setPreferredSize(new Dimension(80, 20));
		// btnHighlightColour.setMinimumSize(new Dimension(80, 20));
		// btnHighlightColour.setMaximumSize(new Dimension(80, 20));
		btnHighlightColour.setText("Highlight");
		btnHighlightColour.setBackground(highlightColour);
		btnHighlightColour.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnHighlightColour_actionPerformed(e);
			}
		});

		// cbPrefixWithMDSYS.setMaximumSize(new Dimension(163, 15));
		// cbPrefixWithMDSYS.setMinimumSize(new Dimension(163, 15));
		// cbPrefixWithMDSYS.setPreferredSize(new Dimension(163, 15));
		// cbPrefixWithMDSYS.setSize(new Dimension(215, 15));
		cbPrefixWithMDSYS.setText("Prefix SDO Types With MDSYS");
		cbPrefixWithMDSYS.setHorizontalTextPosition(SwingConstants.LEADING);
		cbPrefixWithMDSYS.setSelected(true);
		cbPrefixWithMDSYS.setHorizontalAlignment(SwingConstants.RIGHT);

		// lblQueryLimit.setPreferredSize(new Dimension(169, 15));
		// lblQueryLimit.setMaximumSize(new Dimension(169, 15));
		// lblQueryLimit.setMinimumSize(new Dimension(169, 15));
		lblQueryLimit.setText("Search/Display/Query Limit (feats):");
		lblQueryLimit.setHorizontalAlignment(SwingConstants.TRAILING);
		lblQueryLimit.setLabelFor(tfQueryLimit);
		lblQueryLimit.setOpaque(true);

		cbSQLSchemaPrefix.setText("Prefix SQL database objects with schema:");
		cbSQLSchemaPrefix.setHorizontalTextPosition(SwingConstants.LEADING);
		cbSQLSchemaPrefix.setToolTipText("eg SELECT ... FROM schema.table");
		cbSQLSchemaPrefix.setHorizontalAlignment(SwingConstants.RIGHT);
		tfQueryLimit.setText("0");
		tfQueryLimit.setToolTipText("Value must be an integer value >= 0 (0 means no limit is applied).");
		cbDebug.setText("Debug mode:");
		cbDebug.setHorizontalTextPosition(SwingConstants.LEADING);
		cbDebug.setHorizontalAlignment(SwingConstants.RIGHT);
		cbDebug.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cbDebug_actionPerformed(e);
			}
		});

		cbDrawQueryGeometry.setText("Draw Query Geometry?");
		cbDrawQueryGeometry.setHorizontalTextPosition(SwingConstants.LEADING);
		cbDrawQueryGeometry.setHorizontalAlignment(SwingConstants.RIGHT);
		cbNumberOfFeatures.setText("Display Layer's Feature Count?");
		cbNumberOfFeatures.setHorizontalTextPosition(SwingConstants.LEADING);
		cbNumberOfFeatures.setHorizontalAlignment(SwingConstants.RIGHT);
		pnlColumnShorten.setLayout(xyLayoutColumnShorten);
		pnlColumnShorten.setBorder(BorderFactory.createTitledBorder("Database Table Column Name to DBF Name Mapping"));
		rbLast10.setText("Last 10 Chars");
		cbRandomRendering.setText("Randomly Style New Layers:");
		cbRandomRendering.setHorizontalTextPosition(SwingConstants.LEADING);
		cbRandomRendering.setHorizontalAlignment(SwingConstants.RIGHT);
		cbRandomRendering.setSelected(true);
		lblGeometryColumnName.setText("Default Name of Geometry Column:");
		lblGeometryColumnName.setHorizontalTextPosition(SwingConstants.LEADING);
		lblGeometryColumnName.setHorizontalAlignment(SwingConstants.RIGHT);
		lblGeometryColumnName.setLabelFor(tfGeometryColumnName);
		tfGeometryColumnName.setText("GEOM");
		tfGeometryColumnName.setToolTipText("Default name for geometry column eg GEOM or SHAPE");
		tfGeometryColumnName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tfGeometryColumnName_actionPerformed(e);
			}
		});

		lblMinMbrAspectRatio.setText("Minuimum MBR Aspect Ratio (def 3.0):");
		lblMinMbrAspectRatio.setHorizontalAlignment(SwingConstants.TRAILING);
		lblMinMbrAspectRatio.setHorizontalTextPosition(SwingConstants.RIGHT);
		lblMinMbrAspectRatio.setLabelFor(tfMinMbrAspectRatio);

		// tfMinMbrAspectRatio.setSize(new Dimension(20, 20));
		// tfMinMbrAspectRatio.setPreferredSize(new Dimension(30, 20));
//		cmbPreviewVertexMark.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				cmbPreviewVertexMark_actionPerformed(e);
//			}

//			private void cmbPreviewVertexMark_actionPerformed(ActionEvent evt) {
//				setPreviewVertexMark(cmbPreviewVertexMark.getSelectedItem().toString());
//			}
//		});
		lblVertexMarker.setText("Vertex Marker:");
		lblVertexMarker.setLabelFor(cmbPreviewVertexMark);
		lblVertexMarker.setHorizontalAlignment(SwingConstants.TRAILING);

		sldrVertexMarkSize.setMinimum(4);
		sldrVertexMarkSize.setMaximum(32);
		sldrVertexMarkSize.setMajorTickSpacing(4);
		sldrVertexMarkSize.setSnapToTicks(true);
		sldrVertexMarkSize.setPaintLabels(true);
		sldrVertexMarkSize.setPaintTicks(true);
		sldrVertexMarkSize.setValue(8);

		lblImageWidth.setText("Image (Width,Height) (");
		lblImageWidth.setLabelFor(tfImageWidth);
		lblImageWidth.setInheritsPopupMenu(false);
		lblImageWidth.setHorizontalTextPosition(SwingConstants.LEADING);
		lblImageWidth.setHorizontalAlignment(SwingConstants.RIGHT);

		lblWidthHeightPixels.setText(") pixels.");
		lblWidthHeightPixels.setHorizontalTextPosition(SwingConstants.RIGHT);
		lblWidthHeightPixels.setHorizontalAlignment(SwingConstants.TRAILING);
		lblComma.setText(",");
		lblComma.setHorizontalTextPosition(SwingConstants.CENTER);
		lblComma.setLabelFor(tfImageHeight);

		// lblImageWidth.setMaximumSize(new Dimension(200, 20));
		// lblImageHeight.setMaximumSize(new Dimension(200, 20));
		// lblImageHeight.setMinimumSize(new Dimension(110, 20));
		// lblImageHeight.setPreferredSize(new Dimension(110, 20));

		cbPreviewHorizontalLabels.setText("Horizontal Labels");
		cbPreviewHorizontalLabels.setHorizontalAlignment(SwingConstants.RIGHT);
		// cbPreviewHorizontalLabels.setMinimumSize(new Dimension(60, 18));
		// cbPreviewHorizontalLabels.setPreferredSize(new Dimension(60, 18));
		// cbPreviewHorizontalLabels.setSize(new Dimension(60, 18));

		cbPreviewHorizontalLabels.setHorizontalTextPosition(SwingConstants.LEFT);
		cbPreviewHorizontalLabels.setSize(new Dimension(100, 18));
		cbPreviewVertexNumbering.setText("Vertex Labeling");
		cbPreviewVertexNumbering.setHorizontalTextPosition(SwingConstants.LEFT);
		// cbPreviewVertexNumbering.setSize(new Dimension(50, 18));
		// cbPreviewVertexNumbering.setPreferredSize(new Dimension(50, 18));
		// cbPreviewVertexNumbering.setMinimumSize(new Dimension(50, 18));
		cbPreviewVertexNumbering.setHorizontalTextPosition(SwingConstants.LEFT);

		cbPreviewVertexNumbering.setHorizontalAlignment(SwingConstants.RIGHT);
		// tfImageHeight.setPreferredSize(new Dimension(20, 20));
		// tfImageWidth.setPreferredSize(new Dimension(20, 20));
		// tfImageWidth.setSize(new Dimension(30, 20));
		rbBegin10.setText("First 10 Chars");
		lblDBFColumnTruncation.setText("Extract DBF 10 char name from:");
		lblDBFColumnTruncation.setHorizontalAlignment(SwingConstants.TRAILING);
		cbMapScale.setText("Display 1:xxxx Map Scale");
		cbMapScale.setHorizontalTextPosition(SwingConstants.LEADING);
		cbMapScale.setHorizontalAlignment(SwingConstants.RIGHT);
		cbFastPicklerConversion.setText("Fast conversion of SDO_GEOMETRY:");
		cbFastPicklerConversion.setHorizontalTextPosition(SwingConstants.LEADING);
		cbFastPicklerConversion.setSelected(false);
		cbFastPicklerConversion.setHorizontalAlignment(SwingConstants.RIGHT);

		cbGroupingSeparator.setText("Display Number Grouping Separator");
		cbGroupingSeparator.setHorizontalAlignment(SwingConstants.RIGHT);
		cbGroupingSeparator.setHorizontalTextPosition(SwingConstants.LEFT);
		cbGroupingSeparator.setSelected(true);

		/**
		 * *****************************************************************
		 ** ******************* Miscellaneous Panel (3) *********************
		 *****************************************************************/

		pnlMiscellaneous.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		pnlMiscellaneous.setLayout(xYLayoutMiscellaneous);
		// pnlMiscellaneous.setSize(new Dimension(530, 380));
		// pnlMiscellaneous.setPreferredSize(new Dimension(530, 380));
		// pnlMiscellaneous.setMinimumSize(new Dimension(530, 380));
		// pnlMiscellaneous.setMaximumSize(new Dimension(530, 380));
		// pnlMiscellaneous.setBounds(new Rectangle(2, 20, 530, 380));

		// cbGroupingSeparator.setSize(new Dimension(205, 18));
		// cbGroupingSeparator.setPreferredSize(new Dimension(205, 18));
		// cbGroupingSeparator.setMinimumSize(new Dimension(205, 18));
		// cbGroupingSeparator.setMaximumSize(new Dimension(205, 18));

		// Index Affix
		//
		lblAffix.setText("Prefix/Suffix:");
		lblAffix.setLabelFor(tfAffixString);
		// lblAffix.setPreferredSize(new Dimension(100, 15));
		// lblAffix.setMinimumSize(new Dimension(100, 15));
		// lblAffix.setMaximumSize(new Dimension(100, 15));
		lblAffix.setOpaque(true);
		lblAffix.setHorizontalAlignment(SwingConstants.RIGHT);
		// tfAffixString.setMinimumSize(new Dimension(105, 20));
		// tfAffixString.setMaximumSize(new Dimension(105, 20));
		// tfAffixString.setPreferredSize(new Dimension(105, 20));
		rbPrefix.setText(sRbPrefix);
		// rbPrefix.setMaximumSize(new Dimension(55, 20));
		// rbPrefix.setMinimumSize(new Dimension(55, 20));
		// rbPrefix.setPreferredSize(new Dimension(55, 20));
		bgAffix.add(rbPrefix);
		rbSuffix.setText(sRbSuffix);
		// rbSuffix.setMaximumSize(new Dimension(55, 20));
		// rbSuffix.setMinimumSize(new Dimension(55, 20));
		// rbSuffix.setPreferredSize(new Dimension(55, 20));
		bgAffix.add(rbSuffix);
		cbSdoGeometryFormat.setSelected(true);
		cbSdoGeometryFormat.setText("Render directly from SDO_GEOMETRY:");
		cbSdoGeometryFormat.setHorizontalTextPosition(SwingConstants.LEADING);
		cbSdoGeometryFormat.setHorizontalAlignment(SwingConstants.RIGHT);

		// Finalise
		//
		// cbSdoGeometryFormat.setSize(new Dimension(255, 18));
		spatialndexPanel.add(tfAffixString, new XYConstraints(145, -4, 115, 20));
		spatialndexPanel.add(lblAffix, new XYConstraints(65, -4, 75, 15));
		spatialndexPanel.add(rbPrefix, new XYConstraints(145, 16, 55, 20));
		spatialndexPanel.add(rbSuffix, new XYConstraints(200, 16, 55, 20));
		pnlMiscellaneous.add(tfGeometryColumnName, new XYConstraints(208, 308, 60, 20));
		pnlMiscellaneous.add(lblGeometryColumnName, new XYConstraints(8, 308, 195, 15));
		pnlMiscellaneous.add(lblQueryLimit, new XYConstraints(28, 161, 210, 15));
		pnlMiscellaneous.add(tfQueryLimit, new XYConstraints(238, 158, 35, 20));
		pnlMiscellaneous.add(cbSQLSchemaPrefix, new XYConstraints(13, 283, 255, 20));
		pnlMiscellaneous.add(cbDebug, new XYConstraints(8, 258, 260, 20));
		pnlMiscellaneous.add(cbFastPicklerConversion, new XYConstraints(13, 233, 255, 20));
		pnlMiscellaneous.add(lblDefFetchSize, new XYConstraints(28, 113, 210, 15));
		pnlMiscellaneous.add(tfDefFetchSize, new XYConstraints(238, 108, 30, 20));
		pnlMiscellaneous.add(lblRefreshImage, new XYConstraints(23, 138, 215, 15));
		pnlMiscellaneous.add(tfRefreshImage, new XYConstraints(238, 133, 30, 20));

		pnlMiscellaneous.add(spatialndexPanel, new XYConstraints(3, 8, 275, 70));
		pnlMiscellaneous.add(cbLogSearchStats, new XYConstraints(8, 183, 260, 20));
		pnlMiscellaneous.add(tfTableCountLimit, new XYConstraints(163, 83, 105, 20));
		pnlMiscellaneous.add(lblTableCountLimit, new XYConstraints(73, 86, 86, 14));
		pnlMiscellaneous.add(pnlMbrSource, new XYConstraints(283, 8, 230, 70));
		pnlMiscellaneous.add(cbSdoGeometryFormat, new XYConstraints(13, 208, 255, 20));

		/**
		 * *****************************************************************
		 ** ***************** Import / Export Panel (4) *********************
		 *****************************************************************/

		pnlImportExport.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		pnlImportExport.setLayout(xYLayoutImportExport);
		// pnlImportExport.setSize(new Dimension(530, 380));
		// pnlImportExport.setPreferredSize(new Dimension(530, 380));
		// pnlImportExport.setMinimumSize(new Dimension(530, 380));
		// pnlImportExport.setMaximumSize(new Dimension(530, 380));
		// pnlImportExport.setBounds(new Rectangle(2, 20, 530, 380));

		pnlNullValueSubstitution.setBorder(BorderFactory.createTitledBorder("NULL Value Substitution"));
		pnlNullValueSubstitution.setLayout(xYLayoutNullValueSubstitution);
		tfNullString.setHorizontalAlignment(JTextField.RIGHT);
		tfNullString.setToolTipText("Value to write when text column is NULL");
		lblNullString.setText("String (Varchar etc) NULL Value:");
		lblNullString.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNullString.setLabelFor(tfNullString);
		lblNullNumber.setText("Number NULL Value:");
		lblNullNumber.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNullNumber.setLabelFor(tfNullNumber);
		tfNullNumber.setText("-999.999");
		tfNullNumber.setToolTipText("Value to write when number column is NULL");
		tfNullNumber.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tfNullNumber_actionPerformed(e);
			}
		});
		tfNullDate.setText("1900-01-01");
		tfNullDate.setToolTipText("Value to write when Date/Timestamp column is null");
		lblNullDate.setText("Date / Timestamp NULL Value:");
		lblNullDate.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNullDate.setLabelFor(tfNullDate);

		tfNullInteger.setText("-9");
		tfNullInteger.setToolTipText("Value for NULL Integer Column Values");
		tfNullInteger.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tfNullInteger_actionPerformed(e);
			}
		});
		lblNullInteger.setText("Integer NULL Value:");
		lblNullInteger.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNullInteger.setLabelFor(tfNullInteger);
		cbDbaseNullWriteString.setText("DBase Write Empty String (all Fields):");
		cbDbaseNullWriteString.setHorizontalAlignment(SwingConstants.TRAILING);
		cbDbaseNullWriteString.setHorizontalTextPosition(SwingConstants.LEFT);

		pnlNullValueSubstitution.add(cbDbaseNullWriteString, new XYConstraints(25, 126, 196, 18));
		pnlNullValueSubstitution.add(lblNullInteger, new XYConstraints(24, 71, 170, 15));
		pnlNullValueSubstitution.add(tfNullInteger, new XYConstraints(210, 71, 80, 20));
		pnlNullValueSubstitution.add(lblNullDate, new XYConstraints(10, 101, 185, 15));
		pnlNullValueSubstitution.add(tfNullDate, new XYConstraints(210, 101, 85, 20));
//		pnlNullValueSubstitution.add(tfNullNumber, new XYConstraints(210, 41, 85, 20));
		pnlNullValueSubstitution.add(lblNullNumber, new XYConstraints(9, 41, 185, 15));
		pnlNullValueSubstitution.add(lblNullString, new XYConstraints(10, 11, 185, 15));
		pnlNullValueSubstitution.add(tfNullString, new XYConstraints(210, 6, 60, 20));
		pnlColumnShorten.add(lblDBFColumnTruncation, new XYConstraints(20, -1, 170, 15));
		pnlColumnShorten.add(rbBegin10, new XYConstraints(200, -4, 95, 20));
		pnlColumnShorten.add(rbLast10, new XYConstraints(200, 16, 95, 20));
		bgColumnShorten.add(rbBegin10);
		bgColumnShorten.add(rbLast10);
		pnlImportExport.add(jLabel1, new XYConstraints(13, 258, 146, 14));
		pnlImportExport.add(cmbShapePolygonOrientation, new XYConstraints(173, 253, 135, 20));
		pnlImportExport.add(pnlColumnShorten, new XYConstraints(5, 185, 310, 65));
		pnlImportExport.add(pnlNullValueSubstitution, new XYConstraints(5, 5, 310, 175));

		/* ********************** Add Tabs to Tab Set ******************** */

		tpPreferences.addTab("pnlVisualisation", pnlVisualisation);
		tpPreferences.addTab("SpatialView", pnlSpatialView);
		tpPreferences.addTab("Miscellaneous", pnlMiscellaneous);
		tpPreferences.addTab("Import/Export", pnlImportExport);

		this.add(tpPreferences);
		sldrVertexMarkSize.setModel(new DefaultBoundedRangeModel(8, 0, 4, 32));
		lblMarkerSize.setText("Vertex Marker Size (points):");
		lblMarkerSize.setHorizontalAlignment(SwingConstants.TRAILING);
		lblMarkerSize.setHorizontalTextPosition(SwingConstants.RIGHT);
		jLabel1.setText("Shapefile Polygon Orientation:");
		jLabel1.setLabelFor(cmbShapePolygonOrientation);
	}

	private void btnSRID_actionPerformed(ActionEvent e) {
		// get SRID to pass to SRIDPanel
		//
		String srid = Constants.NULL;
		srid = Strings.isEmpty(this.tfSRID.getText()) ? Constants.NULL : this.tfSRID.getText().trim();
		// Pass SRID to SRIDPanel
		//
//		SRIDPanel sp = SRIDPanel.getInstance();
//		boolean status = sp.initialise(null/* SRIDPanel will find its own connection */, srid);
//		if (status == true) {
//			sp.setLocationRelativeTo(this);
//			sp.setVisible(true);
//			if (!sp.formCancelled()) {
//				tfSRID.setText(sp.getSRID());
//			}
//		}
	}

	private void cmbSdoGeometryBracketType_actionPerformed(ActionEvent e) {
		String sdoGeomBracket = this.cmbSdoGeometryBracket.getSelectedItem().toString();
		if (sdoGeomBracket.equalsIgnoreCase(Constants.bracketType.NONE.toString().toUpperCase())) {
			this.cbSdoGeomCoordNumbering.setEnabled(false);
			this.cbSdoGeomCoordNumbering.setSelected(false);
		} else {
			this.cbSdoGeomCoordNumbering.setEnabled(true);
		}
	}

	private void cmbSdoGeometryVisualFormat_actionPerformed(ActionEvent e) {
		String sdoGeomVisualFormat = this.cmbSdoGeometryVisualFormat.getSelectedItem().toString();
		if (sdoGeomVisualFormat.equalsIgnoreCase(Constants.renderType.SDO_GEOMETRY.toString().toUpperCase())) {
			this.cbColourSdoGeometry.setEnabled(true);
		} else {
			this.cbColourSdoGeometry.setEnabled(false);
			this.cbColourSdoGeometry.setSelected(false);
		}
	}

	private void mapBackgroundButtonActionPerformed(ActionEvent evt) {
		final Color backgroundColor = JColorChooser.showDialog(this, "Spatial properties - Map Background Color",
				this.mapBackground);
		if (backgroundColor != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setMapBackground(backgroundColor);
				}
			});
		}
	}

	private void btnFeatureColorActionPerformed(ActionEvent evt) {
		final Color backgroundColor = JColorChooser.showDialog(this, "Spatial properties - Feature Color",
				this.featureColour);
		if (backgroundColor != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setFeatureColour(backgroundColor);
				}
			});
		}
	}

	// Getters and Setters for variables used in other classes

	public void setRandomRendering(boolean _value) {
		this.cbRandomRendering.setSelected(_value);
	}

	public boolean isRandomRendering() {
		return this.cbRandomRendering.isSelected();
	}

	public void setDefGeomColName(String _name) {
		this.tfGeometryColumnName.setText(_name);
	}

	public String getDefGeomColName() {
		return this.tfGeometryColumnName.getText();
	}

	public void setSQLSchemaPrefix(boolean _draw) {
		this.cbSQLSchemaPrefix.setSelected(_draw);
	}

	public boolean getSQLSchemaPrefix() {
		return this.cbSQLSchemaPrefix.isSelected();
	}

	public void setDrawQueryGeometry(boolean _draw) {
		this.cbDrawQueryGeometry.setSelected(_draw);
	}

	public boolean getDrawQueryGeometry() {
		return this.cbDrawQueryGeometry.isSelected();
	}

	public void setNumberOfFeaturesVisible(boolean _visibility) {
		this.cbNumberOfFeatures.setSelected(_visibility);
	}

	public boolean getNumberOfFeaturesVisible() {
		return this.cbNumberOfFeatures.isSelected();
	}

	public void setMapScaleVisible(boolean _visibility) {
		this.cbMapScale.setSelected(_visibility);
	}

	public boolean getMapScaleVisible() {
		return this.cbMapScale.isSelected();
	}

	public void setFastPicklerConversion(boolean _fast_pickler) {
		this.cbFastPicklerConversion.setSelected(_fast_pickler);
	}

	public boolean getFastPicklerConversion() {
		return this.cbFastPicklerConversion.isSelected();
	}

	// Preview image options

	public void setPreviewImageWidth(int _width) {
		this.tfImageWidth.setText(String.valueOf(_width));
	}

	public int getPreviewImageWidth() {
		if (Strings.isEmpty(this.tfImageWidth.getText())) {
			this.tfImageWidth.setText("100");
		}
		return Integer.valueOf(this.tfImageWidth.getText()).intValue();
	}

	public void setPreviewImageHeight(int _height) {
		this.tfImageHeight.setText(String.valueOf(_height));
	}

	public int getPreviewImageHeight() {
		if (Strings.isEmpty(this.tfImageHeight.getText())) {
			this.tfImageHeight.setText("100");
		}
		return Integer.valueOf(this.tfImageHeight.getText()).intValue();
	}

	public void setMinMbrAspectRatio(double _ratio) {
		this.tfMinMbrAspectRatio.setText(String.valueOf(_ratio));
	}

	public double getMinMbrAspectRatio() {
		if (Strings.isEmpty(this.tfMinMbrAspectRatio.getText())) {
			this.tfMinMbrAspectRatio.setText("3.0");
		}
		return Double.parseDouble(this.tfMinMbrAspectRatio.getText());
	}

	public void setPreviewImageVertexLabeling(boolean _label) {
		this.cbPreviewVertexNumbering.setSelected(_label);
	}

	public boolean getPreviewImageVertexLabeling() {
		return this.cbPreviewVertexNumbering.isSelected();
	}

	public void setPreviewImageHorizontalLabels(boolean _horizontal) {
		this.cbPreviewHorizontalLabels.setSelected(_horizontal);
	}

	public boolean getPreviewImageHorizontalLabels() {
		return this.cbPreviewHorizontalLabels.isSelected();
	}

	// ------------------------------------------------------
	public void setGroupingSeparator(boolean _value) {
		this.cbGroupingSeparator.setSelected(_value);
	}

	public boolean getGroupingSeparator() {
		return this.cbGroupingSeparator.isSelected();
	}

	public void setPrefixWithMDSYS(boolean _value) {
		this.cbPrefixWithMDSYS.setSelected(_value);
	}

	public boolean getPrefixWithMDSYS() {
		return this.cbPrefixWithMDSYS.isSelected();
	}

	public void setDbaseNullWriteString(boolean _value) {
		this.cbDbaseNullWriteString.setSelected(_value);
	}

	public boolean getDbaseNullWriteString() {
		return this.cbDbaseNullWriteString.isSelected();
	}

	public void setNullString(String _value) {
		this.tfNullString.setText(_value);
	}

	public String getNullString() {
		return this.tfNullString.getText();
	}

	public void setNullNumber(double _value) {
		this.tfNullNumber.setText(String.valueOf(_value));
	}

	public double getNullNumber() {
		return Double.valueOf(this.tfNullNumber.getText()).doubleValue();
	}

	public void setNullInteger(int _value) {
		this.tfNullInteger.setText(String.valueOf(_value));
	}

	public int getNullInteger() {
		return Integer.valueOf(this.tfNullInteger.getText()).intValue();
	}

	public void setNullDate(String _value) {
		this.tfNullDate.setText(_value);
	}

	public String getNullDate() {
		return this.tfNullDate.getText();
	}

	public void setNN(boolean _sdo_nn) {
		this.cbSdoNN.setSelected(_sdo_nn);
	}

	public boolean isNN() {
		return this.cbSdoNN.isSelected();
	}

	public boolean getNN() {
		return this.cbSdoNN.isSelected();
	}

	public void setLogSearchStats(boolean _stats) {
		this.cbLogSearchStats.setSelected(_stats);
	}

	public boolean getLogSearchStats() {
		return this.cbLogSearchStats.isSelected();
	}

	public void setPanZoomPercentage(int _percentage) {
		this.sldrPanZoomChange.setValue(_percentage);
	}

	public int getTableCountLimit() {
		return Integer.valueOf(this.tfTableCountLimit.getText()).intValue();
	}

	public void setTableCountLimit(int _limit) {
		this.tfTableCountLimit.setText(String.valueOf(_limit));
	}

	public int getQueryLimit() {
		return Integer.valueOf(this.tfQueryLimit.getText()).intValue();
	}

	public void setQueryLimit(int _limit) {
		this.tfQueryLimit.setText(String.valueOf(_limit));
	}

	public int getPanZoomPercentage() {
		return this.sldrPanZoomChange.getValue();
	}

	public void setSchemaPrefix(boolean _prefix) {
		this.cbSchemaPrefix.setSelected(_prefix);
	}

	public boolean getSchemaPrefix() {
		return this.cbSchemaPrefix.isSelected();
	}

	protected boolean isDBFShortenBegin10() {
		return this.rbBegin10.isSelected();
	}

	public void setDBFColumnShorten(boolean _begin) {
		if (_begin)
			this.rbBegin10.setSelected(true);
		else
			this.rbLast10.setSelected(true);
	}

	public void setDefaultSRID(String _SRID) {
		tfSRID.setText(Strings.isEmpty(_SRID) ? Constants.NULL : _SRID);
	}

	public String getDefaultSRID() {
		return Strings.isEmpty(tfSRID.getText()) ? Constants.NULL : tfSRID.getText();
	}

	public void setLayerMBRSource(String _source) {
		if (_source.equalsIgnoreCase(Constants.CONST_LAYER_MBR_INDEX))
			bgLayerMBR.setSelected(rbLayerMBRIndex.getModel(), true);
		else
			bgLayerMBR.setSelected(rbLayerMBRMetadata.getModel(), true);
	}

	public String getLayerMBRSource() {
		return rbLayerMBRIndex.isSelected() ? Constants.CONST_LAYER_MBR_INDEX : Constants.CONST_LAYER_MBR_METADATA;
	}

	public void setMBRSaveSize(int _MBRSaveSize) {
		this.sldrMBRSaveSize.setValue(_MBRSaveSize);
		this.lblMBRSaveSize.setText(String.format(this.sLblMBRSaveSize + " %3d", this.sldrMBRSaveSize.getValue()));
	}

	public int getMBRSaveSize() {
		return this.sldrMBRSaveSize.getValue();
	}

	public void setMinResolution(boolean _minResolution) {
		cbMinResolution.setSelected(_minResolution);
	}

	public boolean getMinResolution() {
		return this.cbMinResolution.isSelected();
	}

	public void setSearchPixels(int _searchPixels) {
		sldrSearchPixels.setValue(_searchPixels);
	}

	public int getSearchPixels() {
		return sldrSearchPixels.getValue();
	}

	public void setPreviewVertexMarkSize(int _searchMarkSize) {
		this.sldrVertexMarkSize.setValue(_searchMarkSize);
	}

	public int getPreviewVertexMarkSize() {
		return this.sldrVertexMarkSize.getValue();
	}

	public Color getMapBackground() {
		return this.mapBackground;
	}

	public void setMapBackground(Color _mapBackground) {
		this.mapBackground = _mapBackground;
		this.btnMapBackground.setBackground(_mapBackground);
	}

	public Color getFeatureColour() {
		return this.featureColour;
	}

	public void setFeatureColour(Color _featureColour) {
		this.featureColour = _featureColour;
		this.btnFeatureColour.setBackground(_featureColour);
	}

	public Color getOrphanColour() {
		return this.orphanColour;
	}

	public void setOrphanColour(Color _colour) {
		this.orphanColour = _colour;
		this.btnOrphanColour.setBackground(_colour);
	}

	public Color getMissingColour() {
		return this.missingColour;
	}

	public void setMissingColour(Color _colour) {
		this.missingColour = _colour;
		this.btnMissingColour.setBackground(_colour);
	}

	public Color getCorrectColour() {
		return this.correctColour;
	}

	public void setCorrectColour(Color _colour) {
		this.correctColour = _colour;
		this.btnCorrectColour.setBackground(_colour);
	}

	public Color getHighlightColour() {
		return this.correctColour;
	}

	public void setHighlightColour(Color _colour) {
		this.highlightColour = _colour;
		this.btnHighlightColour.setBackground(_colour);
	}

	public Color getSelectionColour() {
		return this.selectionColour;
	}

	public void setSelectionColour(Color _colour) {
		this.selectionColour = _colour;
		this.btnSelectionColour.setBackground(_colour);
	}

	public String getSdoGeometryVisualFormat() {
		return this.cmbSdoGeometryVisualFormat.getSelectedItem().toString();
	}

	public void setSdoGeometryVisualFormat(Constants.renderType _visualFormat) {
		this.setSdoGeometryVisualFormat(_visualFormat.toString());
	}

	public void setSdoGeometryVisualFormat(String _visualFormat) {
		for (int s = 0; s < this.cmbSdoGeometryVisualFormat.getItemCount(); s++) {
			if (this.cmbSdoGeometryVisualFormat.getItemAt(s).toString().equalsIgnoreCase(_visualFormat)) {
				this.cmbSdoGeometryVisualFormat.setSelectedIndex(s);
				return;
			}
		}
		this.cmbSdoGeometryVisualFormat.setSelectedIndex(0);
	}

	public boolean getSdoGeometryFormat() {
		return this.cbSdoGeometryFormat.isSelected();
	}

	public void setSdoGeometryFormat(boolean _geomFormat) {
		this.cbSdoGeometryFormat.setSelected(_geomFormat);
	}

	protected void setTOCPosition(String _position) {
		this.cmbTOCPosition.setSelectedItem(_position);
	}

	protected String getTOCPosition() {
		return this.cmbTOCPosition.getSelectedItem().toString();
	}

	protected void setNewLayerPosition(String _position) {
		this.cmbNewLayerPosition.setSelectedItem(_position);
	}

	protected String getNewLayerPosition() {
		return this.cmbNewLayerPosition.getSelectedItem().toString();
	}

	protected void setColourSdoGeomElements(boolean _colourSdoGeometry) {
		this.cbColourSdoGeometry.setSelected(_colourSdoGeometry);
	}

	protected boolean getColourSdoGeomElements() {
		return this.cbColourSdoGeometry.isSelected();
	}

	protected void setSdoGeometryBracketType(Constants.bracketType _SdoGeometryBracket) {
		this.setSdoGeometryBracketType(_SdoGeometryBracket.toString());
	}

	protected void setSdoGeometryBracketType(String _SdoGeometryBracket) {
		// Have to do this as setSelectedObject refuses to work.
		for (int i = 0; i < this.cmbSdoGeometryBracket.getModel().getSize(); i++) {
			if (this.cmbSdoGeometryBracket.getItemAt(i).toString().equalsIgnoreCase(_SdoGeometryBracket)) {
				this.cmbSdoGeometryBracket.setSelectedIndex(i);
				return;
			}
		}
		this.cmbSdoGeometryBracket.setSelectedIndex(0);
	}

	protected String getSdoGeometryBracketType() {
		return this.cmbSdoGeometryBracket.getSelectedItem().toString();
	}

	protected String getShapePolygonOrientation() {
		return this.cmbShapePolygonOrientation.getSelectedItem().toString();
	}

	protected void setShapePolygonOrientation(String _shapePolygonOrder) {
		for (int i = 0; i < this.cmbShapePolygonOrientation.getModel().getSize(); i++) {
			if (this.cmbShapePolygonOrientation.getItemAt(i).toString().equalsIgnoreCase(_shapePolygonOrder)) {
				this.cmbShapePolygonOrientation.setSelectedIndex(i);
				return;
			}
		}
		this.cmbShapePolygonOrientation.setSelectedIndex(0);
	}

	protected void setPreviewVertexMark(String _markerType) {
		// Passed in _markerType is marker type with underscores in names not spaces
		// this is because we store, in preferences MARKER_TYPE.toString().
		this.cmbPreviewVertexMark.setModel(PointMarker.getComboBoxModel()); // Underscores in MARKER_TYPE names are
																			// replaced with spaces
		this.cmbPreviewVertexMark.setRenderer(PointMarker.getPointTypeRenderer());
		this.cmbPreviewVertexMark.setMaximumRowCount(10);
		String selectedMarker = Strings.TitleCase(_markerType.replaceAll("_", " "));
		for (int i = 0; i < this.cmbPreviewVertexMark.getItemCount(); i++) {
			if (this.cmbPreviewVertexMark.getItemAt(i).toString().equals(selectedMarker)) {
				this.cmbPreviewVertexMark.setSelectedIndex(i);
				return;
			}
		}
		this.cmbPreviewVertexMark.setSelectedIndex(0);
	}

	protected String getPreviewVertexMark() {
		String svm = this.cmbPreviewVertexMark.getSelectedItem().toString();
		PointMarker.MARKER_TYPES mt = PointMarker.getMarkerType(svm);
		String retStr = (mt == null ? "NONE" : mt.toString());
		return retStr;
	}

	protected boolean isSdoGeometryBracketing() {
		String geomBracket = (String) this.cmbSdoGeometryBracket.getSelectedItem();
		return geomBracket.equalsIgnoreCase(Constants.bracketType.NONE.toString().toUpperCase());
	}

	protected void setSdoGeometryCoordinateNumbering(boolean _SdoGeometryCoordinateNumbering) {
		this.cbSdoGeomCoordNumbering.setSelected(_SdoGeometryCoordinateNumbering);
		String sdoGeomBracket = this.cmbSdoGeometryBracket.getSelectedItem().toString();
		if (sdoGeomBracket.equalsIgnoreCase(Constants.bracketType.NONE.toString().toUpperCase())) {
			this.cbSdoGeomCoordNumbering.setEnabled(false);
			this.cbSdoGeomCoordNumbering.setSelected(false);
		}
	}

	protected boolean getSdoGeometryCoordinateNumbering() {
		return this.cbSdoGeomCoordNumbering.isSelected();
	}

	protected void setColourDimInfo(boolean _colourDimInfo) {
		this.cbColourDimInfo.setSelected(_colourDimInfo);
	}

	protected boolean getColourDimInfo() {
		return this.cbColourDimInfo.isSelected();
	}

	protected boolean getSuffixFlag() {
		return this.rbSuffix.isSelected();
	}

	protected void setSuffixFlag(boolean _suffixFlag) {
		if (_suffixFlag)
			rbSuffix.setSelected(true);
		else
			rbPrefix.setSelected(true);
	}

	protected String getAffixString() {
		return this.tfAffixString.getText();
	}

	protected void setAffixString(String _affixString) {
		this.tfAffixString.setText(_affixString);
	}

	protected int getFetchSize() {
		return Integer.valueOf(this.tfDefFetchSize.getText());
	}

	protected void setFetchSize(int _fetchSize) {
		/**
		 * If size is < 0, we set it on 0
		 */
		if (_fetchSize < 0)
			_fetchSize = 0;
		this.tfDefFetchSize.setText(String.valueOf(_fetchSize));
	}

	public int getImageRefreshMS() {
		return Integer.valueOf(this.tfRefreshImage.getText());
	}

	public void setImageRefreshMS(int _imageRefreshMS) {
		/**
		 * Refresh must be > 100
		 */
		if (_imageRefreshMS < 100)
			_imageRefreshMS = 100;
		this.tfRefreshImage.setText(String.valueOf(_imageRefreshMS));
	}

	private void tfNullInteger_actionPerformed(ActionEvent e) {
		if (Strings.isEmpty(tfNullInteger.getText())) {
			JOptionPane.showMessageDialog(null, Resources.getString("INTEGER_ONLY_DIGITS"), MainSettings.EXTENSION_NAME,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void tfNullNumber_actionPerformed(ActionEvent e) {
		if (Strings.isEmpty(tfNullNumber.getText())) {
			JOptionPane.showMessageDialog(null, rBundle.getString("NUMBER_ONLY_DIGITS"), MainSettings.EXTENSION_NAME,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void btnCorrectMetadata_actionPerformed(ActionEvent e) {
		final Color correctColor = JColorChooser.showDialog(this, "Spatial properties - Correct Metadata Color",
				this.correctColour);
		if (correctColor != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setCorrectColour(correctColor);
				}
			});
		}
	}

	private void btnOrphanColour_actionPerformed(ActionEvent e) {
		final Color orphanColor = JColorChooser.showDialog(this, "Spatial properties - Orphan Metadata Color",
				this.orphanColour);
		if (orphanColor != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setOrphanColour(orphanColor);
				}
			});
		}
	}

	private void btnMissingMetadata_actionPerformed(ActionEvent e) {
		final Color missingColor = JColorChooser.showDialog(this, "Spatial properties - Missing Metadata Color",
				this.missingColour);
		if (missingColor != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setMissingColour(missingColor);
				}
			});
		}
	}

	private void btnHighlightColour_actionPerformed(ActionEvent e) {
		final Color highlightColor = JColorChooser.showDialog(this, "Spatial properties - Metadata Highlight Color",
				this.highlightColour);
		if (highlightColor != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setHighlightColour(highlightColor);
				}
			});
		}
	}

	private void btnSelectionColour_actionPerformed(ActionEvent e) {
		final Color selectionColor = JColorChooser.showDialog(this, "Spatial properties - Metadata Highlight Color",
				this.selectionColour);
		if (selectionColor != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setHighlightColour(selectionColor);
				}
			});
		}
	}

	private void cbDebug_actionPerformed(ActionEvent e) {
		if (cbDebug.isSelected()) {
			JPanel panel = new JPanel();
			JLabel label = new JLabel("Password:");
			JPasswordField pass = new JPasswordField(10);
			panel.add(label);
			panel.add(pass);
			String[] options = new String[] { "OK", "Cancel" };
			int option = JOptionPane.showOptionDialog(null, panel, "GeoRaptor Debug Password", JOptionPane.NO_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
			if (option == 0) // pressing OK button
			{
				char[] passwd = pass.getPassword();
				String password = new String(passwd);
				if (password.equals(Constants.DEBUG_PASSWORD)) {
					Constants.DEBUG = true;
				}
			}
		} else {
			Constants.DEBUG = false;
		}
	}

	private void tfGeometryColumnName_actionPerformed(ActionEvent e) {
		if (Strings.isEmpty(tfGeometryColumnName.getText())) {
			tfGeometryColumnName.setText(prefs.getDefGeomColName());
		}
	}

	@Override
	public void onEntry(TraversableContext tc) {
		super.onEntry(tc);

		// Load prefs into the panel controls' states.
		//
		this.prefs = getPreferences(tc);
		this.setColourSdoGeomElements(prefs.isColourSdoGeomElements());
		this.setSdoGeometryBracketType(prefs.getSdoGeometryBracketType().toString());
		this.setSdoGeometryCoordinateNumbering(prefs.isSdoGeometryCoordinateNumbering());
		this.setSdoGeometryVisualFormat(prefs.getSdoGeometryVisualFormat().toString());
		this.cbColourDimInfo.setSelected(prefs.isColourDimInfo());
		this.setSuffixFlag(prefs.isSuffixFlag());
		this.tfAffixString.setText(prefs.getAffixString());
		this.tfRefreshImage.setText(String.valueOf(prefs.getImageRefreshMS()));
		this.tfDefFetchSize.setText(String.valueOf(prefs.getFetchSize()));
		this.cmbNewLayerPosition.setSelectedItem(prefs.getNewLayerPosition());
		this.cmbTOCPosition.setSelectedItem(prefs.getTOCPosition());
		this.sldrSearchPixels.setValue(prefs.getSearchPixels());
		this.cbSchemaPrefix.setSelected(prefs.isSchemaPrefix());

		this.cbSdoGeometryFormat.setSelected(prefs.isSdoGeometryProcessingFormat());
		this.sldrMBRSaveSize.setValue(prefs.getMBRSaveSize());
		this.lblMBRSaveSize.setText(String.format(this.sLblMBRSaveSize + " %3d", this.sldrMBRSaveSize.getValue()));
		this.cbMinResolution.setSelected(prefs.isMinResolution());
		this.setLayerMBRSource(prefs.getLayerMBRSource());
		this.setDefaultSRID(prefs.getSRID());

		this.setMapBackground(prefs.getMapBackground());
		this.setFeatureColour(prefs.getFeatureColour());
		this.setOrphanColour(prefs.getOrphanColour());
		this.setMissingColour(prefs.getMissingColour());
		this.setCorrectColour(prefs.getCorrectColour());
		this.setHighlightColour(prefs.getHighlightColour());
		this.setSelectionColour(prefs.getSelectionColour());

		this.sldrPanZoomChange.setValue(prefs.getPanZoomPercentage());
		this.setTableCountLimit(prefs.getTableCountLimit());
		this.setLogSearchStats(prefs.isLogSearchStats());
		this.setNN(prefs.isNN());

		// Import / Export NULLs
		this.setNullString(prefs.getNullString());
		this.setNullNumber(prefs.getNullNumber());
		this.setNullInteger(prefs.getNullInteger());
		this.setNullDate(prefs.getNullDate());
		this.setDbaseNullWriteString(prefs.getDbaseNullWriteString());
		this.setPrefixWithMDSYS(prefs.getPrefixWithMDSYS());
		this.setGroupingSeparator(prefs.getGroupingSeparator());
		this.setShapePolygonOrientation(prefs.getShapePolygonOrientation().toString());

		this.setPreviewImageHeight(prefs.getPreviewImageHeight());
		this.setPreviewImageWidth(prefs.getPreviewImageWidth());
		this.setPreviewImageVertexLabeling(prefs.isPreviewImageVertexLabeling());
		this.setPreviewImageHorizontalLabels(prefs.isPreviewImageHorizontalLabels());
		this.setMinMbrAspectRatio(prefs.getMinMbrAspectRatio());
		this.setPreviewVertexMark(prefs.getPreviewVertexMark());
		this.setPreviewVertexMarkSize(prefs.getPreviewVertexMarkSize());

		this.setFastPicklerConversion(prefs.isFastPicklerConversion());
		this.setMapScaleVisible(prefs.isMapScaleVisible());
		this.setNumberOfFeaturesVisible(prefs.isNumberOfFeaturesVisible());
		this.setDrawQueryGeometry(prefs.isDrawQueryGeometry());
		this.setDBFColumnShorten(prefs.isDBFShortenBegin10());
		this.cbDebug.setSelected(Constants.DEBUG);
		this.setSQLSchemaPrefix(prefs.getSQLSchemaPrefix());
		this.setQueryLimit(prefs.getQueryLimit());
		this.setDefGeomColName(prefs.getDefGeomColName());
		this.setRandomRendering(prefs.isRandomRendering());
	}

	@Override
	public void onExit(TraversableContext tc) throws TraversalException {
		super.onExit(tc);

		final Preferences prefs = getPreferences(tc);

		// Save the panel controls' states to prefs.
		//
		prefs.setSuffixFlag(this.getSuffixFlag());
		prefs.setAffixString(this.tfAffixString.getText());
		prefs.setImageRefreshMS(Integer.parseInt(this.tfRefreshImage.getText()));
		prefs.setFetchSize(Integer.parseInt(this.tfDefFetchSize.getText()));
		// Can't be changed here:
		// prefs.setMainLayerSplitPos(Constants.VAL_MAIN_LAYER_SPLIT_POS);
		// Can't be changed here: prefs.setMainSplitPos(Constants.VAL_MAIN_SPLIT_POS);
		prefs.setColourSdoGeomElements(this.getColourSdoGeomElements());
		prefs.setSdoGeometryBracketType(this.getSdoGeometryBracketType());
		prefs.setShapePolygonOrientation(this.getShapePolygonOrientation());
		prefs.setSdoGeometryVisualFormat(this.getSdoGeometryVisualFormat());
		prefs.setSdoGeometryCoordinateNumbering(this.cbSdoGeomCoordNumbering.isSelected());
		prefs.setColourDimInfo(this.cbColourDimInfo.isSelected());
		prefs.setNewLayerPosition(this.cmbNewLayerPosition.getSelectedItem().toString());
		prefs.setTOCPosition(this.cmbTOCPosition.getSelectedItem().toString());
		prefs.setSdoGeometryFormat(this.cbSdoGeometryFormat.isSelected());
		prefs.setMapBackground(getMapBackground());
		prefs.setFeatureColour(getFeatureColour());
		prefs.setOrphanColour(getOrphanColour());
		prefs.setMissingColour(getMissingColour());
		prefs.setCorrectColour(getCorrectColour());
		prefs.setHighlightColour(getHighlightColour());
		prefs.setSelectionColour(getSelectionColour());
		prefs.setSearchPixels(this.getSearchPixels());
		prefs.setMBRSaveSize(this.getMBRSaveSize());
		prefs.setMinResolution(this.getMinResolution());
		prefs.setLayerMBRSource(this.getLayerMBRSource());
		prefs.setSRID(this.getDefaultSRID());
		prefs.setSchemaPrefix(this.getSchemaPrefix());
		prefs.setPanZoomPercentage(this.getPanZoomPercentage());
		prefs.setTableCountLimit(this.getTableCountLimit());
		prefs.setLogSearchStats(this.getLogSearchStats());
		prefs.setNN(this.isNN());
		// Import / Export NULLs
		prefs.setNullString(this.getNullString());
		prefs.setNullNumber(this.getNullNumber());
		prefs.setNullInteger(this.getNullInteger());
		prefs.setNullDate(this.getNullDate());
		prefs.setDbaseNullWriteString(this.getDbaseNullWriteString());
		prefs.setPrefixWithMDSYS(this.getPrefixWithMDSYS());
		prefs.setGroupingSeparator(this.getGroupingSeparator());
		prefs.setPreviewImageHeight(this.getPreviewImageHeight());
		prefs.setPreviewImageWidth(this.getPreviewImageWidth());
		prefs.setMinMbrAspectRatio(this.getMinMbrAspectRatio());
		prefs.setPreviewImageVertexLabeling(this.getPreviewImageVertexLabeling());
		prefs.setPreviewImageHorizontalLabels(this.getPreviewImageHorizontalLabels());
		prefs.setPreviewVertexMark(this.getPreviewVertexMark());
		prefs.setPreviewVertexMarkSize(this.getPreviewVertexMarkSize());
		prefs.setFastPicklerConversion(this.getFastPicklerConversion());
		prefs.setMapScaleVisible(this.getMapScaleVisible());
		prefs.setNumberOfFeaturesVisible(this.getNumberOfFeaturesVisible());
		prefs.setDrawQueryGeometry(this.getDrawQueryGeometry());
		prefs.setDBFColumnShorten(this.isDBFShortenBegin10());
		prefs.setSQLSchemaPrefix(this.getSQLSchemaPrefix());
		prefs.setQueryLimit(this.getQueryLimit());
		prefs.setDefGeomColName(this.getDefGeomColName());
		prefs.setRandomRendering(this.isRandomRendering());
	}

}
