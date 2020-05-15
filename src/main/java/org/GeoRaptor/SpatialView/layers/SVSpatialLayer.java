package org.GeoRaptor.SpatialView.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import java.sql.ParameterMetaData;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.io.ExtensionFileFilter;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.LabelStyler;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Data types description in Oracle documentation (Oracle Spatial User Guide and Reference 10g Release 2)
 * 36 - 1.4 Geometry Types
 * 51 - 2.1 Simple Example: Inserting, Indexing, and Querying Spatial Data
 * 59 - Table 2.2 Values and Semantics in SDO_ELEM_INFO
 * 64 - 2.5 Geometry Examples
 * 75 - 2.5.8 Several Geometry Types
 */
@SuppressWarnings("deprecation")
public class SVSpatialLayer 
     extends SVLayer
{
    public static final String CLASS_NAME = Constants.KEY_SVSpatialLayer;

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SVSpatialLayer");

    protected Preferences           preferences = null;
    protected Styling                   styling = new Styling();
    protected SVSpatialLayerDraw      drawTools = new SVSpatialLayerDraw(this); // Class used to draw this layer 
    protected String                  layerName = "";         // Unique layer name. This is the universal unique name for this layer.
    protected String                visibleName = "";         // This text is write into JTree where we show layers
    protected String            layerCopySuffix = " (COPY)";  // Should be Tools>Preferences>GeoRaptor
    protected String                       desc = "";         // Layer description. It is diplay when user left mouse cursor on layer name.
    protected boolean                      draw = true;       // true - draw layer, false - not draw layer
    protected boolean              calculateMBR = false;      // When true, recalculates Layer MBR at next draw from actual data
    protected boolean               indexExists = true;       // Does spatial index exist for this layer
    protected String                   layerSQL = "";         // SQL for geometry object and other columns (for text, line color, shade color, etc)
    protected String                 wrappedSQL = "";         // cached version of layerSQL wrapped for access by GeoRaptor
    protected boolean                   project = false;      // Project to view's projection?
    protected String      projectedGeometryName = "TGEO12_34_098";
    protected boolean             minResolution = false;      // Do we use min_resolution sdo_filter parameter when querying?
    protected int               resultFetchSize = 100;        // Fetch size for ResultSet
    protected long             numberOfFeatures = 0;          // Feature Count for display and stats
    protected Constants.OBJECT_TYPES objectType = Constants.OBJECT_TYPES.TABLE;
    private final String  sdoFilterMinResClause = "min_resolution=%f,querytype=WINDOW";
    private final String        sdoFilterClause = "querytype=WINDOW";

    /*****************************************************************/

    /** Constructors...
     **/
    public SVSpatialLayer(SpatialView _sView) {
        super(_sView);
        initialise();
    } // SVSpatialLayer

    public SVSpatialLayer(SpatialView   _sView, 
                          String        _layerName,
                          String        _visibleName,
                          String        _desc, 
                          MetadataEntry _me, 
                          boolean       _draw) 
    {
        super(_sView, _me);
        this.layerName = Strings.isEmpty(_layerName)? UUID.randomUUID().toString() : _layerName;
        this.visibleName = _visibleName;
        this.desc = _desc;
        this.draw = _draw;
        if ( MainSettings.getInstance().getPreferences().isRandomRendering()) {
            this.styling.setAllRandom();
        }
        initialise();
    } // SVSpatialLayer

    /**
     * Read layer properties from given XML Layer object and set class variables
     * @param _XML String XML <Layer>...</Layer>
     */
    public SVSpatialLayer(SpatialView _sView, String _XML) {
        super(_sView, _XML);
        try {
            Document doc = null;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(_XML)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            this.fromXMLNode((Node)xpath.evaluate("/Layer", doc,
                                                  XPathConstants.NODE));
        } catch (XPathExpressionException xe) {
            LOGGER.error("SVSpatialLayer(XML): XPathExpressionException " + xe.toString());
        } catch (ParserConfigurationException pe) {
            LOGGER.error("SVSpatialLayer(XML): ParserConfigurationException " + pe.toString());
        } catch (SAXException se) {
            LOGGER.error("SVSpatialLayer(XML): SAXException " + se.toString());
        } catch (IOException ioe) {
            LOGGER.error("SVSpatialLayer(XML): IOException " + ioe.toString());
        }
        initialise();
    }

    public SVSpatialLayer(SpatialView _sView, Node _node) {
        super(_sView, _node);
        this.fromXMLNode(_node);
        initialise();
    }

    private void fromXMLNode(Node _node) {
        if (_node == null || _node.getNodeName().equals("Layer") == false) {
            return; // Should throw error
        }
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            this.setLayerName((String)xpath.evaluate("SVSpatialLayer/Name/text()",_node,XPathConstants.STRING));
            this.setVisibleName((String)xpath.evaluate("SVSpatialLayer/ScreenName/text()",_node,XPathConstants.STRING));  // Temporary due to name change.
            this.setVisibleName((String)xpath.evaluate("SVSpatialLayer/VisibleName/text()",_node,XPathConstants.STRING));
            this.setDesc((String)xpath.evaluate("SVSpatialLayer/Description/text()",_node, XPathConstants.STRING));
            this.setDraw(Boolean.valueOf((String)xpath.evaluate("SVSpatialLayer/Draw/text()",_node,XPathConstants.STRING)));
            this.setSQL((String)xpath.evaluate("SVSpatialLayer/LayerSQL/text()",_node,XPathConstants.STRING));
            this.setResultFetchSize(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/ResultFetchSize/text()",_node,XPathConstants.STRING)));
            this.setMinResolution(Boolean.valueOf((String)xpath.evaluate("SVSpatialLayer/MinResolution/text()",_node,XPathConstants.STRING)));
            this.setIndex((String)xpath.evaluate("SVSpatialLayer/hasIndex/text()",_node,XPathConstants.STRING));
            this.setProject(Boolean.valueOf((String)xpath.evaluate("SVSpatialLayer/isProject/text()",_node,XPathConstants.STRING)),false);
            
            // Label has to be after layerSQL because it checks the SQL for the label
            this.styling.setLabelColumn((String)xpath.evaluate("SVSpatialLayer/Label/text()",_node,XPathConstants.STRING));
            this.styling.setRotationColumn((String)xpath.evaluate("SVSpatialLayer/RotationColumn/text()",_node,XPathConstants.STRING));
            this.styling.setRotationValue((String)xpath.evaluate("SVSpatialLayer/RotationValue/text()",_node,XPathConstants.STRING));
            this.styling.setRotationTarget((String)xpath.evaluate("SVSpatialLayer/RotationTarget/text()",_node,XPathConstants.STRING));
            
            this.styling.setTextOffsetPosition((String)xpath.evaluate("SVSpatialLayer/LabelPosition/text()",_node,XPathConstants.STRING));
            this.styling.setLabelOffset((String)xpath.evaluate("SVSpatialLayer/LabelOffset/text()",_node,XPathConstants.STRING));
            Node labelAttributes = (Node)xpath.evaluate("SVSpatialLayer/LabelAttributes", _node,XPathConstants.NODE);
            this.styling.setLabelAttributes(labelAttributes);
            this.styling.setGeometryLabelPosition((String)xpath.evaluate("SVSpatialLayer/GeometryLabelPoint/text()",_node,XPathConstants.STRING));            
            
            this.styling.setPointSize(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/PointSize/text()",_node,XPathConstants.STRING)));
            this.styling.setPointSizeType((String)xpath.evaluate("SVSpatialLayer/PointSizeType/text()",_node,XPathConstants.STRING));
            this.styling.setPointSizeColumn((String)xpath.evaluate("SVSpatialLayer/PointSizeTypeColumn/text()",_node,XPathConstants.STRING));
            this.styling.setPointColor(new Color(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/PointColor/text()",_node,XPathConstants.STRING))));
            this.styling.setPointType((String)xpath.evaluate("SVSpatialLayer/PointType/text()",_node,XPathConstants.STRING));
            this.styling.setPointColorType((String)xpath.evaluate("SVSpatialLayer/PointColorType/text()",_node,XPathConstants.STRING));
            this.styling.setPointColorColumn((String)xpath.evaluate("SVSpatialLayer/PointColorColumn/text()",_node,XPathConstants.STRING));
            
            this.styling.setLineWidth(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/LineWidth/text()",_node,XPathConstants.STRING)));
            this.styling.setLineColor(new Color(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/LineColor/text()",_node,XPathConstants.STRING))));
            this.styling.setLineStrokeType((String)xpath.evaluate("SVSpatialLayer/LineStrokeType/text()",_node,XPathConstants.STRING));
            this.styling.setLineColorColumn((String)xpath.evaluate("SVSpatialLayer/LineColorColumn/text()",_node,XPathConstants.STRING));
            this.styling.setLineColorType((String)xpath.evaluate("SVSpatialLayer/LineColorType/text()",_node,XPathConstants.STRING));
            this.styling.setLineTransLevel(Float.valueOf(Strings.isEmpty((String)xpath.evaluate("SVSpatialLayer/LineTransLevel/text()",_node,XPathConstants.STRING))
                                                                ?"1.0":(String)xpath.evaluate("SVSpatialLayer/LineTransLevel/text()",_node,XPathConstants.STRING)));
            
            this.styling.setMarkGeoStart((String)xpath.evaluate("SVSpatialLayer/MarkGeoStart/text()",_node,XPathConstants.STRING));
            this.styling.setMarkGeoPoints((String)xpath.evaluate("SVSpatialLayer/MarkGeoPoints/text()",_node,XPathConstants.STRING));
            this.styling.setSegmentArrow((String)xpath.evaluate("SVSpatialLayer/MarkLineDir/text()",_node,XPathConstants.STRING));
            this.styling.setMarkVertex((String)xpath.evaluate("SVSpatialLayer/MarkVertex/text()",_node,XPathConstants.STRING));
            this.styling.setMarkOriented(Boolean.valueOf((String)xpath.evaluate("SVSpatialLayer/MarkOriented/text()",_node,XPathConstants.STRING)));
            this.styling.setTextOffsetPosition((String)xpath.evaluate("SVSpatialLayer/MarkPosition/text()",_node,XPathConstants.STRING));
            this.styling.setMarkLabelOffset((String)xpath.evaluate("SVSpatialLayer/MarkOffset/text()",_node,XPathConstants.STRING));
            Node markLabelAttributes = (Node)xpath.evaluate("SVSpatialLayer/MarkLabelAttributes",_node,XPathConstants.NODE);
            this.styling.setMarkLabelAttributes(markLabelAttributes,this.getSpatialView().getMapPanel().getMapBackground()); // .cloneNode(true));            
            this.styling.setMarkSegment((String)xpath.evaluate("SVSpatialLayer/MarkSegment/text()",_node,XPathConstants.STRING));

            this.styling.setShadeColumn((String)xpath.evaluate("SVSpatialLayer/ShadeColumn/text()",_node,XPathConstants.STRING));
            this.styling.setShadeType((String)xpath.evaluate("SVSpatialLayer/ShadeType/text()",_node,XPathConstants.STRING));
            this.styling.setShadeColor(new Color(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/ShadeColor/text()",_node,XPathConstants.STRING))));
            this.styling.setShadeTransLevel(Float.valueOf(Strings.isEmpty((String)xpath.evaluate("SVSpatialLayer/ShadeTransLevel/text()",_node,XPathConstants.STRING))
                                                                 ?"1.0":(String)xpath.evaluate("SVSpatialLayer/ShadeTransLevel/text()",_node,XPathConstants.STRING)));
            
            this.styling.setSelectionPointSize(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/SelectPointSize/text()",_node,XPathConstants.STRING)));
            this.styling.setSelectionLineWidth(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/SelectLineWidth/text()",_node,XPathConstants.STRING)));
            this.styling.setSelectionColor(new Color(Integer.valueOf((String)xpath.evaluate("SVSpatialLayer/SelectionColor/text()",_node,XPathConstants.STRING))));
            this.styling.setSelectionTransLevel(Float.valueOf((String)xpath.evaluate("SVSpatialLayer/SelectShadeTransLevel/text()",_node,XPathConstants.STRING)));
            this.styling.setSelectionLineStrokeType((String)xpath.evaluate("SVSpatialLayer/SelectLineStrokeType/text()",_node,XPathConstants.STRING));
            
            this.styling.setTextLoScale((String)xpath.evaluate("SVSpatialLayer/TextLoScale/text()",_node,XPathConstants.STRING));
            this.styling.setTextHiScale((String)xpath.evaluate("SVSpatialLayer/TextHiScale/text()",_node,XPathConstants.STRING));
            // Some other stuff
            this.setProject(!(this.getSRID().equals(Constants.NULL) ||
                              this.spatialView.getSRID().equals(Constants.NULL) ||
                              this.getSRID().equals(this.spatialView.getSRID())),
                            false) /* Don't recalc MBR */;
        } catch (XPathExpressionException xe) {
            LOGGER.error("fromXMLNode(): XPathExpressionException " + xe.toString());
        }
    }

    public SVSpatialLayer(SVSpatialLayer _sLayer) {
        super(_sLayer.getSpatialView(), _sLayer.getMetadataEntry());
        super.setConnectionName(_sLayer.connName);
        String newName = _sLayer.getSpatialView().getSVPanel().getViewLayerTree().checkName(_sLayer.getLayerName());
        this.setLayerName(newName);
        this.setVisibleName(_sLayer.getVisibleName()+layerCopySuffix);
        this.setDesc(_sLayer.getDesc());
        this.setDraw(_sLayer.isDraw());
        this.setSQL(_sLayer.getSQL());
        this.setSRIDType(_sLayer.getSRIDType());
        this.setGeometryType(_sLayer.getGeometryType());
        this.setSTGeometry(_sLayer.isSTGeometry());
        this.setIndex(_sLayer.hasIndex());
        this.setMBR(_sLayer.getMBR());
        this.setMinResolution(_sLayer.getMinResolution());
        this.setFetchSize(_sLayer.getFetchSize());
        this.setPrecision(_sLayer.getPrecision(false));
        this.setProject(_sLayer.getProject(),false);
        this.styling = new Styling(_sLayer.getStyling());
        initialise();
    }

    private void initialise() {
        this.preferences = MainSettings.getInstance().getPreferences();
        this.setResultFetchSize(preferences.getFetchSize());
        this.setPrecision(-1); // force calculation from mbr
    }

    public String getClassName() {
        return SVSpatialLayer.CLASS_NAME;
    }
    
    protected void setStyling(Styling _style) {
        this.styling = _style;
    }

    public Styling getStyling() {
        return this.styling;
    }

    public String getStylingAsString() {
        return this.styling.toString(this.getSpatialView().getMapPanel().getMapBackground());
    }
    
    protected PropertiesManager getPropertyManager() {
        // propertyManager is always set by initialise() method
        return this.propertyManager;
    }

    public Preferences getPreferences() {
        if (this.preferences==null) {
            this.preferences = MainSettings.getInstance().getPreferences();
        }
        return this.preferences;
    }

    public String toXML() {
        String SVLayerXML = super.toXML();
        String SVSpatialLayerXML = "";
        try {
            SVSpatialLayerXML =
                    String.format("<SVSpatialLayer><Name>%s</Name><VisibleName>%s</VisibleName><Description>%s</Description><Draw>%s</Draw><LayerSQL>%s</LayerSQL>" +
                                      "<Label>%s</Label><RotationColumn>%s</RotationColumn><RotationValue>%s</RotationValue><RotationTarget>%s</RotationTarget>"+
                                      "<LabelPosition>%s</LabelPosition><LabelOffset>%s</LabelOffset><LabelAttributes>%s</LabelAttributes><GeometryLabelPoint>%s</GeometryLabelPoint>" + 
                                      "<ResultFetchSize>%s</ResultFetchSize><MinResolution>%s</MinResolution><hasIndex>%s</hasIndex><isProject>%s</isProject>" +
                                      "<PointSize>%s</PointSize><PointSizeType>%s</PointSizeType><PointSizeTypeColumn>%s</PointSizeTypeColumn>" +
                                      "<PointColor>%d</PointColor><PointType>%s</PointType><PointColorColumn>%s</PointColorColumn><PointColorType>%s</PointColorType>" +
                                      "<LineWidth>%s</LineWidth><LineColor>%d</LineColor><LineStrokeType>%s</LineStrokeType><LineTransLevel>%s</LineTransLevel><LineColorColumn>%s</LineColorColumn><LineColorType>%s</LineColorType>" +
                                      "<ShadeColumn>%s</ShadeColumn><ShadeType>%s</ShadeType><ShadeColor>%d</ShadeColor><ShadeTransLevel>%s</ShadeTransLevel>" +
                                      "<SelectPointSize>%s</SelectPointSize><SelectLineWidth>%s</SelectLineWidth><SelectionColor>%d</SelectionColor><SelectShadeTransLevel>%s</SelectShadeTransLevel>" +
                                      "<MarkGeoStart>%s</MarkGeoStart><MarkGeoPoints>%s</MarkGeoPoints><MarkVertex>%s</MarkVertex><MarkOriented>%s</MarkOriented><MarkLineDir>%s</MarkLineDir><MarkPosition>%s</MarkPosition><MarkOffset>%s</MarkOffset>" + 
                                      "<MarkLabelAttributes>%s</MarkLabelAttributes><MarkSegment>%s</MarkSegment>" +
                                      "<TextLoScale>%s</TextLoScale><TextHiScale>%s</TextHiScale>" +
                                  "</SVSpatialLayer>",
                        this.layerName, // should always be not null
                        this.getVisibleName(), // Could be empty but shouldn't
                        this.getDesc(), // Could be empty
                        String.valueOf(this.draw), 
                        this.getLayerSQL(),
                        this.styling.getLabelColumn(), this.styling.getRotationColumn(),
                        this.styling.getRotationValue().toString(),
                        this.styling.getRotationTarget().toString(),
                        this.styling.getLabelPosition().toString(),
                        String.valueOf(this.styling.getLabelOffset()),
                        LabelStyler.toXML(this.styling.getLabelAttributes()),
                        this.styling.getGeometryLabelPosition().toString(),

                        String.format("%d", this.resultFetchSize),
                        String.valueOf(this.minResolution),
                        String.valueOf(this.hasIndex()),
                        String.valueOf(this.getProject()),
                        String.format("%d", this.styling.getPointSize(4)),
                        this.styling.getPointSizeType().toString(),
                        this.styling.getPointSizeColumn(),          
                        this.styling.getPointColor(null).getRGB(),
                        this.styling.getPointType().toString(),
                        this.styling.getPointColorColumn(), 
                        this.styling.getPointColorType().toString(),
                        String.format("%d", this.styling.getLineWidth()),
                        this.styling.getLineColor(null).getRGB(),
                        this.styling.getLineStrokeType().toString(),
                        this.styling.getLineTransLevel(),
                        this.styling.getLineColorColumn(), 
                        this.styling.getLineColorType().toString(),
                        this.styling.getShadeColumn(), this.styling.getShadeType().toString(),
                        this.styling.getShadeColor(null).getRGB(), this.styling.getShadeTransLevel(),
                        String.format("%d", this.styling.getSelectionPointSize()),
                        String.format("%d", this.styling.getSelectionLineWidth()),
                        this.styling.getSelectionColor().getRGB(),
                        this.styling.getSelectionShadeTransLevel(),
                        String.valueOf(this.styling.getMarkGeoStart()),
                        String.valueOf(this.styling.getMarkGeoPoints()),
                        this.styling.getMarkVertex().toString(),
                        String.valueOf(this.styling.isMarkOriented()),
                        this.styling.getSegmentArrow().toString(),
                        this.styling.getLabelPosition().toString(),
                        String.valueOf(this.styling.getMarkLabelOffset()),
                        LabelStyler.toXML(this.styling.getMarkLabelAttributes(this.getSpatialView().getMapPanel().getMapBackground())),
                        this.styling.getMarkSegment().toString(),
                        String.valueOf(this.styling.getTextLoScale()),
                        String.valueOf(this.styling.getTextHiScale()));
        } catch (Exception e) {
            LOGGER.error("Error saving " + this.getVisibleName() + " (" +
                               this.getLayerName() + ") : " +
                               e.getCause().getLocalizedMessage());
        }
        return SVLayerXML + SVSpatialLayerXML;
    }

    public void savePropertiesToDisk() {
        String saveXML = this.toXML();
        saveXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                  "<Layers><Layer>" + saveXML + "</Layer></Layers>";
        // Now ask where to save and file name....
        try {
            FileFilter xmlFilter = new ExtensionFileFilter("XML", new String[] { "XML" });
            File f = new File(this.getLayerName().replace(".","_")+".xml");
            JFileChooser fc = new JFileChooser()
            {
				private static final long serialVersionUID = 8674762352541526796L;

				@Override
	            public void approveSelection(){
	                File f = getSelectedFile();
	                if(f.exists() && getDialogType() == SAVE_DIALOG){
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
                    LOGGER.fine(this.propertyManager.getMsg("FILE_SAVE_THEME",this.getLayerName()));
                } catch (IOException e) {
                    LOGGER.error(this.propertyManager.getMsg("FILE_SAVE_ERROR",file.getName(),e.getMessage()));
                }
            }
        }catch (Exception e){//Catch exception if any
            LOGGER.error("Error: " + e.getMessage());
        }
    }
    
    public boolean equals(Object obj) {
        //if the two objects are equal in reference, they are equal
        if (this == obj) {
            return true;
        }
        if (obj instanceof SVSpatialLayer) {
            SVSpatialLayer sLayer = (SVSpatialLayer)obj;
            return sLayer.getLayerName().equals(this.getLayerName()) &&
                sLayer.getVisibleName().equals(this.getVisibleName());
        } else {
            return false;
        }
    }

    public SpatialView getSpatialView() {
        return super.getSpatialView();
    }

    public String getLayerNameAndConnectionName() {
        return this.layerName + "." + super.getConnectionDisplayName();
    }
    
    public String getLayerName() {
        return this.layerName;
    }

    public void setLayerName(String _layerName) {
        this.layerName = _layerName;
    }

    // This version checks if connection has changed.
    // Normally database object doesn't change schema but sometimes it can if layer imported from another database.
    //
    public void setConnection(String _connName) {
        String oldSchema = super.getSchemaName();
        super.setConnection(_connName);
        // Check if connection/schema has changed
        if ( ! oldSchema.equalsIgnoreCase(super.getSchemaName()) ) {
            // Should change SVSpatialLayer SQL if has schema name?
            String sql = this.getSQL();
            if (sql.contains(oldSchema+".")) {
                this.layerSQL = sql.replace(oldSchema+".", super.getSchemaName()+".");
            }
            // and force rewrapping
            this.wrappedSQL = "";
        }
    }
    
    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String _desc) {
        this.desc = _desc;
    }

    public String getVisibleName() {
        return this.visibleName;
    }

    public void setVisibleName(String _visibleName) {
        this.visibleName = _visibleName;
    }

    public boolean is_draw() {
        return this.draw;
    }

    public boolean isDraw() {
        return this.draw;
    }

    public void setDraw(boolean _draw) {
        this.draw = _draw;
    }

    public void setObjectType(String _objectType) {
        if (Strings.isEmpty(_objectType) ) {
            try {
                this.objectType = Constants.OBJECT_TYPES.valueOf(MetadataTool.getObjectType(super.getConnection(),
                                                                                            mEntry.getSchemaName(),
                                                                                            mEntry.getObjectName()));
            } catch (SQLException e) {
                this.objectType = Constants.OBJECT_TYPES.TABLE;
            }
        } else {
            try {
                this.objectType = Constants.OBJECT_TYPES.valueOf(_objectType);
            } catch (Exception e) {
                this.objectType = Constants.OBJECT_TYPES.TABLE;
            }
        }
    }
    
    public boolean isView() {
        return this.objectType.equals(Constants.OBJECT_TYPES.VIEW);
    }
    
    public void setIndex() {
        OracleConnection conn = null; 
        try {
            conn = super.getConnection();
        } catch (IllegalStateException ise) {
            LOGGER.warn("No connection available for (" + 
                        this.getLayerNameAndConnectionName() + 
                        ") to check spatial index (" + ise.toString() + ")");
            this.indexExists = false;
        }
        try {
            LOGGER.debug("SVSpatialLayer(" + 
                         this.getLayerNameAndConnectionName() + 
                         ").setIndex.isSpatiallyIndexed()");
            this.indexExists = MetadataTool.isSpatiallyIndexed(conn,
                                                               super.getSchemaName(),
                                                               super.getObjectName(),
                                                               super.getGeoColumn(),
                                                               super.getSRID());                
            LOGGER.debug("SVSpatialLayer(" + this.getLayerNameAndConnectionName() + ").setIndex() = " + indexExists);
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("SVSpatialLayer(" + this.getLayerNameAndConnectionName() + ").isSpatiallyIndexed Exception: " + iae.toString());
            this.indexExists = false;
        } catch (SQLException e) {
            LOGGER.warn("SVSpatialLayer(" + this.getLayerNameAndConnectionName() + ").setIndex() SQLException: " + e.toString());
            this.indexExists = false;
        }
    }

    public void setIndex(String _hasIndex) {
        try {
            if (Strings.isEmpty(_hasIndex) )
                this.setIndex();
            else 
                this.indexExists = Boolean.valueOf(_hasIndex); 
        } catch (Exception e) {
            this.indexExists = true;
        }
    }
  
    public void setIndex(boolean _indexExists) {
        this.indexExists = _indexExists;
    }
    
    public boolean hasIndex() {
        return this.indexExists;
    }
    
    public boolean setLayerMBR(Envelope _defaultMBR,
                               int             _targetSRID)
    {
        OracleConnection conn = super.getConnection();
         
        // If source is INDEX get MBR from RTree index
        //
        Envelope lMBR = new Envelope(this.getDefaultPrecision());
        
        LOGGER.debug("SVSpatialLayer.setLayerMBR() hasIndex()=" + this.hasIndex() + 
                   "\nSRIDTYPE=" + this.getSRIDType().toString()+
                   "\nLayerMbrSource=" + this.getPreferences().getLayerMBRSource().toString());
        
        // If one method fails, a warning is written. When this happens we need to report success of later method
        boolean multiTry = false;
        
        if (this.hasIndex() &&
            this.getSRIDType().toString().startsWith("GEO") == false &&
            this.getPreferences().getLayerMBRSource().equalsIgnoreCase(Constants.CONST_LAYER_MBR_INDEX)) 
        {
            try {
                lMBR.setMBR(MetadataTool.getExtentFromRTree(conn,
                                                            this.getSchemaName(),
                                                            this.getObjectName(),
                                                            this.getGeoColumn(),
                                                            this.getSRID(),
                                                            String.valueOf(_targetSRID))); 
                LOGGER.debug("SVSpatialLayer.setLayerMBR() setMBR(getRTreeExtent)=" + lMBR.toString());
                if ( lMBR.isSet() ) {
                    super.setMBR(lMBR);
                    return true;
                } 
            } catch (SQLException e) {
                LOGGER.warn(e.getMessage() + ": Will now try and extract MBR from metadata.");
                multiTry = true;
            }
        }
        
        // Try and get Metadata from database.
        //
        boolean hasMetadata = false;
        try {
            hasMetadata = MetadataTool.hasGeomMetadataEntry(conn,
                                                            this.getSchemaName(),
                                                            this.getObjectName(),
                                                            this.getGeoColumn());
            // Try and extract from existing Metadata
            if ( hasMetadata ) {
                try 
                {
                    lMBR = MetadataTool.getExtentFromDimInfo(conn, 
                                                             this.getSchemaName(),
                                                             this.getObjectName(), 
                                                             this.getGeoColumn(),
                                                             this.getSpatialView().getSRID());
                    if ( lMBR.isSet() ) {
                        super.setMBR(lMBR);
                        if (multiTry) {
                            LOGGER.warn("Extracting MBR From Metadata Successful.");
                        }
                        return true;
                    } 
                } catch (Exception e) {
                    LOGGER.warn("Error extracting MBR from metadata (" + e.getMessage() + "): Extracting MBR from a sample of records.");
                    multiTry = true;
                }
            }  
        } catch (SQLException e) {
            LOGGER.warn("No User_Sdo_Geom_Metadata: Skipping to extract MBR from a sample of records.");
            multiTry = true;
        }
        
        // Try sampling geometries to get MBR
        //
        try 
        {
            lMBR = MetadataTool.getExtentFromSample(conn, 
                                                    this.getSchemaName(),
                                                    this.getObjectName(), 
                                                    this.getGeoColumn(),
                                                    Constants.VAL_MBR_SAMPLE_LIMIT);
            if ( lMBR.isSet() ) {
                super.setMBR(lMBR);
                if (multiTry) {
                    LOGGER.warn("Extracting MBR From Sample Successful.");
                }
                return true;
            } 
        } catch (SQLException e) {
            LOGGER.warn("Failed to get MBR Through Sampling (" + e.getMessage() + ").");
        }
        
        LOGGER.debug("SVSpatialLayer.setLayerMBR() setMBR=" + lMBR.toString());        
        if ( _defaultMBR!=null && 
             _defaultMBR.isSet() ) {
            super.setMBR(_defaultMBR);
            LOGGER.warn("Default MBR Applied to Layer.");
            return true;
        }
        return false;
    }

    /**
     * Create SQL for given Geometry column and table name
     */
    public String getInitSQL(String _geoColumn) 
    {
        LOGGER.debug("SVSpatialLayer.getInitSQL(" +_geoColumn + ") -> " + this.getVisibleName() + " hasIndex="+this.hasIndex());
        // If SQL Developer still starting up and a layer is being loaded from XML
        // the connections won't work
        //
        String columns = "";
        try {
          columns = MetadataTool.getColumns(super.getConnection(),
                                            super.getSchemaName(),
                                            super.getObjectName(),
                                            true /* _supportedDataTypes */ );
        } catch (IllegalStateException ise) {
            return null;
        } catch (SQLException sqle) {
            LOGGER.warn("(SVSpatialLayer.getInitSQL) SQL Error: " + sqle.getMessage());
            return null;
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("(SVSpatialLayer.getInitSQL) Illegal Argument Exception: " + iae.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.warn("(SVSpatialLayer.getInitSQL) General Exception: " + e.getMessage());
            return null;
        }
        String retLayerSQL;
        retLayerSQL = "SELECT rowid," + (Strings.isEmpty(columns) ? "" : columns+"," ) + 
                             "t." + _geoColumn + " as " + _geoColumn + " \n" +
                      "  FROM " + (this.preferences.getSQLSchemaPrefix()?this.getFullObjectName():this.getObjectName()) + " t \n" +
                      " WHERE ";
        if ( this.hasIndex() ) {
            retLayerSQL += "SDO_FILTER(t." + _geoColumn + 
                           (this.isSTGeometry()
                            ?   ",?) = 'TRUE'"
                            : ",?,?) = 'TRUE'");
        } else {
            retLayerSQL += "t." + _geoColumn + " IS NOT NULL\n" +
                           " AND t." + _geoColumn + ".sdo_gtype is not null\n" +
                           " AND MDSYS.SDO_GEOM.VALIDATE_GEOMETRY(t." + _geoColumn + "," + this.getTolerance() + ") = 'TRUE'\n" +
                           " AND MDSYS.SDO_GEOM.RELATE(t." + _geoColumn + ",'ANYINTERACT',?,"+this.getTolerance()+") = 'TRUE'";
        }
LOGGER.debug("SVSpatialLayer.getInitSQL returning " + retLayerSQL);
        return retLayerSQL;
    }

    public void setInitSQL() {
        this.layerSQL = this.getInitSQL(super.getGeoColumn());
    }

    public String getDefaultSDOFilterClause(Point2D.Double _pixelSize) 
    {
        String sdoFilterClause = this.sdoFilterClause;
        if ( this.getMinResolution() && _pixelSize != null  ) {
            double maxPixelSize = Math.max(_pixelSize.getX(), _pixelSize.getY());
            if (maxPixelSize != 0.0) {
                sdoFilterClause = String.format(this.sdoFilterMinResClause,maxPixelSize);
            }
        }
        return sdoFilterClause;
    }
    
    private STRUCT getSearchFilterGeometry(Envelope _mbr,
                                           boolean         _project,
                                           int             _sourceSRID,
                                           int             _destinationSRID) 
    throws SQLException 
    {
        STRUCT filterGeom = null;
        try {
            OracleConnection conn = null;
            conn = super.getConnection();
            // JGeometry codes NULL srid from value 0 not -1
            int srid = _sourceSRID == Constants.SRID_NULL ? 0 : _sourceSRID; 
            JGeometry jGeom = new JGeometry(_mbr.getMinX(),_mbr.getMinY(),_mbr.getMaxX(),_mbr.getMaxY(),srid);
            filterGeom = _project 
            		     ? MetadataTool.projectJGeometry(conn, jGeom, _destinationSRID) 
                         : (STRUCT) JGeometry.storeJS(conn,jGeom);
        } catch (Exception e) {
            LOGGER.warning(propertyManager.getMsg("ERROR_CREATE_MBR_RECTANGLE",e.getMessage()));
            filterGeom = null;
        }
        return filterGeom;
    }
    
    private static boolean columnExists(String _sql, String _column) 
    {
        if (Strings.isEmpty(_sql) || Strings.isEmpty(_column) ) return false;
        // Quoted attributes can be upper/lower/mixed case and are unique enough
        // that a pure contains search shouldn't not find
        //
        if ( _column.startsWith("\"") && _column.endsWith("\"") ) {
            return _sql.contains(_column);
        }
        String token = "",
               column = _column.replaceAll("\"", "").toUpperCase();
        StringTokenizer st = new StringTokenizer(_sql,". ,()",true);
        int expression = 0; 
        while (st.hasMoreTokens() ) {
            token = st.nextToken().replaceAll("\"", "").toUpperCase();
            if ( token.equals("FROM") ) return false;
            if ( token.matches("[., ]") ) { continue; }        
            if ( token.equals("(") ) { expression++; continue; }
            if ( token.equals(")") ) { expression--; continue; }
            if ( expression==0 && token.equals(column))  { return true; }
            if ( expression==0 && token.matches("\\*") ) { return true; }
        }
        return false;
    }

    /** wrapSQL
     *  Takes basic SQL statement and wraps it in an inline view for use in querying and identifying geometry objects.
     * @param _sql
     * @return wrapped SQL statement.
     */
    public String wrapSQL(String _sql) 
    {
        LOGGER.debug("<WrapSQL>");
        LOGGER.debug("   Before="+_sql );        
        String SQL = _sql;
        
        OracleConnection conn = null;
        // Get connection, including trying to open it.
        if ( super.openConnection() ) {
            conn = super.getConnection();
            if (conn==null) {
                LOGGER.error(this.getLayerName() + ": no open database connection available.");
                return _sql;
            }
        } else {
            LOGGER.error(this.getLayerName() + "'s connect (" + super.getConnectionDisplayName() + ") cannot be opened.");
            return _sql;
        }

        // Create main SELECT clause from user attributes        
        // Only need to stop filter if we have an index()
        //
        String sql = "";
        if ( this.isSTGeometry() ) {
            sql = SQL.replace("SDO_FILTER(t." + this.getGeoColumn() + ",?) = 'TRUE'","1=1");
        } else {
            sql = SQL.replace("SDO_FILTER(t." + this.getGeoColumn() + ",?,?) = 'TRUE'","1=1");
        }
        if ( this.hasIndex() ) {
            LOGGER.debug("  Replacing: " + "SDO_FILTER(t." + this.getGeoColumn() + ",?,?) = 'TRUE'");
            if ( this.isSTGeometry() ) {
                sql = SQL.replace("SDO_FILTER(t." + this.getGeoColumn() + ",?) = 'TRUE'","1=1");
            } else {
                sql = SQL.replace("SDO_FILTER(t." + this.getGeoColumn() + ",?,?) = 'TRUE'","1=1");
            }
        } else {
            LOGGER.debug("  Replacing: MDSYS.SDO_GEOM.RELATE(t." + this.getGeoColumn() + ",'ANYINTERACT',?,"+this.getTolerance()+") = 'TRUE'");
            sql = SQL.replace("MDSYS.SDO_GEOM.RELATE(t." + this.getGeoColumn() + ",'ANYINTERACT',?,"+this.getTolerance()+") = 'TRUE'","1=1");
        }
        LOGGER.debug("        After ="+sql);
        
        // Now get column names from SQL statement by using metadata prepared version of statement creates
        //
        OraclePreparedStatement pstmt;
        try {
            LOGGER.debug("  Preparing Statement");
            pstmt = (OraclePreparedStatement)conn.prepareStatement(sql);
        } catch (SQLException sqle) {
            LOGGER.error(this.getLayerName() + ": failed to prepare SQL Statement. (" + sqle.getMessage());
            return _sql;
        }            
        ResultSetMetaData rsmd;
        int columnCount = 0; 
        try {
            LOGGER.debug("  getting MetaData()");
            rsmd = pstmt.getMetaData();
            columnCount = rsmd.getColumnCount();
        } catch (SQLException sqle) {
            LOGGER.error(this.getLayerName() + ": failed to get metadata (or column count) for SQL Statement. (" + sqle.getMessage());
            return _sql;
        }

        String colName = "",
           columnLabel = "",
           quoteString = "",
           userColumns = "";

        // Iterate over columns
        //
        Pattern p = Pattern.compile("[a-z]+");
        Matcher lowerCaseMatch = null;
        for (int col=1;col<=columnCount;col++) {
            try {
                columnLabel = rsmd.getColumnLabel(col);
            } catch (SQLException sqle) {
                LOGGER.warn(this.getLayerName() + ": failed to get column label for column in position " + col + " (" + sqle.getMessage() + ")");
                continue;
            }
            lowerCaseMatch = p.matcher(columnLabel);
            quoteString = columnLabel.startsWith("\"") ||
                          columnLabel.contains(" ") ||
                          lowerCaseMatch.find()
                          ? "\"" 
                          : "";
            colName = columnLabel.replaceAll("\"","");
            LOGGER.debug("    rsmd.getColumnLabel("+col+")=" + columnLabel + (Strings.isEmpty(quoteString)?"":" - quoteString = " + quoteString) + " Column name is " + colName);
            if (colName.equals(this.getGeoColumn().replaceAll("\"", ""))) {
                if (this.project &&
                    this.spatialView.getSRID().equals(Constants.NULL)==false &&
                    this.getSRID().equals(Constants.NULL)==false) 
                {
                    userColumns += (Strings.isEmpty(userColumns) ? "" : ", ") + 
                                   "MDSYS.SDO_CS.TRANSFORM(" +  quoteString+colName+quoteString + ", " + this.spatialView.getSRID() + ")";
                } else {
                    userColumns += (Strings.isEmpty(userColumns)?quoteString+colName:", "+quoteString+colName) + quoteString;
                }
            } else {
                userColumns += (Strings.isEmpty(userColumns)?quoteString+colName:", "+quoteString+colName) + quoteString;
            }
        }
        // Close statement.
        try {
            pstmt.close();
        } catch (SQLException sqle) {
            LOGGER.error("wrapSQL: Problem checking draw SQL - " + sqle.getMessage());
            return _sql;
        }
            
        LOGGER.debug("  Column names are " + userColumns);
        // Check that columns needed for rendering etc exist in original SQL
        this.styling.setLabelColumn(Strings.isEmpty(this.styling.getLabelColumn())         ? null : this.styling.getLabelColumn());
        this.styling.setRotationColumn(Strings.isEmpty(this.styling.getRotationColumn())      ? null : this.styling.getRotationColumn());
        this.styling.setShadeColumn(Strings.isEmpty(this.styling.getShadeColumn())      || this.styling.getShadeType()      != Styling.STYLING_TYPE.COLUMN ? null : this.styling.getShadeColumn());
        this.styling.setLineColorColumn(Strings.isEmpty(this.styling.getLineColorColumn())     ? null : this.styling.getLineColorColumn());
        this.styling.setPointColorColumn(Strings.isEmpty(this.styling.getPointColorColumn()) || this.styling.getPointColorType() != Styling.STYLING_TYPE.COLUMN ? null : this.styling.getPointColorColumn());
        this.styling.setPointSizeColumn(Strings.isEmpty(this.styling.getPointSizeColumn())  || this.styling.getPointSizeType()  != Styling.STYLING_TYPE.COLUMN ? null : this.styling.getPointSizeColumn());
        String[] neededCols = {"ROWID",this.styling.getLabelColumn(),
                               this.styling.getRotationColumn(),
                               this.styling.getShadeColumn(),
                               this.styling.getLineColorColumn(),
                               this.styling.getPointColorColumn(),
                               this.styling.getPointSizeColumn()};
        for (int i=0;i<neededCols.length;i++) {
            if ( !Strings.isEmpty(neededCols[i]) ) {
                // is this column in the original SQL?
// LOGGER.debug("neededCol["+i+"]="+neededCols[i]);
                if ( ! columnExists(SQL,neededCols[i].replace("\"",""))  ) {
                    // Add it in
                    SQL = SQL.replace(SQL.substring(0,7),SQL.substring(0,7)+ neededCols[i] + ",");
                    userColumns += (Strings.isEmpty(userColumns)?neededCols[i]:"," + neededCols[i]);
                }
            }
        }
        LOGGER.debug("</WrapSQL>");
        return "SELECT " + userColumns + " FROM ( \n" + SQL + "\n) " + ( this.hasIndex() ? "" : "\n WHERE rownum < " + String.valueOf(this.getPreferences().getTableCountLimit()) );
    }

    /**
     * @param SQL
     */
    public void setSQL(String _SQL) {
        LOGGER.debug("SVSpatialLayer.setSQL(" + _SQL +") - START");
        if (Strings.isEmpty(_SQL) ||
            (!_SQL.equalsIgnoreCase(this.layerSQL))) {
            LOGGER.debug("this.layerSQL = _SQL");
            this.layerSQL = _SQL;
            this.wrappedSQL = "";
        }
        LOGGER.debug("SVSpatialLayer.setSQL() finished");
    }

    /**
     * getLayerSQL - for XML use only
     * @return String
     */
    public String getLayerSQL() {
        return this.layerSQL;
    }
    
    public String getSQL() {
        LOGGER.debug("SVSpatialLayer.getSQL() - layerSQL (BEFORE)= " + this.layerSQL);
        if (Strings.isEmpty(this.layerSQL)) {
            this.setInitSQL();
            this.wrappedSQL = "";
        }
        LOGGER.debug("SVSpatialLayer.getSQL() - layerSQL (AFTER)= " + this.layerSQL);
        return this.layerSQL;
    }

    public void setMBRRecalculation(boolean _recalc) {
LOGGER.debug("setMBRRecalculation(" + _recalc + ")");
        this.calculateMBR = _recalc;
    }

    public boolean getMBRRecalculation() {
        return this.calculateMBR;
    }

    public void setNumberOfFeatures(long _number) {
        LOGGER.debug("setNumberOfFeatures()="+_number);
        this.numberOfFeatures = _number;
        if (this.getPreferences().isNumberOfFeaturesVisible() ) {
            this.getSpatialView().getSVPanel().getViewLayerTree().refreshLayerCount(this,_number);
        } else {
            this.getSpatialView().getSVPanel().getViewLayerTree().removeLayerCount(this);
        }
    }
    
    public long getNumberOfFeatures() {
        return this.numberOfFeatures;
    }
    
    protected OraclePreparedStatement setParameters(String          _sql, 
                                                    Envelope _mbr) 
    {
        LOGGER.debug("SVSpatialLayer.setParameters()");
        OraclePreparedStatement pStatement = null;
        String         spatialFilterClause = "";
        STRUCT                  filterGeom = null;
        int                 stmtParamIndex = 1;
        try 
        {
            boolean project = this.project &&
                              this.spatialView.getSRIDAsInteger() != Constants.SRID_NULL &&
                              this.getSRIDAsInteger()             != Constants.SRID_NULL;
            
            int querySRID = project 
                            ? this.spatialView.getSRIDAsInteger() 
                            : (this.spatialView.getSRIDAsInteger()==Constants.SRID_NULL
                               ? this.getSRIDAsInteger()
                               : this.spatialView.getSRIDAsInteger()
                              );
            pStatement = (OraclePreparedStatement)super.getConnection().prepareStatement(_sql);
            
            // Filter Geom is always expressed in terms of the layer's SRID
            //
            filterGeom = this.getSearchFilterGeometry(_mbr,project,querySRID,this.getSRIDAsInteger());
            pStatement.setSTRUCT(stmtParamIndex++, filterGeom);
            LOGGER.debug("SVSpatialLayer.setParameters(): window geometry parameter " + (stmtParamIndex-1) + " to STRUCT is " + (filterGeom==null?"null":"not null"));

            // Set up SDO_Filter clause depending on whether min_resolution is to be applied or not
            // If ST_Geometry, can't use sdoFilterClause
            //
            LOGGER.debug("SVSpatialLayer.setParameters(): hasIndex()=" + this.hasIndex());
            if ( this.hasIndex() && this.isSTGeometry()==false ) {
                spatialFilterClause = this.sdoFilterClause; // Default
                if (this.getMinResolution()) {
                    Point.Double pixelSize = null;
                    try {
                        pixelSize = this.getSpatialView().getMapPanel().getPixelSize();
                    } catch (NoninvertibleTransformException e) {
                        pixelSize = new Point.Double(0.0,0.0);
                    }
                    double maxPixelSize = Math.max(pixelSize.getX(), pixelSize.getY());
                    if (maxPixelSize != 0.0) {
                        spatialFilterClause = String.format(this.sdoFilterMinResClause,maxPixelSize);
                        LOGGER.debug("SVSpatialLayer.setParameters(): spatialFilterClause parameter " + (stmtParamIndex-1) + " to " + spatialFilterClause);
                    }
                }
                pStatement.setString(stmtParamIndex++, spatialFilterClause);
            }
            LOGGER.debug("SVSpatialLayer.setParameters(): this.getPrefreences().isLogSearchStats() = " + this.getPreferences().isLogSearchStats() );
            if ( this.getPreferences().isLogSearchStats() ) {
                LOGGER.info("\n" + _sql + "\n" + 
                            String.format("?=%s\n?=%s", SDO_GEOMETRY.convertGeometryForClipboard(filterGeom),
                                          spatialFilterClause));
            }
        } catch (SQLException sqle) {
            // isView() then say no index
            if ( this.isView() ) {
                this.setIndex(false);
            }
            Tools.copyToClipboard(super.propertyManager.getMsg("SQL_QUERY_ERROR",
                                                               sqle.getMessage()),
                                  _sql + "\n" +
                                  String.format("? %s\n? %s", SDO_GEOMETRY.convertGeometryForClipboard(filterGeom),
                                                spatialFilterClause));
        } 
        LOGGER.debug("returning pStatement");
        return pStatement;
    }

    /**
     * Execute SQL and draw data on given graphical device
     * @param _mbr MBR coordinates
     * @param _g2 graphical device
     * @return if return false, something was wrong (for example, Connection with DB faild)
     */
    public boolean drawLayer(Envelope _mbr, Graphics2D _g2) 
    {
        OracleConnection conn = null;
        try {
            // Make sure layer's connection has not been lost
            conn = super.getConnection();
            if ( conn == null ) {
                LOGGER.warn("SVSpatialLayer.drawLayer(" + this.getLayerNameAndConnectionName() + "). Cannot get layer's database connection.");
                return false;
            }
        } catch (IllegalStateException ise) {
            // Thrown by super.getConnection() indicates starting up, so we do nothing
            LOGGER.warn("SVSpatialLayer.drawLayer(" + this.getVisibleName() + "). Exception getting layer's database connection." + ise.toString());
            return false;
        }
        
        // If not indexed, check again as might have been indexed
        if ( this.hasIndex()==false ) {
            this.setIndex();
            if ( this.hasIndex() ) {
                // force change to SQL 
                this.layerSQL = null;
            }
        }

        // Get and possible wrap the SQL the user may have modified in order to return only the columns needed for visualisation
        // Ensures all columns needed for labelling, colouring, rotation etc area available 
        // in order to reduce network traffic
        //
        String sql = this.getSQL();
        if (Strings.isEmpty(sql)) {
            LOGGER.warn("SVSpatialLayer.drawLayer(" + this.getLayerNameAndConnectionName() + "). Cannot get layer's SQL statement.");
            return false;
        }
        if (Strings.isEmpty(this.wrappedSQL)) {
            sql = this.wrapSQL(sql);
            this.wrappedSQL = sql;
        } else {
            sql = this.wrappedSQL;
        }
        if (Strings.isEmpty(sql)) {
            LOGGER.warn("SVSpatialLayer.drawLayer(" + this.getLayerNameAndConnectionName() + "). Wrapping of layer's SQL statement failed.");
            return false;
        }

        // Set up parameters for statement
        //
        OraclePreparedStatement pStatement = null;
        pStatement = this.setParameters(sql,_mbr);

        // Now execute query and return result
        //
        boolean success = executeDrawQuery(pStatement,sql,_g2);
        return success;
    }

    protected boolean executeDrawQuery(OraclePreparedStatement _pStatement,
                                       String                  _sql2Debug,
                                       Graphics2D              _g2)
    {
        LOGGER.debug("** START: executeDrawQuery\n=======================");
        if ( _g2 == null ) {
            LOGGER.debug("**** Graphics2D is null; return;");
            return false;
        }
        // connection needed for 
        OracleResultSet    ors = null;
        Envelope newMBR = new Envelope(this.getDefaultPrecision());
        String     labelValue = null,
                   shadeValue = null,
              pointColorValue = null,
               lineColorValue = null,
                  sqlTypeName = "";
        boolean isFastPickler = this.getPreferences().isFastPicklerConversion();
        double pointSizeValue = 4,
                   angleValue = 0.0f;
        STRUCT         stGeom = null;
        JGeometry       jGeom = null;
        byte[]    geomPickler = null;
        long    mbrCalcStart  = 0,
                  mbrCalcTime = 0,
                totalFeatures = 0,
                dataReadStart = System.currentTimeMillis(),
                 dataReadTime = 0,
                dataDrawStart = 0,
                 dataDrawTime = 0,
                 executeStart = 0,
                  executeTime = 0;    
        try 
        {
            OracleConnection oConn = (OracleConnection)_pStatement.getConnection();
            // Set graphics2D once for all features
            drawTools.setGraphics2D(_g2);
            executeStart = System.currentTimeMillis();
            ors = (OracleResultSet)_pStatement.executeQuery();
            ors.setFetchDirection(OracleResultSet.FETCH_FORWARD);
            ors.setFetchSize(this.getResultFetchSize());
            executeTime = ( System.currentTimeMillis() - executeStart );
            while ((ors.next()) &&
                   (this.getSpatialView().getSVPanel().isCancelOperation() == false)) 
            {
                /// reading a geometry from database                
                sqlTypeName = ((oracle.sql.STRUCT)ors.getOracleObject(super.getGeoColumn().replace("\"",""))).getSQLTypeName();
                if ( isFastPickler && sqlTypeName.indexOf("MDSYS.ST_")==-1) {
                    geomPickler = ors.getBytes(super.getGeoColumn().replace("\"",""));
                    if (geomPickler == null) { continue; }
                    //convert image into a JGeometry object using the SDO pickler
                    jGeom = JGeometry.load(geomPickler);
                } else {
                    stGeom = (oracle.sql.STRUCT)ors.getOracleObject(super.getGeoColumn().replace("\"",""));
                    if (stGeom == null) continue;
                    // If ST_GEOMETRY, extract SDO_GEOMETRY
                    if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                        stGeom = SDO_GEOMETRY.getSdoFromST(stGeom);
                    } 
                    if (stGeom==null) { continue; }
                    jGeom = JGeometry.load(stGeom);
                }
                if (jGeom == null) continue;
                
                totalFeatures++;
                
                if (this.styling.getLabelColumn() != null) {
                    labelValue = SQLConversionTools.convertToString(oConn,this.styling.getLabelColumn(), ors.getObject(this.styling.getLabelColumn().replace("\"","")));
                }
                if (this.styling.getRotationColumn() != null) {
                    try {
                        angleValue = ors.getNUMBER(this.styling.getRotationColumn().replace("\"","")).doubleValue();
                    } catch (Exception e) {
                        angleValue = 0.0f;
                    }
                }
                if (this.styling.getShadeColumn() != null && this.styling.getShadeType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        // regardless as to whether RGB or Integer, get colour as a string.
                        shadeValue = ors.getString(this.styling.getShadeColumn().replace("\"",""));
                    } catch (Exception e) {
                        shadeValue = "255,255,255";
                    }
                } 
                if (this.styling.getPointColorColumn() != null && this.styling.getPointColorType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        // regardless as to whether RGB or Integer, get colour as a string.
                        pointColorValue = ors.getString(this.styling.getPointColorColumn().replace("\"",""));
                    } catch (Exception e) {
                        pointColorValue = "255,255,255";
                    }
                } 
                if (this.styling.getLineColorColumn() != null && this.styling.getLineColorType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        // regardless as to whether RGB or Integer, get colour as a string.
                        lineColorValue = ors.getString(this.styling.getLineColorColumn().replace("\"",""));
                    } catch (Exception e) {
                        lineColorValue = "0,0,0";
                    }
                }  
                if (this.styling.getPointSizeColumn() != null && this.styling.getPointSizeType() == Styling.STYLING_TYPE.COLUMN) {
                    try {
                        pointSizeValue = ors.getDouble(this.styling.getPointSizeColumn().replace("\"",""));
                    } catch (Exception e) {
                        pointSizeValue = 4;
                    }
                }              
                // Draw the feature
                //
                dataDrawStart = System.currentTimeMillis();
                callDrawFunction(jGeom, 
                                 labelValue, 
                                 shadeValue, 
                                 pointColorValue,
                                 lineColorValue,
                                 (int)(Math.round(pointSizeValue<4.0?4:pointSizeValue) % 72),
                                 this.styling.getRotationValue() == Constants.ROTATION_VALUES.DEGREES 
                                 ? COGO.radians(COGO.normalizeDegrees(angleValue - 90.0f)) 
                                 : angleValue);
                dataDrawTime += ( System.currentTimeMillis() - dataDrawStart );
                
                // Check if we are reccalculating the layer's MBR
                //
                if ( this.getMBRRecalculation() ) {
                    LOGGER.debug("**** MBR Recalculation - processing individual geometry for " + this.getLayerNameAndConnectionName());
                    mbrCalcStart =  System.currentTimeMillis();
                    newMBR.setMaxMBR(SDO_GEOMETRY.getGeoMBR(jGeom));
                    mbrCalcTime += ( System.currentTimeMillis() - mbrCalcStart );
                }
                if ( this.preferences.isQueryLimited() && totalFeatures >= this.preferences.getQueryLimit() ) {
                    break;
                }
            } // while feature to process
            dataReadTime += (  System.currentTimeMillis() - dataReadStart );
            this.setNumberOfFeatures(totalFeatures);
            float featsPerSecond = ( this.getNumberOfFeatures() / (((float)(dataReadTime + executeTime) / (float)Constants.MILLISECONDS) % Constants.SECONDS) );
            LOGGER.logSQL("\n" + 
                           super.getSpatialView().getVisibleName() + ">>" + this.getVisibleName() + "\n" + 
                           ( this.getPreferences().isLogSearchStats() ? 
                           "SQL Execution Time = " + Tools.milliseconds2Time(executeTime) + "\n" : "" ) + 
                           "         Draw Time = " + Tools.milliseconds2Time(dataDrawTime)+ "\n" +
                           ( this.getMBRRecalculation() ? 
                           "     MBR Calc Time = " + Tools.milliseconds2Time(mbrCalcTime)+ "\n" : "" ) + 
                           "    Data Read Time = " + Tools.milliseconds2Time(dataReadTime - (dataDrawTime+mbrCalcTime)) + "\n" +
                           "   Total Read Time = " + Tools.milliseconds2Time(dataReadTime) + "\n" +
                           " Features Returned = " + this.getNumberOfFeatures()  + "\n" +
                           "   Features/Second = " + String.format("%10.2f",featsPerSecond));
            if ( this.getMBRRecalculation() ) {
                LOGGER.debug("**** MBR Recalculation - Final processing " + newMBR.toString());
                if (newMBR.getWidth() == newMBR.getHeight()) {
                  // Must be a single point
                  //
                  Point2D.Double pixelSize = this.getSpatialView().getSVPanel().getMapPanel().getPixelSize();
                  double maxBufferSize = Math.max(pixelSize.getX(),pixelSize.getY()) * this.getPreferences().getSearchPixels();
                  newMBR.setChange(maxBufferSize);
                }
                LOGGER.debug("**** MBR Recalculation - setMBR to " + newMBR.toString());
                super.setMBR(newMBR);
                this.setMBRRecalculation(false);
            }
        } catch (SQLException sqle) {
            // isView() then say no index
            if ( this.isView() ) { this.setIndex(false); };
            String params = "";
            ParameterMetaData pmd = null;
            try {
                pmd = _pStatement.getParameterMetaData();
                for (int i=1;i<=pmd.getParameterCount();i++) {
                    params += "\n" + pmd.getParameterTypeName(i);
                }
            } catch (SQLException e) {
            }
            Tools.copyToClipboard(super.propertyManager.getMsg("SQL_QUERY_ERROR",
                                                               sqle.toString()),
                                  _sql2Debug + params);
            return false;
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null,
                                          super.propertyManager.getMsg("FILE_IO_ERROR",
                                                                       ioe.getLocalizedMessage()),
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (NullPointerException npe) {
            LOGGER.debug("**** NullPointerException - " + npe.toString());
            npe.printStackTrace();
            LOGGER.debug("** FINISH: executeDrawQuery\n========================");
            return false;
        } catch (Exception e) {
            LOGGER.error("SVSpatialLayer.executeDrawQuery - general exception");
            e.printStackTrace();
            LOGGER.debug("** FINISH: executeDrawQuery\n========================");
            return false;
        } finally {
            try { ors.close();        } catch (Exception _e) { }
            try { _pStatement.close(); } catch (Exception _e) { }
        }
        LOGGER.debug("** FINISH: executeDrawQuery\n========================");
        return true;
    }
          
    public LinkedHashMap<String, String> getColumnsAndTypes(boolean _onlyNumbersDatesAndStrings,
                                                            boolean _fullDataType) 
    throws SQLException 
    {
        // Should try and get columns etc from SQL being executed.
        // Otherwise go to table itself.
        //
        try {
            OracleConnection conn = super.getConnection();
            String layerSql = this.getSQL();
            if (Strings.isEmpty(layerSql)) {
                return new LinkedHashMap<String, String>(MetadataTool.getColumnsAndTypes(conn,
                                                                                         this.getSchemaName(),
                                                                                         this.getObjectName(),
                                                                                         _onlyNumbersDatesAndStrings,
                                                                                         _fullDataType));
            }
            LinkedHashMap<String, String> columnsTypes = new LinkedHashMap<String,String>(255);
            try {
                LOGGER.debug("SQL Before="+layerSql + "\nTReplacing"+"SDO_FILTER(t." + this.getGeoColumn() + ",?,?) = 'TRUE'");
                String sql = ""; 
                if ( this.isSTGeometry() ) {
                    sql = layerSql.replace("SDO_FILTER(t." + this.getGeoColumn() + ",?) = 'TRUE'","1=0");
                } else {
                    sql = layerSql.replace("SDO_FILTER(t." + this.getGeoColumn() + ",?,?) = 'TRUE'","1=0");
                }
                LOGGER.debug("SQL After ="+sql);
                OraclePreparedStatement pstmt;
                pstmt = (OraclePreparedStatement)conn.prepareStatement(sql);
                LOGGER.debug("prepared");
                boolean include = false;
                String colName = "",
                       colTypeName = "";
                ResultSetMetaData rsmd = pstmt.getMetaData();
                LOGGER.debug("getMetaData()");
                for (int col=1;col<=rsmd.getColumnCount();col++) {
                    include     = false;
                    colName     = rsmd.getColumnLabel(col);
                    colTypeName = rsmd.getColumnTypeName(col);
                    LOGGER.debug("columnsTypes.put("+colName+","+colTypeName+")");
                    if ( _onlyNumbersDatesAndStrings ) {
                        include = colTypeName.startsWith("TIMESTAMP") ||
                                  colTypeName.startsWith("INTERVAL")  ||
                                  colTypeName.equalsIgnoreCase("DATE") ||
                                  colTypeName.equalsIgnoreCase("CHAR") ||
                                  colTypeName.equalsIgnoreCase("NVARCHAR2") ||
                                  colTypeName.equalsIgnoreCase("VARCHAR") ||
                                  colTypeName.equalsIgnoreCase("VARCHAR2") ||
                                  colTypeName.equalsIgnoreCase("ROWID") ||
                                  colTypeName.equalsIgnoreCase("FLOAT") ||
                                  colTypeName.equalsIgnoreCase("BINARY_DOUBLE") ||
                                  colTypeName.equalsIgnoreCase("BINARY_FLOAT") ||
                                  colTypeName.equalsIgnoreCase("NUMBER");
                    } else {
                        include = true;
                    }
                    if ( include ) {
                        if ( _fullDataType ) {
                            if (rsmd.getPrecision(col)==0) {
                                colTypeName += (colTypeName.contains("CHAR") ? "(" + rsmd.getPrecision(col) + ")" : "");
                            } else if (rsmd.getScale(col) == 0 ) {
                                  colTypeName += "(" + rsmd.getPrecision(col) + ")" ;
                            } else {
                                  colTypeName += "(" + rsmd.getPrecision(col) + "," + rsmd.getScale(col) + ')';
                            }
                        }
                        LOGGER.debug("columnsTypes.put(\""+colName+"\"," +colTypeName+")");
                        columnsTypes.put((String)"\""+colName+"\"",colTypeName);
                    }
                }
                pstmt.close();
            } catch (SQLException e) {
                LOGGER.debug("Problem checking draw SQL - " + e.toString());
            }
            return columnsTypes;
        } catch (Exception ex) {
            LOGGER.error(super.propertyManager.getMsg("QUERY_ERROR_COLUMNS_AND_TYPES",
                                                      this.getObjectName(),
                                                      ex.getMessage()));
            return null;
        }
    }

    /**
     * Convert STRUCT object to JGeometry and call drawX function base on Geometry type
     * @history Simon Greener April 2010.
     *           Added call to draw geometry via new java.awt.Shape aware drawGeometry funciton
     * @history Simon Greener May 31st 2010.
     *          Moved setting of graphics2D of drawTools to calling function
     * @history Simon Greener June 2nd 2010.
     *          Got rid of super.lastReadLayerMBR.setMBR(geo.getMBR())
     *          because we are processing single geometries and NOT the whole layer.
     *          Also, geo.getMBR() returns different arrays depending on whether xy or xyz etc.
     *          - Also moved setting of temporary layer transparency to own function
     *          and to before this function is called (for each and all geometries)
     */
    public void callDrawFunction(STRUCT _struct, 
                                 String _label, 
                                 String _shadeValue,
                                 String _pointColorValue,
                                 String _lineColorValue,
                                 int    _pointSizeValue,
                                 double _rotationAngle) 
    throws SQLException,
           IOException 
    {
        if (_struct == null) {
            return;
        }
        STRUCT stGeom = _struct;
        String sqlTypeName = _struct.getSQLTypeName();
        if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
            stGeom = SDO_GEOMETRY.getSdoFromST(_struct);
        } 
        if (stGeom == null) {
            return;
        }
        JGeometry geo = JGeometry.load(stGeom);
        if (geo == null) {
            return;
        }
        callDrawFunction(geo, 
                         _label, 
                         _shadeValue, 
                         _pointColorValue, 
                         _lineColorValue, 
                         _pointSizeValue, 
                         _rotationAngle);
    }

    public void callDrawFunction(JGeometry _geo, 
                                 String    _label,
                                 String    _shadeValue,
                                 String    _pointColorValue,
                                 String    _lineColorValue,
                                 int       _pointSizeValue,
                                 double    _rotationAngle) 
    throws IOException 
    {
        if (_geo == null) {
            return;
        }
        drawTools.drawGeometry(_geo, 
                               _label, 
                               _shadeValue, 
                               _pointColorValue, 
                               _lineColorValue, 
                               _pointSizeValue,
                               this.styling.getRotationTarget(), 
                               _rotationAngle,
                               this.styling.getLabelAttributes(this.getSpatialView().getMapPanel().getMapBackground()),
                               this.styling.getLabelPosition(),
                               this.styling.getLabelOffset());
    }
  
    /**
     * Create copy of current class.
     */
    public SVSpatialLayer createCopy() 
    throws Exception 
    {
        // _renderOnly is ignored for SVSpatialLayers
        //
        SVSpatialLayer newLayer = null;

        // Shared SVLayer stuff
        //
        newLayer = new SVSpatialLayer(super.getSpatialView());
        // set SVLayer properties (What is a copy? Is it a render layer?)
        newLayer.setMetadataEntry(super.getMetadataEntry());
        newLayer.setSRIDType(super.getSRIDType());
        newLayer.setGeometryType(this.getGeometryType());
        newLayer.setConnectionName(super.connName);
        newLayer.setIndex(this.hasIndex());

        newLayer.setMBR(this.getMBR());
        String newName = super.getSpatialView().getSVPanel().getViewLayerTree().checkName(this.getLayerName());
        newLayer.setLayerName(newName);
        newLayer.setVisibleName(this.getVisibleName()+layerCopySuffix);
        newLayer.setDesc(this.getDesc());
        newLayer.setDraw(this.isDraw());
        newLayer.setSQL(this.getSQL());
        newLayer.setMinResolution(this.getMinResolution());
        newLayer.setFetchSize(this.getFetchSize());
        newLayer.setPrecision(this.getPrecision(false));
        newLayer.setProject(this.getProject(),false);

        // Label
        newLayer.setStyling(new Styling(this.styling));
        
        return newLayer;
    }

    public void setMinResolution(boolean _minResolution) {
        if ( this.geometryType == Constants.GEOMETRY_TYPES.POINT )
            this.minResolution = false;
        else
            this.minResolution = _minResolution;
    }
  
    public boolean getMinResolution() {
        return ( this.geometryType == Constants.GEOMETRY_TYPES.POINT ) ? false : this.minResolution;
    }
  
    public int getPrecision(boolean _calculate) {
        return super.getPrecision(_calculate);
    }

    public double getTolerance() {
        return super.getTolerance();
    }
  
    public void setResultFetchSize(int _resultFetchSize) {
        this.resultFetchSize = _resultFetchSize;
    }
  
    public int getResultFetchSize() {
        return this.resultFetchSize;
    }
    
    public SVSpatialLayerDraw getDrawTools() {
        return this.drawTools;
    }
  
    public SpatialView getView() {
        return super.getSpatialView();
    }
  
    public boolean getProject() {
        return this.project;
    }
  
    public void setProject(boolean _yes, boolean _calcMBR) {
        this.project = _yes;
        if (_calcMBR) {
            // Anytime this is called we need to recalculate the MBR of the layer
            this.setLayerMBR(super.mbr, super.getSRIDAsInteger());
        }
    }

    public void setFetchSize(int _fetchSize) {
        if (_fetchSize == 0)
            this.resultFetchSize = this.getPreferences().getFetchSize();
        else
            this.resultFetchSize = _fetchSize;
    }

    public int getFetchSize() {
        return this.resultFetchSize;
    }

    private String getIdentifySQL(boolean _project) 
    {
        String sql = "";
        String spatialIndexName;
        try {
            spatialIndexName = MetadataTool.getSpatialIndexName(this.getConnection(), 
                                                                super.getSchemaName(), 
                                                                super.getObjectName(), 
                                                                super.getGeoColumn());
        } catch (SQLException e) {
            spatialIndexName = "";
        }
        if ( this.hasIndex() ) {
            String queryHint = "/*+ORDERED" + (Strings.isEmpty(spatialIndexName) ? "" : " INDEX(b " + spatialIndexName+")") + "*/";
            sql = "WITH searchDistance As (" +
                  "  SELECT MDSYS.SDO_GEOM.SDO_DISTANCE(?/*search pt1*/,?/*DistancePoint*/,?/*tol*/,?/*unit*/) AS dist \n" +
                  "    FROM DUAL \n" +
                  ")\n" +
                  "SELECT " + queryHint + 
                          (_project 
                           ? "MDSYS.SDO_CS.TRANSFORM(a." + this.getGeoColumn() + "," + 
                                                           this.spatialView.getSRID() + 
                                                    ") as " + this.projectedGeometryName + ", a.*\n" 
                           : "a.*\n" 
                           ) +
                  "  FROM (" + this.getSQL() + "\n " ;
            if (this.getPreferences().isNN() ) {
               sql += "      ) a,\n" +
                    "       "+this.getFullObjectName() + " b,\n" +
                      "       searchDistance s \n" +
                      " WHERE SDO_NN(b." + super.getGeoColumn() + ",?,?,1) = 'TRUE' \n" +
                      "   AND SDO_NN_DISTANCE(1) < s.dist \n" +  
                      "   AND b.rowid = a.rowid \n " +
                      " ORDER BY sdo_nn_distance(1)";
            } else {
               sql += "   AND MDSYS.SDO_WITHIN_DISTANCE(" + super.getGeoColumn() + ",?/*search point*/,'distance=' || (SELECT s.dist FROM searchDistance s) || ?) = 'TRUE' \n" +
                      "       ) a";
            }
        } else {
            // "   AND MDSYS.SDO_GEOM.SDO_DISTANCE(t." + super.getGeoColumn() + ",
            //                                     ?/*searchPoint*/,
            //                                     ?/*tol*/,
            //                                     ?/*unit*/) < MDSYS.SDO_GEOM.SDO_DISTANCE(?/*searchPoint*/,
            //                                                                              ?/*distancePoint*/,
            //                                                                              ?/*tol*/,
            //                                                                              ?/*unit*/)";
            // If Not Indexed: 7 Parameters
            // SearchPoint, Tol, Unit
            // SearchPoint, DistancePoint, Tol, Unit
            sql = this.getSQL() + "\n" +
                       "   AND MDSYS.SDO_GEOM.SDO_DISTANCE(t." + super.getGeoColumn() + ",?,?/*tol*/,?/*unit*/) < MDSYS.SDO_GEOM.SDO_DISTANCE(?,?,?/*tol*/,?/*unit*/) \n";
        }
        LOGGER.logSQL(sql);
        return sql;
    }

    @SuppressWarnings("unused")
	private double getSearchRadius(int _numSearchPixels) 
    throws NoninvertibleTransformException 
    {
        double maxSearchRadius = 0.0;
        // Get search radius
        // Is diagonal distance computed as sqrt of size of pixel (* number of pixels in search)
        // in X and Y squared (ie pythagoras)
        //
        Point.Double pixelSize = this.spatialView.getMapPanel().getPixelSize();
        maxSearchRadius = Math.sqrt(Math.pow(pixelSize.getX() * _numSearchPixels, 2.0f) +
                                    Math.pow(pixelSize.getY() * _numSearchPixels, 2.0f));            
        if (!this.project) 
        {
            if (super.getSRIDType() == Constants.SRID_TYPE.UNKNOWN)
                super.setSRIDType();
            switch (super.getSRIDType()) {
            case GEODETIC_COMPOUND:
            case GEODETIC_GEOCENTRIC:
            case GEODETIC_GEOGRAPHIC2D:
            case GEODETIC_GEOGRAPHIC3D:
            case GEOGRAPHIC2D:
                // Generate search distance as this is geodetic 
                Point screenPoint1 = this.spatialView.getMapPanel().getScreenCenter();
                Point2D.Double worldPoint1 = (Point2D.Double)this.spatialView.getMapPanel().ScreenToWorld(screenPoint1);
                Point screenPoint2 = new Point(screenPoint1.x+_numSearchPixels,screenPoint1.y);
                // System.out.println("screenPoint1 " + screenPoint1.toString() + " => screenPoint2 " + screenPoint2.toString());
                Point2D.Double worldPoint2 = (Point2D.Double)this.spatialView.getMapPanel().ScreenToWorld(screenPoint2);
                // System.out.println("worldPoint1 " + worldPoint1.toString() + " => " + worldPoint2.toString());
                // There are more efficient ways of calculating this distance... 
                // perhaps via an inline computation query in actual SDO_NN SQL sdo_geom.sdo_distance(sdo_point(1),sdo_point(2))
                //
                try { 
                    double distVincenty = COGO.distVincenty(worldPoint1.getX(),worldPoint1.getY(), worldPoint2.getX(),worldPoint2.getY());
                    maxSearchRadius = distVincenty;
                } catch (Exception e) {
                	LOGGER.info("DistVincenty threw exception " + e.getMessage());
                }
                break;
			default:
				break;
            }
        }
        return maxSearchRadius;
    }
    
    /**
     * @history Simon Greener April 13th 2010
     *          - Reorganised SQL
     *          - Moved to use of new Constants class
     * @history Simon Greener June 7th 2010
     *          - Added
     *            ORDER BY
     *             SDO_NN_DISTANCE ancillary operator
     *          - etc to try and address John O'Toole's partition problem
     * @history Simon Greener December 15th 2010
     *          - Fully parameterized queries. 
     *          - Changed method of calculating cutoff distance for geodetic data
     * @history Simon Greener December 15th 2010
     *          - New approach that honors current SQL defining layer
     **/
    public ArrayList<QueryRow> queryByPoint(Point2D _worldPoint, 
                                            int     _numSearchPixels) 
    {
        STRUCT            retSTRUCT = null; // read SDO_GEOMETRY column
        ArrayList<QueryRow> retList = new ArrayList<QueryRow>(); // list of return rows
        int         numSearchPixels = _numSearchPixels <= 0 ? this.getPreferences().getSearchPixels() : _numSearchPixels;
        // Need future check for 3D indexed layers??
        // Sable 5-1 Data and Index Dimensionality, and Query Support
        //
        String querySQL = "";
        String params = "";
        String spatialFilterClause = ""; 
        OracleConnection conn = null;
        try {
            // Set up the connection and statement
            //
            conn = super.getConnection();
            if (conn == null) { return null; }
            boolean project = this.project &&
                              this.spatialView.getSRIDAsInteger() != Constants.SRID_NULL &&
                              this.getSRIDAsInteger()             != Constants.SRID_NULL;

            // If we need to transform the geometry for display then we are best to give it a different name in the output string
            String geoColumn = project ? projectedGeometryName.toUpperCase() : this.getGeoColumn().toUpperCase();
            geoColumn = geoColumn.replace("\"","");

            querySQL = getIdentifySQL(project);
            
            // Get target SRID and units_parameter in case layer have been drag-and-dropped to a different view/srid
            //
            String     lengthUnits  = Tools.getViewUnits(spatialView,Constants.MEASURE.LENGTH);            
            String units_parameter = "";
            int          querySRID = -1;
            if ( project ) {
                querySRID = this.spatialView.getSRIDAsInteger(); 
                units_parameter = String.format(",unit=%s",lengthUnits);
            } else {
                querySRID = this.spatialView.getSRIDAsInteger()==Constants.SRID_NULL
                            ? this.getSRIDAsInteger()
                            : this.spatialView.getSRIDAsInteger();
                if ( this.getSRIDAsInteger() != Constants.SRID_NULL)
                    units_parameter = String.format(",unit=%s",lengthUnits);
            }
            
            // Create search mbr, distance measurement variables
            //
            Point2D.Double pixelSize = null;
            STRUCT   searchPoint = null;
            STRUCT distancePoint = null;
            STRUCT     searchMBR = null;
            try {
                // Compute End point of search distance line
                // Note: first point of line denoting search distance is the passed in point (world units)
                //
                pixelSize = this.getSpatialView().getSVPanel().getMapPanel().getPixelSize();
                JGeometry searchJPoint = new JGeometry(_worldPoint.getX(),_worldPoint.getY(),querySRID);
                searchPoint = project ? MetadataTool.projectJGeometry(conn,searchJPoint,this.getSRIDAsInteger()) 
                              : (STRUCT) JGeometry.storeJS(conn,  searchJPoint);               
                JGeometry distanceJPoint = new JGeometry(_worldPoint.getX() + ((pixelSize.getX() >= pixelSize.getY()) ? (numSearchPixels*pixelSize.getX()) : 0.0),
                                                         _worldPoint.getY() + ((pixelSize.getY() >  pixelSize.getX()) ? (numSearchPixels*pixelSize.getY()) : 0.0),
                                                         querySRID);
                distancePoint = project ? MetadataTool.projectJGeometry(conn,distanceJPoint,this.getSRIDAsInteger()) 
                                : (STRUCT) JGeometry.storeJS(conn,  distanceJPoint);
                // SDO_Filter geometry has same SRID as layer
                Envelope mbr = new Envelope(_worldPoint.getX() - (numSearchPixels*pixelSize.getX()/1.9),
                                                          _worldPoint.getY() - (numSearchPixels*pixelSize.getY()/1.9),
                                                          _worldPoint.getX() + (numSearchPixels*pixelSize.getX()/1.9),
                                                          _worldPoint.getY() + (numSearchPixels*pixelSize.getY()/1.9));
                JGeometry searchJMBR = new JGeometry(mbr.getMinX(),mbr.getMinY(),mbr.getMaxX(),mbr.getMaxY(),this.getSRIDAsInteger());
                searchMBR = (STRUCT) JGeometry.storeJS(conn,searchJMBR);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Failed to create search SDO_GEOMETRY objects " + e.getMessage());
                return null;
            }


            // Create statement
            //
            OraclePreparedStatement pStatement = (OraclePreparedStatement)conn.prepareStatement(querySQL);

            // Assign parameters
            //
            int stmtParamIndex = 1;

            if ( this.hasIndex() ) {
                // Common 
                // "WITH searchDistance As (SELECT MDSYS.SDO_GEOM.SDO_DISTANCE(?/*search pt1*/,?/*DistancePoint*/,?/*tol*/,?/*unit*/) AS dist FROM DUAL )\n" +
                //   "SELECT /*+ ORDERED*/ " +
                //           (_project 
                //               ? "MDSYS.SDO_CS.TRANSFORM(a." +  this.getGeoColumn() + "," + this.spatialView.getSRID() + ") as " +  projectedGeometryName + ", a.*\n" 
                //               : "a.*\n" 
                //               )
                //"  FROM (" + this.getSQL() + 
                pStatement.setOracleObject(stmtParamIndex++,searchPoint);                     params += String.format("? %s\n",RenderTool.renderSTRUCTAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
                pStatement.setOracleObject(stmtParamIndex++,distancePoint);                   params += String.format("? %s\n",RenderTool.renderSTRUCTAsPlainText(distancePoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
                pStatement.setDouble      (stmtParamIndex++,this.getTolerance());             params += String.format("? %f\n",this.getTolerance());
                pStatement.setString      (stmtParamIndex++,units_parameter.replace(",","")); params += String.format("? '%s'\n",units_parameter.replace(",",""));
                // MBR for SDO_FILTER 
                //
                pStatement.setSTRUCT      (stmtParamIndex++, searchMBR);                          params += String.format("? %s\n", SDO_GEOMETRY.convertGeometryForClipboard(searchMBR));
                if ( ! this.isSTGeometry() ) {
                    spatialFilterClause = this.sdoFilterClause; // Default
                    if (this.getMinResolution()) {
                        double maxPixelSize = Math.max(pixelSize.getX(), pixelSize.getY());
                        if (maxPixelSize != 0.0) {
                            spatialFilterClause = String.format(this.sdoFilterMinResClause,maxPixelSize);
                        }
                    }
                    pStatement.setString(stmtParamIndex++, spatialFilterClause);                  params += "? '"+spatialFilterClause+"'\n";
                }
                if ( this.getPreferences().isNN() ) {
                    //       "       ) a,\n" +
                    //     "       "+this.getFullObjectName() + " b,\n" +
                    //       "       searchDistance s \n" +
                    //       " WHERE SDO_NN(b." + super.getGeoColumn() + ",?,?,1) = 'TRUE' \n" +
                    //       "   AND SDO_NN_DISTANCE(1) < s.dist \n" +  
                    //       "   AND b.rowid = a.rowid \n " +
                    //       " ORDER BY sdo_nn_distance(1)";
                    pStatement.setOracleObject(stmtParamIndex++,searchPoint);                 params += String.format("? %s\n",RenderTool.renderSTRUCTAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
                    String     sdo_num_res = "sdo_num_res=" + String.valueOf(numSearchPixels * 2);
                    pStatement.setString      (stmtParamIndex++,sdo_num_res+units_parameter); params += "? '" + sdo_num_res + "'\n";                    
                } else {
                    // "   AND MDSYS.SDO_WITHIN_DISTANCE(" + super.getGeoColumn() + ",?/*search point*/,'distance=' || (SELECT s.dist FROM searchDistance s) || ?) = 'TRUE' ) a\n";
                    pStatement.setOracleObject(stmtParamIndex++,searchPoint);                     params += String.format("? %s\n",RenderTool.renderSTRUCTAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
                    pStatement.setString      (stmtParamIndex++,units_parameter);                 params += String.format("? '%s'\n",units_parameter);
                }
            } else {
                pStatement.setSTRUCT(stmtParamIndex++, searchMBR);                                params += String.format("? %s\n", SDO_GEOMETRY.convertGeometryForClipboard(searchMBR));
                pStatement.setDouble(stmtParamIndex++, this.getTolerance());                      params += String.format("? '%s'\n",this.getTolerance());
                // "   AND MDSYS.SDO_GEOM.SDO_DISTANCE(t." + super.getGeoColumn() + ",
                //                                     ?/*searchPoint*/,
                //                                     ?/*tol*/,
                //                                     ?/*unit*/) < MDSYS.SDO_GEOM.SDO_DISTANCE(?/*searchPoint*/,
                //                                                                              ?/*distancePoint*/,
                //                                                                              ?/*tol*/,
                //                                                                              ?/*unit*/)";
                // If Not Indexed: 7 Parameters
                // SearchPoint, Tol, Unit
                // SearchPoint, DistancePoint, Tol, Unit
                pStatement.setOracleObject(stmtParamIndex++,searchPoint);                     params += String.format("? %s\n",RenderTool.renderSTRUCTAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
                pStatement.setDouble      (stmtParamIndex++,this.getTolerance());             params += String.format("? %f\n",this.getTolerance());
                pStatement.setString      (stmtParamIndex++,units_parameter.replace(",","")); params += String.format("? '%s'\n",units_parameter.replace(",",""));
                pStatement.setOracleObject(stmtParamIndex++,searchPoint);                     params += String.format("? %s\n",RenderTool.renderSTRUCTAsPlainText(searchPoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
                pStatement.setOracleObject(stmtParamIndex++,distancePoint);                   params += String.format("? %s\n",RenderTool.renderSTRUCTAsPlainText(distancePoint, Constants.bracketType.NONE, this.spatialView.getPrecision(false)));
                pStatement.setDouble      (stmtParamIndex++,this.getTolerance());             params += String.format("? %f\n",this.getTolerance());
                pStatement.setString      (stmtParamIndex++,units_parameter.replace(",","")); params += String.format("? '%s'\n",units_parameter.replace(",",""));
            }

            // ****************** Execute the query ************************
            //
            OracleResultSet ors = (OracleResultSet)pStatement.executeQuery();
            ors.setFetchDirection(OracleResultSet.FETCH_FORWARD);
            ors.setFetchSize(this.getResultFetchSize());
            OracleResultSetMetaData rSetM = (OracleResultSetMetaData)ors.getMetaData(); // for column name

            String value = "";
            String columnName = "";
            String columnTypeName = "";
            String rowID = "";
            while (ors.next()) {
                LinkedHashMap<String, Object> calValueMap = new LinkedHashMap<String, Object>(rSetM.getColumnCount() - 1);
                for (int col = 1; col <= rSetM.getColumnCount(); col++) {
                    columnName     = rSetM.getColumnName(col);
                    columnTypeName = rSetM.getColumnTypeName(col);
                    if (columnTypeName.equals("LONG")) {
                        // LONGs will kill the SQL stream and we can't use them anyway
                        System.out.println("GeoRaptor ignored " + rSetM.getColumnName(col) + "/" + rSetM.getColumnTypeName(col));
                        continue;
                    }
                    if (columnName.equalsIgnoreCase(geoColumn)) {
                        retSTRUCT = (oracle.sql.STRUCT)ors.getObject(col);
                        if (ors.wasNull() || retSTRUCT==null) {
                            break; // process next row
                        }                        
                        String sqlTypeName = retSTRUCT.getSQLTypeName();
                        // If ST_GEOMETRY, extract SDO_GEOMETRY
                        if ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) {
                            retSTRUCT = SDO_GEOMETRY.getSdoFromST(retSTRUCT);
                        } 
                        if (retSTRUCT == null) break;
                    } else {
                          if ( Tools.dataTypeIsSupported(columnTypeName) )
                          {
                              try
                              {
                            	  @SuppressWarnings("unused")
								  Object objValue = ors.getObject(col);
                                  if (ors.wasNull()) {
                                      value = "NULL";
                                  } else {
                                      if ( ors.getMetaData().getColumnType(col) == OracleTypes.ROWID ) {
                                          rowID = ors.getROWID(col).stringValue(); 
                                          value = rowID;
                                      } else {
                                          value = SQLConversionTools.convertToString(conn, ors, col);
                                          if (value == null)
                                              value = "NULL";
                                      }
                                  }
                              } catch (Exception e) {
                                  value = "NULL";
                              }
                              calValueMap.put(rSetM.getColumnName(col), value);
                          }
                    }
                }
                retList.add(new QueryRow(rowID, calValueMap, retSTRUCT));
            }
            ors.close();
            pStatement.close();
            if ( this.getPreferences().isLogSearchStats() ) {
                LOGGER.fine("\n" + querySQL + "\n" + params);
            }
        } catch (SQLException sqlex) {
            Tools.copyToClipboard(super.propertyManager.getMsg("SQL_QUERY_ERROR",sqlex.getMessage()),querySQL + "\n" + params);
/*        } catch (NoninvertibleTransformException nte) {
            JOptionPane.showMessageDialog(null,
                                          this.getSpatialView().getSVPanel().getMapPanel().getPropertyManager().getMsg("ERROR_SCREEN2WORLD_TRANSFORM")+ "\n" + nte.getLocalizedMessage(),
                                          MainSettings.EXTENSION_NAME,
                                          JOptionPane.ERROR_MESSAGE);
*/
        } 
        return retList;
    }

} // class SVSpatialLayer
