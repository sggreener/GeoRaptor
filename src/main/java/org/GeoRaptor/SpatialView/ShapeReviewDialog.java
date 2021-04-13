package org.GeoRaptor.SpatialView;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import oracle.jdeveloper.layout.VerticalFlowLayout;

class ShapeReviewDialog 
extends JDialog
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
  
  private PropertiesManager propertyManager = null;

  private SpatialView       spatialView = null;

  /** Creates the reusable dialog. */
  public ShapeReviewDialog(Frame             _aFrame,
                           String            _text,
                           PropertiesManager _propertyManager,
                           SpatialView       _spatialView) 
  {
      super(_aFrame, true);
      
      setTitle(_propertyManager.getMsg("COPY_TO_CLIPBOARD_TITLE"));

      this.propertyManager = _propertyManager;
      this.spatialView     = _spatialView;
      
      this.originalSdoGeometryString = _text;
      
      this.dFormat = Tools.getDecimalFormatter(spatialView.getPrecision(false),false);
      
      this.lblQuestion.setText(this.propertyManager.getMsg("COPY_TO_CLIPBOARD_TITLE"));
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
      rbPrecisionView.setText(this.propertyManager.getMsg("rbPrecisionView") + 
    		                  " (" + 
    		                  this.spatialView.getPrecision(false) + ")");
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
      String lengthUnits  = Tools.getViewUnits(this.spatialView,Constants.MEASURE.LENGTH);
      String bufferParams = ( ( this.spatialView.getSRID().equals(Constants.NULL) || Strings.isEmpty(lengthUnits) )
                              ?   ")"
                              : ",'unit="+lengthUnits  + "')" );
      /** 
       * SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY,
       *                     dist IN NUMBER,
       *                     tol IN NUMBER
       *                     [, params IN VARCHAR2]
       *                    ) RETURN SDO_GEOMETRY;
       */
      displayString = "MDSYS.SDO_GEOM.SDO_BUFFER(" + 
                          displayString + "," + 
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
