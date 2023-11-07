/**
 * Registers folder and menu-items in menu.xml
 */
package org.GeoRaptor;

import org.GeoRaptor.OracleSpatial.Grid.RenderResultSet;

import oracle.dbtools.raptor.controls.grid.RaptorGridTable;
import oracle.dbtools.raptor.dialogs.actions.XMLBasedObjectAction;
import oracle.ide.Addin;


/**
 * @author olaf
 *
 */
public class Menu implements Addin {
    /* (non-Javadoc)
     * @see oracle.ide.Addin#initialize()
     */
    @SuppressWarnings("deprecation")
	public void initialize() {
        // Add context menus
        XMLBasedObjectAction.registerContextMenus(this.getClass().getResource("menu.xml"));  
        
        /* Add snippets
        URL geoSnippets = this.getClass().getResource("/org/GeoRaptor/snippets.xml");
        if (geoSnippets != null)  {
            SnippetAddin.registerSnippet(geoSnippets);
        } */
        // Create context menu for when a SELECT statement produces a grid of row data
        RaptorGridTable.addGridContextMenu(RenderResultSet.getInstance());
    }
}
