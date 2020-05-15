package org.GeoRaptor.SpatialView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import oracle.jdbc.OracleConnection;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.layers.SVGraphicLayer;
import org.GeoRaptor.SpatialView.layers.SVQueryLayer;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayer;
import org.GeoRaptor.SpatialView.layers.SVWorksheetLayer;
import org.GeoRaptor.io.ExtensionFileFilter;
import org.GeoRaptor.sql.DatabaseConnections;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * @class SpatialView
 * @author Simon Greener, July 2010, Original coding
 * @description This class defines the properties of a single spatial view.
 * There can be as many instances as spatial views the user wishes to define.
 * Each view has:
 *   1. A unique name and a visible name (that describes its JTree node).
 *   2. A single SRID
 *   3. A list of layers in the view
 *   4. A MapPanel into which are drawn the spatial data.
 */
public class SpatialView {

    private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.SpatialView.SpatialView");

    SpatialViewPanel svp = null;
  
    private UUID viewID;
  
  /**
   * View Name
   */
  protected static final String namePrefix = "SRID:";
  protected String name = "";
  protected String visibleName = "";
  
  /**
   * SRID and its Properties
   */
  protected String SRID                  = Constants.NULL;
  protected Constants.SRID_TYPE SRIDType = Constants.SRID_TYPE.UNKNOWN;
  protected String SRIDBaseUnitType      = "M";
  protected String distanceUnitType      = "M";
  protected String areaUnitType          = "SQ_M";
  protected boolean scaleBar             = true;

  /**
   * Its associated Panel into which images are drawn 
   */
  protected MapPanel mPanel;

  /** 
   * Properties File Manager
   **/
  private static final String propertiesFile = "org.GeoRaptor.SpatialView.resource.SpatialView";
  protected PropertiesManager propertyManager = null;

  /**
   * Number conversion properties
   * Default fraction digits for this view/srid
   */
  private final int initialPrecision = -9;
  private final int defaultPrecision = 3;
  private int precision = initialPrecision;

  /** 
   * Constructors
   */
  public SpatialView(SpatialViewPanel _svp) 
  {
      try {
          // Create link to master, controlling panel
          //
          this.svp = _svp;
          
          // Get localisation file
          //
          try {
            this.propertyManager = new PropertiesManager(SpatialView.propertiesFile);
          } catch (Exception e) {
            LOGGER.warn("SpatialView(SpatialViewPanel Constructor Failure: " + e.getMessage());
          }

          // Create unique ID
          this.viewID = UUID.randomUUID();    
      
          // Create Layer List 
          // In access-ordered linked hash maps, merely querying a map with the get method 
          // is a structural modification that can potentially throw ConcurrentModificationException.
          // So, layerList must be declared as insertion-ordered
          //
          this.createMapPanel();
          this.setVisible();
          // Set all toolbar buttons to be disabled until first layer added
          //
          this.svp.setToolbarEnabled(false);
          // Make sure status bar is reset to display nothing.
          this.svp.updateViewStatusMBR(new Envelope(this.getDefaultPrecision()),this.defaultPrecision);          
      } catch (NullPointerException npe) {
         LOGGER.error("Caught Null Pointer Exception in SpatialView(SpatialViewPanel)");
         // npe.printStackTrace();
      }
  }

  private void createMapPanel() {
    this.mPanel = new MapPanel(this /*this.svp*/);
    this.mPanel.addMouseListener(this.mPanel);
    this.mPanel.addMouseMotionListener(this.mPanel);
    this.mPanel.addMouseWheelListener(this.mPanel); 
  }

  public SpatialView(SpatialViewPanel _svp,
                     String           _name,
                     String           _srid) 
  {
      this(_svp);
      this.setSRID(_srid);
      this.setViewName(_name);
      this.setVisibleName(this.getViewName());
  }

