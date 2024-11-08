package org.jbjf.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import org.jbjf.core.AbstractTask;
import org.jbjf.tasks.JBJFTask.JBJFTaskStatus;

/**
 * <p>
 * <hr>
 * The <code>CopyFile</code> is a pre-built <code>AbstractTask</code>
 * that copies a file from one location/filename to a 
 * second location/filename.  The current version is quite limited
 * and only supports full/partial directory paths along with a 
 * filename.
 * But <code>CopyFile</code> can be
 * easily sub-classed (extended) to become adaptable for any copy
 * task, especially to integrate with the JBJF directories elements
 * for a more flexible copy task.
 * </p>
 * <p>
 * <h3>Dependencies:</h3>
 * <hr>
 * <ul>
 * <li>JBJF 1.3.2(+)</li>
 * <li>JRE/JDK 6(+)</li>
 * </ul>
 * </p>
 * <p>
 * <h3>Resources:</h3>
 * The <code>CopyFile</code> task expects the following &lt;resource&gt;
 * tags to be defined within the &lt;task&gt; XML element of the JBJF
 * Definition file:
 * <ul>
 * <li>source - Directory path and filename of the source location.</li>
 * <li>target - Directory path and filename of the target location.</li>
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
 *      <td width='20%'>Type</td>
 *      <td width='1%'>&nbsp;</td>
 *      <td width='8%'>Required</td>
 *      <td width='1%'>&nbsp;</td>
 *      <td>Description/Comments</td>
 *  </tr>
 * </thead>
 *  <tr valign='top'>
 *      <td>&lt;task&gt;</td>
 *      <td>&nbsp;</td>
 *      <td>source</td>
 *      <td>&nbsp;</td>
 *      <td>String</td>
 *      <td>&nbsp;</td>
 *      <td>True</td>
 *      <td>&nbsp;</td>
 *      <td>
 *      The absolute or relative directory path and filename that
 *      you wish to copy.
 *      </td>
 *  </tr>
 *  <tr valign='top'>
 *      <td>&lt;task&gt;</td>
 *      <td>&nbsp;</td>
 *      <td>backup-directory</td>
 *      <td>&nbsp;</td>
 *      <td>String</td>
 *      <td>&nbsp;</td>
 *      <td>True</td>
 *      <td>&nbsp;</td>
 *      <td>
 *      The absolute or relative directory path and filename that
 *      you wish to write out.
 *      </td>
 *  </tr>
 * </table>
 * </p>
 * <p>
 * The following is an example &lt;task&gt;:
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
 *         Copyright (C) 2007. JBJF All rights
 *         reserved.
 * @version 1.3.3
 * @since 1.0.0
 * </p>
 */
@JBJFTask(properties = "source,target", value = "Simple Task that will copy one file to another file/location", task_status = JBJFTaskStatus.READY)
public class CopyFile extends AbstractTask {

    /** Class property that stores the source path and/or file. */
    private String mstrSource;

    /** Class property that stores the target path and/or file. */
    private String mstrTarget;

    /**
     * Default constructor.
     */
    public CopyFile() {
        super();
        mtaskRequired = new ArrayList();
        getRequiredResources().add("source");
        getRequiredResources().add("target");
        setSubTask(false);
    }

    /**
     * Custom constructor.
     */
    public CopyFile(boolean subTask) {
        super();
        mtaskRequired = new ArrayList();
        getRequiredResources().add("source");
        getRequiredResources().add("target");
        setSubTask(subTask);
    }

    /**
     * The <code>CopyFile</code> task will expect at least two
     * resources that should be defined in the JBJF Batch Definition
     * XML file, a source filename and a target filename.  The following
     * is an example <code>CopyFile</code> task definition:
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
     */
    @Override
    public void runTask(HashMap pjobParameters) throws Exception {
        if (hasRequiredResources(isSubTask())) {
            File lfileSource = new File(getSource());
            File lfileTarget = new File(getTarget());
            FileChannel lfisInput = null;
            FileChannel lfosOutput = null;
            try {
                int mbCount = 64;
                boolean lblnDone = false;
                while (!lblnDone) {
                    lfisInput = new FileInputStream(lfileSource).getChannel();
                    lfosOutput = new FileOutputStream(lfileTarget).getChannel();
                    try {
                        int maxCount = (mbCount * 1024 * 1024) - (32 * 1024);
                        long size = lfisInput.size();
                        long position = 0;
                        while (position < size) {
                            position += lfisInput.transferTo(position, maxCount, lfosOutput);
                        }
                        lblnDone = true;
                    } catch (IOException lioXcp) {
                        getLog().warn(lioXcp);
                        if (lioXcp.getMessage().contains("Insufficient system resources exist to complete the requested servic")) {
                            mbCount--;
                            getLog().debug("Dropped resource count down to [" + mbCount + "]");
                            if (mbCount == 0) {
                                lblnDone = true;
                            }
                            if (lfisInput != null) {
                                lfisInput.close();
                            }
                            if (lfosOutput != null) {
                                lfosOutput.close();
                            }
                        } else {
                            throw lioXcp;
                        }
                    }
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

    @Override
    public void initTask(HashMap jobStack) throws Exception {
        super.initTask(jobStack);
        setSource(resolveResource("source"));
        setTarget(resolveResource("target"));
    }

    /**
     * Traditional getter method that returns the mstrSource path/file.
     * <p>
     * @return The mstrSource file/path.
     */
    public String getSource() {
        return mstrSource;
    }

    /**
     * Traditional setter method that sets the file/path.
     * <p>
     * @param source The Source path/file to set the class property.
     */
    public void setSource(String source) {
        this.mstrSource = source;
    }

    /**
     * Traditional getter method that returns the path/file of the
     * mstrTarget file.
     * <p>
     * @return The target path/file.
     */
    public String getTarget() {
        return mstrTarget;
    }

    /**
     * Traditional setter method that sets the path/file of where
     * the file should be copied.
     * <p>
     * @param Target path/file to set the class property.
     */
    public void setTarget(String target) {
        this.mstrTarget = target;
    }
}
