/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002-2006, Geotools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.shapefile;


/**
 *
 * @author  Ian Schneider
 * @source $URL: http://svn.geotools.org/tags/2.4.5/modules/plugin/shapefile/src/main/java/org/geotools/data/shapefile/ShapefileUtilities.java $
 */
public class ShapefileUtilities {
    
    private ShapefileUtilities() {}
    
    /**
     * Marshal a given Object into the given Class.
     */
    public static Object forAttribute(final Object o,Class<?> colType) {
        Object object;
        if(colType == Integer.class) {
            object = o;
        } else if ((colType == Short.class) || (colType == Byte.class)) {
            object = Integer.valueOf(((Number) o).intValue());
        } else if (colType == Double.class) {
            object = o;
        } else if (colType == Float.class) {
            object = Double.valueOf(((Number) o).doubleValue());
        } else if (Number.class.isAssignableFrom(colType)) {
            object = o;
        } else if(colType == String.class) {
            if (o == null) {
                object = o;
            } else {
                object = o.toString();
            }
        } else if (colType == Boolean.class) {
            object = o;
        } else if (java.util.Date.class.isAssignableFrom(colType)) {
            object = o;
        } else {
            if (colType != null) {
                throw new RuntimeException("Cannot convert " + colType.getName());
            } else {
                throw new RuntimeException("Null Class for conversion");
            }
        }
        
        return object;
    }
    
    
    
}
