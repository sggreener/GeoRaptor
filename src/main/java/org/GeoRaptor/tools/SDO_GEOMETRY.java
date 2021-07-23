package org.GeoRaptor.tools;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.DatabaseConnections;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.util.logging.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.io.oracle.OraGeom;
import org.locationtech.jts.io.oracle.OraReader;
import org.locationtech.jts.io.oracle.OraUtil;

import oracle.spatial.geometry.J3D_Geometry;
import oracle.spatial.geometry.JGeometry;
import oracle.spatial.util.GML2;
import oracle.spatial.util.GML3;
import oracle.spatial.util.GeometryExceptionWithContext;
import oracle.spatial.util.KML2;
import oracle.spatial.util.WKT;
import oracle.sql.NUMBER;

public class SDO_GEOMETRY 
{    

    /**
     * For access to logger subsystem
     */
    private static final Logger LOGGER = 
      org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.tools.SDO_Geometry");

    public static double[] reverseOrdinates(int      _dim,
                                            double[] _ordinates) 
    {
        if ( _ordinates == null || _ordinates.length==0 )
            return _ordinates;
        double[] ret = new double[_ordinates.length];
        int totalCoords = _ordinates.length/_dim;
        int fromCoord = totalCoords - 1;
        int fromOrd = 0,
            toOrd   = 0;
        for ( int i=0; i<totalCoords; i++, fromCoord-- ) 
        {
            toOrd   = i * _dim;
            fromOrd = fromCoord * _dim;
            for ( int k=0; k<_dim; k++) {
                ret[toOrd+k] = _ordinates[fromOrd+k];
                // LOGGER.info("ret["+(toOrd+k)+"] = " + ret[toOrd+k] + "  _ordinates["+(toOrd+k)+"] = " + _ordinates[toOrd+k]);
            }
        }
        return ret;
    } 
    

    public static boolean hasMeasure(int _fullGType) {
        return ((_fullGType/100) % 10) == 0 ? (_fullGType > 4000 ? true : false) : true;
    }

