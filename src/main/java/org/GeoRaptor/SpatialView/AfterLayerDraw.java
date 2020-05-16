package org.GeoRaptor.SpatialView;

import java.awt.Graphics2D;


/**
 * Mark class with after layer draw operations.
 */
public class AfterLayerDraw {
    /**
     * Remove class from array list after draw methods is call.
     */
    protected boolean removeAfterDraw;

    public AfterLayerDraw(boolean removeAfterDraw) {
        this.removeAfterDraw = removeAfterDraw;
    }

    public void draw(Graphics2D _g2d) {
    }

    public void setRemoveAfterDraw(boolean removeAfterDraw) {
        this.removeAfterDraw = removeAfterDraw;
    }

    public boolean getRemoveAfterDraw() {
        return this.removeAfterDraw;
    }
}
