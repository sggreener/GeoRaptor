package org.GeoRaptor.SpatialView.layers;


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.oracle.OraReader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

import java.io.IOException;

import java.sql.SQLException;

import java.text.AttributedString;
import java.text.DecimalFormat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

import oracle.jdbc.OracleConnection;

import oracle.spatial.geometry.ElementExtractor;
import oracle.spatial.geometry.J3D_Geometry;
import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.Messages;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.SupportClasses.LineStyle;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.Coordinate;
import org.GeoRaptor.tools.MathUtils;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;

@SuppressWarnings("deprecation")
public class SVSpatialLayerDraw {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVSpatialLayerDraw");

    /**
     * Reference to main class
     */
    protected SVSpatialLayer layer;
    private OracleConnection layerConnection = null;
    
    private Graphics2D graphics2D;

    private BasicStroke markStroke = null;

    public static final int    ALIGN_TOP = 1;
    public static final int ALIGN_MIDDLE = 0;
    public static final int ALIGN_BOTTOM = -1;
    public static final int   ALIGN_LEFT = -1;
    public static final int ALIGN_CENTRE = 0;
    public static final int  ALIGN_RIGHT = 1;

    private int horizPosn = ALIGN_LEFT;
    private int vertPosn = ALIGN_BOTTOM;

    public SVSpatialLayerDraw(SVSpatialLayer _sLayer) {
        this.layer = _sLayer;
        this.markStroke = new BasicStroke(this.layer.styling.getLineWidth() + 1,
                                          BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND);
    }

    /**
     *
     * @param _geo JGeometry
     * @param _label String
     * @param _shadeValue String
     * @param _pointColorValue String
     * @param _pointSizeValue int
     * @param _rotate Constants.ROTATE
     * @param _angle double
     * @param _attributes AttributeSet
     * @param _labelPosition Constants.TEXT_OFFSET
     * @throws IOException
     * @method @method
     * @history @history
     * @author Simon Greener, April 2010, Heavily rewrote to base JGeometry rendering on java.awt.Shape
     * @author Simon Greener, May 27th 2010, Created inner try..catch to isolate sdoapi.jar createShape conversion errors.
     * @author Simon Greener, Nov 2010, Added _label parameter for use with oriented points/multipoints
     */
    public void drawGeometry(JGeometry             _geo,
                             String                _label,
                             String                _shadeValue,
                             String                _pointColorValue,
                             String                _lineColorValue,
                             int                   _pointSizeValue,
                             Constants.ROTATE      _rotate,
                             double                _angle,
                             AttributeSet          _attributes,
                             Constants.TEXT_OFFSET_POSITION _labelPosition,
                             int                   _labelOffset)
    throws IOException
    {
        if ( this.graphics2D==null ) {
            throw new IOException("SVSpatialLayerDraw: No Graphics Context for drawing");
        }
        if (_geo == null) {
            return;
        }
        if (this.getWorldToScreenTransform()==null) {
            throw new IOException("SVSpatialLayerDraw: WorldToScreenTransform is null.");
        }
        // DEBUG LOGGER.debug(String.format("labelValue=%s shadeValue=%s pointColorValue=%s lineColorValue=%s pointSizeValue=%d rotate=%s angle=%f labelPosition=%s labelOffset=%d", _label,_shadeValue,_pointColorValue,_lineColorValue,_pointSizeValue, _rotate.toString(), _angle, _labelPosition.toString(), _labelOffset));
        Shape shp = null;        
        // Cache the connection between calls for one layer
        if (this.layerConnection == null ) {
            this.layerConnection = this.layer.getConnection();
        } 
        this.setLabelPosition(_labelPosition);
        int gtype = _geo.getType();
        switch (gtype) {
        case JGeometry.GTYPE_COLLECTION:
            // Need to extract individual elements that can be mapped to Shapes
            JGeometry[] _geoList = _geo.getElements();
            for (JGeometry _g : _geoList) {
                if ( _g != null ) {
                   drawGeometry(_g, _label, _shadeValue, _pointColorValue, _lineColorValue, _pointSizeValue, _rotate, _angle, _attributes, _labelPosition, _labelOffset);
                }
            }
            break;
        case JGeometry.GTYPE_POINT:
            // Render the point
            try {
                drawPoint(_geo, _label, _pointColorValue, _pointSizeValue, _rotate, _angle, _attributes, _labelOffset);
            } catch (Exception e) {
                LOGGER.warn("SVSpatialLayerDraw drawPoint() failed: " + e.toString() );
            }
            if (this.layer.styling.isMarkVertex() )
            {
                markPoint(_geo);
            }
            break;
        case JGeometry.GTYPE_MULTIPOINT:
            // Render the points of the multipoint
            drawMultiPoint(_geo, _label, _pointColorValue, _pointSizeValue, _rotate, _angle, _attributes, false);
            if (this.layer.styling.isMarkGeoStart()  ||
                this.layer.styling.isMarkGeoPoints() ||
                this.layer.styling.isMarkVertex())
            {
                markElement(_geo,false);
            }
            break;
        case JGeometry.GTYPE_MULTICURVE:
        case JGeometry.GTYPE_CURVE:
        case JGeometry.GTYPE_POLYGON:
        case JGeometry.GTYPE_MULTIPOLYGON:
            // Convert JGeometry to Java2D Shape.
            // Also, translate to viewport
            // This also converts 3D/4D down to 2D and
            // converts circular arcs to stroked linestrings.
            //
            // Separate try..catch to trap sdoapi.jar file errors

                // Creates a transformed shape from the geometry using the given affine transform.
                // Oracle could do (does?) some smart resampling of the shape based on the transformed coordinates.
                //
                shp = _geo.createShape(getWorldToScreenTransform(),true/*Simplify for faster performance?*/);
                if (shp == null) {
                    LOGGER.warn("SVSpatialLayerDraw.drawGeometry - Geometry 2 Shape conversion error.");
                    return;
                }
                
                // Fill and then draw polygons or just draw lines
                //
                switch (gtype) {
                  case JGeometry.GTYPE_POLYGON      :
                  case JGeometry.GTYPE_MULTIPOLYGON : fillShape(shp, _shadeValue);
                  case JGeometry.GTYPE_CURVE        :
                  case JGeometry.GTYPE_MULTICURVE   : drawShape(shp, _lineColorValue);
                }
                // Now mark them
                //
                if (this.layer.styling.isMarkGeoStart()  ||
                    this.layer.styling.isMarkGeoPoints() ||
                    this.layer.styling.isSegmentArrows() ||
                    this.layer.styling.isMarkVertex()    ||
                    this.layer.styling.isMarkSegment()  )
                {
                    // DEBUG 
                    markGeometry(_geo);
                }

                // Now draw label if required
                //
                if ( !Strings.isEmpty(_label) ) {
                    if (layer.getStyling().getGeometryLabelPosition()==Constants.GEOMETRY_LABEL_POSITION.LABEL_ALONG ) {
                        labelAlongLine(shp,_geo.getNumPoints(),_label,_attributes,/*_repeattrue*/   false);
                        //textAlongLine(shp,_label,_attributes);
                    } else {
                        // DEBUG LOGGER.info("labelLineAndPolygonSegments being called");
                        labelLineAndPolygonSegments(_geo,_label,_pointColorValue, _pointSizeValue, _rotate, _angle, _attributes, _labelOffset);
                    }
                }
            break;
        case 8:  // SOLID
        case 9:  // MultiSolid
            draw3DGeometry(new J3D_Geometry( _geo.getType(),
                                             _geo.getSRID(),
                                             _geo.getElemInfo(),
                                             _geo.getOrdinatesArray()),
                           _label, 
                           _shadeValue, _pointColorValue, _lineColorValue, _pointSizeValue, 
                           _rotate, _angle,
                           _attributes, 
                           _labelPosition, 
                           _labelOffset);
            break;
        default:
            LOGGER.warn("Unsupported JGeometry gtype: " + _geo.getType());
        }
        return;
    }

    private void labelAlongLine(Shape        _shp,
                                int          _numPoints,
                                String       _label,
                                AttributeSet _attributes,
                                boolean      _repeat ) 

    {
        Color foregroundColour;
        Color backgroundColour;
        foregroundColour = StyleConstants.getForeground(_attributes);
        // LOGGER.info ("foregroundColour ="+(foregroundColour==null?"NULL":foregroundColour.toString()));
        backgroundColour = StyleConstants.getBackground(_attributes);
        // LOGGER.info ("backgroundColour ="+(backgroundColour==null?"NULL":backgroundColour.toString()));
        
        // LOGGER.info("Name: " + _label);
        TextStroke ts = new TextStroke(_label, 
                                       _attributes,
                                       _repeat);
        ts.setGraphics(this.graphics2D);
        ts.setTotalPoints(_numPoints);
        Shape strokedShape = ts.createStrokedShape(_shp);
        // First render background fill from TextStroke line shape
        Shape bShape = ts.getLineSegment();
        if (bShape != null ) {
            // printShapeCoordinates(bShape);
            float bWidth = ts.getGlyphHeight() * 1.2f; // Add 20% to 
            //LOGGER.info ("bWidth="+bWidth);
            this.graphics2D.setColor(backgroundColour);  // Background is foreground for bShape
            this.graphics2D.setStroke(new BasicStroke(bWidth,
                                                      BasicStroke.CAP_ROUND,
                                                      BasicStroke.JOIN_ROUND,
                                                      10.0f));
            //this.graphics2D.setStroke(LineStyle.getStroke(LineStyle.LINE_STROKES.LINE_SOLID,(int)bWidth));
            this.graphics2D.setComposite(this.layer.styling.getLineAlphaComposite());
            this.graphics2D.draw(bShape);
        }

        // Then render text
        if ( foregroundColour != null ) {
            this.graphics2D.setColor(foregroundColour); // Not used as gShape is rendered with Colour
        }
        this.graphics2D.fill(strokedShape);
    }

