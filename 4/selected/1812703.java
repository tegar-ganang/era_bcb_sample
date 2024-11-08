package org.edits.annotation;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.edits.CommandExecutor;
import org.edits.Edits;
import org.edits.FileLoader;
import org.edits.etaf.AnnotatedText;
import org.edits.etaf.Annotation;
import org.edits.etaf.EntailmentPair;
import org.edits.processor.EDITSIterator;
import org.edits.processor.EDITSListIterator;
import org.edits.processor.EntailmentPairSource;
import org.edits.processor.FileEPTarget;
import org.edits.processor.Target;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * 
 * @author Milen Kouylekov
 * 
 */
public class StanfordParser extends TextAnnotator {

    private LexicalizedParser parser;

    public StanfordParser(String path) {
        logger().debug("Loading Stanford Parser Model " + path);
        parser = new LexicalizedParser(path);
        parser.setOptionFlags(new String[] { "-maxLength", "250", "-retainTmpSubcategories" });
    }

    public void parse(AnnotatedText a) {
        try {
            Tree parse = parser.apply(a.getContent());
            TreebankLanguagePack tlp = new PennTreebankLanguagePack();
            GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
            GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
            Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();
            convert(tdl, a);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(List<AnnotatedText> list) throws Exception {
        for (AnnotatedText a : list) {
            a.annotations().clear();
            parse(a);
        }
    }

    public static void convert(Collection<TypedDependency> data, AnnotatedText a) {
        Set<String> roots = new HashSet<String>();
        Set<String> deps = new HashSet<String>();
        for (TypedDependency t : data) {
            String from = "" + t.gov().index();
            String to = "" + t.dep().index();
            deps.add(to);
            if (roots.contains(to)) roots.remove(to);
            if (!deps.contains(from)) roots.add(from);
            String name = t.reln().getSpecific() == null ? t.reln().getShortName() : t.reln().getShortName() + "_" + t.reln().getSpecific();
            if (a.get(to) == null) {
                Annotation x = new Annotation();
                x.set(Annotation.ID, "" + to);
                t.dep().label().lemmatize(false);
                x.set(Annotation.FORM, t.dep().label().value());
                if (t.dep().label().lemma() == null) x.set(Annotation.LEMMA, x.form()); else x.set(Annotation.LEMMA, t.dep().label().lemma());
                String pos = t.dep().label().tag();
                x.set(Annotation.CPOSTAG, pos);
                a.annotations().add(x);
                if (pos != null) {
                    if (pos.startsWith("N")) x.set(Annotation.POSTAG, "n");
                    if (pos.startsWith("V")) x.set(Annotation.POSTAG, "v");
                    if (pos.startsWith("JJ")) x.set(Annotation.POSTAG, "a");
                    if (pos.contains("RB")) x.set(Annotation.POSTAG, "r");
                }
                x.set(Annotation.DEPREL, name);
                x.set(Annotation.HEAD, from);
            }
            if (a.get(from) == null) {
                Annotation x = new Annotation();
                x.set(Annotation.ID, from);
                t.dep().label().lemmatize(false);
                x.set(Annotation.FORM, t.gov().label().value());
                if (t.gov().label().lemma() == null) x.set(Annotation.LEMMA, x.form()); else x.set(Annotation.LEMMA, t.gov().label().lemma());
                String pos = t.gov().label().tag();
                a.annotations().add(x);
                if (pos != null) {
                    if (pos.startsWith("N")) x.set(Annotation.POSTAG, "n");
                    if (pos.startsWith("V")) x.set(Annotation.POSTAG, "v");
                    if (pos.startsWith("JJ")) x.set(Annotation.POSTAG, "a");
                    if (pos.contains("RB")) x.set(Annotation.POSTAG, "r");
                }
            }
        }
        if (roots.size() == 1) {
            a.get(roots.iterator().next()).feats().add("+root");
        } else {
            if (roots.size() == 0) {
                String idg = selectNode(a);
                if (idg == null) {
                    a.annotations().clear();
                    return;
                }
                a.get(idg).feats().add("+root");
                return;
            }
            Annotation n = new Annotation();
            n.set(Annotation.ID, "aroot");
            n.set(Annotation.FORM, "{root}");
            n.feats().add("+root");
            for (String r : roots) {
                a.get(r).set(Annotation.HEAD, "arrot");
                a.get(r).set(Annotation.DEPREL, "_");
            }
            a.annotations().add(n);
        }
    }

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
        Options xosx = CommandExecutor.defaultOptions();
        Options oxs = new Options();
        for (Object x : xosx.getOptions()) {
            Option xx = (Option) x;
            if (xx.getLongOpt().equals("configuration")) {
                continue;
            }
            if (xx.getLongOpt().equals("model")) {
                xx.setDescription("Stanford model path");
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
            printWriter.println("Stanford Parser Edits Script");
            printWriter.println("EDITS - Edit Distance Textual Entailment Suite - " + Edits.VERSION);
            formatter.printUsage(printWriter, HelpFormatter.DEFAULT_WIDTH, "edits-stanford");
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
        new Edits();
        String output = line.getOptionValue("output");
        boolean overwrite = line.hasOption("force");
        if (output != null && !overwrite && new File(output).exists()) throw new Exception("Output already exists");
        Edits.setVerbose(line.hasOption("verbose"));
        String path = line.getOptionValue("model");
        TextAnnotator annotator = new StanfordParser(path);
        EDITSIterator<EntailmentPair> source = null;
        Target<EntailmentPair> target = new FileEPTarget(line.getOptionValue("output"), true);
        if (!line.hasOption("pipe")) source = new EDITSListIterator<EntailmentPair>(FileLoader.loadEntailmentCorpus(line.getArgs()[0]).getPair()); else source = new EntailmentPairSource(line.getArgs()[0]);
        annotator.annotate(source, target);
        target.close();
    }

    public static String selectNode(AnnotatedText g) {
        String node = null;
        double score = -1000;
        for (Annotation n : g.annotations()) {
            double sc = tree(n, g, new HashSet<String>()).size();
            if (sc > score) {
                score = sc;
                node = n.id();
            }
        }
        return node;
    }

    public static List<Annotation> tree(Annotation node, AnnotatedText g, Set<String> in) {
        List<Annotation> all = new ArrayList<Annotation>();
        all.add(node);
        in.add(node.id());
        for (Annotation e : node.children(g)) {
            if (in.contains(e.id())) continue;
            all.addAll(tree(e, g, in));
        }
        return all;
    }
}
