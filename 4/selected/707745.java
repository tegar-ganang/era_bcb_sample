package jeco.lib.problems.parallelizelJava;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jeco.kernel.algorithm.moga.NSGAII;
import jeco.kernel.operator.crossover.SinglePointCrossover;
import jeco.kernel.operator.mutator.BooleanMutation;
import jeco.kernel.operator.selector.BinaryTournamentNSGAII;
import jeco.kernel.problem.Problem;
import jeco.kernel.problem.Solution;
import jeco.kernel.problem.Solutions;
import jeco.kernel.problem.Variable;

/**
 * Main genetic algorithm to obtain parallel Java code starting from a
 * profile and a source serial code.
 * 
 * @author J. Manuel Colmenar
 */
public class ParallelizeJava extends Problem<Variable<Boolean>> {

    /** Folder containing the input java files */
    public String inputFolder;

    /** Folder containing the folders for all individuals */
    public String outputFolder;

    /** Methods to parallelize, which are read from a profile text file */
    public List<TargetMethod> methodsToParallelize;

    /** Main class of the target code */
    public String mainClass;

    public final int EXECUTION_TIME = 0;

    public static String PARALLEL_CLASS_SUFFIX = "Parallel";

    public static String PARALLEL_CLASSES_FOLDER = "test_java" + File.separatorChar + "parallel_classes";

    public static String THREAD_MANAGER_CLASS = "ThreadManager";

    public static String THREAD_MANAGER_FILE = "test_java" + File.separatorChar + "utils" + File.separatorChar + THREAD_MANAGER_CLASS + ".java";

    /** Already evaluated individuals. The set contains the output folder
    * containing the parallel code (which identifies the individual) and
    * the objective value. **/
    private static HashMap<String, Double> alreadyEvaluated;

    /** For debugging purposes, hardcode it when production **/
    private final boolean debug = true;