     public static void printShapeCoordinates(Shape _shp) 
     {
         LOGGER.info("printShapeCoordinates(_shp)");
         PathIterator pi = _shp.getPathIterator(null);
         double[] coordinates = new double[6];
         int type = 0;
         while (pi.isDone() == false) {
             type = pi.currentSegment(coordinates);
             switch (type) {
             case PathIterator.SEG_MOVETO:
               LOGGER.info("move to " + coordinates[0] + ", " + coordinates[1]);
               break;
             case PathIterator.SEG_LINETO:
               LOGGER.info("line to " + coordinates[0] + ", " + coordinates[1]);
               break;
             case PathIterator.SEG_QUADTO:
               LOGGER.info("quadratic to " + coordinates[0] + ", " + coordinates[1] + ", "
                   + coordinates[2] + ", " + coordinates[3]);
               break;
             case PathIterator.SEG_CUBICTO:
               LOGGER.info("cubic to " + coordinates[0] + ", " + coordinates[1] + ", "
                   + coordinates[2] + ", " + coordinates[3] + ", " + coordinates[4] + ", " + coordinates[5]);
               break;
             case PathIterator.SEG_CLOSE:
               LOGGER.info("close");
               break;
             default:
               break;
             }
             pi.next();
         }
     }

    @SuppressWarnings("incomplete-switch")
	private void labelLineAndPolygonSegments(JGeometry        _jGeom,
                                             String           _label,
                                             String           _pointColorValue,
                                             int              _pointSizeValue,
                                             Constants.ROTATE _rotate,
                                             double           _angle,
                                             AttributeSet     _attributes,
                                             int              _labelOffset)

    {
        try {
            // Get appropriate label position from converted shape
            int     firstLabelOrd = 0;
            float       numCoords = 0.0f;
            double        point[] = null;
            Point2D.Double cPoint = null;
            Point2D    labelPoint = null;
            
            // Process all parts of geometry ie linestring from multilinestring, polygon from multipolygon
            JGeometry element = null;
            int elementCount = _jGeom.getElements().length;
            // DEBUG LOGGER.info("labelLineAndPolygonSegments: Number of parts: " + elementCount + ". GeometryType: " + _jGeom.getType());
            for (int i=1; i<=elementCount; i++) {
                element = _jGeom.getElementAt(i);
                @SuppressWarnings("unused")
				boolean hasCircularArcs = element.hasCircularArcs();
                // DEBUG LOGGER.info("labelLineAndPolygonSegments: hasCircularArcs(): " + hasCircularArcs);
                // DEBUG LOGGER.info("labelLineAndPolygonSegments: element type is: " + element.getType());
                switch (this.layer.styling.getGeometryLabelPosition()) {
                    case SDO_POINT:
                        // DEBUG LOGGER.info("labelLineAndPolygonSegments: SDO_POINT");
                        cPoint = element.getLabelPoint()==null
                                 ?null
                                 :new Point.Double(element.getLabelPoint().getX(),
                                                   element.getLabelPoint().getY());
                        break;
                    case FIRST_VERTEX: 
                        // DEBUG LOGGER.info("labelLineAndPolygonSegments: FIRST_VERTEX");
                        point = element.getFirstPoint();
                        cPoint = new Point.Double(point[firstLabelOrd],point[firstLabelOrd+1]);
                        break;
                    case MIDDLE_VERTEX: 
                        // DEBUG LOGGER.info("labelLineAndPolygonSegments: MIDDLE_VERTEX");
                        point = element.getOrdinatesArray();
                        numCoords = point.length / element.getDimensions();
                        // DEBUG LOGGER.info("labelLineAndPolygonSegments: MIDDLE_VERTEX numCoords=" + numCoords);
                        if ( numCoords == 2 ) {
                            cPoint = new Point.Double(point[0]+((point[element.getDimensions()]-point[0])/2.0),
                                                      point[1]+((point[1+element.getDimensions()]-point[1])/2.0));
                        } else {
                            firstLabelOrd = (int)(Math.round(numCoords / 2.0) - 1) * element.getDimensions(); 
                            cPoint = new Point.Double(point[firstLabelOrd],point[firstLabelOrd+1]);
                        }
                        break;
                    case END_VERTEX:
                        // DEBUG LOGGER.info("labelLineAndPolygonSegments: END_VERTEX");
                        point = element.getOrdinatesArray();
                        firstLabelOrd = point.length - element.getDimensions();
                        cPoint = new Point.Double(point[firstLabelOrd],point[firstLabelOrd+1]);
                        break;
                    case JTS_CENTROID:
                        // DEBUG LOGGER.info("labelLineAndPolygonSegments: JTS_CENTROID");
                        cPoint = ST_Centroid(element);
                        break;
                }
                // DEBUG LOGGER.info("labelLineAndPolygonSegments: cPoint=" + cPoint==null?"NULL":(cPoint.getX() + "," + cPoint.getY()));
                if (cPoint == null) { continue; }
                labelPoint = this.getWorldToScreenTransform().transform(cPoint,null);
                // DEBUG LOGGER.info("labelLineAndPolygonSegments: labelPoint=" + labelPoint==null?"NULL":(labelPoint.getX() + "," + labelPoint.getY()));
                if ( labelPoint == null ) { continue; }
                // Draw marker for label?
                // DEBUG LOGGER.info( this.layer.styling.getPointType().toString());
                if ( this.layer.styling.getPointType()!=PointMarker.MARKER_TYPES.NONE ) {
                    // Don't draw selected colour if polygon
                    this.graphics2D.setStroke(this.layer.styling.getLineStroke());
                    // Check Scale
                    //
                    if ( this.isWithinScale() ) {
                        drawPoint(labelPoint, this.layer.styling.getPointType(), _pointColorValue, _pointSizeValue, _rotate, _angle);
                    }
                }
                // Got point so now draw label
                // if within Scale
                //
                if ( this.isWithinScale() ) {
                    drawString(this.graphics2D,
                               _label,
                               (int)labelPoint.getX(), 
                               (int)labelPoint.getY(),
                               this.horizPosn,
                               this.vertPosn,
                               _rotate,  
                               _angle, 
                               _attributes, 
                               _labelOffset);
                }
            }
        } catch (Exception _e) {
            LOGGER.warn("SVSpatialLayerDraw.labelLineAndPolygonSegments " + (_e.getLocalizedMessage()==null?"":_e.getLocalizedMessage()));
        }

    }
    
	public Point2D.Double ST_Centroid(JGeometry _geom)
    {
        STRUCT stGeom = null;
        try
        {
            stGeom = (STRUCT) JGeometry.storeJS(this.layerConnection,_geom);
            if ( stGeom == null ) {
                return null;
            }
            
            // Check geometry parameters
            //
            if ( _geom == null )
                throw new SQLException("Supplied Sdo_Geometry is NULL.");
    
            // Extract SRID from SDO_GEOEMTRY
            //
            int SRID = _geom.getSRID();
            
            // Convert Geometries
            //
            double precisionModelScale = this.layer.getPrecision(false) < 0 
                                  ? (double)(1.0/Math.pow(10, this.layer.getPrecision(false))) 
                                  : (double)Math.pow(10, this.layer.getPrecision(false));

            PrecisionModel   pm = new PrecisionModel(precisionModelScale);
            GeometryFactory  gf = new GeometryFactory(pm,SRID); 
            OraReader converter = new OraReader(gf);
            Geometry        geo = converter.read(stGeom);
    
            // Check converted geometries are valid
            //
            if ( geo == null ) {
               LOGGER.warn("SDO_Geometry conversion to JTS geometry returned NULL.");
               return null;
            }
            
            // Now do the calculation
            //
            com.vividsolutions.jts.geom.Point p = geo.getInteriorPoint();
            if ( p == null ) {
                LOGGER.warn("Could not compute centroid.");
                return null;
            }
            return new Point2D.Double(p.getX(),p.getY()); 
          } catch(Exception e) {
              LOGGER.warn("Error generating JTS Centroid: " + e.getMessage());
              return null;
          }
     }


    public Graphics2D getGraphics2D() {
        return this.graphics2D;
    }

    public void setGraphics2D(Graphics2D graphics2D) {
        this.graphics2D = graphics2D;
    }

    /**
     * Override function
     */
    public Point2D ScreenToWorld(Point.Double _coord) 
    throws NoninvertibleTransformException 
    {
        return this.layer.getSpatialView().getMapPanel().ScreenToWorld(_coord);
    }

    protected AffineTransform getWorldToScreenTransform() {
        return this.layer.getSpatialView().getMapPanel().getWorldToScreenTransform();
    }

    /**
     * @method draw3DGeometry
     * @precis Extacts 2D elements of a 3D geometry for rendering in 2D space.
     * @param _geo             J3D_Geometry            - Solid/Surface etc
     * @param _label           String                  - Text for rendering as label
     * @param _shadeValue      String                  - shade value from table column
     * @param _pointColorValue String                  - Value from database as string (r,g,b,a)
     * @param _pointSizeValue  int                     - Point marker size 4 - 16 pts
     * @param _rotate          Constants.ROTATE        - Rotate Label, marker or both?
     * @param _angle           double                  - Value for rotating label or marker
     * @param _attributes      AttributeSet            - Text attributes for rendering label
     * @param _labelPosition   Constants.TEXT_OFFSET - Where to place Label in terms of TL, TC, TR, CL, CC, CR, BL, BC, BR
     * @author Simon Greener, December 13th 2010, Original coding
     */
    private void draw3DGeometry(J3D_Geometry     _geo,
                                String           _label,
                                String           _shadeValue,
                                String           _pointColorValue,
                                String           _lineColorValue,
                                int              _pointSizeValue,
                                Constants.ROTATE _rotate,
                                double           _angle,
                                AttributeSet     _attributes,
                                Constants.TEXT_OFFSET_POSITION _labelPosition,
                                int                     _labelOffset)
    {
        J3D_Geometry eGeo = null;
        ElementExtractor ee;
        try {
            ee = new ElementExtractor(_geo,
                                      0, // Element Start
                                      _geo.getType() == J3D_Geometry.GTYPE_MULTISOLID
                                          ? ElementExtractor.MULTICOMP_TOSIMPLE
                                          : ElementExtractor.LOWER_LEVEL,
                                      false );
            if ( ee != null ) {
                int[] inner_outer = {1};  // 1 = outer
                while ( (eGeo = ee.nextElement(inner_outer)) != null ) {
                    if ( eGeo.getType() == J3D_Geometry.GTYPE_SOLID )
                        draw3DGeometry(eGeo, _label, _shadeValue, _pointColorValue, _lineColorValue, _pointSizeValue, _rotate, _angle, _attributes, _labelPosition, _labelOffset);
                    else
                        drawGeometry(eGeo, _label, _shadeValue, _pointColorValue, _lineColorValue, _pointSizeValue, _rotate, _angle, _attributes, _labelPosition, _labelOffset);
                }
            }
        } catch (Exception e) {
            // Error is normally because the geometry is invalid in some way eg ORA-54530
            // RenderTool rt = new RenderTool();
            // System.out.println("process3DGeometry: "+ e.getMessage() + "\n" + rt.renderGeometryAsPlainText(_geo, Constants.bracketType.NONE));
        }

    }

