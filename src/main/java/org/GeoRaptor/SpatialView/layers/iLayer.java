package org.GeoRaptor.SpatialView.layers;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.GeoRaptor.Constants;
import org.GeoRaptor.Constants.SDO_OPERATORS;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.tools.PropertiesManager;

import oracle.spatial.geometry.JGeometry;

public interface iLayer  {
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

    String getSDOFilterParameters();

    void setSQL(String _SQL);

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

    iLayer createCopy() throws Exception;

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

	void setGeometryType(String string);
	
	void setGeometryType(Constants.GEOMETRY_TYPES _geometryType);

	String getSRID();

	int getSRIDAsInteger();

	Constants.SRID_TYPE getSRIDType();

	Connection getConnection();

	void setMBR(Envelope mbr);

	boolean isGeodetic();

	void setView(SpatialView spatialView);

	String getConnectionName();

	boolean isConnectionOpen();

	boolean openConnection();

	String getConnectionDisplayName();

	String getGeoColumn();

	String getSchemaName();

	String getObjectName();

	void setGeoColumn(String trim);

	void setPrecision(String text);

	void setSRIDType(String string);

	void setConnectionName(String connectionName);

	MetadataEntry getMetadataEntry();

	void setStyling(Styling _styling);

	public PropertiesManager getPropertyManager();

	void setGeometry(JGeometry _geometry);

	void setBufferDistance(double bufferDistance);

	void setBuffered(boolean b);

	void setRelationshipMask(String relationshipMask);

	void setSdoOperator(SDO_OPERATORS sdoOperator);

	void setPrecision(int precision);

	void setKeyColumn(String _column);
	String getKeyColumn();
	
}

