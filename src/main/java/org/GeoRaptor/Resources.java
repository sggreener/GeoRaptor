package org.GeoRaptor;

import java.awt.Image;

import javax.swing.Icon;

import oracle.dbtools.raptor.utils.MessagesBase;

/**
 * 
 * @author Bessie Gong 
 * @version 24 Jul 2019
 *
 * Link to the resources.properties file in src/main/resource/org/GeoRaptor/
 */
public class Resources extends MessagesBase {
	
	private static final ClassLoader CLASS_LOADER = Resources.class.getClassLoader();
	private static final String CLASS_NAME = Resources.class.getCanonicalName();
	private static final Resources INSTANCE = new Resources();	
    
    private Resources() {
    	super(CLASS_NAME, CLASS_LOADER);
    }

    public static String getString( String key ) {
        return INSTANCE.getStringImpl(key);
    }
    
    public static String get( String key ) {
        return getString(key);
    }
    
    public static Image getImage( String key ) {
        return INSTANCE.getImageImpl(key);
    }
    
    public static String format(String key, Object ... arguments) {
        return INSTANCE.formatImpl(key, arguments);
    }

    public static Icon getIcon(String key) {
        return INSTANCE.getIconImpl(key);
    }
    
    public static Integer getInteger(String key) {
        return INSTANCE.getIntegerImpl(key);
    }

    
    /**
     * Get localization message from resource bundle. You can also apply custom paramethers
     * @param _key message key
     * @return localization message
     */
    public static String getMsg(String _key, 
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
    public static String getMsg(String _key) {
        return getMsg(_key, null, null, null, null);
    }
    
    /**
     * Override getMsg(String _key, Object _arg1, Object _arg2, Object _arg3)
     */
    public static String getMsg(String _key, 
                         Object _arg1) {
        return getMsg(_key, _arg1, null, null, null);
    }

    /**
     * Override getMsg(String _key, Object _arg1, Object _arg2, Object _arg3)
     */
    public static String getMsg(String _key, 
                         Object _arg1, 
                         Object _arg2) {
        return getMsg(_key, _arg1, _arg2, null, null);
    }

    public static String getMsg(String _key, 
                         Object _arg1, 
                         Object _arg2,
                         Object _arg3) {
        return getMsg(_key, _arg1, _arg2, _arg3, null);
    }

    /**
     * Override getMsg(String _key, Object _arg1, Object _arg2, Object _arg3)
     */
    public static String getMsg(String   _key, 
                         Object[] _args )
    {
        // get message for resource file
        String message = null;
        try {
            message = getString(_key);
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
