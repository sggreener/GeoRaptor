package org.GeoRaptor.SpatialView.layers;


import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.ResultSetMetaData;
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
import oracle.jdbc.OracleStatement;
import oracle.jdbc.OracleTypes;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.io.ExtensionFileFilter;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;

import org.w3c.dom.Node;


@SuppressWarnings("deprecation")
public class SVWorksheetLayer 
extends SVSpatialLayer 
{
    public static final String CLASS_NAME = Constants.KEY_SVWorksheetLayer;

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVWorksheetLayer");
    private String sql = "";
    
    public SVWorksheetLayer(SpatialView   _sView, 
                            String        _name, 
                            String        _screenName,
                            String        _desc, 
                            MetadataEntry _me, 
                            boolean       _draw,
                            String        _querySQL,
                            boolean       _computeMBR) 
    {
        super(_sView, _name, _screenName, _desc, _me, _draw);
        this.setSQL(_querySQL);
        super.setMBRRecalculation(_computeMBR);
        super.objectType = Constants.OBJECT_TYPES.ADHOCSQL;
    }

    /**
     * @constructor SVWorksheetLayer
     * @param _sView
     * @param _node
     * @author Simon Greener, December 6th, 2010
     *          -  XML Constructor
     */
    public SVWorksheetLayer(SpatialView _sView, Node _node) {
        super(_sView, _node);
        this.fromXMLNode(_node);
    }

    /** Copy constructor
     *
     **/
    public SVWorksheetLayer(SVWorksheetLayer _sLayer) 
    {
        super(_sLayer);
        LOGGER.debug("SVWorksheetLayer(SVWorksheetLayer _sLayer) copy constructor");
        try {
            // this.setLayerMBR(_sLayer.getLayerMBR());
            this.setSQL(_sLayer.getSQL());
        } catch (Exception e) {
            LOGGER.error("Worksheet Layer Copy Constructor: " + e.toString());
        }
    }

    @Override
    public String getClassName() {
        return SVWorksheetLayer.CLASS_NAME;
    }
    
    @Override
    public void setObjectType(String _objectType) {
        this.objectType =  Constants.OBJECT_TYPES.ADHOCSQL;
    }
    
    @Override
    public void setSQL(String _qSQL) {
        LOGGER.debug("setSQL(" + _qSQL + ")");
        this.sql = _qSQL;
    }

    @Override
    public String getSQL() {
        LOGGER.debug("getSQL(" + this.sql + ")");
        return this.sql;
    }

    @Override
    public String getLayerSQL() {
        LOGGER.debug("getLayerSQL(" + this.sql + ")");
        return this.sql;
    }
    
    @Override
    public String getInitSQL(String _geoColumn) {
        LOGGER.debug("SVWorksheetLayer: getInitSQL");
        return this.sql;
    }

    /**
     * Execute SQL and draw data on given graphical device
     * @param _mbr Coordinates of new Map MBR
     * @param _g2 graphical device
     * @return if return false, something was wrong (for example, Connection with DB faild)
     */
    @Override
    public boolean drawLayer(Envelope _mbr, 
                             Graphics2D      _g2) 
    {
        Envelope layerMBR = super.getMBR();
        LOGGER.debug("WorksheetLayer.drawLayer(" + _mbr.toString());
        if ( super.getMBRRecalculation() ) {
            LOGGER.info("Layer MBR will be recalculated.");
        } else if ( ! layerMBR.intersects(_mbr) ) {
            LOGGER.info(this.getVisibleName() + "'s MBR is outside map MBR: nothing to do.");
            return true;
        }

        OracleConnection conn = null;
        try {
            // Make sure layer's connection has not been lost
            conn = super.getConnection();
            if (conn == null) {
                return false;
            }
        } catch (IllegalStateException ise) {
            // Thrown by super.getConnection() indicates starting up, so we do nothing
            return false;
        }

        // Get SQL
        //
        String qSql = this.getSQL();
        if (Strings.isEmpty(qSql)) {
            return false;
        }

        // Has no parameters
        //
        OracleStatement stmt = null;
        OracleResultSet rSet = null;
        Envelope geoMBR = new Envelope(super.getDefaultPrecision()),
                   newLayerMBR = new Envelope(super.getDefaultPrecision());
        String     labelValue = null,
                   shadeValue = null,
              pointColorValue = null,
               lineColorValue = null;
        boolean isFastPickler = this.getPreferences().isFastPicklerConversion();
        double pointSizeValue = 4,
                   angleValue = 0.0f;
        String    sqlTypeName = "";
        STRUCT         stGeom = null;
        JGeometry       jgeom = null;
        byte[]    geomPickler = null;
        long    mbrCalcStart  = 0,
                  mbrCalcTime = 0,
                totalFeatures = 0,
                dataReadStart = System.currentTimeMillis(),
                 dataReadTime = 0,
                dataDrawStart = 0,
                 dataDrawTime = 0,
                 executeStart = 0,
                  executeTime = 0;    
        try {            
            // Set graphics2D once for all features
            drawTools.setGraphics2D(_g2);

            executeStart = System.currentTimeMillis();
            stmt = (OracleStatement)conn.createStatement();
            rSet = (OracleResultSet)stmt.executeQuery(qSql);
            rSet.setFetchDirection(OracleResultSet.FETCH_FORWARD);
            rSet.setFetchSize(this.getResultFetchSize());
            executeTime = ( System.currentTimeMillis() - executeStart );
            while ((rSet.next()) &&
                   (this.getSpatialView().getSVPanel().isCancelOperation() == false)) 
            {
                /// reading a geometry from database
                sqlTypeName = ((oracle.sql.STRUCT)rSet.getOracleObject(super.getGeoColumn().replace("\"",""))).getSQLTypeName();
                if ( isFastPickler && sqlTypeName.indexOf("MDSYS.ST_")==-1) {
                    geomPickler = rSet.getBytes(super.getGeoColumn().replace("\"",""));
                    if (geomPickler == null) continue;
                    //convert image into a JGeometry object using the SDO pickler
                    jgeom = JGeometry.load(geomPickler);
                } else {
                    stGeom = (oracle.sql.STRUCT)rSet.getOracleObject(super.getGeoColumn().replace("\"",""));
                    if (stGeom == null) continue;
                    // If ST_GEOMETRY, extract SDO_GEOMETRY
                    if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                        stGeom = SDO_GEOMETRY.getSdoFromST(stGeom);
                    } 
                    if (stGeom==null) {
                        jgeom = null;
                    }else {
                        jgeom = JGeometry.load(stGeom);
                    }
                }
                if (jgeom == null) {
                    continue;
                }
                geoMBR.setMBR(SDO_GEOMETRY.getGeoMBR(jgeom));
                if ( ! _mbr.intersects(geoMBR) ) {
                    continue;
                }
                if ( this.getMBRRecalculation() ) {
                    mbrCalcStart =  System.currentTimeMillis();
                    newLayerMBR.setMaxMBR(geoMBR);
                    mbrCalcTime += ( System.currentTimeMillis() - mbrCalcStart );
                }

                totalFeatures++;
                if (this.styling.getLabelColumn() != null) {
                    labelValue = SQLConversionTools.convertToString(this.getConnection(),this.styling.getLabelColumn(), 
                                                                    rSet.getObject(this.styling.getLabelColumn().replace("\"","")));
                }
                if (this.styling.getRotationColumn() != null) {
                    try {
                        angleValue = rSet.getNUMBER(this.styling.getRotationColumn().replace("\"","")).doubleValue();
                    } catch (Exception e) {
                        angleValue = 0.0f;
                    }
                }
                if (this.styling.getShadeColumn() != null && this.styling.getShadeType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        // regardless as to whether RGB or Integer, get colour as a string.
                        shadeValue = rSet.getString(this.styling.getShadeColumn().replace("\"",""));
                    } catch (Exception e) {
                        shadeValue = "255,255,255";
                    }
                } 
                if (this.styling.getPointColorColumn() != null && this.styling.getPointColorType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        // regardless as to whether RGB or Integer, get colour as a string.
                        pointColorValue = rSet.getString(this.styling.getPointColorColumn().replace("\"",""));
                    } catch (Exception e) {
                        pointColorValue = "255,255,255";
                    }
                } 
                if (this.styling.getLineColorColumn() != null && this.styling.getLineColorType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        // regardless as to whether RGB or Integer, get colour as a string.
                        lineColorValue = rSet.getString(this.styling.getLineColorColumn().replace("\"",""));
                    } catch (Exception e) {
                        lineColorValue = "0,0,0";
                    }
                }  
                if (this.styling.getPointSizeColumn() != null && this.styling.getPointSizeType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        pointSizeValue = rSet.getDouble(this.styling.getPointSizeColumn().replace("\"",""));
                    } catch (Exception e) {
                        pointSizeValue = 4;
                    }
                } 
                dataDrawStart = System.currentTimeMillis();
                callDrawFunction(jgeom, 
                                 labelValue, 
                                 shadeValue, 
                                 pointColorValue,
                                 lineColorValue,
                                 (int)(Math.round(pointSizeValue<4.0?4:pointSizeValue) % 72),
                                 this.styling.getRotationValue() == Constants.ROTATION_VALUES.DEGREES 
                                 ? COGO.radians(COGO.normalizeDegrees(angleValue - 90.0f)) 
                                 : angleValue);
                dataDrawTime += ( System.currentTimeMillis() - dataDrawStart );
                if ( super.preferences.isQueryLimited() && totalFeatures >= super.preferences.getQueryLimit() ) {
                    break;
                }
            }
            dataReadTime += (  System.currentTimeMillis() - dataReadStart );
            this.setNumberOfFeatures(totalFeatures);
            float featsPerSecond = ( this.getNumberOfFeatures() / (((float)(dataReadTime + executeTime) / (float)Constants.MILLISECONDS) % Constants.SECONDS) );
            LOGGER.logSQL("\n" + 
                           super.getSpatialView().getVisibleName() + ">>" + this.getVisibleName() + "\n" + 
                           ( this.getPreferences().isLogSearchStats() ? 
                           "SQL Execution Time = " + Tools.milliseconds2Time(executeTime) + "\n" : "" ) + 
                           "         Draw Time = " + Tools.milliseconds2Time(dataDrawTime)+ "\n" +
                           ( this.getMBRRecalculation() ? 
                           "     MBR Calc Time = " + Tools.milliseconds2Time(mbrCalcTime)+ "\n" : "" ) + 
                           "    Data Read Time = " + Tools.milliseconds2Time(dataReadTime - (dataDrawTime+mbrCalcTime)) + "\n" +
                           "   Total Read Time = " + Tools.milliseconds2Time(dataReadTime) + "\n" +
                           " Features Returned = " + this.getNumberOfFeatures()  + "\n" +
                           "   Features/Second = " + String.format("%10.2f",featsPerSecond));
    
            if ( this.getMBRRecalculation() ) {
                LOGGER.debug("  MBR Recalculation - Final processing " + newLayerMBR.toString());
                if (newLayerMBR.getWidth() == newLayerMBR.getHeight()) {
                  // Must be a single point
                  //
                  Point2D.Double pixelSize = this.getSpatialView().getSVPanel().getMapPanel().getPixelSize();
                  double maxBufferSize = Math.max(pixelSize.getX(),pixelSize.getY()) * this.getPreferences().getSearchPixels();
                  newLayerMBR.setChange(maxBufferSize);
                }
                LOGGER.debug("  MBR Recalculation - setMBR to " + newLayerMBR.toString());
                super.setMBR(newLayerMBR);
                super.setMBRRecalculation(false);
            }

        } catch (SQLException sqle) {
            // isView() then say no index
            if ( this.isView() ) {
                this.setIndex(false);
            }
            Tools.copyToClipboard(super.propertyManager.getMsg("SQL_QUERY_ERROR",
                                                               sqle.toString()),
                                  qSql);
            return false;
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null,
                                          super.propertyManager.getMsg("FILE_IO_ERROR",
                                                                       ioe.getLocalizedMessage()),
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (NullPointerException npe) {
            LOGGER.debug("NullPointerException - " + npe.toString());
            npe.printStackTrace();
            if ( conn != null ) { // Assume conn==null is a startup problem
                if ( this.getPreferences().isLogSearchStats() ) {
                    LOGGER.error("SVSpatialLayer.executeDrawQuery error: " + npe.getMessage());
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("SVSpatialLayer.executeDrawQuery - general exception");
            e.printStackTrace();
            return false;
        } finally {
            try { rSet.close(); } catch (Exception _e) { }
            try { stmt.close(); } catch (Exception _e) { }
        }
        return true;
    }

    @Override
    public LinkedHashMap<String, String> getColumnsAndTypes(boolean _onlyNumbersDatesAndStrings,
                                                            boolean _fullDataType) 
    throws SQLException 
    {
        // Should try and get columns etc from SQL being executed.
        // Otherwise go to table itself.
        //
        try {
            String layerSql = this.getSQL();
            if (Strings.isEmpty(layerSql)) {
                LOGGER.error("SQL statement not set for worksheet layer (" + this.getLayerName());
                return null;
            }
            LinkedHashMap<String, String> columnsTypes = new LinkedHashMap<String,String>(255);
            OracleConnection conn = super.getConnection();
            try {
                LOGGER.debug("SQL Before="+layerSql);
                OraclePreparedStatement pstmt;
                pstmt = (OraclePreparedStatement)conn.prepareStatement(layerSql);
                LOGGER.debug("prepared");
                boolean include = false;
                String     colName = "",
                       colTypeName = "";
                ResultSetMetaData rsmd = pstmt.getMetaData();
                LOGGER.debug("getMetaData()");
                for (int col=1;col<=rsmd.getColumnCount();col++) {
                    include     = false;
                    colName     = rsmd.getColumnLabel(col);
                    colTypeName = rsmd.getColumnTypeName(col);
                    LOGGER.debug("columnsTypes.put("+colName+","+colTypeName+")");
                    if ( _onlyNumbersDatesAndStrings ) {
                        include = colTypeName.startsWith("TIMESTAMP") ||
                                  colTypeName.startsWith("INTERVAL")  ||
                                  colTypeName.equalsIgnoreCase("DATE") ||
                                  colTypeName.equalsIgnoreCase("CHAR") ||
                                  colTypeName.equalsIgnoreCase("NVARCHAR2") ||
                                  colTypeName.equalsIgnoreCase("VARCHAR") ||
                                  colTypeName.equalsIgnoreCase("VARCHAR2") ||
                                  colTypeName.equalsIgnoreCase("ROWID") ||
                                  colTypeName.equalsIgnoreCase("FLOAT") ||
                                  colTypeName.equalsIgnoreCase("BINARY_DOUBLE") ||
                                  colTypeName.equalsIgnoreCase("BINARY_FLOAT") ||
                                  colTypeName.equalsIgnoreCase("NUMBER");
                    } else {
                        include = true;
                    }
                    if ( include ) {
                        if ( _fullDataType ) {
                            if (rsmd.getPrecision(col)==0) {
                                colTypeName += (colTypeName.contains("CHAR") ? "(" + rsmd.getPrecision(col) + ")" : "");
                            } else if (rsmd.getScale(col) == 0 ) {
                                  colTypeName += "(" + rsmd.getPrecision(col) + ")" ;
                            } else {
                                  colTypeName += "(" + rsmd.getPrecision(col) + "," + rsmd.getScale(col) + ')';
                            }
                        }
                        LOGGER.debug("columnsTypes.put(\""+colName+"\"," +colTypeName+")");
                        columnsTypes.put((String)"\""+colName+"\"",colTypeName);
                    }
                }
                pstmt.close();
            } catch (SQLException e) {
                LOGGER.debug("Problem checking draw SQL - " + e.toString());
            }
            return columnsTypes;
        } catch (Exception ex) {
            LOGGER.error(super.propertyManager.getMsg("QUERY_ERROR_COLUMNS_AND_TYPES",
                                                      this.getObjectName(),
                                                      ex.getMessage()));
            return null;
        }
    }

    //@Override
    public ArrayList<QueryRow> queryByPoint(Point2D _worldPoint, 
                                            int     _numSearchPixels) 
    {
        LOGGER.debug("queryByPoint");
        int numPixels = _numSearchPixels == 0 ? 3 : _numSearchPixels;

        // list of return rows
        //
        ArrayList<QueryRow> retList = new ArrayList<QueryRow>(); 
        Envelope   mbr = new Envelope(super.getDefaultPrecision());
        
        OracleConnection oConn = null;
        try {
            // Make sure layer's connection has not been lost
            oConn = super.getConnection();
            if (oConn == null) {
                return retList;
            }
        } catch (IllegalStateException ise) {
            // Thrown by super.getConnection() indicates starting up, so we do nothing
            return retList;
        }

        OracleStatement pStmt = null;
        OracleResultSet  rSet = null;
        try {
            // Get search radius
            // Is diagonal distance computed as sqrt of size of pixel (* number of pixels in search)
            // in X and Y squared (ie pythagoras)
            //
            Point.Double pixelSize = this.spatialView.getMapPanel().getPixelSize();
LOGGER.debug("queryByPoint pixelSize: "+pixelSize.toString());
            mbr.setMBR(_worldPoint.getX() - pixelSize.getX() * numPixels,
                       _worldPoint.getY() - pixelSize.getY() * numPixels,
                       _worldPoint.getX() + pixelSize.getX() * numPixels,
                       _worldPoint.getY() + pixelSize.getY() * numPixels);
LOGGER.debug("queryByPoint mbr: "+mbr.toString());
            // Get SQL
            //
            String qSql = this.getSQL();
            if (Strings.isEmpty(qSql)) {
                return null;
            }
LOGGER.debug("queryByPoint sql: "+qSql);

            long executeStart = 0,
                  executeTime = 0;

            executeStart = System.currentTimeMillis();

            pStmt = (OracleStatement)oConn.createStatement();
            rSet = (OracleResultSet)pStmt.executeQuery(qSql);
            rSet.setFetchDirection(OracleResultSet.FETCH_FORWARD);
            rSet.setFetchSize(this.getResultFetchSize());
            OracleResultSetMetaData rSetM = (OracleResultSetMetaData)rSet.getMetaData(); // for column name

            executeTime = ( System.currentTimeMillis() - executeStart );

LOGGER.debug("SQL executed in " + executeTime);
            
            STRUCT     stGeom = null;
            long dataReadTime = 0,
                dataReadStart = 0,
                totalFeatures = 0;
            String   rowID = "",
                     value = "",
                columnName = "",
            columnTypeName = "";
            String geoColumn = super.getGeoColumn();
            Object objValue = null;
            
            // Process Rows
            while ((rSet.next()) &&
                   (this.getSpatialView().getSVPanel().isCancelOperation() == false)) 
            {
LOGGER.debug("queryByPoint row number: "+rSet.getRow());
                LinkedHashMap<String, Object> colValueMap = new LinkedHashMap<String, Object>(rSetM.getColumnCount() - 1);
                stGeom = null;
                // Process columns
                for (int col = 1; col <= rSetM.getColumnCount(); col++) {
                    columnName     = rSetM.getColumnName(col);
                    columnTypeName = rSetM.getColumnTypeName(col);
LOGGER.debug("queryByPoint columnName: "+columnName+ " columnTypeName="+columnTypeName);
                    if (columnName.equalsIgnoreCase(geoColumn)) {
                        stGeom = (oracle.sql.STRUCT)rSet.getObject(col);
                        // If ST_GEOMETRY, extract SDO_GEOMETRY
                        if ( stGeom.getSQLTypeName().indexOf("MDSYS.ST_")==0 ) {
                            stGeom = SDO_GEOMETRY.getSdoFromST(stGeom);
                        } 
                        if (rSet.wasNull() || stGeom==null) {
                            stGeom = null;
                            break; 
                        }
LOGGER.debug("queryByPoint testing mbr intersection with geometry");
                        // Surrogate search: if MBR of feature contains search point + search distance MBR then return it
                        if (! mbr.intersects(SDO_GEOMETRY.getGeoMBR(stGeom))) {
                            stGeom = null; // make sure not added to result attributes
                            break;
                        }
                    } else {
                        if ( Tools.dataTypeIsSupported(columnTypeName) ) {
LOGGER.debug("data type is supported.");
                              try
                              {
                                  objValue = rSet.getObject(col);
                                  if (rSet.wasNull()) {
                                      value = "NULL";
                                  } else {
                                      if ( rSetM.getColumnType(col) == OracleTypes.ROWID ) {
                                          rowID = rSet.getROWID(col).stringValue(); 
                                          value = rowID;
                                      } else {
                                          value = SQLConversionTools.convertToString(oConn, columnName, objValue);
                                          if (value == null) {
                                              value = "NULL";
                                          }
                                      }
                                  }
                              } catch (Exception e) {
                                  value = "NULL";
                              }
LOGGER.debug("calValueMap.put("+rSetM.getColumnName(col)+","+value+")");
                              colValueMap.put(rSetM.getColumnName(col), value);
LOGGER.debug("calValueMap.put() is done");
                          }
                    }
                }
                if (stGeom != null) { 
LOGGER.debug("retList.add(QueryRow())");
                    retList.add(new QueryRow(rowID, colValueMap, stGeom));
LOGGER.debug("retList.add(QueryRow()) - DONE");
                }
            }
            dataReadTime += (  System.currentTimeMillis() - dataReadStart );
            this.setNumberOfFeatures(totalFeatures);
            float featsPerSecond = ( this.getNumberOfFeatures() / (((float)(dataReadTime + executeTime) / (float)Constants.MILLISECONDS) % Constants.SECONDS) );
            LOGGER.logSQL(qSql + "\n" + 
                           super.getSpatialView().getVisibleName() + ">>" + this.getVisibleName() + "\n" + 
                           ( this.getPreferences().isLogSearchStats() ? 
                           "SQL Execution Time = " + Tools.milliseconds2Time(executeTime) + "\n" : "" ) + 
                           " Features Returned = " + this.getNumberOfFeatures()  + "\n" +
                           "   Features/Second = " + String.format("%10.2f",featsPerSecond)); 
        } catch (SQLException sqle) {
            LOGGER.error(super.propertyManager.getMsg("SQL_QUERY_ERROR",sqle.toString()));
        } catch (NullPointerException npe) {
            LOGGER.error("NullPointerException - " + npe.toString());
            npe.printStackTrace();
        } catch (NoninvertibleTransformException nte) {
            LOGGER.error(this.getSpatialView().getSVPanel().getMapPanel().getPropertyManager().getMsg("ERROR_SCREEN2WORLD_TRANSFORM")+ "\n" + nte.getLocalizedMessage() );
        } finally {
            try { rSet.close(); } catch (Exception _e) { }
            try { pStmt.close(); } catch (Exception _e) { }
        } 
        LOGGER.debug("returning " + (retList==null?0:retList.size()) +" features.");
        return retList;
    }
    
    @Override
    public void setMinResolution(boolean _minResolution) {
        this.minResolution = false; // Always false for SVWorksheetLayers.
    }
    
    @Override
    public boolean getMinResolution() {
        return this.minResolution;
    }

    private void fromXMLNode(Node _node) {
        if (_node == null || _node.getNodeName().equals("Layer") == false) {
            //System.out.println("XML node is null or not a Layer");
            return; // Should throw error
        }
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            this.setSQL((String)xpath.evaluate("SVWorksheetLayer/SQL/text()", _node, XPathConstants.STRING));
        } catch (XPathExpressionException xe) {
            LOGGER.error("Error loading Query layer XML (" + xe.getMessage() + ")");
        }
    }

    @Override
    public String toXML() {
        String SVLayer_SpatialLayerXML = super.toXML();
        return SVLayer_SpatialLayerXML + String.format("<SVWorksheetLayer><SQL>%s</SQL></SVWorksheetLayer>",this.getSQL());
    }

    @Override
    public void savePropertiesToDisk() {
        String saveXML = this.toXML();
        saveXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<Layers><Layer>" + saveXML + "</Layer></Layers>";
        // Now ask where to save and file name....
        try {
            FileFilter xmlFilter = new ExtensionFileFilter("XML", new String[] { "XML" });
            File f = new File(this.getLayerName().replace(".", "_") + ".xml");
            JFileChooser fc = new JFileChooser() {
				private static final long serialVersionUID = -6228770285734305100L;

				@Override
                public void approveSelection() {
                    File f = getSelectedFile();
                    if (f.exists() && getDialogType() == SAVE_DIALOG) {
                        int result = JOptionPane.showConfirmDialog(this, propertyManager.getMsg("CONFIRM_FILE_EXISTS_PROMPT"), propertyManager.getMsg("CONFIRM_FILE_EXISTS_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION);
                        switch (result) {
                            case JOptionPane.YES_OPTION:
                                super.approveSelection();
                                return;
                            case JOptionPane.NO_OPTION:
                            case JOptionPane.CLOSED_OPTION:
                                return;
                            case JOptionPane.CANCEL_OPTION:
                                cancelSelection();
                                return;
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
                LOGGER.fine(this.propertyManager.getMsg("FILE_SAVING", file.getName()));
                FileWriter fw;
                try {
                    fw = new FileWriter(file.getAbsoluteFile());
                    BufferedWriter out = new BufferedWriter(fw);
                    out.write(saveXML);
                    //Close the output stream
                    out.close();
                    LOGGER.fine(this.propertyManager.getMsg("FILE_SAVE_THEME", this.getLayerName()));
                } catch (IOException e) {
                    LOGGER.error(this.propertyManager.getMsg("FILE_SAVE_ERROR", file.getName(), e.getMessage()));
                }
            }
        } catch (Exception e) { //Catch exception if any
            LOGGER.error("Error: " + e.getMessage());
        }
    }
}
