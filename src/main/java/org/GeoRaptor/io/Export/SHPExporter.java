package org.GeoRaptor.io.Export;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.LinkedHashMap;

import javax.sql.RowSetMetaData;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Messages;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SpatialViewPanel;
import org.GeoRaptor.sql.OraRowSetMetaDataImpl;
import org.GeoRaptor.tools.FileUtils;
import org.GeoRaptor.tools.GeometryProperties;
import org.GeoRaptor.tools.JTSGeometry;
import org.GeoRaptor.tools.Strings;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.GeoRaptor.util.logging.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.oracle.OraReader;
import org.xBaseJ.xBaseJException;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.DateField;
import org.xBaseJ.fields.Field;
import org.xBaseJ.fields.LogicalField;
import org.xBaseJ.fields.MemoField;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.fields.PictureField;


public class SHPExporter implements IExporter {

    private static final Logger LOGGER = org.GeoRaptor.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.SHPExporter");

    private Preferences                     geoRaptorPreferences = null;

    private ResultSet                                  resultSet = null;
    @SuppressWarnings("unused")
	private int                                        geoColumn = -1;
    private Connection                                      conn = null;
    private Geometry                                       geom = null;
    
    private String                                 geoColumnName = "";
    private GeometryProperties                geometryProperties = null;
    private int                                              row = 0;
    private int                                        totalRows = 0;    
    private LinkedHashMap<Integer,RowSetMetaData> exportMetadata = null;
    
    private DBaseWriter.XBASE_TYPES             attributeFlavour = DBaseWriter.XBASE_TYPES.DBASEIII;
    
    private LinkedHashMap<Integer,Geometry>              geomSet = null;
    private int                                           commit = 100;
    private OraReader                              geomConverter = null; 
    private GeometryFactory                          geomFactory = null;
    private String                                   SHPFilename = "";
    
    protected ShapeType                                  shpType = ShapeType.UNDEFINED; 
    protected ShapefileWriter                          shpWriter = null;
    protected DBaseWriter                            dbaseWriter = null;
    protected boolean                            needsIdentifier = false;
  

    public SHPExporter (Connection _conn,
                        String     _fileName,
                        int        _shapeCount) 
    {
        super();
        this.setConn(_conn);
        this.totalRows = _shapeCount;
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
        setFileName(_fileName);
    }

    public void setGenerateIdentifier(boolean _identifier) {
        this.needsIdentifier = _identifier;
    }

    public void setExportMetadata(LinkedHashMap<Integer, RowSetMetaData> _exportMetadata) {
      this.exportMetadata = _exportMetadata;
    }
  
    public LinkedHashMap<Integer, RowSetMetaData> getExportMetadata() {
      return this.exportMetadata;
    }

    public GeometryProperties getGeometryProperties() {
        return geometryProperties;
    }
    
    public void setGeometryProperties(GeometryProperties _geometryProperties) {
        this.geometryProperties = _geometryProperties;
    }

    public String getFileName() {
        return this.SHPFilename ; 
    }
  
    public void setFileName(String _fileName) {
        SHPFilename = _fileName; 
    }

    public void setBaseName(String _baseName) {
    }

    public void setAttributeFlavour(String _xType) {
      if (Strings.isEmpty(_xType) )
          return;
      try {
          this.attributeFlavour = DBaseWriter.XBASE_TYPES.valueOf(_xType); 
      } catch (Exception e) {
          this.attributeFlavour = DBaseWriter.XBASE_TYPES.DBASEIII;
      }
    }
    
    public String getFileExtension() {
        return null;
    }

    public void setCommit(int _commit) {
      this.commit = _commit;
    }
    
    public int getCommit() {
        return this.commit; 
    }
    
    public void setGeoColumnIndex(int _geoColumnIndex) {
        this.geoColumn = _geoColumnIndex;
    }
    
    public void setGeoColumnName(String _geoColumnName) {
        this.geoColumnName = _geoColumnName;
    }
    
    public int getRowCount() {
        return this.row;
    }
    
