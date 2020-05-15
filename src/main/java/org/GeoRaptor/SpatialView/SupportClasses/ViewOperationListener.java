package org.GeoRaptor.SpatialView.SupportClasses;

import org.GeoRaptor.SpatialView.SpatialViewPanel;

/**
 * In this class we save last user operation.
 */
public class ViewOperationListener 
{
    public enum VIEW_OPERATION { NONE, 
                                 VIEW_DELETE, VIEW_SWITCH,
                                 QUERY, 
                                 MBR_PREV, MBR_NEXT, 
                                 ZOOM_IN, ZOOM_OUT, MOVE,
                                 ZOOM_BOUNDS, ZOOM_FULL, ZOOM_LAYER, 
                                 DATA_RELOAD, IMAGE_COPY,
                                 MEASURE_DISTANCE, MEASURE_POLYGON, MEASURE_RECTANGLE, MEASURE_CIRCLE,
                                 CREATE_POINT, CREATE_MULTIPOINT, CREATE_LINE, CREATE_RECTANGLE, CREATE_CIRCLE, CREATE_POLYGON,
                                 QUERY_POINT, QUERY_MULTIPOINT, QUERY_LINE, QUERY_RECTANGLE, QUERY_CIRCLE, QUERY_POLYGON};

    protected VIEW_OPERATION   spatialViewOpr = VIEW_OPERATION.ZOOM_IN;
    protected SpatialViewPanel svPanel;
    
    public ViewOperationListener(SpatialViewPanel _svPanel) {
        this.svPanel = _svPanel;
    }

    /**
    * Set operation and then
    * call function in SpatialViewPanel class to set button state
    **/
    public void setSpatialViewOpr(VIEW_OPERATION _spatialViewOpr) {
        this.svPanel.setMessage(null,false);
        this.spatialViewOpr = _spatialViewOpr;
System.out.println(this.svPanel==null?"svPanel is null":(this.svPanel.getActiveView()==null?"getActiveView is null":this.svPanel.getActiveView().getViewName()));
        this.svPanel.setToolbarStatus(_spatialViewOpr,
                                      this.svPanel.getActiveView().getWindowNavigator(),
                                      this.svPanel.getActiveView().getMBR());
    }

    public VIEW_OPERATION getSpatialViewOpr() {
        return this.spatialViewOpr;
    }
    
}