  public SpatialView(SpatialViewPanel _svp,
                     String           _XML)
  {
      this(_svp);
      try 
      {
          Document doc = null;
          DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
          dbf.setNamespaceAware(false);
          dbf.setValidating(false);
          DocumentBuilder db = dbf.newDocumentBuilder();
          doc = db.parse(new InputSource(new StringReader(_XML)));          
          XPath xpath = XPathFactory.newInstance().newXPath();
          this.fromXMLNode((Node)xpath.evaluate("/View",doc,XPathConstants.NODE));
      } catch (XPathExpressionException xe) {
          System.out.println("XPathExpressionException " + xe.toString());
      } catch (ParserConfigurationException pe) {
          System.out.println("ParserConfigurationException " + pe.toString());
      } catch (SAXException se) {
          System.out.println("SAXException " + se.toString());
      } catch (IOException ioe) { 
          System.out.println("IOException " + ioe.toString());
      }
  }

  public SpatialView(SpatialViewPanel _svp,
                     Node             _node)
  {
      this(_svp);
      this.fromXMLNode(_node);
  }
  
    // Copy constructor
    public SpatialView(SpatialView _copyView) {
        this(_copyView.getSVPanel());
        this.setSRID(_copyView.getSRID());
        this.setViewName(_copyView.getViewName());
        this.setVisibleName(_copyView.getVisibleName());
        this.setSRIDType(_copyView.getSRIDType());
        this.setSRIDBaseUnitType(_copyView.getSRIDBaseUnitType());
        this.setDistanceUnitType(_copyView.getDistanceUnitType());
        this.setAreaUnitType(_copyView.getAreaUnitType());
        this.setPrecision(_copyView.getPrecision(false));
        this.getMapPanel().setRenderHint(_copyView.getMapPanel().getRenderHint());
        this.setMBR(_copyView.getMBR());
        this.getMapPanel().setMapBackground(_copyView.getMapPanel().getMapBackground());
        this.setScaleBar(_copyView.isScaleBar());
    }

  public boolean equals(Object obj)
  {
    //if the two objects are equal in reference, they are equal
    if (this == obj)
      return true;
    if (obj instanceof SpatialView)
    {
      SpatialView view = (SpatialView) obj;
      return view.getViewName().equals( this.getViewName() );
    }
    else
    {
      return false;
    }
  }

  private void fromXMLNode(Node _node)
  {
      if ( _node == null || _node.getNodeName().equals("View")==false) {
          System.out.println("Node is null or not View");
          return;  // Should throw error
      }
      try 
      {
          // Shohuld never happen but....
          if ( this.getMapPanel()==null ) {
              this.createMapPanel();
          }
          XPath xpath = XPathFactory.newInstance().newXPath();
          this.setSRID((String)xpath.evaluate("SRID/text()",_node,XPathConstants.STRING));
          this.setViewName((String)xpath.evaluate("Name/text()",_node,XPathConstants.STRING));
          this.setVisibleName((String)xpath.evaluate("VisibleName/text()",_node,XPathConstants.STRING));
          this.setSRIDType((String)xpath.evaluate("SRIDType/text()",_node,XPathConstants.STRING));
          this.setSRIDBaseUnitType((String)xpath.evaluate("SRIDBaseUnitType/text()",_node,XPathConstants.STRING));
          this.setDistanceUnitType((String)xpath.evaluate("DistanceUnitType/text()",_node,XPathConstants.STRING));
          this.setAreaUnitType((String)xpath.evaluate("AreaUnitType/text()",_node,XPathConstants.STRING));
          this.setPrecision((String)xpath.evaluate("MaxFractionDigits/text()",_node,XPathConstants.STRING));  // Deprecated
          this.setPrecision((String)xpath.evaluate("Precision/text()",_node,XPathConstants.STRING));
          this.getMapPanel().setRenderHint((String)xpath.evaluate("RenderingHint/text()",_node,XPathConstants.STRING));
          this.setMBR(new Envelope((Node)xpath.evaluate("MBR",_node,XPathConstants.NODE)));
          String mapBackground = (String)xpath.evaluate("MapBackground/text()",_node,XPathConstants.STRING);
          this.getMapPanel().setMapBackground(mapBackground);
//          if ( this.getViewName().equals("SRID:2872") )
//Tools.doClipboardCopy("SpatialView " + this.getViewName() + " mapBackground="+mapBackground + " = String = " + this.getMapPanel().getMapBackground().toString() + " MBR " + this.getMBR().toString());
          this.setScaleBar((String)xpath.evaluate("ScaleBar/text()",_node,XPathConstants.STRING));
      } catch (XPathExpressionException xe) {
          LOGGER.error("SpatialView.fromXMLNode()" + xe.getMessage());
          xe.printStackTrace();
      }
  }

