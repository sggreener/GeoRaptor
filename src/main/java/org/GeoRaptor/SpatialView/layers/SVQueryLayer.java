package org.GeoRaptor.SpatialView.layers;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.GeoRaptor.Constants;
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
extends SVSpatialLayer
implements iLayer
{
    public  static final String CLASS_NAME = Constants.KEY_SVQueryLayer;

    private static final Logger     LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVQueryLayer");

    protected    String              sql = "";
    protected Constants.SDO_OPERATORS 
                             sdoOperator = Constants.SDO_OPERATORS.ANYINTERACT;
    protected       int        precision = 2;
    protected JGeometry    queryGeometry = null; 
    protected    String relationshipMask = "";
    protected    double   bufferDistance = 0.0;
    protected   boolean         buffered = false;
    protected   boolean     drawGeometry = false;

    public SVQueryLayer(SpatialView   _sView, 
                        String        _name, 
                        String        _screenName,
                        String        _desc, 
                        MetadataEntry _me, 
                        boolean       _draw) 
    {
        super(_sView, 
              _name, 
              _screenName, 
              _desc, 
              _me, 
              _draw);
        this.drawGeometry = super.getPreferences().isDrawQueryGeometry();
    }
    
    /**
     * @constructor SVSpatialLayer
     * @param _sView
     * @param _node
     * @author Simon Greener, December 6th, 2010
     *          -  XML Constructor
     */
    public SVQueryLayer(SpatialView _sView, Node _node) {
      super(_sView, _node);
      this.fromXMLNode(_node);
      this.drawGeometry = super.getPreferences().isDrawQueryGeometry();
    }

    /** Copy constructor
     *
     **/
    public SVQueryLayer(SVSpatialLayer _sLayer) 
    {
        super(_sLayer);
        LOGGER.debug("*********** SVQueryLayer(SVSpatialLayer)");
        if ( this.getSpatialView() != null ) {
            this.setLayerName(this.getSpatialView().checkLayerName(super.getLayerName()));
        }
        // This is an informational query of actual data: filtering would make the result invalid
        this.setMinResolution(false);
        this.drawGeometry = super.getPreferences().isDrawQueryGeometry();
        LOGGER.debug("*********** SVQueryLayer(SVSpatialLayer) -- END");
    }

    /** Copy constructor
     *
     **/
    public SVQueryLayer(SVQueryLayer _qLayer) 
    {
        super(_qLayer);
        LOGGER.debug("*********** SVQueryLayer(SVQueryLayer)");
        if ( this.getSpatialView() != null ) {
            LOGGER.debug("************** this.getSpatialView() != null");
            // this.setLayerName(this.getSpatialView().checkLayerName(super.getLayerName()));
            this.setGeometry(_qLayer.getGeometry());
            this.setBufferDistance(_qLayer.getBufferDistance());
            this.setBuffered(_qLayer.getBufferDistance()!=0.0);
            this.setDrawQueryGeometry(_qLayer.isDrawQueryGeometry());
            this.setRelationshipMask(_qLayer.getRelationshipMask());
            this.setSdoOperator(_qLayer.getSdoOperator());            
            this.setSQL(_qLayer.getSQL());
        } else {
            LOGGER.debug("************** this.getSpatialView() == null");
            this.drawGeometry = super.getPreferences().isDrawQueryGeometry();            
        }
        // This is an informational query of actual data: filtering would make the result invalid
        this.setMinResolution(false);
        LOGGER.debug("*********** SVQueryLayer(SVQueryLayer) -- END");
    }

    public SVQueryLayer createCopy(boolean _renderOnly) 
    {
        LOGGER.debug("SVQueryLayer.createCopy");
        // _renderOnly is ignored for SVQueryLayers
        //
        SVQueryLayer newLayer;
        try {
            newLayer = new SVQueryLayer(this);
        } catch (Exception e) {
            LOGGER.error("Query Layer Copy Constructor: " + e.toString());
            return null;
        }
        return newLayer;
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

    private boolean isProjected() {
        boolean project = super.project && 
                          super.spatialView.getSRID().equals(Constants.NULL) == false && 
                          super.getSRID().equals(Constants.NULL) == false;
		return project;
    }
    
    public String generateSQL(boolean _buffered,
    		                  boolean _project)
    {
System.out.println("<generateSQL>");
    	String sql = "";
        // Get search predicate
        //
        String searchGeom = "",
               bufferGeom = "";
        
        // If filter geometry buffered could need projecting to stored geometry 
        // We transform/project the search geometry (eg draw optimized rectangle) to the SRID of the
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

System.out.println(sql);
System.out.println("</generateSQL>");

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
				pStatement.setDouble(stmtParamIndex++, super.getTolerance());
				params += "? - " + String.valueOf(this.getTolerance());
	        } 

		} catch (Exception e) {
			LOGGER.error("SVQueryLayer.setParameters(" + e.getMessage() + ") - SQL copied to clipboard.");
			Tools.doClipboardCopy(_sql + params);
			return null;
		}
		return pStatement;
	}

    public ArrayList<QueryRow> queryByPoint(Point2D _worldPoint, 
                                            int     _numSearchPixels) 
    {
        // Take simple approach:
    	// 1 Execute normal SQL
    	// 2. Filter each geometry in its resultSet
        // 3. Filtering can occur in srid of outer geometry as always same as SpatialView
        //
        ArrayList<QueryRow> queryList = new ArrayList<QueryRow>(); // list of return rows
        
        String sql = this.getSQL();

        // WITH sGeom AS (
        //	 SELECT ? AS geom FROM DUAL
        // )
        // SELECT f.*
        //   FROM (SELECT "ID","LABEL","ANGLEDEGREES",t.GEOM as GEOM 
        //	         FROM sGeom a,
        //                GEORAPTOR.PROJPOINT2D t 
        //          WHERE SDO_ANYINTERACT(t.GEOM,a.geom) = 'TRUE'
        // ) f
        
        // Add SDO_DISTANCE predicate
        //
        // WHERE MDSYS.SDO_GEOM.SDO_DISTANCE(f.geom,distancePoint,tol,unit) <= distance
        
        sql += "\n WHERE MDSYS.SDO_GEOM.SDO_DISTANCE(f.GEOM,?/*DistancePoint*/,?/*tol*/,?/*unit*/) <= ?";

        // Get distance point and search distance 
        //
        Connection              conn = null;
        PreparedStatement pStatement = null;
        conn = this.getConnection();

        try {
	        double    sdo_distance = super.getSearchRadius(_numSearchPixels);	        
	        Point2D  searchPoint2D = new Point2D.Double(_worldPoint.getX(),_worldPoint.getY());	        
	        Struct     searchPoint = Queries.projectSdoPoint(conn,searchPoint2D,this.spatialView.getSRIDAsInteger(),this.spatialView.getSRIDAsInteger());
	        String units_parameter = String.format(",unit=%s",Tools.getViewUnits(spatialView,Constants.MEASURE.LENGTH));
        
	        // Get sdo_distance tolerance parameter
	        pStatement = this.setParameters(sql);
System.out.println("Query queryByPoint: " + sql);	        
	        int stmtParamIndex = pStatement.getParameterMetaData().getParameterCount();
	        stmtParamIndex -= 3; 
	        String params = "";
	        pStatement.setObject(stmtParamIndex++, searchPoint, java.sql.Types.STRUCT); params  = String.format("? %s\n", SDO_GEOMETRY.getGeometryAsString(searchPoint));
	        pStatement.setDouble(stmtParamIndex++, this.getTolerance());                params += String.format("? '%s'\n",this.getTolerance());
	        pStatement.setDouble(stmtParamIndex++, sdo_distance);                       params += String.format("? '%s'\n",this.getTolerance());
	        pStatement.setString(stmtParamIndex++, units_parameter);                    params += String.format("? '%s'\n",units_parameter);
	        System.out.println(params);
	        
        } catch (Exception e) {
        }
        
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
    
    /**
     * Execute SQL and draw data on given graphical device
     * @param _mbr MBR coordinates
     * @param _g2 graphical device
     * @return if return false, something was wrong (for example, Connection with DB faild)
     */
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

        if ( this.isDrawQueryGeometry() ) {
            try {
                JGeometry jGeom = this.getGeometry();
                if ( jGeom!=null) {
                    // Set graphics2D to draw query feature
                    drawTools.setGraphics2D(_g2);
                    // Save settings
                    LineStyle.LINE_STROKES ls = super.getStyling().getLineStrokeType();
                    int lw = super.getStyling().getLineWidth();
                    Styling.STYLING_TYPE st = super.getStyling().getShadeType();
                    float stl = super.getStyling().getShadeTransLevel();
                    
                    super.getStyling().setLineWidth(2);
                    super.getStyling().setLineStrokeType(LineStyle.LINE_STROKES.LINE_DASH);
                    super.getStyling().setShadeType(Styling.STYLING_TYPE.NONE);
                    // Draw original geometry before buffering
                    //
                    super.callDrawFunction(jGeom,null,null,null,null,4,0.0);
                    
                    if ( this.isBuffered()) {
                        super.getStyling().setLineWidth(lw); // set back
                        super.getStyling().setShadeType(this.getPreferences().isRandomRendering()?Styling.STYLING_TYPE.RANDOM:Styling.STYLING_TYPE.CONSTANT);
                        super.getStyling().setShadeTransLevel(0.5f);
                        super.getStyling().setLineStrokeType(LineStyle.LINE_STROKES.LINE_DOT);
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
                            super.callDrawFunction(jGeom,null,null,null,null,4,0.0);
                        } catch (Exception e) {
                            LOGGER.error("Failed to buffer query geometry by " + this.getBufferDistance());
                            jGeom = this.getGeometry();
                        }
                    }
                    super.getStyling().setShadeTransLevel(stl);
                    super.getStyling().setLineStrokeType(ls);
                    super.getStyling().setShadeType(st);
                }
            } catch (IOException e) {
                LOGGER.error(super.propertyManager.getMsg("ERROR_DISPLAY_QUERY_GEOM",e.getMessage()));
            }
        }

        LOGGER.logSQL(qSql);