	public static boolean hasMeasure(Struct _struct) {
        try {
            if ( _struct == null ||
                 ( _struct.getSQLTypeName().equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) == false &&
                   _struct.getSQLTypeName().indexOf("MDSYS.ST_")==-1) ) {
                return false;
            }
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            return hasMeasure(SDO_GEOMETRY.getFullGType(stGeom,2000));
        } catch (SQLException sqle) {
          return false;
        }
    }

    public static boolean hasZ(int _fullGType ) {
        int numberOrdinates = _fullGType/1000;
        switch ( numberOrdinates ) {
          case 4 : return true;
          case 3 : return ! hasMeasure(_fullGType);
          default:
          case 2 : return false;
        }
    }  
  
	public static boolean hasZ(Struct _struct) {
        try {
            if ( _struct == null ||
                 ( _struct.getSQLTypeName().equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) == false &&
                   _struct.getSQLTypeName().indexOf("MDSYS.ST_")==-1) ) {
                return false;
            }
            @SuppressWarnings("unused")
			Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            return hasZ(SDO_GEOMETRY.getFullGType(_struct,2000));
        } catch (SQLException sqle) {
          return false;
        }
    }

    /**
     * @param _struct
     * @author Simon Greener, April 4th 2010
     * @return
     */
    // note Tested 15th August
    public static double[] getVertexType(Struct _struct) 
    {
        if (_struct == null) {
            return null;
        }
        try {
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( ! sqlTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE)) {
              return null;
            }
            Object[] data = stGeom.getAttributes();
            BigDecimal x = (BigDecimal)data[0];
            BigDecimal y = (BigDecimal)data[1];
            BigDecimal z = (BigDecimal)data[2];
            BigDecimal w = (BigDecimal)data[3];
            return (w == null ) 
                ? new double[] { 
                   x!=null ? x.doubleValue() : null, 
                   y!=null ? y.doubleValue() : null, 
                   z!=null ? z.doubleValue() : null
                }
                : new double[] { 
                   x!=null ? x.doubleValue() : null, 
                   y!=null ? y.doubleValue() : null, 
                   z!=null ? z.doubleValue() : null, 
                   w!=null ? w.doubleValue() : null
                };
        } catch (SQLException sqle) {
            return null;
        }
    }

    public static ShapeType getShapeType(int     _fullGType,
                                         boolean _measure)
    {
        int gType = 1;
        int fullGType = _fullGType;
        if ( _fullGType < 2000 ) {
            gType = _fullGType;
            fullGType += 2000;
        } else
            gType = _fullGType % 10;

        switch (gType) {
          case JGeometry.GTYPE_POINT         :      if (hasZ(fullGType) && hasMeasure(fullGType) ) return _measure ? ShapeType.POINTM : ShapeType.POINTZ;
                                               else if (hasZ(fullGType) )                      return ShapeType.POINTZ;
                                               else if (hasMeasure(fullGType) )                return ShapeType.POINTM;
                                               else                                            return ShapeType.POINT;
          case JGeometry.GTYPE_MULTIPOINT    :      if (hasZ(fullGType) && hasMeasure(fullGType) ) return _measure ? ShapeType.MULTIPOINTM : ShapeType.MULTIPOINTZ;
                                               else if (hasZ(fullGType) )                      return ShapeType.MULTIPOINTZ;
                                               else if (hasMeasure(fullGType) )                return ShapeType.MULTIPOINTM;
                                               else                                            return ShapeType.MULTIPOINT;
          case JGeometry.GTYPE_CURVE         : 
          case JGeometry.GTYPE_MULTICURVE    :      if (hasZ(fullGType) && hasMeasure(fullGType) ) return _measure ? ShapeType.ARCM : ShapeType.ARCZ;
                                               else if (hasZ(fullGType) )                      return ShapeType.ARCZ;
                                               else if (hasMeasure(fullGType) )                return ShapeType.ARCM;
                                               else                                            return ShapeType.ARC;
          case JGeometry.GTYPE_POLYGON       :  
          case JGeometry.GTYPE_MULTIPOLYGON  : 
          case J3D_Geometry.GTYPE_SOLID      : 
          case J3D_Geometry.GTYPE_MULTISOLID :      if (hasZ(fullGType) && hasMeasure(fullGType) ) return _measure ? ShapeType.POLYGONM : ShapeType.POLYGONZ;
                                               else if (hasZ(fullGType) )                      return ShapeType.POLYGONZ;
                                               else if (hasMeasure(fullGType) )                return ShapeType.POLYGONM;
                                               else                                            return ShapeType.POLYGON;
          case JGeometry.GTYPE_COLLECTION    :   
                                      default: return ShapeType.UNDEFINED;
        }
    }

    public static String getGeometryType(int _gtype) 
    {
        switch (_gtype) 
        {
            case 1 : return Constants.GEOMETRY_TYPES.POINT.toString();
            case 2 : return Constants.GEOMETRY_TYPES.LINE.toString(); 
            case 3 : return Constants.GEOMETRY_TYPES.POLYGON.toString(); 
            case 4 : return Constants.GEOMETRY_TYPES.COLLECTION.toString();
            case 5 : return Constants.GEOMETRY_TYPES.MULTIPOINT.toString();
            case 6 : return Constants.GEOMETRY_TYPES.MULTILINE.toString();
            case 7 : return Constants.GEOMETRY_TYPES.MULTIPOLYGON.toString();
            case 8 : return Constants.GEOMETRY_TYPES.SOLID.toString();
           default : break;
        };
        return Constants.GEOMETRY_TYPES.UNKNOWN.toString() + "(" + _gtype + ")"; 
    }

    public static Constants.GEOMETRY_TYPES discoverGeometryType( int                      _fullGType,
                                                                 Constants.GEOMETRY_TYPES _existingGType) 
    {   
       int gType = 1;
       int fullGType = _fullGType;
       if ( _fullGType < 2000 ) {
           gType = _fullGType;
           fullGType += 2000;
       } 
       gType = fullGType % 10;

       Constants.GEOMETRY_TYPES geometryType = Constants.GEOMETRY_TYPES.UNKNOWN;
       switch (gType) {
          case JGeometry.GTYPE_COLLECTION    : geometryType = Constants.GEOMETRY_TYPES.COLLECTION;   break;
          case JGeometry.GTYPE_POINT         : geometryType = Constants.GEOMETRY_TYPES.POINT;        break;
          case JGeometry.GTYPE_MULTIPOINT    : geometryType = Constants.GEOMETRY_TYPES.MULTIPOINT;   break;
          case JGeometry.GTYPE_CURVE         : geometryType = Constants.GEOMETRY_TYPES.LINE;         break;
          case JGeometry.GTYPE_MULTICURVE    : geometryType = Constants.GEOMETRY_TYPES.MULTILINE;    break;
          case JGeometry.GTYPE_POLYGON       : geometryType = Constants.GEOMETRY_TYPES.POLYGON;      break;
          case JGeometry.GTYPE_MULTIPOLYGON  : geometryType = Constants.GEOMETRY_TYPES.MULTIPOLYGON; break;
          case J3D_Geometry.GTYPE_SOLID      : geometryType = Constants.GEOMETRY_TYPES.SOLID;        break;
          case J3D_Geometry.GTYPE_MULTISOLID : geometryType = Constants.GEOMETRY_TYPES.MULTISOLID;   break;
          default: LOGGER.warn("(SDO_Geometry.discoverGeometryType) Unsupported Geometry Type: " + gType );
       }
       // Now do comparison
       if ( _existingGType == Constants.GEOMETRY_TYPES.UNKNOWN )
           return geometryType;
       if ( _existingGType == geometryType )                                // POINT.equal.POINT etc
           return geometryType;
       if ( _existingGType == Constants.GEOMETRY_TYPES.COLLECTION )
           return Constants.GEOMETRY_TYPES.COLLECTION;
       if ( geometryType.toString().contains(_existingGType.toString()) )   // MULTIPOINT.contains.POINT etc
           return geometryType;
       if ( _existingGType.toString().contains(geometryType.toString()) )   // MULTIPOINT.contains.POINT etc
           return _existingGType;
       else 
           return Constants.GEOMETRY_TYPES.COLLECTION;                      // MULTIPOINT and LINE etc 
     }


    /**
     * Get MBR for select Geometry object
     * @param _st
     * @return geometry object MBR
     * @throws SQLException
     * @author Simon Greener, 1st June 2010
     *          Corrected MBR to reflect 3D etc objects
     *          getMBR does not honour oriented Points so constructed getOrientedPointMBR
     */
	public static Envelope getGeoMBR(Struct _struct) 
    {
        if (_struct == null) return null;
        try {
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            JGeometry geo = JGeometry.loadJS(stGeom);
            return JGeom.getGeoMBR(geo);
        } catch (SQLException sqle) {
           return null;
        }
    }

    public static double[] validateRectangle(int _dim,
                                             int _etype,
                                             double[] _ordinates) 
    {
        if ( _etype == 1003 ) {
            switch ( _dim ) {
                case 4  : return new double[] {Math.min(_ordinates[0],_ordinates[4]), Math.min(_ordinates[1],_ordinates[5]), _ordinates[2], _ordinates[3],
                                               Math.max(_ordinates[0],_ordinates[4]), Math.max(_ordinates[1],_ordinates[5]), _ordinates[6], _ordinates[7]};
                case 3  : return new double[] {Math.min(_ordinates[0],_ordinates[3]), Math.min(_ordinates[1],_ordinates[4]), _ordinates[2], 
                                               Math.max(_ordinates[0],_ordinates[3]), Math.max(_ordinates[1],_ordinates[4]), _ordinates[6], _ordinates[7]};
                case 2  : 
                default : return new double[] {Math.min(_ordinates[0],_ordinates[2]), Math.min(_ordinates[1],_ordinates[3]), 
                                               Math.max(_ordinates[0],_ordinates[2]), Math.max(_ordinates[1],_ordinates[3])}; 
            }
        } else if ( _etype == 2003 ) {
            switch ( _dim ) {
                case 4  : return new double[] {Math.max(_ordinates[0],_ordinates[4]), Math.max(_ordinates[1],_ordinates[5]), _ordinates[2], _ordinates[3],
                                               Math.min(_ordinates[0],_ordinates[4]), Math.min(_ordinates[1],_ordinates[5]), _ordinates[6], _ordinates[7]};
                case 3  : return new double[] {Math.max(_ordinates[0],_ordinates[3]), Math.max(_ordinates[1],_ordinates[4]), _ordinates[2], 
                                               Math.min(_ordinates[0],_ordinates[3]), Math.min(_ordinates[1],_ordinates[4]), _ordinates[6], _ordinates[7]};
                case 2  : 
                default : return new double[] {Math.max(_ordinates[0],_ordinates[2]), Math.max(_ordinates[1],_ordinates[3]), 
                                               Math.min(_ordinates[0],_ordinates[2]), Math.min(_ordinates[1],_ordinates[3])}; 
            }            
        }
        return _ordinates;
    }
    
    /**
     * Copy definition of SDO_GEOMETRY object to clipboard 
     * @param colValue Struct cell value
     * Otherwise copy value to SQL Developer log window
     * @author Simon Greener, October 18th 2010
     *          Moved from SpatialRendererMouseListener and made function.
     * @author Simon Greener, November 11th 2010
     *          Moved from RenderResultset
    */
     
	public static String getGeometryAsString(JGeometry _jGeom) 
    {
		Connection conn = DatabaseConnections.getInstance().getAnyOpenConnection();
		Struct sGeom = JGeom.toStruct(_jGeom,conn);
		return getGeometryAsString(sGeom);
    }
	
	public static String getGeometryAsString(Struct _struct) 
	{
        if ( _struct==null ) {
            return "";
        }
        Preferences preferences = MainSettings.getInstance().getPreferences();
        Constants.renderType visualType = preferences.getVisualFormat();
        
        // If visualisation is not SDO_GEOMETRY we need a connection
        // to use sdoutl.jar/JTS conversion routines so grab first available
        //
        Connection conn = DatabaseConnections.getInstance().getActiveConnection(); 
        if ( conn == null ) {
        	conn = DatabaseConnections.getInstance().getAnyOpenConnection();
        }
        if ( conn == null && ( visualType != Constants.renderType.SDO_GEOMETRY ) )
        {
        	visualType = Constants.renderType.SDO_GEOMETRY;
        }
        
        // get geometry structure
        String clipText = "";
        try {
			Struct structGeom = _struct;
            if ( _struct.getSQLTypeName().indexOf("MDSYS.ST_")==0 ) {
                structGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 

            // Images cannot be converted to clipboard as text
            //
            if (visualType == Constants.renderType.ICON || 
                visualType == Constants.renderType.THUMBNAIL ) {
                visualType = Constants.renderType.SDO_GEOMETRY;
            }

            int mDim           = SDO_GEOMETRY.getMeasureDimension(structGeom);
            boolean hasArc     = SDO_GEOMETRY.hasArc(structGeom);
            boolean isCompound = SDO_GEOMETRY.hasCompoundCurve(structGeom);
        	int  coordDims     = SDO_GEOMETRY.getDimension(structGeom,2);
           	int srid           = SDO_GEOMETRY.getSRID(structGeom);

            // If sdo_geometry object has LRS or Compound Elements then 
           	// GML/KML rendering cannot occur
            if ( ( mDim != 0 || isCompound )
                 && (   visualType == Constants.renderType.KML2 
                     || visualType == Constants.renderType.KML 
                     || visualType == Constants.renderType.GML2 
                     || visualType == Constants.renderType.GML3 )
            	    ) 
            {
           		visualType = Constants.renderType.SDO_GEOMETRY;
            }
        	if ( coordDims >= 3 && visualType == Constants.renderType.KML )
        		visualType = Constants.renderType.SDO_GEOMETRY;
            	
            WKT w = null;
            int decimalPlaces = preferences.getPrecision();
            double  precisionModelScale = Tools.getPrecisionScale(decimalPlaces);
    		PrecisionModel           pm = null;
            GeometryFactory geomFactory = null;
            OraReader     geomConverter = null;
    		Geometry               geom = null;

            // Do conversion       
            switch ( visualType )
            {                    
                case GEOJSON:
                    pm                = new PrecisionModel(precisionModelScale);
                    geomFactory       = new GeometryFactory(pm);
                    geomConverter     = new OraReader(geomFactory);
            		geom              = geomConverter.read(structGeom);
                	GeoJsonWriter gjw = new GeoJsonWriter(decimalPlaces);
                	gjw.setEncodeCRS(true);
                	clipText = gjw.write(geom);
                	break;
                	
                case WKT:
                	// WKT is 2D
                	// Use Oracle Spatial WKT not JTS WKTReader
            		w = new WKT();
            		clipText = new String(w.fromStruct(structGeom));
            		break;
            		
                case EWKT : 
            		// JTS does not handle geometries with CircularStrings 
                	if ( hasArc ) {
                		// This degrades geometry to 2D but unless I encode my own WKT writer...
                		w = new WKT();
                		clipText = new String(w.fromStruct(structGeom));
                	} else {
                        pm             = new PrecisionModel(precisionModelScale);
                        geomFactory    = new GeometryFactory(pm);
                        geomConverter  = new OraReader(geomFactory);
                		geom           = geomConverter.read(structGeom);
                		WKTWriter wktw = new WKTWriter(coordDims);
                		clipText       = wktw.write(geom);

                		// JTS WKT writer doesn't interpret measures in the Oracle way
                		if ( mDim == 3 && coordDims == 3 ) 
                			clipText = clipText.replace("Z","M");
                		else if ( mDim == 4 )
                			clipText = clipText.replace("Z","ZM");
                	}
                    clipText = "SRID=" + (srid==Constants.SRID_NULL?"NULL":String.valueOf(srid)) + ";" + clipText;
                    break;

                case KML  : /* Always render with KML2 */
                case KML2 : KML2.setConnection(conn);
                            clipText = KML2.to_KMLGeometry(structGeom);
                            break;
                            
                case GML2 : GML2.setConnection(conn);
                            clipText = GML2.to_GMLGeometry(structGeom);
                            break;
                            
                case GML3 : GML3.setConnection(conn);
                            clipText = GML3.to_GML3Geometry(structGeom);
                            break;

                case SDO_GEOMETRY:
                default: 
                    clipText = RenderTool.renderStructAsPlainText(
                                                structGeom,
                                                Constants.bracketType.NONE,
                                                Constants.MAX_PRECISION);
                    break;
            }

        } catch (Exception e) {
            clipText = "";
        }
        return clipText;
    }

    public static int getFullGType(Struct _struct,
                                   int    _nullValue) 
    {
      // Note Returning null for null sdo_geometry structure
        if (_struct == null) {
          return _nullValue;
        }
      try {
          String sqlTypeName = _struct.getSQLTypeName();
          Object[] data = (Object[])_struct.getAttributes();
          if (sqlTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) ) {
              return (data[2] != null) ? ( data[3] != null ? 4001 : 3001 ) : 2001;
          } else if ( sqlTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) ) {
              double[] ords = OraUtil.toDoubleArray(_struct,Double.NaN);
              return Double.isNaN(ords[2]) ? 2001 : 3001;
          } 
          // Else ST_ or SDO_
          Struct stGeom = _struct;
          if ( sqlTypeName == Constants.TAG_MDSYS_ST_GEOMETRY)
            	stGeom = SDO_GEOMETRY.getSdoFromST(_struct);

          //if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {stGeom = SDO_GEOMETRY.getSdoFromST(_struct);} 
          BigDecimal bigDec = ((BigDecimal)(stGeom.getAttributes())[0]);
          if (bigDec == null) {
              return _nullValue;
          }
          return bigDec.intValue();
      } catch (SQLException sqle) {
          return _nullValue;
      }
    }
        
    public static int getGType(Struct _struct) {
      return getGType(_struct,0);
    }
    
    public static int getGType(Struct _struct,
                               int    _nullValue) 
    {
        if (_struct == null) {
            return _nullValue;
        }
        return getFullGType(_struct,_nullValue) % 10;    	
    }
 
    public static int getMeasureDimension(Struct _struct) 
    {
        // Note Returning null for null sdo_geometry structure
        if (_struct == null) {
            return 0;
        }
        int fullGtype = getFullGType (_struct,0);
        int dimension = 2;
        if ( fullGtype != 0 ) {
            dimension = (int)((fullGtype % 1000) / 100);
        }
        return dimension;
    }
 
    public static int getDimension(Struct _struct,
                                   int    _nullValue) 
    {
        if (_struct == null) {
            return _nullValue;
        }
        return getFullGType(_struct,_nullValue) / 1000;
    }

    public static int getSRID(Struct _struct) {
      return getSRID(_struct,Constants.SRID_NULL);
    }
    
    public static int getSRID(Struct _struct,
                              int    _nullValue) 
    {
        if (_struct == null) {
            return _nullValue;
        }
        try {
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            Object[] data = (Object[])stGeom.getAttributes();
            int srid = OraUtil.toInteger(data[1], _nullValue);
            if ( srid == 0 ) srid = Constants.NULL_SRID;
            return srid;
        } catch (SQLException sqle) {
            return _nullValue;
        }
    }

    /**
     * ******************************************************* 
     * Some Struct construction methods. 
     * */

    public static Struct setSRID(Struct _struct, 
                                 int    _SRID) 
    {
        if (_struct == null) {
            return _struct;
        }
        try {
        	int sdo_gtype  = SDO_GEOMETRY.getFullGType(_struct, 0);
        	JGeometry geom = JGeometry.loadJS(_struct);
        	JGeometry newGeom = null;
        	double[] sdoPoint = geom.getLabelPointXYZ();
        	Coordinate coord = Double.isNaN(sdoPoint[0])
        			           ? new Coordinate(Double.NaN,Double.NaN,Double.NaN)
        			           : new Coordinate(sdoPoint[0],sdoPoint[1],sdoPoint[2]);
        	newGeom = new JGeometry(
        	               /* int gtype */          sdo_gtype,
        	               /* int srid */           _SRID < 0 ? 0 : _SRID,
        	               /* double x */           Double.isNaN(coord.x) ? null : coord.x,
        	               /* double y */           Double.isNaN(coord.x) ? null : coord.y,
        	               /* double z */           Double.isNaN(coord.x) ? null : coord.z,
        	               /* int[] elemInfo */     geom.getElemInfo(),
        	               /* double[] ordinates */ geom.getOrdinatesArray()
        	          );
            Connection localConnection = DatabaseConnections.getInstance().getActiveConnection();
            Struct stGeom = null;
            stGeom = JGeom.toStruct(newGeom,localConnection);
            return stGeom;
        } catch (Exception e) {
            return _struct;
        }
    }
    
    public static Struct setFullGType(Struct _struct, int _gType) 
    {
        if (_struct == null) {
            return _struct;
        }
        try {
            Object[] data = (Object[])_struct.getAttributes();
            if (data == null) return _struct;
            Connection localConnection = DatabaseConnections.getInstance().getActiveConnection(); 
            if ( localConnection==null ) {
                return _struct;
            }
            Object[] attributes = new Object[]{
            		  _gType,
                      data[1],
                      data[2],
                      data[3],
                      data[4]
                  };
            return localConnection.createStruct(Constants.TAG_MDSYS_SDO_GEOMETRY,
            		                            (Object[])attributes ); 
        } catch (SQLException sqle) {
            return _struct;
        }
    }


    /**
     * *********************************************** 
     * Convenience methods for Struct construction. 
     **/
    
    public static Struct getSdoFromST(Struct _struct) 
    {
        if (_struct == null) {
            return null;
        }
        try {
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                Object[] data = (Object[])_struct.getAttributes();
                if (data == null) return _struct;
                return (Struct)data[0];
            } else {
                return _struct;
            }
        } catch (SQLException sqle) {
            LOGGER.error("SDO_Geometry.getSdoFromST: " + sqle.toString());
            return null;
        }
    }

    public static boolean hasSdoPoint(Struct _struct) {
        return getSdoPoint(_struct,Double.NaN) == null ? false : true;
    }

    public static double[] getSdoPoint(Struct _struct,
                                       double _nullValue) 
    {
        if (_struct == null)
            return null;
        try {
            Struct geoStruct   = _struct;
            Struct sdoPoint    = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
            	geoStruct = SDO_GEOMETRY.getSdoFromST(_struct);
                sqlTypeName = geoStruct.getSQLTypeName();
            }
            if ( sqlTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY))
            {
                Object[] data = (Object[])geoStruct.getAttributes();
                sdoPoint = (Struct)data[2];
                if ( sdoPoint == null )
                    return null;
                sqlTypeName = sdoPoint.getSQLTypeName();
            }
            if ( ! sqlTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) )
               return null;
            return OraUtil.toDoubleArray(sdoPoint, _nullValue);
        } catch (SQLException sqle) {
            return null;
        }
    }

    public static boolean hasElemInfoArray(Struct _struct) 
    {
    	if (_struct == null) {
            return false;
        }
        try {
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            Object[] data = (Object[])stGeom.getAttributes();
            return (data[3]==null) ? false : true;
        } catch (SQLException sqle) {
          return false;
        }
    }

    public static int[] getSdoElemInfo(Struct _struct) 
    {
        if (_struct == null) {
            return null;
        }
        try {
            Struct      stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            Object[] data = (Object[])stGeom.getAttributes();
            return OraUtil.toIntArray((Array)data[3],0);
        } catch (SQLException sqle) {
            return null;
        }
    }

    public static boolean hasOrdinateArray(Struct _struct) 
    {
    	if (_struct == null) {
            return false;
        }
        try {
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            Object[] data = (Object[])stGeom.getAttributes();
            return (data[4]==null) ? false : true;
        } catch (SQLException sqle) {
          return false;
        }
    }
        
    public static int getNumberCoordinates(Struct _struct) 
    {
        if (_struct == null) {
            return 0;
        }
        return getNumberOrdinates(_struct) / getDimension(_struct, 2);
    }

    public static int getNumberOrdinates(Struct _struct) 
    {
        if (_struct == null) {
            return -1;
        }
        try {
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            Object[] data = (Object[])stGeom.getAttributes();
            if (data[2]!=null && data[4]==null) {
                // it is a point encoded in sdo_point_type
                Struct sdoPoint = (Struct)data[2];
                double[] ords = OraUtil.toDoubleArray(sdoPoint,Double.NaN);
                return Double.isNaN(ords[2]) ? 2 : 3;
            }
            Array   dblOArray = (Array)data[4];
            Object[] dblArray = (Object[])dblOArray.getArray();
        	return dblArray.length;
            // return OraUtil.toDoubleArray((Array)data[4], Double.NaN).length;
      } catch (SQLException sqle) {
          return -1;
      }
    }
    
    public static double[] getSdoOrdinates(Struct _struct,
                                           double _nullValue) 
    {
        if (_struct == null) {
            return null;
        }
        try {
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            Object[] data = (Object[])stGeom.getAttributes();
            return OraUtil.toDoubleArray((Array)data[4], _nullValue);
        } catch (Exception e) {
        	LOGGER.warn("Error converting SDO_ORDINATES, possibly NULL valued ordinate.");
            return null;
        }
    }

    public static String applyPrecision(String        _geom, 
                                        DecimalFormat _formatter, 
                                        int           _foldOrds) 
    {
        boolean   valueConversion = false;
        boolean  sdoOrdinateArray = false;
        double           ordinate = 0.0;
        int                  ords = -1;
        boolean separatorHandling = false;
        String       formatString = " )(,";
        
        if ( _formatter.getDecimalFormatSymbols().getDecimalSeparator() == ',' ) {
            // We have to splice the ordinates back together. 
            // Luckily the formatter will not have any groupingSymbol so numbers will be of the form 
            // left , right
            separatorHandling = true;
        }
        
        StringTokenizer dst = new StringTokenizer(_geom.replaceAll(" ",""),formatString,true);
        String          tok = "", 
                    prevTok = ""; 
        String       output = "";
        while ( dst.hasMoreTokens() ) 
        {
            tok = dst.nextToken();
            if (tok.contains("SDO_ELEM_INFO_ARRAY")) {
                if ( _foldOrds > 0 ) output += "\n";
            } if (tok.contains("SDO_ORDINATE_ARRAY") ) {
                if ( _foldOrds > 0 ) output += "\n";
                valueConversion = true;
                sdoOrdinateArray = true;
                ords = -1;
            } else if ( tok.contains("SDO_POINT_TYPE") ) {
                  valueConversion = true;
                  if ( _foldOrds > 0 ) output += "\n";
                  ords = -1;
            } else if ( tok.equals(")") ) {
                ords = -1;
                valueConversion = false;
            }
            if ( valueConversion ) 
            { 
                if ( tok.equals(",") ) 
                    continue;
                
                if ( Character.isDigit(tok.toCharArray()[0]) ||
                     tok.startsWith("-") ||
                     tok.startsWith("+") )
                {
                    try {
                        ords++;
                        if ( separatorHandling ) 
                        {
                            if ( ords > 0 && ords % 2 == 1 ) 
                            {
                              ordinate = Double.parseDouble(prevTok +"." + tok);
                              output += _formatter.format(ordinate) + "," ;
                              prevTok = "";
                            } else {
                              prevTok = tok;
                            }
                        } else {
                            prevTok = "";
                            ordinate = Double.parseDouble(tok);
                            output += _formatter.format(ordinate) + "," ;
                        }
                        if ( sdoOrdinateArray  &&
                             prevTok.isEmpty() &&
                             ords  > 0         && 
                             _foldOrds!=0      && 
                             (ords+1) % _foldOrds == 0 )  
                        {
                          output += "\n";         
                        }
                    } catch(NumberFormatException e) {
                    }
                } else 
                    output += tok;
            } else 
              output += tok;
        }
        return output.replace(",)",")").replace(",\n)",")");
    }

    public static boolean isPoint(Struct _struct) {
        if ( _struct == null ) {
            return false;
        }
        return ( (getFullGType(_struct,1000) % 1000 ) == 1);
    }
    
    /**
     * @function hasArc
     * @param _struct
     * @return
     * @author Simon Greener, January 12th 2011
     */
    public static boolean hasArc(Struct _struct) 
    {
    	int compoundType = compound(_struct);
    	switch (compoundType) {
    	case 0 : return false;
    	case 1 : return true;
    	case 2 : return true;
    	}
    	return false;
    }

    public static boolean hasCompoundCurve(Struct _struct) 
    {
    	// Is a compound linestring that is made up of LineStrings and CircularStrings
    	int compoundType = compound(_struct);
    	switch (compoundType) {
    	case 0 : return false; // Stroked object or point
    	case 1 : return false; // Single CircularString or Circle
    	case 2 : return true;  // CompoundCurve
    	}
    	return false; 
    }

    public static int compound(Struct _struct) 
    {
      if (_struct == null) 
    	  return 0;
      try 
      {
          Struct stGeom = _struct;
          String sqlTypeName = _struct.getSQLTypeName();
          if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
              stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
          } 

          int gtype = SDO_GEOMETRY.getGType(stGeom,-1);
          if (gtype == -1 || gtype == 0 || gtype == 1 || gtype == 5 )
              return 0;
          
          int[] eia = SDO_GEOMETRY.getSdoElemInfo(stGeom);
          if ( eia == null )
        	  return 0;

          int       elements = ( eia.length / 3 );
          int          etype = 0;
          int interpretation = 0;
          for (int i = 1; 
        		   i <= elements; 
        		   i++) 
          {
        	  etype          = eia[(i - 1) * 3 + 1];
        	  interpretation = eia[(i - 1) * 3 + 2];
              if ( etype == 2 && interpretation == 2) 
                  return 1; // Single CircularString
              if ( etype == 4 || etype == 1005 || etype == 2005 )
            	  return 2;  // CompoundCurve
              if ( (etype == 1003 || etype == 2003) 
                   && 
                   (interpretation == 2 || interpretation == 4))
            	  return 1; // CircularString ring or Circle
          }
          return 0;
      } catch (SQLException sqle) {
        return 0;
      }
    }
  
    public static double getLength(Struct _struct, int _precision) {
        Geometry g = asJTSGeometry(_struct,_precision);
        if (g==null)
            return Double.NaN;
        return g.getLength();
    }

    public static double getArea(Struct _struct, int _precision) {
        Geometry g = asJTSGeometry(_struct,_precision);
        if (g==null)
            return Double.NaN;
        return g.getArea();
    }
    
	public static Geometry asJTSGeometry(Struct _struct, int _precision) 
    {
        Geometry geom = null;
        
        // - construct conversion utility for Oracle geometry objects
        OraReader converter = new OraReader(null); // doesn't need the Oracle connection for what we're using it for
        
        // Bug in JTS 1.8.1 and previous in MultiLineHandler sees need to handle linestring differently from polygons in for loop below */
        //
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(_precision));

        // Skip whole record IFF this geometry is for the SHP file and is NULL
        //
        try {
            Struct stGeom = _struct;
            String sqlTypeName = _struct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
            } 
            geom = converter.read((java.sql.Struct)stGeom);
            if (geom == null) {
                return null;   
            }
        } catch (SQLException e) {
            LOGGER.warn("(SDO_Geometry.asJTSGeometry) Error converting SDO_Geometry to JTS Geometry (" + e.getMessage() + ")");
            return null;
        }
        
        if (geom instanceof LineString) 
            geom = geometryFactory.createMultiLineString(new LineString[] { (LineString) geom });
        else
            geom = geometryFactory.createGeometry(geom); // do this to assign the PrecisionModel
        
        return geom;
    }

    public static String getWKT(Struct _geom) { 
    	WKT w = new WKT();
    	try {
			return new String(w.fromStruct(_geom));
		} catch (SQLException | GeometryExceptionWithContext e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }

    public static Struct toSdoGeometry(Connection _conn,
    		                           int        _SDO_GTYPE,
    		                           int        _SDO_SRID,
    		                           double[]   _SDO_POINT,
    		                           int[]      _SDO_ELEM_INFO_ARRAY,
                                       double[]   _SDO_ORDINATE_ARRAY)
    throws SQLException
    {
    	Integer[] _sdo_point = Arrays.stream( _SDO_ELEM_INFO_ARRAY ).boxed().toArray( Integer[]::new );
        Integer[] _sdo_elem_info_array = Arrays.stream( _SDO_ELEM_INFO_ARRAY ).boxed().toArray( Integer[]::new );
        Double[] _sdo_ordinate_array = Arrays.stream( _SDO_ORDINATE_ARRAY ).boxed().toArray( Double[]::new );
    	Struct sdo_point = _conn.createStruct (Constants.TAG_MDSYS_SDO_POINT_TYPE,(Object[])_sdo_point);
    	Array  sdo_elem_info_array = _conn.createArrayOf(Constants.TAG_MDSYS_SDO_ELEM_ARRAY,(Object[])_sdo_elem_info_array);
    	Array  sdo_ordinate_array = _conn.createArrayOf(Constants.TAG_MDSYS_SDO_ORD_ARRAY,(Object[])_sdo_ordinate_array);
    	Object sdoGeometryComponents[] = new Object[] { 
                new NUMBER(_SDO_GTYPE), 
                new NUMBER(_SDO_SRID), 
                sdo_point, 
                sdo_elem_info_array,
                sdo_ordinate_array
                };
        return OraUtil.toStruct(sdoGeometryComponents, OraGeom.TYPE_GEOMETRY, _conn);
    }

}
