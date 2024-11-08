package net.sf.xsltbuddy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.xml.transform.Templates;
import net.sf.xsltbuddy.xslt.ChartUtil;
import net.sf.xsltbuddy.xslt.CommonUtil;
import net.sf.xsltbuddy.xslt.HTTPUtil;
import net.sf.xsltbuddy.xslt.Logger;
import net.sf.xsltbuddy.xslt.Monitor;
import net.sf.xsltbuddy.xslt.XMLUtil;
import net.sf.xsltbuddy.xslt.XSLTransformer;
import net.sf.xsltbuddy.xslt.struts.BeanUtil;
import net.sf.xsltbuddy.xslt.struts.HTMLUtil;
import org.w3c.dom.Node;

/**
 * @author Chico Charlesworth
 * @version 1.0
 */
public class XSLTBuddy {

    public static final String BLANK_XML = "<?xml version=\"1.0\"?><Blank/>";

    private String src;

    private String xsl;

    private String out;

    private Hashtable params = new Hashtable();

    private String templateDir;

    private Hashtable vars = new Hashtable();

    private static final String DEFAULT_RESULT = "default_result";

    public Node getResultAsNode() {
        return (Node) this.getTemplateResult();
    }

    /** Create new instance
   *
   * @return xstbuddy instance
   */
    public static XSLTBuddy newInstance() {
        return new XSLTBuddy();
    }

    /** Get logger
   *
   * @return logger instance
   */
    public Logger getLogger() {
        return new Logger(this);
    }

    /** Get utility
   *
   * @return utility instance
   * @throws Exception
   */
    public CommonUtil getCommonUtil() throws Exception {
        return new CommonUtil(this);
    }

    /** Get chart utility
   *
   * @return chart utility instance
   * @throws Exception
   */
    public ChartUtil getChartUtil() throws Exception {
        return new ChartUtil(this);
    }

    /** Get monitor utility
   *
   * @return monitor utility instance
   * @throws Exception
   */
    public Monitor getMonitor() throws Exception {
        return new Monitor(this);
    }

    /** Get HTTP utility
   *
   * @return http utility instance
   * @throws Exception
   */
    public HTTPUtil getHTTPUtil() throws Exception {
        return new HTTPUtil(this);
    }

    /** Get XML utility
   *
   * @return xml utility instance
   * @throws Exception
   */
    public XMLUtil getXMLUtil() throws Exception {
        return new XMLUtil(this);
    }

    /** Get struts-html tag renderer
   *
   * @return struts-html renderer
   * @throws Exception
   */
    public HTMLUtil getHTMLUtil() throws Exception {
        return new HTMLUtil(this);
    }

    /** Get struts-bean
   *
   * @return struts-bean renderer
   * @throws Exception
   */
    public BeanUtil getBeanUtil() throws Exception {
        return new BeanUtil(this);
    }

    public Object getTemplateResult() {
        return this.getVar(DEFAULT_RESULT);
    }

    public void setTemplateResult(Object templateResult) {
        this.setVar(DEFAULT_RESULT, templateResult);
    }

    public Object getVar(String key) {
        return vars.get(key);
    }

    public void setVar(String key, Object templateResult) {
        vars.put(key, templateResult);
    }

