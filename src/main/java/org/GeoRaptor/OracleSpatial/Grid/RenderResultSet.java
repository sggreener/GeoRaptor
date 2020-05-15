package org.GeoRaptor.OracleSpatial.Grid;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.reflect.Method;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.sql.RowSetMetaData;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import oracle.dbtools.raptor.controls.grid.ResultSetTable;
import oracle.dbtools.raptor.controls.grid.ResultSetTableModel;
import oracle.dbtools.raptor.controls.grid.contextmenu.GridContextMenuItem;
import oracle.dbtools.raptor.utils.Connections;

import oracle.ide.Context;
import oracle.ide.Ide;
import oracle.ide.controller.ContextMenu;
import oracle.ide.controller.IdeAction;
import oracle.ide.dialogs.ProgressBar;

import oracle.javatools.db.DBException;

import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.driver.OracleConnection;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.ARRAY;
import oracle.sql.Datum;
import oracle.sql.NUMBER;
import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.Constants.EXPORT_TYPE;
import org.GeoRaptor.Constants.GEOMETRY_TYPES;
import org.GeoRaptor.Constants.SHAPE_TYPE;
import org.GeoRaptor.Constants.XMLAttributeFlavour;
import org.GeoRaptor.Constants.renderType;
import org.GeoRaptor.SpatialView.JDevInt.ControlerSV;
import org.GeoRaptor.SpatialView.JDevInt.DockableSV;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.JDevInt.SpatialRenderer;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SpatialViewPanel;
import org.GeoRaptor.SpatialView.SupportClasses.Preview;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.layers.SVGraphicLayer;
import org.GeoRaptor.SpatialView.layers.SVWorksheetLayer;
import org.GeoRaptor.io.Export.ui.ExporterDialog;
import org.GeoRaptor.io.Export.GMLExporter;
import org.GeoRaptor.io.Export.IExporter;
import org.GeoRaptor.io.Export.KMLExporter;
import org.GeoRaptor.io.Export.SHPExporter;
import org.GeoRaptor.io.Export.TABExporter;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.sql.OraRowSetMetaDataImpl;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.GeometryProperties;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.util.logging.Logger;

