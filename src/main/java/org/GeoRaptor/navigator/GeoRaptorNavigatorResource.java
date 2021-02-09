package org.GeoRaptor.navigator;

import java.awt.Image;
import javax.swing.Icon;
import oracle.dbtools.raptor.utils.MessagesBase;

public class GeoRaptorNavigatorResource extends MessagesBase
{
//  public static final String ACCESS_EXTENSION_ERROR = "ACCESS_EXTENSION_ERROR";
//  public static final String ACCESS_SECURITY_LABEL = "ACCESS_SECURITY_LABEL";
//  public static final String ACCESS_EXTENSION_TITLE = "ACCESS_EXTENSION_TITLE";
//  public static final String ACCESS_SELECT_FILE_LABEL = "ACCESS_SELECT_FILE_LABEL";
//  public static final String ACCESS_BROWSE_LABEL = "ACCESS_BROWSE_LABEL";
  private static final String BUNDLE_NAME = "org.geoRaptor.navigator.GeoRaptorNavigatorResource";
  private static final GeoRaptorNavigatorResource INSTANCE = new GeoRaptorNavigatorResource();

  private GeoRaptorNavigatorResource() {
      super(BUNDLE_NAME,null);
  }

  public static String getString(String paramString)
  {
    return INSTANCE.getStringImpl(paramString);
  }

  public static String get(String paramString) {
    return getString(paramString);
  }

  public static Image getImage(String paramString) {
    return INSTANCE.getImageImpl(paramString);
  }

  public static String format(String paramString, Object[] paramArrayOfObject) {
    return INSTANCE.formatImpl(paramString, paramArrayOfObject);
  }

  public static Icon getIcon(String paramString) {
    return INSTANCE.getIconImpl(paramString);
  }

  public static Integer getInteger(String paramString) {
    return INSTANCE.getIntegerImpl(paramString);
  }
}
