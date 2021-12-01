# GeoRaptor for SQL Developer

This is the GeoRaptor extension for SQL Developer 18, 19 and 20.

It is currently in beta. Please raise issues at the project’s github site or contact me by email.

## Installation notes ##
There is an issue with SQL Developer 20.4 (and probably before) which sees the conversion of an Oracle’s internal Java JGeometry object to SDO_GEOMETRY fail.

(This problem is with the SQL Developer internals, not GeoRaptor, and it took the actual Oracle developers to find the problem.)

Until the Oracle developers finally fix the problem and release a new version, you will have to implement the following workaround.

Add following fragment to sqldeveloper.conf in sqldeveloper/sqldeveloper/bin (and restart):
```
AddVMOption -Dsqldev.jdbcproxy=false
```