    private Geometry Struct2Geometry( Struct _shape ) 
    {
    	LOGGER.debug("Struct2Geometry");
        // Convert Struct to Geometry object
        // PrecisionModel is assigned during conversion
        //
        Geometry jtsGeom = null;
        if ( _shape != null ) {
        	
            try {
                jtsGeom = this.geomConverter.read(_shape);
            } catch (SQLException e) {
                jtsGeom = null;
            }
            
            if ( jtsGeom == null)
            	return jtsGeom;
           
            try {
            	LOGGER.debug("\tjtsGeom.getGeometryType()=" + jtsGeom.getGeometryType().toString());
                if (jtsGeom instanceof LineString) {
                    jtsGeom = this.geomFactory.createMultiLineString(new LineString[] { (LineString) jtsGeom });
                } else if (jtsGeom instanceof Polygon ||
                		   jtsGeom instanceof MultiPolygon) {
                	LOGGER.debug("\tOrienting Rings...");
                    JTSGeometry.orientRings(jtsGeom,this.geomFactory);
                }
            } catch (Exception e) {
            	LOGGER.error("SHPExporter.structToGeometry" + e.getMessage());
            	e.printStackTrace();
            }
        }
        LOGGER.debug("End StructToGeometry");
        return jtsGeom;
    }
        
    @SuppressWarnings("unused")
	private void add(Struct _shape) 
    {
        this.setGeom(Struct2Geometry(_shape));
    }
 
    private void addToGeomSet(Struct _shape) 
    {
    	LOGGER.debug("addToGeomSet: Before Struct2Geometry");
        Geometry jGeom = Struct2Geometry(_shape);
    	LOGGER.debug("addToGeomSet: After Struct2Geometry");
        if ( jGeom == null ) {
          this.geomSet.put(this.row,(Geometry)null);
        } else {
          this.geomSet.put(this.row,jGeom);
        }
    }
    
    private void writeGeomSet() 
    throws IOException 
    {
        // Write the collection
        //
        this.shpWriter.write(this.geomSet);
        this.geomSet = new LinkedHashMap<Integer,Geometry>(this.getCommit());
    }

    public void start(String _encoding) 
         throws Exception
    {
    	LOGGER.debug("start()");
        this.row = 0;
        try {
            if ( this.geomSet == null ) {
                this.geomSet = new LinkedHashMap<Integer,Geometry>(this.getCommit());
            }
            // Create required sdo_geometry to shape conversion functions
            //
            SpatialViewPanel svp = SpatialViewPanel.getInstance();
            SpatialView    sView = svp.getActiveView();
            this.geomFactory     = new GeometryFactory(new PrecisionModel(sView.getPrecision(false)));
            this.geomConverter   = new OraReader(this.geomFactory);

            if (Strings.isEmpty(SHPFilename) ) {
                throw new Exception("Shape filename not set");
            }
            String dirName    = FileUtils.getDirectory(this.SHPFilename);
            String fNameNoExt = FileUtils.getFileNameFromPath(this.SHPFilename,true);
            this.shpType = (ShapeType)this.geometryProperties.getShapefileType(true);
LOGGER.debug("\t*******");
LOGGER.debug("\tshpfileType="+this.shpType + " -- shapeType " + this.geometryProperties.getShapeType().toString());
            // Temporarily force measured shape output ordinate to the z ordinate
            if ( this.shpType.hasMeasure() ) {
                // A z always has an id value 10 less than m
                this.shpType = ShapeType.forID(this.shpType.id-10);
                LOGGER.debug("start: Writing " + ShapeType.forID(this.shpType.id+10).toString() + " shapes as " + this.shpType.toString());
            } else if ( this.shpType.id == ShapeType.UNDEFINED.id ) {
                throw new Exception("ShapefileWriter: Unknown or unsupported shapeType (" + this.shpType.toString() + ") provided.");
            }
            this.shpWriter   = new ShapefileWriter(dirName, fNameNoExt, this.shpType, this.getTotalRows());
            this.dbaseWriter = new DBaseWriter(this.needsIdentifier);
            this.dbaseWriter.setXBaseType(this.attributeFlavour);
            this.dbaseWriter.createDBF(FileUtils.FileNameBuilder(dirName, fNameNoExt, "dbf"),_encoding);
            this.dbaseWriter.createHeader(this.exportMetadata,this.geoColumnName);
        } catch (ShapefileException se) {
            throw new Exception("Shapefile Exception " + se.getMessage());
        } catch (FileNotFoundException fnfe) {
            throw new Exception("Unable to access directory for writing: "+fnfe.getMessage());
        } catch (IOException ioe) {
            throw new Exception("Error initialising output streams for writing: "+ioe.getMessage());
        } catch (xBaseJException xbJe) {
            throw new Exception("(XBase) Problem creating DBase file: "+xbJe.getMessage());          
        } catch (Exception e) {
            throw new Exception("Error creating shapefile/dbase file: "+ e.getMessage());
        }
    }

