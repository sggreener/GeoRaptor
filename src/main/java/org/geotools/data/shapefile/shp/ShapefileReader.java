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

import java.io.EOFException;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.geotools.data.DataSourceException;
import org.geotools.data.shapefile.Lock;
import org.geotools.data.shapefile.StreamLogging;
import org.geotools.resources.NIOUtilities;
//import org.GeoRaptor.io.Export.ShapefileWriter;

/**
 * The general use of this class is: <CODE><PRE>
 *
 * FileChannel in = new FileInputStream("thefile.dbf").getChannel();
 * ShapefileReader r = new ShapefileReader( in ) while (r.hasNext()) { Geometry
 * shape = (Geometry) r.nextRecord().shape() // do stuff } r.close();
 *
 * </PRE></CODE> You don't have to immediately ask for the shape from the record. The
 * record will contain the bounds of the shape and will only read the shape when
 * the shape() method is called. This ShapefileReader.Record is the same object
 * every time, so if you need data from the Record, be sure to copy it.
 *
 * @author jamesm
 * @author aaime
 * @author Ian Schneider
 * @source $URL: http://svn.geotools.org/tags/2.4.5/modules/plugin/shapefile/src/main/java/org/geotools/data/shapefile/shp/ShapefileReader.java $
 */
public class ShapefileReader {

	/**
	 * The reader returns only one Record instance in its lifetime. The record
	 * contains the current record information.
	 */
	public final class Record {
		int length;

		int number = 0;

		int offset; // Relative to the whole file

		int start = 0; // Relative to the current loaded buffer

		/** The minimum X value. */
		public double minX;

		/** The minimum Y value. */
		public double minY;

		/** The maximum X value. */
		public double maxX;

		/** The maximum Y value. */
		public double maxY;

		ShapeType type;

		int end = 0; // Relative to the whole file

		Object shape = null;

		/** Fetch the shape stored in this record. */
		public Object shape() {
			if (shape == null) {
				buffer.position(start);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				shape = handler.read(buffer, type);
			}
			return shape;
		}

		public int offset() {
			return offset;
		}

		/** A summary of the record. */
		public String toString() {
			return "Record " + number + " length " + length + " bounds " + minX
					+ "," + minY + " " + maxX + "," + maxY;
		}
	}

  private Lock lock;
  
	private ShapeHandler handler;

	private ShapefileHeader header;

	private ReadableByteChannel channel;

	ByteBuffer buffer;

	private ShapeType fileShapeType = ShapeType.UNDEFINED;

	private ByteBuffer headerTransfer;

	private final Record record = new Record();

	private final boolean randomAccessEnabled;

	private boolean useMemoryMappedBuffer;

	private long currentOffset = 0L;
	private StreamLogging streamLogger=new StreamLogging("Shapefile Reader");

	/**
	 * Creates a new instance of ShapeFile.
	 * 
	 * @param channel
	 *            The ReadableByteChannel this reader will use.
	 * @param strict
	 *            True to make the header parsing throw Exceptions if the
	 *            version or magic number are incorrect.
	 * @throws IOException
	 *             If problems arise.
	 * @throws ShapefileException
	 *             If for some reason the file contains invalid records.
	 */
	public ShapefileReader(ReadableByteChannel channel, 
                         boolean strict, 
                         boolean useMemoryMapped,
                         Lock lock) 
    throws IOException, ShapefileException 
  {
		this.channel = channel;
		this.useMemoryMappedBuffer=useMemoryMapped;
		streamLogger.open();
		randomAccessEnabled = channel instanceof FileChannel;
		this.lock = lock;
		lock.lockRead();
		init(strict);
	}

	/**
	 * Default constructor. Calls ShapefileReader(channel,true).
	 * 
	 * @param channel
	 * @throws IOException
	 * @throws ShapefileException
	 */
	public ShapefileReader(ReadableByteChannel channel, Lock lock)
			throws IOException, ShapefileException {
		this(channel, true, true, lock);
	}

