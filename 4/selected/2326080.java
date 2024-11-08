package com.sun.specweb2005;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.NameValuePair;
import com.sun.faban.harness.Benchmark;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.RemoteCallable;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.sun.faban.harness.RunContext.*;
import static com.sun.faban.harness.util.FileHelper.copyFile;

/**
 * Harness hook for SPECweb2005 drives the process for executing
 * SPECweb2005.
 *
 * @author Sreekanth Setty
 */
public class SpecWeb2005Benchmark implements Benchmark {

    static Logger logger = Logger.getLogger(SpecWeb2005Benchmark.class.getName());

    String clientJar;

    String testType;

    String runDir;

    String runID;

    ParamRepository par;

    CommandHandle handle;

    String dbServer;

    private Calendar startTime;

    List<NameValuePair<Integer>> hostsPorts;

    /**
     * Allows benchmark to validate the configuration file. Note that no
     * execution facility is available during validation. This method is just
     * for validation and modifications of the run configuration.
     *
     * @throws Exception if any error occurred.
     * @see com.sun.faban.harness.RunContext#exec(com.sun.faban.common.Command)
     */
    public void validate() throws Exception {
        par = getParamRepository();
        int rampUp = Integer.parseInt(par.getParameter("fa:runConfig/threadRampupSeconds"));
        rampUp += Integer.parseInt(par.getParameter("fa:runConfig/warmupSeconds"));
        par.setParameter("fa:runConfig/fa:runControl/fa:rampUp", String.valueOf(rampUp));
        hostsPorts = par.getHostPorts("fa:runConfig/fa:hostConfig/fa:hostPorts");
        runDir = getOutDir();
        runID = getRunId();
        testType = par.getParameter("fa:runConfig/testtype");
        clientJar = par.getParameter("fa:runConfig/clientDir");
        StreamSource stylesheet = new StreamSource(getBenchmarkDir() + "META-INF" + File.separator + "run.xsl");
        StreamSource src = new StreamSource(getParamFile());
        StreamResult result = new StreamResult(new File(runDir, "Test.config"));
        Transformer t = TransformerFactory.newInstance().newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);
        stylesheet = new StreamSource(getBenchmarkDir() + "META-INF" + File.separator + testType + ".xsl");
        result = new StreamResult(new File(runDir, testType + ".config"));
        t = TransformerFactory.newInstance().newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);
        stylesheet = new StreamSource(getBenchmarkDir() + "META-INF" + File.separator + "testbed.xsl");
        result = new StreamResult(new File(runDir, "Testbed.config"));
        t = TransformerFactory.newInstance().newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);
    }

    /**
     * This method is called to configure the specific benchmark run
     * Tasks done in this method include reading user parameters,
     * logging them and initializing various local variables.
     */
    public void configure() throws Exception {
    }

    /**
     * This method is responsible for starting the benchmark run
     * @throws java.lang.Exception
     */
    private void start_clients() throws Exception {
        String javaOptions = par.getParameter("fh:jvmConfig/fh:jvmOptions");
        String javaOptionsNoGC = javaOptions.replaceFirst("-Xloggc:\\S+", " ");
        cleanOldFiles(par.getTokenizedValue("fa:runConfig/fa:hostConfig/fa:host"));
        for (NameValuePair hostPort : hostsPorts) {
            String tmp = getTmpDir(hostPort.name);
            String out = tmp + "out." + runID;
            String errors = tmp + "errors." + runID;
            String gc = tmp + "gc." + runID;
            if (hostPort.value != null) {
                out += "." + hostPort.value;
                errors += "." + hostPort.value;
                gc += "." + hostPort.value;
            }
            String cmd = "java " + javaOptionsNoGC + " -Xloggc:" + gc + " -classpath " + clientJar + " specwebclient ";
            if (hostPort.value != null) cmd += " -p " + hostPort.value;
            logger.info("Starting the client on " + hostPort.name + ": " + cmd);
            Command client = new Command(cmd);
            client.setSynchronous(false);
            client.setOutputFile(Command.STDOUT, out);
            client.setOutputFile(Command.STDERR, errors);
            client.setStreamHandling(Command.STDOUT, Command.CAPTURE);
            client.setStreamHandling(Command.STDERR, Command.CAPTURE);
            exec(hostPort.name, client);
        }
    }

    private static void cleanOldFiles(String[] hosts) {
        for (String hostName : hosts) {
            final String tmp = getTmpDir(hostName);
            try {
                exec(hostName, new RemoteCallable() {

                    public Serializable call() throws Exception {
                        logger.info("Cleaning the temporary files in " + tmp);
                        File tmpDir = new File(tmp);
                        File[] fileList = tmpDir.listFiles();
                        for (File f : fileList) {
                            if (f.getName().startsWith("out") || f.getName().startsWith("err") || f.getName().startsWith("gc")) if (f.isFile() && !f.delete()) logger.info("Could not delete file: " + tmp + f.getName());
                        }
                        return null;
                    }
                });
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception deleting files", e);
            }
        }
    }

    /**
     * This method is responsible for starting the benchmark run
     * @throws java.lang.Exception 
     */
    public void start() throws Exception {
        start_clients();
        StringBuilder classpath = new StringBuilder();
        File clientDir = new File(clientJar).getParentFile();
        File[] files = clientDir.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(".jar")) {
                if (fileName.startsWith("jcommon") || fileName.startsWith("jfreechart")) {
                    String absolutePath = file.getAbsolutePath();
                    classpath.append(absolutePath).append(File.pathSeparatorChar);
                }
            }
        }
        classpath.append(clientJar);
        String cmd = "java -server -Xmx800m -Xms800m -classpath " + classpath + "  specweb";
        logger.info("Starting the Master: " + cmd);
        Command c = new Command(cmd);
        c.setSynchronous(false);
        c.setOutputFile(Command.STDOUT, "result.txt");
        c.setStreamHandling(Command.STDOUT, Command.TRICKLE_LOG);
        c.setStreamHandling(Command.STDERR, Command.TRICKLE_LOG);
        c.setWorkingDirectory(runDir);
        startTime = Calendar.getInstance();
        handle = exec(c);
    }

    /**
     * This method is responsible for waiting for all commands started and
     * run all postprocessing needed.
     *
     * @throws Exception if any error occurred.
     */
    public void end() throws Exception {
        handle.waitFor();
        Calendar endTime = Calendar.getInstance();
        File resultsDir = new File(runDir, "results");
        if (!resultsDir.isDirectory()) throw new Exception("The results directory not found!");
        String resHtml = null;
        String resTxt = null;
        String[] resultFiles = resultsDir.list();
        for (String resultFile : resultFiles) {
            if (resultFile.indexOf("html") >= 0) resHtml = resultFile; else if (resultFile.indexOf("txt") >= 0) resTxt = resultFile;
        }
        if (resHtml == null) throw new IOException("SPECweb2005 output (html) file not found");
        if (resTxt == null) throw new IOException("SPECweb2005 output (txt) file not found");
        File resultHtml = new File(resultsDir, resHtml);
        copyFile(resultHtml.getAbsolutePath(), runDir + "SPECWeb-result.html", false);
        BufferedReader reader = new BufferedReader(new FileReader(new File(resultsDir, resTxt)));
        logger.fine("Text file: " + resultsDir + resTxt);
        Writer writer = new FileWriter(runDir + "summary.xml");
        SummaryParser parser = new SummaryParser(getRunId(), startTime, endTime, logger);
        parser.convert(reader, writer);
        writer.close();
        reader.close();
    }

    /**
     * This method aborts the current benchmark run and is
     * called when a user asks for a run to be killed
     */
    public void kill() {
    }
}
