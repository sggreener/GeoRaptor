package org.GeoRaptor.tools;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.GeoRaptor.Constants;

public class Strings {
    public Strings() {
        super();
    }
    
  /****************** STRING ************************/
  
  public static String toProperCase(String input) {
      //A pattern for all (UNICODE-) lower case characters preceded by a word boundary
      Pattern p = Pattern.compile("\\b([\\p{javaLowerCase}])",Pattern.UNICODE_CASE);
      Matcher m = p.matcher(input.toLowerCase(Locale.getDefault()));
      StringBuffer sb = new StringBuffer(input.length());
      while (m.find()) {
          m.appendReplacement(sb, m.group(1).toUpperCase());
      }
      m.appendTail(sb);
      return sb.toString();  
  }  
  
  public static String TitleCase(String string){
    String result = "";
    for (int i = 0; i < string.length(); i++){
      String next = string.substring(i, i + 1);
      if (i == 0){
        result += next.toUpperCase();
      } else {
        result += next.toLowerCase();
      }
    }
    return result;
  }
  
  /**
   * Trim specified character from front of string
   * 
   * @param text
   *          Text
   * @param character
   *          Character to remove
   * @return Trimmed text
   */
  public static String trimFront(String text, char character) {
    String normalizedText;
    int index;

    if (isEmpty(text)) {
      return text;
    }

    normalizedText = text.trim();
    index = 0;

    while (normalizedText.charAt(index) == character) {
      index++;
    }
    return normalizedText.substring(index).trim();
  }

  /**
   * Trim specified character from end of string
   * 
   * @param text
   *          Text
   * @param character
   *          Character to remove
   * @return Trimmed text
   */
  public static String trimEnd(String text, char character) {
    String normalizedText;
    int index;

    if (isEmpty(text)) {
      return text;
    }

    normalizedText = text.trim();
    index = normalizedText.length() - 1;

    while (normalizedText.charAt(index) == character) {
      if (--index < 0) {
        return "";
      }
    }
    return normalizedText.substring(0, index + 1).trim();
  }

  /**
   * Trim specified character from both ends of a String
   * 
   * @param text
   *          Text
   * @param character
   *          Character to remove
   * @return Trimmed text
   */
  public static String trimAll(String text, char character) {
    String normalizedText = trimFront(text, character);

    return trimEnd(normalizedText, character);
  }
  
  public static String condense(String _name) 
  { 
      // get rid of all vowels!
      Pattern p = Pattern.compile("([AEIOU|aeiou]){1,}");
      Matcher m = p.matcher(_name);
      if (m.find())
          return m.replaceAll("");
      return _name;
  } 

    public static String Left(String _text, int _length)
    {
         if (_length <= 0)
            return "";
         else if (_length > _text.length())
            return _text;
         else {
            return _text.substring(0,(_length-1));
         }
    }  

    public static String Right(String _text, int _length)
    {
        if (_length <= 0)
            return "";
         else if (_length > _text.length())
            return _text;
         else {
            int iLen = _text.length() - _length;
            return _text.substring(iLen);
         }
    }  
          
