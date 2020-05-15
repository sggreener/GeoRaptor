package org.GeoRaptor.SpatialView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import java.sql.SQLException;

import java.text.DecimalFormat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import oracle.jdbc.OracleConnection;

import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.ControlerSV;
import org.GeoRaptor.SpatialView.SupportClasses.MyImageSelection;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.ViewOperationListener;
import org.GeoRaptor.SpatialView.layers.SVGraphicLayer;
import org.GeoRaptor.SpatialView.layers.SVQueryLayer;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayer;
import org.GeoRaptor.SpatialView.layers.SVWorksheetLayer;
import org.GeoRaptor.SpatialView.layers.Styling;
import org.GeoRaptor.io.ExtensionFileFilter;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * @author Simon Greener, April 11th
 **/

@SuppressWarnings("deprecation")
public class SpatialViewPanel 
extends JPanel 
{

	private static final long serialVersionUID = -1243756863674418808L;

	private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.SpatialView.SpatialViewPanel");

    @SuppressWarnings("unused")
	private ClassLoader cl = null;

    /** 
     * Handle to Preferences File 
     **/
    Preferences SVPanelPreferences;
    
    /** 
     * Properties File Manager
     **/
    private static final String propertiesFile = "org.GeoRaptor.SpatialView.resource.SpatialViewPanel";
    protected PropertiesManager propertyManager;
    private XYLayout xyMainToolbarLayout = new XYLayout();

    public static enum LayerReturnCode {Metadata,MBR,Fail,Success};  
    
    /**
     * User interface 
     */
    protected SelectionView         attDataView;
    protected ViewOperationListener voListener;

    /**
     * When loading from XML we don't want the maps to be refreshed changing their MBRs
     * 
     */
    protected boolean loading = false;
    
    /** 
     * The Active View
     **/
    protected SpatialView activeView;

    /**
     * A views and layers in GeoRaptor manipulated via single JTree
     */
    protected ViewLayerTree viewLayerTree;

    /**
     * Dialog for image center selection
     */
    protected SelectWindowCenter swCenter;

    protected int mainSplitPercentage = -1;
    protected int mainLayerSplitPercentage = -1;
    
    /**
     * Set this variable to "true" when user select cancel operation. All long time duration operations should check this flag.
     */
    protected boolean cancelOperation;
    
    /**
     * Decimal format conversion
     */
    private final String defaultCoordText   = "(? ?)";
    private final String defaultCoordFormat = "(%s %s)";
    private final String mousePosnCoordText = "(x y)";
    private final String defaultLLCoordText = "LL(x y)";
    private final String defaultURCoordText = "UR(x y)";

    private String    SHOW_GEOMETRY_PARAMETER_ERROR = "Show Geometries function called with no geometries to process.";
    private String SHOW_GEOMETRYSET_PARAMETER_ERROR = "Empty geometry set provided to Show Geometries function. called with no geometries to process";
    private String    STATUS_RECTANGLE_CIRCLE_START = null;
    private JLabel                         lblScale = new JLabel();

    public SpatialViewPanel() 
    {
        super();
        try 
        {
            this.cl = this.getClass().getClassLoader();

            // Get handle to preferences
            //
            this.SVPanelPreferences = MainSettings.getInstance().getPreferences();
            
            try {
                // Get localisation file
                //
                this.propertyManager = new PropertiesManager(SpatialViewPanel.propertiesFile);
            } catch (Exception e) {
                System.out.println("Problem loading properties file: " + SpatialViewPanel.propertiesFile + "\n" + e.getMessage());
            }
            
            this.SHOW_GEOMETRY_PARAMETER_ERROR    = this.propertyManager.getMsg("SHOW_GEOMETRY_PARAMETER_ERROR");
            this.SHOW_GEOMETRYSET_PARAMETER_ERROR = this.propertyManager.getMsg("SHOW_GEOMETRYSET_PARAMETER_ERROR");
            this.STATUS_RECTANGLE_CIRCLE_START    = this.propertyManager.getMsg("STATUS_RECTANGLE_CIRCLE_START");

            // default view for query data
            this.attDataView = new SelectionView(this);
            
            // list of layers
            // In access-ordered linked hash maps, merely querying a map with the get method 
            // is a structural modification that can potentially throw ConcurrentModificationException.
            // So, layerList must be declared as insertion-ordered
            //
    
            // Set user interface operation
            this.voListener = new ViewOperationListener(this);

            // Icons for use in tool bar etc must be loaded
            //
            this.loadIcons();
          
            // Create JTree that holds view/layer hierarchy
            //
            this.viewLayerTree = new ViewLayerTree(this);

            jbInit();
            
            setIconsAndText();
            
            int splitPos = SVPanelPreferences.getMainLayerSplitPos();
            if ( splitPos < 100 ) {
                // convert to pixels as cannot set percentage when window not visible
                this.mainLayerSplitPercentage = splitPos == 0 ? 10 : splitPos;
                splitPos = getMainLayerSplitPercentageAsPixels();
            } else {
                calcMainLayerSplitPercentage();
            }
            mainLayerSplit.setDividerLocation(splitPos + mainLayerSplit.getInsets().left);
            
            splitPos = SVPanelPreferences.getMainSplitPos();
            if ( splitPos < 100 ) {
                // convert to pixels as cannot set percentage when window not visible
                this.mainSplitPercentage = splitPos == 0 ? 10 : splitPos;
                splitPos = getMainSplitPercentageAsPixels();
            } else {
                calcMainSplitPercentage();
            }
            
            mainSplit.setDividerLocation( splitPos + mainSplit.getInsets().bottom );
            this.addComponentListener(new ComponentAdapter() {
              public void componentResized(ComponentEvent e) {
                  if ( getWidth() != 0 && getHeight() != 0 && isVisible()) {
                      mainLayerSplit.setDividerLocation( getMainLayerSplitPercentageAsPixels() + mainLayerSplit.getInsets().left );
                      mainSplit.setDividerLocation(getMainSplitPercentageAsPixels() + mainSplit.getInsets().bottom);
                  }
              }
            });

            // Ensure Prev/Next MBR buttons are enabled/disabled
            this.setToolbarStatus();
            // progress console is on false at startup
            setProgressBar(false, null, -1, -1);
      
        } catch (NullPointerException npe) {
            System.out.println("Caught Null Pointer Exception in SpatialViewPanel()");
            npe.printStackTrace();
        } catch (Exception e) {
            System.out.println("Caught Exception in SpatialViewPanel() - jbInit()");
        }
        this.viewLayerTree.expandAll();
    }

    /** 
     * Icons
     */
    private ImageIcon mapMousePosition;
    private ImageIcon mapWorkareaLL;
    private ImageIcon mapWorkareaUR;
    private ImageIcon operationProgressEnable;
  
    private ImageIcon iconQueryNormal;
    private ImageIcon iconQueryPress;
    private ImageIcon iconZoomBoundsNormal;
    private ImageIcon iconZoomBoundsPress;
    private ImageIcon iconZoomInNormal;
    private ImageIcon iconZoomInPress;
    private ImageIcon iconZoomOutNormal;
    private ImageIcon iconZoomOutPress;
    private ImageIcon iconZoomFullNormal;
    private ImageIcon iconZoomFullPress;
    private ImageIcon iconZoomLayerNormal;
    private ImageIcon iconZoomLayerPress;
    private ImageIcon iconMoveNormal;
    private ImageIcon iconMovePress;
    private ImageIcon iconReloadNormal;
    private ImageIcon iconReloadPress;
    private ImageIcon iconCopyImageNormal;
    private ImageIcon iconCopyImagePress;
    private ImageIcon iconPrevMBRNormal;
    private ImageIcon iconPrevMBRPress;
    private ImageIcon iconNextMBRNormal;
    private ImageIcon iconNextMBRPress;
    
    private ImageIcon iconMeasureInactiveNormal;
    private ImageIcon iconMeasureDistanceNormal;
    private ImageIcon iconMeasurePolygonNormal;
    private ImageIcon iconMeasureCircleNormal;
    private ImageIcon iconMeasureRectangleNormal;
    
    // Buttons for geometry creation
    private ImageIcon iconCreateShapeInactive; 
    private ImageIcon iconCreatePointNormal;
    private ImageIcon iconCreateMultiPointNormal;
    private ImageIcon iconCreateLineNormal;
    private ImageIcon iconCreatePolygonNormal;
    private ImageIcon iconCreateCircleNormal;
    private ImageIcon iconCreateRectangleNormal;
    private ImageIcon iconGeoRaptorAbout;

    private void loadIcons() 
    {
        ClassLoader cl = this.getClass().getClassLoader();
        
        this.mapMousePosition = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/map_mouse_position.png"));
        this.mapWorkareaLL = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/map_workarea_ll.png"));
        this.mapWorkareaUR = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/map_workarea_ur.png"));
        this.operationProgressEnable = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/operation_progress_enable.png"));
  
        this.iconQueryNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_query_2.png"));
        this.iconQueryPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_query_3.png"));
        this.iconZoomBoundsNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoombounds_2.png"));
        this.iconZoomBoundsPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoombounds_3.png"));
        this.iconZoomInNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoomin_2.png"));
        this.iconZoomInPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoomin_3.png"));
        this.iconZoomOutNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoomout_2.png"));
        this.iconZoomOutPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoomout_3.png"));
        this.iconZoomFullNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoomfull_2.png"));
        this.iconZoomFullPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoomfull_3.png"));
        this.iconZoomLayerNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoom_layer_n.png"));
        this.iconZoomLayerPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_zoom_layer_p.png"));
        this.iconMoveNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_move_2.png"));
        this.iconMovePress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_move_3.png"));
        this.iconReloadNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_data_reload_2.png"));
        this.iconReloadPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_data_reload_3.png"));
        this.iconCopyImageNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_copy_image_2.png"));
        this.iconCopyImagePress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_copy_image_3.png"));
        this.iconPrevMBRNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_prev_mbr_n.png"));
        this.iconPrevMBRPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_prev_mbr_p.png"));
        this.iconNextMBRNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_next_mbr_n.png"));
        this.iconNextMBRPress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_next_mbr_p.png"));
        
        this.iconMeasureInactiveNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/measure_inactive_n.png"));
        this.iconMeasureDistanceNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_measure_distance_n.png"));
        //this.iconMeasureDistancePress  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/button_measure_distance_p.png"));
        this.iconMeasurePolygonNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/measure_polygon_n.png"));
        this.iconMeasureCircleNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/measure_circle_n.png"));
        this.iconMeasureRectangleNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/measure_rectangle_n.png"));
        
        // Buttons for geometry creation
        this.iconCreateShapeInactive    = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/operation_progress_disable.png")); 
        this.iconCreatePointNormal      = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/create_point_n.png"));
        this.iconCreateMultiPointNormal = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/create_multipoint_n.png"));
        this.iconCreateLineNormal       = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/create_line_n.png"));
        this.iconCreatePolygonNormal    = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/create_polygon_n.png"));
        this.iconCreateCircleNormal     = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/create_circle_n.png"));
        this.iconCreateRectangleNormal  = new ImageIcon(cl.getResource("org/GeoRaptor/SpatialView/images/create_rectangle_n.png"));
        
        this.iconGeoRaptorAbout         = new ImageIcon(cl.getResource("org/GeoRaptor/images/GeoRaptorLogoIcon.jpg"));
    }

    /** 
     * Swing Widgets
     */
    private JWindow viewWindow            = new JWindow();
  
    private BorderLayout borderLayoutMain = new BorderLayout();
    private JPanel pnlMainToolbar         = new JPanel(new BorderLayout(10,0));
    private JToolBar mainToolbar          = new JToolBar();
    private JSplitPane mainSplit          = new JSplitPane();
    private JButton btnQueryByPoint       = new JButton();
    private JButton btnPrevMBR            = new JButton();
    private JButton btnNextMBR            = new JButton();
    private JButton zoomInButton          = new JButton();
    private JButton zoomOutButton         = new JButton();
    private JButton zoomBoundsButton      = new JButton();
    private JButton zoomFullButton        = new JButton();
    private JButton zoomLayerButton       = new JButton();
    private JButton moveButton            = new JButton();
    private JButton reloadButton          = new JButton();
    private JButton copyImageButton       = new JButton();
    
    private JComboBox<String> measureShapeCB      = null;
    private JComboBox<String> createShapeCB = null;
    
    private JButton btnGeoRaptorAbout     = new JButton();
  
    private JLabel lblMeasureInactive     = new JLabel();
    private JLabel lblMeasureLine         = new JLabel();
    private JLabel lblMeasurePolygon      = new JLabel();
    private JLabel lblMeasureCircle       = new JLabel();
    private JLabel lblMeasureRectangle    = new JLabel();
  
    private JLabel lblCreateShapeInactive = new JLabel(); 
    private JLabel lblCreatePoint         = new JLabel();
    private JLabel lblCreateMultiPoint    = new JLabel();
    private JLabel lblCreateLine          = new JLabel();
    private JLabel lblCreatePolygon       = new JLabel();
    private JLabel lblCreateCircle        = new JLabel();
    private JLabel lblCreateRectangle     = new JLabel();
    
    private JSplitPane mainLayerSplit     = new JSplitPane();
    private JScrollPane treeView          = new JScrollPane();

    private JPanel statusPanel            = new JPanel();
    private JPanel LayerTreePanel         = new JPanel();
    private JPanel progressBarPanel       = new JPanel();
    private BorderLayout borderLayout2    = new BorderLayout();
    
    private BorderLayout borderLayout3    = new BorderLayout();
    private FlowLayout flowLayout1        = new FlowLayout();
    protected JLabel lblMessages          = new JLabel();
    private JLabel mousePosIcon           = new JLabel();
    private JLabel mousePosLabel          = new JLabel();
    private JLabel worldUpperRightIcon    = new JLabel();
    private JLabel worldLowerLeftIcon     = new JLabel();
    private JProgressBar loadDataPB       = new JProgressBar();
    private JButton oprCancelButton       = new JButton();

    private void jbInit() 
     throws Exception 
    {
        this.setLayout(this.borderLayoutMain);
        this.setSize(new Dimension(900, 300));
        this.setMinimumSize(new Dimension(750,650));
        this.borderLayoutMain.minimumLayoutSize(this);
        this.borderLayoutMain.maximumLayoutSize(this);

        viewWindow.setLayout(new BorderLayout());
        viewWindow.setAlwaysOnTop(true);
        viewWindow.setPreferredSize(new Dimension(100, 20));
        viewWindow.setSize(new Dimension(150, 20));
        viewWindow.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

        btnQueryByPoint.setIcon(iconQueryNormal);
        btnQueryByPoint.setToolTipText(this.propertyManager.getMsg("TOOLBAR_QUERY"));
        btnQueryByPoint.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        btnQueryByPoint();
                    }
                });
        btnPrevMBR.setIcon(iconPrevMBRNormal);
        btnPrevMBR.setToolTipText(this.propertyManager.getMsg("TOOLBAR_PREV_MBR"));
        btnPrevMBR.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        btnPrevMBR();
                    }
                });
        btnNextMBR.setIcon(iconNextMBRNormal);
        btnNextMBR.setToolTipText(this.propertyManager.getMsg("TOOLBAR_NEXT_MBR"));
        btnNextMBR.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        btnNextMBR();
                    }
                });
        zoomInButton.setIcon(iconZoomInNormal);
        zoomInButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOMIN"));
        zoomInButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        zoomInButton();
                    }
                });
        zoomOutButton.setIcon(iconZoomOutNormal);
        zoomOutButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOMOUT"));
        zoomOutButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        zoomOutButton();
                    }
                });
        zoomBoundsButton.setIcon(iconZoomBoundsNormal);
        zoomBoundsButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOMBOUNDS"));
        zoomBoundsButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        zoomBoundsButton();
                    }
                });
        zoomFullButton.setIcon(iconZoomFullNormal);
        zoomFullButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOMFULL"));
        zoomFullButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        zoomFullButton(null);
                    }
                });
        zoomLayerButton.setIcon(iconZoomLayerNormal);
        zoomLayerButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOM_LAYER"));
        zoomLayerButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if ( activeView != null ) {
                          SVSpatialLayer sLayer = activeView.getActiveLayer();
                          if ( sLayer != null ) {
                              zoomFullButton(sLayer);
                          } else {
                              setMessage(propertyManager.getMsg("STATUS_NO_ACTIVE_LAYER",activeView.getVisibleName()),true);
                          }
                        } else {
                            setMessage(propertyManager.getMsg("STATUS_NO_ACTIVE_VIEW"),true);
                        }
                    }
                });

        moveButton.setIcon(iconMoveNormal);
        moveButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_MOVE"));
        moveButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        moveButton();
                    }
                });
        reloadButton.setIcon(iconReloadNormal);
        reloadButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_RELOAD"));
        reloadButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        redraw();
                    }
                });
        copyImageButton.setIcon(iconCopyImageNormal);
        copyImageButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_COPY_IMAGE"));
        copyImageButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        copyImageButton();
                    }
                });

        lblMeasureInactive.setText(this.propertyManager.getMsg("LABEL_MEASURE_INACTIVE"));
        lblMeasureLine.setText(this.propertyManager.getMsg("LABEL_LINE"));
        lblMeasurePolygon.setText(this.propertyManager.getMsg("LABEL_POLYGON"));
        lblMeasureCircle.setText(this.propertyManager.getMsg("LABEL_CIRCLE"));
        lblMeasureRectangle.setText(this.propertyManager.getMsg("LABEL_RECTANGLE"));

        lblMeasureInactive.setIcon(iconMeasureInactiveNormal);
        lblMeasureLine.setIcon(iconMeasureDistanceNormal);
        lblMeasurePolygon.setIcon(iconMeasurePolygonNormal);
        lblMeasureCircle.setIcon(iconMeasureCircleNormal);
        lblMeasureRectangle.setIcon(iconMeasureRectangleNormal);
        
        measureShapeCB = new JComboBox<String>(
                              new DefaultComboBoxModel<String>(
                                  new String[] { 
                                      lblMeasureInactive.getText(),  /* Default */
                                      lblMeasureLine.getText(),
                                      lblMeasurePolygon.getText(),
                                      lblMeasureCircle.getText(),
                                      lblMeasureRectangle.getText()
                                  }
                              )
                          );
        measureShapeCB.setSize(new Dimension(100,22));
        measureShapeCB.setToolTipText(this.propertyManager.getMsg("TT_MEASURE_SHAPE"));
        measureShapeCB.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                String s = measureShapeCB.getSelectedItem().toString();                
                if ( s.equals(lblMeasureInactive.getText()) )
                  return;
                else if ( s.equals(lblMeasureLine.getText()) )
                  createMeasureCombo(ViewOperationListener.VIEW_OPERATION.MEASURE_DISTANCE);
                else if ( s.equals(lblMeasurePolygon.getText()) )
                  createMeasureCombo(ViewOperationListener.VIEW_OPERATION.MEASURE_POLYGON);
                else if ( s.equals(lblMeasureCircle.getText()) )
                  createMeasureCombo(ViewOperationListener.VIEW_OPERATION.MEASURE_CIRCLE);
                else if ( s.equals(lblMeasureRectangle.getText()) )
                  createMeasureCombo(ViewOperationListener.VIEW_OPERATION.MEASURE_RECTANGLE);
              }
          });
        measureShapeCB.setRenderer(new MeasureListRenderer());
        
        //measureShapeCB
            
        lblCreateShapeInactive.setText(this.propertyManager.getMsg("LABEL_CREATE_INACTIVE")); 
        lblCreatePoint.setText(this.propertyManager.getMsg("LABEL_POINT"));
        lblCreateMultiPoint.setText(this.propertyManager.getMsg("LABEL_MULTIPOINT"));
        lblCreateLine.setText(this.propertyManager.getMsg("LABEL_LINE"));
        lblCreatePolygon.setText(this.propertyManager.getMsg("LABEL_POLYGON"));
        lblCreateCircle.setText(this.propertyManager.getMsg("LABEL_CIRCLE"));
        lblCreateRectangle.setText(this.propertyManager.getMsg("LABEL_RECTANGLE"));

        lblCreateShapeInactive.setIcon(iconCreateShapeInactive); 
        lblCreatePoint.setIcon(iconCreatePointNormal);
        lblCreateMultiPoint.setIcon(iconCreateMultiPointNormal);
        lblCreateLine.setIcon(iconCreateLineNormal);
        lblCreatePolygon.setIcon(iconCreatePolygonNormal);
        lblCreateCircle.setIcon(iconCreateCircleNormal);
        lblCreateRectangle.setIcon(iconCreateRectangleNormal);

        createShapeCB = new JComboBox<String>(
                                new DefaultComboBoxModel<String>(
                                    new String[] { 
                                        lblCreateShapeInactive.getText(),  /* Default */
                                        lblCreatePoint.getText(),
                                        lblCreateMultiPoint.getText(),
                                        lblCreateLine.getText(),
                                        lblCreatePolygon.getText(),
                                        lblCreateCircle.getText(),
                                        lblCreateRectangle.getText()
                                    }
                                )
                            );
        
        createShapeCB.setSize(new Dimension(100,22));
        createShapeCB.setToolTipText(this.propertyManager.getMsg("TT_CREATE_SHAPE"));
        createShapeCB.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  if ( createShapeCB.isEnabled() )   // We programmatically set this for use when spatial querying 
                  {
                      String s = createShapeCB.getSelectedItem().toString();                
                      if ( s.equals(lblCreateShapeInactive.getText()) )
                        return;
                      else if ( s.equals(lblCreatePoint.getText()) )
                        createMeasureCombo(ViewOperationListener.VIEW_OPERATION.CREATE_POINT);
                      else if ( s.equals(lblCreateMultiPoint.getText()) )
                        createMeasureCombo(ViewOperationListener.VIEW_OPERATION.CREATE_MULTIPOINT);
                      else if ( s.equals(lblCreateLine.getText()) )
                        createMeasureCombo(ViewOperationListener.VIEW_OPERATION.CREATE_LINE);
                      else if (  s.equals(lblCreatePolygon.getText()) )
                        createMeasureCombo(ViewOperationListener.VIEW_OPERATION.CREATE_POLYGON);
                      else if ( s.equals(lblCreateCircle.getText()) )
                        createMeasureCombo(ViewOperationListener.VIEW_OPERATION.CREATE_CIRCLE);
                      else if ( s.equals(lblCreateRectangle.getText()) )
                        createMeasureCombo(ViewOperationListener.VIEW_OPERATION.CREATE_RECTANGLE);
                  }
              }
          });
        createShapeCB.setRenderer(new CreateListRenderer());

        statusPanel.setLayout(borderLayout3);
        LayerTreePanel.setLayout(borderLayout2);
        progressBarPanel.setLayout(flowLayout1);
        
        flowLayout1.setVgap(1);
        oprCancelButton.setToolTipText(this.propertyManager.getMsg("CANCEL_BUTTON_TOOLTIP"));
        oprCancelButton.setIcon(operationProgressEnable);
        oprCancelButton.setMaximumSize(new Dimension(20, 20));
        oprCancelButton.setMinimumSize(new Dimension(20, 20));
        oprCancelButton.setPreferredSize(new Dimension(20, 20));
        oprCancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    oprCancelButton();
                }
            });
        oprCancelButton.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    oprCancelButton_mouseEntered(e);
                }

                public void mouseExited(MouseEvent e) {
                    oprCancelButton_mouseExited(e);
                }
            });

        lblScale.setText("1:10000");
        lblScale.setMaximumSize(new Dimension(60, 14));
        lblScale.setMinimumSize(new Dimension(10, 14));
        lblScale.setHorizontalAlignment(SwingConstants.TRAILING);

        btnGeoRaptorAbout.setIcon(iconGeoRaptorAbout);
        btnGeoRaptorAbout.setText(this.propertyManager.getMsg("GEORAPTOR_ABOUT"));
        btnGeoRaptorAbout.setToolTipText(this.propertyManager.getMsg("GEORAPTOR_ABOUT_TT"));
        btnGeoRaptorAbout.setMargin(new Insets(2, 2, 2, 2));
        btnGeoRaptorAbout.setPreferredSize(new Dimension(100, 35));
        btnGeoRaptorAbout.setVerticalTextPosition(AbstractButton.CENTER);
        btnGeoRaptorAbout.setHorizontalTextPosition(AbstractButton.TRAILING);
        btnGeoRaptorAbout.setVerticalAlignment(SwingConstants.CENTER);
        btnGeoRaptorAbout.setHorizontalAlignment(SwingConstants.RIGHT);
        btnGeoRaptorAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnShowAbout();
            }
        });
        
        mainToolbar.setLayout(xyMainToolbarLayout);
        mainToolbar.setBounds(new Rectangle(0, 0, 640, 40));
        mainToolbar.setMinimumSize(new Dimension(600, 40));
        mainToolbar.setPreferredSize(new Dimension(640, 40));
        mainToolbar.setSize(new Dimension(640, 40));
        mainToolbar.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        mainToolbar.add(btnQueryByPoint,  new XYConstraints(2, 6, 28, 28));
        
        mainToolbar.add(btnPrevMBR,       new XYConstraints(37, 6, 28, 28));
        mainToolbar.add(btnNextMBR,       new XYConstraints(64, 6, 28, 28));
        
        mainToolbar.add(zoomInButton,     new XYConstraints(105, 6, 28, 28));
        mainToolbar.add(zoomOutButton,    new XYConstraints(132, 6, 28, 28));
        mainToolbar.add(zoomBoundsButton, new XYConstraints(159, 6, 28, 28));
        mainToolbar.add(moveButton,       new XYConstraints(186, 6, 28, 28));
        
        mainToolbar.add(zoomLayerButton,  new XYConstraints(224, 6, 28, 28));
        mainToolbar.add(zoomFullButton,   new XYConstraints(249, 6, 28, 28));
        mainToolbar.add(reloadButton,     new XYConstraints(289, 6, 28, 28));

        mainToolbar.add(copyImageButton,  new XYConstraints(324, 6, 28, 28));
        
        mainToolbar.add(measureShapeCB,   new XYConstraints(369, 6, 120, 30));
        mainToolbar.add(createShapeCB,    new XYConstraints(494, 6, 120, 30));
        
        // Allow tree to be scrolled
        treeView = new JScrollPane(this.viewLayerTree);
        LayerTreePanel.add(treeView);

        // ------------------------------------------------- Splitting of panels

        mainSplit.setOneTouchExpandable(true);
        mainSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        (((BasicSplitPaneUI)mainSplit.getUI()).getDivider()).addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    // Convert main split from pixels to percentage for use by componentResized event and for preferences storage
                    calcMainSplitPercentage();
                }
            });

        mainLayerSplit.setOneTouchExpandable(true);
        (((BasicSplitPaneUI)mainLayerSplit.getUI()).getDivider()).addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    // Convert main layer split from pixels to percentage for use by componentResized event and for preferences storage
                    calcMainLayerSplitPercentage();
                }
            });

        // Position of layer tree depends on preference
        //
        mainLayerSplit.add(LayerTreePanel,
                           SVPanelPreferences.getTOCPosition().equalsIgnoreCase(JSplitPane.LEFT) ?
                           JSplitPane.LEFT : JSplitPane.RIGHT);
        mainSplit.add(mainLayerSplit, JSplitPane.LEFT);
        mainSplit.add(attDataView, JSplitPane.RIGHT);
        
        pnlMainToolbar.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        pnlMainToolbar.add(mainToolbar, BorderLayout.LINE_START);
        pnlMainToolbar.add(btnGeoRaptorAbout, BorderLayout.LINE_END);
        
        this.add(pnlMainToolbar, BorderLayout.NORTH);
        this.add(mainSplit, BorderLayout.CENTER);

        // ------------------------------------------------- END Splitting of panels

        /** Start creation of status panel at bottom of view
        */
        mousePosIcon.setIcon(mapMousePosition);
        mousePosIcon.setText("(x y)");
        mousePosIcon.setToolTipText(this.propertyManager.getMsg("STATUS_LINE_MOUSE_TT"));
        mousePosLabel.setText("(x y)");

        worldLowerLeftIcon.setIcon(mapWorkareaLL);
        worldLowerLeftIcon.setText("LL(x y)");
        worldLowerLeftIcon.setHorizontalAlignment(SwingConstants.LEADING);
        worldLowerLeftIcon.setToolTipText(this.propertyManager.getMsg("STATUS_LINE_MBR_LL"));
        worldLowerLeftIcon.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    windowCentreMousePressed(e);
                }
            });

        worldUpperRightIcon.setIcon(mapWorkareaUR);
        worldUpperRightIcon.setText("UR(x y)");
        worldUpperRightIcon.setHorizontalAlignment(SwingConstants.TRAILING);
        worldUpperRightIcon.setToolTipText(this.propertyManager.getMsg("STATUS_LINE_MBR_UR"));
        worldUpperRightIcon.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    windowCentreMousePressed(e);
                }
            });

        lblMessages.setPreferredSize(new Dimension(394, 14));
        lblMessages.setHorizontalAlignment(SwingConstants.LEFT);
        lblMessages.setSize(new Dimension(394, 14));
        lblMessages.setHorizontalTextPosition(SwingConstants.LEFT);
        lblMessages.setBounds(new Rectangle(0, 0, 394, 14));
        
        progressBarPanel.add(mousePosLabel, null);
        progressBarPanel.add(worldLowerLeftIcon, null);
        progressBarPanel.add(worldUpperRightIcon, null);
        progressBarPanel.add(lblScale, null);
        progressBarPanel.add(loadDataPB, null);
        progressBarPanel.add(oprCancelButton, null);
        statusPanel.add(progressBarPanel, BorderLayout.EAST);
        statusPanel.add(lblMessages, BorderLayout.WEST);
        this.add(statusPanel, BorderLayout.SOUTH);
    }

    private void setIconsAndText() {
        btnQueryByPoint.setIcon(iconQueryNormal);
        btnQueryByPoint.setToolTipText(this.propertyManager.getMsg("TOOLBAR_QUERY"));
        btnPrevMBR.setIcon(iconPrevMBRNormal);
        btnPrevMBR.setToolTipText(this.propertyManager.getMsg("TOOLBAR_PREV_MBR"));
        btnNextMBR.setIcon(iconNextMBRNormal);
        btnNextMBR.setToolTipText(this.propertyManager.getMsg("TOOLBAR_NEXT_MBR"));
        zoomInButton.setIcon(iconZoomInNormal);
        zoomInButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOMIN"));
        zoomOutButton.setIcon(iconZoomOutNormal);
        zoomOutButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOMOUT"));
        zoomBoundsButton.setIcon(iconZoomBoundsNormal);
        zoomBoundsButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOMBOUNDS"));
        zoomFullButton.setIcon(iconZoomFullNormal);
        zoomFullButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOMFULL"));
        zoomLayerButton.setIcon(iconZoomLayerNormal);
        zoomLayerButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_ZOOM_LAYER"));
        moveButton.setIcon(iconMoveNormal);
        moveButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_MOVE"));
        reloadButton.setIcon(iconReloadNormal);
        reloadButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_RELOAD"));
        copyImageButton.setIcon(iconCopyImageNormal);
        copyImageButton.setToolTipText(this.propertyManager.getMsg("TOOLBAR_COPY_IMAGE"));
        lblMeasureInactive.setText(this.propertyManager.getMsg("LABEL_MEASURE_INACTIVE"));
        lblMeasureLine.setText(this.propertyManager.getMsg("LABEL_LINE"));
        lblMeasurePolygon.setText(this.propertyManager.getMsg("LABEL_POLYGON"));
        lblMeasureCircle.setText(this.propertyManager.getMsg("LABEL_CIRCLE"));
        lblMeasureRectangle.setText(this.propertyManager.getMsg("LABEL_RECTANGLE"));
        lblMeasureInactive.setIcon(iconMeasureInactiveNormal);
        lblMeasureLine.setIcon(iconMeasureDistanceNormal);
        lblMeasurePolygon.setIcon(iconMeasurePolygonNormal);
        lblMeasureCircle.setIcon(iconMeasureCircleNormal);
        lblMeasureRectangle.setIcon(iconMeasureRectangleNormal);
        measureShapeCB.setToolTipText(this.propertyManager.getMsg("TT_MEASURE_SHAPE"));
        lblCreateShapeInactive.setText(this.propertyManager.getMsg("LABEL_CREATE_INACTIVE")); 
        lblCreatePoint.setText(this.propertyManager.getMsg("LABEL_POINT"));
        lblCreateMultiPoint.setText(this.propertyManager.getMsg("LABEL_MULTIPOINT"));
        lblCreateLine.setText(this.propertyManager.getMsg("LABEL_LINE"));
        lblCreatePolygon.setText(this.propertyManager.getMsg("LABEL_POLYGON"));
        lblCreateCircle.setText(this.propertyManager.getMsg("LABEL_CIRCLE"));
        lblCreateRectangle.setText(this.propertyManager.getMsg("LABEL_RECTANGLE"));
        lblCreateShapeInactive.setIcon(iconCreateShapeInactive); 
        lblCreatePoint.setIcon(iconCreatePointNormal);
        lblCreateMultiPoint.setIcon(iconCreateMultiPointNormal);
        lblCreateLine.setIcon(iconCreateLineNormal);
        lblCreatePolygon.setIcon(iconCreatePolygonNormal);
        lblCreateCircle.setIcon(iconCreateCircleNormal);
        lblCreateRectangle.setIcon(iconCreateRectangleNormal);
        createShapeCB.setToolTipText(this.propertyManager.getMsg("TT_CREATE_SHAPE"));
        oprCancelButton.setToolTipText(this.propertyManager.getMsg("CANCEL_BUTTON_TOOLTIP"));
        oprCancelButton.setIcon(operationProgressEnable);
        lblScale.setText("");
        
        btnGeoRaptorAbout.setIcon(iconGeoRaptorAbout);
        btnGeoRaptorAbout.setText(this.propertyManager.getMsg("GEORAPTOR_ABOUT"));
        btnGeoRaptorAbout.setToolTipText(this.propertyManager.getMsg("GEORAPTOR_ABOUT_TT"));

        mousePosIcon.setIcon(mapMousePosition);
        mousePosIcon.setText(this.defaultCoordText);
        mousePosIcon.setToolTipText(this.propertyManager.getMsg("STATUS_LINE_MOUSE_TT"));
        mousePosLabel.setText(this.mousePosnCoordText);
        worldLowerLeftIcon.setIcon(mapWorkareaLL);
        worldLowerLeftIcon.setText(this.defaultLLCoordText);
        worldLowerLeftIcon.setToolTipText(this.propertyManager.getMsg("STATUS_LINE_MBR_LL"));
        worldUpperRightIcon.setIcon(mapWorkareaUR);
        worldUpperRightIcon.setText(this.defaultURCoordText);
        worldUpperRightIcon.setToolTipText(this.propertyManager.getMsg("STATUS_LINE_MBR_UR"));
    }

    /** ----------------------------------------------------------------
     * 
     * Widget Event Handlers
     * 
     */

      public void setNone() {
        this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);    
      }
    
     public void btnPrevMBR() 
     {
         //LOGGER.info("btnPrevMBR - Before hasPrevious()");
         if (this.activeView.getMapPanel().windowNavigator.hasPrevious() ) {
             this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.MBR_PREV);
             //LOGGER.info("btnPrevMBR - MBR_PREV about to set mapPanel window to previous()");
             this.activeView.getMapPanel().setWindow(this.activeView.getMapPanel().windowNavigator.previous());
             //LOGGER.info("btnPrevMBR - Before refreshAll");
             this.activeView.getMapPanel().refreshAll();
             //LOGGER.info("btnPrevMBR - After refreshAll");
         } 
         // LOGGER.info("btnPrevMBR - Setting operation to NONE");
         this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
     }

     public void btnNextMBR() 
     {
         if (this.activeView.getMapPanel().windowNavigator.hasNext() ) {
             this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.MBR_NEXT);
             this.activeView.getMapPanel().setWindow(this.activeView.getMapPanel().windowNavigator.next());
             //LOGGER.info("btnNextMBR - Before refreshAll");
             this.activeView.getMapPanel().refreshAll();
             // LOGGER.info("btnNextMBR - After refreshAll");
          } 
         //LOGGER.info("btnNextMBR - Setting operation to NONE");
         this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
     }

    private void zoomInButton() {
        this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.ZOOM_IN);
        // perform zoom operation
        this.activeView.getMapPanel().Zoom(org.GeoRaptor
                     .SpatialView
                     .SupportClasses
                     .Envelope
                     .zoom
                     .IN);
        // refreshAll resets setSpatialViewOpr()
        this.activeView.getMapPanel().refreshAll();
    }

    private void zoomOutButton() {
        this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.ZOOM_OUT);
        // perform zoom operation
        this.activeView.getMapPanel().Zoom(org.GeoRaptor
                     .SpatialView
                     .SupportClasses
                     .Envelope
                     .zoom
                     .OUT);
        this.activeView.getMapPanel().refreshAll();
    }

    private void zoomBoundsButton() {
        // if query operation is already selected, deselect it.
        if (this.voListener.getSpatialViewOpr() == ViewOperationListener.VIEW_OPERATION.ZOOM_BOUNDS) {
            this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
            // clean copy of background image
            this.activeView.getMapPanel().deleteBufferImage();
        } else {
            this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.ZOOM_BOUNDS);
            // create copy of background image
            this.activeView.getMapPanel().createBufferImage();
            this.setMessage(this.STATUS_RECTANGLE_CIRCLE_START,false);
        }
    }

    private void moveButton() {
        if (this.voListener.getSpatialViewOpr() != ViewOperationListener.VIEW_OPERATION.MOVE) {
            this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.MOVE);
        }
    }

    public void redraw() {
        this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
        // perform reload operation
        this.activeView.getMapPanel().setRedrawBIOnly(MapPanel.REDRAW_BI.IMAGE_SIZE_CHANGE);
        this.activeView.getMapPanel().refreshAll();
    }

    private void copyImageButton() {
        // copy image to clipboard
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        MyImageSelection myI = new MyImageSelection(activeView.getMapPanel().getBiImage());
        clipboard.setContents(myI, myI);
        this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
    }

    /**
     * We also use this function for "Zoom to Layer".
     * If _zoomLayer is null then zoom extent is calculated from all drawable layers, 
     * otherwise only using the provided layer.
     */
    public void zoomFullButton(SVSpatialLayer _zoomLayer) 
    {
        if (this.activeView.initializeMBR(_zoomLayer)) {
            // zoom to whole world
            this.voListener.setSpatialViewOpr(_zoomLayer==null
                                              ?ViewOperationListener.VIEW_OPERATION.ZOOM_FULL
                                              :ViewOperationListener.VIEW_OPERATION.ZOOM_LAYER); 
            this.activeView.getMapPanel().refreshAll();
        } else {
            JOptionPane.showMessageDialog(null,
                                          this.propertyManager.getMsg("ZOOMFULL_NO_DRAWABLE_LAYER"),
                                          this.propertyManager.getMsg("ERROR_MESSAGE_DIALOG_TITLE"),
                                          JOptionPane.ERROR_MESSAGE);
        }
        this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);  
    }

    protected void btnQueryByPoint() 
    {
LOGGER.debug("btnQueryByPoint: start");
        if (this.voListener.getSpatialViewOpr() == ViewOperationListener.VIEW_OPERATION.QUERY) {
            this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
            // clean copy of background image
            this.activeView.getMapPanel().createBufferImage();
        } else {
            this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.QUERY);
            // create copy of background image
            // Not needed now Query mouse is tracked with circle
            // this.activeView.getMapPanel().createBufferImage();
        } 
