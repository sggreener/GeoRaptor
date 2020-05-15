package org.GeoRaptor;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

//import org.GeoRaptor.SpatialView.SpatialViewSettings;
//import org.GeoRaptor.SpatialView.SpatialViewSettings;
//import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;
//import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** 
 * @author BessieGong 
 * Comment out all content related to the Spatial view
 * Use Resources class to replace propertiesFile
 * 
 * Class for control all other settings classes.
 */
public class MainSettings {
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.MainSettings");

	/**
	 * Properties File Manager
	 **/
	private static final String propertiesFile = "org.GeoRaptor.resource.Res";
	//protected PropertiesManager propertyManager;

	//protected static SpatialViewSettings spatialViewS;
	protected static MainSettings classInstance;
	protected static Preferences mainPrefs;
	/**
	 * Root XML element for settings storage file
	 */
	protected Element geoRaptorE;

	// Some public constants from resources file
	//
	public static String MENU_ITEM = Constants.GEORAPTOR;
	public static String VERSION = "";
	public static String EXTENSION_NAME = MENU_ITEM;
	public static String XML_VERSION_MESSAGE = "";

	/**
	 * We cannot call constructor of this class
	 */
	private MainSettings() {
		// Instantiate preferences
		//
		oracle.ide.config.Preferences prefs = oracle.ide.config.Preferences.getPreferences();
		MainSettings.mainPrefs = Preferences.getInstance(prefs);
		// Get localisation file
		//
//		this.propertyManager = new PropertiesManager(MainSettings.propertiesFile);
//		MainSettings.MENU_ITEM = this.propertyManager.getMsg("MENU_ITEM");
//		MainSettings.EXTENSION_NAME = this.propertyManager.getMsg("EXTENSION_NAME");
//		MainSettings.XML_VERSION_MESSAGE = this.propertyManager.getMsg("XML_VERSION_MESSAGE");
//		MainSettings.VERSION = Tools.getVersion();
	}

	/**
	 * Get instance of MainSettings class
	 */
	public static MainSettings getInstance() {
		if (MainSettings.classInstance == null) {
			MainSettings.classInstance = new MainSettings();
		}
		return MainSettings.classInstance;
	}

	/**
	 * Get instance of SpatialViewSettings class
	 */
//	public static SpatialViewSettings getSpatialViewSettingsInstance() {
//		if (MainSettings.spatialViewS == null) {
//			MainSettings.spatialViewS = new SpatialViewSettings();
//		}
//		return MainSettings.spatialViewS;
//	}

	public Preferences getPreferences() {
		if (MainSettings.mainPrefs == null) {
			oracle.ide.config.Preferences prefs = oracle.ide.config.Preferences.getPreferences();
			MainSettings.mainPrefs = Preferences.getInstance(prefs);
		}
		return MainSettings.mainPrefs;
	}

	/**
	 * Save all settings to settings file Note: Preferences are taken care of
	 * automatically
	 */
	public void save() {
//		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><GeoRaptor><Version>" + MainSettings.VERSION
//				+ "</Version>" + MainSettings.getSpatialViewSettingsInstance().toXML() + "</GeoRaptor>";
//		MainSettings.mainPrefs.setSpatialLayerXML(xmlString);
	}

