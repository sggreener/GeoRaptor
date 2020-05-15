package org.GeoRaptor.tools;

import org.GeoRaptor.Constants;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;

import java.awt.geom.Point2D;

import java.util.Iterator;
import java.util.List;

import oracle.spatial.geometry.JGeometry;


public class JGEOM
{    

    public static final int                 SRID_NULL = -1;    
    public static final int                 NULL_SRID = -1;
    public static final int                   NO_SRID = 0;
    
    public static JGeometry rectangle2Polygon2D(JGeometry _rectangle) {
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
            mbr.setMaxMBR(SDO.getGeoMBR(iter.next()));
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
    public static boolean hasArc(JGeometry _geo) {
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
  
    /**
     * @function buildFullGType
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
        /*    
        return ((_geom.getDimensions() * 1000) +
                ((_geom.isLRSGeometry() && _geom.getDimensions() == 3) ? 300 :
                 ((_geom.isLRSGeometry() && _geom.getDimensions() == 4) ? 400 :
                  0)) + _geom.getType());
        */
    }


}
