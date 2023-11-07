package org.GeoRaptor.io.Export;

import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class GeoJSONWriter {

    private static int INDENT = 2;

    private static int CURRENT = 0;
    
    private static DecimalFormat formatter;

    private static String  newLine = System.getProperty("line.separator");

    public GeoJSONWriter() {
        super();
    }

    /** =============== Public methods ================= **/
    
    public static void startDocument(StringBuffer _buffer) {
        _buffer.append("{" + newLine);
        CURRENT = 0;
    }

    public static void endDocument(StringBuffer _buffer) {
        CURRENT = 0;
        _buffer.append(newLine + "}");
    }
    
    public static void appendFeatureCollectionBeginText(Geometry     _bbox,
                                                        Object       _idValue,
                                                        StringBuffer _buffer)
    throws IOException 
    {
        CURRENT = 2;
        indent(CURRENT,_buffer,"{",                                true );
        CURRENT++;
        indent(CURRENT,_buffer,"\"type\": \"FeatureCollection\",", true );
        if ( _idValue != null ) {
            if ( _idValue instanceof String ) {
                indent(CURRENT,_buffer,"\"id\": \"" + ((String)_idValue) + "\",", true );
            } else {
                indent(CURRENT,_buffer,"\"id\": " + ((String)_idValue) + ",", true );
            }
        }
        if ( _bbox != null ) {
            writeBBox(_bbox,CURRENT,_buffer);
        }
        indent(CURRENT,_buffer,"\"features\": [",false);
        CURRENT++;
    }
    
    public static void appendFeatureCollectionEndText(StringBuffer _buffer) 
    throws IOException 
    {
        CURRENT--;
        indent(0,_buffer,"],",true);
    }

    public static void appendFeatureBeginText(Geometry     _bbox,
                                              Object       _idValue,
                                              StringBuffer _buffer)
    throws IOException 
    {
        CURRENT = 2;
        indent(CURRENT,_buffer,"{",                      true );
        CURRENT++;
        indent(CURRENT,_buffer,"\"type\": \"Feature\",", true );
        if ( _idValue != null ) {
            if ( _idValue instanceof String ) {
                indent(CURRENT,_buffer,"\"id\": \"" + _idValue + "\",", true );
            } else {
                indent(CURRENT,_buffer,"\"id\": " + _idValue + ",", true );
            }
        }
        // Create BBOX at feature level?
        if ( _bbox != null ) {
            writeBBox(_bbox,CURRENT,_buffer);
        }
        indent(CURRENT,_buffer,"\"geometry\": ",false);
    }

    public static void appendFeatureEndText(boolean      _attributes,
                                            StringBuffer _buffer) 
    throws IOException 
    {
        setCURRENT(CURRENT-1);
        indent(_buffer,"}" + (_attributes?",":newLine), false);
    }

    /**
     * Converts a <code>Geometry</code> to its GeoJSON representation.
     * 
     * @param geometry
     *        a <code>Geometry</code> to process
     */
    public static void write(Geometry     _geometry, 
                             String       _featureType,
                             boolean      _bbox,
                             StringBuffer _buffer) 
    throws IOException 
    {
        if (formatter == null) {
            formatter = createFormatter(_geometry.getPrecisionModel());
        }
        CURRENT++;
        appendGeometryTaggedText(_geometry, CURRENT, _featureType, _bbox, _buffer);
    }

    /** ========================================================================= **/
    /** =========================== Private methods ============================= **/
    /** ========================================================================= **/
    
    /**
     * Creates the <code>DecimalFormat</code> used to write
     * <code>double</code> s with a sufficient number of decimal places.
     * 
     * @param precisionModel
     *        the <code>PrecisionModel</code> used to determine the number
     *        of decimal places to write.
     * @return a <code>DecimalFormat</code> that write <code>double</code> s
     *         without scientific notation.
     */
    private static DecimalFormat createFormatter(PrecisionModel precisionModel) 
    {
        // the default number of decimal places is 16, which is sufficient
        // to accomodate the maximum precision of a double.
        int decimalPlaces = precisionModel.getMaximumSignificantDigits();
        // specify decimal separator explicitly to avoid problems in other
        // locales
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        return new DecimalFormat("#" + 
                                 (decimalPlaces > 0 ? "." : "") + Strings.stringOfChar('#', decimalPlaces), 
                                 symbols);
    }

    /**
     * @param level
     * @param _buffer
     * @throws IOException
     */
    private static void indent(int          _level, 
                               StringBuffer _buffer) 
    throws IOException 
    {
        _buffer.append(Strings.stringOfChar(' ', INDENT * _level));
    }

    public  static void indent(int          _level, 
                               StringBuffer _buffer,
                               String       _value,
                               boolean      _newLine) 
    throws IOException 
    {
        _buffer.append(Strings.stringOfChar(' ', INDENT * (_level-1)) + 
                      _value + 
                      (_newLine?newLine:""));
    }

    public  static void indent(StringBuffer _buffer,
                               String       _value,
                               boolean      _newLine) 
    throws IOException 
    {
        _buffer.append(Strings.stringOfChar(' ', INDENT * (CURRENT-1)) + 
                       _value + 
                       (_newLine?newLine:""));
    }

    /**
     * Converts a <code>double</code> to a <code>String</code>, not in
     * scientific notation.
     * 
     * @param d
     *        the <code>double</code> to convert
     * @return the <code>double</code> as a <code>String</code>, not in
     *         scientific notation
     */
    private static String writeNumber(double d) {
        return formatter!=null ? formatter.format(d) : String.valueOf(d);
    }
    
    public static String getBBOX(Geometry _geometry)
    throws IOException 
    {
        Geometry envelope = _geometry.getEnvelope();
        // Returns:
        // empty, returns an empty Point.
        // a point, returns a Point.
        // a line parallel to an axis, a two-vertex LineString
        // otherwise, returns a Polygon whose vertices are (minx miny, maxx miny, maxx maxy, minx maxy, minx miny).
        String envelopeString = ""; 
        if ( envelope instanceof Polygon) {
            Coordinate[] coords = ((Polygon)envelope).getCoordinates();
            envelopeString = "\"bbox\": [" + 
                         writeNumber(coords[0].x) +
                         ", " +
                         writeNumber(coords[0].y) 
                         +
                         (Double.isNaN(coords[0].z) ? "" : ", " + writeNumber(coords[0].z))
                         +
                         writeNumber(coords[2].x) +
                         ", " +
                         writeNumber(coords[2].y)
                         +
                         (Double.isNaN(coords[2].z) ? "" : ", " + writeNumber(coords[2].z)) + "]";
        }
        return envelopeString;
    }
    
    private static void writeBBox(Geometry     _geometry,
                                  int          _level,
                                  StringBuffer _buffer) 
    throws IOException 
    {
        String envelopeString = getBBOX(_geometry);
        if ( ! Strings.isEmpty(envelopeString) ) {
          indent(_level,
                 _buffer,
                 envelopeString +  ",",
                 true);
        }
    }

    /**
     * Converts a <code>Geometry</code> to &lt;Geometry Tagged Text&gt;
     * format, then appends it to the _buffer.
     * 
     * @param geometry
     *            the <code>Geometry</code> to process
     * @param _buffer
     *            the output _buffer to append to
     */
    private static void appendGeometryTaggedText(Geometry     _geometry, 
                                                 int          _level,
                                                 String       _featureOption,
                                                 boolean      _bbox,
                                                 StringBuffer _buffer) 
    throws IOException 
    {
        boolean isFeature = _featureOption.toUpperCase().startsWith("FEATURE");
        _buffer.append("{" + newLine);
        setCURRENT(_level);
        if (_geometry instanceof Point) {
            Point point = (Point) _geometry;
            indent(_buffer,"\"type\": \"Point\",", true );
            indent(_buffer,"\"coordinates\": ",    false);
            _buffer.append(getCoordinate(point.getCoordinate()) + newLine);
        } else if (_geometry instanceof LinearRing) {
            indent(_buffer,"\"type\": \"LineString\",", true );
            if ( isFeature==false && _bbox ) { writeBBox(_geometry, CURRENT, _buffer); }
            indent(_buffer,"\"coordinates\": [", true);
            appendLinearRingTaggedText((LinearRing) _geometry, CURRENT+1, _buffer);
            indent(_buffer,"]",true);
        } else if (_geometry instanceof LineString) {
            indent(_buffer,"\"type\": \"LineString\",", true );
            if ( isFeature==false && _bbox ) { writeBBox(_geometry,CURRENT,_buffer); }
            indent(_buffer,"\"coordinates\": [", true);
            appendLineStringTaggedText((LineString) _geometry, CURRENT+1, _buffer);
            indent(_buffer,"]",true);
        } else if (_geometry instanceof Polygon) {
            indent(_buffer,"\"type\": \"Polygon\",", true );
            if ( isFeature==false && _bbox ) { writeBBox(_geometry, CURRENT, _buffer); }
            indent(_buffer,"\"coordinates\": [", true);
            appendPolygonTaggedText((Polygon) _geometry, CURRENT+2, _buffer);
            indent(_buffer,"]",true);
        } else if (_geometry instanceof MultiPoint) {
            indent(_buffer,"\"type\": \"MultiPoint\",", true );
            if ( isFeature==false && _bbox ) { writeBBox(_geometry, CURRENT,_buffer); }
            indent(_buffer,"\"coordinates\": [", true);
            appendMultiPointTaggedText((MultiPoint) _geometry, CURRENT+1, _buffer);
            indent(_buffer,"]",true);
        } else if (_geometry instanceof MultiLineString) {
            indent(_buffer,"\"type\": \"MultiLineString\",", true );
            if ( isFeature==false && _bbox ) { writeBBox(_geometry, CURRENT,_buffer); }
            indent(_buffer,"\"coordinates\": [", true);
            appendMultiLineStringTaggedText((MultiLineString) _geometry, CURRENT+1, _buffer);
            indent(_buffer,"]",true);
        } else if (_geometry instanceof MultiPolygon) {
            indent(_buffer,"\"type\": \"MultiPolygon\",", true );
            if ( isFeature==false && _bbox ) { writeBBox(_geometry ,CURRENT, _buffer); }
            indent(_buffer,"\"coordinates\": [", true);
            appendMultiPolygonTaggedText((MultiPolygon) _geometry, CURRENT+1, _buffer);
            indent(_buffer,"]",true);
        } else if (_geometry instanceof GeometryCollection) {
            indent(_buffer,"\"type\": \"GeometryCollection\",", true );
            if ( isFeature==false && _bbox ) { writeBBox(_geometry, CURRENT, _buffer); }
            indent(CURRENT+1, _buffer,"\"geometries\": [", true);
            appendGeometryCollectionTaggedText((GeometryCollection) _geometry, CURRENT+1, _featureOption, _bbox, _buffer);
            indent(_buffer,"]",true);
        } else {
            Assert.shouldNeverReachHere("Unsupported Geometry implementation:" + _geometry.getClass());
        }
        if ( _featureOption.toUpperCase().startsWith("FEATURE") ) {
          setCURRENT(CURRENT-1);
          indent(_buffer,"}", false);
        }
    }

    /**
     * Converts a <code>Coordinate</code> to &lt;Point Tagged Text&gt; format,
     * then appends it to the _buffer.
     * 
     * @param coordinate
     *        the <code>Coordinate</code> to process
     * @param _buffer
     *        the output _buffer to append to
     * @param precisionModel
     *        the <code>PrecisionModel</code> to use to convert from a
     *        precise coordinate to an external coordinate
     */
    @SuppressWarnings("unused")
	private static void appendPointTaggedText(Coordinate   _coordinate, 
                                              int          _level,
                                              StringBuffer _buffer) 
    throws IOException {
        appendPointText(_coordinate, _level, _buffer);
    }

    /**
     * Converts a <code>LineString</code> to &lt;LineString Tagged Text&gt;
     * format, then appends it to the _buffer.
     * 
     * @param lineString
     *        the <code>LineString</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendLineStringTaggedText(LineString   _lineString, 
                                                   int          _level,
                                                   StringBuffer _buffer) 
    throws IOException {
        appendLineStringText(_lineString, _level, true, _buffer);
    }

    /**
     * Converts a <code>LinearRing</code> to &lt;LinearRing Tagged Text&gt;
     * format, then appends it to the _buffer.
     * 
     * @param linearRing
     *        the <code>LinearRing</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendLinearRingTaggedText(LinearRing   _linearRing, 
                                                   int          _level,
                                                   StringBuffer _buffer) 
    throws IOException {
        appendLineStringText(_linearRing, _level, true, _buffer);
    }

    /**
     * Converts a <code>Polygon</code> to &lt;Polygon Tagged Text&gt; format,
     * then appends it to the _buffer.
     * 
     * @param polygon
     *        the <code>Polygon</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendPolygonTaggedText(Polygon      _polygon, 
                                                int          _level,
                                                StringBuffer _buffer) 
    throws IOException {
        appendPolygonText(_polygon, _level, false, _buffer);
    }

    /**
     * Converts a <code>MultiPoint</code> to &lt;MultiPoint Tagged Text&gt;
     * format, then appends it to the _buffer.
     * 
     * @param multipoint
     *        the <code>MultiPoint</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendMultiPointTaggedText(MultiPoint   _multiPoint, 
                                                   int          _level,
                                                   StringBuffer _buffer) 
    throws IOException {
        appendMultiPointText(_multiPoint, _level, _buffer);
    }

    /**
     * Converts a <code>MultiLineString</code> to &lt;MultiLineString Tagged
     * Text&gt; format, then appends it to the _buffer.
     * 
     * @param multiLineString
     *        the <code>MultiLineString</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendMultiLineStringTaggedText(MultiLineString _multiLineString, 
                                                        int             _level, 
                                                        StringBuffer    _buffer)
    throws IOException {
        appendMultiLineStringText(_multiLineString, _level, false, _buffer);
    }

    /**
     * Converts a <code>MultiPolygon</code> to &lt;MultiPolygon Tagged
     * Text&gt; format, then appends it to the _buffer.
     * 
     * @param multiPolygon
     *        the <code>MultiPolygon</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendMultiPolygonTaggedText(MultiPolygon _multiPolygon,
                                                     int          _level,
                                                     StringBuffer _buffer) 
    throws IOException {
        appendMultiPolygonText(_multiPolygon, _level, _buffer);
    }

    /**
     * Converts a <code>GeometryCollection</code> to &lt;GeometryCollection
     * Tagged Text&gt; format, then appends it to the _buffer.
     * 
     * @param geometryCollection
     *        the <code>GeometryCollection</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendGeometryCollectionTaggedText(GeometryCollection _geometryCollection, 
                                                           int                _level, 
                                                           String             _featureOption,
                                                           boolean            _bbox,
                                                           StringBuffer       _buffer)
    throws IOException {
        appendGeometryCollectionText(_geometryCollection, _level, _featureOption, _bbox, _buffer);
    }

    private static String getCoordinate(Coordinate _coordinate) 
    {
        return "[" 
               + writeNumber(_coordinate.x)
               + ", "
               + writeNumber(_coordinate.y)
               + (Double.isNaN(_coordinate.z) 
               ? "" 
               : ", " + writeNumber(_coordinate.z))
               + "]";
    }

    /**
     * Converts a <code>Coordinate</code> to &lt;Point Text&gt; format, then
     * appends it to the _buffer.
     * The value of the geometry member SHALL be either a Geometry object or, 
     * in the case that the Feature is unlocated, a JSON null value.
     * @param coordinate
     *        the <code>Coordinate</code> to process
     * @param _buffer
     *        the output _buffer to append to
     * @param precisionModel
     *        the <code>PrecisionModel</code> to use to convert from a
     *        precise coordinate to an external coordinate
     */
    private static void appendPointText(Coordinate   _coordinate, 
                                        int          _level,
                                        StringBuffer _buffer) 
    throws IOException {
        indent(_level, _buffer);
        if (_coordinate == null) {
            _buffer.append("null");
        } else {
            appendCoordinate(_coordinate, _buffer);
            _buffer.append(newLine);
        }
    }

    /**
     * Converts a <code>Coordinate</code> to &lt;Point&gt; format, then
     * appends it to the _buffer.
     * 
     * @param coordinate
     *        the <code>Coordinate</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendCoordinate(Coordinate   _coordinate, 
                                         StringBuffer _buffer) 
    throws IOException {
        indent(CURRENT,_buffer,getCoordinate(_coordinate),false);
    }

    /**
     * Converts a <code>LineString</code> to &lt;LineString Text&gt; format,
     * then appends it to the _buffer.
     * The value of the geometry member SHALL be either a Geometry object or, 
     * in the case that the Feature is unlocated, a JSON null value.
     * 
     * @param lineString
     *        the <code>LineString</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendLineStringText(LineString   _lineString, 
                                             int          _level,
                                             boolean      _doIndent, 
                                             StringBuffer _buffer) 
    throws IOException 
    {
        if (_doIndent) {
            indent(_level, _buffer);
        }
        if (_lineString.isEmpty()) {
            _buffer.append("null");
        } else {
            for (int i = 0; i < _lineString.getNumPoints(); i++) {
                if (i > 0) {
                    _buffer.append(", ");
                    if (i % 10 == 0)
                    {
                        _buffer.append(newLine);
                        // Only indent after newLine and not last point
                        if ( i+1 != _lineString.getNumPoints() ) {
                            indent(_level, _buffer);
                        }
                    }
                }
                _buffer.append(getCoordinate(_lineString.getCoordinateN(i)));
            }
        }
        _buffer.append(newLine);
    }

    /**
     * Converts a <code>Polygon</code> to &lt;Polygon Text&gt; format, then
     * appends it to the _buffer.
     * The value of the geometry member SHALL be either a Geometry object or, 
     * in the case that the Feature is unlocated, a JSON null value.
     * @param polygon
     *        the <code>Polygon</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendPolygonText(Polygon      _polygon, 
                                          int          _level,
                                          boolean      _indentFirst, 
                                          StringBuffer _buffer) 
    throws IOException {
        if (_polygon.isEmpty()) {
            _buffer.append("null");
        } else {
            if (_indentFirst) {
                indent(_level, _buffer);
            }
            appendLineStringText(_polygon.getExteriorRing(), _level, false,_buffer);
            for (int i = 0; i < _polygon.getNumInteriorRing(); i++) {
                indent(_level,_buffer,", ",true);
                appendLineStringText(_polygon.getInteriorRingN(i), _level + 1, true, _buffer);
            }
        }
    }

    /**
     * Converts a <code>MultiPoint</code> to &lt;MultiPoint Text&gt; format,
     * then appends it to the _buffer.
     * The value of the geometry member SHALL be either a Geometry object or, 
     * in the case that the Feature is unlocated, a JSON null value.
     * 
     * @param multiPoint
     *        the <code>MultiPoint</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static  void appendMultiPointText(MultiPoint   _multiPoint, 
                                              int          _level,
                                              StringBuffer _buffer) 
    throws IOException 
    {
        indent(_level,_buffer,"",false);
        if (_multiPoint.isEmpty()) {
            _buffer.append("null");
        } else {
            for (int i = 0; i < _multiPoint.getNumGeometries(); i++) {
                if (i > 0) {
                    _buffer.append(", " + newLine);
                    if ( i+1 != _multiPoint.getNumGeometries() ) {
                        indent(_level,_buffer,"",false);
                    }
                }
                _buffer.append(getCoordinate(_multiPoint.getGeometryN(i).getCoordinate()));
            }
        }
    }

    /**
     * Converts a <code>MultiLineString</code> to &lt;MultiLineString Text&gt;
     * format, then appends it to the _buffer.
     * The value of the geometry member SHALL be either a Geometry object or, 
     * in the case that the Feature is unlocated, a JSON null value.
     * 
     * @param multiLineString
     *        the <code>MultiLineString</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendMultiLineStringText(MultiLineString _multiLineString,
                                                  int             _level, 
                                                  boolean         _indentFirst, 
                                                  StringBuffer    _buffer) 
    throws IOException {
        if (_multiLineString.isEmpty()) {
            _buffer.append("null");
        } else {
            int level2 = _level;
            boolean doIndent = _indentFirst;
            for (int i = 0; i < _multiLineString.getNumGeometries(); i++) {
                if (i > 0) {
                        _buffer.append(", " + newLine);
                        level2 = _level + 1;
                        doIndent = true;
                }
                appendLineStringText((LineString) _multiLineString.getGeometryN(i), 
                                     level2, 
                                     doIndent, 
                                     _buffer);
            }
        }
    }

    /**
     * Converts a <code>MultiPolygon</code> to &lt;MultiPolygon Text&gt;
     * format, then appends it to the _buffer.
     * The value of the geometry member SHALL be either a Geometry object or, 
     * in the case that the Feature is unlocated, a JSON null value.
     * 
     * @param multiPolygon
     *        the <code>MultiPolygon</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static void appendMultiPolygonText(MultiPolygon _multiPolygon, 
                                               int          _level,
                                               StringBuffer _buffer) 
    throws IOException {
        if (_multiPolygon.isEmpty()) {
            _buffer.append("null");
        } else {
            int level2 = _level;
            boolean doIndent = false;
            _buffer.append("(");
            for (int i = 0; i < _multiPolygon.getNumGeometries(); i++) {
                if (i > 0) {
                    _buffer.append(", " + newLine);
                    level2 = _level + 1;
                    doIndent = true;
                }
                appendPolygonText((Polygon) _multiPolygon.getGeometryN(i),
                                  level2, 
                                  doIndent, 
                                  _buffer);
            }
        }
    }

    /**
     * Converts a <code>GeometryCollection</code> to
     * &lt;GeometryCollectionText&gt; format, then appends it to the _buffer.
     * The value of the geometry member SHALL be either a Geometry object or, 
     * in the case that the Feature is unlocated, a JSON null value.
     * 
     * @param geometryCollection
     *        the <code>GeometryCollection</code> to process
     * @param _buffer
     *        the output _buffer to append to
     */
    private static  void appendGeometryCollectionText(GeometryCollection _geometryCollection, 
                                                      int                _level,
                                                      String             _featureOption,
                                                      boolean            _bbox,
                                                      StringBuffer       _buffer)
    throws IOException {
        if (_geometryCollection.isEmpty()) {
            _buffer.append("null");
        } else {
            int level2 = _level;
            for (int i = 0; i < _geometryCollection.getNumGeometries(); i++) {
                if (i > 0) {
                    _buffer.append(", " + newLine);
                    level2 = _level + 1;
                }
                _buffer.append("{" + newLine);
                appendGeometryTaggedText(_geometryCollection.getGeometryN(i),level2,_featureOption,_bbox,_buffer);
                _buffer.append("}");
            }
        }
    }

    public static void setFormatter(PrecisionModel _precisionModel) {
        GeoJSONWriter.formatter = Tools.getDecimalFormatter(_precisionModel);
    }

    public static DecimalFormat getFormatter() {
        return formatter;
    }

    public static int getCURRENT() {
        return CURRENT;
    }

    public static void setCURRENT(int CURRENT) {
        GeoJSONWriter.CURRENT = CURRENT;
    }
}
