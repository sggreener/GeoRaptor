/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002-2006, Geotools Project Managment Committee (PMC)
 *    (C) 2002, Centre for Computational Geography
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
package org.geotools.data.shapefile.shp;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.RayCrossingCounter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Wrapper for a Shapefile polygon.
 * @author aaime
 * @author Ian Schneider
 * @source $URL: http://svn.geotools.org/tags/2.4.5/modules/plugin/shapefile/src/main/java/org/geotools/data/shapefile/shp/PolygonHandler.java $
 * @version $Id: PolygonHandler.java 30009 2008-04-21 14:46:19Z groldan $
 */
public class PolygonHandler implements ShapeHandler {
  GeometryFactory geometryFactory = new GeometryFactory();
  
  final ShapeType shapeType;
  
  
  public PolygonHandler() {
    shapeType = ShapeType.POLYGON;
  }
  
  public PolygonHandler(ShapeType type) throws ShapefileException {
    if ((type != ShapeType.POLYGON) && (type != ShapeType.POLYGONM) && (type != ShapeType.POLYGONZ)) {
      throw new ShapefileException(
      "PolygonHandler constructor - expected type to be 5, 15, or 25.");
    }
    
    shapeType = type;
  }
  
  //returns true if testPoint is a point in the pointList list.
  boolean pointInList(Coordinate testPoint, Coordinate[] pointList) {
    Coordinate p;
    
    for (int t = pointList.length - 1; t >= 0; t--) {
      p = pointList[t];
      
      if ((testPoint.x == p.x) && (testPoint.y == p.y) &&
      ((testPoint.z == p.z) || (!(testPoint.z == testPoint.z))) //nan test; x!=x iff x is nan
      ) {
        return true;
      }
    }
    
    return false;
  }
  
  
  public ShapeType getShapeType() {
    return shapeType;
  }
  
  public int getLength(Object geometry) {
    MultiPolygon multi;
    
    if (geometry instanceof MultiPolygon) {
      multi = (MultiPolygon) geometry;
    } else {
      multi = geometryFactory.createMultiPolygon(new Polygon[] { (Polygon) geometry });
    }
    
    int nrings = 0;
    
    for (int t = 0; t < multi.getNumGeometries(); t++) {
      Polygon p;
      p = (Polygon) multi.getGeometryN(t);
      nrings = nrings + 1 + p.getNumInteriorRing();
    }
    
    int npoints = multi.getNumPoints();
    int length;
    
    if (shapeType == ShapeType.POLYGONZ) {
      length = 44 + (4 * nrings) + (16 * npoints) + (8 * npoints) + 16 + (8 * npoints) + 16;
    } else if (shapeType == ShapeType.POLYGONM) {
      length = 44 + (4 * nrings) + (16 * npoints) + (8 * npoints) + 16;
    } else if (shapeType == ShapeType.POLYGON) {
      length = 44 + (4 * nrings) + (16 * npoints);
    } else {
      throw new IllegalStateException("Expected ShapeType of Polygon, got " + shapeType);
    }
    return length;
  }

  
  
  public Object read(ByteBuffer buffer, ShapeType type) 
  {
    if (type == ShapeType.NULL) {
      return createNull();
    }
    //bounds
    buffer.position(buffer.position() + 4 * 8);
    
    int[] partOffsets;
    
    int numParts = buffer.getInt();
    int numPoints = buffer.getInt();
    
    partOffsets = new int[numParts];
    
    for (int i = 0; i < numParts; i++) {
      partOffsets[i] = buffer.getInt();
    }
    
    ArrayList<LinearRing> shells = new ArrayList<LinearRing>();
    ArrayList<LinearRing> holes = new ArrayList<LinearRing>();
    Coordinate[] coords = readCoordinates(buffer, numPoints);
    
    if (shapeType == ShapeType.POLYGONZ) {
      //z
      buffer.position(buffer.position() + 2 * 8);
      for (int t = 0; t < numPoints; t++) {
        coords[t].z = buffer.getDouble();
      }
    }
    
    int offset = 0;
    int start;
    int finish;
    int length;
    
    for (int part = 0; part < numParts; part++) {
      start = partOffsets[part];
      
      if (part == (numParts - 1)) {
        finish = numPoints;
      } else {
        finish = partOffsets[part + 1];
      }
      
      length = finish - start;
      
      // Use the progressive CCW algorithm.
      // basically the area algorithm for polygons
      // which also tells us vertex order based upon the
      // sign of the area.
      Coordinate[] points = new Coordinate[length];
      //double area = 0;
      //int sx = offset;
      for (int i = 0; i < length; i++) {
        points[i] = coords[offset++];
        //int j = sx + (i + 1) % length;
        //area += points[i].x * coords[j].y;
        //area -= points[i].y * coords[j].x;
      }
      //area = -area / 2;
      //REVISIT: polygons with only 1 or 2 points are not polygons - geometryFactory will bomb so we skip if we find one.
      if(points.length == 0 || points.length > 3){ 
          LinearRing ring = geometryFactory.createLinearRing(points);

          if (Orientation.isCCW(points)) {
            // counter-clockwise
            holes.add(ring);
          } else {
            // clockwise
            shells.add(ring);
          } 
      }
    }
    
    // quick optimization: if there's only one shell no need to check
    // for holes inclusion
    if(shells.size() == 1) {
      return createMulti( (LinearRing) shells.get(0) , holes );
    }
    // if for some reason, there is only one hole, we just reverse it and carry on.
    else if (holes.size() == 1 && shells.size() == 0) {
      org.GeoRaptor.util.logging.Logging.getLogger("org.geotools.data.shapefile").warning(
      "only one hole in this polygon record");
      return createMulti(
        JTSUtilities.reverseRing( (LinearRing) holes.get(0) )
      );
    } else {

      // build an association between shells and holes
      final ArrayList<ArrayList<LinearRing>> holesForShells = assignHolesToShells(shells, holes);
      
      Geometry g = buildGeometries(shells, holes, holesForShells);
      
      return g;
    }
  }

