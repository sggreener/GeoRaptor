package org.GeoRaptor;

import oracle.ide.Addin;
import oracle.ide.cmd.ExitCommand;
import oracle.ide.cmd.ShutdownHook;

public final class PreferenceAddin 
        implements Addin 
{
    /**
     * @author Simon Greener April 29th 2010
     *          Removed UI load stuff just so can use this startup hook 
     *          to load and write out Spatial View layer preferences from/to XML.
     *          NOTE: GeoRaptor\Settings.xml is no longer used
     *          NOTE: This hook is needed to force load of preferences/settings
     *          when GeoRaptor loads rather than when Tools\Preferences\GeoRaptor is selected
     *          by the user.
    **/
    public void initialize()
    {
        // Load Spatial View Settings 
        MainSettings.getInstance().load();
        // add listener for application exit. Before exit, save properties
        ExitCommand.addShutdownHook( new GeoRaptorShutdownHook());        
    }

    /**
     * Listener for application exit
     * @author Simon Greener April 29th 2010
     *          Changed name from ShutdownHookTest to GeoRaptorShutdownHook
     */
    class GeoRaptorShutdownHook implements ShutdownHook {

        public void shutdown() {
            // Save Spatial View settings 
             MainSettings.getInstance().save();
        }

        public boolean canShutdown() {
             // do not permit application exit
            return true;
        }
    }    
}