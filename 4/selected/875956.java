package org.edits.treetagger;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.annolab.tt4j.TokenHandler;
import org.annolab.tt4j.TreeTaggerWrapper;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.edits.CommandExecutor;
import org.edits.Edits;
import org.edits.annotation.TextAnnotator;
import org.edits.etaf.ConllLine;
import org.edits.etaf.ConllText;
import org.edits.etaf.EntailmentCorpus;
import org.edits.etaf.EntailmentPair;
import org.edits.etaf.ObjectFactory;
import org.edits.processor.EDITSIterator;
import org.edits.processor.EDITSListIterator;
import org.edits.processor.EntailmentPairSource;
import org.edits.processor.FileEPTarget;
import org.edits.processor.Target;

public class TreeTagger extends TextAnnotator {

    public static void main(String[] args) {
        try {
            mainEX(args);
        } catch (Exception e) {
            Logger logger = Logger.getLogger("edits.main");
            logger.debug(e.getMessage(), e);
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void mainEX(String[] args) throws Exception {
        CommandExecutor exec = new CommandExecutor();
        Options xosx = exec.defaultOptions();
        Options oxs = new Options();
        for (Object x : xosx.getOptions()) {
            Option xx = (Option) x;
            if (xx.getLongOpt().equals("configuration")) {
                continue;
            }
            if (xx.getLongOpt().equals("model")) {
                xx.setDescription("Tree Tagger path");
                xx.setRequired(true);
            }
            if (xx.getLongOpt().equals("output")) {
                xx.setDescription("Annotated dataset path");
                xx.setRequired(true);
            }
            oxs.addOption(xx);
        }
        if (args.length == 0) {
            Writer result = new StringWriter();
            PrintWriter printWriter = new PrintWriter(result);
            HelpFormatter formatter = new HelpFormatter();
            printWriter.println("Tree Taggger Edits Script");
            printWriter.println("EDITS - Edit Distance Textual Entailment Suite - " + Edits.VERSION);
            formatter.printUsage(printWriter, HelpFormatter.DEFAULT_WIDTH, "edits-treetagger");
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
        new Edits(exec);
        String output = line.getOptionValue("output");
        boolean overwrite = line.hasOption("force");
        if (output != null && !overwrite && new File(output).exists()) throw new Exception("Output already exists");
        Edits.setVerbose(line.hasOption("verbose"));
        String path = line.getOptionValue("model");
        TextAnnotator annotator = new TreeTagger(path);
        EDITSIterator<EntailmentPair> source = null;
        Target<EntailmentPair> target = new FileEPTarget(line.getOptionValue("output"), true);
        if (!line.hasOption("pipe")) source = new EDITSListIterator<EntailmentPair>(((EntailmentCorpus) new ObjectFactory().load(line.getArgs()[0])).getPair()); else source = new EntailmentPairSource(line.getArgs()[0]);
        annotator.annotate(source, target);
        target.close();
    }

    public static String selectNode(ConllText g) {
        String node = null;
        double score = -1000;
        for (ConllLine n : g.annotations()) {
            double sc = tree(n, g, new HashSet<String>()).size();
            if (sc > score) {
                score = sc;
                node = n.id();
            }
        }
        return node;
    }

    public static List<ConllLine> tree(ConllLine node, ConllText g, Set<String> in) {
        List<ConllLine> all = new ArrayList<ConllLine>();
        all.add(node);
        in.add(node.id());
        for (ConllLine e : node.children(g)) {
            if (in.contains(e.id())) continue;
            all.addAll(tree(e, g, in));
        }
        return all;
    }

    private String path;

    public TreeTagger(String path_) {
        path = path_;
        System.setProperty("treetagger.home", path);
    }

    @Override
    public void run(List<ConllText> list) throws Exception {
        TreeTaggerWrapper<String> tt = new TreeTaggerWrapper<String>();
        tt.setModel(path + "models/english.par:iso8859-1");
        if (language().equals("it")) tt.setModel(path + "models/italian.par:iso8859-1");
        for (ConllText a : list) {
            final Iterator<ConllLine> iterator = a.annotations().iterator();
            List<String> s = new ArrayList<String>();
            for (ConllLine x : a.annotations()) s.add(x.form());
            tt.setHandler(new TokenHandler<String>() {

                @Override
                public void token(String token, String pos, String lemma) {
                    ConllLine a = iterator.next();
                    a.set(ConllLine.LEMMA, lemma);
                    a.set(ConllLine.POSTAG, pos);
                    if (pos.startsWith("N")) a.set(ConllLine.CPOSTAG, "noun");
                    if (pos.startsWith("V")) a.set(ConllLine.CPOSTAG, "vern");
                }
            });
            tt.process(s);
            while (iterator.hasNext()) continue;
        }
    }
}
