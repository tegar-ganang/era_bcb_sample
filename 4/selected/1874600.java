package wsdl2doc.documenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import org.w3c.dom.Node;
import wsdl2doc.freemarker.FreeMarkerTransformer;
import wsdl2doc.freemarker.xmlTools.FreeMarkerXmlDocumentContext;
import wsdl2doc.freemarker.xmlTools.FreeMarkerXmlHelper;
import wsdl2doc.freemarker.xmlTools.XPathXmlWrapper;
import wsdl2doc.skinManager.SkinManager;
import wsdl2doc.utils.EnhancedException;
import wsdl2doc.utils.FileUtils;

/**
 * <p>
 * The skins can access following varibles from the context:
 * <ul>
 * <li>xml : [org.w3c.dom.Node] Document Root Node </li>
 * <li>file : [java.io.File] the file reference to the input file </li>
 * <li>css_url : [java.lang.String] Relative URL to the CSS file </li>
 * <li>index_url : [java.lang.String] <i>optional</i> The link back to the
 * index page, if any </li>
 * <li>wsdl_url : [java.lang.String] <i>optional</i> The link to the copied
 * original of the wsdl</li>
 * </ul>
 * </p>
 */
public class SingleFileDocumenter {

    private String inlineSchemaDir = "inlineSchemas";

    private String copyWsdlDir = "../wsdl";

    private boolean fCopyWsdl = false;

    private boolean fSplitSchema = false;

    private FreeMarkerXmlDocumentContext context;

    private String outputDir;

    private File inputFile;

    /**
	 * Inits a new single file documenter. The documentation file will be named
	 * <i>nameOfWsdl</i>_documentation.html. Inline schemas will be written to
	 * a subdirectory of outputDir named 'inlineSchemas'.
	 * 
	 * @param outputDir
	 *            the directory where the file should be written to, without
	 *            trailing // or \ or whatever
	 * @param inputFile
	 *            the wsdlFile used for input
	 * @param wsdlTransformationContext
	 *            <p>
	 *            A freemarker context. Following entries have to be in inside
	 *            the conetxt
	 *            <ul>
	 *            <li>xml : [org.w3c.dom.Node] Document Root Node </li>
	 *            <li>file : [java.io.File] the file reference to the input
	 *            file </li>
	 *            <li>css_url : [java.lang.String] Relative URL to the CSS file
	 *            </li>
	 *            <li>index_url : [java.lang.String] <i>optional</i> The link
	 *            back to the index page, if any </li>
	 * 
	 * </ul>
	 * </p>
	 */
    public SingleFileDocumenter(String outputDir, File inputFile, FreeMarkerXmlDocumentContext wsdlTransformationContext) {
        this.outputDir = outputDir;
        this.context = wsdlTransformationContext;
        this.inputFile = inputFile;
    }

    /**
	 * Starts processing.
	 * 
	 * @throws Exception
	 *             Something went wrong. Hell knows what...
	 */
    public void process() throws Exception {
        if (fSplitSchema) {
            splitSchema();
        }
        if (fCopyWsdl) {
            copyWsdl();
        }
        transform();
    }

    /**
	 * Copies the source wsdl file
	 * 
	 * @throws EnhancedException
	 *             nixi gut
	 * 
	 */
    private void copyWsdl() throws EnhancedException {
        FileUtils.createDirectory(outputDir + "//" + copyWsdlDir);
        FileUtils.copyFile(inputFile, new File(outputDir + "//" + copyWsdlDir + "//" + inputFile.getName()));
        context.put("wsdl_url", copyWsdlDir + "/" + inputFile.getName());
    }

