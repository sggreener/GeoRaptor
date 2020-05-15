package org.GeoRaptor.SpatialView.layers;

import java.awt.Graphics2D;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.LineStyle;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.io.ExtensionFileFilter;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.apache.commons.codec.binary.Base64;

import org.geotools.util.logging.Logger;

import org.w3c.dom.Node;

@SuppressWarnings("deprecation")
public class SVQueryLayer 
extends SVSpatialLayer 
{
    public static final String CLASS_NAME = Constants.KEY_SVQueryLayer;
    private static final Logger   LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVQueryLayer");

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
    public SVQueryLayer(SVQueryLayer _sLayer) 
    {
        super(_sLayer);
        LOGGER.debug("*********** SVQueryLayer(SVQueryLayer)");
        if ( this.getSpatialView() != null ) {
            LOGGER.debug("************** this.getSpatialView() != null");
            // this.setLayerName(this.getSpatialView().checkLayerName(super.getLayerName()));
            this.setGeometry(_sLayer.getGeometry());
            this.setBufferDistance(_sLayer.getBufferDistance());
            this.setBuffered(_sLayer.getBufferDistance()!=0.0);
            this.setDrawQueryGeometry(_sLayer.isDrawQueryGeometry());
            this.setRelationshipMask(_sLayer.getRelationshipMask());
            this.setSdoOperator(_sLayer.getSdoOperator());            
            this.setSQL("");
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
        // no nothing
    }

    public String getSQL() {
        return getQuerySQL();
    }
    public String getQuerySQL() 
    {
        // If filter geometry buffered? 
        //
//  try { String s = null; s.length(); } catch (Exception e) { e.printStackTrace(); }
        String bufferClause = "?";
        if (this.isBuffered()) 
        {
            String lengthUnits = super.getSpatialView().getDistanceUnitType();
            String bufferParams = ((super.getSRID().equals(Constants.NULL) || Strings.isEmpty(lengthUnits)) 
                                   ? "" 
                                   : ",'unit=" + lengthUnits + "'");
            /**
             * SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY,
             *                     dist IN NUMBER,
             *                     tol IN NUMBER
             *                     [, params IN VARCHAR2]
             *                    ) RETURN SDO_GEOMETRY;
            **/
            bufferClause = String.format("SDO_GEOM.SDO_BUFFER(?,?,?%s)",bufferParams);
        }

        // Create filter Geometry that could be buffered and might need projecting
        //
        String filterGeometry = "";
        filterGeometry = (super.project && 
                          super.spatialView.getSRID().equals(Constants.NULL) == false && 
                          super.getSRID().equals(Constants.NULL) == false) 
                         ? String.format("MDSYS.SDO_CS.TRANSFORM(t.%s,%s)",bufferClause,super.getSRID()) 
                         : bufferClause;

        // Our query SQL can be:
        // 1. Built on top of the current Layer SQL (including user modifications), or
        // 2. Build on top of the original dataimport org.apache.commons.codec.binary.Base64; object using "clean" SQL
        // While 1 means duplicate filter queries, 1 is chosen for the time being
        //
        String qSql = String.format("%s\n  AND 2=2",super.getSQL());   // Note use of 2=2 Predicate
        if (Strings.isEmpty(super.wrappedSQL)) {
            qSql = super.wrapSQL(qSql);
            this.wrappedSQL = qSql;
        } else {
            qSql = this.wrappedSQL;
        }
        LOGGER.debug("sdoOperator is " + this.sdoOperator.toString() + " with isSTGeometry of "+this.isSTGeometry());
        if (   this.hasIndex() && 
            ! (this.isSTGeometry() && this.sdoOperator!=Constants.SDO_OPERATORS.RELATE)) {
            qSql = qSql.replace("2=2",
                   ( this.isSTGeometry() 
                     ? "t." + super.getGeoColumn() + ".ST_" + this.sdoOperator.toString() + "(MDSYS.ST_GEOMETRY("  + filterGeometry + ")) = 1"
                   : String.format("SDO_%s(t.%s,\n       %s%s) = 'TRUE'",
                                   this.sdoOperator.toString(),
                                   super.getGeoColumn(),
                                   filterGeometry,
                                   (this.sdoOperator==Constants.SDO_OPERATORS.RELATE 
                                    ? ( ",'" + getRelationshipMask() + "'" ) : "")
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
LOGGER.debug("SVQueryLayer - getSQL = " + qSql);
        return qSql;
    }

    protected OraclePreparedStatement setParameters(String          _sql, 
                                                    Envelope _mbr) 
    {
        OraclePreparedStatement pStatement = null;
        int                 stmtParamIndex = 1;
        String                      params = "";
        try 
        {
            pStatement = super.setParameters(_sql,_mbr);
            if ( pStatement==null ) {
                LOGGER.error("Failed to set parameters for Query SQL");
                return null;
            }
            // Now set up parameters for this layer
            //
            stmtParamIndex = super.isSTGeometry() ? 2 : 3; // This is UGLY and hardcoded! I need to know what parameters are set by super.setParameters()
            OracleConnection conn = super.getConnection();
            if (super.hasIndex()) 
            {
                // Use SDO_RELATE, SDO_EQUAL etc.
                // Only SDO_RELATE has third parameter
                //
                pStatement.setSTRUCT(stmtParamIndex++,
                                     (STRUCT)JGeometry.storeJS(conn,this.getGeometry()));
                params += "\n? = " +
                    SDO_GEOMETRY.convertGeometryForClipboard(
                    		       (STRUCT)JGeometry.storeJS(conn,this.getGeometry()),conn);

                if (this.isBuffered()) {
                    /**
                     * SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY, <-- previous
                     *                     dist IN NUMBER,
                     *                     tol IN NUMBER
                     *                     [, params IN VARCHAR2]
                     *                    ) RETURN SDO_GEOMETRY;
                    **/
                    pStatement.setDouble(stmtParamIndex++,this.getBufferDistance());
                    params += "\n? = " + this.getBufferDistance();
                    pStatement.setDouble(stmtParamIndex++,super.getTolerance());
                    params += "\n? = " + String.valueOf(super.getTolerance());
                }
                if ( this.sdoOperator==Constants.SDO_OPERATORS.RELATE ) {
                    pStatement.setString(stmtParamIndex++,"mask=" + this.getRelationshipMask() );
                    params += "\n? = mask=" + this.getRelationshipMask();
                }
            } else {
                // SDO_GEOM.RELATE(
                //      geom1 IN SDO_GEOMETRY,
                //      mask IN VARCHAR2,
                //      geom2 IN SDO_GEOMETRY,
                //      tol IN NUMBER
                //      ) RETURN VARCHAR2;
                //
                pStatement.setString(stmtParamIndex++,"'" + this.getRelationshipMask() + "'" );
                params += "\n? = mask=" + this.getRelationshipMask();

                pStatement.setSTRUCT(stmtParamIndex++,
                             (STRUCT)JGeometry.storeJS(conn,this.getGeometry()));
                params += "\n? = " +
                    SDO_GEOMETRY.convertGeometryForClipboard(
                         (STRUCT)JGeometry.storeJS(conn,this.getGeometry()),conn);
                if (this.isBuffered()) {
                    /**
                     * SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY, <-- previous
                     *                     dist IN NUMBER,
                     *                     tol IN NUMBER
                     *                     [, params IN VARCHAR2]
                     *                    ) RETURN SDO_GEOMETRY;
                    **/
                    pStatement.setDouble(stmtParamIndex++,this.getBufferDistance());
                    params += "\n? = " + this.getBufferDistance();
                    pStatement.setDouble(stmtParamIndex++,super.getTolerance());
                    params += "\n? = " + String.valueOf(super.getTolerance());
                }
                pStatement.setDouble(stmtParamIndex++,super.getTolerance());
                params += "\n? = " + String.valueOf(super.getTolerance());
            }
        } catch (SQLException sqle) {
            LOGGER.error("SVQueryLayer.setParameters("+sqle.getMessage()+") - SQL copied to clipboard.");
            Tools.doClipboardCopy(_sql + params);
            return null;
        } catch (Exception e) {
			// TODO Auto-generated catch block
            LOGGER.error("SVQueryLayer.setParameters("+e.getMessage()+") - SQL copied to clipboard.");
            Tools.doClipboardCopy(_sql + params);
            return null;
		}
        return pStatement;
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
        OracleConnection conn = null;
        try {
            // Make sure layer's connection has not been lost
            conn = super.getConnection();
            if ( conn == null ) {
                return false;
            }
        } catch (IllegalStateException ise) {
            // Thrown by super.getConnection() indicates starting up, so we do nothing
            return false;
        }
        
        // Get SQL
        // Will call SVSpatialLayer to wrapSQL
        //
        String qSql = this.getQuerySQL();
        if (Strings.isEmpty(qSql)) {
            return false;
        }

        // Set up parameters for host layer 
        //
        OraclePreparedStatement pStatement = null;
        pStatement = this.setParameters(qSql,_mbr);
        if (pStatement==null) {
            return false;
        }

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
        OraclePreparedStatement pStatement = null;
        OracleConnection conn = null;
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

        // We wrap the SQL the user may have modified in order to return only the columns needed for visualisation
        // Ensures all columns needed for labelling, colouring, rotation etc area available 
        // in order to reduce network traffic
        //
        qSql = wrapSQL(qSql);

        if ( this.getPreferences().isLogSearchStats() ) {
            LOGGER.fine(qSql);
        }

        // Set up parameters for host layer 
        //
        pStatement = this.setParameters(qSql,_mbr);
        if (pStatement==null) {
            return null;
        }

        // ****************** Execute the query ************************
        //
        ArrayList<QueryRow> retList = new ArrayList<QueryRow>(); // list of return rows
        STRUCT            retSTRUCT = null;
        OracleResultSet         ors = null;
        String                rowID = null;
        try 
        {
            ors = (OracleResultSet)pStatement.executeQuery();
            ors.setFetchDirection(OracleResultSet.FETCH_FORWARD);
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
                        System.out.println("GeoRaptor ignored " + rSetM.getColumnName(col) + "/" + rSetM.getColumnTypeName(col));
                        continue;
                    }
                    if (columnName.equalsIgnoreCase(super.getGeoColumn())) {
                        retSTRUCT = (oracle.sql.STRUCT)ors.getObject(col);
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
                                          rowID = ors.getROWID(col).stringValue(); 
                                          value = rowID;
                                      } else {
                                          value = SQLConversionTools.convertToString(this.getConnection(), ors, col);
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
                retList.add(new QueryRow(rowID, calValueMap, retSTRUCT));
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

}
