package de.str.prettysource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Default {@link OutputFormat} implementation.
 * 
 * @author Denny.Strietzbaum
 */
public class DefaultOutputFormat implements OutputFormat {

    private Map<String, OutputNode> outputNodeMap = null;

    private InputFormat inpFormat = null;

    /**
	 * Constructor. Creates a standard {@link InputFormat} instance and uses the passed tree of
	 * {@link InputNode} instances for that parser.
	 * 
	 * @param inputRootNode
	 *            tree of {@link InputNode} instances
	 */
    public DefaultOutputFormat(InputNode inputRootNode) {
        this.outputNodeMap = new HashMap<String, OutputNode>();
        this.inpFormat = new InputFormat(inputRootNode, this);
    }

    public void printHead(PrintWriter printer) {
    }

    public void printNewlineStart(PrintWriter printer) {
    }

    public void printTail(PrintWriter printer) {
    }

    public void setOutputNode(String inputNodeId, OutputNode out) {
        this.outputNodeMap.put(inputNodeId, out);
    }

    public OutputNode getOutputNode(String inputNodeId) {
        return this.outputNodeMap.get(inputNodeId);
    }

    public void format(File source, File target) {
        if (!source.exists()) {
            throw new IllegalArgumentException("Source '" + source + " doesn't exist");
        }
        if (!source.isFile()) {
            throw new IllegalArgumentException("Source '" + source + " is not a file");
        }
        target.mkdirs();
        String fileExtension = source.getName().substring(source.getName().lastIndexOf(".") + 1);
        String _target = source.getName().replace(fileExtension, "html");
        target = new File(target.getPath() + "/" + _target);
        try {
            Reader reader = new FileReader(source);
            Writer writer = new FileWriter(target);
            this.format(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String format(String input) {
        Reader reader = new StringReader(input);
        Writer writer = new StringWriter();
        this.format(reader, writer);
        return writer.toString();
    }

    public void format(Reader source, Writer target) {
        this.inpFormat.parse(source, target);
    }

    /**
	 * Returns the underlying {@link InputFormat}.
	 * 
	 * @return {@link InputFormat}
	 */
    public InputFormat getInputFormat() {
        return this.inpFormat;
    }
}