  public boolean isActive() {
     return ( this == this.getSVPanel().getActiveView() );
  }
  
  public void setActiveLayer(SVSpatialLayer _layer) {
      if ( _layer == null ) {
          return;
      }
      svp.getViewLayerTree().setLayerActive(_layer.getLayerName());
  }

  public SVSpatialLayer getActiveLayer() {
      return this.svp.getViewLayerTree().getActiveLayer(this.getViewName());
  }

  public windowNavigator getWindowNavigator() {
    return this.mPanel.windowNavigator;
  }

  public MapPanel getMapPanel() {
    return this.mPanel;
  }
  
  public void removeMapPanel() {
      this.mPanel.setVisible(false);
      this.mPanel.spatialView = null;
      this.mPanel.propertyManager = null;
      this.mPanel.biG2D = null;
      this.mPanel.worldToScreenTransform = null;
      this.mPanel.biImage = null;
      this.mPanel.biBasic = null;
      this.mPanel.bi = null;
      this.mPanel.windowNavigator = null;
      this.mPanel.window = null;
      this.mPanel.clientView = null;
      this.mPanel.startScreen = null;
      this.mPanel.currentScreen = null;
      this.mPanel.startWorld = null;
      this.mPanel.mapBackground = null;
      this.mPanel.createShapeWorld = null;
      this.mPanel.createShapeScreen = null;
      this.mPanel.lastScreen = null;
      this.mPanel.qualityHints = null;
      this.mPanel.drawTimer = null;
      this.mPanel.afterLayerDrawList = null;
      this.mPanel.iconNoQueryableLayer = null; 
      this.mPanel.messageNoQueryableLayer = null;
      this.mPanel = null;
  }
  
  public SpatialViewPanel getSVPanel() {
    return this.svp;
  }

  public int getLayerCount() {
      return this.svp.getViewLayerTree().getLayerCount(this.getViewName());
  }

  public SVSpatialLayer getFirstLayer() {
      if ( this.getLayerCount() == 0 )
          return null;
      LinkedHashMap<String,SVSpatialLayer> layerList = this.svp.getViewLayerTree().getLayers(this.getViewName());
      if ( layerList == null )
          return null;
      Iterator<SVSpatialLayer> it = layerList.values().iterator();
      SVSpatialLayer retLayer = null;
      if (it.hasNext()) {
          retLayer = it.next();
      }
      return retLayer;
  }
  
  public Map<String, SVSpatialLayer> getLayerList() {
      return this.svp.getViewLayerTree().getLayers(this.getViewName());
  }

  protected SVSpatialLayer getLayer(String _layerName) {
      if ( this.svp.getViewLayerTree().getLayerCount(this.getViewName()) == 0 )
          return null;
      SVSpatialLayer retLayer = null;
      LinkedHashMap<String,SVSpatialLayer> layerList = this.svp.getViewLayerTree().getLayers(this.getViewName());
      if ( layerList == null )
          return null;
      Iterator<SVSpatialLayer> it = layerList.values().iterator();
      while (it.hasNext()) {
          retLayer = it.next();
          if (retLayer.getLayerName().equalsIgnoreCase(_layerName)) {
              return retLayer;
          }
      }
      return null;
  }

  public void removeLayer(String _layerName,
                          boolean _confirm) {
LOGGER.debug("*** Removing " + _layerName);
      this.svp.getViewLayerTree().removeLayer(this.getViewName(), _layerName, _confirm);
  }
  
