package org.GeoRaptor;
import java.awt.Color;

import javax.swing.JSplitPane;

import org.GeoRaptor.Preferences;
import org.GeoRaptor.tools.Strings;

import oracle.javatools.data.HashStructure;
import oracle.javatools.data.HashStructureAdapter;
import oracle.javatools.data.PropertyStorage;

import oracle.jdbc.OracleTypes;

/**
 * 
 * @author Bessie Gong 
 * @version 24 Jul 2019
 *	
 *	The entity of preference
 */
public class Preferences extends HashStructureAdapter {

	private static final String DATA_KEY = "org.GeoRaptor.Preferences";

	private Preferences(final HashStructure hash) {
		super(hash);
	}

	public static Preferences getInstance() {
        return getInstance(oracle.ide.config.Preferences.getPreferences());
    }
	
	public static Preferences getInstance(final PropertyStorage prefs) {
		return new Preferences(HashStructureAdapter.findOrCreate(prefs, Preferences.DATA_KEY));
	}

    public void setRandomRendering(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_LAYER_RENDER_RANDOM, _value);
        }
    }
    
    public boolean isRandomRendering() {
        return _hash.getBoolean(Constants.KEY_LAYER_RENDER_RANDOM, 
                                Constants.VAL_LAYER_RENDER_RANDOM);
    }

    public void setDefGeomColName(String _name) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_GEOMETRY_COLUMN_NAME, _name);
        }
    }

    public String getDefGeomColName() {
        return _hash.getString(Constants.KEY_GEOMETRY_COLUMN_NAME, 
                               Constants.VAL_GEOMETRY_COLUMN_NAME);
    }

    public void setSQLSchemaPrefix(boolean _prefix) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_USER_SQL_SCHEMA_PREFIX, _prefix);
        }
    }

    public boolean getSQLSchemaPrefix() {
        return _hash.getBoolean(Constants.KEY_USER_SQL_SCHEMA_PREFIX, 
                                Constants.VAL_USER_SQL_SCHEMA_PREFIX);
    }
    
    public void setDBFColumnShorten(boolean _begin) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_DBASE_COLUMN_SHORTEN_BEGIN, _begin);
        }
    }
    
    public boolean isDBFShortenBegin10() {
        return _hash.getBoolean(Constants.KEY_DBASE_COLUMN_SHORTEN_BEGIN, 
                                Constants.VAL_DBASE_COLUMN_SHORTEN_BEGIN);
    }

    public void setMapScaleVisible(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_MAP_SCALE_VISIBLE, _value);
        }
    }
    
    public boolean isMapScaleVisible() {
        return _hash.getBoolean(Constants.KEY_MAP_SCALE_VISIBLE, 
                                Constants.VAL_MAP_SCALE_VISIBLE);
    }

    public void setDrawQueryGeometry(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_DRAW_QUERY_GEOMETRY, _value);
        }
    }
    
    public boolean isDrawQueryGeometry() {
        return _hash.getBoolean(Constants.KEY_DRAW_QUERY_GEOMETRY, 
                                Constants.VAL_DRAW_QUERY_GEOMETRY);
    }

        
    public void setNumberOfFeaturesVisible(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_NUMBER_FEATURES_VISIBLE, _value);
        }
    }
    
    public boolean isNumberOfFeaturesVisible() {
        return _hash.getBoolean(Constants.KEY_NUMBER_FEATURES_VISIBLE, 
                                Constants.VAL_NUMBER_FEATURES_VISIBLE);
    }

    public void setFastPicklerConversion(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_FAST_PICKLER, _value);
        }
    }
    
    public boolean isFastPicklerConversion() {
        return _hash.getBoolean(Constants.KEY_FAST_PICKLER, 
                                Constants.VAL_FAST_PICKLER);
    }

   // Thumbnail image options
    
    public void setPreviewImageWidth(int _width) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_PREVIEW_IMAGE_WIDTH, _width);
        }
    }
    public int getPreviewImageWidth() {
        return _hash.getInt(Constants.KEY_PREVIEW_IMAGE_WIDTH, 
                            Constants.VAL_PREVIEW_IMAGE_WIDTH);
    }

    public void setPreviewImageHeight(int _height) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_PREVIEW_IMAGE_HEIGHT, _height);
        }
    }
    public int getPreviewImageHeight() {
        return _hash.getInt(Constants.KEY_PREVIEW_IMAGE_HEIGHT, 
                            Constants.VAL_PREVIEW_IMAGE_HEIGHT);
    }

    public void setMinMbrAspectRatio(double _ratio) {
        synchronized (_hash) {
            _hash.putDouble(Constants.KEY_MIN_MBR_ASPECT_RATIO, _ratio);
        }
    }
    public double getMinMbrAspectRatio() {
        return _hash.getDouble(Constants.KEY_MIN_MBR_ASPECT_RATIO, 
                               Constants.VAL_MIN_MBR_ASPECT_RATIO);
    }

    public void setPreviewImageHorizontalLabels(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_PREVIEW_IMAGE_HORIZONTAL_LABELS, _value);
        }
    }
    public boolean isPreviewImageHorizontalLabels() {
        return  _hash.getBoolean(Constants.KEY_PREVIEW_IMAGE_HORIZONTAL_LABELS, 
                                 Constants.VAL_PREVIEW_IMAGE_HORIZONTAL_LABELS);
    }

    public void setPreviewImageVertexLabeling(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_PREVIEW_IMAGE_VERTEX_LABELING, _value);
        }
    }
    public boolean isPreviewImageVertexLabeling() {
        return  _hash.getBoolean(Constants.KEY_PREVIEW_IMAGE_VERTEX_LABELING, 
                                 Constants.VAL_PREVIEW_IMAGE_VERTEX_LABELING);
    }

    public void setPreviewVertexMark(String _markerType) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_PREVIEW_IMAGE_VERTEX_MARK, _markerType);
        }
    }

    public String getPreviewVertexMark() {
        return _hash.getString(Constants.KEY_PREVIEW_IMAGE_VERTEX_MARK, 
                               Constants.VAL_PREVIEW_IMAGE_VERTEX_MARK);
    }

    public void setPreviewVertexMarkSize(int _markerSize) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_PREVIEW_IMAGE_VERTEX_MARK_SIZE, _markerSize);
        }
    }

    public int getPreviewVertexMarkSize() {
        return _hash.getInt(Constants.KEY_PREVIEW_IMAGE_VERTEX_MARK_SIZE, 
                            Constants.VAL_PREVIEW_IMAGE_VERTEX_MARK_SIZE);
    }
    // -----------------------------------------------
    
    public void setGroupingSeparator(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_GROUPING_SEPARATOR, _value);
        }
    }
    
    public boolean getGroupingSeparator() {
        return _hash.getBoolean(Constants.KEY_GROUPING_SEPARATOR, 
                                Constants.VAL_GROUPING_SEPARATOR);
    }
    
    public void setPrefixWithMDSYS(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_MDSYS_PREFIX, _value);
        }
    }
    
    public boolean getPrefixWithMDSYS() {
        return _hash.getBoolean(Constants.KEY_MDSYS_PREFIX, 
                                Constants.VAL_MDSYS_PREFIX);
    }
        
    public static String getDefaultNullValue(int _type) 
    {
        switch (_type)
        {
           case OracleTypes.ROWID         : return "";
           case OracleTypes.NCHAR         : 
           case OracleTypes.NVARCHAR      : 
           case OracleTypes.CHAR          : 
           case OracleTypes.VARCHAR       : return "";
           case OracleTypes.NCLOB         :
           case OracleTypes.CLOB          : return "";
           case OracleTypes.TINYINT       : return "0";
           case OracleTypes.SMALLINT      : return "-32768";
           case OracleTypes.BIGINT        : return "-9223372036854775808";
           case OracleTypes.INTEGER       : return "-2147483648";
           case OracleTypes.FLOAT         :
           case OracleTypes.DOUBLE        :
           case OracleTypes.DECIMAL       :
           case OracleTypes.NUMBER        :
           case OracleTypes.BINARY_DOUBLE : return "-9999999999";
           case OracleTypes.TIMESTAMPTZ   : 
           case OracleTypes.TIMESTAMPLTZ  : 
           case OracleTypes.INTERVALYM    : 
           case OracleTypes.INTERVALDS    : 
           case OracleTypes.DATE          : return "1900-01-01";
           case OracleTypes.TIMESTAMP     :
           case OracleTypes.TIME          : return "120000";
           case OracleTypes.BFILE         : 
           case OracleTypes.RAW           :
           case OracleTypes.BLOB          : 
           case OracleTypes.STRUCT        :
           default                        : return "";
        }
    }

    public String getNullValue(int _type) 
    {
        switch (_type)
        {
           case OracleTypes.ROWID         : return "";
           case OracleTypes.NCHAR         : 
           case OracleTypes.NVARCHAR      : 
           case OracleTypes.CHAR          : 
           case OracleTypes.VARCHAR       : 
           case OracleTypes.NCLOB         :
           case OracleTypes.CLOB          : return getNullString();
           case OracleTypes.TINYINT       : return "0";
           case OracleTypes.SMALLINT      : return "-32768";
           case OracleTypes.BIGINT        : 
           case OracleTypes.INTEGER       : return String.valueOf(getNullInteger());
           case OracleTypes.FLOAT         :
           case OracleTypes.DOUBLE        :
           case OracleTypes.DECIMAL       :
           case OracleTypes.NUMBER        :
           case OracleTypes.BINARY_DOUBLE : return String.valueOf(getNullNumber());
           case OracleTypes.TIMESTAMPTZ   : 
           case OracleTypes.TIMESTAMPLTZ  : 
           case OracleTypes.INTERVALYM    : 
           case OracleTypes.INTERVALDS    : 
           case OracleTypes.DATE          : return getNullDate();
           case OracleTypes.TIMESTAMP     :
           case OracleTypes.TIME          : return "1900-01-01";
           case OracleTypes.BFILE         : 
           case OracleTypes.RAW           :
           case OracleTypes.BLOB          : 
           default                        : return null;
        }
    }
        
    public void setNullString(String _value) {
            synchronized (_hash) {
        _hash.putString(Constants.KEY_NULL_STRING_VALUE, _value);
        }
    }
    public String getNullString() {
        return _hash.getString(Constants.KEY_NULL_STRING_VALUE, 
                               getDefaultNullValue(OracleTypes.VARCHAR));
    }

    public void setNullNumber(double _value) {
            synchronized (_hash) {
        _hash.putDouble(Constants.KEY_NULL_NUMBER_VALUE, _value);
        }
    }
    public double getNullNumber() {
        return _hash.getDouble(Constants.KEY_NULL_NUMBER_VALUE, 
                               Double.valueOf(getDefaultNullValue(OracleTypes.BINARY_DOUBLE)).doubleValue());
    }

    public void setNullInteger(int _value) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_NULL_INTEGER_VALUE, _value);
        }
    }
    public int getNullInteger() {
        return _hash.getInt(Constants.KEY_NULL_INTEGER_VALUE,
                            Integer.valueOf(getDefaultNullValue(OracleTypes.INTEGER)).intValue());
    }

    public void setNullDate(String _value) {
            synchronized (_hash) {
        _hash.putString(Constants.KEY_NULL_DATE_VALUE, _value);
        }
    }
    
    public String getNullDate() {
        return _hash.getString(Constants.KEY_NULL_DATE_VALUE, 
                               getDefaultNullValue(OracleTypes.DATE));
    }

    public void setDbaseNullWriteString(boolean _value) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_DBASE_NULL_FIELD, _value);
        }
    }
    
    public boolean getDbaseNullWriteString() {
        return _hash.getBoolean(Constants.KEY_DBASE_NULL_FIELD, 
                                Constants.VAL_DBASE_NULL_FIELD);
    }
        
    public void setNN(boolean _sdo_nn) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_SDO_NN, _sdo_nn);
        }
    }

    public boolean isNN() {
        return _hash.getBoolean(Constants.KEY_SDO_NN, Constants.VAL_SDO_NN);
    }
    
    public void setLogSearchStats(boolean _stats) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_LOG_SEARCH_STATISTICS, _stats);
        }
    }
    
    public boolean isLogSearchStats() {
        return _hash.getBoolean(Constants.KEY_LOG_SEARCH_STATISTICS, Constants.VAL_LOG_SEARCH_STATISTICS);
    }

    public void setTableCountLimit(int _limit) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_TABLE_COUNT_LIMIT, _limit);
        }
    }
    
    public int getTableCountLimit() {        
      return _hash.getInt(Constants.KEY_TABLE_COUNT_LIMIT, Constants.VAL_TABLE_COUNT_LIMIT);
    }
    
    public void setQueryLimit(int _limit) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_QUERY_LIMIT, _limit);
        }
    }
    
    public boolean isQueryLimited() {
      return this.getQueryLimit()!=0;
    }
    public int getQueryLimit() {
      return _hash.getInt(Constants.KEY_QUERY_LIMIT, Constants.VAL_QUERY_LIMIT);
    }   
    public void setDefaultAssociationXML(String _associationXML) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_ASSOCIATION_XML, _associationXML);
        }
    }

    public String getDefaultAssociationXML() {
      return Constants.VAL_ASSOCIATION_XML;      
    }
    
    public void setCoordsys(String _coordsys) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_TAB_COORDSYS, _coordsys);
        }
    }

    public String getCoordsys() {
        return _hash.getString(Constants.KEY_TAB_COORDSYS, Constants.VAL_TAB_COORDSYS);
    }
    
    public void setPrj(String _prj) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_SHAPEFILE_PRJ, _prj);
        }
    }

    public String getPrj() {
        return _hash.getString(Constants.KEY_SHAPEFILE_PRJ, Constants.VAL_SHAPEFILE_PRJ);
    }
        
    public void setExportDirectory(String _source) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_EXPORT_DIRECTORY, _source);
        }
    }

    public String getExportDirectory() {
        return _hash.getString(Constants.KEY_EXPORT_DIRECTORY, Constants.VAL_EXPORT_DIRECTORY);
    }

    public void setExportType(Constants.EXPORT_TYPE _source) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_EXPORT_TYPE, _source.toString());
        }
    }

    public Constants.EXPORT_TYPE getExportType() {
        String exportType = _hash.getString(Constants.KEY_EXPORT_TYPE, Constants.EXPORT_TYPE.SHP.toString());
        return Constants.EXPORT_TYPE.valueOf(exportType);
    }

    public void setLayerTreeGeometryGrouping(boolean _prefix) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_LAYER_TREE_GEOMETRY_GROUPING, _prefix);
        }    
    }

    public boolean isLayerTreeGeometryGrouping() {
        return _hash.getBoolean(Constants.KEY_LAYER_TREE_GEOMETRY_GROUPING, Constants.VAL_LAYER_TREE_GEOMETRY_GROUPING);
    }

    public void setPanZoomPercentage(int _percentage) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_PAN_ZOOM_PERCENTAGE, _percentage);
        }
    }
    
    public int getPanZoomPercentage() {    
        // 13/4/2011: Max value reduced from 100% to 50%. Must ensure any saved user settings greater
        // than new 50% value are not supplied to application esp PreferencesPanel sldrPanZoomPercentage.
      int pzValue = _hash.getInt(Constants.KEY_PAN_ZOOM_PERCENTAGE, Constants.VAL_PAN_ZOOM_PERCENTAGE);
      return pzValue > Constants.VAL_PAN_ZOOM_PERCENTAGE ? Constants.VAL_PAN_ZOOM_PERCENTAGE : pzValue;
    }
    
    public void setSchemaPrefix(boolean _prefix) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_SCHEMA_PREFIX, _prefix);
        }    
    }

    public boolean isSchemaPrefix() {
        return _hash.getBoolean(Constants.KEY_SCHEMA_PREFIX, Constants.VAL_SCHEMA_PREFIX);
    }

    public void setSdoGeometryBracketType(String _SdoGeometryBracket) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_SDO_GEOMETRY_BRACKETING, _SdoGeometryBracket);
        }
    }

    public Constants.bracketType getSdoGeometryBracketType() {
        try {
            return Constants.bracketType.valueOf(_hash.getString(Constants.KEY_SDO_GEOMETRY_BRACKETING, 
                                                                 Constants.VAL_SDO_GEOMETRY_BRACKETING));
        }
        catch (Exception e) {
            return Constants.bracketType.NONE;
        }
    }

    public boolean isSdoGeometryBracketing() {
        return _hash.getString(Constants.KEY_SDO_GEOMETRY_BRACKETING, 
                               Constants.VAL_SDO_GEOMETRY_BRACKETING).equals(Constants.bracketType.NONE.toString());
    }

    public void setSdoGeometryCoordinateNumbering(boolean _SDOGeometryCoordinateNumbering) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_SDO_GEOMETRY_COORDINATE_NUMBERING, _SDOGeometryCoordinateNumbering);
        }    }

    public boolean isSdoGeometryCoordinateNumbering() {
        return _hash.getBoolean(Constants.KEY_SDO_GEOMETRY_COORDINATE_NUMBERING, Constants.VAL_SDO_GEOMETRY_COORDINATE_NUMBERING);
    }

    public void setLayerMBRSource(String _source) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_LAYER_MBR_SOURCE, _source);
        }
    }

    public String getLayerMBRSource() {
        return _hash.getString(Constants.KEY_LAYER_MBR_SOURCE, Constants.VAL_LAYER_MBR_SOURCE);
    }

    public void setMBRSaveSize(int _MBRSaveSize) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_MBR_SAVE_SIZE, _MBRSaveSize);
        }
    }

    public int getMBRSaveSize() {
        return _hash.getInt(Constants.KEY_MBR_SAVE_SIZE, Constants.VAL_MBR_SAVE_SIZE);
    }

    public void setMinResolution(boolean _minResolution) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_MIN_RESOLUTION, _minResolution);
        }
    }

    public boolean isMinResolution() {
        return _hash.getBoolean(Constants.KEY_MIN_RESOLUTION, Constants.VAL_MIN_RESOLUTION);
    }

    public void setSearchPixels(int _searchPixel) 
    {
        if (_searchPixel < 0) _searchPixel = 0;
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_SEARCH_PIXEL , _searchPixel);
        }
    }
    
    public int getSearchPixels() {
        return _hash.getInt(Constants.KEY_SEARCH_PIXEL, Constants.VAL_SEARCH_PIXEL);
    }

    // ******************** Colours
    public void setMapBackground(Color _mapBackground) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_MAP_BACKGROUND, _mapBackground.getRGB() );
        }
    }
    
    public Color getMapBackground() {
        return new Color(_hash.getInt(Constants.KEY_MAP_BACKGROUND, Constants.VAL_MAP_BACKGROUND ));
    }

    public void setFeatureColour(Color _featureColour) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_FEATURE_COLOUR, _featureColour.getRGB() );
        }
    }
    
    public Color getFeatureColour() {
        return new Color(_hash.getInt(Constants.KEY_FEATURE_COLOUR, Constants.VAL_FEATURE_COLOUR ));
    }

    public void setOrphanColour(Color _colour) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_METADATA_ORPHAN_COLOUR, 
                         _colour.getRGB() );
        }
    }
    
    public Color getOrphanColour() {
        return new Color(_hash.getInt(Constants.KEY_METADATA_ORPHAN_COLOUR, 
                                      Constants.VAL_METADATA_ORPHAN_COLOUR ));
    }

    public void setMissingColour(Color _colour) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_METADATA_MISSING_COLOUR, 
                         _colour.getRGB() );
        }
    }
    
    public Color getMissingColour() {
        return new Color(_hash.getInt(Constants.KEY_METADATA_MISSING_COLOUR, 
                                      Constants.VAL_METADATA_MISSING_COLOUR ));
    }

    public void setCorrectColour(Color _colour) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_METADATA_CORRECT_COLOUR, 
                         _colour.getRGB() );
        }
    }
    
    public Color getCorrectColour() {
        return new Color(_hash.getInt(Constants.KEY_METADATA_CORRECT_COLOUR, 
                                      Constants.VAL_METADATA_CORRECT_COLOUR ));
    }

    public void setHighlightColour(Color _colour) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_METADATA_HIGHLIGHT_COLOUR, 
                         _colour.getRGB() );
        }
    }
    
    public Color getHighlightColour() {
        return new Color(_hash.getInt(Constants.KEY_METADATA_HIGHLIGHT_COLOUR, 
                                      Constants.VAL_METADATA_HIGHLIGHT_COLOUR ));
    }

    public void setSelectionColour(Color _colour) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_SELECTION_COLOUR, 
                         _colour.getRGB() );
        }
    }
    
    public Color getSelectionColour() {
        return new Color(_hash.getInt(Constants.KEY_SELECTION_COLOUR, 
                                      Constants.VAL_SELECTION_COLOUR ));
    }
        
    // **************** END Colours
    
    public void setSRID(String _SRID) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_DEFAULT_SRID, _SRID);
        }
    }

    public String getSRID() {
        return _hash.getString(Constants.KEY_DEFAULT_SRID, Constants.VAL_DEFAULT_SRID);
    }

    public int getSRIDAsInteger() {
        String srid = _hash.getString(Constants.KEY_DEFAULT_SRID, Constants.VAL_DEFAULT_SRID);
        return (srid.equals(Constants.NULL) 
                ? Constants.SRID_NULL 
                : ( Integer.valueOf(srid).intValue())==0
                   ? Constants.SRID_NULL 
                   : Integer.valueOf(srid).intValue());
    }

    public void setSpatialLayerXML( String _spatialLayerXML ) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_LAYERS,_spatialLayerXML);
        }
    }

