package org.GeoRaptor.io.Export;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import javax.sql.RowSetMetaData;

import org.GeoRaptor.Constants;
import org.GeoRaptor.sql.OraRowSetMetaDataImpl;
import org.GeoRaptor.tools.GeometryProperties;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.geotools.util.logging.Logger;

import oracle.spatial.util.KML2;
import oracle.sql.STRUCT;

@SuppressWarnings("deprecation")
public class KMLExporter implements IExporter 
{

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.KMLExporter");

    private final static String             KmlNameSpace = "\"http://www.opengis.net/kml/2.2\"";
    private String                               newLine = System.getProperty("line.separator");
    private String                       defaultSyleName = "geoRaptorDefaultSyles";
    private boolean                     styleUrlIsColumn = false;
    private Connection                              conn = null;
    private int                                geoColumn = -1;
    @SuppressWarnings("unused")
	private String                         geoColumnName = "";
    private int                                   commit = 100;
    private GeometryProperties        geometryProperties = null;
    private LinkedHashMap<Integer,
                          RowSetMetaData> exportMetadata = null;
    private Constants.renderType                 kmlType = Constants.renderType.KML2;
    private String                          extendedData = "<ExtendedData>";
    private StringBuffer                       rowBuffer = null;
    private String                           kmlFilename = "";
    private BufferedWriter                       kmlFile = null;
    private boolean                      needsIdentifier = false;
    private int                                      row = 0;
    private int                                totalRows = 0;
    @SuppressWarnings("unused")
	private String                              baseName = "";
    private Constants.XMLAttributeFlavour attributeFlavour = Constants.XMLAttributeFlavour.KML;
        
    public KMLExporter (Connection _conn,
                        String     _fileName,
                        int        _totalRows) 
    {
        super();
        this.conn = _conn;
        this.totalRows = _totalRows;
        setFileName(_fileName);
        KML2.setConnection(this.conn);
    }

    public void setGenerateIdentifier(boolean _identifier) {
        this.setNeedsIdentifier(_identifier);
    }
  
    public void setExportMetadata(LinkedHashMap<Integer, RowSetMetaData> _exportMetadata) {
      this.exportMetadata = _exportMetadata;
    }
    
    public LinkedHashMap<Integer, RowSetMetaData> getExportMetadata() {
      return this.exportMetadata;
    }
    
    public GeometryProperties getGeometryProperties() {
        return this.geometryProperties;
    }
    
    public void setGeometryProperties(GeometryProperties _geometryProperties) {
        this.geometryProperties = _geometryProperties;
    }

    public String getFileName() {
        return this.kmlFilename; 
    }

    public void setFileName(String _fileName) {
        this.kmlFilename = _fileName;    
    }
    
    public void setBaseName(String _baseName) {
        this.baseName = _baseName;
    }

    public void setAttributeFlavour(String _flavour) {
        if (Strings.isEmpty(_flavour) )
            return;
        this.attributeFlavour = Constants.XMLAttributeFlavour.KML;
    }
  
    public String getFileExtension() {
        return "kml";          
    }

    public void setCommit(int _commit) {
      this.commit = _commit;
    }
    
    public int getCommit() {
        return this.commit;      
    }
    
    public void setGeoColumnIndex(int _geoColumnIndex) {
        this.setGeoColumn(_geoColumnIndex);
    }
    
    public void setGeoColumnName(String _geoColumnName) {
        this.geoColumnName = _geoColumnName;
    }
  
    public void start(String _encoding) throws Exception {
        this.rowBuffer = new StringBuffer(100000);
        if (Strings.isEmpty(this.kmlFilename) ) {
            throw new IOException("Filename not set");
        }
        this.kmlFile = new BufferedWriter(new FileWriter(this.kmlFilename));
        this.row = 0;
        String baseName =
            Strings.isEmpty(this.kmlFilename)
                          ? ""
                          : "    <name>" + this.kmlFilename + "</name>" + newLine;
        this.kmlFile.write("<?xml version='1.0'  encoding='" + _encoding + "' ?>" + newLine +
              "<kml xmlns= " + KmlNameSpace + ">" + newLine +
              "  <Document>" + newLine +
              baseName );
        // Default Styling depending on geometryType
        String defaultStyles = 
            "    <Style id=\"" + defaultSyleName + "\">" + newLine +
            "      <IconStyle>" + newLine + 
            "        <color>a1ff00ff</color>" + newLine + 
            "        <scale>1.4</scale>" + newLine + 
            "        <Icon>" + newLine + 
            "          <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>" + newLine + 
            "        </Icon>" + newLine + 
            "      </IconStyle>" + newLine + 
            "      <LabelStyle>" + newLine + 
            "        <color>7fffaaff</color>" + newLine + 
            "        <scale>1.5</scale>" + newLine + 
            "      </LabelStyle>" + newLine +
            "      <LineStyle>" + newLine + 
            "        <color>ffffffff</color>" + newLine + 
            "        <colorMode>random</colorMode>" + newLine + 
            "        <width>2</width>" + newLine + 
            "      </LineStyle>" + newLine +
            "      <PolyStyle>" + newLine + 
            "        <color>ffffffff</color>" + newLine + 
            "        <colorMode>random</colorMode>" + newLine + 
            "      </PolyStyle>" + newLine + 
            "    </Style>" + newLine;
        this.kmlFile.append(defaultStyles);
    }

    public void startRow() throws IOException {
        this.styleUrlIsColumn = false;
        this.rowBuffer.append("    <Placemark>" + newLine);
        this.extendedData = "      <ExtendedData>" + newLine;
    }

