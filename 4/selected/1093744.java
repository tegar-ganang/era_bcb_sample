package org.jbjf.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import org.jbjf.core.AbstractTask;
import org.jbjf.util.JBJFFileFilter;

/**
 * <p>
 * The <code>CopyFilesLike</code> is similar in nature to the original
 * <code>CopyFile</code> task.  The <code>CopyFilesLike</code> task
 * takes an additional "filter" &lt;resource&gt; element that provides
 * a filtering of files and thus, all files that match the "filter"
 * will be copied.
 * </p>
 * <p>
 * <h3>Dependencies:</h3>
 * <ul>
 * <li>JBJF 1.3.0(+)</li>
 * <li>JRE/JDK 6(+)</li>
 * </ul>
 * <h3>Resources:</h3>
 * <code>CopyFilesLike</code> depends on the following &lt;resource&gt; 
 * elements to function correctly:
 * <ul>
 * <li>source - Directory path and filename of the source location.</li>
 * <li>target - Directory path and filename of the target location.</li>
 * <li>filter - A partial file name pattern to filter on.</li>
 * </ul>
 * </p>
 * <p>
 * <h3>Details</h3>
 * <hr>
 * <h4>Input Resources</h4>
 * <table border='1' width='65%'>
 * <thead>
 *  <tr>
 *      <td width='15%'>Location</td>
 *      <td width='1%'>&nbsp;</td>
 *      <td width='15%'>Id/Name</td>
 *      <td width='1%'>&nbsp;</td>
 *      <td width='25%'>Type</td>
 *      <td width='1%'>&nbsp;</td>
 *      <td width='10%'>Required</td>
 *      <td width='1%'>&nbsp;</td>
 *      <td>Description/Comments</td>
 *  </tr>
 * </thead>
 *  <tr valign='top'>
 *      <td>&lt;task&gt;</td>
 *      <td>&nbsp;</td>
 *      <td>type-attr</td>
 *      <td>&nbsp;</td>
 *      <td>String</td>
 *      <td>&nbsp;</td>
 *      <td>True</td>
 *      <td>&nbsp;</td>
 *      <td>The &lt;sql-definition&gt; in the JBJF Batch Definition
 *      file that contains the SQL statement to run.  The SQL statement
 *      is assumed to have no parameters, thus no substitution is
 *      performed prior to statement execution.
 *      </td>
 *  </tr>
 * </table>
 * </p>
 * <p>
 * The following is an example XML &lt;task&gt; element:
 * </p>
 * <p>
 * <pre>
 * &lt;jbjf-tasks>
 *     &lt;task name="tNNN" order="N" active="true"&gt;
 *         &lt;class>org.jbjf.tasks.CopyFile&lt;/class&gt;
 *         &lt;resource type="source"&gt;my-source-file.xml&lt;/resource&gt;
 *         &lt;resource type="target"&gt;my-target-file.xml&lt;/resource&gt;
 *     &lt;/task&gt;
 *     &lt;task name="two" order="2" active="false"&gt;
 * </pre>
 * </p>
 * <p>
 * @author Adym S. Lincoln<br>
 *         Copyright (C) 2007-2009. JBJF All rights reserved.
 * @version 1.3.0
 * @since   1.3.0
 * </p>
 */
public class CopyFilesLike extends AbstractTask {

    /** 
     * Stores a fully qualified class name.  Used for debugging and 
     * auditing.
     * @since 1.0.0
     */
    public static final String ID = CopyFilesLike.class.getName();

    /** 
     * Stores the class name, primarily used for debugging and so 
     * forth.  Used for debugging and auditing.
     * @since 1.0.0
     */
    private String SHORT_NAME = "CopyFilesLike()";

    /** 
     * Stores a <code>SYSTEM IDENTITY HASHCODE</code>.  Used for
     * debugging and auditing.
     * @since 1.0.0
     */
    private String SYSTEM_IDENTITY = String.valueOf(System.identityHashCode(this));

    /**
     * Default constructor.  Sets the required &lt;resource&gt;
     * elements.  
     */
    public CopyFilesLike() {
        mtaskRequired = new ArrayList();
        getRequiredResources().add("source");
        getRequiredResources().add("filter");
        getRequiredResources().add("target");
    }

    @Override
    public void runTask(HashMap jobStack) throws Exception {
        String lstrFilter = (String) getResources().get("filter");
        String lstrTarget = (String) getResources().get("target");
        String lstrSource = (String) getResources().get("source");
        String[] lstrFilesFound = null;
        lstrFilesFound = searchForFiles(lstrSource, lstrFilter);
        if (lstrFilesFound != null) {
            for (int i = 0; i < lstrFilesFound.length; i++) {
                getLog().debug("Found match [" + lstrSource + File.separator + lstrFilesFound[i] + "]");
                File lfileSource = new File(lstrSource + File.separator + lstrFilesFound[i]);
                File lfileTarget = new File(lstrTarget + File.separator + lstrFilesFound[i]);
                FileChannel lfisInput = null;
                FileChannel lfosOutput = null;
                try {
                    lfisInput = new FileInputStream(lfileSource).getChannel();
                    lfosOutput = new FileOutputStream(lfileTarget).getChannel();
                    int maxCount = (32 * 1024 * 1024) - (32 * 1024);
                    long size = lfisInput.size();
                    long position = 0;
                    while (position < size) {
                        position += lfisInput.transferTo(position, maxCount, lfosOutput);
                    }
                } finally {
                    if (lfisInput != null) {
                        lfisInput.close();
                    }
                    if (lfosOutput != null) {
                        lfosOutput.close();
                    }
                }
            }
        }
    }

    /**
     * Utility method that takes in a directory path and a file search
     * pattern and returns a <code>String[]</code> of filenames that
     * match the file filtering.
     * <p>
     * @param   pstrDirectory   String value of the directory/folder
     *                          to search.
     * @param   pstrPattern     Filename pattern.
     * @return                  Returns a <code>String[]</code> of files.
     */
    public String[] searchForFiles(String pstrDirectory, String pstrPattern) {
        getLog().debug("File Search Directory [" + pstrDirectory + "] for Pattern [" + pstrPattern + "]");
        File lfileDirectory = new File(pstrDirectory);
        String[] lstrResults;
        FilenameFilter filter = new JBJFFileFilter(pstrPattern);
        lstrResults = lfileDirectory.list(filter);
        if (lstrResults != null) {
            getLog().info("File Search Complete [" + lstrResults.length + "] files found");
        } else {
            getLog().info("File Search Complete [0] files found...Hope that's OK...");
        }
        return lstrResults;
    }
}
