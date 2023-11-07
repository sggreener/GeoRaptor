package org.GeoRaptor;

import java.sql.Connection;

import javax.swing.JOptionPane;

import org.GeoRaptor.OracleSpatial.CreateSpatialIndex.ManageSpatialIndex;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel;
import org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.ValidateSDOGeometry;
import org.GeoRaptor.SpatialView.SpatialViewPanel;
import org.GeoRaptor.io.Export.ui.ExporterWizard;
import org.GeoRaptor.io.Import.ShapefileLoad;
import org.GeoRaptor.sql.Queries;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import oracle.dbtools.raptor.utils.Connections;
import oracle.dbtools.raptor.utils.DBObject;
import oracle.ide.Context;
import oracle.ide.Ide;
import oracle.ide.controller.Controller;
import oracle.ide.controller.IdeAction;

public class TableContextMenuController implements Controller
{
    private static final String GENERAL_ERROR     = MainSettings.EXTENSION_NAME + " Error";
	
	private static final int ZOOM_TO_MAP          = Ide.findOrCreateCmdID("ZOOM_TO_MAP");
	private static final int ADD_TO_MAP           = Ide.findOrCreateCmdID("ADD_TO_MAP");
	private static final int CREATE_SPATIAL_INDEX = Ide.findOrCreateCmdID("CREATE_SPATIAL_INDEX");
	private static final int DROP_SPATIAL_INDEX   = Ide.findOrCreateCmdID("DROP_SPATIAL_INDEX");
	private static final int MANAGE_METADATA      = Ide.findOrCreateCmdID("MANAGE_METADATA");
	private static final int DROP_METADATA        = Ide.findOrCreateCmdID("DROP_METADATA");
	private static final int EXPORT               = Ide.findOrCreateCmdID("EXPORT");
	private static final int EXPORT_COLUMN        = Ide.findOrCreateCmdID("EXPORT_COLUMN");
	private static final int VALIDATE_GEOMETRY    = Ide.findOrCreateCmdID("VALIDATE_GEOMETRY");
	private static final int VALIDATE_COLUMN      = Ide.findOrCreateCmdID("VALIDATE_COLUMN");
	private static final int IMPORT_SHAPEFILE     = Ide.findOrCreateCmdID("IMPORT_SHAPEFILE");

    protected ValidateSDOGeometry validateSDOGeom;

