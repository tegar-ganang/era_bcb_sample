package org.jbjf.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.jbjf.core.AbstractTask;
import org.jbjf.plugin.IJBJFPluginCipher;
import org.jbjf.util.APILog4j;
import org.jbjf.xml.JBJFFTPDefinition;
import org.jbjf.xml.JBJFPluginDefinition;

/**
 * <p>
 * The <code>FTPPushFile</code> class is a pre-defined <code>AbstractTask</code>
 * that will "push" a local file to a remote server.  The
 * <code>FTPPushFile</code> uses a standard <code>JBJFFTPDefinition</code>
 * object to store and pass the file transfer properties.
 * </p>
 * <p>
 * The FTP functionality of the JBJF Batch Framework is provided by
 * an open source library, Apache Common Net.  See the Apache Software
 * Foundations website for complete details on this component library.
 * </p>
 * <p>
 * <h3>Dependencies:</h3>
 * <ul>
 * <li>JBJF 1.3.0(+)</li>
 * <li>JRE/JDK 6(+)</li>
 * </ul>
 * <h3>Resources:</h3>
 * <code>FTPPushFile</code> depends on the following &lt;resource&gt; 
 * elements to function correctly:
 * <ul>
 * <li>ftp-definition - The &lt;ftp-definition&gt; in the JBJF Batch
 * Definition file to use.  This contains all the FTP properties
 * needed to connect to a server and copy a file.</li>
 * <li>plugin-cipher - An optional &lt;resource&gt;, the &lt;plug-cipher&gt; 
 * contains an id to a &plugin-definition&gt; in the JBJF Batch 
 * Definition file to use for encryption/decryption.</li>
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
 *      <td>ftp-definition</td>
 *      <td>&nbsp;</td>
 *      <td>String</td>
 *      <td>&nbsp;</td>
 *      <td>True</td>
 *      <td>&nbsp;</td>
 *      <td>The &lt;ftp-definition&gt; in the JBJF Batch Definition
 *      file that contains the FTP properties and file names/directory
 *      paths.
 *      </td>
 *  </tr>
 *  <tr valign='top' bgcolor="#cccccc">
 *      <td>&lt;task&gt;</td>
 *      <td>&nbsp;</td>
 *      <td>plugin-cipher</td>
 *      <td>&nbsp;</td>
 *      <td>JBJFPluginDefinition</td>
 *      <td>&nbsp;</td>
 *      <td>False</td>
 *      <td>&nbsp;</td>
 *      <td>The id for the &lt;plugin-definition&gt; that will get
 *      used in this task.  This will be the Cipher Plugin 
 *      (<code>IJBJFPluginCipher</code>) for the encryption/decryption
 *      of &lt;ftp-definition&gt; properties.
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
 *         &lt;task name="t003" order="3" active="true"&gt;
 *             &lt;class&gt;org.jbjf.tasks.FTPPullFile&lt;/class&gt;
 *             &lt;resource type="ftp-definition"&gt;&BUILD;-pull-ftp&lt;/resource&gt;
 *             &lt;resource type="plugin-cipher"&gt;default-cipher&lt;/resource&gt;
 *         &lt;/task&gt;
 * </pre>
 * </p>
 * <p>
 * @author Adym S. Lincoln<br>
 *         Copyright (C) 2007-2009. JBJF All rights reserved.
 * @version 1.3.0
 * @since   1.0.0
 * </p>
 */
public class FTPPushFile extends AbstractTask {

    /** 
     * Stores a fully qualified class name.  Used for debugging and 
     * auditing.
     * @since 1.0.0
     */
    public static final String ID = FTPPushFile.class.getName();

    /** 
     * Stores the class name, primarily used for debugging and so 
     * forth.  Used for debugging and auditing.
     * @since 1.0.0
     */
    private String SHORT_NAME = "FTPPushFile()";

    /** 
     * Stores a <code>SYSTEM IDENTITY HASHCODE</code>.  Used for
     * debugging and auditing.
     * @since 1.0.0
     */
    private String SYSTEM_IDENTITY = String.valueOf(System.identityHashCode(this));

    /**
     * Class property that stores the full/partial directory 
     * path to the source file.  This will not include the 
     * source filename, which is stored in <code>mstrFilename</code>.
     */
    private String mstrSourceDirectory = null;

    /**
     * Class property that stores the full/partial directory 
     * path to the target file.  This will not include the 
     * filename, which is stored in <code>mstrFilename</code>.
     */
    private String mstrTargetDirectory = null;

    /**
     * Class property that stores the filename.
     */
    private String mstrFilename = null;

