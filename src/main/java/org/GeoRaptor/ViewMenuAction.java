package org.GeoRaptor;

import java.sql.Connection;

import javax.swing.JOptionPane;

import org.GeoRaptor.OracleSpatial.CreateSpatialIndex.ManageSpatialIndex;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel;
import org.GeoRaptor.SpatialView.SpatialViewPanel;
import org.GeoRaptor.io.Export.ui.ExporterWizard;
import org.GeoRaptor.sql.Queries;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.GeoRaptor.util.logging.Logger;

import oracle.dbtools.raptor.controls.sqldialog.ObjectAction;
import oracle.dbtools.raptor.dialogs.actions.AbstractMenuAction;
import oracle.dbtools.raptor.utils.Connections;
import oracle.dbtools.raptor.utils.DBObject;
import oracle.ide.Ide;
import oracle.ide.controller.IdeAction;

public class ViewMenuAction extends AbstractMenuAction 
{
	private static final Logger LOGGER = org.GeoRaptor.util.logging.Logging.getLogger("org.GeoRaptor.ViewMenuAction");

	private static final String GENERAL_ERROR = MainSettings.EXTENSION_NAME + " Error";

	private static final String MENU_VIEW_COLUMN_ZOOM_TO_MAP 			= "ZOOM_TO_MAP";
	private static final String MENU_VIEW_COLUMN_ADD_TO_MAP 			= "ADD_TO_MAP";
	private static final String MENU_VIEW_COLUMN_METADATA_ARG 			= "METADATA";
	private static final String MENU_VIEW_COLUMN_DROP_METADATA_ARG 		= "DROP_METADATA";
	private static final String MENU_MV_COLUMN_CREATE_SPATIAL_INDEX_ARG = "CREATE_SPATIAL_INDEX";
	private static final String MENU_MV_COLUMN_DROP_SPATIAL_INDEX_ARG 	= "DROP_SPATIA_INDEX";
	private static final String MENU_VIEW_COLUMN_EXPORT_COLUMN_ARG 		= "EXPORT";

	public void launch() 
	{
		LOGGER.debug("handleEvent()");

		DBObject dbo = getDBObject();

		// Get Connection information
		//
		Connection conn = dbo.getDatabase().getConnection();
		String activeConnectionName = dbo.getConnectionName();
		String connectionUserName = Connections.getInstance().getConnectionInfo(activeConnectionName)
				.getProperty("user");
		String connectionType = dbo.getConnectionType();

		if (isMySQL(connectionType)) {
			Tools.displayMessage("MySQL support not yet Implemented", JOptionPane.WARNING_MESSAGE, true);
			return;
		}

		// Get connection information
		// Get object that has been selected in this connection
		//
		String selectedSchemaName = dbo.getSchemaName();
		String selectedObjectName = dbo.getObjectName();
		String selectedColumnName = dbo.getChildName(); // Column name if column node selected, null if table node
														// selected.
		String selectedObjectType = dbo.getObjectFolderType(); // SGG

		LOGGER.debug("Connection Type = " + connectionType + "; Schema = " + selectedSchemaName + "; Object = "
				+ selectedObjectName + "; Column = " + selectedColumnName + "; Type = " + selectedObjectType);

		ObjectAction oat = this.getObjectAction();
		String menuType = oat.getType();
		String menuArgument = oat.getClassArgs();
		String menuTitle = oat.getTitle();

		LOGGER.debug("Menu Argument = " + menuArgument + "; Menu Title = '" + menuTitle + "; Menu Type = " + menuType);

		IdeAction oa = null;
		int actionId = -1;
		oa = oat.getAction();
		actionId = oa.getCommandId();

		try {
			String actionName = Ide.findCmdName(actionId);
			LOGGER.debug("Action Name = " + actionName);
			// debug: Action Name = raptor.objectaction.Oracle.VIEW_COLUMN.Manage Spatial
			// Metadata....0c6d3cdc-4768-47ea-b9da-741c7539cb00
		} catch (Exception e) {
		}

		LOGGER.debug(
				"Handling " + (menuType.equals("VIEW_COLUMN") ? "View Menu Entry" : "Materialized View Menu Entry"));

		if (!hasGeometryColumn(conn, selectedSchemaName, selectedObjectName, selectedColumnName)) {
			Tools.displayMessage("No geometry column exists in " + selectedObjectName, JOptionPane.WARNING_MESSAGE,
					true);
			return;
		}

		// View Menu Actions first...
		if (menuArgument.equalsIgnoreCase(MENU_VIEW_COLUMN_METADATA_ARG)) {
			manageMetadata(conn, selectedSchemaName, selectedObjectName, selectedColumnName, connectionUserName);
		} else if (menuArgument.equalsIgnoreCase(MENU_VIEW_COLUMN_DROP_METADATA_ARG)) {
			dropMetadata(conn, selectedSchemaName, selectedObjectName, selectedColumnName, connectionUserName);
		} else if (menuArgument.equalsIgnoreCase(MENU_VIEW_COLUMN_ZOOM_TO_MAP)
				|| menuArgument.equalsIgnoreCase(MENU_VIEW_COLUMN_ADD_TO_MAP)) {
			geometriesToMap(menuArgument, conn, activeConnectionName, connectionUserName, selectedSchemaName, selectedObjectName, selectedColumnName, selectedObjectType);
		} else if (menuArgument.equalsIgnoreCase(MENU_VIEW_COLUMN_EXPORT_COLUMN_ARG)) {
			export(conn, selectedSchemaName, selectedObjectName, selectedColumnName);
		} else if (menuArgument.equalsIgnoreCase(MENU_MV_COLUMN_CREATE_SPATIAL_INDEX_ARG)) {
			createSpatialIndex(conn, connectionUserName, activeConnectionName, selectedSchemaName, selectedObjectName, selectedColumnName);
		} else if (menuArgument.equalsIgnoreCase(MENU_MV_COLUMN_DROP_SPATIAL_INDEX_ARG)) {
			LOGGER.debug("Dropping Spatial Index");
			ManageSpatialIndex.getInstance().dropIndex(conn, selectedSchemaName, selectedObjectName, selectedColumnName, connectionUserName, true);
		}
	}