  public void removeAllLayers() {
      this.svp.getViewLayerTree().removeView(this.getViewName(),false/*Only Layers*/);
  }
  
  public ViewLayerTree.ViewNode getViewNode() {
      return this.svp.getViewLayerTree().getViewNode(this.name);
  }

  public void collapseViewNode() {
      if ( this.svp.getViewLayerTree().getViewNode(this.name) != null )
        this.svp.getViewLayerTree().getViewNode(this.name).collapse();
  }        
        
  public boolean addLayer(SVSpatialLayer _layer,
                          boolean        _isDrawn,
                          boolean        _isActive,
                          boolean        _zoom) 
  {
      ViewLayerTree.ViewNode vNode = this.svp.getViewLayerTree().getViewNode(this.name);
      if ( vNode == null ) {
          LOGGER.info("Layer " + _layer.getVisibleName() + " could not be add to "+this.getVisibleName());
          return false;
      }
      vNode.addLayer(_layer,_isDrawn,_isActive);
      // Set view reference in new layer
      _layer.setView(this);
      // Compute precision and initialize View's MBR when first new layer is added.
      if ( this.getLayerCount() == 1 ) 
      {
          this.setPrecision(_layer.getPrecision(true));  // Only one layer: assign its precision to view
          if ( ! this.initializeMBR(_layer) ) {
              LOGGER.info(this.getVisibleName() + "'s MBR could not be set for layer " + _layer.getVisibleName());
          }
      } else if ( _zoom ) {
          this.setMBR(_layer.getMBR()); // Sets associated map extent calling Initialise()
      }
      return true;
  }

    
  /**
    * Check if layer with this name already exist. If exist, add "_OccureNumber" after name.
    * @param _name layer name
    * @return new layer name (can be the some one as _name)
    */
  public String checkLayerName(String _name) {
      return this.svp.getViewLayerTree().checkName(_name);
  }

  public PropertiesManager getPropertyManager() {
    return this.propertyManager;
  }
  
  public static String createViewName(String _SRID) {
      return namePrefix + (Strings.isEmpty(_SRID) ? Constants.NULL : _SRID);
  }

  public static String createViewName(int _SRID) {
      return namePrefix + Integer.valueOf(_SRID);
  }
  
  protected void setViewName(String _name) {
    this.name = Strings.isEmpty(_name) ? namePrefix + this.getSRID() : _name;
  }

  public String getViewName(){
    return this.name;
  }

  protected void setVisibleName(String _name) {
    this.visibleName = Strings.isEmpty(_name) ? namePrefix + this.getSRID() : _name;
  }

  public String getVisibleName(){
    return this.visibleName;
  }
  
  public UUID getViewID(){
    return this.viewID;
  }
  
  protected void setSRID(String _srid) {
      if (Strings.isEmpty(_srid) ) {
          this.SRID = Constants.NULL;
          this.SRIDType = Constants.SRID_TYPE.PROJECTED; // Assume NULL is projected
          this.distanceUnitType = "M";
          this.SRIDBaseUnitType = "M";
      } else {
          this.SRID = _srid;
      }
  }

  public boolean isGeodetic() {
      return getSRIDType().toString().toUpperCase().startsWith("GEO");      
  }
  
  public Constants.SRID_TYPE getSRIDType() {
      return this.SRIDType == null ? Constants.SRID_TYPE.UNKNOWN : this.SRIDType;
  }
  
  public void setSRIDType(String _SRIDType) {
      try {
        this.SRIDType =
                Strings.isEmpty(_SRIDType) 
                          ? Constants.SRID_TYPE.UNKNOWN 
                          : Constants.SRID_TYPE.valueOf(_SRIDType);
      } catch (Exception e) {
          this.SRIDType = Constants.SRID_TYPE.UNKNOWN;
      }
  }
  
  public void setSRIDType(Constants.SRID_TYPE _SRIDType) {
    this.SRIDType = _SRIDType;
  }

