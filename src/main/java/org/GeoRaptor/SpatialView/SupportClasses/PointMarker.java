package org.GeoRaptor.SpatialView.SupportClasses;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.Random;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

import org.GeoRaptor.Constants;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;


public class PointMarker {
    
    public static enum MARKER_TYPES {NONE, ALMOND, ARROWHEAD, ASTERISK, ASTROID, BOAT, CIRCLE, CROSS, DONUT, DIAMOND, PLUS, FISH, HASH, HOURGLASS, HEXAGON, HEPTAGON, OCTAGON, REULEAUX, STAR, SQUARE, TICK, TRIANGLE, PIN, PENTAGON};
    
    protected static PointMarker classInstance;

    public PointMarker() {
        super();
    }

    /**
     * Get instance of PointMarker class
     */
    public static PointMarker getInstance() {
        if (PointMarker.classInstance == null) {
            PointMarker.classInstance = new PointMarker();
        }
        return PointMarker.classInstance;
    }
    
    public static PointMarker.MARKER_TYPES getRandomMarker() {
        int numberMarkerTypes = PointMarker.MARKER_TYPES.values().length;
        MARKER_TYPES[] mts = PointMarker.MARKER_TYPES.values();
        Random numGen = new Random(System.currentTimeMillis());
        int marker = numGen.nextInt(numberMarkerTypes);
        while ( mts[marker] == PointMarker.MARKER_TYPES.NONE) {
            marker = numGen.nextInt(numberMarkerTypes);
        }
        return mts[marker];
    }
    
    public static PointMarker.CreatePointTypeRenderer getPointTypeRenderer() {
      return getInstance().new CreatePointTypeRenderer();
    }
    
    public static Shape getPointShape( MARKER_TYPES _type, 
                                       Point2D      _point, 
                                       double       _pointSize,
                                       double       _rotation) 
    {
        switch( _type ) {
          case NONE      : return new Area();
          case ALMOND    : return almond(_point.getX(),           _point.getY(),      _pointSize,     _rotation);
          case ARROWHEAD : return arrowHead(_point.getX(),        _point.getY(), (int)_pointSize,     _rotation);
          case ASTROID   : return astroid(_point.getX(),          _point.getY(),      _pointSize,     _rotation);
          case BOAT      : return boat(_point.getX(),             _point.getY(), (int)_pointSize,     _rotation);
          case ASTERISK  : return asterisk(_point.getX(),         _point.getY(), (int)_pointSize,     _rotation);
          case PLUS      : return cross(_point.getX(),            _point.getY(),      _pointSize,     _rotation);
          case CROSS     : return cross(_point.getX(),            _point.getY(),      _pointSize*1.21,_rotation + Math.PI/4);
          case DIAMOND   : return diamond(_point.getX(),          _point.getY(), (int)_pointSize,     _rotation);
          case DONUT     : return donut(_point.getX(),            _point.getY(),      _pointSize/ 2,  _pointSize/4 /* second parameter is inner radius*/ );
          case FISH      : return fish(_point.getX(),             _point.getY(), (int)_pointSize,     _rotation);
          case HASH      : return hash(_point.getX(),             _point.getY(), (int)_pointSize,     _rotation);
          case HEPTAGON  : return StrokedCircle(_point.getX(),    _point.getY(), (int)_pointSize, 7,  _rotation);
          case HEXAGON   : return StrokedCircle(_point.getX(),    _point.getY(), (int)_pointSize, 6,  _rotation);
          case HOURGLASS : return hourglass(_point.getX(),        _point.getY(),      _pointSize,     _rotation); 
          case OCTAGON   : return StrokedCircle(_point.getX(),    _point.getY(), (int)_pointSize, 8,  _rotation);
          case PENTAGON  : return StrokedCircle(_point.getX(),    _point.getY(), (int)_pointSize, 5,  _rotation);
          case PIN       : return pin(_point.getX(),              _point.getY(),      _pointSize,     _rotation);
          case REULEAUX  : return ReuleauxTriangle(_point.getX(), _point.getY(),      _pointSize,     _rotation);
          case SQUARE    : return square(_point.getX(),           _point.getY(),      _pointSize,     _rotation);
          case STAR      : return star(_point.getX(),             _point.getY(), (int)_pointSize,     _rotation);
          case TICK      : return tick(_point.getX(),             _point.getY(), (int)_pointSize,     _rotation);
          case TRIANGLE  : return triangle(_point.getX(),         _point.getY(),      _pointSize,     _rotation);        
          case CIRCLE    :
          default        : return circle(_point.getX(), _point.getY(),     _pointSize);
        }
    }

