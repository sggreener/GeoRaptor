package org.GeoRaptor.SpatialView.SupportClasses;

import java.sql.SQLException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import oracle.jdbc.OracleConnection;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.JDevInt.SpatialRenderer;

/**
 * Result row for "queryByPos" function
 */
@SuppressWarnings("deprecation")
public class QueryRow {

    protected SpatialRenderer              sRenderer = null;
    protected Preferences            mainPreferences = null;
    protected Constants.renderType currentRenderType = null;
    protected boolean              currentIsColoured = false;
  
    /**
     * Database row ID
     */
    protected   String  rowID;
    /**
     * Map of ColumnName - ColumnValue pairs. It does not include selected SDO_GEOMETRY column.
     */     
    protected   LinkedHashMap<String, Object> attData;
    
    /**
     * Selected SDO_GEOMETRY column
     */
    protected    STRUCT    geoValue = null;
    protected JGeometry       jGeom = null;
    protected    String geoRenderer = null;  // Selected SDO_GEOMETRY column in (possibly coloured) display string form.

    public QueryRow(                       String _rowID, 
                    LinkedHashMap<String, Object> _attData, 
                                           STRUCT _geoValue) 
    {
        this.rowID           = _rowID;
        this.attData         = _attData;
        this.geoValue        = _geoValue;
        this.sRenderer       = SpatialRenderer.getInstance();
        this.mainPreferences = MainSettings.getInstance().getPreferences();
        this.setGeoValue(geoValue);
    }

    public QueryRow(String                        _rowID, 
                    LinkedHashMap<String, Object> _attData, 
                                        JGeometry _geoValue,
                                 OracleConnection _conn) 
    throws Exception 
    {
        this.rowID           = _rowID;
        this.attData         = _attData;
        this.jGeom           = _geoValue;
        this.sRenderer       = SpatialRenderer.getInstance();
        this.mainPreferences = MainSettings.getInstance().getPreferences();
        this.setJGeom(_geoValue,_conn);
    }

    public void setRowID(String rowID) {
        this.rowID = rowID;
    }

    public String getRowID() {
        return this.rowID;
    }

    public void setAttData(LinkedHashMap<String, Object> attData) {
        this.attData = attData;
    }

    public Map<String, Object> getAttData() {
        return this.attData;
    }

    public LinkedHashMap<String,Object> getAttrHashMap() {
        return this.attData==null
               ? null
               : new LinkedHashMap<String,Object>(this.attData);      
    }

    public void setJGeom(JGeometry _geoValue,
                         OracleConnection _conn) {
      this.jGeom = _geoValue;
      try {
          if ( this.jGeom != null && _conn != null ) {
              this.geoValue = (STRUCT) JGeometry.storeJS(_conn, _geoValue);
              if ( _geoValue != null ) {
                  // Cache as row is created
                  this.currentRenderType = this.mainPreferences.getVisualFormat();
                  this.currentIsColoured = this.mainPreferences.isColourSdoGeomElements();
                  this.geoRenderer = this.sRenderer.renderSdoGeometry(this.geoValue,this.currentIsColoured);
              }
          }
      } catch (Exception e) {
          // nothing
      }
    }
  
    public JGeometry getJGeom() {
        if ( this.jGeom != null )
            return this.jGeom;
        if ( this.geoValue == null )
            return null;
        try {
            this.jGeom = JGeometry.load(this.geoValue);
        } catch (SQLException e) {
            this.jGeom = null;
        }
        return this.jGeom;
    }
    
    public void setGeoValue(STRUCT _geoValue) {
        this.geoValue = _geoValue;
        try {
            if ( _geoValue != null ) {
                try {
                    if ( _geoValue != null ) {
                        this.jGeom = JGeometry.load(this.geoValue);
                    }
                } catch (Exception e) {
                    this.jGeom = null;
                }
                // Store visual format at point of creation
                this.currentRenderType = this.mainPreferences.getVisualFormat();
                this.currentIsColoured = this.mainPreferences.isColourSdoGeomElements();
                // Cache as row is created
                this.geoRenderer = this.sRenderer.renderSdoGeometry(this.geoValue,this.currentIsColoured);
            }
        } catch (Exception e) {
            // nothing
        }
    }

    public STRUCT getGeoValue() {
        return this.geoValue;
    }
    
    public String getGeoConstructor() {
      try {
          if ( this.geoValue != null )
              return RenderTool.renderSTRUCTAsPlainText(this.geoValue,Constants.bracketType.NONE, Constants.MAX_PRECISION);
      } catch (Exception e) {
          return null;
      }
      return null;
  }

  public String getGeoRender() {
      // For speed reasons we only recreate the cached value if user changes display format.          
      if ( this.geoRenderer == null || 
           this.currentIsColoured != this.mainPreferences.isColourSdoGeomElements() ||
           this.currentRenderType != this.mainPreferences.getVisualFormat() ) 
      {
          if ( this.geoValue == null && this.jGeom != null ) 
              this.setGeoValue(null);
          if ( this.geoValue != null ) {
              this.currentRenderType = this.mainPreferences.getVisualFormat();
              this.currentIsColoured = this.mainPreferences.isColourSdoGeomElements();
              this.geoRenderer = this.sRenderer.renderSdoGeometry(this.geoValue,this.currentIsColoured);

          }
      } 
      return this.geoRenderer;
  }
  
    /**
     * Calculate Hash value for table structure
     * @return
     */
    public int calcColumnsHash() {
        int retHash = -1;
        if ( this.attData!=null ) {
          StringBuffer columnsList = new StringBuffer(100);
          Iterator<String> it = this.attData.keySet().iterator();
          while (it.hasNext()) {
              columnsList.append(it.next());
          }
          retHash = columnsList.toString().hashCode();
        }
        return retHash;
    }
    
    /**
     * Return number of attribute columns (without geometry column) in the row
     */
    public int getColumnCount() {
        int retCount = 0;
        if ( this.attData != null ) {
          Iterator<String> it = this.attData.keySet().iterator();
          while (it.hasNext()) {
              it.next();
              retCount++;
          }
        }
        return retCount;
    }
    
    /**
     * Get value of row by column index
     */
    public Object getValueForColumnIndex(int _index) {
        Object value = null;
        if ( this.attData != null ) {
          int count = -1;
          String lastRetValue = null;
          Iterator<String> it = this.attData.keySet().iterator();
          while ((it.hasNext()) && (_index != count)) {
              lastRetValue = (String)it.next();
              count++;
          }
          value = this.attData.get(lastRetValue);
        }
        return value;
    }
    
    // We don't want this layer saved....
    public String toXML() {
      return null;
    }
    
}