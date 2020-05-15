package org.GeoRaptor.OracleSpatial.Metadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.GeoRaptor.Constants;
import org.GeoRaptor.tools.Strings;
import org.geotools.util.logging.Logger;

import oracle.ide.dialogs.ProgressBar;

/**
 * Thread for calculating layer MBR
 */
public class RetrieveDimElementThread implements Runnable {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.OracleSpatial.Metadata.RetrieveDimElementThread");

    protected ProgressBar progressBar;
    protected Connection dbConn;
    
    /**
     * Operation error message if occures
     */
    protected String errorMessage;
    
    /**
     * MBR coordinates are placed in a MetadataEntry structure
     */
    protected MetadataEntry me;
    
    /**
     * Place to save SRID etc
     * What is the SRID associated with MBR and is it geographic or not
     */
    protected String       mbrSRID;
    protected int       sampleSize;
    protected boolean isGeographic;

    public RetrieveDimElementThread(Connection _dbConn, 
                                    String     _schema,
                                    String     _tableName, 
                                    String     _columnName,
                                    String     _srid,
                                    int        _sampleSize) 
    {
        this.dbConn = _dbConn;
        this.sampleSize = _sampleSize;
        me = new MetadataEntry( _schema, _tableName, _columnName, _srid ); 
    }

