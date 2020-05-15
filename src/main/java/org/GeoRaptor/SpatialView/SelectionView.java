package org.GeoRaptor.SpatialView;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import oracle.jdbc.OracleConnection;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.SpatialRenderer;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.ViewOperationListener;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;


/**
 * @author Simon Greener, April 27th 2010
 *          Changed getMsg handling due to change to separate PropertiesManager class
 * @author Simon Greener, May 9th 2010
 *          Modified to make work on unselected coordinates AND refresh immediately all points created
 *          Note: This does not use any of the code in getDrawTools() when it should do.
 **/
@SuppressWarnings("deprecation")
public class SelectionView 
extends JPanel
{
	private static final long serialVersionUID = 3264723010361605090L;

	protected SpatialViewPanel svPanel;

    /** 
     * Properties File Manager
     **/
    private static final String propertiesFile = "org.GeoRaptor.SpatialView.resource.SpatialViewPanel";
    protected PropertiesManager propertyManager;
    /**
     * Get reference to single instance of GeoRaptor's Preferences
     */
    protected Preferences GeoRaptorPrefs;

    /**
     * List of attribute data
     */
    protected List<QueryRow>    qList;

    protected String            ERROR_NO_SELECTION   = "No row(s) selected.";

    private JTabbedPane         mainTabPanel     = new JTabbedPane();
    private BorderLayout        mainBorderLayout = new BorderLayout();
    
    private JPanel              attribPanel = new JPanel();
    private BorderLayout        attribBorderLayout = new BorderLayout();
    private JScrollPane         attribTableScrollPane = new JScrollPane();
    private JTable              attribTable = new JTable();
    protected AttribTableModel  attTableModel;
    
    private JPanel              geoPanel = new JPanel();
    private BorderLayout        geoPanelBorderLayout = new BorderLayout();    
    private JScrollPane         geometriesScrollPane = new JScrollPane();
    private JTable              geoTable = new JTable();
    protected GeoTableModel     geoTableModel;
    
    private JPopupMenu          geoPopupMenu = new JPopupMenu();

    public SelectionView(SpatialViewPanel _svPanel) 
    {
        // Get localisation file
        //
        this.propertyManager = new PropertiesManager(SelectionView.propertiesFile);
        this.GeoRaptorPrefs  = MainSettings.getInstance().getPreferences();
        this.svPanel = _svPanel;

        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.ERROR_NO_SELECTION = this.propertyManager.getMsg("ERROR_NO_SELECTION");

        this.attTableModel = new AttribTableModel(this);
        this.attribTable.setModel(this.attTableModel);
        this.attribTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.attribTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        this.geoTableModel = new GeoTableModel();
        this.geoTable.setModel(this.geoTableModel);
        this.geoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.geoTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.geoTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                svPanel.setMessage(null,false);
                geoTableMouseClicked(evt);
            }
        });
        // Make two tables share same selection model
        this.geoTable.setSelectionModel(this.attribTable.getSelectionModel());
    } 

    private void jbInit() throws Exception {
        attribPanel.setLayout(attribBorderLayout);
        attribTableScrollPane.getViewport().add(attribTable, null);
        attribPanel.add(attribTableScrollPane, BorderLayout.CENTER);
        
        geoPanel.setLayout(geoPanelBorderLayout);
        geometriesScrollPane.getViewport().add(geoTable, null);
        geoPanel.add(geometriesScrollPane, BorderLayout.CENTER);

        // Create the right mouse click menus for geoTable
        //
        createMenu();
        
        mainTabPanel.addTab(this.svPanel.propertyManager.getMsg("ATTRIBUT_PANEL_TAB_A"),attribPanel);
        mainTabPanel.addTab(this.svPanel.propertyManager.getMsg("ATTRIBUT_PANEL_TAB_G"),geoPanel);
        this.setLayout(mainBorderLayout);
        this.setSize(new Dimension(609, 300));
        this.add(mainTabPanel, BorderLayout.CENTER);
    } // jbInit

    private void createMenu() {

        AbstractAction zoomSelected = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_ZOOM_GEOMETRIES")) 
        {
				private static final long serialVersionUID = 6418449028007614502L;

				public void actionPerformed(ActionEvent e) {
                  displaySelected(true,false);
                }
            };
        geoPopupMenu.add(zoomSelected);

        AbstractAction highlightSelected = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_HIGHLIGHT_GEOMETRIES")) 
        {
				private static final long serialVersionUID = 174444977368688321L;

				public void actionPerformed(ActionEvent e) {
                      displaySelected(false,true);
                  }
              };
        geoPopupMenu.add(highlightSelected);
        
        AbstractAction drawSelected = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_DRAW_GEOMETRIES")) 
        {
				private static final long serialVersionUID = -5395349238709568399L;

				public void actionPerformed(ActionEvent e) {
                    displaySelected(false,false);
                }
        };
        geoPopupMenu.add(drawSelected);

        AbstractAction showGeometry = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_SHOW_GEOMETRIES_IN_WINDOW")) 
        {
			private static final long serialVersionUID = -6162949615882502260L;

			public void actionPerformed(ActionEvent e) {
              showSelected();
            }
        };
        geoPopupMenu.add(showGeometry);

        AbstractAction copyClipboard = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_COPY_CLIPBOARD")) 
        {
				private static final long serialVersionUID = -112514286570293673L;

				public void actionPerformed(ActionEvent e) {
                  copyToClipboard();
                }
        };
        geoPopupMenu.add(copyClipboard);

        AbstractAction measureShape = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_MEASURE")) 
        {
			private static final long serialVersionUID = -2862497001290611104L;

			public void actionPerformed(ActionEvent e) {
              measureShape();
            }
        };
        geoPopupMenu.add(measureShape);

        AbstractAction countVertices = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_COUNT_VERTICES")) 
        {
        		private static final long serialVersionUID = -7155322955806208433L;

				public void actionPerformed(ActionEvent e) {
                  showCount();
                }
            };
        geoPopupMenu.add(countVertices);
        
        JMenu markVerticesSubMenu = new JMenu(this.propertyManager.getMsg("GEOMETRY_MENU_MARK_VERTICES"));
        AbstractAction markVerticesId = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_MARK_VERTICES_ID")) 
        {
				private static final long serialVersionUID = 3401760711649975473L;

				public void actionPerformed(ActionEvent e) {
                  markVertices(Constants.VERTEX_LABEL_TYPE.ID);
                }
            };
        markVerticesSubMenu.add(markVerticesId);
        AbstractAction markVerticesIdCoord = 
            new AbstractAction(this.propertyManager.getMsg("GEOMETRY_MENU_MARK_VERTICES_ID_COORD")) 
        {
        	private static final long serialVersionUID = 3520601789707311488L;

			public void actionPerformed(ActionEvent e) {
              markVertices(Constants.VERTEX_LABEL_TYPE.ID_COORD);
            }
        };
        markVerticesSubMenu.add(markVerticesIdCoord);
        geoPopupMenu.add(markVerticesSubMenu);

    }
    
    private void showCount()
    {
        // Feature %s, Dimensions %d, Number of Vertices = %d\n
        String formatString = this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_COUNT_VERTICES_RESULT"),
               displayString = "";
        int rows[] = geoTable.getSelectedRows();
        String columnName = attTableModel.getColumnName(0)==null?"NO ATTRIBUTES":attTableModel.getColumnName(0);
        if ( rows.length == 0 ) {
            // Process one underneath mouse click?
            Point i = geoPopupMenu.getLocation();
            int row = this.geoTable.rowAtPoint(i);
            STRUCT geomStruct = (STRUCT)geoTableModel.getValueAt(row,GeoTableModel.GeoValue);
            displayString = String.format(formatString,
                                          columnName,
                                          (columnName.equalsIgnoreCase("NO ATTRIBUTES")?"":attTableModel.getValueAt(row,0)),
                              SDO_GEOMETRY.getDimension(geomStruct,2) /* Dimensions */,
                              SDO_GEOMETRY.getNumberCoordinates(geomStruct));
        } else if ( rows.length == 1 ) {
            STRUCT geomStruct = (STRUCT)geoTableModel.getValueAt(rows[0],GeoTableModel.GeoValue);
            displayString = String.format(formatString,
                                          columnName,
                                          (columnName.equalsIgnoreCase("NO ATTRIBUTES")?"":attTableModel.getValueAt(rows[0],0)),
                              SDO_GEOMETRY.getDimension(geomStruct,2) /* Dimensions */,
                              SDO_GEOMETRY.getNumberCoordinates(geomStruct));
        } else if ( rows.length > 1 ) {
            for (int i=0; i<rows.length; i++) {
                QueryRow qr = (QueryRow)geoTableModel.getRow(i);
                STRUCT geomStruct = qr.getGeoValue();
                displayString = String.format(formatString,
                                              attTableModel.getValueAt(i,0)==null?""+i:attTableModel.getValueAt(i,0)  /* Feature */,
                                  SDO_GEOMETRY.getDimension(geomStruct,2) /* Dimensions */,
                                  SDO_GEOMETRY.getNumberCoordinates(geomStruct));
            }
        }
        // Display result
        //
        Object[] options = {this.svPanel.propertyManager.getMsg("GEOMETRY_RESULTS_COPY_TO_CLIPBOARD"),
                            this.svPanel.propertyManager.getMsg("CLOSE_BUTTON")};
        
        int n = JOptionPane.showOptionDialog(null,
                                             new JLabel(displayString),
                                             MainSettings.EXTENSION_NAME +  " - SELECT",
                                             JOptionPane.YES_NO_CANCEL_OPTION,
                                             JOptionPane.QUESTION_MESSAGE,
                                             null,
                                             options,
                                             options[1]);
        if (n == 0) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection ss = new StringSelection(displayString);
            clipboard.setContents(ss, ss);
        }
        
    }

    
    private void markVertices(Constants.VERTEX_LABEL_TYPE _markType)
    {
          int rows[] = geoTable.getSelectedRows();
          Constants.VERTEX_LABEL_TYPE slt = this.svPanel.activeView.getActiveLayer().getStyling().getMarkVertex(); 
          this.svPanel.activeView.getActiveLayer().getStyling().setMarkVertex(_markType);
          if ( rows.length == 0 ) {
              // Process all or process one?
            Point i = geoPopupMenu.getLocation();
            int row = this.geoTable.rowAtPoint(i);
            this.svPanel.showGeometry(null,
                                      (STRUCT)geoTableModel.getValueAt(row,GeoTableModel.GeoValue), null,
                                      SDO_GEOMETRY.getGeoMBR((STRUCT)geoTableModel.getValueAt(row,GeoTableModel.GeoValue)),
                                      false,false,false);
          } else if ( rows.length == 1 ) {
              this.svPanel.showGeometry(null,
                                        (STRUCT)geoTableModel.getValueAt(rows[0],GeoTableModel.GeoValue),null,
                                      SDO_GEOMETRY.getGeoMBR((STRUCT)geoTableModel.getValueAt(rows[0],GeoTableModel.GeoValue)),
                                        false,false,false);
          } else if ( rows.length > 1 ) {
              List<QueryRow> geoSet = new ArrayList<QueryRow>(); 
              for (int i=0; i<rows.length; i++) {
                  geoSet.add((QueryRow)geoTableModel.getRow(i));
              }
              this.svPanel.showGeometry(null,null,geoSet,new Envelope(geoSet),false,false,false);
          }
          this.svPanel.activeView.getActiveLayer().getStyling().setMarkVertex(slt);
          this.svPanel.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
    }
    
    private void copyToClipboard() 
    {
        STRUCT geoStruct = null;
        StringBuffer sBuffer = new StringBuffer();
      
        int rows[] = geoTable.getSelectedRows();
        if ( rows.length == 0 ) {
            // Process one if exists?
            Point i = geoPopupMenu.getLocation();
            int row = this.geoTable.rowAtPoint(i);
            if ( row == -1 )
                sBuffer.append("");
            else {
                geoStruct = (STRUCT)geoTableModel.getValueAt(row,GeoTableModel.GeoValue);
                sBuffer.append((geoStruct==null
                           ? "" : SDO_GEOMETRY.convertGeometryForClipboard(geoStruct)));
            }
        } else if ( rows.length >= 1 ) {
            int viewRow = -1;
            for (int row=0; row<rows.length; row++) {
                viewRow = row;
                if ( this.geoTable.getSelectedRowCount() != 0 ) {
                    viewRow = rows[row];
                }
                geoStruct = (STRUCT)geoTableModel.getValueAt(viewRow,GeoTableModel.GeoValue);
                sBuffer.append("\n" +
                               (geoStruct==null
                               ? "" : SDO_GEOMETRY.convertGeometryForClipboard(geoStruct)));
            }
        }
        if ( sBuffer.length() > 0 ) {
          Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
          StringSelection ss = new StringSelection(sBuffer.toString());
          clipboard.setContents(ss, ss);
        }
    }

    @SuppressWarnings("unused")
	private void measureShape()
    {
        STRUCT geoStruct = null;
        
        int[] rows = this.geoTable.getSelectedRows();
        int viewRow = -1;
        int rowsToProcess = 0; 
        if ( rows.length == 0 ) {
            // Process all or process one?
            Point i = geoPopupMenu.getLocation();
            int row = this.geoTable.rowAtPoint(i);
            if ( row != -1 ) {
                rows = new int[]{row};
                rowsToProcess = 1;
            }
        } else {
            rowsToProcess = this.geoTable.getSelectedRowCount()!=0
                ? this.geoTable.getSelectedRowCount()
                : this.geoTable.getRowCount() ;
        }

        double[] dMeasures;
        int AREA = 0;
        int LENGTH = 1;
        double lineLength = 0.0f;
        int numberPoints = 0;
        int numberMultiPoints = 0;
        int numberLines = 0;
        int numberPolygons = 0;
        double polygonArea = 0.0f;
        double polygonBoundary = 0.0f;
        int numberCollections = 0;
        int numberColnLines=0;
        int numberColnPolygons=0;
        double colnArea = 0.0f;
        double colnLength = 0.0f;
        for ( int row=0; row < rowsToProcess; row++ ) 
        {
            viewRow = row;
            if ( this.geoTable.getSelectedRowCount() != 0 ) {
              viewRow = rows[row];
            } 
            JGeometry geom = null; 
            geom = (JGeometry)geoTableModel.getValueAt(viewRow,GeoTableModel.JGeometry);
            if ( geom == null )
                continue;
            switch ( geom.getType() ) {        
              case JGeometry.GTYPE_COLLECTION :
                    numberCollections++;
                    dMeasures = Tools.computeAreaLength(geom,
                                                        Constants.MEASURE.BOTH,
                                                        svPanel.getActiveView());
                    colnArea += dMeasures[AREA];
                    if ( dMeasures[AREA] != 0.0f ) numberColnPolygons++;
                    colnLength += dMeasures[LENGTH];
                    if ( dMeasures[LENGTH] != 0.0f ) numberColnLines++;
                    break;
              case JGeometry.GTYPE_POLYGON:
              case JGeometry.GTYPE_MULTIPOLYGON:
                    numberPolygons++;
                    dMeasures = Tools.computeAreaLength(geom,
                                                        Constants.MEASURE.BOTH,
                                                        svPanel.getActiveView());
                    polygonArea     += dMeasures[AREA];
                    polygonBoundary += dMeasures[LENGTH];
                    break;
              case JGeometry.GTYPE_MULTICURVE:
              case JGeometry.GTYPE_CURVE:
                    numberLines++;
                    lineLength += Tools.computeAreaLength(geom,
                                                             Constants.MEASURE.LENGTH,
                                                             svPanel.getActiveView())[LENGTH];
                    break;
              case JGeometry.GTYPE_POINT :
                  numberPoints++;
                  break;
              case JGeometry.GTYPE_MULTIPOINT :
                  numberMultiPoints++;
                  break;
              default:
                  break;
            }
        } // for
        
        // Let user know
        //
        String distanceUnit = Strings.TitleCase(Tools.getViewUnits(svPanel.getActiveView(),Constants.MEASURE.LENGTH).replace("_"," "));
        String areaUnit     = Strings.TitleCase(Tools.getViewUnits(svPanel.getActiveView(),Constants.MEASURE.AREA).replace("_"," "));
        
        String featureCount = (numberPoints==0     ?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_HTML_POINTS").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                     numberPoints)) +
                              (numberMultiPoints==0?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_HTML_MULTIPOINTS").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                     numberMultiPoints)) +
                              (numberLines==0      ?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_HTML_LINES").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                     numberLines,
                                                                     /* Total   */lineLength,                                   distanceUnit,
                                                                     /* Average */numberLines==0?0.0f:(lineLength/numberLines), distanceUnit )) +
                              (numberPolygons==0   ?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_HTML_POLYGONS").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                     numberPolygons,
                                                                     /* Total   */polygonArea,                                            areaUnit,
                                                                     /* Average */numberPolygons==0?0.0f:(polygonArea/numberPolygons),    areaUnit,
                                                                     /* Total   */polygonBoundary,                                        distanceUnit,
                                                                     /* Average */numberPolygons==0?0.0f:(polygonBoundary/numberPolygons),distanceUnit )) +
                              (numberCollections==0?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_HTML_COLLECTIONS").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                     numberCollections,
                                                                     /* Total   */colnLength,                                              distanceUnit,
                                                                     /* Average */colnLength==0?0.0f:(colnLength/numberColnLines),         distanceUnit,
                                                                     /* Total   */colnArea,                                                areaUnit,
                                                                     /* Average */numberColnPolygons==0?0.0f:(colnArea/numberColnPolygons),areaUnit ));
        String finalResult = String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_HTML"),featureCount);
        
        Object[] options = {this.svPanel.propertyManager.getMsg("GEOMETRY_RESULTS_COPY_TO_CLIPBOARD"),
                            this.svPanel.propertyManager.getMsg("CLOSE_BUTTON")};
        
        int n = JOptionPane.showOptionDialog(null,
                                             new JLabel(finalResult),
                                             MainSettings.EXTENSION_NAME +  " - SELECT",
                                             JOptionPane.YES_NO_CANCEL_OPTION,
                                             JOptionPane.QUESTION_MESSAGE,
                                             null,
                                             options,
                                             options[1]);
        if (n == 0) {
            // Get without HTML
            finalResult = (numberPoints==0     ?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_POINTS").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                 numberPoints)) +
                          (numberMultiPoints==0?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_MULTIPOINTS").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                 numberMultiPoints)) +
                          (numberLines==0      ?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_LINES").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                 numberLines,
                                                                 /* Total   */lineLength,                                   distanceUnit,
                                                                 /* Average */numberLines==0?0.0f:(lineLength/numberLines), distanceUnit )) +
                          (numberPolygons==0   ?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_POLYGONS").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                 numberPolygons,
                                                                 /* Total   */polygonArea,                                            areaUnit,
                                                                 /* Average */numberPolygons==0?0.0f:(polygonArea/numberPolygons),    areaUnit,
                                                                 /* Total   */polygonBoundary,                                        distanceUnit,
                                                                 /* Average */numberPolygons==0?0.0f:(polygonBoundary/numberPolygons),distanceUnit )) +
                          (numberCollections==0?"":String.format(this.svPanel.propertyManager.getMsg("GEOMETRY_MENU_MEASURE_RESULT_COLLECTIONS").replace("%f","%." + svPanel.getActiveView().getPrecision(false)+"f"),
                                                                 numberCollections,
                                                                 /* Total   */colnLength,                                              distanceUnit,
                                                                 /* Average */colnLength==0?0.0f:(colnLength/numberColnLines),         distanceUnit,
                                                                 /* Total   */colnArea,                                                areaUnit,
                                                                 /* Average */numberColnPolygons==0?0.0f:(colnArea/numberColnPolygons),areaUnit ));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection ss = new StringSelection(finalResult);
            clipboard.setContents(ss, ss);
        }
    }
    
    private void displaySelected(boolean _zoom,
                                 boolean _highlight)
    {
        int rows[] = geoTable.getSelectedRows();
        if ( rows.length == 0 ) {
            // Process all or process one?
          Point i = geoPopupMenu.getLocation();
          int row = this.geoTable.rowAtPoint(i);
          if ( row != -1 ) 
              this.svPanel.showGeometry(null,
                                        (STRUCT)geoTableModel.getValueAt(row,GeoTableModel.GeoValue),null,
                                          SDO_GEOMETRY.getGeoMBR((STRUCT)geoTableModel.getValueAt(row,GeoTableModel.GeoValue)),
                                        _highlight,_zoom,false);
        } else if ( rows.length == 1 ) {
            this.svPanel.showGeometry(null,
                                      (STRUCT)geoTableModel.getValueAt(rows[0],GeoTableModel.GeoValue),null,
                                      SDO_GEOMETRY.getGeoMBR((STRUCT)geoTableModel.getValueAt(rows[0],GeoTableModel.GeoValue)),
                                      _highlight,_zoom,false);
        } else if ( rows.length > 1 ) {
            List<QueryRow> geoSet = new ArrayList<QueryRow>();
            for (int i=0; i<rows.length; i++) {
                geoSet.add((QueryRow)geoTableModel.getRow(rows[i]));
            }
            this.svPanel.showGeometry(null,null,geoSet,new Envelope(geoSet),_highlight,_zoom,false);
        }
    }

    private void showSelected()
    {
        int rows[] = geoTable.getSelectedRows();
        int row = -1;        
        if ( rows.length == 0 ) {
            // Process all or process one?
          Point i = geoPopupMenu.getLocation();
          row = this.geoTable.rowAtPoint(i);
          if ( row == -1 ) 
              row = rows[0];
        } 
        if ( rows.length == 1 ) {
            row = rows[0];
        } 

        SpatialRenderer sr = SpatialRenderer.getInstance();
        JLabel lblGeom = null;
        int modelRow = -1;
        Dimension imageSize = new Dimension(GeoRaptorPrefs.getPreviewImageWidth(),
                                            GeoRaptorPrefs.getPreviewImageHeight());
        if (row != -1 ) {
            modelRow = geoTable.convertRowIndexToModel(row);
            lblGeom = sr.getPreview((STRUCT)geoTableModel.getValueAt(modelRow,GeoTableModel.GeoValue), 
                                      imageSize);
        } else if ( rows.length > 1 ) {
            List<JGeometry> geoSet = new ArrayList<JGeometry>(rows.length);
            for (int i=0; i<rows.length; i++) {
                modelRow = geoTable.convertRowIndexToModel(rows[i]);
                geoSet.add((JGeometry)geoTableModel.getValueAt(modelRow,GeoTableModel.JGeometry));
            }
            lblGeom = sr.getPreview((JGeometry)null, geoSet, imageSize);
        }
        JFrame frame = new JFrame();
        frame.add(lblGeom);
        frame.setTitle(Constants.GEORAPTOR);
        frame.setLocationRelativeTo(svPanel);
        frame.pack();
        frame.setVisible(true);
    }

    private void geoTableMouseClicked(java.awt.event.MouseEvent evt) 
    {
        if ( SwingUtilities.isRightMouseButton(evt) ) 
        {
            // Is there anything under the mouse? 
            //
            if ( this.geoTable.getSelectedRows().length == 0 ) 
            {
                int row = this.geoTable.rowAtPoint(evt.getPoint());
                if (row == -1) {
                    svPanel.setMessage(ERROR_NO_SELECTION,true);
                    return;
                } 
            }
            // Show popup menu
            geoPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }
    
    /**
     * Show data in geometry and attribute panel
     * @param _qList
     * @author Simon Greener, May 7th 2010
     *          Fixed bug caused by hardcoding of "ORD_ARRAY" search string.
     *          Moved to use of Tokenizer rather than substringing.... 
     *          Code seems to show only one geometry in the array.
     */
    public void showData(List<QueryRow> _qList) 
    {
        if ((_qList == null) || (_qList.size() == 0)) {
            return;
        }
        this.qList = _qList;
        // set attribute data
        this.attTableModel.refreshAttRecord();
        // set geometry data
        this.geoTableModel.setData(this.qList);
        Tools.autoResizeColWidth(this.attribTable,null);
        Tools.autoResizeColWidth(this.geoTable,null);
        if ( this.attTableModel.getColumnCount()==0)
          mainTabPanel.setSelectedComponent(geoPanel); 
    }
  
    public void clear() {
        this.attTableModel.clearModel();
        this.geoTableModel.clearModel();
        // Shouldn't need this but...
        this.repaint();
    }
    
    @SuppressWarnings("unused")
	private JGeometry convertGeometry(STRUCT _stGeom)
    {
        if ( _stGeom == null )
            return null;
        JGeometry geom;
        try {
            geom = JGeometry.load(_stGeom);
            if ( geom == null )
                return null;
            double[] mbr = geom.getMBR();
            if ( mbr == null || mbr.length == 0 )
                return null;
            // Must be OK
            return geom;
        } catch (SQLException e) {
            return null;
        }
    }

    public List<QueryRow> getQList() {
        return this.qList;
    }

} // class SelectionView


