package org.GeoRaptor;


import java.awt.Color;

import java.util.UUID;

import javax.swing.DefaultListModel;

//import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.tools.Tools;

import org.geotools.data.shapefile.shp.ShapeType;

/**
 * Comment out all content related to the Spatial view
 * @author Bessie Gong 
 * @version 10 Oct 2019
 *
 */
public class Constants {

	public static boolean DEBUG = false;
	public static String DEBUG_PASSWORD = ".G305apt05!";

	public static final String GEORAPTOR = "GeoRaptor";
	public static final String MENU_ITEM_SPATIAL_VIEWER = "Open Map";
	public static final String MENU_ITEM_SHAPEFILE_LOADER = "Load Shapefile";
	public static final String MENU_ITEM_METADATA_MANAGER = "Manage All Metadata";
	public static final String MENU_ITEM_ABOUT_BOX = "About GeoRaptor";

	public static final int SRID_NULL = -1;
	public static final int NULL_SRID = -1;
	public static final String NULL = "NULL";
	public static final String TABLE_COLUMN_SEPARATOR = ".";
	public static final float TRANSPARENT = 0.0f;
	public static final float SOLID = 1.0f;
	public static final int MAX_PRECISION = 10; // decimal places
	public static final int DEFAULT_PRECISION = 3;
	public static final double DEFAULT_TOLERANCE = 0.05; // 5 cm is default for geodetic
	public static final long MILLISECONDS = 1000;
	public static final long SECONDS = 60;
	public static final long MINUTES = 60;

	public static enum OBJECT_TYPES {
		TABLE, MATERIALIZED_VIEW, VIEW, ADHOCSQL
	};

	public static enum SHAPE_POLYGON_ORIENTATION {
		ORACLE, INVERSE, CLOCKWISE, ANTICLOCKWISE
	}

	public static javax.swing.DefaultComboBoxModel getShpPolygonOrientationCombo() {
		return new javax.swing.DefaultComboBoxModel(
				new String[] { Constants.SHAPE_POLYGON_ORIENTATION.ORACLE.toString(),
						Constants.SHAPE_POLYGON_ORIENTATION.INVERSE.toString(),
						Constants.SHAPE_POLYGON_ORIENTATION.CLOCKWISE.toString(),
						Constants.SHAPE_POLYGON_ORIENTATION.ANTICLOCKWISE.toString() });
	};

	public static enum SHAPE_TYPE {

		// Note that all constants have same ID as related ShapeTypes to aid in
		// conversion
		// Can't subclass ShapeType as is a final class
		//
		ARC(ShapeType.ARC.id), ARCM(ShapeType.ARCM.id), ARCZ(ShapeType.ARCZ.id), MULTIPOINT(ShapeType.MULTIPOINT.id),
		MULTIPOINTM(ShapeType.MULTIPOINTM.id), MULTIPOINTZ(ShapeType.MULTIPOINTZ.id), POINT(ShapeType.POINT.id),
		POINTM(ShapeType.POINTM.id), POINTZ(ShapeType.POINTZ.id), POLYGON(ShapeType.POLYGON.id),
		POLYGONM(ShapeType.POLYGONM.id), POLYGONZ(ShapeType.POLYGONZ.id), UNDEFINED(ShapeType.UNDEFINED.id);

		public final int id;

		SHAPE_TYPE(int id) {
			this.id = id;
		}

		public static SHAPE_TYPE getShapeType(int _shapeTypeId) {
			for (SHAPE_TYPE sType : SHAPE_TYPE.values()) {
				if (sType.id == _shapeTypeId) {
					return sType;
				}
			}
			return null;
		}

		public static javax.swing.DefaultComboBoxModel getArcShapeTypeCombo() {
			return new javax.swing.DefaultComboBoxModel(new String[] { ARC.toString(), ARCM.toString(), ARCZ.toString(),
					MULTIPOINT.toString(), MULTIPOINTM.toString(), MULTIPOINTZ.toString(), POINT.toString(),
					POINTM.toString(), POINTZ.toString(), POLYGON.toString(), POLYGONM.toString(), POLYGONZ.toString(),
					UNDEFINED.toString() });
		}
	};

	public static enum GEOMETRY_LABEL_POSITION {
		FIRST_VERTEX, MIDDLE_VERTEX, END_VERTEX, JTS_CENTROID, SDO_POINT, LABEL_ALONG
	};

