package org.GeoRaptor.SpatialView.layers;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;

import org.geotools.util.logging.Logger;

public class TextStroke 
  implements Stroke 
{
    @SuppressWarnings("unused")
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.TextStroke");
    private static final float FLATNESS = 1;

    private String             text = null;
    private AttributeSet attributes = null;
    private Font               font = null;
    private boolean          repeat = false;
    private AffineTransform      af = null;
    private int         totalPoints = 0;
    private GeneralPath lineSegment = null;
    private float       glyphHeight = 0.0f;

    public TextStroke( String       _text, 
                       AttributeSet _attributes) {
        this( _text, _attributes, false );
    }

    public TextStroke( String       _text, 
                       AttributeSet _attributes,
                       boolean      _repeat ) 
    {
        this.text = (_text==null ? " " : _text.trim());
        this.setAttributes(_attributes);
        this.font = new Font(StyleConstants.getFontFamily(_attributes),
                                  ((StyleConstants.isBold(_attributes)   ? Font.BOLD : Font.PLAIN) +
                                 (StyleConstants.isItalic(_attributes) ? Font.ITALIC : Font.PLAIN)),
                               StyleConstants.getFontSize(_attributes));
        this.repeat = _repeat;
    }

    public void setAffineTransformation(AffineTransform _af) {
        this.af = _af;
    }
    
    public void setGraphics(Graphics2D _graphics2D) {
        this.af = _graphics2D.getTransform();
    }

    public void setTotalPoints(int _numPoints) {
        this.totalPoints = _numPoints;
    }

    public Shape getLineSegment() {
        return this.lineSegment;
    }
    
    public float getGlyphHeight() {
        return this.glyphHeight;
    }
    
    @Override
    public Shape createStrokedShape(Shape _shape) 
    {
        FontRenderContext frc = new FontRenderContext(/*AffineTransform*/ null, 
                                                        /*isAntiAliased*/ true, 
                                                /*usesFractionalMetrics*/ true) ;
        GeneralPath result = new GeneralPath();
        GlyphVector glyphVector = this.font.createGlyphVector(frc, this.text);
        PathIterator it = new FlatteningPathIterator(_shape.getPathIterator( null ), FLATNESS );
        
        /* Set up parallel glyphVector for use if direction of vector goes right to left turning glphVector text on its head */
        StringBuilder flipString = new StringBuilder(this.text);
        String flipText = flipString.reverse().toString();
        GlyphVector glyphReverseVector = this.font.createGlyphVector(frc, flipText);
        
        int numPoints = 0;
        float points[] = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float thisX = 0, thisY = 0;
        int    type = 0;
        float  next = 0;
        int currentChar = 0;
        
        int numGlyphs = glyphVector.getNumGlyphs();  // glyphFlipVector is same length
        if ( numGlyphs == 0 ) {
            return result;
        }

        this.lineSegment = null;
        this.glyphHeight = 0.0f;
        float nextAdvance = 0;
        while ( currentChar < numGlyphs && !it.isDone() ) {
            type = it.currentSegment( points );
            switch( type ) {
            case PathIterator.SEG_MOVETO:
                moveX = lastX = points[0];
                moveY = lastY = points[1];
                numPoints = 1;
                nextAdvance = glyphVector.getGlyphMetrics( currentChar ).getAdvance() * 0.5f;
                next = nextAdvance;
                break;

            case PathIterator.SEG_CLOSE:
                points[0] = moveX;
                points[1] = moveY;
              // Fall into....

            case PathIterator.SEG_LINETO:
                numPoints++;
                thisX = points[0];
                thisY = points[1];
                float dx = thisX-lastX;
                float dy = thisY-lastY;
                float distance = (float)Math.sqrt( dx*dx + dy*dy );
                if (this.totalPoints==2 || numPoints > 2) /* skip first segment if possible */ {
                    if ( this.lineSegment == null ) {
                        result.moveTo( thisX, thisY );
                    }
                    if ( distance >= next ) {
                        float             r = 1.0f/distance;  // Don't know what r does.
                        float originalAngle = (float)Math.atan2( dy, dx );
                        float         angle = adjustAngle(originalAngle);
                        Shape     glyph = null;
                        Point2D       p = null;
                        Rectangle2D box = null;
                        int w = 0, 
                            h = 0;
                        while ( currentChar < numGlyphs && distance >= next ) {
                            if (originalAngle==angle) {
                                glyph = glyphVector.getGlyphOutline(currentChar);
                                p     = glyphVector.getGlyphPosition(currentChar);
                                box   = glyphVector.getGlyphMetrics(currentChar).getBounds2D();
                            } else {
                                glyph = glyphReverseVector.getGlyphOutline(currentChar);
                                p     = glyphReverseVector.getGlyphPosition(currentChar);
                                box   = glyphReverseVector.getGlyphMetrics(currentChar).getBounds2D();
                            }
                            float px = (float)p.getX();
                            float py = (float)p.getY();
                            float x = lastX + next*dx*r;
                            float y = lastY + next*dy*r;
                            float advance = nextAdvance;
                            nextAdvance = currentChar < numGlyphs-1 
                                          ? (originalAngle==angle
                                             ? glyphVector.getGlyphMetrics(currentChar+1).getAdvance() * 0.5f 
                                             : glyphReverseVector.getGlyphMetrics(currentChar+1).getAdvance() * 0.5f  )
                                          : 0;
                            this.af.setToTranslation(x,y);
                            this.af.rotate( angle );
                            // Get Height of Glyph for use in drawing background colour
                            this.glyphHeight = (float) ((box.getHeight() > this.glyphHeight) ? box.getHeight() : this.glyphHeight);
                            w = (int)(box.getWidth() /2.0f);
                            h = (int)(box.getHeight()/2.0f);
                            this.af.translate( -px-advance+w, -py+h ); // h&w - Straddle line
                            Shape shp = this.af.createTransformedShape( glyph );
                            result.append( shp, false ); 
                            if ( this.lineSegment == null ) {
                                this.lineSegment = new GeneralPath();
                                this.lineSegment.moveTo( x, y );
                                // LOGGER.debug("lineSegment.MoveTo: " + x + "," + y);
                            } else {
                                this.lineSegment.lineTo(x,y);
                                // LOGGER.debug("lineSegment.LineTo: " + x + "," + y);
                            }
                            next += (advance+nextAdvance);
                            currentChar++;
                            if ( repeat ) {
                                currentChar %= numGlyphs;
                            }
                        }
                    }
                }
                next -= distance;
                lastX = thisX;
                lastY = thisY;
                break;
            }
            it.next();
        }
        if (this.lineSegment != null ) {
            this.lineSegment.closePath();
        }
        return result;
    }

    private float adjustAngle(float _angle) {
        float angle = _angle;
        /*
         * 3|4
         * -.-
         * 2|1
         * 4th is angle between -PI/2 and 0 
         * 3rd is between -1 * PI and -1 * PI/2 
         * 1st is between 0 and PI/2 
         * 2nd is between PI/2 and PI 
         **/
        if ( angle <= 0 && angle > (float)(-1.0f * Math.PI / 2.0f) ) /* 4rd Sector */ {
            angle += 0.0f;
        } if ( angle > (float)(-1 * Math.PI) && angle < (float)(-1.0f * Math.PI / 2.0f) ) /* 3rd Sector */ {
            angle += Math.PI; /* 1st Sector */ 
        } if ( 0 < angle && angle <= (float)(Math.PI / 2.0f) ) /* 1st Sector */ {
            angle += 0.0f;
        } if ( (float)(Math.PI / 2.0f) < angle && angle <= Math.PI ) /* 2nd Sector */ {
            angle -= Math.PI;
        }
        // All angles should now be in 1st and 4th sectors
        //LOGGER.debug("OriginalAngle: "+ _angle + " Adjusted: " + angle + " as Degrees: " + COGO.degrees(angle));
        return angle;
    }
    
    public float measurePathLength( Shape shape ) 
    {
        PathIterator it = new FlatteningPathIterator( shape.getPathIterator( null ), FLATNESS );
        float points[] = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float thisX = 0, thisY = 0;
        int type = 0;
        float total = 0;

        while ( !it.isDone() ) {
                type = it.currentSegment( points );
                switch( type ){
                case PathIterator.SEG_MOVETO:
                        moveX = lastX = points[0];
                        moveY = lastY = points[1];
                        break;

                case PathIterator.SEG_CLOSE:
                        points[0] = moveX;
                        points[1] = moveY;
                        // Fall into....

                case PathIterator.SEG_LINETO:
                        thisX = points[0];
                        thisY = points[1];
                        float dx = thisX-lastX;
                        float dy = thisY-lastY;
                        total += (float)Math.sqrt( dx*dx + dy*dy );
                        lastX = thisX;
                        lastY = thisY;
                        break;
                }
                it.next();
        }

        return total;
    }

	public AttributeSet getAttributes() {
		return attributes;
	}

	public void setAttributes(AttributeSet attributes) {
		this.attributes = attributes;
	}

}

