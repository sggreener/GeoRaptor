package org.GeoRaptor.SpatialView.JDevInt;

import oracle.ide.IdeConstants;
import oracle.ide.docking.DockStation;
import oracle.ide.docking.Dockable;
import oracle.ide.docking.DockableFactory;
import oracle.ide.docking.DockingParam;
import oracle.ide.layout.ViewId;


public final class DockableSVFactory 
        implements DockableFactory 
{
    /**
     * Must be the same as class name
     */
    public static final String FAMILY = "DockableSV";

    private DockableSV _myDockable;

	@SuppressWarnings("deprecation")
	public DockableSVFactory() {
        final DockStation dockStation = DockStation.getDockStation();
        //dockStation.registerDockableFactory(DockableSVFactory.FAMILY, this);
    }

    /* This method will only be called the first time this factory is encountered in a layout.
   */

    public void install() {
        final DockStation dockStation = DockStation.getDockStation();
        DockingParam dp = new DockingParam();
        dp.setPosition(IdeConstants.SOUTH);
        dockStation.dock(getDockableSV(), dp);
    }

    /* A factory can be responsible for multiple dockables.  For example, there is only one debugger factory to control
   * the debugger windows.
   * The view ID will be MyDockableFactory.FAMILY + "." + MyDockable.VIEW_ID
   */

    public Dockable getDockable(ViewId viewId) {
        final Dockable dockable;

        if (DockableSV.VIEW_ID.equals(viewId)) {
            dockable = getDockableSV();
        } else {
            dockable = null;
        }

        return dockable;
    }

    public DockableSV getDockableSV() {
        if (_myDockable == null) {
            _myDockable = new DockableSV();
        }
        return _myDockable;
    }
}
