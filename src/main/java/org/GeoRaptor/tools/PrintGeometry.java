package org.GeoRaptor.tools;

import org.GeoRaptor.Constants;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import oracle.spatial.geometry.JGeometry;

public class PrintGeometry {

    // ********************************************************************

    public static void printJTSGeometry(Geometry _geom) 
    {
    	System.out.println("  GeometryType: " + _geom.getGeometryType());
    	System.out.println("    Dimensions:" + _geom.getDimension());
    	System.out.println("          SRID: " + _geom.getSRID());
    	System.out.println("      Envelope:" + _geom.getEnvelope().toString());
    	System.out.println(" NumGeometries: "+ _geom.getNumGeometries());
    	System.out.println("     NumPoints: "+ _geom.getNumPoints());
    	System.out.println("      Envelope:" + _geom.getEnvelope().toString());
    	for (int i=1; i<=_geom.getNumGeometries();i++)
    	{
    		Geometry geom = _geom.getGeometryN(i);
    		System.out.println("Geometry(" + i + ")");
    		Coordinate[] coord = geom.getCoordinates();
    		for (int c=0;c<coord.length;c++) 
    		{
    			System.out.println("Coordinate(" + (c+1) + ") = " + coord[c].toString());
    		}
    	}	
    }
    
    /**
     * @function printGeometryType
     * @param _gtype
     * @param _hasArc
     * @author Simon Greener, April 2010
     *          Useful function for debugging JGeometries
     */
    public static void printGeometryType(int _gtype, boolean _hasArc) {
        String compound = _hasArc ? "(C)" : "";
        switch (_gtype) {
        case JGeometry.GTYPE_POINT:        System.out.println("POINT") ;                  break; 
        case JGeometry.GTYPE_POLYGON:      System.out.println("POLYGON"      + compound); break; 
        case JGeometry.GTYPE_CURVE:        System.out.println("CURVE"        + compound); break; 
        case JGeometry.GTYPE_MULTICURVE:   System.out.println("MULTICURVE"   + compound); break; 
        case JGeometry.GTYPE_MULTIPOINT:   System.out.println("MULTIPOINT");              break; 
        case JGeometry.GTYPE_MULTIPOLYGON: System.out.println("MULTIPOLYGON" + compound); break; 
        case JGeometry.GTYPE_COLLECTION:   System.out.println("COLLECTION"   + compound); break; 
        default: System.out.println("UNKNOWN");
        }
    }

    public static void printElemInfoArray(JGeometry _geo) 
    {
        if (_geo == null || _geo.getOrdinatesArray()==null )
            return;
        int[] eia = _geo.getElemInfo();
        printElemInfo(eia);
    }

    /**
     * @function printElemInfo
     * @param _eia
     * @author Simon Greener, April 2010
     *          Useful function for printing ElemInfo arrays
     */
    public static void printElemInfo(int[] _eia) 
    {
        if (_eia == null )
            return;
        String elemInfoArray = Constants.TAG_MDSYS_SDO_ELEM_ARRAY + "(";
        for (int i = 0; i < (_eia.length / 3); i++) {
        	elemInfoArray += _eia[(i * 3)] + "," + _eia[(i * 3) + 1] + "," + _eia[(i * 3) + 2];
        }
        elemInfoArray += ")";
        System.out.println(elemInfoArray);
    }

    /**
     * @function printOrdArray
     * @param _geo
     * @author Simon Greener, April 2010
     *          Useful function for debugging ordArrays
     */
    public static void printOrdArray(JGeometry _geo) 
    {
        if (_geo == null )
            return;
        double[] _points = _geo.getOrdinatesArray();
        if ( _points ==null )
        	return;
        int dim          = _geo.getDimensions();
        int numCoords    = _geo.getNumPoints();
        String coordString = Constants.TAG_MDSYS_SDO_ORD_ARRAY + "(";
        for (int coord = 0; coord < numCoords; coord++ ) 
        {
        	int ordPosition = coord * dim;  // Java arrays start with 0 not 1 as in oracle.
            coordString += (coord==0 ? "" : ",") + _points[ordPosition] + "," + _points[ordPosition + 1];
            if ( dim == 3) 
            	coordString += "," + _points[ordPosition + 2];
            if ( dim == 4 )
            	coordString += "," + _points[ordPosition + 3];
        }
        coordString += ")";
        System.out.println(coordString);
    }
	
}
