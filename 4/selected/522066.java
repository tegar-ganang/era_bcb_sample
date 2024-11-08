package com.germinus.portlet.content_export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;
import javax.portlet.RenderRequest;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import com.germinus.xpression.cms.CMSRuntimeException;
import com.germinus.xpression.cms.CmsConfig;
import com.germinus.xpression.cms.contents.Content;
import com.germinus.xpression.cms.contents.ContentWithoutChaptersException;
import com.germinus.xpression.cms.contents.JCRContentPersister;
import com.germinus.xpression.cms.directory.DirectoryFolder;
import com.germinus.xpression.cms.directory.DirectoryPersister;
import com.germinus.xpression.cms.directory.ZipUtils;
import com.germinus.xpression.cms.educative.ExportContentData;
import com.germinus.xpression.cms.educative.ImportExportFileUtil;
import com.germinus.xpression.cms.educative.WebFileUtil;
import com.germinus.xpression.cms.model.ContentTypes;
import com.germinus.xpression.cms.util.ManagerRegistry;
import com.germinus.xpression.cms.web.TemporaryFilesHandler;
import com.germinus.xpression.i18n.I18NUtils;
import com.germinus.xpression.portlet.cms.CmsToolsConfig;
import com.liferay.portal.kernel.servlet.StringServletResponse;

public abstract class ContentExporter {

    private static final String TMP_FOLDER_PATH = "/tmp/";

    private static final String CSS_FOLDER_PATH = "/css";

    private static final String TMP_DIR_TO_COMPRESS = "tmpDirToCompress";

    private static Log log = LogFactory.getLog(ContentExporter.class);

    private static final String FILEREGEX = "[^a-zA-Z0-9]";

    private static final String FILENONAME = "NONAME";

    protected static final String VIEW_CONTENT_JSP_PATH = "/html/portlet/visor/view_content_alone.jsp";

    private HttpSession servletSession;

    private String tmpFolderToCompressPath;

    protected ExportContentData exportContentData;

    public ContentExporter(Content content, HttpServletRequest httpReq, HttpServletResponse httpRes) {
        exportContentData = new ExportContentData(content, null, httpReq, httpRes);
        if (!exportedContentIsWebFile()) {
            exportContentData.setCssFolder(createTmpCSSFolder());
        }
        exportContentData.setPathToExport(getViewPathToExport());
        JCRContentPersister.syncContentNow(content);
        this.servletSession = httpReq.getSession();
    }

    public String exportContent(RenderRequest request) throws ContentWithoutChaptersException, Exception {
        String originalErrorMessage = null;
        File targetZipFile = targetFile();
        ZipOutputStream zos = null;
        File cssFolder = exportContentData.getCssFolder();
        Boolean contentWithoutChapters = Boolean.FALSE;
        try {
            if (log.isDebugEnabled()) log.debug("Default repository path: " + CmsConfig.getDefaultRepositoryPath());
            zos = new ZipOutputStream(new FileOutputStream(targetZipFile));
            String indexOrFirstChapterFileName = importExportFileUtilFactory().exportContentToZip(exportContentData, zos);
            zos.flush();
            TemporaryFilesHandler.register(null, targetZipFile, servletSession);
            if (cssFolder != null) {
                compressAdditionalFiles(zos, indexOrFirstChapterFileName, cssFolder);
            }
        } catch (ContentWithoutChaptersException e) {
            contentWithoutChapters = Boolean.TRUE;
        } catch (Exception e) {
            originalErrorMessage = e.getMessage();
        } finally {
            try {
                if (contentWithoutChapters) {
                    String errorMessage = "content_admin.export.error.scorm-no-chapters";
                    throw new ContentWithoutChaptersException(errorMessage);
                }
                if (zos != null) zos.close();
            } catch (IOException e) {
                String errorMessage = "Error exporting to content with id: " + exportContentData.getContent().getId();
                log.error(errorMessage, e);
                throw new CMSRuntimeException((originalErrorMessage != null) ? originalErrorMessage : errorMessage, e);
            }
        }
        return exportedFilePath(targetZipFile);
    }

    private void compressAdditionalFiles(ZipOutputStream zos, String indexOrFirstChapterFileName, File cssFolder) throws IOException {
        if (StringUtils.isNotEmpty(indexOrFirstChapterFileName)) {
            String renderResult = renderContent(exportContentData);
            Content contentToExport = exportContentData.getContent();
            String renderResultWithRelativeUrls = WebFileUtil.getInstance().convertToLocalUrls(renderResult, zos, contentToExport.getResourcesFolder().getId(), null, contentToExport.getWorkspace(), exportContentData);
            String indexOrFirstChapterPath = tmpFolderToCompressPath + "/" + indexOrFirstChapterFileName;
            writeFile(renderResultWithRelativeUrls, indexOrFirstChapterPath);
        }
        ZipUtils.zipDirectoryFiles(tmpFolderToCompressPath, zos);
    }

