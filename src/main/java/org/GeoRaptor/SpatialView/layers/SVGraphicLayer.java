package org.GeoRaptor.SpatialView.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JOptionPane;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.sql.Queries;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.JGeom;
import org.GeoRaptor.tools.MathUtils;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.RenderTool;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.geotools.util.logging.Logger;

import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;
import oracle.spatial.geometry.JGeometry;
import oracle.sql.NUMBER;

public class SVGraphicLayer 
extends SVLayer 
implements iLayer 
{

    public static final String CLASS_NAME = Constants.KEY_SVGraphicLayer;
    
	public PropertiesManager propertyManager = null; // Properties File Manager

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVGraphicLayer");
    
    protected List<QueryRow>     cache = new ArrayList<QueryRow>();

    protected Constants.SDO_OPERATORS 
                 sdoOperator = Constants.SDO_OPERATORS.ANYINTERACT;
    
    protected String sql = "";         // SQL used to create layer
    
    protected String layerName   = "";         // Unique layer name. This is the universal unique name for this layer.
    
    protected String layerCopySuffix = " (COPY)";  // Should be Tools>Preferences>GeoRaptor

    protected String visibleName = "";         // This text is write into JTree where we show layers
    
    protected Preferences preferences = null;

    protected String  desc = "";         // Layer description. It is diplay when user left mouse cursor on layer name.

    protected boolean draw = true;

    protected boolean drawGeometry = false;

    protected boolean project = false;
    
    protected double bufferDistance = 0.0;
    
    protected boolean buffered = false;

    protected Styling styling = new Styling();
    
    protected boolean calculateMBR = false;

    protected boolean indexExists = true;       // Does spatial index exist for this layer

    protected boolean minResolution = false;      // Do we use min_resolution sdo_filter parameter when querying?
    
    protected int resultFetchSize = 100;        // Fetch size for ResultSet
    
    protected long numberOfFeatures = 0;          // Feature Count for display and stats

    protected JGeometry queryGeometry = null;

    protected String relationshipMask = "";    

    protected SVSpatialLayerDraw drawTools = new SVSpatialLayerDraw(this); // Class used to draw this layer
    
    public SVGraphicLayer(SpatialView   _sView, 
                          String        _layerName,
                          String        _visibleName,
                          String        _desc, 
                          MetadataEntry _me, 
                          boolean       _draw) 
    {
        super(_sView, _me);
        this.layerName   = Strings.isEmpty(_layerName)? UUID.randomUUID().toString() : _layerName;
        this.visibleName = _visibleName;
        this.desc        = _desc;
        this.draw        = _draw;
        this.preferences = MainSettings.getInstance().getPreferences();
        this.drawGeometry = this.preferences.isDrawQueryGeometry();
        this.setResultFetchSize(this.preferences.getFetchSize());
        this.setPrecision(-1); // force calculation from mbr
        this.styling = new Styling();
    }

    public SVGraphicLayer(iLayer _sLayer) 
    {
        super(_sLayer.getSpatialView(), 
              _sLayer.getMetadataEntry());
        this.preferences = MainSettings.getInstance().getPreferences();
        // Change base name
        String layerName = _sLayer.getSpatialView().checkLayerName(_sLayer.getLayerName());
        this.setLayerName(layerName);
        this.setVisibleName(_sLayer.getVisibleName());
        this.setDesc(_sLayer.getDesc());
        this.setDraw(_sLayer.isDraw());
        this.setMinResolution(false);
        this.drawGeometry = this.preferences.isDrawQueryGeometry();
        this.setResultFetchSize(this.preferences.getFetchSize());
        this.setPrecision(_sLayer.getPrecision(false)); // force calculation from mbr
        this.setStyling(_sLayer.getStyling());
        if ( _sLayer instanceof SVGraphicLayer )
        {
        	this.setMBR(_sLayer.getMBR());
        	this.setSRIDType(_sLayer.getSRIDType());
        	this.setGeometryType(_sLayer.getGeometryType());
        	this.setPrecision(_sLayer.getPrecision(false));
        	this.setIndex(_sLayer.hasIndex());
            this.styling.setShadeType(Styling.STYLING_TYPE.NONE);
            this.add(((SVGraphicLayer)_sLayer).cache);
        }
    }

    public SVGraphicLayer createCopy()  
    throws Exception 
    {
        // _renderOnly is ignored for SVSpatialLayers
        //
        SVGraphicLayer newLayer = null;

        // Shared SVLayer stuff
        //
        newLayer = new SVGraphicLayer(
        		         super.getSpatialView(),
                         this.getLayerName(),
                         this.getVisibleName(),
                         this.getDesc(),
                         super.getMetadataEntry(),
                         this.isDraw()
        		);
                
        // set SVLayer properties (What is a copy? Is it a render layer?)
        newLayer.setSRIDType(super.getSRIDType());
        newLayer.setGeometryType(super.getGeometryType());
        newLayer.setConnectionName(super.connName);
        newLayer.setIndex(this.hasIndex());
        newLayer.setMBR(this.getMBR());
        String newName = super.getSpatialView().getSVPanel().getViewLayerTree().checkName(this.getLayerName());
        newLayer.setLayerName(newName);
        newLayer.setVisibleName(this.getVisibleName()+layerCopySuffix);
        newLayer.setDesc(this.getDesc());
        newLayer.setDraw(this.isDraw());
        newLayer.setSQL(this.getSQL());
        newLayer.setMinResolution(this.getMinResolution());
        newLayer.setFetchSize(this.getFetchSize());
        newLayer.setPrecision(this.getPrecision(false));
        newLayer.setProject(this.getProject(),false);
        newLayer.setSdoOperator(this.getSdoOperator());
        newLayer.setGeometry(this.getGeometry());
        newLayer.setStyling(this.styling);        
        return newLayer;
    }

    public SVGraphicLayer createCopy(boolean _renderCopy) 
    throws Exception {
        if ( _renderCopy ) {
          return this.createCopy();
        } else {
            throw new Exception(this.propertyManager.getMsg("GRAPHIC_THEME_COPY"));
        }
    }

	@Override
    public String getClassName() {
        return SVGraphicLayer.CLASS_NAME;
    }
    
    public void setConnection() {
        // Stops call to SVLayer to set the oracle connection
    }

    public String getConnectionName() {
        return super.getConnectionName();
    }

    public void setIndex() {
        this.indexExists = false; // Graphic Layers do not have indexes
    }
    
    public void setIndex(String _hasIndex) {
        this.indexExists = false; // Graphic Layers do not have indexes
    }
  
    public void setIndex(boolean _indexExists) {
       this.indexExists = false; // Graphic Layers do not have indexes
    }
    
    public boolean hasIndex() {
        return true;
    }

    public boolean isBuffered() {
        return this.buffered && this.bufferDistance != 0.0;
    }

    private boolean isProjected() {
        boolean project = super.spatialView.getSRID().equals(Constants.NULL) == false && 
                          super.getSRID().equals(Constants.NULL) == false;
		return project;
    }
    
	public String getSQL() {
    	if (Strings.isEmpty(this.sql)) {
            this.setSQL(this.getInitSQL());
    	} 
    	return this.sql;
    }
	
	@Override
	public void setInitSQL() {
    	if (Strings.isEmpty(this.sql)) {
            this.setSQL(this.getInitSQL());
    	} 
		// TODO Auto-generated method stub		
	}

    public String getInitSQL()
	{
		String sql = "";
		// Get search predicate
		//
		String searchGeom = "",
		bufferGeom = "";
		
		// If filter geometry buffered could need projecting to stored geometry 
		// We transform/project the search geometry (eg draw optimized rectangle) to the SRID of the
		// to-be-searched table/column as the search geometry will be in the SRID units of the view.
		//
		if (this.isBuffered() && this.isProjected()) 
		{
		  /**
           * MDSYS.SDO_GEOM.SDO_BUFFER(
		   *    geom IN SDO_GEOMETRY,
		   *    dist IN NUMBER,
		   *    tol IN NUMBER
		   *    [, params IN VARCHAR2]
		   * ) RETURN SDO_GEOMETRY;
		  **/
		  String lengthUnits  = super.getSpatialView().getDistanceUnitType();
	  	  String bufferParams = ((super.getSRID().equals(Constants.NULL) || 
			                     Strings.isEmpty(lengthUnits)) 
		                         ? "" 
		                         : ",'unit=" + lengthUnits + "'");
		  bufferGeom = String.format("MDSYS.SDO_GEOM.SDO_BUFFER(?,?,?%s)",bufferParams);
		  searchGeom = String.format("MDSYS.SDO_CS.TRANSFORM(%s,%s)",bufferGeom,super.getSRID());
		}
		else if ( this.isBuffered()) {
		  String lengthUnits  = super.getSpatialView().getDistanceUnitType();
		  String bufferParams = ((super.getSRID().equals(Constants.NULL) || 
			                Strings.isEmpty(lengthUnits)) 
		                 ? "" 
		                 : ",'unit=" + lengthUnits + "'");
		  searchGeom = String.format("MDSYS.SDO_GEOM.SDO_BUFFER(?,?,?%s)",bufferParams);        	
		}
		else if ( this.isProjected() ) {
	      searchGeom = String.format("MDSYS.SDO_CS.TRANSFORM(?,%s)",super.getSRID());        	
		} else {
		  searchGeom = "?";
		}        	
		
		// Base CTE
		//
		sql = "WITH sGeom AS (\n" +
		"  SELECT " + searchGeom + " AS geom FROM DUAL\n" +
		")\n";
		
		// Build rest of SQL
		//
		String columns = "";
		try {
		columns = Queries.getColumns(super.getConnection(),
		                     super.getSchemaName(),
		                     super.getObjectName(),
		                     true /* _supportedDataTypes */ );
		} catch (Exception e) {
		LOGGER.error("SVQueryLayer GetColumns " + e.getMessage());
		return null;
		}
		
		String geoColumn = "";
		if ( this.isProjected() )
		// Project actual geometry column data to view srid
		geoColumn = String.format("MDSYS.SDO_CS.TRANSFORM(t.%s,%s)",super.getGeoColumn(),super.spatialView.getSRID());
		else
		geoColumn = "t." + super.getGeoColumn();
		
		sql += "SELECT f.* \n" +
		"  FROM (SELECT " + (Strings.isEmpty(columns) ? "" : columns+"," ) + 
		             geoColumn + " as " + super.getGeoColumn() + " \n" +
		     "  FROM sGeom a,\n" + 
		     "       " + this.getFullObjectName() + " t \n";
		
		String searchPredicate = "";
		if ( this.hasIndex() ) {
		searchPredicate = String.format("SDO_%s(t.%s,%s%s) = 'TRUE'",
		                   this.sdoOperator.toString(),
		                   super.getGeoColumn(),
		                   "a.geom",  // Note: Is in units of getGeoColumn 
		                   (this.sdoOperator==Constants.SDO_OPERATORS.RELATE 
		                    ? ( ",'" + this.getRelationshipMask() + "'" ) 
		                    : "")
		                   );
		} else {
		searchPredicate = "t." + super.getGeoColumn() + " IS NOT NULL\n" +
		       " AND t." + super.getGeoColumn() + ".SDO_GTYPE is not null\n" +
		       " AND MDSYS.SDO_GEOM.VALIDATE_GEOMETRY(t." + super.getGeoColumn() + "," + this.getTolerance() + ") = 'TRUE'\n" +
		       " AND MDSYS.SDO_GEOM.RELATE(t." + super.getGeoColumn() + ",'ANYINTERACT',a.geom,"+this.getTolerance()+") = 'TRUE'";
		}
		
		sql += " WHERE " + searchPredicate + "\n" +
		") f";
		
		LOGGER.logSQL(sql);
				
		return sql;
	}

    public void setSQL(String _layerSQL) {
        if ( Strings.isEmpty(_layerSQL))
        	this.sql = this.getInitSQL();
        else
        	this.sql = _layerSQL;
    }

    public void clearCache() {
        if (this.cache!=null && this.cache.size()>0 )
            this.cache.clear();
    }
    
    public void add(QueryRow _qr) {
        this.cache.add(_qr);
    }

    public void add(List<QueryRow> _geoSet) {
        this.cache = new ArrayList<QueryRow>(_geoSet);
    }

    public Envelope getLayerMBR() 
    {
        return new Envelope(this.mbr);
    }

	public boolean setLayerMBR(Envelope _defaultMBR, 
                                    int _targetSRID) 
	{
		return this.setLayerMBR(_defaultMBR);
	}

    public boolean setLayerMBR(Envelope _mbr) {
        if (_mbr == null) {
            return false;
        }
        if ( _mbr.getWidth()==0 || _mbr.getHeight()==0 ) {
            double halfSide = Math.max(_mbr.getWidth(),_mbr.getHeight()) / 2.0;
            if ( halfSide == 0.0 ) halfSide = 0.05;
            this.mbr = new Envelope(_mbr.centre().getX() - halfSide,
                                           _mbr.centre().getY() - halfSide,
                                           _mbr.centre().getX() + halfSide,
                                           _mbr.centre().getY() + halfSide);
        } else {
            this.mbr = new Envelope(_mbr);
        }
        return true;
    }

    public void setLayerMBR() 
    {
        Envelope lMBR = new Envelope(this.cache);
        if (lMBR.getWidth()==0 && lMBR.getHeight()==0 && this.getSpatialView()!=null ) {
            // Must be a single point
            // 
            Point2D.Double pixelSize;
            try {
                pixelSize = this.getSpatialView().getSVPanel().getMapPanel().getPixelSize();
                if ( pixelSize == null || Double.isNaN(pixelSize.getX()) ||  Double.isNaN(pixelSize.getY()) )  {
                    pixelSize = new Point2D.Double(0.05,0.05);
                }
                oracle.ide.config.Preferences prefs = oracle.ide.config.Preferences.getPreferences();
                double maxBufferSize = Math.max(pixelSize.getX(),
                                                pixelSize.getY()) * 
                                       Preferences.getInstance(prefs).getSearchPixels();
                lMBR.setChange(maxBufferSize);
            } catch (NoninvertibleTransformException e) {
                LOGGER.warn("Problem setting layerMBR of graphic theme: " + this.getLayerName());
            }
        }
        this.mbr.setMBR(new Envelope(lMBR));
    }

    public List<QueryRow> getCache() {
        return this.cache;
    }

    public long getCacheCount() {
        return this.cache==null?0:this.cache.size();
    }

	protected PreparedStatement setParameters(String _sql) 
	{
		Connection              conn = null;
		PreparedStatement pStatement = null;
		
		int stmtParamIndex  = 1;
		String params       = "";
		Struct stSearchGeom = null;
		
		try {
			conn = super.getConnection();

            pStatement = super.getConnection().prepareStatement(_sql);
	        
			stSearchGeom = JGeom.toStruct(this.getGeometry(), conn);
			pStatement.setObject(stmtParamIndex++, stSearchGeom, java.sql.Types.STRUCT);
			params = "? - " + RenderTool.renderGeometryAsPlainText(this.getGeometry(), Constants.TAG_MDSYS_SDO_GEOMETRY, Constants.bracketType.NONE, 12);
	        if (this.isBuffered() ) 
	        {
	            // String.format("MDSYS.SDO_GEOM.SDO_BUFFER(?,?,?,unit='....')");
				pStatement.setDouble(stmtParamIndex++, this.getBufferDistance());
				params += "? - " + String.valueOf(this.getBufferDistance());
				pStatement.setDouble(stmtParamIndex++, this.getTolerance());
				params += "? - " + String.valueOf(this.getTolerance());
	        } 

		} catch (Exception e) {
			LOGGER.error("SVQueryLayer.setParameters(" + e.getMessage() + ") - SQL copied to clipboard.");
			Tools.doClipboardCopy(_sql + params);
			return null;
		}
		return pStatement;
	}

    public void setCache()
    {
        Connection conn = null;
        try {
            // Make sure layer's connection has not been lost
            conn = super.getConnection();
            if ( conn == null ) {
                return;
            }
        } catch (IllegalStateException ise) {
            // Thrown by super.getConnection() indicates starting up, so we do nothing
            return;
        }
        
        // get SQL creating 
        //
        String qSql = this.getSQL();
        if (Strings.isEmpty(qSql)) {
            return;
        }

        // Set up parameters for host layer 
        //
        PreparedStatement pStatement = null;
        pStatement = this.setParameters(qSql);
        if (pStatement==null) {
            return;
        }

        // ****************** Execute the query ************************
        //
        Struct stGeom = null;
        ResultSet ors = null;
        String  rowID = null;
        try 
        {
            ors = (ResultSet)pStatement.executeQuery();
            ors.setFetchDirection(ResultSet.FETCH_FORWARD);
            ors.setFetchSize(this.getResultFetchSize());
            OracleResultSetMetaData rSetM = (OracleResultSetMetaData)ors.getMetaData(); // for column name

            String          value = "";
            String     columnName = "";
            String columnTypeName = "";
            
            this.cache = new ArrayList<QueryRow>();

            while ((ors.next()) &&
                   (this.getSpatialView().getSVPanel().isCancelOperation()==false)) 
            {
                LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>(rSetM.getColumnCount() - 1);
                for (int col = 1; col <= rSetM.getColumnCount(); col++) {
                    columnName     = rSetM.getColumnName(col);
                    columnTypeName = rSetM.getColumnTypeName(col);
                    
                    if (columnTypeName.equals("LONG")) {
                        // LONGs will kill the SQL stream and we can't use them anyway
                        LOGGER.warn("GeoRaptor ignored " + rSetM.getColumnName(col) + "/" + rSetM.getColumnTypeName(col));
                        continue;
                    }
                    
                    if (columnName.equalsIgnoreCase(super.getGeoColumn())) {
                        stGeom = (java.sql.Struct)ors.getObject(col);
                        if (ors.wasNull()) {
                            break; // process next row
                        }
                    } else {
                          if ( Tools.dataTypeIsSupported(columnTypeName) )
                          {
                              try
                              {
                                  ors.getObject(col);
                                  if (ors.wasNull()) {
                                      value = "NULL";
                                  } else {
                                      if ( ors.getMetaData().getColumnType(col) == OracleTypes.ROWID ) {
                                    	  continue;
                                      } else {
                                          value = SQLConversionTools.convertToString(this.getConnection(),ors,col);                                          
                                          if (value == null)
                                              value = "NULL";
                                      }
                                  }
                              } catch (Exception e) {
                                  value = "NULL";
                              }
                              attributes.put(rSetM.getColumnName(col), value);
                          }
                    }
                }
                this.cache.add(new QueryRow(rowID, attributes, stGeom));
            }
            try { ors.close();        } catch (Exception _e) { _e.printStackTrace(); }
            try { pStatement.close(); } catch (Exception _e) { _e.printStackTrace(); }
            
            this.setNumberOfFeatures();
            if ( this.getPreferences().isLogSearchStats() ) {
                LOGGER.info(super.getSpatialView() + "." + this.layerName + 
                            " Total Features Returned = " + this.getNumberOfFeatures());
            }
            
        } catch (SQLException sqle) {
            // isView() then say no index
            if ( this.isView() ) {
                this.setIndex(false);
            }
            Tools.copyToClipboard(super.propertyManager.getMsg("SQL_QUERY_ERROR",
                                                               sqle.getMessage()),
                                  qSql);
        } catch (NullPointerException npe) {
            if ( conn != null ) { // Assume conn==null is a startup problem
                if ( this.getPreferences().isLogSearchStats() ) {
                    LOGGER.error("SVQueryLayer.getCache error: " + npe.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("SVGraphicLayer.setCache - general exception");
            e.printStackTrace();
        } finally {
            try { ors.close();        } catch (Exception _e) { }
            try { pStatement.close(); } catch (Exception _e) { }
        }
    }

    public void setNumberOfFeatures(long _number) 
    {
    	// Do nothing as feature count is determined by cache.
    	this.setNumberOfFeatures();
    }

    // 
    public void setNumberOfFeatures() 
    {
    	// Set visible display of features
    	long numberOfFeatures = this.getNumberOfFeatures();
        try 
        {
        	if (this.getPreferences().isNumberOfFeaturesVisible() ) {
        		this.getSpatialView().getSVPanel().getViewLayerTree().refreshLayerCount(this,numberOfFeatures);
            } else {
        	    this.getSpatialView().getSVPanel().getViewLayerTree().removeLayerCount(this);
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    public long getNumberOfFeatures() {
        return this.cache==null?0:this.cache.size();
    }

    public ArrayList<QueryRow> getAttributes() {
        if (this.cache.size() == 0) {
            return (ArrayList<QueryRow>)null;
        }
        // list of return rows
        //
        ArrayList<QueryRow> retList = new ArrayList<QueryRow>(); 
        JGeometry geo = null;
        for (QueryRow qrow : this.cache) {
            try {
                if (qrow.getGeoValue() == null) {
                    continue;
                }
                geo = JGeometry.loadJS(qrow.getGeoValue());
                if (geo == null) {
                    continue;
                }
                if (!Strings.isEmpty(qrow.getGeoConstructor())) {
                    retList.add(qrow);
                } else {
                    retList.add(new QueryRow(qrow.getRowID(),
                                             qrow.getAttrHashMap(),
                                             qrow.getGeoValue()));
                }
            } catch (SQLException e) {
                continue;
            }
        }
        return retList;
    }
    
    /**
     * Execute SQL and draw data on given graphical device
     * @param _mbr MBR coordinates
     * @param _g2 graphical device
     * @return if return false, something was wrong (for example, Connection with DB faild)
     */
    @Override
    public boolean drawLayer(Envelope _mbr, Graphics2D _g2) 
    {
        LOGGER.debug("drawing graphic layer " + this.getLayerName());
        Connection conn = null;
        try {
            // Make sure layer's connection has not been lost
            conn = this.getConnection();
            if ( conn == null ) {
                return false;
            }
        } catch (IllegalStateException ise) {
            // Thrown by this.conn indicates starting up, so we do nothing
            return false;
        }

        try {
            // Nothing to do if cache is empty
            //
            if (this.cache==null || this.cache.size() == 0) {
                LOGGER.warn(this.getVisibleName() + " contains no geometries to draw.");
                return false;
            }

            // Done for readability only
            Map<String, Object> attData = this.cache.get(0).getAttData();
            
            // Check if labeling is to occur
            String label = Strings.isEmpty(this.getStyling().getLabelColumn()) 
                           ? null 
                           : this.getStyling().getLabelColumn();            
            boolean bLabel = false;
            if (this.cache.get(0).getColumnCount() == 0 ||
                attData == null || Strings.isEmpty(label)) {
                bLabel = false;
            } else {
                if (attData.containsKey(label)) {
                    bLabel = true;
                }
            }
            String      shadeCol =
                Strings.isEmpty(this.getStyling().getShadeColumn())     ?"":this.getStyling().getShadeColumn();
            String pointColorCol =
                Strings.isEmpty(this.getStyling().getPointColorColumn())?"":this.getStyling().getPointColorColumn();
            String  lineColorCol =
                Strings.isEmpty(this.getStyling().getLineColorColumn()) ?"":this.getStyling().getLineColorColumn();
            String  pointSizeCol =
                Strings.isEmpty(this.getStyling().getPointSizeColumn()) ?"":this.getStyling().getPointSizeColumn();
            boolean bShade      = false,
                    bPointColor = false,
                    bLineColor  = false,
                    bPointSize  = false;
            if ( attData != null ) {
                bShade      = (this.getStyling().getShadeType()      == Styling.STYLING_TYPE.COLUMN && attData.containsKey(shadeCol));
                bPointColor = (this.getStyling().getPointColorType() == Styling.STYLING_TYPE.COLUMN && attData.containsKey(pointColorCol));
                bLineColor  = (this.getStyling().getLineColorType()  == Styling.STYLING_TYPE.COLUMN && attData.containsKey(lineColorCol));
                bPointSize  = (this.getStyling().getPointSizeType()  == Styling.STYLING_TYPE.COLUMN && attData.containsKey(pointSizeCol));                
            }
            boolean bRotate = (Strings.isEmpty(this.getStyling().getRotationColumn()) ? false : true); 

            // Set graphics2D once for all features
            drawTools.setGraphics2D(_g2);

            // Now process and draw all JGeometry objects in the cache.
            //
            JGeometry geo = null;
            QueryRow qrow = null;
            Iterator<QueryRow> iter = this.cache.iterator();
            while (iter.hasNext()) 
            {
                qrow = iter.next();
                if ( qrow == null ) {
                    continue;
                }
                geo = qrow.getJGeom();
                if (geo == null) {
                    LOGGER.warn("Null Graphic layer JGeometry found when drawing and will be skipped.");
                    continue;
                }
                // Display geometry only if overlaps display MBR
                if ((_mbr.isSet() && _mbr.overlaps(JGeom.getGeoMBR(geo))) || _mbr.isNull()) 
                {
                    // Draw the geometry using current display settings
                    String   sLabelText = (bLabel      ? SQLConversionTools.convertToString(conn,label,         qrow.getAttData().get(label)) : "");
                    Color   cShadeValue = Colours.fromRGBa((bShade      ? SQLConversionTools.convertToString(conn,shadeCol,      qrow.getAttData().get(shadeCol)) : "0,0,0,255")); 
                    Color   cPointValue = Colours.fromRGBa((bPointColor ? SQLConversionTools.convertToString(conn,pointColorCol, qrow.getAttData().get(pointColorCol)) : "0,0,0,255")); 
                    Color    cLineValue = Colours.fromRGBa((bLineColor  ? SQLConversionTools.convertToString(conn,lineColorCol,  qrow.getAttData().get(lineColorCol)) : "0,0,0,255"));
                    int iPointSizeValue = (bPointSize  ? MathUtils.numberToInt(qrow.getAttData().get(pointSizeCol),4) : 4 );
                    double dRotateAngle = (bRotate     ? (this.getStyling().getRotationValue() == Constants.ROTATION_VALUES.DEGREES 
                                                          ? COGO.radians(COGO.normalizeDegrees(((NUMBER)qrow.getAttData().get(this.getStyling().getRotationColumn())).doubleValue() - 90.0f)) 
                                                          : ((NUMBER)qrow.getAttData().get(this.getStyling().getRotationColumn())).doubleValue()) 
                                                       : 0.0f);
                    this.drawTools.callDrawFunction(
                            (JGeometry)geo,
                            (Styling)  this.styling,
                            (String)   sLabelText,
                            (Color)    cShadeValue,
                            (Color)    cPointValue,
                            (Color)    cLineValue,
                            (int)      iPointSizeValue,
                            (double)   dRotateAngle
                     );

                }
            }
            if ( this.calculateMBR ) {
                this.setLayerMBR();
            }
            this.setNumberOfFeatures();
            LOGGER.debug("GraphicLayer.drawLayer end");
        } catch (Exception e) {
            LOGGER.warn(this.getClass().getName()+".drawLayer: error - " + e.toString());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public LinkedHashMap<String, String> getColumnsAndTypes(boolean _onlyNumbersDatesAndStrings,
                                                            boolean _fullDataType) 
    throws SQLException 
    {
        // Only dates and numbers is ignored by this theme
        LinkedHashMap<String, String> returnSet =
            new LinkedHashMap<String, String>();
        if (this.getCache() == null || this.getCacheCount() == 0)
            return null;
        if (this.getCache().get(0).getAttData() == null ||
            this.getCache().get(0).getAttData().size() == 0)
            return null;
        // Only need to process one row to get column names
        QueryRow qrow = this.getCache().get(0);
        Iterator<?> it = qrow.getAttData().keySet().iterator();
        String key = null;
        String obj = null;
        String validDataTypes =
            "NUMBER,FLOAT,ROWID,CLOB,CHAR,DATE,TIMESTAMP,TIMESTAMPTZ,TIMESTAMPLTZ,INTERVALDS,INTERVALYM,BINARY_DOUBLE,BINARY_FLOAT,STRING,BIGDECIMAL,INTEGER,SHORT,LONG,DOUBLE";
        while (it.hasNext()) {
            key = (String)it.next();
            obj = Tools.dataTypeAsString(qrow.getAttData().get(key));
            if (_onlyNumbersDatesAndStrings) {
                if (validDataTypes.contains(obj)) {
                    returnSet.put(key, obj);
                }
            } else {
                returnSet.put(key, obj);
            }

        }
        return new LinkedHashMap<String, String>(returnSet);
    }

    @Override
    public ArrayList<QueryRow> queryByPoint(Point2D _worldPoint,
                                                int _numSearchPixels) 
    {
        if (this.cache.size() == 0) {
            return (ArrayList<QueryRow>)null;
        }
        
        // list of return rows
        //
        ArrayList<QueryRow> retList = new ArrayList<QueryRow>(); 
        
        Envelope mbr = new Envelope(this.getDefaultPrecision());

        int numPixels = _numSearchPixels == 0 ? 3 : _numSearchPixels;
        try {
            // Get search radius
            // Is diagonal distance computed as sqrt of size of pixel (* number of pixels in search)
            // in X and Y squared (ie pythagoras)
            //
            Point.Double pixelSize = this.spatialView.getMapPanel().getPixelSize();
            mbr.setMBR(_worldPoint.getX() - pixelSize.getX() * numPixels,
                       _worldPoint.getY() - pixelSize.getY() * numPixels,
                       _worldPoint.getX() + pixelSize.getX() * numPixels,
                       _worldPoint.getY() + pixelSize.getY() * numPixels);

            JGeometry geo = null;
            for (QueryRow qrow : this.cache) {
                try {
                    if (qrow.getGeoValue() == null) {
                        continue;
                    }
                    geo = JGeometry.loadJS(qrow.getGeoValue());
                    if (geo == null) {
                        continue;
                    }
                    // TOBEDONE: Use JTS to implement Intersects
                    // Surrogate search: if MBR of feature contains search point + search distance MBR then return it
                    if (!mbr.isSet() || // No filter applied
                        // OR
                        mbr.overlaps(SDO_GEOMETRY.getGeoMBR(qrow.getGeoValue()))) // geometry overlaps display MBR
                    {
                        if (!Strings.isEmpty(qrow.getGeoConstructor())) {
                            retList.add(qrow);
                        } else {
                            retList.add(new QueryRow(qrow.getRowID(),
                                                     qrow.getAttrHashMap(),
                                                     qrow.getGeoValue()));
                        }
                    }
                } catch (SQLException e) {
                    continue;
                }
            }
        } catch (NoninvertibleTransformException nte) {
            JOptionPane.showMessageDialog(null,
                                          this.getSpatialView().getSVPanel().getMapPanel().getPropertyManager().getMsg("ERROR_SCREEN2WORLD_TRANSFORM")+ "\n" +
                                          nte.getLocalizedMessage(),
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.ERROR_MESSAGE);
        }
        return retList;
    }

	@Override
	public Styling getStyling() {
		return this.styling;
	}

	@Override
	public void setStyling(Styling _styling) {
		this.styling = new Styling(_styling);
	}

	@Override
	public String getStylingAsString() {
		return this.styling.toString();
	}

	@Override
	public Preferences getPreferences() {
		return this.preferences;
	}

	@Override
	public void savePropertiesToDisk() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getLayerNameAndConnectionName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLayerName() {
		// TODO Auto-generated method stub
		return this.layerName;
	}

	@Override
	public void setLayerName(String _layerName) {
		this.layerName = _layerName;
	}

	@Override
	public String getDesc() {
		return this.desc;
	}

	@Override
	public void setDesc(String _desc) {
		this.desc = _desc;
	}

	@Override
	public String getVisibleName() {
		return this.visibleName;
	}

	@Override
	public void setVisibleName(String _visibleName) {
		this.visibleName = _visibleName;
	}

	@Override
	public boolean isDraw() {
		return this.draw;
	}

	@Override
	public void setDraw(boolean _draw) {
		this.draw = _draw;
	}

	@Override
	public void setObjectType(String _objectType) {
		// Nothing to do as graphic theme is not table/view/mview		
	}

	@Override
	public boolean isView() {
		return false;  // Object type is not a view
	}

	@Override
	public String getInitSQL(String _geoColumn) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSDOFilterClause() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String wrapSQL(String _sql) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLayerSQL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMBRRecalculation(boolean _recalc) {
        this.calculateMBR = _recalc;
	}

	@Override
	public boolean getMBRRecalculation() {
        return this.calculateMBR;
	}


    public String getRelationshipMask() {
        return this.relationshipMask;
    }
    
    public void setRelationshipMask(String _relationshipMask) {
        this.relationshipMask = Strings.isEmpty(_relationshipMask)?"ANYINTERACT":_relationshipMask;
    }

    public void setBufferDistance(double _bufferDistance) {
        this.bufferDistance = _bufferDistance;
    }
    
    public void setBufferDistance(String _bufferDistance) {
        try 
        {
            this.bufferDistance = Double.valueOf(_bufferDistance).doubleValue();
        } catch (Exception e) {
            this.bufferDistance = 0.0;
        }
    }

    public double getBufferDistance() {
        return this.bufferDistance;
    }
    
    public void setBuffered(boolean _buffered) {
        this.buffered = _buffered;
    }
    
    public void setBuffered(String _buffered) {
        try 
        {
            this.buffered = Boolean.valueOf(_buffered);
        } catch (Exception e) {
            this.buffered = false;
        }
    }
    
    @Override
    public void setMinResolution(boolean _minResolution) {
        if ( this.geometryType == Constants.GEOMETRY_TYPES.POINT )
            this.minResolution = false;
        else
            this.minResolution = _minResolution;
    }
  
    @Override
    public boolean getMinResolution() {
        return ( this.geometryType == Constants.GEOMETRY_TYPES.POINT ) ? false : this.minResolution;
    }
  
    @Override
    public int getPrecision(boolean _calculate) {
        return super.getPrecision(_calculate);
    }

    @Override
    public double getTolerance() {
        return super.getTolerance();
    }

    @Override
    public void setResultFetchSize(int _resultFetchSize) {
        this.resultFetchSize = _resultFetchSize;
    }
  
    @Override
    public int getResultFetchSize() {
        return this.resultFetchSize;
    }
    
    @Override
    public SVSpatialLayerDraw getDrawTools() {
      return this.drawTools;
    }

    @Override
    public SpatialView getView() {
      return super.getSpatialView();
    }

    @Override
    public boolean getProject() {
        return this.project;
    }
  
    @Override
    public void setProject(boolean _yes, boolean _calcMBR) {
        this.project = _yes;
        if (_calcMBR) {
            // Anytime this is called we need to recalculate the MBR of the layer
            this.setLayerMBR(super.mbr, super.getSRIDAsInteger());
        }
    }
    
    @Override
    public void setFetchSize(int _fetchSize) {
        if (_fetchSize == 0)
            this.resultFetchSize = this.getPreferences().getFetchSize();
        else
            this.resultFetchSize = _fetchSize;
    }

    @Override
    public int getFetchSize() {
        return this.resultFetchSize;
    }

	@Override
	public PropertiesManager getPropertyManager() {
	  return this.propertyManager;
	}

    public void setGeometry(JGeometry _jGeom) {
        this.queryGeometry = _jGeom;
    }
    
    public JGeometry getGeometry() {
        return this.queryGeometry;
    }

    public void setSdoOperator(Constants.SDO_OPERATORS _sdoOperator) {
        this.sdoOperator = _sdoOperator;
    }

    public void setSdoOperator(String _sdoOperator) {
        try {
            this.sdoOperator = Constants.SDO_OPERATORS.valueOf(_sdoOperator);
        } catch (Exception e) {
            this.sdoOperator = Constants.SDO_OPERATORS.ANYINTERACT;
        }  
    }
    
    public Constants.SDO_OPERATORS getSdoOperator() {
        return this.sdoOperator;
    }

}
