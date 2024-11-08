package edu.cmu.minorthird.text;

import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.text.CharAnnotation;
import montylingua.JMontyLingua;
import org.apache.log4j.Logger;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Adds part of speech tags to a TextLabels. 
 *
 * @author Richard Wang rcwang@cmu.edu
 */
public class POSTagger extends StringAnnotator {

    private static Logger log = Logger.getLogger(POSTagger.class);

    private static JMontyLingua montyLingua = new JMontyLingua();

    public POSTagger() {
        providedAnnotation = "pos";
    }

    /**
   * Returns char based stand-off annotations for pos in the given string
   *
   * This will not work with html/xml in the string!
   *
   * @param in String to tag
   * @return tagged String
   */
    protected CharAnnotation[] annotateString(String in) {
        String tagged = montyTag(in);
        String strToken = null;
        String pos = null;
        String word = null;
        int sep = 0;
        StringTokenizer tokeTagged = new StringTokenizer(tagged, "\n ", false);
        log.debug("\n" + in);
        List list = new ArrayList();
        int curLocation = 0;
        while (tokeTagged.hasMoreTokens()) {
            strToken = tokeTagged.nextToken();
            sep = strToken.lastIndexOf("/");
            word = strToken.substring(0, sep);
            curLocation = in.indexOf(word, curLocation);
            pos = strToken.substring(sep + 1);
            if (pos.endsWith("$")) pos = pos.replace('$', 'S');
            CharAnnotation ca = new CharAnnotation(curLocation, word.length(), pos);
            list.add(ca);
            log.debug("tag: " + strToken + " with " + ca);
            curLocation += word.length();
        }
        return (CharAnnotation[]) list.toArray(new CharAnnotation[0]);
    }

    private static void writeFile(File out, String content) {
        log.debug("Writing " + out);
        try {
            BufferedWriter bWriter = new BufferedWriter(new FileWriter(out));
            bWriter.write(content);
            bWriter.close();
        } catch (Exception ioe) {
            log.error("Error writing to " + out + ": " + ioe);
        }
    }

    public static String substFirst(String in, String find, String newStr, boolean case_sensitive) {
        char[] working = in.toCharArray();
        StringBuffer sb = new StringBuffer();
        int startindex = 0;
        if (case_sensitive) startindex = in.indexOf(find); else startindex = (in.toLowerCase()).indexOf(find.toLowerCase());
        if (startindex < 0) return in;
        int currindex = 0;
        for (int i = currindex; i < startindex; i++) sb.append(working[i]);
        currindex = startindex;
        sb.append(newStr);
        currindex += find.length();
        for (int i = currindex; i < working.length; i++) sb.append(working[i]);
        return sb.toString();
    }

    private static String montyTag(String string) {
        string = string.replaceAll("<[^>]+>", "");
        return montyLingua.tag_text(string);
    }

    public static String POSTag(String in) {
        String tagged = montyTag(in);
        String strToken = null;
        String pos = null;
        String word = null;
        StringBuffer XMLTagged = new StringBuffer("");
        int sep = 0;
        int endPointer = 0;
        StringTokenizer tokeTagged = new StringTokenizer(tagged, "\n ", false);
        String workingString = new String(in);
        while (tokeTagged.hasMoreTokens()) {
            strToken = tokeTagged.nextToken();
            sep = strToken.lastIndexOf("/");
            word = strToken.substring(0, sep);
            pos = strToken.substring(sep + 1);
            if (pos.endsWith("$")) pos = pos.replace('$', 'S');
            workingString = substFirst(workingString, word, "<" + pos + ">" + word + "</" + pos + ">", false);
            endPointer = workingString.lastIndexOf("</" + pos + ">") + ("</" + pos + ">").length();
            XMLTagged.append(workingString.substring(0, endPointer));
            workingString = workingString.substring(endPointer);
        }
        return XMLTagged.toString();
    }

    public String explainAnnotation(edu.cmu.minorthird.text.TextLabels labels, edu.cmu.minorthird.text.Span documentSpan) {
        return "no idea";
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println(montyLingua.tag_text("hello"));
            System.out.println(montyLingua.tag_text("world"));
            log.info("Usage:\t1. java POSTagger [input_file] [output_file]\n\t2. java POSTagger [input_dir]  [output_dir]\n\t3. java POSTagger [input_file] [output_dir]");
            return;
        }
        File inFile = new File(args[0]);
        File outFile = new File(args[1]);
        if (!inFile.exists()) {
            log.fatal("Error: File " + inFile + " could not be found!");
            return;
        }
        if (inFile.isFile()) {
            if (outFile.isDirectory()) outFile = new File(outFile.getPath() + File.separator + inFile.getName());
            writeFile(new File(outFile.getName() + ".l"), POSTag(IOUtil.readFile(inFile)));
        } else if (inFile.isDirectory()) {
            if (!outFile.exists()) outFile.mkdir();
            File[] fileList = inFile.listFiles();
            for (int i = 0; i < fileList.length; i++) if (fileList[i].isFile()) {
                File outTo = new File(outFile.getPath() + File.separator + fileList[i].getName());
                log.debug("tagging " + fileList[i]);
                writeFile(outTo, POSTag(IOUtil.readFile(fileList[i])));
            }
        }
    }
}
