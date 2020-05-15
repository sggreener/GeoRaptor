/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2005-2006, Geotools Project Managment Committee (PMC)
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.util.logging.Logger;

/**
 * A read-write lock for shapefiles so that OS file locking exceptions will not
 * ruin an attempt to update a shapefile. On windows there are often operating
 * system locking conflicts when writing to a shapefile. In order to not have
 * exceptions thrown everytime a write is made, geotools has implemented file
 * locking for shapefiles.
 * 
 * @author jeichar
 * @source $URL: http://svn.geotools.org/tags/2.4.5/modules/plugin/shapefile/src/main/java/org/geotools/data/shapefile/Lock.java $
 */
public class Lock {

    Logger logger = org.geotools.util.logging.Logging.getLogger("org.geotools.data.shapefile");

    /**
     * indicates a write is occurring
     */
    int writeLocks = 0;

    /**
     * if not null a writer is waiting for the lock or is writing.
     */
    Thread writer;
    /**
     * Thread->Owner map. If empty no read locks exist.
     */
	Map<Thread, Owner> owners = new HashMap<Thread, Owner>();

    /**
     * If the lock can be read locked the lock will be read and default
     * visibility for tests
     * 
     * @throws IOException
     */
    synchronized boolean canRead() throws IOException {
        if ( writer!=null && writer!=Thread.currentThread() )
            return false;
        if ( writer==null )
            return true;
        
        
        if ( owners.size()>1 )
            return false;

        return true;
    }

    /**
     * If the lock can be read locked the lock will be read and default
     * visibility for tests
     * 
     * @throws IOException
     */
    synchronized boolean canWrite() throws IOException {
        if( owners.size()>1 )
            return false;
        if ((canRead()) 
                && (writer == Thread.currentThread() || writer==null)) {
            if( owners.isEmpty() )
                return true;
            if( owners.containsKey(Thread.currentThread()))
                return true;
        }
        return false;
    }

    /**
     * Called by shapefileReader before a read is started and before an IOStream
     * is openned.
     * 
     * @throws IOException
     */
    public synchronized void lockRead() throws IOException {
        if (!canRead()) {
            while (writeLocks > 0 || writer != null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw (IOException) new IOException().initCause(e);
                }
            }
        }

        assertTrue("A write lock exists that is owned by another thread", canRead());
        Thread current = Thread.currentThread();
        Owner owner = owners.get(current);
        if (owner != null) {
            owner.timesLocked++;
        } else {
            owner = new Owner(current);
            owners.put(current, owner);
        }

        logger.finer("Start Read Lock:" + owner);
    }

    private void assertTrue(String message, boolean b) {
        if (!b) {
            throw new AssertionError(message);
        }
    }

    /**
     * Called by ShapefileReader after a read is complete and after the IOStream
     * is closed.
     */
    public synchronized void unlockRead() {

        assertTrue("Current thread does not have a readLock", owners
                .containsKey(Thread.currentThread()));

        Owner owner = owners.get(Thread.currentThread());
        assertTrue("Current thread has " + owner.timesLocked
                + "negative number of locks", owner.timesLocked > 0);

        owner.timesLocked--;
        if (owner.timesLocked == 0)
            owners.remove(Thread.currentThread());

        notifyAll();

        logger.finer("unlock Read:" + owner);
    }

    /**
     * Called by ShapefileDataStore before a write is started and before an
     * IOStream is openned.
     * 
     * @throws IOException
     */
    public synchronized void lockWrite() throws IOException {
        Thread currentThread = Thread.currentThread();
        if (writer == null)
            writer = currentThread;
        while (!canWrite()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw (IOException) new IOException().initCause(e);
            }

            if (writer == null)
                writer = currentThread;
        }

        if (writer == null)
            writer = currentThread;
        
        assertTrue("The current thread is not the writer",
                writer == currentThread);
        assertTrue("There are read locks not belonging to the current thread.",
                canRead());

        writeLocks++;
        logger.finer(currentThread.getName() + " is getting write lock:"
                + writeLocks);
    }

    private class Owner {
        final Thread owner;

        int timesLocked;

        Owner(Thread owner) {
            this.owner = owner;
            timesLocked = 1;
        }

        public String toString() {
            return owner.getName() + " has " + timesLocked + " locks";
        }
    }

    /**
     * default visibility for tests
     * 
     */
    synchronized int getReadLocks(Thread thread) {
        Owner owner = owners.get(thread);
        if (owner == null)
            return -1;
        return owner.timesLocked;
    }

    public synchronized void unlockWrite() {
        if (writeLocks > 0) {
            assertTrue("current thread does not own the write lock",
                    writer == Thread.currentThread());
            assertTrue("writeLock has already been unlocked", writeLocks > 0);
            writeLocks--;
            if (writeLocks == 0)
                writer = null;
        }
        logger.finer("unlock write:" + Thread.currentThread().getName());
        notifyAll();
    }
    /**
     * default visibility for tests
     * 
     */
    synchronized boolean ownWriteLock(Thread thread) {
        return writer==thread && writeLocks>0;
    }

}
