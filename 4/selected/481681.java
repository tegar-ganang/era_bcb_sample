package pogo.gene;

import fr.esrf.TangoDs.TangoConst;
import java.io.*;

/**
 *	This class generates source file.
 *	it could generate .pogo, .cpp or .java files.
 */
public class PogoGene implements PogoDefs, TangoConst {

    /**
 *	Pogo Class to be used for generation.
 */
    PogoClass pogo;

    /**
 *	trace used during code generation.
 */
    protected String strTrace;

    public PogoGene(PogoClass pogo) {
        this.pogo = pogo;
    }

    public String getTrace() {
        return strTrace;
    }

    protected boolean mustGenerate(String filename) throws FileNotFoundException, IOException {
        return !already_exists(filename);
    }

    protected boolean already_exists(String filename) throws FileNotFoundException, IOException {
        return (new File(filename).exists());
    }

    protected String setMethodInComments(PogoString pgs, int startline) {
        int end_bl = pgs.inMethod(startline);
        end_bl = pgs.outMethod(end_bl);
        PogoString pgs1 = new PogoString(pgs.str.substring(startline, end_bl));
        StringBuffer sb = new StringBuffer("//");
        for (int i = 0; i < pgs1.str.length(); i++) {
            sb.append(pgs1.str.charAt(i));
            if (pgs1.str.charAt(i) == '\n') sb.append("//");
        }
        return sb.toString();
    }

    public static void copyFile(String f_in, String f_out) throws FileNotFoundException, IOException {
        copyFile(f_in, f_out, true);
    }

    public static void copyFile(String f_in, String f_out, boolean remove) throws FileNotFoundException, IOException {
        if (remove) {
            PogoString readcode = new PogoString(PogoUtil.readFile(f_in));
            readcode = PogoUtil.removeLogMessages(readcode);
            PogoUtil.writeFile(f_out, readcode.str);
        } else {
            FileInputStream fid = new FileInputStream(f_in);
            FileOutputStream fidout = new FileOutputStream(f_out);
            int nb = fid.available();
            byte[] inStr = new byte[nb];
            if (fid.read(inStr) > 0) fidout.write(inStr);
            fid.close();
            fidout.close();
        }
    }

    public static void copyDirectory(String src, String target) throws FileNotFoundException, IOException {
        File src_dir = new File(src);
        File target_dir = new File(target);
        if (!target_dir.exists()) target_dir.mkdir();
        System.out.println("Copiing " + src + " to " + target);
        String[] src_files = src_dir.list();
        if (src_files == null) {
            System.out.println("	ERROR");
            return;
        }
        for (int i = 0; i < src_files.length; i++) {
            String src_file = src + "/" + src_files[i];
            if (!new File(src_file).isDirectory()) {
                String target_file = target_dir + "/" + src_files[i];
                copyFile(src_file, target_file, false);
            }
        }
    }

    protected void replacePatternFile(String f_in, String f_out, String to_find, String target) throws FileNotFoundException, SecurityException, IOException {
        PogoString pgs = new PogoString(PogoUtil.readFile(f_in));
        pgs = PogoUtil.removeLogMessages(pgs);
        for (int start = 0; (start = pgs.str.indexOf(to_find, start)) >= 0; start += target.length()) pgs.replace(start, to_find, target);
        PogoUtil.writeFile(f_out, pgs.str);
    }

    protected String changeExeMethodArgs(Cmd cmd, String str, int idx, int lang) {
        StringBuffer sb = new StringBuffer(str);
        boolean in_method = false;
        int cnt = 0;
        char c;
        String description;
        sb.insert(idx, "//");
        while (!in_method || cnt > 0) {
            if ((c = sb.charAt(idx++)) == '\n') sb.insert(idx, "//");
            if (c == '{') {
                cnt++;
                in_method = true;
            } else if (c == '}') cnt--;
        }
        switch(lang) {
            case PogoDefs.javaLang:
                description = cmd.buildJavaExecCmdMethodComments();
                sb.insert(idx, description);
                idx += description.length();
                sb.insert(idx, cmd.buildJavaExecCmdMethod());
                break;
            case PogoDefs.cppLang:
                break;
        }
        return sb.toString();
    }

