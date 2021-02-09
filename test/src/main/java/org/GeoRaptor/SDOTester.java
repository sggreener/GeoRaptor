package org.GeoRaptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Locale;
import java.util.ResourceBundle;

import org.GeoRaptor.OracleSpatial.Metadata.MetadataEntry;
import org.GeoRaptor.OracleSpatial.Metadata.MetadataTool;
import org.GeoRaptor.SpatialView.SupportClasses.Envelope;
import org.GeoRaptor.io.PrintGeometry;
import org.GeoRaptor.sql.SQLConversionTools;
import org.GeoRaptor.tools.JGeom;
import org.GeoRaptor.tools.RenderTool;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Tools;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.oracle.OraReader;
import org.locationtech.jts.io.oracle.OraUtil;

import oracle.jdbc.OracleTypes;
import oracle.jdbc.driver.OracleConnection;
import oracle.spatial.geometry.JGeometry;
import oracle.dbtools.raptor.utils.Connections;

public class SDOTester
{

	public static void main(String[] args) 
    {
        try
        {
            System.out.println("Start");
            /*
            int nTests = 10;
            for (int i = 0; i<=nTests; i++) 
              switch (i) {
              case 0 : TestOraUtil(); break;
              case 1 : TestStruct(); break;
              case 2 : TestDiminfo(); break;
              case 3 : TestMetadata(); break;
              case 4 : TestWriting(); break;
              case 5 : TestVertexType(); break;
              case 6 : TestPointType(); break;
              case 7 : dataTypeExamination(); break;
              case 8 : TestConversionToString(); break;
              case 9 : TestConversionToObject(); break;
              case 10: getLayerGType(); 
              }
              */
            //testBundle();
            testJMbr(new Envelope(0,0,10,10));
//            getLayerGType(); 
            System.out.println("Finished");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

	public static void testJMbr(Envelope _mbr)
	throws SQLException, Exception
	{
        Connection conn = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
        System.out.println(_mbr.toString());
        JGeometry jGeom = JGeom.fromEnvelope(_mbr,8307);
        
        System.out.println(RenderTool.renderGeometryAsPlainText(jGeom, 
                        		                                "MDSYS.SDO_GEOMETRY", 
                        		                                Constants.bracketType.NONE, 
                        		                                3));

        Struct s;
        s = JGeom.fromGeometry(jGeom,conn);
        
        System.out.println(RenderTool.renderStructAsPlainText(s, 
                                                              Constants.bracketType.NONE, 
                                                              3));
        
	}

	public static void TestMetadata() {
        System.out.println("<TestMetadata>");
		MetadataEntry me = new MetadataEntry();
		me.setEntry("Schema","Object","Column",null);
		me.add("X",0.0,10.0,0.1);
		me.add("Y",0.0,10.0,0.1);
		System.out.println("Srid=" + me.getSRID() + " " + me.getMBR().toString());
		me.add("Z",198.1,203.5,0.1);
		System.out.println("Srid=" + me.getSRID() + " " + me.toDimArray());
		me.add("M",0.1,10.4,0.01);
		System.out.println(me.toSdoGeometry());
        System.out.println("</TestMetadata>");
	}

	public static void TestStruct()
    {
        System.out.println("<TestStruct>");
        try
        {
        	//String url = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=localhost)(PORT=1522))(CONNECT_DATA=(SERVICE_NAME=GISDB12)))"; 
            //String url = "jdbc:oracle:thin:@localhost:1522:GISDB12";
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            Statement statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            ResultSet   results = statement.executeQuery(
            "select 1 as gid, SDO_GEOMETRY(2002,29182,MDSYS.SDO_POINT_TYPE(638254,7954500,NULL),SDO_ELEM_INFO_ARRAY(1,2,1),SDO_ORDINATE_ARRAY(638231.54,7954538.73,638235.33,7954554.09)) as geom from dual " 
            + " union all " +
            "select 2 as gid, SDO_GEOMETRY(2002,29182,NULL,SDO_ELEM_INFO_ARRAY(1,2,2),SDO_ORDINATE_ARRAY(100.0,100.0,150.0,150.0,200.0,100.0)) as geom from dual " 
            + " union all " +
            "select 3 as gid, SDO_GEOMETRY(2002,29182,NULL,SDO_ELEM_INFO_ARRAY(1,4,2,1,2,2,7,2,1),SDO_ORDINATE_ARRAY(100.0,100.0,150.0,150.0,200.0,100.0,300.0,300.0)) as geom from dual " 
            + " union all " +
            "select 4 as gid, SDO_GEOMETRY(3302,29182,NULL,SDO_ELEM_INFO_ARRAY(1,4,2,1,2,2,7,2,1),SDO_ORDINATE_ARRAY(100.0,100.0,1.0,150.0,150.0,25.0,200.0,100.0,100.0,300.0,300.0,167.9)) as geom from dual"
            + " UNION ALL " +
            "select 5 as gid, SDO_GEOMETRY(2003,29182,MDSYS.SDO_POINT_TYPE(638254,7954500,NULL),SDO_ELEM_INFO_ARRAY(1,1003,1),SDO_ORDINATE_ARRAY(638231.54,7954538.73,638235.33,7954554.09,638235.33,7954554.09,638231.54,7954554.09,638231.54,7954538.73)) as geom from dual " 
            + " UNION ALL " +
            "select 6 as gid, SDO_GEOMETRY(2001,NULL,MDSYS.SDO_POINT_TYPE(638254,7954500,NULL),NULL,NULL) as geom from dual " 
            + " UNION ALL " +
            "select 7 as gid, SDO_GEOMETRY(3002,28356,NULL,SDO_ELEM_INFO_ARRAY(1,2,1),SDO_ORDINATE_ARRAY(638231.543567,7954538.72123,1983.93,638235.312123,7954554.01229,2394.4)) as geom from dual "
            //"select 5 as gid, ST_GEOMETRY(SDO_GEOMETRY(3302,29182,NULL,SDO_ELEM_INFO_ARRAY(1,4,2,1,2,2,7,2,1),SDO_ORDINATE_ARRAY(100.0,100.0,1.0,150.0,150.0,25.0,200.0,100.0,100.0,300.0,300.0,167.9))) as geom from dual"
            );
            Tools.setPrecisionScale(3);
            Integer            gid = -1;
            Struct         SdoGeom = null;
            int[]    sdo_elem_info = null;
            double[] sdo_ordinates = null;
            String     sqlTypeName = "";

            while (results.next()) {
                try {
                    gid  = OraUtil.toInteger(results.getObject("GID"),0);
                    SdoGeom = (Struct)results.getObject("GEOM");
                    
                    sqlTypeName = SdoGeom.getSQLTypeName();
                    if ( sqlTypeName.equalsIgnoreCase(Constants.TAG_MDSYS_ST_GEOMETRY) )  {
                    	SdoGeom = SDO_GEOMETRY.getSdoFromST(SdoGeom);
                        sqlTypeName = SdoGeom.getSQLTypeName();
                    }
                    // Use SDO class to extract individual elements of geometry
                    int FullGType      = SDO_GEOMETRY.getFullGType(SdoGeom, 2001);
                    int GTYPE          = SDO_GEOMETRY.getGType(SdoGeom, 2001);
                    int DIM            = SDO_GEOMETRY.getDimension(SdoGeom,2);
                    int SRID           = SDO_GEOMETRY.getSRID(SdoGeom,-1);
                    int numOrdinates   = SDO_GEOMETRY.getNumberOrdinates(SdoGeom);
                    int numCoordinates = SDO_GEOMETRY.getNumberCoordinates(SdoGeom);
                    boolean hasMeasure = SDO_GEOMETRY.hasMeasure(SdoGeom);
                    
                    System.out.println("            GID= " + gid);
                    System.out.println("      FullGType= " + FullGType);
                    System.out.println("      Dimension= " + DIM + "D ");
                    System.out.println("  Geometry Type= " + SDO_GEOMETRY.getGeometryType(GTYPE));
                    System.out.println("     Shape Type= " + SDO_GEOMETRY.getShapeType(FullGType,hasMeasure).toString());
                    System.out.println("       Measured= " + (hasMeasure ?"YES": "NO"));
                    System.out.println("  LRS Dimension= " + SDO_GEOMETRY.getMeasureDimension(SdoGeom));
                    System.out.println("    Z ordinates= " + (SDO_GEOMETRY.hasZ(SdoGeom) ?"YES":"NO"));
                    System.out.println("     SDO_POINT?= " + (SDO_GEOMETRY.hasSdoPoint(SdoGeom)?"EXISTS":"DOES NOT EXIST") );
                    System.out.println(" Circular Arcs?= " + (SDO_GEOMETRY.hasArc(SdoGeom)     ?"EXISTS":"DOES NOT EXIST"));
                    System.out.println("   numOrdinates= " + numOrdinates);
                    System.out.println(" numCoordinates= " + numCoordinates);                    
                    System.out.print(  "SDO Constructor= MDSYS.SDO_GEOMETRY(" + FullGType + "," + (SRID==-1?"NULL":String.valueOf(SRID)));
                    
                    if ( SDO_GEOMETRY.hasSdoPoint(SdoGeom) ) 
                    {
                        double[] pointType    = SDO_GEOMETRY.getSdoPoint(SdoGeom,Double.NaN);
                        String sdo_point_type = ",SDO_POINT_TYPE("      + 
                                                      pointType[0] + "," +
                                                      pointType[1] + "," +
                                                      (Double.isNaN(pointType[2])?"NULL":pointType[2]) + ")";
                        System.out.print(sdo_point_type);
                    }
                    else
                    	System.out.print(",NULL");

                    // Read sdo_elem_info
                    if ( SDO_GEOMETRY.hasElemInfoArray(SdoGeom) ) {
                        sdo_elem_info = SDO_GEOMETRY.getSdoElemInfo(SdoGeom,0);
                      String sdo_elem_info_array = ",SDO_ELEM_INFO_ARRAY(" + sdo_elem_info[0];
                      for (int i=1;i<sdo_elem_info.length;i++) {
                      	sdo_elem_info_array += "," + sdo_elem_info[i];
                      }
                      sdo_elem_info_array += ")";
                      System.out.print(sdo_elem_info_array );
                    } else {
                    	System.out.print(",NULL");
                    }
                                        
                    // Read sdo_ordinates
                    if ( SDO_GEOMETRY.hasOrdinateArray(SdoGeom) ) {
                      sdo_ordinates = SDO_GEOMETRY.getSdoOrdinates(SdoGeom, Double.NaN);
                      String sdo_ordinates_array = ",SDO_ORDINATE_ARRAY(" + sdo_ordinates[0];
                      for (int i=1;i<sdo_ordinates.length;i++) { sdo_ordinates_array += "," + sdo_ordinates[i]; }
                      sdo_ordinates_array += ")";
                      System.out.println(sdo_ordinates_array + ")");
                      
                      // Reverse sdo_ordinates
                      double[] r_sdo_ordinates = SDO_GEOMETRY.reverseOrdinates(DIM,sdo_ordinates);
                      sdo_ordinates_array = "REVERSE SDO_ORDINATE_ARRAY(" + r_sdo_ordinates[0];
                      for (int i=1;i<r_sdo_ordinates.length;i++) {
                          sdo_ordinates_array += "," + r_sdo_ordinates[i];
                      }
                      sdo_ordinates_array += ")";
                      System.out.println("    " + sdo_ordinates_array);

                    } else {
                        System.out.println(",NULL)");                    	
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("    Conversion error " + e.toString());
                }
            }
            results.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("</TestConversion>");
    }

    public static void TestWriting() 
    {
        System.out.println("<TestWriting>");
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            Statement statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            //DBConnection.setConnection(database);
            ResultSet results = statement.executeQuery(
              "select 1 as gid, SDO_GEOMETRY(2002,29182,MDSYS.SDO_POINT_TYPE(638254,7954500,NULL),SDO_ELEM_INFO_ARRAY(1,2,1),SDO_ORDINATE_ARRAY(638231.54,7954538.73,638235.33,7954554.09)) as geom from dual"
           );
            // Convert Geometries
            //
            PrecisionModel  pm = new PrecisionModel(Tools.getPrecisionScale(8));
            GeometryFactory gf = new GeometryFactory(pm); 
            OraReader       or = new OraReader(gf);
            Geometry       geo = null;
            WKTWriter     wWkt = new WKTWriter(2);
            wWkt.setPrecisionModel(pm);

            Integer gid = -1;
            Struct geom = null;
            while (results.next()) {
                try {
                    gid     = ((BigDecimal)results.getObject("GID")).intValue();
                    System.out.println("Testing geometry gid=" + gid);
                    geom    = (Struct)results.getObject("GEOM");
                    int DIM = SDO_GEOMETRY.getDimension(geom,2);
                    geo     = or.read(geom);
                    System.out.println("    Geometry Read from SDO_GEOMETRY has type " + 
                                       geo.getGeometryType() + 
                                       " with dimension " + 
                                       DIM + 
                                       ". is Valid?=" + 
                                       geo.isValid() 
                                       );
                    System.out.println("Read SDO_GEOMETRY: " + wWkt.write(geo));                    
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("     Conversion error " + e.toString());
                }
            }
            results.close();
            statement.close();
            database.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("</TestWriting>");
    }
        
    public static void TestDiminfo()
        throws Exception
    {
        System.out.println("<TestDiminfo>");
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            Statement statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            ResultSet results = statement.executeQuery(
              "select diminfo from user_sdo_geom_metadata"
              //"select MDSYS.SDO_DIM_ARRAY(" +
              //          "MDSYS.SDO_DIM_ELEMENT('Longitude', 144.559350003827, 144.736170957996, 0.05), " +
              //         "MDSYS.SDO_DIM_ELEMENT('Latitude', -37.944685418421, -37.801062538134, 0.05)," +
              //          "MDSYS.SDO_DIM_ELEMENT('M',801.06384, 944.685, 0.05)"
              //      + ") as DIMINFO " +
              // " from dual" 
            );
            PrecisionModel pm = new PrecisionModel(Tools.getPrecisionScale(8));
            WKTWriter    wWkt = new WKTWriter(3); // 2D
            wWkt.setPrecisionModel(pm);
            Array     dimInfo = null;
            while (results.next()) {
                try {
                    dimInfo = results.getArray("DIMINFO");
                    if ( results.wasNull() )
                        continue;
                    JGeometry  jGeom = JGeom.getDimArrayAsJGeometry(dimInfo,4326);
                    if ( jGeom != null ) {
                    	PrintGeometry.printElemInfoArray(jGeom);
                    	PrintGeometry.printOrdArray(jGeom);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("  Conversion error " + e.toString());
                }
            }
            results.close();
            statement.close();
            database.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("</TestDiminfo>");
    }

    public static void TestVertexType()
        throws Exception
    {
        System.out.println("<TestVertexType>");
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            Statement statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            ResultSet results = statement.executeQuery(
              "select 1 as gid, mdsys.vertex_type(1,2,3,4) as geom from dual "
              + "union all " + 
              "select 2 as gid, mdsys.vertex_type(1,2,3,NULL) as geom from dual"
            );
            Integer gid = -1;
            Struct geom = null;
            while (results.next()) {
                try {
                    gid  = ((BigDecimal)results.getObject("GID")).intValue();
                    geom = (Struct)results.getObject("GEOM");
                    double[] vertexType = SDO_GEOMETRY.getVertexType(geom);
                    String vertex_type = Constants.TAG_MDSYS_VERTEX_TYPE +"(";
                    for (int i=0;i<vertexType.length;i++) {
                    	vertex_type += ((i==0)?"":",") + vertexType[i]; 
                    }
                    vertex_type += ")" ;
                    System.out.println(gid + "," + vertex_type);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("     Conversion error " + e.toString());
                }
            }
            results.close();
            statement.close();
            database.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("</TestVertexType>");
    }
    
    public static void TestPointType()
        throws Exception
    {
        System.out.println("<TestPointType>");
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            Statement statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            ResultSet   results = statement.executeQuery(
                "select 1 as gid, MDSYS.SDO_POINT_TYPE(1,2,3) as geom from dual"
                //"select 1 as gid, SDO_GEOMETRY(2001,NULL,MDSYS.SDO_POINT_TYPE(638254,7954500,NULL),NULL,NULL) as geom from dual"
            );
            Tools.setPrecisionScale(3);
            Integer gid = -1;
            Struct geom = null;
            while (results.next()) {
                try {
                    gid  = ((BigDecimal)results.getObject("GID")).intValue();
                    geom = (Struct)results.getObject("GEOM");
                    double[] pointType = SDO_GEOMETRY.getSdoPoint(geom,Double.NaN);
                    String sdo_point_type = Constants.TAG_MDSYS_SDO_POINT_TYPE + "(" + 
                              pointType[0] + "," +
                              pointType[1] + "," +
                              (Double.isNaN(pointType[2])?"NULL":pointType[2]) + 
                    ")";
                    System.out.println("GID(" + gid + "),Sdo_Point("+sdo_point_type + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("     Conversion error " + e.toString());
                }
            }
            results.close();
            statement.close();
            database.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("</TestPointType>");
    }

    public static void TestOraUtil()
    {
        System.out.println("<TestOraUtil>");
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            
            Statement statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            ResultSet results = statement.executeQuery(
            		"SELECT GID, GEOM FROM spdba.BLACKMANSBAYHOUSES WHERE rownum <= 10"
            		+ " UNION ALL " +
            		"SELECT GID, GEOM FROM spdba.parcels WHERE rownum <= 10"
            		+ " UNION ALL " +
            		"SELECT GID, GEOM FROM spdba.streets WHERE rownum <= 10"

            //"select 1 as gid, SDO_GEOMETRY(2002,29182,MDSYS.SDO_POINT_TYPE(638254,7954500,NULL),SDO_ELEM_INFO_ARRAY(1,2,1),SDO_ORDINATE_ARRAY(638231.54,7954538.73,638235.33,7954554.09)) as geom from dual union all " +
            //"select 2 as gid, SDO_GEOMETRY(2003,29182,MDSYS.SDO_POINT_TYPE(638254,7954500,NULL),SDO_ELEM_INFO_ARRAY(1,1003,1),SDO_ORDINATE_ARRAY(638231.54,7954538.73,638235.33,7954554.09,638235.33,7954554.09,638231.54,7954554.09,638231.54,7954538.73)) as geom from dual union all "+
            //"select 3 as gid, SDO_GEOMETRY(2001,NULL,MDSYS.SDO_POINT_TYPE(638254,7954500,NULL),NULL,NULL) as geom from dual union all " +
            //"select 4 as gid, SDO_GEOMETRY(3002,28356,NULL,SDO_ELEM_INFO_ARRAY(1,2,1),SDO_ORDINATE_ARRAY(638231.543567,7954538.72123,1983.93,638235.312123,7954554.01229,2394.4)) as geom from dual "
            );

            PrecisionModel  pm = new PrecisionModel(Tools.getPrecisionScale(8));
            GeometryFactory gf = new GeometryFactory(pm); 
            OraReader       or = new OraReader(gf);
            Geometry       geo = null;
            WKTWriter     wWkt = new WKTWriter(2);
            wWkt.setPrecisionModel(pm);

            Integer    gid = -1;
            Struct SdoGeom = null;

            while (results.next()) {
                try {
                    gid  = ((BigDecimal)results.getObject("GID")).intValue();
                    System.out.print(" GID=" + gid);
                    SdoGeom = (Struct)results.getObject("GEOM");
                    
                    // Tests OraRead/OraUtil/OraGeom as well
                    geo = or.read(SdoGeom);                    
                    System.out.println(" WKT: " + wWkt.write(geo)); 

                    System.out.println("            " + SDO_GEOMETRY.getGeometryAsString(SdoGeom,database)); 
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("    Conversion error " + e.toString());
                }
            }
            results.close();
            statement.close();
            database.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("</TestOraUtil>");
    }

    public static void dataTypeExamination() 
    {
        System.out.println("<dataTypeExamination>");
        try {
            // connect to test database
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            Statement statement;
            statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet results; 
            results = statement.executeQuery(
                  //"select BDBL_COL,BFLOAT_COL from oracle_all_types"
                          "select rowid, short_col, geom, int_col," +
                          "num021_col,num042_col,num063_col,num084_col,num106_col,num128_col,num1810_col,num2012_col, num1_col," +
                          "num2_col, num3_col, num4_col, num5_col, num6_col, num7_col, num8_col, num9_col, num10_col, real_col, real2_col, double_col,  float_col,  bigdecimal_col, " +
                          "string_col, bigstr_col, nstr_col, charstream_col, " +
                          "bytes_col, binarystream_col, " +
                          "date_col, timestamp_col, " +
                          "clob_col, blob_col, bfile_col, array_col, object_col " +
                     "from oracle_all_types"            );
            double dbl = -1;
            float flt = -1;
            while (results.next()) {
                ResultSetMetaData meta = results.getMetaData();
                for (int i=1; i<=meta.getColumnCount();i++) {
                    System.out.println(meta.getColumnName(i) + "/" + meta.getColumnLabel(i) + "," + meta.getColumnType(i) + "," + meta.getColumnTypeName(i));
                    if ( meta.getColumnTypeName(i).equalsIgnoreCase("BINARY_DOUBLE")) {
                        dbl = ((Double)results.getObject(i)).doubleValue();
                        System.out.println(dbl);
                    }
                  if ( meta.getColumnTypeName(i).equalsIgnoreCase("BINARY_FLOAT")) {
                      flt = ((Float)results.getObject(i)).floatValue();
                      System.out.println(flt);
                  }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("</dataTypeExamination>");
    }

    public static void TestConversionToString()
            throws Exception
    {
        System.out.println("<TestConversionToString>");
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            Statement statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            ResultSet   results = statement.executeQuery(
                    "select rowid, short_col, int_col," +
                           "num021_col,num042_col,num063_col,num084_col,num106_col,num128_col,num1810_col,num2012_col, num1_col," +
                           "num2_col, num3_col, num4_col, num5_col, num6_col, num7_col, num8_col, num9_col, num10_col, " +
                           "real_col, real2_col, double_col,  float_col,  bigdecimal_col, " +
                           "string_col, bigstr_col, nstr_col, charstream_col, " +
                           "bytes_col, binarystream_col, " +
                           "date_col, timestamp_col, " +
                           "clob_col, blob_col, bfile_col, array_col, object_col, geom " +
                      "from oracle_all_types"
            );
            Tools.setPrecisionScale(3);
            while (results.next()) {
                try {
                    ResultSetMetaData meta = results.getMetaData();
                    for (int i=1; i<=meta.getColumnCount();i++) {
                        System.out.print(meta.getColumnName(i) + "/" + 
                                         meta.getColumnLabel(i) + "," + 
                                         meta.getColumnType(i) + "," + 
                                         meta.getColumnTypeName(i));
                        
                        if (SQLConversionTools.isSupportedType(meta.getColumnType(1),
                    		                               meta.getColumnTypeName(1) ) )
                        {
                        	String s = convertToString(database,results,i);
                            System.out.println(": " + s );
                        } else
                          System.out.println("...is Not Supported");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("...Error " + e.toString());
                }
            }
            results.close();
            statement.close();
            database.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("</TestConversionToString>");
    }

    public static void TestConversionToObject()
            throws Exception
    {
        System.out.println("<TestConversionToObject>");
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection database = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "spdba", "sPdbA");
            Statement statement = database.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            ResultSet   results = statement.executeQuery(
                    "select rowid, short_col, int_col," +
                           "num021_col,num042_col,num063_col,num084_col,num106_col,num128_col,num1810_col,num2012_col, num1_col," +
                           "num2_col, num3_col, num4_col, num5_col, num6_col, num7_col, num8_col, num9_col, num10_col, " +
                           "real_col, real2_col, double_col,  float_col,  bigdecimal_col, " +
                           "string_col, bigstr_col, nstr_col, charstream_col, " +
                           "bytes_col, binarystream_col, " +
                           "date_col, timestamp_col, " +
                           "clob_col, blob_col, bfile_col, array_col, object_col, geom " +
                      "from oracle_all_types"
            );
            Tools.setPrecisionScale(3);
            while (results.next()) {
                try {
                    ResultSetMetaData meta = results.getMetaData();
                    for (int i=1; i<=meta.getColumnCount();i++) {
                        System.out.print(meta.getColumnName(i) + "/" + 
                                         meta.getColumnLabel(i) + "," + 
                                         meta.getColumnType(i) + "," + 
                                         meta.getColumnTypeName(i));
                        
                        if (SQLConversionTools.isSupportedType(meta.getColumnType(1),
                    		                               meta.getColumnTypeName(1) ) )
                        {
                        	Object obj = convertToObject(database,results,i);
                        	System.out.println(": " + ((obj == null) ? "NULL" : obj.getClass().toString()));
                        } else
                          System.out.println("...is Not Supported");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("...Error " + e.toString());
                }
            }
            results.close();
            statement.close();
            database.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("</TestConversionToString>");
    }

    public static String readClob(Clob _clob)
    {		
		try {
			Reader         rdr = _clob.getCharacterStream();
	    	StringBuilder   sb = new StringBuilder();
	        BufferedReader  br = new BufferedReader(rdr);
	        String line;
	        while(null != (line = br.readLine())) {
	            sb.append(line);
	        }
	        br.close();
			rdr.close();
	        return (sb.length() <= Integer.MAX_VALUE ? sb.toString() : sb.substring(0, Integer.MAX_VALUE));
	    } catch (SQLException e) {
	        // handle this exception
	    	return null;
	    } catch (IOException e) {
	        // handle this exception
	    	return null;
	    }
    }

    public static String readNClob(NClob _nClob)
    {
		try {
			Reader         rdr = _nClob.getCharacterStream();
	    	StringBuilder   sb = new StringBuilder();
	        BufferedReader  br = new BufferedReader(rdr);
	        String line;
	        while(null != (line = br.readLine())) {
	            sb.append(line);
	        }
	        br.close();
			rdr.close();
	        return (sb.length() <= Integer.MAX_VALUE ? sb.toString() : sb.substring(0, Integer.MAX_VALUE));
	    } catch (SQLException e) {
	        // handle this exception
	    	return null;
	    } catch (IOException e) {
	        // handle this exception
	    	return null;
	    }
	}

	public static String convertToString(Connection _conn, ResultSet _rSet, int _col) 
	{
		if (_conn == null || _rSet == null) {
			return "<NULL>";
		}
		String value = "";
		try {
			//if (_ors.wasNull()) {return "<NULL>";}
			ResultSetMetaData meta = (ResultSetMetaData) _rSet.getMetaData();
			Connection conn = _conn;

            /*System.out.println("convertToString(conn,obj,col) - " +
             * meta.getColumnLabel(_col) + "," + meta.getColumnDisplaySize(_col) + "," +
             * meta.getColumnType(_col)  + "/" + meta.getColumnTypeName(_col) + "("+
             * meta.getPrecision(_col)   + "," + meta.getScale(_col) +")");*/

            DecimalFormat df = Tools.getDecimalFormatter(meta.getScale(_col)==0?-1:meta.getScale(_col));
            
			switch (meta.getColumnType(_col)) {

			case Types.ROWID:    value = _rSet.getRowId(_col).toString(); break;
			case Types.NVARCHAR:
			case Types.NCHAR:    value = _rSet.getNString(_col); break;
			case Types.CHAR: 
			case Types.VARCHAR:  value = (String)_rSet.getString(_col); break;
			case Types.BOOLEAN:  value = Boolean.valueOf(_rSet.getBoolean(_col)).toString(); break;
			case Types.NCLOB:    value = readNClob(_rSet.getNClob(_col)); break;
			case Types.CLOB:     value = readClob(_rSet.getClob(_col));   break;
			case Types.TINYINT: 
			case Types.SMALLINT:
			case Types.INTEGER:  Integer intValue = _rSet.getInt(_col);   value = String.valueOf(intValue);  break;
			case Types.BIGINT:      Long longValue = _rSet.getLong(_col); value = String.valueOf(longValue); break;
            case OracleTypes.BINARY_DOUBLE:
            case OracleTypes.BINARY_FLOAT:
    			//case OracleTypes.BINARY_DOUBLE: BINARY_DOUBLE bdbl = new BINARY_DOUBLE(_ors.getOracleObject(_col).getBytes()); value = df.format(new Double(bdbl.stringValue())); break;				
    			//case OracleTypes.BINARY_FLOAT: BINARY_FLOAT bflt = (BINARY_FLOAT) _ors.getOracleObject(_col); value = df.format(new Float(bflt.stringValue())); break;
			case Types.FLOAT:
			case Types.DOUBLE:
			case Types.DECIMAL:
			case OracleTypes.NUMBER:
				BigDecimal bd = (BigDecimal)_rSet.getObject(_col);
				value = ( meta.getScale(_col)==0 ) ? bd.toBigInteger().toString() : df.format(Double.valueOf(bd.doubleValue())); break;
			case OracleTypes.TIMESTAMPTZ:  value = ((oracle.jdbc.OracleResultSet)_rSet).getTIMESTAMPTZ(_col).stringValue(conn); break;
			case OracleTypes.TIMESTAMPLTZ: value = ((oracle.jdbc.OracleResultSet)_rSet).getTIMESTAMPLTZ(_col).stringValue(conn); break;
			case OracleTypes.INTERVALYM:   value = ((oracle.jdbc.OracleResultSet)_rSet).getINTERVALYM(_col).stringValue(); break;
			case OracleTypes.INTERVALDS:   value = ((oracle.jdbc.OracleResultSet)_rSet).getINTERVALDS(_col).stringValue(); break;
			case Types.DATE:
			case Types.TIMESTAMP:          value = (meta.getColumnTypeName(_col).equalsIgnoreCase("DATE")) ? String.valueOf(_rSet.getDate(_col)) : String.valueOf(_rSet.getTimestamp(_col)); break;				
			case Types.TIME:               value = String.valueOf(_rSet.getTime(_col)); break;
			case OracleTypes.STRUCT:
				//SpatialRenderer renderer = SpatialRenderer.getInstance();
				if (meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY)
						|| meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE)
						|| meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE)) {
					value = SDO_GEOMETRY.getGeometryAsString((Struct)_rSet.getObject(_col),_conn);
				    // renderer.renderGeoObject(_ors.getOracleObject(_col), false);
				}
				break;
			case OracleTypes.BFILE:
			case OracleTypes.RAW:
			case Types.BLOB:
			default:
				System.out.print(": Data Type Not Handled");
				value = null;
			}
			
		} catch (Exception e) {
			System.out.println(e.toString());
			value = null;
		}
		return value;
	}

	public static Object convertToObject(Connection _conn, ResultSet _rSet, int _col) 
	{
		if (_conn == null || _rSet == null) {
			return "<NULL>";
		}
		Object value = null;
		try {
			ResultSetMetaData meta = (ResultSetMetaData) _rSet.getMetaData();

            /*System.out.println("convertToString(conn,obj,col) - " +
             * meta.getColumnLabel(_col) + "," + meta.getColumnDisplaySize(_col) + "," +
             * meta.getColumnType(_col)  + "/" + meta.getColumnTypeName(_col) + "("+
             * meta.getPrecision(_col)   + "," + meta.getScale(_col) +")");*/

			switch (meta.getColumnType(_col)) {

			case Types.ROWID:   value = _rSet.getRowId(_col); break;
			case Types.NVARCHAR:
			case Types.NCHAR:   value = _rSet.getNString(_col); break;			
			case Types.CHAR:
			case Types.VARCHAR: value = (String)_rSet.getString(_col); break;
			case Types.BOOLEAN: value = _rSet.getBoolean(_col); break;
			case Types.NCLOB:   value = readNClob(_rSet.getNClob(_col)); break;
			case Types.CLOB:    value = readClob(_rSet.getClob(_col));   break;
			case Types.TINYINT: 
			case Types.SMALLINT:
			case Types.INTEGER: value = _rSet.getInt(_col); break;
			case Types.BIGINT:  value = _rSet.getLong(_col); break;
            case OracleTypes.BINARY_DOUBLE:
            case OracleTypes.BINARY_FLOAT:
    			//case OracleTypes.BINARY_DOUBLE: BINARY_DOUBLE bdbl = new BINARY_DOUBLE(_ors.getOracleObject(_col).getBytes()); value = df.format(new Double(bdbl.stringValue())); break;				
    			//case OracleTypes.BINARY_FLOAT:  BINARY_FLOAT bflt = (BINARY_FLOAT) _ors.getOracleObject(_col); value = df.format(new Float(bflt.stringValue())); break;
			case Types.FLOAT:
			case Types.DOUBLE:
			case Types.DECIMAL:
			case OracleTypes.NUMBER:
				BigDecimal bd = (BigDecimal)_rSet.getObject(_col);
                value = ( meta.getScale(_col)==0 ) ? bd.toBigInteger() : Double.valueOf(bd.doubleValue());
				break;
			case OracleTypes.TIMESTAMPTZ:  value = ((oracle.jdbc.OracleResultSet)_rSet).getTIMESTAMPTZ(_col); break;
			case OracleTypes.TIMESTAMPLTZ: value = ((oracle.jdbc.OracleResultSet)_rSet).getTIMESTAMPLTZ(_col); break;
			case OracleTypes.INTERVALYM:   value = ((oracle.jdbc.OracleResultSet)_rSet).getINTERVALYM(_col); break;
			case OracleTypes.INTERVALDS:   value = ((oracle.jdbc.OracleResultSet)_rSet).getINTERVALDS(_col); break;
			case Types.DATE:
			case Types.TIMESTAMP:          value = (meta.getColumnTypeName(_col).equalsIgnoreCase("DATE")) ? _rSet.getDate(_col) : _rSet.getTimestamp(_col); break;
			case Types.TIME:               value = _rSet.getTime(_col); break;

			case OracleTypes.STRUCT:
				//SpatialRenderer renderer = SpatialRenderer.getInstance();
				if ( meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_GEOMETRY)
                  || meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_SDO_POINT_TYPE)
                  || meta.getColumnTypeName(_col).equalsIgnoreCase(Constants.TAG_MDSYS_VERTEX_TYPE)) {
                  value = _rSet.getObject(_col);
				  //value = SDO_GEOMETRY.getGeometryAsString((Struct)_rSet.getObject(_col),_conn);
				  // renderer.renderGeoObject(_ors.getOracleObject(_col), false);
				}
				break;

			case Types.BLOB:
			case OracleTypes.BFILE:
			case OracleTypes.RAW:
			default:
				System.out.print(": Data Type Not Handled");
				value = null;
			}
			
		} catch (Exception e) {
			System.out.println(e.toString());
			value = null;
		}
		return value;
	}

	public static void getLayerGType() throws Exception
	{
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection conn = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "codesys", "c0d3mgr");

        boolean isGeodetic = MetadataTool.querySridGeodetic(conn,"4326");
        System.out.println("Is SRID 4326 geodetic? " + String.valueOf(isGeodetic));
        
        String wkt = MetadataTool.getSRIDWKT(conn,"4326");
        System.out.println("SRID as WKT " + wkt);        
        
        String refSys = MetadataTool.getSRIDRefSysKind(conn,"4326");
        System.out.println("SRID RefSysKind " + refSys);        
       
        String uom = MetadataTool.getSRIDBaseUnitOfMeasure(conn,"4326");
        System.out.println("SRID Unit Of Measure " + uom);        

        int layerSRID = MetadataTool.getLayerSRID( conn,"CODESYS","AUSTRALIAN_QUAD","GEOM");
        System.out.println("Layer SRID for AUSTRALIAN_QUAD " + String.valueOf(layerSRID));

        int rowCount = MetadataTool.getRowCount( conn,"CODESYS","AUSTRALIAN_QUAD",null);
        System.out.println("Number Rows n AUSTRALIAN_QUAD " + String.valueOf(rowCount));
                
        String spName = MetadataTool.getSpatialIndexName(conn,"CODESYS","AUSTRALIAN_QUAD","GEOM");  
        System.out.println("Layer Geometry Spatial Index name " + spName);

        int dims = MetadataTool.getSpatialIndexDims(conn,"CODESYS","AUSTRALIAN_QUAD","GEOM");  
        System.out.println("Spatial DIms " + String.valueOf(dims));
        		
        boolean hasIndex = MetadataTool.hasSpatialIndex(conn,"CODESYS","AUSTRALIAN_QUAD","GEOM",true);
        System.out.println("Has Spatial index " + String.valueOf(hasIndex));
        
        boolean spIndex = MetadataTool.isSpatiallyIndexed(conn,"CODESYS","AUSTRALIAN_QUAD","GEOM","4326");
        System.out.println("Spatally indexed " + String.valueOf(spIndex));

        String gType = MetadataTool.getLayerGeometryType(conn,"CODESYS","AUSTRALIAN_QUAD","GEOM",10,10);
        System.out.println("Layer Geometry Type " + gType);
	}
	
	public static void testBundle()
	{
		 final String FILENAME = "org.GeoRaptor.Resources";

System.out.println("Opening " + FILENAME);

         try {
        	 URL resUrl = SDOTester.class.getClassLoader().getResource( FILENAME );
System.out.println(resUrl==null?"resUrl is null":resUrl.toString());
         	Properties props = new Properties();
			props.load( resUrl.openStream() );
		 } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
        	 
System.out.println("Opening2 " + FILENAME + " Locale= "+Locale.getDefault());

		 ResourceBundle resourceBundle = ResourceBundle.getBundle(FILENAME,Locale.getDefault());

         String str = null;
	     if (resourceBundle != null) {
	         str = resourceBundle.getString(FILENAME);
             System.out.println("Value found: " + str + " for key: " + FILENAME);
	     } else {
	         System.out.println("Properties file was not loaded correctly!!");
	     }

	}
}



