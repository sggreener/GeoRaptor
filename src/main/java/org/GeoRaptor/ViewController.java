package org.GeoRaptor;

import java.sql.Connection;

import javax.swing.JOptionPane;

import org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel;
import org.GeoRaptor.io.Import.ShapefileLoad;
import org.GeoRaptor.io.Import.ShapefileLoadEmpty;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.Strings;
import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;

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
	
	private static final Logger LOGGER = Logging.getLogger(ViewController.class.getName());
	
	/**
	 * VIEW_SUBMENU_1_ACTION_ID Open Map
	 * VIEW_SUBMENU_2_ACTION_ID Manage All Metadata
	 * VIEW_SUBMENU_3_ACTION_ID Load Shapefile
	 * VIEW_SUBMENU_4_ACTION_ID About GeoRaptor
	 */
	private static final int OPEN_MAP = Ide.findOrCreateCmdID("OPEN_MAP");
	private static final int MANAGE_ALL_METADATA = Ide.findOrCreateCmdID("MANAGE_ALL_METADATA");
	private static final int LOAD_SHAPEFILE = Ide.findOrCreateCmdID("LOAD_SHAPEFILE");
	private static final int ABOUT_GEORAPTOR = Ide.findOrCreateCmdID("ABOUT_GEORAPTOR");
	
	

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
	public boolean handleEvent(IdeAction action, Context context) {
		int cmdId = action.getCommandId();
	
		if (cmdId == OPEN_MAP) {
			if (checkConnection()) {			
				show("Action CmdID: " + cmdId + " Name: " + action.getValue("Name"));
			}else {
				show("No active connection");
			}
		}else if(cmdId == MANAGE_ALL_METADATA){
			if (checkConnection()) {
//				MenuAction menuAction = new MenuAction();
//				menuAction.setArgs("metadata");
//				menuAction.launch();
				show("Action CmdID: " + cmdId + " Name: " + action.getValue("Name"));
			}else {
				show("No active connection");
			}
		}else if (cmdId == LOAD_SHAPEFILE) {
			if (checkConnection()) {			
				ShapefileLoadEmpty.getInstance().initialise();
			}else {
				show("No active connection");
			}
		}else if (cmdId == ABOUT_GEORAPTOR) {
			AboutDialog.getInstance().setVisible(true);
		}
		return true;
	}

	@Override
	public boolean update(IdeAction action, Context context) {
		action.setEnabled(true);
		return action.isEnabled();
	}

}
