package de.schlund.pfixcore.util;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import de.schlund.pfixxml.util.FileUtils;
import de.schlund.pfixxml.util.Xml;

/**
 * This ant task iterates over all statusmessage files included from a source directory 
 * and creates or merges changes into an according merge file in the destination directory.
 * Source and destination directory are used as base directories, i.e. a statusmessage file
 * in a source sub directory will produce a merge file in an according sub directory of
 * the destination directory. You additionally can define a filename suffix for this file,
 * which is added after the name but before the file extension.
 * 
 * @author mleidig@schlund.de
 */
public class MergeTask extends MatchingTask {

    private File srcDir;

    private File destDir;

    private String suffix;

    private String selection;

    /**
     * Iterate over all included files and create according merge file or,
     * if already existing, merge included file into the merge file. 
     */
    @Override
    public void execute() throws BuildException {
        int createNo = 0;
        int mergeNo = 0;
        if (srcDir.exists()) {
            DirectoryScanner scanner = getDirectoryScanner(srcDir);
            String[] files = scanner.getIncludedFiles();
            for (String file : files) {
                File srcFile = new File(srcDir, file);
                File destFile = new File(destDir, file);
                if (suffix != null) {
                    String name = destFile.getName();
                    int ind = name.indexOf('.');
                    if (ind > -1) {
                        name = name.substring(0, ind) + suffix + name.substring(ind);
                    } else throw new BuildException("Expected file name containing file extension: " + destFile.getAbsolutePath());
                    destFile = new File(destFile.getParentFile(), name);
                }
                boolean destExists = destFile.exists();
                if (!destExists) {
                    try {
                        if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();
                        FileUtils.copyFile(srcFile, destFile);
                    } catch (IOException x) {
                        throw new BuildException("Error copying statusmessages from '" + srcFile.getAbsolutePath() + "' to '" + destFile.getAbsolutePath() + "'.", x);
                    }
                    try {
                        Document doc = Xml.parseMutable(destFile);
                        addComment(doc, file);
                        Xml.serialize(doc, destFile, true, true);
                    } catch (Exception x) {
                        throw new BuildException("Error adding comment to statusmessages file '" + destFile.getAbsolutePath() + "'.", x);
                    }
                    createNo++;
                } else if (srcFile.lastModified() > destFile.lastModified()) {
                    try {
                        Merge merge = new Merge(srcFile, selection, destFile, false);
                        merge.run();
                    } catch (Exception x) {
                        throw new BuildException("Merging to file " + destFile.getAbsolutePath() + " failed.", x);
                    }
                    mergeNo++;
                }
            }
        }
        if (createNo > 0) log("Created " + createNo + " statusmessage merge file" + (createNo > 1 ? "s" : "") + ".");
        if (mergeNo > 0) log("Merged " + mergeNo + " statusmessage file" + (mergeNo > 1 ? "s" : "") + ".");
    }

    /**
     * Set source base directory (where originating statusmessages can be found).
     */
    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    /**
     * Set destination base directory (where merged statusmessage are written).
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Set optional filename suffix for merged statusmessage file.
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Set XPath selecting the nodes to be merged.
     */
    public void setSelection(String selection) {
        this.selection = selection;
    }

    /**
     * Insert a comment at the beginning of the statusmessage document.
     */
    private void addComment(Document doc, String mergeSource) {
        Comment comment = doc.createComment("\nThis file contains merged statusmessages from '" + mergeSource + "'.\n" + "You can modify this file to change the messages for exisiting statuscodes.\n" + "Adding new statuscodes has to be done within the originating module.\n");
        Element element = doc.getDocumentElement();
        if (element.hasChildNodes()) {
            element.insertBefore(comment, element.getFirstChild());
            element.insertBefore(doc.createTextNode(" \n \n"), element.getFirstChild());
        } else {
            element.appendChild(doc.createTextNode("\n"));
            element.appendChild(comment);
        }
    }
}
