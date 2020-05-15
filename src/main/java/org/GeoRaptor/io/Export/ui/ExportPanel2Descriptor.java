package org.GeoRaptor.io.Export.ui;

import java.awt.Component;

import org.GeoRaptor.io.Export.ExporterWriter;
import org.GeoRaptor.tools.wizard.WizardPanelDescriptor;


public class ExportPanel2Descriptor extends WizardPanelDescriptor {
    
    public static final String IDENTIFIER = "Attribute Selection";
  
    protected ExportPanel2 ePanel2 = null;
    protected WizardPanelDescriptor previousPanel = null;

    public ExportPanel2Descriptor() {
        ePanel2 = new ExportPanel2();
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(ePanel2);
    }
  
    public ExportPanel2Descriptor(Object object, Component component) {
        super(object, component);
    }
   
    protected ExporterWriter exporterWriter = null;

    @Override
    public void setWriterObject(Object _writerObject) {
        if ( _writerObject instanceof ExporterWriter) 
            this.exporterWriter = (ExporterWriter) _writerObject;
    }

    public void aboutToDisplayPanel() {
        if ( this.exporterWriter != null ) {
            // These two RBs should only be activated when XML output is GML
            ePanel2.setExportType(this.exporterWriter.getExportType());
            ePanel2.loadAttributes(this.exporterWriter.getAttributeColumns(), 
                                   this.exporterWriter.getColumnName());
        }
    }

    public void aboutToHidePanel() {
        this.exporterWriter.setAttributeFlavour(ePanel2.getAttributeFlavour());
        this.exporterWriter.setColumns(this.ePanel2.getExportableColumns());
        this.exporterWriter.setAttributes(this.ePanel2.isAttributeSelected());
    }
    
    public void displayingPanel() {
      ePanel2.setColumnWidths();
    }
  
    public Object getNextPanelDescriptor() {
        return ExportPanel3Descriptor.IDENTIFIER;
    }
    
    public Object getBackPanelDescriptor() {
        return ExportPanel1Descriptor.IDENTIFIER;
    }  
}
