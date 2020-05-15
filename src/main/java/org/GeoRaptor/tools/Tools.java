package org.GeoRaptor.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import javax.ide.extension.Extension;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import oracle.dbtools.raptor.config.DBConfig;

import oracle.ide.ExtensionRegistry;
import oracle.ide.Ide;
import oracle.ide.config.EnvironOptions;
import oracle.ide.config.IdeSettings;

import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;

import oracle.spatial.geometry.JGeometry;

import oracle.sql.NUMBER;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.SpatialView.JDevInt.RenderTool;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SupportClasses.ViewOperationListener;
import org.GeoRaptor.sql.DatabaseConnections;

import org.geotools.util.logging.Logger;

public class Tools {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.tools.Tools");    
    private static final String propertiesFile = "org.GeoRaptor.tools.tools";
    protected static PropertiesManager propertyManager = new PropertiesManager(propertiesFile);

    /**
     * For access to preferences
     */
    protected static Preferences geoRaptorPreferences;

    public Tools() {
        super();
    }

    public static String getVersion() {
       ExtensionRegistry extReg = ExtensionRegistry.getExtensionRegistry(); 
       Extension ext = extReg.findExtension("org.GeoRaptor");
       return ext.getVersion().toString();
    }

    @SuppressWarnings("unused")
	private String minuteSeconds(long _seconds) {
        String returnString = "";
        long minutes = _seconds / 60;
        if ( minutes != 0 )
            returnString = String.format("%d minutes ",minutes);
        long seconds = _seconds - ( minutes * 60 );
        return String.format("%s%d seconds",returnString,seconds);
    }
  
    public static String milliseconds2Time( long _time )
    {
        float time = (float)_time / (float)Constants.MILLISECONDS;
        float seconds = (time % Constants.SECONDS);
        time /= Constants.SECONDS;
        int minutes = (int) (time % Constants.MINUTES);
        time /= Constants.MINUTES;
        int hours = (int) (time % 24);
        int days = (int) (time / 24);
        if( days == 0 )
            return String.format( "%d:%02d:%05.3f", hours,minutes,seconds );
        else
            return String.format( "%dd%d:%02d:%05.3f", days, hours,minutes,seconds );
    }  

    public static ArrayList<String> getCharsets(String _oracleCharset) {
        DBConfig dbConf = DBConfig.getInstance();
        // String nlsLength   = dbConf.getString(DBConfig.NLS_LENGTH);  // CHAR or BYTE
        String expEncoding = dbConf.getString(DBConfig.EXPORT_ENCODING);

        String envEncoding = null; 
        IdeSettings ideSettings = Ide.getSettings();
        EnvironOptions environOptions = (EnvironOptions)ideSettings.getData("environment-options");
        envEncoding = environOptions.getEncoding();

        String[] encodings = { _oracleCharset, expEncoding, envEncoding, "Cp1252", "ISO-8859-1", "UTF-8", "US-ASCII", "UTF-16" };
        ArrayList<String> myCharsets = new ArrayList<String>();
        for (int i=0; i<encodings.length;i++) {
            if ( !Strings.isEmpty(encodings[i]) ) {
                if ( ! myCharsets.contains(encodings[i]) ) {
                    myCharsets.add(encodings[i]);
                }
            }
        }
        return new ArrayList<String>(myCharsets);
    }

    public static DecimalFormat getNLSDecimalFormat(int     _maxFractionDigits,
                                                    boolean _includeGroupingSeparator)  
    {
        // Get the relevant NLS settings for use with a Java Formatter
        // 
        DBConfig dbConf = DBConfig.getInstance();
        String nlsSep = dbConf.getString(DBConfig.NLS_DEC_SEP);
        String nlsGrp = dbConf.getString(DBConfig.NLS_GRP_SEP);
        String nlsCur = dbConf.getString(DBConfig.NLS_CURR);

        // Create Default Format based on current client locale
        //
        DecimalFormat df = new DecimalFormat();
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.getDefault());

