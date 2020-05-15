package org.GeoRaptor.tools;

/**
 * Code published at:  http://www.java2s.com/Code/Java/Tiny-Application/AdvancedFontChooser.htm
 * Renamed to LabelStyler
 **/

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import static java.awt.font.TextAttribute.FAMILY;
import static java.awt.font.TextAttribute.POSTURE;
import static java.awt.font.TextAttribute.POSTURE_OBLIQUE;
import static java.awt.font.TextAttribute.SIZE;
import static java.awt.font.TextAttribute.STRIKETHROUGH;
import static java.awt.font.TextAttribute.STRIKETHROUGH_ON;
import static java.awt.font.TextAttribute.UNDERLINE;
import static java.awt.font.TextAttribute.UNDERLINE_LOW_ONE_PIXEL;
import static java.awt.font.TextAttribute.WEIGHT;
import static java.awt.font.TextAttribute.WEIGHT_BOLD;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Map;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import oracle.jdeveloper.layout.VerticalFlowLayout;

import org.GeoRaptor.Messages;

import org.geotools.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class LabelStyler extends JDialog {

	private static final long serialVersionUID = 4321686304382369642L;

	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.tools.LabelStyler");

  /** 
   * Properties File Manager
   **/
  private static final String propertiesFile = "org.GeoRaptor.tools.LabelStyler";
  protected PropertiesManager propertyManager;

  protected int Closed_Option = JOptionPane.CLOSED_OPTION;

  protected MutableAttributeSet attributes;

  public static enum LabelAttribute { FontFamily,FontSize,Bold,Italic,Underline,StrikeThrough,Subscript,Superscript,Foreground,Background};

  private static enum COLOUR_ACTIONS { BACKGROUND, FOREGROUND };
  private static COLOUR_ACTIONS actionColour = COLOUR_ACTIONS.FOREGROUND;

  public static String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
  public static String[] fontSizes = new String[] { "6", "7", "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72" };
  private static String  PREVIEW_TEXT = "Preview Font";

  protected InputList    fontNameInputList      = new InputList(fontNames, "Name:");
  protected InputList    fontSizeInputList      = new InputList(fontSizes, "Size:");
  protected JCheckBox    boldCheckBox           = new JCheckBox("Bold");
  protected JCheckBox    italicCheckBox         = new JCheckBox("Italic");
  protected JCheckBox    underlineCheckBox      = new JCheckBox("Underline");
  protected JCheckBox    strikethroughCheckBox  = new JCheckBox("Strikethrough");
  protected JLabel       lblAlpha               = new JLabel("Transparency");
  protected JSlider      sldrLabelAlpha         = new JSlider();
  protected JLabel       lblAlphaClearEnd       = new JLabel();
  protected JLabel       lblAlphaSolidEnd       = new JLabel();
  protected JLabel       previewLabel;
  
  protected JPanel       pnlMain                = new JPanel(new VerticalFlowLayout());

  protected JPanel       pnlForegroundFontColor = new JPanel(new FlowLayout(FlowLayout.CENTER));
  protected TitledBorder bdrForegroundFontColor;
  protected JButton      btnForegroundFontColor = new JButton();
  
  protected JPanel       pnlBackgroundFillColor = new JPanel(new FlowLayout(FlowLayout.CENTER));
  protected TitledBorder bdrBackgroundFillColor;
  protected TitledBorder bdrBackgroundNoFillColor;
  protected JButton      btnBackgroundFillColor = new JButton();

  private   Color        dialogBackground;
  protected Color        cMapBackground         = new Color(255,255,255,0);
  protected int          alphaBackground        = 255; // Opaque
  protected int          alphaForeground        = 255; // Opaque
 
  public LabelStyler(JFrame _owner,
                     Color  _mapBackground) {
    super(_owner, "Font Chooser", false);

    this.propertyManager = new PropertiesManager(LabelStyler.propertiesFile);

    this.setTitle(this.propertyManager.getMsg("TITLE_LABEL_STYLER"));
    this.setResizable(false);

    PREVIEW_TEXT = this.propertyManager.getMsg("PREVIEW_TEXT");
    this.cMapBackground = Colours.setAlpha(_mapBackground,0); // 0 means utterly transparent
    this.alphaBackground = 0;
    this.dialogBackground = getBackground();
    
    JPanel pnlFont = new JPanel(new GridLayout(1, 2, 10, 2));
    pnlFont.setBorder(new TitledBorder(new EtchedBorder(), this.propertyManager.getMsg("LABEL_FONT")));
    fontNameInputList.setText(this.propertyManager.getMsg("LABEL_FONT_NAME_LIST"));
    pnlFont.add(fontNameInputList);
    fontNameInputList.setDisplayedMnemonic('n');
    fontNameInputList.setToolTipText(this.propertyManager.getMsg("TT_FONT_NAME"));

    pnlFont.add(fontSizeInputList);
    fontSizeInputList.setText(this.propertyManager.getMsg("LABEL_FONT_SIZE_LIST"));
    fontSizeInputList.setDisplayedMnemonic('s');
    fontSizeInputList.setToolTipText(this.propertyManager.getMsg("TT_FONT_SIZE"));
    pnlMain.add(pnlFont);

    JPanel pnlEffects = new JPanel(new GridLayout(2, 2, 10, 5));
    pnlEffects.setBorder(new TitledBorder(new EtchedBorder(), this.propertyManager.getMsg("BORDER_EFFECTS")));
    
    boldCheckBox.setText(this.propertyManager.getMsg("LABEL_BOLD"));
    boldCheckBox.setMnemonic('b');
    boldCheckBox.setToolTipText(this.propertyManager.getMsg("TT_FONT_STYLE",this.propertyManager.getMsg("LABEL_BOLD")));
    pnlEffects.add(boldCheckBox);

    italicCheckBox.setText(this.propertyManager.getMsg("LABEL_ITALIC"));
    italicCheckBox.setMnemonic('i');
    italicCheckBox.setToolTipText(this.propertyManager.getMsg("TT_FONT_STYLE",this.propertyManager.getMsg("LABEL_ITALIC")));
    pnlEffects.add(italicCheckBox);

    underlineCheckBox.setText(this.propertyManager.getMsg("LABEL_UNDERLINE"));
    underlineCheckBox.setMnemonic('u');
    underlineCheckBox.setToolTipText(this.propertyManager.getMsg("TT_FONT_STYLE",this.propertyManager.getMsg("LABEL_UNDERLINE")));
    pnlEffects.add(underlineCheckBox);

    strikethroughCheckBox.setText(this.propertyManager.getMsg("LABEL_STRIKETHROUGH"));
    strikethroughCheckBox.setMnemonic('r');
    strikethroughCheckBox.setToolTipText(this.propertyManager.getMsg("LABEL_FONT_STYLE",this.propertyManager.getMsg("LABEL_STRIKETHROUGH")));
    pnlEffects.add(strikethroughCheckBox);
    pnlMain.add(pnlEffects);

    // Fore/Back Colour
    JPanel pnlColour = new JPanel(new BorderLayout());
    pnlColour.setBorder(new TitledBorder(new EtchedBorder(),this.propertyManager.getMsg("BORDER_COLOUR")));
    
    JPanel pnlColorButtons = new JPanel(new BorderLayout());
    bdrForegroundFontColor = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), 
                                                              this.propertyManager.getMsg("LABEL_FORECOLOR"));
    bdrForegroundFontColor.setTitleJustification(TitledBorder.CENTER);
    pnlForegroundFontColor.setBorder(bdrForegroundFontColor);
    btnForegroundFontColor.setPreferredSize(new java.awt.Dimension(120, 90));
    btnForegroundFontColor.setMaximumSize(new java.awt.Dimension(120, 90));    
    btnForegroundFontColor.setFont(new Font("Times New Roman",Font.BOLD,16));
    btnForegroundFontColor.setToolTipText(this.propertyManager.getMsg("TT_FORECOLOR"));
    ToolTipManager.sharedInstance().registerComponent(btnForegroundFontColor);
    btnForegroundFontColor.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
          actionColour = COLOUR_ACTIONS.FOREGROUND;
          btnBackgroundFillColor.setBorder(BorderFactory.createRaisedBevelBorder());
          btnForegroundFontColor.setBorder(BorderFactory.createLoweredBevelBorder());
          setAlwaysOnTop(false);
          Color fontColor = null;
          fontColor = JColorChooser.showDialog(getGlassPane(),
                                                     propertyManager.getMsg("TITLE_COLOR_CHOOSER"),
                                                     btnForegroundFontColor.getBackground());
          setAlwaysOnTop(true);
          // Foreground/Font color is not allowed to be null
          //
          if (fontColor != null) {
              // Save alpha and make slider active
              //
              setSliderAndAlpha(alphaForeground,actionColour,true);
              // Show selected colour
              //
              btnForegroundFontColor.setBackground(fontColor);
              SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                      updatePreview(previewLabel);
                  }
              });
          }
      }
    });
    pnlForegroundFontColor.add(btnForegroundFontColor);
    pnlColorButtons.add(pnlForegroundFontColor,BorderLayout.WEST);
    
    bdrBackgroundFillColor = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), 
                                                              this.propertyManager.getMsg("LABEL_BACKCOLOR"));
    bdrBackgroundFillColor.setTitleJustification(TitledBorder.CENTER);
    bdrBackgroundNoFillColor = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), 
                                                                this.propertyManager.getMsg("LABEL_NO_BACKCOLOR"));
    bdrBackgroundNoFillColor.setTitleJustification(TitledBorder.CENTER);
    
    pnlBackgroundFillColor.setBorder(bdrForegroundFontColor);
    btnBackgroundFillColor.setFont(new Font("Times New Roman",Font.BOLD,16));
    btnBackgroundFillColor.setPreferredSize(new java.awt.Dimension(120, 90));
    btnBackgroundFillColor.setMaximumSize(new java.awt.Dimension(120, 90));    
    btnBackgroundFillColor.setToolTipText(this.propertyManager.getMsg("TT_BACKCOLOR"));
    ToolTipManager.sharedInstance().registerComponent(btnBackgroundFillColor);
    btnBackgroundFillColor.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
          actionColour = COLOUR_ACTIONS.BACKGROUND;
          btnBackgroundFillColor.setBorder(BorderFactory.createLoweredBevelBorder());
          btnForegroundFontColor.setBorder(BorderFactory.createRaisedBevelBorder());
          setAlwaysOnTop(false);
          Color bgColor = null;
          bgColor = JColorChooser.showDialog(getGlassPane(),
                                                     propertyManager.getMsg("TITLE_COLOR_CHOOSER"),
                                                     btnBackgroundFillColor.getBackground());
          setAlwaysOnTop(true);

          // Returned colour for background is allowed to be null
          //
          
          //
          setSliderAndAlpha((bgColor==null
                         ? 0
                         : alphaBackground),
                         actionColour,
                         true /* Set slider to alpha value */);
          
          // Set background to either map's background or user selected colour but with solid alpha
          // Note we always show the colour without alpha in the button but keep the actual alpha separately
          // for import/export and review
          //
          btnBackgroundFillColor.setBackground(bgColor==null
                                               ? Colours.setAlpha(cMapBackground,255)
                                               : bgColor );
          // Now update preview
          //
          final Color backColor = bgColor;
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                  // Possibly change border around button
                  //
                  setBackgroundFillBorder(backColor);
                  updatePreview(previewLabel);
              }
          });
        }
    });
    pnlBackgroundFillColor.add(btnBackgroundFillColor);
    pnlColorButtons.add(pnlBackgroundFillColor,BorderLayout.EAST);
    
    JPanel pnlAlpha = new JPanel(new FlowLayout());
    lblAlpha.setText(this.propertyManager.getMsg("LABEL_TRANSPARENCY"));
    lblAlpha.setLabelFor(sldrLabelAlpha);
    lblAlpha.setMaximumSize(new Dimension(70,21));
    sldrLabelAlpha.setMinimum(0);
    sldrLabelAlpha.setMaximum(255);
    sldrLabelAlpha.setValue(255);  // Opaque
    sldrLabelAlpha.setMajorTickSpacing(25);
    sldrLabelAlpha.setMinorTickSpacing(5);
    sldrLabelAlpha.setPaintTicks(true);
    sldrLabelAlpha.setSnapToTicks(true);
    sldrLabelAlpha.setPreferredSize(new Dimension(160, 28));
    sldrLabelAlpha.setMaximumSize(new Dimension(160, 28));    
    sldrLabelAlpha.setToolTipText(this.propertyManager.getMsg("TT_VALUE_0_1"));
    sldrLabelAlpha.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            setSliderAndAlpha(sldrLabelAlpha.getValue(),actionColour,false);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updatePreview(previewLabel);
                }
            });
        }
    });
    this.lblAlphaSolidEnd.setText(this.propertyManager.getMsg("LABEL_TRANSPARENT"));
    this.lblAlphaClearEnd.setText(this.propertyManager.getMsg("LABEL_OPAQUE"));
    this.lblAlphaSolidEnd.setHorizontalAlignment(SwingConstants.LEFT);
    this.lblAlphaClearEnd.setHorizontalAlignment(SwingConstants.RIGHT);
    
    javax.swing.GroupLayout pnlColorLayout = new javax.swing.GroupLayout(pnlAlpha);
    pnlAlpha.setLayout(pnlColorLayout);
    pnlColorLayout.setHorizontalGroup(
            pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlColorLayout.createSequentialGroup()
              .addContainerGap()
              .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addComponent(lblAlpha))
              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
              .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                  .addGroup(pnlColorLayout.createSequentialGroup()
                      .addComponent(lblAlphaSolidEnd)
                      .addGap(65, 65, 100)
                      .addComponent(lblAlphaClearEnd))
                  .addComponent(sldrLabelAlpha, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
              .addContainerGap(12, Short.MAX_VALUE))
              );
    pnlColorLayout.setVerticalGroup(
        pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(pnlColorLayout.createSequentialGroup()
            .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
            .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(lblAlpha)
                .addComponent(sldrLabelAlpha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(pnlColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lblAlphaSolidEnd)
                .addComponent(lblAlphaClearEnd)))
    );    
    pnlColour.add(pnlColorButtons,BorderLayout.NORTH);
    pnlColour.add(pnlAlpha,BorderLayout.SOUTH);
    pnlMain.add(pnlColour);

    JPanel pnlPreview = new JPanel(new BorderLayout()) {
          private static final long serialVersionUID = 1L;          
          @Override
          protected void paintComponent(Graphics g) {
              super.paintComponent(g);
              g.setColor(dialogBackground); // new Color(237, 237, 237, 255));
              g.fillRect(0, 0, getWidth(), getHeight());
          }
      };
    pnlPreview.setBorder(new TitledBorder(new EtchedBorder(),this.propertyManager.getMsg("BORDER_PREVIEW")));
    pnlPreview.setBackground(Colours.setAlpha(this.cMapBackground,255));
    pnlPreview.setOpaque(false);

    previewLabel = createPreviewLabel();    
    pnlPreview.add(previewLabel, BorderLayout.CENTER);
    pnlMain.add(pnlPreview);

    JPanel pnlButtons = new JPanel(new FlowLayout());
    JPanel p1 = new JPanel(new GridLayout(1, 2, 10, 2));
    JButton btOK = new JButton(this.propertyManager.getMsg("BUTTON_OK"));
    btOK.setToolTipText(this.propertyManager.getMsg("TT_BUTTON_OK"));
    getRootPane().setDefaultButton(btOK);
    ActionListener actionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Closed_Option = JOptionPane.OK_OPTION;
        dispose();
      }
    };
    btOK.addActionListener(actionListener);
    p1.add(btOK);

    JButton btCancel = new JButton(this.propertyManager.getMsg("BUTTON_CANCEL"));
    btCancel.setToolTipText(this.propertyManager.getMsg("TT_BUTTON_CANCEL"));
    actionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Closed_Option = JOptionPane.CANCEL_OPTION;
        dispose();
      }
    };
    btCancel.addActionListener(actionListener);
    p1.add(btCancel);
    pnlButtons.add(p1);
    pnlMain.add(pnlButtons);
    
    getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER,1,1)); //BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    getContentPane().add(pnlMain);
    // getContentPane().setBackground(Color.GRAY);

    pack();

    ListSelectionListener listSelectListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              updatePreview(previewLabel);
            }
        });
      }
    };
    
    fontNameInputList.addListSelectionListener(listSelectListener);
    fontSizeInputList.addListSelectionListener(listSelectListener);

    ActionListener updatePreviewListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              updatePreview(previewLabel);
            }
        });
      }
    };
    boldCheckBox.addActionListener(updatePreviewListener);
    italicCheckBox.addActionListener(updatePreviewListener);
    underlineCheckBox.addActionListener(updatePreviewListener);
    strikethroughCheckBox.addActionListener(updatePreviewListener);
  }

  public void setSliderAndAlpha(final int      _alpha,
                                COLOUR_ACTIONS _actionColour,
                                final boolean  _slider) 
  {
      if ( _actionColour == COLOUR_ACTIONS.BACKGROUND )
          this.alphaBackground = _alpha;
      else
          this.alphaForeground = _alpha;
      
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            lblAlpha.setText(String.format(propertyManager.getMsg("LABEL_TRANSPARENCY") + " %3d",_alpha));
            if (_slider)
              sldrLabelAlpha.setValue(_alpha);
          }
      });      
  }
  
  public float getLabelAlpha() {
      return this.sldrLabelAlpha.getValue(); 
  }
  
  public int getOption() {
    return Closed_Option;
  }

  public void setAttributes(AttributeSet _aSet) {
    attributes = new SimpleAttributeSet(_aSet);
    String name = StyleConstants.getFontFamily(_aSet);
    fontNameInputList.setSelected(name);
    int size = StyleConstants.getFontSize(_aSet);
    fontSizeInputList.setSelectedInt(size);
    boldCheckBox.setSelected(StyleConstants.isBold(_aSet));
    italicCheckBox.setSelected(StyleConstants.isItalic(_aSet));
    underlineCheckBox.setSelected(StyleConstants.isUnderline(_aSet));
    strikethroughCheckBox.setSelected(StyleConstants.isStrikeThrough(_aSet));
    
    Color c = StyleConstants.getBackground(_aSet);
    this.setSliderAndAlpha(c.getAlpha(), COLOUR_ACTIONS.BACKGROUND, true);
    this.btnBackgroundFillColor.setBackground(Colours.setAlpha(c,255));
    this.setBackgroundFillBorder(c);
    this.btnBackgroundFillColor.setBorder(BorderFactory.createRaisedBevelBorder());

    c = StyleConstants.getForeground(_aSet);
    this.setSliderAndAlpha(c.getAlpha(), COLOUR_ACTIONS.FOREGROUND, true);
    this.btnForegroundFontColor.setBackground(Colours.setAlpha(c,255));
    this.btnForegroundFontColor.setBorder(BorderFactory.createLoweredBevelBorder());

    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          updatePreview(previewLabel);
        }
    });
  }

  private void setBackgroundFillBorder(Color _bgColor) {
      if ( _bgColor == null || _bgColor.getAlpha()==0 ) {
          this.pnlBackgroundFillColor.setBorder(this.bdrBackgroundNoFillColor);
      } else { 
          this.pnlBackgroundFillColor.setBorder(this.bdrBackgroundFillColor);
      }
  }

  public AttributeSet getAttributes() {
    if (attributes == null)
      return null;
    StyleConstants.setFontFamily(attributes, fontNameInputList.getSelected());
    StyleConstants.setFontSize(attributes, fontSizeInputList.getSelectedInt());
    StyleConstants.setBold(attributes, boldCheckBox.isSelected());
    StyleConstants.setItalic(attributes, italicCheckBox.isSelected());
    StyleConstants.setUnderline(attributes, underlineCheckBox.isSelected());
    StyleConstants.setStrikeThrough(attributes, strikethroughCheckBox.isSelected());
    StyleConstants.setForeground(attributes, Colours.setAlpha(this.btnForegroundFontColor.getBackground(),this.alphaForeground));
    StyleConstants.setBackground(attributes, Colours.setAlpha(this.btnBackgroundFillColor.getBackground(),this.alphaBackground));
    return attributes;
  }

  public static SimpleAttributeSet getDefaultAttributes(Color _cMapBackground) {
      SimpleAttributeSet saa = new SimpleAttributeSet();
      StyleConstants.setFontFamily(saa, "Serif");
      StyleConstants.setFontSize(saa, 12);
      StyleConstants.setBold(saa, true);
      StyleConstants.setForeground(saa, Color.BLACK);
      if ( _cMapBackground==null )
          StyleConstants.setBackground(saa, new Color(255,255,255,0));  // 0 is utterly transparent
      else {
          StyleConstants.setBackground(saa,
                                       new Color(_cMapBackground.getRed(),
                                                 _cMapBackground.getGreen(),
                                                 _cMapBackground.getBlue(),
                                                 0));
      }
      return new SimpleAttributeSet(saa);
  }

  public static String toXML(SimpleAttributeSet a) {
      if (a==null) {
          return "";
      }
      StringBuffer sb = new StringBuffer();
      sb.append("<LabelAttributes>");
      sb.append("<FontFamily>"    + StyleConstants.getFontFamily(a)          + "</FontFamily>");
      sb.append("<FontSize>"      + StyleConstants.getFontSize(a)            + "</FontSize>");
      sb.append("<Bold>"          + StyleConstants.isBold(a)                 + "</Bold>");
      sb.append("<Italic>"        + StyleConstants.isItalic(a)               + "</Italic>");
      sb.append("<Underline>"     + StyleConstants.isUnderline(a)            + "</Underline>");
      sb.append("<StrikeThrough>" + StyleConstants.isStrikeThrough(a)        + "</StrikeThrough>");
      sb.append("<Subscript>"     + StyleConstants.isSubscript(a)            + "</Subscript>");
      sb.append("<Superscript>"   + StyleConstants.isSuperscript(a)          + "</Superscript>");
      sb.append("<Foreground>"    + Colours.getRGBa(StyleConstants.getForeground(a)) + "</Foreground>");
      sb.append("<Background>"    + Colours.getRGBa(StyleConstants.getBackground(a)) + "</Background>");
      sb.append("</LabelAttributes>");
      return sb.toString();
  }
 
  public static SimpleAttributeSet fromNode(Node _node) 
  {
      if ( _node == null ) {
          System.out.println("XML is null");
          return null;  // Should throw error         
      }
      if (_node.getNodeName().equalsIgnoreCase("LabelAttributes")==false ) {
          System.out.println("XML is of type " + _node.getNodeName() + " when it should be <LabelAttributes>");
          return null;  // Should throw error
      }
      if ( _node.getFirstChild()==null ) {
          System.out.println("LabelAttributes has no child nodes");
          return null;  // Should throw error
      }
      SimpleAttributeSet attributes = null; 
      String nodeValue = "";
      try 
      {
          attributes = new SimpleAttributeSet();
          // Process all nodes of LabelAttributes
          //
          NodeList nl = _node.getChildNodes();  // LabelAttributes
          for (int i=0;i<nl.getLength();i++) {
              Node n = nl.item(i);
              nodeValue = n.getTextContent();
              switch (LabelAttribute.valueOf(n.getNodeName())) {
                  case FontFamily   : StyleConstants.setFontFamily(   attributes, Strings.isEmpty(nodeValue)?"Arial":nodeValue); break;
                  case FontSize     : StyleConstants.setFontSize(     attributes, Integer.valueOf(Strings.isEmpty(nodeValue)?"10":nodeValue).intValue()); break;
                  case Bold         : StyleConstants.setBold(         attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue)); break;
                  case Italic       : StyleConstants.setItalic(       attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue)); break;
                  case Underline    : StyleConstants.setUnderline(    attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue)); break;
                  case StrikeThrough: StyleConstants.setStrikeThrough(attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue)); break;
                  case Subscript    : StyleConstants.setSubscript(    attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue)); break;
                  case Superscript  : StyleConstants.setSuperscript(  attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue)); break;
                  case Foreground   : StyleConstants.setForeground(   attributes, Colours.fromRGBa(Strings.isEmpty(nodeValue)?"00,00,00,255":nodeValue)); break;
                  case Background   : StyleConstants.setBackground(   attributes, Colours.fromRGBa(Strings.isEmpty(nodeValue)?"255,255,255,255":nodeValue)); break;
              }
          }  // for each node....
          return attributes;
      } catch (Exception e) {
         System.out.println("Exception fromNode - " + e.toString());
      }
      return attributes;
  }
        
  public static SimpleAttributeSet fromXML(Node _node) 
  {
      if ( _node == null ) {
          System.out.println("XML is null"); 
          Messages.log("XML is null"); 
          return null;  // Should throw error 
      }
      if (_node.getNodeName().equalsIgnoreCase("LabelAttributes")==false ) {
          System.out.println("XML is of type " + _node.getNodeName() + " when it should be <LabelAttributes>");
          Messages.log("XML is of type " + _node.getNodeName() + " when it should be <LabelAttributes>");
          return null;  // Should throw error
      }
      if ( _node.getFirstChild()==null ) {
          System.out.println("LabelAttributes has no child nodes");
          Messages.log("LabelAttributes has no child nodes");
          return null;  // Should throw error
      }
      // return fromNode(_node);
      SimpleAttributeSet attributes = null; 
      String nodeValue = "";
      try 
      {
          XPath xpath = XPathFactory.newInstance().newXPath();
          attributes = new SimpleAttributeSet();
          nodeValue = (String)xpath.evaluate("LabelAttributes/FontFamily/text()",_node,XPathConstants.STRING);
          StyleConstants.setFontFamily(   attributes, Strings.isEmpty(nodeValue)?"Arial":nodeValue);
          nodeValue = (String)xpath.evaluate("LabelAttributes/FontSize/text()",_node,XPathConstants.STRING);
          StyleConstants.setFontSize(     attributes, Integer.valueOf(Strings.isEmpty(nodeValue)?"10":nodeValue).intValue());  
          nodeValue = (String)xpath.evaluate("LabelAttributes/Bold/text()",_node,XPathConstants.STRING);
          StyleConstants.setBold(         attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue));
          nodeValue = (String)xpath.evaluate("LabelAttributes/Italic/text()",_node,XPathConstants.STRING);
          StyleConstants.setItalic(       attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue));
          nodeValue = (String)xpath.evaluate("LabelAttributes/Underline/text()",_node,XPathConstants.STRING);
          StyleConstants.setUnderline(    attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue));
          nodeValue = (String)xpath.evaluate("LabelAttributes/StrikeThrough/text()",_node,XPathConstants.STRING);
          StyleConstants.setStrikeThrough(attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue));
          nodeValue = (String)xpath.evaluate("LabelAttributes/Subscript/text()",_node,XPathConstants.STRING);
          StyleConstants.setSubscript(    attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue));
          nodeValue = (String)xpath.evaluate("LabelAttributes/Superscript/text()",_node,XPathConstants.STRING);
          StyleConstants.setSuperscript(  attributes, Boolean.valueOf(Strings.isEmpty(nodeValue)?"false":nodeValue));
          nodeValue = (String)xpath.evaluate("LabelAttributes/Foreground/text()",_node,XPathConstants.STRING);
          StyleConstants.setForeground(   attributes, Colours.fromRGBa(Strings.isEmpty(nodeValue)?"00,00,00,255":nodeValue));
          nodeValue = (String)xpath.evaluate("LabelAttributes/Background/text()",_node,XPathConstants.STRING);
          StyleConstants.setBackground(   attributes, Colours.fromRGBa(Strings.isEmpty(nodeValue)?"255,255,255,255":nodeValue));
          return attributes;
      } catch (Exception e) {
         System.out.println("Exception fromXML - " + e.toString());
      }
      return attributes;
  }

  public JLabel createPreviewLabel() 
  {
      JLabel label = new JLabel(PREVIEW_TEXT,JLabel.CENTER) {
          private static final long serialVersionUID = 1L;
          @Override
          protected void paintComponent(Graphics g) 
          {
              Color color = g.getColor();
              g.setColor(getBackground());
              g.fillRect(0, 0, getWidth(), getHeight());
              g.setColor(color);
              super.paintComponent(g);
          }
      };
      label.setBackground(Colours.setAlpha(this.cMapBackground,255));
      label.setForeground(Color.black);
      label.setOpaque(false);
      label.setBorder(BorderFactory.createLineBorder(Color.black));
      label.setPreferredSize(new Dimension(120, 40));
      return label;
  }
  
  public void updatePreview(JLabel _previewLabel) 
  {
    StringBuilder previewText = new StringBuilder(PREVIEW_TEXT);
    String name = fontNameInputList.getSelected();
    int size = fontSizeInputList.getSelectedInt();
    if (size <= 0)
      return ;

    Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();

    attributes.put(FAMILY, name);
    attributes.put(SIZE, size);

    // Using HTML to force JLabel manage natively unsupported attributes
    if (underlineCheckBox.isSelected() || strikethroughCheckBox.isSelected()){
      previewText.insert(0,"<html>");
      previewText.append("</html>");
    }

    if (underlineCheckBox.isSelected()){
      attributes.put(UNDERLINE, UNDERLINE_LOW_ONE_PIXEL);
      previewText.insert(6,"<u>");
      previewText.insert(previewText.length() - 7, "</u>");
    }
    if (strikethroughCheckBox.isSelected()){
      attributes.put(STRIKETHROUGH, STRIKETHROUGH_ON);
      previewText.insert(6,"<strike>");
      previewText.insert(previewText.length() - 7, "</strike>");
    }

    if (boldCheckBox.isSelected())
      attributes.put(WEIGHT, WEIGHT_BOLD);
    if (italicCheckBox.isSelected())
      attributes.put(POSTURE, POSTURE_OBLIQUE);

    Font fn = new Font(attributes);

    _previewLabel.setText(previewText.toString());
    _previewLabel.setFont(fn);
    _previewLabel.setForeground(Colours.setAlpha(this.btnForegroundFontColor.getBackground(),
                                               this.alphaForeground));
    _previewLabel.setBackground(Colours.setAlpha(btnBackgroundFillColor.getBackground(),
                                               this.alphaBackground));
  } 
    
  class InputList 
  extends JPanel 
  implements ListSelectionListener, 
             ActionListener 
  {

	private static final long serialVersionUID = 8815401981570961002L;

	protected JLabel label = new JLabel();
    
      protected JTextField textfield;
    
      protected JList<String> list;
    
      protected JScrollPane scroll;
    
      public InputList(String[] _data, 
                       String title) {
        label = new OpelListLabel(title, JLabel.LEFT);
        setLayout(null);
        add(label);
        textfield = new OpelListText();
        textfield.addActionListener(this);
        label.setLabelFor(textfield);
        add(textfield);
        list = new OpelListList(_data);
        list.setVisibleRowCount(4);
        list.addListSelectionListener(this);
        scroll = new JScrollPane(list);
        add(scroll);
      }
    
      public InputList(String title, int numCols) {
        setLayout(null);
        label = new OpelListLabel(title, JLabel.LEFT);
        add(label);
        textfield = new OpelListText(numCols);
        textfield.addActionListener(this);
        label.setLabelFor(textfield);
        add(textfield);
        list = new OpelListList();
        list.setVisibleRowCount(4);
        list.addListSelectionListener(this);
        scroll = new JScrollPane(list);
        add(scroll);
      }
    
      public void setToolTipText(String text) {
        super.setToolTipText(text);
        label.setToolTipText(text);
        textfield.setToolTipText(text);
        list.setToolTipText(text);
      }
    
      public void setText(String text) {
        label.setText(text);
      }
      
      public void setDisplayedMnemonic(char ch) {
        label.setDisplayedMnemonic(ch);
      }
    
      public void setSelected(String sel) {
        list.setSelectedValue(sel, true);
        textfield.setText(sel);
      }
    
      public String getSelected() {
        return textfield.getText();
      }
    
      public void setSelectedInt(int value) {
        setSelected(Integer.toString(value));
      }
    
      public int getSelectedInt() {
        try {
          return Integer.parseInt(getSelected());
        } catch (NumberFormatException ex) {
          return -1;
        }
      }
    
      public void valueChanged(ListSelectionEvent e) {
        Object obj = list.getSelectedValue();
        if (obj != null)
          textfield.setText(obj.toString());
      }
    
      public void actionPerformed(ActionEvent e) {
        ListModel<String> model = list.getModel();
        String key = textfield.getText().toLowerCase();
        for (int k = 0; k < model.getSize(); k++) {
          String data = (String) model.getElementAt(k);
          if (data.toLowerCase().startsWith(key)) {
            list.setSelectedValue(data, true);
            break;
          }
        }
      }
    
      public void addListSelectionListener(ListSelectionListener lst) {
        list.addListSelectionListener(lst);
      }
    
      public Dimension getPreferredSize() {
        Insets ins = getInsets();
        Dimension labelSize = label.getPreferredSize();
        Dimension textfieldSize = textfield.getPreferredSize();
        Dimension scrollPaneSize = scroll.getPreferredSize();
        int w = Math.max(Math.max(labelSize.width, textfieldSize.width),
            scrollPaneSize.width);
        int h = labelSize.height + textfieldSize.height + scrollPaneSize.height;
        return new Dimension(w + ins.left + ins.right, h + ins.top + ins.bottom);
      }
    
      public Dimension getMaximumSize() {
        return getPreferredSize();
      }
    
      public Dimension getMinimumSize() {
        return getPreferredSize();
      }
    
      public void doLayout() {
        Insets ins = getInsets();
        Dimension size = getSize();
        int x = ins.left;
        int y = ins.top;
        int w = size.width - ins.left - ins.right;
        int h = size.height - ins.top - ins.bottom;
    
        Dimension labelSize = label.getPreferredSize();
        label.setBounds(x, y, w, labelSize.height);
        y += labelSize.height;
        Dimension textfieldSize = textfield.getPreferredSize();
        textfield.setBounds(x, y, w, textfieldSize.height);
        y += textfieldSize.height;
        scroll.setBounds(x, y, w, h - y);
      }
    
      public void appendResultSet(ResultSet results, int index,
          boolean toTitleCase) {
        textfield.setText("");
        DefaultListModel<String> model = new DefaultListModel<>();
        try {
          while (results.next()) {
            String str = results.getString(index);
            if (toTitleCase) {
              str = Character.toUpperCase(str.charAt(0)) + str.substring(1);
            }    
            model.addElement(str);
          }
        } catch (SQLException ex) {
          LOGGER.error("appendResultSet: " + ex.toString());
        }
        list.setModel(model);
        if (model.getSize() > 0)
          list.setSelectedIndex(0);
      }
    
      class OpelListLabel extends JLabel {

		private static final long serialVersionUID = 4143984799789394803L;

		public OpelListLabel(String text, int alignment) {
          super(text, alignment);
        }
    
        public AccessibleContext getAccessibleContext() {
          return InputList.this.getAccessibleContext();
        }
      }
    
      class OpelListText extends JTextField {

		private static final long serialVersionUID = 3256874393691308765L;

		public OpelListText() {
        }
    
        public OpelListText(int numCols) {
          super(numCols);
        }
    
        public AccessibleContext getAccessibleContext() {
          return InputList.this.getAccessibleContext();
        }
      }
    
      class OpelListList extends JList<String> {
    	  
        private static final long serialVersionUID = -7488543087044568857L;

		public OpelListList() {
        }
    
        public OpelListList(String[] data) {
          super(data);
        }
    
        public AccessibleContext getAccessibleContext() {
          return InputList.this.getAccessibleContext();
        }
      }
    
      // Accessibility Support
    
      public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null)
          accessibleContext = new AccessibleOpenList();
        return accessibleContext;
      }
    
      protected class AccessibleOpenList extends AccessibleJComponent {
    
		private static final long serialVersionUID = 608644373076998402L;

		public String getAccessibleName() {
          if (accessibleName != null)
            return accessibleName;
          return label.getText();
        }
    
        public AccessibleRole getAccessibleRole() {
          return AccessibleRole.LIST;
        }
      }
    }

}
