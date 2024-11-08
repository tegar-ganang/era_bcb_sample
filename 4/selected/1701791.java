package org.openware.job.generator.output.java;

import org.openware.job.generator.metadata.MetaData;
import org.openware.job.generator.metadata.MetaElement;
import org.openware.job.generator.metadata.MDMClass;
import org.openware.job.generator.metadata.TypeMapping;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Vincent Sheffer
 * @version $Revision: 1.10 $ $Date: 2003/02/27 01:12:24 $
 */
public class ApiOutput extends JavaOutput {

    private static final String HANDWRITTEN_DELIMITER_LINE = "/*----------- YOUR METHODS BELOW -- DO NOT DELETE THIS LINE -----------*/";

    private String userLines = null;

    private LinkedList userImports = new LinkedList();

    private String apiName = null;

    private String datasourceName = null;

    private String persistPackageRoot = null;

    public ApiOutput(JavaDestination dest, String apiName, String datasourceName, String persistPackageRoot) {
        super(dest);
        this.apiName = apiName;
        this.datasourceName = datasourceName;
        this.persistPackageRoot = persistPackageRoot;
    }

    private void addImport(String className, LinkedList usedImports, PrintWriter pwriter) {
        if (!usedImports.contains(className)) {
            pwriter.println("import " + className + ";");
            usedImports.add(className);
        }
    }

    private void writeImports(MetaData md, PrintWriter pwriter) {
        LinkedList usedImports = null;
        Iterator iter = null;
        boolean hasLists = false;
        usedImports = new LinkedList();
        addImport(persistPackageRoot + ".ClassInfo", usedImports, pwriter);
        addImport("org.openware.job.data.PersistentManager", usedImports, pwriter);
        addImport("org.openware.job.data.PersistException", usedImports, pwriter);
        addImport("org.openware.job.data.BusinessRules", usedImports, pwriter);
        addImport("org.openware.job.cache.CacheManager", usedImports, pwriter);
        addImport("org.openware.job.cache.CacheInvalidationInfo", usedImports, pwriter);
        pwriter.println();
        addImport("org.apache.log4j.Category", usedImports, pwriter);
        pwriter.println();
        addImport("java.util.Hashtable", usedImports, pwriter);
        addImport("javax.servlet.http.HttpServletRequest", usedImports, pwriter);
        addImport("javax.servlet.http.HttpSession", usedImports, pwriter);
        iter = userImports.iterator();
        if (iter.hasNext()) {
            pwriter.println();
        }
        while (iter.hasNext()) {
            addImport((String) iter.next(), usedImports, pwriter);
        }
        pwriter.println();
    }

    private void outputUserLines(PrintWriter pwriter) throws IOException {
        pwriter.print(userLines);
    }

