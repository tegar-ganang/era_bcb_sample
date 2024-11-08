package com.bytetranslation.boltran;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.ScopeType;
import org.jboss.seam.log.Log;
import com.bytetranslation.translation.Project;
import com.bytetranslation.translation.FileInfo;
import com.bytetranslation.translation.ProjectException;

@Scope(ScopeType.EVENT)
@Name("downloadAction")
public class DownloadAction {

    @Logger
    private Log log;

    @In(value = "#{facesContext.externalContext}")
    private ExternalContext extCtx;

    @In(value = "#{facesContext}")
    FacesContext facesContext;

    @In
    private Project project;

    private enum FILE_TYPE {

        TRANSLATIONS, BACKUP
    }

    ;

    public void downloadZipTranslations() throws DownloadException {
        downloadZipFile(FILE_TYPE.TRANSLATIONS);
    }

    public void downloadZipBackup() throws DownloadException {
        downloadZipFile(FILE_TYPE.BACKUP);
    }

    private void downloadZipFile(FILE_TYPE fileType) throws DownloadException {
        ServletOutputStream os = null;
        FileInputStream fis = null;
        File outputZipFile = null;
        String outputFilename = project.getName() + ".zip";
        HttpServletResponse response = (HttpServletResponse) extCtx.getResponse();
        response.setContentType("application/zip");
        response.addHeader("Content-disposition", "attachment; filename=\"" + outputFilename + "\"");
        try {
            outputZipFile = File.createTempFile("zippedTranslation", ".tmp");
            switch(fileType) {
                case TRANSLATIONS:
                    project.createTranslations(outputZipFile);
                    break;
                case BACKUP:
                    project.createBackup(outputZipFile);
                    break;
            }
            os = response.getOutputStream();
            fis = new FileInputStream(outputZipFile);
            byte buffer[] = new byte[4096];
            int read = 0;
            while ((read = fis.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            os.flush();
            os.close();
            fis.close();
            facesContext.responseComplete();
        } catch (Exception e) {
            throw new DownloadException("Download of zip file failed", e);
        } finally {
            if (outputZipFile != null) {
                boolean success = outputZipFile.delete();
                if (!success) {
                    log.error("\nCouldn't delete temp file after download.\n");
                }
            }
        }
    }
}
