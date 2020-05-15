package org.GeoRaptor.SpatialView.SupportClasses;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.io.IOException;
import java.io.StringReader;

import java.math.BigDecimal;

import java.text.DecimalFormat;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import oracle.spatial.geometry.JGeometry;

import org.GeoRaptor.Constants;
import org.GeoRaptor.tools.Coordinate;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class Envelope 
implements Comparable<Object>
{
    // private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.SpatialView.SupportClasses.Envelope");

    public double minX, 
                  minY, 
                  maxX, 
                  maxY;
    protected int decimalPlaces = 8;
   
    public Envelope(int _precision) {
        this.decimalPlaces = _precision;
        setNull();
    }

    /**
     *  Initialize a <code>Envelope</code> ensuring LL and UR are correct
     *
     *@param  _x1  the first x-value
     *@param  _x2  the second x-value
     *@param  _y1  the first y-value
     *@param  _y2  the second y-value
     */
    public void init(double _x1, double _y1, double _x2, double _y2)
    {
        if (_x1 < _x2) {
            this.minX = new BigDecimal(_x1).setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
            this.maxX = new BigDecimal(_x2).setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
        } else {
            this.minX = new BigDecimal(_x2).setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
            this.maxX = new BigDecimal(_x1).setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
        } 
        if (_y1 < _y2) {
            this.minY = new BigDecimal(_y1).setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
            this.maxY = new BigDecimal(_y2).setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
        } else {
            this.minY = new BigDecimal(_y2).setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
            this.maxY = new BigDecimal(_y1).setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
    }

    public Envelope(double _minX, double _minY,
                           double _maxX, double _maxY) {
        this.init(_minX,_minY,_maxX,_maxY);
    }

    public Envelope(int _precision,
                           double _minX, double _minY,
                           double _maxX, double _maxY) {
        this.decimalPlaces = _precision;
        this.init(_minX,_minY,_maxX,_maxY);
    }

    public Envelope(Point2D _min, 
                           Point2D _max) {
        this.init(_min.getX(),_min.getY(),_max.getX(),_max.getY());
    }

    public Envelope(Envelope _mbr) {
        if (_mbr == null) {
            this.setNull();
        }
        else if ( _mbr.isSet() ) {
            this.decimalPlaces = _mbr.decimalPlaces;
            this.init(_mbr.minX,_mbr.minY,_mbr.maxX,_mbr.maxY);
        }
    }

    public Envelope(double _originX, 
                           double _originY,
                           Dimension _dimension) {
        this.init(_originX,
                  _originY,
                  _originX + _dimension.getWidth(),
                  _originY + _dimension.getHeight());
    }

    public Envelope(Point2D _centre, 
                           double _width,
                           double _height) {
        this.init(_centre.getX() - _width,
                  _centre.getY() - _height,
                  _centre.getX() + _width,
                  _centre.getY() + _height);
    }
    
    public Envelope(List<QueryRow> _geoSet) {
        this.setNull();
        if (_geoSet == null) {
            return;
        } else  {
            for (QueryRow qrow : _geoSet) {
                if (qrow.getGeoValue() == null)
                    continue;
                this.setMaxMBR(SDO_GEOMETRY.getGeoMBR(qrow.getGeoValue()));
            }
        }
    }
    
    public Envelope(JGeometry _geo) 
    {
        this.setNull();
        if (_geo == null) {
            return ;
        }
                
        // getMBR returns:
        // a double array containing the minX,minY, maxX,maxY value of the MBR for 2D or
        // a double array containing the minX,minY,minZ maxX,maxY, maxZ value of the MBR for 3D
        //
        int dims = _geo.getDimensions();
        if ( _geo.isOrientedPoint() || _geo.isOrientedMultiPoint() ) {
            this.setMBR(SDO_GEOMETRY.getOrientedPointMBR(_geo));
        } else {
            // getMBR() doesn't seem to return measures in MBR if of type 3302 etc
            //
            if ( _geo.getLRMDimension()==dims )
                dims = 2;
            double[] mbr = null;
            try {
                // getMBR() Gets the MBR of this geometry. When a JSDOGeoemtry is first instantiated from a 
                // db geometry STRUCT, no MBR is computed. The MBR exists only after the first call to this method. 
                // The MBR will be recalcuated only when the geoemtry's structure has been modified.
                //
                mbr = _geo.getMBR();
            } catch (Exception e) {
                try {
                  mbr = _geo.getMBR();
                } catch (Exception e2) {
                    return; 
                }
            }
            if ( mbr == null ) {
                return;
            }
            if ( mbr.length < 4 || Double.isInfinite(mbr[0]) || Double.isNaN(mbr[0]) ) {
                this.setMBR(SDO_GEOMETRY.getOrdinatesMBR(_geo));
            }
            if ( mbr.length < 4 ) {
                return;
            }
            switch ( dims ) {
                case 4  : init(mbr[0], mbr[1], mbr[2], mbr[3]);
                case 3  : init(mbr[0], mbr[1], mbr[3], mbr[4]);
                case 2  : 
                default : init(mbr[0], mbr[1], mbr[2], mbr[3]); 
            }
        }

    }
    
    public Rectangle2D.Double getRectangle2D() {
        return new java.awt.geom.Rectangle2D.Double(this.getMinX(), this.getMinY(), this.getWidth(), this.getHeight());
    }

    /**
     *  Returns <code>true</code> if this <code>Envelope</code> is a "null"
     *  Envelope.
     *
     * @return <code>true</code> if this <code>Envelope</code> is uninitialized
     *               or is the Envelope of the empty geometry.
     * @version 1.0
     * @author Simon Greener
     */
    public boolean isNull() {
      return ((this.minX == Double.MAX_VALUE) && 
              (this.minY == Double.MAX_VALUE) &&
              (this.maxX == Double.MIN_VALUE) && 
              (this.maxY == Double.MIN_VALUE) );
    }

    public boolean isInvalid() {
        return this.isNull() ||
               Double.isInfinite(this.minX) || 
               Double.isInfinite(this.minY) ||
               Double.isInfinite(this.maxX) || 
               Double.isInfinite(this.maxY) ||
               Double.isNaN(this.minX) || 
               Double.isNaN(this.minY) ||
               Double.isNaN(this.maxX) || 
               Double.isNaN(this.maxY); 

    }

    /** isEmpty. Checks if MBR has decayed to being zero sided.
     * @return
     */
    public boolean isEmpty() {
        return (this.getWidth()==0 && this.getHeight()==0 );
    }
    
    /**
     * Check if MBR is set
     * @history: SGG - Changed 23/12/2015
     */
    public boolean isSet() 
    {
        return ! (this.isNull() || this.isInvalid());
    }
        
    /**
     *  Returns the Envelope's minimum x-value. 
     *  min x > max x 
     *  indicates that this is a null Envelope.
     *
     *@return    the minimum x-coordinate
     */
    public double getMinX() {
      return this.minX;
    }

    /**
     *  Returns the Envelopes maximum x-value. 
     *  min x > max x
     *  indicates that this is a null Envelope.
     *
     *@return    the maximum x-coordinate
     */
    public double getMaxX() {
      return this.maxX;
    }

    /**
     *  Returns the Envelopes minimum y-value. 
     *  min y > max y
     *  indicates that this is a null Envelope.
     *
     *@return    the minimum y-coordinate
     */
    public double getMinY() {
      return this.minY;
    }

    /**
     *  Returns the Envelopes maximum y-value. 
     *  min y > max y
     *  indicates that this is a null Envelope.
     *
     *@return    the maximum y-coordinate
     */
    public double getMaxY() {
      return this.maxY;
    }

    /**
     *  Returns the width of the X side. 
     *
     *@return  width
     */
    public double getWidth() {
        return this.maxX - this.minX;
    }
    
    /**
     *  Returns the height of the Y side. 
     *
     *@return  height
     */
    public double getHeight() {
        return this.maxY - this.minY;
    }

    public double area() {
        return this.getWidth() * this.getHeight();
    }

    public double length() {
        return (this.getWidth()*2.0) + (this.getHeight()*2.0);
    }

    /**
     * Gets the minimum extent of this envelope across both dimensions.
     * 
     * @return the minimum extent of this envelope
     */
    public double minExtent()
    {
        if (this.isNull()) return 0.0;
        
        double w = getWidth();
        double h = getHeight();
        if (w < h) {
            return w;
        } else {
            return h;
        }
    }

    /**
     * Gets the maximum extent of this envelope across both dimensions.
     * 
     * @return the maximum extent of this envelope
     */
    public double maxExtent()
    {
        if (isNull()) return 0.0;
        
        double w = getWidth();
        double h = getHeight();
        if (w > h) {
            return w;
        } else {
            return h;
        }
    }

    /**
     * @function setNull
     * @precis Sets inverted rectangle
     * @author Simon Greener, June 3rd 2010
     **/          
    public void setNull() {
        // set default MBR
        this.minX = Double.MAX_VALUE;
        this.minY = Double.MAX_VALUE;
        this.maxX = Double.MIN_VALUE;
        this.maxY = Double.MIN_VALUE;        
    }
    
    /**
     * @function setMBR
     * @precis Setter based on another Envelope
     * @param _mbr (Envelope)
     * @author Simon Greener, May 2010
     **/          
    public void setMBR(Envelope _mbr ) {
        if ( _mbr == null ) return;
        this.minX = _mbr.minX;
        this.minY = _mbr.minY;
        this.maxX = _mbr.maxX;
        this.maxY = _mbr.maxY;
    }

    /**
     * @function setMBR
     * @precis Alternate setter based on four doubles
     * @param _minX
     * @param _minY
     * @param _maxX
     * @param _maxY
     * @author Simon Greener, May 2010
     *          "Constructor" based on four doubles
     */
    public void setMBR(double _minX, double _minY, 
                       double _maxX, double _maxY) {
        this.minX = _minX;
        this.minY = _minY;
        this.maxX = _maxX;
        this.maxY = _maxY;
    }

    /**
     * @function setMBR()
     * @precis Setter based on output of JGeometry MBR function
     * @param _corrs
     */
    public void setMBR(int _dims, double _corrs[]) 
    {
        this.minX = _corrs[0];
        this.minY = _corrs[1];
        switch ( _dims ) {
            case 2 : { this.maxX = _corrs[2]; this.maxY = _corrs[3]; break; }
            case 3 : { this.maxX = _corrs[3]; this.maxY = _corrs[4]; break; }
            case 4 : { this.maxX = _corrs[4]; this.maxY = _corrs[5]; break; }
        }
    }
    
    /**
     * @function setMaxMBR
     * @precis Sets Envelope to maximum of supplied and existing MBR values
     * @param _mbr
     */
    public void setMaxMBR(Envelope _mbr ) {
        if ( _mbr == null ) return;
        setMaxMBR(_mbr.minX,
                  _mbr.minY,
                  _mbr.maxX,
                  _mbr.maxY);
    }

    public void setMaxMBR(double _corrs[]) {
        setMaxMBR(_corrs[0],_corrs[1],_corrs[2],_corrs[3]);
    }

    public void expandToInclude(double x, double y) {
      setMaxMBR(x,x,y,y);
    }
    
    /**
     * @function setMaxMBR
     * @precis Sets Envelope to maximum of supplied and existing MBR values
     * @param _minX
     * @param _minY
     * @param _maxX
     * @param _maxY
     */
    public void setMaxMBR(double _minX, double _minY, 
                          double _maxX, double _maxY) 
    {
        Envelope other = new Envelope(_minX, _minY, _maxX, _maxY);
        if (other.isNull()) return;
        if (this.isNull()) {
            this.minX = other.getMinX();
            this.maxX = other.getMaxX();
            this.minY = other.getMinY();
            this.maxY = other.getMaxY();
        } else {
            if (other.minX < this.minX) this.minX = other.minX;
            if (other.maxX > this.maxX) this.maxX = other.maxX;
            if (other.minY < this.minY) this.minY = other.minY;
            if (other.maxY > this.maxY) this.maxY = other.maxY;
        }
    }

    /**
     * Computes the coordinate of the centre of this envelope (as long as it is non-null
     *
     * @return the centre coordinate of this envelope
     * <code>null</code> if the envelope is null
     */
    public Point2D centre() {
        if (this.isNull()) {
          return null;
        }
        return new Point.Double(((this.maxX + this.minX) / 2.0f),
                                ((this.maxY + this.minY) / 2.0f));
    }

    public Coordinate center() {
        if (this.isNull()) {
          return null;
        }
        return new Coordinate(((this.maxX + this.minX) / 2.0f),
                              ((this.maxY + this.minY) / 2.0f));
    }

    public boolean Normalize(Rectangle _rectangle) 
    {
        if ( this.isNull() ) {
            return false;
        }
        
        // If it is already normalized, don't bother doing it again otherwise we will get MBR drift
        //
        if ( (this.getWidth() / this.getHeight()) == (_rectangle.getWidth() / _rectangle.getHeight()) ) {
            return false;
        }

        double center, delta;
        if ( (this.getWidth() / this.getHeight()) > (_rectangle.getWidth() / _rectangle.getHeight()) )
        {
            center = (this.maxY + this.minY) / 2;
            delta  = this.getWidth() / (long)_rectangle.getWidth() * (long)_rectangle.getHeight();
            this.minY = center - ( delta / 2f );
            this.maxY = center + ( delta / 2f );
        } else {
            center = (this.maxX + this.minX) / 2;
            delta = this.getHeight() / (long)_rectangle.getHeight() * (long)_rectangle.getWidth();
            this.minX = center - ( delta / 2f );
            this.maxX = center + ( delta / 2f );
        } // if - else
        return true;
    }

    public boolean Normalize(Dimension _component) 
    {
        return this.Normalize(new Rectangle(_component));
    }

    /**
     * Move current rectangle MBR centred on new position
     * @param _point center coordinate
     */
    public void moveTo(Point2D _point) 
    {
        if ( this.isNull() ) {
            return;
        }
        double xDif = _point.getX() - this.centre().getX();
        double yDif = _point.getY() - this.centre().getY();
        // set new MBR
        this.minX += xDif;
        this.maxX += xDif;
        this.minY += yDif;
        this.maxY += yDif;        
    }
    
    /**
     * Translates this Envelope by given amounts in the X and Y direction.
     *
     * @param _delta the amount to translate 
     **/
    public void translate(Point2D _delta) {
        if (this.isNull())
          return;
        this.minX += _delta.getX();
        this.maxX += _delta.getX();
        this.minY += _delta.getY();
        this.maxY += _delta.getY();   
    }
        
    /**
     * Calculate distance between MIN and MAX value
     */
    protected double calcDistance(double _min, double _max) {
             if (_min == 0) return _max / 2;
        else if (_max == 0) return _min / 2;
        else if (Math.abs(_min) >= Math.abs(_max)) return (_min - _max) / 2;
        else return (-_min + _max) / 2;
    }
    
    /**
     * Computes the distance between this and another
     * <code>Envelope</code>.
     * The distance between overlapping Envelopes is 0.  Otherwise, the
     * distance is the Euclidean distance between the closest points.
     */
    public double distance(Envelope env)
    {
      if (intersects(env)) return 0;
      
      double dx = 0.0;
      if (this.maxX < env.minX) {
        dx = env.minX - this.maxX;
      } else if (this.minX > env.maxX) {
        dx = this.minX - env.maxX;
      }
      
      double dy = 0.0;
      if (this.maxY < env.minY) 
        dy = env.minY - this.maxY;
      else if (this.minY > env.maxY) { 
          dy = this.minY - env.maxY;
      }

      // if either is zero, the envelopes overlap either vertically or horizontally
      if (dx == 0.0) return dy;
      if (dy == 0.0) return dx;
      return Math.sqrt(dx * dx + dy * dy);
    }
    
    public void setIncreaseByPercent(int _percentage) {
        // add _percentage around MBR
        // If _percent is 
        double diffX = (this.maxX - this.minX) * ( (double)Math.abs(_percentage) / 100f );
        double diffY = (this.maxY - this.minY) * ( (double)Math.abs(_percentage) / 100f );
        this.minX -= diffX;
        this.minY -= diffY;
        this.maxX += diffX;
        this.maxY += diffY;
    }
    
    public Envelope increaseByPercent(int _percentage) {
        // add _percentage around MBR
        double diffX = (this.maxX - this.minX) * ( (double)Math.abs(_percentage) / 100f );
        double diffY = (this.maxY - this.minY) * ( (double)Math.abs(_percentage) / 100f );
        return new Envelope( this.minX - diffX,
                                    this.minY - diffY,
                                    this.maxX + diffX,
                                    this.maxY + diffY );
    }

    public void setPrecision(int _precision) {
        this.decimalPlaces = _precision;
    }
    public int getPrecision() {
        return this.decimalPlaces;
    }

    public void setChange(double _change) {
        // add/substract value in mbr units to MBR
        double diffX = (this.maxX - this.minX) + _change;
        double diffY = (this.maxY - this.minY) + _change;
        this.minX -= diffX;
        this.minY -= diffY;
        this.maxX += diffX;
        this.maxY += diffY;
    }
    
    public Envelope change(double _change) {
        // add/subtract _change around MBR
        double diffX = (this.maxX - this.minX) + _change;
        double diffY = (this.maxY - this.minY) + _change;
        return new Envelope( this.minX - diffX,
                                    this.minY - diffY,
                                    this.maxX + diffX,
                                    this.maxY + diffY );
    }

    @Override
    public int compareTo(Object o) {
      Envelope env = (Envelope) o;
      // compare nulls if present
      if (this.isNull()) {
        if (env.isNull()) return 0;
        return -1;
      }
      else {
        if (env.isNull()) return 1;
      }
      // compare based on numerical ordering of ordinates
      if (this.minX < env.minX) return -1;
      if (this.minX > env.minX) return 1;
      if (this.minY < env.minY) return -1;
      if (this.minY > env.minY) return 1;
      if (this.maxX < env.maxX) return -1;
      if (this.maxX > env.maxX) return 1;
      if (this.maxY < env.maxY) return -1;
      if (this.maxY > env.maxY) return 1;
      return 0;
    }
    
    /**
     *  Tests to see if an <code>Envelope</code> is equal to an <code>other</code>.
     *
     * @param  _other the <code>Envelope</code> which this <code>Envelope</code> is
     *      being checked for equality
     * @return <code>true</code> if the <code>other</code> is equal to 
     *         <code>Envelope</code>.
     * @version 1.0
     * @author Simon Greener
     */
    public boolean equals(Envelope _other) {
        if (this.isNull()) {
            return _other.isNull();
        }
        BigDecimal tMinX = new BigDecimal(  this.minX); 
        BigDecimal oMinX = new BigDecimal(_other.minX);
        BigDecimal tMinY = new BigDecimal(  this.minY); 
        BigDecimal oMinY = new BigDecimal(_other.minY);
        BigDecimal tMaxX = new BigDecimal(  this.maxX); 
        BigDecimal oMaxX = new BigDecimal(_other.maxX);
        BigDecimal tMaxY = new BigDecimal(  this.maxY); 
        BigDecimal oMaxY = new BigDecimal(_other.maxY);
        return (((tMinX.setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP)).compareTo(oMinX.setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP))==0) &&
                ((tMinY.setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP)).compareTo(oMinY.setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP))==0) &&
                ((tMaxX.setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP)).compareTo(oMaxX.setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP))==0) &&
                ((tMaxY.setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP)).compareTo(oMaxY.setScale(this.decimalPlaces, BigDecimal.ROUND_HALF_UP))==0) );
    }

    public enum zoom { NONE, IN, OUT, N, S, E, W, NE, SE, NW, SW, PAN, LEFT, RIGHT, UP, DOWN };
    
    public int ZoomOrPan(zoom _direction,
                         int  _percentage) 
    {
        if ( _percentage == 0 ) {
            return 0;
        }
        switch (_direction) {
        case NONE: // all
            break;
        case IN: // in
            this.ZoomIn(_percentage);
            break;
        case OUT: // out
            this.ZoomOut(_percentage);
            break;
        case N  :
        case UP :
            this.PanN(_percentage);
            break;
        case S :
            this.PanS(_percentage);
            break;
        case E : 
        case RIGHT :
            this.PanE(_percentage);
            break;
        case W :
        case LEFT :
            this.PanW(_percentage);
            break;
        case NE :
            this.PanNE(_percentage);
            break;
        case SE :
            this.PanSE(_percentage);
            break;
        case NW : 
            this.PanNW(_percentage);
            break;
        case SW :
            this.PanSW(_percentage);
            break;
        case DOWN :
        default:
            break;
        } // switch
        return 0;
    } // public Zoom

    /**
     * A whole bunch of pan and zoom methods
     */
    
    /** 
     * Implements ZoomOut method.  Zooms out from the centre of the Envelope a
     *   percentage of the current Envelope.
     *   
     * @version   1.0
     * @param     percentage : Change to Envelope. Number greater than 1. 
     * @author    Simon Greener, November 2002 - Original Coding
    **/
    public void ZoomOut( int percentage ) {
        double deltaX = this.getWidth()  * ((double)percentage / 100.0f);
        double deltaY = this.getHeight() * ((double)percentage / 100.0f);
        double width  = this.getWidth()  + deltaX;
        double height = this.getHeight() + deltaY;
        Point2D center = this.centre();
        this.minX = center.getX() - (width  / 2.0f);
        this.minY = center.getY() - (height / 2.0f);
        this.maxX = center.getX() + (width  / 2.0f);
        this.maxY = center.getY() + (height / 2.0f);
    }
    
    /** 
     * Implements ZoomIn method.  Zooms in from the centre of the Envelope a
     *   percentage of the current Envelope.
     *   
     * @version   1.0
     * @param     percentage : Change to Envelope. Number greater than 1. 
     * @author    Simon Greener, November 2002 - Original Coding
    **/
    public void ZoomIn( int percentage ) {
        double deltaX = this.getWidth()  * ((double)percentage / 100.0f);
        double deltaY = this.getHeight() * ((double)percentage / 100.0f);
        double width  = this.getWidth()  - deltaX;
        double height = this.getHeight() - deltaY;
        Point2D center = this.centre();
        this.minX = center.getX() - (width  / 2.0f);
        this.minY = center.getY() - (height / 2.0f);
        this.maxX = center.getX() + (width  / 2.0f);
        this.maxY = center.getY() + (height / 2.0f);
    }
    
    /** 
     * Implements PanN method.  Pans upwards (north) a percentage of the
     *   height of the Envelope eg 100% will shift the Envelope height distance 
     *   to the north.
     * 
     * @version   1.0
     * @param     percentage : Shift in y direction expressed as percentage
     *                         of Envelope. Number greater than 1. 
     * @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanN( int percentage ) {
      double deltaY = (this.getHeight() * (double)percentage / 100.0f);
      this.minY += deltaY;
      this.maxY += deltaY;
    }
    
    /** 
     * Implements PanUp method.  Pans upwards (north) a percentage of the
     *   height of the Envelope eg 100% will shift the Envelope height distance 
     *   to the north.
     *   
     *  @param     percentage : Shift in y direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanUp( int percentage ) {
      this.PanN( percentage );
    }
    
    /** 
     * Implements PanS method.  Pans downwards (south) a percentage of the
     *   height of the Envelope eg 100% will shift the Envelope height distance 
     *   to the south.
     *   
     *  @param     percentage : Shift in y direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanS( int percentage ) {
      double deltaY = (this.getHeight() * (double)percentage / 100.0f);
      this.minY -= deltaY;
      this.maxY -= deltaY;
    }
    
    /**
     * Implements PanDown method.  Pans downwards (south) a percentage of the
     *   height of the Envelope eg 100% will shift the Envelope height distance 
     *   to the south.
     *   
     *  @param     percentage : Shift in y direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanDown( int percentage ) {
      this.PanS( percentage );
    }
    
    /** 
     * Implements PanE method.  Pans right (east) a percentage of the
     *   width of the Envelope eg 100% will shift the Envelope width distance 
     *   to the east.
     *   
     *  @param     percentage : Shift in x direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanE( int percentage ) {
      double deltaX = (this.getWidth()  * (double)percentage / 100.0f);
      this.minX += deltaX;
      this.maxX += deltaX;
    }
    
    /** 
     * Implements PanRight method.  Pans right (east) a percentage of the
     *   width of the Envelope eg 100% will shift the Envelope width distance 
     *   to the east.
     *   
     *  @param     percentage : Shift in x direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanRight( int percentage ) {
      this.PanE( percentage );
    }
    
    /** 
     * Implements PanW method.  Pans left (west) a percentage of the
     *   width of the Envelope eg 100% will shift the Envelope width distance 
     *   to the west.
     *   
     *  @param     percentage : Shift in x direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanW( int percentage ) {
      double deltaX = (this.getWidth()  * (double)percentage / 100.0f);
      this.minX -= deltaX;
      this.maxX -= deltaX;
    }
    
    /** 
     * Implements PanLeft method.  Pans left (west) a percentage of the
     *   width of the Envelope eg 100% will shift the Envelope width distance 
     *   to the west.
     *   
     *  @param     percentage : Shift in x direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanLeft( int percentage ) {
      this.PanW( percentage );
    }
    
    /** 
     * Implements PanNE method.  Pans up and right (NE) a percentage of the
     *   width and height of the Envelope eg 100% will shift the Envelope 1 x width 
     *   and 1 x height distance to the NE.
     *   
     *  @param     percentage : Shift in NE direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanNE( int percentage ) {
      double deltaX = (this.getWidth()  * (double)percentage / 100.0f);
      double deltaY = (this.getHeight() * (double)percentage / 100.0f);
      this.minY += deltaY;
      this.maxY += deltaY;
      this.minX += deltaX;
      this.maxX += deltaX;
    }
    
    /** 
     * Implements PanNW method.  Pans up and left (NW) a percentage of the
     *   width and height of the Envelope eg 100% will shift the Envelope 1 x width 
     *   and 1 x height distance to the NW.
     *   
     *  @param     percentage : Shift in NW direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanNW( int percentage ) {
      double deltaX = (this.getWidth()  * (double)percentage / 100.0f);
      double deltaY = (this.getHeight() * (double)percentage / 100.0f);
      this.minY += deltaY;
      this.maxY += deltaY;
      this.minX -= deltaX;
      this.maxX -= deltaX;
    }
    
    /** 
     * Implements PanSE method.  Pans down and right (NE) a percentage of the
     *   width and height of the Envelope eg 100% will shift the Envelope 1 x width 
     *   and 1 x height distance to the SE.
     *   
     *  @param     percentage : Shift in SE direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanSE( int percentage ) {
      double deltaX = (this.getWidth()  * (double)percentage / 100.0f);
      double deltaY = (this.getHeight() * (double)percentage / 100.0f);
      this.minY -= deltaY;
      this.maxY -= deltaY;
      this.minX += deltaX;
      this.maxX += deltaX;
    }
    
    /** 
     * Implements PanSW method.  Pans down and left (SW) a percentage of the
     *   width and height of the Envelope eg 100% will shift the Envelope 1 x width 
     *   and 1 x height distance to the SW.
     *   
     *  @param     percentage : Shift in SW direction expressed as percentage
     *                          of Envelope. Number greater than 1. 
     *  @version   1.0
     *  @author    Simon Greener, November 2002 - Original Coding
    **/
    public void PanSW( int percentage ) {
      double deltaX = (this.getWidth()  * (double)percentage / 100.0f);
      double deltaY = (this.getHeight() * (double)percentage / 100.0f);
      this.minY -= deltaY;
      this.maxY -= deltaY;
      this.minX -= deltaX;
      this.maxX -= deltaX;
    }

    public double[] toOrdinateArray() {
        return new double[]{this.minX,this.minY,this.maxX,this.maxY};
    }

    public JGeometry toJGeometry(int _srid) {
        return new JGeometry(this.minX,this.minY,this.maxX,this.maxY,
                             _srid>0?_srid:Constants.NULL_SRID);
    }
    
    public String toSdoGeometry(String _SRID,
                                int    _precision) {
        DecimalFormat df = Tools.getDecimalFormatter(_precision); 
        String _fmt = "%s"; 
        String _sFormat = _fmt + "," + _fmt + "," + _fmt +"," + _fmt;
        return "sdo_geometry(2003,"+(Strings.isEmpty(_SRID)?"NULL":_SRID)+",NULL,sdo_elem_info_array(1,1003,3),sdo_ordinate_array(" + 
               ( this.isNull() ? "null,null,null,null"
                               : String.format(_sFormat,
                                               df.format(this.minX),df.format(this.minY),
                                               df.format(this.maxX),df.format(this.maxY)) ) + "))"; 
    }
    
    public Envelope(Node _node) {
        this.fromXMLNode(_node);
    }
    
    public Envelope(String _XML) 
    {
      try 
      {
          Document doc = null;
          DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
          dbf.setNamespaceAware(false);
          dbf.setValidating(false);
          DocumentBuilder db = dbf.newDocumentBuilder();
          doc = db.parse(new InputSource(new StringReader(_XML)));         
          XPath xpath = XPathFactory.newInstance().newXPath();
          this.fromXMLNode((Node)xpath.evaluate("/MBR",doc,XPathConstants.NODE));
        } catch (XPathExpressionException xe) {
            System.out.println("XPathExpressionException " + xe.toString());
        } catch (ParserConfigurationException pe) {
            System.out.println("ParserConfigurationException " + pe.toString());
        } catch (SAXException se) {
            System.out.println("SAXException " + se.toString());
        } catch (IOException ioe) { 
            System.out.println("IOException " + ioe.toString());
        }
    }

    private void fromXMLNode(Node _node) 
    {
      if ( _node == null || _node.getNodeName().equals("MBR")==false) {
          System.out.println("Node is null or not MBR");
          return;  // Should throw error
      }
      try 
      {
          XPath xpath = XPathFactory.newInstance().newXPath();
          this.minX = Double.valueOf((String)xpath.evaluate("MinX/text()",_node,XPathConstants.STRING));
          this.minY = Double.valueOf((String)xpath.evaluate("MinY/text()",_node,XPathConstants.STRING));
          this.maxX = Double.valueOf((String)xpath.evaluate("MaxX/text()",_node,XPathConstants.STRING));
          this.maxY = Double.valueOf((String)xpath.evaluate("MaxY/text()",_node,XPathConstants.STRING));
      } catch (XPathExpressionException xe) {
        System.out.println("XPathExpressionException " + xe.toString());
      }
    }
    
    public String toString() {
        String fmt = "%." + this.decimalPlaces + "f";
        String sFormat = "(" + fmt + "," + fmt + "),(" + fmt +"," + fmt + ")";
        return this.isNull() 
            ? "NULL"
            : String.format(sFormat,this.minX,this.minY,this.maxX,this.maxY); 
    }

    public String toXML() {
        return String.format("<MBR><MinX>%s</MinX><MinY>%s</MinY><MaxX>%s</MaxX><MaxY>%s</MaxY></MBR>",
                             this.minX,this.minY,this.maxX,this.maxY);
    }

    /*  ==================================================================================== */

    /**
     *  Returns <code>true</code> if any points on the boundary of 
     *  <code>other</code> coincide with any points on the boundary of 
     *  this <code>Envelope</code>.
     *
     * @param  _other - the <code>Envelope</code> which this <code>Envelope</code> is
     *      being checked for overlapping
     * @return <code>true</code> if the boundaries of the two <code>Envelope</code>s
     *      intersect
     * @version 1.0
     * @author Simon Greener
     */
    public boolean overlaps(Envelope _other) {
      return (_other==null
          ? false 
          : !(_other.getMinX() > this.maxX ||
              _other.getMaxX() < this.minX ||
              _other.getMinY() > this.maxY ||
              _other.getMaxY() < this.minY));
    }

    /*  ==================================================================================== */

    /**
     *  Returns <code>true</code> if all points on the boundary of 
     *  <code>other</code> lie in the interior or on the boundary of 
     *  this <code>Envelope</code>.
     *
     * @param  _other the <code>Envelope</code> which this <code>Envelope</code> is
     *      being checked for containing
     * @return <code>true</code> if the interior and boundary of <code>other</code>
     *      is a subset of the interior and boundary of this <code>Envelope</code>
     * @version 1.0
     * @author Simon Greener
     */
    public boolean contains(Envelope _other) {
      return _other.getMinX() >= this.minX &&
             _other.getMaxX() <= this.maxX &&
             _other.getMinY() >= this.minY &&
             _other.getMaxY() <= this.maxY;
    }

    public boolean contains(Point2D _point) {
      return _point.getX() >= this.minX &&
             _point.getX() <= this.maxX &&
             _point.getY() >= this.minY &&
             _point.getY() <= this.maxY;
    }

    public boolean contains(Coordinate _point) {
      return _point.getX() >= this.minX &&
             _point.getX() <= this.maxX &&
             _point.getY() >= this.minY &&
             _point.getY() <= this.maxY;
    }

    public boolean contains(Line2D _line) {
      return this.contains(_line.getP1()) && this.contains(_line.getP2());
    }

    /*  ==================================================================================== */

    public double difference(Envelope _mbr) {
      if (_mbr == null ) {
         return 100.0;
      }
      if (this.overlaps(_mbr) == false)  {
          return 100;
      }
      // How much are they different?
      return (_mbr.area()/this.area()) * 100.0;
    }

    /*  ==================================================================================== */

    /**
     * Tests if the <code>Envelope _other</code>
     * lies wholely inside this <code>Envelope</code> (inclusive of the boundary).
     *
     *@param  _other the <code>Envelope</code> to check
     *@return true if this <code>Envelope</code> covers the <code>other</code> 
     */
    public boolean covers(Envelope _other) {
      if (isNull() || _other.isNull()) { return false; }
      return _other.getMinX() >= this.minX &&
             _other.getMaxX() <= this.maxX &&
             _other.getMinY() >= this.minY &&
             _other.getMaxY() <= this.maxY;
    }

    /**
     * Tests if the given point lies in or on the envelope.
     *
     *@param  x  the x-coordinate of the point which this <code>Envelope</code> is
     *      being checked for containing
     *@param  y  the y-coordinate of the point which this <code>Envelope</code> is
     *      being checked for containing
     *@return    <code>true</code> if <code>(x, y)</code> lies in the interior or
     *      on the boundary of this <code>Envelope</code>.
     */
    public boolean covers(double x, double y) {
        if (this.isNull()) return false;
        return x >= this.minX &&
               x <= this.maxX &&
               y >= this.minY &&
               y <= this.maxY;
    }

    /**
     * Tests if the given point lies in or on the envelope.
     *
     *@param  p  the point which this <code>Envelope</code> is
     *      being checked for containing
     *@return    <code>true</code> if the point lies in the interior or
     *      on the boundary of this <code>Envelope</code>.
     */
    public boolean covers(Coordinate p) {
      return covers(p.x, p.y);
    }

    /*  ==================================================================================== */
    
    /**
     *  Check if the point <code>(x, y)</code>
     *  overlaps (lies inside) the region of this <code>Envelope</code>.
     *
     *@param _x  the x-ordinate of the point
     *@param _y  the y-ordinate of the point
     *@return        <code>true</code> if the point overlaps this <code>Envelope</code>
     */
    public boolean intersects(double _x, double _y) {
      if (this.isNull()) return false;
      return ! (_x > this.maxX ||
                _x < this.minX ||
                _y > this.maxY ||
                _y < this.minY);
    }

    /**
     *  Check if the region defined by <code>other</code>
     *  overlaps (intersects) the region of this <code>Envelope</code>.
     *
     *@param  _other  the <code>Envelope</code> which this <code>Envelope</code> is
     *                 being checked for overlapping
     *@return        <code>true</code> if the <code>Envelope</code>s overlap
     */
    public boolean intersects(Envelope _other) {
        if (isNull() || _other.isNull()) { return false; }
      return !(_other.minX > this.maxX ||
               _other.maxX < minX ||
               _other.minY > maxY ||
               _other.maxY < minY);
    }
    
    /**
     *  Check if the point <code>p</code> overlaps (lies inside) the region of this <code>Envelope</code>.
     *
     *@param  p  the <code>Coordinate</code> to be tested
     *@return        <code>true</code> if the point overlaps this <code>Envelope</code>
     */
    public boolean intersects(Coordinate p) {
      return intersects(p.x, p.y);
    }

    /**
     * Computes the intersection of two {@link Envelope} s.
     *
     * @param env the Envelope to intersect with
     * @return a new Envelope representing the intersection of the Envelopes (this will be
     * the null Envelope if either argument is null, or they do not intersect
     */
    public Envelope intersection(Envelope _env)
    {
        if (isNull() || _env.isNull() || ! this.intersects(_env)) {
            return new Envelope(this.decimalPlaces);
        }
        double intMinX = this.minX > _env.minX ? this.minX : _env.minX;
        double intMinY = this.minY > _env.minY ? this.minY : _env.minY;
        double intMaxX = this.maxX < _env.maxX ? this.maxX : _env.maxX;
        double intMaxY = this.maxY < _env.maxY ? this.maxY : _env.maxY;
        return new Envelope(intMinX, intMaxX, intMinY, intMaxY);
    }

}