    protected String renderContent(ExportContentData exportContentData) {
        String pathToExport = exportContentData.getPathToExport();
        RequestDispatcher rd = exportContentData.getServletContext().getRequestDispatcher(pathToExport);
        StringServletResponse servletResponse = new StringServletResponse(exportContentData.getHttpRes());
        HttpServletRequest httpReq = exportContentData.getHttpReq();
        httpReq.setAttribute(Globals.MESSAGES_KEY, I18NUtils.getDefaultThreadMessageResources());
        log.debug("Locale: " + I18NUtils.getThreadLocale());
        httpReq.setAttribute("exportProcess", true);
        httpReq.setAttribute("content", exportContentData.getContent());
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put("cssFilePaths", getCSSPaths(exportContentData.getCssFolder()));
        if (attributes != null) {
            Set<String> attributeNames = attributes.keySet();
            Iterator<String> attributeNamesIter = attributeNames.iterator();
            while (attributeNamesIter.hasNext()) {
                String attributeName = attributeNamesIter.next();
                httpReq.setAttribute(attributeName, attributes.get(attributeName));
            }
        }
        try {
            rd.include(httpReq, servletResponse);
            return servletResponse.getString();
        } catch (Exception e) {
            String errorMessage = "Error rendering path " + pathToExport;
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    private ArrayList<String> getCSSPaths(File cssFolder) {
        Iterator<File> filesIter = Arrays.asList(cssFolder.listFiles()).iterator();
        ArrayList<String> cssFilePaths = new ArrayList<String>();
        while (filesIter.hasNext()) {
            File file = filesIter.next();
            cssFilePaths.add(cssFolder.getName() + "/" + file.getName());
        }
        return cssFilePaths;
    }

    private File createTmpCSSFolder() {
        tmpFolderToCompressPath = tmpFolderToCompress();
        createTargetFolder(servletSession, tmpFolderToCompressPath);
        File cssFolder = createCssFolder(tmpFolderToCompressPath);
        includeContentViewCssFiles(cssFolder, exportContentData.getContent().getResourcesFolder());
        return cssFolder;
    }

    protected boolean exportedContentIsWebFile() {
        return ContentTypes.WEB_FILES.equals(exportContentData.getContent().getContentTypeId());
    }

    protected boolean exportedContentIsChaptersType() {
        return ContentTypes.CHAPTERS.equals(exportContentData.getContent().getContentTypeId());
    }

    protected String exportedFilePath(File targetFile) {
        return CmsConfig.getUrlPrefix("binary") + targetFile.getName();
    }

    private String tmpFolderToCompress() {
        return TMP_FOLDER_PATH + TMP_DIR_TO_COMPRESS + new Date().getTime();
    }

    protected abstract ImportExportFileUtil importExportFileUtilFactory();

    protected abstract String prefix();

    protected abstract String fileBaseName();

    protected abstract String getViewPathToExport();

    protected File targetFile() throws Exception {
        String prefix = prefix();
        File downloadDir = downloadDir();
        String fileBaseName = fileBaseName();
        if (StringUtils.isEmpty(fileBaseName)) fileBaseName = FILENONAME;
        return File.createTempFile(prefix + fileBaseName, ".zip", downloadDir);
    }

    private File createCssFolder(String targetFolderPath) {
        String cssTargetFolderPath = targetFolderPath + CSS_FOLDER_PATH;
        File cssFolder = new File(cssTargetFolderPath);
        cssFolder.mkdir();
        return cssFolder;
    }

    private void createTargetFolder(HttpSession servletSession, String targetFolderPath) {
        File targetFolder = new File(targetFolderPath);
        targetFolder.mkdir();
        TemporaryFilesHandler.register(null, targetFolder, servletSession);
    }

    private File downloadDir() throws Exception {
        String downloadPath = CmsConfig.getDefaultRepositoryPath();
        if (!new File(downloadPath).exists()) throw new Exception("Export destination folder not found, please create this folder: " + downloadPath);
        return new File(downloadPath);
    }

    protected String formatExportFileBaseName(String fileName) {
        return fileName.replaceAll(FILEREGEX, "_").replaceAll("_+", "_");
    }

    private void includeContentViewCssFiles(File targetFolder, DirectoryFolder contentResourcesFolder) {
        String targetFolderAbsolutePath = targetFolder.getAbsolutePath();
        DirectoryPersister directoryPersister = ManagerRegistry.getDirectoryPersister();
        for (String cssFileName : CmsToolsConfig.getExportCssFiles()) {
            writeCssFileIfNotFoundInResources(directoryPersister, contentResourcesFolder, targetFolderAbsolutePath, cssFileName);
        }
    }

    private void writeCssFileIfNotFoundInResources(DirectoryPersister directoryPersister, DirectoryFolder contentResourcesFolder, String targetFolderAbsolutePath, String cssFileName) {
        String sourceCssFilePath = CSS_FOLDER_PATH + "/" + cssFileName;
        String targetPathPrefix = targetFolderAbsolutePath + "/";
        if (log.isDebugEnabled()) log.debug("Css file name: " + cssFileName);
        if (!existsInResourcesFolder(contentResourcesFolder, directoryPersister, sourceCssFilePath)) {
            InputStream resourceAsStream = exportContentData.getServletContext().getResourceAsStream(sourceCssFilePath);
            if (resourceAsStream != null) {
                String targetCssFilePath = targetPathPrefix + cssFileName;
                writeStream(resourceAsStream, targetCssFilePath);
            } else log.warn("Css file " + cssFileName + " not found");
        }
    }

    private boolean existsInResourcesFolder(DirectoryFolder contentResourcesFolder, DirectoryPersister directoryPersister, String cssFilePath) {
        return directoryPersister.existFileInRootFolder(cssFilePath, contentResourcesFolder.getId(), contentResourcesFolder.getWorkspace());
    }

    private void writeStream(InputStream inputStream, String filePath) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
        } catch (FileNotFoundException e) {
            log.error("Cannot find target route for write resource: " + filePath);
        } catch (IOException e) {
            log.error("Reading resource", e);
        } finally {
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
            }
        }
    }

    private void writeFile(String textToWrite, String filePath) {
        PrintWriter writer = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            writer = new PrintWriter(fos);
            writer.print(textToWrite);
            writer.flush();
        } catch (Exception e) {
            String errorMessage = "Error writing file: " + filePath;
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    String errorMessage = "Error trying to create file " + filePath;
                    throw new CMSRuntimeException(errorMessage, e);
                }
            }
        }
    }
}
