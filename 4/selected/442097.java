package net.sourceforge.xhtmldoclet.pages;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import net.sourceforge.xhtmldoclet.AbstractPageWriter;
import net.sourceforge.xhtmldoclet.Doclet;

/**
 * Creates "help-doc.html", a page for explaining the javadoc layout.
 */
public final class Help extends AbstractPageWriter {

    /**
	 * Generate "help-doc.html" and handle errors.
	 */
    public static void generateHelp() {
        if (CONF.nohelp) return;
        String filename = "help-doc" + CONF.ext;
        try {
            new Help(filename);
        } catch (IOException exc) {
            throw Doclet.exception(exc, filename);
        }
    }

    /**
	 * Initialize and create help page with the given filename. Copy input from
	 * either the default or user-specified help file. Reads data from the
	 * specified file and writes it directly to the help file, within the same
	 * document structure, inside #Content.
	 * 
	 * @param filename The desired name of the file (with extension).
	 * @throws IOException If {@link java.io.FileOutputStream} creation fails.
	 */
    private Help(String filename) throws IOException {
        super(filename);
        pageType = PageType.HELP;
        windowTitle = (CONF.windowtitle.length() > 0) ? CONF.windowtitle : CONF.propertyText("Help");
        printXhtmlHeader();
        InputStreamReader stream;
        if (!CONF.helpfile.equals("")) stream = new InputStreamReader(new FileInputStream(CONF.helpfile)); else stream = new InputStreamReader(AbstractPageWriter.class.getResourceAsStream("resources/help" + CONF.ext));
        char[] buf = new char[Doclet.BUFFER_SIZE];
        int n;
        while ((n = stream.read(buf)) > 0) write(buf, 0, n);
        stream.close();
        println();
        printXhtmlFooter();
        this.close();
    }

    /** Highlight "Help" as current section, don't create link. */
    protected void navLinkHelp() {
        println(listItemCurrent(HELP));
    }
}
