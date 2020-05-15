package org.GeoRaptor.tools;

import java.awt.geom.Point2D;

import org.GeoRaptor.SpatialView.layers.SVSpatialLayer;


/**
 * Support methods for SVSpatialLayer class
 */
public class COGO {
    /**
     * Platform line separator
     */
    protected String lineSeparator;

    /**
     * Reference to main class
     */
    protected SVSpatialLayer sPanel;

    /**
     * If we have HTML output, do we still writting coordinates.
     */
    protected boolean stillWriteHTMLtoOutput;

    /**
     * current length of HTML output
     */
    protected int currentOutputLengths;

    public COGO(SVSpatialLayer _sPanel) {
        this.sPanel = _sPanel;
        this.lineSeparator = System.getProperty("line.separator");
    }

    /**
     * @method computeAngle
     * @param _startPoint
     * @param _endPoint
     * @return double Angle in Radians
     * @method @method
     * @note    Point data has to be in world coordinates
     * @author @author
     */
    public static double computeAngle(Point2D _startPoint, Point2D _endPoint) 
    {
        double dBearing;
        double dEast;
        double dNorth;
    
        if (_startPoint == null || _endPoint == null)
            return 0.0f;
    
        if ((_startPoint.getX() == _endPoint.getX()) &&
            (_startPoint.getY() == _endPoint.getY()))
            return 0.0f;
    
        dEast = _endPoint.getX() - _startPoint.getX();
        dNorth = _endPoint.getY() - _startPoint.getY();
        if (dEast == 0.0f) {
            if (dNorth < 0) {
                dBearing = Math.PI;
            } else {
                dBearing = 0.0f;
            }
        } else {
            dBearing = (0.0f - Math.atan(dNorth / dEast)) + (Math.PI / 2.0f);
        }
        if (dEast < 0)
            dBearing += Math.PI;
        // -90 is to compensate for bearings being clockwise from north and Java2D is 90 degree different
        return dBearing - (Math.PI / 2.0f);
    }

    /**
     * Convert polygon definition from "Oracle Spatial form" (three points) to Java2D form (X,Y, radius).
     * Origin source code by Stephen R. Schmitt (http://home.att.net/~srschmitt/script_circle_solver.html)
     * @param _x1 First point
     * @param _y1 First point
     * @param _x2 Second point
     * @param _y2 Second point
     * @param _x3 Third point
     * @param _y3 Third point
     * @return circle definition in X,Y,radius
     */
    public static double[] convertCircleCorrs(double _x1, double _y1, double _x2,
                                              double _y2, double _x3, double _y3) {
        double pMatrika[][] = new double[3][3];

        pMatrika[0][0] = _x1;
        pMatrika[0][1] = _y1;
        pMatrika[1][0] = _x2;
        pMatrika[1][1] = _y2;
        pMatrika[2][0] = _x3;
        pMatrika[2][1] = _y3;

        // Calculate center and radius of circle given three points
        double circleRet[] = calcCircle(pMatrika);
        return circleRet;
    }

