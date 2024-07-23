package org.GeoRaptor.util.logging;

import org.GeoRaptor.MainSettings;
import org.GeoRaptor.Messages;


public class Logger {

	private String loggerClass;

	private String lastMessage = "";

	public Logger(String _classname) {
		this.loggerClass = _classname;
	}

	public String getLoggerClass() {
		return loggerClass;
	}

	public void setLoggerClass(String loggerClass) {
		this.loggerClass = loggerClass;
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}

    public void severe   (String _message) {error(_message);}
    public void error    (String _message) {if (_message!=null) try {Messages.log("error: " + _message,true); System.err.println("error:" + _message);} catch (Exception e) { e.printStackTrace(); } }
    public void warning  (String _message) {warn(_message); }
    public void warn     (String _message) {Messages.log("warning: " + _message,false); }
    
    public void debug    (String _message) { 
		if ( _message.equals(this.lastMessage) )
			return;
    	try {
    		if (MainSettings.getInstance().getPreferences().isDebugMode())
    			System.out.println("debug: " + this.loggerClass + " " + _message);
    			this.lastMessage = _message;
    		} catch (Exception e) {
    			e.printStackTrace(); 
    		} 
    }
    
    public void debug    (int    _message) { this.debug(String.valueOf(_message)); }
    
    public void logSQL   (String _message) { 
    	try {
    		if (MainSettings.getInstance().getPreferences().isLogSearchStats() ) 
    			Messages.log("logSQL: " + _message,false);
    		} catch (Exception e) { 
    			e.printStackTrace(); 
    	} 
    }
    
    public void info     (String _message) {
		if ( _message.equals(this.lastMessage) )
			return;
    	Messages.log("info: " + _message,false);
    	this.lastMessage = _message;
    }
    
    public void info     (int    _message) { this.info(String.valueOf(_message)); }
    
    public void config   (String _message) {Messages.log("config: " + _message,false);}
    public void fine     (String _message) {Messages.log("fine: " + _message,false); }
    public void finer    (String _message) {Messages.log("finer: " + _message,false); }
    public void finest   (String _message) {Messages.log("finest: " + _message,false); }
    
}
