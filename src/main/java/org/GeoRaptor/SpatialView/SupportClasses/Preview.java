package org.GeoRaptor.SpatialView.SupportClasses;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.geotools.util.logging.Logger;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;


@SuppressWarnings("deprecation")
public class Preview 
extends JPanel 
implements ComponentListener
{
	private static final long serialVersionUID = 5989780313861347975L;

	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.SupportClasses.Thumbnail");

    /**
     * For access to preferences
     */
    protected Preferences mainPrefs;

    private BufferedImage previewImage = null;
    private MutableAttributeSet 
                        attributes = null;
    private int         gridMargin = 30;  // Top    and Right
    private int       labelPadding = 60;  // Bottom and Left
    private int         tickLength =  4;
    private int         yDivisions = 10;
    private int         xDivisions = 10;
        
    private int      vertexMarkSize = 6;
    private Font         vertexFont = new Font("Arial",Font.BOLD,12);
    private PointMarker.MARKER_TYPES 
                         vertexMark = PointMarker.MARKER_TYPES.DONUT;
    private Color       vertexColor = new Color(  0, 153,  76, 255);
    private int       pointMarkSize = 8;
    private PointMarker.MARKER_TYPES 
                          pointMark = PointMarker.MARKER_TYPES.STAR;
        
    private Font mbrWidthHeightFont = new Font("Arial",Font.ITALIC,9);
    private Color     mbrLabelColor = new Color(  0,   0, 204, 255);

    private Font      gridLabelFont = new Font("Arial",Font.BOLD,9);    
    private Color    gridLabelColor = new Color(  0,   0,   0, 255);
    private Color         gridColor = new Color(200, 200, 200, 210);
    
    private Color         lineColor = new Color( 44, 102, 230, 200);
    private Color         fillColor = new Color(255,   0,   0, 150);
    private Stroke      SOLID_GRAPH = new BasicStroke(1.5f);
    private Stroke     DASHED_GRAPH = new BasicStroke(1.0f,
                                                      BasicStroke.CAP_BUTT,
                                                      BasicStroke.JOIN_MITER,
                                                      10.0f,
                                                      new float[]{10.0f}, 
                                                      0.0f);
    private List<JGeometry>          geoSet = null;
    private Envelope             mbr = null;
    private AffineTransform affineTransform = null;
    private Graphics2D                  g2d = null;
    private Dimension             imageSize = null;
    private double   geometryMBRAspectRatio = 0.0f;
    private double        minMbrAspectRatio = 3.0f;
    private boolean             widthBigger = true;
    private boolean              isGeodetic = false;
    private boolean        horizontalLabels = false;
    private boolean          vertexLabeling = false;
    private JFrame              parentFrame = null;
    
    public Preview(JGeometry       _geom, 
                   List<JGeometry> _geomSet,
                   Dimension       _imageSize,
                   Connection      _conn,
                   JFrame          _frame)
    {
        this.parentFrame = _frame;
        initialize(_geom,_geomSet,_imageSize, _conn);
    }

    public Preview(JGeometry  _geom, 
                   Dimension  _imageSize,
                   Connection _conn,
                   JFrame     _frame)
    {
        this.parentFrame = _frame;
        initialize(_geom,null,_imageSize,_conn);
    }

    public Preview(STRUCT     _geoStruct, 
                   Dimension  _imageSize,
                   Connection _conn,
                   JFrame     _frame)
    {
        this.parentFrame = _frame;
        if ( _geoStruct == null ) {
            return ;
        }
        String sqlTypeName = "";
        try { sqlTypeName = _geoStruct.getSQLTypeName(); } catch (SQLException e) {LOGGER.error("Thumbnail: Failed to get sqlTypeName of Struct"); return; }
        STRUCT stGeom = ( sqlTypeName.indexOf("MDSYS.ST_")==0 ) ? SDO_GEOMETRY.getSdoFromST(_geoStruct) 
                        : _geoStruct;
        JGeometry geom = null;
        try { geom = JGeometry.load(stGeom); } catch (SQLException e) { geom = null; }
        if ( geom == null ) {
            LOGGER.error("Failed to get convert STRUCT to JGeometry");
            return ;
        }
        initialize(geom,null,_imageSize,_conn);
    }

    private void initialize(JGeometry       _geom, 
                            List<JGeometry> _geomSet,
                            Dimension       _imageSize,
                            Connection      _conn)
    {
        this.mainPrefs = MainSettings.getInstance().getPreferences();

        if ( _geom    == null && 
             _geomSet == null ||
             (_geomSet != null && 
              _geomSet.size() == 0 ) )  {
            return;
        }

        this.setLabelAttributes("Arial",Font.BOLD,9,Color.BLACK,null);
        
        JGeometry jGeom = _geom!=null?_geom:(_geomSet.size()==1?_geomSet.get(0):null);

        int SRID = -1,
            gtype = -1;
        if ( jGeom != null ) {
            SRID = jGeom.getSRID();
            gtype = jGeom.getType();
            if (gtype == JGeometry.GTYPE_COLLECTION ) {
                this.geoSet = new ArrayList<JGeometry>();
                // Need to extract individual elements that can be mapped to Shapes
                JGeometry[] _geoList = jGeom.getElements();
                for (JGeometry _g : _geoList) {
                    if ( _g != null ) {
                        this.geoSet.add(_g);
                    }
                }
            } else {
                this.geoSet = new ArrayList<JGeometry>(1);
                this.geoSet.add(jGeom);
            }
        } else {
            this.geoSet = new ArrayList<JGeometry>(_geomSet.size());
            this.geoSet.addAll(_geomSet);
            SRID = _geomSet.get(0).getSRID();
        }
        
        if (SRID != -1) {
            try {
                String dRefSysKind;
                dRefSysKind = MetadataTool.getSRIDRefSysKind(_conn, String.valueOf(SRID));
                this.isGeodetic = dRefSysKind.startsWith("GEO");
            } catch (SQLException e) {
                this.isGeodetic = false;
            }
        }

        // MBR ....
        //
        this.mbr = SDO_GEOMETRY.getGeoMBR(geoSet);
        if (this.mbr == null) {
            LOGGER.error("Failed to get geometry(s) this.mbr");
            return;
        }
        this.mbr.setPrecision((this.isGeodetic?8:3));  // hardcoded.

        // LOGGER.info("Before MBR Normalize Check=" + this.mbr.toString() + " width("+this.mbr.getWidth()+")/height("+this.mbr.getHeight()+")=" + (this.mbr.getWidth()/this.mbr.getHeight()));
        // If one side of this.mbr is 0 then modify it.        
        //
        if ( this.mbr.getWidth()==0 || 
             this.mbr.getHeight()==0 ) {
            double halfSide = Math.max(this.mbr.getWidth(),
                                       this.mbr.getHeight()) / 2.0;
            if ( halfSide == 0.0 ) { halfSide = 0.5; }
            this.mbr = new Envelope(this.mbr.centre().getX() - halfSide,
                                           this.mbr.centre().getY() - halfSide,
                                           this.mbr.centre().getX() + halfSide,
                                           this.mbr.centre().getY() + halfSide);
        }
        // Normalise MBR if ratio > 10
        //
        if ( (Math.max(this.mbr.getWidth(),this.mbr.getHeight()) / 
              Math.min(this.mbr.getWidth(),this.mbr.getHeight())) > this.minMbrAspectRatio) {
            this.mbr.Normalize(_imageSize);
        }
        // Normalize window to MBR
        //
        this.widthBigger = (this.mbr.getWidth() > this.mbr.getHeight() ? true : false);
        this.geometryMBRAspectRatio = (this.mbr.getWidth() / this.mbr.getHeight());

        // LOGGER.info("Before MBR Normalize Check=" + this.mbr.toString() + " width("+this.mbr.getWidth()+")/height("+this.mbr.getHeight()+")=" + (this.mbr.getWidth()/this.mbr.getHeight()));
        // LOGGER.info("widthBigger="+ this.widthBigger + " Aspect="+ this.geometryMBRAspectRatio);

        // Normalise requested image size using MBR
        //
        this.imageSize = new Dimension(_imageSize);
        this.imageSize.setSize(this.widthBigger==true  ? this.imageSize.getWidth()  : this.imageSize.getHeight() * this.geometryMBRAspectRatio,
                               this.widthBigger==false ? this.imageSize.getHeight() : this.imageSize.getWidth()  / this.geometryMBRAspectRatio);
        // Ensure Panel width/height ratio same as MBR
        // LOGGER.info("Initialise: imageSize=" + this.imageSize.toString());
        GridBagLayout gbl = new GridBagLayout();
        this.setLayout(gbl);  // to enable minimum jpanel size
        this.setMinimumSize  (new Dimension(this.imageSize));
        this.setPreferredSize(new Dimension(this.imageSize));
        this.setSize(this.imageSize.width,
                     this.imageSize.height); 

        // Set underlying graphics to image
        //
        this.previewImage = new BufferedImage(this.imageSize.width, 
                                              this.imageSize.height, 
                                              BufferedImage.TYPE_INT_ARGB);
        this.g2d = this.previewImage.createGraphics();
        //this.g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        this.g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void setWorldToScreenTransform(Envelope _mbr,
                                           Rectangle2D     _screenSize) 
    {
        double scaleX = _screenSize.getWidth()  / (_mbr.maxX - _mbr.minX);
        double scaleY = _screenSize.getHeight() / (_mbr.maxY - _mbr.minY);
        double tx = - (_mbr.minX * scaleX);
        double ty =   (_mbr.minY * scaleY) + _screenSize.getHeight();
        this.affineTransform = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
    }
    
    public void componentMoved(ComponentEvent e) {}

    public void componentShown(ComponentEvent e) {}

    public void componentHidden(ComponentEvent e) {}

    @Override
    public void componentResized(ComponentEvent e) {
        int w = this.getWidth();
        int h = this.getHeight();
        double ratio = (double)w/(double)h;
//LOGGER.info("componentResized: (w,h)(" + w + "," + h + ") ResizedRatio="+ratio);
//LOGGER.info("this.imageSize (before aspect ratio applied)="+this.imageSize.toString() + " geometryMBRAspectRatio=" + this.geometryMBRAspectRatio);
        if (ratio != this.geometryMBRAspectRatio) {
            this.imageSize.setSize(this.widthBigger==true  ? w : h * this.geometryMBRAspectRatio,
                                   this.widthBigger==false ? h : w / this.geometryMBRAspectRatio);
        } else {
            this.imageSize.setSize((int)(this.getWidth()),(int)(this.getHeight()));
        }
//LOGGER.info("this.imageSize (after aspect ratio applied)="+this.imageSize.toString() + " Ratio from ImageSize=" + (this.imageSize.getWidth()/this.imageSize.getHeight()));
        this.setSize       (new Dimension((int)this.imageSize.getWidth(),(int)this.imageSize.getHeight()));
        this.setBounds(this.getLocation().x,this.getLocation().y,this.getWidth(),this.getHeight());
        this.parentFrame.pack();
        this.repaint(); // revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) 
    {
        super.paintComponent(g);        
        this.previewImage = new BufferedImage(this.imageSize.width, this.imageSize.height, BufferedImage.TYPE_INT_ARGB);
        // this.previewImage.getScaledInstance((int)this.imageSize.getWidth(), (int)this.imageSize.getHeight(), BufferedImage.SCALE_SMOOTH);
        this.g2d = this.previewImage.createGraphics();
        //this.g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        this.g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintImage();
        g.drawImage(this.previewImage,0,0,(int)this.imageSize.getWidth(),(int)this.imageSize.getHeight(),null);
        this.setPreferredSize(this.imageSize);
    }

    private void paintImage()
    {
        this.horizontalLabels  = mainPrefs.isPreviewImageHorizontalLabels();
        this.vertexLabeling    = mainPrefs.isPreviewImageVertexLabeling();
        this.minMbrAspectRatio = mainPrefs.getMinMbrAspectRatio();
        this.vertexMark        = PointMarker.getMarkerType(mainPrefs.getPreviewVertexMark());
        this.vertexMarkSize    = mainPrefs.getPreviewVertexMarkSize();
        this.pointMark         = PointMarker.MARKER_TYPES.STAR;

        // Geometry is drawn in viewport defined as graph area from origin to edge of geometry
        //
        //int totalPadding = (2 * gridMargin) + labelPadding;
        int totalPadding = gridMargin + labelPadding;
        
        // int xOrigin = gridMargin + labelPadding;
        int xOrigin = labelPadding; // From left -> grid
        int yOrigin = gridMargin; // From top -> down

        // Record viewport part of panel
        //
        Rectangle2D viewport = new Rectangle(this.getWidth()  - totalPadding,  // (2 * gridMargin) - labelPadding, 
                                             this.getHeight() - totalPadding); // (2 * gridMargin) - labelPadding);
        //LOGGER.info("viewport("+viewport.toString());

        // paint grid lines and background
        //
        this.g2d.setColor(Color.WHITE); 
        this.g2d.fillRect(xOrigin, yOrigin, (int)viewport.getWidth(), (int)viewport.getHeight());
        
        //this.g2d.setColor(Color.CYAN);
        //this.g2d.drawRect(xOrigin, yOrigin,this.getWidth() - totalPadding, this.getHeight() - totalPadding); 

        double heightPerYDivision = this.mbr.getHeight()/(double)this.yDivisions; 
        double widthPerXDivision  = this.mbr.getWidth() /(double)this.xDivisions;
        //LOGGER.info(" widthPerXDivision=" + widthPerXDivision  + " xDivisions="+xDivisions);
        //LOGGER.info("heightPerYDivision=" + heightPerYDivision + " yDivisions="+yDivisions);

        double unitsPerYDivision  = viewport.getHeight()/(double)this.yDivisions; 
        double unitsPerXDivision  = viewport.getWidth() /(double)this.xDivisions;
        
        // ----------------------------------------------------------------------
        // Draw Grid Y lines (ie horizontal)
        //
        for (int yDivision = 0; yDivision <= yDivisions; yDivision++) {
            // From Bottom
            int y0 = (yOrigin + (int)viewport.getHeight()) - 
                     (int)(yDivision * unitsPerYDivision);
            // draw internal grid line for constant y
            this.g2d.setColor(gridColor);
            this.g2d.setStroke(DASHED_GRAPH);
            this.g2d.drawLine(xOrigin,                            y0, 
                              xOrigin + (int)viewport.getWidth(), y0);            
            // draw tick mark
            this.g2d.setColor(Color.BLACK);
            this.g2d.setStroke(SOLID_GRAPH);
            this.g2d.drawLine(xOrigin,            y0, 
                              xOrigin+tickLength, y0);
        }
        
        // ----------------------------------------------------------------------
        // Draw X Grid Lines (ie vertical)
        //
        for (int xDivision = 0; xDivision <= xDivisions; xDivision++) {
            int x0 = xOrigin + (int)(xDivision * unitsPerXDivision);
            // draw internal grid line (bottom to top) for constant x
            this.g2d.setColor(gridColor);
            this.g2d.setStroke(DASHED_GRAPH);
            this.g2d.drawLine(x0, gridMargin,
                              x0, (int)viewport.getHeight() + gridMargin);
            // draw tick mark
            this.g2d.setColor(Color.BLACK);
            this.g2d.setStroke(SOLID_GRAPH);
            this.g2d.drawLine(x0, yOrigin + (int)viewport.getHeight(), 
                              x0, yOrigin + (int)viewport.getHeight() - tickLength);
        }
  
        // draw main horizontal x axis
        this.g2d.drawLine(xOrigin, 
                          yOrigin + (this.getHeight() - totalPadding), 
                          xOrigin + (this.getWidth()  - totalPadding), 
                          yOrigin + (this.getHeight() - totalPadding));
        
        // draw main vertical y axis
        this.g2d.drawLine(xOrigin, yOrigin, 
                          xOrigin, yOrigin + (this.getHeight() - totalPadding));

        // ************************************************
        // Draw X and Y Axis labels
        // ************************************************
        
        // Get metrics for a single maximum digit string 
        //
        FontMetrics metrics = this.g2d.getFontMetrics(this.gridLabelFont);
        int sh = metrics.getHeight();  // stringHeight is standardised for a particular font
        // 3 char margin left of Y axis
        int charMargin  = metrics.stringWidth("000");
        // LOGGER.info("charMargin=" + charMargin);
        
        AffineTransform af = this.g2d.getTransform();  // Save current transform

        // **********************************************
        // Draw mbr UR ordinates and Width/hEIGHT
        // **********************************************
        //
        this.g2d.setFont(this.gridLabelFont);
        this.g2d.setColor(this.mbrLabelColor);
        String URx, URy, mbrWidth, mbrHeight;
        if (this.isGeodetic) {
            URx = String.valueOf(COGO.DD2DMS(this.mbr.getMaxX(),0));
            URy = String.valueOf(COGO.DD2DMS(this.mbr.getMaxY(),0));
            mbrWidth  = String.valueOf(COGO.DD2DMS(this.mbr.getMaxX()-this.mbr.getMinX(),1));
            mbrHeight = String.valueOf(COGO.DD2DMS(this.mbr.getMaxY()-this.mbr.getMinY(),1));
        } else {
            URx = String.format("%,d",(int)this.mbr.getMaxX());
            URy = String.format("%,d",(int)this.mbr.getMaxY());
            mbrWidth  = String.format("(%,.1f)",(this.mbr.getMaxX()-this.mbr.getMinX()));
            mbrHeight = String.format("(%,.1f)",(this.mbr.getMaxY()-this.mbr.getMinY()));
        }
        this.g2d.drawString(URx, 
                            xOrigin+(int)(viewport.getWidth()-metrics.stringWidth(URx)), 
                            this.gridMargin/2);
        this.g2d.setFont(this.mbrWidthHeightFont);
        this.g2d.drawString(mbrWidth, 
                            xOrigin+(int)(viewport.getWidth()/2.0)-(metrics.stringWidth(URx)/2), 
                            this.gridMargin-(this.gridMargin/3)+(sh/2));
        // Y Always at 90 degrees
        AffineTransform urat = new AffineTransform();
        urat.setToRotation(Math.toRadians(90.0),
                           this.getWidth()-(this.gridMargin/2)-(sh/2), 
                           this.gridMargin);
        g2d.setTransform(urat);
        this.g2d.setFont(this.gridLabelFont);
        g2d.drawString(URy,
                       this.getWidth()-(this.gridMargin/2)-(sh/2), 
                       this.gridMargin);
        urat.setToRotation(Math.toRadians(90.0),
                           (int)xOrigin+(int)viewport.getWidth()+(this.gridMargin/3)-(sh/2), 
                           this.gridMargin+(int)(viewport.getHeight()/2));
        g2d.setTransform(urat);
        this.g2d.setFont(this.mbrWidthHeightFont);
        g2d.drawString(mbrHeight,
                       (int)xOrigin+(int)viewport.getWidth()+(this.gridMargin/2)-(sh/2), 
                       this.gridMargin+(int)(viewport.getHeight()/2));
        this.g2d.setTransform(af);  // Set back
        this.g2d.setFont(this.gridLabelFont);
        this.g2d.setColor(this.gridLabelColor);

        // ************************************************
        // Y Labels
        // ************************************************
        //
        String yLabel = "";
        double yValue = 0;
        for (int yDivision = 0; yDivision <= yDivisions; yDivision++) {
            int y0 = (yOrigin + (int)viewport.getHeight()) - 
                     (int)(yDivision * unitsPerYDivision);
            // LOGGER.info("(xOrigin,y0)("+xOrigin+","+y0+")");            
            if (this.isGeodetic) {
                yLabel = String.valueOf(COGO.DD2DMS(this.mbr.getMinY()+(((double)yDivision)*heightPerYDivision),0));
            } else {
                if (yDivision==0) {
                    yLabel = String.format("%,d",(int)this.mbr.getMinY());
                } else {
                    yValue = (((double)yDivision)*heightPerYDivision);
                    // LOGGER.info("yValue="+yValue);
                    if (Math.round(yValue) <= 1.0f ) {
                        yLabel = String.format("%,.1f",yValue);
                    } else {
                        yLabel = String.format("%,d",(int)yValue);
                    }
                }
            }
            //markPoint(new Point.Double(xOrigin,y0), Color.GREEN, 6);
            // Get new label's font box 
            //
            int sw = metrics.stringWidth(yLabel);
            // LOGGER.info("yLabel:" +yLabel+" sw="+sw + " sh=" + sh);

            // Get label origin (for right aligned label)
            //
            int labelX = labelPadding - charMargin - sw, 
                labelY = y0-sh;

            // Label Box: this.g2d.drawRect(labelX, labelY, sw, sh);  // show text box
            
            // Draw label horizontally or at angle
            //
            this.g2d.setColor(yDivision==0?this.mbrLabelColor : this.gridLabelColor);
            if (yDivision==0 || horizontalLabels) {
                //LOGGER.info("yLabel="+yLabel+" (xOrigin,y0)(" + xOrigin + "," + y0 +") metrics.stringWidth="+sw+" metrics.getHeight()="+sh);
                this.g2d.drawString(yLabel, 
                                    labelX, 
                                    y0);
            } else {
                AffineTransform at = new AffineTransform();
                //markPoint(new Point.Double(labelX, labelY), Color.RED, 6);
                // Compute optimal rotation angle for label's metric
                double angRad = Math.abs(Math.atan(((float)sh)/((float)sw)));
                //LOGGER.info("at.rotate(ang,x,y)("+String.format("%3.2f",Math.toDegrees(angRad)) + "," + labelX + "," + labelY + ")");
                 at.setToRotation(angRad,labelX,labelY);
                g2d.setTransform(at);
                g2d.drawString(yLabel,labelX,labelY);
               this.g2d.setTransform(af);  // Set back
            }            
        }

        /*
        SVSpatialLayerDraw.drawString(this.g2d,yLabel,x0,y0,SVSpatialLayerDraw.ALIGN_TOP,SVSpatialLayerDraw.ALIGN_RIGHT,Constants.ROTATE.LABEL,Math.toRadians(45),this.attributes,0);
        */

        // ************************************************
        // X labels
        // ************************************************
        //
        String xLabel = "";
        sh = metrics.getHeight();
        double xValue = 0.0f;
        int y0 = (yOrigin + (int)viewport.getHeight()) + sh;
        for (int xDivision = 0; xDivision <= xDivisions; xDivision++) {
            // Compute label point on x axis
            int x0 = xOrigin  + (int)(xDivision * unitsPerXDivision);
            // draw label text
            if (this.isGeodetic) {
                xLabel = COGO.DD2DMS(this.mbr.getMinX()+(((double)xDivision)*widthPerXDivision),0);
            } else {
                if (xDivision==0) {
                    xLabel = String.format("%,d",(int)this.mbr.getMinX());
                } else {
                    xValue = (((double)xDivision)*widthPerXDivision);
                    // LOGGER.info("xValue="+xValue);
                    if (Math.round(xValue) <= 1.0f ) {
                        xLabel = String.format("%,.1f",xValue);
                    } else {
                        xLabel = String.format("%,d",(int)xValue);
                    }
                }
            }
            // Get font box around label
            int sw = metrics.stringWidth(xLabel);
            //LOGGER.info("sw)("+sw+")");

            // Compute label point (same as x0,y0
            int labelX = x0 - (sw/2),
                labelY = y0;
            //markPoint(new Point.Double(labelX, labelY), Color.RED, 6);

            // Label Box: this.g2d.drawRect(labelX, labelY, sw, sh);  // show text box

            if (horizontalLabels && xDivision!=0) {
                //LOGGER.info("drawString(yLabel,x,y)=("+xLabel+"," + labelX + "," + (y0+sh-3)+")");
                this.g2d.drawString(xLabel, labelX, labelY + sh);
            } else {
                AffineTransform at = new AffineTransform();
                // Compute rotation angle 
                double angRad = Math.abs(Math.toRadians(xDivision==0?90:45)); //(float)sh)/((float)sw)));
                //LOGGER.info("at.rotate(ang,x,y)("+String.format("%3.2f",Math.toDegrees(angRad)) + "," + labelX + "," + labelY + ")");
                at.setToRotation(angRad,xDivision==0?x0:labelX,labelY);
                g2d.setTransform(at);
                this.g2d.setColor(xDivision==0?this.mbrLabelColor : this.gridLabelColor);
                g2d.drawString(xLabel,xDivision==0?x0:labelX,labelY);
               this.g2d.setTransform(af);  // Set back
            }
        }

        // ********************************************************
        // Create World to Image transformation for drawing geometries
        //
        setWorldToScreenTransform(this.mbr,viewport);
        if ( this.affineTransform == null ) {
            LOGGER.error("Failed to create Affine Transformation for geometry"); 
            return;
        }
        
        // Draw geometry object linework first if has it
        //
        Stroke oldStroke = this.g2d.getStroke();
        this.g2d.setStroke(SOLID_GRAPH);

        // Process all Java shapes within the geoSet
        //
        int   gtype = -1;
        Shape shp = null;
        Iterator<JGeometry> iter = geoSet.iterator();
        while (iter.hasNext()) 
        {
            JGeometry jGeom = iter.next();
            gtype = jGeom.getType();
            if ( gtype != JGeometry.GTYPE_POINT ) {
                try {
                    shp = jGeom.createShape(this.affineTransform,false/*Simplify as is Thumnail*/);
                    shp = AffineTransform.getTranslateInstance(xOrigin, yOrigin).createTransformedShape(shp);
                } catch (ArrayIndexOutOfBoundsException aibe) {
                    LOGGER.error("SpatialRenderer - ArrayIndexOutOfBounds Exception (" + aibe.getMessage() + 
                                 ") for Geometry (" + gtype + ") " +
                                 (jGeom.hasCircularArcs() ? "with" : "without") + " CircularArcs." );
                    shp = null;
                } catch (Exception e) {
                    LOGGER.error("SpatialRenderer - Error transforming geometry to mappable shape.\n" + e.getMessage());
                    shp = null;
                }
                if (shp == null) {
                    LOGGER.error("SpatialRenderer - No mappable shape exists for geometry.");
                    continue;
                }
            }
            
            // Draw/fill line/poly shapes
            //
            switch (gtype) {
            case JGeometry.GTYPE_POLYGON      :
            case JGeometry.GTYPE_MULTIPOLYGON :
                this.g2d.setColor(this.fillColor);
                this.g2d.fill(shp);
                this.g2d.setColor(this.lineColor);
                this.g2d.draw(shp);
                break;
            
            case JGeometry.GTYPE_CURVE        :
            case JGeometry.GTYPE_MULTICURVE   :
                this.g2d.setColor(this.lineColor);
                this.g2d.draw(shp);
                break;
            }

            // Draw any sdo_points whether single or on a line/polygon
            //
            Point2D point2D = jGeom.getLabelPoint();
            if (point2D == null ) {
                point2D = new Point.Double(Double.MIN_VALUE,Double.MIN_VALUE);
                double point[] = jGeom.getFirstPoint();
                if ( (point != null) && (point.length >= 2)) {
                    point2D = this.affineTransform.transform(new Point.Double(point[0],point[1]), null);
                }
            } else {
                point2D = this.affineTransform.transform(point2D, null);
            }            
            if ( point2D != null ) {
                this.g2d.setColor(this.vertexColor);
                point2D.setLocation(Math.round(point2D.getX()+xOrigin),
                                    Math.round(point2D.getY()+yOrigin));
                this.g2d.fill(PointMarker.getPointShape(this.pointMark,
                                                        point2D,
                                                        this.pointMarkSize,
                                                        0.0f));
            }

            // Now draw vertices and vertex labels
            this.g2d.setStroke(oldStroke);
            this.g2d.setColor(this.vertexColor);
            switch (gtype) 
            {
                case JGeometry.GTYPE_MULTIPOINT   :
                case JGeometry.GTYPE_POLYGON      :
                case JGeometry.GTYPE_MULTIPOLYGON :
                case JGeometry.GTYPE_CURVE        :
                case JGeometry.GTYPE_MULTICURVE   :
                    // transformed Java Shape2D exists
                    // So use it to draw points.
                    //
                    markPoints(shp.getPathIterator(null),
                               this.vertexColor,
                               this.vertexMarkSize,
                               this.vertexMark);
                    break;
            } // switch
        } // while         
    }

    @SuppressWarnings("unused")
	private void markPoints(PathIterator             _pit, 
                            Color                    _pointColor,
                            int                      _pointSize,
                            PointMarker.MARKER_TYPES _vertexMark)
    {
        Rectangle prevBox = new Rectangle(Integer.MIN_VALUE,
                                          Integer.MIN_VALUE,
                                          Integer.MIN_VALUE,
                                          Integer.MIN_VALUE);
        double[] coords = new double[6];
        Point2D markPoint = new Point.Double(Double.NEGATIVE_INFINITY,
                                             Double.NEGATIVE_INFINITY);
        Point2D prevPoint = new Point.Double(Double.NEGATIVE_INFINITY,
                                             Double.NEGATIVE_INFINITY);
        int pointId = 0;
        String s;
        while(!_pit.isDone())
        {
            pointId++;
            int type = _pit.currentSegment(coords);
            switch(type)
            {
                case PathIterator.SEG_MOVETO:
                    markPoint.setLocation(Math.round(coords[0]),Math.round(coords[1]));
                    s = "SEG_MOVETO" + "\tx = " + markPoint.getX() + "\ty = " + markPoint.getY();
                    break;
                case PathIterator.SEG_LINETO:
                    markPoint.setLocation(Math.round(coords[0]),Math.round(coords[1]));
                    s = "SEG_LINETO" + "\tx = " + markPoint.getX() + "\ty = " + markPoint.getY();
                    break;
                case PathIterator.SEG_CLOSE:
                    s = "SEG_CLOSE";
                    break;
                default:
                    s = "unexpected segment type: " + type;
            }
            // Don't overlap labels/symbols
            //LOGGER.info("markPoint(" + markPoint.toString() + ").equals(prevPoint(" + prevPoint.toString() + "))=" + markPoint.equals(prevPoint));
            if ( markPoint.equals(prevPoint)==false ) {
                prevBox = new Rectangle(markPoint(pointId,
                                                  markPoint, 
                                                  _pointColor, 
                                                  _pointSize,
                                                  prevBox,
                                                  _vertexMark)
                                              );
                prevPoint.setLocation(markPoint);
            }
            _pit.next();
        }
    }
    
    private Rectangle markPoint(int                      _pointId,
                                Point2D                  _point2D, 
                                Color                    _color,
                                int                      _pointSize,
                                Rectangle                _prevBox,
                                PointMarker.MARKER_TYPES _vertexMark)
    {
        Rectangle renderBox = new Rectangle((int)_point2D.getX(),
                                            (int)_point2D.getY(),
                                            Integer.MIN_VALUE,
                                            Integer.MIN_VALUE);
        Color orig = this.g2d.getColor();
        this.g2d.setPaint(_color);
        if ( ! _vertexMark.equals(PointMarker.MARKER_TYPES.NONE)) {
            this.g2d.fill(PointMarker.getPointShape(_vertexMark,
                                                    _point2D, 
                                                    _pointSize /*Point Size*/,
                                                    0.0f       /* Rotation*/));
        }
        if ( this.vertexLabeling ) {
            this.g2d.setColor(Color.BLACK);
            this.g2d.setFont(this.vertexFont);
            FontMetrics metrics = this.g2d.getFontMetrics(this.vertexFont);
            String label = "";
            label = String.valueOf(_pointId);
            int stringWidth = metrics.stringWidth(label);
            renderBox = new Rectangle((int)_point2D.getX()-(stringWidth/2),
                                      (int)_point2D.getY(),
                                      stringWidth, 
                                      metrics.getHeight());
            //LOGGER.info("_prevBox: "+_prevBox.toString() + ".intersects(renderBox("+renderBox.toString() + "))=" + renderBox.intersects(_prevBox));
            // Only label the vertex if the label's bounds do not overlap current previous boundary.
            if ( ! renderBox.intersects(_prevBox) ) {
                //LOGGER.info("DrawString("+label+")");
                this.g2d.drawString(label,
                                    (int)(_point2D.getX() - (stringWidth/2)), 
                                    (int)(_point2D.getY() + (metrics.getHeight()/2)));
            } else {
                // Keep carrying through the previous label's box.
                renderBox.setBounds(_prevBox);
            }
        }
        this.g2d.setColor(orig);
        return new Rectangle(renderBox);
    }

    protected void setLabelAttributes(String _fontFamily,
                                      int    _fontSize,
                                      int    _fontStyle,
                                      Color  _foreground,
                                      Color  _background) 
    {
        SimpleAttributeSet saa = new SimpleAttributeSet();
        StyleConstants.setFontFamily(saa, Strings.isEmpty(_fontFamily)?"Serif":_fontFamily);
        StyleConstants.setFontSize(saa, _fontSize);
        switch (_fontStyle ) {
            case Font.BOLD   : StyleConstants.setBold  (saa, true); break;
            case Font.ITALIC : StyleConstants.setItalic(saa, true); break;
            case Font.PLAIN  : StyleConstants.setBold  (saa, false); 
                               StyleConstants.setItalic(saa, false);break;
        }
        StyleConstants.setForeground(saa, 
                                     _foreground==null 
                                     ? Color.BLACK 
                                     : new Color(_foreground.getRed(),
                                                 _foreground.getGreen(),
                                                 _foreground.getBlue(),
                                                 _foreground.getAlpha()));
        StyleConstants.setBackground(saa, 
                                     (_background==null) 
                                     ? new Color(255,255,255,0) 
                                     : new Color(_background.getRed(),
                                                 _background.getGreen(),
                                                 _background.getBlue(),
                                                 _background.getAlpha()));
        this.attributes = new SimpleAttributeSet(saa);
    }

    protected MutableAttributeSet getLabelAttributes() {
        return this.attributes;
    }

}
