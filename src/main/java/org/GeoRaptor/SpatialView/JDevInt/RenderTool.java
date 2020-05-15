package org.GeoRaptor.SpatialView.JDevInt;

import java.sql.SQLException;
import java.sql.Struct;
import java.text.DecimalFormat;

import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.Datum;
import oracle.sql.STRUCT;
import oracle.sql.NUMBER;
import oracle.sql.ARRAY;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.tools.SDO_GEOMETRY;

import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;

@SuppressWarnings("deprecation")
public class RenderTool {

    @SuppressWarnings("unused")
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.JDevInt.RenderTool");

    /**
     * For access to preferences
     */
    protected static Preferences GeoRaptorPrefs;

    private static final String NULL_COLOUR          = "#003366";
    private static final String VERTEX_TYPE_COLOUR   = "#008411";
    
    private static final String GTYPE_COLOUR         = "#ff0000";
    private static final String SRID_COLOUR          = "#008400";
    private static final String SDO_POINT_COLOUR     = "#008411";
    
    private static final String SDO_ELEM_ARRAY_COLOUR = "#008411";
    private static final String ETYPE_COLOUR          = "#000000";
    private static final String ELEM_ORD_COLOUR       = "#0000ff";
    
    private static final String SDO_ORD_ARRAY_COLOUR  = "#008411";
    private static final String COORD_BRACKET_COLOUR  = "#ff0000";
    private static final String ORD_COLOUR            = "#0000ff";
    
    private static final String FONT_START            = "<B><font color=\"%s\">";
    private static final String FONT_END              = "</font></B>";
    private static final String FONT_NULL             = "<B><font color=\"" + NULL_COLOUR + "\">NULL" + FONT_END;
    private static final String FONT_COORD_SUPERSCRIPT = "<font color=\"black\" size=\"2\"><sup>###</sup></font>";

    public RenderTool() {
        super();
    }

    public static String htmlWrap(String _inHTML) 
    {
        return "<html><body style=\"background:transparent\">" + _inHTML + "</body></html>";
    }
  
