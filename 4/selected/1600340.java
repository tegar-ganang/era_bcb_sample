package com.controltier.ctl.execution.script;

import com.controltier.ctl.common.Framework;
import java.io.*;

/**
 * Utility methods for writing temp files for scripts and setting file permissions.
 */
public class ScriptfileUtils {

    /**
     * Write an inputstream to a FileWriter
     * @param input input stream
     * @param writer writer
     * @throws IOException if an error occurs
     */
    private static void writeStream(final InputStream input, final FileWriter writer) throws IOException {
        final InputStreamReader inStream = new InputStreamReader(input);
        final BufferedReader inbuff = new BufferedReader(inStream);
        String inData;
        final String linesep = System.getProperty("line.separator");
        while ((inData = inbuff.readLine()) != null) {
            writer.write(inData);
            writer.write(linesep);
        }
        inbuff.close();
    }

    /**
     * Copy from a Reader to a FileWriter
     * @param reader reader
     * @param writer writer
     * @throws IOException if an error occurs
     */
    private static void writeReader(final Reader reader, final FileWriter writer) throws IOException {
        final BufferedReader inbuff = new BufferedReader(reader);
        String inData;
        final String linesep = System.getProperty("line.separator");
        while ((inData = inbuff.readLine()) != null) {
            writer.write(inData);
            writer.write(linesep);
        }
        inbuff.close();
    }

    /**
     * Copy a source file to a tempfile for script execution
     *
     * @param framework  framework
     * @param sourcefile source file
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(final Framework framework, final File sourcefile) throws IOException {
        return writeScriptTempfile(framework, new FileInputStream(sourcefile), null, null);
    }

    /**
     * Copy a source stream to a tempfile for script execution
     *
     * @param framework framework
     * @param stream    source
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(final Framework framework, final InputStream stream) throws IOException {
        return writeScriptTempfile(framework, stream, null, null);
    }

    /**
     * Copy string content to a tempfile for script execution
     *
     * @param framework framework
     * @param source    string content
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(final Framework framework, final String source) throws IOException {
        return writeScriptTempfile(framework, null, source, null);
    }

    /**
     * Copy reader content to a tempfile for script execution
     *
     * @param framework framework
     * @param source    string content
     *
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    public static File writeScriptTempfile(final Framework framework, final Reader source) throws IOException {
        return writeScriptTempfile(framework, null, null, source);
    }

    /**
     * Copy a source stream or string content to a tempfile for script execution
     *
     * @param framework framework
     * @param stream    source stream
     * @param source    content
     *
     * @param reader
     * @return tempfile
     *
     * @throws IOException if an error occurs
     */
    private static File writeScriptTempfile(final Framework framework, final InputStream stream, final String source, final Reader reader) throws IOException {
        final File scriptfile;
        scriptfile = File.createTempFile("ctl-exec", ".tmp", new File(framework.getProperty("framework.var.dir")));
        final FileWriter writer = new FileWriter(scriptfile);
        if (null != source) {
            writer.write(source);
        } else if (null != reader) {
            ScriptfileUtils.writeReader(reader, writer);
        } else if (null != stream) {
            ScriptfileUtils.writeStream(stream, writer);
        }
        writer.close();
        scriptfile.deleteOnExit();
        return scriptfile;
    }

    /**
     * Set the executable flag on a file if supported by the OS
     *
     * @param scriptfile target file
     *
     * @throws IOException if an error occurs
     */
    public static void setExecutePermissions(final File scriptfile) throws IOException {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            final Process process = Runtime.getRuntime().exec(new String[] { "chmod", "+x", scriptfile.getAbsolutePath() });
            int result = -1;
            try {
                result = process.waitFor();
            } catch (InterruptedException e) {
            }
            if (result > 0) {
                throw new IOException("exec returned: " + result);
            }
        }
    }
}
