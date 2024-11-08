package net.mitnet.tools.pdf.book.publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import net.mitnet.tools.pdf.book.io.FileExtensionConstants;
import net.mitnet.tools.pdf.book.io.FileHelper;
import net.mitnet.tools.pdf.book.io.FileNameHelper;
import net.mitnet.tools.pdf.book.model.toc.Toc;
import net.mitnet.tools.pdf.book.model.toc.TocBuilder;
import net.mitnet.tools.pdf.book.model.toc.TocTemplateDataBuilder;
import net.mitnet.tools.pdf.book.model.toc.TocTracer;
import net.mitnet.tools.pdf.book.openoffice.converter.OpenOfficeDocConverter;
import net.mitnet.tools.pdf.book.openoffice.net.OpenOfficeServerContext;
import net.mitnet.tools.pdf.book.openoffice.reports.OpenOfficeReportBuilder;
import net.mitnet.tools.pdf.book.pdf.builder.PdfBookBuilder;
import net.mitnet.tools.pdf.book.pdf.builder.PdfBookBuilderConfig;
import net.mitnet.tools.pdf.book.pdf.event.PdfPageEventLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfCopyFields;
import com.lowagie.text.pdf.PdfPageEvent;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.SimpleBookmark;
import com.lowagie.toolbox.plugins.HtmlBookmarks;
import com.lowagie.toolbox.plugins.InspectPDF;
import com.lowagie.toolbox.plugins.XML2Bookmarks;

/**
 * Book Publisher.
 * 
 * TODO: Review process to create NUP PDF books with TOC and embedded bookmarks
 * TODO: Refactor class variables into config class
 * 
 * @author Tim Telcik <telcik@gmail.com>
 * 
 * @see com.lowagie.toolbox.plugins.Handouts
 * @see com.lowagie.toolbox.plugins.NUp
 * @see HtmlBookmarks
 * @see InspectPDF
 * @see SimpleBookmark
 * @see XML2Bookmarks
 */
public class BookPublisher {

    private BookPublisherConfig config = new BookPublisherConfig();

    public BookPublisher() {
        getConfig().setServerContext(new OpenOfficeServerContext());
        getConfig().setPageSize(PageSize.A4);
    }

    public BookPublisher(Rectangle pageSize) {
        getConfig().setServerContext(new OpenOfficeServerContext());
        getConfig().setPageSize(pageSize);
    }

    public BookPublisher(OpenOfficeServerContext serverContext, Rectangle pageSize) {
        getConfig().setServerContext(serverContext);
        getConfig().setPageSize(pageSize);
    }

    public void setConfig(BookPublisherConfig config) {
        if (config == null) {
            this.config = new BookPublisherConfig();
        } else {
            this.config = config;
        }
    }

    public BookPublisherConfig getConfig() {
        return this.config;
    }

    /**
	 * Publishes the OpenOffice source documents into a single PDF booklet.
	 *
	 * This method chains together the OpenOffice to PDF conversion process and PDF book assembly.
	 * 
	 * TODO: refactor
	 * 
	 * @param sourceDir
	 * @param outputDir
	 * @param outputBookFile
	 * @param progresMonitor
	 * @throws Exception
	 */
    public void publish(File sourceDir, File outputDir, File outputBookFile) throws Exception {
        if (isVerboseEnabled()) {
            verbose("Publishing from \"" + sourceDir + " to \"" + outputDir + "\" as book \"" + outputBookFile + "\" ...");
        }
        if (isDebugEnabled()) {
            debug("sourceDir: " + sourceDir);
            debug("outputDir: " + outputDir);
            debug("outputBookFile: " + outputBookFile);
            debug("progressMonitor: " + getConfig().getProgressMonitor());
        }
        File tempDir = FileHelper.getSystemTempDir();
        if (isDebugEnabled()) {
            debug("tempDir: " + tempDir);
        }
        OpenOfficeDocConverter openOfficeDocConverter = new OpenOfficeDocConverter(getConfig().getServerContext());
        openOfficeDocConverter.setDebugEnabled(isDebugEnabled());
        openOfficeDocConverter.setVerboseEnabled(isVerboseEnabled());
        openOfficeDocConverter.setProgressMonitor(getConfig().getProgressMonitor());
        openOfficeDocConverter.convertDocuments(sourceDir, outputDir, OpenOfficeDocConverter.OUTPUT_FORMAT_PDF);
        TocBuilder tocBuilder = null;
        if (getConfig().isBuildTocEnabled()) {
            tocBuilder = new TocBuilder();
            if (isDebugEnabled()) {
                debug("tocBuilder: " + tocBuilder);
            }
        }
        PdfBookBuilderConfig pdfConfig = new PdfBookBuilderConfig();
        PdfPageEvent pdfPageEventListener = new PdfPageEventLogger();
        File pdfSourceDir = outputDir;
        PdfBookBuilder pdfBookBuilder = new PdfBookBuilder();
        pdfConfig.setPageSize(getConfig().getPageSize());
        pdfConfig.setVerboseEnabled(getConfig().isVerboseEnabled());
        pdfConfig.setMetaTitle(getConfig().getMetaTitle());
        pdfConfig.setMetaAuthor(getConfig().getMetaAuthor());
        pdfConfig.setProgressMonitor(getConfig().getProgressMonitor());
        pdfConfig.setTocRowChangeListener(tocBuilder);
        pdfConfig.setPdfPageEventListener(pdfPageEventListener);
        pdfBookBuilder.setConfig(pdfConfig);
        pdfBookBuilder.buildBook(pdfSourceDir, outputBookFile);
        if (getConfig().isBuildTocEnabled()) {
            Toc toc = tocBuilder.getToc();
            if (isVerboseEnabled()) {
                verbose("Output PDF Table Of Contents contains " + toc.getTocRowCount() + " entries");
            }
            if (isDebugEnabled()) {
                debug("Output PDF Table Of Contents is " + toc);
                TocTracer.traceToc(toc);
            }
            File tocTemplateFile = getTocTemplateFile();
            debug("tocTemplateFile: " + tocTemplateFile);
            File tocOutputFile = new File(tempDir, getTempTocFileName());
            debug("tocOutputFile: " + tocOutputFile);
            buildTocDoc(tocTemplateFile, toc, tocOutputFile);
            File tocSourceFile = tocOutputFile;
            openOfficeDocConverter.convertDocument(tocSourceFile, tempDir, OpenOfficeDocConverter.OUTPUT_FORMAT_PDF);
            if (isVerboseEnabled()) {
                verbose("Merging TOC with book");
            }
            String firstPdfName = FileNameHelper.rewriteFileNameSuffix(tocSourceFile, FileExtensionConstants.PDF_EXTENSION);
            File firstPdf = new File(tempDir, firstPdfName);
            File secondPdf = outputBookFile;
            String concatName = FileNameHelper.rewriteFileNameSuffix(outputBookFile, "-plus-toc", FileExtensionConstants.PDF_EXTENSION);
            File concatPdf = new File(outputBookFile.getParent(), concatName);
            concatPdf(firstPdf, secondPdf, concatPdf);
            if (concatPdf.exists()) {
                FileUtils.copyFile(concatPdf, outputBookFile);
                FileUtils.deleteQuietly(concatPdf);
            }
        }
    }

