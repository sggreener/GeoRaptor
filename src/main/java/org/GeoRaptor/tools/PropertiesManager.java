package org.GeoRaptor.tools;

import java.text.MessageFormat;

import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Simon Greener, April 27th 2010
 *         Created out of duplicate getMsg handling code in three other classes.
 **/

public class PropertiesManager {
    
    /**
     * Reference to class instance's resource bundle 
     */
    protected ResourceBundle rBoundle;

    /**
     * Message formater for class instance's Resource Boundle. 
     * Use when .getMsg function is called.
     */
    protected MessageFormat formatter;

    public PropertiesManager(String _place) 
    throws IllegalArgumentException
    {
        super();
        // eg "org.GeoRaptor.OracleSpatial.Metadata.MetadataPanel";
        if (Strings.isEmpty(_place) )
            throw new IllegalArgumentException("Resource name must be provided");
        
        this.rBoundle = ResourceBundle.getBundle(_place, Locale.getDefault());

        // support for internationalization
        this.formatter = new MessageFormat("", Locale.getDefault());
    }
    
    /**
     * Get localization message from resource bundle. You can also apply custom paramethers
     * @param _key message key
     * @return localization message
     */
    public String getMsg(String _key, 
                         Object _arg1, 
                         Object _arg2,
                         Object _arg3,
                         Object _arg4) 
    {
        Object args[] = null;

        // set object array
        if ((_arg1 != null) && (_arg2 != null) && (_arg3 != null) && (_arg4 != null)) {
            args = new Object[4];
            args[0] = _arg1;
            args[1] = _arg2;
            args[2] = _arg3;
            args[3] = _arg4;
        } else if ((_arg1 != null) && (_arg2 != null) && (_arg3 != null)) {
            args = new Object[3];
            args[0] = _arg1;
            args[1] = _arg2;
            args[2] = _arg3;
        } else if ((_arg1 != null) && (_arg2 != null)) {
            args = new Object[2];
            args[0] = _arg1;
            args[1] = _arg2;
        } else if (_arg1 != null) {
            args = new Object[1];
            args[0] = _arg1;
        }
        return getMsg(_key,args);
    }

    /**
     * Override getMsg(String _key, Object _arg1, Object _arg2, Object _arg3)
     */
    public String getMsg(String _key) {
        return getMsg(_key, null, null, null, null);
    }
    
    /**
     * Override getMsg(String _key, Object _arg1, Object _arg2, Object _arg3)
     */
    public String getMsg(String _key, 
                         Object _arg1) {
        return getMsg(_key, _arg1, null, null, null);
    }

    /**
     * Override getMsg(String _key, Object _arg1, Object _arg2, Object _arg3)
     */
    public String getMsg(String _key, 
                         Object _arg1, 
                         Object _arg2) {
        return getMsg(_key, _arg1, _arg2, null, null);
    }

    public String getMsg(String _key, 
                         Object _arg1, 
                         Object _arg2,
                         Object _arg3) {
        return getMsg(_key, _arg1, _arg2, _arg3, null);
    }

    /**
     * Override getMsg(String _key, Object _arg1, Object _arg2, Object _arg3)
     */
    public String getMsg(String   _key, 
                         Object[] _args )
    {
        // get message for resource file
        String message = null;
        if (this.rBoundle == null) {
            message = "ERROR_RB_FOR_KEY: '" + _key + "'";
        } // if
        try {
            message = this.rBoundle.getString(_key);
        } catch (Exception _e) {
            message = "ERROR_FOR_KEY: '" + _key + "' ; " + _e;
        } // try - catch
        // fill message if arguments are provided
        if (_args != null) {
            message = java.text.MessageFormat.format(message,_args);
        }
        return message;
    } // getMsg


}
