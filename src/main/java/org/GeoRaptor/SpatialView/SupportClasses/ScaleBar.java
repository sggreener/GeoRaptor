package org.GeoRaptor.SpatialView.SupportClasses;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;

import java.text.AttributedString;
import java.text.DecimalFormat;

import java.util.HashMap;

import org.GeoRaptor.tools.COGO;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;


/**
 * @author ETj
 */
public class ScaleBar {

    public static final float EARTH_RADIUS_KM = 6371;

    class Conversion {
        protected String text = "m";
        protected double unit = 1.0;
        protected String down = "CM";
        protected String up   = "KM";
        protected double inches = 0.0393700787402;
        Conversion(String _text,
                   double _baseUnits,
                   String _down,
                   String _up,
                   double _inches) {
          this.text   = _text;
          this.unit   = _baseUnits;
          this.down   = _down;
          this.up     = _up;
          this.inches = _inches;
        }
    }    
    private HashMap<String,Conversion> conversions = new HashMap<String,Conversion>();
    
    // get pixels per inch
    private int ppi                          = 96;
    private int                   imageWidth = 106;
    private int                  imageHeight =  30;
    private int                scaleBarInset =   3;
    private int                scaleBarWidth = 100;
    private int               scaleBarHeight =  10; 
    private float             scaleBarTransp = 0.80f; 
    private Color             scaleBackColor = null;
    private Font                   scaleFont = null;
    private BasicStroke          scaleStroke = null;
    
    public ScaleBar (Color _background) {
      conversions.put("KM",    new Conversion("km",1000,  "M",null,39370.0787));
      conversions.put("M",     new Conversion("m",    1, "CM","KM",39.3700787));
      conversions.put("METRE", new Conversion("m",    1, "CM","KM",39.3700787));
      conversions.put("METER", new Conversion("m",    1, "CM","KM",39.3700787));
      conversions.put("CM",    new Conversion("cm", 0.01, "MM", "M",0.393700787));
      conversions.put("MM",    new Conversion("mm",0.001, null,"CM",0.0393700787));
      
      conversions.put("MILE",new Conversion("Mile",5280,"YARD",null,63360.0));
      conversions.put("YARD",new Conversion("Yard",   3,"FOOT","MILE",36.0));
      conversions.put("FT",  new Conversion("Ft",     1,"INCH","YARD",12.0));
      conversions.put("FOOT",new Conversion("Ft",     1,"INCH","YARD",12.0));
      conversions.put("INCH",new Conversion("In",  1/12,  null,"FT",   1.0));
      
      this.setBackground(_background);
      this.scaleFont         = new Font("Times New Roman", Font.BOLD + Font.ITALIC, 12);
      this.scaleStroke       = new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
      // Set width of scale bar to one inch's worth of pixels
      //
      this.ppi           = Toolkit.getDefaultToolkit().getScreenResolution();
      this.scaleBarWidth = this.ppi;
      this.imageWidth    = this.scaleBarWidth + 6;
    }
    
