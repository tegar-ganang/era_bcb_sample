package org.cishell.templates.staticexecutable;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.cishell.framework.CIShellContext;
import org.cishell.framework.algorithm.Algorithm;
import org.cishell.framework.algorithm.AlgorithmExecutionException;
import org.cishell.framework.algorithm.AlgorithmProperty;
import org.cishell.framework.algorithm.ProgressMonitor;
import org.cishell.framework.data.BasicData;
import org.cishell.framework.data.Data;
import org.cishell.framework.data.DataProperty;
import org.cishell.templates.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * 
 * @author Bruce Herr (bh2@bh2.net)
 */
public class StaticExecutableRunner implements Algorithm {

    public static final String DEFAULT_SAFE_SUBSTITUTE = "_";

    public static final Map<String, String> TROUBLE_CHARACTER_SUBSTITUTIONS;

    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("\"", "''");
        m.put(";", DEFAULT_SAFE_SUBSTITUTE);
        m.put(",", DEFAULT_SAFE_SUBSTITUTE);
        m.put("&", DEFAULT_SAFE_SUBSTITUTE);
        m.put("|", DEFAULT_SAFE_SUBSTITUTE);
        m.put("<", DEFAULT_SAFE_SUBSTITUTE);
        m.put(">", DEFAULT_SAFE_SUBSTITUTE);
        TROUBLE_CHARACTER_SUBSTITUTIONS = Collections.unmodifiableMap(m);
    }

    public static final String EXECUTABLE_PLACEHOLDER = "executable";

    public static final String DATA_LABEL_PLACEHOLDER = "data_label";

    public static final String IN_FILE_PLACEHOLDER = "inFile";

    private String algorithm;

    private String macOsXPpcDirectoryPath;

    private String macOsX;

    private String algorithmWin32;

    private String win32;

    private String algorithmLinuxX86;

    private String linux;

    private String algorithmDefault;

    private String algorithmDirectoryPath;

    private String temporaryDirectoryPath;

    private Data[] data;

    private Dictionary<String, Object> parameters;

    private Properties properties;

    private CIShellContext ciShellContext;

    private ProgressMonitor monitor;

    private BundleContext bundleContext;

    private String algorithmName;

    public StaticExecutableRunner(BundleContext bundleContext, CIShellContext ciShellContext, Properties properties, Dictionary<String, Object> parameters, Data[] data, ProgressMonitor monitor, String algorithmName) throws IOException {
        this.bundleContext = bundleContext;
        this.ciShellContext = ciShellContext;
        this.properties = properties;
        this.parameters = parameters;
        this.data = data;
        this.monitor = monitor;
        this.algorithmName = algorithmName;
        this.algorithm = algorithmName + "/";
        this.macOsXPpcDirectoryPath = algorithm + "macosx.ppc/";
        this.macOsX = "macosx";
        this.algorithmWin32 = algorithm + "win32/";
        this.win32 = "win32";
        this.algorithmLinuxX86 = algorithm + "linux.x86/";
        this.linux = "linux";
        this.algorithmDefault = algorithm + "default/";
        if (this.monitor == null) {
            this.monitor = ProgressMonitor.NULL_MONITOR;
        }
        if (this.data == null) {
            this.data = new Data[0];
        }
        if (this.parameters == null) {
            this.parameters = new Hashtable<String, Object>();
        }
        this.temporaryDirectoryPath = makeTemporaryDirectory();
        this.algorithmDirectoryPath = String.format("%s%s%s%s", temporaryDirectoryPath, File.separator, properties.getProperty("Algorithm-Directory"), File.separator);
    }

    /**
	 * @see org.cishell.framework.algorithm.Algorithm#execute()
	 */
    public Data[] execute() throws AlgorithmExecutionException {
        copyFilesUsedByExecutableIntoDir(getTempDirectory());
        makeDirExecutable(algorithmDirectoryPath);
        String[] commandLineArguments = createCommandLineArguments(algorithmDirectoryPath, this.data, this.parameters);
        File[] rawOutput = executeProgram(commandLineArguments, algorithmDirectoryPath);
        return formatAsData(rawOutput);
    }

    private void copyFilesUsedByExecutableIntoDir(File dir) throws AlgorithmExecutionException {
        try {
            Enumeration e = bundleContext.getBundle().getEntryPaths("/" + algorithmName);
            Set entries = new HashSet();
            while (e != null && e.hasMoreElements()) {
                String entryPath = (String) e.nextElement();
                if (entryPath.endsWith("/")) {
                    entries.add(entryPath);
                }
            }
            dir = new File(dir.getPath() + File.separator + algorithmName);
            dir.mkdirs();
            String os = bundleContext.getProperty("osgi.os");
            String arch = bundleContext.getProperty("osgi.arch");
            String path = null;
            if (entries.contains(algorithmDefault)) {
                String defaultPath = algorithmDefault;
                copyDir(dir, defaultPath, 0);
            }
            if (os.equals(win32) && entries.contains(algorithmWin32)) {
                path = algorithmWin32;
            } else if (os.equals(macOsX) && entries.contains(macOsXPpcDirectoryPath)) {
                path = macOsXPpcDirectoryPath;
            } else if (os.equals(linux) && entries.contains(algorithmLinuxX86)) {
                path = algorithmLinuxX86;
            }
            String platformPath = algorithm + os + "." + arch + "/";
            if (entries.contains(platformPath)) {
                path = platformPath;
            }
            if (path == null) {
                throw new AlgorithmExecutionException("Unable to find compatible executable");
            } else {
                copyDir(dir, path, 0);
            }
        } catch (IOException e) {
            throw new AlgorithmExecutionException(e.getMessage(), e);
        }
    }

    protected void makeDirExecutable(String baseDir) throws AlgorithmExecutionException {
        if (new File("/bin/chmod").exists()) {
            try {
                String executable = baseDir + properties.getProperty("executable");
                Runtime.getRuntime().exec("/bin/chmod +x " + executable).waitFor();
            } catch (IOException e) {
                throw new AlgorithmExecutionException(e);
            } catch (InterruptedException e) {
                throw new AlgorithmExecutionException(e);
            }
        }
    }

    protected File[] executeProgram(String[] commandArray, String baseDirPath) throws AlgorithmExecutionException {
        File baseDir = new File(baseDirPath);
        String[] beforeFiles = baseDir.list();
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
            processBuilder.directory(new File(baseDirPath));
            process = processBuilder.start();
            process.getOutputStream().close();
        } catch (IOException e1) {
            throw new AlgorithmExecutionException(e1.getMessage(), e1);
        }
        monitor.start(ProgressMonitor.CANCELLABLE, -1);
        InputStream in = process.getInputStream();
        StringBuffer inBuffer = new StringBuffer();
        InputStream err = process.getErrorStream();
        StringBuffer errBuffer = new StringBuffer();
        Integer exitValue = null;
        boolean killedOnPurpose = false;
        while (!killedOnPurpose && exitValue == null) {
            inBuffer = logStream(LogService.LOG_INFO, in, inBuffer);
            errBuffer = logStream(LogService.LOG_ERROR, err, errBuffer);
            if (monitor.isCanceled()) {
                killedOnPurpose = true;
                process.destroy();
            }
            try {
                int value = process.exitValue();
                exitValue = new Integer(value);
            } catch (IllegalThreadStateException e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        monitor.done();
        if (process.exitValue() != 0 && !killedOnPurpose) {
            throw new AlgorithmExecutionException("Algorithm exited unexpectedly (exit value: " + process.exitValue() + "). Please check the console window for any error messages.");
        }
        String[] afterFiles = baseDir.list();
        Arrays.sort(beforeFiles);
        Arrays.sort(afterFiles);
        List outputs = new ArrayList();
        int beforeIndex = 0;
        int afterIndex = 0;
        while (beforeIndex < beforeFiles.length && afterIndex < afterFiles.length) {
            if (beforeFiles[beforeIndex].equals(afterFiles[afterIndex])) {
                beforeIndex++;
                afterIndex++;
            } else {
                outputs.add(new File(baseDirPath + afterFiles[afterIndex]));
                afterIndex++;
            }
        }
        while (afterIndex < afterFiles.length) {
            outputs.add(new File(baseDirPath + afterFiles[afterIndex]));
            afterIndex++;
        }
        return (File[]) outputs.toArray(new File[] {});
    }

    protected Data[] formatAsData(File[] files) {
        String outData = (String) properties.get(AlgorithmProperty.OUT_DATA);
        if (("" + outData).trim().equalsIgnoreCase(AlgorithmProperty.NULL_DATA)) {
            return null;
        }
        String[] formats = outData.split(",");
        Map nameToFileMap = new HashMap();
        for (int i = 0; i < files.length; i++) {
            nameToFileMap.put(files[i].getName(), files[i]);
        }
        Data[] data = null;
        if (formats.length > files.length) {
            data = new Data[formats.length];
        } else {
            data = new Data[files.length];
        }
        for (int i = 0; i < data.length; i++) {
            String file = properties.getProperty("outFile[" + i + "]", null);
            if (i < formats.length) {
                File f = (File) nameToFileMap.remove(file);
                if (f != null) {
                    data[i] = new BasicData(f, formats[i]);
                    String label = properties.getProperty("outFile[" + i + "].label", f.getName());
                    data[i].getMetadata().put(DataProperty.LABEL, label);
                    String type = properties.getProperty("outFile[" + i + "].type", DataProperty.OTHER_TYPE);
                    type = type.trim();
                    if (type.equalsIgnoreCase(DataProperty.MATRIX_TYPE)) {
                        type = DataProperty.MATRIX_TYPE;
                    } else if (type.equalsIgnoreCase(DataProperty.NETWORK_TYPE)) {
                        type = DataProperty.NETWORK_TYPE;
                    } else if (type.equalsIgnoreCase(DataProperty.TREE_TYPE)) {
                        type = DataProperty.TREE_TYPE;
                    } else if (type.equalsIgnoreCase(DataProperty.TEXT_TYPE)) {
                        type = DataProperty.TEXT_TYPE;
                    } else if (type.equalsIgnoreCase(DataProperty.PLOT_TYPE)) {
                        type = DataProperty.PLOT_TYPE;
                    } else if (type.equalsIgnoreCase(DataProperty.TABLE_TYPE)) {
                        type = DataProperty.TABLE_TYPE;
                    } else {
                        type = DataProperty.OTHER_TYPE;
                    }
                    data[i].getMetadata().put(DataProperty.TYPE, type);
                }
            } else {
                Iterator iter = nameToFileMap.values().iterator();
                while (iter.hasNext()) {
                    File f = (File) iter.next();
                    data[i] = new BasicData(f, "file:text/plain");
                    data[i].getMetadata().put(DataProperty.LABEL, f.getName());
                    i++;
                }
                break;
            }
        }
        return data;
    }

    protected StringBuffer logStream(int logLevel, InputStream is, StringBuffer buffer) throws AlgorithmExecutionException {
        try {
            int available = is.available();
            if (available > 0) {
                byte[] b = new byte[available];
                is.read(b);
                buffer.append(new String(b));
                buffer = log(logLevel, buffer);
            }
        } catch (EOFException e) {
        } catch (IOException e) {
            throw new AlgorithmExecutionException("Error when processing the algorithm's screen output", e);
        }
        return buffer;
    }

    protected StringBuffer log(int logLevel, StringBuffer buffer) {
        if (buffer.indexOf("\n") != -1) {
            LogService log = (LogService) ciShellContext.getService(LogService.class.getName());
            int lastGoodIndex = 0;
            int fromIndex = 0;
            while (fromIndex != -1 && fromIndex < buffer.length()) {
                int toIndex = buffer.indexOf("\n", fromIndex);
                if (toIndex != -1) {
                    String message = buffer.substring(fromIndex, toIndex);
                    if (log == null) {
                        System.out.println(message);
                    } else {
                        log.log(logLevel, message);
                    }
                    fromIndex = toIndex + 1;
                    lastGoodIndex = toIndex + 1;
                } else {
                    fromIndex = -1;
                }
            }
            if (lastGoodIndex > 0) {
                buffer = new StringBuffer(buffer.substring(lastGoodIndex));
            }
        }
        return buffer;
    }

    protected String[] createCommandLineArguments(String algorithmDirectory, Data[] data, Dictionary<String, Object> parameters) {
        String template = "" + this.properties.getProperty("template");
        String[] commands = template.split("\\s");
        for (int ii = 0; ii < commands.length; ii++) {
            commands[ii] = substituteVars(commands[ii], data, parameters);
        }
        if (!new File(algorithmDirectory + commands[0]).exists()) {
            if (new File(algorithmDirectory + commands[0] + ".bat").exists()) {
                commands[0] = commands[0] + ".bat";
            }
        }
        commands[0] = algorithmDirectory + commands[0];
        return commands;
    }

    protected String substituteVars(String template, Data[] data, Dictionary parameters) {
        template = template.replace(String.format("${%s}", EXECUTABLE_PLACEHOLDER), properties.getProperty(EXECUTABLE_PLACEHOLDER));
        for (int ii = 0; ii < data.length; ii++) {
            template = substituteForDataLabel(template, data, ii);
            template = substituteForFilePath(template, data, ii);
        }
        for (Enumeration i = parameters.keys(); i.hasMoreElements(); ) {
            String key = (String) i.nextElement();
            Object value = parameters.get(key);
            if (value == null) {
                value = "";
            }
            template = template.replace(String.format("${%s}", key), value.toString());
        }
        return template;
    }

    private String substituteForDataLabel(String template, Data[] data, int ii) {
        String key = String.format("${%s[%d]}", DATA_LABEL_PLACEHOLDER, ii);
        if (!template.contains(key)) {
            return template;
        } else {
            Object labelObject = data[ii].getMetadata().get(DataProperty.LABEL);
            String label = "unknown_data_label";
            if (labelObject != null) {
                label = labelObject.toString();
            }
            String cleanedLabel = cleanDataLabel(label);
            return template.replace(key, cleanedLabel);
        }
    }

    private String cleanDataLabel(String label) {
        String cleanedLabel = label;
        for (String troubleCharacter : TROUBLE_CHARACTER_SUBSTITUTIONS.keySet()) {
            cleanedLabel = cleanedLabel.replace(troubleCharacter, TROUBLE_CHARACTER_SUBSTITUTIONS.get(troubleCharacter));
        }
        return cleanedLabel;
    }

    private String substituteForFilePath(String template, Data[] data, int ii) {
        String key = String.format("${%s[%d]}", IN_FILE_PLACEHOLDER, ii);
        if (!template.contains(key)) {
            return template;
        } else {
            Object datumObject = data[ii].getData();
            String filePath = "unknown_file_path";
            if (datumObject != null && datumObject instanceof File) {
                File file = (File) datumObject;
                filePath = file.getAbsolutePath();
            }
            String substituted = template.replace(key, filePath);
            return substituted;
        }
    }

    public File getTempDirectory() {
        return new File(temporaryDirectoryPath);
    }

    protected String makeTemporaryDirectory() throws IOException {
        File sessionDir = Activator.getTempDirectory();
        File dir = File.createTempFile("StaticExecutableRunner-", "", sessionDir);
        dir.delete();
        dir.mkdirs();
        return dir.getAbsolutePath();
    }

    private void copyDir(File dir, String dirPath, int depth) throws IOException {
        Enumeration e = bundleContext.getBundle().getEntryPaths(dirPath);
        while (e != null && e.hasMoreElements()) {
            String path = (String) e.nextElement();
            if (path.endsWith("/")) {
                String dirName = getName(path);
                File subDirectory = new File(dir.getPath() + File.separator + dirName);
                subDirectory.mkdirs();
                copyDir(subDirectory, path, depth + 1);
            } else {
                copyFile(dir, path);
            }
        }
    }

    private void copyFile(File dir, String path) throws IOException {
        URL entry = bundleContext.getBundle().getEntry(path);
        String file = getName(path);
        FileOutputStream outStream = new FileOutputStream(dir.getPath() + File.separator + file);
        ReadableByteChannel in = Channels.newChannel(entry.openStream());
        FileChannel out = outStream.getChannel();
        out.transferFrom(in, 0, Integer.MAX_VALUE);
        in.close();
        out.close();
    }

    private String getName(String path) {
        if (path.lastIndexOf('/') == path.length() - 1) {
            path = path.substring(0, path.length() - 1);
        }
        path = path.substring(path.lastIndexOf('/') + 1, path.length());
        return path;
    }
}
