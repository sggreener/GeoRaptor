package org.GeoRaptor.SpatialView.JDevInt;

import oracle.dbtools.raptor.controls.cellrenderers.CellRenderingFactory;

import oracle.ide.Addin;

import oracle.sql.ARRAY;
import oracle.sql.STRUCT;

import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;


/**
 * Register a custom renderer for database objects
 */
@SuppressWarnings("deprecation")
public class SpatialColRendererAddin implements Addin {
    public void initialize() {
        // set MainSettings
        @SuppressWarnings("unused")
		Preferences p = MainSettings.getInstance().getPreferences();
        // register the custom renderer for SDO_GEOMETRY
        CellRenderingFactory.registerCellRenderer(STRUCT.class,
                                                  new SpatialRenderer());
        // Set up hook for SDO_DIM_ARRAY, SDO_ELEM_INFO_ARRAY, SDO_ORDINATE_ARRAY
        CellRenderingFactory.registerCellRenderer(ARRAY.class, 
                                                  new SpatialRenderer());        
    }
}