  /**
    * Replaces characters that may be confused by a HTML
    * parser with their equivalent character entity references.
    * 
    * Any data that will appear as text on a web page should
    * be be escaped.  This is especially important for data
    * that comes from untrusted sources such as Internet users.
    * A common mistake in CGI programming is to ask a user for
    * data and then put that data on a web page.  For example:<pre>
    * Server: What is your name?
    * User: &lt;b&gt;Joe&lt;b&gt;
    * Server: Hello <b>Joe</b>, Welcome</pre>
    * If the name is put on the page without checking that it doesn't
    * contain HTML code or without sanitizing that HTML code, the user
    * could reformat the page, insert scripts, and control the the
    * content on your web server.
    * 
    * This method will replace HTML characters such as &gt; with their
    * HTML entity reference (&amp;gt;) so that the html parser will
    * be sure to interpret them as plain text rather than HTML or script.
    * 
    * This method should be used for both data to be displayed in text
    * in the html document, and data put in form elements. For example:<br>
    * <code>&lt;html&gt;&lt;body&gt;<i>This in not a &amp;lt;tag&amp;gt;
    * in HTML</i>&lt;/body&gt;&lt;/html&gt;</code><br>
    * and<br>
    * <code>&lt;form&gt;&lt;input type="hidden" name="date" value="<i>This data could
    * be &amp;quot;malicious&amp;quot;</i>"&gt;&lt;/form&gt;</code><br>
    * In the second example, the form data would be properly be resubmitted
    * to your cgi script in the URLEncoded format:<br>
    * <code><i>This data could be %22malicious%22</i></code>
    *
    * @param s String to be escaped
    * @return escaped String
    * @throws NullPointerException if s is null.
    *
    * @since ostermillerutils 1.00.00
    */
   public static String escapeHTML(String s){
     int length = s.length();
     int newLength = length;
     boolean someCharacterEscaped = false;
     // first check for characters that might
     // be dangerous and calculate a length
     // of the string that has escapes.
     for (int i=0; i<length; i++){
       char c = s.charAt(i);
       int cint = 0xffff & c;
       if (cint < 32){
         switch(c){
           case '\r':
           case '\n':
           case '\t':
           case '\f':{
           } break;
           default: {
             newLength -= 1;
             someCharacterEscaped = true;
           }
         }
       } else {
         switch(c){
           case '\"':{
             newLength += 5;
             someCharacterEscaped = true;
           } break;
           case '&':
           case '\'':{
             newLength += 4;
             someCharacterEscaped = true;
           } break;
           case '<':
           case '>':{
             newLength += 3;
             someCharacterEscaped = true;
           } break;
         }
       }
     }
     if (!someCharacterEscaped){
       // nothing to escape in the string
       return s;
     }
     StringBuffer sb = new StringBuffer(newLength);
     for (int i=0; i<length; i++){
       char c = s.charAt(i);
       int cint = 0xffff & c;
       if (cint < 32){
         switch(c){
           case '\r':
           case '\n':
           case '\t':
           case '\f':{
             sb.append(c);
           } break;
           default: {
             // Remove this character
           }
         }
       } else {
         switch(c){
           case '\"':{
             sb.append("&quot;");
           } break;
           case '\'':{
             sb.append("&#39;");
           } break;
           case '&':{
             sb.append("&amp;");
           } break;
           case '<':{
             sb.append("&lt;");
           } break;
           case '>':{
             sb.append("&gt;");
           } break;
           default: {
             sb.append(c);
           }
         }
       }
     }
     return sb.toString();
   }

    public static boolean isEmpty(String s) {
        return (s == null || s.trim().isEmpty());
    }

    /**
     * @function append
     * @precis appends two non-null strings togther with separator if required
     * @param a
     * @param b
     * @param separator
     * @return String a, b or a<sep>b
     * @author Simon Greener, April 20th 2010, Original coding
     */
    public static String append(String a, String b, String separator) {
        if (Strings.isEmpty(a))
            return b;
        if (Strings.isEmpty(b))
            return a;
        return a + separator + b;
    }
    
    /**
     * @function objectString
     * @param _schemaName
     * @param _tableName
     * @param _columnName
     * @return string
     * @author Simon Greener, April 20th 2010
     *          Generates <schema><sep><table><sep><column> string depending on 
     *          nullability of input strings
     */
    public static String objectString (String _schemaName,
                                       String _tableName,
                                       String _columnName,
                                       String _separator )
    {
        String separator = (Strings.isEmpty(_separator) ? Constants.TABLE_COLUMN_SEPARATOR : _separator );
        return Strings.append(Strings.append(_schemaName,
                                         _tableName,
                                         separator),
                            _columnName,
                            separator);
    }

    /**
     * @function objectString
     * @param _schemaName
     * @param _tableName
     * @param _columnName
     * @return String
     * @author Simon Greener, April 21st 2010, Overload of above
     */
    public static String objectString (String _schemaName,
                                       String _tableName,
                                       String _columnName)
    {
        return objectString(_schemaName,_tableName,_columnName,Constants.TABLE_COLUMN_SEPARATOR);
    }

}
