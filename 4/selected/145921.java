package com.ssg.tools.jsonxml;

import com.ssg.tools.jsonxml.Comparer.COMPARE_STATUS;
import com.ssg.tools.jsonxml.Comparer.ComparatorContext;
import com.ssg.tools.jsonxml.Comparer.ComparatorPair;
import com.ssg.tools.jsonxml.common.tools.CommandLineToolPrototype;
import com.ssg.tools.jsonxml.common.Formats;
import com.ssg.tools.jsonxml.common.Utilities;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author ssg
 */
public class diff extends CommandLineToolPrototype {

    public void help() {
        System.out.println("Comparator tool: reads 2 JSON and or XML texts and writes to STDOUT comparison results.");
        System.out.println(" diff [-format=JSON|XML|Dump] [-indent=true|false] [-locale=locale] [-depth=number] [-delta=true|false] [-help] <1st input file or URL> <2nd input file or URL>");
        System.out.println("   -format - defines output format. Default is JSON.");
        System.out.println("   -indent - sets/clears output indentation. Default is true.");
        System.out.println("   -locale - Name of locale to use when parsing input.");
        System.out.println("   -depth  - Maximal nesint depth when comparing. Default is 100.");
        System.out.println("   -delta  - Shows only mismatching nodes info if true. Default is false.");
        System.out.println("   1st file  - Path or URL for first compared file.  In oputput it corresponds to A.");
        System.out.println("   2nd file  - Path or URL for second compared file. In oputput it corresponds to B.");
    }

    public void run(String[] args) throws Exception {
        boolean indent = true;
        String format = "JSON";
        Locale locale = Locale.getDefault();
        String path1 = null;
        String path2 = null;
        int depth = 100;
        boolean delta = false;
        if (args == null || args.length == 0) {
            help();
            return;
        }
        for (String arg : args) {
            if (arg.startsWith("-h") || arg.startsWith("-H") || arg.startsWith("-?")) {
                help();
                return;
            } else if (arg.startsWith("-format=")) {
                format = arg.substring("-format=".length());
                if (format.equalsIgnoreCase("json")) {
                    format = "JSON";
                } else if (format.equalsIgnoreCase("XML")) {
                    format = "XML";
                } else if (format.equals("Dump")) {
                    format = "Dump";
                } else {
                    help();
                    System.err.println("  wrong parameter: " + arg + ". Error: unknown output format.");
                }
            } else if (arg.startsWith("-indent=")) {
                try {
                    indent = Boolean.parseBoolean(arg.substring("-indent=".length()));
                } catch (Throwable th) {
                    help();
                    System.err.println("  wrong parameter: " + arg + ". Error: " + th);
                }
            } else if (arg.startsWith("-locale=")) {
                try {
                    locale = parseLocale(arg.substring("-locale=".length()));
                } catch (Throwable th) {
                    help();
                    System.err.println("  wrong parameter: " + arg + ". Error: " + th);
                }
            } else if (arg.startsWith("-depth=")) {
                try {
                    depth = Integer.parseInt(arg.substring("-depth=".length()));
                    if (depth <= 0) {
                        throw new NumberFormatException("Cannot set depth to less or equal to 0. Provided value is " + arg);
                    }
                } catch (NumberFormatException nfex) {
                    help();
                    System.err.println("  wrong parameter: " + arg + ". Error: " + nfex);
                }
            } else if (arg.startsWith("-delta=")) {
                try {
                    delta = parseBoolean(arg.substring("-delta=".length()));
                } catch (Throwable th) {
                    help();
                    System.err.println("  wrong parameter: " + arg + ". Error: " + th);
                }
            } else if (arg.startsWith("-")) {
                help();
                System.err.println("  unknown parameter: " + arg + ".");
            } else {
                if (path1 == null) {
                    path1 = arg;
                } else if (path2 == null) {
                    path2 = arg;
                } else {
                    help();
                    System.err.println("  wrong parameter: " + arg + ". Only 2 files (URLs) are accepted. This is 3rd.");
                    System.err.println("    1st file (URL): " + path1);
                    System.err.println("    2nd file (URL): " + path2);
                }
            }
        }
        if (path1 == null || path2 == null) {
            help();
            System.err.println("  ERROR: both input file name(s) or URL(s) are required.");
            System.err.println("    1st file (URL): " + path1);
            System.err.println("    2nd file (URL): " + path2);
            return;
        }
        setFormats(new Formats(locale, null));
        Object obj = null;
        setFactory(BigFactory.getInstance(locale, TimeZone.getDefault(), null));
        Object obj1 = parsePath(Utilities.asURL(path1), null);
        Object obj2 = parsePath(Utilities.asURL(path2), null);
        if (obj1 == null || obj2 == null) {
            System.err.println("  ERROR: either of both input files could not be parsed:");
            System.err.println("    1st file (URL): " + path1 + " parsed=" + (obj1 != null));
            System.err.println("    2nd file (URL): " + path2 + " parsed=" + (obj2 != null));
            return;
        }
        Writer writer = new OutputStreamWriter(System.out);
        Formatter formatter = this.getFactory().getFormatter(format);
        ComparatorContext context = new ComparatorContext();
        Comparer diff = new Comparer();
        ComparatorPair result = diff.compare(context, obj1, obj2, depth);
        if (delta) {
            removeMatching(result);
        }
        formatter.format(writer, result, "");
        writer.close();
    }

    public void removeMatching(ComparatorPair cp) {
        List<ComparatorPair> nested = cp.getNested();
        if (nested == null || nested.isEmpty()) {
            return;
        }
        List<ComparatorPair> toRemove = new ArrayList<ComparatorPair>();
        for (ComparatorPair scp : nested) {
            if (scp.getStatus() == COMPARE_STATUS.match) {
                toRemove.add(scp);
            } else {
                removeMatching(scp);
            }
        }
        for (ComparatorPair rcp : toRemove) {
            nested.remove(rcp);
        }
    }

    public static void main(String[] args) throws Exception {
        diff diff = new diff();
        diff.run(args);
    }
}
