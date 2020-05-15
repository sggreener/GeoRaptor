package org.GeoRaptor.io.Export.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import java.io.File;
import java.io.IOException;

import java.sql.Connection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import oracle.jdbc.OracleConnection;

import oracle.jdeveloper.layout.VerticalFlowLayout;
import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.SRID.SRIDPanel;
import org.GeoRaptor.io.Export.DBaseWriter;
import org.GeoRaptor.io.ExtensionFileFilter;
import org.GeoRaptor.tools.FileUtils;
import org.GeoRaptor.tools.JStatusBar;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.util.logging.Logger;

public class ExporterDialog extends JDialog 
{

    /**
	 * 
	 */
	private static final long serialVersionUID = -2415816894343885314L;

	@SuppressWarnings("unused")
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.ui.ExporterDialog");

    /**
     * Get reference to single instance of GeoRaptor's Preferences
     */
    protected Preferences geoRaptorPreferences;

    protected Connection         conn = null;
    private boolean         CANCELLED = false;

    private JPanel          pnlExport = new JPanel();
    private JLabel        lblFilename = new JLabel();
    private JButton       btnFilename = new JButton();
    private XYLayout     layoutExport = new XYLayout();
    private JCheckBox   cbSkipNullRow = new JCheckBox();
    private JCheckBox    cbAttributes = new JCheckBox();
    private JTextField         tfSRID = new JTextField();
    private JButton           btnSRID = new JButton();
    private JTextField     tfFilename = new JTextField();
    private JLabel            lblSRID = new JLabel();
    private JLabel         lblCharset = new JLabel();
    private JComboBox<String> cmbCharset = new JComboBox<>();
    private JButton btnCharsetDefault = new JButton();
    private JButton        btnExecute = new JButton();
    private JButton         btnCancel = new JButton();
    private JStatusBar   pnlStatusBar = new JStatusBar(new Dimension(385, 23));
    private JLabel         lblFlavour = new JLabel();
    private JLabel          lblEntity = new JLabel();
    private JTextField       tfEntity = new JTextField();
    private JComboBox<String> cmbAttributeFlavourType = new JComboBox<>();

    private Constants.EXPORT_TYPE EXPORT_TYPE = Constants.EXPORT_TYPE.GML;
    private JLabel          lblCommitInterval = new JLabel();
    private JSlider        sldrCommitInterval = new JSlider();

    private String sCommitFormat            = "Commit(%-4d)";
    private String SRID_NOT_NULL            = "To proceed SRID must not be NULL";
    private String SRID_INTEGER             = "SRID must be an integer";
    private String SRID_GOOGLE_COMPLIANT    = "SRID must be Google Maps compliant";
    private String SRID_NULL_NOT_EXPORTABLE = "NULL SRID Geometries cannot be exported.";
    private String GEOMETRY_FILTER_FORMAT   = "Filtering by %s";
    
    private JLabel     lblShapeType = new JLabel();
    private JComboBox<String> cmbShapeTypes = new JComboBox<>();
    private JButton          btnPRJ = new JButton();