public class RenderResultSet 
     extends GridContextMenuItem 
{

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.OracleSpatial.Grid.RenderResultSet");

    private static final String cNoLabel = "No Label";
    /** Class instance
     */
    private static RenderResultSet _instance = null;

    /** For Visual Menu
     **/
    private ClassLoader             cl = this.getClass().getClassLoader();
    private final String iconDirectory = "org/GeoRaptor/SpatialView/images/";
    private ImageIcon         iconTick = new ImageIcon(cl.getResource(iconDirectory + "view_menu_layers_on.png"));

    private static enum DiminfoCopyType {SDO_GEOMETRY, DIMINFO};
    private static enum sqlType {INSERT, UPDATE, DELETE};
    
    /** 
     * Properties File Manager
     **/
    private static final String propertiesFile = "org.GeoRaptor.OracleSpatial.Grid.gridcontextmenu";
    protected PropertiesManager propertyManager;

    private RenderTool renderTool;

    /**
     * For access to preferences
     */
    protected Preferences mainPrefs;
    
    private OracleResultSetMetaData    meta = null;
    private ResultSetTable              rst = null;
    
    private static final int COMMAND_COPY_GEOMMBR2CLIPBOARD = Ide.findOrCreateCmdID("cmdGridCopyMBR");
    private static IdeAction ACTION_COPY_SELECTED_GEOMS2MBR = null;
    
    private static final int COMMAND_COPY_GEOMS2CLIPBOARD = Ide.findOrCreateCmdID("cmdGridClipboardGeometries");
    private static IdeAction ACTION_COPY_GEOMS2CLIPBOARD = null;

    private static final int COMMAND_VIEW_SELECTED_GEOMS = Ide.findOrCreateCmdID("cmdGridRenderResultSet");
    private static IdeAction ACTION_VIEW_SELECTED_GEOMS = null;

    private static final int COMMAND_CREATE_QUERY_LAYER = Ide.findOrCreateCmdID("cmdWorksheetSQLLayer");
    private static IdeAction ACTION_CREATE_QUERY_LAYER = null;

    private static final int COMMAND_ZOOM_SELECTED_GEOMS = Ide.findOrCreateCmdID("cmdGridZoomSelectedGeometries");
    private static IdeAction ACTION_ZOOM_SELECTED_GEOMS = null;
    
    private static final int COMMAND_CREATE_IMAGE_SELECTED_GEOMS = Ide.findOrCreateCmdID("cmdCreateImageSelectedGeometries");
    private static IdeAction ACTION_CREATE_IMAGE_SELECTED_GEOMS = null;

    private static final int COMMAND_GRID_EXPORT_GML = Ide.findOrCreateCmdID("cmdGridExportGML");
    private static IdeAction ACTION_GRID_EXPORT_GML = null;

    private static final int COMMAND_GRID_EXPORT_KML = Ide.findOrCreateCmdID("cmdGridExportKML");
    private static IdeAction ACTION_GRID_EXPORT_KML = null;

    private static final int COMMAND_GRID_EXPORT_SHP = Ide.findOrCreateCmdID("cmdGridExportSHP");
    private static IdeAction ACTION_GRID_EXPORT_SHP = null;

    private static final int COMMAND_GRID_EXPORT_TAB = Ide.findOrCreateCmdID("cmdGridExportTAB");
    private static IdeAction ACTION_GRID_EXPORT_TAB = null;

    private static final int COMMAND_GRID_VISUAL_SDO_COLOUR = Ide.findOrCreateCmdID("cmdGridVisualSDOColoured");
    private static IdeAction ACTION_GRID_VISUAL_SDO_COLOUR = null;
    
    private static final int COMMAND_GRID_VISUAL_SDO = Ide.findOrCreateCmdID("cmdGridVisualSDO");
    private static IdeAction ACTION_GRID_VISUAL_SDO = null;
    
    private static final int COMMAND_GRID_VISUAL_WKT = Ide.findOrCreateCmdID("cmdGridVisualWKT");
    private static IdeAction ACTION_GRID_VISUAL_WKT = null;
    
    private static final int COMMAND_GRID_VISUAL_KML = Ide.findOrCreateCmdID("cmdGridVisualKML");
    private static IdeAction ACTION_GRID_VISUAL_KML = null;
    
    private static final int COMMAND_GRID_VISUAL_GML = Ide.findOrCreateCmdID("cmdGridVisualGML");
    private static IdeAction ACTION_GRID_VISUAL_GML = null;
    
    private static final int COMMAND_GRID_VISUAL_ICON = Ide.findOrCreateCmdID("cmdGridVisualICON");
    private static IdeAction ACTION_GRID_VISUAL_ICON = null;
    
    private static final int COMMAND_GRID_VISUAL_THUMBNAIL = Ide.findOrCreateCmdID("cmdGridVisualTHUMBNAIL");
    private static IdeAction ACTION_GRID_VISUAL_THUMBNAIL = null;

    // DimInfo
    private static final int COMMAND_COPY_DIMINFO2SDO = Ide.findOrCreateCmdID("cmdGridClipboardDimInfo2SDO");
    private static IdeAction ACTION_COPY_DIMINFO2SDO = null;
    private static final int COMMAND_COPY_DIMINFO2CLIPBOARD = Ide.findOrCreateCmdID("cmdGridClipboardDimInfo");
    private static IdeAction ACTION_COPY_DIMINFO2CLIPBOARD = null;
    private static final int COMMAND_INSERT_METADATA = Ide.findOrCreateCmdID("cmdGridSdoMetadataInsert");
    private static IdeAction ACTION_INSERT_METADATA = null;
    private static final int COMMAND_UPDATE_METADATA = Ide.findOrCreateCmdID("cmdGridSdoMetadataUpdate");
    private static IdeAction ACTION_UPDATE_METADATA = null;
    private static final int COMMAND_DELETE_METADATA = Ide.findOrCreateCmdID("cmdGridSdoMetadataDelete");
    private static IdeAction ACTION_DELETE_METADATA = null;
    private static final int COMMAND_MAP_DIMINFO = Ide.findOrCreateCmdID("cmdGridSdoMetadataMap");
    private static IdeAction ACTION_MAP_DIMINFO = null;

    // Label SubMenu
    private static LinkedHashMap<Integer, IdeAction> labelMenuActions = null; 

    private int clickColumn = -1;
    private int clickRow = -1;

    private GeometryProperties geometryMetadata = null;
    
    private RenderResultSet()
    {
        // Get localisation file
        //
        this.propertyManager = new PropertiesManager(RenderResultSet.propertiesFile);
        this.mainPrefs = MainSettings.getInstance().getPreferences();
        this.renderTool = new RenderTool();
        makeVisible();
        initAction();
    }
    
    public static RenderResultSet getInstance() {
        if (_instance == null) {
         _instance = new RenderResultSet();
        }
        return _instance;
     }

    public void makeVisible() {
        if ( ! ControlerSV.isSpatialViewVisible() ) {
            ControlerSV.showSpatialView();
        }
    }
    
    private void initAction() 
    {
        if (RenderResultSet.ACTION_COPY_DIMINFO2SDO == null) { RenderResultSet.ACTION_COPY_DIMINFO2SDO = createAction(COMMAND_COPY_DIMINFO2SDO,this.propertyManager.getMsg("DimInfo2SDO"),null); }
        if (RenderResultSet.ACTION_COPY_DIMINFO2CLIPBOARD==null) { RenderResultSet.ACTION_COPY_DIMINFO2CLIPBOARD = createAction(COMMAND_COPY_DIMINFO2CLIPBOARD,this.propertyManager.getMsg("CopyDimInfos"),null); }
        if (RenderResultSet.ACTION_MAP_DIMINFO == null)     { RenderResultSet.ACTION_MAP_DIMINFO = createAction(COMMAND_MAP_DIMINFO,this.propertyManager.getMsg("MapDimInfos"),null); }
        if (RenderResultSet.ACTION_INSERT_METADATA == null) { RenderResultSet.ACTION_INSERT_METADATA = createAction(COMMAND_INSERT_METADATA,this.propertyManager.getMsg("CreateSdoMetadataInsert"),null); }
        if (RenderResultSet.ACTION_UPDATE_METADATA == null) { RenderResultSet.ACTION_UPDATE_METADATA = createAction(COMMAND_UPDATE_METADATA,this.propertyManager.getMsg("CreateSdoMetadataUpdate"),null);  }
        if (RenderResultSet.ACTION_DELETE_METADATA == null) { RenderResultSet.ACTION_DELETE_METADATA = createAction(COMMAND_DELETE_METADATA,this.propertyManager.getMsg("CreateSdoMetadataDelete"),null); }

        if (RenderResultSet.ACTION_VIEW_SELECTED_GEOMS == null) { 
            RenderResultSet.ACTION_VIEW_SELECTED_GEOMS = createAction(COMMAND_VIEW_SELECTED_GEOMS,this.propertyManager.getMsg("RenderResultSet"),null); }
        if (RenderResultSet.ACTION_ZOOM_SELECTED_GEOMS == null) { 
            RenderResultSet.ACTION_ZOOM_SELECTED_GEOMS = createAction(COMMAND_ZOOM_SELECTED_GEOMS, this.propertyManager.getMsg("ZoomRenderResultSet"),null); }

        if (RenderResultSet.ACTION_CREATE_QUERY_LAYER == null)          { 
            RenderResultSet.ACTION_CREATE_QUERY_LAYER = createAction(COMMAND_CREATE_QUERY_LAYER, this.propertyManager.getMsg("CreateLayerFromWorksheet"),null); }
        if (RenderResultSet.ACTION_CREATE_IMAGE_SELECTED_GEOMS == null) { 
            RenderResultSet.ACTION_CREATE_IMAGE_SELECTED_GEOMS = createAction(COMMAND_CREATE_IMAGE_SELECTED_GEOMS, this.propertyManager.getMsg("createImageSelectedGeometries"),null); }
   
        if (RenderResultSet.ACTION_COPY_GEOMS2CLIPBOARD == null)    { 
            RenderResultSet.ACTION_COPY_GEOMS2CLIPBOARD = createAction(COMMAND_COPY_GEOMS2CLIPBOARD,this.propertyManager.getMsg("CopyGeometries"), null); } 
        if (RenderResultSet.ACTION_COPY_SELECTED_GEOMS2MBR == null) { 
            RenderResultSet.ACTION_COPY_SELECTED_GEOMS2MBR = createAction(COMMAND_COPY_GEOMMBR2CLIPBOARD,this.propertyManager.getMsg("CopyGeometriesMBR"),null); }
        
        if (RenderResultSet.ACTION_GRID_EXPORT_GML == null) { RenderResultSet.ACTION_GRID_EXPORT_GML = createAction(COMMAND_GRID_EXPORT_GML,this.propertyManager.getMsg("ExportGML"),null); }
        if (RenderResultSet.ACTION_GRID_EXPORT_KML == null) { RenderResultSet.ACTION_GRID_EXPORT_KML = createAction(COMMAND_GRID_EXPORT_KML,this.propertyManager.getMsg("ExportKML"),null); }
        if (RenderResultSet.ACTION_GRID_EXPORT_SHP == null) { RenderResultSet.ACTION_GRID_EXPORT_SHP = createAction(COMMAND_GRID_EXPORT_SHP,this.propertyManager.getMsg("ExportSHP"),null); }
        if (RenderResultSet.ACTION_GRID_EXPORT_TAB == null) { RenderResultSet.ACTION_GRID_EXPORT_TAB = createAction(COMMAND_GRID_EXPORT_TAB,this.propertyManager.getMsg("ExportTAB"),null); }

        if (RenderResultSet.ACTION_GRID_VISUAL_SDO == null)      { RenderResultSet.ACTION_GRID_VISUAL_SDO = createAction(COMMAND_GRID_VISUAL_SDO,this.propertyManager.getMsg("VisualSDO"),null); }
        if (RenderResultSet.ACTION_GRID_VISUAL_SDO_COLOUR==null) { RenderResultSet.ACTION_GRID_VISUAL_SDO_COLOUR = createAction(COMMAND_GRID_VISUAL_SDO_COLOUR,this.propertyManager.getMsg("VisualSDOColoured"),null); }        
        if (RenderResultSet.ACTION_GRID_VISUAL_WKT== null)       { RenderResultSet.ACTION_GRID_VISUAL_WKT = createAction(COMMAND_GRID_VISUAL_WKT,this.propertyManager.getMsg("VisualWKT"),null); }
        if (RenderResultSet.ACTION_GRID_VISUAL_KML== null)       { RenderResultSet.ACTION_GRID_VISUAL_KML = createAction(COMMAND_GRID_VISUAL_KML,this.propertyManager.getMsg("VisualKML"),null); }
        if (RenderResultSet.ACTION_GRID_VISUAL_GML== null)       { RenderResultSet.ACTION_GRID_VISUAL_GML = createAction(COMMAND_GRID_VISUAL_GML,this.propertyManager.getMsg("VisualGML"),null); }
        if (RenderResultSet.ACTION_GRID_VISUAL_ICON==null)       { RenderResultSet.ACTION_GRID_VISUAL_ICON = createAction(COMMAND_GRID_VISUAL_ICON,this.propertyManager.getMsg("VisualICON"),null); }
        if (RenderResultSet.ACTION_GRID_VISUAL_THUMBNAIL==null)  { RenderResultSet.ACTION_GRID_VISUAL_THUMBNAIL = createAction(COMMAND_GRID_VISUAL_THUMBNAIL,this.propertyManager.getMsg("VisualTHUMBNAIL"),null); }
    }

    public boolean handleEvent(IdeAction _ideAction, 
                               Context   _context) 
    {
        if ( this.rst == null ) {
          if (_context.getEvent().getSource() instanceof ResultSetTable) {
              this.rst = (ResultSetTable)_context.getEvent().getSource();
          }
          if (  this.rst == null ) {
              return false;
          }
        }
        if ( this.meta == null ) {
          try {
              ResultSetTableModel rstm = this.rst.getGridModel();
              // 3.0
              this.meta = (OracleResultSetMetaData)rstm.getResultSetMetaData();
              // 2.1 this.meta = (OracleResultSetMetaData)rstm.getResultSet().getMetaData();
          } catch (SQLException e) {
              LOGGER.warn("RenderResultSet.handleEvent: SQLException - " + e.getMessage());
              return false;
          } catch (Exception e) {
              LOGGER.warn("RenderResultSet.handleEvent: Exception - " + e.getMessage());
              return false;
          }
        }
        if ( _ideAction.getCommandId() == COMMAND_COPY_DIMINFO2SDO            ) { copyDimInfoToClipboard(DiminfoCopyType.SDO_GEOMETRY); } else        
        if ( _ideAction.getCommandId() == COMMAND_COPY_DIMINFO2CLIPBOARD      ) { copyDimInfoToClipboard(DiminfoCopyType.DIMINFO);      } else        
        if ( _ideAction.getCommandId() == COMMAND_INSERT_METADATA             ) { copySdoMetadataSQLClipboard(sqlType.INSERT); } else 
        if ( _ideAction.getCommandId() == COMMAND_UPDATE_METADATA             ) { copySdoMetadataSQLClipboard(sqlType.UPDATE); } else 
        if ( _ideAction.getCommandId() == COMMAND_DELETE_METADATA             ) { copySdoMetadataSQLClipboard(sqlType.DELETE); } else 
        if ( _ideAction.getCommandId() == COMMAND_MAP_DIMINFO                 ) { return mapSelectedDimInfos();                } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_EXPORT_GML             ) { exportResultSet(Constants.EXPORT_TYPE.GML);  } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_EXPORT_KML             ) { exportResultSet(Constants.EXPORT_TYPE.KML);  } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_EXPORT_SHP             ) { exportResultSet(Constants.EXPORT_TYPE.SHP);  } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_EXPORT_TAB             ) { exportResultSet(Constants.EXPORT_TYPE.TAB);  } else
        if ( _ideAction.getCommandId() == COMMAND_COPY_GEOMMBR2CLIPBOARD      ) { copyMBRToClipboard();                        } else
        if ( _ideAction.getCommandId() == COMMAND_COPY_GEOMS2CLIPBOARD        ) { copyToClipboard();                           } else
        if ( _ideAction.getCommandId() == COMMAND_CREATE_IMAGE_SELECTED_GEOMS ) { displayImageOfSelectedGeometries();         } else
        if ( _ideAction.getCommandId() == COMMAND_CREATE_QUERY_LAYER          ) { createWorksheetSQLLayer();                   } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_VISUAL_SDO             ) { mainPrefs.setColourSdoGeomElements(false);
                                                                                  mainPrefs.setSdoGeometryVisualFormat(Constants.renderType.SDO_GEOMETRY); } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_VISUAL_SDO_COLOUR      ) { mainPrefs.setColourSdoGeomElements(true);
                                                                                  mainPrefs.setSdoGeometryVisualFormat(Constants.renderType.SDO_GEOMETRY); } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_VISUAL_WKT             ) { mainPrefs.setSdoGeometryVisualFormat(Constants.renderType.WKT);          } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_VISUAL_KML             ) { mainPrefs.setSdoGeometryVisualFormat(Constants.renderType.KML2);         } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_VISUAL_GML             ) { mainPrefs.setSdoGeometryVisualFormat(Constants.renderType.GML3);         } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_VISUAL_ICON            ) { mainPrefs.setSdoGeometryVisualFormat(Constants.renderType.ICON);         } else
        if ( _ideAction.getCommandId() == COMMAND_GRID_VISUAL_THUMBNAIL       ) { mainPrefs.setSdoGeometryVisualFormat(Constants.renderType.THUMBNAIL);    } else
        if ( _ideAction.getCommandId() == COMMAND_VIEW_SELECTED_GEOMS ||
             _ideAction.getCommandId() == COMMAND_ZOOM_SELECTED_GEOMS         ) { 
            return mapSelectedGeometries(_ideAction.getCommandId(),cNoLabel);
        } else {
            IdeAction labelAction = RenderResultSet.labelMenuActions.get(Integer.valueOf(_ideAction.getCommandId())); 
            if (labelAction != null) {
                return mapSelectedGeometries(labelAction.getCommandId(),
                                             (String)labelAction.getValue(IdeAction.NAME));
            }
        } 
        // Can we reset geometry column to new width based on visual contents?
        return true;
    }

    protected void createAndShowMenu(ContextMenu contextMenu) {
        JMenu GeoRaptorMenu = contextMenu.createMenu(Constants.GEORAPTOR,0);
        if ( this.findDimArrayColumn()!=-1 ) {
            int menuIndex=0;
            JMenu DiminfoSubMenu = new JMenu(this.propertyManager.getMsg("CopyMenu"));
                JMenuItem CopyDimInfo2SDOJMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_COPY_DIMINFO2SDO);
                DiminfoSubMenu.add(CopyDimInfo2SDOJMenuItem,menuIndex++);
                JMenuItem CopyDimInfoJMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_COPY_DIMINFO2CLIPBOARD);
                DiminfoSubMenu.add(CopyDimInfoJMenuItem,menuIndex++);
                if ( this.hasCompleteUserSdoMetadataFields() ) {
                    JMenuItem CreateInsertSdoMetadataJMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_INSERT_METADATA);
                    DiminfoSubMenu.add(CreateInsertSdoMetadataJMenuItem,menuIndex++);
                    JMenuItem CreateUpdateSdoMetadataJMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_UPDATE_METADATA);
                    DiminfoSubMenu.add(CreateUpdateSdoMetadataJMenuItem,menuIndex++);
                    JMenuItem CreateDeleteSdoMetadataJMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_DELETE_METADATA);
                    DiminfoSubMenu.add(CreateDeleteSdoMetadataJMenuItem,menuIndex++);
                }
            GeoRaptorMenu.add(DiminfoSubMenu,0);
            JMenuItem MapDimInfoJMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_MAP_DIMINFO);
            GeoRaptorMenu.add(MapDimInfoJMenuItem,1);
            contextMenu.add(GeoRaptorMenu);                
            return;
        } else {
            int menuIndex=0;
            JMenu ZoomMenu = new JMenu(this.propertyManager.getMsg("ZoomRenderResultSet"));
                /* Layer Menus that need Label submenu */
                ZoomMenu = addLabelSubMenu(contextMenu,RenderResultSet.ACTION_ZOOM_SELECTED_GEOMS,ZoomMenu);
                if ( ZoomMenu.getMenuComponentCount()==0 ) {
                    JMenuItem menuZoom = contextMenu.createMenuItem(RenderResultSet.ACTION_ZOOM_SELECTED_GEOMS); 
                    GeoRaptorMenu.add(menuZoom,menuIndex++);
                } else {
                    GeoRaptorMenu.add(ZoomMenu,menuIndex++);
                }

            JMenu ViewMenu = new JMenu(this.propertyManager.getMsg("RenderResultSet"));
                ViewMenu = addLabelSubMenu(contextMenu,RenderResultSet.ACTION_VIEW_SELECTED_GEOMS,ViewMenu);
                if ( ViewMenu.getMenuComponentCount()==0 ) {
                    JMenuItem ViewMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_VIEW_SELECTED_GEOMS); 
                    GeoRaptorMenu.add(ViewMenuItem,menuIndex++);
                } else {
                    GeoRaptorMenu.add(ViewMenu,menuIndex++);
                }

            JMenu QueryLayerMenu = new JMenu(this.propertyManager.getMsg("CreateLayerFromWorksheet"));
                QueryLayerMenu = addLabelSubMenu(contextMenu,RenderResultSet.ACTION_CREATE_QUERY_LAYER,QueryLayerMenu);
                if ( QueryLayerMenu.getMenuComponentCount()==0 ) {
                    JMenuItem QueryMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_CREATE_QUERY_LAYER); 
                    GeoRaptorMenu.add(QueryMenuItem,menuIndex++);
                } else {
                    GeoRaptorMenu.add(QueryLayerMenu,menuIndex++);
                }

            /* Rest of Menus */
            JMenuItem menuImageSelectedGeoms= contextMenu.createMenuItem(RenderResultSet.ACTION_CREATE_IMAGE_SELECTED_GEOMS);
                GeoRaptorMenu.add(menuImageSelectedGeoms,menuIndex++);

            JMenu CopyGeometriesMenu = new JMenu(this.propertyManager.getMsg("CopyMenu"));
                JMenuItem clipboardGeomJMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_COPY_GEOMS2CLIPBOARD);
                CopyGeometriesMenu.add(clipboardGeomJMenuItem,0);
                JMenuItem geometryMBRMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_COPY_SELECTED_GEOMS2MBR);
                CopyGeometriesMenu.add(geometryMBRMenuItem,1);
                GeoRaptorMenu.add(CopyGeometriesMenu,menuIndex++);

            JMenu ExportMenu = new JMenu(this.propertyManager.getMsg("EXPORT"));
                JMenuItem ExportGMLMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_EXPORT_GML);
                ExportMenu.add(ExportGMLMenuItem,0);
                JMenuItem ExportKMLMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_EXPORT_KML);
                ExportMenu.add(ExportKMLMenuItem,1);        
                JMenuItem ExportSHPMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_EXPORT_SHP);
                ExportMenu.add(ExportSHPMenuItem,2);        
                JMenuItem ExportTABMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_EXPORT_TAB);
                ExportMenu.add(ExportTABMenuItem,3);
                GeoRaptorMenu.add(ExportMenu,menuIndex++);
            
            JMenu VisualMenu = new JMenu(this.propertyManager.getMsg("VISUAL"));
              JMenuItem VisualSDOMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_VISUAL_SDO);
              VisualMenu.add(VisualSDOMenuItem,0);
              JMenuItem VisualSDOColouredMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_VISUAL_SDO_COLOUR);
              VisualMenu.add(VisualSDOColouredMenuItem,1);
              if ( mainPrefs.getVisualFormat()==Constants.renderType.SDO_GEOMETRY ) {
                  if ( mainPrefs.isColourSdoGeomElements() )
                      VisualSDOColouredMenuItem.setIcon(iconTick);
                  else
                      VisualSDOMenuItem.setIcon(iconTick);
              }
              JMenuItem VisualWKTMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_VISUAL_WKT);
              VisualMenu.add(VisualWKTMenuItem,2);
              if ( mainPrefs.getVisualFormat()==Constants.renderType.WKT ) VisualWKTMenuItem.setIcon(iconTick);
              JMenuItem VisualKMLMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_VISUAL_KML);
              VisualMenu.add(VisualKMLMenuItem,3);        
              if ( mainPrefs.getVisualFormat()==Constants.renderType.KML2 ) VisualKMLMenuItem.setIcon(iconTick);
              JMenuItem VisualGMLMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_VISUAL_GML);
              VisualMenu.add(VisualGMLMenuItem,4);
              if ( mainPrefs.getVisualFormat()==Constants.renderType.GML3 ) VisualGMLMenuItem.setIcon(iconTick);
              JMenuItem VisualIconMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_VISUAL_ICON);
              VisualMenu.add(VisualIconMenuItem,5);
              if ( mainPrefs.getVisualFormat()==Constants.renderType.ICON ) VisualIconMenuItem.setIcon(iconTick);
              JMenuItem VisualThumbnailMenuItem = contextMenu.createMenuItem(RenderResultSet.ACTION_GRID_VISUAL_THUMBNAIL);
              VisualMenu.add(VisualThumbnailMenuItem,6);
              if ( mainPrefs.getVisualFormat()==Constants.renderType.THUMBNAIL ) VisualThumbnailMenuItem.setIcon(iconTick);
            GeoRaptorMenu.add(VisualMenu,menuIndex++);
            
            contextMenu.add(GeoRaptorMenu);
        }
    }

    private JMenu addLabelSubMenu(ContextMenu _contextMenu,
                                    IdeAction _ideAction,
                                    JMenu     _subMenu) 
    {
        RenderResultSet.labelMenuActions = new LinkedHashMap<Integer,IdeAction>();

        if ( this.rst == null || this.meta==null  ) {
            return null;
        }
        try 
        {
            int colsToProcess = this.rst.getSelectedColumnCount()!=0
                                ? this.rst.getSelectedColumnCount()
                                : this._table.getColumnCount();
            int[] columns = this.rst.getSelectedColumnCount()!=0 
                            ? this.rst.getSelectedColumns()
                            : this._table.getSelectedColumns();
            int viewCol = -1;
            String columnName = "";
            
            JMenuItem menuItem = null;
            if (colsToProcess>1) {
                menuItem = _contextMenu.createMenuItem(_ideAction);
                menuItem.setText(cNoLabel);
                _subMenu.add(menuItem,0);
            }

            for (int col = 0; col < colsToProcess; col++) 
            {
                viewCol = col;
                if ( this.rst.getSelectedColumnCount() > 0 ) {
                    viewCol = columns[col];
                }
                columnName = this.rst.getColumnName(viewCol);
                for (int rCol=1;rCol<=this.meta.getColumnCount();rCol++) {
                    try {
                        if ( this.meta.getColumnLabel(rCol).equalsIgnoreCase(columnName) ) {
                            if (this.meta.getColumnType(rCol) != OracleTypes.STRUCT 
                                && 
                                Tools.isSupportedType(this.meta.getColumnType(rCol),
                                                      this.meta.getColumnTypeName(rCol))
                                &&
                                colsToProcess > 1 /* More than just geometry column */ ) 
                            {
                                Integer labelCmdId = Integer.valueOf(Ide.findOrCreateCmdID(columnName));
                                IdeAction ideAction = createAction(labelCmdId,columnName,null);
                                RenderResultSet.labelMenuActions.put(labelCmdId,ideAction);
                                menuItem = _contextMenu.createMenuItem(ideAction);
                                menuItem.setText(columnName);
                                _subMenu.add(menuItem);
                            }
                            break;
                        }
                    } catch (SQLException e) {
                        LOGGER.warn("RenderResultSet.getLabels(): SQLException - " + e.getMessage());
                    }
                }
            }
      } catch (SQLException sqle) {
            LOGGER.warn("RenderResultSet.getLabels(): SQLException - " + sqle.getMessage());
      }
      return _subMenu;
    }
    
    protected boolean canShow(ContextMenu _contextMenu) 
    {
        // Test connection type
        // Functionality only for Oracle at the moment.
        try {
            Connection c = this.getConnection();
            if ( c != null ) {
                DatabaseMetaData d = c.getMetaData();
                if ( d.getDatabaseProductName().equalsIgnoreCase("MySQL") ) {
                    Toolkit.getDefaultToolkit().beep();
                    LOGGER.info("Support for MySQL ResultSets not yet implemented.");
                    return false;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get rowset connection metadata");
            return false;
        }
        if (_contextMenu.getContext().getEvent().getSource() instanceof ResultSetTable) 
        {
            this.rst = (ResultSetTable)_contextMenu.getContext().getEvent().getSource();
            // See if click is over a specific geometry column name
            //
            try {
                ResultSetTableModel rstm = this.rst.getGridModel();
                // 3.0  
                if ( rstm.getResultSetMetaData() instanceof OracleResultSetMetaData) {
                    this.meta = (OracleResultSetMetaData)rstm.getResultSetMetaData();
                }
                // 2.1 this.meta = (OracleResultSetMetaData)rstm.getResultSet().getMetaData();
            } catch (SQLException e) {
                LOGGER.error("Could not retrieve result set metadata");
            }
            
            clickRow    = _table.rowAtPoint(((MouseEvent)_contextMenu.getContext().getEvent()).getPoint());
            clickColumn = _table.columnAtPoint(((MouseEvent)_contextMenu.getContext().getEvent()).getPoint());
            int modelRow    = _table.convertRowIndexToModel(clickRow);
            int modelColumn = _table.convertColumnIndexToModel(clickColumn);
            Object obj  = _table.getModel().getValueAt(modelRow, modelColumn);
            STRUCT struct = null;
            ARRAY array = null;
            if (obj instanceof STRUCT) {
                try {
                    struct = (STRUCT)obj;
                    if ( struct.getSQLTypeName().indexOf("MDSYS.ST_")==0 ||
                         struct.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_GEOMETRY) ||
                         struct.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_POINT_TYPE) ||
                         struct.getSQLTypeName().equals(Constants.TAG_MDSYS_VERTEX_TYPE) )  
                    {
                        // Return true for this clicked geometry column if:
                        // 1. No selection exists
                        // 2. The clicked geometry column is also selected 
                        //
                        if ( _table.getSelectedColumnCount()==0 ) {
                            return clickColumn!=-1;
                        } else {
                            // we always return the click column and not the model
                            //
                            return _table.isColumnSelected(clickColumn);
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.error("SQL error examining STRUCT column " + _table.getModel().getColumnName(modelColumn) + " (" + e.toString() + ")");
                }
            } else if (obj instanceof ARRAY){
                try {
                    array = (ARRAY)obj;
                    if ( array.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_DIMARRAY) ) 
                    {
                        // Return true for this clicked geometry column if:
                        // 1. No selection exists
                        // 2. The clicked geometry column is also selected 
                        //
                        if ( _table.getSelectedColumnCount()==0 ) {
                            return clickColumn!=-1;
                        } else {
                            // we always return the click column and not the model
                            //
                            return _table.isColumnSelected(clickColumn);
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.error("SQL error examining ARRAY column " + _table.getModel().getColumnName(modelColumn) + " (" + e.toString() + ")");
                }   
            }
            // If we end up here the user did not click over the top of a geometry column.
            // Search table for first SDO_Geometry/SDO_Point_Type/VERTEX_TYPE column
            //
            this.clickColumn = this.findAnyGeometryColumn();
            // If no column exists, return false
            //
            if ( clickColumn == -1 ) {
                this.clickColumn = this.findDimArrayColumn();
                if ( clickColumn == -1 ) {
                    return false;
                }
            }
            // Return true if found column:
            // 1. No selection exists
            // 2. The clicked geometry column is also selected 
            // 
            if ( _table.getSelectedColumnCount()==0 ) {
                return clickColumn!=-1;
            } else {
                return _table.isColumnSelected(clickColumn);
            }
        }
        return false;
    }
    
    private int findDimArrayColumn() 
    {
        if ( this.rst == null || this.meta==null  )
            return -1;
        
        // All processing is based on getting a not-null object in row 0!
        //
        int dimColumn = -1;
        try 
        {
            ARRAY dimArray = null;
            // We still haven't found the dimArray column
            // Grab first on of right type if exists
            //
            int colsToProcess = this.rst.getSelectedColumnCount()!=0
                                ? this.rst.getSelectedColumnCount()
                                : this._table.getColumnCount();
            int[] columns = this.rst.getSelectedColumnCount()!=0 
                            ? this.rst.getSelectedColumns()
                            : this._table.getSelectedColumns();
            int viewCol = -1;
            String columnName = "";
            for (int col = 0; col < colsToProcess; col++) 
            {
                viewCol = col;
                if ( this.rst.getSelectedColumnCount() > 0 ) {
                    viewCol = columns[col];
                }
                columnName = this.rst.getColumnName(viewCol);
                for (int rCol=1;rCol<=this.meta.getColumnCount();rCol++) {
                    try {
                        // Is the meta column in the current set of selected columns in the JTable?
                        //
                        if ( this.meta.getColumnLabel(rCol).equalsIgnoreCase(columnName) ) {
                            // Is it an SDO_GEOMETRY?
                            //
                            //LOGGER.debug(this.meta.getColumnLabel(rCol) + " of "+this.meta.getColumnType(rCol) + " at " + viewCol);
                            if (this.meta.getColumnType(rCol) == OracleTypes.ARRAY ) {
                                dimArray = this.findDimInfoValue(viewCol);
                                if (dimArray == null ) {
                                    break;
                                }
                                if ( dimArray.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_DIMARRAY) ) {
                                    return viewCol;
                                }
                            }
                            break;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("RenderResultSet.findAnyGeometryColumn(): Exception - " + e.getMessage());
                    }
                }
            }
        } catch (SQLException sqle) {
            LOGGER.warn("RenderResultSet.findAnyGeometryColumn(): SQLException - " + sqle.getMessage());
        }
        return dimColumn;
    }
    
    private boolean hasCompleteUserSdoMetadataFields() 
    {
        if ( this.rst == null || this.meta==null  ) {
            return false;
        }
                
        // Process that which the user has selected only
        //
        int colsToProcess = this.rst.getSelectedColumnCount()!=0
                            ? this.rst.getSelectedColumnCount()
                            : this._table.getColumnCount();
        int[] columns = this.rst.getSelectedColumnCount()!=0 
                        ? this.rst.getSelectedColumns()
                        : this._table.getSelectedColumns();
        int viewCol = -1;
        String columnName = "";
        int fieldCount = 0; // Must have all four fields
        for (int col = 0; col < colsToProcess; col++) 
        {
            viewCol = col;
            if ( this.rst.getSelectedColumnCount() > 0 ) {
                viewCol = columns[col];
            }
            columnName = this.rst.getColumnName(viewCol);
            if ( columnName.equalsIgnoreCase("TABLE_NAME") )  { fieldCount++; }
            if ( columnName.equalsIgnoreCase("COLUMN_NAME") ) { fieldCount++; }
            if ( columnName.equalsIgnoreCase("DIMINFO") )     { fieldCount++; }
            if ( columnName.equalsIgnoreCase("SRID") )        { fieldCount++; }
        }
        return fieldCount==4;
    }

    private int findAnyGeometryColumn() 
    {
        if ( this.rst == null || this.meta==null  )
            return -1;
        
        // All processing is based on getting a not-null object in row 0!
        //
        int mappableColumn = -1;
        try 
        {
            STRUCT geo = null;
            // We still haven't found the geometry column
            // Grab first on of right type if exists
            //
            int colsToProcess = this.rst.getSelectedColumnCount()!=0
                                ? this.rst.getSelectedColumnCount()
                                : this._table.getColumnCount();
            int[] columns = this.rst.getSelectedColumnCount()!=0 
                            ? this.rst.getSelectedColumns()
                            : this._table.getSelectedColumns();
            int viewCol = -1;
            String columnName = "";
            for (int col = 0; col < colsToProcess; col++) 
            {
                viewCol = col;
                if ( this.rst.getSelectedColumnCount() > 0 ) {
                    viewCol = columns[col];
                }
                columnName = this.rst.getColumnName(viewCol);
                for (int rCol=1;rCol<=this.meta.getColumnCount();rCol++) {
                    try {
                        // Is the meta column in the current set of selected columns in the JTable?
                        //
                        if ( this.meta.getColumnLabel(rCol).equalsIgnoreCase(columnName) ) {
                            // Is it an SDO_GEOMETRY?
                            //
                            if (this.meta.getColumnType(rCol) == OracleTypes.STRUCT ) {
                                geo = findStructValue(viewCol);
                                if (geo == null ) {
                                    break;
                                }
                                if ( geo.getSQLTypeName().indexOf("MDSYS.ST_")==0  ||
                                     geo.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_GEOMETRY) ||
                                     geo.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_POINT_TYPE) ||
                                     geo.getSQLTypeName().equals(Constants.TAG_MDSYS_VERTEX_TYPE) ) {
                                    return viewCol;
                                }
                            }
                            break;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("RenderResultSet.findAnyGeometryColumn(): Exception - " + e.getMessage());
                    }
                }
            }
      } catch (SQLException sqle) {
            LOGGER.warn("RenderResultSet.findAnyGeometryColumn(): SQLException - " + sqle.getMessage());
      }
      return mappableColumn;
    }

    private void writeXSD(String _outputObjectName,
                          String _flavour,
                          String _xsdFileName,
                          String _characterSet,
                          int    _mappableColumn) 
    {
        if (Strings.isEmpty(_xsdFileName) || Strings.isEmpty(_outputObjectName) )
            return;
        try {
            String xsdFileName = _xsdFileName;
            BufferedWriter xsdFile = null;
            String newLine = System.getProperty("line.separator");
            
            String xmlns_xs  = "  xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"";
            String xmlns_gml = "  xmlns:gml=\"http://www.opengis.net/gml\"";
            String import_gml = "<xs:import namespace=\"http://www.opengis.net/gml\" schemaLocation=\"http://schemas.opengis.net/gml/3.1.1/base/gml.xsd\"/>";
            String xmlns_flavour = _flavour.equalsIgnoreCase(Constants.XMLAttributeFlavour.FME.toString())==false
                                   ? "  xmlns:ogr=\"http://ogr.maptools.org/\""
                                   : "  xmlns:fme=\"http://www.safe.com/gml/fme\"";
            String targetNamespace_flavour = _flavour.equalsIgnoreCase(Constants.XMLAttributeFlavour.FME.toString())==false
                                             ? "  targetNamespace=\"http://ogr.maptools.org/\""
                                             : "  targetNamespace=\"http://www.safe.com/gml/fme\"";
  
            xsdFile = new BufferedWriter(new FileWriter(xsdFileName)); 
            xsdFile.write("<?xml version='1.0'  encoding='" + _characterSet + "' ?>" + newLine);
            xsdFile.write("<xs:schema " + newLine +
                          xmlns_xs + newLine +
                          xmlns_gml + newLine +
                          xmlns_flavour  + newLine +
                          targetNamespace_flavour + newLine + "  elementFormDefault=\"qualified\" version=\"1.0\">" + newLine +
                          import_gml + newLine);
            if ( _flavour.equalsIgnoreCase(Constants.XMLAttributeFlavour.OGR.toString()) ) {
                // Feature collection type is entirely OGR
                //
                xsdFile.write("<xs:element name=\"FeatureCollection\" type=\"ogr:FeatureCollectionType\" substitutionGroup=\"gml:_FeatureCollection\"/>" + newLine +
                              " <xs:complexType name=\"FeatureCollectionType\">" + newLine +
                              "  <xs:complexContent>" + newLine +
                              "    <xs:extension base=\"gml:AbstractFeatureCollectionType\">" + newLine +
                              "      <xs:attribute name=\"lockId\" type=\"xs:string\" use=\"optional\"/>" + newLine +
                              "      <xs:attribute name=\"scope\" type=\"xs:string\" use=\"optional\"/>" + newLine +
                              "    </xs:extension>" + newLine +
                              "  </xs:complexContent>" + newLine +
                              " </xs:complexType>" + newLine);                              
            }
            // Everything else is common
            //
            xsdFile.write(   "<xs:element name=\"" + _outputObjectName + "\" type=\"" + _flavour.toString().toLowerCase() +":" + _outputObjectName + "Type\" substitutionGroup=\"gml:_Feature\"/>" + newLine +
                             " <xs:complexType name=\"" + _outputObjectName + "Type\">" + newLine + 
                              "  <xs:complexContent>" + newLine + 
                              "    <xs:extension base=\"gml:AbstractFeatureType\">" + newLine + 
                              "      <xs:sequence>" + newLine);
            
            // Write common attribute XSD elements
            //
            int colsToProcess = this.rst.getSelectedColumnCount()==0?this._table.getColumnCount():this.rst.getSelectedColumnCount();
            int[] columns = this.rst.getSelectedColumns();
            int viewCol = -1;
            String columnName = "";
            for (int col = 0; col < colsToProcess; col++) 
            {
                viewCol = col;
                if ( this.rst.getSelectedColumnCount() > 0 ) {
                    viewCol = columns[col];
                }
                columnName = this._table.getColumnName(viewCol).replace("\"","");
                if ( viewCol == _mappableColumn ) {
                    xsdFile.write("    <xs:element name=\"geometryProperty\" type=\"gml:GeometryPropertyType\"" +
                                               "   nillable=\""  + (meta.isNullable(viewCol)==OracleResultSetMetaData.columnNullable) +
                                               "\" minOccurs=\"" + (meta.isNullable(viewCol)==OracleResultSetMetaData.columnNullable?"0":"1") +
                                               "\" maxOccurs=\"1\"/>" + newLine);
                } else {
                    String xsdDataType = "";
                    for (int rCol=1;rCol<=meta.getColumnCount();rCol++) {
                      // Is a supported column?
                      if ( meta.getColumnLabel(rCol).equalsIgnoreCase(columnName) ) {
                          if ( Tools.isSupportedType(meta.getColumnType(rCol),meta.getColumnTypeName(rCol))==false) 
                              continue;
                          xsdDataType = Tools.dataTypeToXSD(meta,rCol);
                          xsdFile.write("    <xs:element name=\"" + columnName + 
                                                      "\" nillable=\""  + (meta.isNullable(rCol)==OracleResultSetMetaData.columnNullable) +
                                                      "\" minOccurs=\"" + (meta.isNullable(rCol)==OracleResultSetMetaData.columnNullable?"0":"1") + 
                                                      "\" maxOccurs=\"1\">" + newLine + 
                                        "      <xs:simpleType>" + newLine +
                                        "        <xs:restriction base=\"xs:");
                          
                          int prec = meta.getPrecision(rCol);
                          int scale = meta.getScale(rCol);
                          if ( prec <= 0 ) {
                              prec = meta.getColumnDisplaySize(rCol);
                              scale = 0;
                          }
                          if ( xsdDataType.equalsIgnoreCase("string") ) {
                              xsdFile.write(xsdDataType + "\">" + newLine + 
                                            "          <xs:maxLength value=\"" + prec + "\" fixed=\"false\"/>" + newLine +
                                            "        </xs:restriction>" + newLine);
                          } else if ( xsdDataType.equalsIgnoreCase("clob") ) {
                              xsdFile.write("string" + "\">" + newLine + 
                                            "          <xs:minLength value=\"1\"/>" + newLine +
                                            "        </xs:restriction>" + newLine);
                          } else if ( xsdDataType.equalsIgnoreCase("float") || 
                                      xsdDataType.equalsIgnoreCase("double") ||
                                      xsdDataType.equalsIgnoreCase("date")   || 
                                      xsdDataType.equalsIgnoreCase("time")   || 
                                      xsdDataType.equalsIgnoreCase("dateTime") ) {
                              xsdFile.write(xsdDataType + "\"/>" + newLine);
                          } else {
                              xsdFile.write(xsdDataType + "\">" + newLine + 
                                                 "               <xs:totalDigits value=\"" + prec + "\"/>" + newLine +
                                (scale==0 ? "" : "               <xs:fractionDigits value=\"" + scale + "\"/>" + newLine ) +
                                                 "        </xs:restriction>" + newLine );
                          }
                          xsdFile.write( 
                            "      </xs:simpleType>" + newLine + 
                            "    </xs:element>" + newLine);
                          break;
                      }
                  }
                }
            }            
            xsdFile.write("      </xs:sequence>" + newLine +
                          "    </xs:extension>" + newLine +
                          "  </xs:complexContent>" + newLine +
                          " </xs:complexType>" + newLine +
                          "</xs:schema>");
            xsdFile.flush();
            xsdFile.close();
      } catch (IOException ioe) {
          LOGGER.warn("(RenderResultSet.writeXSD) IOException: " + ioe.getMessage());        
      } catch (SQLException sqle) {
          LOGGER.warn("(RenderResultSet.writeXSD) SQLException: " + sqle.getMessage());        
      } catch (Exception e) {
          LOGGER.warn("(RenderResultSet.writeXSD) General Exception: " + e.getMessage());
      }
    /* Not OGR/FME
    * <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:sql="urn:schemas-microsoft-com:xml-sql">
      </xsd:element>
        <xsd:complexType name="row">
           <xsd:choice maxOccurs="unbounded" minOccurs="0">
              <xsd:element name="CATALOG_NAME" type="xsd:string" sql:field="CATALOG_NAME"></xsd:element>
              <xsd:element name="DESCRIPTION" type="xsd:string" sql:field="DESCRIPTION"></xsd:element>
              <xsd:element name="ROLES" type="xsd:string" sql:field="ROLES"></xsd:element>
              <xsd:element name="DATE_MODIFIED" type="xsd:time" sql:field="DATE_MODIFIED"></xsd:element>
           </xsd:choice>
        </xsd:complexType>
      </xsd:schema>
      <row>
        <CATALOG_NAME>FoodMart 2000</CATALOG_NAME>
        <DESCRIPTION></DESCRIPTION>
        <ROLES>All Users</ROLES>
        <DATE_MODIFIED>3/11/2001 6:49:36 PM</DATE_MODIFIED>
      </row>
      */
  
    }
    
    /**
     * 
     * @method geoGeometryProperties
     * @return GEOMETRY_METADATA
     * @author Simon Greener, February 2011, Original Coding
     *          Could be moved to SDO_Geometry with GEOMETRY_METADATA being made part of SDO_Geometry.
     */
    private GeometryProperties getGeometryProperties() 
    {
        GeometryProperties geomMetadata = new GeometryProperties();
        STRUCT stGeom = null;

        try {
            int FULL_GTYPE = 2000;
            int DIMENSION  = 2;
            int rowsToProcess = this.rst.getSelectedRowCount()!=0
                              ? this.rst.getSelectedRowCount()
                              : ( _table.getLoadedRowCount() != 0
                                  ? _table.getLoadedRowCount() 
                                  : _table.getRowCount() );
            // Only examine up to 500 rows.
            //
            SpatialViewPanel svp = DockableSV.getSpatialViewPanel();
            geomMetadata.setSRID(svp.getActiveView().getSRIDAsInteger());
            geomMetadata.setGeometryType(Constants.GEOMETRY_TYPES.UNKNOWN);

            rowsToProcess = rowsToProcess > mainPrefs.getTableCountLimit() 
                            ? mainPrefs.getTableCountLimit() 
                            : rowsToProcess;
            int[] rows = this.rst.getSelectedRows();
            for (int row=0, viewRow=0; 
                 row < rowsToProcess; 
                 row++)
            {
                viewRow = row;
                if ( this.rst.getSelectedRowCount() > 0 ) {
                    viewRow   = rows[row];
                }
                Object obj = this._table.getValueAt(viewRow,this.clickColumn);
                if ( obj != null && obj instanceof STRUCT ) {
                    stGeom = (STRUCT)obj;
                    if ( stGeom != null ) {
                        if ( stGeom.getSQLTypeName().indexOf("MDSYS.ST_")==0 ) {
                            stGeom = SDO_GEOMETRY.getSdoFromST(stGeom);
                        }
                        if ( stGeom.getSQLTypeName().equals(Constants.TAG_MDSYS_SDO_GEOMETRY) ) {
                            FULL_GTYPE = SDO_GEOMETRY.getFullGType(stGeom, FULL_GTYPE);
                            DIMENSION = SDO_GEOMETRY.getDimension(stGeom, DIMENSION);
                            if ( DIMENSION > 3 ) {
                                geomMetadata.addShapeType(SDO_GEOMETRY.getShapeType(FULL_GTYPE,true));  // M
                                geomMetadata.addShapeType(SDO_GEOMETRY.getShapeType(FULL_GTYPE,false)); // Z
                            } else {
                                geomMetadata.addShapeType(SDO_GEOMETRY.getShapeType(FULL_GTYPE,false));
                            }
                            geomMetadata.setSRID(SDO_GEOMETRY.getSRID(stGeom, geomMetadata.getSRID()));
                            geomMetadata.setGeometryType(SDO_GEOMETRY.discoverGeometryType(SDO_GEOMETRY.getFullGType(stGeom,FULL_GTYPE),
                                                                                           geomMetadata.getGeometryType()));
                        } else {
                          FULL_GTYPE = SDO_GEOMETRY.getFullGType(stGeom,FULL_GTYPE);
                          DIMENSION  = FULL_GTYPE / 1000;                            
                          geomMetadata.addShapeType(SDO_GEOMETRY.getShapeType(FULL_GTYPE,false));
                        }
                    }
                }
            } // For rows
            geomMetadata.setFullGType(FULL_GTYPE);
            geomMetadata.setDimension(DIMENSION);
        } catch (SQLException sqle) {
            LOGGER.warn("RenderResultSet.getGeometryProperties(): Exception caught when examining geometries " + sqle.getMessage());
        }
        return geomMetadata;        
    }
    
    private LinkedHashMap<Integer,RowSetMetaData> getExportMetadata(int    colsToProcess,
                                                                    String mappableColumnName,
                                                                    int[]  columns) 
    {
        if ( this.rst == null || this.meta==null  ) {
            return null;
        }
      
        // ==================================================================
        // Map JTable columns to more accurate SQL Metadata
        // for use in conversion ...
        // Column's SQL Type from resultSet for use in export
        //
        LinkedHashMap<Integer,RowSetMetaData> resultMeta = new LinkedHashMap<Integer,RowSetMetaData>(colsToProcess);
        
        // We use viewCol as the index as columnName will fail if column is duplicated 
        // (which SQL Developer doesn't seem to let happen, but let's be safe)
        //
        int viewCol = -1;
        String columnName = "";
        try {
            for (int col = 0; col < colsToProcess; col++) 
            {
                viewCol = col;
                if ( this.rst.getSelectedColumnCount() > 0 ) {
                    viewCol = columns[col];
                }
                // Should we? viewCol = this.rst.convertColumnIndexToModel(viewCol);
                columnName = this.rst.getColumnName(viewCol);
                for (int rCol=1; rCol <= meta.getColumnCount(); rCol++) {
                    try {
                        if ( this.meta.getColumnLabel(rCol).equalsIgnoreCase(columnName) ) {
                            RowSetMetaData rsMD = new OraRowSetMetaDataImpl();
                            rsMD.setColumnCount(1);  // Must go first
                            rsMD.setCatalogName(1, "");
                            if ( columnName.equals(mappableColumnName) ) {
                                rsMD.setCatalogName(1,mappableColumnName);
                            }
                            rsMD.setColumnName(1, this.meta.getColumnLabel(rCol));
                            rsMD.setColumnType(1, this.meta.getColumnType(rCol));
                            rsMD.setColumnDisplaySize(1,this.meta.getColumnDisplaySize(rCol));
                            rsMD.setColumnTypeName(1, this.meta.getColumnTypeName(rCol));
                            rsMD.setPrecision(1,this.meta.getPrecision(rCol)<0?0:meta.getPrecision(rCol));  // BLOBs and CLOBs have -1 precision
                            rsMD.setScale(1,this.meta.getScale(rCol)<0?0:meta.getScale(rCol));              // Scales like -127 exist for binary_float and binary_double but don't contribute to conversion
                            resultMeta.put(viewCol+1,rsMD);
                        break;
                      }
                  } catch (Exception e) {
                      LOGGER.warn("RenderResultSet.getExportMetadata(): Error processing " + this.meta.getColumnLabel(rCol) + "'s metadata " + e.getMessage());
                  }
                }
            }
        } catch (SQLException sqle) {
            LOGGER.warn("RenderResultSet.getExportMetadata(): Exception caught - " + sqle.getMessage());
        }
        return resultMeta;
    }
    
    private String getUserName() {
        OracleConnection conn = (OracleConnection)this.getConnection();
        if ( conn==null ) {
            return "";
        }
        try {
            return conn.getMetaData().getUserName();
        } catch (SQLException e) {
            return "";
        }
    }
    
    private void exportResultSet(Constants.EXPORT_TYPE _exportType) 
    {
      try 
      { 
          if ( this.rst == null ) {
              throw new IllegalArgumentException("No ResultSet Table to process.");
          }
          
          if ( this._table == null ) {
              throw new IllegalArgumentException("No SQL Developer Grid Table to process.");
          }
          OracleConnection conn = (OracleConnection)this.getConnection();
          if ( conn==null ) {
            throw new IllegalArgumentException("Could not access database connection of Query.");
          }
  
          int mappableColumn = -1;
          String mappableColumnName = "";
          // Find geometry column to be mapped
          //
          if ( this.clickColumn == -1 ) {
            mappableColumn = findAnyGeometryColumn();
            if ( mappableColumn == -1 ) {
                throw new IllegalArgumentException(this.propertyManager.getMsg("NO_SDO_GEOMETRY"));
            }
          } else {
              mappableColumn = this.clickColumn;
          }
          mappableColumnName       = this._table.getColumnName(mappableColumn);
          
          String characterSet = MetadataTool.getCharacterSet(conn);
          
          int colsToProcess = this.rst.getSelectedColumnCount()==0?this._table.getColumnCount():rst.getSelectedColumnCount();
          int[]     columns = this.rst.getSelectedColumns();
  
          // ==================================================================
          // Map JTable columns to more accurate SQL Metadata
          // for use in conversion ...
          // Column's SQL Type from resultSet for use in export
          //
          LinkedHashMap<Integer,RowSetMetaData> resultMeta = getExportMetadata(colsToProcess,mappableColumnName,columns);
          
          // Find out what sorts of shapes we have in this selection (mainly for use in shapefile export)
          //
          this.geometryMetadata = getGeometryProperties();
          
          String initialFileName = "";
          if ( !Strings.isEmpty(this._table.getDefaultExportName()) ) {
            initialFileName = this._table.getDefaultExportName();
          } 
          initialFileName = this.mainPrefs.getExportDirectory() + 
                            mappableColumnName + 
                            (Strings.isEmpty(initialFileName) ? "" : "_" + initialFileName) + 
                            "." + 
                            _exportType.toString().toLowerCase();
          // Get:
          // 1. Output directory + filename; 
          // 2. Whether to skip rows with null SRID;
          // 3. Get SRID if SRID is NULL;
          // 4. Get Charset;
          // 5. Output attributes as well?
          //
          ExporterDialog options = new ExporterDialog(null, // Ide.getMainWindow(),
                                                      conn,
                                                      _exportType);
          options.setFilename(initialFileName);
          options.setSkipNullGeometry(true);
          // if Shapefile export and no columns other than mappableColumn we show message
          //
          if ( resultMeta.size()==1 && 
               ( _exportType == Constants.EXPORT_TYPE.SHP  ||
                 _exportType == Constants.EXPORT_TYPE.TAB ) ) {
            options.setLeftLabelText(this.propertyManager.getMsg("NO_USER_ATTRIBUTES"));
            options.setAttributes(false);
          } else {
            options.setAttributes(resultMeta.size()>1);
          }
          options.setSRID(this.geometryMetadata.getSRID());
          options.setCharset(characterSet);          
          options.setCenterLabelText(conn.getMetaData().getUserName());
          options.setRightLabelText(_exportType.toString());
          
          if (_exportType == Constants.EXPORT_TYPE.SHP || 
              _exportType == Constants.EXPORT_TYPE.TAB ) {
              if ( this.geometryMetadata.getShapeTypes() != null && this.geometryMetadata.getShapeTypes().size()>0 ) {
                  options.setShapeTypes(this.geometryMetadata.getShapeTypes());
                  options.setShapefileType(Constants.SHAPE_TYPE.valueOf(this.geometryMetadata.getShapeTypes().get(0)));
              }
          }
          this.geometryMetadata.setShapefileType((ShapeType)options.getShapefileType(true));

          // Display
          //
          options.setAlwaysOnTop(true);
          options.setVisible(true);
          if ( options.wasCancelled() ) {
              return;
          }
            
          characterSet = options.getCharset();
          
          if ( ( _exportType==Constants.EXPORT_TYPE.GML || 
                 _exportType==Constants.EXPORT_TYPE.KML) && 
               options.getSRID() == Constants.SRID_NULL ) {
              return;
          }
  
          int rowsToProcess = this.rst.getSelectedRowCount()!=0
                              ? this.rst.getSelectedRowCount()
                              : ( this._table.getLoadedRowCount() != 0
                                  ? this._table.getLoadedRowCount() 
                                  : this._table.getRowCount() );
          
          runExport work = new runExport(conn,
                                         this.rst,
                                         mappableColumn,
                                         mappableColumnName,
                                         options,
                                         _exportType,
                                         options.getAttributeFlavour(),
                                         characterSet,
                                         resultMeta,
                                         this.geometryMetadata);
          long startTime = System.currentTimeMillis(); 
          ProgressBar progress = new ProgressBar(this._table, 
                                                 _exportType.toString() + " Exporter", 
                                                 work, 
                                                 false /* task is of determinate duration */);
          progress.setCancelable(true); 
          work.setProgressBar(progress);
          progress.start("Exporting " + rowsToProcess + " record" + ((rowsToProcess==1) ? "" : "s") + ", please wait...",
                         _exportType.toString(),
                         0,
                         100,
                         50);
          progress.waitUntilDone();
          if (!Strings.isEmpty(work.getErrorMessage()) ) { 
              // show error message
              JOptionPane.showMessageDialog(null,
                                            work.getErrorMessage(),
                                            Constants.GEORAPTOR,
                                            JOptionPane.ERROR_MESSAGE);
          } else {
              java.util.Date endTime = new java.util.Date();
              long timeDiff = ( System.currentTimeMillis() - startTime );
              String processingTime = Tools.milliseconds2Time(timeDiff);
              work.showExportStats(processingTime);
          }
          progress = null;
          work = null;
          options.dispose();
        } catch (Exception e) {
           JOptionPane.showMessageDialog(null, "exportResultSet Error: " + _exportType.toString());
        }
        return ;
    }
    
    private boolean isSQLDeveloper30(ResultSetTableModel _rstm) {
        boolean ret = false;
        Class cl = _rstm.getClass();
        // If 3.0 then rstm.getResultSetMetaData();
        // If 2.1 then rstm.getResultSet().getMetaData();
        Method[] methods = cl.getDeclaredMethods();
        for (Method m : methods) {
          String name = m.getName();
          if ( !Strings.isEmpty(name) ) {
              if ( name.equals("getResultSetMetaData") )
                  return true;
          }
          // Class retType = m.getReturnType();
          // Class[] paramTypes = m.getParameterTypes();
        }
        return ret;
    }

    public List<JGeometry> getGeoSetFromResultSet() 
    {
        List<JGeometry> geoSet = null;
        try
        {
            if ( this.rst == null ) {
                throw new IllegalArgumentException("No ResultSet Table to process.");
            }
            if ( this._table == null ) {
                throw new IllegalArgumentException("No SQL Developer Grid Table to process.");
            }
            int mappableColumn = -1;
            if ( this.clickColumn == -1 ) {
              mappableColumn = findAnyGeometryColumn();
              if ( mappableColumn == -1 ) {
                  throw new IllegalArgumentException(this.propertyManager.getMsg("NO_SDO_GEOMETRY"));
              }
            } 
            
            int colsToProcess = this.rst.getSelectedColumnCount()==0 ? this._table.getColumnCount() : this.rst.getSelectedColumnCount();
            // For rows, we have to include what rows are actually loaded into the JTable as we can only
            // convert those that are visible to a GraphicTheme otherwise conversion of the ones not in 
            // the table will be corrupted as data conversion of column data types will fail
            //
            int rowsToProcess = this.rst.getSelectedRowCount()!=0
                                ? this.rst.getSelectedRowCount()
                                : ( this._table.getLoadedRowCount() != 0
                                    ? this._table.getLoadedRowCount() 
                                    : this._table.getRowCount() );
            int[] rows = this.rst.getSelectedRows();
            int viewRow = -1;
            int[] columns = this.rst.getSelectedColumns();
            int viewCol = -1;

            geoSet = new ArrayList<JGeometry>(rowsToProcess);
            String        columnName = "";
            String         classname = "";
            String    structTypeName = "";
            STRUCT            stGeom = null;
            JGeometry          jGeom = null;  
            int           FULL_GTYPE = 2001;
            int                 SRID = Constants.SRID_NULL;
            try { SRID = mainPrefs.getSRIDAsInteger(); } catch (Exception e) { LOGGER.warn("(getGeoSetFromResultSet) getSRID Error: " + e.getMessage()); }
            Constants.GEOMETRY_TYPES geometryType = Constants.GEOMETRY_TYPES.UNKNOWN;

            for (int row=0; row < rowsToProcess; row++) 
            {
                stGeom = null; 
                jGeom = null;
                viewRow = row;
                if ( this.rst.getSelectedRowCount() > 0 ) {
                    viewRow   = rows[row];
                }
                for (int col = 0; col < colsToProcess; col++) 
                {
                    viewCol = col;
                    if ( this.rst.getSelectedColumnCount() > 0 ) {
                        viewCol   = columns[col];
                    }
                    classname = this._table.getColumnClass(viewCol).getName();
                    columnName = this._table.getColumnName(viewCol);
                    try 
                    {
                        if ( ! classname.equals("oracle.sql.STRUCT") ) 
                            continue;
                        stGeom = (STRUCT)this._table.getValueAt(viewRow,viewCol);
                        if ( stGeom == null ) {
                            continue; // Can't render a null geometry so skip it  
                        }
                        // If an ST_ geometry then extract its sdo_geometry
                        structTypeName = stGeom.getSQLTypeName();
                        if ( structTypeName.indexOf("MDSYS.ST_")==0 ) {
                            stGeom = SDO_GEOMETRY.getSdoFromST(stGeom);
                            structTypeName = stGeom.getSQLTypeName();
                        } 
                        if ( structTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ) {
                            FULL_GTYPE = SDO_GEOMETRY.getFullGType(stGeom,FULL_GTYPE);
                            SRID = SDO_GEOMETRY.getSRID(stGeom,SRID);
                            geometryType = SDO_GEOMETRY.discoverGeometryType(FULL_GTYPE,geometryType);
                            jGeom        = JGeometry.load(stGeom);
                        } else {
                            if ( structTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) ) {
                                Datum data[] = stGeom.getOracleAttributes();
                                double x = ((NUMBER)data[0]).doubleValue();
                                double y = ((NUMBER)data[1]).doubleValue();
                                double z = (data[2] != null)
                                            ? ((NUMBER)data[2]).doubleValue() 
                                            : (data[3] != null) 
                                              ? ((NUMBER)data[3]).doubleValue() 
                                              : Double.NaN;
                                double m = (data[2] != null && data[3] != null) ? ((NUMBER)data[3]).doubleValue() : Double.NaN;
                                jGeom = (Double.isNaN(z) && Double.isNaN(m))
                                        ? new JGeometry(x,y,SRID)
                                        : (Double.isNaN(m)
                                          ? new JGeometry(x,y,z,SRID)
                                          : new JGeometry(x,y,z,m,SRID));
                            } else if ( structTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) ) {
                                double[] ords = SDO_GEOMETRY.asDoubleArray(stGeom,Double.NaN);
                                jGeom = Double.isNaN(ords[2])
                                    ? new JGeometry(ords[0],ords[1],        SRID)
                                    : new JGeometry(ords[0],ords[1],ords[2],SRID);
                            }
                        }
                        geoSet.add(jGeom);
                  } catch (SQLException sqle) {
                    LOGGER.error("RenderResultSet.processResultSet(): SQL Error converting column/type " + columnName + "/" + classname);
                  } 
                }
            }
        } catch (IllegalArgumentException iae) {
          JOptionPane.showMessageDialog(null, iae.getMessage());
        }
        return geoSet;
    }
    
    public void displayImageOfSelectedGeometries() {
        final List<JGeometry> geoSet = this.getGeoSetFromResultSet();
        if (geoSet == null ) {
            return;
        }
        final Dimension imageSize = new Dimension(this.mainPrefs.getPreviewImageWidth(),
                                                  this.mainPrefs.getPreviewImageHeight());
        final OracleConnection conn = (OracleConnection)this.getConnection();
        final SpatialViewPanel  svp = DockableSV.getSpatialViewPanel();
        SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               JFrame frame = new JFrame(Constants.GEORAPTOR + 
                                         (geoSet.size()==1 
                                         ? " - Preview Selected Geometry" 
                                         : " - Preview " + geoSet.size() + " Selected Geometries"));
               frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
               frame.setLocationRelativeTo(svp);
               // Add preview object to frame
               Preview prevObj = new Preview((JGeometry)null, geoSet, imageSize, conn, frame);
               prevObj.addComponentListener(prevObj);
               frame.setContentPane(prevObj);
               frame.setMinimumSize(prevObj.getSize());
               frame.pack();
               frame.setVisible(true);
               frame = null;
               prevObj = null;
               return;
           }
        });
    }
    
    private boolean copyMBRToClipboard() 
    {
        List<JGeometry> geoSet = this.getGeoSetFromResultSet();
        if (geoSet == null || geoSet.size()==0 ) {
            return false;
        }
        Envelope mbr = new Envelope(Constants.MAX_PRECISION);
        String srid = null;
        Iterator<JGeometry> iter = geoSet.iterator();
        while (iter.hasNext() ) {
            JGeometry jGeom = iter.next();
            if (Strings.isEmpty(srid) ) {
                srid = jGeom.getSRID()<1?"NULL":""+jGeom.getSRID();
            }
            mbr.setMaxMBR(iter.next().getMBR());
        }
        if ( mbr.isNull() )
            return false;
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        SpatialViewPanel svp = DockableSV.getSpatialViewPanel();
        StringSelection ss = new StringSelection(mbr.toSdoGeometry(srid,svp.getActiveView().getPrecision(false)));
        clipboard.setContents(ss, ss);
        Toolkit.getDefaultToolkit().beep();
        return true;        
    }
    
    private boolean copySdoMetadataSQLClipboard(sqlType _sqlType) 
    {
        // This function can only be called if table_name,column_name,diminfo and srid columns exist in resultset....
        // LOGGER.info("Not yet implemented.");
        OracleConnection conn = (OracleConnection)this.getConnection();
        if ( conn==null ) {
          throw new IllegalArgumentException("Could not access database connection of Query.");
        }
        String sqlStmt = "";
        try
        {
            if ( this.rst == null ) {
                throw new IllegalArgumentException("No ResultSet Table to process.");
            }
            if ( this._table == null ) {
                throw new IllegalArgumentException("No SQL Developer Grid Table to process.");
            }
            int colsToProcess = this.rst.getSelectedColumnCount()==0 
                                 ? this._table.getColumnCount() 
                                 : this.rst.getSelectedColumnCount();
            // For rows, we have to include what rows are actually loaded into the JTable as we can only
            // convert those that are visible to a GraphicTheme otherwise conversion of the ones not in 
            // the table will be corrupted as data conversion of column data types will fail
            //
            int rowsToProcess = this.rst.getSelectedRowCount()!=0
                                ? this.rst.getSelectedRowCount()
                                : ( this._table.getLoadedRowCount() != 0
                                    ? this._table.getLoadedRowCount() 
                                    : this._table.getRowCount() );
            int[] rows = this.rst.getSelectedRows();
            int viewRow = -1;
            int[] columns = this.rst.getSelectedColumns();
            int viewCol = -1;

            MetadataEntry  me = null;
            String columnName = "";
            String  classname = "";
            String   typeName = "";
            ARRAY     diminfo = null;
            for (int row=0; row < rowsToProcess; row++) 
            {
                me = new MetadataEntry();
                diminfo = null; 
                viewRow = row;
                if ( this.rst.getSelectedRowCount() > 0 ) {
                    viewRow   = rows[row];
                }
                for (int col = 0; col < colsToProcess; col++) 
                {
                    viewCol = col;
                    if ( this.rst.getSelectedColumnCount() > 0 ) {
                        viewCol   = columns[col];
                    }
                    classname  = this._table.getColumnClass(viewCol).getName();
                    columnName = this._table.getColumnName(viewCol);
                    try 
                    {
                        if ( columnName.equalsIgnoreCase("DIMINFO") ||
                             columnName.equalsIgnoreCase("TABLE_NAME") ||
                             columnName.equalsIgnoreCase("COLUMN_NAME") ||
                             columnName.equalsIgnoreCase("SRID") ) 
                        {
                            // Now the values
                            //
                            if ( columnName.equalsIgnoreCase("DIMINFO") && 
                                 classname.equals("oracle.sql.ARRAY") ) {
                                diminfo = (ARRAY)this._table.getValueAt(viewRow,viewCol);
                                if ( diminfo != null ) {
                                    typeName = diminfo.getSQLTypeName();
                                    if ( typeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_DIMARRAY) ) {
                                        me.add(diminfo);
                                    } 
                                }
                            } else if ( columnName.equalsIgnoreCase("OWNER") ) {
                                Object obj = this._table.getValueAt(viewRow,viewCol);                                
                                me.setSchemaName(obj.toString());
                            } else if ( columnName.equalsIgnoreCase("TABLE_NAME") ) {
                                Object obj = this._table.getValueAt(viewRow,viewCol);                                
                                me.setObjectName(obj.toString());
                            } else if ( columnName.equalsIgnoreCase("COLUMN_NAME") ) {
                                Object obj = this._table.getValueAt(viewRow,viewCol);
                                me.setColumnName(obj.toString());
                            } else if (columnName.equalsIgnoreCase("SRID") ) {
                                Object obj = this._table.getValueAt(viewRow,viewCol);
                                if ( obj instanceof oracle.sql.NUMBER ) {
                                    NUMBER num = (NUMBER)obj;
                                    me.setSRID(String.valueOf(num.longValue()));
                                } else {
                                    me.setSRID(obj.toString());
                                }
                            }
                        }
                  } catch (SQLException sqle) {
                    LOGGER.error("RenderResultSet.processResultSet(): SQL Error converting column/type " + columnName + "/" + classname);
                  } 
                }
                sqlStmt += (Strings.isEmpty(sqlStmt) ? "" : "\n") + 
                           (_sqlType == sqlType.INSERT 
                            ? me.insertSQL(false)
                            : (_sqlType == sqlType.UPDATE 
                               ? me.updateSQL()
                               : me.deleteSQL()));
            }
        } catch (Exception e) {
            LOGGER.error("Copying of DIMINFO as SQL statement (" + _sqlType.toString() + ") to clipboard produced error: " + e.getMessage());
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
        if (Strings.isEmpty(sqlStmt) ) {
            return false;
        }
        // remove last commas and concatentate
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection ss = new StringSelection(sqlStmt);
        clipboard.setContents(ss, ss);
        Toolkit.getDefaultToolkit().beep();
        return true;
    }
    
    private boolean copyDimInfoToClipboard(DiminfoCopyType _copyType) 
    {
        OracleConnection conn = (OracleConnection)this.getConnection();
        if ( conn==null ) {
          throw new IllegalArgumentException("copyDimInfoToClipboard: Could not access queries database connection.");
        }
        
        String clipboardText = ""; 
        try
        {
            if ( this.rst == null ) {
                throw new IllegalArgumentException("copyDimInfoToClipboard: No ResultSet Table to process.");
            }
            if ( this._table == null ) {
                throw new IllegalArgumentException("copyDimInfoToClipboard: No SQL Developer Grid Table to process.");
            }
            int mappableColumn = -1;
            if ( this.clickColumn == -1 ) {
              mappableColumn = this.findDimArrayColumn();
              if ( mappableColumn == -1 ) {
                  throw new IllegalArgumentException(this.propertyManager.getMsg("NO_DIMINFO"));
              }
            } 
            
            int colsToProcess = this.rst.getSelectedColumnCount()==0 ? this._table.getColumnCount() : this.rst.getSelectedColumnCount();
            // For rows, we have to include what rows are actually loaded into the JTable as we can only
            // convert those that are visible to a GraphicTheme otherwise conversion of the ones not in 
            // the table will be corrupted as data conversion of column data types will fail
            //
            int rowsToProcess = this.rst.getSelectedRowCount()!=0
                                ? this.rst.getSelectedRowCount()
                                : ( this._table.getLoadedRowCount() != 0
                                    ? this._table.getLoadedRowCount() 
                                    : this._table.getRowCount() );
            int[] rows = this.rst.getSelectedRows();
            int viewRow = -1;
            int[] columns = this.rst.getSelectedColumns();
            int viewCol = -1;

            String  columnName = "";
            String   classname = "";
            String    typeName = "";
            ARRAY      diminfo = null;
            MetadataEntry   me = null;
            String SRID_marker = "-9999";
            int           SRID = Constants.SRID_NULL;   
            try { SRID = mainPrefs.getSRIDAsInteger(); } catch (Exception e) { }
            for (int row=0; row < rowsToProcess; row++) 
            {
                diminfo     = null; 
                me          = new MetadataEntry(null,null,null,SRID_marker);
                viewRow     = row;
                if ( this.rst.getSelectedRowCount() > 0 ) {
                    viewRow   = rows[row];
                }
                SRID = Constants.SRID_NULL;
                for (int col = 0; col < colsToProcess; col++) 
                {
                    viewCol = col;
                    if ( this.rst.getSelectedColumnCount() > 0 ) {
                        viewCol   = columns[col];
                    }
                    classname = this._table.getColumnClass(viewCol).getName();
                    columnName = this._table.getColumnName(viewCol);
                    try 
                    {
                        // Note: DIMINFO may appear before SRID or vice versa
                        //
                        if ( columnName.equalsIgnoreCase("DIMINFO") && 
                             classname.equals("oracle.sql.ARRAY") ) {
                            diminfo = (ARRAY)this._table.getValueAt(viewRow,viewCol);
                            if ( diminfo != null ) {
                                typeName = diminfo.getSQLTypeName();
                                if ( ! typeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_DIMARRAY) ) {
                                    diminfo = null;
                                } 
                            }
                        } else if (columnName.equalsIgnoreCase("SRID") ) {
                            Object obj = this._table.getValueAt(viewRow,viewCol);
                            if ( obj instanceof oracle.sql.NUMBER ) {
                                NUMBER num = (NUMBER)obj;
                                if ( num==null ) {
                                    SRID = Constants.SRID_NULL;
                                } else {
                                    SRID = num.intValue();
                                }
                            }
                        }
                    } catch (SQLException sqle) {
                        LOGGER.error("RenderResultSet.processResultSet(): SQL Error converting column/type " + columnName + "/" + classname);
                    }
                }  // for cols ....
                if ( diminfo != null ) {
                    // LOGGER.info("diminfo= " + this.renderTool.renderDimArray(diminfo, false) + " " + _copyType.toString() + " " + SRID);
                    if ( _copyType.equals(DiminfoCopyType.DIMINFO)) {
                        clipboardText += (Strings.isEmpty(clipboardText) 
                                         ? "" 
                                         : "\n" ) + 
                                         RenderTool.renderDimArray(diminfo, false);
                    } else { /* _copyType.equals(DiminfoCopyType.SDO_GEOMETRY) */
                        me.setSRID(SRID);
                        me.add(diminfo);
                        clipboardText += (Strings.isEmpty(clipboardText) 
                                         ? "" 
                                         : "\n" ) + 
                                         me.toSdoGeometry();
                    }
                }
            } // for rows ...
        } catch (Exception e) {
            LOGGER.error("copyDimInfoToClipboard: Copying of DIMINFO to clipboard produced error: " + e.getMessage());
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
        if (Strings.isEmpty(clipboardText) ) {
            return false;
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection ss = new StringSelection(clipboardText);
        clipboard.setContents(ss, ss);
        Toolkit.getDefaultToolkit().beep();
        return true;
    }
    
    private boolean copyToClipboard() 
    {
        OracleConnection conn = (OracleConnection)this.getConnection();
        if ( conn==null ) {
          throw new IllegalArgumentException("Could not access database connection of Query.");
        }

        List<JGeometry> geoSet = this.getGeoSetFromResultSet();
        if (geoSet == null || geoSet.size()==0 ) {
            return false;
        }
        String clipboardText = ""; 
        Iterator<JGeometry> iter = geoSet.iterator();
        while (iter.hasNext() ) {
            try {
                clipboardText += (Strings.isEmpty(clipboardText) ? "" : "\n" ) +
                    SDO_GEOMETRY.convertGeometryForClipboard(JGeometry.store(iter.next(),conn),conn);
            } catch (SQLException e) {
                LOGGER.error("Conversion of JGeometry to clipboard produced error: " + e.getMessage());
            }
        }
        if (Strings.isEmpty(clipboardText) ) {
            return false;
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection ss = new StringSelection(clipboardText);
        clipboard.setContents(ss, ss);
        Toolkit.getDefaultToolkit().beep();
        return true;
    }
    
    private boolean createWorksheetSQLLayer() {
        try
        {
            if ( this.rst == null ) {
                throw new IllegalArgumentException("No ResultSet Table to process.");
            }
            String sqlCmd = this.rst.getCurrentSql();
            if (Strings.isEmpty(sqlCmd)) {
                throw new IllegalArgumentException("No SQL Command available.");
            }
            if ( this._table == null ) {
                throw new IllegalArgumentException("No SQL Developer Grid Table to process.");
            }
            SpatialViewPanel svp = DockableSV.getSpatialViewPanel();
            if ( svp == null ) {
                throw new IllegalArgumentException("Could not access GeoRaptor's Spatial View Panel.");
            }
            int         mappableColumn = -1;
            String  mappableColumnName = "";
            if ( this.clickColumn == -1 ) {
              mappableColumn = findAnyGeometryColumn();
              if ( mappableColumn == -1 ) {
                  throw new IllegalArgumentException(this.propertyManager.getMsg("NO_SDO_GEOMETRY"));
              }
            } else {
                mappableColumn = this.clickColumn;
            }
            mappableColumnName =  this._table.getColumnName(mappableColumn);
            int rowsToProcess = ( this._table.getLoadedRowCount() != 0
                                  ? this._table.getLoadedRowCount() 
                                  : this._table.getRowCount() );
            if (rowsToProcess==0) {
                throw new IllegalArgumentException("SQL appears to have generated no rows, so is not mappable.");
            }
            // See if can extract schema and table name from SQL
            //
            String layerName = "",
                  schemaName = "",
                       token = "", 
                   tableName = "",
                      modSQL = sqlCmd;
            // Get rid of wrapped SELECT ...
            modSQL = modSQL.substring(modSQL.indexOf("(")+1);
            modSQL = modSQL.substring(0,modSQL.length()-1);
            StringTokenizer st = new StringTokenizer(modSQL," (),",false /* Do not return separators */);
            while (st.hasMoreTokens() ) {
                token = st.nextToken().replaceAll("\"", "").toUpperCase();
                if ( token.equals("FROM") ) {
                    tableName = st.nextToken().replaceAll("\"", "").toUpperCase();
                    break;
                }
            }
            if (Strings.isEmpty(tableName)) {
                if ( !Strings.isEmpty(this.rst.getName()) ) {
                    layerName = this.rst.getName();
                } else {
                    layerName = this.rst.getDefaultExportName();
                }
            } else {
                layerName = tableName;
                int dotIndex = tableName.indexOf(".");
                if (dotIndex > -1) {
                    schemaName = tableName.substring(0,dotIndex-1);
                    tableName  = tableName.substring(dotIndex+1);
                }
            }
            schemaName = Strings.isEmpty(schemaName)?this.getUserName():schemaName;
            LOGGER.debug("schemaName(" + schemaName+") tableName("+tableName + ")");
            
            // MAKE SURE THE MAP VIEW IS OPEN ...
            //
            makeVisible();

            STRUCT                      stGeom = null;
            JGeometry                       jGeom = null;  
            int                        FULL_GTYPE = 2001;
            int                              SRID = Constants.SRID_NULL;
            MetadataEntry                      me = null;
            SVWorksheetLayer      sWorksheetLayer = null;
            Envelope              layerMBR = new Envelope(Constants.MAX_PRECISION);
            Constants.GEOMETRY_TYPES geometryType = Constants.GEOMETRY_TYPES.UNKNOWN;

            me = new MetadataEntry(schemaName,
                                   tableName,
                                   mappableColumnName,
                                   null);

            int[] rows = this.rst.getSelectedRows();
            int viewRow = -1;
            LOGGER.info("Calculation of SQL's layer extent based on " + rowsToProcess + " rows.");
            for (int row=0; row < rowsToProcess; row++) 
            {
                viewRow = row;
                if ( this.rst.getSelectedRowCount() > 0 ) {
                    viewRow   = rows[row];
                }
                try {
                    stGeom = null; 
                        jGeom = null;
                    stGeom = (STRUCT)this._table.getValueAt(viewRow,mappableColumn);
                    if ( stGeom == null ) {
                        continue; 
                    }
                    FULL_GTYPE = SDO_GEOMETRY.getFullGType(stGeom,FULL_GTYPE);
                    geometryType = SDO_GEOMETRY.discoverGeometryType(FULL_GTYPE,geometryType);
                    SRID = SDO_GEOMETRY.getSRID(stGeom,SRID);
                    layerMBR.setMaxMBR(SDO_GEOMETRY.getGeoMBR(stGeom));
                    if ( viewRow == 0 ) {
                        me.setSRID((SRID==Constants.SRID_NULL || SRID==0) ? "NULL"  : String.valueOf(SRID));
                    } 
                } catch (Exception e) {
                    LOGGER.error("RenderResultSet.processResultSet(): Error converting " + mappableColumnName + " (" + e.getMessage() +")");
                }
            }
            me.add(layerMBR);
            if ( me.getMBR()==null || me.getMBR().isNull() || me.getMBR().isInvalid() ) {
                // Need to compute mbr
                throw new IllegalArgumentException("Cannot create starting MBR from result set, so is not mappable.");
            }
            LOGGER.debug(me.toString());
            // Create WorksheetLayer from known information
            SpatialView sView = svp.getMostSuitableView(SRID);
            sWorksheetLayer = new SVWorksheetLayer(sView,
                                                   layerName,
                                                   layerName,
                                                   layerName,
                                                   me         /*_me - MetadataEntry */,
                                                   true       /*_draw*/,
                                                   sqlCmd     /*_querySQL*/,
                                                   (this._table.getRowCount()==rowsToProcess)?false:true /*_computeMBR*/);
            // Set additional properties
            sWorksheetLayer.setConnection(this.getConnectionName());
            sWorksheetLayer.setGeometryType(geometryType);
            if ( this.mainPrefs.isRandomRendering()) {
                sWorksheetLayer.getStyling().setAllRandom();
            }
            // Finally add to view
            if ( svp.addLayerToView(sWorksheetLayer,false/*zoom*/) ) {
               svp.redraw();
            }
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(null, iae.getMessage());
            return false;
        }
        return true;
    }

    private boolean mapSelectedGeometries(   int _commandId,
                                          String _labelMenuValue) 
    {
        try
        {
            if ( this.rst == null ) {
                throw new IllegalArgumentException("No ResultSet Table to process.");
            }
            String sqlCmd = this.rst.getCurrentSql();
            
            if ( this._table == null ) {
                throw new IllegalArgumentException("No SQL Developer Grid Table to process.");
            }
            
            OracleConnection conn = (OracleConnection)this.getConnection();
            if ( conn==null ) {
              throw new IllegalArgumentException("Could not access database connection of Query.");
            }

            SpatialViewPanel svp = DockableSV.getSpatialViewPanel();
            if ( svp == null ) {
                throw new IllegalArgumentException("Could not access GeoRaptor's Spatial View Panel.");
            }
            // MAKE SURE THE VIEW IS OPEN ...
            //
            makeVisible();
            
            int     mappableColumn = -1;
            if ( this.clickColumn == -1 ) {
                mappableColumn = findAnyGeometryColumn();
                if ( mappableColumn == -1 ) {
                    throw new IllegalArgumentException(this.propertyManager.getMsg("NO_SDO_GEOMETRY"));
                }
            } else {
                mappableColumn = this.clickColumn;
            }
            
            String mappableColumnName = "";
            mappableColumnName =  this._table.getColumnName(mappableColumn);
  
            int colsToProcess = this.rst.getSelectedColumnCount()==0 ? this._table.getColumnCount() : this.rst.getSelectedColumnCount();
            // For rows, we have to include what rows are actually loaded into the JTable as we can only
            // convert those that are visible to a GraphicTheme otherwise conversion of the ones not in 
            // the table will be corrupted as data conversion of column data types will fail
            //
            int rowsToProcess = this.rst.getSelectedRowCount()!=0
                                ? this.rst.getSelectedRowCount()
                                : ( this._table.getLoadedRowCount() != 0
                                    ? this._table.getLoadedRowCount() 
                                    : this._table.getRowCount() );
            int[] rows = this.rst.getSelectedRows();
            int viewRow = -1;
            int[] columns = this.rst.getSelectedColumns();
            int viewCol = -1;

            // Create a layer name from information gathered
            //
            String layerName = "";
            if ( !Strings.isEmpty(this.rst.getName()) ) {
              layerName = this.rst.getName();
            } else {
              layerName = this.rst.getDefaultExportName();
            }
            layerName = (Strings.isEmpty(layerName)?"":layerName+" ") +
                        mappableColumnName + 
                        (Strings.isEmpty(_labelMenuValue)|| _labelMenuValue.equalsIgnoreCase(cNoLabel) ? "" : "_" + _labelMenuValue) + 
                        ((this.rst.getSelectedRowCount()!=0||this.rst.getSelectedColumnCount()!=0)
                        ? " (Selection)" 
                        : " (RecordSet)"
                        );
            
            String                          rowID = "";
            String                     columnName = "";
            String                      classname = "";
            String                 structTypeName = "";
            STRUCT                         stGeom = null;
            JGeometry                       jGeom = null;  
            STRUCT              nonMappableStruct = null; 
            int                        FULL_GTYPE = 2001;
            int                              SRID = Constants.SRID_NULL;
            MetadataEntry                      me = null;
            SVGraphicLayer          sGraphicLayer = null;
            SpatialRenderer              renderer = SpatialRenderer.getInstance();
            Constants.GEOMETRY_TYPES geometryType = Constants.GEOMETRY_TYPES.UNKNOWN;
            LinkedHashMap<String, Object>    attr = new LinkedHashMap<String, Object>();

            for (int row=0; row < rowsToProcess; row++) 
            {
                stGeom = null; 
                jGeom = null;
                attr.clear();
                rowID = null;
                viewRow = row;
                if ( this.rst.getSelectedRowCount() > 0 ) {
                    viewRow   = rows[row];
                }
                for (int col = 0; col < colsToProcess; col++) 
                {
                    viewCol = col;
                    if ( this.rst.getSelectedColumnCount() > 0 ) {
                        viewCol   = columns[col];
                    }
                    classname  = this._table.getColumnClass(viewCol).getName();
                    columnName = this._table.getColumnName(viewCol);
                    try 
                    {
                        if (classname.equals("oracle.sql.STRUCT") ) 
                        {
                          if ( viewCol == mappableColumn ) 
                          {
                              stGeom = (STRUCT)this._table.getValueAt(viewRow,viewCol);
                              if ( stGeom == null ) {
                                  break; // Can't render a null geometry so skip whole record 
                              }
                              // Check if ST_Geometry ... extract SDO_
                              if ( stGeom.getSQLTypeName().indexOf("MDSYS.ST_")==0 ) {
                                  stGeom = SDO_GEOMETRY.getSdoFromST(stGeom);
                              } 
                              structTypeName = stGeom.getSQLTypeName();
                              if ( structTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ) {
                                  FULL_GTYPE = SDO_GEOMETRY.getFullGType(stGeom,FULL_GTYPE);
                                  SRID = SDO_GEOMETRY.getSRID(stGeom,SRID);
                                  geometryType = SDO_GEOMETRY.discoverGeometryType(FULL_GTYPE,geometryType);
                                  jGeom        = JGeometry.load(stGeom);
                              } else {
                                  if (row==0) {
                                      SRID         = mainPrefs.getSRIDAsInteger(); 
                                      geometryType = Constants.GEOMETRY_TYPES.POINT;                                  
                                  }
                                  if ( structTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) ) {
                                      Datum data[] = stGeom.getOracleAttributes();
                                      double x = ((NUMBER)data[0]).doubleValue();
                                      double y = ((NUMBER)data[1]).doubleValue();
                                      double z = (data[2] != null)
                                                  ? ((NUMBER)data[2]).doubleValue() 
                                                  : (data[3] != null) 
                                                    ? ((NUMBER)data[3]).doubleValue() 
                                                    : Double.NaN;
                                      double m = (data[2] != null && data[3] != null) ? ((NUMBER)data[3]).doubleValue() : Double.NaN;
                                      jGeom = (Double.isNaN(z) && Double.isNaN(m))
                                              ? new JGeometry(x,y,SRID)
                                              : (Double.isNaN(m)
                                                ? new JGeometry(x,y,z,SRID)
                                                : new JGeometry(x,y,z,m,SRID));
                                  } else if ( structTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) ) {
                                      double[] ords = SDO_GEOMETRY.asDoubleArray(stGeom,Double.NaN);
                                      jGeom = Double.isNaN(ords[2])
                                              ? new JGeometry(ords[0],ords[1],        SRID)
                                              : new JGeometry(ords[0],ords[1],ords[2],SRID);
                                  }
                              }
                              if ( jGeom == null ) {
                                  break;
                              }

                              if ( me == null )
                              {
                                  try {
                                      // Now we have a STRUCT we can get its SRID and create the layer
                                      me = new MetadataEntry(this.getUserName(),
                                                             layerName,
                                                             mappableColumnName,
                                                             (SRID==Constants.SRID_NULL || SRID==0)
                                                                 ? "NULL" 
                                                                 : String.valueOf(SRID));
                                      sGraphicLayer = new SVGraphicLayer(svp.getMostSuitableView(SRID),
                                                                         layerName,
                                                                         layerName,
                                                                         layerName,
                                                                         me,
                                                                         true);
                                      sGraphicLayer.setConnection(this.getConnectionName());
                                      sGraphicLayer.setSQL(sqlCmd);
                                      sGraphicLayer.getStyling().setPointSize(12);
                                      if ( this.mainPrefs.isRandomRendering()) {
                                          sGraphicLayer.getStyling().setAllRandom();
                                      }
                                  } catch (Exception e) {
                                      LOGGER.error("Error creating Graphic Theme to hold selection." + e.getMessage());
                                      return false;
                                  }
                              }
                          } else {
                            nonMappableStruct = (STRUCT)this._table.getValueAt(viewRow,viewCol);
                            if ( nonMappableStruct != null && 
                                 RenderTool.isSupported(nonMappableStruct) ) 
                            {
                                // Convert to current output text format
                                attr.put(columnName,
                                         (nonMappableStruct==null
                                          ? null
                                          : renderer.renderGeoObject(nonMappableStruct,true)));
                            }
                          }
                      } else if (classname.equalsIgnoreCase("oracle.sql.ROWID") ) {
                            rowID = SQLConversionTools.convertToString(conn,
                                                                       columnName,
                                                                       this._table.getValueAt(viewRow,viewCol));
                      } else {
                              if ( Tools.dataTypeIsSupported(classname) ) {
                                String value = SQLConversionTools.convertToString(conn,
                                                                                  columnName,
                                                                                  this._table.getValueAt(viewRow,viewCol));
                                // Object value = this._table.getValueAt(viewRow,viewCol);
                                attr.put(columnName, value==null ? null : value );
                              }
                      }
                  } catch (ClassCastException cce) {
                    LOGGER.error("RenderResultSet.processResultSet(): Cast Error converting column/type " + columnName + "/" + classname);                          
                  } catch (SQLException sqle) {
                    LOGGER.error("RenderResultSet.processResultSet(): SQL Error converting column/type " + columnName + "/" + classname);
                  } catch (Exception e) {
                    LOGGER.error("RenderResultSet.processResultSet(): Error converting column/type " + columnName + "/" + classname);                      
                  }
                }
                // rowid is always column 0 in the underlying tableModel
                if ( jGeom!=null )
                    try {
                        sGraphicLayer.add(new QueryRow(rowID,
                                                       (attr==null || attr.size()==0?null:new LinkedHashMap<String,Object>(attr)),
                                                       jGeom,
                                                       conn));
                    } catch (Exception e) {
                        LOGGER.warn("RenderResultSet.mapSelectedGeometries: QueryRow create error = " + e.getMessage());
                    }
            }
            // Map result
            if ( sGraphicLayer.getCacheCount() != 0 ) {
                // Need to calculate and set layerMBR()
                sGraphicLayer.setLayerMBR();
                // Set label field if not cNoLabel
                if ( ! (Strings.isEmpty(_labelMenuValue) || _labelMenuValue.equalsIgnoreCase(cNoLabel)) ) {
                    sGraphicLayer.getStyling().setLabelColumn(_labelMenuValue);
                }
                // We display the whole set via graphic theme 
                //
                   sGraphicLayer.setGeometryType(geometryType);
                   if ( svp.addLayerToView(sGraphicLayer,false/*zoom*/) ) {
                       if ( _commandId != COMMAND_VIEW_SELECTED_GEOMS) { 
                          svp.zoomFullButton(sGraphicLayer);
                       } else {
                           svp.redraw();
                       }
                }
            }

        } catch (IllegalArgumentException iae) {
          JOptionPane.showMessageDialog(null, iae.getMessage());
          return false;
        }
        return true;
    }

    private boolean mapSelectedDimInfos() 
    {
        try
        {
            if ( this.rst == null ) {
                throw new IllegalArgumentException("No ResultSet Table to process.");
            }
            String sqlCmd = this.rst.getCurrentSql();
            
            if ( this._table == null ) {
                throw new IllegalArgumentException("No SQL Developer Grid Table to process.");
            }
            
            OracleConnection conn = (OracleConnection)this.getConnection();
            if ( conn==null ) {
              throw new IllegalArgumentException("Could not access database connection of Query.");
            }

            SpatialViewPanel svp = DockableSV.getSpatialViewPanel();
            if ( svp == null ) {
                throw new IllegalArgumentException("Could not access GeoRaptor's Spatial View Panel.");
            }
            // MAKE SURE THE VIEW IS OPEN ...
            //
            makeVisible();
            
            String mappableColumnName = "";
            int        mappableColumn = -1;
            mappableColumn = this.findDimArrayColumn();
            if ( mappableColumn == -1 ) {
                 throw new IllegalArgumentException(this.propertyManager.getMsg("NO_DIMINFO"));
            } else {
                mappableColumn = this.clickColumn;
            }
            mappableColumnName =  this._table.getColumnName(mappableColumn);
            LOGGER.debug("mappable: " + mappableColumn + " name " + mappableColumnName);
            int colsToProcess = this.rst.getSelectedColumnCount()==0 ? this._table.getColumnCount() : this.rst.getSelectedColumnCount();
            // For rows, we have to include what rows are actually loaded into the JTable as we can only
            // convert those that are visible to a GraphicTheme otherwise conversion of the ones not in 
            // the table will be corrupted as data conversion of column data types will fail
            //
            int rowsToProcess = this.rst.getSelectedRowCount()!=0
                                ? this.rst.getSelectedRowCount()
                                : ( this._table.getLoadedRowCount() != 0
                                    ? this._table.getLoadedRowCount() 
                                    : this._table.getRowCount() );
            int[] rows = this.rst.getSelectedRows();
            int viewRow = -1;
            int[] columns = this.rst.getSelectedColumns();
            int viewCol = -1;

            // Create a layer name from information gathered
            //
            String layerName = "";
            if ( !Strings.isEmpty(this.rst.getName()) ) {
              layerName = this.rst.getName();
            } else {
              layerName = this.rst.getDefaultExportName();
            }
            layerName = mappableColumnName +
                        ((this.rst.getSelectedRowCount()!=0||this.rst.getSelectedColumnCount()!=0)
                        ? ": Selection" 
                        : ": full RecordSet" + (Strings.isEmpty(layerName)?"":" "+layerName));
            
            String                          rowID = "";
            String                     columnName = "";
            String                      classname = "";
            String                 structTypeName = "";
            ARRAY                         diminfo = null;
            JGeometry                       jGeom = null; 
            int                              SRID = Constants.SRID_NULL;
            MetadataEntry                      me = null;
            SVGraphicLayer          sGraphicLayer = null;
            Constants.GEOMETRY_TYPES geometryType = Constants.GEOMETRY_TYPES.UNKNOWN;
            LinkedHashMap<String, Object>    attr = new LinkedHashMap<String, Object>();

            for (int row=0; row < rowsToProcess; row++) 
            {
                jGeom = null;
                attr.clear();
                rowID = null;
                viewRow = row;
                if ( this.rst.getSelectedRowCount() > 0 ) {
                    viewRow   = rows[row];
                }
                for (int col = 0; col < colsToProcess; col++) 
                {
                    viewCol = col;
                    if ( this.rst.getSelectedColumnCount() > 0 ) {
                        viewCol   = columns[col];
                    }
                    classname  = this._table.getColumnClass(viewCol).getName();
                    columnName = this._table.getColumnName(viewCol);
                    LOGGER.debug("columnName= " + columnName + " className=" + classname);
                    try 
                    {
                        if (columnName.equalsIgnoreCase("SRID")) {
                            Object obj = this._table.getValueAt(viewRow,viewCol);
                            if ( obj instanceof oracle.sql.NUMBER ) {
                                NUMBER num = (NUMBER)obj;
                                SRID = num.intValue();
                            } else {
                                SRID =
                                    Strings.isEmpty(obj.toString()) 
                                        ? Constants.NULL_SRID 
                                        : Integer.valueOf(obj.toString()).intValue();
                            }
                            attr.put(columnName, SRID);
                        }  
                        if ( columnName.equalsIgnoreCase("DIMINFO") && 
                             classname.equals("oracle.sql.ARRAY") )
                        {
                            LOGGER.debug("viewCol == mappableCol - viewCol " + viewCol + " == " + mappableColumn);
                            diminfo = (ARRAY)this._table.getValueAt(viewRow,viewCol);
                            if ( diminfo.getSQLTypeName().equalsIgnoreCase(Constants.TAG_MDSYS_SDO_DIMARRAY) ) 
                            {
                                if ( viewCol == mappableColumn ) {
                                   if ( diminfo != null ) {
                                       jGeom = SDO_GEOMETRY.getGeometry(diminfo,SRID);
                                    }
                                } else {
                                    jGeom = null;
                                    me.add(diminfo);
                                    attr.put(columnName, me.toDimArray());
                                }
                            }
                            if ( jGeom == null ) { break; }
                            if ( me == null )
                            {
                                try 
                                {
                                    // Now we have a STRUCT we can get its SRID and create the layer
                                    me = new MetadataEntry(this.getUserName(),
                                                           layerName,
                                                           mappableColumnName,
                                                           (SRID==Constants.SRID_NULL || SRID==0)
                                                          ? "NULL" 
                                                           : String.valueOf(SRID));
                                    sGraphicLayer = new SVGraphicLayer(svp.getMostSuitableView(SRID),
                                                                       layerName,
                                                                       layerName,
                                                                       layerName,
                                                                       me,
                                                                       true);
                                    sGraphicLayer.setConnection(this.getConnectionName());
                                    sGraphicLayer.setSQL(sqlCmd);
                                    sGraphicLayer.getStyling().setPointSize(12);
                                    sGraphicLayer.setGeometryType(Constants.GEOMETRY_TYPES.POLYGON);
                                    if ( this.mainPrefs.isRandomRendering()) {
                                        sGraphicLayer.getStyling().setAllRandom();
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("Error creating Graphic Theme to hold selection." + e.getMessage());
                                    return false;
                                }
                            }
                        }
                        if (classname.equalsIgnoreCase("oracle.sql.ROWID") ) {
                            rowID = SQLConversionTools.convertToString(conn,columnName,this._table.getValueAt(viewRow,viewCol));
                        } else {
                            try 
                            {
                                if ( Tools.dataTypeIsSupported(classname) ) {
                                    String value = SQLConversionTools.convertToString(conn,
                                                                                      columnName,
                                                                                      this._table.getValueAt(viewRow,viewCol));
                                    // Object value = this._table.getValueAt(viewRow,viewCol);
                                    attr.put(columnName, value==null ? null : value );
                                }
                            } catch (Exception e) {
                                LOGGER.error("RenderResultSet.processResultSet(): Error converting " + columnName + " (" + e.getMessage() +")");
                            }
                        }
                  } catch (ClassCastException cce) {
                    LOGGER.error("RenderResultSet.processResultSet(): Cast Error converting column/type " + columnName + "/" + classname);                          
                  } catch (SQLException sqle) {
                    LOGGER.error("RenderResultSet.processResultSet(): SQL Error converting column/type " + columnName + "/" + classname);
                  } catch (Exception e) {
                    LOGGER.error("RenderResultSet.processResultSet(): Error converting column/type " + columnName + "/" + classname);                      
                  }
                }
                // rowid is always column 0 in the underlying tableModel
                if ( jGeom!=null )
                    try {
                        sGraphicLayer.add(new QueryRow(rowID,
                                                       (attr==null || attr.size()==0?null:new LinkedHashMap<String,Object>(attr)),
                                                       jGeom,
                                                       conn));
                    } catch (Exception e) {
                        LOGGER.warn("RenderResultSet.mapSelectedGeometries: QueryRow create error = " + e.getMessage());
                    }
            }
            // Map result
            if ( sGraphicLayer.getCacheCount() != 0 ) {
                // Need to calculate and set layerMBR()
                sGraphicLayer.setLayerMBR();
                // We display the whole set as graphic theme 
                //
                sGraphicLayer.setGeometryType(geometryType);
                if ( svp.addLayerToView(sGraphicLayer,true/*zoom*/) ) {
                    svp.zoomFullButton(sGraphicLayer);
                }
            }

        } catch (IllegalArgumentException iae) {
          JOptionPane.showMessageDialog(null, iae.getMessage());
          return false;
        }
        return true;
    }

    private STRUCT findStructValue(int _viewCol) 
    {
        // Get non-null value for STRUCT so we can check its actual database type name (ie SDO_GEOMETRY not just STRUCT
        //
        STRUCT geo = null;
        if ( this.rst == null )
          return null;
        
        int rowsToProcess = rst.getSelectedRowCount()!=0
                          ? rst.getSelectedRowCount()
                          : ( _table.getLoadedRowCount() != 0
                              ? _table.getLoadedRowCount() 
                              : _table.getRowCount() );
        int[] rows = rst.getSelectedRows();
        for (int row=0, viewRow=0; row < rowsToProcess; row++)
        {
          viewRow = row;
          if ( rst.getSelectedRowCount() > 0 ) {
              viewRow   = rows[row];
          }
          if (  this._table.getValueAt(0,_viewCol)!=null ) {
              geo = ((STRUCT)this._table.getValueAt(viewRow,_viewCol));
              break;
          }
        }
        return geo;
    }

    private ARRAY findDimInfoValue(int _viewCol) 
    {
        if ( this.rst == null ) {
            return null;
        }
        // Get non-null value for ARRAY so we can check its actual database type name (ie DIMINFO not just ARRAY)
        //
        ARRAY diminfo = null;
        int rowsToProcess = this.rst.getSelectedRowCount()!=0
                          ? this.rst.getSelectedRowCount()
                          : ( _table.getLoadedRowCount() != 0
                              ? _table.getLoadedRowCount() 
                              : _table.getRowCount() );
        int[] rows = this.rst.getSelectedRows();
        for (int row=0, viewRow=0; row < rowsToProcess; row++)
        {
            viewRow = row;
            if ( this.rst.getSelectedRowCount() > 0 ) {
                viewRow   = rows[row];
                viewRow = this.rst.convertRowIndexToModel(viewRow);
            }
            if ( this._table.getValueAt(0,_viewCol)!=null ) {
                diminfo = ((ARRAY)this._table.getValueAt(viewRow,_viewCol));
                break;
            }
        }
        return diminfo;
    }
    
    /**
     * getConnectionName
     * gets connection name associated with the SQL
     * @note: getConnectionName() does not return display name, rather returns something like this "IdeConnections%23codesys%40GIS11R2"
     * @return
     */
    private String getConnectionName() {
        return this.rst.getGridModel().getConnectionName();
    }

    /**
     * @method getConnection
     * @return Connection
     * @method @method
     * @author Simon Greener, October 18th 2010
     * @description Gets the connection this record set was created using.
     */
    private Connection getConnection() {
        if ( this.rst == null ) {
            return null;
        }
        Connection localConnection = null;
        String connName = this.getConnectionName();
        if (!Strings.isEmpty(connName)) {
            // Try going straight to SQL Developer connections
            try {
                localConnection = Connections.getInstance().getConnection(connName, false);
                if (localConnection!=null) {
                    // Ensure added to databaseConnections
                    DatabaseConnections.getInstance().addConnection(connName);
                }
            } catch (DBException e) {
                LOGGER.error("RenderResultSet.getConnection(): Exception - " + e.getMessage());
            }
        }
        return (localConnection==null)
               ? DatabaseConnections.getInstance().getActiveConnection()
               : localConnection;
    }
    
    public boolean update(IdeAction ideAction, Context context) 
    {
        int i = 0;
        if (this._table != null)
        {
            Connection localConnection = this.getConnection();
            if ((localConnection != null) && (Connections.getInstance().isOracle(localConnection))) {
                i = 1;
            }
            if ((i == 0) || (this._table.isVerticalDisplay()))
                ideAction.setEnabled(false);
            else
                ideAction.setEnabled(true);
        }
        return true;
    }

  // use inner class to do work in a thread that ProgressBar can use
  class runExport implements Runnable 
  {
      private   Connection                                          conn = null;
      private   ResultSetTable                                       rst = null;
      private   int                                       mappableColumn = -1;
      private   String                                mappableColumnName = "";
      protected ProgressBar                                  progressBar = null;
      protected String                                      errorMessage = "";
      private   Constants.EXPORT_TYPE                         exportType = Constants.EXPORT_TYPE.GML;
      private   String                                  attributeFlavour = "";
      private   LinkedHashMap<Integer,RowSetMetaData>     exportMetadata = null;
      private   String                                      characterSet = "UTF-8";
      private   ExporterDialog                                   options = null;
      private   int                                         rowsExported = 0;
      private   GeometryProperties                      geometryMetadata = null;
  
      public runExport(Connection                            _conn,
                       ResultSetTable                        _rst,
                       int                                   _mappableColumn,
                       String                                _mappableColumnName,
                       ExporterDialog                        _options,
                       Constants.EXPORT_TYPE                 _exportType,
                       String                                _AttributeFlavour,
                       String                                _characterSet,
                       LinkedHashMap<Integer,RowSetMetaData> _resultMeta,
                       GeometryProperties                    _geometryMetadata) 
      {
          this.conn = _conn;
          if ( this.conn==null ) {
              this.conn = getConnection();
              if ( this.conn==null ) {
                 throw new IllegalArgumentException("No valid database connection for export.");
              }
          }
          this.rst                = _rst;
          this.mappableColumn     = _mappableColumn;
          this.options            = _options;
          this.exportType         = _exportType;
          this.attributeFlavour   = _AttributeFlavour;
          this.characterSet       = _characterSet;
          this.exportMetadata     = _resultMeta;
          this.geometryMetadata   = _geometryMetadata;
          this.mappableColumnName = _mappableColumnName;
      }
      
      public void setProgressBar(ProgressBar _progressBar) {
          this.progressBar = _progressBar;
      }

      public String getErrorMessage() {
          return this.errorMessage;
      }      

      public int getRowCount() {
        return this.rowsExported;
      }
      
      private LinkedHashMap<Integer,Integer> skippedRecords = new LinkedHashMap<Integer,Integer>(Constants.SHAPE_TYPE.values().length);

      public LinkedHashMap<Integer,Integer> getSkipStatistics() {
          return new LinkedHashMap<Integer,Integer>(this.skippedRecords);
      }
  
      private void setSkipStatistics(ShapeType _sType,
                                     boolean   _hasArc) {
          if ( _sType == null ) {
              return;
          }
          
          if ( skippedRecords == null ) {
              this.skippedRecords = new LinkedHashMap<Integer,Integer>(Constants.SHAPE_TYPE.values().length);
          }
        
          int shapeTypeId = _sType.id;
          if ( _sType.id!=ShapeType.UNDEFINED.id || _sType.id < -1) {
               shapeTypeId = _hasArc ? _sType.id * -1 : _sType.id;
               if ( _sType.hasMeasure() )
                   shapeTypeId = _sType.id * -100;
          }

          if ( skippedRecords.size()==0) {
              skippedRecords.put(shapeTypeId,1);
              return;
          }
          
          Integer recordCount = skippedRecords.get(shapeTypeId);
          if (recordCount==null) {
              recordCount = new Integer(1);
          } else {
              recordCount++;
          }
          
          // Create new or update existing
          skippedRecords.put(shapeTypeId,recordCount);
        
      }    

      public void run () 
      {
          int colsToProcess = this.rst.getSelectedColumnCount()==0?_table.getColumnCount():this.rst.getSelectedColumnCount();
          // For rows, we have to include what rows are actually loaded into the JTable as we can only
          // convert those that are visible to a GraphicTheme otherwise conversion of the ones not in 
          // the table will be corrupted as data conversion of column data types will fail
          //
          int rowsToProcess =   this.rst.getSelectedRowCount()!=0
                              ? this.rst.getSelectedRowCount()
                              : ( _table.getLoadedRowCount() != 0
                                  ? _table.getLoadedRowCount() 
                                  : _table.getRowCount() );
          int[]        rows = this.rst.getSelectedRows();
          int       viewRow = -1;
          int[]     columns = this.rst.getSelectedColumns();
          int       viewCol = -1;
          String columnName = "";

          IExporter geoExporter = null;
          try 
          {
              switch ( this.exportType ) {
                case GML : geoExporter = new GMLExporter(this.conn,this.options.getFilename(),rowsToProcess);
                           if ( this.options.hasAttributes() ) {
                               writeXSD(this.options.getEntityName(),
                                        this.options.getAttributeFlavour(),
                                        this.options.getFilename().replace("."+this.exportType.toString().toLowerCase(),".xsd"),
                                        this.characterSet,
                                        this.mappableColumn);
                           } 
                           break;
                case KML : geoExporter = new KMLExporter(this.conn,this.options.getFilename(),rowsToProcess); break;
                case SHP : geoExporter = new SHPExporter(this.conn,this.options.getFilename(),rowsToProcess); break;
                case TAB : geoExporter = new TABExporter(this.conn,this.options.getFilename(),rowsToProcess); break;
              }
              geoExporter.setBaseName(this.options.getEntityName());
              geoExporter.setGenerateIdentifier(! this.options.hasAttributes() );
              geoExporter.setAttributeFlavour(this.options.getAttributeFlavour());
              geoExporter.setCommit(this.options.getCommit());
              geoExporter.setGeoColumnIndex(this.mappableColumn);
              geoExporter.setGeoColumnName(this.mappableColumnName);
              geoExporter.setGeometryProperties(this.geometryMetadata);
              geoExporter.setExportMetadata(this.exportMetadata);
        
              geoExporter.start(this.characterSet);
              STRUCT stGeom = null;
              for (int row=0; row < rowsToProcess; row++)
              {
                  viewRow = row;
                  if ( rst.getSelectedRowCount() > 0 ) {
                      viewRow   = rows[row];
                  }
                  
                  // Check if row's sdo_geometry is null
                  // or is of right type.
                  // If null or wrong type, skip row
                  //
                  stGeom = (STRUCT)_table.getValueAt(viewRow,mappableColumn);
                  if ( stGeom==null ) {
                      if ( this.options.isSkipNullGeometry() ) { 
                          setSkipStatistics(ShapeType.NULL,false);
                      }
                      continue;
                  }
                  // Need to test sdo_geometry type is same as output geometry type only for SHP and TAB
                  // Or has unsupported arcs
                  ShapeType shpType =
                        SDO_GEOMETRY.getShapeType(SDO_GEOMETRY.getFullGType(stGeom,2000),
                                           ((ShapeType)this.geometryMetadata.getShapefileType(true)).hasMeasure());
                  
                  if ( (this.exportType==Constants.EXPORT_TYPE.SHP || 
                        this.exportType==Constants.EXPORT_TYPE.TAB) &&
                        ((ShapeType)this.geometryMetadata.getShapefileType(true)).equals(shpType)==false ||
                        SDO_GEOMETRY.hasArc(stGeom) 
                      || 
                       ( (this.exportType==Constants.EXPORT_TYPE.GML  || 
                          this.exportType==Constants.EXPORT_TYPE.KML) && SDO_GEOMETRY.hasMeasure(stGeom) 
                       ) )
                  {
                      setSkipStatistics(shpType, SDO_GEOMETRY.hasArc(stGeom));
                      continue;
                  }
                  // Now process whole row
                  //
                  geoExporter.startRow();
                  for (int col = 0; col < colsToProcess; col++) 
                  {
                      viewCol = col;
                      if ( rst.getSelectedColumnCount() > 0 ) {
                          viewCol = columns[col];
                      }
                      columnName     = _table.getColumnName(viewCol).replace("\"","");
                      OraRowSetMetaDataImpl columnMetadata = (OraRowSetMetaDataImpl)this.exportMetadata.get(viewCol+1);
                      try 
                      {
                          if (viewCol == this.mappableColumn ) 
                          {
                              JGeometry jGeo = null;
                              stGeom = (STRUCT)_table.getValueAt(viewRow,viewCol);
                              if (stGeom == null ) {
                                  if ( ! options.isSkipNullGeometry() ) {
                                      // Must want to write NULL geometry\
                                      geoExporter.printColumn(stGeom,  columnMetadata);
                                      continue;
                                  }
                                  break;
                              }
                              String saveStructTypeName = columnMetadata.getColumnTypeName(1);
                              // Check if ST_Geometry ... extract SDO_
                              if ( saveStructTypeName.indexOf("MDSYS.ST_")==0 ) {
                                  stGeom = SDO_GEOMETRY.getSdoFromST(stGeom);
                                  saveStructTypeName = stGeom.getSQLTypeName();
                              } 
                              if ( saveStructTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ) {
                                  // We need to replace SRID if GML/KML and original was NULL
                                  //
                                  if ( this.exportType==Constants.EXPORT_TYPE.GML || this.exportType==Constants.EXPORT_TYPE.KML ) {
                                      int SRID = SDO_GEOMETRY.getSRID(stGeom, Constants.SRID_NULL);
                                      if ( (SRID==Constants.SRID_NULL && this.geometryMetadata.getSRID()!=Constants.SRID_NULL) ||
                                           (SRID!=Constants.SRID_NULL && this.geometryMetadata.getSRID()!=Constants.SRID_NULL && SRID!=this.geometryMetadata.getSRID()) ) {
                                          stGeom = SDO_GEOMETRY.setSRID(stGeom,this.options.getSRID());
                                      }
                                  }
                              } else if ( saveStructTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) ) {
                                  columnMetadata.setColumnTypeName(1,Constants.TAG_MDSYS_SDO_GEOMETRY);
                                  if (stGeom == null ) {
                                      // Must want to write NULL geometry
                                      geoExporter.printColumn(stGeom, columnMetadata);
                                      continue;
                                  }
                                  Datum data[] = stGeom.getOracleAttributes();
                                  double x = ((NUMBER)data[0]).doubleValue();
                                  double y = ((NUMBER)data[1]).doubleValue();
                                  double z = (data[2] != null)
                                              ? ((NUMBER)data[2]).doubleValue() 
                                              : (data[3] != null) 
                                                ? ((NUMBER)data[3]).doubleValue() 
                                                : Double.NaN;
                                  double m = (data[2] != null && data[3] != null) ? ((NUMBER)data[3]).doubleValue() : Double.NaN;
                                  jGeo = (Double.isNaN(z) && Double.isNaN(m))
                                         ? new JGeometry(x,y,this.options.getSRID())
                                         : Double.isNaN(m)
                                           ? new JGeometry(x,y,z,this.options.getSRID())
                                           : new JGeometry(x,y,z,m,this.options.getSRID());
                                  stGeom = JGeometry.store(jGeo, this.conn);                                                                
                              } else if ( saveStructTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) ) {
                                  columnMetadata.setColumnTypeName(1,Constants.TAG_MDSYS_SDO_GEOMETRY);
                                  if (stGeom == null ) {
                                      // Must want to write NULL geometry
                                      geoExporter.printColumn(stGeom, columnMetadata);
                                      continue;
                                  }
                                  double[] ords = SDO_GEOMETRY.asDoubleArray(stGeom,Double.NaN);
                                  jGeo = Double.isNaN(ords[2])
                                         ? new JGeometry(ords[0],ords[1],        this.options.getSRID())
                                         : new JGeometry(ords[0],ords[1],ords[2],this.options.getSRID());
                                  stGeom = JGeometry.store(jGeo, this.conn);
                              }
                              geoExporter.printColumn(stGeom, columnMetadata);
                              // May have been changed so swap it back for processing of next row
                              //
                              columnMetadata.setColumnTypeName(1,saveStructTypeName);
                              continue;
                          } 
                          
                          // TOBEDONE For shapefiles must export at least a row id
                          //
                          if ( this.options.hasAttributes() == false ) {
                              continue;
                          }

                          if ( Tools.isSupportedType(columnMetadata.getColumnType(1),columnMetadata.getColumnTypeName(1)) ) {
                              geoExporter.printColumn(SQLConversionTools.convertToString((OracleConnection)this.conn,
                                                                                         _table.getValueAt(viewRow,viewCol),
                                                                                         columnMetadata),
                                                      columnMetadata);
                          } else {
                              if ( row == 1 ) {
                                  LOGGER.warn("runExport.run: Output of column " + columnMetadata.getColumnName(1) + " of type " + columnMetadata.getColumnTypeName(1) + " is not supported");
                              }
                          }
                      } catch (Exception e) {
                          LOGGER.warn("runExport.run: Error converting column/type " + columnName + "/" + columnMetadata.getColumnType(1));                      
                      }
                  }
                  // Flush converted data to disk
                  geoExporter.endRow();
                  if ( (options.getCommit()==1) ||
                        geoExporter.getRowCount() % options.getCommit() == 0 ) {
                      this.progressBar.sleepForUIToRepaint();
                      if ( this.progressBar.hasUserCancelled() ) {
                          geoExporter.end();
                          geoExporter.close();
                          geoExporter = null;
                          this.progressBar.setDoneStatus();
                          return;
                      }
                      // Update progress
                      String stepText = String.format("Processed %s of %s",
                                               String.valueOf(geoExporter.getRowCount()),
                                               String.valueOf(rowsToProcess));
                      this.progressBar.updateProgress(Math.round(geoExporter.getRowCount() / rowsToProcess * 100),
                                                      "Generating " + exportType.toString() + " " + this.options.getFilename(),
                                                      stepText);
                  }
              }  // for
          } catch (Exception e) {
              this.errorMessage = "Exception caught when exporting " + this.exportType.toString() + "\n" + e.getMessage();
          } finally {
              try {
                  this.rowsExported = geoExporter.getRowCount();
                  geoExporter.end();
                  geoExporter.close();
                  geoExporter = null;
              } catch (IOException e) {
              }
              this.progressBar.setDoneStatus();
          }
      }
      
      public void showExportStats(String _processingTime)
      {
          // Show Export Statistics
          //
          int exportedRecordCount = getRowCount();
          LinkedHashMap<Integer,Integer> skipStats = getSkipStatistics();
          StringBuffer sb = new StringBuffer(1000);
          String writtenTypes = "";
          if ( (this.exportType==Constants.EXPORT_TYPE.SHP || 
                this.exportType==Constants.EXPORT_TYPE.TAB) ) {
              writtenTypes = ((ShapeType)this.geometryMetadata.getShapefileType(true)).toString();
          } else { 
              writtenTypes = this.geometryMetadata.getGeometryType().toString();
          }
          sb.append(propertyManager.getMsg("RESULTS_TOP", 
                                           options.getEntityName(),
                                           String.format("%d (%s)",
                                                         exportedRecordCount,
                                                         writtenTypes),
                                           _processingTime));
          
          if ( skipStats!=null && skipStats.size()!=0 ) {
              sb.append(propertyManager.getMsg("RESULTS_TABLE_HEADER"));
              String sShapeType = "";
              int shapeTypeId = -1;
              int shapeTypeCount = -1;
              int shapeTotalCount = 0;
              Iterator<Integer> iter = skipStats.keySet().iterator();
              while ( iter.hasNext() ) {
                  shapeTypeId = iter.next();
                  shapeTypeCount = skipStats.get(shapeTypeId);
                  shapeTotalCount += shapeTypeCount;
                  sShapeType = "";
                  if ( shapeTypeId!=ShapeType.UNDEFINED.id ) {
                      if ( shapeTypeId < -100 ) {
                          shapeTypeId = Math.abs(shapeTypeId);
                          sShapeType = " (Measures)";
                      } else if ( shapeTypeId > -100 && shapeTypeId < -1) {
                          shapeTypeId = Math.abs(shapeTypeId);
                          sShapeType = " (CIRCULAR ARC)";
                      }
                  }
                  ShapeType sType = ShapeType.forID(shapeTypeId);
                  sb.append(propertyManager.getMsg("RESULTS_TABLE_REPEATABLE_ROW",sType.toString() + sShapeType,shapeTypeCount));              
              }
              sb.append(propertyManager.getMsg("RESULTS_TABLE_TOTAL_ROW",shapeTotalCount));
          }
          sb.append(propertyManager.getMsg("RESULTS_END"));
          JOptionPane.showMessageDialog(null, new JLabel(sb.toString()));              
      }

  }
    
}