	@Override
	public boolean handleEvent(IdeAction action, Context context) 
	{
        
    	DBObject                dbo = new DBObject(context.getNode());

        // Get Connection information 
        //
        Connection             conn = dbo.getDatabase().getConnection();

        String activeConnectionName = dbo.getConnectionName();
        String       connectionType = dbo.getConnectionType();
        boolean             isMySQL = "MySQL".equals(connectionType);

        // Get connection information
        // Get object that has been selected in this connection
        //
        String selectedSchemaName = dbo.getSchemaName();
        String selectedObjectName = dbo.getObjectName();
        String selectedColumnName = dbo.getChildName(); // Column name if column node selected, null if table node selected.
        String connectionUserName = Connections.getInstance().getConnectionInfo(activeConnectionName).getProperty("user");

        String selectedObjectType = dbo.getObjectFolderType(); // SGG
        
		int cmdId = action.getCommandId();
		
		if (cmdId == ZOOM_TO_MAP || cmdId == ADD_TO_MAP) 
        {
            if ( isMySQL ) {
        		Tools.displayMessage(
        				"MySQL support not yet Implemented",
        			    JOptionPane.WARNING_MESSAGE,
        				true);
            } else {

            	if ( ! hasGeometryColumn(
            			conn, 
                        selectedSchemaName,
                        selectedObjectName, 
                        selectedColumnName) )
            	{
            		Tools.displayMessage(
            				"No geometry column exists in " + selectedObjectName,
            			    JOptionPane.WARNING_MESSAGE,
            				true);
            		return true;
            	}

              // Add Object to spatial view
			  SpatialViewPanel svp = SpatialViewPanel.getInstance();
              SpatialViewPanel.LayerReturnCode lrc;
              lrc = svp.addNewSpatialLayer(selectedSchemaName,
                                           selectedObjectName,
                                           selectedColumnName,
                                           selectedObjectType,
                                           activeConnectionName,
                                           conn,
                                           (cmdId == ADD_TO_MAP)?false:true);
              if ( lrc == SpatialViewPanel.LayerReturnCode.MBR ) {
                  Messages.log("Menu: No MBR could be computed for " +
                               Strings.append(Strings.append(selectedSchemaName,selectedObjectName,"."),selectedColumnName, "."));
              } else if ( lrc == SpatialViewPanel.LayerReturnCode.Metadata ) {
                  Messages.log("Menu: No spatial metadata for " +
                               Strings.append(Strings.append(selectedSchemaName,selectedObjectName,"."),selectedColumnName, "."));
      			MetadataPanel mp = MetadataPanel.getInstance();
    			boolean status = false;
				try {
					status = mp.initialise(conn, 
					                       selectedSchemaName,
					                       selectedObjectName,
					                       selectedColumnName,
					                       connectionUserName);
				} catch (Exception e) {
					e.printStackTrace();
				}
    			if (status == true) {
    				mp.setVisible(true);
    			}

              } else if ( lrc == SpatialViewPanel.LayerReturnCode.Success ) {
                // show Spatial View (maybe window is not open)
                svp.show();
              } else if ( lrc == SpatialViewPanel.LayerReturnCode.Fail ) {           	  
                Messages.log("Spatial View Panel failed to load");
              }
            }
            
        } else if (cmdId == CREATE_SPATIAL_INDEX) {
        	
        	if ( ! hasGeometryColumn(
        			  conn,
        			  selectedSchemaName,
                      selectedObjectName, 
                      selectedColumnName) ) 
        	{
        		Tools.displayMessage(
        				"No geometry column exists in " + selectedObjectName,
        			    JOptionPane.WARNING_MESSAGE,
        				true);
        		return true;
        	}
        	
            ManageSpatialIndex msi = ManageSpatialIndex.getInstance(); 
            boolean status = msi.setInit(activeConnectionName, 
                                         selectedSchemaName,
                                         selectedObjectName, 
                                         selectedColumnName,
                                         connectionUserName);
            if (status == true) {
                ManageSpatialIndex.getInstance().setVisible(true);
            }
			
		} else if (cmdId == DROP_SPATIAL_INDEX) {

        	if ( ! hasGeometryColumn(
                    conn,
      			    selectedSchemaName,
                    selectedObjectName, 
                    selectedColumnName) ) 
        	{
        		Tools.displayMessage(
        				"No geometry column exists in " + selectedObjectName,
        			    JOptionPane.WARNING_MESSAGE,
        				true);
        		return true;
        	}

			// ManageSpatialIndex.getInstance().dropIndex(null, "", "", "", "", true);
            ManageSpatialIndex.getInstance().dropIndex(
            		conn, 
                    selectedSchemaName, 
                    selectedObjectName, 
                    selectedColumnName,
                    connectionUserName,
                    true
            );
            
		} else if (cmdId == MANAGE_METADATA) {

        	if ( ! hasGeometryColumn(
        			conn, 
                    selectedSchemaName,
                    selectedObjectName, 
                    selectedColumnName) )
        	{
        		Tools.displayMessage(
        				"No geometry column exists in " + selectedObjectName,
        			    JOptionPane.WARNING_MESSAGE,
        				true);
        		return true;
        	}
			
			MetadataPanel mp = MetadataPanel.getInstance();
			boolean status = false;
			try {
				status = mp.initialise(
				                     conn, 
				                     selectedSchemaName,
				                     selectedObjectName,
				                     selectedColumnName,
				                     connectionUserName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (status == true) {
				mp.setVisible(true);
			}
            
		} else if (cmdId == DROP_METADATA) {

        	if ( ! hasGeometryColumn(
        			conn, 
                    selectedSchemaName,
                    selectedObjectName, 
                    selectedColumnName) )
        	{
        		Tools.displayMessage(
        				"No geometry column exists in " + selectedObjectName,
        			    JOptionPane.WARNING_MESSAGE,
        				true);
        		return true;
        	}
        	
			MetadataPanel mp = MetadataPanel.getInstance();
			mp.deleteMetadata(conn, 
                              selectedSchemaName,
                              selectedObjectName,
                              selectedColumnName);

		} else if (cmdId == EXPORT || cmdId == EXPORT_COLUMN ) {
			
        	if ( ! hasGeometryColumn(
        			conn, 
                    selectedSchemaName,
                    selectedObjectName, 
                    selectedColumnName) )
        	{
        		Tools.displayMessage(
        				"No geometry column exists in " + selectedObjectName,
        			    JOptionPane.WARNING_MESSAGE,
        				true);
        		return true;
        	}
        	
            try 
            {
                ExporterWizard ew = new ExporterWizard("Export to ...",
                                                       conn,
                                                       selectedSchemaName,
                                                       selectedObjectName,
                                                       selectedColumnName);
                boolean status = ew.initialise();
                if (status == true) {
                    ew.show();
                }

            } catch (Exception _e) {
                JOptionPane.showMessageDialog(null, _e.getMessage(), GENERAL_ERROR, JOptionPane.ERROR_MESSAGE);
            }

		} else if (cmdId == VALIDATE_GEOMETRY || cmdId == VALIDATE_COLUMN) {
			
        	if ( ! hasGeometryColumn(
        			conn, 
                    selectedSchemaName,
                    selectedObjectName, 
                    selectedColumnName) )
        	{
        		Tools.displayMessage(
        				"No geometry column exists in " + selectedObjectName,
        			    JOptionPane.WARNING_MESSAGE,
        				true);
        		return true;
        	}
        	
			//ValidateSDOGeometry vs = new ValidateSDOGeometry();
			//vs.setVisible(true);

            if (this.validateSDOGeom == null) {
                this.validateSDOGeom = new ValidateSDOGeometry();
            }

            try {
                this.validateSDOGeom.init(activeConnectionName, 
                                          selectedSchemaName,
                                          selectedObjectName, 
                                          selectedColumnName);
            } catch (Exception _e) {
                JOptionPane.showMessageDialog(null, 
                                              _e.getMessage(),
                                              GENERAL_ERROR,
                                              JOptionPane.ERROR_MESSAGE);
            }
 		} else if (cmdId == IMPORT_SHAPEFILE) {
			ShapefileLoad.getInstance().initialise();
		}
		return true;
	}

	@Override
	public boolean update(IdeAction action, Context context) {
		action.setEnabled(true);
		return action.isEnabled();
	}

	private boolean hasGeometryColumn(Connection _conn,
                                      String     _schemaName, 
                                      String     _objectName, 
                                      String     _columnName) 
	{
	    String aGeomColumn;
		try {
			aGeomColumn = Queries.getGeometryColumn(
					                 _conn,
					                 _schemaName,
					                 _objectName,
					                 _columnName);
		} catch (Exception e) {
			return false;
		}
	    return ! Strings.isEmpty(aGeomColumn);
	}
}
