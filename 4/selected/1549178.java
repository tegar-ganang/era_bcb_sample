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
import net.sf.poormans.gui.IProgressViewer;
import net.sf.poormans.model.IRenderable;
import net.sf.poormans.model.domain.PoInfo;
import net.sf.poormans.model.domain.PoPathInfo;
import net.sf.poormans.model.domain.pojo.Site;
import net.sf.poormans.tool.ChecksumTool;
import net.sf.poormans.tool.PathTool;
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
import org.eclipse.swt.widgets.Display;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
 *	ExportRenderer exportRenderer = InitializationManager.getBean("exportRenderer");
 *	exportRenderer.setSite(site);
 *	exportRenderer.setMessages(messages);
 *	exportRenderer.init();
 *	if (!exportRenderer.isValidToExport())
 *		WARNING
 *	else
 *		DialogManager.startProgressDialog(shell, exportRenderer);
 * </pre>
 * 
 * @version $Id: ExportRenderer.java 2135 2011-07-25 13:07:43Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
@Service()
public class ExportRenderer implements IProgressViewer {

    private static Logger logger = Logger.getLogger(ExportRenderer.class);

    private Site site;

    private File exportDir;

    private Set<IRenderable> renderableObjects;

    private IProgressMonitor monitor = null;

    private StringBuilder messages;

    private boolean isInterruptByUser = false;

    private Display display = null;

    private ExportThreadPoolController exportController;

    @Autowired
    private RenderData renderData;

    @Autowired
    private VelocityRenderer renderer;

    @Value("${poormans.export.maxthreadspercore}")
    private int maxThreadsPerCount;

    @Value("${poormans.filename.checksums}")
    private String checksumFilename;

    @Value("${poormans.export.extention}")
    private String poExtension;

    public void setSite(final Site site) {
        this.site = site;
        this.exportDir = PoPathInfo.getSiteExportDirectory(this.site);
    }

    public void setMessages(final StringBuilder messages) {
        this.messages = messages;
    }

    public void setDisplay(Display display) {
        this.display = display;
    }

    public void init() {
        try {
            if (!this.exportDir.exists()) this.exportDir.mkdirs(); else FileUtils.cleanDirectory(this.exportDir);
        } catch (IOException e) {
            throw new RuntimeException("While checking/cleaning the export path:" + e.getMessage(), e);
        }
        renderableObjects = PoInfo.collectRenderables(site, messages);
        exportController = new ExportThreadPoolController(maxThreadsPerCount);
        logger.info("Site successfully exported.");
    }

    /**
	 * Start the export of the static html pages.
	 * 
	 * @throws RuntimeException if an exception is happened during rendering.
	 */
    @Override
    public void run() {
        logger.debug("Entered run.");
        File siteDir = PoPathInfo.getSiteDirectory(this.site);
        if (monitor != null) monitor.beginTask(String.format("%s: %d", LabelHolder.get("task.export.monitor"), this.renderableObjects.size()), this.renderableObjects.size());
        if (CollectionUtils.isEmpty(site.getPages())) renderRedirector();
        try {
            FileUtils.cleanDirectory(exportDir);
            for (IRenderable ro : renderableObjects) {
                File dir = PathTool.getExportFile(ro, poExtension).getParentFile();
                if (!dir.exists()) dir.mkdirs();
            }
            renderRenderables();
            if (!exportController.isError() && !isInterruptByUser) {
                logger.debug("Static export successfull!");
                Set<File> filesToCopy = renderData.getFilesToCopy();
                for (File srcFile : filesToCopy) {
                    String exportPathPart = srcFile.getAbsolutePath().substring(siteDir.getAbsolutePath().length() + 1);
                    File destFile = new File(exportDir, exportPathPart);
                    if (srcFile.isFile()) FileUtils.copyFile(srcFile, destFile); else FileUtils.copyDirectoryToDirectory(srcFile, destFile.getParentFile());
                }
                logger.debug("Extra files successful copied!");
                Collection<File> exportedFiles = FileTool.collectFiles(exportDir);
                if (monitor != null) {
                    monitor.done();
                    monitor.beginTask("Calculate checksums", exportedFiles.size());
                }
                Document dom = ChecksumTool.getDomChecksums(ChecksumTool.get(exportedFiles, exportDir.getAbsolutePath(), monitor));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                OutputFormat outformat = OutputFormat.createPrettyPrint();
                outformat.setEncoding(Constants.STANDARD_ENCODING);
                XMLWriter writer = new XMLWriter(out, outformat);
                writer.write(dom);
                writer.flush();
                String formatedDomString = out.toString();
                InputStream in = new ByteArrayInputStream(formatedDomString.getBytes());
                Map<InputStream, String> toCompress = new HashMap<InputStream, String>();
                toCompress.put(in, checksumFilename);
                File zipFile = new File(PoPathInfo.getSiteExportDirectory(site), FilenameUtils.getBaseName(checksumFilename) + ".zip");
                Zip.compress(zipFile, toCompress);
                zipFile = null;
            } else FileUtils.cleanDirectory(exportDir);
        } catch (Exception e) {
            logger.error("Error while export: " + e.getMessage(), e);
            throw new FatalException("Error while export " + this.site.getUrl() + e.getMessage(), e);
        } finally {
            if (monitor != null) monitor.done();
        }
    }

    private void renderRenderables() throws RenderingException {
        exportController.addAll(buildThreads(exportController, renderableObjects));
        SWTUtils.asyncExec(exportController, display);
        int oldThreadCount = 0;
        do {
            isInterruptByUser = (monitor != null && monitor.isCanceled());
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                logger.debug("Controller interrupted.");
            }
            int threadCount = exportController.getTerminatedThreadCount();
            if (oldThreadCount < threadCount) {
                incProgressValue(threadCount - oldThreadCount);
                oldThreadCount = threadCount;
            }
        } while (!exportController.isError() && !exportController.isTerminated() && !isInterruptByUser);
        if (exportController.isError()) throw new RenderingException(exportController.getThreadException());
        if (isInterruptByUser) {
            exportController.cancel();
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
        String linkToRootPage = PoInfo.getRootLevel(this.site).getName().concat("/").concat(this.site.getIndexPageName());
        try {
            Map<String, Object> ctxObjs = new HashMap<String, Object>(1);
            ctxObjs.put("linktorootpage", linkToRootPage);
            String renderedRed = renderer.renderString(FileUtils.readFileToString(redFile), ctxObjs);
            FileUtils.writeStringToFile(outputFile, renderedRed);
        } catch (IOException e) {
            throw new FatalException("Error while writing the redirector: " + e.getMessage(), e);
        }
    }

    public boolean isValidToExport() {
        return (renderableObjects.size() > 0 && messages.length() == 0);
    }

    public boolean isInterruptByUser() {
        return isInterruptByUser;
    }

    @Override
    public void setMonitor(final IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    private void incProgressValue(int worked) {
        if (monitor != null) {
            monitor.worked(worked);
        }
    }

    private Collection<Thread> buildThreads(final ExportThreadPoolController controller, final Collection<IRenderable> renderables) {
        Set<Thread> objs = new HashSet<Thread>(renderables.size());
        for (IRenderable renderable : renderables) objs.add(new ExportRenderThread(controller, renderable, renderer, poExtension));
        return objs;
    }
}
