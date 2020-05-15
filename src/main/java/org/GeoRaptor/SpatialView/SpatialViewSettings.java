package org.GeoRaptor.SpatialView;

import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.DockableSV;
import org.GeoRaptor.SpatialView.layers.SVGraphicLayer;
import org.GeoRaptor.SpatialView.layers.SVQueryLayer;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayer;
import org.GeoRaptor.SpatialView.layers.SVWorksheetLayer;
import org.GeoRaptor.tools.Strings;
import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Class for default settings of Spatial view module.
 * All properties are persistance and must be saved when we save properties.
 */
final public class SpatialViewSettings {

    public static final String CLASS_NAME = "SpatialViewSettings";

    private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.SpatialView.SpatialViewSettings");

    /**
     * Load settings for SpatialView
     * @param _node
     */
    public void loadXML(Node _node) 
    {
        SpatialViewPanel svp = DockableSV.getSpatialViewPanel();
        svp.setLoading(true);
        
        // Regardless as to what happens interactively, we insert from bottom because then
        // the first returned will be at the top of the list as this is the order they are stored in
        //
        String oldPosition = Preferences.getInstance().getNewLayerPosition();
        Preferences.getInstance().setNewLayerPosition("BOTTOM");
        String activeViewName = "";
        // Load XML
        //
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            // What was the activeView when last saved?
            activeViewName = (String)xpath.evaluate("ActiveView/text()",_node,XPathConstants.STRING);
//LOGGER.debug("activeViewName is " + activeViewName);
            // Process all saved views
            //
            NodeList vList = (NodeList) xpath.evaluate("Views/View",_node,XPathConstants.NODESET);
//LOGGER.debug("vList is null? " + vList==null?"true":"false");            
            for (int i = 0 ; i < vList.getLength(); i++) 
            { 
                // Now have a <View>....</View> node 
                // Create a new SpatialView object from its attributes
                //
                SpatialView loadedView = new SpatialView(svp,vList.item(i));
                // If this view already exists (created by default) replace existing 
                // one with loadedView add it to the ViewLayerTree
                //
                ViewLayerTree.ViewNode vNode = null;
                vNode = svp.viewLayerTree.addView(loadedView,false);                  

                // Get ActiveLayer for this view
                //
                String activeLayerName = (String)xpath.evaluate("ActiveLayer/text()",vList.item(i),XPathConstants.STRING);

                // Now process View's layers
                //
                SVSpatialLayer    sLayer = null;
                SVQueryLayer      qLayer = null;
                SVWorksheetLayer  wLayer = null;
                NodeList lList = (NodeList) xpath.evaluate("Layers/Layer",vList.item(i),XPathConstants.NODESET);
                for (int j = 0 ; j < lList.getLength(); j++){
                    Node lNode = lList.item(j).cloneNode(true);
                    sLayer = null;
                    qLayer = null;
                    if ( isOfLayerType(lNode,"SVQueryLayer/SQL/text()") ) {
                        qLayer = new SVQueryLayer(loadedView,lNode);
                        vNode.addLayer(qLayer,
                                       qLayer.isDraw(), /* isDrawn */
                                       qLayer.getLayerName().equals(activeLayerName));
                    } else if ( isOfLayerType(lNode,"SVWorksheetLayer/SQL/text()") ) {
                        wLayer = new SVWorksheetLayer(loadedView,lNode);
                        vNode.addLayer(wLayer,
                                       wLayer.isDraw(), /* isDrawn */
                                       wLayer.getLayerName().equals(activeLayerName));
                    } else /* SVSpatialLayer */ {
                        sLayer = new SVSpatialLayer(loadedView,lNode);
                        vNode.addLayer(sLayer,
                                       sLayer.isDraw(), /* isDrawn */
                                       sLayer.getLayerName().equals(activeLayerName));
                    }
                }
                // We've finished processing the view and its layers.
                // Just check to make sure the view has:
                // 1. an active layer
                // 2. A valid MBR
                //
                if ( loadedView.getLayerCount()!=0) {
                    if ( loadedView.getActiveLayer()==null ) {
                        loadedView.setActiveLayer(sLayer==null?qLayer:sLayer);
                    }
                    // And a valid MBR
                    if (loadedView.getMBR()==null || 
                        loadedView.getMBR().isNull() ) 
                    {
                        loadedView.initializeMBR(null);
                    }
                }
            }
        } catch (XPathExpressionException xe) {
            LOGGER.error("XPathExpressionException " + xe.toString());
            xe.getStackTrace();
        }
        // Now create defaultView if not loaded back from settings
        // And make it the default view
        //
        if ( ! svp.createDefaultView(true) ) {
            LOGGER.error("Default GeoRaptor Spatial View not created");
        }
        // Now set Active View if other than defaultView 
        //
        if ( svp.getViewCount()!=0) {
          svp.viewLayerTree.collapseAllViews();
          if ( !Strings.isEmpty(activeViewName) ) {
              if (svp.getView(activeViewName)!=null) {
                  svp.setActiveView(activeViewName);  // Will expand the provided view
              }
          }
          SpatialView activeView = null;
          activeView = svp.getActiveView();
          if ( activeView!=null ) {
              activeView.getMapPanel().Initialise();
          } else {
              ((SpatialView)svp.getViews().values().toArray()[0]).getMapPanel().Initialise();
          }
        } else {
            LOGGER.error("Creation of GeoRaptor Spatial Views failed.");
        }
        
