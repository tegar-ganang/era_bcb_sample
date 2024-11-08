package edu.gcsc.jacuzzi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 *
 * @author Alexander Heusel
 */
public class NVCC {

    private static NVCC instance = null;

    private String nvcc = null;

    private boolean noExternC = false;

    private boolean keep = false;

    protected NVCC() {
        Map<String, String> envMap = System.getenv();
        if (envMap.containsKey("JACUZZI_NVCC")) {
            nvcc = envMap.get("JACUZZI_NVCC");
        } else {
            String opSys = System.getProperty("os.name");
            if (opSys.equals("Mac OS X")) {
                nvcc = "/usr/local/cuda/bin/nvcc";
            } else if (opSys.contains("Windows") && Float.valueOf(System.getProperty("os.version")) > 5.0) {
                nvcc = "C:\\CUDA\\bin\\ncc";
            } else if (opSys.equals("Linux")) {
                nvcc = "/usr/local/cuda/bin/nvcc";
            } else {
                throw new java.lang.IllegalStateException("System not supported!");
            }
        }
    }

    public static NVCC getNVCC() {
        if (instance == null) {
            instance = new NVCC();
        }
        return instance;
    }

    public boolean isKeep() {
        return keep;
    }

    public void setKeep(boolean keep) {
        this.keep = keep;
    }

    public boolean isNoExternC() {
        return noExternC;
    }

    public void setNoExternC(boolean noExternC) {
        this.noExternC = noExternC;
    }

    public String getNvcc() {
        return nvcc;
    }

    public void setNvcc(String nvcc) {
        this.nvcc = nvcc;
    }

    /**
     * Compiles the source of a kernel into a kernel module.
     *
     * @param inStr The InputStream with the sourcecode of the kernel
     * @return The kernel module
     */
    public String compile(InputStream inStr) throws IOException, InterruptedException, Exception {
        return compile(readToString(inStr));
    }

    /**
     * Compiles the source of a kernel into a kernel module.
     *
     * @param source The sourcecode of the kernel
     * @return The kernel module
     */
    public String compile(String source) throws IOException, InterruptedException, Exception {
        return compile(source, this.nvcc, null, this.keep, this.noExternC);
    }

