package com.ideo.sweetdevria.elbuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.digester.Digester;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.xml.sax.SAXException;
import com.ideo.sweetdevria.elbuilder.model.tld.Tag;
import com.ideo.sweetdevria.elbuilder.model.tld.TagLib;

/**
 * This mojo can generate an 'el' tld, and the 'el' java tags.
 * 
 * @goal el-generator
 */
public class ELGeneratorMojo extends AbstractMojo {

    /**
     * The directory where java are generated.
     * This directory will be added to the project compile directories.
     * 
     * @parameter expression="${project.build.directory}/generated-sources/el-builder"
     */
    private File sourcesOutputDirectory;

    /**
     * The directory where the mojo store temporary file.
     * 
     * @parameter expression="${project.build.directory}/el-builder"
     */
    private File outputDirectory;

    /**
     * The directory where the tld will be generated.
     * 
     * @required
     * @parameter
     */
    private File tldDirectory;

    /**
     * The package name for the java generated source code.
     * 
     * @required
     * @parameter
     */
    private String packageName;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${maven.el-generator.skip}"
     */
    private boolean skip = false;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!skip) {
            if (!sourcesOutputDirectory.exists()) {
                sourcesOutputDirectory.mkdirs();
            }
            File sourcesOutputDirectory = new File(this.sourcesOutputDirectory.getAbsoluteFile() + "/" + packageName.replace('.', '/'));
            if (!sourcesOutputDirectory.exists()) {
                sourcesOutputDirectory.mkdirs();
            }
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }
            project.addCompileSourceRoot(this.sourcesOutputDirectory.getAbsolutePath());
            genetateElTld();
            generateElTag();
        } else {
            getLog().info("Skip el generator.");
        }
    }

    private void genetateElTld() throws MojoFailureException {
        getLog().info("Generating el tld ...");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        InputStream xsl = Thread.currentThread().getContextClassLoader().getResourceAsStream("xsl/ELTld.xsl");
        File original = new File(tldDirectory + "/sweetdev-ria-el.tld");
        File source = new File(outputDirectory + "/tlds/source.tld");
        File result = new File(outputDirectory + "/tlds/sweetdev-ria-el.tld");
        try {
            if (!source.exists()) {
                FileUtils.copyFile(original, source);
            }
        } catch (IOException e) {
            throw new MojoFailureException("Problem when trying to copy file: " + original + " in file: " + source, e);
        }
        try {
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xsl));
            transformer.setParameter("packageName", packageName);
            transformer.transform(new StreamSource(source), new StreamResult(result));
        } catch (TransformerConfigurationException e) {
            throw new MojoFailureException("Problem when trying to generate el tld.", e);
        } catch (TransformerException e) {
            throw new MojoFailureException("Problem when trying to generate el tld.", e);
        }
        try {
            FileUtils.copyFileToDirectory(result, tldDirectory);
        } catch (IOException e) {
            throw new MojoFailureException("Problem when trying to copy file: " + result + " in directory: " + tldDirectory, e);
        }
        getLog().info("Generated el tld.");
    }

    private void generateElTag() throws MojoFailureException {
        getLog().info("Generating el tags ...");
        InputStream elTagXSL = Thread.currentThread().getContextClassLoader().getResourceAsStream("xsl/ELTag.xsl");
        InputStream elTagBeanInfoXSL = Thread.currentThread().getContextClassLoader().getResourceAsStream("xsl/ELTagBeanInfo.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer elTagTransformer;
        Transformer elTagBeanInfoTransformer;
        try {
            elTagTransformer = transformerFactory.newTransformer(new StreamSource(elTagXSL));
            elTagBeanInfoTransformer = transformerFactory.newTransformer(new StreamSource(elTagBeanInfoXSL));
        } catch (TransformerConfigurationException e) {
            throw new MojoFailureException("Problem when trying to generate el tags.", e);
        }
        String outputPath = this.sourcesOutputDirectory.getAbsoluteFile() + "/" + packageName.replace('.', '/');
        File source = new File(outputDirectory + "/tlds/source.tld");
        TagLib taglib = parseTld(new File(outputDirectory + "/tlds/source.tld"));
        List tags = taglib.getTags();
        for (int i = 0; i < tags.size(); i++) {
            Tag tag = (Tag) tags.get(i);
            String[] splits = tag.getTagClass().replace('.', '/').split("/");
            String tagName = splits[splits.length - 1];
            File originalTag = new File(project.getBuild().getSourceDirectory() + "/" + tag.getTagClass().replace('.', '/') + ".java");
            File resultElTag = new File(outputPath + "/EL" + tagName + ".java");
            File resultElTagBeanInfo = new File(outputPath + "/EL" + tagName + "BeanInfo.java");
            elTagTransformer.setParameter("tag", tagName);
            elTagTransformer.setParameter("packageName", packageName);
            elTagBeanInfoTransformer.setParameter("tag", tagName);
            elTagBeanInfoTransformer.setParameter("packageName", packageName);
            try {
                if (needToGenerate(originalTag, resultElTag)) {
                    getLog().info("Generate el support for tag: " + tagName);
                    elTagTransformer.transform(new StreamSource(source), new StreamResult(resultElTag));
                    elTagBeanInfoTransformer.transform(new StreamSource(source), new StreamResult(resultElTagBeanInfo));
                }
            } catch (TransformerException e) {
                throw new MojoFailureException("Problem when trying to generate el support for tag: " + tagName, e);
            }
        }
        getLog().info("Generated el tags.");
    }

    private TagLib parseTld(File tldFile) throws MojoFailureException {
        Digester digester = new Digester();
        digester.setValidating(false);
        digester.addObjectCreate("taglib", TagLib.class);
        digester.addObjectCreate("taglib/tag", Tag.class);
        digester.addCallMethod("taglib/tag/tag-class", "setTagClass", 0);
        digester.addSetNext("taglib/tag", "addTag");
        try {
            return (TagLib) digester.parse(tldFile);
        } catch (IOException e) {
            throw new MojoFailureException("Problem when parsing tld file: " + tldFile, e);
        } catch (SAXException e) {
            throw new MojoFailureException("Problem when parsing tld file: " + tldFile, e);
        }
    }

    private boolean needToGenerate(File source, File result) {
        if (result.exists()) {
            return source.lastModified() > result.lastModified();
        }
        return true;
    }
}
