package org.GeoRaptor;

import oracle.ide.log.LogManager;
import oracle.ide.log.LogPage;

import org.GeoRaptor.tools.Strings;
//import org.GeoRaptor.tools.Tools;

public class Messages {
    public Messages() {
        super();
    }

    /**
     * @param   message - The message itself
     * @author Simon Greener, March 31st 2010
     *           Added overloaded method to make second parameter default to true
     **/
    public static void log(String message) {
        log(message,true);
    }
    
    /**
     * @param   message - The message itself
     * @param   openLogWindow - Makes method open SQL Developer log window. 
     * @author Simon Greener, march 31st 2010
     *           Method made public so other GeoRaptor classes can see it
     *           Added ability to open log window
     *           Made GeoRaptor prefix part of a variable.
     **/
    public static void log(String  message, 
                           boolean openLogWindow) 
    {
        String finalMsg = "";
        if (!Strings.isEmpty(message)) {
            finalMsg = message.toUpperCase().contains("GEORAPTOR") ? message : ("(GeoRaptor) " + message);
        }
        try {
            if ( openLogWindow )
                   LogManager.getLogManager().showLog();
            if ( LogManager.getLogManager().isLogVisible() ) {
                LogPage msgPage = LogManager.getLogManager().getMsgPage();
                msgPage.log(finalMsg + "\n");
            } else {
                System.out.println(finalMsg + "\n");
            }
        }
        catch (IllegalStateException _ise) {
            // we start aplication as standalone, so we just log message in standard output
            System.out.println(finalMsg);
        }
    }
}
