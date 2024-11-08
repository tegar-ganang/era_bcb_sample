package multimanipulators;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import multimanipulators.converters.simple.Converter;
import multimanipulators.converters.simple.Converter_delete;
import multimanipulators.converters.simple.Converter_replaceString;
import multimanipulators.converters.simple.Converter_tagToString;
import multimanipulators.finders.Finder;
import multimanipulators.finders.Finder_HTMLTAG_ALFA;
import multimanipulators.interfaces.Converters;
import multimanipulators.interfaces.Finders;

/**
 * @author Lieven Roegiers
 * @copyright 2010
 * @project javamylibs
 * @from http://javamylibs.googlecode.com/svn/trunk/
**/
public class test4BETA {

    public static void main(String[] args) {
        System.out.println("<<<START>>>");
        try {
            File file = new File("E:/workspace/JAVA_mylibs/presrc/multimanipulators/lp_es_ES_V2.0.xsl");
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
            InputStream is = Channels.newInputStream(channel);
            StreamMultiManipulatorBETA x = new StreamMultiManipulatorBETA(20);
            ArrayList<Finders> y = new ArrayList<Finders>();
            y.add(new Finder("\r\n", new Converter_delete()));
            y.add(new Finder("]", new Converter_delete()));
            y.add(new Finder("#", new Converter_delete()));
            y.add(new Finder_HTMLTAG_ALFA(false, new Converter_replaceString(" -%- ")));
            String encoding = "GB18030";
            File textFile = new File("E:/workspace/JAVA_mylibs/presrc/multimanipulators/lp_es_ES_V2.0.txt");
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(textFile));
            x.findAndReplace(is, y, writer);
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n<<<STOP:TEST>>>");
    }
}
