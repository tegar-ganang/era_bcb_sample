package cz.cvut.felk.cig.jcool.benchmark.method.cmaesmetod.reporting;

import cz.cvut.felk.cig.jcool.benchmark.function.*;
import cz.cvut.felk.cig.jcool.benchmark.method.ant.aaca.AACAMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.ant.aco.ACOMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.ant.api.APIMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.ant.caco.CACOMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.ant.daco.DACOMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.cmaesmetod.*;
import cz.cvut.felk.cig.jcool.benchmark.method.cmaesmetod.reporting.solver.StepSolver;
import cz.cvut.felk.cig.jcool.benchmark.method.genetic.de.DifferentialEvolutionMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.genetic.de.PALDifferentialEvolutionMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.genetic.pbil.PBILMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.genetic.sade.SADEMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.gradient.qn.QuasiNewtonMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.gradient.sd.SteepestDescentMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.hgapso.HGAPSOMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.orthogonalsearch.OrthogonalSearchMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.powell.PowellMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.pso.PSOMethod;
import cz.cvut.felk.cig.jcool.benchmark.method.random.RandomMethod;
import cz.cvut.felk.cig.jcool.core.Function;
import cz.cvut.felk.cig.jcool.core.OptimizationMethod;
import cz.cvut.felk.cig.jcool.core.Point;
import cz.cvut.felk.cig.jcool.solver.BaseObjectiveFunction;
import cz.cvut.felk.cig.jcool.solver.BasicSolver;
import cz.cvut.felk.cig.jcool.utils.CentralDifferenceGradient;
import cz.cvut.felk.cig.jcool.utils.CentralDifferenceHessian;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: sulcanto
 * Date: 5/13/11
 * Time: 1:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComparsionReporting {

    Function[] functions;

    OptimizationMethod[] methods;

    String path;

    public static final int MAX_ITERATIONS = 1000;

    BufferedWriter writer;

    public ComparsionReporting(String path) {
        try {
            this.path = path;
            writer = new BufferedWriter(new FileWriter(path + "comparsion" + MAX_ITERATIONS + ".m"));
        } catch (Exception ex) {
            System.out.println("exception occured at FitnessReporting:" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void init() {
        this.methods = new OptimizationMethod[] { new AACAMethod(), new ACOMethod(), new APIMethod(), new CACOMethod(), new cz.cvut.felk.cig.jcool.benchmark.method.cmaes.CMAESMethod(), new DACOMethod(), new DifferentialEvolutionMethod(), new HGAPSOMethod(), new OrthogonalSearchMethod(), new PALDifferentialEvolutionMethod(), new PBILMethod(), new PowellMethod(), new PSOMethod(), new QuasiNewtonMethod(), new RandomMethod(), new SADEMethod(), new SteepestDescentMethod(), new PureCMAESMethod(), new IPOPCMAESMethod(), new SigmaMeanIPOPCMAESMethod() };
        functions = new Function[] { new AckleyFunction(), new BealeFunction(), new BohachevskyFunction(), new BoothFunction(), new BraninFunction(), new ColvilleFunction(), new DixonPriceFunction(), new EasomFunction(), new GoldsteinPriceFunction(), new GriewangkFunction(), new HartmannFunction(), new HimmelblauFunction(), new LangermannFunction(), new LevyFunction(), new Levy3(), new Levy5(), new MatyasFunction(), new MichalewiczFunction(), new PermFunction(), new PowellFunction(), new PowerSumFunction(), new RanaFunction(), new RastriginFunction(), new RosenbrockFunction(), new SchwefelFunction(), new ShubertFunction(), new SphereFunction(), new TridFunction(), new WhitleyFunction(), new ZakharovFunction() };
    }

    public void measure() {
        int maxIterations = 0;
        try {
            init();
            writer.write("clear all;\nclose all;\nclc;\n\n");
            writer.write("functions={");
            for (int f = 0; f < functions.length; f++) {
                String fname = functions[f].getClass().getSimpleName();
                if (fname.contains("Function")) fname = fname.substring(0, fname.length() - 8);
                writer.write("\'" + fname + "\'\n");
            }
            writer.write("}\n");
            writer.write("methods={");
            for (int f = 0; f < methods.length; f++) {
                String methodName = methods[f].getClass().getSimpleName();
                if (methodName.contains("Method")) methodName = methodName.substring(0, methodName.length() - 6);
                writer.write("\'" + methodName + "\'\n");
            }
            writer.write("}\n");
            for (int f = 0; f < functions.length; f++) {
                String fname = functions[f].getClass().getSimpleName();
                if (fname.contains("Function")) fname = fname.substring(0, fname.length() - 8);
                LinkedList<LinkedList<Double>> bestResultsForFunctionOfMethods = new LinkedList<LinkedList<Double>>();
                LinkedList<LinkedList<Double>> worstResultsForFunctionOfMethods = new LinkedList<LinkedList<Double>>();
                for (int m = 0; m < methods.length; m++) {
                    System.out.println(functions[f].getClass().getSimpleName() + "\t" + methods[m].getClass().getSimpleName());
                    StepSolver solver = new StepSolver(MAX_ITERATIONS);
                    solver.init(functions[f], methods[m]);
                    solver.solve();
                    bestResultsForFunctionOfMethods.add(solver.getBestValues());
                    worstResultsForFunctionOfMethods.add(solver.getWorstValues());
                    maxIterations = Math.max(bestResultsForFunctionOfMethods.size(), maxIterations);
                }
                BufferedWriter bestWriter = new BufferedWriter(new FileWriter(path + fname + "Best.csv"));
                for (int m = 0; m < bestResultsForFunctionOfMethods.size(); m++) {
                    for (int i = 0; i < bestResultsForFunctionOfMethods.get(m).size(); i++) {
                        if (Double.isInfinite(bestResultsForFunctionOfMethods.get(m).get(i))) bestWriter.write("" + "-Inf" + ","); else bestWriter.write("" + bestResultsForFunctionOfMethods.get(m).get(i) + ",");
                    }
                    bestWriter.write("\n");
                }
                bestWriter.flush();
                bestWriter.close();
                BufferedWriter worstWriter = new BufferedWriter(new FileWriter(path + fname + "Worst.csv"));
                for (int m = 0; m < bestResultsForFunctionOfMethods.size(); m++) {
                    for (int i = 0; i < bestResultsForFunctionOfMethods.get(m).size(); i++) {
                        if (Double.isInfinite(worstResultsForFunctionOfMethods.get(m).get(i))) worstWriter.write("" + "-Inf" + ","); else worstWriter.write("" + bestResultsForFunctionOfMethods.get(m).get(i) + ",");
                    }
                    worstWriter.write("\n");
                }
                worstWriter.flush();
                worstWriter.close();
            }
            writer.write("\n\ncolors=hsv(size(methods,1));\n");
            writer.write("\n\n");
            for (int f = 0; f < functions.length; f++) {
                String fname = functions[f].getClass().getSimpleName();
                if (fname.contains("Function")) fname = fname.substring(0, fname.length() - 8);
                writer.write(fname + "Best=csvread('" + fname + "Best.csv');\n");
                writer.write(fname + "Worst=csvread('" + fname + "Worst.csv');\n");
            }
            writer.write("\n\n");
            for (int f = 0; f < functions.length; f++) {
                String fname = functions[f].getClass().getSimpleName();
                if (fname.contains("Function")) fname = fname.substring(0, fname.length() - 8);
                for (int m = 0; m < methods.length; m++) {
                    writer.write(fname + "Best(" + (m + 1) + ",find(" + fname + "Best(" + (m + 1) + ",:)==0))=-Inf;\n");
                    writer.write(fname + "Worst(" + (m + 1) + ",find(" + fname + "Worst(" + (m + 1) + ",:)==0))=-Inf;\n");
                }
            }
            writer.write("\n\n");
            for (int f = 0; f < functions.length; f++) {
                String fname = functions[f].getClass().getSimpleName();
                if (fname.contains("Function")) fname = fname.substring(0, fname.length() - 8);
                writer.write("\nfbest=figure('Name','" + fname + "');\n" + "hold on;\n");
                for (int m = 0; m < methods.length; m++) {
                    if (methods[m] instanceof PureCMAESMethod || methods[m] instanceof RestartCMAESMethod) writer.write("plot((" + fname + "Best(" + (m + 1) + ",:)),'LineWidth',2,'Color',colors(" + (m + 1) + ",:))\n"); else writer.write("plot((" + fname + "Best(" + (m + 1) + ",:)),'Color',colors(" + (m + 1) + ",:))\n");
                }
                writer.write("legend(methods);\n");
                writer.write("title('" + fname + "');\n");
                writer.write("xlabel('Generace (g)');\n");
                writer.write("ylabel('Fitness f(x)')\n");
                writer.write("\nprint('figures/" + fname + "Best.eps','-depsc2');\n");
                writer.write("\nfworst=figure('Name','" + fname + "');\n" + "hold on;\n");
                for (int m = 0; m < methods.length; m++) {
                    if (methods[m] instanceof PureCMAESMethod || methods[m] instanceof RestartCMAESMethod) writer.write("plot((" + fname + "Worst(" + (m + 1) + ",:)),'LineWidth',2,'Color',colors(" + (m + 1) + ",:))\n"); else writer.write("plot((" + fname + "Worst(" + (m + 1) + ",:)),'Color',colors(" + (m + 1) + ",:))\n");
                }
                writer.write("legend(methods);\n");
                writer.write("title('" + fname + "');\n");
                writer.write("xlabel('Generace (g)');\n");
                writer.write("ylabel('Fitness f(x)')\n");
                writer.write("\nprint('figures/" + fname + "Worst.eps','-depsc2');\n");
            }
            writer.write("\n\nclose all;\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LinkedList<Double> fillWithTo(LinkedList<Double> list, int to, double with) {
        for (int i = list.size(); i <= to; i++) {
            list.add(with);
        }
        return list;
    }

    public static void main(String[] argch) {
        ComparsionReporting reporting = new ComparsionReporting("/home/sulcanto/Desktop/comparsion/");
        reporting.measure();
        return;
    }
}
