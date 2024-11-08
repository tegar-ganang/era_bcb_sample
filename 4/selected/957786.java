package org.tango.pogo.pogo_gui;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoDs.Except;
import fr.esrf.tango.pogo.pogoDsl.*;
import org.eclipse.emf.common.util.EList;
import org.tango.pogo.pogo_gui.tools.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * This class is able to comnvert old pogo project
 * (not generated with OAW) to new model.
 *
 * @author verdier
 */
public class OldPogoModel {

    private PogoDeviceClass new_model = null;

    private pogo.gene.PogoClass old_model = null;

    public static final String[] cppFilenames = { "Makefile", "main.cpp", "ClassFactory.cpp", "ClassName.h", "ClassName.cpp", "ClassNameClass.h", "ClassNameClass.cpp", "ClassNameStateMachine.cpp" };

    @SuppressWarnings({ "ConstantConditions" })
    public OldPogoModel(String filename) throws DevFailed {
        new_model = OAWutils.factory.createPogoDeviceClass();
        new_model.setDescription(OAWutils.factory.createClassDescription());
        try {
            System.out.println("Trying to load " + filename);
            if (System.getProperty("TEMPL_HOME") == null) System.setProperty("TEMPL_HOME", "");
            old_model = new pogo.gene.PogoClass(filename);
            new_model.setName(old_model.class_name);
            new_model.getDescription().setDescription(old_model.class_desc);
            new_model.getDescription().setLanguage(PogoConst.strLang[old_model.language]);
            new_model.getDescription().setSourcePath(Utils.getPath(filename));
            new_model.getDescription().setTitle(old_model.title);
            Inheritance inheritance = OAWutils.factory.createInheritance();
            String inherPath = System.getProperty("SUPER_HOME");
            if (inherPath == null) inherPath = "";
            String inherClass = old_model.inherited_from;
            int pos = inherClass.lastIndexOf('/');
            if (pos < 0) pos = inherClass.lastIndexOf('\\');
            if (pos > 0) {
                inherPath = inherClass.substring(0, pos);
                inherClass = inherClass.substring(pos + 1);
            }
            inheritance.setClassname(inherClass);
            inheritance.setSourcePath(inherPath);
            EList<Inheritance> inher = new_model.getDescription().getInheritances();
            inher.add(inheritance);
            if (old_model.device_id != null) if (!old_model.device_id.failed) {
                ClassIdentification ident = OAWutils.factory.createClassIdentification();
                DeviceIdDialog.buildIdContact(ident, old_model.device_id.contact);
                ident.setPlatform(old_model.device_id.platform);
                ident.setBus(old_model.device_id.bus);
                ident.setManufacturer(old_model.device_id.manufacturer);
                ident.setReference(old_model.device_id.reference);
                ident.setClassFamily(old_model.device_id.family);
                ident.setSiteSpecific(old_model.device_id.site);
                new_model.getDescription().setIdentification(ident);
            } else System.err.println("old_model.device_id Failed !");
            EList<Property> classprops = new_model.getClassProperties();
            EList<Property> devprops = new_model.getDeviceProperties();
            EList<Command> commands = new_model.getCommands();
            EList<Attribute> attributes = new_model.getAttributes();
            EList<State> states = new_model.getStates();
            manageOldProperties(classprops, old_model.class_properties);
            manageOldProperties(devprops, old_model.dev_properties);
            manageOldCommands(commands, old_model.commands);
            manageOldAttributes(attributes, old_model.attributes);
            manageOldStates(states, old_model.states);
            System.out.println(filename + "  Loaded !!!");
        } catch (Exception e) {
            e.printStackTrace();
            Except.throw_exception("Loading OLD Pogo Class Failed", e.toString() + "\n\n" + "Loading OLD Pogo Class Failed\n", "OldPogoModel.OldPogoModel()");
        }
    }

    public PogoDeviceClass getPogoDeviceClass() {
        return new_model;
    }

    public boolean isAbsAbstract() {
        return old_model.is_abstractclass;
    }

