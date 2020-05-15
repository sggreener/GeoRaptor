package org.GeoRaptor.io.Export;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.sql.RowSetMetaData;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleStatement;

import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.sql.OraRowSetMetaDataImpl;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.GeometryProperties;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.util.logging.Logger;


@SuppressWarnings("deprecation")
public class ExporterWriter 
     extends JFrame 
  implements ActionListener
{
	private static final long serialVersionUID = -8552639848052825310L;

	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.ExportWriter");

      /**
       * For access to preferences
       */
      protected Preferences geoRaptorPreferences;
      
      /** 
       * Properties File Manager
       **/
      private static final String propertiesFile = "org.GeoRaptor.io.Export.ui.export";
      protected PropertiesManager propertyManager;

      /** UI for when work is actually done
       */
      private JPanel          pnlStatus = new JPanel();
      private XYLayout        xYLayout2 = new XYLayout();
      private FlowLayout    flowLayout1 = new FlowLayout();
      private JLabel        lblProgress = new JLabel("Exporting...");
      private JProgressBar  progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
      private JLabel          lblStatus = new JLabel("Status...");
      private JButton      cancelButton = new JButton();
      private ExportTask     exportTask = null;
      private java.util.Date  startTime = null;
    
      private Connection           conn = null;
      private String         schemaName = "";
      private String         objectName = "";
      private String         columnName = "";
      private String            columns = "";
      private String     exportFileName = "";
      private String     exportBaseName = "";
      private Constants.EXPORT_TYPE
                             exportType = Constants.EXPORT_TYPE.GML;
      private String   attributeFlavour = "";
      private String       characterSet = "UTF-8";
      private int                  SRID = Constants.SRID_NULL;
      private boolean        attributes = false; 
      private boolean  skipNullGeometry = true;
      private ShapeType  shapefileType = ShapeType.UNDEFINED; 

      private String        errorMessage = "";
      private int           rowsExported = 0;
      private int                 commit = 100;
      protected LinkedHashMap<String, String> attrColumns = null;

      public ExporterWriter() {
          super();
          this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
          this.propertyManager = new PropertiesManager(ExporterWriter.propertiesFile);
      }
      
      public ExporterWriter(Connection _conn,
                            String     _schemaName,
                            String     _objectName,
                            String     _columnName) {
          this();
          this.setConn(_conn);
          this.setSchemaName(_schemaName);
          this.setObjectName(_objectName);
          this.setExportBaseName(_objectName);
          this.setColumnName(_columnName);
          this.setSRID();
          this.setExportFileName(this.geoRaptorPreferences.getExportDirectory());
      }

      private void writeXSD(OracleResultSet _rSet) 
      {
          if (Strings.isEmpty(this.getExportFileName()) || Strings.isEmpty(this.getExportBaseName()) )
              return;
          try {
              String xsdFileName = this.getExportFileName().replace("."+this.exportType.toString().toLowerCase(),".xsd");
              BufferedWriter xsdFile = null;
              String newLine = System.getProperty("line.separator");
              
              String xmlns_xs  = "  xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"";
              String xmlns_gml = "  xmlns:gml=\"http://www.opengis.net/gml\"";
              String import_gml = "<xs:import namespace=\"http://www.opengis.net/gml\" schemaLocation=\"http://schemas.opengis.net/gml/3.1.1/base/gml.xsd\"/>";
              String xmlns_flavour = this.getAttributeFlavour().equalsIgnoreCase(Constants.XMLAttributeFlavour.FME.toString())==false
                                     ? "  xmlns:ogr=\"http://ogr.maptools.org/\""
                                     : "  xmlns:fme=\"http://www.safe.com/gml/fme\"";
              String targetNamespace_flavour = this.getAttributeFlavour().equalsIgnoreCase(Constants.XMLAttributeFlavour.FME.toString())==false 
                                               ? "  targetNamespace=\"http://ogr.maptools.org/\""
                                               : "  targetNamespace=\"http://www.safe.com/gml/fme\"";
    
              xsdFile = new BufferedWriter(new FileWriter(xsdFileName)); 
              xsdFile.write("<?xml version='1.0'  encoding='" + this.getCharacterSet() + "' ?>" + newLine);
              xsdFile.write("<xs:schema " + newLine +
                            xmlns_xs + newLine +
                            xmlns_gml + newLine +
                            xmlns_flavour  + newLine +
                            targetNamespace_flavour + newLine + "  elementFormDefault=\"qualified\" version=\"1.0\">" + newLine +
                            import_gml + newLine);
              if ( this.getAttributeFlavour().equalsIgnoreCase(Constants.XMLAttributeFlavour.OGR.toString()) ) {
                  // Feature collection type is entirely OGR
                  //
                  xsdFile.write("<xs:element name=\"FeatureCollection\" type=\"ogr:FeatureCollectionType\" substitutionGroup=\"gml:_FeatureCollection\"/>" + newLine +
                                " <xs:complexType name=\"FeatureCollectionType\">" + newLine +
                                "  <xs:complexContent>" + newLine +
                                "    <xs:extension base=\"gml:AbstractFeatureCollectionType\">" + newLine +
                                "      <xs:attribute name=\"lockId\" type=\"xs:string\" use=\"optional\"/>" + newLine +
                                "      <xs:attribute name=\"scope\" type=\"xs:string\" use=\"optional\"/>" + newLine +
                                "    </xs:extension>" + newLine +
                                "  </xs:complexContent>" + newLine +
                                " </xs:complexType>" + newLine);                              
              }
              // Everything else is common
              //
              xsdFile.write(   "<xs:element name=\"" + this.exportBaseName + 
                               "\" type=\"" + this.getAttributeFlavour().toString().toLowerCase() + ":" + this.exportBaseName + "Type\" substitutionGroup=\"gml:_Feature\"/>" + newLine +
                               " <xs:complexType name=\"" + this.exportBaseName + "Type\">" + newLine + 
                                "  <xs:complexContent>" + newLine + 
                                "    <xs:extension base=\"gml:AbstractFeatureType\">" + newLine + 
                                "      <xs:sequence>" + newLine);
              
              // Write common attribute XSD elements
              //
              String columnName = "";
              OracleResultSetMetaData meta = (OracleResultSetMetaData)_rSet.getMetaData();
              for (int col = 1; col < meta.getColumnCount(); col++) 
              {
                  columnName = meta.getColumnName(col).replace("\"","");
                  if ( columnName.equalsIgnoreCase(this.columnName) ) {
                      xsdFile.write("    <xs:element name=\"geometryProperty\" type=\"gml:GeometryPropertyType\"" +
                                                 "   nillable=\""  + (meta.isNullable(col)==OracleResultSetMetaData.columnNullable) +
                                                 "\" minOccurs=\"" + (meta.isNullable(col)==OracleResultSetMetaData.columnNullable?"0":"1") +
                                                 "\" maxOccurs=\"1\"/>" + newLine);
                  } else {
                      String xsdDataType = "";
                      for (int rCol=1;rCol<=meta.getColumnCount();rCol++) {
                        // Is a supported column?
                        if ( meta.getColumnLabel(rCol).equalsIgnoreCase(columnName) ) {
                            if ( Tools.isSupportedType(meta.getColumnType(rCol),meta.getColumnTypeName(rCol))==false) 
                                continue;
                            xsdDataType = Tools.dataTypeToXSD(meta,rCol);
                            xsdFile.write("    <xs:element name=\"" + columnName + 
                                                        "\" nillable=\""  + (meta.isNullable(rCol)==OracleResultSetMetaData.columnNullable) +
                                                        "\" minOccurs=\"" + (meta.isNullable(rCol)==OracleResultSetMetaData.columnNullable?"0":"1") + 
                                                        "\" maxOccurs=\"1\">" + newLine + 
                                          "      <xs:simpleType>" + newLine +
                                          "        <xs:restriction base=\"xs:");
                            
                            int prec = meta.getPrecision(rCol);
                            int scale = meta.getScale(rCol);
                            if ( prec <= 0 ) {
                                prec = meta.getColumnDisplaySize(rCol);
                                scale = 0;
                            }
                            if ( xsdDataType.equalsIgnoreCase("string") ) {
                                xsdFile.write(xsdDataType + "\">" + newLine + 
                                              "          <xs:maxLength value=\"" + prec + "\" fixed=\"false\"/>" + newLine +
                                              "        </xs:restriction>" + newLine);
                            } else if ( xsdDataType.equalsIgnoreCase("clob") ) {
                                xsdFile.write("string" + "\">" + newLine + 
                                              "          <xs:minLength value=\"1\"/>" + newLine +
                                              "        </xs:restriction>" + newLine);
                            } else if ( xsdDataType.equalsIgnoreCase("float") || 
                                        xsdDataType.equalsIgnoreCase("double") ||
                                        xsdDataType.equalsIgnoreCase("date")   || 
                                        xsdDataType.equalsIgnoreCase("time")   || 
                                        xsdDataType.equalsIgnoreCase("dateTime") ) {
                                xsdFile.write(xsdDataType + "\"/>" + newLine);
                            } else {
                                xsdFile.write(xsdDataType + "\">" + newLine + 
                                                   "               <xs:totalDigits value=\"" + prec + "\"/>" + newLine +
                                  (scale==0 ? "" : "               <xs:fractionDigits value=\"" + scale + "\"/>" + newLine ) +
                                                   "        </xs:restriction>" + newLine );
                            }
                            xsdFile.write( 
                              "      </xs:simpleType>" + newLine + 
                              "    </xs:element>" + newLine);
                            break;
                        }
                    }
                  }
              }            
              xsdFile.write("      </xs:sequence>" + newLine +
                            "    </xs:extension>" + newLine +
                            "  </xs:complexContent>" + newLine +
                            " </xs:complexType>" + newLine +
                            "</xs:schema>");
              xsdFile.flush();
              xsdFile.close();
        } catch (IOException e) {
            LOGGER.error("IOException in ExporterWriter.writeXSD() " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.error("SQLException in ExporterWriter.writeXSD() " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception in ExporterWriter.writeXSD() " + e.getMessage());
        }
      }

      public Connection getConn() {
          return this.conn;
      }
  
      public void setConn(Connection conn) {
          this.conn = conn;
      }
  
      public String getSchemaName() {
          return this.schemaName;
      }
  
      public void setSchemaName(String _schemaName) {
          this.schemaName = _schemaName;
      }
  
      public String getObjectName() {
          return this.objectName;
      }
  
      public void setObjectName(String _objectName) {
          this.objectName = _objectName;
      }
  
      public String getColumnName() {
          return this.columnName;
      }
  
      public void setColumnName(String _columnName) {
          this.columnName = _columnName;
      }
  
      public String getColumns() {
          return this.columns;
      }
  
      public void setColumns(String columns) {
          this.columns = columns;
      }
  
      public String getExportFileName() {
          return this.exportFileName;
      }
  
      public void setExportFileName(String exportFileName) {
          this.exportFileName = exportFileName;
      };
  
      public String getExportBaseName() {
          return this.exportBaseName;
      }
  
      public void setExportBaseName(String exportBaseName) {
          this.exportBaseName = exportBaseName;
      }
      
      public void setCommit(int _value) {
          this.commit = _value;
      }
      
      public int getCommit() {
          return this.commit;
      }
      
      public Constants.EXPORT_TYPE getExportType() {
          return this.exportType;
      }
      
      public void setExportType(Constants.EXPORT_TYPE exportType) {
          this.exportType = exportType;
      }
      
      public void setExportType(String _exportType) {
          try {
              this.exportType = Constants.EXPORT_TYPE.valueOf(_exportType.toUpperCase());
          } catch (Exception e) {
             this.exportType = Constants.EXPORT_TYPE.GML;
          }
      }
  
      public String getAttributeFlavour() {
          return this.attributeFlavour;
      }
      
      public void setAttributeFlavour(String _attributeFlavour) {
          this.attributeFlavour = _attributeFlavour;
      }
  
      public String getCharacterSet() {
          return this.characterSet;
      }
      public void setCharacterSet(String characterSet) {
          this.characterSet = characterSet;
      }
  
      public void setShapefileType(ShapeType _shapeType) {
          if ( _shapeType == null ) {
              return;
          }
          this.shapefileType = _shapeType;
      }
      public ShapeType getShapefileType() {
          return this.shapefileType;
      }

      public int getSRID() {
          return this.SRID;
      }
      
      public void setSRID() {
        this.SRID = Constants.NULL_SRID;
        try {
            SRID = MetadataTool.getLayerSRID(this.getConn(), 
                                             this.getSchemaName(), 
                                             this.getObjectName(),
                                             this.getColumnName());
        } catch (SQLException e) {
            LOGGER.warn("Exporter Writer could not retrieve SRID of " + 
                        this.getSchemaName()+"."+this.getObjectName()+"."+this.getColumnName() +
                        ": " + e.getMessage());
        }
      }
      public void setSRID(int SRID) {
          this.SRID = SRID;
      }
      public void setSRID(String _SRID) {
          if (Strings.isEmpty(_SRID) )
              return;
          if ( _SRID.equalsIgnoreCase(Constants.NULL) )
              return;
          try {
            this.setSRID(Integer.valueOf(_SRID));
          } catch (Exception e) {
              return;
          }
      }
  
      public boolean isSkipNullGeometry() {
          return this.skipNullGeometry;
      }
      public void setSkipNullGeometry(boolean skipNullGeometry) {
          this.skipNullGeometry = skipNullGeometry;
      }
    
      public LinkedHashMap<String,String> getAttributeColumns() {
          return this.attrColumns;
      }
      public boolean hasAttributeColumns() {
          String geoColumn = "\""+this.columnName.replace("\"","")+"\"";
          if ( this.attrColumns == null || this.attrColumns.size()==0 )
              return false;
          else if ( this.attrColumns.size()==1 && !Strings.isEmpty(this.attrColumns.get(geoColumn)) )
              return false;
          else
              return this.attrColumns.size()!=0;
      }
      
      public void setAttributeColumns(LinkedHashMap<String,String> _columns) {
          if ( _columns == null || Strings.isEmpty(this.columnName)) return;
          this.attrColumns = _columns;
          this.setAttributes(this.hasAttributeColumns());
      }
      
      public boolean isAttributes() {
          return this.attributes;
      }
      public void setAttributes(boolean _attributes) {
          this.attributes = _attributes;
      }
  
      public void setErrorMessage(String errorMessage) {
          this.errorMessage = errorMessage;
      }
      public String getErrorMessage() {
          return this.errorMessage;
      }
      
      public int getTotalRowCount() {
        return this.rowsExported;
      }
  
      public void actionPerformed(ActionEvent e) {
          if (cancelButton.getActionCommand().equals(e.getActionCommand())) {
              exportTask.cancel(true);
              this.setVisible(false);
              exportTask = null;
          }
      }

      public void Export ()
      {
          this.getContentPane().setLayout(flowLayout1);
          this.setLocationRelativeTo(null);
          this.setSize(new Dimension(600, 159));
          
          pnlStatus.setLayout(xYLayout2);
          pnlStatus.setSize(new Dimension(590, 130));
          pnlStatus.setPreferredSize(new Dimension(595, 130));
          
          lblProgress.setText("Exporting ");
          lblStatus.setText("");

          //Make button
          cancelButton.setText("Cancel");
          cancelButton.setActionCommand("Cancel");
          cancelButton.addActionListener(this);
          cancelButton.setEnabled(false);
          
          pnlStatus.add(lblProgress, new XYConstraints(5, 5, 580, 20));
          pnlStatus.add(progressBar, new XYConstraints(5, 30, 580, 25));
          pnlStatus.add(lblStatus, new XYConstraints(5, 65, 580, 20));
          pnlStatus.add(cancelButton, new XYConstraints(505, 90, 80, 25));
          this.getContentPane().add(pnlStatus);
      
          //Display the window.
          pack();
          setVisible(true);
          
          startTime = new java.util.Date();
          cancelButton.setEnabled(true);
          exportTask = new ExportTask();
          exportTask.execute();          
      }

        /** Swing worker based methods
         */
        private class ExportTask extends SwingWorker<Void, Integer> 
        {
            private OracleResultSetMetaData                     meta = null;
            private LinkedHashMap<Integer,RowSetMetaData> resultMeta = null;
            private int                                    geoColumn = -1;
            private LinkedHashMap<Integer,Integer>    skippedRecords = new LinkedHashMap<Integer,Integer>(Constants.SHAPE_TYPE.values().length);
            private int                                totalRowCount = -1;
            
            private LinkedHashMap<Integer,RowSetMetaData> getExportMetadata() 
            {
                if ( this.meta ==null  )
                    return null;              
                try {
                    LinkedHashMap<Integer,RowSetMetaData> resultMeta = new LinkedHashMap<Integer,RowSetMetaData>(meta.getColumnCount());
                    this.geoColumn = -1;
                    for (int col = 1; col <= this.meta.getColumnCount(); col++) 
                    {
                        RowSetMetaData rsMD = new OraRowSetMetaDataImpl();
                        rsMD.setColumnCount(1);  // Must go first
                        rsMD.setCatalogName(1, "");
                        if (this.meta.getColumnLabel(col).equals(getColumnName()) &&
                            this.meta.getColumnTypeName(col).equals(Constants.TAG_MDSYS_SDO_GEOMETRY) ) {
                            this.geoColumn = col;
                            rsMD.setCatalogName(1,this.meta.getColumnLabel(col));
                        }
                        rsMD.setColumnName(1, this.meta.getColumnLabel(col));
                        rsMD.setColumnType(1, this.meta.getColumnType(col));
                        rsMD.setColumnDisplaySize(1,this.meta.getColumnDisplaySize(col));
                        rsMD.setColumnTypeName(1, this.meta.getColumnTypeName(col));
                        rsMD.setPrecision(1,this.meta.getPrecision(col)<0?0:this.meta.getPrecision(col));  // BLOBs and CLOBs have -1 precision
                        rsMD.setScale(1,this.meta.getScale(col)<0?0:this.meta.getScale(col));              // Scales like -127 exist for binary_float and binary_double but don't contribute to conversion
                        resultMeta.put(col,rsMD);
                    }
                    if ( this.geoColumn == -1 ) {
                        throw new SQLException ("Table does not have an sdo_geometry column.");
                    }
                    return new LinkedHashMap<Integer,RowSetMetaData>(resultMeta); 
                } catch (SQLException sqle) {
                    LOGGER.error("ExporterWriter.getExportMetadata(): Exception caught when creating export metadata " + sqle.getMessage());
                }
                return null;
            }

            public LinkedHashMap<Integer,Integer> getSkipStatistics() {
                return new LinkedHashMap<Integer,Integer>(this.skippedRecords);
            }
            
            public int getTotalRows() {
              return this.totalRowCount;
            }
        
            private void setTotalRows() {
                this.totalRowCount = -1;
                try {
                    lblStatus.setText(propertyManager.getMsg("CALCULATING_ROWCOUNT_START"));
                    errorMessage = null;
                    this.totalRowCount = MetadataTool.getRowCount(getConn(),
                                                                  getSchemaName(),
                                                                  getObjectName(),
                                                                  null);
                  lblStatus.setText(propertyManager.getMsg("CALCULATING_ROWCOUNT_FINISHED",this.totalRowCount));
                } catch (SQLException sqle) { 
                  errorMessage = "Set Total Rows: " + sqle.getMessage();
                }
            }
        

            protected void export() 
            {
                OracleResultSet rSet = null;
                OracleStatement st = null;
                
                String sql = "SELECT " + getColumnName() + (Strings.isEmpty(getColumns()) ? "" : "," + getColumns()) + "\n" +
                             "  FROM " + Strings.append(getSchemaName(), getObjectName(), ".");
                
                IExporter geoExporter = null;
                try 
                {
                    // Compute total number of rows for progress bar
                    //
                    setTotalRows();
                    
                    lblStatus.setText(propertyManager.getMsg("EXECUTING_QUERY"));
                    
                    st = (OracleStatement)getConn().createStatement(OracleResultSet.TYPE_FORWARD_ONLY,
                                                                    OracleResultSet.CONCUR_READ_ONLY);
                    st.setFetchDirection(OracleResultSet.FETCH_FORWARD);
                    st.setFetchSize(geoRaptorPreferences.getFetchSize());
                    rSet      = (OracleResultSet)st.executeQuery(sql);
                    // Setting up metadata
                    
                    this.meta       = (OracleResultSetMetaData)rSet.getMetaData();
                    this.resultMeta = getExportMetadata();
                  
                    switch ( getExportType() ) {
                      case GML : geoExporter = new GMLExporter(getConn(),getExportFileName(),getTotalRows()); 
                                 if ( isAttributes() ) 
                                     writeXSD(rSet);
                                 break;
                      case KML : geoExporter = new KMLExporter(getConn(),getExportFileName(),getTotalRows()); break;
                      case SHP : geoExporter = new SHPExporter(getConn(),getExportFileName(),getTotalRows()); break;
                      case TAB : geoExporter = new TABExporter(getConn(),getExportFileName(),getTotalRows()); break;
                    }
                    geoExporter.setBaseName(getExportBaseName());
                    geoExporter.setAttributeFlavour(getAttributeFlavour());
                    geoExporter.setCommit(getCommit());
                    geoExporter.setGenerateIdentifier(! hasAttributeColumns());
                    geoExporter.setGeoColumnIndex(geoColumn);
                    geoExporter.setGeoColumnName(getColumnName());
                    geoExporter.setExportMetadata(this.resultMeta);
                    
                    STRUCT geoStruct = null;

                    // Discover geometryMetadata
                    //
                    lblStatus.setText(propertyManager.getMsg("DISCOVERING_SHAPE_PROPERTIES"));
                    errorMessage = lblStatus.getText();
                    
                    GeometryProperties geomMetadata = new GeometryProperties();
                    geomMetadata.setShapefileType(getShapefileType());
                    geomMetadata.setSRID(getSRID());
                    // We don't care what sort of geometry type is written to a GML or KML files but we do for a shapefile/tabfile
                    //
                    if (getExportType().compareTo(Constants.EXPORT_TYPE.SHP)==0 || 
                        getExportType().compareTo(Constants.EXPORT_TYPE.TAB)==0 ) 
                    {
                        geomMetadata.setFullGType( ( getShapefileType().id < 10 
                                                    ? 2000 
                                                    : getShapefileType().id < 20 
                                                      ? 3000 
                                                      : 4000 
                                                   ) +
                                                      ( getShapefileType().isPointType() 
                                                      ? 1 : getShapefileType().isMultiPointType()
                                                            ? 5 : getShapefileType().isLineType() 
                                                                  ? 6 : getShapefileType().isPolygonType() 
                                                                        ? 7 : 1 )
                                                  ); 
                        geomMetadata.setGeometryType(SDO_GEOMETRY.discoverGeometryType(geomMetadata.getGType(),
                                                                                       Constants.GEOMETRY_TYPES.POINT));
                        geomMetadata.setDimension(geomMetadata.getFullGType() / 1000); 
                    }
                    geoExporter.setGeometryProperties(geomMetadata);

                    lblStatus.setText(propertyManager.getMsg("PROCESSING_TABLE_DATA",
                                                         Strings.append(getSchemaName(), getObjectName(), "."),
                                                             getExportFileName()));
                    errorMessage = null;
                    
                    int percentageProcessed = -1;
                    OraRowSetMetaDataImpl rsMD = new OraRowSetMetaDataImpl();

                    geoExporter.start(getCharacterSet());
                    while (rSet.next()) 
                    {
                        // Process geometry first to see if we can skip the whole row.
                        //
                        geoStruct = (oracle.sql.STRUCT)rSet.getOracleObject(getColumnName());
                        if ( rSet.wasNull() || geoStruct == null ) {
                            if ( isSkipNullGeometry() ) {
                                setSkipStatistics(ShapeType.NULL,false);
                                continue;
                            } 
                        } else {
                            // Certain geometry types cannot be written to a shapefile/tab file as are unsupported
                            // Measured geometries cannot be written to KML/GML
                            //
                            ShapeType shpType =
                            SDO_GEOMETRY.getShapeType(SDO_GEOMETRY.getFullGType(geoStruct,2000),
                                                                          getShapefileType().hasMeasure());
                            if ( (getExportType()==Constants.EXPORT_TYPE.SHP || 
                                  getExportType()==Constants.EXPORT_TYPE.TAB) &&
                                  (getShapefileType().equals(shpType)==false || SDO_GEOMETRY.hasArc(geoStruct)) 
                                || 
                                 ( (getExportType()==Constants.EXPORT_TYPE.GML  || 
                                    getExportType()==Constants.EXPORT_TYPE.KML) && SDO_GEOMETRY.hasMeasure(geoStruct) 
                                 ) ) 
                            {
                                setSkipStatistics(shpType, SDO_GEOMETRY.hasArc(geoStruct));
                                continue;
                            }
                        } 
                        String columnValue = "";
                        geoExporter.startRow();
                        for (int col = 1; col <= this.meta.getColumnCount(); col++) 
                        {                     
                            // Now iterate over columns and export values
                            //
                            try 
                            {
                                rsMD = (OraRowSetMetaDataImpl)this.resultMeta.get(col);
                                if (rsMD.getColumnName(1).equalsIgnoreCase(getColumnName()) )  
                                {
                                    rsMD.setCatalogName(1, rsMD.getColumnName(1));
                                    if ( rsMD.getColumnTypeName(1).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ) {
                                        if (geoStruct != null ) {
                                            int SRID = SDO_GEOMETRY.getSRID(geoStruct, Constants.SRID_NULL);
                                            if ( (SRID==Constants.SRID_NULL && getSRID()!=Constants.SRID_NULL) || 
                                                 (SRID!=Constants.SRID_NULL && getSRID()!=Constants.SRID_NULL && SRID!=getSRID()) ) {
                                                geoStruct = SDO_GEOMETRY.setSRID(geoStruct, getSRID());
                                            }
                                        }
                                        try {
                                            geoExporter.printColumn(geoStruct,rsMD);
                                        } catch (SQLException sqle) {
                                            // Failed to convert geometry to JTS Geometry
                                            // Write null geometry instead
                                            //
                                            geoExporter.printColumn(null,rsMD);
                                        }
                                        continue;
                                    }
                                }
                                
                                if (! isAttributes()) {
                                    continue;
                                }
                                
                                if ( Tools.isSupportedType(rsMD.getColumnType(1),rsMD.getColumnTypeName(1)) ) {
                                    // dbase Specific date issue 
                                    if ( rsMD.getColumnTypeName(1).equalsIgnoreCase("DATE") ) {
                                        geoExporter.printColumn(rSet.getDate(col)!=null 
                                                                ? rSet.getDate(col).toString()
                                                                : Preferences.getInstance().getNullDate(),
                                                                rsMD); // This gives us yyyy-MM-DD as required by dBase etc.
                                    } else {
                                        columnValue = SQLConversionTools.convertToString((OracleConnection)conn,rSet,col);
                                        if ( columnValue == null ) {
                                            if ((getExportType().compareTo(Constants.EXPORT_TYPE.SHP)==0 ||
                                                 getExportType().compareTo(Constants.EXPORT_TYPE.TAB)==0 ) ) {
                                                if (Preferences.getInstance().getDbaseNullWriteString() ) {
                                                    columnValue = ""; 
                                                } else {
                                                    columnValue = Preferences.getInstance().getNullValue(rsMD.getColumnType(1));
                                                }
                                            } else {
                                                columnValue = Preferences.getInstance().getNullValue(rsMD.getColumnType(1));
                                            }
                                        }
                                        geoExporter.printColumn(columnValue,rsMD);
                                    }
                                } else {
                                    if ( geoExporter.getRowCount() == 1 ) {
                                        LOGGER.error("ExporterWriter.run(): Column " + rsMD.getColumnName(1) + " of type " + rsMD.getColumnTypeName(1) + " is not supported");
                                    }
                                }
                            } catch (Exception e) {
                              LOGGER.error("ExporterWriter.run(): Error converting column/type " + rsMD.getColumnName(1) + "/" + rsMD.getColumnType(1));                      
                            }
                        }
                        geoExporter.endRow();
                        rowsExported = geoExporter.getRowCount();
                        if ( (rowsExported % geoExporter.getCommit()) == 0 ) {
                            if ( totalRowCount == -1 ) {
                                percentageProcessed = totalRowCount;
                            } else {
                                percentageProcessed = (int)(((double)rowsExported/(double)totalRowCount) * 100.0);
                                // Update progress
                                publish(new Integer(percentageProcessed));
                            }
                        }
                    }
                    errorMessage = null;
                } catch (IOException ioe) {
                  ioe.printStackTrace();
                  lblStatus.setText("ExporterWriter: File Error: " + ioe.getMessage());
                  errorMessage = lblStatus.getText();
                  exportTask.cancel(true);
                } catch (SQLException sqle) {
                  lblStatus.setText("ExporterWriter: SQL Error\n"+sqle.getMessage());
                  errorMessage = lblStatus.getText();
                  exportTask.cancel(true);
                } catch (NullPointerException npe) {
                  lblStatus.setText("ExporterWriter: Null pointer exception occurred "+ npe.getMessage());
                  errorMessage = lblStatus.getText();
                  exportTask.cancel(true);
                } catch (Exception e) {
                  lblStatus.setText("ExporterWriter: Exception caught when starting export of " + getExportType().toString() + " \n"+e.getMessage());
                  errorMessage = lblStatus.getText();
                  exportTask.cancel(true);
                } finally {
                    try { if (rSet!=null ) rSet.close(); } catch (Exception _e) { }
                    try { if (st!=null)      st.close(); } catch (Exception _e) { }
                    rowsExported = geoExporter.getRowCount();
                    try {
                        geoExporter.end();
                        geoExporter.close();
                        geoExporter = null;
                    } catch (IOException e) { }
                }
                if ( totalRowCount != -1 ) {
                    // Update progress
                    publish(new Integer(100));
                }
                lblProgress.setText("Rows Exported: " + (totalRowCount==-1 ? String.valueOf(rowsExported) : "100%"));
            }
            
            private void setSkipStatistics(ShapeType _sType,boolean _hasArc) 
            {
                if (_sType==null) {
                    return;
                }
                if ( skippedRecords == null ) {
                    this.skippedRecords = new LinkedHashMap<Integer,Integer>(Constants.SHAPE_TYPE.values().length);
                }
              
                int shapeTypeId = _hasArc && _sType.id!=ShapeType.UNDEFINED.id ? _sType.id * -1 : _sType.id;
                if ( _sType.id!=ShapeType.UNDEFINED.id || _sType.id < -1) {
                    shapeTypeId = _hasArc ? _sType.id * -1 : _sType.id;
                    if ( _sType.hasMeasure() ) {
                        shapeTypeId = _sType.id * -100;
                    }
                }

                if ( skippedRecords.size()==0) {
                    skippedRecords.put(shapeTypeId,1);
                    return;
                }
              
                Integer recordCount = skippedRecords.get(shapeTypeId);
                if (recordCount==null) {
                    recordCount = new Integer(1);
                }
                else {
                    recordCount++;
                }
                
                // Create new or update existing
                skippedRecords.put(shapeTypeId,recordCount);
            }
        
            private void showExportStats(String _processingTime) 
            {
                // Show Export Statistics
                //
                int exportedRecordCount = rowsExported;
                LinkedHashMap<Integer,Integer> skipStats = exportTask.getSkipStatistics();
                StringBuffer sb = new StringBuffer(1000);
                sb.append(propertyManager.getMsg("RESULTS_TOP", Strings.append(getSchemaName(), getObjectName(), "."),
                                               String.format("%d (%s)",
                                                             exportedRecordCount,
                                                             getShapefileType().toString()),
                                               _processingTime));
                if ( skipStats!=null && skipStats.size()!=0 ) {
                    sb.append(propertyManager.getMsg("RESULTS_TABLE_HEADER")); 
                    
                    String sShapeType = "";
                    int shapeTypeId = -1;
                    int shapeTypeCount = -1;
                    int shapeTotalCount = 0;
                    
                    Iterator<Integer> iter = skipStats.keySet().iterator();
                    while ( iter.hasNext() ) {
                        shapeTypeId = iter.next();
                        shapeTypeCount = skipStats.get(shapeTypeId);
                        shapeTotalCount += shapeTypeCount;
                        sShapeType = "";
                        if ( shapeTypeId!=ShapeType.UNDEFINED.id ) {
                            if ( shapeTypeId < -100 ) {
                                shapeTypeId = Math.abs(shapeTypeId);
                                sShapeType = " (Measures)";
                            } else if ( shapeTypeId > -100 && shapeTypeId < -1) {
                                shapeTypeId = Math.abs(shapeTypeId);
                                sShapeType = " (CIRCULAR ARC)";
                            }
                        }
                        ShapeType sType = ShapeType.forID(shapeTypeId);
                        sb.append(propertyManager.getMsg("RESULTS_TABLE_REPEATABLE_ROW",sType.toString() + sShapeType,shapeTypeCount));              
                    }
                    sb.append(propertyManager.getMsg("RESULTS_TABLE_TOTAL_ROW",shapeTotalCount));
                }
                sb.append(propertyManager.getMsg("RESULTS_END"));
                JOptionPane.showMessageDialog(null, new JLabel(sb.toString())); 
            }
            
            @Override
            protected Void doInBackground() {
                export();
                return null;  // Stop job
            }
    
            @Override
            protected void done() {
              setVisible(false);
              if (!Strings.isEmpty(getErrorMessage()) ) {
                  // show error message
                  JOptionPane.showMessageDialog(null,
                                                getErrorMessage(),
                                                Constants.GEORAPTOR + " - " + this.getClass().getName(),
                                                JOptionPane.ERROR_MESSAGE);
              } else {
                  java.util.Date endTime = new java.util.Date();
                  long timeDiff = ( endTime.getTime() - startTime.getTime() );
                  String processingTime = Tools.milliseconds2Time(timeDiff);
                  showExportStats(processingTime);
              }
        }
    
            @Override
            protected void process(List<Integer> percents) {
                if ( percents == null ) {
                    return;
                }
                Integer percent  = percents.get(percents.size()-1);
                progressBar.setValue(percent);
                // Update progress
                lblProgress.setText(String.format("Wrote %d of %d rows (%d%%) ...",
                                                  rowsExported,
                                                  totalRowCount,
                                                  percent));
            }
        
      }

}
