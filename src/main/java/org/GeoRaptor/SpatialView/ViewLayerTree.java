package org.GeoRaptor.SpatialView;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import java.io.IOException;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import oracle.dbtools.raptor.controls.RaptorDnD;
import oracle.dbtools.raptor.navigator.db.DatabaseConnection;
import oracle.dbtools.raptor.utils.Connections;

import oracle.ide.util.dnd.TransferableTreeNode;

import oracle.ideimpl.explorer.dnd.MultiTransferable;

import oracle.jdbc.OracleConnection;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.SupportClasses.LineStyle;
import org.GeoRaptor.SpatialView.SupportClasses.PointMarker;
import org.GeoRaptor.SpatialView.SupportClasses.QueryRow;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.SpatialView.SupportClasses.ViewOperationListener;
import org.GeoRaptor.SpatialView.layers.SVGraphicLayer;
import org.GeoRaptor.SpatialView.layers.SVQueryLayer;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayer;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayerProps;
import org.GeoRaptor.SpatialView.layers.SVWorksheetLayer;
import org.GeoRaptor.tools.Colours;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;


/**
 * @author Simon Greener, September 13th 2010
 *          Created from original TreeActiveLayer
 **/
 @SuppressWarnings("deprecation")
public class ViewLayerTree 
      extends JTree
   implements DragSourceListener,
              DropTargetListener,
              DragGestureListener,
              TreeWillExpandListener
{
    private static final long serialVersionUID = 1L;
    
    protected static ViewLayerTree classInstance;
    
    private ClassLoader cl = this.getClass().getClassLoader();
    
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.ViewLayerTree");

    private Preferences mainPreferences = null;
    
    private SpatialViewPanel svp;
      
    private static final String propertiesFile = "org.GeoRaptor.SpatialView.resource.ViewLayerTree";
    protected PropertiesManager propertyManager = null;
    
    protected DefaultMutableTreeNode   rootNode = null;
    protected TreeSelectionModel selectionModel = null;
    
    private DragSource dragSource     = null;
    @SuppressWarnings("unused")
	private DropTarget dropTarget     = null;
    private TreeNode   dropTargetNode = null;
    private TreeNode   draggedNode    = null;
    private Font       fontValue      = null;

    protected ButtonGroup btnGroupViews;
  
    private static enum expansionTypes {ProcessingUserAction,
                                        RootAction,
                                        ViewActiveButton,
                                        TreeViewPlus};
    private expansionTypes expansionType = expansionTypes.TreeViewPlus;

    protected SVSpatialLayer                queryTargetLayer = null;
    protected Constants.SDO_OPERATORS currentSpatialOperator = Constants.SDO_OPERATORS.RELATE;
    
    // For use with deleting nodes ... I can't get the standard methods to work
    //
    protected LinkedHashMap<String,DefaultMutableTreeNode> selectedNodes = new LinkedHashMap<String,DefaultMutableTreeNode>();

    private final static int CTRL_LEFT_MOUSE = InputEvent.CTRL_MASK + InputEvent.BUTTON1_MASK;  

    Color selectionBorderColor, 
          selectionForeground, 
          selectionBackground,
          textForeground, 
          textBackground;

    private final String iconDirectory = "org/GeoRaptor/SpatialView/images/";
    
    private ImageIcon iconNoDrawableLayer  = new ImageIcon(cl.getResource(iconDirectory + "no_drawable_layer.png"));
    private ImageIcon iconViewRemoveAll    = new ImageIcon(cl.getResource(iconDirectory + "operation_progress_enable.png"));
    private ImageIcon iconViewDrawAllOn    = new ImageIcon(cl.getResource(iconDirectory + "view_menu_layers_on.png"));
    private ImageIcon iconViewDrawAllOff   = new ImageIcon(cl.getResource(iconDirectory + "view_menu_layers_off.png"));
      
    private ImageIcon iconSetLayerMBR      = new ImageIcon(cl.getResource(iconDirectory + "layer_menu_set_mbr.png"));
    private ImageIcon iconShowLayerMBR     = new ImageIcon(cl.getResource(iconDirectory + "layer_menu_show_mbr.png"));
    private ImageIcon iconShowLayerAttrs   = new ImageIcon(cl.getResource(iconDirectory + "layer_show_attributes.png"));
    private ImageIcon iconLayerZoom        = new ImageIcon(cl.getResource(iconDirectory + "layer_menu_zoom.png"));
    private ImageIcon iconSelectionActive  = new ImageIcon(cl.getResource(iconDirectory + "layer_selection_active.png"));
    private ImageIcon iconLayerRemoveAll   = new ImageIcon(cl.getResource(iconDirectory + "layer_menu_remove_all.png"));
    private ImageIcon iconLayerRemove      = new ImageIcon(cl.getResource(iconDirectory + "layer_menu_remove.png"));
    private ImageIcon iconLayerCopy        = new ImageIcon(cl.getResource(iconDirectory + "button_quick_view_2.png"));
    private ImageIcon iconNavigationTop    = new ImageIcon(cl.getResource(iconDirectory + "navigation_top.png"));
    private ImageIcon iconNavigationUp     = new ImageIcon(cl.getResource(iconDirectory + "navigation_up.png"));
    private ImageIcon iconNavigationDown   = new ImageIcon(cl.getResource(iconDirectory + "navigation_down.png"));
    private ImageIcon iconNavigationBottom = new ImageIcon(cl.getResource(iconDirectory + "navigation_bottom.png"));
    private ImageIcon iconLayerProperties  = new ImageIcon(cl.getResource(iconDirectory + "layer_menu_properties.png"));
    private ImageIcon iconQuery            = new ImageIcon(cl.getResource(iconDirectory + "layer_menu_query.png"));
    private ImageIcon iconQuestionMark     = new ImageIcon(cl.getResource(iconDirectory + "icon_question_mark.png"));
    private ImageIcon iconSaveFile         = new ImageIcon(cl.getResource(iconDirectory + "icon_save_file.gif"));
    private ImageIcon iconLoadFile         = new ImageIcon(cl.getResource(iconDirectory + "icon_load_file.gif"));

    // We have an icon per feature/topological operator eg line/sdo_inside
    //
    private LinkedHashMap<String, ImageIcon> iconsTopological  = new LinkedHashMap<String,ImageIcon>();
    private LinkedHashMap<String, ImageIcon> iconsGeometryType = new LinkedHashMap<String,ImageIcon>();
    String[] spatialObjects = { ViewOperationListener.VIEW_OPERATION.QUERY_POINT.toString().replace("QUERY_","").toLowerCase(),
                                ViewOperationListener.VIEW_OPERATION.QUERY_MULTIPOINT.toString().replace("QUERY_","").toLowerCase(),
                                ViewOperationListener.VIEW_OPERATION.QUERY_LINE.toString().replace("QUERY_","").toLowerCase(),
                                ViewOperationListener.VIEW_OPERATION.QUERY_RECTANGLE.toString().replace("QUERY_","").toLowerCase(),
                                ViewOperationListener.VIEW_OPERATION.QUERY_CIRCLE.toString().replace("QUERY_","").toLowerCase(),
                                ViewOperationListener.VIEW_OPERATION.QUERY_POLYGON.toString().replace("QUERY_","").toLowerCase() };
    
    @SuppressWarnings("unused")
	private String dialogError             = null;
    private String noDrawableLayer         = null;
    private JPanel messageNoDrawableLayer  = null;

    static DataFlavor localObjectFlavor;
    static {
        try {
            localObjectFlavor = 
                new DataFlavor (DataFlavor.javaJVMLocalObjectMimeType);
        } catch (ClassNotFoundException cnfe) { cnfe.printStackTrace(); }
    }
    static DataFlavor[] supportedFlavors = { localObjectFlavor, TransferableTreeNode.TREE_NODE_FLAVOR };
    
    public ViewLayerTree(SpatialViewPanel _sViewPanel) 
    {
        try {
            this.btnGroupViews = new ButtonGroup();
            
  	        this.svp = _sViewPanel;
	        
            ViewLayerTree.classInstance = this;
	        
            // Get localisation file
            //
            this.propertyManager = new PropertiesManager(ViewLayerTree.propertiesFile);

            this.mainPreferences = MainSettings.getInstance().getPreferences();
            
            this.fontValue = UIManager.getFont("Tree.font");
            
            this.selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
            this.selectionForeground  = UIManager.getColor("Tree.selectionForeground");
            this.selectionBackground  = UIManager.getColor("Tree.selectionBackground");
            this.textForeground       = UIManager.getColor("Tree.textForeground");
            this.textBackground       = UIManager.getColor("Tree.textBackground"); 
      
            this.noDrawableLayer      = this.propertyManager.getMsg("LAYER_MENU_ZOOM_TO_LAYER_CONFIRM");
            this.dialogError          = this.propertyManager.getMsg("ERROR_MESSAGE_DIALOG_TITLE");
            
            // Set up noDrawableLayer Menu items
            //
            this.messageNoDrawableLayer  = new JPanel();
            this.messageNoDrawableLayer.add(new JLabel(iconNoDrawableLayer));
            JTextArea ta = new JTextArea(noDrawableLayer,3,1); // {
            ta.setBorder(javax.swing.BorderFactory.createEmptyBorder());
            ta.setOpaque(false);  // Set see through
            this.messageNoDrawableLayer.add(ta);
    
            this.setRootVisible(true);
            this.setUI( new javax.swing.plaf.metal.MetalTreeUI() );
            this.putClientProperty("JTree.lineStyle", "Angled");
              
            this.rootNode = new DefaultMutableTreeNode(this.propertyManager.getMsg("ROOT_TEXT"));
            DefaultTreeModel mod = new DefaultTreeModel (this.rootNode);
            this.setModel(mod);
              
            selectionModel = getSelectionModel();
            // Make it capable of deleting many nodes at once
            selectionModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
              
            // add MouseListener to tree
            this.addMouseListener(AddMouseAdapter());
            // Add Key listener
            this.addKeyListener(getKeyAdapter());
            // And a tree will expand listener to ensure close open views before expansion of new one
            this.addTreeWillExpandListener(this);
            
            this.setCellRenderer(new ViewLayerTreeCellRenderer());
      
            dragSource = new DragSource();
            @SuppressWarnings("unused")
			DragGestureRecognizer dgr = 
                  dragSource.createDefaultDragGestureRecognizer (this, 
                                                                 DnDConstants.ACTION_MOVE,
                                                                 this);
            dropTarget = new DropTarget (this, this);

            // Load icons
            //
            loadTopologicalSearchIcons();
            loadGeometryTypeIcons();
              
      } catch (NullPointerException npe) {
          System.out.println("Caught Null Pointer Exception in ViewLayerTree() " + npe.getMessage());
      } catch (Exception e) {
          System.out.println("Caught Exception in ViewLayerTree() " + e.getMessage());
      }
    }

    /**
     * Get instance of MainSettings class
     */
    public static ViewLayerTree getInstance(SpatialViewPanel _sViewPanel) {
        if (ViewLayerTree.classInstance == null) {
            ViewLayerTree.classInstance = new ViewLayerTree(_sViewPanel);
        }
        return ViewLayerTree.classInstance;
    }
    
    @SuppressWarnings("unused")
	private ImageIcon getGeometryTypeIcon(Constants.GEOMETRY_TYPES _geometryType) 
    {
        try {
            String key = "geometry_type_" + _geometryType.toString().toLowerCase();
            final ImageIcon iIcon = iconsGeometryType.get(key);
            if ( iIcon == null )
                return iconQuestionMark;
            else
                return iIcon;
      } catch (Exception e) {
            return iconQuestionMark;
      }              
    }
    
    private void loadGeometryTypeIcons() 
    {
        int maxIconHeight = 15;
        String key = "";
        ImageIcon iIcon = null;
        iconsGeometryType = new LinkedHashMap<String,ImageIcon>();
        for ( int i=0; i < Constants.GEOMETRY_TYPES.values().length; i++) {
            key = "geometry_type_" + Constants.GEOMETRY_TYPES.values()[i].toString().toLowerCase();
            try {
                iIcon = new ImageIcon(cl.getResource(iconDirectory + key + ".png"),key);
                if ( iIcon != null ) {
                    iconsGeometryType.put(key,iIcon);
                    maxIconHeight = Math.max(maxIconHeight,iIcon.getIconHeight());
                }
            } catch (Exception e) {
                iconsGeometryType.put(key,iconQuestionMark);
            }
        }
        this.setRowHeight(maxIconHeight + 2);
    }
    
    private void loadTopologicalSearchIcons() 
    {
        String key = "";
        ImageIcon iIcon = null;
        for ( int i=0; i < spatialObjects.length; i++) {
            for (int j=0; j < Constants.SDO_OPERATORS.values().length; j++) {
                key = spatialObjects[i] + "_sdo_" + Constants.SDO_OPERATORS.values()[j].toString().toLowerCase();
                try {
                    iIcon = new ImageIcon(cl.getResource(iconDirectory + key + ".png"),key);
                    iconsTopological.put(key,iIcon);
                } catch (Exception e) {
                    iconsTopological.put(key,null);
                }
            }
        }
    }
    
    private ImageIcon getTopologicalSearchIcon(ViewOperationListener.VIEW_OPERATION _queryFeature,
                                               Constants.SDO_OPERATORS              _operator ) 
    {
      try {
          String key = _queryFeature.toString().toString().replace("QUERY_","").toLowerCase() + "_sdo_" + _operator.toString().toLowerCase();
          final ImageIcon iIcon = iconsTopological.get(key);
          if ( iIcon == null )
              return null;
          else
              return iIcon;
      } catch (Exception e) {
          return null;
      }      
    }
                                         
    private void setQueryTarget(SVSpatialLayer _sLayer) {
      this.queryTargetLayer = _sLayer;
    }
    
    public SVSpatialLayer getQueryTarget() {
      return this.queryTargetLayer;
    }
    
    public Constants.SDO_OPERATORS getSpatialOperator() {
        return this.currentSpatialOperator;      
    }

    //Required by TreeWillExpandListener interface.
     public void treeWillExpand(TreeExpansionEvent e) 
                 throws ExpandVetoException 
     {
        TreePath path = e.getPath();
        if (path == null)
            return; 
        TreeNode selectedNode = (TreeNode) path.getLastPathComponent();
        if ( selectedNode instanceof ViewNode ) 
        {
            switch (expansionType )
            {
                case ProcessingUserAction :
                    // This is the consequent action that we need to ignore, so ignore it and set flag back to default
                    expansionType = expansionTypes.TreeViewPlus; 
                    return;
                case RootAction :
                    return;
                case ViewActiveButton :
                	break;
                case TreeViewPlus :
                     // Set expansionType to indicate that the next expansion is being caused by the processing of a user action
                     // so that no processing of consequent expansion is ignored (ie we don't need to react to it)
                     //
                     expansionType = expansionTypes.ProcessingUserAction;
                     ((ViewNode)selectedNode).getSpatialView().getSVPanel().setActiveView(((ViewNode)selectedNode).getSpatialView());
            } 
            /* user wants all views/layers to be displayed regardless as to active status */ 
            // collapse all other view nodes
            ((ViewNode)selectedNode).collapse();
            // Make opened node active
            ((ViewNode)selectedNode).setActive(true);
            // Notify change
            DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
            model.nodeStructureChanged(selectedNode);
        }
     }
     
     public void treeWillCollapse(TreeExpansionEvent e) {};
  
     private MouseAdapter AddMouseAdapter() 
     {    
         return new MouseAdapter()
         { 
            DefaultTreeModel model = ((DefaultTreeModel)getModel());
            
            private void processPopupEvent(final MouseEvent e) 
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() 
                    {
                        int x = e.getX();
                        int y = e.getY();
                        TreePath path = getPathForLocation(x, y);
                        if (path == null) return; 
                        TreeNode clickedNode = (TreeNode) path.getLastPathComponent();
                        JPopupMenu popup = null;
                        if ( clickedNode.getParent() == null )
                            popup = getRootMenu();
                        else if (clickedNode instanceof LayerNode) {
                            LayerNode lNode = (LayerNode)clickedNode;
                            popup = getLayerMenu((ViewNode)lNode.getParent(),lNode);
                        } else if (clickedNode instanceof ViewNode) {
                            popup = getViewMenu((ViewNode)clickedNode);
                        }
                        popup.show((JTree)e.getSource(), x, y);
                    }
                 });
            }

            /**
             * Finds component (JCheckBox/JRadioButton/Icon/JLabel) in JTree's node's JPanel
             */
            private Object processClickedComponent(LayerNode checkedNode,
                                                   int _x,
                                                   int _xNodeOffset) 
            {
                int actualCursorPosition = _x - _xNodeOffset;
                int incrementalPosition = 0;
                Component[] components = checkedNode.getLeafRenderer().getComponents();
                if ( checkedNode.getLeafRenderer().getLayout() instanceof FlowLayout ) { 
                  actualCursorPosition -= ((FlowLayout)checkedNode.getLeafRenderer().getLayout()).getHgap();                  
                }
                for (int i=0; i<components.length; i++) 
                {
                   if ( incrementalPosition  <= actualCursorPosition &&
                        actualCursorPosition <= ( incrementalPosition+components[i].getWidth()) ) {
                        return components[i];
                   }
                   incrementalPosition += components[i].getWidth();
                }
                return null;
            }

            public void processMultiSelection(MouseEvent e) 
            {
                int x = e.getX();
                int y = e.getY();
                int row = getRowForLocation(x, y);
                TreePath path = getPathForRow(row);
                if (path != null) 
                {
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)path.getLastPathComponent();
                    if ( selectedNode instanceof LayerNode ) {
                      if ( ! selectedNodes.containsKey(((LayerNode)selectedNode).getSpatialLayer().getLayerName()) )
                         selectedNodes.put(((LayerNode)selectedNode).getSpatialLayer().getLayerName(),selectedNode); 
                    }
                }
            }
            
            // Process left mouse clicks
            //
            public void processLeftMouseEvent(MouseEvent e)  
            {
                int x = e.getX();
                int y = e.getY();
                int row = getRowForLocation(x, y);
                TreePath path = getPathForRow(row);
                if (path != null)
                {                                           
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                    if (node instanceof LayerNode) 
                    {
                        // Get absolute left location of the component in the selected row. 
                        // Needed to discover which element in JPanel clicked.
                        LayerNode checkedNode = (LayerNode)node;
                        if ( getRowBounds(row) == null ) {
                            // refresh anyway
                            model.nodeStructureChanged(checkedNode.getParent());
                            return;
                        }
                        int xOffset = (int)getRowBounds(row).getX();
                        Object nodeObject = processClickedComponent(checkedNode,x,xOffset);
                        if (nodeObject instanceof JCheckBox) {
                            // Check Drawable
                            //
                            checkedNode.setDraw(!checkedNode.isDraw());
                            // Only a single node requires refreshing...
                            //
                            model.nodeChanged(checkedNode);
                        } else if (nodeObject instanceof JRadioButton  ) {
                            // Only a single node requires refreshing...
                            //
                            checkedNode.setLayerActive();
                        } else {
                            // Toggle? or change selected node
                            //
                            if ( selectedNodes.size() == 1 ) 
                            {
                                if ( selectedNodes.containsKey(checkedNode.getSpatialLayer().getLayerName()) ) 
                                {
                                    // We are toggling this node
                                    selectedNodes.clear();
                                    getSelectionModel().clearSelection();
                                    model.nodeChanged(checkedNode);
                                } else {
                                    // The selection has changed
                                    selectedNodes.clear();
                                    selectedNodes.put(checkedNode.getSpatialLayer().getLayerName(),node);
                                }
                            } else {
                                selectedNodes.clear();
                                selectedNodes.put(checkedNode.getSpatialLayer().getLayerName(),node);
                            }
                        }
                    }  else if (node instanceof ViewNode) {
                        // Need to make view's layers visible and close others
                        ViewNode vNode = (ViewNode)node;
                        expansionType = expansionTypes.ViewActiveButton;
                        // All views may be open because user requested it via root menu
                        collapseAllViews();
                        // Now set the node as active as it will force expansion
                        vNode.getSpatialView().getSVPanel().setActiveView(vNode.getSpatialView());
                        expansionType = expansionTypes.TreeViewPlus;
                    } 
                } // if
            } // public mouseClicked
                   
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

           /**
            * Display right mouse click menu when release right mouse button
            */
           public void mouseReleased(MouseEvent e) 
           {
                if ((e.getModifiers() & CTRL_LEFT_MOUSE) == CTRL_LEFT_MOUSE) {
                      processMultiSelection(e);
                } else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                      processLeftMouseEvent(e);
                } else
                      maybeShowPopup(e);
           }

           private void maybeShowPopup(MouseEvent e) {
               if (e.isPopupTrigger()) {
                 if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) 
                     processPopupEvent(e);
               }
           }
           
      };
    }

    public void refreshNode(String _nodeName) {
        // find layer by name
        DefaultMutableTreeNode Node = getNodeByName(_nodeName);
        if ( Node instanceof LayerNode ||
             Node instanceof ViewNode ) 
        {
            DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
            model.nodeChanged(Node);
        }
    }

    public void collapseView(String _viewName) {
        // find View
        Object node = getNodeByName(_viewName);
        if ( node != null ) {
            if ( node instanceof ViewNode ) {
                ViewNode vNode = (ViewNode)node; 
                vNode.collapse();
            }
        }
    }
  
    public void collapseAllViews() 
    {
        if ( this.rootNode != null ) 
        {    	
            Enumeration<?> e = this.rootNode.children();
            if (e != null ) {
                DefaultMutableTreeNode currentNode = null;
                while (e.hasMoreElements()) 
                {
                  currentNode = (DefaultMutableTreeNode)e.nextElement();
                  if ( currentNode != null && currentNode instanceof ViewNode ) 
                  {
                      this.collapsePath(new TreePath(currentNode.getPath()));
                  }
                }
            }
        }
    }

    public void expandView(String _viewName) {
        // find View
        Object node = getNodeByName(_viewName);
        if ( node != null ) {
            if ( node instanceof ViewNode ) {
                ViewNode vNode = (ViewNode)node; 
                vNode.expand();
            }
        }
    }

    public void expandAll() {
      // Set flag
      this.expansionType = expansionTypes.RootAction;
      for (int row = 0;  row < this.getRowCount(); row++) {
          this.expandRow(row);
      }
      this.expansionType = expansionTypes.TreeViewPlus;
    }

    public void removeAllViews() {
        DefaultMutableTreeNode currentNode = null;
        Enumeration<?> e = this.rootNode.children();
        while (e.hasMoreElements()) {
          currentNode = (DefaultMutableTreeNode) e.nextElement();
          if ( currentNode instanceof ViewNode ) {
              this.removeView((ViewNode)currentNode,true);
          }
        }
    }
    
    public void expandAllViews() {
        DefaultMutableTreeNode currentNode = null;
        Enumeration<?> e = this.rootNode.children();
        while (e.hasMoreElements()) {
          currentNode = (DefaultMutableTreeNode) e.nextElement();
          if ( currentNode instanceof ViewNode ) {
              this.expandPath(new TreePath(currentNode.getPath()));
          }
        }
    }
  
    public ViewNode addView(SpatialView _view,
                            boolean     _isActive) 
    {
        // Make sure view with this name doesn't already exist
        Object vNode = getNodeByName(_view.getViewName());
        if ( vNode != null ) {
            if ( vNode instanceof ViewNode ) {
                return (ViewNode)vNode;
            }
        }
        ViewNode newViewNode = new ViewNode(_view,_isActive,this.fontValue);
        // Add alphabetically 
        int insertPosition = findOrderedInsertPosition(_view.getViewName(),true/*descending*/);
        DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
        model.insertNodeInto(newViewNode,this.rootNode,insertPosition);
        return newViewNode;
    }

    // Cannot remove active view
    //
    public boolean removeView(String  _viewName,
                              boolean _removeViewNode) 
    {
        // find View
        Object vNode = getNodeByName(_viewName);
        if ( vNode != null ) {
            if ( vNode instanceof ViewNode ) {
                this.removeView((ViewNode)vNode,_removeViewNode);
            }
        }
        return false;
    }

    public boolean removeView(ViewNode _vNode,
                              boolean  _removeViewNode)
    {
        if ( _vNode.getSpatialView().isActive() && _removeViewNode ) {
            _vNode.getSpatialView().getSVPanel().setMessage(this.propertyManager.getMsg("STATUS_ACTIVE_VIEW_REMOVAL",_vNode.getNodeName()),true/*ring Bell*/);
            return false;
      }
      
      // Do removal
      //
      DefaultTreeModel model = ((DefaultTreeModel) this.getModel());

      if ( _vNode.getChildCount()!=0 ) 
      {
          // Remove all layer nodes
          //
          MutableTreeNode lNode = null;
          int i = _vNode.getChildCount();
          while ( i > 0 ) {
              i--;
              lNode = (MutableTreeNode)_vNode.getChildAt(i);
              model.removeNodeFromParent(lNode);
              i = _vNode.getChildCount();
          }
      }
      
      if ( _removeViewNode ) 
      {
           // Delete the SpatialView's MapPanel 
           // 
           _vNode.getSpatialView().removeMapPanel();
           // Remove all view references to actual view, button groups etc.
           //
          _vNode.removeViewReferences();
          // Remove node from tree
          //
          model.removeNodeFromParent(_vNode);
          // Now destroy node
          //
          _vNode=null;
       } else {
          // No more layers and View still exists so reset MBR, status bar etc
          //
          this.svp.reInitialisePanel();
       }
       return true;
    }
    
    public int getViewCount() {
      int viewCount = 0;
      if ( this.rootNode.getChildCount() != 0 ) {
          DefaultMutableTreeNode node = null;
          Enumeration<?> e = this.rootNode.breadthFirstEnumeration();
          while (e.hasMoreElements()) {
              node = (DefaultMutableTreeNode)e.nextElement();
              if ( node instanceof ViewNode ) {
                  viewCount++;
              }
          }
      }
      return viewCount;
    }
    
    public SpatialView getView(String _viewName) {
        if (Strings.isEmpty(_viewName)) {
            return null;
        }
        Object node = getNodeByName(_viewName);
        if ( node != null ) {
          if ( node instanceof ViewNode )
              return ((ViewNode)node).getSpatialView();
        }
        return null; 
    }

    public ViewNode getViewNode(String _viewName) {
      DefaultMutableTreeNode node = getNodeByName(_viewName);
      if ( node != null ) {
        if ( node instanceof ViewNode )
            return (ViewNode)node;
      }
      return null; 
    }
    
    public int getViewPosition(String _viewName) {
      DefaultMutableTreeNode node = getNodeByName(_viewName);
      if ( node != null ) {
        if ( node instanceof ViewNode )
            return this.rootNode.getIndex(node);
      }
      return -1; 
    }
    
    public SpatialView getActiveView() {
      if ( this.rootNode.getChildCount() != 0 ) {
          DefaultMutableTreeNode node = null;
          Enumeration<?> e = this.rootNode.breadthFirstEnumeration();
          while (e.hasMoreElements()) {
              node = (DefaultMutableTreeNode)e.nextElement();
              if ( node instanceof ViewNode ) {
                  if ( ((ViewNode)node).isActive() ) {
                      return ((ViewNode)node).getSpatialView();
                  }
              }
          }
      }
      return null;
    }

    public SVSpatialLayer getActiveLayer(String _viewName) {
      // find view object by name
      DefaultMutableTreeNode vNode = getNodeByName(_viewName);
      if ( vNode instanceof ViewNode ) {
          LayerNode activeLayer = ((ViewNode)vNode).getActiveLayerNode();
          return activeLayer==null ? null : activeLayer.getSpatialLayer();
      }
      return null;
    }
    
    public void setLayerActive(String _layerName) {
        // find layer by name
        DefaultMutableTreeNode node = getNodeByName(_layerName);
        if ( node instanceof LayerNode ) {
            ((ViewNode)node.getParent()).setLayerActive((LayerNode)node);
        }
    }

    /**
     * refreshLayerText - To show feature count.
     * @param _layer
     * @param _numFeats
     */
    public void refreshLayerCount(SVSpatialLayer _layer,
                                  long           _numFeats) 
    {
        try {
LOGGER.debug("refreshLayerCount("+_layer.getLayerName()+" " + _numFeats);
            // find layer by name
            DefaultMutableTreeNode node = getNodeByName(_layer.getLayerName());
LOGGER.debug("node is "+(node==null?"null":"not null"));
            if ( node instanceof LayerNode ) {
                LayerNode lNode = (LayerNode)node;
LOGGER.debug("setting lNode to "+String.format("(%d) %s",_numFeats,_layer.getVisibleName()));
                lNode.setText(String.format("(%d) %s",_numFeats,_layer.getVisibleName()));
                DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
                if (model!=null) {
                    model.nodeChanged(node);
                }
            }
        } catch (NullPointerException npe) {
        } catch (ArrayIndexOutOfBoundsException obe) {
        }
LOGGER.debug("End refreshLayerCount " + _layer.getLayerName());
    }

    public void removeLayerCount(SVSpatialLayer _layer) {
LOGGER.debug("start removeLayerCount " + _layer.getLayerName());
        // find layer by name
        DefaultMutableTreeNode node = getNodeByName(_layer.getLayerName());
        if ( node instanceof LayerNode ) {
            LayerNode lNode = (LayerNode)node;
LOGGER.debug("Old text = " + lNode.getText());
            lNode.setText(_layer.getVisibleName());
LOGGER.debug("New text = " + lNode.getText());
            DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
            if (model!=null) {
                model.nodeChanged(node);
            }
        }
LOGGER.debug("End removeLayerCount " + _layer.getLayerName());
    }

    public SpatialView getViewAt(int _viewNumber) {
      int viewCount = 0;
      if ( this.rootNode.getChildCount() != 0 ) {
          DefaultMutableTreeNode node = null;
          Enumeration<?> e = this.rootNode.breadthFirstEnumeration();
          while (e.hasMoreElements()) {
            node = (DefaultMutableTreeNode)e.nextElement();
              if ( node instanceof ViewNode ) {
                  if ( viewCount == _viewNumber ) {
                      return ((ViewNode)node).getSpatialView();
                  }
                  viewCount++;
              }
          }
      }
      return null;
    }
    
    public void setViewActive(int _viewNumber) 
    {
        try {          
            int viewCount = 0;
            if ( this.rootNode.getChildCount() != 0 ) {
                DefaultMutableTreeNode node = null;
                Enumeration<?> e = this.rootNode.breadthFirstEnumeration();
                while (e.hasMoreElements()) 
                {
                    node = (DefaultMutableTreeNode)e.nextElement();
                    if ( node == null )
                        continue;
                    if ( node instanceof ViewNode ) {
                        ViewNode vNode = (ViewNode)node;
                        if ( viewCount == _viewNumber ) {
                            vNode.setActive(true);
                            // Notify change to this view node and all other view nodes
                            // that use the same radio button
                            DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
                            if ( model != null ) {
                                model.nodeChanged(node);
                                this.collapseAllViews();
                                vNode.expand();
                            }
                            return;
                        }
                        viewCount++;
                    }
                }
            }
        } catch (Exception e) {
          // Ignore
        }
    }
    
    
    public int getDrawableLayerCount(String _viewName) {
      // find view node by name
      Object vNode = getNodeByName(_viewName);
      if ( vNode instanceof ViewNode ) {
          return ((ViewNode)vNode).getDrawableLayerCount();
      }
      return -1;
    }

    public int getLayerCount(String _viewName) {
      // find view node by name
      Object vNode = getNodeByName(_viewName);
      if ( vNode instanceof ViewNode ) {
          return ((ViewNode)vNode).getChildCount();
      }
      return -1;
    }
  
    private int confirmLayerDelete(String _layerName) {
      return JOptionPane.showConfirmDialog(this,
                                           this.propertyManager.getMsg("CONFIRM",_layerName),
                                           this.propertyManager.getMsg("LAYER_MENU_REMOVE_LAYER",_layerName),
                                           JOptionPane.YES_NO_OPTION);
    }
    
    private void setRemainingActive(ViewNode _vNode) 
    {
        if ( _vNode == null )
            return ;
        LayerNode lNode = _vNode.getActiveLayerNode();
        if ( lNode == null && _vNode.getChildCount() > 0 ) 
        {
            ((LayerNode)_vNode.getChildAt(0)).setActive(true);
            // Notify change to this node and all other layer nodes that use the same button group
            // by nominating the shared view node and all its children having been changed.
            //
            DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
            model.nodeChanged(_vNode.getChildAt(0));
      }
      // What if it is the last?
      // Then view's MBR, status bar etc need to be reset 
      //
      if ( this.getLayerCount(_vNode.getSpatialView().getViewName()) == 0 ) 
          this.svp.reInitialisePanel();
    }
        
    private KeyAdapter getKeyAdapter() 
    {
        return new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {    
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DELETE: bulkLayerDelete(); break;
                    //case KeyEvent.VK_DOWN  : processMoveLayerKeyPress(Constants.layerPositionType.DOWN); break;
                    //case KeyEvent.VK_UP    : processMoveLayerKeyPress(Constants.layerPositionType.UP); break;
                    //case KeyEvent.VK_HOME  : processMoveLayerKeyPress(Constants.layerPositionType.TOP); break;
                    //case KeyEvent.VK_END   : processMoveLayerKeyPress(Constants.layerPositionType.BOTTOM); break;
                }
            }
        };
    }

    private void bulkLayerDelete() 
    {
        DefaultTreeModel model = ((DefaultTreeModel)getModel());

        int selectedNodeCount = getSelectionModel().getSelectionCount();
        if ( selectedNodeCount == 0 )
            return;
        
        TreePath[] nodes = getSelectionModel().getSelectionPaths();
        // Ask to confirm deletion
        // Build message
        //
        String confirmName = ""; 
        DefaultMutableTreeNode nodeToDelete = null;
        if ( selectedNodeCount == 1 ) {
            nodeToDelete = (DefaultMutableTreeNode)nodes[0].getLastPathComponent();
            if ( nodeToDelete instanceof LayerNode )
                confirmName = ((LayerNode)nodeToDelete).getSpatialLayer().getVisibleName();
            else
                confirmName = selectedNodeCount + " layer?";
        } else {
            confirmName = propertyManager.getMsg("REMOVE_MANY_LAYERS",selectedNodeCount); 
        } 
        
        // Ask...
        //
        int selectOpt = confirmLayerDelete(confirmName);
        if (selectOpt == 0)
        {
            // Process
            //
            LinkedHashMap<String,ViewNode> vNodes = new LinkedHashMap<String, ViewNode>();
            for ( int i = 0; i < selectedNodeCount; i++ ) 
            {
                nodeToDelete = (DefaultMutableTreeNode)nodes[i].getLastPathComponent();
                if ( nodeToDelete instanceof LayerNode ) {
                    // Remove all internal variable that reference a layer/button groups etc
                    ((LayerNode)nodeToDelete).removeLayerReferences();
                    ViewNode vNode = (ViewNode)nodeToDelete.getParent();
                    model.removeNodeFromParent(nodeToDelete);
                    if ( i == 0 ) {
                      vNodes.put(vNode.getSpatialView().getViewName(),vNode);
                    } else if ( ! vNodes.containsKey(vNode.getSpatialView().getViewName()) ) {
                        vNodes.put(vNode.getSpatialView().getViewName(),vNode);
                    }
                }
            }
            // Now process any viewNodes to check that they have an active layer
            if ( ! vNodes.isEmpty() && vNodes.size() > 0 ) 
            {
                Iterator<String> iter = vNodes.keySet().iterator();
                while (iter.hasNext() ) {
                    ViewNode vNode = vNodes.get(iter.next());
                  setRemainingActive(vNode);
                }
            }
            getSelectionModel().clearSelection();
        }    
    }
    
    public void processMoveLayerKeyPress(Constants.layerPositionType _direction) 
    {
        int selectedNodeCount = getSelectionModel().getSelectionCount();
        if ( selectedNodeCount != 1 ) // We also do not handle situation of multiple selected layers
            return;
        // Get path to selected node
        //
        TreePath tPath = getSelectionModel().getSelectionPath();
        if (getSelectionModel().getSelectionPath()==null )
            return;
        // Selection must not be a View
        //
        MutableTreeNode nodeToMove = null;
        nodeToMove = (MutableTreeNode)tPath.getLastPathComponent();
        if ( nodeToMove instanceof ViewNode )
           return;
        // Now move the layer node        
        //
        final Constants.layerPositionType direction = _direction;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() 
            {
                moveSelectedNode(direction);
            }
        });
    }

    public boolean removeLayer(String _viewName,
                               String _layerName,
                               boolean _confirm) 
    {
LOGGER.debug("removeLayer: " + _layerName);
        // find View
        Object node = getNodeByName(_viewName);
        if ( node == null ) {
            JOptionPane.showMessageDialog(this,this.propertyManager.getMsg("VLT_VIEW_DOES_NOT_EXIST",_viewName));
        } else {
            if ( node instanceof ViewNode ) {
              ViewNode vNode = (ViewNode)node;
              // find Layer 
              node = getNodeByName(_layerName);
              if ( node instanceof LayerNode ) 
              {
                  LayerNode lNode = (LayerNode)node;
                  return this.removeLayer(vNode,lNode,_confirm);
              }
            }
        }
        return false;
    }
    
    public boolean removeLayer(ViewNode _viewNode,
                              LayerNode _layerNode,
                              boolean   _confirm) 
    {
       // Is this a child of the View 
       if ( _layerNode.getParent() == _viewNode ) {
           int selectOpt = 0;
           if ( _confirm ) {
               // confirm dialog
               selectOpt = confirmLayerDelete(Strings.isEmpty(_layerNode.getText())?_layerNode.getVisibleName():_layerNode.getText());
           }
           if (selectOpt == 0) {
               // Now remove the layer node (triggers a repaint)
               DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
               // Remove all internal variable that reference a layer/button groups etc
               _layerNode.removeLayerReferences();
               model.removeNodeFromParent(_layerNode);
               setRemainingActive(_viewNode);
            }
      }
      return false;
    }
    
    public static TreePath getPath(TreeNode _treeNode) {
        TreeNode treeNode = null;
        List<Object> nodes = new ArrayList<Object>();
        if (_treeNode != null) {
            nodes.add(_treeNode);
            treeNode = _treeNode.getParent();
            while (treeNode != null) {
                nodes.add(0, treeNode);
                treeNode = treeNode.getParent();
            }
        }
        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
     }
    
    private boolean moveSelectedNode(Constants.layerPositionType _direction) 
    {
        DefaultTreeModel model = ((DefaultTreeModel)getModel());

        // Get Selected Layer 
        //
        TreeNode selectedNode = null;
        selectedNode = (TreeNode)getSelectionModel().getSelectionPath().getLastPathComponent();
        if (selectedNode == null ) {
           JOptionPane.showMessageDialog(null, "No layer node selected", "Error", JOptionPane.ERROR_MESSAGE);
           return false;
        }
        
        // Get selected layer node's parent
        //
        MutableTreeNode parentNode = (MutableTreeNode)selectedNode.getParent();
        ViewNode parent = (ViewNode)selectedNode.getParent();  // ViewNode
        
        // Now get position of layer node in that parent
        //
        int oldPosition = parent.getIndex(selectedNode);
        int totalLayers = parent.getChildCount();
        int newPosition = 0;
        switch ( _direction ) {
            case TOP    : newPosition = 0;
                          break;
            case UP     : newPosition = (oldPosition>0 ? (oldPosition-1) : 0 );
                          break;
            case DOWN   : newPosition = (oldPosition<(totalLayers-1) ? (oldPosition+1) : (totalLayers-1) );
                          break;
            case BOTTOM : newPosition = (totalLayers - 1);
                          break;
        }

        // Did they try to go too far up or down (indicated by position remaining the same)?
        //
        if ( newPosition==oldPosition ) {
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
        model.removeNodeFromParent((MutableTreeNode)selectedNode);
        model.insertNodeInto((MutableTreeNode)selectedNode,parentNode,newPosition);
        
        // Select the newly added node 
        // 
        selectionModel.clearSelection();
        DefaultMutableTreeNode insertedNode = this.getNodeByName(((LayerNode)selectedNode).getNodeName()); // SGG
        if (insertedNode != null ) {
            TreePath path = ViewLayerTree.getPath(selectedNode); 
            getSelectionModel().setSelectionPath(path);
        }
        
        this.collapseAllViews();
        this.expandView(((ViewNode)parentNode).getNodeName());
            
        return false;
    }

    private void moveLayer(TreeNode                    _lNode,
                           final Constants.layerPositionType _direction) {
        if ( _lNode == null )
            return;
        if ( _lNode instanceof LayerNode ) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() 
                {
                    moveSelectedNode(_direction);
                }
            });
        }
    }
    
    /**
      * Check if node with this name already exists. If exist, add "_OccureNumber" after name.
      * @param _name layer name
      * @return new layer name (can be the some one as _name)
      */
    public String checkName(String _name) 
    {
        String newName = _name.trim();
LOGGER.debug("ViewLayerTree.checkName " + newName);
        int count = 1;
        while (true) {
            // find node
            Object node = getNodeByName(newName);
LOGGER.debug("ViewLayerTree.getNodeByName("+newName+") " + (node==null?"is unique":"already exists"));
            if ( node == null) {
                return newName;
            }
            newName = _name.trim() + "_" + count++;
        }
    }
  
    public int findOrderedInsertPosition(String  _viewName,
                                         boolean _ascending) 
    {
        int insertPosition = 0;
        DefaultMutableTreeNode node = null;
        String nodeName = "";
        Enumeration<?> e = this.rootNode.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
          node = (DefaultMutableTreeNode) e.nextElement();
            if ( node instanceof ViewNode ) { 
              nodeName = ((ViewNode)node).getNodeName();
              if (_viewName.compareToIgnoreCase(nodeName) > 0 && _ascending)
                  insertPosition++;
              else if (_viewName.compareToIgnoreCase(nodeName) < 0 && ! _ascending)
                  insertPosition++;
            }
        }
        return insertPosition;
    }
    
    public DefaultMutableTreeNode getNodeByName(String _nodeName) {
      DefaultMutableTreeNode node = null;
      String nodeName = "";
      Enumeration<?> e = this.rootNode.breadthFirstEnumeration();
      while (e.hasMoreElements()) {
        node = (DefaultMutableTreeNode) e.nextElement();
        if ( node instanceof LayerNode ) 
            nodeName = ((LayerNode)node).getNodeName();
        else if ( node instanceof ViewNode ) 
            nodeName = ((ViewNode)node).getNodeName();
        if (_nodeName.equals(nodeName) ) {
          return node;
        }
      }
      return null;
    }

    public LinkedHashMap<String,SpatialView> getViewsAsList() {
      LinkedHashMap<String,SpatialView> lhm = new LinkedHashMap<String,SpatialView>();
      DefaultMutableTreeNode node = null;
      ViewNode vNode = null;
      Enumeration<?> e = this.rootNode.breadthFirstEnumeration();
      while (e.hasMoreElements()) {
          node = (DefaultMutableTreeNode) e.nextElement();
          if ( node instanceof ViewNode ) {
              vNode = (ViewNode)node;
              lhm.put(vNode.getNodeName(),vNode.getSpatialView());
          }
      }
      return lhm;
    }

    public LinkedHashMap<String,SVSpatialLayer> getLayers(String _viewName) 
    {
        // find View
        DefaultMutableTreeNode vNode = getNodeByName(_viewName);
        if ( vNode == null ) {
            return null; // System.out.println(this.propertyManager.getMsg("VLT_VIEW_DOES_NOT_EXIST",_viewName));
        } else {
          if ( vNode instanceof ViewNode )
              return ((ViewNode)vNode).getLayers();
          return null;
        }
    }

    private JPopupMenu getRootMenu () {
        JPopupMenu popup = new JPopupMenu();
       
        AbstractAction collapseAllViews = 
            new AbstractAction(this.propertyManager.getMsg("ROOT_MENU_COLLAPSE_ALL_VIEWS"),iconLayerRemoveAll) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                  collapseAllViews(); 
                }
            };
        popup.add(collapseAllViews);

        AbstractAction expandAll = 
            new AbstractAction(this.propertyManager.getMsg("ROOT_MENU_EXPAND_ALL_VIEWS"),iconLayerRemoveAll) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    expandAll();
                }
            };
        popup.add(expandAll);

        AbstractAction removeAllViews = 
            new AbstractAction(this.propertyManager.getMsg("ROOT_MENU_REMOVE_ALL_VIEWS"),iconLayerRemoveAll) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                  // confirm dialog
                  int selectOpt = 
                    JOptionPane.showConfirmDialog(null, 
                                                  propertyManager.getMsg("CONFIRM",propertyManager.getMsg("REMOVE_VIEWS_ALL")), 
                                                  propertyManager.getMsg("TITLE_REMOVE_ALL_VIEWS"), 
                                                  JOptionPane.YES_NO_OPTION);
                  if (selectOpt == 0) {
                    removeAllViews(); 
                  }
                }
            };
        popup.add(removeAllViews);

        AbstractAction createNewView = 
            new AbstractAction(this.propertyManager.getMsg("ROOT_MENU_CREATE_NEW_VIEW"),iconLayerRemoveAll) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    SpatialView newView = new SpatialView(svp,"SRID:NULL","NULL");
                    SpatialViewProperties svProp = new SpatialViewProperties(null,true,newView);
                    boolean ok = svProp.initDialog(true);
                    if (ok) {
                        svProp.setVisible(true);
                        if ( ! svProp.wasCancelled() ) {
                            newView.setViewName(checkName(newView.getViewName()));
                            boolean activate = true;
                            svp.addView(newView,activate);
                        }
                    }
                }
            };
        popup.add(createNewView);
        
        String switchValue = mainPreferences.isNumberOfFeaturesVisible()
                           ? this.propertyManager.getMsg("VALUE_OFF") 
                           : this.propertyManager.getMsg("VALUE_ON");
        AbstractAction layerFeatureCount= 
            new AbstractAction(this.propertyManager.getMsg("ROOT_MENU_LAYER_FEATURE_COUNT",switchValue),iconLayerRemoveAll) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    mainPreferences.setNumberOfFeaturesVisible(!mainPreferences.isNumberOfFeaturesVisible());
                }
            };
        popup.add(layerFeatureCount);
        return popup;
    }
  
    private JPopupMenu getViewMenu (final ViewNode _viewNode) 
    {
        JPopupMenu popup = new JPopupMenu(_viewNode.getText());

        AbstractAction addStaticLayer = 
            new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_ADD_STATIC_LAYER"),iconLayerZoom) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    // Can bring context menu up on non-active view, so make it active!
                    SVWorksheetLayer sWorksheetLayer = null;
                    MetadataEntry                 me = null;
                    String                layerName = "New AdHoc Layer";
                    me = new MetadataEntry("",
                                           layerName,
                                           mainPreferences.getDefGeomColName(),
                                           _viewNode.getSpatialView().getSRID());
                    sWorksheetLayer = new SVWorksheetLayer(_viewNode.getSpatialView(),
                                                           layerName,
                                                           layerName,
                                                           layerName,
                                                           me,
                                                           true,
                                                           null,
                                                           false);
                    sWorksheetLayer.setConnection(_viewNode.getSpatialView().getConnectionName());
                    if ( mainPreferences.isRandomRendering()) {
                        sWorksheetLayer.getStyling().setAllRandom();
                    }
                    sWorksheetLayer.setMBR(_viewNode.getSpatialView().getMBR());
                    _viewNode.getSpatialView().addLayer(sWorksheetLayer,false,false,false /*zoom*/);
                    SVSpatialLayerProps slp = new SVSpatialLayerProps(null,"",false);
                    slp.initDialog(sWorksheetLayer);
                    // show dialog
                    slp.setVisible(true);
                }
            };
        popup.add(addStaticLayer);

        popup.addSeparator();
        
        AbstractAction copyWindowClipboard = 
            new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_MBR2CLIPBOARD"),iconShowLayerMBR) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    Envelope mbr = _viewNode.getSpatialView().getMBR();
                    Tools.doClipboardCopy(mbr.toSdoGeometry(_viewNode.getSpatialView().getSRID(),
                                                            _viewNode.getSpatialView().getPrecision(false)));
                }
            };
        popup.add(copyWindowClipboard);
        
        AbstractAction zoomAllDrawableLayers = 
            new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_ZOOM_ALL_LAYERS"),iconLayerZoom) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    // Can bring context menu up on non-active view, so make it active!
                    svp.setActiveView(_viewNode.getSpatialView());
                    // Reset MBR of layers in view
                    svp.zoomFullButton(null);
                    // Save new extent
                    svp.getMapPanel().windowNavigator.add(svp.getMapPanel().getWindow());
                }
            };
        popup.add(zoomAllDrawableLayers);
        
        popup.addSeparator();

        AbstractAction removeAllLayers = 
            new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_REMOVE_LAYERS_ALL"),iconLayerRemoveAll) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                  // confirm dialog
                  int selectOpt = 
                    JOptionPane.showConfirmDialog(null, 
                                                  propertyManager.getMsg("CONFIRM",propertyManager.getMsg("REMOVE_LAYERS_ALL",_viewNode.getSpatialView().getVisibleName())), 
                                                  propertyManager.getMsg("TITLE_REMOVE_ALL_LAYERS"), 
                                                  JOptionPane.YES_NO_OPTION);
                  if (selectOpt == 0) {
                    boolean removeViewNode = false;
                    removeView(_viewNode.getSpatialView().getViewName(),removeViewNode);
                  }
                }
            };
        popup.add(removeAllLayers);

        // Cannot remove active view and its layers
        if ( ! _viewNode.isActive() ) {
            AbstractAction removeViewAllLayers = 
              new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_REMOVE_VIEW_ALL",_viewNode.getVisibleName()),iconViewRemoveAll) {
                  /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    // confirm dialog
                    String defaultText = _viewNode.getSpatialView().getSRID().equals(svp.getDefaultSRID()) 
                                         ? propertyManager.getMsg("TREE_VIEW_IS_DEFAULT")
                                         : "";
                    int selectOpt = 
                      JOptionPane.showConfirmDialog(null, 
                                                    propertyManager.getMsg("CONFIRM",
                                                                           propertyManager.getMsg("REMOVE_VIEW_AND_ALL_LAYERS",
                                                                                                  _viewNode.getSpatialView().getVisibleName(),
                                                                                                  defaultText)), 
                                                    propertyManager.getMsg("TITLE_REMOVE_VIEW"),
                                                    JOptionPane.YES_NO_OPTION);
                    if (selectOpt == 0) {
                        boolean removeViewNode = true;
                        removeView(_viewNode.getSpatialView().getViewName(),removeViewNode);
                    }
                  }
              };
            popup.add(removeViewAllLayers);
        }

        popup.addSeparator();
        
        AbstractAction viewAllLayersOn = 
          new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_DRAW_ALL_LAYERS_ON"),iconViewDrawAllOn) {
              /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                  _viewNode.setAllLayersDrawable(true);
              }
          };
        popup.add(viewAllLayersOn);
        AbstractAction viewAllLayersOff = 
          new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_DRAW_ALL_LAYERS_OFF"),iconViewDrawAllOff) {
              /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                  _viewNode.setAllLayersDrawable(false);
              }
          };
        popup.add(viewAllLayersOff);

        popup.addSeparator();
        
        AbstractAction ViewLayersSave =
            new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_SAVE_VIEW_LAYERS"),iconSaveFile) {
          /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
              LinkedHashMap<String,SVSpatialLayer> selectedLayers = getSelectedLayers(_viewNode);
              if ( selectedLayers == null || selectedLayers.size() == 0) {
                  selectedLayers = _viewNode.getLayers();
              } 
              _viewNode.getSpatialView().saveToDisk(selectedLayers);
          }
        };
        popup.add(ViewLayersSave);

        AbstractAction ViewLayersLoad =
            new AbstractAction(this.propertyManager.getMsg("VIEW_MENU_LOAD_VIEW_LAYERS"),iconLoadFile) {
          /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
              svp.loadViewFromDisk(_viewNode);
          }
        };
        popup.add(ViewLayersLoad);
        
        popup.addSeparator();
            
        AbstractAction ViewProperties = 
          new AbstractAction(this.propertyManager.getMsg("TREE_PROPERTIES"),iconLayerProperties) {
              /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                  SpatialViewProperties svp = new SpatialViewProperties(null,true,_viewNode.getSpatialView());
                  svp.initDialog(false);
                  svp.setVisible(true);
                  DefaultTreeModel model = ((DefaultTreeModel)getModel());
                  // Force refresh of possibly change views's name 
                  model.nodeChanged(_viewNode);
              }
          };
        popup.add(ViewProperties);
        return popup;
    }

    private LinkedHashMap<String,SVSpatialLayer> getSelectedLayers(ViewNode _vNode) 
    {
        int selectedNodeCount = getSelectionModel().getSelectionCount();
        if ( selectedNodeCount == 0 || _vNode == null || _vNode.getChildCount()==0 ) {
            return null;
        }
        // Process
        //
        LinkedHashMap<String,SVSpatialLayer> lhm = new LinkedHashMap<String,SVSpatialLayer>(_vNode.getChildCount());
        DefaultMutableTreeNode nodeToSave = null;
        LayerNode lNode = null;
        TreePath[] nodes = getSelectionModel().getSelectionPaths();
        for ( int i = 0; i < selectedNodeCount; i++ ) 
        {
            nodeToSave = (DefaultMutableTreeNode)nodes[i].getLastPathComponent();
            if ( nodeToSave instanceof ViewNode ) {
                continue;
            }
            if ( nodeToSave instanceof LayerNode ) {
                lNode = (LayerNode)nodeToSave;
                // Is this a child of our view?
                if ( ((ViewNode)lNode.getParent()).getSpatialView().getViewID().equals(_vNode.getSpatialView().getViewID()) ) {
                    // Add to return list
                    lhm.put(lNode.getSpatialLayer().getLayerName(), lNode.getSpatialLayer());
                }
            }
        }
        return new LinkedHashMap<String,SVSpatialLayer>(lhm);
    }
    
    private JPopupMenu getLayerMenu (final ViewNode  _view,
                                     final LayerNode _layer) 
    {
        // Layer node is either the one passed in or the first one when only one is selected
        //
        boolean selectedNodeIsLayer = false;
        LayerNode lNode = null;
        TreeNode  tNode = null;
        if ( getSelectionModel().getSelectionCount() == 1 ) {
          tNode = (TreeNode)getSelectionModel().getSelectionPath().getLastPathComponent();
          if ( tNode instanceof LayerNode ) {
              lNode = (LayerNode)tNode;
              selectedNodeIsLayer = true;
          } else {
              selectedNodeIsLayer = false;
              lNode = _layer;
          }
        } else {
            if (getSelectionModel().getSelectionCount() != 0 ) {
                selectedNodeIsLayer = true; 
            }
            lNode = _layer;
        }
        final LayerNode fLayerNode = lNode;
        final TreeNode  fTreeNode  = tNode;

        // Where menu uses visible name of layer, shorten it where it is greater than 40 cars
        // SGG limit should be a preference
        String shortVisibleName = fLayerNode.getSpatialLayer().getVisibleName().length()>40
                                ? fLayerNode.getSpatialLayer().getVisibleName().substring(0, 39) + "...."
                                : fLayerNode.getSpatialLayer().getVisibleName();
            
        
        // Cannot bring context menu up for layers of a non-active view so we
        // can directly reference the passed in SpatialView to get the mapPanel()
        //
        final SpatialView sView = _view.getSpatialView();
        
        JPopupMenu popup = new JPopupMenu(fLayerNode.getText());
        
        String zoomToLayerMenu = ( selectedNodeIsLayer 
                                 ? this.propertyManager.getMsg("LAYER_MENU_ZOOM_TO_SELECTED_LAYERS")
                                 : this.propertyManager.getMsg("LAYER_MENU_ZOOM_TO_LAYER") );
        AbstractAction zoomToLayer = 
            new AbstractAction(zoomToLayerMenu,iconLayerZoom) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    Envelope mbr = new Envelope(sView.getDefaultPrecision());
                    if ( getSelectionModel().getSelectionCount() > 1 ) 
                    {
                        TreePath[] nodes = getSelectionModel().getSelectionPaths();
                        // Process
                        //
                        DefaultMutableTreeNode node = null;
                        for ( int i = 0; i < nodes.length; i++ ) 
                        {
                            node = (DefaultMutableTreeNode)nodes[i].getLastPathComponent();
                            if ( node instanceof LayerNode ) 
                            {
                                mbr.setMaxMBR(((LayerNode)node).getSpatialLayer().getMBR());
                            }
                        }
                    } else {
                        if ( fLayerNode.isDraw() ) {
                            mbr = fLayerNode.getSpatialLayer().getMBR();
                        } else {
                          // Use layer extent anyway?
                          int selectOpt = JOptionPane.showConfirmDialog(null,
                                                                        messageNoDrawableLayer,
                                                                        fLayerNode.getText(),
                                                                        JOptionPane.YES_NO_OPTION);
                          if (selectOpt == 0) {
                              // Reset MBR of layers in view
                              mbr = fLayerNode.getSpatialLayer().getMBR();
                          } else {
                              return;
                          }
                        }
                    }
                    // Zoom to new extent
                    if ( mbr.isSet() ) 
                    { 
                        LOGGER.debug("ViewLayerTree.zoomToLayer()");
                        svp.getVoListener().setSpatialViewOpr(ViewOperationListener.VIEW_OPERATION.ZOOM_BOUNDS);
                        sView.setMBR(mbr); // sets MapPanel window and saves MBR in windowNavigator
                        sView.getMapPanel().refreshAll();
                    }
                }
            };
        popup.add(zoomToLayer);

        String showLayerMBRMenu = ( selectedNodeIsLayer 
                                 ? this.propertyManager.getMsg("LAYER_MENU_SHOW_SELECTED_LAYERS_MBRS")
                                 : this.propertyManager.getMsg("LAYER_MENU_SHOW_LAYER_MBR") );
        AbstractAction showLayerMBR = 
            new AbstractAction(showLayerMBRMenu,iconShowLayerMBR) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) 
                {
                    // Get Layer MBR 
                    Envelope mbr = new Envelope(sView.getDefaultPrecision());
                    if ( getSelectionModel().getSelectionCount() > 1 ) {
                        TreePath[] nodes = getSelectionModel().getSelectionPaths();
                        // Process
                        //
                        DefaultMutableTreeNode node = null;
                        for ( int i = 0; i < nodes.length; i++ ) 
                        {
                            node = (DefaultMutableTreeNode)nodes[i].getLastPathComponent();
                            if ( node instanceof LayerNode ) 
                            {
                                mbr.setMaxMBR(((LayerNode)node).getSpatialLayer().getMBR());
                            }
                        }
                    } else {
                        // Get Layer MBR 
                        mbr = fLayerNode.getSpatialLayer().getMBR();
                    }
                    // Create JGeometry from MBR
                    JGeometry geom = new JGeometry(mbr.getMinX(), 
                                                   mbr.getMinY(), 
                                                   mbr.getMaxX(), 
                                                   mbr.getMaxY(), 
                                                   fLayerNode.getSpatialLayer().getSRIDAsInteger());
					STRUCT stGeom;
                    try {
                        stGeom = (STRUCT) JGeometry.storeJS(geom,fLayerNode.getSpatialLayer().getConnection());
                        svp.showGeometry(fLayerNode.getSpatialLayer(),stGeom, null, SDO_GEOMETRY.getGeoMBR(stGeom), true, true, false);
                        /* ??? */ sView.getMapPanel().windowNavigator.add(svp.getMapPanel().getWindow());
                    } catch (Exception f) {
                        JOptionPane.showMessageDialog(null, 
                                                      propertyManager.getMsg("LAYER_MENU_SHOW_LAYER_MBR_ERROR",
                                                                             f.getLocalizedMessage()));
                    }
                }
            };
        popup.add(showLayerMBR);

        String setLayerMBRMenu = ( selectedNodeIsLayer 
                                 ? this.propertyManager.getMsg("LAYER_MENU_SET_SELECTED_LAYERS_MBRS")
                                 : this.propertyManager.getMsg("LAYER_MENU_SET_LAYER_MBR") );
        AbstractAction setLayerMBR = 
            new AbstractAction(setLayerMBRMenu,iconSetLayerMBR) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) 
                {
                    // Get Map's MBR 
                    //
                    Envelope mbr = null;
                    mbr = svp.getMapPanel().getWindow();
                    
                    // Now apply to selected layers
                    if ( getSelectionModel().getSelectionCount() > 1 ) {
                        TreePath[] nodes = getSelectionModel().getSelectionPaths();
                        // Process
                        //
                        DefaultMutableTreeNode node = null;
                        for ( int i = 0; i < nodes.length; i++ ) 
                        {
                            node = (DefaultMutableTreeNode)nodes[i].getLastPathComponent();
                            if ( node instanceof LayerNode ) {
                                ((LayerNode)node).getSpatialLayer().setMBR(mbr);
                            }
                        }
                    } else {
                        // Get Layer MBR 
                        fLayerNode.getSpatialLayer().setMBR(mbr);
                    }
                }
            };
        popup.add(setLayerMBR);
