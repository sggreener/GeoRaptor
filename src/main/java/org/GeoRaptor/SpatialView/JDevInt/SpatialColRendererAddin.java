package org.GeoRaptor.SpatialView.JDevInt;

import java.sql.Array;
import java.sql.Struct;

import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;

import oracle.dbtools.raptor.controls.cellrenderers.CellRenderingFactory;
import oracle.ide.Addin;


/**
 * Register a custom renderer for database objects
 */
public class SpatialColRendererAddin implements Addin {
    public void initialize() {
        // set MainSettings
        @SuppressWarnings("unused")
		Preferences p = MainSettings.getInstance().getPreferences();
        // register the custom renderer for SDO_GEOMETRY
        CellRenderingFactory.registerCellRenderer(Struct.class,
                                                  new SpatialRenderer());
        // Set up hook for SDO_DIM_ARRAY, SDO_ELEM_INFO_ARRAY, SDO_ORDINATE_ARRAY
        CellRenderingFactory.registerCellRenderer(Array.class, 
                                                  new SpatialRenderer());        
    }
}
