package org.GeoRaptor.SpatialView.JDevInt;

import java.awt.Component;

import javax.swing.ImageIcon;

import oracle.ide.docking.DockableWindow;
import oracle.ide.layout.ViewId;

import org.GeoRaptor.SpatialView.SpatialViewPanel;

public class DockableSV extends DockableWindow {
    
    /**
     * Must be the same as class name
     */
    public static final String VIEW_NAME = "DockableSV";

    /**
     * The name of title bar and tab
     */
    public final String DockableSV_TITLE_NAME = "  Spatial View";

    public static final ViewId VIEW_ID = new ViewId(DockableSVFactory.FAMILY,VIEW_NAME);

    private static SpatialViewPanel spatialViewPanel;

    public DockableSV() {
        super(VIEW_ID.getId());
    }

    public Component getGUI() {
        return getSpatialViewPanel();
    }

    public static SpatialViewPanel getSpatialViewPanel() {
        if (DockableSV.spatialViewPanel == null) {
            DockableSV.spatialViewPanel = new SpatialViewPanel();
        }
        return DockableSV.spatialViewPanel;
    }

    public String getTitleName() {
        return DockableSV_TITLE_NAME;
    }

    public String getTabName() {
        return DockableSV_TITLE_NAME;
    }

    public javax.swing.Icon getTabIcon() {
        ClassLoader cl = this.getClass().getClassLoader();
        ImageIcon iIcon = new ImageIcon(cl.getResource("org/GeoRaptor/images/main_icon_2_18x18.png"));
        return iIcon;
    }
}
