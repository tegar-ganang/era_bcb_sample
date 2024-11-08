package pogo.gene;

import fr.esrf.TangoDs.TangoConst;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

/**
 *	This class generates Cpp source file for Server files.
 *
 * @author	$Author: pascal_verdier $
 * @version	$Revision: 3.16 $
 */
public class CppServer extends PogoGene implements PogoDefs, TangoConst {

    private PropertyTable properties;

    private static String readprop_method_name = "get_device_property";

    private String readprop_method_signature;

    private String init_signature;

    private String classname;

    public CppServer(PogoClass pogo) {
        super(pogo);
        classname = pogo.class_name;
        properties = pogo.dev_properties;
        init_signature = "void " + classname + "::" + "init_device";
        readprop_method_signature = "void " + classname + "::" + readprop_method_name + "()";
    }

    private String addPropertyDataMembers(PogoString readcode) throws PogoException {
        int start, end;
        String tagStr = "Device properties member data.";
        if ((start = readcode.str.indexOf(tagStr)) < 0) {
            if ((start = readcode.str.indexOf("Attributs member data.")) < 0) throw new PogoException("Tags not found in header file");
            start = readcode.inMethod(start) + 1;
            start = readcode.outMethod(start) + 1;
            String sb = "/**\n";
            sb += " * @name Device properties\n";
            sb += " * " + tagStr + "\n */\n";
            sb += "//@{\n//@}\n\n";
            readcode.insert(start, sb);
        }
        start = readcode.inMethod(start) + 2;
        end = readcode.str.indexOf("//@}", start);
        String old = readcode.str.substring(start, end);
        String sb = "";
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.propertyAt(i);
            sb += "/**\n";
            sb += " *\t" + new PogoString(property.description).setComments() + "\n";
            sb += " */\n";
            String type = property.type.cpp;
            if (type.equals("Tango::DevString")) type = "string";
            sb += "\t" + type + "\t" + property.getVarName() + ";\n";
        }
        if (old.length() > 0) readcode.replace(start, old, sb); else readcode.insert(start, sb);
        if (readcode.str.indexOf(readprop_method_name) < 0) {
            start = readcode.str.indexOf("Here is the end of the automatic");
            while (readcode.str.charAt(start) != '}') start--;
            start = readcode.previousCr(start);
            sb = "";
            sb += "/**\n";
            sb += " *	Read the device properties from database\n";
            sb += " */\n";
            sb += "\t void " + readprop_method_name + "();\n";
            readcode.insert(start, sb);
        }
        return readcode.toString();
    }

    protected String addPrototypes(PogoString readcode) throws PogoException {
        String newcode = "";
        int start = -1;
        int end;
        for (int i = 1; start < 0 && i < PogoUtil.cpp_target.length; i++) start = readcode.str.indexOf(PogoUtil.cpp_target[i]);
        if (start < 0) if ((start = readcode.str.indexOf(": public " + pogo.inheritedNamespace() + "::" + pogo.inheritedClassName())) < 0) {
            System.out.println("Do not found\n" + ": public " + pogo.inheritedNamespace() + "::" + pogo.inheritedClassName());
            throw new PogoException("Input File Syntax error !", "\"public Tango::Device_3Impl\" NOT FOUND");
        }
        while (start > 0 && readcode.str.charAt(start) != '\n') start--;
        String header = setProjectTitle(readcode.str.substring(0, start));
        if ((end = header.indexOf(classDescRes)) > 0) {
            end = header.indexOf(" * ", end) + 3;
            newcode += header.substring(0, end);
        } else {
            newcode += header;
            newcode += "\n/**\n * " + classDescRes + "\n * ";
        }
        if (pogo.class_desc != null) newcode += new PogoString(pogo.class_desc).setComments();
        newcode += "\n */\n";
        newcode += "\n/*\n";
        newcode += pogo.states.toStringComments(cppLang);
        newcode += " */\n\n";
        if ((end = readcode.str.indexOf("//@{")) < 0) throw new PogoException("Input File Syntax error !", "tag:  \"//@{\"   NOT FOUND");
        while (readcode.str.charAt(end) != '\n') end++;
        end++;
        newcode += readcode.str.substring(start, end);
        for (int i = 0; i < pogo.attributes.size(); i++) {
            Attrib attr = pogo.attributes.attributeAt(i);
            newcode += attr.cppMemberData();
        }
        String start_proto = "/**\n * @name " + pogo.class_name + " methods prototypes\n */\n\n//@{\n";
        start = readcode.str.indexOf("//@}", end);
        if ((end = readcode.str.indexOf("always_executed_hook")) < 0) {
            if (pogo.is_abstractclass) {
                if ((end = readcode.str.indexOf(start_proto)) < 0) throw new PogoException("Input File Syntax error !", "\"" + start_proto + "\" NOT FOUND");
            } else throw new PogoException("Input File Syntax error !", "\"always_executed_hook\" NOT FOUND");
        }
        newcode += readcode.str.substring(start, end);
        if (!pogo.is_abstractclass) {
            newcode += readcode.str.substring(end, readcode.str.indexOf("\n", end) + 1);
            newcode += "\n//@}\n\n";
        }
        newcode += start_proto;
        if (pogo.attributes.size() > 0) {
            String sig = pogo.attributes.readHardwareFullSignatureMethod(null);
            newcode += sig + ";\n";
        }
        for (int i = 0; i < pogo.attributes.size(); i++) {
            Attrib attr = pogo.attributes.attributeAt(i);
            newcode += attr.readFullSignatureMethod(null) + ";\n";
            if (attr.getWritable()) newcode += attr.writeFullSignatureMethod(null) + ";\n";
        }
        for (int i = 0; i < pogo.attributes.size(); i++) {
            Attrib attr = pogo.attributes.attributeAt(i);
            newcode += attr.allowedFullSignatureMethod(null) + ";\n";
        }
        for (int i = 0; i < pogo.commands.size(); i++) {
            Cmd cmd = pogo.commands.cmdAt(i);
            if (!cmd.virtual_method) newcode += cmd.allowedFullSignatureMethod(null) + ";\n";
        }
        for (int i = 0; i < pogo.commands.size(); i++) {
            Cmd cmd = pogo.commands.cmdAt(i);
            if (!cmd.virtual_method || (cmd.virtual_method && cmd.override_method != NOT_OVERRIDE)) newcode += cmd.buildCppCmdProtypes();
        }
        newcode += "\n//@}\n";
        start = readcode.str.indexOf("Here is the end of the automatic");
        while (start > 0 && readcode.str.charAt(start) != '\n') start--;
        if (start < 0) throw new PogoException("Input File Syntax error !\n\"//@}\" tag NOT FOUND");
        newcode += readcode.str.substring(start);
        return newcode;
    }

    private String toAbstractClassHeader(String str) {
        PogoString code = new PogoString(str);
        String start_proto = "/**\n * @name " + pogo.class_name + " methods prototypes\n */\n\n//@{\n";
        int start = code.str.indexOf("/**@name Destructor");
        int end;
        if (start > 0) {
            end = code.str.indexOf(start_proto, start);
            if (end < 0) {
                System.out.println("WARNING: " + start_proto + "  Not Found !!!");
                return str;
            }
            code.remove(code.str.substring(start, end));
        }
        start = code.str.indexOf("void get_device_property();");
        if (start > 0) {
            end = code.str.indexOf("//@}", start);
            start = code.str.lastIndexOf("/**", start);
            code.remove(code.str.substring(start, end));
        }
        Vector old_lines = new Vector();
        Vector new_lines = new Vector();
        start = code.str.indexOf(start_proto) + start_proto.length();
        start = code.str.indexOf("/**", start);
        if (start < 0) return code.str;
        end = code.str.indexOf("//@}", start);
        while (start < end) {
            start = code.str.indexOf("*/", start) + "*/".length();
            int end_line = code.str.indexOf("/**", start);
            if (end_line < 0) end_line = end;
            String old_line = code.str.substring(start, end_line).trim();
            String new_line = old_line;
            if (!new_line.startsWith("virtual ")) new_line = "virtual " + new_line;
            if (new_line.indexOf("_allowed(") < 0) {
                if (new_line.indexOf("0") < 0) {
                    new_line = new_line.substring(0, new_line.length() - 1) + " = 0;";
                }
            } else {
                if (new_line.indexOf("return true;") < 0) {
                    new_line = new_line.substring(0, new_line.length() - 1) + "{ return true; };";
                }
            }
            old_lines.add(old_line);
            new_lines.add(new_line);
            start = end_line;
        }
        for (int i = 0; i < old_lines.size(); i++) code.replace((String) old_lines.elementAt(i), (String) new_lines.elementAt(i));
        return code.str;
    }

    private PogoString udateConstructorPrototypes(PogoString readcode) {
        String[] old_constr = { pogo.class_name + "(Tango::DeviceClass *,string &)", pogo.class_name + "(Tango::DeviceClass *,const char *)", pogo.class_name + "(Tango::DeviceClass *,const char *,const char *)" };
        String[] new_constr = { pogo.class_name + "(Tango::DeviceClass *cl,string &s)", pogo.class_name + "(Tango::DeviceClass *cl,const char *s)", pogo.class_name + "(Tango::DeviceClass *cl,const char *s,const char *d)" };
        for (int i = 0; i < old_constr.length; i++) readcode.replace(old_constr[i], new_constr[i]);
        if (pogo.is_abstractclass) {
            String from_class = tangoDeviceImpl;
            if (pogo.inherited_from != null) from_class = pogo.inheritedClassName();
            String[] sc_ext = { ":" + from_class + "(cl,s) {}", ":" + from_class + "(cl,s) {}", ":" + from_class + "(cl,s,d) {}" };
            int pos;
            for (int i = 0; i < new_constr.length; i++) if ((pos = readcode.str.indexOf(new_constr[i])) > 0) {
                pos += new_constr[i].length();
                if (readcode.str.substring(pos).trim().charAt(0) == ';') readcode.insert(pos, sc_ext[i]);
            }
        }
        return readcode;
    }

    public void generateHeader(String server_h) throws FileNotFoundException, SecurityException, IOException, PogoException {
        System.out.println("Generating " + server_h + "....");
        boolean from_template = false;
        String filename;
        if (already_exists(server_h)) filename = server_h; else {
            from_template = true;
            filename = pogo.templates_dir + "/cpp/" + templateFile + ".h";
        }
        PogoString readcode = new PogoString(PogoUtil.readFile(filename, templateClass, pogo.class_name));
        if (from_template) {
            readcode = PogoUtil.removeLogMessages(readcode);
            while (readcode.str.indexOf(templateClass.toUpperCase()) >= 0) readcode.replace(templateClass.toUpperCase(), pogo.class_name.toUpperCase());
        } else {
            while (readcode.str.indexOf("DeviceImpl") >= 0) {
                System.out.println("Replacing DeviceImpl by " + tangoDeviceImpl);
                readcode.replace("DeviceImpl", tangoDeviceImpl);
            }
            while (readcode.str.indexOf("Device_2Impl") >= 0) {
                System.out.println("Replacing Device_2Impl by " + tangoDeviceImpl);
                readcode.replace("Device_2Impl", tangoDeviceImpl);
            }
        }
        if (pogo.inherited_from != null) if (!pogo.inherited_from.equals(tangoDeviceImpl)) {
            String inheritance = ": public " + pogo.inheritedNamespace() + "::" + pogo.inheritedClassName() + "	//	" + PogoUtil.cpp_target[0] + tangoDeviceImpl;
            readcode.replace(PogoUtil.cpp_target[1], inheritance);
            String sc_header = "<" + pogo.inheritedClassName() + ".h>";
            if (readcode.str.indexOf(sc_header) < 0) {
                int pos = readcode.str.indexOf("<tango.h>");
                pos = readcode.nextCr(pos) + 1;
                readcode.insert(pos, "#include " + sc_header + "\n");
            }
        }
        readcode = udateConstructorPrototypes(readcode);
        String newcode = addPrototypes(readcode);
        newcode = addPropertyDataMembers(new PogoString(newcode));
        if (pogo.is_abstractclass) {
            newcode = toAbstractClassHeader(newcode);
        }
        PogoUtil.writeFile(server_h, newcode);
    }

    public void generateSource(String server) throws FileNotFoundException, SecurityException, IOException, PogoException {
        System.out.println("Generating " + server + "....");
        String filename;
        boolean src_exist = already_exists(server);
        if (src_exist) filename = server; else filename = pogo.templates_dir + "/cpp/" + templateFile + ".cpp";
        PogoString readcode = new PogoString(PogoUtil.readFile(filename, templateClass, pogo.class_name));
        if (src_exist) {
            while (readcode.str.indexOf("DeviceImpl(cl") >= 0) {
                System.out.println("Replacing DeviceImpl by Device_2Impl");
                readcode.replace("DeviceImpl(cl", "Device_2Impl(cl");
            }
            while (readcode.str.indexOf("Device_2Impl(cl") >= 0) {
                System.out.println("Replacing Device_2Impl by " + tangoDeviceImpl);
                readcode.replace("Device_2Impl(cl", tangoDeviceImpl + "(cl");
            }
            String target = "<" + pogo.class_name + "Class.h>";
            int start = readcode.str.indexOf("<" + pogo.class_name + ".h>");
            start = readcode.nextCr(start) + 1;
            if (readcode.str.indexOf(target, start) < 0) readcode.insert(start, "#include " + target + "\n");
        } else readcode = PogoUtil.removeLogMessages(readcode);
        if (pogo.inherited_from != null) if (!pogo.inherited_from.equals(tangoDeviceImpl)) {
            String inheritance = ":" + pogo.inheritedNamespace() + "::" + pogo.inheritedClassName();
            String target = ":Tango::" + tangoDeviceImpl;
            while (readcode.str.indexOf(target) > 0) readcode.replace(target, inheritance);
        }
        int namespace = readcode.indexOf("namespace " + pogo.namespace());
        if (namespace < 0) namespace = readcode.indexOf("namespace " + pogo.class_name);
        PogoString header = new PogoString(readcode.str.substring(0, namespace));
        readcode = new PogoString(readcode.str.substring(namespace));
        int start = header.indexOf("The following table gives the correspondance");
        if (start < 0) start = header.indexOf("The folowing table gives the correspondance");
        int end = header.indexOf("//==========================", start);
        start = header.lastIndexOf("//==========", start);
        end = header.nextCr(end);
        String old = header.substring(start, end);
        header.replace(old, pogo.commands.addCommentsTable());
        String newcode = addMethodsToExecuteCmd(readcode.str);
        if (src_exist) newcode = checkForExecMethodModif(new PogoString(newcode), cppLang);
        if (pogo.attributes.size() > 0) newcode = buildAttributesMethods(new PogoString(newcode));
        if (properties.size() > 0) newcode = addReadPropMethod(new PogoString(newcode));
        String finalcode = header.str + newcode;
        PogoUtil.writeFile(server, finalcode);
    }

    private String cpp_flag_dev_impl_2 = "#ifdef DEV_IMPL_2\n";

    void comment_read_write_attr_methods(PogoString code, String classname) {
        String pattern = "void " + classname + "::read_attr(Tango::Attribute &attr)";
        int start, end;
        if ((start = code.str.indexOf(pattern)) > 0) {
            end = code.inMethod(start);
            end = code.outMethod(end);
            code.insert(end, "#endif\n");
            code.insert(start, cpp_flag_dev_impl_2);
        }
        pattern = "void " + classname + "::write_attr_hardware(vector<long> &attr_list)";
        if ((start = code.str.indexOf(pattern)) > 0) {
            end = code.inMethod(start);
            end = code.outMethod(end);
            code.insert(end, "#endif\n");
            code.insert(start, cpp_flag_dev_impl_2);
        }
    }

    private String buildAttributesMethods(PogoString code) {
        int insert_pos = code.indexOf(pogo.class_name + "::always_executed_hook()");
        insert_pos = code.inMethod(insert_pos);
        insert_pos = code.outMethod(insert_pos);
        String signature = pogo.attributes.readHardwareSignatureMethod(pogo.class_name);
        int start;
        if ((start = code.indexOf(signature)) < 0) {
            String method = pogo.attributes.readHardwareFullSignatureMethod(pogo.class_name);
            method += "\n{\n" + PogoUtil.enteringTrace(signature) + "	//	Add your own code here\n" + "}\n\n";
            code.insert(insert_pos, method);
            insert_pos += method.length();
        } else {
            insert_pos = code.inMethod(start);
            insert_pos = code.outMethod(insert_pos);
        }
        for (int i = 0; i < pogo.attributes.size(); i++) {
            Attrib attr = pogo.attributes.attributeAt(i);
            signature = attr.readSignatureMethod(pogo.class_name);
            if (code.indexOf(signature) < 0) {
                String method = attr.readFullSignatureMethod(pogo.class_name);
                method += "\n{\n" + PogoUtil.enteringTrace(signature);
                method += attr.getDevImpl2ReadAttr(code, pogo.class_name);
                method += "}\n\n";
                code.insert(insert_pos, method);
                insert_pos += method.length();
            }
            if (attr.getWritable()) {
                signature = attr.writeSignatureMethod(pogo.class_name);
                if (code.indexOf(signature) < 0) {
                    String method = attr.writeFullSignatureMethod(pogo.class_name);
                    method += "\n{\n" + PogoUtil.enteringTrace(signature);
                    method += attr.getDevImpl2WriteAttr(code, pogo.class_name);
                    method += "}\n\n";
                    code.insert(insert_pos, method);
                    insert_pos += method.length();
                }
            }
        }
        comment_read_write_attr_methods(code, pogo.class_name);
        return code.str;
    }

    protected String addMethodsToExecuteCmd(String readcode) throws PogoException {
        StringBuffer newcode = new StringBuffer();
        int start = 0;
        int end;
        boolean namespace_exist;
        String namespace = "namespace " + pogo.namespace();
        if (readcode.indexOf(namespace) < 0 && readcode.indexOf("namespace " + pogo.class_name) < 0) {
            newcode.append(readcode.substring(start));
            namespace_exist = false;
        } else {
            end = readcode.length() - 1;
            while (readcode.charAt(end) != '}') end--;
            while (readcode.charAt(end) != '\n') end--;
            newcode.append(readcode.substring(start, end));
            namespace_exist = true;
        }
        for (int i = 0; i < pogo.commands.size(); i++) {
            Cmd cmd = pogo.commands.cmdAt(i);
            if (!cmd.virtual_method || cmd.override_method == OVERRIDE) {
                String line = cmd.buildCppExecCmdMethodSignature(pogo.class_name);
                int sl = line.indexOf("(");
                int el = line.indexOf(" ", sl);
                if (el < 0) el = sl + 1;
                line = line.substring(0, el);
                if (readcode.indexOf(line) < 0) {
                    newcode.append("\n");
                    newcode.append(cmd.buildCppExecCmdMethodComments(pogo.class_name));
                    newcode.append(cmd.buildCppExecCmdMethod(pogo.class_name));
                    if (cmd.virtual_method) cmd.override_method = ALREADY_OVERRIDED;
                }
            }
        }
        if (namespace_exist) newcode.append("\n}	//	namespace\n");
        return newcode.toString();
    }

    private String addReadPropMethod(PogoString readcode) throws FileNotFoundException, SecurityException, IOException, PogoException {
        if (properties.size() == 0) return readcode.str;
        String templatefile = pogo.templates_dir + "/cpp/readPropMethodName.cpp";
        PogoString method = new PogoString(PogoUtil.readFile(templatefile, templateClass, pogo.class_name));
        while (method.str.indexOf("target") >= 0) method.replace("target", "device");
        String template_method = method.str;
        int start, end;
        if ((start = readcode.str.indexOf(readprop_method_signature)) < 0) {
            if ((start = readcode.str.indexOf(init_signature)) < 0) throw new PogoException("Syntax error in existing Source file", init_signature + " not found ");
            start = readcode.inMethod(start);
            start = readcode.outMethod(start) + 1;
            readcode.insert(start, template_method);
        }
        String pattern = "Automatic code generation";
        if ((start = readcode.str.indexOf(pattern, start)) <= 0) throw new PogoException("Syntax error in existing Source file", "\"" + pattern + "\"  Not Found");
        start = readcode.nextCr(start) + 1;
        start = readcode.nextCr(start) + 1;
        if ((end = readcode.str.indexOf(pattern, start)) < 0) throw new PogoException("Syntax error in existing Source file", "\"" + pattern + "\"  Not Found");
        end = readcode.previousCr(end) - 1;
        String oldCode = readcode.str.substring(start, end);
        String sb = "\tTango::DbData	dev_prop;\n";
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.propertyAt(i);
            sb += "\tdev_prop.push_back(Tango::DbDatum(\"" + property.name + "\"));\n";
        }
        sb += "\n";
        sb += "\t//	Call database and extract values\n";
        sb += "\t//--------------------------------------------\n";
        sb += "\tif (Tango::Util::instance()->_UseDb==true)\n";
        sb += "\t\tget_db_device()->get_property(dev_prop);\n";
        sb += "\tTango::DbDatum	def_prop, cl_prop;\n";
        sb += "\t" + classname + "Class	*ds_class =\n";
        sb += "\t\t(static_cast<" + classname + "Class *>(get_device_class()));\n";
        sb += "\tint	i = -1;\n\n";
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.propertyAt(i);
            sb += "\t//	Try to initialize " + property + " from class property\n";
            sb += "\tcl_prop = ds_class->get_class_property(dev_prop[++i].name);\n";
            sb += "\tif (cl_prop.is_empty()==false)	cl_prop  >>  " + property.getVarName() + ";\n";
            sb += "\t//	Try to initialize " + property + " from default device value\n";
            sb += "\tdef_prop = ds_class->get_default_device_property(dev_prop[i].name);\n";
            sb += "\tif (def_prop.is_empty()==false)	def_prop  >>  " + property.getVarName() + ";\n";
            sb += "\t//	And try to extract " + property + " value from database\n";
            sb += "\tif (dev_prop[i].is_empty()==false)	dev_prop[i]  >>  " + property.getVarName() + ";\n";
            sb += "\n";
        }
        sb += "\n";
        readcode.replace(start, oldCode, sb);
        addReadPropMethodCall(readcode);
        return readcode.str;
    }

    private void addReadPropMethodCall(PogoString readcode) throws PogoException {
        int start, end;
        if ((start = readcode.str.indexOf(init_signature)) < 0) throw new PogoException("Syntax error in existing Source file");
        start = readcode.inMethod(start);
        end = readcode.outMethod(start);
        String meth_core = readcode.str.substring(start, end);
        if (meth_core.indexOf(readprop_method_name) >= 0) return;
        start = readcode.str.indexOf("Initialise variables to default values", start);
        start = readcode.nextCr(start) + 1;
        start = readcode.nextCr(start) + 1;
        readcode.insert(start, "\t" + readprop_method_name + "();\n");
    }
}
