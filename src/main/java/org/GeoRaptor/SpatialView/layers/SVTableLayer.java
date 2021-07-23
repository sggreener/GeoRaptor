package org.GeoRaptor.SpatialView.layers;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.GeoRaptor.Constants;
import org.GeoRaptor.Constants.GEOMETRY_TYPES;
import org.GeoRaptor.Constants.SDO_OPERATORS;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.io.ExtensionFileFilter;
import org.GeoRaptor.sql.Queries;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.JGeom;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.RenderTool;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.geotools.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSetMetaData;
import oracle.spatial.geometry.JGeometry;


/**
 * Data types description in Oracle documentation (Oracle Spatial User Guide and Reference 10g Release 2)
 * 36 - 1.4 Geometry Types
 * 51 - 2.1 Simple Example: Inserting, Indexing, and Querying Spatial Data
 * 59 - Table 2.2 Values and Semantics in SDO_ELEM_INFO
 * 64 - 2.5 Geometry Examples
 * 75 - 2.5.8 Several Geometry Types
 */
public class SVTableLayer 
     extends SVLayer 
  implements iLayer 
{

    public final String CLASS_NAME = Constants.KEY_SVTableLayer;

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVTableLayer");

    protected Preferences           preferences = null;
    protected Styling                   styling = new Styling();
    protected SVSpatialLayerDraw      drawTools = new SVSpatialLayerDraw(this); // Class used to draw this layer 
    protected String                  layerName = "";         // Unique layer name. This is the universal unique name for this layer.
    protected String                visibleName = "";         // This text is write into JTree where we show layers
    protected String                  keyColumn = null;       // Single unique column name 
    protected String            layerCopySuffix = " (COPY)";  // Should be Tools>Preferences>GeoRaptor
    protected String                       desc = "";         // Layer description. It is diplay when user left mouse cursor on layer name.
    protected boolean                      draw = true;       // true - draw layer, false - not draw layer
    protected boolean              calculateMBR = false;      // When true, recalculates Layer MBR at next draw from actual data
    protected boolean               indexExists = true;       // Does spatial index exist for this layer
    protected String                   layerSQL = "";         // SQL for geometry object and other columns (for text, line color, shade color, etc)
    protected String            searchPredicate = "";         // WHERE clause search predicate
    protected boolean                   project = false;      // Project to view's projection?
    protected String      projectedGeometryName = "TGEO12_34_098";
    protected boolean             minResolution = false;      // Do we use min_resolution sdo_filter parameter when querying?
    protected int               resultFetchSize = 100;        // Fetch size for ResultSet
    protected long             numberOfFeatures = 0;          // Feature Count for display and stats
    protected Constants.OBJECT_TYPES objectType = Constants.OBJECT_TYPES.TABLE;
    private final String  sdoFilterMinResClause = "min_resolution=%f,querytype=WINDOW";
    private final String        sdoFilterClause = "querytype=WINDOW";

    /*****************************************************************/

    /** Constructors...
     **/
    public SVTableLayer(SpatialView _sView) {
        super(_sView);
        this.preferences = MainSettings.getInstance().getPreferences();
        this.setResultFetchSize(preferences.getFetchSize());
        this.setPrecision(-1); // force calculation from mbr
    } // SVTableLayer

    public SVTableLayer(SpatialView     _sView, 
                          String        _layerName,
                          String        _visibleName,
                          String        _desc, 
                          MetadataEntry _me, 
                          boolean       _draw) 
    {
        super(_sView, _me);
        this.layerName = Strings.isEmpty(_layerName)? UUID.randomUUID().toString() : _layerName;
        this.visibleName = _visibleName;
        this.desc = _desc;
        this.draw = _draw;
        this.styling = new Styling();
        this.preferences = MainSettings.getInstance().getPreferences();
        this.setResultFetchSize(preferences.getFetchSize());
        this.setPrecision(-1); // force calculation from mbr
    } // SVTableLayer

    /**
     * Read layer properties from given XML Layer object and set class variables
     * @param _XML String XML <Layer>...</Layer>
     */
    public SVTableLayer(SpatialView _sView, String _XML) {
        super(_sView, _XML);
        try {
            Document doc = null;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(_XML)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            this.fromXML((Node)xpath.evaluate("/Layer", doc,
                                                  XPathConstants.NODE));
        } catch (XPathExpressionException xe) {
            LOGGER.error("SVTableLayer(XML): XPathExpressionException " + xe.toString());
        } catch (ParserConfigurationException pe) {
            LOGGER.error("SVTableLayer(XML): ParserConfigurationException " + pe.toString());
        } catch (SAXException se) {
            LOGGER.error("SVTableLayer(XML): SAXException " + se.toString());
        } catch (IOException ioe) {
            LOGGER.error("SVTableLayer(XML): IOException " + ioe.toString());
        }
        this.preferences = MainSettings.getInstance().getPreferences();
        this.setResultFetchSize(preferences.getFetchSize());
        this.setPrecision(-1); // force calculation from mbr
    }

    public SVTableLayer(SpatialView _sView, Node _node) {
        super(_sView, _node);
        this.fromXML(_node);
        this.preferences = MainSettings.getInstance().getPreferences();
        this.setResultFetchSize(preferences.getFetchSize());
        this.setPrecision(-1); // force calculation from mbr
    }

    /**
     * Copy Constructor
     * 
     */
    public SVTableLayer(SVTableLayer _sLayer) 
    {
        super(_sLayer.getSpatialView(), 
              _sLayer.getMetadataEntry());
        
        super.setConnectionName(_sLayer.connName);
        this.setLayerName(_sLayer.getSpatialView().getSVPanel().getViewLayerTree().checkName(_sLayer.getLayerName()));
        this.setVisibleName(_sLayer.getVisibleName()+layerCopySuffix);
        this.setDesc(_sLayer.getDesc());
        this.setDraw(_sLayer.isDraw());
        this.setSQL(_sLayer.getSQL());
        this.setKeyColumn(this.getKeyColumn());
        this.setSRIDType(_sLayer.getSRIDType());
        this.setGeometryType(_sLayer.getGeometryType());
        this.setSTGeometry(_sLayer.isSTGeometry());
        this.setIndex(_sLayer.hasIndex());
        this.setMBR(_sLayer.getMBR());
        this.setMinResolution(_sLayer.getMinResolution());
        this.setFetchSize(_sLayer.getFetchSize());
        this.setPrecision(_sLayer.getPrecision(false));
        this.setProject(_sLayer.getProject(),false);
        this.setStyling(_sLayer.getStyling());
        this.preferences = MainSettings.getInstance().getPreferences();
        this.setResultFetchSize(preferences.getFetchSize());
        this.setPrecision(-1); // force calculation from mbr
    }

    /**
     * Create copy of current class.
     */
    public SVTableLayer createCopy() 
    throws Exception 
    {
        // _renderOnly is ignored for SVTableLayers
        //
        return new SVTableLayer(this);
    }

    private void fromXML(Node _node) {
        if (_node == null || _node.getNodeName().equals("Layer") == false) {
            return; // Should throw error
        }
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            this.setLayerName(           (String)xpath.evaluate("SVTableLayer/Name/text()",_node,XPathConstants.STRING));
            this.setVisibleName(         (String)xpath.evaluate("SVTableLayer/VisibleName/text()",_node,XPathConstants.STRING));
            this.setDesc(                (String)xpath.evaluate("SVTableLayer/Description/text()",_node, XPathConstants.STRING));
            this.setDraw(Boolean.valueOf((String)xpath.evaluate("SVTableLayer/Draw/text()",_node,XPathConstants.STRING)));
            this.setResultFetchSize(
                 Integer.valueOf(
                         Strings.isEmpty((String)xpath.evaluate("SVTableLayer/ResultFetchSize/text()",_node,XPathConstants.STRING))
                         ? "0" :         (String)xpath.evaluate("SVTableLayer/ResultFetchSize/text()",_node,XPathConstants.STRING)));
            this.setMinResolution(
                         Boolean.valueOf((String)xpath.evaluate("SVTableLayer/MinResolution/text()",_node,XPathConstants.STRING)));
            this.setIndex(               (String)xpath.evaluate("SVTableLayer/hasIndex/text()",_node,XPathConstants.STRING));
            this.setProject(
                         Boolean.valueOf((String)xpath.evaluate("SVTableLayer/isProject/text()",_node,XPathConstants.STRING)),false);
            /*
            this.setProject(!(this.getSRID().equals(Constants.NULL) ||
                              this.spatialView.getSRID().equals(Constants.NULL) ||
                              this.getSRID().equals(this.spatialView.getSRID())),
                             false);
            */
            this.setSQL((String)xpath.evaluate("SVTableLayer/SQL/text()",_node,XPathConstants.STRING));

            Node stylingNode = (Node)xpath.evaluate("SVTableLayer/Styling",_node,XPathConstants.NODE);
            this.styling = new Styling(stylingNode);

        } catch (XPathExpressionException xe) {
            LOGGER.error("fromXMLNode(): XPathExpressionException " + xe.toString());
        }
    }

    public String toXML() {
        String xml = super.toXML();
        try {
            xml += "<SVTableLayer>";
            xml += String.format("<Name>%s</Name><VisibleName>%s</VisibleName><Description>%s</Description><Draw>%s</Draw><SQL>%s</SQL>",
          		               this.getLayerName(),
          		               this.getVisibleName(),
          		               this.getDesc(),
                               String.valueOf(this.draw), 
                               this.getSQL()
                   );
            xml += String.format("<ResultFetchSize>%s</ResultFetchSize><MinResolution>%s</MinResolution><hasIndex>%s</hasIndex><isProject>%s</isProject>",
                                 String.format("%d", this.resultFetchSize),
                                 String.valueOf(this.minResolution),
                                 String.valueOf(this.hasIndex()),
                                 String.valueOf(this.getProject()));            
            xml += this.styling.toXML(Preferences.getInstance().getMapBackground());
            xml += "</SVTableLayer>";
        } catch (Exception e) {
            LOGGER.error("Error saving " + this.getVisibleName() + " (" +
                               this.getLayerName() + ") : " +
                               e.getCause().getLocalizedMessage());
        }
        return xml;
    }

    public void savePropertiesToDisk() {
        String saveXML = this.toXML();
        saveXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                  "<Layers><Layer>" + saveXML + "</Layer></Layers>";
        // Now ask where to save and file name....
        try {
            FileFilter xmlFilter = new ExtensionFileFilter("XML", new String[] { "XML" });
            File f = new File(this.getLayerName().replace(".","_")+".xml");
            JFileChooser fc = new JFileChooser()
            {
				private static final long serialVersionUID = 8674762352541526796L;

				@Override
	            public void approveSelection(){
	                File f = getSelectedFile();
	                if(f.exists() && getDialogType() == SAVE_DIALOG){
	                    int result = JOptionPane.showConfirmDialog(this,
	                                                               propertyManager.getMsg("CONFIRM_FILE_EXISTS_PROMPT"),
	                                                               propertyManager.getMsg("CONFIRM_FILE_EXISTS_TITLE"),
	                                                               JOptionPane.YES_NO_CANCEL_OPTION);
	                    switch(result){
	                        case JOptionPane.YES_OPTION   : super.approveSelection(); return;
	                        case JOptionPane.NO_OPTION    :
	                        case JOptionPane.CLOSED_OPTION: return;
	                        case JOptionPane.CANCEL_OPTION: cancelSelection(); return;
	                    }
	                }
	                super.approveSelection();
	            }
            };                                                     
            fc.setDialogTitle(this.propertyManager.getMsg("FILE_SAVE_DIALOG_TITLE"));
            fc.setFileFilter(xmlFilter);
            fc.setSelectedFile(f);
            int returnVal = fc.showSaveDialog(null);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                LOGGER.fine(this.propertyManager.getMsg("FILE_SAVE_CANCELLED"));
            } else {
                File file = fc.getSelectedFile();
                //This is where a real application would save the file.
                LOGGER.fine(this.propertyManager.getMsg("FILE_SAVING",file.getName()));
                FileWriter fw;
                try {
                    fw = new FileWriter(file.getAbsoluteFile());
                    BufferedWriter out = new BufferedWriter(fw);
                    out.write(saveXML);
                    //Close the output stream
                    out.close();
                    LOGGER.fine(this.propertyManager.getMsg("FILE_SAVE_THEME",this.getLayerName()));
                } catch (IOException e) {
                    LOGGER.error(this.propertyManager.getMsg("FILE_SAVE_ERROR",file.getName(),e.getMessage()));
                }
            }
        } catch (Exception e){//Catch exception if any
            LOGGER.error("Error: " + e.getMessage());
        }
    }

    public String getClassName() {
        return this.CLASS_NAME;
    }
    
    public void setStyling(Styling _style) {
        this.styling = new Styling(_style);
    }

    public Styling getStyling() {
        return this.styling;
    }

    public String getStylingAsString() {
        return this.styling.toXML(this.getSpatialView().getMapPanel().getMapBackground());
    }
    
    public PropertiesManager getPropertyManager() {
        // propertyManager is always set by initialise() method
        return this.propertyManager;
    }

    public Preferences getPreferences() {
        if (this.preferences==null) {
            this.preferences = MainSettings.getInstance().getPreferences();
        }
        return this.preferences;
    }

    public boolean equals(Object obj) {
        //if the two objects are equal in reference, they are equal
        if (this == obj) {
            return true;
        }
        if (obj instanceof SVTableLayer) {
            SVTableLayer sLayer = (SVTableLayer)obj;
            return sLayer.getLayerName().equals(this.getLayerName()) &&
                sLayer.getVisibleName().equals(this.getVisibleName());
        } else {
            return false;
        }
    }

    public SpatialView getSpatialView() {
        return super.getSpatialView();
    }

    public String getLayerNameAndConnectionName() {
        return this.layerName + "." + super.getConnectionDisplayName();
    }
    
    public String getLayerName() {
        return this.layerName;
    }

    public void setLayerName(String _layerName) {
        this.layerName = _layerName;
    }

    // This version checks if connection has changed.
    // Normally database object doesn't change schema but sometimes it can if layer imported from another database.
    //
    public void setConnection(String _connName) {
        String oldSchema = super.getSchemaName();
        super.setConnection(_connName);
        // Check if connection/schema has changed
        if ( ! oldSchema.equalsIgnoreCase(super.getSchemaName()) ) {
            // Should change SVTableLayer SQL if has schema name?
            String sql = this.getSQL();
            if (sql.contains(oldSchema+".")) {
                this.setSQL(sql.replace(oldSchema+".", super.getSchemaName()+"."));
            }
        }
    }
    
    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String _desc) {
        this.desc = _desc;
    }

    public String getVisibleName() {
        return this.visibleName;
    }

    public void setVisibleName(String _visibleName) {
        this.visibleName = _visibleName;
    }

    public boolean isDraw() {
        return this.draw;
    }

    public void setDraw(boolean _draw) {
        this.draw = _draw;
    }

    public void setObjectType(String _objectType) {
        if (Strings.isEmpty(_objectType) ) {
            try {
                this.objectType = Constants.OBJECT_TYPES.valueOf(Queries.getObjectType(super.getConnection(),
                                                                                            mEntry.getSchemaName(),
                                                                                            mEntry.getObjectName()));
            } catch (SQLException e) {
                this.objectType = Constants.OBJECT_TYPES.TABLE;
            }
        } else {
            try {
                this.objectType = Constants.OBJECT_TYPES.valueOf(_objectType);
            } catch (Exception e) {
                this.objectType = Constants.OBJECT_TYPES.TABLE;
            }
        }
    }
    
    public boolean isView() {
        return this.objectType.equals(Constants.OBJECT_TYPES.VIEW);
    }
    
    public void setIndex() {
        Connection conn = null; 
        try {
            conn = super.getConnection();
        } catch (IllegalStateException ise) {
            LOGGER.warn("No connection available for (" + 
                        this.getLayerNameAndConnectionName() + 
                        ") to check spatial index (" + ise.toString() + ")");
            this.indexExists = false;
        }
        try {
            LOGGER.debug("SVTableLayer(" + 
                         this.getLayerNameAndConnectionName() + 
                         ").setIndex.isSpatiallyIndexed()");
            this.indexExists = Queries.isSpatiallyIndexed(conn,
                                                          super.getSchemaName(),
                                                          super.getObjectName(),
                                                          super.getGeoColumn(),
                                                          super.getSRID());                
            LOGGER.debug("SVTableLayer(" + this.getLayerNameAndConnectionName() + ").setIndex() = " + indexExists);
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("SVTableLayer(" + this.getLayerNameAndConnectionName() + ").isSpatiallyIndexed Exception: " + iae.toString());
            this.indexExists = false;
        }
    }

    public void setIndex(String _hasIndex) {
        try {
            if (Strings.isEmpty(_hasIndex) )
                this.setIndex();
            else 
                this.indexExists = Boolean.valueOf(_hasIndex); 
        } catch (Exception e) {
            this.indexExists = true;
        }
    }
  
    public void setIndex(boolean _indexExists) {
        this.indexExists = _indexExists;
    }
    
    public boolean hasIndex() {
        return this.indexExists;
    }
    
    public boolean setLayerMBR(Envelope _defaultMBR,
                               int      _targetSRID)
    {
        Connection conn = super.getConnection();
         
        // If source is INDEX get MBR from RTree index
        //
        Envelope lMBR = new Envelope(this.getDefaultPrecision());
        
        //System.out.println("SVTableLayer.setLayerMBR() hasIndex()=" + this.hasIndex() + 
        //           "\nSRIDTYPE=" + this.getSRIDType().toString()+
        //           "\nLayerMbrSource=" + this.getPreferences().getLayerMBRSource().toString());
        
        // If one method fails, a warning is written. When this happens we need to report success of later method
        boolean multiTry = false;
        
        if (this.hasIndex() &&
            this.getSRIDType().toString().startsWith("GEO") == false &&
            this.getPreferences().getLayerMBRSource().equalsIgnoreCase(Constants.CONST_LAYER_MBR_INDEX)) 
        {
            try {
                lMBR.setMBR(Queries.getExtentFromRTree(conn,
                                                       this.getSchemaName(),
                                                       this.getObjectName(),
                                                       this.getGeoColumn(),
                                                       this.getSRID(),
                                                       String.valueOf(_targetSRID))); 
                if ( lMBR.isSet() ) {
                    super.setMBR(lMBR);
                    return true;
                } 
            } catch (SQLException e) {
                LOGGER.warn("Could not extract extent from RTree index, so will extract from metadata.");
                multiTry = true;
            }
        }
        
        // Try and get Metadata from database.
        //
        boolean hasMetadata = false;
        try {
            hasMetadata = Queries.hasGeomMetadataEntry(conn,
                                                       this.getSchemaName(),
                                                       this.getObjectName(),
                                                       this.getGeoColumn());
            // Try and extract from existing Metadata
            if ( hasMetadata ) {
                try 
                {
                    lMBR = Queries.getExtentFromDimInfo(conn, 
                                                             this.getSchemaName(),
                                                             this.getObjectName(), 
                                                             this.getGeoColumn(),
                                                             this.getSpatialView().getSRID());
                    if ( lMBR.isSet() ) {
                        super.setMBR(lMBR);
                        if (multiTry) {
                            LOGGER.warn("Extracting extent from metadata successful.");
                        }
                        return true;
                    } 
                } catch (Exception e) {
                    LOGGER.warn("Could not extract extent from metadata, so will extract from a sample of records.");
                    multiTry = true;
                }
            }  
        } catch (SQLException e) {
            LOGGER.warn("Could not extract extent from User_Sdo_Geom_Metadata, so will extract MBR from a sample of records.");
            multiTry = true;
        }
        
        // Try sampling geometries to get MBR
        //
        try 
        {
            lMBR = Queries.getExtentFromSample(conn, 
                                               this.getSchemaName(),
                                               this.getObjectName(), 
                                               this.getGeoColumn(),
                                               Constants.VAL_MBR_SAMPLE_LIMIT);
            if ( lMBR.isSet() ) {
                super.setMBR(lMBR);
                if (multiTry) {
                    LOGGER.warn("Extracting extent from sample successful.");
                }
                return true;
            } 
        } catch (SQLException e) {
            LOGGER.warn("Failed to get extent through sampling (" + e.getMessage() + ").");
        }
        
        if ( _defaultMBR!=null && 
             _defaultMBR.isSet() ) {
            super.setMBR(_defaultMBR);
            LOGGER.warn("Default extent applied to layer.");
            return true;
        }
        return false;
    }

    /**
     * Create SQL for given Geometry column and table name
     */
    public String getInitSQL(String _geoColumn) 
    {
        // If SQL Developer still starting up and a layer is being loaded from XML
        // the connections won't work
        //
        String columns = "";
        try {
          columns = Queries.getColumns(super.getConnection(),
                                       super.getSchemaName(),
                                       super.getObjectName(),
                                       true /* _supportedDataTypes */ );
        } catch (Exception e) {
            LOGGER.error("SVTableLayer.getInitSQL GetColumns Exception: " + e.getMessage());
            return null;
        }
        
        // create database table/view/mview name
        //
        String databaseObjectName = (this.preferences.getSQLSchemaPrefix()
                                    ? this.getFullObjectName()
                                    : this.getObjectName()) ;
        
        // Get search predicate
        //
        String searchPredicate = "";
        if ( this.hasIndex() ) {
        	if ( this.isSTGeometry() )
              searchPredicate = "SDO_FILTER(t." + _geoColumn + ",?) = 'TRUE'";
        	else
              searchPredicate = "SDO_FILTER(t." + _geoColumn + ",?,?) = 'TRUE'";
        } else {
        	searchPredicate = "t." + _geoColumn + " IS NOT NULL\n" +
                         " AND t." + _geoColumn + ".SDO_GTYPE IS NOT NULL\n" +
                         " AND MDSYS.SDO_GEOM.VALIDATE_GEOMETRY(t." + _geoColumn + "," + this.getTolerance() + ") = 'TRUE'\n" +
                         " AND MDSYS.SDO_GEOM.RELATE(t." + _geoColumn + ",'ANYINTERACT',?,"+this.getTolerance()+") = 'TRUE'";
        }

        // Now create initial SQL
        String initialSQL;
        initialSQL = "SELECT ROWID," + (Strings.isEmpty(columns) ? "" : columns+"," ) + 
                             "t." + _geoColumn + " as " + _geoColumn + " \n" +
                     "  FROM " + databaseObjectName + " t \n" +
                     " WHERE " + searchPredicate;
        
        LOGGER.logSQL(initialSQL);

        return this.generateSQL();
        //return initialSQL;
    }

    public void setInitSQL() {
        this.layerSQL = this.getInitSQL(super.getGeoColumn());
    }

    public String getSDOFilterParameters() 
    {
        String sdoFilterClause = this.sdoFilterClause;
        if (this.getMinResolution()) {
            Point.Double pixelSize = null;
            try {
                pixelSize = this.getSpatialView().getMapPanel().getPixelSize();
            } catch (NoninvertibleTransformException e) {
                pixelSize = new Point.Double(0.0,0.0);
            }
	        if ( this.getMinResolution() && pixelSize != null  ) {
	            double maxPixelSize = Math.max(pixelSize.getX(), pixelSize.getY());
	            if (maxPixelSize != 0.0) {
	                sdoFilterClause = String.format(this.sdoFilterMinResClause,maxPixelSize);
	            }
	        }
        }
        return sdoFilterClause;
    }
    
    private Struct getSearchFilterGeometry(Envelope _mbr,
                                           boolean  _project,
                                           int      _sourceSRID,
                                           int      _destinationSRID) 
    throws SQLException 
    {
    	Struct sGeom = null;
        try {
            Connection conn = super.getConnection();
            Envelope mbr = new Envelope(_mbr);
            mbr.setSRID(_sourceSRID);
            sGeom = Queries.projectEnvelope(conn,_mbr,_destinationSRID);
        } catch (Exception e) {
            LOGGER.error(super.propertyManager.getMsg("ERROR_CREATE_MBR_RECTANGLE",e.getMessage()));
            sGeom = null;
        }
        return sGeom;
    }
    
    private static boolean columnExists(String _sql, String _column) 
    {
        if (Strings.isEmpty(_sql) || Strings.isEmpty(_column) ) return false;
        // Quoted attributes can be upper/lower/mixed case and are unique enough
        // that a pure contains search shouldn't not find
        //
        if ( _column.startsWith("\"") && _column.endsWith("\"") ) {
            return _sql.contains(_column);
        }
        String token = "",
               column = _column.replaceAll("\"", "").toUpperCase();
        StringTokenizer st = new StringTokenizer(_sql,". ,()",true);
        int expression = 0; 
        while (st.hasMoreTokens() ) {
            token = st.nextToken().replaceAll("\"", "").toUpperCase();
            if ( token.equals("FROM") ) return false;
            if ( token.matches("[., ]") ) { continue; }        
            if ( token.equals("(") ) { expression++; continue; }
            if ( token.equals(")") ) { expression--; continue; }
            if ( expression==0 && token.equals(column))  { return true; }
            if ( expression==0 && token.matches("\\*") ) { return true; }
        }
        return false;
    }

    // ***********************************************************
    // ********************** SQL ********************************

    private boolean mustProject () {
    	// If the view and the layer have non-null SRIDs and they are not the same projection occurs.
    	// If one or both SRIDs are null, we do not project.
		return super.getSpatialView().getSRIDAsInteger() != Constants.NULL_SRID
                              && this.getSRIDAsInteger() != Constants.NULL_SRID
	        && super.getSpatialView().getSRIDAsInteger() != this.getSRIDAsInteger(); 
    }
    
	public String generateSQL() 
	{
		String sql = "";
		boolean project = this.mustProject(); 

        // The search geom (MBR) is always the SRID of the spatial view 
        // So, may need to be transformed to SRID of base table
        //
        // Display Geometry Column may also need projecting to SRID of displaying view
        //
        String geoColumn = "";
		String searchGeom = "";
		if ( ! project ) {
			searchGeom = "?";
            geoColumn = "t." + super.getGeoColumn();
		} else { 
           searchGeom = String.format("MDSYS.SDO_CS.TRANSFORM(?,%d)", 
                                      super.getSRIDAsInteger());
           geoColumn  = String.format("MDSYS.SDO_CS.TRANSFORM(t.%s,%d)", 
					                  super.getGeoColumn(),
					                  super.spatialView.getSRIDAsInteger());
		} 


		// Retrieve columns to select
		//
		String columns = "";
		try {
			// Get NON-SDO_GEMETRY columns of table
			columns = Queries.getColumns(super.getConnection(), 
					                     super.getSchemaName(), 
					                     super.getObjectName(),
					                     true /* _supportedDataTypes */ );
		} catch (Exception e) {
			LOGGER.error("SVTableLayer GetColumns " + e.getMessage());
			return null;
		}

		// Force setting and getting of Primary Key/Unique column
		this.setKeyColumn(null);
		keyColumn = this.getKeyColumn();

		this.searchPredicate = "";
		if (this.hasIndex()) {
			
            if ( this.isSTGeometry() )
			   this.searchPredicate = "SDO_FILTER(t." + super.getGeoColumn() + ",a.GEOM) = 'TRUE'";
			else
				this.searchPredicate = "SDO_FILTER(t." + super.getGeoColumn() + ",a.GEOM,?) = 'TRUE'";
            
		} else {
			
			this.searchPredicate = "t." + super.getGeoColumn() + " IS NOT NULL\n" + 
                    " AND t." + super.getGeoColumn() + ".SDO_GTYPE IS NOT NULL\n" + 
                    " AND MDSYS.SDO_GEOM.VALIDATE_GEOMETRY(t." + super.getGeoColumn() + ",?) = 'TRUE'\n" + 
                    " AND MDSYS.SDO_GEOM.RELATE(t." + super.getGeoColumn() + ",'ANYINTERACT',a.geom,?) = 'TRUE'";
		}

		sql = "SELECT " + (Strings.isEmpty(columns) ? "" : columns + ",") + 
                          geoColumn + " as " + super.getGeoColumn() + " \n" + 
              "  FROM " + "(SELECT " + searchGeom + " AS geom FROM DUAL) a,\n" +
              "       " + this.getFullObjectName() + " t \n" +
              " WHERE " + this.searchPredicate;

		LOGGER.logSQL(sql);

		return sql;
	}

    // TODO: Rewrite for new generateSQL approach
    //
    private String getIdentifySQL(boolean _project) 
    {
        String sql = "";
        String spatialIndexName;
        try {
            spatialIndexName = Queries.getSpatialIndexName(this.getConnection(), 
                                                                super.getSchemaName(), 
                                                                super.getObjectName(), 
                                                                super.getGeoColumn());
        } catch (SQLException e) {
            spatialIndexName = "";
        }

        this.setKeyColumn(this.getKeyColumn());
    	String keyColumnPredicate = Strings.isEmpty(this.getKeyColumn()) 
                                    ? "" 
                                    : "   AND b." + this.getKeyColumn() + 
                                          " = a." + this.getKeyColumn() + "\n";

    	String attributes = "a.*" +
                            (_project 
                             ? ",MDSYS.SDO_CS.TRANSFORM(a." + super.getGeoColumn() + "," + 
                                                       this.spatialView.getSRID() + 
                               ") as " + this.projectedGeometryName + "\n"
                             : "");
    	
        if ( this.hasIndex() ) 
        {
            String queryHint = "/*+ORDERED" + (Strings.isEmpty(spatialIndexName) 
            		           ? "" : " INDEX(b " + spatialIndexName+")") + 
            		           "*/";
            
            if ( ! this.getPreferences().isNN() ) 
            {
            	// Modify baseSQL to enable reference to search distance CTE
            	String baseSQL = this.getSQL();
            	baseSQL = baseSQL.replaceFirst("FROM ",
                                               "FROM searchDistanceCTE sdcte,\n       ");
            	// Construct SQL
            	sql = String.format(
                        "WITH searchDistanceCTE As (\n" +
                        "  SELECT MDSYS.SDO_GEOM.SDO_DISTANCE(?,?,?,?) AS dist \n" +
                        "    FROM DUAL \n" +
                        ")\n" +
                        "%s" +
                        "   AND MDSYS.SDO_WITHIN_DISTANCE(t.%s,?,'distance='||sdcte.dist) = 'TRUE'",
                        baseSQL,
                        super.getGeoColumn()
            		  );
            	//1 ? searchPt                 (sdo_geometry)
            	//2 ? DistancePoint            (sdo_geometry)
            	//3 ? tolerance                (number)
            	//4 ? unit                     (string)
            	//5 ? baseSQL sdo_filter Geom  (sdo_geometry)
            	//6 ? baseSQL sdo_filter param (string)
            	//7 ? search point             (sdo_geometry)
            	
            } else /* isNN() */ {
            	
            	sql = String.format(
                        "WITH searchDistanceCTE As (\n" +
                        "  SELECT MDSYS.SDO_GEOM.SDO_DISTANCE(?,?,?,?) AS dist \n" +
                        "    FROM DUAL \n" +
                        ")\n" +
                        "SELECT %s\n" + 
                        "       %s\n" +
                        "  FROM (%s" +
                        "       ) a,\n" +
                        "       %s b,\n" +
                        "       searchDistanceCTE s \n" +
                        " WHERE SDO_NN(b.%s,?,?,1) = 'TRUE' \n" +
                        "   AND SDO_NN_DISTANCE(1) < s.dist \n" +
                        "%s" + 
                        " ORDER BY sdo_nn_distance(1)",
                		queryHint, 
                		attributes,
                		this.getSQL(),
                		this.getFullObjectName(),
                		super.getGeoColumn(),
                		keyColumnPredicate
                	  );
            	//1 ? searchPt         (sdo_geometry)
            	//2 ? DistancePoint    (sdo_geometry)
            	//3 ? tolerance        (number)
            	//4 ? unit             (string)
            	//5 ? CTE Search       (sdo_geometry)
            	//6 ? sdo_filter param (string)
            	//7 ? search point     (sdo_geometry)
            	//8 ? sdo_nn parameter (string) <-- Not SDO_NN
            }
            
        } else /* ! hasIndex() */  {
        	
        	// Modify baseSQL to enable reference to search distance CTE
        	String baseSQL = this.getSQL();
        	baseSQL = baseSQL.replaceFirst("FROM ",
                                           "FROM searchCTE scte,\n       ");
        	
        	// Construct SQL
        	sql = String.format(
                    "WITH searchCTE As (\n" +
        	        "SELECT MDSYS.SDO_GEOM.SDO_BUFFER(\n" +
                    (_project  
                  ? "            MDSYS.SDO_CS.TRANSFORM(?,"+ this.spatialView.getSRID()+"),\n" 
                  : "            ?,\n") +
        	        "            MDSYS.SDO_GEOM.SDO_DISTANCE(?,?,?,?),\n" +
        	        "            ?) as geom\n" +
                    "  FROM DUAL \n" +
                    ")\n" +
                    "%s",
                    baseSQL
        		  );

        	//1 ? searchPt           (sdo_geometry)
        	//2 ? searchPt           (sdo_geometry)
        	//3 ? DistancePoint      (sdo_geometry)
        	//4 ? tolerance          (number)
        	//5 ? unit               (string)
        	//6 ? buffer tolerance   (number)
        	//7 ? validate tolerance (number)
        	//8 ? relate tolerance   (number)
        }
        LOGGER.logSQL(sql);
        return sql;
    }

    protected PreparedStatement getIdentifyPS(Point2D _worldPoint, 
                                              int     _numSearchPixels) 
    {
    	// ********************************
        // Create parameter values ......
    	//
        
        // Get target SRID and units_parameter in case layer have been drag-and-dropped to a different view/srid
		//
		String lengthUnits = Tools.getViewUnits(this.spatialView,Constants.MEASURE.LENGTH);
		int      querySRID = -1;

		boolean project = this.project 
	            && this.spatialView.getSRIDAsInteger() != Constants.SRID_NULL 
	            && this.getSRIDAsInteger()             != Constants.SRID_NULL ;

        String units_parameter = "";
        if ( project )
		{
		  querySRID = this.spatialView.getSRIDAsInteger(); 
		  units_parameter = String.format(",unit=%s",lengthUnits);
		} else {
		  querySRID = this.getSRIDAsInteger();
		  if ( querySRID != Constants.SRID_NULL )
		      units_parameter = String.format(",unit=%s",lengthUnits);
		}

		// Create search mbr, point and distance measurement variables
		//
		Point2D.Double pixelSize = null;
		Struct       searchPoint = null;
		Struct     distancePoint = null;
		Struct         searchMBR = null;
		Connection          conn = this.getConnection();
	    String       identifySQL = this.getIdentifySQL(project);
        String            params = ""; // For LOGGER.logSQL

        try 
        {
        	if ( conn == null )
        		throw new SQLException ("No active database connection");
        	
        	// Compute End point of search distance line
            // Note: first point of line denoting search distance is the passed in point (view/world units)
		    //
        	pixelSize   = this.getSpatialView().getSVPanel().getMapPanel().getPixelSize();
            searchPoint = JGeom.toStruct(new JGeometry(_worldPoint.getX(),_worldPoint.getY(),querySRID),conn);

            // Add 10% to distance
	        Point2D distancePoint2D = new Point2D.Double(
	                                       _worldPoint.getX() + 
	                                       ((pixelSize.getX() >= pixelSize.getY()) ? (_numSearchPixels*pixelSize.getX() * 1.1) : 0.0),
			                               _worldPoint.getY() + 
			                               ((pixelSize.getY() >  pixelSize.getX()) ? (_numSearchPixels*pixelSize.getY() * 1.1) : 0.0)
                                          );
            distancePoint = JGeom.toStruct(new JGeometry(distancePoint2D.getX(),distancePoint2D.getY(),querySRID),conn);

	  	    // SDO_Filter geometry has same SRID as layer (for base SQL)
            // _worldPoint/pixelSize is in view units. Could even be decimal degrees
            // TODO But when layer has been dragged into a different view????
            //
		    Envelope mbr = new Envelope(
                                 _worldPoint.getX() - (_numSearchPixels*pixelSize.getX()/1.9),
		                         _worldPoint.getY() - (_numSearchPixels*pixelSize.getY()/1.9),
		                         _worldPoint.getX() + (_numSearchPixels*pixelSize.getX()/1.9),
		                         _worldPoint.getY() + (_numSearchPixels*pixelSize.getY()/1.9),
		                         querySRID
		                   );
            searchMBR = Queries.projectEnvelope(conn,mbr,querySRID);
      	} catch (Exception e) {
			LOGGER.warn(e.getLocalizedMessage());
			return null;
		}

    	// *******************************************
		// Create PreparedStatement and set its values
		//
			
        PreparedStatement pStatement = null;
        try 
        {
	        pStatement = (PreparedStatement)conn.prepareStatement(identifySQL);

	        // Assign parameters (see getIdentifySQL)
	        //
	        String spatialFilterClause = null;
	        int stmtParamIndex = 1;
	        if ( this.hasIndex() ) 
	        {
	        	//1 ? searchPt         (sdo_geometry)
	            pStatement.setObject(stmtParamIndex++,searchPoint,java.sql.Types.STRUCT);                     
	                params += String.format("? %s\n",RenderTool.renderStructAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
	           	//2 ? DistancePoint    (sdo_geometry)
	            pStatement.setObject(stmtParamIndex++,distancePoint,java.sql.Types.STRUCT);
	                params += String.format("? %s\n",RenderTool.renderStructAsPlainText(distancePoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
	           	//3 ? tolerance        (number)
	            pStatement.setDouble(stmtParamIndex++,this.getTolerance());
	                params += String.format("? %f\n",this.getTolerance());
	           	//4 ? unit             (string)
	            pStatement.setString(stmtParamIndex++,units_parameter.replace(",",""));
	                params += String.format("? '%s'\n",units_parameter.replace(",",""));
	           	//5 ? baseSQL sdo_filter Geom (sdo_geometry)
	            pStatement.setObject (stmtParamIndex++, searchMBR,java.sql.Types.STRUCT);
	                params += String.format("? %s\n", RenderTool.renderStructAsPlainText(searchMBR, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
	          	//6 ? baseSQL sdo_filter param (string)
	            spatialFilterClause = this.sdoFilterClause; // Default
	            if (this.getMinResolution()) {
	                double maxPixelSize = Math.max(pixelSize.getX(), pixelSize.getY());
	                if (maxPixelSize != 0.0) {
	                    spatialFilterClause = String.format(this.sdoFilterMinResClause,maxPixelSize);
	                }
	            }
	            pStatement.setString(stmtParamIndex++, spatialFilterClause);
	                params += "? '"+spatialFilterClause+"'\n";
	
	            //7 ? search point     (sdo_geometry)
	            pStatement.setObject(stmtParamIndex++,searchPoint,java.sql.Types.STRUCT);                     
	                params += String.format("? %s\n",RenderTool.renderStructAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
	
	            if ( this.getPreferences().isNN() ) {
	            	//8 ? sdo_nn parameter (string) <-- Not SDO_NN
	                String sdo_num_res = "sdo_num_res=" + String.valueOf(_numSearchPixels * 2);
	                pStatement.setString(stmtParamIndex++,sdo_num_res+units_parameter); 
	                    params += "? '" + sdo_num_res + "'\n";                    
	            }
	            
	        } else { /* ! this.hasIndex() */

	        	//1 ? searchPt         (sdo_geometry)
	            pStatement.setObject(stmtParamIndex++,searchPoint,java.sql.Types.STRUCT);                     
	                params += String.format("? %s\n",RenderTool.renderStructAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
	        	//2 ? searchPt         (sdo_geometry)
	            pStatement.setObject(stmtParamIndex++,searchPoint,java.sql.Types.STRUCT);                     
	                params += String.format("? %s\n",RenderTool.renderStructAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
	           	//3 ? DistancePoint    (sdo_geometry)
	            pStatement.setObject(stmtParamIndex++,distancePoint,java.sql.Types.STRUCT);
	                params += String.format("? %s\n",RenderTool.renderStructAsPlainText(distancePoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
	           	//4 ? tolerance        (number)
	            pStatement.setDouble(stmtParamIndex++,this.getTolerance());
	                params += String.format("? %f\n",this.getTolerance());
	           	//5 ? unit             (string)
	            pStatement.setString(stmtParamIndex++,units_parameter.replace(",",""));
	                params += String.format("? '%s'\n",units_parameter.replace(",",""));
                //6 ? buffer tolerance        (number)
	            pStatement.setDouble(stmtParamIndex++,this.getTolerance());
                    params += String.format("? %f\n", this.getTolerance());	            
  	           	//7 ? baseSQL sdo_filter Geom (sdo_geometry)
    	        pStatement.setObject (stmtParamIndex++, searchMBR,java.sql.Types.STRUCT);
    	                params += String.format("? %s\n", RenderTool.renderStructAsPlainText(searchMBR, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
                //8 ? validate tolerance        (number)
    	        pStatement.setDouble(stmtParamIndex++,this.getTolerance());
                        params += String.format("? %f\n", this.getTolerance());	            
	        	//9 ? relate tolerance        (number)
	            pStatement.setDouble(stmtParamIndex++,this.getTolerance());             
	                params += String.format("? %f\n", this.getTolerance());
	        }
	    	LOGGER.logSQL(identifySQL + "\n" + params);
        } catch (Exception e) {
        	e.printStackTrace();
        }
		return pStatement;
    }

    /**
     * @param SQL
     */
    public void setSQL(String _SQL) {
        if (Strings.isEmpty(_SQL) ||
            (!_SQL.equalsIgnoreCase(this.layerSQL))) {
            this.layerSQL = _SQL;
        }
    }

    public String getSQL() {
    	if ( Strings.isEmpty(this.layerSQL) )
    		this.layerSQL = this.generateSQL();
    	return this.layerSQL;
    }

    /*
     * getSQLPS is for the main draw SQL (not identify)
     */
    protected PreparedStatement getSQLPS(String _sql,
    	                               Envelope _mbr) 
	{
    	// See generateSQL for created base layer SQL
		boolean project = this.mustProject();

		int querySRID = project 
				        ? this.spatialView.getSRIDAsInteger()
				        : (this.spatialView.getSRIDAsInteger() == Constants.SRID_NULL 
				          ? this.getSRIDAsInteger()
						  : this.spatialView.getSRIDAsInteger());

		PreparedStatement pStatement = null;
		String sdoFilterClause = "";
		String params = "";
		Struct filterGeom = null;
		int stmtParamIndex = 1;
		try {
			pStatement = super.getConnection().prepareStatement(_sql);

			// Filter Geom is always expressed in terms of the layer's SRID
			// even if displaying in projected view (TRANSFORM in SQL will do the transformation)
			//
			filterGeom = this.getSearchFilterGeometry(_mbr, project, querySRID, this.getSRIDAsInteger());
			pStatement.setObject(stmtParamIndex++, filterGeom, java.sql.Types.STRUCT);
			    params += String.format("? %s\n", SDO_GEOMETRY.getGeometryAsString(filterGeom));

			// Set up SDO_Filter clause depending on whether min_resolution is to be applied or not
			// If ST_Geometry, can't use sdoFilterClause
			//
			if (this.hasIndex() ) {
				if ( this.isSTGeometry() == false) { 
					sdoFilterClause = this.getSDOFilterParameters();
				    pStatement.setString(stmtParamIndex++, sdoFilterClause);
                        params += "? " + sdoFilterClause + "\n";
				}
			} else {
                //     "MDSYS.SDO_GEOM.VALIDATE_GEOMETRY(t."+super.getGeoColumn()+",?) = 'TRUE'\n"
                // "AND MDSYS.SDO_GEOM.RELATE(t."+super.getGeoColumn()+",'ANYINTERACT',a.geom,?) = 'TRUE'";
	            pStatement.setDouble(stmtParamIndex++, this.getTolerance());
                    params += String.format("? %f\n",this.getTolerance());
	            pStatement.setDouble(stmtParamIndex++, this.getTolerance());
                    params += String.format("? %f\n",this.getTolerance());
			}

			LOGGER.logSQL(_sql + "\n" + params);

		} catch (SQLException sqle) {
			// isView() then say no index
			if (this.isView()) {
				this.setIndex(false);
			}
			Tools.copyToClipboard(super.propertyManager.getMsg("SQL_QUERY_ERROR", sqle.getMessage()), _sql + "\n"
					+ String.format("? %s\n? %s", SDO_GEOMETRY.getGeometryAsString(filterGeom), sdoFilterClause));
		}
		return pStatement;
	}

    /**
     * Execute SQL and draw data on given graphical device
     * @param _mbr MBR coordinates
     * @param _g2 graphical device
     * @return if return false, something was wrong (for example, Connection with DB faild)
     */
    public boolean drawLayer(Envelope _mbr, Graphics2D _g2) 
    {
        Connection conn = null;
        try {
            // Make sure layer's connection has not been lost
            conn = super.getConnection();
            if ( conn == null ) { return false; }
        } catch (IllegalStateException ise) {
            // Thrown by super.getConnection() indicates starting up, so we do nothing
            return false;
        }
        
        // If not indexed, check again as might have been indexed
        if ( this.hasIndex()==false ) {
            this.setIndex();
            if ( this.hasIndex() ) {
                // force change to SQL 
                this.layerSQL = null;
            }
        }

        // Get SQL and set up its parameters 
        //
        String sql = this.getSQL();
        PreparedStatement pStatement = null;
        pStatement = this.getSQLPS(sql,_mbr);

        // Now execute query and return result
        //
        boolean success = SVDrawQueries.executeQuery(
                                        this, 
                                        pStatement,
                                        sql,
                                        _g2,
                                        this.drawTools);
        return success;
    }

    private boolean queryContainsOperator(String _querySQL) {
        Constants.SDO_OPERATORS[] sdoOperators = Constants.SDO_OPERATORS.values();
        for (int i=0; i < sdoOperators.length;i++) {
        	if ( _querySQL.contains("SDO_"+sdoOperators[i].toString()) ) 
        	  return true;
        }    	
    	return false;
    }

    /**
     * @history Simon Greener April 13th 2010
     *          - Reorganised SQL
     *          - Moved to use of new Constants class
     * @history Simon Greener June 7th 2010
     *          - Added
     *            ORDER BY
     *             SDO_NN_DISTANCE ancillary operator
     *          - etc to try and address John O'Toole's partition problem
     * @history Simon Greener December 15th 2010
     *          - Fully parameterized queries. 
     *          - Changed method of calculating cutoff distance for geodetic data
     * @history Simon Greener December 15th 2010
     *          - New approach that honors current SQL defining layer
     **/
    public ArrayList<QueryRow> queryByPoint(Point2D _worldPoint, 
                                            int     _numSearchPixels) 
    {
    	Struct              rStruct = null; // read SDO_GEOMETRY column
        ArrayList<QueryRow>   rList = new ArrayList<QueryRow>(); // list of return rows
        
        int         numSearchPixels = _numSearchPixels <= 0 
                                      ? this.getPreferences().getSearchPixels() 
                                      : _numSearchPixels;
        // Need future check for 3D indexed layers??
        // Sable 5-1 Data and Index Dimensionality, and Query Support
        //
        String querySQL = "";
        String params = "";
        Connection conn = null;
        try {
            // Set up the connection and statement
            //
            conn = super.getConnection();
            if (conn == null) { return null; }
            
            // Create statement
            //
            PreparedStatement pStatement = this.getIdentifyPS(
            		                            _worldPoint, 
                                                _numSearchPixels   		
                                           );

    		// If geometry is transformed (for display) then we are best to give it a different name in the output string
            //
    		String geoColumn = project 
    		                 ? this.projectedGeometryName.toUpperCase() 
    		                 : this.getGeoColumn().toUpperCase();
    		geoColumn = geoColumn.replace("\"","");

            // ****************** Execute the query ************************
            //
            ResultSet ors = pStatement.executeQuery();
            ors.setFetchDirection(ResultSet.FETCH_FORWARD);
            ors.setFetchSize(this.getResultFetchSize());
            OracleResultSetMetaData rSetM = (OracleResultSetMetaData)ors.getMetaData(); // for column name

            String    rowID = "",
                      value = "",
                 columnName = "",
             columnTypeName = "";
            while (ors.next()) {
                LinkedHashMap<String, Object> calValueMap = new LinkedHashMap<String, Object>(rSetM.getColumnCount() - 1);
                for (int col = 1; col <= rSetM.getColumnCount(); col++) {
                    columnName     = rSetM.getColumnName(col);
                    columnTypeName = rSetM.getColumnTypeName(col);
                    if (columnTypeName.equals("LONG")) {
                        // LONGs will kill the SQL stream and we can't use them anyway
                        LOGGER.info("GeoRaptor ignored " + rSetM.getColumnName(col) + "/" + rSetM.getColumnTypeName(col));
                        continue;
                    }
                    if (columnName.equalsIgnoreCase(geoColumn)) {
                        rStruct = (java.sql.Struct)ors.getObject(col);
                        if (ors.wasNull() || rStruct==null) {
                            break; // process next row
                        }              
                        String sqlTypeName = rStruct.getSQLTypeName();
                        // If ST_GEOMETRY, extract SDO_GEOMETRY
                        if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                            rStruct = SDO_GEOMETRY.getSdoFromST(rStruct);
                        } 
                        if (rStruct == null) break;
                    } else {
                          if ( Tools.dataTypeIsSupported(columnTypeName) )
                          {
                              try
                              {
                            	  @SuppressWarnings("unused")
								  Object objValue = ors.getObject(col);
                                  if (ors.wasNull()) {
                                      value = "NULL";
                                  } else {
                                      if ( ors.getMetaData().getColumnType(col) == Types.ROWID ) {
                                          //rid   = ors.getRowId(col);
                                          //rowID = "NULL"; // rid.toString();
                                          //value = rowID;
                                          continue;
                                      } else {
                                          value = SQLConversionTools.convertToString(conn, ors, col);
                                          if (value == null)
                                              value = "NULL";
                                      }
                                  }
                              } catch (Exception e) {
                                  value = "NULL";
                              }
                              calValueMap.put(rSetM.getColumnName(col), value);
                          }
                    }
                }
                rList.add(new QueryRow(rowID, calValueMap, rStruct));
            }
            ors.close();
            pStatement.close();
            if ( this.getPreferences().isLogSearchStats() ) {
                LOGGER.fine("\n" + querySQL + "\n" + params);
            }
        } catch (SQLException sqlex) {
            Tools.copyToClipboard(super.propertyManager.getMsg("SQL_QUERY_ERROR",sqlex.getMessage()),querySQL + "\n" + params);
        } 
        return rList;
    }

    // ********************** SQL ********************************
    // ***********************************************************

    public LinkedHashMap<String, String> 
           getColumnsAndTypes(boolean _onlyNumbersDatesAndStrings,
                              boolean _fullDataType) 
    throws SQLException 
    {
        LinkedHashMap<String, String> columnsTypes = new LinkedHashMap<String,String>(255);

        // Should try and get columns etc from SQL being executed.
        // Otherwise go to table itself.
        //
        Connection conn = super.getConnection();
        if ( conn == null )
        	return columnsTypes;
            
        String layerSql = this.getSQL();
        if (Strings.isEmpty(layerSql)) {
        	// No SQL, so no possible modification of columns by user
            LinkedHashMap<String, String> colsAndTypes =
                              Queries.getColumnsAndTypes(conn,
                                                         this.getSchemaName(),
                                                         this.getObjectName(),
                                                         _onlyNumbersDatesAndStrings,
                                                         _fullDataType);
            return colsAndTypes;
        }
            
        // User may have modified the SQL, so execute it and discover columns and types...
        // We have to assign all parameter values and ensure now rows are processed as we
        // don't want data only the metadata (use WHERE 1=0)
        // The use of the PrepareStatement metadata is a cool way of accessing actual columns
        // but it is dangerous as it has to assume that certain where clause predicates exist
        // with exact string value. 
        // If user has modified SQL it hould be limited to only adding or deleting columns.
        //
        try 
        {
        	/* Layer SQL normally has only two parameters and should look like this...
            	WITH cteGeom AS (
            	  SELECT ? AS geom FROM DUAL
                )
            	SELECT f.*
            	  FROM (SELECT "ID","LABEL","ANGLEDEGREES",t.GEOM as GEOM
            	          FROM cteGeom a,
            			       GEORAPTOR.PROJPOINT2D t
            			 WHERE SDO_FILTER(t.GEOM,a.GEOM,?) = 'TRUE'
            			) f
            */

            // Replace parameters (?) with meaningless values that doesn't stop statement preparation
            // We are only after the attributes/columns in a potentially modified SQL statement
            //
            String sql = "";
            sql = layerSql.replaceFirst("\\?","SDO_GEOMETRY(2001," + this.getSRID()+",SDO_POINT_TYPE(0,0,null),null,null)");                
            if ( this.hasIndex() ) {
                sql = sql.replace("\\?",sdoFilterClause);                  
            } 
            sql = sql.replace("WHERE ","WHERE 1=0 AND ");

            OraclePreparedStatement pstmt;
            pstmt = (OraclePreparedStatement)conn.prepareStatement(sql);
            ResultSetMetaData rsmd = pstmt.getMetaData();
            columnsTypes = SVDrawQueries.getAttributesFromMetadata(rsmd,
              				                                       _onlyNumbersDatesAndStrings,
               				                                       _fullDataType);
            rsmd = null;
            pstmt.close();
            pstmt = null;
        } catch (SQLException e) {
            LOGGER.info("Can't identify columns in SQL");
            e.printStackTrace();
            return Queries.getColumnsAndTypes(conn,
                      this.getSchemaName(),
                      this.getObjectName(),
                      _onlyNumbersDatesAndStrings,
                      _fullDataType);
        }
        return columnsTypes;
    }

    public void setMBRRecalculation(boolean _recalc) {
        this.calculateMBR = _recalc;
    }

    public boolean getMBRRecalculation() {
        return this.calculateMBR;
    }

    public void setNumberOfFeatures(long _number) 
    {
        LOGGER.debug("setNumberOfFeatures()="+_number);
        this.numberOfFeatures = _number;
        if (this.getPreferences().isNumberOfFeaturesVisible() ) {
            this.getSpatialView().getSVPanel().getViewLayerTree().refreshLayerCount(this,_number);
        } else {
            this.getSpatialView().getSVPanel().getViewLayerTree().removeLayerCount(this);
        }
    }
    
    public long getNumberOfFeatures() {
        return this.numberOfFeatures;
    }
    
    public void setMinResolution(boolean _minResolution) {
        if ( this.geometryType == Constants.GEOMETRY_TYPES.POINT )
            this.minResolution = false;
        else
            this.minResolution = _minResolution;
    }
  
    public boolean getMinResolution() {
        return ( this.geometryType == Constants.GEOMETRY_TYPES.POINT ) ? false : this.minResolution;
    }
  
    public int getPrecision(boolean _calculate) {
        return super.getPrecision(_calculate);
    }

    public double getTolerance() {
        return super.getTolerance();
    }
  
    public void setResultFetchSize(int _resultFetchSize) {
        this.resultFetchSize = _resultFetchSize;
    }
  
    public int getResultFetchSize() {
        return this.resultFetchSize;
    }
    
    public SVSpatialLayerDraw getDrawTools() {
        return this.drawTools;
    }
  
    public SpatialView getView() {
        return super.getSpatialView();
    }
  
    public boolean getProject() {
        return this.project;
    }
  
    public void setProject(boolean _yes, boolean _calcMBR) {
        this.project = _yes;
        if (_calcMBR) {
            // Anytime this is called we need to recalculate the MBR of the layer
            this.setLayerMBR(super.mbr, super.getSRIDAsInteger());
        }
    }

    public void setFetchSize(int _fetchSize) {
        if (_fetchSize == 0)
            this.resultFetchSize = this.getPreferences().getFetchSize();
        else
            this.resultFetchSize = _fetchSize;
    }

    public int getFetchSize() {
        return this.resultFetchSize;
    }

	protected double getSearchRadius(int _numSearchPixels) 
    throws NoninvertibleTransformException 
    {
        double maxSearchRadius = 0.0;
        // Get search radius
        // Is diagonal distance computed as sqrt of size of pixel (* number of pixels in search)
        // in X and Y squared (ie pythagoras)
        //
        Point.Double pixelSize = this.spatialView.getMapPanel().getPixelSize();
        maxSearchRadius = Math.sqrt(Math.pow(pixelSize.getX() * _numSearchPixels, 2.0f) +
                                    Math.pow(pixelSize.getY() * _numSearchPixels, 2.0f));            
        if (!this.project) 
        {
            if (super.getSRIDType() == Constants.SRID_TYPE.UNKNOWN)
                super.setSRIDType();
            switch (super.getSRIDType()) {
            case GEODETIC_COMPOUND:
            case GEODETIC_GEOCENTRIC:
            case GEODETIC_GEOGRAPHIC2D:
            case GEODETIC_GEOGRAPHIC3D:
            case GEOGRAPHIC2D:
                // Generate search distance as this is geodetic 
                Point screenPoint1 = this.spatialView.getMapPanel().getScreenCenter();
                Point2D.Double worldPoint1 = (Point2D.Double)this.spatialView.getMapPanel().ScreenToWorld(screenPoint1);
                Point screenPoint2 = new Point(screenPoint1.x+_numSearchPixels,screenPoint1.y);
                // System.out.println("screenPoint1 " + screenPoint1.toString() + " => screenPoint2 " + screenPoint2.toString());
                Point2D.Double worldPoint2 = (Point2D.Double)this.spatialView.getMapPanel().ScreenToWorld(screenPoint2);
                // System.out.println("worldPoint1 " + worldPoint1.toString() + " => " + worldPoint2.toString());
                // There are more efficient ways of calculating this distance... 
                // perhaps via an inline computation query in actual SDO_NN SQL sdo_geom.sdo_distance(sdo_point(1),sdo_point(2))
                //
                try { 
                    double distVincenty = COGO.distVincenty(worldPoint1.getX(),worldPoint1.getY(), worldPoint2.getX(),worldPoint2.getY());
                    maxSearchRadius = distVincenty;
                } catch (Exception e) {
                	LOGGER.info("DistVincenty threw exception " + e.getMessage());
                }
                break;
			default:
				break;
            }
        }
        return maxSearchRadius;
    }

    public GEOMETRY_TYPES getGeometryType()
    {
    	return super.getGeometryType();
    }
    

    public Envelope getMBR() 
    {
    	return super.getMBR();
    }
    

	public MetadataEntry getMetadataEntry()
	{
		return super.getMetadataEntry();
	}

	@Override
	public void setGeometry(JGeometry _geometry) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void setBufferDistance(double bufferDistance) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setBuffered(boolean b) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setRelationshipMask(String relationshipMask) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setSdoOperator(SDO_OPERATORS sdoOperator) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getLayerSQL() {
		return this.layerSQL;
	}

	@Override
	public void setKeyColumn(String _keyColumn) 
	{
		if ( Strings.isEmpty(_keyColumn) )
			try {
				this.keyColumn = Queries.getPrimaryKey(
						             this.getConnection(),
				                     this.getSchemaName(),
				                     this.getObjectName()
				                 );
			} catch (IllegalArgumentException | SQLException e) {
				e.printStackTrace();
			}
		else
			this.keyColumn = _keyColumn;		
	}

	@Override
	public String getKeyColumn() {
		return this.keyColumn;
	}

} 