//    public String getSpatialLayerXML( ) {
//        return _hash.getString(Constants.KEY_LAYERS,Constants.VAL_SPATIAL_LAYER_XML);
//    }

    public void setColourSdoGeomElements(boolean _colourSDOGeometry) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_SDO_GEOMETRY_COLOUR, _colourSDOGeometry);
        }
    }

    public boolean isColourSdoGeomElements() {
        return _hash.getBoolean(Constants.KEY_SDO_GEOMETRY_COLOUR, Constants.VAL_SDO_GEOMETRY_COLOUR);
    }

    public void setColourDimInfo(boolean _colourDimInfo) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_DIMINFO_COLOUR, _colourDimInfo);
        }
    }

    public boolean isColourDimInfo() {
        return _hash.getBoolean(Constants.KEY_DIMINFO_COLOUR, Constants.VAL_DIMINFO_COLOUR);
    }

    public void setSuffixFlag(boolean _suffixFlag) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_INDEX_SUFFIX_FLAG, _suffixFlag);
        }
    }
    
    public boolean isSuffixFlag() {
        return _hash.getBoolean(Constants.KEY_INDEX_SUFFIX_FLAG, Constants.VAL_INDEX_SUFFIX_FLAG);
    }

    public void setAffixString(String _affixString) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_INDEX_AFFIX, _affixString);
        }
    }

    public String getAffixString() {
        return _hash.getString(Constants.KEY_INDEX_AFFIX, Constants.VAL_INDEX_AFFIX);
    }
    
    public void setFetchSize(int _fetchSize) 
    {
        /**
         * If size is < 0, we set it on 0
         */
        if (_fetchSize < 0) _fetchSize = 0;
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_FETCH_SIZE, _fetchSize);
        }
    }
    
    public int getFetchSize() {
        return _hash.getInt(Constants.KEY_FETCH_SIZE, Constants.VAL_FETCH_SIZE);
    }
    
    public void setImageRefreshMS(int _imageRefreshMS) 
    {
        /**
         * Refresh must be > 100
         */
        if (_imageRefreshMS < 100) _imageRefreshMS = 100;
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_IMAGE_REFRESH_MS, _imageRefreshMS);
        }
    }

    public int getImageRefreshMS() {
        return _hash.getInt(Constants.KEY_IMAGE_REFRESH_MS, Constants.VAL_IMAGE_REFRESH_MS);
    }
    
    public void setMainLayerSplitPos(int _mainLayerSplitPos) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_MAIN_LAYER_SPLIT_POS, _mainLayerSplitPos);
        }
    }

    public int getMainLayerSplitPos() {
        return _hash.getInt(Constants.KEY_MAIN_LAYER_SPLIT_POS, 
                            Constants.VAL_MAIN_LAYER_SPLIT_POS);
    }

    public void setMainSplitPos(int _mainSplitPos) {
        synchronized (_hash) {
            _hash.putInt(Constants.KEY_MAIN_SPLIT_POS, _mainSplitPos);
        }
    }

    public int getMainSplitPos() {
        return _hash.getInt(Constants.KEY_MAIN_SPLIT_POS,
                            Constants.VAL_MAIN_SPLIT_POS);
    }

    public void setNewLayerPosition(String _newLayerPosition) {
        String newPosn = Strings.isEmpty(_newLayerPosition) 
                         ? Constants.layerPositionType.TOP.toString() 
                         : _newLayerPosition;        
        // prevent concurrent access issues
         synchronized (_hash) {
             _hash.putString(Constants.KEY_NEW_LAYER_POSITION, newPosn);
         }
    }

    public String getNewLayerPosition() {
        return _hash.getString(Constants.KEY_NEW_LAYER_POSITION,
                               Constants.VAL_NEW_LAYER_POSITION_TOP);
    }

    public void setTOCPosition(String _tocPosition) {
        String newPosn = Strings.isEmpty(_tocPosition) 
                         ? JSplitPane.LEFT 
                         : _tocPosition;
        // prevent concurrent access issues
         synchronized (_hash) {
           _hash.putString(Constants.KEY_TOC_POSITION, newPosn);
         }
    }

    public String getTOCPosition() {
        return _hash.getString(Constants.KEY_TOC_POSITION,
                               Constants.VAL_TOC_POSITION);
    }
    
    public void setSdoGeometryVisualFormat(String _visualFormat) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_SDO_GEOMETRY_VISUAL_FORMAT, _visualFormat.toUpperCase());
        }
    }

    public void setSdoGeometryVisualFormat(Constants.renderType _visualFormat) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_SDO_GEOMETRY_VISUAL_FORMAT, _visualFormat.toString());
        }
    }

    public Constants.renderType getSdoGeometryVisualFormat() {
        try {
            return Constants.renderType.valueOf(_hash.getString(Constants.KEY_SDO_GEOMETRY_VISUAL_FORMAT, 
                                                                Constants.VAL_SDO_GEOMETRY_VISUAL_FORMAT));
        }
        catch (Exception e) {
            return Constants.renderType.SDO_GEOMETRY;
        }
    }

    public Constants.renderType getVisualFormat() {
        try {
              return Constants.renderType.valueOf(_hash.getString(Constants.KEY_SDO_GEOMETRY_VISUAL_FORMAT, 
                                                                  Constants.VAL_SDO_GEOMETRY_VISUAL_FORMAT));
        } catch (Exception e) {
              return Constants.renderType.SDO_GEOMETRY;
        }
    }

    public void setShapePolygonOrientation(Constants.SHAPE_POLYGON_ORIENTATION _ShapePolygonOrientation) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_SHAPE_POLYGON_ORIENTATION, _ShapePolygonOrientation.toString());
        }
    }
    public void setShapePolygonOrientation(String _ShapePolygonOrientation) {
        synchronized (_hash) {
            _hash.putString(Constants.KEY_SHAPE_POLYGON_ORIENTATION, _ShapePolygonOrientation.toUpperCase());
        }
    }
    
    public Constants.SHAPE_POLYGON_ORIENTATION getShapePolygonOrientation() {
        try {
              return Constants.SHAPE_POLYGON_ORIENTATION.valueOf(_hash.getString(Constants.KEY_SHAPE_POLYGON_ORIENTATION, 
                                                                                 Constants.VAL_SHAPE_POLYGON_ORIENTATION));
        } catch (Exception e) {
              return Constants.SHAPE_POLYGON_ORIENTATION.ORACLE;
        }
    }

        
    public void setSdoGeometryFormat(boolean _geometryFormat) {
        synchronized (_hash) {
            _hash.putBoolean(Constants.KEY_SDO_GEOMETRY_PROCESS_FORMAT, _geometryFormat);
        }
    }
    
    public boolean isSdoGeometryProcessingFormat() {
        return _hash.getBoolean(Constants.KEY_SDO_GEOMETRY_PROCESS_FORMAT, Constants.VAL_SDO_GEOMETRY_PROCESS_FORMAT);
    }

}
