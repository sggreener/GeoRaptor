package org.GeoRaptor.io.Export.ui;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.GeoRaptor.Constants;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.io.Export.ExporterWriter;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.wizard.Wizard;
import org.GeoRaptor.tools.wizard.WizardPanelDescriptor;


public class ExporterWizard {
    
    private Wizard                                wizard = null;
    private int                          finalReturnCode = -1;
    protected ExporterWriter exporterWriter = null;
    protected WizardPanelDescriptor  firstPageDescriptor = null;
    protected WizardPanelDescriptor secondPageDescriptor = null;
    protected WizardPanelDescriptor  thirdPageDescriptor = null;
    protected List<String>                    geoColumns = null;
    
    public ExporterWizard(String     _wizardTitle,
                          Connection _conn,
                          String     _schemaName,
                          String     _objectName,
                          String     _columnName) 
    throws IllegalArgumentException, 
           SQLException
    {
        super();
        this.wizard = new Wizard();
        this.wizard.getDialog().setTitle(_wizardTitle);
        String geometryColumn = checkGeometryColumn(_conn,
                                                    _schemaName,
                                                    _objectName,
                                                    _columnName);
        this.exporterWriter = new ExporterWriter(_conn,
                                                 _schemaName,
                                                 _objectName,
                                                 geometryColumn);
        this.exporterWriter.setAttributeColumns(MetadataTool.getColumnsAndTypes(this.exporterWriter.getConn(),
                                                  this.exporterWriter.getSchemaName(),
                                                  this.exporterWriter.getObjectName(),
                                                  true,
                                                  true));
    }
    
    public String checkGeometryColumn(Connection _conn,
                                       String     _schemaName,
                                       String     _objectName,
                                       String     _columnName) 
    throws IllegalArgumentException
    {
        // Let user select from the table's geometry columns
        //
        geoColumns = new ArrayList<String>(MetadataTool.validateColumnName(_conn,
                                                                           _schemaName,
                                                                           _objectName,
                                                                           _columnName));
        // Does it have any?
        //
        if ( geoColumns != null || geoColumns.size()!=0 ) {
            if (Strings.isEmpty(_columnName) ) {
                // Just get first
                return geoColumns.get(0);
            } else {
                if (geoColumns.indexOf(_columnName) != -1 )
                    return _columnName;
            }
        } 
        // Table doesn't have a GEOMETRY column....
        //
        String sName = Strings.append(_schemaName,_objectName,Constants.TABLE_COLUMN_SEPARATOR);
        throw new IllegalArgumentException(Strings.isEmpty(_columnName) 
                      ? "Selected object " + sName +  " must have SDO_GEOMETRY column"
                      : _columnName + " is not an SDO_GEOMETRY object or it does not exist in " + sName );
    }
    
    public int getReturnCode() {
      return this.finalReturnCode; 
    }
    
    public boolean initialise() 
     throws SQLException,
            IllegalArgumentException 
     {
          boolean status = createPage1();
          if ( ! status )
              return status;
          status = createPage2();
           if ( ! status )
               return status;
           status = createPage3();
            if ( ! status )
                return status;
          setCurrent((String)firstPageDescriptor.getPanelDescriptorIdentifier());
          return true;
     }
    
    private boolean createPage1() {
         firstPageDescriptor = new ExportPanel1Descriptor();
         add(firstPageDescriptor);
         firstPageDescriptor.setWriterObject(this.exporterWriter);

        ((ExportPanel1)firstPageDescriptor.getPanelComponent())
                                          .setGeometryColumns(
                                        		  new javax.swing.DefaultComboBoxModel<String>(new Vector<String>(geoColumns)));
        return true;
     }

    private boolean createPage2() 
    {
         secondPageDescriptor = new ExportPanel2Descriptor();
         add(secondPageDescriptor);
         secondPageDescriptor.setWriterObject(this.exporterWriter);
         return true;
    }

    private boolean createPage3() 
    {
         thirdPageDescriptor = new ExportPanel3Descriptor();
         add(thirdPageDescriptor);
         thirdPageDescriptor.setWriterObject(this.exporterWriter);         
         return true;
    }
        
    public void show() {
        this.finalReturnCode = wizard.showModalDialog();
        if ( this.finalReturnCode == 0 )
            writeToFile();
    }
    
    private void writeToFile() {
        exporterWriter.Export();
    }
    
    public void add(WizardPanelDescriptor _descriptor) {
        wizard.registerWizardPanel((String)_descriptor.getPanelDescriptorIdentifier(), _descriptor);
    }

    public void setCurrent(String _identifier) {
        wizard.setCurrentPanel(_identifier);
    }
    
}
