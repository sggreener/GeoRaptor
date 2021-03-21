package org.GeoRaptor;

import java.sql.Connection;

import javax.swing.JOptionPane;

import org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel;
import org.GeoRaptor.SpatialView.SpatialViewPanel;
import org.GeoRaptor.io.Import.ShapefileLoad;
import org.GeoRaptor.sql.DatabaseConnection;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.Strings;

import oracle.ide.Context;
import oracle.ide.Ide;
import oracle.ide.controller.Controller;
import oracle.ide.controller.IdeAction;

/**
 * 
 * @author Bessie Gong 
 * @version 24 Jul 2019
 *	Menu Hook controller
 */
public class ViewController implements Controller {
		
	private static final int            OPEN_MAP = Ide.findOrCreateCmdID("OPEN_MAP");
	private static final int MANAGE_ALL_METADATA = Ide.findOrCreateCmdID("MANAGE_ALL_METADATA");
	private static final int      LOAD_SHAPEFILE = Ide.findOrCreateCmdID("LOAD_SHAPEFILE");
	private static final int     ABOUT_GEORAPTOR = Ide.findOrCreateCmdID("ABOUT_GEORAPTOR");

	public ViewController() {
		super();
	}
	
	private void show (String message) {
		JOptionPane.showMessageDialog(null, message, Resources.getString("DIALOG_SHOW_TITLE"), JOptionPane.INFORMATION_MESSAGE);
	}

	private boolean checkConnection() {
		//Connection localConnection = DatabaseConnections.getInstance().getActiveConnection(); 
		String connectionName = DatabaseConnections.getActiveConnectionName();
		if (!Strings.isEmpty(connectionName)) {
			return true;
		}
		return false;
		
	}
	
	@Override
	public boolean handleEvent(IdeAction action, Context context) 
	{
		int cmdId = action.getCommandId();

        DatabaseConnections dcs = DatabaseConnections.getInstance();
        Connection         conn = dcs.getAnyOpenConnection();

		if (cmdId == OPEN_MAP) {
			if (checkConnection()) {			
                SpatialViewPanel svp = SpatialViewPanel.getInstance();
                svp.show();
			} else {
				show("No active connection");
			}
		} else if(cmdId == MANAGE_ALL_METADATA) {
			DatabaseConnection dc = dcs.getConnectionAt(0);  
			if (conn!=null) {
    			MetadataPanel mp = MetadataPanel.getInstance();
    			boolean   status = false;
				try {
					status = mp.initialise(conn,
							               dc.getCurrentSchema(),
                                           null, /* _objectName */
                                           null, /* _columnName */
                                           dc.getUserName());
	    			if (status == true) {
	    				mp.setVisible(true);
	    			}
				} catch (Exception e) {
					show("Could not create Metadata Dialog");
				}
    			
			} else {
				show("No connection available");
			}
		} else if (cmdId == LOAD_SHAPEFILE) {
			if (conn!=null) {
				ShapefileLoad.getInstance().initialise();
			} else {
				show("No active connection");
			}
		} else if (cmdId == ABOUT_GEORAPTOR) {
			//AboutDialog.getInstance().setVisible(true);
            AboutDialog ad = new AboutDialog(null,true);
            ad.setVisible(true);            
		}
		return true;
	}

	@Override
	public boolean update(IdeAction action, Context context) {
		action.setEnabled(true);
		return action.isEnabled();
	}

}
