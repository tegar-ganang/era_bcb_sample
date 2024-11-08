package net.sf.myra.datamining.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sf.myra.datamining.ProbabilisticRule;
import net.sf.myra.datamining.Rule;
import net.sf.myra.datamining.Unit;
import net.sf.myra.datamining.UnitAveragingMode;
import net.sf.myra.datamining.data.Dataset;
import net.sf.myra.datamining.data.Label;
import net.sf.myra.datamining.data.Metadata;
import net.sf.myra.datamining.data.NominalAttribute;
import net.sf.myra.datamining.data.Operator;
import net.sf.myra.datamining.data.Term;
import net.sf.myra.datamining.io.Helper;
import net.sf.myra.datamining.measure.PooledPRCurveMeasure;
import net.sf.myra.datamining.model.ProbabilisticRuleList;
import net.sf.myra.datamining.util.RuleParser.Condition;
import net.sf.myra.datamining.util.RuleParser.ConditionType;
import net.sf.myra.framework.Vertex;

/**
 * @author Fernando Esteban Barril Otero
 * @version $Revision: 2332 $ $Date: 2011-01-25 06:08:31 -0500 (Tue, 25 Jan 2011) $
 */
public class EvaluatorHelper {

    private static final int LIMIT = 5;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("usage: " + EvaluatorHelper.class.getName() + " <output> <data set file>");
            System.exit(1);
        }
        Helper helper = Helper.getHelper(args[1]);
        Dataset dataset = helper.read(args[1]);
        ZipFile zip = new ZipFile(new File(args[0]), ZipFile.OPEN_READ);
        Enumeration entries = zip.entries();
        Unit<?>[] performance = new Unit<?>[LIMIT];
        int index = 0;
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().endsWith(".out")) {
                File temp = File.createTempFile("PARSER", ".zip");
                temp.deleteOnExit();
                PrintStream writer = new PrintStream(new FileOutputStream(temp));
                BufferedInputStream reader = new BufferedInputStream(zip.getInputStream(entry));
                byte[] buffer = new byte[4096];
                int read = -1;
                while ((read = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, read);
                }
                writer.close();
                reader.close();
                BufferedReader outfile = new BufferedReader(new FileReader(temp));
                String line = null;
                RuleParser parser = new RuleParser();
                ProbabilisticRuleList list = new ProbabilisticRuleList();
                while ((line = outfile.readLine()) != null) {
                    if (line.startsWith("IF")) {
                        ProbabilisticRule rule = new ProbabilisticRule(dataset.getMetadata());
                        list.add(fill(dataset.getMetadata(), rule, parser.parse(line)));
                    }
                }
                outfile.close();
                PooledPRCurveMeasure measure = new PooledPRCurveMeasure();
                performance[index] = measure.evaluate(dataset, list);
                System.out.println(entry.getName() + ": " + performance[index]);
                index++;
                if (index >= LIMIT) {
                    break;
                }
            }
        }
        System.out.println(UnitAveragingMode.get(Double.class).average(performance));
    }

    private static Rule fill(Metadata metadata, ProbabilisticRule rule, net.sf.myra.datamining.util.RuleParser.Rule other) {
        for (Condition condition : other.getAntecedent()) {
            int index = metadata.getIndex(condition.getAttribute());
            Term term = new Term(condition.getAttribute(), index);
            Vertex<Term> vertex = null;
            if (condition.getType() == ConditionType.EQUAL) {
                NominalAttribute n = (NominalAttribute) metadata.get(index);
                term.setValues(n.getIndex(condition.getValue()));
                term.setDescription(condition.getValue());
                term.setOperator(Operator.EQUAL_TO);
                vertex = new Vertex<Term>(n.toString(condition.getValue()));
                vertex.setInfo(term);
            } else {
                term.setValues(Double.parseDouble(condition.getValue()));
                Operator operator = null;
                switch(condition.getType()) {
                    case GREATER_THAN:
                        operator = Operator.GREATER_THAN;
                        break;
                    case GREATER_THAN_EQUAL_TO:
                        operator = Operator.GREATER_THAN_EQUAL_TO;
                        break;
                    case LESS_THAN:
                        operator = Operator.LESS_THAN;
                        break;
                }
                term.setOperator(operator);
                vertex = new Vertex<Term>(condition.getAttribute());
                vertex.setInfo(term);
            }
            rule.add(vertex);
        }
        String consequent = other.getConsequent();
        consequent = consequent.substring(0, consequent.length() - 1).substring(1);
        StringTokenizer labels = new StringTokenizer(consequent, ",");
        Label prototype = metadata.getLabel();
        double[] probabilities = new double[prototype.length()];
        while (labels.hasMoreElements()) {
            String[] entry = labels.nextToken().split(":");
            for (int i = 0; i < probabilities.length; i++) {
                if (entry[0].equals(prototype.get(i))) {
                    probabilities[i] = Double.parseDouble(entry[1]);
                }
            }
        }
        rule.setProbabiblities(probabilities);
        return rule;
    }
}
