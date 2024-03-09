package org.GeoRaptor.tools;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.util.logging.Logger;
import org.locationtech.jts.geom.Coordinate;

public class JTSGeometry {

    private static final Logger LOGGER = org.GeoRaptor.util.logging.Logging.getLogger("org.GeoRaptor.tools.JTSGeometry");

    public static int getCoordinateDimension( Geometry geom ) {
    	Coordinate coord = geom.getCoordinate();
    	if ( coord == null )
    		return 2;
    	if ( Double.isNaN(coord.getM()) && Double.isNaN(coord.getZ()) ) 
    		return 4;
    	if ( Double.isNaN(coord.getM()) || Double.isNaN(coord.getZ()) ) 
    		return 3;
    	return 2;
    }  
    
	public static Geometry fromWKT(String wkt) {
		try {
			return new WKTReader().read(wkt);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
    
    public static String toWKT(Geometry geom) {
    	int coordDim = JTSGeometry.getCoordinateDimension(geom);
       	return new WKTWriter(coordDim).write(geom);
    }

    public static Geometry orientRings( Geometry 		_geom,
    	    							GeometryFactory _geomFactory)
    {
        LOGGER.debug("orientRings()");

    	Preferences geoRaptorPreferences = null;
        geoRaptorPreferences = MainSettings.getInstance().getPreferences();

        LOGGER.debug("\tRequired Orientation is " + geoRaptorPreferences.getShapePolygonOrientation());

        LinearRing    eRing = null;
        LinearRing[] iRings = null;
     
        Geometry jtsGeom = _geom;
        if (_geom instanceof Polygon) {
            LOGGER.debug("Polygon detected");
            //Let's print out our JTS Geometry to see if it appears to be structured OK.
            PrintGeometry.printJTSGeometry(jtsGeom);
            //See if problem is with general casting and nothing to do with my converted Struct
            LOGGER.debug("POLYGON((0 0,10 0,10 10, 0 10, 0 0))");
            jtsGeom = JTSGeometry.fromWKT("POLYGON((0 0,10 0,10 10, 0 10, 0 0))");
            // This is failing....
        	Polygon poly = (Polygon) jtsGeom;
            LOGGER.debug("Cast JTS Geometry to Polygon: Required Orientation " + geoRaptorPreferences.getShapePolygonOrientation().toString());
            switch (geoRaptorPreferences.getShapePolygonOrientation()) {
              case INVERSE      : 
              case CLOCKWISE    : eRing = (LinearRing)poly.getExteriorRing().reverse(); 
                                  break;
              case ANTICLOCKWISE: eRing = (LinearRing)poly.getExteriorRing(); 
                                  break;
			  case ORACLE       :
                  default       : break;
            }
            LOGGER.debug("NumInteriorRings: " + poly.getNumInteriorRing());
            iRings = new LinearRing[poly.getNumInteriorRing()];
            for (int i=0; i < poly.getNumInteriorRing();  i++) {
                // Oracle polygons inner rings have clockwise rotation
                switch (geoRaptorPreferences.getShapePolygonOrientation()) {
                  case INVERSE       : 
                  case ANTICLOCKWISE : iRings[i] = (LinearRing)poly.getInteriorRingN(i).reverse(); 
                                       break;
                  case CLOCKWISE     : iRings[i] = (LinearRing)poly.getInteriorRingN(i); 
                                       break;
				  case ORACLE      	 :
                  default       	 : break;
                }
            }
            LOGGER.debug("geomFactory.createPolygon("+eRing+ "," + iRings + ")");
            jtsGeom = _geomFactory.createPolygon(eRing, iRings);
        } else if (_geom instanceof MultiPolygon ) {
        	LOGGER.debug("MultiPolygon detected");
        	Polygon       poly = null;
            MultiPolygon mPoly = (MultiPolygon) _geom;
            Polygon[]    polys = new Polygon[mPoly.getNumGeometries()];
            for (int p=0; p < mPoly.getNumGeometries(); p++ ) {
                poly   = (Polygon)mPoly.getGeometryN(p);
                switch (geoRaptorPreferences.getShapePolygonOrientation()) {
                  case INVERSE      : 
                  case CLOCKWISE    : eRing = (LinearRing)poly.getExteriorRing().reverse(); 
                                      break;
                  case ANTICLOCKWISE: eRing = (LinearRing)poly.getExteriorRing(); 
                                      break;
				  case ORACLE       :
                  default       	: break;
                }
                iRings = new LinearRing[poly.getNumInteriorRing()];
                for (int i=0; i < poly.getNumInteriorRing();  i++) {
                    // Oracle polygons inner rings have clockwise rotation
                    switch (geoRaptorPreferences.getShapePolygonOrientation()) {
                      case INVERSE       : 
                      case ANTICLOCKWISE : iRings[i] = (LinearRing)poly.getInteriorRingN(i).reverse(); 
                                           break;
                      case CLOCKWISE     : iRings[i] = (LinearRing)poly.getInteriorRingN(i); 
                                           break;
					  case ORACLE        :
                      default       	 : break;
                    }
                }
                polys[p] = _geomFactory.createPolygon(eRing, iRings);
            }
            jtsGeom = _geomFactory.createMultiPolygon(polys);
        } 
        return jtsGeom;
    }

}
