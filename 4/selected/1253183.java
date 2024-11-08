package org.jaffa.modules.printing.services;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PRAcroForm;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.SimpleBookmark;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.jaffa.modules.printing.services.exceptions.EngineInstantiationException;
import org.jaffa.modules.printing.services.exceptions.EngineProcessingException;
import org.jaffa.modules.printing.services.exceptions.FormPrintException;
import org.jaffa.session.ContextManagerFactory;
import org.jaffa.util.StringHelper;

/**
 * Generate Multiple forms at the same time and merge the results.
 * Each form can have a different template and data source, but they
 * all must use the same print engine.
 * @author PaulE
 */
public class MultiFormPrintEngine {

    private static final Logger log = Logger.getLogger(MultiFormPrintEngine.class);

    private String m_engineType = FormPrintFactory.ENGINE_TYPE_ITEXT;

    private List<byte[]> m_documents = new ArrayList<byte[]>();

    private byte[] m_output = null;

    private List<IFormPrintEngine> m_engines = new ArrayList<IFormPrintEngine>();

    private boolean m_canPrint;

    private boolean m_canCopy;

    private boolean m_canModify;

    private boolean m_processed = false;

    private boolean m_initialized = false;

    private Properties m_documentProperties = null;

    private List<String> m_templateFilenames = new ArrayList<String>();

    private List m_objectModels = new ArrayList();

    private String m_searchPath = null;

    private int m_totalPages = 0;

    /**
     * The core method in the engine is the generate() method, it must only be
     * called once, and must be called before you call any method that
     * accessed the generated PDF ( like writeForm() or getGeneratedForm() )
     * 
     * 
     * @throws FormPrintException This is thrown if there is any error in generating the PDF document.
     * The details will be in the message text
     */
    public void generate() throws FormPrintException {
        if (m_processed) throw new IllegalStateException("The form has already been processed");
        int pageOffset = 0;
        int index = -1;
        for (String templateName : m_templateFilenames) {
            index++;
            IFormPrintEngine engine = FormPrintFactory.newInstance(m_engineType);
            if (engine == null) {
                String err = "No Engine Created. Type=" + m_engineType;
                if (log.isDebugEnabled()) log.error(err);
                throw new EngineInstantiationException(err);
            }
            m_engines.add(engine);
            engine.setTemplateName(templateName);
            engine.setDataSource(m_objectModels.get(index));
            engine.setPermissions(m_canPrint, m_canCopy, m_canModify);
            if (m_documentProperties != null) engine.setDocumentProperties(m_documentProperties);
            if (m_searchPath != null) engine.setTemplateSearchPath(m_searchPath);
            engine.initialize();
            m_totalPages += engine.getTotalPages();
        }
        for (IFormPrintEngine engine : m_engines) {
            engine.setTotalPagesOverride(m_totalPages);
            engine.setCurrentPageOffset(pageOffset);
            engine.generate();
            m_documents.add(engine.getGeneratedForm());
            pageOffset += engine.getTotalPages();
        }
        try {
            if (FormPrintFactory.ENGINE_TYPE_ITEXT.equals(m_engineType) || FormPrintFactory.ENGINE_TYPE_PDFLIB.equals(m_engineType)) m_output = mergePdf(m_documents); else m_output = mergeText(m_documents);
        } catch (Exception e) {
            log.error("Failed to merge PDF documents", e);
            throw new EngineProcessingException("Merge Failed", e);
        }
        m_processed = true;
    }

    /**
     * Set the permissions on the generated PDF file. <B>NOTE: These must be
     * set prior to generating the PDF</B>
     * @param canPrint Allow generated PDF to be printed?
     * @param canCopy Allow generated PDF contents to be copied?
     * @param canModify Allow generated PDF contents to be modified?
     * @throws FormPrintException Throw if there was an error setting these properties.
     */
    public void setPermissions(boolean canPrint, boolean canCopy, boolean canModify) throws FormPrintException {
        m_canPrint = canPrint;
        m_canCopy = canCopy;
        m_canModify = canModify;
    }

    /** Return the generated document. The generate() method MUST be called
     * prior to this else an exception will be thrown
     * @throws FormPrintException thrown if any pdf access error occurs
     * @return the generated pdf as a byte array
     */
    public byte[] getGeneratedForm() throws FormPrintException {
        if (!m_processed) throw new IllegalStateException("The form not been processed yet");
        return m_output;
    }

    /** Write the Form to a temp file, returns the file handle, or null if it failed!
     * The file will typically end up in the java temp folder
     * ( <CODE>System.getProperty("java.io.tempdir")</CODE> )
     * @throws FormPrintException thrown if any pdf access error occurs
     * @return return the file handle to the file that was written containing
     * the generated PDF document. This will be null if there were any
     * write errors.
     */
    public File writeForm() throws FormPrintException {
        return writeForm(null);
    }

    /** Write the Form to a specified file
     * @param fileout file handle to use to write out the pdf
     * @throws FormPrintException thrown if any pdf access error occurs
     * @return return the file handle to the file that was written containing
     * the generated PDF document. This will be null if there were any
     * write errors.
     */
    public File writeForm(File fileout) throws FormPrintException {
        if (!m_processed) throw new IllegalStateException("The form not been processed yet");
        try {
            if (fileout == null) {
                String extn = ".txt";
                if (FormPrintFactory.ENGINE_TYPE_ITEXT.equals(m_engineType) || FormPrintFactory.ENGINE_TYPE_PDFLIB.equals(m_engineType)) extn = ".pdf";
                fileout = File.createTempFile("form_", extn);
            }
            OutputStream bos = new FileOutputStream(fileout);
            bos.write(m_output);
            bos.close();
        } catch (IOException e) {
            log.error("Error Writing out PDF", e);
            return null;
        }
        return fileout;
    }

