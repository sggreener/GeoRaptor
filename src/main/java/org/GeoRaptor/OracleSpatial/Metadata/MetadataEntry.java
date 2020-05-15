package org.GeoRaptor.OracleSpatial.Metadata;

import java.io.IOException;
import java.io.StringReader;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.ARRAY;
import oracle.sql.Datum;
import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.tools.Strings;

import org.geotools.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


@SuppressWarnings("deprecation")
public class MetadataEntry 
{    

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry");

    protected Preferences geoRaptorPreferences;

    protected String schemaName = "";
    protected String objectName = "";
    protected String columnName = "";
    protected String       SRID = Constants.NULL;
    protected boolean    orphan = false;
    protected boolean   missing = false;
    
    private int intialCapacity = 1000;
    protected List<MetadataRow> rows;

    private final static String sDIM_ARRAY_COLOUR   = "#008411";
    private final static String sDIM_ELEMENT_COLOUR = "#670000";
    private final static String sDIM_NAME_COLOUR    = "#006784";
    private final static String sNULL_COLOUR        = "#003366";
    private final static String sDOUBLE_COLOUR      = "#0000ff";

    public MetadataEntry() {
        this(null,null,null,null);
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
    }

    public MetadataEntry(String _schemaName, 
                         String _objectName,
                         String _columnName, 
                         String _SRID) 
    {
        this.schemaName = _schemaName;
        this.objectName = _objectName;
        this.columnName = _columnName;
        this.SRID       = _SRID;
        this.orphan     = false;
        this.missing    = false;        
        this.rows       = new ArrayList<MetadataRow>(this.intialCapacity);
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
    }

    public MetadataEntry(MetadataEntry _me) 
    {
        this(null,null,null,null);
        if ( _me != null ) {
          this.schemaName = _me.getSchemaName();
          this.objectName = _me.getObjectName();
          this.columnName = _me.getColumnName();
          this.SRID       = _me.getSRID();
          this.orphan     = _me.isOrphan();
          this.missing    = _me.isMissing();        
          this.rows       = new ArrayList<MetadataRow>(this.intialCapacity);
          for (int i=0; i<_me.getEntryCount();i++) {
              this.add(_me.getEntry(i).getDimName(),
                       _me.getEntry(i).getSdoLB(),
                       _me.getEntry(i).getSdoUB(),
                       _me.getEntry(i).getTol()); 
          }
        }
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
    }

    public MetadataEntry(Node _node) 
    {
        this(null,null,null,null); 
        this.fromXMLNode(_node);
        this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
    }

