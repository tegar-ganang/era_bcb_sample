package org.alfresco.extension.pdftoolkit.repo.action.executer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.util.TempFileProvider;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.apache.pdfbox.util.Splitter;

/**
 * Insert PDF action executer
 * 
 * @author Jared Ottley
 * 
 */
public class PDFInsertAtPageActionExecuter extends BasePDFActionExecuter {

    /**
	 * The logger
	 */
    private static Log logger = LogFactory.getLog(PDFInsertAtPageActionExecuter.class);

    /**
	 * Action constants
	 */
    public static final String NAME = "pdf-insert-at-page";

    public static final String PARAM_DESTINATION_FOLDER = "destination-folder";

    public static final String PARAM_INSERT_AT_PAGE = "insert-at-page";

    public static final String PARAM_DESTINATION_NAME = "destination-name";

    public static final String PARAM_INSERT_CONTENT = "insert-content";

    /**
	 * Add parameter definitions
	 */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER, DataTypeDefinition.NODE_REF, true, getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
        paramList.add(new ParameterDefinitionImpl(PARAM_INSERT_AT_PAGE, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_INSERT_AT_PAGE)));
        paramList.add(new ParameterDefinitionImpl(PARAM_INSERT_CONTENT, DataTypeDefinition.NODE_REF, true, getParamDisplayLabel(PARAM_INSERT_CONTENT)));
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
        ContentReader insertContentReader = getReader((NodeRef) ruleAction.getParameterValue(PARAM_INSERT_CONTENT));
        if (contentReader != null) {
            doInsert(ruleAction, actionedUponNodeRef, contentReader, insertContentReader);
            {
                if (logger.isDebugEnabled()) {
                    logger.debug("Can't execute rule: \n" + "   node: " + actionedUponNodeRef + "\n" + "   reader: " + contentReader + "\n" + "   action: " + this);
                }
            }
        }
    }

    /**
	 * 
	 * Build out the insert call
	 * 
	 */
    protected void doInsert(Action ruleAction, NodeRef actionedUponNodeRef, ContentReader contentReader, ContentReader insertContentReader) {
        Map<String, Object> options = new HashMap<String, Object>(5);
        options.put(PARAM_DESTINATION_NAME, ruleAction.getParameterValue(PARAM_DESTINATION_NAME));
        options.put(PARAM_DESTINATION_FOLDER, ruleAction.getParameterValue(PARAM_DESTINATION_FOLDER));
        options.put(PARAM_INSERT_AT_PAGE, ruleAction.getParameterValue(PARAM_INSERT_AT_PAGE));
        try {
            this.action(ruleAction, actionedUponNodeRef, contentReader, insertContentReader, options);
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
    protected final void action(Action ruleAction, NodeRef actionedUponNodeRef, ContentReader reader, ContentReader insertReader, Map<String, Object> options) throws AlfrescoRuntimeException {
        PDDocument pdf = null;
        PDDocument insertContentPDF = null;
        InputStream is = null;
        InputStream cis = null;
        File tempDir = null;
        ContentWriter writer = null;
        try {
            int insertAt = new Integer(((String) options.get(PARAM_INSERT_AT_PAGE))).intValue();
            is = reader.getContentInputStream();
            cis = insertReader.getContentInputStream();
            pdf = PDDocument.load(is);
            insertContentPDF = PDDocument.load(cis);
            Splitter splitter = new Splitter();
            splitter.setSplitAtPage(insertAt - 1);
            List pdfs = splitter.split(pdf);
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.appendDocument((PDDocument) pdfs.get(0), insertContentPDF);
            merger.appendDocument((PDDocument) pdfs.get(0), (PDDocument) pdfs.get(1));
            merger.setDestinationFileName(options.get(PARAM_DESTINATION_NAME).toString());
            merger.mergeDocuments();
            File alfTempDir = TempFileProvider.getTempDir();
            tempDir = new File(alfTempDir.getPath() + File.separatorChar + actionedUponNodeRef.getId());
            tempDir.mkdir();
            String fileName = options.get(PARAM_DESTINATION_NAME).toString();
            PDDocument completePDF = (PDDocument) pdfs.get(0);
            completePDF.save(tempDir + "" + File.separatorChar + fileName + FILE_EXTENSION);
            if (completePDF != null) {
                try {
                    completePDF.close();
                } catch (Throwable e) {
                    e.printStackTrace();
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
}
