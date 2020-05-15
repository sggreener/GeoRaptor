package org.GeoRaptor.SpatialView.JDevInt;

//import GeoRaptor.AboutBox;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import oracle.ide.Context;
import oracle.ide.Ide;
import oracle.ide.IdeMainWindow;
import oracle.ide.controller.Controller;
import oracle.ide.controller.IdeAction;
import oracle.ide.controller.Menubar;
import oracle.ide.docking.DockStation;
import oracle.ide.docking.Dockable;

import org.GeoRaptor.AboutDialog;
import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.MenuViewController;
import org.GeoRaptor.Messages;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel;
import org.GeoRaptor.io.Import.ShapefileLoad;
import org.GeoRaptor.sql.DatabaseConnections;

public class ControlerSV 
  implements Controller 
{
    protected static final String GENERAL_ERROR = MainSettings.EXTENSION_NAME + " Error";

    protected JMenuItem menuItem;
    private int VIEW_CMD_ID;
    private IdeAction VIEW_ACTION;

    private int SHAPEFILE_LOADER_ID;
    private IdeAction SHAPEFILE_ACTION;

    private int METADATA_MANAGER_ID;
    private IdeAction METADATA_MANAGER_ACTION;

    private int ABOUT_BOX_ID;
    private IdeAction ABOUT_BOX_ACTION;

    /**
     * @function Controler.SV
     * @author Simon Greener, April 30th 2010
     *          Added Messages.log to inform user GeoRaptor installed by moving
     *          from MenuViewController.java.
     */
    public ControlerSV() {
        AddSpatialView();
        AddMetadataManager();
        AddShapeFileLoader();
        AddAbout();
        // All menus installed so, let user know
        Messages.log (Constants.GEORAPTOR + ": Menus installed",true);
    }

    @SuppressWarnings("deprecation")
	private void AddSpatialView() {
        ClassLoader cl = getClass().getClassLoader();
        this.VIEW_CMD_ID = Ide.findOrCreateCmdID("VIEW_GEORAPTOR_SPATIALVIEW_CMD_ID");
        this.VIEW_ACTION = IdeAction.get(VIEW_CMD_ID, 
                                         (String)null, 
                                         Constants.MENU_ITEM_SPATIAL_VIEWER, 
                                         IdeMainWindow.ACTION_CATEGORY_VIEW,
                                         new Integer((int)'V'),
                                         new ImageIcon(cl.getResource("org/GeoRaptor/images/main_icon_2_18x18.png")),
                                         null, 
                                         true);
        this.VIEW_ACTION.addController(this);
        final Menubar menubar = Ide.getMenubar();
        final JMenuItem menuSpatialView = menubar.createMenuItem(this.VIEW_ACTION);
        MenuViewController.getInstance().addToViewMenu(menuSpatialView);
    }

    @SuppressWarnings("deprecation")
	private void AddMetadataManager() {
        ClassLoader cl = getClass().getClassLoader();
        this.METADATA_MANAGER_ID = Ide.findOrCreateCmdID("VIEW_GEORAPTOR_METADATA_MANAGER_CMD_ID");
        this.METADATA_MANAGER_ACTION = IdeAction.get(METADATA_MANAGER_ID, 
                                                     (String)null, 
                                                     Constants.MENU_ITEM_METADATA_MANAGER, 
                                                     IdeMainWindow.ACTION_CATEGORY_VIEW,
                                                     new Integer((int)'M'),
                                                     new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/layer_show_attributes.png")), 
                                                     null, 
                                                     true);
        this.METADATA_MANAGER_ACTION.addController(this);
        final Menubar menubar = Ide.getMenubar();
        final JMenuItem menuShapefileLoader = menubar.createMenuItem(this.METADATA_MANAGER_ACTION);
        MenuViewController.getInstance().addToViewMenu(menuShapefileLoader);
    }

    @SuppressWarnings("deprecation")
	private void AddShapeFileLoader() {
        ClassLoader cl = getClass().getClassLoader();
        this.SHAPEFILE_LOADER_ID = Ide.findOrCreateCmdID("VIEW_GEORAPTOR_SHAPEFILELOADER_CMD_ID");
        this.SHAPEFILE_ACTION = IdeAction.get(SHAPEFILE_LOADER_ID, 
                                              (String)null, 
                                              Constants.MENU_ITEM_SHAPEFILE_LOADER, 
                                              IdeMainWindow.ACTION_CATEGORY_VIEW,
                                              new Integer((int)'S'), 
                                              new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/icon_load_file.gif")), 
                                              null, 
                                              true);
        this.SHAPEFILE_ACTION.addController(this);
        final Menubar menubar = Ide.getMenubar();
        final JMenuItem menuShapefileLoader = menubar.createMenuItem(this.SHAPEFILE_ACTION);
        MenuViewController.getInstance().addToViewMenu(menuShapefileLoader);
    }

    @SuppressWarnings("deprecation")
	private void AddAbout() {
        ClassLoader cl = getClass().getClassLoader();
        this.ABOUT_BOX_ID = Ide.findOrCreateCmdID("GEORAPTOR_ABOUT_CMD_ID");
        this.ABOUT_BOX_ACTION = IdeAction.get(ABOUT_BOX_ID,
                                              (String)null,
                                              Constants.MENU_ITEM_ABOUT_BOX,
                                              IdeMainWindow.ACTION_CATEGORY_VIEW,
                                              new Integer((int)'A'),
                                              new ImageIcon(cl.getResource("org/GeoRaptor/images/GeoRaptorLogoIcon18x18.png")),
                                              null, true);
        this.ABOUT_BOX_ACTION.addController(this);
        final Menubar menubar = Ide.getMenubar();
        final JMenuItem menuAboutBox = menubar.createMenuItem(this.ABOUT_BOX_ACTION);
        MenuViewController.getInstance().addToViewMenu(menuAboutBox);
    }

    public boolean handleEvent(IdeAction action, Context context) {
        final int cmdId = action.getCommandId();
        if (cmdId == this.VIEW_CMD_ID) 
            showSpatialView();
       else if (cmdId == this.SHAPEFILE_LOADER_ID)
            showShapeFileLoader();
        else if (cmdId == this.ABOUT_BOX_ID)
            showAboutBox();
        else if (cmdId == this.METADATA_MANAGER_ID)
            showMetadataManager();
        return true;
    }

    public boolean update(IdeAction action, Context context) {
        return true;
    }

    /**
     * Show Spatial View dockable window in SQL Developer
     */
    public static void showSpatialView() {
        final AnAddinSV addin = AnAddinSV.getInstance();
        final DockableSVFactory myDockableFactory = addin.getFactory();
        final Dockable dockable = myDockableFactory.getDockableSV();
        final DockStation dockStation = DockStation.getDockStation();
        dockStation.setDockableVisible(dockable, true);
    }
    
    public static void showMetadataManager() {
        try 
        {
            MetadataPanel mp = MetadataPanel.getInstance();
            boolean status =  mp.initialise(null,null,null,null,null);
            if (status == true) {
                mp.setVisible(true);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                                          e.getMessage(),
                                          MainSettings.EXTENSION_NAME + " - Metadata Manager.",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showShapeFileLoader() {
        final ShapefileLoad shpLoad = ShapefileLoad.getInstance();
        if ( shpLoad != null ) {
            String activeConnectionName = DatabaseConnections.getActiveConnectionName();
            shpLoad.setConnection(activeConnectionName);
            shpLoad.initialise();
        }
    }

    public static void showAboutBox() {       
        AboutDialog aboutDialog = AboutDialog.getInstance();
        aboutDialog.setLocationRelativeTo(null);
        aboutDialog.setVisible(true);
    }

    public static void activateSpatialView() {
        final AnAddinSV addin = AnAddinSV.getInstance();
        final DockableSVFactory myDockableFactory = addin.getFactory();
        final Dockable dockable = myDockableFactory.getDockableSV();
        final DockStation dockStation = DockStation.getDockStation();
        dockStation.activateDockable(dockable);
    }

    public static boolean isSpatialViewVisible() {
        final AnAddinSV addin = AnAddinSV.getInstance();
        final DockableSVFactory myDockableFactory = addin.getFactory();
        final Dockable dockable = myDockableFactory.getDockableSV();
        final DockStation dockStation = DockStation.getDockStation();
        return dockStation.isDockableVisible(dockable);
    }

}