  public OracleConnection getConnection() {
      OracleConnection c = null;
      try {
          if ( this.getLayerCount()!=0 ) {
              c = this.getActiveLayer().getConnection();
          }
          if ( c==null ) {
              // Get any connection though it should relate to the actual owner of object being queried...
              // unless query is for a generic purpose such as calculating distance etc.
              c = DatabaseConnections.getInstance().getAnyOpenConnection();
          }
      } catch ( IllegalStateException ise ) {
        // Do nothing -- startup
      }
      return c;
  }

    public String getConnectionName() {
        String c = null;
        try {
            if ( this.getLayerCount()!=0 ) {
                c = this.getActiveLayer().getConnectionName();
            }
            if (Strings.isEmpty(c) ) {
                // Get any connection though it should relate to the actual owner of object being queried...
                // unless query is for a generic purpose such as calculating distance etc.
                c = DatabaseConnections.getActiveConnectionName();
            }
        } catch ( IllegalStateException ise ) {
          // Do nothing -- startup
        }
        return c;
    }

  public void setSRIDType() 
  {
      if ( this.getSRID().equalsIgnoreCase(Constants.NULL) ) {
        this.setSRIDType(Constants.SRID_TYPE.PROJECTED);
        return;
      }
      if (this.SRIDType == Constants.SRID_TYPE.UNKNOWN) 
      {
          this.SRIDType = Tools.discoverSRIDType(this.getConnection(),
                                                 this.SRID);
      }
  }

  public String getSRIDBaseUnitType() {
      return this.SRIDBaseUnitType;
  }

  public void setSRIDBaseUnitType(String _baseUnitType) {
      this.SRIDBaseUnitType = _baseUnitType==null?"M":_baseUnitType;
  }

  public String getAreaUnitType() {
    return this.areaUnitType;
  }

  public void setAreaUnitType(String _areaUnitType) {
      this.areaUnitType = _areaUnitType;
  }

  public void setAreaUnitType() 
  {
      this.areaUnitType = "SQ_" + this.distanceUnitType;
  }

  public String getDistanceUnitType() {
    return this.distanceUnitType;
  }

  public void setDistanceUnitType(String _DistanceUnitType) {
      this.distanceUnitType = _DistanceUnitType;
  }
  
  public void setDistanceUnitType() 
  {
      // Default is meters
      //
      this.SRIDBaseUnitType = "M";
      this.distanceUnitType = "M";
      if ( this.getSRID().equalsIgnoreCase(Constants.NULL) ) {
        return;
      } 
      try 
      {
          Connection c = this.getConnection();
          if ( c == null )  // There is no connection available. Perhaps we are starting up? 
              return;
          String SRIDUOM = MetadataTool.getSRIDBaseUnitOfMeasure(c,this.getSRID());
          if (Strings.isEmpty(SRIDUOM)) { 
              this.SRIDBaseUnitType = "M";
              this.distanceUnitType = "M";
          } else {
              this.setSRIDBaseUnitType(SRIDUOM.replace("metre","M"));  // To alighn with current valid distance measurements as defined by SDO_DIST_UNIT.SDO_UNIT
              this.distanceUnitType =  SRIDUOM.replace("metre","M");
          }
      } catch (IllegalArgumentException iae) {
        JOptionPane.showMessageDialog(null,
                                  this.propertyManager.getMsg("ARGUMENT_ERROR", iae.getMessage()),
                                  MainSettings.EXTENSION_NAME,
                                  JOptionPane.ERROR_MESSAGE);
          
      } catch (SQLException sqlex) {
        JOptionPane.showMessageDialog(null,
                                  "setDistanceUnitType: " + this.propertyManager.getMsg("QUERY_ERROR", sqlex.getMessage()),
                                  MainSettings.EXTENSION_NAME,
                                  JOptionPane.ERROR_MESSAGE);
      }
  }
  
  public String getSRID() {
    return Strings.isEmpty(this.SRID) ? Constants.NULL : this.SRID;
  }
  