    public void recoverCppCodeFromOldPogoModel(PogoDeviceClass new_model) throws Exception {
        PogoParser new_parser;
        OldModelParser old_parser;
        String code;
        String classname = new_model.getName();
        String filename = new_model.getDescription().getSourcePath() + "/" + new_model.getName();
        new_parser = new PogoParser(filename + ".h");
        old_parser = new OldModelParser(old_model.projectFiles.getServer_h(), old_model);
        code = getIncludeFiles(".h");
        code += getDefinitions();
        if (code != null && code.length() > 0) new_parser.insertIncludeFiles(code);
        code = "public:\n" + old_parser.getDataMembers();
        new_parser.insertInProtectedZone(classname, "Data Members", code);
        code = old_parser.getAdditionalMethodPrototypes();
        new_parser.insertAdditionalMethodPrototypes(classname, code);
        code = old_parser.getAdditionalClassDeclarations();
        new_parser.insertAdditionalClassDefs(classname, code);
        code = old_parser.getAdditionalClasses();
        new_parser.insertAdditionalClasses(classname, code);
        new_parser.write();
        System.out.println(filename + ".h  updated");
        new_parser = new PogoParser(filename + ".cpp");
        old_parser = new OldModelParser(old_model.projectFiles.getServer(), old_model);
        code = getIncludeFiles(".cpp");
        new_parser.insertIncludeFiles(code);
        code = getStaticInit(old_parser);
        new_parser.insertInProtectedZone(classname, "namespace_starting", code);
        code = getInitDeviceCodeBefore(old_parser);
        new_parser.insertInProtectedZone(classname, "init_device_before", code);
        code = getInitDeviceCodeAfter(old_parser);
        new_parser.insertInProtectedZone(classname, "init_device", code);
        code = getDeleteDeviceCode(old_parser);
        new_parser.insertInProtectedZone(classname, "delete_device", code);
        code = getInitPropertyCode(old_parser, true);
        new_parser.insertInProtectedZone(classname, "get_device_property_before", code);
        code = getEndPropertyCode(old_parser, true);
        new_parser.insertInProtectedZone(classname, "get_device_property_after", code);
        code = getAlwaysExecutedHookCode(old_parser);
        new_parser.insertInProtectedZone(classname, "always_executed_hook", code);
        EList<Command> commands = new_model.getCommands();
        for (Command cmd : commands) {
            code = getCommandCode(old_parser, cmd);
            if (code != null && code.length() > 0) System.out.println("inserting code for command" + cmd.getName());
            new_parser.insertInProtectedZone(classname, cmd.getExecMethod(), code);
        }
        code = getReadAttrHardwareCode(old_parser);
        new_parser.insertInProtectedZone(classname, "read_attr_hardware", code);
        EList<Attribute> attributes = new_model.getAttributes();
        for (Attribute att : attributes) {
            code = getReadAttributeCode(old_parser, att);
            if (code != null && code.length() > 0) System.out.println("inserting code for attribute " + att.getName());
            new_parser.removeProtectedZone(classname, "read_" + att.getName());
            new_parser.insertInProtectedZone(classname, "read_" + att.getName(), code);
            code = getWriteAttributeCode(old_parser, att);
            new_parser.removeProtectedZone(classname, "write_" + att.getName());
            new_parser.insertInProtectedZone(classname, "write_" + att.getName(), code);
        }
        code = old_parser.getUnexpectedMethods();
        new_parser.insertInProtectedZone(classname, "namespace_ending", code);
        new_parser.write();
        System.out.println(filename + ".cpp  updated");
        new_parser = new PogoParser(filename + "Class.cpp");
        old_parser = new OldModelParser(old_model.projectFiles.getServerClass(), old_model);
        code = getIncludeFiles("Class.cpp");
        new_parser.insertIncludeFiles(code);
        code = getConstructorCode(old_parser);
        new_parser.insertInProtectedZone(classname, "Class::constructor", code);
        code = getInitPropertyCode(old_parser, false);
        new_parser.insertInProtectedZone(classname, "Class::get_class_property_before", code);
        code = getEndPropertyCode(old_parser, false);
        new_parser.insertInProtectedZone(classname, "Class::get_class_property_after", code);
        code = getDserverAdditionnalMethods(old_parser);
        new_parser.insertInProtectedZone(classname, "Class::Additional Methods", code);
        code = getDeviceFactoryCode(old_parser, true);
        new_parser.insertInProtectedZone(classname, "Class::device_factory_before", code);
        code = getDeviceFactoryCode(old_parser, false);
        new_parser.insertInProtectedZone(classname, "Class::device_factory_after", code);
        new_parser.write();
        System.out.println(filename + "Class.cpp  updated");
        new_parser = new PogoParser(filename + "Class.h");
        old_parser = new OldModelParser(old_model.projectFiles.getServerClass_h(), old_model);
        code = getIncludeFiles("Class.h");
        new_parser.insertIncludeFiles(code);
        code = getDserverDataMembers(old_parser);
        new_parser.insertInProtectedZone(classname, "Additionnal DServer data members", code);
        code = getDserverDynamicClasses(old_parser);
        new_parser.insertInProtectedZone(classname, "classes for dynamic creation", code);
        new_parser.write();
        System.out.println(filename + "Class.h  updated");
        new_parser = new PogoParser(filename + "StateMachine.cpp");
        old_parser = new OldModelParser(old_model.projectFiles.getAllowed(), old_model);
        for (Command cmd : commands) {
            code = getIsAllowedCode(old_parser, cmd.getName());
            new_parser.insertInProtectedZone(classname, cmd.getName() + "StateAllowed", code);
        }
        for (Attribute att : attributes) {
            code = getIsAllowedCode(old_parser, att.getName());
            new_parser.insertInProtectedZone(classname, "read_" + att.getName() + "StateAllowed_READ", code);
        }
        new_parser.write();
        System.out.println(filename + "StateMachine.cpp  updated");
        try {
            new_parser = new PogoParser(new_model.getDescription().getSourcePath() + "/main.cpp");
            old_parser = new OldModelParser(old_model.projectFiles.getPath() + "/main.cpp", old_model);
            code = getMainCode(old_parser);
            new_parser.removeProtectedZoneAtEnd(classname, "main.cpp", "#i");
            new_parser.insertInProtectedZoneAtEnd(classname, "main.cpp", code);
            new_parser.write();
            System.out.println("main.cpp  updated");
        } catch (DevFailed e) {
            System.err.println("================================================================");
            System.err.println("\t" + e.errors[0].desc);
            System.err.println("================================================================");
        }
    }

