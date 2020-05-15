package org.GeoRaptor.io.Export;

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
 *
 */
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.util.Iterator;
import java.util.LinkedHashSet;

import org.GeoRaptor.tools.FileUtils;

import org.geotools.data.shapefile.shp.ShapeHandler;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileHeader;
import org.geotools.resources.NIOUtilities;
import org.geotools.util.logging.Logger;


/**
 * ShapefileWriter allows for the storage of geometries in ESRI's shp format.
 * During writing, an index will also be created. To create a ShapefileWriter,
 * do something like<br>
 * <code>
 *   GeometryCollection geoms;
 *   File shp = new File("myshape.shp");
 *   File shx = new File("myshape.shx");
 *   ShapefileWriter writer = new ShapefileWriter(
 *     shp.getChannel(),shx.getChannel()
 *   );
 *   writer.write(geoms,ShapeType.ARC);
 * </code>
 * This example assumes that each shape in the collection is a LineString.
 *
 * @see org.geotools.data.shapefile.ShapefileDataStore
 * @author jamesm
 * @author aaime
 * @author Ian Schneider
 *
 * @author Simon Greener, February 2011, re-wrote large chunks to encapsulate
 *          FileOutStreams, File etc and to work with independent DBF writer.
 *          Additionally, made capable of writing set chunks of geometry data
 *          with re-writing of shapefile header.
 *
 */
public class ShapefileWriter 
{
    @SuppressWarnings("unused")
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.ShapefileWriter");

    private FileChannel      shpChannel;  // FileChannels are public because ShapefileReader.java requires it!
    private FileChannel      shxChannel;
    private ByteBuffer      shapeBuffer;
    private ByteBuffer      indexBuffer;

    private Envelope        shpEnvelope;
    
    private FileOutputStream  shpOutput;
    private FileOutputStream  shxOutput;
    private File                shpFile;
    private File                shxFile;
    
    private ShapefileHeader      header;
    private ShapeHandler        handler;
    private ShapeType         shapeType = ShapeType.UNDEFINED;
    private int     totalNumberOfShapes = 0;
    private int           shapesWritten = 0;
    private int          bufferPosition = 0;
    private int              fileLength = 100;
    private static int           offset = 50;
    
    /** Creates a new instance of ShapeFileWriter 
    * @throws IOException */
    public ShapefileWriter(java.lang.String _outputDirectory, 
                           java.lang.String _fileName,
                           ShapeType        _shpType,
                           int              _totalNumberOfShapes) 
    throws FileNotFoundException, 
           IOException,
           Exception
    {
        try {
            this.shapeType = _shpType;
            this.handler = _shpType.getShapeHandler();
        } catch (ShapefileException se) {
            throw new RuntimeException("Unexpected Exception when getting ShapeHandler");
        }
        
        File directory = new File(_outputDirectory);
        // create directory if necessary
        if (!directory.exists()) {
            directory.mkdirs();
        }
        shpFile     = new File(FileUtils.FileNameBuilder(_outputDirectory,_fileName,".shp"));
        shxFile     = new File(FileUtils.FileNameBuilder(_outputDirectory,_fileName,".shx"));
        shpOutput   = new FileOutputStream(shpFile);
        shpChannel  = shpOutput.getChannel();
        shxOutput   = new FileOutputStream(shxFile);
        shxChannel  = shxOutput.getChannel();
        header      = new ShapefileHeader();
        shpEnvelope = new Envelope();
        ShapefileWriter.setOffset(50);
        totalNumberOfShapes = _totalNumberOfShapes;
        shapesWritten = 0;
        allocateBuffers();
        writeHeaders(Math.max(totalNumberOfShapes,shapesWritten));
    }

    public void setTotalShapes(int _totalShapes   ) {
        this.totalNumberOfShapes = _totalShapes;
    }
    
    public int getTotalShapesWritten() {
        return this.totalNumberOfShapes;
    }

    public Envelope getEnvelope() {
        return this.shpEnvelope;
    }
    
    @SuppressWarnings("unused")
	private void setEnvelope(Envelope _envelope) {
        if ( _envelope != null )
          this.shpEnvelope.expandToInclude(_envelope);
    }
        
    public FileChannel getShpChannel() {
      return shpChannel; 
    }
    
    public FileChannel getShxChannel() {
      return shxChannel;
    }
    
    /**
     * Allocate some buffers for writing.
     */
    private void allocateBuffers() {
        shapeBuffer = NIOUtilities.allocate(16 * 1024);
        indexBuffer = NIOUtilities.allocate(100);
    }
    
    /**
     * Make sure our shape buffer is of size.
     */ 
    private void checkShapeBuffer(int _sizeBytes) {
      if (shapeBuffer.capacity() < _sizeBytes) {
          if( shapeBuffer!=null ) {
              NIOUtilities.clean(shapeBuffer,false);
          }
          shapeBuffer = NIOUtilities.allocate(_sizeBytes);
      }
    }
       

    /**
     * Drain internal buffers into underlying channels.
     */
    private void drain() 
    throws IOException 
    {
        shapeBuffer.flip();
        indexBuffer.flip();
        while (shapeBuffer.remaining() > 0) {
            shpChannel.write(shapeBuffer);
        }
        while (indexBuffer.remaining() > 0) {
            shxChannel.write(indexBuffer);
        }
        shapeBuffer.flip().limit(shapeBuffer.capacity());
        indexBuffer.flip().limit(indexBuffer.capacity());
    }