    public MetadataEntry(String _XML) 
    {
        this(null,null,null,null); 
        try 
        {
            Document doc = null;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(_XML)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            this.fromXMLNode((Node)xpath.evaluate("Metadata/MetadataEntry",doc,XPathConstants.NODE));
            this.geoRaptorPreferences = MainSettings.getInstance().getPreferences();
        } catch (XPathExpressionException xe) {
            System.out.println("XPathExpressionException " + xe.toString());
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
      if ( _node == null || _node.getNodeName().equals("MetadataEntry")==false) {
          System.out.println("Node is null or not MetadataEntry");
          return;  // Should throw error
      }
      try 
      {
          XPath xpath = XPathFactory.newInstance().newXPath();          
          this.schemaName = (String)xpath.evaluate("Schema/text()",_node,XPathConstants.STRING);
          this.objectName = (String)xpath.evaluate("Object/text()",_node,XPathConstants.STRING);
          this.columnName = (String)xpath.evaluate("Column/text()",_node,XPathConstants.STRING);
          this.SRID       = (String)xpath.evaluate("SRID/text()",  _node,XPathConstants.STRING);
          NodeList mRows = (NodeList) xpath.evaluate("MetadataRow",_node,XPathConstants.NODESET);
          for(int i = 1; i < ( mRows.getLength() + 1 ); i++) 
          {
              @SuppressWarnings("unused")
			  Node n = mRows.item(i-1).cloneNode(true);
              // Prune MetadataRow from XML and give it to constructor
              // 
              this.add(new MetadataRow(mRows.item(i-1)));
          }
      } catch (XPathExpressionException xe) {
         System.out.println("XPathExpressionException " + xe.toString());
      }      
    }
      
    public void setEntry(String _schemaName, 
                         String _objectName,
                         String _columnName, 
                         String _SRID) {
        this.schemaName = _schemaName;
        this.objectName = _objectName;
        this.columnName = _columnName;
        this.setSRID(_SRID);
        this.orphan     = false;
        this.missing    = false;        

    }

    public boolean isNull() {
        return this.schemaName == null
               &&
               this.objectName == null 
               &&
               this.columnName == null;
    }

    public boolean isValid() {
        return ! ( this.orphan || this.missing );
    }

    public boolean isOrphan() {
        return this.orphan;
    }
    
    public void setOrphan(boolean _orphan) {
        this.orphan = _orphan;
    }
    
    public boolean isMissing() {
        return this.missing;
    }
    
    public void setMissing(boolean _missing) {
        this.missing = _missing;
    }
    
    public int getSRIDAsInteger() {
        return this.getSRID().equals(Constants.NULL) ? Constants.SRID_NULL :
               Integer.valueOf(this.getSRID()).intValue();
    }

    public String getSRID() {
        return Strings.isEmpty(this.SRID) || this.SRID.equalsIgnoreCase(Constants.NULL) ? Constants.NULL : this.SRID;
    }
    
    public void setSRID(String _SRID) {
        this.SRID = Strings.isEmpty(_SRID) ? Constants.NULL : _SRID;
    }

    public void setSRID(int _SRID) {
        this.SRID = _SRID == Constants.SRID_NULL ? Constants.NULL : String.valueOf(_SRID);
    }

    public void setColumnName(String _columnName) {
        this.columnName = _columnName;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public String getObjectName() {
        return this.objectName;
    }
    public void setObjectName(String _objectName) {
        this.objectName = _objectName;
    }

    public String getSchemaName() {
        return this.schemaName;
    }
    public void setSchemaName(String _schemaName) {
        this.schemaName = _schemaName;
    }

    public String getFullName() {
        return Strings.objectString(this.schemaName, this.objectName, this.columnName);
    }

    public String getFullObjectName() {
       return Strings.append(this.schemaName, this.objectName, ".");
    }

    public int getEntryCount() {
        return this.rows.size();
    }

    public MetadataRow getEntry(int _i) {
        return this.rows.get(_i);
    }

   public List<MetadataRow> getEntries() {
        return this.rows;
    }

    public void add(String _dimName, Double _sdoLB, Double _sdoUB, Double _sdoTol) 
    {
        MetadataRow mr = new MetadataRow(_dimName, _sdoLB, _sdoUB, _sdoTol);
        this.rows.add(mr);
    }

    public void add(ARRAY _dimArray)
    throws SQLException
    {
        if ( _dimArray == null ) {
            return;
        }
        ARRAY dimArray =  _dimArray;
        if ( dimArray.getDescriptor().getSQLName().getName().equals(Constants.TAG_MDSYS_SDO_DIMARRAY) ) 
        {
            String DIM_NAME = "";
            double SDO_LB   = Double.MAX_VALUE;
            double SDO_UB   = Double.MAX_VALUE;
            double SDO_TOL  = Double.MAX_VALUE;
            Datum[]    objs = dimArray.getOracleArray();
            for (int i =0; i < objs.length; i++) {
                STRUCT dimElement = (STRUCT)objs[i];
                Datum data[] = dimElement.getOracleAttributes();
                DIM_NAME = data[0].stringValue();
                SDO_LB   = data[1].doubleValue();
                SDO_UB   = data[2].doubleValue();
                SDO_TOL  = data[3].doubleValue();
                MetadataRow mr = new MetadataRow(DIM_NAME,SDO_LB,SDO_UB,SDO_TOL);
                this.rows.add(mr);
            }
        }
    }

    public void add(Envelope _mbr) 
    {
        if (_mbr == null || _mbr.isNull() ) { return; }
        this.rows.add(new MetadataRow("X",_mbr.getMinX(),_mbr.getMaxX(),0.005));
        this.rows.add(new MetadataRow("Y",_mbr.getMinY(),_mbr.getMaxY(),0.005));
    }
    
    public void add(MetadataRow _mr) 
    {
        this.rows.add(_mr);
    }

    public void remove(MetadataRow _mr) 
    {
        if ( _mr == null ) {
            return;
        }
        for (int i=0; i<this.rows.size();i++) {
            if ( this.rows.get(i).dimName.equalsIgnoreCase(_mr.getDimName())) {
                this.rows.remove(i);
            }
        }
    }

    public void remove(int _mr) 
    {
        if ( _mr == -1 || _mr >= this.rows.size() ) {
            return;
        }
        this.rows.remove(_mr);
    }

    public void removeAll() 
    {
        if ( ( this.rows == null ) || ( this.rows.size() == 0 ) ) {
            return;
        }
        this.rows.removeAll(new ArrayList<MetadataRow>(this.rows));
    }
    
    /**
     * @method set
     * @param _dimName
     * @param _sdoLB
     * @param _sdoUB
     * @param _sdoTol
     * @author Simon Greener, 28th May, 2010. Original coding
     *          Needed ability to update existing metadata row for an entry for 
     *          shapefile loading of a set of shapefiles.
     */
    public int set(String _dimName, Double _sdoLB, Double _sdoUB, Double _sdoTol) 
    {
        int rowsAdded = 0;
        // Does _dimName already exist?
        //
        MetadataRow mr = findRow(_dimName);
        if ( mr != null ) {
            // Update its values
            //
            mr.setSdoLB( _sdoLB  < mr.getSdoLB() ? _sdoLB  : mr.getSdoLB() );
            mr.setSdoUB( _sdoUB  > mr.getSdoUB() ? _sdoUB  : mr.getSdoUB() );
            mr.setTol(   _sdoTol < mr.getTol()   ? _sdoTol : mr.getTol() );
        } else {
            this.add(_dimName, _sdoLB, _sdoUB, _sdoTol);
            rowsAdded++;
        }
        return rowsAdded;
    }

    public int set(MetadataRow _mr) 
    {
        int rowsAdded = 0;
        // Does _dimName already exist?
        //
        if ( _mr == null ) {
            return rowsAdded;
        }
        MetadataRow mr = findRow(_mr.getDimName());
        if ( mr != null ) {
            // Update its values
            //
            mr.setSdoLB( _mr.getSdoLB()  < _mr.getSdoLB() ? _mr.getSdoLB()  : mr.getSdoLB() );
            mr.setSdoUB( _mr.getSdoUB()  >  mr.getSdoUB() ? _mr.getSdoUB()  : mr.getSdoUB() );
            mr.setTol(   _mr.getTol()    <  mr.getTol()   ? _mr.getTol()    : mr.getTol() );
        } else {
            this.add(_mr);
            rowsAdded++;
        }
        return rowsAdded;
    }

    public void set(int _row, MetadataRow _mr) 
    {
        if ( ( _row == -1 ) || ( _row >= this.rows.size() ) || ( _mr == null ) ) {
            return ;
        }
        // Update its values
        //
        this.rows.get(_row).setDimName(_mr.getDimName() );
        this.rows.get(_row).setSdoLB( _mr.getSdoLB()  < _mr.getSdoLB() ? _mr.getSdoLB()  : this.rows.get(_row).getSdoLB() );
        this.rows.get(_row).setSdoUB( _mr.getSdoUB()  >  this.rows.get(_row).getSdoUB() ? _mr.getSdoUB()  : this.rows.get(_row).getSdoUB() );
        this.rows.get(_row).setTol(   _mr.getTol()    <  this.rows.get(_row).getTol()   ? _mr.getTol()    : this.rows.get(_row).getTol() );
    }

    public MetadataRow findRow(String _dimName) 
    {
        for (int i=0; i<this.rows.size();i++) {
            if ( this.rows.get(i).dimName.equalsIgnoreCase(_dimName)) {
                return this.rows.get(i);
            }
        }
        return null;
    }

    public MetadataEntry copy() 
    {
      MetadataEntry mEntryCopy = new MetadataEntry(this.schemaName,this.objectName,this.columnName,this.SRID);
      for (int i=0; i<this.rows.size();i++) {
          mEntryCopy.add(this.rows.get(i).getDimName(), 
                         this.rows.get(i).getSdoLB(),
                         this.rows.get(i).getSdoUB(),
                         this.rows.get(i).getTol()); 
      }
      return mEntryCopy;
    }
    
    /**
     * @method getMBR
     * @precis gets MBR of X and Y sdoLBs
     * @note We have to assume first and second MetadataRow entries are X and Y as we don't want to have to interpret Internationalisation strings
     **/
    public Envelope getMBR() 
    {
        if ( this.rows.size() == 0 ) {
            return new Envelope(Constants.MAX_PRECISION);
        }
        MetadataRow mrX = this.rows.get(0);
        MetadataRow mrY = this.rows.get(1);
        return new Envelope(mrX.getSdoLB(),mrY.getSdoLB(),mrX.getSdoUB(),mrY.getSdoUB());
    }

    public JGeometry getJGeometry() 
    {
        if ( this.rows.size() == 0 ) {
            return null;
        }
        MetadataRow mrX = this.rows.get(0);
        MetadataRow mrY = this.rows.get(1);
        return new JGeometry(mrX.getSdoLB(),mrY.getSdoLB(),mrX.getSdoUB(),mrY.getSdoUB(),this.getSRIDAsInteger());
    }

    public String toSdoGeometry() 
    {
        if ( this.rows.size() <= 1 ) {
            return null;
        }
        MetadataRow mrX = this.rows.get(0);
        MetadataRow mrY = this.rows.get(1);
        String sdo_gtype = "2003";
        MetadataRow mrZ = null;
        if ( this.rows.size() > 2 ) {
            mrZ = this.rows.get(2);
            sdo_gtype = "3003";
        }
        LOGGER.debug("MetadataEntry.toSdoGeometry(): sdo_gtype="+sdo_gtype+" getSRID="+this.getSRID());
        return "SDO_GEOMETRY(" + 
                             sdo_gtype + "," +
                             this.getSRID() + ",NULL,SDO_ELEM_INFO_ARRAY(1,1003,3),SDO_ORDINATE_ARRAY(" +
                             mrX.getSdoLB() + "," + mrY.getSdoLB() + (mrZ==null ? "": ","+mrZ.getSdoLB()) + "," +
                             mrX.getSdoUB() + "," + mrY.getSdoUB() + (mrZ==null ? "": ","+mrZ.getSdoUB()) +
                             "))";
    }

    public String toString() 
    {
        // (('X', 0, 1, .00005),('Y', 0, 1, .00005))
        StringBuffer sBuffer = new StringBuffer(1000);
        sBuffer.append(this.schemaName + "." + this.objectName + "." + this.columnName + ",");
        sBuffer.append(this.toDimArray());
        sBuffer.append("," + this.SRID);
        return sBuffer.toString();
    }

    public String updateSQL() 
    {
        return "UPDATE user_sdo_geom_metadata usgm\n" +
               "   SET usgm.diminfo = " + this.toDimArray() + "," + "\n" +
               "          usgm.srid = " + this.SRID + "\n" +
               " WHERE usgm.table_name  = '" + this.objectName.toUpperCase() + "' \n" +
               "   AND usgm.column_name = '" + this.columnName.toUpperCase() + "'";
    }

    public String deleteSQL() 
    {
        return "DELETE FROM user_sdo_geom_metadata usgm\n" +
               " WHERE usgm.table_name  = '" + this.objectName.toUpperCase() + "' \n" +
               "   AND usgm.column_name = '" + this.columnName.toUpperCase() + "'";
    }
    
    public String insertSQL(boolean upsert) 
    {
        if ( upsert ) {
            return "MERGE INTO User_Sdo_Geom_Metadata M \n" + 
                      "USING (SELECT'" + this.objectName.toUpperCase() + "' as table_name, \n" +
                      "             '" + this.columnName.toUpperCase() + "' as column_name \n" +
                      "         FROM dual \n" +
                      "       ) S \n" + 
                      "       On (     M.Table_Name  = S.Table_Name \n" + 
                      "            And M.Column_Name = S.Column_Name \n" + 
                      "           ) \n" +
                      "WHEN MATCHED THEN \n" +
                      "UPDATE SET m.srid  = " + this.SRID + ", \n" +
                      "           m.DimInfo = " + this.toDimArray() + " \n" +
                      "WHEN NOT MATCHED THEN \n" +
                      "INSERT ( m.table_name,m.column_name,m.DimInfo,m.srid ) \n" +
                      "VALUES ('" + this.objectName.toUpperCase() + "', \n" +
                      "        '" + this.columnName.toUpperCase() + "', \n" +
                      "        "  + this.toDimArray() + ", \n" +
                      "        "  + this.SRID + ")";
        } else {
            return "INSERT INTO USER_SDO_GEOM_METADATA (table_name,column_name,DimInfo,srid) \n" +
                    "VALUES ('" + this.objectName.toUpperCase() + "', \n" +
                    "        '" + this.columnName.toUpperCase() + "', \n" +
                    "        "  + this.toDimArray() + ", \n" +
                    "        "  + this.SRID + ")";
        }
    }
    
    public String toDimArray() 
    {
        StringBuffer sBuffer = new StringBuffer(1000);
        for (int i = 0; i < rows.size(); i++) {
            if (i != 0) {
                sBuffer.append(", ");
            }
            sBuffer.append(rows.get(i).toString());
        }
        return (this.geoRaptorPreferences.getPrefixWithMDSYS() 
                ? Constants.TAG_MDSYS_SDO_DIMARRAY 
                : Constants.TAG_SDO_DIM_ARRAY ) +
               "(" +
               sBuffer.toString() + ")";
    }

    public String toXML() 
    {
      StringBuffer sBuffer = new StringBuffer(1000);
      sBuffer.append(String.format("<MetadataEntry><Schema>%s</Schema><Object>%s</Object><Column>%s</Column><SRID>%s</SRID>",
                                   this.schemaName,
                                   this.objectName,
                                   this.columnName,
                                   this.SRID));
      for (int i = 0; i < rows.size(); i++) {
          sBuffer.append(rows.get(i).toXML());
      }
      return sBuffer.toString() + "</MetadataEntry>";
    }
    
    public String render() 
    {
        StringBuffer sBuffer      = new StringBuffer(1000);
        String DIM_NAME_COLOUR    = "<B><font color=\"" + MetadataEntry.sDIM_NAME_COLOUR  +  "\">";
        String DIM_ARRAY_COLOUR   = "<B><font color=\"" + MetadataEntry.sDIM_ARRAY_COLOUR + "\">";
        String DIM_ELEMENT_COLOUR = "<B><font color=\"" + MetadataEntry.sDIM_ELEMENT_COLOUR + "\">";
        String NULL               = "<B><font color=\"" + MetadataEntry.sNULL_COLOUR      + "\">NULL</font></B>";
        String DOUBLE_COLOUR      = "<B><font color=\"" + MetadataEntry.sDOUBLE_COLOUR    + "\">";
        String COLOUR_END         = "</font></B>";

        MetadataRow mr = null;
        
        // Render DIM_ARRAY
        sBuffer.append(DIM_ARRAY_COLOUR + 
                       (this.geoRaptorPreferences.getPrefixWithMDSYS() 
                       ? Constants.TAG_MDSYS_SDO_DIMARRAY 
                       : Constants.TAG_SDO_DIM_ARRAY ) + 
                       COLOUR_END + "(");
        for (int i = 0; i < rows.size(); i++) {
            if (i != 0)
                sBuffer.append(", ");
            mr = rows.get(i);
            sBuffer.append(DIM_ELEMENT_COLOUR + 
                           (this.geoRaptorPreferences.getPrefixWithMDSYS() 
                           ? Constants.TAG_MDSYS_SDO_ELEMENT  
                           : Constants.TAG_SDO_ELEMENT  ) + 
                           COLOUR_END + 
                           "(" +
                           (Strings.isEmpty(mr.getDimName())        ? NULL : "'" + DIM_NAME_COLOUR + mr.getDimName()          + "'" + COLOUR_END ) + "," + 
                           (Double.valueOf(mr.getSdoLB()).isNaN() ? NULL : DOUBLE_COLOUR + mr.getStr(mr.getSdoLB().doubleValue()) + COLOUR_END ) + "," + 
                           (Double.valueOf(mr.getSdoUB()).isNaN() ? NULL : DOUBLE_COLOUR + mr.getStr(mr.getSdoUB().doubleValue()) + COLOUR_END ) + "," + 
                           (Double.valueOf(mr.getTol()).isNaN()   ? NULL : DOUBLE_COLOUR + mr.getStr(  mr.getTol().doubleValue()) + COLOUR_END ) + 
                           ")");
        }
        return sBuffer.toString() + ")"; 
    }

    /**
     * @method  setXYTolerances
     * @precis  Sets all X,Y tolerances to the supplied one
     * @param   _tolerance
     * @note    First two MetadataRows (dim_elements) assumed to be X and Y
     * @author Simon Greener, June 30th 2010
     *          Original coding
     */
    public void setXYTolerances(double _tolerance) 
    {
        for ( int j = 0; j < 2; j++ )  
            this.rows.get(j).setTol(_tolerance);
    }

    /**
     * @method  setAllTolerances
     * @precis  Sets all X,Y,Z etc tolerances to the supplied one
     * @param   _tolerance
     * @author Simon Greener, June 30th 2010
     *          Original coding
     */
    public void setAllTolerances(double _tolerance) 
    {
        for ( int j = 0; j < this.rows.size(); j++ ) {
            if ( this.rows.get(j).getTol() == _tolerance )
                this.rows.get(j).setTol(_tolerance);
        }
    }

    /**
     * @method  replaceTolerances
     * @precis  Sets all X,Y,Z etc tolerances to the supplied one if equal to old
     * @param   _old
     * @param   _new
     * @author Simon Greener, June 30th 2010
     *          Original coding
     */
    public void replaceTolerances(double _old, double _new) 
    {
        for ( int j = 0; j < this.rows.size(); j++ ) {
            if ( this.rows.get(j).getTol() == _old ) {
                this.rows.get(j).setTol(_new);
            }
        }
    }

    public double getMaxTolerance(double _defaultTolerance) 
    {
        double tol = Double.MIN_VALUE;
        for ( int j = 0; j < this.rows.size(); j++ ) {
            tol = Math.max(this.rows.get(j).getTol(),tol);
        }
        if ( tol == Double.MIN_VALUE ) {
            tol = _defaultTolerance;
        }
        return tol;
    }

    public double getMinTolerance(double _defaultTolerance) 
    {
        double tol = Double.MAX_VALUE;
        for ( int j = 0; j < this.rows.size(); j++ ) {
            tol = Math.min(this.rows.get(j).getTol(),tol);
        }
        if ( tol == Double.MAX_VALUE ) {
            tol = _defaultTolerance;
        }
        return tol;
    }
    
    /**
     * Check MBR coordinates tolerance, get the one with minimum one and calculate number of fraction digits
     */
    public int getMaxFractionDigits(int _defaultValue) 
    {
        double minTol = this.getMinTolerance(Double.NaN);
        if ( Double.isNaN(minTol) ) {
            return _defaultValue;
        } else {
            return (int)Math.ceil(Math.log10(1.0/minTol));
        }
    }
}