    private String getMainCode(OldModelParser parser) {
        String code = parser.getCode();
        int start = code.indexOf("Program Obviously used to Generate tango Object");
        if (start > 0) {
            start = code.indexOf("\n", start) + 1;
            start = code.indexOf("\n", start) + 1;
            start = code.indexOf("\n", start) + 1;
            start = code.indexOf("\n", start) + 1;
            code = code.substring(start);
        }
        return code;
    }

    public void manageCppAdditionalFiles(PogoDeviceClass new_model) throws Exception {
        ArrayList<String> generated = new ArrayList<String>();
        for (String filename : cppFilenames) generated.add(Utils.strReplace(filename, "ClassName", old_model.class_name));
        String new_path = new_model.getDescription().getSourcePath();
        String old_path = old_model.projectFiles.getPath();
        ArrayList<String> files = Utils.getInstance().getFileList(old_path);
        ArrayList<String> addFiles = new ArrayList<String>();
        for (String filename : files) {
            boolean found = false;
            for (String cppFile : generated) if (filename.equals(cppFile)) found = true;
            if (!found) addFiles.add(filename);
        }
        for (String filename : addFiles) {
            System.out.println("Copiing " + filename);
            ParserTool.writeFile(new_path + "/" + filename, ParserTool.readFile(old_path + "/" + filename));
        }
        String oldCFfile = old_path + "/ClassFactory.cpp";
        if (new File(oldCFfile).exists()) {
            String newCFfile = new_model.getDescription().getSourcePath() + "/ClassFactory.cpp";
            ParserTool.writeFile(newCFfile, "/*----- PROTECTED REGION ID(ClassFactory.cpp) ENABLED START -----*/\n" + ParserTool.readFile(oldCFfile) + "/*----- PROTECTED REGION END -----*/\n");
            System.out.println("ClassFactory.cpp  updated");
        }
        File makefile = new File(old_path + "/Makefile");
        if (makefile.exists()) {
            String new_makefile_name = new_path + "/Makefile";
            String code = "#PROTECTED REGION ID(" + new_model.getName() + "Makefile) ENABLED START#\n" + ParserTool.readFile(makefile.toString()) + "#PROTECTED REGION END#\n";
            ParserTool.writeFile(new_makefile_name, code);
        } else {
            StringBuilder objFiles = new StringBuilder();
            for (String filename : addFiles) if (filename.endsWith(".cpp")) {
                objFiles.append("	\\\n		$(OBJS_DIR)/");
                objFiles.append(filename.substring(0, filename.length() - 3));
                objFiles.append("o");
            }
            if (objFiles.length() > 0) {
                try {
                    PogoParser new_parser = new PogoParser(new_path + "/Makefile");
                    new_parser.addObjFiles(objFiles.toString());
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
        }
    }

    @SuppressWarnings({ "ResultOfMethodCallIgnored" })
    public void swapDirectories(PogoDeviceClass new_model) throws Exception {
        String in_test = System.getenv("TEST_MODE");
        if (in_test != null && in_test.toLowerCase().equals("true")) return;
        String old_path = old_model.projectFiles.getPath();
        File old_dir = new File(old_path + "/old_src");
        old_dir.mkdir();
        ArrayList<String> files = Utils.getInstance().getFileList(old_path);
        for (String filename : files) {
            new File(old_path + "/" + filename).renameTo(new File(old_path + "/old_src/" + filename));
        }
        String gene_path = new_model.getDescription().getSourcePath();
        files = Utils.getInstance().getFileList(gene_path);
        for (String filename : files) {
            new File(gene_path + "/" + filename).renameTo(new File(old_path + "/" + filename));
        }
        new File(gene_path).delete();
    }

    private void manageOldProperties(EList<Property> properties, pogo.gene.PropertyTable oldProps) {
        for (int i = 0; i < oldProps.size(); i++) {
            pogo.gene.Property old_prop = oldProps.propertyAt(i);
            Property property = OAWutils.factory.createProperty();
            property.setName(old_prop.name);
            property.setDescription(Utils.strReplaceSpecialCharToCode(old_prop.description));
            EList<String> list = property.getDefaultPropValue();
            if (old_prop.default_value != null) {
                StringTokenizer st = new StringTokenizer(old_prop.default_value, "\n");
                while (st.hasMoreTokens()) list.add(st.nextToken());
            }
            property.setType(PropertyDialog.tango2pogoType(old_prop.type.cpp));
            InheritanceStatus status = OAWutils.factory.createInheritanceStatus();
            status.setAbstract("false");
            status.setInherited("false");
            status.setConcrete("true");
            status.setConcreteHere("true");
            property.setStatus(status);
            properties.add(property);
        }
    }

    private void manageOldStates(EList<State> states, pogo.gene.DevStateTable oldStates) {
        for (int i = 0; i < oldStates.size(); i++) {
            pogo.gene.DevState old_state = oldStates.stateAt(i);
            State state = OAWutils.factory.createState();
            String name = old_state.name;
            if (name.startsWith("Tango::")) name = name.substring("Tango::".length());
            state.setName(name);
            state.setDescription(old_state.description);
            InheritanceStatus status = OAWutils.factory.createInheritanceStatus();
            status.setAbstract("false");
            status.setInherited("false");
            status.setConcrete("true");
            status.setConcreteHere("true");
            state.setStatus(status);
            states.add(state);
        }
    }

    private void manageOldCommands(EList<Command> commands, pogo.gene.CmdTable oldCmds) {
        for (int i = 0; i < oldCmds.size(); i++) {
            pogo.gene.Cmd old_cmd = oldCmds.cmdAt(i);
            Command cmd = OAWutils.factory.createCommand();
            cmd.setName(old_cmd.name);
            cmd.setDescription(old_cmd.description);
            InheritanceStatus status = OAWutils.factory.createInheritanceStatus();
            if (old_model.is_abstractclass) status.setAbstract("true"); else {
                status.setAbstract("false");
                if (old_model.inherited_from.startsWith("Device_") && old_model.inherited_from.endsWith("Impl")) status.setInherited("false"); else status.setInherited("true");
                status.setConcrete("true");
                status.setConcreteHere("true");
            }
            if (old_cmd.virtual_method) {
                status.setAbstract("true");
                status.setInherited("true");
                if (old_cmd.override_method == pogo.gene.PogoDefs.NOT_OVERRIDE) {
                    if (old_cmd.name.equals("State") || old_cmd.name.equals("Status")) status.setConcrete("true"); else status.setConcrete("false");
                    status.setConcreteHere("false");
                }
            }
            cmd.setStatus(status);
            Argument argin = OAWutils.factory.createArgument();
            Argument argout = OAWutils.factory.createArgument();
            argin.setType(OAWutils.tango2pogoType(old_cmd.argin.cpp));
            argin.setDescription(old_cmd.argin.description);
            argout.setType(OAWutils.tango2pogoType(old_cmd.argout.cpp));
            argout.setDescription(old_cmd.argout.description);
            cmd.setArgin(argin);
            cmd.setArgout(argout);
            cmd.setDisplayLevel(PogoConst.strLevel[old_cmd.level.value()]);
            if (old_cmd.polled_period > 0) cmd.setPolledPeriod(Integer.toString(old_cmd.polled_period));
            EList<String> new_states = cmd.getExcludedStates();
            pogo.gene.DevStateTable old_states = old_cmd.notAllowedFor;
            for (int st = 0; st < old_states.size(); st++) {
                String name = old_states.stateAt(st).name;
                if (name.startsWith("Tango::")) name = name.substring("Tango::".length());
                new_states.add(name);
            }
            cmd.setExecMethod(Utils.buildExcecMethodName(cmd.getName()));
            commands.add(cmd);
        }
    }

    private void manageOldAttributes(EList<Attribute> attributes, pogo.gene.AttribTable oldAttribTable) {
        for (int i = 0; i < oldAttribTable.size(); i++) {
            pogo.gene.Attrib old_attr = oldAttribTable.attributeAt(i);
            Attribute attribute = OAWutils.factory.createAttribute();
            attribute.setName(old_attr.name);
            attribute.setAttType(PogoConst.AttrTypeArray[old_attr.attrType]);
            attribute.setRwType(PogoConst.AttrRWtypeArray[old_attr.rwType]);
            Type pogoDataType = OAWutils.tango2pogoType(old_attr.dataType.cpp);
            attribute.setDataType(pogoDataType);
            attribute.setMaxX(Integer.toString(old_attr.xSize));
            attribute.setMaxY(Integer.toString(old_attr.ySize));
            int n = 0;
            AttrProperties prop = OAWutils.factory.createAttrProperties();
            prop.setLabel(old_attr.getPropValue(n++));
            prop.setUnit(old_attr.getPropValue(n++));
            prop.setStandardUnit(old_attr.getPropValue(n++));
            prop.setDisplayUnit(old_attr.getPropValue(n++));
            prop.setFormat(old_attr.getPropValue(n++));
            prop.setMaxValue(old_attr.getPropValue(n++));
            prop.setMinValue(old_attr.getPropValue(n++));
            prop.setMaxAlarm(old_attr.getPropValue(n++));
            prop.setMinAlarm(old_attr.getPropValue(n++));
            prop.setMaxWarning(old_attr.getPropValue(n++));
            prop.setMinWarning(old_attr.getPropValue(n++));
            prop.setDeltaTime(old_attr.getPropValue(n++));
            prop.setDeltaValue(old_attr.getPropValue(n));
            prop.setDescription(old_attr.getDescription());
            attribute.setProperties(prop);
            attribute.setPolledPeriod(Integer.toString(old_attr.polled_period));
            attribute.setDisplayLevel(PogoConst.strLevel[old_attr.disp_level.value()]);
            if (old_attr.memorized) {
                attribute.setMemorized("true");
                if (old_attr.memorized_init) attribute.setMemorizedAtInit("true");
            }
            FireEvents fe_change = OAWutils.factory.createFireEvents();
            boolean[] b_change = old_attr.getFireEvent(pogo.gene.Attrib.CHANGE);
            fe_change.setFire("" + b_change[0]);
            fe_change.setLibCheckCriteria("" + b_change[1]);
            attribute.setChangeEvent(fe_change);
            FireEvents fe_archive = OAWutils.factory.createFireEvents();
            boolean[] b_archive = old_attr.getFireEvent(pogo.gene.Attrib.ARCHIVE);
            fe_archive.setFire("" + b_archive[0]);
            fe_archive.setLibCheckCriteria("" + b_archive[1]);
            attribute.setArchiveEvent(fe_archive);
            EList<String> new_states = attribute.getReadExcludedStates();
            pogo.gene.DevStateTable old_states = old_attr.notAllowedFor;
            for (int st = 0; st < old_states.size(); st++) {
                String name = old_states.stateAt(st).name;
                if (name.startsWith("Tango::")) name = name.substring("Tango::".length());
                new_states.add(name);
            }
            InheritanceStatus status = OAWutils.factory.createInheritanceStatus();
            if (old_model.is_abstractclass) status.setAbstract("true"); else {
                status.setAbstract("false");
                if (old_model.inherited_from.startsWith("Device_") && old_model.inherited_from.endsWith("Impl")) status.setInherited("false"); else status.setInherited("true");
                status.setConcrete("true");
                status.setConcreteHere("true");
                attribute.setStatus(status);
            }
            attribute.setStatus(status);
            attributes.add(attribute);
        }
    }

    private String getIncludeFiles(String ext) throws Exception {
        String filename;
        if (ext.equals(".h")) filename = old_model.projectFiles.getServer_h(); else if (ext.equals(".cpp")) filename = old_model.projectFiles.getServer(); else if (ext.equals("Class.h")) filename = old_model.projectFiles.getServerClass_h(); else if (ext.equals("Class.cpp")) filename = old_model.projectFiles.getServerClass(); else return "";
        OldModelParser parser = new OldModelParser(filename, old_model);
        return parser.getIncludeFiles();
    }

    private String getDefinitions() throws Exception {
        OldModelParser parser = new OldModelParser(old_model.projectFiles.getServer_h(), old_model);
        String defs = parser.getDefinitions();
        if (defs != null && defs.length() > 0) defs = "\n\n" + defs;
        return defs;
    }

    private String getEndPropertyCode(OldModelParser parser, boolean is_dev) throws Exception {
        String code = null;
        String signature;
        int start;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                if (is_dev) signature = "void " + old_model.class_name + "::get_device_property()"; else signature = "void " + old_model.class_name + "Class::get_class_property()";
                code = parser.getCppMethodCode(signature);
                if (code == null) System.out.println("Cannot read post code from " + signature); else {
                    start = code.indexOf("End of Automatic code generation");
                    if (start < 0) return null;
                    start = code.indexOf('\n', start);
                    if (start < 0) return null;
                    start = code.indexOf('\n', start + 1);
                    if (start < 0) return null;
                    code = "\t" + code.substring(start).trim();
                }
                break;
        }
        return code;
    }

    private String getConstructorCode(OldModelParser parser) throws Exception {
        String code = null;
        String signature;
        int start, end;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                signature = old_model.class_name + "Class::" + old_model.class_name + "Class(string &s):DeviceClass(s)";
                code = parser.getCppMethodCode(signature);
                if (code == null) System.err.println("Cannot read init code from " + signature); else {
                    start = code.indexOf("write_class_property();");
                    if (start < 0) {
                        System.err.println("Cannot read code from " + signature);
                        return null;
                    }
                    start = code.indexOf('\n', start);
                    start = code.indexOf('\n', start + 1);
                    if ((end = code.indexOf("Leaving " + old_model.class_name + "Class constructor")) > 0) {
                        end = code.lastIndexOf('\n', end);
                        code = "\t" + code.substring(start, end).trim();
                    } else code = "\t" + code.substring(start).trim();
                }
                break;
        }
        return code;
    }

