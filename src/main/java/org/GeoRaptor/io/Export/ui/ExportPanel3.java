package org.GeoRaptor.io.Export.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

import java.sql.Connection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import oracle.jdeveloper.layout.VerticalFlowLayout;
import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.tools.FileUtils;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.data.shapefile.shp.ShapeType;


public class ExportPanel3 extends JPanel {
    
	private static final long serialVersionUID = 7200945103277286686L;

	/**
     * Get reference to single instance of GeoRaptor's Preferences
     */
    protected Preferences geoRaptorPreferences = null;
    private Constants.EXPORT_TYPE exportType = Constants.EXPORT_TYPE.SHP;
    protected Connection                conn = null;
    private String     SRID_GOOGLE_COMPLIANT = "SRID must be Google Maps compliant";
    private String  SRID_NULL_NOT_EXPORTABLE = "NULL SRID Geometries cannot be exported.";
    private String             sCommitFormat = "Commit(%-4d)";
    
    private   JPanel           pnlExport = new JPanel();
    private   JLabel           lblEntity = new JLabel();
    protected JTextField        tfEntity = new JTextField();
    private   JLabel         lblFilename = new JLabel();
    protected JTextField      tfFilename = new JTextField();
    protected JButton        btnFilename = new JButton();
    private   XYLayout      layoutExport = new XYLayout();
    protected JCheckBox    cbSkipNullRow = new JCheckBox();
    protected JTextField          tfSRID = new JTextField();
    protected JButton            btnSRID = new JButton();
    private   JLabel             lblSRID = new JLabel();
    private   JLabel          lblCharset = new JLabel();
    private   JButton  btnCharsetDefault = new JButton();
    private   JLabel           lblStatus = new JLabel();
    private   JLabel   lblCommitInterval = new JLabel();
    protected JSlider sldrCommitInterval = new JSlider();
    private   JLabel        lblShapeType = new JLabel();
    private   JButton             btnPRJ = new JButton();
    protected JComboBox<String>    cmbCharset = new JComboBox<>();
    protected JComboBox<String> cmbShapeTypes = new JComboBox<>();

    public ExportPanel3() {
        super();
        // Get the one reference to GeoRaptor's preferences
        //
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
        jbInit();
    }