    public void printColumn(Object                _object, 
                            OraRowSetMetaDataImpl _columnMetaData) 
    throws SQLException
    {
        String kmlText = "";
        STRUCT stValue = null;
        try {
            // Mappable column?
            //
            if (!Strings.isEmpty(_columnMetaData.getCatalogName(1)) )   // Catalog name holds name of actual geometry column 
            { 
                if ( _columnMetaData.getColumnTypeName(1).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) )
                {
                    kmlText = "";
                    stValue = (STRUCT)_object;
                    if ( stValue == null ) {
                        LOGGER.warn("NULL Geometry: No KML written for row " + (row+1));
                        return;
                    }
                    
                    // get SRID
                    int SRID = SDO_GEOMETRY.getSRID(stValue, Constants.SRID_NULL);
                    if ( SRID == Constants.SRID_NULL ) {
                        LOGGER.warn("Geometry's SRID is NULL: No KML written for row " + (row+1));
                        return;
                    }
                  
                    // If sdo_geometry object is 3D then certain renders cannot occur
                    //
                    int dim = SDO_GEOMETRY.getDimension(stValue, 2);
                    if ( dim >= 3 ) {
                        if ( this.kmlType == Constants.renderType.KML ) {
                            // reduce to 2D
                        }
                    }
                    kmlText = KML2.to_KMLGeometry(stValue);
                    if ( !Strings.isEmpty(kmlText) ) {
                        kmlText = kmlText.replaceAll(KmlNameSpace, "").replace("srsName=\"SDO:\"","").replace("  "," ").replace(" >",">");
                    }
                    this.rowBuffer.append("      " + kmlText + newLine);
                }
            } else { // Process Attribute column
                try {
                    // Passed in object is already a string
                    //
                    String columnName = _columnMetaData.getColumnName(1);
                    if ( columnName.equalsIgnoreCase("name") ) {
                        this.rowBuffer.append("      <name>" + ((String)_object) + "</name>" + newLine);
                    } else if ( columnName.equalsIgnoreCase("description") ) {
                        this.rowBuffer.append("      <description>" + ((String)_object) + "</description>" + newLine);
                    } else if ( columnName.equalsIgnoreCase("styleUrl") ) {
                        this.rowBuffer.append("      <styleUrl>" + ((String)_object) + "</styleUrl>" + newLine);
                        this.styleUrlIsColumn = true;
                    } else {
                        try {
            /**
            * <ExtendedData>             OR  <ExtendedData xmlns:prefix="camp">           OR <ExtendedData>
            *   <Data name="holeNumber">       <camp:number>14</camp:number>                   <SchemaData schemaUrl="#TrailHeadTypeId">
            *     <value>1</value>             <camp:parkingSpaces>2</camp:parkingSpaces>        <SimpleData name="TrailHeadName">Mount Everest</SimpleData>
            *   </Data>                        <camp:tentSites>4</camp:tentSites>                <SimpleData name="TrailLength">347.45</SimpleData>
            *   <Data name="holePar">                                                            <SimpleData name="ElevationGain">10000</SimpleData>
            *     <value>4</value>                                                             </SchemaData>
            *   </Data>
            * </ExtendedData>                </ExtendedData>                                 </ExtendedData> 
            *                                // Where camp will be the tablename/filename 
            */
                            this.extendedData += "        <Data name=" + "\"" + columnName + "\">" + newLine +
                                                 "          <value>" + Strings.escapeHTML((String)_object) + "</value>" + newLine +
                                                 "        </Data>" + newLine;
                        } catch (Exception e) {
                            LOGGER.warn("Conversion of " + _columnMetaData.getColumnName(1) + "/" + _columnMetaData.getColumnTypeName(1) + " failed at row " + (row+1) + " - " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("KMLExporter.printColumn: Exception: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("KMLExporter: printColumn(object,metadata) = " + e.getMessage());
        }
    }

    public void endRow() throws IOException {
        this.row++;
        if ( this.styleUrlIsColumn == false) {
            this.rowBuffer.append("      <styleUrl>#" + this.defaultSyleName + "</styleUrl>" + newLine);
        }
        if (! Strings.trimAll(this.extendedData,' ').equals("<ExtendedData>") ) {
            this.rowBuffer.append(this.extendedData + 
                               "      </ExtendedData>" + newLine);
        }
        this.rowBuffer.append("    </Placemark>" + newLine);
        if ( (this.row % this.getCommit()) == 0 ) {
            this.kmlFile.write(this.rowBuffer.toString());
            this.rowBuffer = new StringBuffer(100000);
            this.kmlFile.flush();
        }
    }

    public void end() throws IOException {
        if ( this.rowBuffer.length() > 0 ) {
            this.kmlFile.write(this.rowBuffer.toString());
        }  
        this.kmlFile.write("  </Document>" + newLine + 
                           "</kml>" + newLine);
        this.kmlFile.flush();
    }
    
    public int getRowCount() {
      return this.row;
    }

  public void close() {
      try {
          this.kmlFile.close();
          this.kmlFile = null;
      } catch (IOException ioe) {
        // Do nothing.
      }
  }

    @Override
    public void setTotalRows(int _totalRows) {
        this.totalRows = _totalRows;
    }

    @Override
    public int getTotalRows() {
        return this.totalRows;
    }

	public Constants.XMLAttributeFlavour getAttributeFlavour() {
		return attributeFlavour;
	}

	public void setAttributeFlavour(Constants.XMLAttributeFlavour attributeFlavour) {
		this.attributeFlavour = attributeFlavour;
	}

	public boolean isNeedsIdentifier() {
		return needsIdentifier;
	}

	public void setNeedsIdentifier(boolean needsIdentifier) {
		this.needsIdentifier = needsIdentifier;
	}

	public int getGeoColumn() {
		return geoColumn;
	}

	public void setGeoColumn(int geoColumn) {
		this.geoColumn = geoColumn;
	}
}
