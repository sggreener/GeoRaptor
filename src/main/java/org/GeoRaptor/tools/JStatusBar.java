/*
Swing Hacks Tips and Tools for Killer GUIs
By Joshua Marinacci, Chris Adamson
First Edition June 2005  
Series: Hacks
ISBN: 0-596-00907-0
Pages: 542
website: http://www.oreilly.com/catalog/swinghks/
*/
package org.GeoRaptor.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.DateFormat;

import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;


public class JStatusBar extends JPanel {
    
    private static final long serialVersionUID = 1L;

    private static JPanel leftLabelPanel,
                          timeLabelPanel,
                          centerLabelPanel,
                          rightLabelPanel;
    private static JLabel leftLabel,
                          centerLabel,
                          timeLabel,
                          rightLabel;

    public JStatusBar(Dimension _preferredSize) 
    {
        setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        setBorder(BorderFactory.createRaisedBevelBorder());
        setPreferredSize(_preferredSize);
        setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        
        leftLabelPanel = new JPanel(new BorderLayout());
        leftLabelPanel.setBorder(BorderFactory.createEtchedBorder());
        leftLabel = new JLabel();
        leftLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        leftLabelPanel.add(leftLabel); // , BorderLayout.CENTER);
        leftLabelPanel.setOpaque(false);
        
        timeLabelPanel = new JPanel(new BorderLayout());
        timeLabelPanel.setBorder(BorderFactory.createEtchedBorder());
        timeLabel = new TimePanel();
        timeLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        timeLabelPanel.add(timeLabel); // , BorderLayout.CENTER);
        timeLabelPanel.setOpaque(false);

        centerLabelPanel = new JPanel(new BorderLayout());
        centerLabelPanel.setBorder(BorderFactory.createEtchedBorder());
        centerLabel = new JLabel();
        centerLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        centerLabelPanel.add(centerLabel); // , BorderLayout.CENTER);
        centerLabelPanel.setOpaque(false);
        
        rightLabelPanel = new JPanel(new BorderLayout());
        rightLabelPanel.setBorder(BorderFactory.createEtchedBorder());
        rightLabel = new JLabel(); 
        rightLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        rightLabelPanel.add(rightLabel); // , BorderLayout.LINE_START);
        // rightLabelPanel.add(new JLabel(new AngledLinesWindowsCornerIcon()),BorderLayout.LINE_END);
        rightLabelPanel.setOpaque(false);

        add(leftLabelPanel);
        add(centerLabelPanel);
        add(timeLabelPanel);
        add(rightLabelPanel);
    }

    public void setBackgroundColor(Color _background) {
        setBackground(_background); // new Color(236, 233, 216));
    }
    
    public void setLeftLabelText(String _text, Color _color) {
        JStatusBar.leftLabel.setText(_text);
        JStatusBar.leftLabel.setForeground(_color);
    }
  
    public void setLeftLabelTextSize(int _fontSize) {
        Font f = new Font("Dialog", Font.PLAIN, _fontSize);
        JStatusBar.leftLabel.setFont(f);
        JStatusBar.leftLabel.setHorizontalAlignment(SwingConstants.LEFT);
    }

    public void setCenterLabelText(String _text) {
        JStatusBar.centerLabel.setText(_text);
    }