  public int getSRIDAsInteger() {
    return this.getSRID().equals(Constants.NULL) 
         ? Constants.SRID_NULL 
         : (Integer.valueOf(this.getSRID()).intValue()==0
            ? Constants.SRID_NULL 
            : Integer.valueOf(this.getSRID()).intValue());
  }
  
    
    public Envelope getMBR() {
        return this.mPanel.getWindow();
    }

    // **************************************** MBR ********************************

    /** Sets associated map's extent to the extent provided.
     *  Will also set world to page transformation.
     * @param _mbr
     */
    public void setMBR(Envelope _mbr) {
        if (_mbr==null) {
            return;
        }
        this.mPanel.setWindow(_mbr);
    }
    
    public void setMBR(double _minX, double _minY,
                       double _maxX, double _maxY) {
        this.setMBR(new Envelope(_minX,_minY,_maxX,_maxY));
    }

    /**
     * Set initialite world parameters for provided layer or, if null, all layers.
     * @param _layer : Layer whose MBR is to be used to set extent of current view/map
     *                 If _layer is null, all layers in view will be used to compute a new MBR
     */
    protected boolean initializeMBR(SVSpatialLayer _layer) 
    {
        // get layer's MBR
        Envelope mbr = null;
        if ( _layer != null ) {
            mbr = _layer.getMBR();
        } else {
            mbr = this.getMBRForDrawLayers();   
        }
        
        if (mbr == null || mbr.isSet()==false )  {
            LOGGER.debug(this.getViewName() + " View's mbr is not set.");
            return false;
        }
        // Ensure one side is not 0 in length
        //
        if ( mbr.getWidth()==0 || mbr.getHeight()==0 ) {
            double halfSide = Math.max(mbr.getWidth(),mbr.getHeight()) / 2.0;
            if (halfSide==0.0f) {
                halfSide -= 0.05;
            }
            mbr = new Envelope(mbr.centre().getX() - halfSide,
                                      mbr.centre().getY() - halfSide,
                                      mbr.centre().getX() + halfSide,
                                      mbr.centre().getY() + halfSide);
        }
        this.setMBR(mbr);
        return true;
    }
    
    /**
     * Return MBR for all layers ticked for drawing.
     * If right mouse click ZoomToLayer has occurred, getZoomToLayer() will be set forcing this function to only
     * generate an MBR for that layer only.
     * @return array of MBR coordinates (minx, maxx, miny, maxy) or null if no layer is select for draw
     */
    public Envelope getMBRForDrawLayers() 
    {
        Envelope returnMBR = new Envelope(this.getDefaultPrecision());
        // Find MBR from what is ticked for draw.
        //
        SVSpatialLayer layer = null;
        if ( this.svp.getViewLayerTree().getLayerCount(this.getViewName()) > 0 ) 
        {
            LinkedHashMap<String,SVSpatialLayer> layerList = this.svp.getViewLayerTree().getLayers(this.getViewName());
            if ( layerList == null ) {
                return null;
            }
            Iterator<SVSpatialLayer> layerListIt = layerList.values().iterator();
            while (layerListIt.hasNext())
            {
                layer = layerListIt.next();
                // This is a layer that is the target of the zoomTo operation
                //
                if (layer.getMBR().isSet() && layer.isDraw()) {
                    returnMBR.setMaxMBR(layer.getMBR());
                }
            }
        }
        return (returnMBR.isSet() ? returnMBR : null);
    }

    public void setVisible() {
      // set this view active?
      this.mPanel.setVisible(true);
    }
    
    public void setInvisible() {
      this.mPanel.setVisible(false);
    }
  
    public double getTolerance() 
    {
        double tolerance = Double.MAX_VALUE;
        SVSpatialLayer layer = null;
        LinkedHashMap<String,SVSpatialLayer> layerList = this.svp.getViewLayerTree().getLayers(this.getViewName());
        if ( layerList == null )
            return tolerance;
        Iterator<SVSpatialLayer> it = layerList.values().iterator();
        while (it.hasNext()) 
        {
            layer = it.next();
            tolerance = Math.max(layer.getTolerance(),tolerance);
        }
        return tolerance;
    }

