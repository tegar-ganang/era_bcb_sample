package org.telscenter.sail.webapp.presentation.web.controllers.admin;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.sail.webapp.domain.Curnit;
import net.sf.sail.webapp.domain.User;
import net.sf.sail.webapp.presentation.web.controllers.ControllerUtil;
import net.sf.sail.webapp.service.curnit.CurnitService;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.telscenter.sail.webapp.domain.impl.CreateUrlModuleParameters;
import org.telscenter.sail.webapp.domain.impl.ProjectParameters;
import org.telscenter.sail.webapp.domain.project.Project;
import org.telscenter.sail.webapp.domain.project.ProjectMetadata;
import org.telscenter.sail.webapp.domain.project.ProjectUpload;
import org.telscenter.sail.webapp.domain.project.impl.ProjectMetadataImpl;
import org.telscenter.sail.webapp.domain.project.impl.ProjectType;
import org.telscenter.sail.webapp.service.project.ProjectService;

/**
 * Admin tool for uploading a zipped LD project.
 * Unzips to curriculum_base_dir and registers the project (ie creates project in DB).
 * 
 * @author hirokiterashima
 * @version $Id: UploadProjectController.java 3221 2011-08-10 17:12:24Z honchikun@gmail.com $
 */
public class UploadProjectController extends SimpleFormController {

    private ProjectService projectService;

    private CurnitService curnitService;

    private Properties portalProperties;

    /**
	 * @override @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
        ProjectUpload projectUpload = (ProjectUpload) command;
        MultipartFile file = projectUpload.getFile();
        String curriculumBaseDir = portalProperties.getProperty("curriculum_base_dir");
        File uploadDir = new File(curriculumBaseDir);
        if (!uploadDir.exists()) {
            throw new Exception("curriculum upload directory does not exist.");
        }
        String sep = System.getProperty("file.separator");
        long timeInMillis = Calendar.getInstance().getTimeInMillis();
        String zipFilename = file.getOriginalFilename();
        String filename = zipFilename.substring(0, zipFilename.indexOf(".zip"));
        String newFilename = filename + "-" + timeInMillis;
        String newFileFullPath = curriculumBaseDir + sep + newFilename + ".zip";
        File uploadedFile = new File(newFileFullPath);
        uploadedFile.createNewFile();
        FileCopyUtils.copy(file.getBytes(), uploadedFile);
        String newFileFullDir = curriculumBaseDir + sep + newFilename;
        File newFileFullDirFile = new File(newFileFullDir);
        newFileFullDirFile.mkdir();
        try {
            ZipFile zipFile = new ZipFile(newFileFullPath);
            Enumeration entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    System.out.println("Extracting directory: " + entry.getName());
                    (new File(entry.getName().replace(filename, newFileFullDir))).mkdir();
                    continue;
                }
                System.out.println("Extracting file: " + entry.getName());
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(entry.getName().replace(filename, newFileFullDir))));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
        }
        uploadedFile.delete();
        String path = sep + newFilename + sep + "wise4.project.json";
        String name = projectUpload.getName();
        User signedInUser = ControllerUtil.getSignedInUser();
        Set<User> owners = new HashSet<User>();
        owners.add(signedInUser);
        CreateUrlModuleParameters cParams = new CreateUrlModuleParameters();
        cParams.setUrl(path);
        Curnit curnit = curnitService.createCurnit(cParams);
        ProjectParameters pParams = new ProjectParameters();
        pParams.setCurnitId(curnit.getId());
        pParams.setOwners(owners);
        pParams.setProjectname(name);
        pParams.setProjectType(ProjectType.LD);
        ProjectMetadata metadata = new ProjectMetadataImpl();
        metadata.setTitle(name);
        pParams.setMetadata(metadata);
        Project project = projectService.createProject(pParams);
        ModelAndView modelAndView = new ModelAndView(getSuccessView());
        modelAndView.addObject("msg", "Upload project complete, new projectId is: " + project.getId());
        return modelAndView;
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    /**
	 * @param projectService the projectService to set
	 */
    public void setProjectService(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
	 * @param curnitService the curnitService to set
	 */
    public void setCurnitService(CurnitService curnitService) {
        this.curnitService = curnitService;
    }

    /**
	 * @param portalProperties the portalProperties to set
	 */
    public void setPortalProperties(Properties portalProperties) {
        this.portalProperties = portalProperties;
    }
}
