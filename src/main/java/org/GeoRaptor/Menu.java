/**
 * Registers folder and menu-items in menu.xml
 */
package org.GeoRaptor;

import java.net.URL;

import oracle.dbtools.raptor.controls.grid.RaptorGridTable;
import oracle.dbtools.raptor.dialogs.actions.XMLBasedObjectAction;

import oracle.dbtools.raptor.snippet.SnippetAddin;

import oracle.ide.Addin;

import org.GeoRaptor.OracleSpatial.Grid.RenderResultSet;


/**
 * @author olaf
 *
 */
public class Menu implements Addin {
    /* (non-Javadoc)
     * @see oracle.ide.Addin#initialize()
     */
    public void initialize() {
        // Add context menus
        XMLBasedObjectAction.registerContextMenus(this.getClass().getResource("menu.xml"));  
        
        /* Add snippets
        URL geoSnippets = this.getClass().getResource("/org/GeoRaptor/snippets.xml");
        if (geoSnippets != null)  {
            System.out.println("Menu: Registering: " + geoSnippets.toString());
            //  Registering: jar:file:/F:/oracle/sqldeveloper32/sqldeveloper/extensions/org.GeoRaptor.jar!/org/GeoRaptor/snippets.xml
            SnippetAddin.registerSnippet(geoSnippets);
        } */
        // Create context menu for when a SELECT statement produces a grid of row data
        //FormatRegistry.registerFormater(new GMLFormatter());
        //FormatRegistry.registerFormater(new KMLFormatter());
        RaptorGridTable.addGridContextMenu(RenderResultSet.getInstance());
    }
}
