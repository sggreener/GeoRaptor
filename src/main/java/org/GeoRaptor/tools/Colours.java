package org.GeoRaptor.tools;

import java.awt.Color;

import java.util.Random;
import java.util.StringTokenizer;

public class Colours {

    public Colours() {
        super();
    }

    public static Color getRandomColor() {
        Random numGen = new Random();
        return new Color(numGen.nextInt(256), numGen.nextInt(256), numGen.nextInt(256));
    }
    
    public static Color setAlpha(Color _c,
                                 int _a) 
    {
        if (_c == null )
            return null;
        return new Color(_c.getRed(),
                         _c.getGreen(),
                         _c.getBlue(),
                         _a);    
    }
    
    public static String getRGBa(Color _c) {
        return String.format("%d,%d,%d,%d",_c.getRed(),_c.getGreen(),_c.getBlue(),_c.getAlpha());
    }


    public static Color fromRGBa(String _rgba) 
    {
        if (Strings.isEmpty(_rgba) )
            return Color.BLACK;
        if ( _rgba.indexOf(",")==-1 )
            return new Color(Integer.parseInt(_rgba));
        StringTokenizer st = new StringTokenizer(_rgba,",",false);
        String tok = ""; 
        int red = 255, 
            green = 255, 
            blue = 255, 
            alpha = 255;
        int i = 0;
        while ( st.hasMoreTokens() ) {
            i++;
            tok = st.nextToken();
            try {
                switch ( i ) {
                  case 1 : red   = Integer.parseInt(tok); break;
                  case 2 : green = Integer.parseInt(tok); break; 
                  case 3 : blue  = Integer.parseInt(tok); break;
                  case 4 : alpha = Integer.parseInt(tok);  
                } 
            } catch (NumberFormatException e) {
                switch ( i ) {
                  case 1 : red   = 255; break;
                  case 2 : green = 255; break; 
                  case 3 : blue  = 255; break;
                  case 4 : alpha = 255;  
                }               
            }  
          }
      return new Color(red,green,blue,alpha);
    }
    
    /**
     * @function getColorFromHTML
     * @param htmlColor
     * @return
     * @example String s = "#FFFFFF";
     * s = s.substring(1);
     * int hexColor = Integer.parseInt(s, 16);
     * Color rgb = getColorFromHtml(hexColor);
     *
     */
    public static Color getColorFromHtml(int htmlColor) {
        int red = htmlColor>>16;
        int green = (htmlColor>>8)&0x00ff;
        int blue = htmlColor&0x0000ff;
        return(new Color(red, green, blue));
    }

    /**
     * Convert Color object to string in form R,G,B (like 255,128,64)
     * @param _col Color object
     * @return string or empty string if parameter _cor is null
     */
    public static String colorToString(Color _col) {
        if (_col == null)
            return "";
        return String.format("%d,%d,%d",_col.getRed(),_col.getGreen(),_col.getBlue());
    }
    
