package pogo.gene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

public class AttribTable extends Vector implements PogoDefs {

    Exception except = null;

    public AttribTable() {
    }

    public AttribTable(Vector v_in) {
        for (int i = 0; i < v_in.size(); i++) addElement(v_in.elementAt(i));
    }

    public AttribTable(ProjectFiles prjfiles, int lang) throws FileNotFoundException, SecurityException, IOException, PogoException {
        boolean toWWW = (System.getProperty("CVSROOT") != null);
        String filename = prjfiles.getServerClass();
        int devimpl = javaDeviceImpl;
        if (lang == cppLang) devimpl = PogoUtil.deviceImplRevisionNumber(prjfiles.getServer_h());
        PogoString pgs = new PogoString(PogoUtil.readFile(filename));
        if (toWWW) System.out.println("Reading " + filename);
        int start, end;
        if (lang == pyLang) {
            parsePyAttributes(pgs);
            return;
        } else if ((start = pgs.str.indexOf("attribute_factory(")) < 0) {
            System.out.println("Attribute factory not found in :\n" + filename);
            return;
        }
        PogoString method = new PogoString(pgs.extractMethodCore(start));
        if (lang == javaLang || devimpl < 3) {
            start = 0;
            String patern = "Attr(\"";
            while ((start = method.str.indexOf(patern, start)) >= 0) {
                start = method.str.lastIndexOf("\n", start);
                Attrib attr;
                end = method.str.indexOf("(", start) + 1;
                String constructor = method.str.substring(start, end);
                start = end;
                end = method.str.indexOf(")", start);
                PogoString s = new PogoString(method.str.substring(start, end));
                String[] params = s.getParams();
                int type = ATTR_READ;
                if (constructor.indexOf("Image") >= 0) {
                    int xSize;
                    int ySize;
                    if (params.length > 4) {
                        for (int i = 0; i < AttrRWtypeArray.length; i++) {
                            String target;
                            if (lang == javaLang) target = "AttrWriteType." + AttrRWtypeArray[i]; else target = PogoDefs.cppNameSpace + AttrRWtypeArray[i];
                            if (params[2].equals(target)) type = i;
                        }
                        try {
                            xSize = (new Integer(params[3])).intValue();
                            ySize = (new Integer(params[4])).intValue();
                        } catch (NumberFormatException e) {
                            if (toWWW) xSize = ySize = 1; else throw e;
                        }
                    } else {
                        try {
                            xSize = (new Integer(params[2])).intValue();
                            ySize = (new Integer(params[3])).intValue();
                        } catch (NumberFormatException e) {
                            if (toWWW) xSize = ySize = 1; else throw e;
                        }
                    }
                    attr = new Attrib(params[0], ATTR_IMAGE, params[1], type, xSize, ySize);
                } else if (constructor.indexOf("Spectrum") >= 0) {
                    int xSize;
                    if (params.length > 3) {
                        for (int i = 0; i < AttrRWtypeArray.length; i++) {
                            String target;
                            if (lang == javaLang) target = "AttrWriteType." + AttrRWtypeArray[i]; else target = PogoDefs.cppNameSpace + AttrRWtypeArray[i];
                            if (params[2].equals(target)) type = i;
                        }
                        try {
                            xSize = (new Integer(params[3])).intValue();
                        } catch (NumberFormatException e) {
                            if (toWWW) xSize = 1; else throw e;
                        }
                    } else {
                        try {
                            xSize = (new Integer(params[2])).intValue();
                        } catch (NumberFormatException e) {
                            if (toWWW) xSize = 1; else throw e;
                        }
                    }
                    attr = new Attrib(params[0], ATTR_SPECTRUM, params[1], type, xSize);
                } else {
                    for (int i = 0; i < AttrRWtypeArray.length; i++) {
                        String target;
                        if (lang == javaLang) target = "AttrWriteType." + AttrRWtypeArray[i]; else target = PogoDefs.cppNameSpace + AttrRWtypeArray[i];
                        if (params.length > 2) if (params[2].equals(target)) type = i;
                    }
                    if (params.length == 4 && type == ATTR_READ_WITH_WRITE) attr = new Attrib(params[0], ATTR_SCALAR, params[1], type, params[3]); else attr = new Attrib(params[0], ATTR_SCALAR, params[1], type, "");
                }
                attr.getDefaultProperties(method, lang);
                addElement(attr);
            }
        } else {
            String header = PogoUtil.readFile(prjfiles.getServerClass_h());
            String pattern = "Attribute : ";
            start = 0;
            while ((start = method.indexOf(pattern, start)) > 0) {
                if (method.isCommentLine(start)) {
                    start += pattern.length();
                    end = method.nextCr(start);
                    String name = method.substring(start, end);
                    Attrib attr = new Attrib(name, header);
                    attr.getDefaultProperties(method, lang);
                    addElement(attr);
                }
            }
        }
    }