    private String getInitPropertyCode(OldModelParser parser, boolean is_dev) throws Exception {
        String code = null;
        String signature;
        int start, end;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                if (is_dev) signature = "void " + old_model.class_name + "::get_device_property()"; else signature = "void " + old_model.class_name + "Class::get_class_property()";
                code = parser.getCppMethodCode(signature);
                if (code == null) System.err.println("Cannot read init code from " + signature); else {
                    start = code.indexOf("Initialize your default values here");
                    if (start < 0) {
                        System.err.println("Cannot read init code from " + signature);
                        return null;
                    }
                    start = code.indexOf('\n', start);
                    start = code.indexOf('\n', start + 1);
                    end = code.indexOf("properties from database.(Automatic code generation)");
                    if (end < 0) {
                        System.err.println("Cannot read init code from " + signature);
                        return null;
                    }
                    end = code.lastIndexOf('\n', end);
                    code = "\t" + code.substring(start, end).trim();
                }
                break;
        }
        return code;
    }

    private String getDserverDataMembers(OldModelParser parser) throws Exception {
        String code = parser.getCode();
        int start, end;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                start = code.indexOf("Class : public Tango::DeviceClass");
                if (start < 0) return null;
                start = code.indexOf("add your own data members here", start);
                if (start < 0) return null;
                start = code.indexOf('\n', start);
                start = code.indexOf('\n', start + 1);
                end = code.indexOf("public:", start);
                code = "public:\n\t" + code.substring(start, end).trim();
                break;
        }
        return code;
    }

    private String getDserverDynamicClasses(OldModelParser parser) throws Exception {
        String code = parser.getCode();
        int start, end;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                start = code.indexOf("namespace " + old_model.class_name + "_ns");
                if (start < 0) return null;
                start = code.indexOf("{\n", start) + 2;
                end = code.indexOf("Define classes for attributes", start);
                if (end < 0) return null;
                end = code.lastIndexOf("//========", end);
                code = code.substring(start, end).trim();
                break;
        }
        return code;
    }

    private String getDserverAdditionnalMethods(OldModelParser parser) throws Exception {
        String code = parser.getCode();
        int start, end;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                start = code.indexOf("get_db_class()->put_property(data);");
                if (start < 0) return null;
                start = code.indexOf('}', start);
                start += 2;
                end = code.indexOf("}	// namespace", start);
                code = code.substring(start, end).trim();
                break;
        }
        return code;
    }

    private String getDeviceFactoryCode(OldModelParser parser, boolean pre_processing) throws Exception {
        String code = parser.getCode();
        int start, end;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                if (pre_processing) {
                    end = code.indexOf("Create all devices.(Automatic code generation)");
                    if (end < 0) return null;
                    end = code.lastIndexOf('\n', end) + 1;
                    start = code.lastIndexOf('{', end) + 1;
                    code = "\t" + code.substring(start, end).trim();
                } else {
                    start = code.indexOf("Create all devices.(Automatic code generation)");
                    if (start < 0) return null;
                    start = code.indexOf("End of Automatic code generation", start);
                    if (start < 0) return null;
                    start = code.indexOf('\n', start) + 1;
                    start = code.indexOf('\n', start) + 1;
                    end = code.indexOf("\n}", start);
                    code = "\t" + code.substring(start, end).trim();
                }
                break;
        }
        return code;
    }

    private String getStaticInit(OldModelParser parser) throws Exception {
        String code = null;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                code = parser.getCppStaticInit(old_model.class_name);
                break;
        }
        return code;
    }

    private String getInitDeviceCodeBefore(OldModelParser parser) throws Exception {
        String code = null;
        String signature;
        int end;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                signature = "void " + old_model.class_name + "::init_device()";
                code = parser.getCppMethodCode(signature);
                if ((end = code.indexOf("get_device_property();")) > 0) {
                    end = code.lastIndexOf("\n", end) + 1;
                    code = code.substring(0, end);
                    int start = code.indexOf("Initialise variables to default values");
                    if (start >= 0) {
                        end = code.indexOf('\n', start);
                        start = code.lastIndexOf('\n', start);
                        if (start < 0) start = 0;
                        code = code.substring(0, start) + code.substring(end + 1);
                        if (code.trim().startsWith("//-----------------------")) {
                            start = code.indexOf("//----------------------");
                            end = code.indexOf('\n', start + 1);
                            code = code.substring(0, start) + code.substring(end + 1);
                        }
                    }
                } else code = "";
                break;
        }
        return code;
    }

    private String getInitDeviceCodeAfter(OldModelParser parser) throws Exception {
        String code = null;
        String signature;
        int start;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                signature = "void " + old_model.class_name + "::init_device()";
                code = parser.getCppMethodCode(signature);
                if ((start = code.indexOf("get_device_property();")) > 0) {
                    start = code.indexOf("\n", start) + 1;
                    code = code.substring(start);
                }
                break;
        }
        return code;
    }

    private String getDeleteDeviceCode(OldModelParser parser) throws Exception {
        String code = null;
        String signature;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                signature = "void " + old_model.class_name + "::delete_device()";
                code = parser.getCppMethodCode(signature);
                break;
        }
        return code;
    }

    private String getCommandCode(OldModelParser parser, Command cmd) throws Exception {
        String code = null;
        pogo.gene.CmdTable old_commands = old_model.commands;
        for (int i = 0; i < old_commands.size(); i++) {
            pogo.gene.Cmd old_cmd = old_commands.cmdAt(i);
            if (old_cmd.name.equals(cmd.getName())) {
                String signature;
                switch(old_model.language) {
                    case pogo.gene.PogoDefs.cppLang:
                        signature = old_cmd.buildCppExecCmdMethodSignature(old_model.class_name);
                        code = parser.getCppMethodCode(signature);
                        if (code != null) {
                            code = checkLinesToRemove(old_cmd, code);
                            String argin_def = parser.getCppMethodArginDef(signature);
                            if (argin_def != null && !argin_def.contains("argin")) {
                                int pos = argin_def.lastIndexOf('*');
                                if (pos < 0) pos = argin_def.lastIndexOf(' ');
                                if (pos < 0) pos = argin_def.lastIndexOf('\t');
                                if (pos > 0) {
                                    String vartype = argin_def.substring(0, pos);
                                    String varname = argin_def.substring(pos);
                                    code = "	" + vartype + " " + varname + " = argin;\n" + code;
                                }
                            }
                        }
                        break;
                }
            }
        }
        return code;
    }

    private String checkLinesToRemove(pogo.gene.Cmd old_cmd, String code) {
        code = code.trim();
        int start = code.indexOf(old_cmd.argout.cpp);
        if (start > 0) {
            if ((start = code.lastIndexOf('\n', start)) < 0) start = 0;
            int end = code.indexOf('\n', start + 1);
            String line = code.substring(start, end);
            if (line.indexOf(" argout;") > 0 || line.indexOf("\targout;") > 0) code = code.substring(0, start) + code.substring(end);
        }
        if (code.endsWith("return argout;")) {
            start = code.lastIndexOf('\n');
            code = code.substring(0, start).trim();
        }
        if (code.startsWith("INFO_STREAM") || code.startsWith("DEBUG_STREAM")) {
            int end = code.indexOf('\n');
            code = code.substring(end).trim();
        }
        if (code.startsWith("//  Add your own code") || code.startsWith("//\tAdd your own code")) {
            int end = code.indexOf('\n');
            if (end > 0) code = code.substring(end).trim();
        }
        return "\t" + code;
    }

    private String getReadAttributeCode(OldModelParser parser, Attribute att) throws Exception {
        String code = null;
        pogo.gene.AttribTable old_attributes = old_model.attributes;
        for (int i = 0; i < old_attributes.size(); i++) {
            pogo.gene.Attrib old_att = old_attributes.attributeAt(i);
            if (old_att.name.equals(att.getName())) {
                String signature;
                switch(old_model.language) {
                    case pogo.gene.PogoDefs.cppLang:
                        signature = "void " + old_model.class_name + "::read_" + old_att.name + "(Tango::Attribute &attr)";
                        code = parser.getCppMethodCode(signature);
                        break;
                }
            }
        }
        return code;
    }

    private String getReadAttrHardwareCode(OldModelParser parser) throws Exception {
        String code = null;
        String signature;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                signature = "void " + old_model.class_name + "::read_attr_hardware(vector<long> &attr_list)";
                code = parser.getCppMethodCode(signature);
                break;
        }
        return code;
    }

    private String getAlwaysExecutedHookCode(OldModelParser parser) throws Exception {
        String code = null;
        String signature;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                signature = "void " + old_model.class_name + "::always_executed_hook()";
                code = parser.getCppMethodCode(signature);
                break;
        }
        return code;
    }

    private String getWriteAttributeCode(OldModelParser parser, Attribute att) throws Exception {
        String code = null;
        pogo.gene.AttribTable old_attributes = old_model.attributes;
        for (int i = 0; i < old_attributes.size(); i++) {
            pogo.gene.Attrib old_att = old_attributes.attributeAt(i);
            if (old_att.name.equals(att.getName())) {
                String signature;
                switch(old_model.language) {
                    case pogo.gene.PogoDefs.cppLang:
                        signature = "void " + old_model.class_name + "::write_" + old_att.name + "(Tango::WAttribute &attr)";
                        code = parser.getCppMethodCode(signature);
                        code = parser.manageWriteValue(code, att.getName());
                        break;
                }
            }
        }
        return code;
    }

    private String getIsAllowedCode(OldModelParser parser, String name) throws Exception {
        String code = parser.getCode();
        int start, end;
        String signature;
        switch(old_model.language) {
            case pogo.gene.PogoDefs.cppLang:
                signature = "bool " + old_model.class_name + "::is_" + name + "_allowed(Tango::AttReqType type)";
                start = code.indexOf(signature);
                if (start < 0) return null;
                start = code.indexOf("End of Generated Code", start);
                if (start < 0) return null;
                start = code.indexOf("\n", start);
                end = code.indexOf("Re-Start of Generated Code", start);
                if (end < 0) return null;
                end = code.lastIndexOf("\n", end);
                code = code.substring(start, end).trim();
                if (code.length() > 0) code = "\t\t" + code;
                break;
        }
        return code;
    }

    public void generateDocFromOldModel(String oldDescFilename, String targetDir) throws DevFailed, IOException {
        try {
            String newDescFilename = targetDir + "/index.html";
            System.out.println("Trying to add description from " + oldDescFilename + "\n to  " + newDescFilename);
            PogoParser new_parser = new PogoParser(targetDir + "/index.html");
            OldModelParser old_parser = new OldModelParser(oldDescFilename, old_model);
            String code = old_parser.getHtmlDescripion();
            if (code != null && code.length() > 0) {
                if (!new_parser.codeExists(code.trim())) new_parser.insertInProtectedZone("", "./doc_html/index.html", code);
                new_parser.write();
            }
        } catch (DevFailed e) {
            System.err.println("Cannot add description: " + e.errors[0].desc);
        }
    }

    public static int checkForInheritance(JFrame parent, DeviceClass devclass) throws DevFailed {
        PogoDeviceClass pg = devclass.getPogoDeviceClass();
        String className = pg.getName();
        Inheritance inheritance = pg.getDescription().getInheritances().get(0);
        if (DeviceClass.isDefaultInheritance(inheritance)) {
            if (devclass.getPogoDeviceClass().getDescription().getLanguage().toLowerCase().equals("java")) inheritance.setClassname("Device_2Impl");
            System.out.println(className + " has no inheritance -> " + inheritance.getClassname());
        } else {
            String inheritName = inheritance.getClassname();
            String message = className + " inherite from " + inheritName + "\n" + "The class " + inheritName + " needs to be converted before !!!\n\n";
            Object[] options = { "Specified path for converted abstract class", "Remove inheritance", "Cancel" };
            int choice = JOptionPane.showOptionDialog(parent, message, "Confirmation Window", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            switch(choice) {
                case 0:
                    String path = getInheritanceDirectory(inheritance);
                    if (path == null) return JOptionPane.CANCEL_OPTION;
                    inheritance.setSourcePath(path);
                    break;
                case 1:
                    devclass.removeInheritance();
                    break;
                case 2:
                case -1:
                default:
                    return JOptionPane.CANCEL_OPTION;
            }
        }
        return JOptionPane.OK_OPTION;
    }

    private static String getInheritanceDirectory(Inheritance inher) throws DevFailed {
        String className = inher.getClassname();
        String path = InheritanceUtils.checkInheritanceFileFromEnv(null);
        if (path == null) path = inher.getSourcePath();
        JFileChooser chooser = new JFileChooser(new File(path).getAbsolutePath());
        PogoFileFilter filter = new PogoFileFilter("xmi", className + " Class");
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(new JFrame()) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                if (!file.isDirectory()) {
                    String filename = file.getAbsolutePath();
                    if (filename.endsWith(className + ".xmi")) {
                        DeviceClass devClass = new DeviceClass(filename);
                        return devClass.getPogoDeviceClass().getDescription().getSourcePath();
                    } else JOptionPane.showMessageDialog(new JFrame(), filename + "\n     is NOT the file defining " + className + " class", "Error Window", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
        return null;
    }
}
