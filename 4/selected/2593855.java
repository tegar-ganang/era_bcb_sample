package org.alfresco.extension.pdftoolkit.repo.action.executer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.Splitter;

/**
 * Split PDF action executer
 * 
 * @author Jared Ottley
 * 
 */
public class PDFSplitActionExecuter extends BasePDFActionExecuter {

    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(PDFSplitActionExecuter.class);

    /**
     * Action constants
     */
    public static final String NAME = "pdf-split";

    public static final String PARAM_DESTINATION_FOLDER = "destination-folder";

    public static final String PARAM_SPLIT_FREQUENCY = "split-frequency";

    /**
     * Add parameter definitions
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER, DataTypeDefinition.NODE_REF, true, getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
        paramList.add(new ParameterDefinitionImpl(PARAM_SPLIT_FREQUENCY, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_SPLIT_FREQUENCY)));
    }

    /**
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.repository.NodeRef,
     *      org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef) {
        if (serviceRegistry.getNodeService().exists(actionedUponNodeRef) == false) {
            return;
        }
        ContentReader contentReader = getReader(actionedUponNodeRef);
        if (contentReader != null) {
            doSplit(ruleAction, actionedUponNodeRef, contentReader);
            {
                if (logger.isDebugEnabled()) {
                    logger.debug("Can't execute rule: \n" + "   node: " + actionedUponNodeRef + "\n" + "   reader: " + contentReader + "\n" + "   action: " + this);
                }
            }
        }
    }

    /**
     * @see org.alfresco.repo.action.executer.TransformActionExecuter#doTransform(org.alfresco.service.cmr.action.Action,
     *      org.alfresco.service.cmr.repository.ContentReader, org.alfresco.service.cmr.repository.ContentWriter)
     */
    protected void doSplit(Action ruleAction, NodeRef actionedUponNodeRef, ContentReader contentReader) {
        Map<String, Object> options = new HashMap<String, Object>(5);
        options.put(PARAM_DESTINATION_FOLDER, ruleAction.getParameterValue(PARAM_DESTINATION_FOLDER));
        options.put(PARAM_SPLIT_FREQUENCY, ruleAction.getParameterValue(PARAM_SPLIT_FREQUENCY));
        try {
            this.action(ruleAction, actionedUponNodeRef, contentReader, options);
        } catch (AlfrescoRuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param reader
     * @param writer
     * @param options
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    protected final void action(Action ruleAction, NodeRef actionedUponNodeRef, ContentReader reader, Map<String, Object> options) throws AlfrescoRuntimeException {
        PDDocument pdf = null;
        InputStream is = null;
        File tempDir = null;
        ContentWriter writer = null;
        try {
            int splitFrequency = 0;
            String splitFrequencyString = options.get(PARAM_SPLIT_FREQUENCY).toString();
            if (!splitFrequencyString.equals("")) {
                splitFrequency = new Integer(splitFrequencyString);
            }
            is = reader.getContentInputStream();
            pdf = PDDocument.load(is);
            Splitter splitter = new Splitter();
            if (splitFrequency > 0) {
                splitter.setSplitAtPage(splitFrequency);
            }
            List pdfs = splitter.split(pdf);
            Iterator it = pdfs.iterator();
            int page = 1;
            int endPage = 0;
            File alfTempDir = TempFileProvider.getTempDir();
            tempDir = new File(alfTempDir.getPath() + File.separatorChar + actionedUponNodeRef.getId());
            tempDir.mkdir();
            while (it.hasNext()) {
                String pagePlus = "";
                String pg = "_pg";
                PDDocument splitpdf = (PDDocument) it.next();
                int pagesInPDF = splitpdf.getNumberOfPages();
                if (splitFrequency > 0) {
                    endPage = endPage + pagesInPDF;
                    pagePlus = "-" + endPage;
                    pg = "_pgs";
                }
                String fileNameSansExt = getFilenameSansExt(actionedUponNodeRef, FILE_EXTENSION);
                splitpdf.save(tempDir + "" + File.separatorChar + fileNameSansExt + pg + page + pagePlus + FILE_EXTENSION);
                if (splitFrequency > 0) {
                    page = (page++) + pagesInPDF;
                } else {
                    page++;
                }
                if (splitpdf != null) {
                    try {
                        splitpdf.close();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
            for (File file : tempDir.listFiles()) {
                try {
                    if (file.isFile()) {
                        String filename = file.getName();
                        writer = getWriter(filename, (NodeRef) ruleAction.getParameterValue(PARAM_DESTINATION_FOLDER));
                        writer.setEncoding(reader.getEncoding());
                        writer.setMimetype(FILE_MIMETYPE);
                        writer.putContent(file);
                        file.delete();
                    }
                } catch (FileExistsException e) {
                    throw new AlfrescoRuntimeException("Failed to process file.", e);
                }
            }
        } catch (COSVisitorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pdf != null) {
                try {
                    pdf.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            if (tempDir != null) {
                tempDir.delete();
            }
        }
        if (logger.isDebugEnabled()) {
        }
    }

    /**
     * @param fileName
     * @param extension
     * @return
     */
    public String removeExtension(String fileName, String extension) {
        if (fileName != null) {
            if (fileName.contains(extension)) {
                int extensionStartsAt = fileName.indexOf(extension);
                fileName = fileName.substring(0, extensionStartsAt);
            }
        }
        return fileName;
    }

    protected String getFilename(NodeRef actionedUponNodeRef) {
        FileInfo fileInfo = serviceRegistry.getFileFolderService().getFileInfo(actionedUponNodeRef);
        String filename = fileInfo.getName();
        return filename;
    }

    protected String getFilenameSansExt(NodeRef actionedUponNodeRef, String extension) {
        String filenameSansExt;
        filenameSansExt = removeExtension(getFilename(actionedUponNodeRef), extension);
        return filenameSansExt;
    }
}
