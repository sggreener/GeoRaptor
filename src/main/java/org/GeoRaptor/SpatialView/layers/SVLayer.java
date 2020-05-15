package org.GeoRaptor.SpatialView.layers;

import java.io.IOException;
import java.io.StringReader;

import java.sql.Connection;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import oracle.jdbc.OracleConnection;

import org.GeoRaptor.Constants;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.DatabaseConnection;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class describe one layer on map. It can be vector or raster type.
 */
public class SVLayer {

	public static final String CLASS_NAME = Constants.KEY_SVLAYER;
	private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVLayer");
	private static final String propertiesFile = "org.GeoRaptor.SpatialView.resource.SVLayer";
	protected PropertiesManager propertyManager = null; // Properties File Manager
	protected SpatialView spatialView = null; // reference to main class for common operations (show error message, etc)
	protected String connName = ""; // Name of the connection in Raptor project
	protected MetadataEntry mEntry = null; // MetadataEntry for this table/column object
	private final int defaultPrecision = 3;
	private int precision = defaultPrecision;
	private boolean isSTGeometry = false;
	protected Constants.GEOMETRY_TYPES geometryType = Constants.GEOMETRY_TYPES.UNKNOWN;
	protected Constants.SRID_TYPE SRIDType = Constants.SRID_TYPE.UNKNOWN; // property describing whether layer is
																			// geodetic or not
	protected Envelope mbr = null; // Layer MBR: Is different from mEntry's extent.
									// The latter forms the initial extent of the layer but this
									// could change as a user initiates MBR recalculation.

	public SVLayer(SpatialView _spatialView) {
		this.spatialView = _spatialView;
		this.mbr = new Envelope(this.getDefaultPrecision());
		this.setSRIDType(Constants.SRID_TYPE.UNKNOWN);
		this.propertyManager = new PropertiesManager(SVLayer.propertiesFile);
	}

	public SVLayer(SpatialView _spatialView, MetadataEntry _me) {
		this(_spatialView);
		if (_me != null) {
			this.setMetadataEntry(_me);
			this.mbr = new Envelope(_me.getMBR());
		}
	}

	/**
	 * Read layer properties from given XML Layer object and set class variables
	 * 
	 * @param _XML String XML <Layer>...</Layer>
	 */
	public SVLayer(SpatialView _spatialView, String _XML) {
		this(_spatialView);
		try {
			Document doc = null;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			dbf.setValidating(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(new InputSource(new StringReader(_XML)));
			XPath xpath = XPathFactory.newInstance().newXPath();
			this.fromXMLNode((Node) xpath.evaluate("/Layer", doc, XPathConstants.NODE));
		} catch (XPathExpressionException xe) {
			LOGGER.error("SVLayer(XML): XPathExpressionException " + xe.toString());
		} catch (ParserConfigurationException pe) {
			LOGGER.error("SVLayer(XML): ParserConfigurationException " + pe.toString());
		} catch (SAXException se) {
			LOGGER.error("SVLayer(XML): SAXException " + se.toString());
		} catch (IOException ioe) {
			LOGGER.error("SVLayer(XML): IOException " + ioe.toString());
		}
	}

	public SVLayer(SpatialView _spatialView, Node _node) {
		this(_spatialView);
		this.fromXMLNode(_node);
	}

	private void fromXMLNode(Node _node) {
		if (_node == null || _node.getNodeName().equals("Layer") == false) {
			System.out.println("Node is null or not Layer");
			return; // Should throw error
		}
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			Node mNode = (Node) xpath.evaluate("SVLayer/Metadata/MetadataEntry", _node, XPathConstants.NODE);
			this.mEntry = new MetadataEntry(new MetadataEntry(mNode.cloneNode(true)));
			this.setSRIDType((String) xpath.evaluate("SVLayer/SridType/text()", _node, XPathConstants.STRING));
			String precision = (String) xpath.evaluate("SVLayer/Precision/text()", _node, XPathConstants.STRING);
			precision = Strings.isEmpty(precision)
					? (String) xpath.evaluate("SVLayer/MaxFractionDigits/text()", _node, XPathConstants.STRING)
					: precision; // Change of name from this to the previous Precision
			this.setPrecision(precision);
			this.setGeometryType((String) xpath.evaluate("SVLayer/GeometryType/text()", _node, XPathConstants.STRING));
			// Last spatial extent of view
			this.mbr.minX = Double
					.valueOf((String) xpath.evaluate("SVLayer/MBR/MinX/text()", _node, XPathConstants.STRING));
			this.mbr.minY = Double
					.valueOf((String) xpath.evaluate("SVLayer/MBR/MinY/text()", _node, XPathConstants.STRING));
			this.mbr.maxX = Double
					.valueOf((String) xpath.evaluate("SVLayer/MBR/MaxX/text()", _node, XPathConstants.STRING));
			this.mbr.maxY = Double
					.valueOf((String) xpath.evaluate("SVLayer/MBR/MaxY/text()", _node, XPathConstants.STRING));
			// Set directly to avoid checking connection because we may be starting up
			this.connName = (String) xpath.evaluate("SVLayer/ConnName/text()", _node, XPathConstants.STRING);
			this.setSTGeometry((String) xpath.evaluate("SVLayer/isSTGeometry/text()", _node, XPathConstants.STRING));
		} catch (XPathExpressionException xe) {
			LOGGER.error("SVLayer.fromXMLNode: XPathExpressionException " + xe.toString());
		}
	}