	// convenience to peak at a header
	/**
	 * A short cut for reading the header from the given channel.
	 * 
	 * @param channel
	 *            The channel to read from.
	 * @param strict
	 *            True to make the header parsing throw Exceptions if the
	 *            version or magic number are incorrect.
	 * @throws IOException
	 *             If problems arise.
	 * @return A ShapefileHeader object.
	 */
	public static ShapefileHeader readHeader(ReadableByteChannel channel,
			boolean strict) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocateDirect(100);
		if (fill(buffer, channel) == -1) {
			throw new EOFException("Premature end of header");
		}
		buffer.flip();
		ShapefileHeader header = new ShapefileHeader();
		header.read(buffer, strict);
                NIOUtilities.clean(buffer);
		return header;
	}

	// ensure the capacity of the buffer is of size by doubling the original
	// capacity until it is big enough
	// this may be naiive and result in out of MemoryError as implemented...
	public static ByteBuffer ensureCapacity(ByteBuffer buffer, int size, boolean useMemoryMappedBuffer) {
		// This sucks if you accidentally pass is a MemoryMappedBuffer of size
		// 80M
		// like I did while messing around, within moments I had 1 gig of
		// swap...
		if (buffer.isReadOnly() || useMemoryMappedBuffer) {
			return buffer;
		}

		int limit = buffer.limit();
		while (limit < size) {
			limit *= 2;
		}
		if (limit != buffer.limit()) {
			// if (record.ready) {
			buffer = ByteBuffer.allocateDirect(limit);
			// }
			// else {
			// throw new IllegalArgumentException("next before hasNext");
			// }
		}
		return buffer;
	}

	// for filling a ReadableByteChannel
	public static int fill(ByteBuffer buffer, ReadableByteChannel channel)
			throws IOException {
		int r = buffer.remaining();
		// channel reads return -1 when EOF or other error
		// because they a non-blocking reads, 0 is a valid return value!!
		while (buffer.remaining() > 0 && r != -1) {
			r = channel.read(buffer);
		}
		if (r == -1) {
			buffer.limit(buffer.position());
		}
		return r;
	}

	private void init(boolean strict) throws IOException, ShapefileException {
		header = readHeader(channel, strict);
		fileShapeType = header.getShapeType();
		handler = fileShapeType.getShapeHandler();

		// recordHeader = ByteBuffer.allocateDirect(8);
		// recordHeader.order(ByteOrder.BIG_ENDIAN);

		if (handler == null) {
			throw new IOException("Unsuported shape type:" + fileShapeType);
		}

		if (channel instanceof FileChannel && useMemoryMappedBuffer) {
			FileChannel fc = (FileChannel) channel;
			buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			buffer.position(100);
			this.currentOffset = 0;
		} else {
			// force useMemoryMappedBuffer to false
			this.useMemoryMappedBuffer = false;
			// start with 8K buffer
			buffer = ByteBuffer.allocateDirect(8 * 1024);
			fill(buffer, channel);
			buffer.flip();
			this.currentOffset = 100;
		}

		headerTransfer = ByteBuffer.allocate(8);
		headerTransfer.order(ByteOrder.BIG_ENDIAN);

		// make sure the record end is set now...
		record.end = this.toFileOffset(buffer.position());
	}

	/**
	 * Get the header. Its parsed in the constructor.
	 * 
	 * @return The header that is associated with this file.
	 */
	public ShapefileHeader getHeader() {
		return header;
	}

	// do important cleanup stuff.
	// Closes channel !
	/**
	 * Clean up any resources. Closes the channel.
	 * 
	 * @throws IOException
	 *             If errors occur while closing the channel.
	 */
	public void close() throws IOException {
		lock.unlockRead();
		if (channel.isOpen()) {
			channel.close();
			streamLogger.close();
		}
		if (buffer instanceof MappedByteBuffer) {
			NIOUtilities.clean(buffer);
		}
		channel = null;
		header = null;
	}

	public boolean supportsRandomAccess() {
		return randomAccessEnabled;
	}

	/**
	 * If there exists another record. Currently checks the stream for the
	 * presence of 8 more bytes, the length of a record. If this is true and the
	 * record indicates the next logical record number, there exists more
	 * records.
	 * 
	 * @throws IOException
	 * @return True if has next record, false otherwise.
	 */
	public boolean hasNext() throws IOException {
		return this.hasNext(true);
	}

	/**
	 * If there exists another record. Currently checks the stream for the
	 * presence of 8 more bytes, the length of a record. If this is true and the
	 * record indicates the next logical record number (if checkRecord == true),
	 * there exists more records.
	 * 
	 * @param checkRecno
	 *            If true then record number is checked
	 * @throws IOException
	 * @return True if has next record, false otherwise.
	 */
	private boolean hasNext(boolean checkRecno) throws IOException {
		// mark current position
		int position = buffer.position();

		// ensure the proper position, regardless of read or handler behavior
		buffer.position(this.toBufferOffset(record.end));

		// no more data left
		if (buffer.remaining() < 8)
			return false;

		// looks good
		boolean hasNext = true;
		if (checkRecno) {
			// record headers in big endian
			buffer.order(ByteOrder.BIG_ENDIAN);
			hasNext = buffer.getInt() == record.number + 1;
		}

		// reset things to as they were
		buffer.position(position);

		return hasNext;
	}

	/**
	 * Transfer (by bytes) the data at the current record to the
	 * ShapefileWriter.
	 * 
	 * @param bounds
	 *            double array of length four for transfering the bounds into
	 * @return The length of the record transfered in bytes
	 */
