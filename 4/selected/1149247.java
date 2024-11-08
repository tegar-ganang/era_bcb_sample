package com.erinors.tapestry.tapdoc.service;

import java.io.StringReader;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.hivemind.Registry;

/**
 * @author Norbert Sándor
 */
public class TapdocContextImpl implements TapdocContext {

    public TapdocContextImpl(Registry registry, FileObject javaDom, List<String> javadocLinks, List<String> libraryLocations, FileObject outputDirectory, List<String> tapdocLinks, DocumentGenerator documentGenerator) {
        this.registry = registry;
        this.documentGenerator = documentGenerator;
        try {
            if (javaDom == null) {
                javaDom = outputDirectory.resolveFile("tapdoc-javadom.xml");
            }
            if (!javaDom.exists()) {
                javaDom.createFile();
                javaDom.close();
                IOUtils.copy(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?><tapdoc-javadom></tapdoc-javadom>"), javaDom.getContent().getOutputStream());
            }
            this.javaDom = javaDom;
            this.javadocLinks = javadocLinks;
            this.tapdocLinks = tapdocLinks;
            this.libraryLocations = libraryLocations;
            this.outputDirectory = outputDirectory;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final DocumentGenerator documentGenerator;

    public String getGeneratedFileNameExtension() {
        return documentGenerator.getGeneratedFileNameExtension();
    }

    private final Registry registry;

    public Registry getRegistry() {
        return registry;
    }

    private final FileObject javaDom;

    public FileObject getJavaDom() {
        return javaDom;
    }

    private final List<String> javadocLinks;

    public List<String> getJavadocLinks() {
        return javadocLinks;
    }

    private final List<String> tapdocLinks;

    public List<String> getTapdocLinks() {
        return tapdocLinks;
    }

    private final List<String> libraryLocations;

    public List<String> getLibraryLocations() {
        return libraryLocations;
    }

    private final FileObject outputDirectory;

    public FileObject getOutputDirectory() {
        return outputDirectory;
    }
}
