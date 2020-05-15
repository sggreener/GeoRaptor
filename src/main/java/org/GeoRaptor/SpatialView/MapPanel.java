package org.GeoRaptor.SpatialView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import java.sql.SQLException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import oracle.jdbc.OracleConnection;

import oracle.jdeveloper.layout.VerticalFlowLayout;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.ScaleBar;
import org.GeoRaptor.SpatialView.SupportClasses.ViewOperationListener;
import org.GeoRaptor.SpatialView.layers.SVGraphicLayer;
import org.GeoRaptor.SpatialView.layers.SVQueryLayer;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayer;
import org.GeoRaptor.SpatialView.layers.SpatialQueryReview;
import org.GeoRaptor.SpatialView.layers.Styling;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.MathUtils;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;


/**
 * Main class for map.
 */
@SuppressWarnings("deprecation")
public class MapPanel 
     extends JPanel 
  implements MouseListener, 
             MouseMotionListener,
             MouseWheelListener, 
             KeyListener 
{
	private static final long serialVersionUID = -5077924222660508572L;

	/**
     * Reference to main class
     */
    protected SpatialView spatialView;

    /**
     * Reference to preferences
     */
    protected static Preferences mainPrefs;

    /** 
     * Properties File Manager
     **/
    private static final String propertiesFile = "org.GeoRaptor.SpatialView.resource.MapPanel";
    protected PropertiesManager propertyManager = null;

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.MapPanel");

    /**
     * Graphics 
     */
    protected Graphics2D                       biG2D;
    protected AffineTransform worldToScreenTransform;
    protected BufferedImage                  biImage, 
                                             biBasic;
    protected Dimension                           bi;  // Size of buffered image in pixels

    /** 
     * Each view's mapPanel maintains a set of navigation MBRs in the view SRID's coordinate system
     */
    protected windowNavigator windowNavigator  = null;

    /**
     * Screen size of map panel
     */
    protected Envelope   window = null;
    protected Dimension     clientView = null;
    protected Point2D      startScreen = null, 
                         currentScreen = null,
                            startWorld = null;

    protected double              zoomFactor = 1.0;
    protected static final double moveFactor = 0.1;

    protected boolean switchDOFState = false;

    protected Color mapBackground = Color.WHITE;
    protected ScaleBar   scaleBar = null;

    // For IMAGE_MEASURE_SHAPE length
    protected Path2D.Double createShapeWorld  = null;
    protected Path2D.Float  createShapeScreen = null;
    protected Point2D              lastScreen = null;

    private String   ERROR_SCREEN2WORLD_TRANSFORM = "(MapPanel): Problem computing screenToWorld transformation.";
    private String  STATUS_RECTANGLE_CIRCLE_START = null;
    private String           STATUS_MEASURE_START = null;
    private String             STATUS_MEASURE_END = null;
    protected String            MAPEXTENT_NOT_SET = "Map extent not set";
    protected String           NO_DRAWABLE_LAYERS = "No drawable layers.";
    private String                  MAP_MENU_JUMP = "Jump to new XY Location.";
    private String              MAP_MENU_COPY_MBR = "Copy Map Extent to Clipboard.";
    private String           MAP_MENU_COPY_CENTRE = "Copy Map Centre Point to Clipboard.";
    private String         MAP_MENU_PROJECT_POINT = "Copy Map Point to Clipboard as Lat/Long (4326).";
    private String              MAP_MENU_XY_POINT = "Copy Map Point to Clipboard.";
    
    protected enum RENDER_HINTS { NORMAL,   /* VALUE_RENDER_DEFAULT */
                                  QUALITY,  /* VALUE_RENDER_QUALITY */
                                  SPEED };  /* VALUE_RENDER_SPEED */
    protected RENDER_HINTS     renderHint = RENDER_HINTS.SPEED;
    protected RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                                                               RenderingHints.VALUE_ANTIALIAS_ON);
     
    /**
     * When user select redraw operation, this timer start draw operation.
     */
    protected Timer drawTimer;

    public enum REDRAW_BI {
            /**
             * Default value - when we do not perform this operation
             */
            UNDEFINE,
            /**
             * Redraw buffered image and finish paint method
             */
            FINISH,
            /**
             * Only redraw buffered image
             */
            ONLY_PAINT,
            /**
             * Size of image change. Create new buffered image.
             * ie Request to redraw all layers using current map extent.
             */
            IMAGE_SIZE_CHANGE,
            /**
             * User perform mouse move operation or window resize
             */
            IMAGE_MOUSE_MOVE,
            /**
             * User perform zoom bounds operation
             */
            IMAGE_ZOOM_BOUNDS,
            /**
             * IMAGE_MEASURE_SHAPE Area/Length
             */
            IMAGE_MEASURE_SHAPE,
            /** 
             * IMAGE_CREATE_SHAPE SDO_Geometry for clipboard 
             */
            IMAGE_CREATE_SHAPE,
            /** 
             * IMAGE_QUERY_SHAPE - circular shape being drawn as mouse moves
             */
            IMAGE_QUERY_SHAPE,
            /** 
              * IMAGE_ZOOM_GEOMETRY - SpatialViewPanel zoom to ascetate geometry (don't check if existing layers exist and are drawable)
              */            
            IMAGE_ZOOM_GEOMETRY
            };

    /**
     * Refresh buffered image only when "paintComponent" method is called.
     *
     */
    REDRAW_BI redrawBIOnly = REDRAW_BI.UNDEFINE;

    /**
     * List of classes with additional draw object logic. For example : shade select object after image draw.
     */
    protected ArrayList<AfterLayerDraw> afterLayerDrawList = new ArrayList<AfterLayerDraw>();

    protected String        noQueryableLayer = null;
    protected ImageIcon iconNoQueryableLayer = null; 
    protected ImageIcon      iconMenuCopyMbr = null;
    protected ImageIcon   iconMenuCopyCentre = null;
    protected ImageIcon  iconMenuProjectPoint = null; 
    protected ImageIcon      iconMenuXYPoint = null; 
    protected ImageIcon         iconJump2MBR = null;
    protected JLabel messageNoQueryableLayer = null;

    private MapPanel() 
    {
        // Instantiate preferences
        //
        MapPanel.mainPrefs = Preferences.getInstance();            
       
        try {
            // Get localisation file
            //
            this.propertyManager = new PropertiesManager(MapPanel.propertiesFile);
            
            this.ERROR_SCREEN2WORLD_TRANSFORM  = this.propertyManager.getMsg("ERROR_SCREEN2WORLD_TRANSFORM");
            this.noQueryableLayer              = this.propertyManager.getMsg("QUERY_NO_LAYER_SELECTED");
            this.STATUS_RECTANGLE_CIRCLE_START = this.propertyManager.getMsg("STATUS_RECTANGLE_CIRCLE_START");
            this.STATUS_MEASURE_START          = this.propertyManager.getMsg("STATUS_MEASURE_START");
            this.STATUS_MEASURE_END            = this.propertyManager.getMsg("STATUS_MEASURE_END");
            this.MAPEXTENT_NOT_SET             = this.propertyManager.getMsg("MAPEXTENT_NOT_SET");
            this.NO_DRAWABLE_LAYERS            = this.propertyManager.getMsg("NO_DRAWABLE_LAYERS");
            this.MAP_MENU_JUMP                 = this.propertyManager.getMsg("MAP_MENU_JUMP");
            this.MAP_MENU_COPY_MBR             = this.propertyManager.getMsg("MAP_MENU_COPY_MBR");
            this.MAP_MENU_COPY_CENTRE          = this.propertyManager.getMsg("MAP_MENU_COPY_CENTRE");
            this.MAP_MENU_PROJECT_POINT        = this.propertyManager.getMsg("MAP_MENU_PROJECT_POINT");
            this.MAP_MENU_XY_POINT             = this.propertyManager.getMsg("MAP_MENU_XY_POINT");

            this.iconNoQueryableLayer          = new ImageIcon(getClass().getClassLoader().getResource("org/GeoRaptor/SpatialView/images/no_queryable_layer.png"));
            this.iconMenuCopyMbr               = new ImageIcon(getClass().getClassLoader().getResource("org/GeoRaptor/SpatialView/images/layer_menu_set_mbr.png"));
            this.iconMenuCopyCentre            = new ImageIcon(getClass().getClassLoader().getResource("org/GeoRaptor/SpatialView/images/geometry_type_point.png"));
            this.iconMenuProjectPoint          = new ImageIcon(getClass().getClassLoader().getResource("org/GeoRaptor/SpatialView/images/project_xy_to_geodetic.png"));
            this.iconMenuXYPoint               = new ImageIcon(getClass().getClassLoader().getResource("org/GeoRaptor/SpatialView/images/xy_to_geometry.png"));
            this.iconJump2MBR                  = new ImageIcon(getClass().getClassLoader().getResource("org/GeoRaptor/SpatialView/images/layer_menu_move_view.png"));
            this.messageNoQueryableLayer       = new JLabel(noQueryableLayer, iconNoQueryableLayer, JLabel.TRAILING);

        } catch (Exception e) {
            System.out.println("(MapPanel Constructor): Problem loading properties file (" + MapPanel.propertiesFile + ")\n" + e.getMessage());
        }

        // Get map background color
        //
        Color mBackground = (MapPanel.mainPrefs==null || 
                             MapPanel.mainPrefs.getMapBackground() == null )
                             ? Color.WHITE 
                             : MapPanel.mainPrefs.getMapBackground();
        this.setMapBackground(mBackground);
        // Create scalebar class and set its background
        this.scaleBar = new ScaleBar(this.mapBackground);
        
        //  The constructor for RectangleDouble sets values to Double.MAX_VALUE/Double.MIN_VALUE
        //
        this.window        = new Envelope(Constants.DEFAULT_PRECISION);
        this.startScreen   = new Point.Double(0,0); 
        this.currentScreen = new Point.Double(0,0); 
        this.startWorld    = new Point.Double(0,0);
        this.bi            = new Dimension(0,0);
        this.clientView    = new Dimension(0,0);
        this.lastScreen    = new Point.Double(0,0);
        
        this.windowNavigator = new windowNavigator(MapPanel.mainPrefs.getMBRSaveSize()); 
        // Set up default rendering hint
        qualityHints.put(RenderingHints.KEY_RENDERING,
                         RenderingHints.VALUE_RENDER_DEFAULT);

        try {
            jbInit();
        } catch (Exception e) {
            LOGGER.error("(MapPanel Constructor): Exception Caught (jbInit)");
        }
        
    }

    public MapPanel(SpatialView _spatialView) {
        this(); // call empty constructor first
        this.spatialView = _spatialView;
        this.window      = new Envelope(this.spatialView.getDefaultPrecision());
    }

    private void jbInit() 
     throws Exception {
        this.setBorder(BorderFactory.createLineBorder(Color.black, 1));
    }

    @SuppressWarnings("unused")
	private Preferences getMainPreferences()
    {
        if (MapPanel.mainPrefs==null) {
            MapPanel.mainPrefs = Preferences.getInstance(); 
        }
        return MapPanel.mainPrefs;
    }

    public Color getMapBackground() {
      return this.mapBackground;
    }
    
    public void setMapBackground(String _color) {
        LOGGER.debug("MapPanel.setMapBackground(String " + _color==null?"NULL":_color.toString());
        if (Strings.isEmpty(_color) ) {
            return;
        }
        Color tempColor;
        if ( _color.contains(",") ) {
            tempColor = Colours.stringToColor(_color);
        } else {
            tempColor = new Color(Integer.valueOf(_color));
        }
        this.setMapBackground(tempColor); // sets scalebar, background image etc.
    }
    
    public void setMapBackground(Color _color) {
        LOGGER.debug("MapPanel.setMapBackground(Color " + _color==null?"NULL":_color.toString());
        this.mapBackground = _color;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setBackground( mapBackground ); 
            }
        });
        if ( this.scaleBar != null ) {
           this.scaleBar.setBackground(this.mapBackground);
        }
    }

    public Color getFeatureColour() {
        return MapPanel.mainPrefs.getFeatureColour() == null 
               ? new Color(Constants.VAL_FEATURE_COLOUR ) 
               : MapPanel.mainPrefs.getFeatureColour();
    }
    
    public Point getScreenCenter() {
      return new Point(getDimension().width/2,
                       getDimension().height/2);
    }
    
    /** 
     * @function getDimensions
     * @author Simon Greener April 2010
     *          Made available for calculation of transformations.
     **/
    public Dimension getDimension() {
        return this.getSize();
    }

    public boolean isWorldToScreenSet() {
        return (this.worldToScreenTransform!=null);
    }
    
    public AffineTransform getWorldToScreenTransform() 
    {
        return this.worldToScreenTransform;
    }
    
    /**
     * @method setWorldToScreenTransform()
     * @author Simon Greener, April 2010, Added to support java.awt.shape rendering.
    public void setWorldToScreenTransform() 
    {
        if ( this.window.isSet() ) {
            setWorldToScreenTransform( this.window.minX, 
                                       this.window.minY,
                                       this.window.maxX,
                                       this.window.maxY,
                                       this.getDimension() );
        }
    }
     */

    /**
     * @method worldToScreenTransform(RectangleDouble, Dimension)
     * @author Simon Greener, April 2010, Added to support java.awt.shape rendering.
     */
    public void setWorldToScreenTransform(Envelope _mapExtent,
                                                Dimension _screenSize) 
    {
        setWorldToScreenTransform( _mapExtent.minX,
                                   _mapExtent.minY,
                                   _mapExtent.maxX,
                                   _mapExtent.maxY, 
                                   _screenSize);
    }

    /**
     * @method  setWorldToScreenTransform(double,double,double,dluble,Dimension)
     * @author Simon Greener, April 2010, Added to support java.awt.shape rendering.
     * 
     */
    public void setWorldToScreenTransform(double _minX,
                                          double _minY,
                                          double _maxX,
                                          double _maxY, 
                                          Dimension _screenSize) 
    {
        // LOGGER.info(String.format("%s: setWorldToScreenTransform(%f,%f,%f,%f)",this.spatialView.getViewName(),_minX,_minY,_maxX,_maxY));
        // LOGGER.info("(_maxX - _minX)="+(_maxX - _minX) + " (_maxY - _minY)=" + (_maxY - _minY));
        // LOGGER.info("_screenSize.getWidth()="+_screenSize.getWidth()+" _screenSize.getHeight()="+_screenSize.getHeight());
        if ( _screenSize.getWidth()  == Double.NaN || 
             _screenSize.getWidth()  == Double.MAX_VALUE || 
             _screenSize.getWidth()  == 0.0f || 
             _screenSize.getHeight() == Double.NaN || 
             _screenSize.getHeight() == Double.MAX_VALUE ||
             _screenSize.getHeight()  == 0.0f ) {
            LOGGER.debug("Can't set world to page due to screen not yet set.");
            return ;
        }
        double scaleX = _screenSize.getWidth()  / (_maxX - _minX);
        double scaleY = _screenSize.getHeight() / (_maxY - _minY);
        // LOGGER.info("_scaleX="+scaleX+" _scaleY="+scaleY);
        double tx = - (_minX * scaleX);
        double ty =   (_minY * scaleY) + _screenSize.getHeight();
        this.worldToScreenTransform = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
    }

    /**
     * @method worldToPixel
     * @precis Gets position of a world coordinate point in screen coordintes
     * @param _coord
     * @return
     * @author Simon Greener, June 2nd 2010
     *          Original Coding.
     */
    public java.awt.Point worldToPixel( Point2D _coord ) 
    {
        if ( this.worldToScreenTransform == null ) {
            return new Point(0,0); // or null?
        }
        AffineTransform at = this.worldToScreenTransform;
        Point2D p = at.transform(_coord, new Point2D.Double());
        return new java.awt.Point((int) p.getX(), (int) p.getY());
    }

    /**
     * @method pixelToWorld
     * @precis Gets position of a screen coordinate in world coordinates
     * @param _pixel
     * @return
     * @author Simon Greener, June 2nd 2010
     *          Original Coding.
     */
    public Point2D pixelToWorld ( Point.Double _pixel ) 
    throws NoninvertibleTransformException
    {
        if ( this.worldToScreenTransform == null ) {
            return new Point.Double(0,0); // or null?
        }
        AffineTransform atPixelToWorld = this.worldToScreenTransform.createInverse();
        Point2D p = atPixelToWorld.transform(_pixel,null);
        return new Point.Double(p.getX(), p.getY());
    }

    /**
     * Convert Screen coordinate to World 
     * @param _pixel screen coordinate (x,y)
     * @return world coordinate
     */
    public Point2D ScreenToWorld(Point.Double _pixel) 
    throws NoninvertibleTransformException
    {
        return pixelToWorld(_pixel);
    }

    public Point2D ScreenToWorld(Point2D _pixel) 
    throws NoninvertibleTransformException
    {
        if (Double.isNaN(_pixel.getX()) || Double.isNaN(_pixel.getY())) {
            return new Point.Double(0,0); // or null?
        }
        return pixelToWorld(new Point.Double(_pixel.getX(),_pixel.getY()));
    }
    
    /**
     * @method getPixelSize()
     * @precis Gets size of one pixel in world units
     * @return
     * @author Simon Greener, June 2nd 2010
     *          Original Coding.
     */
    public Point2D.Double getPixelSize() 
    throws NoninvertibleTransformException
    {
        Point2D pixelBase = this.pixelToWorld(new Point.Double(1,1));
        Point2D pixelX    = this.pixelToWorld(new Point.Double(2,1));
        Point2D pixelY    = this.pixelToWorld(new Point.Double(1,2));
        return new Point2D.Double(Math.abs(pixelX.getX()-pixelBase.getX()),
                                  Math.abs(pixelY.getY()-pixelBase.getY()));
    }
    
    /**
     * Create new BufferedImage base on current screen settings
     */
    public BufferedImage getCompatibleBI(Dimension _d) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
        return gc.createCompatibleImage(_d.width, _d.height);
    } // getCompatibleBI

    /**
     * This is only for no curved (ie vector stroked) Path2D
     * @param _finalMeasureLine
     * @return
     * @method @method
     * @author @author
     */
    private int getCoordCount(final Path2D _finalMeasureLine) {
      {
        int coordCount = 0;
        if (_finalMeasureLine!=null) 
        {
            try 
            {
                PathIterator pi = _finalMeasureLine.getPathIterator(null);
                double[] coords     = new double[6];
                double[] prevCoords = new double[6];  // We do not count adjacent coords 
                while (pi.isDone() == false) 
                {
                    switch (pi.currentSegment(coords)) 
                    {
                      case PathIterator.SEG_MOVETO:
                        coordCount++;
                        break;
                      case PathIterator.SEG_LINETO:
                          // SEG_LINETO returns one point,
                          // printScreenArray("SEG_LINETO: ",coords,2,1);                      
                          // Don't get coordinates if same as previous
                          //
                          if ( !(prevCoords[0] == coords[0] &&
                                 prevCoords[1] == coords[1] ) )
                          {
                            coordCount++;
                          }
                          break;
                      case PathIterator.SEG_CLOSE:
                          coordCount++;
                          break;
                      case PathIterator.SEG_QUADTO:
                      case PathIterator.SEG_CUBICTO:
                      default:
                        break;
                    }
                    pi.next();
                    if ( ! pi.isDone() )
                        prevCoords = coords.clone();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return coordCount;
      }
      
    }
    
    private JGeometry getGeometryFromShape(final Path2D.Double                        _finalShape,
                                           final ViewOperationListener.VIEW_OPERATION _svo) 
    {
        if (_finalShape==null)
            return null;
        
        boolean bLine = ( _svo==ViewOperationListener.VIEW_OPERATION.MEASURE_DISTANCE  ||
                          _svo==ViewOperationListener.VIEW_OPERATION.CREATE_LINE       ||
                          _svo==ViewOperationListener.VIEW_OPERATION.QUERY_LINE         );
        boolean bArea = ( _svo==ViewOperationListener.VIEW_OPERATION.CREATE_POLYGON    ||
                          _svo==ViewOperationListener.VIEW_OPERATION.MEASURE_POLYGON   ||
                          _svo==ViewOperationListener.VIEW_OPERATION.QUERY_POLYGON     ||
                          _svo==ViewOperationListener.VIEW_OPERATION.MEASURE_RECTANGLE ||
                          _svo==ViewOperationListener.VIEW_OPERATION.QUERY_RECTANGLE   ||
                          _svo==ViewOperationListener.VIEW_OPERATION.MEASURE_CIRCLE    ||
                          _svo==ViewOperationListener.VIEW_OPERATION.QUERY_CIRCLE       );
        boolean bMultiPoint = _svo==ViewOperationListener.VIEW_OPERATION.CREATE_MULTIPOINT;
        
        // Need number of coordinates to allocate ordinate array memory
        //
        int numCoords = getCoordCount(_finalShape);
        
        // No point going on if not enough points for required shape
        //
        if ( ( numCoords == 0 && bMultiPoint ) ||
             ( numCoords  < 2 && bLine ) ||  
             ( numCoords  < 3 && bArea ) )
            return null;

        int gtype = 2000 + ( bLine ? JGeometry.GTYPE_CURVE 
                         : ( bArea ? JGeometry.GTYPE_POLYGON : JGeometry.GTYPE_MULTIPOINT )); 
        int[] elemInfo =   ( bLine ? new int[] {1,2,1} 
                         : ( bArea ? new int[] {1,1003,1} : new int[] {1,1,numCoords}) );
        
        // Now get coordinates
        //
        double[] ordinates = new double[numCoords*2];
        int ordCount = 0;
        try 
        {
          PathIterator pi = _finalShape.getPathIterator(null);
          double[] coords      = new double[6];
          double[] prevCoords  = new double[6];
          while (pi.isDone() == false) 
          {
              switch (pi.currentSegment(coords)) {
              case PathIterator.SEG_MOVETO:
                  // SEG_MOVETO returns one point,
                  // load ordinate array
                  ordinates[ordCount++] = coords[0];
                  ordinates[ordCount++] = coords[1];
                  // printScreenArray("SEG_MOVETO: ",startCoords,2,1);
                  break;
              case PathIterator.SEG_LINETO:
                  // SEG_LINETO returns one point,
                  // printScreenArray("SEG_LINETO: ",coords,2,1);                      
                  // Don't get coordinates if same as previous
                  //
                  if ( !(prevCoords[0] == coords[0] &&
                         prevCoords[1] == coords[1] ) )
                  {
                    ordinates[ordCount++] = coords[0];
                    ordinates[ordCount++] = coords[1];
                  }
                break;
              case PathIterator.SEG_CLOSE:
                  /* SEG_CLOSE does not return any points. */
                  ordinates[ordCount++] = ordinates[0];
                  ordinates[ordCount++] = ordinates[1];
              case PathIterator.SEG_QUADTO:  /* Whilst these two could occur, in this "GIS" situation they won't */
              case PathIterator.SEG_CUBICTO:
              default:
                  break;
              }
              pi.next();
              if ( ! pi.isDone() )
                  prevCoords = coords.clone();
          }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( ordCount != (numCoords*2) )
            return null;
        return new JGeometry(gtype,
                             this.spatialView.getSRIDAsInteger(),
                             elemInfo,
                             ordinates);
    }
    
    @SuppressWarnings("unused")
	private double getDistance(final Path2D _finalMeasureLine) 
    {
      double distance = 0.0f;
      if (_finalMeasureLine!=null) 
      {
          try 
          {
            PathIterator pi = _finalMeasureLine.getPathIterator(null);
            double[] coords      = new double[6];
            double[] startCoords = new double[6];
            double[] prevCoords  = new double[6];
                while (pi.isDone() == false) 
                {
                    switch (pi.currentSegment(coords)) 
                    {
                      case PathIterator.SEG_MOVETO:
                          // SEG_MOVETO returns one point,
                          prevCoords = coords.clone();
                          startCoords = coords.clone();
                          break;
                      case PathIterator.SEG_LINETO:
                          distance += Math.sqrt( Math.pow(coords[0]-prevCoords[0],2.0f) +
                                                 Math.pow(coords[1]-prevCoords[1],2.0f) );
                          prevCoords = coords.clone();
                          break;
                      case PathIterator.SEG_QUADTO:
                      case PathIterator.SEG_CUBICTO:
                          break;
                      case PathIterator.SEG_CLOSE:
                          distance += Math.sqrt( Math.pow(coords[0]-startCoords[0],2.0f) +
                                                 Math.pow(coords[1]-startCoords[1],2.0f) );
                          break;
                      default:
                        break;
                    }
                    pi.next();
                    if ( ! pi.isDone() )
                      prevCoords = coords.clone();
                }
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
      return distance;
    }

    private void computeAreaLength(final JGeometry                            _geom,
                                   final ViewOperationListener.VIEW_OPERATION _svo) 
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DecimalFormat dFormat = Tools.getDecimalFormatter(spatialView.getPrecision(false),false);

                Constants.MEASURE measureType = _svo==ViewOperationListener.VIEW_OPERATION.MEASURE_DISTANCE
                                                ? Constants.MEASURE.LENGTH
                                                : Constants.MEASURE.AREA;
                double dMeasures[] = Tools.computeAreaLength(_geom,
                                                            measureType,
                                                            spatialView);
                String finalMessage = "";
                if ( measureType == Constants.MEASURE.AREA || measureType == Constants.MEASURE.BOTH ) {
                  finalMessage = propertyManager.getMsg("MEASURE_AREA_RESULT",
                                                        dFormat.format(dMeasures[0]).replace(String.valueOf(dFormat.getDecimalFormatSymbols().getGroupingSeparator()),""),
                                                        Strings.TitleCase(Tools.getViewUnits(spatialView,ViewOperationListener.VIEW_OPERATION.MEASURE_POLYGON).replace("_"," ")));
                }
                if ( measureType == Constants.MEASURE.LENGTH || measureType == Constants.MEASURE.BOTH ) {
                  finalMessage += propertyManager.getMsg("MEASURE_DISTANCE_RESULT",
                                                         dFormat.format(dMeasures[1]).replace(String.valueOf(dFormat.getDecimalFormatSymbols().getGroupingSeparator()),""),
                                                         Strings.TitleCase(Tools.getViewUnits(spatialView,ViewOperationListener.VIEW_OPERATION.MEASURE_DISTANCE).replace("_"," ")));
                }            
                spatialView.getSVPanel().setMessage(finalMessage,false);
            }
        });
    }
    
    /**
     * Called when MEASURE_* or CREATE_* button pressed or QUERY_* function
     * Set SpatialViewPanel.measureButton() method
     * @method @method
     * @author @author
     */
    public void initialiseShapeActions() 
    {
      this.createShapeWorld  = new Path2D.Double(Path2D.WIND_NON_ZERO);
      this.createShapeScreen = new Path2D.Float(Path2D.WIND_NON_ZERO);
      this.startScreen.setLocation(Double.NaN,Double.NaN);
      this.startWorld.setLocation(Double.NaN,Double.NaN);
      // So that the mouse move code doesn't think a starting point has already been entered
      this.lastScreen.setLocation(Double.NaN,Double.NaN);
      switch ( this.spatialView.getSVPanel().getVoListener().getSpatialViewOpr() ) {
        case CREATE_RECTANGLE  :
        case MEASURE_RECTANGLE :
        case QUERY_RECTANGLE   :
        case CREATE_CIRCLE     :
        case MEASURE_CIRCLE    :
        case QUERY_CIRCLE      :
            this.spatialView.getSVPanel().setMessage(this.STATUS_RECTANGLE_CIRCLE_START,false);
            break;
        default:
            this.spatialView.getSVPanel().setMessage(this.STATUS_MEASURE_START,false);
      }
    }
    
    public void mouseEntered(MouseEvent e) {
        // remove focus to this window
        requestFocusInWindow();
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) 
    {
        ViewOperationListener.VIEW_OPERATION svo = this.spatialView.getSVPanel().getVoListener().getSpatialViewOpr();
        LOGGER.debug("mousePressed: " + svo.toString());
        try {
            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) 
            {
                LOGGER.debug("mousePressed: BUTTON1_MASK is LEFT MOUSE BUTTON");
                Stroke oldStroke = this.getBiG2D().getStroke();
                switch (svo)
                {
                    case MOVE:
                        LOGGER.debug("mousePressed: VIEW_OPERATION.MOVE");
                        // Get Screen pixels where user clicked
                        //
                        this.startScreen.setLocation(e.getX(),e.getY());            
                        this.startWorld.setLocation(this.ScreenToWorld(this.startScreen));
                        this.redrawBIOnly = REDRAW_BI.IMAGE_MOUSE_MOVE;
                        break;

                    case CREATE_RECTANGLE :
                    case QUERY_RECTANGLE : 
                    case MEASURE_RECTANGLE:
                    case CREATE_CIRCLE :
                    case QUERY_CIRCLE : 
                    case MEASURE_CIRCLE:
                    case QUERY:                

                        // Only need to record startScreen and World
                        // Get Screen pixels where user clicked
                        //
                        this.startScreen.setLocation(e.getX(),e.getY());            
                        // Convert screenPixels into world coordinates
                        //
                        this.startWorld.setLocation(this.ScreenToWorld(this.startScreen));
                        break;
                
                    // For Area we will use the General Paths created and build during MEASURE_LINE operations
                    // in a final Area() constructor from which area is calculated
                    //
                    case QUERY_POINT :
                    case CREATE_POINT :
                        // create copy of background image
                        this.createBufferImage();
                        this.startScreen.setLocation(e.getX(),e.getY());            
                        this.startWorld.setLocation(this.ScreenToWorld(this.startScreen));                
                        this.getBiG2D().setColor(this.getFeatureColour());
                        this.getBiG2D().setStroke(new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                        this.getBiG2D().draw(PointMarker.cross(this.startScreen, 8, 0 ));
                        this.getBiG2D().setStroke(oldStroke);                    
                        final JGeometry geometry = new JGeometry(this.startWorld.getX(), this.startWorld.getY(),spatialView.getSRIDAsInteger());
                        if (svo==ViewOperationListener.VIEW_OPERATION.CREATE_POINT) {
                            presentShapeForFinalReview(geometry);
                        } else {
                            presentSpatialSQLForFinalReview(geometry);
                        }
                        // End of this operation
                        this.initialiseShapeActions();
                        // repaint leaving point on screen till next action
                        this.setRedrawBIOnly(REDRAW_BI.FINISH);  // After redraw, FINISH will become UNDEFINED
                        // repaint screen image
                        repaint();
                        return;

                    case CREATE_MULTIPOINT :
                    case QUERY_MULTIPOINT :
                          // create copy of background image
                            if ( ( Double.isNaN(this.lastScreen.getX()) && 
                                   Double.isNaN(this.lastScreen.getY()) ) )
                                this.createBufferImage();
                    case MEASURE_POLYGON:
                    case MEASURE_DISTANCE:
                    case CREATE_LINE :
                    case CREATE_POLYGON :
                    case QUERY_LINE :
                    case QUERY_POLYGON :
                        // Get current position
                        //
                        this.currentScreen.setLocation(e.getX(),e.getY());
                        Point2D currentWorld = this.ScreenToWorld(this.currentScreen);

                        if ( ( Double.isNaN(this.lastScreen.getX()) && 
                               Double.isNaN(this.lastScreen.getY()) ) )
                        {
                          this.startScreen.setLocation(e.getX(), e.getY());
                          this.startWorld.setLocation(currentWorld.getX(),currentWorld.getY());
                          createShapeWorld = new Path2D.Double(Path2D.Double.WIND_NON_ZERO);
                          createShapeScreen = new Path2D.Float(Path2D.Float.WIND_NON_ZERO);
                          createShapeWorld.moveTo(this.startWorld.getX(),this.startWorld.getY());
                          createShapeScreen.moveTo(this.startScreen.getX(),this.startScreen.getY());
                          
                          // Record fact that the shape has commenced
                          //
                          this.lastScreen.setLocation(this.startScreen.getX(),this.startScreen.getY());

                        } else {
                          // We have the next point in the shape, transform to world position
                          //
                          createShapeWorld.lineTo(currentWorld.getX(),currentWorld.getY());
                          createShapeScreen.lineTo(this.currentScreen.getX(),this.currentScreen.getY());

                          // Record for use with right mouse click finish
                          //
                          this.lastScreen.setLocation(this.currentScreen.getX(),this.currentScreen.getY());
                        }
                    
                        // Draw MultiPoint, Line or Area
                        //
                        this.getBiG2D().setColor(this.getFeatureColour());
                    
                        switch ( svo ) {
                          case CREATE_MULTIPOINT:
                          case QUERY_MULTIPOINT:
                                this.getBiG2D().setStroke(new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                                this.getBiG2D().draw(PointMarker.cross(this.currentScreen, 8, 0 ));
                                this.getBiG2D().setStroke(oldStroke);
                              break;
                          case MEASURE_POLYGON :
                          case CREATE_POLYGON :
                          case QUERY_POLYGON :
                              if ( getCoordCount(createShapeScreen)>2 ) { 
                                  Composite oldAlpha = this.getBiG2D().getComposite();
                                  this.getBiG2D().setComposite( Styling.getAlphaComposite(0.3f) );
                                  this.getBiG2D().fill(new Area(createShapeScreen));
                                  this.getBiG2D().setComposite(oldAlpha);
                              }
                          case MEASURE_DISTANCE:
                          case CREATE_LINE:
                          case QUERY_LINE:
                              // Draw line last (might be as boundary of above area
                              this.getBiG2D().draw(createShapeScreen);
                              break;
						default:
							break;
                        }
                        // Display "left click next/right to finish" message
                        //
                        this.spatialView.getSVPanel().setMessage(this.STATUS_MEASURE_END,false);
                        // set only show buffered image
                        //
                        if ( svo.toString().startsWith("CREATE") ||
                             svo.toString().startsWith("QUERY") ) 
                            setRedrawBIOnly(REDRAW_BI.IMAGE_CREATE_SHAPE);
                        else
                            setRedrawBIOnly(REDRAW_BI.IMAGE_MEASURE_SHAPE);
                        this.refreshAll();
                        break;
                
                    case ZOOM_BOUNDS:
                        this.startScreen.setLocation(e.getX(),e.getY()); 
                        this.startWorld.setLocation(this.ScreenToWorld(this.startScreen));
                        this.setRedrawBIOnly(REDRAW_BI.IMAGE_ZOOM_BOUNDS);
                        break;
                
                    case ZOOM_IN :
                        this.startScreen.setLocation(e.getX(),e.getY());            
                        this.startWorld.setLocation(this.ScreenToWorld(this.startScreen));
                        this.window.moveTo(this.startWorld);
                        // windows doesn't need normalising before use
                        Zoom(org.GeoRaptor
                            .SpatialView
                            .SupportClasses
                            .Envelope
                            .zoom
                            .IN);
                        this.refreshAll();
                        break;
                
                    case ZOOM_OUT :
                        this.startScreen.setLocation(e.getX(),e.getY());            
                        this.startWorld.setLocation(this.ScreenToWorld(this.startScreen));                
                        this.window.moveTo(this.startWorld);  
                        // windows doesn't need normalising before use
                        Zoom(org.GeoRaptor
                            .SpatialView
                            .SupportClasses
                            .Envelope
                            .zoom
                            .OUT);
                        this.refreshAll();
                        break;
				default:
					break;
                }
            }
        } catch (NoninvertibleTransformException nte) {
            if ( this.spatialView.getLayerCount()!=0 && 
                 this.spatialView.getMBR().isInvalid()==false) {
                LOGGER.warn("(MapPanel.mousePressed)" +this.ERROR_SCREEN2WORLD_TRANSFORM + "\n" + nte.getLocalizedMessage());
            }
        }
    }

    public void mouseMoved(MouseEvent e) 
    {
        // Get position where clicked on screen 
        //
        // Previously Point2D currentScreen = new Point.Double(e.getX(),e.getY())
        // LOGGER.info(this.worldToScreenTransform==null?"this.worldToScreenTransform is null" : "this.worldToScreenTransform is not null");
        this.currentScreen.setLocation(e.getX(),e.getY());
        if ( this.window.isSet() ) 
        {
            try {
                // LOGGER.info("currentScreen("+this.currentScreen.getX()+','+this.currentScreen.getY()+")");
                Point2D currentWorld = this.ScreenToWorld(this.currentScreen);
                // LOGGER.info("currentWorld("+currentWorld.getX()+','+currentWorld.getY()+")");                
                // LOGGER.info("spatialViewMBR("+this.spatialView.getMBR().toString());
                this.spatialView.getSVPanel().updateMouseMove(currentWorld,this.spatialView.getPrecision(false));
            } catch (NoninvertibleTransformException nte) {
                // LOGGER.info("layerCount=" + this.spatialView.getLayerCount() + " getMBR.isInvalid()=" + this.spatialView.getMBR().isInvalid());
                if ( this.spatialView.getLayerCount()!=0 && 
                     this.spatialView.getMBR().isInvalid()==false) {
                    LOGGER.warn("(MapPanel.mouseMoved)" +this.ERROR_SCREEN2WORLD_TRANSFORM + " = " + nte.getLocalizedMessage());
                }
            }
        } else {
            return;  // Not set, can't do anything. (SGG 23/12/2015)
        }
        ViewOperationListener.VIEW_OPERATION svo = this.spatialView.getSVPanel().getVoListener().getSpatialViewOpr();
        switch (svo)
        {
          case QUERY :
              // create copy of background image
              this.createBufferImage();
              this.getBiG2D().setColor(this.getFeatureColour());
              Composite oldAlpha = this.getBiG2D().getComposite();
              Shape circle = PointMarker.circle(this.currentScreen, 2.0 * this.getSearchPixels() );
              this.getBiG2D().setComposite( Styling.getAlphaComposite(0.3f) );
              this.getBiG2D().fill(circle);
              this.getBiG2D().setComposite(oldAlpha);
              this.getBiG2D().draw(circle);
              setRedrawBIOnly(REDRAW_BI.IMAGE_QUERY_SHAPE);
              repaint();
              break;
        
          case MEASURE_DISTANCE :
          case CREATE_LINE :
          case QUERY_LINE :
        
          case MEASURE_POLYGON :
          case CREATE_POLYGON :
          case QUERY_POLYGON :
            // nothing to do if the user hasn't clicked start of line
            //
            if ( Double.isNaN(this.lastScreen.getX()) && 
                 Double.isNaN(this.lastScreen.getY()))
                return;

            // Do the elastic band thing...
            //                
            // create copy of background image
            this.createBufferImage();

            // Set new line color 
            //
            this.getBiG2D().setColor(this.getFeatureColour()); 
            this.getBiG2D().draw(this.createShapeScreen);
            this.getBiG2D().drawLine((int)this.lastScreen.getX(),   (int)this.lastScreen.getY(),
                                     (int)this.currentScreen.getX(),(int)this.currentScreen.getY());
            // set only show buffered image
            //
            switch (svo) {
              case MEASURE_POLYGON:
              case MEASURE_DISTANCE:
                setRedrawBIOnly(REDRAW_BI.IMAGE_MEASURE_SHAPE);
                break;
              case CREATE_LINE:
              case QUERY_LINE:
              case CREATE_POLYGON:
              case QUERY_POLYGON:
                setRedrawBIOnly(REDRAW_BI.IMAGE_CREATE_SHAPE);
                break;
			default:
				break;
            }
            // repaint screen image
            //
            repaint();
            break;
        
          default :
            // image data read - ignore all events
            if (redrawBIOnly != REDRAW_BI.UNDEFINE) {
                return;
            }
        }
    }

    public void mouseDragged(MouseEvent e) 
    {
        ViewOperationListener.VIEW_OPERATION svo = this.spatialView.getSVPanel().getVoListener().getSpatialViewOpr();
        
        // Some actions cannot be associated with mouse drag events
        //
        switch ( svo )
        {
          case MOVE :
        
          case ZOOM_BOUNDS:
          case MEASURE_RECTANGLE :
          case CREATE_RECTANGLE :
          case QUERY_RECTANGLE :
        
          case MEASURE_CIRCLE :
          case CREATE_CIRCLE :
          case QUERY_CIRCLE :
              // Always let in
              break;
          case ZOOM_IN :
          case ZOOM_OUT :
        
          case CREATE_POINT :
          case QUERY_POINT :
        
          case CREATE_MULTIPOINT :
          case QUERY_MULTIPOINT :
        
          case MEASURE_DISTANCE:
          case CREATE_LINE :
          case QUERY_LINE :
        
          case MEASURE_POLYGON:
          case CREATE_POLYGON :
          case QUERY_POLYGON :
          default:
            return;
        }
        
        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
            LOGGER.debug("mouseDragged: LEFT MOUSE BUTTON; svo="+svo.toString() );
            switch (svo)
            {
                case MOVE: 
                    if (this.redrawBIOnly == REDRAW_BI.IMAGE_MOUSE_MOVE) 
                    {
                        // Get position where clicked on screen 
                        //
                        this.currentScreen.setLocation(e.getX(),e.getY());
                        // transform to world position
                        //
                        Point2D dragWorldPosn = null;
                        try {
                             dragWorldPosn = ScreenToWorld(this.currentScreen);
                             // LOGGER.info(" "+dragWorldPosn.getX()+','+dragWorldPosn.getY());
                        } catch (NoninvertibleTransformException nte) {
                            if ( this.getDimension().getWidth()  == 0.0f ||
                                 this.getDimension().getHeight() == 0.0f ) {
                                 // Screen dimensions not yet set. Could be no layers are turned on
                                 return;
                            }
                            if ( this.spatialView.getLayerCount()!=0 && 
                                 this.spatialView.getMBR().isInvalid()==false) {
                                LOGGER.warn("(MapPanel.mouseDragged)" +this.ERROR_SCREEN2WORLD_TRANSFORM + " - " + nte.getLocalizedMessage());
                            }
                        }

                        // calculate image position change in world coordinates
                        //
                        Point2D wDelta = new Point.Double((this.startWorld.getX() - dragWorldPosn.getX()),
                                                          (this.startWorld.getY() - dragWorldPosn.getY()));
                        if ((wDelta.getX() != 0) && (wDelta.getY() != 0))
                        {
                            this.window.translate(wDelta);
                            this.setWorldToScreenTransform(this.window.minX, 
                                                           this.window.minY,
                                                           this.window.maxX,
                                                           this.window.maxY,
                                                           this.getDimension());
                            repaint();
                        }
                    }
                break;
                
              case ZOOM_BOUNDS :
              case MEASURE_RECTANGLE :
              case CREATE_RECTANGLE :
              case QUERY_RECTANGLE :
            
              case MEASURE_CIRCLE :
              case CREATE_CIRCLE :
              case QUERY_CIRCLE :
                // Get position where clicked on screen 
                //
                this.currentScreen.setLocation(e.getX(),e.getY());
                if ( this.currentScreen.equals(this.startScreen) )
                    return;

                // create copy of background image
                this.createBufferImage();                    

                // set window color and save old alpha
                //
                this.getBiG2D().setColor(this.getFeatureColour());
                Composite oldAlpha = this.getBiG2D().getComposite();
                
                if ( svo==ViewOperationListener.VIEW_OPERATION.CREATE_CIRCLE  ||
                     svo==ViewOperationListener.VIEW_OPERATION.MEASURE_CIRCLE ||
                     svo==ViewOperationListener.VIEW_OPERATION.QUERY_CIRCLE ) 
                {
                    Shape circle = PointMarker.circle(this.startScreen, (MathUtils.pythagoras(this.startScreen,this.currentScreen) * 2.0f));
                    this.getBiG2D().setComposite( Styling.getAlphaComposite(0.3f) );
                    this.getBiG2D().fill(circle);
                    this.getBiG2D().setComposite(oldAlpha);
                    this.getBiG2D().draw(circle);
                } else {
                    Envelope rectangle = 
                        new Envelope( 
                            new Point.Double(((this.startScreen.getX() < this.currentScreen.getX()) ? this.startScreen.getX()   : this.currentScreen.getX()),
                                             ((this.startScreen.getY() < this.currentScreen.getY()) ? this.startScreen.getY()   : this.currentScreen.getY())),
                            new Point.Double(((this.startScreen.getX() < this.currentScreen.getX()) ? this.currentScreen.getX() : this.startScreen.getX()),
                                             ((this.startScreen.getY() < this.currentScreen.getY()) ? this.currentScreen.getY() : this.startScreen.getY()))
                            );
                    this.getBiG2D().setComposite( Styling.getAlphaComposite(0.3f) );
                    this.getBiG2D().fillRect((int)rectangle.getMinX(), (int)rectangle.getMinY(), 
                                             (int)rectangle.getWidth(),(int)rectangle.getHeight());
                    this.getBiG2D().setComposite(oldAlpha);
                    this.getBiG2D().drawRect((int)rectangle.getMinX(), (int)rectangle.getMinY(), 
                                             (int)rectangle.getWidth(),(int)rectangle.getHeight());
                }
                // set only show buffered image
                setRedrawBIOnly(REDRAW_BI.IMAGE_ZOOM_BOUNDS);
                // repaint screen image
                repaint();
			default:
				break;
            }
        }
    }

    public void mouseReleased(MouseEvent _mouseEvent) 
    {
        final ViewOperationListener.VIEW_OPERATION svo = this.spatialView.getSVPanel().getVoListener().getSpatialViewOpr();
        // image data read - ignore all events
        if ((redrawBIOnly != REDRAW_BI.UNDEFINE) && 
            (redrawBIOnly != REDRAW_BI.IMAGE_MOUSE_MOVE) &&
            (redrawBIOnly != REDRAW_BI.IMAGE_ZOOM_BOUNDS) &&
            (redrawBIOnly != REDRAW_BI.IMAGE_CREATE_SHAPE) &&
            (redrawBIOnly != REDRAW_BI.IMAGE_QUERY_SHAPE) &&
            (redrawBIOnly != REDRAW_BI.IMAGE_MEASURE_SHAPE)) {
            return;
        }
        // Check view window and panel is initialized.
        if ( this.getDimension().getWidth() == 0.0f && this.getDimension().getWidth() == 0.0f ) {
            return;
        }
        if ( this.window.isNull() ) {
            if (this.spatialView.initializeMBR(null) == false) {
                return;
            }
        }
        try {
            if ((_mouseEvent.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) 
            {
                LOGGER.debug("mouseReleased: LEFT MOUSE BUTTON: "+ svo.toString());
                switch (svo)
                {
                  case ZOOM_BOUNDS :
                  case MEASURE_RECTANGLE :
                  case CREATE_RECTANGLE:
                  case QUERY_RECTANGLE:

                  case MEASURE_CIRCLE :
                  case CREATE_CIRCLE :
                  case QUERY_CIRCLE :
                      this.currentScreen.setLocation(_mouseEvent.getX(),_mouseEvent.getY());
                      if ( this.currentScreen.equals(this.startScreen) ) {
                          // No zoom window defined
                          spatialView.getSVPanel().setMessage(this.propertyManager.getMsg("ZOOM_WINDOW_INSUFFICIENT"),true);
                          // repaint whole image as this operation is stuffed
                          setRedrawBIOnly(REDRAW_BI.IMAGE_SIZE_CHANGE);
                          this.refreshAll();
                          return;
                      }
  
                      // Create new MBR window
                      //
                      Envelope mbr = new Envelope(this.startWorld,
                                                                ScreenToWorld(this.currentScreen));
                    
                      this.getBiG2D().setColor(this.getFeatureColour());
                      Composite oldAlpha = this.getBiG2D().getComposite();
                    
                      if ( svo==ViewOperationListener.VIEW_OPERATION.CREATE_CIRCLE  || 
                           svo==ViewOperationListener.VIEW_OPERATION.MEASURE_CIRCLE || 
                           svo==ViewOperationListener.VIEW_OPERATION.QUERY_CIRCLE  ) 
                      {
                          double radius = MathUtils.pythagoras(this.startScreen,this.currentScreen);
                          Shape circle = PointMarker.circle(this.startScreen, (radius * 2.0f) );
                          this.getBiG2D().setComposite( Styling.getAlphaComposite(0.3f) );
                          this.getBiG2D().fill(circle);
                          this.getBiG2D().setComposite(oldAlpha);
                          this.getBiG2D().draw(circle);
                          
                          // Create geometry and copy to clipboard
                          //
                          Rectangle circleMBR = circle.getBounds();
                          double worldWidth  = circleMBR.getWidth()  * this.getPixelSize().getX();
                          double worldHeight = circleMBR.getHeight() * this.getPixelSize().getY();
                          JGeometry jgeom = new JGeometry(2003,
                                                         spatialView.getSRIDAsInteger(), 
                                                         new int[] {1,1003,4}, 
                                                         new double[] {this.startWorld.getX()-worldWidth/2.0,this.startWorld.getY(),
                                                                       this.startWorld.getX(),               this.startWorld.getY()-worldHeight/2.0,
                                                                       this.startWorld.getX()+worldWidth/2.0,this.startWorld.getY()});

                          if ( svo==ViewOperationListener.VIEW_OPERATION.CREATE_CIRCLE ||
                               svo==ViewOperationListener.VIEW_OPERATION.QUERY_CIRCLE  ) 
                          {
                              if (svo==ViewOperationListener.VIEW_OPERATION.CREATE_CIRCLE) {
                                  presentShapeForFinalReview(jgeom);
                              } else {
                                  presentSpatialSQLForFinalReview(jgeom);
                              }
                          } else {
                              computeAreaLength(jgeom, svo);
                          }
                          // End of this operation
                          this.initialiseShapeActions();
                          // repaint leaving object on screen unless query
                          //
                          if (svo!=ViewOperationListener.VIEW_OPERATION.QUERY_CIRCLE) {
                              setRedrawBIOnly(REDRAW_BI.FINISH);
                              repaint();
                          }
                      } else if ( svo==ViewOperationListener.VIEW_OPERATION.CREATE_RECTANGLE  ||
                                  svo==ViewOperationListener.VIEW_OPERATION.MEASURE_RECTANGLE ||
                                  svo==ViewOperationListener.VIEW_OPERATION.QUERY_RECTANGLE     ) 
                      {
                          // Redraw final rectangle with fill if CREATE_RECTANGLE
                          Envelope rectangle = 
                                new Envelope( 
                                    new Point.Double(((this.startScreen.getX() < this.currentScreen.getX()) ? this.startScreen.getX()   : this.currentScreen.getX()),
                                                     ((this.startScreen.getY() < this.currentScreen.getY()) ? this.startScreen.getY()   : this.currentScreen.getY())),
                                    new Point.Double(((this.startScreen.getX() < this.currentScreen.getX()) ? this.currentScreen.getX() : this.startScreen.getX()),
                                                     ((this.startScreen.getY() < this.currentScreen.getY()) ? this.currentScreen.getY() : this.startScreen.getY()))
                                    );
                          this.getBiG2D().setComposite( Styling.getAlphaComposite(0.3f) );
                          this.getBiG2D().fillRect((int)rectangle.getMinX(), (int)rectangle.getMinY(), 
                                                   (int)rectangle.getWidth(),(int)rectangle.getHeight());
                          this.getBiG2D().setComposite(oldAlpha);
                          // Draw line on top of fill
                          this.getBiG2D().drawRect((int)rectangle.getMinX(), (int)rectangle.getMinY(), 
                                                   (int)rectangle.getWidth(),(int)rectangle.getHeight());
                          
                          if ( svo==ViewOperationListener.VIEW_OPERATION.CREATE_RECTANGLE ||
                               svo==ViewOperationListener.VIEW_OPERATION.QUERY_RECTANGLE  ) 
                          {
                              JGeometry jGeom = new JGeometry(2003,
                                                              spatialView.getSRIDAsInteger(), 
                                                              mbr.centre().getX(),mbr.centre().getY(),Double.NaN,
                                                              new int[] {1,1003,3}, 
                                                              new double[] {mbr.getMinX(),mbr.getMinY(),mbr.getMaxX(),mbr.getMaxY()});
                              if (svo==ViewOperationListener.VIEW_OPERATION.CREATE_RECTANGLE ) {
                                  presentShapeForFinalReview(jGeom);
                              } else {
                                  presentSpatialSQLForFinalReview(jGeom);
                              }
                          } else {
                            JGeometry geom = new JGeometry(mbr.getMinX(),mbr.getMinY(),mbr.getMaxX(),mbr.getMaxY(),  spatialView.getSRIDAsInteger());
                            computeAreaLength(geom, svo);                            
                          }
                          // End of this operation
                          this.initialiseShapeActions();
                          // repaint leaving object on screen unless query
                          //
                          if (svo!=ViewOperationListener.VIEW_OPERATION.QUERY_RECTANGLE) {
                              setRedrawBIOnly(REDRAW_BI.FINISH);
                              repaint();
                          }
                      } else {
                          // set new world window size
                          //
                          this.setWindow(mbr);
                          // repaint whole image as we have zoomed in
                          setRedrawBIOnly(REDRAW_BI.IMAGE_SIZE_CHANGE);
                          this.refreshAll();
                      }
                      break;

                case QUERY :
                  this.currentScreen.setLocation(_mouseEvent.getX(),_mouseEvent.getY());
                  Point2D worldPosn = null;
                  worldPosn = this.ScreenToWorld(this.currentScreen);
                  queryByPoint(worldPosn,true);
                  setRedrawBIOnly(REDRAW_BI.IMAGE_SIZE_CHANGE);  // Sadly, this does a full redraw
                  // setRedrawBIOnly(REDRAW_BI.IMAGE_QUERY_SHAPE);
                  repaint();
                  break;
                
                case MOVE :  
                    // Final release of the mouse is the final position.
                    // Make sure final position is OK ie normalized window
                    //
                    this.Initialise();
                    // repaint whole image
                    this.redrawBIOnly = REDRAW_BI.IMAGE_SIZE_CHANGE;
                    this.refreshAll();
                    break;
				case CREATE_LINE:
					break;
				case CREATE_MULTIPOINT:
					break;
				case CREATE_POINT:
					break;
				case CREATE_POLYGON:
					break;
				case DATA_RELOAD:
					break;
				case IMAGE_COPY:
					break;
				case MBR_NEXT:
					break;
				case MBR_PREV:
					break;
				case MEASURE_DISTANCE:
					break;
				case MEASURE_POLYGON:
					break;
				case NONE:
					break;
				case QUERY_LINE:
					break;
				case QUERY_MULTIPOINT:
					break;
				case QUERY_POINT:
					break;
				case QUERY_POLYGON:
					break;
				case VIEW_DELETE:
					break;
				case VIEW_SWITCH:
					break;
				case ZOOM_FULL:
					break;
				case ZOOM_IN:
					break;
				case ZOOM_LAYER:
					break;
				case ZOOM_OUT:
					break;
				default:
					break;
                }
            } else if ((_mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) 
            {
                LOGGER.debug("mouseReleased: RIGHT MOUSE BUTTON: "+ svo.toString());
                Stroke oldStroke = this.getBiG2D().getStroke();
                switch (svo)
                {
                    case MEASURE_DISTANCE  :
                    case CREATE_LINE       :
                    case QUERY_LINE        :
                    case CREATE_MULTIPOINT :
                    case QUERY_MULTIPOINT  :
                    case MEASURE_POLYGON   :
                    case CREATE_POLYGON    :
                    case QUERY_POLYGON     :
                    
                      // nothing to do if the user hasn't clicked start of shape
                      //
                      if ( Double.isNaN(this.lastScreen.getX()) && 
                           Double.isNaN(this.lastScreen.getY()))
                          return;
                    
                      // Next point in the line
                      // transform to world position
                      //
                      this.currentScreen.setLocation(_mouseEvent.getX(),_mouseEvent.getY());
                    
                      // Add only if not equal to last point                     
                      if ( ! this.currentScreen.equals(this.lastScreen) )
                      { 
                        Point2D worldPt = ScreenToWorld(this.currentScreen);
                        this.createShapeWorld.lineTo(worldPt.getX(),worldPt.getY());
                        this.createShapeScreen.lineTo(this.currentScreen.getX(),this.currentScreen.getY());
                      }
                    
                      // Now check that final shape has enough coordinates 
                      //
                      int numCoords = getCoordCount(this.createShapeScreen);
                      
                      // No point going on if not enough points for required shape
                      //
                      if ( ( numCoords == 0 && ( svo==ViewOperationListener.VIEW_OPERATION.CREATE_MULTIPOINT ||
                                                 svo==ViewOperationListener.VIEW_OPERATION.QUERY_MULTIPOINT ) ) ||
                           ( numCoords  < 2 && ( svo==ViewOperationListener.VIEW_OPERATION.MEASURE_DISTANCE ||
                                                 svo==ViewOperationListener.VIEW_OPERATION.CREATE_LINE      ||
                                                 svo==ViewOperationListener.VIEW_OPERATION.QUERY_LINE       ) ) ||
                           ( numCoords  < 3 && ( svo==ViewOperationListener.VIEW_OPERATION.MEASURE_POLYGON ||
                                                 svo==ViewOperationListener.VIEW_OPERATION.CREATE_POLYGON  ||
                                                 svo==ViewOperationListener.VIEW_OPERATION.QUERY_POLYGON ) ) ) 
                      {
                          spatialView.getSVPanel().setMessage(this.propertyManager.getMsg("MEASURE_INSUFFICIENT_COORDS"),
                                                              true);
                          return;
                      }
                    
                      // Draw/Fill temporary shape
                      //
                      // Set window color
                      //
                      this.getBiG2D().setColor(this.getFeatureColour());
                    
                      if ((svo==ViewOperationListener.VIEW_OPERATION.MEASURE_POLYGON) ||
                          (svo==ViewOperationListener.VIEW_OPERATION.CREATE_POLYGON)) 
                      {
                          Composite oldAlpha = this.getBiG2D().getComposite();
                          this.getBiG2D().setComposite( Styling.getAlphaComposite(0.3f) );
                          // close area shape 
                          this.createShapeWorld.closePath();
                          this.createShapeScreen.closePath();
                          this.getBiG2D().fill(new Area(createShapeScreen));
                          this.getBiG2D().setComposite(oldAlpha);
                      } 
                    
                      if ( svo==ViewOperationListener.VIEW_OPERATION.CREATE_MULTIPOINT  ||
                           svo==ViewOperationListener.VIEW_OPERATION.QUERY_MULTIPOINT ) 
                      {
                          this.getBiG2D().setStroke(new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                          this.getBiG2D().draw(PointMarker.cross(this.currentScreen, 8, 0 ));
                          this.getBiG2D().setStroke(oldStroke);
                      } else { 
                        // Draw line last (will draw boundary of area)
                        //
                        this.getBiG2D().draw(createShapeScreen);
                      }

                      // Do appropriate computations/processing
                      //
                      // Build SDO_GEOMETRY from closed Path2D.Double (area) object
                      //
                      final JGeometry jGeom = getGeometryFromShape(createShapeWorld,svo);
                      if ( jGeom == null ) {
                        this.initialiseShapeActions();
                        spatialView.getSVPanel().setMessage(propertyManager.getMsg("SHAPE_CONVERSION_ERROR"),true);
                        return;
                      }

                      switch (svo) 
                      {
                        case MEASURE_POLYGON:
                        case MEASURE_DISTANCE:
                          computeAreaLength(jGeom, svo);
                          setRedrawBIOnly(REDRAW_BI.FINISH);
                          repaint();
                          break;
                        case CREATE_LINE:
                        case CREATE_POLYGON:
                        case CREATE_MULTIPOINT:
                          presentShapeForFinalReview(jGeom);
                          setRedrawBIOnly(REDRAW_BI.FINISH);
                          repaint();
                          break;
                        case QUERY_LINE:
                        case QUERY_MULTIPOINT:
                        case QUERY_POLYGON:
                          @SuppressWarnings("unused") 
                          double maxBuffer = Math.max(this.getPixelSize().getX(), 
                                                      this.getPixelSize().getY()) 
                                                    * this.getSearchPixels();
                          presentSpatialSQLForFinalReview(jGeom);
                          break;
					default:
						break;
                      }
                      // End of operation
                      //
                      this.initialiseShapeActions();
                      break;
                    
                    default: /* Right Mouse Click Menu */
                        LOGGER.debug("mouseReleased: SET Point/COPY MBR/Point");
                        // Create Menu
                        JPopupMenu popup = new JPopupMenu(Constants.GEORAPTOR);                        
                        final MouseEvent mouseEvent = _mouseEvent;
                        AbstractAction jumpLocation = 
                            new AbstractAction(this.MAP_MENU_JUMP,this.iconJump2MBR) {
								private static final long serialVersionUID = 4434653818566059963L;

								public void actionPerformed(ActionEvent e) {
                                    spatialView.getSVPanel().windowCentreMousePressed(mouseEvent);
                                }
                            };
                        popup.add(jumpLocation);
                        AbstractAction copyMBRAsSdoGeometry = 
                            new AbstractAction(this.MAP_MENU_COPY_MBR,this.iconMenuCopyMbr) {
								private static final long serialVersionUID = 2329631276044216612L;

								public void actionPerformed(ActionEvent e) {
                                    JGeometry mbr = new JGeometry(getWindow().minX, getWindow().minY,
                                                                  getWindow().maxX, getWindow().maxY,
                                                                  spatialView.getSRIDAsInteger());
                                    String sGeom = RenderTool.renderGeometryAsPlainText(mbr,
                                                                                        Constants.TAG_MDSYS_SDO_GEOMETRY,
                                                                                        Constants.bracketType.NONE,
                                                                                        spatialView.getPrecision(false));
                                    Tools.doClipboardCopy(sGeom);
                                    spatialView.getSVPanel().setMessage("MBR copied...",false);
                                }
                            };
                        popup.add(copyMBRAsSdoGeometry);
                        AbstractAction copyMBRCenterAsSdoGeometry = 
                            new AbstractAction(this.MAP_MENU_COPY_CENTRE,this.iconMenuCopyCentre) {
								private static final long serialVersionUID = -8820662543240339163L;

								public void actionPerformed(ActionEvent e) {
                                    Point2D worldCentre = window.centre();
                                    JGeometry jGeom = new JGeometry(worldCentre.getX(),
                                                                    worldCentre.getY(),
                                                                    spatialView.getSRIDAsInteger());
                                    String sGeom = RenderTool.renderGeometryAsPlainText(jGeom,
                                                                                        Constants.TAG_MDSYS_SDO_GEOMETRY,
                                                                                        Constants.bracketType.NONE,
                                                                                        spatialView.getPrecision(false));
                                    Tools.doClipboardCopy(sGeom);
                                    spatialView.getSVPanel().setMessage("Center point copied...",false);
                                }
                            };
                        popup.add(copyMBRCenterAsSdoGeometry);
                        // Convert clicked point (not centre) to geometry (offer geodetic when projected)
                        final Point2D mPoint = this.pixelToWorld(new Point2D.Double(_mouseEvent.getX(), _mouseEvent.getY()));
                        AbstractAction xyToSdoGeometry = 
                            new AbstractAction(this.MAP_MENU_XY_POINT,this.iconMenuXYPoint) {
								private static final long serialVersionUID = 5360802700419296750L;

								public void actionPerformed(ActionEvent e) {
                                    JGeometry jGeom = new JGeometry(mPoint.getX(),mPoint.getY(),spatialView.getSRIDAsInteger());
                                    String sGeom = RenderTool.renderGeometryAsPlainText(jGeom,Constants.TAG_MDSYS_SDO_GEOMETRY,Constants.bracketType.NONE,spatialView.getDefaultPrecision());
                                    Tools.doClipboardCopy(sGeom);
                                    spatialView.getSVPanel().setMessage("XY Point copied...",false);
                            }
                        };
                        popup.add(xyToSdoGeometry);
                        if ( ! this.spatialView.isGeodetic() ) {
                            // Allow coordinate shown as lat/long
                            AbstractAction projectToGeodeticSdoGeometry = 
                                new AbstractAction(this.MAP_MENU_PROJECT_POINT,this.iconMenuProjectPoint) {
									private static final long serialVersionUID = 2909522653798726186L;

									public void actionPerformed(ActionEvent e) {
                                        try {
                                            Point2D pPoint = MetadataTool.projectPoint(spatialView.getActiveLayer().getConnection(), mPoint, spatialView.getSRIDAsInteger(), 4326);
                                            JGeometry jGeom = new JGeometry(pPoint.getX(),pPoint.getY(),4326);
                                            String sGeom = RenderTool.renderGeometryAsPlainText(jGeom,Constants.TAG_MDSYS_SDO_GEOMETRY,Constants.bracketType.NONE,8);
                                            Tools.doClipboardCopy(sGeom);
                                            spatialView.getSVPanel().setMessage("Lat/Long Point copied...",false);
                                        } catch (SQLException sqle) {
                                            LOGGER.warn("Projection of point to geodetic failed: " + sqle.getMessage());
                                        }
                                }
                            };
                            popup.add(projectToGeodeticSdoGeometry);
                        }
                        // display locate menu at click point
                        popup.show(this,_mouseEvent.getX(), _mouseEvent.getY());
                        break;
                }
            }
        } catch (NoninvertibleTransformException nte) {
            if ( this.spatialView.getLayerCount()!=0 && 
                 this.spatialView.getMBR().isInvalid()==false) {
                LOGGER.warn("(MapPanel.mouseReleased)" +this.ERROR_SCREEN2WORLD_TRANSFORM + " - " + nte.getLocalizedMessage());
            }
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) 
    {
        // image data read - ignore all events
        if (redrawBIOnly != REDRAW_BI.UNDEFINE) {
            return;
        }
        int notches = e.getWheelRotation();
        // zoom in
        if (notches < 0) {
            Zoom(org.GeoRaptor
                    .SpatialView
                    .SupportClasses
                    .Envelope
                    .zoom
                    .IN);
        } // zoom out
        else {
            Zoom(org.GeoRaptor
                    .SpatialView
                    .SupportClasses
                    .Envelope
                    .zoom
                    .OUT);
        }
        this.refreshAll();
    }

    public void focusGained(FocusEvent e) {}

    public void focusLost(FocusEvent e) {}

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyChar() == KeyEvent.VK_ESCAPE ) 
            this.spatialView.getSVPanel().setCancelOperation(true);
    }

    /** Handle the key pressed event from the text field. */
    public void keyTyped(KeyEvent e) {}

    /** Handle the key released event from the text field. */
    public void keyReleased(KeyEvent e) {}

    public void refreshAll() 
    {
        // Make sure next/prev etc buttons are correct
        this.spatialView.getSVPanel().setToolbarStatus();
        if ( this.window==null || 
             this.window.isNull() ||
             this.window.isInvalid() )  {
            LOGGER.debug("refreshALL(): window is null so initialize from view");
            if (this.spatialView.initializeMBR(null) == false) {
                return;
            }
        }
        LOGGER.debug("refreshALL: Check View Operation: " + this.spatialView.getSVPanel().getVoListener().getSpatialViewOpr().toString());
        repaint();
    } // public RefreshAll

    public void setRenderHint(String _hint) {
        if (Strings.isEmpty(_hint) ) {
            setRenderHint(RENDER_HINTS.NORMAL);
        } else {
            setRenderHint(RENDER_HINTS.valueOf(_hint)); 
        }
    }

    public void setRenderHint(RENDER_HINTS _hint) {
        // Set global rendering hints
        // Default rendering is done with antialiasing.
        //
        this.qualityHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                                               RenderingHints.VALUE_ANTIALIAS_ON);
        switch (_hint) {
          case QUALITY : 
            qualityHints.put(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY);
            // VALUE_STROKE_PURE no normalization is performed. Use this hint when you prefer that the rendering of your geometry is accurate rather than visually consistent. 
            qualityHints.put(RenderingHints.KEY_STROKE_CONTROL,
                             RenderingHints.VALUE_STROKE_PURE);
            // Text rendering to be done with some form of antialiasing.
            qualityHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // Alpha blending algorithms are chosen with a preference for calculation speed. 
            qualityHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                             RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            break;
          case SPEED : 
            qualityHints.put(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_SPEED);
            // VALUE_STROKE_NORMALIZE improves the consistency of the appearance of a stroke whether or not antialiasing is applied to it and wherever the stoke is rendered on the pixel grid. 
            qualityHints.put(RenderingHints.KEY_STROKE_CONTROL,
                             RenderingHints.VALUE_STROKE_NORMALIZE);
            // text rendering to be done without any form of antialiasing.
            qualityHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            // rendering is done without antialiasing for speed
            qualityHints.put(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_OFF);
            // Alpha blending algorithms are chosen with a preference for precision and visual quality. 
            qualityHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                             RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            break;
          case NORMAL :  
          default:
            qualityHints.put(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_DEFAULT);
            // VALUE_STROKE_DEFAULT indicates preference whether or not a stroke is normalized.
            qualityHints.put(RenderingHints.KEY_STROKE_CONTROL,
                             RenderingHints.VALUE_STROKE_DEFAULT);
            // text rendering to be done according to the KEY_ANTIALIASING hint or a default chosen by the implementation.
            qualityHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
            // Rendering is done with a default antialiasing mode chosen by the implementation.
            qualityHints.put(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_DEFAULT);
            // Alpha blending algorithms are chosen by the implementation for a good tradeoff of performance vs. quality.
            qualityHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                             RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
        }
        if (this.getBiG2D() != null)
          this.getBiG2D().setRenderingHints(this.qualityHints);
        this.renderHint = _hint;
    }
    
    private boolean hasDrawableLayer() {
      
        Iterator<SVSpatialLayer> layerList = this.spatialView.getLayerList().values().iterator();
        boolean layerSelectForDraw = false;
        while (layerList.hasNext()) {
            SVSpatialLayer layer = layerList.next();
            if (layer.isDraw()) {
                layerSelectForDraw = true;
                break;
            }
        }
        return layerSelectForDraw;
    }
          
    public RENDER_HINTS getRenderHint()  {
        return this.renderHint;
    }

    private void checkLayerConnections() {
        LOGGER.debug("------------------------------------\nSTART: checkLayerConnections");
        String connName = "",
               nameList = "";
        Iterator<SVSpatialLayer> iter = this.spatialView.getLayerList().values().iterator();
        while (iter.hasNext()) {
            SVSpatialLayer layer = iter.next();
            connName = layer.getConnectionName();
            if ( ( !Strings.isEmpty(connName) ) && 
                 ( ! nameList.contains(connName) ) ) {
                try {
                    LOGGER.debug("checkLayerConnections. Checking " + connName);
                    // Make sure layer's connection exists...
                    if ( ! DatabaseConnections.getInstance().connectionExists(connName)) {
                        DatabaseConnections.getInstance().addConnection(connName);
                    }
                    // Make sure layer's connection is open 
                    if ( ! layer.isConnectionOpen() ) {
                        boolean oc = layer.openConnection();
                        LOGGER.debug("Connection " + connName + " " + (oc?"opened":"closed."));
                    }
                } catch (IllegalStateException ise) {
                    // Thrown by super.getConnection() indicates starting up, so we do nothing
                    LOGGER.debug("checkLayerConnections exception " + ise.toString());
                }
                // Already checked it, don't do it again
                //
                nameList = " " + connName;
            }
        }
        LOGGER.debug("End: checkLayerConnections\n------------------------------------");
    }
    
    public void paintComponent(Graphics g) 
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        // MBR is not set yet - do nothing
        if ( this.window.isNull() ) {
            spatialView.getSVPanel().setMessage(MAPEXTENT_NOT_SET,false);
            return;
        }
        //LOGGER.debug("***** START: MapPanel.paintComponent() - BI=" + this.bi.toString() + " getSize()=" + this.getSize().toString() + " SVO=" + spatialView.getSVPanel().getVoListener().getSpatialViewOpr().toString());

        boolean isInitialising = (this.bi.width==0 && this.bi.height==0);
        
        // if window size change, or a refresh is asked for,
        // refresh all image and read again all data
        //
        if ((this.bi.width  != this.getSize().width  ) || 
            (this.bi.height != this.getSize().height ) ||
            this.redrawBIOnly.equals(REDRAW_BI.IMAGE_SIZE_CHANGE)
           )
        {
            // Reset regardless
            this.clientView.setSize(this.getSize().width,this.getSize().height);
                    this.bi.setSize(this.getSize().width,this.getSize().height);
            // dimension of user screen changed - create new device context
            this.biImage = getCompatibleBI(this.bi);
            this.biG2D   = this.biImage.createGraphics();
            this.biG2D.setRenderingHints(this.qualityHints);
            // delete buffered image if exist
            this.biBasic = null;
            // fill image with background color
            this.biG2D.setColor(this.mapBackground );
            this.biG2D.fillRect(0, 0, this.bi.width, this.bi.height);
            // refresh image MBR labels
            this.spatialView.getSVPanel().updateViewStatusMBR(this.window,this.spatialView.getPrecision(false));
            if ( isInitialising ) {
                //LOGGER.debug(" ***** END: MapPanel.paintComponent() isInitialising()");
                return;
            }
            //LOGGER.debug(" ******** this.setRedrawBIOnly(REDRAW_BI.IMAGE_SIZE_CHANGE)");
            this.setRedrawBIOnly(REDRAW_BI.IMAGE_SIZE_CHANGE);
        }
        
        // Now do appropriate rendering 
        //
        switch ( this.redrawBIOnly ) 
        {
        case ONLY_PAINT :
        case IMAGE_ZOOM_BOUNDS :
        case IMAGE_MEASURE_SHAPE :
        case IMAGE_CREATE_SHAPE :  
            // Remark : long story why we need two status
            // show last image
            // true sets drawTimer to null
            paintComponentFinish(g2, true);
            //LOGGER.debug(" ***** END: MapPanel.paintComponent() this.redrawBIOnly="+this.redrawBIOnly.toString());
            return;
            
        case IMAGE_QUERY_SHAPE :
            // show last buffered image
            paintComponentFinish(g2, false);
            if ( afterLayerDrawList.size() < 0 ) {
                this.redrawBIOnly = REDRAW_BI.ONLY_PAINT;
                // perform after draw operations
                AfterLayerDraw ald = null;
                Iterator<AfterLayerDraw> iter = afterLayerDrawList.iterator();
                while (iter.hasNext()) {
                    ald = iter.next();
                    ald.draw(biG2D);
                }
                redrawBIOnly = REDRAW_BI.FINISH;
                refreshAll();
            }
            //LOGGER.debug(" ***** END: MapPanel.paintComponent() this.redrawBIOnly="+this.redrawBIOnly.toString());
            return;

        case IMAGE_SIZE_CHANGE :
            // Nothing to do if no layer is selected for draw
            // and afterLayerDrawList is empty (ie ascetate draw)
            //
            if ( (this.hasDrawableLayer()==false) && afterLayerDrawList.size()==0 ) 
            {
                spatialView.getSVPanel().setMessage(spatialView.getViewName() + ": " + NO_DRAWABLE_LAYERS,false);
                //LOGGER.debug(" ***** END: MapPanel.paintComponent() this.redrawBIOnly="+this.redrawBIOnly.toString() +" - No redrawable layer");
                return;
            }

            boolean viewChanged = this.window.Normalize(this.clientView);
            if ( this.worldToScreenTransform==null || viewChanged) {
                this.setWorldToScreenTransform(this.window, this.getDimension());
                this.windowNavigator.add(this.window);                
            }
            
            // Draw border around mapoverview
            this.biG2D.setColor(this.mapBackground);
            this.biG2D.drawRect(0, 0, 
                                g2.getClipBounds().width - 1, 
                                g2.getClipBounds().height - 1);
            this.spatialView.getSVPanel().setCursorWait();
            this.drawTimer = new Timer();

            // Check all connections in case UI dialog thrown for password expiry
            //
            checkLayerConnections();

            // Do drawing and save window only at end of the draw
            // 
            LOGGER.debug(" ******* this.drawTimer.schedule(...)");
            this.drawTimer.schedule(new DrawTask(g2,this.spatialView), 0);
            //LOGGER.debug(" ***** END: MapPanel.paintComponent() this.redrawBIOnly="+this.redrawBIOnly.toString());
            return;

        case IMAGE_MOUSE_MOVE : 
            // start coordinate for buffered image. We change coordinate when mouse move
            Point pictureStart = new Point(0,0);
            pictureStart.x = (int)(this.currentScreen.getX() - this.startScreen.getX());
            pictureStart.y = (int)(this.currentScreen.getY() - this.startScreen.getY());
            g2.drawImage(this.biImage, pictureStart.x, pictureStart.y, this);
            return;

        case UNDEFINE :
            // show last buffered image
            paintComponentFinish(g2, false);
            //LOGGER.debug(" ***** END: MapPanel.paintComponent() this.redrawBIOnly="+this.redrawBIOnly.toString());
            return;

        case FINISH :
            // show last image
            // true sets drawTimer to null
            paintComponentFinish(g2, true); 
            // set default cursor
            this.spatialView.getSVPanel().setCursorDefault();
            this.redrawBIOnly = REDRAW_BI.UNDEFINE;
            //LOGGER.debug(" ***** END: MapPanel.paintComponent() this.redrawBIOnly="+this.redrawBIOnly.toString());
            return;
		default:
			return;
        }
    }

    /**
     * Paint current buffered image to screen. It can be called many times.
     * @param _g2 reference to graphics context
     * @param _lastTime if this is last call in redraw image, mark image is "finish buffered image"
     */
    public void paintComponentFinish(Graphics2D _g2, boolean _lastTime) {
        // this is image paint - first coordinate is always 0,0
        _g2.drawImage(this.biImage, 0, 0, this);
        if (_lastTime == true) {
            // dispose draw timer
            this.drawTimer = null;
        }
    }

    public void setRedrawBIOnly(REDRAW_BI _redrawBIOnly) {
        this.redrawBIOnly = _redrawBIOnly;
        /** IF DEBUG
        if ( this.redrawBIOnly==REDRAW_BI.UNDEFINE ) {
             StackTraceElement[] elements = Thread.currentThread().getStackTrace();
             for(int i=0; i<elements.length; i++) {
                 System.out.println(elements[i]);
             }
        }
        **/
    }

    public REDRAW_BI getRedrawBIOnly() {
        return this.redrawBIOnly;
    }

    public BufferedImage getBiImage() {
        return this.biImage;
    }

    /**
     * Set window to new MBR. 
     * Then initialize map panel. 
     * @param _window RectangleDouble - The MBR coordinate
     */
    public void setWindow(Envelope _mbr) 
    {
        if ( _mbr == null || 
             _mbr.isNull() || 
             _mbr.isInvalid() ||
             _mbr.isEmpty() ) {
            // DEBUG System.out.println("MapPanel.setWindow: Nothing to do");
            return;
        }
        // Create modifiable local window
        Envelope mbr = new Envelope(_mbr);
        
        // DEBUG System.out.println("MapPanel.setWindow: Current window mbr = " + this.window.toString());
        // DEBUG System.out.println("MapPanel.setWindow: Supplied mbr.equals(this.window)=" + (mbr.equals(this.window)));
        // DEBUG System.out.println("MapPanel.setWindow: this.isWorldToScreenSet()=" + this.isWorldToScreenSet());

        if ( mbr.equals(this.window) ) {
        // DEBUG System.out.println("MapPanel.setWindows: mbr equals this.window.");
            if (this.isWorldToScreenSet())  {
                // DEBUG System.out.println("MapPanel.setWindow: Transform set. Nothing to do.");
                return;
            }
        } else {
            // DEBUG System.out.println("MapPanel.setWindow: mbr does not equal this.window.... ");
            // DEBUG System.out.println("MapPanel.setWindow: window.getWidth="+this.window.getWidth()+" window.getHeight="+this.window.getHeight());
            // Ensure new MBR does not have one side == 0
            //
            if ( _mbr.getWidth()==0 || 
                 _mbr.getHeight()==0 ) 
            {
                double halfSide = Math.max(_mbr.getWidth(),
                                           _mbr.getHeight()) / 2.0;
                if ( halfSide == 0.0 ) {halfSide = 0.5;}
                mbr = new Envelope(_mbr.centre(),halfSide,halfSide);
            }
        }
        // DEBUG System.out.println("MapPanel.setWindow: Set window to current mbr and recalculate WorldToScreen Transformation");
        this.window.Normalize(this.clientView);
        this.window.setMBR(new Envelope(mbr));
        this.setWorldToScreenTransform(this.window.minX, 
                                       this.window.minY,
                                       this.window.maxX,
                                       this.window.maxY,
                                       this.getDimension());
        Initialise();  // Also normalizes window
    }
  
    /**
     * Set window MBR. 
     * then initialize map panel. 
     * @param _Xmin MBR coordinate
     * @param _Ymin MBR coordinate
     * @param _Xmax MBR coordinate
     * @param _Ymax MBR coordinate
     */
    public void setWindow(double _Xmin, double _Ymin, double _Xmax, double _Ymax) 
    {
        this.setWindow(new Envelope(_Xmin,_Ymin,_Xmax,_Ymax));
        Initialise();
    }

    public Envelope getWindow() {
        return this.window;
    }

    public Graphics2D getBiG2D() {
        LOGGER.debug("getBiG2D " + this.biG2D==null?"null":"set");
        return this.biG2D;
    }

    private int getSearchPixels() {
      return MapPanel.mainPrefs.getSearchPixels(); 
    }

    public PropertiesManager getPropertyManager() {
       return this.propertyManager;
    }
    
    private int getPanZoomPercentage() {
      return MapPanel.mainPrefs.getPanZoomPercentage();       
    }
    
    /**
     * When you need to save the basic screen image.
     */
    public void createBufferImage() {
        if (this.biBasic == null) {
            this.biBasic = this.biImage;
        }
        ColorModel cm = this.biBasic.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = this.biBasic.copyData(null);
        this.biImage = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
        this.biG2D = (Graphics2D)this.biImage.getGraphics();
        this.biG2D.setRenderingHints(this.qualityHints);
    }

    public BufferedImage getBufferedImage() {
        if (this.biBasic == null) {
            this.biBasic = this.biImage;
        }
        ColorModel cm = this.biBasic.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = this.biBasic.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * Set application state as there is no buffer image
     */
    public void deleteBufferImage() {
        // set on start image
        createBufferImage();
        // delete basic buffered image
        this.biBasic = null;
    }

    public void setAfterLayerDrawList(ArrayList<AfterLayerDraw> afterLayerDrawList) {
        this.afterLayerDrawList = afterLayerDrawList;
    }

    public ArrayList<AfterLayerDraw> getAfterLayerDrawList() {
        return afterLayerDrawList;
    }

    // This method returns an Image object from a buffered image
    public static Image toImage(BufferedImage bufferedImage) {
        return Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource());
    }

    /**
    * Initialize map panel. 
    * Zoom image to Window size.
    */
    public void Initialise() 
    {
        // Is map panel visible?
        // DEBUG System.out.println("MapPanel.initialize(): width/height: " + this.getBounds().width + " && " + this.getBounds().height );
        if ( this.getBounds().isEmpty() ) {
            // DEBUG System.out.println("MapPanel.initialize(): View " + this.spatialView.getViewName() + " height/width = 0");
            return;
        }
        // remember client view
        this.clientView.setSize(this.getBounds().getWidth(),
                                this.getBounds().getHeight());
        // SGG: Jan 2011, only set if Normalize did something
        // or no world to screen transform is set
        //
        boolean viewChanged = this.window.Normalize(this.clientView);
        // DEBUG System.out.println("MapPanel.initialize(): After normalise, viewChanged is: " + viewChanged + " isWorldToScreenSet()==" + this.isWorldToScreenSet());
        if ( viewChanged || this.isWorldToScreenSet()==false ) 
        {
            setWorldToScreenTransform( this.window.minX, 
                                       this.window.minY,
                                       this.window.maxX,
                                       this.window.maxY,
                                       this.getDimension() );
            // Request new image
            this.setRedrawBIOnly(REDRAW_BI.IMAGE_SIZE_CHANGE);
        }
    } // public _initialize

    public void Zoom(Envelope.zoom _zoom) 
    {
        // window is not initialize yet. do it (initializeLayersMBR does it)
        if ( this.window.isNull() ) {
            if (this.spatialView.initializeMBR(null) == false) {
                return;
            }
        }
        switch (_zoom) {
        case IN:
        case OUT: 
            this.window.ZoomOrPan(_zoom,this.getPanZoomPercentage());
            // request for new image
            this.redrawBIOnly = REDRAW_BI.IMAGE_SIZE_CHANGE;
            // IMAGE_SIZE_CHANGE incorporates - this.setWorldToScreenTransform();
            break;
        default:
            break;
        } // switch
        return;
    } // public Zoom

    private void presentSpatialSQLForFinalReview(final JGeometry _geometry) 
    {
        if ( _geometry == null )
            return;
        OracleConnection conn = null;
        conn = spatialView.getActiveLayer().getConnection();
        STRUCT stGeom = null;
        try {
            stGeom = (STRUCT)JGeometry.storeJS(conn,_geometry);
        } catch (Exception e) {
            return;
        }
        final String geometry = SDO_GEOMETRY.convertGeometryForClipboard(stGeom,conn);
        SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                  final ViewLayerTree         vlt = spatialView.getSVPanel().getViewLayerTree();
                  final SVSpatialLayer     sLayer = vlt.getQueryTarget();
                  SVGraphicLayer    sGraphicLayer = null;
                  SVQueryLayer        sQueryLayer = null;
                  final SpatialQueryReview dialog = new SpatialQueryReview(new JFrame(), true);
                  boolean                 success = false;
                  dialog.initDialog(sLayer, vlt.getSpatialOperator(), geometry);
                  dialog.setVisible(true);
                  if ( dialog.isCanceled() ) {
                      spatialView.getSVPanel().setNone();
                      return;
                  }
                  sQueryLayer = new SVQueryLayer(sLayer);   // Create from existing superclass
                  sQueryLayer.setVisibleName(vlt.getSpatialOperator().toString() + " - " + sLayer.getVisibleName());
                  sQueryLayer.setPrecision(dialog.getPrecision());
                  if ( dialog.targetGraphic() ) {
                      sGraphicLayer = new SVGraphicLayer(sLayer);  // Create from existing superclass
                      sGraphicLayer.setVisibleName(vlt.getSpatialOperator().toString() + " - " + sLayer.getVisibleName());
                      sGraphicLayer.setSQL("");
                      sGraphicLayer.add(sQueryLayer.getCache(window));
                      sGraphicLayer.setLayerMBR();
                      if ( mainPrefs.isRandomRendering()) {
                          sGraphicLayer.getStyling().setAllRandom();
                      }
                      success = spatialView.getSVPanel().addLayerToView(sGraphicLayer,false /*zoom*/);
                  } else {
                      sQueryLayer.setGeometry(_geometry);
                      sQueryLayer.setBufferDistance(dialog.getBufferDistance());
                      sQueryLayer.setBuffered(sQueryLayer.getBufferDistance()!=0.0);
                      sQueryLayer.setRelationshipMask(dialog.getRelationshipMask(sLayer.hasIndex()));
                      sQueryLayer.setSdoOperator(dialog.getSdoOperator());
                      sQueryLayer.setSQL(dialog.getQuerySQL());
                      sQueryLayer.getStyling().setShadeType(Styling.STYLING_TYPE.NONE);
                      sQueryLayer.setDraw(true);
                      if ( mainPrefs.isRandomRendering() ) {
                          sQueryLayer.getStyling().setAllRandom();
                      }
                      sQueryLayer.setMBRRecalculation(true);
                      // Add to view and ignore return
                      success = spatialView.addLayer(sQueryLayer,true,true,false /*zoom*/);
                  }
                  if ( success ) {
                      // repaint whole image as we have more to show
                      spatialView.getSVPanel().redraw();
                      if ( dialog.targetGraphic() ) {
                          // show attrib and geometry data in bottom tabbed pane
                          //
                          spatialView.getSVPanel().getAttDataView().showData(sGraphicLayer.getCache());
                          spatialView.getSVPanel().redraw();
                      } 
                  }
              } 
        });
    }
    
    private String presentShapeForFinalReview(JGeometry _geometry)
    {
        if ( _geometry == null )
            return null;
        OracleConnection conn = null;
        conn = spatialView.getActiveLayer().getConnection();
        STRUCT stGeom = null;
        try {
            stGeom = (STRUCT)JGeometry.storeJS(conn,_geometry);
        } catch (Exception e) {
            return null;
        }
        String returnVal = "";
        JFrame frame = new JFrame ();
        ShapeReviewDialog cd = new ShapeReviewDialog(frame, SDO_GEOMETRY.convertGeometryForClipboard(stGeom,conn));
        cd.pack();
        cd.setVisible(true);
        if ( ! cd.isCancelled() ) 
        {
            Tools.doClipboardCopy(cd.getFinalText());
            returnVal = cd.getFinalText();
        } else /* Cancel */ {
            returnVal = null;
        }
        frame.dispose();
        return returnVal;
    }   

    /**
     * @function queryByPoint
     * @param    _point
     * @author Matic
     * @author Simon Greener, May 9th 2010
     *          Modified to allow selection when only one layer (unselected) is in list.
     * @author Simon Greener, June 7th 2010
     *          Modified to handle changes to SVSpatialLayer.queryByPoint() and showGeometry
     * @author Simon Greener, November 2010
     *          Changed name to queryByPoint()
     */
    public void queryByPoint(Point2D _point,
                             boolean _leave)
    {
        if ( _point == null ) {
            return;
        }
        // get selected layer
        //
        SVSpatialLayer sLayer = this.spatialView.getActiveLayer();
        if (sLayer == null) 
        {
            // allow selection if only one layer in list
            //
            if ( this.spatialView.getLayerCount() == 1 ) {
                sLayer = (SVSpatialLayer)this.spatialView.getLayerList().values().toArray()[0];
                this.spatialView.setActiveLayer(sLayer);
            } 
        }
        if ( sLayer == null || ( ! sLayer.isDraw() ) ) {
            JOptionPane.showMessageDialog(null,
                                          messageNoQueryableLayer,
                                          this.propertyManager.getMsg("ERROR_MESSAGE_DIALOG_TITLE"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<QueryRow> aList = sLayer.queryByPoint(_point, this.getSearchPixels());

        if ((aList == null) || (aList.size() == 0)) {
            Toolkit.getDefaultToolkit().beep();
            // Clear display anyway
            this.spatialView.getSVPanel().getAttDataView().clear();
            return;
        }

        // Show geometry objects
        //
        this.spatialView.getSVPanel().showGeometry(sLayer,
                                                   null, 
                                                   aList, 
                                                   new Envelope(aList),
                                                   true /* Selection Colouring*/, 
                                                   false /*Zoom To Selection*/,
                                                   _leave);

        // show attrib data in bottom tabbed pane
        //
        this.spatialView.getSVPanel().getAttDataView().showData(aList);
    }
    
    public int getMapScale() {
        try {
            if ( scaleBar==null ) {
                scaleBar = new ScaleBar(this.mapBackground);
            }
            return scaleBar.GetMapScale(window,
                                        getPixelSize().getX(),
                                        this.spatialView.getSRIDBaseUnitType(),
                                        this.spatialView.isGeodetic() );
        } catch (NoninvertibleTransformException e) {
            return 0;
        }
    }
    
    /**
     * Timer to perform paint of buffered image to screen image every X miliseconds
     */
    class RefreshBFTask extends TimerTask {
        //public static final int REFRESH_BF_SLEEP_TIME = 1000;
        protected Graphics2D g2d;

        public RefreshBFTask(Graphics2D _g2d) {
            this.g2d = _g2d;
        }

        public void run() {
            do {
                // refresh image on screen - draw buffered image
                refreshAll();
                try {
                    Preferences grp = MainSettings.getInstance().getPreferences();
                    Thread.sleep(grp.getImageRefreshMS());
                } catch (Exception _e) {
                }
                // repeat until we read data
            } while (redrawBIOnly == REDRAW_BI.ONLY_PAINT);
        }
    }

      class DrawTask 
    extends TimerTask 
    {
        protected Graphics2D       g2d;
        protected SpatialViewPanel svp;
        protected SpatialView      spatialView;
        protected MapPanel         mapPanel;

        public DrawTask(Graphics2D      _g2d,
                        SpatialView     _spatialView) 
        {
            this.g2d         = _g2d;
            this.spatialView = _spatialView;
            this.svp         = _spatialView.getSVPanel();
            this.mapPanel    = _spatialView.getMapPanel();
        }

        public void run() {
            LOGGER.debug(" ****** START DrawTask.run()");
            //LOGGER.debug(" ******** WorldToScreen ? " + this.mapPanel.isWorldToScreenSet());
            if ( ! this.mapPanel.isWorldToScreenSet() ) {
                //LOGGER.debug(" ******** Setting World To Screen with " + this.spatialView.getMBR().toString());
                this.mapPanel.setWindow(this.spatialView.getMBR());
                //LOGGER.debug(" ******** World To Screen is set ? " + this.mapPanel.isWorldToScreenSet());
            }

            // we are in read data mode
            redrawBIOnly = REDRAW_BI.ONLY_PAINT;

            // start thread for refresh screen image
            Timer refreshBFTask = new Timer();
            refreshBFTask.schedule(new RefreshBFTask(this.g2d), 0);
            
            // refresh progress bar
            this.svp.setProgressBar(true, null, -1, -1);

            // Draw layers from last to the first one
            // by getting iterator starting a maximum size
            // and "unwinding" the layers in the list via litr.hasPrevious()/litr.previous()
            //
            ArrayList<SVSpatialLayer> tempAR = new ArrayList<SVSpatialLayer>(this.spatialView.getLayerList().values());
            if ( ! this.mapPanel.isWorldToScreenSet() ) {
                // finish layer draw
                this.svp.setProgressBar(false, null, -1, -1);
                redrawBIOnly = REDRAW_BI.FINISH;
                // reset cancel operation
                this.svp.setCancelOperation(true);
                // Make sure next/prev etc buttons are correct
                this.svp.setToolbarStatus();
                refreshAll();
                return;
            }
            
            LOGGER.debug("******** tempAR.size "+(tempAR==null?"null":tempAR.size()));
            ListIterator<SVSpatialLayer> litr = tempAR.listIterator(tempAR.size());
            int currentlLayer = 0,
            maxDrawableLayers = svp.getViewLayerTree().getDrawableLayerCount(this.spatialView.getViewName());
            svp.setMessage(this.spatialView.getViewName(),false);
            while (litr.hasPrevious() && (this.svp.isCancelOperation() == false)) {
                SVSpatialLayer layer = litr.previous();
                LOGGER.debug("******** MapPanel.drawTask: " + layer.getLayerName() + " isDraw=" + layer.isDraw());
                if (layer.isDraw()) 
                {
                    // update progress bar
                    currentlLayer++;
                    this.svp.setProgressBar(true,
                                            layer.getLayerName(),
                                            currentlLayer,
                                            maxDrawableLayers);
                    if ( layer.hasIndex()==false ) {
                        this.svp.setMessage("No Index: " + layer.getVisibleName(), true);
                    }
                    boolean status = layer.drawLayer(this.mapPanel.getWindow(), getBiG2D());
                    if (status == false) {
                        LOGGER.warn(layer.getVisibleName() + " failed to draw correctly");
                    } else {
                        LOGGER.debug("******** " + layer.getVisibleName() + " draw successful");
                    }
                }
            }

            // perform after draw operations
            if (afterLayerDrawList != null && 
                afterLayerDrawList.size()>0) 
            {
                LOGGER.debug("******** AfterLayerDraw of " + afterLayerDrawList.size() + " layers.");
                AfterLayerDraw ald = null;
                Iterator<AfterLayerDraw> iter = afterLayerDrawList.iterator();
                while (iter.hasNext()) {
                    ald = iter.next();
                    ald.draw(biG2D);
                    // if layer is selected for removal after draw, remove it
                    if (ald.getRemoveAfterDraw()) {
                        iter.remove();
                    }
                }
            }
            
            if ( this.spatialView.isScaleBar() && window.isSet() ) 
            {
                // Draw scale bar
                try {
                    if (scaleBar==null) {
                        scaleBar = new ScaleBar(mapBackground);
                    }
                    biG2D.drawImage(toImage(scaleBar.getFixedScaleBar(window,
                                                                      getPixelSize().getX(),
                                                                      this.spatialView.getSRIDBaseUnitType(),
                                                                      this.spatialView.getPrecision(false),
                                                                      this.spatialView.isGeodetic())
                                            ),                                                                            
                                    5,
                                    getDimension().height-40,
                                    null);
                    if ( mainPrefs.isMapScaleVisible() ) {
                        this.svp.setScale("1:"+getMapScale());
                    } else {
                        this.svp.setScale("");
                    }
                } catch (NoninvertibleTransformException e) {
                }
            }
            // finish layer draw

            // finish progress bar
            this.svp.setProgressBar(false, null, -1, -1);
            redrawBIOnly = REDRAW_BI.FINISH;
            // reset cancel operation
            this.svp.setCancelOperation(false);
            // Make sure next/prev etc buttons are correct
            this.svp.setToolbarStatus();
            refreshAll();
            LOGGER.debug(" ****** FINISH DrawTask.run()");
        }
    }
    
    class ShapeReviewDialog extends JDialog
    {   
      private static final long serialVersionUID = 7068062067910673449L;
      
      private String originalSdoGeometryString = "";
    
      private JPanel createPanel = new JPanel(new VerticalFlowLayout());
      private JLabel lblQuestion = new  JLabel();
      private JTextArea displayText = new JTextArea();
      private JScrollPane scrollPane = new JScrollPane(displayText);
      
      private JPanel pnlPrecision = new JPanel();
      private JLabel lblPrecision = new JLabel();
      private JRadioButton rbPrecisionView = new JRadioButton();
      private JRadioButton rbPrecisionNone = new JRadioButton();
      private ButtonGroup bgPrecision = new ButtonGroup();
    
      private JTextField tfBufferDistance = new JTextField("0");
      private JLabel lblBuffer = new JLabel("Buffer Distance:");
      private JPanel  pnlButtons       = new JPanel(new GridLayout(1,2));
      private JButton btnCopyClipboard = new JButton();
      private JButton btnCancel        = new JButton();
      
      private DecimalFormat dFormat    = null;
    
      private boolean CANCELLED = false;
      
      /** Creates the reusable dialog. */
      public ShapeReviewDialog(Frame   _aFrame,
                               String  _text) 
      {
          super(_aFrame, true);
          
          setTitle(propertyManager.getMsg("COPY_TO_CLIPBOARD_TITLE"));
    
          this.originalSdoGeometryString = _text;
          
          this.dFormat = Tools.getDecimalFormatter(spatialView.getPrecision(false),false);
          
          this.lblQuestion.setText(propertyManager.getMsg("COPY_TO_CLIPBOARD_TITLE"));
          this.displayText.setEditable(false);
          this.displayText.setEnabled(true);
          this.displayText.setLineWrap(true);
          this.displayText.setText(SDO_GEOMETRY.applyPrecision(_text,dFormat,8 /* 2D * 4 coords = 8 ordinates */));
          this.displayText.setWrapStyleWord(true);
          
          this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
          this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
          this.scrollPane.setViewportView(this.displayText);
          this.scrollPane.setPreferredSize(new java.awt.Dimension(350,300));
          this.scrollPane.setAutoscrolls(true);
            
          bgPrecision.add(rbPrecisionView);
          rbPrecisionView.setText(propertyManager.getMsg("rbPrecisionView") + " (" + spatialView.getPrecision(false) + ")");
          rbPrecisionView.setSelected(true);
          rbPrecisionView.addActionListener(new java.awt.event.ActionListener() {
              public void actionPerformed(java.awt.event.ActionEvent evt) {
                  modifyGeometry();
              }
          });
    
          bgPrecision.add(rbPrecisionNone);
          rbPrecisionNone.setText(propertyManager.getMsg("rbPrecisionNone"));
          rbPrecisionNone.addActionListener(new java.awt.event.ActionListener() {
              public void actionPerformed(java.awt.event.ActionEvent evt) {
                  modifyGeometry();
              }
          });
          lblPrecision.setLabelFor(rbPrecisionView);
          lblPrecision.setText(propertyManager.getMsg("lblPrecision"));
          pnlPrecision.add(lblPrecision);
          pnlPrecision.add(rbPrecisionView);
          pnlPrecision.add(rbPrecisionNone);
        
          this.lblBuffer.setText(propertyManager.getMsg("BUFFER_DISTANCE"));
          this.lblBuffer.setLabelFor(this.tfBufferDistance);
          
          this.tfBufferDistance.setText(String.valueOf(0));
          this.tfBufferDistance.setMinimumSize(new Dimension(30,22));
          this.tfBufferDistance.setPreferredSize(new Dimension(30,22));
          this.tfBufferDistance.setMaximumSize(new Dimension(30,22));
          this.tfBufferDistance.setInputVerifier(new InputVerifier() {
              public boolean verify(JComponent comp) {
                  boolean returnValue = true;
                  JTextField textField = (JTextField)comp;
                  try {
                      // This will throw an exception if the value is not an integer
                      double size = Double.parseDouble(textField.getText());
                      if ( size < 0 )
                          throw new NumberFormatException(propertyManager.getMsg("ERROR_BUFFER_SIZE"));
                      modifyGeometry();
                  } catch (NumberFormatException e) {
                      JOptionPane.showMessageDialog(null,
                                                    e.getMessage(),
                                                    MainSettings.EXTENSION_NAME,
                                                    JOptionPane.ERROR_MESSAGE);
                      returnValue = false;
                  }
                  return returnValue;
              }
          });
    
          this.btnCopyClipboard.setText(propertyManager.getMsg("BUTTON_COPY_TO_CLIPBOARD"));
          this.btnCancel.setText(propertyManager.getMsg("BUTTON_CANCEL"));
          this.pnlButtons.add(this.btnCopyClipboard);
          this.pnlButtons.add(this.btnCancel);
          
          createPanel.add(this.lblQuestion      );
          createPanel.add(this.scrollPane       );
          createPanel.add(this.pnlPrecision     );
          createPanel.add(this.lblBuffer        );
          createPanel.add(this.tfBufferDistance );
          createPanel.add(this.pnlButtons       );
          
          // Apply defaults
          modifyGeometry();
          
          //Make this dialog display it.
          this.setContentPane(this.createPanel);
          this.setSize(360,350);
          this.setResizable(false);
          this.setAlwaysOnTop(true);
          this.pack();
    
          //Handle window closing correctly.
          this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    
          //Register an event handler that reacts to buttons being pressed
          btnCopyClipboard.addActionListener(new java.awt.event.ActionListener() {
              public void actionPerformed(java.awt.event.ActionEvent evt) {
                  CANCELLED = false;
                  setVisible(false);
              }
          });
    
          btnCancel.addActionListener(new java.awt.event.ActionListener() {
              public void actionPerformed(java.awt.event.ActionEvent evt) {
                  CANCELLED = true;
                  setVisible(false);
              }
          });
          
      }
      
      public boolean isCancelled () {
          return this.CANCELLED;        
      }
      
      private void modifyGeometry() 
      {
        String displayString = this.originalSdoGeometryString;
        if ( this.rbPrecisionView.isSelected() ) {
          displayString =
                    SDO_GEOMETRY.applyPrecision(displayString,dFormat,8 /* 2D * 4 coords = 8 ordinates */ );
        }
        
        if ( Double.valueOf(this.tfBufferDistance.getText()) > 0 ) {
          String lengthUnits  = Tools.getViewUnits(spatialView,Constants.MEASURE.LENGTH);
          String bufferParams = ( ( spatialView.getSRID().equals(Constants.NULL) || Strings.isEmpty(lengthUnits) )
                                  ?   ")"
                                  : ",'unit="+lengthUnits  + "')" );
          /** 
           * SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY,
           *                     dist IN NUMBER,
           *                     tol IN NUMBER
           *                     [, params IN VARCHAR2]
           *                    ) RETURN SDO_GEOMETRY;
           */
          displayString = "SDO_GEOM.SDO_BUFFER(" + displayString + "," + 
                                               this.tfBufferDistance.getText() + "," + 
                                               String.format("%f",(float)(1f/Math.pow(10,dFormat.getMaximumFractionDigits()))) +
                                               bufferParams;
        }
        this.displayText.setText(displayString);
      }
      
      public String getCopyButtonText() {
        return this.btnCopyClipboard.getText();
      }
    
      public String getFinalText() {
          return this.displayText.getText();        
      }
      
    }
  
}
