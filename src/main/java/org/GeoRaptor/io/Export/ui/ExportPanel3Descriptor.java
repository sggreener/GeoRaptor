package org.GeoRaptor.io.Export.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.OracleSpatial.SRID.SRIDPanel;
import org.GeoRaptor.io.ExtensionFileFilter;
import org.GeoRaptor.io.Export.ExporterWriter;
import org.GeoRaptor.tools.FileUtils;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.wizard.WizardPanelDescriptor;
import org.geotools.util.logging.Logger;

import oracle.jdbc.OracleConnection;


public class ExportPanel3Descriptor 
     extends WizardPanelDescriptor 
  implements ActionListener, FocusListener, ChangeListener {

    @SuppressWarnings("unused")
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.ui.ExportPanel3Descriptor");
    
    public static final String IDENTIFIER = "Export Options";
    private String          SRID_NOT_NULL = "To proceed SRID must not be NULL";
    private String           SRID_INTEGER  = "SRID must be an integer";
    private String  SRID_GOOGLE_COMPLIANT = "SRID must be Google Maps compliant";

    /**
     * For access to preferences
     */
    protected Preferences geoRaptorPreferences;
  
    protected ExporterWriter exporterWriter = null;
    protected ExportPanel3                ePanel3 = null;
    protected WizardPanelDescriptor previousPanel = null;
    private   boolean       isAboutToDisplayPanel = true;

    /**
     * Database connection
     */
    protected Connection conn;
  
    /**
     * Name of:
     * 1. schema owning object
     * 2. objectName containing index
     * 3. columnName actual SDO_GEOMETRY column
     */
    protected String schemaName;
    protected String objectName;
    protected String columnName;
    
    protected String xmlFileName = "";
    
    public ExportPanel3Descriptor() {
        this.ePanel3 = new ExportPanel3();
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(ePanel3);
        this.ePanel3.addActionListeners(this);
        this.ePanel3.addFocusListeners(this);
        this.ePanel3.addChangeListeners(this);
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
    }

    public ExportPanel3Descriptor(Object object, Component component) {
        super(object, component);
    }

    public void focusGained(FocusEvent evt) {
    }

    public void stateChanged(ChangeEvent evt) {
        if ( evt.getSource() instanceof javax.swing.JSlider ) {
            JSlider sldr = (javax.swing.JSlider)evt.getSource();
            if ( this.ePanel3.sldrCommitInterval.getName().equals(sldr.getName()) ) {
                if ( sldr.getValue()==0 ) {
                    sldr.setValue(10);
                }
                this.ePanel3.setCommitLabel(sldr.getValue());
                this.exporterWriter.setCommit(sldr.getValue());
            }
        }
    }

    public void focusLost(FocusEvent evt) {
      if ( isAboutToDisplayPanel )
          return;
      if ( evt.getSource() instanceof javax.swing.JTextField ) {
        javax.swing.JTextField jTF = (javax.swing.JTextField)evt.getSource();
        if ( ePanel3.tfFilename.getName().equals(jTF.getName()) ) {
            ePanel3.tfEntity.setText(FileUtils.getFileNameFromPath(ePanel3.tfFilename.getText(),true));
            this.exporterWriter.setExportBaseName(ePanel3.getEntityName());
            this.exporterWriter.setExportFileName(ePanel3.getFilename());
            this.geoRaptorPreferences.setExportDirectory(FileUtils.getDirectory(ePanel3.getFilename()));
        } else if ( ePanel3.tfSRID.getName().equals(jTF.getName()) ) {
            if ( ePanel3.tfSRID.getText().equalsIgnoreCase(Constants.NULL) &&
                ( this.exporterWriter.getExportType()==Constants.EXPORT_TYPE.GML || 
                  this.exporterWriter.getExportType()==Constants.EXPORT_TYPE.KML )  ){
                ePanel3.setStatusText(this.SRID_NOT_NULL);
                return;
            } else {
                try {
                    Integer.valueOf(ePanel3.tfSRID.getText() );
                    ePanel3.setStatusText(this.exporterWriter.getExportType()==Constants.EXPORT_TYPE.KML
                                          ?this.SRID_GOOGLE_COMPLIANT
                                          :null);
                } catch (NumberFormatException nfe) {
                    ePanel3.setStatusText(this.SRID_INTEGER);
                }
            }
            this.exporterWriter.setSRID(ePanel3.getSRID());
            this.exporterWriter.setShapefileType(ePanel3.getShapefileType());
        }
      }
    }
    
    public void actionPerformed(ActionEvent e) {
        if ( isAboutToDisplayPanel ) {
            return;
        }
               if ( this.ePanel3.btnSRID.getActionCommand().equals(e.getActionCommand()) ) {
            processBtnSRID();
        } else if ( this.ePanel3.btnFilename.getActionCommand().equals(e.getActionCommand()) ) {
            processBtnFilename(); 
        } else if ( this.ePanel3.cbSkipNullRow.getActionCommand().equals(e.getActionCommand()) ) {
            this.exporterWriter.setSkipNullGeometry(this.ePanel3.isSkipNullGeometry());
        } else if ( this.ePanel3.cmbCharset.getActionCommand().equals(e.getActionCommand()) ) {
            this.exporterWriter.setCharacterSet(this.ePanel3.getCharset());
        } else if ( this.ePanel3.cmbShapeTypes.getActionCommand().equals(e.getActionCommand()) ) {
            this.exporterWriter.setShapefileType( this.ePanel3.getShapefileType() );
        }
    }
  
    private void processBtnSRID() {
        // Allows user to select SRID from SRIDPanel class
        String srid = String.valueOf(this.ePanel3.getSRID());
        SRIDPanel sp = SRIDPanel.getInstance();
        boolean status = sp.initialise((OracleConnection)this.conn,srid);
        if (status == true) {
            sp.setLocationRelativeTo(this.ePanel3);
            sp.setVisible(true);
            if ( ! sp.formCancelled() ) {
                this.ePanel3.setSRID(sp.getSRID());
                this.exporterWriter.setSRID(sp.getSRID());
            }
        }
    }

    private void processBtnFilename() {
      final String extractName = FileUtils.getFileNameFromPath(ePanel3.tfFilename.getText(), false);
      File f = new File(ePanel3.tfFilename.getText());
      f = new File(f.getParent());
      // Set the current directory
      try {
          f = new File(f.getCanonicalPath());
      } catch (IOException g) {
          f = new File(f.getParent());
      }
      ExtensionFileFilter extFilter = new ExtensionFileFilter(exporterWriter.getExportType().toString().toUpperCase() + " Files", 
                                               new String[] { exporterWriter.getExportType().toString().toUpperCase() });
      JFileChooser fc = new JFileChooser(f);
      fc.setFileFilter(extFilter);
      fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      int returnVal = fc.showOpenDialog(ePanel3);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
          final File file = fc.getSelectedFile();
          SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                      String fileName = file.getAbsolutePath();
                      if ( !Strings.isEmpty(fileName) ) {
                          ePanel3.tfFilename.setText(fileName);
                          // Check has file extension
                          if ( ! fileName.toLowerCase(Locale.getDefault()).endsWith(exporterWriter.getExportType().toString().toLowerCase(Locale.getDefault())) ) {
                              ePanel3.tfFilename.setText(fileName + "." + exporterWriter.getExportType().toString().toLowerCase(Locale.getDefault()));
                          } 
                      } else {
                          ePanel3.tfFilename.setText(extractName);
                      }
                      ePanel3.tfEntity.setText(FileUtils.getFileNameFromPath(ePanel3.tfFilename.getText(),true));
                      exporterWriter.setExportBaseName(ePanel3.getEntityName());
                      exporterWriter.setExportFileName(ePanel3.getFilename());
                      geoRaptorPreferences.setExportDirectory(FileUtils.getDirectory(ePanel3.getFilename()));
                  }
              });
      } // if
    }
  
    @Override
    public void setWriterObject(Object _writerObject) {
        if ( _writerObject instanceof ExporterWriter)
            this.exporterWriter = (ExporterWriter) _writerObject;
        this.ePanel3.setExportType(this.exporterWriter.getExportType());
        this.ePanel3.setConn(exporterWriter.getConn());
    }
  
    public void aboutToDisplayPanel() {
        isAboutToDisplayPanel = true;
        if ( this.exporterWriter != null ) {
          this.conn       = this.exporterWriter.getConn();
          this.schemaName = this.exporterWriter.getSchemaName();
          this.objectName = this.exporterWriter.getObjectName();
          this.columnName = this.exporterWriter.getColumnName();
          
          this.ePanel3.setExportType(this.exporterWriter.getExportType());

          // Set:
          // 1. base name
          // 2. Filename (constructed in Entity2
          // 3. Whether to skip rows with null SRID;
          // 4. Charset;
          // 5. SRID if SRID is NULL;
          // 6. ShapeTypes
          //
          ePanel3.tfFilename.setText(this.exporterWriter.getExportFileName());
          ePanel3.tfEntity.setText(FileUtils.getFileNameFromPath(ePanel3.tfFilename.getText(),true));
          ePanel3.setSkipNullGeometry(true); this.exporterWriter.setSkipNullGeometry(ePanel3.isSkipNullGeometry());
          String characterSet = null;
          try {
              characterSet = MetadataTool.getCharacterSet(this.conn);
          } catch (SQLException e) {
              characterSet = null;
          }
          ePanel3.setCharset(characterSet); this.exporterWriter.setCharacterSet(ePanel3.getCharset());
          ePanel3.setSRID(retrieveSRID());  this.exporterWriter.setSRID(ePanel3.getSRID());
          ePanel3.setShapeTypes(MetadataTool.getShapeType(this.conn,this.schemaName,this.objectName,this.columnName, 0, 500));
          this.exporterWriter.setShapefileType(ePanel3.getShapefileType());
        }
        isAboutToDisplayPanel = false;
    }

    @Override
    public void aboutToHidePanel() {
        writeChanges();
    }

    public void writeChanges(){
        this.exporterWriter.setCharacterSet(ePanel3.getCharset());
        this.exporterWriter.setSRID(ePanel3.getSRID());
        this.exporterWriter.setSkipNullGeometry(ePanel3.isSkipNullGeometry());
        this.exporterWriter.setExportBaseName(ePanel3.getEntityName());
        this.exporterWriter.setExportFileName(ePanel3.getFilename());
        this.exporterWriter.setCommit(ePanel3.getCommit());
        this.exporterWriter.setShapefileType( this.ePanel3.getShapefileType() );
        this.geoRaptorPreferences.setExportDirectory(FileUtils.getDirectory(ePanel3.getFilename()));
    }
    
    private String retrieveSRID() {
      try {
          int SRID = Constants.SRID_NULL;
          LinkedHashMap<String, MetadataEntry> metaEntries;
          metaEntries = MetadataTool.getMetadata(this.conn,
                                                 this.schemaName,
                                                 this.objectName,
                                                 this.columnName,
                                                 false);
          // We only ever load the first one (there should only be one!)
          //
          if ( metaEntries.size()==0 ) {
              // No entry so query actual table
              //
              SRID = MetadataTool.getLayerSRID(this.exporterWriter.getConn(),
                                               this.exporterWriter.getSchemaName(),
                                               this.exporterWriter.getObjectName(),
                                               this.exporterWriter.getColumnName());
              if ( SRID <= Constants.SRID_NULL ) // No records in table
                  return "NULL";
              else
                  return String.valueOf(SRID);
          } else { 
              MetadataEntry me = metaEntries.get(Strings.objectString(this.schemaName,this.objectName,this.columnName));
              return (me==null) ? "NULL" : me.getSRID();
          }
      } catch (SQLException sqle) {
          ePanel3.setStatusText("SQL error detected when retrieving SRID (" + sqle.getMessage() +")");
      } catch (IllegalArgumentException iae) {
          ePanel3.setStatusText("Illegal Argument detected when retriving SRID (" + iae.getMessage() +")");
      } 
      return "NULL";
    }
  
    public Object getBackPanelDescriptor() {
        if ( this.exporterWriter.isAttributes() )
            return ExportPanel2Descriptor.IDENTIFIER;
        else
            return ExportPanel1Descriptor.IDENTIFIER;
    }
    
    public Object getNextPanelDescriptor() {
        return FINISH;
    }

}
