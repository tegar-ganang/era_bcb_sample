package de.jassda.util.installer.ant;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import de.jassda.util.installer.Constants;

/**
 * A ant task which creates an installer. Therefore some attributes have to be
 * specified:
 * <br/><code>file</code> tells this task where to store the installer. This file 
 * 		will be an executable jar file - hence it'll be appropriate that the name ends with ".jar"
 * 
 * <br/><code>name</code> is the name of the program.
 * 
 *  <br /><code>version</code> is the version of the program.
 *  
 *  <br /><code>description</code> is the description of the program. If the description 
 *  	is identified to be a filename the content of that file is loaded.
 *  
 *  <br /><code>uiBannerImage</code> the background image of the ui.
 *  
 *  <br /><code>uiBannerColor</code> the color of the text in the ui-banner.
 * 
 * <br/><code>updatefiles</code> is a <b>optional</b> but quite handy attribute. If set
 * 		to <code>true</code> the file-elements specified in installation.xml will be updated
 * 		with all files actually installed. If this attribute is set file-elements can be ommitted.
 * 
 * <br/>A nested element <code>fileset</code> which defines the files which will be
 * 		packed into the installer archive. Note: File will only be installed iff they are
 * 		declared in the installation.xml-file. 
 * <br />
 * The jar file will have a manifest which declares the Main-Class attribute, which is the
 * Main-Class of the Installer (de.jassda.util.installer.InstallerConfiguration). Once this task
 * is done the resulting jar file will have the structure shown below:
 <pre>
 META-INF/
 ...manifest.mf
 ...install/
 ......installation.xml
 ......header.gif
 FILES/
 ...file0
 ...file1
 ...file2
    ...		
 </pre>
 * 
 * @author <a href="johannes.rieken@informatik.uni-oldenburg.de">riejo</a>
 */
public class InstallBuilder extends Task {

    static final String manifestEntries = "Manifest-Version: 1.0\n" + "Main-Class: de.jassda.util.installer.InstallationConfiguration\n";

    private Manifest manifest;

    private List allFiles;

    private String programName;

    private String programVersion;

    private String programDescription;

    private File uiBannerImage;

    private String uiBannerColor;

    private boolean updateFiles;

    private boolean needsToolsJar;

    private File file;

    private List filesets;

    private String uiProgramNameColor;

    public InstallBuilder() {
        filesets = new LinkedList();
        allFiles = new LinkedList();
        try {
            manifest = new Manifest(new ByteArrayInputStream(manifestEntries.getBytes()));
        } catch (IOException e) {
            throw new BuildException("InstallBuilder is broken! Can not create Manifest\n" + e);
        }
    }

    public void setName(String programName) {
        this.programName = programName;
    }

    public void setVersion(String version) {
        this.programVersion = version;
    }

    public void setDescription(String programDescription) {
        this.programDescription = programDescription;
    }

    public void setNeedsToolsJar(boolean b) {
        this.needsToolsJar = b;
    }

    public void setUiBannerImage(File image) {
        this.uiBannerImage = image;
    }

    public void setUiBannerColor(String color) {
        this.uiBannerColor = color;
    }

    public void setUiProgramNameColor(String color) {
        this.uiProgramNameColor = color;
    }

