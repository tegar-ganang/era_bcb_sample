package org.jbjf.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import org.jbjf.core.AbstractTask;

/**
 * <p>
 * The <code>GUnzipFile</code> is a JBJF task class that will use
 * the Unix zip compression file format to unzip a file that
 * has been compressed with gzip.  The task is available as a sub-task,
 * as the <code>initTask()</code> handles all the initialization
 * of the class properties from the &lt;resource&gt; elements.  When
 * using this in a sub-task operational mode, make sure you use the
 * custom constructor <code>GUnzipFile ( boolean )</code>, passing
 * a true value in as the parameter.  This decouples the dependencies
 * of the <code>runTask()</code> to the required &lt;resource&gt; 
 * elements and forces you (as the sub-task parent) to populate
 * the class properties correctly.
 * </p>
 * <p>
 * <h3>Dependencies:</h3>
 * <ul>
 * <li>JBJF 1.3.2(+)</li>
 * <li>JRE/JDK 6(+)</li>
 * </ul>
 * <h3>Resources:</h3>
 * <code>GUnzipFile</code> depends on the following &lt;resource&gt; 
 * elements to function correctly:
 * <ul>
 * <li>source-filename - The name of the file to unzip.  This may
 * also contain directory path information.  If so, then don't 
 * include the &lt;resource&gt; for source-directory.</li>
 * <li>target-filename - The name of the destination file to unzip
 * into.  This may also contain directory path information.  If so, 
 * then don't include the &lt;resource&gt; for target-directory.</li>
 * <li>source-directory - An optional directory path for the source file 
 * to unzip.  Include this when you don't put path information in 
 * the source-filename &lt;resource&gt; element.</li>
 * <li>target-directory - An optional directory path for the target file 
 * location.  Include this when you don't put path information in 
 * the target-filename &lt;resource&gt; element.</li>
 * </ul>
 * </p>
 * <p>
 * <h3>Details</h3>
 * <hr>
 * <h4>Input Resources</h4>
 * <table border='1' width='100%'>
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
 *      <td>source-filename</td>
 *      <td>&nbsp;</td>
 *      <td>String</td>
 *      <td>&nbsp;</td>
 *      <td>True</td>
 *      <td>&nbsp;</td>
 *      <td>The name of the source file to unzip.  This may
 *      also contain directory path information.  If so, then don't 
 *      include the &lt;resource&gt; for source-directory.
 *      </td>
 *  </tr>
 *  <tr valign='top'>
 *      <td>&lt;task&gt;</td>
 *      <td>&nbsp;</td>
 *      <td>target-filename</td>
 *      <td>&nbsp;</td>
 *      <td>String</td>
 *      <td>&nbsp;</td>
 *      <td>True</td>
 *      <td>&nbsp;</td>
 *      <td>The name of the target file to unzip to.  This may
 *      also contain directory path information.  If so, then don't 
 *      include the &lt;resource&gt; for target-directory.
 *      </td>
 *  </tr>
 *  <tr valign='top'>
 *      <td>&lt;task&gt;</td>
 *      <td>&nbsp;</td>
 *      <td>source-directory</td>
 *      <td>&nbsp;</td>
 *      <td>String</td>
 *      <td>&nbsp;</td>
 *      <td>False</td>
 *      <td>&nbsp;</td>
 *      <td>
 *      The name of the source directory where the source-filename
 *      is located.  Include this when you don't put path information in 
 *      the source-filename &lt;resource&gt; element.
 *      </td>
 *  </tr>
 *  <tr valign='top'>
 *      <td>&lt;task&gt;</td>
 *      <td>&nbsp;</td>
 *      <td>target-directory</td>
 *      <td>&nbsp;</td>
 *      <td>String</td>
 *      <td>&nbsp;</td>
 *      <td>False</td>
 *      <td>&nbsp;</td>
 *      <td>
 *      The name of the target directory where you unzip the source
 *      file.  Include this when you don't put path information in 
 *      the target-filename &lt;resource&gt; element.
 *      </td>
 *  </tr>
 * </table>
 * </p>
 * <p>
 * The following is an example XML &lt;task&gt; element:
 * </p>
 * <p>
 * <pre>
 *     &lt;jbjf-tasks&gt;
 *         &lt;task name="t001" order="1" active="true"&gt;
 *             &lt;class&gt;org.jbjf.tasks.GUnzipFile&lt;/class&gt;
 *             &lt;resource type="source-filename"&gt;my-file.gz&lt;/resource&gt;
 *             &lt;resource type="target-filename"&gt;my-file.txt&lt;/resource&gt;
 *             &lt;resource type="source-directory"&gt;./inbound&lt;/resource&gt;
 *             &lt;resource type="target-directory"&gt;./outbound&lt;/resource&gt;
 *         &lt;/task&gt;
 * </pre>
 * </p>
 * <p>
 * @author Adym S. Lincoln<br>
 *         Copyright (C) 2007-2011. JBJF All rights reserved.
 * @version 1.3.3
 * @since   1.3.3
 * </p>
 */
