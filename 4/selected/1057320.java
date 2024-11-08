package org.deft.operation.linebreak.exporter.html;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.deft.operation.linebreak.debug.AbstractDebuggable;
import org.deft.operation.linebreak.exporter.html.writers.HtmlWriter;
import org.deft.operation.linebreak.exporter.html.writers.JsonWriter;
import org.deft.operation.linebreak.wrapper.options.LineWrapperOptions;
import org.deft.repository.ast.TreeNode;

/**
 * Example implementation of a basic HTML exporter with support
 * for dynamic line wrapping.
 * 
 * The principle export procedure has five distinct steps:
 * 1. Ensure that all target directories exist.
 * 2. Collect all required CSS style sheets and JavaScript files that are to be copied.
 * 3. Create the content for the JSON file used for data exchange and write it to disc.
 * 4. Create the content for the HTML file and write it to disc.
 * 5. Copy the required CSS style sheets and JavaScript files.
 * 
 * @author Christoph Seidl
 * 
 * @see JsonWriter
 * @see HtmlWriter
 */
public class HtmlExporter extends AbstractDebuggable {

    private static final String SOURCE_DIRECTORY = "res";

    private static final String CSS_SUBDIRECTORY = "css";

    private static final String JAVA_SCRIPT_SUBDIRECTORY = "javascript";

    private static final String JSON_SUBDIRECTORY = "snippets";

    private Map<TreeNode, String> snippetIDs;

    private List<String> relativeCssFilenames;

    private List<String> relativeJavaScriptFilenames;

    private JsonWriter jsonWriter;

    private HtmlWriter htmlWriter;

    /**
	 * Creates a new <code>HtmlExporter</code>.
	 */
    public HtmlExporter() {
        snippetIDs = new HashMap<TreeNode, String>();
        relativeCssFilenames = new LinkedList<String>();
        relativeJavaScriptFilenames = new LinkedList<String>();
        jsonWriter = new JsonWriter(false);
        htmlWriter = new HtmlWriter(false);
    }

    /**
	 * Initializes the exporter.
	 */
    private void initialize() {
        snippetIDs.clear();
        relativeCssFilenames.clear();
        relativeJavaScriptFilenames.clear();
    }

    /**
	 * Collects all relevant CSS style sheets that are used during the export.
	 */
    private void collectRelativeCssFilenames() {
        relativeCssFilenames.add("style.css");
    }

    /**
	 * Collects all relevant JavaScript files that are used during the export.
	 */
    private void collectRelativeJavaScriptFilenames() {
        relativeJavaScriptFilenames.add("json_parse.js");
        relativeJavaScriptFilenames.add("Exporter.js");
        relativeJavaScriptFilenames.add("Caret.js");
        relativeJavaScriptFilenames.add("DefaultLineWrappingStrategy.js");
        relativeJavaScriptFilenames.add("LineWrapper.js");
        relativeJavaScriptFilenames.add("DynamicFormatter.js");
    }

    /**
	 * Creates unique IDs for all snippets that are exported so that
	 * they can be referenced. This ID is for HTML elements and in the
	 * JSON file alike. 
	 * 
	 * @param snippets A list of all the snippets that are to be exported.
	 */
    private void createSnippetIDs(List<TreeNode> snippets) {
        int currentID = 0;
        Iterator<TreeNode> iterator = snippets.iterator();
        while (iterator.hasNext()) {
            TreeNode snippet = iterator.next();
            String snippetID = "snippet_" + currentID;
            snippetIDs.put(snippet, snippetID);
            ++currentID;
        }
    }

    /**
	 * Helper method to create "complete" relative paths from local paths.
	 * This attributes for the fact that local file system might have another structure
	 * than the one after the export. Especially the relation to the HTML file
	 * makes this necessary.
	 * 
	 * @param baseDirectory The base directory to use. May be blank.
	 * @param subdirectory Sub-directory for special kind of files, e.g. CSS style sheets. May be blank.
	 * @param relativeFilename The filename without any path information.
	 * 
	 * @return The "complete" relative path.
	 */
    private String createCompleteFilename(String baseDirectory, String subdirectory, String relativeFilename) {
        String output = "";
        if (baseDirectory != "") {
            output += baseDirectory + "\\";
        }
        if (subdirectory != "") {
            output += subdirectory + "\\";
        }
        output += relativeFilename;
        return output;
    }

    /**
	 * Creates all the data required for writing the HTML file.
	 * 
	 * @param snippets A list of all snippets that are to be exported.
	 * @param destinationDirectory The directory the HTML file will be written to.
	 * @param relativeJsonFilenames A list of the relative filenames of the JSON files used for data exchange.
	 * 
	 * @return A string containing the contents of the HTML file.
	 */
    private String createHtmlData(List<TreeNode> snippets, String destinationDirectory, List<String> relativeJsonFilenames) {
        List<String> completeJsonFilenames = new LinkedList<String>();
        Iterator<String> iterator3 = relativeJsonFilenames.iterator();
        while (iterator3.hasNext()) {
            String completeFilename = createCompleteFilename("", JSON_SUBDIRECTORY, iterator3.next());
            completeJsonFilenames.add(completeFilename);
        }
        List<String> destinationCssFileNames = new LinkedList<String>();
        Iterator<String> iterator = relativeCssFilenames.iterator();
        while (iterator.hasNext()) {
            String relativeCssFile = iterator.next();
            destinationCssFileNames.add(createCompleteFilename("", CSS_SUBDIRECTORY, relativeCssFile));
        }
        List<String> destinationJavaScriptFileNames = new LinkedList<String>();
        Iterator<String> iterator2 = relativeJavaScriptFilenames.iterator();
        while (iterator2.hasNext()) {
            String relativeCssFile = iterator2.next();
            destinationJavaScriptFileNames.add(createCompleteFilename("", JAVA_SCRIPT_SUBDIRECTORY, relativeCssFile));
        }
        return htmlWriter.writeHtml(completeJsonFilenames, snippets, snippetIDs, destinationCssFileNames, destinationJavaScriptFileNames);
    }

