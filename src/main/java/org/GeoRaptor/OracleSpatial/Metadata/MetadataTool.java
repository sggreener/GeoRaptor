package org.GeoRaptor.OracleSpatial.Metadata;

import java.awt.geom.Point2D;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JOptionPane;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleStatement;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.ARRAY;
import oracle.sql.STRUCT;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;

import org.geotools.util.logging.Logger;


/**
 * @author Matic
 * @version 1.1
 * @author Simon Greener, April 4th 2010
 *          Implemented keysAndColumns()
 **/

@SuppressWarnings("deprecation")
public class MetadataTool {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.OracleSpatial.Metadata.MetadataTool");

    /**
     * Reference to resource manager for accesing messages in properties file
     */
    protected static PropertiesManager propertyManager;
    private static final String propertiesFile = "org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel";
    
    public MetadataTool() {
    }

    /**
     * @function keysAndColumns
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @return String
     * @author Simon Greener, May 4th 2010
     *          - retrieves table keys
     **/
    public static List<String> keysAndColumns(Connection _conn,
                                                  String _schemaName,
                                                  String _objectName)
         throws SQLException,
                IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","USER_SDO_GEOM_METADATA SQL"));
        
        if (Strings.isEmpty(_objectName) )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        
        // This SQL will work in 9i, 10g and 11g.
        // It returns names of indexes that are primary key or unique constraints
        // as both are valid for unique row access
        // The SQL aggregates the columns in a multi-column key in column_postion order.
        // The SQL orders so that the first returned key is one with the smallest number of columns in the key
        //
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String aliasCol = "KeyColumns";
        String sql = 
        "Select owner, index_name, table_name, constraint_type, number_of_columns, " + aliasCol + " \n" +
        "  from ( \n" +
        "Select A.Table_Owner as owner, A.Index_Name, a.Table_Name, A.Constraint_Type, \n" +
        "       Max(A.Column_Position) Number_Of_Columns, \n " +
        "       Ltrim(Max(Sys_Connect_By_Path(A.Column_Name,',')) \n " +
        "       Keep (Dense_Rank Last Order By A.Curr),',') As " + aliasCol + " \n" +
        "  From (Select Aic.Table_Owner, aic.Index_Name, aic.Table_Name, ac.constraint_type, aic.Column_Name, aic.Column_Position, \n" +
        "               Row_Number() Over (Partition By Aic.Table_Owner, aic.Index_Name,aic.Table_Name,ac.Constraint_Type Order By aic.Column_Position) As Curr, \n" +
        "               ROW_NUMBER() OVER (PARTITION BY Aic.Table_Owner, aic.index_name,aic.table_name,ac.constraint_type ORDER BY aic.column_position) -1 As Prev \n" +
        "          From all_Ind_Columns aic \n" +
        "               Inner Join \n" +
        "               all_Constraints ac \n" +
        "               On (     ac.Table_Name      = aic.Table_Name \n" +
        "                    And ac.Constraint_Name = aic.Index_Name ) \n" +
        "         where Aic.Table_Owner = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
        "           and aic.Table_Owner = ac.Owner \n " +
        "           and aic.Table_Name  = ? \n" +
        "           and ac.Constraint_Type In ('P','U') \n" +
        "       ) A \n" +
        "Group By A.Table_Owner, A.Index_Name, A.Table_Name, A.Constraint_Type \n" +
        "Connect By A.Prev        = Prior A.Curr \n" +
        "       And A.Table_Owner = Prior A.Table_Owner \n" +
        "       And A.Index_Name  = Prior A.Index_Name \n" +
        "       And A.Table_Name  = Prior A.Table_Name \n" +
        "       And A.Constraint_Type = Prior A.Constraint_Type \n" +
        "Start With A.Curr = 1 \n" +
        "order by 5 \n" +
        ")";
        List<String> keyColumns = new ArrayList<String>();

