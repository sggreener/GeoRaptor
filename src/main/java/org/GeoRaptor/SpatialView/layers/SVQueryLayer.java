package org.GeoRaptor.SpatialView.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.LineStyle;
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
import org.apache.commons.codec.binary.Base64;
import org.geotools.util.logging.Logger;
import org.w3c.dom.Node;

import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;
import oracle.spatial.geometry.JGeometry;

public class SVQueryLayer 
   extends SVLayer
implements iLayer
{
    public  static final String CLASS_NAME = Constants.KEY_SVQueryLayer;

    private static final Logger     LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVQueryLayer");

    protected Preferences      preferences = null;

    protected String           layerName   = "";         // Unique layer name. This is the universal unique name for this layer.
    
    protected String           visibleName = "";         // This text is write into JTree where we show layers

	protected String             keyColumn = "";         // Unique key column		

    protected Styling              styling = new Styling();

    protected SVSpatialLayerDraw drawTools = new SVSpatialLayerDraw(this); // Class used to draw this layer 

    protected Constants.SDO_OPERATORS 
                               sdoOperator = Constants.SDO_OPERATORS.ANYINTERACT;
    
    protected String layerCopySuffix = " (COPY)";  // Should be Tools>Preferences>GeoRaptor

    protected    String                sql = "";

    protected int          resultFetchSize = 100;        // Fetch size for ResultSet

    protected       int          precision = 2;
    
    protected JGeometry      queryGeometry = null;
    
    protected    String   relationshipMask = "";
    
    protected    double     bufferDistance = 0.0;
    
    protected   boolean           buffered = false;
    
    protected   boolean       drawGeometry = false;

    protected Constants.OBJECT_TYPES 
                                objectType = Constants.OBJECT_TYPES.TABLE;

    protected boolean          indexExists = true;       // Does spatial index exist for this layer

    private final String sdoFilterMinResClause = "min_resolution=%f,querytype=WINDOW";

    public SVQueryLayer(SpatialView   _sView, 
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
        this.setResultFetchSize(preferences.getFetchSize());
        this.setPrecision(-1); // force calculation from mbr
    	this.styling = new Styling();
    }
    
    public SVQueryLayer(SpatialView _sView, Node _node) {
      super(_sView, _node);
      this.fromXML(_node);
      this.drawGeometry = this.getPreferences().isDrawQueryGeometry();
    }

    /** Copy constructors
    *
    **/
    public SVQueryLayer(SVQueryLayer _qLayer) 
    {
        super(_qLayer.getSpatialView(),
              _qLayer.getMetadataEntry());
        this.setLayerName(_qLayer.getLayerName());
        if ( _qLayer.getSpatialView() != null ) {
          this.setLayerName(_qLayer.getSpatialView().checkLayerName(this.getLayerName()));
        }
        this.setVisibleName(_qLayer.getVisibleName());
        this.setDesc(_qLayer.getDesc());
        this.setDraw(_qLayer.isDraw());
        this.setSQL(_qLayer.getSQL());
        
        this.setStyling(_qLayer.getStyling());
        // This is an informational query of actual data: filtering would make the result invalid
        this.setMinResolution(false);
            
        this.setBuffered(_qLayer.getBufferDistance()!=0.0);
        this.setBufferDistance(_qLayer.getBufferDistance());
        this.setDrawQueryGeometry(_qLayer.isDrawQueryGeometry());
        this.setGeometry(_qLayer.getGeometry());
        this.setRelationshipMask(_qLayer.getRelationshipMask());
        this.setSdoOperator(_qLayer.getSdoOperator());            
        this.drawGeometry = _qLayer.getPreferences().isDrawQueryGeometry();        
    }

    public SVQueryLayer(iLayer _sLayer) 
    {
        super(_sLayer.getView(), 
              _sLayer.getMetadataEntry());
        
        this.setLayerName(_sLayer.getLayerName());
        if ( _sLayer.getSpatialView() != null ) {
          this.setLayerName(_sLayer.getSpatialView().checkLayerName(this.getLayerName()));
        }
        this.setVisibleName(_sLayer.getVisibleName());
        this.setDesc(_sLayer.getDesc());
        this.setDraw(_sLayer.isDraw());
        this.setSQL(_sLayer.getSQL());
        
        this.setStyling(_sLayer.getStyling());
        this.styling.setShadeType(Styling.STYLING_TYPE.NONE);
        this.styling.setTextHiScale(null); // Set to max scale
        // This is an informational query of actual data: filtering would make the result invalid
        this.setMinResolution(false);
        
        if ( _sLayer instanceof SVQueryLayer ) 
        {
            this.setBuffered(((SVQueryLayer)_sLayer).getBufferDistance()!=0.0);
            this.setBufferDistance(((SVQueryLayer)_sLayer).getBufferDistance());
            this.setDrawQueryGeometry(((SVQueryLayer)_sLayer).isDrawQueryGeometry());
            this.setGeometry(((SVQueryLayer)_sLayer).getGeometry());
            this.setRelationshipMask(((SVQueryLayer)_sLayer).getRelationshipMask());
            this.setSdoOperator(((SVQueryLayer)_sLayer).getSdoOperator());            
        }
	}

	public SVQueryLayer createCopy(boolean _renderOnly) 
    {
        // _renderOnly is ignored for SVQueryLayers
        //
        return new SVQueryLayer(this);
    }

	// *************************************************************
	
	@Override
	public PropertiesManager getPropertyManager() {
		return super.propertyManager;
	}
	
    @Override
    public String getClassName() {
        return SVQueryLayer.CLASS_NAME;
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

    private boolean isProjected() {
        boolean project = super.spatialView.getSRID().equals(Constants.NULL) == false && 
                          super.getSRID().equals(Constants.NULL) == false;
		return project;
    }
    
    public void setSQL(String _layerSQL) {
        if ( Strings.isEmpty(_layerSQL))
        	this.sql = this.generateSQL(this.isBuffered(),this.isProjected());
        else
        	this.sql = _layerSQL;
    }

    public String getSQL() {
    	if (Strings.isEmpty(this.sql)) {
            this.setSQL(this.generateSQL(this.isBuffered(),this.isProjected()));
    	} 
    	return this.sql;
    }

	@Override
	public String getInitSQL(String _geoColumn) {
		return this.getSQL();
	}

	@Override
	public void setInitSQL() {
        this.setSQL(this.generateSQL(this.isBuffered(),this.isProjected()));
	}
    
    public String generateSQL(boolean _buffered,
    		                  boolean _project)
    {
    	String sql = "";
        // Get search predicate
        //
        String searchGeom = "",
               bufferGeom = "";
        
        // If filter geometry buffered could need projecting to stored geometry 
        // We transform the search geometry (eg draw optimized rectangle) to the SRID of the
        // to-be-searched table/column as the search geometry will be in the SRID units of the view.
        //
        if (_buffered && _project) 
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
        else if ( _buffered ) {
            String lengthUnits  = super.getSpatialView().getDistanceUnitType();
            String bufferParams = ((super.getSRID().equals(Constants.NULL) || 
            		                Strings.isEmpty(lengthUnits)) 
                                   ? "" 
                                   : ",'unit=" + lengthUnits + "'");
            searchGeom = String.format("MDSYS.SDO_GEOM.SDO_BUFFER(?,?,?%s)",bufferParams);        	
        }
        else if ( _project ) {
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
        if ( _project )
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
                                      ? ( ",'" + getRelationshipMask() + "'" ) 
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

    public ArrayList<QueryRow> queryByPoint(Point2D _worldPoint, 
                                            int     _numSearchPixels) 
    {

        // Take simple approach:
    	// 1. Execute normal - buffer search point - SQL
    	// 2. Filter each resulting geometry by distance to search point 
        // 3. Filtering can occur in srid layer or SpatialView
        //
        ArrayList<QueryRow> queryList = new ArrayList<QueryRow>(); // list of return rows
        
        String sql = null;

        // Since we force a search based on a buffered search point
        // We force buffer mode for SQL generation
        //
    	boolean saveBuffered       = this.isBuffered();
    	double  saveBufferDistance = this.getBufferDistance();
    	
        Connection conn = null;
        conn = this.getConnection();

        PreparedStatement pStatement = null;

		String params          = "";
        try {
			int stmtParamIndex     = 1;
        	int viewSrid = this.getView().getSRIDAsInteger();
			
	        // Get searchPoint from click on screen and compute search distance using searchPixels
        	//
	        double     sdoDistance = this.getSearchRadius(_numSearchPixels);	        
	        Point2D  searchPoint2D = new Point2D.Double(_worldPoint.getX(),_worldPoint.getY());	        
	        Struct     searchPoint = Queries.projectSdoPoint(conn,searchPoint2D,viewSrid,this.spatialView.getSRIDAsInteger());
	        
	        // Units of measure if srid of layer different from view
	        //
	        String units_parameter = String.format("unit=%s",Tools.getViewUnits(spatialView,Constants.MEASURE.LENGTH));

	        // Force buffering of search geometry
	        //
			this.setBufferDistance(sdoDistance);
            this.setBuffered(true);

            // Generate SQL
            //
            sql = this.generateSQL(this.isBuffered(),this.isProjected());
            if ( this.getSRIDAsInteger() == Constants.SRID_NULL)
              sql += "\n WHERE MDSYS.SDO_GEOM.SDO_DISTANCE(f.GEOM,?/*DistancePoint*/,?/*tol*/) <= ?";
            else
              sql += "\n WHERE MDSYS.SDO_GEOM.SDO_DISTANCE(f.GEOM,?/*DistancePoint*/,?/*tol*/,?/*unit*/) <= ?";

            // Prepare SQL ready for parameter assignment
            //
            pStatement = super.getConnection().prepareStatement(sql);

        	params = "";
            if (this.isBuffered() && this.isProjected()) 
            {
                /**
                 * MDSYS.SDO_GEOM.SDO_BUFFER(
                 *    MDSYS.SDO_CS.TRANSFORM(? geom IN SDO_GEOMETRY, ? Srid ),
                 *    ? dist IN NUMBER,
                 *    ? tol IN NUMBER
                 *    [, ? params IN VARCHAR2]
                 * ) RETURN SDO_GEOMETRY;
                **/
            	pStatement.setObject(stmtParamIndex++, searchPoint,java.sql.Types.STRUCT);
        	    params  = String.format("? %s\n", SDO_GEOMETRY.getGeometryAsString(searchPoint));
        	    
            	pStatement.setInt(stmtParamIndex++, viewSrid );
        	    params += String.format("? %s\n",  String.valueOf(viewSrid));
        	    
            	pStatement.setDouble(stmtParamIndex++, sdoDistance);
        	    params += String.format("? %s\n",  String.valueOf(sdoDistance));
        	    
        	    pStatement.setDouble(stmtParamIndex++, this.getTolerance());
        	    params += String.format("? %s\n",String.valueOf(this.getTolerance()));
        	    
                if ( this.getSRIDAsInteger() != Constants.SRID_NULL) {
                  pStatement.setString(stmtParamIndex++, units_parameter);
        	      params += String.format("? '%s'\n",units_parameter);
                }
            }
            else if ( this.isBuffered() ) {
                /**
                 * MDSYS.SDO_GEOM.SDO_BUFFER(
                 *    ? geom IN SDO_GEOMETRY, 
                 *    ? dist IN NUMBER,
                 *    ? tol IN NUMBER
                 *    [, ? params IN VARCHAR2]
                 * ) RETURN SDO_GEOMETRY;
                **/
            	pStatement.setObject(stmtParamIndex++, searchPoint,java.sql.Types.STRUCT);  
        	    params  = String.format("? %s\n", SDO_GEOMETRY.getGeometryAsString(searchPoint));

            	pStatement.setDouble(stmtParamIndex++, sdoDistance);
        	    params += String.format("? %s\n",  String.valueOf(sdoDistance));
        	    
        	    pStatement.setDouble(stmtParamIndex++, this.getTolerance());
        	    params += String.format("? %s\n",String.valueOf(this.getTolerance()));
        	    
                if ( this.getSRIDAsInteger() != Constants.SRID_NULL) {
                  pStatement.setString(stmtParamIndex++, units_parameter);
        	      params += String.format("? '%s'\n",units_parameter);
                }
                        	
            }
            else if ( this.isProjected() ) {
                // MDSYS.SDO_CS.TRANSFORM(? geom IN SDO_GEOMETRY, ? Srid )
            	// Never get here because of forced buffering
            	pStatement.setObject(stmtParamIndex++, searchPoint,java.sql.Types.STRUCT);  
        	    params  = String.format("? %s\n", SDO_GEOMETRY.getGeometryAsString(searchPoint));
        	    pStatement.setInt(stmtParamIndex++, viewSrid);
        	    params += String.format("? %s\n",  String.valueOf(viewSrid));
            } else {
            	// WITH ... AS (SELECT ? as geom FROM DUAL)
            	// Never get here because of forced buffering
            	pStatement.setObject(stmtParamIndex++, searchPoint,java.sql.Types.STRUCT);  
        	    params  = String.format("? %s\n", SDO_GEOMETRY.getGeometryAsString(searchPoint));            	
            }

            // WHERE SDO_DISTANCE
	        pStatement.setObject(stmtParamIndex++, searchPoint, java.sql.Types.STRUCT);
	            params += String.format("? %s\n", SDO_GEOMETRY.getGeometryAsString(searchPoint));
	        pStatement.setDouble(stmtParamIndex++, this.getTolerance());
	            params += String.format("? %s\n",  String.valueOf(this.getTolerance()));
	        if ( this.getSRIDAsInteger() != Constants.SRID_NULL) {
              pStatement.setString(stmtParamIndex++, units_parameter);
	            params += String.format("? '%s'\n",units_parameter);
	        }
	        pStatement.setDouble(stmtParamIndex++, sdoDistance);
	            params += String.format("? %s\n",  String.valueOf(sdoDistance));
        } catch (Exception e) {
        	this.setBuffered(saveBuffered);
            this.setBufferDistance(saveBufferDistance);
        	return null;
        }
        
        this.setBuffered(saveBuffered);
        this.setBufferDistance(saveBufferDistance);
        
        LOGGER.logSQL(sql + "\n" + params);

        Struct retStruct = null;
        ResultSet    ors = null;
        try 
        {
            ors = pStatement.executeQuery();
            
            ors.setFetchDirection(ResultSet.FETCH_FORWARD);
            ors.setFetchSize(this.getResultFetchSize());

            OracleResultSetMetaData rSetM = (OracleResultSetMetaData)ors.getMetaData(); // for column name

            String          value = "";
            String     columnName = "";
            String columnTypeName = "";
            long    totalFeatures = 0;    
            while ((ors.next()) &&
                   (this.getSpatialView().getSVPanel().isCancelOperation()==false)) 
            {
                LinkedHashMap<String, Object> calValueMap = new LinkedHashMap<String, Object>(rSetM.getColumnCount() - 1);
                for (int col = 1; col <= rSetM.getColumnCount(); col++) {
                	
                    columnName     = rSetM.getColumnName(col);
                    columnTypeName = rSetM.getColumnTypeName(col);
                    
                    if (columnTypeName.equals("LONG")) {
                        // LONGs will kill the SQL stream and we can't use them anyway
                        LOGGER.warn("GeoRaptor ignored " + rSetM.getColumnName(col) + "/" + rSetM.getColumnTypeName(col));
                        continue;
                    }
                    
                    if (columnName.equalsIgnoreCase(super.getGeoColumn())) {
                        retStruct = (java.sql.Struct)ors.getObject(col);
                        if (ors.wasNull()) {
                            break; // process next row
                        }
                        totalFeatures++;
                    } else {
                          if ( Tools.dataTypeIsSupported(columnTypeName) )
                          {
                              try
                              {
                                  if (ors.wasNull()) {
                                      value = "NULL";
                                  } else {
                                      if ( ors.getMetaData().getColumnType(col) == OracleTypes.ROWID ) {
                                    	  continue;
                                      } else {
                                          value = SQLConversionTools.convertToString(conn,ors,col);                                          
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
                queryList.add(new QueryRow(null, calValueMap, retStruct));
            }
            ors.close();
            pStatement.close();

            if ( this.getPreferences().isLogSearchStats() ) {
                LOGGER.info(super.getSpatialView() + "." + this.layerName + 
                            " Total Features Returned = " + totalFeatures);
            }
            
        } catch (SQLException sqle) {
        }
        return queryList;        
    }
        
	@Override
    public boolean drawLayer(Envelope _mbr, Graphics2D _g2) 
    {
        LOGGER.debug("QueryLayer.drawLayer(" + _mbr.toString());
        Connection conn = null;
        try {
            // Also make sure layer's connection has not been lost
            conn = super.getConnection();
            if ( conn == null ) {
                return false;
            }
        } catch (IllegalStateException ise) {
            // Indicates SQL Developer is starting up, so we do nothing
            return false;
        }
        
        // Get SQL and set its parameters 
        //
        String                  qSql = this.getSQL();
        PreparedStatement pStatement = this.setParameters(qSql);

        if ( this.isDrawQueryGeometry() ) 
        {
            try 
            {
                JGeometry jGeom = this.getGeometry();
                if ( jGeom!=null) {
                    // Set graphics2D to draw query feature
                    this.drawTools.setGraphics2D(_g2);
                    // Save settings
                    LineStyle.LINE_STROKES ls = this.styling.getLineStrokeType();
                    int lw = this.styling.getLineWidth();
                    Styling.STYLING_TYPE st = this.styling.getShadeType();
                    float stl = this.styling.getShadeTransLevel();
                    
                    this.styling.setLineWidth(2);
                    this.styling.setLineStrokeType(LineStyle.LINE_STROKES.LINE_DASH);
                    this.styling.setShadeType(Styling.STYLING_TYPE.NONE);
                    // Draw original geometry before buffering
                    //
                    this.drawTools.callDrawFunction(
                            (JGeometry)jGeom,
                            (Styling)this.styling,
                            (String)null,(Color)null,(Color)null,(Color)null,
                            (int)4,
                            (double)0.0
                    );
                    if ( this.isBuffered()) {
                        this.styling.setLineWidth(lw); // set back
                        this.styling.setShadeType(this.getPreferences().isRandomRendering()
                        		                  ? Styling.STYLING_TYPE.RANDOM
                                                  : Styling.STYLING_TYPE.CONSTANT);
                        this.styling.setShadeTransLevel(0.5f);
                        this.styling.setLineStrokeType(LineStyle.LINE_STROKES.LINE_DOT);
                        try {
                            if (this.getSRIDType().toString().startsWith("GEO")) {
                                double lat = 0.0;
                                if ( jGeom.getFirstPoint().length >= 2) {
                                    lat = jGeom.getFirstPoint()[1];
                                }
                                jGeom = jGeom.buffer(COGO.meters2Degrees(this.getBufferDistance(),lat));
                                //Can't get this to work
                                // jGeom.buffer(COGO.meters2Degrees(this.getBufferDistance(),lat), 
                                //             COGO.WGS84_SEMI_MAJOR_AXIS, 
                                //             COGO.WGS84_INVERSE_FLATENNING, 
                                //             this.getTolerance()*20.0);
                            } else  {
                                jGeom = jGeom.buffer(this.getBufferDistance());
                            }
                            this.drawTools.callDrawFunction(
                                    (JGeometry)jGeom,
                                    (Styling)this.styling,
                                    (String)null,
                                    (Color)null,(Color)null,(Color)null,
                                    (int)4,
                                    (double)0.0
                         );

                        } catch (Exception e) {
                            LOGGER.error("Failed to buffer query geometry by " + this.getBufferDistance());
                            jGeom = this.getGeometry();
                        }
                    }
                    this.styling.setShadeTransLevel(stl);
                    this.styling.setLineStrokeType(ls);
                    this.styling.setShadeType(st);
                }
            } catch (IOException e) {
                LOGGER.error(super.propertyManager.getMsg("ERROR_DISPLAY_QUERY_GEOM",e.getMessage()));
            }
        }

        LOGGER.logSQL(qSql);

        // Now execute query
        //
        return SVDrawQueries.executeQuery(
        		this,
        		pStatement,
        		qSql,
        		_g2,
        		this.drawTools);
    }
	
    public ArrayList<QueryRow> getCache(Envelope _mbr)
    {
        PreparedStatement pStatement = null;
        Connection conn = null;
        try {
            // Make sure layer's connection has not been lost
            conn = super.getConnection();
            if ( conn == null ) {
                return null;
            }
        } catch (IllegalStateException ise) {
            // Thrown by super.getConnection() indicates starting up, so we do nothing
            return null;
        }
        
        // get SQL creating 
        //
        String qSql = this.getSQL();
        if (Strings.isEmpty(qSql)) {
            return null;
        }

        if ( this.getPreferences().isLogSearchStats() ) {
            LOGGER.fine(qSql);
        }

        // Set up parameters for host layer 
        //
        pStatement = this.setParameters(qSql);
        if (pStatement==null) {
            return null;
        }

        // ****************** Execute the query ************************
        //
        ArrayList<QueryRow> retList = new ArrayList<QueryRow>(); // list of return rows
        Struct            retStruct = null;
        ResultSet               ors = null;
        String                rowID = null;
        try 
        {
            ors = (ResultSet)pStatement.executeQuery();
            ors.setFetchDirection(ResultSet.FETCH_FORWARD);
            ors.setFetchSize(this.getResultFetchSize());
            OracleResultSetMetaData rSetM = (OracleResultSetMetaData)ors.getMetaData(); // for column name

            String          value = "";
            String     columnName = "";
            String columnTypeName = "";
            @SuppressWarnings("unused")
			Object       objValue = null;
            long    totalFeatures = 0;    
            while ((ors.next()) &&
                   (this.getSpatialView().getSVPanel().isCancelOperation()==false)) 
            {
                LinkedHashMap<String, Object> calValueMap = new LinkedHashMap<String, Object>(rSetM.getColumnCount() - 1);
                for (int col = 1; col <= rSetM.getColumnCount(); col++) {
                	
                    columnName     = rSetM.getColumnName(col);
                    columnTypeName = rSetM.getColumnTypeName(col);
                    
                    if (columnTypeName.equals("LONG")) {
                        // LONGs will kill the SQL stream and we can't use them anyway
                        LOGGER.warn("GeoRaptor ignored " + rSetM.getColumnName(col) + "/" + rSetM.getColumnTypeName(col));
                        continue;
                    }
                    
                    if (columnName.equalsIgnoreCase(super.getGeoColumn())) {
                        retStruct = (java.sql.Struct)ors.getObject(col);
                        if (ors.wasNull()) {
                            break; // process next row
                        }
                        totalFeatures++;
                    } else {
                          if ( Tools.dataTypeIsSupported(columnTypeName) )
                          {
                              try
                              {
                                  objValue = ors.getObject(col);
                                  if (ors.wasNull()) {
                                      value = "NULL";
                                  } else {
                                      if ( ors.getMetaData().getColumnType(col) == OracleTypes.ROWID ) {
                                    	  continue;
                                          //rid   = ors.getRowId(col);
                                          //rowID = rid.toString();
                                          //value = rowID;
                                      } else {
                                          value = SQLConversionTools.convertToString(this.getConnection(),ors,col);                                          
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
                retList.add(new QueryRow(rowID, calValueMap, retStruct));
            }
            ors.close();
            pStatement.close();
            
            this.setNumberOfFeatures(totalFeatures);
            
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
            LOGGER.error("SVQueryLayer.getCache - general exception");
            e.printStackTrace();
        } finally {
            try { ors.close();        } catch (Exception _e) { }
            try { pStatement.close(); } catch (Exception _e) { }
        }
        return retList;
    }

    public double getTolerance() {
    	return super.getTolerance();
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
    
    public boolean isBuffered() {
        return this.buffered && this.bufferDistance != 0.0;
    }
    
    public void setPrecision(int _precision) {
        this.precision = _precision;
    }

    public int getPrecision() {
        return this.precision;
    }

    public void setDrawQueryGeometry(boolean _draw) {
        this.drawGeometry = _draw;
    }

    public void setDrawQueryGeometry(String _draw) {
        try 
        {
            this.drawGeometry = Boolean.valueOf(_draw);
        } catch (Exception e) {
            this.drawGeometry = false;
        }
    }

    public boolean isDrawQueryGeometry() {
        return this.drawGeometry;
    }
    
    public void setGeometry(JGeometry _jGeom) {
        this.queryGeometry = _jGeom;
    }
    
    public JGeometry getGeometry() {
        return this.queryGeometry;
    }

    private void fromXML(Node _node)
    {
      if (_node == null || _node.getNodeName().equals("Layer") == false) {
          return;
      }
      
      try {
          XPath xpath = XPathFactory.newInstance().newXPath();
                  		  
          this.setLayerName(       (String)xpath.evaluate("SVQueryLayer/Name/text()",_node,XPathConstants.STRING));
          this.setVisibleName(     (String)xpath.evaluate("SVQueryLayer/VisibleName/text()",_node,XPathConstants.STRING));
          this.setDesc(            (String)xpath.evaluate("SVQueryLayer/Description/text()",_node, XPathConstants.STRING));
          this.setDraw(
        		  Boolean.valueOf( (String)xpath.evaluate("SVQueryLayer/Draw/text()",_node,XPathConstants.STRING)));
          this.setSQL(             (String)xpath.evaluate("SVQueryLayer/SQL/text()",_node,XPathConstants.STRING));
          this.setResultFetchSize(
                Integer.valueOf(
                   Strings.isEmpty((String)xpath.evaluate("SVQueryLayer/ResultFetchSize/text()",_node,XPathConstants.STRING))
                   ? "0" :         (String)xpath.evaluate("SVQueryLayer/ResultFetchSize/text()",_node,XPathConstants.STRING)));
          this.setMinResolution(
                   Boolean.valueOf((String)xpath.evaluate("SVQueryLayer/MinResolution/text()",_node,XPathConstants.STRING)));
          this.setIndex(           (String)xpath.evaluate("SVQueryLayer/hasIndex/text()",_node,XPathConstants.STRING));
          this.setProject(
                   Boolean.valueOf((String)xpath.evaluate("SVQueryLayer/isProject/text()",_node,XPathConstants.STRING)),false);
          this.setBuffered(        (String)xpath.evaluate("SVQueryLayer/isBuffered/text()",_node,XPathConstants.STRING));
          this.setBufferDistance(  (String)xpath.evaluate("SVQueryLayer/bufferDistance/text()",_node,XPathConstants.STRING));
          this.setSdoOperator(     (String)xpath.evaluate("SVQueryLayer/sdoOperator/text()",_node,XPathConstants.STRING));
          this.setRelationshipMask((String)xpath.evaluate("SVQueryLayer/relationshipMask/text()",_node,XPathConstants.STRING));
                    
          Node stylingNode = (Node)xpath.evaluate("SVQueryLayer/Styling",_node,XPathConstants.NODE);
          this.styling = new Styling(stylingNode);
          
          String jGeomAsByteStr =  (String)xpath.evaluate("SVQueryLayer/geometry/text()",_node,XPathConstants.STRING);          
          // Convert string back to JGeometry
          if ( !Strings.isEmpty(jGeomAsByteStr) ) 
          {
              try 
              {
                  //byte[] geomBytes = com.sun.org.apache.xerces.internal.impl.dv.util.Base64.decode(jGeomAsByteStr);
				  byte[] geomBytes = Base64.decodeBase64(jGeomAsByteStr);
                  JGeometry nGeom = JGeometry.load(geomBytes);
                  this.setGeometry(nGeom);
              } catch (IOException e) {
                  LOGGER.error("Could not construct byte[] array from " + jGeomAsByteStr);
              } catch (Exception e) {
                  LOGGER.error("Could not construct JGeometry from stored XML geometry (" + e.getMessage() + ")");
              }
          }
          this.setDrawQueryGeometry((String)xpath.evaluate("SVQueryLayer/drawQueryGeometry/text()",_node,XPathConstants.STRING));

      } catch (XPathExpressionException xe) {
          LOGGER.error("Error loading Query layer XML (" +xe.getMessage() + ")");
      }
    }

    public String toXML() 
    {      
      // First convert JGeometry to serializable string via byte array
      String jGeomAsByteStr = "";
      try {
          byte[] jGeomBytes = JGeometry.store(this.getGeometry());
          //jGeomAsByteStr = com.sun.org.apache.xerces.internal.impl.dv.util.Base64.encode(jGeomBytes);
		  jGeomAsByteStr = Base64.encodeBase64String(jGeomBytes);
      } catch (IOException e) {
          LOGGER.warn(this.getVisibleName() + ": Could not serialise query geometry (" + e.getMessage() + ")");
      } catch (Exception e) {
          LOGGER.warn(this.getVisibleName() + ": Could not convert query JGeometry into byte array (" + e.getMessage() + ")");          
      }

      String xml = super.toXML();
      xml += "<SVQueryLayer>";
      xml += String.format("<Name>%s</Name><VisibleName>%s</VisibleName><Description>%s</Description><Draw>%s</Draw><SQL>%s</SQL>",
    		               this.getLayerName(),
    		               this.getVisibleName(),
    		               this.getDesc(),
                           String.valueOf(this.draw), 
                           this.getLayerSQL()
      );
      xml += String.format("<ResultFetchSize>%s</ResultFetchSize><MinResolution>%s</MinResolution><hasIndex>%s</hasIndex><isProject>%s</isProject>",
                           String.format("%d", this.resultFetchSize),
                           String.valueOf(this.minResolution),
                           String.valueOf(this.hasIndex()),
                           String.valueOf(this.getProject())
      );
      xml += String.format("<isBuffered>%s</isBuffered><bufferDistance>%s</bufferDistance><sdoOperator>%s</sdoOperator><relationshipMask>%s</relationshipMask><geometry>%s</geometry><drawQueryGeometry>%s</drawQueryGeometry>",
                           this.isBuffered(),
                           this.getBufferDistance(),
                           this.getSdoOperator(),
                           this.getRelationshipMask(),
                           jGeomAsByteStr,
                           this.isDrawQueryGeometry()
      );
      xml += this.styling.toXML(Preferences.getInstance().getMapBackground());
      xml += "</SVQueryLayer>";
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
            JFileChooser fc = new JFileChooser() {
				private static final long serialVersionUID = -7025374970321565482L;
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
        }catch (Exception e){//Catch exception if any
            LOGGER.error("Error: " + e.getMessage());
        }
    }

    public SpatialView getSpatialView() {
        return super.getSpatialView();
    }    

	@Override
	public Styling getStyling() {
        return this.styling;
	}

	public void setStyling(Styling _styling) {
        this.styling = new Styling(_styling);
	}

	@Override
	public String getStylingAsString() {
		// Returns styling as XML string
        return this.styling.toXML(this.getSpatialView().getMapPanel().getMapBackground());
	}

	@Override
	public Preferences getPreferences() {
        if (this.preferences==null) {
            this.preferences = MainSettings.getInstance().getPreferences();
        }
        return this.preferences;
	}

	@Override
	public String getLayerNameAndConnectionName() {
        return this.layerName + "." + super.getConnectionDisplayName();
	}

	@Override
	public String getLayerName() {
        return this.layerName;
	}

	@Override
	public void setLayerName(String _layerName) {
        this.layerName = _layerName;		
	}

    protected String  desc = "";         // Layer description. It is diplay when user left mouse cursor on layer name.

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String _desc) {
        this.desc = _desc;
    }

	@Override
	public String getVisibleName() {
        return this.visibleName;
    }

    public void setVisibleName(String _visibleName) {
        this.visibleName = _visibleName;		
	}

    protected boolean draw = true;       // true - draw layer, false - not draw layer

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
	
	@Override
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
            LOGGER.debug("SVSpatialLayer(" + 
                         this.getLayerNameAndConnectionName() + 
                         ").setIndex.isSpatiallyIndexed()");
            this.indexExists = Queries.isSpatiallyIndexed(conn,
                                                          super.getSchemaName(),
                                                          super.getObjectName(),
                                                          super.getGeoColumn(),
                                                          super.getSRID());
            LOGGER.debug("SVSpatialLayer(" + this.getLayerNameAndConnectionName() + ").setIndex() = " + indexExists);
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("SVSpatialLayer(" + this.getLayerNameAndConnectionName() + ").isSpatiallyIndexed Exception: " + iae.toString());
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

	@Override
	public boolean setLayerMBR(Envelope _defaultMBR, int _targetSRID)
    {
        Connection conn = super.getConnection();
         
        // If source is INDEX get MBR from RTree index
        //
        Envelope lMBR = new Envelope(this.getDefaultPrecision());
        
        // If one method fails, a warning is written. When this happens we need to report success of later method
        boolean multiTry = false;
        
        if (this.hasIndex() &&
            this.getSRIDType().toString().startsWith("GEO") == false &&
            this.getPreferences()
                .getLayerMBRSource()
                .equalsIgnoreCase(Constants.CONST_LAYER_MBR_INDEX)) 
        {
            try {
                lMBR.setMBR(Queries.getExtentFromRTree(conn,
                                                       this.getSchemaName(),
                                                       this.getObjectName(),
                                                       this.getGeoColumn(),
                                                       this.getSRID(),
                                                       String.valueOf(_targetSRID))); 
                LOGGER.debug("SVQueryLayer.setLayerMBR() setMBR(getRTreeExtent)=" + lMBR.toString());
                if ( lMBR.isSet() ) {
                    super.setMBR(lMBR);
                    return true;
                } 
            } catch (SQLException e) {
                LOGGER.warn("Could not extract extent from RTree index so will now try and extract from metadata.");
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
                    LOGGER.warn("Could not extract extent from metadata, so will now extract from a sample of records.");
                    multiTry = true;
                }
            }  
        } catch (SQLException e) {
            LOGGER.warn("No User_Sdo_Geom_Metadata, so will now extract extent from a sample of records.");
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

	@Override
	public String getSDOFilterParameters() 
    {
        String sdoFilterClause = this.getSDOFilterParameters();
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

	@Override
	public String getLayerSQL() {
		return this.getSQL();
	}

    protected boolean calculateMBR = false;      // When true, recalculates Layer MBR at next draw from actual data

    public Envelope getMBR() 
    {
    	return super.getMBR();
    }

	@Override
    public void setMBRRecalculation(boolean _recalc) {
        this.calculateMBR = _recalc;
    }

	@Override
    public boolean getMBRRecalculation() {
        return this.calculateMBR;
    }

    protected long numberOfFeatures = 0;          // Feature Count for display and stats

	@Override
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

	@Override
	public long getNumberOfFeatures() {
        return this.numberOfFeatures;
	}

	@Override
    public LinkedHashMap<String, String> 
    getColumnsAndTypes(boolean _onlyNumbersDatesAndStrings,
                       boolean _fullDataType) 
	throws SQLException 
	{
		// Should try and get columns etc from SQL being executed.
        // Until that day get columns and types directly from underlying object
		//
		try
	    {
			Connection conn = super.getConnection();	     
			LinkedHashMap<String, String> colsAndTypes =
	                       Queries.getColumnsAndTypes(conn,
	                                                  this.getSchemaName(),
	                                                  this.getObjectName(),
	                                                  _onlyNumbersDatesAndStrings,
	                                                  _fullDataType);
			return colsAndTypes;
		} catch (SQLException e) {
			LOGGER.debug("Problem retrieving attributes and types - " + e.toString());
	    }
		return null;
	}

	@Override
	public SVQueryLayer createCopy() throws Exception {
        SVQueryLayer newLayer = null;

        String newName = super.getSpatialView().getSVPanel().getViewLayerTree().checkName(this.getLayerName());

        // Shared SVLayer stuff
        //
        newLayer = new SVQueryLayer(this.getSpatialView(),
                                    newName,
                                    this.getVisibleName()+layerCopySuffix,
                                    this.getDesc(),
                                    this.getMetadataEntry(),
                                    this.isDraw());
        
        // set SVLayer properties (What is a copy? Is it a render layer?)
        newLayer.setSRIDType(this.getSRIDType());
        newLayer.setGeometryType(this.getGeometryType());
        newLayer.setConnectionName(super.connName);
        newLayer.setIndex(this.hasIndex());
        newLayer.setMBR(this.getMBR());
        newLayer.setSQL(this.getSQL());
        newLayer.setMinResolution(this.getMinResolution());
        newLayer.setFetchSize(this.getFetchSize());
        newLayer.setPrecision(this.getPrecision(false));
        newLayer.setProject(this.getProject(),false);

        // Label
        newLayer.setStyling(this.styling);
        
        return newLayer;
	}

    protected boolean minResolution = false;      // Do we use min_resolution sdo_filter parameter when querying?

	@Override
	public void setMinResolution(boolean _minResolution) 
	{
        if ( this.geometryType == Constants.GEOMETRY_TYPES.POINT )
            this.minResolution = false;
        else
            this.minResolution = _minResolution;
    }
  
    public boolean getMinResolution() {
        return ( this.geometryType == Constants.GEOMETRY_TYPES.POINT ) ? false : this.minResolution;
    }

	@Override
    public void setResultFetchSize(int _resultFetchSize) {
        this.resultFetchSize = _resultFetchSize;
    }
  
    public int getResultFetchSize() {
        return this.resultFetchSize;
    }
    
    public SVSpatialLayerDraw getDrawTools() {
        return this.drawTools;
    }

	@Override
    public SpatialView getView() {
        return super.getSpatialView();
    }

    protected boolean project = false;      // Project to view's projection?

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
	public void setKeyColumn(String _column) {
		this.keyColumn = _column;		
	}

	@Override
	public String getKeyColumn() {
		return this.keyColumn;
	}

}