class AttribTableModel extends DefaultTableModel 
{
	private static final long serialVersionUID = -1838904546118522615L;
	
	protected String columnsNames[] = { "No Columns" };
    protected String geoColumnName;

    /**
     * Reference to main panel
     */
    protected SelectionView adv;

    /**
     * Last table structure hash value
     */
    protected int lastTableHash = -99;

    /**
     * Cache Object for getValueAt function
     */
    protected QueryRow lastQueryRow;

    /**
     * Last read row with getValueAt function
     */
    protected int lastQueryRowIndex;

    public AttribTableModel(SelectionView _adv) {
        this.adv = _adv;
    }

    public void refreshAttRecord() {
        // get table structure
        if ( this.adv.getQList()!=null ) 
        {
            QueryRow firstRow = this.adv.getQList().get(0);
            int rowHash = firstRow.calcColumnsHash();
            if (this.lastTableHash != rowHash) // table structure has changed 
            { 
                // create columns list new table structure
                this.columnsNames = new String[firstRow.getColumnCount()];
                if ( firstRow.getAttData() != null ) {
                  Iterator<String> it = firstRow.getAttData().keySet().iterator();
                  int arrayIndex = 0;
                  while (it.hasNext()) {
                      this.columnsNames[arrayIndex] = (String)it.next();
                      arrayIndex++;
                  }
                }
                this.fireTableStructureChanged();
                this.lastTableHash = rowHash;
            }
          // mark table data change
          this.fireTableDataChanged();
        } else {
          this.fireTableStructureChanged();
        }
        // set default values for cache data
        this.lastQueryRowIndex = -1;
        this.lastQueryRow = null;
    } // refreshAttRecord

