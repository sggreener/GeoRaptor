DROP   TABLE ORACLE_TEST_GEOMETRIES ;
CREATE TABLE ORACLE_TEST_GEOMETRIES (
  id       NUMBER,
  name     VARCHAR2(250),
  geometry MDSYS.SDO_GEOMETRY
);
DROP   SEQUENCE ORACLE_TEST_GEOMETRIES_ID_SEQ;
CREATE SEQUENCE ORACLE_TEST_GEOMETRIES_ID_SEQ;

create or replace trigger ORACLE_TEST_GEOMETRIES_PK  
   before insert on ORACLE_TEST_GEOMETRIES 
   for each row 
begin  
   if inserting then 
      if :NEW.ID is null then 
         select ORACLE_TEST_GEOMETRIES_ID_SEQ.nextval into :NEW.ID from dual; 
      end if; 
   end if; 
end;
/
show errors

INSERT ALL
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Single Point (SDO_POINT encoding)',
  mdsys.sdo_geometry(2001, NULL, mdsys.sdo_point_type(900.0,900.0,NULL), NULL, NULL))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Single Point (Ordinate Encoding)',
  mdsys.sdo_geometry (2001, null, null, mdsys.sdo_elem_info_array (1,1,1), mdsys.sdo_ordinate_array (10,5)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Single Point With Too Many Ordinates',
  mdsys.sdo_geometry (2001, null, null, mdsys.sdo_elem_info_array (1,1,1), mdsys.sdo_ordinate_array (10,5, 11,6)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Line segment',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,1), mdsys.sdo_ordinate_array (10,10, 20,10)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Arc segment',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,2), mdsys.sdo_ordinate_array (10,15, 15,20, 20,15)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Line string',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,1), mdsys.sdo_ordinate_array (10,25, 20,30, 25,25, 30,30)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Arc string',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,2), mdsys.sdo_ordinate_array (10,35, 15,40, 20,35, 25,30, 30,35)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Compound line string',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,4,3, 1,2,1, 3,2,2, 7,2,1), mdsys.sdo_ordinate_array (10,45, 20,45, 23,48, 20,51, 10,51)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Closed line string',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,1), mdsys.sdo_ordinate_array (10,55, 15,55, 20,60, 10,60, 10,55)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Closed arc string',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,2), mdsys.sdo_ordinate_array (15,65, 10,68, 15,70, 20,68, 15,65)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Closed mixed line',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,4,2, 1,2,1, 7,2,2), mdsys.sdo_ordinate_array (10,78, 10,75, 20,75, 20,78, 15,80, 10,78)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Self-crossing line',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,1), mdsys.sdo_ordinate_array (10,85, 20,90, 20,85, 10,90, 10,85)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Polygon',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,1), mdsys.sdo_ordinate_array (10,105, 15,105, 20,110, 10,110, 10,105)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Polygon with void',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,3, 5,2003,3), mdsys.sdo_ordinate_array (50,135, 60,140, 51,136, 59,139)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Polygon with void - reverse',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,2003,3, 5,1003,3), mdsys.sdo_ordinate_array (51,146, 59,149, 50,145, 60,150)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Arc polygon',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,2), mdsys.sdo_ordinate_array (15,115, 20,118, 15,120, 10,118, 15,115)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Compound polygon',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1005,2, 1,2,1, 7,2,2), mdsys.sdo_ordinate_array (10,128, 10,125, 20,125, 20,128, 15,130, 10,128)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Rectangle',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,3), mdsys.sdo_ordinate_array (10,135, 20,140)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Rectangle with Rectangular hole',
  mdsys.Sdo_Geometry(2003, Null,Null, mdsys.Sdo_Elem_Info_Array(1,1003,3,5,2003,3), mdsys.Sdo_Ordinate_Array(10.0,135.0,20.0,140.0, 30.0,100.0,70.0,100.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Circle',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,4), mdsys.sdo_ordinate_array (15,145, 10,150, 20,150)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Point cluster',
  mdsys.sdo_geometry (2005, null, null, mdsys.sdo_elem_info_array (1,1,3), mdsys.sdo_ordinate_array (50,5, 55,7, 60,5)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Multipoint',
  mdsys.sdo_geometry (2005, null, null, mdsys.sdo_elem_info_array (1,1,1, 3,1,1, 5,1,1), mdsys.sdo_ordinate_array (65,5, 70,7, 75,5)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Multiline',
  mdsys.sdo_geometry (2006, null, null, mdsys.sdo_elem_info_array (1,2,1, 5,2,1), mdsys.sdo_ordinate_array (50,15, 55,15, 60,15, 65,15)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Multiline - crossing',
  mdsys.sdo_geometry (2006, null, null, mdsys.sdo_elem_info_array (1,2,1, 5,2,1), mdsys.sdo_ordinate_array (50,22, 60,22, 55,20, 55,25)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Multiarc',
  mdsys.sdo_geometry (2006, null, null, mdsys.sdo_elem_info_array (1,2,2, 7,2,2), mdsys.sdo_ordinate_array (50,35, 55,40, 60,35, 65,35, 70,30, 75,35)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Multiline - closed',
  mdsys.sdo_geometry (2006, null, null, mdsys.sdo_elem_info_array (1,2,1, 9,2,1), mdsys.sdo_ordinate_array (50,55, 50,60, 55,58, 50,55, 56,58, 60,55, 60,60, 56,58)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Multiarc - touching',
  mdsys.sdo_geometry (2006, null, null, mdsys.sdo_elem_info_array (1,2,2, 7,2,2), mdsys.sdo_ordinate_array (50,65, 50,70, 55,68, 55,68, 60,65, 60,70)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Multipolygon - disjoint',
  mdsys.sdo_geometry (2007, null, null, mdsys.sdo_elem_info_array (1,1003,1, 11,1003,3), mdsys.sdo_ordinate_array (50,105, 55,105, 60,110, 50,110, 50,105, 62,108, 65,112)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('13350: Multipolygon - touching',
  mdsys.sdo_geometry (2007, null, null, mdsys.sdo_elem_info_array (1,1003,3, 5,1003,3), mdsys.sdo_ordinate_array (50,115, 55,120, 55,120, 58,122)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('13351: Multipolygon - complex touch',
  mdsys.sdo_geometry (2007, null, null, mdsys.sdo_elem_info_array (1,1003,3, 5,1003,3), mdsys.sdo_ordinate_array (50,125, 55,130, 55,128, 60,132)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('13350: Multipolygon - multi-touch',
  mdsys.sdo_geometry (2007, null, null, mdsys.sdo_elem_info_array (1,1003,1, 17,1003,1), mdsys.sdo_ordinate_array (50,95, 55,95, 53,96, 55,97, 53,98, 55,99, 50,99, 50,95, 55,100, 55,95, 60,95, 60,100, 55,100)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('13349: Crescent (straight lines)',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,1), mdsys.sdo_ordinate_array (10,175, 10,165, 20,165, 15,170, 25,170, 20,165, 30,165, 30,175, 10,175)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('13349: Crescent (arcs)',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,2), mdsys.sdo_ordinate_array (14,180, 10,184, 14,188, 18,184, 14,180, 16,182, 14,184, 12,182, 14,180)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Heterogeneous collection',
  mdsys.sdo_geometry (2004, null, null, mdsys.sdo_elem_info_array (1,1,1, 3,2,1, 7,1003,1), mdsys.sdo_ordinate_array (10,5, 10,10, 20,10, 10,105, 15,105, 20,110, 10,110, 10,105)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Polygon+void+island touch',
  mdsys.sdo_geometry (2007, null, null, mdsys.sdo_elem_info_array (1,1003,1, 11,2003,1, 31,1003,1), mdsys.sdo_ordinate_array (50,168, 50,160, 55,160, 55,168, 50,168,  51,167, 54,167, 54,161, 51,161, 51,162, 52,163, 51,164, 51,165, 51,166, 51,167, 52,166, 52,162, 53,162, 53,166, 52,166)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Line',
  mdsys.sdo_geometry(2002, NULL, NULL, mdsys.sdo_elem_info_array(1,2,1), mdsys.sdo_ordinate_array(100.0, 100.0, 900.0, 900.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Polygon no holes',
  mdsys.sdo_geometry(2003, NULL, NULL, mdsys.sdo_elem_info_array(1,1003,3), mdsys.sdo_ordinate_array(100.0, 100.0, 500.0, 500.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Polygon with point and a hole',
  mdsys.sdo_geometry(2003, NULL, MDSYS.SDO_POINT_TYPE(1000.0,1000.0,NULL), mdsys.sdo_elem_info_array(1,1003,3,5,2003,3), mdsys.sdo_ordinate_array(500.0, 500.0, 1500.0, 1500.0, 600.0, 750.0, 900.0, 1050.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('Compound geometry',
  mdsys.sdo_geometry(2004, NULL, NULL, mdsys.sdo_elem_info_array(1,1003,3,5,1,1,9,2,1), mdsys.sdo_ordinate_array(0.0, 0.0, 100.0, 100.0, 50.0, 50.0, 0.0, 0.0, 100.0, 100.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('MultiPoint(2)',
  mdsys.sdo_geometry(2005, NULL, NULL, mdsys.sdo_elem_info_array(1,1,2), mdsys.sdo_ordinate_array(100.0, 100.0, 900.0, 900.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('MultiPolygon rectangles',
  mdsys.sdo_geometry(2007, NULL, NULL, mdsys.sdo_elem_info_array(1,1003,3,5,1003,3), mdsys.sdo_ordinate_array(1500.0, 100.0, 1900.0, 500.0, 1900.0, 500.0, 2300.0, 900.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('3D line',
  mdsys.sdo_geometry(3002, NULL, NULL, mdsys.sdo_elem_info_array(1,2,1), mdsys.sdo_ordinate_array(442.062, 799.423, 63.157, 450.611, 802.963, 63.157)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('2D Line with Measure',
  mdsys.sdo_geometry(3302, NULL, NULL, mdsys.sdo_elem_info_array(1,2,1), mdsys.sdo_ordinate_array(442.062, 799.423, 63.157, 450.611, 802.963, 63.157)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES ('4D line with Measure in 4',
  mdsys.sdo_geometry(4002, NULL, NULL, mdsys.sdo_elem_info_array(1,2,1), mdsys.sdo_ordinate_array(442.062, 802.963, 63.157, 0.0, 450.611, 799.423, 63.157, 1.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES( 'Null Geometry', NULL)
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES( 'Wrong GType', 
  mdsys.sdo_geometry(2002, NULL, NULL, mdsys.sdo_elem_info_array(1,1,2), mdsys.sdo_ordinate_array(100.0, 100.0, 900.0, 900.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('Multiline with duplicate vertices part 1', 
  mdsys.sdo_geometry(2006, NULL, NULL, mdsys.sdo_elem_info_array(1,2,1,7,2,1), mdsys.sdo_ordinate_array(50.0, 15.0, 55.0, 15.0, 55.0, 15.0, 60.0, 15.0, 65.0, 15.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('Multiline with duplicate vertices part 2', 
  mdsys.sdo_geometry(2006, NULL, NULL, mdsys.sdo_elem_info_array(1,2,1,5,2,1), mdsys.sdo_ordinate_array(50.0, 15.0, 55.0, 15.0, 60.0, 15.0, 65.0, 15.0, 65.0, 15.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('Multiline with duplicate vertices parts 1 and 2', 
  mdsys.sdo_geometry(2006, NULL, NULL, mdsys.sdo_elem_info_array(1,2,1,7,2,1), mdsys.sdo_ordinate_array(50.0, 15.0, 55.0, 15.0, 55.0, 15.0, 60.0, 15.0, 65.0, 15.0, 65.0, 15.0)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('Multipolygon - crossing',
  mdsys.sdo_geometry (2007, null, null, mdsys.sdo_elem_info_array (1,1003,1, 11,1003,3), mdsys.sdo_ordinate_array (50,105, 55,105, 60,110, 50,110, 50,105, 54,108, 65,112)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('Polygon with hole - crossing',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,1, 11,2003,3), mdsys.sdo_ordinate_array (50,105, 55,105, 60,110, 50,110, 50,105, 54,108, 65,112)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('Polygon with hole - Wrong SDO_GTYPE',
  mdsys.sdo_geometry (2007, null, null, mdsys.sdo_elem_info_array (1,1003,1, 11,2003,3), mdsys.sdo_ordinate_array (50,105, 55,105, 60,110, 50,110, 50,105, 54,108, 65,112)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13028: Invalid Gtype in the SDO_GEOMETRY object',
  mdsys.sdo_geometry(null,null,null,null,null))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13032: Invalid SDO_POINT_TYPE, SDO_ELEM_INFO_ARRAY or SDO_ORDINATE_ARRAY fields. Set whole SDO_GEOMETRY to NULL instead of setting each field to NULL.', 
  mdsys.sdo_geometry(2001, NULL, NULL, NULL, NULL))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13028: Invalid Gtype in the SDO_GEOMETRY object',
  mdsys.sdo_geometry(2002,null,null,sdo_elem_info_array(1,1003,1),sdo_ordinate_array(1,1,2,2)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13028: Invalid Gtype in the SDO_GEOMETRY object',
  mdsys.sdo_geometry(2002,null,sdo_point_type(1,1,null),sdo_elem_info_array(1,1003,1),sdo_ordinate_array(1,1,2,2)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13031: Invalid Gtype in the SDO_GEOMETRY object for point object',
  mdsys.sdo_geometry(2003,null,sdo_point_type(0,1,null),null,null))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13033: The triplets in SDO_ELEM_INFO_ARRAY field do not make a valid geometry.',
  mdsys.sdo_geometry(2002,null,null,sdo_elem_info_array(1,4,1),sdo_ordinate_array(1,1,2,2)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13034: May be NULL values for X or Y or both in SDO_ORDINATE_ARRAY field.',
  mdsys.sdo_geometry(2002,null,null,sdo_elem_info_array(1,1003,1),sdo_ordinate_array(1,null,2,2)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13035: Invalid data (arcs in geodetic data) in the SDO_GEOMETRY object.',
  mdsys.sdo_geometry (2002, 8311, null, mdsys.sdo_elem_info_array (1,2,2), mdsys.sdo_ordinate_array (10,15, 15,20, 20,15)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13371: invalid position of measure dimension',
mdsys.sdo_geometry (2112, null, null, mdsys.sdo_elem_info_array (1,2,2), mdsys.sdo_ordinate_array (10,15, 15,20, 20,15)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13354: Offset field in ELEM_INFO_ARRAY references an invalid array subscript in SDO_ORDINATE_ARRAY.',
  mdsys.sdo_geometry (2005, null, null, mdsys.sdo_elem_info_array (1,1,4), mdsys.sdo_ordinate_array (65,5, 70,7, 75,5)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13341: Line geometry has fewer than two coordinates.',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,1), mdsys.sdo_ordinate_array (10,15 )))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13342: An arc geometry has fewer than 3 coordinates.',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,2), mdsys.sdo_ordinate_array (10,15, 15,20)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13343: A polygon geometry has fewer than 4 coordinates.',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,1), mdsys.sdo_ordinate_array (10,105, 15,105, 10,105)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13348: polygon boundary is not closed.',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,1), mdsys.sdo_ordinate_array (10,105, 15,105, 20,110, 10,110)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13346: the coordinates defining an arc are collinear',
  mdsys.sdo_geometry (2003, null, null, mdsys.sdo_elem_info_array (1,1003,4), mdsys.sdo_ordinate_array (15,145, 10,150, 10,150)))
INTO ORACLE_TEST_GEOMETRIES (name, geometry) VALUES('13356: Duplicate Vertices',
  mdsys.sdo_geometry (2002, null, null, mdsys.sdo_elem_info_array (1,2,1), mdsys.sdo_ordinate_array (15,145, 10,150, 10,150, 10,150)))
SELECT * FROM DUAL;

COMMIT;

SELECT geometry,name, SC4O.ST_isValidReason(geometry,1) as sc4oValidReason
  FROM ORACLE_TEST_GEOMETRIES a
 WHERE a.geometry is not null
   AND a.geometry.sdo_gtype is not null  -- id=53 breaks validate_geometry_with_context
   AND SC4O.ST_isValidReason(geometry,1) <> 'VALID';


-- Ring Self-intersection at or near point org.locationtech.jts.geom.Coordinate@87e8fae9
-- Self-intersection at or near point org.locationtech.jts.geom.Coordinate@8b49dae9
-- Ring Self-intersection at or near point (20.0 165.0)
-- Self-intersection at or near point (55.0 128.0)

update oracle_test_geometries o
   set o.name = sdo_geom.validate_geometry(geometry,0.05) || ': ' || o.name
 where name not like '13%'
   and sdo_geom.validate_geometry(geometry,0.05) <> 'TRUE';
commit;

-- Test the lot
set serveroutput on size unlimited
DECLARE
   v_err varchar2(10);
BEGIN
   FOR rec IN (SELECT id, geometry FROM ORACLE_TEST_GEOMETRIES) loop
   BEGIN
      v_err := substr(SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(rec.geometry,0.5),1,10); 
      dbms_output.put_line('Geometry with ID - ' || rec.id || ' - validate result is ' || v_err );
      EXCEPTION 
         WHEN OTHERS THEN
            dbms_output.put_line('Geometry with ID - ' || rec.id || ' - failed to validate with ' || SQLCODE );
   END;
   END LOOP;
END;
/

-- Metadata entry
DELETE FROM user_sdo_geom_metadata where table_name = 'ORACLE_TEST_GEOMETRIES' and column_name = 'GEOMETRY';
commit;
INSERT INTO user_sdo_geom_metadata
    (TABLE_NAME,
     COLUMN_NAME,
     DIMINFO,
     SRID)
  VALUES (
  'ORACLE_TEST_GEOMETRIES',
  'GEOMETRY',
  SDO_DIM_ARRAY(   -- 20X20 grid
    SDO_DIM_ELEMENT('X', 0, 500, 0.05),
    SDO_DIM_ELEMENT('Y', 0, 500, 0.05)
     ),
  NULL   -- SRID
);
COMMIT;

PURGE RECYCLEBIN;

quit;