	public static javax.swing.DefaultComboBoxModel getGeometryLabelPositionCombo() {
		return new javax.swing.DefaultComboBoxModel(
				new String[] { Constants.GEOMETRY_LABEL_POSITION.FIRST_VERTEX.toString(),
						Constants.GEOMETRY_LABEL_POSITION.MIDDLE_VERTEX.toString(),
						Constants.GEOMETRY_LABEL_POSITION.END_VERTEX.toString(),
						Constants.GEOMETRY_LABEL_POSITION.JTS_CENTROID.toString(),
						Constants.GEOMETRY_LABEL_POSITION.SDO_POINT.toString(),
						Constants.GEOMETRY_LABEL_POSITION.LABEL_ALONG.toString() });
	}

	public static enum SRID_TYPE {
		UNKNOWN, COMPOUND, ENGINEERING, GEODETIC_COMPOUND, GEODETIC_GEOCENTRIC, GEODETIC_GEOGRAPHIC2D,
		GEODETIC_GEOGRAPHIC3D, GEOGRAPHIC2D, PROJECTED, VERTICAL
	};

	public static javax.swing.DefaultComboBoxModel getSRIDTypeCombo() {
		return new javax.swing.DefaultComboBoxModel(new String[] { Constants.SRID_TYPE.UNKNOWN.toString(),
				Constants.SRID_TYPE.COMPOUND.toString(), Constants.SRID_TYPE.ENGINEERING.toString(),
				Constants.SRID_TYPE.GEODETIC_COMPOUND.toString(), Constants.SRID_TYPE.GEODETIC_GEOCENTRIC.toString(),
				Constants.SRID_TYPE.GEODETIC_GEOGRAPHIC2D.toString(),
				Constants.SRID_TYPE.GEODETIC_GEOGRAPHIC3D.toString(), Constants.SRID_TYPE.GEOGRAPHIC2D.toString(),
				Constants.SRID_TYPE.PROJECTED.toString(), Constants.SRID_TYPE.VERTICAL.toString() });
	}

	public static enum SEGMENT_ARROWS_TYPE {
		NONE, START, MIDDLE, END, END_ONLY
	};

	public static javax.swing.DefaultComboBoxModel getSegmentArrowsType() {
		return new javax.swing.DefaultComboBoxModel(new String[] { "None" /* Constants.SEGMENT_ARROWS_TYPE.NONE */,
				"Start" /* Constants.SEGMENT_ARROWS_TYPE.START */, "Middle" /* Constants.SEGMENT_ARROWS_TYPE.MIDDLE */,
				"End" /* Constants.SEGMENT_ARROWS_TYPE.END */, "End Only" /* Constants.SEGMENT_ARROWS_TYPE.END_ONLY */
		});
	}

	public static enum VERTEX_LABEL_TYPE {
		NONE, ID, COORD, ID_COORD, ID_CR_COORD, CUMULATIVE_LENGTH, X, Y, ELEVATION, MEASURE
	};

	public static javax.swing.DefaultComboBoxModel getVertexLabelCombo() {
		return new javax.swing.DefaultComboBoxModel(new String[] { "None" /* Constants.VERTEX_LABEL_TYPE.NONE */,
				"<Id>" /* Constants.VERTEX_LABEL_TYPE.ID */, "(X,Y[,Z[,M]])" /* Constants.VERTEX_LABEL_TYPE.COORD */,
				"<Id> (X,Y[,Z[,M]])" /* Constants.VERTEX_LABEL_TYPE.ID_COORD */,
				"<html>&lt;Id&gt;<BR/>(X,Y[,Z[,M]])</html>" /* Constants.VERTEX_LABEL_TYPE.ID_CR_COORD */,
				"Cumulative Length" /* Constants.VERTEX_LABEL_TYPE.CUMULATIVE_LENGTH */,
				"(X)" /* Constants.VERTEX_LABEL_TYPE.X */, "(Y)" /* Constants.VERTEX_LABEL_TYPE.Y */,
				"(Z)" /* Constants.VERTEX_LABEL_TYPE.ELEVATION */, "(M)" /* Constants.VERTEX_LABEL_TYPE.MEASURE */
		});
	}

	public static Constants.VERTEX_LABEL_TYPE getVertexLabelType(String _comboText) {
		if (_comboText.equalsIgnoreCase("None"))
			return Constants.VERTEX_LABEL_TYPE.NONE;
		if (_comboText.equalsIgnoreCase("<Id>"))
			return Constants.VERTEX_LABEL_TYPE.ID;
		if (_comboText.equalsIgnoreCase("(X,Y[,Z[,M]])"))
			return Constants.VERTEX_LABEL_TYPE.COORD;
		if (_comboText.equalsIgnoreCase("<Id> (X,Y[,Z[,M]])"))
			return Constants.VERTEX_LABEL_TYPE.ID_COORD;
		if (_comboText.equalsIgnoreCase("<html>&lt;Id&gt;<BR/>(X,Y[,Z[,M]])</html>"))
			return Constants.VERTEX_LABEL_TYPE.ID_CR_COORD;
		if (_comboText.equalsIgnoreCase("Cumulative Length"))
			return Constants.VERTEX_LABEL_TYPE.CUMULATIVE_LENGTH;
		if (_comboText.equalsIgnoreCase("(X)"))
			return Constants.VERTEX_LABEL_TYPE.X;
		if (_comboText.equalsIgnoreCase("(Y)"))
			return Constants.VERTEX_LABEL_TYPE.Y;
		if (_comboText.equalsIgnoreCase("(Z)"))
			return Constants.VERTEX_LABEL_TYPE.ELEVATION;
		if (_comboText.equalsIgnoreCase("(M)"))
			return Constants.VERTEX_LABEL_TYPE.MEASURE;
		return Constants.VERTEX_LABEL_TYPE.NONE;
	}