    /**
     * Compiles the source of a kernel into a kernel module.
     *
     * @param source The sourcecode of the kernel
     * @param nvcc The NVCC commandline
     * @param options The options to be passed to NVCC
     * @param keep Determines if temporary files of the build-process are kept
     * @param noExternC Determines if the kernel-source will be surrounded
     * by <code>extern 'C'</code>.
     * @return The kernel module
     */
    public String compile(String source, String nvcc, String[] options, boolean keep, boolean noExternC) throws IOException, InterruptedException, Exception {
        File tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".cu");
        writeToFile(tmpFile, noExternC ? source : "extern \"C\"\n{\n" + source + "\n}\n");
        options = appendTo(options, "--output-directory", tmpFile.getParent(), "--cubin");
        if (keep) {
            System.err.format("*** compiler output in %s", tmpFile.getParent());
            options = appendTo(options, "--keep");
        }
        options = appendTo(options, tmpFile.getPath());
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(nvcc + " " + merge(options));
        String err = readToString(proc.getErrorStream());
        int exitVal = proc.waitFor();
        if (exitVal != 0) {
            throw new java.lang.Exception(err);
        }
        File outFile = new File(tmpFile.getParent() + File.separator + (tmpFile.getName().split("\\."))[0] + ".cubin");
        String module = readToString(outFile);
        tmpFile.delete();
        outFile.delete();
        return module;
    }

    /**
     * Compiles the source of a kernel into a kernel module. The source-files have to
     * be located in the given package. The name of the main-file has to be the same as
     * the package-name. noExternC is ignored in this compilation method.
     *
     * @param pkgName The name of the package, e.g. "myproject/cuda/mykernel"
     * @return The kernel module
     */
    public String compilePackage(String pkgName) throws IOException, InterruptedException, Exception {
        return compilePackage(pkgName, this.nvcc, null, this.keep);
    }

    /**
     * Compiles the source of a kernel into a kernel module. The source-files have to
     * be located in the given package. The name of the main-file has to be the same as
     * the package-name. noExternC is ignored in this compilation method.
     *
     * @param pkgName The name of the package, e.g. "myproject/cuda/mykernel"
     * @param nvcc The NVCC commandline
     * @param options The options to be passed to NVCC
     * @param keep Determines if temporary files of the build-process are kept
     * by <code>extern 'C'</code>.
     * @return The kernel module
     */
    public String compilePackage(String pkgName, String nvcc, String[] options, boolean keep) throws IOException, InterruptedException, Exception {
        pkgName = pkgName.startsWith("/") ? pkgName.substring(1) : pkgName;
        pkgName = pkgName.endsWith("/") ? pkgName.substring(0, pkgName.length() - 2) : pkgName;
        String[] parts = pkgName.split("/");
        String module = parts[parts.length - 1];
        Pattern pattern = Pattern.compile(".*" + pkgName + "/[^\\.]+\\.(h|cu)");
        Collection<String> list = getResources(pattern);
        ArrayList<String> files = new ArrayList<String>();
        boolean moduleExists = false;
        String currentFile = null;
        for (String res : list) {
            parts = res.split("/");
            currentFile = parts[parts.length - 1];
            files.add(currentFile);
            if (currentFile.equals(module + ".cu")) {
                moduleExists = true;
            }
        }
        if (!moduleExists) {
            throw new Exception("Module file not found in package. Has it the same name as the package?");
        }
        File folder = new File(System.getProperty("java.io.tmpdir") + File.separator + module);
        folder.mkdir();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Iterator<String> iter = files.iterator();
        while (iter.hasNext()) {
            currentFile = iter.next();
            writeToFile(new File(folder.getPath() + File.separator + currentFile), classLoader.getResourceAsStream(pkgName + "/" + currentFile));
        }
        options = appendTo(options, "--output-directory", folder.getPath(), "--cubin");
        if (keep) {
            System.err.format("*** compiler output in %s", folder.getPath());
            options = appendTo(options, "--keep");
        }
        options = appendTo(options, folder.getPath() + File.separator + module + ".cu");
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(nvcc + " " + merge(options));
        String err = readToString(proc.getErrorStream());
        int exitVal = proc.waitFor();
        if (exitVal != 0) {
            throw new java.lang.Exception(err);
        }
        File outFile = new File(folder.getPath() + File.separator + module + ".cubin");
        String res = readToString(outFile);
        File[] delFiles = folder.listFiles();
        for (File delFile : delFiles) {
            delFile.delete();
        }
        folder.delete();
        return res;
    }

    /**
     * Appends strings to the given array.
     *
     * @param array The array to append to.
     * @param args The strings to append to the array.
     * @return A new array with the args appended to it.
     */
    private static String[] appendTo(String[] array, String... args) {
        if (array == null) {
            return args;
        }
        String[] buffArray = new String[array.length + args.length];
        int i = 0;
        for (; i < array.length; i++) {
            buffArray[i] = array[i];
        }
        for (int j = 0; j < args.length; j++) {
            buffArray[i++] = args[j];
        }
        return buffArray;
    }

    /**
     * Merges all elements of a string array in one string, separating them by spaces.
     *
     * @param array The array to be merged.
     * @return The merged array.
     */
    private static String merge(String[] array) {
        if (array == null) {
            return "";
        }
        if (array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(" ");
            sb.append(array[i]);
        }
        return sb.toString();
    }

    /**
     * Reads from an InputStream to a String.
     *
     * @param inStr The InputStream to use.
     * @return The String containing the data of the InputStream.
     */
    private static String readToString(InputStream inStr) throws IOException {
        StringBuffer sb = new StringBuffer(1024);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStr));
        char[] chars = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(chars)) > -1) {
            sb.append(chars, 0, numRead);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Reads the contents of a file to a String.
     *
     * @param file The File to read from.
     * @return The contents of the file.
     */
    private static String readToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader tin = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line = tin.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Writes a String to a file.
     *
     * @param file The file to write to.
     * @param string The String to write.
     */
    private static void writeToFile(File file, String string) throws IOException {
        FileWriter wrt = new FileWriter(file);
        Writer tout = new BufferedWriter(wrt);
        tout.write(string);
        tout.write("\n");
        tout.flush();
    }

    /**
     * Writes an InputStream to a file.
     *
     * @param file The file to write to.
     * @param inStr The InputStream to write.
     */
    private static void writeToFile(File file, InputStream inStr) throws IOException {
        writeToFile(file, readToString(inStr));
    }

    /**
     * For all elements of java.class.path get a Collection of resources
     * Pattern pattern = Pattern.compile(".*"); gets all resources
     *
     * @param pattern the pattern to match
     * @return the resources in the order they are found
     */
    public static Collection<String> getResources(Pattern pattern) {
        ArrayList<String> retval = new ArrayList<String>();
        String classPath = System.getProperty("java.class.path", ".");
        String[] classPathElements = classPath.split(":");
        for (String element : classPathElements) {
            retval.addAll(getResources(element, pattern));
        }
        return retval;
    }

    private static Collection<String> getResources(String element, Pattern pattern) {
        ArrayList<String> retval = new ArrayList<String>();
        File file = new File(element);
        if (file.isDirectory()) {
            retval.addAll(getResourcesFromDirectory(file, pattern));
        } else {
            retval.addAll(getResourcesFromJarFile(file, pattern));
        }
        return retval;
    }

    private static Collection<String> getResourcesFromJarFile(File file, Pattern pattern) {
        ArrayList<String> retval = new ArrayList<String>();
        ZipFile zf;
        try {
            zf = new ZipFile(file);
        } catch (ZipException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
        Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();
            String fileName = ze.getName();
            boolean accept = pattern.matcher(fileName).matches();
            if (accept) {
                retval.add(fileName);
            }
        }
        try {
            zf.close();
        } catch (IOException e1) {
            throw new Error(e1);
        }
        return retval;
    }

    private static Collection<String> getResourcesFromDirectory(File directory, Pattern pattern) {
        ArrayList<String> retval = new ArrayList<String>();
        File[] fileList = directory.listFiles();
        for (File file : fileList) {
            if (file.isDirectory()) {
                retval.addAll(getResourcesFromDirectory(file, pattern));
            } else {
                try {
                    String fileName = file.getCanonicalPath();
                    boolean accept = pattern.matcher(fileName).matches();
                    if (accept) {
                        retval.add(fileName);
                    }
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }
        return retval;
    }
}
