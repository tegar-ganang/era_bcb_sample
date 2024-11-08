package de.str.prettysource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Base formatter for any kind of source -> target transformations. This formatter always uses a
 * {@link SourceSelection} (or better: a {@link SourceSelectionProcessor}) to process the current
 * content. The type of {@link SourceSelection} which currently is active depends on configured
 * properties according a start and end expression. These both are given as regular expression and
 * clearly define the selection bounds for this selection within the documents text content.
 * Different types of {@link SourceSelection} may be used per line. The state of an selection can be
 * active, inactive or shadowed by another higher prioritized selection. But at the end there's
 * always only one selection active.
 * 
 * @author Denny.Strietzbaum
 */
public class SourceFormat {

    protected int lineCount = 0;

    protected String sourceLine = null;

    protected String remainingLine = null;

    protected StringBuilder targetLine = null;

    protected long startTime = 0;

    protected Stack<SourceSelectionProcessor> selectionStack = null;

    private SourceSelection rootSelection = null;

    public SourceFormat(SourceSelection selections) {
        this.rootSelection = selections;
        SourceSelectionProcessor root = createProcessors(this.rootSelection);
        this.selectionStack = new Stack<SourceSelectionProcessor>();
        this.selectionStack.push(root);
    }

    public static SourceSelection createRootSelection() {
        return new SourceSelection(null, null);
    }

    /**
	 * Returns the root selection for this source document. This one has the whole document selected
	 * but child selections are added via {@link SourceSelection#addChild(SourceSelection)}
	 * 
	 * @return {@link SourceSelection} which selects the whole document
	 */
    public SourceSelection getSourceSelection() {
        return this.rootSelection;
    }

    /**
	 * Creates the hierarchy of all {@link SourceSelectionProcessor} for this document. The passed
	 * sourceselection ought to be the root selection which selects the whole document. For this
	 * selection as well as each child selection an appropriate {@link SourceSelectionProcessor} is
	 * created.
	 * 
	 * @param sourceSelection
	 *            root selection which selects the full document
	 * @return {@link SourceSelectionProcessor}
	 */
    private SourceSelectionProcessor createProcessors(SourceSelection sourceSelection) {
        SourceSelectionProcessor result = null;
        if (sourceSelection.getAllChilds().isEmpty()) {
            result = new SourceSelectionProcessor(sourceSelection);
        } else {
            List<SourceSelectionProcessor> subElements = new ArrayList<SourceSelectionProcessor>();
            for (SourceSelection subnode : sourceSelection.getAllChilds()) {
                SourceSelectionProcessor se = createProcessors(subnode);
                subElements.add(se);
            }
            result = new ParentSourceSelectionProcessor(sourceSelection, subElements);
        }
        return result;
    }

    /**
	 * Processes content from one file and writes html markup into another file.
	 * 
	 * @param source
	 *            file which provides the source
	 * @param target
	 *            file which is used for generated html markup
	 */
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

    /**
	 * Processes content from one Reader and writes html markup into a Writer.
	 * 
	 * @param source
	 *            reader which provides the source
	 * @param target
	 *            writer which is used for generated html markup
	 */
    public void format(Reader source, Writer target) {
        this.startTime = System.currentTimeMillis();
        BufferedReader reader = new BufferedReader(source);
        PrintWriter printer = new PrintWriter(new BufferedWriter(target));
        this.lineCount = 0;
        this.beforeFormatingSource(printer);
        try {
            while (reader.ready()) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                this.sourceLine = line;
                this.remainingLine = line;
                this.targetLine = new StringBuilder();
                this.lineCount++;
                this.beforeFormatingLine(printer);
                while (this.remainingLine != null && this.remainingLine.length() > 0) {
                    SourceSelectionProcessor foMode = this.selectionStack.peek();
                    foMode.process(this, null);
                }
                printer.println(this.targetLine.toString());
            }
            this.afterFormatingSource(printer);
            printer.flush();
            printer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Generates html markup from java source code.
	 * 
	 * @param input
	 *            java source code (with or without linebreaks)
	 * @return generated html
	 */
    public String format(String input) {
        Reader source = new StringReader(input);
        Writer target = new StringWriter();
        this.format(source, target);
        return target.toString();
    }

    /**
	 * Gets called before source formatting starts. Use this when a header shall be printed out.
	 * 
	 * @param printer
	 *            The printer which is used for target output
	 */
    protected void beforeFormatingSource(PrintWriter printer) {
    }

    /**
	 * Gets called before a new line gets formatted. Can be used to print out a line number.
	 * 
	 * @param printer
	 *            The printer which is used for target output
	 */
    protected void afterFormatingSource(PrintWriter printer) {
    }

    /**
	 * Gets called after source formatting has finished. Use this when a footer shall be printed.
	 * 
	 * @param printer
	 *            The printer which is used for target output
	 */
    protected void beforeFormatingLine(PrintWriter printer) {
    }
}
