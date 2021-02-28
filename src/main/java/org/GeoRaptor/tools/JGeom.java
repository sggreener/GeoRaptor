package org.GeoRaptor.tools;

import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Iterator;
import java.util.List;

import org.GeoRaptor.Constants;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.Queries;
import org.locationtech.jts.io.oracle.OraGeom;
import org.locationtech.jts.io.oracle.OraUtil;

import oracle.jdbc.OracleConnection;
import oracle.spatial.geometry.JGeometry;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.Datum;
import oracle.sql.NUMBER;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

public class JGeom {

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

    public static Struct toStruct(JGeometry _geom, 
                                 Connection _conn) 
    throws Exception 
    {
      Struct stGeom = null;
      String sdo_geometry = null;
	  sdo_geometry = RenderTool.renderGeometryAsPlainText(_geom, Constants.TAG_MDSYS_SDO_GEOMETRY, Constants.bracketType.NONE,12);
      stGeom = Queries.getSdoGeometry(_conn,sdo_geometry);
      // stGeom = JGeom.fromGeometry(_geom,_conn);
      return stGeom;
    }
    
    @SuppressWarnings("deprecation")
	public static Struct fromGeomElements(JGeometry _geom, 
                                         Connection _conn)
    throws Exception
    {
    	BigDecimal SDO_GTYPE = BigDecimal.valueOf(0);
        try {
        	int sdo_gtype = ((_geom.getDimensions() * 1000) + 
                     ((_geom.isLRSGeometry() && _geom.getDimensions()==3) ? 300 
                   : ((_geom.isLRSGeometry() && _geom.getDimensions()==4) ? 400
                   : 0)) + _geom.getType());
        	SDO_GTYPE = BigDecimal.valueOf(sdo_gtype);
        } catch (Exception e) {
        	SDO_GTYPE = BigDecimal.valueOf(0);
        }
        
        int sdo_srid = 0;
        try { sdo_srid = _geom.getSRID(); } catch (Exception e) { sdo_srid = Constants.SRID_NULL; }
        BigDecimal SDO_SRID = BigDecimal.valueOf(sdo_srid);
        
        double[] sdo_point = null;                
        try { 
        	sdo_point = _geom.getLabelPointXYZ()==null 
        			  ? _geom.getPoint() 
                      : _geom.getLabelPointXYZ(); 
        } catch (Exception e) { }
        StructDescriptor sdo_point_descriptor = StructDescriptor.createDescriptor(Constants.TAG_MDSYS_SDO_POINT_TYPE,_conn);
        Datum sdo_point_data[] = new Datum[] {
            OraUtil.toNUMBER(sdo_point[0]),
            OraUtil.toNUMBER(sdo_point[1]), 
            OraUtil.toNUMBER(sdo_point[2])
        };
        STRUCT SDO_POINT = new STRUCT(sdo_point_descriptor, _conn, sdo_point_data);
        
        int[] iSDO_ELEM_INFO_ARRAY = null;
        try { iSDO_ELEM_INFO_ARRAY = _geom.getElemInfo(); } catch (Exception e) { }
        ArrayDescriptor sdo_elem_info_array_descriptor = ArrayDescriptor.createDescriptor(Constants.TAG_MDSYS_SDO_ELEM_ARRAY,_conn);
        ARRAY SDO_ELEM_INFO_ARRAY = new ARRAY(sdo_elem_info_array_descriptor, _conn, iSDO_ELEM_INFO_ARRAY);
        
        double[] dSDO_ORDINATE_ARRAY = null;
        try { dSDO_ORDINATE_ARRAY    = _geom.getOrdinatesArray(); } catch (Exception e) { }
        ArrayDescriptor sdo_ordinate_array_descriptor = ArrayDescriptor.createDescriptor(Constants.TAG_MDSYS_SDO_ORD_ARRAY,_conn);
        ARRAY SDO_ORDINATE_ARRAY = new ARRAY(sdo_ordinate_array_descriptor, _conn, dSDO_ORDINATE_ARRAY);

        // SDO_GEOMETRY
        Datum sdo_geometry_data[] = new Datum[] {
        		  new NUMBER(SDO_GTYPE),
                  new NUMBER(SDO_SRID),
                  SDO_POINT,
                  SDO_ELEM_INFO_ARRAY,
                  SDO_ORDINATE_ARRAY
        };
        StructDescriptor sdo_geometry_descriptor = StructDescriptor.createDescriptor(Constants.TAG_MDSYS_SDO_GEOMETRY,_conn);
        Struct stGeom = null;
        stGeom = new STRUCT(sdo_geometry_descriptor, _conn, sdo_geometry_data);
        return stGeom;
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

	public static JGeometry getDimArrayAsJGeometry(Array _dimArray, int _srid) throws SQLException {
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
				} else if (i >= 2) {
					if (DIM_NAME.toUpperCase().equals("Z")) {
						minZ = SDO_LB;
						maxZ = SDO_UB;
					} else if (DIM_NAME.toUpperCase().equals("M")) {
						minM = SDO_LB;
						maxM = SDO_UB;
					}
				}
			}
			if (arraySize == 2) {
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
}
