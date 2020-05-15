package org.GeoRaptor.io.Export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Preferences;
import org.GeoRaptor.tools.FileUtils;
import org.GeoRaptor.tools.Strings;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.util.logging.Logger;
import org.xBaseJ.micro.fields.Field;

import com.vividsolutions.jts.geom.Envelope;

public class TABExporter 
     extends SHPExporter 
    implements IExporter
{
    @SuppressWarnings("unused")
	private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.io.Export.TABExporter");

    public TABExporter(Connection connection, String string, int totalRows) {
        super(connection, string, totalRows);
    }
    
    public void end() throws IOException {
        super.end();
        try {
            String dirName    = FileUtils.getDirectory(super.getFileName());
            String fNameNoExt = FileUtils.getFileNameFromPath(super.getFileName(),true);
            String coordString = "";
            String symbolisationString = "";
            Preferences geoRaptorPreferences = MainSettings.getInstance().getPreferences();
            if ( !Strings.isEmpty(geoRaptorPreferences.getCoordsys()) ) 
                coordString = geoRaptorPreferences.getCoordsys();
            String tabContents = createTabString(super.dbaseWriter,
                                                 fNameNoExt+ ".dbf",
                                                 coordString,
                                                 symbolisationString,
                                                 super.shpType,
                                                 super.shpWriter.getEnvelope());
            writeTabFile(FileUtils.FileNameBuilder(dirName, fNameNoExt, "tab"),tabContents);
        } catch (Exception e) {
            throw new IOException("Failed to create TAB file wrapper over SHP file (" + e.getMessage() + ")");
        }
    }

  /**
   * @param fullyQualifiedFileName - filename including directory path and extension. 
   * @param tabString - Actual string containing TAB file's contents.
   * @throws FileNotFoundException - if there is an error creating the file.
   * @throws IOException  - if there is an error writing to the file.
   * @name writeTabFile() 
   * @description Writes actual TAB file 
   */
  protected static void writeTabFile(String _fullyQualifiedFileName,
                                     String _tabString ) 
  throws FileNotFoundException, 
         IOException 
  {
      File tabFile;
      tabFile = new File(_fullyQualifiedFileName);
      Writer tabOutput = new BufferedWriter(new FileWriter(tabFile));
      try {
           //FileWriter always assumes default encoding is OK!
           tabOutput.write( _tabString );
      }
      catch (IOException ioe) {
          tabOutput.close();
          tabOutput = null;
          throw new RuntimeException("Error writing TAB file.", ioe);
      }
      finally {
           tabOutput.close();
           tabOutput = null;
       }
  }
  
  /**
  * @name   createTabString() 
  * @description Creates the contents of a TAB file that can "wrap" a shapefile allowing it to be used with MapInfo
  * @param header the result set, including a geometry column.
  * @param coordSys - A MapInfo CoordSys string. 
  * @param shpEnvelope - An envelope or MBR covering all the shapes in the shapefile.
  * @return String that is the contents of the TAB file.
  * @throws SQLException if there is an error reading the result set metadata.
  */
  protected static String createTabString(DBaseWriter dbaseWriter,
                                          String      dbfFileName,
                                          String      coordSys,
                                          String      symbolisationString,
                                          ShapeType   sType,
                                          Envelope    shpEnvelope) 
  throws Exception {
      Envelope localEnv = shpEnvelope;
      if ( localEnv != null && ! localEnv.isNull() ) {
          // Should one do this for "CoordSys Earth Projection" ie Lat/Long?
          if ( localEnv.getMinX() == localEnv.getMaxX() && 
               localEnv.getMinY() == localEnv.getMaxY() ) {
              // expand this baby...
              localEnv.expandBy( localEnv.getMaxX() / 10 ); // eg 10m / 10 = 1; 1 degree / 10 = 1.1 degree
          }
          /* Check if supplied CoordSys already has Bounds() element. 
           * If so, remove it before adding in shapefile's actual bounds */
          if (coordSys.indexOf("Bound") > 0)
              coordSys = coordSys.substring(0,coordSys.indexOf("Bound")-1);
          /* Add computed Bounds to coordSys string */
          coordSys += " Bounds (" + 
                      localEnv.getMinX() + "," + localEnv.getMinY() +
                      ") (" + 
                      localEnv.getMaxX() + "," + localEnv.getMaxY() + 
                      ")";
      }
      String mapInfoTabdbaseWriter = "!table\n" + 
      "!version 700\n" + 
      "!charset WindowsLatin1\n" + 
      "\n" + 
      "Definition Table\n" + 
      "  File \"" + dbfFileName + "\"\n" +
      "  Type SHAPEFILE Charset \"WindowsLatin1\"\n  Fields ";
      String mapInfoTabContents = "";
      String mapInfoTabFooter = "begin_metadata\n" + 
      "\"\\IsReadOnly\" = \"FALSE\"\n" + 
      "\"\\Shapefile\" = \"\"\n" + 
      "\"\\Shapefile\\PersistentCache\" = \"FALSE\"\n" + 
      "\"\\Spatial Reference\" = \"\"\n" + 
      "\"\\Spatial Reference\\Geographic\" = \"\"\n" + 
      "\"\\Spatial Reference\\Geographic\\Projection\" = \"\"\n" + 
      "\"\\Spatial Reference\\Geographic\\Projection\\Clause\" = \"" + coordSys + "\"\n";
      
      /** The following needs to be improved via parameterization */
      if ( !Strings.isEmpty(symbolisationString) ) {
          mapInfoTabFooter += symbolisationString;
      } else {
          mapInfoTabFooter += "\"\\DefaultStyles\" = \"\"\n" ;
          if (sType.isPointType() || 
              sType.isMultiPointType()) {
              mapInfoTabFooter += 
              "\"\\DefaultStyles\\Symbol\" = \"\"\n" + 
              "\"\\DefaultStyles\\Symbol\\Type\" = \"0\"\n" + 
              "\"\\DefaultStyles\\Symbol\\Pointsize\" = \"12\"\n" + 
              "\"\\DefaultStyles\\Symbol\\Color\" = \"0\"\n" + 
              "\"\\DefaultStyles\\Symbol\\Code\" = \"35\"\n";
          } else {
              /* must be lines and polygons.
               * only brush polygons */
              if ( sType.isPolygonType() ) {
                  mapInfoTabFooter += 
                  "\"\\DefaultStyles\\Brush\" = \"\"\n" + 
                  "\"\\DefaultStyles\\Brush\\Pattern\" = \"2\"\n" + 
                  "\"\\DefaultStyles\\Brush\\Forecolor\" = \"16777215\"\n" + 
                  "\"\\DefaultStyles\\Brush\\Backcolor\" = \"16777215\"\n" ;
              }
              /* But add line style to both polygons and lines */
              mapInfoTabFooter += 
                  "\"\\DefaultStyles\\Pen\" = \"\"\n" + 
                  "\"\\DefaultStyles\\Pen\\LineWidth\" = \"1\"\n" + 
                  "\"\\DefaultStyles\\Pen\\LineStyle\" = \"0\"\n" + 
                  "\"\\DefaultStyles\\Pen\\Color\" = \"0\"\n" + 
                  "\"\\DefaultStyles\\Pen\\Pattern\" = \"2\"\n";
          }
      }
      
      mapInfoTabFooter += "end_metadata";
      
      Iterator<String> iter = dbaseWriter.fields.keySet().iterator();
      while (iter.hasNext() ) {
          Field f = dbaseWriter.getField(iter.next());
          // write the field name
          int tabPrecision = 0;
          try {
              mapInfoTabContents += "\t" + f.getName();
              for (int j = 1; j < 10 - f.getName().length(); j++)
                  mapInfoTabContents += " ";
              mapInfoTabContents += "\t";
              switch (f.getType()) {
                  case 'C' : mapInfoTabContents += "Char (" + f.getLength() + ")"; break;
                  case 'F' : mapInfoTabContents += "Float"; break;
                  /*case 'N' : mapInfoTabContents += "Decimal (" + dbaseWriter.getFieldLength(i) + "," + dbaseWriter.getFieldDecimalCount(i) + ")";*/
                  case 'N' : 
                     /** A dbase column is defined N n m but MapInfo's TAB file wrapper
                      *  wants n + m + 1. So, N 3 2 becomes Decimal (6,2).
                      */
                     tabPrecision = ( f.getDecimalPositionCount() == 0 ) ? 
                                      f.getLength() : 
                                      f.getLength() + 1 + f.getDecimalPositionCount();
                     /** Need to check resultant precision to see if need to upscale N data type to Float
                      **/
                    if ( tabPrecision > DBaseWriter.MAX_NUMERIC_LENGTH )
                         mapInfoTabContents += "Float";
                    else
                         mapInfoTabContents += "Decimal (" +  tabPrecision + "," + f.getDecimalPositionCount() + ")"; 
                    break;
                  case 'D' : mapInfoTabContents += "Date";
              }
              mapInfoTabContents += " ;\n";
          }
          catch (Exception e) {
              throw new RuntimeException("Error constructing Tab file string", e);
          }
      }
      return mapInfoTabdbaseWriter + String.valueOf(dbaseWriter.fields.size()) + "\n" + 
             mapInfoTabContents + 
             mapInfoTabFooter;
  }

}
