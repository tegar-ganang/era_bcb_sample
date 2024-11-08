package com.ssg.tools.jsonxml;

import com.ssg.tools.jsonxml.common.tools.CommandLineToolPrototype;
import com.ssg.tools.jsonxml.common.Formats;
import com.ssg.tools.jsonxml.common.StructureProcessingException;
import com.ssg.tools.jsonxml.common.Utilities;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 
 * @author ssg
 */
public class converter extends CommandLineToolPrototype {

    private BigFactory factory = BigFactory.getInstance();

    private Formats formats = new Formats();

    public converter() {
    }

    public void run(String[] args) throws Exception {
        boolean indent = true;
        String format = "JSON";
        Locale locale = Locale.getDefault();
        String path = null;
        String validate = null;
        if (args.length == 0) {
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
                    indent = this.parseBoolean(arg.substring("-indent=".length()));
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
            } else if (arg.startsWith("-validate=")) {
                validate = arg.substring("-validate=".length());
            } else if (arg.startsWith("-")) {
                help();
                System.err.println("  unknown parameter: " + arg + ".");
            } else {
                path = arg;
            }
        }
        if (path == null) {
            help();
            System.err.println("  ERROR: input file name or URL is missing.");
        }
        setFormats(new Formats(locale, null));
        Object obj = null;
        URL url = Utilities.asURL(path);
        URL validateURL = Utilities.asURL(validate);
        setFactory(BigFactory.getInstance(locale, TimeZone.getDefault(), null));
        Formatter formatter = null;
        try {
            formatter = getFactory().getFormatter(format, indent);
        } catch (StructureProcessingException spex) {
            help();
            System.err.println("  ERROR: unknown or unsupported output format [" + format + "]");
            return;
        }
        obj = this.parsePath(url, validateURL);
        Writer writer = new OutputStreamWriter(System.out);
        formatter.format(writer, obj, "");
        writer.close();
    }

    public void help() {
        System.out.println("Conversion tool: reads JSON/XML text and writes to STDOUT re-formatted one.");
        System.out.println(" converter [-format=JSON|XML] [-indent=true|false] [-locale=locale] [-validate=<validation file or URL>] [-help] <input file or URL>");
        System.out.println("   By default reads STDIN and writes to STDOUT in indented JSON format.");
        System.out.println(" Supported formats are " + Arrays.asList(BigFactory.getInstance().getSupportedFormats()));
    }

    public static void main(String[] args) throws Exception {
        converter conv = new converter();
        conv.run(args);
    }
}