        // Set layer add to ViewLayerTree to what user had it set to
        //
        Preferences.getInstance().setNewLayerPosition(oldPosition);

        svp.setLoading(false);
        
  }

    @SuppressWarnings("unused")
	private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
          Transformer t = TransformerFactory.newInstance().newTransformer();
          t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
          t.setOutputProperty(OutputKeys.INDENT, "yes");
          t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
          System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
      }
    
  public static boolean isOfLayerType(Node _node,String _layerTag) 
  {
      if (_node == null || _node.getNodeName().equals("Layer") == false) {
          //System.out.println("XML node is null or not a Layer");
          return false; // Should throw error
      }
      String xml = null;
      try {
          XPath xpath = XPathFactory.newInstance().newXPath();
          xml = (String)xpath.evaluate(_layerTag,_node,XPathConstants.STRING);
      } catch (XPathExpressionException xe) {
          //LOGGER.error("isQueryLayer - XPathExpressionException " + xe.toString());
      }
      return !Strings.isEmpty(xml);
  }

  /**
   * XML Encode properties for Spatial View, Views etc.
   */
  public String toXML() 
  {
      SpatialViewPanel svPanel = DockableSV.getSpatialViewPanel();
      String SVPanelXML = "<SpatialPanel><ActiveView>" + svPanel.getActiveView().getViewName() + "</ActiveView><Views>";
      // Save all views under the SpatialViewPanel
      //
      Iterator<String> it = svPanel.getViews().keySet().iterator();
      SpatialView itView = null;
      while (it.hasNext()) {
          itView = svPanel.getViews().get(it.next());
          SVPanelXML += "<View>" + itView.toXML(false) + "<Layers>";

          Iterator<String> layerNamesIter = itView.getLayerList().keySet().iterator();
          while (layerNamesIter.hasNext()) {
              SVSpatialLayer sLayer = itView.getLayerList().get(layerNamesIter.next());
              // We don't save SVGraphicLayers
              if ( ! (sLayer instanceof SVGraphicLayer) )  
              {
                SVPanelXML += "<Layer>" + sLayer.toXML() + "</Layer>";
              }
          }
          SVPanelXML += "</Layers></View>";
      }
      SVPanelXML += "</Views></SpatialPanel>";
    return SVPanelXML;
  }
  
  /**
     * @method elementToString()
     * @param n
     * @return
     * @author From http://www.java2s.com/Code/Java/XML/ConvertnodeelementToString.htm
     * @author Simon Greener, Modified August 5th 2010
     */
  public static String elementToString(Node n) 
  {
      if ( n==null )
          return "";

     String name = n.getNodeName();
     short type = n.getNodeType();

     if (Node.CDATA_SECTION_NODE == type) {
       return "<![CDATA[" + n.getNodeValue() + "]]&gt;";
     }

     if ( Node.TEXT_NODE == type) {
       return n.getTextContent();
     }

     StringBuffer sb = new StringBuffer();
     sb.append('<').append(name);

     NamedNodeMap attrs = n.getAttributes();
     if (attrs != null) {
       for (int i = 0; i < attrs.getLength(); i++) {
         Node attr = attrs.item(i);
         sb.append(' ').append(attr.getNodeName()).append("=\"").append(attr.getNodeValue()).append("\"");
       }
     }

     String   textContent = null;
     NodeList    children = n.getChildNodes();
     if (children.getLength() == 0) {
       textContent = n.getTextContent();
       if ((textContent != null && !"".equals(textContent)) ) {
         sb.append(textContent).append("</").append(name).append('>');
       } else {
         sb.append("/>");
       }
     } else {
       sb.append('>');
       for (int i = 0; i < children.getLength(); i++) {
         String childToString = elementToString(children.item(i));
         if (!"".equals(childToString)) {
           sb.append(childToString);
         }
       }
       sb.append("</").append(name).append('>');
     }
     return sb.toString();
   }
}