    /**
	 * Extracts the inline schema from a wsdl and stores it to a temp file.
	 * 
	 * @throws FileNotFoundException
	 */
    private void splitSchema() throws Exception {
        File inputFile = (File) context.get("file");
        XPathXmlWrapper wrapper = new XPathXmlWrapper(context.getXmlNode());
        Node schemaNode = (Node) wrapper.xPathNode("//xsd:schema");
        FileUtils.createDirectory(outputDir + "//" + inlineSchemaDir);
        File schemaFile = new File(outputDir + "//" + inlineSchemaDir + "//inline_schema_of_" + inputFile.getName() + ".xsd");
        PrintWriter schemaWriter = new PrintWriter(schemaFile);
        if (schemaNode != null) {
            FreeMarkerXmlHelper.printNode((Node) schemaNode, schemaWriter);
        }
        schemaWriter.close();
    }

    /**
	 * Performs the transformation
	 * 
	 * @throws Exception
	 */
    private void transform() throws Exception {
        File inputFile = (File) context.get("file");
        String outputFilename = getDocumentationFileName(inputFile.getName());
        File outputFile = new File(outputDir + "//" + outputFilename);
        File templateFile = SkinManager.getSingleFileTemplate();
        InputStream templateInputStream = new FileInputStream(templateFile);
        Writer outputFileWriter = new FileWriter(outputFile);
        XPathXmlWrapper wrapper = new XPathXmlWrapper(context.getXmlNode());
        context.put("xml", wrapper);
        FreeMarkerTransformer transformer = FreeMarkerTransformer.borrowTransformer();
        transformer.transform(outputFileWriter, context, templateInputStream);
        transformer.returnTransformer();
    }

    /**
	 * Creates a doc file name out of the wsdl file name.
	 * 
	 * @param wsdlFileName
	 *            the original wsdl file name
	 * @return documentation name
	 */
    public static String getDocumentationFileName(String wsdlFileName) {
        return wsdlFileName.toLowerCase().replaceAll(".wsdl", "_documentation.html");
    }

    /**
	 * If true, copies the original wsdl file to the copyWsdlDir and creates a
	 * link in the documentation page to it.
	 * 
	 * @return the fCopyWsdl
	 */
    public boolean isFCopyWsdl() {
        return fCopyWsdl;
    }

    /**
	 * 
	 * If true, copies the original wsdl file to the copyWsdlDir and creates a
	 * link in the documentation page to it. False by default.
	 * 
	 * @param fCopyWsdl
	 *            the fCopyWsdl to set
	 */
    public void setFCopyWsdl(boolean copyWsdl) {
        this.fCopyWsdl = copyWsdl;
    }

    /**
	 * Relative path to copy the wsdl's if fCopyWsdl is true.
	 * 
	 * @return the copyWsdlDir
	 */
    public String getCopyWsdlDir() {
        return copyWsdlDir;
    }

    /**
	 * Relative path to copy the wsdl's if fCopyWsdl is true. Dont use ../
	 * 
	 * @param copyWsdlDir
	 *            the copyWsdlDir to set
	 */
    public void setCopyWsdlDir(String copyWsdlDir) {
        this.copyWsdlDir = copyWsdlDir;
    }

    /**
	 * Relative path to the inline schema location.
	 * 
	 * @return the inlineSchemaDir
	 */
    public String getInlineSchemaDir() {
        return inlineSchemaDir;
    }

    /**
	 * Relative path to the inline schema location. Dont use ../
	 * 
	 * @param inlineSchemaDir
	 *            the inlineSchemaDir to set
	 */
    public void setInlineSchemaDir(String inlineSchemaDir) {
        this.inlineSchemaDir = inlineSchemaDir;
    }

    /**
	 * Base dir for output
	 * 
	 * @return the outputDir
	 */
    public String getOutputDir() {
        return outputDir;
    }

    /**
	 * True for extracting the inline xml schmema to an external file.
	 * 
	 * @return the fSplitSchema
	 */
    public boolean isFSplitSchema() {
        return fSplitSchema;
    }

    /**
	 * True for extracting the inline xml schmema to an external file. False by
	 * default.
	 * 
	 * @param splitSchema
	 *            the fSplitSchema to set
	 */
    public void setFSplitSchema(boolean splitSchema) {
        fSplitSchema = splitSchema;
    }

    /**
	 * Base dir for output
	 * 
	 * @param outputDir
	 *            the outputDir to set
	 */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
}