	public String toXML() {
		return String.format(
				"<SVLayer><Metadata>%s</Metadata><GeometryType>%s</GeometryType><SridType>%s</SridType>%s<Precision>%s</Precision><ConnName>%s</ConnName><isSTGeometry>%s</isSTGeometry></SVLayer>",
				this.mEntry.toXML(), this.geometryType.toString(), this.SRIDType.toString(), this.mbr.toXML(),
				this.precision, this.connName, String.valueOf(this.isSTGeometry));
	}

	public boolean equals(Object obj) {
		// if the two objects are equal in reference, they are equal
		if (this == obj)
			return true;
		if (obj instanceof SVLayer) {
			SVLayer layer = (SVLayer) obj;
			return layer.getFullName().equals(this.getFullName());
		} else {
			return false;
		}
	}

	public void setView(SpatialView _spatialView) {
		this.spatialView = _spatialView;
	}

	public void setSTGeometry(String _value) {
		if (Strings.isEmpty(_value)) {
			return;
		}
		try {
			this.isSTGeometry = Boolean.valueOf(_value);
		} catch (Exception e) {
			return;
		}
	}

	public void setSTGeometry(boolean _value) {
		this.isSTGeometry = _value;
	}

	public boolean isSTGeometry() {
		return this.isSTGeometry;
	}

	public void setMetadataEntry(MetadataEntry _mEntry) {
		this.mEntry = new MetadataEntry(_mEntry);
		LOGGER.debug("SLayer.setMetadataEntry=" + this.mEntry.toString());
		try {
			if (this.getConnection() != null && !Strings.isEmpty(this.mEntry.getObjectName())) {
				this.isSTGeometry = MetadataTool.isSTGeometry((Connection) this.getConnection(),
						this.mEntry.getSchemaName(), this.mEntry.getObjectName(), this.mEntry.getColumnName());
			}
			LOGGER.debug("SLayer.isSTGeometry=" + this.isSTGeometry);
		} catch (Exception e) {
			LOGGER.error("SVLayer.setMetadataEntry(): " + e.getMessage());
		}
	}

	public MetadataEntry getMetadataEntry() {
		LOGGER.debug("SLayer.getMetadataEntry=" + this.mEntry + " copy=" + this.mEntry.copy());
		return this.mEntry.copy();
	}

	public int getDefaultPrecision() {
		return SRIDType.toString().startsWith("GEO") ? Constants.MAX_PRECISION : this.defaultPrecision;
	}

	public double getTolerance() {
		return this.mEntry.getMaxTolerance(1 / Math.pow(10.0, this.getDefaultPrecision()));
	}

	public void setPrecision(int numberFractions) {
		if (this.precision < 0) {
			this.precision = this.getPrecision(true);
		} else {
			this.precision = numberFractions;
		}
	}