    private void extractUserLines() throws IOException {
        File file = null;
        PrintWriter pwriter = null;
        StringWriter stringWriter = null;
        userImports.clear();
        stringWriter = new StringWriter();
        pwriter = new PrintWriter(stringWriter);
        file = dest.getFile(apiName);
        if (file.exists()) {
            BufferedReader in = null;
            String line = null;
            boolean foundDelimiter = false;
            in = new BufferedReader(new FileReader(file));
            while ((line = in.readLine()) != null) {
                if (line.trim().startsWith("import ")) {
                    userImports.add(line.trim().substring(7, line.trim().length() - 1));
                }
                if (line.trim().equals(HANDWRITTEN_DELIMITER_LINE)) {
                    foundDelimiter = true;
                }
                if (foundDelimiter) {
                    pwriter.println(line);
                }
            }
            if (!foundDelimiter) {
                throw new IOException("The user code delimiter wasn't found.");
            }
        } else {
            pwriter.println("    " + HANDWRITTEN_DELIMITER_LINE);
            pwriter.println("    /**");
            pwriter.println("     * If you already have an existing <code>PersistentManager</code> use");
            pwriter.println("     * this create method.");
            pwriter.println("     *");
            pwriter.println("     * @param  pmanager  The <code>PersistentManager</code> to use with the");
            pwriter.println("     *                   API.");
            pwriter.println("     */");
            pwriter.println("    public static " + apiName + " create(PersistentManager pmanager) throws PersistException {");
            pwriter.println("        if (pmanager.getName() == null) {");
            pwriter.println("            pmanager.setName(\"" + apiName + "\");");
            pwriter.println("        }");
            pwriter.println("        return new " + apiName + "(pmanager);");
            pwriter.println("    }");
            pwriter.println();
            pwriter.println("    /**");
            pwriter.println("     * Create a factory that communicates with JOB via the HTTP ");
            pwriter.println("     * protocol.");
            pwriter.println("     */");
            pwriter.println("    public static " + apiName + " createInHttpMode(String url) ");
            pwriter.println("        throws PersistException {");
            pwriter.println("        PersistentManager pmanager = null;");
            pwriter.println();
            pwriter.println("        pmanager = new PersistentManager(cinfo, url, ");
            pwriter.println("                                         PersistentManager.SERVLET);");
            pwriter.println("        pmanager.setName(\"" + apiName + "\");");
            pwriter.println("        return new " + apiName + "(pmanager);");
            pwriter.println("    }");
            pwriter.println();
            pwriter.println("    /**");
            pwriter.println("     * Create a factory that communicates with JOB via a \"normal\" EJB");
            pwriter.println("     * remote interface (usually this is some variant of RMI).");
            pwriter.println("     */");
            pwriter.println("    public static " + apiName + " createInEjbMode()");
            pwriter.println("        throws PersistException {");
            pwriter.println("        PersistentManager pmanager = null;");
            pwriter.println("        initClassInfo();");
            pwriter.println("        pmanager = new PersistentManager(cinfo, DATASOURCE_JNDI_NAME, ");
            pwriter.println("                                         PersistentManager.EJB);");
            pwriter.println("        pmanager.setName(\"" + apiName + "\");");
            pwriter.println("        return new " + apiName + "(pmanager);");
            pwriter.println("    }");
            pwriter.println();
            pwriter.println("    /**");
            pwriter.println("     * Create a factory that communicates with JOB via EJB remote ");
            pwriter.println("     * interfaces.");
            pwriter.println("     *");
            pwriter.println("     * @param  jndiEnv  The JNDI environment properties.");
            pwriter.println("     */");
            pwriter.println("    public static " + apiName + " createInEjbMode(Hashtable jndiEnv)");
            pwriter.println("        throws PersistException {");
            pwriter.println("        PersistentManager pmanager = null;");
            pwriter.println("        initClassInfo();");
            pwriter.println("        pmanager = new PersistentManager(jndiEnv, DATASOURCE_JNDI_NAME, cinfo);");
            pwriter.println("        pmanager.setName(\"" + apiName + "\");");
            pwriter.println("        return new " + apiName + "(pmanager);");
            pwriter.println("    }");
            pwriter.println();
            pwriter.println("    /**");
            pwriter.println("     * Create a factory that communicates with JOB via in memory ");
            pwriter.println("     * calls.");
            pwriter.println("     *");
            pwriter.println("     */");
            pwriter.println("    public static " + apiName + " createInLocalMode()");
            pwriter.println("        throws PersistException {");
            pwriter.println("        PersistentManager pmanager = null;");
            pwriter.println("        initClassInfo();");
            pwriter.println("        pmanager = new PersistentManager(cinfo, DATASOURCE_JNDI_NAME, PersistentManager.LOCAL);");
            pwriter.println("        return new " + apiName + "(pmanager);");
            pwriter.println("    }");
            pwriter.println();
            pwriter.println("    public static " + apiName + " getApi(HttpServletRequest request) throws Exception {");
            pwriter.println("        String __API_NAME__ = \"__" + apiName + "__\";");
            pwriter.println("        " + apiName + " api = (" + apiName + ")request.getSession().getAttribute(__API_NAME__);");
            pwriter.println("        if ( api == null ) {");
            pwriter.println("            api = createInLocalMode();");
            pwriter.println("            request.getSession().setAttribute(__API_NAME__, api);");
            pwriter.println("        }");
            pwriter.println();
            pwriter.println("        return api;");
            pwriter.println("    }");
            pwriter.println();
            pwriter.println("    public PersistentManager getPersistentManager() throws PersistException {");
            pwriter.println("        return pmanager;");
            pwriter.println("    }");
            pwriter.println();
            pwriter.println("}");
        }
        userLines = stringWriter.toString();
    }

    private void outputManagerDecls(PrintWriter pwriter, MetaData md) throws IOException {
        Iterator iter = null;
        iter = md.getClasses();
        while (iter.hasNext()) {
            MDMClass c = null;
            c = (MDMClass) iter.next();
            pwriter.println("    private " + c.getName() + "Manager " + lowerCaseFirstChar(c.getName() + "Manager") + " = null;");
        }
    }

