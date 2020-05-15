package org.GeoRaptor.tools;

import javax.swing.JDialog;
import javax.swing.JOptionPane;


public class ErrorDialogHandler 
{
    /** 
     * Properties File Manager
     **/
    private String propertiesFile = null;
    protected PropertiesManager propertyManager;

    public ErrorDialogHandler(String _propertiesFile) 
    throws Exception
    {
        super();
        if (Strings.isEmpty(_propertiesFile) ) {
          throw new Exception("null properties file");
        }
        this.propertiesFile = _propertiesFile;
        this.propertyManager = new PropertiesManager(this.propertiesFile);
    }
    
    /** ===================================================================================
     * Show default (JOptionPane) dialog with given message. Message can also have arguments
     * @param _messageID message ID in ResourceBundle class
     * @param _args Array of all arguments (can be null)
     */
     public void showErrorDialog(JDialog _dialog, 
                                 String _messageID, 
                                 Object[]  _args) 
     {
         String message = this.propertyManager.getMsg(_messageID, _args);
         if ( _dialog != null && _dialog.isVisible() ) {
             _dialog.setAlwaysOnTop(false);
         }
         JOptionPane.showMessageDialog(_dialog, 
                                       message, 
                                       this.propertyManager.getMsg("ERROR_MESSAGE_DIALOG_TITLE"),
                                       JOptionPane.ERROR_MESSAGE);
         if ( _dialog != null && _dialog.isVisible() ) {
             _dialog.setAlwaysOnTop(true);
         }
     }

    /**
     * Override showErrorDialog(String _messageID, Object _arg1, Object _arg2) method
     */
    public void showErrorDialog(JDialog _dialog, String _messageID) {
        this.showErrorDialog(_dialog,_messageID, null, null, null, null);
    }

    /**
     * Override showErrorDialog(String _messageID, Object _arg1, Object _arg2) method
     */
    public void showErrorDialog(JDialog _dialog, String _messageID, Object _arg1) {
        this.showErrorDialog(_dialog, _messageID, _arg1, null, null, null);
    }
    
    /**
     * Override showErrorDialog(String _messageID, Object _arg1, Object _arg2) method
     */
    public void showErrorDialog(JDialog _dialog, String _messageID, Object _arg1, Object _arg2) {
        this.showErrorDialog(_dialog, _messageID, _arg1, _arg2, null, null);
    }

    public void showErrorDialog(JDialog _dialog, 
                                String _messageID, 
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
        this.showErrorDialog(_dialog, _messageID, args);
    }
    
}
