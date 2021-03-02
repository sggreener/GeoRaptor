package org.GeoRaptor.sql;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.GeoRaptor.tools.Strings;
import org.geotools.util.logging.Logger;

import oracle.dbtools.raptor.utils.Connections;
import oracle.javatools.db.DBException;
import oracle.jdbc.OracleConnection;

public class DatabaseConnection {

    private static final Logger LOGGER = 
    		org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.sql.DatabaseConnection");
    
    public String       connectionName;
    public String          displayName;
    
    public DatabaseConnection(String _connectionName) {
        super();
        this.connectionName = _connectionName;
        setDisplayName();
    }
    
    public void setDisplayName(String _displayName) {
        this.displayName = _displayName;
    }

    public void setDisplayName() {
        this.displayName = this.getDisplayName();
    }

    public String getDisplayName() {
        if (Strings.isEmpty(this.displayName) ) {
            return Connections.getDisplayName(this.connectionName);
        }
        return this.displayName;
    }

    public boolean exists() {
        if (Strings.isEmpty(this.connectionName) ) {
            return Strings.isEmpty(Connections.getDisplayName(this.connectionName));
        }
        return false;
    }

    public String toString() {
        return this.getConnectionName();
    }

    public void setConnectionName(String _connectionName) {
        this.connectionName = _connectionName;
    }

    public String getConnectionName() {
        return this.connectionName;
    }

    public String getPrettyConnectionName() {
        String prettyName;
        try {
            prettyName = URLDecoder.decode(this.connectionName, "UTF-8");
            int pos = prettyName.indexOf("#");
            prettyName = prettyName.substring(pos + 1);
        } catch (UnsupportedEncodingException e) {
            prettyName = "Empty";
        }
        return prettyName;
    }
    
    public String getUserName() {
        try {
            Connection conn = this.getConnection();
            return conn.getSchema();
        } catch (SQLException e) {
            return "";
        }
    }

    public String getCurrentSchema() {
        try {
            Connection conn = this.getConnection();
            return ( conn!=null ) ? conn.getSchema() : "";
        } catch (SQLException e) {
            return "";
        }
    }

    public String getConnectionUsername() {
        Properties myConnProps;
        myConnProps = Connections.getInstance().getConnectionInfo(this.connectionName);
        if (myConnProps == null)
            return null;
        return myConnProps.getProperty("user");
    }

    public String getConnectionPassword() {
        Properties myConnProps;
        myConnProps = Connections.getInstance().getConnectionInfo(this.connectionName);
        if (myConnProps == null)
            return null;
        return myConnProps.getProperty("password");
    }

    public String getConnectionHostname() {
        Properties myConnProps;
        myConnProps = Connections.getInstance().getConnectionInfo(this.connectionName);
        if (myConnProps == null)
            return null;
        return myConnProps.getProperty("hostname");
    }

    public String getConnectionPort() {
        Properties myConnProps;
        myConnProps = Connections.getInstance().getConnectionInfo(this.connectionName);
        if (myConnProps == null)
            return null;
        return myConnProps.getProperty("port");
    }

    public String getConnectionSID() {
        Properties myConnProps;
        myConnProps = Connections.getInstance().getConnectionInfo(this.connectionName);
        if (myConnProps == null)
            return null;
        return myConnProps.getProperty("serviceName");
    }

    public Connection getConnection() {
        Connection conn = null;
        if (Strings.isEmpty(this.connectionName) )
            return conn;
        try {
            //conn = (Connection)DriverManager.getConnection("jdbc:oracle:thin:@localhost:1522:GISDB12", "georaptor", "georaptor");
            conn = Connections.getInstance().getConnection(this.connectionName,true);
            if (conn.isWrapperFor(OracleConnection.class)){
            	return (OracleConnection)conn.unwrap(OracleConnection.class);  
            } else {
            	return conn;
          	}
        } catch (Exception e) {
            LOGGER.error("Problem getting connection (" + this.connectionName + ") of " + e.toString());
        }
        return conn;
    }

    public boolean isOpen() {
        return Connections.getInstance().isConnectionOpen(this.connectionName);
    }

    public boolean reOpen() {
    LOGGER.debug("reOpen");
        // Try and reopen
        this.close();
        Connection conn;
        try {
        	LOGGER.debug("Trying to reopen " + this.connectionName);
            conn = Connections.getInstance().getConnection(this.connectionName);
            return this.checkConnection(conn);
        } catch (DBException e) {
        	LOGGER.debug("reopen failed with " + e.toString());
        }
        return false;
    }

    public boolean close() {
        boolean c = Connections.getInstance().closeConnection(this.connectionName);
        return c;
    }
    
	public boolean checkConnection(Connection _conn) {
		LOGGER.debug("checkConnection _conn = " + (_conn==null?"null":"not null"));
        if ( _conn == null )
            return false;
        try {
        	LOGGER.debug("_conn.isValid()=" + _conn.isValid(0));
            if ( _conn.isValid(0) ) {
                return false;
            }
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

}
