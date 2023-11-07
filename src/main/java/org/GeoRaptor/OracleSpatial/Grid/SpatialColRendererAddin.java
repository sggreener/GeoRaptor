package org.GeoRaptor.OracleSpatial.Grid;

import java.sql.Array;
import java.sql.Struct;

import org.GeoRaptor.tools.SpatialRenderer;

import oracle.dbtools.raptor.controls.cellrenderers.CellRenderingFactory;
import oracle.ide.Addin;
import oracle.sql.ARRAY;
import oracle.sql.STRUCT;


/**
 * Register a custom renderer for database objects
 */
@SuppressWarnings("deprecation")
public class SpatialColRendererAddin 
implements Addin 
{
	public void initialize() {
        // set MainSettings
        //Preferences p = MainSettings.getInstance().getPreferences();
        // register the custom renderer for SDO_GEOMETRY
        CellRenderingFactory.registerCellRenderer(Struct.class,new SpatialRenderer());
        CellRenderingFactory.registerCellRenderer(STRUCT.class,new SpatialRenderer());
        // Set up hook for SDO_DIM_ARRAY, SDO_ELEM_INFO_ARRAY, SDO_ORDINATE_ARRAY
        CellRenderingFactory.registerCellRenderer(Array.class,new SpatialRenderer());        
        CellRenderingFactory.registerCellRenderer(ARRAY.class,new SpatialRenderer());        
    }
}
