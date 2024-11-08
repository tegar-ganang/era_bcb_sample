package com.scriptella.server.core.project;

import com.scriptella.server.core.io.FileRef;
import com.scriptella.server.project.model.XmlProject.Resource;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.util.List;

public class ProjectCompiler {

    FileRef srcDir;

    FileRef outDir;

    public void compile(Project project) throws ProjectCompilerException {
        List<Resource> resources = project.getModel().getResource();
        for (Resource resource : resources) {
            try {
                IOUtils.copy(srcDir.getRelative(resource.getLocation()).getInputStream(), outDir.getRelative(resource.getLocation()).getOutputStream());
            } catch (IOException e) {
                throw new ProjectCompilerException("Resource cannot be copied. Compilation failed", e);
            }
        }
    }
}
