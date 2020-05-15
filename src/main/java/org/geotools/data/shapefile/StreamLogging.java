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

//import java.util.logging.Logger;
import org.geotools.util.logging.Logger;

public class StreamLogging {
    private static final Logger LOGGER=org.geotools.util.logging.Logging.getLogger("org.geotools.data.shapefile");

	private String name;
	private int open=0;
	/**
	 * The name that will appear in the debug message
	 * @param name
	 */
	public StreamLogging( String name){
		this.name=name;
	}
	
	/**
	 * Call when reader or writer is opened
	 */
	public synchronized void open(){
		open++;
		LOGGER.finest(name+" has been opened. Number open: "+open);
	}
	
	public synchronized void close(){
		open--;
		LOGGER.finest(name+" has been closed. Number open: "+open);
	}
	
}
