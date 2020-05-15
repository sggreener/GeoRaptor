package org.GeoRaptor.sql;

import java.sql.Connection;

import java.sql.SQLException;

import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.JComboBox;

import oracle.dbtools.raptor.config.DBConfig;
import oracle.dbtools.raptor.connections.ConnectionEvent;
import oracle.dbtools.raptor.connections.ConnectionListener;
import oracle.dbtools.raptor.utils.Connections;

import oracle.javatools.db.DBException;

import oracle.jdbc.OracleConnection;

import org.GeoRaptor.Messages;
import org.GeoRaptor.SpatialView.JDevInt.DockableSV;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.SpatialView.SpatialViewPanel;
import org.GeoRaptor.SpatialView.layers.SVSpatialLayer;
import org.GeoRaptor.tools.Strings;

import org.geotools.util.logging.Logger;


public class DatabaseConnections {

    private static final Logger                      LOGGER = org.geotools.util.logging.Logging.getLogger("org.GeoRaptor.sql.DatabaseConnections");
    private static DatabaseConnections            _instance = null;
    private int                             initialCapacity = 255;
    private LinkedHashMap<String, DatabaseConnection> cache = null;
    
    public DatabaseConnections() {
        super();
        this.setCache();
        this.setConnections(true);
        Connections.getInstance().addConnectionListener(new ConnectionListener() {

            private void addSQLConnection(ConnectionEvent evt) {
                Connection conn = null;
                try {
                    conn = Connections.getInstance().getConnection(evt.getConnectionName());
                    if (conn instanceof OracleConnection) { 
                        ((OracleConnection)conn).setDefaultRowPrefetch(DBConfig.getInstance().getInt(DBConfig.ARRAYFETCHSIZE));
                        addConnection(new DatabaseConnection(evt.getConnectionName()));
                    }
                } catch (DBException dbe) {
                    LOGGER.error("DatabaseConnections failed to instantiate: " + dbe.getMessage());
                } catch (SQLException sqle) {
                    LOGGER.error("DatabaseConnections failed to instantiate: " + sqle.getMessage());
                }
            }
         public void connectionAdded(ConnectionEvent evt) {
             //LOGGER.debug("Connection - "+evt.getConnectionName() + " Added.");
             addSQLConnection(evt);
         }
         public void connectionClosed(ConnectionEvent evt) {
             //LOGGER.debug("Connection - "+evt.getConnectionName() + " Closed.");
         }
         public void connectionModified(ConnectionEvent evt) {
             //LOGGER.debug("Connection - "+evt.getConnectionName() + " Modified.");
         }
         public void connectionOpened(ConnectionEvent evt) {
             //LOGGER.debug("Connection - "+evt.getConnectionName() + " Opened.");
             addSQLConnection(evt);
         }
         public void connectionRemoved(ConnectionEvent evt) {
             DatabaseConnections.getInstance().removeConnection(evt.getConnectionName());
         }
         public void connectionRenamed(ConnectionEvent evt) {
             DatabaseConnections.getInstance().removeConnection(evt.getOldName());
             addSQLConnection(evt);
             // Change layers that have oldName to newName
             //
             SpatialViewPanel svPanel = DockableSV.getSpatialViewPanel();
             Iterator<String> it = svPanel.getViews().keySet().iterator();
             SpatialView itView = null;
             while (it.hasNext()) {
                 itView = svPanel.getViews().get(it.next());
                 Iterator<String> layerNamesIter = itView.getLayerList().keySet().iterator();
                 while (layerNamesIter.hasNext()) {
                     SVSpatialLayer sLayer = itView.getLayerList().get(layerNamesIter.next());
                     if ( sLayer.getConnectionName().equalsIgnoreCase(evt.getOldName()) ) {
                         sLayer.setConnectionName(evt.getConnectionName());
                     }
                 }
             }
         }
         });
    }
    
    public static DatabaseConnections getInstance() {
        if (_instance == null) {
             _instance = new DatabaseConnections();
        }
       return _instance;
     }

    private void setCache() {
        if ( this.cache==null || this.cache.size()==0 ) {
            this.cache = new LinkedHashMap<String, DatabaseConnection>(this.initialCapacity);
        } 
    }

    public int getConnectionCount() {
        return this.cache==null?-1:this.cache.size();
    }
    
    public void setInitialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    public int getInitialCapacity() {
        return this.initialCapacity;
    }
    
     public static String getActiveConnectionName() 
     {
        return Connections.getActiveConnectionName();
     }

    public OracleConnection getActiveConnection() 
    {
        OracleConnection dbConn;
        try {
            dbConn = (OracleConnection)Connections.getInstance().getConnection(Connections.getActiveConnectionName());
        } catch (DBException e) {
            LOGGER.error("getActiveConnection failed with " + e.getMessage());
            return null;
        }
        return dbConn==null?null:dbConn;
    }

