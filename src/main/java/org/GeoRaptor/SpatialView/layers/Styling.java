package org.GeoRaptor.SpatialView.layers;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Stroke;
import java.util.Random;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.SpatialView.SupportClasses.LineStyle;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.LabelStyler;
import org.GeoRaptor.tools.Strings;
import org.geotools.util.logging.Logger;
import org.w3c.dom.Node;

public class Styling {

	private static final Logger LOGGER = org.geotools.util.logging.Logging 
			.getLogger("org.GeoRaptor.SpatialView.layers.Styling");

	public static enum STYLING_TYPE {
		NONE, CONSTANT, COLUMN, RANDOM
	};

	protected Color lineColor      = Color.BLACK;
	protected Color pointColor     = Color.BLACK;
	protected Color selectionColor = new Color(255, 255, 0, 80); // Yellow
	protected Color shadeColor     = Color.WHITE;

	protected Constants.GEOMETRY_LABEL_POINT geometryLabelPoint = Constants.GEOMETRY_LABEL_POINT.MIDDLE_VERTEX;
	protected Constants.TEXT_OFFSET_POSITION textOffsetPosition = Constants.TEXT_OFFSET_POSITION.BL;
	protected Constants.ROTATE rotationTarget                   = Constants.ROTATE.NONE;
	protected Constants.ROTATION_VALUES rotationValue           = Constants.ROTATION_VALUES.DEGREES;

	protected Constants.SEGMENT_ARROWS_TYPE segmentArrow  = Constants.SEGMENT_ARROWS_TYPE.NONE;
	protected Constants.SEGMENT_LABEL_TYPE markSegment    = Constants.SEGMENT_LABEL_TYPE.NONE;
	protected Constants.VERTEX_LABEL_TYPE markVertex      = Constants.VERTEX_LABEL_TYPE.NONE;
	protected Constants.TEXT_OFFSET_POSITION markPosition = Constants.TEXT_OFFSET_POSITION.CL;
	protected LineStyle.LINE_STROKES lineStrokeType       = LineStyle.LINE_STROKES.LINE_SOLID;
	protected LineStyle.LINE_STROKES selectLineStrokeType = LineStyle.LINE_STROKES.LINE_DASH;

	protected PointMarker.MARKER_TYPES markGeoPoints = PointMarker.MARKER_TYPES.NONE;
	protected PointMarker.MARKER_TYPES markGeoStart  = PointMarker.MARKER_TYPES.NONE;
	protected PointMarker.MARKER_TYPES pointType     = PointMarker.MARKER_TYPES.CROSS;
	protected Random randomColorGenerator            = new Random();
	protected SimpleAttributeSet labelAttributes     = null;
	protected SimpleAttributeSet markLabelAttributes = null;
	protected String labelColumn                     = null;

	protected String lineColorColumn  = null;
	protected String lineSeparator    = null;
	protected String pointColorColumn = null;
	protected String pointSizeColumn  = null;
	protected String rotationColumn   = null;
	protected String shadeColumn      = null;
	protected Styling.STYLING_TYPE lineColorType  = Styling.STYLING_TYPE.CONSTANT;
	protected Styling.STYLING_TYPE pointColorType = Styling.STYLING_TYPE.CONSTANT;
	protected Styling.STYLING_TYPE pointSizeType  = Styling.STYLING_TYPE.CONSTANT;
	protected Styling.STYLING_TYPE shadeType      = Styling.STYLING_TYPE.RANDOM;

	protected float selectionShadeTransLevel = 1.0f;
	protected float lineTransLevel           = 1.0f;
	protected float shadeTransLevel          = 1.0f;
	protected int markOffset          = 2;
	protected int labelOffset         = 4;
	protected int lineWidth           = 1;
	protected int pointSize           = 4;
	protected int selectLineWidth     = 3;
	protected Stroke lineStroke       = LineStyle.getStroke(lineStrokeType, lineWidth);
	protected Stroke selectLineStroke = LineStyle.getStroke(selectLineStrokeType, selectLineWidth);
	protected int selectPointSize     = 6;
	protected int textHiScale         = Integer.MAX_VALUE;
	protected int textLoScale         = 0;

	protected boolean orientVertexMark = false;
	protected boolean selectionActive  = false;
	
    protected Preferences preferences  = null;

	public Styling() {
		this.setLineSeparator(System.getProperty("line.separator"));
        this.preferences = MainSettings.getInstance().getPreferences();
        if ( this.preferences.isRandomRendering()) {
          this.setAllRandom();
        }
	}

	// Copy constructor
	public Styling(Styling _style) {
		this();
		if (_style == null )
			return;
		
		// Point
		this.setPointColorColumn(_style.getPointColorColumn());
		this.setPointColorType(_style.getPointColorType());
		this.setPointSizeColumn(_style.getPointSizeColumn());
		this.setPointSizeType(_style.getPointSizeType());
		this.setPointColor(_style.getPointColor(null));
		this.setPointSize(_style.getPointSize(4));
		this.setPointType(_style.getPointType());

		// Line
		this.setLineWidth(_style.getLineWidth());
		this.setLineColor(_style.getLineColor(null));
		this.setLineStrokeType(_style.getLineStrokeType());
		this.setLineSeparator(_style.getLineSeparator());
		this.setLineColorColumn(_style.getLineColorColumn());
		this.setLineColorType(_style.getLineColorType());
		this.setLineTransLevel(_style.getLineTransLevel());

		// Shade
		this.setShadeType(_style.getShadeType());
		this.setShadeColor(new Color(_style.getShadeColor(null).getRed(), this.getShadeColor(null).getBlue(),
        this.getShadeColor(null).getGreen(), this.getShadeColor(null).getAlpha()));
		this.setShadeColumn(_style.getShadeColumn());
		this.setShadeTransLevel(_style.getShadeTransLevel());

		// Label
		this.setLabelAttributes(_style.getLabelAttributes());
		this.setLabelColumn(_style.getLabelColumn());
		this.setLabelOffset(_style.getLabelOffset());
		this.setLabelPosition(_style.getLabelPosition());
		this.setTextLoScale(_style.getTextLoScale());
		this.setTextHiScale(_style.getTextLoScale());
		this.setGeometryLabelPoint(_style.getGeometryLabelPoint().toString());

		// Rotation
		this.setRotationColumn(_style.getRotationColumn());
		this.setRotationValue(_style.getRotationValue());
		this.setRotationTarget(_style.getRotationTarget());

		// Mark
		this.setMarkVertex(_style.getMarkVertex());
		this.setMarkGeoStart(_style.getMarkGeoStart());
		this.setMarkGeoPoints(_style.getMarkGeoPoints());
		this.setSegmentArrow(_style.getSegmentArrow());
		this.setMarkOriented(_style.isMarkOriented());
		this.setMarkSegment(_style.getMarkSegment());
		this.setMarkLabelAttributes(_style.getMarkLabelAttributes(Color.BLACK));
		this.setMarkLabelOffset(_style.getMarkLabelOffset());
		this.setMarkLabelPosition(_style.getMarkLabelPosition());

		// Selection
		this.setSelectionLineStrokeType(_style.getSelectionLineStrokeType());
		this.setSelectionLineWidth(_style.getSelectionLineWidth());
		this.setSelectionColor(_style.getSelectionColor());
		this.setSelectionShadeTransLevel(_style.getSelectionShadeTransLevel());
	}