    /**
     * Get template name being used to generate the final PDF
     * @return Template Name
     */
    public List<String> getTemplateNames() {
        return m_templateFilenames;
    }

    /**
     * Get object Models being used to generate the final PDF
     * @return Template Name
     */
    public List getObjectModels() {
        return m_objectModels;
    }

    /**
     * Get search path being used to locate the template
     * @return Search Path
     */
    public String getTemplateSearchPath() {
        return m_searchPath;
    }

    /**
     * Set template names
     * 
     * @param objectModel 
     * @param templateName Template Name
     */
    public void addDocument(String templateName, Object objectModel) {
        if (m_templateFilenames == null) m_templateFilenames = new ArrayList<String>();
        m_templateFilenames.add(templateName);
        if (m_objectModels == null) m_objectModels = new ArrayList();
        m_objectModels.add(objectModel);
    }

    /**
     * Set Search Path
     * @param templateSearchPath Search Path
     */
    public void setTemplateSearchPath(String templateSearchPath) {
        m_searchPath = templateSearchPath;
    }

    /**
     * Set Form Print Engine Type. See contants on the FormPrintFactory
     * @param engineType Type of engine to create for this printing
     */
    public void setEngineType(String engineType) {
        m_engineType = engineType;
    }

    /**
     * Get the type of engine being used to generate the output
     * @return engine being used, see FormPrintFactory for constant values
     */
    public String getEngineType() {
        return m_engineType;
    }

    /**
     * Getter for property totalPages.
     * @return Value of property totalPages.
     */
    public int getTotalPages() {
        return m_totalPages;
    }

    /**
     * Set the properties for the document
     * @param documentProperties 
     */
    public void setDocumentProperties(Properties documentProperties) {
        m_documentProperties = documentProperties;
    }

    /**
     * Merge a list of generated Pdf Documents together
     * @param documents 
     * @throws java.io.IOException 
     * @throws com.lowagie.text.DocumentException 
     * @return byte[]
     */
    public static byte[] mergePdf(List<byte[]> documents) throws IOException, DocumentException {
        int pageOffset = 0;
        ArrayList master = new ArrayList();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = null;
        PdfCopy writer = null;
        boolean first = true;
        for (Iterator<byte[]> it = documents.iterator(); it.hasNext(); ) {
            PdfReader reader = new PdfReader(it.next());
            reader.consolidateNamedDestinations();
            int n = reader.getNumberOfPages();
            List bookmarks = SimpleBookmark.getBookmark(reader);
            if (bookmarks != null) {
                if (pageOffset != 0) SimpleBookmark.shiftPageNumbers(bookmarks, pageOffset, null);
                master.addAll(bookmarks);
            }
            pageOffset += n;
            if (first) {
                first = false;
                document = new Document(reader.getPageSizeWithRotation(1));
                writer = new PdfCopy(document, output);
                document.open();
            }
            PdfImportedPage page;
            for (int i = 0; i < n; ) {
                ++i;
                page = writer.getImportedPage(reader, i);
                writer.addPage(page);
            }
            PRAcroForm form = reader.getAcroForm();
            if (form != null) writer.copyAcroForm(reader);
        }
        if (master.size() > 0) writer.setOutlines(master);
        if (document != null) document.close();
        return output.toByteArray();
    }

    /**
     * Merge a list of generated Text Documents together
     * @param documents 
     * @return byte[]
     */
    public static byte[] mergeText(List<byte[]> documents) {
        int size = 0;
        for (Iterator<byte[]> it = documents.iterator(); it.hasNext(); ) {
            byte[] d = it.next();
            size += d.length;
        }
        byte[] out = new byte[size];
        int pos = 0;
        for (Iterator<byte[]> it = documents.iterator(); it.hasNext(); ) {
            byte[] d = it.next();
            for (int i = 0; i < d.length; i++) out[pos++] = d[i];
        }
        return out;
    }

    /**
     * This allows a test page to be printed where the form is genrated
     * using the field names as the data for each field. Each engine may also
     * do extra stuff in test mode for example iText will outline the field boxes.
     * <p>
     * Factory Types are <CODE>iText</CODE>, <CODE>PDFlib</CODE>, <CODE>Velocity</CODE> and <CODE>BSF</CODE>
     * @param args <Template Name> [<Output Name>] [<Factory Type>]
     */
    public static void main(String[] args) {
        org.jaffa.util.LoggerHelper.init();
        if (args.length < 2 || args.length > 3) {
            System.out.println(MultiFormPrintEngine.class.getName() + " <Template Name> [<Output Name>] [<Factory Type>]");
            System.exit(1);
        }
        try {
            ContextManagerFactory.instance().setThreadContext(null);
            MultiFormPrintEngine engine = new MultiFormPrintEngine();
            engine.setEngineType(args.length > 2 ? args[2] : FormPrintFactory.ENGINE_TYPE_ITEXT);
            Properties documentProperties = new Properties();
            documentProperties.setProperty(IFormPrintEngine.DOCUMENT_PROPERTY_TEMPLATE_MODE, "true");
            engine.setDocumentProperties(documentProperties);
            engine.addDocument(args[0], null);
            engine.generate();
            File out = null;
            if (args.length > 1) out = new File(args[1]);
            File out3 = engine.writeForm(out);
            if (out3 == null || !out3.exists()) {
                System.out.println("writePdf(File) did not return a file");
                System.exit(2);
            }
            System.out.println("Generated File: " + out3.getAbsolutePath());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        } finally {
            ContextManagerFactory.instance().unsetThreadContext();
        }
    }
}
