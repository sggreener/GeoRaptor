  package org.GeoRaptor.io.Export;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import javax.sql.RowSetMetaData;

import org.GeoRaptor.Constants;
import org.GeoRaptor.sql.OraRowSetMetaDataImpl;
import org.GeoRaptor.tools.GeometryProperties;

public interface IExporter {

    public Connection                                     conn = null;
    public String                                     baseName = "";
    public int                                             row = 1;
    public int                                       totalRows = 0;
    public Constants.XMLAttributeFlavour      attributeFlavour = Constants.XMLAttributeFlavour.OGR;
    public GeometryProperties                geometryProperties = null;
    public LinkedHashMap<Integer,RowSetMetaData> exportMetadata = null;
    public String                                 geoColumnName = "";
  
    public void setGenerateIdentifier(boolean _identifier);
    
    public String getFileName();
    public   void setFileName(String _fileName);

    public void setBaseName(String _baseName);

    public void setAttributeFlavour(String _flavour);

    public String getFileExtension();

    public void setTotalRows(int _totalRows);
    public int  getTotalRows();

    public void setCommit(int _commit);
    public int getCommit();
    
    public void setGeoColumnIndex(int _geoColumnIndex);
    public void setGeoColumnName(String _geoColumnName);

    public void start(String _encoding) throws Exception;

    public void startRow() throws IOException;

    public void printColumn(Object _object, OraRowSetMetaDataImpl _columnMetaData) throws SQLException;

    public void endRow() throws IOException;

    public void end() throws IOException;
    
    public int getRowCount();

    public GeometryProperties getGeometryProperties();
    
    public void setGeometryProperties(GeometryProperties _geometryProperties);

    public LinkedHashMap<Integer, RowSetMetaData> getExportMetadata();
    
    public void setExportMetadata(LinkedHashMap<Integer, RowSetMetaData> _exportMetadata);

    public void close();
    
}