    /**
	 * Ensures that all the directories required for output exist.
	 * 
	 * @param destinationDirectory The path of the directory the HTML file is exported to.
	 */
    private void createDestinationDirectories(String destinationDirectory) {
        File jsonDirectory = new File(createCompleteFilename(destinationDirectory, JSON_SUBDIRECTORY, ""));
        File cssDirectory = new File(createCompleteFilename(destinationDirectory, CSS_SUBDIRECTORY, ""));
        File javaScriptDirectory = new File(createCompleteFilename(destinationDirectory, JAVA_SCRIPT_SUBDIRECTORY, ""));
        jsonDirectory.mkdirs();
        cssDirectory.mkdirs();
        javaScriptDirectory.mkdirs();
    }

    /**
	 * Writes the specified content to a file.
	 * 
	 * @param pathAndFilename The path and filename of the file to be written.
	 * @param content The content that will be written in that file.
	 * 
	 * @throws IOException Throws an exception if writing could not be completed.
	 */
    private void writeFile(String pathAndFilename, String content) throws IOException {
        FileWriter fstream = new FileWriter(pathAndFilename);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(content);
        out.close();
    }

    /**
	 * Copies a source file to a destination file.
	 * 
	 * @param source The file to be copied.
	 * @param destination The destination for the copy.
	 * 
	 * @throws IOException Throws an exception if copying could not be completed.
	 */
    public static void copyFile(File source, File destination) throws IOException {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(destination).getChannel();
            long size = in.size();
            MappedByteBuffer buffer = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buffer);
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    /**
	 * Copies a list of files to a destination directory keeping the filenames.
	 * 
	 * @param relativeFilenames A list of the files to be copied.
	 * @param destinationDirectory The directory the files are copied to.
	 * @param subdirectory The sub-directory the files reside in and should also be placed in the destination directory.
	 * 
	 * @throws IOException Throws an exception if copying could not be completed.
	 */
    private void copyFiles(List<String> relativeFilenames, String destinationDirectory, String subdirectory) throws IOException {
        Iterator<String> iterator = relativeFilenames.iterator();
        while (iterator.hasNext()) {
            String relativeFilename = iterator.next();
            String sourcePathAndfilename = createCompleteFilename(SOURCE_DIRECTORY, subdirectory, relativeFilename);
            String destinationPathAndFilename = createCompleteFilename(destinationDirectory, subdirectory, relativeFilename);
            copyFile(new File(sourcePathAndfilename), new File(destinationPathAndFilename));
        }
    }

    /**
	 * Exports a list of snippets to HTML in a destination directory.
	 * 
	 * @param snippets A list of snippets to export.
	 * @param lineWrapperOptions The options used for export.
	 * @param destinationDirectory The directory the HTML file is written to.
	 * @param relativeHtmlFilename The name of the HTML file.
	 */
    public void export(List<TreeNode> snippets, List<LineWrapperOptions> lineWrapperOptions, String destinationDirectory, String relativeHtmlFilename) {
        if (snippets.size() != lineWrapperOptions.size()) {
            logError("Number of snippets does not match the number of line wrapper options.");
            return;
        }
        initialize();
        String relativeJsonBaseFilename = relativeHtmlFilename.substring(0, relativeHtmlFilename.lastIndexOf("."));
        collectRelativeCssFilenames();
        collectRelativeJavaScriptFilenames();
        createSnippetIDs(snippets);
        List<String> relativeJsonFilenames = new LinkedList<String>();
        try {
            createDestinationDirectories(destinationDirectory);
            Iterator<TreeNode> iterator = snippets.iterator();
            Iterator<LineWrapperOptions> iterator2 = lineWrapperOptions.iterator();
            while (iterator.hasNext() && iterator2.hasNext()) {
                TreeNode currentSnippet = iterator.next();
                LineWrapperOptions currentLineWrapperOptions = iterator2.next();
                String currentSnippetID = snippetIDs.get(currentSnippet);
                assert (currentSnippetID != null);
                if (currentSnippetID == null) {
                    logError("Snippet ID is missing!");
                    return;
                }
                String jsonData = jsonWriter.writeJson(currentSnippet, currentSnippetID, currentLineWrapperOptions);
                String relativeJsonFilename = relativeJsonBaseFilename + "_" + currentSnippetID + ".json";
                relativeJsonFilenames.add(relativeJsonFilename);
                String destinationJsonFilename = createCompleteFilename(destinationDirectory, JSON_SUBDIRECTORY, relativeJsonFilename);
                writeFile(destinationJsonFilename, jsonData);
            }
            String htmlData = createHtmlData(snippets, destinationDirectory, relativeJsonFilenames);
            String destinationHtmlFilename = createCompleteFilename(destinationDirectory, "", relativeHtmlFilename);
            writeFile(destinationHtmlFilename, htmlData);
            copyFiles(relativeCssFilenames, destinationDirectory, CSS_SUBDIRECTORY);
            copyFiles(relativeJavaScriptFilenames, destinationDirectory, JAVA_SCRIPT_SUBDIRECTORY);
        } catch (IOException e) {
            logError("Export failed!");
        }
    }
}