	public static String getVertexLabelType(Constants.VERTEX_LABEL_TYPE _comboText) {
		if (_comboText == Constants.VERTEX_LABEL_TYPE.NONE)
			return "None";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.ID)
			return "<Id>";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.COORD)
			return "(X,Y[,Z[,M]])";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.ID_COORD)
			return "<Id> (X,Y[,Z[,M]])";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.ID_CR_COORD)
			return "<html>&lt) return Id&gt) return <BR/>(X,Y[,Z[,M]])</html>";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.CUMULATIVE_LENGTH)
			return "Cumulative Length";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.X)
			return "(X)";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.Y)
			return "(Y)";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.ELEVATION)
			return "(Z)";
		if (_comboText == Constants.VERTEX_LABEL_TYPE.MEASURE)
			return "(M)";
		return "None";
	}

	public static enum SEGMENT_LABEL_TYPE {
		NONE, SEGMENT_LENGTH, CUMULATIVE_LENGTH, BEARING, BEARING_AND_DISTANCE, SEGMENT_ID
	};

	public static javax.swing.DefaultComboBoxModel getSegmentLabelCombo() {
		return new javax.swing.DefaultComboBoxModel(new String[] { Constants.SEGMENT_LABEL_TYPE.NONE.toString(),
				Constants.SEGMENT_LABEL_TYPE.SEGMENT_LENGTH.toString(),
				Constants.SEGMENT_LABEL_TYPE.CUMULATIVE_LENGTH.toString(),
				Constants.SEGMENT_LABEL_TYPE.BEARING.toString(),
				Constants.SEGMENT_LABEL_TYPE.BEARING_AND_DISTANCE.toString(),
				Constants.SEGMENT_LABEL_TYPE.SEGMENT_ID.toString() });
	}

	public static enum geometrySourceType {
		SDO_GEOMETRY, JGEOMETRY
	};

	public static enum EXPORT_TYPE {
		GML, KML, SHP, TAB
	};

	public static javax.swing.DefaultComboBoxModel getExportTypeAsCombo() {
		return new javax.swing.DefaultComboBoxModel(
				new String[] { Constants.EXPORT_TYPE.GML.toString(), Constants.EXPORT_TYPE.KML.toString(),
						Constants.EXPORT_TYPE.SHP.toString(), Constants.EXPORT_TYPE.TAB.toString() });
	}

	public static enum XMLAttributeFlavour {
		OGR, FME, KML, SHP
	};

	public static javax.swing.DefaultComboBoxModel getXMLAttributeFlavourAsCombo(boolean _kml) {
		if (_kml)
			return new javax.swing.DefaultComboBoxModel(new String[] { Constants.XMLAttributeFlavour.KML.toString() });
		else
			return new javax.swing.DefaultComboBoxModel(new String[] { Constants.XMLAttributeFlavour.OGR.toString(),
					Constants.XMLAttributeFlavour.FME.toString() });
	}

	public static enum renderType {
		SDO_GEOMETRY, WKT, GML2, GML3, KML, KML2, ICON, THUMBNAIL
	}

	public static javax.swing.DefaultComboBoxModel getRenderTypeCombo() {
		return new javax.swing.DefaultComboBoxModel(new String[] {
				Constants.renderType.SDO_GEOMETRY.toString().toUpperCase(),
				Constants.renderType.WKT.toString().toUpperCase(), Constants.renderType.GML2.toString().toUpperCase(),
				Constants.renderType.GML3.toString().toUpperCase(), Constants.renderType.KML.toString().toUpperCase(),
				Constants.renderType.KML2.toString().toUpperCase(), Constants.renderType.ICON.toString().toUpperCase(),
				Constants.renderType.THUMBNAIL.toString().toUpperCase() });
	}

	public static enum bracketType {
		NONE, ELIPSIS, SQUARE, ROUND, ANGLED
	};

	public static javax.swing.DefaultComboBoxModel getBracketTypeCombo() {
		return new javax.swing.DefaultComboBoxModel(new String[] { Constants.bracketType.NONE.toString().toUpperCase(),
				Constants.bracketType.ELIPSIS.toString().toUpperCase(),
				Constants.bracketType.SQUARE.toString().toUpperCase(),
				Constants.bracketType.ROUND.toString().toUpperCase(),
				Constants.bracketType.ANGLED.toString().toUpperCase() });
	}

	public static enum TEXT_OFFSET_POSITION {
		TL, TC, TR, CL, CC, CR, BL, BC, BR
	};

	public static enum ROTATION_VALUES {
		DEGREES, RADIANS
	};

	public static enum ROTATE {
		BOTH, LABEL, MARKER, NONE
	};

	public static enum MEASURE {
		AREA, LENGTH, BOTH
	};

	public static enum SDO_OPERATORS {
		ANYINTERACT, CONTAINS, COVEREDBY, COVERS, EQUAL, INSIDE, ON, OVERLAPBDYDISJOINT, OVERLAPBDYINTERSECT, OVERLAPS,
		RELATE, TOUCH
	};

	public static javax.swing.DefaultComboBoxModel getSdoOperators() {
		return new javax.swing.DefaultComboBoxModel(
				new String[] { "SDO_" + Constants.SDO_OPERATORS.ANYINTERACT.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.CONTAINS.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.COVEREDBY.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.COVERS.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.EQUAL.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.INSIDE.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.ON.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.OVERLAPBDYDISJOINT.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.OVERLAPBDYINTERSECT.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.OVERLAPS.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.RELATE.toString().toUpperCase(),
						"SDO_" + Constants.SDO_OPERATORS.TOUCH.toString().toUpperCase() });
	}

	public static DefaultListModel getRelateMasks() {
		DefaultListModel dlm = new DefaultListModel();
		String[] masks = new String[] { Constants.SDO_OPERATORS.ANYINTERACT.toString().toUpperCase(),
				Constants.SDO_OPERATORS.CONTAINS.toString().toUpperCase(),
				Constants.SDO_OPERATORS.COVEREDBY.toString().toUpperCase(),
				Constants.SDO_OPERATORS.COVERS.toString().toUpperCase(),
				Constants.SDO_OPERATORS.EQUAL.toString().toUpperCase(),
				Constants.SDO_OPERATORS.INSIDE.toString().toUpperCase(),
				Constants.SDO_OPERATORS.ON.toString().toUpperCase(),
				Constants.SDO_OPERATORS.OVERLAPBDYDISJOINT.toString().toUpperCase(),
				Constants.SDO_OPERATORS.OVERLAPBDYINTERSECT.toString().toUpperCase(),
				Constants.SDO_OPERATORS.OVERLAPS.toString().toUpperCase(),
				Constants.SDO_OPERATORS.RELATE.toString().toUpperCase(),
				Constants.SDO_OPERATORS.TOUCH.toString().toUpperCase() };
		dlm.copyInto(masks);
		return dlm;
	}

	public static enum GEOMETRY_TYPES {
		UNKNOWN, POINT, MULTIPOINT, LINE, MULTILINE, POLYGON, MULTIPOLYGON, COLLECTION, SOLID, MULTISOLID, IMAGE
	};

	public static javax.swing.DefaultComboBoxModel getGeometryTypesCombo() {
		return new javax.swing.DefaultComboBoxModel(
				new String[] { Constants.GEOMETRY_TYPES.UNKNOWN.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.COLLECTION.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.POINT.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.MULTIPOINT.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.LINE.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.MULTILINE.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.POLYGON.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.MULTIPOLYGON.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.SOLID.toString().toUpperCase(),
						Constants.GEOMETRY_TYPES.MULTISOLID.toString().toUpperCase() });
	}

	// public static enum layerPositionType { TOP("TOP") = -2, UP(-1), DOWN(1),
	// BOTTOM(2) };
	public static enum layerPositionType {
		TOP, UP, DOWN, BOTTOM
	};

	public static final String MDSYS_SCHEMA = "MDSYS"; // In case 9i being used
	public static final String TAG_SDO_GEOMETRY = "SDO_GEOMETRY";
	public static final String TAG_SDO_GTYPE = "SDO_GTYPE";
	public static final String TAG_SDO_SRID = "SDO_SRID";
	public static final String TAG_SDO_POINT_TYPE = "SDO_POINT_TYPE";
	public static final String TAG_SDO_ELEM_ARRAY = "SDO_ELEM_INFO_ARRAY";
	public static final String TAG_SDO_ORD_ARRAY = "SDO_ORDINATE_ARRAY";
	// ST_ Types
	public static final String TAG_ST_GEOMETRY = "ST_GEOMETRY";
	public static final String TAG_ST_CIRCULARSTRING = "ST_CIRCULARSTRING";
	public static final String TAG_ST_COMPOUNDCURVE = "ST_COMPOUNDCURVE";
	public static final String TAG_ST_CURVE = "ST_CURVE";
	public static final String TAG_ST_CURVEPOLYGON = "ST_CURVEPOLYGON";
	public static final String TAG_ST_GEOMCOLLECTION = "ST_GEOMCOLLECTION";
	public static final String TAG_ST_LINESTRING = "ST_LINESTRING";
	public static final String TAG_ST_MULTICURVE = "ST_MULTICURVE";
	public static final String TAG_ST_MULTILINESTRING = "ST_MULTILINESTRING";
	public static final String TAG_ST_MULTIPOINT = "ST_MULTIPOINT";
	public static final String TAG_ST_MULTIPOLYGON = "ST_MULTIPOLYGON";
	public static final String TAG_ST_MULTISURFACE = "ST_MULTISURFACE";
	public static final String TAG_ST_POINT = "ST_POINT";
	public static final String TAG_ST_POLYGON = "ST_POLYGON";
	public static final String TAG_ST_SURFACE = "ST_SURFACE";
	// others
	public static final String TAG_VERTEX_TYPE = "VERTEX_TYPE";
	public static final String TAG_SDO_DIM_ARRAY = "SDO_DIM_ARRAY";
	public static final String TAG_SDO_ELEMENT = "SDO_DIM_ELEMENT";

	public static final String TAG_MDSYS_SDO_GEOMETRY = MDSYS_SCHEMA + "." + TAG_SDO_GEOMETRY;
	public static final String TAG_MDSYS_SDO_POINT_TYPE = MDSYS_SCHEMA + "." + TAG_SDO_POINT_TYPE;
	public static final String TAG_MDSYS_SDO_ELEM_ARRAY = MDSYS_SCHEMA + "." + TAG_SDO_ELEM_ARRAY;
	public static final String TAG_MDSYS_SDO_ORD_ARRAY = MDSYS_SCHEMA + "." + TAG_SDO_ORD_ARRAY;

	// ST_GEOMETRY types
	public static final String TAG_MDSYS_ST_CIRCULARSTRING = MDSYS_SCHEMA + "." + TAG_ST_CIRCULARSTRING;
	public static final String TAG_MDSYS_ST_COMPOUNDCURVE = MDSYS_SCHEMA + "." + TAG_ST_COMPOUNDCURVE;
	public static final String TAG_MDSYS_ST_CURVE = MDSYS_SCHEMA + "." + TAG_ST_CURVE;
	public static final String TAG_MDSYS_ST_CURVEPOLYGON = MDSYS_SCHEMA + "." + TAG_ST_CURVEPOLYGON;
	public static final String TAG_MDSYS_ST_GEOMCOLLECTION = MDSYS_SCHEMA + "." + TAG_ST_GEOMCOLLECTION;
	public static final String TAG_MDSYS_ST_GEOMETRY = MDSYS_SCHEMA + "." + TAG_ST_GEOMETRY;
	public static final String TAG_MDSYS_ST_LINESTRING = MDSYS_SCHEMA + "." + TAG_ST_LINESTRING;
	public static final String TAG_MDSYS_ST_MULTICURVE = MDSYS_SCHEMA + "." + TAG_ST_MULTICURVE;
	public static final String TAG_MDSYS_ST_MULTILINESTRING = MDSYS_SCHEMA + "." + TAG_ST_MULTILINESTRING;
	public static final String TAG_MDSYS_ST_MULTIPOINT = MDSYS_SCHEMA + "." + TAG_ST_MULTIPOINT;
	public static final String TAG_MDSYS_ST_MULTIPOLYGON = MDSYS_SCHEMA + "." + TAG_ST_MULTIPOLYGON;
	public static final String TAG_MDSYS_ST_MULTISURFACE = MDSYS_SCHEMA + "." + TAG_ST_MULTISURFACE;
	public static final String TAG_MDSYS_ST_POINT = MDSYS_SCHEMA + "." + TAG_ST_POINT;
	public static final String TAG_MDSYS_ST_POLYGON = MDSYS_SCHEMA + "." + TAG_ST_POLYGON;
	public static final String TAG_MDSYS_ST_SURFACE = MDSYS_SCHEMA + "." + TAG_ST_SURFACE;

	// Others
	public static final String TAG_MDSYS_VERTEX_TYPE = MDSYS_SCHEMA + "." + TAG_VERTEX_TYPE;
	public static final String TAG_MDSYS_SDO_DIMARRAY = MDSYS_SCHEMA + "." + TAG_SDO_DIM_ARRAY;
	public static final String TAG_MDSYS_SDO_ELEMENT = MDSYS_SCHEMA + "." + TAG_SDO_ELEMENT;

	/**
	 * Preference Keys: One required for each value being saved.
	 **/
	public static final String GEORAPTOR_SETTINGS_FILE = "\\GeoRaptor\\Settings.xml";

	public static final String CONST_LAYER_MBR_INDEX = "INDEX";
	public static final String CONST_LAYER_MBR_METADATA = "METADATA";

	public static final String KEY_SDO_GEOMETRY_COLOUR = "UseSDOGeometryPanel";
	public static final String KEY_SDO_GEOMETRY_VISUAL_FORMAT = "SDOGeometryVisualFormat";
	public static final String KEY_SDO_GEOMETRY_PROCESS_FORMAT = "SDOGeometryProcessingFormat";
	public static final String KEY_DIMINFO_COLOUR = "UseDimInfoPanel";
	public static final String KEY_FETCH_SIZE = "fetchSize";
	public static final String KEY_IMAGE_REFRESH_MS = "imageRefreshMS";
	public static final String KEY_NEW_LAYER_POSITION = "newLayerPosition";
	public static final String KEY_TOC_POSITION = "TOCPosition";
	public static final String KEY_MAIN_SPLIT_POS = "mainSplitPos";
	public static final String KEY_MAIN_LAYER_SPLIT_POS = "mainLayerSplitPos";
	public static final String KEY_DEFAULT_SRID = "defaultSRID";
	public static final String KEY_SHAPE_POLYGON_ORIENTATION = "shapePolygonOrientation";
	public static final String KEY_MAP_BACKGROUND = "mapBackground";
	public static final String KEY_FEATURE_COLOUR = "featureColour";
	public static final String KEY_SELECTION_COLOUR = "selectionColour";
	public static final String KEY_METADATA_HIGHLIGHT_COLOUR = "highlightMetadataColour";
	public static final String KEY_METADATA_ORPHAN_COLOUR = "orphanMetadataColour";
	public static final String KEY_METADATA_MISSING_COLOUR = "missingMetadataColour";
	public static final String KEY_METADATA_CORRECT_COLOUR = "correctMetadataColour";

	public static final String KEY_SEARCH_PIXEL = "searchPixels";
	public static final String KEY_MIN_RESOLUTION = "minResolution";
	public static final String KEY_MBR_SAVE_SIZE = "mbrSaveSize";
	public static final String KEY_LAYER_MBR_SOURCE = "layerMBRSource";
	public static final String KEY_SELECTION_SINGLE = "selectionSingle";
	public static final String KEY_SDO_GEOMETRY_BRACKETING = "sdoGeometryBracket";
	public static final String KEY_SDO_GEOMETRY_COORDINATE_NUMBERING = "coordinateNumbering";
	public static final String KEY_SCHEMA_PREFIX = "schemaPrefix";
	public static final String KEY_PAN_ZOOM_PERCENTAGE = "panZoomPercentage";
	public static final String KEY_LAYER_TREE_GEOMETRY_GROUPING = "layerTreeGrouping";
	public static final String KEY_EXPORT_DIRECTORY = "exportDirectory";
	public static final String KEY_EXPORT_TYPE = "exportType";
	public static final String KEY_TAB_COORDSYS = "exportTabCoordsys";
	public static final String KEY_SHAPEFILE_PRJ = "exportShpPRJ";
	public static final String KEY_ASSOCIATION_XML = "associationXML";
	public static final String KEY_TABLE_COUNT_LIMIT = "tableCountLimit";
	public static final String KEY_LOG_SEARCH_STATISTICS = "logSearchStats";

	public static final String KEY_ROOT_SPATIAL_INDEX = "SpatialIndex";
	public static final String KEY_INDEX_SUFFIX_FLAG = "suffix";
	public static final String KEY_INDEX_AFFIX = "affix";

	/**
	 * Name of XML elements where spatial layers in view are saved in the spatialXML
	 * tag
	 */
	public static final String KEY_SPATIAL_VIEWS = "SpatialViews";
	public static final String KEY_SPATIAL_VIEW = "SpatialView";
	public static final String KEY_LAYERS = "Layers";
	public static final String KEY_LAYER = "Layer";
	public static final String KEY_CLASS_ATTRIBUTE = "class";

	public static final String KEY_SVLAYER = "SVLayer";
	public static final String KEY_SVSpatialLayer = "SVSpatialLayer";
	public static final String KEY_SVDBObjectLayer = "SVDBObjectLayer";
	public static final String KEY_SVQueryLayer = "SVQueryLayer";
	public static final String KEY_SVGraphicLayer = "SVGraphicLayer";
	public static final String KEY_SVWorksheetLayer = "SVWorksheetLayer";

	public static final String KEY_SDO_NN = "sdo_nn";
	public static final String KEY_NULL_STRING_VALUE = "null_string";
	public static final String KEY_NULL_NUMBER_VALUE = "number_string";
	public static final String KEY_NULL_INTEGER_VALUE = "integer_string";
	public static final String KEY_NULL_DATE_VALUE = "date_string";
	public static final String KEY_DBASE_NULL_FIELD = "dbase_null_field";
	public static final String KEY_DBASE_COLUMN_SHORTEN_BEGIN = "dbase_shorten_name_begin";
	public static final String KEY_MDSYS_PREFIX = "mdsys_prefix";
	public static final String KEY_GROUPING_SEPARATOR = "grouping_separator";
	// *************
	public static final String KEY_PREVIEW_IMAGE_WIDTH = "preview_image_width";
	public static final String KEY_PREVIEW_IMAGE_HEIGHT = "preview_image_height";
	public static final String KEY_MIN_MBR_ASPECT_RATIO = "min_mbr_ratio";
	public static final String KEY_PREVIEW_IMAGE_HORIZONTAL_LABELS = "preview_image_label_horiz";
	public static final String KEY_PREVIEW_IMAGE_VERTEX_LABELING = "preview_image_labeling";
	public static final String KEY_PREVIEW_IMAGE_VERTEX_MARK = "preview_vertex_mark";
	public static final String KEY_PREVIEW_IMAGE_VERTEX_MARK_SIZE = "preview_vertex_mark_size";

	// *************
	public static final String KEY_FAST_PICKLER = "fast_pickler";
	public static final String KEY_MAP_SCALE_VISIBLE = "status_map_scale";
	public static final String KEY_NUMBER_FEATURES_VISIBLE = "numFeaturesVisible";
	public static final String KEY_DRAW_QUERY_GEOMETRY = "drawQueryGeometry";
	public static final String KEY_USER_SQL_SCHEMA_PREFIX = "sqlSchemaPrefix";
	public static final String KEY_QUERY_LIMIT = "queryLimit";
	public static final String KEY_GEOMETRY_COLUMN_NAME = "defGeomColName";
	public static final String KEY_LAYER_RENDER_RANDOM = "layerRenderRandom";

	/**
	 * Preference Default Values: One needed per KEY above
	 **/
	public static final String VAL_SDO_GEOMETRY_VISUAL_FORMAT = "SDO_GEOMETRY";
	public static final boolean VAL_SDO_GEOMETRY_PROCESS_FORMAT = true; // Internal STRUCT or JGeometry
	public static final int VAL_FETCH_SIZE = 100;
	public static final int VAL_IMAGE_REFRESH_MS = 1500;
	public static final boolean VAL_SDO_GEOMETRY_COLOUR = false; // Too memory intensive to be default.
	public static final boolean VAL_DIMINFO_COLOUR = true;
	public static final boolean VAL_INDEX_SUFFIX_FLAG = true;
	public static final String VAL_INDEX_AFFIX = "_SPIX";
	public static final int VAL_MAIN_LAYER_SPLIT_POS = 900;
	public static final int VAL_MAIN_SPLIT_POS = 300;
	public static final String VAL_NEW_LAYER_POSITION_TOP = "TOP";
	public static final String VAL_TOC_POSITION = "LEFT";
	public static final String VAL_DEFAULT_SRID = "NULL";
	public static final int VAL_MAP_BACKGROUND = Color.WHITE.getRGB();
	public static final int VAL_FEATURE_COLOUR = Color.RED.getRGB();
	public static final int VAL_SELECTION_COLOUR = Color.YELLOW.getRGB();
	public static final int VAL_METADATA_ORPHAN_COLOUR = (new java.awt.Color(255, 102, 102)).getRGB();
	public static final int VAL_METADATA_MISSING_COLOUR = (new java.awt.Color(153, 153, 225)).getRGB();
	public static final int VAL_METADATA_CORRECT_COLOUR = (new java.awt.Color(153, 255, 153)).getRGB();
	public static final int VAL_METADATA_HIGHLIGHT_COLOUR = (new java.awt.Color(255, 165, 0)).getRGB();

	public static final int VAL_SEARCH_PIXEL = 3;
	public static final boolean VAL_MIN_RESOLUTION = false;
	public static final int VAL_MBR_SAVE_SIZE = 20;
	public static final String VAL_LAYER_MBR_SOURCE = CONST_LAYER_MBR_INDEX;
	public static final String VAL_SDO_GEOMETRY_BRACKETING = "NONE";
	public static final boolean VAL_SDO_GEOMETRY_COORDINATE_NUMBERING = false;
	public static final boolean VAL_SCHEMA_PREFIX = true;
	public static final int VAL_PAN_ZOOM_PERCENTAGE = 25;
	public static final boolean VAL_LAYER_TREE_GEOMETRY_GROUPING = false;
	public static final String VAL_EXPORT_DIRECTORY = System.getProperty("java.io.tmpdir");
	public static final String VAL_TAB_COORDSYS = "CoordSys Earth Projection 1, 104";
	public static final String VAL_SHAPEFILE_PRJ = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]]";
	public static final String VAL_ASSOCIATION_XML = "<Validations>"
			+ "<Row><ErrorCode>13000</ErrorCode><Function>MDSYS.SDO_MIGRATE.TO_CURRENT(:G,:D)</Function></Row>"
			+ "<Row><ErrorCode>13349,13351</ErrorCode><Function>MDSYS.SDO_GEOM.SDO_UNION(:G,:G,:T)</Function></Row>"
			+ "<Row><ErrorCode>13356</ErrorCode><Function>MDSYS.SDO_UTIL.REMOVE_DUPLICATE_VERTICES(:G,:T)</Function></Row>"
			+ "<Row><ErrorCode>13366,13367</ErrorCode><Function>MDSYS.SDO_UTIL.RECTIFY_GEOMETRY(:G,:T)</Function></Row>"
			+ "<Row><ErrorCode>ELSE</ErrorCode><Function>NVL(MDSYS.SDO_UTIL.RECTIFY_GEOMETRY(:G,:T),:G)</Function></Row>"
			+ "</Validations>";
	public static final boolean VAL_MDSYS_PREFIX = true;
	public static final boolean VAL_USER_SQL_SCHEMA_PREFIX = true;
	public static final int VAL_TABLE_COUNT_LIMIT = 1000;
	public static final boolean VAL_NUMBER_FEATURES_VISIBLE = true;
	public static final boolean VAL_LOG_SEARCH_STATISTICS = false;
	public static final boolean VAL_SDO_NN = true;
	public static final boolean VAL_DBASE_NULL_FIELD = true;
	public static final boolean VAL_DBASE_COLUMN_SHORTEN_BEGIN = false;
	public static final boolean VAL_GROUPING_SEPARATOR = true;
	public static final int VAL_PREVIEW_IMAGE_WIDTH = 400;
	public static final int VAL_PREVIEW_IMAGE_HEIGHT = 300;
	public static final boolean VAL_PREVIEW_IMAGE_HORIZONTAL_LABELS = true;
	public static final boolean VAL_PREVIEW_IMAGE_VERTEX_LABELING = true;
	public static final String VAL_PREVIEW_IMAGE_VERTEX_MARK = "CIRCLE";
	public static final int VAL_PREVIEW_IMAGE_VERTEX_MARK_SIZE = 6;
	public static final String VAL_SHAPE_POLYGON_ORIENTATION = "CLOCKWISE";
	public static final double VAL_MIN_MBR_ASPECT_RATIO = 3.0;
	public static final int VAL_MBR_SAMPLE_LIMIT = 1000;
	public static final boolean VAL_FAST_PICKLER = false;
	public static final boolean VAL_MAP_SCALE_VISIBLE = false;
	public static final boolean VAL_DRAW_QUERY_GEOMETRY = true;
	public static final int VAL_QUERY_LIMIT = 0;
	public static final String VAL_GEOMETRY_COLUMN_NAME = "GEOM";
	public static final boolean VAL_LAYER_RENDER_RANDOM = true;