	public void setPrecision(String _precision) {
		if (Strings.isEmpty(_precision)) {
			this.precision = this.getPrecision(true);
		} else {
			try {
				this.precision = Integer.valueOf(_precision);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

	public int getPrecision(boolean _compute) {
		if (this.precision < 0 || _compute) {
			double layerTolerance = this.getTolerance();
			if (layerTolerance == Double.MAX_VALUE) {
				this.precision = this.defaultPrecision;
			} else if (this.getSRIDType().toString().startsWith("GEO")) {
				/*
				 * Tolerances are normally expressed in meters Translation is roughly 500 - 3 50
				 * - 4 5 - 5 0.5 - 6 0.05 - 7 0.005 - 8
				 */
				if (layerTolerance == 500.0)
					return 3;
				else if (layerTolerance == 50.0)
					return 4;
				else if (layerTolerance == 5.0)
					return 5;
				else if (layerTolerance == 0.5)
					return 6;
				else if (layerTolerance == 0.05)
					return 7;
				else if (layerTolerance == 0.005)
					return 8;
				else if (layerTolerance == 0.0005)
					return 9;
				else if (layerTolerance == 0.00005)
					return 10;
				else
					return this.defaultPrecision;
			} else {
				this.precision = (int) Math.rint(Math.log10(1.0 / layerTolerance));
			}
		}
		return this.precision;
	}

	public String getSchemaName() {
		return this.mEntry.getSchemaName();
	}

	public void setSchemaName(String _schemaName) {
		this.mEntry.setSchemaName(_schemaName);
	}

	public String getObjectName() {
		return this.mEntry.getObjectName();
	}

	public void setObjectName(String _objectName) {
		this.mEntry.setObjectName(_objectName);
	}

	public String getFullObjectName() {
		return this.mEntry.getFullObjectName();
	}

	public String getFullName() {
		return this.mEntry.getFullName();
	}

	public void setGeoColumn(String _geoColumn) {
		this.mEntry.setColumnName(_geoColumn);
	}

	public String getGeoColumn() {
		return this.mEntry.getColumnName();
	}

	public void setGeometryType(String _geometryType) {
		try {
			this.geometryType = Strings.isEmpty(_geometryType) ? Constants.GEOMETRY_TYPES.UNKNOWN
					: Constants.GEOMETRY_TYPES.valueOf(_geometryType.toUpperCase());
		} catch (Exception e) {
			this.geometryType = Constants.GEOMETRY_TYPES.UNKNOWN;
		}
	}

	public void setGeometryType(Constants.GEOMETRY_TYPES _getGeometryType) {
		this.geometryType = _getGeometryType;
	}

	public Constants.GEOMETRY_TYPES getGeometryType() {
		return this.geometryType;
	}

	// +++++++++++++++++++++++ Connection Methods

	public void setConnectionName(String _connName) {
		// No schema checks, just set the name
		//
		this.connName = _connName;
		// addConnection checks if connection exists
		DatabaseConnections.getInstance().addConnection(this.connName);
	}

	public void setConnection(String _connName) throws IllegalStateException {
		LOGGER.debug("SVLayer setConnection(" + _connName + ")");
		if (Strings.isEmpty(_connName)) {
			return;
		}
		if (Strings.isEmpty(this.connName)) {
			this.setConnectionName(_connName);
			return;
		}
		if (this.connName.equalsIgnoreCase(_connName)) {
			// Do nothing as both are equal
			return;
		}
		// They are different so we need to check the schema in the metadata
		DatabaseConnection dbConn = DatabaseConnections.getInstance().findConnectionByName(_connName);
		if (dbConn == null) {
			LOGGER.error("Could not find connection with name " + _connName);
			return;
		}
		String newSchema = dbConn.getCurrentSchema();
		if (Strings.isEmpty(newSchema)) {
			newSchema = dbConn.getUserName();
			if (Strings.isEmpty(newSchema)) {
				LOGGER.error("Could not find schema/user associated with connection " + _connName);
				return;
			}
		}

		if (!newSchema.equalsIgnoreCase(this.getSchemaName())) {
			this.setSchemaName(newSchema);
			// Check that database object exists in new schema/connection
			if (!Strings.isEmpty(this.getObjectName())) {
				String objCol = null;
				try {
					objCol = MetadataTool.getGeometryColumn(dbConn.getConnection(), this.getSchemaName(),
							this.getObjectName(), this.getGeoColumn());
				} catch (SQLException e) {
					// Probably connection not open.
					String reason = "Failed to query database to check "
							+ Strings.objectString(this.getSchemaName(), this.getObjectName(), this.getGeoColumn())
							+ " existance: " + e.toString();
					throw new IllegalStateException(reason);
				}
				if (Strings.isEmpty(objCol)) {
					String reason = "New connection schema " + newSchema + " does not have object called "
							+ this.getObjectName() + " with sdo_geometry column called " + this.getGeoColumn();
					LOGGER.error(reason);
					throw new IllegalStateException(reason);
				}
			}
		}
		this.connName = _connName;
		DatabaseConnections.getInstance().addConnection(this.connName);
	}

	public boolean isConnectionOpen() {
		if (!Strings.isEmpty(this.connName)) {
			return DatabaseConnections.getInstance().isConnectionOpen(this.connName);
		}
		return false;
	}

	public boolean openConnection() {
		if (!Strings.isEmpty(this.connName)) {
			return DatabaseConnections.getInstance().openConnection(this.connName);
		}
		return false;
	}

	public OracleConnection getConnection() throws IllegalStateException {
		if (Strings.isEmpty(this.connName)) {
			return DatabaseConnections.getInstance().getActiveConnection();
		} else {
			return DatabaseConnections.getInstance().getConnection(this.connName);
		}
	}

	public String getConnectionName() {
		return this.connName;
	}

	public String getConnectionDisplayName() {
		return DatabaseConnections.getInstance().getConnectionDisplayName(this.connName);
	}

	// +++++++++++++++++++++++ End Connection Methods

	public int getSRIDAsInteger() {
		return this.getSRID().equals(Constants.NULL) ? Constants.SRID_NULL : Integer.valueOf(this.getSRID()).intValue();
	}

	public String getSRID() {
		return (this.mEntry != null ? this.mEntry.getSRID() : Constants.NULL);
	}

	public void setSRID(String _SRID) {
		this.mEntry.setSRID(Strings.isEmpty(_SRID) ? Constants.NULL : _SRID);
		this.SRIDType = Tools.discoverSRIDType(this.getConnection(), this.getSRID());
	}

	public boolean isGeodetic() {
		return getSRIDType().toString().toUpperCase().startsWith("GEO");
	}

	public Constants.SRID_TYPE getSRIDType() {
		return this.SRIDType == null ? Constants.SRID_TYPE.UNKNOWN : this.SRIDType;
	}

	public void setSRIDType() {
		if (this.getSRID().equalsIgnoreCase(Constants.NULL)) {
			this.SRIDType = Constants.SRID_TYPE.PROJECTED;
			return;
		}
		if (this.SRIDType == Constants.SRID_TYPE.UNKNOWN) {
			this.SRIDType = Tools.discoverSRIDType(this.getConnection(), this.getSRID());
		}
	}

	public void setSRIDType(String _SRIDType) {
		try {
			this.SRIDType = Strings.isEmpty(_SRIDType) ? Constants.SRID_TYPE.UNKNOWN
					: Constants.SRID_TYPE.valueOf(_SRIDType);
		} catch (Exception e) {
			this.SRIDType = Constants.SRID_TYPE.UNKNOWN;
			this.setSRIDType();
		}
	}

	public void setSRIDType(Constants.SRID_TYPE _SRIDType) {
		this.SRIDType = _SRIDType;
	}

	public void setMBR(double _mbrMinX, double _mbrMinY, double _mbrMaxX, double _mbrMaxY) {
		this.mbr.setMBR(_mbrMinX, _mbrMinY, _mbrMaxX, _mbrMaxY);
		LOGGER.debug("SLayer.setMBR(x,y,x,y)=" + this.mbr.toString());
	}

	public void setMBR(Envelope _mbr) {
		if (_mbr != null && _mbr.isSet()) {
			this.mbr.setMBR(_mbr);
			LOGGER.debug("SLayer.setMBR(RectangleDouble)=" + this.mbr.toString());
		}
	}

	public Envelope getMBR() {
		LOGGER.debug("SLayer.getMBR()=" + (new Envelope(this.mbr)).toString());
		return new Envelope(this.mbr);
	}

	public void setSpatialView(SpatialView _spatialView) {
		this.spatialView = _spatialView;
	}

	public SpatialView getSpatialView() {
		return this.spatialView;
	}

	/**
	 * Create layer copy. Method is empty because extend class must create new
	 * instance of his own type
	 */
	public SVLayer createCopy() throws Exception {
		return null;
	}

} // SVLayer