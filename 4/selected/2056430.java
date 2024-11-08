package net.sourceforge.javautil.developer.ui.cli;

import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import javax.swing.table.TableModel;
import net.sourceforge.javautil.developer.ui.command.set.DeveloperCommandContext;
import net.sourceforge.javautil.developer.ui.command.set.web.WebApplicationContext;
import net.sourceforge.javautil.ui.cli.CommandLineContext;
import net.sourceforge.javautil.ui.cli.ICommandLineInterface;

/**
 * The command line context for the developer CLI.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: DeveloperCLIContext.java 2586 2010-11-15 04:14:32Z ponderator $
 */
public class DeveloperCLIContext extends DeveloperCommandContext<DeveloperCLI> implements CommandLineContext<DeveloperCLI> {

    protected CommandLineContext parent;

    protected Reader reader;

    protected PrintStream writer;

    public DeveloperCLIContext(DeveloperCLI cli, CommandLineContext<ICommandLineInterface> parent) {
        this(cli, parent.getReader(), parent.getWriter());
        this.parent = parent;
    }

    public DeveloperCLIContext(DeveloperCLI cli, Reader reader, PrintStream writer) {
        super(cli);
        this.reader = reader;
        this.writer = writer;
    }

    /**
	 * @return The parent command line context, or null if this is the 'root' context
	 */
    public CommandLineContext getParent() {
        return parent;
    }

    public PrintStream getWriter() {
        return this.writer;
    }

    public void setWriter(PrintStream writer) {
        this.writer = writer;
    }

    public Reader getReader() {
        return this.reader;
    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }
}
