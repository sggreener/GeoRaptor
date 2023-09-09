# GeoRaptor for SQL Developer

This is the GeoRaptor extension for SQL Developer 18, 19 and 20.

It is currently in beta. Please raise issues at the project’s github site or contact author by email.

## Installation notes ##

See https://www.spdba.com.au/georaptor-install/

### Manual installation ###

1. Build project with Maven and JDK 11+
2. Create "sqldeveloper/GeoRaptor" folder.
3. Copy ./target/GeoRaptor.zip to "sqldeveloper/GeoRaptor" folder.
4. Start SQL Developer
5. Select Help / Check for Updates
6. Select “Install from local file”
7. Select georaptor.zip file in "sqldeveloper/GeoRaptor" folder.
8. Select Next Button
9. Press Finish to complete install.
10. Restart SQL Developer

### Checking Install ###

1. Check: View / GeoRaptor
2. Check: Preferences/ GeoRaptor
3. Check: Table Node Menu
4. Check: Main GeoRaptor Table Functions
5. Check: Grid Menu and Functions

See "screenshots" directory for details.

### Known bugs and workarounds ###

There is an issue with SQL Developer 20.4 (and probably before) which sees the conversion of an Oracle’s internal Java JGeometry object to SDO_GEOMETRY fail.

(This problem is with the SQL Developer internals, not GeoRaptor, and it took the actual Oracle developers to find the problem.)

Until the Oracle developers finally fix the problem and release a new version, you will have to implement the following workaround.

Add following fragment to sqldeveloper.conf in sqldeveloper/sqldeveloper/bin (and restart):
```
AddVMOption -Dsqldev.jdbcproxy=false
```
