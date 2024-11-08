package org.apache.tools.ant.taskdefs.optional;

import java.util.Vector;
import java.util.StringTokenizer;
import java.io.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;
import org.apache.tools.ant.taskdefs.compilers.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.lang.Class;
import java.lang.reflect.Method;

/**
 * This Task allows a file to be read in, have tokens replaced, and be 
 * written/overwritten/appended to another file. The template file should
 * have tokens set denoted with the '{' and '}' characters. For example:
 * <BR><BR>
 *
 * The token {replace_me} is the token.
 * <BR><BR>
 * The process template can get token replacements from either system
 * properties or a PropDataHash, with the PropDataHash always taking
 * precidents. 
 *
 *<BR><BR>
 <textarea cols="70" rows="6">
  <processtemplate target="${bin_dir}init_local.sh" 
 template="${template_dir}init.sh" 
 propdatahashid="tmplvalues" overwrite="true"/>
 </textarea>
 *
 * @see org.apache.tools.ant.taskdefs.optional.PropDataHash
 * @author Lucas McGregor
 */
public class ProcessTemplate extends org.apache.tools.ant.Task {

    private static boolean VERBOSE = true;

    boolean append = false;

    boolean overwrite = false;

    String targetFile = null;

    String templateFile = null;

    String propDataHashId = null;

    /** Creates a new instance of WhileDo */
    public void init() {
    }

    /**
     * If true, this task will overwrite the target file
     * if it already exists. If the file exists, and this is 
     * set to false, it will throw an Exception.
     **/
    public void setOverwrite(boolean b) {
        this.overwrite = b;
    }

    public boolean getOverwrite() {
        return this.overwrite;
    }

    /**
     * If this is set to true, and the file already exists,
     * it will append to the file instead of overwriting it.
     **/
    public void setAppend(boolean b) {
        this.append = b;
    }

    public boolean getAppend() {
        return this.append;
    }

    /**
     * The location of the file to write to.
     **/
    public void setTarget(String s) {
        this.targetFile = s;
    }

    public String getTarget() {
        return this.targetFile;
    }

    /**
     * The location of the file to read in as
     * a template.
     **/
    public void setTemplate(String s) {
        this.templateFile = s;
    }

    public String getTemplate() {
        return this.templateFile;
    }

    /**
     * The refernce ID of a PropDataHash to read data from.
     * If it is left null, then it will only use system
     * properties.
     **/
    public void setPropDataHashId(String s) {
        propDataHashId = s;
    }

    public void execute() throws BuildException {
        Project proj = getProject();
        if (templateFile == null) throw new BuildException("Template file not set");
        if (targetFile == null) throw new BuildException("Target file not set");
        try {
            File template = new File(templateFile);
            File target = new File(targetFile);
            if (!template.exists()) throw new BuildException("Template file does not exist " + template.toString());
            if (!template.canRead()) throw new BuildException("Cannot read template file: " + template.toString());
            if (((!append) && (!overwrite)) && (!target.exists())) throw new BuildException("Target file already exists and append and overwrite are false " + target.toString());
            if (VERBOSE) {
                System.out.println("ProcessTemplate: tmpl in " + template.toString());
                System.out.println("ProcessTemplate: file out " + target.toString());
            }
            BufferedReader reader = new BufferedReader(new FileReader(template));
            BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, append));
            parse(reader, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            if (VERBOSE) e.printStackTrace();
            throw new BuildException(e);
        }
    }

    private void parse(Reader in, Writer out) throws Exception {
        int charsRead = 0;
        char[] buffer = new char[10240];
        StringBuffer sb = new StringBuffer(10240);
        while (in.ready()) {
            charsRead = in.read(buffer, 0, buffer.length);
            sb.append(buffer, 0, charsRead);
        }
        String str = sb.toString();
        Hashtable shash = null;
        if (propDataHashId != null) {
            shash = getReferencedHash(propDataHashId);
        } else {
            shash = new Hashtable();
        }
        if (VERBOSE) System.out.println("ProcessTemplate: template: " + str);
        Vector v = new Vector();
        int index0 = 0;
        int index1 = 0;
        int index2 = 0;
        String tag = null;
        while ((index1 + 1) < str.length()) {
            index1 = str.indexOf("{", index0);
            index2 = str.indexOf("}", (index1 + 1));
            if (index1 == -1) {
                if (VERBOSE) System.out.println("ProcessTemplate: write: " + str.substring(index0));
                out.write(str.substring(index0).toCharArray());
                break;
            } else if ((index1 + 1) < (index2)) {
                if (index0 < index1) {
                    if (VERBOSE) System.out.println("ProcessTemplate: write: " + str.substring(index0, index1));
                    out.write(str.substring(index0, index1).toCharArray());
                }
                tag = (str.substring((index1 + 1), (index2)));
                try {
                    if (VERBOSE) System.out.println("ProcessTemplate: " + tag + " is from PDH: " + (shash.containsKey(tag)));
                    if (shash.containsKey(tag)) out.write((String) shash.get(tag)); else out.write(getProject().getProperty(tag));
                } catch (Exception e) {
                    System.out.println("ProcessTemplate: {" + tag + "} not set");
                    throw e;
                }
                if (VERBOSE) System.out.println("ProcessTemplate: substitute: {" + tag + "} for '" + getProject().getProperty(tag) + "'");
            } else {
                out.write(str.substring(index0).toCharArray());
            }
            index0 = index2 + 1;
        }
    }

    /**
     * Use reflection to get the hashtable since the system
     * throws class cast exceptions when you try to cast the
     * object as a PropDataHash
     **/
    private Hashtable getReferencedHash(String idStr) throws Exception {
        if (!project.getReferences().containsKey(idStr)) throw new Exception("Project does not contain a reference to " + idStr);
        Hashtable ht = null;
        try {
            Object obj = project.getReferences().get(idStr);
            Class[] args = null;
            Object[] args2 = null;
            Method mth = obj.getClass().getMethod("getHash", args);
            ht = (Hashtable) mth.invoke(obj, args2);
        } catch (Exception e) {
            throw new Exception("Unable invoke getHash on " + idStr);
        }
        return ht;
    }
}
