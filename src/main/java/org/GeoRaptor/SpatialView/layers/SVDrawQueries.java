package org.GeoRaptor.SpatialView.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.JGeom;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.geotools.util.logging.Logger;

import oracle.spatial.geometry.JGeometry;

public class SVDrawQueries {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVDrawQueries");

    public static LinkedHashMap<String,String> 
                  getAttributesFromMetadata (
    		         ResultSetMetaData _rsmd,
    		         boolean           _onlyNumbersDatesAndStrings,
    		         boolean           _fullDataType)
    {
    	// _rsmd is value
    	if ( _rsmd ==  null )
    		return null;

    	LinkedHashMap<String,String> columnsTypes = new LinkedHashMap<String,String>();
    	try 
    	{
    		boolean include = false;
    		String  colName = "",
                colTypeName = "";
        
            for (int col=1;col<=_rsmd.getColumnCount();col++) 
            {
            	include     = false;
            	colName     = _rsmd.getColumnLabel(col);
            	colTypeName = _rsmd.getColumnTypeName(col);
            	
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
            			if (_rsmd.getPrecision(col)==0) {
            				colTypeName += (colTypeName.contains("CHAR") ? "(" + _rsmd.getPrecision(col) + ")" : "");
            			} else if (_rsmd.getScale(col) == 0 ) {
            				colTypeName += "(" + _rsmd.getPrecision(col) + ")" ;
            			} else {
                            colTypeName += "(" + _rsmd.getPrecision(col) + "," + _rsmd.getScale(col) + ')';
                        }
                    }
                    columnsTypes.put(Strings.doubleQuote(colName),colTypeName);
                }
            } // for 
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return columnsTypes;
    }

    public static ArrayList<String> 
                       getStylingColumns (
                          ResultSetMetaData _rsmd,
    		              Styling          _styling)
    {
		ArrayList<String> columns = new ArrayList<String>();
		if ( _rsmd == null )
			return new ArrayList<String>();
    	try 
    	{
    		String colName = null;
    		for (int i=1;i<_rsmd.getColumnCount();i++)
    		{
    			colName = _rsmd.getColumnLabel(i);
    			if (_styling.getLabelColumn()           != null && _styling.getLabelColumn().replaceAll("\"","").equalsIgnoreCase(colName))
    				columns.add(Strings.doubleQuote(colName));
    			else if (_styling.getRotationColumn()   != null && _styling.getRotationColumn().replaceAll("\"","").equalsIgnoreCase(colName))
    				columns.add(Strings.doubleQuote(colName));
    			else if (_styling.getPointColorColumn() != null && _styling.getPointColorColumn().replaceAll("\"","").equalsIgnoreCase(colName))
    				columns.add(Strings.doubleQuote(colName));
    			else if (_styling.getLineColorColumn()  != null &&_styling.getLineColorColumn().replaceAll("\"","").equalsIgnoreCase(colName))
    				columns.add(Strings.doubleQuote(colName));
    			else if (_styling.getShadeColumn()      != null && _styling.getShadeColumn().replaceAll("\"","").equalsIgnoreCase(colName))
    				columns.add(Strings.doubleQuote(colName));
    			else if (_styling.getPointSizeColumn()  != null && _styling.getPointSizeColumn().replaceAll("\"","").equalsIgnoreCase(colName))
    				columns.add(Strings.doubleQuote(colName));
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return columns;
    }
    
    public static boolean executeQuery(
                                 iLayer             _layer,
                                 PreparedStatement  _pStatement,
                                 String             _sql2Debug,
                                 Graphics2D         _g2,
                                 SVSpatialLayerDraw _drawTools
                          )
    {
        if ( _g2 == null ) {
            return false;
        }
        if ( _layer == null ) {
            return false;
        }
        // connection needed for 
        ResultSet         ors = null;
        Envelope       newMBR = new Envelope(_layer.getSpatialView().getDefaultPrecision());
        Styling       styling = _layer.getStyling();
        String     labelValue = null,
                   shadeValue = styling.getShadeColor(null).toString(),
              pointColorValue = styling.getPointColor(null).toString(),
               lineColorValue = styling.getLineColor(null).toString(),
                  sqlTypeName = "";
        Color      shadeColor = null, 
                    lineColor = null, 
                   pointColor = null;
        double pointSizeValue = styling.getPointSize(4),
                   angleValue = 0.0f;
        
        Struct         stGeom = null;
        JGeometry       jGeom = null;
        boolean isFastPickler = _layer.getPreferences().isFastPicklerConversion();
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
        try 
        {
            Connection oConn = (Connection)_pStatement.getConnection();
            
            // Set graphics2D once for all features
            _layer.getDrawTools().setGraphics2D(_g2);
            
            executeStart = System.currentTimeMillis();
            
            ors = _pStatement.executeQuery();
            ors.setFetchDirection(ResultSet.FETCH_FORWARD);
            ors.setFetchSize(_layer.getResultFetchSize());
            
            ResultSetMetaData rsmd = ors.getMetaData(); // for column name
            ArrayList<String> labelColumns = getStylingColumns(rsmd,styling);            

            // Computed MBR will be in SRID of view NOT layer
            newMBR.setSRID(_layer.getSpatialView().getSRIDAsInteger());

            executeTime = ( System.currentTimeMillis() - executeStart );
            while ((ors.next()) &&
                   (_layer.getSpatialView().getSVPanel().isCancelOperation() == false)) 
            {
                /// reading a geometry from database       
                sqlTypeName = ((java.sql.Struct)ors.getObject(_layer.getGeoColumn().replace("\"",""))).getSQLTypeName();
                if ( isFastPickler && sqlTypeName.indexOf("MDSYS.ST_")==-1) {
                    geomPickler = ors.getBytes(_layer.getGeoColumn().replace("\"",""));
                    if (geomPickler == null) { continue; }
                    //convert image into a JGeometry object using the SDO pickler
                    jGeom = JGeometry.load(geomPickler);
                } else {
                    stGeom = (java.sql.Struct)ors.getObject(_layer.getGeoColumn().replace("\"",""));
                    if (stGeom == null) continue;
                    // If ST_GEOMETRY, extract SDO_GEOMETRY
                    if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                        stGeom = SDO_GEOMETRY.getSdoFromST(stGeom);
                    } 
                    if (stGeom==null) { continue; }
                    jGeom = JGeometry.loadJS(stGeom);
                }
                if (jGeom == null) continue;
                
                totalFeatures++;

                // Access styling for this draw....
                //
                if (styling.getLabelColumn() != null 
                 && labelColumns.indexOf(styling.getLabelColumn()) != -1 ) {
                    labelValue = SQLConversionTools.convertToString(
                    		        oConn,
                    		        styling.getLabelColumn(), 
                    		        ors.getObject(styling.getLabelColumn().replace("\"","")));
                }
                
                if (styling.getRotationColumn() != null
                    && labelColumns.indexOf(styling.getRotationColumn()) != -1 ) {
                    try {
                        angleValue = ors.getDouble(styling.getRotationColumn().replace("\"",""));
                    } catch (Exception e) {
                        angleValue = 0.0f;
                    }
                }
                
                if (styling.getPointColorColumn() != null
                 && labelColumns.indexOf(styling.getPointColorColumn()) != -1 
                 && styling.getPointColorType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        // regardless as to whether RGB or Integer, get colour as a string.
                        pointColorValue = ors.getString(styling.getPointColorColumn().replace("\"",""));
                    } catch (Exception e) {
                        pointColorValue = Colours.transparentBlackRGBa;
                    }
                    pointColor = Colours.fromRGBa(pointColorValue);
                }
                
                if (styling.getLineColorColumn() != null  
                 && labelColumns.indexOf(styling.getLineColorColumn()) != -1 
                 && styling.getLineColorType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        // regardless as to whether RGB or Integer, get colour as a string.
                        lineColorValue = ors.getString(styling.getLineColorColumn().replace("\"",""));
                    } catch (Exception e) {
                        lineColorValue = Colours.transparentBlackRGBa;
                    }
                    lineColor = Colours.fromRGBa(lineColorValue);
                }

                if (styling.getShadeColumn() != null 
                 && labelColumns.indexOf(styling.getShadeColumn()) != -1 
                 && styling.getShadeType() == Styling.STYLING_TYPE.COLUMN) 
                {
                	try 
                	{
                		// regardless as to whether RGB or Integer, get colour as a string.
                        shadeValue = ors.getString(styling.getShadeColumn().replace("\"",""));
                    } catch (Exception e) {
                        shadeValue = Colours.transparentBlackRGBa;
                    }
                    shadeColor = Colours.fromRGBa(shadeValue);
                }
                    
                if (styling.getPointSizeColumn() != null  
                 && labelColumns.indexOf(styling.getPointSizeColumn()) != -1 
                 && styling.getPointSizeType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        pointSizeValue = ors.getDouble(styling.getPointSizeColumn().replace("\"",""));
                    } catch (Exception e) {
                        pointSizeValue = styling.getPointSize(4);
                    }
                }
                
                // Draw the feature
                //
                _drawTools.callDrawFunction(
                		         jGeom,
                		         styling,
                                 labelValue, 
                                 shadeColor, 
                                 pointColor,
                                 lineColor,
                                 (int)(Math.round(pointSizeValue<4.0?4:pointSizeValue) % 72),
                                 styling.getRotationValue() 
                                    == 
                                    Constants.ROTATION_VALUES.DEGREES 
                                         ? COGO.radians(COGO.normalizeDegrees(angleValue - 90.0f)) 
                                         : angleValue);
                dataDrawTime += ( System.currentTimeMillis() - dataDrawStart );
                
                // Check if we are re-calculating the layer's MBR
                // The values in the jGeom will be in the View's SRID units 
                //
                if ( _layer.getMBRRecalculation() ) {
                    mbrCalcStart =  System.currentTimeMillis();
                    newMBR.setMaxMBR(JGeom.getGeoMBR(jGeom));
                    mbrCalcTime += ( System.currentTimeMillis() - mbrCalcStart );
                }
                
                if ( _layer.getPreferences().isQueryLimited() && 
                     totalFeatures >= _layer.getPreferences().getQueryLimit() ) {
                    break;
                }
                
            } // while feature to process
            dataReadTime += (  System.currentTimeMillis() - dataReadStart );
            
            _layer.setNumberOfFeatures(totalFeatures);
            
            float featsPerSecond = ( _layer.getNumberOfFeatures() / (((float)(dataReadTime + executeTime) / (float)Constants.MILLISECONDS) % Constants.SECONDS) );
            
            LOGGER.logSQL("\n" + 
                           _layer.getSpatialView().getVisibleName() + ">>" + _layer.getVisibleName() + "\n" + 
                           ( _layer.getPreferences().isLogSearchStats() ? 
                           "SQL Execution Time = " + Tools.milliseconds2Time(executeTime) + "\n" : "" ) + 
                           "         Draw Time = " + Tools.milliseconds2Time(dataDrawTime)+ "\n" +
                           ( _layer.getMBRRecalculation() ? 
                           "     MBR Calc Time = " + Tools.milliseconds2Time(mbrCalcTime)+ "\n" : "" ) + 
                           "    Data Read Time = " + Tools.milliseconds2Time(dataReadTime - (dataDrawTime+mbrCalcTime)) + "\n" +
                           "   Total Read Time = " + Tools.milliseconds2Time(dataReadTime) + "\n" +
                           " Features Returned = " + _layer.getNumberOfFeatures()  + "\n" +
                           "   Features/Second = " + String.format("%10.2f",featsPerSecond));
            
            if ( _layer.getMBRRecalculation() ) {
                if (newMBR.getWidth() == newMBR.getHeight()) {
                  // Must be a single point
                  //
                  Point2D.Double pixelSize = _layer.getSpatialView().getSVPanel().getMapPanel().getPixelSize();
                  double maxBufferSize = Math.max(pixelSize.getX(),pixelSize.getY()) * _layer.getPreferences().getSearchPixels();
                  newMBR.setChange(maxBufferSize);
                }
                _layer.setMBR(newMBR);
                _layer.setMBRRecalculation(false);
            }
            
        } catch (SQLException sqle) {
            // isView() then say no index
            if ( _layer.isView() ) { _layer.setIndex(false); };
            String params = "";
            ParameterMetaData pmd = null;
            try {
                pmd = _pStatement.getParameterMetaData();
                for (int i=1;i<=pmd.getParameterCount();i++) {
                    params += "\n" + pmd.getParameterTypeName(i);
                }
            } catch (SQLException e) {
            }
            Tools.copyToClipboard(_layer.getPropertyManager().getMsg("SQL_QUERY_ERROR",
                                                                     sqle.toString()),
                                  _sql2Debug + params);
            return false;
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null,
                                          _layer.getPropertyManager().getMsg("FILE_IO_ERROR",
                                                                             ioe.getLocalizedMessage()),
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (NullPointerException npe) {
            LOGGER.debug("**** NullPointerException - " + npe.toString());
            npe.printStackTrace();
            LOGGER.debug("** FINISH: executeDrawQuery\n========================");
            return false;
        } catch (Exception e) {
            LOGGER.error("SVSpatialLayer.executeDrawQuery - general exception");
            e.printStackTrace();
            LOGGER.debug("** FINISH: executeDrawQuery\n========================");
            return false;
        } finally {
            try { ors.close();        } catch (Exception _e) { }
            try { _pStatement.close(); } catch (Exception _e) { }
        }
        return true;
    }

}
