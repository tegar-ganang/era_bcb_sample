package com.anthonyeden.lib.util;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/** A simple tool for parsing files which use the Velocity templating
    language.
    
    @author Anthony Eden
*/
public class VelocityTool {

    private VelocityTool() {
    }

    /** Parse the text file at the given path.
    
        @param path The path
        @return The resulting String
        @throws Exception
    */
    public static String parseTextFile(String path) throws Exception {
        return parseTextFile(new File(path));
    }

    /** Parse the text file at the given path.  Variables are specified as 
        name/value pairs in the contextMap.
        
        @param path The path
        @param contextMap The name/value variable pairs
        @return The resulting String
        @throws Exception
    */
    public static String parseTextFile(String path, Map contextMap) throws Exception {
        return parseTextFile(path, contextMap);
    }

    /** Parse the text file at the given path.
        
        @param file The File
        @return The resulting String
        @throws Exception
    */
    public static String parseTextFile(File file) throws Exception {
        return parseTextFile(file, Collections.EMPTY_MAP);
    }

    /** Parse the text file.  Variables are specified as name/value pairs in 
        the contextMap.
        
        @param file The File
        @param contextMap The name/value variable pairs
        @return The resulting String
        @throws Exception
    */
    public static String parseTextFile(File file, Map contextMap) throws Exception {
        StringWriter writer = new StringWriter();
        FileReader reader = new FileReader(file);
        parse(reader, writer, contextMap);
        return writer.toString();
    }

    /** Parse the InputStream.
        
        @param in The InputStream
        @return The resulting String
        @throws Exception
    */
    public static String parseInputStream(InputStream in) throws Exception {
        return parseInputStream(in, Collections.EMPTY_MAP);
    }

    /** Parse the text file at the given path.  Variables are specified as 
        name/value pairs in the contextMap.
        
        @param in The input stream
        @param contextMap The name/value variable pairs
        @return The resulting String
        @throws Exception
    */
    public static String parseInputStream(InputStream in, Map contextMap) throws Exception {
        StringWriter writer = new StringWriter();
        InputStreamReader reader = new InputStreamReader(in);
        parse(reader, writer, contextMap);
        return writer.toString();
    }

    /** Pass all data from the given Reader through Velocity and write the 
        resulting data to the given Writer.
        
        @param reader The reader
        @param writer The writer
        @param contextMap The context map
        @throws Exception
    */
    public static void parse(Reader reader, Writer writer, Map contextMap) throws Exception {
        VelocityContext customContext = new VelocityContext();
        Iterator keys = contextMap.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            customContext.put(key.toString(), contextMap.get(key));
        }
        Velocity.evaluate(customContext, writer, VelocityTool.class.getName(), reader);
    }
}
