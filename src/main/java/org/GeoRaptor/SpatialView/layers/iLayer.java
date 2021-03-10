package org.GeoRaptor.SpatialView.layers;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import java.io.IOException;

import java.sql.SQLException;
import java.sql.Struct;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.GeoRaptor.Constants;
import org.GeoRaptor.Preferences;
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

    void setMBRRecalculation(boolean _recalc);

    boolean getMBRRecalculation();

    void setNumberOfFeatures(long _number);

    long getNumberOfFeatures();

    /**
     * Execute SQL and draw data on given graphical device
     * @param _mbr MBR coordinates
     * @param _g2 graphical device
     * @return if return false, something was wrong (for example, Connection with DB faild)
     */
    boolean drawLayer(Envelope _mbr, Graphics2D _g2);

    LinkedHashMap<String, String> getColumnsAndTypes(boolean _onlyNumbersDatesAndStrings,
                                                     boolean _fullDataType) throws SQLException;

    /**
     * Convert STRUCT object to JGeometry and call drawX function base on Geometry type
     * @history Simon Greener April 2010.
     * Added call to draw geometry via new java.awt.Shape aware drawGeometry funciton
     * @history Simon Greener May 31st 2010.
     * Moved setting of graphics2D of drawTools to calling function
     * @history Simon Greener June 2nd 2010.
     * Got rid of super.lastReadLayerMBR.setMBR(geo.getMBR())
     * because we are processing single geometries and NOT the whole layer.
     * Also, geo.getMBR() returns different arrays depending on whether xy or xyz etc.
     * - Also moved setting of temporary layer transparency to own function
     * and to before this function is called (for each and all geometries)
     */
    void callDrawFunction(Struct _struct, String _label, String _shadeValue, String _pointColorValue,
                          String _lineColorValue, int _pointSizeValue, double _rotationAngle) throws SQLException,
                                                                                                     IOException;

    void callDrawFunction(JGeometry _geo, String _label, String _shadeValue, String _pointColorValue,
                          String _lineColorValue, int _pointSizeValue, double _rotationAngle) throws IOException;

    /**
     * Create copy of current class.
     */
    org.GeoRaptor.SpatialView.layers.SVSpatialLayer createCopy() throws Exception;

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

    /**
     * @history Simon Greener April 13th 2010
     * - Reorganised SQL
     * - Moved to use of new Constants class
     * @history Simon Greener June 7th 2010
     * - Added
     * ORDER BY
     * SDO_NN_DISTANCE ancillary operator
     * - etc to try and address John O'Toole's partition problem
     * @history Simon Greener December 15th 2010
     * - Fully parameterized queries.
     * - Changed method of calculating cutoff distance for geodetic data
     * @history Simon Greener December 15th 2010
     * - New approach that honors current SQL defining layer
     */
    ArrayList<QueryRow> queryByPoint(Point2D _worldPoint, int _numSearchPixels);
}