    public int getColumnCount() {
        if ((this.adv == null) || (this.adv.getQList() == null)) {
            return 0;
        }
        return columnsNames.length;
    } // getColumnCount

    public String getColumnName(int column) {
        if ( this.columnsNames.length!=0 )
            return this.columnsNames[column];
        else
            return null;
    } // getColumnName

    public int getRowCount() {
        if ((this.adv == null) || (this.adv.getQList() == null)) {
            return 0;
        }
        return this.adv.getQList().size();
    }

    public Object getValueAt(int row, int column) {
        if ( (this.lastQueryRowIndex==row && this.lastQueryRow!=null)==false) {
            this.lastQueryRowIndex = row;
            this.lastQueryRow = this.adv.getQList().get(row);
        }
            Object value = this.lastQueryRow.getValueForColumnIndex(column);
            if ( value==null ) {
                return null;
            } else if ( value instanceof String ) {
                return (String)value;
            } else  {
                return SQLConversionTools.convertToString((OracleConnection)this.adv.svPanel.activeView.getConnection(),
                                                          this.getColumnName(column), 
                                                          value);
            }
    }

    public void clearModel() 
    {
        if ( this.adv.getQList() != null && this.adv.getQList().size() > 0 ) {
            int numRows = this.adv.getQList().size()-1;
            this.adv.getQList().clear();
            this.fireTableRowsDeleted(0,numRows);
        }
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
} // AttribTableModel

class GeoTableModel extends DefaultTableModel 
{
	private static final long serialVersionUID = -5043384942084833042L;
	