	// XML Constructor
	//
	public Styling(Node _node) 
	{
		this();
		if (_node == null ) {
            System.out.println("Styling Constructor's Node is null");
			return; // Could throw error?
		}
		if ( ! _node.getNodeName().equals("Styling") ) {
            System.out.println("Styling Constructor's Node is " + _node.getNodeName());
			return; // Could throw error?
		}
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			
			// Point
			this.setPointSize(
                  Integer.valueOf(
              		 Strings.isEmpty((String)xpath.evaluate("PointSize/text()",_node,XPathConstants.STRING))
                     ? "4" :         (String)xpath.evaluate("PointSize/text()",_node,XPathConstants.STRING)
                   ).intValue() 
            );
			this.setPointSizeType(   (String)xpath.evaluate("PointSizeType/text()",_node,XPathConstants.STRING));
			this.setPointSizeColumn( (String)xpath.evaluate("PointSizeTypeColumn/text()",_node,XPathConstants.STRING));
			this.setPointColor(
                    Colours.fromRGBa((String)xpath.evaluate("PointColor/text()",_node,XPathConstants.STRING)));
			this.setPointType(       (String)xpath.evaluate("PointType/text()",_node,XPathConstants.STRING));
			this.setPointColorColumn((String)xpath.evaluate("PointColorColumn/text()",_node,XPathConstants.STRING));
			this.setPointColorType(  (String)xpath.evaluate("PointColorType/text()",_node,XPathConstants.STRING));
			
			// Line
			this.setLineWidth(
				Integer.valueOf(
					Strings.isEmpty( (String)xpath.evaluate("LineWidth/text()",_node,XPathConstants.STRING))
					         ? "1" : (String)xpath.evaluate("LineWidth/text()",_node,XPathConstants.STRING)
                ).intValue()); 
			this.setLineColor(
					Colours.fromRGBa( (String)xpath.evaluate("LineColor/text()",_node,XPathConstants.STRING)));
			this.setLineStrokeType(   (String)xpath.evaluate("LineStrokeType/text()",_node,XPathConstants.STRING)); 
			this.setLineTransLevel(
                  Double.valueOf(
                     Strings.isEmpty( (String)xpath.evaluate("LineTransLevel/text()",_node,XPathConstants.STRING))
                           ? "1.0" :  (String)xpath.evaluate("LineTransLevel/text()",_node,XPathConstants.STRING)
                     ).floatValue());
			this.setLineColorColumn(  (String)xpath.evaluate("LineColorColumn/text()",_node,XPathConstants.STRING));
			this.setLineColorType(    (String)xpath.evaluate("LineColorType/text()",_node,XPathConstants.STRING));
			this.setLineSeparator(    (String)xpath.evaluate("LineSeparator/text()",_node,XPathConstants.STRING));

			// Shade
			this.setShadeColumn(      (String)xpath.evaluate("ShadeColumn/text()",_node,XPathConstants.STRING));
			this.setShadeType(        (String)xpath.evaluate("ShadeType/text()",_node,XPathConstants.STRING));
			this.setShadeColor(
					Colours.fromRGBa( (String)xpath.evaluate("ShadeColor/text()",_node,XPathConstants.STRING))); 
			this.setShadeTransLevel(
                    Double.valueOf(
                     Strings.isEmpty( (String)xpath.evaluate("ShadeTransLevel/text()",_node,XPathConstants.STRING))
                           ? "1.0" :  (String)xpath.evaluate("LineTransLevel/text()",_node,XPathConstants.STRING)
                     ).floatValue());
			
			// Label
			this.setLabelColumn(      (String)xpath.evaluate("Label/text()", _node, XPathConstants.STRING));			
			this.setTextLoScale(      (String)xpath.evaluate("LabelLoScale/text()",_node,XPathConstants.STRING));
			this.setTextHiScale(      (String)xpath.evaluate("LabelHiScale/text()",_node,XPathConstants.STRING));
			this.setLabelPosition(    (String)xpath.evaluate("LabelPosition/text()",_node,XPathConstants.STRING));
			this.setLabelOffset(      (String)xpath.evaluate("LabelOffset/text()",_node,XPathConstants.STRING));
			this.setLabelAttributes(
                 LabelStyler.fromXML((Node)xpath.evaluate("LabelAttributes/LabelStyler",_node,XPathConstants.NODE)));
			this.setGeometryLabelPoint( 
					                  (String)xpath.evaluate("GeometryLabelPoint/text()",_node,XPathConstants.STRING));

			// Rotation
			this.setRotationColumn(   (String)xpath.evaluate("RotationColumn/text()",_node,XPathConstants.STRING));
			this.setRotationValue(    (String)xpath.evaluate("RotationValue/text()",_node,XPathConstants.STRING));
			this.setRotationTarget(   (String)xpath.evaluate("RotationTarget/text()",_node,XPathConstants.STRING));
			
			// Mark
			this.setMarkGeoStart(     (String)xpath.evaluate("MarkGeoStart/text()",_node,XPathConstants.STRING));
			this.setMarkGeoPoints(    (String)xpath.evaluate("MarkGeoPoints/text()",_node,XPathConstants.STRING));
			this.setMarkVertex(       (String)xpath.evaluate("MarkVertex/text()",_node,XPathConstants.STRING));
			this.setMarkOriented(
					 Boolean.valueOf( (String)xpath.evaluate("MarkOriented/text()",_node,XPathConstants.STRING)).booleanValue());
			this.setSegmentArrow(     (String)xpath.evaluate("MarkLineDir/text()",_node,XPathConstants.STRING));
			this.setMarkLabelPosition((String)xpath.evaluate("MarkPosition/text()",_node,XPathConstants.STRING));
			this.setMarkLabelOffset(  (String)xpath.evaluate("MarkOffset/text()",_node,XPathConstants.STRING));
            this.setMarkLabelAttributes(
                  LabelStyler.fromXML((Node)  xpath.evaluate("MarkLabelAttributes/LabelStyler",_node,XPathConstants.NODE))); 
			this.setMarkSegment(      (String)xpath.evaluate("MarkSegment/text()",_node,XPathConstants.STRING));

			// Selection
			this.setSelectionPointSize(
                    Integer.valueOf(
                     Strings.isEmpty( (String)xpath.evaluate("SelectPointSize/text()",_node,XPathConstants.STRING))
                    		 ? "4" :  (String)xpath.evaluate("SelectPointSize/text()",_node,XPathConstants.STRING)
                    ).intValue());
			this.setSelectionLineWidth(
                    Integer.valueOf(
                     Strings.isEmpty( (String)xpath.evaluate("SelectLineWidth/text()",_node,XPathConstants.STRING))
                             ? "1" :  (String)xpath.evaluate("SelectLineWidth/text()",_node,XPathConstants.STRING)
                    ).intValue());
			this.setSelectionColor(
					Colours.fromRGBa( (String)xpath.evaluate("SelectionColor/text()",_node,XPathConstants.STRING)));
			this.setSelectionShadeTransLevel(
                    Double.valueOf(
                     Strings.isEmpty( (String)xpath.evaluate("SelectShadeTransLevel/text()",_node,XPathConstants.STRING))
                           ? "1.0" :  (String)xpath.evaluate("SelectShadeTransLevel/text()",_node,XPathConstants.STRING)
                    ).floatValue());
			
		} catch (XPathExpressionException xe) {
			LOGGER.error("SVLayer.fromXML: XPathExpressionException " + xe.toString());
		}
	}

	public String toXML(Color _mapBackground) {
		try {
			String xml = "<Styling>";
			
           xml += String.format(
					  "<Label>%s</Label><RotationColumn>%s</RotationColumn><RotationValue>%s</RotationValue><RotationTarget>%s</RotationTarget>" 
					+ "<LabelPosition>%s</LabelPosition><LabelLoScale>%s</LabelLoScale><LabelHiScale>%s</LabelHiScale>"
					+ "<LabelOffset>%s</LabelOffset><LabelAttributes>%s</LabelAttributes>"
					+ "<GeometryLabelPoint>%s</GeometryLabelPoint>",
					this.getLabelColumn(), 
					this.getRotationColumn(), 
					this.getRotationValue().toString(), 
					this.getRotationTarget().toString(),
					this.getLabelPosition().toString(), 
					this.getTextLoScale(),
					this.getTextHiScale(),
					String.valueOf(this.getLabelOffset()), 
					LabelStyler.toXML(this.getLabelAttributes()),
					this.getGeometryLabelPoint().toString() );
					
			xml += String.format(
                      "<PointSize>%d</PointSize><PointSizeType>%s</PointSizeType><PointSizeTypeColumn>%s</PointSizeTypeColumn>"
					+ "<PointColor>%d</PointColor><PointType>%s</PointType><PointColorColumn>%s</PointColorColumn><PointColorType>%s</PointColorType>",
					this.getPointSize(4),
					this.getPointSizeType().toString(), 
					this.getPointSizeColumn(),					
					this.getPointColor(null).getRGB(),
					this.getPointType().toString(), 
					this.getPointColorColumn(), 
					this.getPointColorType().toString() );
			
			xml += String.format(
					"<LineWidth>%d</LineWidth><LineColor>%d</LineColor><LineStrokeType>%s</LineStrokeType><LineTransLevel>%s</LineTransLevel><LineColorColumn>%s</LineColorColumn><LineColorType>%s</LineColorType><LineSeparator>%s</LineSeparator>",
					this.getLineWidth(), 
					this.getLineColor(null).getRGB(),
					this.getLineStrokeType().toString(), 
					this.getLineTransLevel(), 
					this.getLineColorColumn(),
					this.getLineColorType().toString(),
					this.getLineSeparator() );
					
			xml += String.format(
					"<ShadeColumn>%s</ShadeColumn><ShadeType>%s</ShadeType><ShadeColor>%d</ShadeColor><ShadeTransLevel>%s</ShadeTransLevel>",
					this.getShadeColumn(), 
					this.getShadeType().toString(),
					this.getShadeColor(null).getRGB(), 
					this.getShadeTransLevel() );

			xml += String.format(
					"<SelectPointSize>%d</SelectPointSize><SelectLineWidth>%d</SelectLineWidth><SelectionColor>%d</SelectionColor><SelectShadeTransLevel>%s</SelectShadeTransLevel>",
					this.getSelectionPointSize(),
					this.getSelectionLineWidth(), 
					this.getSelectionColor().getRGB(),
					this.getSelectionShadeTransLevel() );
			
			xml += String.format(
					"<MarkGeoStart>%s</MarkGeoStart><MarkGeoPoints>%s</MarkGeoPoints><MarkVertex>%s</MarkVertex><MarkOriented>%s</MarkOriented><MarkLineDir>%s</MarkLineDir><MarkPosition>%s</MarkPosition>",
					String.valueOf(this.getMarkGeoStart()),
					String.valueOf(this.getMarkGeoPoints()), 
					this.getMarkVertex().toString(),
					String.valueOf(this.isMarkOriented()),
					this.getSegmentArrow().toString(),
					this.getMarkLabelPosition().toString() ); 

			xml += String.format(
					"<MarkOffset>%s</MarkOffset><MarkLabelAttributes>%s</MarkLabelAttributes><MarkSegment>%s</MarkSegment>",					
					String.valueOf(this.getMarkLabelOffset()),
					LabelStyler.toXML(this.getMarkLabelAttributes(_mapBackground)), 
					this.getMarkSegment().toString() );
			
			xml += "</Styling>"; 

			return xml;
		} catch (Exception e) {
			LOGGER.error("Error converting styling to text " + e.toString());
			return "";
		}
	}
	
	@Override
	public String toString() {
		Preferences mainPrefs;
		oracle.ide.config.Preferences prefs = oracle.ide.config.Preferences.getPreferences();
		mainPrefs = Preferences.getInstance(prefs);
		return this.toXML(mainPrefs.getMapBackground());
	}

	// *****************************************************************************
	
	public int getTextLoScale() {
		return this.textLoScale;
	}

	public void setTextLoScale(int _loScale) {
		this.textLoScale = _loScale;
	}

	public void setTextLoScale(String _loScale) {
		if (Strings.isEmpty(_loScale))
			this.textLoScale = 0;
		try {
			this.textLoScale = Integer.valueOf(_loScale);
		} catch (Exception e) {
			return;
		}
	}

	public int getTextHiScale() {
		return this.textHiScale;
	}

	public void setTextHiScale(int _hiScale) {
		this.textHiScale = _hiScale;
	}

	public void setTextHiScale(String _hiScale) {
		if (Strings.isEmpty(_hiScale))
			this.textHiScale = Integer.MAX_VALUE;
		try {
			this.textHiScale = Integer.valueOf(_hiScale);
		} catch (Exception e) {
			return;
		}
	}

	public void setRotationTarget(String _rotate) {
		try {
			this.rotationTarget = Strings.isEmpty(_rotate) ? Constants.ROTATE.NONE
					: Constants.ROTATE.valueOf(_rotate.toUpperCase());
		} catch (Exception e) {
			this.rotationTarget = Constants.ROTATE.NONE;
		}
	}

	public void setRotationTarget(Constants.ROTATE _rotate) {
		this.rotationTarget = _rotate;
	}

	public Constants.ROTATE getRotationTarget() {
		return this.rotationTarget;
	}

	public void setRotationValue(String _rotationValue) {
		try {
			this.rotationValue = Strings.isEmpty(_rotationValue) ? Constants.ROTATION_VALUES.DEGREES
					: Constants.ROTATION_VALUES.valueOf(_rotationValue.toUpperCase());
		} catch (Exception e) {
			this.rotationValue = Constants.ROTATION_VALUES.DEGREES;
		}
	}

	public void setRotationValue(Constants.ROTATION_VALUES _rotationValue) {
		this.rotationValue = _rotationValue;
	}

	public Constants.ROTATION_VALUES getRotationValue() {
		return this.rotationValue;
	}

	public void setRotationColumn(String _rotationColumn) {
		if (Strings.isEmpty(_rotationColumn) || _rotationColumn.equalsIgnoreCase("null")) // Because saving to XML can
																							// replace null with string
																							// "null"
			this.rotationColumn = null;
		else
			this.rotationColumn = _rotationColumn;
	}

	public String getRotationColumn() {
		return this.rotationColumn == null ? "" : this.rotationColumn;
	}

	public static AlphaComposite getAlphaComposite(float _shadeTransLevel) {
		AlphaComposite tempAlphaComposite = null;
		if ((0.0f <= _shadeTransLevel) && (_shadeTransLevel <= 1.0f)) {
			// create alpha composition
			tempAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, _shadeTransLevel);
		}
		return tempAlphaComposite;
	}

	public void setLabelOffset(String _lOffset) {
		try {
			this.labelOffset = Strings.isEmpty(_lOffset) ? 0 : Integer.valueOf(_lOffset).intValue();
		} catch (Exception e) {
			this.labelOffset = 0;
		}
	}

	public void setLabelOffset(int _lOffset) {
		this.labelOffset = _lOffset;
	}

	public int getLabelOffset() {
		return this.labelOffset;
	}

	/* For when marking vertices etc with symbols and text */

	public void setMarkLabelOffset(String _lOffset) {
		try {
			this.markOffset = Strings.isEmpty(_lOffset) ? 0 : Integer.valueOf(_lOffset).intValue();
		} catch (Exception e) {
			this.markOffset = 0;
		}
	}

	public void setMarkLabelOffset(int _lOffset) {
		this.markOffset = _lOffset;
	}

	public int getMarkLabelOffset() {
		return this.markOffset;
	}

	public void setMarkOriented(boolean _orient) {
		this.orientVertexMark = _orient;
	}

	public boolean isMarkOriented() {
		return this.orientVertexMark;
	}

	/**
	 * @method setTextOffsetPosition
	 * @param _lPosition
	 * @history
	 */
	public void setTextOffsetPosition(String _lPosition) {
		if (Strings.isEmpty(_lPosition))
			this.textOffsetPosition = Constants.TEXT_OFFSET_POSITION.BL;
		else
			this.textOffsetPosition = Constants.TEXT_OFFSET_POSITION.valueOf(_lPosition);
	}

	public void setTextOffsetPosition(Constants.TEXT_OFFSET_POSITION _lPosition) {
		this.textOffsetPosition = _lPosition;
	}

	public Constants.TEXT_OFFSET_POSITION getMarkLabelPosition() {
		return this.markPosition;
	}

	public void setMarkLabelPosition(String _lPosition) {
		if (Strings.isEmpty(_lPosition))
			this.markPosition = Constants.TEXT_OFFSET_POSITION.CL;
		else
			this.markPosition = Constants.TEXT_OFFSET_POSITION.valueOf(_lPosition);
	}

	public void setMarkLabelPosition(Constants.TEXT_OFFSET_POSITION _lPosition) {
		this.markPosition = _lPosition;
	}

	public void setLabelColumn(String _labelColumn) {
		if (Strings.isEmpty(_labelColumn) || _labelColumn.equalsIgnoreCase("null")) // Because saving to XML can replace
																					// null with string "null"
			this.labelColumn = null;
		else
			this.labelColumn = _labelColumn;
	}

	public String getLabelColumn() {
		return this.labelColumn;
	}

	public void setLabelAttributes(SimpleAttributeSet _labelAttributes) {
		this.labelAttributes = new SimpleAttributeSet(_labelAttributes);
	}

	public SimpleAttributeSet getLabelAttributes() {
		return getLabelAttributes(Color.BLACK);
	}

	public SimpleAttributeSet getLabelAttributes(Color _mapBackground) {
		if (this.labelAttributes == null) {
			this.labelAttributes = LabelStyler.getDefaultAttributes(_mapBackground);
		}
		return new SimpleAttributeSet(this.labelAttributes);
	}

	public void setLabelAttributes(Node _labelAttributes) {
		this.labelAttributes = LabelStyler.fromXML(_labelAttributes);
	}

	public void setLabelPosition(String _labelPoint) {
		if (Strings.isEmpty(_labelPoint))
			this.textOffsetPosition = Constants.TEXT_OFFSET_POSITION.CC;
		else 			
			this.textOffsetPosition = Constants.TEXT_OFFSET_POSITION.valueOf(_labelPoint);
	}

	public void setLabelPosition(Constants.TEXT_OFFSET_POSITION _labelPosition) {
		this.textOffsetPosition = _labelPosition;
	}

	public Constants.TEXT_OFFSET_POSITION getLabelPosition() {
		return this.textOffsetPosition;
	}

	public void setGeometryLabelPoint(String _labelGeometryPoint) {
		if (Strings.isEmpty(_labelGeometryPoint))
			this.geometryLabelPoint = Constants.GEOMETRY_LABEL_POINT.MIDDLE_VERTEX;
		else
			try {
				this.geometryLabelPoint = Constants.GEOMETRY_LABEL_POINT.valueOf(_labelGeometryPoint);
			} catch (Exception e) {
				this.geometryLabelPoint = Constants.GEOMETRY_LABEL_POINT.FIRST_VERTEX;
			}
	}

	public void setGeometryLabelPoint(Constants.GEOMETRY_LABEL_POINT _labelGeometryPosition) {
		this.geometryLabelPoint = _labelGeometryPosition;
	}

	public Constants.GEOMETRY_LABEL_POINT getGeometryLabelPoint() {
		return this.geometryLabelPoint;
	}

	public void setMarkLabelAttributes(SimpleAttributeSet _labelAttributes) {
		this.markLabelAttributes = new SimpleAttributeSet(_labelAttributes);
	}

	public SimpleAttributeSet getMarkLabelAttributes() {
		return this.getMarkLabelAttributes(Color.BLACK);
	}

	public SimpleAttributeSet getMarkLabelAttributes(Color _mapBackground) {
		if (this.markLabelAttributes == null) {
			this.markLabelAttributes = LabelStyler.getDefaultAttributes(_mapBackground);
		}
		return new SimpleAttributeSet(this.markLabelAttributes);
	}

	public void setMarkLabelAttributes(Node _labelAttributes, 
			                          Color _mapBackground)
	{
		if (_labelAttributes == null) { /* SGG Changed from != to == Aug 2019*/
			this.markLabelAttributes = LabelStyler.getDefaultAttributes(_mapBackground);
		} else {
			this.markLabelAttributes = LabelStyler.fromNode(_labelAttributes.getChildNodes().item(0));
		}
	}

	public Color getLabelColor() {
		if (this.labelAttributes == null)
			return Color.BLACK;
		else
			return StyleConstants.getForeground(this.labelAttributes);
	}

	public void setPointColorColumn(String _columnName) {
		if (Strings.isEmpty(_columnName) || _columnName.equalsIgnoreCase("null")) {
			this.pointColorColumn = null;
		} else {
			this.pointColorColumn = _columnName;
		}
	}

	public String getPointColorColumn() {
		return this.pointColorColumn == null ? "" : this.pointColorColumn;
	}

	public void setPointColorType(Styling.STYLING_TYPE _colorType) {
		this.pointColorType = _colorType;
	}

	public void setPointColorType(String _colorType) {
		this.pointColorType = Strings.isEmpty(_colorType) || _colorType.equalsIgnoreCase("null")
				? Styling.STYLING_TYPE.CONSTANT
				: Styling.STYLING_TYPE.valueOf(_colorType);
	}

	public Styling.STYLING_TYPE getPointColorType() {
		return this.pointColorType == null ? Styling.STYLING_TYPE.CONSTANT : this.pointColorType;
	}

	public void setPointColor(Color _pointColor) {
		this.pointColor = _pointColor;
	}

	public boolean isSelectionActive() {
		return this.selectionActive;
	}

	public void setSelectionActive(boolean _selectionActive) {
		this.selectionActive = _selectionActive;
	}

	public void setAllRandom() {
		Random sizeGen = new Random(System.currentTimeMillis());
		int pointSize = sizeGen.nextInt(16);
		while (pointSize < 4) {
			pointSize = sizeGen.nextInt(16);
		}
		this.setPointSize(pointSize);
		this.setPointType(PointMarker.getRandomMarker());
		LOGGER.debug("pointSize set to " + pointSize + " pointType set to " + this.getPointType().toString());
		this.setPointColor(Colours.getRandomColor());
		this.setLineColor(Colours.getRandomColor());
		this.setShadeColor(Colours.getRandomColor());
		this.setShadeType("RANDOM");
		this.setSelectionActive(false);
		// Set all trans levels to SOLID
		this.setLineTransLevel(1.0f);
		this.setShadeTransLevel(1.0f);
		this.setSelectionShadeTransLevel(1.0f);
	}

	/**
	 * @method getPointColor
	 * @param _columnValue
	 * @return Color
	 * @history Simon Greener, 8th January 2011, Fixed bug with COLUMN based
	 *          rendering
	 */
	public Color getPointColor(String _columnValue)
	{
		if (this.isSelectionActive()) {
			return this.getSelectionColor();
		} else if (this.getPointColorType() == Styling.STYLING_TYPE.CONSTANT) {
			return (this.pointColor == null) ? Color.BLACK : new Color(this.pointColor.getRGB());
		} else if (this.getPointColorType() == Styling.STYLING_TYPE.RANDOM) {
			// To try and be a little faster we don't use Tools.getRandomColor()
			return new Color(this.randomColorGenerator.nextInt(256), this.randomColorGenerator.nextInt(256),
					this.randomColorGenerator.nextInt(256));
		} else if (this.getPointColorType() == Styling.STYLING_TYPE.COLUMN) {
			if (Strings.isEmpty(_columnValue)) {
				return Color.GRAY; // Hint that the colour of the objects is not fixed.
			} else {
				// from RGBa also handles integer colours
				return Colours.fromRGBa(_columnValue);
			}
		}
		return (this.pointColor == null) ? Color.BLACK : new Color(this.pointColor.getRGB());
	}

	public void setPointSizeColumn(String _sizeColumn) {
		if (Strings.isEmpty(_sizeColumn) || _sizeColumn.equalsIgnoreCase("null"))
			this.pointSizeColumn = null;
		else {
			this.pointSizeColumn = _sizeColumn;
		}
	}

	public String getPointSizeColumn() {
		return this.pointSizeColumn;
	}

	public Styling.STYLING_TYPE getPointSizeType() {
		return this.pointSizeType;
	}

	public void setPointSizeType(Styling.STYLING_TYPE _sizeType) {
		this.pointSizeType = _sizeType;
	}

	public void setPointSizeType(String _sizeType) {
		try {
			this.pointSizeType = Strings.isEmpty(_sizeType) ? Styling.STYLING_TYPE.CONSTANT
					: Styling.STYLING_TYPE.valueOf(_sizeType.toUpperCase());
		} catch (Exception e) {
			this.pointSizeType = Styling.STYLING_TYPE.CONSTANT;
		}
	}

	public int getPointSize(int _defaultSize) 
	{
		if (this.isSelectionActive()) {
			return this.selectPointSize;
		} else if (this.getPointSizeType() == Styling.STYLING_TYPE.CONSTANT) {
			return this.pointSize;
		} else if (this.getPointSizeType() == Styling.STYLING_TYPE.COLUMN) {
			return _defaultSize;
		}
		return this.pointSize;
	}

	public void setPointSize(int _pointSize) {
		this.pointSize = _pointSize;
	}

	public void setPointSize(String _pointSize) {
		this.pointSize = Strings.isEmpty(_pointSize)?4:Integer.valueOf(_pointSize);
	}

	public boolean isPointMarked() {
		return this.pointType != PointMarker.MARKER_TYPES.NONE;
	}

	public PointMarker.MARKER_TYPES getPointType() {
		return this.pointType;
	}

	public void setPointType(String _pointType) {
		try {
			this.pointType = Strings.isEmpty(_pointType) ? PointMarker.MARKER_TYPES.CIRCLE
					: PointMarker.MARKER_TYPES.valueOf(_pointType.toUpperCase());
		} catch (Exception e) {
			this.pointType = PointMarker.MARKER_TYPES.CIRCLE;
		}
	}

	public void setPointType(PointMarker.MARKER_TYPES _pointType) {
		this.pointType = _pointType;
	}

	/**
	 * ************************************************* Line
	 * **************************************************
	 **/

	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	public String getLineSeparator() {
		return this.lineSeparator;
	}

	public void setLineColor(Color _lineColor) {
		this.lineColor = _lineColor;
	}

	public void setLineColorType(Styling.STYLING_TYPE _colorType) {
		this.lineColorType = _colorType;
	}

	public void setLineTransLevel(float _lineTransLevel) {
		this.lineTransLevel = _lineTransLevel;
	}

	public void setLineColorType(String _colorType) {
		this.lineColorType = Strings.isEmpty(_colorType) || _colorType.equalsIgnoreCase("null")
				? Styling.STYLING_TYPE.CONSTANT
				: Styling.STYLING_TYPE.valueOf(_colorType);
	}

	public Styling.STYLING_TYPE getLineColorType() {
		return this.lineColorType == null ? Styling.STYLING_TYPE.CONSTANT : this.lineColorType;
	}

	public float getLineTransLevel() {
		return this.lineTransLevel;
	}

	public int getLineTransLevelAsPercent() {
		return Math.round(this.getLineTransLevel() * 100.0f);
	}

	public void setLineColorColumn(String _columnName) {
		if (Strings.isEmpty(_columnName) || _columnName.equalsIgnoreCase("null")) {
			this.lineColorColumn = null;
		} else {
			this.lineColorColumn = _columnName;
		}
	}

	public String getLineColorColumn() {
		return this.lineColorColumn == null ? "" : this.lineColorColumn;
	}

	public int getLineWidth() {
		if (this.isSelectionActive())
			return this.selectLineWidth < 1 ? 1 : this.selectLineWidth;
		else
			return this.lineWidth < 1 ? 1 : this.lineWidth;
	}

	public Color getLineColor(String _columnValue) 
	{
		if (this.isSelectionActive()) {
			return this.getSelectionColor();
		} else if (this.getLineColorType() == Styling.STYLING_TYPE.CONSTANT) {
			return (this.lineColor == null) ? Color.BLACK : new Color(this.lineColor.getRGB());
		} else if (this.getLineColorType() == Styling.STYLING_TYPE.RANDOM) {
			// To try and be a little faster we don't use Tools.getRandomColor()
			return new Color(this.randomColorGenerator.nextInt(256), this.randomColorGenerator.nextInt(256),
					this.randomColorGenerator.nextInt(256));
		} else if (this.getLineColorType() == Styling.STYLING_TYPE.COLUMN) {
			if (Strings.isEmpty(_columnValue)) {
				return Color.GRAY; // Hint that the colour of the objects is not fixed.
			} else {
				return Colours.fromRGBa(_columnValue);
			}
		} else {
			return (this.lineColor == null) ? Color.BLACK : new Color(this.lineColor.getRGB());
		}
	}

	public void setLineStrokeType(String _strokeType) {
		try {
			this.setLineStrokeType(Strings.isEmpty(_strokeType) ? LineStyle.LINE_STROKES.LINE_SOLID
					: LineStyle.LINE_STROKES.valueOf(_strokeType.toUpperCase()));
		} catch (Exception e) {
			this.setLineStrokeType(LineStyle.LINE_STROKES.LINE_SOLID);
		}
	}

	public void setLineStrokeType(LineStyle.LINE_STROKES _strokeType) {
		this.lineStrokeType = _strokeType;
		this.lineStroke = LineStyle.getStroke(_strokeType, this.getLineWidth());
	}

	public LineStyle.LINE_STROKES getLineStrokeType() {
		if (this.isSelectionActive()) {
			return this.selectLineStrokeType;
		} else {
			return this.lineStrokeType;
		}
	}

	public AlphaComposite getLineAlphaComposite() {
		return Styling.getAlphaComposite(this.lineTransLevel);
	}

	public Stroke getLineStroke() {
		if (this.isSelectionActive())
			return this.getSelectionLineStroke();
		else {
			if (this.lineStroke == null)
				this.lineStroke = LineStyle.getStroke(this.getLineStrokeType(), this.getLineWidth());
			return this.lineStroke;
		}
	}

	public void setLineWidth(int _lineWidth) {
		if (this.lineWidth != _lineWidth) {
			this.lineWidth = _lineWidth;
			this.lineStroke = LineStyle.getStroke(this.getLineStrokeType(), _lineWidth);
		}
	}

	public void setMarkGeoStart(PointMarker.MARKER_TYPES _marker) {
		this.markGeoStart = _marker == null ? PointMarker.MARKER_TYPES.NONE : _marker;
	}

	public void setMarkGeoStart(String _markGeoStart) {
		try {
			this.markGeoStart = PointMarker.MARKER_TYPES.valueOf(_markGeoStart);
		} catch (Exception e) {
			this.markGeoStart = PointMarker.MARKER_TYPES.NONE;
		}
	}

	public PointMarker.MARKER_TYPES getMarkGeoStart() {
		return this.markGeoStart;
	}

	public boolean isMarkGeoStart() {
		return this.markGeoStart != PointMarker.MARKER_TYPES.NONE;
	}

	public void setMarkGeoPoints(PointMarker.MARKER_TYPES _marker) {
		this.markGeoPoints = _marker == null ? PointMarker.MARKER_TYPES.NONE : _marker;
	}

	public void setMarkGeoPoints(String _markGeoPoints) {
		try {
			this.markGeoPoints = PointMarker.MARKER_TYPES.valueOf(_markGeoPoints);
		} catch (Exception e) {
			this.markGeoPoints = PointMarker.MARKER_TYPES.NONE;
		}
	}

	public PointMarker.MARKER_TYPES getMarkGeoPoints() {
		return this.markGeoPoints;
	}

	public boolean isMarkGeoPoints() {
		return this.markGeoPoints != PointMarker.MARKER_TYPES.NONE;
	}

	public void setMarkVertex(String _mark) {
		try {
			this.setMarkVertex(Strings.isEmpty(_mark) ? Constants.VERTEX_LABEL_TYPE.NONE
					: Constants.VERTEX_LABEL_TYPE.valueOf(_mark));
		} catch (Exception e) {
			this.setMarkVertex(Constants.VERTEX_LABEL_TYPE.NONE);
		}
	}

	public void setMarkSegment(Constants.SEGMENT_LABEL_TYPE _mark) {
		this.markSegment = _mark;
	}

	public void setMarkSegment(String _mark) {
		try {
			this.setMarkSegment(Strings.isEmpty(_mark) ? Constants.SEGMENT_LABEL_TYPE.NONE
					: Constants.SEGMENT_LABEL_TYPE.valueOf(_mark));
		} catch (Exception e) {
			this.setMarkSegment(Constants.SEGMENT_LABEL_TYPE.NONE);
		}
	}

	public Constants.SEGMENT_LABEL_TYPE getMarkSegment() {
		return this.markSegment;
	}

	public boolean isMarkSegment() {
		return this.markSegment.compareTo(Constants.SEGMENT_LABEL_TYPE.NONE) != 0;
	}

	public void setMarkVertex(Constants.VERTEX_LABEL_TYPE _mark) {
		this.markVertex = _mark;
	}

	public Constants.VERTEX_LABEL_TYPE getMarkVertex() {
		return this.markVertex;
	}

	public boolean isMarkVertex() {
		return this.markVertex != Constants.VERTEX_LABEL_TYPE.NONE;
	}

	public void setSegmentArrow(Constants.SEGMENT_ARROWS_TYPE _segmentArrow) {
		if (_segmentArrow == null) {
			this.segmentArrow = Constants.SEGMENT_ARROWS_TYPE.NONE;
		} else {
			this.segmentArrow = _segmentArrow;
		}
	}

	public void setSegmentArrow(String _segmentArrow) {
		try {
			String directionType = Strings.isEmpty(_segmentArrow) ? Constants.SEGMENT_ARROWS_TYPE.NONE.toString()
					: _segmentArrow.replace(" ", "_").toUpperCase();
			this.setSegmentArrow(Constants.SEGMENT_ARROWS_TYPE.valueOf(directionType));
		} catch (Exception e) {
			this.setSegmentArrow(Constants.SEGMENT_ARROWS_TYPE.NONE);
		}
	}

	public Constants.SEGMENT_ARROWS_TYPE getSegmentArrow() {
		return this.segmentArrow;
	}

	public boolean isSegmentArrows() {
		return this.segmentArrow.compareTo(Constants.SEGMENT_ARROWS_TYPE.NONE) != 0;
	}

	/**
	 * ************************************************* Shade / Fill
	 * **************************************************
	 **/

	public void setShadeColumn(String _labelColumn) {
		if (Strings.isEmpty(_labelColumn) || _labelColumn.equalsIgnoreCase("null"))
			this.shadeColumn = null;
		else {
			this.shadeColumn = _labelColumn;
		}
	}

	public String getShadeColumn() {
		return this.shadeColumn;
	}

	public void setShadeType(Styling.STYLING_TYPE _shadeType) {
		this.shadeType = _shadeType;
	}

	public void setShadeType(String _shadeType) {
		this.shadeType = Strings.isEmpty(_shadeType) || _shadeType.equalsIgnoreCase("null") ? Styling.STYLING_TYPE.NONE
				: Styling.STYLING_TYPE.valueOf(_shadeType);
	}

	public Styling.STYLING_TYPE getShadeType() {
		return this.shadeType;
	}

	public void setShadeColor(Color _shadeColor) {
		if (_shadeColor == null)
			this.shadeColor = Color.BLACK;
		else
			this.shadeColor = _shadeColor;
	}

	/* @tobedone: handle alpha
	 * 
	 */
	public Color getShadeColor(String _columnValue) 
	{
		if (this.isSelectionActive()) {
			return this.getSelectionColor();
		} else {
			if (this.getShadeType() == Styling.STYLING_TYPE.RANDOM) {
				// To try and be a little faster we don't use Tools.getRandomColor()
				return new Color(
						this.randomColorGenerator.nextInt(256), 
						this.randomColorGenerator.nextInt(256),
						this.randomColorGenerator.nextInt(256));
			} else {
				Color retColor = Color.BLACK;
				if (this.getShadeType() == Styling.STYLING_TYPE.COLUMN) 
				{
					try {
						if (Strings.isEmpty(_columnValue)) {
							// Hint that the colour of the objects is not fixed.
							retColor = Color.GRAY; 
						} else {
							// from RGBa also handles integer colours
							retColor = Colours.fromRGBa(_columnValue);
						}
					} catch (Exception e) {
						retColor = Color.BLACK;
					}
				} else {
					retColor = this.shadeColor == null ? Color.BLACK : new Color(this.shadeColor.getRGB());
				}
				return retColor;
			}
		}
	}

	public void setShadeTransLevel(float _shadeTransLevel) {
		this.shadeTransLevel = _shadeTransLevel;
	}

	public float getShadeTransLevel() {
		return this.shadeTransLevel;
	}

	public int getShadeTransLevelAsPercent() {
		return Math.round(this.getShadeTransLevel() * 100.0f);
	}

	public AlphaComposite getShadeAlphaComposite() {
		if (this.isSelectionActive())
			return Styling.getAlphaComposite(this.selectionShadeTransLevel);
		else
			return Styling.getAlphaComposite(this.shadeTransLevel);
	}

	/**
	 * Do we perform shade operation
	 */
	public boolean isPerformShade() {
		return this.shadeType != Styling.STYLING_TYPE.NONE;
	}

	/**
	 * Do we perform shade operation
	 */
	public boolean isPerformLine() {
		return this.lineColorType != Styling.STYLING_TYPE.NONE;
	}

	/**
	 * Do we perform shade transparency operations
	 */
	public boolean isPerformShadeTrans() {
		return this.shadeType != Styling.STYLING_TYPE.NONE && this.shadeTransLevel != 1.0f;
	}

	public boolean isPerformLineTrans() {
		return this.lineColorType != Styling.STYLING_TYPE.NONE && this.shadeTransLevel != 1.0f;
	}

	public void setSelectionLineWidth(int _selectLineWidth) {
		if (this.selectLineWidth != _selectLineWidth) {
			this.selectLineWidth = _selectLineWidth;
			this.selectLineStroke = LineStyle.getStroke(this.getSelectionLineStrokeType(), _selectLineWidth);
		}
	}

	public int getSelectionPointSize() {
		return this.selectPointSize;
	}

	public void setSelectionPointSize(int _selectPointSize) {
		this.selectPointSize = _selectPointSize;
	}

	public int getSelectionLineWidth() {
		return selectLineWidth;
	}

	public void setSelectionLineStrokeType(String _strokeType) {
		this.setSelectionLineStrokeType(Strings.isEmpty(_strokeType) ? LineStyle.LINE_STROKES.LINE_DASH
				: LineStyle.LINE_STROKES.valueOf(_strokeType));
	}

	public void setSelectionLineStrokeType(LineStyle.LINE_STROKES _strokeType) {
		this.selectLineStrokeType = _strokeType;
		this.selectLineStroke = LineStyle.getStroke(_strokeType, this.getLineWidth());
	}

	public LineStyle.LINE_STROKES getSelectionLineStrokeType() {
		return this.selectLineStrokeType;
	}

	public Stroke getSelectionLineStroke() {
		if (this.selectLineStroke == null)
			this.setSelectionLineStrokeType(this.selectLineStrokeType);
		return this.selectLineStroke;
	}

	public void setSelectionLineStroke(String _strokeType) {
		try {
			this.setSelectionLineStroke(Strings.isEmpty(_strokeType) ? LineStyle.LINE_STROKES.LINE_SOLID
					: LineStyle.LINE_STROKES.valueOf(_strokeType.toUpperCase()));
		} catch (Exception e) {
			this.setSelectionLineStroke(LineStyle.LINE_STROKES.LINE_SOLID);
		}
	}

	public void setSelectionLineStroke(LineStyle.LINE_STROKES _strokeType) {
		this.selectLineStrokeType = _strokeType;
		this.selectLineStroke = LineStyle.getStroke(_strokeType, this.getLineWidth());
	}

	public void setSelectionColor(Color _selectionColor) {
		this.selectionColor = _selectionColor == null
				              ? Color.YELLOW
                              : new Color(_selectionColor.getRGB());
		this.selectionColor = Colours.setAlpha(this.selectionColor,
				                               this.selectionShadeTransLevel);
	}

	public int getSelectionShadeTransLevelAs255() {
		return Math.abs(this.selectionShadeTransLevel) > 1.0
				? 255
                : (int)(Math.abs(this.selectionShadeTransLevel) * 255.0);
	}

	public Color getSelectionColor() {
		return new Color(this.selectionColor.getRed(),
				         this.selectionColor.getGreen(),
				         this.selectionColor.getBlue(),
				         this.getSelectionShadeTransLevelAs255()
				        );
	}

	public void setSelectionShadeTransLevel(float _selectShadeTransLevel) {
		this.selectionShadeTransLevel = _selectShadeTransLevel;
	}

	public int getSelectionShadeTransLevelAsPercent() {
		return Math.round(this.getSelectionShadeTransLevel() * 100.0f);
	}

	public float getSelectionShadeTransLevel() {
		return this.selectionShadeTransLevel;
	}

	public AlphaComposite getSelectionAlphaComposite() {
		return Styling.getAlphaComposite(this.selectionShadeTransLevel);
	}

}
