package org.GeoRaptor.io.Export;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;

import java.util.LinkedHashMap;

import javax.sql.RowSetMetaData;

import oracle.spatial.util.GML3;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.OraRowSetMetaDataImpl;
import org.GeoRaptor.tools.GeometryProperties;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;

import org.geotools.util.logging.Logger;


@SuppressWarnings("deprecation")
public class GMLExporter implements IExporter 
{
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.GMLExporter");

    private final static String GmlNameSpace = "xmlns:gml=\"http://www.opengis.net/gml\"";
    private String                   newLine = System.getProperty("line.separator");
  
    private Connection                                      conn = null;
    private int                                        geoColumn = -1;
    private String                                 geoColumnName = "";
    private static int                                    commit = 100;
    private GeometryProperties                geometryProperties = null;
    private LinkedHashMap<Integer,RowSetMetaData> exportMetadata = null;
    private String                                      baseName = "";
    private int                                              row = 0;
    private int                                        totalRows = 0;    
    private Constants.XMLAttributeFlavour       attributeFlavour = Constants.XMLAttributeFlavour.OGR;
    
    private String          srsName      = null;
    private String          srsNameSpace = null;
    private int             srsDimension = 2;
    private int                 prevSRID = Constants.SRID_NULL;
    private Envelope     envelope = null;
    private StringBuffer       rowBuffer = null;
    private String           gmlFilename = "";
    private BufferedWriter       gmlFile = null;
    private boolean      needsIdentifier = false;
    
    public GMLExporter (Connection _conn,
                        String     _fileName,
                        int        _rowsToProcess) 
    {
        super();
        this.conn = _conn;
        this.totalRows = _rowsToProcess;
        setFileName(_fileName);
        GML3.setConnection(this.conn);
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
        return this.gmlFilename; 
    }
    
    public void setFileName(String _fileName) {
        this.gmlFilename = _fileName;    
    }
    
    public void setBaseName(String _baseName) {
        this.baseName = _baseName;
    }

    public void setAttributeFlavour(String _flavour) {
        if (Strings.isEmpty(_flavour) )
            return;
        try {
            this.attributeFlavour = Constants.XMLAttributeFlavour.valueOf(_flavour); 
        } catch (Exception e) {
            this.attributeFlavour = Constants.XMLAttributeFlavour.OGR;
        }
    }

    public String getFileExtension() {
        return "gml";
    }
    
    public void setCommit(int _commit) {
      GMLExporter.commit = _commit;
    }
    
    public int getCommit() {
        return GMLExporter.commit;      
    }
        
    public void setGeoColumnIndex(int _geoColumnIndex) {
        this.setGeoColumn(_geoColumnIndex);
    }

    public void setGeoColumnName(String _geoColumnName) {
        this.geoColumnName = _geoColumnName;
    }

    public String getGeoColumnName() {
        return this.geoColumnName;
    }

