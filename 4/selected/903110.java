package net.sf.uibuilder.mojo;

import java.io.File;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.Native2Ascii;

/**
 * UibuilderTransformerMojo - uibuilder transformer maven plugin.
 *
 * @goal transform
 * @phase compile
 * @version   1.0 2008-8-25
 * @author    <A HREF="mailto:chyxiang@yahoo.com">Chen Xiang (Sean)</A>
 */
public class UibuilderTransformerMojo extends AbstractMojo {

    /**
     * the directory for source files
     * @parameter default-value="${basedir}/src/main/uibuilder/net/sf/uibuilder"
     */
    protected File src;

    /**
     * the directory for destination files.
     * @parameter default-value="${project.build.directory}/classes/net/sf/uibuilder"
     */
    protected File dest;

    /**
     * the resources xml file for generate the resources bundle properties
     * files, it should located in the src directory.
     * @parameter default-value="Resources.xml"
     */
    protected String resourcesXml;

    /**
     * the dest properties file name.
     * @parameter default-value="Resources"
     */
    protected String destPropertiesName;

    /**
     * flag to remove temp xsl files folder.
     * @parameter default-value=true
     */
    protected boolean cleanUp;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /**
     * ResourceManager for this plugin.
     * @component
     * @required
     * @readonly
     */
    private ResourceManager locator;

    /**
     * execute this goal.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.locator.addSearchPath(FileResourceLoader.ID, this.project.getFile().getParentFile().getAbsolutePath());
        this.locator.addSearchPath("url", "");
        executeN2A();
        try {
            if (!FileUtils.fileExists("target/uibuilder")) FileUtils.mkdir("target/uibuilder");
            if (!FileUtils.fileExists("target/uibuilder/resources-common.xsl")) {
                File xslFile = this.locator.getResourceAsFile("xsl/resources-common.xsl");
                FileUtils.copyFile(xslFile, new File("target/uibuilder/resources-common.xsl"));
            }
            executeXslt("");
            executeXslt("_zh_CN");
            if (this.cleanUp) FileUtils.forceDelete("target/uibuilder");
        } catch (Exception e) {
            getLog().error(e);
        }
    }

    /**
     * ask Ant to do the native 2 ascii work.
     */
    protected void executeN2A() {
        Project antProject = new Project();
        antProject.setName("native2ascii");
        Native2Ascii antTask = new Native2Ascii();
        antTask.setProject(antProject);
        antTask.setSrc(src);
        antTask.setDest(dest);
        antTask.execute();
    }

    /**
     * tranform the resources xml to properties.
     */
    protected void executeXslt(String localSurfix) throws Exception {
        String xslFileName = "resources" + localSurfix + ".xsl";
        if (!FileUtils.fileExists("target/uibuilder/" + xslFileName)) {
            File xslFileTemp = this.locator.getResourceAsFile("xsl/" + xslFileName);
            FileUtils.copyFile(xslFileTemp, new File("target/uibuilder/" + xslFileName));
        }
        File xslFile = new File("target/uibuilder/" + xslFileName);
        File srcXml = new File(this.dest, this.resourcesXml);
        String destPropertiesPath = this.dest.getAbsolutePath() + "/" + this.destPropertiesName + localSurfix + ".properties";
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(xslFile));
        transformer.setParameter("xdocsPath", this.dest.getAbsolutePath());
        transformer.transform(new StreamSource(srcXml), new StreamResult(destPropertiesPath));
    }
}
