package com.erinors.tapestry.tapdoc.service.xdoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.hivemind.ClassResolver;
import org.apache.hivemind.Resource;
import org.apache.hivemind.util.ClasspathResource;
import com.erinors.tapestry.tapdoc.model.Component;
import com.erinors.tapestry.tapdoc.model.Library;
import com.erinors.tapestry.tapdoc.service.DocumentGenerator;
import com.erinors.tapestry.tapdoc.service.FileNameGenerator;
import com.erinors.tapestry.tapdoc.util.ZipUtils;
import com.erinors.tapestry.tapdoc.xml.XsltUtils;

/**
 * @author Norbert Sándor
 */
public class XdocDocumentGenerator implements DocumentGenerator {

    public XdocDocumentGenerator(ClassResolver classResolver, FileNameGenerator fileNameGenerator) {
        this.classResolver = classResolver;
        this.fileNameGenerator = fileNameGenerator;
    }

    private final ClassResolver classResolver;

    private final FileNameGenerator fileNameGenerator;

    public void generate(FileObject outputDirectory, FileObject generatedOutputDirectory, List<Library> libraryModels, String tapdocXml) throws FileSystemException {
        if (!generatedOutputDirectory.exists()) {
            generatedOutputDirectory.createFolder();
        }
        if (outputDirectory.exists()) {
            outputDirectory.createFolder();
        }
        ZipUtils.extractZip(new ClasspathResource(classResolver, "/com/erinors/tapestry/tapdoc/service/xdoc/resources.zip"), outputDirectory);
        for (Library library : libraryModels) {
            String libraryName = library.getName();
            String libraryLocation = library.getLocation();
            generatedOutputDirectory.resolveFile(fileNameGenerator.getLibraryDirectory(libraryLocation)).createFolder();
            try {
                String result = XsltUtils.xsltTransform(tapdocXml, getClass().getResourceAsStream("Library.xsl"), "libraryName", libraryName);
                FileObject index = generatedOutputDirectory.resolveFile(fileNameGenerator.getLibraryDirectory(libraryLocation)).resolveFile("index.xml");
                Writer out = new OutputStreamWriter(index.getContent().getOutputStream(), "UTF-8");
                out.write(result);
                out.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (Component component : library.getComponents()) {
                String componentName = component.getName();
                System.out.println("Generating " + libraryName + ":" + componentName + "...");
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("libraryName", libraryName);
                parameters.put("componentName", componentName);
                String result = XsltUtils.xsltTransform(tapdocXml, getClass().getResourceAsStream("Component.xsl"), parameters);
                Writer out = null;
                try {
                    FileObject index = generatedOutputDirectory.resolveFile(fileNameGenerator.getComponentIndexFile(libraryLocation, componentName, true));
                    out = new OutputStreamWriter(index.getContent().getOutputStream(), "UTF-8");
                    out.write(result);
                    out.close();
                    Resource specificationLocation = component.getSpecificationLocation();
                    if (specificationLocation.getRelativeResource(componentName + "_tapdoc/resource").getResourceURL() != null) {
                        File srcResourcesDirectory = new File(specificationLocation.getRelativeResource(componentName + "_tapdoc/resource").getResourceURL().toURI());
                        FileObject dstResourcesFileObject = outputDirectory.resolveFile(fileNameGenerator.getComponentDirectory(libraryLocation, componentName)).resolveFile("resource");
                        if (srcResourcesDirectory.exists() && srcResourcesDirectory.isDirectory()) {
                            File[] files = srcResourcesDirectory.listFiles();
                            if (files != null) {
                                for (File resource : files) {
                                    if (resource.isFile() && !resource.isHidden()) {
                                        FileObject resourceFileObject = dstResourcesFileObject.resolveFile(resource.getName());
                                        resourceFileObject.createFile();
                                        InputStream inResource = null;
                                        OutputStream outResource = null;
                                        try {
                                            inResource = new FileInputStream(resource);
                                            outResource = resourceFileObject.getContent().getOutputStream();
                                            IOUtils.copy(inResource, outResource);
                                        } finally {
                                            IOUtils.closeQuietly(inResource);
                                            IOUtils.closeQuietly(outResource);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(out);
                }
            }
        }
        {
            Writer out = null;
            try {
                String result = XsltUtils.xsltTransform(tapdocXml, getClass().getResourceAsStream("Overview.xsl"));
                FileObject index = generatedOutputDirectory.resolveFile("index.xml");
                out = new OutputStreamWriter(index.getContent().getOutputStream(), "UTF-8");
                out.write(result);
                out.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }

    public String getGeneratedFileNameExtension() {
        return "xml";
    }
}