//	public int transferTo(ShapefileWriter writer, int recordNum, double[] bounds)
//			throws IOException {
//
//		buffer.position(this.toBufferOffset(record.end));
//		buffer.order(ByteOrder.BIG_ENDIAN);
//
//		buffer.getInt(); // record number
//		int rl = buffer.getInt();
//		int mark = buffer.position();
//		int len = rl * 2;
//
//		buffer.order(ByteOrder.LITTLE_ENDIAN);
//		ShapeType recordType = ShapeType.forID(buffer.getInt());
//
//		if (recordType.isMultiPoint()) {
//			for (int i = 0; i < 4; i++) {
//				bounds[i] = buffer.getDouble();
//			}
//		} else if (recordType != ShapeType.NULL) {
//			bounds[0] = bounds[1] = buffer.getDouble();
//			bounds[2] = bounds[3] = buffer.getDouble();
//		}
//
//		// write header to shp and shx
//		headerTransfer.position(0);
//		headerTransfer.putInt(recordNum).putInt(rl).position(0);
//		writer.getShpChannel().write(headerTransfer);
//		headerTransfer.putInt(0, ShapefileWriter.getOffset()).position(0);
//		ShapefileWriter.setOffset(ShapefileWriter.getOffset() + rl + 4);
//		writer.getShxChannel().write(headerTransfer);
//
//		// reset to mark and limit at end of record, then write
//		buffer.position(mark).limit(mark + len);
//		writer.getShpChannel().write(buffer);
//		buffer.limit(buffer.capacity());
//
//		record.end = this.toFileOffset(buffer.position());
//		record.number++;
//
//		return len;
//	}

	/**
	 * Fetch the next record information.
	 * 
	 * @throws IOException
	 * @return The record instance associated with this reader.
	 */
	public Record nextRecord() throws IOException {

		// need to update position
		buffer.position(this.toBufferOffset(record.end));

		// record header is big endian
		buffer.order(ByteOrder.BIG_ENDIAN);

		// read shape record header
		int recordNumber = buffer.getInt();
		// silly ESRI say contentLength is in 2-byte words
		// and ByteByffer uses bytes.
		// track the record location
		int recordLength = buffer.getInt() * 2;

		if (!buffer.isReadOnly() && !useMemoryMappedBuffer) {
			// capacity is less than required for the record
			// copy the old into the newly allocated
			if (buffer.capacity() < recordLength + 8) {
				this.currentOffset += buffer.position();
				ByteBuffer old = buffer;
				// ensure enough capacity for one more record header
				buffer = ensureCapacity(buffer, recordLength + 8, useMemoryMappedBuffer);
				buffer.put(old);
                                NIOUtilities.clean(old);
				fill(buffer, channel);
				buffer.position(0);
			} else
			// remaining is less than record length
			// compact the remaining data and read again,
			// allowing enough room for one more record header
			if (buffer.remaining() < recordLength + 8) {
				this.currentOffset += buffer.position();
				buffer.compact();
				fill(buffer, channel);
				buffer.position(0);
			}
		}

		// shape record is all little endian
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		// read the type, handlers don't need it
		ShapeType recordType = ShapeType.forID(buffer.getInt());

		// this usually happens if the handler logic is bunk,
		// but bad files could exist as well...
		if (recordType != ShapeType.NULL && recordType != fileShapeType) {
			throw new IllegalStateException("ShapeType changed illegally from "
					+ fileShapeType + " to " + recordType);
		}

		// peek at bounds, then reset for handler
		// many handler's may ignore bounds reading, but we don't want to
		// second guess them...
		buffer.mark();
		if (recordType.isMultiPoint()) {
			record.minX = buffer.getDouble();
			record.minY = buffer.getDouble();
			record.maxX = buffer.getDouble();
			record.maxY = buffer.getDouble();
		} else if (recordType != ShapeType.NULL) {
			record.minX = record.maxX = buffer.getDouble();
			record.minY = record.maxY = buffer.getDouble();
		}
		buffer.reset();

		record.offset = record.end;
		// update all the record info.
		record.length = recordLength;
		record.type = recordType;
		record.number = recordNumber;
		// remember, we read one int already...
		record.end = this.toFileOffset(buffer.position()) + recordLength - 4;
		// mark this position for the reader
		record.start = buffer.position();
		// clear any cached shape
		record.shape = null;

		return record;
	}

	/**
	 * Needs better data, what is the requirements for offset?
	 * 
	 * @param offset
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 */
	public void goTo(int offset) throws IOException,
			UnsupportedOperationException {
		if (randomAccessEnabled) {
			if (this.useMemoryMappedBuffer) {
				buffer.position(offset);
			} else {
				/*
				 * Check to see if requested offset is already loaded; ensure
				 * that record header is in the buffer
				 */
				if (this.currentOffset <= offset
						&& this.currentOffset + buffer.limit() >= offset + 8) {
					buffer.position(this.toBufferOffset(offset));
				} else {
					FileChannel fc = (FileChannel) this.channel;
					fc.position(offset);
					this.currentOffset = offset;
					buffer.position(0);
					fill(buffer, fc);
					buffer.position(0);
				}
			}

			int oldRecordOffset = record.end;
			record.end = offset;
			try {
				hasNext(false); // don't check for next logical record equality
			} catch (IOException ioe) {
				record.end = oldRecordOffset;
				throw ioe;
			}
		} else {
			throw new UnsupportedOperationException("Random Access not enabled");
		}
	}

	/**
	 * TODO needs better java docs!!! What is offset?
	 * 
	 * @param offset
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 */
	public Object shapeAt(int offset) throws IOException,
			UnsupportedOperationException {
		if (randomAccessEnabled) {
			this.goTo(offset);
			return nextRecord().shape();
		}
		throw new UnsupportedOperationException("Random Access not enabled");
	}

	/**
	 * Sets the current location of the byteStream to offset and returns the
	 * next record. Usually used in conjuctions with the shx file or some other
	 * index file.
	 * 
	 * @param offset
	 *            If using an shx file the offset would be: 2 *
	 *            (index.getOffset(i))
	 * @return The record after the offset location in the bytestream
	 * @throws IOException
	 *             thrown in a read error occurs
	 * @throws UnsupportedOperationException
	 *             thrown if not a random access file
	 */

	public Record recordAt(int offset) throws IOException,
			UnsupportedOperationException {
		if (randomAccessEnabled) {
			this.goTo(offset);
			return nextRecord();
		}
		throw new UnsupportedOperationException("Random Access not enabled");
	}

	/**
	 * Converts file offset to buffer offset
	 * 
	 * @param offset
	 *            The offset relative to the whole file
	 * @return The offset relative to the current loaded portion of the file
	 */
	private int toBufferOffset(int offset) {
		return (int) (offset - this.currentOffset);
	}

	/**
	 * Converts buffer offset to file offset
	 * 
	 * @param offset
	 *            The offset relative to the buffer
	 * @return The offset relative to the whole file
	 */
	private int toFileOffset(int offset) {
		return (int) (this.currentOffset + offset);
	}

	/**
	 * Parses the shpfile counting the records.
	 * 
	 * @return the number of non-null records in the shapefile
	 */
	public int getCount(int count) throws DataSourceException {
		try {
			if (channel == null)
				return -1;
			count = 0;

			for (int tmp = readRecord(); tmp != -1; tmp = readRecord())
				count += tmp;

		} catch (IOException ioe) {
			count = -1;
			// What now? This seems arbitrarily appropriate !
			throw new DataSourceException("Problem reading shapefile record",
					ioe);
		}
		return count;
	}

	/**
	 * Reads a record and returns 1 if the record is not null.
	 * 
	 * @param channel
	 *            the io channel
	 * @param buffer
	 * @return 0 if null feature; 1 if valid feature; -1 if end of file reached.
	 * @throws IOException
	 */
	private int readRecord() throws IOException {
		if (!fillBuffer())
			return -1;
		// burn the record number
		buffer.getInt();
		if (!fillBuffer())
			return -1;
		int recordlength = buffer.getInt() * 2;
		// Going to read the first 4 bytes of the record so
		// subtract that from the record length
		recordlength -= 4;
		if (!fillBuffer())
			return -1;

		// read record type (used to determine if record is a null record)
		int type = buffer.getInt();
		// go to end of record
		while (buffer.limit() < buffer.position() + recordlength) {
			recordlength -= buffer.limit() - buffer.position();
			buffer.clear();
			if (channel.read(buffer) < 1) {
				return -1;
			}
		}
		buffer.position(buffer.position() + recordlength);

		// return 0 if record is null. Null records should be counted.
		if (type == 0) {
			// this is a null feature
			return 0;
		}
		return 1;
	}

	/**
	 * Ensures that there is at least 1 integer (4 bytes) is in the buffer.
	 * 
	 * @return true if there is data in the buffer, false less than a byte is in
	 *         the buffer.
	 * @throws IOException
	 *             if exception during reading occurs.
	 */
	private boolean fillBuffer() throws IOException {
		int result = 1;
		if (buffer.limit() <= buffer.position() + 4) {
			result = fill(buffer, channel);
		}
		return result > 0;
	}

	/**
	 * @param handler
	 *            The handler to set.
	 */
	public void setHandler(ShapeHandler handler) {
		this.handler = handler;
	}
}
