package org.GeoRaptor.tools;

import java.awt.geom.Point2D;

import java.math.BigDecimal;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import org.GeoRaptor.Constants;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.Queries;
import org.GeoRaptor.util.logging.Logger;
import org.locationtech.jts.io.oracle.OraUtil;

@SuppressWarnings("deprecation")
public class JGeom {

	private static final Logger LOGGER = org.GeoRaptor.util.logging.Logging.getLogger("org.GeoRaptor.tools.JGeom");

    /**
     * @function gType
     * @param _geom
     * @author Simon Greener, April 2010
     *          Useful function for creating SDO_GEOMETRY SDO_GTYPEs numbers
     *          as JGeometry class does not do this.
     */
    public static int buildFullGType(JGeometry _jGeom) 
    {
        return (_jGeom.getDimensions()  * 1000) + 
               (_jGeom.getLRMDimension() * 100) + 
                _jGeom.getType();
    }

	public static int getGType(JGeometry _geom) {
		return ((_geom.getDimensions() * 1000) + ((_geom.isLRSGeometry() && _geom.getDimensions() == 3) ? 300
				: ((_geom.isLRSGeometry() && _geom.getDimensions() == 4) ? 400 : 0)) + _geom.getType());
	}

	public static JGeometry roundOrdinates(JGeometry _geom, int _round) 
	{
		// Rounding Ordinates ...
		int gType = getGType(_geom);
		int dimension = _geom.getDimensions();
		double[] ords = _geom.getOrdinatesArray();
		
		Double x = null, y = null, z = null;
		if (_geom.isPoint() && ords == null) {
			// A point could be stored in the ordinate array
			// If not, it it in the SDO_POINT_TYPE and needs processing
			//
			double[] point = _geom.getFirstPoint();
			if (point != null && point.length != 0) {
				x = MathUtils.roundToDecimals(point[0], _round);
				y = MathUtils.roundToDecimals(point[1], _round);
				if (dimension > 2) {
					z = MathUtils.roundToDecimals(point[2], _round);
					return new JGeometry(x, y, z, _geom.getSRID());
				} else {
					return new JGeometry(x, y, _geom.getSRID());
				}
			} else {
				return _geom;
			}
		}
		
		if (ords != null) {
			int ordLength = ords.length;
			if (ordLength != 0) {
				for (int i = 0; i < ordLength; i++) {
					ords[i] = MathUtils.roundToDecimals(ords[i], _round);
				}
			}
			return new JGeometry(gType, _geom.getSRID(), _geom.getElemInfo(), ords);
		}
		return _geom;
       			
	}
	
    public static Struct toStruct(JGeometry _jGeom, 
                                 Connection _conn)  
    {
      Struct stGeom = null;
      try 
      {
    	  //stGeom = JGeometry.storeJS(_jGeom,_conn);
    	  stGeom = JGeom.fromGeomElements(_jGeom, _conn);
      } catch (Exception e) {
    	  e.printStackTrace();
    	  // Fall back to string method...
    	  String sdo_geometry = null;
    	  sdo_geometry = RenderTool.renderGeometryAsPlainText(_jGeom, Constants.TAG_MDSYS_SDO_GEOMETRY, Constants.bracketType.NONE,12);
          try 
          {
        	  stGeom = Queries.getSdoGeometry(_conn,sdo_geometry);
          } catch (SQLException e1) {
			  e1.printStackTrace();
          }
      }
      return stGeom;
    }
    