    private void parsePyAttributes(PogoString code) throws PogoException {
        int start = code.str.indexOf("Class(PyTango.PyDeviceClass):");
        if (start < 0) throw new PogoException("No DeviceClass class found !");
        start = code.str.indexOf("attr_list", start);
        if (start < 0) throw new PogoException("No Attribute list found !");
        start = code.inMethod(start);
        int end = code.outMethod(start);
        String attr_list = code.str.substring(start, end);
        String target = "[[";
        end = 0;
        while ((start = attr_list.indexOf(target, end)) > 0) {
            end = attr_list.lastIndexOf("\'", start - 1);
            start = attr_list.lastIndexOf("\'", end - 1);
            String name = attr_list.substring(start + 1, end).trim();
            start = end + target.length();
            end = attr_list.indexOf(",", start);
            start = attr_list.lastIndexOf(".", end);
            String type = "Tango::" + attr_list.substring(start + 1, end).trim();
            start = end + 1;
            end = attr_list.indexOf(",", start);
            start = attr_list.lastIndexOf(".", end);
            String s_att_type = attr_list.substring(start + 1, end).trim();
            int att_type = ATTR_SCALAR;
            for (int i = 0; i < AttrTypeArray.length; i++) if (s_att_type.equals(AttrTypeArray[i])) att_type = i;
            start = end + 1;
            end = attr_list.indexOf("]", start);
            start = attr_list.lastIndexOf(".", end);
            int dim_x = 1;
            int dim_y = 1;
            int idx = end;
            switch(att_type) {
                case ATTR_SCALAR:
                    break;
                case ATTR_SPECTRUM:
                    end = attr_list.lastIndexOf(",", end);
                    try {
                        dim_x = Integer.parseInt(attr_list.substring(end + 1, idx).trim());
                    } catch (NumberFormatException e) {
                        System.out.println(e);
                    }
                    break;
                case ATTR_IMAGE:
                    end = attr_list.lastIndexOf(",", end);
                    int idx2 = end;
                    end = attr_list.lastIndexOf(",", end - 1);
                    try {
                        dim_x = Integer.parseInt(attr_list.substring(end + 1, idx2).trim());
                        dim_y = Integer.parseInt(attr_list.substring(idx2 + 1, idx).trim());
                    } catch (NumberFormatException e) {
                        System.out.println(e);
                    }
                    break;
            }
            String s_rw_type = attr_list.substring(start + 1, end).trim();
            int rw_type = ATTR_READ;
            for (int i = 0; i < AttrRWtypeArray.length; i++) if (s_rw_type.equals(AttrRWtypeArray[i])) rw_type = i;
            Attrib attr = new Attrib(name, att_type, type, rw_type, dim_x, dim_y);
            start = idx + 1;
            end = attr_list.indexOf("]", start);
            String str_prop = attr_list.substring(start, end);
            attr.setPyProperties(str_prop);
            attr.notAllowedFor.setPyNotAllowedFor(code.str, name);
            this.add(attr);
        }
    }