	/**
	 * Load all settings from settings file
	 */
	public void load() {
		// Load non-Settings.xml preferences into memory for immediate use by system
		//
		loadPreferences();

		// The original formatted Spatial View stuff is XML and is stored as one
		// preference
		//
//		String svpXML = MainSettings.mainPrefs.getSpatialLayerXML();
//		if (Strings.isEmpty(svpXML)) {
//			svpXML = Constants.VAL_SPATIAL_LAYER_XML; // default
//		}
		try {
			Document doc = null;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			dbf.setValidating(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
//			doc = db.parse(new InputSource(new StringReader(svpXML)));
			XPath xpath = XPathFactory.newInstance().newXPath();
			// Extract Version
			//
			String version = (String) xpath.evaluate("/GeoRaptor/Version/text()", doc, XPathConstants.STRING);
			if (Strings.isEmpty(version)) {
				System.err.println("GeoRaptor Version not found: continuing.");
			} else {
//				if (!version.equalsIgnoreCase(MainSettings.VERSION)) {
//					System.err
//							.println(this.propertyManager.getMsg("XML_VERSION_MESSAGE", version, MainSettings.VERSION));
//				}
			}
			Node panelNode = (Node) xpath.evaluate("/GeoRaptor/SpatialPanel", doc, XPathConstants.NODE);
			// load data for Spatial View
//			MainSettings.getSpatialViewSettingsInstance().loadXML(panelNode.cloneNode(true));
		} catch (XPathExpressionException e) {
			LOGGER.error("MainSettings.XPathExpressionException = " + e.getMessage());
			e.getStackTrace();
		} catch (ParserConfigurationException e) {
			LOGGER.error("MainSettings.ParserConfigurationException = " + e.getMessage());
			e.getStackTrace();
		} 
//		catch (SAXException e) {
//			LOGGER.error("MainSettings.SAXException = " + e.getMessage());
//			e.getStackTrace();
//		} catch (IOException e) {
//			LOGGER.error("MainSettings.IOException = " + e.getMessage());
//			e.getStackTrace();
//		}
	}

	protected void loadPreferences() {
		// New instance of Panel as other classes uses its variables as master access
		// point
		//
		PreferencePanel pp = PreferencePanel.getInstance();

		// Load prefs into the panel controls' states.
		//
		pp.setAffixString(MainSettings.mainPrefs.getAffixString());
		pp.setColourDimInfo(MainSettings.mainPrefs.isColourDimInfo());
		pp.setColourSdoGeomElements(MainSettings.mainPrefs.isColourSdoGeomElements());
		pp.setCorrectColour(MainSettings.mainPrefs.getCorrectColour());
		pp.setDbaseNullWriteString(MainSettings.mainPrefs.getDbaseNullWriteString());
		pp.setDefaultSRID(MainSettings.mainPrefs.getSRID());
		pp.setDrawQueryGeometry(MainSettings.mainPrefs.isDrawQueryGeometry());
		pp.setFastPicklerConversion(MainSettings.mainPrefs.isFastPicklerConversion());
		pp.setFeatureColour(MainSettings.mainPrefs.getFeatureColour());
		pp.setFetchSize(MainSettings.mainPrefs.getFetchSize());
		pp.setGroupingSeparator(MainSettings.mainPrefs.getGroupingSeparator());
		pp.setHighlightColour(MainSettings.mainPrefs.getHighlightColour());
		pp.setPreviewImageHeight(MainSettings.mainPrefs.getPreviewImageHeight());
		pp.setPreviewImageWidth(MainSettings.mainPrefs.getPreviewImageWidth());
		pp.setImageRefreshMS(MainSettings.mainPrefs.getImageRefreshMS());
		pp.setLayerMBRSource(MainSettings.mainPrefs.getLayerMBRSource());
		pp.setLogSearchStats(MainSettings.mainPrefs.isLogSearchStats());
		pp.setMBRSaveSize(MainSettings.mainPrefs.getMBRSaveSize());
		pp.setMapBackground(MainSettings.mainPrefs.getMapBackground());
		pp.setMapScaleVisible(MainSettings.mainPrefs.isMapScaleVisible());
		pp.setMinResolution(MainSettings.mainPrefs.isMinResolution());
		pp.setMissingColour(MainSettings.mainPrefs.getMissingColour());
		pp.setNumberOfFeaturesVisible(MainSettings.mainPrefs.isNumberOfFeaturesVisible());
		pp.setNN(MainSettings.mainPrefs.isNN());
		pp.setNewLayerPosition(MainSettings.mainPrefs.getNewLayerPosition());
		pp.setNullDate(MainSettings.mainPrefs.getNullDate());
//		pp.setNullNumber(MainSettings.mainPrefs.getNullNumber());
		pp.setNullString(MainSettings.mainPrefs.getNullString());
		pp.setOrphanColour(MainSettings.mainPrefs.getOrphanColour());
		pp.setPanZoomPercentage(MainSettings.mainPrefs.getPanZoomPercentage());
		pp.setPrefixWithMDSYS(MainSettings.mainPrefs.getPrefixWithMDSYS());
		pp.setSchemaPrefix(MainSettings.mainPrefs.isSchemaPrefix());
		pp.setSdoGeometryBracketType(MainSettings.mainPrefs.getSdoGeometryBracketType());
		pp.setSdoGeometryCoordinateNumbering(MainSettings.mainPrefs.isSdoGeometryCoordinateNumbering());
		pp.setSdoGeometryFormat(MainSettings.mainPrefs.isSdoGeometryProcessingFormat());
		pp.setSdoGeometryVisualFormat(MainSettings.mainPrefs.getSdoGeometryVisualFormat());
		pp.setSearchPixels(MainSettings.mainPrefs.getSearchPixels());
		pp.setSelectionColour(MainSettings.mainPrefs.getSelectionColour());
		pp.setSuffixFlag(MainSettings.mainPrefs.isSuffixFlag());
		pp.setTOCPosition(MainSettings.mainPrefs.getTOCPosition());
		pp.setTableCountLimit(MainSettings.mainPrefs.getTableCountLimit());
		pp.setDBFColumnShorten(MainSettings.mainPrefs.isDBFShortenBegin10());
		pp.setSQLSchemaPrefix(MainSettings.mainPrefs.getSQLSchemaPrefix());
		pp.setQueryLimit(MainSettings.mainPrefs.getQueryLimit());
		pp.setRandomRendering(MainSettings.mainPrefs.isRandomRendering());
		pp.setDefGeomColName(MainSettings.mainPrefs.getDefGeomColName());
	}

}
