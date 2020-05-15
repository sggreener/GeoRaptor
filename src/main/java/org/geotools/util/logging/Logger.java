package org.geotools.util.logging;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Messages;


public class Logger {

    public void severe   (String _message) {error(_message);}
    public void error    (String _message) {if (_message!=null) { Messages.log("error: " + _message,true); System.err.println("error:" + _message); } }
    public void warning  (String _message) {warn(_message);}
    public void warn     (String _message) {Messages.log("warn: " + _message,false); }
    public void debug    (String _message) {if ( Constants.DEBUG ) { Messages.log("debug:" + _message,false); } }
    public void debug    (int    _message) {if ( Constants.DEBUG ) { Messages.log("debug:" + _message,false); } }
    public void info     (String _message) {Messages.log("info: " + _message,false); }
    public void info     (int    _message) {Messages.log("info: " + _message,false); }
    public void logSQL   (String _message) {if (MainSettings.getInstance().getPreferences().isLogSearchStats() ) Messages.log("logSQL: " + _message,false); }
    public void config   (String _message) {Messages.log("config: " + _message,false);}
    public void fine     (String _message) {Messages.log("debug: " + _message,false); }
    public void finer    (String _message) {Messages.log("debug: " + _message,false); }
    public void finest   (String _message) {Messages.log("trace: " + _message,false); }
    
}
