package net.sf.ovanttasks.ovanttasks;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.swing.ImageIcon;
import net.charabia.jsmoothgen.pe.PEFile;
import net.charabia.jsmoothgen.pe.res.ResIcon;
import net.charabia.util.codec.IcoCodec;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;

public abstract class AbstractStubTask extends Task {

    public class Execute {

        public void addText(String s) {
            execute = getProject().replaceProperties(s.trim());
        }
    }

    public class WorkingDirectory {

        public void addText(String s) {
            workingDirectory = getProject().replaceProperties(s.trim());
        }
    }

    private static String escape(String s) {
        int i = s.length();
        StringBuffer stringbuffer = new StringBuffer(i * 2);
        for (int j = 0; j < i; ) {
            char c = s.charAt(j++);
            switch(c) {
                case '\"':
                    stringbuffer.append("\\\"");
                    break;
                default:
                    stringbuffer.append(c);
                    break;
            }
        }
        return stringbuffer.toString();
    }

    public static String prepareUnixCommand(String s) {
        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        ProjectHelper.parsePropertyString(s, fragments, propertyRefs);
        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();
        String fragment;
        for (; i.hasMoreElements(); sb.append(fragment)) {
            fragment = (String) i.nextElement();
            if (fragment == null) {
                String propertyName = (String) j.nextElement();
                if (propertyName.startsWith("app:")) {
                    propertyName = propertyName.substring(4, propertyName.length()).toLowerCase();
                    if (propertyName.equals("name") || propertyName.equals("absolutename") || propertyName.equals("path") || propertyName.equals("absolutepath")) {
                        fragment = "${" + propertyName + "}";
                    } else {
                        throw new BuildException("\"app:\" token " + propertyName + " is unknown. Valid \"app:\" tokens are \"name\", \"absolutename\", \"path\" and \"absolutepath\"");
                    }
                } else if (propertyName.startsWith("env:")) {
                    propertyName = propertyName.substring(4, propertyName.length());
                    if (propertyName.indexOf('|') != -1) {
                        fragment = "${" + propertyName.substring(0, propertyName.indexOf('|')) + ":-" + propertyName.substring(propertyName.indexOf('|') + 1) + "}";
                    } else {
                        fragment = "${" + propertyName + "}";
                    }
                } else {
                    fragment = "${" + propertyName + "}";
                }
            }
        }
        return escape(sb.toString());
    }

    protected File archive = null;

    protected String execute = null;

    protected File iconFile = null;

    protected boolean isConsole = false;

    protected String mode = null;

    protected File output = null;

    protected String workingDirectory = "";

    protected void copy(File file, OutputStream out) throws IOException {
        copy(new FileInputStream(file), out);
    }

