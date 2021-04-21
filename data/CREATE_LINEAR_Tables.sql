-- *************************************
-- Create a table for routes (highways).
DROP   TABLE lrs_routes PURGE;
CREATE TABLE lrs_routes (
  route_id        NUMBER PRIMARY KEY,
  route_name      VARCHAR2(32),
  route_geometry  SDO_GEOMETRY
);

-- Populate table with just one route for this example.
INSERT INTO lrs_routes VALUES(
  1,
  'Route1',
  SDO_GEOMETRY(
    3302,  -- line string, 3 dimensions: X,Y,M
    NULL,
    NULL,
    SDO_ELEM_INFO_ARRAY(1,2,1), -- one line string, straight segments
    SDO_ORDINATE_ARRAY(
      2,2,0,   -- Start point - Exit1; 0 is measure from start.
      2,4,2,   -- Exit2; 2 is measure from start. 
      8,4,8,   -- Exit3; 8 is measure from start. 
      12,4,12,  -- Exit4; 12 is measure from start. 
      12,10,NULL,  -- Not an exit; measure automatically calculated and filled.
      8,10,22,  -- Exit5; 22 is measure from start.  
      5,14,27)  -- End point (Exit6); 27 is measure from start.
  )
);

-- Update the Spatial metadata.
DELETE FROM user_sdo_geom_metadata where table_name = 'LRS_ROUTES' and column_name = 'ROUTE_GEOMETRY';
commit;
INSERT INTO user_sdo_geom_metadata
    (TABLE_NAME,
     COLUMN_NAME,
     DIMINFO,
     SRID)
  VALUES (
  'lrs_routes',
  'route_geometry',
  SDO_DIM_ARRAY(   -- 20X20 grid
    SDO_DIM_ELEMENT('X', 0, 20, 0.005),
    SDO_DIM_ELEMENT('Y', 0, 20, 0.005),
    SDO_DIM_ELEMENT('M', 0, 20, 0.005) -- Measure dimension
     ),
  NULL   -- SRID
);

-- Create the spatial index.
CREATE INDEX lrs_routes_idx 
          ON lrs_routes(route_geometry)
  INDEXTYPE IS MDSYS.SPATIAL_INDEX;