  /**
   * @param buffer
   * @param numPoints
   */
  private Coordinate[] readCoordinates(final ByteBuffer buffer, final int numPoints) {
      Coordinate[] coords = new Coordinate[numPoints];
      
      for (int t = 0; t < numPoints; t++) {
        coords[t] = new Coordinate(buffer.getDouble(), buffer.getDouble());
      }

      return coords;
  }

  /**
   * @param shells
   * @param holes
   * @param holesForShells
   */
  private Geometry buildGeometries(final List<LinearRing> shells, 
                                   final List<LinearRing> holes, 
		                           final ArrayList<ArrayList<LinearRing>> holesForShells) 
  {
      Polygon[] polygons;
      
      // if we have shells, lets use them
      if (shells.size() > 0) {
        polygons = new Polygon[shells.size()];
      // oh, this is a bad record with only holes
      } else {
        polygons = new Polygon[holes.size()];
      }
      
      // this will do nothing for the "only holes case"
      for (int i = 0; i < shells.size(); i++) {
        polygons[i] = geometryFactory.createPolygon((LinearRing) shells.get(i),
        (LinearRing[]) holesForShells.get(i).toArray(new LinearRing[0]));
      }
      
      // this will take care of the "only holes case"
      // we just reverse each hole
      if (shells.size() == 0) {
        for (int i = 0, ii = holes.size(); i < ii; i++) {
          LinearRing hole = (LinearRing) holes.get(i);
          polygons[i] = geometryFactory.createPolygon(
            JTSUtilities.reverseRing(hole),
            new LinearRing[0]
          );
        }
      }
      
      Geometry g = geometryFactory.createMultiPolygon(polygons);

      return g;
  }

  /** 
   * <b>Package private for testing</b>
   * @param shells
   * @param holes
   */
  ArrayList<ArrayList<LinearRing>> assignHolesToShells(final ArrayList<LinearRing> shells, 
                                                       final ArrayList<LinearRing> holes) 
  {
      ArrayList<ArrayList<LinearRing>> holesForShells = new ArrayList<>(shells.size());
      for (int i = 0; i < shells.size(); i++) {
        holesForShells.add(new ArrayList<>());
      }
      
      //find homes
      for (int i = 0; i < holes.size(); i++) 
      {
        LinearRing testRing = (LinearRing) holes.get(i);
        LinearRing minShell = null;
        Envelope minEnv = null;
        Envelope testEnv = testRing.getEnvelopeInternal();
        Coordinate testPt = testRing.getCoordinateN(0);
        LinearRing tryRing;
        
        
        for (int j = 0; j < shells.size(); j++) {
          tryRing = (LinearRing) shells.get(j);
          
          Envelope tryEnv = tryRing.getEnvelopeInternal();
          if (minShell != null) {
            minEnv = minShell.getEnvelopeInternal();
          }
          
          boolean isContained = false;
          Coordinate[] coordList = tryRing.getCoordinates();
          
          int location = RayCrossingCounter.locatePointInRing(testPt, coordList);
          if ( tryEnv.contains(testEnv) 
               && 
               (location != Location.EXTERIOR || 
               (pointInList(testPt, coordList)))) {
            isContained = true;
          }
          
          // check if this new containing ring is smaller than the current minimum ring
          if (isContained) {
            if ((minShell == null) || minEnv.contains(tryEnv)) {
              minShell = tryRing;
            }
          }
        }
        
        if (minShell == null) {
          org.GeoRaptor.util.logging.Logging.getLogger("org.geotools.data.shapefile").warning(
          "polygon found with a hole thats not inside a shell"
          );
          // now reverse this bad "hole" and turn it into a shell
          shells.add(JTSUtilities.reverseRing(testRing));
          holesForShells.add(new ArrayList<LinearRing>());
        } else {
        	((ArrayList<LinearRing>) 
        	       holesForShells.get(shells.indexOf(minShell))
            ).add(testRing);
        }
      }
      return holesForShells;
  }
  