    public static boolean isSupported(Object _value) {
        try {
            if ( _value instanceof oracle.sql.STRUCT ) {
                STRUCT stValue = (STRUCT)_value;
                return ( stValue.getSQLTypeName().indexOf("MDSYS.ST_")==0 ||
                         stValue.getSQLTypeName().equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ||
                         stValue.getSQLTypeName().equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) ||
                         stValue.getSQLTypeName().equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) );
            } else if (_value instanceof oracle.sql.ARRAY) {
                ARRAY aryValue = (ARRAY)_value;
                return ( aryValue.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_DIMARRAY) ||
                         aryValue.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_ELEM_ARRAY) ||
                         aryValue.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_ORD_ARRAY));
            }
        } catch (SQLException sqle) {
          return false;
        }
        return false;
    }
    
    public static String renderSTRUCTAsPlainText(Struct                _struct,
    		                                     Constants.bracketType _bracket,
                                                 int                   _ordPrecision)
           throws SQLException {
      return renderSTRUCT((STRUCT)_struct, false, _bracket, false, _ordPrecision);
    }


    /**
     * @param st
     * @return
     * @throws SQLException
     */
    public static String renderSTRUCTAsPlainText(STRUCT                _struct,
                                                 Constants.bracketType _bracket,
                                                 int                   _ordPrecision)
    throws SQLException {
        return renderSTRUCT(_struct, false, _bracket, false, _ordPrecision);
    }

    public static String renderSTRUCTAsHTML(STRUCT                _struct, 
                                            Constants.bracketType _bracket,
                                            boolean               _coordNumbering,
                                            int                   _ordPrecision) 
    throws SQLException {
        return renderSTRUCT(_struct, true, _bracket, _coordNumbering, _ordPrecision);
    }

    /**
     * @param _struct
     * @return
     * @throws SQLException
     * @author Simon Greener April 13th 2010
     *          Changed to use Constants class
     * @author Simon Greener May 28th 2010
     *          Changed GTYPE assignment so that it defaults to 2D if not provided.
     */
    public static String renderSTRUCT(STRUCT                _struct,
                                      boolean               _renderAsHTML,
                                      Constants.bracketType _bracket,
                                      boolean               _coordNumbering,
                                      int                   _ordPrecision) 
    throws SQLException 
    {
        // Note Returning null for null sdo_geometry structure
        if (_struct == null) {
            return null;
        }
        STRUCT stGeom = _struct;
        String sqlTypeName = _struct.getSQLTypeName();
        if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
            stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
        } 
        final int    GTYPE = SDO_GEOMETRY.getFullGType(stGeom,0);
        final int    SRID = SDO_GEOMETRY.getSRID(stGeom);
        final double POINT[] = SDO_GEOMETRY.getSdoPoint(stGeom);
        final int    ELEMINFO[] = SDO_GEOMETRY.getSdoElemInfo(stGeom);
        final double ORDINATES[] = SDO_GEOMETRY.getSdoOrdinates(stGeom);
        return (_renderAsHTML ? renderGeometryElementsAsHTML(sqlTypeName, GTYPE, SRID, POINT, ELEMINFO, ORDINATES, _bracket, _coordNumbering, _ordPrecision)
                              : renderGeometryElements      (sqlTypeName, GTYPE, SRID, POINT, ELEMINFO, ORDINATES, _bracket, _ordPrecision) );
    }

    /**
     * @param geom
     * @return
     */
    public static String renderGeometryAsPlainText(JGeometry             _geom,
                                                   String                _sqlTypeName,
                                                   Constants.bracketType _bracket,
                                                   int                   _ordPrecision)
    {
        return renderGeometry(_geom, _sqlTypeName, false, _bracket, false, _ordPrecision);
    }

    /**
     * @param geom
     * @return
     */
    public static String renderGeometryAsHTML(JGeometry             _geom, 
                                              String                _sqlTypeName,
                                              Constants.bracketType _bracket,
                                              boolean               _coordNumbering,
                                              int                   _ordPrecision) {
        return htmlWrap(renderGeometry(_geom, _sqlTypeName, true, _bracket, _coordNumbering, _ordPrecision));
    }

    private static String renderGeometry(JGeometry             _geom, 
                                         String                _sqlTypeName,
                                         boolean               _renderAsHTML,
                                         Constants.bracketType _bracket,
                                         boolean               _coordNumbering,
                                         int                   _ordPrecision) 
    {
        int GTYPE = 0;
        try {
             GTYPE = ((_geom.getDimensions() * 1000) + 
                     ((_geom.isLRSGeometry() && _geom.getDimensions()==3) ? 300 
                   : ((_geom.isLRSGeometry() && _geom.getDimensions()==4) ? 400
                   : 0)) + _geom.getType());
        } catch (Exception e) {
          GTYPE = 0;
        }
        int SRID           = Constants.SRID_NULL; try { SRID = _geom.getSRID();                } catch (Exception e) { SRID = Constants.SRID_NULL; }
        double POINT[]     = null;                try { POINT = _geom.getLabelPointXYZ()==null ? _geom.getPoint() : _geom.getLabelPointXYZ(); } catch (Exception e) { }
        int ELEMINFO[]     = null;                try { ELEMINFO = _geom.getElemInfo();        } catch (Exception e) { }
        double ORDINATES[] = null;                try { ORDINATES = _geom.getOrdinatesArray(); } catch (Exception e) { }
        return (_renderAsHTML ? renderGeometryElementsAsHTML(_sqlTypeName, GTYPE, SRID, POINT, ELEMINFO, ORDINATES, _bracket, _coordNumbering, _ordPrecision)
                              : renderGeometryElements      (_sqlTypeName, GTYPE, SRID, POINT, ELEMINFO, ORDINATES, _bracket, _ordPrecision) );
    }

    /**
     * @param _sdo_gtype from sdo_geometry 
     * @param _sdo_srid from sdo_geometry 
     * @param _sdo_point (as array) from sdo_geometry 
     * @param _sdo_elem_info (array) from sdo_geometry 
     * @param _sdo_ordinates (array) from sdo_geometry 
     * @return sdo_geometry in string form
     * @author Simon Greener, 31st March 2010
     *          Changed method and rendering of sdo_gtype and sdo_srid.
     * @author Simon Greener, 1th April 2010
     *          Changed to use of Constants
     * @author Simon Greener, 27th May 2010
     *          Put space between coordinates in output.
     *          Added bracketing around coordinate sets in sdo_ordinate_array
     */
    private static String renderGeometryElementsAsHTML(String                _sqlTypeName,
                                                       int                   _sdo_gtype, 
                                                       int                   _sdo_srid,
                                                       double[]              _sdo_point,
                                                       int[]                 _sdo_elem_info,
                                                       double[]              _sdo_ordinates,
                                                       Constants.bracketType _bracket,
                                                       boolean               _coordNumbering,
                                                       int                   _ordPrecision) 
    {
        if ( GeoRaptorPrefs == null ) {
            GeoRaptorPrefs = MainSettings.getInstance().getPreferences();
        }
        StringBuffer labelBuffer = new StringBuffer();
        // Bracketing
        //
        String bracketStart = getBracketStart(_bracket,true);
        String bracketEnd   = getBracketEnd(_bracket,true);
        String COORD_OPEN_BRACKET_HTML  = String.format(FONT_START,COORD_BRACKET_COLOUR) + bracketStart + FONT_END;
        String COORD_CLOSE_BRACKET_HTML = String.format(FONT_START,COORD_BRACKET_COLOUR) + bracketEnd  + FONT_END;
        String startBracket = ( _bracket != Constants.bracketType.NONE ) ? COORD_OPEN_BRACKET_HTML  : "";
        String endBracket   = ( _bracket != Constants.bracketType.NONE ) ? COORD_CLOSE_BRACKET_HTML : "";

        // Render Geometry
        // Render GType
        // Render SRID
        //
        if ( _sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
            labelBuffer.append(String.format(FONT_START,"BLACK") + 
                                ( GeoRaptorPrefs.getPrefixWithMDSYS() 
                                  ? _sqlTypeName 
                                  : _sqlTypeName.replace(Constants.MDSYS_SCHEMA+".","") ) + 
                                FONT_END + "(");
        }
        
        labelBuffer.append(String.format(FONT_START,"BLACK") + 
                           ( GeoRaptorPrefs.getPrefixWithMDSYS() 
                                   ? Constants.TAG_MDSYS_SDO_GEOMETRY
                                   : Constants.TAG_SDO_GEOMETRY ) + 
                           FONT_END + 
                           "(" + String.format(FONT_START,GTYPE_COLOUR) +(_sdo_gtype==0?FONT_NULL:_sdo_gtype) + FONT_END + "," +
                           ((_sdo_srid == Constants.SRID_NULL || _sdo_srid == 0) 
                             ? FONT_NULL + "," 
                             : String.format(FONT_START,SRID_COLOUR) + _sdo_srid + FONT_END + ",")); 

        // ***********************************************************
        // Render SDO Point based data
        if (_sdo_point != null && _sdo_point.length == 3) {
            labelBuffer.append(renderSdoPoint(_sdo_point,true,false/*don't wrap*/,_ordPrecision) + ","); 
        } else {
            labelBuffer.append(FONT_NULL + ",");
        }

        // ***********************************************************
        // Render Element Info data
        // And capture first ordinate in each elem_info set
        // @tobedone: this should not include compound element headers
        //
        List<Integer> elemOrds = null;        
        String OFFSET_HTML   = String.format(FONT_START,ELEM_ORD_COLOUR);
        String ELEMENT_HTML  = String.format(FONT_START,ETYPE_COLOUR);
        if (_sdo_elem_info != null) 
        {
            elemOrds = new ArrayList<Integer>(_sdo_elem_info.length/3);
            labelBuffer.append(String.format(FONT_START,SDO_ELEM_ARRAY_COLOUR) + 
                               ( GeoRaptorPrefs.getPrefixWithMDSYS() 
                                 ? Constants.TAG_MDSYS_SDO_ELEM_ARRAY
                                 : Constants.TAG_SDO_ELEM_ARRAY ) +
                               FONT_END + "(" + "<B>");               
            for (int ecount = 0; 
                     ecount < _sdo_elem_info.length; 
                     ecount += 3) 
            {
                if ( ecount % 3 == 0 ) {
                    elemOrds.add(Integer.valueOf(_sdo_elem_info[ecount]));
                }
                labelBuffer.append((ecount > 0 ? ", " : "") +
                                   startBracket +
                                   ((ecount%3==0) 
                                     ? OFFSET_HTML + _sdo_elem_info[ecount] + FONT_END + ELEMENT_HTML 
                                     : ELEMENT_HTML) + "," +
                                    _sdo_elem_info[ecount+1] + "," +
                                    _sdo_elem_info[ecount+2] + 
                                   FONT_END + endBracket);
            }
            labelBuffer.append(FONT_END + "), ");
        } else {
            labelBuffer.append(FONT_NULL + ",");
        }
        
        // ***********************************************************
        // Render SDO_ORDINATE_ARRAY data
        //
        if (_sdo_ordinates != null)
        {
            labelBuffer.append(renderSdoOrdinates(_sdo_ordinates,
                                                  true,
                                                  false /* wrap */,
                                                  _sdo_gtype,
                                                  elemOrds,
                                                  _bracket,
                                                  _coordNumbering,
                                                  _ordPrecision));
        } else {
            labelBuffer.append(FONT_NULL);
        }
          
        if ( elemOrds!=null ) {
            elemOrds = null;
        }
        return htmlWrap(labelBuffer.toString() + ")" + (_sqlTypeName.indexOf("MDSYS.ST_")==0?")":""));
    }

    private static String renderGeometryElements(String                _sqlTypeName,
                                                 int                   _sdo_gtype, 
                                                 int                   _sdo_srid,
                                                 double[]              _sdo_point,
                                                 int[]                 _sdo_elem_info,
                                                 double[]              _sdo_ordinates,
                                                 Constants.bracketType _bracket,
                                                 int                   _ordPrecision)
    {
        if ( GeoRaptorPrefs == null ) {
            GeoRaptorPrefs = MainSettings.getInstance().getPreferences();
        }
        StringBuffer labelBuffer = new StringBuffer();
        
        if ( _sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
            labelBuffer.append((GeoRaptorPrefs.getPrefixWithMDSYS() 
                                ? _sqlTypeName 
                                : _sqlTypeName.replace(Constants.MDSYS_SCHEMA+".","")) + 
                                "(" );
        }

        // Render Geometry
        // GType and SRID
        //
        labelBuffer.append((GeoRaptorPrefs.getPrefixWithMDSYS()
                              ? Constants.TAG_MDSYS_SDO_GEOMETRY
                              : Constants.TAG_SDO_GEOMETRY ) +
                           "(" + 
                           (_sdo_gtype==0 ? Constants.NULL : _sdo_gtype) + 
                           "," + 
                           (_sdo_srid==0||_sdo_srid==Constants.SRID_NULL ? Constants.NULL : _sdo_srid) + 
                           ",");
  
        // ***********************************************************
        // Render SDO Point based data
        if (_sdo_point != null) {
            labelBuffer.append(renderSdoPoint(_sdo_point,false,false,_ordPrecision) + "," );
        } else {
          labelBuffer.append(Constants.NULL + ",");
        }
  
        // ***********************************************************
        // Render Element Info data
        //
        if (_sdo_elem_info != null) 
            labelBuffer.append(renderElemInfoArray(_sdo_elem_info,false,false,_bracket) + ",") ;
        else
            labelBuffer.append(Constants.NULL + ",");

        // ***********************************************************
        // Render SDO_ORDINATE_ARRAY data
        // 
        if (_sdo_ordinates != null) 
        {
          labelBuffer.append(renderSdoOrdinates(_sdo_ordinates,
                                                false,
                                                false,
                                                _sdo_gtype,
                                                null,
                                                _bracket,
                                                false,
                                                _ordPrecision));
        } else {
            labelBuffer.append(Constants.NULL);
        }
        return labelBuffer.toString() + ")" + (_sqlTypeName.indexOf("MDSYS.ST_")==0?")":"");
    }
  
    public static String renderSdoPoint(double[] _sdo_point,
                                        boolean  _renderAsHTML,
                                        boolean  _wrap /* Sometimes this is being called from SDO_GEOMETRY rendering */,
                                        int      _ordPrecision) 
    {
        if ( GeoRaptorPrefs == null ) {
            GeoRaptorPrefs = MainSettings.getInstance().getPreferences();
        }
        DecimalFormat df = Tools.getDecimalFormatter(_ordPrecision); 
        StringBuffer labelBuffer = new StringBuffer(100);
        if (_sdo_point != null && _sdo_point.length == 3) 
        {
            if ( _renderAsHTML ) {
                if (Double.isNaN(_sdo_point[0]) && Double.isNaN(_sdo_point[1]) && Double.isNaN(_sdo_point[2])) {
                    labelBuffer.append(FONT_NULL);
                } else {
                    labelBuffer.append(String.format(FONT_START,SDO_POINT_COLOUR) + 
                                        ( GeoRaptorPrefs.getPrefixWithMDSYS() 
                                          ? Constants.TAG_MDSYS_SDO_POINT_TYPE
                                          : Constants.TAG_SDO_POINT_TYPE ) + 
                                       FONT_END + "(<B>");
                    for (int i = 0; i < _sdo_point.length; i++)
                        labelBuffer.append((i > 0 ? " </B>,<B>" : "") +
                                           (Double.isNaN(_sdo_point[i]) ? FONT_NULL : df.format(_sdo_point[i])) );
                    labelBuffer.append("</B>)");
                }
            } else {
                  if (Double.isNaN(_sdo_point[0]) && Double.isNaN(_sdo_point[1]) && Double.isNaN(_sdo_point[2])) {
                      labelBuffer.append(Constants.NULL );
                  } else {
                      labelBuffer.append( (GeoRaptorPrefs.getPrefixWithMDSYS() 
                                           ? Constants.TAG_MDSYS_SDO_POINT_TYPE
                                           : Constants.TAG_SDO_POINT_TYPE) + 
                                          "(");
                      for (int i = 0; i < _sdo_point.length; i++)
                          labelBuffer.append((i > 0 ? "," : "") + 
                                             (Double.isNaN(_sdo_point[i]) ? Constants.NULL : df.format(_sdo_point[i])) );
                      labelBuffer.append(")");
                  }
            }
        } else {
            labelBuffer.append(FONT_NULL);
        }
        return _wrap ? htmlWrap(labelBuffer.toString()) : labelBuffer.toString();
    }
  
    public static String renderElemInfoArray(int[]                 _sdo_elem_info,
                                             boolean               _renderAsHTML,
                                             boolean               _wrap,
                                             Constants.bracketType _bracket )
    {
        if ( GeoRaptorPrefs == null ) {
            GeoRaptorPrefs = MainSettings.getInstance().getPreferences();
        }

        if (_sdo_elem_info == null || _sdo_elem_info.length < 1) {
            return "";
        }
     
        StringBuffer labelBuffer = new StringBuffer();
    
        // Bracketing
        String bracketStart = _renderAsHTML ? getBracketStart(_bracket,_renderAsHTML) : "";
        String bracketEnd   = _renderAsHTML ? getBracketEnd(_bracket,_renderAsHTML) : "";
    
        if ( _renderAsHTML ) 
        { 
          String OFFSET_HTML   = String.format(FONT_START,ELEM_ORD_COLOUR);
          String ELEMENT_HTML  = String.format(FONT_START,ETYPE_COLOUR);
          labelBuffer.append(String.format(FONT_START,SDO_ELEM_ARRAY_COLOUR) + 
                             (GeoRaptorPrefs.getPrefixWithMDSYS() 
                             ? Constants.TAG_MDSYS_SDO_ELEM_ARRAY
                             : Constants.TAG_SDO_ELEM_ARRAY) + 
                             FONT_END + "(" + "<B>");               
          for (int ecount = 0; 
                   ecount < _sdo_elem_info.length; 
                   ecount += 3) 
          {
              labelBuffer.append((ecount > 0 ? ", " : "") +
                                 bracketStart +
                                 ((ecount%3==0) 
                                   ? OFFSET_HTML + 
                                       (Integer.MIN_VALUE==_sdo_elem_info[ecount]?FONT_NULL:_sdo_elem_info[ecount]) + 
                                     FONT_END + ELEMENT_HTML 
                                   : ELEMENT_HTML) + "," +
                                 (Integer.MIN_VALUE==_sdo_elem_info[ecount+1]?FONT_NULL:_sdo_elem_info[ecount+1]) + "," +
                                 (Integer.MIN_VALUE==_sdo_elem_info[ecount+2]?FONT_NULL:_sdo_elem_info[ecount+2]) + 
                                 FONT_END + bracketEnd);
          }
          labelBuffer.append(FONT_END + ")");
        } else {
              labelBuffer.append((GeoRaptorPrefs.getPrefixWithMDSYS() 
                                  ? Constants.TAG_MDSYS_SDO_ELEM_ARRAY
                                  : Constants.TAG_SDO_ELEM_ARRAY) + 
                                 "(" );
              for (int ecount = 0;
                   ecount < _sdo_elem_info.length;
                   ecount += 3) 
              {
                  labelBuffer.append((ecount > 0 ? ", " : "") +
                                     bracketStart +
                                     (Integer.MIN_VALUE==_sdo_elem_info[ecount]?Constants.NULL:_sdo_elem_info[ecount]) + "," +
                                     (Integer.MIN_VALUE==_sdo_elem_info[ecount+1]?Constants.NULL:_sdo_elem_info[ecount+1]) + "," +
                                     (Integer.MIN_VALUE==_sdo_elem_info[ecount+2]?Constants.NULL:_sdo_elem_info[ecount+2]) +
                                     bracketEnd);
              }
              labelBuffer.append(")");
        }
        return _wrap ? htmlWrap(labelBuffer.toString()) : labelBuffer.toString(); 
    }
    
    public static String renderSdoOrdinates(double[]              _sdo_ordinates,
                                            boolean               _renderAsHTML,
                                            boolean               _wrap,
                                            int                   _sdo_gtype,
                                            List<Integer>         _elemOrds,
                                            Constants.bracketType _bracket,
                                            boolean               _coordNumbering,
                                            int                   _ordPrecision) 
{
        if ( GeoRaptorPrefs == null ) {
            GeoRaptorPrefs = MainSettings.getInstance().getPreferences();
        }
        DecimalFormat df = Tools.getDecimalFormatter(_ordPrecision); 
        boolean elementOrdinateMapping = ( _elemOrds == null || _elemOrds.size()==0 ) ? false : true;
        StringBuffer labelBuffer = new StringBuffer();
        
        // If dimension == 0 we don't know what the dimensionality is
        int dimension = (int)Math.floor(_sdo_gtype / 1000);
        int numberOfCoordinates = dimension==0?0:(_sdo_ordinates.length / dimension);
        
        int ordIndex  = -1;
        int ordOffset = -1;
        if ( _renderAsHTML ) 
        {
            String bracketStart = ( _bracket != Constants.bracketType.NONE ) 
                                  ? String.format(FONT_START,COORD_BRACKET_COLOUR) + getBracketStart(_bracket,_renderAsHTML) + FONT_END
                                  : "";
            String bracketEnd   = ( _bracket != Constants.bracketType.NONE ) 
                                  ? String.format(FONT_START,COORD_BRACKET_COLOUR) + getBracketEnd(_bracket,_renderAsHTML)  + FONT_END 
                                  : "";
            String coordSuperscript = _coordNumbering ? FONT_COORD_SUPERSCRIPT : "";
            String ORD_ELEMENT_HTML = elementOrdinateMapping ? String.format(FONT_START,ELEM_ORD_COLOUR) : "";
            labelBuffer.append(String.format(FONT_START,SDO_ORD_ARRAY_COLOUR) +
                                ( GeoRaptorPrefs.getPrefixWithMDSYS() 
                                  ? Constants.TAG_MDSYS_SDO_ORD_ARRAY
                                  : Constants.TAG_SDO_ORD_ARRAY) +
                                FONT_END + "(" + "<B>");       
            // Following duplication is done for speed purposes
            switch (dimension) {
                case 0 : {
                  for (int i = 0; i < _sdo_ordinates.length; i++) {
                      labelBuffer.append(( i!=0 ? ", <B>" : "") + (Double.isNaN(_sdo_ordinates[i]) ? FONT_NULL : df.format(_sdo_ordinates[i]).toString()) + "</B>");
                  }
                  break;
                }
                case 2 : {
                    for (int i = 0; i < numberOfCoordinates; i++) 
                    {
                        ordOffset = i * dimension;
                        // Search for element in list
                        //
                        ordIndex = (_elemOrds==null) ? 0 :  Collections.binarySearch(_elemOrds, Integer.valueOf(1 + ordOffset));
                        labelBuffer.append(( i!=0 ? ", " : "") +
                                           bracketStart +                         
                                            ( (ordIndex>=0) /* SDO_ELEM_INFO reference to starting ordinate */ ? ORD_ELEMENT_HTML : "" ) + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset]) ? FONT_NULL : df.format(_sdo_ordinates[ordOffset]).toString()) + 
                                           "," + 
                                           ( (ordIndex>=0) ? FONT_END : "" ) +
                                           (Double.isNaN(_sdo_ordinates[ordOffset+1]) ? FONT_NULL : df.format(_sdo_ordinates[ordOffset+1]).toString()) +
                                           bracketEnd +
                                           coordSuperscript.replace("###",String.valueOf(i+1)) );
                    }
                    break;
                }
                case 3 : {
                    for (int i = 0; i < numberOfCoordinates; i++) {
                        ordOffset = i * dimension;
                        // Search for element in list
                        //
                        ordIndex = (_elemOrds==null) ? 0 :  Collections.binarySearch(_elemOrds, Integer.valueOf(1 + ordOffset));
                        labelBuffer.append(( i!=0 ? ", " : "") +
                                           bracketStart + 
                                           ( (ordIndex>=0) /* SDO_ELEM_INFO references this (single) ordinate */
                                             ? ORD_ELEMENT_HTML : "" ) + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset])   ? FONT_NULL : df.format(_sdo_ordinates[ordOffset]).toString()) + 
                                           "," + 
                                           ( (ordIndex>=0) ? FONT_END : "" ) +
                                           (Double.isNaN(_sdo_ordinates[ordOffset+1]) ? FONT_NULL : df.format(_sdo_ordinates[ordOffset+1]).toString()) + 
                                           "," + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset+2]) ? FONT_NULL : df.format(_sdo_ordinates[ordOffset+2]).toString()) +
                                           bracketEnd +
                                           coordSuperscript.replace("###",String.valueOf(i+1)) );
                    }
                    break;
                }
                case 4 :
                default : {
                    for (int i = 0; i < numberOfCoordinates; i++) {
                        // Search for element in list
                        //
                        ordIndex = (_elemOrds==null) ? 0 :  Collections.binarySearch(_elemOrds, Integer.valueOf(1 + ordOffset));
                        ordOffset = i * dimension;
                        labelBuffer.append(( i!=0 ? ", " : "") +
                                           bracketStart + 
                                           ( (ordIndex>=0) /* SDO_ELEM_INFO references this (single) ordinate */
                                             ? ORD_ELEMENT_HTML 
                                             : "" ) + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset]) ? FONT_NULL : df.format(_sdo_ordinates[ordOffset]).toString()) + 
                                           "," + 
                                           ( (ordIndex>=0) ? FONT_END : "" ) + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset+1]) ? FONT_NULL : df.format(_sdo_ordinates[ordOffset+1]).toString()) + 
                                           "," + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset+2]) ? FONT_NULL : df.format(_sdo_ordinates[ordOffset+2]).toString()) + 
                                           "," + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset+3]) ? FONT_NULL : df.format(_sdo_ordinates[ordOffset+3]).toString()) +
                                           bracketEnd +
                                           coordSuperscript.replace("###",String.valueOf(i+1)) );
                    }
                    break;
                }
              }
        } else {
            String bracketStart = getBracketStart(_bracket,_renderAsHTML);
            String bracketEnd   = getBracketEnd(_bracket,_renderAsHTML);
            labelBuffer.append((GeoRaptorPrefs.getPrefixWithMDSYS() 
                                ? Constants.TAG_MDSYS_SDO_ORD_ARRAY
                                : Constants.TAG_SDO_ORD_ARRAY) + 
                               "(");
            // Following duplication is done for speed purposes
            switch (dimension) {
              case 0 : {
                for (int i = 0; i < _sdo_ordinates.length; i++) {
                    labelBuffer.append(( i!=0 ? "," : "") + 
                    (Double.isNaN(_sdo_ordinates[i]) ? Constants.NULL : df.format(_sdo_ordinates[i]).toString() ));
                }
                break;
              }
                case 2 : {
                    for (int i = 0; i < numberOfCoordinates; i++) {
                        ordOffset = i * dimension;
                        labelBuffer.append(( i!=0 ? ", " : "") +
                                           bracketStart + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset])   ? Constants.NULL : df.format(_sdo_ordinates[ordOffset]).toString()) +
                                           "," + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset+1]) ? Constants.NULL : df.format(_sdo_ordinates[ordOffset+1]).toString()) +
                                           bracketEnd);
                    }
                    break;
                }
                case 3 : {
                    for (int i = 0; i < numberOfCoordinates; i++) {
                        ordOffset = i * dimension;
                        labelBuffer.append(( i!=0 ? ", " : "") +
                                           bracketStart + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset])   ? Constants.NULL : df.format(_sdo_ordinates[ordOffset]).toString()) +
                                           "," + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset+1]) ? Constants.NULL : df.format(_sdo_ordinates[ordOffset+1]).toString()) +
                                           "," +
                                           (Double.isNaN(_sdo_ordinates[ordOffset+2]) ? Constants.NULL : df.format(_sdo_ordinates[ordOffset+2]).toString()) +
                                           bracketEnd);
                    }
                    break;
                }
                case 4 :
                default : {
                    for (int i = 0; i < numberOfCoordinates; i++) {
                        ordOffset = i * dimension;
                        labelBuffer.append(( i!=0 ? ", " : "") +
                                           bracketStart + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset])   ? Constants.NULL : df.format(_sdo_ordinates[ordOffset]).toString()) +
                                           "," + 
                                           (Double.isNaN(_sdo_ordinates[ordOffset+1]) ? Constants.NULL : df.format(_sdo_ordinates[ordOffset+1]).toString()) +
                                           "," +
                                           (Double.isNaN(_sdo_ordinates[ordOffset+2]) ? Constants.NULL : df.format(_sdo_ordinates[ordOffset+2]).toString())  +
                                           "," +
                                           (Double.isNaN(_sdo_ordinates[ordOffset+3]) ? Constants.NULL : df.format(_sdo_ordinates[ordOffset+3]).toString()) 
                                           +
                                           bracketEnd);
                    }
                    break;
                }
            }
        }
        labelBuffer.append(")");
        return _wrap ? htmlWrap(labelBuffer.toString()) : labelBuffer.toString();      
    }
  
    /**
     * @method renderVertexType
     * @param _colValue
     * @param _renderAsHTML
     * @return
     * @author Simon Greener, June 8th 2010 - Original coding
     */
    public static String renderVertexType(STRUCT  _colValue,
                                          boolean _renderAsHTML )
    {
        if ( GeoRaptorPrefs == null ) {
            GeoRaptorPrefs = MainSettings.getInstance().getPreferences();
        }

        StringBuffer labelBuffer = new StringBuffer();
        try 
        {
            Datum data[] = _colValue.getOracleAttributes();
            double x = ((NUMBER)data[0]).doubleValue();
            double y = ((NUMBER)data[1]).doubleValue();
            double z = (data[2] != null) ? ((NUMBER)data[2]).doubleValue() : Double.NaN;
            double w = (data[3] != null) ? ((NUMBER)data[3]).doubleValue() : Double.NaN;

            String NULL     = _renderAsHTML ? FONT_NULL : "NULL";
            labelBuffer.append((_renderAsHTML 
                                ? String.format(FONT_START,VERTEX_TYPE_COLOUR)
                                : "") +
                               Constants.TAG_MDSYS_VERTEX_TYPE +
                               ( _renderAsHTML ? FONT_END : "" )
                               + "(" );
            if ( Double.isInfinite(x) || Double.isNaN(x) )
                labelBuffer.append(NULL + ",");
            else
                labelBuffer.append((_renderAsHTML ? String.format(FONT_START,ORD_COLOUR) + x + FONT_END : x ) + ",");
            if ( Double.isInfinite(y) || Double.isNaN(y) )
                labelBuffer.append(NULL + ",");
            else
                labelBuffer.append((_renderAsHTML ? String.format(FONT_START,ORD_COLOUR) + y + FONT_END : y ) + ",");
            if ( Double.isInfinite(z) || Double.isNaN(z) )
                labelBuffer.append(NULL + ",");
            else
                labelBuffer.append((_renderAsHTML ? String.format(FONT_START,ORD_COLOUR) + z + FONT_END : z ) + ",");
            if ( Double.isInfinite(w) || Double.isNaN(w) ) 
                labelBuffer.append(NULL + ")");
            else
                labelBuffer.append((_renderAsHTML ? String.format(FONT_START,ORD_COLOUR) + w + FONT_END : w ) + ")");

        } catch (SQLException _e) {
          labelBuffer.append(_e.getLocalizedMessage());
        }
        return _renderAsHTML ? htmlWrap(labelBuffer.toString()) : labelBuffer.toString();      
    }

    public static String renderDimArray(ARRAY   _dimArray,
                                        boolean _renderAsHTML)
    {
        if ( GeoRaptorPrefs == null ) {
            GeoRaptorPrefs = MainSettings.getInstance().getPreferences();
        }

        String returnString = "";
        try 
        {
            ARRAY dimArray =  _dimArray;
            if ( dimArray.getDescriptor().getSQLName().getName().equals(Constants.TAG_MDSYS_SDO_DIMARRAY) ) 
            {
                String DIM_NAME = "";
                double SDO_LB   = Double.MAX_VALUE;
                double SDO_UB   = Double.MAX_VALUE;
                double SDO_TOL  = Double.MAX_VALUE;
                MetadataEntry mEntry = new MetadataEntry();
                Datum[] objs = dimArray.getOracleArray();
                for (int i =0; i < objs.length; i++) {
                    STRUCT dimElement = (STRUCT)objs[i];
                    Datum data[] = dimElement.getOracleAttributes();
                    DIM_NAME = data[0].stringValue();
                    SDO_LB   = data[1].doubleValue();
                    SDO_UB   = data[2].doubleValue();
                    SDO_TOL  = data[3].doubleValue();
                    mEntry.add(DIM_NAME,SDO_LB,SDO_UB,SDO_TOL);
                }
                if ( _renderAsHTML ) {
                    returnString = mEntry.render();
                } else {
                    returnString = mEntry.toDimArray();
                }
            }
        } catch (SQLException sqle) {
            return sqle.getLocalizedMessage();
        }
        return _renderAsHTML ? htmlWrap(returnString) : returnString;
    }

    public static String getBracketStart(Constants.bracketType _bracket,
                                         boolean               _html) {
        switch (_bracket) {
            case SQUARE  : return "["; 
            case ELIPSIS : return "{"; 
            case ROUND   : return "("; 
            case ANGLED  : return _html ? "&lt;" : "<"; 
            case NONE    :
            default      : return "";
        }
    }
    
    public static String getBracketEnd(Constants.bracketType _bracket,
                                       boolean               _html) {
        switch (_bracket) {
            case SQUARE  : return "]";  
            case ELIPSIS : return "}";  
            case ROUND   : return ")";  
            case ANGLED  : return _html ? "&gt;" : ">";  
            case NONE    :
            default      : return "";
        }
    }

}