        // Overwrite with SQL Developer database settings
        //
        dfs.setDecimalSeparator(Strings.isEmpty(nlsSep)?dfs.getDecimalSeparator():nlsSep.charAt(0));
        if ( _includeGroupingSeparator ) {
            df.setGroupingUsed(true);
            dfs.setGroupingSeparator(Strings.isEmpty(nlsGrp)?dfs.getGroupingSeparator():nlsGrp.charAt(0));
        } else {
            df.setGroupingUsed(false);
        }
        dfs.setCurrencySymbol(Strings.isEmpty(nlsCur)?dfs.getCurrencySymbol():nlsCur);
        // Ensure we get a leading and trailing 0
        // SGG: changed from setMinimum to setMaximum ...
        df.setMaximumFractionDigits(_maxFractionDigits >= 0 ? _maxFractionDigits : df.getMaximumFractionDigits() );
        df.setMinimumFractionDigits(1);
        df.setMinimumIntegerDigits(1);
        df.setGroupingUsed(_includeGroupingSeparator);        
        return df;
    }
    
    public static DecimalFormat getDecimalFormatter(int     _maxFractionDigits,
                                                    boolean _includeGroupingSeparator) 
    {
        // Get current, locale dependent DecimalFormat using SQL Developer NLS Settings
        //
        DecimalFormat dFormat = getNLSDecimalFormat(_maxFractionDigits,_includeGroupingSeparator);
        return dFormat; 
    }
    
    public static DecimalFormat getDecimalFormatter(int _maxFractionDigits) {
        String dfPattern = "###0.0#####";
        if ( _maxFractionDigits == 1 ) {
            dfPattern = "###0.0";
        } else if ( _maxFractionDigits > 1 ) {
            dfPattern = "###0.0" + "####################".substring(0,_maxFractionDigits-1);
        }
        DecimalFormat df = new DecimalFormat(dfPattern, new DecimalFormatSymbols(Locale.US));
        // Ensure we get a leading and trailing 0
        //
        df.setMinimumFractionDigits(_maxFractionDigits >= 0 ? _maxFractionDigits : df.getMaximumFractionDigits() );
        df.setMinimumFractionDigits(1);
        df.setMinimumIntegerDigits(1);
        return df;

    }
    public static DecimalFormat getDecimalFormatter() {
        // Method with no arguments should be used whenever a double is to be used directly in SQL 
        return new DecimalFormat("###0.0#####", new DecimalFormatSymbols(Locale.US));
    }

    public static void copyToClipboard(String _text, String _copyText) {
        //Custom button text
        Object[] options = {propertyManager.getMsg("BUTTON_COPY_CLIPBOARD"),
                            propertyManager.getMsg("BUTTON_CLOSE")};
        int n = JOptionPane.showOptionDialog(null,
            _text,
            MainSettings.EXTENSION_NAME,
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1]);
        if (n == 0) {
            doClipboardCopy(Strings.isEmpty(_copyText) ? _text : _copyText);
        }
    }
    
    public static void doClipboardCopy(String _text) 
    {
        Toolkit.getDefaultToolkit().beep();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection ss = new StringSelection(_text);
        clipboard.setContents(ss, ss);
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
          case Types.ROWID : return "string"; 
          case Types.BOOLEAN : return "boolean";
          case OracleTypes.BFILE :
          case OracleTypes.RAW :
          case Types.BLOB : return "base64Binary";

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
     * @author Simon Greener 12th December 2010 Original coding
     *          These data types can be used for labeling and display
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
    
    public static String getViewUnits(SpatialView       _spatialView,
                                      Constants.MEASURE _measureType) 
    {
        return getViewUnits(_spatialView, 
                            _measureType==Constants.MEASURE.AREA 
                            ? ViewOperationListener.VIEW_OPERATION.MEASURE_POLYGON
                            : ViewOperationListener.VIEW_OPERATION.MEASURE_DISTANCE );
    }
    
    public static String getViewUnits(final SpatialView _spatialView,
                                      final ViewOperationListener.VIEW_OPERATION _svo )
    {
        // Are there any units associated with this _spatialView?
        if ( _spatialView.getSRIDType()==Constants.SRID_TYPE.UNKNOWN)
            _spatialView.setSRIDType();  // Query database and set the SRIDType
        
        // Get units of measure for view
        //
        String viewUnits = "";
        if ( _svo==ViewOperationListener.VIEW_OPERATION.MEASURE_DISTANCE ) {
          viewUnits = _spatialView.getDistanceUnitType();
          if (Strings.isEmpty(viewUnits) ) {
             _spatialView.setDistanceUnitType();
             viewUnits = _spatialView.getDistanceUnitType();
             if (Strings.isEmpty(viewUnits) )
                 viewUnits = propertyManager.getMsg("DEFAULT_UNIT"); 
          }
        } else {
          viewUnits = _spatialView.getAreaUnitType();
          if (Strings.isEmpty(viewUnits) ) {
             _spatialView.setAreaUnitType();
             viewUnits = _spatialView.getAreaUnitType();
             if (Strings.isEmpty(viewUnits) )
                 viewUnits = "SQ_" + propertyManager.getMsg("DEFAULT_UNIT"); 
          }
        }
        return viewUnits;  
    }

    public static double[] computeAreaLength(final JGeometry         _jGeom,
                                             final Constants.MEASURE _measure,
                                             final SpatialView       _spatialView) 
    {
        if ( propertyManager == null )
          propertyManager = new PropertiesManager(propertiesFile);
            
          String sql = "";
          String   areaUnits = getViewUnits(_spatialView,Constants.MEASURE.AREA);
          String lengthUnits = getViewUnits(_spatialView,Constants.MEASURE.LENGTH);
          String aSuffix = ( ( _spatialView.getSRID().equals(Constants.NULL) || Strings.isEmpty(areaUnits) )
                           ?   ")"
                           : ",areaUnits)" );
          String lSuffix = ( ( _spatialView.getSRID().equals(Constants.NULL) || Strings.isEmpty(lengthUnits) )
                           ?   ")"
                           : ",lengthUnits)" );
          if ( _measure == Constants.MEASURE.AREA ) {
              sql = "SELECT mdsys.sdo_geom.sdo_area(geom,tol" + aSuffix + " as area,\n" +
                    "       0.0 as length\n";
          } else if ( _measure == Constants.MEASURE.LENGTH ) { 
              sql = "SELECT 0.0 as area, \n" + 
                    "       mdsys.sdo_geom.sdo_length(geom,tol" + lSuffix + " as length\n"; 
          } else { // _measure == Constants.MEASURE.BOTH
              sql = "SELECT mdsys.sdo_geom.sdo_area(geom,tol"   + aSuffix + " as area,\n" +
                    "       mdsys.sdo_geom.sdo_length(geom,tol" + lSuffix + " as length\n";
          }
          sql += "  FROM (SELECT ? as geom, ? as tol, ? as areaUnits, ? as lengthUnits FROM dual)";

          double tolerance = 1.0 / Math.pow(10.0f,_spatialView.getPrecision(false));
          double[] dMeasures = new double[] {0.0f,0.0f};
          int AREA = 0; int LENGTH = 1;
          try {
            // Get area of geometry via sdo_geom method call
            PreparedStatement ps;
            Connection localConnection = DatabaseConnections.getInstance().getActiveConnection(); 
            ps = localConnection.prepareStatement(sql);
            //convert JGeometry instance to DB STRUCT
            ps.setObject(1,JGeometry.storeJS(localConnection,_jGeom));
            ps.setDouble(2,tolerance);
            ps.setString(3,"unit="+areaUnits);
            ps.setString(4,"unit="+lengthUnits);
            try {
                LOGGER.logSQL(sql + 
                              "\n? = " + RenderTool.renderGeometryAsPlainText(_jGeom,
                                                                              Constants.TAG_MDSYS_SDO_GEOMETRY, 
                                                                              Constants.bracketType.NONE, 
                                                                              Constants.MAX_PRECISION) +
                              "\n? = " + tolerance +
                              "\n? = " + "unit="+areaUnits +
                              "\n? = " + "unit="+lengthUnits);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ResultSet rs = ps.executeQuery();
            if ( rs.next() ) {
                dMeasures[AREA]   = rs.getDouble(1); if ( rs.wasNull() ) dMeasures[AREA]   = 0.0f;
                dMeasures[LENGTH] = rs.getDouble(2); if ( rs.wasNull() ) dMeasures[LENGTH] = 0.0f;
            }
            rs.close(); ps.close();
        } catch (SQLException sqle) {
              String errString = sqle.getMessage();
              if ( !Strings.isEmpty(errString) && errString.indexOf("ORA-", 2)!=-1 )
                errString = errString.substring(0, errString.indexOf("ORA-", 2));
                errString += String.format("\n" + sql.replace("?","%s"),
                                           "SDO_GEOM",
                                           String.valueOf(1.0 / Math.pow(10.0f,_spatialView.getPrecision(false))),
                                           "unit="+areaUnits,
                                           "unit="+lengthUnits);
              JOptionPane.showMessageDialog(null, 
                                            propertyManager.getMsg("ERROR_MEASUREMENT",errString),
                                            MainSettings.EXTENSION_NAME, 
                                            JOptionPane.ERROR_MESSAGE);
              return new double[] {-1.0f,-1.0f};
        } catch ( IllegalArgumentException iae ) {
            JOptionPane.showMessageDialog(null, 
                                          propertyManager.getMsg("ERROR_MEASUREMENT",iae.getLocalizedMessage()),
                                          MainSettings.EXTENSION_NAME, 
                                          JOptionPane.ERROR_MESSAGE);
            return new double[] {-1.0f,-1.0f};
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                                          propertyManager.getMsg("ERROR_MEASUREMENT",e.getLocalizedMessage()),
                                          MainSettings.EXTENSION_NAME, 
                                          JOptionPane.ERROR_MESSAGE);
            return new double[] {-1.0f,-1.0f};
        }
        return dMeasures;
    }

    public static JTable autoResizeColWidth(JTable             _table, 
                                            ArrayList<Integer> _columns) 
    {
        if ( _table==null || _table.getRowCount()==0 || _table.getColumnCount()==0 )
            return _table;

        // By default, the column widths of all columns are equal. 
        // In order for column width adjustments to be made, autoResizeMode must be disabled
        //
        _table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        DefaultTableColumnModel colModel = (DefaultTableColumnModel) _table.getColumnModel();
        TableColumn             tableCol = null;
        TableCellRenderer       renderer = null;
        Component                   comp = null;
        
        // Don't check everything if more than 100 rows (should be preference).
        // 
        int maxRows   = _table.getRowCount();
        int rowCount  = maxRows;
        boolean sample = rowCount>100; 
        rowCount = sample ? Math.round((float)rowCount * 0.1f) : rowCount;

        ArrayList<Integer> columns = null;
        if ( _columns == null || _columns.size()==0 ) {
            columns = new ArrayList<Integer>(_table.getColumnCount());
            for (int col = 0; col < _table.getColumnCount(); col++) 
                columns.add(Integer.valueOf(col));
        } else {
            columns = new ArrayList<Integer>(_columns);
        }
            
        int margin    = 5;
        int vColIndex = 0;
        int width     = 0;
        int actualRow = 0;
        Iterator<Integer> iter = columns.listIterator();
        while (iter.hasNext()) {
            vColIndex = iter.next().intValue();
            width     = 0;
            tableCol  = colModel.getColumn(vColIndex);
    
            // Get renderer for column header
            //
            renderer = tableCol.getHeaderRenderer();
            if (renderer == null) {
                renderer = _table.getTableHeader().getDefaultRenderer();
            }
            
            // Get starting Column Preferred width
            //
            comp = renderer.getTableCellRendererComponent(_table, tableCol.getHeaderValue(), false, false, 0, 0);
            width = comp.getPreferredSize().width;
            
            // Get maximum width of column from all or a sample of row data
            //
            for (int r = 0; r < rowCount; r++) {
                actualRow = sample 
                            ? (int)(Math.random() * (maxRows-1)) 
                            : r;
                renderer = _table.getCellRenderer(actualRow, vColIndex);
                comp     = renderer.getTableCellRendererComponent(_table, _table.getValueAt(actualRow, vColIndex), false, false, actualRow, vColIndex);
                width    = Math.max(width, comp.getPreferredSize().width);
            }

            // Add margin
            //
            width += 2 * margin;
              
            // Set the width
            //
            tableCol.setPreferredWidth(width);
          }
          return _table;
      }

    /**
       * Given a BasicStroke, create an ImageIcon that shows it.
       * Draws a chevron or squiggle
       * 
       * @param _stroke     the BasicStroke to draw on the Icon.
       * @param _iconWidth  the width of the icon.
       * @param _iconHeight the height of the icon.
       */
      public static ImageIcon createIcon(BasicStroke _stroke, 
                                         int         _iconWidth,
                                         int         _iconHeight) 
      { 
          BufferedImage bImage = new BufferedImage(_iconWidth, _iconHeight, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g2D = (Graphics2D) bImage.getGraphics();
          g2D.setBackground(new Color(255,255,255,0));
          g2D.setPaint(new Color(255,255,255,0));
          g2D.fillRect(0, 0, _iconWidth, _iconHeight);
          g2D.setPaint(Color.BLACK);
          g2D.setStroke(_stroke);
          // Create squiggle
          g2D.drawLine(                 0, _iconHeight,     _iconWidth / 3,           0);
          g2D.drawLine(    _iconWidth / 3,           0, _iconWidth * 2 / 3, _iconHeight);
          g2D.drawLine(_iconWidth * 2 / 3, _iconHeight,         _iconWidth,           0);          
          return new ImageIcon(bImage);
      }

    /**
       * Given a BasicStroke, create an ImageIcon that shows it.
       * Draws a chevron or squiggle
       * 
       * @param _stroke     the BasicStroke to draw on the Icon.
       * @param _iconWidth  the width of the icon.
       * @param _iconHeight the height of the icon.
       */
      public static ImageIcon createIcon(BasicStroke _stroke, 
                                         int         _iconWidth,
                                         int         _iconHeight,
                                         Shape       _shape,
                                         Color       _fillColor,
                                         Color       _drawColor) 
      { 
          BufferedImage bImage = new BufferedImage(_iconWidth, _iconHeight, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g2D = (Graphics2D) bImage.getGraphics();
          g2D.setBackground(new Color(255,255,255,0));
          g2D.setPaint(new Color(255,255,255,0));
          g2D.fillRect(0, 0, _iconWidth, _iconHeight);
          g2D.setPaint(_fillColor==null?Color.RED:_fillColor);
          g2D.setStroke(_stroke);
          // Create squiggle
          g2D.fill(_shape);
          g2D.setPaint(_drawColor==null?Color.BLACK:_drawColor);
          g2D.draw(_shape);          
          return new ImageIcon(bImage);
      }

      public static ImageIcon createIcon(BasicStroke _stroke, 
                                         Dimension   _iconSize,
                                         Shape       _shape,
                                         Color       _fillColor,
                                         Color       _drawColor) {
        return createIcon(_stroke,_iconSize.width,_iconSize.height,_shape,_fillColor,_drawColor);
      }
  
      /**
       * @function formatCoord
       * @param _X
       * @param _Y
       * @return String (?,?)
       * @author Simon Greener, May 2010
       */
      public static String formatCoord(double _X, double _Y, int _precision) {
          // Don't use NLS settings as this may use comma as decimal separator which is not supported due to SDO_ORDINATE_ARRAY using comma as separator
          // dFormat = Tools.getNLSDecimalFormat(_precision, false);
          DecimalFormat dFormat = Tools.getDecimalFormatter(_precision); 
          return "(" + dFormat.format(_X) + "," + dFormat.format(_Y) + ")";
      }
 
    public static Constants.SRID_TYPE discoverSRIDType(Connection _c,
                                                       String _srid) 
    {
        Constants.SRID_TYPE sridType;
        try 
        {
            if ( _c == null ) {
                // There is no connection available. Perhaps we are starting up? 
                return Constants.SRID_TYPE.UNKNOWN;
            }
            sridType = Constants.SRID_TYPE.valueOf(MetadataTool.getSRIDRefSysKind(_c,_srid)); 
        } catch (IllegalStateException ise) {
            // We are starting up
            return Constants.SRID_TYPE.UNKNOWN;   
        } catch (SQLException sqle) {
            JOptionPane.showMessageDialog(null,
                                        propertyManager.getMsg("ERROR_QUERYING_DB_FOR_SRID_TYPE", _srid,sqle.toString()),
                                        MainSettings.EXTENSION_NAME,
                                        JOptionPane.ERROR_MESSAGE);
            return Constants.SRID_TYPE.UNKNOWN;            
        } catch (IllegalArgumentException iae) {
            // Probably Null Connection in getSRIDRefSysKind
            JOptionPane.showMessageDialog(null,
                                        propertyManager.getMsg("ERROR_SRID_TYPE_CONNECTION", _srid,iae.toString()),
                                        MainSettings.EXTENSION_NAME,
                                        JOptionPane.ERROR_MESSAGE);
            return Constants.SRID_TYPE.UNKNOWN;
        }
        return sridType;
    }
    
    public static Constants.SRID_TYPE discoverSRIDType(String _srid) 
    {
        Connection localConnection = DatabaseConnections.getInstance().getActiveConnection(); 
        return Tools.discoverSRIDType(localConnection,_srid);
    }

}
