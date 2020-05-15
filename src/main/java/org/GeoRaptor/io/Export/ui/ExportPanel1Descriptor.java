package org.GeoRaptor.io.Export.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.io.Export.ExporterWriter;
import org.GeoRaptor.tools.FileUtils;
import org.GeoRaptor.tools.wizard.WizardPanelDescriptor;


public class ExportPanel1Descriptor 
     extends WizardPanelDescriptor 
  implements ActionListener,
             ListSelectionListener
{
    /**
     * Get reference to single instance of GeoRaptor's Preferences
     **/
    protected Preferences geoRaptorPreferences;
    
    public static final String IDENTIFIER = "Geometry and Export Type";
    
    protected ExportPanel1 ePanel1 = null;
    protected ExporterWriter exporterWriter = null;
  
    public ExportPanel1Descriptor() {
        ePanel1 = new ExportPanel1();
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(ePanel1);
        ePanel1.addAttributeActionListener(this);
        ePanel1.addListSelectionListener(this);
    }

    public void aboutToDisplayPanel() {
        if ( this.exporterWriter != null ) {
            ePanel1.setObjectName(this.exporterWriter.getObjectName());
            ePanel1.setSchemaName(this.exporterWriter.getSchemaName());
            ePanel1.setAttributes(this.exporterWriter.isAttributes());
            Constants.EXPORT_TYPE eType = this.geoRaptorPreferences.getExportType();
            for (int i = 0; i < ePanel1.lstExportType.getModel().getSize(); i++) {
                   Constants.EXPORT_TYPE item = Constants.EXPORT_TYPE.valueOf(ePanel1.lstExportType.getModel().getElementAt(i).toString());
                    if ( item.compareTo(eType)==0) {
                        ePanel1.lstExportType.setSelectedIndex(i);
                        break;
                    }
            }
            if ( ! this.exporterWriter.hasAttributeColumns() )
              ePanel1.setNoAttributes();
        }
    }

    public void aboutToHidePanel() {
        this.exporterWriter.setExportType(ePanel1.getExportType());
        this.exporterWriter.setExportFileName(String.format("%s%s_%s.%s",
                                                            this.geoRaptorPreferences.getExportDirectory(),
                                                            this.exporterWriter.getObjectName(),
                                                            this.exporterWriter.getColumnName(),
                                                            this.exporterWriter.getExportType().toString().toLowerCase()));
        this.exporterWriter.setExportBaseName(FileUtils.getFileNameFromPath(this.exporterWriter.getExportFileName(),true));
    }

    @Override
    public void setWriterObject(Object _writerObject) {
        if ( _writerObject instanceof ExporterWriter) 
            this.exporterWriter = (ExporterWriter) _writerObject;
    }

    @Override
    public Object getWriterObject() {
      return this.exporterWriter;
    }
    
    public Object getNextPanelDescriptor() {
      if (this.ePanel1.isAttributesSelected())
          return ExportPanel2Descriptor.IDENTIFIER;
      else
          return ExportPanel3Descriptor.IDENTIFIER;

    }
    
    public Object getBackPanelDescriptor() {
        return null;
    }  
    
    public WizardPanelDescriptor getPreviousDescriptor() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        if (ePanel1.cbAttributes.getActionCommand().equalsIgnoreCase(e.getActionCommand())) {
            if ( this.exporterWriter.hasAttributeColumns() )
                this.exporterWriter.setAttributes(ePanel1.cbAttributes.isSelected());
        } else if ( ePanel1.cmbGeometries.getActionCommand().equals(e.getActionCommand()) ) {
            this.exporterWriter.setColumnName(ePanel1.cmbGeometries.getSelectedItem().toString());
        }
    }

    public void valueChanged(ListSelectionEvent evt) {
        if ( evt.getSource() instanceof JList ) {
            JList<?> list = (javax.swing.JList<?>)evt.getSource();
            if ( ePanel1.lstExportType.getName().equals(list.getName()) ) {
                if ( ePanel1.lstExportType.getSelectedIndex() == -1 ) 
                    ePanel1.lstExportType.setSelectedIndex(0);
                this.exporterWriter.setExportType(ePanel1.lstExportType.getSelectedValue().toString());
                this.geoRaptorPreferences.setExportType(ePanel1.getExportType());
            }
        }
    }
}