    public OracleConnection getAnyOpenConnection() 
    {
        this.setConnections(true);
        OracleConnection conn = getActiveConnection();
        if ( conn==null ) {
           // Iterate over the whole set...
           Iterator<DatabaseConnection> iter = this.cache.values().iterator();
           while (iter.hasNext()) {
               DatabaseConnection dbConn = iter.next();
               if (dbConn.isOpen()) {
                   conn = dbConn.getConnection();
                   break;
               }
           }
        }
        return conn;
    }
    
    public OracleConnection getConnection(String  _connName)
    {
        //LOGGER.debug("DatabaseConnections.getConnection("+_connName+")");
        DatabaseConnection dbConn = this.findConnectionByName(_connName);
        if ( dbConn == null ) {
            //LOGGER.debug("DatabaseConnections.getConnection - findConnectionByName returned null");
            return null;
        }
        return dbConn.getConnection();
    }
    
    public javax.swing.DefaultComboBoxModel<String> getComboBoxModel(String _defaultConnName) 
    {
        if ( this.cache == null || this.cache.size()==0 ) {
            if ( !Strings.isEmpty(_defaultConnName) ) {
                this.addConnection(_defaultConnName);
            } else {
                return (javax.swing.DefaultComboBoxModel<String>)null;
            }
        }
        javax.swing.DefaultComboBoxModel<String> cm = new javax.swing.DefaultComboBoxModel<String>();
        Iterator<DatabaseConnection>           iter = this.cache.values().iterator();
        while (iter.hasNext()) {
            DatabaseConnection dbConn = iter.next();
            if (dbConn.isOpen()) {
            	String displayName = dbConn.getDisplayName(); 
                cm.addElement(displayName);
            }
        }
        return cm;
    }

    public String[] getConnectionNames(String _defaultConnName) 
    {
        if ( this.cache == null || this.cache.size()==0 ) {
            if ( !Strings.isEmpty(_defaultConnName) ) {
                this.addConnection(_defaultConnName);
            } else {
                return null;
            }
        }
        java.util.ArrayList<String> cm = new java.util.ArrayList<String>(this.cache.size());
        Iterator<DatabaseConnection>           iter = this.cache.values().iterator();
        while (iter.hasNext()) {
            DatabaseConnection dbConn = iter.next();
            if (dbConn.isOpen()) {
            	String displayName = dbConn.getDisplayName(); 
                cm.add(displayName);
            }
        }
        return (String[])cm.toArray();
    }

    public JComboBox<String> getConnectionsCombo() 
    {
        return new JComboBox<String>(this.getComboBoxModel(null));
    }

    public LinkedHashMap<String, DatabaseConnection> getConnections() {
        return this.cache;
    }

    public void removeConnection(String _connName) {
    	if ( this.connectionExists(_connName) ) {
            this.cache.remove(_connName);
        }
    }

    public void removeAll() {
        //LOGGER.debug("DatabaseConnections.removeAll()");
        if ( this.cache == null || this.cache.size()==0 ) {
            return;
        }
        this.cache.clear();
    }

    public boolean connectionExists(String  _connName) 
    {
        //LOGGER.debug("DatabaseConnections.connectionExists("+_connName+")");
        if ( this.cache == null || this.cache.size()==0 || Strings.isEmpty(_connName)) {
            return false;
        }
        DatabaseConnection dbConn = this.cache.get(_connName);
        //LOGGER.debug("    this.cache.get("+_connName+")=" + (dbConn==null ? "false" : "true"));
        return ( dbConn==null ? false : true);
    }

    public boolean isConnectionOpen(String _connName) 
    {
        if ( ! connectionExists(_connName)) {
            return false;
        }
        DatabaseConnection dbConn = this.cache.get(_connName);
        if ( dbConn==null ) {
            return false;
        }
        return dbConn.isOpen();
    }

    public boolean openConnection(String  _connName) 
    {
        if ( ! connectionExists(_connName)) {
            return false;
        }
        DatabaseConnection dbConn = this.cache.get(_connName);
        if ( dbConn==null ) {
            return false;
        }
        if ( dbConn.isOpen() ) {
            return true;
        } else {
            return dbConn.reOpen();
        }
    }

    public DatabaseConnection findConnectionName(String  _connName) 
    {
        if (Strings.isEmpty(_connName) || this.cache == null || this.cache.size()==0 ) {
            return null;
        }
        //LOGGER.debug("DatabaseConnections.findConnectionName(" + _connName + ")");
        DatabaseConnection dbConn = this.cache.get(_connName);
        if ( dbConn==null ) {
            // Not in GeoRaptor's cached set of connections
            // See if SQL Developer has one in case listener not synchronised
            //
            this.setConnections(true);
            Iterator<DatabaseConnection> iter = this.cache.values().iterator();
            while (iter.hasNext()) {
                dbConn = iter.next();
                if ( dbConn.getConnectionName().equalsIgnoreCase(_connName) ) {
                    break;
                }
            }
        }
        return dbConn;
    }
    
