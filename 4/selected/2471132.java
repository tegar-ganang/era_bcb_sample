package ingenias.generator.interpreter;

import ingenias.exception.FileTagEmpty;
import ingenias.exception.TextTagEmpty;
import ingenias.generator.datatemplate.Repeat;
import ingenias.generator.datatemplate.Sequences;
import ingenias.generator.datatemplate.Var;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Codegen {

    private static String readURLTemplate(String templateURL) throws MalformedURLException {
        URLClassLoader urlLoader = new URLClassLoader(new URL[] { new URL(templateURL) });
        InputStream is = urlLoader.getResourceAsStream("templates/servlet.xml");
        return readFile(is);
    }

    /**
   *  Description of the Method
   *
   *@param  constraints  Description of Parameter
   *@param  trans        Description of Parameter
   *@param  files        Description of Parameter
   *@param  target       Description of Parameter
   *@param  template     Description of Parameter
   */
    public static String readFile(InputStream fis) {
        String result = "";
        try {
            int read = 0;
            while (read >= 0) {
                read = fis.read();
                if (read >= 0) {
                    result = result + (char) read;
                }
            }
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static TemplateTree applyArroba(String sequences, InputStream template) throws ingenias.exception.NotWellFormed, Exception {
        String readFile = readFile(template);
        TemplateTree tags = new TemplateTree(new Tag("root", 0, 0));
        readFile = ingenias.generator.util.Conversor.convertArrobaFormat(readFile);
        File output = File.createTempFile("ingenias", "_tmp");
        FileOutputStream fos = new FileOutputStream(output);
        fos.write(readFile.getBytes());
        Vector templateData = obtainTemplateData(sequences);
        File tempFile = File.createTempFile("idk", "cgen");
        PrintWriter pw = new PrintWriter(new FileOutputStream(tempFile));
        try {
            TemplateHandler.process(output.getAbsolutePath(), templateData, pw, tags);
        } catch (org.xml.sax.SAXParseException spe) {
            ingenias.editor.Log.getInstance().logERROR("Parser error at " + ((SAXParseException) spe).getLineNumber() + ":" + ((SAXParseException) spe).getColumnNumber() + " " + spe.getMessage());
            throw new ingenias.exception.NotWellFormed();
        }
        pw.close();
        ingenias.editor.Log.getInstance().logSYS("Processing " + template);
        Codegen.decompose(tempFile.getPath());
        fos.close();
        tempFile.delete();
        return tags;
    }

    public static void apply(String sequences, String template) throws Exception {
        Vector templateData = obtainTemplateData(sequences);
        File tempFile = File.createTempFile("idk", "cgen");
        PrintWriter pw = new PrintWriter(new FileOutputStream(tempFile));
        TemplateHandler.process(template, templateData, pw, new TemplateTree((File) null));
        pw.close();
        Codegen.decompose(tempFile.getPath());
        tempFile.delete();
    }

    public static void decompose(String target) throws FileTagEmpty, TextTagEmpty, java.io.IOException, SAXException {
        new SplitHandler(target);
    }

    /**
   *  The main program for the Codegen class
   *
   *@param  args           The command line arguments
   *@exception  Exception  Description of Exception
   */
    public static void main(String[] args) throws Exception {
        ingenias.editor.Log.initInstance(new PrintWriter(System.err));
        Sequences seq = new Sequences();
        Repeat main = new Repeat("A");
        Repeat sec = new Repeat("B");
        sec.add(new Var("C", "adios"));
        main.add(sec);
        sec = new Repeat("B");
        sec.add(new Var("C", "hola"));
        main.add(sec);
        main.add(new Var("filename", "primero"));
        seq.addRepeat(main);
        main = new Repeat("A");
        sec = new Repeat("B");
        sec.add(new Var("C", "otro"));
        main.add(sec);
        main.add(new Var("filename", "segundo"));
        seq.addRepeat(main);
        TemplateTree tags = null;
        tags = Codegen.applyArroba(seq.toString(), new FileInputStream("ejemplos/plantilla.xml"));
    }

    /**
   *  It converts a text contained in "target" into a XML data structure.
   *
   *@param  target         Description of Parameter
   *@return                Description of the Returned Value
   *@exception  Exception  Description of Exception
   */
    private static Vector obtainTemplateData(String target) throws Exception {
        DOMParser parser = new DOMParser();
        Vector result = new Vector();
        try {
            parser.parse(new org.xml.sax.InputSource(new java.io.StringBufferInputStream(target)));
            Document document = parser.getDocument();
            traverse(document.getFirstChild(), result, null);
        } catch (SAXException e) {
            e.printStackTrace();
            System.err.println("Original text follows. Line numbers appear to the left:");
            System.err.println("-------------------------------------------------------");
            String[] lines = target.split("\n");
            int counter = 1;
            for (String line : lines) {
                System.err.println(counter + ":" + line);
                counter++;
            }
        } catch (java.io.UTFDataFormatException formatEx) {
            ingenias.editor.Log.getInstance().logERROR("The following text contains non UTF-8 characters");
            ingenias.editor.Log.getInstance().logERROR(target);
            throw formatEx;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String encodeutf8Text(String text) {
        try {
            java.io.ByteArrayOutputStream ba = new java.io.ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(ba, "UTF-8");
            osw.write(text);
            osw.close();
            String s = new String(ba.toByteArray(), "UTF-8");
            s = text;
            s = ingenias.generator.util.Conversor.replaceInvalidChar(s);
            StringBuffer sb = new StringBuffer(s);
            return s;
        } catch (Exception uee) {
            uee.printStackTrace();
        }
        return "";
    }

    /**
   *  It transforms the XML representation of the instatiation data to Java in terms of TemplateDAtaRepeat and
   *  TemplateDataVar elements. 
   *
   *@param  node           The node of the DOM tree which is being converted
   *@param  td             The vector of elements transformed so far and belonging to the same root.
 * @param tdr The root of the parent node
 * @throws Exception
 */
    private static void traverse(Node node, Vector td, TemplateDataRepeat tdr) throws Exception {
        int type = node.getNodeType();
        if (type == Node.ELEMENT_NODE) {
            if (node.getNodeName().equalsIgnoreCase("repeat")) {
                String id = node.getAttributes().getNamedItem("id").getNodeValue();
                Vector body = new Vector();
                NodeList children = node.getChildNodes();
                TemplateDataRepeat ctdr = new TemplateDataRepeat(id, body, tdr);
                if (children != null) {
                    for (int i = 0; i < children.getLength(); i++) {
                        Vector tempBody = new Vector();
                        traverse(children.item(i), tempBody, ctdr);
                        Enumeration enumeration = tempBody.elements();
                        while (enumeration.hasMoreElements()) {
                            Object o = enumeration.nextElement();
                            body.add(o);
                        }
                    }
                }
                td.add(ctdr);
            } else if (node.getNodeName().equalsIgnoreCase("v")) {
                String id = node.getAttributes().getNamedItem("id").getNodeValue();
                if (node.getChildNodes().getLength() > 1) {
                    throw new Exception(" At " + id + ":" + node + ". There must be only plain text within <v> tags");
                }
                String value = "";
                if (node.getChildNodes().getLength() != 0) {
                    if (node.getChildNodes().item(0).getNodeType() != Node.TEXT_NODE) {
                        throw new Exception(" At " + node + ". There must be only text withing <v> tags");
                    }
                    value = encodeutf8Text(node.getChildNodes().item(0).getNodeValue());
                }
                TemplateDataVar tdv = new TemplateDataVar(id, value);
                if (node.getAttributes().getNamedItem("entityID") != null && !node.getAttributes().getNamedItem("entityID").getNodeValue().equals("")) {
                    tdv.entityID = node.getAttributes().getNamedItem("entityID").getNodeValue();
                }
                if (node.getAttributes().getNamedItem("attID") != null && !node.getAttributes().getNamedItem("attID").equals("")) tdv.attID = node.getAttributes().getNamedItem("attID").getNodeValue();
                td.add(tdv);
            } else if (node.getNodeName().equalsIgnoreCase("sequences")) {
                NodeList children = node.getChildNodes();
                if (children != null) {
                    for (int i = 0; i < children.getLength(); i++) {
                        traverse(children.item(i), td, tdr);
                    }
                }
            }
        }
    }
}