    protected String checkForExecMethodModif(PogoString readcode, int lang) throws PogoException {
        if (lang == cppLang) for (int i = 0; i < pogo.commands.size(); i++) {
            Cmd cmd = pogo.commands.cmdAt(i);
            if (!cmd.virtual_method) cmd.updateCppExecCmdMethodComments(readcode, pogo.class_name);
        }
        for (int i = 0; i < pogo.commands.size(); i++) {
            Cmd cmd = pogo.commands.cmdAt(i);
            if (!cmd.virtual_method) {
                int startmethod;
                int startline;
                int endline;
                String pattern;
                if (lang == cppLang) pattern = pogo.class_name + "::" + cmd.exec_method + "("; else pattern = cmd.exec_method + "(";
                endline = 0;
                boolean done = false;
                while (!done && (startmethod = readcode.str.indexOf(pattern, endline)) >= 0) {
                    startline = readcode.previousCr(startmethod);
                    endline = readcode.nextCr(startline);
                    String line = readcode.str.substring(startline, endline);
                    int sl = line.indexOf("(");
                    int el = line.indexOf(")", sl);
                    if (el < 0) el = sl + 1;
                    line = line.substring(0, el);
                    if (cmd.isExecMethod(readcode.str.substring(startmethod, endline))) {
                        if (line.indexOf("//") < 0 && line.indexOf("\"") < 0 && line.indexOf("<<") < 0 && line.indexOf("println") < 0) {
                            if (cmd.execMethodArgsChanged(line, lang)) {
                                String s = changeExeMethodArgs(cmd, readcode.str, startline, lang);
                                readcode = new PogoString(s);
                            }
                            done = true;
                        }
                    }
                }
            }
        }
        return readcode.str;
    }

    protected String setProjectTitle(String header) {
        int start, end;
        if ((start = header.indexOf(projectTitleRes)) < 0) return header;
        start += projectTitleRes.length();
        end = header.indexOf("\n", start);
        return header.substring(0, start) + "\t" + pogo.title + header.substring(end);
    }

    protected String readAttributeTemplate(String f_in, int part) throws FileNotFoundException, SecurityException, IOException, PogoException {
        PogoString rw = new PogoString(PogoUtil.readFile(f_in));
        rw = PogoUtil.removeLogMessages(rw);
        while (rw.str.indexOf(templateClass) >= 0) rw.replace(templateClass, pogo.class_name);
        int end;
        if ((end = rw.str.indexOf("WRITE ATTRIBUTE")) < 0) throw new PogoException("\'WRITE ATTRIBUTE\' method not found");
        end = rw.previousCr(end);
        String str = null;
        switch(part) {
            case readFile:
                str = rw.str.substring(0, end);
                break;
            case writeFile:
                int start = rw.nextCr(end) + 1;
                str = rw.str.substring(start);
                break;
        }
        return str;
    }