    void setAllowedState(String class_name, String filename, boolean is_abstractclass) throws FileNotFoundException, SecurityException, IOException, PogoException {
        PogoString readcode;
        try {
            readcode = new PogoString(PogoUtil.readFile(filename));
        } catch (FileNotFoundException e) {
            if (is_abstractclass) return; else {
                except = e;
                return;
            }
        }
        for (int i = 0; i < size(); i++) {
            Attrib attr = attributeAt(i);
            String signature = attr.allowedFullSignatureMethod(class_name);
            int start;
            if ((start = readcode.indexOf(signature)) > 0) {
                String method = readcode.extractMethodCore(start);
                attr.notAllowedFor = new DevStateTable(method);
            }
        }
    }

    public Attrib attributeAt(int idx) {
        return ((Attrib) (elementAt(idx)));
    }

    public String buildFactory(int lang) {
        if (lang == pyLang) {
            String str = "";
            for (int i = 0; i < size(); i++) {
                Attrib attr = attributeAt(i);
                str += attr.buildCodeForAttributeConstructor(lang);
            }
            return str;
        } else {
            String tab = (lang == javaLang) ? "\t" : "";
            String str = tab + "{\n";
            String addKeyWord = (lang == javaLang) ? javaAddAttrib : cppAddAttrib;
            for (int i = 0; i < size(); i++) {
                Attrib attr = attributeAt(i);
                str += tab + "\t" + "//	Attribute : " + attr.name + "\n";
                str += attr.buildCodeForAttributeConstructor(lang);
                str += attr.buildCodeForDefaultProperties(lang);
                str += tab + "\t" + addKeyWord + "(" + attr.getLowerName() + ");\n\n";
            }
            if (lang == cppLang) {
                str += "\t//\tEnd of Automatic code generation\n" + "\t//-------------------------------------------------------------\n";
            }
            str += tab + "}\n";
            return str;
        }
    }

    String readHardwareFullSignatureMethod(String class_name) {
        return signature(class_name, true);
    }

    String readHardwareSignatureMethod(String class_name) {
        return signature(class_name, false);
    }

    private String signature(String class_name, boolean full) {
        PogoString pgs = new PogoString(readAttrhardwareTemplate);
        if (class_name == null) pgs.replace("CLASS::", ""); else {
            pgs.replace("CLASS", class_name);
            if (full) {
                String target = " *\t";
                int start = pgs.indexOf(target, "/**".length());
                start += target.length();
                int end = pgs.nextCr(start);
                String desc = pgs.substring(start, end);
                PogoString separator = new PogoString(cppMethodSeparatorTemplate);
                separator.replace("CLASS", class_name);
                separator.replace("METHOD", "read_attr_hardware");
                separator.replace("DESCRIPTION", desc);
                target = " */\n\tvirtual ";
                end = pgs.indexOf(target);
                String comments = pgs.substring(0, end + target.length());
                pgs.replace(comments, separator.str);
            }
        }
        if (!full) {
            String target = " */\n\tvirtual ";
            int start = pgs.indexOf(target);
            start += target.length();
            return pgs.substring(start);
        }
        return pgs.str;
    }

