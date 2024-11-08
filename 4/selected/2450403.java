package niskala.sej;

import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by IntelliJ IDEA.
 * User: mburton
 * Date: Aug 20, 2006
 * Time: 6:40:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class Chmod {

    private static final File[] EMPTY_FILES = new File[] {};

    private static final String[] EMPTY_STRINGS = new String[] {};

    protected File[] files = EMPTY_FILES;

    protected String[] options = EMPTY_STRINGS;

    protected String perms;

    protected String chmod = "/bin/chmod";

    public Chmod() {
    }

    public Chmod(String perms, File[] files) {
        this.files = files;
        this.perms = perms;
    }

    public void invoke() throws IOException {
        String[] command = new String[files.length + options.length + 2];
        command[0] = chmod;
        System.arraycopy(options, 0, command, 1, options.length);
        command[1 + options.length] = perms;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            command[2 + options.length + i] = file.getAbsolutePath();
        }
        Process p = Runtime.getRuntime().exec(command);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (p.exitValue() != 0) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(p.getErrorStream(), writer);
            throw new IOException("Unable to chmod files: " + writer.toString());
        }
    }

    public File[] getFiles() {
        return files;
    }

    public void setFiles(File[] files) {
        this.files = files;
        if (this.files == null) this.files = EMPTY_FILES;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
        if (this.options == null) this.options = EMPTY_STRINGS;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getChmod() {
        return chmod;
    }

    public void setChmod(String chmod) {
        this.chmod = chmod;
    }
}