    public void startRow() 
    throws IOException 
    {
    	LOGGER.debug("startRow()");
        if ( this.dbaseWriter.needsRecordIdentifier() ) {
            Field field;
            try {
                field = this.dbaseWriter.getField(this.dbaseWriter.getRecordIdentiferName());
                if (field!=null) {
                    ((NumField)field).put(row+1);
                    this.dbaseWriter.setField(field);
                }
            } catch (Exception e) {
                throw new IOException("SHP: Exception writing " + this.dbaseWriter.getRecordIdentiferName() + " value " + e.getMessage());
            }
        }
    }

    public void printColumn(Object                _object, 
                            OraRowSetMetaDataImpl _columnMetaData) 
    throws SQLException
    {
    	LOGGER.debug("printColumn()");
        String columnTypeName = "";
        try {
            columnTypeName = _columnMetaData.getColumnTypeName(1); // For use in catch()
            LOGGER.debug("printColumn: Column being written is " + _columnMetaData.getColumnName(1) + " of Type " + columnTypeName);
            // Check if Mappable column
            //
            if (!Strings.isEmpty(_columnMetaData.getCatalogName(1)) )   // Catalog name holds name of actual Geometry column 
            { 
                if ( _columnMetaData.getColumnTypeName(1).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) )
                {
                	LOGGER.debug("Before addToGeomSet");
               		this.addToGeomSet((Struct)_object);
               		LOGGER.debug("After addToGeomSet - size is " + this.getRowCount());
                    return;
                }
            }
        }
        catch (Exception e) {
    		LOGGER.debug("printColumn(" + columnTypeName + "), Row(" + (row+1) + ") = " + e.getMessage());
    		LOGGER.error("Failed to add geometry to GeomSet");
      	  e.printStackTrace();
      	  return;
        }
        