        PreparedStatement psGeomColumn = _conn.prepareStatement(sql);
        psGeomColumn.setString(1,schema);
        psGeomColumn.setString(2,_objectName.toUpperCase());
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase());
        
        ResultSet geomColumnRSet = psGeomColumn.executeQuery();
        while (geomColumnRSet.next()) {
            // Add columns in key to return variable
            keyColumns.add(geomColumnRSet.getString(geomColumnRSet.findColumn(aliasCol)));
        }
        geomColumnRSet.close();
        geomColumnRSet = null;
        psGeomColumn.close();
        psGeomColumn = null;

        return keyColumns;
    }
    
    /** 
     * @function validateColumnName
     * @precis   Checks that _columnName exists in _schemaName._objectName and 
     *           is SDO_GEOMETRY. If no _columnName is provided, returns list 
     *           of SDO_GEOMETRY columns in object.
     * @author   Simon Greener, May 1st 2010 Original Coding
     **/
    public static List<String> validateColumnName (Connection _conn,
                                                   String     _schemaName,
                                                   String     _objectName,
                                                   String     _columnName)
    {
        List<String> geoColumns = new ArrayList<String>();

        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if (Strings.isEmpty(_columnName)) {
            // Get all Geometry Columns for this schema.table
            try {
                geoColumns = getGeoColumns(_conn, 
                                           _schemaName, 
                                           _objectName);
            } catch (SQLException sqle) {
                LOGGER.error(sqle.toString());
                JOptionPane.showMessageDialog(null,
                                              propertyManager.getMsg("MD_TABLE_NO_SDO_GEOMETRY_COLUMN",
                                                                     Strings.append(_schemaName,
                                                                                  _objectName,
                                                                                  Constants.TABLE_COLUMN_SEPARATOR)),
                                              MainSettings.EXTENSION_NAME, 
                                              JOptionPane.INFORMATION_MESSAGE);
                return (List<String>)null;
            } catch (IllegalArgumentException iae ) {
                LOGGER.error(iae.toString());
                JOptionPane.showMessageDialog(null,
                                              iae.getLocalizedMessage(),
                                              MainSettings.EXTENSION_NAME,
                                              JOptionPane.ERROR_MESSAGE);
                return (List<String>)null;
            }
        } else { 
            // Check if supplied column is sdo_geometry
            //
            try 
            {
                geoColumns.add(getGeometryColumn(_conn, 
                                                 _schemaName, 
                                                 _objectName, 
                                                 _columnName));
                if (!geoColumns.get(0).toString().equalsIgnoreCase(_columnName)) {
                    // Pop up dialog box...
                    JOptionPane.showMessageDialog(null,
                                                  propertyManager.getMsg("MD_COLUMN_NOT_SDO_GEOMETRY",
                                                                         _columnName),
                                                  MainSettings.EXTENSION_NAME, 
                                                  JOptionPane.INFORMATION_MESSAGE);
                    return (List<String>)null;
                }
            } catch (SQLException sqle) {
                JOptionPane.showMessageDialog(null,
                                              propertyManager.getMsg("MSI_GEO_COLUMN_RETRIEVAL_ERROR",
                                                          sqle.getMessage(),
                                                                     Strings.append(_schemaName, 
                                                                              _objectName, 
                                                                              Constants.TABLE_COLUMN_SEPARATOR)),
                                              MainSettings.EXTENSION_NAME, 
                                              JOptionPane.INFORMATION_MESSAGE);
                return (List<String>)null;
            }
        }
        return geoColumns;
    }
    
    public static String getColumns(Connection _conn,
                                    String     _schemaName,
                                    String     _objectName,
                                    boolean    _supportedDataTypes) 
      throws IllegalArgumentException,
             SQLException
    {
      if ( propertyManager == null ) 
          propertyManager = new PropertiesManager(propertiesFile);
      
      if ( _conn==null )
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","USER_SDO_GEOM_METADATA SQL"));
      
      if (Strings.isEmpty(_objectName) ){
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                             propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
      }
      
      String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
      String columns = "";
      String sql = 
          "SELECT LTRIM(MAX(SYS_CONNECT_BY_PATH(f.column_name,',')) KEEP (DENSE_RANK LAST ORDER BY f.curr),',') AS columns\n" + 
          "  FROM (SELECT atc.table_name,'\"'||atc.column_name||'\"' as column_name,\n" + 
          "               ROW_NUMBER() OVER (PARTITION BY atc.table_name ORDER BY atc.column_id) AS curr, \n" + 
          "               ROW_NUMBER() OVER (PARTITION BY atc.table_name ORDER BY atc.column_id) -1 AS prev \n" + 
          "          FROM all_tab_columns atc \n" +
          "         WHERE atc.owner      = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" + 
          "           AND atc.table_name = ? \n" +
          "           AND NOT (   atc.data_type not like 'ST_ANNOT%' AND \n" +
          "                     ( atc.data_type = 'SDO_GEOMETRY' OR atc.data_type LIKE 'ST!_%' escape '!' ) ) \n" +
          ( _supportedDataTypes 
          ? 
          "           AND (    atc.data_type in ('DATE','CHAR','NVARCHAR2','VARCHAR','VARCHAR2','CLOB','ROWID','FLOAT','BINARY_DOUBLE','BINARY_FLOAT','NUMBER')\n" +
          "                 OR atc.data_type like 'TIMESTAMP%' \n" +
          "                 OR atc.data_type like 'INTERVAL%' )\n"  
          : "" ) +
          "         ORDER BY atc.COLUMN_ID \n" +
          "       ) f \n" +
          "  GROUP BY f.table_name\n" + 
          "CONNECT BY f.prev = PRIOR f.curr AND f.table_name = PRIOR f.table_name\n" + 
          "  START WITH curr = 1\n";
      
      /* Get Schema, table, column and associated metadata */
      PreparedStatement psColumns = _conn.prepareStatement(sql);
      psColumns.setString(1,schema);
      psColumns.setString(2,_objectName.toUpperCase());
      LOGGER.logSQL(sql + 
                    "\n? = " + schema +
                    "\n? = " + _objectName.toUpperCase());

      psColumns.setFetchSize(100); // default is 10
      psColumns.setFetchDirection(ResultSet.FETCH_FORWARD);
      ResultSet entriesRSet = psColumns.executeQuery();
      if (entriesRSet.next()) {
          columns = entriesRSet.getString(1);
      } 
      entriesRSet.close();
      entriesRSet = null;
      return columns;
    }

    public static LinkedHashMap<String, String> getColumnsAndTypes(Connection _conn,
                                                                   String     _schemaName,
                                                                   String     _objectName,
                                                                   boolean    _supportedDataTypes,
                                                                   boolean    _fullDataType) 
      throws IllegalArgumentException,
             SQLException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }
      
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","USER_SDO_GEOM_METADATA SQL"));
        }
        
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        
        LinkedHashMap<String, String> columnTypes = new LinkedHashMap<String, String>();
        String schemaClause = ""; 
        if (Strings.isEmpty(_schemaName) ) {
            schemaClause = "( atc.owner is not null ) \n";
        } else {
            schemaClause = " atc.owner = ? \n";
        }
        
        String sql = 
          "SELECT '\"' || atc.column_name || '\"' as column_name, atc.data_type, \n" +
          "       case when data_precision is null \n" +
          "            then data_type || \n" +
          "                 DECODE(data_type, \n" +
          "                        'NVARCHAR2','(' || data_length || ')', \n" +
          "                        'VARCHAR2', '(' || data_length || ')', \n" +
          "                        'CHAR',     '(' || data_length || ')','') \n" +
          "            when data_scale is null OR data_scale = 0 then data_type || '(' || data_precision || ')' \n" +
          "            else data_type || '(' || data_precision || ',' || data_scale || ')' \n" +
          "        end as full_data_type \n" +
          "  FROM ALL_TAB_COLUMNS atc \n" +
          " WHERE " + schemaClause + 
          "   AND atc.table_name = ?\n" +
          ( _supportedDataTypes 
          ? 
          "   AND (      atc.data_type in ('DATE','CHAR','NVARCHAR2','VARCHAR','VARCHAR2','CLOB','ROWID','FLOAT','BINARY_DOUBLE','BINARY_FLOAT','NUMBER')\n" +
          "         OR ( atc.data_type not like 'ST_ANNOT%' \n" +
          "          AND ( atc.data_type = 'SDO_GEOMETRY' OR atc.data_type LIKE 'ST!_%' escape '!' ) ) \n" +
          "         OR atc.data_type like 'TIMESTAMP%'\n" +
          "         OR atc.data_type like 'INTERVAL%' )\n" 
          : "" ) +
          "   AND EXISTS (SELECT 1 \n" +
          "                 FROM all_tab_columns atc1 \n" +
          "                WHERE atc1.owner      = atc.owner \n" +
          "                  AND atc1.table_name = atc.table_name ) \n" +
          " ORDER BY atc.column_id";

        /* Get Schema, table, column and associated metadata */
        PreparedStatement psMEntries = _conn.prepareStatement(sql);
        if (Strings.isEmpty(_schemaName) ) {
            psMEntries.setString(1, _objectName.toUpperCase());
            LOGGER.logSQL(sql + "\n? = " + _objectName.toUpperCase());
            
        } else {
            psMEntries.setString(1, _schemaName.toUpperCase());
            psMEntries.setString(2, _objectName.toUpperCase());
            LOGGER.logSQL(sql + 
                            "\n? = " + _schemaName +
                            "\n? = " + _objectName.toUpperCase());
        }
        psMEntries.setFetchSize(100); // default is 10
        psMEntries.setFetchDirection(ResultSet.FETCH_FORWARD);
        ResultSet entriesRSet = psMEntries.executeQuery();
        String columnName = "";
        String dataType = "";
        while (entriesRSet.next()) {
            columnName = entriesRSet.getString(1);
            dataType   = _fullDataType ? entriesRSet.getString(3) : entriesRSet.getString(2);
            columnTypes.put(columnName,dataType);
        } 
        entriesRSet.close();
        entriesRSet = null;
        return new LinkedHashMap<String, String>(columnTypes);
    }
    
    /**
     * @function getGeometryColumn
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @return String
     * @author Simon Greener, April 20th 2010
     *          Refactored to be more commoditised
     */
    public static String getGeometryColumn(Connection _conn,
                                           String     _schemaName, 
                                           String     _objectName, 
                                           String     _columnName) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","SDO_GEOMETRY column_name SQL"));
        }
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String columnName = Strings.isEmpty(_columnName) ? ""     : _columnName.toUpperCase();
        String columnClause = (Strings.isEmpty(_columnName)
                              ? ""
                              : "and atc.COLUMN_NAME = ? \n");
        String sql = "select atc.COLUMN_NAME \n" +
                       "from ALL_TAB_COLUMNS atc \n" +
                     " where atc.owner      = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
                     "   and atc.table_name = ? \n" + 
                     columnClause +
                     "   and (atc.data_type not like 'ST_ANNOT%' \n" +
                     "   and ( atc.data_type = 'SDO_GEOMETRY' or atc.data_type LIKE 'ST!_%' escape '!' ) ) \n" +
                      "  and rownum < 2 \n" +
                      "order by atc.COLUMN_ID";
        
        PreparedStatement psGeomColumn = _conn.prepareStatement(sql);
        psGeomColumn.setString(1,schema);
        psGeomColumn.setString(2,_objectName.toUpperCase() );
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase());

        if ( columnClause.contains("?") ) {
           psGeomColumn.setString(3, columnName.replace("\"","") );
        }
        ResultSet geomColumnRSet = psGeomColumn.executeQuery();
        if (geomColumnRSet.next()) {
            columnName = geomColumnRSet.getString(1);
            geomColumnRSet.close();
            psGeomColumn.close(); 
        }
        geomColumnRSet.close();
        geomColumnRSet = null;
        psGeomColumn.close();
        psGeomColumn = null;
        
        return columnName;
    }

    public static boolean isSTGeometry(Connection    _conn,
                                       MetadataEntry _mEntry) 
    throws SQLException,
           IllegalArgumentException
    {
        if (_mEntry==null) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_METADATA_ENTRY"));
        }
        return isSTGeometry(_conn,_mEntry.getSchemaName(),_mEntry.getObjectName(),_mEntry.getColumnName());
    }
    /**
     * @function isSTGeometry
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @return boolean
     * @author Simon Greener, April 20th 2013
     */
    public static boolean isSTGeometry(Connection _conn,
                                       String     _schemaName, 
                                       String     _objectName, 
                                       String     _columnName) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR",
                                                                      "GEOMETRY check if ST_Geometry SQL"));
        }
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }

        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String columnClause = (Strings.isEmpty(_columnName)
                              ? ""
                              : "and atc.COLUMN_NAME = ? \n");
        String sql = "select case when atc.data_type = 'SDO_GEOMETRY' then 0 else 1 end \n" +
                       "from ALL_TAB_COLUMNS atc \n" +
                     " where atc.owner      = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
                     "   and atc.table_name = ? \n" + 
                     columnClause +
                     "   and ( atc.data_type not like 'ST_ANNOT%' \n" +
                     "   and ( atc.data_type = 'SDO_GEOMETRY' or atc.data_type LIKE 'ST!_%' escape '!' ) ) \n" +
                      "  and rownum < 2 \n" +
                      "order by atc.COLUMN_ID";
        
        PreparedStatement psGeomColumn = _conn.prepareStatement(sql);
        psGeomColumn.setString(1,schema);
        psGeomColumn.setString(2,_objectName.toUpperCase() );
        String logSQL = sql + 
                        "\n? = " + schema +
                        "\n? = " + _objectName;
        if ( columnClause.contains("?") ) {
           psGeomColumn.setString(3, _columnName.replace("\"","").toUpperCase() );
           logSQL += "\n? = " + _columnName.toUpperCase();
        }
        LOGGER.logSQL(logSQL);
        boolean isST = false;
        ResultSet geomColumnRSet = psGeomColumn.executeQuery();
        if (geomColumnRSet.next()) {
            int isSTGeometry = geomColumnRSet.getInt(1);
            if ( ! geomColumnRSet.wasNull() ) {
                isST = isSTGeometry==1 ? true : false;
            }
            geomColumnRSet.close();
            psGeomColumn.close(); 
        }
        geomColumnRSet.close();
        geomColumnRSet = null;
        psGeomColumn.close();
        psGeomColumn = null;
        return isST;
    }
    
    public static List<String> getTablespaces(Connection _conn)
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","TABLESPACE SQL"));

        List<String> tableSpaces = new ArrayList<String>();
        String sql = "select ut.tablespace_name \n" +
                     "  From User_Tablespaces ut \n" +
                     " Where ut.Contents = 'PERMANENT' \n" +
                     "   And ut.Logging = 'LOGGING' \n" +
                     "   And ut.Tablespace_Name Not In ('SYSTEM','SYSAUX')";

        LOGGER.logSQL(sql);
        
        Statement st = _conn.createStatement();
        ResultSet rSet = st.executeQuery(sql);
        while (rSet.next()) 
            tableSpaces.add(rSet.getString(1));
        rSet.close(); rSet = null;
        st.close(); st = null;

        return tableSpaces;
    }
        
    public static List<String> getGeoColumns(Connection _conn,
                                             String     _schemaName, 
                                             String     _objectName)
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","SDO_GEOMETRY column_name SQL"));
        
        if (Strings.isEmpty(_objectName) ) 
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        List<String> geoColumns = new ArrayList<String>();
        String sql = "select atc.COLUMN_NAME \n" +
                     "  from ALL_TAB_COLUMNS atc \n" +
                     " where atc.owner      = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
                     "   and atc.table_name = ? \n" + 
                     "   and atc.data_type not like 'ST_ANNOT%' \n" + 
                     "   and ( atc.data_type = 'SDO_GEOMETRY' \n" + 
                     "      or atc.data_type LIKE 'ST!_%' escape '!' )\n" +
                     " order by atc.COLUMN_ID";
        
        PreparedStatement psGeomColumns = _conn.prepareStatement(sql);
        psGeomColumns.setString(1,schema);
        psGeomColumns.setString(2,_objectName.toUpperCase());
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase());

        ResultSet geomColumnsRSet = psGeomColumns.executeQuery();
        while (geomColumnsRSet.next()) {
            geoColumns.add(geomColumnsRSet.getString(1));
        }
        geomColumnsRSet.close();
        geomColumnsRSet = null;
        psGeomColumns.close();
        psGeomColumns = null;

        return geoColumns;
    }

    /**
     * @function hasGeomMetadataEntry
     * @precis Given parameters finds if a metadata record exists
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @return boolean
     * @author Simon Greener, April 20th 2010, Original Coding
     */
    public static boolean hasGeomMetadataEntry( Connection _conn,
                                                String _schemaName, 
                                                String _objectName, 
                                                String _columnName) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) { 
            propertyManager = new PropertiesManager(propertiesFile);
        }
        
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR",
                                                                      "USER_SDO_GEOM_METADATA SQL"));
        }
        
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }

        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        boolean mDataExists = false;
        String columnClause = (Strings.isEmpty(_columnName)
                              ? ""
                              : "and asgm.COLUMN_NAME = ? \n");
        String sql = 
             "select asgm.column_name \n" +
             "  from all_sdo_geom_metadata asgm \n" +
             " where asgm.owner       = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
             "   and asgm.table_name  = ? \n" + 
                 columnClause +
             "   and EXISTS(select 1 \n" +
             "                from all_tab_columns atc \t" +
             "               where atc.owner       = asgm.owner \n" +
             "                 and atc.table_name  = asgm.table_name \n" +
             "                 and atc.column_name = asgm.column_name ) \n" +
              "order by asgm.table_name, asgm.column_name";
        
        PreparedStatement psTable = _conn.prepareStatement(sql);
        psTable.setString(1,schema);
        psTable.setString(2,_objectName.toUpperCase());
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase());
        
        if ( columnClause.contains("?") ) {
           psTable.setString(3, _columnName.toUpperCase() );
        }
        ResultSet rSet = psTable.executeQuery();
        mDataExists = rSet.next(); 
        rSet.close();
        psTable.close();
      
        return mDataExists;
    }
    
    /**
     * @function getObjectType
     * @precis   sdo_geometry can be in a TABLE, VIEW or MATERIALISED VIEW
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @return VIEW, TABLE or MATERIALIZED VIEW
     * @throws SQLException
     * @author Simon Greener, April 21st 2010, Original coding
     */
    public static String getObjectType(Connection _conn,
                                       String     _schemaName, 
                                       String     _objectName) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","object_type SQL"));
        
        if (Strings.isEmpty(_objectName) ) 
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String objectType = "";
        String sql = "select ao.object_type \n" +
                       "from ALL_OBJECTS ao \n" +
                     " where ao.owner       = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
                     "   and ao.object_name = ? \n" + 
                      "  and rownum < 2";
        
        PreparedStatement psObjectType = _conn.prepareStatement(sql);
        psObjectType.setString(1,schema);
        psObjectType.setString(2,_objectName.toUpperCase());
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase());

        ResultSet objectTypeRSet = psObjectType.executeQuery();
        if (objectTypeRSet.next()) {
            objectType = objectTypeRSet.getString(1);
        }
        objectTypeRSet.close();
        objectTypeRSet = null;
        psObjectType.close(); 
        psObjectType = null;
        
        return objectType;
    }
    
    /**
     * @function getMetadata
     * @precis   Reads everything in metadata for current schema
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @param _allSchemas
     * @return
     * @throws SQLException
     * @author Simon Greener, April 17th 2010
     *          Handling of Z and M dimensions corrected.
     * @author Simon Greener, April 28th 2010
     *          Re-wrote to improve performance when fetching all metadata for all schemas
     * @author Simon Greener, June 9th 2010
     *          Fixed bug in writing wrong string for metadataEntry to LinkedHashMap
     */
    public static LinkedHashMap<String, MetadataEntry> getMetadata( Connection _conn,
                                                                    String     _schemaName, 
                                                                    String     _objectName, 
                                                                    String     _columnName,
                                                                    boolean    _allSchemas) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }
        
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","MetadataTool.getMetadata(_conn)"));
        }

        LinkedHashMap<String, MetadataEntry> metaEntries = new LinkedHashMap<String, MetadataEntry>();
        
        String schemaClause = "";
        String objectClause = "";
        String columnClause = "";
        
        if ( _allSchemas ) {
            schemaClause = "( asgm.owner is not null ) \n";
        } else {
            schemaClause = (Strings.isEmpty(_schemaName) ? "asgm.OWNER = SYS_CONTEXT('USERENV','SESSION_USER')" : " asgm.owner = ? \n");
            objectClause = (Strings.isEmpty(_objectName) ? "" : "   AND asgm.TABLE_NAME  = ? \n");
            columnClause = (Strings.isEmpty(_columnName) ? "" : "   AND asgm.COLUMN_NAME = ? \n");
        }
        String sql = 
            "SELECT f.owner, f.table_name, f.column_name,\n" + 
            "       asgm.srid,\n" + 
            "       case when f.which_meta = -1 then 'true' else 'false' end is_Orphan,\n" + 
            "       case when f.which_meta =  1 then 'true' else 'false' end is_Missing,\n" + 
            "       asgm.diminfo\n" + 
            "  FROM (SELECT owner, table_name, column_name, sum(meta) as which_meta\n" + 
            "          FROM (SELECT asgm.owner, asgm.table_name, asgm.column_name, -1 as meta\n" + 
            "                  FROM all_sdo_geom_metadata asgm \n" +
            "                 WHERE " + schemaClause + objectClause + columnClause + 
            "                UNION ALL\n" + 
            "                SELECT atc.owner, atc.table_name, atc.column_name, 1 as meta\n" + 
            "                  FROM all_tab_columns atc\n" + 
            "                 where " + (schemaClause + objectClause + columnClause).replace("asgm.","atc.") + 
            "                   AND atc.TABLE_NAME NOT LIKE 'BIN$%' \n" +
            "                   AND ( atc.data_type not like 'ST_ANNOT%' \n" +
            "                   AND ( atc.data_type = 'SDO_GEOMETRY' OR atc.data_type LIKE 'ST!_%' escape '!' ) ) \n" +
            "               ) i\n" + 
            "            group by owner,table_name,column_name\n" + 
            "       ) f,\n" + 
            "       all_sdo_geom_metadata asgm\n" + 
            " WHERE (    asgm.owner       (+)= f.owner \n" + 
            "        and asgm.table_name  (+)= f.table_name \n" + 
            "        and asgm.column_name (+)= f.column_name )\n" + 
            " order by f.owner,f.table_name,f.column_name";
        /* Get Schema, table, column and associated metadata */
        OraclePreparedStatement psMEntries = (OraclePreparedStatement)_conn.prepareStatement(sql);
        int i = 0;
        if ( ! _allSchemas ) {
            String parmValues = "";
            if ( !Strings.isEmpty(_schemaName)) { psMEntries.setString(++i, _schemaName.toUpperCase()); parmValues += "\n? = " + _schemaName.toUpperCase(); }
            if ( !Strings.isEmpty(_objectName)) { psMEntries.setString(++i, _objectName.toUpperCase()); parmValues += "\n? = " + _objectName.toUpperCase(); }
            if ( !Strings.isEmpty(_columnName)) { psMEntries.setString(++i, _columnName.toUpperCase()); parmValues += "\n? = " + _columnName.toUpperCase(); }
            if ( !Strings.isEmpty(_schemaName)) { psMEntries.setString(++i, _schemaName.toUpperCase()); parmValues += "\n? = " + _schemaName.toUpperCase(); }
            if ( !Strings.isEmpty(_objectName)) { psMEntries.setString(++i, _objectName.toUpperCase()); parmValues += "\n? = " + _objectName.toUpperCase(); }
            if ( !Strings.isEmpty(_columnName)) { psMEntries.setString(++i, _columnName.toUpperCase()); parmValues += "\n? = " + _columnName.toUpperCase(); }
            LOGGER.logSQL(sql + parmValues);
        } else {
            LOGGER.logSQL(sql);
        }
        
        psMEntries.setFetchSize(100); // default is 10
        psMEntries.setFetchDirection(ResultSet.FETCH_FORWARD);
        OracleResultSet entriesRSet = (OracleResultSet)psMEntries.executeQuery();
        String schema    = "";
        String table     = "";
        String geoColumn = "";
        String srid      = "";
        boolean orphan   = false;
        boolean missing  = false;
        ARRAY dimArray   = null;
        MetadataEntry metaEntry ;
        while (entriesRSet.next()) {
            // Get MetadaEntry header
            //
            schema    = entriesRSet.getString(1);
            table     = entriesRSet.getString(2);
            geoColumn = entriesRSet.getString(3);
            srid      = entriesRSet.getString(4);
            if (entriesRSet.wasNull()) srid = "NULL";
            orphan    = Boolean.valueOf(entriesRSet.getString(5)).booleanValue();
            missing   = Boolean.valueOf(entriesRSet.getString(6)).booleanValue();
            dimArray  = entriesRSet.getARRAY(7);
            if (entriesRSet.wasNull()) dimArray = null;            
            metaEntry = new MetadataEntry(schema, table, geoColumn, srid);
            metaEntry.setOrphan(orphan);
            metaEntry.setMissing(missing);
            metaEntry.add(dimArray);
            // Add Metadata Entry to List
            metaEntries.put(metaEntry.getFullName(),metaEntry);
        } // While
        entriesRSet.close();
        entriesRSet = null; 
        return new LinkedHashMap<String, MetadataEntry>(metaEntries);
    }

    /**
     * @function getDimInfo 
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @return List<String>
     * @throws SQLException
     * @author Simon Greener, May 4th 2010
     * INCOMPLETE. Need to integrate with sdoapi.jar DimInfo class.
     */
    public List<String> getDimInfo(Connection _conn, 
                                   String     _schemaName,
                                   String     _objectName,
                                   String     _columnName) 
      throws SQLException,
             IllegalArgumentException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }
        
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","USER_SDO_GEOM_METADATA SQL"));
        }
        
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String columnClause = (Strings.isEmpty(_columnName)
                              ? ""
                              : "and asgm.COLUMN_NAME = ? \n");
        String sql = 
        "Select B.DimInfo, B.Mintolerance, B.Maxtolerance \n" +
        "  From ( Select Row_Number() Over (Partition By a.owner,a.table_name,a.column_name order by t.sdo_tolerance) As rin, \n" +
        "                DimInfo,  \n" +
        "                Max(T.Sdo_Tolerance) Over (Partition By A.Owner,A.Table_Name,A.Column_Name ) As Maxtolerance, \n" +
        "                Min(t.Sdo_Tolerance) Over (Partition By a.owner,a.table_name,a.column_name ) As MinTolerance \n" +
        "           From All_Sdo_Geom_Metadata A, \n" +
        "                Table(A.DimInfo) T \n" +
        "          Where a.owner       = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
        "            and a.table_name  = ? \n" + 
                         columnClause +
        "       ) B \n" +
        " Where b.rin = 1"; /* Select first in each set of dim_elements for a DimInfo */
        List<String> dimensionInfo = new ArrayList<String>();
        
        PreparedStatement psTable = _conn.prepareStatement(sql);
        psTable.setString(1,schema);
        psTable.setString(2,_objectName.toUpperCase());
        if ( columnClause.contains("?") ) {
           psTable.setString(3, _columnName.replace("\"","").toUpperCase() );
        }
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase() + 
                      (columnClause.contains("?") ? ("\n? = " + _objectName.toUpperCase()) : ""));

        ResultSet rSet = psTable.executeQuery();
        while (rSet.next()) {
            dimensionInfo.add(rSet.getString(1));
        }
        rSet.close();
        rSet = null;
        psTable.close();
        psTable = null;
        
        return dimensionInfo;
    }

    public static List<String> getSRIDS(Connection _conn,
                                        boolean _sortBySRID) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }
        
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","SRIDs SQL"));
        }

        String sortField = _sortBySRID ? "SRID" : "CS_NAME";
        List<String> srids = new ArrayList<String>();
        String sql = "Select to_char(cs.Srid,'99999999') || ' (' || \n" +
                     "       Case When (Select 1 \n" +
                     "                    From Mdsys.Geodetic_Srids Gs \n" +
                     "                   Where Gs.Srid = Cs.Srid ) = 1 \n" +
                     "            Then 'G' else 'P' end  || ') ' || \n" +
                     "       cs.cs_name \n" + 
                     "  From Mdsys.Cs_Srs Cs \n" + 
                     "  order by cs." + sortField;
        LOGGER.logSQL(sql);
        
        Statement st = _conn.createStatement();
        st.setFetchSize(100); // default is 10
        st.setFetchDirection(ResultSet.FETCH_FORWARD);
        ResultSet rSet = st.executeQuery(sql);
        while (rSet.next()) 
            srids.add(rSet.getString(1));
        rSet.close(); rSet = null;
        st.close(); st = null;

        return srids;
    }

    /**
     * @function createDimElement
     * @param _dimName
     * @param _minVal
     * @param _maxVal
     * @param _tol
     * @return
     * @throws IllegalArgumentException
     * @precis Creates string version of SDO_DIM_ELEMENT
     * @author Simon Greener, May 15th, 2010
     *          Moved from MetadataPanel.java to here so can be shared with other classes
     */
    public static String createDimElement(String _dimName, 
                                          String _minVal,
                                          String _maxVal,
                                          String _tol) 
    throws IllegalArgumentException 
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);

        if (Strings.isEmpty(_dimName)) {throw new IllegalArgumentException(propertyManager.getMsg("MD_DIMNAME")); }
        if (Strings.isEmpty(_minVal))  {throw new IllegalArgumentException(propertyManager.getMsg("MD_DIMENSION_ERROR","MD_MINIMUM",_dimName)); }
        if (Strings.isEmpty(_maxVal))  {throw new IllegalArgumentException(propertyManager.getMsg("MD_DIMENSION_ERROR","MD_MAXIMUM",_dimName)); }
        if (Strings.isEmpty(_tol))     {throw new IllegalArgumentException(propertyManager.getMsg("MD_DIMENSION_ERROR","MD_TOLERANCE",_dimName)); }
        return Constants.MDSYS_SCHEMA + "." + 
               Constants.TAG_SDO_ELEMENT +
               "('" + _dimName + "', " + _minVal + ", " + _maxVal + ", " + _tol + ")";
    }

    /**
     * @function checkCrossSchemaDMLPermissions
     * @precis   Checks if can do all three DML operations against SDO_GEOM_METADATA_TABLE
     * @param _conn
     * @return boolean
     * @author Simon Greener, May 16th 2010, Original Coding
     */
    public static boolean checkCrossSchemaDMLPermissions(Connection _conn)
    throws IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","layer_gtype SQL"));
        
        int sqlCount = 0;
        String sql = "Select count(*) \n" +
                    "  From All_Tab_Privs  \n" +
                    " Where Table_Schema = 'MDSYS' \n" +
                    "   And Table_Name = 'SDO_GEOM_METADATA_TABLE' \n" +
                    "   And Privilege IN ('INSERT','UPDATE','DELETE') \n" +
                    "   And ( Grantee = 'PUBLIC' \n" +
                    "      Or Grantee = Sys_Context('USERENV','SESSION_USER') \n" +
                    "       )";
        try {
            LOGGER.logSQL(sql);
            Statement st = _conn.createStatement();
            ResultSet rSet = st.executeQuery(sql);
            if (rSet.next()) {
                sqlCount = rSet.getInt(1);
                sqlCount = rSet.wasNull() ? 0 : sqlCount;
            }
            rSet.close(); rSet = null;
            st.close(); st = null;
        } catch (Exception ex) {
            LOGGER.error("checkCrossSchemaDMLPermissions: " + ex.toString());
            return false;
        }
        return sqlCount == 3;
    }

    /**
     * @method getShapeType
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @param _samplePercentage
     * @param _sampleRows
     * @return
     * @author Simon Greener, Original Coding
     * @author Simon Greener, December 7th 2010, modified for fast, one row, GType detection
     */
     public static List<String> getShapeType(Connection _conn,
                                             String     _schemaName,
                                             String     _objectName,
                                             String     _columnName,
                                             int        _samplePercentage,
                                             int        _sampleRows) 
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","layer_gtype SQL"));
        
        if (Strings.isEmpty(_objectName) ) 
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        if (Strings.isEmpty(_columnName) )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
        
        if (_samplePercentage < 0 || _samplePercentage > 100) {
            return null;
        }
  
        List<String> shapeTypes = new ArrayList<String>(20);
        boolean isSTGeom;
        try {
            isSTGeom = isSTGeometry(_conn,_schemaName,_objectName,_columnName);
            String columnName = isSTGeom ? _columnName + ".GEOM" : _columnName;
            String fullName =
                Strings.isEmpty(_schemaName) 
                              ? _objectName :
                Strings.append(_schemaName,_objectName,Constants.TABLE_COLUMN_SEPARATOR);
            
            String sourceSQL = "select i." + columnName + " as geom from " + fullName;
            if ( _samplePercentage == 0 || _samplePercentage == 100 ) {
                sourceSQL += " i Where i." + _columnName + " Is Not Null ";
                if ( _sampleRows > 0 )
                    sourceSQL += " and rownum < " + (_sampleRows+1);
            } else {
                sourceSQL += " sample(" + _samplePercentage + ") i union all " +
                             "select j." + columnName + " as geom from " + fullName + " j where j." + _columnName + " Is Not Null and rownum < " + 
                             (_sampleRows>0 ? String.valueOf(_sampleRows+1) : "2");
            }
      
            String sql =  
            "select Case f.geometryType\n" + 
            "             When 0 Then 'POINT'     When 1 Then 'MULTIPOINT'\n" + 
            "             When 2 Then 'ARC'       When 6 Then 'ARC'\n" + 
            "             When 3 Then 'POLYGON'   When 7 Then 'POLYGON'\n" + 
            "             When 8 Then 'POLYGON'   When 9 Then 'POLYGON'\n" + 
            "             When 4 Then 'UNDEFINED' Else 'UNDEFINED'\n" + 
            "         End as gType,\n" + 
            "        isZ,isM\n" + 
            "  from (Select DISTINCT \n" + 
            "        Case Mod(A.GEOM.Sdo_Gtype,10)\n" + 
            "             When 0 Then 0\n" + 
            "             When 1 Then 0 When 5 Then 1\n" + 
            "             When 2 Then 2 When 6 Then 2\n" + 
            "             When 3 Then 3 When 7 Then 3\n" + 
            "             When 4 Then 4\n" + 
            "             When 8 Then 8 When 9 Then 8 Else -1\n" + 
            "        End as geometryType,\n" + 
            "       case TRUNC(A.GEOM.Sdo_Gtype/1000) \n" + 
            "            WHEN 2 THEN CAST(NULL AS VARCHAR2(1))\n" + 
            "            WHEN 3 THEN CASE WHEN MOD(TRUNC(A.GEOM.Sdo_Gtype/100),10)=0 THEN 'Z' ELSE CAST(NULL AS VARCHAR2(1)) END\n" + 
            "            WHEN 4 THEN 'Z' \n" + 
            "            ELSE CAST(NULL AS VARCHAR2(1))\n" + 
            "        end isZ,\n" + 
            "         case TRUNC(A.GEOM.Sdo_Gtype/1000) \n" + 
            "              WHEN 2 THEN CAST(NULL AS VARCHAR2(1))\n" + 
            "              WHEN 3 THEN CASE WHEN MOD(TRUNC(A.GEOM.Sdo_Gtype/100),10)=0 THEN CAST(NULL AS VARCHAR2(1)) ELSE 'M' END\n" + 
            "              WHEN 4 THEN 'M' \n" + 
            "              ELSE CAST(NULL AS VARCHAR2(1))\n" + 
            "          end isM\n" + 
            "   From (" + sourceSQL + " ) A \n" +
            " where mod(a.geom.sdo_gtype,10) <> 0 ) f\n" + 
            "order by geometryType asc, isZ desc, isM desc";
            
          LOGGER.logSQL(sql);

          String shapeType = "";
          String isM       = "";
          String isZ       = "";
            
          Statement st = _conn.createStatement();
          ResultSet rSet = st.executeQuery(sql);
          while (rSet.next() ) {
              shapeType = rSet.getString(1); if ( rSet.wasNull() || Strings.isEmpty(shapeType) ) continue;
              isZ       = rSet.getString(2); if ( rSet.wasNull() || Strings.isEmpty(isZ) ) isZ = "";
              isM       = rSet.getString(3); if ( rSet.wasNull() || Strings.isEmpty(isM) ) isM = "";
              if (Strings.isEmpty(isZ) && Strings.isEmpty(isM) ) {
                  if ( ! shapeTypes.contains(shapeType) ) {
                      shapeTypes.add(shapeType);
                  }
              } else {
                  if ( !Strings.isEmpty(isZ) ) {
                      if ( ! shapeTypes.contains(shapeType+isZ) ) {
                          shapeTypes.add(shapeType+isZ);
                      }
                  }
                  if ( !Strings.isEmpty(isM) ) {
                      if ( ! shapeTypes.contains(shapeType+isM) ) {
                          shapeTypes.add(shapeType+isM);
                      }
                  }
              }
          }
          rSet.close();
          st.close();
      } catch (Exception e) {
          // Can be SQLException or IllegalArgumentException!
          String message = propertyManager.getMsg("OBJECT_RETRIEVAL_ERROR",
                                                  e.getMessage(),
                                                  propertyManager.getMsg("OBJECT_LAYER_GTYPE") + 
                                                  "(" + Strings.objectString(_schemaName,_objectName,_columnName) + ")" );
          LOGGER.error(message);
          return (List<String>)null;
      }
      return new ArrayList<String>(shapeTypes);
    }
    
    /**
     * @method getLayerGType
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @param _percentage
     * @return
     * @author Simon Greener, Original Coding
     * @author Simon Greener, December 7th 2010, modified for fast, one row, GType detection
     */
    public static String getLayerGeometryType(Connection _conn,
                                              String     _schemaName,
                                              String     _objectName,
                                              String     _columnName,
                                              int        _samplePercentage,
                                              int        _sampleRows)
    throws Exception
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }

        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","layer_gtype SQL"));
        }
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        if (Strings.isEmpty(_columnName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
        }        
        if (_samplePercentage < 0 || _samplePercentage > 100) {
            return null;
        }
        
        boolean isSTGeom;
        String layerGType = "";
        try {
            isSTGeom = isSTGeometry(_conn,_schemaName,_objectName,_columnName);
            String columnName = isSTGeom ? _columnName + ".GEOM" : _columnName;
            String fullName =
                Strings.isEmpty(_schemaName) 
                ? _objectName :
                Strings.append(_schemaName,_objectName,Constants.TABLE_COLUMN_SEPARATOR);
            String sourceSQL = "select i." + columnName + " as " + _columnName + " from " + fullName;
            if ( _samplePercentage == 0 || _samplePercentage == 100 ) {
                sourceSQL += " i Where i." + _columnName + " Is Not Null ";
                if ( _sampleRows > 0 ) {
                    sourceSQL += " and rownum < " + (_sampleRows+1);
                }
            } else {
                sourceSQL += " sample(" + _samplePercentage + ") i union all " +
                             "select j." + columnName + " as " + columnName + " from " + fullName + " j where j." + _columnName + " Is Not Null and rownum < " + 
                             (_sampleRows>0 ? String.valueOf(_sampleRows+1) : "2");
            }
            String drivingSQL =   "Select A." + _columnName + ".Sdo_Gtype As Gtype, \n" +
            "                             SUBSTR(Case MOD(A." + _columnName + ".sdo_gtype,10) \n" +
            "                                         When 0 Then 'UNKNOWN' \n" +
            "                                         When 1 Then 'POINT'      When 5 Then 'MULTIPOINT' \n" +
            "                                         When 2 Then 'LINE'       When 6 Then 'MULTILINE' \n" +
            "                                         When 3 Then 'POLYGON'    When 7 Then 'MULTIPOLYGON' \n" +
            "                                         When 8 Then 'SOLID'      When 9 Then 'MULTISOLID' \n" +
            "                                         When 4 Then 'COLLECTION' Else 'NULL'  \n" +
            "                                      End, \n" +
            "                                     1,20) As Layer_Gtype, \n" +
            "                             Case Mod(A." + _columnName + ".Sdo_Gtype,10) \n" +
            "                                  When 0 Then 0 \n" +
            "                                  When 1 Then 10 When 5 Then 11 \n" +
            "                                  When 2 Then 20 When 6 Then 21 \n" +
            "                                  When 3 Then 30 When 7 Then 31 \n" +
            "                                  When 4 Then 41  \n" +
            "                                  When 8 Then 80 When 9 Then 81 Else -1 \n" +
            "                              End As layer_gtype_num, \n" +
            "                             count(*) as gtype_count \n" +
            "                        From (" + sourceSQL + " ) A \n" +
            "                       Where mod(a." + _columnName + ".sdo_gtype,10) <> 0 \n" + 
            "                       group by A." + _columnName + ".Sdo_Gtype, MOD(A." + _columnName + ".sdo_gtype,10)";
    
            String layerGeometryTypeSQL =  
                            "Select Case When All_Count = 1 Then Layer_Gtype \n" +
                            "            When Layer_Gtype = 'COLLECTION' or all_count > 2 Then 'COLLECTION' \n" +
                            "            When Instr(Layer_Gtype,Next_Layer_Gtype) > 0  \n" +
                            "            Then layer_gtype \n" +
                            "            Else 'COLLECTION' \n" +
                            "        end as layer_gtype \n" +
                            "  from (Select Layer_Gtype, \n" +
                            "               Lag(Layer_Gtype,1) Over (Order By Layer_Gtype_Num) As Next_Layer_Gtype, \n" +
                            "               Lag(Layer_Gtype,2) Over (Order By layer_gtype_Num) As Next_Layer_Gtype2, \n" +
                            "               All_Count \n" +
                            "          From (Select Layer_Gtype, Gtype_Count, layer_gtype_Num, count(*) over () as all_count \n" +
                            "                  From (" + drivingSQL + "\n" +
                            "                       ) \n" +
                            "                ) \n" +
                            "          Order By Layer_Gtype_Num Desc \n" +
                            "        ) \n";

            LOGGER.logSQL(layerGeometryTypeSQL);
            Statement st = _conn.createStatement();
            ResultSet rSet = st.executeQuery(layerGeometryTypeSQL);
            if (rSet.next())
                layerGType = rSet.getString(1);
            if ( rSet.wasNull() )
                layerGType = "";
            rSet.close();
            st.close();
        } catch (SQLException sqle) {
            // Probably "No data read" error, ie table exists but is empty.
            //
            String message = propertyManager.getMsg("OBJECT_RETRIEVAL_ERROR",
                                                    sqle.getMessage(),
                                                    propertyManager.getMsg("OBJECT_LAYER_GTYPE") + 
                                                    "(" + Strings.objectString(_schemaName,_objectName,_columnName) + ")" );
            LOGGER.error(message);
            throw new Exception(message);
        }
        return layerGType;
    }

    public static String getLayerGeometryType(Connection    _conn,
                                              MetadataEntry _mEntry,
                                              int           _samplePercentage,
                                              int           _sampleRows) 
    throws Exception {
        return getLayerGeometryType(_conn,
                                    _mEntry.getSchemaName(), 
                                    _mEntry.getObjectName(), 
                                    _mEntry.getColumnName(),
                                    _samplePercentage,
                                    _sampleRows);
    }
    
    public static boolean isSpatiallyIndexed(Connection _conn, 
                                             String     _schemaName,
                                             String     _objectName,
                                             String     _columnName,
                                             String     _SRID) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","isSpatiallyIndexed()"));
        }
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        if (Strings.isEmpty(_columnName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
        }
        if (Strings.isEmpty(_SRID)) {
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                             propertyManager.getMsg("METADATA_TABLE_COLUMN_3")));
        }
        String fullName =
            Strings.isEmpty(_schemaName) 
                          ? _objectName :
            Strings.append(_schemaName,_objectName,Constants.TABLE_COLUMN_SEPARATOR);
        String sql = 
            "SELECT /*+FIRST_ROWS(1)*/ count(*) \n" + 
            "  FROM " + fullName + " a\n" + 
            " WHERE SDO_FILTER(a." + _columnName + 
                             ",MDSYS.SDO_GEOMETRY(2003," + _SRID + ",NULL,\n" +
                             " MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,3),\n" +
                             " MDSYS.SDO_ORDINATE_ARRAY(0,0,1,1))) = 'TRUE'" +
            "   AND rownum < 2"; 
        LOGGER.logSQL(sql);
        OracleStatement st = (OracleStatement)_conn.createStatement();
        PreparedStatement psTable = _conn.prepareStatement(sql);
        boolean indexExists = true;
        try {
            ResultSet rSet = psTable.executeQuery();
            if (rSet.next()) {
                indexExists = true;
            } 
            rSet.close(); rSet = null;
            st.close();   st = null;
        } catch (SQLException sqle) {
            String msg = sqle.getMessage();
            if ( !Strings.isEmpty(msg) ) {
                msg = msg.startsWith("ORA-13226: interface not supported without a spatial index") 
                      ? "ORA-13226: interface not supported without a spatial index"
                      : msg;
            }
            LOGGER.warn(msg);
            indexExists = false;
        }
        return indexExists;
    }
    
    public static boolean hasSpatialIndex(Connection _conn, 
                                          String     _schemaName,
                                          String     _objectName,
                                          String     _columnName,
                                          boolean    _valid) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","hasSpatialIndex()"));
        
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        
        if (Strings.isEmpty(_columnName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
        }
    
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String sql = 
            "SELECT INDEX_NAME\n" + 
            "  FROM all_indexes ai\n" + 
            " WHERE ai.owner      = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER'))\n" + 
            "   AND ai.table_name = ?\n" + 
            "   AND ai.index_type = 'DOMAIN'\n" + 
            "   AND ai.ITYP_OWNER = 'MDSYS'\n"  + 
            "   AND ai.ITYP_NAME  = 'SPATIAL_INDEX'\n" + 
            ( _valid ? "   AND ai.domidx_opstatus = 'VALID'\n" : "" ) + 
            "   AND EXISTS ( SELECT 1\n" + 
            "                  FROM all_ind_columns aic \n" + 
            "                 WHERE aic.index_owner = ai.owner \n" + 
            "                   AND aic.index_name  = ai.index_name \n" + 
            "                   AND aic.column_name = ?)";

        OracleStatement st = (OracleStatement)_conn.createStatement();
        PreparedStatement psTable = _conn.prepareStatement(sql);
        psTable.setString(1,schema);
        psTable.setString(2,_objectName.toUpperCase());
        psTable.setString(3,_columnName.replace("\"","").toUpperCase());
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase() +
                      "\n? = " + _columnName.replace("\"","").toUpperCase());

        ResultSet rSet = psTable.executeQuery();
        boolean indexExists = false;
        String indexName = "";
        if (rSet.next()) {
            indexName = rSet.getString(1);  // ignore result
            if ( !Strings.isEmpty(indexName) )
                indexExists = true;
        } 
        rSet.close(); rSet = null;
        st.close();   st = null;
        return indexExists;
    }
    
    public static int getSpatialIndexDims(Connection _conn, 
                                          String     _schemaName,
                                          String     _objectName,
                                          String     _columnName) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","RTree sdo_index_dims SQL"));
        
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        
        if (Strings.isEmpty(_columnName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
        }
  
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String sql = 
              "select sdo_index_dims \n" + 
              "  from all_sdo_index_info asii \n" + 
              "       inner join  \n" + 
              "       all_sdo_index_metadata asim \n" + 
              "       on ( asim.sdo_index_owner = asii.sdo_index_owner \n" + 
              "            and \n" + 
              "            asim.sdo_index_name = asii.index_name \n" + 
              "           ) \n" +
              "   Where asii.table_owner = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
              "     and asii.table_name  = ? \n" + 
              "     and asii.column_name = ?";

        OracleStatement st = (OracleStatement)_conn.createStatement();
        PreparedStatement psTable = _conn.prepareStatement(sql);
        psTable.setString(1,schema);
        psTable.setString(2,_objectName.toUpperCase());
        psTable.setString(3,_columnName.replace("\"","").toUpperCase());
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase() +
                      "\n? = " + _columnName.replace("\"","").toUpperCase());

        ResultSet rSet = psTable.executeQuery();
        int sdo_index_dims = -1;
        if (rSet.next()) {
            sdo_index_dims = rSet.getInt(1);
        } 
        rSet.close(); rSet = null;
        st.close();   st = null;
        return sdo_index_dims;
    }

    public static String getSpatialIndexName(Connection _conn, 
                                             String     _schemaName,
                                             String     _objectName,
                                             String     _columnName) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }
        
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","RTree sdo_index_dims SQL"));
        }
        
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        
        if (Strings.isEmpty(_columnName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
        }
    
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String sql = 
              "select asim.sdo_index_name \n" + 
              "  from all_sdo_index_info asii \n" + 
              "       inner join  \n" + 
              "       all_sdo_index_metadata asim \n" + 
              "       on ( asim.sdo_index_owner = asii.sdo_index_owner \n" + 
              "            and \n" + 
              "            asim.sdo_index_name = asii.index_name \n" + 
              "           ) \n" +
              "   Where asii.table_owner = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
              "     and asii.table_name  = ? \n" + 
              "     and asii.column_name = ?";

        OracleStatement st = (OracleStatement)_conn.createStatement();
        PreparedStatement psTable = _conn.prepareStatement(sql);
        psTable.setString(1,schema);
        psTable.setString(2,_objectName.toUpperCase());
        psTable.setString(3,_columnName.replace("\"","").toUpperCase());
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase() +
                      "\n? = " + _columnName.replace("\"","").toUpperCase());

        ResultSet rSet = psTable.executeQuery();
        String sdo_index_name = "";
        if (rSet.next()) {
            sdo_index_name = rSet.getString(1);
            if (rSet.wasNull()) {
                sdo_index_name = null;
            }
        } 
        rSet.close(); rSet = null;
        st.close();   st = null;
        return sdo_index_name;
    }
    
    public static STRUCT projectFilterGeometry(OracleConnection _conn,
                                               Envelope  _mbr,
                                               int              _sourceSRID,
                                               int              _destinationSRID) 
    throws SQLException, 
           IllegalArgumentException
    {
        if ( propertyManager == null )
            propertyManager = new PropertiesManager(propertiesFile);

        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","projectFilterGeoemtry"));
          
        if ( _mbr == null)
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                                                      propertyManager.getMsg("MBR")));
        if (_sourceSRID == _destinationSRID || 
            _sourceSRID == Constants.SRID_NULL || 
            _destinationSRID == Constants.SRID_NULL ) {
            return null;
        }
        
        String sql = "SELECT MDSYS.SDO_CS.TRANSFORM( \n" +
                            "MDSYS.SDO_GEOMETRY(2003,?,NULL,\n" +
                            "MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,3),\n" +
                            "MDSYS.SDO_ORDINATE_ARRAY(?,?,?,?)),?) as rect " +
                     "  FROM DUAL";

        OraclePreparedStatement pStatement = (OraclePreparedStatement)_conn.prepareStatement(sql);
        pStatement.setInt(1,_sourceSRID);
        pStatement.setDouble(2,_mbr.getMinX());
        pStatement.setDouble(3,_mbr.getMinY());
        pStatement.setDouble(4,_mbr.getMaxX());
        pStatement.setDouble(5,_mbr.getMaxY());
        pStatement.setInt(6,_destinationSRID);
        LOGGER.logSQL(sql + 
                      "\n? = " + _sourceSRID +
                      "\n? = " + _mbr.getMinX() +
                      "\n? = " + _mbr.getMinY() +
                      "\n? = " + _mbr.getMaxX() +
                      "\n? = " + _mbr.getMaxY() +
                      "\n? = " + _destinationSRID);
        OracleResultSet rSet = (OracleResultSet)pStatement.executeQuery();
        STRUCT retGeom = null;
        if (rSet.next()) {
            retGeom = rSet.getSTRUCT(1);
            if ( rSet.wasNull() ) retGeom = null;
        }
        rSet.close();
        rSet = null;
        pStatement.close();
        pStatement = null;
        return retGeom;
    }

    public static STRUCT projectJGeometry(OracleConnection _conn,
                                          JGeometry        _jGeom,
                                          int              _destinationSRID) 
    throws SQLException, 
           IllegalArgumentException
    {
        if ( propertyManager == null )
            propertyManager = new PropertiesManager(propertiesFile);

        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","projectFilterGeoemtry"));
          
        if ( _jGeom == null)
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                                                      propertyManager.getMsg("MBR")));
        if (_destinationSRID == Constants.SRID_NULL ) {
            return null;
        }
        
        String sql = "SELECT MDSYS.SDO_CS.TRANSFORM(?,?) as geom FROM DUAL";

        OraclePreparedStatement pStatement = (OraclePreparedStatement)_conn.prepareStatement(sql);
        try {
			pStatement.setSTRUCT(1,(STRUCT) JGeometry.storeJS(_conn,_jGeom));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        pStatement.setInt   (2,_destinationSRID);
        try {
            LOGGER.logSQL(sql + 
                          "\n? = " + RenderTool.renderGeometryAsPlainText(_jGeom, Constants.TAG_SDO_GEOMETRY, Constants.bracketType.NONE, Constants.MAX_PRECISION) +
                          "\n? = " + _destinationSRID );
        } catch (Exception e) {
            LOGGER.logSQL(sql);
            e.printStackTrace();
        }
        OracleResultSet rSet = (OracleResultSet)pStatement.executeQuery();
        STRUCT retGeom = null;
        if (rSet.next()) {
            retGeom = rSet.getSTRUCT(1);
            if ( rSet.wasNull() ) retGeom = null;
        }
        rSet.close();
        rSet = null;
        pStatement.close();
        pStatement = null;
        return retGeom;
    }

    public static Point2D projectPoint(OracleConnection _conn,
                                       Point2D          _point,
                                       int              _sourceSRID,
                                       int              _destinationSRID) 
    throws SQLException, 
           IllegalArgumentException
    {
        if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
        }

        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","projectFilterGeoemtry"));
        }
          
        if ( _point == null) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                                                      propertyManager.getMsg("MBR")));
        }
        if ( (_sourceSRID == Constants.SRID_NULL || _destinationSRID == Constants.SRID_NULL ) 
             ||
             (_sourceSRID==_destinationSRID ) ) {
            return _point;
        }
        
        String sql = "SELECT f.geom.sdo_point.x, f.geom.sdo_point.y " +
                      " FROM (SELECT MDSYS.SDO_CS.TRANSFORM(SDO_GEOMETRY(2001,?,SDO_POINT_TYPE(?,?,NULL),NULL,NULL),?) as geom FROM DUAL) f" +
                     " WHERE f.geom is not null";

        OraclePreparedStatement pStatement = (OraclePreparedStatement)_conn.prepareStatement(sql);
        pStatement.setInt   (1,_sourceSRID);
        pStatement.setDouble(2,_point.getX());
        pStatement.setDouble(3,_point.getY());
        pStatement.setInt   (4,_destinationSRID);
        try {
            LOGGER.logSQL(String.format(sql.replaceAll("?", "%s"),
                                        String.valueOf(_sourceSRID),
                                        String.valueOf(_point.getX()),
                                        String.valueOf(_point.getY()),
                                        String.valueOf(_destinationSRID)));
        } catch (Exception e) {
            LOGGER.logSQL(sql);
            e.printStackTrace();
        }
        OracleResultSet rSet = (OracleResultSet)pStatement.executeQuery();
        double x = Double.NaN, y = Double.NaN;
        if (rSet.next()) {
            x = rSet.getDouble(1);
            if ( rSet.wasNull() ) x = Double.NaN;
            y = rSet.getDouble(2);
            if ( rSet.wasNull() ) y = Double.NaN;
        }
        rSet.close();
        rSet = null;
        pStatement.close();
        pStatement = null;
        Point2D xy = new Point2D.Double(x,y);
        return xy;
    }
    
    /** ============================================================   
      * =======              EXTENT METHODS           ==============
    ***/
    
    /**
     * @method getRTreeExtent
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @return
     * @throws SQLException
     * @throws IllegalArgumentException
     * @author Simon Greener, June 22nd 2010, Original Coding
     * @author Simon Greener, July 5th 2010, 
     *          Modified code to detect geodetic srids and throw exception
     * @author Simon Greener, August 10th 2010, 
     *          Modified code to include projection of geometry.
     *          Need _sourceSRID because sdo_root_mbr does not have sdo_srid set
     */
    public static Envelope getExtentFromRTree(Connection _conn, 
                                                     String     _schemaName,
                                                     String     _objectName,
                                                     String     _columnName,
                                                     String     _sourceSRID,
                                                     String     _destinationSRID) 
      throws SQLException, 
             IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);

        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","RTree Extent SQL"));
        
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        if (Strings.isEmpty(_columnName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
        }
        
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        String tableVerticesString = "         table(sdo_util.getVertices(asim.sdo_root_mbr)) v \n";
        if (Strings.isEmpty(_sourceSRID)==false && Strings.isEmpty(_destinationSRID)==false && 
            _sourceSRID.equalsIgnoreCase(_destinationSRID)==false ) {
            // sdo_root_mbr SDO_GEOMETRY does not have a its SRID set!
            tableVerticesString = 
"          table(sdo_util.getVertices(MDSYS.SDO_CS.TRANSFORM(\n" +
"                                   mdsys.sdo_geometry(\n" +
"                                      asim.sdo_root_mbr.sdo_gtype,\n" +
"                                      " + _sourceSRID + ",\n" +
"                                      asim.sdo_root_mbr.sdo_point,\n" +
"                                      asim.sdo_root_mbr.sdo_elem_info,\n" +
"                                      asim.sdo_root_mbr.sdo_ordinates),\n" +
"                                      " + _destinationSRID + ") ) ) v \n";
        }

        String sql = 
        " select sdo_index_geodetic, \n" +
        "        min(v.x) as minx, min(v.y) as miny, max(v.x) as maxx, max(v.y) as maxy \n" +
        "   from all_sdo_index_info asii \n" +
        "         inner join  \n" +
        "         all_sdo_index_metadata asim \n" +
        "         on ( asim.sdo_index_owner = asii.sdo_index_owner \n" +
        "              and \n" +
        "              asim.sdo_index_name = asii.index_name \n" +
        "             ), \n" +
        tableVerticesString +
        "   Where asii.table_owner = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
        "     and asii.table_name  = ? \n" + 
        "     and asii.column_name = ? \n" +
        "   Group by sdo_index_geodetic";
        Envelope rd = new Envelope(Constants.MAX_PRECISION);
        String geodetic = "";
        PreparedStatement psTable = _conn.prepareStatement(sql);
        psTable.setString(1,schema);
        psTable.setString(2,_objectName.toUpperCase());
        psTable.setString(3,_columnName.replace("\"","").toUpperCase());
        LOGGER.logSQL(sql + 
                      "\n? = " + schema +
                      "\n? = " + _objectName.toUpperCase() +
                      "\n? = " + _columnName.replace("\"","").toUpperCase());

        ResultSet rSet = psTable.executeQuery();
        if (rSet.next()) {
            geodetic = rSet.getString(1); if ( rSet.wasNull() ) geodetic = "FALSE";
            if ( geodetic.equalsIgnoreCase("TRUE") ) {
                throw new SQLException("Can't Get Extent from Spatial Index of a Geodetic SRID");
            }
            rd.minX  = rSet.getDouble(2); if ( rSet.wasNull() ) rd.minX = Double.MAX_VALUE;
            rd.minY  = rSet.getDouble(3); if ( rSet.wasNull() ) rd.minY = Double.MAX_VALUE;
            rd.maxX  = rSet.getDouble(4); if ( rSet.wasNull() ) rd.maxX = Double.MIN_VALUE;
            rd.maxY  = rSet.getDouble(5); if ( rSet.wasNull() ) rd.maxY = Double.MIN_VALUE;
        }
        rSet.close();
        rSet = null;
        psTable.close();
        psTable = null;
        return new Envelope(rd);
    }

    /**
     * @method getSampleExtent
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @param _columnName
     * @param _sampleSize
     * @return
     * @throws SQLException
     * @throws IllegalArgumentException
     * @author Simon Greener, June 22nd 2010, Original Coding
     * @author Simon Greener, July 5th 2010, 
     *          Modified code to detect geodetic srids and throw exception
     * @author Simon Greener, August 10th 2010, 
     *          Modified code to include projection of geometry.
     *          Need _sourceSRID because sdo_root_mbr does not have sdo_srid set
     */
    public static Envelope getExtentFromSample(Connection _conn, 
                                                  String     _schemaName,
                                                  String     _objectName,
                                                  String     _columnName,
                                                  int        _sampleSize) 
      throws SQLException, 
             IllegalArgumentException
    {
        if ( propertyManager == null )  {
            propertyManager = new PropertiesManager(propertiesFile);
        }
        if ( _conn==null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","RTree Extent SQL"));
        }
        if (Strings.isEmpty(_objectName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
        }
        if (Strings.isEmpty(_columnName) ) {
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                               propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
        }
        int sampleSize = _sampleSize<=0?1:_sampleSize;
        String sql = "SELECT SDO_GEOM.SDO_MIN_MBR_ORDINATE(f.mbr,1) as minx,\n" + 
                     "       SDO_GEOM.SDO_MIN_MBR_ORDINATE(f.mbr,2) as miny,\n" + 
                     "       SDO_GEOM.SDO_MAX_MBR_ORDINATE(f.mbr,1) as maxx,\n" + 
                     "       SDO_GEOM.SDO_MAX_MBR_ORDINATE(f.mbr,2) as maxy \n" +
                     "  FROM (SELECT SDO_GEOM.SDO_MBR(t."+_columnName+") as mbr \n" +
                     "          FROM " +
            Strings.append(_schemaName,_objectName,Constants.TABLE_COLUMN_SEPARATOR) + " t\n" +
                     "  WHERE rownum < " + sampleSize + "\n" +
                     "       ) f";
        Envelope rd = new Envelope(Constants.MAX_PRECISION);
        try {
            LOGGER.logSQL(sql);
            OracleStatement st = (OracleStatement)_conn.createStatement();
            OracleResultSet rSet = (OracleResultSet)st.executeQuery(sql);
            if (rSet.next()) {
                rd.minX  = rSet.getDouble(1); if ( rSet.wasNull() ) rd.minX = Double.MAX_VALUE;
                rd.minY  = rSet.getDouble(2); if ( rSet.wasNull() ) rd.minY = Double.MAX_VALUE;
                rd.maxX  = rSet.getDouble(3); if ( rSet.wasNull() ) rd.maxX = Double.MIN_VALUE;
                rd.maxY  = rSet.getDouble(4); if ( rSet.wasNull() ) rd.maxY = Double.MIN_VALUE;
            }
            rSet.close(); rSet = null;
            st.close(); st = null;
        } catch (SQLException sqle) {
          throw new SQLException(sql, sqle.getSQLState(), sqle.getErrorCode());
        }
        return new Envelope(rd);
    }
    
  /**
   * @method getExtentByDimInfo
   * @param _conn
   * @param _schemaName
   * @param _objectName
   * @param _columnName
   * @param _sourceSRID
   * @param _destinationSRID
   * @return
   * @throws SQLException
   * @throws IllegalArgumentException
   * @author Simon Greener, June 22nd 2010, Original Coding
   * @author Simon Greener, July 5th 2010, 
   *          Modified code to detect geodetic srids and throw exception
   */
    public static Envelope getExtentFromDimInfo(Connection _conn, 
                                                     String     _schemaName,
                                                     String     _objectName,
                                                     String     _columnName,
                                                     String     _destinationSRID) 
      throws SQLException, 
             IllegalArgumentException
    {
      if ( propertyManager == null ) {
            propertyManager = new PropertiesManager(propertiesFile);
      }
      if ( _conn==null ) {
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","USER_SDO_GEOM_METADATA SQL"));
      }
      if (Strings.isEmpty(_objectName) ) {
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                             propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
      }
      if (Strings.isEmpty(_columnName) ) {
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                             propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
      }

        String sql = "SELECT SDO_GEOM.SDO_MIN_MBR_ORDINATE(c.GEOM,1) as minx,\n" + 
                     "       SDO_GEOM.SDO_MIN_MBR_ORDINATE(c.GEOM,2) as miny,\n" + 
                     "       SDO_GEOM.SDO_MAX_MBR_ORDINATE(c.GEOM,1) as maxx,\n" + 
                     "       SDO_GEOM.SDO_MAX_MBR_ORDINATE(c.GEOM,2) as maxy \n";
        sql += (Strings.isEmpty(_destinationSRID) || _destinationSRID.equals(Constants.NULL)
                   ? "  FROM (SELECT MDSYS.SDO_GEOMETRY(2003,b.srid,null,mdsys.sdo_elem_info_array(1,1003,3),mdsys.sdo_ordinate_array(b.minx,b.miny,b.maxx,b.maxy)) as geom\n" 
                   : "  FROM (SELECT MDSYS.SDO_CS.TRANSFORM(MDSYS.SDO_GEOMETRY(2003,b.srid,null,mdsys.sdo_elem_info_array(1,1003,3),mdsys.sdo_ordinate_array(b.minx,b.miny,b.maxx,b.maxy))," + _destinationSRID + " ) as geom\n" ) + 
                     "          FROM (SELECT a.rin, a.srid,\n" + 
                     "                       case when a.rin = 1 then a.sdo_lb else null end as minx,\n" + 
                     "                       case when a.rin = 1 then LEAD(a.sdo_lb,1) OVER (ORDER BY a.RIN) else null end as miny,\n" + 
                     "                       case when a.rin = 1 then a.sdo_ub else null end as maxx,\n" + 
                     "                       case when a.rin = 1 then LEAD(a.sdo_ub,1) OVER (ORDER BY a.RIN) else null end as maxy\n" + 
                     "                 FROM ( SELECT rownum as rin, asgm.srid, dim.*\n" + 
                     "                          FROM all_sdo_geom_metadata asgm,\n" + 
                     "                               TABLE(asgm.DimInfo) dim\n" + 
                     "                         WHERE asgm.owner       = NVL(?,SYS_CONTEXT('USERENV','SESSION_USER')) \n" +
                     "                           AND asgm.table_name  = ? \n" + 
                     "                           AND asgm.column_name = ? ) a \n" +
                     "               ) b\n" + 
                     "         WHERE b.rin = 1\n" + 
                     "      ) c";
        String schema = Strings.isEmpty(_schemaName) ? "NULL" : _schemaName.toUpperCase();
        Envelope rd = new Envelope(Constants.MAX_PRECISION);
        try 
        {
            PreparedStatement psTable = _conn.prepareStatement(sql);
            psTable.setString(1,schema);
            psTable.setString(2,_objectName.toUpperCase());
            psTable.setString(3,_columnName.replace("\"","").toUpperCase() );
            LOGGER.logSQL(sql + 
                          "\n? = " + schema +
                          "\n? = " + _objectName.toUpperCase() +
                          "\n? = " + _columnName.replace("\"","").toUpperCase());

            ResultSet rSet = psTable.executeQuery();
            if (rSet.next()) {
                rd.minX  = rSet.getDouble(1); if ( rSet.wasNull() ) rd.minX = Double.MAX_VALUE;
                rd.minY  = rSet.getDouble(2); if ( rSet.wasNull() ) rd.minY = Double.MAX_VALUE;
                rd.maxX  = rSet.getDouble(3); if ( rSet.wasNull() ) rd.maxX = Double.MIN_VALUE;
                rd.maxY  = rSet.getDouble(4); if ( rSet.wasNull() ) rd.maxY = Double.MIN_VALUE;
            }
            rSet.close();
            rSet = null;
            psTable.close();
            psTable = null;
        } catch (SQLException sqle) {
            throw new SQLException( sqle );
        }
        return new Envelope(rd);
    }

    /** ============================================================   
      * =========         END EXTENT METHODS           =============
    ***/
  
    /**
     * @method getRowCount
     * @param _conn
     * @param _schemaName
     * @param _objectName
     * @param _whereClause 
     *        Limiting WHERE clause (optionally can have WHERE prefix). 
     *        Should not use any table aliases in where clause predicates eg t.geom is not null. Only "geom is not null"
     * @return number of rows
     * @throws SQLException
     * @throws IllegalArgumentException
     * @author Simon Greener, July 2010, Original coding.
     * @author Simon Greener, 9th July, Added where clause support.
     */
    public static int getRowCount(Connection _conn, 
                                  String     _schemaName,
                                  String     _objectName,
                                  String     _whereClause) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( propertyManager == null ) 
            propertyManager = new PropertiesManager(propertiesFile);
        
        if ( _conn==null )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","RTree Extent SQL"));
        if (Strings.isEmpty(_schemaName) ) 
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                                                      propertyManager.getMsg("METADATA_TABLE_COLUMN_0")));
        if (Strings.isEmpty(_objectName) )
            throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                                                      propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));

        if (Strings.isEmpty(_objectName) ) 
            throw new IllegalArgumentException("Table/View name must be supplied");
        if (Strings.isEmpty(_schemaName) )
            throw new IllegalArgumentException("Schema name must be supplied");
        
        String whereClause = _whereClause;
        if ( !Strings.isEmpty(_whereClause) && ( ! _whereClause.toUpperCase().startsWith("WHERE") ) )
            whereClause = "WHERE " + _whereClause;
        String sql = "SELECT count(*) \n" +
                     "  FROM " +
            Strings.append(_schemaName,_objectName,Constants.TABLE_COLUMN_SEPARATOR) + " t \n" +
                     (Strings.isEmpty(whereClause) ? "" : whereClause );
        int totalRows = 0;
        try {
            LOGGER.logSQL(sql);

            OracleStatement st = (OracleStatement)_conn.createStatement();
            OracleResultSet rSet = (OracleResultSet)st.executeQuery(sql);
            if (rSet.next()) {
                totalRows = rSet.getInt(1);
            } else {
                totalRows = -1;
            }
            rSet.close(); rSet = null;
            st.close(); st = null;
        } catch (SQLException sqle) {
          throw new SQLException(sql, sqle.getSQLState(), sqle.getErrorCode());
        }
        return totalRows;
    }

    /**
     * @method getSdoVersion
     * @param _conn
     * @return
     * @throws SQLException
     * @throws IllegalArgumentException
     * @author Simon Greener, July 2nd 2010, Original Coding.
     */
    public static String getSdoVersion(Connection _conn) 
    throws SQLException,
           IllegalArgumentException
    {
        OracleStatement st = (OracleStatement)_conn.createStatement();
        String sql = "select sdo_version() from dual";
        OracleResultSet rSet = (OracleResultSet)st.executeQuery(sql);
        String version = null;
        if (rSet.next()) {
            version = rSet.getString(1);
            if ( rSet.wasNull() )
                version = null;
        } 
        rSet.close(); rSet = null;
        st.close(); st = null;
        return version;
    }

    private static int sridAsInteger(String _srid) {
        return Strings.isEmpty(_srid) || _srid.equals(Constants.NULL) 
               ? Constants.SRID_NULL 
               : Integer.valueOf(_srid).intValue();
    }

    public static boolean querySridGeodetic(Connection _conn,
                                            String     _srid) 
    throws IllegalArgumentException, 
           Exception 
    {
      if ( propertyManager == null ) 
          propertyManager = new PropertiesManager(propertiesFile);

      if (Strings.isEmpty(_srid))
        throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                           propertyManager.getMsg("METADATA_TABLE_COLUMN_3")));

      if (_srid.equalsIgnoreCase(Constants.NULL) )
          return false;

      if (_conn == null)
         throw new IllegalArgumentException(propertyManager.getMsg(propertyManager.getMsg("MD_NO_CONNECTION_FOR","SRID SQL")));

      String sql = "SELECT COUNT(*) " +
                   "  FROM MDSYS.GEODETIC_SRIDS " +
                   " WHERE SRID = ?";
      int srid = sridAsInteger(_srid);
      PreparedStatement pStmt = _conn.prepareStatement(sql);
      pStmt.setInt(1,srid);
      LOGGER.logSQL(sql + "\n? = " + _srid);
      
      ResultSet rs = pStmt.executeQuery();
      rs.next();
      int cnt = rs.getInt(1);
      rs.close();
      pStmt.close();
      return (cnt != 0);
  }

  public static String getSRIDWKT(Connection _conn,
                                  String     _srid) 
           throws IllegalArgumentException, 
                  Exception 
  {
      if ( propertyManager == null ) 
        propertyManager = new PropertiesManager(propertiesFile);

      if (Strings.isEmpty(_srid))
        throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                           propertyManager.getMsg("METADATA_TABLE_COLUMN_3")));
  
      if (_srid.equalsIgnoreCase(Constants.NULL) )
          return "";

      if (_conn == null)
         throw new IllegalArgumentException(propertyManager.getMsg(propertyManager.getMsg("MD_NO_CONNECTION_FOR","SRID WKT SQL")));
          
      String sql = "SELECT a.wktext " +
                   "  FROM mdsys.cs_srs a " +
                   " WHERE a.SRID = ?";
      int srid = sridAsInteger(_srid);
      PreparedStatement psStmt = _conn.prepareStatement(sql);
      psStmt.setInt(1,srid);
      LOGGER.logSQL(sql + "\n? = " + _srid);
      
      String wkt = "";
      ResultSet rs = psStmt.executeQuery();
      if (rs.next()) {
        wkt = rs.getString(1);
        if ( rs.wasNull() )
            wkt = null;
      }
      rs.close();     rs = null;
      psStmt.close(); psStmt = null;
      return wkt;
  }

  public static String getSRIDRefSysKind(Connection _conn,
                                         String     _srid) 
  throws SQLException,
         IllegalArgumentException
  {
      if ( propertyManager == null ) 
          propertyManager = new PropertiesManager(propertiesFile);
      
      if (Strings.isEmpty(_srid) )
          return Constants.SRID_TYPE.UNKNOWN.toString();
          
      if ( _srid.equalsIgnoreCase(Constants.NULL) )
          return Constants.SRID_TYPE.PROJECTED.toString();

      if ( _conn == null ) 
          throw new IllegalArgumentException(propertyManager.getMsg("SRID_REF_SYS_CONNECTION_ERROR",_srid));
      
      String refSysKind = "";
      String sql = 
      "select Case When Crs.Coord_Ref_Sys_Kind Is Not Null\n" + 
      "            Then Case When (select wktext from cs_srs s where s.srid = crs.srid) Like 'GEOGCS%'\n" + 
      "                      Then 'GEODETIC_' \n" + 
      "                      Else '' \n" + 
      "                  End\n" + 
      "            Else 'UNKNOWN'\n" + 
      "        End || \n" + 
      "       DECODE(crs.coord_ref_sys_kind,'GEOGENTRIC','GEOCENTRIC',crs.coord_ref_sys_kind) \n" + 
      "       as unit_of_measure \n" + 
      "  from mdsys.sdo_coord_ref_system crs\n" + 
      " where crs.srid = ?";
      
      int srid = sridAsInteger(_srid);
      PreparedStatement ps = _conn.prepareStatement(sql);
      ps.setInt(1,srid);
      LOGGER.logSQL(sql + "\n? = " + _srid);

      ps.setFetchSize(100); // default is 10
      ps.setFetchDirection(ResultSet.FETCH_FORWARD);
      ResultSet rSet = ps.executeQuery();
      if (rSet.next()) {
        refSysKind = rSet.getString(1); if ( rSet.wasNull() ) refSysKind = Constants.SRID_TYPE.UNKNOWN.toString();
      }
      rSet.close(); rSet = null;
      ps.close(); ps = null;
                  
      return refSysKind;
    }
    
  public static String getSRIDBaseUnitOfMeasure(Connection _conn,
                                                String     _srid) 
  throws SQLException,
         IllegalArgumentException
  {
      if ( propertyManager == null ) 
          propertyManager = new PropertiesManager(propertiesFile);

      if (Strings.isEmpty(_srid) || _srid.equalsIgnoreCase(Constants.NULL) )
          return "M";

      if ( _conn == null ) 
          throw new IllegalArgumentException(propertyManager.getMsg("UNIT_OF_MEASURE_CONNECTION_ERROR",_srid));      
      
      String unitOfMeasure = "";
      String sql = 
      "select case when crs.coord_ref_sys_kind in ('GEOCENTRIC','GEOGRAPHIC2D','GEOGENTRIC','GEOGRAPHIC3D') \n" + 
      "            then (select uom.unit_of_meas_name \n" + 
      "                    from mdsys.sdo_datums dat,\n" + 
      "                         mdsys.sdo_ellipsoids ell,\n" + 
      "                         mdsys.sdo_units_of_measure uom \n" + 
      "                   where dat.datum_id = crs.datum_id \n" + 
      "                     and ell.ellipsoid_id = dat.ellipsoid_id\n" + 
      "                     and ell.uom_id is not null\n" + 
      "                     and uom.uom_id = ell.uom_id\n" + 
      "                     and uom.unit_of_meas_type = 'length') \n" + 
      "            else (select uom.unit_of_meas_name \n" + 
      "                    from mdsys.sdo_coord_axes ca,\n" + 
      "                         mdsys.sdo_units_of_measure uom\n" + 
      "                   where ca.coord_sys_id = crs.coord_sys_id\n" + 
      "                     and ca.\"ORDER\" = 1\n" + 
      "                     and uom.uom_id = ca.uom_id \n" + 
      "                     and uom.unit_of_meas_type = 'length')\n" + 
      "            end as unit_of_measure \n" + 
      "  from mdsys.sdo_coord_ref_system crs\n" + 
      " where crs.srid = ?";

      int srid = sridAsInteger(_srid);
      PreparedStatement ps = _conn.prepareStatement(sql);
      ps.setInt(1,srid);
      LOGGER.logSQL(sql + "\n? = " + _srid);

      ps.setFetchSize(100); // default is 10
      ps.setFetchDirection(ResultSet.FETCH_FORWARD);
      ResultSet rSet = ps.executeQuery();
      if (rSet.next()) {
        unitOfMeasure = rSet.getString(1); if ( rSet.wasNull() ) unitOfMeasure = "M";
      }
      rSet.close(); rSet = null;
      ps.close(); ps = null;
      return unitOfMeasure;
    }

  public static LinkedHashMap<String, String> getUnitsOfMeasure(Connection _conn,
                                                                boolean    _length) 
  throws SQLException,
         IllegalArgumentException
  {
      if ( propertyManager == null ) 
          propertyManager = new PropertiesManager(propertiesFile);

      if ( _conn == null ) 
          throw new IllegalArgumentException(propertyManager.getMsg("UNITS_OF_MEASURE_CONNECTION_ERROR"));
      
      LinkedHashMap<String, String> unitsOfMeasure = new LinkedHashMap<String, String>();
      // Query table on which SDO_DIST_UNITS and SDO_AREA_UNITS based
      String sql =  
      "select distinct uom.short_name as SDO_UNIT,\n" +
      "                uom.unit_of_meas_name \n" + 
      "  from mdsys.sdo_units_of_measure uom \n" + 
      " where lower(uom.unit_of_meas_type) = ? \n" + 
      "   and uom.short_name is not null\n" +
      "   and uom.short_name <> uom.unit_of_meas_name\n" +   // gets rid of SDO_UNITS that don't work like British foot (1936)
      "  order by 1";
      PreparedStatement ps = _conn.prepareStatement(sql);
      ps.setString(1,_length ? "length" : "area");
      LOGGER.logSQL(sql + 
                    "\n? = " + (_length ? "length" : "area"));

      ps.setFetchSize(100); // default is 10
      ps.setFetchDirection(ResultSet.FETCH_FORWARD);
      ResultSet rSet = ps.executeQuery();
      while (rSet.next()) {
          unitsOfMeasure.put(rSet.getString(1),rSet.getString(2));
      }
      rSet.close(); rSet = null;
      ps.close(); ps = null;

      return unitsOfMeasure;
  }    

    public static String getSrsNames(Connection _conn,
                                     int        _srid,
                                     String     _separator,
                                     boolean    _insert) 
      throws SQLException
    {
        String sql = "select 'select' as source_object,\n" +
                     "       scrs.srid,\n" +
                     "       'x-ogc:def:'         || case when data_source is null then 'SDO' else UPPER(scrs.data_source) end || ':' || scrs.srid as srsname,\n" + 
                     "       'urn:x-ogc:def:crs:' || case when data_source is null then 'SDO' else UPPER(scrs.data_source) end as srsnamespace\n" +
                     "  from sdo_coord_ref_system scrs\n" +
                     " where scrs.srid = ?\n" +
                     "   and not exists (select 1 from MDSYS.SrsNameSpace_Table snst where snst.sdo_srid = scrs.srid)\n" +
                     "union all\n" +
                     "select 'table' as source_object,\n" +
                     "       snst.sdo_srid,\n" +
                     "       snst.srsname, \n" +
                     "       snst.srsnamespace \n" +
                     "  from MDSYS.SrsNameSpace_Table snst \n" +
                     " where snst.sdo_srid = ?\n" +
                     "   and exists (select 1 from MDSYS.SrsNameSpace_Table snst1 where snst1.sdo_srid = snst.sdo_srid)";
        PreparedStatement ps = _conn.prepareStatement(sql);
        ps.setInt(1,_srid);
        ps.setInt(2,_srid);
        LOGGER.logSQL(sql + 
                      "\n? = " + _srid +
                      "\n? = " + _srid );
        
        ps.setFetchSize(100); // default is 10
        ps.setFetchDirection(ResultSet.FETCH_FORWARD);
        ResultSet rSet = ps.executeQuery();
        String sep = Strings.isEmpty(_separator) ? "," : _separator;
        String sourceObject = "", srsName = "", srsNamespace = "";
        int sdo_srid = 0;
        if (rSet.next()) {
            sourceObject = rSet.getString(1);
            sdo_srid     = rSet.getInt(2);
            srsName      = rSet.getString(3);
            srsNamespace = rSet.getString(4);
        }
        rSet.close(); rSet = null;
        ps.close(); ps = null;
        
        // Do we need to insert into table?
        if ( _insert ) {
            if ( !Strings.isEmpty(sourceObject) && sourceObject.equalsIgnoreCase("select") ) {
              // SQL
              sql = "insert into MDSYS.SrsNameSpace_Table (sdo_srid,srsname,srsnamespace) values (?,?,?)";
              PreparedStatement insertPS = _conn.prepareStatement(sql);
              insertPS.setInt(1,sdo_srid);
              insertPS.setString(2,srsName);
              insertPS.setString(3,srsNamespace);
              insertPS.execute();
              _conn.commit(); 
              insertPS.close(); insertPS = null;
            }
        }
        return srsName + sep + srsNamespace;
    }
    
    public static String getCharacterSet(Connection _conn) 
    throws SQLException,
           IllegalArgumentException
    {
        if ( _conn == null ) {
            throw new IllegalArgumentException(propertyManager.getMsg("GET_CHARACTER_SET_CONNECTION"));
        }
        String characterSet = "UTF-8";
        Statement stmt = _conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT value FROM NLS_DATABASE_PARAMETERS WHERE parameter = 'NLS_CHARACTERSET'");
        if ( rs.next() ) {
            characterSet = rs.getString(1);
        }
        rs.close();
        stmt.close();
        return characterSet;
    }

  /**
   * @function getLayerSRID
   * @precis   Get's sample of first SRID in table/view/mview
   * @param _conn
   * @param _schemaName
   * @param _objectName
   * @param _columnName
   * @return SRID as number else -9999
   * @throws SQLException
   * @author Simon Greener, April 21st 2010, Original coding
   */
  public static int getLayerSRID(Connection _conn,
                                 String _schemaName,
                                 String _objectName,
                                 String _columnName) 
  throws SQLException,
         IllegalArgumentException
  {
      if ( propertyManager == null ) 
          propertyManager = new PropertiesManager(propertiesFile);
      
      if ( _conn==null )
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_CONNECTION_FOR","object_type SQL"));
      
      if (Strings.isEmpty(_objectName) ) 
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                             propertyManager.getMsg("METADATA_TABLE_COLUMN_1")));
      if (Strings.isEmpty(_columnName) )
          throw new IllegalArgumentException(propertyManager.getMsg("MD_NO_OBJECT_NAME",
                                             propertyManager.getMsg("METADATA_TABLE_COLUMN_2")));
      String sql = "select srid from (\n" +
                   "select DISTINCT a." + _columnName + ".sdo_srid as srid\n" +
                   "  from " +
            Strings.append(_schemaName,_objectName,Constants.TABLE_COLUMN_SEPARATOR) + " a \n" +
                   " where rownum < 1000\n" +
                   " order by srid asc\n" +
                   ")\n " +
                   "where rownum < 2";

      int srid = Constants.SRID_NULL;
      LOGGER.logSQL(sql);
      Statement st = _conn.createStatement();
      ResultSet rSet = st.executeQuery(sql);
      if (rSet.next()) {
          srid = rSet.getInt(1);
          srid = rSet.wasNull() ? Constants.SRID_NULL : srid;
      } else {
          srid = -9999;
      }
      rSet.close(); rSet = null;
      st.close(); st = null;
      return srid;
  }
    
}