    /**
     * Allocate internal buffers and position the channels to the beginning or
     * the record section of the shapefile. The headers MUST be rewritten after
     * this operation, or the file may be corrupt...
     */
    public void skipHeaders() throws IOException {
        if (shapeBuffer == null)
            allocateBuffers();
        shpChannel.position(100);
        shxChannel.position(100);
    }
    
    /**
     * Write the headers for this shapefile including the bounds, shape type,
     * the number of geometries and the total fileLength (in actual bytes, NOT
     * 16 bit words).
     */
    public void writeHeaders(int _numberOfGeometries) 
    throws IOException 
    {

        if (shapeBuffer == null) {
            allocateBuffers();
        }
        
        Envelope shapeEnvelope = this.getEnvelope();

        header.write(this.shapeBuffer,
                     this.shapeType, 
                     this.fileLength / 2,
                     shapeEnvelope);

        header.write(this.indexBuffer, 
                     this.shapeType,
                     50 + 4 * _numberOfGeometries, 
                     shapeEnvelope);

        ShapefileWriter.setOffset(50);
        
        shpChannel.position(0);
        shxChannel.position(0);

        drain();
    }

    /**
     * Write a single Geometry to this shapefile. The Geometry must be 
     * compatable with the ShapeType assigned during the writing of the headers.
     */
    private void writeGeometry(Geometry _geom) 
    throws IOException 
    {
        int length = 0;
        bufferPosition = shapeBuffer.position();
        this.shapesWritten++;
        if(_geom == null) {
            length = writeNullGeometry(this.shapesWritten);
        } else {
            length = writeNonNullGeometry(_geom,this.shapesWritten);
            Envelope envelope = _geom.getEnvelopeInternal();
            if (!envelope.isNull()) {
                this.shpEnvelope.expandToInclude(envelope);
            }
        }
        assert (length * 2 == (shapeBuffer.position() - bufferPosition) - 8);
        bufferPosition = shapeBuffer.position();
        
        // write to the shx
        indexBuffer.putInt(ShapefileWriter.getOffset());
        indexBuffer.putInt(length);
        offset += length + 4;
        drain();
        assert (shapeBuffer.position() == 0);
    }

    /**
     * Writes a valid geometry to the shapefile - has associated DBF record.
     * @param _geom
     * @return
     * @method writeNonNullGeometry
     * @author GeoTools
     */
    private int writeNonNullGeometry(Geometry _geom,
                                     int      _shapeNumber) 
    {
        int length = handler.getLength(_geom);
        int size = length + 8;
        checkShapeBuffer(size);
        fileLength += size;
        length /= 2;
        shapeBuffer.order(ByteOrder.BIG_ENDIAN);
        shapeBuffer.putInt(_shapeNumber);
        shapeBuffer.putInt(length);
        shapeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        shapeBuffer.putInt(this.shapeType.id);
        handler.write(shapeBuffer, _geom);
        return length;
    }

    /**
     * Writes a null geometry to the shapefile - has associated DBF record.
     * @return shapeBuffer length
     * @throws IOException
     * @method writeNullGeometry
     * @author GeoTools
     */
    protected int writeNullGeometry(int _shapeNumber) 
    throws IOException 
    {
        int length = 4;
        int size = length + 8;
        checkShapeBuffer(size);
        fileLength += size;
        length /= 2;
        shapeBuffer.order(ByteOrder.BIG_ENDIAN);
        shapeBuffer.putInt(_shapeNumber);
        shapeBuffer.putInt(length);
        shapeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        shapeBuffer.putInt(ShapeType.NULL.id);
        return length;
    }

    /**
     * Writes collection of geometries to shapefile.
     * @param _geometries
     * @throws IOException
     * @method write
     * @author Simon Greener, February 2011
     **/
    public void write(LinkedHashSet<Geometry>  _geometries) 
    throws IOException 
    {
        if ( _geometries == null || _geometries.size() == 0 ) {
            return;
        }
        Iterator<Geometry> iter = _geometries.iterator();
        while (iter.hasNext()) {
            Geometry geom = (Geometry)iter.next();
            writeGeometry(geom);
        }
    }

    /**
     * Close the underlying Channels.
     */
    public void close() 
    throws IOException
    {
        // Write envelope (not known at start) and exact feature count
        // Take shapesWritten value as it may not include unwritten nulls in original set.
        //
        //LOGGER.info("ShapesWritten=" + this.shapesWritten + " out of " + this.totalNumberOfShapes);
        writeHeaders(this.shapesWritten);  // writeHeaders calls drain()
        
        if( shpChannel!=null && shpChannel.isOpen()){
            shpChannel.close();
        }
        if( shxChannel!=null && shxChannel.isOpen()){
            shxChannel.close();
        }
        shpChannel = null;
        shxChannel = null;
        handler    = null;
        if(indexBuffer instanceof MappedByteBuffer && indexBuffer!=null) {
            NIOUtilities.clean(indexBuffer);
        }
        if(shapeBuffer instanceof MappedByteBuffer && shapeBuffer!=null) {
            NIOUtilities.clean(shapeBuffer);
        }
        indexBuffer = null;
        shapeBuffer = null;
    }

    public static int getOffset() {
        return offset;
    }

    public static void setOffset(int _offset) {
        offset = _offset;
    }
    
    public void write(GeometryCollection _geometries)
    throws IOException
    {
        if ( _geometries == null || _geometries.getNumGeometries() == 0 ) {
            return;
        }
        for (int i = 0, ii = _geometries.getNumGeometries(); i < ii; i++) {
            Geometry g = _geometries.getGeometryN(i);
            writeGeometry(g);
        }
    }
}

