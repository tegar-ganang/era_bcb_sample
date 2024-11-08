package org.edits.cmd;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.edits.CommandExecutor;
import org.edits.Edits;
import org.edits.FileTools;
import org.edits.distance.cost.DefaultWeightCalculator;
import org.edits.etaf.ConllLine;
import org.edits.etaf.ConllText;
import org.edits.etaf.EntailmentPair;
import org.edits.processor.EDITSIterator;
import org.edits.processor.FilesEPSource;

/**
 * 
 * @author Milen Kouylekov
 * 
 */
public class IDFCommand extends CommandExecutor {

    public static void calculateIDF(EDITSIterator<EntailmentPair> pairs, String filename, boolean makeIndex) throws Exception {
        double count = 0;
        Map<String, Double> table = new HashMap<String, Double>();
        while (pairs.hasNext()) {
            EntailmentPair p = pairs.next();
            for (ConllText t : p.getT()) {
                Set<String> seen = new HashSet<String>();
                for (ConllLine a : t.annotations()) {
                    String form = a.form().toLowerCase();
                    if (form == null || seen.contains(form)) continue;
                    seen.add(form);
                    table.put(form, table.containsKey(form) ? table.get(form) + 1.0 : 1.0);
                }
                count++;
            }
        }
        if (makeIndex) {
            Directory d = FSDirectory.open(new File(Edits.path() + "/share/indexes/idf/" + filename));
            DefaultWeightCalculator.makeIndex(d, table, true, count);
            return;
        }
        StringBuilder b = new StringBuilder();
        for (String key : table.keySet()) b.append(key + "\t" + Math.log(count / table.get(key)) + "\n");
        FileTools.saveString(filename, b.toString().trim(), true);
    }

    public static void main(String[] args) {
        try {
            mainEX(args);
        } catch (Exception e) {
            Logger logger = Logger.getLogger("edits.main");
            logger.debug(e.getMessage(), e);
            System.out.println(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void mainEX(String[] args) throws Exception {
        CommandExecutor exex = new CommandExecutor();
        new Edits(exex);
        Options oxsx = exex.defaultOptions();
        Options oxs = new Options();
        for (Object x : oxsx.getOptions()) {
            Option xx = (Option) x;
            if (xx.getLongOpt().equals("configuration")) continue;
            if (xx.getLongOpt().equals("model")) continue;
            if (xx.getLongOpt().equals("output")) {
                xx.setDescription("Extracted idfs");
                xx.setRequired(true);
            }
            oxs.addOption(xx);
        }
        Option o = new Option("mi", "make_index", false, "Make an idf index");
        oxs.addOption(o);
        if (args.length == 0) {
            Writer result = new StringWriter();
            PrintWriter printWriter = new PrintWriter(result);
            HelpFormatter formatter = new HelpFormatter();
            printWriter.println("IDF Calculator Edits Script");
            printWriter.println("EDITS - Edit Distance Textual Entailment Suite - " + Edits.VERSION);
            formatter.printUsage(printWriter, HelpFormatter.DEFAULT_WIDTH, "edits-idf");
            formatter.printOptions(printWriter, 120, oxs, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD);
            System.out.println(result.toString());
            return;
        }
        CommandLine line = null;
        try {
            line = new BasicParser().parse(oxs, args);
        } catch (Exception e) {
            System.out.println(e.getMessage() + "\n");
            return;
        }
        Edits.setVerbose(line.hasOption("verbose"));
        String output = line.getOptionValue("output");
        boolean overwrite = line.hasOption("force");
        if (output != null && !overwrite) throw new Exception("Output already exists");
        List<String> files = FileTools.inputFiles(line.getArgList());
        EDITSIterator<EntailmentPair> all = null;
        if (!line.hasOption("pipe")) all = FilesEPSource.loadFromShell(files); else all = FilesEPSource.initFromShell(files);
        calculateIDF(all, line.getOptionValue("output"), line.hasOption("make_index"));
    }
}
