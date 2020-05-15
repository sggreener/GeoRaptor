package org.GeoRaptor.tools;

import java.io.File;

public class FileUtils {
    public FileUtils() {
        super();
    }
    
    
    public static String getDirectory(String _filename) {
        if (Strings.isEmpty(_filename))
            return null;
        File f = new File(_filename);
        String dir = f.getParent();
        if (Strings.isEmpty(dir))
            return dir;
        int lastIndex = dir.contains("\\") ? dir.lastIndexOf("\\") : dir.lastIndexOf("/");
        if ( lastIndex != dir.length() )
            dir += dir.contains("\\") ? "\\" : "/";
        return dir;
    }

    /**
     * Builds a valid, fully specified, file path
     */ 
    public static String FileNameBuilder( String outputDir,  /* output directory with separator on end */
                                          String fileName,  /* filename without suffix */
                                          String fileSuffix /* SHP, DBF, TAB, SHX, PRJ, GML, KML etc */ ) 
    throws IllegalArgumentException {
        
        if (outputDir == null || outputDir.length() == 0) {
            throw new IllegalArgumentException("no output directory");
        }
        if (fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("no output filename specified");
        }
        if (fileSuffix == null || fileSuffix.length() == 0 ) {
            throw new IllegalArgumentException("no output filename suffix eg shp specified.");
        }
    
        int loc;
        loc = outputDir.lastIndexOf(File.separatorChar);
        if (loc == -1) {
            outputDir = File.separatorChar + outputDir; 
        } else {
            // Add separatorChar at end of outputDir if not already the last char
            if ( outputDir.charAt(outputDir.length()-1) != File.separatorChar )
                outputDir += File.separatorChar;
        }
        loc = fileName.lastIndexOf(".");
        if (loc != -1) {
            fileName = fileName.substring(0, loc + 1);
        }
        loc = fileSuffix.lastIndexOf(".");
        if (loc != -1) {
            fileSuffix = fileSuffix.substring(loc + 1);;
        }
        return outputDir + fileName + "." + fileSuffix;
    }
    
    public static String getFileNameFromPath(String  _path,
                                             boolean _noSuffix) {
        String filename = null;
        int lastIndex;
        
        if (_path != null && _path.trim().length() > 0) {
            _path = _path.trim();
            if (! ( _path.endsWith("\\") || _path.endsWith("/")) ) {
                filename = _path;
                lastIndex = _path.contains("\\") ? _path.lastIndexOf("\\") : _path.lastIndexOf("/"); 
                if (lastIndex >= 0) {
                    filename = _path.substring(lastIndex + 1, _path.length());
                }
            }
            if ( _noSuffix && filename.indexOf(".") > 0 )
                filename = filename.substring(0,filename.indexOf("."));
       }
       return filename;
     }

}