    protected void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read = 0;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
    }

    public Execute createExecute() {
        if (execute != null) {
            throw new BuildException("Property \"execute\" is always defined. You have to use either <execute> or the property attribute of task sfx");
        }
        return new Execute();
    }

    protected void createShellStub(Properties p) {
        try {
            FileOutputStream out = new FileOutputStream(output);
            byte scanBuffer[] = new byte[getTokenLength()];
            InputStream in = getShellStub();
            in.read(scanBuffer, 0, scanBuffer.length);
            long position = 1;
            while (p.size() > 0) {
                while (!p.containsKey(new String(scanBuffer))) {
                    out.write(scanBuffer[0]);
                    int nextb = in.read();
                    if (nextb == -1) {
                        throw new BuildException("input empty");
                    }
                    position++;
                    shiftArray(scanBuffer);
                    scanBuffer[scanBuffer.length - 1] = (byte) nextb;
                }
                String value = p.getProperty(new String(scanBuffer));
                p.remove(new String(scanBuffer));
                log(new String(scanBuffer) + " at " + (position - 1) + " replaced by " + value);
                out.write(value.getBytes());
                in.read(scanBuffer, 0, scanBuffer.length);
            }
            out.write(scanBuffer);
            copy(in, out);
            if (iconFile != null) {
                log("Setting the Icon for unix executables is not supported.", Project.MSG_INFO);
            }
            copy(archive, out);
            out.close();
        } catch (FileNotFoundException e) {
            throw new BuildException("Cannot open archive", e);
        } catch (IOException e) {
            throw new BuildException("IO Problem occured", e);
        }
    }

    protected void createWin32Stub(Properties p) {
        try {
            FileOutputStream out = new FileOutputStream(output);
            byte scanBuffer[] = new byte[getTokenLength()];
            File tmpExe = null;
            if (iconFile != null) {
                try {
                    tmpExe = File.createTempFile(output.getName(), "tmp");
                    tmpExe.deleteOnExit();
                    FileOutputStream _out = new FileOutputStream(tmpExe);
                    copy(isConsole ? getWin32ConsoleStub() : getWin32ConsoleLessStub(), _out);
                    _out.close();
                    PEFile peFile = new PEFile(tmpExe);
                    peFile.open();
                    ImageIcon ii = null;
                    if (iconFile.getName().toLowerCase().endsWith(".ico")) {
                        ii = new ImageIcon(IcoCodec.loadImages(iconFile)[0]);
                    } else {
                        ii = new ImageIcon(iconFile.getAbsolutePath());
                    }
                    Image img = ii.getImage().getScaledInstance(32, 32, Image.SCALE_REPLICATE);
                    while (img.getHeight(null) == -1) {
                        Thread.sleep(50);
                    }
                    ResIcon resIcon = new ResIcon(img);
                    peFile.replaceDefaultIcon(resIcon);
                    tmpExe = File.createTempFile(output.getName(), "tmp.patched");
                    tmpExe.deleteOnExit();
                    peFile.dumpTo(tmpExe);
                    peFile.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new BuildException("Replacing the default icon on executable " + output + " failed", ex);
                }
            }
            try {
                InputStream in = tmpExe != null ? new FileInputStream(tmpExe) : (isConsole ? getWin32ConsoleStub() : getWin32ConsoleLessStub());
                in.read(scanBuffer, 0, scanBuffer.length);
                long position = 1;
                while (p.size() > 0) {
                    while (!p.containsKey(new String(scanBuffer))) {
                        String s = new String(scanBuffer);
                        out.write(scanBuffer[0]);
                        int nextb = in.read();
                        if (nextb < 0) {
                            throw new BuildException("Strem empty");
                        }
                        position++;
                        shiftArray(scanBuffer);
                        scanBuffer[scanBuffer.length - 1] = (byte) nextb;
                    }
                    String value = p.getProperty(new String(scanBuffer));
                    p.remove(new String(scanBuffer));
                    log(new String(scanBuffer) + " at " + (position - 1) + " replaced by " + value);
                    out.write(value.getBytes());
                    for (int j = value.getBytes().length; j < 128; j++) {
                        out.write(0);
                    }
                    in.skip(128 - getTokenLength());
                    position += 128 - getTokenLength();
                    in.read(scanBuffer, 0, scanBuffer.length);
                }
                out.write(scanBuffer);
                copy(in, out);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            copy(archive, out);
            out.close();
        } catch (FileNotFoundException e) {
            throw new BuildException("Cannot open archive", e);
        } catch (IOException e) {
            throw new BuildException("IO Problem occured", e);
        }
    }

    public WorkingDirectory createWorkingDirectory() {
        if (workingDirectory.length() > 0) {
            throw new BuildException("Property \"workingdirectory\" is always defined. You have to use either <workdingdirectory> or the property attribute of task sfx");
        }
        return new WorkingDirectory();
    }

    @Override
    public void execute() {
        if (archive == null) {
            throw new BuildException("archive attribute not set");
        }
        if (execute == null) {
            throw new BuildException("execute attribute not set");
        }
        if (mode == null) {
            throw new BuildException("mode attribute not set");
        }
        if (output == null) {
            throw new BuildException("output attribute not set");
        }
    }

    protected abstract InputStream getShellStub();

    protected abstract int getTokenLength();

    protected abstract InputStream getWin32ConsoleLessStub();

    protected abstract InputStream getWin32ConsoleStub();

    public void setArchive(File archive) {
        this.archive = archive;
    }

    public void setExecute(String execute) {
        if (this.execute != null) {
            throw new BuildException("Property \"execute\" is always defined. You have to use either <execute> or the property attribute of task sfx");
        }
        if (execute.length() >= 128) {
            throw new BuildException("Property \"execute\" is too long (max 128 chars). Please contact the developers if you need this!");
        }
        this.execute = execute;
    }

    public void setIcon(File iconFile) {
        this.iconFile = iconFile;
    }

    public void setMode(String mode) {
        int consoleIndex = mode.indexOf("-console");
        if (consoleIndex > 0) {
            this.mode = mode.substring(0, consoleIndex);
            isConsole = true;
        } else {
            isConsole = false;
            this.mode = mode;
        }
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public void setWorkingDirectory(String workingDirectory) {
        if (this.workingDirectory.length() > 0) {
            throw new BuildException("Property \"workingdirectory\" is always defined. You have to use either <workdingdirectory> or the property attribute of task jstub");
        }
        this.workingDirectory = workingDirectory;
    }

    protected void shiftArray(byte[] array) {
        for (int i = 0; i < (array.length - 1); i++) {
            array[i] = array[i + 1];
        }
        array[array.length - 1] = 0;
    }
}