    public static String getMarkerTypeAsString(MARKER_TYPES _markerType) {
      return Strings.TitleCase(_markerType.toString().replace("_"," "));  
    }

    public static MARKER_TYPES getMarkerType(String _markerType) {
      return MARKER_TYPES.valueOf(_markerType.replace(" ","_").toUpperCase());  
    }
    
    public static javax.swing.DefaultComboBoxModel<String> getComboBoxModel() {
        return new javax.swing.DefaultComboBoxModel<String>(
                              new String[] { 
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.NONE)),  // Always has to be first
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.ALMOND)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.ARROWHEAD)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.ASTERISK)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.ASTROID)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.BOAT)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.CIRCLE)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.CROSS)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.DIAMOND)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.DONUT)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.FISH)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.HASH)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.HEPTAGON)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.HEXAGON)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.HOURGLASS)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.OCTAGON)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.PENTAGON)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.PIN)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.PLUS)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.REULEAUX)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.SQUARE)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.STAR)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.TICK)),
                                  Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.TRIANGLE)) } );
    }

    public static Shape diamond(Point2D _point, int _pointSize, double _angle) {
        return diamond(_point.getX(), _point.getY(), _pointSize, _angle);
    }
    
    public static Shape diamond(double _cx, double _cy, int _pointSize, double _angle) 
    {
        double halfLength  = _pointSize / 2.0f;
        double thirdLength = _pointSize / 3.0;
        GeneralPath p = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        p.moveTo(_cx              ,_cy + halfLength);
        p.lineTo(_cx + thirdLength,_cy);
        p.lineTo(_cx              ,_cy - halfLength);
        p.lineTo(_cx - thirdLength,_cy);
        p.closePath();
        return ( _angle == 0 ) 
             ? p
             : rotate(p, _angle, _cx, _cy);
    }
                          
    public static Shape arrowHead(Point2D _point, int _pointSize, double _angle) 
    { 
        return arrowHead(_point.getX(), _point.getY(), _pointSize, _angle);
    }

    /**
     * @method arrowHead
     * @param _cx
     * @param _cy
     * @param _pointSize
     * @return
     * @author Simon Greener, January 12th 2011 - Original coding
     */
    public static Shape arrowHead(double _endX, double _endY, int _pointSize, double _angle) 
    {
        int halfSize = _pointSize / 2;
        double cx = _endX, 
               cy = _endY + halfSize;
        GeneralPath p = new GeneralPath(GeneralPath.WIND_NON_ZERO); 
        p.moveTo(cx + (_pointSize * -0.50), cy + (_pointSize *  0.5));
        p.quadTo(cx + (_pointSize * -0.15), cy + (_pointSize *  0.2), 
                 cx + (_pointSize *  0.00), cy + (_pointSize * -0.5));
        p.quadTo(cx + (_pointSize *  0.15), cy + (_pointSize *  0.2), 
                 cx + (_pointSize *  0.50), cy + (_pointSize *  0.5));
        p.quadTo(cx + (_pointSize *  0.00), cy + (_pointSize *  0.0), 
                 cx + (_pointSize * -0.50), cy + (_pointSize *  0.5));
        p.closePath();
        return ( _angle == 0 ) 
             ? p
             : rotate(p,_angle, cx, cy - halfSize);
    }
  
    public static Shape tick(Point2D _point, int _pointSize, double _angle) 
    { 
        return tick(_point.getX(), _point.getY(), _pointSize, _angle);
    }
    
    /**
     * @method tick
     * @param _cx
     * @param _cy
     * @param _pointSize
     * @return
     * @author Simon Greener, May 28th 2010 - Original coding
     */
    public static Shape tick(double _cx, double _cy, int _pointSize, double _angle) 
    {
        int halfSize = _pointSize / 2;
        double x = _cx - halfSize,
               y = _cy - halfSize;
        GeneralPath p = new GeneralPath(GeneralPath.WIND_NON_ZERO); 
        p.moveTo(x + (halfSize * 0.22f), y + (halfSize));
        p.quadTo(x + (halfSize * 0.60f), y + (halfSize * 1.00f),  
                 x + (halfSize * 0.60f), y + (halfSize * 1.40f));  
        p.quadTo(x + (halfSize * 0.70f), y + (halfSize * 0.72f), 
                 x + (halfSize * 1.20f), y + (halfSize * 0.40f));
        p.quadTo(x + (halfSize * 0.80f), y + (halfSize * 0.80f), 
                 x + (halfSize * 0.60f), y + (halfSize * 1.40f));
        p.quadTo(x + (halfSize * 0.50f), y + (halfSize * 1.10f), 
                 x + (halfSize * 0.22f), y + (halfSize * 1.00f));
        p.closePath();
        return ( _angle == 0 ) 
             ? p
             : rotate(p,_angle, _cx, _cy);
    } 

    public static Shape boat(Point2D _point, int _pointSize, double _angle) 
    {
      return boat(_point.getX(), _point.getY(), _pointSize, _angle);
    }
    
    public static Shape boat(double _cx, double _cy, int _pointSize, double _angle) 
    { 
        float halfHeight = _pointSize / 2.0f;
        float oneThird   = _pointSize / 3.0f;
        float oneQuarter = _pointSize / 4.0f;
        float twoThirds  = _pointSize / 3.0f * 2.0f;
        GeneralPath boatGP = new GeneralPath(Path2D.WIND_NON_ZERO);
        // Bow
        boatGP.moveTo(_cx + halfHeight,                         _cy - halfHeight + twoThirds );
        // Front of cabin
        boatGP.lineTo(_cx + halfHeight - oneThird,              _cy - halfHeight + twoThirds );
        // Top of cabin top
        boatGP.lineTo(_cx + halfHeight - oneThird,              _cy - halfHeight + oneThird );
        // Back of cabin top
        boatGP.lineTo(_cx + halfHeight - oneThird - oneQuarter, _cy - halfHeight + oneThird );
        // Back of cabin deck
        boatGP.lineTo(_cx + halfHeight - oneThird - oneQuarter, _cy - halfHeight + twoThirds );
        // stern
        boatGP.lineTo(_cx - halfHeight,                         _cy - halfHeight + twoThirds );
        // Stern bottom hull
        boatGP.lineTo(_cx - halfHeight,                         _cy + halfHeight );
        // Bottom hull and bow
        boatGP.lineTo(_cx + oneQuarter,                         _cy + halfHeight );
        boatGP.closePath();
        Shape boat = new Area(boatGP); 
        double angle = _angle;
        return  ( angle == 0 ) 
             ? boat
             : rotate(boat,angle, _cx, _cy);     
    }
    
    private static final double BODY_WIDTH = .55, BODY_LENGTH = .75;
    private static final double TAIL_WIDTH = .5, TAIL_LENGTH = .4;
    private static final double EYE_SIZE = .14;  // 0.08

    public static Shape fish(Point2D _point, int _pointSize, double _angle) 
    {
      return fish(_point.getX(), _point.getY(), _pointSize, _angle);
    }
    
    
    public static Shape fish(double _cx, double _cy, int _pointSize, double _angle) 
    { 
       Shape fish;
       int pointSize = _pointSize; // * 2;
    
       double bodyWidth  = pointSize * BODY_WIDTH;
       double bodyLength = pointSize * BODY_LENGTH;
       double tailWidth  = pointSize * TAIL_WIDTH;
       double tailLength = pointSize * TAIL_LENGTH;
       double eyeSize    = pointSize * EYE_SIZE;

      // Build a set of paths for a fish facing North in a unit-length cell.
      float halfFishLength = (float) (bodyLength + tailLength / 3) / 2;

      // The fish body is an ellipse of the given body width and length.
      // The ellipse is horizontally centered and slightly above vertical
      // center (to leave room for tail).
      Area body = new Area(new Ellipse2D.Double(_cx - bodyWidth / 2,
                                                _cy - halfFishLength,
                                                bodyWidth,
                                                bodyLength) );

      // Now create an eye
      Area eye =  new Area( new Ellipse2D.Double(_cx - bodyWidth / 4,
                                                 _cy - halfFishLength + bodyLength / 4,
                                                 eyeSize, eyeSize) );
      // Remove it from body
      body.subtract(eye);

      // The fish tail is a triangle overlapping the end of body.
      GeneralPath tail = new GeneralPath();
      tail.moveTo(_cx - (float) tailWidth / 2, _cy + halfFishLength); 
      tail.lineTo(_cx,                         _cy + halfFishLength - (float) tailLength); // top of tail
      tail.lineTo(_cx + (float) tailWidth / 2, _cy + halfFishLength); // lower right
      tail.closePath();

      // Join body and tail together in one path.
      body.add(new Area(tail));
      fish = body;
      double angle = _angle + Math.PI/2.0; // we actually want fish pointing east not northing 
      return  ( angle == 0 ) 
           ? fish
           : rotate(fish,angle, _cx, _cy); 
    }
    
    public static Shape hash(Point2D _point, int _pointSize, double _angle) 
    {
      return hash(_point.getX(), _point.getY(), _pointSize, _angle);
    }
    
    public static Shape hash(double _cx, double _cy, int _pointSize, double _angle) 
    { 
        double third    = (double)_pointSize / 3.0;
        double twoThird = third + third;
        double x = _cx - ((double)_pointSize / 2.0), 
               y = _cy - ((double)_pointSize / 2.0);
        Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO);
        p.moveTo(x,  y + third   ); p.lineTo(x + _pointSize, y + third);
        p.moveTo(x,  y + twoThird); p.lineTo(x + _pointSize, y + twoThird);
        p.moveTo(x + third,     y); p.lineTo(x + third,      y + _pointSize);
        p.moveTo(x + twoThird,  y); p.lineTo(x + twoThird,   y + _pointSize);
        return ( _angle == 0 ) 
           ? p
           : rotate(p,_angle, _cx, _cy);
    }
    
    /**
     * @method cross
     * @param _point
     * @param _pointSize
     * @return
     * @author Simon Greener, May 28th 2010 - Original coding
     */
    public static Shape cross(Point2D _point, double _pointSize, double _angle) 
    {
      return cross(_point.getX(), _point.getY(), _pointSize, _angle);
    }
    
    public static Shape cross(double _cx, double _cy, double _pointSize, double _angle) 
    { 
        double halfLength = _pointSize / 2.0f;
        Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO);
        p.moveTo(_cx,              _cy - halfLength); 
        p.lineTo(_cx,              _cy + halfLength);
        p.moveTo(_cx - halfLength, _cy); 
        p.lineTo(_cx + halfLength, _cy);
        return ( _angle == 0 ) 
           ? p
           : rotate(p,_angle, _cx, _cy);
    }
      
    public static Shape asterisk(Point2D _point, int _pointSize, double _angle) 
    { 
        return star(_point.getX(), _point.getY(), _pointSize, _angle);
    }
    
    public static Shape asterisk(double _cx, double _cy, int _pointSize, double _angle) 
    {
        float halfSize = _pointSize / 2.0f;
        Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO); 
        p.moveTo(_cx + (-1 * halfSize), _cy + (-1 * halfSize));
        p.lineTo(_cx + (1 * halfSize), _cy + (1 * halfSize));
        p.moveTo(_cx + (-1 * halfSize), _cy + (1 * halfSize));
        p.lineTo(_cx + (1 * halfSize), _cy + (-1 * halfSize));
        p.moveTo(_cx + (-1.41 * halfSize), _cy + (0 * halfSize));
        p.lineTo(_cx + (1.41 * halfSize), _cy + (0 * halfSize));
        p.moveTo(_cx + (0 * halfSize), _cy + (-1.41 * halfSize));
        p.lineTo(_cx + (0 * halfSize), _cy + (1.41 * halfSize));
        return ( _angle == 0 ) 
             ? p
             : rotate(p,_angle, _cx, _cy);
    } 

    public static Shape star(Point2D _point, int _pointSize, double _angle) 
    { 
        return star(_point.getX(), _point.getY(), _pointSize, _angle);
    }
    
    /**
     * @method star
     * @param _cx
     * @param _cy
     * @param _pointSize
     * @return
     * @author Simon Greener, May 28th 2010 - Original coding
     */
    public static Shape star(double _cx, double _cy, int _pointSize, double _angle) 
    {
        float halfSize = _pointSize / 2.0f;
        Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO); 
        p.moveTo(_cx - halfSize,         _cy - halfSize / 4.0f); 
        p.lineTo(_cx + halfSize,         _cy - halfSize / 4.0f); 
        p.lineTo(_cx - halfSize / 2.0f,  _cy + halfSize); 
        p.lineTo(_cx,                    _cy - halfSize); 
        p.lineTo(_cx + halfSize / 2.0f,  _cy + halfSize); 
        p.closePath(); 
        return ( _angle == 0 ) 
             ? p
             : rotate(p,_angle, _cx, _cy);
    } 
  
    /**
     * @method circle
     * @param _point
     * @param _diameter
     * @return
     * @author Simon Greener, May 28th 2010 - Original coding
     */
     public static Shape circle(Point2D _point, double _diameter)
    {
        return circle(_point.getX(), _point.getY(), _diameter);
    }

    public static Shape circle(double _cx, double _cy, double _diameter) 
    { 
        double radius = _diameter / 2.0;
        return new Ellipse2D.Double(_cx - radius, 
                                    _cy - radius, 
                                    _diameter, 
                                    _diameter);
    }
  
    /**
     * @method square
     * @param _point
     * @param _sideLength
     * @return
     * @author Simon Greener, December 16th 2010 - Original coding
     */
     public static Shape square(Point2D _point, double _sideLength, double _angle) 
     { 
         return square(_point.getX(), _point.getY(), _sideLength, _angle);
     }     

     public static Shape square(double _cx, double _cy, double _sideLength, double _angle)
     {
         return rectangle(_cx, _cy, _sideLength, _sideLength, _angle);
     }

    public static Shape rectangle(double _cx, double _cy, double _breadth, double _depth, double _angle)
    {
        return ( _angle == 0 ) 
               ? new Rectangle2D.Double(_cx - _breadth / 2f, 
                                        _cy - _depth / 2f, 
                                        _breadth, 
                                        _depth)
               : rotate(new Rectangle2D.Double(_cx - _breadth / 2f, 
                                               _cy - _depth / 2f, 
                                               _breadth, 
                                               _depth),
                        _angle, _cx, _cy);
    }
  
    public static Shape triangle(Point2D _point, double _pointSize, double _angle) 
    { 
        return triangle(_point.getX(), _point.getY(), _pointSize, _angle);
    }

    public static Shape triangle(double _cx, double _cy, double _pointSize, double _angle)
    {
       double halfSide = _pointSize / 2.0f ; 
       Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO);
       // bottom left
       p.moveTo(_cx - halfSide, _cy + halfSide);
       // top middle
       p.lineTo(_cx,            _cy - halfSide);
       // bottom right
       p.lineTo(_cx + halfSide, _cy + halfSide); 
       p.closePath();
       return ( _angle == 0 ) 
           ? p
           : rotate(p, _angle, _cx, _cy);
  }

    public static Shape hourglass( Point2D _point, double _pointSize, double _angle ) {
       return hourglass(_point.getX(), _point.getY(), _pointSize, _angle );
    }

    public static Shape hourglass(double _cx,        double _cy,
                                  double _pointSize, double _angle ) 
    {
      Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO); 
      p.moveTo(_cx,                      _cy);
      p.lineTo(_cx - _pointSize / 2.0,   _cy - _pointSize / 2.0f); 
      p.lineTo(_cx + _pointSize / 2.0f,  _cy - _pointSize / 2.0f); 
      p.lineTo(_cx - _pointSize / 2.0f,  _cy + _pointSize / 2.0f); 
      p.lineTo(_cx + _pointSize / 2.0f,  _cy + _pointSize / 2.0f);      
      p.closePath(); 
      return ( _angle == 0 ) 
           ? p
           : rotate(p,_angle, _cx, _cy);        
    }

    public static Shape pin( Point2D _point, double _pointSize, double _angle ) {
       return pin( _point.getX(), _point.getY(), _pointSize / 3, _pointSize, _angle );
    }

    public static Shape pin( double _cx, double _cy, double _pointSize, double _angle ) 
    {
       return pin( _cx, _cy, _pointSize / 3, _pointSize, _angle );
    }

    public static Shape pin(double _cx,     double _cy,
                            double _radius, double _height,
                            double _angle ) 
    {
        // All angles passed in are radians as any degree values are converted to radians
        double angle = COGO.degrees(_angle);  
        Arc2D.Double head = new Arc2D.Double( _cx - (_radius * 1), _cy - (_radius * 1), _radius * 2, _radius * 2, 0, 181, Arc2D.PIE );
        GeneralPath body = new GeneralPath();
        body.moveTo( _cx - _radius, _cy );
        body.lineTo( _cx, _cy + _height );
        body.lineTo( _cx + _radius, _cy );
        body.closePath();
        Shape pin = new Area( head );
        ((Area) pin).add( new Area( body ) );
        return ( _angle != 0  
               ? rotate(translate(pin, 0, 0 - _height), angle, _cx, _cy )
               : translate(pin, 0, 0 - _height) );
    }

    public static Shape donut( Point2D _point, double _oRadius, double _iRadius ) {
       return donut( _point.getX(), _point.getY(), _oRadius, _iRadius );
    }
    
    public static Shape donut( double _cx, double _cy, double _oRadius, double _iRadius) 
    {
      Shape innerShape = null;
      Shape outerShape = null;
      Shape donut      = null;
      int iRadius = (int)_iRadius;
      if ( _iRadius == 0 || _iRadius < ( _oRadius / 2 ) )
          iRadius = (int)_oRadius / 2;
      int oRadius = (int)_oRadius; 
      if ( _oRadius <= _iRadius )
          oRadius = (int)_iRadius + 1;
      outerShape = new Ellipse2D.Double( (_cx - _oRadius), (_cy - oRadius), (oRadius * 2), (oRadius * 2) );
      innerShape = new Ellipse2D.Double( (_cx - _iRadius), (_cy - iRadius), (iRadius * 2), (iRadius * 2) );
      donut = new Area( outerShape );
      ((Area) donut).subtract( new Area( innerShape ) );
      return donut;
    }

    public static Shape astroid(Point2D _point, double _radius, double _angle ) {
       return astroid( _point.getX(), _point.getY(), _radius, _angle );
    }
  
    public static Shape astroid( double _cx, double _cy, double _radius, double _angle ) 
    {
        double halfRadius = _radius / 2.0f;
       double r2 = halfRadius * 2;
       Shape astroid = new Area( new Rectangle2D.Double( _cx - halfRadius, _cy - halfRadius, r2, r2 ) );
       ((Area) astroid).subtract( new Area( new Ellipse2D.Double( _cx - r2, _cy - r2,  r2, r2 ) ) );
       ((Area) astroid).subtract( new Area( new Ellipse2D.Double( _cx - r2, _cy,       r2, r2 ) ) );
       ((Area) astroid).subtract( new Area( new Ellipse2D.Double( _cx,      _cy,       r2, r2 ) ) );
       ((Area) astroid).subtract( new Area( new Ellipse2D.Double( _cx,      _cy - r2,  r2, r2 ) ) );
        return ( _angle == 0 ) 
           ? astroid
           : rotate(astroid,_angle, _cx, _cy);
    }

    public static Shape almond(Point2D _point, double _pointSize, double _angle ) {
      return almond( _point.getX(), _point.getY(), _pointSize, _angle );
    }
  
    public static Shape almond( double _cx, double _cy, double _pointSize, double _angle )
    {
        double halfHeight = _pointSize / 2.0;
        double widthHeight = halfHeight * 1.5;
        Shape almond =             new Area( new Ellipse2D.Double( _cx - halfHeight / 2, _cy - halfHeight, widthHeight, widthHeight ) );
        ((Area) almond).intersect( new Area( new Ellipse2D.Double( _cx - halfHeight,     _cy - halfHeight, widthHeight, widthHeight ) ) );
        return rotate( almond, _angle, _cx, _cy );
    }

    public static Shape ReuleauxTriangle(Point2D _point, double _pointSize, double _angle ) {
      return ReuleauxTriangle( _point.getX(), _point.getY(), _pointSize, _angle );
    }
  
    public static Shape ReuleauxTriangle(double _cx, double _cy, double _pointSize, double _angle)
    {
        double pointSize = _pointSize * 0.9;  // Hack to make roughly same as other markers
        double[] p1 = {_cx, _cy};
        double[] p2 = {_cx + pointSize, _cy};
        double[] p3 = {_cx + (pointSize / 2), _cy - (float) Math.abs( Math.sqrt( 3 ) / 2 * pointSize )};        
        double doublePointSize = pointSize * 2.0;
        double twoThirdPointSize = ( 2.0 * pointSize) / 3.0;
        double position = (pointSize + (pointSize / 2.0));
        Ellipse2D c1 = new Ellipse2D.Double(p1[0] - position, p1[1] - twoThirdPointSize, doublePointSize, doublePointSize );
        Ellipse2D c2 = new Ellipse2D.Double(p2[0] - position, p2[1] - twoThirdPointSize, doublePointSize, doublePointSize );
        Ellipse2D c3 = new Ellipse2D.Double(p3[0] - position, p3[1] - twoThirdPointSize, doublePointSize, doublePointSize);
        Shape triangle = new Area(c1);
        ((Area)triangle).intersect( new Area(c2) );
        ((Area)triangle).intersect( new Area(c3) );
        return ( _angle == 0 )
            ? triangle
            : rotate(triangle, _angle, _cx, _cy);
    }

    public static Shape StrokedCircle(Point2D _point, int _pointSize, int _vertexCount, double _startAngle) 
    {
        return StrokedCircle((int)_point.getX(),(int)_point.getY(),_pointSize,_vertexCount,_startAngle);
    }
    
    public static Shape StrokedCircle(double _x, double _y, int _pointSize, int _vertexCount) 
    {
        int pointSize = ( _pointSize <= 0 ) ? 1 : _pointSize;
        int vCount = ( _vertexCount < 3 ) ? 3 : _vertexCount;
        return StrokedCircle(_x, _y, pointSize, vCount, 0);
    }
      
    public static Shape StrokedCircle(double _x, double _y, int _pointSize, int _vertexCount, double _startAngle) 
    {
        if ( _vertexCount < 3 )
            return triangle(_x,_y,_pointSize/2.0,_startAngle);
        float radius = _pointSize / 2.0f;
        int xArray[]=new int[_vertexCount];
        int yArray[]=new int[_vertexCount];
        double addAngle=2.0*Math.PI/(double)_vertexCount;
        double angle=_startAngle + (_vertexCount%2==1?3.0*addAngle/4.0:0.0); // Latter clause makes things like Pentagons have a vertex centre top
        for (int i=0; i<_vertexCount; i++) {
            xArray[i]=(int)Math.round(radius*Math.cos(angle)) + (int)_x;
            yArray[i]=(int)Math.round(radius*Math.sin(angle)) + (int)_y;
            angle+=addAngle;
        }
        return new Polygon(xArray,yArray,_vertexCount);
    }

    public static Shape rotate( Shape shape, double angle, double x, double y ){
       if( angle == 0 ){
          return shape;
       }
       return AffineTransform.getRotateInstance( angle, x, y ).createTransformedShape(shape );
    }

    public static Shape translate( Shape _shape, double _tx, double _ty ) {
       if( _tx == 0 && _ty == 0) {
          return _shape;
       }
       return AffineTransform.getTranslateInstance( _tx, _ty ).createTransformedShape(_shape);
    }

    public static Shape crossAsArea(double _cx, double _cy, int _pointSize) 
    { 
        Area cross    = new Area(new Rectangle2D.Double( _cx-Math.round((float)_pointSize/2f), _cy-1, 
                                                         _pointSize, 1));
        Area vertical = new Area(new Rectangle2D.Double( _cx-1,              _cy-Math.round((float)_pointSize/2f),
                                                         1, _pointSize)); 
        cross.add(vertical);
        return cross; 
    }

    public static Icon getGeometryTypeIcon(Constants.GEOMETRY_TYPES _geometryType,
                                           int                      _iconWidth,
                                           int                      _iconHeight,
                                           Color                    _fillColor,
                                           Color                    _drawColor,
                                           ImageIcon                _unknown) 
    {
        int cx = _iconWidth / 2;
        int cy = _iconHeight / 2;
        int radius = Math.min(_iconWidth,_iconHeight) / 2;
        radius = radius > 2 ? radius - 1 : radius;
        Shape shape = null;
        // Generate shape
        //
        switch( _geometryType ) {
          case POINT        : shape = cross(cx, cy, radius, 0);
                              break;
          case MULTIPOINT   : float radiusF = (float)Math.min(_iconWidth,_iconHeight) / 8f;
                              shape =                    crossAsArea( Math.round((float)_iconWidth/3f),       Math.round((float)_iconHeight/3f),       Math.round(radiusF*2f));
                              ((Area)shape).add(new Area(crossAsArea( Math.round((float)_iconWidth*2f/3f+1f), Math.round((float)_iconHeight/2f-1f),    Math.round(radiusF*2f))));
                              ((Area)shape).add(new Area(crossAsArea( Math.round((float)_iconWidth/2f-1f),    Math.round((float)_iconHeight*2f/3f+1f), Math.round(radiusF*2f))));
                              break;
          case LINE         : shape = new Path2D.Float();
                              ((Path2D)shape).moveTo(                 0, _iconHeight);
                              ((Path2D)shape).lineTo(    _iconWidth / 3,           0);
                              ((Path2D)shape).moveTo(    _iconWidth / 3,           0);
                              ((Path2D)shape).lineTo(_iconWidth * 2 / 3, _iconHeight);
                              ((Path2D)shape).moveTo(_iconWidth * 2 / 3, _iconHeight);
                              ((Path2D)shape).lineTo(        _iconWidth,           0);
                              break;
          case MULTILINE    : shape = new Path2D.Float();
                              ((Path2D)shape).moveTo(         0, _iconHeight);  // from bottom left to ...
                              ((Path2D)shape).lineTo(        cx,           0);  // 1/2 along top boundary.
                              ((Path2D)shape).moveTo(        cx,          cy);  // from middle to ...
                              ((Path2D)shape).lineTo(_iconWidth,  _iconHeight);  // bottom right
                              break;
          case SOLID        :
          case POLYGON      : shape = StrokedCircle(cx,cy,radius,5); 
                              break;
          case MULTISOLID   :
          case MULTIPOLYGON : radius = Math.min(_iconWidth,_iconHeight) / 4;
                              shape  =          new Area(StrokedCircle(cx/3,cy/3,radius,5)); 
                              ((Area)shape).add(new Area(       square(_iconWidth*2/3,   _iconHeight/2-1, radius,0)));
                              ((Area)shape).add(new Area(       circle(  _iconWidth/2-1, _iconHeight*2/3, radius)));
                              break;
          case COLLECTION   : radius = Math.min(_iconWidth,_iconHeight) / 4;
                              shape  =          new Area(StrokedCircle(          cx/3,            cy/3, radius,5)); 
                              ((Area)shape).add(new Area(    rectangle(_iconWidth*2/3,   _iconHeight/2, _iconWidth/4, 1, 0)));  // depth of 1 == line
                              ((Area)shape).add(new Area(       circle(  _iconWidth/3, _iconHeight*2/3, Math.abs(radius - 2))));
                              break;
          case UNKNOWN      : 
          default           : break;
        } 
        return shape==null 
               ? _unknown
               : Tools.createIcon(LineStyle.getStroke(LineStyle.LINE_STROKES.LINE_SOLID,2),
                                  _iconWidth,_iconHeight,
                                  shape,
                                  _fillColor, _drawColor);
    }
    
    class CreatePointTypeRenderer extends DefaultListCellRenderer 
    {
        
		private static final long serialVersionUID = 8575394913618667131L;

		public CreatePointTypeRenderer() {
        }
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, 
                                                      Object value, 
                                                      int index,
                                                      boolean isSelected, 
                                                      boolean cellHasFocus)
        {
            int iconWidth = 20;
            int iconHeight = 20;
            int markerPointSize = 8;
            Point2D point = new Point2D.Double((double)iconWidth/2.0f,(double)iconHeight/2.0f);
            // Get icon's string for the list item value
            String s = (String)value;
            JLabel label = new JLabel(s,SwingConstants.LEFT);
            BasicStroke solidLine = LineStyle.getStroke( LineStyle.LINE_STROKES.LINE_SOLID, 1);
            if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.NONE))) ) {
              label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                             PointMarker.getPointShape(MARKER_TYPES.NONE, point, markerPointSize, 0),
                                             null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.ALMOND))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.ALMOND, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.ARROWHEAD))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.ARROWHEAD, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.ASTROID))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.ASTROID, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.ASTERISK))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.ASTERISK, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.BOAT))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.BOAT, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.CIRCLE))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.CIRCLE, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.CROSS))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.CROSS, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.DIAMOND))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.DIAMOND, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.PLUS))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.PLUS, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.FISH))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.FISH, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.DONUT))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.DONUT, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.HASH))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.HASH, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.REULEAUX))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.REULEAUX, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.SQUARE))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.SQUARE, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.STAR))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.STAR, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.HOURGLASS))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.HOURGLASS, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.PENTAGON))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.PENTAGON, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.HEXAGON))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.HEXAGON, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.HEPTAGON))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.HEPTAGON, point, markerPointSize, 0),
                                               null, null));
            } else if ( s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.OCTAGON))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.OCTAGON, point, markerPointSize, 0),
                                               null, null));
            } else if (  s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.PIN))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.PIN, 
                                                                         new Point2D.Double(iconWidth/2, iconHeight - ( iconHeight / 4)), markerPointSize, 0),null, null));
            } else if (  s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.TICK))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.TICK, point, markerPointSize, 0),
                                                                         null, null));
            } else if (  s.equals(Strings.TitleCase(getMarkerTypeAsString(MARKER_TYPES.TRIANGLE))) ) {
                label.setIcon(Tools.createIcon(solidLine, iconWidth, iconHeight,
                                               PointMarker.getPointShape(MARKER_TYPES.TRIANGLE, point, markerPointSize, 0),
                                               null, null));
            }
            return label;
        }
    }
}