    public String getConnectionDisplayName(String _connName) 
    {
        if (Strings.isEmpty(_connName) ) {
            return "";
        }
        //LOGGER.debug("DatabaseConnections.getConnectionDisplayName("+_connName+")");
        DatabaseConnection dbConn = this.findConnectionName(_connName);
        if ( dbConn == null ) {
           //LOGGER.debug("DatabaseConnections.getConnectionDisplayName().findConnectionName() returned null");
           return _connName;
        }
        //LOGGER.debug("DatabaseConnections.getConnectionDisplayName().findConnectionName() returned " + dbConn.getDisplayName());
        return dbConn.getDisplayName();
    }

    public DatabaseConnection findDisplayName(String  _displayName) 
    {
        if ( this.cache == null || this.cache.size()==0 || Strings.isEmpty(_displayName)) {
            return null;
        }
        DatabaseConnection dbConn = null;
        Iterator<DatabaseConnection> iter = this.cache.values().iterator();
        while (iter.hasNext()) {
            dbConn = iter.next();
            if ( dbConn.getDisplayName().equalsIgnoreCase(_displayName) ) {
                return dbConn;
            }
        }
        return null;
    }

    public DatabaseConnection findConnectionByName(String  _connName) 
    {
        //LOGGER.debug("DatabaseConnections.findConnectionByName("+_connName+")");
        DatabaseConnection dbConn = DatabaseConnections.getInstance().findConnectionName(_connName);
        if ( dbConn == null ) {
            // Try other way around
            dbConn = DatabaseConnections.getInstance().findDisplayName(_connName);
            if ( dbConn == null ) {
                this.setConnections(false);
                dbConn = DatabaseConnections.getInstance().findConnectionName(_connName);
                if ( dbConn == null ) {
                    // Try other way around
                    dbConn = DatabaseConnections.getInstance().findDisplayName(_connName);
                }
            }
        }
        return dbConn == null ? null : dbConn;
    }

    public DatabaseConnection getConnectionAt(int _index) {
        if ( this.cache == null || this.cache.size()==0 || (_index > this.cache.size()-1) )
            return null;
        DatabaseConnection dbConn = null;
        // LinkedHashMap iterates in the order in which the entries were put into the map
        int i = 0; // is 0 based
        Iterator<DatabaseConnection> iter = this.cache.values().iterator();
        while (iter.hasNext()) {
            dbConn = iter.next();
            if ( _index == i ) {
                return dbConn;
            }
            i++;
        }
        LOGGER.error("Null DatabaseConnection at index "+ _index);
        return null;
    }

    public void addConnection(String _connName) {
        //LOGGER.debug("DatabaseConnections.addConnection");
        if (Strings.isEmpty(_connName) ) {
            return;
        }
        this.setCache();
        // Find by display or connection name
        //
        DatabaseConnection dbConn  = this.findConnectionByName(_connName);
        if (dbConn!=null) {
            //LOGGER.debug("Connection already exists in Connections cache.");
            return;
        } else {
            //LOGGER.debug("Connection does not exist in Connections cache, so add it.");
            this.cache.put(_connName,new DatabaseConnection(_connName));
        }
    }

    public void addConnection(DatabaseConnection _dbConnection) {
        if ( _dbConnection == null) {
            return;
        }
        this.setCache();
        // Don't add it twice
        if ( this.connectionExists(_dbConnection.getConnectionName()) ) {
            return;
        }
        this.cache.put(_dbConnection.getConnectionName(),_dbConnection);
    }
 
    // Refreshes connections to contain only those that are open
    //
    public void setConnections(boolean _openOnly) {
        //LOGGER.debug("DatabaseConnections.setConnections("+_openOnly+")");
        
        // Clear current cache
        this.removeAll();
        this.setCache();
        
        // Add in connections depending on if open or not
        //
        try {
            String   connName = "";
            for (int i=0;i<Connections.getInstance().getConnNames().length; i++) 
            {
                try {
                    connName = Connections.getInstance().getConnNames()[i];
                    //LOGGER.debug("refreshConnections - Checking (" + connName + ")");
                    if (( ! _openOnly ) ||
                          ( _openOnly && Connections.getInstance().isConnectionOpen(connName) )) {
                        this.cache.put(connName,new DatabaseConnection(connName));                    
                        //LOGGER.debug("DatabaseConnections.setConnections " + connName + " loaded.");
                    }
                } catch (Exception _e) {
                    int l = "java.sql.SQLException:".length();
                    String actualErr = _e.toString().substring(_e.toString().lastIndexOf("java.sql.SQLException:")+l);
                    // Check if network adapter can't make connection
                    LOGGER.error("DatabaseConnections.setConnections (" + connName + ") produced error " + actualErr);
                }
            }
        } catch (IllegalStateException ise) {
            LOGGER.warn("DatabaseConnections.setConnections: "+ise.getMessage());
        }
    }
                           
    public void print() {
        if ( this.cache == null || this.cache.size()==0 ) {
            return;
        }
        Iterator<DatabaseConnection> iter = this.cache.values().iterator();
        while (iter.hasNext()) {
            DatabaseConnection dbConn = iter.next();
            Messages.log(dbConn.toString());
        }
    }

}