System.out.println("=======================");
System.out.println(qSql);
System.out.println("=======================");

        // Now execute query
        //
        return super.executeDrawQuery(pStatement,qSql,_g2);
    }

    /**
     * @method getCache
     * @param _mbr - Needed for SVSpatialLayer sdo_filter that is a part of a Query
     * @description Turns current query into a cache of geometry and attribute objects.
     * @author Simon Greener, December 2012, Original Coding.
     */
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
System.out.println("getCache() - " + qSql);
        pStatement = this.setParameters(qSql);
        if (pStatement==null) {
            return null;
        }

        // ****************** Execute the query ************************
        //
        ArrayList<QueryRow> retList = new ArrayList<QueryRow>(); // list of return rows
        Struct            retStruct = null;
        ResultSet               ors = null;
        RowId                   rid = null;
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
                    LOGGER.error("SVSpatialLayer.executeDrawQuery error: " + npe.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("SVSpatialLayer.executeDrawQuery - general exception");
            e.printStackTrace();
        } finally {
            try { ors.close();        } catch (Exception _e) { }
            try { pStatement.close(); } catch (Exception _e) { }
        }
        return retList;
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

  private void fromXMLNode(Node _node) {
      if (_node == null || _node.getNodeName().equals("Layer") == false) {
          //System.out.println("XML node is null or not a Layer");
          return; // Should throw error
      }
      try {
          XPath xpath = XPathFactory.newInstance().newXPath();
          this.setSQL((String)xpath.evaluate("SVQueryLayer/SQL/text()",_node,XPathConstants.STRING));
          this.setBuffered((String)xpath.evaluate("SVQueryLayer/isBuffered/text()",_node,XPathConstants.STRING));
          this.setBufferDistance((String)xpath.evaluate("SVQueryLayer/bufferDistance/text()",_node,XPathConstants.STRING));
          this.setSdoOperator((String)xpath.evaluate("SVQueryLayer/sdoOperator/text()",_node,XPathConstants.STRING));
          this.setRelationshipMask((String)xpath.evaluate("SVQueryLayer/relationshipMask/text()",_node,XPathConstants.STRING));
          String jGeomAsByteStr = (String)xpath.evaluate("SVQueryLayer/geometry/text()",_node,XPathConstants.STRING);
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

    public String toXML() {
      String SVLayer_SpatialLayerXML = super.toXML(); 
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
      return SVLayer_SpatialLayerXML + 
             String.format("<SVQueryLayer><SQL>%s</SQL><isBuffered>%s</isBuffered><bufferDistance>%s</bufferDistance><sdoOperator>%s</sdoOperator><relationshipMask>%s</relationshipMask><geometry>%s</geometry><drawQueryGeometry>%s</drawQueryGeometry></SVQueryLayer>",
                           super.getSQL(),   
                           this.isBuffered(),
                           this.getBufferDistance(),
                           this.getSdoOperator(),
                           this.getRelationshipMask(),
                           jGeomAsByteStr,
                           this.isDrawQueryGeometry());
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

    public String getQuerySQLOld() 
    {
        // If filter geometry buffered? 
        //
        String bufferClause = "?";
        
        if (this.isBuffered()) 
        {
            String lengthUnits  = super.getSpatialView().getDistanceUnitType();
            String bufferParams = ((super.getSRID().equals(Constants.NULL) || Strings.isEmpty(lengthUnits)) 
                                   ? "" 
                                   : ",'unit=" + lengthUnits + "'");
            /**
             * MDSYS.SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY,
             *                     dist IN NUMBER,
             *                     tol IN NUMBER
             *                     [, params IN VARCHAR2]
             *                    ) RETURN SDO_GEOMETRY;
            **/
            bufferClause = String.format("MDSYS.SDO_GEOM.SDO_BUFFER(?,?,?%s)",bufferParams);
        }

        // Create filter Geometry that could be buffered and might need projecting
        //
        String filterGeometry = "";
        filterGeometry = (super.project && 
                          super.spatialView.getSRID().equals(Constants.NULL) == false && 
                          super.getSRID().equals(Constants.NULL) == false) 
                         ? String.format("MDSYS.SDO_CS.TRANSFORM(t.%s,%s)",
                                         bufferClause,
                                         super.spatialView.getSRID()) 
                         : bufferClause;

        // Our query SQL can be:
        // 1. Built on top of the current Layer SQL (including user modifications), or
        // 2. Build on top of the original data import org.apache.commons.codec.binary.Base64; object using "clean" SQL
        // While 1 means duplicate filter queries, 1 is chosen for the time being
        //
        String qSql = String.format("%s\n AND 2=2",super.getSQL());   // Note use of 2=2 Predicate
        if (Strings.isEmpty(super.wrappedSQL)) {
            qSql = super.wrapSQL(qSql);
            this.wrappedSQL = qSql;
        } else {
            qSql = this.wrappedSQL;
        }
        
        LOGGER.debug("sdoOperator is " + this.sdoOperator.toString() + " with isSTGeometry of "+this.isSTGeometry());

        if (   this.hasIndex() && 
            ! (this.isSTGeometry() && 
               this.sdoOperator!=Constants.SDO_OPERATORS.RELATE)
           )
        {
            qSql = qSql.replace("2=2",
                   ( this.isSTGeometry() 
                     ? "t." + super.getGeoColumn() + ".ST_" + this.sdoOperator.toString() + "(MDSYS.ST_GEOMETRY("  + filterGeometry + ")) = 1"
                     : String.format("SDO_%s(t.%s,\n       %s%s) = 'TRUE'",
                                     this.sdoOperator.toString(),
                                     super.getGeoColumn(),
                                     filterGeometry,
                                     (this.sdoOperator==Constants.SDO_OPERATORS.RELATE 
                                      ? ( ",'" + getRelationshipMask() + "'" ) 
                                      : "")
                                     ) 
                   ));
        } else {
            // Original SQL may have sdo_relate already. If so, change its ANYTINTERACT mask
            //
            if ( qSql.toUpperCase().indexOf("MDSYS.SDO_GEOM.RELATE")!=0 &&
                 qSql.toUpperCase().indexOf("'ANYINTERACT'")!=0 ) {
                qSql = qSql.replace("'ANYINTERACT'", "'mask=" + getRelationshipMask() + "'");
            } else {
                // Add new relate mask
                qSql = qSql.replace("2=2",
                                    String.format("%s(t.%s,'%s',%s,%f) = 'TRUE'",
                                     super.getGeoColumn() + (this.isSTGeometry() ? ".GEOM" : ""),
                                     getRelationshipMask(),
                                     filterGeometry,
                                     this.getTolerance()));
            }
        }
        LOGGER.logSQL("SVQueryLayer - getSQL = " + qSql);
        return qSql;
    }

	protected PreparedStatement setParametersOLD(String _sql, Envelope _mbr) {
		Connection conn = null;
		PreparedStatement pStatement = null;
		int stmtParamIndex = 1;
		String params = "";
		try {
			conn = super.getConnection();

			pStatement = super.setParameters(_sql, _mbr);
			if (pStatement == null) {
				LOGGER.error("Failed to set SQL parameters for Query Layer " + super.layerName);
				return null;
			}
// Now set up parameters for this layer
//
			stmtParamIndex = super.isSTGeometry() ? 2 : 3; // This is UGLY and hardcoded! I need to know what parameters
															// are set by super.setParameters()
			if (super.hasIndex()) {
// Use SDO_RELATE, SDO_EQUAL etc.
// Only SDO_RELATE has third parameter
//

				Struct stSearchGeom = null;
				String searchGeometry = "";
				stSearchGeom = JGeom.toStruct(this.getGeometry(), conn);
				searchGeometry = SDO_GEOMETRY.getGeometryAsString(stSearchGeom);
				pStatement.setObject(stmtParamIndex++, stSearchGeom, java.sql.Types.STRUCT);
				params += "\n? = " + searchGeometry;

				if (this.isBuffered()) {
// This buffer geometry replaces search geometry...
					/**
					 * MDSYS.SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY, <-- super.setParameters dist
					 * IN NUMBER, tol IN NUMBER [, params IN VARCHAR2] ) RETURN SDO_GEOMETRY;
					 **/
					pStatement.setDouble(stmtParamIndex++, this.getBufferDistance());
					params += "\n? = " + this.getBufferDistance();
					pStatement.setDouble(stmtParamIndex++, super.getTolerance());
					params += "\n? = " + String.valueOf(super.getTolerance());
				}

				if (this.sdoOperator == Constants.SDO_OPERATORS.RELATE) {
					pStatement.setString(stmtParamIndex++, "mask=" + this.getRelationshipMask());
					params += "\n? = mask=" + this.getRelationshipMask();
				}
			} else /* ! super.hasIndex() ie no SDO_FILTER */
			{
// Add geom1  <-- super.setParameters
// mask/geom2,tol parameter values
//
// SDO_GEOM.RELATE(
//      geom1 IN SDO_GEOMETRY,
//      mask IN VARCHAR2,
//      geom2 IN SDO_GEOMETRY,
//      tol IN NUMBER
//      ) RETURN VARCHAR2;
//
				pStatement.setString(stmtParamIndex++, "'" + this.getRelationshipMask() + "'");
				params += "\n? = mask=" + this.getRelationshipMask();

				Struct stGeom = null; // (Struct)JGeometry.storeJS(conn,this.getGeometry());
				stGeom = JGeom.toStruct(this.getGeometry(), conn);
				pStatement.setObject(stmtParamIndex++, stGeom, java.sql.Types.STRUCT);
				params += "\n? = " + SDO_GEOMETRY.getGeometryAsString(stGeom);

				if (this.isBuffered()) {
					/**
					 * MDSYS.SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY, <-- super.setParameters dist
					 * IN NUMBER, tol IN NUMBER [, params IN VARCHAR2] ) RETURN SDO_GEOMETRY;
					 **/
					pStatement.setDouble(stmtParamIndex++, this.getBufferDistance());
					params += "\n? = " + this.getBufferDistance();
					pStatement.setDouble(stmtParamIndex++, super.getTolerance());
					params += "\n? = " + String.valueOf(super.getTolerance());
				}
// Tolerance
				pStatement.setDouble(stmtParamIndex++, super.getTolerance());
				params += "\n? = " + String.valueOf(super.getTolerance());
			}
		} catch (SQLException sqle) {
			LOGGER.error("SVQueryLayer.setParameters(" + sqle.getMessage() + ") - SQL copied to clipboard.");
			Tools.doClipboardCopy(_sql + params);
			return null;
		} catch (Exception e) {
// TODO Auto-generated catch block
			LOGGER.error("SVQueryLayer.setParameters(" + e.getMessage() + ") - SQL copied to clipboard.");
			Tools.doClipboardCopy(_sql + params);
			return null;
		}
		return pStatement;
	}

}