    protected String buildAttributesMethods(PogoString pgs, int lang) throws PogoException, IOException, FileNotFoundException {
        String tab = (lang == javaLang) ? "\t\t" : "\t";
        int start, end, ptr;
        if (lang == javaLang) {
            if ((start = pgs.str.indexOf(startAttrStr)) < 0) throw new PogoException("Input File Syntax error !\n" + startAttrStr + "\nNot Found !");
            if ((end = pgs.str.indexOf(endAttrStr)) < 0) throw new PogoException("Input File Syntax error !\n" + endAttrStr + "\n Not found !");
            String prev_declar = pgs.str.substring(start, end);
            StringBuffer sb = new StringBuffer(startAttrStr);
            sb.append("\n\n");
            for (int i = 0; i < pogo.attributes.size(); i++) {
                Attrib attr = pogo.attributes.attributeAt(i);
                sb.append(attr.javaMemberData());
            }
            sb.append("\n");
            pgs.replace(prev_declar, sb.toString());
        }
        String rwAttr;
        if (lang == javaLang) rwAttr = pogo.templates_dir + "/java/ReadWriteAttr.java"; else rwAttr = pogo.templates_dir + "/cpp/ReadHardwareAttr.cpp";
        String read = readAttributeTemplate(rwAttr, readFile);
        String write = readAttributeTemplate(rwAttr, writeFile);
        String pattern;
        if (lang == javaLang) pattern = "public void read_attr_hardware"; else pattern = "::read_attr_hardware";
        if (pgs.str.indexOf(pattern) < 0) {
            if (lang == javaLang) pattern = "always_executed_hook()"; else pattern = "::always_executed_hook";
            if ((ptr = pgs.str.indexOf(pattern)) < 0) throw new PogoException("\'" + pattern + "()\' method not found");
            ptr = pgs.inMethod(ptr) + 1;
            ptr = pgs.outMethod(ptr) + 1;
            pgs.insert(ptr, read);
        }
        boolean writable = false;
        for (int i = 0; i < pogo.attributes.size(); i++) {
            Attrib attr = pogo.attributes.attributeAt(i);
            if (attr.getWritable()) writable = true;
        }
        if (lang == javaLang) pattern = "public void write_attr_hardware"; else pattern = "::write_attr_hardware";
        if (writable && pgs.str.indexOf(pattern) < 0) {
            if (lang == javaLang) pattern = "public void always_executed_hook()"; else pattern = "::always_executed_hook()";
            ptr = pgs.str.indexOf(pattern);
            ptr = pgs.inMethod(ptr) + 1;
            ptr = pgs.outMethod(ptr) + 1;
            pgs.insert(ptr, write);
        }
        if (lang == javaLang) pattern = "public void read_attr("; else pattern = "::read_attr(";
        if ((start = pgs.str.indexOf(pattern)) < 0) throw new PogoException("\'" + pattern + ")\' method not found");
        start = pgs.inMethod(start) + 1;
        end = pgs.outMethod(start);
        end = pgs.str.lastIndexOf('}', end);
        PogoString method = new PogoString(pgs.str.substring(start, end));
        String oldmethod = method.str;
        if ((ptr = method.str.indexOf("Switch on attribute name")) < 0) {
            System.out.println(method);
            throw new PogoException("\'Switch on attribute name\' not found");
        }
        ptr = method.nextCr(ptr) + 1;
        ptr = method.nextCr(ptr) + 1;
        for (int i = 0; i < pogo.attributes.size(); i++) {
            Attrib attr = pogo.attributes.attributeAt(i);
            if (attr.rwType != ATTR_WRITE) {
                String target = "\"" + attr.name + "\"";
                if (method.str.indexOf(target, ptr) < 0) {
                    String str = "";
                    if (i != 0) str += tab + "else\n";
                    str += tab + "if (attr_name == " + target + ")\n" + tab + "{\n" + tab + "\t//	Add your own code here\n" + tab + "}\n";
                    int here;
                    if (i != 0) here = method.str.lastIndexOf("}", method.str.length()) + "}\n".length(); else here = ptr;
                    method.insert(here, str);
                }
            }
        }
        pgs.replace(oldmethod, method.str);
        if (writable) {
            if (lang == javaLang) pattern = "public void write_attr_hardware("; else pattern = "::write_attr_hardware";
            if ((start = pgs.str.indexOf(pattern)) < 0) throw new PogoException("\'" + pattern + "\' method not found");
            start = pgs.inMethod(start) + 1;
            end = pgs.outMethod(start) - 2;
            method = new PogoString(pgs.str.substring(start, end));
            oldmethod = method.str;
            if ((ptr = method.str.indexOf("Switch on attribute name")) < 0) throw new PogoException("\'Switch on attribute name\' not found");
            ptr = method.nextCr(ptr) + 1;
            ptr = method.nextCr(ptr) + 1;
            for (int i = 0, n = 0; i < pogo.attributes.size(); i++) {
                Attrib attr = pogo.attributes.attributeAt(i);
                tab = (lang == javaLang) ? "\t\t\t" : "\t\t";
                if (attr.getWritable()) {
                    String target = "\"" + attr.name + "\"";
                    if (method.str.indexOf(target, ptr) < 0) {
                        String str = "";
                        if (n != 0) str += tab + "else\n";
                        str += tab + "if (attr_name == " + target + ")\n" + tab + "{\n" + tab + "\t//	Add your own code here\n" + tab + "}\n";
                        int here;
                        if (n != 0) {
                            here = method.str.lastIndexOf("}", method.str.length());
                            here = method.str.lastIndexOf("}", here - 1) + "}\n".length();
                        } else here = ptr;
                        method.insert(here, str);
                    }
                    n++;
                }
            }
            pgs.replace(oldmethod, method.str);
        }
        return pgs.toString();
    }