    /**
     * Calculate center and radius of circle given three points
     */
    public static double[] calcCircle(double[][] _p) {
        int i = 0;
        double m11 = 0, m12 = 0, m13 = 0, m14 = 0;
        double a[][] = { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
        double retValue[] = { 0, 0, 0 };

        for (i = 0; i < 3; i++) { // find minor 11
            a[i][0] = _p[i][0];
            a[i][1] = _p[i][1];
            a[i][2] = 1;
        }
        m11 = calcDeterminant(a, 3);

        for (i = 0; i < 3; i++) { // find minor 12
            a[i][0] = _p[i][0] * _p[i][0] + _p[i][1] * _p[i][1];
            a[i][1] = _p[i][1];
            a[i][2] = 1;
        }
        m12 = calcDeterminant(a, 3);

        for (i = 0; i < 3; i++) { // find minor 13
            a[i][0] = _p[i][0] * _p[i][0] + _p[i][1] * _p[i][1];
            a[i][1] = _p[i][0];
            a[i][2] = 1;
        }
        m13 = calcDeterminant(a, 3);

        for (i = 0; i < 3; i++) { // find minor 14
            a[i][0] = _p[i][0] * _p[i][0] + _p[i][1] * _p[i][1];
            a[i][1] = _p[i][0];
            a[i][2] = _p[i][1];
        }
        m14 = calcDeterminant(a, 3);

        if (m11 == 0) { // not a circle
            retValue[2] = 0;
        } else { // center of circle
            retValue[0] = 0.5 * m12 / m11;
            retValue[1] = -0.5 * m13 / m11;
            retValue[2] =
                    Math.sqrt(retValue[0] * retValue[0] + retValue[1] * retValue[1] +
                              m14 / m11);
        }

        return retValue; // the radius
    }

    /**
     * Recursive definition of determinate using expansion by minors.
     */
    public static double calcDeterminant(double[][] _a, double _n) {
        int i = 0, j = 0, j1 = 0, j2 = 0;
        double d = 0;
        double m[][] = { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };

        if (_n == 0) {
            d = _a[0][0] * _a[1][1] - _a[1][0] * _a[0][1];
        }

        if (_n == 2) { // terminate recursion
            d = _a[0][0] * _a[1][1] - _a[1][0] * _a[0][1];
        } else {
            d = 0;
            for (j1 = 0; j1 < _n; j1++) { // do each column
                for (i = 1; i < _n; i++) { // create minor
                    j2 = 0;
                    for (j = 0; j < _n; j++) {
                        if (j == j1) {
                            continue;
                        }
                        m[i - 1][j2] = _a[i][j];
                        j2++;
                    }
                }
                // sum (+/-)cofactor * minor
                d += Math.pow(-1.0, j1) * _a[0][j1] * calcDeterminant(m, _n - 1);
            }
        }
        return d;
    }

    /**
     * @function dot
     * @precis dot product of two vectors
     * @param _dStartX
     * @param _dStartY
     * @param _dCentreX
     * @param _dCentreY
     * @param _dEndX
     * @param _dEndY
     * @return dot product in radians
     * @author Simon Greener, March 2010
     */
    public static double dot(double _dStartX, double _dStartY, double _dCentreX,
                             double _dCentreY, double _dEndX, double _dEndY) {
        // Calculate the dot product.
        return (_dStartX - _dCentreX) * (_dEndX - _dCentreX) +
               (_dStartY - _dCentreY) * (_dEndY - _dCentreY);
    }

    /**
     * @function cross
     * @precis cross product between 2 vectors
     * @param _dStartX
     * @param _dStartY
     * @param _dCentreX
     * @param _dCentreY
     * @param _dEndX
     * @param _dEndY
     * @return cross product as radians
     * @author Simon Greener, March 2010
     */
    public static double cross(double _dStartX, double _dStartY, double _dCentreX,
                               double _dCentreY, double _dEndX, double _dEndY) {
        // Calculate the Z coordinate of the cross product.
        return (_dStartX - _dCentreX) * (_dEndY - _dCentreY) -
               (_dStartY - _dCentreY) * (_dEndX - _dCentreX);
    }

    /**
     * @function angleBetween3Points
     * @precis calculates angle between three projected points in radians.
     * @param _dStartX
     * @param _dStartY
     * @param _dCentreX
     * @param _dCentreY
     * @param _dEndX
     * @param _dEndY
     * @return angle as radians.
     * @author Simon Greener, March 2010
     */
    public static double angleBetween3Points(double _dStartX,  double _dStartY,
                                             double _dCentreX, double _dCentreY,
                                             double _dEndX,    double _dEndY) {
        double dDotProduct;
        double dCrossProduct;
        try {
            // Get the dot product and cross product.
            dDotProduct   = dot(_dStartX, _dStartY, _dCentreX, _dCentreY, _dEndX, _dEndY);
            dCrossProduct = cross(_dStartX, _dStartY, _dCentreX, _dCentreY, _dEndX, _dEndY);
            // Calculate the angle in Radians.
            return Math.atan2(dCrossProduct, dDotProduct);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("3 points do not have an angle.",
                                               iae);
        }
    }

    public static double angleBetween3Points(Point2D _Start,
                                             Point2D _Middle,
                                             Point2D _End )
   {
        return angleBetween3Points( _Start.getX(), _Start.getY(),
                                   _Middle.getX(),_Middle.getY(),
                                      _End.getX(),   _End.getY());
   }

    public static double angleBetween3Points(Coordinate _Start,
                                             Coordinate _Middle,
                                             Coordinate _End )
    {
        return angleBetween3Points( _Start.getX(), _Start.getY(),
                                   _Middle.getX(),_Middle.getY(),
                                      _End.getX(),   _End.getY());
    }

    /**
     * @function optimalCircleSegments
     * @precis Returns number of segments in a circle given a radius and
     *         arc to chord separation distance
     * @param _dRadius
     * @param _dArcToChordSeparation
     * @return number of segments as integer
     * @author Simon Greener, March 2010
     **/
    public static int optimalCircleSegments(double _dRadius,
                                            double _dArcToChordSeparation) {
        double dAngleRad;
        double dCentreToChordMidPoint;
        dCentreToChordMidPoint = _dRadius - _dArcToChordSeparation;
        dAngleRad = 2.0 * Math.acos(dCentreToChordMidPoint / _dRadius);
        return (int)Math.ceil((2.0 * Math.PI) / dAngleRad);
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */
    /* Vincenty Inverse Solution of Geodesics on the Ellipsoid (c) Chris Veness 2002-2010             */
    /*                                                                                                */
    /* from: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the */
    /*       Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975    */
    /*       http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf                                             */
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */

    /**
     * Calculates geodetic distance between two points specified by latitude/longitude using 
     * Vincenty inverse formula for ellipsoids
     *
     * @param   {Number} lon1, lat1: first point in decimal degrees
     * @param   {Number} lon2, lat2: second point in decimal degrees
     * @returns (Number} distance in metres between points
     */
    
     public static final double WGS84_UNIT               = 0.01745329251994330;
     public static final double WGS84_SEMI_MAJOR_AXIS    = 6378137;
     public static final double WGS84_SEMI_MINOR_AXIS    = 6356752.3142451794975639665996336551568; 
     public static final double WGS84_FLATENNING         = 298.257223563;
     public static final double WGS84_INVERSE_FLATENNING = 1/WGS84_FLATENNING;
     
     public static final double GRS80_SEMI_MINOR_AXIS    = 6356752.31414035584785210686152953307862;
     public static final double GRS80_FLATENNING         = 298.257222101;
     public static final double GRS80_INVERSE_FLATENNING = 1/GRS80_FLATENNING;
    
     public static double meters2Degrees(double _meters,
                                         double _latitude)
     {
         /* 5 Decimal places is more than enough for these rough calculations.
          * At 31 degrees South latitude, we get:
          * 1 degree of latitude              = 1.000000 degree or 110,874.40 meters
          * 1/10 of a degree of latitude      =	0.100000 degree	or  11,087.44 meters
          * 1/100 of a degree of latitude     =	0.010000 degree	or   1,108.74 meters
          * 1/1000 of a degree of latitude    =	0.001000 degree	or     110.87 meters
          * 1/10000 of a degree of latitude   =	0.000100 degree	or      11.09 meters
          * 1/100000 of a degree of latitude  =	0.000010 degree	or       1.11 meters
          * 1/1000000 of a degree of latitude =	0.000001 degree	or        .11 meters
          */
         double latitude = Math.abs(Math.cos(MathUtils.roundToDecimals(_latitude,5)));
         return MathUtils.roundToDecimals(_meters / (WGS84_UNIT * WGS84_SEMI_MAJOR_AXIS * latitude),5);
     }
     
     public static double distVincenty(double _semiMajorAxis,
                                       double _semiMinorAxis,
                                       double _inverseFlattening,
                                       double _lon1, double _lat1, 
                                       double _lon2, double _lat2) 
     throws Exception
    {
         // Check if all values in right range
         //
         if ( ( Math.abs(_lon1) > 180 ) ||
              ( Math.abs(_lat1) > 90)   ||
              ( Math.abs(_lon2) > 180 ) ||
              ( Math.abs(_lat2) > 90 ) ) {
             throw new Exception ("Longitude not between -180 and 180 or Latitude between -90 and 90");
        }

        double L = Math.toRadians(_lon2-_lon1);
        double U1 = Math.atan((1-_inverseFlattening) * Math.tan(Math.toRadians(_lat1)));
        double U2 = Math.atan((1-_inverseFlattening) * Math.tan(Math.toRadians(_lat2)));
        double sinU1 = Math.sin(U1);
        double cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2);
        double cosU2 = Math.cos(U2);
        
        double lambda = L, lambdaP, iterLimit = (double)100.0;
        double cosSqAlpha, cosSigma, sinSigma, cosLambda, cos2SigmaM, sigma;
        double sinLambda, sinAlpha, C;
        do {
          sinLambda = Math.sin(lambda);
          cosLambda = Math.cos(lambda);
          sinSigma = Math.sqrt((cosU2*sinLambda) * 
                               (cosU2*sinLambda) + 
                               (cosU1*sinU2-sinU1*cosU2*cosLambda) * 
                               (cosU1*sinU2-sinU1*cosU2*cosLambda));
          if (sinSigma==0) return (double)0.0;  // co-incident points
          cosSigma = sinU1*sinU2 + cosU1*cosU2*cosLambda;
          sigma = Math.atan2(sinSigma, cosSigma);
          sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
          cosSqAlpha = (double)1.0 - sinAlpha*sinAlpha;
          cos2SigmaM = cosSigma - (double)2.0 *sinU1*sinU2/cosSqAlpha;
          if (Double.isNaN(cos2SigmaM)) cos2SigmaM = (double)0.0;  // equatorial line: cosSqAlpha=0 
          C = _inverseFlattening / (double)16.0 * cosSqAlpha*((double)4.0 + _inverseFlattening*((double)4.0 - (double)3.0 * cosSqAlpha));
          lambdaP = lambda;
          lambda = L + ((double)1.0-C) * _inverseFlattening * sinAlpha * (sigma + C*sinSigma*(cos2SigmaM+C*cosSigma*((double)-1.0 + (double)2.0 * cos2SigmaM*cos2SigmaM)));
        } while (Math.abs(lambda-lambdaP) > (double)1e-12 && --iterLimit > 0);
    
        if (iterLimit==0) return Double.NaN ; // formula failed to converge
    
        double uSq = cosSqAlpha * (_semiMajorAxis*_semiMajorAxis - _semiMinorAxis*_semiMinorAxis) / (_semiMinorAxis*_semiMinorAxis);
        double A = (double)1.0 + uSq/(double)16384.0 * ((double)4096.0 + uSq*((double)-768.0 + uSq*((double)320.0 - (double)175.0 * uSq)));
        double B = uSq/(double)1024.0 * ((double)256.0 + uSq * ((double)-128.0 + uSq*((double)74.0 - (double)47.0 * uSq)));
        double deltaSigma = B * sinSigma * (cos2SigmaM+B/(double)4.0 * (cosSigma*((double)-1.0 + (double)2.0 * cos2SigmaM*cos2SigmaM)-
          B/6*cos2SigmaM*((double)-3.0 + (double)4.0 * sinSigma*sinSigma)*((double)-3.0 + (double)4.0 * cos2SigmaM*cos2SigmaM)));
        double s = _semiMinorAxis*A*(sigma-deltaSigma);
        
        s = MathUtils.roundToDecimals(s,3); // round to 1mm precision
        return s;
      }
     
     public static double distVincenty(double _lon1, double _lat1, 
                                       double _lon2, double _lat2) 
     throws Exception
     {
         return distVincenty(WGS84_SEMI_MAJOR_AXIS, WGS84_SEMI_MINOR_AXIS, WGS84_INVERSE_FLATENNING,
                             _lon1, _lat1, _lon2, _lat2);
     }

      public static double distVincenty(Coordinate _from, Coordinate _to) 
      throws Exception
      {
            return distVincenty(WGS84_SEMI_MAJOR_AXIS, WGS84_SEMI_MINOR_AXIS, WGS84_INVERSE_FLATENNING,
                                _from.getX(),_from.getY(), _to.getX(),_to.getY());       
      }

     public static double distVincenty(double _semiMajorAxis,
                                       double _semiMinorAxis,
                                       double _inverseFlattening,
                                       Coordinate _from, 
                                       Coordinate _to) 
     throws Exception
     {
         return distVincenty(_semiMajorAxis,_semiMinorAxis,_inverseFlattening,
                             _from.getX(),_from.getY(), 
                             _to.getX(),    _to.getY());
     }

    // does not work if one latitude is polar!!!
    public static double GreatCircleBearing( double _lon1, double _lat1,
                                             double _lon2, double _lat2 ) 
    throws Exception
    {
       // Check if all values in right range
       //
       if ( ( Math.abs(_lon1) > 180 ) ||
            ( Math.abs(_lat1) > 90)   ||
            ( Math.abs(_lon2) > 180 ) ||
            ( Math.abs(_lat2) > 90 ) ) {
           throw new Exception ("Longitude not between -180 and 180 or Latitude between -90 and 90");
       }
       double v_lon1 = Math.toRadians(_lon1),
              v_lat1 = Math.toRadians(_lat1),
              v_lon2 = Math.toRadians(_lon2),
              v_lat2 = Math.toRadians(_lat2);
       double v_dLong;
       double v_cosC;
       double v_cosD;
       double v_C;
       double v_D;

       v_dLong = v_lon2 - v_lon1;
       v_cosD  = ( Math.sin(v_lat1) * Math.sin(v_lat2) ) +
                  ( Math.cos(v_lat1) * Math.cos(v_lat2) * Math.cos(v_dLong) );
       v_D     = Math.acos(v_cosD);
       if ( v_D == 0.0 ) {
         v_D = 0.00000001; // roughly 1mm
       }
       v_cosC  = ( Math.sin(v_lat2) - v_cosD * Math.sin(v_lat1) ) /
                 ( Math.sin(v_D) * Math.cos(v_lat1) );
       // numerical error can result in |cosC| slightly > 1.0
       if ( v_cosC > 1.0 ) {
           v_cosC = 1.0;
       }
       if ( v_cosC < -1.0 ) {
           v_cosC = -1.0;
       }
       v_C  = 180.0 * Math.acos( v_cosC ) / Math.PI;
       if ( Math.sin(v_dLong) < 0.0 ) {
           v_C = 360.0 - v_C;
       }
       return (Math.round( 100.0 * v_C ) / 100.0);
    }

    public static double GreatCircleBearing(Coordinate _from, Coordinate _to) 
        throws Exception
    {
        return GreatCircleBearing(_from.getX(),_from.getY(), _to.getX(),_to.getY());   
    }

    /* ----------------------------------------------------------------------------------------
    * @function   : Distance
    * @precis     : Returns the distance between (dE1,dN1) and (dE2,dN2).
    * @version    : 1.0
    * @usage      : FUNCTION Distance( dE1 in number,
    *                                  dN1 in number,
    *                                  dE2 in number,
    *                                  dN2 in number)
    *                        RETURN NUMBER DETERMINISTIC;
    *               eg :new.shape := COGO.Distance(299900, 5200000, 300000, 5200100);
    * @param      : dE1      : NUMBER : X Ordinate of the start point for the vector
    * @param      : dN1      : NUMBER : Y Ordinate of the start point for the vector
    * @param      : dE2      : NUMBER : X Ordinate of the end point for the vector
    * @param      : dN2      : NUMBER : Y Ordinate of the end point for the vector
    * @return     : Distance : NUMBER : the length in metres of the vector between (dE1,dN1) and (dE2,dN2)
    * @note       : Does not throw exceptions
    * @note       : Assumes planar projection eg UTM.
    * @history    : Simon Greener - Feb 2005 - Original coding.
    */
    public static double distance(double dE1,
                                  double dN1,
                                  double dE2,
                                  double dN2)
    {
        double dEast;
        double dNorth;
        dEast = dE2 - dE1;
        dNorth = dN2 - dN1;
        return Math.sqrt(dEast * dEast + dNorth * dNorth);
    }

    public static double distance(Point2D _Start,
                                  Point2D _End) {
        return distance(_Start.getX(),_Start.getY(),
                        _End.getX(),  _End.getY());
    }
    
    public static double distance(Coordinate _Start,
                                  Coordinate _End) {
        double dEast;
        double dNorth;
        double dZ;
        dEast  = _End.getX() - _Start.getX();
        dNorth = _End.getY() - _Start.getY();
        if ( Double.isNaN(_Start.getZ()) || Double.isNaN(_End.getZ()) ) {
            return Math.sqrt(dEast * dEast + dNorth * dNorth);
        } else {
            dZ = _End.getZ() - _Start.getZ();
            return Math.sqrt(dEast * dEast + dNorth * dNorth + dZ * dZ );  
        }
    }

    /**
     * @function bearing
     * @precis Returns whole circle bearing given two points
     * @note   Only valid for projected data
     * @param _dE1
     * @param _dN1
     * @param _dE2
     * @param _dN2
     * @return angle as double in radians
     * @author Simon Greener, March 2010
     */
    public static double bearing(double _dE1, double _dN1, double _dE2, double _dN2) {
        double dBearing;
        double dEast;
        double dNorth;

        if (Double.isNaN(_dE1) || Double.isNaN(_dN1) || Double.isNaN(_dE2) || Double.isNaN(_dE1))
            return Double.NaN;

        if ((_dE1 == _dE2) && (_dN1 == _dN2))
            return Double.NaN;

        dEast  = _dE2 - _dE1;
        dNorth = _dN2 - _dN1;
        if (dEast == 0) {
            if (dNorth < 0) {
                dBearing = Math.PI;
            } else {
                dBearing = 0;
            }
        } else {
            dBearing = (0.0 - Math.atan(dNorth / dEast)) + Math.PI / 2.0;
        }
        if (dEast < 0)
            dBearing = dBearing + Math.PI;

        return dBearing;
    }

    public static double bearing(Point2D _Start,
                                 Point2D _End) {
        return bearing(_Start.getX(),_Start.getY(),
                       _End.getX(),  _End.getY());
    }

    public static double bearing(Coordinate _Start,
                                 Coordinate _End) {
        return bearing(_Start.getX(),_Start.getY(),
                         _End.getX(),  _End.getY());
    }

    public static double normalizeDegrees(double _angle) {
        double angle = _angle % 360;
        return angle < 0 ? angle + 360 : angle;
    }

    /**
     * @method degrees
     * @param _radians
     * @return int - Number between 0 and 360 - degrees converted from radians
     * @method @method
     * @author @author
     */
    public static double degrees(double _radians) {
        return Math.toDegrees(_radians);
    }

    /**
     * @method radians
     * @param _degrees
     * @return double - radians converted from degrees
     * @method @method
     * @author @author
     */
    public static double radians(double _degrees) {
        return Math.toRadians(_degrees);
        // return ( _degrees  - 90.0f) * (double)(Math.PI / 180.0f);
    }

    public static String DD2DMS(double _dDecDeg, 
                                int    _decPlaces ) {
        String sDegreeSymbol = "^";
        String sMinuteSymbol = "'";
        String sSecondSymbol = "\"";
        int iDeg;
        int iMin;
        double dSec;
        iDeg = (int)_dDecDeg;
        iMin = (int)((Math.abs(_dDecDeg) - Math.abs(iDeg)) * 60);
        dSec = (((Math.abs(_dDecDeg) - Math.abs(iDeg)) * 60) - iMin) * 60;
        String sSec = _decPlaces==0
                      ? String.format("%02d",(int)dSec)
                      : String.format("%02." + _decPlaces + "f", dSec);
        return "" + iDeg + sDegreeSymbol + iMin + sMinuteSymbol + sSec + sSecondSymbol;
    }

}
