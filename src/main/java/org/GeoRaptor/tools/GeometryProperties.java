package org.GeoRaptor.tools;

import java.util.ArrayList;
import java.util.List;

import org.GeoRaptor.Constants;

import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;


public class GeometryProperties {

    private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.tools.GeometryProperties");

    protected int FULL_GTYPE = 2001;    
    protected int       SRID = Constants.SRID_NULL;
    protected int  DIMENSION = 2;
    protected Constants.GEOMETRY_TYPES GEOMETRY_TYPE = Constants.GEOMETRY_TYPES.UNKNOWN;
    protected List<String> shapeTypes = new ArrayList<String>(20);
    protected ShapeType shapefileType = ShapeType.UNDEFINED;
    
    public GeometryProperties() {
        super();
    }

    public boolean hasMeasure( ) {
        return ((this.FULL_GTYPE/100) % 10) == 0 ? false : true;
    }

    public boolean hasZ( ) {
        int numberOrdinates = this.FULL_GTYPE/1000;
        switch ( numberOrdinates ) {
          case 4 : return true;
          case 3 : return ! hasMeasure();
          default:
          case 2 : return false;
        }
    }  
    
    public int getFullGType() {
        return this.FULL_GTYPE;
    }

    public void setFullGType(int GTYPE) {
        this.FULL_GTYPE = GTYPE;
    }

    public int getGType() {
        return this.FULL_GTYPE % 10;
    }
  
    public int getDimension() {
        return this.DIMENSION;
    }

    public void setDimension(int _dimension) {
        this.DIMENSION = _dimension;
    }

    public int getSRID() {
        return this.SRID;
    }

    public void setSRID(int SRID) {
        this.SRID = SRID;
    }

    public Constants.GEOMETRY_TYPES getGeometryType() {
        return this.GEOMETRY_TYPE;
    }

    public void setGeometryType(Constants.GEOMETRY_TYPES _geometryType) {
        this.GEOMETRY_TYPE = _geometryType;
    }

    public void setShapefileType(ShapeType _shapeType) {
LOGGER.warn("(GeometryProperties.setShapefileType) " + _shapeType==null?"null":_shapeType.toString());
        this.shapefileType = _shapeType;
    }

    public Object getShapefileType(boolean _asShapeType) {
          return _asShapeType 
                 ? this.shapefileType 
                 : Constants.SHAPE_TYPE.getShapeType(this.shapefileType.id);
    }
      
    public void addShapeType(ShapeType _shapeType) {
        if ( _shapeType.id != ShapeType.UNDEFINED.id ) {
            if ( ! shapeTypes.contains(_shapeType.toString().toUpperCase()) ) {
                shapeTypes.add(_shapeType.toString().toUpperCase());
            }
        }
    }

    public List<String> getShapeTypes() {
        return this.shapeTypes;
    }
    
    public Constants.SHAPE_TYPE getShapeType()
    {
        int shapeType = SDO_GEOMETRY.getShapeType(this.getGType(),true).id;
        return Constants.SHAPE_TYPE.getShapeType(shapeType);
    }

    public String shapeTypesAsString() {
        String shpTypes = "";
        for (int i = 0; i < shapeTypes.size(); i++) {
            shpTypes += ((i>1)?",":"") + shapeTypes.get(i);
        }
        return shpTypes;        
    }
    
    public String toString() {
        return String.format("SDO_GTYPE=%d,DIMENSION=%d,SDO_SRID=%d,GEOMETRY_TYPE=%s,SHAPE_TYPES={%s}",FULL_GTYPE,DIMENSION,SRID,GEOMETRY_TYPE.toString(),shapeTypesAsString());
    }
    
}