    public ParallelizeJava(String mainClass, String inputFolder, int numberOfVariables, List<TargetMethod> methodsToParallelize) {
        super("ParallelizeJava", numberOfVariables, 1);
        this.mainClass = mainClass;
        this.inputFolder = inputFolder;
        this.outputFolder = inputFolder + "_out";
        File f = new File(this.outputFolder);
        if (!f.exists()) f.mkdir();
        this.methodsToParallelize = methodsToParallelize;
        alreadyEvaluated = new HashMap<String, Double>();
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Parameters: <mainClass> <inputFolder> <csv profile> <population> <generations>");
            args = new String[5];
            args[0] = "commandline";
            args[1] = "test_java" + File.separatorChar + "code_scimark2";
            args[2] = "test_java" + File.separatorChar + "scimark2_profile4.csv";
            args[3] = "10";
            args[4] = "5";
            return;
        }
        double time = System.currentTimeMillis();
        String mainClass = args[0];
        String inputFolder = args[1];
        String profile = args[2];
        Integer population = Integer.valueOf(args[3]);
        Integer generations = Integer.valueOf(args[4]);
        int numberOfVariables = 0;
        List<TargetMethod> methods = readProfile(profile, inputFolder);
        numberOfVariables = methods.size();
        if (numberOfVariables == 0) {
            System.out.println("Empty profile. Exiting ... \n");
            return;
        }
        ParallelizeJava pj = new ParallelizeJava(mainClass, inputFolder, numberOfVariables, methods);
        NSGAII<Variable<Boolean>> nsga2 = new NSGAII<Variable<Boolean>>(pj, population, generations, new BooleanMutation<Variable<Boolean>>(1.0 / numberOfVariables), new SinglePointCrossover<Variable<Boolean>>(), new BinaryTournamentNSGAII<Variable<Boolean>>());
        nsga2.initialize();
        Solutions<Variable<Boolean>> solutions = nsga2.execute();
        Logger.getLogger(ParallelizeJava.class.getName()).info("solutions.size()=" + solutions.size());
        Solution<Variable<Boolean>> solution = solutions.get(0);
        Logger.getLogger(ParallelizeJava.class.getName()).info("\nSolution folder: " + pj.generateOutputFolder(solution) + " - Execution time: " + solution.getObjectives().get(pj.EXECUTION_TIME) + "\n");
        time = ((System.currentTimeMillis() - time) / 1000);
        Logger.getLogger(ParallelizeJava.class.getName()).info("\nTotal execution time:" + time + " segs\n");
    }

    /**
     * Given the initial data of a method (class, name and attributes), this
     * method looks for the serial code of the method. Therefore, the
     * parallelTargetClassSourceCode attribute of the input parameter will be
     * filled in.
     *
     * @param method, souce code inputFolder
     */
    private static void obtainSourceCode(TargetMethod method, String inputFolder) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(inputFolder + File.separator + method.getSourceFileName())));
            String line = null;
            boolean foundHeader = false;
            int pairingBrackets = 0;
            while (((line = reader.readLine()) != null) && !foundHeader) {
                if (line.contains(method.getName()) && line.contains("(") && !line.contains(";")) {
                    String[] auxStr = line.trim().split("\\(");
                    String[] leftSide = auxStr[0].split(" ");
                    String[] rightSide = auxStr[1].split("\\,");
                    boolean ok = true;
                    if ((rightSide != null) && (!rightSide[0].startsWith(")")) && rightSide.length == method.getAttributeClassList().size()) {
                        int i2 = 0;
                        while ((i2 < rightSide.length) && ok) {
                            String attrClass = rightSide[i2].trim().split(" ")[0];
                            String attrName = rightSide[i2].trim().split(" ")[1];
                            String attrClassOrig = method.getAttributeClassList().get(i2);
                            if (attrClassOrig.contains("[")) attrClassOrig = attrClassOrig.split("\\[")[0];
                            if (!attrClass.equals(attrClassOrig)) {
                                ok = false;
                                method.setAttributeNameList(new ArrayList<String>());
                            } else {
                                if (attrName.contains(")")) {
                                    attrName = attrName.substring(0, attrName.indexOf(")"));
                                }
                                if (attrName.contains("[")) {
                                    attrName = attrName.substring(0, attrName.indexOf("["));
                                }
                                method.getAttributeNameList().add(attrName);
                            }
                            i2++;
                        }
                    } else {
                        if (method.getAttributeClassList().isEmpty() && (rightSide[0].startsWith(")"))) ok = true; else ok = false;
                    }
                    if (ok) {
                        String returnClass = leftSide[leftSide.length - 2];
                        method.setReturnClass(returnClass);
                        method.setMethodPrototype(line.trim());
                        foundHeader = true;
                        if (line.contains("{")) pairingBrackets++;
                    }
                }
            }
            if (foundHeader) {
                List<String> sourceCode = new ArrayList<String>();
                boolean getCode = true;
                while ((line != null) && getCode && !((pairingBrackets == 1) && (line.trim().startsWith("}")))) {
                    if (!((pairingBrackets == 0) && (line.trim().startsWith("{")))) sourceCode.add(line);
                    if (line.contains("{") || line.contains("}")) {
                        for (int i = 0; i < line.length(); i++) {
                            if (line.charAt(i) == '{') pairingBrackets++; else if (line.charAt(i) == '}') pairingBrackets--;
                        }
                    }
                    if (pairingBrackets == 0) getCode = false;
                    line = reader.readLine();
                }
                method.setMethodSourceCode(sourceCode);
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error obtaining source code (" + method.getName() + "): " + ex.getLocalizedMessage(), ex);
        } catch (IOException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error obtaining source code (" + method.getName() + "): " + ex.getLocalizedMessage(), ex);
        }
    }

    /**
     * Searches all around the input folder source files to get the places
     * where the method is called.
     *
     * @param method
     * @param inputFolder
     */
    private static void findMethodCalls(TargetMethod method, String inputFolder) {
        File directory = new File(inputFolder);
        File[] files = directory.listFiles();
        for (int index = 0; index < files.length; index++) {
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(new File(inputFolder + File.separator + files[index].getName())));
                String line = null;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.contains(method.getName() + "(") && line.contains(";") && !line.contains("abstract")) {
                        String[] auxStr = line.trim().split(method.getName());
                        int numArgs = 0;
                        if (auxStr[1] != null) {
                            int i = 1;
                            int paired = 1;
                            while ((paired != 0) && (i < auxStr[1].length() - 1)) {
                                if (auxStr[1].substring(i, i + 1).equals("(")) paired++;
                                if (auxStr[1].substring(i, i + 1).equals(")")) paired--;
                                i++;
                            }
                            String auxStr2 = auxStr[1].substring(1, i - 1);
                            auxStr = auxStr2.split("\\,");
                            if (auxStr.length == 1 && auxStr[0].equals("")) numArgs = 0; else numArgs = auxStr.length;
                        }
                        if (numArgs == method.getAttributeClassList().size()) {
                            method.getCallsList().add(new MethodCallInfo(files[index].getName(), lineNumber, line));
                        }
                    }
                }
                reader.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error finding method calls (" + method.getName() + "): " + ex.getLocalizedMessage(), ex);
            } catch (IOException ex) {
                Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error finding method calls (" + method.getName() + "): " + ex.getLocalizedMessage(), ex);
            }
        }
    }

    /**
     * Given all data of a method (class, name, attributes and source code), this
     * method generates the parallel code depending on the method prototype.
     *
     * It "clones" the original class, also cloning the target method with
     * the new "call" one.
     *
     * @param method
     */
    private static void generateCompleteParallelClass(TargetMethod method, String inputFolder) {
        File srcFile = new File(inputFolder + File.separator + method.getSourceFileName());
        BufferedReader reader;
        List<String> newCode = new ArrayList<String>();
        try {
            reader = new BufferedReader(new FileReader(srcFile));
            String line = null;
            while (((line = reader.readLine()) != null) && !line.contains("public class")) {
                newCode.add(line);
            }
            if (!line.contains("{")) do {
                line = reader.readLine();
            } while ((line != null) && !line.contains("{"));
            newCode.add("import java.util.concurrent.Callable;");
            String classHeader = "public class " + method.getParallelName() + " implements Callable {";
            newCode.add("\n" + classHeader + "\n");
            for (int i = 0; i < method.getAttributeClassList().size(); i++) {
                String attrClassOrig = method.getAttributeClassList().get(i);
                String arrayBrackets = "";
                if (attrClassOrig.contains("[")) {
                    arrayBrackets = attrClassOrig.substring(attrClassOrig.indexOf("["));
                    attrClassOrig = attrClassOrig.split("\\[")[0];
                }
                newCode.add("public " + attrClassOrig + " " + method.getAttributeNameList().get(i) + arrayBrackets + ";");
            }
            newCode.add("\npublic Object call() throws Exception {");
            for (String s : method.getMethodSourceCode()) {
                newCode.add(s);
            }
            if (method.getReturnClass().equals("void")) {
                newCode.add("return 1;");
            }
            newCode.add("}\n");
            while ((line = reader.readLine()) != null) {
                newCode.add(line);
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error generating parallel class (" + method.getParallelName() + "): " + ex.getLocalizedMessage(), ex);
        } catch (IOException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error generating parallel class (" + method.getParallelName() + "): " + ex.getLocalizedMessage(), ex);
        }
        String containerClass = method.getSourceFileName().split("\\.")[0];
        int pairedBrackets = 0;
        boolean foundHeader = false;
        for (int i = 0; i < newCode.size(); i++) {
            if (newCode.get(i).contains(containerClass)) {
                String aux[] = newCode.get(i).trim().split(containerClass);
                if (aux.length >= 2) {
                    if (aux[0].trim().equals("public") && aux[1].trim().startsWith("(")) {
                        foundHeader = true;
                        newCode.set(i, "/* " + newCode.get(i));
                    }
                }
            }
            if (foundHeader) {
                if (newCode.get(i).contains("{")) pairedBrackets++;
                if (newCode.get(i).contains("}")) {
                    pairedBrackets--;
                    if (pairedBrackets == 0) {
                        newCode.set(i, newCode.get(i) + "*/");
                        foundHeader = false;
                    }
                }
            }
        }
        String destFile = PARALLEL_CLASSES_FOLDER + File.separator + method.getParallelFileName();
        writeToFile(newCode, destFile);
    }

    /**
     * Reads a file containing the hotspots of the profiled Java code.
     * Each line must contain just one method, fully qualified.
     *
     * The profile must be in CSV format as exported from Netbeans
     *
     * @param profile text file
     * @return list of methods fully characterized and containing the
     * parallel class representing them, which depends on its returning
     * classes.
     */
    private static List<TargetMethod> readProfile(String profile, String inputFolder) {
        List<TargetMethod> methods = new ArrayList<TargetMethod>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(new File(profile)));
            String line = null;
            boolean skipFirstLine = true;
            while (((line = reader.readLine()) != null) && !line.isEmpty()) {
                if (skipFirstLine) {
                    skipFirstLine = false;
                } else {
                    if (!line.trim().startsWith("#")) {
                        TargetMethod method = new TargetMethod();
                        String auxStr = line.split("\",\"")[0];
                        auxStr = auxStr.substring(1);
                        String[] auxStr2 = auxStr.split("\\(");
                        method.setSourceFileName(auxStr2[0].split("\\.")[0] + ".java");
                        method.setName(auxStr2[0].split("\\.")[1]);
                        if ((auxStr2.length > 1) && (auxStr2[1].split("\\)").length > 0)) {
                            auxStr = auxStr2[1].split("\\)")[0];
                            if (!auxStr.isEmpty()) {
                                String[] attributeClassesList = auxStr.split(",");
                                List<String> l = new ArrayList<String>();
                                for (String s : attributeClassesList) {
                                    l.add(s.trim());
                                }
                                method.setAttributeClassList(l);
                            }
                        }
                        String parName = auxStr2[0].split("\\.")[0] + "_" + method.getName() + "_" + method.getAttributeClassList().size() + "_" + PARALLEL_CLASS_SUFFIX;
                        method.setParallelName(parName);
                        method.setParallelFileName(parName + ".java");
                        obtainSourceCode(method, inputFolder);
                        findMethodCalls(method, inputFolder);
                        generateCompleteParallelClass(method, inputFolder);
                        methods.add(method);
                    }
                }
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error reading profile: " + ex.getLocalizedMessage(), ex);
        } catch (IOException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error reading profile: " + ex.getLocalizedMessage(), ex);
        }
        return methods;
    }

    /**
     * Takes a solution and generates the name of the output folder
     * of the individual.
     *
     * @param solution
     * @return individual output folder
     */
    private String generateOutputFolder(Solution<Variable<Boolean>> solution) {
        String indivOutputFolder;
        indivOutputFolder = this.outputFolder + File.separator + "indiv_";
        Iterator<Variable<Boolean>> iter = solution.getVariables().iterator();
        while (iter.hasNext()) if (iter.next().getValue()) indivOutputFolder += "1"; else indivOutputFolder += "0";
        return indivOutputFolder;
    }

    /**
     * Substitutes each call to the target method by a call to the new 
     * parallel class. The substitution is made on output folder.
     * 
     * @param method already has a list of method calls
     * @param outputFolder
     */
    private void substituteMethodCalls(TargetMethod method, String outputFolder) {
        for (MethodCallInfo callInfo : method.getCallsList()) {
            BufferedReader reader;
            String fileName = outputFolder + File.separator + callInfo.fileName;
            List<String> newCode = new ArrayList<String>();
            newCode.add("import java.util.concurrent.Future;\n");
            newCode.add("import java.util.concurrent.ExecutionException;");
            try {
                reader = new BufferedReader(new FileReader(new File(fileName)));
                String line = null;
                boolean cont = true;
                while (cont) {
                    line = reader.readLine();
                    if ((line == null) || (line.trim().equals(callInfo.lineCode.trim()))) {
                        cont = false;
                    } else {
                        newCode.add(line);
                    }
                }
                String callableObj = "parObj_" + method.getName() + "_" + method.getAttributeClassList().size();
                newCode.add("\n" + method.getParallelName() + " " + callableObj + " = new " + method.getParallelName() + "();");
                if (method.getAttributeNameList().size() > 0) {
                    String args[] = line.trim().split(method.getName())[1].substring(1).split("\\)")[0].split("\\,");
                    int i = 0;
                    for (String s : method.getAttributeNameList()) {
                        newCode.add(callableObj + "." + s + " = " + args[i] + ";");
                        i++;
                    }
                }
                String newFuture = "fut_" + method.getName() + "_" + method.getAttributeClassList().size();
                String returnClass = method.getReturnClass().substring(0, 1).toUpperCase() + method.getReturnClass().substring(1);
                if (returnClass.equals("Int")) returnClass = "Integer";
                if (returnClass.equals("Void")) returnClass = "Integer";
                newCode.add("@SuppressWarnings(\"unchecked\")");
                newCode.add("Future<" + returnClass + "> " + newFuture + " = (Future<" + returnClass + ">) ThreadManager.runInNewThread(" + callableObj + ");");
                newCode.add("//" + line + "\n");
                String receptor = null;
                String recepType = null;
                if (line.trim().split(method.getName())[0].contains("=")) {
                    receptor = line.trim().split(method.getName())[0].split("=")[0].trim();
                    recepType = receptor.split(" ")[0];
                }
                if ((recepType != null) && (recepType != receptor)) {
                    newCode.add(receptor + " = 0;");
                    receptor = receptor.split(" ")[1].trim();
                    if (receptor.contains("[")) receptor = receptor.split("\\[")[0].trim();
                }
                if (receptor != null) {
                    do {
                        line = reader.readLine();
                        if (!line.contains(receptor)) newCode.add(line);
                    } while (!line.contains(receptor));
                    newCode.add("try {");
                    newCode.add(receptor + " = " + newFuture + ".get();");
                    newCode.add("ThreadManager.finishThread(" + newFuture + ");");
                    newCode.add("} catch (InterruptedException ex) {");
                    newCode.add("   System.out.println(ex);");
                    newCode.add("} catch (ExecutionException ex) {");
                    newCode.add("   System.out.println(ex);\n}\n");
                    newCode.add(line);
                }
                while ((line = reader.readLine()) != null) {
                    newCode.add(line);
                }
                reader.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error subst. method call (reading " + callInfo + "): " + ex.getLocalizedMessage(), ex);
            } catch (IOException ex) {
                Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error subst. method call (reading " + callInfo + "): " + ex.getLocalizedMessage(), ex);
            }
            writeToFile(newCode, fileName);
        }
    }

    /**
     * Takes a solution and generates the parallel code that it
     * represents. The code is generated in the output folder passed as
     * argument. The output folder is created in this method.
     * 
     * @param solution, output folder.
     * @return Output folder in absolute path representation
     */
    private String generateParallelizedCode(Solution<Variable<Boolean>> solution, String indivOutputFolder) {
        File f = new File(indivOutputFolder);
        indivOutputFolder = f.getAbsolutePath();
        if (!f.exists()) f.mkdir(); else Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Individual output folder " + indivOutputFolder + " already exist !!");
        File directory = new File(this.inputFolder);
        File[] files = directory.listFiles();
        for (int index = 0; index < files.length; index++) {
            copyFile(files[index].getAbsolutePath(), indivOutputFolder + File.separator + files[index].getName());
        }
        copyFile(THREAD_MANAGER_FILE, indivOutputFolder + File.separator + THREAD_MANAGER_CLASS + ".java");
        Iterator<Variable<Boolean>> iter = solution.getVariables().iterator();
        int i = 0;
        while (iter.hasNext()) {
            Variable<Boolean> c = iter.next();
            if (c.getValue()) {
                TargetMethod method = this.methodsToParallelize.get(i);
                copyFile(PARALLEL_CLASSES_FOLDER + File.separator + method.getParallelFileName(), indivOutputFolder + File.separator + method.getParallelName() + ".java");
                substituteMethodCalls(method, indivOutputFolder);
            }
            i++;
        }
        return indivOutputFolder;
    }

    /** 
     * Compiles the files on the given path
     *
     * @return true if compilation was successful; false if failed
     */
    private Boolean compileIndividual(String path) {
        Boolean compilationOK = true;
        File directory = new File(path);
        String[] files = directory.list();
        String cmd = "javac -Xlint:unchecked -d " + path;
        for (int i = 0; i < files.length; i++) {
            cmd += " " + path + File.separator + files[i];
        }
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String s = stdError.readLine();
            if (s != null) {
                compilationOK = false;
                System.out.println("Compilation of " + path + " failed. See output error:");
                System.out.println(s);
                while ((s = stdError.readLine()) != null) {
                    System.out.println(s);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error on compilation (" + path + "): " + ex.getLocalizedMessage(), ex);
        }
        return compilationOK;
    }

    /**
     * Executes the individual files stored in indivOutputFolder.
     *
     * @return individual execution time, and -1 if the execution
     * produced any error.
     */
    private Double executeIndividual(String indivOutputFolder) {
        String cmd = "java -cp " + indivOutputFolder + " " + this.mainClass;
        boolean anyError = false;
        double indivTime = System.currentTimeMillis();
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            String s;
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            if (debug) System.out.println("\nStandard output of the individual " + indivOutputFolder);
            while ((s = stdInput.readLine()) != null) {
                if (debug) System.out.println(s);
            }
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String e;
            if (debug) {
                System.out.println("\nStandard ERROR given by the individual " + indivOutputFolder);
                System.out.flush();
            }
            while ((e = stdError.readLine()) != null) {
                if (debug) System.out.println(e);
                anyError = true;
            }
        } catch (IOException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error on execution (" + indivOutputFolder + "): " + ex.getLocalizedMessage(), ex);
        }
        if (anyError) indivTime = -1; else indivTime = ((System.currentTimeMillis() - indivTime) / 1000);
        if (debug) System.out.println("\nIndividual " + indivOutputFolder + " - Time: " + indivTime);
        return indivTime;
    }

    /**
     * Helper method to dump a list of strings to a file, each string in a
     * different line.
     */
    private static void writeToFile(List<String> newCode, String fileName) {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(new File(fileName)));
            for (String s : newCode) writer.write(s + "\n");
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error writing code to " + fileName + ": " + ex.getLocalizedMessage(), ex);
        } catch (IOException ex) {
            Logger.getLogger(ParallelizeJava.class.getName()).log(Level.SEVERE, "Error writing code to " + fileName + ": " + ex.getLocalizedMessage(), ex);
        }
    }

    /**
     * Helper method to copy a file
     */
    private static void copyFile(String srFile, String dtFile) {
        try {
            File f1 = new File(srFile);
            File f2 = new File(dtFile);
            InputStream in = new FileInputStream(f1);
            OutputStream out = new FileOutputStream(f2);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Error copying " + srFile + " to " + dtFile);
            System.out.println(ex.getMessage() + " in the specified directory.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void evaluate(Solution<Variable<Boolean>> solution) {
        String indivOutputFolder = generateOutputFolder(solution);
        if (alreadyEvaluated.containsKey(indivOutputFolder)) {
            solution.getObjectives().set(EXECUTION_TIME, alreadyEvaluated.get(indivOutputFolder));
            return;
        }
        String fullIndivOutputFolder = generateParallelizedCode(solution, indivOutputFolder);
        Double execTime;
        if (!compileIndividual(fullIndivOutputFolder)) {
            execTime = Double.POSITIVE_INFINITY;
        } else {
            execTime = executeIndividual(fullIndivOutputFolder);
        }
        if (execTime < 0) execTime = (Double.POSITIVE_INFINITY / 2);
        solution.getObjectives().set(EXECUTION_TIME, execTime);
        alreadyEvaluated.put(indivOutputFolder, execTime);
    }

    @Override
    public void newRandomSetOfSolutions(Solutions<Variable<Boolean>> solutions) {
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        for (int i = 0; i < numberOfVariables; ++i) {
            idxs.add(i);
        }
        for (Solution<Variable<Boolean>> solution : solutions) {
            Collections.shuffle(idxs);
            for (int j = 0; j < numberOfVariables; ++j) {
                int idx = idxs.get(j);
                boolean varJ = false;
                if (Math.random() > 0.5) varJ = true;
                solution.getVariables().set(idx, new Variable<Boolean>(varJ));
            }
        }
    }

    @Override
    public Variable<Boolean> newVariable() {
        return new Variable<Boolean>(false);
    }

    @Override
    public Problem<Variable<Boolean>> clone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
