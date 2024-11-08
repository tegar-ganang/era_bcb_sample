package uk.ac.ncl.cs.instantsoap.r;

import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * Describe class RProcessorBase here.
 *
 *
 * Created: Fri Jul  4 16:35:05 2008
 *
 * @author <a href="mailto:phillord@ncl.ac.uk">Phillip Lord</a>
 * @version 1.0
 */
public abstract class RProcessorBase {

    public void writeStringToFile(File file, String string) throws IOException {
        writeStringToOutputStream(new FileOutputStream(file), string);
    }

    public void writeStringToOutputStream(OutputStream outp, String string) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outp));
        writer.write(string, 0, string.length());
        writer.close();
    }

    public String readStringFromFile(File file) throws IOException {
        return readStringFromInputStream(new FileInputStream(file));
    }

    public String readStringFromInputStream(InputStream inp) throws IOException {
        StringBuffer retn = new StringBuffer();
        BufferedInputStream bufferedInp = new BufferedInputStream(inp);
        int read;
        while ((read = bufferedInp.read()) != -1) {
            retn.append((char) read);
        }
        inp.close();
        return retn.toString();
    }

    public InputStream getInputStreamForRScript(Class clazz, String rScriptName) throws IOException {
        InputStream inp = clazz.getResourceAsStream(rScriptName);
        if (inp == null) {
            throw new IOException("Unable to find the R script " + rScriptName + " as a class resource of " + clazz);
        }
        return inp;
    }

    public String executeR(Map<String, String> commandLine) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add("R");
        command.add("--no-save");
        command.add("--no-restore");
        command.add("--no-readline");
        for (String s : commandLine.keySet()) {
            command.add(s + "=" + commandLine.get(s));
        }
        final Process process = new ProcessBuilder(command).start();
        final InputStream rDriverInput = getInputStreamForRScript(RProcessorBase.class, getRDriverName());
        final StringBuffer rExecutionOutput = new StringBuffer();
        final IOException[] exceptionArray = new IOException[2];
        Thread input = new Thread() {

            public void run() {
                try {
                    int read;
                    while ((read = rDriverInput.read()) != -1) {
                        process.getOutputStream().write(read);
                    }
                    process.getOutputStream().close();
                } catch (IOException iop) {
                    exceptionArray[0] = iop;
                    throw new RuntimeException("Problem with reading R Script, or writing to process", iop);
                }
            }
        };
        input.setDaemon(true);
        input.start();
        Thread output = new Thread() {

            public void run() {
                try {
                    rExecutionOutput.append(readStringFromInputStream(process.getInputStream()));
                } catch (IOException iop) {
                    exceptionArray[1] = iop;
                    throw new RuntimeException("Problem with reading result from R process", iop);
                }
            }
        };
        output.setDaemon(true);
        output.start();
        try {
            process.waitFor();
        } catch (InterruptedException inexp) {
            Thread.currentThread().interrupt();
        }
        process.getInputStream().close();
        process.getErrorStream().close();
        if (exceptionArray[0] != null) {
            throw exceptionArray[0];
        }
        if (exceptionArray[1] != null) {
            throw exceptionArray[1];
        }
        return rExecutionOutput.toString();
    }

    public abstract String getRDriverName();
}
