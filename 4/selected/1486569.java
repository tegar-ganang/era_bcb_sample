package com.georgeandabe.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * @goal buildHtml
 */
public class TemplateMojo extends AbstractMojo {

    /**
     * @parameter  
     */
    private File templateDirectory;

    /**
     * @parameter  
     */
    private File targetDirectory;

    /**
     * @parameter
     */
    private String projectName;

    public void execute() throws MojoExecutionException {
        if (templateDirectory == null) {
            getLog().warn("No velocity templates configured!");
            return;
        } else if ((!templateDirectory.exists()) || (!templateDirectory.canRead())) {
            getLog().warn("Can't find any velocity templates to compile!");
            return;
        }
        if (targetDirectory.exists() == false) {
            if (targetDirectory.mkdir() == false) {
                getLog().error("Unable to create target directory:" + targetDirectory.getName());
                throw new MojoExecutionException("Bad target directory");
            }
        }
        try {
            VelocityEngine engine = new VelocityEngine();
            engine.setProperty("file.resource.loader.path", templateDirectory.getCanonicalPath());
            engine.init();
            VelocityContext velocityContext = new VelocityContext();
            if (projectName != null) {
                velocityContext.put("projectName", projectName);
            } else {
                velocityContext.put("projectName", "Unnamed Project");
            }
            File[] children = templateDirectory.listFiles();
            for (int i = 0; children != null && i < children.length; i++) {
                if (children[i].getName().endsWith(".html")) {
                    Template template = engine.getTemplate(children[i].getName());
                    File output = new File(targetDirectory, children[i].getName());
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));
                    template.merge(velocityContext, writer);
                    writer.flush();
                    writer.close();
                } else if (children[i].getName().endsWith(".vm")) {
                    continue;
                } else if (children[i].getName().equals(".svn")) {
                    continue;
                } else {
                    copy(children[i], targetDirectory);
                }
            }
        } catch (ResourceNotFoundException rnfe) {
            getLog().error(rnfe);
            throw new MojoExecutionException("Can't find resource", rnfe);
        } catch (ParseErrorException pee) {
            getLog().warn("Syntax error in template:" + pee);
            throw new MojoExecutionException("Bad syntax", pee);
        } catch (Exception e) {
            throw new MojoExecutionException("Unexpected exception", e);
        }
    }

    private void copy(File source, File destinationDirectory) throws IOException {
        if (source.isDirectory()) {
            File newDir = new File(destinationDirectory, source.getName());
            newDir.mkdir();
            File[] children = source.listFiles();
            for (int i = 0; i < children.length; i++) {
                if (children[i].getName().equals(".svn")) {
                    continue;
                }
                copy(children[i], newDir);
            }
        } else {
            File newFile = new File(destinationDirectory, source.getName());
            if (newFile.exists() && source.lastModified() == newFile.lastModified()) {
                return;
            }
            FileOutputStream output = new FileOutputStream(newFile);
            FileInputStream input = new FileInputStream(source);
            byte[] buff = new byte[2048];
            int read = 0;
            while ((read = input.read(buff)) > 0) {
                output.write(buff, 0, read);
            }
            output.flush();
            output.close();
            input.close();
        }
    }
}
