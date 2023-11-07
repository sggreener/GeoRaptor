package org.GeoRaptor.io.Export;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;

import javax.sql.RowSetMetaData;

import org.GeoRaptor.Constants;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.sql.OraRowSetMetaDataImpl;
import org.GeoRaptor.tools.GeometryProperties;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.geotools.util.logging.Logger;

public class GeoJSONExporter 
implements IExporter 
{
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.GeoJSONExporter");

	private BufferedWriter GeoJsonFile = null;
    private StringBuffer     rowBuffer = null;
    private String attributeProperties = null;
	private String     GeoJsonFilename = null;
	private int                    row = 0;
	private int              totalRows = 0;
	private int                 commit = 0;
	private int              geoColumn = 0;
	private String       geoColumnName = null;
	private String            geometry = null;  // geoJSON for each row
	private boolean          writeBBOX = true;
	private String                bbox = null;  // geoJSON bbox for each row
	private String          attributes = null;  // Attributes for each row
	private boolean    needsIdentifier = false;
	private Envelope               mbr = null;
	DecimalFormat            formatter = null;
    private static String      newLine = System.getProperty("line.separator");

    public  static final String   geometryCollectionOption = "GeometryCollection";
    private static final String             geometryOption = "geometry";

    public  static final String            noFeatureOption = "NO_FEATURES";
    public  static final String    featureCollectionOption = "FeatureCollection";
    public  static final String              featureOption = "Feature";

    private Constants.renderType savedSdoGeometryVisualFormat = Constants.renderType.GEOJSON;
    
    public GeoJSONExporter(Connection _conn,
                           String     _fileName,
                           int        _totalRows)
    {
    	super();
    	this.totalRows           = _totalRows;
        this.GeoJsonFilename     = _fileName;    
    	this.formatter           = Tools.getDecimalFormatter(8);
    	this.writeBBOX           = false;  // Write BBOX of each geometry
    	this.needsIdentifier     = true;
    	this.attributeProperties = "\"properties\": {";
    	this.mbr                 = new Envelope(Preferences.getInstance().getPrecision());
    	// Save user visual format for use in sdo_geometry attributes
        this.savedSdoGeometryVisualFormat = Preferences.getInstance().getSdoGeometryVisualFormat();
    }

    public void setWriteBBOX(boolean _write) {
    	this.writeBBOX = _write;
    }
    
    public boolean getWriteBBOX() {
    	return this.writeBBOX;
    }
    
	@Override
	public void setGenerateIdentifier(boolean _identifier) {
        this.needsIdentifier = _identifier;
	}

	public boolean getNeedsIdentifier() {
		return this.needsIdentifier;
	}

	public int getGeoColumn() {
		return this.geoColumn;
	}
	
	@Override
	public String getFileName() {
        return this.GeoJsonFilename; 
	}

	@Override
	public void setFileName(String _fileName) {
        this.GeoJsonFilename = _fileName;    
	}

	@Override
	public void setBaseName(String _baseName) {
	}

	@Override
	public void setAttributeFlavour(String _flavour) {
	}

	@Override
	public String getFileExtension() {
		return "geojson";
	}

	@Override
	public void setTotalRows(int _totalRows) {
        this.totalRows = _totalRows;
	}

	@Override
	public int getTotalRows() {
		return this.totalRows==0?this.row:this.totalRows;
	}

	@Override
	public void setCommit(int _commit) {
	      this.commit = _commit;
	}

	@Override
	public int getCommit() {
		return this.commit;
	}

	@Override
    public void setGeoColumnIndex(int _geoColumnIndex) {
        this.setGeoColumn(_geoColumnIndex);
    }
    
	public void setGeoColumn(int geoColumn) {
		this.geoColumn = geoColumn;
	}

	@Override
    public void setGeoColumnName(String _geoColumnName) {
        this.geoColumnName = _geoColumnName;
    }

    public String getGeoColumnName() {
        return this.geoColumnName;
    }

	@Override
	public int getRowCount() {
	      return this.row;
	}

	@Override
	public GeometryProperties getGeometryProperties() {
		return null;
	}

	@Override
	public void setGeometryProperties(GeometryProperties _geometryProperties) {
	}

	@Override
	public LinkedHashMap<Integer, RowSetMetaData> getExportMetadata() {
		return null;
	}

	@Override
	public void setExportMetadata(LinkedHashMap<Integer, RowSetMetaData> _exportMetadata) {
	}
	
    @SuppressWarnings("unused")
	private String writeNumber(double d) {
        return formatter!=null ? formatter.format(d) : String.valueOf(d);
    }
    
	// ******************************************************
	
	@Override
	public void start(String _encoding) throws Exception {
        this.rowBuffer = new StringBuffer(100000);
        if (Strings.isEmpty(this.GeoJsonFilename) ) {
            throw new IOException("Filename not set");
        }
        this.GeoJsonFile = new BufferedWriter(new FileWriter(this.GeoJsonFilename));
        this.row = 0;
        // FeatureCollection always used
        this.rowBuffer.append("{" + newLine);
        this.rowBuffer.append("\"type\": \"FeatureCollection\",");
        this.rowBuffer.append("\"features\": [ " + newLine);
	}

	@Override
	public void startRow() throws IOException 
	{
        this.geometry   = "";
        this.attributes = "";
	}

	@Override
	public void printColumn(Object _object, 
			 OraRowSetMetaDataImpl _columnMetaData) 
	throws SQLException 
	{
		// This is called for each column/attribute in the result set's row
		// So, sdo_geometry could be the first, middle or last attribute.
		// Save sdo_geometry and attributes for writing at row end.
		//
        Struct stValue = null;
        try {
            // Mappable column?
            //
            // Catalog name holds name of actual geometry column
            if (!Strings.isEmpty(_columnMetaData.getCatalogName(1))
            	&&
            	_columnMetaData.getCatalogName(1).equalsIgnoreCase(this.getGeoColumnName())) 
            {
                if ( _columnMetaData.getColumnTypeName(1).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) )
                {
                	this.geometry = "";
                    stValue = (Struct)_object;
                    if ( stValue == null ) {
                        LOGGER.warn("NULL Geometry: No GeoJson written for row " + (row+1));
                        this.geometry = "";
                        return;
                    }
                    // Switch to GeoJSON writer. 
                    Preferences.getInstance().setSdoGeometryVisualFormat(Constants.renderType.GEOJSON);
                    
                    // Create GeoJSON representation of sdo_geometry
                    this.geometry = SDO_GEOMETRY.getGeometryAsString(stValue);
                    
                    // Switch back for non-mapping sdo_geometry attributes 
                    Preferences.getInstance().setSdoGeometryVisualFormat(this.savedSdoGeometryVisualFormat);
                    
                    // Get individual geometry MBR
                	Envelope gMBR = SDO_GEOMETRY.getGeoMBR(stValue);
                	
                	// Add to whole dataset MBR
                	this.mbr.setMaxMBR(gMBR); 
                    if ( this.getWriteBBOX() ) {
                    	// Record bbox for individual geometry
                    	this.bbox = gMBR.toGeoJSON(); 
                    }
                }
            } else { 
            	// Process Attribute column
            	String obj = "";
            	if (_columnMetaData.getColumnTypeName(1).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY)) {
                	// Any secondary SDO_GEOMETRY columns are converted to a String
                    if ( _object instanceof Struct )
                    	obj = SDO_GEOMETRY.getGeometryAsString((Struct)_object); // Uses this.savedSdoGeometryVisualFormat
                    else if (_object instanceof String)
                    	obj = (String)_object;
                    else
                    	obj = "";
            	} else {
            		obj = (String)_object;
            	}
                String columnName = _columnMetaData.getColumnName(1);
            	/**
                  *      "properties": {
                  *        "prop0": "value0",
                  *        "prop1": 0.0
                  *      }
                  */
                columnName = "\"" + columnName + "\": ";
                // No quotes for numbers
                if ( ! Tools.isNumericType(_columnMetaData.getColumnType(1)) )
                	obj = "\"" + obj + "\"";
                if ( Strings.isEmpty(this.attributes) )
                	this.attributes += columnName + obj;
                else
                	this.attributes += "," + columnName + obj;                    
            }
        } catch (Exception e) {
            LOGGER.warn("GeoJSONExporter: printColumn(object,metadata) = " + e.getMessage());
        }
	}

	@Override
	public void endRow() throws IOException {
        row++;
        if ( ! Strings.isEmpty(this.geometry) )
        {
            // If we have attributes write the Feature tag
            //
        	if ( ! Strings.isEmpty(this.attributes) ) { 
                this.rowBuffer.append((this.row==1?"":",") + 
                                      "{ \"type\":\"" + featureOption + "\",");
                if ( this.needsIdentifier )
                	this.rowBuffer.append(" \"id\": \"" + this.row + "\"," );
        	}
            if ( this.writeBBOX )
                this.rowBuffer.append(this.bbox + "," );
            this.rowBuffer.append(" \"" + geometryOption + "\": " );
        	this.rowBuffer.append(this.geometry);
        	if ( ! Strings.isEmpty(this.attributes) )
        	{
        		this.rowBuffer.append(", " + this.attributeProperties );
          		  this.rowBuffer.append(this.attributes );
                this.rowBuffer.append("}");
        	}
        	this.rowBuffer.append("}" + newLine);
        }

        if ( (row % getCommit()) == 0 ) {
        	this.GeoJsonFile.write(this.rowBuffer.toString());
            this.rowBuffer = new StringBuffer(100000);
            this.GeoJsonFile.flush();
        }
	}

	@Override
	public void end() throws IOException 
	{
        // Write total BBOX of whole export data (this.mbr)
        this.rowBuffer.append("]");
        this.rowBuffer.append("," + this.mbr.toGeoJSON());
        this.rowBuffer.append("\n}");
        this.GeoJsonFile.write(this.rowBuffer.toString());
        this.GeoJsonFile.flush();
	}

	@Override
	public void close() {
		try 
		{
			this.GeoJsonFile.close();
			this.GeoJsonFile = null;
		} catch (IOException ioe) {
        }
	}

}