    public void setProgressBar(ProgressBar _progressBar) {
        this.progressBar = _progressBar;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    protected MetadataEntry getMetadataEntry() {
        return this.me;
    }
    
    /**
     * 
     * @author: Simon Greener, April 12th 2010
     *           Changed SQL and rSet handling to handle NULL sdo_geometry
     *           columns and tables more informatively and robustly.
     *           Also fixed SQL to generate bounds for X,Y,Z and M dimensions.
     * @author: Simon Greener, May 3rd 2010
     *           Improved error handling.
     * @author: Simon Greener, May 14th 2010
     *           Fixed but with getting correct SRID by filtering out any NULL sdo_geometys...
     */
    public void run() {        
        try  
        {
            String objectName =
                Strings.append(this.me.getSchemaName(),this.me.getObjectName(),Constants.TABLE_COLUMN_SEPARATOR);
            if ( this.sampleSize > 0 && this.sampleSize != 100 ) {
                objectName += " SAMPLE(" + String.valueOf(this.sampleSize) + ")";
            }
            // Column name/aliases in query
            String notNullGeomCount = "NOT_NULL_GEOM_COUNT";
            String nullGeomCount = "NULL_GEOM_COUNT";
            String xLB = "X_LB"; String xUB = "X_UB";
            String yLB = "Y_LB"; String yUB = "Y_UB";
            String zLB = "Z_LB"; String zUB = "Z_UB";
            String mLB = "M_LB"; String mUB = "M_UB";
            String isGeographic = "ISGEOGRAPHIC";
            String sSRID = "SRID";
            // If geometry is ST_ geometry we need to reference the internal sdo_geometry
            boolean isSTGeom = MetadataTool.isSTGeometry(dbConn,
                                                         getMetadataEntry().getSchemaName(),
                                                         getMetadataEntry().getObjectName(),
                                                         getMetadataEntry().getColumnName());
            String columnName = getMetadataEntry().getColumnName() + (isSTGeom ? ".GEOM" : "");
            String sql = 
                "With tSample As ( \n" + 
                " select n.rowid,\n" +
                "        sum(case when n." + columnName + " is null then 1 else 0 end) over (order by 1) as Null_Geom_Count,\n" + 
                "        n." + columnName + ".sdo_srid as srid,\n" + 
                "        case when n." + columnName + ".sdo_srid is null then 0 else \n" + 
                "        sum((select 1\n" + 
                "              from Mdsys.Geodetic_Srids GS\n" + 
                "             where n." + this.me.getColumnName() + " is not null\n" + 
                "               and Gs.Srid = n." + columnName + ".Sdo_Srid)) over (order by 1) end As isGeographic,\n" + 
                "        n." + columnName + " as geom \n" + 
                "   From " + objectName + " n \n" + 
                ") " +
               "Select Min(B.X) As " + xLB + ", \n" +
               "       Max(B.X) As " + xUB + ", \n" +
               "       Min(B.Y) As " + yLB + ", \n" +
               "       Max(B.Y) As " + yUB + ", \n" +
               "       Min(B.Z) As " + zLB + ", \n" +
               "       Max(B.Z) As " + zUB + ", \n" +
               "       Min(B.M) As " + mLB + ", \n" +
               "       Max(B.M) As " + mUB + ", \n" +
               "       Sum(A.Null_Geom_Count)    As " + nullGeomCount    + ", \n" +
               "       Count(Distinct B.Geom_Id) As " + notNullGeomCount + ", \n" + 
               "       Max(b.isGeographic)       AS " + isGeographic     + ", \n" +
               "       Max(b.srid)               As " + sSRID            + "\n" +
               "  From (select 1 as rid, \n" +
               "               Null_Geom_Count \n" +
               "          From tSample n \n" +
               "         Where n.null_geom_count = 0\n" + 
               "           and rownum < 2) A \n" +
               "       Left Outer Join /* Ensure we get a record if no rows or no geoms have values */ \n" +
               "       (Select 1 As Rid, \n" +
               "               T.Rowid As Geom_Id,\n" + 
               "               T.srid, \n" + 
               "               T.isGeographic,\n" +
               "               C.X, \n" +
               "               C.Y, \n" +
               "               Case When Trunc(t.geom.Sdo_Gtype/100) In (30,40,44) \n" +
               "                    Then C.Z Else Null End As Z, \n" +
               "               Case When Trunc(t.geom.Sdo_Gtype/100) In (33,43) \n" +
               "                    Then C.Z \n" +
               "                    When Trunc(t.geom.Sdo_Gtype/100) = 44 Then C.W \n" +
               "                    Else Null End As M \n" +
               "          From tSample T, \n" +
               "               Table( Mdsys.Sdo_Util.Getvertices(T.geom) ) C \n" +
               "         Where T.geom Is Not Null \n" +
               "        ) B \n" +
               "        On (A.Rid = B.Rid)";
            LOGGER.logSQL(sql);
            Statement st = this.dbConn.createStatement();
            ResultSet rSet = st.executeQuery(sql);
            Double min, max;
            if (rSet.next()) {
                if ( rSet.getInt(rSet.findColumn(notNullGeomCount)) != 0 ) {
                    this.isGeographic =
                            (rSet.getInt(rSet.findColumn(isGeographic)) == 1);
                    min = rSet.getDouble(rSet.findColumn(xLB)); min = rSet.wasNull() ? null : min; 
                    max = rSet.getDouble(rSet.findColumn(xUB)); max = rSet.wasNull() ? null : max;
                    me.add((this.isGeographic ? "Longitude" : "X"),min,max, (this.isGeographic ? 0.05 : 0.005));
                    min = rSet.getDouble(rSet.findColumn(yLB)); min = rSet.wasNull() ? null : min;
                    max = rSet.getDouble(rSet.findColumn(yUB)); max = rSet.wasNull() ? null : max;
                    me.add(( this.isGeographic ? "Latitude" : "Y"), min, max, (this.isGeographic ? 0.05 : 0.005));
                    min = rSet.getDouble(rSet.findColumn(zLB));
                    min = rSet.wasNull() ? null : min;
                    max = rSet.getDouble(rSet.findColumn(zUB));
                    max = rSet.wasNull() ? null : max;
                    if ( min != null ) me.add("Z", min, max, 0.005 );
                    min = rSet.getDouble(rSet.findColumn(mLB));
                    min = rSet.wasNull() ? null : min;
                    max = rSet.getDouble(rSet.findColumn(mUB));
                    max = rSet.wasNull() ? null : max;
                    if (min != null) me.add("M", min, max, 0.005);
                    this.mbrSRID = String.valueOf(rSet.getInt(rSet.findColumn(sSRID)));
                    if ( rSet.wasNull() ) this.mbrSRID = "NULL";
                    me.setSRID(this.mbrSRID);
                } else {
                    if ( rSet.getInt(rSet.findColumn(nullGeomCount)) == 0)
                        this.errorMessage = objectName + " contains no rows.\n";
                    else
                        this.errorMessage = objectName + " contains " + 
                                            rSet.getInt(rSet.findColumn(nullGeomCount)) + 
                                            " rows with all "+ this.me.getColumnName() + 
                                            "'s values being NULL.\n";
                    this.errorMessage = this.errorMessage + 
                                        "Enter bounds manually, and press Update.\n" +
                                        "Or get extent of another geometry column and use it to update " + this.me.getColumnName() + ".";
                }
            } else {
                /* this should never be called */
                this.errorMessage = "Table contains no rows";
            }
            rSet.close();
            st.close();
        } catch (SQLException sqle)  {
            this.errorMessage = sqle.getMessage();
        }
        // "shut down" progressbar
        this.progressBar.setDoneStatus();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    
}