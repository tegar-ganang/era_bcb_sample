package com.notuvy.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * A utlity to launch an external appliction
 *
 * @author  murali
 */
public class ProgramLauncher implements Runnable {

    protected static final Logger LOG = Logger.getLogger(ProgramLauncher.class);

    protected static String[] createCmdArray(String cmd) {
        ArrayList<String> list = new ArrayList<String>();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        StringBuffer buffer = new StringBuffer();
        for (char ch : cmd.toCharArray()) {
            if (inDoubleQuotes) {
                if (ch == '\"') {
                    inDoubleQuotes = false;
                } else {
                    buffer.append(ch);
                }
            } else if (inSingleQuotes) {
                if (ch == '\'') {
                    inSingleQuotes = false;
                } else {
                    buffer.append(ch);
                }
            } else {
                if (ch == '\"') {
                    inDoubleQuotes = true;
                } else if (ch == '\'') {
                    inSingleQuotes = true;
                } else if (Character.isWhitespace(ch)) {
                    if (buffer.length() > 0) {
                        list.add(buffer.toString());
                        buffer.delete(0, buffer.length());
                    }
                } else {
                    buffer.append(ch);
                }
            }
        }
        if (buffer.length() > 0) {
            list.add(buffer.toString());
        }
        return list.toArray(new String[list.size()]);
    }

    private final String vCommand;

    private boolean fBlocking = false;

    public ProgramLauncher(String pCommand) {
        vCommand = pCommand;
    }

    public String getCommand() {
        return vCommand;
    }

    public boolean isBlocking() {
        return fBlocking;
    }

    public void setBlocking(boolean pBlocking) {
        fBlocking = pBlocking;
    }

    public String toString() {
        return "ProgramLauncher[" + getCommand() + "]";
    }

    public void run() {
        LOG.debug(this);
        String[] parts = createCmdArray(getCommand());
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(parts);
            if (isBlocking()) {
                process.waitFor();
                StringWriter out = new StringWriter();
                IOUtils.copy(process.getInputStream(), out);
                String stdout = out.toString().replaceFirst("\\s+$", "");
                if (StringUtils.isNotBlank(stdout)) {
                    LOG.info("Process stdout:\n" + stdout);
                }
                StringWriter err = new StringWriter();
                IOUtils.copy(process.getErrorStream(), err);
                String stderr = err.toString().replaceFirst("\\s+$", "");
                if (StringUtils.isNotBlank(stderr)) {
                    LOG.error("Process stderr:\n" + stderr);
                }
            }
        } catch (IOException ioe) {
            LOG.error(String.format("Could not exec [%s]", getCommand()), ioe);
        } catch (InterruptedException ie) {
            LOG.error(String.format("Interrupted [%s]", getCommand()), ie);
        }
    }
}
