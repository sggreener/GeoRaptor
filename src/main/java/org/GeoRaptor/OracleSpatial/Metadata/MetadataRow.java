package org.GeoRaptor.OracleSpatial.Metadata;

import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.tools.Strings;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class MetadataRow 
{
    protected DecimalFormat decFormat;

    protected String dimName;
    protected Double sdoLB, sdoUB, sdoTol;

    protected static Preferences geoRaptorPreferences;

    public MetadataRow() 
    {
        DecimalFormatSymbols decFormatSymbols = new DecimalFormatSymbols();
        decFormatSymbols.setDecimalSeparator('.');

        this.decFormat = (DecimalFormat)DecimalFormat.getInstance(Locale.getDefault());
        this.decFormat.setDecimalFormatSymbols(decFormatSymbols);
        this.decFormat.setMaximumFractionDigits(12); // Is 0.123456789012 enough precision digits for spatial data?
        this.decFormat.setMaximumIntegerDigits(12);  // Is 100,000,000,000 ie 100 billion enough?
        this.decFormat.setGroupingUsed(false);
        
        MetadataRow.geoRaptorPreferences = MainSettings.getInstance().getPreferences();

    }

    public MetadataRow(String _dimName, Double _sdoLB, Double _sdoUB, Double _sdoTol) {
        this();
        this.dimName = _dimName;
        this.sdoLB = _sdoLB;
        this.sdoUB = _sdoUB;
        this.sdoTol = _sdoTol;
    }

    private double toDouble(String _number, double _default) {
        if (Strings.isEmpty(_number) || _number.equalsIgnoreCase("null") )
            return _default;
        try {
        return Double.valueOf(_number);
        } catch (NumberFormatException nfe) {
          return _default;
        }
    }
    
    public MetadataRow(Node _node) 
    {
        this(null,null,null,null); 
        this.fromXMLNode(_node);
    }
    
    public MetadataRow(String _XML) 
    {
        this(null,null,null,null); 
        String xPathExpression = "MetadataRow";
        try 
        {
            Document doc = null;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(_XML)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            this.fromXMLNode((Node)xpath.evaluate(xPathExpression,doc,XPathConstants.NODE));
        } catch (XPathExpressionException e) {
          System.out.println("Error processing XML(" + _XML + ") Expression=" + xPathExpression);
        } catch (ParserConfigurationException pe) {
          System.out.println("ParserConfigurationException " + pe.toString());
        } catch (SAXException se) {
          System.out.println("SAXException " + se.toString());
        } catch (IOException ioe) { 
          System.out.println("IOException " + ioe.toString());
        }
    }

    private void fromXMLNode(Node _node) 
    {
        if ( _node == null || _node.getNodeName().equals("MetadataRow")==false) {
          System.out.println("Node is null or not MetadataRow");
          return;  // Should throw error
        }
        String xPathExpression = "";
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();  
            xPathExpression = "DimName/text()";
            this.dimName = (String)xpath.evaluate(xPathExpression,_node,XPathConstants.STRING);
            xPathExpression = "SdoLB/text()";
            this.sdoLB   = toDouble((String)xpath.evaluate(xPathExpression,_node,XPathConstants.STRING),0.0f);
            xPathExpression = "SdoUB/text()";
            this.sdoUB   = toDouble((String)xpath.evaluate(xPathExpression,_node,XPathConstants.STRING),1.0f);
            xPathExpression = "Tol/text()";
            this.sdoTol  = toDouble((String)xpath.evaluate(xPathExpression,_node,XPathConstants.STRING),0.05f);        
        } catch (XPathExpressionException e) {
            System.out.println("XPathExpressionException for Expression=" + xPathExpression);
        }
    }
    
    public void set(String _dimName, Double _sdoLB, Double _sdoUB, Double _sdoTol) 
    {
        this.dimName = _dimName;
        this.sdoLB   = _sdoLB;
        this.sdoUB   = _sdoUB;
        this.sdoTol  = _sdoTol;
    }

    public String toXML() {
      StringBuffer sBuffer = new StringBuffer(1000);
      sBuffer.append(String.format("<MetadataRow><DimName>%s</DimName><SdoLB>%s</SdoLB><SdoUB>%s</SdoUB><Tol>%s</Tol></MetadataRow>",
                     ((this.dimName=="") ? "" : this.dimName),getStr(this.sdoLB),getStr(this.sdoUB),getStr(this.sdoTol)));
      return sBuffer.toString();
    }
    
    public String toString() 
    {
        StringBuffer sBuffer = new StringBuffer(1000);
        if ( ( !Strings.isEmpty(this.dimName) ) && 
             (this.sdoLB != null) && 
             (this.sdoUB != null) && 
             (this.sdoTol != null)) {
            sBuffer.append("'" + ((this.dimName=="") ? "" : this.dimName) +
                           "', " + getStr(this.sdoLB) + 
                            ", " + getStr(this.sdoUB) + 
                            ", " + getStr(this.sdoTol));
             } else {
            sBuffer.append("?,?,?,?");
        }
        return (MetadataRow.geoRaptorPreferences.getPrefixWithMDSYS() 
                ? Constants.TAG_MDSYS_SDO_ELEMENT
                : Constants.TAG_SDO_ELEMENT ) + 
               "(" + sBuffer.toString() + ")";
    }

    public void setDimName(String _dimName) {
        this.dimName = _dimName;
    }

    public String getDimName()  {
        return this.dimName;
    }
        
    public void setSdoLB(Double _sdoLB) {
        this.sdoLB = _sdoLB;
    }

    public Double getSdoLB() {
        return this.sdoLB;
    }

    public void setSdoUB(Double _sdoUB) {
        this.sdoUB = _sdoUB;
    }

    public Double getSdoUB() {
        return this.sdoUB;
    }

    public void setTol(Double _sdoTol) {
        this.sdoTol = _sdoTol;
    }

    public Double getTol() {
        return this.sdoTol;
    }

    public String getStr(double _d) {
        return this.decFormat.format(_d);
    }

}