    public void setCenterLabelTextSize(int _fontSize) {
        Font f = new Font("Dialog", Font.PLAIN, _fontSize);
        JStatusBar.centerLabel.setFont(f);
        JStatusBar.centerLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    public void setTimePanelTextSize(int _fontSize) {
        Font f = new Font("Dialog", Font.PLAIN, _fontSize);
        JStatusBar.timeLabel.setFont(f);
        JStatusBar.timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    public void setRightLabelText(String _text) {
        JStatusBar.rightLabel.setText(_text);
    }
    
    public void setRightLabelTextSize(int _fontSize) {
        Font f = new Font("Dialog", Font.PLAIN, _fontSize);
        JStatusBar.rightLabel.setFont(f);
        JStatusBar.rightLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        leftLabelPanel.setMaximumSize(new Dimension((int)(getWidth()*0.5),getHeight()-4));
        centerLabelPanel.setMinimumSize(new Dimension(5,getHeight()-4));
        centerLabelPanel.setMaximumSize(new Dimension((int)(getWidth()*0.3),getHeight()-4));        
        timeLabelPanel.setMaximumSize(new Dimension((int)(getWidth()*0.2),getHeight()-4));
        rightLabelPanel.setMinimumSize(new Dimension(5,getHeight()-4));
        rightLabelPanel.setMaximumSize(new Dimension((int)(getWidth()*0.2),getHeight()-4));
    }
}

class AngledLinesWindowsCornerIcon implements Icon {
    //RGB values discovered using ZoomIn

    private final Color WHITE_LINE_COLOR = new Color(255, 255, 255);
    private final Color GRAY_LINE_COLOR = new Color(172, 168, 153);
    //Dimensions

    private static final int WIDTH = 13;
    private static final int HEIGHT = 13;
    public int getIconHeight() {
        return WIDTH;
    }

    public int getIconWidth() {
        return HEIGHT;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(WHITE_LINE_COLOR);
        g.drawLine(0, 12, 12, 0);
        g.drawLine(5, 12, 12, 5);
        g.drawLine(10, 12, 12, 10);
        g.setColor(GRAY_LINE_COLOR);
        g.drawLine(1, 12, 12, 1);
        g.drawLine(2, 12, 12, 2);
        g.drawLine(3, 12, 12, 3);
        g.drawLine(6, 12, 12, 6);
        g.drawLine(7, 12, 12, 7);
        g.drawLine(8, 12, 12, 8);
        g.drawLine(11, 12, 12, 11);
        g.drawLine(12, 12, 12, 12);
    }

}

/**
 * Time panel. This will show the current time.
 * A timer to update the time is started when the component
 * is added to its parent and stopped when removed from its parent.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
class TimePanel extends JLabel implements ActionListener
{
	private static final long serialVersionUID = 5815691342221086370L;

/** Timer that updates time. */
  private Timer _timer;

  /** Used to format the displayed date. */
  private DateFormat _fmt = DateFormat.getTimeInstance(DateFormat.LONG);
  private Dimension _prefSize;
  private Calendar _calendar = Calendar.getInstance();

  /**
   * Default ctor.
   */
  public TimePanel()
  {
    super("", JLabel.CENTER);
  }

  /**
   * Add component to its parent. Start the timer for auto-update.
   */
  public void addNotify()
  {
    super.addNotify();
    _timer = new Timer(1000, this);
    _timer.start();
  }

  /**
   * Remove component from its parent. Stop the timer.
   */
  public void removeNotify()
  {
    super.removeNotify();
    if (_timer != null)
    {
      _timer.stop();
      _timer = null;
    }
  }

  /**
   * Update component with the current time.
   *
   * @param evt   The current event.
   */
  public void actionPerformed(ActionEvent evt)
  {
    _calendar.setTimeInMillis(System.currentTimeMillis());
    setText(_fmt.format(_calendar.getTime()));
  }

  /**
   * Return the preferred size of this component.
   *
   * @return  the preferred size of this component.
   */
  public Dimension getPreferredSize()
  {
    if(null == _prefSize)
    {
      // This was originaly done every time.
      // and the count of instantiated objects was amazing
      _prefSize = new Dimension();
      _prefSize.height = 20;
      FontMetrics fm = getFontMetrics(getFont());
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 23);
      cal.set(Calendar.MINUTE, 59);
      cal.set(Calendar.SECOND, 59);
      _prefSize.width = fm.stringWidth(_fmt.format(cal.getTime()));
      Border border = getBorder();
      if (border != null)
      {
        Insets ins = border.getBorderInsets(this);
        if (ins != null)
        {
          _prefSize.width += (ins.left + ins.right);
        }
      }
      Insets ins = getInsets();
      if (ins != null)
      {
        _prefSize.width += (ins.left + ins.right) + 20;
      }
    }
    return _prefSize;
  }
}