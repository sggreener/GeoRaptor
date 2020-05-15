package org.GeoRaptor;

import java.awt.Toolkit;

import java.sql.Connection;
import java.sql.SQLException;

import javax.swing.JOptionPane;

import oracle.dbtools.raptor.dialogs.actions.AbstractMenuAction;
import oracle.dbtools.raptor.utils.Connections;
import oracle.dbtools.raptor.utils.DBObject;

import oracle.jdbc.OracleConnection;

import org.GeoRaptor.OracleSpatial.CreateSpatialIndex.ManageSpatialIndex;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.ValidateSDOGeometry;
import org.GeoRaptor.SpatialView.JDevInt.ControlerSV;
import org.GeoRaptor.SpatialView.JDevInt.DockableSV;
import org.GeoRaptor.SpatialView.SpatialViewPanel;
import org.GeoRaptor.io.Export.ui.ExporterWizard;
import org.GeoRaptor.io.Import.ShapefileLoad;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;


/**
 * Handler for menu items defined in menu.xml.
 * <p> Note : this is SQL Developer 1.1 and above specific. Previeous versions of
 * SQL Developer use MenuControler
 * @author olaf
 * @author Simon Greener May 2010
 *          Passed schema object and connecting user to functions.
 *          Modifed ManageMetadata to check DML permissions before launch when schema<>user.
 *
 */
public class MenuAction extends AbstractMenuAction {

    private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.MenuAction");

    protected static final String GENERAL_ERROR = MainSettings.EXTENSION_NAME + " Error";
    
    // Action to do at launch
    private String action;

    protected ValidateSDOGeometry validateSDOGeom;

    /**
     * Called at menu-item click; action set in setArgs() method
     * <p>
     * @see oracle.dbtools.raptor.dialogs.actions.AbstractMenuAction#launch()
     */
    @Override
    public void launch() {

        // Get connection and name of user that made the connection
        //
        DBObject                dbo = getDBObject();
        Connection             conn = dbo.getDatabase().getConnection();
        String activeConnectionName = dbo.getConnectionName();
        String       connectionType = dbo.getConnectionType();
        boolean             isMySQL = "MySQL".equals(connectionType);

        String connectionUserName = Connections.getInstance().getConnectionInfo(activeConnectionName).getProperty("user");
        
        // Get object that has been selected in this connection
        //
        String selectedSchemaName = dbo.getSchemaName();
        String selectedObjectName = dbo.getObjectName();
        String selectedColumnName = dbo.getChildName();
        // Create layer and add to spatial view
        //
        if ("importNew".equals(action))  {
            final ShapefileLoad shpLoad = ShapefileLoad.getInstance();
            if ( shpLoad != null ) {
                shpLoad.setConnection(activeConnectionName);
                shpLoad.initialise();
            }
        } else if ("add2map".equals(action) || 
                   "zoom2map".equals(action) || 
                   "column_add2map".equals(action) ||
                   "column_zoom2map".equals(action) ) {
            if ( isMySQL ) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(null, "MySQL support not yet Implemented");
            } else {
                // Add Object to spatial view
                SpatialViewPanel svp = DockableSV.getSpatialViewPanel();
                SpatialViewPanel.LayerReturnCode lrc;
                lrc = svp.addNewSpatialLayer(selectedSchemaName,
                                             selectedObjectName,
                                             selectedColumnName,
                                             dbo.getObjectFolderType(),
                                             activeConnectionName,
                                             (OracleConnection)conn,
                                             action.contains("add2map")?false:true);
                if ( lrc == SpatialViewPanel.LayerReturnCode.MBR ) {
                    Messages.log("Menu: No MBR could be computed for " +
                                 Strings.append(Strings.append(selectedSchemaName,selectedObjectName,"."),selectedColumnName, "."));
                } else if ( lrc == SpatialViewPanel.LayerReturnCode.Metadata ) {
                    Messages.log("Menu: No spatial metadata for " +
                                 Strings.append(Strings.append(selectedSchemaName,selectedObjectName,"."),selectedColumnName, "."));
                    Metadata(conn,
                             selectedSchemaName,
                             selectedObjectName,
                             selectedColumnName,
                             connectionUserName);
                } else if ( lrc == SpatialViewPanel.LayerReturnCode.Success ) {
                    // show Spatial View (maybe window is not open)
                    ControlerSV.showSpatialView();
                    svp.redraw();  // Because we are opening the window again, force a redraw
                }
            }

        // Validate geometry
        //
        } else if ("validate".equals(action) || "column_validate".equals(action)) 
        {
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

        // Create spatial index for geometry column
        //
        } else if ("create_index".equals(action) || "column_create_index".equals(action)) 
        {
            boolean status =
                ManageSpatialIndex.getInstance().setInit(activeConnectionName, 
                                                         selectedSchemaName,
                                                         selectedObjectName, 
                                                         selectedColumnName,
                                                         connectionUserName);
            if (status == true) {
                ManageSpatialIndex.getInstance().setVisible(true);
            }

        // Drop spatial index on a geometry column
        //
        } else if ("drop_index".equals(action) || "column_drop_index".equals(action)) 
        {
            boolean status =
                ManageSpatialIndex.getInstance().dropIndex(conn, 
                                                           selectedSchemaName, 
                                                           selectedObjectName, 
                                                           selectedColumnName,
                                                           connectionUserName,
                                                           true);

        // Export data to GML/KML/Shapefile 
        //
        }  else if ("export".equals(action) || "column_export".equals(action))   
        {
            String title = GENERAL_ERROR;
            int message = JOptionPane.ERROR_MESSAGE;
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

            } catch (SQLException sqle) {
                JOptionPane.showMessageDialog(null,
                                              "SQLException: " + sqle.getMessage(),
                                              GENERAL_ERROR,
                                              JOptionPane.ERROR_MESSAGE);
                
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(null,
                                              iae.getMessage(),
                                              GENERAL_ERROR,
                                              JOptionPane.ERROR_MESSAGE);
                
            } catch (Exception _e) {
                JOptionPane.showMessageDialog(null, _e.getMessage(), title, message);
            }

