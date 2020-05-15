package org.GeoRaptor.SpatialView.layers;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import oracle.jdbc.OracleConnection;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.NUMBER;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.MathUtils;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;


public class SVGraphicLayer extends SVSpatialLayer 
{

    public static final String CLASS_NAME = Constants.KEY_SVGraphicLayer;
    
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVGraphicLayer");
    
    protected List<QueryRow>     cache = new ArrayList<QueryRow>();

    public SVGraphicLayer(SpatialView   _sView, 
                          String        _name, 
                          String        _screenName,
                          String        _desc, 
                          MetadataEntry _me, 
                          boolean       _draw) 
    {
        super(_sView, _name, _screenName, _desc, _me, _draw);
    }

    /** Create from existing superclass
     *
     **/
    public SVGraphicLayer(SVSpatialLayer _sLayer) 
    {
        super(_sLayer);
        // Change base name
        if ( this.getSpatialView() != null ) {
            this.setLayerName(this.getSpatialView().checkLayerName(super.getLayerName()));
        }
        // This is an informational query of actual data: filtering would make the result invalid
        this.setMinResolution(false);
    }

    @Override
    public String getClassName() {
        return SVGraphicLayer.CLASS_NAME;
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
        OracleConnection conn = null;
        try {
            // Make sure layer's connection has not been lost
            conn = super.getConnection();
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
            String label =
                Strings.isEmpty(this.getStyling().getLabelColumn()) ? null : this.getStyling().getLabelColumn();            
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
            boolean bRotate = (Strings.isEmpty(super.getStyling().getRotationColumn()) ? false : true); 

            // Set graphics2D once for all features
            drawTools.setGraphics2D(_g2);

            // Now process and draw all JGeometry objects in the cache.
            //
            JGeometry geo = null;
            long numFeats = 0;
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
                if ((_mbr.isSet() && _mbr.overlaps(SDO_GEOMETRY.getGeoMBR(geo))) || _mbr.isNull()) 
                {
                    numFeats++;
                    // Draw the geometry using current (probably default) display settings
                    super.callDrawFunction(geo,
                                           (bLabel      ? SQLConversionTools.convertToString(conn,label,         qrow.getAttData().get(label)) : ""),
                                           (bShade      ? SQLConversionTools.convertToString(conn,shadeCol,      qrow.getAttData().get(shadeCol)) : ""),
                                           (bPointColor ? SQLConversionTools.convertToString(conn,pointColorCol, qrow.getAttData().get(pointColorCol)) : ""),
                                           (bLineColor  ? SQLConversionTools.convertToString(conn,lineColorCol,  qrow.getAttData().get(lineColorCol)) : ""),
                                           (bPointSize  ? MathUtils.numberToInt(qrow.getAttData().get(pointSizeCol),4) : 4 ),
                                           (bRotate     ? (super.getStyling().getRotationValue() == Constants.ROTATION_VALUES.DEGREES 
                                                           ? COGO.radians(COGO.normalizeDegrees(((NUMBER)qrow.getAttData().get(super.getStyling().getRotationColumn())).doubleValue() - 90.0f)) 
                                                           : ((NUMBER)qrow.getAttData().get(super.getStyling().getRotationColumn())).doubleValue())
                                                        : 0.0f));
                }
            }
            if ( super.calculateMBR ) {
                this.setLayerMBR();
            }
            this.setNumberOfFeatures(numFeats);
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

    public SVGraphicLayer createCopy(boolean _renderCopy) 
    throws Exception {
        if ( _renderCopy ) {
          SVGraphicLayer newLayer = null;
          newLayer = new SVGraphicLayer(super.getSpatialView(),
                                        super.getSpatialView().getSVPanel().getViewLayerTree().checkName(this.getLayerName()),
                                        super.getVisibleName(),
                                        super.getDesc(),
                                        super.getMetadataEntry(),
                                        super.isDraw());
          newLayer.setMBR(this.getMBR());
          newLayer.setSRIDType(super.getSRIDType());
          newLayer.setGeometryType(this.getGeometryType());
          newLayer.setPrecision(this.getPrecision(false));
          newLayer.setIndex(this.hasIndex());
          newLayer.setStyling(new Styling(this.getStyling()));
          return newLayer;
        } else {
            throw new Exception(super.getPropertyManager().getMsg("GRAPHIC_THEME_COPY"));
        }
    }

    public void setConnection() {
        // Stops call to SVLayer to set the oracle connection
    }

    public String getConnectionName() {
        // Stops call to SVLayer to get the oracle connection
        return "";
    }

    public void setIndex() {
        // Stops call to parent
        this.indexExists = true;
    }
    
    public void setIndex(String _hasIndex) {
        // Stops call to parent
        this.indexExists = true;
    }
  
    public void setIndex(boolean _indexExists) {
       // Stops call to parent
       this.indexExists = true;
    }
    
    public boolean hasIndex() {
        return true;
    }

    public void setSQL(String _layerSQL) {
        // do nothing
    }

    public String getSQL() {
        // do nothing
        return null;
    }

    public void setInitSQL() {
        // Stops call to parent
    }

    public String getInitSQL() {
        return "";
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

    public void setLayerMBR() {
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

    public Envelope getLayerMBR() {
        return new Envelope(this.mbr);
    }

    public List<QueryRow> getCache() {
        return this.cache;
    }

    public long getCacheCount() {
        return this.cache==null?0:this.cache.size();
    }

    public void setNumberOfFeatures(long _number) {
        LOGGER.debug("SVGraphicTheme("+this.getLayerName()+" _number="+_number);
        super.setNumberOfFeatures(_number);
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
    
    @Override
    public ArrayList<QueryRow> queryByPoint(Point2D _worldPoint, int _numSearchPixels) 
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

}
