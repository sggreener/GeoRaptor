/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.io.oracle;

import java.math.BigDecimal;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;

import oracle.sql.NUMBER;

/**
 * Utility methods for working with Oracle structures.
 *
 * @author Martin Davis
 *
 */
public class OraUtil
{
  /**
   * Converts an Oracle <code>Datum</code> into an <code>int</code> value, 
   * with a default value used if the datum is NULL.
   * 
   * @param datum the Oracle Datum
   * @param defaultValue the value to use for NULLs
   * @return an integer value
   * @throws SQLException if an error occurs
   */
    public static int toInteger(Object datum,
    		                       int defaultValue) 
    {
      // Note Returning null for null sdo_geometry structure
        if (datum == null) 
          return defaultValue;
        
        BigDecimal bigDec = (BigDecimal)datum;
        return (bigDec == null) ? defaultValue : Integer.valueOf(bigDec.intValue());
    }

  /**
   * Converts an Oracle <code>ARRAY</code> into a <code>int</code> array, 
   * with a default value used to represent NULL values.
   * 
   * @param array an Oracle ARRAY of integer values
   * @param defaultValue the value to use for NULL values
   * @return an array of ints
   * @throws SQLException if an error was encountered
   */
  // Changed 17th August 2019
  public static int[] toIntArray(Array array, 
                                 int defaultValue) 
  throws SQLException
  {
      if (array == null) 
          return null;
      
      Array   intOArray = (Array)array;
      Object[] intArray = (Object[])intOArray.getArray();
      int[]        ints = new int[intArray.length];    
      BigDecimal     bd;
      for (int i =0; i < intArray.length; i++) { 
          bd = (BigDecimal)intArray[i];          
          ints[i] = (defaultValue == 0) ? bd.intValue() : defaultValue;
      }
      return ints;
  }

  /** Presents Datum[] as a int[] */
  public static int[] toIntArray(Object[] data, final int defaultValue)
      throws SQLException
  {
    if (data == null)
      return null;
        
    int array[] = new int[data.length];
    for (int i = 0; i < data.length; i++) {
      array[i] = toInteger(data[i], defaultValue);
    }
    return array;
  }


  /** Presents array as a double[] */
  // Changed 17th August 2019
  public static double[] toDoubleArray(Array array, final double defaultValue)
      throws SQLException
  {
      if ( array == null )
          return null;
      
      Array   dblOArray = (Array)array;
      Object[] dblArray = (Object[])dblOArray.getArray();
      double[]     dbls = new double[dblArray.length];    
      BigDecimal     bd;
      for (int i =0; i < dblArray.length; i++) { 
          bd = (BigDecimal)dblArray[i];
          dbls[i] = Double.isNaN(defaultValue) ? bd.doubleValue() : defaultValue;
      }
      return dbls;
  }

    /**
     * An SDO_POINT_TYPE object is not an array but a Struct.
     * This method converts the contents of the SDO_POINT object within an SDO_GEOMETRY to a double array
     * @param _struct
     * @author Simon Greener, August 17th 2019
     * @return
     */
    // note Tested 16th August
    public static double[] toDoubleArray(Struct _SdoPoint, 
                                          final double defaultValue) 
    {
        if (_SdoPoint == null) 
            return null;

        try {
            String sqlTypeName = _SdoPoint.getSQLTypeName();
            if ( ! sqlTypeName.equalsIgnoreCase("MDSYS.SDO_POINT_TYPE") ) {
                return null;
            }
            // Extract values and return array
            Object[] data = _SdoPoint.getAttributes();
            BigDecimal x = (BigDecimal)data[0];
            BigDecimal y = (BigDecimal)data[1];
            BigDecimal z = (BigDecimal)data[2];
            return new double[] { (x==null ? defaultValue : x.doubleValue()), 
                                  (y==null ? defaultValue : y.doubleValue()), 
                                  (z==null ? defaultValue : z.doubleValue()) 
                                };
        } catch (SQLException sqle) {
            return null;
        }
    }

  /** Presents datum as a double */
  public static double toDouble(Object datum, final double defaultValue)
  {
      // Note Returning null for null sdo_geometry structure
      if (datum == null) 
          return defaultValue;
      
      BigDecimal bigDec = (BigDecimal)datum;
      return (bigDec == null) ? defaultValue : bigDec.doubleValue();
  }

  /**
   * Convenience method for NUMBER construction.
   * <p>
   * Double.NaN is represented as <code>NULL</code> to agree with JTS use.
   * </p>
   */
  public static NUMBER toNUMBER(double number) throws SQLException
  {
    if (Double.isNaN(number)) {
      return null;
    }
    return new NUMBER(number);
  }

  /**
   * Convenience method for ARRAY construction.
   * </p>
   */
  // Changed 17th August 2019
  public static Array toArray(double[] doubles, 
                              String dataType,
                              Connection connection) 
  throws SQLException
  {
      Object arrayOfDoubles = doubles; 
      Array dArray = connection.createArrayOf(dataType,(Object[]) arrayOfDoubles);
      return dArray;
  }

  /**
   * Convenience method for ARRAY construction.
   */
  // Changed 17th August 2019
  public static Array toArray(int[] ints, 
                              String dataType,
                              Connection connection) 
  throws SQLException
  {
      Object arrayOfIntegers = ints; 
      Array dArray = connection.createArrayOf(dataType, (Object[])arrayOfIntegers);
      return dArray;
  }

  /** Convenience method for Struct construction. */
  // Changed 17th August 2019
  public static Struct toStruct(Object[] attributes, 
                                String dataType, 
                                Connection connection) 
  throws SQLException
  {
    //TODO: fix this to be more generic
    if (dataType.startsWith("*.")) {
      dataType = "DRA." + dataType.substring(2);
    }
    Struct stObject = connection.createStruct(dataType, attributes);
    return stObject;
  }

}
