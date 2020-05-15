package org.GeoRaptor.sql;

import java.io.IOException;
import java.io.Reader;

import java.math.BigDecimal;

import java.sql.Date;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import java.text.DecimalFormat;

import oracle.dbtools.raptor.datatypes.oracle.plsql.BOOLEAN;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;

import oracle.sql.BINARY_DOUBLE;
import oracle.sql.BINARY_FLOAT;
import oracle.sql.CHAR;
import oracle.sql.CLOB;
import oracle.sql.DATE;
import oracle.sql.INTERVALDS;
import oracle.sql.INTERVALYM;
import oracle.sql.NCLOB;
import oracle.sql.NUMBER;
import oracle.sql.ROWID;
import oracle.sql.TIMESTAMP;
import oracle.sql.TIMESTAMPLTZ;
import oracle.sql.TIMESTAMPTZ;

import org.GeoRaptor.Constants;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.SpatialRenderer;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;

import org.geotools.util.logging.Logger;


@SuppressWarnings("deprecation")
public class SQLConversionTools {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.sql.SQLConversionTools");
    
    public SQLConversionTools() {
        super();
    }
    
    public static String convertToString(OracleConnection _conn,
                                         OracleResultSet  _ors,
                                         int              _col) 
    {
        // Null checks    
        if ( _ors == null ) {
            return null;
        }
        String value = "";
        try {
            if (_ors.wasNull()) {
                return null;
            }
            
            OracleResultSetMetaData meta = (OracleResultSetMetaData)_ors.getMetaData();
            OracleConnection conn = (_conn == null) ? DatabaseConnections.getInstance().getAnyOpenConnection() : _conn;
/**
System.out.println("convertToString(conn,obj,col) - " + 
                   meta.getColumnLabel(_col) + "," + 
                   meta.getColumnDisplaySize(_col) + "," + 
                   meta.getColumnType(_col) + "/" + 
                   meta.getColumnTypeName(_col) + ",("+
                   meta.getPrecision(_col)+ "," +meta.getScale(_col) +")");
**/ 
          DecimalFormat df = Tools.getDecimalFormatter(meta.getScale(_col)==0?-1:meta.getScale(_col), false);
          Reader in = null;
          CHAR CharString = null;
          df = Tools.getDecimalFormatter(meta.getScale(_col)==0?-1:meta.getScale(_col), false);
          switch ( meta.getColumnType(_col)) {
              case Types.ROWID       : value = _ors.getROWID(_col).stringValue(); break;
              
              // CHAR values retain the same same representation as they have in the database, so there can be no loss of information through conversion. 
              case OracleTypes.NCHAR    : 
              case OracleTypes.NVARCHAR : value = _ors.getNString(_col); break;
              case Types.CHAR           : 
              case Types.VARCHAR        : CharString = _ors.getCHAR(_col);
                                          value = CharString.stringValue();
                                          break;
              
              case Types.BOOLEAN     : value = _ors.getObject(_col).toString(); break; 
              case OracleTypes.BFILE :
              case OracleTypes.RAW   :
              case Types.BLOB        : break;
              
              case Types.NCLOB :
                  NClob nClob = _ors.getNClob(_col);
                  in = nClob.getCharacterStream(  );
                try {
                    value = "";
                    int length = -1;
                    char[] buffer = new char[1024];
                    while ((length = in.read(buffer)) != -1) {
                        value += String.valueOf(buffer).substring(0,length);
                    }
                    in.close( ); 
                    in = null; 
                    value = (nClob.length()<=1000?value:value.substring(0,1000)) + " ... ";
                 } catch (IOException e) {
                    value = null;
                 }
                 break;
                case Types.CLOB  :
                   CLOB sClob = _ors.getCLOB(_col);
                   in = sClob.getCharacterStream();
                  try {
                      value = "";
                      int length = -1;
                      char[] buffer = new char[1024];
                      while ((length = in.read(buffer)) != -1) {
                          value += String.valueOf(buffer).substring(0,length);
                      }
                      in.close( ); 
                      in = null; 
                      value= (sClob.length()<=1000?value:value.substring(0,1000)) + " ... ";
                   } catch (IOException e) {
                      value = null;
                   }
                   break;
          
            case Types.TINYINT      : /* Integer data from 0 through 255. Storage size is 1 byte. */
            case Types.SMALLINT     : /* Integer data from -2^15 (-32,768) through 2^15 - 1 (32,767). Storage size is 2 bytes. */
            case Types.BIGINT       : /* Integer (whole number) data from -2^63 (-9,223,372,036,854,775,808) through 2^63-1 (9,223,372,036,854,775,807). Storage size is 8 bytes. */
            case Types.INTEGER      : /* Integer (whole number) data from -2^31 (-2,147,483,648) through 2^31 - 1 (2,147,483,647). Storage size is 4 bytes. The SQL-92 synonym for int is integer. */
            case Types.FLOAT        : 
            case Types.DOUBLE       : 
            case Types.DECIMAL      : 
            case OracleTypes.NUMBER : value = _ors.getNUMBER(_col).stringValue();  break;
          
            case OracleTypes.BINARY_DOUBLE : BINARY_DOUBLE bdbl = new BINARY_DOUBLE(_ors.getOracleObject(_col).getBytes()); 
                                             value = df.format(new Double(bdbl.stringValue()));
                                             break;
            case OracleTypes.BINARY_FLOAT  : BINARY_FLOAT  bflt = (BINARY_FLOAT)_ors.getOracleObject(_col);  
                                             value = df.format(new Float(bflt.stringValue())); 
                                             break; 
            case OracleTypes.TIMESTAMPTZ   : value = _ors.getTIMESTAMPTZ(_col).stringValue(conn); break;
            case OracleTypes.TIMESTAMPLTZ  : value = _ors.getTIMESTAMPLTZ(_col).stringValue(conn);  break;
            case OracleTypes.INTERVALYM    : value = _ors.getINTERVALYM(_col).stringValue();   break;
            case OracleTypes.INTERVALDS    : value = _ors.getINTERVALDS(_col).stringValue();   break;
           
            case OracleTypes.DATE          : value = _ors.getDATE(_col).stringValue();  
                                             break;
            case OracleTypes.TIMESTAMP     :
              if ( meta.getColumnTypeName(_col).equalsIgnoreCase("DATE") ) {
                DATE d = _ors.getDATE(_col); 
                try {
                    value = d.stringValue();
                } catch (Exception e) {
                    value = d.stringValue();
                }
              } else {
                  value = _ors.getTIMESTAMP(_col).stringValue();
              }
              break;
            case Types.TIME                : value = _ors.getTime(_col).toString(); break;
            
            case OracleTypes.STRUCT : 
              
              SpatialRenderer renderer = SpatialRenderer.getInstance();
              if ( meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ||
                   meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) ||
                   meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) ) {
                  value = renderer.renderGeoObject(_ors.getOracleObject(_col),false);
              }
              break;
            
            default : LOGGER.warn("Not handled: " + 
                                  meta.getColumnLabel(_col) + "," + 
                                  meta.getColumnType(_col) + "," +
                                  meta.getColumnTypeName(_col) + "," + 
                                  meta.getPrecision(_col) + "," + 
                                  meta.getScale(_col));
                      value = null;
          }
      } catch (Exception e) {
          value = null;
      }
      return value;
  }

  public static String convertToString(OracleConnection       _conn,
                                       Object                 _object,
                                       OraRowSetMetaDataImpl  _meta) 
  {
      // Check parameters and load constantly accessed metadata elements to variables.
      String columnName        = "";
      String columnTypeName    = "";
      int    columnType        = -1;
      int    dataTypePrecision = 0;
      int    dataTypeScale     = 0;
      try {
          columnTypeName    = _meta.getColumnTypeName(1);
          columnType        = _meta.getColumnType(1);
          if ( _object == null ) {
              return Preferences.getInstance().getNullValue(columnType);
          }
          dataTypeScale     = _meta.getScale(1);
          columnName        = _meta.getColumnName(1);
          dataTypePrecision = _meta.getPrecision(1);
          // LOGGER.info("convertToString(conn,obj,meta) - " + _object.getClass() + " : " + columnTypeName + "/" +columnType + "," +dataTypePrecision+ "," +dataTypeScale);
      } catch (SQLException sqle) {
          LOGGER.error("convertToString: Failed to access metadata needed to convert column " + columnName + " (" + sqle.getMessage());
          return "";
      }
      
      try {
          // Rough mapping as only having access to actual data will help determine correct value
          // ie 255 will be TINYINT but 999 would not be
          //
          DecimalFormat df = Tools.getDecimalFormatter(dataTypeScale==0?-1:dataTypeScale, false);
          Reader in = null;
          OracleConnection conn = (_conn == null) ? DatabaseConnections.getInstance().getAnyOpenConnection()  : _conn;
          switch (columnType) 
          {
            case Types.ROWID       : return ((ROWID)_object).stringValue(); 
            case Types.BOOLEAN     : return ((BOOLEAN)_object).toString();
            case OracleTypes.BFILE :
            case OracleTypes.RAW   :
            case Types.BLOB        : return "";
    
            case Types.NCLOB : in = ((NCLOB)_object).getCharacterStream(  );
            try {
                String nClob = "";
                int length = (int)((NCLOB)_object).length();
                char[] buffer = new char[1024];
                while ((length = in.read(buffer)) != -1) {
                    nClob += String.valueOf(buffer).substring(0,length);
                }
                in.close( ); 
                in = null; 
                return (nClob.length()<=1000?nClob:nClob.substring(0,1000)) + " ... ";
             } catch (IOException e) {
                return "";
             }
            case Types.CLOB  : in = ((CLOB)_object).getCharacterStream(  );
              try {
                  String sClob = "";
                  int length = (int)((CLOB)_object).length();
                  char[] buffer = new char[1024];
                  while ((length = in.read(buffer)) != -1) {
                      sClob += String.valueOf(buffer).substring(0,length);
                  }
                  in.close( ); 
                  in = null; 
                  return (sClob.length()<=1000?sClob:sClob.substring(0,1000)) + " ... ";
               } catch (IOException e) {
                  return "";
               }
            case Types.NCHAR    : 
            case Types.CHAR     : CHAR ch = (CHAR)_object;
                                  return ch.getString(); 
            case Types.NVARCHAR : 
            case Types.VARCHAR  :
              // Try CHAR based conversion first
              String retStr = "";
              try { 
                  retStr = ((CHAR)_object).getString();
              } catch ( Exception e ) {
                  retStr = (String)_object; 
              }
              return retStr;
            case Types.TINYINT      : /* Integer data from 0 through 255. Storage size is 1 byte. */
            case Types.SMALLINT     : /* Integer data from -2^15 (-32,768) through 2^15 - 1 (32,767). Storage size is 2 bytes. */
            case Types.BIGINT       : /* Integer (whole number) data from -2^63 (-9,223,372,036,854,775,808) through 2^63-1 (9,223,372,036,854,775,807). Storage size is 8 bytes. */
            case Types.INTEGER      : /* Integer (whole number) data from -2^31 (-2,147,483,648) through 2^31 - 1 (2,147,483,647). Storage size is 4 bytes. The SQL-92 synonym for int is integer. */
            case Types.FLOAT        : 
            case Types.DOUBLE       : 
            case Types.DECIMAL      : 
            case OracleTypes.NUMBER :
              BigDecimal bd = null;
              if ( _object instanceof java.math.BigDecimal ) {
                  bd = (BigDecimal)_object;
                  return df.format(_object);
              }
              NUMBER num = (NUMBER)_object;
              if ( dataTypeScale == 0 ) {
                  if (dataTypePrecision == 0)  { Long  l = new Long(-1);          if ( num.isConvertibleTo(l.getClass()) ) return String.valueOf(num.longValue()); }
                  if (dataTypePrecision <= 3 ) { Byte  b   = new Byte((byte)255); if ( num.isConvertibleTo(b.getClass()) ) return String.valueOf(num.byteValue()); } 
                  if (dataTypePrecision <= 5 ) { Short s   = -1;                  if ( num.isConvertibleTo(s.getClass()) ) return String.valueOf(num.shortValue()); } 
                  if (dataTypePrecision <= 9 ) { Integer i = new Integer(-1);     if ( num.isConvertibleTo(i.getClass()) ) return String.valueOf(num.intValue()); }
                  Long l = new Long(-1); 
                  if ( num.isConvertibleTo(l.getClass()) ) return String.valueOf(num.longValue());
              }
              if ( dataTypePrecision <= 63  ) { Float  f  = new Float(Float.NaN);   if ( num.isConvertibleTo(f.getClass()) ) return String.valueOf(df.format(num.floatValue())); }
              if ( dataTypePrecision == 126 ) { Double d  = new Double(Double.NaN); if ( num.isConvertibleTo(d.getClass()) ) return String.valueOf(df.format(num.doubleValue())); }
              bd = new BigDecimal(Double.NaN);
              if ( num.isConvertibleTo(bd.getClass()) ) return String.valueOf(df.format(num.bigDecimalValue()));
              return num.stringValue();
            case OracleTypes.BINARY_DOUBLE : BINARY_DOUBLE bdbl = (BINARY_DOUBLE)_object; return df.format(new Double(bdbl.stringValue())); // bdbl.doubleValue(); throws "Conversion to double failed" 
            case OracleTypes.BINARY_FLOAT  : BINARY_FLOAT  bflt =  (BINARY_FLOAT)_object; return df.format(new Float(bflt.stringValue())); // bflt.floatValue();  throws "Conversion to double failed" 
            case OracleTypes.TIMESTAMPTZ   : return((TIMESTAMPTZ)_object).stringValue(conn);
            case OracleTypes.TIMESTAMPLTZ  : return((TIMESTAMPLTZ)_object).stringValue(conn);
            case OracleTypes.INTERVALYM    : return((INTERVALYM)_object).stringValue();  
            case OracleTypes.INTERVALDS    : return((INTERVALDS)_object).stringValue();
            case Types.TIME                :
            case OracleTypes.TIMESTAMP     :
            case OracleTypes.DATE          :
                if ( _object instanceof String && columnTypeName.equalsIgnoreCase("DATE")) {
                    return (String)_object;
                }
                TIMESTAMP ts = (TIMESTAMP) _object;
                Timestamp ti = new Timestamp(1000000); if ( ts.isConvertibleTo(ti.getClass()) ) { return String.valueOf(ts.timestampValue().toString()); }
                Date dt = new Date(20100130);          if ( ts.isConvertibleTo(dt.getClass()) ) { return String.valueOf(ts.dateValue().toString()); }
                Time tm = new Time(1500);              if ( ts.isConvertibleTo(tm.getClass()) ) { return String.valueOf(ts.timeValue().toString()); }
                return (String)_object;
          
            case OracleTypes.STRUCT : 
              SpatialRenderer renderer = SpatialRenderer.getInstance();
              if ( columnTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ||
                   columnTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) ||
                   columnTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) )
                  return renderer.renderGeoObject(_object,false);
          
            default : 
              LOGGER.warn("convertToString did not handle " + columnName +"," + columnTypeName +","+dataTypePrecision+ "," +dataTypeScale);
              return null;
          }
       } catch (SQLException e) {
           LOGGER.warn("convertToString: Error converting column value " + columnName +"," + columnTypeName +","+dataTypePrecision+ "," +dataTypeScale + " (" + e.getMessage() + ")");
           return null;
       }
    }

    public static String convertToString(OracleConnection _conn,
                                         String           _column,
                                         Object           _object) 
    {
        if ( _object==null ) { return ""; }
        try 
        {
            OracleConnection conn = (_conn == null) 
                ? DatabaseConnections.getInstance().getAnyOpenConnection() 
                : _conn;
            if ( _object instanceof oracle.sql.NUMBER ) {
                NUMBER num = (NUMBER)_object;
                /* Integer i = new Integer(-1); if ( num.isConvertibleTo(i.getClass()) ) i = num.intValue();
                   Short s   = -1; if ( num.isConvertibleTo(s.getClass()) ) s = num.shortValue();
                   Long l    = new Long(-1); if ( num.isConvertibleTo(l.getClass()) ) l = num.longValue();
                   Double d  = new Double(Double.NaN); if ( num.isConvertibleTo(d.getClass()) ) d = num.doubleValue();
                   Date dt   = Date.valueOf("20000101"); if ( num.isConvertibleTo(dt.getClass()) ) dt = num.dateValue();
                */
                return num.stringValue();
            } else if (_object instanceof oracle.sql.ROWID ) {
                return ((ROWID)_object).stringValue();
            } else if (_object instanceof oracle.sql.CLOB) { 
                String sClob = "";
                Reader in = ((CLOB)_object).getCharacterStream(  );
                int length = (int)((CLOB)_object).length();
                char[] buffer = new char[1024];
                try {
                    while ((length = in.read(buffer)) != -1) {
                        sClob += String.valueOf(buffer).substring(0,length);
                    }
                } catch (IOException e) {
                }
                try {in.close( ); } catch (IOException e) {}
                in = null; 
                return (sClob.length()<=1000?sClob:sClob.substring(0,1000)) + " ... ";
            } else if (_object instanceof oracle.sql.CHAR) {
                return ((CHAR)_object).stringValue();
            } else if (_object instanceof oracle.sql.DATE || _object instanceof Date ) {
                return ((DATE)_object).toString();
            } else if (_object instanceof oracle.sql.TIMESTAMP) {
                return((TIMESTAMP)_object).stringValue();
            } else if (_object instanceof oracle.sql.TIMESTAMPTZ) {
                return((TIMESTAMPTZ)_object).stringValue(conn);
            } else if (_object instanceof oracle.sql.TIMESTAMPLTZ) {
                return((TIMESTAMPLTZ)_object).stringValue(conn);
            } else if (_object instanceof oracle.sql.INTERVALDS) {
                return((INTERVALDS)_object).stringValue();
            } else if (_object instanceof oracle.sql.INTERVALYM) {
                return((INTERVALYM)_object).stringValue();
            } else if (_object instanceof oracle.sql.BINARY_DOUBLE) {
                return((BINARY_DOUBLE)_object).stringValue();
            } else if (_object instanceof oracle.sql.BINARY_FLOAT) {
                return((BINARY_FLOAT)_object).stringValue();
            } else if (_object instanceof java.lang.String) {
                return String.valueOf(_object);
            } else if ( _object instanceof BigDecimal ) {
                return ((BigDecimal)_object).toPlainString();
            } else if ( _object instanceof Integer ) {
                return ((Integer)_object).toString();
            } else if ( _object instanceof Number ) {
                return ((Number)_object).toString();
            } else if ( _object instanceof Timestamp ) {
                return ((Timestamp)_object).toString();
            } else if ( _object instanceof RowId ) {
                return ((RowId)_object).toString();
            } else if (_object instanceof java.lang.Object) {
                // Seems we get this when we have all null values in a column
                return null;
            } else {
                LOGGER.warn("GeoRaptor: " + _column + "'s data type " + _object.getClass().getName() + " is unsupported.");
            }
      } catch (SQLException sqle) {
        LOGGER.warn("GeoRaptor: Error converting column/type " + _column + "/" + _object.getClass().getName());
      }
      return null;
    }

    public static boolean isOracleSpecialDataType(String _dataType) {
        if (Strings.isEmpty(_dataType) )
            return false;
        String dataType = _dataType.toLowerCase().replace("class","").replace("oracle.sql.","").replace("java.lang.","").trim();
        if (  dataType.equalsIgnoreCase("INTERVALDS") ||
              dataType.equalsIgnoreCase("INTERVALYM") ||
              dataType.equalsIgnoreCase("BINARY_DOUBLE") ||
              dataType.equalsIgnoreCase("BINARY_FLOAT") ||
              dataType.equalsIgnoreCase("STRUCT")
           ) {
        return true;
      } else {
        return false;
      }  
    }
    
    
    /**
     * @method dataTypeIsSupported
     * @param _dataType
     * @return boolean
     * @author @author Simon Greener 12th December 2010 Original coding
     *          These data types can be used for labelling and display
     */
    public static boolean dataTypeIsSupported(String _dataType) 
    {
        if (Strings.isEmpty(_dataType) )
          return false;
        String dataType = _dataType.toLowerCase().replace("class","").replace("oracle.sql.","").replace("java.lang.","").trim();
        if ( dataType.equalsIgnoreCase("ROWID")      ||
             dataType.equalsIgnoreCase("CLOB")       || dataType.equalsIgnoreCase("NCLOB") || 
             dataType.equalsIgnoreCase("CHAR")       || dataType.equalsIgnoreCase("NCHAR") ||
             dataType.equalsIgnoreCase("VARCHAR")    || dataType.equalsIgnoreCase("NVARCHAR") ||
             dataType.equalsIgnoreCase("VARCHAR2")   || dataType.equalsIgnoreCase("NVARCHAR2") ||
             dataType.equalsIgnoreCase("STRING")     ||
             dataType.equalsIgnoreCase("DATE")       || dataType.equalsIgnoreCase("TIME")     ||
             dataType.equalsIgnoreCase("TIMESTAMP")  || dataType.equalsIgnoreCase("TIMESTAMPTZ") || dataType.equalsIgnoreCase("TIMESTAMPLTZ") ||
             dataType.equalsIgnoreCase("INTERVALDS") || dataType.equalsIgnoreCase("INTERVALYM") ||
             dataType.equalsIgnoreCase("NUMBER")     || dataType.equalsIgnoreCase("NUMERIC")     ||
             dataType.equalsIgnoreCase("DOUBLE")     || dataType.equalsIgnoreCase("BINARY_DOUBLE") ||
             dataType.equalsIgnoreCase("FLOAT")      || dataType.equalsIgnoreCase("BINARY_FLOAT") ||
             dataType.equalsIgnoreCase("INTEGER")    || dataType.equalsIgnoreCase("BIGINT") ||
             dataType.equalsIgnoreCase("STRUCT")
          ) {
          return true;
        } else {
          return false;
        }
    }

    /**
     * Determines if the given column type is defined/handled by this writer.  Undefined column types
     * are ignored by this writer.
     * @param columnType the oracle column type.
     * @return true if the column type is defined, otherwise false.
     */
    public static boolean isSupportedType(int    columnType,
                                          String columnTypeName) {
            return (columnType == OracleTypes.ROWID ||
                    columnType == Types.CHAR      || columnType == Types.NCHAR ||  
                    columnType == Types.CLOB      || columnType == Types.NCLOB ||
                    columnType == Types.VARCHAR   || columnType == Types.NVARCHAR ||  
                    columnType == Types.SMALLINT  || columnType == Types.TINYINT ||
                    columnType == Types.INTEGER   || columnType == Types.BIGINT ||  
                    columnType == Types.NUMERIC /* OracleTypes.NUMBER */ ||
                    columnType == Types.FLOAT     || columnType == OracleTypes.BINARY_FLOAT ||
                    columnType == Types.DOUBLE    || columnType == OracleTypes.BINARY_DOUBLE ||
                    columnType == Types.DATE      || columnType == Types.TIME      ||
                    columnType == OracleTypes.INTERVALDS || columnType == OracleTypes.INTERVALYM ||
                    columnType == Types.TIMESTAMP || columnType == OracleTypes.TIMESTAMPLTZ ||  columnType == OracleTypes.TIMESTAMPTZ ||
                (   columnType == OracleTypes.STRUCT &&
                    columnTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ||
                    columnTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) ||
                    columnTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) )
                );
    }

    public static String dataTypeToXSD(OracleResultSetMetaData _meta,
                                       int                     _column) 
    throws SQLException 
    {
        // Rough mapping as only having access to actual data will help determine correct value
        // ie 255 will be TINYINT but 999 would not be
        //
        switch (_meta.getColumnType(_column)) 
        {
          case Types.ROWID       : return "string"; 
          case Types.BOOLEAN     : return "boolean";
          case OracleTypes.BFILE :
          case OracleTypes.RAW   :
          case Types.BLOB        : return "base64Binary";

          case Types.CLOB     : return "clob";  // for handling of length
          case Types.CHAR     : 
          case Types.NCHAR    :
          case Types.NCLOB    :
          case Types.NVARCHAR :
          case Types.VARCHAR  : return "string"; 
          
          case Types.TINYINT  : return "byte";
              /* Integer data from 0 through 255. Storage size is 1 byte. */
          case Types.SMALLINT : return "short";
              /* Integer data from -2^15 (-32,768) through 2^15 - 1 (32,767). Storage size is 2 bytes. */
          case Types.INTEGER  : return "int"; 
              /* Integer (whole number) data from -2^31 (-2,147,483,648) through 2^31 - 1 (2,147,483,647). Storage size is 4 bytes. The SQL-92 synonym for int is integer. */
          case Types.BIGINT   : return "long"; // or Decimal?
              /* Integer (whole number) data from -2^63 (-9,223,372,036,854,775,808) through 2^63-1 (9,223,372,036,854,775,807). Storage size is 8 bytes. */
        
          case Types.FLOAT    : return "float";
          case Types.DOUBLE   : return "double";
        
          case OracleTypes.BINARY_FLOAT  : return "float";   // 32bit binary number
          case OracleTypes.BINARY_DOUBLE : return "double";  // 64 bit binary float
                                            
        
          case Types.DECIMAL : 
          case OracleTypes.NUMBER :
              if ( _meta.getScale(_column) == 0 ) {
                       if ( _meta.getPrecision(_column) < 3 )  return "byte";
                  else if ( _meta.getPrecision(_column) < 5 )  return "short";
                  else if ( _meta.getPrecision(_column) < 10 ) return "long";
                  else                                         return "integer";
              } else {
                       if ( _meta.getPrecision(_column) == 63  ) return "float";
                  else if ( _meta.getPrecision(_column) == 126 ) return "double";
                  else return "decimal"; 
              }

          case Types.DATE               : return "date"; 
          case Types.TIME               : return "time"; 
          case Types.TIMESTAMP          : if ( _meta.getScale(_column) != 0 ) return "dateTime"; else return "date";
          case OracleTypes.TIMESTAMPLTZ : return "string";
          case OracleTypes.TIMESTAMPTZ  : return "dateTime";
          case OracleTypes.INTERVALDS   : return "string"; // could be "duration" 
          case OracleTypes.INTERVALYM   : return "string";  
        
          case OracleTypes.STRUCT : if ( _meta.getColumnTypeName(_column).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY) ||
                                         _meta.getColumnTypeName(_column).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE) ||
                                         _meta.getColumnTypeName(_column).equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE) )
                                    return "string";

          default : LOGGER.warn("(Tools.dataTypeToXSD) Data Type Not Handled For Column: " + _meta.getColumnClassName(_column));
                    return "string";
        }
    }
    
    /**
     * @method dataTypeAsString
     * @param _object
     * @return String
     * @throws Exception
     * @method @method
     * @author @author Simon Greener 12th December 2010 Original coding
     *          These data types can be used for labelling and display
     */
    public static String dataTypeAsString(Object _object) 
    {
        try {
            if ( _object instanceof oracle.sql.NUMBER ) {
                NUMBER num = (NUMBER)_object;
                Integer i = new Integer(-1); if ( num.isConvertibleTo(i.getClass()) )        return "INTEGER";
                Short s   = -1; if ( num.isConvertibleTo(s.getClass()) )                     return "SHORT";
                Long l    = new Long(-1); if ( num.isConvertibleTo(l.getClass()) )           return "LONG";
                Double d  = new Double(Double.NaN); if ( num.isConvertibleTo(d.getClass()) ) return "DOUBLE";
            }
            return _object.getClass().toString().replace("class","").replace("oracle.sql.","").replace("java.lang.","").trim().toUpperCase();
        } catch (Exception e) {
          return null;
        }
    }

    public static int numberToInt(Object _object, int _default) {
        if ( _object instanceof oracle.sql.NUMBER ) {
          try {
            NUMBER num = (NUMBER)_object;
            Integer i = new Integer(-1);        if ( num.isConvertibleTo(i.getClass()) ) return num.intValue();
            Short s   =             -1;         if ( num.isConvertibleTo(s.getClass()) ) return num.shortValue();
            Long l    = new Long(-1);           if ( num.isConvertibleTo(l.getClass()) ) return (int)num.longValue();
            Float  f  = new Float(Float.NaN);   if ( num.isConvertibleTo(f.getClass()) ) return (int)Math.round(num.floatValue());
            Double d  = new Double(Double.NaN); if ( num.isConvertibleTo(d.getClass()) ) return (int)Math.round(num.doubleValue());
          } catch (SQLException e) {
              return _default;
          }
        }
        return _default;
    }
    
}