//	public static final String VAL_SPATIAL_LAYER_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><GeoRaptor><Version>"
//			+ Tools.getVersion() + "</Version>" + "<SpatialPanel><ActiveView>SRID:NULL</ActiveView>" + "<Views>"
//			+ "<View><ID>" + UUID.randomUUID().toString() + "</ID>" + "<Name>"
//			+ SpatialView.createViewName(MainSettings.getInstance().getPreferences().getSRID()) + "</Name>"
//			+ "<VisibleName>" + SpatialView.createViewName(MainSettings.getInstance().getPreferences().getSRID())
//			+ "</VisibleName>" + "<SRID>" + MainSettings.getInstance().getPreferences().getSRID()
//			+ "</SRID><SRIDType>PROJECTED</SRIDType>"
//			+ "<DistanceUnitType>M</DistanceUnitType><AreaUnitType>SQ_M</AreaUnitType><Precision>3</Precision>"
//			+ "<ActiveLayer></ActiveLayer><RenderingHint>SPEED</RenderingHint><MBR><MinX>0</MinX><MinY>0</MinY><MaxX>1</MaxX><MaxY>1</MaxY></MBR>"
//			+ "<MapBackground>-1</MapBackground><ScaleBar>true</ScaleBar><SRIDBaseUnitType>M</SRIDBaseUnitType>"
//			+ "<Layers></Layers>" + "</View></Views></SpatialPanel></GeoRaptor>";

}
