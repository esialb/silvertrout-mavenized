/*   _______ __ __                    _______                    __   
 *  |     __|__|  |.--.--.-----.----.|_     _|.----.-----.--.--.|  |_ 
 *  |__     |  |  ||  |  |  -__|   _|  |   |  |   _|  _  |  |  ||   _|
 *  |_______|__|__| \___/|_____|__|    |___|  |__| |_____|_____||____|
 * 
 *  Copyright 2008 - Gustav Tiger, Henrik Steen and Gustav "Gussoh" Sohtell
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package silvertrout.commons;

import java.nio.ByteBuffer;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.IDN;
import java.net.URL;
import java.net.URLEncoder;

import java.net.HttpURLConnection;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 *
 *
 * TODO: implement better with a ConnectHelper exception.
 *
 */
public class ConnectHelper {

    // Content types the tile giver should check
    // XXX: Is text/plain neccesary?
    private static final String[] contentTypes = new String[]{"text/html",
        "application/xhtml+xml", "application/xml", "text/xml", "text/plain"};
    // Charset to fall back to if none was found
    private static final String fallbackCharset = "iso-8859-1";


    /**
     * Extracts a charset from a Content-Type.
     *
     * @param  contentType  The Content-Type to extract from
     * @return              The extracted charset. Null if no charset was found.
     *
     */
    private static String getCharset(String contentType) {
        String[] parameters = contentType.split(";");

        for(int i = 1; i < parameters.length; i++) {
            String parameter = parameters[i];
            if(parameter.indexOf('=') != -1) {
                int    split = parameter.indexOf('=');
                String key   = parameter.substring(0, split).trim();
                String value = parameter.substring(split + 1).trim();

                if(key.equalsIgnoreCase("charset")) {
                    // XXX: This should be enough for quoted strings. We should
                    // not have any special chars in our charsets.
                    if(value.startsWith("\"")) {
                      return value.substring(1, value.length() - 1);
                    }                    
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Checks if this is a okay content type
     *
     * TODO: rename function
     *
     * @param  contentType  The Content-Type to check
     * @return              True if okay, false if not.
     */
    private static boolean okContentType(String contentType) {
        for (int i = 0; i < contentTypes.length; i++) {
            if (contentTypes[i].equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }


    /**
     *
     * @param connectionType
     * @param server
     * @param file
     * @param port
     * @param maxContentLength
     * @return
     */
    public static String Connect(String connectionType, String server,
            String file, int port, int maxContentLength,
            String requestMethod, Map<String, String> postData) {
        try {
            // Set up connection to disallow output and allow input. It should follow
            // redirects but dont use a cache.
            URL url = new URL(connectionType, IDN.toASCII(server), port, file);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            
            con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)");

            con.setAllowUserInteraction(false);
            con.setDoInput(true);
            con.setDoOutput(false);
            con.setUseCaches(false);
            con.setInstanceFollowRedirects(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Request method
            if(requestMethod != null) {
                con.setRequestMethod(requestMethod);
            }

            // Write postdata:
            if(postData != null && !postData.isEmpty()) {
                con.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                String postdata = "";
                for(Map.Entry<String,String> entry: postData.entrySet()) {
                    postdata += "&" + URLEncoder.encode(entry.getKey(), "utf-8")
                            + "=" + URLEncoder.encode(entry.getValue(), "utf-8");
                }
                wr.write(postdata.substring(1));
                wr.flush();
                wr.close();
            }
            
            // Read in content type. If we have no content type this most 
            // certainly means that the web page was not loaded due to some
            // reason -- like connection error or a bad server.
            String contentType = con.getContentType();
            if(contentType == null) {
                System.out.println("Could not get the content type");
                return null;
            } else  {
                contentType = contentType.split(";")[0];
            }
            
            // Check for content type. Only accept web pages.
            if (!okContentType(contentType)) {
                System.out.println("Found unrecognized content type: " + contentType);
                return null;
            }
    
            // Get data
            ByteBuffer bb = getConnectionData(con, maxContentLength);
            if(bb == null) {
                System.out.println("Unable to fetch data");
                return null;
            }
    
            // Try to use charset from HTTP header Content-Type:
            try {
                String charset = getCharset(con.getContentType());
                System.out.println("Charset from content-type header: '" + charset + "'");
                if(charset != null)
                    return new String(bb.array(), charset);
            } catch(UnsupportedEncodingException e) {
              // ...
            }
            
            // Try to use charset from Meta tags:
            try {
                String charset = getCharsetFromDocument(bb);
                System.out.println("Charset from document meta tag: '" + charset + "'");
                if(charset != null)
                    return new String(bb.array(), charset);
            } catch(UnsupportedEncodingException e) {
                // ...
            }
            
            // Try to use fallback encoding:
            System.out.println("Using fallback charset: '" + fallbackCharset + "'");
            return new String(bb.array(), fallbackCharset);
      
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
    }

    /**
     * Fetches connection data from an open connection.
     *
     * @param   con               The connection to fetch bytes from
     * @param   maxContentLength  The maxiumum number of bytes to fetch
     *
     * @return  The data from the connect (max maxContentLength bytes)
     */
    private static ByteBuffer getConnectionData(HttpURLConnection con, int maxContentLength) {
        // Byte buffer (from content length):
        int contentLength = con.getContentLength();
        // TODO: why '< 100'?
        if (contentLength > maxContentLength || contentLength < 100) {
            contentLength = maxContentLength;
        }
        ByteBuffer bb = ByteBuffer.allocate(contentLength);

        try {
            while (true) {
                byte[] tmp = new byte[256];
                int cnt = con.getInputStream().read(tmp);
                if (cnt == -1) {
                    break;
                }
                bb.put(tmp, 0, cnt);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        } catch (java.nio.BufferOverflowException e) {
            //e.printStackTrace();
            // TODO: Work around
        }
        return bb;
    }

    /**
     * Get charset from a document.
     *
     * This function searches the document for meta tags and parses them, and
     * searches for a content-type httpequiv with some charset.
     *
     * @param bb  The document to search
     *
     * @return    The charset, if found. Otherwise null.
     *
     */
    private static String getCharsetFromDocument(ByteBuffer bb) {
        String patternMeta = "(?i)<meta\\s([^>]*)>";
        Pattern pm = Pattern.compile(patternMeta);
        Matcher mm;
        try{
            mm = pm.matcher(new String(bb.array(), fallbackCharset));
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }

        while(mm.find()) {

            String patternAttrib = "(?i)([a-z\\-]+)=(\"|')([^\"|']*)(\"|')";
            Pattern pa = Pattern.compile(patternAttrib);
            Matcher ma = pa.matcher(mm.group(1));

            System.out.println(mm.group(1));

            String httpEquiv = null, content = null;
            while(ma.find()) {
                System.out.println(ma.group(1) + ": " + ma.group(3));
                if(ma.group(1).equalsIgnoreCase("http-equiv")) {
                    httpEquiv = ma.group(3);
                } else if(ma.group(1).equalsIgnoreCase("content")) {
                    content  = ma.group(3);
                }
            }

            if(httpEquiv != null && content != null) {
                if(httpEquiv.equalsIgnoreCase("Content-Type")) {
                    System.out.println("Found charset in meta");
                    System.out.println(httpEquiv + ", " + content);
                    return getCharset(content);
                }
            }
        }
        return null;
    }

}

