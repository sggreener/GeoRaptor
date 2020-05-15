package org.geotools.util.logging;

public class Logging {

    public Logging() {
    }

    public static Logger getLogger(String _name) {
        return new Logger();
    }
    
    public static Logger getLogger(Class<?> _class) {
        return new Logger();
    }
    
    public static void unexpectedException(Class<?> _class,  String _type, Exception _exception) {
      if (_type.equalsIgnoreCase("remove") )
          return;
    }

    public static void unexpectedException(Class<?> _class,  String _type, AssertionError _error) {
      if (_type.equalsIgnoreCase("remove") )
          return;
    }
  
}
