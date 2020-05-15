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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
//import java.util.logging.Logger;
import org.geotools.util.logging.Logger;

import org.geotools.data.shapefile.StreamLogging;
import org.geotools.resources.NIOUtilities;


/** IndexFile parser for .shx files.<br>
 * For now, the creation of index files is done in the ShapefileWriter. But this
 * can be used to access the index.<br>
 * For details on the index file, see <br>
 * <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf"><b>"ESRI(r) Shapefile - A Technical Description"</b><br>
 * * <i>'An ESRI White Paper . May 1997'</i></a>
 *
 * @author Ian Schneider
 * @source $URL: http://svn.geotools.org/tags/2.4.5/modules/plugin/shapefile/src/main/java/org/geotools/data/shapefile/shp/IndexFile.java $
 */
public class IndexFile {
	private static final Logger LOGGER = 
	      org.geotools.util.logging.Logging.getLogger("org.geotools.data.shapefile");
	  
	  private static final int RECS_IN_BUFFER = 2000;
	  
	  private boolean useMemoryMappedBuffer;
	  private FileChannel channel;
	  private int channelOffset;
	  private ByteBuffer buf = null;
	  private int lastIndex = -1;
	  private int recOffset;
	  private int recLen;
	  private ShapefileHeader header = null;
	  private int[] content;
      private StreamLogging streamLogger=new StreamLogging("IndexFile");


	  /** Load the index file from the given channel.
	   * @param channel The channel to read from.
	   * @throws IOException If an error occurs.
	   */  
	  public IndexFile( ReadableByteChannel channel ) throws IOException {
	    this(channel, false);
	  }

	  /** Load the index file from the given channel.
	   * @param channel The channel to read from.
	   * @throws IOException If an error occurs.
	   */  
	  public IndexFile(ReadableByteChannel channel, boolean useMemoryMappedBuffer) 
	  throws IOException 
	  {
	    this.useMemoryMappedBuffer = useMemoryMappedBuffer;
	    streamLogger.open();
	    readHeader(channel);
	    if (channel instanceof FileChannel) {
	        
	        this.channel = (FileChannel)channel;
	        if (useMemoryMappedBuffer) {
	            LOGGER.finest("Memory mapping file...");
	            this.buf = this.channel.map(FileChannel.MapMode.READ_ONLY, 
	                                        0, this.channel.size());
	        
	            this.channelOffset = 0;
	        } else {
	            LOGGER.finest("Reading from file...");
	            this.buf = ByteBuffer.allocateDirect(8 * RECS_IN_BUFFER);
	            this.channelOffset = 100;
	        }
	        
	        
	    } else {
	        LOGGER.finest("Loading all shx...");
	        readRecords(channel);
	    }
	  }
	  
	  /** Get the header of this index file.
	   * @return The header of the index file.
	   */  
	  public ShapefileHeader getHeader() {
	    return header;
	  }
	  
	  private void readHeader(ReadableByteChannel channel) throws IOException {
	    ByteBuffer buffer = ByteBuffer.allocateDirect(100);
	    while (buffer.remaining() > 0) {
	      channel.read(buffer);
	    }
	    buffer.flip();
	    header = new ShapefileHeader();
	    header.read(buffer, true);
            
            NIOUtilities.clean(buffer);
	  }
	  
	  private void readRecords(ReadableByteChannel channel) throws IOException {
	    int remaining = (header.getFileLength() * 2) - 100;
	    ByteBuffer buffer = ByteBuffer.allocateDirect(remaining);
	    buffer.order(ByteOrder.BIG_ENDIAN);
	    while (buffer.remaining() > 0) {
	      channel.read(buffer);
	    }
	    buffer.flip();
	    int records = remaining / 4;
	    content = new int[ records ];
	    IntBuffer ints = buffer.asIntBuffer();
	    ints.get(content);
            NIOUtilities.clean(buffer);
	  }
	  
	  private void readRecord(int index) throws IOException {
	      int pos = 100 + index * 8;
	      if (this.useMemoryMappedBuffer) {
	          
	      } else {
	          if (pos-this.channelOffset<0 || this.channelOffset + buf.limit() <= pos || this.lastIndex == -1) {
	              LOGGER.finest("Filling buffer...");
	              this.channelOffset = pos;
	              this.channel.position(pos);
	              buf.clear();
	              this.channel.read(buf);
	              buf.flip();
	          }
	      }
	      
	      buf.position(pos - this.channelOffset);
	      this.recOffset = buf.getInt();
	      this.recLen = buf.getInt();
	      this.lastIndex = index;
	  }
	  
	  public void close() throws IOException {
	    if (channel != null && channel.isOpen()) {
	      channel.close();
		  streamLogger.close();
		    
	      if (buf instanceof MappedByteBuffer) {
	        NIOUtilities.clean(buf);
	      } else {
	        buf.clear();
	      }
	    }
	    this.buf = null;
	    this.content = null;
	  }

	  /**
	   * @see java.lang.Object#finalize()
	   */
	  protected void finalize() throws Throwable {
	      this.close();
	      super.finalize();
	  }
	  /** Get the number of records in this index.
	   * @return The number of records.
	   */  
	  public int getRecordCount( ) {
	    return (header.getFileLength() * 2 - 100) / 8;
	  }
	  
	  /** Get the offset of the record (in 16-bit words).
	   * @param index The index, from 0 to getRecordCount - 1
	   * @return The offset in 16-bit words.
	   * @throws IOException
	   */  
	  public int getOffset( int index ) throws IOException {
	      int ret = -1;
	      
	      if (this.channel != null) {
	          if (this.lastIndex != index) {
	              this.readRecord(index);
	          }
	          
	          ret = this.recOffset;
	      } else {
	          ret = content[2 * index];
	      }
	      
	    return ret;
	  }

	  /** Get the offset of the record (in real bytes, not 16-bit words).
	   * @param index The index, from 0 to getRecordCount - 1
	   * @return The offset in bytes.
	   * @throws IOException
	   */  
	  public int getOffsetInBytes(int index) throws IOException {
	      return this.getOffset(index) * 2;
	  }
	  
	  /** Get the content length of the given record in bytes, not 16 bit words.
	   * @param index The index, from 0 to getRecordCount - 1
	   * @return The lengh in bytes of the record.
	   * @throws IOException
	   */  
	  public int getContentLength( int index) throws IOException {
	      int ret = -1;
	      
	      if (this.channel != null) {
	          if (this.lastIndex != index) {
	              this.readRecord(index);
	          }
	          
	          ret = this.recLen;
	      } else {
	          ret = content[2 * index + 1];
	      }

	    return ret;
	  }
	  
  
}