LOGGER.debug("Testing class type of " + fLayerNode.getSpatalLayerType());
        String showLayerAttrMenuText = selectedNodeIsLayer
                                   ? ( selectionModel.getSelectionCount()==1 
                                       ? this.propertyManager.getMsg("LAYER_MENU_SHOW_SELECTED_LAYER_ATTRIBUTES",shortVisibleName)
                                       : this.propertyManager.getMsg("LAYER_MENU_SHOW_SELECTED_LAYERS_ATTRIBUTES")
                                     )
                                   : this.propertyManager.getMsg("LAYER_MENU_SHOW_LAYER_ATTRIBUTES",shortVisibleName);
LOGGER.debug("Menus entry is " + showLayerAttrMenuText);
        if ( ! showLayerAttrMenuText.equals(this.propertyManager.getMsg("LAYER_MENU_SHOW_SELECTED_LAYERS_ATTRIBUTES")) ) {
LOGGER.debug("Layer type is " + SVSpatialLayer.CLASS_NAME);
            if ( fLayerNode.getSpatalLayerType().equalsIgnoreCase(Constants.KEY_SVGraphicLayer)) 
            {
LOGGER.debug(fLayerNode.getSpatialLayer().getLayerName() + " is an SVGraphicLayer");
               String menuText = this.propertyManager.getMsg("LAYER_MENU_SHOW_LAYER_ATTRIBUTES");
               AbstractAction showLayerAttributes = new AbstractAction(menuText, iconShowLayerAttrs) {
                /**
				 * 
				 */
				private static final long serialVersionUID = -4232349263033974290L;

				public void actionPerformed(ActionEvent e) 
                {
                    List<QueryRow> aList = null;
                    if ( getSelectionModel().getSelectionCount() == 0 ) {
                        aList = ((SVGraphicLayer)fLayerNode.getSpatialLayer()).getAttributes();
                    } else if ( getSelectionModel().getSelectionCount() == 1 ) {
                        TreePath[] nodes = getSelectionModel().getSelectionPaths();
                        DefaultMutableTreeNode node = null;
                        node = (DefaultMutableTreeNode)nodes[0].getLastPathComponent();
                        aList = ((SVGraphicLayer)((LayerNode)node).getSpatialLayer()).getAttributes();
                    }
                    if ((aList == null) || (aList.size() == 0)) {
                        LOGGER.debug("No attributes to display");
                        Toolkit.getDefaultToolkit().beep();
                        // Clear display anyway
                        sView.getSVPanel().getAttDataView().clear();
                        return;
                    }
                    // show attrib data in bottom tabbed pane
                    //
                    sView.getSVPanel().getAttDataView().showData(aList);
                }
            };
               popup.add(showLayerAttributes);
            }
        }
        popup.addSeparator();

        String removeLayerMenuText = selectedNodeIsLayer
                                       ? ( selectionModel.getSelectionCount()==1 
                                           ? this.propertyManager.getMsg("LAYER_MENU_REMOVE_SELECTED_LAYER",shortVisibleName)
                                           : this.propertyManager.getMsg("LAYER_MENU_REMOVE_SELECTED_LAYERS",selectionModel.getSelectionCount())
                                         )
                                       : this.propertyManager.getMsg("LAYER_MENU_REMOVE_LAYER",shortVisibleName);
        AbstractAction removeLayer = new AbstractAction(removeLayerMenuText,iconLayerRemove) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                /*ToolTipManager.sharedInstance().mouseMoved(
                        new MouseEvent(tMenu, 0, 0, 0,
                                0, 0, // X-Y of the mouse for the tool tip
                                0, false));
                */
                if ( getSelectionModel().getSelectionCount() <= 1 ) {
                    removeLayer(_view,fLayerNode,true);
                } else {
                    bulkLayerDelete(); 
                }
            }
        };
        popup.add(removeLayer);
        
        if ( ! ( fLayerNode.getSpatialLayer() instanceof SVGraphicLayer ) )
        {
            AbstractAction createLayerCopy =
                new AbstractAction(this.propertyManager.getMsg("LAYER_MENU_CREATE_LAYER_COPY"),iconLayerCopy) {
              /**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                  _view.createLayerCopy(fLayerNode);
              }
            };
            popup.add(createLayerCopy);
        }
  
        popup.addSeparator();
        if ( ! ( fLayerNode.getSpatialLayer() instanceof SVGraphicLayer  ||
                 fLayerNode.getSpatialLayer() instanceof SVQueryLayer    ||
                 fLayerNode.getSpatialLayer() instanceof SVWorksheetLayer ) ) 
        {
            popup = addQueryMenuElements(popup, fLayerNode);
        }
        
        String switchValue = this.propertyManager.getMsg("VALUE_OFF") + "/" + this.propertyManager.getMsg("VALUE_ON");
        switchValue = "";
        if ( fLayerNode != null ) {
          switchValue = fLayerNode.getSpatialLayer().getStyling().isSelectionActive()
                           ? this.propertyManager.getMsg("VALUE_OFF") 
                           : this.propertyManager.getMsg("VALUE_ON");
        }
        AbstractAction toggleSelectionActive = 
          new AbstractAction(this.propertyManager.getMsg("LAYER_MENU_SWITCH_SELECTION_ACTIVE",switchValue),
                             iconSelectionActive) {
              /**
								 * 
								 */
								private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                // get active layer
                fLayerNode.getSpatialLayer().getStyling().setSelectionActive(!fLayerNode.getSpatialLayer().getStyling().isSelectionActive());
              }
          };
        popup.add(toggleSelectionActive);   
        
        // Common, non selection specific, entries
        //
        AbstractAction moveLayerTop = 
          new AbstractAction(this.propertyManager.getMsg("LAYER_MENU_MOVE_LAYER_TOP"),iconNavigationTop) {
              /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                  moveLayer(fTreeNode,Constants.layerPositionType.TOP);
              }
          };
        AbstractAction moveLayerUp = 
            new AbstractAction(this.propertyManager.getMsg("LAYER_MENU_MOVE_LAYER_UP"),iconNavigationUp) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    moveLayer(fTreeNode,Constants.layerPositionType.UP);
                }
            };
        AbstractAction moveLayerDown = 
            new AbstractAction(this.propertyManager.getMsg("LAYER_MENU_MOVE_LAYER_DOWN"),iconNavigationDown) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    moveLayer(fTreeNode,Constants.layerPositionType.DOWN);
                }
            };
        AbstractAction moveLayerBottom = 
            new AbstractAction(this.propertyManager.getMsg("LAYER_MENU_MOVE_LAYER_BOTTOM"),iconNavigationBottom) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    moveLayer(fTreeNode,Constants.layerPositionType.BOTTOM);
                }
            };
        
        popup.addSeparator();

        boolean usePopup = false;
        if ( usePopup ) {
          JMenu layerOrderMenu = new JMenu(this.propertyManager.getMsg("LAYER_MENU_MOVE_LAYER"));
          layerOrderMenu.add(moveLayerTop);
          layerOrderMenu.add(moveLayerUp);
          layerOrderMenu.add(moveLayerDown);
          layerOrderMenu.add(moveLayerBottom);
          popup.add(layerOrderMenu);
        } else {
          popup.add(moveLayerTop);
          popup.add(moveLayerUp);
          popup.add(moveLayerDown);
          popup.add(moveLayerBottom);
        }
        if ( ! ( fLayerNode.getSpatialLayer() instanceof SVGraphicLayer ) )
        {
            popup.addSeparator();
            String layerSaveMenu = selectedNodeIsLayer
                                           ? ( selectionModel.getSelectionCount()==1 
                                               ? this.propertyManager.getMsg("LAYER_MENU_SAVE_ONE_SELECTED_LAYER",shortVisibleName)
                                               : this.propertyManager.getMsg("LAYER_MENU_SAVE_SELECTED_LAYERS",selectionModel.getSelectionCount())
                                             )
                                           : this.propertyManager.getMsg("LAYER_MENU_SAVE_LAYER",shortVisibleName);
            AbstractAction LayerSave =
                new AbstractAction(layerSaveMenu,iconSaveFile) {
                   /**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                  if ( getSelectionModel().getSelectionCount() > 1 ) {
                      _view.getSpatialView().saveToDisk(getSelectedLayers(_view));
                  } else {
                      fLayerNode.getSpatialLayer().savePropertiesToDisk();
                  }
              }
            };
            popup.add(LayerSave);
        }
        popup.addSeparator();
        AbstractAction a1 = 
            new AbstractAction(this.propertyManager.getMsg("TREE_PROPERTIES"),iconLayerProperties) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    SVSpatialLayerProps slp = new SVSpatialLayerProps(null,"",false);
                    slp.initDialog(fLayerNode.getSpatialLayer());
                    // show dialog
                    slp.setVisible(true);
                }
            };
        popup.add(a1);
        return popup;
    }
    
    private JPopupMenu addQueryMenuElements(JPopupMenu       _popup,
                                            final LayerNode  _fLayerNode)  
    {
        // Query by shape but not when SVGraphicLayer or SVQueryLayer
        //
        JMenu layerOrderMenu = new JMenu(this.propertyManager.getMsg("LAYER_MENU_QUERY_MENU"));
        layerOrderMenu.setIcon(iconQuery);
        // Add in objects
        //
        ViewOperationListener.VIEW_OPERATION[] operations = ViewOperationListener.VIEW_OPERATION.values();
        for (int i = 0; i < operations.length; i++) {
            ViewOperationListener.VIEW_OPERATION vop = operations[i];
            // We only create context menus for QUERY_* operations
            //
            if ( vop.toString().startsWith("QUERY_") == false )
                continue;
            
            // Also, no menus for Circle queries when layer is geodetic
            //
            if ( _fLayerNode.getSpatialLayer().isGeodetic() &&
                 vop == ViewOperationListener.VIEW_OPERATION.QUERY_CIRCLE ) {
                continue;
            }
        
          final JMenu objectMenu = getMenu(vop); 
          
          layerOrderMenu.add(objectMenu);
          final ViewOperationListener.VIEW_OPERATION vo = ViewOperationListener.VIEW_OPERATION.valueOf("QUERY_" + objectMenu.getText().toUpperCase());
          // Add in topological search types
          //
          ImageIcon searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.RELATE);
          if ( searchIcon != null ) {
              objectMenu.add( new AbstractAction(this.propertyManager.getMsg("RELATE"),searchIcon) {
                  /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.RELATE);
                  }
              });
          }
          
          boolean supportedVersion = true;
          try {
              OracleConnection conn = _fLayerNode.getSpatialLayer().getConnection();
              if ( conn == null ) {
                  supportedVersion = false;
              } else {
                  supportedVersion = conn.getMetaData().getDatabaseMajorVersion() > 9;
              }
          } catch (SQLException e) {
              supportedVersion = true;
          }
          if ( ! supportedVersion ) {
              continue;
          }
          
          // Add in topological search types
          //
           searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.ANYINTERACT);
          if ( searchIcon != null ) {
              objectMenu.add( new AbstractAction(this.propertyManager.getMsg("ANYINTERACT"),searchIcon) {
                  /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.ANYINTERACT);
                  }
              });
          }
          
          // No more menus if layer is geodetic and feature is a rectangle
          // NOTE: SDO_RELATE and SDO_ANYINTERACT must be above and everything else below
          //
          if ( _fLayerNode.getSpatialLayer().isGeodetic() &&
               vop == ViewOperationListener.VIEW_OPERATION.QUERY_RECTANGLE )
              continue;
          
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.CONTAINS);
          if ( searchIcon != null ) {
              objectMenu.add( new AbstractAction(this.propertyManager.getMsg("CONTAINS"),searchIcon) {
                      /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent e) {
                        query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.CONTAINS);
                      }
              });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.COVEREDBY);
          if ( searchIcon != null ) {
              objectMenu.add( new AbstractAction(this.propertyManager.getMsg("COVEREDBY"),searchIcon) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.COVEREDBY);
                }
            });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.COVERS);
          if ( searchIcon != null ) {
            objectMenu.add( new AbstractAction(this.propertyManager.getMsg("COVERS"),searchIcon) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                    query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.COVERS);
                }
            });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.EQUAL);
          if ( searchIcon != null ) {
          objectMenu.add( new AbstractAction(this.propertyManager.getMsg("EQUAL"),searchIcon) {
                  /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.EQUAL);
                  }
              });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.INSIDE);
          if ( searchIcon != null ) {
          objectMenu.add( new AbstractAction(this.propertyManager.getMsg("INSIDE"),searchIcon) {
                  /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.INSIDE);
                  }
              });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.ON);
          if ( searchIcon != null ) {
          objectMenu.add( new AbstractAction(this.propertyManager.getMsg("ON"),searchIcon) {
                  /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.ON);
                  }
              });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.OVERLAPBDYDISJOINT);
          if ( searchIcon != null ) {
          objectMenu.add( new AbstractAction(this.propertyManager.getMsg("OVERLAPBDYDISJOINT"),searchIcon) {
                  /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.OVERLAPBDYDISJOINT);
                  }
              });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.OVERLAPBDYINTERSECT);
          if ( searchIcon != null ) {
            objectMenu.add( new AbstractAction(this.propertyManager.getMsg("OVERLAPBDYINTERSECT"),searchIcon) {
                  /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.OVERLAPBDYINTERSECT);
                  }
              });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.OVERLAPS);
          if ( searchIcon != null ) {
            objectMenu.add( new AbstractAction(this.propertyManager.getMsg("OVERLAPS"),searchIcon) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.OVERLAPS);
                  }
              });
          }
          searchIcon = getTopologicalSearchIcon(vo,Constants.SDO_OPERATORS.TOUCH);
          if ( searchIcon != null ) {
            objectMenu.add( new AbstractAction(this.propertyManager.getMsg("TOUCH"),searchIcon) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
                      query(_fLayerNode.getSpatialLayer(), vo, Constants.SDO_OPERATORS.TOUCH);
                  }
              });
          }
        }
        _popup.add(layerOrderMenu);
        _popup.addSeparator();
        return _popup;
    }

    private JMenu getMenu(ViewOperationListener.VIEW_OPERATION _vop) {
      JMenu m = new JMenu(_vop.toString().replace("QUERY_","").toLowerCase());
      m.setIcon(this.svp.getQueryIcon(_vop));              
      return m;
    }

    private void query(SVSpatialLayer                       _layer,
                       ViewOperationListener.VIEW_OPERATION _queryType,
                       Constants.SDO_OPERATORS              _operatorType) 
    {
        // set spatial query operator
        this.currentSpatialOperator = _operatorType;
        this.setQueryTarget(_layer);
        svp.createMeasureCombo(_queryType);
    }
    
  // ============================================================  

  class RJLTransferable implements Transferable {
      Object object;
      public RJLTransferable (Object o) {
          object = o;
      }
      public Object getTransferData(DataFlavor df)
          throws UnsupportedFlavorException, IOException {
          if (isDataFlavorSupported (df))
              return object;
          else
              throw new UnsupportedFlavorException(df);
      }
      public boolean isDataFlavorSupported(DataFlavor df) {
          return (df.equals(localObjectFlavor) ||
                  df.equals(TransferableTreeNode.TREE_NODE_FLAVOR));
      }
      public DataFlavor[] getTransferDataFlavors () {
          return supportedFlavors;
      }
  }
  
  // ------------------------------- Drag and Drop Source
    
    boolean dragging = false;
    public void setDragging(boolean _mode) {
      this.dragging = _mode;
    }
    public boolean isDragging() {
      return this.dragging;
    }
  
    public void dragEnter (DragSourceDragEvent dsde) {}
    public void dragOver (DragSourceDragEvent dsde) {}
    public void dropActionChanged (DragSourceDragEvent dsde) {}  
    public void dragDropEnd (DragSourceDropEvent dsde) {
          dropTargetNode = null;
          draggedNode = null;
          repaint();
    }
    public void dragExit (DragSourceEvent dse) {
        dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
    }
  
    // ------------------------------- Drag and Drop Target methods

    public void dragEnter (DropTargetDragEvent dtde) {
        Point dropPoint = dtde.getLocation();
        // int index = locationToIndex (dropPoint);
        TreePath path = getPathForLocation (dropPoint.x, dropPoint.y);
        if (path == null ) {
          return;
        }
        
        // Check if dragging database connection
        int i;
        Transferable transferable = dtde.getTransferable();
        List<?> list;
        try {
            list = RaptorDnD.getTransferableNodes(new Transferable[] { transferable });
            i = 0;
            DefaultMutableTreeNode dmttn = null;
            if(list.get(i) instanceof MultiTransferable) {
               dmttn = (DefaultMutableTreeNode)((MultiTransferable)list.get(i)).getTransferData(TransferableTreeNode.TREE_NODE_FLAVOR);
            } else {
               dmttn = (DefaultMutableTreeNode)list.get(i);
            }
            Object obj = dmttn.getUserObject();
            if(obj instanceof DatabaseConnection) {
                if(!Connections.getInstance().isOracle(Connections.getActiveConnectionName())) {
                    dtde.acceptDrag(dtde.getDropAction());
                    dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
                    setDragging(true);
                } else {
                    dtde.rejectDrag();
                    setDragging(false);
                    return;
                }
            }
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        }
        TreeNode tmpSourceNode = (TreeNode) path.getLastPathComponent();
        if ( tmpSourceNode instanceof LayerNode ) {
          // check if SVGraphicLayer 
          LayerNode lNode = (LayerNode)tmpSourceNode;
          if ( ( lNode.getSpatialLayer() instanceof SVGraphicLayer    || lNode.getSpatialLayer().getClassName().equals(Constants.KEY_SVGraphicLayer) ) ||
               ( lNode.getSpatialLayer() instanceof SVQueryLayer      || lNode.getSpatialLayer().getClassName().equals(Constants.KEY_SVQueryLayer) ) ||
               ( lNode.getSpatialLayer() instanceof SVWorksheetLayer  || lNode.getSpatialLayer().getClassName().equals(Constants.KEY_SVWorksheetLayer) )) {
              // let user know
              lNode.getSpatialLayer().getSpatialView().getSVPanel().setMessage(this.propertyManager.getMsg("STATUS_CANNOT_DND_GRAPHIC_THEME",lNode.getText()),true/*ring Bell*/);
              dtde.rejectDrag();
              setDragging(false);
              return;
          } 
        }
        dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        setDragging(true);
    }
    
    public void dragExit (DropTargetEvent dte) {}    
    public void dropActionChanged (DropTargetDragEvent dtde) {}
    // DragGestureListener
    public void dragGestureRecognized (DragGestureEvent dge) {
        // find object at this x,y
        Point clickPoint = dge.getDragOrigin();
        TreePath path = getPathForLocation (clickPoint.x, clickPoint.y);
        if (path == null) {
            // Not on a node
            return;
        }
        draggedNode = (TreeNode) path.getLastPathComponent();
        if (draggedNode instanceof LayerNode) {
            Transferable trans = new RJLTransferable (draggedNode);
            dragSource.startDrag (dge,Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),trans, this);
        } else {
            dropTargetNode = null;
            draggedNode = null;          
        }
    }
    public void dragOver (DropTargetDragEvent dtde) {
        // figure out which cell it's over, no drag to self
        Point dragPoint = dtde.getLocation();
        TreePath path = getPathForLocation (dragPoint.x, dragPoint.y);
        if (path == null)
            dropTargetNode = null;
        else {
            TreeNode tmpTargetNode = (TreeNode) path.getLastPathComponent();
            // if not equal to previous then record this node
            if ( dropTargetNode != tmpTargetNode ) {
                dropTargetNode = tmpTargetNode;
                DefaultTreeModel model = ((DefaultTreeModel) this.getModel());
                if ( dropTargetNode instanceof LayerNode ) {
                    LayerNode lNode = (LayerNode)dropTargetNode;
                    // Force redraw of moved layer's new view 
                    model.nodeStructureChanged(lNode.getParent());
                } else if ( dropTargetNode instanceof ViewNode ) {
                    ViewNode vNode = (ViewNode)dropTargetNode;
                    vNode.expand();
                    // Force redraw of moved layer's new view 
                    model.nodeStructureChanged(vNode);
                }
            }
        }
    } 
    public void drop (DropTargetDropEvent dtde) 
    {
        LOGGER.debug("START: drop = localObjectFlavor " + dtde.getTransferable().isDataFlavorSupported(localObjectFlavor) + 
             " TransferableTreeNode.TREE_NODE_FLAVOR = " + dtde.getTransferable().isDataFlavorSupported(TransferableTreeNode.TREE_NODE_FLAVOR));
    
        Point dropPoint = dtde.getLocation();
        // int index = locationToIndex (dropPoint);
        TreePath path = getPathForLocation (dropPoint.x, dropPoint.y);
  
        if (path == null ) {
            // Drop path cannot be determined
            return;
        }
        
        if ( ! dtde.getTransferable().isDataFlavorSupported(localObjectFlavor) ) {
            if ( dtde.getTransferable().isDataFlavorSupported(TransferableTreeNode.TREE_NODE_FLAVOR) ) {
                LOGGER.info("Drag/Drop of this object not yet supported");
                dtde.dropComplete (true);
                return;
            }
        }
        
        try 
        {
            dtde.acceptDrop(DnDConstants.ACTION_MOVE);
            Object droppedObject = dtde.getTransferable().getTransferData(localObjectFlavor);
            LOGGER.debug("START: drop - " + droppedObject.getClass().toString());
  
            // Note: draggedNode can only ever be a LayerNode (see dragGestureRecognized())
            
            MutableTreeNode draggedObject = null;
            if (droppedObject instanceof MutableTreeNode) {
                draggedObject = (MutableTreeNode) droppedObject;
            } else {
                draggedObject = new DefaultMutableTreeNode (droppedObject);
            }
            
            // insert into spec'd path.  
            // if dropped into a view make it last child of that parent
            DefaultTreeModel model = ((DefaultTreeModel) this.getModel());

            DefaultMutableTreeNode dropNode = (DefaultMutableTreeNode) path.getLastPathComponent();

            // Save SRID information
            ViewNode sourceViewNode = (ViewNode)((LayerNode)draggedObject).getParent();
            ViewNode targetViewNode   = null;
            
            // Move the node to its new home
            //
            // get position of new view node
            int index = -1;
            if (dropNode instanceof LayerNode ) 
            {
                // dropped on self?
                if (draggedObject == dropNode) {
                    // Don't remove node or reinsert it, simply do nothing
                    dtde.dropComplete (true);
                    return;
                } 
                
                LayerNode targetLayerNode = (LayerNode)dropNode;
                targetViewNode = (ViewNode)targetLayerNode.getParent(); 
                // get position of new view node
                index = targetViewNode.getIndex(dropNode);
            } else if ( dropNode instanceof ViewNode ) {
                targetViewNode = (ViewNode)dropNode;
                // get position of new view node
                index = targetViewNode.getChildCount();
            } else {
                // dropped on to root so do nothing
                dtde.dropComplete (true);
                return;
            }
            // Now do insert
            model.insertNodeInto(draggedObject,targetViewNode,index);
            // Make targetView active if different
            if ( ! targetViewNode.getNodeName().equals(sourceViewNode.getNodeName()) ) {
                sourceViewNode.setActive(false);
                sourceViewNode.collapse();
                targetViewNode.setActive(true);
                targetViewNode.expand();
            }
            LayerNode lNode = (LayerNode)draggedObject;            
            // Do we need to change Active status of layers left behind?
            // If dragged layer was active in old group then we need to reassign active status to first node in the source view
            //
            if ( lNode.isActive() ) 
            {
                LayerNode newActiveLayer = sourceViewNode.getLayerAt(0);
                if ( newActiveLayer != null )
                {
                    newActiveLayer.setActive(true);
                    // Only new Active layer needs notification for a redraw as previously active node has now gone from group/view
                    model.nodeChanged(newActiveLayer);
                }
            }
            // Set layer active only if it is the only layer in the view
            lNode.setActive(targetViewNode.getChildCount()==0?true:false);

            // Do we need to change ButtonGroup and source Layer's SpatialView?
            //
            if ( ! sourceViewNode.getNodeName().equalsIgnoreCase(targetViewNode.getNodeName()) ) {
                // Yes
                // Remove dragged layer from its old button group
                sourceViewNode.btnGroupViewLayers.remove(lNode.getRadioButton());
                // Change ButtonGroup of this view so that it confirms to the selection determined within the new view
                //
                lNode.setButtonGroup(targetViewNode.btnGroupViewLayers);
            }
            
            // Ensure static SpatialView reference in new layer is correct
            lNode.getSpatialLayer().setView(targetViewNode.getSpatialView());

            // We do not need to project this layer if:
            // 1. If the layer's actual SRID NULL OR
            // 2. The targetView's SRID is NULL
            // 3. The source and target view's SRIDs are the same
            //
            // NOTE: When a layer with same SRID as a view is dragged to another and projected, 
            //       but then moved back it needs its SQL changed
            //
/*// Messages.log("lNode.getSpatialLayer().getSRID()-" + lNode.getSpatialLayer().getSRID() + "-.equals(Constants.NULL) " + 
             lNode.getSpatialLayer().getSRID().equals(Constants.NULL) + 
             " || targetViewNode.getSpatialView().getSRID()-"+targetViewNode.getSpatialView().getSRID()+"-.equals(Constants.NULL) " + 
             targetViewNode.getSpatialView().getSRID().equals(Constants.NULL) + 
             " || sourceViewNode.getSpatialView().getSRID()-"+sourceViewNode.getSpatialView().getSRID()+"-.equals(targetViewNode.getSpatialView().getSRID())" + 
             sourceViewNode.getSpatialView().getSRID().equals(targetViewNode.getSpatialView().getSRID()));
*/
            
            boolean projectionRequired = false;
            if ( lNode.getSpatialLayer().getSRIDAsInteger() != targetViewNode.getSpatialView().getSRIDAsInteger() ) {
                projectionRequired = ! (    lNode.getSpatialLayer().getSRID().equals(Constants.NULL)           // Layer has NULL SRID
                                         || targetViewNode.getSpatialView().getSRID().equals(Constants.NULL)   // Target View has NULL SRID 
                                         || sourceViewNode.getSpatialView().getSRID().equals(targetViewNode.getSpatialView().getSRID() ) // Layer == View SRID (and both not NULL)
                                       );
            }
            lNode.getSpatialLayer().setProject(projectionRequired, 
                                               true /* Always recalc mbr of layer*/);
            
            // Change SQL of droppedNode based on layer/target SRID
            // lNode.getSpatialLayer().setInitSQL();

            // refresh any changes to tree 
            model.nodeStructureChanged(sourceViewNode);
            model.nodeStructureChanged(targetViewNode);
            
        } catch (UnsupportedFlavorException e) {
            LOGGER.error("Invalid drop of non-view/layer object."); 
        } catch (Exception e) {
            LOGGER.error("drag and drop error " + e.getMessage());
        } 
        dtde.dropComplete (true);
        setDragging(false);
LOGGER.debug("START: drop");
    }

    // --------------------------------------------------- ViewNode
    
    class ViewNode 
    extends DefaultMutableTreeNode {
        
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private SpatialView          sView = null;
        private JRadioButton       btnView = new JRadioButton();
        private DefaultTreeModel treeModel = null;
        
        /**
          * View's list of layers 
         */
        protected ButtonGroup btnGroupViewLayers = new ButtonGroup();

        public ViewNode(SpatialView _view,
                        boolean     _isSelected,
                        Font        _fontValue)
        
        {
            this.treeModel = (DefaultTreeModel)getModel();
            btnGroupViews.add(this.btnView);
            this.sView = _view;
            
            if (_fontValue != null) {
                btnView.setFont(_fontValue);
            }
            Boolean booleanValue = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
            btnView.setFocusPainted((booleanValue != null) && (booleanValue.booleanValue()));
            btnView.setHorizontalTextPosition(SwingConstants.RIGHT);
            btnView.setText(_view.getVisibleName());
            setActive(_isSelected);
        }

        public void expand() {
            expandPath(new TreePath(this.getPath()));
            treeModel.nodeStructureChanged(this);
        }

        public void collapse() {
            collapsePath(new TreePath(this.getPath()));
            treeModel.nodeStructureChanged(this);
        }
        
        public int getDrawableLayerCount() {
            if ( this.getChildCount() == 0 ) {
                return 0;
            }
            int drawCount = 0;
            DefaultMutableTreeNode node = null;
            Enumeration<?> e = this.breadthFirstEnumeration();
            while (e.hasMoreElements()) {
                node = (DefaultMutableTreeNode)e.nextElement();
                if ( node instanceof LayerNode ) {
                    drawCount++;
                } 
            }
            return drawCount;
        }
        
        public void setAllLayersDrawable(boolean _onOff) {
          if ( this.getChildCount() != 0 ) {
              DefaultMutableTreeNode node = null;
              Enumeration<?> e = this.breadthFirstEnumeration();
              while (e.hasMoreElements()) {
                  node = (DefaultMutableTreeNode)e.nextElement();
                  if ( node instanceof LayerNode ) {
                      ((LayerNode)node).setDraw(_onOff);
                  } 
              }
              treeModel.nodeStructureChanged(this);
            }
        }

        public boolean hasDrawableLayer() {
          if ( this.getChildCount() != 0 ) {
              DefaultMutableTreeNode node = null;
              Enumeration<?> e = this.breadthFirstEnumeration();
              while (e.hasMoreElements()) {
                  node = (DefaultMutableTreeNode)e.nextElement();
                  if ( node instanceof LayerNode && ((LayerNode)node).isDraw() ) {
                    return true;
                  } 
              }
            }
          return false;
        }

        public SVSpatialLayer getActiveLayer() 
        {
            LayerNode lNode = this.getActiveLayerNode();
            return lNode==null?null:lNode.getSpatialLayer();
        }

        public LayerNode getActiveLayerNode()
        {
            if ( this.getChildCount() != 0 )
            {
                DefaultMutableTreeNode node = null;
                Enumeration<?> e = this.breadthFirstEnumeration();
                while (e.hasMoreElements())
                {
                    node = (DefaultMutableTreeNode)e.nextElement();
                    if ( node instanceof LayerNode && ((LayerNode)node).isActive() )
                    {
                        return (LayerNode)node;
                    }
                }
            }
            return null;
        }

        public void createLayerCopy(LayerNode _lNode)  
        {
            try {
                if ( _lNode.getSpatialLayer() instanceof SVQueryLayer ) {
LOGGER.debug("ViewLayerTree.createLayerCopy - SVQueryLayer");
                    SVQueryLayer qLayer = new SVQueryLayer((SVQueryLayer)_lNode.getSpatialLayer());
                    this.addLayer(qLayer,_lNode.isDraw(),false);
                } else if ( _lNode.getSpatialLayer() instanceof SVWorksheetLayer ) {
LOGGER.debug("ViewLayerTree.createLayerCopy - SVWorksheetLayer");
                    SVWorksheetLayer wLayer = new SVWorksheetLayer((SVWorksheetLayer)_lNode.getSpatialLayer());
                    this.addLayer(wLayer,_lNode.isDraw(),false);
                } else if ( _lNode.getSpatialLayer() instanceof SVGraphicLayer ) {
LOGGER.debug("ViewLayerTree.createLayerCopy - SVGraphicLayer");
                    SVGraphicLayer gLayer = new SVGraphicLayer((SVGraphicLayer)_lNode.getSpatialLayer());
                    this.addLayer(gLayer,_lNode.isDraw(),false);
                } else {
LOGGER.debug("ViewLayerTree.createLayerCopy - SVSpatialLayer");
                    SVSpatialLayer sLayer = new SVSpatialLayer(_lNode.getSpatialLayer());
                    this.addLayer(sLayer,_lNode.isDraw(),false);
                }
            } catch (Exception e) {
                // Throws message from createCopy() in SVGraphicTheme will be
                // throw new Exception(super.getPropertyManager().getMsg("GRAPHIC_THEME_COPY"));
                //
                JOptionPane.showMessageDialog(null,
                                              e.getMessage(),
                                              MainSettings.EXTENSION_NAME,
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
        
        public void addLayer(SVSpatialLayer _layer,
                             boolean        _isDraw,
                             boolean        _isActive) 
        {
LOGGER.debug("ViewLayerTree.addlayer: " + _layer.getLayerName());
              int insertPosition = 0;
              if ( mainPreferences.getNewLayerPosition().equalsIgnoreCase(Constants.layerPositionType.BOTTOM.toString()) ) {
                  insertPosition = this.getChildCount();
              }
              String adjustedName = checkName(_layer.getLayerName());
LOGGER.debug("viewLayerTree.addLayer adjustedName =" + adjustedName);
              if ( ! adjustedName.equalsIgnoreCase(_layer.getLayerName())) {
                  _layer.setLayerName(adjustedName);
                  _layer.setVisibleName(_layer.getLayerName().replace("_"," "));
              }
              MutableTreeNode newLayerNode = new LayerNode(_layer,_isDraw,_isActive,this.btnGroupViewLayers,fontValue);
              this.insert(newLayerNode,insertPosition);
        }
        
        public LayerNode getLayerAt(int _layerNumber) 
        {
            int layerCount = 0;
            if ( this.getChildCount() != 0 )
            {
                DefaultMutableTreeNode node = null;
                Enumeration<?> e = this.breadthFirstEnumeration();
                while (e.hasMoreElements())
                {
                    node = (DefaultMutableTreeNode)e.nextElement();
                    if ( node instanceof LayerNode )
                    {
                        if ( layerCount == _layerNumber )
                        {
                            return (LayerNode)node;
                        }
                        layerCount++;
                    }
                }
            }
            return null;
        }
        
        public LinkedHashMap<String,SVSpatialLayer> getLayers() {
            LinkedHashMap<String,SVSpatialLayer> lhm = new LinkedHashMap<String,SVSpatialLayer>(25);
            DefaultMutableTreeNode node = null;
            LayerNode lNode = null;
            Enumeration<?> e = this.breadthFirstEnumeration();
            while (e.hasMoreElements()) {
                node = (DefaultMutableTreeNode) e.nextElement();
                if ( node instanceof LayerNode ) {
                    lNode = (LayerNode)node; 
                    lhm.put(lNode.getNodeName(),
                            lNode.getSpatialLayer());
                }
            }
            return lhm;
        }      

        private List<LayerNode> getLayersAsList() {
              List<LayerNode> lhm = new ArrayList<LayerNode>(this.getChildCount());
              DefaultMutableTreeNode node = null;
              int i = 0;
              int sortPosition = this.getChildCount();
              Enumeration<?> e = this.preorderEnumeration();
              while (e.hasMoreElements()) {
                  node = (DefaultMutableTreeNode) e.nextElement();
                  if ( node instanceof LayerNode ) {
                      LayerNode lNode = (LayerNode)node;
                      lNode.setSortPosition(sortPosition--);
                      lhm.add(i++,lNode);
                  }
              }
              return lhm;
          }

        boolean reinsert = false;
        @Override
        public void insert(MutableTreeNode newChild, int childIndex) {
            if ( newChild == null )
                return;
            super.insert(newChild, childIndex);
            if ( this.reinsert )
                return;
            this.reinsert = true;
            if ( svp.isLoading() || isDragging() || ! mainPreferences.isLayerTreeGeometryGrouping() ) {
                insert(newChild,childIndex);
            } else {
                List<LayerNode> lNodes = getLayersAsList();
                Collections.sort(lNodes,nodeComparator);
                this.removeAllChildren();
                treeModel.nodeStructureChanged(this);
                ListIterator<LayerNode> i = lNodes.listIterator();
                for (int j=0; j<lNodes.size(); j++) {
                    insert(i.next(),j);
                }
            }
            treeModel.nodeStructureChanged(this);
            this.reinsert = false;
        }
       
        protected Comparator<LayerNode> nodeComparator = new Comparator<LayerNode> () 
        {
            @Override
            public int compare(LayerNode _layerOne, 
                               LayerNode _layerTwo) {
/*
System.out.println("_layerOne.getSpatialLayer(" + _layerOne.getSpatialLayer().getLayerName() + ").getGeometryType(" +
_layerOne.getSpatialLayer().getGeometryType().toString() + 
").ordinal() - _layerTwo.getSpatialLayer(\" + _layerTwo.getSpatialLayer().getLayerName() + \").getGeometryType().ordinal(" + 
_layerTwo.getSpatialLayer().getGeometryType().toString() + ") = " + 
(_layerTwo.getSpatialLayer().getGeometryType().ordinal() < _layerOne.getSpatialLayer().getGeometryType().ordinal() ? -1 : (_layerTwo.getSpatialLayer().getGeometryType().ordinal() == _layerOne.getSpatialLayer().getGeometryType().ordinal() ? 0 : 1)));
*/
                return (_layerTwo.getSpatialLayer().getGeometryType().ordinal() < 
                        _layerOne.getSpatialLayer().getGeometryType().ordinal() 
                        ? -1 
                        : (_layerTwo.getSpatialLayer().getGeometryType().ordinal() == 
                           _layerOne.getSpatialLayer().getGeometryType().ordinal() 
                           ? 0 
                           : 1));
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof LayerNode))
                    return false;
                LayerNode lNode = (LayerNode)obj;
                return (lNode.getSortPosition() == lNode.getSortPosition());
            }
         
            @Override
            public int hashCode() {
                int hash = 7;
                return hash;
            }
        };
        
        public void setLayerActive(LayerNode _layerNode) {
            _layerNode.setLayerActive();
            treeModel.nodeStructureChanged(this);
        }
        
        public void setLayerActive(int _layerNumber) 
        {
            if ( this.getChildCount() != 0 ) {
                int layerCount = -1;
                DefaultMutableTreeNode node = null;
                Enumeration<?> e = this.breadthFirstEnumeration();
                while (e.hasMoreElements())
                {
                    node = (DefaultMutableTreeNode)e.nextElement();
                    if (node instanceof LayerNode ) {
                        LayerNode lNode = (LayerNode)node;
                        if ( layerCount == _layerNumber ) {
                            this.setLayerActive(lNode);
                            return;
                        }
                        layerCount++;
                    }
                }
            }
        }

        public void removeViewReferences() {
            btnGroupViews.remove(this.btnView);
            this.btnView = null;
            this.sView = null;
        }
        
        public SpatialView getSpatialView() {
          return this.sView;
        }
        
        public JRadioButton getRadioButton() {
          return this.btnView;
        }
        
        public boolean isActive() {
            return btnGroupViews.isSelected(this.btnView.getModel());
        }
        
        public void setActive(boolean _isActive) {
              if (_isActive) btnGroupViews.setSelected(this.btnView.getModel(),_isActive);
        }
        
        // Rendering of node done with visible name user can change
        public void setVisibleName(String _text) {
            this.sView.setVisibleName(_text);
        }
        
        public String getVisibleName() {
            return this.sView.getVisibleName();
        }

        // Internal work is done with unique non-changeable name
        //
        public void setViewName(String _text) {
            this.sView.setViewName(_text);
        }

        public String getNodeName() {
            return this.sView.getViewName();
        }
        
        public void setText(String _text) {
            this.btnView.setText(_text);
        }
        
        public String getText() {
            return this.btnView.getText();
        }
        
        public String toString() {
            return this.sView.getViewName();
        }
        
        public void setColor(boolean _selected) {
            if ( _selected ) {
              this.setTextSelectionColor();
            } else {
              this.setTextNormalColor();          
            }
        }

        public void setTextSelectionColor() {
          this.btnView.setForeground(selectionForeground);
          this.btnView.setBackground(selectionBackground);        
        }
        
        public void setTextNormalColor() {
            this.btnView.setForeground(textForeground);
            this.btnView.setBackground(textBackground);        
        }
  
    }
  
    // --------------------------------------------------- LayerNode 
  
    class LayerNode 
    extends DefaultMutableTreeNode {
  
      private static final long serialVersionUID = 1L;
      
	  private SVSpatialLayer     sLayer = null;
      private JPanel       leafRenderer = new JPanel();
      private FlowLayout     leafLayout = new FlowLayout(FlowLayout.LEFT,5,2);
      private JCheckBox       leafCheck = new JCheckBox();
      private JRadioButton   leafActive = new JRadioButton();
      private JLabel           leafText = new JLabel();
      private ButtonGroup btnGroupLayer = null;
      private DefaultTreeModel treeModel = null;

      public LayerNode(SVSpatialLayer _layer, 
                       boolean        _isDraw,
                       boolean        _isActive,
                       ButtonGroup    _btnGroup,
                       Font           _fontValue)
      {
          this.treeModel = (DefaultTreeModel)getModel();
          this.leafActive.setSelected(_isActive);
          this.btnGroupLayer = _btnGroup;
          this.sLayer        = _layer;
          this.leafText      = new JLabel(_layer.getVisibleName(), this.getIcon(), JLabel.RIGHT);

          if (_fontValue != null) {
            this.leafText.setFont(_fontValue);
          }
          Boolean booleanValue = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
          this.leafCheck.setFocusable(true);
          this.leafCheck.setFocusPainted((booleanValue != null) && (booleanValue.booleanValue()));
          
          setDraw(_isDraw);
          setActive(_isActive);
          this.btnGroupLayer.add(this.leafActive);
          
          this.leafRenderer.setFocusable(true);
          this.leafLayout.addLayoutComponent("DrawBox",          this.leafCheck);
          this.leafLayout.addLayoutComponent("ActiveButton",     this.leafActive);
          this.leafLayout.addLayoutComponent("IconAndLayerName", this.leafText);
          this.leafRenderer.setLayout(this.leafLayout);
          this.leafRenderer.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
          this.leafRenderer.add(this.leafCheck);
          this.leafRenderer.add(this.leafActive);
          this.leafRenderer.add(this.leafText);
      } // public CheckNode

      public void removeLayerReferences() {
          this.btnGroupLayer.remove(this.leafActive);
          this.btnGroupLayer = null;
          this.leafActive = null;
          this.leafLayout = null;
          this.leafCheck = null;
          this.leafText = null;
          this.leafRenderer = null;
          this.sLayer = null;
      }
  
      public void setTextForeground() {
          // Mark layer according to whether it is indexed or not
          if ( this.sLayer instanceof SVSpatialLayer ) {
              Font lFont = this.leafText.getFont();
              if ( this.sLayer.hasIndex() ) { 
                  this.leafText.setFont(new Font(lFont.getName(), Font.PLAIN, lFont.getSize()));
              } else {
                  this.leafText.setFont(new Font(lFont.getName(), Font.ITALIC, lFont.getSize()));
              }
          }
          if ( ! this.sLayer.getStyling().getLabelColor().equals(Color.BLACK) ) {
             this.leafText.setForeground(this.sLayer.getStyling().getLabelColor());
          } else {
              switch ( this.sLayer.getGeometryType() ) { 
                case POINT:
                case MULTIPOINT:
                    this.leafText.setForeground(this.sLayer.getStyling().getPointColor("0,0,1"));
                    break;
                case LINE:
                case MULTILINE:
                    this.leafText.setForeground(this.sLayer.getStyling().getLineColor("0,1,0"));
                    break;
                case SOLID:
                case MULTISOLID:
                case POLYGON:
                case MULTIPOLYGON:
                case COLLECTION:
                    if ( this.sLayer.getStyling().isPerformShade() ) 
                        this.leafText.setForeground(this.sLayer.getStyling().getShadeColor(Colours.getRGBa(Color.BLACK)));
                    else
                        this.leafText.setForeground(this.sLayer.getStyling().getLineColor(Colours.getRGBa(Color.BLACK)));
                    break;
                default:
                    this.leafText.setForeground(Color.BLACK);
              }
          }
      }

      public String getSpatalLayerType() {
          return SVSpatialLayer.CLASS_NAME;
      }
      
      public SVSpatialLayer getSpatialLayer() {
          return this.sLayer;
      }

      public void setSpatialLayer(SVSpatialLayer _sLayer) {
          this.sLayer = _sLayer;
      }

      public void setIcon() {
          this.leafText.setIcon(this.getIcon());        
      }
      
      public Icon getIcon() 
      {
          Color fillColor = Color.WHITE;
          Color drawColor = Color.BLACK;
          switch (this.sLayer.getGeometryType()) {
          case POINT        : return Tools.createIcon(LineStyle.getStroke(LineStyle.LINE_STROKES.LINE_SOLID,1),
                                                      18,18,
                                                      PointMarker.getPointShape(this.sLayer.getStyling().getPointType(),
                                                                                new Point2D.Double(9,9),
                                                                                18,  /* PointSize */ 
                                                                                0),
                                                      this.sLayer.getStyling().getPointColor(null), 
                                                      this.sLayer.getStyling().getPointColor(null));
          
          case MULTIPOINT   : drawColor = this.sLayer.getStyling().getPointColor(null);
                              fillColor = this.sLayer.getStyling().getPointColor(null); 
               break;
          
          case LINE         : 
          case MULTILINE    : drawColor = this.sLayer.getStyling().getLineColor(null);
                              fillColor = this.sLayer.getStyling().getLineColor(null);
               break;
          
          case SOLID        :
          case MULTISOLID   :
          case POLYGON      : 
          case MULTIPOLYGON : 
          case COLLECTION   : drawColor = this.sLayer.getStyling().getLineColor(Colours.getRGBa(drawColor));
                              fillColor = this.sLayer.getStyling().getShadeColor(Colours.getRGBa(fillColor));                            
               break;
          case UNKNOWN      : 
          default           : break;
          } 
          return PointMarker.getGeometryTypeIcon(this.sLayer.getGeometryType(),
                                                 18,18,
                                                 fillColor, drawColor,
                                                 iconQuestionMark); 
      }
      
      public JRadioButton getRadioButton() {
        return this.leafActive;
      }
      
      public boolean isActive() {
        return this.leafActive.isSelected(); 
      }
  
      public void setActive(boolean _isActive) {
          // this.btnGroupLayer.setSelected(this.leafActive.gemodel(),_isActive);
          this.leafActive.setSelected(_isActive);
      }
      
      public void setColor(boolean _selected) {
          if ( _selected ) {
            this.setTextSelectionColor();
          } else {
            this.setTextNormalColor();
          } 
      }
  
      public void setDraw(boolean _isDraw) {
          this.leafCheck.setSelected(_isDraw);
          // Ensure layer is synchronised with this.
          this.sLayer.setDraw(_isDraw);
      }
  
      public boolean isDraw() {
          return this.leafCheck.isSelected();
      }

      public void setButtonGroup(ButtonGroup _bg) {
          if ( this.btnGroupLayer != null )
              this.btnGroupLayer.remove(this.leafActive);
          this.btnGroupLayer = _bg;
          this.btnGroupLayer.add(this.leafActive);
      }

      // Rendering of node done with visible name user can change
      public void setVisibleName(String _text) {
          this.sLayer.setVisibleName(_text);
      }

      public String getVisibleName() {
          return this.sLayer.getVisibleName();
      }

      public String getNodeName() {
          return this.sLayer.getLayerName();
      }
      
      public String getText() {
          return this.leafText.getText(); // this.sLayer.getLayerName();
      }
      
      public void setText(String _text) {
          this.leafText.setText(_text); // this.sLayer.setLayerName(_text);
      } 

      public void setTextSelectionColor()
      {
        // this.leafText.repaint();
        this.leafCheck.setForeground(selectionForeground);
        this.leafCheck.setBackground(selectionBackground);
        this.leafActive.setForeground(selectionForeground);
        this.leafActive.setBackground(selectionBackground);
        this.leafText.setForeground(selectionForeground);
        this.leafText.setBackground(selectionBackground);
        this.leafRenderer.setForeground(selectionForeground);
        this.leafRenderer.setBackground(selectionBackground);
      }

      public void setTextNormalColor() 
      {
        this.leafText.setForeground(textForeground);
        // Override if user has changed color properties.
        this.setTextForeground();
        this.leafText.setBackground(textBackground);
        this.leafText.repaint();
        this.leafRenderer.setForeground(textForeground);
        this.leafRenderer.setBackground(textBackground);
        this.leafCheck.setForeground(textForeground);
        this.leafCheck.setBackground(textBackground);
        this.leafActive.setForeground(textForeground);
        this.leafActive.setBackground(textBackground);
      }
  
      public String getTextColor() {
          return this.leafText.getForeground() + "/" + this.leafText.getBackground();
      }
      
      public JPanel getLeafRenderer() {
          return this.leafRenderer;
          /**
           * System.out.println("leafRenderer (" + node.getLeafRenderer().getWidth() +"," +node.getLeafRenderer().getWidth()+")");
           * for (int i=0; i<node.getLeafRenderer().getComponents().length; i++)
           *    System.out.println(node.getLeafRenderer().getComponents()[i].getName() + " - " + node.getLeafRenderer().getComponents()[i].getWidth() + "," + node.getLeafRenderer().getComponents()[i].getHeight());
           */
      }
      
      int viewNodeSortPosition = -1;
      public void setSortPosition(int _sortPosition) {
          this.viewNodeSortPosition = _sortPosition;
      }
      public int getSortPosition() {
        return this.viewNodeSortPosition;
      }
      
      private void setLayerActive() {
          this.setActive(!this.isActive());
          treeModel.nodeStructureChanged(this.getParent());
      } 
    }

    // --------------------------------------------------- Custom renderer
    
     class ViewLayerTreeCellRenderer
    extends DefaultTreeCellRenderer
    {
		private static final long serialVersionUID = 1L;
		
		boolean isDragOverNode;
        Insets normalInsets, 
               lastItemInsets;
        int BOTTOM_PAD = 30;
        
        public ViewLayerTreeCellRenderer() {
            super();
            normalInsets = super.getInsets();
            lastItemInsets =
                new Insets (normalInsets.top,
                            normalInsets.left,
                            normalInsets.bottom + BOTTOM_PAD,
                            normalInsets.right);
        }
  
      public Component getTreeCellRendererComponent(JTree tree, 
                                                    Object value, 
                                                    boolean isSelected, 
                                                    boolean isExpanded, 
                                                    boolean isLeaf, 
                                                    int row, 
                                                    boolean hasFocus) 
      {
          // Is this node being rendered the current dragOver node?
          isDragOverNode = ((TreeNode)value == dropTargetNode);
          if ( value instanceof LayerNode )
          {
              LayerNode lNode = (LayerNode)value;
              // node.setVisibleName(node.getVisibleName());
              lNode.setText(lNode.getText());
              lNode.setIcon();
              lNode.setDraw(lNode.isDraw());
              lNode.setActive(lNode.isActive());
              lNode.setColor(isSelected || isDragOverNode );
              return lNode.getLeafRenderer();
          } else if ( value instanceof ViewNode ) {
              ViewNode vNode = (ViewNode)value;
              // node.setVisibleName(node.getVisibleName());  // node.getText()
              vNode.setText(vNode.getText());
              vNode.setActive(vNode.isActive());
              vNode.setColor(isSelected || isDragOverNode );
              return vNode.btnView;
          } 
          return super.getTreeCellRendererComponent (tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);
      }
    } 

    // ------------------------------------------------------ MouseListener
    
   /**
    * Finds component (JCheckBox/JRadioButton/Icon/JLabel) in JTree's node's JPanel
    */
   @SuppressWarnings("unused")
   private Object processClickedComponent(LayerNode checkedNode,
                                          int _x,
                                          int _xNodeOffset) 
   {
       int actualCursorPosition = _x - _xNodeOffset;
       int incrementalPosition = 0;
       Component[] components = checkedNode.getLeafRenderer().getComponents();
       if ( checkedNode.getLeafRenderer().getLayout() instanceof FlowLayout ) { 
           actualCursorPosition -= ((FlowLayout)checkedNode.getLeafRenderer().getLayout()).getHgap();                  
       }
       for (int i=0; i<components.length; i++) 
       {
          if ( incrementalPosition  <= actualCursorPosition &&
               actualCursorPosition <= ( incrementalPosition+components[i].getWidth()) ) {
               return components[i];
          }
          incrementalPosition += components[i].getWidth();
       }
       return null;
   }
   
}
