package niskala.sej;

import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

/**
 * Write a java shebang (hash-bang) line to the start of an output
 * stream.  Useful for making jar files executable.
 *
 * Assumes the location of java can be known at the time the jarfile
 * is written.
 */
public class JarInterpretted {

    protected OutputStream outputStream;

    protected String interpretter = "/usr/bin/java";

    protected String[] args = new String[] { "-jar" };

    protected boolean allowUnlimitedArgs = false;

    public JarInterpretted() {
    }

    public JarInterpretted(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public String getInterpretter() {
        return interpretter;
    }

    public void setInterpretter(String interpretter) {
        this.interpretter = interpretter;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public boolean isAllowUnlimitedArgs() {
        return allowUnlimitedArgs;
    }

    public void setAllowUnlimitedArgs(boolean allowUnlimitedArgs) {
        this.allowUnlimitedArgs = allowUnlimitedArgs;
    }

    void write() throws IOException {
        if (!allowUnlimitedArgs && args != null && args.length > 1) throw new IllegalArgumentException("Only one argument allowed unless allowUnlimitedArgs is enabled");
        String shebang = "#!" + interpretter;
        for (int i = 0; i < args.length; i++) {
            shebang += " " + args[i];
        }
        shebang += '\n';
        IOUtils.copy(new StringReader(shebang), outputStream);
    }
}