	private void createSpatialIndex(Connection _conn, 
			   							String _connectionUserName, 
			   							String _activeConnectionName,
			   							String _selectedSchemaName, 
			   							String _selectedObjectName, 
			   							String _selectedColumnName) 
	{
		LOGGER.debug("CreateSpatialIndex");
		ManageSpatialIndex msi = ManageSpatialIndex.getInstance();
		boolean status = msi.setInit(_activeConnectionName, _selectedSchemaName, _selectedObjectName,
				_selectedColumnName, _connectionUserName);
		if (status == true) {
			ManageSpatialIndex.getInstance().setVisible(true);
		}
	}

	private void export(Connection _conn, 
							String _selectedSchemaName, 
							String _selectedObjectName,
							String _selectedColumnName) 
	{
		LOGGER.debug("Export");

		try {
			ExporterWizard ew = new ExporterWizard("Export to ...", _conn, _selectedSchemaName, _selectedObjectName,
					_selectedColumnName);
			boolean status = ew.initialise();
			LOGGER.debug("ExporterWizard Status " + status);
			if (status == true) {
				ew.show();
			}

		} catch (Exception _e) {
			JOptionPane.showMessageDialog(null, _e.getMessage(), GENERAL_ERROR, JOptionPane.ERROR_MESSAGE);
		}
	}

	private void geometriesToMap(String _menuArgument, 
							 Connection _conn, 
							     String _activeConnectionName,
							     String _connectionUserName, 
							     String _selectedSchemaName, 
							     String _selectedObjectName,
							     String _selectedColumnName, 
							     String _selectedObjectType)

	{
		LOGGER.debug("addGeometriesToMap()");

		String fullObjectName = Strings.append(Strings.append(_selectedSchemaName, _selectedObjectName, "."),
				_selectedColumnName, ".");

		// Add Object to spatial view
		SpatialViewPanel svp = SpatialViewPanel.getInstance();
		SpatialViewPanel.LayerReturnCode lrc;
		lrc = svp.addNewSpatialLayer(_selectedSchemaName, _selectedObjectName, _selectedColumnName, _selectedObjectType,
				_activeConnectionName, _conn, _menuArgument.equalsIgnoreCase(MENU_VIEW_COLUMN_ADD_TO_MAP) ? false : true);

		if (lrc == SpatialViewPanel.LayerReturnCode.MBR) {
			Messages.log("Menu: No MBR could be computed for " + fullObjectName);
		} else if (lrc == SpatialViewPanel.LayerReturnCode.Metadata) {
			Messages.log("Menu: No spatial metadata for " + fullObjectName);
			MetadataPanel mp = MetadataPanel.getInstance();
			boolean status = false;
			try {
				status = mp.initialise(_conn, _selectedSchemaName, _selectedObjectName, _selectedColumnName,
						_connectionUserName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (status == true) {
				mp.setVisible(true);
			}
		} else if (lrc == SpatialViewPanel.LayerReturnCode.Success) {
			// show Spatial View (maybe window is not open)
			svp.show();
		} else if (lrc == SpatialViewPanel.LayerReturnCode.Fail) {
			Messages.log("Spatial View Panel failed to load");
		}

	}

	private void dropMetadata(Connection _conn, 
			 					  String _selectedSchemaName, 
			 					  String _selectedObjectName,
			 					  String _selectedColumnName, 
			 					  String _connectionUserName) 
	{
		LOGGER.debug("dropMetadata()");
		MetadataPanel mp = MetadataPanel.getInstance();
		mp.deleteMetadata(_conn, _selectedSchemaName, _selectedObjectName, _selectedColumnName);
	}

	private void manageMetadata(Connection _conn, 
									String _selectedSchemaName, 
									String _selectedObjectName,
									String _selectedColumnName, 
									String _connectionUserName) 
	{
		LOGGER.debug("manageMetadata()");
		MetadataPanel mp = MetadataPanel.getInstance();
		boolean status = false;
		try {
			status = mp.initialise(_conn, _selectedSchemaName, _selectedObjectName, _selectedColumnName,
					_connectionUserName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (status == true) {
			mp.setVisible(true);
		}
	}

	private boolean hasGeometryColumn(Connection _conn, 
										  String _schemaName, 
										  String _objectName, 
										  String _columnName) 
	{
		LOGGER.debug("hasGeometryColumn");
		String aGeomColumn;
		try {
			aGeomColumn = Queries.getGeometryColumn(_conn, _schemaName, _objectName, _columnName);
		} catch (Exception e) {
			return false;
		}
		return !Strings.isEmpty(aGeomColumn);
	}

	private boolean isMySQL(String _connectionType) 
	{
		return "MySQL".equals(_connectionType);
	}

}