    public void start(String _encoding) throws Exception {
        this.rowBuffer = new StringBuffer(10000);
        if (Strings.isEmpty(gmlFilename) ) {
            throw new IOException("Filename not set");
        }
        this.gmlFile = new BufferedWriter(new FileWriter(this.gmlFilename));
        this.row = 0;
        this.envelope = new Envelope(Constants.MAX_PRECISION);
        this.srsName = null;
        this.srsNameSpace = null; 
        this.prevSRID = Constants.SRID_NULL;
        this.srsDimension = 2;
        this.gmlFile.write("<?xml version='1.0'  encoding='" + _encoding + "' ?>" + newLine);
        if ( this.attributeFlavour == Constants.XMLAttributeFlavour.OGR ) {
          this.gmlFile.write("<ogr:FeatureCollection" + newLine + 
                              "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newLine +
                              "     " + GmlNameSpace + newLine +
                              "     xmlns:ogr=\"http://ogr.maptools.org/\"" + newLine +
                              "     xsi:schemaLocation=\"http://ogr.maptools.org/ " + this.baseName + ".xsd\">" + newLine );
        } else if (this.attributeFlavour == Constants.XMLAttributeFlavour.FME ) {
          this.gmlFile.write("<gml:FeatureCollection" + newLine + 
                              "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newLine +
                              "     xmlns:xlink=\"http://www.w3.org/1999/xlink\"" + newLine +
                              "     " + GmlNameSpace + newLine +
                              "     xmlns:fme=\"http://www.safe.com/gml/fme\"" + newLine +
                              "     xsi:schemaLocation=\"http://www.safe.com/gml/fme " + this.baseName + ".xsd\">" + newLine); 
        } else {
          this.gmlFile.write("<gml:FeatureCollection " + newLine + 
                              "     xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + newLine +
                              "     " + GmlNameSpace + " " + newLine +
                              "     xsi:schemaLocation=\"file:///" + this.gmlFilename.replace(".gml",".xsd").replace("\\","/") + "\"" + newLine + 
                              "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + newLine );
        }
    }

    public void startRow() throws IOException {
        this.rowBuffer.append("  <gml:featureMember>" + newLine);
        if ( this.attributeFlavour == Constants.XMLAttributeFlavour.OGR ) 
            this.rowBuffer.append("    <ogr:" + this.baseName + /* " ogr:fid=\"F" + String.valueOf(row) + "\""*/ ">" + newLine);
        else if ( this.attributeFlavour == Constants.XMLAttributeFlavour.FME )
            this.rowBuffer.append("    <fme:" + this.baseName + /* " fme:id=\"" + String.valueOf(row) + "\"" */ ">" + newLine);
    }

    public void printColumn(Object                _object, 
                            OraRowSetMetaDataImpl _columnMetaData) 
    {
        String gmlText = "";
        STRUCT stValue = null; 
        try {
            // Mappable column?
            // System.out.println(_columnMetaData.getCatalogName(1) + "\t" + _columnMetaData.getColumnTypeName(1));
            //
            if (!Strings.isEmpty(_columnMetaData.getCatalogName(1)) )   // Catalog name holds name of actual geometry column 
            { 
                if ( _columnMetaData.getColumnTypeName(1).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ) 
                {
                    gmlText = "";
                    stValue = (STRUCT)_object; 
                    if ( stValue == null ) {
                        LOGGER.warn("NULL Geometry: No featureMember element written for row " + (row+1));
                        // Will produce an empty featureMember
                       return;
                    }
                    
                    // get SRID
                    int SRID = SDO_GEOMETRY.getSRID(stValue, Constants.SRID_NULL);
                    if ( SRID == Constants.SRID_NULL ) {
                        LOGGER.warn("Geometry's SRID is NULL: No featureMember element written for row " + (row+1));
                        return;
                    }
                    
                    if ( SRID != this.prevSRID ) {
                        // Get srsName and srsNamespace from SrsNameSpace_Table
                        String srsNames = MetadataTool.getSrsNames(conn,SRID,"@",true);
                        if ( !Strings.isEmpty(srsNames) ) {
                            this.srsName      = srsNames.substring(0,srsNames.indexOf("@"));
                            this.srsNameSpace = srsNames.substring(srsNames.indexOf("@")+1);
                        }
                        this.prevSRID = SRID;
                    }
                    
                    int dim = SDO_GEOMETRY.getDimension(stValue, 2);
                    if ( dim != this.srsDimension )
                        this.srsDimension = dim; 
                    
                    gmlText = GML3.to_GML3Geometry(stValue);
                    if ( this.attributeFlavour == Constants.XMLAttributeFlavour.FME ) {
                        gmlText = gmlText.replaceAll(GmlNameSpace,"");
                        gmlText = gmlText.replace("srsName=\"SDO:" + String.valueOf(SRID) + "\"",
                                                  "srsName=\"EPSG:" + String.valueOf(SRID) + "\"");
                    } else {
                        gmlText = gmlText.replaceAll(GmlNameSpace, "xmlns:urn=\"" + this.srsNameSpace + "\"");
                        gmlText = gmlText.replace("srsName=\"SDO:" + String.valueOf(SRID) + "\"", "srsName=\"urn:" + this.srsName + "\" ");
                    }

                    // Update GML file's envelope
                    //
                    Envelope geomMBR = SDO_GEOMETRY.getGeoMBR(stValue);
                    this.envelope.setMaxMBR(geomMBR);
                    
                    this.rowBuffer.append("      <" + this.getXMLFlavourPrefix(this.attributeFlavour) + ":geometryProperty>" + newLine + 
                                          "        " + gmlText + newLine + 
                                          "      </" + this.getXMLFlavourPrefix(this.attributeFlavour) + ":geometryProperty>" + newLine);
                } 
            } else { // Process Attribute column
                // Passed in _object is already a string
                //
                try {
                    this.rowBuffer.append("      <" + this.getXMLFlavourPrefix(this.attributeFlavour) + ":" + _columnMetaData.getColumnName(1) + ">" +
                                          _object.toString() +
                                          "</" + this.getXMLFlavourPrefix(this.attributeFlavour) + ":" + _columnMetaData.getColumnName(1) + ">"+ newLine);
                } catch (Exception e) {
                    LOGGER.warn("Conversion of " + _columnMetaData.getColumnName(1) + "/" + _columnMetaData.getColumnTypeName(1) + " failed at row " + (this.row+1) + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("GMLExporter.printColumn(object,ResultSetMetadata) = " + e.getMessage());
        }
    }

    public void endRow() throws IOException {        
        this.row++;
        this.rowBuffer.append("    </" + this.getXMLFlavourPrefix(this.attributeFlavour) + ":" + this.baseName + ">" + newLine);
        this.rowBuffer.append("  </gml:featureMember>" + newLine);
        if ( this.row % this.getCommit() == 0 ) {
            this.gmlFile.write(this.rowBuffer.toString());
            this.rowBuffer = new StringBuffer(10000);
        }
    }

    public void end() throws IOException {
        if ( this.rowBuffer.length() > 0 ) {
            this.gmlFile.write(this.rowBuffer.toString());
        } 
        String srsNameEnv = "";
        if ( this.attributeFlavour == Constants.XMLAttributeFlavour.FME )
            srsNameEnv = "srsName=\"EPSG:" + this.prevSRID +  "\" ";
        else
            srsNameEnv = "srsName=\"urn:" + this.srsName + "\" ";
        String srsDimEnv = ( ( this.attributeFlavour == Constants.XMLAttributeFlavour.FME ) ? "" : ( "srsDimension=\"" + srsDimension + "\"" )) ;
        gmlFile.write("<gml:boundedBy>" + newLine +  
              "  <gml:Envelope " + srsNameEnv + srsDimEnv + ">" + newLine + 
              "    <gml:lowerCorner>" + this.envelope.getMinX() + " " + envelope.getMinY() + "</gml:lowerCorner>" + newLine + 
              "    <gml:upperCorner>" + this.envelope.getMaxX() + " " + envelope.getMaxY() + "</gml:upperCorner>" + newLine + 
              "  </gml:Envelope>" + newLine + 
              "</gml:boundedBy>" + newLine);
        if ( this.attributeFlavour == Constants.XMLAttributeFlavour.OGR ) 
            this.gmlFile.write("</ogr:FeatureCollection>");
        else
            this.gmlFile.write("</gml:FeatureCollection>");
        this.gmlFile.flush();
    }
    
    private String getXMLFlavourPrefix(Constants.XMLAttributeFlavour _flavour) {
        switch (_flavour) {
        case OGR : return "ogr";
        case FME : return "fme";
        default : return "gml";
        }
    }
    
    public int getRowCount() {
      return this.row;
    }

    public void close() {
        try {
            this.gmlFile.close();
            this.gmlFile = null;
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
        return this.totalRows ;
    }

	public int getGeoColumn() {
		return geoColumn;
	}

	public void setGeoColumn(int geoColumn) {
		this.geoColumn = geoColumn;
	}

	public boolean isNeedsIdentifier() {
		return needsIdentifier;
	}

	public void setNeedsIdentifier(boolean needsIdentifier) {
		this.needsIdentifier = needsIdentifier;
	}
}