    public static Struct fromGeomElements(JGeometry geom, 
    		                              Connection conn) 
    throws Exception 
    {
    	/*
    	LOGGER.debug("JGeometry: " + geom);
    	LOGGER.debug("Type: " + geom.getType());
    	LOGGER.debug("Dimensions: " + geom.getDimensions());
    	LOGGER.debug("LRS: " + geom.isLRSGeometry());
    	LOGGER.debug("SRID: " + geom.getSRID());
    	LOGGER.debug("Point: " + Arrays.toString(geom.getPoint()));
    	LOGGER.debug("LabelPointXYZ: " + Arrays.toString(geom.getLabelPointXYZ()));
    	LOGGER.debug("ElemInfo: " + Arrays.toString(geom.getElemInfo()));
    	LOGGER.debug("Ordinates.Count: " + geom.getOrdinatesArray().length);
    	*/
        // SDO_GTYPE
        BigDecimal sdoGtype;
        try {
            int dims = geom.getDimensions();
            int type = geom.getType();
            if (dims <= 0 || type <= 0) {
                throw new IllegalArgumentException("Invalid dimensions or type");
            }
            int gtype = (dims * 1000)
                      + (geom.isLRSGeometry() && dims == 3 ? 300
                      : geom.isLRSGeometry() && dims == 4 ? 400 : 0)
                      + type;
            sdoGtype = BigDecimal.valueOf(gtype);
        } catch (Exception e) {
            sdoGtype = BigDecimal.ZERO;
        }

        // SDO_SRID
        BigDecimal sdoSrid;
        try {
            int srid = geom.getSRID();
            sdoSrid = (srid > 0) ? BigDecimal.valueOf(srid) : BigDecimal.valueOf(Constants.SRID_NULL);
        } catch (Exception e) {
            sdoSrid = BigDecimal.valueOf(Constants.SRID_NULL);
        }

        // SDO_POINT
        Struct sdoPoint = null;
        double[] point = null;
        try {
            point = geom.getLabelPointXYZ() != null ? geom.getLabelPointXYZ() : geom.getPoint();
        } catch (Exception e) {
            point = null;
        }
        if (point != null && point.length == 3 &&
            Arrays.stream(point).noneMatch(Double::isNaN)) {
            sdoPoint = conn.createStruct("MDSYS.SDO_POINT_TYPE", new Object[] {
                point[0], point[1], point[2]
            });
        }

        // SDO_ELEM_INFO
        ARRAY elemInfoArray = null;
        try {
            int[] elemInfo = geom.getElemInfo();
            if (elemInfo != null && elemInfo.length > 0) {
                Integer[] boxedElemInfo = Arrays.stream(elemInfo)
                                                .filter(i -> i > 0)
                                                .boxed()
                                                .toArray(Integer[]::new);
                if (boxedElemInfo.length > 0) {
                    ArrayDescriptor elemDesc = ArrayDescriptor.createDescriptor("MDSYS.SDO_ELEM_INFO_ARRAY", conn);
                    elemInfoArray = new ARRAY(elemDesc, conn, boxedElemInfo);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create SDO_ELEM_INFO_ARRAY: " + e.getMessage());
            elemInfoArray = null;
        }

        // SDO_ORDINATES
        Array ordinateArray = null;
        try {
            double[] ordinates = geom.getOrdinatesArray();
            if (ordinates != null && ordinates.length > 0) {
                Double[] boxedOrdinates = Arrays.stream(ordinates)
                                                .filter(d -> !Double.isNaN(d))
                                                .boxed()
                                                .toArray(Double[]::new);
                if (boxedOrdinates.length > 0) {
                    ArrayDescriptor ordDesc = ArrayDescriptor.createDescriptor("MDSYS.SDO_ORDINATE_ARRAY", conn);
                    ordinateArray = new ARRAY(ordDesc, conn, boxedOrdinates);
                }
            }
        } catch (Exception e) {
            ordinateArray = null;
        }

        // Diagnostics
        //LOGGER.debug("SDO_GTYPE: " + sdoGtype);
        //LOGGER.debug("SDO_SRID: " + sdoSrid);
        //LOGGER.debug("SDO_POINT: " + Arrays.toString(point));
        //LOGGER.debug("ElemInfo: " + (elemInfoArray != null ? Arrays.toString((Object[]) elemInfoArray.getArray()) : "null"));
        //LOGGER.debug("Ordinates: " + (ordinateArray != null ? Arrays.toString((Object[]) ordinateArray.getArray()) : "null"));

        // Assemble SDO_GEOMETRY
        return conn.createStruct("MDSYS.SDO_GEOMETRY", new Object[] {
            sdoGtype,
            sdoSrid,
            sdoPoint,
            elemInfoArray,
            ordinateArray
        });
    }
    
	public static JGeometry fromEnvelope(Envelope _mbr,
                                         int      _sourceSRID)    
    {
    	int srid = _sourceSRID == Constants.SRID_NULL ? 0 : _sourceSRID; 
    	JGeometry jGeom = new JGeometry(_mbr.getMinX(),_mbr.getMinY(),
    									_mbr.getMaxX(),_mbr.getMaxY(),
    									srid);
    	return jGeom; 
    }
    
    public static JGeometry fromEnvelope(JGeometry _rectangle) {
        // We only map in 2D so don't worry about loss of other dimensions (yet)
        double[] LL = _rectangle.getFirstPoint();
        double[] UR = _rectangle.getLastPoint();
        int[] elemInfo = {1,1003,1};
        double[] ordArray = {LL[0],LL[1],
                             UR[0],LL[1],
                             UR[0],UR[1],
                             LL[0],UR[1],
                             LL[0],LL[1]};
        return new JGeometry(2003,_rectangle.getSRID(),elemInfo,ordArray);
    }

    public static Point2D getLabelPoint(JGeometry _geo) {
        if ( _geo.getLabelPoint()!=null )
            return _geo.getLabelPoint();
        else {
            double[] points = _geo.getFirstPoint();
            return new Point2D.Double(points[0],points[1]); // Only need X and Y ordinate
        } 
    }

    /**
     * @method getOrientedPointMBR
     * @description MBR of oriented point has to be manually calculated from manipulation of sdo_ordinate_array
     * @param _geo
     * @return
     * @method @method
     * @author @author Simon Greener, June 2010
     */
    public static Envelope getOrientedPointMBR(JGeometry _geo) 
    {
        // Single oriented point uses sdo_ordinate array just as oriented multi point does
        //
        double[] points = _geo.getOrdinatesArray();
        int dim = _geo.getDimensions();
        int coord = 1;
        Envelope mbr = new Envelope(Constants.MAX_PRECISION);
        Point2D point = null;
        for ( int i = 0; i < points.length; i += dim ) {
            if ( coord%dim == 1 ) // Save first point for use with its oriented point
                point = new Point2D.Double(points[i], points[i + 1]);
            else /* oriented */ {
                // Calculate oriented point position
                // and then calc MBR of the pair and union its MBR
                //
                mbr.setMaxMBR(point.getX(),
                              point.getY(),
                              point.getX() + points[i],
                              point.getY() + points[i+1]);
            }
            coord++;
        }
        return mbr;
    }

    public static Envelope getOrdinatesMBR(JGeometry _geo) 
    {
        if ( _geo.isOrientedPoint() || _geo.isOrientedMultiPoint() ) {
            return getOrientedPointMBR(_geo);
        }
        
        // get ordinates
        //
        double[] points = _geo.getOrdinatesArray();
        if ( points==null || points.length==0 )
            return null;
        int dim         = _geo.getDimensions();
        Envelope mbr = new Envelope(Constants.MAX_PRECISION);
        for ( int i = 0; i < points.length; i += dim ) {
            mbr.setMaxMBR(points[i],points[i+1],
                          points[i],points[i+1]);
        }
        return mbr;
    }

    /**
     * @function isRectangle
     * @precis JGeometry.isRectangle does not work in all cases
     * @param _geo
     * @return
     * @author Simon Greener, April 4th 2010
     */
    public static boolean isRectangle(JGeometry _geo) {
        if (_geo == null)
            return false;
  
        if (_geo.isPoint() )
            return false;
        
        if (_geo.isRectangle())
            return true;
  
        int[] eia = _geo.getElemInfo();
        for (int i = 0; i < (eia.length / 3); i++) {
            if ((eia[(i * 3) + 1] == 1003 || eia[(i * 3) + 1] == 2003) &&
                eia[(i * 3) + 2] == 3) {
                return true;
            }
        }
        return false;
    }
  
    /**
     * @function hasArc
     * @precis JGeometry.hasCompoundArc/isCircle, are wrapped in a new function
     *         like isRectangle
     * @param _geo
     * @return
     * @author Simon Greener, April 4th 2010
     */
    public static boolean hasArc(JGeometry _geo) 
    {
        if (_geo == null)
            return false;
  
        if (_geo.isPoint() )
            return false;
        
        if (_geo.hasCircularArcs() || _geo.isCircle())
            return true;
  
        int[] eia = _geo.getElemInfo();
        for (int i = 1; i < (eia.length / 3); i = (i * 3) + 1) {
            if ((eia[i] == 1005 || eia[i] == 2005 || eia[i] == 4) ||
                (eia[i] == 2 && eia[i + 1] == 2)) {
                return true;
            }
        }
        return false;
    }
  
    public static Envelope getGeoMBR(JGeometry _geo) 
    {
        if (_geo == null) return null;
        
        // getMBR returns:
        // a double array containing the minX,minY, maxX,maxY value of the MBR for 2D or
        // a double array containing the minX,minY,minZ maxX,maxY, maxZ value of the MBR for 3D
        //
        int dims = _geo.getDimensions();
        if ( _geo.isOrientedPoint() || _geo.isOrientedMultiPoint() ) {
            return getOrientedPointMBR(_geo);
        } else {
            // getMBR() doesn't seem to return measures in MBR if of type 3302 etc
            //
            if ( _geo.getLRMDimension()==dims )
                dims = 2;
            double[] mbr = null;
            try {
                // getMBR() Gets the MBR of this geometry. When a JSDOGeoemtry is first instantiated from a 
                // db geometry Struct, no MBR is computed. The MBR exists only after the first call to this method. 
                // The MBR will be recalcuated only when the geoemtry's structure has been modified.
                //
                mbr = _geo.getMBR();
            } catch (Exception e) {
                try {
                  mbr = _geo.getMBR();
                } catch (Exception e2) {
                  mbr = null; 
                }
            }
            if ( mbr == null )
                return null;
            if ( mbr.length < 4 || Double.isInfinite(mbr[0]) || Double.isNaN(mbr[0]) )
                return getOrdinatesMBR(_geo);
            if ( mbr.length < 4 )
                return null;
            switch ( dims ) {
                case 4  : return new Envelope(mbr[0], mbr[1], mbr[2], mbr[3]);
                case 3  : return new Envelope(mbr[0], mbr[1], mbr[3], mbr[4]);
                case 2  : 
                default : return new Envelope(mbr[0], mbr[1], mbr[2], mbr[3]); 
            }
        }

    }

    public static Envelope getGeoMBR(List<JGeometry> _geomSet) 
    {
        if (_geomSet == null || _geomSet.size() == 0) return null;
        
        Envelope mbr = new Envelope(Constants.MAX_PRECISION);
        Iterator<JGeometry> iter = _geomSet.iterator();
        while (iter.hasNext()) {
            mbr.setMaxMBR(getGeoMBR(iter.next()));
        }
        return mbr;
    }

	public static JGeometry getDimArrayAsJGeometry(
	                Array   _dimArray, 
                    int     _srid,
                    boolean _2D) 
 	throws SQLException 
	{
		if (_dimArray == null) {
			return null;
		}
		if (_dimArray.getBaseTypeName().equals(Constants.TAG_MDSYS_SDO_ELEMENT)) {
			Object[] dimElements = (Object[]) _dimArray.getArray();
			int arraySize = dimElements.length;
			if (arraySize == 0)
				return null;

			String DIM_NAME = "";
			double SDO_LB = Double.NaN;
			double SDO_UB = Double.NaN;
			double minX = 0.0, minY = 0.0, maxX = 0.0, maxY = 0.0;
			double minZ = Double.NaN, maxZ = Double.NaN, minM = Double.NaN, maxM = Double.NaN;

			for (int i = 0; i < arraySize; i++) {
				Object[] element = ((Struct) dimElements[i]).getAttributes();
				DIM_NAME = (String) element[0];
				SDO_LB = OraUtil.toDouble(element[1], Double.NaN);
				SDO_UB = OraUtil.toDouble(element[2], Double.NaN);
				if (i == 0) {
					minX = SDO_LB;
					maxX = SDO_UB;
				} else if (i == 1) {
					minY = SDO_LB;
					maxY = SDO_UB;
				} else if (i >= 2 && ! _2D) {
					if (DIM_NAME.toUpperCase().equals("Z")) {
						minZ = SDO_LB;
						maxZ = SDO_UB;
					} else if (DIM_NAME.toUpperCase().equals("M")) {
						minM = SDO_LB;
						maxM = SDO_UB;
					}
				}
			}
			if (arraySize == 2 || _2D) {
				return new JGeometry(minX, minY, maxX, maxY, _srid > 0 ? _srid : Constants.NULL_SRID);
			} else if (arraySize == 3) {
				if (Double.isNaN(minZ)) {
					return new JGeometry(3303, _srid > 0 ? _srid : Constants.NULL_SRID, new int[] { 1, 1003, 3 },
							new double[] { minX, minY, minM, maxX, minY, minM, maxX, maxY, maxM, minX, maxY, maxM, minX,
									minY, minM });

				} else {
					return new JGeometry(3003, _srid > 0 ? _srid : Constants.NULL_SRID, new int[] { 1, 1003, 3 },
							new double[] { minX, minY, minZ, maxX, minY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX,
									minY, minZ });
				}
			} else if (arraySize == 4) {
				return new JGeometry(4403, _srid > 0 ? _srid : Constants.NULL_SRID, new int[] { 1, 1003, 3 },
						new double[] { minX, minY, minZ, minM, maxX, minY, minZ, minM, maxX, maxY, maxZ, maxM, minX,
								maxY, maxZ, maxM, minX, minY, minZ, minM });
			}
		}
		return null;
	}

	public static String toString(JGeometry _jGeom)
	{
		return RenderTool.renderGeometryAsPlainText(_jGeom,"MDSYS.SDO_GEOMETRY",Constants.bracketType.NONE,12);
	}
	
	public static JGeometry setSrid(JGeometry _jGeom,
			                        int _srid )
	{
		JGeometry nGeom = _jGeom;
		nGeom.setSRID(_srid);
		return nGeom;
	}
}