	protected final static int      GeoRender = 0;
    protected final static int       GeoValue = 1;
    protected final static int GeoConstructor = 2;
    protected final static int      JGeometry = 3;
    
    protected List<QueryRow> cache;
    
    public GeoTableModel() {
    }

    public Object getRow(int _row) {
        return this.cache==null?null:this.cache.get(_row);
    }
    
    public int getRowCount() {
        return (this.cache==null)?0:this.cache.size();
    }
    
    public Object getValueAt(int row, int col) 
    {
        if ( this.cache == null )
            return null;
        if ( row < this.cache.size() )  {
            QueryRow qr = this.cache.get(row);
            switch ( col ) {
              case 0 : return qr.getGeoRender();
              case 1 : return qr.getGeoValue();
              case 2 : return qr.getGeoConstructor();
              case 3 : return qr.getJGeom(); 
            }
      }
      return null;
   }

    Class<?>[] types = new Class[] { String.class };

    public Class<?> getColumnClass(int columnIndex) {
        return this.types[columnIndex];  // should always be 0
    }

    protected String[] headers = { "Geometry" };

    public String getColumnName(int _col) {
        return this.headers[_col];
    }

    public String[] getColumnNames() {
        return this.headers;
    }
  
    boolean[] canEdit = new boolean[] { false };
  
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit[columnIndex];
    }

    public void setData(List<QueryRow> _geoData) 
    {
        this.cache = _geoData;
        this.fireTableRowsInserted(0,cache==null?0:cache.size()); // notify everyone that we have a new table.
    }

    public void clearModel() 
    {
        if ( this.cache != null && this.cache.size() > 0 ) {
            int numRows = this.cache.size()-1;
            this.cache.clear();
            this.fireTableRowsDeleted(0,numRows);
        }
    }
   
    public int getColumnCount() {
        return 1;
    } // getColumnCount
    
} // GeoTableModel