    private String getTempTocFileName() {
        return "toc" + FileExtensionConstants.OPEN_DOC_TEXT_EXTENSION;
    }

    private File getTocTemplateFile() throws IOException {
        File tocTemplateFile = null;
        String templatePath = getConfig().getTocTemplatePath();
        if (isDebugEnabled()) {
            debug("templatePath: " + templatePath);
        }
        if (StringUtils.isEmpty(templatePath)) {
            templatePath = BookPublisherConfig.DEFAULT_TOC_TEMPLATE_PATH;
            if (isDebugEnabled()) {
                debug("templatePath: " + templatePath);
            }
            URL tocTemplateUrl = getClass().getClassLoader().getResource(templatePath);
            if (isDebugEnabled()) {
                debug("tocTemplateUrl: " + tocTemplateUrl);
            }
            if (tocTemplateUrl != null) {
                tocTemplateFile = File.createTempFile("toc-template", FileExtensionConstants.OPEN_DOC_TEXT_EXTENSION);
                tocTemplateFile.deleteOnExit();
                FileUtils.copyURLToFile(tocTemplateUrl, tocTemplateFile);
            }
        } else {
            tocTemplateFile = new File(templatePath);
        }
        if (isDebugEnabled()) {
            debug("tocTemplateFile: " + tocTemplateFile);
        }
        return tocTemplateFile;
    }

    private void buildTocDoc(File tocTemplateFile, Toc toc, File tocOutputFile) throws Exception {
        if (isVerboseEnabled()) {
            verbose("Building TOC doc");
            verbose("Template file is " + tocTemplateFile);
            verbose("TOC output file is " + tocOutputFile);
        }
        if (tocTemplateFile == null) {
            throw new Exception("TOC template file is null");
        }
        if (tocOutputFile == null) {
            throw new Exception("TOC output file is null");
        }
        if ((tocTemplateFile != null) && (tocOutputFile != null)) {
            if (tocTemplateFile.exists()) {
                Map tocTemplateDataMap = TocTemplateDataBuilder.buildTocTemplateData(toc);
                if (isDebugEnabled()) {
                    debug("tocTemplateDataMap: " + tocTemplateDataMap);
                    debug("Generating TOC report");
                }
                OpenOfficeReportBuilder reportBuilder = new OpenOfficeReportBuilder();
                reportBuilder.setDebugEnabled(isDebugEnabled());
                reportBuilder.setVerboseEnabled(isVerboseEnabled());
                reportBuilder.buildReport(tocTemplateFile, tocTemplateDataMap, tocOutputFile);
            } else {
                System.err.println("Template file " + tocTemplateFile + " does not exist - skipping TOC build phase");
            }
        }
    }

    /**
	 * TODO: review concat process and compare to PdfCopy.
	 */
    public void concatPdf(File firstPdf, File secondPdf, File concatPdf) throws IOException, DocumentException {
        if (isDebugEnabled()) {
            debug("firstPdf: " + firstPdf);
            debug("secondPdf: " + secondPdf);
            debug("concatPdf: " + concatPdf);
            debug("concat PDFs");
        }
        PdfReader firstReader = new PdfReader(firstPdf.getPath());
        PdfReader secondReader = new PdfReader(secondPdf.getPath());
        PdfCopyFields copy = new PdfCopyFields(new FileOutputStream(concatPdf.getPath()));
        copy.addDocument(firstReader);
        copy.addDocument(secondReader);
        copy.close();
    }

    private boolean isDebugEnabled() {
        return getConfig().isDebugEnabled();
    }

    private boolean isVerboseEnabled() {
        return getConfig().isVerboseEnabled();
    }

    private void debug(String msg) {
        if (isDebugEnabled()) {
            System.out.println("-- " + msg);
        }
    }

    private void verbose(String msg) {
        if (isVerboseEnabled()) {
            System.out.println(msg);
        }
    }
}
