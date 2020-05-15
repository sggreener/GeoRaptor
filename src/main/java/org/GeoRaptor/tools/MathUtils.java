package org.GeoRaptor.tools;

import java.awt.geom.Point2D;

import java.sql.SQLException;

import oracle.sql.NUMBER;

public class MathUtils {
    public MathUtils() {
        super();
    }
    

    public static double pythagoras(Point2D _one, Point2D _two) {
        return Math.sqrt( Math.pow((_two.getX() - _one.getX()),2.0f) +
                          Math.pow((_two.getX() - _one.getX()),2.0f) ); 
    }

    public static double roundToDecimals(double d, int c) 
    {
        long temp=(long)((d*Math.pow(10,c)));
        return (((double)temp)/Math.pow(10,c));
    }

    public static int numberToInt(Object _object, int _default) {
        if ( _object instanceof oracle.sql.NUMBER ) {
          try {
              NUMBER num = (NUMBER)_object;
              Integer i = new Integer(Integer.MIN_VALUE); if ( num.isConvertibleTo(i.getClass()) ) return num.intValue();
              Short   s = new Short(Short.MIN_VALUE);     if ( num.isConvertibleTo(s.getClass()) ) return num.shortValue();
              Long    l = new Long(Long.MIN_VALUE);       if ( num.isConvertibleTo(l.getClass()) ) return (int)num.longValue();
              Float   f = new Float(Float.NaN);           if ( num.isConvertibleTo(f.getClass()) ) return (int)Math.round(num.floatValue());
              Double  d = new Double(Double.NaN);         if ( num.isConvertibleTo(d.getClass()) ) return (int)Math.round(num.doubleValue());
          } catch (SQLException e) {
              return _default;
          }
        }
        return _default;
    }

}