    public void setUpdateFiles(boolean update) {
        this.updateFiles = update;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void addFileSet(FileSet fileset) {
        filesets.add(fileset);
    }

    protected void validate() {
        if (uiBannerImage == null || !uiBannerImage.isFile()) throw new BuildException("installationdef not set or not a directory");
        if (programName == null) throw new BuildException("programName not set");
        if (file == null) throw new BuildException("installerfile not set");
    }

    /**
	 * Executes this task, which consists of a number of steps. First of all
	 * the prerequirements are checked {@linkplain #validate()}. On success
	 * a jar is created which contains the installer class files, a special directory
	 * <code>FILES</code> containing the files to install and metadata like
	 * <code>installation.xml</code> and <code>header.gif</code>.
	 * @throws BuildException If {@linkplain #validate()} fails or files can not
	 * 		be copied.
	 */
    public void execute() {
        validate();
        try {
            JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(file), manifest);
            log("Writing to archive " + file);
            copyInstallerClassFiles(jarOut);
            for (Iterator iter = filesets.iterator(); iter.hasNext(); ) {
                FileSet fs = (FileSet) iter.next();
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                String[] includedFiles = ds.getIncludedFiles();
                for (int i = 0; i < includedFiles.length; i++) {
                    copyToJar(new JarEntry("FILES/" + includedFiles[i]), new FileInputStream(ds.getBasedir() + File.separator + includedFiles[i]), jarOut);
                    if (updateFiles) allFiles.add(includedFiles[i]);
                    log("Added file [" + includedFiles[i] + "]");
                }
            }
            updateProgramDescription();
            InputStream installationxml = createInstallationXML();
            copyToJar(new JarEntry("META-INF/install/installation.xml"), installationxml, jarOut);
            copyToJar(new JarEntry("META-INF/install/" + uiBannerImage.getName()), new FileInputStream(uiBannerImage), jarOut);
            log("Added metadata");
            jarOut.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }

    /**
	 * Copies the passed file as the passed jar/zip entry to the passes jar output stream.
	 */
    protected void copyToJar(JarEntry entry, InputStream in, JarOutputStream jarOut) throws IOException {
        jarOut.putNextEntry(entry);
        int read;
        byte[] block = new byte[4096];
        while ((read = in.read(block)) != -1) {
            jarOut.write(block, 0, read);
        }
        jarOut.closeEntry();
    }

    private void updateProgramDescription() {
        try {
            String newProgramDescription = "";
            BufferedReader reader = new BufferedReader(new FileReader(programDescription));
            String line;
            while ((line = reader.readLine()) != null) {
                newProgramDescription += line + "\n";
            }
            log("Updated program description from file: " + programDescription);
            programDescription = newProgramDescription;
        } catch (IOException e) {
        }
    }

    /**
	 * Modifies the passed installation.xml so that all files are included. 
	 */
    private InputStream createInstallationXML() throws IOException {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element elementInstall = doc.createElement("install");
            elementInstall.setAttribute(Constants.ATTR_NAME, programName);
            elementInstall.setAttribute(Constants.ATTR_VERSION, programVersion);
            elementInstall.setAttribute(Constants.ATTR_DESC, programDescription);
            elementInstall.setAttribute(Constants.ATTR_NEEDS_TOOLS_JAR, new Boolean(needsToolsJar).toString());
            elementInstall.setAttribute(Constants.ATTR_UI_BANNER_COLOR, uiBannerColor);
            elementInstall.setAttribute(Constants.ATTR_UI_BANNER_IMAGE, uiBannerImage.getName());
            elementInstall.setAttribute(Constants.ATTR_UI_PROGRAM_NAME_COLOR, uiProgramNameColor);
            for (int i = 0; i < allFiles.size(); i++) {
                String fileName = (String) allFiles.get(i);
                Element element = doc.createElement("file");
                element.setAttribute("name", fileName);
                elementInstall.appendChild(element);
            }
            doc.appendChild(elementInstall);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(buffer));
            return new ByteArrayInputStream(buffer.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            log("Warning: Could NOT update <file name=\"...\" />-elements in installation.xml");
            return null;
        }
    }

    private static final String INSTALLER_CLASS_FILES_JAR = "/installer_class_files.jar";

    /**
	 * Copies the contents of the nested jar file into the new one.
	 * @param jarOut
	 * @throws IOException
	 */
    private void copyInstallerClassFiles(JarOutputStream jarOut) throws IOException {
        JarInputStream jarIn = new JarInputStream(getClass().getResourceAsStream(INSTALLER_CLASS_FILES_JAR));
        JarEntry jarEntry;
        while ((jarEntry = jarIn.getNextJarEntry()) != null) {
            jarOut.putNextEntry(jarEntry);
            int read;
            byte[] block = new byte[2048];
            while ((read = jarIn.read(block)) != -1) {
                jarOut.write(block, 0, read);
            }
            jarOut.closeEntry();
        }
    }
}
