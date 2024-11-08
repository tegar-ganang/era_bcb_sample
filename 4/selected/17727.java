package net.sf.poormans.view.renderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sf.poormans.Constants;
import net.sf.poormans.configuration.InitializationManager;
import net.sf.poormans.configuration.resource.LabelHolder;
import net.sf.poormans.exception.FatalException;
import net.sf.poormans.exception.RenderingException;
import net.sf.poormans.model.domain.IRenderable;
import net.sf.poormans.model.domain.InstanceUtil;
import net.sf.poormans.model.domain.PojoInfo;
import net.sf.poormans.model.domain.PojoPathInfo;
import net.sf.poormans.model.domain.pojo.Gallery;
import net.sf.poormans.model.domain.pojo.Image;
import net.sf.poormans.model.domain.pojo.Level;
import net.sf.poormans.model.domain.pojo.Page;
import net.sf.poormans.model.domain.pojo.Site;
import net.sf.poormans.tool.ChecksumTool;
import net.sf.poormans.tool.compression.Zip;
import net.sf.poormans.tool.file.FileTool;
import net.sf.poormans.tool.swt.SWTUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Generates the static pages to the export directory of a site.
 * 
 * <b>Export rules: </b>
 * <ul>
 * <li>Every level is a directory.</li>
 * <li>The main welcome file (at the root directory of a site) redirects to the welcome file in the root level.</li>
 * <li>All welcome pages named with the index file name of the site.</li>
 * </ul>
 * The following pattern is required:
 * <pre>
 *	IExportRenderer exportRenderer = (IExportRenderer) InitializationManager.getBean("exportrenderer");
 *	exportRenderer.setSite(site);
 *	exportRenderer.setMessages(messages);
 *	exportRenderer.init();
 *	if (!exportRenderer.isValidToExport())
 *		WARNING
 *	else
 *		DialogManager.startProgressDialog(shell, exportRenderer);
 * </pre>
 * 
 * @version $Id: ExportRendererThreaded.java 1983 2010-04-24 15:46:36Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class ExportRendererThreaded implements IExportRenderer {

    private static Logger logger = Logger.getLogger(ExportRendererThreaded.class);

    private Site site;

    private File exportDir;

    private Set<IRenderable> renderableObjects = new HashSet<IRenderable>();

    private IProgressMonitor monitor = null;

    private StringBuilder messages;

    private IExportThreadController controller;

    private boolean isInterruptByUser = false;

    private RenderData renderData = (RenderData) InitializationManager.getBean("renderData");

    public ExportRendererThreaded(final IExportThreadController controller) {
        this.controller = controller;
    }

    public void setSite(final Site site) {
        this.site = site;
        this.exportDir = new File(PojoPathInfo.getSiteExportPath(this.site));
    }

    public void setMessages(final StringBuilder messages) {
        this.messages = messages;
    }

    public void init() {
        if (site == null) throw new IllegalArgumentException("Missing site object!");
        try {
            if (!this.exportDir.exists()) this.exportDir.mkdirs(); else FileUtils.cleanDirectory(this.exportDir);
        } catch (IOException e) {
            throw new RuntimeException("While checking/cleaning the export path:" + e.getMessage(), e);
        }
        collectRenderables(site, renderableObjects);
    }

    private void collectRenderables(final Level level, Collection<IRenderable> renderables) {
        for (Level tmpContainer : level.getSublevels()) {
            collectRenderables(tmpContainer, renderables);
        }
        if (!InstanceUtil.isSite(level) && CollectionUtils.isEmpty(level.getPages())) {
            messages.append(LabelHolder.get("task.export.error.pojo.levelhasnopage"));
            messages.append(level.getDecorationString());
            messages.append('\n');
            renderables.clear();
            return;
        }
        for (Page page : level.getPages()) {
            renderables.add(page);
            if (InstanceUtil.isGallery(page)) {
                Set<Image> images = ((Gallery) page).getImages();
                if (CollectionUtils.isEmpty(images)) {
                    messages.append(LabelHolder.get("task.export.error.pojo.galleryhasnoimage"));
                    messages.append(page.getDecorationString());
                    messages.append('\n');
                    renderableObjects.clear();
                    return;
                } else renderables.addAll(images);
            }
        }
    }

    /**
	 * Start the export of the static html pages.
	 * 
	 * @throws RuntimeException if an exception is happened during rendering.
	 */
    public void run() {
        logger.debug("Entered run.");
        File siteDir = new File(PojoPathInfo.getSitePath(this.site)).getAbsoluteFile();
        if (monitor != null) monitor.beginTask(LabelHolder.get("task.export.monitor") + this.renderableObjects.size(), this.renderableObjects.size());
        if (CollectionUtils.isEmpty(site.getPages())) renderRedirector();
        try {
            renderRenderables();
            if (!controller.isError() && !isInterruptByUser) {
                logger.debug("Static export successfull!");
                Collection<File> exportedFiles = FileTool.collectFiles(exportDir);
                if (monitor != null) {
                    monitor.done();
                    monitor.beginTask("Calculate checksums", exportedFiles.size());
                }
                Document dom = ChecksumTool.getDomChecksums(ChecksumTool.get(exportedFiles, exportDir.getAbsolutePath(), this.monitor));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                OutputFormat outformat = OutputFormat.createPrettyPrint();
                outformat.setEncoding(Constants.XML_ENCODING);
                XMLWriter writer = new XMLWriter(out, outformat);
                writer.write(dom);
                writer.flush();
                String formatedDomString = out.toString();
                InputStream in = new ByteArrayInputStream(formatedDomString.getBytes());
                Map<InputStream, String> toCompress = new HashMap<InputStream, String>();
                toCompress.put(in, InitializationManager.getProperty("poormans.filename.checksums"));
                File zipFile = new File(PojoPathInfo.getSiteExportPath(site), FilenameUtils.getBaseName(InitializationManager.getProperty("poormans.filename.checksums")) + ".zip");
                Zip.compress(zipFile, toCompress);
                zipFile = null;
                Set<File> filesToCopy = renderData.getFilesToCopy();
                for (File srcFile : filesToCopy) {
                    String exportPathPart = srcFile.getAbsolutePath().substring(siteDir.getAbsolutePath().length() + 1);
                    File destFile = new File(exportDir, exportPathPart);
                    if (srcFile.isFile()) FileUtils.copyFile(srcFile, destFile); else FileUtils.copyDirectoryToDirectory(srcFile, destFile.getParentFile());
                }
            } else FileUtils.cleanDirectory(exportDir);
        } catch (Exception e) {
            logger.error("Error while export: " + e.getMessage(), e);
            throw new FatalException("Error while export " + this.site.getUrl() + e.getMessage(), e);
        } finally {
            if (monitor != null) monitor.done();
        }
    }

    private void renderRenderables() throws RenderingException {
        controller.addAll(ExportRenderThread.buildThreads(controller, renderableObjects));
        Thread tmpController = (Thread) this.controller;
        SWTUtils.asyncExec(tmpController);
        long oldThreadCount = controller.threadCount();
        do {
            isInterruptByUser = (monitor != null && monitor.isCanceled());
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                logger.debug("Controller interrupted.");
            }
            long threadCount = controller.threadCount();
            if (oldThreadCount > threadCount) {
                incProgressValue(oldThreadCount - threadCount);
                oldThreadCount = threadCount;
            }
        } while (!controller.isError() && !controller.isIdle() && !isInterruptByUser);
        if (controller.isError()) throw new RenderingException(controller.getThreadException());
        if (isInterruptByUser) {
            controller.cancel();
            try {
                FileUtils.cleanDirectory(exportDir);
            } catch (IOException e) {
                logger.error("While cleaning the export directory: " + e.getLocalizedMessage(), e);
            }
            logger.debug("Export was interrupt by user, export dir will be deleted!");
        }
    }

    /**
	 * Create a static html page, which redirects to the welcome page of the root level.
	 */
    private void renderRedirector() {
        File redFile = new File(InitializationManager.getDefaultResourcesPath(), "redirector.html");
        if (!redFile.exists()) throw new RuntimeException("Default redirector not found: " + redFile);
        File outputFile = new File(this.exportDir, this.site.getIndexPageName());
        String linkToRootPage = PojoInfo.getRootLevel(this.site).getName().concat("/").concat(this.site.getIndexPageName());
        try {
            Map<String, Object> ctxObjs = new HashMap<String, Object>(1);
            ctxObjs.put("linktorootpage", linkToRootPage);
            VelocityRenderer velocityRenderer = (VelocityRenderer) InitializationManager.getBean("velocityRenderer");
            String renderedRed = velocityRenderer.renderString(FileUtils.readFileToString(redFile), ctxObjs);
            FileUtils.writeStringToFile(outputFile, renderedRed);
        } catch (IOException e) {
            throw new FatalException("Error while writing the redirector: " + e.getMessage(), e);
        }
    }

    public boolean isValidToExport() {
        return (renderableObjects.size() > 0);
    }

    public boolean isInterruptByUser() {
        return isInterruptByUser;
    }

    public void setMonitor(final IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    private void incProgressValue(long worked) {
        if (monitor != null) monitor.worked((int) worked);
    }
}