    /**
     * Convert string in form R,G,B (like 255,128,64) into Color object
     * @param _str
     * @return
     */
    public static Color stringToColor(String _str) {
        if (_str == null)
            return null;

        _str = _str.trim();
        String colStr[] = _str.split(",");
        if (colStr.length != 3) {
            // #FFFFFF ???
            if (_str.length() == 7 && _str.indexOf("#") > -1)
                return new Color(Integer.parseInt(_str.toString()
                                                      .substring(1)
                                                      .toUpperCase(), 16));
            else
                return null;
        }

        try {
            int red = Integer.parseInt(colStr[0]);
            int green = Integer.parseInt(colStr[1]);
            int blue = Integer.parseInt(colStr[2]);
            if ((red < 0) || (red > 255) || (green < 0) || (green > 255) || (blue < 0) || (blue > 255))
                return null;
            return new Color(red, green, blue);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /* For text being readable, it must have a good contrast difference. Why?
     * Your eye has receptors for brightness and receptors for each of the colors
     * red, green and blue. However, it has much more receptors for brightness
     * than for color. If you only change the color, but both colors have the
     * same contrast, your eye must distinguish fore- and background by the
     * color only and this stresses the brain a lot over the time, because it
     * can only use the very small amount of signals it gets from the color
     * receptors, since the breightness receptors won't note a difference.
     * Actually contrast is so much more important than color that you don't
     * have to change the color at all. E.g. light red on dark red reads nicely
     * even though both are the same color, red.
     * From: http://stackoverflow.com/questions/301869/how-to-find-good-looking-font-color-if-background-color-is-known
     */

    public static String rgb2hex(Color _color) {
        return rgb2hex(_color.getRed(), _color.getGreen(), _color.getBlue());
    }

    public static String rgb2hex(int r, int g, int b) {
        String rs = Integer.toHexString(r);
        String gs = Integer.toHexString(g);
        String bs = Integer.toHexString(b);
        if (rs.length() == 1)
            rs = "0" + rs;
        if (gs.length() == 1)
            gs = "0" + gs;
        if (bs.length() == 1)
            bs = "0" + bs;
        return (rs + gs + bs);
    }

    private static double contrastDiff(int r1, int g1, int b1, int r2, int g2, int b2) {
        double l1;
        double l2;

        l1 =
            (0.2126 * Math.pow((double) r1 / 255.0, 2.2) + 0.7152 * Math.pow((double) g1 / 255.0, 2.2) +
             0.0722 * Math.pow((double) b1 / 255.0, 2.2) + 0.05);
        l2 =
            (0.2126 * Math.pow((double) r2 / 255.0, 2.2) + 0.7152 * Math.pow((double) g2 / 255.0, 2.2) +
             0.0722 * Math.pow((double) b2 / 255.0, 2.2) + 0.05);

        return (l1 > l2) ? (l1 / l2) : (l2 / l1);
    }

    public static Color highContrast(Color _color) {
        if (_color == null)
            return null;
        final double cutOff = 10.0f;
        Color returnColor = null;
        int bred, bgreen, bblue;

        bred = _color.getRed();
        bgreen = _color.getGreen();
        bblue = _color.getBlue();

        // create default return color
        int[] hsv = rgb2hsv(bred, bgreen, bblue);
        hsv[0] = (hsv[0] + 180) % 360; // h
        hsv[1] = 1 - hsv[1]; // s
        hsv[2] = 1 - hsv[2]; // v
        returnColor = Color.getHSBColor((float) hsv[0], (float) hsv[1], (float) hsv[2]);

        // try 4096 colors
        for (int r = 0; r <= 15; r++) {
            for (int g = 0; g <= 15; g++) {
                for (int b = 0; b <= 15; b++) {
                    int red, blue, green;
                    double cDiff;

                    // brightness increases like this: 00, 11,22, ..., ff
                    red = (r << 4) | r;
                    blue = (b << 4) | b;
                    green = (g << 4) | g;

                    cDiff = contrastDiff(red, green, blue, bred, bgreen, bblue);
                    if (cDiff < cutOff) {
                        continue;
                    } else {
                        return new Color(red, green, blue);
                    }
                }
            }
        }
        return returnColor;
    }

    /**
     * @method rgb2hsv
     * @param r
     * @param g
     * @param b
     * @return int[] hsv values
     * @author From http://www.f4.fhtw-berlin.de/~barthel/ImageJ/ColorInspector//HTMLHelp/farbraumJava.htm
     * @author Simon Greener, 1st December 2010, Modified for use in Tools
     */
    public static int[] rgb2hsv(int r, int g, int b) {
        int min; //Min. value of RGB
        int max; //Max. value of RGB
        int delMax; //Delta RGB value

        if (r > g) {
            min = g;
            max = r;
        } else {
            min = r;
            max = g;
        }
        if (b > max)
            max = b;
        if (b < min)
            min = b;

        delMax = max - min;

        float H = 0, S;
        float V = max;

        if (delMax == 0) {
            H = 0;
            S = 0;
        } else {
            S = delMax / 255f;
            if (r == max)
                H = ((g - b) / (float) delMax) * 60;
            else if (g == max)
                H = (2 + (b - r) / (float) delMax) * 60;
            else if (b == max)
                H = (4 + (r - g) / (float) delMax) * 60;
        }
        return new int[] { (int) (H), (int) (S * 100), (int) (V * 100) };
    }

}