        // Is not mappable column, so process attribute column
        // 
        char fieldType = ' ';
        try {
            String objectString = (String)_object;
			LOGGER.debug(_columnMetaData.getColumnName(1) +"," + 
			             _columnMetaData.getColumnDisplaySize(1) + "," +
			             _columnMetaData.getColumnType(1) + "/" + 
			             _columnMetaData.getColumnTypeName(1) +",("+
			             _columnMetaData.getPrecision(1)+ "," +
			             _columnMetaData.getScale(1) + ") => " + 
			             objectString);
            if ( ! DBaseWriter.isSupportedType(_columnMetaData.getColumnType(1),
                                               _columnMetaData.getColumnTypeName(1)) ) {
                LOGGER.warn(_columnMetaData.getColumnName(1) + "'s data type (" + _columnMetaData.getColumnTypeName(1) + ") not supported for dBase field (value is " + objectString + ")");
                return;
            }
            
            // String has been converted by calling program into a string.
            // Regardless as to xBase field data type all field data are put as strings
            //
            Field field = this.dbaseWriter.getField(_columnMetaData.getColumnName(1));                    
            if ( field == null ) {
                LOGGER.warn("Cannot find field in dBase file for column " + _columnMetaData.getColumnName(1));
                return;
            }
            fieldType = field.getType();
            switch (field.getType())
            {
              case 'C': ((CharField)field).put(objectString.substring(0,Math.min(field.getLength(),objectString.length()))); break;
              case 'F': ((NumField)field).put(objectString);        break;
              case 'L': ((LogicalField)field).put(Strings.isEmpty(objectString)?false:Boolean.valueOf(objectString)); break;
              case 'M': ((MemoField)field).put(objectString);       break;
              case 'P': ((PictureField)field).put((byte[])_object); break;
              case 'N': if (_columnMetaData.getPrecision(1)!=0 && _columnMetaData.getScale(1)==0) {
                          ((NumField)field).put(objectString); 
                        } else {
                          ((NumField)field).put(objectString);   
                        }
                        break;
              case 'D': String dateString = (objectString.indexOf(' ')!=-1 ? objectString.substring(0, objectString.indexOf(' ')) : objectString); 
                        // Date.valueOf() expects "yyyy-mm-dd"
                        ((DateField)field).put(this.dbaseWriter.getDateFormat().format(Date.valueOf(dateString)));  
                        break;
            }
            this.dbaseWriter.setField(field);
        } catch (Exception e) {
            Messages.log("Conversion of DBase Attribute " + 
                         _columnMetaData.getColumnName(1) + "/" + _columnMetaData.getColumnTypeName(1) + "(" +_columnMetaData.getScale(1)+ ")/" + fieldType + 
                         " failed at row " + (row+1) + ": " + 
                         e.getMessage());
        }
    }

    public void endRow() 
    throws IOException 
    {
    	LOGGER.debug("endRow()");
        this.row++;
        
        // Write geometry objects only if we have hit the commit point
        //
        LOGGER.debug("endRow: this.row="+this.row+" geomSet.size= " + this.geomSet.size() + "  " + getCommit());
        if ( this.geomSet.size() == getCommit() ) {
            this.writeGeomSet();
        }
        // For now, write each and every DBF record
        //
        try {
            this.dbaseWriter.write();
        } catch (xBaseJException e) {
            throw new IOException("DBase file write error " + e.getMessage());
        }
    }

    public void end() 
    throws IOException 
    {
    	LOGGER.debug("end()");
    	
    	// make sure to write the last Geometry set feature...
		//
		if ( this.geomSet.size() > 0 ) {
		      this.writeGeomSet();
		}
		/**
		* Get reference to single instance of GeoRaptor's Preferences
		*/
		if ( !Strings.isEmpty(geoRaptorPreferences.getPrj()) ) {
		      String dirName    = FileUtils.getDirectory(this.SHPFilename);
		      String fNameNoExt = FileUtils.getFileNameFromPath(this.SHPFilename,true);
		      // Only write Prj is SRID is value
		      // Should be in ExporterDialog
		      if ( getGeometryProperties().getSRID() != Constants.SRID_NULL ) {
		          writePrjFile(FileUtils.FileNameBuilder(dirName,fNameNoExt,"prj"),
		                       geoRaptorPreferences.getPrj());
		      }
		}
	}
	
	/**
	 * @param fullyQualifiedFileName - filename including directory path and extension. 
	 * @param prjString - Actual string containing PRJ file's contents.
	 * @throws FileNotFoundException - if there is an error creating the file.
	 * @throws IOException - if there is an error writing to the file.
	 * @name writePrjFile() 
	 * @description Writes actual PRJ file 
	*/
	protected static void writePrjFile(String   fullyQualifiedFileName,
	                                   String   prjString ) 
	throws IOException 
	{
    	LOGGER.debug("writePrjFile()");

	    File prjFile;
	    prjFile = new File(fullyQualifiedFileName);
	    Writer prjOutput = new BufferedWriter(new FileWriter(prjFile));
	    try {
	         //FileWriter always assumes default encoding is OK!
	         prjOutput.write( prjString );
	    }
	    catch (IOException ioe) {
	        throw new IOException("Error writing PRJ file.", ioe);
	    }
	    finally {
	         prjOutput.close();
	     }
	    prjOutput.close();
    }

    public void close() 
    {
    	LOGGER.debug("close()");
        try {
            // Close shapefile (writes header)
            //
            if (this.shpWriter != null) {
                this.shpWriter.close(); 
                this.shpWriter = null;
            }
            if (this.dbaseWriter != null) {
                this.dbaseWriter.close();
                this.dbaseWriter = null;
            }
        } catch ( IOException ioe ) {
        	if (this.shpWriter != null)
        		LOGGER.error("Failed to close SHP file: " + ioe.getMessage());
        	else if ( this.dbaseWriter!=null)
        		LOGGER.error("Failed to close DBF file: " + ioe.getMessage());
        	else
        		LOGGER.error("Failed to close SHP or DBF file: " + ioe.getMessage());
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

	public Geometry getGeom() {
		return geom;
	}

	public void setGeom(Geometry geom) {
		this.geom = geom;
	}

	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public void setResultSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}
}
