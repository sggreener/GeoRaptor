package org.GeoRaptor;

import javax.swing.JOptionPane;

import org.GeoRaptor.OracleSpatial.CreateSpatialIndex.ManageSpatialIndex;
import org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.ValidateSDOGeometryEmpty;
import org.GeoRaptor.io.Import.ShapefileLoadEmpty;

import oracle.ide.Context;
import oracle.ide.Ide;
import oracle.ide.controller.Controller;
import oracle.ide.controller.IdeAction;

/**
 * @author Bessie Gong Email: sadbessie@gmail.com
 * @version 11 Sep 2019
 *
 *	User Object Hooks controller
 */
public class TableContextMenuController implements Controller{
	
	

	private static final int ZOOM_TO_MAP = Ide.findOrCreateCmdID("ZOOM_TO_MAP");
	private static final int ADD_TO_MAP = Ide.findOrCreateCmdID("ADD_TO_MAP");
	private static final int CREATE_SPATIAL_INDEX = Ide.findOrCreateCmdID("CREATE_SPATIAL_INDEX");
	private static final int DROP_SPATIAL_INDEX = Ide.findOrCreateCmdID("DROP_SPATIAL_INDEX");
	private static final int MANAGE_METADATA = Ide.findOrCreateCmdID("MANAGE_METADATA");
	private static final int DROP_METADATA = Ide.findOrCreateCmdID("DROP_METADATA");
	private static final int EXPORT = Ide.findOrCreateCmdID("EXPORT");
	private static final int VALIDATE_GEOMETRY = Ide.findOrCreateCmdID("VALIDATE_GEOMETRY");
	private static final int IMPORT_SHAPEFILE = Ide.findOrCreateCmdID("IMPORT_SHAPEFILE");

	private void show (String message) {
		JOptionPane.showMessageDialog(null, message, Resources.getString("DIALOG_SHOW_TITLE"), JOptionPane.INFORMATION_MESSAGE);
	}
	@Override
	public boolean handleEvent(IdeAction action, Context context) {
		int cmdId = action.getCommandId();
		
		if (cmdId == ZOOM_TO_MAP) {
			show("Action CmdID: " + cmdId + " Name: " + action.getValue("Name"));
		}else if(cmdId == ADD_TO_MAP){
			show("Action CmdID: " + cmdId + " Name: " + action.getValue("Name"));
		}else if (cmdId == CREATE_SPATIAL_INDEX) {
			ManageSpatialIndex.getInstance().setVisible(true);
		}else if (cmdId == DROP_SPATIAL_INDEX) {
			ManageSpatialIndex.getInstance().dropIndex(null, "", "", "", "", true);
		}else if (cmdId == MANAGE_METADATA) {
			show("Action CmdID: " + cmdId + " Name: " + action.getValue("Name"));
		}else if (cmdId == DROP_METADATA) {
			show("Action CmdID: " + cmdId + " Name: " + action.getValue("Name"));
		}else if (cmdId == EXPORT) {
			show("Action CmdID: " + cmdId + " Name: " + action.getValue("Name"));
		}else if (cmdId == VALIDATE_GEOMETRY) {
//			ValidateSDOGeometryEmpty vs = new ValidateSDOGeometryEmpty();
//			vs.setVisible(true);
			show("Action CmdID: " + cmdId + " Name: " + action.getValue("Name"));
		}else if (cmdId == IMPORT_SHAPEFILE) {
			ShapefileLoadEmpty.getInstance().initialise();
		}
		return true;
	}

	@Override
	public boolean update(IdeAction action, Context context) {
		action.setEnabled(true);
		return action.isEnabled();
	}

}
