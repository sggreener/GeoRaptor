package org.GeoRaptor.SpatialView.JDevInt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;

import oracle.dbtools.raptor.controls.cellrenderers.ICellRenderer;

import oracle.spatial.geometry.JGeometry;
import oracle.spatial.util.GML2;
import oracle.spatial.util.GML3;
import oracle.spatial.util.KML2;
import oracle.spatial.util.WKT;

import oracle.sql.ARRAY;
import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;

import org.geotools.util.logging.Logger;


@SuppressWarnings("deprecation")
public class SpatialRenderer 
  implements ICellRenderer {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.JDevInt.SpatialRenderer");

    protected static SpatialRenderer classInstance;
    
    private final static String iconDirectory = "org/GeoRaptor/SpatialView/images/";
    private ClassLoader cl             = null;
    private ImageIcon iconQuestionMark = null;   

    /**
     * dbConnection
     */
    private Connection dbConnection; 
    
    /**
     * For access to preferences.
     */
    protected Preferences GeoRaptorPrefs;
    
    /**
     * @author : Simon Greener - 13th April 2010 
     *           - Modified to use Constants class
     **/
    public SpatialRenderer() 
    {
        this.GeoRaptorPrefs = MainSettings.getInstance().getPreferences();
        this.cl = this.getClass().getClassLoader();
        this.iconQuestionMark = new ImageIcon(cl.getResource(iconDirectory + "icon_question_mark.png"));
    }

    /**
     * Get an instance of SpatialRenderer class.
     * @return SpatialRenderer class instance
     */
    public static SpatialRenderer getInstance() {
        if (SpatialRenderer.classInstance == null) {
            SpatialRenderer.classInstance = new SpatialRenderer();
        }
        return SpatialRenderer.classInstance;
    }

    /**
     * getComponent() is called first. 
     * If nothing is returned from any of the registered addins the getText is called next and will just be used in the standard component.
    **/
    public String getText(JTable  table, 
                          Object  value, 
                          boolean isSelected,
                          boolean hasFocus, 
                          int     row, 
                          int     column) 
    {
      String retLabel = value==null ? Constants.NULL : "";  
      if ( table.getSelectionModel().getValueIsAdjusting() || value == null ) {
        return retLabel;
      }
      return this.renderGeoObject(value,true);
    }

    /**
     * getComponent() is called first. 
     * If nothing is returned from any of the registered addins the getText is called next and will just be used in the standard component.
     * 
     * @author : Simon Greener - March 2010 
     *           - Changed getSQLTypeName().equals
     *             to static string constant. Made change to way rendering 
     *             is called to cater for STRUCT or JGeometry based processing.
     *             It seems to me that STRUCT based rendering saves a conversion
     *             to JGeometry so must be faster.
     * @author : Simon Greener - 13th April 2010 
     *           - Moved to use Constants class
     *           - First stage implementation of GML/KML/WKT etc user selectable rendering.
     *           - Also, some work was needed to get a valid Oracle connection for the sdoutl.jar
     *             based conversion functions.
     * @author : Simon Greener - May 27th 2010 
     *            When Sdo_Geometry data is 3D or above and WKT is selected we get a Java Exception.
     *            Code added to change WKT dynamically to SDO_GEOMETRY based rendering.
     *            May need to make this change for GML etc.
     * @author Simon Greener, June 8th 2010 
     *            Broke code into components based on structure/data type being rendered.
     **/
    public Component getComponent(JTable  table, 
                                  Object  value,
                                  boolean isSelected, 
                                  boolean hasFocus,
                                  int     row, 
                                  int     column) 
    {
        JLabel retLabel = new JLabel(value == null ? Constants.NULL : "");  
        retLabel.setOpaque(true);
        retLabel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        if ( table.getSelectionModel().getValueIsAdjusting() || value == null )
          return retLabel;

        // Get rendering 
        //
        Dimension imageSize = new Dimension(table.getRowHeight(),table.getRowHeight());
        try {
            String sqlTypeName = "";
            if ( value instanceof STRUCT ) {
                sqlTypeName = ((STRUCT)value).getSQLTypeName();
                if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ||
                     sqlTypeName.equals(Constants.TAG_MDSYS_SDO_GEOMETRY) ) 
                {
                    retLabel = this.getSdoRenderLabel((STRUCT)value,true,imageSize);
                    // Because getSdoRenderLabel returns new JLabel
                    retLabel.setOpaque(true);
                    retLabel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                } else {
                    // SDO_POINT, VERTEX_TYPE etc rendered normally
                    //
                    retLabel.setText(this.renderGeoObject(value,true));
                }
            } else {
                retLabel.setText(this.renderGeoObject(value,true));
            }
        } catch (SQLException e) {
            LOGGER.error(e.toString());
            retLabel.setText(this.renderGeoObject(value,true));
        }
        return retLabel;
    }

    public JLabel getSdoRenderLabel(STRUCT    _geoStruct,
                                    boolean   _colourText,
                                    Dimension _imageSize )
    { 
      if ( _geoStruct == null ) return new JLabel(Constants.NULL);      
      JLabel retLabel = new JLabel();
      switch ( this.GeoRaptorPrefs.getVisualFormat() ) {
          case SDO_GEOMETRY : 
          case WKT          :
          case GML2         :
          case GML3         :
          case KML          :
          case KML2         : retLabel.setText(this.renderSdoGeometry(_geoStruct,_colourText)); 
                              break;
          case ICON         : retLabel = this.getIcon(_geoStruct,_imageSize); 
                              break;
          case THUMBNAIL    : int gtype = SDO_GEOMETRY.getGType(_geoStruct,-1);
                              if (  gtype == JGeometry.GTYPE_POINT || 
                                    gtype == 8 /* SOLID */         || 
                                    gtype == 9 /* MultiSolid */ )
                              { 
                                  retLabel = this.getIcon(_geoStruct,_imageSize); 
                              } else {
                                  retLabel = this.getPreview(_geoStruct,_imageSize);
                              }
                              break;
      }
      if ( retLabel.getIcon()==null ) {
          // Create text
          retLabel.setText(this.renderSdoGeometry(_geoStruct,_colourText));
      }
      return retLabel;
    }

    /** 
     * htmlWrap.
     * @param _html snippet 
     * @return _html wrapped with <html><body/>_html<html/> tags
     */
    public String htmlWrap(String _html) {
        return RenderTool.htmlWrap(_html);
    }
    
    public JLabel getPreview(STRUCT    _geoStruct, 
                             Dimension _imageSize)
    {
        if ( _geoStruct == null ) return new JLabel(Constants.NULL);
        String sqlTypeName = "";
        try { sqlTypeName = _geoStruct.getSQLTypeName(); } catch (SQLException e) {LOGGER.error("getPreview: Failed to get sqlTypeName of Struct");  return new JLabel(Constants.NULL);}
        STRUCT stGeom = ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) ? SDO_GEOMETRY.getSdoFromST(_geoStruct) 
                        : _geoStruct;
        JGeometry geom = null;
        try { geom = JGeometry.load(stGeom); } catch (SQLException e) { geom = null; }
        if ( geom == null ) {
            LOGGER.error("Failed to get convert STRUCT to JGeometry");
            return new JLabel(this.renderGeoObject(stGeom,true));
        }
        return getPreview(geom,null,_imageSize);
    }

    public JLabel getPreview(JGeometry _geom, 
                               Dimension _imageSize)
    {
        return getPreview(_geom,null,_imageSize);
    }

    public JLabel getPreview(JGeometry       _geom, 
                             List<JGeometry> _geomSet,
                             Dimension       _imageSize)
    {
        if ( _geom    == null && 
             _geomSet == null ||
             (_geomSet != null && 
              _geomSet.size() == 0 ) )  {
            return new JLabel(Constants.NULL);
        }

        List<JGeometry> geoSet = null;
        
        JGeometry jGeom = _geom!=null?_geom:(_geomSet.size()==1?_geomSet.get(0):null);    
        if ( jGeom != null ) {
            int gtype = jGeom.getType();
            if (gtype == JGeometry.GTYPE_COLLECTION ) {
                geoSet = new ArrayList<JGeometry>();
                // Need to extract individual elements that can be mapped to Shapes
                JGeometry[] _geoList = jGeom.getElements();
                for (JGeometry _g : _geoList) {
                    if ( _g != null ) {
                        geoSet.add(_g);
                    }
                }
            } else {
                geoSet = new ArrayList<JGeometry>(1);
                geoSet.add(jGeom);
            }
        } else {
            geoSet = new ArrayList<JGeometry>(_geomSet.size());
            geoSet.addAll(_geomSet);
        }
        
        JLabel retLabel = new JLabel();
        Envelope mbr = SDO_GEOMETRY.getGeoMBR(geoSet);
        if (mbr == null) {
            LOGGER.error("Failed to get geometry(s) MBR");
            return retLabel;
        }
        // If either side of MBR is 0 then modify it.
        //
        if ( mbr.getWidth()==0 || mbr.getHeight()==0 ) {
            double halfSide = Math.max(mbr.getWidth(),
                                       mbr.getHeight()) / 2.0;
            if ( halfSide == 0.0 ) halfSide = 0.5;
            if ( mbr.getWidth()==0 ) {
                mbr = new Envelope(mbr.centre().getX() - halfSide,
                                          mbr.centre().getY(),
                                          mbr.centre().getX() + halfSide,
                                          mbr.centre().getY());
            } 
            if ( mbr.getHeight()==0 ) {
                mbr = new Envelope(mbr.centre().getX(),
                                          mbr.centre().getY() - halfSide,
                                          mbr.centre().getX(),
                                          mbr.centre().getY() + halfSide);
            }
        }
        // Expand MBR
        //
        mbr = mbr.increaseByPercent(10);
        // Normalise image
        // Because mainly used in rowset image we normalise so that the X dimension is > Y dimension
        double normaliseRatio = ((mbr.getWidth() / mbr.getHeight()) < 1) ? ((mbr.getHeight() / mbr.getWidth())): ((mbr.getWidth() / mbr.getHeight()));
        Dimension imageSize = new Dimension((int)((_imageSize == null ? GeoRaptorPrefs.getPreviewImageWidth()  : _imageSize.getWidth()) * normaliseRatio),
                                            (int) (_imageSize == null ? GeoRaptorPrefs.getPreviewImageHeight() : _imageSize.getHeight()));
        // Don't want image to disappear to nothing.....
        //
        if ( imageSize.width < 5 ) {
            imageSize.setSize(5, imageSize.height);
        }
        mbr.Normalize(imageSize);
//LOGGER.info("mbr:" + mbr.toString() + " imageSize:" + imageSize.toString());
        // Create transformation from world to image
        //
        AffineTransform af = null; 
        try { af = getWorldToScreenTransform(mbr,imageSize); } catch (Exception e) { af = null; }
        if ( af == null ) {
          LOGGER.error("Failed to create Affine Transformation for mbr");
          retLabel.setText(mbr.toString());
          return retLabel;
        }

        // Create image and paint background
        //
        BufferedImage img = new BufferedImage(imageSize.width, imageSize.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0,0,imageSize.width, imageSize.height);

        // Process all Java shapes
        //
        int gtype = -1;
        Shape shp = null;
        Point2D point2D = null;
        Iterator<JGeometry> iter = geoSet.iterator();
        while (iter.hasNext()) {
            jGeom = iter.next();
            gtype = jGeom.getType();
            if ( gtype != JGeometry.GTYPE_MULTIPOINT &&
                 gtype != JGeometry.GTYPE_POINT ) { 
                try {
                    shp = jGeom.createShape(af,true/*Simplify as is Thumnail*/);
                } catch (ArrayIndexOutOfBoundsException aibe) {
                    LOGGER.error("SpatialRenderer - ArrayIndexOutOfBounds Exception (" + aibe.getMessage() + ") for Geometry (" + gtype + ") " +
                                 (jGeom.hasCircularArcs() ? "with" : "without") + " CircularArcs." );
                    shp = null;
                } catch (Exception e) {
                    LOGGER.error("SpatialRenderer - Error transforming geometry to mappable shape.\n" + e.getMessage());
                    shp = null;
                }
                if (shp == null) {
                    LOGGER.error("SpatialRenderer - No mappable shape exists for geometry.");
                    continue;
                }
            }
            
            // now draw/fill shape
            //
            switch (gtype) {
            case JGeometry.GTYPE_MULTIPOINT:
                point2D = new Point.Double(Double.MIN_VALUE,Double.MIN_VALUE);
                g2d.setColor(Color.RED);
                double[] points = jGeom.getOrdinatesArray();
                int i = 0;
                int dims = jGeom.getDimensions();
                if ( points.length != 0 ) {
                    for (int ord=0; ord<jGeom.getNumPoints(); ord++) { 
                        i = ord * dims;
                        point2D.setLocation(points[i],points[i+1]); 
                        g2d.fill(PointMarker.getPointShape(PointMarker.MARKER_TYPES.STAR,
                                                           af.transform(point2D, null),
                                                           10,
                                                           0.0f));
                    }
                }
                break;
            
            case JGeometry.GTYPE_POLYGON      :
            case JGeometry.GTYPE_MULTIPOLYGON :
                g2d.setColor(Color.RED);
                g2d.fill(shp);
                g2d.setColor(Color.BLACK);
                g2d.draw(shp);
                break;
            
            case JGeometry.GTYPE_CURVE        :
            case JGeometry.GTYPE_MULTICURVE   :
                g2d.setColor(Color.BLUE);
                g2d.draw(shp);
                break;
            
            
            } /* switch */
            
            // A linear or polygon geometry can have an SDO_POINT.
            point2D = jGeom.getLabelPoint();
            if ( point2D != null ) {
                g2d.setColor(Color.GREEN);
                g2d.fill(PointMarker.getPointShape(PointMarker.MARKER_TYPES.STAR,
                                                   af.transform(point2D, null),
                                                   6,
                                                   0.0f));
            }
            
        }
        Icon icon = new ImageIcon(img);
        retLabel.setIcon(icon);
        return retLabel;
    }
  
    public JLabel getIcon(STRUCT    _geoStruct, 
                          Dimension _imageSize)
    {
        JLabel retLabel = new JLabel();
        Icon icon = null;
        if ( _geoStruct==null ) {
            return retLabel;
        }
        STRUCT _gStruct = _geoStruct;
        Constants.GEOMETRY_TYPES geomType = Constants.GEOMETRY_TYPES.UNKNOWN;
        int gtype = -1; 
        try {
            String sqlTypeName = _gStruct.getSQLTypeName();
            if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                STRUCT stGeom = SDO_GEOMETRY.getSdoFromST(_gStruct);
                gtype = SDO_GEOMETRY.getGType(stGeom,-1);
            } else if ( sqlTypeName.equals(Constants.TAG_MDSYS_SDO_GEOMETRY) ) {
                gtype = SDO_GEOMETRY.getGType(_gStruct,-1);
            } else if ( sqlTypeName.equals(Constants.TAG_MDSYS_VERTEX_TYPE) ) {
                gtype = 1;
            } else if ( sqlTypeName.equals(Constants.TAG_MDSYS_SDO_POINT_TYPE) ) {
                gtype = 1;
            }
        } catch (SQLException e) { gtype = -1; }
        switch (gtype) {
            case JGeometry.GTYPE_POINT        : geomType = Constants.GEOMETRY_TYPES.POINT;      break;
            case JGeometry.GTYPE_MULTIPOINT   : geomType = Constants.GEOMETRY_TYPES.MULTIPOINT; break;
            case 8                /* SOLID */ :
            case 9           /* MULTISOLID */ :
            case JGeometry.GTYPE_POLYGON      :
            case JGeometry.GTYPE_MULTIPOLYGON : geomType = gtype==JGeometry.GTYPE_POLYGON
                                                    ? Constants.GEOMETRY_TYPES.POLYGON
                                                    : Constants.GEOMETRY_TYPES.MULTIPOLYGON;    break;
            case JGeometry.GTYPE_CURVE        :
            case JGeometry.GTYPE_MULTICURVE   : geomType = gtype==JGeometry.GTYPE_CURVE
                                                    ? Constants.GEOMETRY_TYPES.LINE
                                                    : Constants.GEOMETRY_TYPES.MULTILINE;       break;
            case JGeometry.GTYPE_COLLECTION   : geomType = Constants.GEOMETRY_TYPES.COLLECTION; break;
            case -1                           :
            default                           : retLabel.setIcon(this.iconQuestionMark);
                                                return retLabel;
        }
        icon = PointMarker.getGeometryTypeIcon(geomType, _imageSize.width, _imageSize.height, Color.WHITE, Color.BLUE, null);
        if ( icon == null ) {
            retLabel.setText(this.renderGeoObject(_gStruct,true));
        } else { 
            retLabel.setIcon(icon);
        }
        return retLabel;
    }
    
    /**
     * @method  getWorldToScreenTransform(double,double,double,dluble,Dimension)
     * @author Simon Greener, April 2010, Added to support java.awt.shape rendering.
     * 
     */
    public static AffineTransform getWorldToScreenTransform(Envelope _mbr,
                                                            Dimension       _screenSize) 
    {
            double scaleX = _screenSize.getWidth()  / (_mbr.maxX - _mbr.minX);
            double scaleY = _screenSize.getHeight() / (_mbr.maxY - _mbr.minY);
            double tx = - (_mbr.minX * scaleX);
            double ty =   (_mbr.minY * scaleY) + _screenSize.getHeight();
            // LOGGER.info("getWorldToScreenTransform(" + scaleX + ", 0.0d, 0.0d, " + -scaleY + "," + tx + "," + ty+")");
            return new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
    }
  
    /**
     * Processes Object which could be SDO_GEOMETRY, VERTEX_TYPE, SDO_ELEM_INFO_ARRAY, POINT_TYPE etc 
     * and returns representational (normally coloured) string.
     * @param _value
     * @param _allowColouring
     * @return
     * @history Simon Greener, 2010
     */
    public String renderGeoObject(Object  _value,
                                  boolean _allowColouring) 
    {
        String    clipText = "",
               sqlTypeName = "";
        try
        {
            // is this really geometry / vertex type column?
              boolean colourSDOGeomElems = _allowColouring ? this.GeoRaptorPrefs.isColourSdoGeomElements() : false;
              if ( _value instanceof oracle.sql.STRUCT ) {
                STRUCT stValue = (STRUCT)_value;
                sqlTypeName = stValue.getSQLTypeName();
                if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                    clipText =  this.renderSdoGeometry(stValue,colourSDOGeomElems);
                } else if (sqlTypeName.equals(Constants.TAG_MDSYS_SDO_GEOMETRY) ) {
                    clipText = this.renderSdoGeometry(stValue,colourSDOGeomElems);
                } else if ( sqlTypeName.equals(Constants.TAG_MDSYS_VERTEX_TYPE) ) {
                    clipText = RenderTool.renderVertexType(stValue,colourSDOGeomElems);
                } else if ( sqlTypeName.equals(Constants.TAG_MDSYS_SDO_POINT_TYPE) ) {
                    clipText = RenderTool.renderSdoPoint(SDO_GEOMETRY.asDoubleArray(stValue,Double.NaN),
                                                         colourSDOGeomElems,
                                                         colourSDOGeomElems,
                                                         Constants.MAX_PRECISION);
                }
            } else if (_value instanceof oracle.sql.ARRAY) {
                ARRAY aryValue = (ARRAY)_value;
                sqlTypeName =  aryValue.getSQLTypeName();
                if (sqlTypeName.equals(Constants.TAG_MDSYS_SDO_DIMARRAY)) {
                    clipText = RenderTool.renderDimArray(aryValue,
                                                         _allowColouring 
                                                         ? this.GeoRaptorPrefs.isColourDimInfo() 
                                                         : false);
                } else if (sqlTypeName.equals(Constants.TAG_MDSYS_SDO_ELEM_ARRAY)) {
                    clipText = RenderTool.renderElemInfoArray(SDO_GEOMETRY.asIntArray(aryValue,Integer.MIN_VALUE),
                                                              colourSDOGeomElems,
                                                              colourSDOGeomElems, /* wrap with HTML */
                                                              this.GeoRaptorPrefs.getSdoGeometryBracketType());
                } else if (sqlTypeName.equals(Constants.TAG_MDSYS_SDO_ORD_ARRAY)) {
                    clipText = RenderTool.renderSdoOrdinates(SDO_GEOMETRY.asDoubleArray(aryValue,Double.NaN),
                                                             colourSDOGeomElems,
                                                             colourSDOGeomElems, /* wrap with HTML */
                                                             0,  /* We don't know its dimensionality to let function know to simply render the ordinates as is */
                                                             null,  /* No elemOrds to indicate start of each element */
                                                             this.GeoRaptorPrefs.getSdoGeometryBracketType(),
                                                             this.GeoRaptorPrefs.isSdoGeometryCoordinateNumbering(),
                                                             Constants.MAX_PRECISION);
                }
            } else {
                clipText = Constants.NULL;
            }
        } catch (Exception _e) {
            clipText = sqlTypeName + " rendering Failed (" + _e.getMessage() + ")";
        }
        return clipText;
   }
    
    /**
     * Renders SDO_GEOMETRY object in to appropriate string.
     * @param _colValue
     * @param _allowColouring
     * @return
     * @method @method
     * @history @history
     */
    public String renderSdoGeometry(STRUCT  _colValue, 
                                    boolean _allowColouring) 
    {
        String clipText = "";
        if (_colValue==null) {
            return "NULL";
        }
        String sqlTypeName;
        try { sqlTypeName = _colValue.getSQLTypeName(); } catch (SQLException e) {LOGGER.error("renderSdoGeometry: Failed to get sqlTypeName of Struct:\n"+e.toString()); return ""; }
        STRUCT stValue = _colValue;
        
        boolean colourSDOGeomElems                  = _allowColouring ? this.GeoRaptorPrefs.isColourSdoGeomElements() : false;

        Constants.geometrySourceType geomSourceType = this.GeoRaptorPrefs.isSdoGeometryProcessingFormat()
                                                      ? Constants.geometrySourceType.SDO_GEOMETRY
                                                      : Constants.geometrySourceType.JGEOMETRY;
        JGeometry jGeo = null;  // Test to see if it will work
        if (  geomSourceType == Constants.geometrySourceType.JGEOMETRY ) {
            try {
                if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                   jGeo = JGeometry.load(SDO_GEOMETRY.getSdoFromST(_colValue));
                } else {
                   jGeo = JGeometry.load(stValue);
                }
            } catch (SQLException e) {
                // Since we want to render even bad geometries, switch to STRUCT based processing.
                geomSourceType = Constants.geometrySourceType.SDO_GEOMETRY; 
            }
        }

        Constants.renderType visualFormat = this.GeoRaptorPrefs.getVisualFormat();
        // If ICON or THUMBNAIL drop back to SDO_GEOMETRY text
        //
        if (visualFormat == Constants.renderType.ICON ||
            visualFormat == Constants.renderType.THUMBNAIL) {
            visualFormat = Constants.renderType.SDO_GEOMETRY;
        }

        // If visualisation is not SDO_GEOMETRY we need a connection
        // to use sdoutl.jar conversion routines so grab first available
        //
        if ( this.dbConnection == null && ( visualFormat != Constants.renderType.SDO_GEOMETRY ) )
        {
            this.dbConnection = DatabaseConnections.getInstance().getActiveConnection();
            if ( dbConnection == null ) {
                visualFormat = Constants.renderType.SDO_GEOMETRY;
            }
        }

        try 
        {
            if ( visualFormat != Constants.renderType.SDO_GEOMETRY ) {
                // If sdo_geometry object is 3D then certain renders cannot occur
                if (SDO_GEOMETRY.getDimension(stValue,2) >= 3 ) {
                    if ( visualFormat == Constants.renderType.WKT ||
                         visualFormat == Constants.renderType.KML )
                         visualFormat = Constants.renderType.SDO_GEOMETRY;
                }
            }
            
            switch ( visualFormat )
            {
            case SDO_GEOMETRY:
                if ( colourSDOGeomElems  ) {
                    clipText = ( geomSourceType == Constants.geometrySourceType.SDO_GEOMETRY )
                               ? RenderTool.renderSTRUCTAsHTML(stValue,
                                                               this.GeoRaptorPrefs.getSdoGeometryBracketType(),
                                                               this.GeoRaptorPrefs.isSdoGeometryCoordinateNumbering(),
                                                               Constants.MAX_PRECISION)
                               : RenderTool.renderGeometryAsHTML(jGeo, 
                                                                 sqlTypeName, 
                                                                 this.GeoRaptorPrefs.getSdoGeometryBracketType(),
                                                                 this.GeoRaptorPrefs.isSdoGeometryCoordinateNumbering(),
                                                                 Constants.MAX_PRECISION);
                  } else {
                    clipText = ( geomSourceType == Constants.geometrySourceType.SDO_GEOMETRY )
                             ? RenderTool.renderSTRUCTAsPlainText(stValue,this.GeoRaptorPrefs.getSdoGeometryBracketType(),Constants.MAX_PRECISION)
                             : RenderTool.renderGeometryAsPlainText(jGeo, sqlTypeName, this.GeoRaptorPrefs.getSdoGeometryBracketType(),Constants.MAX_PRECISION);
                  }
                  break;
            default :
                if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                   stValue = SDO_GEOMETRY.getSdoFromST(_colValue);
                }
                if ( visualFormat == Constants.renderType.WKT || SDO_GEOMETRY.hasArc(stValue) ) {
                    visualFormat = Constants.renderType.WKT;
                    WKT w = new WKT();
                    clipText = new String(w.fromSTRUCT(stValue));
                } else {
                    switch (visualFormat) {
                    case KML2 : KML2.setConnection(this.dbConnection);
                                clipText = KML2.to_KMLGeometry(stValue);
                                break;
                    case GML2 : GML2.setConnection(this.dbConnection);
                                clipText = GML2.to_GMLGeometry(stValue);
                                break;
                    case GML3 : GML3.setConnection(this.dbConnection);
                                clipText = GML3.to_GML3Geometry(stValue);
                                break;
					   default: break;
                    }
                }
            }
        } catch (Exception _e) {
          LOGGER.error("SpatailRenderer.renderGeometry(): Caught exception when rendering geometry as " + visualFormat.toString() + " (" + _e.getMessage() + ")");
        }
        if ( visualFormat != Constants.renderType.SDO_GEOMETRY && Strings.isEmpty(clipText) ) 
        { 
              try 
              {
                  if ( colourSDOGeomElems ) 
                  { 
                      clipText = RenderTool.renderSTRUCTAsHTML(stValue,
                                                               GeoRaptorPrefs.getSdoGeometryBracketType(),
                                                               GeoRaptorPrefs.isSdoGeometryCoordinateNumbering(),
                                                               Constants.MAX_PRECISION);
                  } else {
                      clipText = RenderTool.renderSTRUCTAsPlainText(stValue,
                                                                    this.GeoRaptorPrefs.getSdoGeometryBracketType(),
                                                                    Constants.MAX_PRECISION);
                  }
              } catch (SQLException e) {
                    return null;
              }
        }
        return clipText;
    }

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		// TODO Auto-generated method stub
		return null;
	}
}