    /**
     * Class property that stores the name of the remote server.
     */
    private String mstrRemoteServer = null;

    /**
     * Class property that stores the userid of the remote server.
     * Stored as an encrypted text string.
     */
    private String mstrServerUsr = null;

    /**
     * Class property that stores the userid of the remote server.
     * Stored as an encrypted text string.
     */
    private String mstrServerPwd = null;

    /**
     * Default constructor...allows the use of Class.forName().
     * The <code>AbstractTask</code> class will resolve any &lt;resource&gt;
     * elements from the XML batch definition file, thus your 
     * &lt;ftp-definition&gt; will be resolved in the <code>initialize()</code>
     * method.
     */
    public FTPPushFile() {
        this.mstrSourceDirectory = null;
        this.mstrTargetDirectory = null;
        this.mstrFilename = null;
        this.mstrRemoteServer = null;
        this.mstrServerUsr = null;
        this.mstrServerPwd = null;
        mtaskRequired = new ArrayList();
        getRequiredResources().add("ftp-definition");
    }

    public FTPPushFile(String sourceDirectory, String targetDirectory, String fileName, String remoteServerName, String serverUsr, String serverPwd) {
        this.mstrSourceDirectory = sourceDirectory;
        this.mstrTargetDirectory = targetDirectory;
        this.mstrFilename = fileName;
        this.mstrRemoteServer = remoteServerName;
        this.mstrServerUsr = serverUsr;
        this.mstrServerPwd = serverPwd;
    }

    public void runTask(HashMap pjobParms) throws Exception {
        FTPClient lftpClient = null;
        FileInputStream lfisSourceFile = null;
        JBJFPluginDefinition lpluginCipher = null;
        IJBJFPluginCipher theCipher = null;
        try {
            JBJFFTPDefinition lxmlFTP = null;
            if (getFTPDefinition() != null) {
                lxmlFTP = getFTPDefinition();
                this.mstrSourceDirectory = lxmlFTP.getSourceDirectory();
                this.mstrTargetDirectory = lxmlFTP.getTargetDirectory();
                this.mstrFilename = lxmlFTP.getFilename();
                this.mstrRemoteServer = lxmlFTP.getServer();
                if (getResources().containsKey("plugin-cipher")) {
                    lpluginCipher = (JBJFPluginDefinition) getResources().get("plugin-cipher");
                }
                if (lpluginCipher != null) {
                    theCipher = getTaskPlugins().getCipherPlugin(lpluginCipher.getPluginId());
                }
                if (theCipher != null) {
                    this.mstrServerUsr = theCipher.decryptString(lxmlFTP.getUser());
                    this.mstrServerPwd = theCipher.decryptString(lxmlFTP.getPass());
                } else {
                    this.mstrServerUsr = lxmlFTP.getUser();
                    this.mstrServerPwd = lxmlFTP.getPass();
                }
            } else {
                throw new Exception("Work unit [ " + SHORT_NAME + " ] is missing an FTP Definition.  Please check" + " your JBJF Batch Definition file an make sure" + " this work unit has a <resource> element added" + " within the <task> element.");
            }
            lfisSourceFile = new FileInputStream(mstrSourceDirectory + File.separator + mstrFilename);
            lftpClient = new FTPClient();
            lftpClient.connect(mstrRemoteServer);
            lftpClient.setFileType(lxmlFTP.getFileTransferType());
            if (!FTPReply.isPositiveCompletion(lftpClient.getReplyCode())) {
                throw new Exception("FTP server [ " + mstrRemoteServer + " ] refused connection.");
            }
            if (!lftpClient.login(mstrServerUsr, mstrServerPwd)) {
                throw new Exception("Unable to login to server [ " + mstrTargetDirectory + " ].");
            }
            if (!lftpClient.changeWorkingDirectory(mstrTargetDirectory)) {
                throw new Exception("Unable to change to remote directory [ " + mstrTargetDirectory + "]");
            }
            lftpClient.enterLocalPassiveMode();
            if (!lftpClient.storeFile(mstrFilename, lfisSourceFile)) {
                throw new Exception("Unable to upload [ " + mstrSourceDirectory + "/" + mstrFilename + " ]" + " to " + mstrTargetDirectory + File.separator + mstrFilename + " to " + mstrRemoteServer);
            }
            lfisSourceFile.close();
            lftpClient.logout();
        } catch (Exception e) {
            throw e;
        } finally {
            if (lftpClient != null && lftpClient.isConnected()) {
                try {
                    lftpClient.disconnect();
                } catch (IOException ioe) {
                }
            }
            if (lfisSourceFile != null) {
                try {
                    lfisSourceFile.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