LOGGER.debug("btnQueryByPoint: finish");
    }

    public void createMeasureCombo(ViewOperationListener.VIEW_OPERATION _measureOperation) 
    {
        // if operation is already select, deselect it.
        if (this.voListener.getSpatialViewOpr() == _measureOperation) {
            this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
            // clean copy of background image
            this.activeView.getMapPanel().deleteBufferImage();
        } else if ( this.activeView.isGeodetic() && 
                    ( _measureOperation == ViewOperationListener.VIEW_OPERATION.CREATE_CIRCLE ||
                      _measureOperation == ViewOperationListener.VIEW_OPERATION.MEASURE_CIRCLE ) ) 
        {
            // This call resets the status message
            this.voListener.setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.NONE);
            // clean copy of background image
            this.activeView.getMapPanel().deleteBufferImage();
            // Now write the right message to the status bar
            this.setMessage(this.propertyManager.getMsg("ERROR_MESSAGE_GEODETIC_CIRCLE",this.activeView.getSRID()),true);
        } else {
            this.voListener.setSpatialViewOpr(_measureOperation);
            // create copy of background image
            this.activeView.getMapPanel().createBufferImage();
            // initialise mapPanel measurement shapes
            //
            this.activeView.getMapPanel().initialiseShapeActions();
        }
    }

    protected void btnShowAbout() {
        ControlerSV.showAboutBox();        
    }

    /** ----------------------------------------------------------------
     * 
     * SpatialViewPanel methods 
     * 
     */
    
     // Need to create a default view like SRID:NULL but use defaultSRID 
     // and let SpatialView constructor create name from SRID
     //
    public boolean createDefaultView(boolean _active) {
        String defaultViewName = SpatialView.createViewName(SVPanelPreferences.getSRID());
        SpatialView defaultSpatialView = this.getView(defaultViewName);
        if ( defaultSpatialView==null ) {
            defaultSpatialView = 
                this.newView(defaultViewName,
                             SVPanelPreferences.getSRID(),
                             _active); // Ignore SpatialView returned
            return ( defaultSpatialView == null ) ? false : true;
        } else {
            this.setActiveView(defaultSpatialView);
            return true;
        }
     }

    public boolean hasDefaultView() {
        String defaultViewName = SpatialView.createViewName(SVPanelPreferences.getSRID());
        return this.getView(defaultViewName)==null?false:true;
    }
    
     public String getDefaultSRID() {
       return SVPanelPreferences.getSRID();
     }

     public ImageIcon getQueryIcon(ViewOperationListener.VIEW_OPERATION _vol) 
     {
         switch (_vol) {
           case CREATE_POINT      :  
           case QUERY_POINT       : return iconCreatePointNormal;
           case CREATE_MULTIPOINT :
           case QUERY_MULTIPOINT  : return iconCreateMultiPointNormal;
           case QUERY             : return iconQueryNormal;
           case MOVE              : return iconMoveNormal;
           case MEASURE_RECTANGLE : return iconMeasureRectangleNormal;
           case CREATE_RECTANGLE  :
           case QUERY_RECTANGLE   : return iconCreateRectangleNormal;
           case MEASURE_CIRCLE    : return iconMeasureCircleNormal;
           case CREATE_CIRCLE     :
           case QUERY_CIRCLE      : return iconCreateCircleNormal;
           case ZOOM_BOUNDS       : return iconZoomBoundsNormal;
           case ZOOM_IN           : return iconZoomInNormal;
           case ZOOM_OUT          : return iconZoomOutNormal;
           case CREATE_LINE       :
           case QUERY_LINE        : return iconCreateLineNormal;
           case MEASURE_DISTANCE  : return iconMeasureDistanceNormal;
           case MEASURE_POLYGON   : return iconMeasurePolygonNormal;
           case CREATE_POLYGON    : 
           case QUERY_POLYGON     : return iconCreatePolygonNormal;
		default:
			break;
         }
         return iconQueryNormal;
     }

     public void setScale(String _scale) {
       lblScale.setText(_scale);       
     }
     
    public void setMessage(String _message,
                           boolean _beep) 
    {
        if ( _beep ) {
            this.lblMessages.setForeground(Color.RED);
            Toolkit.getDefaultToolkit().beep();
        } else {
            this.lblMessages.setForeground(Color.BLACK);
        }
        this.lblMessages.setText(Strings.isEmpty(_message) 
                                 ? this.activeView.getViewName() 
                                 : _message);
    }
    
    public PropertiesManager getPropertyManager() {
        return this.propertyManager;
    }

    /**
     * @method getMainLayerSplitPercentageAsPixels
     * @return generates divider location in pixels from stored percentage value
     * @author Simon Greener, June 21st 2010 Original Coding
     */
    private int getMainLayerSplitPercentageAsPixels() {
        if ( this.getWidth() != 0 && this.mainLayerSplitPercentage != 0 ) {
            return Math.round((float)this.mainLayerSplitPercentage / 100.0f * (float)this.getWidth() );
        }
        return this.mainLayerSplit.getDividerLocation();
    }
    
    /**
     * @method getMainSplitPercentageAsPixels
     * @return generates divider location in pixels from stored percentage value
     * @author Simon Greener, June 21st 2010 Original Coding
     */
    private int getMainSplitPercentageAsPixels() {
         if ( this.getHeight() != 0 && this.mainSplitPercentage != 0 ) {
             return Math.round((float)this.mainSplitPercentage / 100.0f * (float)this.getHeight() );
         }
         return this.mainSplit.getDividerLocation();
    }

    /**
     * @method calcMainLayerSplitPercentage
     * @return  From existing divider location in pixels, calcs value as a percentage and stores it
     * @author Simon Greener, June 21st 2010 Original Coding
     */
    private void calcMainLayerSplitPercentage() {
        if ( this.getWidth() != 0 ) {
            this.mainLayerSplitPercentage = Math.round( (float)this.mainLayerSplit.getDividerLocation() / (float)this.getWidth() * 100.0f);
            // Save change to global preferences
            this.SVPanelPreferences.setMainLayerSplitPos(this.mainLayerSplitPercentage);
        }
    }
    
    /**
     * @method calcMainSplitPercentage
     * @return  From existing divider location in pixels, calcs value as a percentage and stores it
     * @author Simon Greener, June 21st 2010 Original Coding
     */
    private void calcMainSplitPercentage() {
        if ( this.getHeight() != 0 ) {
            this.mainSplitPercentage = Math.round( (float)this.mainSplit.getDividerLocation() / (float)this.getHeight() * 100.0f);
            // Save change to global preferences
            this.SVPanelPreferences.setMainSplitPos(this.mainSplitPercentage);
        }
    }
    
    public void setLoading(boolean _loading) {
         this.loading = _loading;
    }
    
    public boolean isLoading() {
      return this.loading;
    }
    
    public ImageIcon getMapWorkareaUR() {
        return this.mapWorkareaUR;
    }

    /**
     * Refresh state of progress bar and cancel icon.
     * @param _enable enable or disable progress bar
     * @param _text bar text
     * @param _currentLayer current value of progress bar scale
     * @param _allLayers max value of progress bar scale
     */
    public void setProgressBar(final boolean _enable, 
                               final String _text, 
                               final int _currentLayer,
                               final int _allLayers) 
    {
        this.loadDataPB.setEnabled(_enable);
        this.oprCancelButton.setEnabled(_enable);

        if (_enable == false) { // TODO
            this.loadDataPB.setMinimum(0);
            this.loadDataPB.setMaximum(100);
            this.loadDataPB.setString("");
            this.loadDataPB.setValue(0);
        } else {
            this.loadDataPB.setMinimum(0);
            this.loadDataPB.setMaximum(_allLayers);
            this.loadDataPB.setValue(_currentLayer);
            this.loadDataPB.setStringPainted(true);
            this.loadDataPB.setString(_text);
        }
    }

    public ViewLayerTree getViewLayerTree() {
      return this.viewLayerTree;
    }

    public void reInitialisePanel()
    {
        // Reset MapPanel MBR to nothing
        this.getMapPanel().setWindow(new Envelope(this.activeView.getDefaultPrecision()));
        // Disable toolbar buttons
        this.setToolbarEnabled(false);
        // Refresh coordinate MBRs readout labels
        this.updateViewStatusMBR(new Envelope(this.activeView.getDefaultPrecision()),
                                 this.activeView.getPrecision(false));
    }
    
    public LayerReturnCode addNewSpatialLayer(String           _schemaName,
                                              String           _objectName, 
                                              String           _columnName,
                                              String           _objectType, /* TABLE/VIEW/MATERIALIZED VIEW */
                                              String           _connectionName, 
                                              OracleConnection _conn,
                                              boolean          _zoom) 
    {
        LOGGER.debug("START: SpatialViewPanel.addNewSpatialLayer");
        String columnName = null;
        try 
        {
            // Get all geo columns in the object
            //
            List<String> geoColumns;
            try
            {
                geoColumns = MetadataTool.getGeoColumns(_conn,
                                                        _schemaName,
                                                        _objectName);
            } catch (SQLException sqle) {
                JOptionPane.showMessageDialog( null,
                                               propertyManager.getMsg("ERROR_MESSAGE_NO_TABLE_GEOMETRY_COLUMN",
                                                                     Strings.append(_schemaName,_objectName,Constants.TABLE_COLUMN_SEPARATOR)),
                                               MainSettings.EXTENSION_NAME,
                                               JOptionPane.INFORMATION_MESSAGE);
                return LayerReturnCode.Fail;
            } catch (IllegalArgumentException iae) {
              JOptionPane.showMessageDialog(null,
                                            propertyManager.getMsg("OBJECT_RETRIEVAL_ERROR",
                                                                   iae.getLocalizedMessage(),"OBJECT_GEOMETRY_COLUMNS"),
                                            MainSettings.EXTENSION_NAME,
                                            JOptionPane.ERROR_MESSAGE);
              return LayerReturnCode.Fail;
            }
            if ( geoColumns ==null || geoColumns.size()==0) {
                throw new Exception("No geometry columns exist in " + Strings.append(_schemaName, _objectName, "."));
            } 
            
            // If supplied name is empty we need to ask for the column to map
            //
            if (Strings.isEmpty(_columnName) ) { 
                if ( geoColumns.size()==1 ) {
                    columnName = geoColumns.get(0);
                } else {
                    LOGGER.debug("Requesting column to map from geometry column list.");
                    // Ask which one want to map
                    //
                    String entryList[] = new String[geoColumns.size()];
                    for (int i = 0; i < geoColumns.size(); i++) {
                        entryList[i] = geoColumns.get(i);
                    }
                    String value = (String)JOptionPane.showInputDialog(
                                              null,
                                              propertyManager.getMsg("ADD_LAYER_SELECT_COLUMN"),
                                              MainSettings.EXTENSION_NAME,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null, // Use default icon
                                              entryList,
                                              entryList[0]);
                    if (Strings.isEmpty(value) ) {
                        return LayerReturnCode.Fail;
                    }
                    columnName = value;
                }
            } else {
                // supplied columnName should be in list
                for (int i = 0; i < geoColumns.size(); i++) {
                    if ( geoColumns.get(i).equalsIgnoreCase(_columnName) ) {
                        columnName = _columnName;
                    }
                }
                if (Strings.isEmpty(columnName) ) {
                    LOGGER.error("Supplied column name " + _columnName + " does not exist in " +
                                 Strings.append(_schemaName, _objectName, "."));
                    return LayerReturnCode.Fail;                    
                }
            }
            LOGGER.debug("Geometry column being processed is " + columnName);
        } catch (Exception e) {
          LOGGER.error("Failed to add new layer because " + e.toString());
          return LayerReturnCode.Fail;
        }
        
        // Now get new layer's metadata
        //
        String schemaTableColumn =
            Strings.append(_schemaName, Strings.append(_objectName,
                                                             columnName,
                                                             Constants.TABLE_COLUMN_SEPARATOR),
                                                Constants.TABLE_COLUMN_SEPARATOR);
        MetadataEntry mEntry = null;
        try {
            LinkedHashMap<String, MetadataEntry> metaEntries = null;
            metaEntries = MetadataTool.getMetadata(_conn,
                                                   _schemaName,
                                                   _objectName,
                                                   columnName,
                                                   false);
            LOGGER.debug(metaEntries.toString());
            // Find and assign mEntry based on choice
            for (MetadataEntry me: metaEntries.values() ) {
                if ( me.getColumnName().equalsIgnoreCase(columnName) )  {
                    mEntry = new MetadataEntry(me);
                }
            }
            // Check if missing
            //
            if ( mEntry.isMissing()) {
                throw new Exception(schemaTableColumn + " has no XXX_SDO_GEOM_METADATA entry.");
            } 
        } catch (Exception e) {
            int n = JOptionPane.showConfirmDialog(null,
                                                  propertyManager.getMsg("ADD_LAYER_MISSING_MD",
                                                                         schemaTableColumn),
                                                  MainSettings.EXTENSION_NAME,
                                                  JOptionPane.YES_NO_OPTION);
            if (n == 0) {
                return LayerReturnCode.Metadata;
            } else {
                return LayerReturnCode.Fail;
            }
        }
        String layerName = Strings.objectString(mEntry.getSchemaName(), 
                                                     mEntry.getObjectName(), 
                                                     mEntry.getColumnName());
        LOGGER.debug("layerName from mEntry is " + layerName);

        // layer gtype discovery
        //
        String layerGeometryType = "";
        try {
            // 0 Percentage means no sampling. 
            // 1 means first one found ie ROWNUM < 2
            layerGeometryType = MetadataTool.getLayerGeometryType(_conn,
                                                                  mEntry.getSchemaName(), 
                                                                  mEntry.getObjectName(), 
                                                                  mEntry.getColumnName(),
                                                                  0, /* samplePercentage */
                                                                  1  /* sampleRows */ );
            if (Strings.isEmpty(layerGeometryType)) {
                LOGGER.error("getLayerGeometryType for " + layerName + " returned null");
            }
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
            LOGGER.error("Add New Spatial Layer terminated.");
            return LayerReturnCode.Fail;
        }
        
        // *************************************************************************************
        // We have enough information to create a valid layer
        // Need to construct using _objectType ????
        //
        SVSpatialLayer layer = new SVSpatialLayer(this.activeView,  // Use activeView temporarily
                                                  layerName, 
                                                  SVPanelPreferences.isSchemaPrefix()
                                                     ? layerName :
                               Strings.append(mEntry.getObjectName(),
                                                                    mEntry.getColumnName(),"."), 
                                                  layerName,
                                                  mEntry,
                                                  true /* draw - should be GeoRaptor Property */);
        
        // See if we can discover a better MBR than that derived from metadata.
        if (layer.setLayerMBR(mEntry.getMBR(),
                              layer.getSRIDAsInteger())==false) {
            return LayerReturnCode.MBR;
        }
        LOGGER.debug(layer.getVisibleName() + "'s mbr is " + layer.getMBR());

        // Set layer properties
        // 
        layer.setConnectionName(_connectionName);
        layer.setGeometryType(layerGeometryType); LOGGER.debug("connection set to " + _connectionName);
        layer.setSRIDType();                      LOGGER.debug("layer srid type is " + layer.getSRIDType().toString());
        if ( layer.getSRIDType().toString().startsWith("GEO") ) {
            layer.setPrecision(Constants.MAX_PRECISION);
        } else {
            layer.setPrecision(-1);  // -1 forces calculation
        }
        layer.setFetchSize(this.SVPanelPreferences.getFetchSize());
        layer.setMinResolution(this.SVPanelPreferences.isMinResolution());
        layer.getStyling().setSelectionTransLevel(Constants.SOLID);
        layer.getStyling().setShadeTransLevel(Constants.SOLID);
        layer.setIndex();    // Will call MetadataTool.hasSpatialIndex()
        // Set whether ST_GEOMETRY object
        try {
            layer.setSTGeometry(MetadataTool.isSTGeometry(_conn,mEntry));
        } catch (SQLException e) {
            layer.setSTGeometry(false);
        }
        // Add layer to relevant view
        //
        LayerReturnCode b = addLayerToView(layer,_zoom) ? LayerReturnCode.Success : LayerReturnCode.Fail;        
        return b;
    }

  public boolean addLayerToView(SVSpatialLayer _layer,
                                boolean        _zoom)
  {
      SpatialView targetView = null;
      LOGGER.debug("addLayerToView: "+SVSpatialLayer.CLASS_NAME);
      // 1. Does a view exist with same SRID as layer?
      //
      targetView = getViewBySRID(_layer.getSRID());
      if (targetView != null) {
          // Yes, so add layer to the view (addLayer checks layerName)
          if ( targetView.addLayer(_layer,_layer.isDraw(),true/*active*/, _zoom) ) {
              this.setActiveView(targetView);
              return true;
          } else {
              return false;
          }
      } 

      // 2. No view has layer's SRID 
      //    Ask which view to add layer to if not graphic theme 
      //    as graphic themes cannot be projected by GeoRaptor.
      //
      String selectedViewName = "";
      String createNewView =  this.propertyManager.getMsg("VIEW_NEW_NAME");
      if ( _layer instanceof SVGraphicLayer ) {
          selectedViewName = createNewView;
      } else {
          selectedViewName = this.whichView(createNewView,_layer.getSRID());
          if (Strings.isEmpty(selectedViewName) ) {
              return false;
          }
      }
      
      if ( selectedViewName.equals(createNewView) ) 
      {
          LOGGER.debug("selectedViewName.equals(createNewView) ie " + selectedViewName + ".equals("+createNewView+")");
          // Create new view.
          String createViewName = SpatialView.createViewName(_layer.getSRID());
          targetView = this.newView(createViewName,_layer.getSRID(),false);
          targetView.setSRID(_layer.getSRID());
          targetView.setSRIDType(_layer.getSRIDType());
          targetView.setPrecision(_layer.getPrecision(false));
          targetView.setDistanceUnitType();
          targetView.setAreaUnitType();
          // Now add layer to the new view
          if ( targetView.addLayer(_layer,_layer.isDraw(),true/*active*/,_zoom) ) {
            // set MBR of new View
            //
            if ( ! targetView.initializeMBR(_layer) ) {
                LOGGER.warn("SpatialViewPanel: " + targetView.getVisibleName() + "'s MBR could not be set for layer " + _layer.getVisibleName());
            }
            this.setActiveView(targetView); // Will expand the provided view
            this.setToolbarEnabled(true);   // Make toolbar buttons active as we have a new view with a layer
            this.updateViewStatusMBR(_layer.getMBR(),_layer.getPrecision(false));          
          }

      } else {
          targetView = this.getView(selectedViewName);
          if (targetView != null) {
              // notify layer that it may need to be projected
              _layer.setProject( ! (    _layer.getSRID().equals(Constants.NULL) || 
                                    targetView.getSRID().equals(Constants.NULL) ||
                                        _layer.getSRID().equals(targetView.getSRID()) ),
                                 true );
              if ( targetView.addLayer(_layer,_layer.isDraw(),true/*active*/,_zoom) ) {
                  this.setActiveView(targetView);
              } else {
                  return false;
              }
          } 
      }
      // Create initial layer SQL with possible requirement to project
      // can only be determined after assignment to a view because
      // only then will both SRIDs be known
      //
      //if ( ! _layer.CLASS_NAME.equalsIgnoreCase(Constants.KEY_SVWorksheetLayer) ) {
      _layer.setSQL(_layer.getInitSQL(_layer.getMetadataEntry().getColumnName()));
      return true;
  }
  
  public String whichView(String _newViewName,
                          String _layerSRID) 
  {
      // If _newViewName is null then the creation of a new view is not offered as an option.
      Object[] selectionValues = this.getViewNames(_newViewName);
      Object selectedValue = JOptionPane.showInputDialog(null, 
                                                         this.propertyManager.getMsg("VIEW_LAYER_TO_VIEW",_layerSRID),
                                                         this.propertyManager.getMsg("VIEW_LAYER_TO_VIEW_HEADER"),
                                                         JOptionPane.QUESTION_MESSAGE, 
                                                         null, 
                                                         selectionValues, 
                                                         selectionValues[0]);
      return (String)selectedValue;
  }

    /**
     * @method setMapPanelInvisible
     * @description Because a request to switch views may originate from a click on a View radio button
     *              We need to make the map panel associated with the old button selection (the active View)
     *              invisible without calling back to the (changing/changed) ViewLayerTree to discover the 
     *              old name (now lost). Best way is to simply iterate over all MapPanels and turn off that
     *              which is currently visible (the old active view).
     * @author Simon Greener, October 2010
     */
    public void setMapPanelsInvisible() {
        SpatialView itView = null;
        Iterator<SpatialView> it = this.viewLayerTree.getViewsAsList().values().iterator();
        while (it.hasNext()) {
            itView = it.next();
            if ( itView.getMapPanel().isVisible() ) {
                itView.getMapPanel().setVisible(false);
            }
        }
    }

    public void setActiveView(final SpatialView _sView) 
    {
        if ( _sView == null ) {
            return;
        }

        LOGGER.debug("setActiveView(" + _sView.getViewName() + ")");

        // Save split position of existing screen as the removal of the mapPanel will change it.
        //
        final int saveMainLayerSplit = getMainLayerSplitPercentageAsPixels() + this.mainLayerSplit.getInsets().left;
        final int saveMainSplit      = getMainSplitPercentageAsPixels()      + this.mainSplit.getInsets().bottom;

        // Make existing visible MapPanel panel invisible
        //
        setMapPanelsInvisible();

        /**
         * Because this method could be called from a ViewActiveLayer method in response to user selection,
         * we need to ensure that the call back to the ViewActiveLayer to expand the layer nodes of the new view
         * does not interfere with the calling, unfinished, method.
         */
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainLayerSplit.add(_sView.getMapPanel(),
                                   SVPanelPreferences.getTOCPosition().equalsIgnoreCase(JSplitPane.LEFT)
                                   ? JSplitPane.RIGHT
                                   : JSplitPane.LEFT);
                // Re-Apply splits
                //
                if ( saveMainLayerSplit != 0 ) mainLayerSplit.setDividerLocation(saveMainLayerSplit );
                if ( saveMainSplit != 0 )           mainSplit.setDividerLocation(saveMainSplit);
                // Make new view's MapPanel visible
                //
                lblMessages.setText(_sView.getVisibleName());
                // Ensure the associated node is active
                int posn = viewLayerTree.getViewPosition(_sView.getViewName());
                if ( posn > -1 ) {
                    // Set passed in view as the activeView (radio button should also be checked)
                    //
                    activeView = _sView;
                    viewLayerTree.collapseAllViews(); 
                    viewLayerTree.setViewActive(posn);
                    // Ensure this view's node is expanded
                    ViewLayerTree.ViewNode vNode = viewLayerTree.getViewNode(_sView.getViewName());
                    viewLayerTree.setSelectionPath(ViewLayerTree.getPath(vNode));  // Highlight
                    if (vNode!=null) {
                        vNode.expand();
                    }
                    // Make sure MapPanel is visible and initialized
                    activeView.getMapPanel().Initialise();    
                    activeView.setVisible();
                } // else it isn't active
            }
        });
    }

    public void setActiveView(String _viewName) {
        // find newView
        SpatialView newActiveView = this.viewLayerTree.getView(_viewName);
        if ( newActiveView == null ) {
            return;
        }
        this.setActiveView(newActiveView);
    }
  
    public SpatialView newView(String  _viewName, 
                               String  _srid, 
                               boolean _activate) 
    {
        SpatialView newView = new SpatialView(this,_viewName,_srid);
        this.addView(newView,_activate);
        return newView;
    }
        
    public void addView(SpatialView _sView,
                        boolean     _activate) 
    {
        if ( _sView == null ) {
            return;
        }
        this.viewLayerTree.addView(_sView, ( _activate || this.activeView == null ));
        if ( _activate || this.activeView == null ) {
            this.setActiveView(_sView); 
        }
    }
    
    public int getViewCount() {
        return this.viewLayerTree.getViewCount();
    }
    
    public void setViewAt(int _viewNum) {
        this.viewLayerTree.setViewActive(_viewNum);
        this.setActiveView(this.viewLayerTree.getViewAt(_viewNum));
    }
    
    public Map<String, SpatialView> getViews() {
        return this.viewLayerTree.getViewsAsList();
    }  

    private Object[] getViewNames(String _newView) 
    {
        if ( this.viewLayerTree.getViewsAsList() == null )
            return new Object[]{_newView};
        Object[] retList = new Object[this.viewLayerTree.getViewsAsList().size() + (Strings.isEmpty(_newView) ? 0 : 1 ) ];
        if (Strings.isEmpty(_newView) == false )
            retList[0] = (Object)_newView;
        SpatialView itView = null;
        Iterator<SpatialView> it = this.viewLayerTree.getViewsAsList().values().iterator();
        int i = (Strings.isEmpty(_newView) ? 0 : 1 );
        while (it.hasNext()) {
            itView = it.next();
            retList[i] = itView.getViewName();  i++;
          }
      return retList;
    }
    
    protected SpatialView getViewBySRID(String _SRID) {
        if (this.viewLayerTree.getViewsAsList() == null )
            return null;
        SpatialView retView = null;
        Iterator<SpatialView> it = this.viewLayerTree.getViewsAsList().values().iterator();
        while (it.hasNext()) {
            retView = it.next();
            if (retView.getSRID().equalsIgnoreCase(_SRID)) {
                return retView;
            }
        }
        return null;
    }
    
    public SpatialView getMostSuitableView(int _srid) {
        // Get right view to draw geometries in by checking SRID compatibility
        // Does not create a new view!
        //
        SpatialView mappingView = null;
        int viewSRID = this.activeView.getSRIDAsInteger();
        if ( viewSRID == _srid ) {
            // activeView is the right view for mapping
            //
            mappingView = this.activeView;
        } else {
            // SRIDs are different....
            // Is there a view with the right SRID?
            //
            String srid = _srid==Constants.SRID_NULL?Constants.NULL:String.valueOf(_srid);
            mappingView = this.viewLayerTree.getView(SpatialView.createViewName(srid));
        } 
        return mappingView;
    }
    
    private static String readFile(String path) throws IOException {
      FileInputStream stream = new FileInputStream(new File(path));
      try {
        FileChannel fc = stream.getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        /* Instead of using default, pass in a decoder. */
        return Charset.defaultCharset().decode(bb).toString();
      }
      finally {
        stream.close();
      }
    }
    public void loadViewFromDisk(ViewLayerTree.ViewNode _clickedViewNode) 
    {
        String loadXML = "";
        // Get XML from disk
        //
        File file = null;
        try {
            FileFilter xmlFilter = new ExtensionFileFilter("XML", new String[] { "XML" });
            JFileChooser fc = new JFileChooser();            
            fc.setDialogTitle(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_TITLE"));
            fc.setFileFilter(xmlFilter);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                LOGGER.fine(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_CANCEL"));
            } else {
                file = fc.getSelectedFile();
                //This is where a real application would save the file.
                LOGGER.fine(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_XML_FILE",file.getName()));            
                loadXML = SpatialViewPanel.readFile(file.getAbsolutePath());
                if (Strings.isEmpty(loadXML) ) {
                    throw new Exception(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_EMPTY"));
                }
            }
        }catch (Exception e){//Catch exception if any
            LOGGER.error(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_ERROR",(file==null?"unknown.xml":file.getName()),e.getMessage()));
            return;
        }

        try {
            Document doc = null;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Check what sort of XML we have 
            //
            doc = db.parse(new InputSource(new StringReader(loadXML))); 
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            ViewLayerTree.ViewNode vNode = null;
            SpatialView newView = null;
            SpatialView existingView = null;
            String topLayerNode = "Layers/Layer";
            String activeLayerName = "";
            Node lNode = null;
            Node topNode = (Node)xpath.evaluate("/View",doc,XPathConstants.NODE);
            if ( topNode == null || 
                 topNode.getNodeName().equals("View")==false ) {
                // Check if layer
                topNode = (Node)xpath.evaluate("/Layers",doc,XPathConstants.NODE);
                if (topNode==null) {
                    this.setMessage(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_NO_DEFN"),true);
                    return;
                }
                vNode = _clickedViewNode==null?this.getActiveView().getViewNode():_clickedViewNode;
                topLayerNode = "Layer";
            } else {
                newView = new SpatialView(this,loadXML);
                // If this view already exists - we will re-use it, else use new one
                // 
                if ( newView != null ) {
                    existingView = this.getViewBySRID(newView.getSRID());
                    vNode = _clickedViewNode;
                    if ( existingView == null ) {
                        vNode = this.viewLayerTree.addView(newView,false);
                    } else if (newView.getViewID().equals(existingView.getViewID())) {
                        vNode = this.viewLayerTree.addView(newView,false);
                    } else {
                        vNode = existingView.getViewNode();
                    }
                    try { activeLayerName = (String)xpath.evaluate("View/ActiveLayer/text()",topNode,XPathConstants.STRING); } catch (XPathExpressionException e) {}
                }
            }
            this.setMessage(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_LOAD_VIEW",vNode.getSpatialView().getVisibleName()), false);
            // Now process layers in the XML
            //
            SVSpatialLayer newLayer = null;
            NodeList lList = (NodeList) xpath.evaluate(topLayerNode,topNode,XPathConstants.NODESET);
            for(int j = 0 ; j < lList.getLength(); j++){
                lNode = lList.item(j).cloneNode(true);
                if ( SpatialViewSettings.isOfLayerType(lNode,"SVQueryLayer/SQL/text()") ) {
                    newLayer = new SVQueryLayer(vNode.getSpatialView(),lNode);
                } else if ( SpatialViewSettings.isOfLayerType(lNode,"SVWorksheetLayer/SQL/text()") ) {
                    newLayer = new SVWorksheetLayer(vNode.getSpatialView(),lNode);
                } else {
                    newLayer = new SVSpatialLayer(vNode.getSpatialView(),lNode);
                }
                LOGGER.info(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_LOAD_LAYER",newLayer.getLayerName(),vNode.getVisibleName()));                
                // ignore result as error message has been displayed
                //
                vNode.addLayer(newLayer,
                               newLayer.isDraw(), /* isDrawn */
                               newLayer.getLayerName().equals(activeLayerName));
            }
            // We've finished processing the view and its layers.
            // Just check to make sure the view has an active layer
            //
            if ( newView.getActiveLayer()==null ) {
                if ( newView.getLayerCount()!=0)
                    newView.setActiveLayer(newLayer);
            }
            
            this.viewLayerTree.collapseAllViews();
            // Only set the new view active
            //
            if ( existingView != null ) {
                this.setActiveView(newView);  // Will expand the provided view
                this.activeView.getMapPanel().Initialise();
            }

        } catch (XPathExpressionException xe) {
            LOGGER.error(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_XPATH_ERR",xe.getMessage()));
        } catch (ParserConfigurationException pe) {
            LOGGER.error("ParserConfigurationException " + pe.toString());
        } catch (SAXException se) {
            LOGGER.error("SAXException " + se.toString());
        } catch (IOException ioe) { 
            LOGGER.error(this.propertyManager.getMsg("LOAD_VIEW_FROM_DISK_XML_PARSE",ioe.toString()));
        }
    }

    /**
    * @method  showGeometry
    * @param   _geo
    * @param   _zoomToGeometry
    * @author Simon Greener May 31st 2010
    *          Added call
    *               tLayer.getDrawTools().setGraphics2D(_g2d);
    *          to before:
    *               tLayer.callDrawFunction(_geo,"",0.0f);
    *          as callDrawFunction no longer called setGraphics2D
    *          for each and every geometry it draws in a layer.
    *          Called once at start of draw of all.
    *          - Added following call to just before callDrawFunction()
    *             tLayer.setLayerTempAlphaComposition();
    * @author Simon Greener June 7th 2010
    *          Modified function so that it will render more than one geometry
    *          as queryByPoint calls this.
    *          - Made function more efficient for setting environment as
    *            original coding obliterated all multiple calls by queryByPoint
    *            to display a selection.
    * @author Simon Greener June 17th 2010
    *          Added improved selection colouring based on geometry or layer type
    */  
    public void showGeometry(final SVSpatialLayer  _layer,
                             final STRUCT          _geo, 
                             final List<QueryRow>  _geoSet,
                             final Envelope _mbr,
                             final boolean         _selectionColouring,
                             final boolean         _zoom,
                             boolean               _drawAfter) 
    {
        if ( _geo == null && _geoSet == null) {
            JOptionPane.showMessageDialog(null,
                                          this.SHOW_GEOMETRY_PARAMETER_ERROR,
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if ( _geoSet!=null && _geoSet.size()==0 ) {
            JOptionPane.showMessageDialog(null,
                                          this.SHOW_GEOMETRYSET_PARAMETER_ERROR,
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.ERROR_MESSAGE);
          return;
        }

        // Get right view to draw geometries in by checking SRID compatibility
        // Question: What if passed in _layer is not null and has a layer?
        //
        int geometrySRID = (_geo!=null ? SDO_GEOMETRY.getSRID(_geo,Constants.SRID_NULL) :
             SDO_GEOMETRY.getSRID(_geoSet.get(0).getGeoValue(),Constants.SRID_NULL) );
        SpatialView mappingView = null;
        mappingView = this.getMostSuitableView(geometrySRID);
        if ( mappingView==null ) {
            String srid = geometrySRID==Constants.SRID_NULL?Constants.NULL:String.valueOf(geometrySRID);
            // geometry incompatible with active view
            JOptionPane.showMessageDialog(null,
                                          this.propertyManager.getMsg("SHOW_GEOMETRY_SRID_MISMATCH",
                                                                      srid),
                                          this.propertyManager.getMsg("ERROR_MESSAGE_DIALOG_TITLE"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        int viewSRID = mappingView.getSRIDAsInteger();
        boolean zoom = _zoom;
        
        // ... And set it active if not already
        //
        if ( ! this.activeView.getViewName().equals(mappingView.getViewName()) ) {
            this.setActiveView(mappingView);
        }

        // Get MBR of input geometries
        // so we can draw correctly
        //
        Envelope mbr = null;
        if ( _mbr == null || _mbr.isNull() ) {
            mbr = ( _geoSet == null ) 
                  ? new Envelope(SDO_GEOMETRY.getGeoMBR(_geo))
                  : new Envelope(_geoSet);
        } else {
            mbr = new Envelope(_mbr);
        }

        // If a Point is being drawn (point's width/height may be 0 pixels)
        // then we can't expand by 10% but have to add simply 4 pixels
        //
        if ( mbr == null || mbr.isNull() ) {
            LOGGER.error("Invalid object MBR");
            return;
        }
        if ( mbr.getWidth() == 0 && mbr.getHeight() == 0 ) {
            Point2D.Double p = null;
            try {
                p = mappingView.getMapPanel().getPixelSize();
            } catch (NoninvertibleTransformException e) {
                LOGGER.error("SpatialViewPanel.showGeometry: expand MBR="+e.toString());
                p = new Point2D.Double(0.1,0.1);
            }
            double w = (Double.isNaN(p.getX()) ? 0.0 : Math.max(p.getX(),p.getY()));
            if ( w==0.0 ) {
                w = (mappingView.getSRIDType().toString().startsWith("GEO")
                    ? 0.00001 / 4.0
                    : 20.0 / 4.0);
            }
            mbr.setChange(w*4.0);
        } else { 
            mbr.setIncreaseByPercent(10); // add 10% around MBR
        }
        // Set MBR of new view (regardless as to whether we are zooming or not)
        // because if we need world to screen transformation set to be able to
        // view anything. Also, if processing a single point then we can't getPixelSize 
        // of current map unless MBR set for (possibly new) view.
        //
        if ( mappingView.getMapPanel().getWorldToScreenTransform()==null ) {
            // Force setting of WorldToScreenTransform()
            zoom = true;
        }
        
        // If layer not supplied can we get our properties from the active layer?
        //
        SVSpatialLayer propertiesLayer = null;
        if ( _layer == null ) {
            propertiesLayer = mappingView.getActiveLayer();
            if  (propertiesLayer == null) 
            {
                // Get first layer to use for properties
                //
                propertiesLayer = mappingView.getFirstLayer();
                // If there isn't any layers then create one
                //
                if ( propertiesLayer == null ) {
                    // Make a temporary layer...
                    //
                    MetadataEntry me = new MetadataEntry(null,"ASCETATE",null,String.valueOf(viewSRID));
                    me.add(mbr);
                    propertiesLayer = new SVGraphicLayer(mappingView,"Render Layer","Screen Name","A temporary layer for properties only",me,true);
                    propertiesLayer.setMBR(mbr);
                    propertiesLayer.getStyling().setPointSize(12);
                    propertiesLayer.getStyling().setPointType(PointMarker.MARKER_TYPES.CIRCLE);
                    propertiesLayer.getStyling().setPointColor(Colours.getRandomColor());
                    propertiesLayer.getStyling().setLineColor(Colours.getRandomColor());
                    propertiesLayer.getStyling().setShadeColor(Colours.getRandomColor());
                    propertiesLayer.getStyling().setShadeTransLevel(0.5f);
                    propertiesLayer.getStyling().setPointColorType(SVPanelPreferences.isRandomRendering()?Styling.STYLING_TYPE.RANDOM:Styling.STYLING_TYPE.CONSTANT);
                    propertiesLayer.getStyling().setLineColorType(SVPanelPreferences.isRandomRendering()?Styling.STYLING_TYPE.RANDOM:Styling.STYLING_TYPE.CONSTANT);
                    propertiesLayer.getStyling().setShadeType(SVPanelPreferences.isRandomRendering()?Styling.STYLING_TYPE.RANDOM:Styling.STYLING_TYPE.CONSTANT);
                    // this.errorDialogHandler.showErrorDialog(null,"SPATIAL_COLUMN_OPR_ACT_LAYER_SELECT",mappingView.getViewName());
                    // return;
                }
            } 
        } else {
            propertiesLayer = _layer;
        }
        if ( propertiesLayer.getConnection()==null ) {
            propertiesLayer.setConnection(mappingView.getConnectionName());
        }
LOGGER.debug("propertiesLayer = " + (propertiesLayer!=null?propertiesLayer.getLayerName():"null"));

        // Temporary layer with only those layer properties so that our changed 
        // rendering (se selection) won't affect original layer
        //
        SVSpatialLayer tLayer = null;
        try {
            tLayer = propertiesLayer.createCopy( );
        } catch (Exception e) {
            LOGGER.error("Error creating copy of propertiesLayer used for rendering geometries");
            return;
        }

        // create for after draw shade operation
        class ShadeAfterDraw extends AfterLayerDraw 
        {
            STRUCT geo;
            List<QueryRow> geoSet;

            boolean selectionColouring = false;
            boolean        initialised = false; // Controls multiple initiation in draw method
            Graphics2D             g2d = null;
            SVSpatialLayer renderLayer = null;
            
            public ShadeAfterDraw(Graphics2D     _g2d,
                                  STRUCT         _geo, 
                                  List<QueryRow> _geoSet,
                                  SVSpatialLayer _renderLayer,
                                  boolean        _selectionColouring)
            {
                super(true);  // true means remove after drawing layer
                this.g2d    = _g2d;
                this.geo    = _geo;
                this.geoSet = _geoSet;
                this.renderLayer = _renderLayer;
                this.selectionColouring = _selectionColouring;
                this.initialised = false;
            }
            
            public void initialize() 
            {
                renderLayer.getStyling().setSelectionActive(this.selectionColouring);
                if ( renderLayer.getStyling().getShadeType() == Styling.STYLING_TYPE.NONE ) {
                    renderLayer.getStyling().setShadeType(Styling.STYLING_TYPE.CONSTANT );
                    renderLayer.getStyling().setShadeTransLevel(0.5f);
                }
                renderLayer.getDrawTools().setGraphics2D(this.g2d);
            }
            
            @Override
            public void draw(Graphics2D _g2d) 
            {
                if ( ! this.initialised ) {
                    this.g2d = _g2d;
                    initialize();
                    this.initialised = true;
                }
                // draw geometry
                try {
                    if ( this.geo != null ) {
                        renderLayer.callDrawFunction(this.geo,"","","","",renderLayer.getStyling().getPointSize(4),0.0f);
                    } else {
                        for (QueryRow geo : this.geoSet) {
                            renderLayer.callDrawFunction(geo.getJGeom(),"","","","",renderLayer.getStyling().getPointSize(4),0.0f);
                        }
                    }
                } catch (Exception _e) {
                    System.out.println("ShadeAfterDraw.Draw Function failed (" + _e.getMessage() + ")");
                }
            }
        }

        // Let everyone know what selection mode we are in for drawing 
        // Is this needed in all cases?
        //
        ShadeAfterDraw shadeAfterClass = new ShadeAfterDraw(mappingView.getMapPanel().getBiG2D(), 
                                                            _geo,
                                                            _geoSet,
                                                            tLayer,
                                                            _selectionColouring);

        // create copy of background image
        //
        mappingView.getMapPanel().createBufferImage();

        // Now draw passed in objects
        //
        if ( zoom ) {
            // Redraw all layers in view then draw geometries
            mappingView.setMBR(mbr);
LOGGER.debug("setMBR - is world2Screen? " + (mappingView.getMapPanel().getWorldToScreenTransform()==null?"null":"ok"));
            mappingView.getMapPanel().getAfterLayerDrawList().add(shadeAfterClass);
            mappingView.getMapPanel().setRedrawBIOnly(MapPanel.REDRAW_BI.IMAGE_SIZE_CHANGE);
            mappingView.getMapPanel().refreshAll();
        } else {
            if ( mbr.overlaps(mappingView.getMBR()) ) {
                // Draw selected objects
                if ( _drawAfter ) {
                  // Redraw all layers in view then draw geometries
                  mappingView.getMapPanel().getAfterLayerDrawList().add(shadeAfterClass);
                  mappingView.getMapPanel().setRedrawBIOnly(MapPanel.REDRAW_BI.IMAGE_SIZE_CHANGE);
                  mappingView.getMapPanel().refreshAll();
                } else {
                  shadeAfterClass.draw(this.activeView.getMapPanel().getBiG2D());
                  // Draw without refreshing any other layers
                  mappingView.getMapPanel().setRedrawBIOnly(MapPanel.REDRAW_BI.IMAGE_QUERY_SHAPE);
                  repaint();
                }
            } else {
                this.setMessage(propertyManager.getMsg("SHOW_GEOMETRY_OUTSIDE_WINDOW"),true);
            }
        }
    }

    /**
     * Set state of toolbar buttons, depend on status in ViewOperationListener
     */
    public void setToolbarStatus() {
        if (this.getActiveView()==null) {            
            this.setToolbarStatus(this.voListener.getSpatialViewOpr(),
                                  null,
                                  null);
        } else {
            this.setToolbarStatus(this.voListener.getSpatialViewOpr(),
                                  this.getMapPanel().windowNavigator,
                                  this.getMapPanel().getWindow());
        }
    }
    
    public void setToolbarEnabled(final boolean _enabled) {
      SwingUtilities.invokeLater(new Runnable() {
           public void run() 
           {
               measureShapeCB.setEnabled(_enabled);
               createShapeCB.setEnabled(_enabled);
               btnQueryByPoint.setEnabled(_enabled);
               btnPrevMBR.setEnabled(_enabled); 
               btnNextMBR.setEnabled(_enabled);
               zoomInButton.setEnabled(_enabled);
               zoomOutButton.setEnabled(_enabled);
               moveButton.setEnabled(_enabled);
               zoomBoundsButton.setEnabled(_enabled);
               zoomLayerButton.setEnabled(_enabled);
               zoomFullButton.setEnabled(_enabled);
               reloadButton.setEnabled(_enabled);
               copyImageButton.setEnabled(_enabled);
           }
      });
    }
    
    public void setToolbarStatus(final ViewOperationListener.VIEW_OPERATION _viewOperation,
                                 final windowNavigator                      _windowNavigator,
                                 final Envelope                      _mbr) 
    {
        SwingUtilities.invokeLater(new Runnable() {
            
            public void setButton(JButton _button,
                                  boolean _enabled,
                                  boolean _selected, 
                                  ImageIcon _pressed,
                                  ImageIcon _normal) 
            {
                _button.setEnabled(_enabled);
                _button.setSelected(_selected);
                _button.setBorderPainted(_selected);
                _button.setIcon(_selected ? _pressed : _normal);
            }

            public void run() 
            {
                // If want to create many objects at once without the inconvenience of having to click the combo box each time
                // ...
                createShapeCB.setEnabled(true);
                if ( ! _viewOperation.toString().startsWith("CREATE_") ) {
                    createShapeCB.setSelectedIndex(0);
                }
                measureShapeCB.setEnabled(true);
                if ( ! _viewOperation.toString().startsWith("MEASURE_") ) {
                    measureShapeCB.setSelectedIndex(0);
                }
                if ( _viewOperation.toString().startsWith("QUERY_") ) {
                    createShapeCB.setEnabled(false);
                    // need to set createShapeCB to desired icon/action
                    String entry = _viewOperation.toString().replace("QUERY_","").toLowerCase();
                    for (int i=0; i<createShapeCB.getItemCount(); i++) {
                        if ( createShapeCB.getItemAt(i).toString().toLowerCase().equals(entry) ) {
                            createShapeCB.setSelectedIndex(i);
                            createShapeCB.setEnabled(true);
                            break;
                        }
                    }
                    createShapeCB.setEnabled(true);                            
                }

                // Now set toolbar buttons according to what is current
                //
                setButton(btnQueryByPoint, true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.QUERY,
                          iconQueryPress, iconQueryNormal);
                setButton(btnPrevMBR, 
                          ((_windowNavigator!=null)?_windowNavigator.hasPrevious():false),
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.MBR_PREV,
                          iconPrevMBRPress, iconPrevMBRNormal);
                setButton(btnNextMBR,
                          ((_windowNavigator!=null)?_windowNavigator.hasNext():false),
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.MBR_NEXT,
                          iconNextMBRPress, iconNextMBRNormal);
                setButton(zoomInButton, true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.ZOOM_IN,
                          iconZoomInPress, iconZoomInNormal);
                setButton(zoomOutButton, true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.ZOOM_OUT,
                          iconZoomOutPress, iconZoomOutNormal);
                setButton(moveButton, true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.MOVE,
                          iconMovePress, iconMoveNormal);
                setButton(zoomBoundsButton, true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.ZOOM_BOUNDS,
                          iconZoomBoundsPress, iconZoomBoundsNormal);
                setButton(zoomLayerButton,true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.ZOOM_LAYER,
                          iconZoomLayerPress, iconZoomLayerNormal);
                setButton(zoomFullButton,true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.ZOOM_FULL,
                          iconZoomFullPress, iconZoomFullNormal);
                setButton(reloadButton, true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.DATA_RELOAD,
                          iconReloadPress, iconReloadNormal);
                setButton(copyImageButton, true,
                          _viewOperation == ViewOperationListener.VIEW_OPERATION.IMAGE_COPY,
                          iconCopyImagePress, iconCopyImageNormal);

                // Save new MBR 
                switch ( _viewOperation  )
                {
                    case        MOVE: // return;
                    case     ZOOM_IN:
                    case    ZOOM_OUT:
                    case ZOOM_BOUNDS:
                    case   ZOOM_FULL: 
                    case  ZOOM_LAYER: if (_windowNavigator!=null) { 
                                          _windowNavigator.add(_mbr); 
                                      }
                                      break;
				default:
					break;
                }
            }
        });
    }

    public ViewOperationListener getVoListener() {
        return voListener;
    }

    protected SpatialView getView(String _viewName) {
        return this.viewLayerTree.getView(_viewName);
    }
    
    public SpatialView getActiveView() {
        return this.activeView == null ? this.viewLayerTree.getActiveView() : this.activeView;
    }

    public MapPanel getMapPanel() {
        return this.getActiveView().getMapPanel();
    }

    public void setActiveLayer(SVSpatialLayer _activeLayer) {
        this.activeView.setActiveLayer(_activeLayer);
    }
 
    public SVSpatialLayer getActiveLayer() {
        return this.getActiveView().getActiveLayer();
    }

    private void oprCancelButton() {
        this.setCancelOperation(true);
    }

    /**
     * Change mouse cursor when user mouse mouse on icon
     * @param e
     */
    private void oprCancelButton_mouseEntered(MouseEvent e) {
        // change status only when data are loaded
        if (this.oprCancelButton.isEnabled() == true) {
            setCursorDefault();
        }
    }

    /**
     * Change mouse cursor when user mouse mouse on icon
     * @param e
     */
    private void oprCancelButton_mouseExited(MouseEvent e) {
        // change status only when data are loaded
        if (this.oprCancelButton.isEnabled() == true) {
            setCursorWait();
        }
    }

    /**
     * Set default cursor
     */
    public void setCursorDefault() {
        SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                });
    }

    /**
     * Set "Wait" cursor
     */
    public void setCursorWait() {
        SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    }
                });
    }

    public synchronized void setCancelOperation(boolean cancelOperation) {
        this.cancelOperation = cancelOperation;
    }

    public synchronized boolean isCancelOperation() {
        return cancelOperation;
    }

    public JSplitPane getMainLayerSplit() {
        return this.mainLayerSplit;
    }

    public JSplitPane getMainSplit() {
        return this.mainSplit;
    }

    /**
     * Show current mouse coordinate on screen
     * @param _x X coordinate
     * @param _y Y coordinate
     * @param _precision number of fraction digits
     */
    public void updateMouseMove(final double _x, 
                                final double _y, 
                                final int _precision) 
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() 
            {
                DecimalFormat dFormat = Tools.getNLSDecimalFormat(_precision,SVPanelPreferences.getGroupingSeparator());
                String labelText = "";
                labelText = String.format(defaultCoordFormat,
                                          dFormat.format(_x),
                                          dFormat.format(_y));
                mousePosLabel.setText(labelText);
            }
        });
    }
    
    public void updateMouseMove(Point2D _coord,
                                int _precision) 
    {
        updateMouseMove(_coord.getX(), _coord.getY(), _precision);
    }

    /**
     * Show current MBR coordinates on screen
     * @param _precision number of fraction digits
     */
    public void updateViewStatusMBR(final Envelope _mbr,
                                    final int             _precision) 
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() 
            {
                if ( _mbr == null || _mbr.isNull() ) {
                    mousePosLabel.setText(defaultCoordText);
                    worldUpperRightIcon.setText(defaultCoordText);
                    worldLowerLeftIcon.setText(defaultCoordText);
                } else {
                    DecimalFormat dFormat = Tools.getDecimalFormatter(_precision,SVPanelPreferences.getGroupingSeparator());
                    String labelText = "";
                    labelText = String.format(defaultCoordFormat, 
                                              dFormat.format(_mbr.minX),
                                              dFormat.format(_mbr.minY)); 
                    worldLowerLeftIcon.setText(labelText);
                    labelText = String.format(defaultCoordFormat, 
                                              dFormat.format(_mbr.maxX),
                                              dFormat.format(_mbr.maxY)); 
                    worldUpperRightIcon.setText(labelText);
              }
            }
        });
    }

    /**
     * No matter where user click - just show dialog for World coordinate input.
     * @param e
     */
    protected void windowCentreMousePressed(MouseEvent e) 
    {
        // create dialog if not exist yet
        if (this.swCenter == null) {
            this.swCenter = new SelectWindowCenter(this);
            this.swCenter.setModal(true);
            this.swCenter.setTitle(this.propertyManager.getMsg("SELECT_WINDOW_CENTER_TITLE"));
            this.swCenter.setSize(350, 120);
        }
        // set init last operation, if MBR is already set
        if (this.activeView.getMapPanel().getWindow().isSet()) {
            // show dialog
            this.swCenter.setLocationRelativeTo(this);
            this.swCenter.init();
            this.swCenter.setVisible(true);

            if (this.swCenter.getLastOpr() == SelectWindowCenter.OPR_OK) {
                // calculate and set new MRR
                this.activeView.getMapPanel().getWindow().moveTo(this.swCenter.getCenter());
                // image redraw
                this.redraw();
            }
        }
    }

    public SelectionView getAttDataView() {
        return this.attDataView;
    }

  private class CreateListRenderer extends DefaultListCellRenderer {
      
	private static final long serialVersionUID = -3031544955500333911L;

	public CreateListRenderer() {
      }
      
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,boolean isSelected, boolean cellHasFocus)
      {
          // Get the renderer component from parent class
          JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          // Get icon to use for the list item value
          String s = (String)value;
          if ( s.equals(lblCreateShapeInactive.getText()) ) {
              label.setIcon(iconCreateShapeInactive);
              label.setToolTipText(propertyManager.getMsg("TT_CREATE_SHAPE"));
          } else if ( s.equals(lblCreatePoint.getText()) ) {
              label.setIcon(iconCreatePointNormal);
              label.setToolTipText(propertyManager.getMsg("TT_CREATE_SHAPE_POINT"));
          } else if ( s.equals(lblCreateMultiPoint.getText()) ) {
              label.setIcon(iconCreateMultiPointNormal);
              label.setToolTipText(propertyManager.getMsg("TT_CREATE_SHAPE_MULTIPOINT"));
          } else if ( s.equals(lblCreateLine.getText()) ) {
              label.setIcon(iconCreateLineNormal);
              label.setToolTipText(propertyManager.getMsg("TT_CREATE_SHAPE_LINE"));
          } else if (  s.equals(lblCreatePolygon.getText()) ) {
              label.setIcon(iconCreatePolygonNormal);
              label.setToolTipText(propertyManager.getMsg("TT_CREATE_SHAPE_POLYGON"));
          } else if ( s.equals(lblCreateCircle.getText()) ) {
              label.setIcon(iconCreateCircleNormal);
              label.setToolTipText(propertyManager.getMsg("TT_CREATE_SHAPE_CIRCLE"));
          } else if ( s.equals(lblCreateRectangle.getText()) ) {
              label.setIcon(iconCreateRectangleNormal);
              label.setToolTipText(propertyManager.getMsg("TT_CREATE_SHAPE_RECTANGLE"));
          } 

          return label;
      }
  }
  
  private class MeasureListRenderer extends DefaultListCellRenderer {
      
	private static final long serialVersionUID = 2967296341827094628L;

	public MeasureListRenderer() {
      }
      
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,boolean isSelected, boolean cellHasFocus)
      {
          // Get the renderer component from parent class
          JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          // Get icon to use for the list item value
          String s = (String)value;
          if ( s.equals(lblMeasureInactive.getText()) ) {
              label.setIcon(iconMeasureInactiveNormal);
              label.setToolTipText(propertyManager.getMsg("TT_MEASURE_SHAPE"));
          } else if (  s.equals(lblMeasureLine.getText()) ) {
                label.setIcon(iconMeasureDistanceNormal);
                label.setToolTipText(propertyManager.getMsg("TT_MEASURE_SHAPE_LINE"));
          } else if (  s.equals(lblMeasurePolygon.getText()) ) {
              label.setIcon(iconMeasurePolygonNormal);
              label.setToolTipText(propertyManager.getMsg("TT_MEASURE_SHAPE_POLYGON"));
          } else if ( s.equals(lblMeasureCircle.getText()) ) {
              label.setIcon(iconMeasureCircleNormal);
              label.setToolTipText(propertyManager.getMsg("TT_MEASURE_SHAPE_CIRCLE"));
          } else if ( s.equals(lblMeasureRectangle.getText()) ) {
              label.setIcon(iconMeasureRectangleNormal);
              label.setToolTipText(propertyManager.getMsg("TT_MEASURE_SHAPE_RECTANGLE"));
          } 
          return label;
      }
  }

}