    public BufferedImage getFixedScaleBar(Envelope _window,
                                          double          _pixelSize,
                                          String          _mapUnits,
                                          int             _viewPrecision,
                                          boolean         _isGeodetic)
    {
        String unitText = "";
        String mUnit = Strings.isEmpty(_mapUnits) ? "" : _mapUnits.toUpperCase();
        int precision = _viewPrecision;
        double distScaleBar = _pixelSize * this.scaleBarWidth;

        if ( _isGeodetic ) {
            try {
                double distVincenty = COGO.distVincenty(_window.getMinX() /*lon1*/,_window.getMinY()                                 /*lat1*/,
                                                        _window.getMinX() /*lon2*/,_window.getMinY()+(this.scaleBarWidth*_pixelSize) /*lat2*/);
                distScaleBar = distVincenty;
            } catch (Exception e) {
            }
            precision = 1;  // Because it is only roughly calculated
            mUnit    = "M";
            unitText = "m";
        }
        // We have a scale distance and its unit
        // Should we change the distance and unit? 
        //
        // 1. Is there a unit higher than current which we should use?
        Conversion c = conversions.get(mUnit);
        if ( c != null ) {
            boolean up = false;
            if ( distScaleBar > c.unit ) {
                up = true;
            }
            unitText = c.text;
            double scalerValue = c.unit;
            String scaledUnit = "XX";
            while ( !Strings.isEmpty(scaledUnit) ) {
                scaledUnit = up ? conversions.get(mUnit).up : conversions.get(mUnit).down;
                if (Strings.isEmpty(scaledUnit) )
                    break;
                else {
                    if ( up && distScaleBar > (conversions.get(scaledUnit).unit / scalerValue) ) {
                        mUnit    = scaledUnit;
                        unitText = conversions.get(mUnit).text;
                        scalerValue = conversions.get(scaledUnit).unit / scalerValue;
                        distScaleBar /= scalerValue;
                        precision = (int)Math.log10(scalerValue);
                    } else if ( ! up ) {
                        scalerValue = conversions.get(scaledUnit).unit;
                        mUnit    = scaledUnit;
                        unitText = conversions.get(mUnit).text; 
                        if ( distScaleBar > scalerValue ) {
                          distScaleBar *= 1.0 / scalerValue;
                          precision = 1;
                          break;
                        } 
                    } else {
                        break;
                    }
                }
            }
        }
        DecimalFormat dFormat = Tools.getDecimalFormatter(precision,true);
        String regex = "[0][0]*$";  // Trailing zeros
        String fText = dFormat.format(distScaleBar);
        if ( fText.matches(regex) )
            fText = fText.replaceAll(regex,"");
        regex = "\\[.,]$";  // If left with 93439438. rather than 3483094.000 get rid of point too
        if ( fText.matches(regex) )
          fText = fText.replaceAll(regex,"");
        fText += mUnit.length()==0?"":" " + unitText;

        AttributedString label = new AttributedString(fText);
        label.addAttribute(TextAttribute.FONT, this.scaleFont);
        label.addAttribute(TextAttribute.FOREGROUND, Color.BLACK);
        label.addAttribute(TextAttribute.BACKGROUND, this.scaleBackColor);
        label.addAttribute(TextAttribute.KERNING,TextAttribute.KERNING_ON);

        BufferedImage img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setColor(this.scaleBackColor);
        g2d.fillRect(0,0,imageWidth,imageHeight);
        
        g2d.setFont(this.scaleFont);
        int textXPosition = scaleBarWidth - (int)g2d.getFontMetrics().getStringBounds(fText, g2d).getWidth();
        g2d.setStroke(this.scaleStroke);
        
        // Now can set foreground colour
        //
        g2d.setColor(Color.BLACK);
        
        // Draw main horizontal line
        //
        g2d.drawLine(scaleBarInset,                 scaleBarInset + (scaleBarHeight/2), 
                     scaleBarInset + scaleBarWidth, scaleBarInset + (scaleBarHeight/2)); // g2d.drawLine(0, 5, 99,5);
        
        // Draw verticals for ends
        //
        g2d.drawLine(scaleBarInset, scaleBarInset, 
                     scaleBarInset, scaleBarInset + scaleBarHeight);                     // g2d.drawLine(0, 0, 0, 10);
        g2d.drawLine(scaleBarInset + scaleBarWidth, scaleBarInset, 
                     scaleBarInset + scaleBarWidth, scaleBarInset+scaleBarHeight);       // g2d.drawLine(99, 0, 99, 10);
        
        // Add in dividers at 1/4 1/2 and 2/4 position
        //
        g2d.drawLine(scaleBarInset + scaleBarWidth/2,   scaleBarInset + scaleBarHeight/2, 
                     scaleBarInset + scaleBarWidth/2,   scaleBarInset + scaleBarHeight);
        g2d.drawLine(scaleBarInset + scaleBarWidth/4,   scaleBarInset + scaleBarHeight/2, 
                     scaleBarInset + scaleBarWidth/4,   scaleBarInset + scaleBarHeight);
        g2d.drawLine(scaleBarInset + scaleBarWidth/4*3, scaleBarInset + scaleBarHeight/2, 
                     scaleBarInset + scaleBarWidth/4*3, scaleBarInset + scaleBarHeight);

        // Draw text elements
        //
        g2d.drawString("0",                 scaleBarInset,                 imageHeight - scaleBarInset );
        g2d.drawString(label.getIterator(), scaleBarInset + textXPosition, imageHeight - scaleBarInset );
        return img; 
    }

    public void setBackground(Color _background) {
        this.scaleBackColor = ( _background == null ) ? Color.WHITE : _background;
        this.scaleBackColor = Colours.setAlpha(this.scaleBackColor,(int)(this.scaleBarTransp*255));
    }
    
    public void setFont(Font scaleFont) {
        this.scaleFont = scaleFont;
    }
  
    public void setStroke(BasicStroke scaleStroke) {
        this.scaleStroke = scaleStroke;
    }
  
    public int GetMapScale(Envelope _window,
                           double          _pixelSize,
                           String          _mapUnits,
                           boolean         _isGeodetic)
    {
        String mUnit = Strings.isEmpty(_mapUnits) ? "" : _mapUnits.toUpperCase();
        double pixelSizeWorld = _pixelSize;
        // Now convert pixel size to world distance and then to inches
        // 
        if ( _isGeodetic ) {
            // 1 metre = 39.3700787 inches
            try {
                double distVincenty = COGO.distVincenty(_window.getMinX() /*lon1*/,_window.getMinY() /*lat1*/,
                                                        _window.getMinX() /*lon2*/,_window.getMinY()+(this.ppi*_pixelSize) /*lat2*/)
                             / this.ppi;
                pixelSizeWorld = distVincenty;
            } catch (Exception e) {
            }
            pixelSizeWorld *= 39.3700787;
        } else {
            // 1" = ppi thus 1" == what real world distance?
            // Problem is ppi is in inches and pixelSize in decimal degrees, meters or feet etc.
            // Need to mUnit to inches conversion
            //
            Conversion c = conversions.get(mUnit);
            if ( c != null ) {
                pixelSizeWorld *= c.inches;
            }
        }
        Double worldInch = this.ppi * pixelSizeWorld;
        return worldInch.intValue();
    } 
}