    /** XSLTBuddy can also be invoked from the command-line
   *
   * @param args
   */
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        XSLTBuddy buddy = new XSLTBuddy();
        buddy.parseArgs(args);
        XSLTransformer transformer = new XSLTransformer();
        if (buddy.templateDir != null) {
            transformer.setTemplateDir(buddy.templateDir);
        }
        FileReader xslReader = new FileReader(buddy.xsl);
        Templates xslTemplate = transformer.getXSLTemplate(buddy.xsl, xslReader);
        for (Enumeration e = buddy.params.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            transformer.addParam(key, buddy.params.get(key));
        }
        Reader reader = null;
        if (buddy.src == null) {
            reader = new StringReader(XSLTBuddy.BLANK_XML);
        } else {
            reader = new FileReader(buddy.src);
        }
        if (buddy.out == null) {
            String result = transformer.doTransform(reader, xslTemplate, buddy.xsl);
            buddy.getLogger().info("\n\nXSLT Result:\n\n" + result + "\n");
        } else {
            File file = new File(buddy.out);
            File dir = file.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            FileWriter writer = new FileWriter(buddy.out);
            transformer.doTransform(reader, xslTemplate, buddy.xsl, writer);
            writer.flush();
            writer.close();
        }
        buddy.getLogger().info("Transform done successfully in " + (System.currentTimeMillis() - start) + " milliseconds");
    }

    /** Parses user arguments and starts the JXML generation process
   **/
    private void parseArgs(String[] args) {
        boolean ok = true;
        for (int i = 0; i < args.length; i++) {
            String p = args[i];
            int next = i + 1;
            if (p.equalsIgnoreCase("-src")) {
                this.src = this.parseNextArgument(p, args, next);
            } else if (p.equalsIgnoreCase("-xsl")) {
                this.xsl = this.parseNextArgument(p, args, next);
            } else if (p.equalsIgnoreCase("-out")) {
                this.out = this.parseNextArgument(p, args, next);
            } else if (p.equalsIgnoreCase("-param")) {
                String[] keyValuePair = this.parseNextArguments(p, args, next, 2);
                this.params.put(keyValuePair[0], keyValuePair[1]);
            } else if (p.equalsIgnoreCase("-dir")) {
                this.templateDir = this.parseNextArgument(p, args, next);
            } else if (p.equalsIgnoreCase("-?")) {
                usage();
            } else if (p.startsWith("-")) {
                usage("Unrecognised option " + p);
            }
        }
        if ((this.xsl == null) || ((this.xsl.trim().length() < 1))) {
            usage("-xsl option must be defined");
        }
    }

    /**
   *
   * @param args
   * @param next
   * @return next argument value
   */
    private String parseNextArgument(String option, String[] args, int next) {
        if (next < args.length) {
            if (args[next].startsWith("-")) {
                usage("Failed to parse option " + option + " - Invalid argument value: " + args[next]);
            }
            return args[next];
        }
        usage("Failed to parse option " + option);
        return null;
    }

    /**
   *
   * @param args
   * @param next
   * @return argument list
   */
    private String[] parseNextArguments(String option, String[] args, int next, int noArgs) {
        if ((next + (noArgs - 1)) < args.length) {
            String[] values = new String[noArgs];
            for (int i = 0; i < noArgs; i++) {
                if (args[next + i].startsWith("-")) {
                    usage("Failed to parse option " + option + " - Invalid argument value: " + args[next + i]);
                }
                values[i] = args[next + i];
            }
            return values;
        }
        usage("Failed to parse option " + option);
        return null;
    }

    /** Display command-line usage
   **/
    private void usage() {
        this.usage(null);
    }

    /** Display command-line usage
   **/
    private void usage(String errorMsg) {
        if (errorMsg != null) {
            System.out.println("\nError: " + errorMsg);
        }
        String javaCmd = "java sf.net.xsltbuddy.XSLTBuddy";
        String osname = System.getProperty("os.name");
        if ((osname != null) && (osname.trim().length() > 0)) {
            if (osname.toUpperCase().startsWith("WINDOWS")) {
                javaCmd = "xsltbuddy.bat";
            } else {
                javaCmd = "xsltbuddy.sh";
            }
        }
        System.out.println("\nUsage: " + javaCmd + " [-options]");
        System.out.println("\nOptions:");
        System.out.println("-xsl   <XSL File>               (Required, e.g.: test.xsl)");
        System.out.println("-src   <XML Source File>        (Optional, e.g.: test.xml)");
        System.out.println("-out   <Output File>            (Optional, e.g.: test.html)");
        System.out.println("-dir   <XSL Template Directory> (Optional, Default: ../templates)");
        System.out.println("-param <XSL Param Name> <Value> (Optional, Multiple, e.g.: myparam test)");
        System.out.println("-?     Display This Usage");
        System.out.println("\nExamples:");
        System.out.println(javaCmd + " -xsl test.xsl");
        System.out.println(javaCmd + " -xsl test.xsl -param Firstname John -param Surname Doe ");
        System.out.println(javaCmd + " -src test.xml -xsl test.xsl -out test.html");
        System.out.println(javaCmd + " -src test.xml -xsl test.xsl -dir ../templates");
        System.exit(0);
    }

    /** Invoked when called from XSL as <xslbuddy:log>
   *
   * @param context
   * @param extElem
   */
    public void log(org.apache.xalan.extensions.XSLProcessorContext context, org.apache.xalan.templates.ElemExtensionCall extElem) {
        String msg = extElem.getAttribute("msg");
        String level = extElem.getAttribute("level");
        this.getLogger().log(msg, level);
    }
}
