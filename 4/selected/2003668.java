package edu.whitman.halfway.jigs.cmdline;

import edu.whitman.halfway.util.*;
import edu.whitman.halfway.jigs.*;
import org.apache.log4j.*;

public class TextInfoExtractor extends AbstractCmdLine {

    private String fileSep = System.getProperty("file.separator");

    private static Logger log = Logger.getLogger(TextInfoExtractor.class.getName());

    private TextStatusWidget status = new TextStatusWidget();

    static int[] opts = { RECURSE_ALBUM, LOG4J_FILE, HELP };

    public TextInfoExtractor() {
        super(opts);
    }

    public String getAdditionalParseArgs() {
        return "o";
    }

    public static void main(String[] args) {
        (new TextInfoExtractor()).mainDriver(args);
    }

    protected void doMain() {
        boolean owrite = hasOption('o');
        ExtractorThread imageInfo = new ExtractorThread(status, getAlbum(), owrite);
        imageInfo.start();
    }

    protected void specificUsage() {
        System.out.println("Usage: java TextInfoExtractor [options] " + newline + "\t[-o] turns overwriting description fields ON" + newline);
    }
}