    /**
     * @precis Does the actual drawing for POINT and MULTIPOINT geometries.
     * @param _point2D         Point2D                 - transformed point
     * @param _pointType       PointMarker.MARKER_TYPE - type of point eg CIRCLE, ASTEROID, SQUARE
     * @param _pointColorValue String                  - Value from database as string (r,g,b,a)
     * @param _pointSizeValue  int                     - Point marker size 4 - 16 pts
     * @param _rotate          Constants.ROTATE        - type of rotatation of marker symbol 
     * @param _angle           double                  - rotate marker unless 0
     * @throws IOException
     * @author Simon Greener May 28th 2010 - Original Coding
     *          Modified to draw symbol based on PointMarker.MARKER_TYPES (though most better done via drawLabel and a font char
     **/
    private void drawPoint(Point2D                  _point2D,
                           PointMarker.MARKER_TYPES _pointType,
                           String                   _pointColorValue,
                           int                      _pointSizeValue,
                           Constants.ROTATE         _rotate,
                           double                   _angle)
    {
        Shape drawShape = PointMarker.getPointShape(_pointType,
                                                    _point2D,
                                                    this.layer.styling.getPointSize(_pointSizeValue),
                                                    (_rotate == Constants.ROTATE.MARKER ||
                                                     _rotate == Constants.ROTATE.BOTH)
                                                    ? _angle : 0.0f);
        this.graphics2D.setColor(Strings.isEmpty(_pointColorValue)
                                 ? this.layer.styling.getPointColor(Colours.colorToString(Color.BLACK))
                                 : Colours.fromRGBa(_pointColorValue));
        this.graphics2D.fill(drawShape);
        this.graphics2D.draw(drawShape);
    }

    private void drawPoint(Shape _drawShape)
    {
        this.graphics2D.fill(_drawShape);
        this.graphics2D.draw(_drawShape);
    }

    /**
     * @precis Function that transforms a point before calling actaul drawing method
     *         does the actual drawing for POINT and MULTIPOINT geometries.
     * @param _X
     * @param _Y
     * @param _mt type of point marker
     * @param _rotate boolean rotate marker symbol using _angle or not
     * @param _angle (rotate marker unless 0)
     * @note  Calling functions have to have set styling colours etc
     * @throws IOException
     * @author Simon Greener May 28th 2010 - Original Coding
     */
    public void drawPoint(double                   _X, double _Y,
                          PointMarker.MARKER_TYPES _mt,
                          String                   _pointColorValue,
                          int                      _pointSizeValue,
                          Constants.ROTATE         _rotate,
                          double                   _angle)
    {
        // Everything should use the standard transform attached to the graphics2D
        // Should not keep getting transform every time...
        //
        Point2D point2D = this.getWorldToScreenTransform().transform(new Point.Double(_X, _Y), null);
        drawPoint(point2D, _mt, _pointColorValue, _pointSizeValue, _rotate, _angle);
    }

    /**
     * Draw Point
     * @param _point Oracle Spatial polygon
     * @param _label text to draw
     * @param _rotate boolean rotate marker symbol using _angle or not
     * @param _angle (rotate marker unless 0)
     * @author Simon Greener, May 28th 2010
     *          Heavily modified to support oriented points
     */
    protected void drawPoint(JGeometry        _point,
                             String           _label,
                             String           _pointColorValue,
                             int              _pointSizeValue,
                             Constants.ROTATE _rotate,
                             double           _angle,
                             AttributeSet     _attributes,
                             int              _offset)
    throws Exception
    {
        // Define variable to hold possible label point
        //
        Point2D labelPoint = new Point.Double(Double.NaN, Double.NaN);
        // get point coordinates
        //
        int dims = _point.getDimensions();

        double leaderRotation = _angle;

        // Now draw point or orientedPoint
        //
        if (_point.isOrientedPoint() == false) {
            double point[] = _point.getFirstPoint();
            if ((point == null) || (point.length != dims))
                return;

            // Call function that does actual drawing of point
            //
            labelPoint = this.getWorldToScreenTransform().transform(new Point.Double(point[0],
                                                                                     point[1]),
                                                                    null);
            drawPoint(labelPoint, this.layer.styling.getPointType(), _pointColorValue, _pointSizeValue, _rotate, _angle);

        } else {
            // getLabelPoint/getLabelPointXYZ do not work.
            // Need to access ordinatesArray directly
            //
            double orientedPoint[] = _point.getOrdinatesArray();
            if ((orientedPoint == null) ||
                (orientedPoint.length != (2 * dims)))
                return;

            Point2D point = null;

            // Compute position of ordinary point
            //
            Point2D worldPoint = new Point.Double(orientedPoint[0], orientedPoint[1]);

            point = this.getWorldToScreenTransform().transform(worldPoint, null);

            // Compute position of oriented point
            //
            Point2D worldLabelPoint = new Point.Double(orientedPoint[0] + orientedPoint[0 + dims],
                                                       orientedPoint[1] + orientedPoint[1 + dims]);

            labelPoint = this.getWorldToScreenTransform().transform(worldLabelPoint, null);

            // Draw oriented point
            //
            drawOrientedPoint(point, labelPoint, _pointColorValue, _pointSizeValue, _rotate, _angle);

            // Compute rotation for drawString
            //
            leaderRotation = Math.atan2(labelPoint.getY() - point.getY(), labelPoint.getX() - point.getX()) + (Math.PI / 2.0f);
        }
        // Now draw label if required
        if (!Strings.isEmpty(_label)) {
            // Got point so now draw label
            // if within Scale
            //
            if ( this.isWithinScale() ) {
                drawString(this.graphics2D,_label,
                           (int)labelPoint.getX(), (int)labelPoint.getY(),
                           this.horizPosn, this.vertPosn,
                           _rotate, leaderRotation, _attributes, _offset);
            }
        }
    }

    private void drawOrientedPoint(Point2D _point,
                                   Point2D _orientedPoint,
                                   String _pointColorValue,
                                   int    _pointSizeValue,
                                   Constants.ROTATE _rotate,
                                   double _rotation)
    {
        drawPoint(_point, this.layer.styling.getPointType(), _pointColorValue, _pointSizeValue, _rotate, _rotation);

        // Draw connecting vector
        //
        if (true) {
            // Save colour of starting point
            Color oldColor = this.graphics2D.getColor();
            this.graphics2D.setColor(Color.GRAY);
            this.graphics2D.setStroke(new BasicStroke(this.layer.styling.getLineWidth(),
                                                      BasicStroke.CAP_BUTT,
                                                      BasicStroke.JOIN_MITER,
                                                      10.0f,
                                                      new float[] { 2f, 2f },
                                                      /* Expressed in pixel space? */
                        0.1f));
            this.graphics2D.drawLine((int)_point.getX(), (int)_point.getY(),
                                     (int)_orientedPoint.getX(),
                                     (int)_orientedPoint.getY());
            // Restore colour of starting point
            this.graphics2D.setColor(oldColor);
        }
        // Now draw oriented point
        //
        drawPoint(_orientedPoint, PointMarker.MARKER_TYPES.CROSS, _pointColorValue, _pointSizeValue, _rotate, 0);
    }

    /**
     * Draw Multi point
     * @param _mpoint Oracle Spatial polygon
     * @throws IOException
     * @author Simon Greener, March 31st 2010
     *          - Fixed 3D/4D rendering.
     * @author Simon Greener, April 4th 2010
     *          - renamed parameter; small performance changes to code
     * @author Simon Greener, May 28th 2010
     *          Heavily modified to support oriented points
     * @author Simon Greener, November 16th 2010
     *          Modified for rotation/labelling
     *          Also, implemented check to see if individual point in multipoint should be drawn or not
     */
    protected void drawMultiPoint(JGeometry        _mpoint,
                                  String           _label,
                                  String           _pointColorValue,
                                  int              _pointSizeValue,
                                  Constants.ROTATE _rotate,
                                  double           _angle,
                                  AttributeSet     _attributes,
                                  boolean          _numberVertices)
    {
        // get point coordinates
        double points[] = _mpoint.getOrdinatesArray();
        int dim = _mpoint.getDimensions();

        // Do I have to do anything if it is a multi oriented point?
        //
        boolean isOriented = _mpoint.isOrientedMultiPoint();

        // Set color once
        //
        this.graphics2D.setColor(this.layer.styling.getPointColor(_pointColorValue));
        PointMarker.MARKER_TYPES pointType = this.layer.styling.getPointType();
        int pointSize = this.layer.styling.getPointSize(_pointSizeValue);
        double angle = (_rotate == Constants.ROTATE.MARKER || _rotate == Constants.ROTATE.BOTH) ? _angle : 0.0f;

        Envelope mapWindow = this.layer.getSpatialView().getMBR();

        // call function for point draw
        int coord = 1;
        Point2D point = new Point2D.Double(Double.NaN, Double.NaN),
               oPoint = null;
        for (int i = 0; i < points.length; i += dim) {
            // is this point inside the current display window?
            point.setLocation(points[i], points[i + 1]);
            if (!mapWindow.contains(point))
                continue;
            if (!isOriented || (coord % dim == 1)) {
                point = this.getWorldToScreenTransform().transform(point, null);
                if ( _numberVertices ) {
                    drawLabel("<" + String.valueOf(1) + ">",
                              point,
                              Constants.ROTATE.LABEL,
                              0.0,
                              _attributes,
                              this.layer.styling.getMarkLabelPosition(),
                              this.layer.styling.getMarkLabelOffset());
                }
                if (!isOriented) {
                    drawPoint(PointMarker.getPointShape(pointType,point,pointSize,angle));
                    // Now draw label if required
                    if (!Strings.isEmpty(_label)) {
                        // Got point so now draw label
                        // if within Scale
                        //
                        if ( this.isWithinScale() ) {
                            drawString(this.graphics2D,_label,
                                       (int)point.getX(), (int)point.getY(),
                                       this.horizPosn,    this.vertPosn,
                                       _rotate,  _angle, _attributes, this.layer.styling.getMarkLabelOffset());
                        }
                        this.graphics2D.setColor(this.layer.styling.getPointColor(_pointColorValue));
                    }
                }
            } else /* oriented */ {
                // Calculate oriented point position
                //
                point = this.getWorldToScreenTransform().transform(
                                 new Point2D.Double(points[i - dim],
                                                    points[i - dim + 1]),
                                 null);
                oPoint = this.getWorldToScreenTransform().transform(
                                 new Point2D.Double(points[i - dim] + points[i],
                                                    points[i - dim  + 1] + points[i + 1]),
                                 null);

                // Draw cross using font
                //
                drawOrientedPoint(point, oPoint, _pointColorValue, _pointSizeValue, _rotate, _angle);

                // Compute rotation for drawString
                //
                double rotationRadians = Math.atan2(oPoint.getY() - point.getY(), oPoint.getX() - point.getX()) + (Math.PI / 2.0f);

                // Now draw actual label if required
                //
                if (!Strings.isEmpty(_label)) {
                    // Got point so now draw label
                    // if within Scale
                    //
                    if ( this.isWithinScale() ) {
                        drawString(this.graphics2D,_label,
                                   (int)oPoint.getX(), (int)oPoint.getY(),
                                   this.horizPosn,     this.vertPosn,
                                   _rotate, rotationRadians, _attributes, this.layer.styling.getLabelOffset());
                    }
                    this.graphics2D.setColor(this.layer.styling.getPointColor(_pointColorValue));
                }
            }
            coord++;
        }
    }

