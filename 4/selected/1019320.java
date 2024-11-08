package net.sf.linns.comp.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import net.sf.linns.model.INeuralNetwork;

public class NetworkCompiler {

    public static final class CompileResult {

        private String stderr;

        private String stdout;

        public CompileResult(String stdout, String stderr) {
            super();
            this.stderr = stderr;
            this.stdout = stdout;
        }

        public String getStdErr() {
            return stderr;
        }

        public String getStdOut() {
            return stdout;
        }
    }

    public NetworkCompiler() {
    }

    /**
	 * Returns the substring after the last occurence of any of the chars
	 * provided.
	 * 
	 * @param line
	 * @param terminals
	 * @return
	 */
    private static String afterLastOccurenceOf(String line, char[] terminals) {
        if (line == null) {
            throw new IllegalArgumentException("line must not be null.");
        }
        int i = 0;
        int lastOcc = -1;
        while (line.length() > i) {
            char c = line.charAt(i);
            boolean isT = false;
            for (char t : terminals) {
                if (t == c) {
                    isT = true;
                    break;
                }
            }
            if (isT) {
                lastOcc = i;
            } else {
            }
            i++;
        }
        if (lastOcc < 0) {
            return "";
        } else {
            return line.substring(lastOcc + 1);
        }
    }

    public CompileResult compile(INeuralNetwork network, URL classURL) {
        String classFileName = classURL.getFile();
        String className = afterLastOccurenceOf(classFileName, new char[] { '/', '\\' });
        className = className.substring(0, className.indexOf('.'));
        if (classFileName.length() < 7 || !".class".equals(classFileName.substring(classFileName.length() - 6))) {
            throw new IllegalArgumentException("Invalid class file name. Filename must end with '.class'.");
        }
        File classFile = new File(classFileName);
        if (classFile.exists()) {
            throw new IllegalArgumentException("Class file does already exist. Cannot overwrite existing files.");
        }
        String javaFileName = classFileName.substring(0, classFileName.length() - 6) + ".java";
        File javaFile = new File(javaFileName);
        if (classFile.exists()) {
            throw new IllegalArgumentException("Class file is invalid. A Java file with same name does already exist.");
        }
        JavaGeneratorVisitor v = new JavaGeneratorVisitor();
        v.visit(network, className);
        String source = v.getSourceCode();
        try {
            javaFile.createNewFile();
            FileWriter f = new FileWriter(javaFile);
            f.write(source);
            f.close();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        CompileResult res;
        Process p;
        try {
            String classpath = System.getProperty("java.class.path");
            String fsep = System.getProperty("file.separator");
            p = Runtime.getRuntime().exec("javac -g:none -cp \"" + classpath + "\" \"" + javaFileName.replace("/", fsep) + "\"");
            String nextLine;
            InputStreamReader javac_stderrIS = new InputStreamReader(p.getErrorStream());
            BufferedReader javac_stderrBR = new BufferedReader(javac_stderrIS);
            StringBuilder javac_stderr = new StringBuilder(512);
            while ((nextLine = javac_stderrBR.readLine()) != null) {
                javac_stderr.append(nextLine);
                javac_stderr.append("\n");
            }
            javac_stderrBR.close();
            InputStreamReader javac_stdoutIS = new InputStreamReader(p.getInputStream());
            BufferedReader javac_stdoutBR = new BufferedReader(javac_stdoutIS);
            StringBuilder javac_stdout = new StringBuilder(512);
            while ((nextLine = javac_stdoutBR.readLine()) != null) {
                javac_stdout.append(nextLine);
                javac_stdout.append("\n");
            }
            javac_stdoutBR.close();
            System.currentTimeMillis();
            res = new CompileResult(javac_stdout.toString(), javac_stderr.toString());
        } catch (IOException e) {
            e.printStackTrace();
            res = null;
        }
        return res;
    }
}
