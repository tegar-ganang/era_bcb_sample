package org.telscenter.sail.webapp.presentation.web.controllers.admin;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.sail.webapp.domain.impl.CurnitGetCurnitUrlVisitor;
import org.apache.commons.io.IOUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.telscenter.sail.webapp.domain.project.Project;
import org.telscenter.sail.webapp.service.project.ProjectService;

/**
 * @author hirokiterashima
 * @version $Id:$
 */
public class ExportProjectController extends AbstractController {

    private ProjectService projectService;

    private Properties portalProperties;

    static final int BUFFER = 2048;

    /**
	 * @see org.springframework.web.servlet.mvc.AbstractController#handleRequestInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String projectId = request.getParameter("projectId");
        Project project = projectService.getById(projectId);
        String curriculumBaseDir = portalProperties.getProperty("curriculum_base_dir");
        String sep = System.getProperty("file.separator");
        String rawProjectUrl = (String) project.getCurnit().accept(new CurnitGetCurnitUrlVisitor());
        String projectJSONFullPath = curriculumBaseDir + sep + rawProjectUrl;
        String foldername = rawProjectUrl.substring(1, rawProjectUrl.lastIndexOf(sep));
        String projectJSONDir = projectJSONFullPath.substring(0, projectJSONFullPath.lastIndexOf(sep));
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", "attachment;filename=\"" + foldername + ".zip" + "\"");
        ServletOutputStream outputStream = response.getOutputStream();
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(outputStream));
        File zipFolder = new File(projectJSONDir);
        int len = zipFolder.getAbsolutePath().lastIndexOf(File.separator);
        String baseName = zipFolder.getAbsolutePath().substring(0, len + 1);
        addFolderToZip(zipFolder, out, baseName);
        out.close();
        return null;
    }

    private static void addFolderToZip(File folder, ZipOutputStream zip, String baseName) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getAbsolutePath().substring(baseName.length());
                ZipEntry zipEntry = new ZipEntry(name + "/");
                zip.putNextEntry(zipEntry);
                zip.closeEntry();
                addFolderToZip(file, zip, baseName);
            } else {
                String name = file.getAbsolutePath().substring(baseName.length());
                ZipEntry zipEntry = new ZipEntry(updateFilename(name));
                zip.putNextEntry(zipEntry);
                IOUtils.copy(new FileInputStream(file), zip);
                zip.closeEntry();
            }
        }
    }

    /**
	 * Given old filename, returns new, updated filename corresponding with new standards
	 * e.g. "Global Warming.project.json"->"wise4.project.json"
	 * "Global Warming.project-min.json"->wise4.project-min.json"
	 * @param oldFilename
	 * @return newFilename
	 */
    private static String updateFilename(String oldFilename) {
        int lastIndexOfSlash = oldFilename.lastIndexOf("/");
        String prepend = oldFilename.substring(0, lastIndexOfSlash);
        if (oldFilename.endsWith(".project.json")) {
            return prepend + "/wise4.project.json";
        } else if (oldFilename.endsWith(".project-min.json")) {
            return prepend + "/wise4.project-min.json";
        } else if (oldFilename.endsWith(".project-meta.json")) {
            return prepend + "/wise4.project-meta.json";
        }
        return oldFilename;
    }

    /**
	 * @param projectService the projectService to set
	 */
    public void setProjectService(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
	 * @param portalProperties the portalProperties to set
	 */
    public void setPortalProperties(Properties portalProperties) {
        this.portalProperties = portalProperties;
    }
}