    private void fillShape(Shape  _shp,
                           String _shadeValue)
    {
        // Shade polygon and colour line ...
        if (this.layer.styling.isPerformShade()) {
            this.graphics2D.setColor(this.layer.styling.getShadeColor(_shadeValue));
            this.graphics2D.setComposite(this.layer.styling.getShadeAlphaComposite());
            this.graphics2D.fill(_shp);
        }
    }

    /**
     * @param _shp Java2D AWT shape
     * @author Simon Greener April 3rd 2010, Original Coding
     * @author Simon Greener April 19rd 2010
     *          Fixed code to mark start, points and direction
     **/
    private void drawShape(Shape  _shp,
                           String _lineColorValue)
    {
        if (this.layer.styling.isPerformLine()) {
            this.graphics2D.setColor(this.layer.styling.getLineColor(_lineColorValue));
            this.graphics2D.setStroke(this.layer.styling.getLineStroke());
            this.graphics2D.setComposite(this.layer.styling.getLineAlphaComposite());
            this.graphics2D.draw(_shp);
        }
    }

    public void markGeometry(JGeometry _geo)
    {
        try
        {
            JGeometry element = null;
            int elementCount = _geo.getElements().length,
                       gType = 0;
            // DEBUG LOGGER.info("markGeometry: elementCount=" + elementCount);
            int[] elem_info = null;
            for (int i=1; i<=elementCount; i++) {
                element = _geo.getElementAt(i);
                // DEBUG LOGGER.info("markGeometry: element=" + element==null?"NULL":String.valueOf(element.getNumPoints()));
                if ( element != null )
                {
                    gType = (element.getDimensions() * 1000) + (element.getLRMDimension() * 100) + element.getType();
                    // DEBUG LOGGER.info("markGeometry: gType=" + gType);
                    if ( element.getType() == JGeometry.GTYPE_CURVE ) {
                        if ( element.hasCircularArcs() ) {
                            // DEBUG LOGGER.info("markGeometry: Handle circular arc elements");
                            elem_info = strokeCompoundElements(element.getElemInfo());
                            double[] sdoPoint = element.getLabelPointXYZ();
                            if ( Double.isNaN(sdoPoint[0]) ) {
                                markElement(new JGeometry(gType,element.getSRID(),sdoPoint[0],sdoPoint[1],sdoPoint[2],elem_info,element.getOrdinatesArray()),false);
                            } else {
                                markElement(new JGeometry(gType,element.getSRID(),elem_info,element.getOrdinatesArray()),false);
                            }  
                        } else {
                            markElement(element,false);
                        }
                    } else if (element.getType() == JGeometry.GTYPE_POLYGON && element.getElemInfo().length == 3) {
                        // DEBUG LOGGER.info("markGeometry: GTYPE_POLYGON");
                        JGeometry jgeom = null;
                        jgeom = new JGeometry(gType,element.getSRID(),element.getElemInfo(),element.getOrdinatesArray());
                        if ( jgeom.isRectangle() ) {
                            jgeom = SDO_GEOMETRY.rectangle2Polygon2D(jgeom);
                        }
                        markElement(jgeom,false);
                    } else if (element.getType() == JGeometry.GTYPE_POLYGON ||
                               element.getType() == JGeometry.GTYPE_MULTIPOLYGON )
                    {
                        // DEBUG LOGGER.info("markGeometry: GTYPE_POLYGON || GTYPE_MULTIPOLYGON");
                        // Element can be a polygon with rings
                        //
                        ElementExtractor ee;
                        int[] inner_outer = {1};  // 1 = outer
                        // Note: to use the Extractor the geometry has to be 3D.
                        // This stuffs up ordinate interpretation
                        //
                        if ( ! element.hasCircularArcs() ) {
                            elem_info = element.getElemInfo();
                        } else {
                            elem_info = strokeCompoundElements(element.getElemInfo());
                        }                         
                        ee = new ElementExtractor(new J3D_Geometry(gType,
                                                                   element.getSRID(),
                                                                   elem_info,
                                                                   element.getOrdinatesArray()),
                                                  0 /* Element Start*/,
                                                  ElementExtractor.INNER_OUTER /* Want rings */ );
                        if ( ee != null ) {
                            // Get Outer (1003,1005)
                            //
                            inner_outer[0] = 1;  // 1 = outer
                            JGeometry geom = null;
                            J3D_Geometry ring = ee.nextElement(inner_outer);
                            while ( ring != null ) {
                                geom = new JGeometry(_geo.getDimensions() * 1000 + JGeometry.GTYPE_POLYGON,
                                                     _geo.getSRID(),
                                                     (inner_outer[0]==1) 
                                                     ? new int[] {1,1003,ring.isRectangle()?3:1} 
                                                     : new int[] {1,2003,ring.isRectangle()?3:1},
                                                     ring.getOrdinatesArray()  );
                                
                                // What about isCircle()?
                                //
                                if ( geom.isRectangle() ) {
                                    geom = SDO_GEOMETRY.rectangle2Polygon2D(geom);
                                }
                                markElement(geom,(inner_outer[0]==2));
                                // Get inner rings 2003
                                //
                                inner_outer[0] = 2;
                                ring = ee.nextElement(inner_outer);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("markGeometry element extraction error (" + e.getMessage() + ") - probably invalid geometry");
        }
    }

    private int[] strokeCompoundElements(int[] _elem_info) {
        // DEBUG LOGGER.info("strokeCompoundElements: Re-write the elemInfoArray to remove 4,1005, and 2005 elements to make element pretend to be a series of vertex connected linestrings");
        LinkedHashMap<Integer,Integer> elemInfo = new LinkedHashMap<Integer,Integer>(_elem_info.length/3);
        int elements = 0;
        for (int e=0;e<_elem_info.length;e=e+3) {                                
            if ( _elem_info[e+1]==4 || _elem_info[e+1]==1005 || _elem_info[e+1]==2005 ) {
                elemInfo.put(e,  _elem_info[e]);
                elemInfo.put(e+1,_elem_info[e+1]-2);
                elemInfo.put(e+2,1);
                // Force skipping over the sub-elements of the compound element
                elements = _elem_info[e+2];  
                continue;
            } else if ( elements != 0 ) {
                // We are about to process one less sub-element
                elements--;
            } else {
              elemInfo.put(e+1,_elem_info[e]);   // offset
              elemInfo.put(e+2,_elem_info[e+1]); // etype
              elemInfo.put(e+3,1);               // interpretation
            }
        }
        int[] elem_info = new int[elemInfo.size()];
        Iterator<Integer> iter = elemInfo.values().iterator();
        int k=0;
        while (iter.hasNext() ) {
          elem_info[k] = iter.next(); k++;
        }
        return elem_info;
    }
    
    private String getFormattedString(int        _dim, 
                                      int        _pointNum, 
                                      String     _formatter,
                                      Coordinate _currentPoint) {
        switch (_dim) {
            case 2: return String.format(_formatter,
                                         String.valueOf(_pointNum),
                                         getFormattedNumber(_currentPoint.getX()),
                                         getFormattedNumber(_currentPoint.getY()));
            case 3: return String.format(_formatter,
                                         String.valueOf(_pointNum),
                                         getFormattedNumber(_currentPoint.getX()),
                                         getFormattedNumber(_currentPoint.getY()),
                                         getFormattedNumber(_currentPoint.getZ()));
            case 4: return String.format(_formatter,
                                         String.valueOf(_pointNum),
                                         getFormattedNumber(_currentPoint.getX()),
                                         getFormattedNumber(_currentPoint.getY()),
                                         getFormattedNumber(_currentPoint.getZ()),
                                         getFormattedNumber(_currentPoint.getM()));
            default : return "";
        }
    }
    
    private String getFormattedString(int        _dim, 
                                      String     _formatter,
                                      Coordinate _currentPoint) {
        switch (_dim) {
            case 2: return String.format(_formatter,
                                         getFormattedNumber(_currentPoint.getX()),
                                         getFormattedNumber(_currentPoint.getY()));
            case 3: return String.format(_formatter,
                                         getFormattedNumber(_currentPoint.getX()),
                                         getFormattedNumber(_currentPoint.getY()),
                                         getFormattedNumber(_currentPoint.getZ()));
            case 4: return String.format(_formatter,
                                         getFormattedNumber(_currentPoint.getX()),
                                         getFormattedNumber(_currentPoint.getY()),
                                         getFormattedNumber(_currentPoint.getZ()),
                                         getFormattedNumber(_currentPoint.getM()));
            default : return "";
        } 
    }

    /**
     *
     * @param _geom JGeometry
     * @tobedone Need to handle circles/circular arcs. 
     * @tobedone Probably better to reduce complexity of the code by a MarkGeometry interface, 
     *           with markPoint, markMultiPoint, markLine, markPolygon implementation classes.
     * @author Simon Greener, April 2011, Original coding
     */
    private void markElement(JGeometry _geom,
                             boolean   _isInnerRing)
    {
        if ( _geom == null ) {
            return;
        }
        try
        {
            double[] points    = _geom.getOrdinatesArray();
            if ( points == null || points.length < 1) {
                return;
            }
            
            int numOrds        = points.length;
            int dim            = _geom.getDimensions();
            int numPoints      = _geom.getNumPoints();
            int geomType       = _geom.getType();
            Envelope mapWindow = new Envelope(0.0,0.0,this.layer.getSpatialView().getMapPanel().getSize());

            // Set up and save current styling information
            //
            MutableAttributeSet styleAttributes = this.layer.styling.getMarkLabelAttributes();
            if ( StyleConstants.getBackground(styleAttributes)==null ) {
               StyleConstants.setBackground(styleAttributes,this.layer.getView().getMapPanel().getBackground());
            }

            Stroke oldStroke = this.graphics2D.getStroke();
            this.graphics2D.setStroke(LineStyle.getStroke(this.layer.styling.getLineStrokeType(),
                                                          this.layer.styling.getLineWidth() + 1));
            Color savePointColor = this.layer.styling.getPointColor(null);
            String pointColorRGB = Colours.getRGBa(savePointColor);
            this.layer.styling.setPointColor(null);
            Styling.STYLING_TYPE savePointColorSource = this.layer.styling.getPointColorType();
            this.layer.styling.setPointColorType(Styling.STYLING_TYPE.CONSTANT);

            // Set up formatter string
            //
            String format       = "",
                   formatter    = "%s",
                   coordForm    = "%s,%s",
                   bracketStart = RenderTool.getBracketStart(this.layer.getPreferences().getSdoGeometryBracketType(),false),
                   bracketEnd   = RenderTool.getBracketEnd(  this.layer.getPreferences().getSdoGeometryBracketType(),false);
            if ( dim >= 3 ) { coordForm += ",%s"; if ( dim>=4 )  coordForm += ",%s"; }
            switch ( this.layer.styling.getMarkVertex() ) {
              case ID                : formatter = "<%s>"; break;
              case COORD             : formatter =            bracketStart + coordForm + bracketEnd; break;
              case ID_COORD          : formatter = "<%s> "  + bracketStart + coordForm + bracketEnd; break;
              case ID_CR_COORD       : formatter = "<%s>\n" + bracketStart + coordForm + bracketEnd; break;                
              case CUMULATIVE_LENGTH : formatter = "%s";                                             break;
              case X                 :
              case Y                 : 
              case MEASURE           :
              case ELEVATION         : formatter =            bracketStart + "%s"      + bracketEnd; break;
              case NONE              : 
              default                : /* Do nothing */
            }

            // Record starting point for initial mark vertex and for later processing
            //
            Coordinate startPoint = new Coordinate(points[0],points[1]);
            switch (dim) {
                case 4: startPoint.setM(points[3]); /* Flow though to set its Z ordinate */
                case 3: startPoint.setZ(points[2]);
            }
            Coordinate currentPoint  = new Coordinate(startPoint);
            Coordinate previousPoint = new Coordinate();
            Coordinate nextPoint     = new Coordinate(points[dim],points[dim+1]);
            int lastOrdPosn = -1;
            if ( geomType==JGeometry.GTYPE_POLYGON ) {
                lastOrdPosn = numOrds-(dim*2);
                previousPoint = new Coordinate(points[lastOrdPosn++],points[lastOrdPosn++]);
            } else {
                // Fabricate one be extending backwards from start point
                previousPoint = new Coordinate(currentPoint.getX() - (nextPoint.getX() - currentPoint.getX()),
                                               currentPoint.getY() - (nextPoint.getY() - currentPoint.getY()));
                lastOrdPosn = 2;
            }
            switch (dim) {
                case 4: previousPoint.setM(points[lastOrdPosn++]);
                            nextPoint.setM(points[dim+3]);
                        /* Flow though to set its Z ordinate */
                case 3: previousPoint.setZ(points[lastOrdPosn++]);
                            nextPoint.setZ(points[dim+2]);
            }

            int ord = dim;
            // =================== MULTIPOINTS handled separately
            //
            if ( geomType == JGeometry.GTYPE_MULTIPOINT  ) {
                // Multipoints markers are drawn by call to drawPoint.
                //
                for (int pointNum = 0; pointNum < numPoints; pointNum++ )
                {
                    ord = ( pointNum * dim );
                    switch (dim) {
                        case 4: currentPoint.setM(points[ord+3]); /* Flow though to set its Z ordinate */
                        case 3: currentPoint.setZ(points[ord+2]); /* Flow though to set its XY ordinate */
                        case 2: currentPoint.setLocation(points[ord],points[ord+1]);
                    } 
                    // Mark this point if in the window and marking of points is required
                    //
                    if ( this.layer.getSpatialView().getMBR().contains(currentPoint) ) 
                    {
                        switch ( this.layer.styling.getMarkVertex() ) {
                          case ID          : format = String.format(formatter,String.valueOf(pointNum+1));       break;
                          case ID_COORD    :
                          case ID_CR_COORD : format = getFormattedString(dim,pointNum+1,formatter,currentPoint); break;
                          case COORD       : format = getFormattedString(dim,formatter,currentPoint);            break;
                          case X           : format = String.format(formatter,getFormattedNumber(currentPoint.getX())); break;
                          case Y           : format = String.format(formatter,getFormattedNumber(currentPoint.getY())); break;
                          case MEASURE     : format = _geom.getLRMDimension()==0
                                                      ? ""
                                                      : getFormattedNumber(_geom.getLRMDimension()==3
                                                                           ? currentPoint.getZ()
                                                                           : currentPoint.getM()); break;
                          case ELEVATION   : if (_geom.getLRMDimension()==0 && _geom.getDimensions()==2)
                                                 format = "";
                                             else if ( _geom.getLRMDimension()==0 && _geom.getDimensions()>=3 )
                                                 format = getFormattedNumber(currentPoint.getZ());
                                             else if ( _geom.getLRMDimension()==4 && _geom.getDimensions()==4 )
                                                 format = getFormattedNumber(currentPoint.getZ()); break;
						default:
							break;
                        }
                        drawLabel(format,
                                  currentPoint,
                                  Constants.ROTATE.LABEL,
                                  0.0,
                                  styleAttributes,
                                  this.layer.styling.getMarkLabelPosition(),
                                  this.layer.styling.getMarkLabelOffset());
                    }
                } // For all points
                return;
                
            // =================== POLYGONS and LINESTRING handling
            //
            } else {
                int maxPointsMinusOne = numPoints - 1;
                double         length = 0.0,
                    cumulative_length = 0.0;
                
                // Place a marker on the first point
                // Start is rendered differently
                //
                if (this.layer.getSpatialView().getMBR().contains(startPoint) ) 
                {
                    if ( this.layer.styling.isMarkGeoStart() )  {
                        drawPoint(startPoint.getX(),startPoint.getY(),
                                  this.layer.styling.getMarkGeoStart(),
                                  Colours.getRGBa(this.layer.styling.getLineColor(Colours.getRGBa(Color.BLUE))),
                                  this.layer.styling.getPointSize(4),
                                  Constants.ROTATE.MARKER,
                                  0.0);
                    }
                    if (this.layer.styling.isMarkVertex() ) {
                        markLabelVertex(dim,
                                        geomType,
                                        0,
                                        maxPointsMinusOne,
                                        currentPoint,
                                        nextPoint,
                                        previousPoint,
                                        _isInnerRing,
                                        formatter,
                                        cumulative_length,
                                        _geom);
                    }
                }
                Line2D lineSegment = new Line2D.Double(previousPoint.getX(),previousPoint.getY(),
                                                        currentPoint.getX(), currentPoint.getY());

                previousPoint.setLocation(currentPoint);

                // Now process all other points
                //
                for (int pointNum = 1; pointNum < numPoints; pointNum++ )
                {
                    ord = ( pointNum * dim );
                    
                    // Get current point being processed
                    //
                    switch (dim) {
                        case 4: currentPoint.setM(points[ord+3]); /* Flow though to set Z ordinate */
                        case 3: currentPoint.setZ(points[ord+2]); /* Flow though to set XY ordinates */
                        case 2: currentPoint.setLocation(points[ord],points[ord+1]);
                    } 

                    // Get NEXT point to determine angle between vectors
                    //
                    if ( pointNum == maxPointsMinusOne ) {
                        if ( geomType==JGeometry.GTYPE_CURVE ) {
                            // fabricate point on same projected line
                            switch (dim) {
                                case 4: nextPoint.setM(currentPoint.getM() + (currentPoint.getM()-previousPoint.getM())); /* Flow though to set Z ordinate */
                                case 3: nextPoint.setZ(currentPoint.getZ() + (currentPoint.getZ()-previousPoint.getZ())); /* Flow though to set XY ordinates */
                                case 2: nextPoint.setLocation(currentPoint.getX() + (currentPoint.getX()-previousPoint.getX()),
                                                              currentPoint.getY() + (currentPoint.getY()-previousPoint.getY()));
                            }
                        } else if ( geomType==JGeometry.GTYPE_POLYGON ) { // Polygon
                            // Get second point from start
                            switch (dim) {
                                case 4: nextPoint.setM(points[dim+3]); /* Flow though to set Z ordinate */
                                case 3: nextPoint.setZ(points[dim+2]); /* Flow though to set XY ordinates */
                                case 2: nextPoint.setLocation(points[dim],points[dim+1]);
                            } 
                        }
                    } else {
                        switch (dim) {
                            case 4: nextPoint.setM(points[ord+dim+3]); /* Flow though to set Z ordinate */
                            case 3: nextPoint.setZ(points[ord+dim+2]); /* Flow though to set XY ordinates */
                            case 2: nextPoint.setLocation(points[ord+dim],
                                                          points[ord+dim+1]);
                        } 
                    }

                    // Now that all points are determined we can create our vector line
                    //
                    lineSegment.setLine(previousPoint.getX(),previousPoint.getY(), 
                                         currentPoint.getX(), currentPoint.getY());

                    // Compute length and cumulative_length
                    //
                    if ( this.layer.getSRIDType().toString().startsWith("GEO") ) {
                        length = COGO.distVincenty(previousPoint,currentPoint);
                    } else {
                        // If 3D COGO.distance(Coord,Coord) will compute slope length
                        //
                        if ( _geom.getLRMDimension()!=3 ) {
                            length = COGO.distance(previousPoint,currentPoint);
                        } else {
                            // Compute planar distance
                            length = COGO.distance(previousPoint.getX(),previousPoint.getY(), 
                                                    currentPoint.getX(), currentPoint.getY());
                        }
                    }
                    cumulative_length += length;

                    // Are we marking/labelling a vertex and its text?
                    //
                    if ( this.layer.getSpatialView().getMBR().contains(currentPoint) ) {
                        // Do we mark the vertex with an icon (circle, star etc)?
                        //
                        if ( this.layer.styling.isMarkGeoPoints() ) {
                            // but not where the end vertex is same as start....
                            //
                            if ( pointNum!=maxPointsMinusOne &&
                               ! ( ( dim==2 && startPoint.equals2D(currentPoint)) ||
                                   ( dim==3 && startPoint.equals3D(currentPoint)) ||
                                   ( dim==4 && startPoint.equals4D(currentPoint)) ) ) 
                            {
                                drawPoint(currentPoint.getX(),
                                          currentPoint.getY(),
                                          this.layer.styling.getMarkGeoPoints(),
                                          pointColorRGB,
                                          this.layer.styling.getPointSize(4),
                                          Constants.ROTATE.MARKER,
                                          0.0);
                            }
                        }
                        // Dow we label the vertex with text?
                        //
                        if (this.layer.styling.isMarkVertex() ) {
                            markLabelVertex(dim,
                                            geomType,
                                            pointNum,
                                            maxPointsMinusOne,
                                            currentPoint,
                                            nextPoint,
                                            previousPoint,
                                            _isInnerRing,
                                            formatter,
                                            cumulative_length,
                                            _geom);
                        }
                    } 

                    // Now mark line with its direction
                    // Only if end point (current point) is visible
                    //
                    if ( this.layer.styling.getSegmentArrow() != Constants.SEGMENT_ARROWS_TYPE.NONE &&
                         this.layer.styling.getSegmentArrow() != Constants.SEGMENT_ARROWS_TYPE.END_ONLY && 
                         this.layer.getSpatialView().getMBR().contains(currentPoint) )
                    {
                        drawLineDirection(mapWindow,
                                          lineSegment,
                                          this.layer.styling.getLineColor(Colours.getRGBa(Color.BLUE)),
                                          this.layer.styling.getSegmentArrow(),
                                          this.markStroke);
                    }

                    // ********************** Label segment with bearing/distance? ....
                    // 
                    if ( this.layer.styling.getMarkSegment() != Constants.SEGMENT_LABEL_TYPE.NONE ) 
                    {
                        // We will draw this label in the middle of the line
                        //
                        double midPointX = (previousPoint.getX()+currentPoint.getX()) / 2.0;
                        double midPointY = (previousPoint.getY()+currentPoint.getY()) / 2.0;
                        
                        // Compute bearing 
                        // drawAngle based on graphics and not projection.
                        //
                        double bearing = 0.0,
                               drawAngle = 0.0;

                        bearing   = COGO.normalizeDegrees(Math.toDegrees(COGO.bearing(previousPoint,currentPoint)));
                        drawAngle = COGO.radians((180.0<bearing && bearing<360.0)?bearing+90:bearing-90);

                        // Process Geodetic/geographic data differently
                        //
                        if ( this.layer.getSRIDType().toString().startsWith("GEO") ) {
                            // If polygon inner/outer.... do something with bearing.
                            bearing = COGO.normalizeDegrees(COGO.GreatCircleBearing(previousPoint,currentPoint));
                        }
 
                        // Create marking text
                        //
                        // DEBUG Messages.log("Bearing is " + bearing + " and drawAngle is "+ drawAngle);
                        String  bearingText = "",
                               distanceText = "";
                        
                        if ( this.layer.styling.getMarkSegment() == Constants.SEGMENT_LABEL_TYPE.SEGMENT_ID ) {
                            drawLabel(String.format("<%s>",String.valueOf(pointNum)),
                                      midPointX,
                                      midPointY,
                                      Constants.ROTATE.LABEL,
                                      drawAngle,
                                      this.layer.styling.getMarkLabelAttributes(),
                                      Constants.TEXT_OFFSET_POSITION.TC,
                                      this.layer.styling.getMarkLabelOffset() );                            
                        } else {
                            // Mark bearing
                            //
                            if (this.layer.styling.getMarkSegment() == Constants.SEGMENT_LABEL_TYPE.BEARING ||
                                this.layer.styling.getMarkSegment() == Constants.SEGMENT_LABEL_TYPE.BEARING_AND_DISTANCE ) {
                                bearingText = COGO.DD2DMS(bearing,1);
                                // Draw bearing label
                                //
                                drawLabel(bearingText,
                                          midPointX,
                                          midPointY,
                                          Constants.ROTATE.LABEL,
                                          drawAngle,
                                          this.layer.styling.getMarkLabelAttributes(),
                                          Constants.TEXT_OFFSET_POSITION.BC,
                                          this.layer.styling.getMarkLabelOffset() );
                            }
                            // Mark Distance/length
                            //
                            if (this.layer.styling.getMarkSegment() == Constants.SEGMENT_LABEL_TYPE.BEARING_AND_DISTANCE ||
                                this.layer.styling.getMarkSegment() == Constants.SEGMENT_LABEL_TYPE.SEGMENT_LENGTH       ||
                                this.layer.styling.getMarkSegment() == Constants.SEGMENT_LABEL_TYPE.CUMULATIVE_LENGTH ) {
                                
                                if ( this.layer.styling.getMarkSegment() == Constants.SEGMENT_LABEL_TYPE.CUMULATIVE_LENGTH ) {
                                    distanceText = getFormattedNumber(cumulative_length); 
                                } else {
                                    distanceText = getFormattedNumber(length); 
                                }
                                // Draw distance label
                                //
                                drawLabel(distanceText,
                                          midPointX,
                                          midPointY,
                                          Constants.ROTATE.LABEL,
                                          drawAngle,
                                          this.layer.styling.getMarkLabelAttributes(),
                                          Constants.TEXT_OFFSET_POSITION.TC,
                                          this.layer.styling.getMarkLabelOffset() );
                            }
                        }
                    }
                    previousPoint.setLocation(currentPoint);
                } // For all points
                
                // Mark last segment's end arrow or end point (if not polygon)
                //
                if ( ( this.layer.styling.getSegmentArrow() == Constants.SEGMENT_ARROWS_TYPE.END ||
                       this.layer.styling.getSegmentArrow() == Constants.SEGMENT_ARROWS_TYPE.END_ONLY ) 
                    && this.layer.getSpatialView().getMBR().contains(lineSegment))  {
                    drawLineDirection(mapWindow,
                                      lineSegment,
                                      this.layer.styling.getLineColor(Colours.getRGBa(Color.BLUE)),
                                      this.layer.styling.getSegmentArrow(),
                                      this.markStroke);
                }
                
                if (    geomType != JGeometry.GTYPE_POLYGON 
                     && geomType != JGeometry.GTYPE_MULTIPOLYGON 
                     && this.layer.styling.isMarkGeoPoints() 
                     && this.layer.getSpatialView().getMBR().contains(lineSegment.getP2()) ) 
                {
                    drawPoint(lineSegment.getP2().getX(),lineSegment.getP2().getY(),
                              this.layer.styling.getMarkGeoPoints(),
                              pointColorRGB,
                              this.layer.styling.getPointSize(4),
                              Constants.ROTATE.MARKER,
                              0.0);
                }
            }
            this.graphics2D.setStroke(oldStroke);
            this.layer.styling.setPointColor(savePointColor );
            this.layer.styling.setPointColorType(savePointColorSource);
        } catch (Exception e) {
            LOGGER.error("Problem marking GeoPoints() " + e.getMessage());
        }
    }

    private void markLabelVertex(int        _dim,
                                 int        _geomType, 
                                 int        _pointNum, 
                                 int        _maxPointsMinusOne, 
                                 Coordinate _currentPoint, 
                                 Coordinate _nextPoint, 
                                 Coordinate _previousPoint,
                                 boolean    _isInnerRing, 
                                 String     _formatter,
                                 double     _cumulative_length,
                                 JGeometry  _geom) 
    {
        // Label the current point
        //
        String format = "";
        double bearing = 90.0,
                angle  = 0.0,
               radians = 0.0;
        if ( this.layer.styling.isMarkOriented() ) {
            double bearingNext = 0.0,
                   bearingPrev = 0.0;
            // Compute mark radians
            //
            if ( _geomType == JGeometry.GTYPE_CURVE ) {
                if ( _pointNum == 0 || _pointNum == _maxPointsMinusOne ) {
                    bearing = COGO.degrees(COGO.bearing(_currentPoint,_nextPoint)) - 90.0;
                } else {
                    angle = COGO.angleBetween3Points(_nextPoint,_currentPoint,_previousPoint);
                    angle = Math.round(COGO.degrees(angle));
                    if (angle < 0 ) {
                       angle = angle + 360.0;
                    }
                    bearing = COGO.degrees(COGO.bearing(_currentPoint,_nextPoint)) - (angle/2.0);
                }
                bearing += 180.0;
            } else if ( _geomType == JGeometry.GTYPE_POLYGON ) {
                // 1. calculate bearings
                //
                bearingNext = Math.round(Math.toDegrees(COGO.bearing(_currentPoint,_nextPoint)));
                bearingPrev = Math.round(Math.toDegrees(COGO.bearing(_previousPoint,_currentPoint)));
                // 2. Reverse prev
                //
                double revBearingPrev = bearingPrev + (bearingPrev>=180.0?-180.0:180.0);
                // 3. Compute left angle 
                //
                angle = COGO.normalizeDegrees(bearingNext<revBearingPrev?360.0+bearingNext:bearingNext) - revBearingPrev; 
                // 4. Compute bisector angle
                if ( _isInnerRing ) {
                    // Rotation is clockwise, create angle on inside of hole which is to the right of bearingNext
                    if (angle>=0)
                        angle = angle - 360.0;
                    bearing = bearingNext - (angle/2.0);
                } else {
                    // Rotation is anti-clockwise, create angle on outside of polygon which is to the left of bearingNext
                    if (angle>=0)
                        angle = 360.0 - angle;
                    else
                        angle = Math.abs(angle);
                    bearing = bearingNext + (angle/2.0);
                } // delta != 0.0
                // If last point, flip the label
                if ( _pointNum==_maxPointsMinusOne ) {
                    bearing += 180;
                   //labelPosn = Constants.TEXT_OFFSET.CR;
                }
                /*
LOGGER.debug(String.format("Point=% 3d -> (bearNext=%6.1f,bearPrev=%6.1f,revBearPrev=%6.1f,delta=%6.1f,bear=%6.1f) - %b",
                          _pointNum+1,
                          bearingNext,
                          bearingPrev,
                          revBearingPrev,
                          angle,
                          bearing,
                          _isInnerRing));
                */
            }
            // Subtract 90.0 as axes for radians is 90 degrees different than for surveying bearings
            //
            bearing = COGO.normalizeDegrees(bearing - 90.0);
            radians = Math.toRadians(bearing);
        }
        switch ( this.layer.styling.getMarkVertex() ) {
            case ID                : format = String.format(_formatter,String.valueOf(_pointNum+1)); break;
            case ID_COORD          :
            case ID_CR_COORD       : format = getFormattedString(_dim,_pointNum+1,_formatter,_currentPoint); break;
            case COORD             : format = getFormattedString(_dim,_formatter,_currentPoint); break;
            case CUMULATIVE_LENGTH : format = getFormattedNumber(_cumulative_length); break;             
            case X                 : format = String.format(_formatter,getFormattedNumber(_currentPoint.getX())); break;
            case Y                 : format = String.format(_formatter,getFormattedNumber(_currentPoint.getY())); break;
            case MEASURE           : format = _geom.getLRMDimension()==0
                                              ? ""
                                              : getFormattedNumber(_geom.getLRMDimension()==3
                                                                   ? _currentPoint.getZ()
                                                                   : _currentPoint.getM()); break;
            case ELEVATION         : if (_geom.getLRMDimension()==0 && _geom.getDimensions()==2)      
                                         format = "";
                                     else if ( _geom.getLRMDimension()==0 && _geom.getDimensions()>=3 )
                                         format = getFormattedNumber(_currentPoint.getZ());
                                     else if ( _geom.getLRMDimension()==4 && _geom.getDimensions()==4 )
                                         format = getFormattedNumber(_currentPoint.getZ()); break;
		default:
			break;

        }
        drawLabel(format,
                  _currentPoint,
                  Constants.ROTATE.LABEL,
                  radians,
                  this.layer.styling.getMarkLabelAttributes(),
                  this.layer.styling.getMarkLabelPosition(),  // SGG?
                  this.layer.styling.getMarkLabelOffset() );
    }

    private void markPoint(JGeometry _geo) {
        
        double[] point = _geo.getFirstPoint();
        Coordinate markPoint = new Coordinate(point[0],point[1]);
        int dim = _geo.getDimensions();
        int mDim = _geo.getLRMDimension();
        if ( dim == 3 ) {
            if ( mDim != 0 ) {
                markPoint.setM(point[2]);
            } else {
                markPoint.setZ(point[2]);
            }
        } else if ( dim == 4 ) {
            if ( mDim == 3 ) {
                markPoint.setM(point[2]);
                markPoint.setZ(point[3]);
            } else if ( mDim == 4 ) {
                markPoint.setM(point[3]);
                markPoint.setZ(point[2]);
            }
        }
        
        MutableAttributeSet styleAttributes = this.layer.styling.getMarkLabelAttributes(); // getLabelAttributes();
        if ( StyleConstants.getBackground(styleAttributes)==null ) {
            StyleConstants.setBackground(styleAttributes,this.layer.getView().getMapPanel().getBackground());
        }
        
        // Set up and get formatted string
        //
        String bracketStart = RenderTool.getBracketStart(this.layer.getPreferences().getSdoGeometryBracketType(),false),
               bracketEnd   = RenderTool.getBracketEnd(  this.layer.getPreferences().getSdoGeometryBracketType(),false);

        String displayString = "",
                   coordForm = "%s,%s";
        if ( _geo.getDimensions() >= 3 ) { 
            coordForm += ",%s"; 
            if ( _geo.getDimensions()>=4 ) {
                coordForm += ",%s"; 
            }
        }
        switch ( this.layer.styling.getMarkVertex() ) {
          case ID                : displayString = String.format("<%s>","1");
                                   break;
          case COORD             : displayString = getFormattedString(dim,bracketStart + coordForm + bracketEnd,markPoint);
                                   break;
          case ID_COORD          : displayString = getFormattedString(dim,1,"<%s> "  + bracketStart + coordForm + bracketEnd,markPoint);
                                   break;
          case ID_CR_COORD       : displayString = getFormattedString(dim,1,"<%s>\n" + bracketStart + coordForm + bracketEnd,markPoint);
                                   break;
          case X                 : displayString = bracketStart + getFormattedNumber(markPoint.getX()) + bracketEnd; 
                                   break;
          case Y                 : displayString = bracketStart + getFormattedNumber(markPoint.getY()) + bracketEnd; 
                                   break;
          case MEASURE           : if ( Double.isNaN(markPoint.getM())) {
                                      displayString = "";
                                   } else {
                                      displayString = bracketStart + getFormattedNumber(markPoint.getM()) + bracketEnd; 
                                   }
                                   break;
          case ELEVATION         : if ( Double.isNaN(markPoint.getZ())) {
                                      displayString = "";
                                   } else {
                                       displayString = bracketStart + getFormattedNumber(markPoint.getZ()) + bracketEnd; 
                                   }
                                   break;
          case NONE              : 
          default                : /* Do nothing */
        }
        drawLabel(displayString,
                  point[0],point[1],
                  Constants.ROTATE.LABEL,
                  0.0f,
                  styleAttributes,
                  this.layer.styling.getMarkLabelPosition(),
                  this.layer.styling.getMarkLabelOffset());
    }

    private String getFormattedNumber(double _number) {
      DecimalFormat dFormat = Tools.getNLSDecimalFormat(this.layer.getPrecision(false),
                                                        this.layer.getPreferences().getGroupingSeparator());
      return Double.isNaN(_number)?"NULL":dFormat.format(_number);
    }

    /**
     * Draw line direction for given coordinates
     */
    public void drawLineDirection(Envelope          _mapWindow,
                                  Line2D                   _line,
                                  Color                    _symbColor,
                                  Constants.SEGMENT_ARROWS_TYPE _position,
                                  BasicStroke              _stroke)
    {
        if ( _line == null|| _line.getP1() == null || _line.getP2()==null )
            return;
        Point2D point1 = this.getWorldToScreenTransform().transform(_line.getP1(),null);
        Point2D point2 = this.getWorldToScreenTransform().transform(_line.getP2(),null);

        if ((point1.getX() == point2.getX()) &&
            (point1.getY() == point2.getY())) {
            // not valid combination - do nothing
            return;
        }

        // Compute rotation for arrow
        //
        double rotationAngle = Math.atan2(point2.getY() - point1.getY(), point2.getX() - point1.getX()) + (Math.PI / (double)2.0);

        // Compute arrow position
        //
        Point2D drawPoint = new Point2D.Double(point1.getX(),point1.getY());

        switch (_position) {
        case END_ONLY :
        case END      : drawPoint.setLocation(point2.getX(), point2.getY());
                        break;
        case START    : // Compensate arrow position
                        double length = MathUtils.pythagoras(point1,point2);
                        Point2D delta = new Point2D.Double(0,0);
                        if ( (int)length != 0 && length>(double)this.layer.styling.getPointSize(4) ) {
                            delta.setLocation((point2.getX() - point1.getX()) *
                                              (double)this.layer.styling.getPointSize(4) / length,
                                              (point2.getY() - point1.getY()) *
                                              (double)this.layer.styling.getPointSize(4) / length);
                        }
                        drawPoint.setLocation(point1.getX() + delta.getX() / (double)2.0,
                                              point1.getY() + delta.getY() / (double)2.0 );
                        break;
        case MIDDLE   : drawPoint.setLocation((point1.getX() + point2.getX()) / (double)2.0,
                                              (point1.getY() + point2.getY()) / (double)2.0);
                        break;
		default:
			break;
        }

        if (! _mapWindow.contains(drawPoint))
          return;

        Shape drawShape = PointMarker.getPointShape(PointMarker.MARKER_TYPES.ARROWHEAD,
                                                    drawPoint,
                                                    this.layer.styling.getPointSize(4),
                                                    rotationAngle);
        // remember previous settings
        Color  oldColor  = this.graphics2D.getColor();
        Stroke oldStroke = this.graphics2D.getStroke();

        // set symbol color on some new color
        this.graphics2D.setColor(_symbColor);
        this.graphics2D.setStroke(_stroke);

        this.graphics2D.fill(drawShape);
        this.graphics2D.draw(drawShape);

        // set back original settings
        this.graphics2D.setColor(oldColor);
        this.graphics2D.setStroke(oldStroke);
    }

    public void setLabelPosition(Constants.TEXT_OFFSET_POSITION _labelPosition) {
        switch (_labelPosition) {
        case TL:
            vertPosn = ALIGN_TOP;
            horizPosn = ALIGN_LEFT;
            break;
        case TC:
            vertPosn = ALIGN_TOP;
            horizPosn = ALIGN_MIDDLE;
            break;
        case TR:
            vertPosn = ALIGN_TOP;
            horizPosn = ALIGN_RIGHT;
            break;
        case CL:
            vertPosn = ALIGN_MIDDLE;
            horizPosn = ALIGN_LEFT;
            break;
        case CC:
        default:
            vertPosn = ALIGN_MIDDLE;
            horizPosn = ALIGN_MIDDLE;
            break;
        case CR:
            vertPosn = ALIGN_MIDDLE;
            horizPosn = ALIGN_RIGHT;
            break;
        case BL:
            vertPosn = ALIGN_BOTTOM;
            horizPosn = ALIGN_LEFT;
            break;
        case BC:
            vertPosn = ALIGN_BOTTOM;
            horizPosn = ALIGN_MIDDLE;
            break;
        case BR:
            vertPosn = ALIGN_BOTTOM;
            horizPosn = ALIGN_RIGHT;
            break;
        }
    }

    private boolean isWithinScale() {
        // Check Scale
        //
        int mapScale = this.layer.getSpatialView().getMapPanel().getMapScale();
        return ( mapScale >= this.layer.styling.getTextLoScale() && 
                 mapScale <= this.layer.styling.getTextHiScale() );
    }
    
    public void drawLabel(String                  _label,
                          Point2D                 _point,
                          Constants.ROTATE        _rotate,
                          double                  _angle,
                          AttributeSet            _attributes,
                          Constants.TEXT_OFFSET_POSITION _labelPosition,
                          int                     _offset)
    {
        if (_point == null)
            return;
        Point2D point2D = this.getWorldToScreenTransform().transform(_point,null);
        setLabelPosition(_labelPosition);
        // Got point so now draw label
        // if within Scale
        //
        if ( this.isWithinScale() ) {
            drawString(this.graphics2D,_label,
                       (int)point2D.getX(), (int)point2D.getY(), 
                       this.horizPosn, this.vertPosn, 
                       _rotate, 
                       _angle, 
                       _attributes,
                       _offset);
        }
    }

    public void drawLabel(String                  _label,
                          Coordinate              _point,
                          Constants.ROTATE        _rotate,
                          double                  _angle,
                          AttributeSet            _attributes,
                          Constants.TEXT_OFFSET_POSITION _labelPosition,
                          int                     _offset)
    {
        if (_point == null) {
            return;
        }
        Point2D point2D = this.getWorldToScreenTransform().transform(new Point.Double(_point.x,_point.y),null);
        setLabelPosition(_labelPosition);
        // Got point so now draw label
        // if within Scale
        //
        if ( this.isWithinScale() ) {
            drawString(this.graphics2D,_label,
                       (int)point2D.getX(), (int)point2D.getY(), 
                       this.horizPosn, this.vertPosn, 
                       _rotate, 
                       _angle, 
                       _attributes,
                       _offset);
        }
    }

    /**
     * @method drawLabel
     * @param _label
     * @param _x
     * @param _y
     * @param _rotate
     * @param _angle
     * @param _attributes
     * @param _labelPosition
     * @param _offset
     * @author Simon Greener, May 28th 2010
     *          Heavily modified to make work and support drawing oriented point symbol
     */
    public void drawLabel(String           _label, 
                          double           _x, 
                          double           _y,
                          Constants.ROTATE _rotate, 
                          double           _angle,
                          AttributeSet     _attributes,
                          Constants.TEXT_OFFSET_POSITION _labelPosition,
                          int              _offset) 
    {
        // Assumes Point2D has NOT been world2Screened!
        Point2D point = this.getWorldToScreenTransform().transform(new Point.Double(_x,_y),null);
        setLabelPosition(_labelPosition);
        // Got point so now draw label
        // if within Scale
        //
        if ( this.isWithinScale() ) {
            drawString(this.graphics2D,_label,
                       (int)point.getX(), (int)point.getY(), 
                       this.horizPosn, this.vertPosn, 
                       _rotate, 
                       _angle, 
                       _attributes,
                       _offset);
        }
    }

    /**
     * @method drawString
     * @param _string : Label to be written
     * @param _x      : base X position of label point in transformed units
     * @param _y      : base Y position of label point in transformed units
     * @param _alignX : Constant that represents X component of LL, UR etc.
     * @param _alignY : Constant that represents Y component of LL, UR etc.
     * @param _rotate : boolean rotate marker symbol using _angle or not
     * @param _angle  : angle in radians
     * @param _attributes : properties of _string font
     * @param _offset     : position of label as offset in display from _x,_y
     * @author Simon Greener April 6th 2010
     * @author Simon Greener, May 28th 2010
     *          Heavily modified to make work
     *          Font and Colour supported internally until more general support for labelling at the layer level is completed.
     * @author Simon Greener, December 15th 2010,
     *          Changed custom code for background colour etc to code based on standard AttributedString Java class.
     * @author Simon Greener, April 20th 2015,
     *          Added Graphics2D parameter to allow for method to be called in other situations than a layer render.
     */
    public static void drawString(Graphics2D       _g2d,
                                  String           _string,
                                  int              _x,      int _y,
                                  int              _alignX, int _alignY,
                                  Constants.ROTATE _rotate,
                                  double           _angle,
                                  AttributeSet     _attributes,
                                  int              _offset)
    {
        if (Strings.isEmpty(_string)) {
            return;
        }

        Font renderFont = new Font(StyleConstants.getFontFamily(_attributes),
                                   ((StyleConstants.isBold(_attributes)   ? Font.BOLD : Font.PLAIN) +
                                    (StyleConstants.isItalic(_attributes) ? Font.ITALIC : Font.PLAIN)),
                                   StyleConstants.getFontSize(_attributes));

        Color foreColour = StyleConstants.getForeground(_attributes);
        Color backColour = StyleConstants.getBackground(_attributes);

        AttributedString label = new AttributedString(_string);
        label.addAttribute(TextAttribute.FONT, renderFont);
        if (StyleConstants.isUnderline(_attributes))     label.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        if (StyleConstants.isStrikeThrough(_attributes)) label.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        if ( foreColour != null ) label.addAttribute(TextAttribute.FOREGROUND, foreColour);
        if ( backColour != null ) label.addAttribute(TextAttribute.BACKGROUND, backColour);
        label.addAttribute(TextAttribute.KERNING,TextAttribute.KERNING_ON);

        // Determine box width/height for string/font
        //
        _g2d.setFont(renderFont);
        FontMetrics fm = _g2d.getFontMetrics();
        int w = _g2d.getFontMetrics().stringWidth(_string);
        int h = Math.min(fm.getHeight(),
                         (int)fm.getStringBounds(_string,_g2d).getHeight()) - fm.getMaxDescent();

        // Need to calculate position of text relative to 9 points: TL, TC, TR, CL, CC, CR, BL, BC, BR
        //
        int x2 =   (_alignX == ALIGN_RIGHT)  ? _x - (w+_offset)
                 : (_alignX == ALIGN_CENTRE) ? _x - (w / 2)
                 : (_alignX == ALIGN_LEFT)   ? _x + _offset : _x;
        int y2 =   (_alignY == ALIGN_TOP)    ? _y + (h+_offset)
                 : (_alignY == ALIGN_MIDDLE) ? _y + (h / 2)
                 : (_alignY == ALIGN_BOTTOM) ? _y - _offset : _y;

        int deltaX = x2 - _x;
        int deltaY = y2 - _y;
        // DEBUG LOGGER.info("drawString(" + _string + " (_x,_y)("+_x+","+_y+")(x2,y2)(" + x2+ "," + y2+")(w,h)("+w+","+h+")=>(deltaX,deltaY)("+deltaX+","+deltaY+")");
        if ( _string.indexOf("\n")==-1 ) {
          drawTransformedString(_g2d,
                                label,
                                _x, _y,
                                deltaX, deltaY,
                                (_rotate == Constants.ROTATE.LABEL || 
                                 _rotate == Constants.ROTATE.BOTH),
                                _angle);
        } else {
            Point position = new Point(_x,_y);
            Pattern p = Pattern.compile("\n");
            String[] s = p.split(_string);
            int lineHeight = fm.getAscent() + fm.getDescent() + fm.getLeading();
            for ( int i=0; i<s.length; i++) {                
                AttributedString labelPart = new AttributedString(s[i],label.getIterator().getAttributes());
                drawTransformedString(_g2d,
                                      labelPart,
                                      position.x, position.y,
                                      deltaX, deltaY,
                                      (_rotate == Constants.ROTATE.LABEL ||
                                       _rotate == Constants.ROTATE.BOTH),
                                      _angle);
                // Move down one line
                position.y += lineHeight;
            }
        }
    }

    private static void drawTransformedString(Graphics2D        _g2d,
                                              AttributedString  _label,
                                              int _rPointX, int _rPointY,
                                              int _deltaX,  int _deltaY,
                                              boolean _rotate,
                                              double  _angle)
    {
        AffineTransform at = _g2d.getTransform();
        // Rotate and Translate in one action.
        //
        if (_rotate) {
            // DEBUG LOGGER.info("drawTransformedString: rotate(" + _angle + " (rX,rY)("+_rPointX+","+_rPointY+"))");
            _g2d.rotate(_angle, _rPointX, _rPointY);
        }
        if (!(_deltaX == 0 && _deltaY == 0)) {
            // DEBUG LOGGER.info("drawTransformedString: translate(dX,dY)("+_deltaX+","+_deltaY+"))");
            _g2d.translate(_deltaX, _deltaY);
        }
        _g2d.drawString(_label.getIterator(), _rPointX, _rPointY);
        _g2d.setTransform(at);
    }

    //****************** Unused ********************
    
    @SuppressWarnings("unused")
	private Shape getTranslated(Shape _shape, int _deltaX, int _deltaY) {
        if (!(_deltaX == 0 && _deltaY == 0)) {
            AffineTransform at = this.graphics2D.getTransform();
            this.graphics2D.translate(_deltaX, _deltaY);
            Shape s =
                this.graphics2D.getTransform().createTransformedShape(_shape);
            this.graphics2D.setTransform(at);
            return s;
        } else
            return _shape;
    }

    @SuppressWarnings("unused")
	private Shape getTransformed(Shape _shape, 
                                 int _rPointX, int _rPointY,
                                 int _deltaX,  int _deltaY, 
                                 boolean _rotate,
                                 double _angle) 
    {
        AffineTransform at = this.graphics2D.getTransform();
        // Rotate and Translate in one action.
        //
        if (_rotate) {
            this.graphics2D.rotate(_angle, _rPointX, _rPointY);
        }
        if (!(_deltaX == 0 && _deltaY == 0)) {
            this.graphics2D.translate(_deltaX, _deltaY);
        }
        Shape s =
            this.graphics2D.getTransform().createTransformedShape(_shape);
        this.graphics2D.setTransform(at);
        return s;
    }

    /**
     * @function printScreenArray
     * @param _message
     * @param _points
     * @param _dim
     * @param _coordsToDraw
     * @author Simon Greener April 4th 2010
     *          - Added to aid debugging of printing of java.awt.Shape
     */
    protected void printScreenArray(String   _message, 
                                    double[] _points,
                                    int      _dim, 
                                    int      _coordsToDraw) {
        if (_message != null)
            Messages.log(_message);
        if ((_coordsToDraw * _dim) < 1)
            return;
        int pLength =
            _points.length < (_coordsToDraw * _dim) ? _points.length :
            (_coordsToDraw * _dim);
        Point2D coord = null;
        for (int i = 0; i <= (pLength - _dim); i = i + _dim) {
            Messages.log("[" + i + "](" + _points[i] + "," + _points[i + 1] +
                         ((_dim >= 3) ?
                          "," + _points[i + _dim] + ((_dim == 4) ?
                                                     "," + _points[i + _dim +
                                                     1] : ")") : ")"));
            try {
                coord =
                        this.ScreenToWorld(new Point.Double(_points[i], _points[i +
                                                            1]));
                Messages.log("[" + i + "](" + coord.getX() + "," +
                             coord.getY() + ")");
            } catch (NoninvertibleTransformException nte) {
                LOGGER.warn("screenToWorld transform error for " + _points[i] + "," + _points[i + 1]);
            }
        }
        return;
    }


}
