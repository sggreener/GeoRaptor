package org.GeoRaptor.OracleSpatial.ValidateSDOGeometry;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.JDevInt.SpatialRenderer;

import oracle.sql.STRUCT;

@SuppressWarnings("deprecation")
public class TableRow {

    protected SpatialRenderer              sRenderer = null;
    protected Preferences            mainPreferences = null;  
    protected String                              id = null, 
                                               error = null,
                                         checkResult = null;
    protected STRUCT                        geometry = null;
    protected String                     geoRenderer = null;  // Selected SDO_GEOMETRY column in (possibly coloured) display string form.
    protected boolean              currentIsColoured = false;
    protected Constants.renderType currentRenderType = null;
    
    public TableRow() {
    }
    
    public TableRow(String _id, 
                    String _error,
                    String _checkResult, 
                    STRUCT _geometry) 
    {
        this.id                = _id;
        this.error             = _error;
        this.checkResult       = _checkResult;
        this.geometry          = _geometry;
        this.sRenderer         = SpatialRenderer.getInstance();
        this.mainPreferences   = MainSettings.getInstance().getPreferences();
        this.currentRenderType = this.mainPreferences.getVisualFormat();
        this.currentIsColoured = this.mainPreferences.isColourSdoGeomElements();
        if ( _geometry != null ) {
            // Create as row is created
            this.geoRenderer = this.sRenderer.renderSdoGeometry(this.geometry,
                                                                this.mainPreferences.isColourSdoGeomElements());
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setError(String error) {
        this.error = error;
    }
  
    public String getError() {
        return this.error;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public String getCheckResult() {
        return this.checkResult;
    }

    public void setGeometry(STRUCT geometry) {
        this.geometry = geometry;
    }

    public STRUCT getGeometry() {
        return this.geometry;
    }

    public String getGeoConstructor() {
        try {
            if ( this.geometry != null )
                return RenderTool.renderSTRUCTAsPlainText(
                                        this.geometry,
                                        Constants.bracketType.NONE, 
                                        Constants.MAX_PRECISION
                       );
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
          this.geoRenderer = this.sRenderer.renderSdoGeometry(this.geometry,
                                                              this.mainPreferences.isColourSdoGeomElements());
        } 
        this.currentRenderType = this.mainPreferences.getVisualFormat();
        this.currentIsColoured = this.mainPreferences.isColourSdoGeomElements();
        return this.geoRenderer;
    }
    
}