    public ExporterDialog(JDialog               _owner,
                          Connection            _conn,
                          Constants.EXPORT_TYPE _EXPORT_TYPE) 
    {
        super(_owner,"ExporterDialog",true);
        try {
            // Get the one reference to GeoRaptor's preferences
            //
            this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
            this.conn = _conn;
            this.EXPORT_TYPE = _EXPORT_TYPE;
            jbInit();
            this.lblCommitInterval.setText(
                    String.format(this.sCommitFormat,
                                  Math.max(sldrCommitInterval.getValue(),1)
                                 ).replaceAll(" ","")
                );
            setAttributeFlavour();
            setExportTypeWidgets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void jbInit() throws Exception {
        this.setSize(new Dimension(422, 318));
        this.setResizable(false);
        this.setTitle(this.EXPORT_TYPE.toString() + " Exporter");
        pnlExport.setBorder(BorderFactory.createTitledBorder("Export Options"));
        pnlExport.setPreferredSize(new Dimension(404, 251));
        pnlExport.setMaximumSize(new Dimension(404, 251));
        pnlExport.setMinimumSize(new Dimension(404, 251));
        pnlExport.setLayout(layoutExport);
        pnlExport.setSize(new Dimension(398, 200));
        lblFilename.setText("Filename:");
        lblFilename.setHorizontalAlignment(SwingConstants.LEFT);
        lblFilename.setPreferredSize(new Dimension(50, 15));
        lblFilename.setMinimumSize(new Dimension(50, 15));
        lblFilename.setMaximumSize(new Dimension(50, 1514));
        btnFilename.setText("File");
        btnFilename.setSize(new Dimension(50, 21));
        btnFilename.setPreferredSize(new Dimension(55, 20));
        btnFilename.setMaximumSize(new Dimension(55, 20));
        btnFilename.setMinimumSize(new Dimension(55, 20));
        btnFilename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnFilenameActionPerformed(e);
                }
            });
        cbSkipNullRow.setText("Skip NULL Geometry rows?");
        cbSkipNullRow.setPreferredSize(new Dimension(150, 20));
        cbSkipNullRow.setMinimumSize(new Dimension(150, 20));
        cbSkipNullRow.setMaximumSize(new Dimension(150, 20));
        cbAttributes.setText("Output Attributes?");
        cbAttributes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cbAttributesActionPerformed(evt);
            }
        });
        
        tfSRID.setText("NULL");
        tfSRID.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent evt) {
                tfSRIDFocusLost(evt);
            }
        });
        btnSRID.setText("Get SRID");
        btnSRID.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnSRIDActionPerformed(e);
                }
            });
        this.tfFilename.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent evt) {
                tfFilenameFocusLost(evt);
            }
        });

        this.tfFilename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tfFilenameActionPerformed(e);
                }
            });
        lblSRID.setText("SRID:");
        lblCharset.setText("Charset:");
        btnCharsetDefault.setText("Set Default");
        btnCharsetDefault.setEnabled(false);
        btnCharsetDefault.setVisible(false);
        
        btnExecute.setText("Execute");
        btnExecute.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnExecuteActionPerformed(e);
                }
            });
        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnCancelActionPerformed(e);
                }
            });
        pnlStatusBar.setSize(new Dimension(385, 23));

        lblFlavour.setText("Flavour:");
        lblEntity.setText("Entity:");
        tfEntity.setEditable(false);
        cmbAttributeFlavourType.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cmbAttributeFlavourType_actionPerformed(e);
                }
            });
        lblCommitInterval.setText("Commit(1)");
        lblCommitInterval.setHorizontalTextPosition(SwingConstants.RIGHT);
        lblCommitInterval.setHorizontalAlignment(SwingConstants.LEFT);
        sldrCommitInterval.setMinimum(0);
        sldrCommitInterval.setMaximum(1000);
        sldrCommitInterval.setMinorTickSpacing(25);
        sldrCommitInterval.setMajorTickSpacing(100);
        sldrCommitInterval.setPaintTicks(true);
        sldrCommitInterval.setPaintLabels(true);
        sldrCommitInterval.setSnapToTicks(true);
        sldrCommitInterval.setValue(100);
        sldrCommitInterval.addChangeListener(new ChangeListener() {
          public void stateChanged(ChangeEvent evt) {
              sldrCommitIntervalStateChanged(evt);
          }
          public void sldrCommitIntervalStateChanged(ChangeEvent evt) {
              int commitValue = Math.max(sldrCommitInterval.getValue(),1); // Never be 0
              lblCommitInterval.setText(
                 String.format(sCommitFormat,commitValue).replaceAll(" ","")
              );
          }
        });
        sldrCommitInterval.setEnabled(true);
        
        lblShapeType.setText("ShapeType:");
        cmbShapeTypes.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cmbShapeTypes_actionPerformed(e);
                }
            });
        cmbShapeTypes.setModel(Constants.SHAPE_TYPE.getArcShapeTypeCombo());

        btnPRJ.setText("PRJ / Coordsys");
        btnPRJ.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnPRJ_actionPerformed(e);
                }
            });
        this.pnlStatusBar.setLeftLabelTextSize(10);
        this.pnlStatusBar.setCenterLabelTextSize(10);
        this.pnlStatusBar.setTimePanelTextSize(10);
        this.pnlStatusBar.setRightLabelTextSize(10);

        pnlExport.add(btnPRJ, new XYConstraints(220, 81, 110, 20));
        pnlExport.add(cmbShapeTypes, new XYConstraints(275, 54, 130, 20));
        pnlExport.add(lblShapeType, new XYConstraints(215, 59, 58, 14));
        pnlExport.add(sldrCommitInterval, new XYConstraints(80, 139, 325, 35));
        pnlExport.add(lblCommitInterval, new XYConstraints(0, 139, 70, 15));
        pnlExport.add(cmbAttributeFlavourType, new XYConstraints(220, 109, 185, 20));
        pnlExport.add(tfEntity, new XYConstraints(55, 6, 150, 20));
        pnlExport.add(lblEntity, new XYConstraints(23, 6, -1, -1));
        pnlExport.add(lblFlavour, new XYConstraints(175, 111, 40, 20));
        pnlExport.add(pnlStatusBar, new XYConstraints(0, 241, 410, 25));
        pnlExport.add(btnCancel, new XYConstraints(330, 216, 75, 21));
        pnlExport.add(btnExecute, new XYConstraints(5, 216, 73, 21));
        pnlExport.add(btnCharsetDefault, new XYConstraints(215, 141, 95, 20));
        pnlExport.add(cmbCharset, new XYConstraints(75, 186, 150, 20));
        pnlExport.add(lblCharset, new XYConstraints(30, 186, 45, 15));
        pnlExport.add(lblSRID, new XYConstraints(30, 84, 28, 14));
        pnlExport.add(tfFilename, new XYConstraints(55, 31, 290, 20));
        pnlExport.add(btnSRID, new XYConstraints(115, 81, 90, 20));
        pnlExport.add(tfSRID, new XYConstraints(55, 81, 50, 20));
        pnlExport.add(cbAttributes, new XYConstraints(55, 111, 115, 20));
        pnlExport.add(cbSkipNullRow, new XYConstraints(55, 56, 150, 20));
        pnlExport.add(lblFilename, new XYConstraints(5, 31, 50, 15));
        pnlExport.add(btnFilename, new XYConstraints(350, 31, 55, 20));
        this.getContentPane().add(pnlExport, null);
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
               ? (String)this.cmbCharset.getItemAt(0) 
               : (String)this.cmbCharset.getSelectedItem();
    }

    public boolean hasAttributes() {
      return this.cbAttributes.isSelected();
    }

    public void setAttributes(boolean _process) {
        this.cbAttributes.setSelected(_process);
        this.cbAttributes.setEnabled(_process);
        setExportTypeWidgets();
    }
    
    private void setExportTypeWidgets() {
        boolean isShpTab = this.EXPORT_TYPE==Constants.EXPORT_TYPE.SHP || this.EXPORT_TYPE==Constants.EXPORT_TYPE.TAB;
        this.lblCharset.setVisible(true);
        this.lblCharset.setEnabled(true);
        this.cmbCharset.setVisible(true);
        this.cmbCharset.setEnabled(true);      
        
        this.lblFlavour.setVisible(this.EXPORT_TYPE!=Constants.EXPORT_TYPE.KML);
        this.lblFlavour.setVisible(this.EXPORT_TYPE!=Constants.EXPORT_TYPE.KML);
        this.cmbAttributeFlavourType.setEnabled(this.EXPORT_TYPE!=Constants.EXPORT_TYPE.KML);
        this.cmbAttributeFlavourType.setVisible(this.EXPORT_TYPE!=Constants.EXPORT_TYPE.KML);
        
        this.lblShapeType.setVisible(isShpTab);
        this.lblShapeType.setEnabled(isShpTab);
        this.cmbShapeTypes.setVisible(isShpTab);
        this.cmbShapeTypes.setEnabled(isShpTab);
        this.btnPRJ.setVisible(isShpTab);
        this.btnPRJ.setEnabled(isShpTab);
    }
    
    public Object getShapefileType(boolean _asShapeType) {
        Constants.SHAPE_TYPE tmpShpType = Constants.SHAPE_TYPE.valueOf(this.cmbShapeTypes.getSelectedItem().toString()); 
          return _asShapeType 
                 ? ShapeType.forID(tmpShpType.id) 
                 : tmpShpType; 
    }

    public void setShapefileType(Constants.SHAPE_TYPE _shapeType) {
        if ( _shapeType == null ) {
            return;
        }
        if ( this.cmbShapeTypes == null ) {
            this.setAllShapeTypes();
        }
        for (int i=0; i<this.cmbShapeTypes.getItemCount(); i++ ) {
            if ( this.cmbShapeTypes.getItemAt(i).toString().equals(_shapeType.toString()) ) {
                this.cmbShapeTypes.setSelectedIndex(i);
                this.setLeftLabelText(String.format(this.GEOMETRY_FILTER_FORMAT,this.cmbShapeTypes.getItemAt(i).toString()));
                return;
            }
        }        
    }

    public void setAllShapeTypes() {
        if ( this.cmbShapeTypes != null ) {
            this.cmbShapeTypes.setModel(Constants.SHAPE_TYPE.getArcShapeTypeCombo());
        }
    }
    
    public void setShapeTypes(List<String> _shapeTypes) {
        if ( _shapeTypes == null ) {
            return;
        }
        if ( this.cmbShapeTypes != null ) {
            this.cmbShapeTypes.setModel(new DefaultComboBoxModel<String>(_shapeTypes.toArray(new String[0])));
        }
    }
    
    private void setAttributeFlavour() {
        switch (this.EXPORT_TYPE) {
          case TAB :
          case SHP : this.cmbAttributeFlavourType.setModel(DBaseWriter.getXBaseTypesAsCombo());             break;
          case KML : this.cmbAttributeFlavourType.setModel(Constants.getXMLAttributeFlavourAsCombo(true));  break;
          default  : 
          case GML : this.cmbAttributeFlavourType.setModel(Constants.getXMLAttributeFlavourAsCombo(false)); break;
        }
        this.cmbAttributeFlavourType.setSelectedIndex(0);
    }
    
    public String getAttributeFlavour() 
    {
        return this.cmbAttributeFlavourType.getSelectedIndex()==-1
               ? (String)this.cmbAttributeFlavourType.getItemAt(0)
               : (String)this.cmbAttributeFlavourType.getSelectedItem();
    }

    public int getCommit() {
      return sldrCommitInterval.getValue();
    }
    
    public void setSRID(String _SRID) {
        int SRID = (Strings.isEmpty(_SRID) || _SRID.equalsIgnoreCase(Constants.NULL) ) ? Constants.SRID_NULL : Integer.valueOf(_SRID);
        setSRID(SRID);
    }

    public void setSRID(int _SRID) {
        this.tfSRID.setText(_SRID == Constants.SRID_NULL ? Constants.NULL : String.valueOf(_SRID));
        if ( _SRID == Constants.SRID_NULL &&
             ( this.EXPORT_TYPE == Constants.EXPORT_TYPE.GML ||
               this.EXPORT_TYPE == Constants.EXPORT_TYPE.KML ) ) {
            this.setLeftLabelText(this.SRID_NULL_NOT_EXPORTABLE);
        } else { 
            this.setLeftLabelText(this.EXPORT_TYPE==Constants.EXPORT_TYPE.KML
                                  ? this.SRID_GOOGLE_COMPLIANT
                                  : null);
        }
    }
    
    public int getSRID() {
        try {
          return this.tfSRID.getText().equals(Constants.NULL) || Strings.isEmpty(this.tfSRID.getText())
                  ? Constants.SRID_NULL
                  : (Integer.valueOf(this.tfSRID.getText()).intValue()==0
                     ? Constants.SRID_NULL 
                     : Integer.valueOf(this.tfSRID.getText()).intValue()) ;
        } catch (Exception e) {
          return Constants.SRID_NULL;
        }
    }
    
    public void setCenterLabelText(String _text) {
        this.pnlStatusBar.setCenterLabelText(Strings.trimAll(_text, ' ' ));
    }
  
    public void setLeftLabelText(String _text) {
        if (Strings.isEmpty(_text) ) {
            this.pnlStatusBar.setLeftLabelText("",Color.BLACK);
        } else {
            Toolkit.getDefaultToolkit().beep();
            this.pnlStatusBar.setLeftLabelText(Strings.trimAll(_text, ' ' ),Color.RED);
        }
    }
  
    public void setRightLabelText(String _text) {
        this.pnlStatusBar.setRightLabelText(Strings.trimAll(_text, ' ' ));
    }

    public boolean wasCancelled() {
      return this.CANCELLED;
    }

    private void tfSRIDFocusLost(FocusEvent evt) {
        if ( this.tfSRID.getText().equalsIgnoreCase(Constants.NULL) &&
             ( this.EXPORT_TYPE == Constants.EXPORT_TYPE.GML ||
               this.EXPORT_TYPE == Constants.EXPORT_TYPE.KML ) ) {
            this.setLeftLabelText(this.SRID_NOT_NULL);
            return;
        } else {
            try {
                Integer.valueOf(this.tfSRID.getText() );
                this.setLeftLabelText(this.EXPORT_TYPE==Constants.EXPORT_TYPE.KML
                                      ?this.SRID_GOOGLE_COMPLIANT
                                      :null);
            } catch (NumberFormatException nfe) {
                this.setLeftLabelText(this.SRID_INTEGER);
            }
        }
    }                                           

    private void btnFilenameActionPerformed(ActionEvent e) {
        final String extractName = FileUtils.getFileNameFromPath(this.tfFilename.getText(), false);
        File f = new File(this.tfFilename.getText());
        f = new File(f.getParent());
        // Set the current directory
        try {
            f = new File(f.getCanonicalPath());
        } catch (IOException g) {
            f = new File(f.getParent());
        }
        ExtensionFileFilter extFilter = new ExtensionFileFilter(EXPORT_TYPE.toString().toUpperCase() + " Files", 
                                                     new String[] { EXPORT_TYPE.toString().toUpperCase() });
        JFileChooser fc = new JFileChooser(f);
        fc.setFileFilter(extFilter);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        this.setAlwaysOnTop(false);
        int returnVal = fc.showOpenDialog(this);
        this.setAlwaysOnTop(true);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    String fileName = file.getAbsolutePath();
                    if ( !Strings.isEmpty(fileName) ) {
                        tfFilename.setText(fileName);
                        if ( ! fileName.toLowerCase(Locale.getDefault()).endsWith(EXPORT_TYPE.toString().toLowerCase(Locale.getDefault())) ) {
                            tfFilename.setText(fileName + "." + EXPORT_TYPE.toString().toLowerCase(Locale.getDefault()));
                        }
                    } else {
                        tfFilename.setText(extractName);
                    }
                    tfEntity.setText(FileUtils.getFileNameFromPath(tfFilename.getText(),true));
                }
            });
        } // if
    }

    private void btnCancelActionPerformed(ActionEvent e) {
      this.CANCELLED = true;
      this.setVisible(false);
    }

    private void btnExecuteActionPerformed(ActionEvent e) {
        // Can only get out if GML's SRID is not -1
        //
        if ( this.getSRID() == Constants.SRID_NULL &&
             ( this.EXPORT_TYPE == Constants.EXPORT_TYPE.GML ||
               this.EXPORT_TYPE == Constants.EXPORT_TYPE.KML ) ) {
            JOptionPane.showMessageDialog(this, 
                                          this.SRID_NOT_NULL, 
                                          EXPORT_TYPE.toString() + " exporter.", 
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.geoRaptorPreferences.setExportDirectory(FileUtils.getDirectory(this.tfFilename.getText()));
        this.CANCELLED = false;
        this.setVisible(false);
    }

    private void btnSRIDActionPerformed(ActionEvent e) {
      this.setAlwaysOnTop(false);
      // get SRID to pass to SRIDPanel
      //
      String srid = this.tfSRID.getText().trim();
      srid = (Strings.isEmpty(srid) 
               ? this.geoRaptorPreferences.getSRID()
               : srid);
      // Pass SRID to SRIDPanel
      //
      SRIDPanel sp = SRIDPanel.getInstance();
      boolean status = sp.initialise((OracleConnection)this.conn,srid);
      if (status == true) {
          sp.setLocationRelativeTo(this);
          sp.setVisible(true);
          if ( ! sp.formCancelled() ) {
              this.setSRID(sp.getSRID());
          }
      }
      this.setAlwaysOnTop(true);
    }

    private void cbAttributesActionPerformed(ActionEvent evt) {
        setExportTypeWidgets();
    }

    private void tfFilenameActionPerformed(ActionEvent e) {
        this.tfEntity.setText(FileUtils.getFileNameFromPath(this.tfFilename.getText(),true));
    }
    
    private void tfFilenameFocusLost(FocusEvent evt) {
        this.tfEntity.setText(FileUtils.getFileNameFromPath(this.tfFilename.getText(),true));
    }

    private void cmbAttributeFlavourType_actionPerformed(ActionEvent e) {
        this.setRightLabelText(this.EXPORT_TYPE.toString() + "/" + (String)this.cmbAttributeFlavourType.getSelectedItem());
    }

    private void cmbShapeTypes_actionPerformed(ActionEvent e) {
        this.setLeftLabelText(String.format(this.GEOMETRY_FILTER_FORMAT,this.cmbShapeTypes.getSelectedItem().toString()));
    }

    private void btnPRJ_actionPerformed(ActionEvent e) {
        
        JPanel contents = new JPanel(new VerticalFlowLayout());
        JTextArea prjCoordSys = new JTextArea(this.EXPORT_TYPE==Constants.EXPORT_TYPE.SHP
                                              ? this.geoRaptorPreferences.getPrj()
                                              : this.geoRaptorPreferences.getCoordsys());
        prjCoordSys.setPreferredSize(new Dimension(200,200));
        prjCoordSys.setLineWrap(true);
        prjCoordSys.setAlignmentX(Component.LEFT_ALIGNMENT);
        contents.add(prjCoordSys);
        JLabel lblSpatialReferenceOrg = new JLabel("See: " + ( this.EXPORT_TYPE==Constants.EXPORT_TYPE.SHP
                                                               ? "www.spatialreference.org"
                                                               : "<MapInfo installation path>\\MAPINFOW.prj"));
        lblSpatialReferenceOrg.setAlignmentX(Component.LEFT_ALIGNMENT);
        contents.add(lblSpatialReferenceOrg);
        
        Object[] options = { "OK", "CANCEL" }; 
                           // { propertyManager.getMsg("MD_DELETE_MB_YES_BUTTON"), propertyManager.getMsg("MD_DELETE_MB_NO_BUTTON") };
        int returnStatus = JOptionPane.showOptionDialog( this,
                                                         contents, 
                                                         Constants.GEORAPTOR + " - " + String.format("Enter %s string",this.EXPORT_TYPE==Constants.EXPORT_TYPE.SHP?"PRJ":"Coordsys"), 
                                                         JOptionPane.OK_CANCEL_OPTION,
                                                         JOptionPane.PLAIN_MESSAGE,
                                                         null,
                                                         options,
                                                         options[0]);
        this.setAlwaysOnTop(true);
        if (returnStatus == 0) {
            if ( this.EXPORT_TYPE==Constants.EXPORT_TYPE.SHP )
                this.geoRaptorPreferences.setPrj(prjCoordSys.getText());
            else
                this.geoRaptorPreferences.setCoordsys(prjCoordSys.getText());
        }
    }
    
}