  private void jbInit() {
      
      this.setLayout(new FlowLayout());
      this.setMinimumSize(new Dimension(400, 300));
      this.setMaximumSize(new Dimension(400, 300));
      this.setPreferredSize(new Dimension(400, 300));

      pnlExport.setBorder(BorderFactory.createTitledBorder("Export Options"));
      pnlExport.setPreferredSize(new Dimension(400, 290));
      pnlExport.setMaximumSize(new Dimension(400, 300));
      pnlExport.setMinimumSize(new Dimension(400, 300));
      pnlExport.setSize(new Dimension(400, 300));
      pnlExport.setLayout(layoutExport);

      pnlExport.setBounds(new Rectangle(0, 5, 400, 280));
      lblEntity.setText("Entity:");
      tfEntity.setEditable(false);
      
      lblFilename.setText("Filename:");
      lblFilename.setHorizontalAlignment(SwingConstants.LEFT);
      lblFilename.setPreferredSize(new Dimension(50, 15));
      lblFilename.setMinimumSize(new Dimension(50, 15));
      lblFilename.setMaximumSize(new Dimension(50, 1514));
      
      tfFilename.setActionCommand("TF_FILE_NAME");
      tfFilename.setName("TF_FILE_NAME");
      tfFilename.setFont(new Font("Tahoma", 0, 10));
      
      btnFilename.setText("File");
      btnFilename.setSize(new Dimension(50, 21));
      btnFilename.setPreferredSize(new Dimension(55, 20));
      btnFilename.setMaximumSize(new Dimension(55, 20));
      btnFilename.setMinimumSize(new Dimension(55, 20));
      btnFilename.setActionCommand("BTN_SKIP_NULL_ROW");
      
      cbSkipNullRow.setText("Skip NULL Geometry rows?");
      cbSkipNullRow.setSelected(true);
      cbSkipNullRow.setActionCommand("SKIP_NULL_ROW");
      
      lblSRID.setText("SRID:");
      tfSRID.setText("NULL");
      tfSRID.setName("TF_SRID");
      tfSRID.setActionCommand("TF_SRID");
      
      btnSRID.setText("Get SRID");
      btnSRID.setActionCommand("BTN_SRID");
      
      lblCharset.setText("Charset:");
      btnCharsetDefault.setText("Set Default");
      btnCharsetDefault.setEnabled(false);
      btnCharsetDefault.setVisible(false);
                  
      cmbCharset.setActionCommand("CMB_CHARSET");
    
      lblStatus.setText(" ");
      lblStatus.setPreferredSize(new Dimension(375, 15));
      lblStatus.setMinimumSize(new Dimension(375, 15));
      lblStatus.setMaximumSize(new Dimension(375, 15));

      cmbShapeTypes.setActionCommand("SHAPE_TYPES");
      lblShapeType.setText("ShapeType:");

        btnPRJ.setText("PRJ / CoordSys");
        btnPRJ.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnPRJ_actionPerformed(e);
                }
            });
        lblCommitInterval.setText("Commit(100)");
        sldrCommitInterval.setValue(100);
        sldrCommitInterval.setName("SLDR_COMMIT");
        sldrCommitInterval.setMaximum(1000);
        sldrCommitInterval.setMinorTickSpacing(25);
        sldrCommitInterval.setMajorTickSpacing(100);
        sldrCommitInterval.setPaintTicks(true);
        sldrCommitInterval.setPaintLabels(true);
        sldrCommitInterval.setSnapToTicks(true);
        sldrCommitInterval.setEnabled(true);

        pnlExport.add(btnPRJ, new XYConstraints(230, 86, 115, 20));
        pnlExport.add(lblShapeType, new XYConstraints(235, 66, 60, 10));
        pnlExport.add(cmbShapeTypes, new XYConstraints(295, 61, 85, 20));
        pnlExport.add(lblCommitInterval, new XYConstraints(0, 116, 65, 15));
        pnlExport.add(sldrCommitInterval, new XYConstraints(70, 116, 300, 35));
        pnlExport.add(lblStatus, new XYConstraints(5, 246, 380, 15));
        pnlExport.add(tfEntity, new XYConstraints(70, 6, 190, 20));
        pnlExport.add(lblEntity, new XYConstraints(33, 6, 32, 14));
        pnlExport.add(btnCharsetDefault, new XYConstraints(215, 141, 95, 20));
        pnlExport.add(cmbCharset, new XYConstraints(70, 161, 150, 20));
        pnlExport.add(lblCharset, new XYConstraints(20, 161, 45, 15));
        pnlExport.add(lblSRID, new XYConstraints(37, 86, 28, 14));
        pnlExport.add(tfFilename, new XYConstraints(70, 31, 270, 20));
        pnlExport.add(btnSRID, new XYConstraints(130, 86, 90, 20));
        pnlExport.add(tfSRID, new XYConstraints(70, 86, 50, 20));

        pnlExport.add(cbSkipNullRow, new XYConstraints(70, 61, 150, 20));
        pnlExport.add(lblFilename, new XYConstraints(15, 31, 50, 15));
        pnlExport.add(btnFilename, new XYConstraints(345, 31, 45, 20));
        this.add(pnlExport, new XYConstraints(100, 5, 165, 85));
    }

    public void addChangeListeners(ChangeListener _listenerInDescriptor) {
        this.sldrCommitInterval.addChangeListener(_listenerInDescriptor);
    }

  public void addActionListeners(ActionListener _listenerInDescriptor) {
      this.tfFilename.addActionListener(_listenerInDescriptor);
      this.btnFilename.addActionListener(_listenerInDescriptor);
      this.tfSRID.addActionListener(_listenerInDescriptor);
      this.btnSRID.addActionListener(_listenerInDescriptor);
      
      this.cbSkipNullRow.addActionListener(_listenerInDescriptor);
      this.cmbCharset.addActionListener(_listenerInDescriptor);
      
      this.cmbShapeTypes.addActionListener(_listenerInDescriptor);
  }

  public void addFocusListeners(FocusListener _listenerInDescriptor) {
      this.tfFilename.addFocusListener(_listenerInDescriptor);
      this.tfSRID.addFocusListener(_listenerInDescriptor);
  }
  
  public int getCommit() {
      int commitValue = this.sldrCommitInterval.getValue() == 0
          ? 1 
          : this.sldrCommitInterval.getValue();
      return commitValue;
  }
  
  public void setCommitLabel(int _commit) {
      this.lblCommitInterval.setText(String.format(this.sCommitFormat,_commit).replaceAll(" ",""));
  }

  public void setStatusText(String _text) {
      if (Strings.isEmpty(_text) ) {
          this.lblStatus.setForeground(Color.BLACK);
          this.lblStatus.setText("");
      } else {
          Toolkit.getDefaultToolkit().beep();
          this.lblStatus.setText(Strings.trimAll(_text, ' ' ));
          this.lblStatus.setForeground(Color.RED);
      }
  }

  public void setSkipNullGeometry(boolean _skip) {
      this.cbSkipNullRow.setSelected(_skip);      
  }
  
  public boolean isSkipNullGeometry() {
    return cbSkipNullRow.isSelected();      
  }
  
  public void setFilename(String _filename) {
      this.tfFilename.setText(_filename);
      this.tfEntity.setText(FileUtils.getFileNameFromPath(this.tfFilename.getText(),true));
  }

  public String getFilename() {
      return this.tfFilename.getText();
  }

  public String getEntityName() {
      return this.tfEntity.getText();  
  }
  
  public void setCharset(String _oracleCharset) {
      this.cmbCharset.removeAllItems();
      ArrayList<String> charsets = Tools.getCharsets(_oracleCharset);
      Iterator<String> iter = charsets.iterator();
      while (iter.hasNext()) {
          this.cmbCharset.addItem(iter.next());
      } 
  }
  
  public String getCharset() {
      return this.cmbCharset.getSelectedIndex()==-1 
             ? this.cmbCharset.getItemAt(0) 
             : (String)this.cmbCharset.getSelectedItem();
  }
  
  public void setSRID(String _SRID) {
      int SRID = (Strings.isEmpty(_SRID) || _SRID.equalsIgnoreCase(Constants.NULL) ) ? Constants.SRID_NULL : Integer.valueOf(_SRID);
      setSRID(SRID);
  }

  public void setSRID(int _SRID) {
      this.tfSRID.setText(_SRID == Constants.SRID_NULL ? Constants.NULL : String.valueOf(_SRID));
      if ( _SRID == Constants.SRID_NULL &&
           ( this.exportType==Constants.EXPORT_TYPE.GML || 
             this.exportType==Constants.EXPORT_TYPE.KML ) ) {
          this.setStatusText(this.SRID_NULL_NOT_EXPORTABLE);
      } else {        
          this.setStatusText(this.exportType==Constants.EXPORT_TYPE.KML
                             ?this.SRID_GOOGLE_COMPLIANT
                             :null);
      }
  }
  
  public int getSRID() {
      try {
        return this.tfSRID.getText().toUpperCase().equals(Constants.NULL) || Strings.isEmpty(this.tfSRID.getText())
                ? Constants.SRID_NULL
                : Integer.valueOf(this.tfSRID.getText()).intValue();
      } catch (Exception e) {
        return Constants.SRID_NULL;
      }
  }
  
    public void setExportType(Constants.EXPORT_TYPE exportType) {
        this.exportType = exportType;
        this.lblShapeType.setVisible(this.exportType==Constants.EXPORT_TYPE.SHP||this.exportType==Constants.EXPORT_TYPE.TAB);
        this.cmbShapeTypes.setVisible(this.exportType==Constants.EXPORT_TYPE.SHP||this.exportType==Constants.EXPORT_TYPE.TAB);
        this.btnPRJ.setVisible(this.exportType==Constants.EXPORT_TYPE.SHP||this.exportType==Constants.EXPORT_TYPE.TAB);
        this.lblCharset.setVisible(true);
        this.cmbCharset.setVisible(true);
    }
    
    public void setConn(Connection _conn) {
        this.conn = _conn;
    }

  public void setShapefileType(Constants.SHAPE_TYPE _shapeType) {
      if ( _shapeType == null ) {
          return;
      }
      if ( this.cmbShapeTypes.getItemCount() == 0 ) {
          this.setAllShapeTypes();
      }
      for (int i=0; i<this.cmbShapeTypes.getItemCount(); i++ ) {
          if ( this.cmbShapeTypes.getItemAt(i).toString().equalsIgnoreCase(_shapeType.toString()) ) {
              this.cmbShapeTypes.setSelectedIndex(i);
              return;
          }
      }        
  }

  public ShapeType getShapefileType() {
      int item = this.cmbShapeTypes.getSelectedIndex()==-1?0:this.cmbShapeTypes.getSelectedIndex();
      Constants.SHAPE_TYPE st = Constants.SHAPE_TYPE.valueOf(this.cmbShapeTypes.getItemAt(item).toString());
      return ShapeType.forID(st.id);
  }
  
  public void setAllShapeTypes() {
      this.cmbShapeTypes.setModel((javax.swing.DefaultComboBoxModel<String>)Constants.SHAPE_TYPE.getArcShapeTypeCombo());
  }
  
  public void setShapeTypes(List<String> _shapeTypes) {
      if ( _shapeTypes == null || _shapeTypes.size()==0) {
          setAllShapeTypes();
      } else {
          ComboBoxModel<String> shapeTypes = new DefaultComboBoxModel<String>(_shapeTypes.toArray(new String[0]));
          this.cmbShapeTypes.setModel(shapeTypes);
      }
      this.cmbShapeTypes.setSelectedIndex(0);
  }

    private void btnPRJ_actionPerformed(ActionEvent e) 
    {
        
        JPanel contents = new JPanel(new VerticalFlowLayout());
        JTextArea prjCoordSys = new JTextArea(this.exportType==Constants.EXPORT_TYPE.SHP
                                              ? this.geoRaptorPreferences.getPrj()
                                              : this.geoRaptorPreferences.getCoordsys());
        prjCoordSys.setPreferredSize(new Dimension(200,200));
        prjCoordSys.setLineWrap(true);
        prjCoordSys.setAlignmentX(Component.LEFT_ALIGNMENT);
        contents.add(prjCoordSys);
        JLabel lblSpatialReferenceOrg = new JLabel("See: " + ( this.exportType==Constants.EXPORT_TYPE.SHP
                                                               ? "www.spatialreference.org"
                                                               : "<MapInfo installation path>\\MAPINFOW.prj"));
        lblSpatialReferenceOrg.setAlignmentX(Component.LEFT_ALIGNMENT);
        contents.add(lblSpatialReferenceOrg);
      
        Object[] options = { "OK", "CANCEL" }; // { propertyManager.getMsg("MD_DELETE_MB_YES_BUTTON"), propertyManager.getMsg("MD_DELETE_MB_NO_BUTTON") };
        int returnStatus = JOptionPane.showOptionDialog( this,
                                                         contents, 
                                                         Constants.GEORAPTOR + " - " + String.format("Enter %s string",
                                                                                                     this.exportType==Constants.EXPORT_TYPE.SHP?"PRJ":"Coordsys"), 
                                                         JOptionPane.OK_CANCEL_OPTION,
                                                         JOptionPane.PLAIN_MESSAGE,
                                                         null,
                                                         options,
                                                         options[0]);
        if (returnStatus == 0) {
            if ( this.exportType==Constants.EXPORT_TYPE.SHP )
                this.geoRaptorPreferences.setPrj(prjCoordSys.getText());
            else
                this.geoRaptorPreferences.setCoordsys(prjCoordSys.getText());
        }
    }
}