public class GUnzipFile extends AbstractTask {

    /** 
     * Stores a fully qualified class name.  Used for debugging and 
     * auditing.
     * @since 1.0.0
     */
    public static final String ID = GUnzipFile.class.getName();

    /** 
     * Stores the class name, primarily used for debugging and so 
     * forth.  Used for debugging and auditing.
     * @since 1.0.0
     */
    private String SHORT_NAME = "GUnzipFile()";

    /** 
     * Stores a <code>SYSTEM IDENTITY HASHCODE</code>.  Used for
     * debugging and auditing.
     * @since 1.0.0
     */
    private String SYSTEM_IDENTITY = String.valueOf(System.identityHashCode(this));

    /**
     * Class property that stores the name of the tarfile you wish
     * to unpack.
     */
    private String sourceFilename = null;

    /**
     * Class property that stores the name of the filename you wish
     * to extract the contents to.  Please note, many tarfiles contain
     * multiple files in them.  In this event, the targetFilename
     * is ignored and the fielname(s) within the tarfile are used.
     */
    private String targetFilename = null;

    /**
     * Class property that stores the directory path where the source
     * file can be found.
     */
    private String sourceDirectory = null;

    /**
     * Class property that stores the directory path where you want 
     * the file(s) unpacked.
     */
    private String targetDirectory = null;

    /**
     * Default constructor.  Sets the required &lt;resource&gt;
     * elements.  
     */
    public GUnzipFile() {
        super();
        this.sourceDirectory = null;
        this.sourceFilename = null;
        this.targetDirectory = null;
        this.targetFilename = null;
        mtaskRequired = new ArrayList();
        getRequiredResources().add("source-filename");
        getRequiredResources().add("target-filename");
        setSubTask(false);
    }

    /**
     * Custom constructor.  Allows you to instantiate this Task
     * in a Sub-Task operational mode by passing "true" in for the
     * parameter.
     */
    public GUnzipFile(boolean subTask) {
        super();
        this.sourceDirectory = null;
        this.sourceFilename = null;
        this.targetDirectory = null;
        this.targetFilename = null;
        mtaskRequired = new ArrayList();
        getRequiredResources().add("source-filename");
        getRequiredResources().add("target-filename");
        setSubTask(subTask);
    }

    @Override
    public void runTask(HashMap pjobParameters) throws Exception {
        if (hasRequiredResources(isSubTask())) {
            String lstrSource = getSourceFilename();
            String lstrTarget = getTargetFilename();
            if (getSourceDirectory() != null) {
                lstrSource = getSourceDirectory() + File.separator + getSourceFilename();
            }
            if (getTargetDirectory() != null) {
                lstrTarget = getTargetDirectory() + File.separator + getTargetFilename();
            }
            GZIPInputStream lgzipInput = new GZIPInputStream(new FileInputStream(lstrSource));
            OutputStream lfosGUnzip = new FileOutputStream(lstrTarget);
            byte[] buf = new byte[1024];
            int len;
            while ((len = lgzipInput.read(buf)) > 0) lfosGUnzip.write(buf, 0, len);
            lgzipInput.close();
            lfosGUnzip.close();
        }
    }

    @Override
    public void initTask(HashMap jobStack) throws Exception {
        super.initTask(jobStack);
        setSourceFilename(resolveResource("source-filename"));
        setTargetFilename(resolveResource("target-filename"));
        if (getResources().containsKey("source-directory")) {
            setSourceDirectory(resolveResource("source-directory"));
        }
        if (getResources().containsKey("target-directory")) {
            setTargetDirectory(resolveResource("target-directory"));
        }
    }

    /**
     * <p>
     * Traditional getter() method that...
     * </p>
     * @return The sourceFilename
     */
    public String getSourceFilename() {
        return sourceFilename;
    }

    /**
     * <p>
     * Traditional setter() method that...
     * </p>
     * @param sourceFilename the sourceFilename to set
     */
    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    /**
     * <p>
     * Traditional getter() method that...
     * </p>
     * @return The targetFilename
     */
    public String getTargetFilename() {
        return targetFilename;
    }

    /**
     * <p>
     * Traditional setter() method that...
     * </p>
     * @param targetFilename the targetFilename to set
     */
    public void setTargetFilename(String targetFilename) {
        this.targetFilename = targetFilename;
    }

    /**
     * <p>
     * Traditional getter() method that...
     * </p>
     * @return The sourceDirectory
     */
    public String getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * <p>
     * Traditional setter() method that...
     * </p>
     * @param sourceDirectory the sourceDirectory to set
     */
    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    /**
     * <p>
     * Traditional getter() method that...
     * </p>
     * @return The targetDirectory
     */
    public String getTargetDirectory() {
        return targetDirectory;
    }

    /**
     * <p>
     * Traditional setter() method that...
     * </p>
     * @param targetDirectory the targetDirectory to set
     */
    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }
}