    protected void buildReadMeFile(String f_in, String f_out) {
        try {
            PogoString pgs = new PogoString(PogoUtil.readFile(f_in));
            pgs = PogoUtil.removeLogMessages(pgs);
            while (pgs.str.indexOf(templateClass) > 0) pgs.replace(templateClass, pogo.class_name);
            FileOutputStream fidout = new FileOutputStream(f_out);
            fidout.write(pgs.str.getBytes());
            if (pogo.language == javaLang) {
                for (int i = 0; i < pogo.commands.size(); i++) {
                    Cmd cmd = pogo.commands.cmdAt(i);
                    if (!cmd.virtual_method) {
                        String str = cmd.name + "Cmd" + javaExtention + ":\n\t" + "Java source code for the command " + cmd.name + "\n\t" + cmd.description + "\n\n";
                        fidout.write(str.getBytes());
                    }
                }
            }
            fidout.close();
        } catch (Exception ex) {
            System.out.println("README Cannot be generated\n" + ex.toString());
        }
    }

    private String buildProxyMethods() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < pogo.commands.size(); i++) {
            Cmd cmd = pogo.commands.cmdAt(i);
            sb.append(cmd.buildJavaExecCmdMethodComments());
            sb.append(cmd.buildJavaExecCmdMethodSignature(CLIENT_PROXY));
        }
        sb.append("\n\n");
        return sb.toString();
    }

    protected void buildClientProxyFile(String template, String proxyfile, String proxyclass) throws PogoException, IOException, FileNotFoundException {
        if (mustGenerate(proxyfile)) {
            System.out.println("Generating " + proxyfile);
            PogoString pgs = new PogoString(PogoUtil.readFile(template));
            pgs = PogoUtil.removeLogMessages(pgs);
            while (pgs.str.indexOf("TangoclassProxy") >= 0) pgs.replace("TangoclassProxy", proxyclass);
            PogoUtil.writeFile(proxyfile, pgs.str);
        }
        PogoString pgs = new PogoString(PogoUtil.readFile(proxyfile));
        int start, end;
        String constructor = "public " + proxyclass + "(String devname) throws DevFailed";
        end = pgs.str.indexOf(constructor);
        end = pgs.inMethod(end);
        end = pgs.outMethod(end) + 1;
        constructor = pgs.str.substring(0, end);
        start = pgs.str.indexOf(mainSignature);
        start = pgs.str.lastIndexOf("/**", start);
        start = pgs.str.lastIndexOf("//==========", start);
        String main = pgs.str.substring(start);
        PogoUtil.writeFile(proxyfile, constructor + buildProxyMethods() + main);
    }
}