    public void setPrecision(int numberFractions) {
        if (this.precision < 0) 
        {
            this.precision = this.getPrecision(true);
        } else {
            this.precision = numberFractions;
        }
    }

    public void setPrecision(String _precision) {
        if (Strings.isEmpty(_precision)) {
            this.precision = this.getPrecision(true);
        } else {
            try {
                this.precision = Integer.valueOf(_precision);
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    public int getPrecision(boolean _compute) 
    {
        if ( this.precision < 0 || _compute ) {
            double allLayersTolerance = this.getTolerance();
            if ( allLayersTolerance == Double.MAX_VALUE ) {
                this.precision =  this.getDefaultPrecision(); 
            } else if ( this.getSRIDType().toString().startsWith("GEO") ) {
                /* Tolerances are normally expressed in meters
                  Translation is roughly
                  500   - 3 
                  50    - 4 
                  5     - 5
                  0.5   - 6
                  0.05  - 7
                  0.005 - 8
                */
                     if ( allLayersTolerance ==   500.0 ) return 3;
                else if ( allLayersTolerance ==    50.0 ) return 4;
                else if ( allLayersTolerance ==     5.0 ) return 5;
                else if ( allLayersTolerance ==     0.5 ) return 6;
                else if ( allLayersTolerance ==    0.05 ) return 7;
                else if ( allLayersTolerance ==   0.005 ) return 8;
                else if ( allLayersTolerance ==  0.0005 ) return 9;
                else if ( allLayersTolerance == 0.00005 ) return 10;
                else return this.getDefaultPrecision();
            } else {
                this.precision = (int)Math.rint(Math.log10(1.0/allLayersTolerance));
            }
        }
        return this.precision;
    }

    public int getDefaultPrecision() {
      return SRIDType.toString().startsWith("GEO") ? Constants.MAX_PRECISION : this.defaultPrecision;
    }
    
    public String toXML(boolean _noActiveLayer) 
    {
LOGGER.debug("<toXML>");
LOGGER.debug("  getMapBackground="+this.getMapPanel().getMapBackground().toString());
//Tools.doClipboardCopy(this.getMapPanel().getMapBackground().toString() + " - " + this.getMapPanel().getMapBackground().getRGB());
LOGGER.debug("</toXML>");
        // Note, no wrapping tags
        return String.format("<ID>%s</ID><Name>%s</Name><VisibleName>%s</VisibleName><SRID>%s</SRID><SRIDType>%s</SRIDType><DistanceUnitType>%s</DistanceUnitType><AreaUnitType>%s</AreaUnitType><Precision>%s</Precision><ActiveLayer>%s</ActiveLayer><RenderingHint>%s</RenderingHint>%s<MapBackground>%s</MapBackground><ScaleBar>%s</ScaleBar><SRIDBaseUnitType>%s</SRIDBaseUnitType>",
                             this.viewID,
                             this.name,
                             this.visibleName,
                             this.SRID,
                             this.SRIDType.toString(),
                             this.distanceUnitType,
                             this.areaUnitType,
                             this.getPrecision(false),
                             (_noActiveLayer||this.getActiveLayer()==null)?"null":this.getActiveLayer().getLayerName(),
                             this.getMapPanel().getRenderHint().toString(),
                             this.getMBR().toXML(),
                             this.getMapPanel().getMapBackground().getRGB(),
                             String.valueOf(this.isScaleBar()),
                             this.SRIDBaseUnitType);
    }
    
    public void saveToDisk(LinkedHashMap<String,SVSpatialLayer> _selected) {
        String saveXML  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
               saveXML += "<View>" + this.toXML(true/*Don't save Active Layer Name*/) + "<Layers>";
        if ( _selected == null || _selected.size() == 0) {
            return;
        }
        Iterator<String> layerNamesIter = _selected.keySet().iterator();
        String layerName = "";
        while (layerNamesIter.hasNext()) {
            // We don't save SVGraphicLayers
            layerName = layerNamesIter.next();
            if ( _selected.get(layerName) instanceof SVGraphicLayer ) {
                SVGraphicLayer gLayer = (SVGraphicLayer)_selected.get(layerName);
                LOGGER.info(propertyManager.getMsg("FILE_SAVE_SKIPPING_GRAPHIC",gLayer.getLayerName()));
            } else if ( _selected.get(layerName) instanceof SVQueryLayer ) {
                SVQueryLayer qLayer = (SVQueryLayer)_selected.get(layerName);
                LOGGER.info(propertyManager.getMsg("FILE_SAVE_THEME",qLayer.getLayerName()));
                saveXML += "<Layer>" + qLayer.toXML() + "</Layer>";
            } else if ( _selected.get(layerName) instanceof SVWorksheetLayer ) {
                SVWorksheetLayer wLayer = (SVWorksheetLayer)_selected.get(layerName);
                LOGGER.info(propertyManager.getMsg("FILE_SAVE_THEME",wLayer.getLayerName()));
                saveXML += "<Layer>" + wLayer.toXML() + "</Layer>";
            } else {
                SVSpatialLayer sLayer = _selected.get(layerName);
                LOGGER.info(propertyManager.getMsg("FILE_SAVE_THEME",sLayer.getLayerName()));
                saveXML += "<Layer>" + sLayer.toXML() + "</Layer>";
            }
        }
        saveXML += "</Layers></View>";
        // Now ask where to save and file name....
        try {
            FileFilter xmlFilter = new ExtensionFileFilter("XML", new String[] { "XML" });
            File f = new File(this.getViewName().replace(":","_")+".xml");
            JFileChooser fc = new JFileChooser() {
				private static final long serialVersionUID = -3226351795311813328L;
				
				@Override
				public void approveSelection()
				{
					File f = getSelectedFile();
					if(f.exists() && getDialogType() == SAVE_DIALOG) {
						int result = JOptionPane.showConfirmDialog(this,
                                                               propertyManager.getMsg("CONFIRM_FILE_EXISTS_PROMPT"),
                                                               propertyManager.getMsg("CONFIRM_FILE_EXISTS_TITLE"),
                                                               JOptionPane.YES_NO_CANCEL_OPTION);
	                    switch(result){
	                        case JOptionPane.YES_OPTION   : super.approveSelection(); return;
	                        case JOptionPane.NO_OPTION    :
	                        case JOptionPane.CLOSED_OPTION: return;
	                        case JOptionPane.CANCEL_OPTION: cancelSelection(); return;
	                    }
	                }
	                super.approveSelection();
				}
			};
            fc.setDialogTitle(this.propertyManager.getMsg("FILE_SAVE_DIALOG_TITLE"));
            fc.setFileFilter(xmlFilter);
            fc.setSelectedFile(f);
            int returnVal = fc.showSaveDialog(null);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                LOGGER.fine(this.propertyManager.getMsg("FILE_SAVE_CANCELLED"));
            } else {
                File file = fc.getSelectedFile();
                //This is where a real application would save the file.
                LOGGER.fine(this.propertyManager.getMsg("FILE_SAVING",file.getName()));
                FileWriter fw;
                try {
                    fw = new FileWriter(file.getAbsoluteFile());
                    BufferedWriter out = new BufferedWriter(fw);
                    out.write(saveXML);
                    //Close the output stream
                    out.close();
                } catch (IOException e) {
                    LOGGER.error(this.propertyManager.getMsg("FILE_SAVE_ERROR",file.getName(),e.getMessage()));
                }
            }
        }catch (Exception e){//Catch exception if any
            LOGGER.error("Error: " + e.getMessage());
        }
    }
    
    public void setScaleBar(String _scaleBar) {
        try {
          this.scaleBar = Boolean.valueOf(_scaleBar).booleanValue();
        } catch (Exception e) {
          this.scaleBar = false;
        }
    }

    public void setScaleBar(boolean _scaleBar) {
        this.scaleBar = _scaleBar;
    }

    public boolean isScaleBar() {
        return scaleBar;
    }
}