        // Create/update user_sdo_geom_metadata entry for geometry column
        //
        } else if ("metadata".equals(action) || "column_metadata".equals(action)) {
            Metadata(conn, 
                     selectedSchemaName,
                     selectedObjectName,
                     selectedColumnName,
                     connectionUserName);
        }
    }

    private void Metadata(Connection conn,
                          String     selectedSchemaName,
                          String     selectedObjectName,
                          String     selectedColumnName,
                          String     connectionUserName) 
    {
        String title = GENERAL_ERROR;
        int message = JOptionPane.ERROR_MESSAGE;
        try 
        {
            if ( selectedSchemaName.equalsIgnoreCase(connectionUserName) || MetadataTool.checkCrossSchemaDMLPermissions(conn)) 
            {
                MetadataPanel mp = MetadataPanel.getInstance();
                boolean status =  mp.initialise((OracleConnection)conn, 
                                                selectedSchemaName,
                                                selectedObjectName, 
                                                selectedColumnName,
                                                connectionUserName);
                if (status == true) {
                    mp.setVisible(true);
                }
            }
            else {
                title = MainSettings.EXTENSION_NAME;
                message = JOptionPane.INFORMATION_MESSAGE;
                throw new Exception("Cannot execute cross-schema metadata inserts, updates or deletes.\n" +
                                    "Unless you: \n" +
                                    "Grant Delete On Mdsys.SDO_GEOM_METADATA_TABLE To Public (or " + connectionUserName + ")");
            }

        } catch (SQLException sqle) {
            JOptionPane.showMessageDialog(null,
                                          "SQLException: " + sqle.getMessage(),
                                          GENERAL_ERROR,
                                          JOptionPane.ERROR_MESSAGE);
            
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(null,
                                          iae.getMessage(),
                                          GENERAL_ERROR,
                                          JOptionPane.ERROR_MESSAGE);
            
        } catch (Exception _e) {
            JOptionPane.showMessageDialog(null, _e.getMessage(), title, message);
        }
    }

    /**
     * @see oracle.dbtools.raptor.dialogs.actions.AbstractMenuAction#setArgs(java.lang.String)
     *
     * <p>
     * Set action
     */
    @Override
    public void setArgs(final String args) {
        this.action = args;
    }

/** SGG - Attempt to see if could get addition of multiple tables to spatial view
 * 
    private List getList(Context context)
    {
        ArrayList arraylist = new ArrayList();
        oracle.ide.model.Element aelement[] = context.getSelection();
        oracle.ide.model.Element aelement1[] = aelement;
        int i = aelement1.length;
        for(int j = 0; j < i; j++) 
        {
            oracle.ide.model.Element element = aelement1[j];
            arraylist.add(new DBObject(element));
        }
        return arraylist;
    }

    public void setArgs(java.lang.Object[] args) 
    {
        try {
        for (int i=0;i<args.length;i++)
            System.out.println(args[i]);
        } catch (Exception e) {
          System.out.println("failed");
        }
        System.out.println("Selected: " + this.getContext().getSelection().length + " nodes");
        if ( this.getContext().getSelection().length > 0 ) {
            List al = getList(this.getContext());
            for (int i=0; i<al.size(); i++) {
                System.out.println(((DBObject)al.get(i)).getSchemaName());
            }
        }
    }
*/
  
}