  private MultiPolygon createMulti(LinearRing single) {
    return createMulti(single,Collections.emptyList());
  }
  
  private MultiPolygon createMulti(LinearRing single,List<LinearRing> holes) {
    return geometryFactory.createMultiPolygon(
      new Polygon[] {
        geometryFactory.createPolygon(single, (LinearRing[]) holes.toArray(new LinearRing[holes.size()]))
      }
    );
  }
  
  private MultiPolygon createNull() {
    return geometryFactory.createMultiPolygon(null);
  }
  
  public void write(ByteBuffer buffer, Object geometry) {
    MultiPolygon multi;
    
    if (geometry instanceof MultiPolygon) {
      multi = (MultiPolygon) geometry;
    } else {
      multi = geometryFactory.createMultiPolygon(new Polygon[] { (Polygon) geometry });
    }
    
    Envelope box = multi.getEnvelopeInternal();
    buffer.putDouble(box.getMinX());
    buffer.putDouble(box.getMinY());
    buffer.putDouble(box.getMaxX());
    buffer.putDouble(box.getMaxY());
    
    //need to find the total number of rings and points
    final int nrings;
    final CoordinateSequence[] coordinates;
    {
        List<CoordinateSequence> allCoords = new ArrayList<>();
        for (int t = 0; t < multi.getNumGeometries(); t++) {
          Polygon p;
          p = (Polygon) multi.getGeometryN(t);
          allCoords.add(p.getExteriorRing().getCoordinateSequence());
          for(int ringN = 0; ringN < p.getNumInteriorRing(); ringN++){
              allCoords.add(p.getInteriorRingN(ringN).getCoordinateSequence());
          }
        }
        coordinates = (CoordinateSequence[])allCoords.toArray(new CoordinateSequence[allCoords.size()]);
        nrings = coordinates.length;
    }
    
    final int npoints = multi.getNumPoints();
    
    buffer.putInt(nrings);
    buffer.putInt(npoints);
    
    int count = 0;
    for (int t = 0; t < nrings; t++) {
      buffer.putInt(count);
      count = count + coordinates[t].size();
    }
    
    final double[] zExtreame = {Double.NaN, Double.NaN};
    
    //write out points here!.. and gather up min and max z values
    for (int ringN = 0; ringN < nrings; ringN++) {
        CoordinateSequence coords = coordinates[ringN];
        
        JTSUtilities.zMinMax(coords, zExtreame);
        
        final int seqSize = coords.size();
        for(int coordN = 0; coordN < seqSize; coordN++){
            buffer.putDouble(coords.getOrdinate(coordN, 0));
            buffer.putDouble(coords.getOrdinate(coordN, 1));
        }
    }
    
    if (shapeType == ShapeType.POLYGONZ) {
      //z      
      if (Double.isNaN(zExtreame[0])) {
        buffer.putDouble(0.0);
        buffer.putDouble(0.0);
      } else {
        buffer.putDouble(zExtreame[0]);
        buffer.putDouble(zExtreame[1]);
      }
      
      for (int ringN = 0; ringN < nrings; ringN++) {
          CoordinateSequence coords = coordinates[ringN];
      
          final int seqSize = coords.size();
          double z;
          for (int coordN = 0; coordN < seqSize; coordN++) {
              z = coords.getOrdinate(coordN, 2);
              if (Double.isNaN(z)) {
                  buffer.putDouble(0.0);
              } else {
                  buffer.putDouble(z);
              }
          }
      }
    }
    
    if (shapeType == ShapeType.POLYGONM || shapeType == ShapeType.POLYGONZ) {
      //m
      buffer.putDouble(-10E40);
      buffer.putDouble(-10E40);
      
      for (int t = 0; t < npoints; t++) {
        buffer.putDouble(-10E40);
      }
    }
  }
  
}

