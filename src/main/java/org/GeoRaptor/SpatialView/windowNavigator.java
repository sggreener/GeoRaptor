package org.GeoRaptor.SpatialView;

import java.awt.Toolkit;

import java.util.ArrayList;

import org.GeoRaptor.Constants;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;

import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;


public class windowNavigator {

    private final Logger LOGGER = Logging.getLogger("org.GeoRaptor.SpatialView.windowNavigator");
    
    private ArrayList<Envelope> mbrs = null;
    private int current = 0;

    /**
     * @param _entries Number of entries in array list
     */
    public windowNavigator(int _entries) 
    {
        int entries = Math.abs(_entries);
        if (entries==0) {
            entries = Preferences.getInstance()==null ? Constants.VAL_MBR_SAVE_SIZE : Preferences.getInstance().getMBRSaveSize();
        } 
        this.mbrs = new ArrayList<Envelope>(entries);
        for (int i=0;i<entries;i++) {
            this.mbrs.add(null);
        }
    }


    /**
     * @return Number of mbr entries in array list
     */
    public synchronized int getSize() {
      return this.mbrs.size();
    }

    /**
     * @return False means no more empty entries exist in array otherwise entries exist.
     */
    public synchronized boolean hasNext() {
        return this.current < (this.mbrs.size()-1);
    }

    /**
     * @return True if the current pointer is at the beginning of the list (ie first entry)
     */
    public synchronized boolean hasPrevious() {
        return this.current > 0;
    }

    /**
     * @param _mbr RectangleDouble to be added to array list
     */
    public synchronized void add(Envelope _mbr) 
    { 
        if ( _mbr == null ) {
            // Do nothing
            return;
        }
        
        // Value held by current is the position for the new mbr
        //
        if (this.current==0 && this.mbrs.get(this.current)==null) {
            this.mbrs.set(this.current,
                          new Envelope(_mbr));
            // LOGGER.info("windowNavigator.add(0): " + _mbr.toString());
            return;
        }

        // Check if current mbr = new mbr
        Envelope mbrCurrent = this.mbrs.get(this.current);
        if ( mbrCurrent.equals(_mbr) ) {
            // Don't add if it is equal to the existing one at the end of the list
            LOGGER.debug("MBR Not Added as it is equal to current");
            return;
        }
        
        double diff = mbrCurrent.difference(_mbr);
        if ( diff < 10.0 ) {
            LOGGER.debug("MBR Percentage Change " + diff + " does not warrant recording of the new window");
            return;
        }
        
        if (hasNext()) {
            // OK, set next array position with passed in value
            this.current++;
            this.mbrs.set(this.current,
                          new Envelope(_mbr));
            // LOGGER.debug("windowNavigator.add(" + this.current + ")="+_mbr.toString());
            return;
        } else {
          // There is no more room for the mbr
          LOGGER.debug("windowNavigator is FULL Remove First (0)");
          this.mbrs.remove(0); // Remove oldest
          // Add to end creates new element to replace removed one: ensures count = original _entries
          this.mbrs.add(new Envelope(_mbr));  
          LOGGER.debug("mbrs.add to end =>" + this.mbrs.size());
        }
    }

    /**
     * @return RectangleDouble holding MBR of previous window
     */
    public synchronized Envelope previous () {
        if (hasPrevious()) {
            current--;
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
        return new Envelope(mbrs.get(current));
    }

    /**
     * @return Gets next MBR if it exists.
     */
    public synchronized Envelope next () {
        if ( hasNext() ) {
            this.current++;
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
        return new Envelope(this.mbrs.get(this.current));
    }

    @SuppressWarnings("unused")
	private synchronized void dumpEntry(String _action) {
        LOGGER.info("Action: " + _action + " List(" + getSize()+ ") MBR(" + this.current + "): " + 
                    ((this.mbrs.get(this.current) != null) ? this.mbrs.get(this.current).toString() : "NULL"));
    }
    
    public synchronized void dump() {
        LOGGER.info("DUMP. Current: " + this.current + " mbrs.Size=" + this.mbrs.size());
        for ( int i = 0; i < this.mbrs.size(); i++ )
            if ( this.mbrs.get(i) != null ) {
                LOGGER.info("DUMP. mbrs(" + i + ")= " + this.mbrs.get(i).toString());
            }
    }
}
