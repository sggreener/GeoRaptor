package org.GeoRaptor.SpatialView.SupportClasses;

import java.awt.BasicStroke;

import java.awt.Component;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

public class LineStyle {

    public static enum LINE_STROKES { LINE_DASH, LINE_DASHDOT, LINE_DASHDOTDOT, LINE_DOT, LINE_SOLID, LINE_SOLID_ROUNDED };

    protected static LineStyle classInstance;
    
    public LineStyle() {
        super();
    }

    /**
     * Get instance of PointMarker class
     */
    public static LineStyle getInstance() {
        if (LineStyle.classInstance == null) {
            LineStyle.classInstance = new LineStyle();
        }
        return LineStyle.classInstance;
    }
    
    public static LineStyle.CreateLineStyleRenderer getLineStyleRenderer() {
      return getInstance().new CreateLineStyleRenderer();
    }
    
    /**
     * @method getStroke
     * @see net.refractions.udig.ui.graphics.ViewportGraphics#setStroke(int, int)
     * @author Simon Greener, 16th November 2010, Copied from uDig and re-written
     */
    public static BasicStroke getStroke( LINE_STROKES style, int width ) 
    {
        switch( style ) {
          case LINE_DASH: 
              return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, 
                                     new float[]{width * 2.0f, width * 2.0f}, 0.0f);
          case LINE_DOT: 
              return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, 
                                     new float[]{width * 1.0f, width * 1.5f}, 0.0f);
          case LINE_DASHDOT: 
              return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, 
                                     new float[]{width * 2.0f, width * 2.0f, width * 1.0f, width * 1.5f},0.0f);
          case LINE_DASHDOTDOT: 
              return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, 
                                     new float[]{width * 2.0f, width * 2.0f, 
                                                 width * 1.0f, width * 1.5f,
                                                 width * 1.0f, width * 1.5f}, 0.0f);
          case LINE_SOLID_ROUNDED:
              return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
          case LINE_SOLID:
          default:
              return new BasicStroke(width);
        }
    }

    public static String getLineStyleAsString(LINE_STROKES _lineStyle) {
      return _lineStyle.toString().replace("LINE_","").replace("_"," ");  
    }
  
    public static LINE_STROKES getLineStyle(String _lineStyle) {
        if ( _lineStyle.startsWith("LINE_") )
            if ( _lineStyle.contains(" ") )
                return LINE_STROKES.valueOf(_lineStyle.replace(" ","_"));
            else
                return LINE_STROKES.valueOf(_lineStyle);
        else 
            return LINE_STROKES.valueOf("LINE_" + _lineStyle.replace(" ","_"));
    }
  
    public static ComboBoxModel<String> getComboBoxModel() {
        return new javax.swing.DefaultComboBoxModel<String>(
                      new String[] { 
                          Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_SOLID)),
                          Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_DASH)),
                          Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_DOT)),
                          Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_DASHDOT)),
                          Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_DASHDOTDOT)),
                          Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_SOLID_ROUNDED)) } );
    }
    
    class CreateLineStyleRenderer extends DefaultListCellRenderer
    {
    
	  private static final long serialVersionUID = -3898370798999774255L;

      public CreateLineStyleRenderer() {
      }
      
      @Override
      public Component getListCellRendererComponent(JList<?> list, 
                                                    Object value, 
                                                    int index,
                                                    boolean isSelected, 
                                                    boolean cellHasFocus)
      {
          int iconWidth = 40;
          int iconHeight = 20;
          // Get icon's string for the list item value
          String s = (String)value;
          JLabel label = new JLabel(s,SwingConstants.LEFT);
          if ( s.equals(Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_SOLID))) ) {
              label.setIcon(Tools.createIcon(LineStyle.getStroke( LineStyle.LINE_STROKES.LINE_SOLID, 2), iconWidth, iconHeight));
          } else if ( s.equals(Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_DASH))) ) {
              label.setIcon(Tools.createIcon(LineStyle.getStroke( LineStyle.LINE_STROKES.LINE_DASH, 2), iconWidth, iconHeight));
          } else if ( s.equals(Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_DOT))) ) {
              label.setIcon(Tools.createIcon(LineStyle.getStroke( LineStyle.LINE_STROKES.LINE_DOT, 2), iconWidth, iconHeight));
          } else if ( s.equals(Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_DASHDOT))) ) {
              label.setIcon(Tools.createIcon(LineStyle.getStroke( LineStyle.LINE_STROKES.LINE_DASHDOT, 2), iconWidth, iconHeight));
          } else if (  s.equals(Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_DASHDOTDOT))) ) {
              label.setIcon(Tools.createIcon(LineStyle.getStroke( LineStyle.LINE_STROKES.LINE_DASHDOTDOT, 2), iconWidth, iconHeight));
          } else if ( s.equals(Strings.TitleCase(getLineStyleAsString(LINE_STROKES.LINE_SOLID_ROUNDED))) ) {
              label.setIcon(Tools.createIcon(LineStyle.getStroke( LineStyle.LINE_STROKES.LINE_SOLID_ROUNDED, 2), iconWidth, iconHeight));
          }
          return label;
      }
  }

}