    public void output(MetaData md) throws IOException {
        PrintWriter pwriter = null;
        Iterator iter = null;
        TypeMapping tm = null;
        extractUserLines();
        tm = md.getTypeMapping();
        pwriter = new PrintWriter(new FileWriter(dest.getFile(apiName)));
        pwriter.println("// Generated by JOBManager -- DO NOT EDIT --");
        pwriter.println("package " + this.dest.getPackageName() + ";");
        pwriter.println();
        writeImports(md, pwriter);
        pwriter.println("/**");
        pwriter.println(" * @author JOB");
        pwriter.println(" */");
        pwriter.println("public class " + apiName);
        pwriter.println("{");
        pwriter.println("    private static final Category log = Category.getInstance(" + apiName + ".class.getName());");
        pwriter.println("    private static final String DATASOURCE_JNDI_NAME = \"" + datasourceName + "\";");
        pwriter.println("    private static ClassInfo cinfo = null;");
        pwriter.println("    private BusinessRules businessRules = null;");
        pwriter.println("    private PersistentManager pmanager = null;");
        outputManagerDecls(pwriter, md);
        pwriter.println();
        pwriter.println("    private " + apiName + "(PersistentManager pmanager) {");
        pwriter.println("        this.pmanager = pmanager;");
        pwriter.println("        businessRules = new BusinessRules();");
        pwriter.println("        initManagers();");
        pwriter.println("    }");
        pwriter.println();
        pwriter.println("    /**");
        pwriter.println("     * Set the username of the person accessing the API for auditing");
        pwriter.println("     * purposes.");
        pwriter.println("     *");
        pwriter.println("     * @param  username  The username (or some other unique ID).  This");
        pwriter.println("     *                   can be any <code>String</code> type.");
        pwriter.println("     */");
        pwriter.println("    public void setUsername(String username) {");
        pwriter.println("        pmanager.setUsername(username);");
        pwriter.println("    }");
        pwriter.println();
        pwriter.println("    public String getUsername() {");
        pwriter.println("        return pmanager.getUsername();");
        pwriter.println("    }");
        pwriter.println();
        pwriter.println("    private static void initClassInfo() throws PersistException {");
        pwriter.println("        if (cinfo == null) {");
        pwriter.println("            cinfo = new ClassInfo();");
        iter = md.getClasses();
        while (iter.hasNext()) {
            MDMClass c = null;
            c = (MDMClass) iter.next();
            if (!c.isView()) {
                pwriter.println("            " + c.getName() + "Manager.addValidators(cinfo);");
            }
        }
        pwriter.println("        }");
        pwriter.println("    }");
        pwriter.println();
        pwriter.println("    private void initManagers() {");
        iter = md.getClasses();
        while (iter.hasNext()) {
            MDMClass c = null;
            c = (MDMClass) iter.next();
            pwriter.println("        " + lowerCaseFirstChar(c.getName()) + "Manager = new " + c.getName() + "Manager(pmanager, this);");
            if (!c.isView()) {
                pwriter.println("        " + lowerCaseFirstChar(c.getName()) + "Manager.addBusinessRules(businessRules);");
            }
        }
        pwriter.println("    }");
        pwriter.println();
        iter = md.getClasses();
        while (iter.hasNext()) {
            MDMClass c = null;
            c = (MDMClass) iter.next();
            pwriter.println("    public " + c.getName() + "Manager get" + c.getName() + "Manager() {");
            pwriter.println("        return " + lowerCaseFirstChar(c.getName()) + "Manager;");
            pwriter.println("    }");
            pwriter.println();
        }
        pwriter.println("    /**");
        pwriter.println("     * Commit any changes made via the API to the database.");
        pwriter.println("     */");
        pwriter.println("    public void commit() throws PersistException {");
        pwriter.println("        CacheInvalidationInfo info = null;");
        pwriter.println();
        pwriter.println("        pmanager.validate();");
        pwriter.println("        info = pmanager.save(businessRules);");
        pwriter.println("        CacheManager.updateCaches(pmanager, info);");
        pwriter.println("    }");
        pwriter.println();
        pwriter.println("    /**");
        pwriter.println("     * Throw away any changes since the last commit.");
        pwriter.println("     */");
        pwriter.println("    public void rollback() throws PersistException {");
        pwriter.println("        pmanager.revert();");
        pwriter.println("    }");
        pwriter.println();
        pwriter.println("    /**");
        pwriter.println("     * JOB keeps database data cached in memory.  Calling this method will");
        pwriter.println("     * wipe that cached data out, resulting in all data coming from the ");
        pwriter.println("     * database.");
        pwriter.println("     */");
        pwriter.println("    public void purgeCaches() throws PersistException {");
        pwriter.println("        pmanager.purgeCaches();");
        pwriter.println("    }");
        pwriter.println();
        outputUserLines(pwriter);
        pwriter.flush();
        pwriter.close();
    }
}
