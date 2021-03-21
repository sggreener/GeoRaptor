package org.GeoRaptor.SpatialView.layers;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.GeoRaptor.Constants;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;

import oracle.spatial.geometry.JGeometry;

public interface iLayer {
    static final String CLASS_NAME = null;

    String getClassName();

    Styling getStyling();

    String getStylingAsString();

    Preferences getPreferences();

    String toXML();

    void savePropertiesToDisk();

    boolean equals(Object obj);

    SpatialView getSpatialView();

    String getLayerNameAndConnectionName();

    String getLayerName();

    void setLayerName(String _layerName);

    void setConnection(String _connName);

    String getDesc();

    void setDesc(String _desc);

    String getVisibleName();

    void setVisibleName(String _visibleName);

    boolean is_draw();

    boolean isDraw();

    void setDraw(boolean _draw);

    void setObjectType(String _objectType);

    boolean isView();

    void setIndex();

    void setIndex(String _hasIndex);

    void setIndex(boolean _indexExists);

    boolean hasIndex();

    boolean setLayerMBR(Envelope _defaultMBR, int _targetSRID);

    /**
     * Create SQL for given Geometry column and table name
     */
    String getInitSQL(String _geoColumn);

    void setInitSQL();

    String getSDOFilterClause();

    /**wrapSQL
     * Takes basic SQL statement and wraps it in an inline view for use in querying and identifying geometry objects.
     * @param _sql
     * @return wrapped SQL statement.
     */
    String wrapSQL(String _sql);

    /**
     * @param SQL
     */
    void setSQL(String _SQL);

    /**
     * getLayerSQL - for XML use only
     * @return String
     */
    String getLayerSQL();

    String getSQL();

    Envelope getMBR();
    
    void setMBRRecalculation(boolean _recalc);

    boolean getMBRRecalculation();

    void setNumberOfFeatures(long _number);

    long getNumberOfFeatures();

    boolean drawLayer(Envelope _mbr, Graphics2D _g2);

    LinkedHashMap<String, String> getColumnsAndTypes(boolean _onlyNumbersDatesAndStrings,
                                                     boolean _fullDataType) throws SQLException;

    void callDrawFunction(Struct _struct, String _label, String _shadeValue, String _pointColorValue,
                          String _lineColorValue, int _pointSizeValue, double _rotationAngle) throws SQLException,
                                                                                                     IOException;

    void callDrawFunction(JGeometry _geo, String _label, String _shadeValue, String _pointColorValue,
                          String _lineColorValue, int _pointSizeValue, double _rotationAngle) throws IOException;

    org.GeoRaptor.SpatialView.layers.iLayer createCopy() throws Exception;

    void setMinResolution(boolean _minResolution);

    boolean getMinResolution();

    int getPrecision(boolean _calculate);

    double getTolerance();

    void setResultFetchSize(int _resultFetchSize);

    int getResultFetchSize();

    SVSpatialLayerDraw getDrawTools();

    SpatialView getView();

    boolean getProject();

    void setProject(boolean _yes, boolean _calcMBR);

    void setFetchSize(int _fetchSize);

    int getFetchSize();

    ArrayList<QueryRow> queryByPoint(Point2D _worldPoint, int _numSearchPixels);
    
	Constants.GEOMETRY_TYPES getGeometryType();

	// Wrappers over SVLayer
    //	
 	int getSRIDAsInteger();

	Connection getConnection();

	void setMBR(Envelope mbr);

	boolean isGeodetic();

	void setView(SpatialView spatialView);

	String getSRID();

	String getConnectionName();

	Constants.SRID_TYPE getSRIDType();

	boolean isConnectionOpen();

	boolean openConnection();

	String getConnectionDisplayName();

	String getGeoColumn();

	String getSchemaName();

	String getObjectName();

	void setGeoColumn(String trim);

	void setPrecision(String text);

	void setSRIDType(String string);

	void setGeometryType(String string);

	void setConnectionName(String connectionName);

	MetadataEntry getMetadataEntry();

	boolean executeDrawQuery(PreparedStatement _pStatement, String _sql2Debug, Graphics2D _g2);

}