    public String[] isJavaCompatible() {
        String[] AttrDataArray = { "Tango::DEV_BOOLEAN", "Tango::DEV_SHORT", "Tango::DEV_LONG", "Tango::DEV_DOUBLE", "Tango::DEV_STRING" };
        Vector v = new Vector();
        for (int i = 0; i < size(); i++) {
            Attrib attr = attributeAt(i);
            if (attr.memorized) v.add("Attribute memorized  (" + attr.name + ")");
            boolean found = false;
            for (int t = 0; !found && t < AttrDataArray.length; t++) found = (attr.dataType.cpp_code_str.equals(AttrDataArray[t]));
            if (!found) v.add("Attribute type " + attr.dataType.cpp_code_str + "  (" + attr.name + ")");
            if (attr.getWritable() && attr.attrType != ATTR_SCALAR) v.add("Write an attribute " + AttrTypeArray[attr.attrType] + "  (" + attr.name + ")");
        }
        if (v.size() == 0) return null; else {
            String[] problems = new String[v.size()];
            System.out.println("Java Servers API do not support:");
            for (int i = 0; i < v.size(); i++) {
                problems[i] = (String) v.elementAt(i);
                System.out.println(problems[i]);
            }
            return problems;
        }
    }

    String updatePyAttributes(String code, String readcode, String template, String class_name) throws PogoException {
        String target = "attr.set_value(";
        int start = template.indexOf(target);
        if (start < 0) throw new PogoException("Attribute template syntax error.");
        start = template.indexOf("\n", start) + 1;
        String read_templ = template.substring(0, start);
        int idx = start;
        target = "#\tAdd your own code here";
        start = template.indexOf(target, start);
        if (start < 0) throw new PogoException("Attribute template syntax error.");
        start += target.length() + 1;
        String write_templ = template.substring(idx, start);
        String stm_templ = "\t" + template.substring(start).trim();
        int insert_pos = code.indexOf("def read_attr_hardware(self,");
        insert_pos = PogoUtil.endOfPythonMethod(code, insert_pos);
        code = code.substring(0, insert_pos) + "\n\n" + code.substring(insert_pos);
        insert_pos += 2;
        for (int i = size() - 1; i >= 0; i--) {
            Attrib attr = attributeAt(i);
            String read_method;
            String signature = "def read_" + attr.name + "(self, attr):";
            if (readcode.indexOf(signature) > 0) {
                read_method = PogoUtil.pythonMethod(readcode, signature);
            } else read_method = attr.buildPyReadMethod(read_templ);
            read_method = "\n" + read_method.trim() + "\n\n";
            String write_method = null;
            if (attr.rwType == ATTR_WRITE || attr.rwType == ATTR_READ_WRITE) {
                signature = "def write_" + attr.name + "(self, attr):";
                if (readcode.indexOf(signature) > 0) {
                    write_method = PogoUtil.pythonMethod(readcode, signature);
                } else write_method = attr.buildPyWriteMethod(write_templ);
                write_method = "\n" + write_method.trim() + "\n\n";
            }
            String stm_method = null;
            signature = "def is_" + attr.name + "_allowed(self, req_type):";
            int sigpos = readcode.indexOf(signature);
            if (sigpos > 0 || attr.notAllowedFor.size() > 0) {
                if (sigpos > 0) {
                    stm_method = PogoUtil.pythonMethod(readcode, signature);
                } else stm_method = attr.buildPyStateMachineMethod(stm_templ);
                stm_method = attr.pyUpdateAllowedStates(stm_method, signature);
                stm_method = "\n" + stm_method.trim() + "\n\n";
            }
            if (stm_method != null) code = code.substring(0, insert_pos) + stm_method + code.substring(insert_pos);
            if (write_method != null) code = code.substring(0, insert_pos) + write_method + code.substring(insert_pos);
            code = code.substring(0, insert_pos) + read_method + code.substring(insert_pos);
        }
        return code;
    }

    public String toString() {
        String str = "";
        for (int i = 0; i < size(); i++) {
            Attrib attr = attributeAt(i);
            str += attr.name + "\t";
            str += attr.dataType.cpp_code_str + "\n";
        }
        return str;
    }

    public static void main(java.lang.String[] args) {
        String filename = "/segfs/tango/tools/pogo/test/cpp/PowerSupply/PowerSupply.h";
        try {
            ProjectFiles prj = new ProjectFiles(filename);
            AttribTable table = new AttribTable(prj, cppLang);
            System.out.println(table);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
